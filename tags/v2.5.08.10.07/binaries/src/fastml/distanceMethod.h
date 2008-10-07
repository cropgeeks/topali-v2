#ifndef ___DISTANCE_METHOD
#define ___DISTANCE_METHOD
#include "definitions.h"
#include "sequence.h"

/*********************************************************
Distance method is a class for computing pairwise distance 
between 2 different sequences
*******************************************************/
class distanceMethod {
public:
	virtual const MDOUBLE giveDistance(	const sequence& s1,
										const sequence& s2,
										const vector<MDOUBLE> * weights,
										MDOUBLE* score=NULL) const=0;
};


#endif

