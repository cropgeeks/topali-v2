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



#include "axml.h"

extern int Thorough;
extern infoList iList;
extern char inverseMeaningDNA[16];
extern char seq_file[1024];

extern boolean (*newview)           (tree *, nodeptr);
extern double  (*evaluate)          (tree *, nodeptr);   
extern double  (*makenewz)          (tree *, nodeptr, nodeptr, double, int);

extern double  (*evaluatePartial)   (tree *, nodeptr, int, double);

extern double  (*evaluatePartition) (tree *, nodeptr, int);
extern boolean (*newviewPartition)  (tree *, nodeptr, int);

extern double evaluateGTRGAMMA_ARRAY (tree *tr, nodeptr p, double *array);

extern int  randomInt(int n);
 
boolean initrav (tree *tr, nodeptr p)
  { /* initrav */
    nodeptr  q;

    if (! p->tip) 
      {
      
	q = p->next;
      
	do 
	  {	   
	    if (! initrav(tr, q->back))  return FALSE;		   
	    q = q->next;	
	  } 
	while (q != p);      
	if (! newview(tr, p)) return FALSE;	      	   
      }

    return TRUE;
  } /* initrav */





boolean initravDIST (tree *tr, nodeptr p, int distance)
  {
    nodeptr  q;

    if (! p->tip && distance > 0) 
      {
      
	q = p->next;
      
	do 
	  {
	    if (! initravDIST(tr, q->back, --distance))  return FALSE;	
	    q = q->next;	
	  } 
	while (q != p);
      
      
	if (! newview(tr, p)) return FALSE;	      
      }

    return TRUE;
  } /* initrav */

void initravPartition (tree *tr, nodeptr p, int model)
{
  nodeptr  q;
  
  if (! p->tip) 
    {      
      q = p->next;      
      do 
	{
	  initravPartition(tr, q->back, model);
	  q = q->next;	
	} 
      while (q != p);
    
      newviewPartition(tr, p, model);       
    }
} 



double partitionLikelihood(tree *tr, int model)
{  
  double result;
 
  initravPartition(tr, tr->start, model);
  initravPartition(tr, tr->start->back, model);  

  result = evaluatePartition(tr, tr->start, model); 
  /*  printf("%d %f\n", model, result);*/

  return result;
}


boolean update (tree *tr, nodeptr p)
  { /* update */
    
    nodeptr  q;
    double   z0, z;
	
    q = p->back;
    z0 = q->z;
        
    if ((z = makenewz(tr, p, q, z0, newzpercycle)) == badZ) 
      {	  
	return FALSE;
      }
    
    p->z = q->z = z;
    if (ABS(z - z0) > deltaz)  tr->smoothed = FALSE;
        
    return TRUE;
  } /* update */

void updateNNI (tree *tr, nodeptr p)
{    
  nodeptr  q;
  double   z0, z;
	
  q = p->back;
  z0 = q->z;
        
  z = makenewz(tr, p, q, z0, 1);  
    
  p->z = q->z = z;           
}

boolean smooth (tree *tr, nodeptr p)
  { /* smooth */
    nodeptr  q;
  
    if (! update(tr, p))               return FALSE; /*  Adjust branch */
    if (! p->tip) 
      {                                  /*  Adjust descendants */
        q = p->next;
        while (q != p) 
	  {
	    if (! smooth(tr, q->back))   return FALSE;
	    q = q->next;
          }	
	if (! newview(tr, p)) return FALSE;
      }

    return TRUE;
  } /* smooth */


boolean smoothTree (tree *tr, int maxtimes)
  { /* smoothTree */
    nodeptr  p, q;   

    /*printf("smoothTree %d \n", maxtimes);*/

    p = tr->start;

    while (--maxtimes >= 0) 
      {
	tr->smoothed = TRUE;
	if (! smooth(tr, p->back))       return FALSE;
	if (! p->tip) 
	  {
	    q = p->next;
	    while (q != p) 
	      {
		if (! smooth(tr, q->back))   return FALSE;
		q = q->next;
	      }
	  }
	if (tr->smoothed)  break;
      }

    return TRUE;
  } /* smoothTree */



