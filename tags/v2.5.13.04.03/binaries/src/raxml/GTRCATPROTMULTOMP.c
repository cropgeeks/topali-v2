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

#include <math.h>
#include <unistd.h>
#include <time.h> 
#include <stdlib.h>
#include <stdio.h>
#include <ctype.h>
#include <string.h>
#include "axml.h"

#define PROTLH_SIZE sizeof(protlikelivector)

static void newviewPartialGTRCATPROTMULT(tree *tr, nodeptr p, int i, double ki, protlikelivector *x3, int model, double *EIGN, double *eptr, double *eptr2) 
{     
  double  d1[19], d2[19], ump_x1[20], ump_x2[20], x1px2, z1, lz1, z2, lz2;
  double  *left, *right, *eptr3, *eptr4;
  nodeptr  q, r;   
  protlikelivector   x1, x2;
  int l, scale;

  q = p->next->back;
  r = p->next->next->back;   

  if(q->tip && r->tip)
    {
      memcpy(&x1, &tr->protTip[model * 23 + q->tip[i]], PROTLH_SIZE);
      memcpy(&x2, &tr->protTip[model * 23 + r->tip[i]], PROTLH_SIZE);
      x3->exp = 0;
    }

  if(q->tip && !r->tip)
    {			
      memcpy(&x1, &tr->protTip[model * 23 + q->tip[i]], PROTLH_SIZE);
      newviewPartialGTRCATPROTMULT(tr, r, i, ki, &x2, model, EIGN, eptr, eptr2); 
      x3->exp = x2.exp;
    }

  if(!q->tip && r->tip)
    {	
      newviewPartialGTRCATPROTMULT(tr, q, i, ki, &x1, model, EIGN, eptr, eptr2); 
      memcpy(&x2, &tr->protTip[model * 23 + r->tip[i]], PROTLH_SIZE);
      x3->exp = x1.exp;
    }
    
  if(!q->tip && !r->tip)
    {	
      newviewPartialGTRCATPROTMULT(tr, q, i, ki, &x1, model, EIGN, eptr, eptr2);
      newviewPartialGTRCATPROTMULT(tr, r, i, ki, &x2, model, EIGN, eptr, eptr2); 
      x3->exp = x1.exp + x2.exp;
    }
   
  z1  = q->z;
  lz1 = (z1 > zmin) ? log(z1) : log(zmin);  
  lz1 *= ki;

  z2  = r->z;
  lz2 = (z2 > zmin) ? log(z2) : log(zmin);
  lz2 *= ki;
  
  left  = &(x1.v[0]);
  right = &(x2.v[0]);

  d1[0] = left[1] * exp(EIGN[0] * lz1);
  d1[1] = left[2] * exp(EIGN[1] * lz1);
  d1[2] = left[3] * exp(EIGN[2] * lz1);
  d1[3] = left[4] * exp(EIGN[3] * lz1);
  d1[4] = left[5] * exp(EIGN[4] * lz1);
  d1[5] = left[6] * exp(EIGN[5] * lz1);
  d1[6] = left[7] * exp(EIGN[6] * lz1);
  d1[7] = left[8] * exp(EIGN[7] * lz1);
  d1[8] = left[9] * exp(EIGN[8] * lz1);
  d1[9] = left[10] * exp(EIGN[9] * lz1);
  d1[10] = left[11] * exp(EIGN[10] * lz1);
  d1[11] = left[12] * exp(EIGN[11] * lz1);
  d1[12] = left[13] * exp(EIGN[12] * lz1);
  d1[13] = left[14] * exp(EIGN[13] * lz1);
  d1[14] = left[15] * exp(EIGN[14] * lz1);
  d1[15] = left[16] * exp(EIGN[15] * lz1);
  d1[16] = left[17] * exp(EIGN[16] * lz1);
  d1[17] = left[18] * exp(EIGN[17] * lz1);
  d1[18] = left[19] * exp(EIGN[18] * lz1);
  
  d2[0] = right[1] * exp(EIGN[0] * lz2);
  d2[1] = right[2] * exp(EIGN[1] * lz2);
  d2[2] = right[3] * exp(EIGN[2] * lz2);
  d2[3] = right[4] * exp(EIGN[3] * lz2);
  d2[4] = right[5] * exp(EIGN[4] * lz2);
  d2[5] = right[6] * exp(EIGN[5] * lz2);
  d2[6] = right[7] * exp(EIGN[6] * lz2);
  d2[7] = right[8] * exp(EIGN[7] * lz2);
  d2[8] = right[9] * exp(EIGN[8] * lz2);
  d2[9] = right[10] * exp(EIGN[9] * lz2);
  d2[10] = right[11] * exp(EIGN[10] * lz2);
  d2[11] = right[12] * exp(EIGN[11] * lz2);
  d2[12] = right[13] * exp(EIGN[12] * lz2);
  d2[13] = right[14] * exp(EIGN[13] * lz2);
  d2[14] = right[15] * exp(EIGN[14] * lz2);
  d2[15] = right[16] * exp(EIGN[15] * lz2);
  d2[16] = right[17] * exp(EIGN[16] * lz2);
  d2[17] = right[18] * exp(EIGN[17] * lz2);
  d2[18] = right[19] * exp(EIGN[18] * lz2);
  

  eptr4 = eptr;

  for(l = 0; l < 20; l++)
    {
      ump_x1[l] = left[0];    
      ump_x1[l] += d1[0] * *eptr4++;
      ump_x1[l] += d1[1] * *eptr4++;
      ump_x1[l] += d1[2] * *eptr4++;
      ump_x1[l] += d1[3] * *eptr4++;
      ump_x1[l] += d1[4] * *eptr4++;
      ump_x1[l] += d1[5] * *eptr4++;
      ump_x1[l] += d1[6] * *eptr4++;
      ump_x1[l] += d1[7] * *eptr4++;
      ump_x1[l] += d1[8] * *eptr4++;
      ump_x1[l] += d1[9] * *eptr4++;
      ump_x1[l] += d1[10] * *eptr4++;
      ump_x1[l] += d1[11] * *eptr4++;
      ump_x1[l] += d1[12] * *eptr4++;
      ump_x1[l] += d1[13] * *eptr4++;
      ump_x1[l] += d1[14] * *eptr4++;
      ump_x1[l] += d1[15] * *eptr4++;
      ump_x1[l] += d1[16] * *eptr4++;
      ump_x1[l] += d1[17] * *eptr4++;
      ump_x1[l] += d1[18] * *eptr4++;   
    }

    
  eptr4 = eptr;
 
  for(l = 0; l < 20; l++)
    {
      ump_x2[l] = right[0];
      ump_x2[l] += d2[0] * *eptr4++;
      ump_x2[l] += d2[1] * *eptr4++;
      ump_x2[l] += d2[2] * *eptr4++;
      ump_x2[l] += d2[3] * *eptr4++;
      ump_x2[l] += d2[4] * *eptr4++;
      ump_x2[l] += d2[5] * *eptr4++;
      ump_x2[l] += d2[6] * *eptr4++;
      ump_x2[l] += d2[7] * *eptr4++;
      ump_x2[l] += d2[8] * *eptr4++;
      ump_x2[l] += d2[9] * *eptr4++;
      ump_x2[l] += d2[10] * *eptr4++;
      ump_x2[l] += d2[11] * *eptr4++;
      ump_x2[l] += d2[12] * *eptr4++;
      ump_x2[l] += d2[13] * *eptr4++;
      ump_x2[l] += d2[14] * *eptr4++;
      ump_x2[l] += d2[15] * *eptr4++;
      ump_x2[l] += d2[16] * *eptr4++;
      ump_x2[l] += d2[17] * *eptr4++;
      ump_x2[l] += d2[18] * *eptr4++;   
      }

  left = x3->v;

  eptr3 = eptr2;

  x1px2 = ump_x1[0] * ump_x2[0];  
  left[0] = x1px2 * *eptr3++;
  left[1] = x1px2 * *eptr3++;
  left[2] = x1px2 * *eptr3++;
  left[3] = x1px2 * *eptr3++;
  left[4] = x1px2 * *eptr3++;
  left[5] = x1px2 * *eptr3++;
  left[6] = x1px2 * *eptr3++;
  left[7] = x1px2 * *eptr3++;
  left[8] = x1px2 * *eptr3++;
  left[9] = x1px2 * *eptr3++;
  left[10] = x1px2 * *eptr3++;
  left[11] = x1px2 * *eptr3++;
  left[12] = x1px2 * *eptr3++;
  left[13] = x1px2 * *eptr3++;
  left[14] = x1px2 * *eptr3++;
  left[15] = x1px2 * *eptr3++;
  left[16] = x1px2 * *eptr3++;
  left[17] = x1px2 * *eptr3++;
  left[18] = x1px2 * *eptr3++;
  left[19] = x1px2 * *eptr3++;

  for(l = 1; l < 20; l++)
    {
      x1px2 = ump_x1[l] * ump_x2[l];
      left[0] += x1px2 * *eptr3++;
      left[1] += x1px2 * *eptr3++;
      left[2] += x1px2 * *eptr3++;
      left[3] += x1px2 * *eptr3++;
      left[4] += x1px2 * *eptr3++;
      left[5] += x1px2 * *eptr3++;
      left[6] += x1px2 * *eptr3++;
      left[7] += x1px2 * *eptr3++;
      left[8] += x1px2 * *eptr3++;
      left[9] += x1px2 * *eptr3++;
      left[10] += x1px2 * *eptr3++;
      left[11] += x1px2 * *eptr3++;
      left[12] += x1px2 * *eptr3++;
      left[13] += x1px2 * *eptr3++;
      left[14] += x1px2 * *eptr3++;
      left[15] += x1px2 * *eptr3++;
      left[16] += x1px2 * *eptr3++;
      left[17] += x1px2 * *eptr3++;
      left[18] += x1px2 * *eptr3++;
      left[19] += x1px2 * *eptr3++;
    }

  scale = 1;
  for(l = 0; scale && (l < 20); l++)
    scale = ((left[l] < minlikelihood) && (left[l] > minusminlikelihood));	       	      	      	       	       
	     
  if(scale)
    {	     
      for(l = 0; l < 20; l++)
	left[l] *= twotothe256;		   
      x3->exp += 1;
    }

  return;
}

