package com.eneco.trading.kafka.connect.nationalgrid.source

import java.util.{Calendar, GregorianCalendar}
import javax.xml.datatype.DatatypeFactory

import com.eneco.trading.kafka.connect.nationalgrid.config.{NGSourceConfig, NGSourceSettings}
import com.eneco.trading.kafka.connect.nationalgrid.domain.{IFDRMessage, MIPIMessage, PullMap}
import nationalgrid.{ArrayOfCLSMIPIPublicationObjectBE, ArrayOfString, CLSRequestObject, GetPublicationDataWMResponse, InstantaneousFlowWebServiceSoap, PublicWebServiceSoap}
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.source.{SourceRecord, SourceTaskContext}

import scala.concurrent._
import scala.collection.JavaConversions._
import org.scala_tools.time.Imports._
import com.datamountaineer.streamreactor.connect.offsets.OffsetHandler
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.apache.kafka.connect.errors.ConnectException
import org.joda.time.Days

import scala.util.{Failure, Success, Try}

object NGReader {
  def apply(settings: NGSourceSettings, context: SourceTaskContext): NGReader = new NGReader(settings, context)
}

class NGReader(settings: NGSourceSettings, context : SourceTaskContext) extends MIPIMessage with IFDRMessage with StrictLogging {
  logger.info("Initialising National Grid Reader")

  private val defaultTimestamp = NGSourceConfig.DEFAULT_OFFSET_TIMESTAMP
  private val dateFormatter = DateTimeFormat.forPattern(NGSourceConfig.DEFAULT_OFFSET_PATTERN)
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
  val frequencies: Map[String, PullMap] = settings.mipiRequests.map(mipi => (mipi.dataItem, mipi)).toMap
  var ifrErrorCounter = 0
  val ifrMaxErrors = 100
  var backoff = new ExponentialBackOff(settings.refreshRate, settings.maxBackOff)

  /**
    * Build a map of table to offset.
    *
    * @param context SourceTaskContext for this task.
    * @return The last stored offset.
    * */
  def buildOffsetMap(context: SourceTaskContext, mipi: Set[PullMap]) : Map[String, DateTime] = {
    mipi.flatMap(mipi => {
      val dataItems = List(mipi.dataItem)
      val offsetKeys = OffsetHandler.recoverOffsets(mipi.dataItem, dataItems, context)

      if (offsetKeys.isEmpty) {
        logger.warn(s"Failed to recover any offset keys fom dataItem ${mipi.dataItem}. Default will be used.")
      }

      dataItems
        .map(di => (di, OffsetHandler.recoverOffset[String](offsetKeys, di, di, NGSourceConfig.OFFSET_FIELD))) //recover partitions
        .map({ case (k, v) => {
          val offsetDate = dateFormatter.parseDateTime(v.getOrElse(defaultTimestamp))
          logger.info(s"Recovered offset for $k with value ${offsetDate.toString()}")
          (k, offsetDate)
        }
      }).toMap
    }).toMap
  }

    /**
    * Process and new NG feeds that are available
    *
    * */
  def process() : List[SourceRecord] = {
    Thread.sleep(5000)

    if (!backoff.passed) {
      return List[SourceRecord]()
    }

    logger.info("Backoff passed, asking National grid for last publication time.")

    val ifd =
      Try(processIFD()) match {
      case Success(s) =>
        backoff = backoff.nextSuccess
        logger.info(s"Next poll will be around ${backoff.endTime}")
        s
      case Failure(f) =>
        backoff = backoff.nextFailure()
        logger.error(s"Error trying to retrieve IFR data. ${f.getMessage}")
        logger.info(s"Backing off. Next poll will be around ${backoff.endTime}")
        ifrErrorCounter += 1
        if (ifrErrorCounter.equals(ifrMaxErrors)) {
          throw new ConnectException(s"Error trying to retrieve IFR data. ${f.getMessage}")
        } else {
          Seq.empty[SourceRecord]
        }
      }

    (ifd ++ settings
      .mipiRequests
      .filter(req => pull(req.dataItem))
      .flatMap(req => buildCLSRequest(req.dataItem))
      .flatMap(processMIPI)).toList
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
    val frequency = frequencies(dataItem)
    val pubTime = new DateTime().withTime(frequency.pubHour, frequency.pubMin, 0, 0)

    if (now.isAfter(marker) && now.isAfter(pubTime) && marker.isBefore(pubTime)) {
      logger.debug(s"Pulling data from $dataItem. Last marker was ${marker.toDateTime.toString()}.")
      true
    } else {
      logger.debug(s"Not pulling data from $dataItem. Last marker was ${marker.toDateTime.toString()}.")
      false
    }
  }

  /**
    * Build a CLSRequest for MIPI
    *
    * @param dataItem The data item to fetch
    * @return A CLS for the gas day and data item
    * */
  def buildCLSRequest(dataItem: String) : List[CLSRequestObject] = {
    val marker = offsetMap(dataItem)
    val now = DateTime.now
    val diff = Days.daysBetween(marker, now).getDays
    val dayDiff = if (diff >= 30) 30 else diff

    val days = List.range(0, dayDiff).reverse
    days.map( x =>
    {
      val gasDay = DatatypeFactory.newInstance().newXMLGregorianCalendar(now.minusDays(x).toGregorianCalendar)
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
    })
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
    val newMarker = DateTime.now.plusMinutes(frequencies(dataItem).frequency)
    offsetMap(dataItem) = newMarker

    if (records.isEmpty) logger.warn(s"No data retrieved for dataItem $dataItem.")

    records.map(r => {
      new SourceRecord(
        Map(dataItem -> dataItem),
        Map(NGSourceConfig.OFFSET_FIELD -> newMarker.toDateTime.toString()),
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
    logger.info("Getting last publication time for IFR")
    val lastPubTime =  Await.result(ifService.getLatestPublicationTime(), Duration(30, SECONDS)).toGregorianCalendar
    val next = if (ifrPubTracker.isDefined) {
      val tracker = ifrPubTracker.get
      tracker.add(Calendar.MINUTE, 5)
      tracker
    } else {
      lastPubTime
    }

    if (lastPubTime.after(next) || lastPubTime.equals(next)) {
      logger.info(s"Poll time ${next.getTime} is later than or equal to the last IFR Publication ${lastPubTime.getTime}. Pulling data.")
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
            Map(NGSourceConfig.OFFSET_FIELD -> lastPubTime.getTime.toString),
            ifrTopic,
            r.schema(),
            r)
        })
    } else {
      logger.info(s"Last IFR publication time is ${lastPubTime.getTime.toString}, last pulled at ${next.getTime}. Not pulling data.")
      ifrPubTracker = Some(lastPubTime)
      Seq.empty[SourceRecord]
    }
  }
}