/*  RAxML-HPC, a program for sequential and parallel estimation of phylogenetic trees 
 *  Copyright March 2006 by Alexandros Stamatakis
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
 *  stamatak@ics.forth.gr
 *
 *  When publishing work that is based on the results from RAxML-VI-HPC please cite:
 *  
 *  Alexandros Stamatakis: "An Efficient Program for phylogenetic Inference Using Simulated Annealing". 
 *  Proceedings of IPDPS2005,  Denver, Colorado, April 2005.
 *  
 *  AND
 *
 *  Alexandros Stamatakis:"RAxML-VI-HPC: maximum likelihood-based phylogenetic analyses with thousands of taxa and mixed models". 
 *  Bioinformatics 2006; doi: 10.1093/bioinformatics/btl446
 */


#ifdef WIN32
   #include <sys/timeb.h>
   #include "getopt.h"
   #include <direct.h>
#else
   #include <sys/times.h>
   #include <sys/types.h>
   #include <sys/time.h>
   #include <unistd.h>  
#endif

#include <limits.h>
#include <math.h>
#include <time.h> 
#include <stdlib.h>
#include <stdio.h>
#include <ctype.h>
#include <string.h>
#include "axml.h"

extern FILE *INFILE;
extern char run_id[128];
extern char workdir[1024];
extern char bootStrapFile[1024];
extern char tree_file[1024];
extern char infoFileName[1024];

int countTips(nodeptr p)
{
  if(p->tip)
    return 1;
  {
    nodeptr q;
    int tips = 0;

    q = p->next;
    while(q != p)
      { 
	tips += countTips(q->back);
	q = q->next;
      } 
    
    return tips;
  }
}

void getTips(nodeptr p, int *c, int *entries)
{
  nodeptr q;   

  if(p->tip)
    {
      entries[*c] = p->number;
      *c = *c + 1;
      return;
    } 
    
  q = p->next;
  while(q != p)
    { 
      getTips(q->back, c, entries);
      q = q->next;
    }   

  return;
}

static int intCompare(const void *p1, const void *p2)
{
 int *rc1 = (int *)p1;
 int *rc2 = (int *)p2;

 int i = *rc1;
 int j = *rc2;
  
  if (i > j)
    return (1);
  if (i < j)
    return (-1);
  return (0);
}



void makeBipartitionsRec(nodeptr p, bList *blThis, int *bCountThis)
{  
  if(p->tip)
    return;
  {
    nodeptr q;
    int l, r, i;
    int c;   
    
    if(!p->back->tip)
      {       
	l = countTips(p);
	r = countTips(p->back);
	c = 0;
	     
	if(l < r)
	  {
	    blThis[*bCountThis].entries = (int *)malloc(l * sizeof(int));	   
	    getTips(p, &c, blThis[*bCountThis].entries);
	  }
	else
	  {
	    blThis[*bCountThis].entries = (int *)malloc(r * sizeof(int));
	    getTips(p->back, &c, blThis[*bCountThis].entries);
	  }
	
	blThis[*bCountThis].length = c;      

	qsort((blThis[*bCountThis].entries), c, sizeof(int), intCompare);
	blThis[*bCountThis].p = p;
	blThis[*bCountThis].pNum = p->number;
	blThis[*bCountThis].qNum = p->back->number;
	*bCountThis = *bCountThis + 1;
      }
  
    q = p->next;
    while(q != p)
      {
	makeBipartitionsRec(q->back, blThis, bCountThis);
	q = q->next;
      } 
    return;
  }
}


static int bListCompare(const void *p1, const void *p2)
{
 bList *rc1 = (bList *)p1;
 bList *rc2 = (bList *)p2;

 int i = rc1->length;
 int j = rc2->length;
  
  if (i > j)
    return (1);
  if (i < j)
    return (-1);
  return (0);
}



bList *bipartitionList(tree *tr, boolean initialList,  int *bCountThis)
{
  int i, j, n = tr->mxtips - 3;
  bList *blThis;

  blThis = (bList *)malloc(sizeof(bList) * n);       
  *bCountThis = 0;

  for(i = 0; i < n; i++)
    {        
      blThis[i].p = (nodeptr) NULL;
      blThis[i].support = 0;
      blThis[i].length = 0;
      blThis[i].entries = (int *)NULL;
      blThis[i].pNum = 0;
      blThis[i].qNum = 0;
    }
      
  makeBipartitionsRec(tr->nodep[1]->back, blThis, bCountThis);
            
  qsort(&(blThis[0]), *bCountThis, sizeof(bList), bListCompare);
      
  
  return blThis;
  /*for(i = 0; i < n; i++)	                
    free(blThis[i].entries);	          
    free(blThis);*/
           
}


