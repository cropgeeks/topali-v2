#include "ChooseOptions.h"

/***************************************************************
 *		ChooseOptions.cc			       *
 * Copyright (c) 2001, Grainne McGuire			       *
 *		Version 1.00b, BARCE			       *
 *			class Options			       *
 * sets all the values which need to be set by the user via a  *
 * text menu TOPAL and PHYLIP (and many others) style	       *
 ***************************************************************/

Options::Options()
{
  // constructor function. Sets the default parameter values
  // strcpy(restart, "NO");
 strcpy(modeltype, "F84");
 strcpy(estimatefreqs, "YES");
 int i;
 for(i=0; i<4; i++) fr[i]=0.25;
 tstv = 2.0;
 strcpy(etstv, "YES");
 strcpy(updatepi, "YES");
 strcpy(updatetstv, "YES");
 strcpy(updateLambda, "YES");  /* TDH */
 strcpy(annealLambda, "NONE");  /* TDH */
 strcpy(readInitMosaic, "NO");  /* TDH */
 mbl=0.1;

 for(i=0; i<3; i++) topfreq[i]=1.0/3.0;
 diffic=0.9;

 burn=10000;
 interval=10;
 numsamples=1000;
 tuning_int=100;


 strcpy(arfile, "ar.log");
 strcpy(lpdfile, "lpdfile.out");
 strcpy(branchfile, "branch.out");
 strcpy(tstvfile, "tstv.out");
 strcpy(pifile, "pi.out");
 strcpy(settingsfile, "settings.out");
 strcpy(postprobfile, "topol_prob.out");
 strcpy(datafile, "seq.phy");
 // strcpy(lambdafile, "lambda.out");

 filemenu=false;
 runmenu=false;
 modelmenu=false;
}

///////////////////////////////////////////////////////////////////
// Options::DisplayOptions() displays the current settings	 //
// Also allows one to be changed, or the options to be finalised //
///////////////////////////////////////////////////////////////////
	
char Options::DisplayOptions()
{
  // menu display function
 char choice;

 if(modelmenu==true)
 {
  choice=DisplayModelmenu();
  if(choice=='X') { modelmenu=false; choice='\n'; }
  else if(choice=='2' || choice=='3') choice='\n';
 }
 else if(runmenu==true)
 {
  choice=DisplayRunmenu();
  if(choice=='X') { runmenu=false; choice='\n'; }
  else if(choice=='1' || choice=='3') choice='\n';
 }
 else if(filemenu==true)
 {
  choice=DisplayFilemenu();
  if(choice=='X') { filemenu=false; choice='\n'; }
  else if(choice=='1' || choice=='2') choice='\n';
 }
 else
 {
  cout << "\n\t\tMain Menu for MCMC-HMM\n";
  cout << "\t\t----------------------\n";
  cout << "1)\tModel submenu\n";
  cout << "2)\tRun settings submenu\n";
  cout << "3)\tFile submenu\n";

  cout << "\n\nEnter in a number to choose an option to change\n";
  cout << "or enter Y if you're happy with the current options: ";
  cin >> choice;
  if(islower(choice)) choice=toupper(choice);
 }
  return choice;
}

//-----------------END OF FUNCTION-----------------//


