package com.eneco.trading.kafka.connect.nationalgrid.config

import java.util

import org.apache.kafka.common.config.ConfigDef.{Importance, Type}
import org.apache.kafka.common.config.{AbstractConfig, ConfigDef}

/**
  * Created by andrew@datamountaineer.com on 08/07/16. 
  * stream-reactor
  */
case class SOAPSourceConfig (props: util.Map[String, String])
  extends AbstractConfig(SOAPSourceConfig.config, props)

object RequestType extends Enumeration {
  type RequestType = Value
  val IFR, MIPI = Value
}

object SOAPSourceConfig {

  val IMPORT_QUERY_ROUTE = "connect.nationalgrid.kcql"
  val IMPORT_QUERY_ROUTE_DOC = "KCQL expression describing field selection and routes."

  val IFR_TOPIC = "connect.nationalgrid.irf.topic"
  val IFR_TOPIC_DEFAULT = "sys_nationalgrid_ifr_raw"
  val IFR_TOPIC_DOC = "The topic to write IFR requests to."
  val IFR_REQUESTS = "connect.nationalgrid.ifr.requests"
  val IFR_REQUESTS_DOC = "Comma separated list or request supported at IRF"

  val MIPI_TOPIC = "connect.nationalgrid.mipi.topic"
  val MIPI_TOPIC_DEFAULT = "sys_nationalgrid_mipi_raw"
  val MIPI_TOPIC_DOC = "The topic to write MIPI requests to."
  val MIPI_REQUESTS = "connect.nationalgrid.mipi.requests"
  val MIPI_REQUESTS_DOC = "Comma separated list or request supported at MIPI"

  val OFFSET_KEY = "nationalgrid"

  val REFRESH_RATE = "connect.national.grid.refresh.rate"
  val REFRESH_RATE_DEFAULT =  "PT12M"
  val REFRESH_RATE_DOC = "The refresh rate at which the source will check for publication of data in milliseconds, " +
    s"default is $REFRESH_RATE_DEFAULT"

  val config: ConfigDef = new ConfigDef()
      .define(IFR_REQUESTS, Type.LIST, List.empty ,Importance.HIGH, IFR_REQUESTS_DOC)
      .define(IFR_TOPIC, Type.STRING, IFR_TOPIC_DEFAULT, Importance.HIGH, IFR_TOPIC_DOC)
      .define(MIPI_REQUESTS, Type.LIST, List.empty, Importance.HIGH, MIPI_REQUESTS_DOC)
      .define(MIPI_TOPIC, Type.STRING, MIPI_TOPIC_DEFAULT, Importance.HIGH, MIPI_TOPIC_DOC)
      .define(REFRESH_RATE, Type.STRING, REFRESH_RATE_DEFAULT, Importance.MEDIUM, REFRESH_RATE_DOC)
}
