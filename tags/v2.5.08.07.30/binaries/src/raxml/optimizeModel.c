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

static const double MNBRAK_GOLD =    1.618034;
static const double MNBRAK_TINY =      1.e-20;
static const double MNBRAK_GLIMIT =     100.0;
static const double BRENT_ITMAX =         100;
static const double BRENT_ZEPS  =      1.e-5;
static const double BRENT_CGOLD =   0.3819660;

extern int optimizeRatesInvocations;
extern int optimizeRateCategoryInvocations;
extern int optimizeAlphaInvocations;
extern int optimizeTTRatioInvocations;
extern double masterTime;
extern char ratesFileName[1024];

extern boolean (*newview)           (tree *, nodeptr);
extern double  (*evaluate)          (tree *, nodeptr);   
extern double  (*makenewz)          (tree *, nodeptr, nodeptr, double, int);
extern double  (*evaluatePartial)   (tree *, nodeptr, int, double);
extern double  (*evaluatePartition) (tree *, nodeptr, int);
extern boolean (*newviewPartition)  (tree *, nodeptr, int);

/*********************FUNCTIONS FOOR EXACT MODEL OPTIMIZATION UNDER GTRGAMMA ***************************************/

double evaluateRate(tree *tr, int i, double rate, analdef *adef, int model)
{
  tr->initialRates[model * tr->numberOfRates + i] = rate;

  initReversibleGTR(tr->rdta, tr->cdta, tr, adef, model);

  if(adef->useMultipleModel)
    {
      return -partitionLikelihood(tr, model);
    }
  else
    {
      initrav(tr, tr->start);
      initrav(tr, tr->start->back);
      evaluate(tr, tr->start); 
      
      return (-tr->likelihood);
    }
}

double evaluateAlpha(tree *tr, double alpha, int model, analdef *adef)
{
   tr->alphas[model] = alpha;    
   makeGammaCats(tr, model); 

   if(adef->useMultipleModel)
     {
       return -partitionLikelihood(tr, model);
     }
   else
     {
       initrav(tr, tr->start);
       initrav(tr, tr->start->back);
       evaluate(tr, tr->start);
      
       return (-tr->likelihood);
     }
}





int brakAlpha(double *param, double *ax, double *bx, double *cx, double *fa, double *fb, double *fc, double lim_inf, double lim_sup, tree *tr, int model, analdef *adef)
{
   double ulim,u,r,q,fu,dum;

   u = 0.0;

   *param = *ax;
   if(*param > lim_sup) *param = lim_sup;
   if(*param < lim_inf) *param = lim_inf;
   *fa = evaluateAlpha(tr, *param, model, adef);

   *param = *bx;
   if(*param > lim_sup) *param = lim_sup;
   if(*param < lim_inf) *param = lim_inf;  
   *fb = evaluateAlpha(tr, *param, model, adef);

   if (*fb > *fa) 
     {
       SHFT(dum,*ax,*bx,dum)
       SHFT(dum,*fb,*fa,dum)
     }

   *cx=(*bx)+MNBRAK_GOLD*(*bx-*ax);
   *param = *cx;
   if(*param > lim_sup) *param = *cx = lim_sup;
   if(*param < lim_inf) *param = *cx = lim_inf;
   *fc = evaluateAlpha(tr, *param, model, adef); 

   while (*fb > *fc) 
     {        
       if(*ax > lim_sup) *ax = lim_sup;
       if(*ax < lim_inf) *ax = lim_inf;
       if(*bx > lim_sup) *bx = lim_sup;
       if(*bx < lim_inf) *bx = lim_inf;
       if(*cx > lim_sup) *cx = lim_sup;
       if(*cx < lim_inf) *cx = lim_inf;      

       r=(*bx-*ax)*(*fb-*fc);
       q=(*bx-*cx)*(*fb-*fa);
       u=(*bx)-((*bx-*cx)*q-(*bx-*ax)*r)/
               (2.0*SIGN(MAX(fabs(q-r),MNBRAK_TINY),q-r));
       ulim=(*bx)+MNBRAK_GLIMIT*(*cx-*bx);
       
       if(u > lim_sup) u = lim_sup;
       if(u < lim_inf) u = lim_inf;
       if(ulim > lim_sup) ulim = lim_sup;
       if(ulim < lim_inf) ulim = lim_inf;

       if ((*bx-u)*(u-*cx) > 0.0) 
	 {
	   *param = u;	   
	   fu = evaluateAlpha(tr, *param, model, adef);
	   if (fu < *fc) 
	     {
	       *ax=(*bx);
	       *bx=u;
	       *fa=(*fb);
	       *fb=fu;	      
	       return(0);
	     } 
	   else if (fu > *fb) 
	     {
	       *cx=u;
	       *fc=fu;		       
	       return(0);
	     }
	   u=(*cx)+MNBRAK_GOLD*(*cx-*bx);
	   *param = u;
	   if(*param > lim_sup) {*param = u = lim_sup;}
	   if(*param < lim_inf) {*param = u = lim_inf;}	
	   fu= evaluateAlpha(tr, *param, model, adef);
	 } 
       else if ((*cx-u)*(u-ulim) > 0.0) 
	 {
	   *param = u;	   
	   fu = evaluateAlpha(tr, *param, model, adef);
	   if (fu < *fc) 
	     {
	       SHFT(*bx,*cx,u,*cx+MNBRAK_GOLD*(*cx-*bx))	        	      
	       SHFT(*fb,*fc,fu, evaluateAlpha(tr, *param, model, adef))
	     }
	 } 
       else if ((u-ulim)*(ulim-*cx) >= 0.0) 
	 {
	   u = ulim;
	   *param = u;	    
	   fu = evaluateAlpha(tr, *param, model, adef);
	 } 
       else 
	 {
	   u=(*cx)+MNBRAK_GOLD*(*cx-*bx);
	   *param = u;
	   if(*param > lim_sup) {*param = u = lim_sup;}
	   if(*param < lim_inf) {*param = u = lim_inf;}
	   fu = evaluateAlpha(tr, *param, model, adef);
	 }
       SHFT(*ax,*bx,*cx,u)
       SHFT(*fa,*fb,*fc,fu)


     }
   
   return(0);
}


double brentAlpha(double ax, double bx, double cx, double fa, double fb, double fc, double tol, double *xmin, int model, tree *tr, analdef *adef)
{
  int iter;
  double a,b,d,etemp,fu,fv,fw,fx,p,q,r,tol1,tol2,u,v,w,x,xm;
  double e=0.0;

  a=((ax < cx) ? ax : cx);
  b=((ax > cx) ? ax : cx);
  x = w = v = bx;
  fw = fv = fx = fb;

  for(iter = 1; iter <= ITMAX; iter++)
    {
      xm = 0.5 * (a + b);
      tol2 = 2.0 * (tol1 = tol * fabs(x) + BRENT_ZEPS);
      if(fabs(x - xm) <= (tol2 - 0.5 * (b - a)))
	{
	  *xmin = x;
	  return -fx;
	}
      if(fabs(e) > tol1)
	{
	  r = (x - w) * (fx - fv);
	  q = (x - v) * (fx - fw);
	  p = (x -v ) * q - (x - w) * r;
	  q = 2.0 * (q - r);
	  if(q > 0.0)
	    p = -p;
	  q = fabs(q);
	  etemp = e;
	  e = d;
	  if(fabs(p) >= fabs(0.5 * q * etemp) || p <= q * (a-x) || p >= q * (b - x))
	    d = BRENT_CGOLD * (e = (x >= xm ? a - x : b - x));
	  else
	    {
	      d = p / q;
	      u = x + d;
	      if( u - a < tol2 || b - u < tol2)
		d = SIGN(tol1, xm - x);
	    }
	}
      else
	{
	  d = BRENT_CGOLD * (e = (x >= xm ? a - x: b - x));
	}
      u = (fabs(d) >= tol1 ? x + d: x +SIGN(tol1, d));
      
      fu = evaluateAlpha(tr, u, model, adef);

      if(fu <= fx)
	{
	  if(u >= x)
	    a = x;
	  else
	    b = x;
	  SHFT(v,w,x,u)
	    SHFT(fv,fw,fx,fu)
	    }
      else
	{
	  if(u < x)
	    a = u;
	  else
	    b = u;
	  if(fu <= fw || w == x)
	    {
	      v = w;
	      w = u;
	      fv = fw;
	      fw = fu;
	    }
	  else
	    {
	      if(fu <= fv || v == x || v == w)
		{
		  v = u;
		  fv = fu;
		}
	    }	    
	}
    }

  printf("\n. Too many iterations in BRENT !");
  exit(-1);
  return(-1);

}


