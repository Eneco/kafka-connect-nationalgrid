package com.eneco.trading.kafka.connect.nationalgrid.source

import java.time.{Duration, Instant}

class ExponentialBackOff(step: Duration, cap: Duration, iteration: Int = 0, now: () => Instant = Instant.now) {
  val endTime = now().plus(exponentialInterval(iteration))

  def remaining: Duration = Duration.between(now(), endTime)

  def passed: Boolean = now().isAfter(this.endTime)

  def nextSuccess(): ExponentialBackOff = new ExponentialBackOff(step, cap, 0, now)

  def nextFailure(): ExponentialBackOff = new ExponentialBackOff(step, cap, iteration + 1, now)

  private def exponentialInterval(i: Int) = Duration.ofMillis(
    Math.min(
      cap.toMillis,
      step.toMillis * Math.pow(2, i).toLong
    )
  )
}

