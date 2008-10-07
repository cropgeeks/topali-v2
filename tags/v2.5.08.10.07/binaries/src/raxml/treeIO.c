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

extern FILE *INFILE;
extern char infoFileName[1024];
extern char tree_file[1024];
extern char *likelihood_key;
extern char *ntaxa_key;
extern char *smoothed_key;
extern int partCount;
extern double masterTime;

extern boolean (*newview)           (tree *, nodeptr);
extern double  (*evaluate)          (tree *, nodeptr);   
extern double  (*makenewz)          (tree *, nodeptr, nodeptr, double, int);





boolean readKeyValue (char *string, char *key, char *format, void *value)
  { /* readKeyValue */

    if (!(string = strstr(string, key)))  return FALSE;
    string += strlen(key);
    string = strchr(string, '=');
    if (! (string = strchr(string, '=')))  return FALSE;
    string++;
    return  sscanf(string, format, value);  /* 1 if read, otherwise 0 */
  } /* readKeyValue */

static int lookupBipartition(tree *tr, nodeptr p)
{
  int i, res;
  boolean found = FALSE;
  int qNum = p->back->number, 
    pNum = p->number;
  double support;

  for(i = 0; (i < tr->countML_Tree) && (!found); i++)
    {
      if((pNum == tr->ML_Tree[i].pNum && qNum == tr->ML_Tree[i].qNum) || (pNum == tr->ML_Tree[i].qNum && qNum == tr->ML_Tree[i].pNum))
	{
	  support = ((double)(tr->ML_Tree[i].support)) / ((double) (tr->numberOfTrees));
	  res = round(support * 100.0);
	  return res;
	  found = TRUE;
	}
    }

  printf("FATAL ERROR BIPARTITION SUPPORT VALUE NOT FOUND %d\n", p->number);
  exit(-1);

  return -1;
}

static char *Tree2StringREC(char *treestr, tree *tr, nodeptr p, boolean printBranchLengths, boolean printNames, boolean printLikelihood, boolean rellTree, boolean finalPrint)
{
  double  x, z;
  char  *nameptr;
  int    c;  

  if (p->tip) 
    {	     
      if(printNames)
	{
	  nameptr = tr->nameList[p->number];     
	  sprintf(treestr, "%s", nameptr);
	}
      else
	sprintf(treestr, "%d", p->number);
      
      while (*treestr) treestr++;
    }
  else 
    {
      *treestr++ = '(';
      treestr = Tree2StringREC(treestr, tr, p->next->back, printBranchLengths, printNames, printLikelihood, rellTree, finalPrint);
      *treestr++ = ',';
      treestr = Tree2StringREC(treestr, tr, p->next->next->back, printBranchLengths, printNames, printLikelihood, rellTree, finalPrint);
      if (p == tr->start->back) 
	{
	  *treestr++ = ',';
	  treestr = Tree2StringREC(treestr, tr, p->back, printBranchLengths, printNames, printLikelihood, rellTree, finalPrint);
        }
      *treestr++ = ')';            
      }

    if (p == tr->start->back) 
      {	
	if(printBranchLengths && !rellTree)
	  sprintf(treestr, ":0.0;\n");
	else
	  sprintf(treestr, ";\n");	 
      }
    else 
      {      
	if(rellTree)
	  {
	    if((!p->tip) && (!p->back->tip))
	      {
		/*int value = round(p->z * 100.0);*/		
		/*sprintf(treestr, ":%f", (p->z));*/
		int value = lookupBipartition(tr, p);
		sprintf(treestr, "%d:%8.20f", value, p->z);
	      }
	    else
	      {
		/*sprintf(treestr, ":1.000000");*/
		sprintf(treestr, ":%8.20f", p->z);
	      }
	  }
	else
	  {
	    if(printBranchLengths)
	      {
		z = p->z;
		if (z < zmin) z = zmin;      	 
		x = -log(z) * tr->fracchange;
		sprintf(treestr, ":%8.20f", x);	
	      }
	    else
	      sprintf(treestr, "\0");
	  }
      }

    while (*treestr) treestr++;
    return  treestr;
}


static void collectSubtrees(nodeptr p, nodeptr *subtrees, int *count, int ogn)
{
  if(p->tip)
    return;
  else
    {
      nodeptr q;
      if(countTips(p) == ogn)
	{
	  subtrees[*count] = p;
	  *count = *count + 1;
	}
	  
      q = p->next;
      while(q != p)
	{
	  collectSubtrees(q->back, subtrees, count, ogn);
	  q = q->next;
	}
    }
}

static void checkOM(nodeptr p, int *n, int *c, tree *tr)
{
  if(p->tip)
    {
      n[*c] = p->number;
      *c = *c + 1;
      /*printf("%s ", tr->nameList[p->number]);*/
    }
  else
    {
      nodeptr q = p->next;

      while(q != p)
	{
	  checkOM(q->back, n, c, tr);
	  q = q->next;
	}
    }
}
    
static char *rootedTreeREC(char *treestr, tree *tr, nodeptr p, boolean printBranchLengths, boolean printNames, boolean printLikelihood, boolean rellTree, 
			   boolean finalPrint, analdef *adef)
{
  double  x, z;
  char  *nameptr;
  int    c;  

  if (p->tip) 
    {	     
      if(printNames)
	{
	  nameptr = tr->nameList[p->number];     
	  sprintf(treestr, "%s", nameptr);
	}
      else
	sprintf(treestr, "%d", p->number);
      
      while (*treestr) treestr++;
    }
  else 
    {
      *treestr++ = '(';
      treestr = rootedTreeREC(treestr, tr, p->next->back, printBranchLengths, printNames, printLikelihood, rellTree, finalPrint, adef);
      *treestr++ = ',';
      treestr = rootedTreeREC(treestr, tr, p->next->next->back, printBranchLengths, printNames, printLikelihood, rellTree, finalPrint, adef);      
      *treestr++ = ')';            
      }

  if(rellTree)
    {
      if(!p->tip && !p->back->tip)
	{
	  /*int value = round(p->z * 100.0);*/		
	  /*sprintf(treestr, ":%f", (p->z));*/
	  int value = lookupBipartition(tr, p);
	  sprintf(treestr, "%d:%8.20f", value, p->z);
	}
      else
	{
	  /*sprintf(treestr, ":1.000000");*/
	  sprintf(treestr, ":%8.20f", p->z);
	}
    }
  else
    {
      if(printBranchLengths)
	{
	  z = p->z;
	  if (z < zmin) z = zmin;      	 
	  x = -log(z) * tr->fracchange;
	  sprintf(treestr, ":%8.20f", x);	
	}
      else
	sprintf(treestr, "\0");
    }

    while (*treestr) treestr++;
    return  treestr;

}