boolean localSmooth (tree *tr, nodeptr p, int maxtimes)
  { /* localSmooth -- Smooth branches around p */
    nodeptr  q;

    if (p->tip) return FALSE;            /* Should be an error */

    while (--maxtimes >= 0) {
      tr->smoothed = TRUE;
      q = p;
      do {
        if (! update(tr, q)) return FALSE;
        q = q->next;
        } while (q != p);
      if (tr->smoothed)  break;
      }

    tr->smoothed = FALSE;             /* Only smooth locally */
    return TRUE;
  } /* localSmooth */






void resetInfoList()
{
  int i;

  iList.valid = 0;

  for(i = 0; i < iList.n; i++)    
    {
      iList.list[i].node = (nodeptr)NULL;
      iList.list[i].likelihood = unlikely;
    }    
}

void initInfoList(int n)
{
  int i;

  iList.n = n;
  iList.valid = 0;
  iList.list = (bestInfo *)malloc(sizeof(bestInfo) * n);

  for(i = 0; i < n; i++)
    {
      iList.list[i].node = (nodeptr)NULL;
      iList.list[i].likelihood = unlikely;
    }
}

void freeInfoList()
{ 
  free(iList.list);   
}


void insertInfoList(nodeptr node, double likelihood)
{
  int i;
  int min = 0;
  double min_l =  iList.list[0].likelihood;

  for(i = 1; i < iList.n; i++)
    {
      if(iList.list[i].likelihood < min_l)
	{
	  min = i;
	  min_l = iList.list[i].likelihood;
	}
    }

  if(likelihood > min_l)
    {
      iList.list[min].likelihood = likelihood;
      iList.list[min].node = node;
      iList.valid += 1;
    }

  if(iList.valid > iList.n)
    iList.valid = iList.n;
}


boolean smoothRegion (tree *tr, nodeptr p, int region)
  { /* smooth */
    nodeptr  q;
   
    if (! update(tr, p))               return FALSE; /*  Adjust branch */
    if(region > 0)
      {
	if (! p->tip) 
	  {                                 
	    q = p->next;
	    while (q != p) 
	      {
		if (! smoothRegion(tr, q->back, --region))   return FALSE;
		q = q->next;
	      }	
	    if (! newview(tr, p)) return FALSE;	
	  }
      }
    
    return TRUE;
  } /* smooth */

boolean regionalSmooth (tree *tr, nodeptr p, int maxtimes, int region)
  {
    nodeptr  q;

    if (p->tip) return FALSE;            /* Should be an error */

    while (--maxtimes >= 0) 
      {
	tr->smoothed = TRUE;
	q = p;
	do 
	  {
	    if (! smoothRegion(tr, q, region)) return FALSE;
	    q = q->next;
	  } 
	while (q != p);
	if (tr->smoothed)  break;
      }

    tr->smoothed = FALSE;             /* Only smooth locally */
    return TRUE;
  } /* localSmooth */





nodeptr  removeNodeBIG (tree *tr, nodeptr p)
{
  double   zqr;
  nodeptr  q, r;
        
  q = p->next->back;
  r = p->next->next->back;
  zqr = q->z * r->z;        

  if ((zqr = makenewz(tr, q, r, zqr, iterations)) == badZ) return (node *) NULL;      

  tr->zqr = zqr;
    
  hookup(q, r, zqr);

  p->next->next->back = p->next->back = (node *) NULL;
     
  return  q;
}

nodeptr  removeNodeRestoreBIG (tree *tr, nodeptr p)
{
  nodeptr  q, r;
        
  q = p->next->back;
  r = p->next->next->back;  

  newview(tr, q);
  newview(tr, r);
  
  hookup(q, r, tr->currentZQR);

  p->next->next->back = p->next->back = (node *) NULL;
     
  return  q;
}


