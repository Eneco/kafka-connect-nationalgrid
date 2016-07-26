package com.eneco.trading.kafka.connect.nationalgrid.config

import com.datamountaineer.connector.config.Config
import com.eneco.trading.kafka.connect.nationalgrid.config.RequestType.RequestType
import org.apache.kafka.connect.errors.ConnectException

import scala.util.{Failure, Success, Try}
import scala.xml.Elem
/**
  * Created by andrew@datamountaineer.com on 08/07/16. 
  * stream-reactor
  */

case class SOAPSourceSettings(requests: Set[RequestType],
                              route : Map[RequestType, String],
                              refreshRate : String)

object SOAPSourceSettings {
  def apply(config: SOAPSourceConfig): SOAPSourceSettings = {
    val raw = config.getString(SOAPSourceConfig.IMPORT_QUERY_ROUTE)
    val routes = raw.split(";").map(r => Config.parse(r)).toSet

    Try(routes.map(r => RequestType.withName(r.getSource))) match {
      case Success(s) => s
      case Failure(f) => new ConnectException("Unknown RequestType in SELECT", f)
    }

    val requestMap = routes.map(r => RequestType.withName(r.getSource))
    val requestRoute = routes.map(r => (RequestType.withName(r.getSource), r.getTarget)).toMap
    val refreshRate = config.getString(SOAPSourceConfig.REFRESH_RATE)
    SOAPSourceSettings(requestMap, requestRoute, refreshRate)
  }
}