static char *rootedTree(char *treestr, tree *tr, nodeptr p, boolean printBranchLengths, boolean printNames, boolean printLikelihood, boolean rellTree, 
			boolean finalPrint, analdef *adef)
{
  double oldz = p->z;


  /*{
    double z = 0.9;
    double rz = exp(-(z * 0.5)/ 0.1);
    
    printf("%f ",-log(rz) * 0.1);   
    
    exit(1);
    }*/

  if(rellTree)    
    p->z = p->back->z = oldz * 0.5;
  else
    {
      double rz, z;
      z = -log(p->z) * tr->fracchange;
      rz = exp(-(z * 0.5)/ tr->fracchange);
      p->z = p->back->z = rz;
    }

  *treestr = '(';
  *treestr++;
  treestr = rootedTreeREC(treestr, tr, p,  printBranchLengths, printNames, printLikelihood, rellTree, finalPrint, adef);
  *treestr = ',';
  *treestr++;
  treestr = rootedTreeREC(treestr, tr, p->back,  printBranchLengths, printNames, printLikelihood, rellTree, finalPrint, adef);
  sprintf(treestr, ");\n");
  while (*treestr) treestr++;

  p->z = p->back->z = oldz;
  
  return  treestr;
}



char *Tree2String(char *treestr, tree *tr, nodeptr p, boolean printBranchLengths, boolean printNames, boolean printLikelihood, boolean rellTree, 
		  boolean finalPrint, analdef *adef)
{
  double  x, z;
  char  *nameptr;
  int    c;  
 
  if(finalPrint && adef->outgroup)
    {
      nodeptr startNode = tr->start;

      if(tr->numberOfOutgroups > 1)
	{
	  nodeptr root;
	  nodeptr *subtrees = (nodeptr *)malloc(sizeof(nodeptr) * tr->mxtips);
	  int i, k, count = 0;
	  int *nodeNumbers = (int*)malloc(sizeof(int) * tr->numberOfOutgroups);
	  int *foundVector = (int*)malloc(sizeof(int) * tr->numberOfOutgroups);
	  boolean monophyletic = FALSE;

	  collectSubtrees(tr->start->back, subtrees, &count, tr->numberOfOutgroups);

	  /*printf("Found %d subtrees of size  %d\n", count, tr->numberOfOutgroups);*/

	  for(i = 0; (i < count) && (!monophyletic); i++)
	    {
	      int l, sum, nc = 0;
	      for(k = 0; k <  tr->numberOfOutgroups; k++)
		{
		  nodeNumbers[k] = -1;
		  foundVector[k] = 0;
		}
	      checkOM(subtrees[i], nodeNumbers, &nc, tr);
	      
	      for(l = 0; l < tr->numberOfOutgroups; l++)
		for(k = 0; k < tr->numberOfOutgroups; k++)
		  {
		    if(nodeNumbers[l] == tr->outgroupNums[k])
		      foundVector[l] = 1;
		  }
	      
	      sum = 0;
	      for(l = 0; l < tr->numberOfOutgroups; l++)
		sum += foundVector[l];
	      
	      if(sum == tr->numberOfOutgroups)
		{	       		  
		  root = subtrees[i];
		  tr->start = root;		
		  /*printf("outgroups are monphyletic!\n");*/
		  monophyletic = TRUE;		  
		}
	      else
		{
		  if(sum > 0)
		    {
		      /*printf("outgroups are NOT monophyletic!\n");*/
		      monophyletic = FALSE;
		    }	     
		}	
	    }
	  
	  if(!monophyletic)
	    {
	      printf("WARNING, outgroups are not monophyletic, using first outgroup \"%s\"\n", tr->nameList[tr->outgroupNums[0]]);
	      printf("from the list to root the tree!\n");
	     
#ifndef PARALLEL
	      {
		FILE *infoFile = fopen(infoFileName, "a");

		fprintf(infoFile, "\nWARNING, outgroups are not monophyletic, using first outgroup \"%s\"\n", tr->nameList[tr->outgroupNums[0]]);
		fprintf(infoFile, "from the list to root the tree!\n");
		
		fclose(infoFile);
	      }
#endif 

	      tr->start = tr->nodep[tr->outgroupNums[0]];
	      /*Tree2StringREC(treestr, tr, tr->start->back, printBranchLengths, printNames, printLikelihood, rellTree, finalPrint);*/
	      rootedTree(treestr, tr, tr->start->back, printBranchLengths, printNames, printLikelihood, rellTree, finalPrint, adef);
	    }
	  else
	    {	     
	      if(tr->start->tip)
		{
		  printf("Outgroup-Monophyly ERROR; tr->start is a tip \n");
		  errorExit(-1);
		}
	      if(tr->start->back->tip)
	      	{
		  printf("Outgroup-Monophyly ERROR; tr->start is a tip \n");
		  errorExit(-1);
		}
	      
	      /*	      Tree2StringREC(treestr, tr, tr->start->back, printBranchLengths, printNames, printLikelihood, rellTree, finalPrint);*/
	      rootedTree(treestr, tr, tr->start->back, printBranchLengths, printNames, printLikelihood, rellTree, finalPrint, adef);
	    }
	  
	  free(foundVector);
	  free(nodeNumbers);
	  free(subtrees);
	}
      else
	{
	  /*printf("Skipping Monophyly Check, only one outgroup\n");*/
	  tr->start = tr->nodep[tr->outgroupNums[0]];
	  /*Tree2StringREC(treestr, tr, tr->start->back, printBranchLengths, printNames, printLikelihood, rellTree, finalPrint); */
	  rootedTree(treestr, tr, tr->start->back, printBranchLengths, printNames, printLikelihood, rellTree, finalPrint, adef);
	}      

      tr->start = startNode;
    }
  else
    {
      Tree2StringREC(treestr, tr, p, printBranchLengths, printNames, printLikelihood, rellTree, finalPrint);  
    }
  while (*treestr) treestr++;
  return treestr;
}



/*=======================================================================*/
/*                         Read a tree from a file                       */
/*=======================================================================*/


/*  1.0.A  Processing of quotation marks in comment removed
 */

