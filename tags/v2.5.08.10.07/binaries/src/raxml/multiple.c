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


extern int optimizeRatesInvocations;
extern int optimizeRateCategoryInvocations;
extern int optimizeAlphaInvocations;
extern int checkPointCounter;
extern int Thorough;

extern double masterTime;

extern FILE   *INFILE, *permutationFile, *logFile, *infoFile;

extern char seq_file[1024];
extern char permFileName[1024], resultFileName[1024], 
  logFileName[1024], checkpointFileName[1024], infoFileName[1024], run_id[128], workdir[1024]; 

#ifndef PARALLEL

void doBootstrap(tree *tr, analdef *adef, rawdata *rdta, cruncheddata *cdta)
{
  int i, k, n;
  double  temp, wtemp, loopTime;
  n = adef->multipleRuns;
          
  for(i = 0; i < n; i++)
    {    
      loopTime = gettime();
                  
      tr->treeID = i;
      tr->checkPointCounter = 0;
     
      if(i > 0)
	{  	 
	  readData(TRUE, adef, rdta, cdta, tr);	 
	  makeweights(adef, rdta, cdta, tr);	  	  
	  makevalues(rdta, cdta, tr, adef, TRUE); 	  
	  initModel(tr, rdta, cdta, adef);                                
	}
      
      getStartingTree(tr, adef);
      computeBIGRAPID(tr, adef);         

      if(adef->bootstrapBranchLengths)
	{
	  if(adef->model == M_GTRGAMMA || adef->model == M_PROTGAMMA)     	    
	    modOpt(tr, adef);		      	    	    
	  else
	    {	  	      
	      if(adef->useMixedModel)
		{		  
		  tr->likelihood = unlikely;
		  freeNodex(tr); 
		  if(adef->model == M_GTRCAT)
		    adef->model = M_GTRGAMMA;	 
		  else
		    adef->model = M_PROTGAMMA;
		  allocNodex(tr, adef); 
		  initModel(tr, rdta, cdta, adef);	  	  	  
		  modOpt(tr, adef);			 		  	      
		  if(adef->model == M_GTRGAMMA)
		    adef->model = M_GTRCAT;
		  else
		    adef->model = M_PROTCAT;
		}	     	
	    }
	}
	  
      printBootstrapResult(tr, adef, TRUE);
      freeNodex(tr);                         
	              
      for(k = 1; k <= rdta->sites; k++)
	{	  
	  rdta->wgt[k] = rdta->wgt2[k] = 1;	 	  
	  cdta->aliaswgt[k] = 0;
	}
           	                         
      loopTime = gettime() - loopTime;
      
      writeInfoFile(adef, tr, loopTime);           
    }      
}


void doInference(tree *tr, analdef *adef, rawdata *rdta, cruncheddata *cdta)
{
  int i, j, n, bestI;
  double loopTime, avgTime, bestLH, avgLH;

  n = adef->multipleRuns;
       
  for(i = 0; i < n; i++)
    {                
      tr->treeID = i;
      tr->checkPointCounter = 0;
         
      loopTime = gettime();
               
      if(i > 0)	       
	initModel(tr, rdta, cdta, adef); 
               
      getStartingTree(tr, adef);     
    
      computeBIGRAPID(tr, adef);                     

      if(adef->model == M_GTRGAMMA || adef->model == M_PROTGAMMA)
	{	 
	  modOpt(tr, adef);	
	  printLog(tr, adef, TRUE);
	  printResult(tr, adef, TRUE);
	  loopTime = gettime() - loopTime;	 
	  tr->likelihoods[i] = tr->likelihood;
	  freeNodex(tr); 
	}
      else
	{	  
	  if(adef->useMixedModel)
	    {
	      tr->likelihood = unlikely;
	      freeNodex(tr); 
	      if(adef->model == M_GTRCAT)
		adef->model = M_GTRGAMMA;	 
	      else
		adef->model = M_PROTGAMMA;
	      allocNodex(tr, adef); 
	      initModel(tr, rdta, cdta, adef);	  	  	  
	      modOpt(tr, adef);	
	      printLog(tr, adef, TRUE);
	      printResult(tr, adef, TRUE);
	      loopTime = gettime() - loopTime;	
	      tr->likelihoods[i] = tr->likelihood;	 
	      freeNodex(tr);   
	      if(adef->model == M_GTRGAMMA)
		adef->model = M_GTRCAT;
	      else
		adef->model = M_PROTCAT;
	    }
	  else
	    {
	      loopTime = gettime() - loopTime;       
	      tr->likelihoods[i] = tr->likelihood;
	      freeNodex(tr);
	    }	
	}

      writeInfoFile(adef, tr, loopTime);                                     
    } 
}
#else



