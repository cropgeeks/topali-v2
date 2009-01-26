#include "parameters.h"
#include "util.h"
#include "runsettings.h"
#include "sitedata.h"
#include "matrixMath.h"

#include <iostream>
#include <math.h>
#include <iomanip>
#include <float.h>

Parameters::Parameters(RunSettings& rs, const SiteData& sd) :
  numTags(rs.getNumTags()),
  model(rs.getModel()),
  useInvariantSites(rs.getUseInvariantSites()),
  p(numTags)
{
  /* Rescale initialTheta if weighted mean is not 1 */

  const double tolerance = 1.0e-14;
  double denom = 0;
  for(int i1=0;i1<numTags;i1++)
    denom += rs.getInitialTheta(i1) * sd.getTagSites(i1);
  denom /= sd.getNumSites();
  rs.scaleInitialTheta(denom);
  if(fabs(denom-1.0) > tolerance)
    error << "Warning: Initial theta values rescaled so weighted average "
	  << "equals 1." << endError;

  int r;
  /* Rescale initialR if they do not sum to 6 */
  if (model == RSModel::GREV) {
    double sum;
    for(int i=0; i<numTags;i++) {
      sum = 0;
      for(r=0;r<NUM_RVALUES;r++)
	sum += rs.getInitialR(r)[i];
      denom = sum / 6.0;
      rs.scaleInitialR(i, denom);
      if(fabs(sum-6.0) > tolerance)
	error << "Warning: Initial r values rescaled so the values sum "
	  << "to 6." << endError;
    }
  }

  for(int i2=0;i2<numTags;i2++) {
    p[i2].kappa = rs.getInitialKappa(i2);
    p[i2].theta = rs.getInitialTheta(i2);

    denom = 0;

    if(!rs.getEstimatePi()) {
      /* Rescale initialPi* if sum is not 1 in each category */
      for(int b1=0;b1<NUM_BASES;b1++)
	denom += rs.getInitialPi(b1)[i2];
      for(int b2=0;b2<NUM_BASES;b2++) 
	p[i2].pi[b2] = rs.getInitialPi(b2)[i2] / denom;
      if(fabs(denom-1.0) > tolerance)
	error << "Warning: Initial pi values in category " << 
	  rs.getCategories(i2) << " rescaled to sum to 1." << endError;
    }
    else {
      /* If a base is missing for the entire data (a rare occurrence, we hope)
	 then bump up all base counts by 1 to avoid 0's in the transition
	 table. */
      int missingBase = 0;
      for(int b=0;b<NUM_BASES;b++) {
	denom += sd.getTagSymbols(i2)[0x1 << b];
	if(sd.getTagSymbols(i2)[0x1 << b]==0)
	  missingBase = 1;
      }
      if(missingBase) 
	for(int b=0;b<NUM_BASES;b++)
	  p[i2].pi[b] = (double)(sd.getTagSymbols(i2)[0x1 << b]+1)
	    / (denom+NUM_BASES);
      else
	for(int b=0;b<NUM_BASES;b++)
	  p[i2].pi[b] = (double)(sd.getTagSymbols(i2)[0x1 << b] / denom);
    }

    p[i2].pi_r = p[i2].pi[0] + p[i2].pi[1];
    p[i2].pi_y = p[i2].pi[2] + p[i2].pi[3];
    p[i2].pry = p[i2].pi_r / p[i2].pi_y;
    p[i2].pyr = p[i2].pi_y / p[i2].pi_r;

    p[i2].invariantProb = rs.getInvariantProb(i2);
    calculateAlphaBeta(rs.getInvariantPriorMean(i2),rs.getInvariantPriorSD(i2),
		       i2);
    switch(rs.getModel()) {
    case RSModel::HKY85 :
      p[i2].ttp = p[i2].kappa/2;
      p[i2].gamma = 1;
      break;
    case RSModel::F84 :
      p[i2].ttp = (p[i2].kappa + 2 * p[i2].pi_r * p[i2].pi_y) 
	/ (4 * p[i2].pi_r * p[i2].pi_y);
      p[i2].gamma = ((p[i2].kappa + p[i2].pi_y) * p[i2].pi_r) /
	((p[i2].kappa + p[i2].pi_r) * p[i2].pi_y);
      break;
    case RSModel::TN93 : 
      p[i2].ttp = rs.getInitialTtp(i2);
      p[i2].gamma = rs.getInitialGamma(i2);
      break;
    case RSModel::GREV :
	  for(r=0;r<NUM_RVALUES;r++)
	    p[i2].rValues[r] = rs.getInitialR(r)[i2];

	  p[i2].ttp = 1;
	  p[i2].gamma = 1;
	  p[i2].kappa = 1;
	  break;
    default : 
      error << "Internal error: Unknown probability model (" << rs.getModel()
	    << ")" << endError;
      quit(1);
    }
  }
}

