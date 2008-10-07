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

boolean newviewGTRGAMMAMULT(tree    *tr, nodeptr  p)
{
  if (p->tip) return TRUE;
    
  { 	
    double  *left, *right, *left_start, *right_start;
    double *EIGN;
    double *EV = tr->EV;
    double tmp_x1_1, tmp_x1_2, tmp_x1_3,
      tmp_x2_1, tmp_x2_2, tmp_x2_3, 
      ump_x1_1, ump_x1_2, ump_x1_3, ump_x1_0, 
      ump_x2_0, ump_x2_1, ump_x2_2, ump_x2_3, x1px2;   
    int  i;
    gammalikelivector   *x1, *x2, *x3;
    double   z1, lz1, z2, lz2, ki;
    nodeptr  q, r;   
    double  d1c, d1g, d1t, d2c, d2g, d2t;
    int model, *modelptr;
   
    modelptr = tr->model;

    q = p->next->back;
    r = p->next->next->back;
                
    z1  = q->z;
    lz1 = (z1 > zmin) ? log(z1) : log(zmin);
   
    z2  = r->z;
    lz2 = (z2 > zmin) ? log(z2) : log(zmin);

    left_start =  left = (double *) malloc(48 * tr->NumberOfModels * sizeof(double));
    right_start = right = (double *)malloc(48 * tr->NumberOfModels * sizeof(double));
     
    for(model = 0; model < tr->NumberOfModels; model++)
      {
	EIGN = &(tr->EIGN[3 * model]);

	left  = &left_start[model * 48];
	right = &right_start[model * 48];

	for(i = 0; i < 4; i++)
	  {       
	    ki = tr->gammaRates[model * 4 + i];
	    
	    EV = &(tr->EI[model * 12]);
	    
	    d1c = exp (EIGN[0] * ki * lz1);
	    d1g = exp (EIGN[1] * ki * lz1);
	    d1t = exp (EIGN[2] * ki * lz1);	
	    
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
	    
	    EV = &(tr->EI[model * 12]);
	    d2c = exp (EIGN[0] * ki * lz2);
	    d2g = exp (EIGN[1] * ki * lz2);
	    d2t = exp (EIGN[2] * ki * lz2);		
	
	    *right++ = d2c * *EV++;
	    *right++ = d2g * *EV++;
	    *right++ = d2t * *EV++;

	    *right++ = d2c * *EV++;
	    *right++ = d2g * *EV++;
	    *right++ = d2t * *EV++;
	    
	    *right++ = d2c * *EV++;
	    *right++ = d2g * *EV++;
	    *right++ = d2t * *EV++;
	    
	    *right++ = d2c * *EV++;
	    *right++ = d2g * *EV++;
	    *right++ = d2t * *EV++;
	  }                 
      }
    
     if(r->tip && q->tip)
       {
	 char *tipX1 = q->tip;
	 char *tipX2 = r->tip;
	 likelivector *x1, *x2;
	 double *uX1, *umpX1, *uX2, *umpX2;	

	 umpX1 = (double *)malloc(256 * tr->NumberOfModels * sizeof(double));
	 umpX2 = (double *)malloc(256 * tr->NumberOfModels * sizeof(double));
	 
	 for(model = 0; model < tr->NumberOfModels; model++)
	   {
	     uX1 = &umpX1[256 * model + 16];
	     uX2 = &umpX2[256 * model + 16];

	     for(i = 1; i < 16; i++)
	       {	    	     
		 x1 = &(tr->gtrTip[model * 16 + i]);
		 x2 = &(tr->gtrTip[model * 16 + i]);

		 left  = &left_start[model * 48];
		 right = &right_start[model * 48];
	     
		 ump_x1_0 =  x1->c * *left++;
		 ump_x1_0 += x1->g * *left++;
		 ump_x1_0 += x1->t * *left++;	      			
		 ump_x1_0 += x1->a;
	     
		 *uX1++ = ump_x1_0;
	     
		 ump_x1_1 =  x1->c * *left++;
		 ump_x1_1 += x1->g * *left++;
		 ump_x1_1 += x1->t * *left++;	      			
		 ump_x1_1 += x1->a;
		 
		 *uX1++ = ump_x1_1;
		 
		 ump_x1_2 =  x1->c * *left++;
		 ump_x1_2 += x1->g * *left++;
		 ump_x1_2 += x1->t * *left++;		
		 ump_x1_2 += x1->a;
	     
		 *uX1++ = ump_x1_2;
		 
		 ump_x1_3 =  x1->c * *left++;
		 ump_x1_3 += x1->g * *left++;
		 ump_x1_3 += x1->t * *left++;		 
		 ump_x1_3 += x1->a;
		 
		 *uX1++ = ump_x1_3;		
		 
		 ump_x2_0 =  x2->c * *right++;
		 ump_x2_0 += x2->g * *right++;
		 ump_x2_0 += x2->t * *right++;	
		 ump_x2_0 += x2->a;
	     
		 *uX2++ = ump_x2_0;
	     
		 ump_x2_1 =  x2->c * *right++;
		 ump_x2_1 += x2->g * *right++;
		 ump_x2_1 += x2->t * *right++;	
		 ump_x2_1 += x2->a;	 
		 
		 *uX2++ = ump_x2_1;
		 
		 ump_x2_2 =  x2->c * *right++;
		 ump_x2_2 += x2->g * *right++;
		 ump_x2_2 += x2->t * *right++;	       
		 ump_x2_2 += x2->a;	  
	     
		 *uX2++ = ump_x2_2;
		 
		 ump_x2_3 =  x2->c * *right++;
		 ump_x2_3 += x2->g * *right++;
		 ump_x2_3 += x2->t * *right++;		     	       
		 ump_x2_3 += x2->a;	    
		 
		 *uX2++ = ump_x2_3;
		 
		 ump_x1_0 =  x1->c * *left++;
		 ump_x1_0 += x1->g * *left++;
		 ump_x1_0 += x1->t * *left++;	      			
		 ump_x1_0 += x1->a;
		 
		 *uX1++ = ump_x1_0;
		 
		 ump_x1_1 =  x1->c * *left++;
		 ump_x1_1 += x1->g * *left++;
		 ump_x1_1 += x1->t * *left++;	      			
		 ump_x1_1 += x1->a;
		 
		 *uX1++ = ump_x1_1;
		 
		 ump_x1_2 =  x1->c * *left++;
		 ump_x1_2 += x1->g * *left++;
		 ump_x1_2 += x1->t * *left++;		
		 ump_x1_2 += x1->a;
		 
		 *uX1++ = ump_x1_2;
		 
		 ump_x1_3 =  x1->c * *left++;
		 ump_x1_3 += x1->g * *left++;
		 ump_x1_3 += x1->t * *left++;		 
		 ump_x1_3 += x1->a;
	     
		 *uX1++ = ump_x1_3;		
	     
		 ump_x2_0 =  x2->c * *right++;
		 ump_x2_0 += x2->g * *right++;
		 ump_x2_0 += x2->t * *right++;	
		 ump_x2_0 += x2->a;
		 
		 *uX2++ = ump_x2_0;
		 
		 ump_x2_1 =  x2->c * *right++;
		 ump_x2_1 += x2->g * *right++;
		 ump_x2_1 += x2->t * *right++;	
		 ump_x2_1 += x2->a;	 
	     
		 *uX2++ = ump_x2_1;
		 
		 ump_x2_2 =  x2->c * *right++;
		 ump_x2_2 += x2->g * *right++;
		 ump_x2_2 += x2->t * *right++;	       
		 ump_x2_2 += x2->a;	  
		 
		 *uX2++ = ump_x2_2;
		 
		 ump_x2_3 =  x2->c * *right++;
		 ump_x2_3 += x2->g * *right++;
		 ump_x2_3 += x2->t * *right++;		     	       
		 ump_x2_3 += x2->a;	    
		 
		 *uX2++ = ump_x2_3;
		 
		 ump_x1_0 =  x1->c * *left++;
		 ump_x1_0 += x1->g * *left++;
		 ump_x1_0 += x1->t * *left++;	      			
		 ump_x1_0 += x1->a;
		 
		 *uX1++ = ump_x1_0;
		 
		 ump_x1_1 =  x1->c * *left++;
		 ump_x1_1 += x1->g * *left++;
		 ump_x1_1 += x1->t * *left++;	      			
		 ump_x1_1 += x1->a;
		 
		 *uX1++ = ump_x1_1;
	     
		 ump_x1_2 =  x1->c * *left++;
		 ump_x1_2 += x1->g * *left++;
		 ump_x1_2 += x1->t * *left++;		
		 ump_x1_2 += x1->a;
	     
		 *uX1++ = ump_x1_2;
	     
		 ump_x1_3 =  x1->c * *left++;
		 ump_x1_3 += x1->g * *left++;
		 ump_x1_3 += x1->t * *left++;		 
		 ump_x1_3 += x1->a;
		 
		 *uX1++ = ump_x1_3;		
		 
		 ump_x2_0 =  x2->c * *right++;
		 ump_x2_0 += x2->g * *right++;
		 ump_x2_0 += x2->t * *right++;	
		 ump_x2_0 += x2->a;
	     
		 *uX2++ = ump_x2_0;
		 
		 ump_x2_1 =  x2->c * *right++;
		 ump_x2_1 += x2->g * *right++;
		 ump_x2_1 += x2->t * *right++;	
		 ump_x2_1 += x2->a;	 
		 
		 *uX2++ = ump_x2_1;
		 
		 ump_x2_2 =  x2->c * *right++;
		 ump_x2_2 += x2->g * *right++;
		 ump_x2_2 += x2->t * *right++;	       
		 ump_x2_2 += x2->a;	  
		 
		 *uX2++ = ump_x2_2;
	     
		 ump_x2_3 =  x2->c * *right++;
		 ump_x2_3 += x2->g * *right++;
		 ump_x2_3 += x2->t * *right++;		     	       
		 ump_x2_3 += x2->a;	    
		 
		 *uX2++ = ump_x2_3;
		 
		 ump_x1_0 =  x1->c * *left++;
		 ump_x1_0 += x1->g * *left++;
		 ump_x1_0 += x1->t * *left++;	      			
		 ump_x1_0 += x1->a;
		 
		 *uX1++ = ump_x1_0;
		 
		 ump_x1_1 =  x1->c * *left++;
		 ump_x1_1 += x1->g * *left++;
		 ump_x1_1 += x1->t * *left++;	      			
		 ump_x1_1 += x1->a;
		 
		 *uX1++ = ump_x1_1;
		 
		 ump_x1_2 =  x1->c * *left++;
		 ump_x1_2 += x1->g * *left++;
		 ump_x1_2 += x1->t * *left++;		
		 ump_x1_2 += x1->a;
		 
		 *uX1++ = ump_x1_2;
		 
		 ump_x1_3 =  x1->c * *left++;
		 ump_x1_3 += x1->g * *left++;
		 ump_x1_3 += x1->t * *left++;		 
		 ump_x1_3 += x1->a;
	     
		 *uX1++ = ump_x1_3;		
		 
		 ump_x2_0 =  x2->c * *right++;
		 ump_x2_0 += x2->g * *right++;
		 ump_x2_0 += x2->t * *right++;	
		 ump_x2_0 += x2->a;
		 
		 *uX2++ = ump_x2_0;
		 
		 ump_x2_1 =  x2->c * *right++;
		 ump_x2_1 += x2->g * *right++;
		 ump_x2_1 += x2->t * *right++;	
		 ump_x2_1 += x2->a;	 
		 
		 *uX2++ = ump_x2_1;
		 
		 ump_x2_2 =  x2->c * *right++;
		 ump_x2_2 += x2->g * *right++;
		 ump_x2_2 += x2->t * *right++;	       
		 ump_x2_2 += x2->a;	  
		 
		 *uX2++ = ump_x2_2;
		 
		 ump_x2_3 =  x2->c * *right++;
		 ump_x2_3 += x2->g * *right++;
		 ump_x2_3 += x2->t * *right++;		     	       
		 ump_x2_3 += x2->a;	    
		 
		 *uX2++ = ump_x2_3;	       
	       }
	   }

	 while ((! p->x)) 
	   {	     
	     if (! p->x) if (! getxnode(p)) return FALSE;	
	   }
	
	 x3  = (gammalikelivector*)p->x; 
	 
	 for (i = 0; i < tr->cdta->endsite; i++) 
	   {
	     model = *modelptr++;
		     
	     uX1 = &umpX1[256 * model + 16 * *tipX1++];
	     uX2 = &umpX2[256 * model + 16 * *tipX2++];

	     EV = &(tr->EV[model * 16]);				
	     x1px2 = *uX1++ * *uX2++;
	     x3->a0 = x1px2 *  *EV++;
	     x3->c0 = x1px2 *  *EV++;
	     x3->g0 = x1px2 * *EV++;
	     x3->t0 = x1px2 *  *EV++;
	     
	     x1px2 = *uX1++ * *uX2++;
	     x3->a0 += x1px2  *  *EV++;
	     x3->c0 += x1px2 *   *EV++;
	     x3->g0 += x1px2 *  *EV++;
	     x3->t0 += x1px2 *   *EV++;
	    
	     x1px2 = *uX1++ * *uX2++;
	     x3->a0 += x1px2 *  *EV++;
	     x3->c0 += x1px2*   *EV++;
	     x3->g0 += x1px2 *  *EV++;
	     x3->t0 += x1px2 *  *EV++;
	     
	     x1px2 = *uX1++ * *uX2++;
	     x3->a0 += x1px2 *   *EV++;
	     x3->c0 += x1px2 *   *EV++;
	     x3->g0 += x1px2 *   *EV++;
	     x3->t0 += x1px2 *   *EV++;

	     /* rate 1 */
	     
	     EV = &(tr->EV[model * 16]);		
	     x1px2 = *uX1++ * *uX2++;
	     x3->a1 = x1px2 *  *EV++;
	     x3->c1 = x1px2 *  *EV++;
	     x3->g1 = x1px2 * *EV++;
	     x3->t1 = x1px2 *  *EV++;
	     
	     x1px2 = *uX1++ * *uX2++;
	     x3->a1 += x1px2  *  *EV++;
	     x3->c1 += x1px2 *   *EV++;
	     x3->g1 += x1px2 *  *EV++;
	     x3->t1 += x1px2 *   *EV++;
	     
	     x1px2 = *uX1++ * *uX2++;
	     x3->a1 += x1px2 *  *EV++;
	     x3->c1 += x1px2*   *EV++;
	     x3->g1 += x1px2 *  *EV++;
	     x3->t1 += x1px2 *  *EV++;
	    
	     x1px2 = *uX1++ * *uX2++;
	     x3->a1 += x1px2 *   *EV++;
	     x3->c1 += x1px2 *   *EV++;
	     x3->g1 += x1px2 *   *EV++;
	     x3->t1 += x1px2 *   *EV++;

	     /* rate 2 */
	     	     
	     EV = &(tr->EV[model * 16]);	
	     x1px2 = *uX1++ * *uX2++;
	     x3->a2 = x1px2 *  *EV++;
	     x3->c2 = x1px2 *  *EV++;
	     x3->g2 = x1px2 * *EV++;
	     x3->t2 = x1px2 *  *EV++;
	
	     x1px2 = *uX1++ * *uX2++;
	     x3->a2 += x1px2 *  *EV++;
	     x3->c2 += x1px2 *  *EV++;
	     x3->g2 += x1px2 *  *EV++;
	     x3->t2 += x1px2 *  *EV++;
	    
	     x1px2 =  *uX1++ * *uX2++;/*ump_x1_2 * ump_x2_2;*/
	     x3->a2 += x1px2 *  *EV++;
	     x3->c2 += x1px2*   *EV++;
	     x3->g2 += x1px2 *  *EV++;
	     x3->t2 += x1px2 *  *EV++;
	     
	     x1px2 =  *uX1++ * *uX2++;/*ump_x1_3 * ump_x2_3;*/
	     x3->a2 += x1px2 *   *EV++;
	     x3->c2 += x1px2 *   *EV++;
	     x3->g2 += x1px2 *   *EV++;
	     x3->t2 += x1px2 *   *EV++;

	     /* rate 3 */	    

	     EV = &(tr->EV[model * 16]);	
	     x1px2 =  *uX1++ * *uX2++;/*ump_x1_0 * ump_x2_0;*/
	     x3->a3 = x1px2 *  *EV++;
	     x3->c3 = x1px2 *  *EV++;
	     x3->g3 = x1px2 * *EV++;
	     x3->t3 = x1px2 *  *EV++;
	     
	     x1px2 =  *uX1++ * *uX2++;/*ump_x1_1 * ump_x2_1;*/
	     x3->a3 += x1px2  *  *EV++;
	     x3->c3 += x1px2 *   *EV++;
	     x3->g3 += x1px2 *  *EV++;
	     x3->t3 += x1px2 *   *EV++;
	    
	     x1px2 =  *uX1++ * *uX2++;/*ump_x1_2 * ump_x2_2;*/
	     x3->a3 += x1px2 *  *EV++;
	     x3->c3 += x1px2*   *EV++;
	     x3->g3 += x1px2 *  *EV++;
	     x3->t3 += x1px2 *  *EV++;
	     
	     x1px2 =  *uX1++ * *uX2++;/*ump_x1_3 * ump_x2_3;*/
	     x3->a3 += x1px2 *   *EV++;
	     x3->c3 += x1px2 *   *EV++;
	     x3->g3 += x1px2 *   *EV++;
	     x3->t3 += x1px2 *   *EV++;

	     /********************************************************************************/

	     x3->exp = 0;		  		    	     
	       
	     x3++;
      
	   }
         
	  free(left_start); 
	  free(right_start);
	  free(umpX1);
	  free(umpX2);
	 
	 return TRUE;
       }
     

     if(r->tip && !q->tip)
       {	 
	 char *tipX2 = r->tip;
	 likelivector *x2;
	 double *uX2, *umpX2;
	 
	 umpX2 = (double *)malloc(256 * tr->NumberOfModels * sizeof(double));
	 for(model = 0; model < tr->NumberOfModels; model++)
	   {	    
	     uX2 = &umpX2[256 * model + 16];

	     for(i = 1; i < 16; i++)
	       {	    	     	     
		 x2 = &(tr->gtrTip[model * 16 + i]);	     
		 right = &right_start[model * 48];	   

		 ump_x2_0 =  x2->c * *right++;
		 ump_x2_0 += x2->g * *right++;
		 ump_x2_0 += x2->t * *right++;		
		 ump_x2_0 += x2->a;
	     
		 *uX2++ = ump_x2_0;
		 
		 ump_x2_1 =  x2->c * *right++;
		 ump_x2_1 += x2->g * *right++;
		 ump_x2_1 += x2->t * *right++;	       
		 ump_x2_1 += x2->a;	 
	     
		 *uX2++ = ump_x2_1;
		 
		 ump_x2_2 =  x2->c * *right++;
		 ump_x2_2 += x2->g * *right++;
		 ump_x2_2 += x2->t * *right++;	       
		 ump_x2_2 += x2->a;	  
		 
		 *uX2++ = ump_x2_2;
		 
		 ump_x2_3 =  x2->c * *right++;
		 ump_x2_3 += x2->g * *right++;
		 ump_x2_3 += x2->t * *right++;		     	       
		 ump_x2_3 += x2->a;	    
		 
		 *uX2++ = ump_x2_3;
		 
		 ump_x2_0 =  x2->c * *right++;
		 ump_x2_0 += x2->g * *right++;
		 ump_x2_0 += x2->t * *right++;		
		 ump_x2_0 += x2->a;
	     
		 *uX2++ = ump_x2_0;
		 
		 ump_x2_1 =  x2->c * *right++;
		 ump_x2_1 += x2->g * *right++;
		 ump_x2_1 += x2->t * *right++;	       
		 ump_x2_1 += x2->a;	 
		 
		 *uX2++ = ump_x2_1;
		 
		 ump_x2_2 =  x2->c * *right++;
		 ump_x2_2 += x2->g * *right++;
		 ump_x2_2 += x2->t * *right++;	       
		 ump_x2_2 += x2->a;	  
		 
		 *uX2++ = ump_x2_2;
		 
		 ump_x2_3 =  x2->c * *right++;
		 ump_x2_3 += x2->g * *right++;
		 ump_x2_3 += x2->t * *right++;		     	       
		 ump_x2_3 += x2->a;	    
		 
		 *uX2++ = ump_x2_3;
		 
		 ump_x2_0 =  x2->c * *right++;
		 ump_x2_0 += x2->g * *right++;
		 ump_x2_0 += x2->t * *right++;		
		 ump_x2_0 += x2->a;
		 
		 *uX2++ = ump_x2_0;
		 
		 ump_x2_1 =  x2->c * *right++;
		 ump_x2_1 += x2->g * *right++;
		 ump_x2_1 += x2->t * *right++;	       
		 ump_x2_1 += x2->a;	 
		 
		 *uX2++ = ump_x2_1;
		 
		 ump_x2_2 =  x2->c * *right++;
		 ump_x2_2 += x2->g * *right++;
		 ump_x2_2 += x2->t * *right++;	       
		 ump_x2_2 += x2->a;	  
	     
		 *uX2++ = ump_x2_2;
		 
		 ump_x2_3 =  x2->c * *right++;
		 ump_x2_3 += x2->g * *right++;
		 ump_x2_3 += x2->t * *right++;		     	       
		 ump_x2_3 += x2->a;	    
		 
		 *uX2++ = ump_x2_3;
		 
		 ump_x2_0 =  x2->c * *right++;
		 ump_x2_0 += x2->g * *right++;
		 ump_x2_0 += x2->t * *right++;		
		 ump_x2_0 += x2->a;
		 
		 *uX2++ = ump_x2_0;
		 
		 ump_x2_1 =  x2->c * *right++;
		 ump_x2_1 += x2->g * *right++;
		 ump_x2_1 += x2->t * *right++;	       
		 ump_x2_1 += x2->a;	 
		 
		 *uX2++ = ump_x2_1;
		 
		 ump_x2_2 =  x2->c * *right++;
		 ump_x2_2 += x2->g * *right++;
		 ump_x2_2 += x2->t * *right++;	       
		 ump_x2_2 += x2->a;	  
		 
		 *uX2++ = ump_x2_2;
		 
		 ump_x2_3 =  x2->c * *right++;
		 ump_x2_3 += x2->g * *right++;
		 ump_x2_3 += x2->t * *right++;		     	       
		 ump_x2_3 += x2->a;	    
		 
		 *uX2++ = ump_x2_3;	    
	       }
	   }

	 while ((! p->x) || (! q->x)) 
	   {
	     if (! q->x) if (! newviewGTRGAMMAMULT(tr, q)) return FALSE;	    
	     if (! p->x) if (! getxnode(p)) return FALSE;	
	   }
	

	 x1  = (gammalikelivector*)q->x;
	 x3  = (gammalikelivector*)p->x; 	        	    	       	
 
	 for (i = 0; i < tr->cdta->endsite; i++) 
	   {
	     model = *modelptr++;
	     left = &left_start[model * 48];	     
	   
	     uX2 = &umpX2[256 * model + 16 * *tipX2++];
	
	     /* Rate cat 0 */
	     EV = &(tr->EV[model * 16]);
	     ump_x1_0 = x1->c0 * *left++;
	     ump_x1_0 += x1->g0 * *left++;
	     ump_x1_0 += x1->t0* *left++;	      		   
	     ump_x1_0 += x1->a0;
	
	     ump_x1_1 =  x1->c0 * *left++;
	     ump_x1_1 += x1->g0 * *left++;
	     ump_x1_1 += x1->t0 * *left++;	      		    
	     ump_x1_1 += x1->a0;
	    
	     ump_x1_2 = x1->c0 * *left++;
	     ump_x1_2 += x1->g0 * *left++;
	     ump_x1_2 += x1->t0 * *left++;	      		  
	     ump_x1_2 += x1->a0;
	     
	     ump_x1_3 =  x1->c0 * *left++;
	     ump_x1_3 += x1->g0 * *left++;
	     ump_x1_3 += x1->t0 * *left++;	      		  
	     ump_x1_3 += x1->a0;
			 	 	    	    	     	     	    	  		   	  	    	
	     x1px2 = ump_x1_0 * *uX2++;
	     x3->a0 = x1px2 *  *EV++;
	     x3->c0 = x1px2 *  *EV++;
	     x3->g0 = x1px2 * *EV++;
	     x3->t0 = x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_1 *  *uX2++;
	     x3->a0 += x1px2  *  *EV++;
	     x3->c0 += x1px2 *   *EV++;
	     x3->g0 += x1px2 *  *EV++;
	     x3->t0 += x1px2 *   *EV++;
	    
	     x1px2 = ump_x1_2 * *uX2++;
	     x3->a0 += x1px2 *  *EV++;
	     x3->c0 += x1px2*   *EV++;
	     x3->g0 += x1px2 *  *EV++;
	     x3->t0 += x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_3 * *uX2++;
	     x3->a0 += x1px2 *   *EV++;
	     x3->c0 += x1px2 *   *EV++;
	     x3->g0 += x1px2 *   *EV++;
	     x3->t0 += x1px2 *   *EV++;


	     /* rate 1 */
	     
	     EV = &(tr->EV[model * 16]);
	     ump_x1_0 = x1->c1 * *left++;
	     ump_x1_0 += x1->g1 * *left++;
	     ump_x1_0 += x1->t1 * *left++;	      		  
	     ump_x1_0 += x1->a1;
	     
	     ump_x1_1 =  x1->c1 * *left++;
	     ump_x1_1 += x1->g1 * *left++;
	     ump_x1_1 += x1->t1 * *left++;	      		
	     ump_x1_1 += x1->a1;
	     
	     ump_x1_2 = x1->c1 * *left++;
	     ump_x1_2 += x1->g1 * *left++;
	     ump_x1_2 += x1->t1 * *left++;	      		   
	     ump_x1_2 += x1->a1;
	     
	     ump_x1_3 =  x1->c1 * *left++;
	     ump_x1_3 += x1->g1 * *left++;
	     ump_x1_3 += x1->t1 * *left++;	      		   
	     ump_x1_3 += x1->a1;
	     
	     
	      		   	  	    
	     
	     x1px2 = ump_x1_0 * *uX2++;
	     x3->a1 = x1px2 *  *EV++;
	     x3->c1 = x1px2 *  *EV++;
	     x3->g1 = x1px2 * *EV++;
	     x3->t1 = x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_1 * *uX2++;
	     x3->a1 += x1px2  *  *EV++;
	     x3->c1 += x1px2 *   *EV++;
	     x3->g1 += x1px2 *  *EV++;
	     x3->t1 += x1px2 *   *EV++;
	     
	     x1px2 = ump_x1_2 * *uX2++;;
	     x3->a1 += x1px2 *  *EV++;
	     x3->c1 += x1px2*   *EV++;
	     x3->g1 += x1px2 *  *EV++;
	     x3->t1 += x1px2 *  *EV++;
	    
	     x1px2 = ump_x1_3 *  *uX2++;;
	     x3->a1 += x1px2 *   *EV++;
	     x3->c1 += x1px2 *   *EV++;
	     x3->g1 += x1px2 *   *EV++;
	     x3->t1 += x1px2 *   *EV++;

	     /* rate 2 */
	     
	     EV = &(tr->EV[model * 16]);
	     ump_x1_0 = x1->c2 * *left++;
	     ump_x1_0 += x1->g2 * *left++;
	     ump_x1_0 += x1->t2 * *left++;	      		    
	     ump_x1_0 += x1->a2;
	     
	     ump_x1_1 =  x1->c2 * *left++;
	     ump_x1_1 += x1->g2 * *left++;
	     ump_x1_1 += x1->t2 * *left++;	      		   
	     ump_x1_1 += x1->a2;
	     
	     ump_x1_2 = x1->c2 * *left++;
	     ump_x1_2 += x1->g2 * *left++;
	     ump_x1_2 += x1->t2 * *left++;	      		   
	     ump_x1_2 += x1->a2;
	    
	     ump_x1_3 =  x1->c2 * *left++;
	     ump_x1_3 += x1->g2 * *left++;
	     ump_x1_3 += x1->t2 * *left++;	      		  
	     ump_x1_3 += x1->a2;
			 	 	    	    	     	     	    
	     x1px2 = ump_x1_0 * *uX2++;
	     x3->a2 = x1px2 *  *EV++;
	     x3->c2 = x1px2 *  *EV++;
	     x3->g2 = x1px2 * *EV++;
	     x3->t2 = x1px2 *  *EV++;
	
	     x1px2 = ump_x1_1 * *uX2++;
	     x3->a2 += x1px2  *  *EV++;
	     x3->c2 += x1px2 *   *EV++;
	     x3->g2 += x1px2 *  *EV++;
	     x3->t2 += x1px2 *   *EV++;
	    
	     x1px2 = ump_x1_2 * *uX2++;;
	     x3->a2 += x1px2 *  *EV++;
	     x3->c2 += x1px2*   *EV++;
	     x3->g2 += x1px2 *  *EV++;
	     x3->t2 += x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_3 *  *uX2++;;
	     x3->a2 += x1px2 *   *EV++;
	     x3->c2 += x1px2 *   *EV++;
	     x3->g2 += x1px2 *   *EV++;
	     x3->t2 += x1px2 *   *EV++;

	     /* rate 3 */
	     EV = &(tr->EV[model * 16]);
	     ump_x1_0 = x1->c3 * *left++;
	     ump_x1_0 += x1->g3 * *left++;
	     ump_x1_0 += x1->t3 * *left++;	      		  
	     ump_x1_0 += x1->a3;
	     
	     ump_x1_1 =  x1->c3 * *left++;
	     ump_x1_1 += x1->g3 * *left++;
	     ump_x1_1 += x1->t3 * *left++;	      		  
	     ump_x1_1 += x1->a3;
	    
	     ump_x1_2 = x1->c3 * *left++;
	     ump_x1_2 += x1->g3 * *left++;
	     ump_x1_2 += x1->t3 * *left++;	      		  
	     ump_x1_2 += x1->a3;
	    
	     ump_x1_3 =  x1->c3 * *left++;
	     ump_x1_3 += x1->g3 * *left++;
	     ump_x1_3 += x1->t3 * *left++;	      		    
	     ump_x1_3 += x1->a3;
		
	 	 	    	    	     	     
	     		   	  	    
	
	     x1px2 = ump_x1_0 * *uX2++;
	     x3->a3 = x1px2 *  *EV++;
	     x3->c3 = x1px2 *  *EV++;
	     x3->g3 = x1px2 * *EV++;
	     x3->t3 = x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_1 * *uX2++;
	     x3->a3 += x1px2  *  *EV++;
	     x3->c3 += x1px2 *   *EV++;
	     x3->g3 += x1px2 *  *EV++;
	     x3->t3 += x1px2 *   *EV++;
	    
	     x1px2 = ump_x1_2 * *uX2++;
	     x3->a3 += x1px2 *  *EV++;
	     x3->c3 += x1px2*   *EV++;
	     x3->g3 += x1px2 *  *EV++;
	     x3->t3 += x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_3 *  *uX2++;
	     x3->a3 += x1px2 *   *EV++;
	     x3->c3 += x1px2 *   *EV++;
	     x3->g3 += x1px2 *   *EV++;
	     x3->t3 += x1px2 *   *EV++;

	     /********************************************************************************/


	     x3->exp = x1->exp;		  
	
	     if (ABS(x3->a0) < minlikelihood && ABS(x3->g0) < minlikelihood && ABS(x3->c0) < minlikelihood && ABS(x3->t0) < minlikelihood &&
		 ABS(x3->a1) < minlikelihood && ABS(x3->g1) < minlikelihood && ABS(x3->c1) < minlikelihood && ABS(x3->t1) < minlikelihood &&
		 ABS(x3->a2) < minlikelihood && ABS(x3->g2) < minlikelihood && ABS(x3->c2) < minlikelihood && ABS(x3->t2) < minlikelihood &&
		 ABS(x3->a3) < minlikelihood && ABS(x3->g3) < minlikelihood && ABS(x3->c3) < minlikelihood && ABS(x3->t3) < minlikelihood) 
	       {	     

		

		 x3->a0   *= twotothe256;
		 x3->c0   *= twotothe256;
		 x3->g0   *= twotothe256;
		 x3->t0   *= twotothe256;
		 
		 x3->a1   *= twotothe256;
		 x3->c1   *= twotothe256;
		 x3->g1   *= twotothe256;
		 x3->t1   *= twotothe256;
		 
		 x3->a2   *= twotothe256;
		 x3->c2   *= twotothe256;
		 x3->g2   *= twotothe256;
		 x3->t2   *= twotothe256;
		 
		 x3->a3   *= twotothe256;
		 x3->c3   *= twotothe256;
		 x3->g3   *= twotothe256;
		 x3->t3   *= twotothe256;
		 
		 x3->exp += 1;
	       }

	     x1++;  
	     x3++;
      
	   }

	  free(left_start); 
	  free(right_start);
	  free(umpX2);
	  return TRUE;
       }
     
     if(!r->tip && q->tip)
       {	 
	 char *tipX1 = q->tip;
	 likelivector *x1;
	 double *uX1, *umpX1;	
	  
	 umpX1 = (double *)malloc(256 * tr->NumberOfModels * sizeof(double));

	 
	 for(model = 0; model < tr->NumberOfModels; model++)
	   {
	     uX1 = &umpX1[256 * model + 16];
	 
	     for(i = 1; i < 16; i++)
	       {	    	     	     
		 x1 = &(tr->gtrTip[model * 16 + i]);	     
		 left = &left_start[model * 48];
	     
		 ump_x1_0 =  x1->c * *left++;
		 ump_x1_0 += x1->g * *left++;
		 ump_x1_0 += x1->t * *left++;	       
		 ump_x1_0 += x1->a;

		 *uX1++ = ump_x1_0;
	     
		 ump_x1_1 =  x1->c * *left++;
		 ump_x1_1 += x1->g * *left++;
		 ump_x1_1 += x1->t * *left++;		 
		 ump_x1_1 += x1->a;	 
	     
		 *uX1++ = ump_x1_1;
	     
		 ump_x1_2 =  x1->c * *left++;
		 ump_x1_2 += x1->g * *left++;
		 ump_x1_2 += x1->t * *left++;		
		 ump_x1_2 += x1->a;	  
		 
		 *uX1++ = ump_x1_2;
		 
		 ump_x1_3 =  x1->c * *left++;
		 ump_x1_3 += x1->g * *left++;
		 ump_x1_3 += x1->t * *left++;		     	      
		 ump_x1_3 += x1->a;	    
		 
		 *uX1++ = ump_x1_3;
		 
		 ump_x1_0 =  x1->c * *left++;
		 ump_x1_0 += x1->g * *left++;
		 ump_x1_0 += x1->t * *left++;	       
		 ump_x1_0 += x1->a;
		 
		 *uX1++ = ump_x1_0;
		 
		 ump_x1_1 =  x1->c * *left++;
		 ump_x1_1 += x1->g * *left++;
		 ump_x1_1 += x1->t * *left++;		 
		 ump_x1_1 += x1->a;	 
		 
		 *uX1++ = ump_x1_1;
		 
		 ump_x1_2 =  x1->c * *left++;
		 ump_x1_2 += x1->g * *left++;
		 ump_x1_2 += x1->t * *left++;		
		 ump_x1_2 += x1->a;	  
		 
		 *uX1++ = ump_x1_2;
	     
		 ump_x1_3 =  x1->c * *left++;
		 ump_x1_3 += x1->g * *left++;
		 ump_x1_3 += x1->t * *left++;		     	      
		 ump_x1_3 += x1->a;	    
		 
		 *uX1++ = ump_x1_3;
		 
		 ump_x1_0 =  x1->c * *left++;
		 ump_x1_0 += x1->g * *left++;
		 ump_x1_0 += x1->t * *left++;	       
		 ump_x1_0 += x1->a;
		 
		 *uX1++ = ump_x1_0;
		 
		 ump_x1_1 =  x1->c * *left++;
		 ump_x1_1 += x1->g * *left++;
		 ump_x1_1 += x1->t * *left++;		 
		 ump_x1_1 += x1->a;	 
		 
		 *uX1++ = ump_x1_1;
		 
		 ump_x1_2 =  x1->c * *left++;
		 ump_x1_2 += x1->g * *left++;
		 ump_x1_2 += x1->t * *left++;		
		 ump_x1_2 += x1->a;	  
		 
		 *uX1++ = ump_x1_2;
		 
		 ump_x1_3 =  x1->c * *left++;
		 ump_x1_3 += x1->g * *left++;
		 ump_x1_3 += x1->t * *left++;		     	      
		 ump_x1_3 += x1->a;	    
		 
		 *uX1++ = ump_x1_3;
		 
		 ump_x1_0 =  x1->c * *left++;
		 ump_x1_0 += x1->g * *left++;
		 ump_x1_0 += x1->t * *left++;	       
		 ump_x1_0 += x1->a;
		 
		 *uX1++ = ump_x1_0;
	     
		 ump_x1_1 =  x1->c * *left++;
		 ump_x1_1 += x1->g * *left++;
		 ump_x1_1 += x1->t * *left++;		 
		 ump_x1_1 += x1->a;	 
		 
		 *uX1++ = ump_x1_1;
		 
		 ump_x1_2 =  x1->c * *left++;
		 ump_x1_2 += x1->g * *left++;
		 ump_x1_2 += x1->t * *left++;		
		 ump_x1_2 += x1->a;	  
		 
		 *uX1++ = ump_x1_2;
		 
		 ump_x1_3 =  x1->c * *left++;
		 ump_x1_3 += x1->g * *left++;
		 ump_x1_3 += x1->t * *left++;		     	      
		 ump_x1_3 += x1->a;	    
		 
		 *uX1++ = ump_x1_3;		 
	       }
	   }

	while ((! p->x) || (! r->x)) 
	  {	    
	    if (! r->x) if (! newviewGTRGAMMAMULT(tr, r)) return FALSE;
	    if (! p->x) if (! getxnode(p)) return FALSE;	
	  }

	 x2  = (gammalikelivector*)r->x;
	 x3  = (gammalikelivector*)p->x; 	        	    	       	


	 for (i = 0; i < tr->cdta->endsite; i++) 
	   {
	     model = *modelptr++;
	     right = &right_start[model * 48];
	     uX1 = &umpX1[model * 256 + 16 * *tipX1++];			    
						
	     EV = &(tr->EV[model * 16]);						 	 	    	    	     	     
	     ump_x2_0 = x2->c0 * *right++;
	     ump_x2_0 += x2->g0 * *right++;
	     ump_x2_0 += x2->t0 * *right++;		     	   
	     ump_x2_0 += x2->a0;
	
	     ump_x2_1 = x2->c0 * *right++;
	     ump_x2_1 += x2->g0 * *right++;
	     ump_x2_1 +=  x2->t0 * *right++;		     	    
	     ump_x2_1 += x2->a0;	 
	
	     ump_x2_2 = x2->c0 * *right++;
	     ump_x2_2 += x2->g0 * *right++;
	     ump_x2_2 +=  x2->t0 * *right++;		     	     
	     ump_x2_2 += x2->a0;	  
		   
	     ump_x2_3 = x2->c0 * *right++;
	     ump_x2_3 += x2->g0 * *right++;
	     ump_x2_3 += x2->t0 * *right++;		     	     
	     ump_x2_3 += x2->a0;	    	  		   	  	    
	
	     x1px2 = *uX1++ * ump_x2_0;
	     x3->a0 = x1px2 *  *EV++;
	     x3->c0 = x1px2 *  *EV++;
	     x3->g0 = x1px2 * *EV++;
	     x3->t0 = x1px2 *  *EV++;
	
	     x1px2 = *uX1++ * ump_x2_1;
	     x3->a0 += x1px2  *  *EV++;
	     x3->c0 += x1px2 *   *EV++;
	     x3->g0 += x1px2 *  *EV++;
	     x3->t0 += x1px2 *   *EV++;
	    
	     x1px2 = *uX1++ * ump_x2_2;
	     x3->a0 += x1px2 *  *EV++;
	     x3->c0 += x1px2*   *EV++;
	     x3->g0 += x1px2 *  *EV++;
	     x3->t0 += x1px2 *  *EV++;
	    
	     x1px2 = *uX1++ * ump_x2_3;
	     x3->a0 += x1px2 *   *EV++;
	     x3->c0 += x1px2 *   *EV++;
	     x3->g0 += x1px2 *   *EV++;
	     x3->t0 += x1px2 *   *EV++;


	     EV = &(tr->EV[model * 16]);	 	    	    	     	     
	     ump_x2_0 = x2->c1 * *right++;
	     ump_x2_0 += x2->g1 * *right++;
	     ump_x2_0 += x2->t1 * *right++;		     	  
	     ump_x2_0 += x2->a1;
	
	     ump_x2_1 = x2->c1 * *right++;
	     ump_x2_1 += x2->g1 * *right++;
	     ump_x2_1 +=  x2->t1 * *right++;		     	   
	     ump_x2_1 += x2->a1;	 
	
	     ump_x2_2 = x2->c1 * *right++;
	     ump_x2_2 += x2->g1 * *right++;
	     ump_x2_2 +=  x2->t1 * *right++;		     	  
	     ump_x2_2 += x2->a1;	  
	     
	     ump_x2_3 = x2->c1 * *right++;
	     ump_x2_3 += x2->g1 * *right++;
	     ump_x2_3 += x2->t1 * *right++;		     	 
	     ump_x2_3 += x2->a1;	    	  		   	  	    
	
	     x1px2 = *uX1++ * ump_x2_0;
	     x3->a1 = x1px2 *  *EV++;
	     x3->c1 = x1px2 *  *EV++;
	     x3->g1 = x1px2 * *EV++;
	     x3->t1 = x1px2 *  *EV++;
	     
	     x1px2 = *uX1++ * ump_x2_1;
	     x3->a1 += x1px2  *  *EV++;
	     x3->c1 += x1px2 *   *EV++;
	     x3->g1 += x1px2 *  *EV++;
	     x3->t1 += x1px2 *   *EV++;
	     
	     x1px2 = *uX1++ * ump_x2_2;
	     x3->a1 += x1px2 *  *EV++;
	     x3->c1 += x1px2*   *EV++;
	     x3->g1 += x1px2 *  *EV++;
	     x3->t1 += x1px2 *  *EV++;
	     
	     x1px2 = *uX1++ * ump_x2_3;
	     x3->a1 += x1px2 *   *EV++;
	     x3->c1 += x1px2 *   *EV++;
	     x3->g1 += x1px2 *   *EV++;
	     x3->t1 += x1px2 *   *EV++;

	      
	     EV = &(tr->EV[model * 16]);
	     ump_x2_0 = x2->c2 * *right++;
	     ump_x2_0 += x2->g2 * *right++;
	     ump_x2_0 += x2->t2 * *right++;		     	   
	     ump_x2_0 += x2->a2;
	
	     ump_x2_1 = x2->c2 * *right++;
	     ump_x2_1 += x2->g2 * *right++;
	     ump_x2_1 +=  x2->t2 * *right++;		     	    
	     ump_x2_1 += x2->a2;	 
	     
	     ump_x2_2 = x2->c2 * *right++;
	     ump_x2_2 += x2->g2 * *right++;
	     ump_x2_2 +=  x2->t2 * *right++;		     	     
	     ump_x2_2 += x2->a2;	  
	     
	     ump_x2_3 = x2->c2 * *right++;
	     ump_x2_3 += x2->g2 * *right++;
	     ump_x2_3 += x2->t2 * *right++;		     	   
	     ump_x2_3 += x2->a2;	    	  		   	  	    
	
	     x1px2 = *uX1++ * ump_x2_0;
	     x3->a2 = x1px2 *  *EV++;
	     x3->c2 = x1px2 *  *EV++;
	     x3->g2 = x1px2 * *EV++;
	     x3->t2 = x1px2 *  *EV++;
	     
	     x1px2 = *uX1++ * ump_x2_1;
	     x3->a2 += x1px2  *  *EV++;
	     x3->c2 += x1px2 *   *EV++;
	     x3->g2 += x1px2 *  *EV++;
	     x3->t2 += x1px2 *   *EV++;
	     
	     x1px2 = *uX1++ * ump_x2_2;
	     x3->a2 += x1px2 *  *EV++;
	     x3->c2 += x1px2*   *EV++;
	     x3->g2 += x1px2 *  *EV++;
	     x3->t2 += x1px2 *  *EV++;
	    
	     x1px2 = *uX1++ * ump_x2_3;
	     x3->a2 += x1px2 *   *EV++;
	     x3->c2 += x1px2 *   *EV++;
	     x3->g2 += x1px2 *   *EV++;
	     x3->t2 += x1px2 *   *EV++;
	     	
	     EV = &(tr->EV[model * 16]);		 		 	    	    	     	     
	     ump_x2_0 = x2->c3 * *right++;
	     ump_x2_0 += x2->g3 * *right++;
	     ump_x2_0 += x2->t3 * *right++;		     	   
	     ump_x2_0 += x2->a3;
	
	     ump_x2_1 = x2->c3 * *right++;
	     ump_x2_1 += x2->g3 * *right++;
	     ump_x2_1 +=  x2->t3 * *right++;		     	    
	     ump_x2_1 += x2->a3;	 
	     
	     ump_x2_2 = x2->c3 * *right++;
	     ump_x2_2 += x2->g3 * *right++;
	     ump_x2_2 +=  x2->t3 * *right++;		     	     
	     ump_x2_2 += x2->a3;	  
	     
	     ump_x2_3 = x2->c3 * *right++;
	     ump_x2_3 += x2->g3 * *right++;
	     ump_x2_3 += x2->t3 * *right++;		     	    
	     ump_x2_3 += x2->a3;	    	  		   	  	    
	     
	     x1px2 = *uX1++ * ump_x2_0;
	     x3->a3 = x1px2 *  *EV++;
	     x3->c3 = x1px2 *  *EV++;
	     x3->g3 = x1px2 * *EV++;
	     x3->t3 = x1px2 *  *EV++;
	     
	     x1px2 = *uX1++ * ump_x2_1;
	     x3->a3 += x1px2  *  *EV++;
	     x3->c3 += x1px2 *   *EV++;
	     x3->g3 += x1px2 *  *EV++;
	     x3->t3 += x1px2 *   *EV++;
	     
	     x1px2 = *uX1++ * ump_x2_2;
	     x3->a3 += x1px2 *  *EV++;
	     x3->c3 += x1px2*   *EV++;
	     x3->g3 += x1px2 *  *EV++;
	     x3->t3 += x1px2 *  *EV++;
	     
	     x1px2 = *uX1++ * ump_x2_3;
	     x3->a3 += x1px2 *   *EV++;
	     x3->c3 += x1px2 *   *EV++;
	     x3->g3 += x1px2 *   *EV++;
	     x3->t3 += x1px2 *   *EV++;
	     
	

	     x3->exp = x2->exp;		  
	
	     if (ABS(x3->a0) < minlikelihood && ABS(x3->g0) < minlikelihood && ABS(x3->c0) < minlikelihood && ABS(x3->t0) < minlikelihood &&
		 ABS(x3->a1) < minlikelihood && ABS(x3->g1) < minlikelihood && ABS(x3->c1) < minlikelihood && ABS(x3->t1) < minlikelihood &&
		 ABS(x3->a2) < minlikelihood && ABS(x3->g2) < minlikelihood && ABS(x3->c2) < minlikelihood && ABS(x3->t2) < minlikelihood &&
		 ABS(x3->a3) < minlikelihood && ABS(x3->g3) < minlikelihood && ABS(x3->c3) < minlikelihood && ABS(x3->t3) < minlikelihood) 
	       {	     	    	 
		 x3->a0   *= twotothe256;
		 x3->c0   *= twotothe256;
		 x3->g0   *= twotothe256;
		 x3->t0   *= twotothe256;
		 
		 x3->a1   *= twotothe256;
		 x3->c1   *= twotothe256;
		 x3->g1   *= twotothe256;
		 x3->t1   *= twotothe256;
		 
		 x3->a2   *= twotothe256;
		 x3->c2   *= twotothe256;
		 x3->g2   *= twotothe256;
		 x3->t2   *= twotothe256;
		 
		 x3->a3   *= twotothe256;
		 x3->c3   *= twotothe256;
		 x3->g3   *= twotothe256;
		 x3->t3   *= twotothe256;
	   
		 x3->exp += 1;
	       }	 	      
	     x2++;
	     x3++;    
	   }
	
	 free(left_start); 
	 free(right_start);
	 free(umpX1);
         		 
	 return TRUE;
	   }           
    
     while ((! p->x) || (! q->x) || (! r->x)) {
       if (! q->x) if (! newviewGTRGAMMAMULT(tr, q)) return FALSE;
       if (! r->x) if (! newviewGTRGAMMAMULT(tr, r)) return FALSE;
       if (! p->x) if (! getxnode(p)) return FALSE;	
     }
       
    

     x1  = (gammalikelivector*)q->x;
     x2  = (gammalikelivector*)r->x;
     x3  = (gammalikelivector*)p->x;     

    for (i = 0; i < tr->cdta->endsite; i++) 
      {	
	model = *modelptr++;
	
	left  = &left_start[model * 48];
	right = &right_start[model * 48];
	
	/* Rate cat 0 */
	EV = &(tr->EV[model * 16]);	
	ump_x1_0 = x1->c0 * *left++;
	ump_x1_0 += x1->g0 * *left++;
	ump_x1_0 += x1->t0* *left++;	      	      
	ump_x1_0 += x1->a0;
	
	ump_x1_1 =  x1->c0 * *left++;
	ump_x1_1 += x1->g0 * *left++;
	ump_x1_1 += x1->t0* *left++;	      	       
	ump_x1_1 += x1->a0;
	    
	ump_x1_2 = x1->c0 * *left++;
	ump_x1_2 += x1->g0 * *left++;
	ump_x1_2 += x1->t0 * *left++;	      		
	ump_x1_2 += x1->a0;
	    
	ump_x1_3 =  x1->c0 * *left++;
	ump_x1_3 += x1->g0 * *left++;
	ump_x1_3 += x1->t0 * *left++;	      	      
	ump_x1_3 += x1->a0;
		
	 	 	    	    	     	     
	ump_x2_0 = x2->c0 * *right++;
	ump_x2_0 += x2->g0 * *right++;
	ump_x2_0 += x2->t0 * *right++;		     	
	ump_x2_0 += x2->a0;
	
	ump_x2_1 = x2->c0 * *right++;
	ump_x2_1 += x2->g0 * *right++;
	ump_x2_1 +=  x2->t0 * *right++;		     	
	ump_x2_1 += x2->a0;	 
	
	ump_x2_2 = x2->c0 * *right++;
	ump_x2_2 += x2->g0 * *right++;
	ump_x2_2 +=  x2->t0 * *right++;		     	
	ump_x2_2 += x2->a0;	  
		   
	ump_x2_3 = x2->c0 * *right++;
	ump_x2_3 += x2->g0 * *right++;
	ump_x2_3 += x2->t0 * *right++;		     	
	ump_x2_3 += x2->a0;	    	  		   	  	    
	
	x1px2 = ump_x1_0 * ump_x2_0;
	x3->a0 = x1px2 *  *EV++;
	x3->c0 = x1px2 *  *EV++;
	x3->g0 = x1px2 * *EV++;
	x3->t0 = x1px2 *  *EV++;
	
	x1px2 = ump_x1_1 * ump_x2_1;
	x3->a0 += x1px2  *  *EV++;
	x3->c0 += x1px2 *   *EV++;
	x3->g0 += x1px2 *  *EV++;
	x3->t0 += x1px2 *   *EV++;
	    
	x1px2 = ump_x1_2 * ump_x2_2;
	x3->a0 += x1px2 *  *EV++;
	x3->c0 += x1px2*   *EV++;
	x3->g0 += x1px2 *  *EV++;
	x3->t0 += x1px2 *  *EV++;
	    
	x1px2 = ump_x1_3 * ump_x2_3;
	x3->a0 += x1px2 *   *EV++;
	x3->c0 += x1px2 *   *EV++;
	x3->g0 += x1px2 *   *EV++;
	x3->t0 += x1px2 *   *EV++;

	/* rate 1 */

	EV = &(tr->EV[model * 16]);
	ump_x1_0 = x1->c1 * *left++;
	ump_x1_0 += x1->g1 * *left++;
	ump_x1_0 += x1->t1* *left++;	      	     
	ump_x1_0 += x1->a1;
	
	ump_x1_1 =  x1->c1 * *left++;
	ump_x1_1 += x1->g1 * *left++;
	ump_x1_1 += x1->t1* *left++;	      	      
	ump_x1_1 += x1->a1;
	    
	ump_x1_2 = x1->c1 * *left++;
	ump_x1_2 += x1->g1 * *left++;
	ump_x1_2 += x1->t1 * *left++;	      	      
	ump_x1_2 += x1->a1;
	    
	ump_x1_3 =  x1->c1 * *left++;
	ump_x1_3 += x1->g1 * *left++;
	ump_x1_3 += x1->t1 * *left++;	      	
	ump_x1_3 += x1->a1;
		
	 	 	    	    	     	     
	ump_x2_0 = x2->c1 * *right++;
	ump_x2_0 += x2->g1 * *right++;
	ump_x2_0 += x2->t1 * *right++;		           
	ump_x2_0 += x2->a1;
	
	ump_x2_1 = x2->c1 * *right++;
	ump_x2_1 += x2->g1 * *right++;
	ump_x2_1 +=  x2->t1 * *right++;		           
	ump_x2_1 += x2->a1;	 
	
	ump_x2_2 = x2->c1 * *right++;
	ump_x2_2 += x2->g1 * *right++;
	ump_x2_2 +=  x2->t1 * *right++;		           
	ump_x2_2 += x2->a1;	  
		   
	ump_x2_3 = x2->c1 * *right++;
	ump_x2_3 += x2->g1 * *right++;
	ump_x2_3 += x2->t1 * *right++;		            
	ump_x2_3 += x2->a1;	    	  		   	  	    
	
	x1px2 = ump_x1_0 * ump_x2_0;
	x3->a1 = x1px2 *  *EV++;
	x3->c1 = x1px2 *  *EV++;
	x3->g1 = x1px2 * *EV++;
	x3->t1 = x1px2 *  *EV++;
	
	x1px2 = ump_x1_1 * ump_x2_1;
	x3->a1 += x1px2  *  *EV++;
	x3->c1 += x1px2 *   *EV++;
	x3->g1 += x1px2 *  *EV++;
	x3->t1 += x1px2 *   *EV++;
	    
	x1px2 = ump_x1_2 * ump_x2_2;
	x3->a1 += x1px2 *  *EV++;
	x3->c1 += x1px2*   *EV++;
	x3->g1 += x1px2 *  *EV++;
	x3->t1 += x1px2 *  *EV++;
	    
	x1px2 = ump_x1_3 * ump_x2_3;
	x3->a1 += x1px2 *   *EV++;
	x3->c1 += x1px2 *   *EV++;
	x3->g1 += x1px2 *   *EV++;
	x3->t1 += x1px2 *   *EV++;

	/* rate 2 */

	EV = &(tr->EV[model * 16]);
	ump_x1_0 = x1->c2 * *left++;
	ump_x1_0 += x1->g2 * *left++;
	ump_x1_0 += x1->t2 * *left++;	      	     
	ump_x1_0 += x1->a2;
	
	ump_x1_1 =  x1->c2 * *left++;
	ump_x1_1 += x1->g2 * *left++;
	ump_x1_1 += x1->t2* *left++;	      	      
	ump_x1_1 += x1->a2;
	    
	ump_x1_2 = x1->c2 * *left++;
	ump_x1_2 += x1->g2 * *left++;
	ump_x1_2 += x1->t2 * *left++;	      	     
	ump_x1_2 += x1->a2;
	    
	ump_x1_3 =  x1->c2 * *left++;
	ump_x1_3 += x1->g2 * *left++;
	ump_x1_3 += x1->t2 * *left++;	      	     
	ump_x1_3 += x1->a2;
		
	 	 	    	    	     	     
	ump_x2_0 = x2->c2 * *right++;
	ump_x2_0 += x2->g2 * *right++;
	ump_x2_0 += x2->t2 * *right++;		     
	ump_x2_0 += x2->a2;
	
	ump_x2_1 = x2->c2 * *right++;
	ump_x2_1 += x2->g2 * *right++;
	ump_x2_1 +=  x2->t2 * *right++;		         
	ump_x2_1 += x2->a2;	 
	
	ump_x2_2 = x2->c2 * *right++;
	ump_x2_2 += x2->g2 * *right++;
	ump_x2_2 +=  x2->t2 * *right++;		            
	ump_x2_2 += x2->a2;	  
		   
	ump_x2_3 = x2->c2 * *right++;
	ump_x2_3 += x2->g2 * *right++;
	ump_x2_3 += x2->t2 * *right++;		          
	ump_x2_3 += x2->a2;	    	  		   	  	    
	
	x1px2 = ump_x1_0 * ump_x2_0;
	x3->a2 = x1px2 *  *EV++;
	x3->c2 = x1px2 *  *EV++;
	x3->g2 = x1px2 * *EV++;
	x3->t2 = x1px2 *  *EV++;
	
	x1px2 = ump_x1_1 * ump_x2_1;
	x3->a2 += x1px2  *  *EV++;
	x3->c2 += x1px2 *   *EV++;
	x3->g2 += x1px2 *  *EV++;
	x3->t2 += x1px2 *   *EV++;
	    
	x1px2 = ump_x1_2 * ump_x2_2;
	x3->a2 += x1px2 *  *EV++;
	x3->c2 += x1px2*   *EV++;
	x3->g2 += x1px2 *  *EV++;
	x3->t2 += x1px2 *  *EV++;
	    
	x1px2 = ump_x1_3 * ump_x2_3;
	x3->a2 += x1px2 *   *EV++;
	x3->c2 += x1px2 *   *EV++;
	x3->g2 += x1px2 *   *EV++;
	x3->t2 += x1px2 *   *EV++;

	/* rate 3 */
	EV = &(tr->EV[model * 16]);
	ump_x1_0 = x1->c3 * *left++;
	ump_x1_0 += x1->g3 * *left++;
	ump_x1_0 += x1->t3* *left++;	      	     
	ump_x1_0 += x1->a3;
	
	ump_x1_1 =  x1->c3 * *left++;
	ump_x1_1 += x1->g3 * *left++;
	ump_x1_1 += x1->t3* *left++;	      	 
	ump_x1_1 += x1->a3;
	    
	ump_x1_2 = x1->c3 * *left++;
	ump_x1_2 += x1->g3 * *left++;
	ump_x1_2 += x1->t3 * *left++;	      	      
	ump_x1_2 += x1->a3;
	    
	ump_x1_3 =  x1->c3 * *left++;
	ump_x1_3 += x1->g3 * *left++;
	ump_x1_3 += x1->t3 * *left++;	      	     
	ump_x1_3 += x1->a3;
		
	 	 	    	    	     	     
	ump_x2_0 = x2->c3 * *right++;
	ump_x2_0 += x2->g3 * *right++;
	ump_x2_0 += x2->t3 * *right++;		         
	ump_x2_0 += x2->a3;
	
	ump_x2_1 = x2->c3 * *right++;
	ump_x2_1 += x2->g3 * *right++;
	ump_x2_1 +=  x2->t3 * *right++;		           
	ump_x2_1 += x2->a3;	 
	
	ump_x2_2 = x2->c3 * *right++;
	ump_x2_2 += x2->g3 * *right++;
	ump_x2_2 +=  x2->t3 * *right++;		         
	ump_x2_2 += x2->a3;	  
		   
	ump_x2_3 = x2->c3 * *right++;
	ump_x2_3 += x2->g3 * *right++;
	ump_x2_3 += x2->t3 * *right++;		            
	ump_x2_3 += x2->a3;	    	  		   	  	    
	
	x1px2 = ump_x1_0 * ump_x2_0;
	x3->a3 = x1px2 *  *EV++;
	x3->c3 = x1px2 *  *EV++;
	x3->g3 = x1px2 * *EV++;
	x3->t3 = x1px2 *  *EV++;
	
	x1px2 = ump_x1_1 * ump_x2_1;
	x3->a3 += x1px2  *  *EV++;
	x3->c3 += x1px2 *   *EV++;
	x3->g3 += x1px2 *  *EV++;
	x3->t3 += x1px2 *   *EV++;
	    
	x1px2 = ump_x1_2 * ump_x2_2;
	x3->a3 += x1px2 *  *EV++;
	x3->c3 += x1px2*   *EV++;
	x3->g3 += x1px2 *  *EV++;
	x3->t3 += x1px2 *  *EV++;
	    
	x1px2 = ump_x1_3 * ump_x2_3;
	x3->a3 += x1px2 *   *EV++;
	x3->c3 += x1px2 *   *EV++;
	x3->g3 += x1px2 *   *EV++;
	x3->t3 += x1px2 *   *EV++;

	/********************************************************************************/

	x3->exp = x1->exp + x2->exp;		  
	
	if (ABS(x3->a0) < minlikelihood && ABS(x3->g0) < minlikelihood && ABS(x3->c0) < minlikelihood && ABS(x3->t0) < minlikelihood &&
	    ABS(x3->a1) < minlikelihood && ABS(x3->g1) < minlikelihood && ABS(x3->c1) < minlikelihood && ABS(x3->t1) < minlikelihood &&
	    ABS(x3->a2) < minlikelihood && ABS(x3->g2) < minlikelihood && ABS(x3->c2) < minlikelihood && ABS(x3->t2) < minlikelihood &&
	    ABS(x3->a3) < minlikelihood && ABS(x3->g3) < minlikelihood && ABS(x3->c3) < minlikelihood && ABS(x3->t3) < minlikelihood) 
	  {	     
	    x3->a0   *= twotothe256;
	    x3->c0   *= twotothe256;
	    x3->g0   *= twotothe256;
	    x3->t0   *= twotothe256;

	    x3->a1   *= twotothe256;
	    x3->c1   *= twotothe256;
	    x3->g1   *= twotothe256;
	    x3->t1   *= twotothe256;

	    x3->a2   *= twotothe256;
	    x3->c2   *= twotothe256;
	    x3->g2   *= twotothe256;
	    x3->t2   *= twotothe256;

	    x3->a3   *= twotothe256;
	    x3->c3   *= twotothe256;
	    x3->g3   *= twotothe256;
	    x3->t3   *= twotothe256;
	   
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







double evaluateGTRGAMMAMULT (tr, p)
    tree    *tr;
    nodeptr  p;
  { /* evaluate */
    double   sum, z, lz, term, ki, term2;    
    nodeptr  q;
    int     i, j;
    int     *wptr = tr->cdta->aliaswgt;
    int model, *modelptr;  
    double  *diagptable, *diagptable_start;
    double *EIGN;
    gammalikelivector   *x1, *x2;
       
    modelptr = tr->model;
    
    q = p->back;
        
    z = p->z;
   
    if (z < zmin) z = zmin;
    lz = log(z);
       
    diagptable = diagptable_start = (double *)malloc(sizeof(double) * 12 * tr->NumberOfModels);

    for(model = 0; model < tr->NumberOfModels; model++)
      {
	EIGN = &(tr->EIGN[model * 3]);
	diagptable = &diagptable_start[12 * model];

	for(i = 0; i < 4; i++)
	  {
	    ki = tr->gammaRates[model * 4 + i];	 
	    
	    *diagptable++ = exp (EIGN[0] * ki * lz);
	    *diagptable++ = exp (EIGN[1] * ki * lz);
	    *diagptable++ = exp (EIGN[2] * ki * lz);
	  }
      }
	
    
    sum = 0.0;
  

    if(p->tip && !q->tip)
      {      
	char *tipX1 = p->tip;
	likelivector *x1;
	
	 while ((! q->x)) 
	   {	     
	     if (! (q->x)) if (! newviewGTRGAMMAMULT(tr, q)) return badEval;
	   }
	 
	 x2  = (gammalikelivector*)q->x;

	for (i = 0; i < tr->cdta->endsite; i++) 
	  {
	    model = *modelptr++;

	    x1 = &(tr->gtrTip[16 * model + *tipX1++]);
	    diagptable = &diagptable_start[12 * model];
	    
	    /* cat 0 */
	    
	    term =  x1->a * x2->a0;
	    term += x1->c * x2->c0 * *diagptable++;
	    term += x1->g * x2->g0 * *diagptable++;
	    term += x1->t * x2->t0 * *diagptable++;     
	    
	    /* cat 1 */
	    
	    term += x1->a * x2->a1;
	    term += x1->c * x2->c1 * *diagptable++;
	    term += x1->g * x2->g1 * *diagptable++;
	    term += x1->t * x2->t1 * *diagptable++;     
       
	    /* cat 2 */

	    term += x1->a * x2->a2;
	    term += x1->c * x2->c2 * *diagptable++;
	    term += x1->g * x2->g2 * *diagptable++;
	    term += x1->t * x2->t2 * *diagptable++;     
	
	    /* cat 3 */
	
	    term += x1->a * x2->a3;
	    term += x1->c * x2->c3 * *diagptable++;
	    term += x1->g * x2->g3 * *diagptable++;
	    term += x1->t * x2->t3 * *diagptable++;     

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
	likelivector *x2;

	while ((! p->x)) 
	  {
	    if (! (p->x)) if (! newviewGTRGAMMAMULT(tr, p)) return badEval;	  
	  }

	x1  = (gammalikelivector*)p->x;
          
	for (i = 0; i < tr->cdta->endsite; i++) 
	  {
	    model = *modelptr++;

	    x2 = &(tr->gtrTip[16 * model + *tipX2++]);
	    diagptable = &diagptable_start[model * 12];
	    
	    /* cat 0 */
	    
	    term =  x1->a0 * x2->a;
	    term += x1->c0 * x2->c * *diagptable++;
	    term += x1->g0 * x2->g * *diagptable++;
	    term += x1->t0 * x2->t * *diagptable++;     
	    
	    /* cat 1 */
	    
	    term += x1->a1 * x2->a;
	    term += x1->c1 * x2->c * *diagptable++;
	    term += x1->g1 * x2->g * *diagptable++;
	    term += x1->t1 * x2->t * *diagptable++;     

       

	    /* cat 2 */
	    
	    term += x1->a2 * x2->a;
	    term += x1->c2 * x2->c * *diagptable++;
	    term += x1->g2 * x2->g * *diagptable++;
	    term += x1->t2 * x2->t * *diagptable++;     
	    
	

	    /* cat 3 */
	    
	    term +=  x1->a3 * x2->a;
	    term += x1->c3 * x2->c * *diagptable++;
	    term += x1->g3 * x2->g * *diagptable++;
	    term += x1->t3 * x2->t * *diagptable++;     
	    
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
	 if (! (p->x)) if (! newviewGTRGAMMAMULT(tr, p)) return badEval;
	 if (! (q->x)) if (! newviewGTRGAMMAMULT(tr, q)) return badEval;
       }

     x1  = (gammalikelivector*)p->x;
     x2  = (gammalikelivector*)q->x;


    for (i = 0; i < tr->cdta->endsite; i++) 
      {	  	 	  
	model = *modelptr++;

	diagptable = &diagptable_start[model * 12];

	/* cat 0 */

	term =  x1->a0 * x2->a0;
	term += x1->c0 * x2->c0 * *diagptable++;
	term += x1->g0 * x2->g0 * *diagptable++;
	term += x1->t0 * x2->t0 * *diagptable++;     

	/* cat 1 */

	term += x1->a1 * x2->a1;
	term += x1->c1 * x2->c1 * *diagptable++;
	term += x1->g1 * x2->g1 * *diagptable++;
	term += x1->t1 * x2->t1 * *diagptable++;     

       

	/* cat 2 */

	term += x1->a2 * x2->a2;
	term += x1->c2 * x2->c2 * *diagptable++;
	term += x1->g2 * x2->g2 * *diagptable++;
	term += x1->t2 * x2->t2 * *diagptable++;     

	

	/* cat 3 */
	
	term +=  x1->a3 * x2->a3;
	term += x1->c3 * x2->c3 * *diagptable++;
	term += x1->g3 * x2->g3 * *diagptable++;
	term += x1->t3 * x2->t3 * *diagptable++;     

	term = log(0.25 * term) + (x1->exp + x2->exp)*log(minlikelihood);
	sum += *wptr++ * term;		  
	x1++;
	x2++;
      }
    
      
    free(diagptable_start); 
    
    tr->likelihood = sum;
    return  sum;
  } /* evaluate */




double makenewzGTRGAMMAMULT (tr, p, q, z0, maxiter)
    tree    *tr;
    nodeptr  p, q;
    double   z0;
    int  maxiter;
  { /* makenewz */
    double   z, zprev, zstep;
    gammalikelivector  *x1, *x2;
    double  *sumtable, *sum, *diagptable, *diagptable_start;
    int     i;
    double  dlnLdlz = 0;
    double d2lnLdlz2 = 0;
    double sumc, ki, kisqr;   
    double *EIGN;
    int model, *modelptr;   
       
    modelptr = tr->model;

    sum = sumtable = (double *)malloc(13 * tr->cdta->endsite * sizeof(double));
    diagptable = diagptable_start = (double *)malloc(sizeof(double) * 36 * tr->NumberOfModels);
  

    
    if(p->tip && q->tip)
      {
	char *tipX1 = p->tip;
	char *tipX2 = q->tip;
	likelivector *x1, *x2;
	
	for (i = 0; i < tr->cdta->endsite; i++) 
	  { 
	    model = *modelptr++;
	    
	    x1 = &(tr->gtrTip[model * 16 + *tipX1++]);
	    x2 = &(tr->gtrTip[model * 16 + *tipX2++]);

	    sumc = x1->a * x2->a;
	    *sum++ = x1->c * x2->c;
	    *sum++ = x1->g * x2->g;
	    *sum++ = x1->t * x2->t;		    
	    
	    sumc += x1->a * x2->a;
	    *sum++ = x1->c * x2->c;
	    *sum++ = x1->g * x2->g;
	    *sum++ = x1->t * x2->t;
		    
	    sumc += x1->a * x2->a;
	    *sum++ = x1->c * x2->c;
	    *sum++ = x1->g * x2->g;
	    *sum++ = x1->t * x2->t;
		    
	    sumc += x1->a * x2->a;
	    *sum++ = x1->c * x2->c;
	    *sum++ = x1->g * x2->g;
	    *sum++ = x1->t * x2->t;
		    
	    *sum++ = sumc;		    
	  }
      }
    else
      {
	if(p->tip && !q->tip)
	  {
	    char *tipX1 = p->tip;       
	    likelivector *x1;       

	    while ((! q->x)) 
	      {	
		if (! (q->x)) if (! newviewGTRGAMMAMULT(tr, q)) return badZ;
	      }
	   
	    x2 = (gammalikelivector*)q->x;

	    for (i = 0; i < tr->cdta->endsite; i++) 
	      {
		model = *modelptr++;
     
		x1 = &(tr->gtrTip[model * 16 + *tipX1++]);
		    
		sumc = x1->a * x2->a0;
		*sum++ = x1->c * x2->c0;
		*sum++ = x1->g * x2->g0;
		*sum++ = x1->t * x2->t0;		    
		
		sumc += x1->a * x2->a1;
		*sum++ = x1->c * x2->c1;
		*sum++ = x1->g * x2->g1;
		*sum++ = x1->t * x2->t1;
		
		sumc += x1->a * x2->a2;
		*sum++ = x1->c * x2->c2;
		*sum++ = x1->g * x2->g2;
		*sum++ = x1->t * x2->t2;
		
		sumc += x1->a * x2->a3;
		*sum++ = x1->c * x2->c3;
		*sum++ = x1->g * x2->g3;
		*sum++ = x1->t * x2->t3;
		
		*sum++ = sumc;
		
		x2++;		 
	      }
	  }
	else
	  {
	    if(!p->tip && q->tip)
	      {
		
		char *tipX2 = q->tip;
		likelivector *x2;


		while ((! p->x)) 
		  {	
		    if (! (p->x)) if (! newviewGTRGAMMAMULT(tr, p)) return badZ;
		  }
	   
		x1 = (gammalikelivector*)p->x;

		for (i = 0; i < tr->cdta->endsite; i++) 
		  {     
		    model = *modelptr++;

		    x2 = &(tr->gtrTip[model * 16 + *tipX2++]);

		    sumc = x1->a0 * x2->a;
		    *sum++ = x1->c0 * x2->c;
		    *sum++ = x1->g0 * x2->g;
		    *sum++ = x1->t0 * x2->t;		    
		    
		    sumc += x1->a1 * x2->a;
		    *sum++ = x1->c1 * x2->c;
		    *sum++ = x1->g1 * x2->g;
		    *sum++ = x1->t1 * x2->t;
		    
		    sumc += x1->a2 * x2->a;
		    *sum++ = x1->c2 * x2->c;
		    *sum++ = x1->g2 * x2->g;
		    *sum++ = x1->t2 * x2->t;
		    
		    sumc += x1->a3 * x2->a;
		    *sum++ = x1->c3 * x2->c;
		    *sum++ = x1->g3 * x2->g;
		    *sum++ = x1->t3 * x2->t;
		    
		    *sum++ = sumc;
		    
		    x1++;		   
		  }
	      }
	    else
	      {
		while ((! p->x) || (! q->x)) 
		  {	
		    if (! (p->x)) if (! newviewGTRGAMMAMULT(tr, p)) return badZ;
		    if (! (q->x)) if (! newviewGTRGAMMAMULT(tr, q)) return badZ;
		  }
	   
		x1 = (gammalikelivector*)p->x;
		x2 = (gammalikelivector*)q->x;


		for (i = 0; i < tr->cdta->endsite; i++) 
		  {     
		    sumc = x1->a0 * x2->a0;
		    *sum++ = x1->c0 * x2->c0;
		    *sum++ = x1->g0 * x2->g0;
		    *sum++ = x1->t0 * x2->t0;		    
		    
		    sumc += x1->a1 * x2->a1;
		    *sum++ = x1->c1 * x2->c1;
		    *sum++ = x1->g1 * x2->g1;
		    *sum++ = x1->t1 * x2->t1;
		    
		    sumc += x1->a2 * x2->a2;
		    *sum++ = x1->c2 * x2->c2;
		    *sum++ = x1->g2 * x2->g2;
		    *sum++ = x1->t2 * x2->t2;
		    
		    sumc += x1->a3 * x2->a3;
		    *sum++ = x1->c3 * x2->c3;
		    *sum++ = x1->g3 * x2->g3;
		    *sum++ = x1->t3 * x2->t3;
		    
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

	for(model = 0; model < tr->NumberOfModels; model++)
	  {
	    EIGN = &(tr->EIGN[model * 3]);
	    
	    diagptable = &diagptable_start[model * 36];

	    for(i = 0; i < 4; i++)
	      {
		ki = tr->gammaRates[model * 4 + i];	 
		kisqr = ki * ki;

		*diagptable++ = exp (EIGN[0] * ki * lz);
		*diagptable++ = exp (EIGN[1] * ki * lz);
		*diagptable++ = exp (EIGN[2] * ki * lz);
		
		*diagptable++ = EIGN[0] * ki;
		*diagptable++ = EIGN[0] * EIGN[0] * kisqr;
		
		*diagptable++ = EIGN[1] * ki;
		*diagptable++ = EIGN[1] * EIGN[1] * kisqr;
		
		*diagptable++ = EIGN[2] * ki;
		*diagptable++ = EIGN[2] * EIGN[2] * kisqr;
	      }
	  }


	modelptr = tr->model;

        for (i = 0; i < tr->cdta->endsite; i++) 
	  {
	    double tmp_1, tmp_2, tmp_3;
	    double inv_Li, dlnLidlz, d2lnLidlz2;	    	   
	    diagptable = &diagptable_start[36 * *modelptr++];
	    
	    /* rate 0*/	   
	    inv_Li =  (tmp_1 = *diagptable++ * *sum++);
	    inv_Li += (tmp_2 = *diagptable++ * *sum++);
	    inv_Li += (tmp_3 = *diagptable++ * *sum++);	    	   	    	  	   	   	    	   	   	    	   	  	    

	    dlnLidlz   = tmp_1 * *diagptable++;
	    d2lnLidlz2 = tmp_1 * *diagptable++;
	    	   
	    dlnLidlz   += tmp_2 * *diagptable++;
	    d2lnLidlz2 += tmp_2 * *diagptable++;	    
	    
	    dlnLidlz   += tmp_3 * *diagptable++;
	    d2lnLidlz2 += tmp_3 * *diagptable++;	    

	    /* rate 1 */
	    inv_Li += (tmp_1 = *sum++ *  *diagptable++);
	    inv_Li += (tmp_2 = *sum++ *  *diagptable++);
	    inv_Li += (tmp_3 = *sum++ *  *diagptable++);	    	   	   	     	  	   	   

	    dlnLidlz   += tmp_1 * *diagptable++;
	    d2lnLidlz2 += tmp_1 * *diagptable++;
	    	   
	    dlnLidlz   += tmp_2 * *diagptable++;
	    d2lnLidlz2 += tmp_2 * *diagptable++;
	    	    
	    dlnLidlz   += tmp_3 * *diagptable++;
	    d2lnLidlz2 += tmp_3 * *diagptable++;

	    /* rates 2 */	 	    

	    inv_Li += (tmp_1 = *sum++ *  *diagptable++);
	    inv_Li += (tmp_2 = *sum++ *  *diagptable++);
	    inv_Li += (tmp_3 = *sum++ *  *diagptable++);	   

	    dlnLidlz   += tmp_1 * *diagptable++;
	    d2lnLidlz2 += tmp_1 * *diagptable++;
	    	   
	    dlnLidlz   += tmp_2 * *diagptable++;
	    d2lnLidlz2 += tmp_2 * *diagptable++;
	    
	    
	    dlnLidlz   += tmp_3 * *diagptable++;
	    d2lnLidlz2 += tmp_3 * *diagptable++;
	    	    
	    /* rates 3 */	     
	  	 
	    inv_Li += (tmp_1 = *sum++ * *diagptable++);
	    inv_Li += (tmp_2 = *sum++ * *diagptable++);
	    inv_Li += (tmp_3 = *sum++ * *diagptable++);	    
	    inv_Li += *sum++;
	     	   	  	   	    	  	   
	    dlnLidlz   += tmp_1 * *diagptable++;
	    d2lnLidlz2 += tmp_1 * *diagptable++;
	    	   
	    dlnLidlz   += tmp_2 * *diagptable++;
	    d2lnLidlz2 += tmp_2 * *diagptable++;
	    
	    
	    dlnLidlz   += tmp_3 * *diagptable++;
	    d2lnLidlz2 += tmp_3 * *diagptable++;
	    
	    /* end */	    	
	   
	    inv_Li = 1.0 / inv_Li;
	    
	    dlnLidlz   *= inv_Li;
	    d2lnLidlz2 *= inv_Li;

	    dlnLdlz   += *wrptr  * dlnLidlz;
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


/******************************************************************************************************************************************************************/


boolean newviewGTRGAMMAMULTPARTITION(tree    *tr, nodeptr  p, int model)
{
  if (p->tip) return TRUE;
    
  { 	
    double  *left, *right, *left_start, *right_start;
    double *EIGN, *EV;
    double tmp_x1_1, tmp_x1_2, tmp_x1_3,
      tmp_x2_1, tmp_x2_2, tmp_x2_3, 
      ump_x1_1, ump_x1_2, ump_x1_3, ump_x1_0, 
      ump_x2_0, ump_x2_1, ump_x2_2, ump_x2_3, x1px2;   
    int  i;
    gammalikelivector   *x1, *x2, *x3;
    double   z1, lz1, z2, lz2, ki;
    nodeptr  q, r;   
    double  d1c, d1g, d1t, d2c, d2g, d2t;
    int lower, upper;
    
    lower = tr->modelIndices[model][0];
    upper = tr->modelIndices[model][1]; 
   
    q = p->next->back;
    r = p->next->next->back;
                
    z1  = q->z;
    lz1 = (z1 > zmin) ? log(z1) : log(zmin);
   
    z2  = r->z;
    lz2 = (z2 > zmin) ? log(z2) : log(zmin);

    left_start =  left = (double *) malloc(48 * sizeof(double));
    right_start = right = (double *)malloc(48 * sizeof(double));
   
    EIGN = &(tr->EIGN[3 * model]);
  
    for(i = 0; i < 4;i++)
      {       
	ki = tr->gammaRates[model * 4 + i];

	EV = &(tr->EI[model * 12]);

	d1c = exp (EIGN[0] * ki * lz1);
	d1g = exp (EIGN[1] * ki * lz1);
	d1t = exp (EIGN[2] * ki * lz1);	

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
	
	EV  = &(tr->EI[model * 12]);
	d2c = exp (EIGN[0] * ki * lz2);
	d2g = exp (EIGN[1] * ki * lz2);
        d2t = exp (EIGN[2] * ki * lz2);		
	
	*right++ = d2c * *EV++;
	*right++ = d2g * *EV++;
	*right++ = d2t * *EV++;

	*right++ = d2c * *EV++;
	*right++ = d2g * *EV++;
	*right++ = d2t * *EV++;

	*right++ = d2c * *EV++;
	*right++ = d2g * *EV++;
	*right++ = d2t * *EV++;

	*right++ = d2c * *EV++;
	*right++ = d2g * *EV++;
	*right++ = d2t * *EV++;
      }                            	 
    
     if(r->tip && q->tip)
       {
	 char *tipX1 = &(q->tip[lower]);
	 char *tipX2 = &(r->tip[lower]);
	 likelivector *x1, *x2;
	 double *uX1, umpX1[256], *uX2, umpX2[256];
	 uX1 = &umpX1[16];
	 uX2 = &umpX2[16];
	 
	 for(i = 1; i < 16; i++)
	   {	    	     
	     x1 = &(tr->gtrTip[16 * model + i]);
	     x2 = &(tr->gtrTip[16 * model + i]);

	     left = left_start;
	     right = right_start;
	     
	     ump_x1_0 =  x1->c * *left++;
	     ump_x1_0 += x1->g * *left++;
	     ump_x1_0 += x1->t * *left++;	      			
	     ump_x1_0 += x1->a;
	     
	     *uX1++ = ump_x1_0;
	     
	     ump_x1_1 =  x1->c * *left++;
	     ump_x1_1 += x1->g * *left++;
	     ump_x1_1 += x1->t * *left++;	      			
	     ump_x1_1 += x1->a;
	     
	     *uX1++ = ump_x1_1;
	     
	     ump_x1_2 =  x1->c * *left++;
	     ump_x1_2 += x1->g * *left++;
	     ump_x1_2 += x1->t * *left++;		
	     ump_x1_2 += x1->a;
	     
	     *uX1++ = ump_x1_2;
	     
	     ump_x1_3 =  x1->c * *left++;
	     ump_x1_3 += x1->g * *left++;
	     ump_x1_3 += x1->t * *left++;		 
	     ump_x1_3 += x1->a;
	     
	     *uX1++ = ump_x1_3;		
	     
	     ump_x2_0 =  x2->c * *right++;
	     ump_x2_0 += x2->g * *right++;
	     ump_x2_0 += x2->t * *right++;	
	     ump_x2_0 += x2->a;
	     
	     *uX2++ = ump_x2_0;
	     
	     ump_x2_1 =  x2->c * *right++;
	     ump_x2_1 += x2->g * *right++;
	     ump_x2_1 += x2->t * *right++;	
	     ump_x2_1 += x2->a;	 
	     
	     *uX2++ = ump_x2_1;
	     
	     ump_x2_2 =  x2->c * *right++;
	     ump_x2_2 += x2->g * *right++;
	     ump_x2_2 += x2->t * *right++;	       
	     ump_x2_2 += x2->a;	  
	     
	     *uX2++ = ump_x2_2;
	     
	     ump_x2_3 =  x2->c * *right++;
	     ump_x2_3 += x2->g * *right++;
	     ump_x2_3 += x2->t * *right++;		     	       
	     ump_x2_3 += x2->a;	    
	     
	     *uX2++ = ump_x2_3;

	     ump_x1_0 =  x1->c * *left++;
	     ump_x1_0 += x1->g * *left++;
	     ump_x1_0 += x1->t * *left++;	      			
	     ump_x1_0 += x1->a;
	     
	     *uX1++ = ump_x1_0;
	     
	     ump_x1_1 =  x1->c * *left++;
	     ump_x1_1 += x1->g * *left++;
	     ump_x1_1 += x1->t * *left++;	      			
	     ump_x1_1 += x1->a;
	     
	     *uX1++ = ump_x1_1;
	     
	     ump_x1_2 =  x1->c * *left++;
	     ump_x1_2 += x1->g * *left++;
	     ump_x1_2 += x1->t * *left++;		
	     ump_x1_2 += x1->a;
	     
	     *uX1++ = ump_x1_2;
	     
	     ump_x1_3 =  x1->c * *left++;
	     ump_x1_3 += x1->g * *left++;
	     ump_x1_3 += x1->t * *left++;		 
	     ump_x1_3 += x1->a;
	     
	     *uX1++ = ump_x1_3;		
	     
	     ump_x2_0 =  x2->c * *right++;
	     ump_x2_0 += x2->g * *right++;
	     ump_x2_0 += x2->t * *right++;	
	     ump_x2_0 += x2->a;
	     
	     *uX2++ = ump_x2_0;
	     
	     ump_x2_1 =  x2->c * *right++;
	     ump_x2_1 += x2->g * *right++;
	     ump_x2_1 += x2->t * *right++;	
	     ump_x2_1 += x2->a;	 
	     
	     *uX2++ = ump_x2_1;
	     
	     ump_x2_2 =  x2->c * *right++;
	     ump_x2_2 += x2->g * *right++;
	     ump_x2_2 += x2->t * *right++;	       
	     ump_x2_2 += x2->a;	  
	     
	     *uX2++ = ump_x2_2;
	     
	     ump_x2_3 =  x2->c * *right++;
	     ump_x2_3 += x2->g * *right++;
	     ump_x2_3 += x2->t * *right++;		     	       
	     ump_x2_3 += x2->a;	    
	     
	     *uX2++ = ump_x2_3;

	     ump_x1_0 =  x1->c * *left++;
	     ump_x1_0 += x1->g * *left++;
	     ump_x1_0 += x1->t * *left++;	      			
	     ump_x1_0 += x1->a;
	     
	     *uX1++ = ump_x1_0;
	     
	     ump_x1_1 =  x1->c * *left++;
	     ump_x1_1 += x1->g * *left++;
	     ump_x1_1 += x1->t * *left++;	      			
	     ump_x1_1 += x1->a;
	     
	     *uX1++ = ump_x1_1;
	     
	     ump_x1_2 =  x1->c * *left++;
	     ump_x1_2 += x1->g * *left++;
	     ump_x1_2 += x1->t * *left++;		
	     ump_x1_2 += x1->a;
	     
	     *uX1++ = ump_x1_2;
	     
	     ump_x1_3 =  x1->c * *left++;
	     ump_x1_3 += x1->g * *left++;
	     ump_x1_3 += x1->t * *left++;		 
	     ump_x1_3 += x1->a;
	     
	     *uX1++ = ump_x1_3;		
	     
	     ump_x2_0 =  x2->c * *right++;
	     ump_x2_0 += x2->g * *right++;
	     ump_x2_0 += x2->t * *right++;	
	     ump_x2_0 += x2->a;
	     
	     *uX2++ = ump_x2_0;
	     
	     ump_x2_1 =  x2->c * *right++;
	     ump_x2_1 += x2->g * *right++;
	     ump_x2_1 += x2->t * *right++;	
	     ump_x2_1 += x2->a;	 
	     
	     *uX2++ = ump_x2_1;
	     
	     ump_x2_2 =  x2->c * *right++;
	     ump_x2_2 += x2->g * *right++;
	     ump_x2_2 += x2->t * *right++;	       
	     ump_x2_2 += x2->a;	  
	     
	     *uX2++ = ump_x2_2;
	     
	     ump_x2_3 =  x2->c * *right++;
	     ump_x2_3 += x2->g * *right++;
	     ump_x2_3 += x2->t * *right++;		     	       
	     ump_x2_3 += x2->a;	    
	     
	     *uX2++ = ump_x2_3;

	     ump_x1_0 =  x1->c * *left++;
	     ump_x1_0 += x1->g * *left++;
	     ump_x1_0 += x1->t * *left++;	      			
	     ump_x1_0 += x1->a;
	     
	     *uX1++ = ump_x1_0;
	     
	     ump_x1_1 =  x1->c * *left++;
	     ump_x1_1 += x1->g * *left++;
	     ump_x1_1 += x1->t * *left++;	      			
	     ump_x1_1 += x1->a;
	     
	     *uX1++ = ump_x1_1;
	     
	     ump_x1_2 =  x1->c * *left++;
	     ump_x1_2 += x1->g * *left++;
	     ump_x1_2 += x1->t * *left++;		
	     ump_x1_2 += x1->a;
	     
	     *uX1++ = ump_x1_2;
	     
	     ump_x1_3 =  x1->c * *left++;
	     ump_x1_3 += x1->g * *left++;
	     ump_x1_3 += x1->t * *left++;		 
	     ump_x1_3 += x1->a;
	     
	     *uX1++ = ump_x1_3;		
	     
	     ump_x2_0 =  x2->c * *right++;
	     ump_x2_0 += x2->g * *right++;
	     ump_x2_0 += x2->t * *right++;	
	     ump_x2_0 += x2->a;
	     
	     *uX2++ = ump_x2_0;
	     
	     ump_x2_1 =  x2->c * *right++;
	     ump_x2_1 += x2->g * *right++;
	     ump_x2_1 += x2->t * *right++;	
	     ump_x2_1 += x2->a;	 
	     
	     *uX2++ = ump_x2_1;
	     
	     ump_x2_2 =  x2->c * *right++;
	     ump_x2_2 += x2->g * *right++;
	     ump_x2_2 += x2->t * *right++;	       
	     ump_x2_2 += x2->a;	  
	     
	     *uX2++ = ump_x2_2;
	     
	     ump_x2_3 =  x2->c * *right++;
	     ump_x2_3 += x2->g * *right++;
	     ump_x2_3 += x2->t * *right++;		     	       
	     ump_x2_3 += x2->a;	    
	     
	     *uX2++ = ump_x2_3;	       
	   }

	 while ((! p->x)) 
	   {	     
	     if (! p->x) if (! getxnode(p)) return FALSE;	
	   }
	
	 x3  = (gammalikelivector*)p->x;
	 x3 = &x3[lower];
	 
	 for (i = lower; i < upper; i++) 
	   {		     
	     uX1 = &umpX1[16 * *tipX1++];
	     uX2 = &umpX2[16 * *tipX2++];

	     EV = &(tr->EV[model * 16]);				
	     x1px2 = *uX1++ * *uX2++;
	     x3->a0 = x1px2 *  *EV++;
	     x3->c0 = x1px2 *  *EV++;
	     x3->g0 = x1px2 * *EV++;
	     x3->t0 = x1px2 *  *EV++;
	     
	     x1px2 = *uX1++ * *uX2++;
	     x3->a0 += x1px2  *  *EV++;
	     x3->c0 += x1px2 *   *EV++;
	     x3->g0 += x1px2 *  *EV++;
	     x3->t0 += x1px2 *   *EV++;
	    
	     x1px2 = *uX1++ * *uX2++;
	     x3->a0 += x1px2 *  *EV++;
	     x3->c0 += x1px2*   *EV++;
	     x3->g0 += x1px2 *  *EV++;
	     x3->t0 += x1px2 *  *EV++;
	     
	     x1px2 = *uX1++ * *uX2++;
	     x3->a0 += x1px2 *   *EV++;
	     x3->c0 += x1px2 *   *EV++;
	     x3->g0 += x1px2 *   *EV++;
	     x3->t0 += x1px2 *   *EV++;

	     /* rate 1 */
	     
	     EV = &(tr->EV[model * 16]);
	     x1px2 = *uX1++ * *uX2++;/*ump_x1_0 * ump_x2_0;*/
	     x3->a1 = x1px2 *  *EV++;
	     x3->c1 = x1px2 *  *EV++;
	     x3->g1 = x1px2 * *EV++;
	     x3->t1 = x1px2 *  *EV++;
	     
	     x1px2 = *uX1++ * *uX2++;/*ump_x1_1 * ump_x2_1;*/
	     x3->a1 += x1px2  *  *EV++;
	     x3->c1 += x1px2 *   *EV++;
	     x3->g1 += x1px2 *  *EV++;
	     x3->t1 += x1px2 *   *EV++;
	     
	     x1px2 = *uX1++ * *uX2++;/*ump_x1_2 * ump_x2_2;*/
	     x3->a1 += x1px2 *  *EV++;
	     x3->c1 += x1px2*   *EV++;
	     x3->g1 += x1px2 *  *EV++;
	     x3->t1 += x1px2 *  *EV++;
	    
	     x1px2 = *uX1++ * *uX2++;/*ump_x1_3 * ump_x2_3;*/
	     x3->a1 += x1px2 *   *EV++;
	     x3->c1 += x1px2 *   *EV++;
	     x3->g1 += x1px2 *   *EV++;
	     x3->t1 += x1px2 *   *EV++;

	     /* rate 2 */
	     	     
	     EV = &(tr->EV[model * 16]);
	     x1px2 = *uX1++ * *uX2++;/*ump_x1_0 * ump_x2_0;*/
	     x3->a2 = x1px2 *  *EV++;
	     x3->c2 = x1px2 *  *EV++;
	     x3->g2 = x1px2 * *EV++;
	     x3->t2 = x1px2 *  *EV++;
	
	     x1px2 = *uX1++ * *uX2++;/*ump_x1_1 * ump_x2_1;*/
	     x3->a2 += x1px2  *  *EV++;
	     x3->c2 += x1px2 *   *EV++;
	     x3->g2 += x1px2 *  *EV++;
	     x3->t2 += x1px2 *   *EV++;
	    
	     x1px2 =  *uX1++ * *uX2++;/*ump_x1_2 * ump_x2_2;*/
	     x3->a2 += x1px2 *  *EV++;
	     x3->c2 += x1px2*   *EV++;
	     x3->g2 += x1px2 *  *EV++;
	     x3->t2 += x1px2 *  *EV++;
	     
	     x1px2 =  *uX1++ * *uX2++;/*ump_x1_3 * ump_x2_3;*/
	     x3->a2 += x1px2 *   *EV++;
	     x3->c2 += x1px2 *   *EV++;
	     x3->g2 += x1px2 *   *EV++;
	     x3->t2 += x1px2 *   *EV++;

	     /* rate 3 */	    

	     EV = &(tr->EV[model * 16]);
	     x1px2 =  *uX1++ * *uX2++;/*ump_x1_0 * ump_x2_0;*/
	     x3->a3 = x1px2 *  *EV++;
	     x3->c3 = x1px2 *  *EV++;
	     x3->g3 = x1px2 * *EV++;
	     x3->t3 = x1px2 *  *EV++;
	     
	     x1px2 =  *uX1++ * *uX2++;/*ump_x1_1 * ump_x2_1;*/
	     x3->a3 += x1px2  *  *EV++;
	     x3->c3 += x1px2 *   *EV++;
	     x3->g3 += x1px2 *  *EV++;
	     x3->t3 += x1px2 *   *EV++;
	    
	     x1px2 =  *uX1++ * *uX2++;/*ump_x1_2 * ump_x2_2;*/
	     x3->a3 += x1px2 *  *EV++;
	     x3->c3 += x1px2*   *EV++;
	     x3->g3 += x1px2 *  *EV++;
	     x3->t3 += x1px2 *  *EV++;
	     
	     x1px2 =  *uX1++ * *uX2++;/*ump_x1_3 * ump_x2_3;*/
	     x3->a3 += x1px2 *   *EV++;
	     x3->c3 += x1px2 *   *EV++;
	     x3->g3 += x1px2 *   *EV++;
	     x3->t3 += x1px2 *   *EV++;

	     /********************************************************************************/

	     x3->exp = 0;		  		    	     
	       
	     x3++;
      
	   }
         
	  free(left_start); 
	  free(right_start);
	 
	 return TRUE;
       }
     

     if(r->tip && !q->tip)
       {	 
	 char *tipX2 = &(r->tip[lower]);
	 likelivector *x2;
	 double *uX2, umpX2[256];	
	 uX2 = &umpX2[16];
	 
	 for(i = 1; i < 16; i++)
	   {	    	     	     
	     x2 = &(tr->gtrTip[16 * model + i]);	     
	     right = right_start;	   

	     ump_x2_0 =  x2->c * *right++;
	     ump_x2_0 += x2->g * *right++;
	     ump_x2_0 += x2->t * *right++;		
	     ump_x2_0 += x2->a;
	     
	     *uX2++ = ump_x2_0;
	     
	     ump_x2_1 =  x2->c * *right++;
	     ump_x2_1 += x2->g * *right++;
	     ump_x2_1 += x2->t * *right++;	       
	     ump_x2_1 += x2->a;	 
	     
	     *uX2++ = ump_x2_1;
	     
	     ump_x2_2 =  x2->c * *right++;
	     ump_x2_2 += x2->g * *right++;
	     ump_x2_2 += x2->t * *right++;	       
	     ump_x2_2 += x2->a;	  
	     
	     *uX2++ = ump_x2_2;
	     
	     ump_x2_3 =  x2->c * *right++;
	     ump_x2_3 += x2->g * *right++;
	     ump_x2_3 += x2->t * *right++;		     	       
	     ump_x2_3 += x2->a;	    
	     
	     *uX2++ = ump_x2_3;

	     ump_x2_0 =  x2->c * *right++;
	     ump_x2_0 += x2->g * *right++;
	     ump_x2_0 += x2->t * *right++;		
	     ump_x2_0 += x2->a;
	     
	     *uX2++ = ump_x2_0;
	     
	     ump_x2_1 =  x2->c * *right++;
	     ump_x2_1 += x2->g * *right++;
	     ump_x2_1 += x2->t * *right++;	       
	     ump_x2_1 += x2->a;	 
	     
	     *uX2++ = ump_x2_1;
	     
	     ump_x2_2 =  x2->c * *right++;
	     ump_x2_2 += x2->g * *right++;
	     ump_x2_2 += x2->t * *right++;	       
	     ump_x2_2 += x2->a;	  
	     
	     *uX2++ = ump_x2_2;
	     
	     ump_x2_3 =  x2->c * *right++;
	     ump_x2_3 += x2->g * *right++;
	     ump_x2_3 += x2->t * *right++;		     	       
	     ump_x2_3 += x2->a;	    
	     
	     *uX2++ = ump_x2_3;

	     ump_x2_0 =  x2->c * *right++;
	     ump_x2_0 += x2->g * *right++;
	     ump_x2_0 += x2->t * *right++;		
	     ump_x2_0 += x2->a;
	     
	     *uX2++ = ump_x2_0;
	     
	     ump_x2_1 =  x2->c * *right++;
	     ump_x2_1 += x2->g * *right++;
	     ump_x2_1 += x2->t * *right++;	       
	     ump_x2_1 += x2->a;	 
	     
	     *uX2++ = ump_x2_1;
	     
	     ump_x2_2 =  x2->c * *right++;
	     ump_x2_2 += x2->g * *right++;
	     ump_x2_2 += x2->t * *right++;	       
	     ump_x2_2 += x2->a;	  
	     
	     *uX2++ = ump_x2_2;
	     
	     ump_x2_3 =  x2->c * *right++;
	     ump_x2_3 += x2->g * *right++;
	     ump_x2_3 += x2->t * *right++;		     	       
	     ump_x2_3 += x2->a;	    
	     
	     *uX2++ = ump_x2_3;

	     ump_x2_0 =  x2->c * *right++;
	     ump_x2_0 += x2->g * *right++;
	     ump_x2_0 += x2->t * *right++;		
	     ump_x2_0 += x2->a;
	     
	     *uX2++ = ump_x2_0;
	     
	     ump_x2_1 =  x2->c * *right++;
	     ump_x2_1 += x2->g * *right++;
	     ump_x2_1 += x2->t * *right++;	       
	     ump_x2_1 += x2->a;	 
	     
	     *uX2++ = ump_x2_1;
	     
	     ump_x2_2 =  x2->c * *right++;
	     ump_x2_2 += x2->g * *right++;
	     ump_x2_2 += x2->t * *right++;	       
	     ump_x2_2 += x2->a;	  
	     
	     *uX2++ = ump_x2_2;
	     
	     ump_x2_3 =  x2->c * *right++;
	     ump_x2_3 += x2->g * *right++;
	     ump_x2_3 += x2->t * *right++;		     	       
	     ump_x2_3 += x2->a;	    
	     
	     *uX2++ = ump_x2_3;	    
	   }

	 while ((! p->x) || (! q->x)) 
	   {
	     if (! q->x) if (! newviewGTRGAMMAMULTPARTITION(tr, q, model)) return FALSE;	    
	     if (! p->x) if (! getxnode(p)) return FALSE;	
	   }
	

	 x1  = (gammalikelivector*)q->x;
	 x3  = (gammalikelivector*)p->x;

	 x1 = &x1[lower];
	 x3 = &x3[lower];
	 
 
	 for (i = lower; i < upper; i++) 
	   {	
	     left = left_start;	     
	   
	     uX2 = &umpX2[16 * *tipX2++];
	
	     /* Rate cat 0 */
	     EV = &(tr->EV[model * 16]);	
	     ump_x1_0 = x1->c0 * *left++;
	     ump_x1_0 += x1->g0 * *left++;
	     ump_x1_0 += x1->t0* *left++;	      		   
	     ump_x1_0 += x1->a0;
	
	     ump_x1_1 =  x1->c0 * *left++;
	     ump_x1_1 += x1->g0 * *left++;
	     ump_x1_1 += x1->t0 * *left++;	      		    
	     ump_x1_1 += x1->a0;
	    
	     ump_x1_2 = x1->c0 * *left++;
	     ump_x1_2 += x1->g0 * *left++;
	     ump_x1_2 += x1->t0 * *left++;	      		  
	     ump_x1_2 += x1->a0;
	     
	     ump_x1_3 =  x1->c0 * *left++;
	     ump_x1_3 += x1->g0 * *left++;
	     ump_x1_3 += x1->t0 * *left++;	      		  
	     ump_x1_3 += x1->a0;
			 	 	    	    	     	     	    	  		   	  	    	
	     x1px2 = ump_x1_0 * *uX2++;
	     x3->a0 = x1px2 *  *EV++;
	     x3->c0 = x1px2 *  *EV++;
	     x3->g0 = x1px2 * *EV++;
	     x3->t0 = x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_1 *  *uX2++;
	     x3->a0 += x1px2  *  *EV++;
	     x3->c0 += x1px2 *   *EV++;
	     x3->g0 += x1px2 *  *EV++;
	     x3->t0 += x1px2 *   *EV++;
	    
	     x1px2 = ump_x1_2 * *uX2++;
	     x3->a0 += x1px2 *  *EV++;
	     x3->c0 += x1px2*   *EV++;
	     x3->g0 += x1px2 *  *EV++;
	     x3->t0 += x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_3 * *uX2++;
	     x3->a0 += x1px2 *   *EV++;
	     x3->c0 += x1px2 *   *EV++;
	     x3->g0 += x1px2 *   *EV++;
	     x3->t0 += x1px2 *   *EV++;


	     /* rate 1 */
	     
	     	 EV = &(tr->EV[model * 16]);	
	     ump_x1_0 = x1->c1 * *left++;
	     ump_x1_0 += x1->g1 * *left++;
	     ump_x1_0 += x1->t1 * *left++;	      		  
	     ump_x1_0 += x1->a1;
	     
	     ump_x1_1 =  x1->c1 * *left++;
	     ump_x1_1 += x1->g1 * *left++;
	     ump_x1_1 += x1->t1 * *left++;	      		
	     ump_x1_1 += x1->a1;
	     
	     ump_x1_2 = x1->c1 * *left++;
	     ump_x1_2 += x1->g1 * *left++;
	     ump_x1_2 += x1->t1 * *left++;	      		   
	     ump_x1_2 += x1->a1;
	     
	     ump_x1_3 =  x1->c1 * *left++;
	     ump_x1_3 += x1->g1 * *left++;
	     ump_x1_3 += x1->t1 * *left++;	      		   
	     ump_x1_3 += x1->a1;
	     
	     
	      		   	  	    
	     
	     x1px2 = ump_x1_0 * *uX2++;
	     x3->a1 = x1px2 *  *EV++;
	     x3->c1 = x1px2 *  *EV++;
	     x3->g1 = x1px2 * *EV++;
	     x3->t1 = x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_1 * *uX2++;
	     x3->a1 += x1px2  *  *EV++;
	     x3->c1 += x1px2 *   *EV++;
	     x3->g1 += x1px2 *  *EV++;
	     x3->t1 += x1px2 *   *EV++;
	     
	     x1px2 = ump_x1_2 * *uX2++;;
	     x3->a1 += x1px2 *  *EV++;
	     x3->c1 += x1px2*   *EV++;
	     x3->g1 += x1px2 *  *EV++;
	     x3->t1 += x1px2 *  *EV++;
	    
	     x1px2 = ump_x1_3 *  *uX2++;;
	     x3->a1 += x1px2 *   *EV++;
	     x3->c1 += x1px2 *   *EV++;
	     x3->g1 += x1px2 *   *EV++;
	     x3->t1 += x1px2 *   *EV++;

	     /* rate 2 */
	     
		 EV = &(tr->EV[model * 16]);	
	     ump_x1_0 = x1->c2 * *left++;
	     ump_x1_0 += x1->g2 * *left++;
	     ump_x1_0 += x1->t2 * *left++;	      		    
	     ump_x1_0 += x1->a2;
	     
	     ump_x1_1 =  x1->c2 * *left++;
	     ump_x1_1 += x1->g2 * *left++;
	     ump_x1_1 += x1->t2 * *left++;	      		   
	     ump_x1_1 += x1->a2;
	     
	     ump_x1_2 = x1->c2 * *left++;
	     ump_x1_2 += x1->g2 * *left++;
	     ump_x1_2 += x1->t2 * *left++;	      		   
	     ump_x1_2 += x1->a2;
	    
	     ump_x1_3 =  x1->c2 * *left++;
	     ump_x1_3 += x1->g2 * *left++;
	     ump_x1_3 += x1->t2 * *left++;	      		  
	     ump_x1_3 += x1->a2;
			 	 	    	    	     	     	    
	     x1px2 = ump_x1_0 * *uX2++;
	     x3->a2 = x1px2 *  *EV++;
	     x3->c2 = x1px2 *  *EV++;
	     x3->g2 = x1px2 * *EV++;
	     x3->t2 = x1px2 *  *EV++;
	
	     x1px2 = ump_x1_1 * *uX2++;
	     x3->a2 += x1px2  *  *EV++;
	     x3->c2 += x1px2 *   *EV++;
	     x3->g2 += x1px2 *  *EV++;
	     x3->t2 += x1px2 *   *EV++;
	    
	     x1px2 = ump_x1_2 * *uX2++;;
	     x3->a2 += x1px2 *  *EV++;
	     x3->c2 += x1px2*   *EV++;
	     x3->g2 += x1px2 *  *EV++;
	     x3->t2 += x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_3 *  *uX2++;;
	     x3->a2 += x1px2 *   *EV++;
	     x3->c2 += x1px2 *   *EV++;
	     x3->g2 += x1px2 *   *EV++;
	     x3->t2 += x1px2 *   *EV++;

	     /* rate 3 */
		 EV = &(tr->EV[model * 16]);	
	     ump_x1_0 = x1->c3 * *left++;
	     ump_x1_0 += x1->g3 * *left++;
	     ump_x1_0 += x1->t3 * *left++;	      		  
	     ump_x1_0 += x1->a3;
	     
	     ump_x1_1 =  x1->c3 * *left++;
	     ump_x1_1 += x1->g3 * *left++;
	     ump_x1_1 += x1->t3 * *left++;	      		  
	     ump_x1_1 += x1->a3;
	    
	     ump_x1_2 = x1->c3 * *left++;
	     ump_x1_2 += x1->g3 * *left++;
	     ump_x1_2 += x1->t3 * *left++;	      		  
	     ump_x1_2 += x1->a3;
	    
	     ump_x1_3 =  x1->c3 * *left++;
	     ump_x1_3 += x1->g3 * *left++;
	     ump_x1_3 += x1->t3 * *left++;	      		    
	     ump_x1_3 += x1->a3;
		
	 	 	    	    	     	     
	     		   	  	    
	
	     x1px2 = ump_x1_0 * *uX2++;
	     x3->a3 = x1px2 *  *EV++;
	     x3->c3 = x1px2 *  *EV++;
	     x3->g3 = x1px2 * *EV++;
	     x3->t3 = x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_1 * *uX2++;
	     x3->a3 += x1px2  *  *EV++;
	     x3->c3 += x1px2 *   *EV++;
	     x3->g3 += x1px2 *  *EV++;
	     x3->t3 += x1px2 *   *EV++;
	    
	     x1px2 = ump_x1_2 * *uX2++;
	     x3->a3 += x1px2 *  *EV++;
	     x3->c3 += x1px2*   *EV++;
	     x3->g3 += x1px2 *  *EV++;
	     x3->t3 += x1px2 *  *EV++;
	     
	     x1px2 = ump_x1_3 *  *uX2++;
	     x3->a3 += x1px2 *   *EV++;
	     x3->c3 += x1px2 *   *EV++;
	     x3->g3 += x1px2 *   *EV++;
	     x3->t3 += x1px2 *   *EV++;

	     /********************************************************************************/


	     x3->exp = x1->exp;		  
	
	     if (ABS(x3->a0) < minlikelihood && ABS(x3->g0) < minlikelihood && ABS(x3->c0) < minlikelihood && ABS(x3->t0) < minlikelihood &&
		 ABS(x3->a1) < minlikelihood && ABS(x3->g1) < minlikelihood && ABS(x3->c1) < minlikelihood && ABS(x3->t1) < minlikelihood &&
		 ABS(x3->a2) < minlikelihood && ABS(x3->g2) < minlikelihood && ABS(x3->c2) < minlikelihood && ABS(x3->t2) < minlikelihood &&
		 ABS(x3->a3) < minlikelihood && ABS(x3->g3) < minlikelihood && ABS(x3->c3) < minlikelihood && ABS(x3->t3) < minlikelihood) 
	       {	     

		

		 x3->a0   *= twotothe256;
		 x3->c0   *= twotothe256;
		 x3->g0   *= twotothe256;
		 x3->t0   *= twotothe256;
		 
		 x3->a1   *= twotothe256;
		 x3->c1   *= twotothe256;
		 x3->g1   *= twotothe256;
		 x3->t1   *= twotothe256;
		 
		 x3->a2   *= twotothe256;
		 x3->c2   *= twotothe256;
		 x3->g2   *= twotothe256;
		 x3->t2   *= twotothe256;
		 
		 x3->a3   *= twotothe256;
		 x3->c3   *= twotothe256;
		 x3->g3   *= twotothe256;
		 x3->t3   *= twotothe256;
		 
		 x3->exp += 1;
	       }

	     x1++;  
	     x3++;
      
	   }

	  free(left_start); 
	  free(right_start);
         		 
	 return TRUE;
       }

     
     if(!r->tip && q->tip)
       {	 
	 char *tipX1 = &(q->tip[lower]);
	 likelivector *x1;
	 double *uX1, umpX1[256];	
	 uX1 = &umpX1[16];
	 
	 for(i = 1; i < 16; i++)
	   {	    	     	     
	     x1 = &(tr->gtrTip[16 * model + i]);	     
	     left = left_start;
	     
	     ump_x1_0 =  x1->c * *left++;
	     ump_x1_0 += x1->g * *left++;
	     ump_x1_0 += x1->t * *left++;	       
	     ump_x1_0 += x1->a;

	     *uX1++ = ump_x1_0;
	     
	     ump_x1_1 =  x1->c * *left++;
	     ump_x1_1 += x1->g * *left++;
	     ump_x1_1 += x1->t * *left++;		 
	     ump_x1_1 += x1->a;	 
	     
	     *uX1++ = ump_x1_1;
	     
	     ump_x1_2 =  x1->c * *left++;
	     ump_x1_2 += x1->g * *left++;
	     ump_x1_2 += x1->t * *left++;		
	     ump_x1_2 += x1->a;	  
	     
	     *uX1++ = ump_x1_2;
	     
	     ump_x1_3 =  x1->c * *left++;
	     ump_x1_3 += x1->g * *left++;
	     ump_x1_3 += x1->t * *left++;		     	      
	     ump_x1_3 += x1->a;	    
	     
	     *uX1++ = ump_x1_3;

	     ump_x1_0 =  x1->c * *left++;
	     ump_x1_0 += x1->g * *left++;
	     ump_x1_0 += x1->t * *left++;	       
	     ump_x1_0 += x1->a;

	     *uX1++ = ump_x1_0;
	     
	     ump_x1_1 =  x1->c * *left++;
	     ump_x1_1 += x1->g * *left++;
	     ump_x1_1 += x1->t * *left++;		 
	     ump_x1_1 += x1->a;	 
	     
	     *uX1++ = ump_x1_1;
	     
	     ump_x1_2 =  x1->c * *left++;
	     ump_x1_2 += x1->g * *left++;
	     ump_x1_2 += x1->t * *left++;		
	     ump_x1_2 += x1->a;	  
	     
	     *uX1++ = ump_x1_2;
	     
	     ump_x1_3 =  x1->c * *left++;
	     ump_x1_3 += x1->g * *left++;
	     ump_x1_3 += x1->t * *left++;		     	      
	     ump_x1_3 += x1->a;	    
	     
	     *uX1++ = ump_x1_3;

	     ump_x1_0 =  x1->c * *left++;
	     ump_x1_0 += x1->g * *left++;
	     ump_x1_0 += x1->t * *left++;	       
	     ump_x1_0 += x1->a;

	     *uX1++ = ump_x1_0;
	     
	     ump_x1_1 =  x1->c * *left++;
	     ump_x1_1 += x1->g * *left++;
	     ump_x1_1 += x1->t * *left++;		 
	     ump_x1_1 += x1->a;	 
	     
	     *uX1++ = ump_x1_1;
	     
	     ump_x1_2 =  x1->c * *left++;
	     ump_x1_2 += x1->g * *left++;
	     ump_x1_2 += x1->t * *left++;		
	     ump_x1_2 += x1->a;	  
	     
	     *uX1++ = ump_x1_2;
	     
	     ump_x1_3 =  x1->c * *left++;
	     ump_x1_3 += x1->g * *left++;
	     ump_x1_3 += x1->t * *left++;		     	      
	     ump_x1_3 += x1->a;	    
	     
	     *uX1++ = ump_x1_3;

	     ump_x1_0 =  x1->c * *left++;
	     ump_x1_0 += x1->g * *left++;
	     ump_x1_0 += x1->t * *left++;	       
	     ump_x1_0 += x1->a;

	     *uX1++ = ump_x1_0;
	     
	     ump_x1_1 =  x1->c * *left++;
	     ump_x1_1 += x1->g * *left++;
	     ump_x1_1 += x1->t * *left++;		 
	     ump_x1_1 += x1->a;	 
	     
	     *uX1++ = ump_x1_1;
	     
	     ump_x1_2 =  x1->c * *left++;
	     ump_x1_2 += x1->g * *left++;
	     ump_x1_2 += x1->t * *left++;		
	     ump_x1_2 += x1->a;	  
	     
	     *uX1++ = ump_x1_2;
	     
	     ump_x1_3 =  x1->c * *left++;
	     ump_x1_3 += x1->g * *left++;
	     ump_x1_3 += x1->t * *left++;		     	      
	     ump_x1_3 += x1->a;	    
	     
	     *uX1++ = ump_x1_3;
	    
	   }

	while ((! p->x) || (! r->x)) 
	  {	    
	    if (! r->x) if (! newviewGTRGAMMAMULTPARTITION(tr, r, model)) return FALSE;
	    if (! p->x) if (! getxnode(p)) return FALSE;	
	  }

	x2  = (gammalikelivector*)r->x; 
	x3  = (gammalikelivector*)p->x;

	x2 = &x2[lower];
	x3 = &x3[lower];


	 for (i = lower; i < upper; i++) 
	   {	
	     right = right_start;
	     uX1 = &umpX1[16 * *tipX1++];			    
						
	     EV = &(tr->EV[model * 16]);						 	 	    	    	     	     
	     ump_x2_0 = x2->c0 * *right++;
	     ump_x2_0 += x2->g0 * *right++;
	     ump_x2_0 += x2->t0 * *right++;		     	   
	     ump_x2_0 += x2->a0;
	
	     ump_x2_1 = x2->c0 * *right++;
	     ump_x2_1 += x2->g0 * *right++;
	     ump_x2_1 +=  x2->t0 * *right++;		     	    
	     ump_x2_1 += x2->a0;	 
	
	     ump_x2_2 = x2->c0 * *right++;
	     ump_x2_2 += x2->g0 * *right++;
	     ump_x2_2 +=  x2->t0 * *right++;		     	     
	     ump_x2_2 += x2->a0;	  
		   
	     ump_x2_3 = x2->c0 * *right++;
	     ump_x2_3 += x2->g0 * *right++;
	     ump_x2_3 += x2->t0 * *right++;		     	     
	     ump_x2_3 += x2->a0;	    	  		   	  	    
	
	     x1px2 = *uX1++ * ump_x2_0;
	     x3->a0 = x1px2 *  *EV++;
	     x3->c0 = x1px2 *  *EV++;
	     x3->g0 = x1px2 * *EV++;
	     x3->t0 = x1px2 *  *EV++;
	
	     x1px2 = *uX1++ * ump_x2_1;
	     x3->a0 += x1px2  *  *EV++;
	     x3->c0 += x1px2 *   *EV++;
	     x3->g0 += x1px2 *  *EV++;
	     x3->t0 += x1px2 *   *EV++;
	    
	     x1px2 = *uX1++ * ump_x2_2;
	     x3->a0 += x1px2 *  *EV++;
	     x3->c0 += x1px2*   *EV++;
	     x3->g0 += x1px2 *  *EV++;
	     x3->t0 += x1px2 *  *EV++;
	    
	     x1px2 = *uX1++ * ump_x2_3;
	     x3->a0 += x1px2 *   *EV++;
	     x3->c0 += x1px2 *   *EV++;
	     x3->g0 += x1px2 *   *EV++;
	     x3->t0 += x1px2 *   *EV++;

	     EV = &(tr->EV[model * 16]);		 	    	    	     	     
	     ump_x2_0 = x2->c1 * *right++;
	     ump_x2_0 += x2->g1 * *right++;
	     ump_x2_0 += x2->t1 * *right++;		     	  
	     ump_x2_0 += x2->a1;
	
	     ump_x2_1 = x2->c1 * *right++;
	     ump_x2_1 += x2->g1 * *right++;
	     ump_x2_1 +=  x2->t1 * *right++;		     	   
	     ump_x2_1 += x2->a1;	 
	
	     ump_x2_2 = x2->c1 * *right++;
	     ump_x2_2 += x2->g1 * *right++;
	     ump_x2_2 +=  x2->t1 * *right++;		     	  
	     ump_x2_2 += x2->a1;	  
	     
	     ump_x2_3 = x2->c1 * *right++;
	     ump_x2_3 += x2->g1 * *right++;
	     ump_x2_3 += x2->t1 * *right++;		     	 
	     ump_x2_3 += x2->a1;	    	  		   	  	    
	
	     x1px2 = *uX1++ * ump_x2_0;
	     x3->a1 = x1px2 *  *EV++;
	     x3->c1 = x1px2 *  *EV++;
	     x3->g1 = x1px2 * *EV++;
	     x3->t1 = x1px2 *  *EV++;
	     
	     x1px2 = *uX1++ * ump_x2_1;
	     x3->a1 += x1px2  *  *EV++;
	     x3->c1 += x1px2 *   *EV++;
	     x3->g1 += x1px2 *  *EV++;
	     x3->t1 += x1px2 *   *EV++;
	     
	     x1px2 = *uX1++ * ump_x2_2;
	     x3->a1 += x1px2 *  *EV++;
	     x3->c1 += x1px2*   *EV++;
	     x3->g1 += x1px2 *  *EV++;
	     x3->t1 += x1px2 *  *EV++;
	     
	     x1px2 = *uX1++ * ump_x2_3;
	     x3->a1 += x1px2 *   *EV++;
	     x3->c1 += x1px2 *   *EV++;
	     x3->g1 += x1px2 *   *EV++;
	     x3->t1 += x1px2 *   *EV++;

	     EV = &(tr->EV[model * 16]);	
	 	 	    	    	     	     
	     ump_x2_0 = x2->c2 * *right++;
	     ump_x2_0 += x2->g2 * *right++;
	     ump_x2_0 += x2->t2 * *right++;		     	   
	     ump_x2_0 += x2->a2;
	
	     ump_x2_1 = x2->c2 * *right++;
	     ump_x2_1 += x2->g2 * *right++;
	     ump_x2_1 +=  x2->t2 * *right++;		     	    
	     ump_x2_1 += x2->a2;	 
	     
	     ump_x2_2 = x2->c2 * *right++;
	     ump_x2_2 += x2->g2 * *right++;
	     ump_x2_2 +=  x2->t2 * *right++;		     	     
	     ump_x2_2 += x2->a2;	  
	     
	     ump_x2_3 = x2->c2 * *right++;
	     ump_x2_3 += x2->g2 * *right++;
	     ump_x2_3 += x2->t2 * *right++;		     	   
	     ump_x2_3 += x2->a2;	    	  		   	  	    
	
	     x1px2 = *uX1++ * ump_x2_0;
	     x3->a2 = x1px2 *  *EV++;
	     x3->c2 = x1px2 *  *EV++;
	     x3->g2 = x1px2 * *EV++;
	     x3->t2 = x1px2 *  *EV++;
	     
	     x1px2 = *uX1++ * ump_x2_1;
	     x3->a2 += x1px2  *  *EV++;
	     x3->c2 += x1px2 *   *EV++;
	     x3->g2 += x1px2 *  *EV++;
	     x3->t2 += x1px2 *   *EV++;
	     
	     x1px2 = *uX1++ * ump_x2_2;
	     x3->a2 += x1px2 *  *EV++;
	     x3->c2 += x1px2*   *EV++;
	     x3->g2 += x1px2 *  *EV++;
	     x3->t2 += x1px2 *  *EV++;
	    
	     x1px2 = *uX1++ * ump_x2_3;
	     x3->a2 += x1px2 *   *EV++;
	     x3->c2 += x1px2 *   *EV++;
	     x3->g2 += x1px2 *   *EV++;
	     x3->t2 += x1px2 *   *EV++;
	     	
	     EV = &(tr->EV[model * 16]);			 		 	    	    	     	     
	     ump_x2_0 = x2->c3 * *right++;
	     ump_x2_0 += x2->g3 * *right++;
	     ump_x2_0 += x2->t3 * *right++;		     	   
	     ump_x2_0 += x2->a3;
	
	     ump_x2_1 = x2->c3 * *right++;
	     ump_x2_1 += x2->g3 * *right++;
	     ump_x2_1 +=  x2->t3 * *right++;		     	    
	     ump_x2_1 += x2->a3;	 
	     
	     ump_x2_2 = x2->c3 * *right++;
	     ump_x2_2 += x2->g3 * *right++;
	     ump_x2_2 +=  x2->t3 * *right++;		     	     
	     ump_x2_2 += x2->a3;	  
	     
	     ump_x2_3 = x2->c3 * *right++;
	     ump_x2_3 += x2->g3 * *right++;
	     ump_x2_3 += x2->t3 * *right++;		     	    
	     ump_x2_3 += x2->a3;	    	  		   	  	    
	     
	     x1px2 = *uX1++ * ump_x2_0;
	     x3->a3 = x1px2 *  *EV++;
	     x3->c3 = x1px2 *  *EV++;
	     x3->g3 = x1px2 * *EV++;
	     x3->t3 = x1px2 *  *EV++;
	     
	     x1px2 = *uX1++ * ump_x2_1;
	     x3->a3 += x1px2  *  *EV++;
	     x3->c3 += x1px2 *   *EV++;
	     x3->g3 += x1px2 *  *EV++;
	     x3->t3 += x1px2 *   *EV++;
	     
	     x1px2 = *uX1++ * ump_x2_2;
	     x3->a3 += x1px2 *  *EV++;
	     x3->c3 += x1px2*   *EV++;
	     x3->g3 += x1px2 *  *EV++;
	     x3->t3 += x1px2 *  *EV++;
	     
	     x1px2 = *uX1++ * ump_x2_3;
	     x3->a3 += x1px2 *   *EV++;
	     x3->c3 += x1px2 *   *EV++;
	     x3->g3 += x1px2 *   *EV++;
	     x3->t3 += x1px2 *   *EV++;
	     
	

	     x3->exp = x2->exp;		  
	
	if (ABS(x3->a0) < minlikelihood && ABS(x3->g0) < minlikelihood && ABS(x3->c0) < minlikelihood && ABS(x3->t0) < minlikelihood &&
	    ABS(x3->a1) < minlikelihood && ABS(x3->g1) < minlikelihood && ABS(x3->c1) < minlikelihood && ABS(x3->t1) < minlikelihood &&
	    ABS(x3->a2) < minlikelihood && ABS(x3->g2) < minlikelihood && ABS(x3->c2) < minlikelihood && ABS(x3->t2) < minlikelihood &&
	    ABS(x3->a3) < minlikelihood && ABS(x3->g3) < minlikelihood && ABS(x3->c3) < minlikelihood && ABS(x3->t3) < minlikelihood) 
	  {	     

	    
	 

	    x3->a0   *= twotothe256;
	    x3->c0   *= twotothe256;
	    x3->g0   *= twotothe256;
	    x3->t0   *= twotothe256;

	    x3->a1   *= twotothe256;
	    x3->c1   *= twotothe256;
	    x3->g1   *= twotothe256;
	    x3->t1   *= twotothe256;

	    x3->a2   *= twotothe256;
	    x3->c2   *= twotothe256;
	    x3->g2   *= twotothe256;
	    x3->t2   *= twotothe256;

	    x3->a3   *= twotothe256;
	    x3->c3   *= twotothe256;
	    x3->g3   *= twotothe256;
	    x3->t3   *= twotothe256;
	   
	    x3->exp += 1;
	  }
	 	
      
	x2++;
	x3++;
      
      }
	
	 free(left_start); 
	 free(right_start);
         		 
	 return TRUE;
	   }           
    
     while ((! p->x) || (! q->x) || (! r->x)) {
       if (! q->x) if (! newviewGTRGAMMAMULTPARTITION(tr, q, model)) return FALSE;
       if (! r->x) if (! newviewGTRGAMMAMULTPARTITION(tr, r, model)) return FALSE;
       if (! p->x) if (! getxnode(p)) return FALSE;	
     }
       
    

     x1  = (gammalikelivector*)q->x;
     x2  = (gammalikelivector*)r->x;
     x3  = (gammalikelivector*)p->x;   

     x1 = &x1[lower];
     x2 = &x2[lower];
     x3 = &x3[lower];

    for (i = lower; i < upper; i++) 
      {	
	left = left_start;
	right = right_start;
	
	/* Rate cat 0 */
	EV = &(tr->EV[model * 16]);	
	ump_x1_0 = x1->c0  * *left++;
	ump_x1_0 += x1->g0 * *left++;
	ump_x1_0 += x1->t0 * *left++;	      	      
	ump_x1_0 += x1->a0;
	
	ump_x1_1 =  x1->c0 * *left++;
	ump_x1_1 += x1->g0 * *left++;
	ump_x1_1 += x1->t0 * *left++;	      	       
	ump_x1_1 += x1->a0;
	    
	ump_x1_2 = x1->c0  * *left++;
	ump_x1_2 += x1->g0 * *left++;
	ump_x1_2 += x1->t0 * *left++;	      		
	ump_x1_2 += x1->a0;
	    
	ump_x1_3 =  x1->c0 * *left++;
	ump_x1_3 += x1->g0 * *left++;
	ump_x1_3 += x1->t0 * *left++;	      	      
	ump_x1_3 += x1->a0;
		
	 	 	    	    	     	     
	ump_x2_0 =  x2->c0  * *right++;
	ump_x2_0 += x2->g0 * *right++;
	ump_x2_0 += x2->t0 * *right++;		     	
	ump_x2_0 += x2->a0;
	
	ump_x2_1 =  x2->c0 * *right++;
	ump_x2_1 += x2->g0 * *right++;
	ump_x2_1 += x2->t0 * *right++;		     	
	ump_x2_1 += x2->a0;	 
	
	ump_x2_2 =  x2->c0 * *right++;
	ump_x2_2 += x2->g0 * *right++;
	ump_x2_2 += x2->t0 * *right++;		     	
	ump_x2_2 += x2->a0;	  
		   
	ump_x2_3 = x2->c0 * *right++;
	ump_x2_3 += x2->g0 * *right++;
	ump_x2_3 += x2->t0 * *right++;		     	
	ump_x2_3 += x2->a0;	    	  		   	  	    
	
	x1px2 = ump_x1_0 * ump_x2_0;
	x3->a0 = x1px2 *  *EV++;
	x3->c0 = x1px2 *  *EV++;
	x3->g0 = x1px2 * *EV++;
	x3->t0 = x1px2 *  *EV++;
	
	x1px2 = ump_x1_1 * ump_x2_1;
	x3->a0 += x1px2  *  *EV++;
	x3->c0 += x1px2 *   *EV++;
	x3->g0 += x1px2 *  *EV++;
	x3->t0 += x1px2 *   *EV++;
	    
	x1px2 = ump_x1_2 * ump_x2_2;
	x3->a0 += x1px2 *  *EV++;
	x3->c0 += x1px2*   *EV++;
	x3->g0 += x1px2 *  *EV++;
	x3->t0 += x1px2 *  *EV++;
	    
	x1px2 = ump_x1_3 * ump_x2_3;
	x3->a0 += x1px2 *   *EV++;
	x3->c0 += x1px2 *   *EV++;
	x3->g0 += x1px2 *   *EV++;
	x3->t0 += x1px2 *   *EV++;

	/* rate 1 */

	EV = &(tr->EV[model * 16]);	
	ump_x1_0 = x1->c1 * *left++;
	ump_x1_0 += x1->g1 * *left++;
	ump_x1_0 += x1->t1* *left++;	      	     
	ump_x1_0 += x1->a1;
	
	ump_x1_1 =  x1->c1 * *left++;
	ump_x1_1 += x1->g1 * *left++;
	ump_x1_1 += x1->t1* *left++;	      	      
	ump_x1_1 += x1->a1;
	    
	ump_x1_2 = x1->c1 * *left++;
	ump_x1_2 += x1->g1 * *left++;
	ump_x1_2 += x1->t1 * *left++;	      	      
	ump_x1_2 += x1->a1;
	    
	ump_x1_3 =  x1->c1 * *left++;
	ump_x1_3 += x1->g1 * *left++;
	ump_x1_3 += x1->t1 * *left++;	      	
	ump_x1_3 += x1->a1;
			 	 	    	    	     	     
	ump_x2_0 = x2->c1 * *right++;
	ump_x2_0 += x2->g1 * *right++;
	ump_x2_0 += x2->t1 * *right++;		           
	ump_x2_0 += x2->a1;
	
	ump_x2_1 = x2->c1 * *right++;
	ump_x2_1 += x2->g1 * *right++;
	ump_x2_1 +=  x2->t1 * *right++;		           
	ump_x2_1 += x2->a1;	 
	
	ump_x2_2 = x2->c1 * *right++;
	ump_x2_2 += x2->g1 * *right++;
	ump_x2_2 +=  x2->t1 * *right++;		           
	ump_x2_2 += x2->a1;	  
		   
	ump_x2_3 = x2->c1 * *right++;
	ump_x2_3 += x2->g1 * *right++;
	ump_x2_3 += x2->t1 * *right++;		            
	ump_x2_3 += x2->a1;	    	  		   	  	    
	
	x1px2 = ump_x1_0 * ump_x2_0;
	x3->a1 = x1px2 *  *EV++;
	x3->c1 = x1px2 *  *EV++;
	x3->g1 = x1px2 * *EV++;
	x3->t1 = x1px2 *  *EV++;
	
	x1px2 = ump_x1_1 * ump_x2_1;
	x3->a1 += x1px2  *  *EV++;
	x3->c1 += x1px2 *   *EV++;
	x3->g1 += x1px2 *  *EV++;
	x3->t1 += x1px2 *   *EV++;
	    
	x1px2 = ump_x1_2 * ump_x2_2;
	x3->a1 += x1px2 *  *EV++;
	x3->c1 += x1px2*   *EV++;
	x3->g1 += x1px2 *  *EV++;
	x3->t1 += x1px2 *  *EV++;
	    
	x1px2 = ump_x1_3 * ump_x2_3;
	x3->a1 += x1px2 *   *EV++;
	x3->c1 += x1px2 *   *EV++;
	x3->g1 += x1px2 *   *EV++;
	x3->t1 += x1px2 *   *EV++;

	/* rate 2 */

	EV = &(tr->EV[model * 16]);	
	ump_x1_0 = x1->c2 * *left++;
	ump_x1_0 += x1->g2 * *left++;
	ump_x1_0 += x1->t2 * *left++;	      	     
	ump_x1_0 += x1->a2;
	
	ump_x1_1 =  x1->c2 * *left++;
	ump_x1_1 += x1->g2 * *left++;
	ump_x1_1 += x1->t2* *left++;	      	      
	ump_x1_1 += x1->a2;
	    
	ump_x1_2 = x1->c2 * *left++;
	ump_x1_2 += x1->g2 * *left++;
	ump_x1_2 += x1->t2 * *left++;	      	     
	ump_x1_2 += x1->a2;
	    
	ump_x1_3 =  x1->c2 * *left++;
	ump_x1_3 += x1->g2 * *left++;
	ump_x1_3 += x1->t2 * *left++;	      	     
	ump_x1_3 += x1->a2;
			 	 	    	    	     	     
	ump_x2_0 = x2->c2 * *right++;
	ump_x2_0 += x2->g2 * *right++;
	ump_x2_0 += x2->t2 * *right++;		     
	ump_x2_0 += x2->a2;
	
	ump_x2_1 = x2->c2 * *right++;
	ump_x2_1 += x2->g2 * *right++;
	ump_x2_1 +=  x2->t2 * *right++;		         
	ump_x2_1 += x2->a2;	 
	
	ump_x2_2 = x2->c2 * *right++;
	ump_x2_2 += x2->g2 * *right++;
	ump_x2_2 +=  x2->t2 * *right++;		            
	ump_x2_2 += x2->a2;	  
		   
	ump_x2_3 = x2->c2 * *right++;
	ump_x2_3 += x2->g2 * *right++;
	ump_x2_3 += x2->t2 * *right++;		          
	ump_x2_3 += x2->a2;	    	  		   	  	    
	
	x1px2 = ump_x1_0 * ump_x2_0;
	x3->a2 = x1px2 * *EV++;
	x3->c2 = x1px2 * *EV++;
	x3->g2 = x1px2 * *EV++;
	x3->t2 = x1px2 * *EV++;
	
	x1px2 = ump_x1_1 * ump_x2_1;
	x3->a2 += x1px2 * *EV++;
	x3->c2 += x1px2 * *EV++;
	x3->g2 += x1px2 * *EV++;
	x3->t2 += x1px2 * *EV++;
	    
	x1px2 = ump_x1_2 * ump_x2_2;
	x3->a2 += x1px2 * *EV++;
	x3->c2 += x1px2 * *EV++;
	x3->g2 += x1px2 * *EV++;
	x3->t2 += x1px2 * *EV++;
	    
	x1px2 = ump_x1_3 * ump_x2_3;
	x3->a2 += x1px2 * *EV++;
	x3->c2 += x1px2 * *EV++;
	x3->g2 += x1px2 * *EV++;
	x3->t2 += x1px2 * *EV++;

	/* rate 3 */
	EV = &(tr->EV[model * 16]);	
	ump_x1_0 =  x1->c3 * *left++;
	ump_x1_0 += x1->g3 * *left++;
	ump_x1_0 += x1->t3 * *left++;	      	     
	ump_x1_0 += x1->a3;
	
	ump_x1_1 =  x1->c3 * *left++;
	ump_x1_1 += x1->g3 * *left++;
	ump_x1_1 += x1->t3 * *left++;	      	 
	ump_x1_1 += x1->a3;
	    
	ump_x1_2 = x1->c3 * *left++;
	ump_x1_2 += x1->g3 * *left++;
	ump_x1_2 += x1->t3 * *left++;	      	      
	ump_x1_2 += x1->a3;
	    
	ump_x1_3 =  x1->c3 * *left++;
	ump_x1_3 += x1->g3 * *left++;
	ump_x1_3 += x1->t3 * *left++;	      	     
	ump_x1_3 += x1->a3;
		
	 	 	    	    	     	     
	ump_x2_0 = x2->c3 * *right++;
	ump_x2_0 += x2->g3 * *right++;
	ump_x2_0 += x2->t3 * *right++;		         
	ump_x2_0 += x2->a3;
	
	ump_x2_1 = x2->c3 * *right++;
	ump_x2_1 += x2->g3 * *right++;
	ump_x2_1 +=  x2->t3 * *right++;		           
	ump_x2_1 += x2->a3;	 
	
	ump_x2_2 = x2->c3 * *right++;
	ump_x2_2 += x2->g3 * *right++;
	ump_x2_2 +=  x2->t3 * *right++;		         
	ump_x2_2 += x2->a3;	  
		   
	ump_x2_3 = x2->c3 * *right++;
	ump_x2_3 += x2->g3 * *right++;
	ump_x2_3 += x2->t3 * *right++;		            
	ump_x2_3 += x2->a3;	    	  		   	  	    
	
	x1px2 = ump_x1_0 * ump_x2_0;
	x3->a3 = x1px2 * *EV++;
	x3->c3 = x1px2 * *EV++;
	x3->g3 = x1px2 * *EV++;
	x3->t3 = x1px2 * *EV++;
	
	x1px2 = ump_x1_1 * ump_x2_1;
	x3->a3 += x1px2 * *EV++;
	x3->c3 += x1px2 * *EV++;
	x3->g3 += x1px2 * *EV++;
	x3->t3 += x1px2 * *EV++;
	    
	x1px2 = ump_x1_2 * ump_x2_2;
	x3->a3 += x1px2 * *EV++;
	x3->c3 += x1px2 * *EV++;
	x3->g3 += x1px2 * *EV++;
	x3->t3 += x1px2 * *EV++;
	    
	x1px2 = ump_x1_3 * ump_x2_3;
	x3->a3 += x1px2 * *EV++;
	x3->c3 += x1px2 * *EV++;
	x3->g3 += x1px2 * *EV++;
	x3->t3 += x1px2 * *EV++;

	/********************************************************************************/

	x3->exp = x1->exp + x2->exp;		  
	
	if (ABS(x3->a0) < minlikelihood && ABS(x3->g0) < minlikelihood && ABS(x3->c0) < minlikelihood && ABS(x3->t0) < minlikelihood &&
	    ABS(x3->a1) < minlikelihood && ABS(x3->g1) < minlikelihood && ABS(x3->c1) < minlikelihood && ABS(x3->t1) < minlikelihood &&
	    ABS(x3->a2) < minlikelihood && ABS(x3->g2) < minlikelihood && ABS(x3->c2) < minlikelihood && ABS(x3->t2) < minlikelihood &&
	    ABS(x3->a3) < minlikelihood && ABS(x3->g3) < minlikelihood && ABS(x3->c3) < minlikelihood && ABS(x3->t3) < minlikelihood) 
	  {	     
	    x3->a0   *= twotothe256;
	    x3->c0   *= twotothe256;
	    x3->g0   *= twotothe256;
	    x3->t0   *= twotothe256;

	    x3->a1   *= twotothe256;
	    x3->c1   *= twotothe256;
	    x3->g1   *= twotothe256;
	    x3->t1   *= twotothe256;

	    x3->a2   *= twotothe256;
	    x3->c2   *= twotothe256;
	    x3->g2   *= twotothe256;
	    x3->t2   *= twotothe256;

	    x3->a3   *= twotothe256;
	    x3->c3   *= twotothe256;
	    x3->g3   *= twotothe256;
	    x3->t3   *= twotothe256;
	   
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







double evaluateGTRGAMMAMULTPARTITION (tree *tr, nodeptr p, int model)
  { /* evaluate */
    double   sum, z, lz, term, ki, term2;    
    nodeptr  q;
    int     i, j;
    int     *wptr;    
    double  *diagptable, *diagptable_start;
    double *EIGN;
    gammalikelivector   *x1, *x2;
    int lower, upper;

    lower = tr->modelIndices[model][0];
    upper = tr->modelIndices[model][1];

   
    wptr = &(tr->cdta->aliaswgt[lower]);

    q = p->back;
        
    z = p->z;
   
    if (z < zmin) z = zmin;
    lz = log(z);
       
    diagptable = diagptable_start = (double *)malloc(sizeof(double) * 12);
    
    EIGN = &(tr->EIGN[model * 3]);

    for(i = 0; i < 4; i++)
      {
	ki = tr->gammaRates[model * 4 + i];	 

	*diagptable++ = exp (EIGN[0] * ki * lz);
	*diagptable++ = exp (EIGN[1] * ki * lz);
	*diagptable++ = exp (EIGN[2] * ki * lz);
      }
	    
    sum = 0.0;
  
    if(p->tip && !q->tip)
      {	
	char *tipX1 = &p->tip[lower];
	likelivector *x1;
	
	 while ((! q->x)) 
	   {	     
	     if (! (q->x)) if (! newviewGTRGAMMAMULTPARTITION(tr, q, model)) return badEval;
	   }
	 
	 x2  = (gammalikelivector*)q->x;

	 x2 = &x2[lower];

	for (i = lower; i < upper; i++) 
	  {
	    x1 = &(tr->gtrTip[16 * model + *tipX1++]);
	    diagptable = diagptable_start;
	    
	    /* cat 0 */
	    
	    term =  x1->a * x2->a0;
	    term += x1->c * x2->c0 * *diagptable++;
	    term += x1->g * x2->g0 * *diagptable++;
	    term += x1->t * x2->t0 * *diagptable++;     
	    
	    /* cat 1 */
	    
	    term += x1->a * x2->a1;
	    term += x1->c * x2->c1 * *diagptable++;
	    term += x1->g * x2->g1 * *diagptable++;
	    term += x1->t * x2->t1 * *diagptable++;     
       
	    /* cat 2 */

	    term += x1->a * x2->a2;
	    term += x1->c * x2->c2 * *diagptable++;
	    term += x1->g * x2->g2 * *diagptable++;
	    term += x1->t * x2->t2 * *diagptable++;     
	
	    /* cat 3 */
	
	    term += x1->a * x2->a3;
	    term += x1->c * x2->c3 * *diagptable++;
	    term += x1->g * x2->g3 * *diagptable++;
	    term += x1->t * x2->t3 * *diagptable++;     

	    term = log(0.25 * term) + (x2->exp)*log(minlikelihood);
	    sum += *wptr++ * term;		  	   
	    x2++;
	  }
    
	 free(diagptable_start); 
          
	 return  sum;
      }

    if(!p->tip && q->tip)
      {
	char *tipX2 = &(q->tip[lower]);
	likelivector *x2;

	while ((! p->x)) 
	  {
	    if (! (p->x)) if (! newviewGTRGAMMAMULTPARTITION(tr, p, model)) return badEval;	  
	  }

	x1  = (gammalikelivector*)p->x;
	x1 = &x1[lower];
          
	for (i = lower; i < upper; i++) 
	  {
	    x2 = &(tr->gtrTip[16 * model + *tipX2++]);
	    diagptable = diagptable_start;
	    
	    /* cat 0 */
	    
	    term =  x1->a0 * x2->a;
	    term += x1->c0 * x2->c * *diagptable++;
	    term += x1->g0 * x2->g * *diagptable++;
	    term += x1->t0 * x2->t * *diagptable++;     
	    
	    /* cat 1 */
	    
	    term += x1->a1 * x2->a;
	    term += x1->c1 * x2->c * *diagptable++;
	    term += x1->g1 * x2->g * *diagptable++;
	    term += x1->t1 * x2->t * *diagptable++;     
      
	    /* cat 2 */
	    
	    term += x1->a2 * x2->a;
	    term += x1->c2 * x2->c * *diagptable++;
	    term += x1->g2 * x2->g * *diagptable++;
	    term += x1->t2 * x2->t * *diagptable++;     
	    	
	    /* cat 3 */
	    
	    term +=  x1->a3 * x2->a;
	    term += x1->c3 * x2->c * *diagptable++;
	    term += x1->g3 * x2->g * *diagptable++;
	    term += x1->t3 * x2->t * *diagptable++;     
	    
	    term = log(0.25 * term) + (x1->exp)*log(minlikelihood);
	    sum += *wptr++ * term;		  
	    x1++;	  
	  }
    
	free(diagptable_start); 
	
	return  sum;
      }

     while ((! p->x) || (! q->x)) 
       {
	 if (! (p->x)) if (! newviewGTRGAMMAMULTPARTITION(tr, p, model)) return badEval;
	 if (! (q->x)) if (! newviewGTRGAMMAMULTPARTITION(tr, q, model)) return badEval;
       }

     x1 = (gammalikelivector*)p->x;
     x2 = (gammalikelivector*)q->x; 

     x1 = &x1[lower];
     x2 = &x2[lower];

    for (i = lower; i < upper; i++) 
      {	  	 	  
	diagptable = diagptable_start;

	/* cat 0 */

	term =  x1->a0 * x2->a0;
	term += x1->c0 * x2->c0 * *diagptable++;
	term += x1->g0 * x2->g0 * *diagptable++;
	term += x1->t0 * x2->t0 * *diagptable++;     

	/* cat 1 */

	term += x1->a1 * x2->a1;
	term += x1->c1 * x2->c1 * *diagptable++;
	term += x1->g1 * x2->g1 * *diagptable++;
	term += x1->t1 * x2->t1 * *diagptable++;     

       

	/* cat 2 */

	term += x1->a2 * x2->a2;
	term += x1->c2 * x2->c2 * *diagptable++;
	term += x1->g2 * x2->g2 * *diagptable++;
	term += x1->t2 * x2->t2 * *diagptable++;     
	
	/* cat 3 */
	
	term +=  x1->a3 * x2->a3;
	term += x1->c3 * x2->c3 * *diagptable++;
	term += x1->g3 * x2->g3 * *diagptable++;
	term += x1->t3 * x2->t3 * *diagptable++;     

	term = log(0.25 * term) + (x1->exp + x2->exp)*log(minlikelihood);
	sum += *wptr++ * term;		  
	x1++;
	x2++;
      }
    
      
    free(diagptable_start); 
      
    return  sum;
  } /* evaluate */


