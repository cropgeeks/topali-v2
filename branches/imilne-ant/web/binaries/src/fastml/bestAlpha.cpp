#include <iostream>
using namespace std;

#include "bestAlpha.h"
#include "brLenOptEM.h"
#include "numRec.h"
#include "logFile.h"

#define VERBOS
//void bestAlpha::checkAllocation() {
//	if (_pi->stocProcessFromLabel(0)->getPijAccelerator() == NULL) {
//		errorMsg::reportError(" error in function findBestAlpha");
//	}
//}
//
bestAlphaAndBBL::bestAlphaAndBBL(tree& et, //find Best Alpha and best BBL
					   const sequenceContainer& sc,
					   stochasticProcess& sp,
					   const Vdouble * weights,
					   const MDOUBLE upperBoundOnAlpha,
					   const MDOUBLE epsilonAlphaOptimization,
					   const MDOUBLE epsilonLikelihoodImprovment,
					   const int maxBBLIterations,
					   const int maxTotalIterations){
//	LOG(5,<<"find Best Alpha and best BBL"<<endl);
//	LOG(5,<<" 1. bestAlpha::findBestAlpha"<<endl);
//	brLenOpt br1(*et,*pi,weights);
	MDOUBLE oldL = VERYSMALL;
	MDOUBLE newL = VERYSMALL;
	const MDOUBLE bx=upperBoundOnAlpha*0.3;
	const MDOUBLE ax=0;
	const MDOUBLE cx=upperBoundOnAlpha;
//
	MDOUBLE bestA=0;
	int i=0;
	for (i=0; i < maxTotalIterations; ++i) {
		newL = -brent(ax,bx,cx,
		C_evalAlpha(et,sc,sp,weights),
		epsilonAlphaOptimization,
		&bestA);

#ifdef VERBOS
		LOG(5,<<"new L = " << newL<<endl);
		LOG(5,<<"new Alpha = " <<bestA<<endl);
#endif
		if (newL > oldL+epsilonLikelihoodImprovment) {
			oldL = newL;
		} else {
			_bestL = oldL;
			_bestAlpha= bestA;
			(static_cast<gammaDistribution*>(sp.distr()))->setAlpha(bestA);
			break;
		}

		(static_cast<gammaDistribution*>(sp.distr()))->setAlpha(bestA);
		newL =brLenOptEM::optimizeBranchLength1G_EM(et,sc,sp,NULL,maxBBLIterations,epsilonLikelihoodImprovment);//maxIterations=1000
#ifdef VERBOS
		LOG(5,<<"new L = " << newL<<endl);
#endif

		if (newL > oldL+epsilonLikelihoodImprovment) {
			oldL = newL;
		}
		else {
			_bestL = oldL;
			_bestAlpha= bestA;
			(static_cast<gammaDistribution*>(sp.distr()))->setAlpha(bestA);
			break;
		}
	}
	if (i==maxTotalIterations) {
		_bestL = newL;
		_bestAlpha= bestA;
		(static_cast<gammaDistribution*>(sp.distr()))->setAlpha(bestA);
	}
}
		
bestAlphaFixedTree::bestAlphaFixedTree(const tree& et, //findBestAlphaFixedTree
					   const sequenceContainer& sc,
					   stochasticProcess& sp,
					   const Vdouble * weights,
					   const MDOUBLE upperBoundOnAlpha,
					   const MDOUBLE epsilonAlphaOptimization){
	//LOG(5,<<"findBestAlphaFixedTree"<<endl);
	MDOUBLE bestA=0;
	const MDOUBLE cx=upperBoundOnAlpha;// left, midle, right limit on alpha
	const MDOUBLE bx=cx*0.3;
	const MDOUBLE ax=0;

	
	_bestL = -brent(ax,bx,cx,
		C_evalAlpha(et,sc,sp,weights),
		epsilonAlphaOptimization,
		&bestA);
	(static_cast<gammaDistribution*>(sp.distr()))->setAlpha(bestA);
	_bestAlpha= bestA;
}

