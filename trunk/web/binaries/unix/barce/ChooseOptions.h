#ifndef CHOOSEOPTIONS_H_
#define CHOOSEOPTIONS_H_

//===================================================================//
//			ChooseOptions.h				     //
//	Copyright (c) 2001, Grainne McGuire			     //
//		     Version 1.00b, BARCE			     //
// Header file for the ChooseOptions class which sets up a text menu //
// which allows the various inputs (run parameters, file names etc   //
// to be set. Modified from the TOPAL 2.01b program ChooseOptions    //
// 16.8.00  Due to large numbers of options, three menus (filenames, //
// run settings and model settings) have been created. Bools say     //
// whether they should be displayed or not.			     //
//===================================================================//

#if defined(USING_ANSI_CPP_)

#include <cmath>
#include <iostream>
#include <cstdlib>
#include <cstring>
#include <cctype>
#include <fstream>

#ifndef USING_NAMESPACE_STD_
#define USING_NAMESPACE_STD_
using namespace std;
#endif

#else  /* ie not ANSI C++  */

#include <stdio.h>
#include <iostream>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <fstream>
#endif

#include "basics.h"

using namespace std;

const int NSTORE=200000;

struct Options
{
 Options();		    // constructor
// Options(Options &other);    copy constructor
 ~Options() { }		    // destructor
 
 char DisplayOptions();             // display and choose an option
 char DisplayModelmenu();
 char DisplayRunmenu();
 char DisplayFilemenu();
 void ChangeOptions(char choice);    // change the option.
 int CheckInput(char* inp);              // makes sure input is okay
 float CheckInput(char* inp, int dummy); 
 int CheckArgs();		    // checks and sets correct arguments
 void PrintSettings();

 char modeltype[6];     // which model to use
 char estimatefreqs[4]; // if YES estimate frequencies from data
 double fr[4];		// vector which holds inputted frequencies
 double tstv;		// initial parameter ratios for F84 and K2P
 char updatetstv[4];	// if YES updates transition-transverstion ratio
 char updateLambda[4];  // if YES, updates Lambda
 char updatepi[4];	// if YES, updates stationary freqs
 char etstv[4];         // if YES estimates initial tstv from data
 char readInitMosaic[4]; // if YES reads in initial mosaic structure
                         // TDH, May 2001 
 char annealLambda[6];  // indicates the annealing scheme for lambda 
                        // TDH, July 2001
 double mbl;		// mean branch length in initial tree
 double topfreq[3];     // topology frequencies
 double diffic;		// patch length

 int burn;		// length of burn-in period
 long int numsamples;	// number of points to be returned
 int interval;		// interval at which to sample the points
 int tuning_int;	// interval to tune the q functions during burn-in

 char arfile[26];	// name of file storing AR values
 char lpdfile[26];	// name of file storing lpd values
 char branchfile[26];   // name of file storing mean branch length
 char tstvfile[26];	// name of file storing param ratios (F81G1 + F84)
 char pifile[26];
 char settingsfile[26];
 char postprobfile[26]; // name of file storing posterior probs of tops
 char datafile[26];
  // char lambdafile[26];

bool filemenu;
bool runmenu;
bool modelmenu;
}; 


#endif
