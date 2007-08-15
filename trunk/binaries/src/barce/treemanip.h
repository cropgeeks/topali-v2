#ifndef TREEMANIP_H_
#define TREEMANIP_H_

/*==============================================================*
 *			treemanip.h				*
 *      Copyright (c) Grainne McGuire,  2001			*
 *		      Version 1.00b, BARCE	       		*
 *								*
 * Header file for the tree manipulations functions. Classes in *
 * the related cc files initialise the tree and implement the   *
 * MCMC algorithm. The likelihood calculation is also included  *
 * here as a base class, with static data members, allowing for *
 * straightforward posterior probability calculations		*
 *==============================================================*/

#if defined(USING_ANSI_CPP_)

#include <cmath>
#include <iostream>
#include <cstdlib>
#include <fstream>
#include <iomanip>

#else

#include <math.h>
#include <iostream>
#include <stdlib.h>
#include <fstream>
#include <iomanip>

#endif

#include "basics.h"
#include "data.h"
#include "mathlib.h"

using namespace std;

typedef enum { JC, F81, K2P, F84 } Model;
// which type of model to use. 
// extern float sbunifreg;   remove??

//const float UR=0.2;  /* UR is with of uniform for scaling branchlengths */
const double AMCH=0.05; /* increment for adjusting uniform */
const double sbprop=1.0;  // prob that a branch is changed.

extern int SCALE;     //0.2;   0.4
extern int SHAPE;
const double MEANI=2.0;   // this implies scale=2*shape

extern const double INIT_DELTA_PI;
extern const double INIT_DELTA_ALPHA;
extern const double INIT_DELTA_LAMBDA;
extern const double INIT_DELTA_BRANCH;
extern double sbunifreg;
const double MAX_ALPHA=2;
const double MAX_BRANCH=1;

/*==================================================================*
 *		      class InitTree				    *
 * contains the definitions of the functions used to initialise the *
 * tree structure. Since this code applies to 4 sequences only, the *
 * three possible topologies are hardcoded. Since Tree is a base    *
 * class to InitTree, the tree can be recovered from this class     *
 * (done via a Tree* pointer in prog.cc)			    *
 *==================================================================*/

class InitTree : public Tree
{
public:
InitTree(double bl=0.0);
~InitTree() { }

 void BlValue(double bl) { init_bl = bl; }
 void InitialiseTree();
 double init_bl;  
};

/*=======================================================================*
 *			class CalcProb					 *
 * This class holds the likelihood and posterior probability calculation *
 * functions. It exists as a base class to all the MCMC proposal classes *
 * since (obviously) they all need to calculate the posterior            *
 * probability of the proposed new value. The data members and some of   *
 * the functions are declared as static. For the data members this means *
 * that each one has the same value in all instances of the class        *
 * CalcProb. This is very desirable since it means that the updated      *
 * likelihoods etc do not have to be passed between the MCMC proposal    *
 * classes.								 *
 *=======================================================================*/

class CalcProb
{
public:
CalcProb() {  }
~CalcProb();
 void   CalcLik(Node**, const int);
 int    GetSeqlen() { return seqlen; }
 static Model  GetModel() { return evomod; }

 // static function members for setting static data values
 static void    SetSpace();   // sets up space for storing ts probabilities
 static void    SetSeqlen(int sl) { seqlen=sl; }
 static void    SetConcatlen(int csl) { concatlen=csl; }
 static void    SetSorted(int* s);
 static void    SetWeight(int* w);
 static void    SetPi(double* p);
 static void    SetModel(Model mod) { evomod=mod; }
 static void    SetTstv(double tr);
 static void    SetLoglik();  // also creates space for concatll

 static void    UpdateConstants(const int wt);  // calculated E, G, H etc
 static void    SetF84Params(const int wt);
 static void    CalcTstv(int wt);

