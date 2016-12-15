package com.eneco.trading.kafka.connect.nationalgrid.source

import com.eneco.trading.kafka.connect.nationalgrid.TestConfig
import com.eneco.trading.kafka.connect.nationalgrid.config.{NGSourceConfig, NGSourceSettings}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

/**
  * Created by andrew@datamountaineer.com on 08/07/16. 
  * stream-reactor
  */
class TestNGReader extends WordSpec with Matchers with BeforeAndAfter with MockitoSugar with TestConfig{

  "should read from SOAP API" in {
    //val taskContext = getSourceTaskContextDefault()
    val props = getProps
    val config = new NGSourceConfig(props)
    val settings = NGSourceSettings(config)
    val reader = new NGReader(settings)
  }
}
