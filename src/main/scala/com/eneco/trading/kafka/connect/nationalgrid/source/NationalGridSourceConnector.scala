package com.eneco.trading.kafka.connect.nationalgrid.source

import java.util

import com.eneco.trading.kafka.connect.nationalgrid.config.{NGSourceConfig, NGSourceConfig$}
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.source.SourceConnector

import scala.collection.JavaConversions._

/**
  * Created by andrew@datamountaineer.com on 13/12/2016. 
  * kafka-connect-nationalgrid
  */
class NationalGridSourceConnector extends SourceConnector {
  private var configProps: util.Map[String, String] = _
  private val configDef = NGSourceConfig.config

  override def start(props: util.Map[String, String]): Unit = {
    configProps = props
  }

  override def taskClass(): Class[_ <: Task] = classOf[NationalGridSourceTask]

  override def taskConfigs(i: Int): util.List[util.Map[String, String]] = {
    List(configProps)
  }

  override def stop(): Unit = {}
  override def config(): ConfigDef = configDef
  override def version(): String = "1"
}