void Parameters::calculateAlphaBeta(double mean, double SD, int index)
{
  double var = SD * SD;

  if(SD > mean*(1-mean)) {
    error << "Error: invariant-prob-prior-sd (" << SD 
      << ") must be strictly positive and strictly less than " 
      << mean*(1-mean) << "." << endError;
    quit(1);
  }

  p[index].alpha = (mean*mean*(1-mean))/var - mean;
  p[index].beta = (mean*(1-mean))/var - p[index].alpha - 1;
}

Parameters& Parameters::operator=(const Parameters& old)
{
  /* Copies old to this. */
  for(int i=0;i<numTags;i++) {
    p[i].kappa = old.p[i].kappa;
    p[i].theta = old.p[i].theta;
    p[i].ttp = old.p[i].ttp;
    p[i].gamma = old.p[i].gamma;
    for(int b=0;b<NUM_BASES;b++)
      p[i].pi[b] = old.p[i].pi[b];
    p[i].pi_r = old.p[i].pi_r;
    p[i].pi_y = old.p[i].pi_y;
    p[i].pry =  old.p[i].pry;
    p[i].pyr =  old.p[i].pyr;
    for(int r=0;r<NUM_RVALUES;r++)
      p[i].rValues[r] = old.p[i].rValues[r];
    p[i].invariantProb = old.p[i].invariantProb;
    p[i].alpha = old.p[i].alpha;
    p[i].beta = old.p[i].beta;
  }
  return *this;
}

Parameters::Parameters(const Parameters& old) :
  numTags(old.numTags),
  model(old.model),
  useInvariantSites(old.useInvariantSites),
  p(numTags)
{
  *this = old;
}

