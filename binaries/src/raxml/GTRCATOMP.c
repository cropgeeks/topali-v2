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
#include <sys/mman.h>
#include <unistd.h>
#include <time.h> 
#include <stdlib.h>
#include <stdio.h>
#include <ctype.h>
#include <string.h>
#include "axml.h"

#define LH_SIZE sizeof(likelivector)

static void newviewPartialGTRCAT(tree *tr, nodeptr p, int i, double ki, likelivector *x3) 
{     
  double  d1[3], d2[3],  ump_x1_1, ump_x1_2, ump_x1_3, ump_x1_0, 
    ump_x2_0, ump_x2_1, ump_x2_2, ump_x2_3, x1px2, z1, lz1, z2, lz2;
  double *EIGN    = tr->EIGN, *eptr = tr->EI, *eptr2 = tr->EV;

  nodeptr  q, r;   
  likelivector   x1, x2;

  q = p->next->back;
  r = p->next->next->back;   

  if(q->tip && r->tip)
    {
      memcpy(&x1, &tr->gtrTip[q->tip[i]], LH_SIZE);
      memcpy(&x2, &tr->gtrTip[r->tip[i]], LH_SIZE);
      x3->exp = 0;
    }

  if(q->tip && !r->tip)
    {			
      memcpy(&x1, &tr->gtrTip[q->tip[i]], LH_SIZE);
      newviewPartialGTRCAT(tr, r, i, ki, &x2); 
      x3->exp = x2.exp;
    }

  if(!q->tip && r->tip)
    {	
      newviewPartialGTRCAT(tr, q, i, ki, &x1); 
      memcpy(&x2, &tr->gtrTip[r->tip[i]], LH_SIZE);
      x3->exp = x1.exp;
    }
    
  if(!q->tip && !r->tip)
    {	
      newviewPartialGTRCAT(tr, q, i, ki, &x1);
      newviewPartialGTRCAT(tr, r, i, ki, &x2); 
      x3->exp = x1.exp + x2.exp;
    }
   
  z1  = q->z;
  lz1 = (z1 > zmin) ? log(z1) : log(zmin);  
  lz1 *= ki;

  z2  = r->z;
  lz2 = (z2 > zmin) ? log(z2) : log(zmin);
  lz2 *= ki;
  
  d1[0] = x1.c * exp(EIGN[0] * lz1);
  d2[0] = x2.c * exp(EIGN[0] * lz2);	    
  d1[1] = x1.g * exp(EIGN[1] * lz1);
  d2[1] = x2.g * exp(EIGN[1] * lz2);
  d1[2] = x1.t * exp(EIGN[2] * lz1);
  d2[2] = x2.t * exp(EIGN[2] * lz2); 
  
  ump_x1_0  = d1[0] * eptr[0];
  ump_x1_0 += d1[1] * eptr[1];
  ump_x1_0 += d1[2] * eptr[2];	      	
  ump_x1_0 += x1.a;
  
  ump_x1_1  = d1[0] * eptr[3];
  ump_x1_1 += d1[1] * eptr[4];
  ump_x1_1 += d1[2] * eptr[5];	      	
  ump_x1_1 += x1.a;
  
  ump_x1_2  = d1[0] * eptr[6];
  ump_x1_2 += d1[1] * eptr[7];
  ump_x1_2 += d1[2] * eptr[8];	      	
  ump_x1_2 += x1.a;
  
  ump_x1_3  = d1[0] * eptr[9];
  ump_x1_3 += d1[1] * eptr[10];
  ump_x1_3 += d1[2] * eptr[11];	      	
  ump_x1_3 += x1.a; 
  	 	    	    	     	     
  ump_x2_0  = d2[0] * eptr[0];
  ump_x2_0 += d2[1] * eptr[1];
  ump_x2_0 += d2[2] * eptr[2];		     
  ump_x2_0 += x2.a;
  
  ump_x2_1  = d2[0] * eptr[3];
  ump_x2_1 += d2[1] * eptr[4];
  ump_x2_1 += d2[2] * eptr[5];		     
  ump_x2_1 += x2.a;	 
  
  ump_x2_2  = d2[0] * eptr[6];
  ump_x2_2 += d2[1] * eptr[7];
  ump_x2_2 += d2[2] * eptr[8];		     
  ump_x2_2 += x2.a;	  
		   
  ump_x2_3  = d2[0] * eptr[9];
  ump_x2_3 += d2[1] * eptr[10];
  ump_x2_3 += d2[2] * eptr[11];		     
  ump_x2_3 += x2.a;	    	  		   	  
    
  x1px2 = ump_x1_0 * ump_x2_0;
  x3->a = x1px2 *  *eptr2++;
  x3->c = x1px2 *  *eptr2++;
  x3->g = x1px2 *  *eptr2++;
  x3->t = x1px2 *  *eptr2++;
  
  x1px2 = ump_x1_1 * ump_x2_1;
  x3->a += x1px2  *  *eptr2++;
  x3->c += x1px2 *   *eptr2++;
  x3->g += x1px2 *   *eptr2++;
  x3->t += x1px2 *   *eptr2++;
  
  x1px2 = ump_x1_2 * ump_x2_2;
  x3->a += x1px2 *  *eptr2++;
  x3->c += x1px2*   *eptr2++;
  x3->g += x1px2 *  *eptr2++;
  x3->t += x1px2 *  *eptr2++;
  
  x1px2 = ump_x1_3 * ump_x2_3;
  x3->a += x1px2 *   *eptr2++;
  x3->c += x1px2 *   *eptr2++;
  x3->g += x1px2 *   *eptr2++;
  x3->t += x1px2 *   *eptr2++;
  
     if (x3->a < minlikelihood && x3->a > minusminlikelihood &&
      x3->c < minlikelihood && x3->c > minusminlikelihood &&
      x3->g < minlikelihood && x3->g > minusminlikelihood &&
      x3->t < minlikelihood && x3->t > minusminlikelihood) 
    {	     
      x3->a   *= twotothe256;
      x3->c   *= twotothe256;
      x3->g   *= twotothe256;     
      x3->t   *= twotothe256;
      x3->exp += 1;
    }	              

  return;
}




