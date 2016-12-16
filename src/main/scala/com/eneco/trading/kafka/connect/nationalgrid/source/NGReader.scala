package com.eneco.trading.kafka.connect.nationalgrid.source

import java.time.Instant
import java.util.{Calendar, GregorianCalendar}
import javax.xml.datatype.{DatatypeFactory, XMLGregorianCalendar}

import com.eneco.trading.kafka.connect.nationalgrid.config.NGSourceSettings
import com.eneco.trading.kafka.connect.nationalgrid.domain.{IFDRMessage, MIPIMessage}
import com.typesafe.scalalogging.StrictLogging
import nationalgrid.{ArrayOfCLSMIPIPublicationObjectBE, ArrayOfString, CLSRequestObject, GetPublicationDataWMResponse, InstantaneousFlowWebServiceSoap, PublicWebServiceSoap}
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.source.SourceRecord
import org.joda.time.Instant

import scala.concurrent._
import scala.collection.JavaConversions._
import scala.collection.mutable
import org.scala_tools.time.Imports._


/**
  * Created by andrew@datamountaineer.com on 08/07/16. 
  * stream-reactor
  */

case class PullMap(dataItem: String, pubTimeHour: Int, pubTimeMinute: Int)

object NGReader {
  def apply(settings: NGSourceSettings): NGReader = new NGReader(settings)
}

class NGReader(settings: NGSourceSettings) extends MIPIMessage with IFDRMessage with StrictLogging {
  logger.info("Initialising SOAP Reader")

  private val defaultTimestamp = "1900-01-01 00:00:00.000Z"
  private val dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'")
  private val offsetField = "publishedTime"
  var ifrPubTracker: Option[GregorianCalendar] = None

  val ifService: InstantaneousFlowWebServiceSoap = (new nationalgrid.InstantaneousFlowWebServiceSoap12Bindings with
    scalaxb.SoapClientsAsync with
    DispatchHttpClientsAsyncNG {}).service

  val mipiService: PublicWebServiceSoap = (new nationalgrid.PublicWebServiceSoap12Bindings with
    scalaxb.SoapClientsAsync with
    DispatchHttpClientsAsyncNG {}).service

  private val ifrTopic = settings.ifrTopic
  private val mipiTopic = settings.mipiTopic

  val dataItem = "NTS Physical Flows, Bacton, Interconnector"
  val mipiSchedule = Map(dataItem -> PullMap(dataItem, 0 , 8))
  val offsets = mutable.Map(dataItem -> dateFormatter.parseDateTime(defaultTimestamp))

  /**
    * Process and new NG feeds that are available
    *
    * */
  def process() : List[SourceRecord] = {
    val ifd = processIFD()
    val mipi = settings
                  .mipiRequests
                  .filter(pull)
                  .map(request => buildCLSRequest(dataItem = request))
                  .flatMap(processMIPI)
    (ifd ++ mipi).toList
  }


  def pull(dataItem: String) : Boolean = {
    val today = DateTime.now
    val marker = offsets(dataItem)
    val interval = mipiSchedule(dataItem).pubTimeMinute

    if((today.plusMinutes(interval)) > marker) {
      true
    } else {
      false
    }
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
      DateType = Some("Gas Day"),
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

    records.map(r => {
      new SourceRecord(Map("MIPI"-> r.get("dataItemName")),
        Map(r.get("dataItemName").toString -> r.get(offsetField)),
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
          new SourceRecord(Map("IFR"-> r.get("reportName")),
            Map(offsetField -> r.get(offsetField)),
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