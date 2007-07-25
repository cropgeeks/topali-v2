#include "definitions.h"
#include "treeUtil.h"
#include "simulateTree.h"
#include "talRandom.h"

simulateTree::simulateTree(const tree&  _inEt,
						   const stochasticProcess& sp,
						   const alphabet* alph) :
  _et(_inEt), _sp(sp),_alph(alph) {};

simulateTree::~simulateTree() {
//	vector<tree::nodeP> vNodes;
//	_et.getAllNodes(vNodes,_et.getRoot());
//	for (int i=0; i < vNodes.size();++i) {
//		delete vNodes[i]->data();
//	}
}


void simulateTree::generate_seq(int seqLength) {
	sequence justAseq(_alph);
	_simulatedSequences.resize(_et.getNodesNum(),justAseq);
	for (int i=0; i < _simulatedSequences.size(); ++i) {
		_simulatedSequences[i].resize(seqLength);
	}
	generateRootSeq(seqLength); 

	vector<MDOUBLE> rateVec(seqLength);
	for (int h = 0; h < seqLength; h++)  {
		int theRanCat = getRandCategory(h);
		rateVec[h] = _sp.rates(theRanCat);
	}
	

	for (int p=0 ; p < _et.getRoot()->getNumberOfSons() ; ++p) {
	  recursiveGenerateSpecificSeq(rateVec, seqLength, _et.getRoot()->getSon(p));
	}
}

void simulateTree::generate_seqWithRateVector(const vector<MDOUBLE>& rateVec, 
											  const int seqLength) {
	sequence justAseq(_alph);
	_simulatedSequences.resize(_et.getNodesNum(),justAseq);
	for (int i=0; i < _simulatedSequences.size(); ++i) {
		_simulatedSequences[i].resize(seqLength);
	}
	generateRootSeq(seqLength); 

	for (int p=0 ; p < _et.getRoot()->getNumberOfSons() ; ++p) {
	  recursiveGenerateSpecificSeq(rateVec,seqLength,_et.getRoot()->getSon(p));
	}
}

void simulateTree::generateRootSeq(int seqLength) {	
	for (int i = 0; i < seqLength; i++) {
		_simulatedSequences[_et.getRoot()->id()][i] =  giveRandomChar();
	}
	_simulatedSequences[_et.getRoot()->id()].setAlphabet(_alph);
	_simulatedSequences[_et.getRoot()->id()].setName(_et.getRoot()->name());
	_simulatedSequences[_et.getRoot()->id()].setID(_et.getRoot()->id());

}

void simulateTree::recursiveGenerateSpecificSeq(
							const vector<MDOUBLE> &rateVec,
							const int seqLength,
							tree::nodeP myNode) {

	for (int y = 0; y < seqLength; y++) {
		MDOUBLE lenFromFather=myNode->dis2father()*rateVec[y];
		int aaInFather = _simulatedSequences[myNode->father()->id()][y];
		int newChar = giveRandomChar(aaInFather,lenFromFather,y);
		_simulatedSequences[myNode->id()][y] = newChar;
    }
	_simulatedSequences[myNode->id()].setAlphabet(_alph);
	_simulatedSequences[myNode->id()].setName(myNode->name());
	_simulatedSequences[myNode->id()].setID(myNode->id());
	for (int x =0 ; x < myNode->getNumberOfSons(); ++x) {
	  recursiveGenerateSpecificSeq(rateVec, seqLength, myNode->getSon(x));
	}
}

int simulateTree::giveRandomChar() const {
  return(
	 talRandom::giveIntRandomNumberBetweenZeroAndEntry(_sp.alphabetSize())
	 );
}

int simulateTree::giveRandomChar(const int letterInFatherNode,
								 const MDOUBLE length,
								 const int pos) const {
//CHECK!!!
	assert(letterInFatherNode>=0);
	int j;
	MDOUBLE sum=0.0;
	const int abcsize = _sp.alphabetSize();
	assert(letterInFatherNode<abcsize);
	for (int loop =0 ;loop<100000 ;loop++) {
		
		MDOUBLE theRandNum = talRandom::giveRandomNumberBetweenZeroAndEntry(1);
		sum = 0.0;
		for (j=0;j<abcsize;++j) {
			sum+=_sp.Pij_t(letterInFatherNode,j, length);
//			sum+=_pi.stocProcess(pos)->Pij_t(letterInFatherNode,j, length);
			if (theRandNum<sum) return j;
		}
	}
	//LOG(500,<<"letterInFatherNode = "<<letterInFatherNode<<endl);
	//LOG(500,<<"const MDOUBLE length = " << length);
	//for (j=0;j<abcsize;++j) {
	//		LOG(500,<<"P["<<letterInFatherNode<<"]["<<j<<"]("<<length<<") ="<<_sp.Pij_t(letterInFatherNode,j, length)<<endl);
	//}
	errorMsg::reportError("Could not give random character. The reason is probably that the Pij_t do not sum to one.");
	return 1;
}


int simulateTree::getRandCategory(const int pos) const {
  MDOUBLE theRandNum = talRandom::giveRandomNumberBetweenZeroAndEntry(1);
  MDOUBLE sum = 0.0;
//  for (int j=0;j<_pi.stocProcess(pos)->categories() ;++j) {
  for (int j=0;j<_sp.categories() ;++j) {
//    sum+=_pi.stocProcess(pos)->ratesProb(j);
     sum+=_sp.ratesProb(j);
   if (theRandNum<sum) return j;
  }
  errorMsg::reportError(" error in function simulateTree::getRandCategory() ");// also quit the program
  return -1;
}

sequenceContainer simulateTree::toSeqData() {
	sequenceContainer myseqData;
	for (int i=0; i < _simulatedSequences.size(); ++i) {
		myseqData.add(_simulatedSequences[i]);
	}
	return myseqData;
}

sequenceContainer simulateTree::toSeqDataWithoutInternalNodes() {
	sequenceContainer myseqData;
	for (int i=0; i < _simulatedSequences.size(); ++i) {
		tree::nodeP theCurNode = _et.findNodeByName(_simulatedSequences[i].name());
		if (theCurNode->isInternal()) continue;
		myseqData.add(_simulatedSequences[i]);
	}
	return myseqData;
}