void printBlist(bList *blThis, int n)
{
  int i, j;

  for(i = 0; i < n; i++)	    
    {
      printf("%d %d %d: (", i, blThis[i].length, blThis[i].support);
      for(j = 0; j < blThis[i].length; j++)
	{
	  if(j == (blThis[i].length - 1))
	    printf("%d)\n", blThis[i].entries[j]);
	  else
	     printf("%d, ", blThis[i].entries[j]);
	}
    }    
}

void freeBList(bList *blThis, int n)
{
  int i;
 
  for(i = 0; i < n; i++)	             
    {    
      free(blThis[i].entries);	      
    }
  free(blThis);
}

void updateReferenceList(bList *referenceList, int referenceListLength, bList *currentList, int currentListLength)
{
  int i, j, k, length;
  boolean found;
  int f = 0;

  /*  printf("%d %d\n", currentListLength, referenceListLength);*/

  for(i = 0; i < currentListLength; i++)
    {
      j = 0;
      length = currentList[i].length;
      while(length > referenceList[j].length)
	j++;

      /*printf("%d at %d\n", length, j);*/

      while(j < referenceListLength && length == referenceList[j].length)
	{	
	  k = 0;
	  found = TRUE;
	  while(k < length && found)
	    {
	      if(currentList[i].entries[k] != referenceList[j].entries[k])
		found = FALSE;
	      k++;
	    }

	  if(found)
	    {
	      referenceList[j].support = referenceList[j].support + 1;	      	      
	      f++;
	      /*goto foundThisOne;*/
	      break;
	    }
	  j++;
	}
      /*foundThisOne:	         */
    }
  /*printf("FOUND %d\n", f);*/
}


void calcBipartitions(tree *tr, analdef *adef)
{
  bList *ML_Tree = (bList *)NULL, *BOOT_Tree = (bList *)NULL;
  int countML_Tree = 0, countBOOT_Tree = 0, numberOfTrees = 0, i;
  char ch;
  nodeptr p;
  FILE *infoFile;

  INFILE = fopen(tree_file, "r");
  treeReadTopologyOnly(INFILE, tr, adef, FALSE);
  fclose(INFILE);

  ML_Tree = bipartitionList(tr, TRUE, &countML_Tree);
  
  INFILE = fopen(bootStrapFile, "r");
  
  while((ch = getc(INFILE)) != EOF)
    {
      if(ch == ';')
	numberOfTrees++;
    }	 
  rewind(INFILE);

  infoFile = fopen(infoFileName, "a");
  printf("Found %d trees in File %s\n", numberOfTrees, bootStrapFile);
  fprintf(infoFile, "Found %d trees in File %s\n", numberOfTrees, bootStrapFile);
  fclose(infoFile);

  for(i = 0; i < numberOfTrees; i++)
    {
      /*printf("Tree %d\n", i);*/
      treeReadTopologyOnly(INFILE, tr, adef, FALSE);
      BOOT_Tree = bipartitionList(tr, FALSE, &countBOOT_Tree);
      updateReferenceList(ML_Tree, countML_Tree, BOOT_Tree, countBOOT_Tree);  
      freeBList(BOOT_Tree, countBOOT_Tree);
    }

  fclose(INFILE);

  /*printBlist(ML_Tree, countML_Tree);*/

 

  INFILE = fopen(tree_file, "r");
  treeReadTopologyOnly(INFILE, tr, adef, TRUE);
  fclose(INFILE);

  /*for(i = 0; i < countML_Tree; i++)
    {
      p = ML_Tree[i].p;
      p->z = p->back->z =  ((double)ML_Tree[i].support) / ((double) numberOfTrees);
      }            */

  tr->ML_Tree = ML_Tree;
  tr->countML_Tree = countML_Tree;
  tr->numberOfTrees = numberOfTrees;

  printBipartitionResult(tr, adef, TRUE);  
}
