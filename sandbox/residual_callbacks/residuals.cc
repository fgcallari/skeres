//
// residuals.cc
//
#include "residuals.h"

ResidualTerm::ResidualTerm() {}
ResidualTerm::~ResidualTerm() {}

int ResidualEvaluator::AddResidualTerm(ResidualTerm const* c) { 
  residuals.push_back(c);
  return residuals.size();
}

double ResidualEvaluator::Eval(double x) const {
  double total = 0.0;
  double y;
  for (int i = 0; i < residuals.size(); ++i) {
    const ResidualTerm& cost = *(residuals[i]);
    cost(x, &y);
    total += y;
    fprintf(stderr, "Computed residuals[%d]=%g, total=%g\n", i, y, total);
  }
  return total;
}