int treeFinishCom (FILE *fp, char **strp)
  { /* treeFinishCom */
    int  ch;

    while ((ch = getc(fp)) != EOF && ch != ']') {
      if (strp != NULL) *(*strp)++ = ch;    /* save character  */
      if (ch == '[') {                      /* nested comment; find its end */
        if ((ch = treeFinishCom(fp, strp)) == EOF)  break;
        if (strp != NULL) *(*strp)++ = ch;  /* save closing ]  */
        }
      }

    if (strp != NULL) **strp = '\0';        /* terminate string  */
    return  ch;
  } /* treeFinishCom */


int treeGetCh (FILE *fp)         /* get next nonblank, noncomment character */
  { /* treeGetCh */
    int  ch;

    while ((ch = getc(fp)) != EOF) {
      if (whitechar(ch)) ;
      else if (ch == '[') {                   /* comment; find its end */
        if ((ch = treeFinishCom(fp, (char **) NULL)) == EOF)  break;
        }
      else  break;
      }

    return  ch;
  } /* treeGetCh */


boolean  treeLabelEnd (int ch)
  { /* treeLabelEnd */
    switch (ch) {
    case EOF:  case '\0':  case '\t':  case '\n':  case '\r': case ' ':
    case ':':  case ',':   case '(':   case ')':   /*case '[':*/
        case ';':
          return TRUE;
        default:
          break;
        }
    return FALSE;
  } /* treeLabelEnd */


boolean  treeGetLabel (FILE *fp, char *lblPtr, int maxlen)
  { /* treeGetLabel */
    int      ch;
    boolean  done, quoted, lblfound;

    if (--maxlen < 0) lblPtr = (char *) NULL;  /* reserves space for '\0' */
    else if (lblPtr == NULL) maxlen = 0;

    ch = getc(fp);
    done = treeLabelEnd(ch);

    lblfound = ! done;
    quoted = (ch == '\'');
    if (quoted && ! done) {ch = getc(fp); done = (ch == EOF);}

    while (! done) {
      if (quoted) {
        if (ch == '\'') {ch = getc(fp); if (ch != '\'') break;}
        }

      else if (treeLabelEnd(ch)) break;
      /*
	else if (ch == '_') ch = ' ';*/  /* unquoted _ goes to space */

      if (--maxlen >= 0) *lblPtr++ = ch;
      ch = getc(fp);
      if (ch == EOF) break;
      }

    if (ch != EOF)  (void) ungetc(ch, fp);

    if (lblPtr != NULL) *lblPtr = '\0';

    return lblfound;
  } /* treeGetLabel */


boolean  treeFlushLabel (FILE *fp)
  { /* treeFlushLabel */
    return  treeGetLabel(fp, (char *) NULL, (int) 0);
  } /* treeFlushLabel */


int  treeFindTipByLabel (char  *str, tree *tr)
                     /*  str -- label string pointer */
  { /* treeFindTipByLabel */
    nodeptr  q;
    char    *nameptr;
    int      ch, i, n;
    boolean  found;
    char num[64];
    char *numptr;

    for (n = 1; n <= tr->mxtips; n++) 
      {
	q = tr->nodep[n];

	if (! (q->back)) 
	  {        
	    i = 0;
	    
	    nameptr = tr->nameList[q->number];
       
	    while ((found = (str[i++] == (ch = *nameptr++))) && ch) ;
	    if (found) return n; 	 	

	    /*
	      Uncomment this to read NEXUS-style trees with taxon number instead of names
	      i = 0;
	      numptr = num;
	      sprintf(numptr,"%d", q->number);
	      while((found = (str[i++] == (ch = *numptr++))) && ch) ;
	      if (found) return n; 
	    */
	  }
      
      }

    printf("ERROR: Cannot find tree species: %s\n", str);

    return  0;
  } /* treeFindTipByLabel */


int  treeFindTipName (FILE *fp, tree *tr)
  { /* treeFindTipName */
    char    *nameptr, str[nmlngth+2];
    int      n;

    if (tr->prelabeled) {
      if (treeGetLabel(fp, str, nmlngth+2))
        n = treeFindTipByLabel(str, tr);
      else
        n = 0;
      }

    else if (tr->ntips < tr->mxtips) {
      n = tr->ntips + 1;
      
      nameptr = tr->nameList[n];
   
      if (! treeGetLabel(fp, nameptr, nmlngth+1)) n = 0;
      }

    else {
      n = 0;
      }

    return  n;
  } /* treeFindTipName */


void  treeEchoContext (FILE *fp1, FILE *fp2, int n)
 { /* treeEchoContext */
   int      ch;
   boolean  waswhite;

   waswhite = TRUE;

   while (n > 0 && ((ch = getc(fp1)) != EOF)) {
     if (whitechar(ch)) {
       ch = waswhite ? '\0' : ' ';
       waswhite = TRUE;
       }
     else {
       waswhite = FALSE;
       }

     if (ch > '\0') {putc(ch, fp2); n--;}
     }
 } /* treeEchoContext */


boolean treeProcessLength (FILE *fp, double *dptr)
  { /* treeProcessLength */
    int  ch;

    if ((ch = treeGetCh(fp)) == EOF)  return FALSE;    /*  Skip comments */
    (void) ungetc(ch, fp);

    if (fscanf(fp, "%lf", dptr) != 1) {
      printf("ERROR: treeProcessLength: Problem reading branch length\n");
      treeEchoContext(fp, stdout, 40);
      printf("\n");
      return  FALSE;
      }

    return  TRUE;
  } /* treeProcessLength */


int treeFlushLen (FILE  *fp)
  { /* treeFlushLen */
    double  dummy;
    boolean res;
    int     ch;

    ch = treeGetCh(fp);
    
    if (ch == ':') 
      {
	ch = treeGetCh(fp);
	
	    ungetc(ch, fp);
	    if(!treeProcessLength(fp, & dummy)) return 0;
	    return 1;	  
      }
   
    

    if (ch != EOF) (void) ungetc(ch, fp);
    return 1;
  } /* treeFlushLen */





boolean  treeNeedCh (FILE *fp, int c1, char *where)
  { /* treeNeedCh */
    int  c2;

    if ((c2 = treeGetCh(fp)) == c1)  return TRUE;

    printf("ERROR: Expecting '%c' %s tree; found:", c1, where);
    if (c2 == EOF) {
      printf("End-of-File");
      }
    else {
      ungetc(c2, fp);
      treeEchoContext(fp, stdout, 40);
      }
    putchar('\n');
    return FALSE;
  } /* treeNeedCh */





