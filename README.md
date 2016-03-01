# skeres

A port to Scala of the Ceres Solver Library.

## Build
On MacOSX:
 * Download and install SWIG from swig.org, or using your favorite package manager.
 * Download and build ceres-solver from its sources at ceres-solver.org, plus its dependencies
   as explained in the ceres user guide. 
 * **NOTE CAREFULLY:** ceres must be built as a DLL, and so must be its dependencies. 
 * Edit file configuraton.sh to reflect your environment.
 * Run *mkceres.sh*, it will compile and link the wrappers, then plop you in the *sbt* console.
 
## Running the examples
 
 * At the *sbt* console, type
 
    \> project examples
    
     [... blah blah ...]
    
    \> run
 
This is a work in progress. Development notes are posted at: http://somelightprojections.blogspot.com/