double brentRates(double ax, double bx, double cx, double fa, double fb, double fc, double tol, double *xmin, int model, tree *tr, analdef *adef, int i)
{
  int iter;
  double a,b,d,etemp,fu,fv,fw,fx,p,q,r,tol1,tol2,u,v,w,x,xm;
  double e=0.0;

  a=((ax < cx) ? ax : cx);
  b=((ax > cx) ? ax : cx);
  x = w = v = bx;
  fw = fv = fx = fb;

  for(iter = 1; iter <= ITMAX; iter++)
    {
      xm = 0.5 * (a + b);
      tol2 = 2.0 * (tol1 = tol * fabs(x) + BRENT_ZEPS);
      if(fabs(x - xm) <= (tol2 - 0.5 * (b - a)))
	{
	  *xmin = x;
	  return -fx;
	}
      if(fabs(e) > tol1)
	{
	  r = (x - w) * (fx - fv);
	  q = (x - v) * (fx - fw);
	  p = (x -v ) * q - (x - w) * r;
	  q = 2.0 * (q - r);
	  if(q > 0.0)
	    p = -p;
	  q = fabs(q);
	  etemp = e;
	  e = d;
	  if(fabs(p) >= fabs(0.5 * q * etemp) || p <= q * (a-x) || p >= q * (b - x))
	    d = BRENT_CGOLD * (e = (x >= xm ? a - x : b - x));
	  else
	    {
	      d = p / q;
	      u = x + d;
	      if( u - a < tol2 || b - u < tol2)
		d = SIGN(tol1, xm - x);
	    }
	}
      else
	{
	  d = BRENT_CGOLD * (e = (x >= xm ? a - x: b - x));
	}
      u = (fabs(d) >= tol1 ? x + d: x +SIGN(tol1, d));
      
      fu = evaluateRate(tr, i, u, adef, model);

      if(fu <= fx)
	{
	  if(u >= x)
	    a = x;
	  else
	    b = x;
	  SHFT(v,w,x,u)
	    SHFT(fv,fw,fx,fu)
	    }
      else
	{
	  if(u < x)
	    a = u;
	  else
	    b = u;
	  if(fu <= fw || w == x)
	    {
	      v = w;
	      w = u;
	      fv = fw;
	      fw = fu;
	    }
	  else
	    {
	      if(fu <= fv || v == x || v == w)
		{
		  v = u;
		  fv = fu;
		}
	    }	    
	}
    }

  printf("\n. Too many iterations in BRENT !");
  exit(-1);
  return(-1);
}






int brakRates(double *param, double *ax, double *bx, double *cx, double *fa, double *fb, double *fc, double lim_inf, double lim_sup, tree *tr, int i, analdef *adef, int model)
{
   double ulim,u,r,q,fu,dum;

   u = 0.0;

   *param = *ax;

   if(*param > lim_sup) *param = lim_sup;
   if(*param < lim_inf) *param = lim_inf;
   
   *fa = evaluateRate(tr, i, *param, adef, model);

   *param = *bx;
   if(*param > lim_sup) *param = lim_sup;
   if(*param < lim_inf) *param = lim_inf;

   *fb = evaluateRate(tr, i, *param, adef, model);

   if (*fb > *fa) 
     {
       SHFT(dum,*ax,*bx,dum)
       SHFT(dum,*fa,*fb,dum)
     }

   *cx=(*bx)+MNBRAK_GOLD*(*bx-*ax);
   *param = *cx;
   if(*param > lim_sup) *param = *cx = lim_sup;
   if(*param < lim_inf) *param = *cx = lim_inf;
   *fc =  evaluateRate(tr, i, *param, adef, model);

   while (*fb > *fc) 
     {                     
       if(*ax > lim_sup) *ax = lim_sup;
       if(*ax < lim_inf) *ax = lim_inf;
       if(*bx > lim_sup) *bx = lim_sup;
       if(*bx < lim_inf) *bx = lim_inf;
       if(*cx > lim_sup) *cx = lim_sup;
       if(*cx < lim_inf) *cx = lim_inf;
       
       r=(*bx-*ax)*(*fb-*fc);
       q=(*bx-*cx)*(*fb-*fa);
       u=(*bx)-((*bx-*cx)*q-(*bx-*ax)*r)/
               (2.0*SIGN(MAX(fabs(q-r),MNBRAK_TINY),q-r));
       ulim=(*bx)+MNBRAK_GLIMIT*(*cx-*bx);
       
       if(u > lim_sup) u = lim_sup;
       if(u < lim_inf) u = lim_inf;
       if(ulim > lim_sup) ulim = lim_sup;
       if(ulim < lim_inf) ulim = lim_inf;

       if ((*bx-u)*(u-*cx) > 0.0)
	 {
	   *param = u;	 
	   fu = evaluateRate(tr, i, *param, adef, model);
	   if (fu < *fc) 
	     {
	       *ax=(*bx);
	       *bx=u;
	       *fa=(*fb);
	       *fb=fu;	       
	       return(0);
	     } 
	   else 
	     {
	       if (fu > *fb) 
		 {
		   *cx=u;
		   *fc=fu;			  
		   return(0);
		 }
	     }
	   u=(*cx)+MNBRAK_GOLD*(*cx-*bx);
	   *param = u;
	   if(*param > lim_sup) {*param = u = lim_sup;}
	   if(*param < lim_inf) {*param = u = lim_inf;}	   
	   fu= evaluateRate(tr, i, *param, adef, model);
	 } 
       else 
	 {
	   if ((*cx-u)*(u-ulim) > 0.0) 
	     {
	       *param = u;	       
	       fu = evaluateRate(tr, i, *param, adef, model);
	       if (fu < *fc) 
		 {
		   SHFT(*bx,*cx,u,*cx+MNBRAK_GOLD*(*cx-*bx))		  
		   SHFT(*fb,*fc,fu, evaluateRate(tr, i, *param, adef, model))
		     }
	     } 
	   else
	     {
	       if ((u-ulim)*(ulim-*cx) >= 0.0) 
		 {
		   u = ulim;
		   *param = u;		  
		   fu = evaluateRate(tr, i, *param, adef, model);		   
		 } 
	       else 
		 {
		   u=(*cx)+MNBRAK_GOLD*(*cx-*bx);
		   *param = u;
		   if(*param > lim_sup) {*param = u = lim_sup;}
		   if(*param < lim_inf) {*param = u = lim_inf;}		   
		   fu = evaluateRate(tr, i, *param, adef, model);
		 }
	     }
	 }     
       SHFT(*ax,*bx,*cx,u)
	 SHFT(*fa,*fb,*fc,fu)
	 }

   
   return(0);
}




void optAlpha(tree *tr, analdef *adef, double modelEpsilon, int model)
{
  double param, a, b, c, fa, fb, fc, x;
  double lim_inf = ALPHA_MIN;
  double lim_sup = ALPHA_MAX;
  double startLH;
  double startAlpha = tr->alphas[model];
  double endAlpha;

  if(adef->useMultipleModel)
    startLH = partitionLikelihood(tr, model);
  else
    startLH = tr->likelihood;

  a = tr->alphas[model] + 0.1;
  b = tr->alphas[model] - 0.1; 
  
  if(b < lim_inf) b = lim_inf;
  
  brakAlpha(&param, &a, &b, &c, &fa, &fb, &fc, lim_inf, lim_sup, tr, model, adef);
          
  endAlpha = brentAlpha(a, b, c, fa, fb, fc, modelEpsilon, &x, model, tr, adef);  

  if(startLH > endAlpha)
    {    
      tr->alphas[model] = startAlpha;
      evaluateAlpha(tr, tr->alphas[model], model, adef);      
    } 
}

