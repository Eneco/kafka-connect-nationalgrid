package com.eneco.trading.kafka.connect.nationalgrid.config

import java.util

import org.apache.kafka.common.config.ConfigDef.{Importance, Type}
import org.apache.kafka.common.config.{AbstractConfig, ConfigDef}

/**
  * Created by andrew@datamountaineer.com on 08/07/16. 
  * stream-reactor
  */
class SOAPSourceConfig (props: util.Map[String, String])
  extends AbstractConfig(SOAPSourceConfig.config, props)

object RequestType extends Enumeration {
  type RequestType = Value
  val IFR, MIPI = Value
}

object SOAPSourceConfig {

  val IMPORT_QUERY_ROUTE = "connect.nationalgrid.import.route.query"
  val IMPORT_QUERY_ROUTE_DOC = "KCQL expression describing field selection and routes."

  val REQUESTS = "connect.nationalgrid.requests"
  val REQUESTS_DOC = "Comma separated list or request supported at IFR, blah"

  val OFFSET_KEY = "nationalgrid"

  val REFRESH_RATE = "connect.national.grid.refresh.rate"
  val REFRESH_RATE_DEFAULT =  "PT12M"
  val REFRESH_RATE_DOC = "The refresh rate at which the source will check for publication of data in milliseconds, " +
    s"default is $REFRESH_RATE_DEFAULT"

  val config: ConfigDef = new ConfigDef()
    .define(IMPORT_QUERY_ROUTE, Type.STRING, Importance.HIGH, IMPORT_QUERY_ROUTE_DOC)
    .define(REFRESH_RATE, Type.STRING, REFRESH_RATE_DEFAULT, Importance.MEDIUM, REFRESH_RATE_DOC)
}
