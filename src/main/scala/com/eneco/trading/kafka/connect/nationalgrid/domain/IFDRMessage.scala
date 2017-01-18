package com.eneco.trading.kafka.connect.nationalgrid.domain

import com.typesafe.scalalogging.slf4j.StrictLogging
import nationalgrid.{EDPEnergyGraphTableBE, EDPObjectBE, EDPReportBE}
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}

import scala.collection.JavaConverters._

trait IFDRMessage extends StrictLogging
{
  val EDPEnergyDataBESchema: Schema = SchemaBuilder.struct().name("EDPEnergyDataBE")
    .field("applicableAt", Schema.STRING_SCHEMA)
    .field("flowRate", Schema.FLOAT64_SCHEMA)
    .field("qualityIndicator", Schema.STRING_SCHEMA)
    .field("amendedTimeStamp", Schema.STRING_SCHEMA)
    .field("scheduleTime", Schema.STRING_SCHEMA)
    .build()

  val EDPObjectBESchema: Schema = SchemaBuilder.struct().name("EDPObjectBE")
    .field("eDPObjectName", Schema.STRING_SCHEMA)
    .field("eDPObjectId", Schema.STRING_SCHEMA)
    .field("eDPObjectType", Schema.STRING_SCHEMA)
    .field("energyDataList", SchemaBuilder.array(EDPEnergyDataBESchema))
    .build()

  val EDPEnergyGraphTableBESchema: Schema = SchemaBuilder.struct().name("EDPEnergyGraphTableBE")
    .field("eDPEnergyGraphTableName", Schema.STRING_SCHEMA)
    .field("itemPosition", Schema.INT32_SCHEMA)
    .field("description", Schema.STRING_SCHEMA)
    .field("eDPObjectCollection", SchemaBuilder.array(EDPObjectBESchema))
    .build()


  val EDPReportPageSchema: Schema = SchemaBuilder.struct().name("EDPReportPage")
    .field("pageName", Schema.STRING_SCHEMA)
    .field("currentGasDay", Schema.STRING_SCHEMA)
    .field("eDPEnergyGraphTableCollection", SchemaBuilder.array(EDPEnergyGraphTableBESchema))
    .build()

  val EDPReportSchema: Schema = SchemaBuilder.struct().name("EDReport")
    .field("reportName", Schema.STRING_SCHEMA)
    .field("publishedTime", Schema.STRING_SCHEMA)
    .field("eDPReportPage", EDPReportPageSchema)
    .build()


  def BuildEDPReportBE(reportName: Option[String],
                       publishedTime: javax.xml.datatype.XMLGregorianCalendar,
                       eDPReportPage: Struct) : Struct = {
    new Struct(EDPReportSchema)
        .put("reportName", reportName.getOrElse(""))
        .put("publishedTime", publishedTime.toString)
        .put("eDPReportPage", eDPReportPage)
  }

  def BuildEDPReportPage(pageName: Option[String],
                         currentGasDay: javax.xml.datatype.XMLGregorianCalendar,
                         eDPEnergyGraphTableCollection: Seq[Struct]) : Struct = {
    new Struct(EDPReportPageSchema)
        .put("pageName", pageName.getOrElse(""))
        .put("currentGasDay", currentGasDay.toString)
        .put("eDPEnergyGraphTableCollection", eDPEnergyGraphTableCollection.asJava)
  }

  def BuildEDPEnergyGraphTableBE(eDPEnergyGraphTableName: Option[String],
                                  itemPosition: Int,
                                  eDPObjectCollection: Seq[Struct],
                                  description: Option[String]) : Struct = {
    new Struct(EDPEnergyGraphTableBESchema)
      .put("eDPEnergyGraphTableName", eDPEnergyGraphTableName.getOrElse(""))
      .put("itemPosition", itemPosition)
      .put("eDPObjectCollection", eDPObjectCollection.asJava)
      .put("description", description.getOrElse(""))
  }

  def BuildEDPObjectBE(eDPObjectName: Option[String],
                       eDPObjectType: Option[String],
                       eDPObjectID: Option[String],
                       energyDataList: Seq[Struct]) : Struct = {
    logger.info(s"Received record for ObjectName ${eDPObjectName.getOrElse("")}")

    new Struct(EDPObjectBESchema)
      .put("eDPObjectName", eDPObjectName.getOrElse(""))
      .put("eDPObjectId", eDPObjectID.getOrElse(""))
      .put("eDPObjectType", eDPObjectType.getOrElse(""))
      .put("energyDataList", energyDataList.asJava)
  }

  def BuildEDPEnergyDataBE(applicableAt: javax.xml.datatype.XMLGregorianCalendar,
                           flowRate: Double,
                           qualityIndicator: Option[String],
                           amendedTimeStamp: Option[String],
                           scheduleTime: javax.xml.datatype.XMLGregorianCalendar) : Struct = {
    new Struct(EDPEnergyDataBESchema)
      .put("applicableAt", applicableAt.toString)
      .put("flowRate", flowRate)
      .put("qualityIndicator", qualityIndicator.getOrElse(""))
      .put("amendedTimeStamp", amendedTimeStamp.getOrElse(""))
      .put("scheduleTime", scheduleTime.toString)
  }

  def processReport(r : EDPReportBE) : Struct = {
    processReportPage(r).map( rp => BuildEDPReportBE(r.ReportName, r.PublishedTime, rp)).get
  }

  def processReportPage(r : EDPReportBE) : Option[Struct] = {
    r.EDPReportPage.map(rp => {
      val egtc = rp.EDPEnergyGraphTableCollection.get
      val gtbe: Seq[EDPEnergyGraphTableBE] = egtc.EDPEnergyGraphTableBE.flatten
      val egtcStruct = processGraphTable(gtbe)
      BuildEDPReportPage(rp.PageName, rp.CurrentGasDay, egtcStruct)
    })
  }

  def processGraphTable(gtbe: Seq[EDPEnergyGraphTableBE]) : Seq[Struct] = {
    gtbe.map(tbe => {
      val aobe = tbe.EDPObjectCollection.get
      val obe = aobe.EDPObjectBE.flatten
      val beoStruct = processEBPObjectBE(obe)
      BuildEDPEnergyGraphTableBE(tbe.EDPEnergyGraphTableName, tbe.ItemPosition, beoStruct, tbe.Description)
    })
  }

  def processEBPObjectBE(obe: Seq[EDPObjectBE]) : Seq[Struct] = {
    obe.map(be => {
      val edl = be.EnergyDataList.get
      val data = edl.EDPEnergyDataBE.flatten
      val structDataBE = data.map(d => {
        BuildEDPEnergyDataBE(d.ApplicableAt, d.FlowRate, d.QualityIndicator,
          d.AmendedTimeStamp, d.ScheduleTime)
      })
      BuildEDPObjectBE(be.EDPObjectName, be.EDPObjectType, be.EDPObjectID, structDataBE)
    })
  }
}