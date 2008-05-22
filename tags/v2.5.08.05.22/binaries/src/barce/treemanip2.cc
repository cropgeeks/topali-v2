/*=================================================================*
 *			treemanip2.cc				   *
 *	Copyright (c) Grainne McGuire, 2001			   *
 *		      Version 1.00b, BARCE   			   *
 *								   *
 * contains definitions for some of the classes declared in	   *
 * treemanip.h. See this file for more information on the contents *
 *=================================================================*/

#include "treemanip.h"

/*============================================================*
 *			class ChangePi			      *
 * definitions of the functions used to propose new character *
 * frequencies. More details in treemanip.h		      *
 *============================================================*/

ChangePi::ChangePi(int wt)
{
 wtree=wt;
 nacc=ntot=0;
 beta=new double[lenalphabet];

 if(!beta) { char mes[]="ChangePi constructor"; OutofMemory(mes); }
 oldpi = new double[lenalphabet];
 if(!oldpi) { char mes[]="ChangePi constructor"; OutofMemory(mes); }
 if(pi)
 {
  for(int i=0; i<lenalphabet; i++) oldpi[i]=pi[i];  // pi in CalcProb
 }
 delta = INIT_DELTA_PI;
 burn=true;
 tune=200;
 print_out=false;
}

//------------------- End of function -------------------//

ChangePi::~ChangePi()
{
 if(beta) { delete [] beta; beta = 0; }
 if(oldpi) { delete [] oldpi; oldpi = 0; }
}

//------------------- End of function -------------------//

void ChangePi::SetPifile(char* pf)
{
 strcpy(pifile, pf);
 outpi.open(pifile, ios::out);
 outpi.close();
}

//------------------- End of function -------------------//

bool ChangePi::MHaccept(Tree* t)
{
 bool update=false;
 wtree=rand()%3;  // deciding which tree's parameters to change
 oldlh=hmmprob;
 GenNewpi();
 if(illegal==true)
 {
  for(int i=0; i<4; i++) pi[i]=oldpi[i];
  update=false;
  ntot++;
  if(print_out == true) PiWrite();
  if(burn==true) { if(ntot%tune==0) AdjustDelta(); }
  return update;
 }

 GetProbs();

 // calculating the posterior
 CalcLik(t->GetRoot(), wtree);
 CalcPost(true);  // use new likelihood values
 newlh=hmmprob;

 double logalpha = newlh + qyx - oldlh - qxy; 
 double Alpha = exp(logalpha);

 double temp=(double)runif(0,1);
 if(temp<Alpha)
 {
  oldlh=newlh;
  update=true;
  nacc++;
  CalcTstv(wtree);
  SetLvec(loglik, wtree);
 }
 else
 {
  int i;
  hmmprob=oldlh;
  gamma[wtree] = oldgamma;
  for(i=0; i<lenalphabet; i++) pi[i]=oldpi[i];
  for(i=0; i<3; i++) prior[i]=oldprior[i];
  for(i=0; i<seqlen; i++) loglik[wtree][i] = lhvec[wtree][i];
  UpdateConstants(wtree);  // changed to fine new gamma
 }
 ntot++;
 if(print_out == true) PiWrite();
 if(burn==true) { if(ntot%tune == 0) AdjustDelta(); }
 return update;
}

//------------------- End of function -------------------//

void ChangePi::GenNewpi()
{
  // calls the function to generate the observation from the 
  // appropriate dirichlet function
  // also stores original values of pi[wtree][] in oldpi[].

 oldgamma = gamma[wtree];

 bool indep=false;
 int i;
 for(i=0; i<4; i++)
 {
  oldpi[i]=pi[i];
  if(pi[i]>minpi) beta[i]=delta*pi[i];
  else { beta[i]=delta*minpi; indep=true; }
 }

 if(indep==true)
 {
  double sum=(beta[0]+beta[1]+beta[2]+beta[3])/delta;
  for(i=0; i<4; i++) beta[i]/=sum;
 }

 rdirich(pi, beta, 4);
 illegal=false;
 for(i=0; i<4; i++)
 {
  if(pi[i]==0.0) { illegal=true; return; }
 }
 SetGamma(wtree);
}

//------------------- End of function -------------------//

void ChangePi::GetProbs()
{
  // calculates the values of qxy and qyx (in logs)
  // note that ddirich return the log value
 qxy=ddirich(pi, beta, lenalphabet);
 int i;
 bool indep=false;
 for(i=0; i<lenalphabet; i++)
 {
  if(pi[i]>minpi) beta[i]=delta*pi[i];
  else { beta[i]=delta*minpi; indep=true; }
 }
 if(indep==true) 
 {
  double sum = (beta[0]+beta[1]+beta[2]+beta[3])/delta;
  for(i=0; i<4; i++) beta[i]/=sum;
 }

 qyx=ddirich(oldpi, beta, lenalphabet);
}

//------------------- End of function -------------------//

void ChangePi::PiWrite()
{
 outpi.open(pifile, ios::app);
 for(int j=0; j<4; j++) outpi << pi[j] << "  ";
 outpi << endl;
 outpi.close();
 print_out=false;
}

//------------------- End of function -------------------//