Parameters::Parameters(const RunSettings& rs, const SiteData& sd,
		       const Parameters& old) :
  numTags(rs.getNumTags()),
  model(rs.getModel()),
  useInvariantSites(rs.getUseInvariantSites()),
  p(numTags)
{
  // The newly created set of parameters is an update of the old one.

  Vector<double> alpha1(numTags);
  Vector<double> newtheta(numTags);
  Vector<double> alpha2(NUM_BASES),newpi(NUM_BASES);
  Vector<double> rValues1(NUM_RVALUES), newR(NUM_RVALUES);

  if(rs.getUpdateKappa()) {
    p[0].kappa = fabs(old.p[0].kappa+(2*Rand::runif()-1)*rs.getKappaWin());
    for(int i=1;i<numTags;i++) 
      if(rs.getSingleKappa())
	p[i].kappa = p[0].kappa;
      else
	p[i].kappa = fabs(old.p[i].kappa+(2*Rand::runif()-1)*rs.getKappaWin());
  }
  else
    for(int i=0;i<numTags;i++)
      p[i].kappa = old.p[i].kappa;
  
  if(rs.getUpdateTheta()) {
    for(int i1=0;i1<numTags;i1++)
      alpha1[i1] = rs.getThetaConst() * old.p[i1].theta * sd.getTagWeight(i1);
    Rand::rdirich(alpha1,newtheta,numTags);
    for(int i2=0;i2<numTags;i2++)
      p[i2].theta = newtheta[i2] / sd.getTagWeight(i2);
  }
  else
    for(int i=0;i<numTags;i++)
      p[i].theta = old.p[i].theta;
  
  if(rs.getUpdatePi()) {
    for(int i=0;i<numTags;i++) {
      for(int b1=0;b1<NUM_BASES;b1++)
	alpha2[b1] = rs.getPiConst() * old.p[i].pi[b1];
      Rand::rdirich(alpha2,newpi,NUM_BASES);
      for(int b2=0;b2<NUM_BASES;b2++)
	p[i].pi[b2] = newpi[b2];
      p[i].pi_r = p[i].pi[0] + p[i].pi[1];
      p[i].pi_y = p[i].pi[2] + p[i].pi[3];
      p[i].pry = p[i].pi_r / p[i].pi_y;
      p[i].pyr = p[i].pi_y / p[i].pi_r;
    }
  }
  else {
    for(int i=0;i<numTags;i++) {
      for(int b=0;b<NUM_BASES;b++)
	p[i].pi[b] = old.p[i].pi[b];
      p[i].pi_r = p[i].pi[0] + p[i].pi[1];
      p[i].pi_y = p[i].pi[2] + p[i].pi[3];
      p[i].pry = p[i].pi_r / p[i].pi_y;
      p[i].pyr = p[i].pi_y / p[i].pi_r;
    }
  }

  // These are updated individually in updateInvariantProb
  for(int i=0;i<numTags;i++) {
    p[i].invariantProb = old.p[i].invariantProb;
  }

  int r;
  for(int i=0;i<numTags;i++) {
    for(r=0;r<NUM_RVALUES;r++)
      p[i].rValues[r] = old.p[i].rValues[r];

    switch(rs.getModel()) {
    case RSModel::HKY85 : 
      p[i].ttp = p[i].kappa/2;
      p[i].gamma = 1;
      break;
    case RSModel::F84 : 
      p[i].ttp = (p[i].kappa + 2 * p[i].pi_r * p[i].pi_y) 
	/ (4 * p[i].pi_r * p[i].pi_y);
      p[i].gamma = ((p[i].kappa + p[i].pi_y) * p[i].pi_r) 
	/ ((p[i].kappa + p[i].pi_r) * p[i].pi_y);
      break;
    case RSModel::TN93 :
      if(rs.getUpdateTtp())
	for(int i1=0;i1<numTags;i1++) 
          p[i1].ttp = fabs(old.p[i1].ttp + (2*Rand::runif()-1)*rs.getTtpWin());
      else
        for(int i2=0;i2<numTags;i2++)
          p[i2].ttp = old.p[i2].ttp;
      if(rs.getUpdateGamma())
        for(int i3=0;i3<numTags;i3++) 
          p[i3].gamma = fabs(old.p[i3].gamma 
			    + (2*Rand::runif()-1)*rs.getGammaWin());
      else
        for(int i4=0;i4<numTags;i4++)
          p[i4].gamma = old.p[i4].gamma;
      break;
    case RSModel::GREV :
      if(rs.getUpdateGrev()) {
	for(int i=0;i<numTags;i++) {
	  for(int r1=0;r1<NUM_RVALUES;r1++)
	    rValues1[r1] = rs.getGrevTune() * old.p[i].rValues[r1];
	  Rand::rdirich(rValues1,newR,NUM_RVALUES);
	  for(int r2=0;r2<NUM_RVALUES;r2++)
	    p[i].rValues[r2] = NUM_RVALUES * newR[r2];
	}
      }
      else {
	for(r=0;r<NUM_RVALUES;r++)
	  p[i].rValues[r] = old.p[i].rValues[r];
      }
      p[i].ttp = 1.0;
      p[i].gamma = 1.0;
      p[i].kappa = 1.0;
      break;
    default :
      error << "Internal error: Unknown probability model (" << rs.getModel()
	    << ")" << endError;
      quit(1);
    }
  }
}

void Parameters::updateInvariantProb(const RunSettings& rs, const Parameters& old)
{
  Vector<double> prob(TWO), newProb(TWO);
  for(int i=0;i<numTags;i++) {
    prob[0] = rs.getInvariantProbTune() * old.p[i].invariantProb;
    prob[1] = rs.getInvariantProbTune() * (1-old.p[i].invariantProb);
    Rand::rdirich(prob,newProb,TWO);
    p[i].invariantProb = newProb[0];
  }
}

