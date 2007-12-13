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
#include <limits.h>
#include <math.h>
#include <time.h> 
#include <stdlib.h>
#include <stdio.h>
#include <ctype.h>
#include <string.h>

#include "axml.h"

extern double masterTime;
extern int allocCount;
extern const int protTipParsimonyValue[23];

extern void    (*newviewParsimony)  ( tree *, nodeptr);
extern int     (*evaluateParsimony) ( tree *, nodeptr);


/********************************DNA FUNCTIONS *****************************************************************/

void newviewParsimonyDNA(tree *tr, nodeptr  p)
{  
  if(p->tip) return;
  
  {
    char *right, *left;
    char le, ri, t;
    parsimonyVector *rightVector, *leftVector, *thisVector;
    int i, ts;    
    nodeptr  q, r; 
    
    q = p->next->back;
    r = p->next->next->back;      

    if(r->tip && q->tip)
      {	  	 	       
 	
	while ((! p->x)) 
	  {     
	    if (! p->x) 
	      getxnode(p);	
	  }
	left = r->tip;
	right = q->tip;
	thisVector = p->x;		
#ifdef _OPENMP
#pragma omp parallel for private(le, ri, t, ts)
#endif
	for(i = 0; i < tr->parsimonyLength; i++)
	   {
	     le = left[i];
	     ri = right[i];
	     
	     t = le & ri;
	     
	     ts = 0;
	     
	     if(!t)
	       {
		 t = le | ri;
		 ts = 1;
	       }
	    	      
	     thisVector[i].parsimonyScore = ts;	       	    
	     thisVector[i].parsimonyState = t;
	     
	   }
	return;
      }

    if(r->tip && !q->tip)
      {	

	while ((! p->x) || (! q->x)) 
	  {	 
	    if(! q->x) 
	      newviewParsimonyDNA(tr, q);
	    if (! p->x)
	      getxnode(p);	
	  }
	left = r->tip;
	rightVector = q->x;
	thisVector  = p->x;            
#ifdef _OPENMP
#pragma omp parallel for private(le, ri, t, ts)
#endif
	for(i = 0; i < tr->parsimonyLength; i++)
	   {
	     le = left[i];
	     ri = rightVector[i].parsimonyState;
	     
	     t = le & ri;
	     
	     ts = rightVector[i].parsimonyScore;
	     
	     if(!t)
	       {
		 t = le | ri;
		 ts++;
	       }

	     thisVector[i].parsimonyScore = ts;	       	    
	     thisVector[i].parsimonyState = t;	    	     
	   }
	return;
      }

     if(!r->tip && q->tip)
       {	
	 

	 
	 
	 while ((! p->x) || (! r->x)) 
	   {	 
	     
	     if (! r->x) 
	       newviewParsimonyDNA(tr, r);
	    
	     if (! p->x) 
	       getxnode(p);	
	   }
	  
	 leftVector = r->x;
	 right = q->tip;
	 thisVector = p->x;	    
#ifdef _OPENMP
#pragma omp parallel for private(le, ri, t, ts)
#endif
	 for(i = 0; i < tr->parsimonyLength; i++)
	   {
	     le = leftVector[i].parsimonyState;
	     ri = right[i];
	     
	     t = le & ri;
	     
	     ts = leftVector[i].parsimonyScore;
	     
	     if(!t)
	       {
		 t = le | ri;
		 ts++;
	       }

	     thisVector[i].parsimonyScore = ts;	       	    
	     thisVector[i].parsimonyState = t;	   	     
	   }	
	 return;
       }

     if(!r->tip && !q->tip)
       {
	
	

	 while ((! p->x) || (! q->x) || (! r->x)) 
	   {
	     if (! q->x) 
	       newviewParsimonyDNA(tr, q);
	     if (! r->x)
	       newviewParsimonyDNA(tr, r);
	     if (! p->x)
	       getxnode(p);	
	   }

	 leftVector = r->x;
	 rightVector = q->x;
	 thisVector = p->x;	
#ifdef _OPENMP
#pragma omp parallel for private(le, ri, t, ts)
#endif
	 for(i = 0; i < tr->parsimonyLength; i++)
	   {
	     le = leftVector[i].parsimonyState;
	     ri = rightVector[i].parsimonyState;
	     
	     t = le & ri;
	     
	     ts = rightVector[i].parsimonyScore + leftVector[i].parsimonyScore;
	     
	     if(!t)
	       {
		 t = le | ri;
		 ts++;
	       }

	     thisVector[i].parsimonyScore = ts;	       	    
	     thisVector[i].parsimonyState = t;	 
	     	     
	   }

       }	         
  }
}



