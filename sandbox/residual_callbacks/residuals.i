%module(directors="1") residuals

%{
#include "residuals.h"
%}

%feature("director", assumeoverride=1) ResidualTerm;

%rename(apply) ResidualTerm::operator();

%include "carrays.i"
%array_class(double, doubleArray);

%include "residuals.h"