double Parameters::hastingsPi(double c, const Parameters& y, int n, int k)
/* c is the constant where y ~ Dirichlet(cx)
   x is the current state (assumed that sum x = 1)
   y is the proposed state (assumed that sum y = 1)
   k is the length of each array
   return q(y,x) / q(x,y)
*/
{
  double prod = 1;
  for(int j=0;j<n;j++) {
    double lhr = 0.0;
    for(int i=0;i<k;i++) {
      double cx = c*p[j].pi[i];
      double cy = c*y.p[j].pi[i];
      lhr += (cy-1)*log(p[j].pi[i]) - (cx-1)*log(y.p[j].pi[i])
	- Rand::lgamma(cy) + Rand::lgamma(cx);
    }
    prod *= SAFE_EXP(lhr);
  }
  return prod;
}

double Parameters::hastingsR(double c, const Parameters& y, int n, int k)
/* c is the constant where y ~ Dirichlet(cx)
   x is the current state (assumed that sum x = 6)
   y is the proposed state (assumed that sum y = 6)
   k is the length of each array
   return q(y,x) / q(x,y)
*/
{
  double prod = 1;
  for(int j=0;j<n;j++) {
    double lhr = 0.0;
    for(int i=0;i<k;i++) {
      double cx = c*p[j].rValues[i];
      double cy = c*y.p[j].rValues[i];
      lhr += (cy-1)*log(p[j].rValues[i]) - (cx-1)*log(y.p[j].rValues[i])
	- Rand::lgamma(cy) + Rand::lgamma(cx);
    }
    prod *= SAFE_EXP(lhr);
  }
  return prod;
}

double Parameters::hastingsP(double c, const Parameters& y, int n)
/* c is the constant where y ~ Dirichlet(cx)
   x is the current state (assumed that sum x = 1)
   y is the proposed state (assumed that sum y = 1)
   n is the number of categories
   return q(y,x) / q(x,y)
*/
{
  double sum = 0;
  for(int j=0;j<n;j++) {
    double lhr = 0.0;
    double probX = p[j].invariantProb;
    double probY = y.p[j].invariantProb;
    double cx = c*probX;
    double cMinuscx = c - cx;
    double cy = c*probY;
    double cMinuscy = c - cy;
    lhr += (cy - p[j].alpha)*log(probX) - (cx - p[j].alpha)*log(probY)
      + (cMinuscy - p[j].beta)*log(1-probX) - (cMinuscx - p[j].beta)*log(1-probY)
      - Rand::lgamma(cy) + Rand::lgamma(cx)
      - Rand::lgamma(cMinuscy) + Rand::lgamma(cMinuscx);
    sum += lhr;
  }
  return SAFE_EXP(sum);
}



double Parameters::hastingsTheta(double c, const Vector<double> w,
				 const Parameters& y, int k)
/* 
   c is the constant where w*y ~ Dirichlet(c(w*x))
   w is the array of weights such that sum wx = sum wy = 1
     (it is also true that sum w = 1, but this isn't necessary)
   x is the current state
   y is the proposed state
   k is the length of each array
   return q(y,x) / q(x,y)
*/
{
  double lhr = 0.0;
  for(int i=0;i<k;i++) {
    double wx = w[i]*p[i].theta;
    double wy = w[i]*y.p[i].theta;
    double cwx = c*wx;
    double cwy = c*wy;
    lhr += (cwy-1)*log(wx) - (cwx-1)*log(wy) - Rand::lgamma(cwy) 
      + Rand::lgamma(cwx);
  }
  return SAFE_EXP(lhr);
}

void Parameters::findPtable(Vector<BaseArray>& ptab, double blen) const
{
  if (model == RSModel::GREV)
    findPtableGRev(ptab, blen);
  else
    findPtableReg(ptab, blen);
}

