package com.eneco.trading.kafka.connect.nationalgrid.source

import com.eneco.trading.kafka.connect.nationalgrid.TestConfig
import com.eneco.trading.kafka.connect.nationalgrid.config.{SOAPSourceConfig, SOAPSourceSettings}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

/**
  * Created by andrew@datamountaineer.com on 08/07/16. 
  * stream-reactor
  */
class TestSOAPReader extends WordSpec with Matchers with BeforeAndAfter with MockitoSugar with TestConfig{

  "should read from SOAP API" in {
    //val taskContext = getSourceTaskContextDefault()
    val props = getProps
    val config = new SOAPSourceConfig(props)
    val settings = SOAPSourceSettings(config)
    val reader = new SOAPReader(settings)
    reader.send()
  }
}