#include <mpi.h>

extern int processID;
extern int numOfWorkers;
static void sendTree(tree *tr, analdef *adef, double t, boolean finalPrint)
{
  int bufferSize, i, bufCount;
  double *buffer;
  char *tree_ptr;

  bufferSize = tr->treeStringLength + 4 + tr->NumberOfModels;

  buffer = (double *)malloc(sizeof(double) * bufferSize);
  
  bufCount = 0;
  
  buffer[bufCount++] = (double) adef->bestTrav;
  buffer[bufCount++] = (double) tr->treeID;
  buffer[bufCount++] = tr->likelihood;
  buffer[bufCount++] = t;

  for(i = 0; i < tr->NumberOfModels; i++)        
    buffer[bufCount++] = tr->alphas[i];
    
    
  if(adef->boot)
    {
     if(adef->bootstrapBranchLengths)
       Tree2String(tr->tree_string, tr, tr->start->back, TRUE, TRUE, FALSE, FALSE, finalPrint, adef);
     else
       Tree2String(tr->tree_string, tr, tr->start->back, FALSE, TRUE, FALSE, FALSE, finalPrint, adef);
    }
  else
    Tree2String(tr->tree_string, tr, tr->start->back, TRUE, TRUE, FALSE, FALSE, finalPrint, adef);

  tree_ptr = tr->tree_string;

  while(*tree_ptr != ';')    
    buffer[bufCount++] = (double)*tree_ptr++;        
 
  buffer[bufCount++] = (double)(';');
  buffer[bufCount++] = (double)('\n');

  MPI_Send(buffer, bufferSize, MPI_DOUBLE, 0, TREE, MPI_COMM_WORLD);
  
  free(buffer);
}

void receiveTree(tree *tr, analdef *adef, int workerID, double *t)
{
  int bufferSize, i, bufCount;
  double *buffer, *buf_ptr;
  char *tree_ptr, content;
  MPI_Status msgStatus; 

  bufferSize = tr->treeStringLength + 4 + tr->NumberOfModels;

  buffer = (double *)malloc(sizeof(double) * bufferSize);

  MPI_Recv(buffer, bufferSize, MPI_DOUBLE, workerID, TREE, MPI_COMM_WORLD, &msgStatus);
  
  bufCount = 0;
  
  adef->bestTrav = (int)buffer[bufCount++]; 
  tr->treeID     = (int) buffer[bufCount++];
  tr->likelihood = buffer[bufCount++];
  *t = buffer[bufCount++];

  tr->likelihoods[tr->treeID] = tr->likelihood;

  for(i = 0; i < tr->NumberOfModels; i++)    
    tr->alphas[i] = buffer[bufCount++];

  buf_ptr = &buffer[bufCount];
  tree_ptr = tr->tree_string;

  while((content = (char)(buffer[bufCount++])) != ';')
    {      
      *tree_ptr++ = content;
    }
  
  *tree_ptr++ = ';';
  *tree_ptr++ = '\n';
#ifdef DEBUG
  printf("Received tree %s\n", tr->tree_string);
#endif 
  free(buffer);
}