void Parameters::findPtableGRev(Vector<BaseArray>& ptab, double blen) const
{
  Vector<BaseArray> Qt(NUM_BASES), eQT(NUM_BASES);
  MatrixMath matrixMath;
  int row, col;
  double scalar;
 
  for(int i=0,m=0;i<numTags;i++,m+=NUM_BASES) {
    scalar = p[i].theta * blen;
    // A G C T
    Qt[0][1] = p[i].pi[1] * p[i].rValues[RAG] * scalar; // piG * rAG
    Qt[0][2] = p[i].pi[2] * p[i].rValues[RAC] * scalar; // piC * rAC
    Qt[0][3] = p[i].pi[3] * p[i].rValues[RAT] * scalar; // piT * rAT
    Qt[0][0] = -(Qt[0][1] + Qt[0][2] + Qt[0][3]);

    Qt[1][0] = p[i].pi[0] * p[i].rValues[RAG] * scalar; // piA * rAG
    Qt[1][2] = p[i].pi[2] * p[i].rValues[RCG] * scalar; // piC * rCG
    Qt[1][3] = p[i].pi[3] * p[i].rValues[RGT] * scalar; // piT * rGT
    Qt[1][1] = -(Qt[1][0] + Qt[1][2] + Qt[1][3]);

    Qt[2][0] = p[i].pi[0] * p[i].rValues[RAC] * scalar; // piA * rAC
    Qt[2][1] = p[i].pi[1] * p[i].rValues[RCG] * scalar; // piG * rCG
    Qt[2][3] = p[i].pi[3] * p[i].rValues[RCT] * scalar; // piT * rCT
    Qt[2][2] = -(Qt[2][0] + Qt[2][1] + Qt[2][3]);

    Qt[3][0] = p[i].pi[0] * p[i].rValues[RAT] * scalar; // piA * rAT
    Qt[3][1] = p[i].pi[1] * p[i].rValues[RGT] * scalar; // piG * rGT
    Qt[3][2] = p[i].pi[2] * p[i].rValues[RCT] * scalar; // piC * rCT
    Qt[3][3] = -(Qt[3][0] + Qt[3][1] + Qt[3][2]);

    matrixMath.ComputeMatrixExponential(Qt, eQT);

    // ptab is transposed.
    for(row=0; row < NUM_BASES; row++)
  	  for(col=0; col < NUM_BASES; col++)
	    ptab[m+row][col] = eQT[col][row];
  }
}

void Parameters::findPtableReg(Vector<BaseArray>& ptab, double blen) const
{
  for(int i=0,m=0;i<numTags;i++,m+=NUM_BASES) {
    double btheta = blen * p[i].theta;
    double pia = p[i].pi[0];
    double pig = p[i].pi[1];
    double pic = p[i].pi[2];
    double pit = p[i].pi[3];
    double pir = p[i].pi_r;
    double piy = p[i].pi_y;
    double pry = p[i].pry;
    double pyr = p[i].pyr;
    double ttp = p[i].ttp;
    double gamma = p[i].gamma;

    double px = SAFE_EXP(-1 * btheta);
    double q = 1 - px;
    double ebetar = SAFE_EXP(-1 * btheta * 
			     ((4 * ttp * pir) / (gamma + 1) + piy));
    double ebetay = SAFE_EXP(-1 * btheta * 
			     ( (4 * ttp * gamma * piy) / (gamma + 1) + pir));
    double tempr1 = 1 + pyr * px;
    double tempy1 = 1 + pry * px;
    double tempr2 = (1 + pyr * px - ebetar / pir);
    double tempy2 = (1 + pry * px - ebetay / piy);

    ptab[m+0][0] = pia*tempr1 + (pig/pir)*ebetar;
    ptab[m+0][1] = pia*tempr2;
    ptab[m+0][2] = ptab[m+0][3] = pia*q;
    ptab[m+1][0] = pig*tempr2;
    ptab[m+1][1] = pig*tempr1 + (pia/pir)*ebetar;
    ptab[m+1][2] = ptab[m+1][3] = pig*q;
    ptab[m+2][0] = pic*q;
    ptab[m+2][1] = ptab[m+2][0];
    ptab[m+2][2] = pic*tempy1 + (pit/piy)*ebetay;
    ptab[m+2][3] = pic*tempy2;
    ptab[m+3][0] = ptab[m+3][1] = pit*q;
    ptab[m+3][2] = pit*tempy2;
    ptab[m+3][3] = pit*tempy1 + (pic/piy) * ebetay;
  }
}