boolean insertBIG (tree *tr, nodeptr p, nodeptr q, boolean glob)
{
  nodeptr  r, s;
  
  r = q->back;
  s = p->back;

  tr->lzi = q->z;

  if(Thorough)
    { 
      double  zqr, zqs, zrs, lzqr, lzqs, lzrs, lzsum, lzq, lzr, lzs, lzmax;      
            
      if ((zqr = makenewz(tr, q, r, q->z, iterations)) == badZ) return FALSE; 
      
      if ((zqs = makenewz(tr, q, s, defaultz, iterations)) == badZ) return FALSE;      
      
      if ((zrs = makenewz(tr, r, s, defaultz, iterations)) == badZ) return FALSE;
      
      lzqr = (zqr > zmin) ? log(zqr) : log(zmin); 
      lzqs = (zqs > zmin) ? log(zqs) : log(zmin);
      lzrs = (zrs > zmin) ? log(zrs) : log(zmin);
      lzsum = 0.5 * (lzqr + lzqs + lzrs);
      
      lzq = lzsum - lzrs;
      lzr = lzsum - lzqs;
      lzs = lzsum - lzqr;
      lzmax = log(zmax);
      
      if      (lzq > lzmax) {lzq = lzmax; lzr = lzqr; lzs = lzqs;} 
      else if (lzr > lzmax) {lzr = lzmax; lzq = lzqr; lzs = lzrs;}
      else if (lzs > lzmax) {lzs = lzmax; lzq = lzqs; lzr = lzrs;}          
            
      hookup(p->next,       q, exp(lzq));
      hookup(p->next->next, r, exp(lzr));
      hookup(p,             s, exp(lzs));      		  
    }
  else
    {       
      double  z = sqrt(q->z);      
   
      if(z < zmin) 
	z = zmin;
      if(z > zmax)
	z = zmax;

      hookup(p->next,       q, z);
      hookup(p->next->next, r, z);	                         
    }

  if (! newview(tr, p)) return FALSE;    
 
  if(Thorough)
    {     
      localSmooth(tr, p, smoothings);   
      tr->lzq = p->next->z;
      tr->lzr = p->next->next->z;
      tr->lzs = p->z;            
    }           
     
  return  TRUE;
}

boolean insertRestoreBIG (tree *tr, nodeptr p, nodeptr q, boolean glob)
{
  nodeptr  r, s;
  
  r = q->back;
  s = p->back;

  if(Thorough)
    {                        
      hookup(p->next,       q, tr->currentLZQ);
      hookup(p->next->next, r, tr->currentLZR);
      hookup(p,             s, tr->currentLZS);      		  
    }
  else
    { 
      
      double  z;
      
      z = sqrt(q->z);     
      if(z < zmin) 
	z = zmin;
      if(z > zmax)
	z = zmax;

      hookup(p->next,       q, z);
      hookup(p->next->next, r, z);
    }   
    
  if (! newview(tr, p)) return FALSE;    
       
  return  TRUE;
}