void Options::ChangeOptions(char choice)
{
  // changes the chosen option
  // prompts for a value or toggles as appropriate
 int temp, dummy=0, i;
 float temf;
 char inp[10];
 char filename[26];

 switch (choice){
   // main menu choices
  case '1':
   modelmenu=true;
   break;

  case '2':
   runmenu=true;
   break;

  case '3':
   filemenu=true;
   break;

   // model settings now

  case 'M':
   if(strcmp("JC", modeltype)==0) strcpy(modeltype,"K2P");
   else if(strcmp("K2P",modeltype)==0)
   {
    strcpy(modeltype,"F81");
    strcpy(estimatefreqs, "YES");
   }
   else if(strcmp("F81", modeltype)==0) strcpy(modeltype, "F84");
   else
   {
    strcpy(modeltype, "JC");
    strcpy(estimatefreqs, "NO");
    for(i=0; i<5; i++) fr[i]=0.2;
   }
   break;

   /* TDH, 10 July 2001 */
  case 'Q':
   if(strcmp("NONE", annealLambda)==0) strcpy(annealLambda,"PAR");
   else if (strcmp("PAR", annealLambda)==0) strcpy(annealLambda,"PROB");
   else strcpy(annealLambda,"NONE");
   break;

  case 'E': 
   if(strcmp(estimatefreqs, "YES")==0) strcpy(estimatefreqs, "NO");
   else strcpy(estimatefreqs, "YES");
   break;

  case 'F':
   cout << "The current frequencies are (in alphabetical order): \n";
   for(i=0; i<4; i++) cout << i+1 << ": " << fr[i] << " ";
   cout << endl;
   char ch[10];
   cout << "Enter 'Y' to change these, 'N' otherwise: ";
   cin >> ch;
   if(toupper(ch[0])=='Y')
   {
    cout << "Note that there is no checking on these values so take care\n";
    cout << "that they sum to one.\n";
    cout << "Enter the frequencies: ";
    for(i=0; i<4; i++) cin >> fr[i];
   }
   break;

  case 'P':
   cout << "Enter the three new topology frequencies (note that there ";
   cout << "is no checking\non these values so take care ";
   cout << "that they sum to one):";
   for(i=0; i<3; i++) cin >> topfreq[i];
   break;

  case 'D':
   cout << "Enter the difficulty of changing trees (number between 0 and 1):";
   cin >> inp;
   temf=CheckInput(inp,dummy);
   if(temf==0) return;
   else diffic=(double)temf;
   break;

  case 'R':
   if(strcmp(etstv, "YES")==0) strcpy(etstv, "NO");
   else strcpy(etstv, "YES");
   break;

  case 'T':
   cout << "Enter the initial transition-transversion ratio: ";
   cin >> inp;
   temf=CheckInput(inp,dummy);
   if(temf==0) return;
   else tstv=(double)temf;
   break;

  case 'B':
   cout << "Enter the length of the burn-in period: ";
   cin >> inp;
   temp=CheckInput(inp);
   if(temp==0) return;
   else burn=temp;
   break;

  case 'N':
   cout << "Enter the number of points to return: ";
   cin >> inp;
   temp=CheckInput(inp);
   if(temp==0) return;
   else numsamples=(long)temp;
   break;

  case 'I':
   cout << "Enter the thinning interval: ";
   cin >> inp;
   temp=CheckInput(inp);
   if(temp==0) return;
   else interval=temp;
   break;

  case 'C':
   cout << "Enter the tuning interval: ";
   cin >> inp;
   temp=CheckInput(inp);
   if(temp==0) return;
   else tuning_int=temp;
   break; 

  case 'A':
   if(strcmp(updatetstv, "YES")==0) strcpy(updatetstv, "NO");
   else strcpy(updatetstv, "YES");
   break;

   // Dirk Husmeier, 21 May 2001
  case 'J':
   if(strcmp(readInitMosaic, "YES")==0) strcpy(readInitMosaic, "NO");
   else strcpy(readInitMosaic, "YES");
   break;

   // TDH, July 2001
  case 'W':
   if(strcmp(updateLambda, "YES")==0) strcpy(updateLambda, "NO");
   else strcpy(updateLambda, "YES");
   break;

  case 'U':
   if(strcmp(updatepi, "YES")==0) strcpy(updatepi, "NO");
   else strcpy(updatepi, "YES");
   break;

  case 'O':
   cout << "Enter the branch length for the initial trees ";
   cout << "(all lengths start off equal): ";
   cin >> inp;
   temf=CheckInput(inp,dummy);
   if(temf==0) return;
   else mbl=(double)temf;
   break;

  case 'L':
   cout << "Enter the name for the LPD file (max 25 characters): ";
   cin >> filename;
   strcpy(lpdfile, filename);
   break;

  case 'G':
   cout << "Enter the name for the mean bl file (max 25 characters): ";
   cin >> filename;
   strcpy(branchfile, filename);
   break;

  case 'V':
   cout << "Enter the name for the stationary frequency";
   cout << " file (max 25 characters): ";
   cin >> filename;
   strcpy(pifile, filename);
   break;

  case 'K':
   cout << "Enter the name for the ts-tv ratio file (max 25 characters): ";
   cin >> filename;
   strcpy(tstvfile, filename);
   break;

  case 'S':
   cout << "Enter the name for the settings file (max 25 characters): ";
   cin >> filename;
   strcpy(settingsfile, filename);
   break;

   /*******
  case 'H':
   cout << "Enter the name for the Lambda file (max 25 characters)\n";
   cout << "(contains difficulty of changing topology): ";
   cin >> filename;
   strcpy(lambdafile, filename);
   break;
   ********/

  case '7':
   cout << "Enter the name of the data file (max 25 characters): ";
   cin >> filename;
   strcpy(datafile, filename);
   break;

  case 'Z':
   cout << "Enter the name for topology posterior probability file: ";
   cin >> filename;
   strcpy(postprobfile, filename);
   break;

  case 'Y':
   break;

  case '\n':
   break;

  default:
   cout << "\a\n\n***Invalid option chosen. Please try again***\n\n";
   break;
 }
}

//-----------------END OF FUNCTION-----------------//

