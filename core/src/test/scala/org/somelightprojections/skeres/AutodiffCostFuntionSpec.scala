package org.somelightprojections.skeres

import com.google.ceres.{DoubleArray, StdVectorDoublePointer, StdVectorDouble}
import org.scalatest.{MustMatchers, WordSpec}
import scala.reflect.ClassTag
import scala.{specialized => sp}
import spire.algebra._
import spire.implicits._
import spire.math._

class AutodiffCostFuntionSpec extends WordSpec with MustMatchers {
  case class BinaryScalarCost(a: Double) extends CostFunctor(1, 2, 2) {
    def apply[@sp(Double) T: Field : Trig : NRoot : Order : ClassTag](p: Array[T]*): Array[T] = {
      require(p.length == 2)
      require(p(0).length == 2)
      require(p(1).length == 2)
      val x = p(0)
      val y = p(1)
      val z = x(0) * y(0) + x(1) * y(1) - a
      Array(z)
    }
  }

  "AutodiffCostFunction" should {
    "evaluate cost and jacobians on a bilinear cost function" in {
      val parametersVector = new StdVectorDoublePointer()
      parametersVector.add(new DoubleArray(2).toPointer)
      parametersVector.add(new DoubleArray(2).toPointer)
      val parameters: DoublePointerPointer = RichDoubleMatrix.fromStdVector(parametersVector)
      parameters.set(0, 0, 1.0)
      parameters.set(0, 1, 2.0)
      parameters.set(1, 0, 3.0)
      parameters.set(1, 1, 4.0)

      val nullJacobians = new DoublePointerPointer {}
      val residuals: DoublePointer = new DoubleArray(1).toPointer

      val costFunction = BinaryScalarCost(1.0).toAutodiffCostFunction
      costFunction.evaluate(parameters, residuals, nullJacobians)
      residuals.get(0) must be(10.0)

      val jacobiansVector = new StdVectorDoublePointer()
      jacobiansVector.add(new DoubleArray(2).toPointer)
      jacobiansVector.add(new DoubleArray(2).toPointer)
      val jacobians: DoublePointerPointer = RichDoubleMatrix.fromStdVector(jacobiansVector)

      residuals.set(0, 0.0)
      costFunction.evaluate(parameters, residuals, jacobians)
      residuals.get(0) must be(10.0)
      jacobians.get(0, 0) must be(3)
      jacobians.get(0, 1) must be(4)
      jacobians.get(1, 0) must be(1)
      jacobians.get(1, 1) must be(2)
    }
  }
}