double evaluatePartialGTRCAT (tree *tr, nodeptr p, int i, double ki)
{
  double z, lz, term;    
  nodeptr  q;
  int     w = tr->cdta->aliaswgt[i];    
  double  d[3], *e =  tr->EIGN;
  likelivector   x1, x2; 
  int scale;
 
  q = p->back; 

  if(!p->tip && q->tip)
    {
      newviewPartialGTRCAT(tr, p, i, ki, &x1);
      memcpy(&x2, &tr->gtrTip[q->tip[i]], sizeof(likelivector));      
      scale = x1.exp;
    }
    
  if(p->tip && !q->tip)
    {     
      memcpy(&x1, &tr->gtrTip[p->tip[i]], sizeof(likelivector));
      newviewPartialGTRCAT(tr, q, i, ki, &x2);     
      scale = x2.exp;
    }

  if(!p->tip && !q->tip)
    {      
      newviewPartialGTRCAT(tr, p, i, ki, &x1);
      newviewPartialGTRCAT(tr, q, i, ki, &x2);     
      scale = x1.exp + x2.exp;
    }
     
  z = p->z;
  
  if (z < zmin) 
    z = zmin;
  
  lz = log(z) * ki;
  
  d[0] = exp (e[0] * lz);
  d[1] = exp (e[1] * lz);
  d[2] = exp (e[2] * lz);       	   
  
  term =  x1.a * x2.a;
  term += x1.c * x2.c * d[0];
  term += x1.g * x2.g * d[1];
  term += x1.t * x2.t * d[2];     

  term = log(term) + scale * log(minlikelihood);   

  return  (w * term);
}






