package com.eneco.trading.kafka.connect.nationalgrid.domain

import javax.xml.datatype.XMLGregorianCalendar

import nationalgrid.{ArrayOfCLSMIPIPublicationObjectBE, ArrayOfCLSPublicationObjectDataBE, CLSPublicationObjectDataBE}
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}

trait MIPIMessage {
  val CLSPublicationObjectDataBESchema: SchemaBuilder = SchemaBuilder.struct().name("CLSPublicationObjectDataBE").version(1)
    .field("dataItemName", Schema.STRING_SCHEMA)
    .field("applicableAt", Schema.STRING_SCHEMA)
    .field("applicableFor", Schema.STRING_SCHEMA)
    .field("value", Schema.OPTIONAL_STRING_SCHEMA)
    .field("generatedTimeStamp", Schema.STRING_SCHEMA)
    .field("qualityIndicator", Schema.OPTIONAL_STRING_SCHEMA)
    .field("substituted", Schema.OPTIONAL_STRING_SCHEMA)
    .field("createdDate", Schema.STRING_SCHEMA)

  def BuildCLSPublicationObjectDataBE(dataItemName: Option[String],
                                      applicableAt: XMLGregorianCalendar,
                                      applicableFor: XMLGregorianCalendar,
                                      value: Option[String],
                                      generatedTimeStamp: XMLGregorianCalendar,
                                      substituted: Option[String],
                                      createdDate: XMLGregorianCalendar) : Struct = {
    new Struct(CLSPublicationObjectDataBESchema)
        .put("dataItemName", dataItemName.getOrElse(""))
        .put("applicableAt", applicableAt.toString)
        .put("applicableFor", applicableFor.toString)
        .put("value", value.get.toString)
        .put("generatedTimeStamp", generatedTimeStamp.toString)
        .put("substituted", substituted.get.toString)
        .put("createdDate", createdDate.toString)
  }

  def processMIPIReport(r : ArrayOfCLSMIPIPublicationObjectBE): Seq[Struct] = {
    val pubObjectBe = r.CLSMIPIPublicationObjectBE.flatten
    pubObjectBe.flatMap(o => processCLSPublicationObjectDataBE(o.PublicationObjectName, o.PublicationObjectData))
  }

  def processCLSPublicationObjectDataBE(name: Option[String], cLSPublicationObjectDataBE: Option[ArrayOfCLSPublicationObjectDataBE]) : Seq[Struct] = {
    val x = cLSPublicationObjectDataBE.map(p => p.CLSPublicationObjectDataBE.flatten).get
    x.map(y => processPubObjectBE(name, y))
  }

  def processPubObjectBE(name : Option[String], cLSPublicationObjectDataBE: CLSPublicationObjectDataBE ): Struct = {
      BuildCLSPublicationObjectDataBE(
        name,
        cLSPublicationObjectDataBE.ApplicableAt,
        cLSPublicationObjectDataBE.ApplicableFor,
        cLSPublicationObjectDataBE.Value,
        cLSPublicationObjectDataBE.GeneratedTimeStamp,
        cLSPublicationObjectDataBE.Substituted,
        cLSPublicationObjectDataBE.CreatedDate)
  }
}
