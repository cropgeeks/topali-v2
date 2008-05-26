
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

/*topolRELL *initTopolRELL(tree *tr)
{
  topolRELL *tpl;
  
  tpl = (topolRELL *)malloc(sizeof(topolRELL));

  tpl->connect = (connectRELL *)malloc((2 * tr->mxtips - 3) * sizeof(connectRELL));
  tpl->likelihood = unlikely;
  return tpl;				       
}

void saveTopolRELLRec(nodeptr p, topolRELL *tpl, int *i)
{
  if(p->tip)
    return;
  else
    {
      nodeptr q = p->next;      
      while(q != p)
	{	  
	   tpl->connect[*i].p = q;
	   tpl->connect[*i].q = q->back;       
	   tpl->connect[*i].z = q->z;
	   *i = *i + 1;
	   
	   saveTopolRELLRec(q->back, tpl, i);
	   q = q->next;
	}

    }

}

void saveTopolRELL(tree *tr, topolRELL *tpl)
{
  nodeptr p = tr->start;
  int i = 0;

  tpl->likelihood = tr->likelihood;
  tpl->start      = 1;

  tpl->connect[i].p = p;
  tpl->connect[i].q = p->back;
  tpl->connect[i].z = p->z;
  i++;

  saveTopolRELLRec(p->back, tpl, &i);
  
}


void restoreTopolRELL(tree *tr, topolRELL *tpl)
{
  int i;
  nodeptr p, p0;
  for (i = 1; i <= 2*(tr->mxtips) - 2; i++) {
    p0 = p = tr->nodep[i];
    do {
      p->back = (nodeptr) NULL;
      p = p->next;
    } while (p != p0);
  }
  
  for (i = 0; i < 2 * tr->mxtips - 3; i++)     
    hookup(tpl->connect[i].p, tpl->connect[i].q, tpl->connect[i].z);    
  
  tr->likelihood = tpl->likelihood;
  tr->start      = tr->nodep[tpl->start];
}

void initTL(topolRELL_LIST *rl, tree *tr, int n)
{
  int i;

  rl->max = n;
  rl->members = 0;
  rl->t = (topolRELL **)malloc(sizeof(topolRELL *) * n);

  for(i = 0; i < n; i++)
    {
      rl->t[i] = (topolRELL *)malloc(sizeof(topolRELL));

      rl->t[i]->connect = (connectRELL *)malloc((2 * tr->mxtips - 3) * sizeof(connectRELL));
      rl->t[i]->likelihood = unlikely;
    }
}


void restoreTL(topolRELL_LIST *rl, tree *tr, int n)
{
  if(n < 0 || n >= rl->members)
    {
      printf("FATAL ERROR restoreTL\n");
      exit(1);
    }

  restoreTopolRELL(tr, rl->t[n]);
}

void resetTL(topolRELL_LIST *rl)
{
  int i;

  for(i = 0; i < rl->max; i++)
    rl->t[i]->likelihood = unlikely;     
}


void saveTL(topolRELL_LIST *rl, tree *tr)
{
  if(rl->members < rl->max)
    {
      saveTopolRELL(tr, rl->tpl[rl->members]);
      rl->members++;      
    }
  else
    {
      int i, found = 0;
      for(i = 0; i < rl->max && !found; i++)
	{
	  if(tr->likelihood > rl->tpl[i]->likelihood)
	    {
	      saveTopolRELL(tr, rl->tpl[i]);
	      found = 1;
	    }
	}      
    }
}

*/



void  *tipValPtr (nodeptr p)
{ 
  return  (void *) & p->number;
}


int  cmpTipVal (void *v1, void *v2)
  { /* cmpTipVal */
    int  i1, i2;

    i1 = *((int *) v1);
    i2 = *((int *) v2);
    return  (i1 < i2) ? -1 : ((i1 == i2) ? 0 : 1);
  } /* cmpTipVal */





/*  These are the only routines that need to UNDERSTAND topologies */