char Options::DisplayModelmenu()
{
  // displays options to do with the model (what model, initial parameter
  // values etc).
 cout << "\t\t\tModel Options\n\t\t\t-------------\n";
 cout << "Model (JC+gaps, K2P+gaps, F81+gaps and F84+gaps)\t";
 cout << "M\t" << modeltype << endl;
 if(strcmp(modeltype, "JC")!=0 && strcmp(modeltype, "K2P")!=0)
 {
  cout << "Estimate initial character frequencies from data\t";
  cout << "E\t" << estimatefreqs << endl;
  if(strcmp(estimatefreqs, "YES")!=0)
    cout << "Type F to see and change the frequency estimates\t" << "F\n";
 } 
 if(strcmp(modeltype, "F81")!=0 && strcmp(modeltype, "JC")!=0)
 {
  cout << "Estimate transition/transversion ratio from data\t" << "R\t";
  cout << etstv << endl;
  if(strcmp(etstv, "YES")!=0)
  {
   cout << "Initial transition-transversion ratio\t\t\t";
   cout << "T\t" << tstv << endl;
  }
 }

 cout << "Frequencies of the three topologies\t\t\t";
 cout << "P\n\t";
 for(int i=0; i<3; i++) cout << topfreq[0] << " ";
 cout << endl;
 cout << "Difficulty of changing trees\t\t\t\t" << "D\t" << diffic << endl;

 // Dirk Husmeier, 21 May 2001
  cout << "Read in initial hidden state sequence?\t\t\t" << "J\t";
  cout << readInitMosaic << endl;

 char choice;
 cout << "\n\nEnter in a letter to choose an option to change\n";
 cout << "or enter X to return to the main menu: ";
 cin >> choice;
 return toupper(choice);
}

//-----------------END OF FUNCTION-----------------//

char Options::DisplayRunmenu()
{
  // displays the run settings submenu
 cout << "\t\t\tRun settings menu\n\t\t\t-----------------\n";
 cout << "\nLength of the burn-in period\t\t\t" << "\tB\t" << burn << endl;
 cout << "Length of the sampling period\t\t\t\t\t" << numsamples*interval << endl;
 cout << "Number of points to return\t\t\t"<< "\tN\t"<< numsamples << endl;
 cout << "Thinning interval\t\t\t\t" << "\tI\t" << interval << endl;
 cout << "Tuning interval (for proposal distributions)\t";
 cout << "\tC\t" << tuning_int << endl;

 /* TDH, 6 July 2001 */
  cout << "Update lambda with Gibbs sampling \t\t";
  cout << "\tW\t" << updateLambda << endl;

 /* TDH, 10 July 2001 */ 
  if(strcmp(updateLambda, "YES")==0){
    cout << "Annealing scheme for lambda\t\t\t";
    cout << "\tQ\t" << annealLambda << endl;
  }

 if(strcmp(modeltype, "F81")==0 || strcmp(modeltype, "F84")==0)
 {
  cout << "Update stationary frequencies in MCMC algorithm\t";
  cout << "\tU\t" << updatepi << endl;
 }
 if(strcmp(modeltype, "K2P")==0 || strcmp(modeltype, "F84")==0)
 {
  cout << "Update transition-transversion ratio in MCMC algorithm";
  cout <<"\tA\t" << updatetstv << endl;
 }

 cout << "Branch length in initial trees\t\t\t\tO\t" << mbl << endl;

 char choice;
 cout << "\n\nEnter in a letter to choose an option to change\n";
 cout << "or enter X to return to the main menu: ";
 cin >> choice;
 return toupper(choice);
}

//-----------------END OF FUNCTION-----------------//

char Options::DisplayFilemenu()
{
 cout << "\t\t\tFile settings menu\n\t\t\t------------------\n";
 cout << "\nFile containing the data\t\t\t\t7\t" << datafile << endl;
 cout << "File for storing the settings\t\t\t";
 cout << "\tS\t" << settingsfile << endl;

 cout << "File for storing lpd values\t\t\t" << "\tL\t" << lpdfile << endl;
 cout << "File for storing mean branch length\t\t";
 cout << "\tG\t" << branchfile << endl;
 // cout << "File for storing lambda values\t\t\t\tH\t" << lambdafile << endl;
 cout << "File for storing the topology posterior probs\t";
 cout << "\tZ\t" << postprobfile << endl;

 if(strcmp(modeltype, "F81")==0 || strcmp(modeltype, "F84")==0)
 {
  if(strcmp(updatepi, "YES")==0)
  {
   cout << "File for storing the stationary frequency values\t";
   cout << "V\t" << pifile << endl;
  }
 }

 if(strcmp(modeltype, "K2P")==0 || strcmp(modeltype, "F84")==0)
 {
  if(strcmp(updatetstv, "YES")==0)
  {
   cout << "File for storing the tstv ratio values\t\t";
   cout << "\tK\t" << tstvfile << endl;
  }
 }

 char choice;
 cout << "\n\nEnter in a letter to choose an option to change\n";
 cout << "or enter X to return to the main menu: ";
 cin >> choice;
 return toupper(choice);
}