void restoreTopologyOnly(tree *tr, bestlist *bt)
{
  nodeptr p = tr->removeNode;
  nodeptr q = tr->insertNode;
  double qz, pz, p1z, p2z;
  nodeptr p1, p2, r, s;
  double currentLH = tr->likelihood;

  p1 = p->next->back;
  p2 = p->next->next->back;

  p1z = p1->z;
  p2z = p2->z;
  
  hookup(p1, p2, tr->currentZQR);

  p->next->next->back = p->next->back = (node *) NULL;  

  /* remove */

  /* testInsert */

  qz = q->z;
  pz = p->z;

  /* testInsert */

  /* insert */

  r = q->back;
  s = p->back;
    
 

  if(Thorough)
    {                        
      hookup(p->next,       q, tr->currentLZQ);
      hookup(p->next->next, r, tr->currentLZR);
      hookup(p,             s, tr->currentLZS);      		  
    }
  else
    { 	
      double  z;	
      z = sqrt(q->z);      
      if(z < zmin)
	z = zmin;
      if(z > zmax)
	z = zmax;
      hookup(p->next,       q, z);
      hookup(p->next->next, r, z);
    }     

  tr->likelihood = tr->bestOfNode;
   
 
  saveBestTree(bt, tr);
 
  tr->likelihood = currentLH;

  hookup(q, r, qz);

  p->next->next->back = p->next->back = (nodeptr) NULL;

  if(Thorough)
    {      
      hookup(p, s, pz);      
    }  

   hookup(p->next,       p1, p1z); 
   hookup(p->next->next, p2, p2z);      
}

boolean testInsertBIG (tree *tr, nodeptr p, nodeptr q)
{
  double  qz, pz;
  nodeptr  r;
  boolean doIt = TRUE;
  double startLH = tr->endLH;
  r = q->back; 
  qz = q->z;
  pz = p->z;
  
  if(tr->grouped)
    {
       int rNumber, qNumber, pNumber;

      doIt = FALSE;
     
      rNumber = tr->constraintVector[r->number];
      qNumber = tr->constraintVector[q->number];
      pNumber = tr->constraintVector[p->number];

      if(pNumber == -9)
	pNumber = checker(tr, p->back);
      if(pNumber == -9)
	doIt = TRUE;
      else
	{
	  if(qNumber == -9)
	    qNumber = checker(tr, q);

	  if(rNumber == -9)
	    rNumber = checker(tr, r);

	  if(pNumber == rNumber || pNumber == qNumber)
	    doIt = TRUE;       
	}
    }

    
  if(doIt)
    {     
      if (! insertBIG(tr, p, q, FALSE))       return FALSE;         
      
      evaluate(tr, p->next->next);   
      
      if(tr->likelihood != badEval)
	{	
	 
  
	  if(tr->likelihood > tr->bestOfNode)
	    {
	      tr->bestOfNode = tr->likelihood;
	      tr->insertNode = q;
	      tr->removeNode = p;   
	      tr->currentZQR = tr->zqr;           
	      tr->currentLZR = tr->lzr;
	      tr->currentLZQ = tr->lzq;
	      tr->currentLZS = tr->lzs;      
	    }
	  
	  if(tr->likelihood > tr->endLH)
	    {	
	    
	      tr->insertNode = q;
	      tr->removeNode = p;   
	      tr->currentZQR = tr->zqr;      
	      tr->endLH = tr->likelihood;                      
	    }    
	}     
      /*      else
	      printf("BAD EVAL\n");*/
      
      hookup(q, r, qz);
      
      p->next->next->back = p->next->back = (nodeptr) NULL;
      
      if(Thorough)
	{
	  nodeptr s = p->back;
	  hookup(p, s, pz);      
	} 
  
      
      if((tr->doCutoff) && (tr->likelihood < startLH))
	{
	  tr->lhAVG += (startLH - tr->likelihood);
	  tr->lhDEC++;
	  if((startLH - tr->likelihood) >= tr->lhCutoff)		    	    
	    return FALSE;	    
	  else
	    return TRUE;
	}
      else
	return TRUE;
    }
  else
    return TRUE;
  
}



 
void addTraverseBIG(tree *tr, nodeptr p, nodeptr q, int mintrav, int maxtrav)
{  
  if (--mintrav <= 0) 
    {              
      if (! testInsertBIG(tr, p, q))  return;        
    }
  
  if ((! q->tip) && (--maxtrav > 0)) 
    {    
      addTraverseBIG(tr, p, q->next->back, mintrav, maxtrav);
      addTraverseBIG(tr, p, q->next->next->back, mintrav, maxtrav);    
    }
} 





