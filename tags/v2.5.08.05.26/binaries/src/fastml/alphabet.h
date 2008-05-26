// version 1.01
// last modified 1 Jan 2004

#ifndef ___ALPHABET_H
#define ___ALPHABET_H

#include <string>
#include <vector>
using namespace std;

class alphabet {
public:
	virtual int relations(const int charInSeq, const int charToCheck) const =0;
	virtual int fromChar(const string& seq,const int pos) const =0;
	virtual string fromInt(const int in_id) const =0;
	virtual int size() const =0;
	virtual ~alphabet()=0;
	virtual int unknown() const =0;
	virtual int gap() const =0;
	virtual alphabet* clone() const = 0;
	virtual int stringSize() const =0;
	virtual vector<int> fromString(const string& str) const =0;

};

#endif

