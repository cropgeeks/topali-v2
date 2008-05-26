/*  RAxML-VI-HPC (version 2.2) a program for sequential and parallel estimation of phylogenetic trees 
 *  Copyright August 2006 by Alexandros Stamatakis
 *
 *  Partially derived from
 *  fastDNAml, a program for estimation of phylogenetic trees from sequences by Gary J. Olsen
 *  
 *  and 
 *
 *  Programs of the PHYLIP package by Joe Felsenstein.
 *
 *  This program is free software; you may redistribute it and/or modify its
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *
 *  For any other enquiries send an Email to Alexandros Stamatakis
 *  Alexandros.Stamatakis@epfl.ch
 *
 *  When publishing work that is based on the results from RAxML-VI-HPC please cite:
 *
 *  Alexandros Stamatakis:"RAxML-VI-HPC: maximum likelihood-based phylogenetic analyses with thousands of taxa and mixed models". 
 *  Bioinformatics 2006; doi: 10.1093/bioinformatics/btl446
 */


#include <sys/times.h>
#include <sys/types.h>
#include <sys/time.h>
#include <unistd.h> 
#include <math.h>
#include <time.h> 
#include <stdlib.h>
#include <stdio.h>
#include <ctype.h>
#include <string.h>

#ifdef PARALLEL
#include <mpi.h>
#endif

#include "axml.h"
#include "globalVariables.h"

/***************** UTILITY FUNCTIONS **************************/


double gettime()
{
  struct timeval ttime;
  gettimeofday(&ttime , NULL);
  return ttime.tv_sec + ttime.tv_usec * 0.000001;
}

int gettimeSrand()
{
  struct timeval ttime;
  gettimeofday(&ttime , NULL);
  return ttime.tv_sec + ttime.tv_usec;
}

double randum (long  *seed)
    /* random number generator, modified to use 12 bit chunks */
  { /* randum */
    long  sum, mult0, mult1, seed0, seed1, seed2, newseed0, newseed1, newseed2;
    double res;

    mult0 = 1549;
    seed0 = *seed & 4095;
    sum  = mult0 * seed0;
    newseed0 = sum & 4095;
    sum >>= 12;
    seed1 = (*seed >> 12) & 4095;
    mult1 =  406;
    sum += mult0 * seed1 + mult1 * seed0;
    newseed1 = sum & 4095;
    sum >>= 12;
    seed2 = (*seed >> 24) & 255;
    sum += mult0 * seed2 + mult1 * seed1;
    newseed2 = sum & 255;

    *seed = newseed2 << 24 | newseed1 << 12 | newseed0;
    res = 0.00390625 * (newseed2 + 0.000244140625 * (newseed1 + 0.000244140625 * newseed0));  

    return res;   
  } /* randum */

int filexists(char *filename)
{
  FILE *fp;
  int res;
  fp = fopen(filename,"r");
  
  if(fp) 
    {
      res = 1;
      fclose(fp);
    }
  else 
    res = 0;
       
  return res;
} 

/********************* END UTILITY FUNCTIONS ********************/


/******************************some functions for the likelihood computation ****************************/

void *getxnode (nodeptr p)  
{ 
  nodeptr  s;

  if (! (p->x)) 
    { 
      if ((s = p->next)->x || (s = s->next)->x) 
	{
	  p->x = s->x;
	  s->x = NULL;
        }     
    }
  return  p->x;
}




void hookup (nodeptr p, nodeptr q, double z)
  { /* hookup */
    p->back = q;
    q->back = p;
    p->z = q->z = z;
  } /* hookup */

/******************************some functions for the likelihood computation ****************************/




/***********************reading and initializing input ******************/

void getnums (rawdata *rdta)
    /* input number of species, number of sites */
  { /* getnums */
    

    if (fscanf(INFILE, "%d %d", & rdta->numsp, & rdta->sites) != 2) 
      {
	if(processID == 0)
	  printf("ERROR: Problem reading number of species and sites\n");
	errorExit(-1);
      }
   

    if (rdta->numsp < 4) 
      {
	if(processID == 0)
	  printf("TOO FEW SPECIES\n");
	errorExit(-1);
      }

    if (rdta->sites < 1) 
      {
	if(processID == 0)
	  printf("TOO FEW SITES\n");
	errorExit(-1);
      }

    return;
  } /* getnums */


boolean digitchar (int ch) 
{
  return (ch >= '0' && ch <= '9'); 
}