int  rearrangeBIG(tree *tr, nodeptr p, int mintrav, int maxtrav)   
  {
    double   p1z, p2z, q1z, q2z;
    nodeptr  p1, p2, q, q1, q2;
    int      mintrav2;  
    boolean doP = TRUE, doQ = TRUE;
      
    if (maxtrav < 1 || mintrav > maxtrav)  return 0;
    q = p->back;

    if(tr->constrained)
    {
      if(! tipHomogeneityChecker(tr, p->back, 0))
	doP = FALSE;
	
      if(! tipHomogeneityChecker(tr, q->back, 0))
	doQ = FALSE;
		        
      if(doQ == FALSE && doP == FALSE)
	return 0;
    }

   

    if (!p->tip && doP) 
      {     
	p1 = p->next->back;
	p2 = p->next->next->back;

	if (! p1->tip || ! p2->tip) 
	  {
	    p1z = p1->z;
	    p2z = p2->z;	   	   

	    if (! removeNodeBIG(tr, p)) return badRear;
	  
	    if (! p1->tip) 
	      {
		addTraverseBIG(tr, p, p1->next->back,
			       mintrav, maxtrav);         
		addTraverseBIG(tr, p, p1->next->next->back,
			       mintrav, maxtrav);          
	      }

	    if (! p2->tip) 
	      {
		addTraverseBIG(tr, p, p2->next->back,
			       mintrav, maxtrav);
		addTraverseBIG(tr, p, p2->next->next->back,
			       mintrav, maxtrav);          
	      }
	    
	   
	    hookup(p->next,       p1, p1z); 
	    hookup(p->next->next, p2, p2z);	   	    	    
	    initravDIST(tr, p, 1);	   	    
	  }
      }  

   
    
    if (! q->tip && maxtrav > 0 && doQ) 
      {
	q1 = q->next->back;
	q2 = q->next->next->back;

	if (! q1->tip && (!q1->next->back->tip || !q1->next->next->back->tip) ||
	    ! q2->tip && (!q2->next->back->tip || !q2->next->next->back->tip)) 
	  {
	    q1z = q1->z;
	    q2z = q2->z;

	    if (! removeNodeBIG(tr, q)) return badRear;
	  
	    mintrav2 = mintrav > 2 ? mintrav : 2;

	    if (! q1->tip) 
	      {
		addTraverseBIG(tr, q, q1->next->back,
			       mintrav2 , maxtrav);
		addTraverseBIG(tr, q, q1->next->next->back,
			       mintrav2 , maxtrav);         
	      }

	    if (! q2->tip) 
	      {
		addTraverseBIG(tr, q, q2->next->back,
			       mintrav2 , maxtrav);
		addTraverseBIG(tr, q, q2->next->next->back,
			       mintrav2 , maxtrav);          
	      }	   
	   
	    hookup(q->next,       q1, q1z); 
	    hookup(q->next->next, q2, q2z);
	   
	    initravDIST(tr, q, 1); 	   
	  }
      } 

    return  1;
  } 




void traversalOrder(nodeptr p, int *count, nodeptr *nodeArray)
{
  nodeptr r, q;

  if(p->tip)    
    return;

  nodeArray[*count] = p;
  *count = *count + 1;
      
  q = p->next;
  while(q != p)
    {    
      if(q->back->tip)
	{
	  nodeArray[*count] = q;
	  *count = *count + 1;
	}

      traversalOrder(q->back, count, nodeArray);
      q = q->next;
    }
           
}