 static double* GetPrior() { return prior; }
 static void    InitPrior() { prior[0]=prior[1]=prior[2]=0.0; }
 static double  Getlh() { return lhood; }
 static double  lhood;

protected:
 void   CalcLikRec(Node*, int site, const int wt);
 void   CalcDP(Node* node, int site, const int wt);
 double StatResProb(char, int, const int);  // for all models
 void   PriorBl(Node*, double&);  // calculates prior prob of branch lengths

 void   JCprob(double* clik, Node* node, bool reuse, const int wt);
 double CalcJCprobs(int par, int child, double bl, int nn, const int wt);
 double CalcRP(Node* node, int site, const int wt);

 void   F81prob(double* clik, Node* node, bool reuse, const int wt);
 double CalcF81probs(int par, int child, double bl, int nn, const int wt);

 void   K2Pprob(double* clik, Node* node, bool reuse, const int wt);
 double CalcK2Pprobs(int par, int child, double bl, int nn, const int wt);

 void   F84prob(double* clik, Node* node, bool reuse, const int wt);
 double CalcF84probs(int par, int child, double bl, int nn, const int wt);

 void   SetGamma(int);

 static int    seqlen;
 static double scale, shape;   // gamma parameters
 static double ***transprob;    // for JC and K2P for each of the trees
 static double ****ftransprob;  // for F81 and F84
 static int    numnodes;
 static Model  evomod;
 static double alpha[3], gamma[3];   // for F84 model 
 static double *tstv; // F84 parameter ratios (3 trees)
 // static double E[3], A[3], B[3], C[3];    // F81 and F84, K2P parameters
 static double E, A, B, C;
 // static double **pi;   // ( one set for each tree)
 static double *pi;
 static double **concatll;  // stores concatenated log likelihoods
 static double **loglik;  // stores log likelihoods for each tree
 static int*   sorted;    // holds order of original sites
 static int*   weight;    // holds weight of each concatenated site
 static int    concatlen;  // length of concatenated data
 static double prior[3];    // stores prior probs (freqs, bl)
 static double sharedprior;
 static double oldprior[3];
 static double minpi;
};

/*================================================================*
 *			class HmmCalc			          *
 * This class operates both as a base class to the other proposal *
 * classes, thereby allowing the posterior probability to be      *
 * calculated for a given sequence of topologies		  *
 *================================================================*/

class HmmCalc : public CalcProb
{
 public:
HmmCalc(int sl=0);
~HmmCalc();

 static void SetSpaceH();
 static void GenRandomSeq();
 static void ReadInitialMosaicStructure(); // TDH
 static void PrintStateSequence(); // TDH
 static void GetTPvalues(double*, double);   // sets frequencies, lambda
 static int* GetCurtop() { return curtop; }
 static double GetLPD() { return hmmprob; }
 void SetLvec();
 void SetLvec(double**, int);
 void CalcPost(bool);
 double CalcHmmProb(double**);
 // void CalcMAP();

 protected:
bool IsElementOf(int*, int);

                     // assume all the quantities below are logs
 static double** lhvec;    // contains lhoods (times prior for branchlengths)
 static double   lambda;
 static double   freq[3];  // holds frequencies of the tree topologies
 static double   tp[3][3]; // transition matrix between the three topologies
 static double   hmmprob;  // posterior prob for hmm model
 static int*     curtop;   // contains current topology sequence
 static bool     lambda_update;  // true if lambda updated
};

/*====================================================================*
 *			class ChangeLambda			      *

 * This class changes the value of lambda, the difficulty of changing *
 * topology by sampling lambda from a uniform distribution, centred   *
 * at the current value						      *
 *====================================================================*/

class ChangeLambda : public HmmCalc
{
 public:
  ChangeLambda();
  ~ChangeLambda() { }

