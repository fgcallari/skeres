# skeres

A port to the JVM of the Ceres Solver Library. Main target language is Scala,
though it should work with minor modifications in Java.

## Build
On MacOSX (should work on other UNIX-like systems):

 * Download and install SWIG from swig.org, or using your favorite package manager.
 * Download and build ceres-solver from its sources at ceres-solver.org, plus its dependencies
   as explained in the ceres user guide. 
 * **NOTE CAREFULLY:** ceres must be built as a DLL, and so must be its dependencies. 
 * Edit file *configuration.sh* to reflect your environment.
 * Run *build.sh*, it will compile and link the wrappers, then plop you in the *sbt* console.
 * When done or in trouble, run *clean.sh* to reset the distribution to its original state.

Please send pull requests with your recipes if you get it to work on Windows or
other OS'.

## Running the examples
 
 * At the *sbt* console, type
      
    \> project root
 
    \> test
     
     [ ... scal unit tests are run ... ]
     
    \> project examples
    
     [... blah blah ...]
    
    \> run
 
This is a work in progress. Development notes are posted at: http://blog.somelightprojections.org/

## Usage in your applications.

This port follows closely the Ceres C++ API, so familiarize yourself with its
documentation first. In keeping with the usual Scala styoe, native Ceres 
methods wrapped by SWIG are accessible using lowerCamelCase names. 

Begin by looking at the Scala sources in the "examples" tree. The
CurveFitting and SimpleBundleAdjuster example are good places to start.

Data used for the evaluation of the residual terms should be stored entirely in
JVM-side data structures as appropriate.

Parameter blocks are instead stored on the native code side, and accessed
through JVM objects wrapping their containers or pointers tosaid containers. 
Utility classes are provided to simplify the management of this memory. See
also [this blog post](http://blog.somelightprojections.org/2016/01/on-calling-ceres-from-scala-30-who-owns.html) 
for a discussion of design choices concerning memory management.

Automatic differentiation is fully supported using the Jet type from the
[spire](https://github.com/non/spire) library. You can access it by defining
a [CostFunctor](https://github.com/fgcallari/skeres/blob/master/core/src/main/scala/org/somelightprojections/skeres/CostFunctor.scala) 
of your own that computes the residual by overriding a generic apply() 
method parametrized on "T: Field: Trig: NRoot: Order: ClassTag" type and typeclasses.
A call to method *toAutodiffCostFunction* on the functor itself will then return
a CostFunction instance suitable for automatic differentiation.