int evaluateParsimonyDNA(tree *tr, nodeptr p)
{
  int sum;   
  char *right, *left;
  char le, ri, t;
  parsimonyVector *rightVector, *leftVector;
  int i, ts;
  nodeptr  q = p->back;
  int     *wptr = tr->cdta->aliaswgt;
  int acc = 0; 
   
  if(q->tip && !p->tip)
    {
      while ((! p->x)) 
	{      
	  if (! p->x)
	    newviewParsimonyDNA(tr, p);	    	
	}

       leftVector = p->x;
       right = q->tip;     
#ifdef _OPENMP	    	
#pragma omp parallel for private(le, ri, t, sum)  reduction(+ : acc)
#endif
       for(i = 0; i < tr->parsimonyLength; i++)
	 {
	   le = leftVector[i].parsimonyState;
	   ri = right[i];
	     
	   t = le & ri;
	  
	   sum = leftVector[i].parsimonyScore;
	     
	   if(!t)
	     {
	       t = le | ri;
	       sum++;
	     }	 	  	   

	  acc += sum *  wptr[i];	  		 	       
	}     
      
       return acc;
    }
  
  if(!q->tip && p->tip)
    {
      while ((! q->x)) 
	{
	  if (! q->x) 
	    newviewParsimonyDNA(tr, q);       
	}

      left = p->tip;
      rightVector = q->x;     
#ifdef _OPENMP	         
#pragma omp parallel for private(le, ri, t, sum)  reduction(+ : acc)	
#endif
      for(i = 0; i < tr->parsimonyLength; i++)
	{
	  le = left[i];
	  ri = rightVector[i].parsimonyState;
	     
	  t = le & ri;
	  
	  sum = rightVector[i].parsimonyScore;
	     
	  if(!t)
	    {
	      t = le | ri;
	      sum++;
	    }
	  	  	  
	  acc += sum *  wptr[i];     
	}       
    	
      return acc;
    }
  

  
  
  if(!q->tip && !p->tip)
    {
      while ((! p->x) || (! q->x)) 
	{
	  if (! q->x) 
	    newviewParsimonyDNA(tr, q);
	  if (! p->x)
	    newviewParsimonyDNA(tr, p);	    	
	}

      leftVector = p->x;
      rightVector = q->x;     	    
#ifdef _OPENMP
#pragma omp parallel for private(le, ri, t, sum)  reduction(+ : acc)	
#endif
      for(i = 0; i < tr->parsimonyLength; i++)
	{
	  le = leftVector[i].parsimonyState;
	  ri = rightVector[i].parsimonyState;
	     
	  t = le & ri;
	  
	  sum = rightVector[i].parsimonyScore + leftVector[i].parsimonyScore;
	     
	  if(!t)
	    {
	      t = le | ri;
	      sum++;
	    }
	  		  
	  acc += sum *  wptr[i];     
	}       	       
     
    }
  
  return acc;
}


/***********************************PROT FUNCTIONS**************************************************************************************/






void newviewParsimonyPROT(tree *tr, nodeptr  p)
{  
  if(p->tip) return;
  
  {
    char *right, *left;
    int le, ri, t;
    parsimonyVectorProt *rightVector, *leftVector, *thisVector;
    int i, ts;    
    nodeptr  q, r; 
    
    q = p->next->back;
    r = p->next->next->back;      

    if(r->tip && q->tip)
      {	  	 	       
 	
	while ((! p->x)) 
	  {     
	    if (! p->x) 
	      getxnode(p);	
	  }
	left = r->tip;
	right = q->tip;
	thisVector = p->x;		
#ifdef _OPENMP
#pragma omp parallel for private(le, ri, t, ts)
#endif
	for(i = 0; i < tr->parsimonyLength; i++)
	   {
	     le = protTipParsimonyValue[left[i]];
	     ri = protTipParsimonyValue[right[i]];
	     
	     t = le & ri;
	     
	     ts = 0;
	     
	     if(!t)
	       {
		 t = le | ri;
		 ts = 1;
	       }
	    	      
	     thisVector[i].parsimonyScore = ts;	       	    
	     thisVector[i].parsimonyState = t;
	     
	   }
	return;
      }

    if(r->tip && !q->tip)
      {	

	while ((! p->x) || (! q->x)) 
	  {	 
	    if(! q->x) 
	      newviewParsimonyPROT(tr, q);
	    if (! p->x)
	      getxnode(p);	
	  }
	left = r->tip;
	rightVector = q->x;
	thisVector  = p->x;            
#ifdef _OPENMP
#pragma omp parallel for private(le, ri, t, ts)
#endif
	for(i = 0; i < tr->parsimonyLength; i++)
	   {
	     le = protTipParsimonyValue[left[i]];
	     ri = rightVector[i].parsimonyState;
	     
	     t = le & ri;
	     
	     ts = rightVector[i].parsimonyScore;
	     
	     if(!t)
	       {
		 t = le | ri;
		 ts++;
	       }

	     thisVector[i].parsimonyScore = ts;	       	    
	     thisVector[i].parsimonyState = t;	    	     
	   }
	return;
      }

     if(!r->tip && q->tip)
       {	
	 	 	 
	 while ((! p->x) || (! r->x)) 
	   {	 
	     
	     if (! r->x) 
	       newviewParsimonyPROT(tr, r);
	    
	     if (! p->x) 
	       getxnode(p);	
	   }
	  
	 leftVector = r->x;
	 right = q->tip;
	 thisVector = p->x;	    
#ifdef _OPENMP
#pragma omp parallel for private(le, ri, t, ts)
#endif
	 for(i = 0; i < tr->parsimonyLength; i++)
	   {
	     le = leftVector[i].parsimonyState;
	     ri = protTipParsimonyValue[right[i]];
	     
	     t = le & ri;
	     
	     ts = leftVector[i].parsimonyScore;
	     
	     if(!t)
	       {
		 t = le | ri;
		 ts++;
	       }

	     thisVector[i].parsimonyScore = ts;	       	    
	     thisVector[i].parsimonyState = t;	   	     
	   }	
	 return;
       }

     if(!r->tip && !q->tip)
       {
		
	 while ((! p->x) || (! q->x) || (! r->x)) 
	   {
	     if (! q->x) 
	       newviewParsimonyPROT(tr, q);
	     if (! r->x)
	       newviewParsimonyPROT(tr, r);
	     if (! p->x)
	       getxnode(p);	
	   }

	 leftVector = r->x;
	 rightVector = q->x;
	 thisVector = p->x;	
#ifdef _OPENMP
#pragma omp parallel for private(le, ri, t, ts)
#endif
	 for(i = 0; i < tr->parsimonyLength; i++)
	   {
	     le = leftVector[i].parsimonyState;
	     ri = rightVector[i].parsimonyState;
	     
	     t = le & ri;
	     
	     ts = rightVector[i].parsimonyScore + leftVector[i].parsimonyScore;
	     
	     if(!t)
	       {
		 t = le | ri;
		 ts++;
	       }

	     thisVector[i].parsimonyScore = ts;	       	    
	     thisVector[i].parsimonyState = t;	 
	     	     
	   }

       }	         
  }
}



