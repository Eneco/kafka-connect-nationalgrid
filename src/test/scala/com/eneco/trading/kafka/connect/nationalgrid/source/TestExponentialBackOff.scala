package com.eneco.trading.kafka.connect.nationalgrid.source

import java.time.{Duration, Instant}

import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

class TestExponentialBackOff extends WordSpec with Matchers with BeforeAndAfter with MockitoSugar {
  val myStep = Duration.ofMinutes(6)
  val myCap = Duration.ofMinutes(30)
  val fixedNow = Instant.now()
  val fixedTime = () => fixedNow
  val backoff = new ExponentialBackOff(myStep, myCap, 0, fixedTime)

  "initial step should work" in {
    backoff.remaining shouldEqual myStep
    backoff.endTime shouldEqual fixedNow.plus(myStep)
  }

  "passed works" in {
    var fixedNow = Instant.now()
    val fixedTime = () => fixedNow
    val backoff = new ExponentialBackOff(myStep, myCap, 0, fixedTime)
    backoff.passed shouldBe false
    fixedNow = fixedNow.plus(myStep.dividedBy(2)) //halfway
    backoff.passed shouldBe false
    fixedNow = fixedNow.plus(myStep)
    backoff.passed shouldBe true
  }

  "remaining works" in {
    backoff.remaining shouldEqual myStep
  }

  "success step is constant" in {
    val next = backoff.nextSuccess()
    next.remaining shouldEqual myStep
    backoff.endTime shouldEqual fixedNow.plus(myStep)
  }

  "failure step grows exponentially" in {
    val twoStep = myStep.multipliedBy(2)
    val firstFailure = backoff.nextFailure()
    firstFailure.remaining shouldEqual twoStep
    firstFailure.endTime shouldEqual fixedNow.plus(twoStep)

    val fourStep = twoStep.multipliedBy(2)
    val secondFailure = firstFailure.nextFailure()
    secondFailure.remaining shouldEqual fourStep
    secondFailure.endTime shouldEqual fixedNow.plus(fourStep)
  }

  "failure step is capped to a max" in {
    val thirdFailure = backoff.nextFailure().nextFailure().nextFailure()
    thirdFailure.remaining shouldEqual myCap
    thirdFailure.endTime shouldEqual fixedNow.plus(myCap)

    val fourthFailure = thirdFailure.nextFailure()
    fourthFailure.remaining shouldEqual myCap
    fourthFailure.endTime shouldEqual fixedNow.plus(myCap)
  }
}
