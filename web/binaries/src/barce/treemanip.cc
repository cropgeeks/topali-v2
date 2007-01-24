/*=================================================================*
 *			treemanip.cc				   *
 *	Copyright (c) Grainne McGuire, 2001			   *
 *		      Version 1.00b, BARCE  			   *
 *								   *
 * contains definitions for some of the classes declared in	   *
 * treemanip.h. See this file for more information on the contents *
 *=================================================================*/

#include "treemanip.h"

/*==================================================================*
 *		      class InitTree				    *
 * contains the definitions of the functions used to initialise the *
 * tree structure						    *
 *==================================================================*/

InitTree::InitTree(double bl) : Tree()
{
 if(bl!=0.0) init_bl = bl;
 else init_bl=(double)runif(0.01, 0.2);  // initial bl chosen randomly
}

//-----------------END OF FUNCTION-----------------//

void InitTree::InitialiseTree()
{
// hard codes the three possible unrooted trees for three sequences
// the sequence length still has to be assigned to the nodes but that
// should be done by a data class.

 char mes[]="InitTree::InitialiseTree()";

 root=new Node*[3];
 if(!root) OutofMemory(mes);

 int temp;
 for(int i=0; i<3; i++)
 {
  root[i] = new Node; if(!root[i]) OutofMemory(mes);
  root[i]->node_num = 6;
  root[i]->parent = 0;

  root[i]->lchild = new Node; if(!root[i]->lchild) OutofMemory(mes);
  root[i]->lchild->parent = root[i];
  root[i]->lchild->parent_bl = root[i]->lchild->oldbl = init_bl;
  root[i]->lchild->node_num=4;

  root[i]->lchild->lchild=new Node;
          if(!root[i]->lchild->lchild) OutofMemory(mes);
  root[i]->lchild->lchild->parent = root[i]->lchild;
  root[i]->lchild->lchild->parent_bl = root[i]->lchild->lchild->oldbl=init_bl;
  root[i]->lchild->lchild->node_num = 0;  // always zero
  root[i]->lchild->lchild->lchild = 0;
  root[i]->lchild->lchild->rchild = 0;

  root[i]->lchild->rchild=new Node;
          if(!root[i]->lchild->rchild) OutofMemory(mes);
  root[i]->lchild->rchild->parent = root[i]->lchild;
  root[i]->lchild->rchild->parent_bl=root[i]->lchild->rchild->oldbl=init_bl;
  root[i]->lchild->rchild->node_num = i+1;  // setting up diff trees
  root[i]->lchild->rchild->lchild = root[i]->lchild->rchild->rchild = 0;

  root[i]->rchild = new Node; if(!root[i]->rchild) OutofMemory(mes);
  root[i]->rchild->parent=root[i];
  root[i]->rchild->parent_bl = root[i]->rchild->oldbl = init_bl;
  root[i]->rchild->node_num = 5;
 
  root[i]->rchild->lchild=new Node;
          if(!root[i]->rchild->lchild) OutofMemory(mes);
  root[i]->rchild->lchild->parent = root[i]->rchild;
  root[i]->rchild->lchild->parent_bl=root[i]->rchild->lchild->oldbl=init_bl;
  if(i==0) temp=2;
  else temp=1;
  root[i]->rchild->lchild->node_num = temp;
  root[i]->rchild->lchild->lchild = root[i]->rchild->lchild->rchild = 0;

  root[i]->rchild->rchild=new Node;
          if(!root[i]->rchild->rchild) OutofMemory(mes);
  root[i]->rchild->rchild->parent = root[i]->rchild;
  root[i]->rchild->rchild->parent_bl=root[i]->rchild->rchild->oldbl=init_bl;
  if(i==2) temp=2;
  else temp=3;
  root[i]->rchild->rchild->node_num = temp;
  root[i]->rchild->rchild->lchild = root[i]->rchild->rchild->rchild = 0;
 }
}