int evaluateParsimonyPROT(tree *tr, nodeptr p)
{
  int sum;   
  char *right, *left;
  int le, ri, t;
  parsimonyVectorProt *rightVector, *leftVector;
  int i, ts;
  nodeptr  q = p->back;
  int     *wptr = tr->cdta->aliaswgt;
  int acc = 0; 
   
  if(q->tip && !p->tip)
    {
      while ((! p->x)) 
	{      
	  if (! p->x)
	    newviewParsimonyPROT(tr, p);	    	
	}

       leftVector = p->x;
       right = q->tip;     
#ifdef _OPENMP	    	
#pragma omp parallel for private(le, ri, t, sum)  reduction(+ : acc)
#endif
       for(i = 0; i < tr->parsimonyLength; i++)
	 {
	   le = leftVector[i].parsimonyState;
	   ri = protTipParsimonyValue[right[i]];
	     
	   t = le & ri;
	  
	   sum = leftVector[i].parsimonyScore;
	     
	   if(!t)
	     {
	       t = le | ri;
	       sum++;
	     }	 	  	   

	  acc += sum *  wptr[i];	  		 	       
	}     
      
       return acc;
    }
  
  if(!q->tip && p->tip)
    {
      while ((! q->x)) 
	{
	  if (! q->x) 
	    newviewParsimonyPROT(tr, q);       
	}

      left = p->tip;
      rightVector = q->x;     
#ifdef _OPENMP	         
#pragma omp parallel for private(le, ri, t, sum)  reduction(+ : acc)   
#endif
      for(i = 0; i < tr->parsimonyLength; i++)
	{
	  le = protTipParsimonyValue[left[i]];
	  ri = rightVector[i].parsimonyState;
	     
	  t = le & ri;
	  
	  sum = rightVector[i].parsimonyScore;
	     
	  if(!t)
	    {
	      t = le | ri;
	      sum++;
	    }
	  	  	  
	  acc += sum *  wptr[i];     
	}       
    	
      return acc;
    }
  

  
  
  if(!q->tip && !p->tip)
    {
      while ((! p->x) || (! q->x)) 
	{
	  if (! q->x) 
	    newviewParsimonyPROT(tr, q);
	  if (! p->x)
	    newviewParsimonyPROT(tr, p);	    	
	}

      leftVector = p->x;
      rightVector = q->x;     	    
#ifdef _OPENMP
#pragma omp parallel for private(le, ri, t, sum)  reduction(+ : acc)	
#endif
      for(i = 0; i < tr->parsimonyLength; i++)
	{
	  le = leftVector[i].parsimonyState;
	  ri = rightVector[i].parsimonyState;
	     
	  t = le & ri;
	  
	  sum = rightVector[i].parsimonyScore + leftVector[i].parsimonyScore;
	     
	  if(!t)
	    {
	      t = le | ri;
	      sum++;
	    }
	  		  
	  acc += sum *  wptr[i];     
	}       	       
     
    }
  
  return acc;
}



/****************************************************************************************************************************************/

void initravParsimonyNormal(tree *tr, nodeptr p)
  {
    nodeptr  q;

    if (! p->tip) 
      {
	q = p->next;
      
	do 
	  {
	    initravParsimonyNormal(tr, q->back);
	    q = q->next;	
	  } 
	while (q != p);
            
	newviewParsimony(tr, p);	      
      }
  }





