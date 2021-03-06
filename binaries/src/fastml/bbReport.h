#ifndef ________BANBREPORT
#define ________BANBREPORT

#include "definitions.h"
#include <fstream>
using namespace std;

class BandBReportAllPos {
public:
	explicit BandBReportAllPos(const string& reportFileName, int minNumOfNodesToVisit) 
		: _reportFileName(reportFileName),_minNumOfNodesToVisit(minNumOfNodesToVisit) {totalNumberOfNodeVisited=0;}
	int totalNumberOfNodeVisited;
	const int _minNumOfNodesToVisit;
	const string& _reportFileName;
	void printReport() const {
		fstream out(_reportFileName.c_str(),ios::app);
		out<<"total positions visited: "<<totalNumberOfNodeVisited<<endl;
		out<<"min positions to be visited: "<<_minNumOfNodesToVisit<<endl;
		out.close();
		return;
	}
};


class BandBReport 
{
public:
	explicit BandBReport(	const string& reportFileName,
							const int position,
							const int alphabetSize);
	void report(
		const string  NodeName,
		const int     charPutInsideNode,
		const int depth,
		const MDOUBLE bestRecord,
		const MDOUBLE probOfVector,
		const MDOUBLE BoundSigma,
		const MDOUBLE boundMax);
	void makeReport() const;
	int size() {return VNodeName.size();}
private:

	vector<string>  VNodeName;
	vector<int>     VcharPutInsideNode;
	vector<MDOUBLE> VbestRecord;
	vector<MDOUBLE> VprobOfVector;
	vector<MDOUBLE> VBoundSigma;
	vector<MDOUBLE> VboundMax;
	vector<int>     Vdepth;

	const int _position;
	const int _alphabetSize;
	const string& _reportFileName;
};


#endif 