boolean addElementLen (FILE *fp, tree *tr, nodeptr p, boolean readBranchLengths)
  {
    double   z, branch;
    nodeptr  q;
    int      n, ch, fres;
    
    if ((ch = treeGetCh(fp)) == '(') 
      { 
	n = (tr->nextnode)++;
	if (n > 2*(tr->mxtips) - 2) 
	  {
	    if (tr->rooted || n > 2*(tr->mxtips) - 1) 
	      {
		printf("ERROR: Too many internal nodes.  Is tree rooted?\n");
		printf("       Deepest splitting should be a trifurcation.\n");
		return FALSE;
	      }
	    else 
	      {
		tr->rooted = TRUE;
	      }
	  }
	q = tr->nodep[n];
	if (! addElementLen(fp, tr, q->next, readBranchLengths))        return FALSE;
	if (! treeNeedCh(fp, ',', "in"))             return FALSE;
	if (! addElementLen(fp, tr, q->next->next, readBranchLengths))  return FALSE;
	if (! treeNeedCh(fp, ')', "in"))             return FALSE;
	(void) treeFlushLabel(fp);
      }
    else 
      {   
	ungetc(ch, fp);
	if ((n = treeFindTipName(fp, tr)) <= 0)          return FALSE;
	q = tr->nodep[n];
	if (tr->start->number > n)  tr->start = q;
	(tr->ntips)++;
      }
  
    if(readBranchLengths)
      {
	double branch;
	if (! treeNeedCh(fp, ':', "in"))                 return FALSE;
	if (! treeProcessLength(fp, &branch))            return FALSE;
	
	/*printf("Branch %8.20f\n", branch);*/
	hookup(p, q, branch);
      }
    else
      {
	fres = treeFlushLen(fp);
	if(!fres) return FALSE;
    
	hookup(p, q, defaultz);
      }
    return TRUE;          
  } 






int saveTreeCom (char  **comstrp)
  { /* saveTreeCom */
    int  ch;
    boolean  inquote;

    inquote = FALSE;
    while ((ch = getc(INFILE)) != EOF && (inquote || ch != ']')) {
      *(*comstrp)++ = ch;                        /* save character  */
      if (ch == '[' && ! inquote) {              /* comment; find its end */
        if ((ch = saveTreeCom(comstrp)) == EOF)  break;
        *(*comstrp)++ = ch;                      /* add ] */
        }
      else if (ch == '\'') inquote = ! inquote;  /* start or end of quote */
      }

    return  ch;
  } /* saveTreeCom */


boolean processTreeCom (FILE *fp, tree *tr)
  { /* processTreeCom */
    int   text_started, functor_read, com_open;

    /*  Accept prefatory "phylip_tree(" or "pseudoNewick("  */

    functor_read = text_started = 0;
    (void) fscanf(fp, " p%nhylip_tree(%n", & text_started, & functor_read);
    if (text_started && ! functor_read) {
      (void) fscanf(fp, "seudoNewick(%n", & functor_read);
      if (! functor_read) {
        printf("Start of tree 'p...' not understood.\n");
        return FALSE;
        }
      }

    com_open = 0;
    (void) fscanf(fp, " [%n", & com_open);

    if (com_open) {                                  /* comment; read it */
      char  com[1024], *com_end;

      com_end = com;
      if (treeFinishCom(fp, & com_end) == EOF) {     /* omits enclosing []s */
        printf("Missing end of tree comment\n");
        return FALSE;
        }

      (void) readKeyValue(com, likelihood_key, "%lg",
                               (void *) &(tr->likelihood));
      
      (void) readKeyValue(com, smoothed_key,   "%d",
                               (void *) &(tr->smoothed));

      if (functor_read) (void) fscanf(fp, " ,");   /* remove trailing comma */
      }

    return (functor_read > 0);
  } /* processTreeCom */


nodeptr uprootTree (tree *tr, nodeptr p)
  { /* uprootTree */
    nodeptr  q, r, s, start;
    int      n;    
    
    
    if (p->tip || p->back) 
      {
	printf("ERROR: Unable to uproot tree.\n");
	printf("       Inappropriate node marked for removal.\n");
	exit(-1);
	return (nodeptr) NULL;
      }

    n = --(tr->nextnode);               /* last internal node added */
    if (n != tr->mxtips + tr->ntips - 1) 
      {
	printf("ERROR: Unable to uproot tree.  Inconsistent\n");
	printf("       number of tips and nodes for rooted tree.\n");
	exit(-1);
	return (nodeptr) NULL;
      }

    q = p->next->back;                  /* remove p from tree */
    r = p->next->next->back;
    hookup(q, r, defaultz);

    if(tr->grouped)
      {
	/*printf("P-NUMBER %d Grouping %d\n", p->number, tr->constraintVector[p->number]);*/
	if(tr->constraintVector[p->number] != 0)
	  {
	    printf("Root node to remove shooud have top-level grouping of 0\n");
	    exit(-1);
	  }
      }

    start = (r->tip || (! q->tip)) ? r : r->next->next->back;

    if (tr->ntips > 2 && p->number != n) 
      {
	q = tr->nodep[n];            /* transfer last node's conections to p */
	r = q->next;
	s = q->next->next;
	
	if(tr->grouped)
	  {
	    tr->constraintVector[p->number] = tr->constraintVector[q->number];
	  }

	hookup(p,             q->back, q->z);   /* move connections to p */
	hookup(p->next,       r->back, r->z);
	hookup(p->next->next, s->back, s->z);
	if (start->number == q->number) start = start->back->back;
	q->back = r->back = s->back = (nodeptr) NULL;
      }
    else 
      {
	p->back = p->next->back = p->next->next->back = (nodeptr) NULL;
      }

    tr->rooted = FALSE;
    return  start;
  } /* uprootTree */


