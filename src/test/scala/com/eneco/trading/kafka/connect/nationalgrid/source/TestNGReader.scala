package com.eneco.trading.kafka.connect.nationalgrid.source

import java.util
import java.util.{Calendar, GregorianCalendar}

import com.eneco.trading.kafka.connect.nationalgrid.TestConfig
import com.eneco.trading.kafka.connect.nationalgrid.config.{NGSourceConfig, NGSourceSettings}
import com.eneco.trading.kafka.connect.nationalgrid.domain.IFDRMessage
import org.apache.kafka.connect.data.Field
import org.apache.kafka.connect.source.SourceTaskContext
import org.joda.time.DateTime
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

/**
  * Created by andrew@datamountaineer.com on 08/07/16. 
  * stream-reactor
  */
class TestNGReader extends WordSpec with Matchers with BeforeAndAfter with MockitoSugar with TestConfig with IFDRMessage {

  val sourceContext = getSourceTaskContext(pullMap.dataItem, pullMap.dataItem, NGSourceConfig.OFFSET_FIELD, OFFSET_DEFAULT)
  val props = getProps
  val config = new NGSourceConfig(props)
  val settings = NGSourceSettings(config)
  val reader = NGReader(settings, sourceContext)

  "should get stored offsets from offset storage" in {
    val offsets = reader.offsetMap(pullMap.dataItem)
    offsets.toDateTime() shouldBe DATE_FORMATTER.parseDateTime(OFFSET_DEFAULT)
  }

  "should pull data" in {
    reader.offsetMap(pullMap.dataItem) -> DATE_FORMATTER.parseDateTime(OFFSET_DEFAULT)
    reader.pull(pullMap.dataItem) shouldBe true
  }

  "should not pull data" in {
    reader.offsetMap(pullMap.dataItem) = DateTime.now.plusMinutes(50)
    reader.pull(pullMap.dataItem) shouldBe false
  }

  "should not read from IFD message" in {
    val now = new GregorianCalendar()
    now.add(Calendar.HOUR, 24)
    reader.ifrPubTracker =  Some(now)
    val records = reader.processIFD()
    records.size shouldBe 0
  }

  "should read all types" in {
    val sourceContext = getSourceTaskContext(pullMap.dataItem, pullMap.dataItem, NGSourceConfig.OFFSET_FIELD, OFFSET_DEFAULT)
    val props = getProps
    val config = new NGSourceConfig(props)
    val settings = NGSourceSettings(config)
    val reader = NGReader(settings, sourceContext)
    reader.offsetMap(pullMap.dataItem) -> DATE_FORMATTER.parseDateTime(OFFSET_DEFAULT)
    val records = reader.process()
    records.size should be > 0
    val records2 = reader.process()
    records2.size shouldBe 0
  }
}
