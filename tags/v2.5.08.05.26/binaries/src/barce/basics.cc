/*====================================================================*
 *			basics.cc				      *
 *	Copyright (c) 2001, Grainne McGuire			      *
 *		     Version 1.00b, BARCE    			      *
 *								      *
 * Function file for basics.h. These two files declare and define     *
 * some basics classes (eg node/tree) and functions (eg memory error  *
 * handling) that are fundamental in the program.		      *
 *====================================================================*/

#include "basics.h"

/*=====================================================================*
 *			struct Node				       *
 * One of the fundamental data types in this program. See basics.h for *
 * more information.						       *
 *=====================================================================*/

// static Node variable
int Node::seqlen=0;

Node::Node()
{
 lchild=rchild=parent=0;
 node_num = -1;
 leaf=false;
 parent_bl=oldbl=0.0;
 residue=0;  // null pointers
 condlik=new double[lenalphabet];
 if(!condlik)
 { char mes[]="Node constructor"; OutofMemory(mes); }
 for(int i=0; i<lenalphabet; i++) condlik[i]=0.0;
}

//------------------- End of function -------------------//

Node::~Node()
{
 if(residue) { delete [] residue; residue=0; }
 if(condlik) { delete [] condlik; condlik=0; }
}

/*=================================================================*
 *			class Tree				   *
 * this class holds the tree structure and has functions for doing *
 * some manipulations on binary trees			           *
 *=================================================================*/

Tree::~Tree()
{
// destructor - clear the tree using a postorder traversal and
// clearing each visited node

 if(root)
 {
  for(int i=0; i<3; i++)
  {
    if(root[i]) { Clear(root[i]); root[i]=0; }
  }
  delete [] root;
  root=0;
 }
}

//------------------- End of function -------------------//

void Tree::ClearNode(Node* node)
{
  // uses inorder traversal to clear the tree
 if(node==0) return;

 ClearNode(node->lchild);
 ClearNode(node->rchild);

 Node* pnode=node->parent;
 if(pnode)
 {
  if(pnode->lchild == node)
  {
   delete pnode->lchild;
   pnode->lchild=0;
  }
  else if(pnode->rchild == node)
  {
   delete pnode->rchild;
   pnode->rchild=0;
  }
  else
  {
   cerr << "Impossible error in Tree::Clear(Node* node)\n";
   exit(1);
  }
 }
 else    // node is the root
 {
  delete node; node=0;
 }
} 

//------------------- End of function -------------------//

double* Tree::GetMeanBL()
{
 for(int i=0; i<3; i++) { mbl[i]=0.0; MeanBL(root[i], i); }
 return mbl;
}

//------------------- End of function -------------------//

void Tree::MeanBL(Node* n, int& wt)
{
 if(n==0) return;
 MeanBL(n->lchild, wt);
 MeanBL(n->rchild, wt);

 mbl[wt] += (n->parent_bl)/6;
}

/*========================================================*
 * OutofMemory(char*) deals with memory allocation errors *
 *========================================================*/

void OutofMemory(char* s)
{
 cerr << "\tnew allocation problem\a\n";
 cerr << "Out of memory in function " << s << endl;
 cerr << "Exiting...\n";
 exit(1);
}

/*====================================================================*
 *			v_xsort()				      *
 * Some sorting functions for integers and strings. The first form of *
 * v_ssort() orders d according to the order of the string vector s   *
 *====================================================================*/

void v_isort(int* v, int n)
{
  // integer sorting function
 int gap, i, j, temp;

 for(gap=n/2;gap >0; gap /= 2)
 {
  for(i=gap;i<n;++i)
  {
   for(j=i-gap;j>=0 && v[j]>v[j+gap];j-=gap)
   {
    temp=v[j];
    v[j]=v[j+gap];
    v[j+gap]=temp;
   }
  }
 }
}

//------------------- End of function -------------------//

void v_ssort(char** v, int* indx, int sl, int ns)
{
  // string sorting function, also sorts the int vector in the same way

 int gap, i, j;
 char* temp;
 int tem;

 for(gap=sl/2; gap >0; gap /= 2)
    for(i=gap;i<sl;++i)
       for(j=i-gap; j>=0 ;j-=gap)
       {
        if(memcmp(v[j], v[j+gap], ns) <= 0) break;
        temp=v[j];      tem=indx[j];
        v[j]=v[j+gap];  indx[j]=indx[j+gap];
        v[j+gap]=temp;  indx[j+gap]=tem;
       }
}

//------------------- End of function -------------------//
