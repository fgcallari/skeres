// Minimal SWIG configuration file for the
// ceres-solver HelloWorld demo
//

// Use directors so we can extend ceres classes,
// Allow access to their protected fields / methods.
%module(directors="1", allprotected="1") ceres

%{
#include <map>
#include <set>
#include <vector>
#include <memory>
#include <cstddef>
#include <cstdlib>

using std::shared_ptr;

#include "glog/logging.h"

// Minimal set of ceres headers
// needed for the demo
#include "ceres/internal/config.h"
#include "ceres/internal/scoped_ptr.h"
#include "ceres/types.h"

#include "ceres/internal/port.h"
#include "ceres/internal/macros.h"

#include "ceres/cost_function.h"
#include "ceres/covariance.h"
#include "ceres/crs_matrix.h"
#include "ceres/iteration_callback.h"
#include "ceres/ordered_groups.h"
#include "ceres/problem.h"
#include "ceres/solver.h"

#include "ceres/local_parameterization.h"
#include "ceres/loss_function.h"

#include "ceres/sized_cost_function.h"
#include "ceres/version.h"

%}

%feature("director", assumeoverride=1) ceres::LossFunction;
%feature("director", assumeoverride=1) ceres::CostFunction;

%include "std_string.i"

// Specializations of std::vectors we need.
%include "std_vector.i";
namespace std {
  %template(StdVectorInt) vector<int>;
  %template(StdVectorDouble) vector<double>;
  %template(StdVectorDoublePointer) vector<double*>;
}

// Remove warnings
%rename(apply) ceres::internal::ScopedPtrMallocFree::operator();
%rename(apply) ceres::IterationCallback::operator();

// Generate identifiers following Scala style naming rules.
%rename("%(camelcase)s", %$isclass) "";
%rename("%(lowercamelcase)s", %$isfunction) "";
%rename("%(lowercamelcase)s", %$isvariable) "";

// Convenient access to matrices represented as double** (e.g.
// the Jacobian passed to the cost function).
// See implicits in skeres.scala to see how these are
// used in practice.
%include "carrays.i"
%array_class(double, DoubleArray);
%inline %{
struct BlockMatrixHelper {
  static bool isNull(double** matrix) {
    return matrix == NULL;
  }
  static double* row(double** matrix, int i) {
    return matrix[i];
  };
};
%}

// glog initialization. We call it from an additional (wrapped)
// C++ global function, to avoid some SNAFU in the interaction
// between SWIG and some glog dependencies that show up if we
// wrap google::InitGoogleLogging directly.
%inline %{
void initGoogleLogging(const char* name) {
  google::InitGoogleLogging(name);
}
%}

%newobject PredefinedLossFunctions::trivial;
%include "ceres/internal/scoped_ptr.h"
%include "ceres/types.h"
%include "ceres/loss_function.h"
%include "ceres/cost_function.h"
%include "ceres/iteration_callback.h"
%include "ceres/ordered_groups.h"
%include "ceres/problem.h"
%include "ceres/solver.h"
%include "ceres/crs_matrix.h"
%include "ceres/local_parameterization.h"
%include "ceres/version.h"

// Convenient creation of the ceres-predefined loss functions.
%inline %{
struct PredefinedLossFunctions {
  static ceres::LossFunction* trivialLoss() { return new ceres::TrivialLoss; }
  static ceres::LossFunction* huberLoss(double a) { return new ceres::HuberLoss(a); }
  static ceres::LossFunction* softLOneLoss(double a) { return new ceres::SoftLOneLoss(a); }
  static ceres::LossFunction* cauchyLoss(double a) { return new ceres::CauchyLoss(a); }
  static ceres::LossFunction* tukeyLoss(double a) { return new ceres::TukeyLoss(a); }
  static ceres::LossFunction* tolerantLoss(double a, double b) { return new ceres::TolerantLoss(a, b); }
  static ceres::LossFunction* composedLoss(const ceres::LossFunction* a, const ceres::LossFunction* b) {
    return new ceres::ComposedLoss(a, ceres::Ownership::DO_NOT_TAKE_OWNERSHIP,
                                   b, ceres::Ownership::DO_NOT_TAKE_OWNERSHIP);
  }
  static ceres::LossFunction* scaledLoss(const ceres::LossFunction* rho, double a) {
    return new ceres::ScaledLoss(rho, a, ceres::Ownership::DO_NOT_TAKE_OWNERSHIP);
  }
};
%}

// Loads the swig-wrapped C++ DLL.
%pragma(java) jniclasscode=%{
  static {
    try {
      System.loadLibrary("skeres");
    } catch (UnsatisfiedLinkError e) {
      System.err.println("Native code library failed to load. \n" + e);
      System.exit(1);
    }
  }
%}

