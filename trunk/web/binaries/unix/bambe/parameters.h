#ifndef PARAMETERSHDR
#define PARAMETERSHDR

#include "util.h"
#include "runsettings.h"
#include "sitedata.h"

/*
 *  typedef struct parameters
 *
 *  pi[b] = p  <==>  the equilibrium proportion of base b is p
 *  kappa      <==>  the rate of transition over the rate of transversion
 */

/* Parameters of the probability model. */

class OneParameter {
 public:
  double kappa, theta, ttp, gamma;
  double pi[NUM_BASES];
  double pi_r, pi_y, pry, pyr;
  double rValues[NUM_RVALUES];
  double invariantProb;
  double alpha, beta;
};

class Parameters {
 public:
  Parameters(RunSettings&, const SiteData&);
  Parameters(const Parameters&);
  Parameters(const RunSettings&,const SiteData&,const Parameters&);
  Parameters& operator=(const Parameters&);
  void calculateAlphaBeta(double, double, int);
  void findPtable(Vector<BaseArray>&, double) const;
  void findPtableReg(Vector<BaseArray>&, double) const;
  void findPtableGRev(Vector<BaseArray>&, double) const;
  int getNumTags () const { return numTags; }
  int getUseInvariantSites() const { return useInvariantSites; }
  void updateInvariantProb(const RunSettings&, const Parameters&);
  const OneParameter& getParam(int i) const { return p[i]; }
  void print(ostream&,const RunSettings&,int,int,int,int,double,double,int) const;
  void printToScreen(ostream&,const RunSettings&,int,int,int,int,double,double,int) const;
  void printInfo(ostream&,const RunSettings&,const SiteData&) const;
  double hastingsPi(double,const Parameters&,int,int);
  double hastingsR(double,const Parameters&,int,int);
  double hastingsP(double,const Parameters&,int);
  double hastingsTheta(double,const Vector<double>,const Parameters&,int);
  void findQ(Vector<BaseArray>&) const;

  enum { RAC=0, RAG, RAT, RCG, RCT, RGT };

 private:
  int numTags;
  int model;			// likelihood model
  int useInvariantSites;
  Vector<OneParameter> p;
};

#endif