double optRates(tree *tr, analdef *adef, double modelEpsilon, int model)
{
  int i;
  double param, a, b, c, fa, fb, fc, x;
  double lim_inf = RATE_MIN;
  double lim_sup = RATE_MAX;
  double *startRates;
  double startLH, start2;
  double endLH;


  startRates = (double *)malloc(sizeof(double) * tr->numberOfRates);

  if(adef->useMultipleModel)
    startLH = partitionLikelihood(tr, model);
  else
    startLH = tr->likelihood;

  start2 = startLH;    

  for(i = 0; i < tr->numberOfRates; i++)
    startRates[i] = tr->initialRates[model * tr->numberOfRates  + i];

  for(i = 0; i < tr->numberOfRates; i++)
    {           
      a = tr->initialRates[model * tr->numberOfRates + i] + 0.1;
      b = tr->initialRates[model * tr->numberOfRates + i] - 0.1;
      
      if(a < lim_inf) a = lim_inf;
      if(a > lim_sup) a = lim_sup;

      if(b < lim_inf) b = lim_inf;
      if(b > lim_sup) b = lim_sup;    
      
      brakRates(&param, &a, &b, &c, &fa, &fb, &fc, lim_inf, lim_sup, tr, i, adef, model);       
	                
      endLH = brentRates(a, b, c, fa, fb, fc, modelEpsilon, &x,  model, tr, adef, i);       
      
      if(startLH > endLH)
	{       	
	  tr->initialRates[model * tr->numberOfRates + i] = startRates[i];       
	  initReversibleGTR(tr->rdta, tr->cdta, tr, adef, model); 

	  if(adef->useMultipleModel)	    	    
	    startLH = partitionLikelihood(tr, model);	    	     	  	    
	  else
	    {
	      initrav(tr, tr->start);
	      initrav(tr, tr->start->back);
	      evaluate(tr, tr->start); 

	      startLH = tr->likelihood;
	    }
	}
      else	
	startLH = endLH;                     	
    } 

  free(startRates);
  return endLH;
}

void resetBranches(tree *tr)
{
  nodeptr  p, q;
  int  nodes;
  
  nodes = tr->mxtips  +  3 * (tr->mxtips - 2);
  p = tr->nodep[1];
  while (nodes-- > 0) 
    {
      p->z = defaultz; 
      q = p->next;
      while(q != p)
	{
	  q->z = defaultz;
	  q = q->next;
	}
      p++;
    }
}

void modOpt(tree *tr, analdef *adef)
{
  double currentLikelihood, intermediateLikelihood;
  int i; 
  double modelEpsilon = MODEL_EPSILON;
  double optTime = gettime();
  int model;
  double *partialLikelihoods = (double *)malloc(tr->NumberOfModels * sizeof(double));

  for(model = 0; model < tr->NumberOfModels; model++)
    {
      for(i = 0; i <  tr->numberOfRates; i++)
	tr->initialRates[model * tr->numberOfRates + i] = 0.5;
       
      tr->alphas[model] = 1.0;
      initReversibleGTR(tr->rdta, tr->cdta, tr, adef, model);
      makeGammaCats(tr, model); 
    }

  resetBranches(tr);
  tr->start = tr->nodep[1];

  /* no need for individual models here, just an init on params equal for all partitions*/
  initrav(tr, tr->start);
  initrav(tr, tr->start->back);
  evaluate(tr, tr->start); 

  treeEvaluate(tr, 1.0);
 
  do
    {
      currentLikelihood = tr->likelihood;
                  
      if(adef->model == M_GTRGAMMA)
	{	       	           
	  for(model = 0; model < tr->NumberOfModels; model++)
	    partialLikelihoods[model] = optRates(tr, adef, modelEpsilon, model);   			
	
	  for(model = 0; model < tr->NumberOfModels; model++)
	    optAlpha(tr, adef, modelEpsilon, model);       	 	
	}
      else
	{
	  if(adef->model == M_PROTGAMMA)
	    {
	      if(adef->proteinMatrix == GTR)
		{
		  for(model = 0; model < tr->NumberOfModels; model++)
		    partialLikelihoods[model] = optRates(tr, adef, modelEpsilon, model); 
		}
	      else
		{
		  if(adef->useMultipleModel)
		    for(model = 0; model < tr->NumberOfModels; model++)
		      partialLikelihoods[model] = partitionLikelihood(tr, model);
		}
	      
	      for(model = 0; model < tr->NumberOfModels; model++)
		optAlpha(tr, adef, modelEpsilon, model);    	      
	    }
	  else
	    {
	      printf("Internal error this function should only be called with GTR+GAMMA or PROT+GAMMA\n");
	      exit(-1);
	    } 
	}

      if(tr->NumberOfModels > 1)
	{
	   tr->fracchange = 0;
	   for(model = 0; model < tr->NumberOfModels; model++)
	     tr->fracchange += tr->fracchanges[model] * abs(partialLikelihoods[model]);
	   tr->fracchange /= abs(tr->likelihood);
	}      
      
      treeEvaluate(tr, 0.25);
           
      modelEpsilon /= 10.0;
      if(modelEpsilon < LIKELIHOOD_EPSILON)
	modelEpsilon = LIKELIHOOD_EPSILON;     
    }
  while(fabs(currentLikelihood - tr->likelihood) > adef->likelihoodEpsilon);

  
  /*currentLikelihood = tr->likelihood;

  initrav(tr, tr->start);
  initrav(tr, tr->start->back);

  for(model = 0; model < tr->NumberOfModels; model++)
    {
      double partialLH;
      partialLH = evaluatePartition(tr, tr->start, model);
      printf("Partition %d Log Likelihood %f\n", model, partialLH);
    }
      
    tr->likelihood = currentLikelihood;*/
  
  free(partialLikelihoods);
}

/*********************FUNCTIONS FOOR EXACT MODEL OPTIMIZATION UNDER GTRGAMMA ***************************************/


/*********************FUNCTIONS FOR APPROXIMATE MODEL OPTIMIZATION ***************************************/

void optimizeAlphaMULT(tree *tr, int model, analdef *adef)
{
  int k; 
  double currentTT, startLikelihood, maxLikelihoodMinus,
    maxLikelihoodPlus, maxTTPlus, maxTTMinus, spacing,
    tree_likelihood;
  boolean finish = FALSE;

  spacing = 0.5/((double)optimizeAlphaInvocations);

  currentTT = tr->alphas[model];
  
  tree_likelihood = partitionLikelihood(tr, model);
 
  maxTTPlus = currentTT;
  maxTTMinus = currentTT;

  startLikelihood = tree_likelihood;
  maxLikelihoodMinus = tree_likelihood;
  maxLikelihoodPlus = tree_likelihood;
      
  k = 1;       

  while(!finish && ((currentTT - spacing * k) > ALPHA_MIN)) 
    {           
      tr->alphas[model] = currentTT - spacing * k;     
                
      makeGammaCats(tr, model);
     
      tree_likelihood = partitionLikelihood(tr, model);
           
      if(tree_likelihood > maxLikelihoodMinus)
	{
	  finish = (fabs(tree_likelihood - maxLikelihoodMinus) < adef->likelihoodEpsilon);

	  maxLikelihoodMinus = tree_likelihood;
	  maxTTMinus = currentTT - spacing * k;
	}    
      else
	finish = TRUE;

      k++;
    }

  finish = FALSE;
  k = 1;
  tree_likelihood = startLikelihood;

  while(!finish && ((currentTT + spacing * k) < ALPHA_MAX)) 
    {      
      tr->alphas[model] = currentTT + spacing * k;     
      makeGammaCats(tr, model);
      
      tree_likelihood = partitionLikelihood(tr, model);     

      if(tree_likelihood > maxLikelihoodPlus)
	{
	  finish = (fabs(tree_likelihood - maxLikelihoodPlus) < adef->likelihoodEpsilon);

	  maxLikelihoodPlus = tree_likelihood;
	  maxTTPlus = currentTT + spacing * k;
	}	 
      else
	finish = TRUE;

      k++;
    }  

  if(maxLikelihoodPlus > startLikelihood || maxLikelihoodMinus > startLikelihood)
    {
      if(maxLikelihoodPlus > maxLikelihoodMinus)	
	tr->alphas[model] = maxTTPlus;	
      else	
	tr->alphas[model] = maxTTMinus;	    

      makeGammaCats(tr, model);    
    }
  else
    { 
      tr->alphas[model] = currentTT;
      makeGammaCats(tr, model);
    }    
}