double treeOptimizeRapid(tree *tr, int mintrav, int maxtrav, analdef *adef, bestlist *bt)
{
  int i;  
  int oldThorough;     
  boolean doIt = TRUE;  

  nodeRectifier(tr);

  if (maxtrav > tr->ntips - 3)  
    maxtrav = tr->ntips - 3;  
    
  resetInfoList();
  
  resetBestTree(bt);
 
  tr->startLH = tr->endLH = tr->likelihood;
 
  if(tr->doCutoff)
    {
      if(tr->itCount == 0)    
	tr->lhCutoff = tr->likelihood / -1000.0;    
      else    
	tr->lhCutoff = (tr->lhAVG) / ((double)(tr->lhDEC));   

      tr->itCount = tr->itCount + 1;
      tr->lhAVG = 0;
      tr->lhDEC = 0;
    }

 


  for(i = 1; i <= tr->mxtips + tr->mxtips - 2; i++)
    {           
      tr->bestOfNode = unlikely;     
      if(rearrangeBIG(tr, tr->nodep[i], mintrav, maxtrav))
	{    
	  if(Thorough)
	    {
	      if(tr->endLH > tr->startLH)                 	
		{			   	     
		  restoreTreeFast(tr);	 	 
		  tr->startLH = tr->endLH = tr->likelihood;	 
		  saveBestTree(bt, tr);
		}
	      else
		{ 		  
		  if(tr->bestOfNode != unlikely)		    	     
		    restoreTopologyOnly(tr, bt);		    
		}	   
	    }
	  else
	    {
	      insertInfoList(tr->nodep[i], tr->bestOfNode);	    
	      if(tr->endLH > tr->startLH)                 	
		{		      
		  restoreTreeFast(tr);	  	      
		  tr->startLH = tr->endLH = tr->likelihood;	  	 	  	  	  	  	  	  
		}	    	  
	    }
	}     
    }
   
  if(!Thorough)
    {           
      Thorough = 1;  
      
      for(i = 0; i < iList.valid; i++)
	{      
	 
	  tr->bestOfNode = unlikely;
	  
	  if(rearrangeBIG(tr, iList.list[i].node, mintrav, maxtrav))
	    {	  
	      if(tr->endLH > tr->startLH)                 	
		{	 	     
		  restoreTreeFast(tr);	 	 
		  tr->startLH = tr->endLH = tr->likelihood;	 
		  saveBestTree(bt, tr);
		}
	      else
		{ 
	      
		  if(tr->bestOfNode != unlikely)
		    {	     
		      restoreTopologyOnly(tr, bt);
		    }	
		}      
	    }
	}       
          
      Thorough = 0;
    }

  return tr->startLH;     
}


boolean testInsertRestoreBIG (tree *tr, nodeptr p, nodeptr q)
  { 
    nodeptr  r;

    r = q->back; 

    if(Thorough)
      {
        if (! insertBIG(tr, p, q, FALSE))       return FALSE;    
	evaluate(tr, p->next->next);

	if(tr->likelihood == badEval)
	  {
	    printf("FATAL ERROR in function  testInsertRestoreBIG likelihood is NaN\n");
	    exit(-1);
	  }

	/*	
	  if (! insertRestoreBIG(tr, p, q, FALSE))       return FALSE;
    
	  {
	  nodeptr x, y;
	  
	  x = p->next->next;
	  y = p->back;
	  while ((! x->x) || (! y->x)) 
	  {
	  if (! (x->x)) if (! newview(tr, x)) return badEval;
	  if (! (y->x)) if (! newview(tr, y)) return badEval;
	  }
	  }
	  
	  tr->likelihood = tr->endLH;
	*/

      }
    else
      {
	if (! insertRestoreBIG(tr, p, q, FALSE))       return FALSE;
    
	{
	  nodeptr x, y;
	  x = p->next->next;
	  y = p->back;
	 
	

	  if(!x->tip && y->tip)
	    {
	      while ((! x->x)) 
		{
		  if (! (x->x)) if (! newview(tr, x)) return badEval;		     
		}
	    }
	  if(x->tip && !y->tip)
	    {
	      while ((! y->x)) 
		{		  
		  if (! (y->x)) if (! newview(tr, y)) return badEval;
		}
	    }
	  if(!x->tip && !y->tip)
	    {
	      while ((! x->x) || (! y->x)) 
		{
		  if (! (x->x)) if (! newview(tr, x)) return badEval;
		  if (! (y->x)) if (! newview(tr, y)) return badEval;
		}
	    }				      	
	  
	}
	
	tr->likelihood = tr->endLH;
      }
     
    return TRUE;
  } /* testInsert */

