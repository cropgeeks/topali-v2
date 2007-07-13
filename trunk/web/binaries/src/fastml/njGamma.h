#ifndef NJ_GAMMA_H
#define NJ_GAMMA_H

#include "distanceMethod.h"
#include "math.h"

// TO DO:
// 1. CHANGE THE CODE: if ((_s1[pos] >= 0) && (_s1[pos] < _sp.alphabetSize())
//				&& (_s2[pos] >= 0) && (_s2[pos] < _sp.alphabetSize())) {
//					TO SOMETHING NORMAL.

// 2. ((_s1[pos] == -2) && (_s2[pos] <_sp.alphabetSize())) ||
//					((_s2[pos] == -2) && (_s1[pos] <_sp.alphabetSize()))
//						SAME...

// 3. remove computation of derivatives in C_eval_gammaMLDistances


#include "stochasticProcess.h"
#include "definitions.h"
#include "sequence.h"

class gammaMLDistances : public distanceMethod{
public:
explicit gammaMLDistances(	const stochasticProcess & sp,
							const VVdouble & posteriorProb, // pos * rate
							const MDOUBLE toll =0.0001,
							const MDOUBLE maxPairwiseDistance = 2.0);

const MDOUBLE giveDistance(	const sequence& s1,
							const sequence& s2,
							const vector<MDOUBLE>  * weights,
							MDOUBLE* score=NULL) const;

private:
	const stochasticProcess &_sp;
	const VVdouble & _posteriorProb;
	const MDOUBLE _toll;
	const MDOUBLE _maxPairwiseDistance;
};


class C_eval_gammaMLDistances_d{ 
private:
	const stochasticProcess& _sp;
	const sequence& _s1;
	const sequence& _s2;
	const Vdouble* _weights;
	const VVdouble& _posteriorProb; // pos, rate
public:
	C_eval_gammaMLDistances_d(const stochasticProcess& sp,
		const sequence& s1,
		const sequence& s2,
		const VVdouble& posteriorProb,
		const Vdouble  * weights):  _sp(sp),
									_s1(s1),
									_s2(s2),
									_weights(weights),
									_posteriorProb(posteriorProb)
	{};


	MDOUBLE operator() (MDOUBLE dist) {
		MDOUBLE sumL=0.0;
		MDOUBLE sumR = 0.0;
		MDOUBLE sumR_d = 0.0;
		for (int pos=0; pos < _s1.seqLen(); ++pos){
			if ((_s1.getAlphabet()->unknown() == _s1[pos]) && (_s2.getAlphabet()->unknown()==_s2[pos])) {	continue;} // two unknowns
			sumR = 0;
			sumR_d = 0;
			for (int rateCategor = 0; rateCategor<_sp.categories(); ++rateCategor) {
				MDOUBLE rate = _sp.rates(rateCategor);
				MDOUBLE pij= 0;
				MDOUBLE dpij=0;
				if ((_s1[pos] >= 0) && (_s1[pos] < _sp.alphabetSize())
				&& (_s2[pos] >= 0) && (_s2[pos] < _sp.alphabetSize())) {
					// normal case, no ? , B, Z and such.
					pij= _sp.Pij_t(_s1[pos],_s2[pos],dist*rate);
					dpij= _sp.dPij_dt(_s1[pos],_s2[pos],dist*rate)*rate;
					if (pij==0) {
						pij = 0.000000001;
					}
					MDOUBLE exp =  _sp.freq(_s1[pos])*_posteriorProb[pos][rateCategor];
					sumR += pij *exp;
					sumR_d += dpij*exp;
				} else if (
					((_s1[pos] == -2) && (_s2[pos] <_sp.alphabetSize())) ||
					((_s2[pos] == -2) && (_s1[pos] <_sp.alphabetSize()))
					)
				{ // this is the more complicated case, where one or both of the letters is ?
				   // but the other is not a combination like B for AA.
					sumR = 1; // unknown pair with one.
					sumR_d =0; // actually, this is the important part, because after dividing we get 0...
				}
				else {// this is the most complicated case, when you have combinations of letters,
				  // for example B in one sequence and ? in the other.
					for (int iS1 =0; iS1< _sp.alphabetSize(); ++iS1) {
						for (int iS2 =0; iS2< _sp.alphabetSize(); ++iS2) {
							if ((_s1.getAlphabet()->relations(_s1[pos],iS1)) &&
								(_s2.getAlphabet()->relations(_s2[pos],iS2))) {
									MDOUBLE exp = _sp.freq(iS1)*_posteriorProb[pos][rateCategor];;
								sumR += exp* _sp.Pij_t(iS1,iS2,dist*rate);
								sumR_d += exp * _sp.dPij_dt(iS1,iS2,dist*rate)*rate;
							}
						}
					}
				}
			}// end of for rate categories
			assert(sumR!=0);
			sumL += (sumR_d/sumR)*(_weights ? (*_weights)[pos]:1);
		}
		return -sumL;
	};
};