void optimizeAlpha(tree *tr, analdef *adef)
{
 int k;
 
 double currentTT = tr->alphas[0];
 double startLikelihood = tr->likelihood;
 double maxLikelihoodMinus = tr->likelihood;
 double maxLikelihoodPlus = tr->likelihood;
 double maxTTPlus = currentTT;
 double maxTTMinus = currentTT;
 double spacing = 0.5/((double)optimizeAlphaInvocations);
 double tree_likelihood = startLikelihood;
 boolean finish = FALSE;
     
 k = 1;
      
 while(!finish && (currentTT - spacing * k) > ALPHA_MIN) 
   {      
     tr->alphas[0] = currentTT - spacing * k;     
     makeGammaCats(tr, 0);
     initrav(tr, tr->start);
     initrav(tr, tr->start->back);
     evaluate(tr, tr->start);                   
     
     tree_likelihood = tr->likelihood;
     
     if(tr->likelihood > maxLikelihoodMinus)
       {
	 finish = (fabs(tree_likelihood - maxLikelihoodMinus) < adef->likelihoodEpsilon);

	 maxLikelihoodMinus = tr->likelihood;
	 maxTTMinus = currentTT - spacing * k;
       }    
     else
	finish = TRUE;
        
     k++;
   }
 
 finish = FALSE;
 k = 1;
 tree_likelihood = startLikelihood;
 while(!finish && ((currentTT + spacing * k) < ALPHA_MAX)) 
   {      
     tr->alphas[0] = currentTT + spacing * k;     
     makeGammaCats(tr, 0);
     initrav(tr, tr->start);
     initrav(tr, tr->start->back);
     evaluate(tr, tr->start);                  
     
     tree_likelihood = tr->likelihood;
             
     if(tr->likelihood > maxLikelihoodPlus)
       {
	 finish = (fabs(tree_likelihood - maxLikelihoodPlus) < adef->likelihoodEpsilon);
	 
	 maxLikelihoodPlus = tr->likelihood;
	 maxTTPlus = currentTT + spacing * k;
       }	 
     else
       finish = TRUE;

     k++;
   }
 
 if(maxLikelihoodPlus > startLikelihood || maxLikelihoodMinus > startLikelihood)
    {
      if(maxLikelihoodPlus > maxLikelihoodMinus)
	{
	  tr->alphas[0] = maxTTPlus;
	}
      else
	{
	  tr->alphas[0] = maxTTMinus;
	}     
      makeGammaCats(tr, 0);
      initrav(tr, tr->start);
      initrav(tr, tr->start->back);
      evaluate(tr, tr->start);      
    }
  else
    { 
      tr->alphas[0] = currentTT;
      makeGammaCats(tr, 0);
      initrav(tr, tr->start);
      initrav(tr, tr->start->back);      
      evaluate(tr, tr->start);
    }    
}

void alterRates(tree *tr, int k, analdef *adef)
{
  int i;
  double granularity = 0.1/((double)optimizeRatesInvocations);
  double bestLikelihood = tr->likelihood, 
    maxLikelihoodMinus = tr->likelihood, maxLikelihoodPlus = tr->likelihood,
    treeLikelihood, originalRate, maxRateMinus, maxRatePlus;
  boolean finish = FALSE;

  i = 1;
  originalRate = tr->initialRates[k];
  maxRateMinus = maxRatePlus = originalRate;
  treeLikelihood = bestLikelihood;

  while(!finish && ((originalRate - granularity * i) > RATE_MIN))
    {     
      tr->initialRates[k] = originalRate - granularity * i;      
      initReversibleGTR(tr->rdta, tr->cdta, tr, adef, 0);     
      initrav(tr, tr->start);
      initrav(tr, tr->start->back);
      evaluate(tr, tr->start);      
      treeLikelihood = tr->likelihood;          

      if(tr->likelihood > maxLikelihoodMinus)
	{
	  finish = (fabs(treeLikelihood - maxLikelihoodMinus) < adef->likelihoodEpsilon);
	  
	  maxLikelihoodMinus = tr->likelihood;
	  maxRateMinus = originalRate - granularity * i;
	}      
      else
	finish = TRUE;

      i++;
    }

  i = 1;
  treeLikelihood = bestLikelihood;
  tr->initialRates[k] = originalRate;
  finish = FALSE;

  while(!finish && ((originalRate + i * granularity) < RATE_MAX))
    {     
      tr->initialRates[k] = originalRate + i * granularity;
      initReversibleGTR(tr->rdta, tr->cdta, tr, adef, 0);     
      initrav(tr, tr->start);
      initrav(tr, tr->start->back);
      evaluate(tr, tr->start);      
      treeLikelihood = tr->likelihood;
      
      if(tr->likelihood > maxLikelihoodPlus)
	{
	  finish = (fabs(treeLikelihood - maxLikelihoodPlus) < adef->likelihoodEpsilon);
	  	  
	  maxLikelihoodPlus = tr->likelihood;
	  maxRatePlus = originalRate + granularity * i;
	}
      else
	finish = TRUE;
      i++;
    }
  if(maxLikelihoodPlus > bestLikelihood || maxLikelihoodMinus > bestLikelihood)
    {
      if(maxLikelihoodPlus > maxLikelihoodMinus)
	{	  
	  tr->initialRates[k] = maxRatePlus;
	  initReversibleGTR(tr->rdta, tr->cdta, tr, adef, 0);	  
	}
      else
	{
	 tr->initialRates[k] = maxRateMinus;
	 initReversibleGTR(tr->rdta, tr->cdta, tr, adef, 0);	  	
	}     
    
      initrav(tr, tr->start);
      initrav(tr, tr->start->back);
      evaluate(tr, tr->start);         
    }
  else
    {      
      tr->initialRates[k] = originalRate;
      initReversibleGTR(tr->rdta, tr->cdta, tr, adef, 0);          
      initrav(tr, tr->start);
      initrav(tr, tr->start->back);
      evaluate(tr, tr->start);   
    }  
}