void initravParsimony(tree *tr, nodeptr p, int *constraintVector)
  {
    nodeptr  q;

    if (! p->tip) 
      {    
	q = p->next;
      
	do 
	  {
	    initravParsimony(tr, q->back, constraintVector);
	    q = q->next;	
	  } 
	while (q != p);
            
	newviewParsimony(tr, p);	      
      }
    else
      constraintVector[p->number] = 1;
  }

void insertParsimony (tree *tr, nodeptr p, nodeptr q)
{
  nodeptr  r, s;
  
  r = q->back;
  s = p->back;
  
  hookup(p->next,       q, defaultz);
  hookup(p->next->next, r, defaultz); 
   
  newviewParsimony(tr, p);     
} 

void insertRandom (nodeptr p, nodeptr q)
{
  nodeptr  r;
  
  r = q->back;
  
  hookup(p->next,       q, defaultz);
  hookup(p->next->next, r, defaultz); 
} 


nodeptr buildNewTip (tree *tr, nodeptr p)
{ 
  nodeptr  q;

  q = tr->nodep[(tr->nextnode)++];
  hookup(p, q, defaultz);
  return  q;
} 

void buildSimpleTree (tree *tr, int ip, int iq, int ir)
{    
  nodeptr  p, s;
  int  i;
  
  i = MIN(ip, iq);
  if (ir < i)  i = ir; 
  tr->start = tr->nodep[i];
  tr->ntips = 3;
  p = tr->nodep[ip];
  hookup(p, tr->nodep[iq], defaultz);
  s = buildNewTip(tr, tr->nodep[ir]);
  insertParsimony(tr, s, p);
}


void buildSimpleTreeRandom (tree *tr, int ip, int iq, int ir)
{    
  nodeptr  p, s;
  int  i;
  
  i = MIN(ip, iq);
  if (ir < i)  i = ir; 
  tr->start = tr->nodep[i];
  tr->ntips = 3;
  p = tr->nodep[ip];
  hookup(p, tr->nodep[iq], defaultz);
  s = buildNewTip(tr, tr->nodep[ir]);
  insertRandom( s, p);
}

int checker(tree *tr, nodeptr p)
{
  int group = tr->constraintVector[p->number];

  if(p->tip)
    {
      group = tr->constraintVector[p->number];
      return group;
    }
  else
    {
      if(group != -9) 
	return group;

      group = checker(tr, p->next->back);
      if(group != -9) 
	return group;

      group = checker(tr, p->next->next->back);
      if(group != -9) 
	return group;

      return -9;
    }
}


void testInsertParsimony (tree *tr, nodeptr p, nodeptr q)
{ 
  int mp;
  boolean doIt = TRUE;
  nodeptr  r = q->back;
 
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
      insertParsimony(tr, p, q);   
      mp = evaluateParsimony(tr, p->next->next);          
      
      if(mp < tr->bestParsimony)
	{
	  tr->bestParsimony = mp;
	  tr->insertNode = q;
	  tr->removeNode = p;
	}
      
      hookup(q, r, defaultz);
      p->next->next->back = p->next->back = (nodeptr) NULL;

    }      

  return;
} 


void restoreTreeParsimony(tree *tr, nodeptr p, nodeptr q)
{
  nodeptr  r = q->back;
 
  insertParsimony(tr, p, q);  

  if(!p->tip && q->tip)
    {
      while ((! p->x)) 
	{
	  if (! (p->x))
	    newviewParsimony(tr, p);		     
	}
    }
  if(p->tip && !q->tip)
    {
      while ((! q->x)) 
	{		  
	  if (! (q->x)) 
	    newviewParsimony(tr, q);
	}
    }
  if(!p->tip && !q->tip)
    {
      while ((! p->x) || (! q->x)) 
	{
	  if (! (p->x))
	    newviewParsimony(tr, p);
	  if (! (q->x))
	    newviewParsimony(tr, q);
	}
    }	
}


int markBranches(nodeptr *branches, nodeptr p, int *counter)
{
  if(p->tip)
    return 0;
  else
    {
      branches[*counter] = p->next;
      branches[*counter + 1] = p->next->next;
      
      *counter = *counter + 2;
      
      return 2 + markBranches(branches, p->next->back, counter) + markBranches(branches, p->next->next->back, counter);
    }
}

void addTraverseParsimony (tree *tr, nodeptr p, nodeptr q, int mintrav, int maxtrav)
  {       
    if (--mintrav <= 0) 
      {
	
	testInsertParsimony(tr, p, q);	
	
      }

    if ((! q->tip) && (--maxtrav > 0)) 
      {	
	addTraverseParsimony(tr, p, q->next->back, mintrav, maxtrav);	
	addTraverseParsimony(tr, p, q->next->next->back, mintrav, maxtrav);              	
      }
  }


nodeptr findAnyTip(nodeptr p)
{ 
  return  p->tip ? p : findAnyTip(p->next->back);
} 


int randomInt(int n)
{
  return rand() %n;
}

