#ifndef ___BRANCH_LEN_OPT_EM
#define ___BRANCH_LEN_OPT_EM

#include "definitions.h"
#include "computePijComponent.h"
#include "sequenceContainer.h"
#include "tree.h"
#include "stochasticProcess.h"

namespace brLenOptEM {

	MDOUBLE optimizeBranchLength1G_EM(	tree& et,
									const sequenceContainer& sc,
									const stochasticProcess& sp,
									const Vdouble * weights,
									const int maxIterations=50, // changed from 1000 on 12.7.04
									const MDOUBLE epsilon=0.05,
									const MDOUBLE tollForPairwiseDist=0.001);//changed from 0.0001 on 12.7.04
	MDOUBLE optimizeBranchLengthNG_EM_SEP(vector<tree>& et,
									const vector<sequenceContainer>& sc,
									const vector<stochasticProcess> &sp,
									const vector<Vdouble *> * weights,
									const int maxIterations=1000,
									const MDOUBLE epsilon=0.05,
									const MDOUBLE tollForPairwiseDist=0.0001);
	MDOUBLE optimizeBranchLengthNG_EM_PROP( // also for the CONCATANATE MODEL. DOESN'T CHANGE THE RATE OF EACH GENE!
									tree& et,
									const vector<sequenceContainer>& sc,
									const vector<stochasticProcess>& sp,
									const vector<Vdouble *> * weights = NULL,
									const int maxIterations=1000,
									const MDOUBLE epsilon=0.05,
									const MDOUBLE tollForPairwiseDist=0.0001);
};


#endif