double alterRatesMULT(tree *tr, int k, analdef *adef, int model)
{
  int i;
  double granularity = 0.1/((double)optimizeRatesInvocations);
  double bestLikelihood, maxLikelihoodMinus, maxLikelihoodPlus,
    treeLikelihood, originalRate, maxRateMinus, maxRatePlus;
  boolean finish = FALSE;
  
  treeLikelihood = partitionLikelihood(tr, model);   

  bestLikelihood = treeLikelihood;
  maxLikelihoodMinus = treeLikelihood;
  maxLikelihoodPlus = treeLikelihood;
  
  i = 1;
  originalRate = tr->initialRates[model * tr->numberOfRates + k];
  maxRateMinus = maxRatePlus = originalRate;
  bestLikelihood;

  while(!finish && ((originalRate - granularity * i) > RATE_MIN))
    {
      tr->initialRates[model * tr->numberOfRates + k] = originalRate - granularity * i;   
      
      initReversibleGTR(tr->rdta, tr->cdta, tr, adef, model);         
      
      treeLikelihood = partitionLikelihood(tr, model);
      
      if(treeLikelihood > maxLikelihoodMinus)
	{
	  finish = (fabs(treeLikelihood - maxLikelihoodMinus) < adef->likelihoodEpsilon);
	  
	  maxLikelihoodMinus = treeLikelihood;
	  maxRateMinus = originalRate - granularity * i;
	}      
      else
	finish = TRUE;

      i++;
    }

  i = 1;
  treeLikelihood = bestLikelihood;
  tr->initialRates[model * tr->numberOfRates + k] = originalRate;
  finish = FALSE;

  while(!finish && ((originalRate + i * granularity) < RATE_MAX))
    {
      tr->initialRates[model * tr->numberOfRates + k] = originalRate + i * granularity;
      initReversibleGTR(tr->rdta, tr->cdta, tr, adef, model);        		          

      treeLikelihood = partitionLikelihood(tr, model);
     
      if(treeLikelihood > maxLikelihoodPlus)
	{
	  finish = (fabs(treeLikelihood - maxLikelihoodPlus) < adef->likelihoodEpsilon);
	  
	  maxLikelihoodPlus = treeLikelihood;
	  maxRatePlus = originalRate + granularity * i;
	}      
      else
	finish = TRUE;

      i++;
    }

  if(maxLikelihoodPlus > bestLikelihood || maxLikelihoodMinus > bestLikelihood)
    {
      if(maxLikelihoodPlus > maxLikelihoodMinus)
	{	  
	  tr->initialRates[model * tr->numberOfRates + k] = maxRatePlus;
	  initReversibleGTR(tr->rdta, tr->cdta, tr, adef, model);	  	 
	  return maxLikelihoodPlus;
	}
      else
	{
	  tr->initialRates[model * tr->numberOfRates + k] = maxRateMinus;
	  initReversibleGTR(tr->rdta, tr->cdta, tr, adef, model);	  		 
	  return maxLikelihoodMinus;
	}              
    }
  else
    {      
      tr->initialRates[model * tr->numberOfRates + k] = originalRate;
      initReversibleGTR(tr->rdta, tr->cdta, tr, adef, model);                    
      return bestLikelihood;
    }
}

void optimizeRates(tree *tr, analdef *adef)
{
  int i, model;
  double start = tr->likelihood;

  if(adef->useMultipleModel)
    {
      double *partialLikelihoods = (double *)malloc(tr->NumberOfModels * sizeof(double));      

      for(model = 0; model < tr->NumberOfModels; model++)
	{	  	
	  for(i = 0; i < tr->numberOfRates; i++)		      	    	   
	    partialLikelihoods[model] = alterRatesMULT(tr, i, adef, model);     	
	}

      initrav(tr, tr->start);
      initrav(tr, tr->start->back);
      evaluate(tr, tr->start);   

      tr->fracchange = 0;

      for(model = 0; model < tr->NumberOfModels; model++)	
	tr->fracchange += tr->fracchanges[model] * abs(partialLikelihoods[model]);	  
	
      tr->fracchange /= abs(tr->likelihood);

      free(partialLikelihoods);
    }
  else
    {                    
      for(i = 0; i < tr->numberOfRates; i++)		      
	alterRates(tr, i, adef);
    }
         
  optimizeRatesInvocations++;
}



static int doublecompare2(const void *p1, const void *p2)
{
  double i = *((double *)p1);

  double j = *((double *)p2);
  
  if (i > j)
    return (1);
  if (i < j)
    return (-1);
  return (0);
}

static int catCompare(const void *p1, const void *p2)
{
 rateCategorize *rc1 = (rateCategorize *)p1;
 rateCategorize *rc2 = (rateCategorize *)p2;

  double i = rc1->accumulatedSiteLikelihood;
  double j = rc2->accumulatedSiteLikelihood;
  
  if (i > j)
    return (1);
  if (i < j)
    return (-1);
  return (0);
}




void categorize(tree *tr, rateCategorize *rc)
{
  int i, k, found;
  double temp, diff, min;

  for (i = 0; i < tr->cdta->endsite; i++) 
      {
	temp = tr->cdta->patrat[i];
	found = 0;
	for(k = 0; k < tr->NumberOfCategories; k++)
	  {
	    if(temp == rc[k].rate || (fabs(temp - rc[k].rate) < 0.001))
	      {
		found = 1;
		tr->cdta->rateCategory[i] = k;				
		break;
	      }
	  }
	if(!found)
	  {
	    min = fabs(temp - rc[0].rate);
	    tr->cdta->rateCategory[i] = 0;

	    for(k = 1; k < tr->NumberOfCategories; k++)
	    {
	      diff = fabs(temp - rc[k].rate);
	      if(diff < min)
		{
		  min = diff;
		  tr->cdta->rateCategory[i] = k;
		}
	    }
	  }
      }

  for(k = 0; k < tr->NumberOfCategories; k++)
    tr->cdta->patrat[k] = rc[k].rate; 

}