void makePermutation(int *perm, int n, analdef *adef)
{    
  int  i, j, k;
  int sum1 = 0, sum2 = 0;

#ifdef PARALLEL
   srand((unsigned int) gettimeSrand()); 
#endif

#ifndef PARALLEL  
  if(adef->parsimonySeed == 0)   
    srand((unsigned int) gettimeSrand());          
  else
    srand(adef->parsimonySeed);
#endif

  for (i = 1; i <= n; i++)
    {
      perm[i] = i;      
      sum1 += i;
    }

  for (i = 1; i <= n; i++) 
    {
      k        = randomInt(n + 1 - i);
      j        = perm[i];
      perm[i]     = perm[i + k];
      perm[i + k] = j; 
    }
}

void initravDISTParsimony (tree *tr, nodeptr p, int distance)
{
  nodeptr  q;

  if (! p->tip && distance > 0) 
    {      
      q = p->next;      
      do 
	{
	  initravDISTParsimony(tr, q->back, --distance);	
	  q = q->next;	
	} 
      while (q != p);
      
      
      newviewParsimony(tr, p);	      
    } 
}


nodeptr  removeNodeParsimony (tree *tr, nodeptr p)
{ 
  nodeptr  q, r;         

  q = p->next->back;
  r = p->next->next->back;   
    
  hookup(q, r, defaultz);

  p->next->next->back = p->next->back = (node *) NULL;
  
  return  q;
}



boolean tipHomogeneityChecker(tree *tr, nodeptr p, int grouping)
{
  if(p->tip)
    {
      if(tr->constraintVector[p->number] != grouping) 
	return FALSE;
      else 
	return TRUE;
    }
  else
    {   
      return  (tipHomogeneityChecker(tr, p->next->back, grouping) && tipHomogeneityChecker(tr, p->next->next->back,grouping));      
    }
}

int rearrangeParsimony(tree *tr, nodeptr p, int mintrav, int maxtrav)  
{   
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
	  removeNodeParsimony(tr, p);
	  
	  if (! p1->tip) 
	    {
	      addTraverseParsimony(tr, p, p1->next->back, mintrav, maxtrav);         
	      addTraverseParsimony(tr, p, p1->next->next->back, mintrav, maxtrav);          
	    }

	  if (! p2->tip) 
	    {
	      addTraverseParsimony(tr, p, p2->next->back, mintrav, maxtrav);
	      addTraverseParsimony(tr, p, p2->next->next->back, mintrav, maxtrav);          
	    }
	    
	   
	  hookup(p->next,       p1, defaultz); 
	  hookup(p->next->next, p2, defaultz);	   	    	    
	  initravDISTParsimony(tr, p, 1);   
	}
    }  
       
  if (! q->tip && maxtrav > 0 && doQ) 
    {
      q1 = q->next->back;
      q2 = q->next->next->back;

      if (! q1->tip && (!q1->next->back->tip || !q1->next->next->back->tip) ||
	  ! q2->tip && (!q2->next->back->tip || !q2->next->next->back->tip)) 
	{	   

	  removeNodeParsimony(tr, q);
	  
	  mintrav2 = mintrav > 2 ? mintrav : 2;

	  if (! q1->tip) 
	    {
	      addTraverseParsimony(tr, q, q1->next->back, mintrav2 , maxtrav);
	      addTraverseParsimony(tr, q, q1->next->next->back, mintrav2 , maxtrav);         
	    }

	  if (! q2->tip) 
	    {
	      addTraverseParsimony(tr, q, q2->next->back, mintrav2 , maxtrav);
	      addTraverseParsimony(tr, q, q2->next->next->back, mintrav2 , maxtrav);          
	    }	   
	   
	  hookup(q->next,       q1, defaultz); 
	  hookup(q->next->next, q2, defaultz);
	   
	  initravDISTParsimony(tr, q, 1); 	   
	}
    }

  return 1;
} 


void restoreTreeRearrangeParsimony(tree *tr)
{    
  removeNodeParsimony(tr, tr->removeNode);  
  restoreTreeParsimony(tr, tr->removeNode, tr->insertNode);  
}

void allocNodexParsimony (tree *tr, analdef *adef)
{
  nodeptr  p;
  int  i;  
  int npat = tr->parsimonyLength; 
 

  if(adef->model == M_PROTCAT || adef->model == M_PROTGAMMA)
    {     
      newviewParsimony = newviewParsimonyPROT;     
      evaluateParsimony = evaluateParsimonyPROT;      
    }
  else
    {
      newviewParsimony = newviewParsimonyDNA;
      evaluateParsimony = evaluateParsimonyDNA;
    }

  for (i = tr->mxtips + 1; (i <= 2*(tr->mxtips) - 2); i++) 
    {       
      p = tr->nodep[i];    
      if(adef->model == M_PROTCAT || adef->model == M_PROTGAMMA)
	p->x = malloc(sizeof(parsimonyVectorProt)   * tr->parsimonyLength);
      else
	p->x = malloc(sizeof(parsimonyVector)   * tr->parsimonyLength);
      allocCount++;
      p->next->x = NULL;
      p->next->next->x = NULL;
    }
}


