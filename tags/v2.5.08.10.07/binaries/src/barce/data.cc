/*==================================================================*
 *			data.cc					    *
 *	Copyright (c) 2001, Grainne McGuire			    *
 *		     Version 1.00b, BARCE    			    *
 *								    *
 * Function file for data.h. These two files declare and define the *
 * class used to read in the data and the class used for data       *
 * output							    *
 *==================================================================*/

#include "data.h"

//int TOT_SEQLEN;  remove??

/*=============================================================*
 *			class ReadData			       *
 * Functions for reading the sequence data from a file and for *
 * finding all the unique site patterns.		       *
 *=============================================================*/

ReadData::ReadData()
{
 data=seqnames=0;
 seqlen = concatlen = 0;
 nseq=4;
 sorted = weight = 0;

 count=new int[lenalphabet];
 if(!count) { char mes[]="ReadData Constructor"; OutofMemory(mes); }
 pi=new double[lenalphabet];
 if(!pi) { char mes[]="ReadData Constructor"; OutofMemory(mes); }

 for(int i=0; i<lenalphabet; i++) count[i]=1;
}

//-------------END OF FUNCTION-------------//

ReadData::~ReadData()
{
 int i;
 if(data)
 {
  for(i=0; i<concatlen; i++) { delete [] data[i]; data[i]=0; }
  delete [] data;
 }

 if(seqnames)
 {
  for(i=0; i<nseq; i++) { delete [] seqnames[i]; seqnames[i]=0; }
  delete [] seqnames;
 }

 if(count) { delete [] count; count=0; }
 if(pi) { delete [] pi; pi=0; }
 if(sorted) { delete [] sorted; sorted=0; }
 if(weight) { delete [] weight; weight=0; }
}

//-------------END OF FUNCTION-------------//

void ReadData::OpenFile(char* dfile)
{
  // opens up file dfile for reading in data. Reads in the number of
  // sequences and the sequence length. Private member function

 finput.open(dfile, ios::in);  // ios::in not actually needed

//________________________________________________________________
#if defined(USING_ANSI_CPP_)

 if(!finput.is_open())
 {
  cout << "The file " << dfile << " does not exist.\n";
  cout << "Enter the name of the input file: ";
  cin >> dfile;
  finput.open(dfile, ios::in);
  while(!finput.is_open())
  {
   cout << "The file " << dfile << " does not exist.\n";
   cout << "Please enter a new filename: ";
   cin >> dfile;
   finput.open(dfile, ios::in);
  }
 }
#endif
//________________________________________________________________

 int temp;
 if(!(finput >> temp)){
  cerr << "Error reading no. of sequences at top of " << dfile << endl;
  exit(1);
		      }
 if(temp!=4)
 {
  cerr << "Error! Program can only handle four sequences at present.\n";
  cerr << "Exiting...\n";
  exit(1);
 }

 if(!(finput >> seqlen)){
  cerr << "Error reading length of sequences at top of " << dfile << endl;
  exit(1);
		        }
 finput.ignore(LINE_LENGTH, '\n');  // ignores rest of first line

 seqnames = new pchar[4];
 if(seqnames==0)
    { char mes[]="ReadData::OpenFile()"; OutofMemory(mes); }

  int i;
 for(i=0; i<nseq; i++)
 {          // setting up array for holding sequence names
  seqnames[i]=new char[MAXSEQNAME];
  if(!seqnames[i])
    { char mes[]="ReadData::OpenFile()"; OutofMemory(mes); }
 }

 data=new pchar[seqlen];   // setting up array to hold data
 if(!data)
    { char mes[]="ReadData::OpenFile()"; OutofMemory(mes); }
 for(i=0; i<seqlen; i++)
 {
  data[i]=new char[nseq];
  if(!data[i])
    { char mes[]="ReadData::OpenFile()"; OutofMemory(mes); }
 }
}

//-------------END OF FUNCTION-------------//

void ReadData::ReadFileSeq(char* dfile)
{
  // Function to read sequential data files

 OpenFile(dfile);           // opens finput and reads in nseq and seqlen
 char tempname[25];    // temporary holder for sequence name
 char ch;
 int i, j;

 for(j=0; j<nseq; j++)
 {
  if(!(finput >> tempname))
  {
   cerr << "Error reading name of sequence " << j+1 << endl;
   exit(1);
  }
  strncpy(seqnames[j], tempname, MAXSEQNAME-1);

  i=0;        // position in 1-d array of all the data
  while(i<seqlen)
  {
   finput.get(ch);
   ProcessCH(ch, &i, j);
  }      // end scanning along one sequence
 }  // end scanning entire data set


 int fsum = 0;
 for(i=0; i<lenalphabet; i++) fsum += count[i];
 for(i=0; i<lenalphabet; i++)
           pi[i]=(double)count[i]/fsum;

 EstimateParams();
 finput.close();
 ProcessData();   // concatenates the data
}

//-------------END OF FUNCTION-------------//