/*=================================================================*
 *			class TreeCalc				   *
 * General purpose class for initially allocating data to the tree *
 * structure and for carrying out the Metropolis-Hastings and      *
 * Gibbs sampling. See treemanip.h for some more details	   *
 *=================================================================*/

TreeCalc::TreeCalc(Tree* t, ReadData* rd)
{
 seqdata=rd;
 tree=t;
 steps=naccept=0;
 for(int i=0; i<3; i++)
       CreateNodeArrays(tree->GetRoot(i), seqdata->GetSeqlen() );
 // allocates memory to the arrays which will hold the residues
}

//------------------- End of function -------------------//

TreeCalc::~TreeCalc()
{
 for(int i=0; i<3; i++)
 {
  if(tree->GetRoot() ) FreeNodeArrays(tree->GetRoot(i));
 }
}

//------------------- End of function -------------------//

void TreeCalc::FreeNodeArrays(Node* n)
{
 if(n==0) return;

 if(n->leaf)
 {
  if(n->residue) { delete [] n->residue; n->residue=0; }
 }

 FreeNodeArrays(n->lchild);
 FreeNodeArrays(n->rchild);
}

//------------------- End of function -------------------//

void TreeCalc::SetValues(Tree* t, ReadData* rd)
{
 seqdata=rd;
 tree=t;
 steps=naccept=0;
 for(int i=0; i<3; i++)
        CreateNodeArrays(tree->GetRoot(i), seqdata->GetConcatlen() );
}

//------------------- End of function -------------------//

void TreeCalc::CreateNodeArrays(Node* n, int sl)
{
  // function to create space for the residue arrays at the leaves
  // note that the concatenated data is stored at each node
 if(n==0) return;

 if(n->leaf)
 {
  if(!(n->residue) )
  {
   n->residue = new char[sl];
   if(!(n->residue) )
   { char mes[]="TreeCalc::CreateNodeArrays"; OutofMemory(mes); }
  }
 }
 n->seqlen=sl;  // concatenated data length

 CreateNodeArrays(n->lchild, sl);
 CreateNodeArrays(n->rchild, sl);
}

//-----------------END OF FUNCTION-----------------//

void TreeCalc::AssignOuterData(Node* n)
{
  // places the residues in their storage containers on the tree
  // for the leaf nodes
 if(n==0) return;
 AssignOuterData(n->lchild);
 AssignOuterData(n->rchild);

 if(n->lchild==0)
 {
  if(!n->residue)
  {
   n->residue = new char[seqdata->GetConcatlen()];
   if(!n->residue) { char mes[]="AssignOuterData()"; OutofMemory(mes); }
  }
  for(int j=0; j<n->seqlen; j++)
                 n->residue[j] = seqdata->GetData()[j][n->node_num];
 }
}

//-----------------END OF FUNCTION-----------------//

void TreeCalc::NumberInfo()
{
// outputs the number of proposals and the proportion of these
// accepted
 cout << steps << " proposals have been made.\n" << naccept;
 cout << " have been accepted.\n\n";
}

/*=====================================================================*
 *			class CalcProb				       *
 * definitions of the likelihood and posterior probability calculating *
 * functions. Note also that space for the static data members has to  *
 * be declared outside of the class (since these are the same for all  *
 * instances of the class, it makes sense that they have to have their *
 * own space independent of the class)				       *
 *=====================================================================*/

