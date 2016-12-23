package com.eneco.trading.kafka.connect.nationalgrid.config

import java.util

import org.apache.kafka.common.config.ConfigDef.{Importance, Type}
import org.apache.kafka.common.config.{AbstractConfig, ConfigDef}

/**
  * Created by andrew@datamountaineer.com on 08/07/16. 
  * stream-reactor
  */
case class NGSourceConfig(props: util.Map[String, String])
  extends AbstractConfig(NGSourceConfig.config, props)

object RequestType extends Enumeration {
  type RequestType = Value
  val IFR, MIPI = Value
}

object NGSourceConfig {

  val DEFAULT_OFFSET_TIMESTAMP = "1900-01-01T00:00:00.000+01:00"
  val DEFAULT_OFFSET_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
  val DATA_ITEM = "DATA_ITEM"
  val OFFSET_FIELD = "publishedTime"
  val IFR_TOPIC = "connect.nationalgrid.irf.topic"
  val IFR_TOPIC_DEFAULT = "sys_nationalgrid_ifr_raw"
  val IFR_TOPIC_DOC = "The topic to write IFR requests to."

  val MIPI_TOPIC = "connect.nationalgrid.mipi.topic"
  val MIPI_TOPIC_DEFAULT = "sys_nationalgrid_mipi_raw"
  val MIPI_TOPIC_DOC = "The topic to write MIPI requests to."
  val MIPI_REQUESTS = "connect.nationalgrid.mipi.requests"
  val MIPI_REQUESTS_DOC = "Pipe separated list or request supported at MIPI"

  val config: ConfigDef = new ConfigDef()
      .define(IFR_TOPIC, Type.STRING, IFR_TOPIC_DEFAULT, Importance.HIGH, IFR_TOPIC_DOC)
      .define(MIPI_REQUESTS, Type.STRING, Importance.HIGH, MIPI_REQUESTS_DOC)
      .define(MIPI_TOPIC, Type.STRING, MIPI_TOPIC_DEFAULT, Importance.HIGH, MIPI_TOPIC_DOC)
}