boolean treeReadLen (FILE *fp, tree *tr, analdef *adef)
{
  nodeptr  p;
  int      i, ch;
  boolean  is_fact;

  for (i = 1; i <= tr->mxtips; i++) 
    tr->nodep[i]->back = (node *) NULL;
    
  tr->start        = tr->nodep[1];
  tr->ntips       = 0;
  tr->nextnode    = tr->mxtips + 1;      
  tr->smoothed    = FALSE;
  tr->rooted      = FALSE;
   
  is_fact = processTreeCom(fp, tr);

  p = tr->nodep[(tr->nextnode)++]; 
  
  while(ch = treeGetCh(fp) != '(');
             
  if (! addElementLen(fp, tr, p, FALSE))                 return FALSE;
  if (! treeNeedCh(fp, ',', "in"))                return FALSE;
  if (! addElementLen(fp, tr, p->next, FALSE))           return FALSE;
  if (! tr->rooted) 
    {
      if ((ch = treeGetCh(fp)) == ',') 
	{ 
	  if (! addElementLen(fp, tr, p->next->next, FALSE)) return FALSE;	    
	}
      else 
	{                                    /*  A rooted format */
	  tr->rooted = TRUE;
	  if (ch != EOF)  (void) ungetc(ch, fp);
	}	
    }
  else 
    {
      p->next->next->back = (nodeptr) NULL;
    }
  if (! treeNeedCh(fp, ')', "in"))                return FALSE;
  (void) treeFlushLabel(fp);
  if (! treeFlushLen(fp))                         return FALSE;
  if (is_fact) 
    {
      if (! treeNeedCh(fp, ')', "at end of"))       return FALSE;
      if (! treeNeedCh(fp, '.', "at end of"))       return FALSE;
    }
  else 
    {
      if (! treeNeedCh(fp, ';', "at end of"))       return FALSE;
    }
  
  if (tr->rooted) 
    {
      p->next->next->back = (nodeptr) NULL;      
      tr->start = uprootTree(tr, p->next->next);
      if (! tr->start)                              return FALSE;
    }
  else 
    {
      tr->start = p->next->next->back;  /* This is start used by treeString */
    }
  
  if(tr->ntips < tr->mxtips)
    {

      /*printf("You provided an incomplete starting tree %d alignmnet has %d taxa\n", tr->ntips, tr->mxtips);*/

      freeNodex(tr);
      makeParsimonyTreeIncomplete(tr, adef);
      allocNodex(tr, adef); 	
    }

  return  (initrav(tr, tr->start) && initrav(tr, tr->start->back));
}








void treeReadTopologyOnly (FILE *fp, tree *tr, analdef *adef, boolean readBranches)
  { 
    nodeptr  p;
    int      i, ch;
    boolean  is_fact;

    for (i = 1; i <= tr->mxtips; i++) tr->nodep[i]->back = (node *) NULL;
    tr->start       = tr->nodep[tr->mxtips];
    tr->ntips       = 0;
    tr->nextnode    = tr->mxtips + 1;
     
    tr->smoothed    = FALSE;
    tr->rooted      = FALSE;   

    is_fact = processTreeCom(fp, tr);

    p = tr->nodep[(tr->nextnode)++]; 
    while(ch = treeGetCh(fp) != '(');
          
    if (! addElementLen(fp, tr, p, readBranches))                 exit(-1);
    if (! treeNeedCh(fp, ',', "in"))                exit(-1);
    if (! addElementLen(fp, tr, p->next, readBranches))           exit(-1);
    if (! tr->rooted) 
      {
	if ((ch = treeGetCh(fp)) == ',') 
	  { 
	    if (! addElementLen(fp, tr, p->next->next, readBranches)) exit(-1);	    
	  }
	else 
	  {                                    
	    tr->rooted = TRUE;
	    if (ch != EOF)  (void) ungetc(ch, fp);
	  }	
      }
    else 
      {
	p->next->next->back = (nodeptr) NULL;
      }
    if (! treeNeedCh(fp, ')', "in"))                exit(-1);
    (void) treeFlushLabel(fp);
    if (! treeFlushLen(fp))                         exit(-1);
    if (is_fact) 
      {
	if (! treeNeedCh(fp, ')', "at end of"))       exit(-1);
	if (! treeNeedCh(fp, '.', "at end of"))       exit(-1);
      }
    else 
      {
	if (! treeNeedCh(fp, ';', "at end of"))       exit(-1);
      }
    
    if (tr->rooted) 
      {
	p->next->next->back = (nodeptr) NULL;      
	tr->start = uprootTree(tr, p->next->next);
	if (! tr->start)                              exit(-1);
      }
    else 
      {
	tr->start = p->next->next->back;  /* This is start used by treeString */
      }

    if(tr->ntips < tr->mxtips)
      {
#ifdef DEBUG_CONSTRAINTS
	printf("You provided an incomplete starting tree %d alignmnet has %d taxa\n", tr->ntips, tr->mxtips);
#endif
	freeNodex(tr);
	makeParsimonyTreeIncomplete(tr, adef);
	allocNodex(tr, adef); 	
      }

    return;
  } 


/********************************MULTIFURCATIONS************************************************/


boolean  addElementLenMULT (FILE *fp, tree *tr, nodeptr p, int partitionCounter)
{ 
  double   z, branch;
  nodeptr  q, r, s;
  int      n, ch, fres, rn;
  double randomResolution;
  int old;
    
  tr->constraintVector[p->number] = partitionCounter;

  if ((ch = treeGetCh(fp)) == '(') 
    {
      partCount++;
      old = partCount;       
      
      n = (tr->nextnode)++;
      if (n > 2*(tr->mxtips) - 2) 
	{
	  if (tr->rooted || n > 2*(tr->mxtips) - 1) 
	    {
	      printf("ERROR: Too many internal nodes.  Is tree rooted?\n");
	      printf("       Deepest splitting should be a trifurcation.\n");
	      return FALSE;
	    }
	  else 
	    {
	      tr->rooted = TRUE;	    
	    }
	}
      q = tr->nodep[n];
      tr->constraintVector[q->number] = partCount;
      if (! addElementLenMULT(fp, tr, q->next, old))        return FALSE;
      if (! treeNeedCh(fp, ',', "in"))             return FALSE;
      if (! addElementLenMULT(fp, tr, q->next->next, old))  return FALSE;
                 
      hookup(p, q, defaultz);

      while((ch = treeGetCh(fp)) == ',')
	{ 
	  n = (tr->nextnode)++;
	  if (n > 2*(tr->mxtips) - 2) 
	    {
	      if (tr->rooted || n > 2*(tr->mxtips) - 1) 
		{
		  printf("ERROR: Too many internal nodes.  Is tree rooted?\n");
		  printf("       Deepest splitting should be a trifurcation.\n");
		  return FALSE;
		}
	      else 
		{
		  tr->rooted = TRUE;
		}
	    }
	  r = tr->nodep[n];
	  tr->constraintVector[r->number] = partCount;	  

	  rn = randomInt(10000);
	  if(rn == 0) 
	    randomResolution = 0;
	  else 
	    randomResolution = ((double)rn)/10000.0;
	   	  
#ifdef DEBUG_CONSTRAINTS
	  if(1)
#endif
#ifndef DEBUG_CONSTRAINTS
	   if(randomResolution < 0.5)
#endif
	    {	    
	      s = q->next->back;	      
	      r->back = q->next;
	      q->next->back = r;	      
	      r->next->back = s;
	      s->back = r->next;	      
	      addElementLenMULT(fp, tr, r->next->next, old);	     
	    }
	  else
	    {	  
	      s = q->next->next->back;	      
	      r->back = q->next->next;
	      q->next->next->back = r;	      
	      r->next->back = s;
	      s->back = r->next;	      
	      addElementLenMULT(fp, tr, r->next->next, old);	     
	    }	    	  	  
	}       

      if(ch != ')')
	{
	  printf("Missing /) in treeReadLenMULT\n");
	  exit(-1);	        
	}
	


      (void) treeFlushLabel(fp);
    }
  else 
    {                             
      ungetc(ch, fp);
      if ((n = treeFindTipName(fp, tr)) <= 0)          return FALSE;
      q = tr->nodep[n];      
      tr->constraintVector[q->number] = partitionCounter;
#ifdef DEBUG_CONSTRAINTS
      printf("%s\n", tr->nameList[q->number]);
#endif
      if (tr->start->number > n)  tr->start = q;
      (tr->ntips)++;
      hookup(p, q, defaultz);
    }
  
  fres = treeFlushLen(fp);
  if(!fres) return FALSE;
    
  return TRUE;          
} 