void restoreTreeFast(tree *tr)
{
  removeNodeRestoreBIG(tr, tr->removeNode);    
  testInsertRestoreBIG(tr, tr->removeNode, tr->insertNode);
}


int determineRearrangementSetting(tree *tr,  analdef *adef, bestlist *bestT, bestlist *bt)
{
  int i, mintrav, maxtrav, bestTrav, impr;
  double rTime, startLH;
  int MaxFast;
  boolean cutoff;


  MaxFast = 26;

  startLH = tr->likelihood;

  cutoff = tr->doCutoff;
  tr->doCutoff = FALSE;
 
    
  mintrav = 1;
  maxtrav = 5;

  bestTrav = maxtrav = 5;

  impr = 1;

  resetBestTree(bt);

  while(impr && maxtrav < MaxFast)
    {	
      recallBestTree(bestT, 1, tr);     
      
      rTime = gettime();
      if (maxtrav > tr->ntips - 3)  
	maxtrav = tr->ntips - 3;    
 
      tr->startLH = tr->endLH = tr->likelihood;

      for(i = 1; i <= tr->mxtips + tr->mxtips - 2; i++)
	{                
	 
	  tr->bestOfNode = unlikely;
	  if(rearrangeBIG(tr, tr->nodep[i], mintrav, maxtrav))
	    {
	      
	      if(tr->endLH > tr->startLH)                 	
		{		 	 	      
		  restoreTreeFast(tr);	        	  	 	  	      
		  tr->startLH = tr->endLH = tr->likelihood;	  	 	  	  	  	  	  	  	      
		}	         	       	
	    }
	}

      treeEvaluate(tr, 0.25);
      saveBestTree(bt, tr);                                    

      /*      printf("%d %f\n", maxtrav, tr->likelihood);*/

      if(tr->likelihood > startLH)
	{	 
	  startLH = tr->likelihood; 	  	  
	  printLog(tr, adef, FALSE);
	  bestTrav = maxtrav;	 
	  impr = 1;
	}
      else
	{
	  impr = 0;
	}
      maxtrav += 5;

      if(tr->doCutoff)
	{
	  tr->lhCutoff = (tr->lhAVG) / ((double)(tr->lhDEC));       
  
	  tr->itCount =  tr->itCount + 1;
	  tr->lhAVG = 0;
	  tr->lhDEC = 0;
	}
    }

  recallBestTree(bt, 1, tr);   
  tr->doCutoff = cutoff;

  return bestTrav;     
}