topol  *setupTopol (int maxtips, int nsites)
  { /* setupTopol */
    topol   *tpl;

    if (! (tpl = (topol *) malloc(sizeof(topol))) || 
        ! (tpl->links = (connptr) malloc((2*maxtips-3) * sizeof(connect))))
      {
	printf("ERROR: Unable to get topology memory");
	tpl = (topol *) NULL;
      }
    else 
      {
	tpl->likelihood  = unlikely;
	tpl->start       = (node *) NULL;
	tpl->nextlink    = 0;
	tpl->ntips       = 0;
	tpl->nextnode    = 0;    
	tpl->scrNum      = 0;     /* position in sorted list of scores */
	tpl->tplNum      = 0;     /* position in sorted list of trees */
	tpl->prelabeled  = TRUE;
	tpl->smoothed    = FALSE; /* branch optimization converged? */
      }

    return  tpl;
  } /* setupTopol */


void  freeTopol (topol *tpl)
  { /* freeTopol */
    free(tpl->links);
    free(tpl);
  } /* freeTopol */


int  saveSubtree (nodeptr p, topol *tpl)
    /*  Save a subtree in a standard order so that earlier branches
     *  from a node contain lower value tips than do second branches from
     *  the node.  This code works with arbitrary furcations in the tree.
     */
  { /* saveSubtree */
    connptr  r, r0;
    nodeptr  q, s;
    int      t, t0, t1;

    r0 = tpl->links;
    r = r0 + (tpl->nextlink)++;
    r->p = p;
    r->q = q = p->back;
    r->z = p->z;
    r->descend = 0;                     /* No children (yet) */

    if (q->tip) {
      r->valptr = tipValPtr(q);         /* Assign value */
      }

    else {                              /* Internal node, look at children */
      s = q->next;                      /* First child */
      do {
        t = saveSubtree(s, tpl);        /* Generate child's subtree */

        t0 = 0;                         /* Merge child into list */
        t1 = r->descend;
        while (t1 && (cmpTipVal(r0[t1].valptr, r0[t].valptr) < 0)) {
          t0 = t1;
          t1 = r0[t1].sibling;
          }
        if (t0) r0[t0].sibling = t;  else  r->descend = t;
        r0[t].sibling = t1;

        s = s->next;                    /* Next child */
        } while (s != q);

      r->valptr = r0[r->descend].valptr;   /* Inherit first child's value */
      }                                 /* End of internal node processing */

    return  r - r0;
  } /* saveSubtree */


nodeptr  minSubtreeTip (nodeptr  p0)
  { /* minTreeTip */
    nodeptr  minTip, p, testTip;

    if (p0->tip) return p0;

    p = p0->next;
    minTip = minSubtreeTip(p->back);
    while ((p = p->next) != p0) {
      testTip = minSubtreeTip(p->back);
      if (cmpTipVal(tipValPtr(testTip), tipValPtr(minTip)) < 0)
        minTip = testTip;
      }
    return minTip;
  } /* minTreeTip */


nodeptr  minTreeTip (nodeptr  p)
  { /* minTreeTip */
    nodeptr  minp, minpb;

    minp  = minSubtreeTip(p);
    minpb = minSubtreeTip(p->back);
    return cmpTipVal(tipValPtr(minp), tipValPtr(minpb)) < 0 ? minp : minpb;
  } /* minTreeTip */


void saveTree (tree *tr, topol *tpl)
    /*  Save a tree topology in a standard order so that first branches
     *  from a node contain lower value tips than do second branches from
     *  the node.  The root tip should have the lowest value of all.
     */
  { /* saveTree */
    connptr  r;
    int  i;

    tpl->nextlink = 0;                             /* Reset link pointer */
    r = tpl->links + saveSubtree(minTreeTip(tr->start), tpl);  /* Save tree */
    r->sibling = 0;

    tpl->likelihood = tr->likelihood;
    tpl->start      = tr->start;
    tpl->ntips      = tr->ntips;
    tpl->nextnode   = tr->nextnode; 
    tpl->prelabeled = tr->prelabeled;
    tpl->smoothed   = tr->smoothed;   
   
  } /* saveTree */