void ChangePi::AdjustDelta()
{
 double ar = (double)nacc/ntot;
 if(ar<0.05) delta = (delta+2 < 100) ? delta+3 : delta;
 else if(ar>0.7) delta = (delta-2 > 1) ? delta-2 : delta;

 nacc = ntot = 0;
}

/*====================================================================*
 *			class ChangeAlpha			      *
 * Class which implements the functions used to propose new values of *
 * the F84+gaps model parameter alpha. For more details, see          *
 * treemanip.h							      *
 *====================================================================*/

ChangeAlpha::ChangeAlpha()
{
 nacc = ntot = 0;
 delta = INIT_DELTA_ALPHA;   // define in prog.cc
 oldalpha = oldgamma = 0.0;
 burn=true;
 tune=200;
 print_out=false;
 wtree=0;
}

//------------------- End of function -------------------//

void ChangeAlpha::SetTstvfile(char* tf)
{
 strcpy(tstvfile, tf);
 outtstv.open(tstvfile, ios::out);
 outtstv.close();
}

//------------------- End of function -------------------//

bool ChangeAlpha::MHaccept(Tree* t)
{
 wtree=rand()%3;   // which tree's parameters to change
 oldlh=hmmprob;
 GenNewalpha();
 CalcLik(t->GetRoot(), wtree);
 CalcPost(true);   // use new likelihood values
 newlh=hmmprob;

 double logalpha = newlh - oldlh;   // symmetric
 double Alpha = exp(logalpha);

 bool update=false;
 double temp=(double)runif(0,1);
 if(temp<Alpha)
 {
  oldlh=newlh;
  update=true;
  nacc++;
  CalcTstv(wtree);  // new alpha, gamma => new tstv
  SetLvec(loglik, wtree);
 }
 else
 {
  int i;
  hmmprob = oldlh;
  alpha[wtree] = oldalpha;
  gamma[wtree] = oldgamma;
  for(i=0; i<3; i++) prior[i]=oldprior[i];
  for(i=0; i<seqlen; i++) loglik[wtree][i] = lhvec[wtree][i];
 }

 ntot++;

 if(print_out == true) TstvWrite();
 if(burn==true) { if(ntot%tune == 0) AdjustDelta(); }
 return update;
}

//------------------- End of function -------------------//

void ChangeAlpha::GenNewalpha()
{
 oldalpha = alpha[wtree];
 oldgamma = gamma[wtree];

 double lb = alpha[wtree] - delta, ub = alpha[wtree] + delta;

 double y = (double)runif(lb, ub);
 if(lb>0 && ub < MAX_ALPHA) alpha[wtree] = y;
 else if(lb<0)
 {
  if(y>0) alpha[wtree] = y;
  else alpha[wtree] = - y;
 }
 else   // if(ub >= MAX_ALPHA)
 {
  if(y<MAX_ALPHA) alpha[wtree] = y;
  else alpha[wtree] = 2*MAX_ALPHA -y;
 }

 gamma[wtree] = (1-A*alpha[wtree])/(B+C);
 if(gamma[wtree] <0)
 {
  cerr << "Error in ChangeAlpha: gamma has gone negative\n";
  exit(1);
 }
}

//------------------- End of function -------------------//

void ChangeAlpha::TstvWrite()
{
 outtstv.open(tstvfile, ios::app);
 for(int i=0; i<3; i++) outtstv << tstv[i] << " ";
 outtstv << endl;
 outtstv.close();
 print_out=false;
}

//------------------- End of function -------------------//

void ChangeAlpha::AdjustDelta()
{
 double ar = (double)nacc/ntot;
 if(ar<0.1) delta = (delta-0.002 > 0) ? delta-0.002 : delta;
 else if(ar>0.7) delta =(delta < 0.3) ? delta+0.002 : delta;

 nacc = ntot = 0;
}

/*============================================================*
 *			class HmmCalc			      *
 * This class implements the Hidden Markov models and         *
 * calculates the posterior probability			      *
 *============================================================*/

double** HmmCalc::lhvec=0;
double   HmmCalc::lambda=0.0;
double   HmmCalc::freq[3];
double   HmmCalc::tp[3][3];
int*     HmmCalc::curtop=0;
double   HmmCalc::hmmprob=0.0;
bool     HmmCalc::lambda_update=false;

HmmCalc::HmmCalc(int sl)
{
 if(sl!=0) HmmCalc::SetSeqlen(sl);
}

//------------------- End of function -------------------//

HmmCalc::~HmmCalc()
{
 int i;

 if(lhvec)
 {
  for(i=0; i<3; i++)
  {
   if(lhvec[i]) { delete [] lhvec[i]; lhvec[i]=0; }
  }
  delete [] lhvec;
  lhvec=0;
 }

 if(curtop) { delete [] curtop; curtop=0; }
}

//------------------- End of function -------------------//

void HmmCalc::GenRandomSeq()
{
  // generates a random sequence of topologies as a starting point
  // not completely random to avoid daft starting points
  // 0-6 breakpoints 
 int i, j, temp, st;
 int bp=rand()%7;
 int* lbp=new int[bp+1];
 if(!lbp) { char mes[]="HmmCalc::GenRandomSeq()"; OutofMemory(mes); }
 for(i=0; i<bp; i++) lbp[i]=rand()%seqlen;
 lbp[bp]=seqlen-1;
 v_isort(lbp, bp);
 for(i=0; i<=bp; i++)
 {
  temp=rand()%3;
  if(i==0) st=0;
  else     st=lbp[i-1]+1;
  for(j=st; j<=lbp[i]; j++) curtop[j] = temp;
 }
 delete [] lbp; lbp=0;
}

