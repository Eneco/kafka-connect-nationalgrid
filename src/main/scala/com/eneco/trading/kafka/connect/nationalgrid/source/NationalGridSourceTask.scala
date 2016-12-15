package com.eneco.trading.kafka.connect.nationalgrid.source

import java.util
import java.util.{Timer, TimerTask}

import com.eneco.trading.kafka.connect.nationalgrid.config.{NGSourceConfig, NGSourceConfig$, NGSourceSettings, NGSourceSettings$}
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.connect.source.{SourceRecord, SourceTask}

import scala.collection.mutable
import scala.collection.JavaConversions._

/**
  * Created by andrew@datamountaineer.com on 13/12/2016. 
  * kafka-connect-nationalgrid
  */
class NationalGridSourceTask extends SourceTask with StrictLogging {
  private val timer = new Timer()
  private val counter = mutable.Map.empty[String, Long]
  private var reader : NGReader = _

  class LoggerTask extends TimerTask {
    override def run(): Unit = logCounts()
  }

  override def start(props: util.Map[String, String]): Unit = {
    logger.info(scala.io.Source.fromInputStream(getClass.getResourceAsStream("/nationalgrid-source-ascii.txt")).mkString)
    val config = NGSourceConfig(props)
    val settings = NGSourceSettings(config)
    reader = NGReader(settings)
    timer.schedule(new LoggerTask, 0, 60000)
  }

  override def stop(): Unit = {}

  override def poll(): util.List[SourceRecord] = {
    reader.process()
  }

  override def version(): String = "1"

  def logCounts(): mutable.Map[String, Long] = {
    counter.foreach( { case (k,v) => logger.info(s"Delivered $v records for $k.") })
    counter.empty
  }

}
