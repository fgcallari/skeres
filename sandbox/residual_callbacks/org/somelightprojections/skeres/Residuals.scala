
package org.somelightprojections.skeres
import residuals.{ResidualTerm, ResidualEvaluator, SWIGTYPE_p_double, doubleArray}

// Concrete residual term: cost(x) = x - y0
case class Residual(y0: Double) extends ResidualTerm {
  override def apply(x: Double, y: SWIGTYPE_p_double): Boolean = {
    val out = doubleArray.frompointer(y)
    out.setitem(0, x - y0)
    true
  }
}

object Test {
  // Load the swig-wrapped C++ DLL.
  System.loadLibrary("residuals");

  def main(args: Array[String]): Unit = {
    val re = new ResidualEvaluator
    println("Creating/adding residual terms")
    Vector(3.0, 5.0)
      .map(Residual)
      .foreach(re.AddResidualTerm)
    println("Evaluating at 10.0")
    val cost = re.Eval(10.0)
    println(s"Total residual = $cost")
  }
}