//-----------------END OF FUNCTION-----------------//

void Options::PrintSettings()
{
 fstream fout;
 int i;

 fout.open(settingsfile, ios::out);
 fout << "\t\tModel settings\n\t\t--------------\n";
 fout << "Model\t\t\t\t\t\t\t" << modeltype << endl;
 if(strcmp(modeltype, "F81")==0 || strcmp(modeltype, "F84")==0)
 {
  fout << "Estimate initial stationary frequencies from data\t";
  fout << estimatefreqs << endl;
  if(strcmp(estimatefreqs, "NO")==0)
  {
   fout << "\tInitial frequencies: ";
   for(i=0; i<4; i++) fout << fr[i] << " ";
   fout << endl;
  }
 }
 if(strcmp(modeltype, "K2P")==0 || strcmp(modeltype, "F84")==0)
 {
  fout << "Estimate initial ts-tv ratio\t\t\t\t" << etstv << endl;
  if(strcmp(etstv, "NO")==0) fout << "\tInitial tstv ratio: " << tstv << endl;
 }
  // Dirk Husmeier, 21 May 2001
 fout << "Read in initial mosaic structure\t\t\t" << readInitMosaic << endl;

 fout << "\n\t\tRun Settings\n\t\t------------\n";
 fout << "Burn-in length\t\t\t\t\t\t" << burn << endl;
 cout << "Sampling length\t\t\t\t\t\t" << numsamples*interval << endl;
 fout << "No. of points returned\t\t\t\t\t" << numsamples << endl;
 fout << "Thinning interval\t\t\t\t\t" << interval << endl;
 fout << "Tuning interval for proposal distributions\t\t" << tuning_int << endl;

 if(strcmp(modeltype, "F81")==0 || strcmp(modeltype, "F84")==0)
        fout << "Update stationary frequencies\t\t\t\t" << updatepi << endl;

 if(strcmp(modeltype, "K2P")==0 || strcmp(modeltype, "F84")==0)
        fout << "Update tstv ratio\t\t\t\t\t" << updatepi << endl;

 /* TDH, July 2001 */
 fout << "Initial (mean) lambda \t\t\t\t\t" << diffic << endl;
 fout << "Update lambda \t\t\t\t\t\t" << updateLambda << endl;

 if(strcmp(updateLambda,"YES")==0)
   fout << "Annealing scheme for lambda\t\t\t\t"<<annealLambda<< endl;

 fout << "Branch length in initial trees\t\t\t\t" << mbl << endl;

 // file names now
 fout << "\n\t\tFile Settings\n\t\t-------------\n";
 fout << "Input data file\t\t\t\t\t\t" << datafile << endl;
 fout << "Topology posterior probability file\t\t\t" << postprobfile << endl;
 fout << "Log posterior density file\t\t\t\t" << lpdfile << endl;
 fout << "Mean Branch length file\t\t\t\t\t" << branchfile << endl;
 // fout << "Lambda file\t\t" << lambdafile << endl;
 if(strcmp(modeltype, "F81")==0 || strcmp(modeltype, "F84")==0)
 {
  if(strcmp(updatepi, "YES")==0)
        fout << "Stationary Frequency file\t\t\t\t" << pifile << endl;
 }
 if(strcmp(modeltype, "K2P")==0 || strcmp(modeltype, "F84")==0)
 {
  if(strcmp(updatetstv, "YES")==0)
           fout << "TsTv ratio file\t\t\t\t\t\t" << tstvfile << endl;
 }
 // Dirk Husmeier, 21 May 2001
 if(strcmp(readInitMosaic,"YES")==0)
        fout << "Initial mosaic structure file \t\t\t\t" << "mosaic.in" << endl;
}
//-----------------END OF FUNCTION-----------------//

int Options::CheckInput(char* inp)
{
  // checking function to make sure inputted value is actually an integer
  // prevents program from going mad.
  // if a non-integer value is entered no change is made to the option
 int ret=0;
 ret=atoi(inp);
 if(!ret){
  cout << "\a\n\n*** Incorrect type of value entered. Must be an integer";
  cout << "***\n\n\t\t\t***Try again***\n";
  return 0;
	 }
 return ret;
}

//-----------------END OF FUNCTION-----------------//

float Options::CheckInput(char* inp, int dummy)
{
  // checks that a float is entered when required. int dummy is used to
  // distinguish it from int CheckInput(char*) above.
 float ret=0.0f;
 ret= (float) atof(inp);
 if(!ret){
  cout << "\a\n\n*** Incorrect type of value entered. Must be a number***";
  cout << "\n\n\t\t\t***Try again***\n";
  return 0;
	 }
 return ret;
}