double evaluatePartialGTRCATPROTMULT (tree *tr, nodeptr p, int i, double ki)
{
  double z, lz, term, *left, *right;    
  nodeptr  q;
  int     w, model;    
  double  *ds, d[19], *e, *EI, *EV;
  protlikelivector   x1, x2; 
  int scale;

  model = tr->model[i];
  w = tr->cdta->aliaswgt[i];
  e =  &(tr->EIGN[model * 19]);
  EI = &(tr->EI[model * 380]);
  EV = &(tr->EV[model * 400]);
  ds = d;

  q = p->back; 

  if(!p->tip && q->tip)
    {
      newviewPartialGTRCATPROTMULT(tr, p, i, ki, &x1, model, e, EI, EV);
      memcpy(&x2, &tr->protTip[model * 23 + q->tip[i]], PROTLH_SIZE);      
      scale = x1.exp;
    }
    
  if(p->tip && !q->tip)
    {     
      memcpy(&x1, &tr->protTip[model * 23 + p->tip[i]], PROTLH_SIZE);
      newviewPartialGTRCATPROTMULT(tr, q, i, ki, &x2, model, e, EI, EV);     
      scale = x2.exp;
    }

  if(!p->tip && !q->tip)
    {      
      newviewPartialGTRCATPROTMULT(tr, p, i, ki, &x1, model, e, EI, EV);
      newviewPartialGTRCATPROTMULT(tr, q, i, ki, &x2, model, e, EI, EV);     
      scale = x1.exp + x2.exp;
    }
     
  z = p->z;
  
  if (z < zmin) 
    z = zmin;
  
  lz = log(z) * ki;
  
  
  d[0] = exp (e[0] * lz);
  d[1] = exp (e[1] * lz);
  d[2] = exp (e[2] * lz);
  d[3] = exp (e[3] * lz);
  d[4] = exp (e[4] * lz);
  d[5] = exp (e[5] * lz);
  d[6] = exp (e[6] * lz);
  d[7] = exp (e[7] * lz);
  d[8] = exp (e[8] * lz);
  d[9] = exp (e[9] * lz);
  d[10] = exp (e[10] * lz);
  d[11] = exp (e[11] * lz);
  d[12] = exp (e[12] * lz);
  d[13] = exp (e[13] * lz);
  d[14] = exp (e[14] * lz);
  d[15] = exp (e[15] * lz);
  d[16] = exp (e[16] * lz);
  d[17] = exp (e[17] * lz);
  d[18] = exp (e[18] * lz);
  

  left  = &(x1.v[0]);
  right = &(x2.v[0]);
  
  term =  *left++ * *right++;  
  term += *left++ * *right++ * *ds++; 
  term += *left++ * *right++ * *ds++; 
  term += *left++ * *right++ * *ds++; 
  term += *left++ * *right++ * *ds++;

  term += *left++ * *right++ * *ds++; 
  term += *left++ * *right++ * *ds++; 
  term += *left++ * *right++ * *ds++; 
  term += *left++ * *right++ * *ds++;
  term += *left++ * *right++ * *ds++; 

  term += *left++ * *right++ * *ds++; 
  term += *left++ * *right++ * *ds++; 
  term += *left++ * *right++ * *ds++; 
  term += *left++ * *right++ * *ds++;
  term += *left++ * *right++ * *ds++; 

  term += *left++ * *right++ * *ds++; 
  term += *left++ * *right++ * *ds++; 
  term += *left++ * *right++ * *ds++; 
  term += *left++ * *right++ * *ds++;
  term += *left++ * *right++ * *ds++; 

  term = log(term) + scale * log(minlikelihood);   
 
  return  (w * term);
}


/*********************************************************************************************/


double evaluateGTRCATPROTMULT (tree *tr, nodeptr p)
  {
    double   sum, z, lz, ki, lza[19]; 
    nodeptr  q;
    int     i, l;
    int     *wptr = tr->cdta->aliaswgt, *cptr;    
    double  *diagptable, *rptr, *diagptable_start;
    double *EIGN = tr->EIGN;
    protlikelivector   *x1, *x2, *x1_start, *x2_start;       
    int model, *modelptr, modelCounter;   
    int cat;
    double term;
    double *left, *right;

    q = p->back;
    
    rptr   = &(tr->cdta->patrat[0]); 
    cptr  = &(tr->cdta->rateCategory[0]);
    modelptr = tr->model;

    z = p->z;
   
    if (z < zmin) z = zmin;
    lz = log(z);        

    diagptable = diagptable_start = (double *)malloc(sizeof(double) * tr->NumberOfCategories * 19 * tr->NumberOfModels);    

    /* OpenMP sucks here, at least for 3 distindct models */
    /*#pragma omp parallel for private(l, lza, rptr, i, ki, diagptable)*/
    for(modelCounter = 0; modelCounter < tr->NumberOfModels; modelCounter++)
     {             
       for(l = 0; l < 19; l++)      
	 lza[l] = EIGN[modelCounter * 19 + l] * lz; 
 
       /* OPENMP 
	  diagptable = &diagptable_start[modelCounter * tr->NumberOfCategories * 19];*/

       rptr   = &(tr->cdta->patrat[0]);

       for(i = 0; i <  tr->NumberOfCategories; i++)
	 {	
	   ki = *rptr++;	 
	   
	   *diagptable++ = exp (ki * lza[0]);	   
	   *diagptable++ = exp (ki * lza[1]);
	   *diagptable++ = exp (ki * lza[2]);	   
	   *diagptable++ = exp (ki * lza[3]);
	   *diagptable++ = exp (ki * lza[4]);	   
	   *diagptable++ = exp (ki * lza[5]);
	   *diagptable++ = exp (ki * lza[6]);	   
	   *diagptable++ = exp (ki * lza[7]);
	   *diagptable++ = exp (ki * lza[8]);	   
	   *diagptable++ = exp (ki * lza[9]);
	   *diagptable++ = exp (ki * lza[10]);	   
	   *diagptable++ = exp (ki * lza[11]);
	   *diagptable++ = exp (ki * lza[12]);	   
	   *diagptable++ = exp (ki * lza[13]);
	   *diagptable++ = exp (ki * lza[14]);	   
	   *diagptable++ = exp (ki * lza[15]);
	   *diagptable++ = exp (ki * lza[16]);	   
	   *diagptable++ = exp (ki * lza[17]);
	   *diagptable++ = exp (ki * lza[18]);	          
	 }
     }
	    
    sum = 0.0;   

    if(p->tip || q->tip)
      {
	char *tipX1;	
	nodeptr tmp; 
	    
	if(q->tip)
	  {	
	    tmp = p;
	    p = q;
	    q = tmp;	      
	  }

	tipX1 = p->tip;      

	while ((! q->x)) 
	  {	   
	    if (! (q->x)) if (! newviewGTRCATPROTMULT(tr, q)) return badEval;
	  }
	
	x2  = x2_start = (protlikelivector*)q->x;
#pragma omp parallel for private(left, right, diagptable, term, cat, x1, x2, model) reduction(+ : sum)
	for (i = 0; i < tr->cdta->endsite; i++) 
	  {	    
	    model = modelptr[i];
	    cat = cptr[i];	  
	    x1 = &(tr->protTip[model * 23 + tipX1[i]]);
	    x2 = &x2_start[i];

	    diagptable = &diagptable_start[model * 19 * tr->NumberOfCategories + 19 * cat];
	    
	    left = x1->v;
	    right = x2->v;
	   
	    term =  left[0] * right[0];
	    term += left[1] * right[1] * *diagptable++; 
	    term += left[2] * right[2] * *diagptable++; 
	    term += left[3] * right[3] * *diagptable++; 
	    term += left[4] * right[4] * *diagptable++;	   
	    term += left[5] * right[5] * *diagptable++; 
	    term += left[6] * right[6] * *diagptable++; 
	    term += left[7] * right[7] * *diagptable++; 
	    term += left[8] * right[8] * *diagptable++;
	    term += left[9] * right[9] * *diagptable++; 	    
	    term += left[10] * right[10] * *diagptable++; 
	    term += left[11] * right[11] * *diagptable++; 
	    term += left[12] * right[12] * *diagptable++; 
	    term += left[13] * right[13] * *diagptable++;
	    term += left[14] * right[14] * *diagptable++; 	    
	    term += left[15] * right[15] * *diagptable++; 
	    term += left[16] * right[16] * *diagptable++; 
	    term += left[17] * right[17] * *diagptable++; 
	    term += left[18] * right[18] * *diagptable++;
	    term += left[19] * right[19] * *diagptable++; 
	    		   	    
	    term = log(term) + x2->exp * log(minlikelihood);	   	  	

	    sum += wptr[i] * term;		  	    	  
	  }

	free(diagptable_start);        
	tr->likelihood = sum;
	return  sum;
      }         
      
    while ((! p->x) || (! q->x)) 
      {
	if (! (p->x)) if (! newviewGTRCATPROTMULT(tr, p)) return badEval;
	if (! (q->x)) if (! newviewGTRCATPROTMULT(tr, q)) return badEval;
      }

    x1  = x1_start = (protlikelivector*)p->x;
    x2  = x2_start = (protlikelivector*)q->x;
#pragma omp parallel for private(left, right, diagptable, term, cat, x1, x2, model) reduction(+ : sum)    
    for (i = 0; i < tr->cdta->endsite; i++) 
      {		   
	model = modelptr[i];
	cat = cptr[i];
	diagptable = &diagptable_start[model * 19 * tr->NumberOfCategories + 19 * cat];		

	x1 = &x1_start[i];
	x2 = &x2_start[i];

	left  = x1->v;
	right = x2->v;
	
	term =  left[0] * right[0];
	term += left[1] * right[1] * *diagptable++; 
	term += left[2] * right[2] * *diagptable++; 
	term += left[3] * right[3] * *diagptable++; 
	term += left[4] * right[4] * *diagptable++;	   
	term += left[5] * right[5] * *diagptable++; 
	term += left[6] * right[6] * *diagptable++; 
	term += left[7] * right[7] * *diagptable++; 
	term += left[8] * right[8] * *diagptable++;
	term += left[9] * right[9] * *diagptable++; 	    
	term += left[10] * right[10] * *diagptable++; 
	term += left[11] * right[11] * *diagptable++; 
	term += left[12] * right[12] * *diagptable++; 
	term += left[13] * right[13] * *diagptable++;
	term += left[14] * right[14] * *diagptable++; 	    
	term += left[15] * right[15] * *diagptable++; 
	term += left[16] * right[16] * *diagptable++; 
	term += left[17] * right[17] * *diagptable++; 
	term += left[18] * right[18] * *diagptable++;
	term += left[19] * right[19] * *diagptable++; 

	term = log(term) + (x1->exp + x2->exp) * log(minlikelihood);
	sum += wptr[i] * term;		         
      }

    free(diagptable_start);     
    tr->likelihood = sum;
    return  sum;         
  } 



