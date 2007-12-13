/*=====================================================================*
 *			prog.cc					       *
 *	copyright (c) Grainne McGuire, 2001			       *
 *		     Version 1.00b, BARCE    			       *
 *								       *
 * Main program file for hmmprog, implements MCMC sampling of trees    *
 * to detect recombination and gene conversion			       *
 *=====================================================================*/

#include "basics.h"
#include "ChooseOptions.h"
#include "data.h"
#include "treemanip.h"

#ifdef USING_ANSI_CPP

#include <ctime>   // is this right
#include <cctype>

#else
#include <time.h>
#include <ctype.h>
#endif

// constants for the priors in use (8.8.00 - clean-up phase)
int SHAPE=1;    // the bl gamma parameters
int SCALE=1000;
const double INIT_DELTA_ALPHA = 0.05;
const double INIT_DELTA_PI = 5;
const double INIT_DELTA_LAMBDA = 0.1;
const double INIT_DELTA_BRANCH = 0.02;

int seed;   // random no. seed
int ix, iy, iz; // seeds for U[0,1] generator
double sbunifreg=0.05;    // width of uniform distribution for changing bls

void Initialise(ReadData*, TreeCalc*, InitTree&, Options&, Model);
void MHstep(TreeCalc&, Options&, OutPut&);
long DoSampling(TreeCalc&, OutPut&, Options&, bool, long);
void ProposeBranch(TreeCalc&);
void ProposeTstv(TreeCalc&);
void ProposePi(TreeCalc&);
void ProposeTop(TreeCalc&);
void ProposeLambda(TreeCalc&); /* TDH, July 2001 */
void ProposeLambda(TreeCalc& tc, Options& op, long int nStep); /* TDH, July 2001 */
void ARopenfile(char* filename);
void ARoutput(char* filename, int& nacc, int& nsteps);

//---------------- MAIN FUNCTION ----------------//

int main()
{
  //seed=1;
 seed=(unsigned)time(NULL);   // let this be 1 for debugging
 srand(seed);
 ix=rand()%30000; iy=rand()%30000; iz=rand()%30000;

 Options options;
 char choice=options.DisplayOptions();
 while(toupper(choice)!='Y')
 {
  options.ChangeOptions(choice);
  choice=options.DisplayOptions();
 }
 options.PrintSettings();

 ReadData rdata;

 Model mod;
 if(strcmp(options.modeltype, "JC")==0) mod=JC;
 else if(strcmp(options.modeltype, "F81")==0) mod=F81;
 else if(strcmp(options.modeltype, "K2P")==0) mod=K2P;
 else mod=F84;

 TreeCalc treecalc;
 InitTree first_tree;

 Initialise(&rdata, &treecalc, first_tree, options, mod); 
 OutPut output(rdata.GetSeqlen() );
 MHstep(treecalc, options, output);
 output.SetFilename(options.postprobfile);
 output.ProcessOutput();
 treecalc.NumberInfo();

 return EXIT_SUCCESS;
}

//-----------------END OF MAIN FUNCTION-----------------//

void Initialise(ReadData* rd, TreeCalc* tc, InitTree& ft,
                Options& op, Model m)
{
  // this function sorts out many of the classes so that parameters like
  // seqlen, input/output file names are set correctly. This information
  // is taken from the data member of the Options structure op.

 rd->ReadFileSeq(op.datafile);
 ft.BlValue(op.mbl);
 ft.InitialiseTree();
 
 Tree* ptrTree = &ft;  // tree is base class for InitTree
 tc->SetValues(ptrTree, rd);

 // 14.8.00  setting the static data members of CalcProb
 CalcProb::SetModel(m);
 CalcProb::SetSeqlen(rd->GetSeqlen() );
 CalcProb::SetConcatlen(rd->GetConcatlen() );
 CalcProb::SetSorted(rd->GetSorted() );
 CalcProb::SetWeight(rd->GetWeight() );

 if(strcmp(op.estimatefreqs, "YES")==0) CalcProb::SetPi(rd->GetPi());
 else CalcProb::SetPi(op.fr);

 if(strcmp(op.etstv, "YES")==0) CalcProb::SetTstv(rd->GetTstv());
 else CalcProb::SetTstv(op.tstv);

 int i;
 CalcProb::UpdateConstants(0);
 if(m==F84 || m==K2P)
 {
  for(i=0; i<3; i++) CalcProb::SetF84Params(i);
 }

 for(i=0; i<3; i++) tc->AssignOuterData( tc->GetTree()->GetRoot(i) );
 if(strcmp(op.updatepi, "YES")==0)   tc->UpdatePi(true);
 else			             tc->UpdatePi(false);
 if(strcmp(op.updatetstv, "YES")==0) tc->UpdateTstv(true);
 else			             tc->UpdateTstv(false);
 /* TDH, July 2001 */
 if(strcmp(op.updateLambda, "YES")==0) tc->UpdateLambda(true);
 else			             tc->UpdateLambda(false);

 CalcProb::SetLoglik();
 CalcProb::SetSpace();
 HmmCalc::SetSpaceH();
 if(strcmp(op.readInitMosaic,"YES")==0)
   HmmCalc::ReadInitialMosaicStructure();
   // Dirk Husmeier, 22 May 2001
   // Read in initial mosaic structure from file 
 else
   HmmCalc::GenRandomSeq();  
   // Creates random topology sequence
 HmmCalc::GetTPvalues(op.topfreq, op.diffic);
 CalcProb::InitPrior();
 for(i=0; i<3; i++) tc->changetop.CalcLik(tc->GetTree()->GetRoot(), i);
 for(i=0; i<3; i++) tc->changetop.SetLvec();
 tc->changetop.CalcPost(false );
 tc->steps=tc->naccept=0;
}