void optimizeRateCategories(tree *tr, int categorized, int _maxCategories, analdef *adef)
{
  int i, k;
  double initialRate, initialLikelihood, v, leftRate, rightRate, leftLH, rightLH, temp, wtemp;   
  double lower_spacing, upper_spacing;
  int maxCategories = _maxCategories;
  double initialLH = tr->likelihood;
  double *oldRat = (double *)malloc(sizeof(double) * tr->cdta->endsite);
  double *ratStored = (double *)malloc(sizeof(double) * tr->cdta->endsite);
  double *oldwr = (double *)malloc(sizeof(double) * tr->cdta->endsite);
  double *oldwr2 = (double *)malloc(sizeof(double) * tr->cdta->endsite);
  double *lhs = (double *)malloc(sizeof(double) * tr->cdta->endsite);
  int *oldCategory = (int *)malloc(sizeof(int) * tr->cdta->endsite);  
  int oldNumber; 
  double epsilon = 0.00001;


  if(optimizeRateCategoryInvocations == 1)
    {
      lower_spacing = 0.5 / ((double)optimizeRateCategoryInvocations);
      upper_spacing = 1.0 / ((double)optimizeRateCategoryInvocations);
    }
  else
    {
      lower_spacing = 0.05 / ((double)optimizeRateCategoryInvocations);
      upper_spacing = 0.1 / ((double)optimizeRateCategoryInvocations);
    }

  if(lower_spacing < 0.001)
    lower_spacing = 0.001;

  if(upper_spacing < 0.001)
    upper_spacing = 0.001;

  optimizeRateCategoryInvocations++;
  
  oldNumber = tr->NumberOfCategories;


  for(i = 0; i < tr->cdta->endsite; i++)
    {    
      oldCategory[i] = tr->cdta->rateCategory[i];
      ratStored[i] = tr->cdta->patratStored[i];    
      oldRat[i] = tr->cdta->patrat[i];
      oldwr[i] =  tr->cdta->wr[i];
      oldwr2[i] =  tr->cdta->wr2[i];
    }

#ifdef _OPENMP
#pragma omp parallel for private(initialRate, leftLH, rightLH, initialLikelihood, leftRate, rightRate, k, v) schedule(dynamic)
#endif

  for(i = 0; i < tr->cdta->endsite; i++)
    {    
      tr->cdta->patrat[i] = tr->cdta->patratStored[i];     
      initialRate = tr->cdta->patrat[i];

      
      initialLikelihood = evaluatePartial(tr, tr->start, i, initialRate);      
       
      leftLH = rightLH = initialLikelihood;
      leftRate = rightRate = initialRate;
       
      k = 1;
     
      while((initialRate - k * lower_spacing > 0.0001) && 
	    ((v = evaluatePartial(tr, tr->start, i, initialRate - k * lower_spacing)) > leftLH) && 
	    (fabs(leftLH - v) > epsilon))
	{	  
	  leftLH = v;
	  leftRate = initialRate - k * lower_spacing;
	  k++;	  
	}      
   
      k = 1;
    
      while(((v = evaluatePartial(tr, tr->start, i, initialRate + k * upper_spacing)) > rightLH) &&
	    (fabs(rightLH - v) > epsilon))	
	{
	  rightLH = v;
	  rightRate = initialRate + k * upper_spacing;	 
	  k++;
	}           

      if(rightLH > initialLikelihood || leftLH > initialLikelihood)
	{
	  if(rightLH > leftLH)	    
	    {	     
	      tr->cdta->patrat[i] = rightRate;
	      lhs[i] = rightLH;
	    }
	  else
	    {	      
	      tr->cdta->patrat[i] = leftRate;
	      lhs[i] = leftLH;
	    }
	}
      else
	lhs[i] = initialLikelihood;

      tr->cdta->patratStored[i] = tr->cdta->patrat[i];     
      
    }


  if(adef->printRates)
    {
      char temporaryFileName[1024];
      char buf[16];
      FILE *out;
      int i;

      strcpy(temporaryFileName, ratesFileName);

      if(adef->multipleRuns > 1)
	{	  	 	  
	  sprintf(buf, "%d", tr->treeID);	  	  
	  strcat(temporaryFileName, ".RUN.");
	  strcat(temporaryFileName, buf);	  	 	             
	}

      sprintf(buf, "%d", optimizeRateCategoryInvocations - 2);
     
      strcat(temporaryFileName, ".");
      strcat(temporaryFileName, buf);
      out = fopen(temporaryFileName, "w");    

      printf("Printing File %s\n", temporaryFileName);
      
      for(i = 0; i < tr->cdta->endsite; i++)
	fprintf(out, "%d %f\n", i, tr->cdta->patrat[i]);     
      
      fclose(out);      
    }

     

  {     
    rateCategorize *rc = (rateCategorize *)malloc(sizeof(rateCategorize) * tr->cdta->endsite);
    int where;
    int found = 0;
    for (i = 0; i < tr->cdta->endsite; i++)
      {
	rc[i].accumulatedSiteLikelihood = 0;
	rc[i].rate = 0;
      }
      
    where = 1;   
    rc[0].accumulatedSiteLikelihood = lhs[0];
    rc[0].rate = tr->cdta->patrat[0];
    tr->cdta->rateCategory[0] = 0;
    
    for (i = 1; i < tr->cdta->endsite; i++) 
      {
	temp = tr->cdta->patrat[i];
	found = 0;
	for(k = 0; k < where; k++)
	  {
	    if(temp == rc[k].rate || (fabs(temp - rc[k].rate) < 0.001))
	      {
		found = 1;						
		rc[k].accumulatedSiteLikelihood += lhs[i];	
		break;
	      }
	  }
	if(!found)
	  {	    
	    rc[where].rate = temp;	    
	    rc[where].accumulatedSiteLikelihood += lhs[i];	    
	    where++;
	  }
	}

    qsort(rc, where, sizeof(rateCategorize), catCompare);
  
    if(where < maxCategories)
      {
	tr->NumberOfCategories = where;
	categorize(tr, rc);
      }
    else
      {
	tr->NumberOfCategories = maxCategories;	
	categorize(tr, rc);
      }
      
    free(rc);
  
    for (i = 0; i < tr->cdta->endsite; i++) 
      {	
	temp = tr->cdta->patrat[tr->cdta->rateCategory[i]];

	tr->cdta->wr[i]  = wtemp = temp * tr->cdta->aliaswgt[i];
	tr->cdta->wr2[i] = temp * wtemp;
      }     

    initrav(tr, tr->start);    
    initrav(tr, tr->start->back);   
    evaluate(tr, tr->start);   

    if(tr->likelihood < initialLH)
      {	
	tr->NumberOfCategories = oldNumber;
	for (i = 0; i < tr->cdta->endsite; i++)
	    {
	      tr->cdta->patratStored[i] = ratStored[i]; 
	      tr->cdta->rateCategory[i] = oldCategory[i];
	      tr->cdta->patrat[i] = oldRat[i];	    
	      tr->cdta->wr[i]  = oldwr[i];
	      tr->cdta->wr2[i] = oldwr2[i];

	    }       

	initrav(tr, tr->start);
	initrav(tr, tr->start->back);
        evaluate(tr, tr->start);

      }
    }
  
  free(oldCategory);
  free(oldRat);
  free(ratStored);
  free(oldwr);
  free(oldwr2); 
  free(lhs);
}
  

void optimizeAlphas(tree *tr, analdef *adef)
{
  int model;
 
  if(adef->useMultipleModel)
    {
      for(model = 0; model < tr->NumberOfModels; model++)       
	optimizeAlphaMULT(tr, model, adef);	  
	    
      initrav(tr, tr->start);
      initrav(tr, tr->start->back);    
      evaluate(tr, tr->start);      
    }
  else
    {      
      optimizeAlpha(tr, adef);
    }

  optimizeAlphaInvocations++;
}

/*static void rateCategorizeExpectation(tree *tr, double *vector, int n)
{
  int i, j;
  double e;
  double allSum;

  for(j = 0; j < 4; j++)
    printf("Rate %d %f\n", j, tr->gammaRates[j]);
  printf("\n");

  for(i = 0; i < n; i++)
    {
      allSum = 0.25 * (vector[i * 4] + vector[i * 4 + 1] + vector[i * 4 + 2] + vector[i * 4 + 3]);

      e = 0.0;
      for(j = 0; j < 4; j++)	
	e += (tr->gammaRates[j] * ((0.25 * vector[i * 4 + j]) / allSum));
      printf("Site %d E %f\n", i, e);
    }
    }*/


static void categorizeGAMMA(tree *tr, nodeptr p)
{
  double   z, lz, ki, term[4];    
  nodeptr  q;
  int     i, index;
  double  *diagptable, *diagptable_start, temp, wtemp, allSum, min;
  double *EIGN = tr->EIGN;
  gammalikelivector *x2; 
  char *tipX1;
  likelivector *x1;
  
  q = p->back;
  
  z = p->z;
  
  if (z < zmin) z = zmin;
  lz = log(z);
  
  diagptable = diagptable_start = (double *)malloc(sizeof(double) * 4 * 3);

  for(i = 0; i < 4; i++)
    {
      ki = tr->gammaRates[i];	 
      
      *diagptable++ = exp (EIGN[0] * ki * lz);
      *diagptable++ = exp (EIGN[1] * ki * lz);
      *diagptable++ = exp (EIGN[2] * ki * lz);
    }      


  tr->cdta->patrat[0] = tr->gammaRates[0];
  tr->cdta->patrat[1] = tr->gammaRates[1];
  tr->cdta->patrat[2] = tr->gammaRates[2];
  tr->cdta->patrat[3] = tr->gammaRates[3];
  
  tr->NumberOfCategories = 4;
  
 
     
  if(q->tip)
    {
      nodeptr temp;
      temp = q;
      q = p;
      p = temp;
    }
  
  tipX1 = p->tip;      
  
  while ((! q->x)) 
    {	     
      if (! (q->x)) if (! newviewGTRGAMMA(tr, q)) return;
    }
  
  x2 = (gammalikelivector*)q->x;
      
  for (i = 0; i < tr->cdta->endsite; i++) 
    {
      x1 = &(tr->gtrTip[*tipX1++]);
      diagptable = diagptable_start;
      
      /* cat 0 */
      
      term[0]  =  x1->a * x2->a0;
      term[0] += x1->c * x2->c0 * *diagptable++;
      term[0] += x1->g * x2->g0 * *diagptable++;
      term[0] += x1->t * x2->t0 * *diagptable++;     
      
      term[0] = log(term[0]) + (x2->exp)*log(minlikelihood);
      
      /* cat 1 */
      
      term[1]  = x1->a * x2->a1;
      term[1] += x1->c * x2->c1 * *diagptable++;
      term[1] += x1->g * x2->g1 * *diagptable++;
      term[1] += x1->t * x2->t1 * *diagptable++;    
      
      term[1] = log(term[1]) + (x2->exp)*log(minlikelihood);
      
      /* cat 2 */
      
      term[2]  = x1->a * x2->a2;
      term[2] += x1->c * x2->c2 * *diagptable++;
      term[2] += x1->g * x2->g2 * *diagptable++;
      term[2] += x1->t * x2->t2 * *diagptable++;     
      
      term[2] = log(term[2]) + (x2->exp)*log(minlikelihood);	 
      
      /* cat 3 */
      
      term[3]  = x1->a * x2->a3;
      term[3] += x1->c * x2->c3 * *diagptable++;
      term[3] += x1->g * x2->g3 * *diagptable++;
      term[3] += x1->t * x2->t3 * *diagptable++;     
      
      term[3] = log(term[3]) + (x2->exp)*log(minlikelihood);
      
      allSum = 0.25 * (term[0] + term[1] + term[2] + term[3]);
      
      allSum = 1.0 / allSum;
      
      term[0] = 0.25 * term[0] * allSum;
      term[1] = 0.25 * term[1] * allSum;
      term[2] = 0.25 * term[2] * allSum;
      term[3] = 0.25 * term[3] * allSum;
      
      min = largeDouble;
      if(term[0] < min)
	{
	  min   = term[0];
	  index = 0;
	}	
      if(term[1] < min)
	{
	  min   = term[1];
	  index = 1;
	}
      if(term[2] < min)
	{
	  min   = term[2];
	  index = 2;
	}
      if(term[3] < min)
	{
	  min   = term[3];
	  index = 3;
	}
      
      tr->cdta->rateCategory[i] = index;
      temp = tr->gammaRates[index];     
      tr->cdta->wr[i]  = wtemp = temp * tr->cdta->aliaswgt[i];
      tr->cdta->wr2[i] = temp * wtemp;
      
      x2++;
    }
  
  free(diagptable_start);    
  return;
}







