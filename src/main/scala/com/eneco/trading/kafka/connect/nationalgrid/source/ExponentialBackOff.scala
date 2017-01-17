package com.eneco.trading.kafka.connect.nationalgrid.source

import java.time.{Duration, Instant}

/**
  * Created by andrew@datamountaineer.com on 17/01/2017. 
  * kafka-connect-nationalgrid
  */
class ExponentialBackOff(step: Duration, cap: Duration, iteration: Int = 0) {
  val endTime = Instant.now.plus(interval(iteration))

  private def interval(i: Int) = Duration.ofMillis(
    Math.min(
      cap.toMillis,
      step.toMillis * Math.pow(2, i).toLong
    )
  )

  def remaining: Duration = Duration.between(Instant.now, endTime)

  def passed: Boolean = Instant.now.isAfter(this.endTime)

  def nextSuccess(): ExponentialBackOff = new ExponentialBackOff(step, cap)

  def nextFailure(): ExponentialBackOff = new ExponentialBackOff(step, cap, iteration + 1)
}