//------------------- End of function -------------------//

void HmmCalc::GetTPvalues(double* fr, double lamb)
{
  // sets up the transition matrix between topologies
 lambda = lamb;
 freq[0]=log(fr[0]);
 freq[1]=log(fr[1]);
 freq[2]=log(fr[2]);
 
 // double lambda = 1.0 - d;

 for(int i=0; i<3; i++)
 {
  for(int j=0; j<3; j++)
  {
   if(i==j) tp[i][j] = log( lambda+(1.0-lambda)*exp(freq[j]) );
   else     tp[i][j] = log( (1.0-lambda)*exp(freq[j]) );
  }
 }
}

//------------------- End of function -------------------//

void HmmCalc::SetSpaceH()
{
  // creates the space dynamically to hold the array of lhood values
  // assumes seqlen in CalcProb already been set

 if(seqlen==0)
 {
  cerr << "Seqlen in the base class CalcProb has not yet been set\n";
  cerr << "Exiting from HmmCalc::SetSpaceH()...\n";
  exit(1);
 }

 if(!lhvec)
 {
  lhvec = new double*[3];
  if(!lhvec) { char mes[]="HmmCalc constructor"; OutofMemory(mes); }
  for(int i=0; i<3; i++)
  {
   lhvec[i] = new double[seqlen];
   if(!lhvec[i]) { char mes[]="HmmCalc constructor"; OutofMemory(mes); }
  }
 }

 if(!curtop)
 {
  curtop = new int[seqlen];
  if(!curtop) { char mes[]="HmmCalc constructor"; OutofMemory(mes); }
 }
}

//------------------- End of function -------------------//

void HmmCalc::SetLvec()
{
  // overloaded function used to initialise lhvec
 int i, j;
 for(i=0; i<3; i++)
 {
  for(j=0; j<seqlen; j++) lhvec[i][j]=loglik[i][j];
 }
}

//------------------- End of function -------------------//

void HmmCalc::SetLvec(double** ll, int wt)
{
  // updates lhvec if a M-H step has been accepted
 for(int j=0; j<seqlen; j++) lhvec[wt][j]=ll[wt][j];
}

//------------------- End of function -------------------//

void HmmCalc::CalcPost(bool b)
{
 if(b==false) hmmprob=CalcHmmProb(lhvec);
 else         hmmprob = CalcHmmProb(loglik);
 int i;
 for(i=0; i<3; i++)
 {
 // assuming all three tree priors need to be included (otherwise it will
 // never add in a new topology in Gibbs)
   if(IsElementOf(curtop, i)==true) hmmprob+=prior[i];
   // hmmprob+=prior[i]; 
}
 hmmprob+=sharedprior;

 if(lambda_update==true) hmmprob += log( dbeta(lambda, 2, 2) );
 // prior prob on lambda
}

//------------------- End of function -------------------//

double HmmCalc::CalcHmmProb(double** ll)
{
  // calculates the log posterior probability of a particular sequence
  // of topology states. All inputted quantities must be logs
 double hmmlh;
 hmmlh = freq[curtop[0]] + ll[curtop[0]][0];  // first position in array
 for(int i=1; i<seqlen; i++) 
       hmmlh += tp[curtop[i-1]][curtop[i]] + ll[curtop[i]][i];
 return hmmlh; 
}

//------------------- End of function -------------------//

/***
void HmmCalc::CalcMAP()
{
  // Viterbi calculation, done in logs to prevent underflow errors
  // 17.10.00   not fully correct yet

 int i, j, n=seqlen-1;
 double **pmc;     // holds partial max contribution for i+1 to n
 double a, b, c, maxcon;
 int **toppath, *mpt;

 pmc = new double*[3];
 if(!pmc) { char mes[]="HmmCalc::CalcMAP()"; OutofMemory(mes); }
 for(i=0; i<3; i++)
 {
  pmc[i] = new double[seqlen];
  if(!pmc[i]) { char mes[]="HmmCalc::CalcMAP()"; OutofMemory(mes); }
 }

 toppath = new int*[3];
 if(!toppath) { char mes[]="HmmCalc::CalcMAP()"; OutofMemory(mes); }
 for(i=0; i<3; i++)
 {
  toppath[i] = new int[seqlen];
  if(!toppath[i]) { char mes[]="HmmCalc::CalcMAP()"; OutofMemory(mes); }
 }

 mpt = new int*[seqlen];
 if(!mpt) { char mes[]="HmmCalc::CalcMAP()"; OutofMemory(mes); }

 for(i=0; i<3; i++) pmc[i][n] = lhvec[i][n];

 for(j=n-1; j>=0; j--)
 {
  for(i=0; i<3; i++)
  {
   a=tp[i][i]       + pmc[i][j+1];
   b=tp[i][(i+1)%3] + pmc[(i+1)%3][j+1];
   c=tp[i][(i+2)%3] + pmc[(i+2)%3][j+1];
   
   if(a>=b && a>=c)
   {
    pmc[i][j] = a + lhvec[i][j];
    toppath[i][j]=i;
   }
   else if(b>a && b>=c)
   {
    pmc[i][j] = b + lhvec[i][j];
    toppath[i][j] = (i+1)%3;
   }
   else
   {
    pmc[i][j] = c + lhvec[i][j];
    toppath[i][j] = (i+2)%3;
   }
  }
 }
 a=freq[0] + pmc[0][0];
 b=freq[1] + pmc[1][0];
 c=freq[2] + pmc[2][0];

 if(a>=b && a>=c)
 {
  maxcon=a;
  mpt[0]=0;
 }
 else if (b>a && b>=c)
 {
  maxcon=b;
  mpt[0]=1;
 }
 else
 {
  maxcon=c;
  mpt[0]=2;
 }

 for(i=1; i<seqlen; i++) mpt[i]=toppath[ mpt[i-1] ][i-1];

// delete dynamically allocated variables
}
   ****/

