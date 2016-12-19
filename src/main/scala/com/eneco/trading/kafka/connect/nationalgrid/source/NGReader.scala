package com.eneco.trading.kafka.connect.nationalgrid.source

import java.util.{Calendar, GregorianCalendar}
import javax.xml.datatype.DatatypeFactory

import com.eneco.trading.kafka.connect.nationalgrid.config.{NGSourceConfig, NGSourceSettings}
import com.eneco.trading.kafka.connect.nationalgrid.domain.{IFDRMessage, MIPIMessage, PullMap}
import com.typesafe.scalalogging.StrictLogging
import nationalgrid.{ArrayOfCLSMIPIPublicationObjectBE, ArrayOfString, CLSRequestObject, GetPublicationDataWMResponse, InstantaneousFlowWebServiceSoap, PublicWebServiceSoap}
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.source.{SourceRecord, SourceTaskContext}

import scala.concurrent._
import scala.collection.JavaConversions._
import org.scala_tools.time.Imports._
import com.datamountaineer.streamreactor.connect.offsets.OffsetHandler



/**
  * Created by andrew@datamountaineer.com on 08/07/16. 
  * stream-reactor
  */
object NGReader {
  def apply(settings: NGSourceSettings, context: SourceTaskContext): NGReader = new NGReader(settings, context)
}

class NGReader(settings: NGSourceSettings, context : SourceTaskContext) extends MIPIMessage with IFDRMessage with StrictLogging {
  logger.info("Initialising National Grid Reader")

  private val defaultTimestamp = "1900-01-01 00:00:00.000Z"
  private val dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'")
  var ifrPubTracker: Option[GregorianCalendar] = None

  val ifService: InstantaneousFlowWebServiceSoap = (new nationalgrid.InstantaneousFlowWebServiceSoap12Bindings with
    scalaxb.SoapClientsAsync with
    DispatchHttpClientsAsyncNG {}).service

  val mipiService: PublicWebServiceSoap = (new nationalgrid.PublicWebServiceSoap12Bindings with
    scalaxb.SoapClientsAsync with
    DispatchHttpClientsAsyncNG {}).service

  private val ifrTopic = settings.ifrTopic
  private val mipiTopic = settings.mipiTopic
  val offsetMap = collection.mutable.Map(buildOffsetMap(context, settings.mipiRequests).toSeq: _*)

  offsetMap.foreach(offset => logger.info(s"Recovered offsets ${offset.toString()}"))

  val frequencies = settings.mipiRequests.map(mipi => (mipi.dataItem, mipi)).toMap

  /**
    * Build a map of table to offset.
    *
    * @param context SourceTaskContext for this task.
    * @return The last stored offset.
    * */
  def buildOffsetMap(context: SourceTaskContext, mipi: Set[PullMap]) : Map[String, DateTime] = {
    mipi.map(mipi => {
      val dataItems = List(mipi.dataItem)
      val offsetKeys = OffsetHandler.recoverOffsets(mipi.dataItem, dataItems, context)
      dataItems
        .map(di => (di, OffsetHandler.recoverOffset[String](offsetKeys, NGSourceConfig.OFFSET_FIELD, di, NGSourceConfig.OFFSET_FIELD))) //recover partitions
        .map({case (k,v) => (k, dateFormatter.parseDateTime(v.getOrElse(defaultTimestamp)))}).toMap
    }).flatten.toMap
  }

  /**
    * Process and new NG feeds that are available
    *
    * */
  def process() : List[SourceRecord] = {
    val ifd = processIFD()
    val mipi = settings
                  .mipiRequests
                  .filter(req => pull(req.dataItem))
                  .map(req => buildCLSRequest(Some(new GregorianCalendar()), req.dataItem))
                  .flatMap(processMIPI)
    (ifd ++ mipi).toList
  }

  /**
    * Determine if we should pull data from NG.
    * The marker is incremented on successful read
    *
    * @param dataItem The dataItem to check
    * @return Boolean indicating if we should pull
    * */
  def pull(dataItem: String) : Boolean = {
    val now = DateTime.now //get current time
    val marker = offsetMap(dataItem) //last publication
    val frequency = frequencies.get(dataItem).get
    val pubTime = new DateTime().withTime(frequency.pubHour, frequency.pubMin, 0, 0)

    if (now.isAfter(marker) && now.isAfter(pubTime)) true else false
  }