boolean treeReadLenMULT (FILE *fp, tree *tr, analdef *adef)
{
  nodeptr  p, r, s;
  int      i, ch, n, rn;
  int partitionCounter = 0;
  double randomResolution;

  srand((unsigned int) time(NULL));
  tr->constraintVector = (int *)malloc((2 * tr->mxtips) * sizeof(int));
  for(i = 0; i < 2 * tr->mxtips; i++)
    tr->constraintVector[i] = -1;

  for (i = 1; i <= tr->mxtips; i++) tr->nodep[i]->back = (node *) NULL;
  tr->start       = tr->nodep[tr->mxtips];
  tr->ntips       = 0;
  tr->nextnode    = tr->mxtips + 1;
 
  tr->smoothed    = FALSE;
  tr->rooted      = FALSE;
 
  p = tr->nodep[(tr->nextnode)++]; 
  while(ch = treeGetCh(fp) != '(');
      
  if (! addElementLenMULT(fp, tr, p, partitionCounter))                 return FALSE;
  if (! treeNeedCh(fp, ',', "in"))                return FALSE;
  if (! addElementLenMULT(fp, tr, p->next, partitionCounter))           return FALSE;
  if (! tr->rooted) 
    {
      if ((ch = treeGetCh(fp)) == ',') 
	{       
	  if (! addElementLenMULT(fp, tr, p->next->next, partitionCounter)) return FALSE;

	  while((ch = treeGetCh(fp)) == ',')
	    { 
	      n = (tr->nextnode)++;
	      if (n > 2*(tr->mxtips) - 2) 
		{
		  printf("error shoud not get here\n");
		  exit(-1);
		  tr->rooted = TRUE;		   
		}

	      r = tr->nodep[n];	
	      tr->constraintVector[r->number] = partitionCounter;	   
	      
	      rn = randomInt(10000);
	      if(rn == 0) 
		randomResolution = 0;
	      else 
		randomResolution = ((double)rn)/10000.0;
	      
#ifdef DEBUG_CONSTRAINTS
	      if(1);
#endif
#ifndef DEBUG_CONSTRAINTS
	      if(randomResolution < 0.5)
#endif
		{	
		  s = p->next->next->back;		  
		  r->back = p->next->next;
		  p->next->next->back = r;		  
		  r->next->back = s;
		  s->back = r->next;		  
		  addElementLenMULT(fp, tr, r->next->next, partitionCounter);	
		}
	      else
		{
		  s = p->next->back;		  
		  r->back = p->next;
		  p->next->back = r;		  
		  r->next->back = s;
		  s->back = r->next;		  
		  addElementLenMULT(fp, tr, r->next->next, partitionCounter);
		}
	    }	  	  	      	  

	  if(ch != ')')
	    {
	      printf("Missing /) in treeReadLenMULT\n");
	      exit(-1);	        	      	      
	    }
	  else
	    ungetc(ch, fp);
	}
      else 
	{ 
	  tr->rooted = TRUE;
	  if (ch != EOF)  (void) ungetc(ch, fp);
	}       
    }
  else 
    {
      p->next->next->back = (nodeptr) NULL;
    }
    
  if (! treeNeedCh(fp, ')', "in"))                return FALSE;
  (void) treeFlushLabel(fp);
  if (! treeFlushLen(fp))                         return FALSE;
   
  if (! treeNeedCh(fp, ';', "at end of"))       return FALSE;
  

  if (tr->rooted) 
    {   
      p->next->next->back = (nodeptr) NULL;
      tr->start = uprootTree(tr, p->next->next);
      if (! tr->start)                              return FALSE;
    }
  else 
    {
      tr->start = p->next->next->back;  /* This is start used by treeString */
    }

  
  

  if(tr->ntips < tr->mxtips)
    {
#ifdef DEBUG_CONSTRAINTS
      printf("You provided an incomplete multifurcating constraint tree %d alignmnet has %d taxa\n", tr->ntips, tr->mxtips);
#endif
      freeNodex(tr);
      makeParsimonyTreeIncomplete(tr, adef);
      allocNodex(tr, adef);     
   }

  return (initrav(tr, tr->start) && initrav(tr, tr->start->back));  
}











/*=======================================================================*/
/*                        Read a tree from a string                      */
/*=======================================================================*/

int str_treeFinishCom (char **treestrp, char **strp)
                      /* treestrp -- tree string pointer */
                      /* strp -- comment string pointer */
  { /* str_treeFinishCom */
    int  ch;
    while ( (ch=*(*treestrp)++)!='\0' && ch!=']' ) {
      if (strp != NULL) *(*strp)++ = ch;    /* save character  */
      if (ch == '[') {                      /* nested comment; find its end */
        if ((ch=str_treeFinishCom(treestrp,strp)) == '\0')  break;
        if (strp != NULL) *(*strp)++ = ch;  /* save closing ]  */
        }
      }
    if (strp != NULL) **strp = '\0';        /* terminate string  */
    return  ch;
  } /* str_treeFinishCom */


int str_treeGetCh (char **treestrp)
    /* get next nonblank, noncomment character */
  { /* str_treeGetCh */
    int  ch;
    while ((ch=*(*treestrp)++) != '\0') {
      if (whitechar(ch)) ;
      else if (ch == '[') {                  /* comment; find its end */
        if ((ch = str_treeFinishCom(treestrp,(char**)NULL)) == '\0')  break;
        }
      else  break;
      }

    return  ch;
  } /* str_treeGetCh */