// defining the static data members
int        CalcProb::seqlen=0;
double     CalcProb::shape=SHAPE, CalcProb::scale=SCALE;
double     CalcProb::lhood=0.0;
double***  CalcProb::transprob=0;
double**** CalcProb::ftransprob=0;
int        CalcProb::numnodes=7;
Model      CalcProb::evomod=JC;
double     CalcProb::alpha[3];
double     CalcProb::gamma[3];
double*    CalcProb::tstv=0;  // allowing for 3 trees
double     CalcProb::E=0;
double     CalcProb::A=0;
double     CalcProb::B=0;
double     CalcProb::C=0;
double*    CalcProb::pi=0;
double**   CalcProb::concatll=0;
int*       CalcProb::sorted=0;
int*       CalcProb::weight=0;
int        CalcProb::concatlen=0;
double**   CalcProb::loglik=0;
double     CalcProb::prior[3];
double     CalcProb::sharedprior=0.0;
double     CalcProb::oldprior[3];
double     CalcProb::minpi=0.0;

CalcProb::~CalcProb()
{
 int i, j, n;

 if(pi) { delete [] pi; pi=0; }

 if(tstv) { delete [] tstv; tstv=0; }

 if(loglik)
 {
  for(i=0; i<3; i++)
  {
   if(loglik[i]) { delete [] loglik[i]; loglik[i]=0; }
  }
  delete [] loglik;
  loglik=0;
 }

 if(concatll)
 {
  for(i=0; i<3; i++)
  {
   if(concatll[i]) { delete [] concatll[i]; concatll[i]=0; }
  }
  delete [] concatll;
  concatll=0;
 }

 if(sorted) { delete [] sorted; sorted=0; }
 if(weight) { delete [] weight; weight=0; }

 if(evomod==JC || evomod==K2P)
 {
  if(transprob)
  {
   for(n=0; n<3; n++)
   {
    if(transprob[n])
    {
     for(i=0; i<numnodes; i++)
     {
      if(transprob[n][i]) { delete [] transprob[n][i]; transprob[n][i]=0;}
     }
     delete [] transprob[n];
     transprob[n]=0;
    }
   }
  delete [] transprob;
  transprob=0;
  }
 }

 else  // F81 and F84 models
 {
  if(ftransprob)
  {
   for(n=0; n<3; n++)
   {
    if(ftransprob[n])
    {
     for(i=0; i<numnodes; i++)
     {
      if(ftransprob[n][i])
      {
       for(j=0; j<lenalphabet; j++)
       {
        if(ftransprob[n][i][j]) 
            { delete [] ftransprob[n][i][j]; ftransprob[n][i][j]=0; }
       }
      delete [] ftransprob[n][i];
      ftransprob[n][i]=0;
      }
     }
     delete [] ftransprob[n];
     ftransprob[n]=0;
    }
   }
   delete [] ftransprob;
   ftransprob=0;
  }
 }
}

//-----------------END OF FUNCTION-----------------//

void CalcProb::SetSpace()
{
  // function which allocates space to the storage containers for the
  // transition probabilies. Called by the constructor

 int i, n;
 if(evomod==JC || evomod==K2P)
 {
  transprob = new double**[3];   // 3 trees
  if(!transprob) { char mes[]="CalcProb::SetSpace()"; OutofMemory(mes); }

  for(n=0; n<3; n++)
  {
   transprob[n]=new double*[numnodes]; 
   if(!transprob[n]) { char mes[]="CalcProb::SetSpace()"; OutofMemory(mes); }

   for(i=0; i<numnodes; i++)
   {
    if(evomod==JC) transprob[n][i]=new double[2];
    else	    transprob[n][i]=new double[3];
    if(!transprob[n][i])
        { char mes[]="CalcProb::SetSpace()"; OutofMemory(mes); }
   }
  }
 }

 else    // evomod is F81 or F84
 {
  ftransprob = new double***[3];
  if(!ftransprob) { char mes[]="CalcProb::SetSpace"; OutofMemory(mes); }

  for(n=0; n<3; n++)
  {
   ftransprob[n]=new double**[numnodes];
   if(!ftransprob[n])
           { char mes[]="CalcProb::SetSpace"; OutofMemory(mes); }

   for(i=0; i<numnodes; i++)
   {
    ftransprob[n][i]=new double*[lenalphabet];
    if(!ftransprob[n][i])
        {char mes[]="CalcProb::SetSpace"; OutofMemory(mes);}

    for(int j=0; j<lenalphabet; j++)
    {
     if(evomod==F81) ftransprob[n][i][j]=new double[2];
     else	     ftransprob[n][i][j]=new double[3];
     if(!ftransprob[n][i][j])
          {char mes[]="CalcProb::SetSpace";OutofMemory(mes);}
    }
   }
  }
 }
}
//-----------------END OF FUNCTION-----------------//