void ReadData::ProcessCH(char c, int* i, int j)
{
  // Private function which processes the input value of char from 
  // the input file. Increments i via pointers when necessary.
  // also works out gap lengths for downweighting purposes
 char ch=toupper(c);
 if(isdigit(ch)) return;  // ignores numbers

 switch (ch){                 // switch now copes with spaces, returns or
  case '\n':                  // tabs in input file. All other characters
   break;                     // get stored in sequence file so care still
  case ' ':                   // needed with format
   break;
  case '\t': break;
  case 'A': data[*i][j]='a'; (*i)++; count[0]++; break;
  case 'C': data[*i][j]='c'; (*i)++; count[1]++; break;
  case 'G': data[*i][j]='g'; (*i)++; count[2]++; break;
  case 'T': data[*i][j]='t'; (*i)++; count[3]++; break;
  case '-': data[*i][j]='-'; (*i)++;             break;
  case 'N': data[*i][j]='n'; (*i)++;             break;
  default:
   cerr << "Program can only deal with nucleotides or - at ";
   cerr << "present.\nOnly 'N' ambiguity code allowed.\n";
   exit(1);
	    }   // end switch
}

//-------------END OF FUNCTION-------------//

void ReadData::EstimateParams()
{
 int i, j, k;
 int ts=0, tv=0;

 for(i=0; i<nseq; i++)
 {
  for(j=i+1; j<nseq; j++)
  {
   for(k=0; k<seqlen; k++)
   {
     // counting for ts/tv ratio
    if(data[k][i]!='-' && data[k][j]!='-' && data[k][i]!=data[k][j])
    {
     if(data[k][i]=='a' && data[k][j]=='g') ts++;
     else if(data[k][i]=='g' && data[k][j]=='a') ts++;
     else if(data[k][i]=='c' && data[k][j]=='t') ts++;

     else if(data[k][i]=='t' && data[k][j]=='c') ts++;
     else tv++;
    }
   }
  }
 }
 tstv=(double)ts/tv;
}

//-------------END OF FUNCTION-------------//

void ReadData::ProcessData()
{
  // sorts the data and concatenates it into the set of distinct patterns

 int j;
 // sort the data
 if(!sorted) 
 {
  sorted = new int[seqlen];
  if(!sorted) { char mes[]="ReadData::ProcessData()"; OutofMemory(mes); }
  for(j=0; j<seqlen; j++) sorted[j]=j;
 }
 v_ssort(data, sorted, seqlen, nseq);
 // sorted now holds the key to restoring the original ordering of the sites

 // make the temporary data and weight vectors to hold the lookup table
 // (ie the pattern and number of occurences of it)
 char** tdata= new char*[seqlen];
 if(!tdata) { char mes[]="ReadData::ProcessData()"; OutofMemory(mes); }
 for(j=0; j<seqlen; j++)
 {
  tdata[j]=new char[nseq];
  if(!tdata[j]) { char mes[]="ReadData::ProcessData()"; OutofMemory(mes); }
 }
 int* tweights = new int[seqlen];
 if(!tweights) { char mes[]="ReadData::ProcessData()"; OutofMemory(mes); }
 for(j=0; j<seqlen; j++) tweights[j]=0;

 // forming the look-up table
 for(j=0; j<nseq; j++) tdata[0][j]=data[0][j];
 tweights[0]++;

 int loc=1, i=1;
 while(i<seqlen)
 {
  if( memcmp(data[i], tdata[loc-1], nseq)==0) { i++; tweights[loc-1]++; }
  else
  {
   for(j=0; j<nseq; j++) tdata[loc][j]=data[i][j];
   tweights[loc]++;
   loc++; i++;
  }
 }
 // creating the new data matrix consisting of the condensed data. The
 // sequence length has been reduced to loc.

 for(i=0; i<seqlen; i++) { delete [] data[i]; data[i]=0; }
 delete [] data;

 concatlen=loc;
 data=new char*[concatlen];
 if(!data) { char mes[]="ReadData::ProcessData()"; OutofMemory(mes); }
 for(i=0; i<concatlen; i++)
 {
  data[i]=new char[nseq];
  if(!data[i]) { char mes[]="ReadData::ProcessData()"; OutofMemory(mes); }
  for(j=0; j<nseq; j++) data[i][j]=tdata[i][j];
 }

 weight=new int[concatlen];
 if(!weight) { char mes[]="ReadData::ProcessData()"; OutofMemory(mes); }
 for(i=0; i<concatlen; i++) weight[i]=tweights[i];

 delete [] tweights;
 for(j=0; j<seqlen; j++) { delete [] tdata[j]; tdata[j]=0; }
 delete [] tdata;
}


/*================================================================*
 *			class OutPut				  *
 * Class which generates the output of the lists of topologies.   *
 * Following the solution to a similar problem by Martyn Bing,    *
 * an initial file is outputted containing the number of regions  *
 * and the beginning, ending and tree of each. These file is then *
 * processed to return the posterior probability of each site     *
 * being in tree 0, 1 or 2 (a numits x 3 file) which can then be  *
 * plotted in a statistical package.				  *
 *================================================================*/

OutPut::OutPut(int sl)
{
 seqlen = sl;
 strcpy(outfile, "tempfile");
 strcpy(poutfile, "posttop.out");
}