void doBootstrap(tree *tr, analdef *adef, rawdata *rdta, cruncheddata *cdta)
{
  int i, k, n, dummy;
  double  temp, wtemp, loopTime;
  MPI_Status msgStatus; 

  n = adef->multipleRuns;
          
  if(processID == 0)
    {
      int jobsSent = 0;
      int jobsReceived = n;

      while(jobsReceived > 0)
	{
	  MPI_Probe(MPI_ANY_SOURCE, MPI_ANY_TAG, MPI_COMM_WORLD, &msgStatus);
	  switch(msgStatus.MPI_TAG)
	    {
	    case JOB_REQUEST:
#ifdef DEBUG
	      printf("Master receiving work request from worker %d\n",  msgStatus.MPI_SOURCE);
#endif	      
	      MPI_Recv(&dummy, 1, MPI_INT, msgStatus.MPI_SOURCE, JOB_REQUEST, MPI_COMM_WORLD, &msgStatus);
	       if(jobsSent < n)
		 {
		   MPI_Send(&jobsSent, 1, MPI_INT, msgStatus.MPI_SOURCE, COMPUTE_TREE, MPI_COMM_WORLD);
#ifdef DEBUG
		   printf("Master sending job %d to worker %d\n",  jobsSent, msgStatus.MPI_SOURCE);
#endif
		   jobsSent++;
		 }
	       break;
	    case TREE:
#ifdef DEBUG
	      printf("--------> Master receiving tree from worker %d\n",  msgStatus.MPI_SOURCE);	
#endif
	      receiveTree(tr, adef, msgStatus.MPI_SOURCE, &loopTime);	     	   	      
	      printBootstrapResult(tr, adef, TRUE);
	      printf("Bootstrap[%d] completed\n", tr->treeID);	 
	      writeInfoFile(adef, tr, loopTime);
	      jobsReceived--;
	      if(jobsSent < n)
		{
		  MPI_Send(&jobsSent, 1, MPI_INT, msgStatus.MPI_SOURCE, COMPUTE_TREE, MPI_COMM_WORLD);
#ifdef DEBUG
		  printf("Master sending job %d to worker %d\n",  jobsSent, msgStatus.MPI_SOURCE);
#endif
		  jobsSent++;
		}
	      break;
	    }
	}
      
       for(i = 1; i < numOfWorkers; i++)
	{
	  MPI_Send(&dummy, 1, MPI_INT, i, FINALIZE, MPI_COMM_WORLD);
#ifdef DEBUG
	  printf("Master sending FINALIZE to worker %d\n",  i);
#endif
	}
       return;
    }
  else
    {
      int treeCounter = 0;

      MPI_Send(&dummy, 1, MPI_INT, 0, JOB_REQUEST, MPI_COMM_WORLD);
#ifdef DEBUG
      printf("Worker %d sending job request to master\n",  processID);
#endif      
       while(1)
	{	
	  MPI_Probe(0, MPI_ANY_TAG, MPI_COMM_WORLD, &msgStatus); 
	  	 
	  switch(msgStatus.MPI_TAG)
	    {
	    case COMPUTE_TREE: 
	      MPI_Recv(&dummy, 1, MPI_INT, 0, COMPUTE_TREE, MPI_COMM_WORLD, &msgStatus);	      
#ifdef DEBUG
	      printf("Worker %d receiving job %d from master\n",  processID, dummy);
#endif	
	      loopTime = masterTime = gettime();

	      tr->treeID = dummy;
	      tr->checkPointCounter = 0;

	      if(treeCounter > 0)
		{  
		  readData(TRUE, adef, rdta, cdta, tr);	 
		  makeweights(adef, rdta, cdta, tr);
		  makevalues(rdta, cdta, tr, adef, TRUE); 		 
		  initModel(tr, rdta, cdta, adef);                                
		}
	      
	      treeCounter++;

	      getStartingTree(tr, adef);
	      computeBIGRAPID(tr, adef);         

	      if(adef->bootstrapBranchLengths)
		{
		  if(adef->model == M_GTRGAMMA || adef->model == M_PROTGAMMA)     	    
		    modOpt(tr, adef);		      	    	    
		  else
		    {	  	      
		      if(adef->useMixedModel)
			{		  
			  tr->likelihood = unlikely;
			  freeNodex(tr); 
			  if(adef->model == M_GTRCAT)
			    adef->model = M_GTRGAMMA;	 
			  else
			    adef->model = M_PROTGAMMA;
			  allocNodex(tr, adef); 
			  initModel(tr, rdta, cdta, adef);	  	  	  
			  modOpt(tr, adef);			 		  	      
			  if(adef->model == M_GTRGAMMA)
			    adef->model = M_GTRCAT;
			  else
			    adef->model = M_PROTCAT;
			}	     	
		    }
		}	      	   
	      
	      freeNodex(tr);                                  
	      
	      for(i = 1; i <= rdta->sites; i++)
		{	  
		  rdta->wgt[i] = rdta->wgt2[i] = 1;	 	  
		  cdta->aliaswgt[i] = 0;
		}
           	                         
	      loopTime = gettime() - loopTime;
	      sendTree(tr, adef, loopTime, TRUE);
	      break;
	    case FINALIZE:
	      MPI_Recv(&dummy, 1, MPI_INT, 0, FINALIZE, MPI_COMM_WORLD, &msgStatus);
#ifdef DEBUG
	      printf("Worker %d receiving FINALIZE %d\n",  processID);
#endif
	      return;
	    }
	}
    }  
}