double makenewzGTRCATPROTMULT (tree *tr, nodeptr p, nodeptr q, double z0, int maxiter)
  { 
    double   z, zprev, zstep;    
    protlikelivector  *x1, *x2, *x1_start, *x2_start;
    double  *sumtable, *sum;
    int     i, l, *cptr;
    double  dlnLdlz;                 
    double  d2lnLdlz2;                  
    double *d1, *d_start, *rptr;
    double ki;
    double *e, *s, *e_start, *s_start;
    double *EIGN = tr->EIGN, *left, *right;
    int model, *modelptr, modelCounter;    

    e = e_start =    (double *)malloc(19 * tr->NumberOfModels * sizeof(double));
    s = s_start =    (double *)malloc(19 * tr->NumberOfModels * sizeof(double));            
    sum = sumtable = (double *)malloc(20 * tr->cdta->endsite * sizeof(double));
    d1 = d_start =   (double *)malloc(tr->NumberOfModels * tr->NumberOfCategories * 19 * sizeof(double));         
   
    modelptr = tr->model;
   
    if(p->tip && q->tip)
      {
	char *tipX1 = p->tip;   
	char *tipX2 = q->tip;     
#pragma omp parallel for private(left, right, sum, model)
	for (i = 0; i < tr->cdta->endsite; i++) 
	  {    
	    model = modelptr[i];
	    left = &(tr->protTip[model * 23 + tipX1[i]].v[0]);
	    right = &(tr->protTip[model * 23 + tipX2[i]].v[0]);	
	    sum = &sumtable[i * 20];

	    *sum++ = left[0] * right[0];			
	    *sum++ = left[1] * right[1];	
	    *sum++ = left[2] * right[2];			
	    *sum++ = left[3] * right[3];
	    *sum++ = left[4] * right[4];			
	    *sum++ = left[5] * right[5];	
	    *sum++ = left[6] * right[6];			
	    *sum++ = left[7] * right[7];
	    *sum++ = left[8] * right[8];			
	    *sum++ = left[9] * right[9];	
	    *sum++ = left[10] * right[10];			
	    *sum++ = left[11] * right[11];
	    *sum++ = left[12] * right[12];			
	    *sum++ = left[13] * right[13];	
	    *sum++ = left[14] * right[14];			
	    *sum++ = left[15] * right[15];
	    *sum++ = left[16] * right[16];			
	    *sum++ = left[17] * right[17];	
	    *sum++ = left[18] * right[18];			
	    *sum++ = left[19] * right[19];	
	  }
      }
    else
      {
	if(p->tip || q->tip)
	  {	    	    
	    char *tipX1;
	    nodeptr tmp; 
	    
	    if(q->tip)
	      {	
		tmp = p;
		p = q;
		q = tmp;	      
	      }
	    
	    tipX1 = p->tip;     
	    
	    while ((! q->x)) 
	      {	 
		if (! (q->x)) if (! newviewGTRCATPROTMULT(tr, q)) return badZ;		 
	      }	   
	    
	    x2 = x2_start = (protlikelivector*)q->x;
#pragma omp parallel for private(left, right, sum, x2, model)	    	    
	    for (i = 0; i < tr->cdta->endsite; i++) 
	      {    
		model = modelptr[i];
		left = &(tr->protTip[model * 23 + tipX1[i]].v[0]);			       
		sum = &sumtable[i * 20];
		x2 = &x2_start[i];
		right = x2->v;
		
		*sum++ = left[0] * right[0];			
		*sum++ = left[1] * right[1];	
		*sum++ = left[2] * right[2];			
		*sum++ = left[3] * right[3];
		*sum++ = left[4] * right[4];			
		*sum++ = left[5] * right[5];	
		*sum++ = left[6] * right[6];			
		*sum++ = left[7] * right[7];
		*sum++ = left[8] * right[8];			
		*sum++ = left[9] * right[9];	
		*sum++ = left[10] * right[10];			
		*sum++ = left[11] * right[11];
		*sum++ = left[12] * right[12];			
		*sum++ = left[13] * right[13];	
		*sum++ = left[14] * right[14];			
		*sum++ = left[15] * right[15];
		*sum++ = left[16] * right[16];			
		*sum++ = left[17] * right[17];	
		*sum++ = left[18] * right[18];			
		*sum++ = left[19] * right[19];				      	 
	      }	  
	  }
	else
	  {	    
	    while ((! p->x) || (! q->x)) 
	      {
		if (! (p->x)) if (! newviewGTRCATPROTMULT(tr, p)) return badZ;		  
		if (! (q->x)) if (! newviewGTRCATPROTMULT(tr, q)) return badZ;		  
	      }
	    
	    x1 = x1_start = (protlikelivector*)p->x;
	    x2 = x2_start = (protlikelivector*)q->x;
	    
#pragma omp parallel for private(left, right, sum, x1, x2, model)
	    for (i = 0; i < tr->cdta->endsite; i++) 
	      {     
		x1 = &x1_start[i];
		x2 = &x2_start[i];
		sum = &sumtable[i * 20];
		left = x1->v;
		right = x2->v;	
		
		*sum++ = left[0] * right[0];			
		*sum++ = left[1] * right[1];	
		*sum++ = left[2] * right[2];			
		*sum++ = left[3] * right[3];
		*sum++ = left[4] * right[4];			
		*sum++ = left[5] * right[5];	
		*sum++ = left[6] * right[6];			
		*sum++ = left[7] * right[7];
		*sum++ = left[8] * right[8];			
		*sum++ = left[9] * right[9];	
		*sum++ = left[10] * right[10];			
		*sum++ = left[11] * right[11];
		*sum++ = left[12] * right[12];			
		*sum++ = left[13] * right[13];	
		*sum++ = left[14] * right[14];			
		*sum++ = left[15] * right[15];
		*sum++ = left[16] * right[16];			
		*sum++ = left[17] * right[17];	
		*sum++ = left[18] * right[18];			
		*sum++ = left[19] * right[19];		    
	      }
	  }
      }
      
    z = z0;
    do 
      {	
	int curvatOK = FALSE;

	zprev = z;
      
	zstep = (1.0 - zmax) * z + zmin;

	do 
	  {		
	    double lz;
	    double *wrptr   = &(tr->cdta->wr[0]);
	    double *wr2ptr  = &(tr->cdta->wr2[0]);
	    double dd[19];	
	    double tmp;
	    double inv_Li, dlnLidlz, d2lnLidlz2;

	    dlnLdlz = 0.0;                 
	    d2lnLdlz2 = 0.0;              

	    if (z < zmin) z = zmin;
	    else if (z > zmax) z = zmax;
	    lz    = log(z);
        
	    cptr  = &(tr->cdta->rateCategory[0]);
	    sum = sumtable;
		
	    d1 = d_start;
	    e = e_start;
	    s = s_start;

	    for(modelCounter = 0; modelCounter < tr->NumberOfModels; modelCounter++)
	      {	    
		for(l = 0; l < 19; l++)
		  {
		    e[modelCounter * 19 + l] = EIGN[modelCounter * 19 + l] * EIGN[modelCounter * 19 + l];
		    s[modelCounter * 19 + l] = EIGN[modelCounter * 19 + l];							       	
		    dd[l] = s[modelCounter * 19 + l] * lz;
		  }
		
		rptr   = &(tr->cdta->patrat[0]);
		
		for(i = 0; i < tr->NumberOfCategories; i++)
		  {
		    ki = *rptr++;
		    for(l = 0; l < 19; l++)			
		      *d1++ = exp(dd[l] * ki); 	      	    	  	   		    
		  }
	      }
	         
	    modelptr = tr->model;
	    
#pragma omp parallel for private(d1, e, s, sum, tmp, inv_Li, dlnLidlz, d2lnLidlz2, l, model) reduction(+ : dlnLdlz) reduction( + : d2lnLdlz2)
	    for (i = 0; i < tr->cdta->endsite; i++) 	
	      {			 		
		model = modelptr[i];
		sum = &sumtable[i * 20];

		d1 = &(d_start[model * tr->NumberOfCategories * 19 + 19 * cptr[i]]);	
		e  = &(e_start[model * 19]);
		s  = &(s_start[model * 19]);
		
		inv_Li = *sum++;	   
		inv_Li += (tmp = d1[0] * *sum++);
		dlnLidlz   = tmp * s[0];	   	   	    
		d2lnLidlz2 = tmp * e[0];
		
		for(l = 1; l < 19; l++)	      
		  {
		    inv_Li     += (tmp = d1[l] * *sum++);
		    dlnLidlz   += tmp * s[l];
		    d2lnLidlz2 += tmp * e[l];
		  }	   	    
		
		inv_Li = 1.0/inv_Li;  	    	    	    	    	    
		
		dlnLidlz   *= inv_Li;
		d2lnLidlz2 *= inv_Li;
		
		dlnLdlz    += wrptr[i] * dlnLidlz;
		d2lnLdlz2  += wr2ptr[i] * (d2lnLidlz2 - dlnLidlz * dlnLidlz);
	      }
	    
	    if ((d2lnLdlz2 >= 0.0) && (z < zmax))
	      zprev = z = 0.37 * z + 0.63;  /*  Bad curvature, shorten branch */
	    else
	      curvatOK = TRUE;
	    
	  } while (! curvatOK);
	
	if (d2lnLdlz2 < 0.0) {
	  double tantmp = -dlnLdlz / d2lnLdlz2;  /* prevent overflow */
	  if (tantmp < 100) {
	    z *= exp(tantmp);
	    if (z < zmin) z = zmin;
	    if (z > 0.25 * zprev + 0.75)    /*  Limit steps toward z = 1.0 */
	      z = 0.25 * zprev + 0.75;
	  } else {
	    z = 0.25 * zprev + 0.75;
	  }
	}
	if (z > zmax) z = zmax;

      } while ((--maxiter > 0) && (ABS(z - zprev) > zstep));
    
    free(d_start);   
    free(sumtable);    
    free(s_start);  
    free(e_start);   

    return  z;
  } /* makenewz */



/************************************************************************************************************************************************************/