void CalcProb::SetSorted(int* s)
{
  // creates space and copies values into the array sorted
 if(seqlen==0)
 {
  cerr << "Error! Seqlen not yet set in CalcProb::SetSorted()\n";
  cerr << "Exiting...\n";
  exit(1);
 }

 sorted=new int[seqlen];
 if(!sorted) { char mes[]="CalcProb::SetSorted()"; OutofMemory(mes); }
 for(int i=0; i<seqlen; i++) sorted[i]=s[i];
}

//-----------------END OF FUNCTION-----------------//

void CalcProb::SetWeight(int* w)
{
  // creates space and copies values into the array sorted
 if(concatlen==0)
 {
  cerr << "Error! Concatlen not yet set in CalcProb::SetWeight()\n";
  cerr << "Exiting...\n";
  exit(1);
 }

 weight=new int[concatlen];
 if(!weight) { char mes[]="CalcProb::SetWeight()"; OutofMemory(mes); }
 for(int i=0; i<concatlen; i++) weight[i]=w[i];
}

//-----------------END OF FUNCTION-----------------//

void CalcProb::SetPi(double* p)
{
 int i;

 if(!pi)
 {
  pi = new double[4];
  if(!pi) { char mes[]="CalcProb::SetPi()"; OutofMemory(mes); }
  for(i=0; i<4; i++) pi[i]=p[i];
  // setting minpi
  // if any of the pi values fall below minpi then we use an independence
  // sampler
  minpi=pi[0];
  for(i=1; i<4; i++)
  {
   if(pi[i]<minpi) minpi=pi[i];
  }
  minpi /= 3;
 }
}

//-----------------END OF FUNCTION-----------------//

void CalcProb::SetTstv(double tr)
{
 if(!tstv)
 {
  tstv = new double[3];
  if(!tstv) { char mes[]="CalcProb::SetTstv()"; OutofMemory(mes); }
  for(int i=0; i<3; i++) tstv[i]=tr;
 }
}

//-----------------END OF FUNCTION-----------------//

void CalcProb::CalcTstv(int wt)
{
  // calculates the new ts-tv ratio, used when alpha, gamma or the
  // stationary frequencies have been updated.
  // tstv[wt] = (alpha[wt]*A[wt] + gamma[wt]*B[wt])/(gamma[wt]*C[wt]);
 tstv[wt] = (alpha[wt]*A + gamma[wt]*B)/(gamma[wt]*C);
}

//-----------------END OF FUNCTION-----------------//

void CalcProb::SetLoglik()
{
  // sets up the storage space for concatll and loglik, but does not assing
  // values to them (called before the first likelihoods are calculated

 if(seqlen==0 || concatlen==0)
 {
  cerr << "Error! Seqlen or concatlen not yet set in CalcProb::SetLoglik()";
  cerr << "\nExiting...\n";
  exit(1);
 }

 if(!concatll)
 {
  concatll = new double*[3];
  if(!concatll) { char mes[]="CalcProb::SetLoglik()"; OutofMemory(mes); }

  for(int i=0; i<3; i++)
  {
   concatll[i]=new double[concatlen];
   if(!concatll[i]) { char mes[]="CalcProb::SetLoglik()"; OutofMemory(mes); }
  }
 }
 
 if(!loglik)
 {
  loglik = new double*[3];
  if(!loglik) { char mes[]="CalcProb::SetLoglik()"; OutofMemory(mes); }

  for(int i=0; i<3; i++)
  {
   loglik[i]=new double[seqlen];
   if(!loglik[i]) { char mes[]="CalcProb::SetLoglik()"; OutofMemory(mes); }
  }
 }
}

