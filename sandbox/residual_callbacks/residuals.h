//
// residuals.h
//
#include <vector>
#include <cstdio>

class ResidualTerm {
public:
  ResidualTerm();
  virtual ~ResidualTerm();
  // Evaluate the residual at point x, write the result into y
  // Returns true iff successful.
  virtual bool operator()(double x, double* y) const = 0;
};

class ResidualEvaluator {
private:
  std::vector<const ResidualTerm*> residuals;
public:
  // Register the given residual term (not owned).
  // Returns the number of registered terms.
  int AddResidualTerm(ResidualTerm const* c);
  // Compute the sum of all residual terms at x.
  double Eval(double x) const;
};
