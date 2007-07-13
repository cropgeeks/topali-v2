#include "goldmanYangModel.h"
#include "codon.h"
#include "readDatMatrix.h" // for the normalizeQ function.

void goldmanYangModel::updateQ() {
	// building q.
	VVdouble qMatrix(61);
	int i,j,z;
	MDOUBLE sum=0.0;
	MDOUBLE epsilon=0.00000000001;
	MDOUBLE factor = 10000.0; 
	for (z=0; z < qMatrix.size();++z) qMatrix[z].resize(61,0);
	for (i=0; i < qMatrix.size();++i) {
		sum=0;
		for (j=0; j < qMatrix.size();++j) {
			if (j==i) continue;
			if (codonUtility::codonDiff(i,j) == codonUtility::different) {
				qMatrix[i][j] =0;
			} else if (codonUtility::codonDiff(i,j) == codonUtility::transition) {
				qMatrix[i][j] =_k*exp(-(1/factor)*_gcd.getGranthamDistance(codonUtility::aaOf(i),codonUtility::aaOf(j))*_v);
				if (qMatrix[i][j]<epsilon)
					cout<<"Qij is small"<<qMatrix[i][j]<<endl;
			} else if (codonUtility::codonDiff(i,j) == codonUtility::transversion) {
				qMatrix[i][j] = exp(-(1/factor)*_gcd.getGranthamDistance(codonUtility::aaOf(i),codonUtility::aaOf(j))*_v);
				if (qMatrix[i][j]<epsilon)
					cout<<"Qij is small"<<qMatrix[i][j]<<endl;
			}
			qMatrix[i][j]*=_freq[j];
		
			sum += qMatrix[i][j];
		}
		qMatrix[i][i]=-sum;
	}
	

	// check:
/*	LOG(5,<<"\n\n\n ===================================== \n");
	int a1,a2;
	for (a1=0;a1<4;++a1){
		for (a2=0;a2<4;++a2){
			LOG(5,<<qMatrix[a1][a2]<<"\t");
		}
		LOG(5,<<endl);
	}
*/

	if (_globalV == true)
		normalizeQ(qMatrix,_freq);
	
	// check:
/*	LOG(5,<<"\n\n\n ===================================== \n");
	for (a1=0;a1<4;++a1){
		for (a2=0;a2<4;++a2){
			LOG(5,<<qMatrix[a1][a2]<<"\t");
		}
		LOG(5,<<endl);
	}
*/
	
	// updating _q2Pt;
	_Q = qMatrix;
	_q2pt.fillFromRateMatrix(_freq,qMatrix); 
}



// original with V and not 1/V 
/*
void goldmanYangModel::updateQ() {
	// building q.
	VVdouble qMatrix(61);
	int i,j,z;
	MDOUBLE sum=0.0;
	for (z=0; z < qMatrix.size();++z) qMatrix[z].resize(61,0);
	for (i=0; i < qMatrix.size();++i) {
		sum=0;
		for (j=0; j < qMatrix.size();++j) {
			if (j==i) continue;
			if (codonUtility::codonDiff(i,j) == codonUtility::different) {
				qMatrix[i][j] =0;
			} else if (codonUtility::codonDiff(i,j) == codonUtility::transition) {
				qMatrix[i][j] =_k*exp(-_gcd.getGranthamDistance(codonUtility::aaOf(i),codonUtility::aaOf(j))/_v);
			} else if (codonUtility::codonDiff(i,j) == codonUtility::transversion) {
				qMatrix[i][j] = exp(-_gcd.getGranthamDistance(codonUtility::aaOf(i),codonUtility::aaOf(j))/_v);
			}
			qMatrix[i][j]*=_freq[j];
			sum += qMatrix[i][j];
		}
		qMatrix[i][i]=-sum;
	}
	// check:
	//LOG(5,<<"\n\n\n ===================================== \n");
	//int a1,a2;
	//for (a1=0;a1<4;++a1){
	//	for (a2=0;a2<4;++a2){
	//		LOG(5,<<qMatrix[a1][a2]<<"\t");
	//	}
	//	LOG(5,<<endl);
	//}

	if (_globalV == true)
		normalizeQ(qMatrix,_freq);

	//LOG(5,<<"\n\n\n ===================================== \n");
	//LOG(5,<<endl<<endl);
	//for (a1=0;a1<4;++a1){
	//	for (a2=0;a2<4;++a2){
	//		LOG(5,<<qMatrix[a1][a2]<<"\t");
	//	}
	//	LOG(5,<<endl);
	//}
	
	// updating _q2Pt;
	_Q = qMatrix;
	_q2pt.fillFromRateMatrix(_freq,qMatrix);
}


*/


