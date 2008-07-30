#include "bbReport.h"
#include "amino.h"
#include "nucleotide.h"
	
BandBReport::BandBReport( const string& reportFileName, const int position, const int alphabetSize ) :
	_reportFileName(reportFileName), _position(position), _alphabetSize(alphabetSize)
{
//	_root = new TreeNode;
//	DecisionNode rootData(-2,"allstar"); // char, node-id
//	_root->Setdata(rootData);
//	_current = _root;
//	_nodes = 1;
}

void BandBReport::report(
		const string  NodeName,
		const int     charPutInsideNode,
		const int depth,
		const MDOUBLE bestRecord,
		const MDOUBLE probOfVector,
		const MDOUBLE BoundSigma,
		const MDOUBLE boundMax
		) {

	VNodeName.push_back(NodeName);
	VcharPutInsideNode.push_back(charPutInsideNode);
	VbestRecord.push_back(bestRecord);
	VprobOfVector.push_back(probOfVector);
	VBoundSigma.push_back(BoundSigma);
	VboundMax.push_back(boundMax);
	Vdepth.push_back(depth);

}


void BandBReport::makeReport() const {

	ofstream out;
	//if (_position==0) out.open("report.txt",ios::trunc);
	//else {
		out.open(_reportFileName.c_str(),ios::app);
	//}
	out<<" position is: "<<_position<<endl;
//	cerr<<"reportFileIs: "<<_reportFileName<<endl;
	if (out == NULL) {
		errorMsg::reportError("unable to open output file for reporting");
	}
//	exit(555);
	amino aa;
	nucleotide nuc;
	for (int k=0; k < VNodeName.size(); ++k) {
		for (int l=0; l < Vdepth[k]; ++l) out<<" ";
		out<<VNodeName[k]<<" ";
		if (_alphabetSize==20) out<<aa.fromInt(VcharPutInsideNode[k])<<" ";
		else if (_alphabetSize==4) out<<nuc.fromInt(VcharPutInsideNode[k])<<" ";
		else errorMsg::reportError(" error in function BandBReport::makeReport( )");
		out<<scientific<<"best Record: "<<VbestRecord[k]<<" ";
		out<<"BoundSigma: "<<VBoundSigma[k]<<" ";
		out<<"boundMax: "<<VboundMax[k]<<" ";
		out<<"probAV: "<<VprobOfVector[k];
		out<<endl;
	}
	out.close();

	return;
}

