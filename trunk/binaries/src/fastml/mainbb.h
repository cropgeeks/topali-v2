#ifndef ___BB__MAIN__FILE
#define ___BB__MAIN__FILE

#include "bb_options.h"
#include "sequenceContainer.h"
#include "stochasticProcess.h"
#include "tree.h"

#include "suffStatComponent.h"
#include <vector>
using namespace std;


class mainbb {
public:
	explicit mainbb(int argc, char* argv[]);
	virtual ~mainbb();

private:
	const bb_options* _options;
	sequenceContainer _sc;
	tree _et;
	stochasticProcess* _sp;
	alphabet* _alph;
	sequenceContainer _resulutingJointReconstruction;

	void getStartingStochasticProcess();
// get starting tree
	void getStartingEvolTreeTopology();
	void getStartingNJtreeNjMLdis();
	void getStartingTreeNJ_fromDistances(const VVdouble& disTab,const vector<string>& vNames);
	void getStartingTreeFromTreeFile();
	void getStartingBranchLengthsAndAlpha();
	void printOutputTree();

	// JOINT WITH GAMMA
	void printAncestralSequencesGammaJoint();
	void findAncestralSequencesGammaJoint();

	// JOINT WITHOUT GAMMA
	void findAncestralSequencesHomJoint();

	// MARGINAL RECONSTRUCTION:
	void getMarginalReconstruction();


	void fillOptionsParameters(int argc, char* argv[]);
	void getStartingSequenceData();
	void printSearchParameters();
	void printBBProjectInfo();


};


#endif