boolean newviewGTRCATPROTMULT(tree *tr, nodeptr  p)
{   
  if(p->tip) return TRUE;

  { 	
    nodeptr q, r;  

    q = p->next->back;
    r = p->next->next->back; 

    if(r->tip && q->tip)
      {	  	 	       
       while (! p->x)
	 {	   
	   if (! p->x) if (! getxnode(p)) return FALSE; 	     
	 }
      }  
    else
      {
	if(r->tip || q->tip)
	  {		
	    nodeptr tmp;

	    if(r->tip)
	      {
		tmp = r;
		r = q;
		q = tmp;
	      }
	    
	    while ((! p->x) || (! r->x)) 
	      {	 
		if (! r->x) if (! newviewGTRCATPROTMULT(tr, r)) return FALSE;		  
		if (! p->x) if (! getxnode(p)) return FALSE;		       		 
	      }	   
	  }
	else
	  {
	    while ((! p->x) || (! q->x) || (! r->x)) 
	      {
		if (! q->x) if (! newviewGTRCATPROTMULT(tr, q)) return FALSE;		  
		if (! r->x) if (! newviewGTRCATPROTMULT(tr, r)) return FALSE;		  
		if (! p->x) if (! getxnode(p)) return FALSE;			  
	      }
	  }
      }
    
     {
       double  *left, *left_start, *right, *right_start, d1[19], d2[19];
       double *EIGN = tr->EIGN, *EV;
       double      
	 ump_x1[20], 
	 ump_x2[20], x1px2, z1, z2, ki;
       int i, l, cat, *cptr, scale,  modelCounter, model, *modelptr;	           
       double  *rptr, lz1[19], lz2[19], val, *v;
       protlikelivector *limit, *x1, *x2, *x3, *x1_start, *x2_start, *x3_start;        

       x3  = x3_start = (protlikelivector*)p->x; 	      
       
       modelptr = tr->model;

       cptr  = &(tr->cdta->rateCategory[0]);          
    
       z1  = q->z;
       z1 = (z1 > zmin) ? log(z1) : log(zmin);
       
       z2  = r->z;
       z2 = (z2 > zmin) ? log(z2) : log(zmin);  
       
       left_start = left = (double *)malloc(380 * tr->NumberOfModels * tr->NumberOfCategories * sizeof(double));
       right_start = right = (double *)malloc(380 * tr->NumberOfModels * tr->NumberOfCategories * sizeof(double));                         

       for(modelCounter = 0; modelCounter < tr->NumberOfModels; modelCounter++)
	 {       	 	   
	   lz1[0] = EIGN[modelCounter * 19] * z1;
	   lz1[1] = EIGN[modelCounter * 19 + 1] * z1;
	   lz1[2] = EIGN[modelCounter * 19 + 2] * z1;
	   lz1[3] = EIGN[modelCounter * 19 + 3] * z1;
	   lz1[4] = EIGN[modelCounter * 19 + 4] * z1;
	   lz1[5] = EIGN[modelCounter * 19 + 5] * z1;
	   lz1[6] = EIGN[modelCounter * 19 + 6] * z1;
	   lz1[7] = EIGN[modelCounter * 19 + 7] * z1;
	   lz1[8] = EIGN[modelCounter * 19 + 8] * z1;
	   lz1[9] = EIGN[modelCounter * 19 + 9] * z1;
	   lz1[10] = EIGN[modelCounter * 19 + 10] * z1;
	   lz1[11] = EIGN[modelCounter * 19 + 11] * z1;
	   lz1[12] = EIGN[modelCounter * 19 + 12] * z1;
	   lz1[13] = EIGN[modelCounter * 19 + 13] * z1;
	   lz1[14] = EIGN[modelCounter * 19 + 14] * z1;
	   lz1[15] = EIGN[modelCounter * 19 + 15] * z1;
	   lz1[16] = EIGN[modelCounter * 19 + 16] * z1;
	   lz1[17] = EIGN[modelCounter * 19 + 17] * z1;
	   lz1[18] = EIGN[modelCounter * 19 + 18] * z1;       
	   
	   rptr   = &(tr->cdta->patrat[0]); 
	   
	   for(i = 0; i < tr->NumberOfCategories; i++)
	     {	
	       ki = *rptr++;
	       
	       d1[0] = exp (ki * lz1[0]);
	       d1[1] = exp (ki * lz1[1]);
	       d1[2] = exp (ki * lz1[2]);
	       d1[3] = exp (ki * lz1[3]);
	       d1[4] = exp (ki * lz1[4]);
	       d1[5] = exp (ki * lz1[5]);
	       d1[6] = exp (ki * lz1[6]);
	       d1[7] = exp (ki * lz1[7]);
	       d1[8] = exp (ki * lz1[8]);
	       d1[9] = exp (ki * lz1[9]);
	       d1[10] = exp (ki * lz1[10]);
	       d1[11] = exp (ki * lz1[11]);
	       d1[12] = exp (ki * lz1[12]);
	       d1[13] = exp (ki * lz1[13]);
	       d1[14] = exp (ki * lz1[14]);
	       d1[15] = exp (ki * lz1[15]);
	       d1[16] = exp (ki * lz1[16]);
	       d1[17] = exp (ki * lz1[17]);
	       d1[18] = exp (ki * lz1[18]);
	       
	       EV = &(tr->EI[modelCounter * 380]);
	       for(l = 0; l < 20; l++)
		 {	       
		   *left++ = d1[0] * *EV++;	     	     		   	      
		   *left++ = d1[1] * *EV++;	 
		   *left++ = d1[2] * *EV++;	     	     		   	      
		   *left++ = d1[3] * *EV++;	 
		   *left++ = d1[4] * *EV++;	     	     		   	      
		   *left++ = d1[5] * *EV++;	 
		   *left++ = d1[6] * *EV++;	     	     		   	      
		   *left++ = d1[7] * *EV++;
		   *left++ = d1[8] * *EV++;	     	     		   	      
		   *left++ = d1[9] * *EV++;	 
		   *left++ = d1[10] * *EV++;	     	     		   	      
		   *left++ = d1[11] * *EV++;	 
		   *left++ = d1[12] * *EV++;	     	     		   	      
		   *left++ = d1[13] * *EV++;	 
		   *left++ = d1[14] * *EV++;	     	     		   	      
		   *left++ = d1[15] * *EV++;
		   *left++ = d1[16] * *EV++;	     	     		   	      
		   *left++ = d1[17] * *EV++;	 
		   *left++ = d1[18] * *EV++;	     	     		   	      	      
		 }	      
	     }
	 }
     

       
       if(r->tip && q->tip)
	 {	  	 	 
	   char *tipX1 = q->tip;
	   char *tipX2 = r->tip;	   	   	        	  
	   
#pragma omp parallel for private(cat, EV, left, x3, v, x1px2, l, ump_x1, ump_x2, model)
	   for (i = 0; i < tr->cdta->endsite; i++) 
	     {	 
	       model = modelptr[i];	   	    	   	   
	       cat = cptr[i];
	       EV = &(tr->EV[model * 400]);	       	   
	       left = &left_start[model * 380 * tr->NumberOfCategories + cat * 380];	      	       
	       v = &(tr->protTip[model * 23 + tipX1[i]].v[0]);	     	       	       
	       x3 = &x3_start[i];

	       for(l = 0; l < 20; l++)
		 {
		   ump_x1[l] = v[0];		 		  		     
		   ump_x1[l] += v[1] * *left++;
		   ump_x1[l] += v[2] * *left++;
		   ump_x1[l] += v[3] * *left++;
		   ump_x1[l] += v[4] * *left++;
		   ump_x1[l] += v[5] * *left++;
		   ump_x1[l] += v[6] * *left++;
		   ump_x1[l] += v[7] * *left++;
		   ump_x1[l] += v[8] * *left++;
		   ump_x1[l] += v[9] * *left++;
		   ump_x1[l] += v[10] * *left++;
		   ump_x1[l] += v[11] * *left++;
		   ump_x1[l] += v[12] * *left++;
		   ump_x1[l] += v[13] * *left++;
		   ump_x1[l] += v[14] * *left++;
		   ump_x1[l] += v[15] * *left++;
		   ump_x1[l] += v[16] * *left++;
		   ump_x1[l] += v[17] * *left++;
		   ump_x1[l] += v[18] * *left++;
		   ump_x1[l] += v[19] * *left++;		       		     
		 }
		
		v = &(tr->protTip[model * 23 + tipX2[i]].v[0]);
		left = &right_start[model * 380 * tr->NumberOfCategories + cat * 380];

		for(l = 0; l < 20; l++)
		  {
		    ump_x2[l] = v[0];		   		  
		    ump_x2[l] += v[1] * *left++;
		    ump_x2[l] += v[2] * *left++;
		    ump_x2[l] += v[3] * *left++;
		    ump_x2[l] += v[4] * *left++;
		    ump_x2[l] += v[5] * *left++;
		    ump_x2[l] += v[6] * *left++;
		    ump_x2[l] += v[7] * *left++;
		    ump_x2[l] += v[8] * *left++;
		    ump_x2[l] += v[9] * *left++;
		    ump_x2[l] += v[10] * *left++;
		    ump_x2[l] += v[11] * *left++;
		    ump_x2[l] += v[12] * *left++;
		    ump_x2[l] += v[13] * *left++;
		    ump_x2[l] += v[14] * *left++;
		    ump_x2[l] += v[15] * *left++;
		    ump_x2[l] += v[16] * *left++;
		    ump_x2[l] += v[17] * *left++;
		    ump_x2[l] += v[18] * *left++;
		    ump_x2[l] += v[19] * *left++;		       		     
		  }	      
		
	       v = x3->v;

	       x1px2 = ump_x1[0] * ump_x2[0];
	       v[0] = x1px2 *  *EV++;	
	       v[1] = x1px2 *  *EV++;
	       v[2] = x1px2 *  *EV++;	
	       v[3] = x1px2 *  *EV++;
	       v[4] = x1px2 *  *EV++;	
	       v[5] = x1px2 *  *EV++;
	       v[6] = x1px2 *  *EV++;	
	       v[7] = x1px2 *  *EV++;
	       v[8] = x1px2 *  *EV++;	
	       v[9] = x1px2 *  *EV++;
	       v[10] = x1px2 *  *EV++;	
	       v[11] = x1px2 *  *EV++;
	       v[12] = x1px2 *  *EV++;	
	       v[13] = x1px2 *  *EV++;
	       v[14] = x1px2 *  *EV++;	
	       v[15] = x1px2 *  *EV++;
	       v[16] = x1px2 *  *EV++;	
	       v[17] = x1px2 *  *EV++;
	       v[18] = x1px2 *  *EV++;	
	       v[19] = x1px2 *  *EV++;
	     

	       for(l = 1; l < 20; l++)
		 {
		   x1px2 = ump_x1[l] * ump_x2[l];
		   
		   v[0] += x1px2 *  *EV++;	
		   v[1] += x1px2 *  *EV++;
		   v[2] += x1px2 *  *EV++;	
		   v[3] += x1px2 *  *EV++;
		   v[4] += x1px2 *  *EV++;	
		   v[5] += x1px2 *  *EV++;
		   v[6] += x1px2 *  *EV++;	
		   v[7] += x1px2 *  *EV++;
		   v[8] += x1px2 *  *EV++;	
		   v[9] += x1px2 *  *EV++;
		   v[10] += x1px2 *  *EV++;	
		   v[11] += x1px2 *  *EV++;
		   v[12] += x1px2 *  *EV++;	
		   v[13] += x1px2 *  *EV++;
		   v[14] += x1px2 *  *EV++;	
		   v[15] += x1px2 *  *EV++;
		   v[16] += x1px2 *  *EV++;	
		   v[17] += x1px2 *  *EV++;
		   v[18] += x1px2 *  *EV++;	
		   v[19] += x1px2 *  *EV++;			 		     
		 }
		   
	       /* NO SCALING AT TIPS */
	       
	       x3->exp = 0;		  	    	   	    	    
	     }	
	   free(left_start); 	  
	   free(right_start);
	   return TRUE;
	 }
          
       if(q->tip)
	 {	
	   char *tipX1 = q->tip;	 
	   x2  = x2_start = (protlikelivector*)r->x;
	  
#pragma omp parallel for private(cat, EV, left, x1, x2, x3, v, x1px2, l, scale,  ump_x1, ump_x2, model)
	   for (i = 0; i < tr->cdta->endsite; i++) 
	     {	
	       model = modelptr[i];
	       cat = cptr[i];
	       EV = &(tr->EV[model * 400]);
	       x1 = &(tr->protTip[model * 23 + tipX1[i]]);     
	       left = &left_start[model * 380 * tr->NumberOfCategories + cat * 380];	    	       
	       x2 = &x2_start[i];
	       x3 = &x3_start[i];

	       v = x1->v;
	     	       
	       for(l = 0; l < 20; l++)
		 {
		   ump_x1[l] = v[0];		 		  		     
		   ump_x1[l] += v[1] * *left++;
		   ump_x1[l] += v[2] * *left++;
		   ump_x1[l] += v[3] * *left++;
		   ump_x1[l] += v[4] * *left++;
		   ump_x1[l] += v[5] * *left++;
		   ump_x1[l] += v[6] * *left++;
		   ump_x1[l] += v[7] * *left++;
		   ump_x1[l] += v[8] * *left++;
		   ump_x1[l] += v[9] * *left++;
		   ump_x1[l] += v[10] * *left++;
		   ump_x1[l] += v[11] * *left++;
		   ump_x1[l] += v[12] * *left++;
		   ump_x1[l] += v[13] * *left++;
		   ump_x1[l] += v[14] * *left++;
		   ump_x1[l] += v[15] * *left++;
		   ump_x1[l] += v[16] * *left++;
		   ump_x1[l] += v[17] * *left++;
		   ump_x1[l] += v[18] * *left++;
		   ump_x1[l] += v[19] * *left++;		       		     
		 }
	       
	       v = x2->v;

	       left = &right_start[model * 380 * tr->NumberOfCategories + cat * 380];
	       for(l = 0; l < 20; l++)
		 {
		   ump_x2[l] = v[0];		   		  
		   ump_x2[l] += v[1] * *left++;
		   ump_x2[l] += v[2] * *left++;
		   ump_x2[l] += v[3] * *left++;
		   ump_x2[l] += v[4] * *left++;
		   ump_x2[l] += v[5] * *left++;
		   ump_x2[l] += v[6] * *left++;
		   ump_x2[l] += v[7] * *left++;
		   ump_x2[l] += v[8] * *left++;
		   ump_x2[l] += v[9] * *left++;
		   ump_x2[l] += v[10] * *left++;
		   ump_x2[l] += v[11] * *left++;
		   ump_x2[l] += v[12] * *left++;
		   ump_x2[l] += v[13] * *left++;
		   ump_x2[l] += v[14] * *left++;
		   ump_x2[l] += v[15] * *left++;
		   ump_x2[l] += v[16] * *left++;
		   ump_x2[l] += v[17] * *left++;
		   ump_x2[l] += v[18] * *left++;
		   ump_x2[l] += v[19] * *left++;		       		     
		 }
	      

	       v = x3->v;

	       x1px2 = ump_x1[0] * ump_x2[0];
	       v[0] = x1px2 *  *EV++;	
	       v[1] = x1px2 *  *EV++;
	       v[2] = x1px2 *  *EV++;	
	       v[3] = x1px2 *  *EV++;
	       v[4] = x1px2 *  *EV++;	
	       v[5] = x1px2 *  *EV++;
	       v[6] = x1px2 *  *EV++;	
	       v[7] = x1px2 *  *EV++;
	       v[8] = x1px2 *  *EV++;	
	       v[9] = x1px2 *  *EV++;
	       v[10] = x1px2 *  *EV++;	
	       v[11] = x1px2 *  *EV++;
	       v[12] = x1px2 *  *EV++;	
	       v[13] = x1px2 *  *EV++;
	       v[14] = x1px2 *  *EV++;	
	       v[15] = x1px2 *  *EV++;
	       v[16] = x1px2 *  *EV++;	
	       v[17] = x1px2 *  *EV++;
	       v[18] = x1px2 *  *EV++;	
	       v[19] = x1px2 *  *EV++;
	     

	       for(l = 1; l < 20; l++)
		 {
		   x1px2 = ump_x1[l] * ump_x2[l];
		   
		   v[0] += x1px2 *  *EV++;	
		   v[1] += x1px2 *  *EV++;
		   v[2] += x1px2 *  *EV++;	
		   v[3] += x1px2 *  *EV++;
		   v[4] += x1px2 *  *EV++;	
		   v[5] += x1px2 *  *EV++;
		   v[6] += x1px2 *  *EV++;	
		   v[7] += x1px2 *  *EV++;
		   v[8] += x1px2 *  *EV++;	
		   v[9] += x1px2 *  *EV++;
		   v[10] += x1px2 *  *EV++;	
		   v[11] += x1px2 *  *EV++;
		   v[12] += x1px2 *  *EV++;	
		   v[13] += x1px2 *  *EV++;
		   v[14] += x1px2 *  *EV++;	
		   v[15] += x1px2 *  *EV++;
		   v[16] += x1px2 *  *EV++;	
		   v[17] += x1px2 *  *EV++;
		   v[18] += x1px2 *  *EV++;	
		   v[19] += x1px2 *  *EV++;			 		     
		 }
		   
	       
	       scale = 1;
	       for(l = 0; scale && (l < 20); l++)
		 scale = ((v[l] < minlikelihood) && (v[l] > minusminlikelihood));
	       
	       x3->exp = x2->exp;		  	      	       	       
	     
	       if(scale)
		 {			  
		   for(l = 0; l < 20; l++)
		     v[l] *= twotothe256;		   
		   x3->exp += 1;
		 }	                   
	     }     
	  
	   free(left_start);	   
	   free(right_start);
	   return TRUE;
	 }
       
       x1  = x1_start = (protlikelivector*)q->x;
       x2  = x2_start = (protlikelivector*)r->x;                 
#pragma omp parallel for private(cat, EV, left, x1, x2, x3, v, x1px2, l, scale,  ump_x1, ump_x2, model)
       for (i = 0; i < tr->cdta->endsite; i++) 
	 {
	   model = modelptr[i];
	   cat = cptr[i];	 
	   x1 = &x1_start[i];
	   x2 = &x2_start[i];
	   x3 = &x3_start[i];

	   v = x1->v;   	   
	   left = &left_start[model * 380 * tr->NumberOfCategories + cat * 380];
	   
	   for(l = 0; l < 20; l++)
	     {
	       ump_x1[l] = v[0];		 		  		     
	       ump_x1[l] += v[1] * *left++;
	       ump_x1[l] += v[2] * *left++;
	       ump_x1[l] += v[3] * *left++;
	       ump_x1[l] += v[4] * *left++;
	       ump_x1[l] += v[5] * *left++;
	       ump_x1[l] += v[6] * *left++;
	       ump_x1[l] += v[7] * *left++;
	       ump_x1[l] += v[8] * *left++;
	       ump_x1[l] += v[9] * *left++;
	       ump_x1[l] += v[10] * *left++;
	       ump_x1[l] += v[11] * *left++;
	       ump_x1[l] += v[12] * *left++;
	       ump_x1[l] += v[13] * *left++;
	       ump_x1[l] += v[14] * *left++;
	       ump_x1[l] += v[15] * *left++;
	       ump_x1[l] += v[16] * *left++;
	       ump_x1[l] += v[17] * *left++;
	       ump_x1[l] += v[18] * *left++;
	       ump_x1[l] += v[19] * *left++;		       		     
	     }
	       
	   v = x2->v;	   	    
	   left = &right_start[model * 380 * tr->NumberOfCategories + cat * 380];

	   for(l = 0; l < 20; l++)
	     {
	       ump_x2[l] = v[0];		   		  
	       ump_x2[l] += v[1] * *left++;
	       ump_x2[l] += v[2] * *left++;
	       ump_x2[l] += v[3] * *left++;
	       ump_x2[l] += v[4] * *left++;
	       ump_x2[l] += v[5] * *left++;
	       ump_x2[l] += v[6] * *left++;
	       ump_x2[l] += v[7] * *left++;
	       ump_x2[l] += v[8] * *left++;
	       ump_x2[l] += v[9] * *left++;
	       ump_x2[l] += v[10] * *left++;
	       ump_x2[l] += v[11] * *left++;
	       ump_x2[l] += v[12] * *left++;
	       ump_x2[l] += v[13] * *left++;
	       ump_x2[l] += v[14] * *left++;
	       ump_x2[l] += v[15] * *left++;
	       ump_x2[l] += v[16] * *left++;
	       ump_x2[l] += v[17] * *left++;
	       ump_x2[l] += v[18] * *left++;
	       ump_x2[l] += v[19] * *left++;		       		     
	     }
	   
	   EV = &tr->EV[model * 400];
	   v = x3->v;
	   
	   x1px2 = ump_x1[0] * ump_x2[0];
	   v[0] = x1px2 *  *EV++;	
	   v[1] = x1px2 *  *EV++;
	   v[2] = x1px2 *  *EV++;	
	   v[3] = x1px2 *  *EV++;
	   v[4] = x1px2 *  *EV++;	
	   v[5] = x1px2 *  *EV++;
	   v[6] = x1px2 *  *EV++;	
	   v[7] = x1px2 *  *EV++;
	   v[8] = x1px2 *  *EV++;	
	   v[9] = x1px2 *  *EV++;
	   v[10] = x1px2 *  *EV++;	
	   v[11] = x1px2 *  *EV++;
	   v[12] = x1px2 *  *EV++;	
	   v[13] = x1px2 *  *EV++;
	   v[14] = x1px2 *  *EV++;	
	   v[15] = x1px2 *  *EV++;
	   v[16] = x1px2 *  *EV++;	
	   v[17] = x1px2 *  *EV++;
	   v[18] = x1px2 *  *EV++;	
	   v[19] = x1px2 *  *EV++;
	   
	   
	   for(l = 1; l < 20; l++)
	     {
	       x1px2 = ump_x1[l] * ump_x2[l];
	       
	       v[0] += x1px2 *  *EV++;	
	       v[1] += x1px2 *  *EV++;
	       v[2] += x1px2 *  *EV++;	
	       v[3] += x1px2 *  *EV++;
	       v[4] += x1px2 *  *EV++;	
	       v[5] += x1px2 *  *EV++;
	       v[6] += x1px2 *  *EV++;	
	       v[7] += x1px2 *  *EV++;
	       v[8] += x1px2 *  *EV++;	
	       v[9] += x1px2 *  *EV++;
	       v[10] += x1px2 *  *EV++;	
	       v[11] += x1px2 *  *EV++;
	       v[12] += x1px2 *  *EV++;	
	       v[13] += x1px2 *  *EV++;
	       v[14] += x1px2 *  *EV++;	
	       v[15] += x1px2 *  *EV++;
	       v[16] += x1px2 *  *EV++;	
	       v[17] += x1px2 *  *EV++;
	       v[18] += x1px2 *  *EV++;	
	       v[19] += x1px2 *  *EV++;			 		     
	     }	 
	   
	   scale = 1;
	   for(l = 0; scale && (l < 20); l++)
	     scale = ((v[l] < minlikelihood) && (v[l] > minusminlikelihood));

	   x3->exp = x1->exp + x2->exp;		  
	   
	   if(scale)	
	     {	 	       
	       for(l = 0; l < 20; l++)
		 v[l] *= twotothe256;
	       x3->exp += 1;
	     }	      	     
	 }     	  
    
       free(left_start); 	
       free(right_start);
       return TRUE;
     }
  }
}