//-----------------END OF FUNCTION-----------------//

void CalcProb::CalcLik(Node** root, const int wt) // node will be root of tree
{
  // "master" likelihood calculation function
  // calculates the likelihood at each site and also the prior probabilities
  // over bl and char freqs which need to be included in the calculation
  // tree wt determines which tree, results are stored in appropriate
  // row of loglik

 int i;
 for(i=0; i<concatlen; i++)
 {
   lhood=0.0;
   CalcLikRec(root[wt], i, wt);   // sending in which tree to use
   concatll[wt][i] = lhood;
 }

 // now to set the values of loglik using sorted, weight and concatll
 int k=0, j=0, sort_loc=0;
 while(k < concatlen)
 {
  for(j=0; j<weight[k]; j++) 
          loglik[wt][ sorted[sort_loc+j] ] = concatll[wt][k];
  sort_loc +=weight[k];
  k++;
 }

 // prior probability calculation
 for(i=0; i<3; i++) oldprior[i]=prior[i];

 // branch lengths (trees are uniform)
 prior[wt]=0.0;
 // placing a uniform[0,1] prior on bls
 // don't need to add any prior probabilities (log 1=0)
 // but note that already set up to be no more than 1
 // (see ScaleBranch proposal function)
 // PriorBl(root[wt], prior[wt]);

 // now the stationary frequencies, ddirich returns log density
 // only one set needed since frequencies are the same in the trees
 // added via sharedprior

 if(evomod==F81 || evomod==F84)
 {
  double* pp=new double[lenalphabet];
  if(!pp) { char mes[]="CalcProb::CalcRP()"; OutofMemory(mes); }
  for(i=0; i<lenalphabet; i++) pp[i]=1.0;
  sharedprior=ddirich(pi, pp, lenalphabet);
  delete [] pp; pp=0;
 }
 // prior on alpha (U[0,2])
 prior[wt]+= log(0.5);   // alpha prior
 // lamdba prior done within HmmCalcPost

 // lhood=total_lhood + prior;
}

//-----------------END OF FUNCTION-----------------//

void CalcProb::CalcLikRec(Node* node, int site, const int wt)
{
  // recursive site likelihood calculation. Calls the appropriate
  // model-based likelihood calculating function.

 if(node==0) return;
 CalcLikRec(node->lchild, site, wt);
 CalcLikRec(node->rchild, site, wt);

 CalcDP(node, site, wt);
 if(node->parent==0) lhood+=log( CalcRP(node, site, wt) );
}

//-----------------END OF FUNCTION-----------------//

void CalcProb::CalcDP(Node* node, int site, const int wt)
{
  // function to calculate likelihoods under the various models
 
 int i;

 if(node->lchild==0)   // leaf node, not root node
 {
  char res = node->residue[site];
  for(i=0; i<lenalphabet; i++)
  {
   if(res!='-'&& res!='n') node->condlik[i]=0;
   else         node->condlik[i]=1;
  }

  if(res=='a') node->condlik[0]=1;
  else if(res=='c') node->condlik[1]=1;
  else if(res=='g') node->condlik[2]=1;
  else if(res=='t') node->condlik[3]=1;
 }


 else    // internal node or root node
 {
  double* lprob=new double[lenalphabet];
  double* rprob=new double[lenalphabet];  // for left, right probs
  if(!lprob || !rprob) { char mes[]="CalcProb::CalcDP()"; OutofMemory(mes); }

  bool reuse=true;
  if(site==0) reuse=false;   // reuse stored transition probs

  if(evomod==JC)  
  {
   JCprob(lprob, node->lchild, reuse, wt);
   JCprob(rprob, node->rchild, reuse, wt);
  }
  else if(evomod==F81)
  {
   F81prob(lprob, node->lchild, reuse, wt);
   F81prob(rprob, node->rchild, reuse, wt);
  }
  else if(evomod==K2P)
  {
   K2Pprob(lprob, node->lchild, reuse, wt);
   K2Pprob(rprob, node->rchild, reuse, wt);
  }
  else
  {
   F84prob(lprob, node->lchild, reuse, wt);
   F84prob(rprob, node->rchild, reuse, wt);
  }
   
  for(i=0; i<lenalphabet; i++) node->condlik[i]=lprob[i]*rprob[i];

  delete [] lprob;
  delete [] rprob;
 }
}

