package com.eneco.trading.kafka.connect.nationalgrid.source

import scala.concurrent.duration.Duration
import scala.concurrent._, duration._
import scalaxb.HttpClientsAsync

/**
  * Created by andrew@datamountaineer.com on 13/07/16. 
  * kafka-connect-nationalgrid
  */
trait DispatchHttpClientsAsyncNG extends HttpClientsAsync {

  lazy val httpClient = new DispatchHttpClient {}
  // https://github.com/AsyncHttpClient/async-http-client/blob/1.9.x/src/main/java/com/ning/http/client/AsyncHttpClientConfigDefaults.java
  def requestTimeout: Duration = 60.seconds
  def connectionTimeout: Duration = 5.seconds

  trait DispatchHttpClient extends HttpClient {
    import dispatch._, Defaults._

    // Keep it lazy. See https://github.com/eed3si9n/scalaxb/pull/279
    lazy val http: Http = Http.configure(_.setCompressionEnabled(true)) //set compression

    def request(in: String, address: java.net.URI, headers: Map[String, String]): concurrent.Future[String] = {
      val req = url(address.toString).setBodyEncoding("UTF-8") <:< headers << in
      http(req > as.String)
    }
  }
}
