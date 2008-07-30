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

/*****************************FUNCTIONS FOR READING MULTIPLE MODEL SPECIFICATIONS************************************************/


extern char modelFileName[1024];

boolean lineContainsOnlyWhiteChars(char *line)
{
  int i, n = strlen(line);

  if(n == 0)
    return TRUE;

  for(i = 0; i < n; i++)
    {
      if(!whitechar(line[i]))
	return FALSE;
    }
  return TRUE;
}


int isNum(char c)
{
  
  return (c == '0' || c == '1' || c == '2' || c == '3' || c == '4' ||
	  c == '5' || c == '6' || c == '7' || c == '8' || c == '9');
}


void skipWhites(char **ch)
{
  while(**ch == ' ' || **ch == '\t')
    *ch = *ch + 1;
}

void analyzeIdentifier(char **ch, analdef *adef, int modelNumber)
{
  char ident[2048] = "";
  char model[128] = "";
  char *protModels[10] = {"DAYHOFF", "DCMUT", "JTT", "MTREV", "WAG", "RTREV", "CPREV", "VT", "BLOSUM62", "MTMAM"};
  char thisModel[1024];
  int i = 0, n;
  int containsComma = 0;

  while(**ch != '=')
    {
      if(**ch != ' ' && **ch != '\t')
	{
	  ident[i] = **ch;      
	  i++;
	}
      *ch = *ch + 1;
    }
  
  n = i;
  i = 0;
  
  for(i = 0; i < n; i++)
    if(ident[i] == ',') 
      containsComma = 1;
 
  if(containsComma)
    {
      i = 0;
      while(ident[i] != ',')
	{
	  model[i] = ident[i];
	  i++;
	}      

      for(i = 0; i < 10; i++)
	{	
	  strcpy(thisModel, protModels[i]);
	  
	  if(strcmp(model, thisModel) == 0)
	    {
	      adef->protModels[modelNumber] = i;
	      adef->protFreqs[modelNumber] = 0;
	      return;	
	    }
	  	  
	  strcpy(thisModel, protModels[i]);
	  strcat(thisModel, "F");
	  
	  if(strcmp(model, thisModel) == 0)
	    {
	      adef->protModels[modelNumber] = i;
	      adef->protFreqs[modelNumber] = 1;
	      return;
	    }	  	 	  
	}
      
      printf("ERROR: you specified the unknown protein model %s for partition %d\n", model, modelNumber);
      exit(-1);
    }
}



void setModel(int model, int position, int *a)
{
  if(a[position] == -1)
    a[position] = model;
  else
    {
      printf("ERROR trying to assign model %d to position %d \n", model, position);
      printf("while already model %d has been assigned to this position\n", a[position]);
      exit(-1);
    }      
}


int myGetline(char **lineptr, int *n, FILE *stream)
{
  char *line, *p;
  int size, copy, len;
  int chunkSize = 256 * sizeof(char);

   if (*lineptr == NULL || *n < 2) 
    {
      line = (char *)realloc(*lineptr, chunkSize);
      if (line == NULL)
	return -1;
      *lineptr = line;
      *n = chunkSize;
    }

   line = *lineptr;
   size = *n;
  
   copy = size;
   p = line;
   
   while(1)
     {
       while (--copy > 0)
	 {
	   register int c = getc(stream);
	   if (c == EOF)
	     goto lose;
	   else
	     {
	       *p++ = c;
	       if(c == '\n' || c == '\r')	
		 goto win;
	     }
	 }

       /* Need to enlarge the line buffer.  */
       len = p - line;
       size *= 2;
       line = realloc (line, size);
       if (line == NULL)
	 goto lose;
       *lineptr = line;
       *n = size;
       p = line + len;
       copy = size - len;
     }
   
 lose:
  if (p == *lineptr)
    return -1;
  /* Return a partial line since we got an error in the middle.  */
 win:
  *p = '\0';
  return p - *lineptr;
}