void Parameters::print(ostream& f, const RunSettings& rs, int cycle, 
		       int accept, int kaccept, int paccept, double ll, 
		       double radius, int interval) const
{
  f << setiosflags(ios::showpoint | ios::fixed) << setw(6) << cycle << ' ' 
    << setw(6) << setprecision(1) << ll << ' ' << setw(1) << setprecision(3)
    << (double)accept/(double)interval << ' ' << setw(1) 
    << (double)kaccept*(double)rs.getParamUpdateInterval()/(double)interval
    << ' ' << setw(1)
    << (double)paccept*(double)rs.getParamUpdateInterval()/(double)interval
    << ' ' << setprecision(6) << radius << ' ' << setw(1) << setprecision(6)
    << rs.getValleyWin() << ' ';

  for(int i=0;i<rs.getNumTags();i++) {
    f << p[i].invariantProb << ' ';
    f << p[i].theta << ' ' << p[i].kappa << ' ' << p[i].ttp << ' ' 
      << p[i].gamma << ' ';
    for(int b=0;b<NUM_BASES;b++) 
      f << p[i].pi[b] << ' ';
    for(int r=0;r<NUM_RVALUES;r++)
      f << p[i].rValues[r] << ' ';
  }
  f << endl;
}

void Parameters::printToScreen(ostream& f, const RunSettings& rs, int cycle, 
		       int accept, int kaccept, int paccept, double ll, 
		       double radius, int interval) const
{
  f << setiosflags(ios::showpoint | ios::fixed) << setw(6) << cycle << ' ' 
    << setw(6) << setprecision(1) << ll << ' ' << setw(1) << setprecision(3)
    << (double)accept/(double)interval << ' ' << setw(1) 
    << (double)kaccept*(double)rs.getParamUpdateInterval()/(double)interval
    << ' ' << setw(1)
    << (double)paccept*(double)rs.getParamUpdateInterval()/(double)interval
    << ' ' << setprecision(6) << radius ;
  if (rs.getBurnAlgorithm() == RSAlgorithm::GLOBAL ||
      rs.getMainAlgorithm() == RSAlgorithm::GLOBAL)
    f << ' ' << setw(1) << setprecision(6) << rs.getValleyWin() << endl;
  else
    f << endl;

  if (rs.getUpdateInvariantProb()) {
    f << "invariant-probability=";
    for(int i=0;i<numTags-1;i++) {
      f << p[i].invariantProb << ',';
    }
    f << p[numTags-1].invariantProb << endl;
  }
  f << "theta=";
  for(int i=0;i<numTags-1;i++)
    f << p[i].theta << ',';
  f << p[numTags-1].theta << endl;
  if (model == RSModel::HKY85) {
    f << "kappa=";
    for(int i=0;i<numTags-1;i++) 
      f << p[i].kappa << ',';
    f << p[numTags-1].kappa << endl;
  }
  if (model == RSModel::TN93) {
    f << "ttp=";
    for(int i=0;i<numTags-1;i++) 
      f << p[i].ttp << ',' ;
    f << p[numTags-1].ttp << endl;
    f << "gamma=";
    for(int i=0;i<numTags-1;i++) 
      f << p[i].gamma << ',';
    f << p[numTags-1].gamma << endl;
  }

  f << "pia=";
  for(int i=0;i<numTags-1;i++) 
      f << p[i].pi[0] << ',';
  f << p[numTags-1].pi[0] << endl;
  f << "pig=";
  for(int i=0;i<numTags-1;i++) 
      f << p[i].pi[1] << ',';
  f << p[numTags-1].pi[1] << endl;
  f << "pic=";
  for(int i=0;i<numTags-1;i++) 
      f << p[i].pi[2] << ',';
  f << p[numTags-1].pi[2] << endl;
  f << "pit=";
  for(int i=0;i<numTags-1;i++) 
      f << p[i].pi[3] << ',';
  f << p[numTags-1].pi[3] << endl;

  if (model == RSModel::GREV) {
    f << "rac=";
    for(int i=0;i<numTags-1;i++)   
      f << p[i].rValues[0] << ',';
    f << p[numTags-1].rValues[0] << endl;

    f << "rag=";
    for(int i=0;i<numTags-1;i++)   
      f << p[i].rValues[1] << ',';
    f << p[numTags-1].rValues[1] << endl;

    f << "rat=";
    for(int i=0;i<numTags-1;i++)   
      f << p[i].rValues[2] << ',';
    f << p[numTags-1].rValues[2] << endl;

    f << "rcg=";
    for(int i=0;i<numTags-1;i++)   
      f << p[i].rValues[3] << ',';
    f << p[numTags-1].rValues[3] << endl;

    f << "rct="; 
    for(int i=0;i<numTags-1;i++)   
      f << p[i].rValues[4] << ',';
    f << p[numTags-1].rValues[4] << endl;

    f << "rgt="; 
    for(int i=0;i<numTags-1;i++)   
      f << p[i].rValues[5] << ',';
    f << p[numTags-1].rValues[5] << endl;
  }
}

