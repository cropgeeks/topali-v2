// version 1.01
// last modified 1 Jan 2004
#include "definitions.h"
#include <cassert>
#include "errorMsg.h"
#include "logFile.h"

ostream *errorMsg::_errorOut= NULL;

void errorMsg::reportError(const vector<string>& textToPrint, const int exitCode) {
	for (int i =0 ; i < textToPrint.size() ; ++i) {
		LOG(1,<<textToPrint[i]<<endl);
		cerr<<textToPrint[i]<<endl;
		if (_errorOut != NULL && *_errorOut != cerr)  {
			(*_errorOut)<<textToPrint[i]<<endl;
		}
	}
	assert(0); // always stop here if in DEBUG mode.
	exit(exitCode);
}

void errorMsg::reportError(const string& textToPrint, const int exitCode) {
	LOG(1,<<endl<<textToPrint<<endl);
	cerr<<endl<<textToPrint<<endl;
	if (_errorOut != NULL && *_errorOut != cerr)  {
		(*_errorOut)<<textToPrint<<endl;
	}
	assert(0); // always stop here if in DEBUG mode.
	exit(exitCode);
}