//-----------------END OF FUNCTION-----------------//

double CalcProb::StatResProb(char r, int nuc, const int wt)
{
  // returns stationary frequencies for the various models
  //
  // can handle either characters or integers for the nucleotides and gap.
  // To use the integers, let r be '\0'.

 if(evomod==JC || evomod==K2P) return 0.25;   // JC, K2P frequency

 // model is F81 or F84
 if(r=='\0') return pi[nuc];
 else
 {
  if(r=='a')      return pi[0];
  else if(r=='c') return pi[1];
  else if(r=='g') return pi[2];
  else            return pi[3];
 }
}

//-----------------END OF FUNCTION-----------------//

void CalcProb::PriorBl(Node* n, double& pbl)
{
 if(n==0) return;
 PriorBl(n->lchild, pbl);
 PriorBl(n->rchild, pbl);

 // pbl+=log(dgamma(n->parent_bl, shape, scale));   removed 14.11
 // pbl -= log(MAX_BRANCH);
 if(n->parent)
 {
   // pbl+=log(dbeta(n->parent_bl, 0.99,1));
  pbl += log(dgamma(n->parent_bl, shape, scale) );
 }
}

//-----------------END OF FUNCTION-----------------//

void CalcProb::JCprob(double* clik, Node* node, bool reuse, const int wt)
{
  // likelihood calculating function
 double bl = node->parent_bl;
 double prod;
 int k, l;

 for(k=0; k<lenalphabet; k++)
 {
  prod=0;
  for(l=0;l<lenalphabet;l++)
  {
   if(reuse==false)
             prod+=CalcJCprobs(k, l, bl, node->node_num, wt)*node->condlik[l];
   else
   {
    if(k==l) prod+=transprob[wt][node->node_num][0]*node->condlik[l];
    else     prod+=transprob[wt][node->node_num][1]*node->condlik[l];
   }
  }
  clik[k]=prod;
 }
}

//-----------------END OF FUNCTION-----------------//

double CalcProb::CalcJCprobs(int par,int child,double bl,int nn,const int wt)
{
  // function to return the JC transition properties 
  // *** Also sets up the entries in transprob(3 x numnodes x 2) matrix which
  // stores the transition probabilities for each branch to cut down on
  // computation time. As such this function is only called once for any
  // likelihood calculation for a tree.
  // nn is the node number, tells which row of transprob the stored 
  // calculations belong in.

 transprob[wt][nn][0] = (0.25 + 0.75*exp(-4*bl/3.0) );
 transprob[wt][nn][1] = (0.25 - 0.25*exp(-4*bl/3.0) );

 // 8.8.00  check for underflow errors

 if(par==child) return transprob[wt][nn][0];
 else           return transprob[wt][nn][1];
}

//-----------------END OF FUNCTION-----------------//

double CalcProb::CalcRP(Node* node, int site, const int wt)
{
  // calculates the overall likelihood at the root

 double prob=0.0;
 for(int i=0; i<lenalphabet; i++)
               prob+= node->condlik[i] * StatResProb('\0',i, wt);
 return prob;
}

//-----------------END OF FUNCTION-----------------//

