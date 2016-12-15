package com.eneco.trading.kafka.connect.nationalgrid.config


import scala.collection.JavaConverters._

/**
  * Created by andrew@datamountaineer.com on 08/07/16. 
  * stream-reactor
  */

case class NGSourceSettings(ifrRequests: Set[String], ifrTopic: String, mipiRequests: Set[String], mipiTopic: String,
                            refreshRate : String)

object NGSourceSettings {
  def apply(config: NGSourceConfig): NGSourceSettings = {
    val ifrRequets = config.getList(NGSourceConfig.IFR_REQUESTS)
    val mipiRequests = config.getList(NGSourceConfig.MIPI_REQUESTS)
    val refreshRate = config.getString(NGSourceConfig.REFRESH_RATE)
    val ifrTopic = config.getString(NGSourceConfig.IFR_TOPIC)
    val mipiTopic = config.getString(NGSourceConfig.MIPI_TOPIC)
    NGSourceSettings(ifrRequets.asScala.toSet, ifrTopic, mipiRequests.asScala.toSet, mipiTopic, refreshRate)
  }
}
