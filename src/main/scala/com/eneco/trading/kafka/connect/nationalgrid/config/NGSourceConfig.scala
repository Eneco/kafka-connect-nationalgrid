package com.eneco.trading.kafka.connect.nationalgrid.config

import java.util

import org.apache.kafka.common.config.ConfigDef.{Importance, Type}
import org.apache.kafka.common.config.{AbstractConfig, ConfigDef}

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

  val MAX_BACK_OFF = "connect.nationalgrid.max.backoff"
  val MAX_BACK_OFF_DEFAULT = "PT40M"
  val MAX_BACK_OFF_DOC = "On failure, exponentially backoff to at most this ISO8601 duration"

  val REFRESH_RATE = "connect.nationalgrid.refresh"
  val REFRESH_RATE_DEFAULT = "PT5M"
  val REFRESH_RATE_DOC = "How often the ftp server is polled; ISO8601 duration"

  val HISTORIC_FETCH = "connect.nationalgrid.historic.fetch"
  val HISTORIC_FETCH_DEFAULT = 1
  val HISTORIC_FETCH_DOC = "The number of historical days to fetch on start with no offsets from the current day."

  val config: ConfigDef = new ConfigDef()
      .define(IFR_TOPIC, Type.STRING, IFR_TOPIC_DEFAULT, Importance.HIGH, IFR_TOPIC_DOC)
      .define(MIPI_REQUESTS, Type.STRING, Importance.HIGH, MIPI_REQUESTS_DOC)
      .define(MIPI_TOPIC, Type.STRING, MIPI_TOPIC_DEFAULT, Importance.HIGH, MIPI_TOPIC_DOC)
      .define(MAX_BACK_OFF, Type.STRING, MAX_BACK_OFF_DEFAULT , Importance.HIGH, MAX_BACK_OFF_DOC)
      .define(REFRESH_RATE, Type.STRING, REFRESH_RATE_DEFAULT , Importance.HIGH, REFRESH_RATE_DOC)
      .define(HISTORIC_FETCH, Type.INT, HISTORIC_FETCH_DEFAULT , Importance.LOW, HISTORIC_FETCH_DOC)
}