boolean  str_treeGetLabel (char **treestrp, char *lblPtr, int maxlen)
  { /* str_treeGetLabel */
    int      ch;
    boolean  done, quoted, lblfound;


    if (--maxlen < 0)
      lblPtr = (char*)NULL;  /* reserves space for '\0' */
    else if(lblPtr == NULL)
      maxlen = 0;

    ch = *(*treestrp)++;
    done = treeLabelEnd(ch);

    lblfound = !done;
    quoted = (ch == '\'');
    if (quoted && ! done) {
      ch = *(*treestrp)++;
      done = (ch == '\0');
    }

    while(!done) {
      if (quoted) 
	{
	  if (ch == '\'') 
	    {
	      ch = *(*treestrp)++;
	      if (ch != '\'') break;
	    }
	}
      else 
	if (treeLabelEnd(ch))
	  break;
      /*else 
	  if (ch == '_')
	    ch = ' ';*/
      if (--maxlen >= 0) *lblPtr++ = ch;
      ch = *(*treestrp)++;
      if (ch == '\0') break;
    }

    (*treestrp)--;

    if (lblPtr != NULL) *lblPtr = '\0';

    return lblfound;
  } /* str_treeGetLabel */


boolean  str_treeFlushLabel (char **treestrp)
  { /* str_treeFlushLabel */
    return  str_treeGetLabel(treestrp, (char*)NULL, (int)0);
  } /* str_treeFlushLabel */


int  str_treeFindTipName (char **treestrp, tree *tr)         /*DKB-orig*/
/*int  str_treeFindTipName (char **treestrp, tree *tr, int ch)*/   /*DKB-change*/
  { /* str_treeFindTipName */
    nodeptr  q;
    char    *nameptr, str[nmlngth+2];
    int      i, n;

    if (tr->prelabeled) {
      if (str_treeGetLabel(treestrp, str, nmlngth+2)) {
        n = treeFindTipByLabel(str, tr);
      }
      else
        n = 0;
      }

    else if (tr->ntips < tr->mxtips) {
      n = tr->ntips + 1;
     
      nameptr = tr->nameList[n];
    
      if (! str_treeGetLabel(treestrp, nameptr, nmlngth+1)) n = 0;
      }

    else {
      n = 0;
      }

    return  n;
  } /* str_treeFindTipName */


boolean str_treeProcessLength (char **treestrp, double *dptr)
  { /* str_treeProcessLength */
    int     used;

    if(!str_treeGetCh(treestrp))  return FALSE;    /*  Skip comments */
    (*treestrp)--;

    if (sscanf(*treestrp, "%lf%n", dptr, &used) != 1) 
      {
	printf("ERROR: str_treeProcessLength: Problem reading branch length\n");
	printf("%40s\n", *treestrp);
	*dptr = 0.0;
	return FALSE;
      }
    else 
      {
	*treestrp += used;
      }

    return  TRUE;
  } /* str_treeProcessLength */


boolean  str_treeFlushLen (char **treestrp)
  { /* str_treeFlushLen */
    int  ch;
    double  x;

    if ((ch = str_treeGetCh(treestrp)) == ':')
    /*return str_treeProcessLength(treestrp, (double*)NULL);*/   /*DKB-orig*/
      return str_treeProcessLength(treestrp, &x);              /*DKB-change*/
    else {
      (*treestrp)--;
      return TRUE;
      }
  } /* str_treeFlushLen */


boolean  str_treeNeedCh (char **treestrp, int c1, char *where)
  { /* str_treeNeedCh */
    int  c2, i;

    if ((c2 = str_treeGetCh(treestrp)) == c1)  return TRUE;

    printf("ERROR: Missing '%c' %s tree; ", c1, where);
    if (c2 == '\0') 
      printf("end-of-string");
    else {
      putchar('"');
      for (i = 24; i-- && (c2 != '\0'); c2 = *(*treestrp)++)  putchar(c2);
      putchar('"');
      }

    printf(" found instead\n");
    return FALSE;
  } /* str_treeNeedCh */


boolean str_processTreeCom(tree *tr, char **treestrp)
  { /* str_processTreeCom */
    char  *com, *com_end;
    int  text_started, functor_read, com_open;

    com = *treestrp;

    /* Comment must begin with either "phylip_tree" or "pseudoNewick".
     * If it is neither, return FALSE. */
    functor_read = text_started = 0;
    sscanf(com, " p%nhylip_tree(%n", &text_started, &functor_read);
    if (functor_read) {
      com += functor_read;
    }
    else if (text_started) {
      com += text_started;
      sscanf(com, "seudoNewick(%n", &functor_read);
      if (! functor_read) {
        printf("Start of tree 'p...' not understood.\n");
        return  FALSE;
      }
      else {
        com += functor_read;
      }
    }

    /* Find opening bracket of comment */
    com_open = 0;
    sscanf(com, " [%n", &com_open);
    com += com_open;

    /* Read comment. */
    if (com_open) {
      if (!(com_end = strchr(com, ']'))) {
        printf("Missing end of tree comment.\n");
        return  FALSE;
      }
      *com_end = 0;
      (void)readKeyValue(com,likelihood_key,"%lg",(void*)&(tr->likelihood));
     
      (void)readKeyValue(com,smoothed_key,  "%d", (void*)&(tr->smoothed));
      *com_end = ']';
      com_end++;

      /* Remove trailing comma, and return addr of next char in treestp */
      if (functor_read) {
        text_started = 0;
        sscanf(com_end, " ,%n", & text_started);
        com_end += text_started;
      }
      *treestrp = com_end;
    }
    return (functor_read > 0);
  } /* str_processTreeCom */

