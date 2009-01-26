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



boolean newviewGTRGAMMAPROT(tree    *tr, nodeptr  p)
{
  if (p->tip) return TRUE;
    
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
		if (! r->x) if (! newviewGTRGAMMAPROT(tr, r)) return FALSE;
		if (! p->x) if (! getxnode(p)) return FALSE;		       
	      }	   
	  }
	else
	  {
	    while ((! p->x) || (! q->x) || (! r->x)) 
	      {
		if (! q->x) if (! newviewGTRGAMMAPROT(tr, q)) return FALSE;
		if (! r->x) if (! newviewGTRGAMMAPROT(tr, r)) return FALSE;
		if (! p->x) if (! getxnode(p)) return FALSE;	
	      }
	  }
      }
    {

      double  *left, *right, *left_start, *right_start, *uX1, *uX2;
      double *EIGN    = tr->EIGN, *v;
      double *EV = tr->EV;
      double x1px2;   
      int  i, l, k, scale;
      protgammalikelivector   *x1, *x2, *x3;
      double   z1, lz1, z2, lz2, ki, ump_x1_0;       
      double  d1[19], d2[19];
                
      z1  = q->z;
      lz1 = (z1 > zmin) ? log(z1) : log(zmin);
      
      z2  = r->z;
      lz2 = (z2 > zmin) ? log(z2) : log(zmin);
      
      left_start  = left = (double *) malloc(1520 * sizeof(double));
      right_start = right = (double *)malloc(1520 * sizeof(double));
      
      for(i = 0; i < 4;i++)
	{       
	  ki = tr->gammaRates[i];	    
	  
	  for(k = 0; k < 19; k++)
	    {
	      d1[k] = exp (EIGN[k] * ki * lz1);
	      d2[k] = exp (EIGN[k] * ki * lz2);
	    }
	
	  EV = tr->EI;
	  for(k = 0; k < 20; k++)
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
	  
	  	  
	  EV = tr->EI;
	  for(k = 0; k < 20; k++)
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
	 char *tipX1 = q->tip;
	 char *tipX2 = r->tip;       
	 double umpX1[1840], umpX2[1840];
	 uX1 = umpX1;
	 uX2 = umpX2;
	 
	 for(i = 0; i < 23; i++)
	   {	    	     
	     v = &(tr->protTip[i].v[0]);	     
	     left = left_start;
	     right = right_start;
	   
	     for(k = 0; k < 80; k++)
	       {
		 ump_x1_0 = v[0];
		 for(l = 1; l < 20; l++)
		   ump_x1_0 +=  v[l] * *left++;
		 *uX1++ = ump_x1_0;
	       }
	    
	     for(k = 0; k < 80; k++)
	       {
		 ump_x1_0 = v[0];
		 for(l = 1; l < 20; l++)
		   ump_x1_0 +=  v[l] * *right++;
		 *uX2++ = ump_x1_0;
	       }
	   }	
	
	 x3  = (protgammalikelivector*)p->x; 
	 
	 for (i = 0; i < tr->cdta->endsite; i++) 
	   {		     
	     uX1 = &umpX1[80 * *tipX1++];
	     uX2 = &umpX2[80 * *tipX2++];

	     for(k = 0; k < 4; k++)
	       {
		 EV = tr->EV;
		 x1px2 = *uX1++ * *uX2++;
		 v = &(x3->v[k * 20]);
		 for(l = 0; l < 20; l++)
		   v[l] = x1px2 *  *EV++;
		 for(l = 0; l < 19; l++)
		   {
		     x1px2 = *uX1++ * *uX2++;
		     v[0] += x1px2 *   *EV++;
		     v[1] += x1px2 *   *EV++;
		     v[2] += x1px2 *   *EV++;
		     v[3] += x1px2 *   *EV++;
		     v[4] += x1px2 *   *EV++;
		     v[5] += x1px2 *   *EV++;
		     v[6] += x1px2 *   *EV++;
		     v[7] += x1px2 *   *EV++;
		     v[8] += x1px2 *   *EV++;
		     v[9] += x1px2 *   *EV++;
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
	 char *tipX1 = q->tip;    
	 double umpX1[1840], ump_x2[20];	
	 uX1 = umpX1;
	 
	 for(i = 0; i < 23; i++)
	   {	    	     		 
	    v = &(tr->protTip[i].v[0]);	     
	    left = left_start;
		       
	     for(k = 0; k < 80; k++)
	       {		 		 	
		 ump_x1_0 = v[0];
		 for(l = 1; l < 20; l++)		 
		   ump_x1_0 +=  v[l] * *left++;		 
		 *uX1++ = ump_x1_0;
	       }
	   }

	 x2  = (protgammalikelivector*)r->x;
	 x3  = (protgammalikelivector*)p->x; 	        	    	       	
	     
	 for (i = 0; i < tr->cdta->endsite; i++) 
	   {		   
	     right = right_start;
	     uX1 = &umpX1[80 * *tipX1++];			    	    
	    
	     for(k = 0; k < 4; k++)
	       {
		 v = &(x2->v[k * 20]);	    
		 for(l = 0; l < 20; l++)
		   {		     
		     ump_x2[l] =  v[0];		 
		     ump_x2[l] += v[1] * *right++;
		     ump_x2[l] += v[2] * *right++;
		     ump_x2[l] += v[3] * *right++;
		     ump_x2[l] += v[4] * *right++;
		     ump_x2[l] += v[5] * *right++;
		     ump_x2[l] += v[6] * *right++;
		     ump_x2[l] += v[7] * *right++;
		     ump_x2[l] += v[8] * *right++;
		     ump_x2[l] += v[9] * *right++;
		     ump_x2[l] += v[10] * *right++;
		     ump_x2[l] += v[11] * *right++;
		     ump_x2[l] += v[12] * *right++;
		     ump_x2[l] += v[13] * *right++;
		     ump_x2[l] += v[14] * *right++;
		     ump_x2[l] += v[15] * *right++;
		     ump_x2[l] += v[16] * *right++;
		     ump_x2[l] += v[17] * *right++;
		     ump_x2[l] += v[18] * *right++;
		     ump_x2[l] += v[19] * *right++;
		   }
		 	     
		 uX2 = ump_x2;
		 EV = tr->EV;
		 x1px2 = *uX1++ * *uX2++;
		 v = &(x3->v[20 * k]);
		 for(l = 0; l < 20; l++)
		   v[l] = x1px2 *  *EV++;
		 
		 for(l = 0; l < 19; l++)
		   {
		     x1px2 = *uX1++ * *uX2++;
		     v[0] += x1px2 *   *EV++;
		     v[1] += x1px2 *   *EV++;
		     v[2] += x1px2 *   *EV++;
		     v[3] += x1px2 *   *EV++;
		     v[4] += x1px2 *   *EV++;
		     v[5] += x1px2 *   *EV++;
		     v[6] += x1px2 *   *EV++;
		     v[7] += x1px2 *   *EV++;
		     v[8] += x1px2 *   *EV++;
		     v[9] += x1px2 *   *EV++;
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
	       }	
	     	     		 	     	     	
	     x3->exp = x2->exp;		
	     v = x3->v;
	     scale = 1;
	     for(l = 0; scale && (l < 80); l++)
	       scale = (ABS(v[l]) <  minlikelihood);
		    
	     if (scale)	         
	       {	     	    	 
		 for(l = 0; l < 80; l++)
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
     		
     x1  = (protgammalikelivector*)q->x;
     x2  = (protgammalikelivector*)r->x;
     x3  = (protgammalikelivector*)p->x;     
	     
     for (i = 0; i < tr->cdta->endsite; i++) 
       {	
	 double *vl, *vr, al, ar;
	 left = left_start;
	 right = right_start;


	 for(k = 0; k < 4; k++)
	   {
	     EV = tr->EV;
	     vl = &(x1->v[20 * k]);
	     vr = &(x2->v[20 * k]);
	     v = &(x3->v[20 * k]);
	     	    
	     for(l = 0; l < 20; l++)	       
	       v[l] = 0;		 
	       
	     for(l = 0; l < 20; l++)
	       {
		 al =  vl[0];		 
		 al += vl[1] * *left++;
		 al += vl[2] * *left++;
		 al += vl[3] * *left++;
		 al += vl[4] * *left++;
		 al += vl[5] * *left++;
		 al += vl[6] * *left++;
		 al += vl[7] * *left++;
		 al += vl[8] * *left++;
		 al += vl[9] * *left++;
		 al += vl[10] * *left++;
		 al += vl[11] * *left++;
		 al += vl[12] * *left++;
		 al += vl[13] * *left++;
		 al += vl[14] * *left++;
		 al += vl[15] * *left++;
		 al += vl[16] * *left++;
		 al += vl[17] * *left++;
		 al += vl[18] * *left++;
		 al += vl[19] * *left++;

		 ar =  vr[0];		 
		 ar += vr[1] * *right++;
		 ar += vr[2] * *right++;
		 ar += vr[3] * *right++;
		 ar += vr[4] * *right++;
		 ar += vr[5] * *right++;
		 ar += vr[6] * *right++;
		 ar += vr[7] * *right++;
		 ar += vr[8] * *right++;
		 ar += vr[9] * *right++;
		 ar += vr[10] * *right++;
		 ar += vr[11] * *right++;
		 ar += vr[12] * *right++;
		 ar += vr[13] * *right++;
		 ar += vr[14] * *right++;
		 ar += vr[15] * *right++;
		 ar += vr[16] * *right++;
		 ar += vr[17] * *right++;
		 ar += vr[18] * *right++;
		 ar += vr[19] * *right++;
		 
		 x1px2 = al * ar;
		 v[0] += x1px2 *   *EV++;
		 v[1] += x1px2 *   *EV++;
		 v[2] += x1px2 *   *EV++;
		 v[3] += x1px2 *   *EV++;
		 v[4] += x1px2 *   *EV++;
		 v[5] += x1px2 *   *EV++;
		 v[6] += x1px2 *   *EV++;
		 v[7] += x1px2 *   *EV++;
		 v[8] += x1px2 *   *EV++;
		 v[9] += x1px2 *   *EV++;
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
	   }
	 
	 x3->exp = x1->exp + x2->exp;
	 v = x3->v;
	 scale = 1;
	 for(l = 0; scale && (l < 80); l++)
	   scale = ((ABS(v[l]) <  minlikelihood));
	 
	 if (scale)	         
	   {	     	    	 
	     for(l = 0; l < 80; l++)
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


/*************************************************************************************************************************************************************/



/*************************************************************************************************************************************************************/


double evaluateGTRGAMMAPROT (tr, p)
    tree    *tr;
    nodeptr  p;
  { /* evaluate */
    double   sum, z, lz, term, ki, term2;    
    nodeptr  q;
    int     i, j;
    int     *wptr = tr->cdta->aliaswgt;    
    double  *diagptable, *diagptable_start;
    double *EIGN = tr->EIGN, *left, *right;
    protgammalikelivector   *x1, *x2;
       
    q = p->back;
        
    z = p->z;
   
    if (z < zmin) z = zmin;
    lz = log(z);
       
    diagptable = diagptable_start = (double *)malloc(sizeof(double) * 4 * 19);
    for(i = 0; i < 4; i++)
      {
	ki = tr->gammaRates[i];	 

	for(j = 0; j < 19; j++)
	  *diagptable++ = exp (EIGN[j] * ki * lz);	
      }
	    
    sum = 0.0;
  

    if(p->tip && !q->tip)
      {
	char *tipX1 = p->tip;      
	
	 while ((! q->x)) 
	   {	     
	     if (! (q->x)) if (! newviewGTRGAMMAPROT(tr, q)) return badEval;
	   }
	 
	 x2  = (protgammalikelivector*)q->x;

	for (i = 0; i < tr->cdta->endsite; i++) 
	  {
	    left = &(tr->protTip[*tipX1++].v[0]);
	    diagptable = diagptable_start;
	    	 
	    term = 0;

	    for(j = 0; j < 4; j++)
	      {
		right = &(x2->v[20 * j]);
		
		term +=  left[0] * right[0];
		term +=  left[1] * right[1] * *diagptable++;
		term +=  left[2] * right[2] * *diagptable++;
		term +=  left[3] * right[3] * *diagptable++;
		term +=  left[4] * right[4] * *diagptable++;
		term +=  left[5] * right[5] * *diagptable++;
		term +=  left[6] * right[6] * *diagptable++;	
		term +=  left[7] * right[7] * *diagptable++;
		term +=  left[8] * right[8] * *diagptable++;
		term +=  left[9] * right[9] * *diagptable++;
		term +=  left[10] * right[10] * *diagptable++;
		term +=  left[11] * right[11] * *diagptable++;
		term +=  left[12] * right[12] * *diagptable++;
		term +=  left[13] * right[13] * *diagptable++;
		term +=  left[14] * right[14] * *diagptable++;
		term +=  left[15] * right[15] * *diagptable++;
		term +=  left[16] * right[16] * *diagptable++;
		term +=  left[17] * right[17] * *diagptable++;
		term +=  left[18] * right[18] * *diagptable++;	
		term +=  left[19] * right[19] * *diagptable++;
	      }	  
	        
	    term = log(0.25 * term) + (x2->exp)*log(minlikelihood);	   
	    sum += *wptr++ * term;		  	   
	    x2++;
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
	    if (! (p->x)) if (! newviewGTRGAMMAPROT(tr, p)) return badEval;	  
	  }

	x1  = (protgammalikelivector*)p->x;
          
	for (i = 0; i < tr->cdta->endsite; i++) 
	  {
	    right = &(tr->protTip[*tipX2++].v[0]);
	    diagptable = diagptable_start;
	    	   
	    term = 0;

	    for(j = 0; j < 4; j++)
	      {
		left = &(x1->v[20 * j]);	
       
		term +=  left[0] * right[0];
		term +=  left[1] * right[1] * *diagptable++;
		term +=  left[2] * right[2] * *diagptable++;
		term +=  left[3] * right[3] * *diagptable++;
		term +=  left[4] * right[4] * *diagptable++;
		term +=  left[5] * right[5] * *diagptable++;
		term +=  left[6] * right[6] * *diagptable++;	
		term +=  left[7] * right[7] * *diagptable++;
		term +=  left[8] * right[8] * *diagptable++;
		term +=  left[9] * right[9] * *diagptable++;
		term +=  left[10] * right[10] * *diagptable++;
		term +=  left[11] * right[11] * *diagptable++;
		term +=  left[12] * right[12] * *diagptable++;
		term +=  left[13] * right[13] * *diagptable++;
		term +=  left[14] * right[14] * *diagptable++;
		term +=  left[15] * right[15] * *diagptable++;
		term +=  left[16] * right[16] * *diagptable++;
		term +=  left[17] * right[17] * *diagptable++;
		term +=  left[18] * right[18] * *diagptable++;	
		term +=  left[19] * right[19] * *diagptable++;
	      }
	       	    
	    term = log(0.25 * term) + (x1->exp)*log(minlikelihood);
	    sum += *wptr++ * term;		  
	    x1++;	  
	  }
    


	free(diagptable_start); 
	
	tr->likelihood = sum;
	return  sum;
      }

     while ((! p->x) || (! q->x)) 
       {
	 if (! (p->x)) if (! newviewGTRGAMMAPROT(tr, p)) return badEval;
	 if (! (q->x)) if (! newviewGTRGAMMAPROT(tr, q)) return badEval;
       }

     x1  = (protgammalikelivector*)p->x;
     x2  = (protgammalikelivector*)q->x;


    for (i = 0; i < tr->cdta->endsite; i++) 
      {	  	 	  
	diagptable = diagptable_start;

	term = 0;

	for(j = 0; j < 4; j++)
	  {
	    left  = &(x1->v[20 * j]);
	    right = &(x2->v[20 * j]);	    
	    term +=  left[0] * right[0];
	    term +=  left[1] * right[1] * *diagptable++;
	    term +=  left[2] * right[2] * *diagptable++;
	    term +=  left[3] * right[3] * *diagptable++;
	    term +=  left[4] * right[4] * *diagptable++;
	    term +=  left[5] * right[5] * *diagptable++;
	    term +=  left[6] * right[6] * *diagptable++;	
	    term +=  left[7] * right[7] * *diagptable++;
	    term +=  left[8] * right[8] * *diagptable++;
	    term +=  left[9] * right[9] * *diagptable++;
	    term +=  left[10] * right[10] * *diagptable++;
	    term +=  left[11] * right[11] * *diagptable++;
	    term +=  left[12] * right[12] * *diagptable++;
	    term +=  left[13] * right[13] * *diagptable++;
	    term +=  left[14] * right[14] * *diagptable++;
	    term +=  left[15] * right[15] * *diagptable++;
	    term +=  left[16] * right[16] * *diagptable++;
	    term +=  left[17] * right[17] * *diagptable++;
	    term +=  left[18] * right[18] * *diagptable++;	
	    term +=  left[19] * right[19] * *diagptable++;	
	  }
	
	term = log(0.25 * term) + (x1->exp + x2->exp)*log(minlikelihood);
	sum += *wptr++ * term;		  
	x1++;
	x2++;
      }
    
      
    free(diagptable_start); 
    
    tr->likelihood = sum;
    return  sum;
  } /* evaluate */




double makenewzGTRGAMMAPROT (tr, p, q, z0, maxiter)
    tree    *tr;
    nodeptr  p, q;
    double   z0;
    int  maxiter;
  { /* makenewz */
    double   z, zprev, zstep;
    protgammalikelivector  *x1, *x2;
    double  *sumtable, *sum, *diagptable, *diagptable_start;
    int     i, l;
    double  dlnLdlz = 0;
    double d2lnLdlz2 = 0;
    double sumc, ki, kisqr;   
    double *EIGN    = tr->EIGN, *left, *right;
       
    sum = sumtable = (double *)malloc(77 * tr->cdta->endsite * sizeof(double));
    diagptable = diagptable_start = (double *)malloc(sizeof(double) * 228);
      
    if(p->tip && q->tip)
      {
	char *tipX1 = p->tip;
	char *tipX2 = q->tip;       
	
	for (i = 0; i < tr->cdta->endsite; i++) 
	  {     
	    left = &(tr->protTip[*tipX1++].v[0]);
	    right = &(tr->protTip[*tipX2++].v[0]);      

	    sumc = 0.0;

	    for(l = 0; l < 4; l++)
	      {
		sumc += left[0] * right[0];
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
		    
	    *sum++ = sumc;		    
	  }
      }
    else
      {
	if(p->tip && !q->tip)
	  {
	    char *tipX1 = p->tip;       	   

	    while ((! q->x)) 
	      {	
		if (! (q->x)) if (! newviewGTRGAMMAPROT(tr, q)) return badZ;
	      }
	   
	    x2 = (protgammalikelivector*)q->x;

	    for (i = 0; i < tr->cdta->endsite; i++) 
	      {     
		left = &(tr->protTip[*tipX1++].v[0]);		    	  
		
		sumc = 0.0;

		for(l = 0; l < 4; l++)
		  {
		    right = &(x2->v[l * 20]);
		    sumc += left[0] * right[0];
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
    
		*sum++ = sumc;	       
		x2++;		 
	      }
	  }
	else
	  {
	    if(!p->tip && q->tip)
	      {		
		char *tipX2 = q->tip;	      

		while ((! p->x)) 
		  {	
		    if (! (p->x)) if (! newviewGTRGAMMAPROT(tr, p)) return badZ;
		  }
	   
		x1 = (protgammalikelivector*)p->x;

		for (i = 0; i < tr->cdta->endsite; i++) 
		  {     
		    right = &(tr->protTip[*tipX2++].v[0]);		    	      
		
		    sumc = 0.0;

		    for(l = 0; l < 4; l++)
		      {
			left = &(x1->v[l * 20]);
			sumc += left[0] * right[0];
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
    
		    *sum++ = sumc;
				    		    
		    x1++;		   
		  }
	      }
	    else
	      {
		while ((! p->x) || (! q->x)) 
		  {	
		    if (! (p->x)) if (! newviewGTRGAMMAPROT(tr, p)) return badZ;
		    if (! (q->x)) if (! newviewGTRGAMMAPROT(tr, q)) return badZ;
		  }
	   
		x1 = (protgammalikelivector*)p->x;
		x2 = (protgammalikelivector*)q->x;

		for (i = 0; i < tr->cdta->endsite; i++) 
		  {     		    
		    sumc = 0.0;
		    
		    for(l = 0; l < 4; l++)
		      {
			left = &(x1->v[l * 20]);
			right = &(x2->v[l * 20]);
			
			sumc += left[0] * right[0];	
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
			
		    *sum++ = sumc;
		    
		    x1++;
		    x2++;		 
		  }
	      }
	  }
      }
          
    z = z0;
    do {
      int curvatOK = FALSE;

      zprev = z;
      
      zstep = (1.0 - zmax) * z + zmin;

      do {		
	double lz;
        int *wrptr   = tr->cdta->aliaswgt;        

        dlnLdlz = 0.0;                 /*  = d(ln(likelihood))/d(lz) */
        d2lnLdlz2 = 0.0;               /*  = d2(ln(likelihood))/d(lz)2 */


        if (z < zmin) z = zmin;
        else if (z > zmax) z = zmax;
        lz    = log(z);
        
	sum = sumtable;

	diagptable = diagptable_start;
	
	for(i = 0; i < 4; i++)
	  {	   
	    ki = tr->gammaRates[i];	 
	    kisqr = ki * ki;

	    for(l = 0; l < 19; l++)
	      {	      
		*diagptable++ = exp (EIGN[l] * ki * lz);
		*diagptable++ = EIGN[l] * ki;
		*diagptable++ = EIGN[l] * EIGN[l] * kisqr;
	      }			    	      
	  }

	

        for (i = 0; i < tr->cdta->endsite; i++) 
	  {
	    double tmp;
	    double inv_Li, dlnLidlz, d2lnLidlz2;	    	   
	    diagptable = diagptable_start;
	    	  
	    inv_Li     = (tmp = *diagptable++  * *sum++);
	    dlnLidlz   = tmp * *diagptable++;
	    d2lnLidlz2 = tmp * *diagptable++;

	    for(l = 1; l < 76; l++)
	      {		
		inv_Li     += (tmp = *diagptable++  * *sum++);
		dlnLidlz   += tmp * *diagptable++;
		d2lnLidlz2 += tmp * *diagptable++;				
	      }
	    
	    inv_Li += *sum++;	   	   	 

	    inv_Li = 1.0 / inv_Li;
	    
	    dlnLidlz   *= inv_Li;
	    d2lnLidlz2 *= inv_Li;

	    dlnLdlz   += *wrptr * dlnLidlz;
	    d2lnLdlz2 += *wrptr++ * (d2lnLidlz2 - dlnLidlz * dlnLidlz);
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
	  if (z < zmin) 
	    {
	      z = zmin;	    
	    }
	  if (z > 0.25 * zprev + 0.75)    /*  Limit steps toward z = 1.0 */
	    z = 0.25 * zprev + 0.75;
	} else {
	  z = 0.25 * zprev + 0.75;
	}
      }
      if (z > zmax) z = zmax;

    } while ((--maxiter > 0) && (ABS(z - zprev) > zstep));

    free(sumtable);
    free(diagptable_start);
  
    return  z;
  } /* makenewz */







