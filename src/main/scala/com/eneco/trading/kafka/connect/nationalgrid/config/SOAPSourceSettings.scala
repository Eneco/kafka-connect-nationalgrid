package com.eneco.trading.kafka.connect.nationalgrid.config


import scala.collection.JavaConverters._

/**
  * Created by andrew@datamountaineer.com on 08/07/16. 
  * stream-reactor
  */

case class SOAPSourceSettings(requests: Set[String], ifrTopic: String, mipiTopic: String, refreshRate : String)

object SOAPSourceSettings {
  def apply(config: SOAPSourceConfig): SOAPSourceSettings = {
    val requests = config.getList(SOAPSourceConfig.MIPI_REQUESTS)
    requests.addAll(config.getList(SOAPSourceConfig.IFR_REQUESTS))
    val refreshRate = config.getString(SOAPSourceConfig.REFRESH_RATE)
    val ifrTopic = config.getString(SOAPSourceConfig.IFR_TOPIC)
    val mipiTopic = config.getString(SOAPSourceConfig.MIPI_TOPIC)
    SOAPSourceSettings(requests.asScala.toSet, ifrTopic, mipiTopic, refreshRate)
  }
}