//-------------END OF FUNCTION-------------//

void OutPut::OpenOutfile()
{
  // opens the initial output file (pre-precessed)

 fout.open(outfile, ios::out);
 fout.close();
}

//-------------END OF FUNCTION-------------//

void OutPut::WriteX(int* x)
{
  // this function processes the topology list and writes out the
  // information to the holding file in the form
  // #regions
  // tree beg end
  // tree beg end   etc
  // x is the sequence of topologies

 int i=0, val=x[0], nregions=0;
 int* beg, *end, *wtree;

 beg = new int[seqlen];
 if(!beg) { char mes[]="OutPut::WriteX()"; OutofMemory(mes); }

 end = new int[seqlen];
 if(!end) { char mes[]="OutPut::WriteX()"; OutofMemory(mes); }

 wtree = new int[seqlen];
 if(!wtree) { char mes[]="OutPut::WriteX()"; OutofMemory(mes); }

 beg[nregions]=1;

 while(i<seqlen)
 {
  i++;
  if(i==seqlen) { end[nregions]=seqlen;wtree[nregions++]=x[i-1];continue; }

  if(x[i]!=val)
  {
   end[nregions]=i;  // i-1  +1  since labled 1-sl not 0-> sl-1
   wtree[nregions]=x[i-1];
   beg[++nregions]=i+1;
   val=x[i];
  }
 }

 fout.open(outfile, ios::app);
 fout << nregions << endl;
 for(i=0; i<nregions; i++)
             fout << wtree[i] << '\t' << beg[i] << '\t' << end[i] << endl;
 fout.close();

 delete [] beg; beg=0;
 delete [] end; end=0;
 delete [] wtree; wtree=0;
}

//-------------END OF FUNCTION-------------//

void OutPut::ProcessOutput()
{
  // called at the end of the program. Processes the output stored
  // in outfile and creates a file containing the posterior probability
  // of each site being in each topology.

 fstream fin;
 fin.open(outfile, ios::in);

 int i,j, num_its=0;
 int nr, beg, end, wt;
 int** count;
 double** prop;

 count = new int*[3];
 if(!count) { char mes[]="OutPut::ProcessOutput()"; OutofMemory(mes); }
 prop = new double*[3];
 if(!prop) { char mes[]="OutPut::ProcessOutput()"; OutofMemory(mes); }

 for(i=0; i<3; i++)
 {
  count[i] = new int[seqlen];
  if(!count[i]) { char mes[]="OutPut::ProcessOutput()"; OutofMemory(mes); }
  prop[i] = new double[seqlen];
  if(!prop[i]) { char mes[]="OutPut::ProcessOutput()"; OutofMemory(mes); }
 }

 for(i=0; i<3; i++)
 {
  for(j=0; j<seqlen; j++) count[i][j]=0;
 }

 while(!fin.eof())
 {
  fin >> nr;
  for(i=0; i<nr; i++)
  {
   fin >> wt >> beg >> end;
   for(j=beg-1; j<end; j++) count[wt][j]++;
  }
  fin.ignore(10, '\n');
  if(fin.peek()==EOF) break;
  //  num_its++;
 } 
 num_its=count[0][0]+count[1][0]+count[2][0];
 fin.close();

 for(i=0; i<3; i++)
 {
  for(j=0; j<seqlen; j++) prop[i][j] = (double)count[i][j]/num_its;
 }

 PrintFinalOutput(prop, num_its);

 for(i=0; i<3; i++)
 {
  delete [] count[i]; count[i]=0;
  delete [] prop[i]; prop[i]=0;
 }
 delete [] count;
 delete [] prop;
}

//-------------END OF FUNCTION-------------//

void OutPut::PrintFinalOutput(double** pr, int ni)
{
  // prints out the final output from the program. This is in the form
  // of a (ni x 3) matrix containing the posterior probability of
  // each site being in each of the three topologies.

 ofstream postout;
 postout.open(poutfile, ios::out);

 for(int j=0; j<seqlen; j++)
 {
  for(int i=0; i<3; i++) postout << pr[i][j] << '\t';
  postout << endl;
 }

 postout.close();
} 

//-------------END OF FUNCTION-------------//

void OutPut::OpenLogfile(char* logfile)
{
  flog.open(logfile, ios::out);
  flog.close();
}

//-------------END OF FUNCTION-------------//

void OutPut::PrintLogfile(double lh, char* logfile)
{
  // function which prints the log posterior probability of the sampled
  // point to a log file
 flog.open(logfile, ios::app);
 flog << lh << endl;
 flog.close();
}

//-------------END OF FUNCTION-------------//

void OutPut::OpenBranchfile(char* branchfile)
{
 fbranch.open(branchfile, ios::out);
 fbranch.close();
}

//-------------END OF FUNCTION-------------//

void OutPut::PrintBranchfile(double *mbl, char* branchfile)
{
  // function which prints out the total branch length of the
  // sampled point to a log file
 fbranch.open(branchfile, ios::app);
 for(int i=0; i<3; i++) fbranch << mbl[i] << " ";
 fbranch << endl;
 fbranch.close();
}