int optimizeModel (tree *tr, analdef *adef, int finalOptimization)
{
  double startLH = tr->likelihood;
  double initialLH;
  int oldInv;

  switch(adef->model)
    {          
    case M_PROTCAT:
      {	              
	if(adef->proteinMatrix == GTR)
	  {
	    oldInv = optimizeRatesInvocations;
	    do
	      {	    
		initialLH = tr->likelihood;
		optimizeRates(tr, adef);       
	      }
	    while(tr->likelihood > initialLH && optimizeRatesInvocations < oldInv + 10);
	  }	   
	
	 oldInv = optimizeRateCategoryInvocations;	 	
	 optimizeRateCategories(tr, finalOptimization, adef->categories, adef);
       }                    
      break;         
    case M_GTRCAT:
      if(!adef->categorizeGamma)
	{
	  oldInv = optimizeRatesInvocations;

	  do
	    {	    
	      initialLH = tr->likelihood;
	      optimizeRates(tr, adef);       
	    }
	  while(tr->likelihood > initialLH && optimizeRatesInvocations < oldInv + 10);       	 
	
	  oldInv = optimizeRateCategoryInvocations;	 	  
	  optimizeRateCategories(tr, finalOptimization, adef->categories, adef);
	}
      else
	{	         		  
	  int j;
	  double catLikelihood;
	  int    *catVector    = (int *)malloc(sizeof(int) * tr->cdta->endsite);	 
	  double *wr           = (double *)malloc(sizeof(double) * tr->cdta->endsite);
	  double *wr2          = (double *)malloc(sizeof(double) * tr->cdta->endsite);
	  double *patrat       = (double *)malloc(sizeof(double) * tr->cdta->endsite);
	  double *initialRates = (double *)malloc(tr->NumberOfModels * 5 * sizeof(double));

	  memcpy(catVector,    tr->cdta->rateCategory, sizeof(int) * tr->cdta->endsite);	  
	  memcpy(initialRates, tr->initialRates, tr->NumberOfModels * 5 * sizeof(double));
	  memcpy(wr, tr->cdta->wr, sizeof(double) * tr->cdta->endsite);
	  memcpy(wr2, tr->cdta->wr2, sizeof(double) * tr->cdta->endsite);
	  memcpy(patrat, tr->cdta->patrat, sizeof(double) * tr->cdta->endsite);	  	  

	  /*

	  Doing optimization under CAT is faster but apparently perturbs likelihood surface 
	  which leads to a larger number of search iterations !

	  oldInv = optimizeRatesInvocations;

	  do
	    {	    
	      initialLH = tr->likelihood;
	      optimizeRates(tr, adef);       	   	    
	    }
	  while((fabs(tr->likelihood - initialLH) > adef->likelihoodEpsilon) && optimizeRatesInvocations < oldInv + 10);
	  */

	  catLikelihood = tr->likelihood;

	  freeNodex(tr); 	
	  adef->model = M_GTRGAMMA;	 	  
	  allocNodex(tr, adef); 
	  
	  newview  = newviewGTRGAMMA;
	  makenewz = makenewzGTRGAMMA;
	  evaluate = evaluateGTRGAMMA;	  	  	  
	  
	  for(j = 0; j < tr->cdta->endsite; j++)
	    tr->cdta->wr[j] = tr->cdta->aliaswgt[j];
	  	 	  
	  initrav(tr, tr->start);
	  initrav(tr, tr->start->back);
	  evaluate(tr, tr->start);
	  
	  /*printf("Model switch %f\n", tr->likelihood);*/
	  
	  oldInv = optimizeRatesInvocations;
	  
	  do
	    {	    
	      initialLH = tr->likelihood;
	      optimizeRates(tr, adef);       	   	    
	    }
	  while((fabs(tr->likelihood - initialLH) > adef->likelihoodEpsilon) && optimizeRatesInvocations < oldInv + 10);
	  
	  oldInv = optimizeAlphaInvocations;
	  
	  do
	    {    	   
	      initialLH = tr->likelihood;
	      optimizeAlphas(tr, adef);		   	  
	    }
	  while((fabs(tr->likelihood - initialLH) > adef->likelihoodEpsilon) && optimizeAlphaInvocations < oldInv + 10);
	  
	  /*printf("Model opt GAMMA %f\n", tr->likelihood);*/
	  categorizeGAMMA(tr, tr->start);	   
	  /*	  printf("Model opt GAMMA 2%f\n", tr->likelihood);*/
	  
	  freeNodex(tr);   	  
	  adef->model = M_GTRCAT;	 
	  allocNodex(tr, adef);	 
	  
	  newview  = newviewGTRCAT; 
	  makenewz = makenewzGTRCAT;
	  evaluate = evaluateGTRCAT;     
	  
	  initrav(tr,  tr->start);
	  initrav(tr,  tr->start->back);
	  evaluate(tr, tr->start);
	  if(catLikelihood > tr->likelihood)
	    {	  
	      int model;
	      /*printf("Need roll-back Rates + CATs %f %f !\n", catLikelihood, tr->likelihood);*/
	      memcpy(tr->cdta->rateCategory, catVector, sizeof(int) * tr->cdta->endsite);	  
	      memcpy(tr->initialRates, initialRates, tr->NumberOfModels * 5 * sizeof(double));
	      memcpy(tr->cdta->wr, wr, sizeof(double) * tr->cdta->endsite);
	      memcpy(tr->cdta->wr2,wr2, sizeof(double) * tr->cdta->endsite);
	      memcpy(tr->cdta->patrat, patrat, sizeof(double) * tr->cdta->endsite);
	     
	      for(model = 0;  model < tr->NumberOfModels; model++)
		initReversibleGTR(tr->rdta, tr->cdta, tr, adef, model);

	      initrav(tr,  tr->start);
	      initrav(tr,  tr->start->back);
	      evaluate(tr, tr->start);

	      /*printf("Roll Back %f\n", tr->likelihood);*/
	    }

	  free(catVector);
	  free(initialRates);
	  free(wr);
	  free(wr2);
	  free(patrat);

	  /*printf("Model switch back GTRCAT %f\n", tr->likelihood);*/
	}      
      break;       
    case M_PROTGAMMA:
      {
	if(adef->proteinMatrix == GTR)
	  {
	    oldInv = optimizeRatesInvocations;

	    do
	      {	    
		initialLH = tr->likelihood;
		optimizeRates(tr, adef);       	   	    
	      }
	    while((fabs(tr->likelihood - initialLH) > adef->likelihoodEpsilon) && optimizeRatesInvocations < oldInv + 10);       
	  }
	 
	 oldInv = optimizeAlphaInvocations;
	
	 do
	   {    	   
	     initialLH = tr->likelihood;	     
	     optimizeAlphas(tr, adef);		   	  
	   }
	 while((fabs(tr->likelihood - initialLH) > adef->likelihoodEpsilon) && optimizeAlphaInvocations < oldInv + 10);
	
      }
      break;     
    case M_GTRGAMMA:
       {
	 oldInv = optimizeRatesInvocations;

	 do
	   {	    
	     initialLH = tr->likelihood;
	     optimizeRates(tr, adef);       	   	    
	   }
	 while((fabs(tr->likelihood - initialLH) > adef->likelihoodEpsilon) && optimizeRatesInvocations < oldInv + 10);       
	 
	 oldInv = optimizeAlphaInvocations;
	
	 do
	   {    	   
	     initialLH = tr->likelihood;
	     optimizeAlphas(tr, adef);		   	  
	   }
	 while((fabs(tr->likelihood - initialLH) > adef->likelihoodEpsilon) && optimizeAlphaInvocations < oldInv + 10);	 		 
       }
      break;             
    }

  if(optimizeRatesInvocations > 90)
    optimizeRatesInvocations = 90;  
  if(optimizeRateCategoryInvocations > 90)
    optimizeRateCategoryInvocations = 90;
  if(optimizeAlphaInvocations > 90)
    optimizeAlphaInvocations = 90;
  if(optimizeTTRatioInvocations > 90)
    optimizeTTRatioInvocations = 90;

  if(startLH > tr->likelihood) return 0;
  else return 1;
}