boolean whitechar (int ch)
{ 
  return (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r'); /* PC-LINEBREAK*/
}


void uppercase (int *chptr)
    /* convert character to upper case -- either ASCII or EBCDIC */
  { /* uppercase */
    int  ch;

    ch = *chptr;
    if ((ch >= 'a' && ch <= 'i') || (ch >= 'j' && ch <= 'r')
                                 || (ch >= 's' && ch <= 'z'))
      *chptr = ch + 'A' - 'a';
  } /* uppercase */


int findch (int c)
  { /* findch */
    int ch;

    while ((ch = getc(INFILE)) != EOF && ch != c) ;
    return  ch;
  } /* findch */

void getyspace (rawdata *rdta)
  { /* getyspace */
    long   size;
    int    i;
    char *y0;

    if (! (rdta->y = (char **) malloc((rdta->numsp + 1) * sizeof(char *)))) 
      {
	printf("ERROR: Unable to obtain space for data array pointers\n");
	exit(-1);
      }

    size = 4 * (rdta->sites / 4 + 1);
    if (! (y0 = (char *) malloc((rdta->numsp + 1) * size * sizeof(char)))) 
      {
	printf("ERROR: Unable to obtain space for data array\n");
	exit(-1);
      }

    rdta->y0 = y0;

    for (i = 0; i <= rdta->numsp; i++) 
      {
	rdta->y[i] = y0;
	y0 += size;
      }

    return;
  } /* getyspace */


void freeyspace(rawdata *rdta)
{
  long   size;
  int    i;
  char *y0;
  for (i = 0; i <= rdta->numsp; i++) 
    {
      free(rdta->y[i]);    
    }
}



boolean setupTree (tree *tr, int nsites, analdef *adef)
  { /* setupTree */
    nodeptr  p0, p, q;
    int      i, j, tips, inter, code;
    
    tr->gammaCategories = 4;

    if(!adef->useMultipleModel)
      tr->NumberOfModels = 1;

    if(adef->grouping)
      tr->grouped = TRUE;
    else
      tr->grouped = FALSE;

    if(adef->constraint)
      tr->constrained = TRUE;
    else
      tr->constrained = FALSE;
    
    tr->treeID = 0;

    tips  = tr->mxtips;
    inter = tr->mxtips - 1;


    if(adef->model == M_PROTCAT || adef->model == M_PROTGAMMA)
      {
	tr->numberOfRates = 190;
	tr->protTip      = (protlikelivector *)malloc(tr->NumberOfModels * 23 * sizeof(protlikelivector));    
	tr->frequencies  = (double *)malloc(tr->NumberOfModels * 20  * sizeof(double));
	tr->EI           = (double *)malloc(tr->NumberOfModels * 380 * sizeof(double));
	tr->EV           = (double *)malloc(tr->NumberOfModels * 400 * sizeof(double));
	tr->EIGN         = (double *)malloc(tr->NumberOfModels * 19  * sizeof(double));
	tr->initialRates = (double *)malloc(tr->NumberOfModels * 190 * sizeof(double));
      }
    else
      {
	tr->numberOfRates = 5;
	tr->gtrTip       = (likelivector *)malloc(tr->NumberOfModels * 16 * sizeof(likelivector));    
	tr->frequencies  = (double *)malloc(tr->NumberOfModels * 4 * sizeof(double));
	tr->EI           = (double *)malloc(tr->NumberOfModels * 12 * sizeof(double));
	tr->EV           = (double *)malloc(tr->NumberOfModels * 16 * sizeof(double));
	tr->EIGN         = (double *)malloc(tr->NumberOfModels * 3 * sizeof(double));
	tr->initialRates = (double *)malloc(tr->NumberOfModels * 5 * sizeof(double));
	tr->ttRatios     = (double *)malloc(tr->NumberOfModels * sizeof(double));
	tr->xvs          = (double *)malloc(tr->NumberOfModels * sizeof(double));
	tr->invfreqrs    = (double *)malloc(tr->NumberOfModels * sizeof(double));
	tr->invfreqys    = (double *)malloc(tr->NumberOfModels * sizeof(double));
      }
   
    tr->gammaRates   = (double *)malloc(tr->NumberOfModels * 4 * sizeof(double));
    tr->alphas       = (double *)malloc(tr->NumberOfModels * sizeof(double));
    tr->fracchanges  = (double *)malloc(tr->NumberOfModels * sizeof(double));
    tr->likelihoods  = (double *)malloc(adef->multipleRuns * sizeof(double));
    tr->treeStringLength = tr->mxtips * (nmlngth+128) + 256 + tr->mxtips * 2;
    tr->tree_string  = (char   *)malloc(tr->treeStringLength * sizeof(char));
    /*TODO, must that be so long ?*/
  
    tr->nameList = (char **)malloc(sizeof(char *) * (tips + 1));    
             
    if (!(p0 = (nodeptr) malloc((tips + 3*inter) * sizeof(node)))) {
      printf("ERROR: Unable to obtain sufficient tree memory\n");
      return  FALSE;
      }


    if (!(tr->nodep = (nodeptr *) malloc((2*tr->mxtips) * sizeof(nodeptr)))) {
      printf("ERROR: Unable to obtain sufficient tree memory, too\n");
      return  FALSE;
      }

    
    tr->nodep[0] = (node *) NULL;    /* Use as 1-based array */

    for (i = 1; i <= tips; i++) {    /* Set-up tips */
      p = p0++;
      p->x      =  NULL;
      p->tip    = (char *) NULL;      
      p->number = i;
      p->next   = p;
      p->back   = (node *) NULL;      
      tr->nodep[i] = p;
      }

    for (i = tips + 1; i <= tips + inter; i++) { /* Internal nodes */
      q = (node *) NULL;
      for (j = 1; j <= 3; j++) {
        p = p0++;
        p->x      =  NULL;
        p->tip    = (char *) NULL;
        p->number = i;

        p->next   = q;
        p->back   = (node *) NULL;
        q = p;
        }
      p->next->next->next = p;
      tr->nodep[i] = p;
      }

    tr->likelihood  = unlikely;
    tr->start       = (node *) NULL;
    tr->ntips       = 0;
    tr->nextnode    = 0;
    tr->prelabeled  = TRUE;
    tr->smoothed    = FALSE;
   
    return TRUE;
  } /* setupTree */

void freeTreeNode (nodeptr p)   /* Free tree node (sector) associated data */
  { /* freeTreeNode */
    if (p) {
      if (p->x) 
	{
	  free(p->x);      
        }
      }
  } /* freeTree */

void freeTree (tree *tr)
  { /* freeTree */
    nodeptr  p, q;
    int  i, tips, inter;

    tips  = tr->mxtips;
    inter = tr->mxtips - 1;

    for (i = 1; i <= tips; i++) freeTreeNode(tr->nodep[i]);

    for (i = tips + 1; i <= tips + inter; i++) {
      if (p = tr->nodep[i]) {
        if (q = p->next) {
          freeTreeNode(q->next);
          freeTreeNode(q);
          }
        freeTreeNode(p);
        }
      }
    

    free(tr->nodep[1]);       /* Free the actual nodes */
  } /* freeTree */



boolean getdata (boolean reRead, analdef *adef, rawdata *rdta, tree *tr)
{
  int   i, j, k, l, basesread, basesnew, ch, my_i;
  int   meaning[256];
  char *nameptr;
  boolean  allread, firstpass;
  char buffer[300]; 
  int len;
   
  for (i = 0; i <= 255; i++) 
    meaning[i] = -1;

  if(adef->model == M_PROTCAT ||  adef->model == M_PROTGAMMA)
    {
      meaning['A'] =  0;  /* alanine */
      meaning['R'] =  1;  /* arginine */
      meaning['N'] =  2;  /*  asparagine*/
      meaning['D'] =  3;  /* aspartic */
      meaning['C'] =  4;  /* cysteine */
      meaning['Q'] =  5;  /* glutamine */
      meaning['E'] =  6;  /* glutamic */
      meaning['G'] =  7;  /* glycine */
      meaning['H'] =  8;  /* histidine */
      meaning['I'] =  9;  /* isoleucine */
      meaning['L'] =  10; /* leucine */
      meaning['K'] =  11; /* lysine */
      meaning['M'] =  12; /* methionine */
      meaning['F'] =  13; /* phenylalanine */
      meaning['P'] =  14; /* proline */
      meaning['S'] =  15; /* serine */
      meaning['T'] =  16; /* threonine */
      meaning['W'] =  17; /* tryptophan */
      meaning['Y'] =  18; /* tyrosine */
      meaning['V'] =  19; /* valine */
      meaning['B'] =  20;/* asparagine, aspartic 2 and 3*/
      meaning['Z'] =  21;/*21 glutamine glutamic 5 and 6*/
      meaning['X'] =  meaning['?'] = meaning['*'] = meaning['-'] = 22; /* all = 1.0 */
    }
  else
    {      
      meaning['A'] =  1;
      meaning['B'] = 14;
      meaning['C'] =  2;
      meaning['D'] = 13;
      meaning['G'] =  4;
      meaning['H'] = 11;
      meaning['K'] = 12;
      meaning['M'] =  3;
      meaning['N'] = 15;
      meaning['O'] = 15;
      meaning['R'] =  5;
      meaning['S'] =  6;
      meaning['T'] =  8;
      meaning['U'] =  8;
      meaning['V'] =  7;
      meaning['W'] =  9;
      meaning['X'] = 15;
      meaning['Y'] = 10;     
      meaning['-'] = 15;	
      meaning['?'] = 15;
    }
  
  basesread = basesnew = 0;

  allread = FALSE;
  firstpass = TRUE;
  ch = ' ';

  while (! allread) 
    {
      for (i = 1; i <= tr->mxtips; i++) 
	{   	  
	  if (firstpass) 
	    {                      	       
	      ch = getc(INFILE);
	      while(ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r') /* PC-LINEBREAK*/
		{
		  ch = getc(INFILE);		  
		}	      
	      my_i = 0;	      

	      do 
		{
		  buffer[my_i] = ch;		  
		  ch = getc(INFILE);		   
		  my_i++;
		  if(my_i >= nmlngth)
		    {
		      if(processID == 0)
			{
			  printf("Taxon Name to long at taxon %d, adapt constant nmlngth in\n", i);
			  printf("axml.h, current setting %d\n", nmlngth);
			}
		      errorExit(-1);
		    }		 
		}
	      while(ch !=  ' ' && ch != '\n' && ch != '\t' && ch != '\r'); /* PC-LINEBREAK*/
		
	      if(!reRead)
		{
		  buffer[my_i] = '\0';
			
		  len = strlen(buffer) + 1;
			       
		  tr->nameList[i] = (char *)malloc(sizeof(char) * len);

		  strcpy(tr->nameList[i], buffer);		
		}
	    }

	  j = basesread;
	  while ((j < rdta->sites) && ((ch = getc(INFILE)) != EOF) && (ch != '\n') && (ch != '\r')) /* PC-LINEBREAK*/
	    {
	      uppercase(& ch);
	      if ((meaning[ch] != -1) || ch == '.') 
		{
		  j++;
		  if (ch == '.') 
		    {
		      if (i != 1) ch = rdta->y[1][j];
		      else 
			{
			  printf("ERROR: Dot (.) found at site %d of sequence 1\n", j + 1);
			  return  FALSE;
			}
		    }
		  rdta->y[i][j] = ch;
		}
	      else 
		if(whitechar(ch) || digitchar(ch)) ;
		else 
		  {
		    printf("ERROR: Bad base (%c) at site %d of sequence %d\n",
			   ch, j + 1, i);
		    return  FALSE;
		  }
	    }

	    if (ch == EOF) 
	      {
		printf("ERROR: End-of-file at site %d of sequence %d\n", j + 1, i);
		return  FALSE;
	      }

	    if (! firstpass && (j == basesread)) i--; 
	    else 
	      if (i == 1) basesnew = j;
	      else 
		if (j != basesnew) 
		  {
		    printf("ERROR: Sequences out of alignment\n");		    
		    printf("%d (instead of %d) residues read in sequence %d %s\n",
			   j - basesread, basesnew - basesread, i, tr->nameList[i]);
		    return  FALSE;
		  }
	    while (ch != '\n' && ch != EOF && ch != '\r') ch = getc(INFILE);  /* flush line *//* PC-LINEBREAK*/
	}                                                  /* next sequence */
      firstpass = FALSE;
      basesread = basesnew;
      allread = (basesread >= rdta->sites);
    }

 
    
  for (j = 1; j <= tr->mxtips; j++)    
    for (i = 1; i <= rdta->sites; i++) 	
      rdta->y[j][i] = meaning[rdta->y[j][i]];		
	    	
    return  TRUE;
  } /* getdata */



void inputweights (analdef *adef, rawdata *rdta, cruncheddata *cdta)
    /* input the character weights 0, 1, 2 ... 9, A, B, ... Y, Z */
  { /* inputweights */
    int i, ch, w, fres;
    FILE *weightFile;
    int *wv = (int *)malloc(sizeof(int) *  rdta->sites + 1);
  

    weightFile = fopen(weightFileName, "r");
    if (!weightFile)
      {
	if(processID == 0)
	  printf( "Could not open weight file: %s\n", weightFileName);
	errorExit(-1);
      }
     
    i = 1;
    
    while((fres = fscanf(weightFile,"%d", &w)) != EOF)
      {
	if(!fres)
	  {
	    if(processID == 0)
	      printf("error reading weight file probably encountered a non-integer weight value\n");
	    errorExit(-1);
	  }
	wv[i] = w;
	i++;	
      }
    
   
    if(i != (rdta->sites + 1))
      {
	if(processID == 0)
	  printf("number %d of weights not equal to number %d of alignment columns\n", i, rdta->sites);
	errorExit(-1);
      }

    cdta->wgtsum = 0;
    for(i = 1; i <= rdta->sites; i++) 
      {     
	rdta->wgt[i] = wv[i];
	cdta->wgtsum += rdta->wgt[i];
      }

    fclose(weightFile);
    free(wv);
  } /* inputweights */



void getinput (boolean reRead, analdef *adef, rawdata *rdta, cruncheddata *cdta, tree *tr)
{ 
  int i,j;
  getnums(rdta);

  if(!reRead)
    {                
      tr->mxtips         = rdta->numsp;
      rdta->wgt          = (int *)    malloc((rdta->sites + 1) * sizeof(int));
      rdta->wgt2         = (int *)    malloc((rdta->sites + 1) * sizeof(int));
      cdta->alias        = (int *)    malloc((rdta->sites + 1) * sizeof(int));
      cdta->reAlias      = (int *)    malloc((rdta->sites + 1) * sizeof(int));
      cdta->aliaswgt     = (int *)    malloc((rdta->sites + 1) * sizeof(int));      
      cdta->rateCategory = (int *)    malloc((rdta->sites + 1) * sizeof(int)); 
      tr->model          = (int *)    malloc((rdta->sites + 1) * sizeof(int));
      tr->saveModel      = (int *)    malloc((rdta->sites + 1) * sizeof(int));
      cdta->wr           = (double *) malloc((rdta->sites + 1) * sizeof(double));  
      cdta->wr2          = (double *) malloc((rdta->sites + 1) * sizeof(double));  	
      cdta->patrat       = (double *) malloc((rdta->sites + 1) * sizeof(double));
      cdta->patratStored = (double *) malloc((rdta->sites + 1) * sizeof(double));             
    }
      
  if(!adef->useWeightFile)
    {
      for (i = 1; i <= rdta->sites; i++) 
	rdta->wgt[i] = 1;
     
      cdta->wgtsum = rdta->sites;   
    }
  else    
    inputweights(adef, rdta, cdta);
      
  if(adef->useMultipleModel)        
    parsePartitions(adef, rdta, cdta, tr, reRead);    

  if(!reRead)
    {
      getyspace(rdta);
      setupTree(tr, rdta->sites, adef);
    }
  else
    {
      long size;
      free(rdta->y0);
      size = 4 * (rdta->sites / 4 + 1);
      rdta->y0 = (char *) malloc((rdta->numsp + 1) * size * sizeof(char));
      for (i = 0; i <= rdta->numsp; i++)       
	rdta->y[i] = &rdta->y0[size * i];             
    }
  
  if(!getdata(reRead, adef, rdta, tr))
    {
      printf("Problem reading alignment file \n");
      errorExit(1);
    }
        
  return;
} 

void makeboot (analdef *adef, rawdata *rdta, cruncheddata *cdta)
  {
    int  i, j, nonzero;
#ifdef PARALLEL
    long seed;
#endif
   
    nonzero = 0;
   
    for (i = 1; i <= rdta->sites; i++)  
      {
	if (rdta->wgt[i] > 0) nonzero++;		
      }

    for (j = 1; j <= nonzero; j++) 
      cdta->aliaswgt[j] = 0;     

#ifdef PARALLEL 
    seed = (long) gettimeSrand();
    for (j = 1; j <= nonzero; j++)
      cdta->aliaswgt[(int) (nonzero*randum(& seed)) + 1]++;
#endif    

#ifndef PARALLEL
    for (j = 1; j <= nonzero; j++)
      cdta->aliaswgt[(int) (nonzero*randum(& adef->boot)) + 1]++;
    
    /*
      Code for testing bootstrap. do not remove !
      srand(1234);
      for (j = 1; j <= nonzero; j++)
      cdta->aliaswgt[(rand() % nonzero) + 1]++;     
    */
#endif

    j = 0;
    cdta->wgtsum = 0;
    for (i = 1; i <= rdta->sites; i++) 
      {
	if (rdta->wgt[i] > 0)
	  cdta->wgtsum += (rdta->wgt2[i] = rdta->wgt[i] * cdta->aliaswgt[++j]);
	else
	  rdta->wgt2[i] = 0;
      }        
  } 




void sitesort (rawdata *rdta, cruncheddata *cdta, tree *tr, analdef *adef)   
{ 
  int  gap, i, j, jj, jg, k, n, nsp;
  int  *index, *category;
  boolean  flip, tied;
  char  **data;
    
  if(adef->useMultipleModel)
    category = tr->model;

  index    = cdta->alias;
  data     = rdta->y;
  n        = rdta->sites;
  nsp      = rdta->numsp;
  index[0] = -1;

  
  
      for (gap = n / 2; gap > 0; gap /= 2) 
	{
	  for (i = gap + 1; i <= n; i++) 
	    {
	      j = i - gap;
	      
	      do 
		{
		  jj = index[j];
		  jg = index[j+gap];
		  if(adef->useMultipleModel)
		    {		   
		      flip = (category[jj] >  category[jg]);
		      tied = (category[jj] == category[jg]);		    
		    }
		  else
		    {		    
		      flip = 0;
		      tied = 1;
		    }
		  
		  for (k = 1; (k <= nsp) && tied; k++) 
		    {
		      flip = (data[k][jj] >  data[k][jg]);
		      tied = (data[k][jj] == data[k][jg]);
		    }
		  
		  if (flip) 
		    {
		      index[j]     = jg;
		      index[j+gap] = jj;
		      j -= gap;
		    }
		} 
	      while (flip && (j > 0));	      
	    }  
	}
    
} 


void sitecombcrunch (rawdata *rdta, cruncheddata *cdta, tree *tr, analdef *adef)
    
  { 
    int  i, sitei, j, sitej, k;
    int gaps = 0;
    boolean  tied;
    int *aliasModel;

    
    if(adef->useMultipleModel)
      aliasModel = (int*)malloc(sizeof(int) * (rdta->sites + 1));

    i = 0;    
    cdta->alias[0] = cdta->alias[1];
    cdta->aliaswgt[0] = 0;   
    
    
    for (j = 1; j <= rdta->sites; j++) 
      {
	sitei = cdta->alias[i];
	sitej = cdta->alias[j];

	if(adef->useMultipleModel)
	  {
	    tied = (tr->model[sitei] == tr->model[sitej]);
	  }
	else
	  {	    
	    tied = 1;
	  }

	for (k = 1; tied && (k <= rdta->numsp); k++)
	  tied = (rdta->y[k][sitei] == rdta->y[k][sitej]);

	if (tied) 
	  {
	    cdta->aliaswgt[i] += rdta->wgt2[sitej];	   
	    if(adef->useMultipleModel)
	      aliasModel[i] = tr->model[sitej];
	       
	  }
	else 
	  {
	    if (cdta->aliaswgt[i] > 0) i++;
	    cdta->aliaswgt[i] = rdta->wgt2[sitej];
	    cdta->alias[i] = sitej;
	    if(adef->useMultipleModel)
	      aliasModel[i] = tr->model[sitej];
	   
	  }
      }

    cdta->endsite = i;
    if (cdta->aliaswgt[i] > 0) cdta->endsite++;       


    if(adef->useMultipleModel)
      {       
	for(i = 0; i <= rdta->sites; i++)
	  tr->model[i] = aliasModel[i];	  
      }


    if(adef->useMultipleModel)
      free(aliasModel);	
  } 




boolean makeweights (analdef *adef, rawdata *rdta, cruncheddata *cdta, tree *tr)
    /* make up weights vector to avoid duplicate computations */
  { /* makeweights */
    int  i;

    if (adef->boot)  
      makeboot(adef, rdta, cdta);
    else 
      {
	for (i = 1; i <= rdta->sites; i++)  
	  rdta->wgt2[i] = rdta->wgt[i];
      }

    for (i = 1; i <= rdta->sites; i++)  
      cdta->alias[i] = i;
        
    sitesort(rdta, cdta, tr, adef);
    sitecombcrunch(rdta, cdta, tr, adef);
      
    return TRUE;
  } /* makeweights */


boolean makevalues (rawdata *rdta, cruncheddata *cdta, tree *tr, analdef *adef, boolean reRead)
    /* set up fractional likelihoods at tips */
  { /* makevalues */
    double  temp, wtemp;
    int  i, j, model;
    char buf;      

    char *y_buf = (char *)malloc(rdta->numsp * cdta->endsite * sizeof(char));

    for (i = 1; i <= rdta->numsp; i++)       
      for (j = 0; j < cdta->endsite; j++) 	  	   
	/*rdta->y[i-1][j] = rdta->y[i][cdta->alias[j]];*/
	y_buf[((i - 1) * cdta->endsite) + j] = rdta->y[i][cdta->alias[j]];	              

    for(i = 0; i < rdta->numsp; i++)
      rdta->y[i] = &y_buf[i * cdta->endsite];

    free(rdta->y0);
    rdta->y0 = y_buf;
                    
    if(adef->useMultipleModel)
      {
	if(!reRead)
	  {
	    tr->modelIndices = (int **)malloc(tr->NumberOfModels * sizeof(int *));
	
	    for(i = 0; i < tr->NumberOfModels; i++)
	      tr->modelIndices[i] = (int *)malloc(2 * sizeof(int));
	  }

	tr->modelIndices[0][0] = 0;	

	model = tr->model[0];
	i = 1;

	while(i <  cdta->endsite)
	  {
	    if(tr->model[i] != model)
	      {
		tr->modelIndices[model][1]     = i;
		tr->modelIndices[model + 1][0] = i;
		model = tr->model[i];
	      }
	    i++;
	  }

	tr->modelIndices[tr->NumberOfModels - 1][1] = cdta->endsite;
      }
    else
      {
	if(!reRead)
	  {
	    tr->modelIndices = (int **)malloc(1 * sizeof(int *));	
	    tr->modelIndices[0] = (int *)malloc(2 * sizeof(int));
	  }

	tr->modelIndices[0][0] = 0;
	tr->modelIndices[0][1] = cdta->endsite;
      }
    
    tr->rdta       = rdta;
    tr->cdta       = cdta; 

    for (i = 0; i < tr->mxtips; i++)   
      tr->nodep[i + 1]->tip = &y_buf[i * cdta->endsite];

    return TRUE;
  } 







int sequenceSimilarity(char *tipJ, char *tipK, int n)
{
  int i;
  
  for(i = 0; i < n; i++)    
    if(*tipJ++ != *tipK++)	
      return 0;	
      
  return 1;
}

void checkSequences(tree *tr, rawdata *rdta, analdef *adef)
{
  int n = tr->mxtips + 1;
  double sim;
  int i, j;
  int *omissionList     = (int *)malloc(n * sizeof(int));
  int *undeterminedList = (int *)malloc((rdta->sites + 1)* sizeof(int));
  int *modelList        = (int *)malloc((rdta->sites + 1)* sizeof(int)); 
  int count = 0;
  int countNameDuplicates = 0;
  int countUndeterminedColumns = 0;
  int countOnlyGaps = 0;
  int modelCounter = 1;
  char undetermined;
  char *tipI, *tipJ;
  FILE *f;

  if(processID == 0)	      
    f = fopen(infoFileName, "a");

  if(adef->model == M_PROTGAMMA || adef->model == M_PROTCAT)    
    undetermined = 22;
  else
    undetermined = 15;

  for(i = 1; i < n; i++)       
    omissionList[i] = 0;              

  for(i = 0; i < rdta->sites + 1; i++)
    undeterminedList[i] = 0;
      
  for(i = 1; i < n; i++)
    {
      for(j = i + 1; j < n; j++)
	if(strcmp(tr->nameList[i], tr->nameList[j]) == 0)
	  {
	    countNameDuplicates++;
	    if(processID == 0)
	      {
		printf("Sequence names of taxon %d and %d are identical, they are both called %s\n", i, j, tr->nameList[i]);
		fprintf(f, "Sequence names of taxon %d and %d are identical, they are both called %s\n", i, j, tr->nameList[i]);
	      }
	  }
    }
	  
  if(countNameDuplicates > 0)
    {
      if(processID == 0)
	{
	  printf("ERROR: Found %d taxa that had equal names in the alignment, exiting...\n", countNameDuplicates);
	  fprintf(f, "ERROR: Found %d taxa that had equal names in the alignment, exiting...\n", countNameDuplicates);
	  fclose(f);
	}
      errorExit(-1);
    }

  for(i = 1; i < n; i++)
    {
      j = 1;
      
      while(j <= rdta->sites && rdta->y[i][j] == undetermined)
	j++;

      if(j == (rdta->sites + 1))
	{       
	  if(processID == 0)
	    {
	      printf("ERROR: Sequence %s consists entirely of undetermined values which will be treated as missing data\n",      tr->nameList[i]);
	      fprintf(f, "ERROR: Sequence %s consists entirely of undetermined values which will be treated as missing data\n",      tr->nameList[i]);	      
	    }
	  countOnlyGaps++;
	}
      
    }
  
  if(countOnlyGaps > 0)
    {
      if(processID == 0)
	{
	  printf("ERROR: Found %d sequences that consist entirely of undetermined values, exiting...\n", countOnlyGaps);
	  fprintf(f, "ERROR: Found %d sequences that consist entirely of undetermined values, exiting...\n", countOnlyGaps);
	  fclose(f);
	}
      errorExit(-1);
    }

  for(i = 0; i <= rdta->sites; i++)
    modelList[i] = -1;

  for(i = 1; i <= rdta->sites; i++)
    {    
      j = 1;
     
      while(j < n && rdta->y[j][i] == undetermined)
	j++;
      
      if(j == n)
	{
	  undeterminedList[i] = 1;
	  if(processID == 0)
	    {
	      printf("IMPORTANT WARNING: Alignment column %d contains only undetermined values which will be treated as missing data\n", i);
	      fprintf(f, "IMPORTANT WARNING: Alignment column %d contains only undetermined values which will be treated as missing data\n", i);
	    }
	  countUndeterminedColumns++;	  
	}
      else
	{
	  if(adef->useMultipleModel)
	    {
	      modelList[modelCounter] = tr->model[i];
	      modelCounter++;
	    }
	}
    }
  

  for(i = 1; i < n; i++)
    {
      if(omissionList[i] == 0)
	{
	  tipI = &(rdta->y[i][1]);

	  for(j = i + 1; j < n; j++)
	    {
	      if(omissionList[j] == 0)
		{
		  tipJ = &(rdta->y[j][1]);
		  if(sequenceSimilarity(tipI, tipJ, rdta->sites))
		    {
		      if(processID == 0)
			{
			  printf("\n\nIMPORTANT WARNING: Sequences %s and %s are exactly identical\n", tr->nameList[i], tr->nameList[j]);
			  fprintf(f, "\n\nIMPORTANT WARNING: Sequences %s and %s are exactly identical\n", tr->nameList[i], tr->nameList[j]);
			}
		      omissionList[j] = 1;
		      count++;
		    }
		}
	    }
	}
    }

  if(count > 0 || countUndeterminedColumns > 0)
    {
      char noDupFile[2048];
      char noDupModels[2048];
              
      if(count > 0)
	{
	  if(processID == 0)
	    {
	      printf("\n");
	      
	      printf("IMPORTANT WARNING\n");
	      
	      printf("Found %d %s that %s exactly identical to other sequences in the alignment.\n", count, (count == 1)?"sequence":"sequences", (count == 1)?"is":"are");
	      printf("Normally they should be excluded from the analysis.\n\n");
	      
	      fprintf(f, "\n");
	      
	      fprintf(f, "IMPORTANT WARNING\n");
	      
	      fprintf(f, "Found %d %s that %s exactly identical to other sequences in the alignment.\n", count, (count == 1)?"sequence":"sequences", (count == 1)?"is":"are");
	      fprintf(f, "Normally they should be excluded from the analysis.\n\n");
	    }
	}
      
      if(countUndeterminedColumns > 0)
	{
	  if(processID == 0)
	    {
	      printf("\n");
	      
	      printf("IMPORTANT WARNING\n");
	      
	      printf("Found %d %s that %s only undetermined values which will be treated as missing data.\n", 
		     countUndeterminedColumns, (countUndeterminedColumns == 1)?"column":"columns", (countUndeterminedColumns == 1)?"contains":"contain");
	      printf("Normally these columns should be excluded from the analysis.\n\n");
	      
	      fprintf(f, "\n");
	      
	      fprintf(f, "IMPORTANT WARNING\n");
	      
	      fprintf(f, "Found %d %s that %s only undetermined values which will be treated as missing data.\n", 
		      countUndeterminedColumns, (countUndeterminedColumns == 1)?"column":"columns", (countUndeterminedColumns == 1)?"contains":"contain");
	      fprintf(f, "Normally these columns should be excluded from the analysis.\n\n");      	  
	    }
	}

      strcpy(noDupFile, seq_file);
      strcat(noDupFile, ".reduced");

      strcpy(noDupModels, modelFileName);
      strcat(noDupModels, ".reduced");

      if(processID == 0)
	{

	  if(adef->useMultipleModel && !filexists(noDupModels) && countUndeterminedColumns)
	    {      
	      FILE *newFile = fopen(noDupModels, "w");

	      printf("\nJust in case you might need it, a mixed model file with \n");
	      printf("model assignments for undetermined columns removed is printed to file %s\n",noDupModels);

	      fprintf(f, "\nJust in case you might need it, a mixed model file with \n");
	      fprintf(f, "model assignments for undetermined columns removed is printed to file %s\n",noDupModels);
	      
 
	      for(i = 0; i < tr->NumberOfModels; i++)
		{
		  boolean modelStillExists = FALSE;
		  
		  for(j = 1; (j <= rdta->sites) && (!modelStillExists); j++)
		    {
		      if(modelList[j] == i)
			modelStillExists = TRUE;
		    }

		  if(modelStillExists)
		    {	  
		      char *protModels[10] = {"DAYHOFF", "DCMUT", "JTT", "MTREV", "WAG", "RTREV", "CPREV", "VT", "BLOSUM62", "MTMAM"};
		      int k = 1;
		      int lower, upper;
		      int parts = 0;
		      
		      if(adef->model == M_PROTCAT || adef->model == M_PROTGAMMA)
			{
			  char AAmodel[1024];
			  
			  strcpy(AAmodel, protModels[adef->protModels[i]]);
			  if(adef->protFreqs[i])
			    strcat(AAmodel, "F");		  
			  
			  fprintf(newFile, "%s, ", AAmodel);
			}
		      
		      fprintf(newFile, "PARTITION_%d = ", i);
		      
		      while(k <= rdta->sites)
			{
			  if(modelList[k] == i)
			    {
			      lower = k;
			      while((modelList[k + 1] == i) && (k <= rdta->sites))		      			
				k++;
			      upper = k;
			      
			      if(lower == upper)		  
				{
				  if(parts == 0)
				    fprintf(newFile, "%d", lower);
				  else
				    fprintf(newFile, ",%d", lower);
				}
			      else
				{
				  if(parts == 0)
				    fprintf(newFile, "%d-%d", lower, upper);
				  else
				    fprintf(newFile, ",%d-%d", lower, upper);
				}		  
			      parts++;
			    }
			  k++;
			}
		      fprintf(newFile, "\n");
		    }		  
		}	
	      fclose(newFile);
	    }
	  else
	    {
	      printf("\n A mixed model file with model assignments for undetermined\n");
	      printf("columns removed has already been printed to  file %s\n",noDupModels);

	      fprintf(f, "\n A mixed model file with model assignments for undetermined\n");
	      fprintf(f, "columns removed has already been printed to  file %s\n",noDupModels);

	      
	    }
	     

	  if(!filexists(noDupFile))
	    {
	      FILE *newFile;
	      
	      printf("Just in case you might need it, an alignment file with \n");
	      if(count && !countUndeterminedColumns)
		printf("sequence duplicates removed is printed to file %s\n", noDupFile);
	      if(!count && countUndeterminedColumns)
		printf("undetermined columns removed is printed to file %s\n", noDupFile);
	      if(count && countUndeterminedColumns)
		printf("sequence duplicates and undetermined columns removed is printed to file %s\n", noDupFile);
	      
	      fprintf(f, "Just in case you might need it, an alignment file with \n");
	      if(count && !countUndeterminedColumns)
		fprintf(f, "sequence duplicates removed is printed to file %s\n", noDupFile);
	      if(!count && countUndeterminedColumns)
		fprintf(f, "undetermined columns removed is printed to file %s\n", noDupFile);
	      if(count && countUndeterminedColumns)
		fprintf(f, "sequence duplicates and undetermined columns removed is printed to file %s\n", noDupFile);
	      
	      newFile = fopen(noDupFile, "w");
	      
	      fprintf(newFile, "%d %d\n", tr->mxtips - count, rdta->sites - countUndeterminedColumns);
	      
	      for(i = 1; i < n; i++)
		{
		  if(!omissionList[i])
		    {
		      fprintf(newFile, "%s ", tr->nameList[i]);
		      tipI =  &(rdta->y[i][1]);
		      if(adef->model == M_PROTCAT || adef->model == M_PROTGAMMA)
			{
			  for(j = 0; j < rdta->sites; j++)
			    if(undeterminedList[j + 1] == 0)
			      fprintf(newFile, "%c", inverseMeaningPROT[tipI[j]]);
			}
		      else
			{
			  for(j = 0; j < rdta->sites; j++)
			    if(undeterminedList[j + 1] == 0)
			      fprintf(newFile, "%c", inverseMeaningDNA[tipI[j]]);
			}
		      fprintf(newFile, "\n");
		    }
		}
	      
	      fclose(newFile);	    
	    }
	  else
	    {
	      if(count && !countUndeterminedColumns)
		printf("An alignment file with sequence duplicates removed has already\n");
	      if(!count && countUndeterminedColumns)
		printf("An alignment file with undetermined columns removed has already\n");
	      if(count && countUndeterminedColumns)
		printf("An alignment file with undetermined columns and sequence duplicates removed has already\n");
	      
	      printf("been printed to file %s\n",  noDupFile);
	      
	      if(count && !countUndeterminedColumns)
		fprintf(f, "An alignment file with sequence duplicates removed has already\n");
	      if(!count && countUndeterminedColumns)
		fprintf(f, "An alignment file with undetermined columns removed has already\n");
	      if(count && countUndeterminedColumns)
		fprintf(f, "An alignment file with undetermined columns and sequence duplicates removed has already\n");
	      
	      fprintf(f, "been printed to file %s\n",  noDupFile);
	    }
	}
    }


  free(undeterminedList);
  free(omissionList);
  free(modelList);
  if(processID == 0)	      
    fclose(f);
}


void splitMultiGene(tree *tr, rawdata *rdta, analdef *adef)
{
  int i, l; 
  int currentModel = tr->model[0];
  int n = rdta->sites + 1;
  int *modelFilter = (int *)malloc(sizeof(int) * n);
  int length, k;
  char *tip;
  FILE *outf;
  char outFileName[2048];
  char buf[16];
  
  for(i = 0; i < tr->NumberOfModels; i++)
    {      
      strcpy(outFileName, seq_file);
      sprintf(buf, "%d", i);
      strcat(outFileName, ".GENE.");
      strcat(outFileName, buf);
      outf = fopen(outFileName, "w");
      length = 0;
      for(k = 1; k < n; k++)
	{
	  if(tr->model[k] == i)
	    {
	      modelFilter[k] = 1;
	      length++;
	    }
	  else
	    modelFilter[k] = -1;
	}

      fprintf(outf, "%d %d\n", rdta->numsp, length);
      
      for(l = 1; l <= rdta->numsp; l++)
	{
	  fprintf(outf, "%s ", tr->nameList[l]);

	  tip = &(rdta->y[l][0]);
	  for(k = 1; k < n; k++)
	    {
	      if(modelFilter[k] == 1)
		{
		  if(adef->model == M_PROTCAT || adef->model == M_PROTGAMMA)		    
		    fprintf(outf, "%c", inverseMeaningPROT[tip[k]]);
		  else
		    fprintf(outf, "%c", inverseMeaningDNA[tip[k]]);
		}
	    }
	  fprintf(outf, "\n");

	}
      
      fclose(outf);

      printf("Wrote individual gene/partition alignment to file %s\n", outFileName);
    }  
            
  free(modelFilter);
  printf("Wrote all %d individual gene/partition alignments\n", tr->NumberOfModels);
  printf("Exiting normally\n");
}







void allocNodex (tree *tr, analdef *adef)
{
  nodeptr  p;
  int  i;
    
  for (i = tr->mxtips + 1; (i <= 2*(tr->mxtips) - 2); i++) 
    {
      p = tr->nodep[i];
      allocCount++;
      
      switch(adef->model)
	{
	case M_PROTCAT:
	   p->x = malloc(tr->cdta->endsite * sizeof(protlikelivector));
	   break;
	case  M_PROTGAMMA:
	  p->x = malloc(tr->cdta->endsite * sizeof(protgammalikelivector));
	  break;	
	case M_GTRGAMMA:
	  p->x = malloc(tr->cdta->endsite * sizeof(gammalikelivector));	    	  
	  break;
	case M_GTRCAT:
	  p->x = malloc(tr->cdta->endsite * sizeof(likelivector)); 	    		  
	  break;	 
	default:
	  printf("FATAL ERROR  allocNodex\n");
	  exit(-1);
	}
	    
    }      
}


void freeNodex(tree *tr)
{
  nodeptr  p;
  int  i;  
  
  for (i = tr->mxtips + 1; (i <= 2*(tr->mxtips) - 2); i++) 
    {
      p = tr->nodep[i];
      while(!p->x)	
	p = p->next;
      allocCount--;
      free(p->x);
      p->x             = (void*)NULL;
      p->next->x       = (void*)NULL;
      p->next->next->x = (void*)NULL;
    }

  if(allocCount != 0)
    {
      printf("FATAL error\n");
      exit(-1);
    }

}

void initAdef(analdef *adef)
{
 
  adef->bootstrapBranchLengths = FALSE;
  adef->model = M_GTRCAT;  
  adef->max_rearrange = 21;  
  adef->stepwidth = 5;
  adef->initial = adef->bestTrav = 10;
  adef->initialSet = FALSE;
  adef->restart = FALSE;
  adef->mode = BIG_RAPID_MODE;
  adef->categories = 25; 
  adef->boot    =           0;
  adef->useWeightFile = FALSE;
  adef->checkpoints = 0;
  adef->startingTreeOnly = 0;
  adef->rapidParsimony = 0;
  adef->useMixedModel = 0;
  adef->multipleRuns = 1;
  adef->useMultipleModel = FALSE;
  adef->likelihoodEpsilon = 0.1;
  adef->constraint        = FALSE;
  adef->grouping =          FALSE; 
  adef->randomStartingTree = FALSE;
  adef->categorizeGamma = FALSE;
  adef->parsimonySeed = 0;
  adef->proteinMatrix = JTT;
  adef->protEmpiricalFreqs = 0;
  adef->protModels = (int *)NULL;
  adef->protFreqs  = (int *)NULL; 
  adef->printRates = FALSE;
  adef->outgroup = FALSE;
}




int modelExists(char *model, analdef *adef)
{
  int i;
  char *protModels[10] = {"DAYHOFF", "DCMUT", "JTT", "MTREV", "WAG", "RTREV", "CPREV", "VT", "BLOSUM62", "MTMAM"};
  char thisModel[1024];
  

  /*********** DNA **********************/
  if(strcmp(model, "GTRGAMMA\0") == 0)
    {
      adef->model = M_GTRGAMMA;
      return 1;
    }

  

  if(strcmp(model, "GTRCAT\0") == 0)
    {
      adef->model = M_GTRCAT;      
      return 1;
    }
  
  if(strcmp(model, "GTRMIX\0") == 0)
    {
      adef->model = M_GTRCAT;
      adef->useMixedModel = 1;
      return 1;
    }


    

  
  /*************** AA GTR ********************/
	
  if(strcmp(model, "PROTCATGTR\0") == 0)
    {
      adef->model = M_PROTCAT;
      adef->proteinMatrix = GTR;
      return 1;
    }
  if(strcmp(model, "PROTMIXGTR\0") == 0)
    {
      adef->model = M_PROTCAT;
      adef->proteinMatrix = GTR;
      adef->useMixedModel = 1;
      return 1;
    }
  if(strcmp(model, "PROTGAMMAGTR\0") == 0)
    {
      adef->model = M_PROTGAMMA;
      adef->proteinMatrix = GTR;
      return 1;
    }
  
  /****************** AA JTT ************************/
  
  for(i = 0; i < 10; i++)
    {
      strcpy(thisModel, "PROTCAT");
      strcat(thisModel, protModels[i]);

      if(strcmp(model, thisModel) == 0)
	{
	  adef->model = M_PROTCAT;
	  adef->proteinMatrix = i;
	  return 1;
	}

      strcpy(thisModel, "PROTCAT");
      strcat(thisModel, protModels[i]);
      strcat(thisModel, "F");

      if(strcmp(model, thisModel) == 0)
	{
	  adef->model = M_PROTCAT;
	  adef->proteinMatrix = i;
	  adef->protEmpiricalFreqs = 1;
	  return 1;
	}


      strcpy(thisModel, "PROTMIX");
      strcat(thisModel, protModels[i]);
      
  
      if(strcmp(model, thisModel) == 0)
	{
	  adef->model = M_PROTCAT;
	  adef->proteinMatrix = i;
	  adef->useMixedModel = 1;
	  return 1;
	}

      strcpy(thisModel, "PROTMIX");
      strcat(thisModel, protModels[i]);
      strcat(thisModel, "F");

      if(strcmp(model, thisModel) == 0)
	{
	  adef->model = M_PROTCAT;
	  adef->proteinMatrix = i;
	  adef->useMixedModel = 1;
	  adef->protEmpiricalFreqs = 1;
	  return 1;
	}
      
      strcpy(thisModel, "PROTGAMMA");
      strcat(thisModel, protModels[i]);

      if(strcmp(model, thisModel) == 0)
	{
	  adef->model = M_PROTGAMMA;
	  adef->proteinMatrix = i;
	  return 1;
	}	

      strcpy(thisModel, "PROTGAMMA");
      strcat(thisModel, protModels[i]);
      strcat(thisModel, "F");

      if(strcmp(model, thisModel) == 0)
	{
	  adef->model = M_PROTGAMMA;
	  adef->proteinMatrix = i;
	  adef->protEmpiricalFreqs = 1;
	  return 1;
	}	
    }

  /*********************************************************************************/

  
  
  return 0;
}



int mygetopt(int argc, char **argv, char *opts, int *optind, char **optarg)
{
  static int sp = 1;
  register int c;
  register char *cp;

  if(sp == 1)
    if(*optind >= argc || argv[*optind][0] != '-' || argv[*optind][1] == '\0')
      return -1;
    else 
      if(strcmp(argv[*optind], "--") == 0) 
	{
	  *optind =  *optind + 1;
	  return -1;
	}
  c = argv[*optind][sp];
  if(c == ':' || (cp=strchr(opts, c)) == 0) 
    {
      printf(": illegal option -- %c \n", c);
      if(argv[*optind][++sp] == '\0') 
	{
	  *optind =  *optind + 1;
	  sp = 1;
	}
      return('?');
    }
  if(*++cp == ':') 
    {
      if(argv[*optind][sp+1] != '\0')
	{
	  *optarg = &argv[*optind][sp+1];
	  *optind =  *optind + 1;
	}
      else 
	{
	  *optind =  *optind + 1;
	  if(*optind >= argc) 
	    {
	      printf(": option requires an argument -- %c\n", c);
	      sp = 1;
	      return('?');
	    } 
	  else
	    {
	      *optarg = argv[*optind];
	      *optind =  *optind + 1;
	    }
	}
      sp = 1;
    } 
  else 
    {
      if(argv[*optind][++sp] == '\0') 
	{
	  sp = 1;
	  *optind =  *optind + 1;
	}
      *optarg = 0;
    }
  return(c);
  }

static void checkOutgroups(tree *tr, analdef *adef)
{
  if(adef->outgroup)
    {
      boolean found;
      int i, j;
      
      for(j = 0; j < tr->numberOfOutgroups; j++)
	{
	  found = FALSE;
	  for(i = 1; (i <= tr->mxtips) && !found; i++)
	    {
	      if(strcmp(tr->nameList[i], tr->outgroups[j]) == 0)
		{
		  tr->outgroupNums[j] = i;
		  found = TRUE;
		}
	    }
	  if(!found)
	    {
	      printf("Error, the outgroup name \"%s\" you specified can not be found in the alignment, exiting ....\n", tr->outgroups[j]);
	      errorExit(-1);
	    }
	}
    }
  
}

static void parseOutgroups(char outgr[2048], tree *tr)
{
  int count = 1, i, k;
  char name[nmlngth];

  i = 0;
  while(outgr[i] != '\0')
    {
      if(outgr[i] == ',')
	count++;
      i++;
    }

  tr->numberOfOutgroups = count;

  tr->outgroups = (char **)malloc(sizeof(char *) * count);

  for(i = 0; i < tr->numberOfOutgroups; i++)   
    tr->outgroups[i] = (char *)malloc(sizeof(char) * nmlngth);    

  tr->outgroupNums = (int *)malloc(sizeof(int) * count);
    
  i = 0;
  k = 0;
  count = 0;
  while(outgr[i] != '\0')
    {
      if(outgr[i] == ',')
	{	
	  name[k] = '\0';
	  strcpy(tr->outgroups[count], name);
	  count++;
	  k = 0;	 
	}
      else
	{
	  name[k] = outgr[i];
	  k++;
	}
      i++;
    }

  name[k] = '\0';
  strcpy(tr->outgroups[count], name);

  /*for(i = 0; i < tr->numberOfOutgroups; i++)
    printf("%d %s \n", i, tr->outgroups[i]);*/


  /*printf("%s \n", name);*/
}


/*********************************** OUTGROUP STUFF END *********************************************************/


void get_args(int argc, char *argv[], boolean print_usage, analdef *adef, rawdata *rdta, tree *tr)
{
  int	optind = 1;
  int        c;
  boolean    bad_opt=FALSE;
  int        i, time, temperature;
  char       buf[2048];
  char       *optarg;
  char       model[2048] = "";
  char       outgroups[2048] = "";
  char       modelChar;
  double likelihoodEpsilon;
  int nameSet = 0, 
    alignmentSet = 0, 
    catSet = 0,
    checkpointSet = 0,
    startingTreeOnly = 0,
    multipleRuns = 0,
    parsimonySeed = 0,
    constraintSet = 0,
    treeSet = 0,
    groupSet = 0,
    modelSet = 0,
    treesSet  = 0;
    
  run_id[0] = 0;
  workdir[0] = 0;
  seq_file[0] = 0;
  tree_file[0] = 0;
  model[0] = 0;
  weightFileName[0] = 0;
  modelFileName[0] = 0;

  /*********** tr inits **************/


  tr->doCutoff = TRUE;

 
  /********* tr inits end*************/

  while(!bad_opt && ((c = mygetopt(argc,argv,"z:g:r:e:a:b:c:f:i:m:t:w:s:n:o:q:#:p:vdyjhk", &optind, &optarg))!=-1))
    {
    switch(c) 
      {     
      case 'o':
	strcpy(outgroups, optarg);
	parseOutgroups(outgroups, tr);
	adef->outgroup = TRUE;
	break;	
      case 'k':
	adef->bootstrapBranchLengths = TRUE;
	break;
      case 'z':	
	strcpy(bootStrapFile, optarg);
	treesSet = 1;
	break;               
      case 'd':
	adef->randomStartingTree = TRUE;
	break;
      case 'g':
	strcpy(tree_file, optarg);
	adef->grouping = TRUE;
	adef->restart  = TRUE;      
	groupSet = 1;
	break;
      case 'r':	
	strcpy(tree_file, optarg);
	adef->restart = TRUE;
	adef->constraint = TRUE;
	constraintSet = 1;
	break;
      case 'e':      
	sscanf(optarg,"%lf", &likelihoodEpsilon);
	adef->likelihoodEpsilon = likelihoodEpsilon;      
	break;
      case 'q':
	strcpy(modelFileName,optarg);
	adef->useMultipleModel = TRUE;      
        break;       
      case 'p':
	sscanf(optarg,"%d", &parsimonySeed);
	adef->parsimonySeed = parsimonySeed;
	break;
      case '#':
	sscanf(optarg,"%d", &multipleRuns);
	adef->multipleRuns = multipleRuns;
	break;
      case 'v':
	printVersionInfo();
	errorExit(0);
      case 'y':
	adef->startingTreeOnly = 1;
	break;
      case 'h':
	printREADME();
	errorExit(0);
      case 'j':	
	adef->checkpoints = 1;
	break;
      case 'a':
	strcpy(weightFileName,optarg);
	adef->useWeightFile = TRUE;
        break;            
      case 'b':
	sscanf(optarg,"%ld", &adef->boot);       
	break;
      case 'c':
	sscanf(optarg, "%d", &adef->categories);
	catSet = 1;
	break;	      	
      case 'f': 
	sscanf(optarg, "%c", &modelChar);
	switch(modelChar)
	  {
	  case 'b': 
	    adef->mode = CALC_BIPARTITIONS; 
	    break;
	  case 'e': 
	    adef->mode = TREE_EVALUATION; 
	    break;	  	 
	  case 'd': 
	    adef->mode = BIG_RAPID_MODE;
	    tr->doCutoff = TRUE;
	    break;	 
	  case 'o': 
	    adef->mode = BIG_RAPID_MODE;
	    tr->doCutoff = FALSE;
	    break;
	  case 's':
	    adef->mode = SPLIT_MULTI_GENE;
	    break;       
	  case 'c':
	    adef->mode = CHECK_ALIGNMENT;
	    break;	 
	  default: 
	    {
	      if(processID == 0)
		{
		  printf("Error select one of the following algorithms:\n");		 
		  printf("-f b : computation of bipartitions\n");
		  printf("-f c : check if alignment can be read by RAxML\n");
		  printf("-f d : new very fast hill-climbing for huge data\n");	
		  printf("-f e : optimization of model parameters and branch lengths\n");	      	     		      		 	      	
		  printf("-f o : older and slower hill-climbing algorithm\n");		 
		  printf("-f s : split a multi-gene alignment into individual genes\n");		  
		  
		}	     
	      errorExit(-1);
	    }
	  }
	break;      
      case 'i':
	sscanf(optarg, "%d", &adef->initial);
	adef->initialSet = TRUE;
	break;     
      case 'n':
        strcpy(run_id,optarg);
	nameSet = 1;
        break;
      case 'w':
        strcpy(workdir,optarg);
        break;                 
      case 't':
	strcpy(tree_file, optarg);
	adef->restart = TRUE;
	treeSet = 1;
	break;
      case 's':
	strcpy(seq_file, optarg);
	alignmentSet = 1;
	break;
      case 'm':
	strcpy(model,optarg);
	if(modelExists(model, adef) == 0)
	  {
	    if(processID == 0)
	      {
		printf("Model %s does not exist\n\n", model);
		printf("please use either GTRCAT or GTRGAMMA or GTRMIX for DNA DATA\n\n");
		printf("or PROTCATmatrixName[F] or PROTGAMMAmatrixName[F] or PROTMIXmatrixName[F] for AA DATA\n\n");
		printf("The AA substitution matrix can be one of the following: \n");
		printf("DAYHOFF, DCMUT, JTT, MTREV, WAG, RTREV, CPREV, VT, BLOSUM62, MTMAM, GTR\n\n");
		printf("With the optional \"F\" appendix you can specify if you want to use empirical base frequencies\n");
		printf("Please not that for mixed models you can in addition specify the per-gene model in\n");
		printf("the mixed model file (see manual for details)\n");	    
	      }
	    errorExit(-1);
	  }      
	else	  
	  modelSet = 1;
	break;      
      default:	   
	errorExit(-1);
        bad_opt = TRUE;
        break;
    }    
  }     


  if(adef->bootstrapBranchLengths && (adef->model == M_GTRCAT || adef->model == M_PROTCAT) && (!adef->useMixedModel))
    {
      if(processID == 0)
	{
	  printf("\nWARNING: you want to print out the branch lengths of your bootstrapped trees\n");	
	  printf("WARNING: However you have currently chosen one of the CAT models where the branch lengths\n");
	  printf("WARNING: are essentially meaningless, you should better use CATMIX/PROTMIX instead\n");
	}    
    }

  /*if(adef->outgroup && adef->mode == CALC_BIPARTITIONS)
    {
      if(processID == 0)
	printf("\n  Error, combination of bipartition option and outgroup option currently not allowed\n");
      errorExit(-1);
      }*/
    

  if(adef->mode == SPLIT_MULTI_GENE && (!adef->useMultipleModel))
    {
      if(processID == 0)
	{
	  printf("\n  Error, you are trying to split a multi-gene alignment into individual genes with the \"-f s\" option\n");	
	  printf("Without specifying a multiple model file with \"-q modelFileName\" \n");
	}
      errorExit(-1);
    }


  if(adef->mode == CALC_BIPARTITIONS && !treesSet)
    {
      if(processID == 0)
	printf("\n  Error, in bipartition computation mode you must specify a file containing multiple trees with the \"-z\" option\n");
      errorExit(-1);
    }

  if(adef->mode == CALC_BIPARTITIONS && !adef->restart)
    {
      if(processID == 0)
	printf("\n  Error, in bipartition computation mode you must specify a tree on which bipartition information will be drawn with the \"-t\" option\n");
      errorExit(-1);
    }

  if(!modelSet)
    { 
      if(processID == 0)
	printf("\n Error, you must specify a model of substitution with the \"-m\" option\n");
      errorExit(-1);
    }
      


  if(adef->useMultipleModel && (adef->model == M_PROTGAMMA || adef->model == M_PROTCAT) && (adef->proteinMatrix == GTR))
    {
      if(processID == 0)
	printf("\n Error GTR model of AA substiution in combination with mixed models is currently not implemented\n");
      errorExit(-1);
    }  

  if(adef->restart && adef->randomStartingTree)
    {
      if(processID == 0)
	{
	  if(adef->constraint)
	    {
	      printf("\n Error you specified a binary constraint tree with -r AND the computation\n");
	      printf("of a random starting tree with -d for the same run\n");
	    }
	  else
	    {
	      if(adef->grouping)
		{
		  printf("\n Error you specified a multifurcating constraint tree with -g AND the computation\n");
		  printf("of a random starting tree with -d for the same run\n");
		}
	      else
		{
		  printf("\n Error you specified a starting tree with -t AND the computation\n");
		  printf("of a random starting tree with -d for the same run\n");
		}
	    }
	}
      errorExit(-1);
    }

  if(treeSet && constraintSet)
    {
      if(processID == 0)
	printf("\n Error you specified a binary constraint tree AND a starting tree for the same run\n");
      errorExit(-1);
    }


  if(treeSet && groupSet)
    {
      if(processID == 0)
	printf("\n Error you specified a multifurcating constraint tree AND a starting tree for the same run\n");
      errorExit(-1);
    }


  if(groupSet && constraintSet)
    {
      if(processID == 0)
	printf("\n Error you specified a bifurcating constraint tree AND a multifurcating constraint tree for the same run\n");
      errorExit(-1);
    }

  if(adef->restart && adef->startingTreeOnly)
    {
      if(processID == 0)
	{
	  printf("\n Error conflicting options: you want to compute only a parsimony starting tree with -y\n");
	  printf(" while you actually specified a starting tree with -t %s\n", tree_file);
	}
      errorExit(-1);
    }
  
  if(adef->mode == TREE_EVALUATION && (!adef->restart))
    {
      if(processID == 0)
	printf("\n Error: please specify a treefile for the tree you want to evaluate with -t\n");
      errorExit(-1);
    }

#ifdef PARALLEL

  if(adef->mode == SPLIT_MULTI_GENE)
    {
      if(processID == 0)
	printf("Multi gene alignment splitting (-f s) not implemented for the MPI-Version\n");
      errorExit(-1);
    }

  if(adef->mode == TREE_EVALUATION)
    {
      if(processID == 0)
	printf("Tree Evaluation mode (-f e) noot implemented for the MPI-Version\n");
      errorExit(-1);
    }

  /*if(adef->mode == RELL_BOOTSTRAP)
    {
      if(processID == 0)
	printf("RELL Bootstrap mode (-f e) not implemented for the MPI-Version\n");
      errorExit(-1);
      }*/

   if(adef->mode == CALC_BIPARTITIONS)
     {
       if(processID == 0)
	 printf("Computation of bipartitions (-f b) not implemented for the MPI-Version\n");
       errorExit(-1);
     }
   
   if(adef->multipleRuns == 1)
     {
       if(processID == 0)
	 {
	   printf("Error: you are running the parallel MPI program but only want to compute one tree\n");
	   printf("For the MPI version you must specify a number of trees greater than 1 with the -# option\n");
	 }
       errorExit(-1);
     }
#endif

   
   
     if(adef->mode == TREE_EVALUATION && (adef->model == M_GTRCAT || adef->model == M_PROTCAT))
     {
       if(processID == 0)
	 {
	   printf("\n Error: No tree evaluation with GTRCAT/PROTCAT possible\n");
	   printf("the GTRCAT likelihood values are instable at present and should not\n");
	   printf("be used to compare trees based on ML values\n");
	 }
       errorExit(-1);
     }

  if(!nameSet)
    {
      if(processID == 0)
	printf("\n Error: please specify a name for this run with -n\n");
      errorExit(-1);
    }
    
  if(! alignmentSet)
    {
      if(processID == 0)
	printf("\n Error: please specify an alignment for this run with -s\n");
      errorExit(-1);
    }

  if(workdir[0]==0 || workdir[0] != '/') 
    {
      getcwd(buf,sizeof(buf));
      if( buf[strlen(buf)-1] != '/') strcat(buf,"/");
      strcat(buf,workdir);
      if( buf[strlen(buf)-1] != '/') strcat(buf,"/");
      strcpy(workdir,buf);
    }
 
  return;
}




void errorExit(int e)
{
#ifdef PARALLEL
  MPI_Status msgStatus; 
  int i, dummy;

  if(processID == 0)
    {      
      for(i = 1; i < numOfWorkers; i++)	
	MPI_Send(&dummy, 1, MPI_INT, i, FINALIZE, MPI_COMM_WORLD);
  
      MPI_Finalize();
      exit(e);
    }     
  else
    {	
      MPI_Recv(&dummy, 1, MPI_INT, 0, FINALIZE, MPI_COMM_WORLD, &msgStatus);     
      MPI_Finalize();
      exit(e);
    }
#else 
  exit(e);
#endif

}



void makeFileNames(tree *tr, analdef *adef, int argc, char *argv[])
{
  int infoFileExists = 0, i;
#ifdef PARALLEL
  MPI_Status msgStatus; 
#endif

  strcpy(permFileName,         workdir);    
  strcpy(resultFileName,       workdir);
  strcpy(logFileName,          workdir);
  strcpy(checkpointFileName,   workdir);
  strcpy(infoFileName,         workdir);
  strcpy(randomFileName,       workdir);
  strcpy(rellInfoFileName,     workdir);
  strcpy(rellBootFileName,     workdir);
  strcpy(bootstrapFileName,    workdir);
  strcpy(bipartitionsFileName, workdir);
  strcpy(ratesFileName,        workdir);

  strcat(permFileName,         "RAxML_parsimonyTree.");
  strcat(resultFileName,       "RAxML_result.");
  strcat(logFileName,          "RAxML_log.");
  strcat(checkpointFileName,   "RAxML_checkpoint.");
  strcat(infoFileName,         "RAxML_info.");
  strcat(randomFileName,       "RAxML_randomTree.");
  strcat(rellInfoFileName,     "RAxML_RELL_INFO.");
  strcat(rellBootFileName,     "RAxML_RELL.");
  strcat(bootstrapFileName,    "RAxML_bootstrap.");  
  strcat(bipartitionsFileName, "RAxML_bipartitions.");
  strcat(ratesFileName,        "RAxML_perSiteRates.");

  strcat(permFileName,         run_id);
  strcat(resultFileName,       run_id);
  strcat(logFileName,          run_id);
  strcat(checkpointFileName,   run_id);
  strcat(infoFileName,         run_id);    
  strcat(randomFileName,       run_id);  
  strcat(rellInfoFileName,     run_id);
  strcat(rellBootFileName,     run_id);
  strcat(bootstrapFileName,    run_id); 
  strcat(bipartitionsFileName, run_id);
  strcat(ratesFileName,        run_id);

  if(processID == 0)
    {
      infoFileExists = filexists(infoFileName);

#ifdef PARALLEL
      for(i = 1; i < numOfWorkers; i++)	
	MPI_Send(&infoFileExists, 1, MPI_INT, i, FINALIZE, MPI_COMM_WORLD);
#endif

      if(infoFileExists)
	{
	  printf("RAxML output files with the run ID <%s> already exist \n", run_id);
	  printf("in directory %s ...... exiting\n", workdir);
#ifdef PARALLEL 	  
	  MPI_Finalize();
	  exit(-1);
#else
	  exit(-1);
#endif
	}     
    }
#ifdef PARALLEL
  else
    {	
      MPI_Recv(&infoFileExists, 1, MPI_INT, 0, FINALIZE, MPI_COMM_WORLD, &msgStatus);
      if(infoFileExists)
	{	 	  
	  MPI_Finalize();
	  exit(-1);
	}    
    }
#endif
}


void readData(boolean reRead, analdef *adef, rawdata *rdta, cruncheddata *cdta, tree *tr)
{
  INFILE = fopen(seq_file, "r");
  
  if (!INFILE)
    {
      if(processID == 0)
	printf( "Could not open sequence file: %s\n", seq_file);
      errorExit(-1);
    }   
  getinput(reRead, adef, rdta, cdta, tr); 
  
  fclose(INFILE);   
}



/***********************reading and initializing input ******************/


/********************PRINTING various INFO **************************************/

void printVersionInfo()
{
  printf("\nThis is %s version %s released by Alexandros Stamatakis in %s\n\n",  programName, programVersion, programDate);
}


/* TODO: update */

void printREADME()
{
  printVersionInfo();  
  printf("\n");
  printf("Please also consult the RAxML-manual\n");
  printf("To report bugs send an email to Alexandros.Stamatakis@epfl.ch\n\n\n");
  
  printf("raxmlHPC[-MPI|-OMP] -s sequenceFileName -n outputFileName -m substitutionModel\n");
  printf("                    [-a weightFileName] [-b bootstrapRandomNumberSeed] [-c numberOfCategories]\n");
  printf("                    [-d] [-e likelihoodEpsilon] [-f b|c|d|e|o|s] [-g groupingFileName]\n");
  printf("                    [-h] [-i initialRearrangementSetting] [-j] [-k] [-o outGroupName1[,outGroupName2[,...]]]\n");
  printf("                    [-q multipleModelFileName] [-t userStartingTree] [-w workingDirectory] [-v] [-y]\n");
  printf("                    [-z multipleTreesFile] [-# numberOfRuns]\n");
  printf("\n");
  printf("       -a     Specify a column weight file name to assign individual weights to each column of \n");
  printf("              the alignment. Those weights must be integers separated by any type and number \n");
  printf("              of whitespaces whithin a separate file, see file \"example_weights\" for an example.\n");
  printf("\n");
  printf("       -b     Specify an integer number (random seed) and turn on bootstrapping\n");
  printf("\n");
  printf("              DEFAULT: OFF\n");
  printf("\n");
  printf("       -c     Specify number of distinct rate catgories for RAxML when modelOfEvolution\n");
  printf("              is set to GTRCAT or GTRMIX\n");
  printf("              Individual per-site rates are categorized into numberOfCategories rate \n");
  printf("              categories to accelerate computations. \n");
  printf("\n");
  printf("              DEFAULT: 25\n");
  printf("\n");
  printf("      -d      start ML optimization from random starting tree \n");
  printf("\n");
  printf("              DEFAULT: OFF\n");
  printf("\n");
  printf("      -e      set model optimization precision in log likelihood units for final\n"); 
  printf("              optimization of tree topology under GTRMIX or GTRGAMMA\n");
  printf("\n");
  printf("              DEFAULT: 0.1\n");
  printf("\n");
  printf("      -f      select algorithm:\n");
  printf("\n");
  printf("              \"-f b\": draw bipartition information on a tree provided with \"-t\" based on multiple trees\n");
  printf("                      (e.g. form a bootstrap) in a file specifed by \"-z\"\n");
  printf("              \"-f c\": check if the alignment can be properly read by RAxML\n");
  printf("              \"-f d\": new rapid hill-climbing \n");
  printf("              \"-f e\": optimize model+branch lengths for given input tree under GTRGAMMA only\n"); 
  printf("              \"-f o\": old and slower rapid hill-climbing \n");
  printf("              \"-f s\": split up a multi-gene partitioned alignment into the respective subalignments \n");
  printf("\n");
  printf("              DEFAULT: new rapid hill climbing\n");  
  printf("\n");
  printf("      -g      specify the file name of a multifurcating constraint tree\n");
  printf("              this tree does not need to be comprehensive, i.e. must not contain all taxa\n");
  printf("\n");
  printf("      -i      Initial rearrangement setting for the subsequent application of topological \n");
  printf("              changes phase\n");
  printf("\n");
  printf("              DEFAULT: determined by program\n");
  printf("\n");
  printf("      -j      Specifies if checkpoints will be written by the program. If checkpoints \n");
  printf("              (intermediate tree topologies) shall be written by the program specify \"-j\"\n");
  printf("\n");
  printf("              DEFAULT: OFF\n");
  printf("\n");
  printf("      -k      Specifies that bootstrapped trees should be printed with branch lengths.\n");
  printf("              The bootstraps will run a bit longer, because model parameters will be optimized\n");
  printf("              at the end of each run. Use with CATMIX/PROTMIX or GAMMA.\n");
  printf("\n");
  printf("              DEFAULT: OFF\n");
  printf("\n");
  printf("      -h      Display this help message.\n");  
  printf("\n");
  printf("      -m      Model of Nucleotide or Amino Acid Substitution: \n");  
  printf("\n");
  printf("              NUCLEOTIDES:\n");
  printf("                \"-m GTRCAT\":   GTR + Optimization of substitution rates + Optimization of site-specific\n");
  printf("                               evolutionary rates which are categorized into numberOfCategories distinct \n");
  printf("                               rate categories for greater computational efficiency\n");
  printf("                               if you do a multiple analysis with  \"-#\" but without bootstrapping the program\n");
  printf("                               will use GTRMIX instead\n");
  printf("                \"-m GTRGAMMA\": GTR + Optimization of substitution rates + GAMMA model of rate \n");
  printf("                               heterogeneity (alpha parameter will be estimated)\n");
  printf("                \"-m GTRMIX\":   Inference of the tree under GTRCAT\n");
  printf("                               and thereafter evaluation of the final tree topology under GTRGAMMA\n");
  printf("\n");
  printf("              AMINO ACIDS:\n");          
  printf("                \"-m PROTCATmatrixName[F]\":    specified AA matrix + Optimization of substitution rates + Optimization of site-specific\n");
  printf("                                              evolutionary rates which are categorized into numberOfCategories distinct \n");
  printf("                                              rate categories for greater computational efficiency\n");
  printf("                                              if you do a multiple analysis with  \"-#\" but without bootstrapping the program\n");
  printf("                                              will use PROTMIX... instead\n");
  printf("                \"-m PROTGAMMAmatrixName[F]\":  specified AA matrix + Optimization of substitution rates + GAMMA model of rate \n");
  printf("                                              heterogeneity (alpha parameter will be estimated)\n");
  printf("                \"-m PROTMIXmatrixName[F]\":    Inference of the tree under specified AA matrix + CAT\n");
  printf("                                              and thereafter evaluation of the final tree topology under specified AA matrix + GAMMA\n");
  printf("\n");
  printf("                Available AA substitution models: DAYHOFF, DCMUT, JTT, MTREV, WAG, RTREV, CPREV, VT, BLOSUM62, MTMAM, GTR\n");
  printf("                With the optional \"F\" appendix you can specify if you want to use empirical base frequencies\n");
  printf("                Please not that for mixed models you can in addition specify the per-gene AA model in\n");
  printf("                the mixed model file (see manual for details)\n");
  printf("\n");
  printf("      -n      Specifies the name of the output file.\n"); 
  printf("\n"); 
  printf("      -o      Specify the name of a single outgrpoup or a comma-separated list of outgroups, eg \"-o Rat\" \n");
  printf("              or \"-o Rat,Mouse\", in case that multiple outgroups are not monophyletic the first name \n");
  printf("              in the list will be selected as outgroup, don't leave spaces between taxon names!\n");
  printf("\n");
  printf("      -q      Specify the file name which contains the assignment of models to alignment\n"); 
  printf("              partitions for multiple models of substitution. For the syntax of this file\n");
  printf("              please consult the manual.\n"); 
  printf("\n"); 
  printf("      -r      Specify the file name of a binary constraint tree.\n");
  printf("              this tree does not need to be comprehensive, i.e. must not contain all taxa\n");
  printf("\n");  
  printf("      -s      Specify the name of the alignment data file in PHYLIP format\n");
  printf("\n");
  printf("      -t      Specify a user starting tree file name in Newick format\n");
  printf("\n");
  printf("      -v      Display version information\n");  
  printf("\n");
  printf("      -w      Name of the working directory where RAxML will write its output files\n");
  printf("\n");
  printf("              DEFAULT: current directory\n"); 
  printf("\n");
  printf("      -y      If you want to only compute a parsimony starting tree with RAxML specify \"-y\",\n");
  printf("              the program will exit after computation of the starting tree\n");
  printf("\n");
  printf("              DEFAULT: OFF\n");
  printf("\n");  
  printf("      -z      Specify the file name of a file containing multiple trees e.g. from a bootstrap\n");
  printf("              that shall be used to draw bipartition values onto a tree provided with \"-t\",\n");
  printf("              This switch only has effect if used with \"-f b\"\n");
  printf("\n");
  printf("      -#      Specify the number of alternative runs on distinct starting trees\n");
  printf("              In combination with the \"-b\" option, this will invoke a multiple boostrap analysis\n");
  printf("\n");
  printf("              DEFAULT: 1 single analysis\n");
  printf("\n\n\n\n");

}

void printModelAndProgramInfo(tree *tr, analdef *adef, int argc, char *argv[])
{ 
  if(processID == 0)
    {
      char dataType[16];
      int i;     
      FILE *infoFile = fopen(infoFileName, "a");  

      if(adef->model == M_PROTCAT || adef->model == M_PROTGAMMA)
	strcpy(dataType, "AA");
      else
	strcpy(dataType, "DNA");
      
      switch(adef->mode)
	{       
	case TREE_EVALUATION : 
	  printf("\nRAxML Model Optimization up to an accuracy of %f log likelihood units for %s data\n\n", adef->likelihoodEpsilon, dataType);
	  fprintf(infoFile, "\nRAxML Model Optimization up to an accuracy of %f log likelihood units for %s data\n\n", adef->likelihoodEpsilon, dataType);
	  break;     
	case  BIG_RAPID_MODE:
	  printf("\nRAxML rapid hill-climbing mode for %s data\n\n", dataType); 
	  fprintf(infoFile, "\nRAxML   rapid hill-climbing mode for %s data\n\n", dataType);
	  break;
	  /*case RELL_BOOTSTRAP:
	  printf("TODO RELL-BOOTSTRAP\n");
	  printf("RELL parameters: Rells per Tree %d, sd cutoff %f lh cutoff %f Thourough %s MaxTrav %d\n", RELLs_PER_TREE,  adef->sdThreshold,  
		 adef->lhThreshold, (adef->THOROUGH)?"ON":"OFF", adef->initial);
	  fprintf(infoFile, "RELL parameters: Rells per Tree %d, sd cutoff %f lh cutoff %f Thourough %s MaxTrav %d\n", RELLs_PER_TREE,  adef->sdThreshold,  
		  adef->lhThreshold, (adef->THOROUGH)?"ON":"OFF", adef->initial);
		  break;*/
	case CALC_BIPARTITIONS:
	  printf("\nRAxML Bipartition Computation: Drawing support values from trees in file %s onto tree in file %s\n\n", bootStrapFile, tree_file);
	  fprintf(infoFile, "\nRAxML Bipartition Computation: Drawing support values from trees in file %s onto tree in file %s\n\n", bootStrapFile, tree_file);
	  fclose(infoFile);
	  return;
	  break;
	}
      
      if(tr->NumberOfModels > 1)
	{
	  int model;
	  
	  printf("Multiple Model Mode: Using %d distinct models\n", tr->NumberOfModels);
	  fprintf(infoFile, "Multiple Model Mode: Using %d distinct models\n", tr->NumberOfModels);	  
	  printf(           "\n\n");
	  fprintf(infoFile, "\n\n");
	}
      
      if(adef->boot)
	{
	  if(adef->multipleRuns > 1)
	    {
	      printf("Executing %d non-parametric bootstrap inferences\n\n", adef->multipleRuns);
	      fprintf(infoFile, "Executing %d non-parametric bootstrap inferences\n\n", adef->multipleRuns);
	    }
	  else
	    {
	      printf("Executing %d non-parametric bootstrap inference\n\n", adef->multipleRuns);
	      fprintf(infoFile, "Executing %d non-parametric bootstrap inference\n\n", adef->multipleRuns);
	    }
	}
      else
	{
	  char treeType[1024];

	  if(adef->restart)
	    strcpy(treeType, "user-specifed");
	  else
	    {
	      if(adef->randomStartingTree)		
		strcpy(treeType, "distinct complete random");
	      else
		strcpy(treeType, "distinct randomized MP");
	    }


	  if(adef->multipleRuns > 1)
	    {
	      printf("Executing %d inferences on the original alignment using %d %s trees\n\n", adef->multipleRuns, adef->multipleRuns, treeType);
	      fprintf(infoFile, "Executing %d inferences on the original alignment using %d %s trees\n\n", adef->multipleRuns, adef->multipleRuns, treeType);
	    }
	  else
	    {
	      printf("Executing %d inference on the original alignment using a %s tree\n\n", adef->multipleRuns, treeType);
	      fprintf(infoFile, "Executing %d inference on the original alignment using a %s tree\n\n", adef->multipleRuns, treeType);
	    }
	}
      
      
      switch(adef->model)
	{    
	case M_PROTCAT:
	  if(adef->useMultipleModel)
	    {
	      if(adef->useMixedModel)
		{ 
		  printf( "Mixed model of amino acid substitution\n"); 
		  printf( "All free model parameters will be estimated by RAxML\n");
		  printf( "ML estimate of %d per site rate categories\n", adef->categories);      
		  printf( "Likelihood of final tree will be evaluated and optimized under GAMMA\n");
		  printf( "GAMMA Model parameters will be estimated up to an accuracy of %2.10f Log Likelihood units\n\n", adef->likelihoodEpsilon);
		  fprintf(infoFile, "Mixed model of amino acid substitution\n"); 
		  fprintf(infoFile, "All free model parameters will be estimated by RAxML\n");
		  fprintf(infoFile, "ML estimate of %d per site rate categories\n", adef->categories);
		  fprintf(infoFile, "Likelihood of final tree will be evaluated and optimized under GAMMA\n");
		  fprintf(infoFile, "GAMMA Model parameters will be estimated up to an accuracy of %2.10f Log Likelihood units\n\n", adef->likelihoodEpsilon);
		}	   
	      else
		{
		  printf( "Aproximation of rate heterogeneity with per-site rate categories\n"); 
		  printf( "All free model parameters will be estimated by RAxML\n");
		  printf( "ML estimate of %d per site rate categories\n", adef->categories);   
		  printf( "WARNING: CAT likelihood values are numerically instable!\n\n");		 
		  fprintf(infoFile, "Aproximation of rate heterogeneity with per-site rate categories\n"); 
		  fprintf(infoFile, "All free model parameters will be estimated by RAxML\n");
		  fprintf(infoFile, "ML estimate of %d per site rate categories\n", adef->categories);   
		  fprintf(infoFile, "WARNING: CAT likelihood values are numerically instable!\n\n");
		}
	    }
	  else
	    {
	      char matrix[1024];
	      char *protModels[11] = {"DAYHOFF", "DCMUT", "JTT", "MTREV", "WAG", "RTREV", "CPREV", "VT", "BLOSUM62", "MTMAM", "GTR"};
	      strcpy(matrix, protModels[adef->proteinMatrix]);

	      if(adef->useMixedModel)
		{ 
		  printf( "%s mixed model of amino acid substitution\n", matrix); 
		  printf( "All free model parameters will be estimated by RAxML\n");
		  printf( "ML estimate of %d per site rate categories\n", adef->categories);      
		  printf( "Likelihood of final tree will be evaluated and optimized under %s+GAMMA\n", matrix);
		  printf( "%s+GAMMA Model parameters will be estimated up to an accuracy of %2.10f Log Likelihood units\n\n", matrix, adef->likelihoodEpsilon);
		  fprintf(infoFile, "%s mixed model of amino acid substitution\n", matrix); 
		  fprintf(infoFile, "All free model parameters will be estimated by RAxML\n");
		  fprintf(infoFile, "ML estimate of %d per site rate categories\n", adef->categories);
		  fprintf(infoFile, "Likelihood of final tree will be evaluated and optimized under %s+GAMMA\n", matrix);
		  fprintf(infoFile, "%s+GAMMA Model parameters will be estimated up to an accuracy of %2.10f Log Likelihood units\n\n", matrix, adef->likelihoodEpsilon);
		}
	      else
		{
		  printf( "%s approximation of amino acid substitution\n", matrix); 
		  printf( "All free model parameters will be estimated by RAxML\n");
		  printf( "ML estimate of %d per site rate categories\n", adef->categories);   
		  printf( "WARNING: %s+CAT likelihood values are numerically instable!\n\n", matrix);	       
		  fprintf(infoFile, "GTR approximation of amino acid substitution\n"); 
		  fprintf(infoFile, "All free model parameters will be estimated by RAxML\n");
		  fprintf(infoFile, "ML estimate of %d per site rate categories\n", adef->categories);
		  fprintf(infoFile, "WARNING: %s+CAT likelihood values are numerically instable!\n\n", matrix);		
		}	      
	    }
	  break;
	case M_PROTGAMMA:	  
	  if(adef->useMultipleModel)
	    {       
	      printf( "All free model parameters will be estimated by RAxML\n");  
	      printf( "GAMMA model of rate heteorgeneity, ML estimate of alpha-parameter\n");
	      printf( "GAMMA Model parameters will be estimated up to an accuracy of %2.10f Log Likelihood units\n\n", adef->likelihoodEpsilon);	
	      fprintf(infoFile, "All free model parameters will be estimated by RAxML\n");  
	      fprintf(infoFile, "GAMMA model of rate heterogeneity, ML estimate of alpha-parameter\n");
	      fprintf(infoFile, "GAMMA Model parameters will be estimated up to an accuracy of %2.10f Log Likelihood units\n\n", adef->likelihoodEpsilon);		
	    }
	  else
	    {
	      char matrix[1024];
	      char *protModels[11] = {"DAYHOFF", "DCMUT", "JTT", "MTREV", "WAG", "RTREV", "CPREV", "VT", "BLOSUM62", "MTMAM", "GTR"};
	      strcpy(matrix, protModels[adef->proteinMatrix]);
	      
	      printf( "%s model of amino acid substitution\n", matrix);  
	      printf( "All free model parameters will be estimated by RAxML\n");  
	      printf( "GAMMA model of rate heteorgeneity, ML estimate of alpha-parameter\n");
	      printf( "%s+GAMMA Model parameters will be estimated up to an accuracy of %2.10f Log Likelihood units\n\n", matrix, adef->likelihoodEpsilon);
	      fprintf(infoFile, "%s model of amino acid substitution\n", matrix);  
	      fprintf(infoFile, "All free model parameters will be estimated by RAxML\n");  
	      fprintf(infoFile, "GAMMA model of rate heterogeneity, ML estimate of alpha-parameter\n");
	      fprintf(infoFile, "%s+GAMMA Model parameters will be estimated up to an accuracy of %2.10f Log Likelihood units\n\n", matrix, adef->likelihoodEpsilon);
	    }
	  break;
	case M_GTRCAT:
	  if(adef->useMixedModel)
	    { 
	      printf( "GTR mixed model of nucleotide substitution\n"); 
	      printf( "All free model parameters will be estimated by RAxML\n");
	      printf( "ML estimate of %d per site rate categories\n", adef->categories);      
	      printf( "Likelihood of final tree will be evaluated and optimized under GTR+GAMMA\n");
	      printf( "GTR+GAMMA Model parameters will be estimated up to an accuracy of %2.10f Log Likelihood units\n", adef->likelihoodEpsilon);
	      fprintf(infoFile, "GTR mixed model of nucleotide substitution\n"); 
	      fprintf(infoFile, "All free model parameters will be estimated by RAxML\n");
	      fprintf(infoFile, "ML estimate of %d per site rate categories\n", adef->categories);
	      fprintf(infoFile, "Likelihood of final tree will be evaluated and optimized under GTR+GAMMA\n");
	      fprintf(infoFile, "GTR+GAMMA Model parameters will be estimated up to an accuracy of %2.10f Log Likelihood units\n", adef->likelihoodEpsilon);
	    }
	  else
	    {
	      printf( "GTR approximation of nucleotide substitution\n"); 
	      printf( "All free model parameters will be estimated by RAxML\n");
	      printf( "ML estimate of %d per site rate categories\n", adef->categories);   
	      printf( "WARNING: GTR+CAT likelihood values are numerically instable!\n");
	      printf( "WARNING: For ML-based comparisons of final trees use -m GTRMIX\n");
	      fprintf(infoFile, "GTR approximation of nucleotide substitution\n"); 
	      fprintf(infoFile, "All free model parameters will be estimated by RAxML\n");
	      fprintf(infoFile, "ML estimate of %d per site rate categories\n", adef->categories);
	      fprintf(infoFile, "WARNING: GTR+CAT likelihood values are numerically instable!\n");
	      fprintf(infoFile, "WARNING: For ML-based comparisons of final trees use -m GTRMIX\n");
	    }
	  break;   
	case M_GTRGAMMA:	  	    
	    printf( "GTR model of nucleotide substitution\n");  
	    printf( "All free model parameters will be estimated by RAxML\n");  
	    printf( "GAMMA model of rate heteorgeneity, ML estimate of alpha-parameter, %d discrete rate categories\n", tr->gammaCategories);
	    printf( "GTR+GAMMA Model parameters will be estimated up to an accuracy of %2.10f Log Likelihood units\n", adef->likelihoodEpsilon);
	    fprintf(infoFile, "GTR model of nucleotide substitution\n");  
	    fprintf(infoFile, "All free model parameters will be estimated by RAxML\n");  
	    fprintf(infoFile, "GAMMA model of rate heterogeneity, ML estimate of alpha-parameter, %d discrete rate categories\n", tr->gammaCategories);
	    fprintf(infoFile, "GTR+GAMMA Model parameters will be estimated up to an accuracy of %2.10f Log Likelihood units\n", adef->likelihoodEpsilon);
	    break;
	}
      
      if(adef->boot == 0)
	{
	  char basesDNA[4] = {'A', 'C', 'G', 'T'};
	  char basesPROT[20] = {'A', 'R', 'N' , 'D', 'C', 'Q','E','G','H','I','L','K','M','F','P','S','T','W','Y','V'};
	  char *ptr;
	  int n, i;

	  if(adef->model == M_PROTCAT || adef->model == M_PROTGAMMA)
	    {
	      n = 20;
	      ptr = basesPROT;

	      if(!adef->useMultipleModel)		
		{		  		 
		  printf("%s Base Frequencies:\n", (adef->protEmpiricalFreqs)?"Empirical":"Fixed");
		  fprintf(infoFile, "%s Base Frequencies:\n", (adef->protEmpiricalFreqs)?"Empirical":"Fixed");
		}
	    }
	  else
	    {
	      ptr =  basesDNA;
	      n = 4;
	      printf("Empirical Base Frequencies:\n");
	      fprintf(infoFile, "Empirical Base Frequencies:\n");
	    }
	 
	  
	  if(tr->NumberOfModels == 1)
	    {	      
	      for(i = 0; i < n; i++)
		{
		  if(i % 4 == 0 && (adef->model == M_PROTCAT || adef->model == M_PROTGAMMA))
		    {
		      printf("\n");
		      fprintf(infoFile, "\n");
		    }	

		  printf("pi(%c): %f ",  ptr[i], tr->frequencies[i]);
		  fprintf(infoFile, "pi(%c): %f ",   ptr[i], tr->frequencies[i]);
		}
	      printf("\n");
	      fprintf(infoFile, "\n");	     
	    }
	  else
	    {
	      int model;
	      
	      if(adef->model == M_PROTCAT || adef->model == M_PROTGAMMA)
		{
		  char *protModels[10] = {"DAYHOFF", "DCMUT", "JTT", "MTREV", "WAG", "RTREV", "CPREV", "VT", "BLOSUM62", "MTMAM"};

		  for(model = 0; model < tr->NumberOfModels; model++)
		    {
		      printf("Partition %d assigned AA substitution matrix %s\n", model, protModels[adef->protModels[model]]);
		      fprintf(infoFile, "Partition %d assigned AA substitution matrix %s\n", model, protModels[adef->protModels[model]]);
		    
		      
		      printf("%s base frequencies:  ", (adef->protFreqs[model] == 1)?"Empirical":"Fixed");
		      fprintf(infoFile, "%s base frequencies: ", (adef->protFreqs[model] == 1)?"Empirical":"Fixed");
		      
		      for(i = 0; i < n; i++)
			{
			  if(i % 4 == 0)
			    {
			      printf("\n");
			      fprintf(infoFile, "\n");
			    }	

			  printf("pi(%c): %f ",  ptr[i], tr->frequencies[model * n + i]);
			  fprintf(infoFile, "pi(%c): %f ",   ptr[i], tr->frequencies[model *n + i]);			  			
			}
		      printf("\n\n");
		      fprintf(infoFile, "\n");
		    }
		}
	      else
		{
		  for(model = 0; model < tr->NumberOfModels; model++)
		    {
		      printf("Partition %d: ",   model);
		      fprintf(infoFile, "Partition %d: ", model);
		      
		      for(i = 0; i < n; i++)
			{
			  printf("pi(%c): %f ",  ptr[i], tr->frequencies[model * n + i]);
			  fprintf(infoFile, "pi(%c): %f ",   ptr[i], tr->frequencies[model *n + i]);
			}
		      printf("\n");
		      fprintf(infoFile, "\n");
		    }
		}
	    }
	}
      
      printf("\n\n");
      fprintf(infoFile, "\n\n");
      
      fprintf(infoFile,"RAxML was called as follows:\n\n");
      for(i = 0; i < argc; i++)
	fprintf(infoFile,"%s ", argv[i]);
      fprintf(infoFile,"\n\n\n");  
      
      fclose(infoFile);
    }
}

void printResult(tree *tr, analdef *adef, boolean finalPrint)
{
  FILE *logFile;
  char temporaryFileName[1024] = "", treeID[64] = "";

  strcpy(temporaryFileName, resultFileName);

  switch(adef->mode)
    {         
    case TREE_EVALUATION:     
      Tree2String(tr->tree_string, tr, tr->start->back, TRUE, TRUE, FALSE, FALSE, finalPrint, adef);
      logFile = fopen(temporaryFileName, "w");
      fprintf(logFile, "%s", tr->tree_string);
      fclose(logFile);    
      break;
      /*case RELL_BOOTSTRAP:
	break;*/
    case BIG_RAPID_MODE:
      if(!adef->boot)
	{
	  if(adef->multipleRuns > 1)
	    {	  	 	  
	      sprintf(treeID, "%d", tr->treeID);	  	  
	      strcat(temporaryFileName, ".RUN.");
	      strcat(temporaryFileName, treeID);	  	 	      	
	    }
	       	      	     
	  Tree2String(tr->tree_string, tr, tr->start->back, TRUE, TRUE, FALSE, FALSE, finalPrint, adef);

	  logFile = fopen(temporaryFileName, "w");
	  fprintf(logFile, "%s", tr->tree_string);
	  fclose(logFile);      
	}
      break;       
    default:
      printf("FATAL ERROR call to printResult from undefined STATE %d\n", adef->mode);
      exit(-1);
      break;
    }
}

void printBootstrapResult(tree *tr, analdef *adef, boolean finalPrint)
{
  if(processID == 0)
    {
      FILE *logFile;
      
      if(adef->mode == BIG_RAPID_MODE && adef->boot)
	{
#ifndef PARALLEL
	  if(adef->bootstrapBranchLengths)
	    Tree2String(tr->tree_string, tr, tr->start->back, TRUE, TRUE, FALSE, FALSE, finalPrint, adef);
	  else
	    Tree2String(tr->tree_string, tr, tr->start->back, FALSE, TRUE, FALSE, FALSE, finalPrint, adef);
#endif
	  logFile = fopen(bootstrapFileName, "a");
	  fprintf(logFile, "%s", tr->tree_string);
	  fclose(logFile);     
	}
      else
	{
	  printf("FATAL ERROR\n");
	  exit(-1);	 
	}
    }
}

/*void printRELLResult(tree *tr, analdef *adef)
{
  if(processID == 0)
    {
      FILE *logFile;
      
      if(adef->mode == RELL_BOOTSTRAP)
	{
	  Tree2String(tr->tree_string, tr, tr->start->back, TRUE, TRUE, FALSE, TRUE, adef);
	  logFile = fopen(rellBootFileName, "a");
	  fprintf(logFile, "%s", tr->tree_string);
	  fclose(logFile);   
	}
      else
	{
	  printf("FATAL ERROR\n");
	  exit(-1);	 
	}
    }
    }*/



void printBipartitionResult(tree *tr, analdef *adef, boolean finalPrint)
{
  if(processID == 0)
    {
      FILE *logFile;
      
      if(adef->mode == CALC_BIPARTITIONS)
	{	
	  Tree2String(tr->tree_string, tr, tr->start->back, FALSE, TRUE, FALSE, TRUE, finalPrint, adef);
	  logFile = fopen(bipartitionsFileName, "a");
	  fprintf(logFile, "%s", tr->tree_string);
	  fclose(logFile);   
	}
      else
	{
	  printf("FATAL ERROR\n");
	  exit(-1);	 
	}
    }
}



void printLog(tree *tr, analdef *adef, boolean finalPrint)
{
  FILE *logFile;
  char temporaryFileName[1024] = "", checkPoints[1024] = "", treeID[64] = "";
  double lh, t;
  
  lh = tr->likelihood;
  t = gettime() - masterTime;

  strcpy(temporaryFileName, logFileName);
  strcpy(checkPoints,       checkpointFileName);

  switch(adef->mode)
    {    
    case TREE_EVALUATION:	 
      logFile = fopen(temporaryFileName, "a");

      printf("%f %f\n", t, lh);
      fprintf(logFile, "%f %f\n", t, lh);

      fclose(logFile);     
      break;
      /*case RELL_BOOTSTRAP:
       NOTHING PRINTED SO FAR 
       break;*/
    case BIG_RAPID_MODE:
      if(adef->boot)
	{
	  /* testing only printf("%f %f\n", t, lh);*/
	  /* NOTHING PRINTED so far */
	}
      else
	{
	  if(adef->multipleRuns > 1)
	    {	  	 	  
	      sprintf(treeID, "%d", tr->treeID);	  	  
	      strcat(temporaryFileName, ".RUN.");
	      strcat(temporaryFileName, treeID);	  	 
	      
	      strcat(checkPoints, ".RUN.");
	      strcat(checkPoints, treeID);	      	      
	    }


	  if(!adef->checkpoints)
	    {
	      logFile = fopen(temporaryFileName, "a");
#ifndef PARALLEL	      
	      printf("%f %f\n", t, lh);
#endif
	      fprintf(logFile, "%f %f\n", t, lh);
	      
	      fclose(logFile);
	    }
	  else
	    {
	      logFile = fopen(temporaryFileName, "a");
#ifndef PARALLEL	      
	      printf("%f %f %d\n", t, lh, tr->checkPointCounter);
#endif
	      fprintf(logFile, "%f %f %d\n", t, lh, tr->checkPointCounter);
	      
	      fclose(logFile);
	      
	      strcat(checkPoints, ".");

	      sprintf(treeID, "%d", tr->checkPointCounter);
	      strcat(checkPoints, treeID);
	      Tree2String(tr->tree_string, tr, tr->start->back, TRUE, TRUE, FALSE, FALSE, finalPrint, adef);

	      logFile = fopen(checkPoints, "a");
	      fprintf(logFile, "%s", tr->tree_string);
	      fclose(logFile);

	      tr->checkPointCounter++;
	    }
	}
      break;       
    default:
      printf("FATAL ERROR call to printLog from undefined STATE %d\n", adef->mode);
      exit(-1);
      break;
    }
}



void printStartingTree(tree *tr, analdef *adef, boolean finalPrint)
{  
  if(adef->boot)
    {          
      /* not printing starting trees for bootstrap */
    }
  else
    {
      FILE *treeFile;
      char temporaryFileName[1024] = "", treeID[64] = "";
   
      Tree2String(tr->tree_string, tr, tr->start->back, FALSE, TRUE, FALSE, FALSE, finalPrint, adef);
          
      if(adef->randomStartingTree)	    
	strcpy(temporaryFileName, randomFileName);	    
      else
	strcpy(temporaryFileName, permFileName);

      if(adef->multipleRuns > 1)
	{	  	 	  
	  sprintf(treeID, "%d", tr->treeID);	  	  
	  strcat(temporaryFileName, ".RUN.");
	  strcat(temporaryFileName, treeID);	  	 
	}
     	  	 
      treeFile = fopen(temporaryFileName, "a");	
      fprintf(treeFile, "%s", tr->tree_string);
      fclose(treeFile);	    	
    }
}

void writeInfoFile(analdef *adef, tree *tr, double t)
{  
  if(processID == 0)
    {
      FILE *infoFile = fopen(infoFileName, "a");
      
      switch(adef->mode)
	{
	case TREE_EVALUATION:	 	  
	  break;
	  /*case  RELL_BOOTSTRAP:	  
	    break;*/
	case BIG_RAPID_MODE:
	  if(adef->boot)
	    {
	      if(!adef->initialSet)	       
		{
		  fprintf(infoFile, "Bootstrap[%d]: Time %f bootstrap likelihood %f, best rearrangement setting %d\n", tr->treeID, t, tr->likelihood,  adef->bestTrav);		
		  printf("Bootstrap[%d]: Time %f bootstrap likelihood %f, best rearrangement setting %d\n", tr->treeID, t, tr->likelihood,  adef->bestTrav);
		}
	      else		
		{
		  fprintf(infoFile, "Bootstrap[%d]: Time %f bootstrap likelihood %f\n", tr->treeID, t, tr->likelihood);	
		  printf("Bootstrap[%d]: Time %f bootstrap likelihood %f\n", tr->treeID, t, tr->likelihood);	
		}
	    }
	  else
	    {
	      if((adef->model == M_GTRCAT || adef->model == M_PROTCAT) && !adef->useMixedModel)
		{		  
		  if(adef->model == M_GTRCAT)
		    {
		      if(!adef->initialSet)		   
			fprintf(infoFile, "Inference[%d]: Time %f GTRCAT-likelihood %f, best rearrangement setting %d\n", tr->treeID, t, tr->likelihood,  adef->bestTrav);
		      else		  
			fprintf(infoFile, "Inference[%d]: Time %f GTRCAT-likelihood %f\n", tr->treeID, t, tr->likelihood);		    
		    }
		  else
		    {
		        if(!adef->initialSet)		   
			fprintf(infoFile, "Inference[%d]: Time %f CAT-likelihood %f, best rearrangement setting %d\n", tr->treeID, t, tr->likelihood,  adef->bestTrav);
		      else		  
			fprintf(infoFile, "Inference[%d]: Time %f CAT-likelihood %f\n", tr->treeID, t, tr->likelihood);
		    }
		}
	      else
		{	
		  int model;

		  if(adef->model == M_GTRGAMMA)
		    {
		      if(!adef->initialSet)		    		     	  
			fprintf(infoFile, "Inference[%d]: Time %f GTRGAMMA-likelihood %f, best rearrangement setting %d, alpha ", 
				tr->treeID, t, tr->likelihood,  adef->bestTrav);
		      else		  
			fprintf(infoFile, "Inference[%d]: Time %f GTRGAMMA-likelihood %f, alpha ", tr->treeID, t, tr->likelihood);		    		  
		    }
		  else
		    {
		      if(!adef->initialSet)		    		     	  
			fprintf(infoFile, "Inference[%d]: Time %f GAMMA-likelihood %f, best rearrangement setting %d, alpha ", 
				tr->treeID, t, tr->likelihood,  adef->bestTrav);
		      else		  
			fprintf(infoFile, "Inference[%d]: Time %f GAMMA-likelihood %f, alpha ", tr->treeID, t, tr->likelihood);
		    }

		  for(model = 0; model < tr->NumberOfModels; model++)		    		    
		    fprintf(infoFile, "%f ", tr->alphas[model]);
		    		  
		  fprintf(infoFile, "\n");
		}
	    }
	  break;
	}

      fclose(infoFile);
    }
}

void finalizeInfoFile(tree *tr, analdef *adef)
{
  if(processID == 0)
    {
      FILE *infoFile;
      double t;

      t = gettime() - masterTime;

      switch(adef->mode)
	{       
	case TREE_EVALUATION :
	  printf("\n\nOverall Time for Tree Evaluation %f\n", t);
	  if(adef->model == M_PROTGAMMA)
	    printf("Final GAMMA  likelihood: %f\n", tr->likelihood);
	  else
	    printf("Final GTR+GAMMA  likelihood: %f\n", tr->likelihood);

	  if(tr->NumberOfModels == 1)
	    printf("Final alpha shape parameter: %f\n", tr->alphas[0]);
	  else
	    {
	      int model;
	      for(model = 0; model < tr->NumberOfModels; model++)
		printf("Partition %d: final alpha shape parameter: %f\n", model, tr->alphas[model]);	      
	    }
	  
	  infoFile = fopen(infoFileName, "a");
	  
	  fprintf(infoFile, "\n\nOverall Time for Tree Evaluation %f\n", t);
	  if(adef->model == M_PROTGAMMA)
	    fprintf(infoFile, "Final GAMMA  likelihood: %f\n", tr->likelihood);
	  else
	    fprintf(infoFile, "Final GTR+GAMMA  likelihood: %f\n", tr->likelihood);
	  if(tr->NumberOfModels == 1)
	    fprintf(infoFile, "Final alpha shape parameter: %f\n", tr->alphas[0]);		
	  else
	    {
	      int model;
	      for(model = 0; model < tr->NumberOfModels; model++)
		fprintf(infoFile, "Partition %d: final alpha shape parameter: %f\n", model, tr->alphas[model]);
	    
	    }	
	  	
	  printf("Final tree written to:                 %s\n", resultFileName);  
	  printf("Execution Log File written to:         %s\n", logFileName);
	   
	  fprintf(infoFile, "Final tree written to:                 %s\n", resultFileName);  
	  fprintf(infoFile, "Execution Log File written to:         %s\n", logFileName);	  
	  
	  fclose(infoFile);
	  break;  
	case  BIG_RAPID_MODE:
	  if(adef->boot)
	    {
	      printf("\n\nOverall Time for %d Bootstraps %f\n", adef->multipleRuns, t);
	      printf("\n\nAverage Time per Bootstrap %f\n", (double)(t/((double)adef->multipleRuns)));
	      printf("All %d bootstrapped trees written to: %s\n", adef->multipleRuns, bootstrapFileName);
	      infoFile = fopen(infoFileName, "a");
	      fprintf(infoFile, "\n\nOverall Time for %d Bootstraps %f\n", adef->multipleRuns, t);
	      fprintf(infoFile, "Average Time per Bootstrap %f\n", (double)(t/((double)adef->multipleRuns)));	     
	      fprintf(infoFile, "\n\nAll %d bootstrapped trees written to: %s\n", adef->multipleRuns, bootstrapFileName);
	      fclose(infoFile);
	    }
	  else
	    {
	      if(adef->multipleRuns > 1)
		{
		  double avgLH = 0;
		  double bestLH = unlikely;
		  int i, bestI  = 0;
		  
		  for(i = 0; i < adef->multipleRuns; i++)
		    {     
		      avgLH   += tr->likelihoods[i];
		      if(tr->likelihoods[i] > bestLH)
			{
			  bestLH = tr->likelihoods[i];
			  bestI  = i;
			}
		    }
		  avgLH /= ((double)adef->multipleRuns);

		  printf("\n\nOverall Time for %d Inferences %f\n", adef->multipleRuns, t);
		  printf("Average Time per Inference %f\n", (double)(t/((double)adef->multipleRuns)));		 
		  printf("Average Likelihood   : %f\n", avgLH);
		  printf("\n");
		  printf("Best Likelihood in run number %d: likelihood %f\n\n", bestI, bestLH);

		  if(adef->checkpoints)   
		    printf("Checkpoints written to:                 %s.RUN.%d.* to %d.*\n", checkpointFileName, 0, adef->multipleRuns - 1);  
		  if(!adef->restart)
		    {
		      if(adef->randomStartingTree)
			printf("Random starting trees written to:       %s.RUN.%d to %d\n", randomFileName, 0, adef->multipleRuns - 1);
		      else
			printf("Parsimony starting trees written to:    %s.RUN.%d to %d\n", permFileName, 0, adef->multipleRuns - 1);   	            
		    }					  
		  printf("Final trees written to:                 %s.RUN.%d to %d\n", resultFileName,  0, adef->multipleRuns - 1);  
		  printf("Execution Log Files written to:         %s.RUN.%d to %d\n", logFileName, 0, adef->multipleRuns - 1);   
		  printf("Execution information file written to:  %s\n", infoFileName);


		  infoFile = fopen(infoFileName, "a");

		  fprintf(infoFile, "\n\nOverall Time for %d Inferences %f\n", adef->multipleRuns, t);
		  fprintf(infoFile, "Average Time per Inference %f\n", (double)(t/((double)adef->multipleRuns)));		 
		  fprintf(infoFile, "Average Likelihood   : %f\n", avgLH);
		  fprintf(infoFile, "\n");
		  fprintf(infoFile, "Best Likelihood in run number %d: likelihood %f\n\n", bestI, bestLH); 
		  if(adef->checkpoints)   
		    fprintf(infoFile, "Checkpoints written to:                %s.RUN.%d.* to %d.*\n", checkpointFileName, 0, adef->multipleRuns - 1);  
		  if(!adef->restart)
		    {
		      if(adef->randomStartingTree)
			fprintf(infoFile, "Random starting trees written to:      %s.RUN.%d to %d\n", randomFileName, 0, adef->multipleRuns - 1);
		      else
			fprintf(infoFile, "Parsimony starting trees written to:   %s.RUN.%d to %d\n", permFileName, 0, adef->multipleRuns - 1);   	            
		    }					  
		  fprintf(infoFile, "Final trees written to:                %s.RUN.%d to %d\n", resultFileName,  0, adef->multipleRuns - 1);  
		  fprintf(infoFile, "Execution Log Files written to:        %s.RUN.%d to %d\n", logFileName, 0, adef->multipleRuns - 1);   
		  fprintf(infoFile, "Execution information file written to: %s\n", infoFileName);

		  fclose(infoFile);		  		 		   
		}
	      else
		{
		  printf("\n\nOverall Time for 1 Inference %f\n", t);		  
		  printf("Likelihood   : %f\n", tr->likelihood);
		  printf("\n\n");	     

		  if(adef->checkpoints)   
		  printf("Checkpoints written to:                %s.*\n", checkpointFileName);  
		  if(!adef->restart)
		    {
		      if(adef->randomStartingTree)
			printf("Random starting tree written to:       %s\n", randomFileName);
		      else
			printf("Parsimony starting tree written to:    %s\n", permFileName);   	            
		    }					  
		  printf("Final tree written to:                 %s\n", resultFileName);  
		  printf("Execution Log File written to:         %s\n", logFileName);   
		  printf("Execution information file written to: %s\n",infoFileName);

		  infoFile = fopen(infoFileName, "a");
		  
		  fprintf(infoFile, "\n\nOverall Time for 1 Inference %f\n", t);		  
		  fprintf(infoFile, "Likelihood   : %f\n", tr->likelihood);
		  fprintf(infoFile, "\n\n");

		  if(adef->checkpoints)   
		    fprintf(infoFile, "Checkpoints written to:                %s.*\n", checkpointFileName);  
		  if(!adef->restart)
		    {
		      if(adef->randomStartingTree)
			fprintf(infoFile, "Random starting tree written to:       %s\n", randomFileName);
		      else
			fprintf(infoFile, "Parsimony starting tree written to:    %s\n", permFileName);   	            
		    }					  
		  fprintf(infoFile, "Final tree written to:                 %s\n", resultFileName);  
		  fprintf(infoFile, "Execution Log File written to:         %s\n", logFileName);   
		  fprintf(infoFile, "Execution information file written to: %s\n",infoFileName);
		  
		  fclose(infoFile);		  
		}
	    }
	    
	  break;
	  /*case RELL_BOOTSTRAP:

	  printf("\n\n%d alternative topologies have been relled\n", numberOfRelledTopologies);	 
	  printf("Overall Time for RELL bootstrap %f\n", t);
	  printf("RELL-Bootstrap Information written to file %s\n",rellInfoFileName);
	  printf("RELL-Bootstrap Tree written to file %s\n", rellBootFileName);

	  infoFile = fopen(infoFileName, "a");
	   
	  fprintf(infoFile, "\n\n%d alternative topologies have been relled\n", numberOfRelledTopologies);
	  fprintf(infoFile, "Overall Time for RELL bootstrap %f\n", t);
	  fprintf(infoFile, "RELL-Bootstrap Information written to file %s\n",rellInfoFileName);
	  fprintf(infoFile, "RELL-Bootstrap Tree written to file %s\n", rellBootFileName);

	  fclose(infoFile);
	  break;*/
	case CALC_BIPARTITIONS:
	  printf("\n\nTime for Computation of Bipartitions %f\n", t);
	  printf("Tree with bipartitions written to file:  %s\n", bipartitionsFileName);
	  printf("Execution information file written to :  %s\n",infoFileName);
	  infoFile = fopen(infoFileName, "a");

	  fprintf(infoFile, "\n\nTime for Computation of Bipartitions %f\n", t);
	  fprintf(infoFile, "Tree with bipartitions written to file:  %s\n", bipartitionsFileName);

	  fclose(infoFile);
	  break;
	}

    }

}



/********************PRINTING various INFO **************************************/

/************************************************************************************/


int main (int argc, char *argv[]) 
{   
  rawdata      *rdta;
  cruncheddata *cdta;
  tree         *tr;         
  analdef      *adef;  
   
#ifdef PARALLEL
  MPI_Init(&argc, &argv); 
  MPI_Comm_rank(MPI_COMM_WORLD, &processID);  
  MPI_Comm_size(MPI_COMM_WORLD, &numOfWorkers);       
#endif  
#ifndef PARALLEL
  processID = 0;
#endif
 

  

  masterTime = gettime();            

  adef = (analdef *)malloc(sizeof(analdef));
  rdta = (rawdata *)malloc(sizeof(rawdata));
  cdta = (cruncheddata *)malloc(sizeof(cruncheddata));     
  tr   = (tree *)malloc(sizeof(tree));

  initAdef(adef);
  get_args(argc,argv,(boolean)1, adef, rdta, tr);                  
  readData(FALSE, adef, rdta, cdta, tr);  
  checkOutgroups(tr, adef);
  makeFileNames(tr, adef, argc, argv);
  checkSequences(tr, rdta, adef); 

  if(adef->mode == SPLIT_MULTI_GENE)
    {
      splitMultiGene(tr, rdta, adef);
      exit(0);
    }
  if(adef->mode == CHECK_ALIGNMENT)
    {
      printf("Alignment format can be read by RAxML \n");
      exit(0);
    }  

  makeweights(adef, rdta, cdta, tr);
  makevalues(rdta, cdta, tr, adef, FALSE);    
  initModel(tr, rdta, cdta, adef);                                                  
  printModelAndProgramInfo(tr, adef, argc, argv);  
 
  switch(adef->mode)
    {    
    case TREE_EVALUATION:    
      getStartingTree(tr, adef);      
      modOpt(tr, adef);
      printLog(tr, adef, TRUE);         
      printResult(tr, adef, TRUE);     
      break;     
    case CALC_BIPARTITIONS:    
      calcBipartitions(tr, adef);   
      break;
    case BIG_RAPID_MODE:
      if(adef->boot)    
	doBootstrap(tr, adef, rdta, cdta);
      else  
	doInference(tr, adef, rdta, cdta);          
      break;
    }

  finalizeInfoFile(tr, adef);

#ifdef PARALLEL
  MPI_Finalize();
#endif

  return 0;
}
