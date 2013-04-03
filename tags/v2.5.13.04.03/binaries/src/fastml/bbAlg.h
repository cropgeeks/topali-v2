#if !defined ___BB__ALG__
#define ___BB__ALG__

#include "computePijComponent.h" 
#include "bbNodeOrderAlg.h"
#include "bbEvaluateSpecificAV.h"
#include "bbfindBestAVDynProg.h"
#include "bbReport.h"
#include "sequenceContainer.h"
#include "stochasticProcess.h"

class bbAlg {
public:
	enum boundMethod {max,sum,both};
	explicit bbAlg( const tree& et,
					const stochasticProcess &sp,
					const sequenceContainer &sc,
					const boundMethod boundType,
					const string& reportFileName,
					const MDOUBLE computeAgainExactTreshold);
	virtual ~bbAlg(); 
	MDOUBLE bbReconstructAllPositions(sequenceContainer& res);
	sequenceContainer fromAncestralSequenceToSeqData();
	void outputTheJointProbAtEachSite(const string & outputFileProbJoint);

private:
	const tree& _et;
	const stochasticProcess& _sp;
	const sequenceContainer& _sc;
	bbEvaluateSpecificAV*  _bbesavp1;
	computePijGam _cpij;
	bbNodeOrderAlg* _bbNodeOrderAlg1; 
	bbfindBestAVDynProg* _bbfindBestAVDynProg1;

	boundMethod _boundMethod;

	int _alphabetSize;
	int _seqLen;
	MDOUBLE _bestRecord; // for 1 position. =0 when new pos is started...
	Vdouble _jointL; // the likelihood of the reconstruction, per position.
	void fillProbOfPosition(const int pos);
	MDOUBLE bbReconstructPositions(const int pos);
	MDOUBLE bbReconstructPositions(const int pos, const int nodeNum);

	vector<sequence> _bestReconstruction; // the sequences (nodes * seqLen)
	vector<sequence> _internalSequences; // the sequences (nodes * seqLen)

	bool decideIfHaveToGoDown(const int pos,
								 MDOUBLE& boundSigma,
								 MDOUBLE& boundMax) const;
	bool checkBoundSigma(const int pos,
							MDOUBLE& inBoundSigma) const;
	bool checkBoundMax(const int pos, MDOUBLE& inboundMax) const;


// reporting:
	BandBReport* _bbReport; // report per position.
	BandBReportAllPos BandBReportAllPos1; // report for all positions.
	const string& _reportFileName;
	MDOUBLE _pOfPos;

};


#endif