//------------------- End of function -------------------//

bool HmmCalc::IsElementOf(int* c, int t)
{
  // returns true if tree t is one of the trees in use, false otherwise
 for(int i=0; i<seqlen; i++)
 {
  if(t==c[i]) return true;
 }
 return false;
}

//------------------- End of function -------------------//

void HmmCalc::PrintStateSequence()
{
  // Dirk Husmeier, 18 May 2001
  // Prints out the sequence of internal states, mainly for
  // debugging purposes.
  for (int t=0; t<seqlen; t++){
    cout << curtop[t];
    cout << " ";
  }
  cout << "\n";
}

//------------------- End of function -------------------//

void HmmCalc::ReadInitialMosaicStructure()
{
  // Dirk Husmeier, 21 May 2001
  // Reads in an initial sequence of hidden states, resulting
  // from a pre-processing step with e.g. RecPars.
  // The initial sequence must be stored in file mosaic.in.
  // Format of the file:
  // 1st line: number of segments
  // 2nd line: location of breakpoints
  // 3rd line: hidden states (tree topologies)
  // Example: A sequence alignment of length 787 with 4
  // segments
  // 4
  // 202 507 538 
  // 3   1   2   1
  // The topologies refer to the segments to the left of the 
  // breakpoints. The breakpoints must be in ascending order.
  // The first breakpoint must be > 1.
  // The last breakpoint must be < seqlen (the sequence length).
  // The number of breakpoints is one less than the number of
  // segments.
  // The hidden states (tree topologies) must be between 0 and 2.
  // The above example gives the following segments:
  // 1-201
  // 202-506
  // 507-537
  // 538-787

 int i,t,startSegment,endSegment,nSegments,nBreakpoints;

 ifstream infile("mosaic.in");
 if (!infile) {
   cerr << "HmmCalc::ReadInitialMosaicStructure(): Cannot open file mosaic.in \n";
   exit(1);
 }

 // Read in number of segments
 infile >> nSegments;
 if (nSegments<1) {
   cerr << "HmmCalc::ReadInitialMosaicStructure(): nSegments must be >= 1 \n";
   exit(1);
 }
 nBreakpoints=nSegments-1;
 int* breakpoints= new int[nBreakpoints];
 if(!breakpoints) { char mes[]="HmmCalc::ReadInitialMosaicStructure()"; OutofMemory(mes); }
 int* topos= new int[nSegments];
 if(!topos) { char mes[]="HmmCalc::ReadInitialMosaicStructure()"; OutofMemory(mes); }

 // Read in breakpoints (number of breakpoints= number of regions-1)
 for (i=0; i<nBreakpoints; i++){
   infile >> breakpoints[i];
   if (i>0 &&  breakpoints[i]<= breakpoints[i-1]){
     cerr << "HmmCalc::ReadInitialMosaicStructure(): The breakpoints must be in ascending order.\n";
     exit(1);
   }
 }
 if (breakpoints[0]<=1){
   cerr << "HmmCalc::ReadInitialMosaicStructure(): First breakpoint must be greater than 1 \n";
   exit(1);
 }
 if (breakpoints[nBreakpoints-1]>=seqlen){
   cerr << "HmmCalc::ReadInitialMosaicStructure(): Last breakpoint must be less than" << seqlen << "\n";
   exit(1);
 }

 // Read in topologies
 for (i=0; i<nSegments; i++){
   infile >> topos[i]; 
   if (topos[i]<0 || topos[i]>2){
     cerr << "HmmCalc::ReadInitialMosaicStructure(): Topologies must be between 0 and 2.\n";
     exit(1);
   }
 }

 // Save hidden state sequence in array curtop[]
 for (i=0; i<nSegments; i++){

   // Define the segment borders
   if(i==0) {
     startSegment=0;
     endSegment=breakpoints[i]-1;
   }
   else if(i==nSegments-1) {
     startSegment=breakpoints[i-1]; 
       // Note: Number of breakpoints is one less than number 
       // of segments.
     endSegment=seqlen-1;
   }
   else {
     startSegment=breakpoints[i-1];
     endSegment=breakpoints[i]-1;
   }

   // Write out sequence of topologies
   for(t=startSegment; t<=endSegment; t++) 
     curtop[t] = topos[i];
 }

 // Output to screen
 cout << "\n";
 cout << "Initial mosaic structure read in from file" << endl;
 cout << "Number of segments: " << nSegments << endl;
 cout << "Breakpoint"<<"\t\t"<<"Topo"<<"\n";
 for (i=0; i<nSegments; i++){
   if (i<nBreakpoints)
	 cout << breakpoints[i] <<"\t\t\t"<< topos[i] << "\n";
   else
     	 cout << seqlen <<"\t\t\t"<< topos[i] << "\n";
 }
 cout << "\n\n";


 // Clean up
 delete [] breakpoints; breakpoints=0;
 delete [] topos; topos=0;
}