void copyTopol (topol *tpl1, topol *tpl2)
  { /* copyTopol */
    connptr  r1, r2, r10, r20;
   
    int  i;

    r10 = tpl1->links;
    r20 = tpl2->links;
    tpl2->nextlink = tpl1->nextlink; 

    r1 = r10;
    r2 = r20;
    i = 2 * tpl1->ntips - 3;
    while (--i >= 0) {
      r2->z = r1->z;
      r2->p = r1->p;
      r2->q = r1->q;
      r2->valptr = r1->valptr;
      r2->descend = r1->descend; 
      r2->sibling = r1->sibling; 
      r1++;
      r2++;
      }

    
    tpl2->likelihood = tpl1->likelihood;
    tpl2->start      = tpl1->start;
    tpl2->ntips      = tpl1->ntips;
    tpl2->nextnode   = tpl1->nextnode;   
    tpl2->prelabeled = tpl1->prelabeled;
    tpl2->scrNum     = tpl1->scrNum;
    tpl2->tplNum     = tpl1->tplNum;
    tpl2->smoothed   = tpl1->smoothed;
  } /* copyTopol */


boolean restoreTreeRecursive (topol *tpl, tree *tr)
  { /* restoreTree */
    void  hookup();
    boolean  initrav();

    connptr  r;
    nodeptr  p, p0;
    
    int  i;



/*  Copy connections from topology */

    for (r = tpl->links, i = 0; i < tpl->nextlink; r++, i++) {
      hookup(r->p, r->q, r->z);
      }

    tr->likelihood = tpl->likelihood;
    tr->start      = tpl->start;
    tr->ntips      = tpl->ntips;
    
    tr->nextnode   = tpl->nextnode;  
    tr->prelabeled = tpl->prelabeled;
    tr->smoothed   = tpl->smoothed;
 
   
    return TRUE;
    
  } /* restoreTree */




boolean restoreTree (topol *tpl, tree *tr)
  { /* restoreTree */
    /*void  hookup();
      boolean  initrav();*/

    connptr  r;
    nodeptr  p, p0;
    
    int  i;

/*  Clear existing connections */

    for (i = 1; i <= 2*(tr->mxtips) - 2; i++) {  /* Uses p = p->next at tip */
      p0 = p = tr->nodep[i];
      do {
        p->back = (nodeptr) NULL;
        p = p->next;
        } while (p != p0);
      }

/*  Copy connections from topology */

    for (r = tpl->links, i = 0; i < tpl->nextlink; r++, i++) {
      hookup(r->p, r->q, r->z);
      }

    tr->likelihood = tpl->likelihood;
    tr->start      = tpl->start;
    tr->ntips      = tpl->ntips;
    
    tr->nextnode   = tpl->nextnode;   
    tr->prelabeled = tpl->prelabeled;
    tr->smoothed   = tpl->smoothed;
 

   

    return (initrav(tr, tr->start) && initrav(tr, tr->start->back));
  } /* restoreTree */


boolean restoreTopology (topol *tpl, tree *tr)
  { 

    connptr  r;
    nodeptr  p, p0;    
    int  i;    

    for (r = tpl->links, i = 0; i < tpl->nextlink; r++, i++) 
      {
	hookup(r->p, r->q, r->z);
      }

    tr->likelihood = tpl->likelihood;
    tr->start      = tpl->start;
    tr->ntips      = tpl->ntips;
    
    tr->nextnode   = tpl->nextnode;
    tr->prelabeled = tpl->prelabeled;
    tr->smoothed   = tpl->smoothed;
    
    return (initrav(tr, tr->start) && initrav(tr, tr->start->back));
  } /* restoreTree */


int initBestTree (bestlist *bt, int newkeep, int numsp, int sites)
  { /* initBestTree */
    int  i, nlogf;


    bt->nkeep = 0;

    if (bt->ninit <= 0) {
      if (! (bt->start = setupTopol(numsp, sites)))  return  0;
      bt->ninit = -1;
      bt->nvalid = 0;
      bt->numtrees = 0;
      bt->best = unlikely;
      bt->improved = FALSE;
      bt->byScore = (topol **) malloc((newkeep+1) * sizeof(topol *));
      bt->byTopol = (topol **) malloc((newkeep+1) * sizeof(topol *));
      if (! bt->byScore || ! bt->byTopol) {
        printf( "initBestTree: malloc failure\n");
        return 0;
        }
      }
    else if (ABS(newkeep) > bt->ninit) {
      if (newkeep <  0) newkeep = -(bt->ninit);
      else newkeep = bt->ninit;
      }

    if (newkeep < 1) {    /*  Use negative newkeep to clear list  */
      newkeep = -newkeep;
      if (newkeep < 1) newkeep = 1;
      bt->nvalid = 0;
      bt->best = unlikely;
      }

    if (bt->nvalid >= newkeep) {
      bt->nvalid = newkeep;
      bt->worst = bt->byScore[newkeep]->likelihood;
      }
    else 
      {
	bt->worst = unlikely;
      }

    for (i = bt->ninit + 1; i <= newkeep; i++) 
      {    
	if (! (bt->byScore[i] = setupTopol(numsp, 0)))  break;
	bt->byTopol[i] = bt->byScore[i];
	bt->ninit = i;
      }

    return  (bt->nkeep = MIN(newkeep, bt->ninit));
  } /* initBestTree */



