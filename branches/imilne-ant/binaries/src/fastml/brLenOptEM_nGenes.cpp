#include "definitions.h"
#include "tree.h"
#include "brLenOptEM.h"
#include "computeUpAlg.h"
#include "computeDownAlg.h"
#include "computePijComponent.h"
#include "logFile.h"
#include "likelihoodComputation.h"

#include "countTableComponent.h"
#include "computeCounts.h"
#include "fromCountTableComponentToDistance.h"

#include <cmath>
using namespace likelihoodComputation;

//#define VERBOS

MDOUBLE brLenOptEM::optimizeBranchLengthNG_EM_SEP(vector<tree>& et,
									const vector<sequenceContainer>& sc,
									const vector<stochasticProcess> &sp,
									const vector<Vdouble *> * weights,
									const int maxIterations,
									const MDOUBLE epsilon,
									const MDOUBLE tollForPairwiseDist) {
	MDOUBLE newL =0;
	for (int i=0; i < et.size(); ++i) {
		#ifdef VERBOS
			LOG(5,<<" OPTIMIZING GENE "<<i<<" ... "<<endl);
		#endif
		MDOUBLE resTmp =  optimizeBranchLength1G_EM(et[i],sc[i],sp[i],(weights?(*weights)[i]:NULL),maxIterations,epsilon);
		#ifdef VERBOS
			LOG(5,<<" GENE "<<i<<" LOG-L = "<< resTmp<<endl);
		#endif
		newL += resTmp;
	}
	return newL;

}

#undef VERBOS