void freeNodexParsimony (tree *tr)
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
      p->x = NULL;
    }

  if(allocCount != 0)
    {
      printf("FATAL error\n");
      exit(-1);
    }
}

void restore(tree *tr, int *alias)
{
  int i, j, k, buf, n;

  for(i = 0; i < tr->cdta->endsite; i++)
    {
      if(alias[i] != i)
	{
	  j = i;
	  k = alias[i];
	  buf = tr->cdta->aliaswgt[j];
	  tr->cdta->aliaswgt[j] = tr->cdta->aliaswgt[k];
	  tr->cdta->aliaswgt[k] = buf;
	  	 	
	  for(n = 1; n <= tr->mxtips; n++)
	    {
	      buf = tr->nodep[n]->tip[j];
	      tr->nodep[n]->tip[j] = tr->nodep[n]->tip[k];
	      tr->nodep[n]->tip[k] = buf;	      	     	      	     	      
	    }
	}
    }
}

void sortInformativeSites(tree *tr, int *informative, int *alias)
{
  int i, j, k, n, buf;

  for(i = 0; i < tr->cdta->endsite; i++)
    alias[i] = i;

  i = 0;

  while(i < tr->cdta->endsite)
    {
      j = i;
      while(informative[j] && j < tr->cdta->endsite)	
	j++;
	  
      /* j is uninformative */    
         
      k = tr->cdta->endsite - 1;
      while(!informative[k] && k > j)
	k--;

      /* k is informative */

      
      if(k > j)
	{ 
	  /*printf("swapping %d <-> %d, %d <-> %d\n", j, k, informative[j], informative[k]);*/
	  buf = informative[j];
	  informative[j] = informative[k];
	  informative[k] = buf;

	  buf = tr->cdta->aliaswgt[j];
	  tr->cdta->aliaswgt[j] = tr->cdta->aliaswgt[k];
	  tr->cdta->aliaswgt[k] = buf;
	  	 
	  alias[k] = j;
	
	  for(n = 1; n <= tr->mxtips; n++)
	    {
	      buf = tr->nodep[n]->tip[j];
	      tr->nodep[n]->tip[j] = tr->nodep[n]->tip[k];
	      tr->nodep[n]->tip[k] = buf;	      	     	      	     	      
	    }
	}
      i = j + 1;
    }  
}

void determineUninformativeSites(tree *tr, int *informative, int *alias)
{
  int i, j;
  int check[16];
  int nucleotide;
  int informativeCounter;      
  int real;
  int erg;
  int number = 0;

  for(i = 0; i < tr->cdta->endsite; i++)
    {            
      for(j = 1; j < 16; j++)
	check[j] = 0;
      
      for(j = 1; j <= tr->mxtips; j++)
	{
	  nucleotide = tr->nodep[j]->tip[i];
	  check[nucleotide] =  check[nucleotide] + 1;
	}

      informativeCounter = 0;
      for(j = 1; j < 15; j++)
	{
	  if(check[j] >= 2)
	    informativeCounter++;    
	} 
        
      if(informativeCounter >= 2)
	{
	  informative[i] = 1;       
	}
      else
	{
	  real = 15;

	  for(j = 1; j < 15; j++)
	    {	     
	      if(check[j] > 0)
		{
		  erg = j;
		  real = real & erg;
		}
	    }
	  if(real > 0)
	    {
	      informative[i] = 0;
	      number++;
	    }
	  else
	    {	     
	      informative[i] = 1;
	    }
	}           
     }

  sortInformativeSites(tr, informative, alias);

  /*printf("Uninformative sites: %d\n", number);*/
  tr->parsimonyLength = tr->cdta->endsite - number;
}



void makeRandomTree(tree *tr, analdef *adef)
{ 
  char   filename[128];    
  nodeptr  q, p, f, randomBranch;    
  int  i, j, k, nextsp, newsp, mintrav, maxtrav, randomMP, startMP;
  int *perm, branchCounter;
  nodeptr *branches;
  
  branches = (nodeptr *)malloc(sizeof(nodeptr) * (2 * tr->mxtips));
  perm = (int *)malloc((tr->mxtips + 1) * sizeof(int));                         
  
  makePermutation(perm, tr->mxtips, adef);              
  
  tr->ntips = 0;       	       
  tr->nextnode = tr->mxtips + 1;    
  
  buildSimpleTreeRandom(tr, perm[1], perm[2], perm[3]);
  
  while (tr->ntips < tr->mxtips) 
    {	       
      tr->bestParsimony = INT_MAX;
      nextsp = ++(tr->ntips);             
      p = tr->nodep[perm[nextsp]];
      
      /*printf("ADDING SPECIES %d\n", nextsp);*/
      
      buildNewTip(tr, p);  	
      
      f = findAnyTip(tr->start);
      f = f->back;
      
      branchCounter = 1;
      branches[0] = f;
      markBranches(branches, f, &branchCounter);
      if(branchCounter != ((2 * (tr->ntips - 1)) - 3))
	{
	  printf("Error Random Tree\n");
	  exit(-1);
	}
      
      randomBranch = branches[randomInt(branchCounter)];
      
      insertRandom(p->back, randomBranch);
      
    }
  free(perm);            
  free(branches);
}


