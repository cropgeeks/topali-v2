#include "logFile.h"
#include "errorMsg.h"

int myLog::_loglvl = 3;
ostream *myLog::_out= NULL;

void myLog::setLog(const string logfilename, const int loglvl) {
	if (_out != NULL) myLog::endLog();
	if ((logfilename == "-")|| (logfilename == "")) {
		myLog::setLogOstream(&cout);
	} else {
		ofstream* outLF = new ofstream;
		outLF->open(logfilename.c_str());
		if (!outLF->is_open()) {
			errorMsg::reportError("unable to open file for reading");
		}
		myLog::setLogOstream(outLF);
	}
	myLog::setLogLvl(loglvl);
	LOG(3,<<"START OF LOG FILE"<<endl);
}

void myLog::endLog(void){
	LOG(3,<<"END OF LOG FILE"<<endl);
        if (_out!=&cout && _out != NULL) {
	  ((ofstream*)_out)->close();
	  delete _out;
	}
}
