package org.somelightprojections.skeres

import spire.math._

object TestUtil {
  def expectClose(x: Double, y: Double, maxAbsRelativeDifference: Double): Boolean = {
    val absoluteDifference = abs(x - y)
    val relativeDifference =
      if (x == 0.0 || y == 0.0) {
        // If x or y is exactly zero, then relative difference doesn't have any
        // meaning. Take the absolute difference instead.
        absoluteDifference
      } else {
        absoluteDifference / max(abs(x), abs(y))
      }
    if (relativeDifference > maxAbsRelativeDifference) {
      println("x=%17g y=%17g abs=%17g rel=%17g".format(
        x, y, absoluteDifference, relativeDifference)
      )
    }
    relativeDifference <= maxAbsRelativeDifference
  }
}