/***************************************************************************************************************************************/

/* Methods to optimize all individual per-site rates on a given tree or on an MP starting tree */

void optimizeAllRateCategories(tree *tr)
{
  int i, k;
  double initialRate, initialLikelihood, v, leftRate, rightRate, leftLH, rightLH, temp, wtemp;   
  double lower_spacing, upper_spacing; 
  double initialLH = tr->likelihood;
  double *oldRat = (double *)malloc(sizeof(double) * tr->cdta->endsite);
  double *ratStored = (double *)malloc(sizeof(double) * tr->cdta->endsite);
  double *oldwr = (double *)malloc(sizeof(double) * tr->cdta->endsite);
  double *oldwr2 = (double *)malloc(sizeof(double) * tr->cdta->endsite);
  double *lhs = (double *)malloc(sizeof(double) * tr->cdta->endsite);
  int *oldCategory = (int *)malloc(sizeof(int) * tr->cdta->endsite);  
  int oldNumber; 
  double epsilon = 0.00001; 

  if(optimizeRateCategoryInvocations == 1)
    {
      lower_spacing = 0.5 / ((double)optimizeRateCategoryInvocations);
      upper_spacing = 1.0 / ((double)optimizeRateCategoryInvocations);
    }
  else
    {
      lower_spacing = 0.05 / ((double)optimizeRateCategoryInvocations);
      upper_spacing = 0.1 / ((double)optimizeRateCategoryInvocations);
    }

  if(lower_spacing < 0.001)
    lower_spacing = 0.001;

  if(upper_spacing < 0.001)
    upper_spacing = 0.001;

  optimizeRateCategoryInvocations++;
  
  oldNumber = tr->NumberOfCategories;

  for(i = 0; i < tr->cdta->endsite; i++)
    {    
      oldCategory[i] = tr->cdta->rateCategory[i];
      ratStored[i] = tr->cdta->patratStored[i];    
      oldRat[i] = tr->cdta->patrat[i];
      oldwr[i] =  tr->cdta->wr[i];
      oldwr2[i] =  tr->cdta->wr2[i];
    }

  for(i = 0; i < tr->cdta->endsite; i++)
    {    
      tr->cdta->patrat[i] = tr->cdta->patratStored[i];     
      initialRate = tr->cdta->patrat[i];
      
      initialLikelihood = evaluatePartial(tr, tr->start, i, initialRate);      
       
      leftLH = rightLH = initialLikelihood;
      leftRate = rightRate = initialRate;
       
      k = 1;
     
      while((initialRate - k * lower_spacing > 0.0001) && 
	    ((v = evaluatePartial(tr, tr->start, i, initialRate - k * lower_spacing)) > leftLH) && 
	    (fabs(leftLH - v) > epsilon))
	{	  
	  leftLH = v;
	  leftRate = initialRate - k * lower_spacing;
	  k++;	  
	}      
   
      k = 1;
    
      while(((v = evaluatePartial(tr, tr->start, i, initialRate + k * upper_spacing)) > rightLH) &&
	    (fabs(rightLH - v) > epsilon))	
	{
	  rightLH = v;
	  rightRate = initialRate + k * upper_spacing;	 
	  k++;
	}           

      if(rightLH > initialLikelihood || leftLH > initialLikelihood)
	{
	  if(rightLH > leftLH)	    
	    {	     
	      tr->cdta->patrat[i] = rightRate;
	      lhs[i] = rightLH;
	    }
	  else
	    {	      
	      tr->cdta->patrat[i] = leftRate;
	      lhs[i] = leftLH;
	    }
	}
      else
	lhs[i] = initialLikelihood;

      tr->cdta->patratStored[i] = tr->cdta->patrat[i];           
    }
     
  tr->NumberOfCategories = tr->cdta->endsite;

  for (i = 0; i < tr->cdta->endsite; i++) 
    {	
      temp = tr->cdta->patrat[i];
      tr->cdta->rateCategory[i] = i;
      tr->cdta->wr[i]  = wtemp = temp * tr->cdta->aliaswgt[i];
      tr->cdta->wr2[i] = temp * wtemp;
    }     

  initrav(tr, tr->start);    
  initrav(tr, tr->start->back);   
  evaluate(tr, tr->start);   
  
  if(tr->likelihood < initialLH)
    {	
      tr->NumberOfCategories = oldNumber;
      for (i = 0; i < tr->cdta->endsite; i++)
	{
	  tr->cdta->patratStored[i] = ratStored[i]; 
	  tr->cdta->rateCategory[i] = oldCategory[i];
	  tr->cdta->patrat[i] = oldRat[i];	    
	  tr->cdta->wr[i]  = oldwr[i];
	  tr->cdta->wr2[i] = oldwr2[i];
	  
	}       
      
      initrav(tr, tr->start);
      initrav(tr, tr->start->back);
      evaluate(tr, tr->start);
      
    }
  
  free(oldCategory);
  free(oldRat);
  free(ratStored);
  free(oldwr);
  free(oldwr2); 
  free(lhs);
}
  

void optimizeRatesOnly(tree *tr, analdef *adef)
{
  int i = 0;
  double modelEpsilon = MODEL_EPSILON;
  double currentLikelihood;
  char temporaryFileName[1024] = "";
  char buf[16];
  FILE *out;
  int counter = 0;
  
  printf("Sites %d\n", tr->cdta->endsite);       
  getStartingTree(tr, adef);
  treeEvaluate(tr, 2.0);
  
  printf("Eval %f\n", tr->likelihood);
 
  do
    {
      currentLikelihood = tr->likelihood;
    
      printf("Num Cat %d\n",  tr->NumberOfCategories); 

      optRates(tr, adef, modelEpsilon, 0);

      printf("GTR Rates %f\n", tr->likelihood);

      optimizeAllRateCategories(tr);  
      printf("Rates %f\n", tr->likelihood);

      modelEpsilon /= 10.0;
      if(modelEpsilon < LIKELIHOOD_EPSILON)
	modelEpsilon = LIKELIHOOD_EPSILON;

      sprintf(buf, "%d", counter);
      
      strcpy(temporaryFileName, ratesFileName);
      strcat(temporaryFileName, ".");
      strcat(temporaryFileName, buf);
      out = fopen(temporaryFileName, "w");

      printf("%s\n", temporaryFileName);

      for(i = 0; i < tr->cdta->endsite; i++)
	fprintf(out, "%d %f\n", i, log(tr->cdta->patrat[i]));     

      fclose(out);

      treeEvaluate(tr, 0.25);
      printf("EVAL %f\n\n", tr->likelihood);
      counter++;
    }
 while(fabs(currentLikelihood - tr->likelihood) > adef->likelihoodEpsilon);
}
