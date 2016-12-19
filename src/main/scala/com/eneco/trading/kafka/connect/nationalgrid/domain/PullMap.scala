package com.eneco.trading.kafka.connect.nationalgrid.domain

/**
  * Created by andrew@datamountaineer.com on 19/12/2016. 
  * kafka-connect-nationalgrid
  */
case class PullMap(dataItem: String, pubHour: Int, pubMin: Int, frequency: Int)