boolean newviewGTRCAT(tree *tr, nodeptr  p)
{  
  if(p->tip) return TRUE;

  { 	
    double  *left, *left_start, d1c, d1g, d1t, d2c, d2g, d2t;
    double *EIGN = tr->EIGN, *EV;
    double      
      ump_x1_1, ump_x1_2, ump_x1_3, ump_x1_0, 
      ump_x2_0, ump_x2_1, ump_x2_2, ump_x2_3, x1px2, z1, z2, ki;
    int i, cat, *cptr, N;	       
    nodeptr  q, r;   
    double  *rptr, lz10, lz11, lz12, lz20, lz21, lz22;
   
    rptr   = &(tr->cdta->patrat[0]); 
    cptr  = &(tr->cdta->rateCategory[0]);

    N = tr->cdta->endsite;

    q = p->next->back;
    r = p->next->next->back;         

    z1  = q->z;
    z1 = (z1 > zmin) ? log(z1) : log(zmin);

    z2  = r->z;
    z2 = (z2 > zmin) ? log(z2) : log(zmin);  
      
    left_start = left = (double *)malloc(24 * tr->NumberOfCategories * sizeof(double));              
         
    lz10 = EIGN[0] * z1;
    lz11 = EIGN[1] * z1;
    lz12 = EIGN[2] * z1;

    lz20 = EIGN[0] * z2;
    lz21 = EIGN[1] * z2;
    lz22 = EIGN[2] * z2;
        
    for(i = 0; i < tr->NumberOfCategories; i++)
      {	
	ki = *rptr++;

	EV = tr->EI;
	d1c = exp (ki * lz10);
	d1g = exp (ki * lz11);
	d1t = exp (ki * lz12);	

	*left++ = d1c * *EV++;
	*left++ = d1g * *EV++;
	*left++ = d1t * *EV++;

	*left++ = d1c * *EV++;
	*left++ = d1g * *EV++;
	*left++ = d1t * *EV++;

	*left++ = d1c * *EV++;
	*left++ = d1g * *EV++;
	*left++ = d1t * *EV++;

	*left++ = d1c * *EV++;
	*left++ = d1g * *EV++;
	*left++ = d1t * *EV++;
	
	EV = tr->EI;
	d2c = exp (ki * lz20);
	d2g = exp (ki * lz21);
        d2t = exp (ki * lz22);	

	*left++ = d2c * *EV++;
	*left++ = d2g * *EV++;
	*left++ = d2t * *EV++;

	*left++ = d2c * *EV++;
	*left++ = d2g * *EV++;
	*left++ = d2t * *EV++;

	*left++ = d2c * *EV++;
	*left++ = d2g * *EV++;
	*left++ = d2t * *EV++;

	*left++ = d2c * *EV++;
	*left++ = d2g * *EV++;
	*left++ = d2t * *EV++;
      }
  
      
    if(r->tip && q->tip)
      {	  	 	 
	char *tipX1 = q->tip;
	char *tipX2 = r->tip;
	likelivector *x1, *x2, *x3, *x3_start;

	while ((! p->x)) 
	  {     
	    if (! p->x) if (! getxnode(p)) return FALSE;	
	  }
   
	x3  = x3_start = (likelivector*)p->x; 	


#pragma omp parallel for private(x1, x2, x3, EV, cat, left, ump_x1_0,  ump_x1_1, ump_x1_2,  ump_x1_3,  ump_x2_0,  ump_x2_1,  ump_x2_2,  ump_x2_3, x1px2)
	for (i = 0; i < N; i++) 
	  {		   	    	   	   
	    cat = cptr[i];
	    EV = tr->EV;
	    x1 = &(tr->gtrTip[tipX1[i]]);
	    x2 = &(tr->gtrTip[tipX2[i]]);
	    x3 = &x3_start[i];

	    left = &left_start[cat * 24];
	    	   
	    ump_x1_0 = x1->a;
	    ump_x1_0 += x1->c * *left++;
	    ump_x1_0 += x1->g * *left++;
	    ump_x1_0 += x1->t * *left++;	      		   	    
	
	    ump_x1_1 =  x1->a;
	    ump_x1_1 += x1->c * *left++;
	    ump_x1_1 += x1->g * *left++;
	    ump_x1_1 += x1->t * *left++;	      		  	 
	    
	    ump_x1_2 =  x1->a;
	    ump_x1_2 += x1->c * *left++;
	    ump_x1_2 += x1->g * *left++;
	    ump_x1_2 += x1->t * *left++;	      		   	 
	    
	    ump_x1_3 =  x1->a;
	    ump_x1_3 += x1->c * *left++;
	    ump_x1_3 += x1->g * *left++;
	    ump_x1_3 += x1->t * *left++;	      		   	  
			 	 	    	    	     	     
	    ump_x2_0 =  x2->a;
	    ump_x2_0 += x2->c * *left++;
	    ump_x2_0 += x2->g * *left++;
	    ump_x2_0 += x2->t * *left++;		     	  	
	  
	    ump_x2_1 =  x2->a;
	    ump_x2_1 += x2->c * *left++;
	    ump_x2_1 += x2->g * *left++;
	    ump_x2_1 += x2->t * *left++;		     	 	     
	    
	    ump_x2_2 =  x2->a;
	    ump_x2_2 += x2->c * *left++;
	    ump_x2_2 += x2->g * *left++;
	    ump_x2_2 += x2->t * *left++;		     	   	      
		   
	    ump_x2_3 =  x2->a;
	    ump_x2_3 += x2->c * *left++;
	    ump_x2_3 += x2->g * *left++;
	    ump_x2_3 += x2->t * *left++;		     	  	       	   
	   	   	  	    	    
	    x1px2 = ump_x1_0 * ump_x2_0;
	    x3->a = x1px2 *  *EV++;
	    x3->c = x1px2 *  *EV++;
	    x3->g = x1px2 *  *EV++;
	    x3->t = x1px2 *  *EV++;
	    
	    x1px2 = ump_x1_1 * ump_x2_1;
	    x3->a += x1px2 *  *EV++;
	    x3->c += x1px2 *  *EV++;
	    x3->g += x1px2 *  *EV++;
	    x3->t += x1px2 *  *EV++;
	    
	    x1px2 = ump_x1_2 * ump_x2_2;
	    x3->a += x1px2 *  *EV++;
	    x3->c += x1px2*   *EV++;
	    x3->g += x1px2 *  *EV++;
	    x3->t += x1px2 *  *EV++;
	    
	    x1px2 = ump_x1_3 * ump_x2_3;
	    x3->a += x1px2 *   *EV++;
	    x3->c += x1px2 *   *EV++;
	    x3->g += x1px2 *   *EV++;
	    x3->t += x1px2 *   *EV++;
	    
	    x3->exp = 0;		  	    	   	    	   	    
	  }

	 free(left_start); 	
	 return TRUE;
      }
    
    if(r->tip && !q->tip)
      {	
	char *tipX2 = r->tip;     
	likelivector   *x1, *x2, *x3, *x1_start, *x3_start;	      

	while ((! p->x) || (! q->x)) 
	  {	 
	    if (! q->x) if (! newviewGTRCAT(tr, q)) return FALSE;
	    if (! p->x) if (! getxnode(p)) return FALSE;	
	  }
		  
	   
	    x1  = x1_start = (likelivector*)q->x;
	    x3  = x3_start = (likelivector*)p->x;
	  
  	 
#pragma omp parallel for private(x1, x2, x3, EV, cat, left, ump_x1_0,  ump_x1_1, ump_x1_2,  ump_x1_3,  ump_x2_0,  ump_x2_1,  ump_x2_2,  ump_x2_3, x1px2)
	for (i = 0; i < N; i++) 
	   {		   
	     cat = cptr[i];
	     EV = tr->EV;
	     x1 = &x1_start[i];
	     x2 = &(tr->gtrTip[tipX2[i]]);   
	     x3 = &x3_start[i];
	     left = &left_start[cat * 24];	     		    

	     ump_x1_0 = x1->a;
	     ump_x1_0 += x1->c * *left++;
	     ump_x1_0 += x1->g * *left++;
	     ump_x1_0 += x1->t * *left++;	      		   	    
	     
	     ump_x1_1 =  x1->a;
	     ump_x1_1 += x1->c * *left++;
	     ump_x1_1 += x1->g * *left++;
	     ump_x1_1 += x1->t * *left++;	      		  	 
	     
	     ump_x1_2 =  x1->a;
	     ump_x1_2 += x1->c * *left++;
	     ump_x1_2 += x1->g * *left++;
	     ump_x1_2 += x1->t * *left++;	      		   	 
	     
	     ump_x1_3 =  x1->a;
	     ump_x1_3 += x1->c * *left++;
	     ump_x1_3 += x1->g * *left++;
	     ump_x1_3 += x1->t * *left++;	      		   	  
	     
	     ump_x2_0 =  x2->a;
	     ump_x2_0 += x2->c * *left++;
	     ump_x2_0 += x2->g * *left++;
	     ump_x2_0 += x2->t * *left++;		     	  	
	     
	     ump_x2_1 =  x2->a;
	     ump_x2_1 += x2->c * *left++;
	     ump_x2_1 += x2->g * *left++;
	     ump_x2_1 += x2->t * *left++;		     	 	     
	     
	     ump_x2_2 =  x2->a;
	     ump_x2_2 += x2->c * *left++;
	     ump_x2_2 += x2->g * *left++;
	     ump_x2_2 += x2->t * *left++;		     	   	      
	     
	     ump_x2_3 =  x2->a;
	     ump_x2_3 += x2->c * *left++;
	     ump_x2_3 += x2->g * *left++;
	     ump_x2_3 += x2->t * *left++;
	     
	     x1px2 = ump_x1_0 * ump_x2_0;
	     x3->a = x1px2 *  *EV++;
	     x3->c = x1px2 *  *EV++;
	     x3->g = x1px2 *  *EV++;
	     x3->t = x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_1 * ump_x2_1;
	     x3->a += x1px2 *  *EV++;
	     x3->c += x1px2 *  *EV++;
	     x3->g += x1px2 *  *EV++;
	     x3->t += x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_2 * ump_x2_2;
	     x3->a += x1px2 *  *EV++;
	     x3->c += x1px2 *  *EV++;
	     x3->g += x1px2 *  *EV++;
	     x3->t += x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_3 * ump_x2_3;
	     x3->a += x1px2 *   *EV++;
	     x3->c += x1px2 *   *EV++;
	     x3->g += x1px2 *   *EV++;
	     x3->t += x1px2 *   *EV++;
	     
	     x3->exp = x1->exp;		  
	     if (x3->a < minlikelihood && x3->a > minusminlikelihood &&
		 x3->c < minlikelihood && x3->c > minusminlikelihood &&
		 x3->g < minlikelihood && x3->g > minusminlikelihood &&
		 x3->t < minlikelihood && x3->t > minusminlikelihood)	       
	       {	     	     
		 x3->a   *= twotothe256;
		 x3->c   *= twotothe256;
		 x3->g   *= twotothe256;		
		 x3->t   *= twotothe256;
		 x3->exp += 1;
	       }	     	    
	  
	   }     
         
	 free(left_start); 	 
	 return TRUE;
      }
       


     if(!r->tip && q->tip)
      {	
	char *tipX1;
	likelivector *x1, *x2, *x2_start, *x3, *x3_start;      

	tipX1 = q->tip;       

	while ((! p->x) || (! r->x)) 
	  {	 
	    if (! r->x) if (! newviewGTRCAT(tr, r)) return FALSE;
	    if (! p->x) if (! getxnode(p)) return FALSE;	
	  }
		  
	x2  = x2_start = (likelivector*)r->x;
	x3  = x3_start = (likelivector*)p->x;
      

#pragma omp parallel for private(x1, x2, x3, EV, cat, left, ump_x1_0,  ump_x1_1, ump_x1_2,  ump_x1_3,  ump_x2_0,  ump_x2_1,  ump_x2_2,  ump_x2_3, x1px2)
	for (i = 0; i < N; i++) 
	   {		     	
	     cat = cptr[i];
	     EV = tr->EV;
	     x1 = &(tr->gtrTip[tipX1[i]]);     
	     x2 = &x2_start[i];
	     x3 = &x3_start[i];
	     left = &left_start[cat * 24];	    
  	    	     
	     ump_x1_0 = x1->a;
	     ump_x1_0 += x1->c * *left++;
	     ump_x1_0 += x1->g * *left++;
	     ump_x1_0 += x1->t * *left++;	      		   	    
	     
	     ump_x1_1 =  x1->a;
	     ump_x1_1 += x1->c * *left++;
	     ump_x1_1 += x1->g * *left++;
	     ump_x1_1 += x1->t * *left++;	      		  	 
	     
	     ump_x1_2 =  x1->a;
	     ump_x1_2 += x1->c * *left++;
	     ump_x1_2 += x1->g * *left++;
	     ump_x1_2 += x1->t * *left++;	      		   	 
	     
	     ump_x1_3 =  x1->a;
	     ump_x1_3 += x1->c * *left++;
	     ump_x1_3 += x1->g * *left++;
	     ump_x1_3 += x1->t * *left++;	      		   	  
	     
	     ump_x2_0 =  x2->a;
	     ump_x2_0 += x2->c * *left++;
	     ump_x2_0 += x2->g * *left++;
	     ump_x2_0 += x2->t * *left++;		     	  	
	     
	     ump_x2_1 =  x2->a;
	     ump_x2_1 += x2->c * *left++;
	     ump_x2_1 += x2->g * *left++;
	     ump_x2_1 += x2->t * *left++;		     	 	     
	     
	     ump_x2_2 =  x2->a;
	     ump_x2_2 += x2->c * *left++;
	     ump_x2_2 += x2->g * *left++;
	     ump_x2_2 += x2->t * *left++;		     	   	      
	     
	     ump_x2_3 =  x2->a;
	     ump_x2_3 += x2->c * *left++;
	     ump_x2_3 += x2->g * *left++;
	     ump_x2_3 += x2->t * *left++;
	     
	     
	     x1px2 = ump_x1_0 * ump_x2_0;
	     x3->a = x1px2 *  *EV++;
	     x3->c = x1px2 *  *EV++;
	     x3->g = x1px2 *  *EV++;
	     x3->t = x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_1 * ump_x2_1;
	     x3->a += x1px2 *  *EV++;
	     x3->c += x1px2 *  *EV++;
	     x3->g += x1px2 *  *EV++;
	     x3->t += x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_2 * ump_x2_2;
	     x3->a += x1px2 *  *EV++;
	     x3->c += x1px2 *  *EV++;
	     x3->g += x1px2 *  *EV++;
	     x3->t += x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_3 * ump_x2_3;
	     x3->a += x1px2 *   *EV++;
	     x3->c += x1px2 *   *EV++;
	     x3->g += x1px2 *   *EV++;
	     x3->t += x1px2 *   *EV++;
	     
	     x3->exp = x2->exp;		  
	     if (x3->a < minlikelihood && x3->a > minusminlikelihood &&
		 x3->c < minlikelihood && x3->c > minusminlikelihood &&
		 x3->g < minlikelihood && x3->g > minusminlikelihood &&
		 x3->t < minlikelihood && x3->t > minusminlikelihood)	     
	       {	     	
		 x3->a   *= twotothe256;
		 x3->c   *= twotothe256;
		 x3->g   *= twotothe256;		
		 x3->t   *= twotothe256;
		 x3->exp += 1;
	       }	     	     	    	     	    

	   }     
         
	 free(left_start); 

	 return TRUE;
      }
       

      {
	likelivector   *x1, *x1_start, *x2, *x2_start, *x3, *x3_start;

	  while ((! p->x) || (! q->x) || (! r->x)) {
	    if (! q->x) if (! newviewGTRCAT(tr, q)) return FALSE;
	    if (! r->x) if (! newviewGTRCAT(tr, r)) return FALSE;
	    if (! p->x) if (! getxnode(p)) return FALSE;	
	  }
	
	  x1  = x1_start = (likelivector*)q->x;
	  x2  = x2_start = (likelivector*)r->x;
	  x3  = x3_start = (likelivector*)p->x;
	 

#pragma omp parallel for private(x1, x2, x3, EV, cat, left, ump_x1_0,  ump_x1_1, ump_x1_2,  ump_x1_3,  ump_x2_0,  ump_x2_1,  ump_x2_2,  ump_x2_3, x1px2)	  
	  for (i = 0; i < N; i++) 
	    {		     
	      cat = cptr[i];
	      EV = tr->EV;
	      left = &left_start[cat * 24];
	      x1 = &x1_start[i];
	      x2 = &x2_start[i];
	      x3 = &x3_start[i];
	     		    
	      ump_x1_0 = x1->a;
	      ump_x1_0 += x1->c * *left++;
	      ump_x1_0 += x1->g * *left++;
	      ump_x1_0 += x1->t * *left++;	      		   	    
	      
	      ump_x1_1 =  x1->a;
	      ump_x1_1 += x1->c * *left++;
	      ump_x1_1 += x1->g * *left++;
	      ump_x1_1 += x1->t * *left++;	      		  	 
	      
	      ump_x1_2 =  x1->a;
	      ump_x1_2 += x1->c * *left++;
	      ump_x1_2 += x1->g * *left++;
	      ump_x1_2 += x1->t * *left++;	      		   	 
	      
	      ump_x1_3 =  x1->a;
	      ump_x1_3 += x1->c * *left++;
	      ump_x1_3 += x1->g * *left++;
	      ump_x1_3 += x1->t * *left++;	      		   	  
	      
	      ump_x2_0 =  x2->a;
	      ump_x2_0 += x2->c * *left++;
	      ump_x2_0 += x2->g * *left++;
	      ump_x2_0 += x2->t * *left++;		     	  	
	      
	      ump_x2_1 =  x2->a;
	      ump_x2_1 += x2->c * *left++;
	      ump_x2_1 += x2->g * *left++;
	      ump_x2_1 += x2->t * *left++;		     	 	     
	      
	      ump_x2_2 =  x2->a;
	      ump_x2_2 += x2->c * *left++;
	      ump_x2_2 += x2->g * *left++;
	      ump_x2_2 += x2->t * *left++;		     	   	      
	      
	      ump_x2_3 =  x2->a;
	      ump_x2_3 += x2->c * *left++;
	      ump_x2_3 += x2->g * *left++;
	      ump_x2_3 += x2->t * *left++;
	      
	     	     	     
	      x1px2 = ump_x1_0 * ump_x2_0;
	      x3->a = x1px2 *  *EV++;
	      x3->c = x1px2 *  *EV++;
	      x3->g = x1px2 *  *EV++;
	      x3->t = x1px2 *  *EV++;
	      
	      x1px2 = ump_x1_1 * ump_x2_1;
	      x3->a += x1px2 *  *EV++;
	      x3->c += x1px2 *  *EV++;
	      x3->g += x1px2 *  *EV++;
	      x3->t += x1px2 *  *EV++;
	      
	      x1px2 = ump_x1_2 * ump_x2_2;
	      x3->a += x1px2 *  *EV++;
	      x3->c += x1px2 *  *EV++;
	      x3->g += x1px2 *  *EV++;
	      x3->t += x1px2 *  *EV++;
	      
	      x1px2 = ump_x1_3 * ump_x2_3;
	      x3->a += x1px2 *   *EV++;
	      x3->c += x1px2 *   *EV++;
	      x3->g += x1px2 *   *EV++;
	      x3->t += x1px2 *   *EV++;
	     
	      x3->exp = x1->exp + x2->exp;		  
	      if (x3->a < minlikelihood && x3->a > minusminlikelihood &&
		  x3->c < minlikelihood && x3->c > minusminlikelihood &&
		  x3->g < minlikelihood && x3->g > minusminlikelihood &&
		  x3->t < minlikelihood && x3->t > minusminlikelihood)	     
		{	     
		  x3->a   *= twotothe256;
		  x3->c   *= twotothe256;
		  x3->g   *= twotothe256;		
		  x3->t   *= twotothe256;
		  x3->exp += 1;
		}	
	  
      	     	     
	    }     	  
	  free(left_start); 	
	  return TRUE;
      }
  }
}