//-----------------END OF FUNCTION-----------------//


void MHstep(TreeCalc& tc, Options& op, OutPut& out)
{
 ARopenfile(op.arfile);
 out.OpenLogfile(op.lpdfile);
 out.OpenBranchfile(op.branchfile);
 out.OpenOutfile();

 tc.scalebranch.SetTuningint(op.tuning_int);
 tc.changepi.SetTuningint(op.tuning_int);
 tc.changealpha.SetTuningint(op.tuning_int);
 tc.changelambda.SetTuningint(op.tuning_int);
 tc.changelambda.PriorBeta(op.diffic);  /* TDH, July 2001 */
 tc.changepi.SetPifile(op.pifile);
 tc.changealpha.SetTstvfile(op.tstvfile);
 //tc.changelambda.SetLambdafile(op.lambdafile);

 long titsRun = 0;

 titsRun = DoSampling(tc, out, op, true, 0);   // burn-in period
 DoSampling(tc, out, op, false, titsRun);  // post=burn-in

 cout << "::END" << endl;
 }

//-----------------END OF FUNCTION-----------------//

////
// function to open file for the acceptance ratio output
////

void ARopenfile(char* filename)
{
 ofstream farfile;

#if defined(ANSI_CPP_)
 farfile.open(filename, ios::out, ios::noreplace);
 if( !(farfile.is_open() ) )
 {
  cout << "The file " << filename << " already exists. ";
  cout << "Replace anyway? (y/n): ";
  char ch;
  cin >> ch;
  cin.ignore(LINE_LENGTH, '\n');
  if(toupper(ch)=='Y') farfile.open(filename, ios::trunc);
  else
  {
   cout <<"Enter the new filename [no checking for existing ";
   cout << "files this time]: ";
   cin >> filename;
   cin.ignore(LINE_LENGTH, '\n');
   //  farfile.open(filename, '\n');
   farfile.open(filename, ios::out);
  }
 }
#else
 farfile.open(filename, ios::out);
#endif
 farfile.close();
}

//-----------------END OF FUNCTION-----------------//

/////
// file which outputs the acceptance ratio into the file of the
// same name as that held in the string filename
/////

void ARoutput(char* filename, int& nacc, int& nsteps)
{
 double ratio = (double)nacc/nsteps;
 ofstream farfile;
 farfile.open(filename, ios::app);
 farfile << ratio << endl;
 farfile.close();
}


//-------------------------END OF FUNCTION-------------------------//

