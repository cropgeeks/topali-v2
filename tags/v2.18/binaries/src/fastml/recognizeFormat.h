#ifndef ___RECOGNIZE_FORMAT
#define ___RECOGNIZE_FORMAT

#include "sequenceContainer.h"

class recognizeFormat{
public:
	static sequenceContainer read(istream &infile, const alphabet* alph);
	static void write(ostream &out, const sequenceContainer& sd);
};

#endif



