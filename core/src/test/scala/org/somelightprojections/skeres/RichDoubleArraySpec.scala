package org.somelightprojections.skeres

import com.google.ceres.DoubleArray
import org.scalatest.{MustMatchers, WordSpec}

class RichDoubleArraySpec extends WordSpec with MustMatchers {
  "RichDoubleArray" should {
    "manipulate DoubleArray with implicit conversion to/from DoublePointer" in {
      val buffer = new DoubleArray(10)
      for (i <- 0 until 10) {
        buffer.setitem(i, i)
      }
      val rich = RichDoubleArray(buffer)
      for (i <- 0 until 10) {
        rich.get(i) must be(buffer.get(i))
        buffer.set(i, 2 * i)
        buffer.getitem(i) must be(2 * i)
      }
    }
  }
}