void reorderNodes(tree *tr, nodeptr *np, nodeptr p, int *count)
{
  int i, found = 0;
  char sector;

  if(p->tip)    
    return;
  else
    {           
      nodeptr q;

      for(i = tr->mxtips + 1; (i <= (tr->mxtips + tr->mxtips - 1)) && (found == 0); i++)
	{
	  if (p == np[i] || p == np[i]->next || p == np[i]->next->next)
	    {
	      if(p == np[i])			       
		q = tr->nodep[*count + tr->mxtips + 1] = np[i];		 		
	      else
		{
		  if(p == np[i]->next)		  
		    q = tr->nodep[*count + tr->mxtips + 1] = np[i]->next;		     	   
		  else		   
		    q = tr->nodep[*count + tr->mxtips + 1] = np[i]->next->next;		    		    
		}

	      found = 1;	      	     
	      *count = *count + 1;
	    }
	} 
      
      if(found == 0)
	{
	  printf("NOT FOUND \n");
	  exit(-1);
	}
     
      reorderNodes(tr, np, p->next->back, count);     
      reorderNodes(tr, np, p->next->next->back, count);                
    }
}

void nodeRectifier(tree *tr)
{
  nodeptr *np = (nodeptr *)malloc(2 * tr->mxtips * sizeof(nodeptr));
  int i;
  int count = 0;
  
  tr->start       = tr->nodep[1];
  tr->rooted      = FALSE;
  
  for(i = tr->mxtips + 1; i <= (tr->mxtips + tr->mxtips - 1); i++)
    np[i] = tr->nodep[i];           
  
  reorderNodes(tr, np, tr->start->back, &count);  

  free(np);
}


void makeParsimonyTree(tree *tr, analdef *adef)
  { 
    char   filename[128];    
    nodeptr  p, f;    
    int  i, j, k, nextsp, newsp, mintrav, maxtrav, randomMP, startMP;
    int *perm, *informative, *alias;

                            
    /*informative = (int *)malloc(sizeof(int) * tr->cdta->endsite);
      alias = (int *)malloc(sizeof(int) * tr->cdta->endsite);*/
    /*determineUninformativeSites(tr, informative, alias);*/

    perm = (int *)malloc((tr->mxtips + 1) * sizeof(int)); 
    makePermutation(perm, tr->mxtips, adef);
    tr->parsimonyLength = tr->cdta->endsite;        
       
    allocNodexParsimony (tr, adef);
  
    tr->ntips = 0;    
   	       
    tr->nextnode = tr->mxtips + 1;      

    buildSimpleTree(tr, perm[1], perm[2], perm[3]);  
   
    while (tr->ntips < tr->mxtips) 
      {	
	tr->bestParsimony = INT_MAX;
	nextsp = ++(tr->ntips);             
	p = tr->nodep[perm[nextsp]];

	/*	printf("ADDING SPECIES %d\n", nextsp);*/

	buildNewTip(tr, p);

	f = findAnyTip(tr->start);
	f = f->back;
	addTraverseParsimony(tr, p->back, f, 1, tr->ntips - 2);               	 	
	restoreTreeParsimony(tr, p->back, tr->insertNode);		      

	/*printf("MP %d\n", tr->bestParsimony);*/

	if(INT_MAX - tr->bestParsimony < 1000)
	  {
	    printf("Numerical problem with maximum parsimony score, exiting .... \n");
	    exit(-1);
	  }
      }    
           
    free(perm);    
  
    nodeRectifier(tr);   
    initravParsimonyNormal(tr, tr->start);
    initravParsimonyNormal(tr, tr->start->back);            

    if(!adef->rapidParsimony)
      {
	mintrav = 1;
	maxtrav = 20;
	randomMP = tr->bestParsimony;        
	
	do
	  {
	    startMP = randomMP;
	    
	    for(i = 1; i <= tr->mxtips + tr->mxtips - 2; i++)
	      {
		rearrangeParsimony(tr, tr->nodep[i], mintrav, maxtrav);
		if(tr->bestParsimony < randomMP)
		  {		
		    restoreTreeRearrangeParsimony(tr);
		    randomMP = tr->bestParsimony;
		  }
	      }      		     
	  }
	while(randomMP < startMP);
   
	/*printf("REARRANGEMENT MP Score %d Time %f\n", tr->bestParsimony, gettime() - masterTime);*/
      }  
 
    nodeRectifier(tr);       

    /*
      restore(tr, alias);
      free(alias);
      free(informative);
    */

    freeNodexParsimony(tr);              
  } 