/*================================================================*
 *			class ChangeTop				  *
 * class responsible for proposing new changes in the sequence of *
 * topologies via the Gibbs sampling algorithm			  *
 *================================================================*/

void ChangeTop::GibbsProb()
{
  // carries out the Gibbs sampling
 double pr[3], prob[3], temp;
 int i;

 for(i=0; i<3; i++)
 {
  curtop[site] = i;
  CalcPost(false);
  pr[i]=hmmprob;
  //  sum+=exp(prob[i]);
 }

 for(i=0; i<3; i++)
 {
  temp=1.0 + exp(pr[(i+1)%3]- pr[i]) + exp(pr[(i+2)%3] - pr[i]);
  prob[i]=1.0/temp;
 }

 temp=(double)runif(0,1);
 if(temp<=prob[0])                { curtop[site]=0; hmmprob=pr[0]; }
 else if(temp<=(prob[0]+prob[1])) { curtop[site]=1; hmmprob=pr[1]; }
 else                             { curtop[site]=2; hmmprob=pr[2]; }

 // working through the sites sequentially
   // if(site<(seqlen-1)) site++;
   // else site=0;

 if(forward==true)
 {
  site++;
  if(site==seqlen) { site = seqlen -1; forward=false; }
 }
 else
 {
  site--;
  if(site== -1) { site = 0; forward=true; }
 }
}

//------------------- End of function -------------------//

int ChangeTop::SingleSiteGibbsSample(int t)
{
  // Dirk Husmeier, 17 May 2001
  // Updates the specified hidden state variable with 
  // Gibbs sampling.
  // t : current site in the alignment
  // ll: array with log likelihood values
  // lhvec: static double** that contains lhoods (times prior for branchlengths), variable in class HmmCalc
  // curtop: variable in class HmmCalc, current topolgy sequence
  // seqlen: variable in class CalcProb

  int k;  // label for tree topologies
  int newTopo;
  double logPr[3]; // unnormalised log prob
  double prob[3]; // posterior probability
  double temp;
  double** ll= lhvec; // array of likelihoods; see above

  //1st step: compute the unnormalised log probabilities
  if (t==0){
    for (k=0; k<3; k++)
      logPr[k]=  ll[k][t] + tp[k][curtop[t+1]];
  }
  else if (t==seqlen-1){
    for (k=0; k<3; k++)
      logPr[k]= tp[k][curtop[t-1]] + ll[k][t];
  }
  else if (t>0 && t<seqlen-1){
    for (k=0; k<3; k++)
      logPr[k]= tp[curtop[t-1]][k] + ll[k][t] + tp[k][curtop[t+1]];
  }
  else{
    cerr << "Wrong argument in ChangeTop::SingleSiteGibbsSample";
    exit(1);
  }

  //2nd step: get probabilities
  for(k=0; k<3; k++){
    temp=1.0 + exp(logPr[(k+1)%3]-logPr[k]) + exp(logPr[(k+2)%3]-logPr[k]);
    prob[k]=1.0/temp;
  }

  //3rd step: sample from the posterior 
  temp=(double)runif(0,1);
  if(temp<=prob[0]) 
    newTopo=0;
  else if(temp<=(prob[0]+prob[1])) 
    newTopo=1;
  else 
    newTopo=2;

  return newTopo;
}

//------------------- End of function -------------------//