void resetBestTree (bestlist *bt)
  { /* resetBestTree */
    bt->best     = unlikely;
    bt->worst    = unlikely;
    bt->nvalid   = 0;
    bt->improved = FALSE;
  } /* resetBestTree */


boolean  freeBestTree(bestlist *bt)
  { /* freeBestTree */
    while (bt->ninit >= 0)  freeTopol(bt->byScore[(bt->ninit)--]);
    freeTopol(bt->start);
    return TRUE;
  } /* freeBestTree */


/*  Compare two trees, assuming that each is in standard order.  Return
 *  -1 if first preceeds second, 0 if they are identical, or +1 if first
 *  follows second in standard order.  Lower number tips preceed higher
 *  number tips.  A tip preceeds a corresponding internal node.  Internal
 *  nodes are ranked by their lowest number tip.
 */

int  cmpSubtopol (connptr p10, connptr p1, connptr p20, connptr p2)
  { /* cmpSubtopol */
    connptr  p1d, p2d;
    int  cmp;

    if (! p1->descend && ! p2->descend)          /* Two tips */
      return cmpTipVal(p1->valptr, p2->valptr);

    if (! p1->descend) return -1;                /* p1 = tip, p2 = node */
    if (! p2->descend) return  1;                /* p2 = tip, p1 = node */

    p1d = p10 + p1->descend;
    p2d = p20 + p2->descend;
    while (1) {                                  /* Two nodes */
      if (cmp = cmpSubtopol(p10, p1d, p20, p2d))  return cmp; /* Subtrees */
      if (! p1d->sibling && ! p2d->sibling)  return  0; /* Lists done */
      if (! p1d->sibling) return -1;             /* One done, other not */
      if (! p2d->sibling) return  1;             /* One done, other not */
      p1d = p10 + p1d->sibling;                  /* Neither done */
      p2d = p20 + p2d->sibling;
      }
  } /* cmpSubtopol */



int  cmpTopol (void *tpl1, void *tpl2)
  { /* cmpTopol */
    connptr  r1, r2;
    int      cmp;

    r1 = ((topol *) tpl1)->links;
    r2 = ((topol *) tpl2)->links;
    cmp = cmpTipVal(tipValPtr(r1->p), tipValPtr(r2->p));
    if (cmp) return cmp;
    return  cmpSubtopol(r1, r1, r2, r2);
  } /* cmpTopol */



int  cmpTplScore (void *tpl1, void *tpl2)
  { /* cmpTplScore */
    double  l1, l2;

    l1 = ((topol *) tpl1)->likelihood;
    l2 = ((topol *) tpl2)->likelihood;
    return  (l1 > l2) ? -1 : ((l1 == l2) ? 0 : 1);
  } /* cmpTplScore */



/*  Find an item in a sorted list of n items.  If the item is in the list,
 *  return its index.  If it is not in the list, return the negative of the
 *  position into which it should be inserted.
 */

int  findInList (void *item, void *list[], int n, int (* cmpFunc)())
  { /* findInList */
    int  mid, hi, lo, cmp;

    if (n < 1) return  -1;                    /*  No match; first index  */

    lo = 1;
    mid = 0;
    hi = n;
    while (lo < hi) {
      mid = (lo + hi) >> 1;
      cmp = (* cmpFunc)(item, list[mid-1]);
      if (cmp) {
        if (cmp < 0) hi = mid;
        else lo = mid + 1;
        }
      else  return  mid;                        /*  Exact match  */
      }

    if (lo != mid) {
       cmp = (* cmpFunc)(item, list[lo-1]);
       if (cmp == 0) return lo;
       }
    if (cmp > 0) lo++;                         /*  Result of step = 0 test  */
    return  -lo;
  } /* findInList */