void CalcProb::K2Pprob(double* clik, Node* node, bool reuse, const int wt)
{
  // likelihood calculating function
 double bl = node->parent_bl;
 double prod;
 int k, l;

 for(k=0; k<lenalphabet; k++)
 {
  prod=0;
  for(l=0;l<lenalphabet;l++)
  {
   if(reuse==false)
             prod+=CalcK2Pprobs(k, l, bl, node->node_num, wt)*node->condlik[l];
   else
   {
    if(k==l)        prod+=transprob[wt][node->node_num][0]*node->condlik[l];
    else if(k+l==2) prod+=transprob[wt][node->node_num][1]*node->condlik[l];
    else            prod+=transprob[wt][node->node_num][2]*node->condlik[l];
   }
  }
  clik[k]=prod;
 }
}

//-----------------END OF FUNCTION-----------------//

double CalcProb::CalcK2Pprobs(int par,int child,double bl,int nn,const int wt)
{
  // function to return the K2P transition properties
 
 transprob[wt][nn][0] = exp(-(alpha[wt]+gamma[wt])*bl) + 
                    0.5*exp(-gamma[wt]*bl)*(1-exp(-alpha[wt]*bl)) +
		    0.25*(1-exp(-gamma[wt]*bl));
 transprob[wt][nn][1] = 0.5*exp(-gamma[wt]*bl)*(1-exp(-alpha[wt]*bl)) +
		    0.25*(1-exp(-gamma[wt]*bl));
 transprob[wt][nn][2] = 0.25*(1-exp(-gamma[wt]*bl));

 if(par==child)                        return transprob[wt][nn][0];
 else if(par+child==2 || par+child==4) return transprob[wt][nn][1];
 else                                  return transprob[wt][nn][2];
}

//-----------------END OF FUNCTION-----------------//

void CalcProb::F81prob(double* clik, Node* node, bool reuse, const int wt)
{
 double bl = node->parent_bl;
 double prod;
 int k, l;

 for(k=0; k<lenalphabet; k++)
 {
  prod=0;
  for(l=0;l<lenalphabet;l++)
  {
   if(reuse==false)
   {
    prod+=CalcF81probs(k, l, bl, node->node_num, wt)*node->condlik[l];
    reuse=true;  // doesn't change value of reuse in calling function
    // (CalcDP()) since it is not a reference. But that's what I want
    // since once the left ts probs have to be stored I then need to 
    // calculate the right ts probs.
   }
   else
   {
    if(k==l) prod+=ftransprob[wt][node->node_num][l][0]*node->condlik[l];
    else     prod+=ftransprob[wt][node->node_num][l][1]*node->condlik[l];
   }
  }
  clik[k]=prod;
 }
}

//-----------------END OF FUNCTION-----------------//

double CalcProb::CalcF81probs(int par,int child,double bl,int nn,const int wt)
{
 for(int i=0; i<lenalphabet; i++)
 {
  ftransprob[wt][nn][i][0] = pi[i] + (1.0 - pi[i])*exp(-bl/E);
  ftransprob[wt][nn][i][1] = pi[i]*(1.0 - exp(-bl/E) );
 }
 if(par==child) return ftransprob[wt][nn][child][0];
 else		return ftransprob[wt][nn][child][1];
}

//-----------------END OF FUNCTION-----------------//

void CalcProb::UpdateConstants(const int wt)
{
  // calculates constants needed in F81 and F84 models
  // note that wt is no longer needed
 int i;
 double sum=0.0;
 if(evomod!=JC && evomod!=K2P)
 {
  for(i=0; i<lenalphabet; i++) sum+=pi[i]*pi[i];
  E=1.0-sum;
 }

 if(evomod==F84)
 {
  A = 2*( pi[0]*pi[2]/(pi[0]+pi[2]) 
                 + pi[1]*pi[3]/(pi[1]+pi[3]) );
  B = 2*pi[0]*pi[2] +2* pi[1]*pi[3];
  C = 2*(pi[0]+pi[2])*(pi[1]+pi[3]);
 }
 else if(evomod==K2P)
 {
  A=0.5; B=0.25;
  C=0.5;
 }
}

