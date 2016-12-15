package com.eneco.trading.kafka.connect.nationalgrid.source

import java.text.SimpleDateFormat
import java.time.{Duration, Instant}
import java.util.{Calendar, GregorianCalendar}
import javax.xml.datatype.{DatatypeFactory, XMLGregorianCalendar}

import com.eneco.trading.kafka.connect.nationalgrid.config.{NGSourceSettings, NGSourceSettings$, RequestType}
import com.eneco.trading.kafka.connect.nationalgrid.domain.{IFDRMessage, MIPIMessage}
import com.typesafe.scalalogging.StrictLogging
import nationalgrid.{ArrayOfCLSMIPIPublicationObjectBE, ArrayOfString, CLSRequestObject, GetPublicationDataWMResponse}
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.source.SourceRecord

import scala.concurrent._
import duration._
import scala.collection.JavaConversions._
import scala.collection.mutable

/**
  * Created by andrew@datamountaineer.com on 08/07/16. 
  * stream-reactor
  */

case class PullMap(dataItem: String, pubTimeHour: Int, pubTimeMinute: Int, frequency: Int)

object NGReader {
  def apply(settings: NGSourceSettings): NGReader = new NGReader(settings)
}

class NGReader(settings: NGSourceSettings) extends MIPIMessage with IFDRMessage with StrictLogging {
  logger.info("Initialising SOAP Reader")

  private val defaultTimestamp = "1900-01-01 00:00:00.0000000Z"
  private val dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'")
  private val offsetField = "publishedTime"
  private var ifrPubTracker = Instant.EPOCH
  private val refreshRate = Duration.parse(settings.refreshRate)
  val ifService = (new nationalgrid.InstantaneousFlowWebServiceSoap12Bindings with
    scalaxb.SoapClientsAsync with
    DispatchHttpClientsAsyncNG {}).service

  val mipiService = (new nationalgrid.PublicWebServiceSoap12Bindings with
    scalaxb.SoapClientsAsync with
    DispatchHttpClientsAsyncNG {}).service

  private val ifrTopic = settings.ifrTopic
  private val mipiTopic = settings.mipiTopic

  val dataItem = "NTS Physical Flows, Bacton, Interconnector"
  val mipiSchedule = Map(dataItem -> PullMap(dataItem, 0 , 8, 60))
  val offsets = mutable.Map(dataItem -> dateFormatter.parse(defaultTimestamp))

  def convertDate(cal: XMLGregorianCalendar) : Instant = cal.toGregorianCalendar().getTime().toInstant

  /**
    * Process and new NG feeds that are available
    *
    * */
  def process() : List[SourceRecord] = ???

  /**
    * Build a CLSRequest for MIPI
    *
    * @param date The gas day to build the request for
    * @param dataItem The data item to fetch
    * @return A CLS for the gas day and data item
    * */
  def buildCLSRequest(date: GregorianCalendar, dataItem: String) : CLSRequestObject = {
    val gasDay = DatatypeFactory.newInstance().newXMLGregorianCalendar(date)
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
    * Process an Instantaneous Flow Request
    *
    * Call the service and convert to source records
    * */
  def processIFD(): Seq[SourceRecord] = {
    val ifd = Await.result(ifService.getInstantaneousFlowData(), 30 seconds)
    val reports = ifd.GetInstantaneousFlowDataResult.getOrElse(None)

    val records = reports match {
      case None =>
        logger.warn("No reports found for IFR!")
        Seq.empty[Struct]
      case report : nationalgrid.EDPReportBE => Seq(processReport(report))
    }

    records
      .map(r => {
        new SourceRecord(Map("IFR"-> r.get("reportName")),
          Map(offsetField -> r.get(offsetField)),
          ifrTopic,
          r.schema(),
          r)
      })
  }

  /**
    * Process a MIPI request
    *
    * @param request The request to process
    * @return A sequence of source records
    * */
  def processMIPI(request : CLSRequestObject) : Seq[SourceRecord] = {
    logger.info(s"Sending request for ${request.PublicationObjectNameList.get} for Gas Day ${request.FromDate.toString}")

    val req = mipiService.getPublicationDataWM(Some(request))

    val mipiPDWMResp: GetPublicationDataWMResponse = Await.result(req, 60 seconds)
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
}