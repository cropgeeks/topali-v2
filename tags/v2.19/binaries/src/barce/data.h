#ifndef DATA_H_
#define DATA_H_

/*=================================================================*
 *			data.h					   *
 *	Copyright (c) 2001, Grainne McGuire			   *
 *		     Version 1.00b, BARCE            		   *
 *								   *
 * Header file for data.cc. These two files declare and define the *
 * class used to read in the data and also the class used to       *
 * output the topologies, lpd and mean branch lengths.             *
 *=================================================================*/

#if defined(USING_ANSI_CPP_)

#include <iostream>
#include <fstream>
#include <cstring>
#include <cstdlib>
#include <cctype>
#include <cstdio>

#else

#include <iostream>
#include <fstream>
#include <string.h>
#include <stdlib.h>
#include <ctype.h>
#include <stdio.h>

#endif

#include "basics.h"

using namespace std;

static const int MAXFILENAME=51;
static const int LINE_LENGTH=80;
static const int MAXSEQNAME=21;   // max sequence name length is 20

typedef char* pchar;

// global variable used to store a tree and restart the chain.
// extern float sbunifreg;  // width of bl proposal distribution - remove??

/*===================================================================*
 *			class ReadData				     *
 * This class contains the functions which read in the sequence data *
 * from a file. The file must be in "loose sequential PHYLIP form",  *
 * ie the first line has only the number of sequences, the number of *
 * bases/residues. After that, the sequences must be: name of        *
 * sequence (all one word), followed by all of the sequence (on one  *
 * line or split over several).					     *
 * This class also amalgamates sites with the same pattern so the    *
 * sequence data stored internally becomes all the unique patterns   *
 * and a vector storing the number of occurences of each pattern.    *
 * Thus, there are two sequence lengths: the number of unique sites  *
 * and the total number of sites. Both are needed later on.	     *
 * Note that depending on the model further amalgamation of sites is *
 * possible. For example for the JC+Gaps model, the likelihood for   *
 * constant sites (all the same nuc/aa) is the same no matter the    *
 * base or residue. However, I have not implemented this.            *
 *								     *
 * A record is kept of the order of the sites since this is          *
 * necessary for the detection of recombination events.		     *
 *===================================================================*/

class ReadData
{
public:
ReadData();
~ReadData();

 void    ReadFileSeq(char*);     // for sequential files only
 char**  GetData()        { return data; }
 char**  GetSeqnames()    { return seqnames; }
 int     GetNseq()        { return nseq; }
 int     GetSeqlen()      { return seqlen; }
 int     GetConcatlen()   { return concatlen; }
 int*    GetWeight()      { return weight; }
 int*    GetSorted()      { return sorted; }
 double  GetTstv()        { return tstv; }
 double* GetPi()          { return pi; }

private:
 void    OpenFile(char*);
 void    ProcessCH(char c, int *i, int j);
 void    EstimateParams();
 void    ProcessData();

 ifstream finput;
 char** data;    // weights is a global variable (in basics.h) now
 char** seqnames;
 int    nseq;
 int    seqlen;
 int    concatlen;    // length of concatenated sequence
 int*   sorted;     // index of sorted values to reconstruct original data
 int*   weight;     // weight of each concatented site
 int*   count;  // holds counts of aa/nucs
 double *pi;    // estimated nucleotide frequencies
 double tstv;
};

/*================================================================*
 *			class OutPut				  *
 * Class which generates the output of the lists of topologies.   *
 * Following the solution to a similar problem by Martyn Bing,    *
 * an initial file is outputted containing the number of regions  *
 * and the beginning, ending and tree of each. These file is then *
 * processed to return the posterior probability of each site     *
 * being in tree 0, 1 or 2 (a numits x 3 file) which can then be  *
 * plotted in a statistical package.				  *
 * This class also handles other output like the log posterior    *
 * density and the mean branch length files			  *
 *================================================================*/

class OutPut
{
 public:
OutPut(int sl=0);
~OutPut() { }

 void SetSeqlen(int sl) { seqlen=sl; }
 void SetFilename(char* file) { strcpy(poutfile, file); }
 void OpenOutfile();
 void WriteX(int* x);   // x is the current topology sequence
 void ProcessOutput();

 void SetTree(Tree* t) { tree = t; }
 void OpenLogfile(char*);
 void PrintLogfile(double, char*);
 void OpenBranchfile(char*);
 void PrintBranchfile(double*, char*);

 private:
 void PrintFinalOutput(double**, int);

ofstream fout;
 char outfile[26];
 char poutfile[26];   // processed output file
 int loc;
 int seqlen;

Tree *tree;
ofstream flog, fbranch;
};

#endif