/************************ LIKELIHOOD OF SINGLE PARTITION ********************************************************************************************/

/* FUNCTIONS BELOW CURRENTLY NOOT USED

boolean newviewPARTITIONGTRCATPROTMULT(tree *tr, nodeptr  p, int model)
{   
  if(p->tip) return TRUE;

  { 	
    nodeptr q, r;   

    q = p->next->back;
    r = p->next->next->back; 

    if(r->tip && q->tip)
      {	  	 	       
       while (! p->x)
	 {	   
	   if (! p->x) if (! getxnode(p)) return FALSE; 	     
	 }
      }  
    else
      {
	if(r->tip || q->tip)
	  {		
	    nodeptr tmp;

	    if(r->tip)
	      {
		tmp = r;
		r = q;
		q = tmp;
	      }
	    
	    while ((! p->x) || (! r->x)) 
	      {	 
		if (! r->x) if (! newviewPARTITIONGTRCATPROTMULT(tr, r, model)) return FALSE;		  
		if (! p->x) if (! getxnode(p)) return FALSE;		       		 
	      }	   
	  }
	else
	  {
	    while ((! p->x) || (! q->x) || (! r->x)) 
	      {
		if (! q->x) if (! newviewPARTITIONGTRCATPROTMULT(tr, q, model)) return FALSE;		  
		if (! r->x) if (! newviewPARTITIONGTRCATPROTMULT(tr, r, model)) return FALSE;		  
		if (! p->x) if (! getxnode(p)) return FALSE;			  
	      }
	  }
      }
    
     {
       double  *left, *left_start, *right, *right_start, d1[19], d2[19];
       double *EIGN = &(tr->EIGN[model * 19]), *EV;
       double      
	 ump_x1[20], 
	 ump_x2[20], x1px2, z1, z2, ki;
       int i, l, cat, *cptr, scale;
       int lower, upper;
       double  *rptr, lz1[19], lz2[19], val, *v;
       protlikelivector *x1, *x2, *x3;   

       lower = tr->modelIndices[model][0];
       upper = tr->modelIndices[model][1];  

       x3 = (protlikelivector*)p->x;
       x3 = &x3[lower]; 	      
       
       rptr   = &(tr->cdta->patrat[0]); 
       cptr  = &(tr->cdta->rateCategory[lower]);          
    
       z1  = q->z;
       z1 = (z1 > zmin) ? log(z1) : log(zmin);
       
       z2  = r->z;
       z2 = (z2 > zmin) ? log(z2) : log(zmin);  
       
       left_start = left = (double *)malloc(380 * tr->NumberOfCategories * sizeof(double));
       right_start = right = (double *)malloc(380 * tr->NumberOfCategories * sizeof(double));
                    
       lz1[0] = EIGN[0] * z1;
       lz1[1] = EIGN[1] * z1;
       lz1[2] = EIGN[2] * z1;
       lz1[3] = EIGN[3] * z1;
       lz1[4] = EIGN[4] * z1;
       lz1[5] = EIGN[5] * z1;
       lz1[6] = EIGN[6] * z1;
       lz1[7] = EIGN[7] * z1;
       lz1[8] = EIGN[8] * z1;
       lz1[9] = EIGN[9] * z1;
       lz1[10] = EIGN[10] * z1;
       lz1[11] = EIGN[11] * z1;
       lz1[12] = EIGN[12] * z1;
       lz1[13] = EIGN[13] * z1;
       lz1[14] = EIGN[14] * z1;
       lz1[15] = EIGN[15] * z1;
       lz1[16] = EIGN[16] * z1;
       lz1[17] = EIGN[17] * z1;
       lz1[18] = EIGN[18] * z1;       
      
       lz2[0] = EIGN[0] * z2;
       lz2[1] = EIGN[1] * z2;
       lz2[2] = EIGN[2] * z2;
       lz2[3] = EIGN[3] * z2;
       lz2[4] = EIGN[4] * z2;
       lz2[5] = EIGN[5] * z2;
       lz2[6] = EIGN[6] * z2;
       lz2[7] = EIGN[7] * z2;
       lz2[8] = EIGN[8] * z2;
       lz2[9] = EIGN[9] * z2;
       lz2[10] = EIGN[10] * z2;
       lz2[11] = EIGN[11] * z2;
       lz2[12] = EIGN[12] * z2;
       lz2[13] = EIGN[13] * z2;
       lz2[14] = EIGN[14] * z2;
       lz2[15] = EIGN[15] * z2;
       lz2[16] = EIGN[16] * z2;
       lz2[17] = EIGN[17] * z2;
       lz2[18] = EIGN[18] * z2;
        
	
     
       for(i = 0; i < tr->NumberOfCategories; i++)
	 {	
	   ki = *rptr++;
	   	   	   
	   d1[0] = exp (ki * lz1[0]);
	   d1[1] = exp (ki * lz1[1]);
	   d1[2] = exp (ki * lz1[2]);
	   d1[3] = exp (ki * lz1[3]);
	   d1[4] = exp (ki * lz1[4]);
	   d1[5] = exp (ki * lz1[5]);
	   d1[6] = exp (ki * lz1[6]);
	   d1[7] = exp (ki * lz1[7]);
	   d1[8] = exp (ki * lz1[8]);
	   d1[9] = exp (ki * lz1[9]);
	   d1[10] = exp (ki * lz1[10]);
	   d1[11] = exp (ki * lz1[11]);
	   d1[12] = exp (ki * lz1[12]);
	   d1[13] = exp (ki * lz1[13]);
	   d1[14] = exp (ki * lz1[14]);
	   d1[15] = exp (ki * lz1[15]);
	   d1[16] = exp (ki * lz1[16]);
	   d1[17] = exp (ki * lz1[17]);
	   d1[18] = exp (ki * lz1[18]);
	   
	   d2[0] = exp (ki * lz2[0]);
	   d2[1] = exp (ki * lz2[1]);
	   d2[2] = exp (ki * lz2[2]);
	   d2[3] = exp (ki * lz2[3]);
	   d2[4] = exp (ki * lz2[4]);
	   d2[5] = exp (ki * lz2[5]);
	   d2[6] = exp (ki * lz2[6]);
	   d2[7] = exp (ki * lz2[7]);
	   d2[8] = exp (ki * lz2[8]);
	   d2[9] = exp (ki * lz2[9]);
	   d2[10] = exp (ki * lz2[10]);
	   d2[11] = exp (ki * lz2[11]);
	   d2[12] = exp (ki * lz2[12]);
	   d2[13] = exp (ki * lz2[13]);
	   d2[14] = exp (ki * lz2[14]);
	   d2[15] = exp (ki * lz2[15]);
	   d2[16] = exp (ki * lz2[16]);
	   d2[17] = exp (ki * lz2[17]);
	   d2[18] = exp (ki * lz2[18]);	   	      	 
	   	   	   	   
	   EV = &(tr->EI[model * 380]);
	   for(l = 0; l < 20; l++)
	     {	       
	       *left++ = d1[0] * *EV++;	     	     		   	      
	       *left++ = d1[1] * *EV++;	 
	       *left++ = d1[2] * *EV++;	     	     		   	      
	       *left++ = d1[3] * *EV++;	 
	       *left++ = d1[4] * *EV++;	     	     		   	      
	       *left++ = d1[5] * *EV++;	 
	       *left++ = d1[6] * *EV++;	     	     		   	      
	       *left++ = d1[7] * *EV++;
	       *left++ = d1[8] * *EV++;	     	     		   	      
	       *left++ = d1[9] * *EV++;	 
	       *left++ = d1[10] * *EV++;	     	     		   	      
	       *left++ = d1[11] * *EV++;	 
	       *left++ = d1[12] * *EV++;	     	     		   	      
	       *left++ = d1[13] * *EV++;	 
	       *left++ = d1[14] * *EV++;	     	     		   	      
	       *left++ = d1[15] * *EV++;
	       *left++ = d1[16] * *EV++;	     	     		   	      
	       *left++ = d1[17] * *EV++;	 
	       *left++ = d1[18] * *EV++;	     	     		   	      	      
	     }

	   EV = &(tr->EI[model * 380]);	   	
	   for(l = 0; l < 20; l++)
	     {
	       *right++ = d2[0] * *EV++;	     	     		   	      
	       *right++ = d2[1] * *EV++;	 
	       *right++ = d2[2] * *EV++;	     	     		   	      
	       *right++ = d2[3] * *EV++;	 
	       *right++ = d2[4] * *EV++;	     	     		   	      
	       *right++ = d2[5] * *EV++;	 
	       *right++ = d2[6] * *EV++;	     	     		   	      
	       *right++ = d2[7] * *EV++;
	       *right++ = d2[8] * *EV++;	     	     		   	      
	       *right++ = d2[9] * *EV++;	 
	       *right++ = d2[10] * *EV++;	     	     		   	      
	       *right++ = d2[11] * *EV++;	 
	       *right++ = d2[12] * *EV++;	     	     		   	      
	       *right++ = d2[13] * *EV++;	 
	       *right++ = d2[14] * *EV++;	     	     		   	      
	       *right++ = d2[15] * *EV++;
	       *right++ = d2[16] * *EV++;	     	     		   	      
	       *right++ = d2[17] * *EV++;	 
	       *right++ = d2[18] * *EV++;    	     		   	      
	     }	  
	 }
   
       if(r->tip && q->tip)
	 {	  	 	 
	   char *tipX1 = &q->tip[lower];
	   char *tipX2 = &r->tip[lower];	   	   	        
	   
	   for (i = lower; i < upper; i++) 
	     {		   	    	   	   
	       cat = *cptr++;
	       EV = &(tr->EV[model * 400]);	       	   
	       left = &left_start[cat * 380];	      	       
	       v = &(tr->protTip[model * 23 + *tipX1++].v[0]);	     	       	       

	       for(l = 0; l < 20; l++)
		 {
		   ump_x1[l] = v[0];		 		  		     
		   ump_x1[l] += v[1] * *left++;
		   ump_x1[l] += v[2] * *left++;
		   ump_x1[l] += v[3] * *left++;
		   ump_x1[l] += v[4] * *left++;
		   ump_x1[l] += v[5] * *left++;
		   ump_x1[l] += v[6] * *left++;
		   ump_x1[l] += v[7] * *left++;
		   ump_x1[l] += v[8] * *left++;
		   ump_x1[l] += v[9] * *left++;
		   ump_x1[l] += v[10] * *left++;
		   ump_x1[l] += v[11] * *left++;
		   ump_x1[l] += v[12] * *left++;
		   ump_x1[l] += v[13] * *left++;
		   ump_x1[l] += v[14] * *left++;
		   ump_x1[l] += v[15] * *left++;
		   ump_x1[l] += v[16] * *left++;
		   ump_x1[l] += v[17] * *left++;
		   ump_x1[l] += v[18] * *left++;
		   ump_x1[l] += v[19] * *left++;		       		     
		 }
		
		v = &(tr->protTip[model * 23 + *tipX2++].v[0]);

		left = &right_start[cat * 380];
		for(l = 0; l < 20; l++)
		  {
		    ump_x2[l] = v[0];		   		  
		    ump_x2[l] += v[1] * *left++;
		    ump_x2[l] += v[2] * *left++;
		    ump_x2[l] += v[3] * *left++;
		    ump_x2[l] += v[4] * *left++;
		    ump_x2[l] += v[5] * *left++;
		    ump_x2[l] += v[6] * *left++;
		    ump_x2[l] += v[7] * *left++;
		    ump_x2[l] += v[8] * *left++;
		    ump_x2[l] += v[9] * *left++;
		    ump_x2[l] += v[10] * *left++;
		    ump_x2[l] += v[11] * *left++;
		    ump_x2[l] += v[12] * *left++;
		    ump_x2[l] += v[13] * *left++;
		    ump_x2[l] += v[14] * *left++;
		    ump_x2[l] += v[15] * *left++;
		    ump_x2[l] += v[16] * *left++;
		    ump_x2[l] += v[17] * *left++;
		    ump_x2[l] += v[18] * *left++;
		    ump_x2[l] += v[19] * *left++;		       		     
		  }	      
		
	       v = x3->v;

	       x1px2 = ump_x1[0] * ump_x2[0];
	       v[0] = x1px2 *  *EV++;	
	       v[1] = x1px2 *  *EV++;
	       v[2] = x1px2 *  *EV++;	
	       v[3] = x1px2 *  *EV++;
	       v[4] = x1px2 *  *EV++;	
	       v[5] = x1px2 *  *EV++;
	       v[6] = x1px2 *  *EV++;	
	       v[7] = x1px2 *  *EV++;
	       v[8] = x1px2 *  *EV++;	
	       v[9] = x1px2 *  *EV++;
	       v[10] = x1px2 *  *EV++;	
	       v[11] = x1px2 *  *EV++;
	       v[12] = x1px2 *  *EV++;	
	       v[13] = x1px2 *  *EV++;
	       v[14] = x1px2 *  *EV++;	
	       v[15] = x1px2 *  *EV++;
	       v[16] = x1px2 *  *EV++;	
	       v[17] = x1px2 *  *EV++;
	       v[18] = x1px2 *  *EV++;	
	       v[19] = x1px2 *  *EV++;
	     

	       for(l = 1; l < 20; l++)
		 {
		   x1px2 = ump_x1[l] * ump_x2[l];
		   
		   v[0] += x1px2 *  *EV++;	
		   v[1] += x1px2 *  *EV++;
		   v[2] += x1px2 *  *EV++;	
		   v[3] += x1px2 *  *EV++;
		   v[4] += x1px2 *  *EV++;	
		   v[5] += x1px2 *  *EV++;
		   v[6] += x1px2 *  *EV++;	
		   v[7] += x1px2 *  *EV++;
		   v[8] += x1px2 *  *EV++;	
		   v[9] += x1px2 *  *EV++;
		   v[10] += x1px2 *  *EV++;	
		   v[11] += x1px2 *  *EV++;
		   v[12] += x1px2 *  *EV++;	
		   v[13] += x1px2 *  *EV++;
		   v[14] += x1px2 *  *EV++;	
		   v[15] += x1px2 *  *EV++;
		   v[16] += x1px2 *  *EV++;	
		   v[17] += x1px2 *  *EV++;
		   v[18] += x1px2 *  *EV++;	
		   v[19] += x1px2 *  *EV++;			 		     
		 }		   	     
	       
	       x3->exp = 0;		  	    	   	    
	       x3++;
	     }

	   free(left_start); 	  
	   free(right_start);
	   return TRUE;
	 }
          
       if(q->tip)
	 {	
	   char *tipX1 = &(q->tip[lower]);	 
	   x2 = (protlikelivector*)r->x;
	   x2 = &x2[lower];
	 
	   for (i = lower; i < upper; i++) 
	     {			     
	       cat = *cptr++;
	       EV = &(tr->EV[model * 400]);
	       x1 = &(tr->protTip[23 * model + *tipX1++]);     
	       left = &left_start[cat * 380];	    
	       
	       v = x1->v;
	     	       
	       for(l = 0; l < 20; l++)
		 {
		   ump_x1[l] = v[0];		 		  		     
		   ump_x1[l] += v[1] * *left++;
		   ump_x1[l] += v[2] * *left++;
		   ump_x1[l] += v[3] * *left++;
		   ump_x1[l] += v[4] * *left++;
		   ump_x1[l] += v[5] * *left++;
		   ump_x1[l] += v[6] * *left++;
		   ump_x1[l] += v[7] * *left++;
		   ump_x1[l] += v[8] * *left++;
		   ump_x1[l] += v[9] * *left++;
		   ump_x1[l] += v[10] * *left++;
		   ump_x1[l] += v[11] * *left++;
		   ump_x1[l] += v[12] * *left++;
		   ump_x1[l] += v[13] * *left++;
		   ump_x1[l] += v[14] * *left++;
		   ump_x1[l] += v[15] * *left++;
		   ump_x1[l] += v[16] * *left++;
		   ump_x1[l] += v[17] * *left++;
		   ump_x1[l] += v[18] * *left++;
		   ump_x1[l] += v[19] * *left++;		       		     
		 }
	       
	       v = x2->v;

	       left = &right_start[cat * 380];
	       for(l = 0; l < 20; l++)
		 {
		   ump_x2[l] = v[0];		   		  
		   ump_x2[l] += v[1] * *left++;
		   ump_x2[l] += v[2] * *left++;
		   ump_x2[l] += v[3] * *left++;
		   ump_x2[l] += v[4] * *left++;
		   ump_x2[l] += v[5] * *left++;
		   ump_x2[l] += v[6] * *left++;
		   ump_x2[l] += v[7] * *left++;
		   ump_x2[l] += v[8] * *left++;
		   ump_x2[l] += v[9] * *left++;
		   ump_x2[l] += v[10] * *left++;
		   ump_x2[l] += v[11] * *left++;
		   ump_x2[l] += v[12] * *left++;
		   ump_x2[l] += v[13] * *left++;
		   ump_x2[l] += v[14] * *left++;
		   ump_x2[l] += v[15] * *left++;
		   ump_x2[l] += v[16] * *left++;
		   ump_x2[l] += v[17] * *left++;
		   ump_x2[l] += v[18] * *left++;
		   ump_x2[l] += v[19] * *left++;		       		     
		 }
	      

	       v = x3->v;

	       x1px2 = ump_x1[0] * ump_x2[0];
	       v[0] = x1px2 *  *EV++;	
	       v[1] = x1px2 *  *EV++;
	       v[2] = x1px2 *  *EV++;	
	       v[3] = x1px2 *  *EV++;
	       v[4] = x1px2 *  *EV++;	
	       v[5] = x1px2 *  *EV++;
	       v[6] = x1px2 *  *EV++;	
	       v[7] = x1px2 *  *EV++;
	       v[8] = x1px2 *  *EV++;	
	       v[9] = x1px2 *  *EV++;
	       v[10] = x1px2 *  *EV++;	
	       v[11] = x1px2 *  *EV++;
	       v[12] = x1px2 *  *EV++;	
	       v[13] = x1px2 *  *EV++;
	       v[14] = x1px2 *  *EV++;	
	       v[15] = x1px2 *  *EV++;
	       v[16] = x1px2 *  *EV++;	
	       v[17] = x1px2 *  *EV++;
	       v[18] = x1px2 *  *EV++;	
	       v[19] = x1px2 *  *EV++;
	     

	       for(l = 1; l < 20; l++)
		 {
		   x1px2 = ump_x1[l] * ump_x2[l];
		   
		   v[0] += x1px2 *  *EV++;	
		   v[1] += x1px2 *  *EV++;
		   v[2] += x1px2 *  *EV++;	
		   v[3] += x1px2 *  *EV++;
		   v[4] += x1px2 *  *EV++;	
		   v[5] += x1px2 *  *EV++;
		   v[6] += x1px2 *  *EV++;	
		   v[7] += x1px2 *  *EV++;
		   v[8] += x1px2 *  *EV++;	
		   v[9] += x1px2 *  *EV++;
		   v[10] += x1px2 *  *EV++;	
		   v[11] += x1px2 *  *EV++;
		   v[12] += x1px2 *  *EV++;	
		   v[13] += x1px2 *  *EV++;
		   v[14] += x1px2 *  *EV++;	
		   v[15] += x1px2 *  *EV++;
		   v[16] += x1px2 *  *EV++;	
		   v[17] += x1px2 *  *EV++;
		   v[18] += x1px2 *  *EV++;	
		   v[19] += x1px2 *  *EV++;			 		     
		 }
		   
	       
	       scale = 1;
	       for(l = 0; scale && (l < 20); l++)
		 scale = ((v[l] < minlikelihood) && (v[l] > minusminlikelihood));
	       
	       x3->exp = x2->exp;		  	      	       	       
	     
	       if(scale)
		 {			  
		   for(l = 0; l < 20; l++)
		     v[l] *= twotothe256;		   
		   x3->exp += 1;
		 }
	       
	       x2++;
	       x3++;	       
	     }     
	 
	   free(left_start);	   
	   free(right_start);
	   return TRUE;
	 }
       
       x1  = (protlikelivector*)q->x;
       x2  = (protlikelivector*)r->x;

       x1 = &x1[lower];
       x2 = &x2[lower];
             
       for (i = lower; i < upper; i++) 
	 {		 	  
	   cat = *cptr++;	 
	   v = x1->v;   	   
	   left = &left_start[cat * 380];
	   
	   for(l = 0; l < 20; l++)
	     {
	       ump_x1[l] = v[0];		 		  		     
	       ump_x1[l] += v[1] * *left++;
	       ump_x1[l] += v[2] * *left++;
	       ump_x1[l] += v[3] * *left++;
	       ump_x1[l] += v[4] * *left++;
	       ump_x1[l] += v[5] * *left++;
	       ump_x1[l] += v[6] * *left++;
	       ump_x1[l] += v[7] * *left++;
	       ump_x1[l] += v[8] * *left++;
	       ump_x1[l] += v[9] * *left++;
	       ump_x1[l] += v[10] * *left++;
	       ump_x1[l] += v[11] * *left++;
	       ump_x1[l] += v[12] * *left++;
	       ump_x1[l] += v[13] * *left++;
	       ump_x1[l] += v[14] * *left++;
	       ump_x1[l] += v[15] * *left++;
	       ump_x1[l] += v[16] * *left++;
	       ump_x1[l] += v[17] * *left++;
	       ump_x1[l] += v[18] * *left++;
	       ump_x1[l] += v[19] * *left++;		       		     
	     }
	       
	   v = x2->v;	   	    
	   left = &right_start[cat * 380];

	   for(l = 0; l < 20; l++)
	     {
	       ump_x2[l] = v[0];		   		  
	       ump_x2[l] += v[1] * *left++;
	       ump_x2[l] += v[2] * *left++;
	       ump_x2[l] += v[3] * *left++;
	       ump_x2[l] += v[4] * *left++;
	       ump_x2[l] += v[5] * *left++;
	       ump_x2[l] += v[6] * *left++;
	       ump_x2[l] += v[7] * *left++;
	       ump_x2[l] += v[8] * *left++;
	       ump_x2[l] += v[9] * *left++;
	       ump_x2[l] += v[10] * *left++;
	       ump_x2[l] += v[11] * *left++;
	       ump_x2[l] += v[12] * *left++;
	       ump_x2[l] += v[13] * *left++;
	       ump_x2[l] += v[14] * *left++;
	       ump_x2[l] += v[15] * *left++;
	       ump_x2[l] += v[16] * *left++;
	       ump_x2[l] += v[17] * *left++;
	       ump_x2[l] += v[18] * *left++;
	       ump_x2[l] += v[19] * *left++;		       		     
	     }
	   
	   EV = &(tr->EV[model * 400]);
	   v = x3->v;
	   
	   x1px2 = ump_x1[0] * ump_x2[0];
	   v[0] = x1px2 *  *EV++;	
	   v[1] = x1px2 *  *EV++;
	   v[2] = x1px2 *  *EV++;	
	   v[3] = x1px2 *  *EV++;
	   v[4] = x1px2 *  *EV++;	
	   v[5] = x1px2 *  *EV++;
	   v[6] = x1px2 *  *EV++;	
	   v[7] = x1px2 *  *EV++;
	   v[8] = x1px2 *  *EV++;	
	   v[9] = x1px2 *  *EV++;
	   v[10] = x1px2 *  *EV++;	
	   v[11] = x1px2 *  *EV++;
	   v[12] = x1px2 *  *EV++;	
	   v[13] = x1px2 *  *EV++;
	   v[14] = x1px2 *  *EV++;	
	   v[15] = x1px2 *  *EV++;
	   v[16] = x1px2 *  *EV++;	
	   v[17] = x1px2 *  *EV++;
	   v[18] = x1px2 *  *EV++;	
	   v[19] = x1px2 *  *EV++;
	   
	   
	   for(l = 1; l < 20; l++)
	     {
	       x1px2 = ump_x1[l] * ump_x2[l];
	       
	       v[0] += x1px2 *  *EV++;	
	       v[1] += x1px2 *  *EV++;
	       v[2] += x1px2 *  *EV++;	
	       v[3] += x1px2 *  *EV++;
	       v[4] += x1px2 *  *EV++;	
	       v[5] += x1px2 *  *EV++;
	       v[6] += x1px2 *  *EV++;	
	       v[7] += x1px2 *  *EV++;
	       v[8] += x1px2 *  *EV++;	
	       v[9] += x1px2 *  *EV++;
	       v[10] += x1px2 *  *EV++;	
	       v[11] += x1px2 *  *EV++;
	       v[12] += x1px2 *  *EV++;	
	       v[13] += x1px2 *  *EV++;
	       v[14] += x1px2 *  *EV++;	
	       v[15] += x1px2 *  *EV++;
	       v[16] += x1px2 *  *EV++;	
	       v[17] += x1px2 *  *EV++;
	       v[18] += x1px2 *  *EV++;	
	       v[19] += x1px2 *  *EV++;			 		     
	     }	 
	   
	   scale = 1;
	   for(l = 0; scale && (l < 20); l++)
	     scale = ((v[l] < minlikelihood) && (v[l] > minusminlikelihood));

	   x3->exp = x1->exp + x2->exp;		  
	   
	   if(scale)	
	     {	 	       
	       for(l = 0; l < 20; l++)
		 v[l] *= twotothe256;
	       x3->exp += 1;
	     }	      
	   x1++;
	   x2++;
	   x3++;	     
	 }     	  
       free(left_start); 	
       free(right_start);
       return TRUE;
     }
  }
}



double evaluateGTRCATPROTMULTPARTITION (tree *tr, nodeptr p, int model)
  {
    double   sum, z, lz, ki, lza[19];    
    nodeptr  q;
    int     i, l;
    int     *wptr, *cptr;    
    double  *diagptable, *rptr, *diagptable_start;
    double *EIGN = &(tr->EIGN[19 * model]);
    protlikelivector   *x1, *x2;       
    int lower, upper;

    lower = tr->modelIndices[model][0];
    upper = tr->modelIndices[model][1];  

    q = p->back;
    
    rptr   = &(tr->cdta->patrat[0]); 
    cptr  = &(tr->cdta->rateCategory[lower]);
    wptr = &(tr->cdta->aliaswgt[lower]);
   
    z = p->z;
   
    if (z < zmin) z = zmin;
    lz = log(z);

    for(l = 0; l < 19; l++)      
      lza[l] = EIGN[l] * lz;     

    diagptable = diagptable_start = (double *)malloc(sizeof(double) * tr->NumberOfCategories * 19);    
 
    for(i = 0; i <  tr->NumberOfCategories; i++)
      {	
	ki = *rptr++;	 
       
	*diagptable++ = exp (ki * lza[0]);	   
	*diagptable++ = exp (ki * lza[1]);
	*diagptable++ = exp (ki * lza[2]);	   
	*diagptable++ = exp (ki * lza[3]);
	*diagptable++ = exp (ki * lza[4]);	   
	*diagptable++ = exp (ki * lza[5]);
	*diagptable++ = exp (ki * lza[6]);	   
	*diagptable++ = exp (ki * lza[7]);
	*diagptable++ = exp (ki * lza[8]);	   
	*diagptable++ = exp (ki * lza[9]);
	*diagptable++ = exp (ki * lza[10]);	   
	*diagptable++ = exp (ki * lza[11]);
	*diagptable++ = exp (ki * lza[12]);	   
	*diagptable++ = exp (ki * lza[13]);
	*diagptable++ = exp (ki * lza[14]);	   
	*diagptable++ = exp (ki * lza[15]);
	*diagptable++ = exp (ki * lza[16]);	   
	*diagptable++ = exp (ki * lza[17]);
	*diagptable++ = exp (ki * lza[18]);	          
      }
	    
    sum = 0.0;   

    if(p->tip || q->tip)
      {
	char *tipX1;	
	nodeptr tmp; 
	    
	if(q->tip)
	  {	
	    tmp = p;
	    p = q;
	    q = tmp;	      
	  }

	tipX1 = &(p->tip[lower]);      

	while ((! q->x)) 
	  {	   
	    if (! (q->x)) if (! newviewPARTITIONGTRCATPROTMULT(tr, q, model)) return badEval;
	  }

	x2  = (protlikelivector*)q->x;
	x2 = &x2[lower];

	for (i = lower; i < upper; i++) 
	  {	    
	    int cat;
	    double term;
	    double *left, *right;
	    cat = *cptr++;	  
	    x1 = &(tr->protTip[23 * model + *tipX1++]);

	    diagptable = &diagptable_start[19 * cat];
	    
	    left = x1->v;
	    right = x2->v;
	   
	    term =  left[0] * right[0];
	    term += left[1] * right[1] * *diagptable++; 
	    term += left[2] * right[2] * *diagptable++; 
	    term += left[3] * right[3] * *diagptable++; 
	    term += left[4] * right[4] * *diagptable++;	   
	    term += left[5] * right[5] * *diagptable++; 
	    term += left[6] * right[6] * *diagptable++; 
	    term += left[7] * right[7] * *diagptable++; 
	    term += left[8] * right[8] * *diagptable++;
	    term += left[9] * right[9] * *diagptable++; 	    
	    term += left[10] * right[10] * *diagptable++; 
	    term += left[11] * right[11] * *diagptable++; 
	    term += left[12] * right[12] * *diagptable++; 
	    term += left[13] * right[13] * *diagptable++;
	    term += left[14] * right[14] * *diagptable++; 	    
	    term += left[15] * right[15] * *diagptable++; 
	    term += left[16] * right[16] * *diagptable++; 
	    term += left[17] * right[17] * *diagptable++; 
	    term += left[18] * right[18] * *diagptable++;
	    term += left[19] * right[19] * *diagptable++; 
	    		   
	    term = log(term) + x2->exp * log(minlikelihood);	   
	    sum += *wptr++ * term;		  	    
	    x2++;
	  }

	free(diagptable_start);             
	return  sum;
      }         
      
    while ((! p->x) || (! q->x)) 
      {
	if (! (p->x)) if (! newviewPARTITIONGTRCATPROTMULT(tr, p, model)) return badEval;
	if (! (q->x)) if (! newviewPARTITIONGTRCATPROTMULT(tr, q, model)) return badEval;
      }

    x1  = (protlikelivector*)p->x;
    x2  = (protlikelivector*)q->x;     
    x1 = &x1[lower];
    x2 = &x2[lower];

    for (i = lower; i < upper; i++) 
      {		   
	int cat;
	double term;
	double *left, *right;

	cat = *cptr++;
	diagptable = &diagptable_start[19 * cat];	

	left = x1->v;
	right = x2->v;
	
	term =  left[0] * right[0];
	term += left[1] * right[1] * *diagptable++; 
	term += left[2] * right[2] * *diagptable++; 
	term += left[3] * right[3] * *diagptable++; 
	term += left[4] * right[4] * *diagptable++;	   
	term += left[5] * right[5] * *diagptable++; 
	term += left[6] * right[6] * *diagptable++; 
	term += left[7] * right[7] * *diagptable++; 
	term += left[8] * right[8] * *diagptable++;
	term += left[9] * right[9] * *diagptable++; 	    
	term += left[10] * right[10] * *diagptable++; 
	term += left[11] * right[11] * *diagptable++; 
	term += left[12] * right[12] * *diagptable++; 
	term += left[13] * right[13] * *diagptable++;
	term += left[14] * right[14] * *diagptable++; 	    
	term += left[15] * right[15] * *diagptable++; 
	term += left[16] * right[16] * *diagptable++; 
	term += left[17] * right[17] * *diagptable++; 
	term += left[18] * right[18] * *diagptable++;
	term += left[19] * right[19] * *diagptable++; 

	term = log(term) + (x1->exp + x2->exp) * log(minlikelihood);
	sum += *wptr++ * term;		  
	x1++;
	x2++;
      }

    free(diagptable_start);      
    return  sum;         
  } 

*/
