#include "sequence.h"

#include <algorithm>
using namespace std;


sequence::sequence(const string& str,
				   const string& name,
				   const string& remark,
				   const int id,
				   const alphabet* inAlph)
: _alphabet(inAlph->clone()), _remark(remark), _name(name),_id(id) 
{
	for (int k=0; k < str.size() ;k += _alphabet->stringSize()) {
		_vec.push_back(inAlph->fromChar(str, k));
	}
}


sequence::sequence(const sequence& other) 
: _vec(other._vec), _alphabet(other._alphabet->clone()), 
  _remark(other._remark), _name(other._name),_id(other._id) 
{
	
}

sequence::~sequence()
{
	if (_alphabet) 
		delete _alphabet;
}

void sequence::resize(const int k, const int* val) {
	if (val == NULL) {
		_vec.resize(k,_alphabet->unknown());
	}
	else {
		_vec.resize(k,*val);
	}
}

string sequence::toString() const{
	string tmp;
	for (int k=0; k < _vec.size() ; ++k ){
		tmp+= _alphabet->fromInt(_vec[k]);
	}
	return tmp;
}

string sequence::toString(const int pos) const{
	return _alphabet->fromInt(_vec[pos]);
}	

void sequence::addFromString(const string& str) {
	for (int k=0; k < str.size() ; k+=_alphabet->stringSize()) {
		_vec.push_back(_alphabet->fromChar(str,k));
	}
}

class particip {
public:
	explicit particip()  {}
	bool operator()(int i) {
		return (i==-1000);
	}
};

//removePositions: the poitions to be removed are marked as '1' in posToRemoveVec
//all othehr positions are '0' 
void sequence::removePositions(const vector<int> & posToRemoveVec) 
{
	if(posToRemoveVec.size() != seqLen())
		errorMsg::reportError("the input vector must be same size as sequence length. in sequence::removePositions");
	for (int k=0; k < posToRemoveVec.size(); ++k) {
		if (posToRemoveVec[k] == 1) 
			_vec[k] = -1000;
	}
	vector<int>::iterator vec_iter;
	vec_iter =  remove_if(_vec.begin(),_vec.end(),particip());
	_vec.erase(vec_iter,_vec.end()); // pg 1170, primer.
}
