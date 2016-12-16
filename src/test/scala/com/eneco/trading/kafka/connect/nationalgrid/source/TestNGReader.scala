package com.eneco.trading.kafka.connect.nationalgrid.source

import java.util
import javax.xml.datatype.DatatypeFactory

import com.eneco.trading.kafka.connect.nationalgrid.TestConfig
import com.eneco.trading.kafka.connect.nationalgrid.config.{NGSourceConfig, NGSourceSettings}
import com.eneco.trading.kafka.connect.nationalgrid.domain.IFDRMessage
import org.apache.kafka.connect.data.{Field, Schema}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

/**
  * Created by andrew@datamountaineer.com on 08/07/16. 
  * stream-reactor
  */
class TestNGReader extends WordSpec with Matchers with BeforeAndAfter with MockitoSugar with TestConfig with IFDRMessage {

  "should read from IFD message" in {
    val props = getProps
    val config = new NGSourceConfig(props)
    val settings = NGSourceSettings(config)
    val reader = new NGReader(settings)
    val records = reader.processIFD()
    records.size should be > 0
    records.head.topic() shouldBe IFR_TOPIC
    val fields: util.List[Field] = records.head.valueSchema().fields()
    fields.get(0).name() shouldBe "reportName"
    fields.get(1).name() shouldBe "publishedTime"
    fields.get(2).name() shouldBe "eDPReportPage"
  }

  "should not read from IFD message" in {
    val props = getProps
    val config = new NGSourceConfig(props)
    val settings = NGSourceSettings(config)
    val reader = new NGReader(settings)
    val now = DatatypeFactory.newInstance().newXMLGregorianCalendar()
    now.setDay(2)
    reader.ifrPubTracker =  Some(now.toGregorianCalendar)
    val records = reader.processIFD()
    records.size shouldBe 0
  }
}