void parsePartitions(analdef *adef, rawdata *rdta, cruncheddata *cdta, tree *tr, boolean reRead)
{
  FILE *f; 
  int numberOfModels = 0;
  int bytes_read;
  int nbytes = 0;
  char *ch;
  char *cc = (char *)NULL;
  char **p_names;
  int n, i, l;
  int lower, upper, modulo;
  char buf[256];
  int **partitions;
  int pairsCount;
  int as, j;
  int k;

  if(reRead)
    {
      for(i = 0; i <= rdta->sites; i++)
	tr->model[i] = tr->saveModel[i];      
      return;
    }

  f = fopen(modelFileName, "r");
  
  if (!f)
    {
      printf( "Could not open multiple model file: %s\n", modelFileName);
      exit(-1);
    }

 
      while(myGetline(&cc, &nbytes, f) > -1)
	{     
	  if(!lineContainsOnlyWhiteChars(cc))
	    {
	      numberOfModels++;
	    }
	  if(cc)
	    free(cc);
	  cc = (char *)NULL;
	}     
      
      rewind(f);
      
      p_names = (char **)malloc(sizeof(char *) * numberOfModels);
      partitions = (int **)malloc(sizeof(int *) * numberOfModels);
      
      if(adef->protModels == (int *)NULL)
	adef->protModels = (int *)malloc(sizeof(int) * numberOfModels);
      if(adef->protFreqs == (int *)NULL)
	adef->protFreqs = (int *)malloc(sizeof(int) * numberOfModels);
      
      for(i = 0; i < numberOfModels; i++) 
	{
	  adef->protModels[i] = adef->proteinMatrix;
	  adef->protFreqs[i]  = adef->protEmpiricalFreqs;
	}

      for(i = 0; i < numberOfModels; i++)    
	partitions[i] = (int *)NULL;
    
      i = 0;
      while(myGetline(&cc, &nbytes, f) > -1)
	{          
	  if(!lineContainsOnlyWhiteChars(cc))
	    {
	      n = strlen(cc);	 
	      p_names[i] = (char *)malloc(sizeof(char) * (n + 1));
	      strcpy(&(p_names[i][0]), cc);
	      i++;
	    }
	  if(cc)
	    free(cc);
	  cc = (char *)NULL;
	}         

      for(i = 0; i < numberOfModels; i++)
	{           
	  ch = p_names[i];     
	  pairsCount = 0;
	  skipWhites(&ch);
     
	  if(*ch == '=')
	    {
	      printf("Identifier missing prior to '=' in %s\n", p_names[i]);
	      exit(-1);
	    }

	  analyzeIdentifier(&ch, adef, i);
	  *ch++;
	  /*
	    while(*ch != '=')	       
	    *ch++;	           
	    *ch++;*/
      
	numberPairs:
	  pairsCount++;
	  partitions[i] = (int *)realloc((void *)partitions[i], (1 + 3 * pairsCount) * sizeof(int));
	  partitions[i][0] = pairsCount;
	  partitions[i][3 + 3 * (pairsCount - 1)] = -1; 	
      
	  skipWhites(&ch);
     
	  if(!isNum(*ch))
	    {
	      printf("%c Number expected in %s\n", *ch, p_names[i]);
	      exit(-1);
	    }   
           
	  l = 0;
	  while(isNum(*ch))		 
	    {
	      /*printf("%c", *ch);*/
	      buf[l] = *ch;
	      *ch++;	
	      l++;
	    }
	  buf[l] = '\0';
	  lower = atoi(buf);
	  partitions[i][1 + 3 * (pairsCount - 1)] = lower;   
           
	  skipWhites(&ch);

	  /* NEW */
	  
	  if((*ch != '-') && (*ch != ','))
	    {
	      if(*ch == '\0' || *ch == '\n' || *ch == '\r')
		{
		  upper = lower;
		  goto SINGLE_NUMBER;
		}
	      else
		{
		  printf("'-' or ',' expected in %s\n", p_names[i]);
		  exit(-1);
		}
	    }	 

	  if(*ch == ',')
	    {	     
	      upper = lower;
	      goto SINGLE_NUMBER;
	    }

	  /* END NEW */

	  *ch++;   

	  skipWhites(&ch);
	  
	  if(!isNum(*ch))
	    {
	      printf("%c Number expected in %s\n", *ch, p_names[i]);
	      exit(-1);
	    }    
 
	  l = 0;
	  while(isNum(*ch))
	    {    
	      buf[l] = *ch;
	      *ch++;	
	      l++;
	    }
	  buf[l] = '\0';
	  upper = atoi(buf);     
	SINGLE_NUMBER:
	  partitions[i][2 + 3 * (pairsCount - 1)] = upper;        	  

	  if(upper < lower)
	    {
	      printf("Upper bound %d smaller than lower bound %d for this partition: %s\n", upper, lower,  p_names[i]);
	      exit(-1);
	    }

	  skipWhites(&ch);

	  if(*ch == '\0' || *ch == '\n' || *ch == '\r') /* PC-LINEBREAK*/
	    {    
	      goto parsed;
	    }
  
	  if(*ch == ',')
	    {	 
	      ch++;
	      goto numberPairs;
	    }
	        
	  if(*ch == '\\')
	    {
	      ch++;
	      skipWhites(&ch);
	      
	      if(!isNum(*ch))
		{
		  printf("%c Number expected in %s\n", *ch, p_names[i]);
		  exit(-1);
		}     
	      
	      l = 0;
	      while(isNum(*ch))
		{
		  buf[l] = *ch;
		  *ch++;	
		  l++;
		}
	      buf[l] = '\0';
	      modulo = atoi(buf);      
	      partitions[i][3 + 3 * (pairsCount - 1)] = modulo; 	
	      
	      skipWhites(&ch);
	      if(*ch == '\0' || *ch == '\n' || *ch == '\r')
		{	     
		  goto parsed;
		}
	      if(*ch == ',')
		{	       
		  ch++;
		  goto numberPairs;
		}
	    }  
       
	  printf("ERROR\n");
	  exit(-1);	   	 
       
	parsed:
	  i = i;
	}
  
  fclose(f);
 
  /*********************************************************************************************************************/ 

  for(i = 0; i <= rdta->sites; i++)
    tr->model[i] = -1;
  
   for(i = 0; i < numberOfModels; i++)
    {   
      as = partitions[i][0];     

      for(j = 0; j < as; j++)
	{
	  lower = partitions[i][1 + j * 3];
	  upper = partitions[i][2 + j * 3]; 
	  modulo = partitions[i][3 + j * 3];	
	 
	  if(modulo == -1)
	    {
	      for(k = lower; k <= upper; k++)
		setModel(i, k, tr->model);
	    }
	  else
	    {
	      for(k = lower; k <= upper; k += modulo)
		{
		  if(k <= rdta->sites)
		    setModel(i, k, tr->model);	      
		}
	    }
	}        
    }


    for(i = 1; i < rdta->sites + 1; i++)
      {

	if(tr->model[i] == -1)
	  {
	    printf("ERROR: Alignment Position %d has not been assigned any model\n", i);
	    exit(-1);
	  }      
      }  

    for(i = 0; i < numberOfModels; i++)
      {
	free(partitions[i]);
	free(p_names[i]);
      }
  
    free(partitions);
    free(p_names);    

    for(i = 0; i <= rdta->sites; i++)      
      tr->saveModel[i] = tr->model[i];    

    tr->NumberOfModels = numberOfModels;         
}





/***********************************************************************************************************************************************************************/