void ChangeTop::GibbsSampleOfStateSequence(int nCycles)
{
// Adriano Velasque Werhli, Jan 2005
  // Supervisor: Dirk Husmeier	
  // Samples new state variables for the whole sequence 
  // with Gibbs Sampling, modified forward algorithm.
  // nCycles, not necessary with modified forward backward...
  nCycles=1;
  int t;
//  int nIter;
  double randX;
  int k;
  int j;
  double** noll= lhvec; // array of likelihoods; see above
  double** emis;
  emis = new double*[3];
  for(int i=0;i<3;i++){
    emis[i] = new double[seqlen];}

  double** myalpha;
  myalpha = new double*[3];
  for(int i=0;i<3;i++){
  myalpha[i] = new double[seqlen];}
  
  double transP[3][3];  
  double normFactor;//Factor to normalize probabilities in each
                    //backwards...
// Just to start, calculate the exp from transition matrix and 
  // from tp an ll
  for (k=0; k<3; k++){
    for(j=0; j<3; j++){
     
      transP[k][j]=exp(tp[k][j]);
      // tp[k][j]=exp(tp[k][j]);
       // cout <<k<<j<<" tp---> "<< transP[k][j]<<"\n";
       //cout <<k<<j<<" transP---> "<< transP[k][j]<<"\n";
       //cout << tp[k][j]<<"\n";
    }
  }
  
    // now need to calculate exp from ll

  for (t=0; t<seqlen; t++){
    for(k=0; k<3; k++){
      emis[k][t]=exp(noll[k][t]);
      //cout << k<<" "<<t<<" "<< emis[k][t]<<" ";
    }
  }


   ///////////////////////////////////////////////////////
   ///// FORWARD ALGORITHM - CALCULATE SCALED ALPHAS /////
   ///////////////////////////////////////////////////////
   double pTopo[3];
for (k=0; k<3; k++){
      pTopo[k]=exp(freq[k]);
      //cout << pTopo[k]<<"\n";
  }
 
   // alpha to first site
   // need to verify priors... for while set to 1/3
  
   int mysite;//variable to visit all sites
   //double myfactor[1000];//variable to scaling factor forward
   double* myfactor;
   myfactor = new double[seqlen];
   
   myalpha[0][0]=pTopo[0]*emis[0][0];
   myalpha[1][0]=pTopo[1]*emis[1][0];
   myalpha[2][0]=pTopo[2]*emis[2][0];

   myfactor[0]=myalpha[0][0]+myalpha[1][0]+myalpha[2][0];
 
   myalpha[0][0]=myalpha[0][0]/myfactor[0];
   myalpha[1][0]=myalpha[1][0]/myfactor[0];
   myalpha[2][0]=myalpha[2][0]/myfactor[0];

   ///// forward iteration////

     for (mysite=1; mysite<seqlen; mysite++){
     myalpha[0][mysite]=transP[0][0]*myalpha[0][mysite-1]*emis[0][mysite]+
                        transP[1][0]*myalpha[1][mysite-1]*emis[0][mysite]+
                        transP[2][0]*myalpha[2][mysite-1]*emis[0][mysite];

     myalpha[1][mysite]=transP[0][1]*myalpha[0][mysite-1]*emis[1][mysite]+
                        transP[1][1]*myalpha[1][mysite-1]*emis[1][mysite]+
                        transP[2][1]*myalpha[2][mysite-1]*emis[1][mysite];

     myalpha[2][mysite]=transP[0][2]*myalpha[0][mysite-1]*emis[2][mysite]+
                        transP[1][2]*myalpha[1][mysite-1]*emis[2][mysite]+
                        transP[2][2]*myalpha[2][mysite-1]*emis[2][mysite];
  
       //calculate the scaling factor.
       myfactor[mysite]=myalpha[0][mysite]+myalpha[1][mysite]+myalpha[2][mysite];
       
       // scale alphas... prevent underflow... 
       myalpha[0][mysite]=myalpha[0][mysite]/myfactor[mysite];
       myalpha[1][mysite]=myalpha[1][mysite]/myfactor[mysite];
       myalpha[2][mysite]=myalpha[2][mysite]/myfactor[mysite];


}
                                                              
     
     //////////////////////////////////////////////////
     /////////// MODIFIED BACKWARD ALGORITHM///////////
     //////////////////////////////////////////////////
     
     // Starts with the last column, choose a state according to myalpha
     // in last column.

   
      randX=runif(0,1);
     


if(randX<=myalpha[0][seqlen-1])
   {curtop[seqlen-1]=0;}
else if(randX>myalpha[0][seqlen-1] && randX<=myalpha[0][seqlen-1]+myalpha[1][seqlen-1])
   {curtop[seqlen-1]=1;}
else if(randX>myalpha[0][seqlen-1]+myalpha[1][seqlen-1] && randX<=1.0)
   {curtop[seqlen-1]=2;}
else
   { cout << "Illegal option \n"; exit(1);}
//}



//now need to sample the other states recursively back...
double myprobnn[3];//probabilities not normalized
double myprob[3];//probabilities normalized

//incluir loop back em todos os sites...


for(mysite=seqlen-2; mysite>=0; mysite--){

// 1st calculate probabilities to actual site 
myprobnn[0]=transP[0][curtop[mysite+1]]*myalpha[0][mysite];
myprobnn[1]=transP[1][curtop[mysite+1]]*myalpha[1][mysite];
myprobnn[2]=transP[2][curtop[mysite+1]]*myalpha[2][mysite];

/// now normalize these probabilities
normFactor= (myprobnn[0]+myprobnn[1]+myprobnn[2]);
myprob[0]=myprobnn[0]/normFactor;
myprob[1]=myprobnn[1]/normFactor;
myprob[2]=myprobnn[2]/normFactor;


// choose a new state using the probabilities.

    randX=runif(0,1);// random number...
    

if(randX<=myprob[0])
   {curtop[mysite]=0;}
else if(randX>myprob[0] && randX<=myprob[0]+myprob[1])
   {curtop[mysite]=1;}
else if(randX>myprob[0]+myprob[1] && randX<=1.0)
   {curtop[mysite]=2;}
else
   { cout << "Illegal option \n"; exit(1);}


}

// Free memory...

delete [] myfactor;
myfactor=0;

for(int i=0;i<3;i++){
    delete [] emis[i]; emis[i]=0;}
delete [] emis;
emis=0;


for(int i=0;i<3;i++){
    delete [] myalpha[i]; myalpha[i]=0;}
delete [] myalpha;
myalpha=0;




  // These are original comments:
  // At the end of Gibbs Sampling, compute the posterior.
  // Ideally, that should be done in the calling function
  // DoSampling, buT to keep the program consistent with
  // Grainne's code (at least for the time being) this is 
  // done here.
  CalcPost(false);


}



