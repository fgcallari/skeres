package org.somelightprojections.skeres

import scala.reflect.ClassTag
import scala.{specialized => sp}
import spire.algebra.{Order, NRoot, Trig, Field}

// Autodifferentiable cost functor
abstract class CostFunctor(val kNumResiduals: Int, val N: Int*) {
  require(kNumResiduals > 0, s"Nonpositive number of residuals specified: $kNumResiduals")
  require(N.forall(_ > 0), s"Nonpositive parameter block sizes specified: ${N.mkString(", ")}")

  def toAutodiffCostFunction = AutodiffCostFunction(this)

  def apply[@sp(Double) T: Field: Trig: NRoot: Order: ClassTag](x: Array[T]*): Array[T]
}