class C_eval_gammaMLDistances{ 
private:
	const stochasticProcess& _sp;
	const sequence& _s1;
	const sequence& _s2;
	const Vdouble* _weights;
	const VVdouble& _posteriorProb; // pos, rate
public:
	C_eval_gammaMLDistances(const stochasticProcess& sp,
		const sequence& s1,
		const sequence& s2,
		const VVdouble& posteriorProb,
		const Vdouble  * weights):  _sp(sp),
					    _s1(s1),
					    _s2(s2),
					    _weights(weights), 
					    _posteriorProb(posteriorProb)
	{};


	MDOUBLE operator() (MDOUBLE dist) {
		MDOUBLE sumL=0.0;
		MDOUBLE sumR = 0.0;
		MDOUBLE sumR_d = 0.0;
		for (int pos=0; pos < _s1.seqLen(); ++pos){
			if ((_s1.getAlphabet()->unknown() == _s1[pos]) && (_s2.getAlphabet()->unknown()==_s2[pos])) {	continue;} // two unknowns
			sumR = 0;
			sumR_d = 0;
			for (int rateCategor = 0; rateCategor<_sp.categories(); ++rateCategor) {
				MDOUBLE rate = _sp.rates(rateCategor);
				MDOUBLE pij= 0;
				MDOUBLE dpij=0;
				if ((_s1[pos] >= 0) && (_s1[pos] < _sp.alphabetSize())
				&& (_s2[pos] >= 0) && (_s2[pos] < _sp.alphabetSize())) {
					// normal case, no ? , B, Z and such.
					pij= _sp.Pij_t(_s1[pos],_s2[pos],dist*rate);
					dpij= _sp.dPij_dt(_s1[pos],_s2[pos],dist*rate)*rate;
					if (pij==0) {
						pij = 0.000000001;
					}
					MDOUBLE exp =  _sp.freq(_s1[pos])*_posteriorProb[pos][rateCategor];
					sumR += pij *exp;
					sumR_d += dpij*exp;
				} else if (
					((_s1[pos] == -2) && (_s2[pos] <_sp.alphabetSize())) ||
					((_s2[pos] == -2) && (_s1[pos] <_sp.alphabetSize()))
					)
				{ // this is the more complicated case, where one or both of the letters is ?
				   // but the other is not a combination like B for AA.
					sumR = 1; // unknown pair with one.
					sumR_d =0; // actually, this is the important part, because after dividing we get 0...
				}
				else {// this is the most complicated case, when you have combinations of letters,
				  // for example B in one sequence and ? in the other.
					for (int iS1 =0; iS1< _sp.alphabetSize(); ++iS1) {
						for (int iS2 =0; iS2< _sp.alphabetSize(); ++iS2) {
							if ((_s1.getAlphabet()->relations(_s1[pos],iS1)) &&
								(_s2.getAlphabet()->relations(_s2[pos],iS2))) {
									MDOUBLE exp = _sp.freq(iS1)*_posteriorProb[pos][rateCategor];;
								sumR += exp* _sp.Pij_t(iS1,iS2,dist*rate);
								sumR_d += exp * _sp.dPij_dt(iS1,iS2,dist*rate)*rate;
							}
						}
					}
				}
			}// end of for rate categories
			assert(sumR!=0);
			sumL += log(sumR)*(_weights ? (*_weights)[pos]:1);
		}
		return -sumL;
	};
};


#endif
