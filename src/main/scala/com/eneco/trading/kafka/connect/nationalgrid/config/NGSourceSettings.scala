package com.eneco.trading.kafka.connect.nationalgrid.config


import com.eneco.trading.kafka.connect.nationalgrid.domain.PullMap

import scala.collection.JavaConverters._

/**
  * Created by andrew@datamountaineer.com on 08/07/16. 
  * stream-reactor
  */

case class NGSourceSettings(ifrRequests: Set[String], ifrTopic: String, mipiRequests: Set[PullMap], mipiTopic: String)

object NGSourceSettings {
  def apply(config: NGSourceConfig): NGSourceSettings = {
    val ifrRequets = config.getList(NGSourceConfig.IFR_REQUESTS)
    val mipiRequestsRaw = config.getString(NGSourceConfig.MIPI_REQUESTS).split('|')

    val ifrTopic = config.getString(NGSourceConfig.IFR_TOPIC)
    val mipiTopic = config.getString(NGSourceConfig.MIPI_TOPIC)
    val mipiRequests  = mipiRequestsRaw
                        .map(mipi => mipi.split(";"))
                        .map(m => {
                          val hourMin = m(1).split(":")
                          PullMap(m(0), hourMin(0).toInt, hourMin(1).toInt, m(2).toInt)
                        }).toSet
    NGSourceSettings(ifrRequets.asScala.toSet, ifrTopic, mipiRequests, mipiTopic)
  }
}