/*********************************************************************************************/






double evaluateGTRCAT (tree *tr, nodeptr p)
  {
    double   sum, z, lz, ki, lz1, lz2, lz3, term;    
    nodeptr  q;
    int     i, cat;
    int     *wptr = tr->cdta->aliaswgt, *cptr;    
    double  *diagptable, *rptr, *diagptable_start;
    double *EIGN = tr->EIGN;
    likelivector   *x1, *x1_start, *x2, *x2_start;
       
    q = p->back;
    
    rptr   = &(tr->cdta->patrat[0]); 
    cptr  = &(tr->cdta->rateCategory[0]);
   
    z = p->z;
   
    if (z < zmin) z = zmin;
    lz = log(z);

    lz1 = EIGN[0] * lz;
    lz2 = EIGN[1] * lz;
    lz3 = EIGN[2] * lz;

    diagptable = diagptable_start = (double *)malloc(sizeof(double) * tr->NumberOfCategories * 3);


 
    for(i = 0; i <  tr->NumberOfCategories; i++)
      {	
	ki = *rptr++;	 
	*diagptable++ = exp (ki * lz1);
	*diagptable++ = exp (ki * lz2);
	*diagptable++ = exp (ki * lz3);	
      }
	    
    sum = 0.0;   

    if(p->tip && !q->tip)
      {
	char *tipX1 = p->tip;     

	while ((! q->x)) 
	  {	   
	    if (! (q->x)) if (! newviewGTRCAT(tr, q)) return badEval;
	  }

	x2  = x2_start = (likelivector*)q->x;

#pragma omp parallel for private(cat, term, x1, x2, diagptable) reduction(+ : sum)
	for (i = 0; i < tr->cdta->endsite; i++) 
	  {	    	  
	    cat = cptr[i];
	    x1 = &(tr->gtrTip[tipX1[i]]);
	    x2 = &x2_start[i];
	    diagptable = &diagptable_start[3 * cat];
	    
	    term =  x1->a * x2->a;
	    term += x1->c * x2->c * *diagptable++;
	    term += x1->g * x2->g * *diagptable++;
	    term += x1->t * x2->t * *diagptable++;     
	    term = log(term) + (x2->exp)*log(minlikelihood);
	    sum += wptr[i] * term;		  	    	  
	  }
	free(diagptable_start); 
    
	tr->likelihood = sum;
	return  sum;
      }
  
    
    if(!p->tip && q->tip)
      {
	char *tipX2 = q->tip;     

	while ((! p->x)) 
	  {
	    if (! (p->x)) if (! newviewGTRCAT(tr, p)) return badEval;	  
	  }
	
	x1  = x1_start = (likelivector*)p->x;
#pragma omp parallel for private(cat, term, x1, x2, diagptable) reduction(+ : sum)      
	for (i = 0; i < tr->cdta->endsite; i++) 
	  {	  	
	    
	    cat = cptr[i];
	    x1 = &x1_start[i];
	    x2 = &(tr->gtrTip[tipX2[i]]);
	    
	    diagptable = &diagptable_start[3 * cat];
	    
	    term =  x1->a * x2->a;
	    term += x1->c * x2->c * *diagptable++;
	    term += x1->g * x2->g * *diagptable++;
	    term += x1->t * x2->t * *diagptable++;     
	    term = log(term) + (x1->exp)*log(minlikelihood);
	    sum += wptr[i] * term;		  	    
	  }
	
	free(diagptable_start); 
    
	tr->likelihood = sum;
	return  sum;
      }

      
    while ((! p->x) || (! q->x)) 
      {
	if (! (p->x)) if (! newviewGTRCAT(tr, p)) return badEval;
	if (! (q->x)) if (! newviewGTRCAT(tr, q)) return badEval;
      }

    x1  = x1_start = (likelivector*)p->x;
    x2  = x2_start = (likelivector*)q->x;


#pragma omp parallel for private(cat, term, x1, x2, diagptable) reduction(+ : sum)
    for (i = 0; i < tr->cdta->endsite; i++) 
      {		   	
	cat = cptr[i];
	diagptable = &diagptable_start[3 * cat];
	x1 = &x1_start[i];
	x2 = &x2_start[i];

	term =  x1->a * x2->a;
	term += x1->c * x2->c * *diagptable++;
	term += x1->g * x2->g * *diagptable++;
	term += x1->t * x2->t * *diagptable++;
	
	term = log(term) + (x1->exp + x2->exp)*log(minlikelihood);
	sum += wptr[i] * term;		  
      }
    free(diagptable_start); 
    
    tr->likelihood = sum;
    return  sum;         
  } /* evaluate */




