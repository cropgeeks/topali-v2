#include "sequenceContainer.h"
#include "logFile.h"
#include "someUtil.h"

const int sequenceContainer::makeSureAllSeqAreSameLengthAndGetLen() const {
	if (_seqDataVec.size() == 0) return 0;
	const int len = _seqDataVec[0].seqLen();
	for (int i=1; i < _seqDataVec.size(); ++i) {
		if (_seqDataVec[i].seqLen()!=len) {
			errorMsg::reportError("not all sequences are of the same lengths");
		}
	}

	return len;
}

//void sequenceContainer::addFromsequenceContainer(sequenceContainer& seqToAdd){
//	if (_seqDataVec.empty()) { // first sequence to add
//		sequenceContainer::taxaIterator tit;
//		sequenceContainer::taxaIterator titEND;
//		tit.begin(seqToAdd);
//		titEND.end(seqToAdd);
//		while (tit!=titEND) {
//			_seqDataVec.push_back(*tit);
//
//		}
//	}
//	else {// now we are adding sequences to sequences that are already there.
//		sequenceContainer::taxaIterator tit;
//		sequenceContainer::taxaIterator titEND;
//		tit.begin(seqToAdd);
//		titEND.end(seqToAdd);
//		while (tit!=titEND) {
//			for (int i=0; i < _seqDataVec.size(); ++i) {
//				if (tit->name() == _seqDataVec[i].name()) {
//					_seqDataVec[i]+=(*tit);
//					break;
//				}
//			}
//			++tit;
//		}
//	}
//}

void sequenceContainer::changeGaps2MissingData() {

	for (int i = 0; i < seqLen();++i) {//going over al positions
		for (int j = 0; j < _seqDataVec.size();++j) {
			if (_seqDataVec[j][i] == -1){
				 _seqDataVec[j][i]=getAlphabet()->unknown(); // missing data
			}
		}
	}
}

const int sequenceContainer::getId(const string &seqName, bool issueWarningIfNotFound) const {
	int k;
	for (k=0 ; k < _seqDataVec.size() ; ++k) {
		if (_seqDataVec[k].name() == seqName) return (_seqDataVec[k].id());
	}
	if (k == _seqDataVec.size() && issueWarningIfNotFound) {
		// debuggin
		LOG(5,<<"seqName = "<<seqName<<endl);
		for (k=0 ; k < _seqDataVec.size() ; ++k) {
			LOG(5,<<"_seqDataVec["<<k<<"].name() ="<<_seqDataVec[k].name()<<endl);
		}
		//end dubug
		LOG(0,<<seqName<<endl);
		vector<string> err;
		err.push_back("Could not find a sequence that matches the sequence name  ");
		err.push_back(seqName);
		err.push_back("in function sequenceContainer::getSeqPtr ");
		err.push_back(" make sure that names in tree file match name in sequence file ");
		errorMsg::reportError(err); // also quit the program
	}
	return -1;
}

const Vstring sequenceContainer::names() const {
	vector<string> res;
	for (int i=0; i < _seqDataVec.size(); ++i) {
		res.push_back(_seqDataVec[i].name());
	}
	return res;
}

sequenceContainer::sequenceContainer() {
	_id2place.resize(100,-1);
}

sequenceContainer::~sequenceContainer(){}

void sequenceContainer::add(const sequence& inSeq) {
	_seqDataVec.push_back(inSeq);
	if (_id2place.size() < inSeq.id()+1) {
		_id2place.resize(inSeq.id()+100,-1);
	}
	if (_id2place[inSeq.id()] != -1) {
		string err = "Two sequences with the same id - error in function sequenceContainer::add";
		err+= "\nThe id of the sequence you are trying to add = ";
		err += int2string(inSeq.id());
		errorMsg::reportError(err);
	}
	_id2place[inSeq.id()] = _seqDataVec.size()-1;
}

void sequenceContainer::removeGapPositions(){
	vector<int> posToRemove(seqLen(),0);
	bool gapCol;
	int i,j;
	for (i = 0; i < seqLen();++i) {//going over al positions
		gapCol = false;
		for (j = 0; j < _seqDataVec.size();++j) {
			if (_seqDataVec[j][i] == -1) posToRemove[i] = 1;
		}
	}

	for (int z = 0; z < _seqDataVec.size();++z) {
		_seqDataVec[z].removePositions(posToRemove);
	}
}

void sequenceContainer::removeGapPositionsAccordingToAReferenceSeq(const string & seqName){
	int idOfRefSeq = getId(seqName,true);
	vector<int> posToRemove(seqLen(),0);
	int i;
	for (i = 0; i < seqLen();++i) {//going over al positions
		if (_seqDataVec[idOfRefSeq][i] == -1) posToRemove[i] = 1;
	}
	for (int z = 0; z < _seqDataVec.size();++z) {
		_seqDataVec[z].removePositions(posToRemove);
	}
}

void sequenceContainer::removeUnknownPositionsAccordingToAReferenceSeq(const string & seqName){
	int idOfRefSeq = getId(seqName,true);
	vector<int> posToRemove(seqLen(),0);
	int i;
	for (i = 0; i < seqLen();++i) {//going over al positions
		if (_seqDataVec[idOfRefSeq][i] == getAlphabet()->unknown()) posToRemove[i] = 1;
	}
	for (int z = 0; z < _seqDataVec.size();++z) {
		_seqDataVec[z].removePositions(posToRemove);
	}
}

void sequenceContainer::changeDotsToGoodCharacters() {
	for (int i = 0; i < seqLen();++i) {//going over al positions
		int charInFirstSeq = _seqDataVec[0][i];
		if (charInFirstSeq == -3) {
			LOG(5,<<" position is "<<i<<endl);
			errorMsg::reportError(" the first line contains dots ");
		}
		for (int j = 1; j < _seqDataVec.size();++j) {
			if ((_seqDataVec[j][i] == -3)) {
				_seqDataVec[j][i] = charInFirstSeq; // missing data
			}
		}
	}
}

int sequenceContainer::numberOfSequencesWithoutGaps (const int pos) const {
	int numOfNonCharPos = numberOfSeqs();
	for (int i=0; i < numberOfSeqs(); ++i) {
		if ((*this)[i][pos] <0) --numOfNonCharPos;
	}
	return numOfNonCharPos;
}

