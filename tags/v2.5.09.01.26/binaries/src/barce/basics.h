#ifndef BASICS_H_
#define BASICS_H_

/*====================================================================*
 *			basics.h				      *
 *	Copyright (c) 2001, Grainne McGuire			      *
 *		     Version 1.00b, BARCE	       		      *
 *								      *
 * Header file for basics.cc. These two files declare and define some *
 * basics classes (eg node/tree) and functions (eg memory error       *
 * handling) that are fundamental in the program.		      *
 *====================================================================*/

//#define DEBUGGING_RUN_
//#define USING_ANSI_CPP_  // to activate the ANSI C++ code
// please note that the ANSI C++ code has NOT been tested so it may not
// work. I take no responsibility for that part of the code.

#if defined(USING_ANSI_CPP_)

#include <cmath>
#include <iostream>
#include <stack>   /* for storing the stack of nodes */
#include <cstdlib>
#include <cstring>

#ifndef USING_NAMESPACE_STD_
#define USING_NAMESPACE_STD_
using namespace std;

#endif

#else  /* ie not ANSI C++  */

#include <math.h>
#include <iostream>
#include <stdlib.h>
#include <string.h>

#endif

using namespace std;

// a variable to switch between nucleotide and amino acid data
// relic from MAC5 but will be useful should I get time to code for
// amino acid calculations using the Kimura (F81) model
// if so lenalphabet below can be 4 or 20
typedef enum { DNA, PROT} DATATYPE;

const int lenalphabet=4;      // four nucleotides

/*========================================================================*
 *			struct Node					  *
 * Fundamental data structure (declared as a struct rather than a class   *
 * since all functions/data members are public). Node defines what the    *
 * tree nodes will look like and also store all the necessary information *
 *========================================================================*/

struct Node
{
Node();
~Node();

 int        node_num;	 // number of this particular node 
bool        leaf;	 // true if leaf node   
 double     parent_bl;   // branchlength between parent and this node 
 double     oldbl;       // "old" bl, used when proposing new bls 
 char*      residue;
 double*    condlik;     // hold conditional likelihoods at each node
 static int seqlen;

Node*       lchild;          
Node*       rchild;	       
Node*       parent;          
};

typedef Node* ptrNode;

typedef enum { NOMOVE, MOVE } Moving;

/*=================================================================*
 *			class Tree				   *
 * this class holds the tree structure and has functions for doing *
 * some manipulations on binary trees			           *
 * Since this code is written only for trees of 4 sequences, these *
 * trees are hardcoded in. Obviously this needs to be changed      *
 * should a way be found to generalise this to more than 4         *
 * sequences							   *
 *=================================================================*/

class Tree
{
public:
 Tree() { root = 0; }
 ~Tree();

 void   Clear(Node* r) { ClearNode(r); }
 Node**  GetRoot() { return root; }
 Node*   GetRoot(int wt) { return root[wt]; }
 double* GetMeanBL();

protected:
 void   ClearNode(Node*);
 void   MeanBL(Node*, int&);

Node**  root;  // vector of Node*s, pointing to each tree root
 double mbl[3];
};

/*===========================================================*
 * OutofMemory(char*) is a function which deals with dynamic *
 * memory allocation errors. s is a string which gives       *
 * information on which function the memory error occurred.  *
 * This function causes the program to exit.		     *
 *===========================================================*/

void OutofMemory(char* s);

/*===============================================================*
 * v_xsort() are sorting functions given to me by Alan Bleasby   *
 * x=i is the interger sorting function while x=s is a (somewhat *
 * modified) string sorting function - used to sort out the site *
 * patterns							 *
 *===============================================================*/

void v_isort(int* v, int n);
void v_ssort(char** v, int* indx, int sl, int ns);

#endif