//-----------------END OF FUNCTION-----------------//

void CalcProb::SetF84Params(const int wt)
{
  // uses the transition-transversion rate and the fact that the average
  // rate must be one to calculate alpha and gamma
  // Despite name, sets the parameters for the K2P model as well

 int finish=1;

 do
 {
   //  gamma[wt] = 1.0/(2*C[wt]*tstv[wt] -2*B[wt] +E[wt]);
   //  alpha[wt] = (1.0 - E[wt]*gamma[wt])/(2*A[wt]);

   //1.11.00  check these are correct
  gamma[wt] = 1.0/(C*tstv[wt] + C);
  alpha[wt] = (1.0-gamma[wt]*(B+C))/A;

  if(alpha[wt]<0 || gamma[wt]<0)
  {
   cout << "F84/K2P parameters are taking invalid values.\n";
   cout << "Enter a new value for the ts/tv ratio: ";
   cin >> tstv[wt];
  }
  else finish=0;
 }while(finish!=0);
}

//-----------------END OF FUNCTION-----------------//


void CalcProb::F84prob(double* clik, Node* node, bool reuse, const int wt)
{
  // calculates probability for F84+gaps model
 double bl = node->parent_bl;
 double prod;
 int k, l;

 for(k=0; k<4; k++)
 {
  prod=0;
  for(l=0;l<4;l++)
  {
   if(reuse==false)
   {
    prod+=CalcF84probs(k, l, bl, node->node_num, wt)*node->condlik[l];
    reuse=true;  // doesn't change value of reuse in calling function
    // (CalcDP()) since it is not a reference. But that's what I want
    // since once the left ts probs have to be stored I then need to 
    // calculate the right ts probs.
   }
   else
   {
    if(k==l) prod+=ftransprob[wt][node->node_num][l][0]*node->condlik[l];
    else if(k+l==2 || k+l==4)
             prod+=ftransprob[wt][node->node_num][l][1]*node->condlik[l];
    else     prod+=ftransprob[wt][node->node_num][l][2]*node->condlik[l];
   }
  }
  clik[k]=prod;
 }
}

//-----------------END OF FUNCTION-----------------//

double CalcProb::CalcF84probs(int par,int child,double bl,int nn,const int wt)
{
  // DNA models only, hence lenalphabet not used
 double tsdiv;   // pi_R or pi_Y as appropriate
 for(int i=0; i<4; i++)
 {
  if(i%2==0) tsdiv=pi[0]+pi[2];
  else tsdiv=pi[1]+pi[3];
  // p_nn prob
  ftransprob[wt][nn][i][0] = exp(-(alpha[wt]+gamma[wt])*bl) + 
      exp(-gamma[wt]*bl)*(1-exp(-alpha[wt]*bl))*pi[i]/tsdiv +
          (1-exp(-gamma[wt]*bl))*pi[i];

  // transition prob
  ftransprob[wt][nn][i][1] = 
      exp(-gamma[wt]*bl)*(1-exp(-alpha[wt]*bl))*pi[i]/tsdiv +
          (1-exp(-gamma[wt]*bl))*pi[i];

  // transversion prob
  ftransprob[wt][nn][i][2] = (1-exp(-gamma[wt]*bl))*pi[i];
 }

 // returning appropriate probabilities

  if(par==child)		        return ftransprob[wt][nn][child][0];
  else if(par+child==2 || par+child==4) return ftransprob[wt][nn][child][1];
  else					return ftransprob[wt][nn][child][2];
}

//-----------------END OF FUCNTION-----------------//

void CalcProb::SetGamma(int wt)
{
 UpdateConstants(wt);
 gamma[wt] = (1-A*alpha[wt])/(B+C);
}
