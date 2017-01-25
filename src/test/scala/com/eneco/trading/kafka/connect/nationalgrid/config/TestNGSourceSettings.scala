package com.eneco.trading.kafka.connect.nationalgrid.config

import com.eneco.trading.kafka.connect.nationalgrid.TestConfig
import org.scalatest.{Matchers, WordSpec}

class TestNGSourceSettings extends WordSpec with TestConfig with Matchers {
  "should test SourceSettings" in {
    val config = NGSourceConfig(getProps)
    val settings = NGSourceSettings(config)
    settings.ifrTopic shouldBe IFR_TOPIC
    settings.mipiTopic shouldBe MIPI_TOPIC
    settings.mipiRequests.contains(pullMap) shouldBe true
  }
}