void computeBIGRAPID (tree *tr, analdef *adef) 
{ 
  int i, rearrangementsMax, rearrangementsMin, noImproveCount, impr, goodTrav, bestTrav, which, 
    count = 0;                
  double lh, previousLh, difference, epsilon, currentLH, bestLH;              
  bestlist *bestT, *bt;  

  /*printf("PATTERNS %d\n", tr->cdta->endsite);*/


  
  bestT = (bestlist *) malloc(sizeof(bestlist));
  bestT->ninit = 0;
  initBestTree(bestT, 1, tr->mxtips, tr->cdta->endsite);
      
  bt = (bestlist *) malloc(sizeof(bestlist));      
  bt->ninit = 0;
  initBestTree(bt, 20, tr->mxtips, tr->cdta->endsite); 

  initInfoList(50);
 
  difference = 10.0;
  epsilon = 0.01;    
    
  Thorough = 0; 

  optimizeModel(tr, adef, 1);     
  treeEvaluate(tr, 2);   
   
  printLog(tr, adef, FALSE);
 
  saveBestTree(bestT, tr);
   
  if(!adef->initialSet)   
    bestTrav = adef->bestTrav = determineRearrangementSetting(tr, adef, bestT, bt);                   
  else
    bestTrav = adef->bestTrav = adef->initial;

  saveBestTree(bestT, tr); 
  impr = 1;
  if(tr->doCutoff)
    tr->itCount = 0;

  while(impr)
    {         
      recallBestTree(bestT, 1, tr);
      optimizeModel(tr, adef, 1);    	 
      treeEvaluate(tr, 2);	 	                    
      saveBestTree(bestT, tr);     
      printLog(tr, adef, FALSE);
      printResult(tr, adef, FALSE);
     
      lh = previousLh = tr->likelihood;
         
      treeOptimizeRapid(tr, 1, bestTrav, adef, bt);   
      impr = 0;
	  
      for(i = 1; i <= bt->nvalid; i++)
	{	    		  	   
	  recallBestTree(bt, i, tr);	    
	  treeEvaluate(tr, 0.25);	    	 	
	      
	  difference = ((tr->likelihood > previousLh)? 
			tr->likelihood - previousLh: 
			previousLh - tr->likelihood); 	    
	  if(tr->likelihood > lh && difference > epsilon)
	    {
	      impr = 1;	       
	      lh = tr->likelihood;	       	     
	      saveBestTree(bestT, tr);
	    }	   	   
	}	
    }

  Thorough = 1;
  impr = 1;
  
  while(1)
    {		
      recallBestTree(bestT, 1, tr);    
      if(impr)
	{	    
	  printResult(tr, adef, FALSE);
	  rearrangementsMin = 1;
	  rearrangementsMax = adef->stepwidth;	    
	}			  			
      else
	{		       	   
	  rearrangementsMax += adef->stepwidth;
	  rearrangementsMin += adef->stepwidth; 	        	      
	  if(rearrangementsMax > adef->max_rearrange)	     	     	 
	    goto cleanup; 	   
	}
      
      optimizeModel(tr, adef, 1);       
      treeEvaluate(tr, 2.0);	      
      previousLh = lh = tr->likelihood;	      
      saveBestTree(bestT, tr);     
      printLog(tr, adef, FALSE);

      treeOptimizeRapid(tr, rearrangementsMin, rearrangementsMax, adef, bt);
	
      impr = 0;			      	
		
      for(i = 1; i <= bt->nvalid; i++)
	{	

	  recallBestTree(bt, i, tr);	    
	  
	  treeEvaluate(tr, 0.25);	    	 
	  
	  difference = ((tr->likelihood > previousLh)? 
			tr->likelihood - previousLh: 
			previousLh - tr->likelihood); 	    
	  if(tr->likelihood > lh && difference > epsilon)
	    {
	      impr = 1;	       
	      lh = tr->likelihood;	  	     
	      saveBestTree(bestT, tr);
	    }	   	   
	}	
    }

 cleanup:   
  freeBestTree(bestT);
  free(bestT);
  freeBestTree(bt);
  free(bt);
  freeInfoList();
  printLog(tr, adef, TRUE);
  printResult(tr, adef, TRUE);
}

boolean treeEvaluate (tree *tr, double smoothFactor)       /* Evaluate a user tree */
  { /* treeEvaluate */
    
    /*double inLH = tr->likelihood;*/

    if (! smoothTree(tr, (int)((double)smoothings * smoothFactor))) 
      {
	return FALSE;      
      }
      
    evaluate(tr, tr->start);

    if(tr->likelihood == badEval)
      {
	printf("FATAL ERROR in function treeEvaluate likelihood is NaN\n");
	exit(-1);
      }


    /*    if(inLH > tr->likelihood)
      {
	printf("FATAL error in treeEvaluate %.20f <-> %.20f factor %d\n", inLH, tr->likelihood, (int)((double)smoothings * smoothFactor));
	}*/

    return TRUE;
  } /* treeEvaluate */


