// version 2.00
// last modified 21 Mar 2004

/************************************************************
 This is a virtual class from which all types of distribution classes inherit from.
************************************************************/

#ifndef ___DISTRIBUTION
#define ___DISTRIBUTION

#include "definitions.h"

class distribution {
public:
	virtual distribution* clone() const = 0;
	virtual ~distribution() = 0;

	virtual const int categories() const=0;
	virtual const MDOUBLE rates(const int i) const=0;
	virtual const MDOUBLE ratesProb(const int i) const=0;
 	virtual void setGlobalRate(const MDOUBLE x)=0;
 	virtual MDOUBLE getGlobalRate()const=0;
	virtual const MDOUBLE getCumulativeProb(const MDOUBLE x) const = 0;

};
#endif