 /* Grainne's functions */
 //bool    MHaccept();
 void   ChangeBurn(bool b=false) { burn=b; }
 void   SetTuningint(int ti) { tune=ti; }
 void   SetLambdafile(char* lf);
 void   Print_Out() { print_out=true; }
 /* Dirk's functions */
 void PriorBeta(double meanLambda);
 void UpdatePosteriorBeta();
 void SampleNewLambda();
 void SampleNewLambda(long int nCurrent,long int nBurnIn, char* flagAnneal);
 /* Grainne's functions modified by Dirk*/
 void   UpdateTPprobs();

 private:
 //void   GenNewLambda();
 void   LambdaWrite();
 //void   AdjustDelta();

 /* Grainne's variables */
 int    nacc, ntot;
 double oldlambda;
 double delta;
 ofstream outlambda;
 bool    burn;
 bool    print_out;
 double newlh, oldlh;
 int    tune;
 char   lambdafile[26];

 /* Dirk's variables */
 double alphaPrior;
 double betaPrior;
 double alphaPosterior;
 double betaPosterior;
};

/*=================================================================*
 *			  class ChangeTop			   *
 * This class proposes a change in the sequence of topologies via  *
 * a Gibbs sampling algorithm. It treats the topology at each site *
 * as a separate parameter and finds the full conditional distn    *
 * for this parameter and samples from it.			   *
 *=================================================================*/

/* Changes made by Dirk Husmeier, 17 May 2001 */

class ChangeTop: public HmmCalc
{
 public:
ChangeTop() { site=0; forward=true; }
~ChangeTop() { }

 void GibbsProb();
 int SingleSiteGibbsSample(int t);  //TDH
 void GibbsSampleOfStateSequence(int nCycles); //TDH

 protected:
 int site;
bool forward; 
};

/*====================================================================*
 *			class ChangePi				      *
 * This class proposes new values of the character frequencies using  *
 * a dirichlet distribution centred on the current values. To achieve *
 * this, the parameters of that Dirichlet are a constant times the    *
 * current stationary frequency. The larger the constant, the closer  *
 * the proposed values will be to the existing ones. Obviously, when  *
 * tuning this distribution, if the acceptance ratio is too low, this *
 * constant is increased. The initial value of this constant (delta)  *
 * is set in prog.cc						      *
 * In the presence of a small amount of data, it appears that this    *
 * proposal distribution can lead to some of the frequencies getting  *
 * very small. I've implemented a fix suggested by Marc Beaumont (see *
 * GenNewpi() for details and a catch to stop any values of zero      *
 * begin proposed - these lead to errors since taking log(0) arises)  *
 * However, I'm not sure that the fix works particularly well in this *
 * case so recommend not updating the frequencies in small data sets. *
 *====================================================================*/

class ChangePi : public HmmCalc
{
 public:
ChangePi(int wt=0);  // wt=which tree to change
~ChangePi();

bool    MHaccept(Tree* t);
 void   ChangeBurn(bool b=false) { burn=b; }
 void   SetTuningint(int ti) { tune=ti; }
 void   SetPifile(char* pf);
 void   Print_Out() { print_out=true; }
 void   SetWT(int wt) { wtree=wt; }

 int    nacc, ntot;

 private:
 void   GenNewpi();
 void   GetProbs();
 void   PiWrite();
 void   AdjustDelta();

 int    wtree;   /* which tree's parameters to change */
 double *beta;    // DIRICHLET parameters
 double oldgamma;
 double *oldpi;
 double delta;   // factor to multiply pis by for Dirichlet
 double qxy, qyx;
ofstream outpi;
 char   pifile[26];
bool    burn;
 double newlh, oldlh;
 int    tune;
bool    print_out;  // true if time for pi values to be printed
bool    illegal;    // true if a pi value of 0.0 proposed
};

