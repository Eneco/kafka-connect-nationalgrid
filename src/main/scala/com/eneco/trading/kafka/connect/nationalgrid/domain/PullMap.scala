package com.eneco.trading.kafka.connect.nationalgrid.domain

case class PullMap(dataItem: String, pubHour: Int, pubMin: Int, frequency: Int)