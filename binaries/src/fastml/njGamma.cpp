#include "njGamma.h"
#include "numRec.h"

gammaMLDistances::gammaMLDistances(const stochasticProcess & sp,
							const VVdouble & posteriorProb,
							const MDOUBLE toll,
							const MDOUBLE maxPairwiseDistance) 
							:
  _sp(sp),_posteriorProb(posteriorProb),_toll(toll),_maxPairwiseDistance(maxPairwiseDistance) 
{}


const MDOUBLE gammaMLDistances::giveDistance(	const sequence& s1,
							const sequence& s2,
							const Vdouble  * weights,
							MDOUBLE* score) const {
	const MDOUBLE ax=0,bx=1.0,cx=_maxPairwiseDistance;
	MDOUBLE dist=-1.0;
	MDOUBLE resL = -dbrent(ax,bx,cx,
		  C_eval_gammaMLDistances(_sp,s1,s2,_posteriorProb,weights),
		  C_eval_gammaMLDistances_d(_sp,s1,s2,_posteriorProb,weights),
		  _toll,
		  &dist);
	if (score) *score = resL;
	return dist;
}
	