boolean str_processTreeComMerge(int *ntaxa, char **treestrp)
  { /* str_processTreeCom */
    char  *com, *com_end;
    int  text_started, functor_read, com_open;

    com = *treestrp;

    /* Comment must begin with either "phylip_tree" or "pseudoNewick".
     * If it is neither, return FALSE. */
    functor_read = text_started = 0;
    sscanf(com, " p%nhylip_tree(%n", &text_started, &functor_read);
    if (functor_read) {
      com += functor_read;
    }
    else if (text_started) {
      com += text_started;
      sscanf(com, "seudoNewick(%n", &functor_read);
      if (! functor_read) {
        printf("Start of tree 'p...' not understood.\n");
        return  FALSE;
      }
      else {
        com += functor_read;
      }
    }

    /* Find opening bracket of comment */
    com_open = 0;
    sscanf(com, " [%n", &com_open);
    com += com_open;

    /* Read comment. */
    if (com_open) {
      if (!(com_end = strchr(com, ']'))) {
        printf("Missing end of tree comment.\n");
        return  FALSE;
      }
      *com_end = 0;
      /*(void)readKeyValue(com,likelihood_key,"%lg",(void*)&(tr->likelihood));*/
      (void)readKeyValue(com,ntaxa_key, "%d", (void *)ntaxa);   
      /*     
	      (void)readKeyValue(com,smoothed_key,  "%d", (void*)&(tr->smoothed));*/
      *com_end = ']';
      com_end++;

      /* Remove trailing comma, and return addr of next char in treestp */
      if (functor_read) {
        text_started = 0;
        sscanf(com_end, " ,%n", & text_started);
        com_end += text_started;
      }
      *treestrp = com_end;
    }
    return (functor_read > 0);
  } /* str_processTreeCom */


boolean  str_addElementLen (char **treestrp, tree *tr, nodeptr p)
  { /* str_addElementLen */
    double   z, branch;
    nodeptr  q;
    int      n, ch;

    if ((ch = str_treeGetCh(treestrp)) == '(') 
      {
	n = (tr->nextnode)++;
	if (n > 2*(tr->mxtips) - 2) {
        if (tr->rooted || n > 2*(tr->mxtips) - 1) {
          printf("ERROR: too many internal nodes.  Is tree rooted?\n");
          printf("Deepest splitting should be a trifurcation.\n");
          return  FALSE;
        }
        else {
          tr->rooted = TRUE;
        }
      }
      q = tr->nodep[n];
      if (! str_addElementLen(treestrp, tr, q->next))          return FALSE;
      if (! str_treeNeedCh(treestrp, ',', "in"))               return FALSE;
      if (! str_addElementLen(treestrp, tr, q->next->next))    return FALSE;
      if (! str_treeNeedCh(treestrp, ')', "in"))               return FALSE;
    /*if (! str_treeFlushLabel(treestrp))                      return FALSE;*//*DKB-orig*/
    }

    else {
      (*treestrp)--;                 
      if((n = str_treeFindTipName(treestrp, tr)) <= 0) return FALSE;
      q = tr->nodep[n];
      if (tr->start->number > n)  tr->start = q;
      (tr->ntips)++;
    }

    /*  Master and Slave always use lengths */

    /*if (! str_treeNeedCh(treestrp, ':', "in"))                 return FALSE;
      if (! str_treeProcessLength(treestrp, &branch))            return FALSE;*/
   
    hookup(p, q, defaultz);

    return  TRUE;
  }




boolean str_treeReadLen (char *treestr, tree *tr)
{
  nodeptr  p;
  int  i;
  boolean  is_fact, found;

  for(i=1; i<=(tr->mxtips); i++) 
    tr->nodep[i]->back = (node*)NULL;
  tr->start       = tr->nodep[tr->mxtips];
  tr->ntips       = 0;
  tr->nextnode    = tr->mxtips + 1;
  
  tr->smoothed    = 1;
  tr->rooted      = FALSE;

  is_fact = str_processTreeCom(tr,&treestr);
  
  p = tr->nodep[(tr->nextnode)++];

  if(!str_treeNeedCh(&treestr, '(', "at start of"))       return FALSE;
  if(!str_addElementLen(&treestr, tr, p))                 return FALSE;
  if(!str_treeNeedCh(&treestr, ',', "in"))                return FALSE;
  if(!str_addElementLen(&treestr, tr, p->next))           return FALSE;
  if(!tr->rooted) 
    {
      if(str_treeGetCh(&treestr) == ',') 
	{   
	  if(!str_addElementLen(&treestr,tr,p->next->next)) return FALSE;
	}
      else 
	{                    
	  p->next->next->back = (nodeptr) NULL;
	  tr->rooted = TRUE;
	  treestr--;
	}
    }
  if(!str_treeNeedCh(&treestr, ')', "in"))                 return FALSE;
  if(!str_treeFlushLen(&treestr))                          return FALSE;
  if(is_fact) 
    {
      if(!str_treeNeedCh(& treestr, ')', "at end of"))       return FALSE;
      if(!str_treeNeedCh(& treestr, '.', "at end of"))       return FALSE;
    }
  else 
    {
      if(!str_treeNeedCh(& treestr, ';', "at end of"))       return FALSE;
    }
  
  if(tr->rooted)  if (! uprootTree(tr, p->next->next))     return FALSE;
  tr->start = p->next->next->back;
  
  return  TRUE;
} 

double str_readTreeLikelihood (char *treestr)
  { /* str_readTreeLikelihood */
    double lk1;
    char    *com, *com_end;
    boolean  readKeyValue();

    if ((com = strchr(treestr, '[')) /*&& (com < strchr(treestr, '('))*/
                                     && (com_end = strchr(com, ']'))) 
      {
      com++;
      *com_end = 0;
      if (readKeyValue(com, likelihood_key, "%lg", (void *) &(lk1))) {
        *com_end = ']';
        return lk1;
        }
      }

    printf( "ERROR reading likelihood in receiveTree\n");
    printf("%s \n", treestr);
    return  badEval;
  } /* str_readTreeLikelihood */

void getStartingTree(tree *tr, analdef *adef)
{
  tr->likelihood = unlikely;
  
  if(adef->restart) 
    {	 	
      allocNodex(tr, adef); 	    

      INFILE = fopen(tree_file, "r");	
      if (!INFILE)
	{
	  printf( "Could not open input tree: %s\n", tree_file);
	  exit(-1);
	}              	
	
      if(!adef->grouping)
	{
	  if (! treeReadLen(INFILE, tr, adef))
	    exit(-1);
	}
      else
	{
	  partCount = 0;
	  if (! treeReadLenMULT(INFILE, tr, adef))
	    exit(-1);
	}            
      
      
      treeEvaluate(tr, 1);        	           

      fclose(INFILE);
    }
  else
    { 
      if(adef->randomStartingTree)	  
	makeRandomTree(tr, adef);       	   	 	   	  
      else			 
	makeParsimonyTree(tr, adef);	   	    	      		      


      if(adef->startingTreeOnly)
	{
	  printStartingTree(tr, adef, TRUE);
	  exit(0);
	}
      else   	         
	printStartingTree(tr, adef, FALSE);
     	         
      allocNodex(tr, adef);                 
      initrav(tr, tr->start);     
      initrav(tr, tr->start->back);
      treeEvaluate(tr, 1);                   
    }         

  tr->start = tr->nodep[1];
}
