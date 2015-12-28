package org.somelightprojections.skeres
import residuals.{ResidualTerm, ResidualEvaluator, SWIGTYPE_p_double, doubleArray}

case class Residual(y0: Double) extends ResidualTerm {
  override def apply(x: Double, y: SWIGTYPE_p_double): Boolean = {
    val out = doubleArray.frompointer(y)
    out.setitem(0, x - y0)
    true
  }
}

object Test {
  System.loadLibrary("residuals");

  def main(args: Array[String]): Unit = {
    val re = new ResidualEvaluator
    println("Creating/adding residual terms")
    val rts = Vector(Residual(3.0), Residual(5.0))
    rts.foreach(re.AddResidualTerm)
    println("Evaluating at 10.0")
    val cost = re.Eval(10.0)
    println(s"Total residual = $cost")
  }
}