  /**
    * Build a CLSRequest for MIPI
    *
    * @param date The gas day to build the request for
    * @param dataItem The data item to fetch
    * @return A CLS for the gas day and data item
    * */
  def buildCLSRequest(date: Option[GregorianCalendar] = None, dataItem: String) : CLSRequestObject = {

    val gasDay =  if (date.isDefined) {
      DatatypeFactory.newInstance().newXMLGregorianCalendar(date.get)
    } else {
      DatatypeFactory.newInstance().newXMLGregorianCalendar()
    }

    gasDay.setHour(0)
    gasDay.setMinute(0)
    gasDay.setSecond(0)
    gasDay.setMillisecond(0)
    CLSRequestObject(LatestFlag = Some("Y"),
      ApplicableForFlag = Some("N"),
      FromDate = gasDay,
      ToDate = gasDay,
      DateType = Some("GASDAY"),
      PublicationObjectNameList = Some(ArrayOfString(Some(dataItem))))
  }

  /**
    * Process a MIPI request
    *
    * @param request The request to process
    * @return A sequence of source records
    * */
  def processMIPI(request : CLSRequestObject) : Seq[SourceRecord] = {
    import scala.concurrent.duration._
    logger.info(s"Sending request for ${request.PublicationObjectNameList.get} for Gas Day ${request.FromDate.toString}")
    val req = mipiService.getPublicationDataWM(Some(request))
    val mipiPDWMResp: GetPublicationDataWMResponse = Await.result(req, Duration.Inf)
    val mipiReports = mipiPDWMResp.GetPublicationDataWMResult.getOrElse(None)

    val records = mipiReports match {
    case None =>
      logger.warn("No MIPI reports found!")
      Seq.empty[Struct]
    case report : ArrayOfCLSMIPIPublicationObjectBE => processMIPIReport(report)
    }
    val dataItem = request.PublicationObjectNameList.get.string.map(m=>m.get).mkString

    //set the offset as now plus frequency
    val newMarker = DateTime.now.plusMinutes(frequencies.get(dataItem).get.frequency)
    offsetMap(dataItem) = newMarker

    records.map(r => {
      new SourceRecord(
        Map(r.get("dataItemName").toString -> r.get("dataItemName")),
        Map(NGSourceConfig.OFFSET_FIELD -> newMarker),
        mipiTopic,
        r.schema(),
        r)
    })
  }

  /**
    * Process an Instantaneous Flow Request
    *
    * Call the service and convert to source records
    * */
  def processIFD(): Seq[SourceRecord] = {

    import duration._
    val lastPubTime =  Await.result(ifService.getLatestPublicationTime(), Duration(30, SECONDS)).toGregorianCalendar
    val next = if (ifrPubTracker.isDefined) {
      val tracker = ifrPubTracker.get
      tracker.add(Calendar.SECOND, 60)
      tracker
    } else {
      lastPubTime
    }

    if (lastPubTime.after(next) || lastPubTime.equals(next)) {
      logger.info(s"Poll time ${next.getTime} is later than or equal to the last IFR Publication. Puling data.")
      val ifd = Await.result(ifService.getInstantaneousFlowData(), Duration(60, SECONDS))
      val reports = ifd.GetInstantaneousFlowDataResult.getOrElse(None)

      val records = reports match {
        case None =>
          logger.warn("No reports found for IFR!")
          Seq.empty[Struct]
        case report : nationalgrid.EDPReportBE => Seq(processReport(report))
      }

      ifrPubTracker = Some(lastPubTime)

      records
        .map(r => {
          new SourceRecord(
            Map("IFR"-> r.get("reportName")),
            Map(NGSourceConfig.OFFSET_FIELD -> r.get(NGSourceConfig.OFFSET_FIELD)),
            ifrTopic,
            r.schema(),
            r)
        })
    } else {
      logger.info(s"Last IFR publication time is ${lastPubTime.getTime.toString}, " +
        s"last pulled at ${next.getTime}. Not pulling data.")
      ifrPubTracker = Some(lastPubTime)
      Seq.empty[SourceRecord]
    }
  }
}