/*===================================================================*
 *			class ScaleBranch			     *
 * responsible for proposing new branch lengths for one of the three *
 * trees. This tree is chosen at random each time this class is      *
 * called upon to do the MCMC					     *
 *===================================================================*/

ScaleBranch::ScaleBranch()
{
 nacc = ntot = 0;
 sbur=sbunifreg;
 delta = INIT_DELTA_BRANCH;   // added 14.11
}

//------------------- End of function -------------------//

bool ScaleBranch::MHaccept(Tree* t)
{
 oldlh = hmmprob;
 int wt = rand()%3;
 ChangeBranches(t, wt);   // change branches of tree wt
 CalcLik(t->GetRoot(), wt);
 CalcPost(true);
 newlh = hmmprob;

 double logalpha = newlh - oldlh;
 double Alpha = exp(logalpha);

 bool update = false;
 double temp = (double)runif(0,1);

 if(temp<Alpha)
 {
  oldlh = newlh;
  update = true;
  nacc++;
  SetLvec(loglik, wt);
 }
 else
 {
  int i;
  RevertBranches(t->GetRoot(wt));
  hmmprob=oldlh;
  for(i=0; i<3; i++) prior[i]=oldprior[i];
  for(i=0; i<seqlen; i++) loglik[wt][i] = lhvec[wt][i];
 }
 ntot++;

 if(burn==true) { if(ntot%tune==0 && ntot!=0) AdjustDelta(); }
 return update;
}

//------------------- End of function -------------------//

void ScaleBranch::ChangeBranches(Tree* t, int wt)
{
  // changes the branch lengths of the wt^{th} tree
 epsilon = 1.0 + sbur;
 DoChanges(t->GetRoot(wt) );
}

//------------------- End of function -------------------//

void ScaleBranch::DoChanges(Node* n)
{
  // recursive function carrying out the branch scaling for node
  // n and all nodes below

 if(n==0) return;
 DoChanges(n->lchild);
 DoChanges(n->rchild);

 /* if(n->parent!=0)    // ie n not root
 {
  n->oldbl = n->parent_bl;
  n->parent_bl *= (double)genunf(1.0/epsilon, epsilon);
  } */

 if(n->parent!=0)
 {
  n->oldbl = n->parent_bl;
  double lb = n->parent_bl - delta, ub = n->parent_bl + delta;
  double y = (double)runif(lb, ub);
  if(lb > 0 && ub < MAX_BRANCH) n->parent_bl = y;
  else if(lb < 0) n->parent_bl = (y>0) ? y : -y;
  else n->parent_bl = (y<MAX_BRANCH) ? y : 2*MAX_BRANCH - y;
 }
}

//------------------- End of function -------------------//

void ScaleBranch::RevertBranches(Node* n)
{
  // recursive function for restoring the original branch length
  // values to node n and those below it

 if(n==0) return;
 RevertBranches(n->lchild);
 RevertBranches(n->rchild);

 if(n->parent!=0) n->parent_bl = n->oldbl;
}

//------------------- End of function -------------------//

void ScaleBranch::AdjustDelta()
{
  // tunes the proposal function for new branch lengths by
  // descreasing/increasing sbur

 double ar = (double)nacc/ntot;
 if(ar < 0.1)
 {
   //  if(sbur>AMCH) sbur -= AMCH;   removed 14.11
   delta = (delta > 0.002) ? delta - 0.002 : delta;
 }
 else   // AR>0.7
 {
   //  if(sbur<0.8) sbur+=AMCH;    removed 14.11
   delta = (delta < 0.03) ? delta + 0.002 : delta;
 }
 // sbunifreg=sbur;  removed 14.11
 nacc=0;
 ntot=0;
} 

/*===========================================================*
 *			class ChangeLambda		     *
 * this class adjusts the value of lambda, the difficulty of *
 * changing topology					     *
 *===========================================================*/


ChangeLambda::ChangeLambda()
{
  alphaPrior=2.0;
  betaPrior=2.0;
  // Dummy initialisation, which gets changed later.

 nacc = ntot = 0;
 delta = INIT_DELTA_LAMBDA;
 burn = print_out = true;
 newlh = oldlh = 0.0;
 tune = 200;
 strcpy(lambdafile, "lambda.out");
}
//------------------- End of function -------------------//

void ChangeLambda::PriorBeta(double meanLambda)
// Sets the parameters of the prior distribution.
{
  betaPrior=2.0;
  alphaPrior= meanLambda*betaPrior/(1.0-meanLambda);
}
//------------------- End of function -------------------//


void ChangeLambda::UpdatePosteriorBeta()
// Updates the posterior parameters of the Beta distribution
// according to equation (2.3) in C.P. Robert et al., 
// Statistics & Probability Letters 16 (1), 77-83, 1993.
{
  int site;
  alphaPosterior= alphaPrior;
  betaPosterior= betaPrior;
  for (site=0; site<seqlen-1; site++){
    if (curtop[site]==curtop[site+1]){
      alphaPosterior++;
    }
    else{
      betaPosterior++;
    }
  }
}
//------------------- End of function -------------------//

