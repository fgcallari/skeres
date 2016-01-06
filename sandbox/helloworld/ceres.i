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
#include "ceres/crs_matrix.h"
#include "ceres/iteration_callback.h"
#include "ceres/ordered_groups.h"
#include "ceres/problem.h"
#include "ceres/solver.h"
%}

// We only extend CostFunction for now.
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

// glog initialization. We call it from an additional (wrapped) 
// C++ global function, to avoid some SNAFU in the interaction 
// between SWIG and some glog dependencies that show up if we
// wrap google::InitGoogleLogging directly.
%inline %{
void initGoogleLogging(const char* name) { 
  google::InitGoogleLogging(name);
}
%}

// Convenient Java manipulation of unsized C arrays through
// pointers. 
%include "carrays.i"
%array_class(double, DoubleArray);

// Convenient access to matrices represented as double** (e.g.
// the Jacobian passed to the cost function).
// See implicits in skeres.scala to see how these are
// used in practice.
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

// Convenient Array copyback c -> java.
// See implicits in skeres.scala to see how this is
// used in practice.
%include "arrays_java.i"
%inline %{
struct ArrayHelper {
  static void pointerToArray(double* from, int n, double to[]) {
    ::memcpy(from, to, n);
  }
};
%}

// Generate ceres wrappers as needed for HelloWorld.
%include "ceres/internal/scoped_ptr.h"
%include "ceres/types.h"
%include "ceres/cost_function.h"
%include "ceres/iteration_callback.h"
%include "ceres/ordered_groups.h"
%include "ceres/problem.h"
%include "ceres/solver.h"