/*==================================================================*
 *			class ChangeAlpha			    *
 * This class proposes changes to alpha, the transition bias in the *
 * F84 (and K2P equivalent) models. Since alpha and gamma have a    *
 * fixed relationship, changing alpha also changes gamma.	    *
 * New values are proposed by centering a uniform interval around   *
 * the present value of alpha. If necessary, this interval can be   *
 * reflected back into the range of valid values of alpha. This     *
 * range is (0,2). Theoretically, the upper bound depends on the    *
 * values of the stationary frequencies, but since these change it  *
 * is easier to impose a limit like 2. In practice this range       *
 * allows for no transition bias to very extreme bias and alpha is  *
 * unlikely ever to get close to two.				    *
 *==================================================================*/

class ChangeAlpha : public HmmCalc
{
 public:
ChangeAlpha();
~ChangeAlpha() { }

bool    MHaccept(Tree*);
 void   ChangeBurn(bool b=false) { burn=b; }
 void   SetTuningint(int ti) { tune=ti; }
 void   SetTstvfile(char* tf);
 void   Print_Out() { print_out=true; }

 private:
 void   GenNewalpha();
 void   TstvWrite();
 void   AdjustDelta();

 int    nacc, ntot;
 int    wtree;
 double oldalpha, oldgamma;
 double delta;
ofstream outtstv;
bool    burn;
 double newlh, oldlh;
bool    print_out;
 int    tune;
 char   tstvfile[26];
};


/*==============================================================*
 *			class ScaleBranch		        * 
 * This class scales the branches in one of the three trees by  *
 * choosing a new value centered around the current one. Should *
 * any of this interval fall outside (0,1) then it is reflected *
 * back into range.						*
 *==============================================================*/

class ScaleBranch : public HmmCalc
{
 public:
ScaleBranch();
~ScaleBranch() { }

bool MHaccept(Tree* t);
 void ChangeBurn(bool b=false) { burn=b; }
 void SetTuningint(int ti) { tune=ti; }
 void SetSBur(float ur) { sbur=ur; }
 
 int nacc, ntot;

 private:
 void   ChangeBranches(Tree* t, int wt);
 void   DoChanges(Node* n);
 void   RevertBranches(Node* n);
 void   AdjustDelta();

 double epsilon;
 double delta;

 double oldlh;
 double newlh;
 double  sbur;  // variable controlling branch length changing
bool    burn;  // true if still in burn-in
 int    tune;
};

/*=====================================================================*
 *			class TreeCalc				       *
 * This is a general all-purpose class in a way. It contains functions *
 * to set things up (eg assign the data to the leaf nodes, assign the  *
 * initial character frequencies etc). It also has the various MH      *
 * proposal classes and Gibbs sampling class as data members, thereby  *
 * allowing the MH sampling to be easily carried out from prog.cc once * 
 * a TreeCalc object has been created.				       *
 *=====================================================================*/

class TreeCalc
{
public:
TreeCalc() : steps(0), naccept(0), tree(0), seqdata(0)
 { }
TreeCalc(Tree*, ReadData*);
~TreeCalc();

 void     SetValues(Tree*, ReadData*);
 void     UpdatePi(bool b)   { updatepi=b; }
bool      UpdatePi()         { return updatepi; }
 void     UpdateTstv(bool b) { updatetstv=b; }
bool      UpdateTstv()       { return updatetstv; }

   /* TDH, 6 July 2001 */
 void     UpdateLambda(bool b) { updateLambda=b; }
bool      UpdateLambda()       { return updateLambda; } 

 void     CreateNodeArrays(Node* n, int sl);
 void     FreeNodeArrays(Node* n);
 void     AssignOuterData(Node*);
 void     NumberInfo();
Tree*     GetTree() { return tree; }
ReadData* GetData() { return seqdata; }

ChangePi     changepi;
ChangeAlpha  changealpha;
ChangeTop    changetop;
ScaleBranch  scalebranch;
ChangeLambda changelambda;

 int       steps;    /* total number of steps made to date */
 int       naccept;  /* number of accepted proposals to date */

private:

Tree*      tree;
ReadData*  seqdata;
 int       pos;
bool       updatepi;
bool	   updatetstv;
bool	   updateLambda;   /* TDH, 6 July 2001 */
};

#endif