double makenewzGTRCAT (tree *tr, nodeptr p, nodeptr q, double z0, int maxiter)
  { 
    double   z, zprev, zstep;    
    likelivector  *x1, *x1_start, *x2, *x2_start;
    double  *sumtable, *sum;
    int     i, *cptr;
    double  dlnLdlz;                 
    double  d2lnLdlz2;                  
    double *d1, *d_start, *rptr;
    double ki;
    double e1, e2, e3, s1, s2, s3;
    double *EIGN = tr->EIGN;

    e1 = EIGN[0] * EIGN[0];
    e2 = EIGN[1] * EIGN[1];
    e3 = EIGN[2] * EIGN[2];
    s1 = EIGN[0];
    s2 = EIGN[1];
    s3 = EIGN[2];
        
    sum = sumtable = (double *)malloc(4 * tr->cdta->endsite * sizeof(double));
    d1 = d_start = (double *)malloc(tr->NumberOfCategories * 3 * sizeof(double));   
   
    if(p->tip && q->tip)
      {
	char *tipX1 = p->tip;      
	char *tipX2 = q->tip;     

#pragma omp parallel for private(x1, x2, sum)
	for (i = 0; i < tr->cdta->endsite; i++) 
	  {    
	    x1 = &(tr->gtrTip[tipX1[i]]);
	    x2 = &(tr->gtrTip[tipX2[i]]);
	    sum = &sumtable[i * 4];

	    *sum++ = x1->a * x2->a;
	    *sum++ = x1->c * x2->c;
	    *sum++ = x1->g * x2->g;
	    *sum++ = x1->t * x2->t;		    	    	  
	  }
      }

    if(p->tip && !q->tip)
      {
	char *tipX1 = p->tip;    

	while ((! q->x)) {	 
	  if (! (q->x)) if (! newviewGTRCAT(tr, q)) return badZ;
	}
	
	x2 = x2_start = (likelivector*)q->x;

#pragma omp parallel for private(x1, x2, sum)
	for (i = 0; i < tr->cdta->endsite; i++) 
	  {    
	    x1 = &(tr->gtrTip[tipX1[i]]);
	    x2 = &x2_start[i];
	    sum = &sumtable[i * 4];

	    *sum++ = x1->a * x2->a;
	    *sum++ = x1->c * x2->c;
	    *sum++ = x1->g * x2->g;
	    *sum++ = x1->t * x2->t;	    	   
	  }

      }

    if(!p->tip && q->tip)
      {
	char *tipX2 = q->tip;     

	 while ((! p->x)) {
	   if (! (p->x)) if (! newviewGTRCAT(tr, p)) return badZ;     
	 }
	 
	 x1 = x1_start = (likelivector*)p->x;

#pragma omp parallel for private(x1, x2, sum)
	for (i = 0; i < tr->cdta->endsite; i++) 
	  {     
	    x1 = &x1_start[i];
	    x2 = &(tr->gtrTip[tipX2[i]]);
	    sum = &sumtable[i * 4];

	    *sum++ = x1->a * x2->a;
	    *sum++ = x1->c * x2->c;
	    *sum++ = x1->g * x2->g;
	    *sum++ = x1->t * x2->t;		  	    
	  }
      }

    if(!p->tip && !q->tip)
      {	
	 while ((! p->x) || (! q->x)) 
	   {
	     if (! (p->x)) if (! newviewGTRCAT(tr, p)) return badZ;
	     if (! (q->x)) if (! newviewGTRCAT(tr, q)) return badZ;
	   }

	 x1 = x1_start = (likelivector*)p->x;
	 x2 = x2_start = (likelivector*)q->x;


#pragma omp parallel for private(x1, x2, sum)
	for (i = 0; i < tr->cdta->endsite; i++) 
	  {     
	    x1 = &x1_start[i];
	    x2 = &x2_start[i];
	    sum = &sumtable[i * 4];

	    *sum++ = x1->a * x2->a;
	    *sum++ = x1->c * x2->c;
	    *sum++ = x1->g * x2->g;
	    *sum++ = x1->t * x2->t;			    	  	 
	  }
      }
      
    z = z0;
    do {
      int curvatOK = FALSE;

      zprev = z;
      
      zstep = (1.0 - zmax) * z + zmin;

      do {		
	double lz;
        double *wrptr   = &(tr->cdta->wr[0]);
        double *wr2ptr  = &(tr->cdta->wr2[0]);
	double dd1, dd2, dd3;	
	double tmp_0, tmp_1, tmp_2;
	double inv_Li, dlnLidlz, d2lnLidlz2;

        dlnLdlz = 0.0;                 
        d2lnLdlz2 = 0.0;              

        if (z < zmin) z = zmin;
        else if (z > zmax) z = zmax;
        lz    = log(z);
        
	rptr   = &(tr->cdta->patrat[0]); 
	cptr  = &(tr->cdta->rateCategory[0]);
	sum = sumtable;
	
	dd1 = s1 * lz;
	dd2 = s2 * lz;
	dd3 = s3 * lz;

	d1 = d_start;

	for(i = 0; i < tr->NumberOfCategories; i++)
	  {
	    ki = *rptr++;	   
	    *d1++ = exp (dd1 * ki); 
	    *d1++ = exp (dd2 * ki);
	    *d1++ = exp (dd3 * ki);	    
	  }

#pragma omp parallel for private(d1, sum, inv_Li, tmp_0, tmp_1, tmp_2,  dlnLidlz, d2lnLidlz2) reduction(+ : dlnLdlz) reduction( + : d2lnLdlz2)
        for (i = 0; i < tr->cdta->endsite; i++) 	
	  {	    	   	    
	    d1 = &d_start[3 * cptr[i]];
	    sum = &sumtable[i * 4];
	    	  
	    inv_Li = *sum++;	   
	    inv_Li += (tmp_0 = d1[0] * *sum++);
	    inv_Li += (tmp_1 = d1[1] * *sum++);
	    inv_Li += (tmp_2 = d1[2] * *sum++);

	    inv_Li = 1.0/inv_Li;	    	  	   
	    	    	    	    
	    dlnLidlz   = tmp_0 * s1;
	    d2lnLidlz2 = tmp_0 * e1;
 
	    dlnLidlz   += tmp_1 * s2;
	    d2lnLidlz2 += tmp_1 * e2;

	    dlnLidlz   += tmp_2 * s3;	    	   	   	   
	    d2lnLidlz2 += tmp_2 * e3;
	    
	    dlnLidlz   *= inv_Li;
	    d2lnLidlz2 *= inv_Li;
	    dlnLdlz    += wrptr[i]  * dlnLidlz;
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

    free (sumtable);
    free(d_start);
   
    return  z;
  } /* makenewz */