int  findTreeInList (bestlist *bt, tree *tr)
  { /* findTreeInList */
    topol  *tpl;

    tpl = bt->byScore[0];
    saveTree(tr, tpl);
    return  findInList((void *) tpl, (void **) (& (bt->byTopol[1])),
                       bt->nvalid, cmpTopol);
  } /* findTreeInList */


int  saveBestTree (bestlist *bt, tree *tr)
  { /* saveBestTree */
   
    topol  *tpl, *reuse;
    int  tplNum, scrNum, reuseScrNum, reuseTplNum, i, oldValid, newValid;

    tplNum = findTreeInList(bt, tr);
    tpl = bt->byScore[0];
    oldValid = newValid = bt->nvalid;

    if (tplNum > 0) {                      /* Topology is in list  */
      reuse = bt->byTopol[tplNum];         /* Matching topol  */
      reuseScrNum = reuse->scrNum;
      reuseTplNum = reuse->tplNum;
      }
                                           /* Good enough to keep? */
    else if (tr->likelihood < bt->worst)  return 0;

    else {                                 /* Topology is not in list */
      tplNum = -tplNum;                    /* Add to list (not replace) */
      if (newValid < bt->nkeep) bt->nvalid = ++newValid;
      reuseScrNum = newValid;              /* Take worst tree */
      reuse = bt->byScore[reuseScrNum];
      reuseTplNum = (newValid > oldValid) ? newValid : reuse->tplNum;
      if (tr->likelihood > bt->start->likelihood) bt->improved = TRUE;
      }

    scrNum = findInList((void *) tpl, (void **) (& (bt->byScore[1])),
                         oldValid, cmpTplScore);
    scrNum = ABS(scrNum);

    if (scrNum < reuseScrNum)
      for (i = reuseScrNum; i > scrNum; i--)
        (bt->byScore[i] = bt->byScore[i-1])->scrNum = i;

    else if (scrNum > reuseScrNum) {
      scrNum--;
      for (i = reuseScrNum; i < scrNum; i++)
        (bt->byScore[i] = bt->byScore[i+1])->scrNum = i;
      }

    if (tplNum < reuseTplNum)
      for (i = reuseTplNum; i > tplNum; i--)
        (bt->byTopol[i] = bt->byTopol[i-1])->tplNum = i;

    else if (tplNum > reuseTplNum) {
      tplNum--;
      for (i = reuseTplNum; i < tplNum; i++)
        (bt->byTopol[i] = bt->byTopol[i+1])->tplNum = i;
      }

   

    tpl->scrNum = scrNum;
    tpl->tplNum = tplNum;
    bt->byTopol[tplNum] = bt->byScore[scrNum] = tpl;
    bt->byScore[0] = reuse;

    if (scrNum == 1)  bt->best = tr->likelihood;
    if (newValid == bt->nkeep) bt->worst = bt->byScore[newValid]->likelihood;

    return  scrNum;
  } /* saveBestTree */




int  recallBestTreeRecursive (bestlist *bt, int rank, tree *tr)
  { /* recallBestTree */
    if (rank < 1)  rank = 1;
    if (rank > bt->nvalid)  rank = bt->nvalid;
    if (rank > 0)  if (! restoreTreeRecursive(bt->byScore[rank], tr)) return FALSE;
    return  rank;
  } /* recallBestTree */



int  recallBestTree (bestlist *bt, int rank, tree *tr)
  { /* recallBestTree */
    if (rank < 1)  rank = 1;
    if (rank > bt->nvalid)  rank = bt->nvalid;
    if (rank > 0)  if (! restoreTree(bt->byScore[rank], tr)) return FALSE;
    return  rank;
  } /* recallBestTree */

int  recallBestTopology (bestlist *bt, int rank, tree *tr)
  { /* recallBestTree */
    if (rank < 1)  rank = 1;
    if (rank > bt->nvalid)  rank = bt->nvalid;
    if (rank > 0)  if (! restoreTopology(bt->byScore[rank], tr)) return FALSE;
    return  rank;
  } /* recallBestTree */

/*=======================================================================*/
/*                       End of best tree routines                       */
/*=======================================================================*/