long DoSampling(TreeCalc& tc, OutPut& out, Options& op, bool b, long titsRun)
{
	// This function carries out the actual iterations of the MCMC
	// nits is the number of iterations to be carried out
	// b is whether this is the burn-in period or not.

	// Dirk Husmeier, 25 May 2001
	// Changing the way MCMC steps are counted;
	// see documentation for further explanations.

	cout << "::START" << endl;

	// Total number of iterations (burn in + post burn in)
	long tits = op.burn + ((op.numsamples-1)*op.interval + 1);

	long int nits;
	if(b==true)
	{
		tc.scalebranch.ChangeBurn(true);
		tc.changealpha.ChangeBurn(true);
		tc.changepi.ChangeBurn(true);
		tc.changelambda.ChangeBurn(true);
		nits=op.burn;
	}
	else
	{
		tc.scalebranch.ChangeBurn(false);
		tc.changealpha.ChangeBurn(false);
		tc.changepi.ChangeBurn(false);
		tc.changelambda.ChangeBurn(false);
		nits=(op.numsamples-1)*op.interval + 1;
	}

	int temi;
	for(long int i=0; i<nits; i++)  // b set at run-time
	{
		// TDH: output to screen
//		if (i%op.interval == 0) {cout << i << endl;}

		if(i%op.interval==0)
		{ 
			tc.changepi.Print_Out();
			tc.changealpha.Print_Out();
			tc.changelambda.Print_Out();
		}

		temi=rand();
		// JC model
		if(CalcProb::GetModel()==JC)
		{
			ProposeTop(tc); // Gibbs step
			ProposeBranch(tc); tc.steps++; // MH steps
			if(tc.UpdateLambda()==true){
				ProposeLambda(tc,op,i); // Gibbs step
			}
		}

		// K2P model
		else if(CalcProb::GetModel()==K2P)
		{
			ProposeTop(tc); // Gibbs step
			ProposeBranch(tc); tc.steps++; // MH steps
			if(tc.UpdateLambda()==true){
				ProposeLambda(tc,op,i); // Gibbs step
			}
			if(tc.UpdateTstv()==true){
				ProposeTstv(tc); tc.steps++; // MH steps
			}
		}

		// F81 model
		else if(CalcProb::GetModel()==F81)
		{
			ProposeTop(tc); // Gibbs step
			ProposeBranch(tc); tc.steps++; // MH steps
			if(tc.UpdateLambda()==true){
				ProposeLambda(tc,op,i); // Gibbs step
			}
			if(tc.UpdatePi()==true){
				ProposePi(tc); tc.steps++; // MH steps
			}
		}

		// F84 model
		else			// model is F84
		{
			ProposeTop(tc); // Gibbs step
			ProposeBranch(tc); tc.steps++; // MH steps
			if(tc.UpdateLambda()==true){
				ProposeLambda(tc,op,i); // Gibbs step
			}
			if(tc.UpdateTstv()==true){
				ProposeTstv(tc); tc.steps++; // MH steps
			}
			if(tc.UpdatePi()==true){
				ProposePi(tc); tc.steps++; // MH steps
			}
		}

		if(i%(op.interval*5) ==0) ARoutput(op.arfile, tc.naccept, tc.steps);

		if(i%op.interval==0)
		{
			out.SetTree(tc.GetTree() );
			if(b==false) out.WriteX(HmmCalc::GetCurtop() );
			out.PrintLogfile(HmmCalc::GetLPD(), op.lpdfile);
			out.PrintBranchfile( tc.GetTree()->GetMeanBL(), op.branchfile );
		}

		titsRun++;
		cout << "p=" << (int)((((float)(titsRun))/tits)*100) << endl;
	}

	return titsRun;
}

//-------------------------END OF FUNCTION-------------------------//


void ProposeBranch(TreeCalc& tc)
{
  // proposes new branch lengths for one of the trees
 bool tb=tc.scalebranch.MHaccept(tc.GetTree() );
 if(tb==true) tc.naccept++;
}

//-------------------------END OF FUNCTION-------------------------//

void ProposeTstv(TreeCalc& tc)
{
 bool tb=tc.changealpha.MHaccept(tc.GetTree() );
 if(tb==true) tc.naccept++;
}

//-------------------------END OF FUNCTION-------------------------//

void ProposePi(TreeCalc& tc)
{
 bool tb=tc.changepi.MHaccept(tc.GetTree() );
 if(tb==true) tc.naccept++;
}

//-------------------------END OF FUNCTION-------------------------//

void ProposeTop(TreeCalc& tc)
{
  // Changes topology sequence via Gibbs sampling.
  // Note that this is not added to the acceptance ratio since
  // Gibbs steps are always accepted.


  /* Grainne's code                                        //
  // for(int i=0; i<(2*tc.changetop.GetSeqlen()); i++)     //
  //          tc.changetop.GibbsProb();                    */

  /* Changes made by Dirk Husmeier, 17 May 2001 */
     tc.changetop.GibbsSampleOfStateSequence(5);
}

//-------------------------END OF FUNCTION-------------------------//

void ProposeLambda(TreeCalc& tc)
// TDH, July 2001
// Changes lambda via Gibbs sampling.
// Note that this is not added to the acceptance ratio since
// Gibbs steps are always accepted.
// Note that there is an overlaid method.
{
  // Step 1: Update the parameters of the posterior distribution.
  tc.changelambda.UpdatePosteriorBeta();
  // Step 2: Sample new value of lambda from the posterior distribution.
  tc.changelambda.SampleNewLambda();
  // Step 3: Update the elements of the transition matrix (between topologies).
  tc.changelambda.UpdateTPprobs();
}
//-------------------------END OF FUNCTION-------------------------//


void ProposeLambda(TreeCalc& tc, Options& op, long int nStep)
// TDH, July 2001
// Changes lambda via Gibbs sampling.
// Note that this is not added to the acceptance ratio since
// Gibbs steps are always accepted.
// Overlaid method to allow simulated annealing for lambda, 
// the difficulty of changing tree topologies.
{
  // Step 1: Update the parameters of the posterior distribution.
  tc.changelambda.UpdatePosteriorBeta();
  // Step 2: Sample new value of lambda from the posterior distribution.
  tc.changelambda.SampleNewLambda(nStep,long(op.burn),op.annealLambda);
  // Step 3: Update the elements of the transition matrix (between topologies).
  tc.changelambda.UpdateTPprobs();
}