void Parameters::printInfo(ostream& f, const RunSettings& rs, 
			   const SiteData& sd) const
{
  f << "Category     ";
  for(int b=0;b<NUM_BASES;b++)
    f << SiteData::baseSymbol[0x1<<b] << "       ";
  f << SiteData::baseSymbol[GAP] << "      other" << endl;
  for(int i=0;i<sd.getNumTags();i++) {
    f << setw(3) << rs.getCategories(i) << "    Pi";
    for(int b1=0;b1<NUM_BASES;b1++)
      f << setiosflags(ios::showpoint | ios::fixed) << "  " << setw(6)
	<< setprecision(4) << p[i].pi[b1];
    f << endl << "     Prop";
    int count = sd.getTagSites(i);
    double sum = (double)sd.getTagSymbols(i)[GAP]
      / (double)(count*sd.getNumTaxa());
    double gapprop = sum;
    for(int b2=0;b2<NUM_BASES;b2++) {
      double prop = (double)sd.getTagSymbols(i)[0x1<<b2]
	/ (double)(count*sd.getNumTaxa());
      sum += prop;
      f << setiosflags(ios::showpoint | ios::fixed) << "  " << setw(6) 
	<< setprecision(4) << prop;
    }
    f << setiosflags(ios::showpoint | ios::fixed) << "  " << setw(6)
      << setprecision(4) << gapprop << "  " << setw(6) << 1-sum << endl;
  }
  f << endl;
}

void Parameters::findQ(Vector<BaseArray>& qtab) const
{
  for(int i=0,m=0;i<numTags;i++,m+=NUM_BASES) {
    double theta = p[i].theta;
    double pia = p[i].pi[0];
    double pig = p[i].pi[1];
    double pic = p[i].pi[2];
    double pit = p[i].pi[3];
    double pir = p[i].pi_r;
    double piy = p[i].pi_y;
    double pry = p[i].pry;
    double pyr = p[i].pyr;
    double ttp = p[i].ttp;
    double gamma = p[i].gamma;
    double konst = 4. * ttp / (gamma + 1.);

    qtab[m+0][0] = -1. * (konst * pig + piy);
    qtab[m+0][1] = konst * pia;
    qtab[m+0][2] = qtab[m+0][3] = pia;
    qtab[m+1][0] = konst * pig;
    qtab[m+1][1] = -1. * (konst * pia + piy);
    qtab[m+1][2] = qtab[m+1][3] = pig;
    qtab[m+2][0] = qtab[m+2][1] = pic;
    qtab[m+2][2] = -1. * (konst * gamma * pit + pir);
    qtab[m+2][3] = konst * gamma * pic;
    qtab[m+3][0] = qtab[m+3][1] = pit;
    qtab[m+3][2] = konst * gamma * pit;
    qtab[m+3][3] = -1. * (konst * gamma * pic + pir);
  }
}