void doInference(tree *tr, analdef *adef, rawdata *rdta, cruncheddata *cdta)
{
  int i, k, n, dummy;
  double  temp, wtemp, loopTime;
  MPI_Status msgStatus; 

  n = adef->multipleRuns;
          
  if(processID == 0)
    {
      int jobsSent = 0;
      int jobsReceived = n;

      while(jobsReceived > 0)
	{
	  MPI_Probe(MPI_ANY_SOURCE, MPI_ANY_TAG, MPI_COMM_WORLD, &msgStatus);
	  switch(msgStatus.MPI_TAG)
	    {
	    case JOB_REQUEST:
#ifdef DEBUG
	      printf("Master receiving work request from worker %d\n",  msgStatus.MPI_SOURCE);
#endif	      
	      MPI_Recv(&dummy, 1, MPI_INT, msgStatus.MPI_SOURCE, JOB_REQUEST, MPI_COMM_WORLD, &msgStatus);
	      if(jobsSent < n)
		{
		  MPI_Send(&jobsSent, 1, MPI_INT, msgStatus.MPI_SOURCE, COMPUTE_TREE, MPI_COMM_WORLD);
#ifdef DEBUG
		  printf("Master snding job %d to worker %d\n",  jobsSent, msgStatus.MPI_SOURCE);
#endif
		  jobsSent++;
		}
	       break;
	    case TREE:
#ifdef DEBUG
	      printf("--------> Master receiving tree from worker %d\n",  msgStatus.MPI_SOURCE);	
#endif
	      receiveTree(tr, adef, msgStatus.MPI_SOURCE, &loopTime);	     	   	      	      
	      printf("Inference[%d] completed\n", tr->treeID);	 
	      writeInfoFile(adef, tr, loopTime);
	      jobsReceived--;
	      if(jobsSent < n)
		{
		  MPI_Send(&jobsSent, 1, MPI_INT, msgStatus.MPI_SOURCE, COMPUTE_TREE, MPI_COMM_WORLD);
#ifdef DEBUG
		  printf("Master sending job %d to worker %d\n",  jobsSent, msgStatus.MPI_SOURCE);
#endif
		  jobsSent++;
		}
	      break;
	    }
	}
      
       for(i = 1; i < numOfWorkers; i++)
	{
	  MPI_Send(&dummy, 1, MPI_INT, i, FINALIZE, MPI_COMM_WORLD);
#ifdef DEBUG
	  printf("Master sending FINALIZE to worker %d\n",  i);
#endif
	}
       return;
    }
  else
    {
      int treeCounter = 0;

      MPI_Send(&dummy, 1, MPI_INT, 0, JOB_REQUEST, MPI_COMM_WORLD);
#ifdef DEBUG
      printf("Worker %d sending job request to master\n",  processID);
#endif      
       while(1)
	{	
	  MPI_Probe(0, MPI_ANY_TAG, MPI_COMM_WORLD, &msgStatus); 
	  	 
	  switch(msgStatus.MPI_TAG)
	    {
	    case COMPUTE_TREE: 
	      MPI_Recv(&dummy, 1, MPI_INT, 0, COMPUTE_TREE, MPI_COMM_WORLD, &msgStatus);	      
#ifdef DEBUG
	      printf("Worker %d receiving job %d from master\n",  processID, dummy);
#endif
	      loopTime =  masterTime = gettime();

	      tr->treeID = dummy;
	      tr->checkPointCounter = 0;
	                    
	      if(treeCounter > 0)	       
		initModel(tr, rdta, cdta, adef); 

	      treeCounter++;

	      getStartingTree(tr, adef);  
     
	      computeBIGRAPID(tr, adef);                     

	      if(adef->model == M_GTRGAMMA || adef->model == M_PROTGAMMA)
		{
		  modOpt(tr, adef);			
		  printLog(tr, adef, TRUE);
		  printResult(tr, adef, TRUE);
		  loopTime = gettime() - loopTime;	 		 
		  freeNodex(tr); 
		}
	      else
		{	  
		  if(adef->useMixedModel)
		    {
		      tr->likelihood = unlikely;
		      freeNodex(tr); 
		      if(adef->model == M_GTRCAT)
			adef->model = M_GTRGAMMA;	 
		      else
			adef->model = M_PROTGAMMA;		      
		      allocNodex(tr, adef); 
		      initModel(tr, rdta, cdta, adef);	  	  	  
		      modOpt(tr, adef);	
		      printLog(tr, adef, TRUE);
		      printResult(tr, adef, TRUE);
		      loopTime = gettime() - loopTime;			    
		      freeNodex(tr);   
		      if(adef->model == M_GTRGAMMA)
			adef->model = M_GTRCAT;
		      else
			adef->model = M_PROTCAT;		      
		    }
		  else
		    {
		      loopTime = gettime() - loopTime;       		      
		      freeNodex(tr);
		    }	
		}
	     
	      sendTree(tr, adef, loopTime, TRUE);
	      break;
	    case FINALIZE:
	      MPI_Recv(&dummy, 1, MPI_INT, 0, FINALIZE, MPI_COMM_WORLD, &msgStatus);
#ifdef DEBUG
	      printf("Worker %d receiving FINALIZE %d\n",  processID);
#endif
	      return;
	    }
	}
    }  
}



#endif