void ChangeLambda::SampleNewLambda()
// TDH, July 2001
// Samples new lambda from the Beta posterior distribution
// according to equation (2.3) in C.P. Robert et al., 
// Statistics & Probability Letters 16 (1), 77-83, 1993.
// Note that there is an overlaid method.
{
  lambda=rbeta(alphaPosterior,betaPosterior); 
  if (print_out==true) LambdaWrite();
}
//------------------- End of function -------------------//

void ChangeLambda::SampleNewLambda(long int nCurrent,long int nBurnIn, char* flagAnneal)
// TDH, July 2001
// Samples new lambda from the Beta posterior distribution
// according to equation (2.3) in C.P. Robert et al., 
// Statistics & Probability Letters 16 (1), 77-83, 1993.
// This is an overlaid method that allows the use of an
// annealing scheme during the burn-in period.
// During burn in, lambda is sampled from a mixed 
// distribution, where the mixing parameter is linearly 
// increased from 0 (pure prior) to 1 (pure posterior). 
// There are two different mixing procedures, mixing
// either the parameters of the distribution or the 
// distributions themselves. See documentation
// for details.
{
  double mixPar; /* mixing parameter: 0-->prior, 1-->posterior */
  double alphaMix,betaMix;

  if (burn==true)  mixPar= double(nCurrent)/double(nBurnIn-1);
     // n is the step number, op.burn the number of burn-in steps,
     // so the mixing parameter is increased linearly from 0-->1.
  if (burn==true && strcmp("PAR",flagAnneal)==0){
    // Burn-in period, option --> annealing by mixing parameters
    alphaMix= mixPar*alphaPosterior + (1.0-mixPar)*alphaPrior;
    betaMix= mixPar*betaPosterior + (1.0-mixPar)*betaPrior;
    lambda=rbeta(alphaMix,betaMix);
  }
  else if (burn==true && strcmp("PROB",flagAnneal)==0){
    // Burn-in period, option --> annealing by mixing distributions
    if (sunif()>mixPar)
      lambda=rbeta(alphaPrior,betaPrior);
    else
      lambda=rbeta(alphaPosterior,betaPosterior);
  }
  else{
    // Sampling period OR option "no annealing"
    lambda=rbeta(alphaPosterior,betaPosterior);
  }
 
   if (print_out==true) LambdaWrite();
}
//------------------- End of function -------------------//


void ChangeLambda::UpdateTPprobs()
// Updates the transition probabilities with the 
// current value of lambda.

// New version: Dirk Husmeier, 4 July 2001
// The functional dependence of the transition probabilities 
// between tree topologies on lambda has been redefined.
// Rather than using Grainne's original definition, where lambda denotes 
// the probability that no recombination occurs, lambda is now defined
// as the probability that no recombination is observed.
// Define S0 and S1 to be the topologies in two adjacent sites, 0 and 1.
// These topologies are the same with probability lambda.
// The probability of a transition from S0 to S1 is equal to
// (1-lambda)*P(S1)/[1-P(S0)], so the total transition 
// probability is  P(S1|S0)= lambda*delta(S0,S1)+
// (1-lambda)*P(S1)/[1-P(S0)][1-delta(S0,S1)].
// For equal prior probabilities P(Si)=1/3, this reduces to
// equation (5) in Husmeier, Neural Network World 4 (10), 589-595, 2000.
// The advantage of this definition is that lambda can now
// be given a conjugate prior of a standard form, namely the 
// beta distribution.

// Note that tp[i][k] is the log probability for a transition from
// topology i into topology k.
{
  int K=3; // Number of different tree topologies
  bool uniformPrior;

  // Check if prior on topologies if uniform
  if (freq[0]==freq[1] && freq[0]==freq[2]){
    uniformPrior=true;
  }
  else{
    cerr << "Prior on tree topologies not uniform";
    uniformPrior=false;
  }
    
  if (uniformPrior){
    // Uniform prior, allows faster computation
    for(int i=0; i<K; i++){
      for(int j=0; j<K; j++){
	if(i==j) tp[i][j] = log(lambda);
	else     tp[i][j] = log((double)(1.0-lambda)) - log((double)(K-1));
      }
    }
  } else {
    // Non-uniform prior; use general formula
    for(int i=0; i<K; i++){
      for(int j=0; j<K; j++){
	if(i==j) tp[i][j] = log(lambda);
	else     tp[i][j] = log(1.0-lambda) + freq[j] - log(1-exp(freq[i]));
      }
    }
  }

}
//------------------- End of function -------------------//


void ChangeLambda::LambdaWrite()
{
  // writes out current lambda value to file
 outlambda.open(lambdafile, ios::app);
 outlambda << lambda << endl;
 outlambda.close();
 print_out = false;
}

//------------------- End of function -------------------//



void ChangeLambda::SetLambdafile(char* lf)
{
 strcpy(lambdafile, lf);
 outlambda.open(lambdafile, ios::out);
 outlambda.close();
}
