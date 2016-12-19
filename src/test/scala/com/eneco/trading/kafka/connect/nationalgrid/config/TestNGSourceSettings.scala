package com.eneco.trading.kafka.connect.nationalgrid.config

import com.eneco.trading.kafka.connect.nationalgrid.TestConfig
import org.scalatest.{Matchers, WordSpec}

/**
  * Created by andrew@datamountaineer.com on 15/12/2016. 
  * kafka-connect-nationalgrid
  */
class TestNGSourceSettings extends WordSpec with TestConfig with Matchers {
  "should test SourceSettings" in {
    val config = NGSourceConfig(getProps)
    val settings = NGSourceSettings(config)
    settings.ifrTopic shouldBe IFR_TOPIC
    settings.mipiTopic shouldBe MIPI_TOPIC
    settings.mipiRequests.head shouldBe pullMap
  }
}