void makeParsimonyTreeIncomplete(tree *tr, analdef *adef)
  { 
    char   filename[128];    
    nodeptr  p, f;    
    int  i, j, k, nextsp, newsp, mintrav, maxtrav, randomMP, startMP;
    int *perm, *informative, *alias;
    int co = 0;

    perm = (int *)malloc((tr->mxtips + 1) * sizeof(int));                         
    /*informative = (int *)malloc(sizeof(int) * tr->cdta->endsite);
      alias = (int *)malloc(sizeof(int) * tr->cdta->endsite);*/      
    if(!tr->grouped)
      {
	tr->constraintVector = (int *)malloc((tr->mxtips + 1) * sizeof(int));
	for(i = 1; i <= tr->mxtips; i++)
	  tr->constraintVector[i] = 0;
      }

    tr->parsimonyLength = tr->cdta->endsite;        
  
    /*determineUninformativeSites(tr, informative, alias);*/

    allocNodexParsimony (tr, adef);
     	 
    if(!tr->grouped)
      {
	initravParsimony(tr, tr->start,       tr->constraintVector);
	initravParsimony(tr, tr->start->back, tr->constraintVector);
      }
    else
      {
	initravParsimonyNormal(tr, tr->start);
	initravParsimonyNormal(tr, tr->start->back);
	
      }
    
    /*printf("Incomplete Parsimony score %d\n", evaluateParsimony(tr, tr->start));*/
            
    j = tr->ntips + 1;
    if(!tr->grouped)
      {
	for(i = 1; i <= tr->mxtips; i++)      
	  if(tr->constraintVector[i] == 0) perm[j++] = i;	    	  
      }
    else
      {
	for(i = 1; i <= tr->mxtips; i++)      
	  {
	    if(tr->constraintVector[i] == -1) 
	      {
		perm[j++] = i;		
		tr->constraintVector[i] = -9;
	      }
	  }
      }
     
#ifdef PARALLEL
   srand((unsigned int) gettimeSrand()); 
#endif

#ifndef PARALLEL  
  if(adef->parsimonySeed == 0)   
    srand((unsigned int) gettimeSrand());          
  else
    srand(adef->parsimonySeed);
#endif

    for (i = tr->ntips + 1; i <= tr->mxtips; i++) 
    {
      k        = randomInt(tr->mxtips + 1 - i);
      j        = perm[i];
      perm[i]     = perm[i + k];
      perm[i + k] = j; 
    }             
 
#ifdef DEBUG_CONSTRAINTS        
     for(i = 1; i <= tr->mxtips; i++)     
       printf("TIP %s %d\n", tr->nameList[i], tr->constraintVector[i]);              
#endif

    while (tr->ntips < tr->mxtips) 
      {	
	tr->bestParsimony = INT_MAX;
	nextsp = ++(tr->ntips);             
	p = tr->nodep[perm[nextsp]];

	/*printf("ADDING SPECIES %d %s\n", perm[nextsp], tr->nameList[perm[nextsp]]);*/
	
	buildNewTip(tr, p);      
	
	if(tr->grouped)
	  {
	    int number = p->back->number;
	    tr->constraintVector[number] = -9;
	  }
	
	f = findAnyTip(tr->start);
	f = f->back;      

	if(tr->grouped)
	  {
	    tr->grouped = FALSE;
	    addTraverseParsimony(tr, p->back, f, 1, tr->ntips - 2);  
	    tr->grouped = TRUE;
	  }
	else
	  addTraverseParsimony(tr, p->back, f, 1, tr->ntips - 2);

	restoreTreeParsimony(tr, p->back, tr->insertNode);		      

	/*printf("MP %d\n", tr->bestParsimony);*/

	if(INT_MAX - tr->bestParsimony < 1000)
	  {
	    printf("Numerical problem with maximum parsimony score, exiting .... \n");
	    exit(-1);
	  }

      }               

    free(perm);

    /*printf("RANDOM ADDITION MP Score %d Time %f\n", tr->bestParsimony, gettime() - masterTime);*/
  
    nodeRectifier(tr);   
    initravParsimonyNormal(tr, tr->start);
    initravParsimonyNormal(tr, tr->start->back); 
    
    if(!adef->rapidParsimony)
      {
	mintrav = 1;
	maxtrav = 20;
	randomMP = tr->bestParsimony;        
	
	do
	  {
	    startMP = randomMP;
	    
	    for(i = 1; i <= tr->mxtips + tr->mxtips - 2; i++)
	      {		
		if(rearrangeParsimony(tr, tr->nodep[i], mintrav, maxtrav))
		  {
		    if(tr->bestParsimony < randomMP)
		      {		
			restoreTreeRearrangeParsimony(tr);
			randomMP = tr->bestParsimony;
		      }				
		  }		    		     		 
	      }   
	  }
	while(randomMP < startMP);
	
	/*printf("REARRANGEMENT MP Score %d Time %f\n", tr->bestParsimony, gettime() - masterTime);*/
      }

    nodeRectifier(tr);  

    /*restore(tr, alias);                
      free(alias);
      free(informative);*/

    if(!tr->constrained && !tr->grouped)
      free(tr->constraintVector);
    freeNodexParsimony(tr);              
  } 

/*************************** PARSIMONY END **********************************/

