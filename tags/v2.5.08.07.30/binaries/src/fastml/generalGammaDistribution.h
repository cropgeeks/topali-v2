// version 1.00
// last modified 3 Nov 2002

#ifndef ___GENERAL_GAMMA_DIST
#define ___GENERAL_GAMMA_DIST
/************************************************************
This distribution can take several forms depending on its free parameters alpha,beta 
(unalike gammaDist. alpha is not necessarily equal to beta). 
For an extensive exlpanation of this distribution
see http://mathworld.wolfram.com/GammaDistribution.html
************************************************************/
#include "definitions.h"
#include "distribution.h"
class generalGammaDistribution : public distribution {

public:
	explicit generalGammaDistribution(MDOUBLE alpha, MDOUBLE beta, int in_number_of_categories);
	explicit generalGammaDistribution(const generalGammaDistribution& other);
	virtual ~generalGammaDistribution() {};
	void setGammaParameters(int numOfCategories ,MDOUBLE alpha, MDOUBLE beta);

	const int categories() const {return _rates.size();}
	virtual const MDOUBLE rates(const int i) const {return _rates[i]*_globalRate;}
	virtual const MDOUBLE ratesProb(const int i) const {return _ratesProb[i];}
	virtual distribution* clone() const { return new generalGammaDistribution(*this); }
 	virtual void setGlobalRate(const MDOUBLE x) {_globalRate = x;}
 	virtual MDOUBLE getGlobalRate()const {return _globalRate;}
	virtual const MDOUBLE getCumulativeProb(const MDOUBLE x) const;
	void setAlpha(MDOUBLE newAlpha);
	MDOUBLE getAlpha() const {return _alpha;};
	void setBeta(MDOUBLE newBeta);
	MDOUBLE getBeta() const {return _beta;};
	void change_number_of_categories(int in_number_of_categories);
	MDOUBLE getBorder(const int i) const {return _bonderi[i];}	//return the ith border. Note:  _bonderi[0] = 0, _bondery[categories()] = infinite


private:	
	int fill_mean();
	int fill_bonderi();

	MDOUBLE _alpha;
	MDOUBLE _beta;
	vector<MDOUBLE> _bonderi;
	vector<MDOUBLE> _rates;
	vector<MDOUBLE> _ratesProb;
	MDOUBLE _globalRate;


};



#endif

