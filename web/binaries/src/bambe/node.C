#include "util.h"
#include "node.h"

#include <iostream>
#include <ctype.h>
#include <stdlib.h>
#include <fstream>
#include <iomanip>

double hastingsBeta(double c, double x, double y, double eta);
double hastingsBetaPair(double cx, double cy, double x1, double y1, double x2, 
                          double y2, double eta);
void findDist(const RunSettings& rs, double m, double r, double& y, double& hr);
                          
const char * const Node::noName="";

const Site Node::noMask;

inline void swapInt(int& a, int& b) { int t=a; a=b; b=t; }

inline void threeSortInt(int& a, int& b, int& c) 
{
  if(lessThan(a,b)) {
    if(lessThan(c,a))
      { int t = a; a = c; c = b; b = t; } // c < a < b
    else if(lessThan(c,b))
      swapInt(b,c); 			// a <= c < b
  }
  else // a>=b
    if(lessThan(c,b))
      swapInt(a,c);			// c < b <= a
    else  // c>=b
      if(lessThan(a,c))
	swapInt(a,b);			// b <= a < c
      else  // a>=c
	{ int t=a; a=b; b=c; c=t; }	// b <= c <= a
}

inline void swapNode(Node*& a, Node*& b) { Node* t=a; a=b; b=t; }

inline void threeSortNode(Node*& a, Node*& b, Node*& c) 
{
  if(lessThan(a,b)) {
    if(lessThan(c,a))
      { Node* t = a; a = c; c = b; b = t; } // c < a < b
    else if(lessThan(c,b))
      swapNode(b,c); 			// a <= c < b
  }
  else // a>=b
    if(lessThan(c,b))
      swapNode(a,c);			// c < b <= a
    else  // c>=b
      if(lessThan(a,c))
	swapNode(a,b);			// b <= a < c
      else  // a>=c
	{ Node* t=a; a=b; b=c; c=t; }	// b <= c <= a
}

void Node::listOneNode(Dlist& list, int n)
{
  // Adds node to doubly-linked list.
  list[n].node = this;
  list[n].prev = n - 1;
  list[n].next = n + 1;
  list[n].done = 0;
}

/* In combination, these functions put together a tree after its
 * excursion has been initialized or modified. Dlist is a
 * doubly-linked list of nodes where every other element (starting at
 * 0) is a leaf. The heights of each nodes in the excursion is stored
 * in the height field of the node. The left, right, and parent pointers
 * of the nodes are set to the corresponding tree. 
 */

int parentIndex(int i, const Dlist& list)
{
  /* Returns -2 if i is the root. Returns -1 if i is not a leaf.
     Otherwise, returns the index of i's parent in the linked list. */

  if(list[i].prev<0)
    if(list[i].next<0)
      return -2;
    else
      return (list.higher(i,list[i].next) ? list[i].next : -1);
  if(list[i].next<0)
    return   (list.higher(i,list[i].prev) ? list[i].prev : -1);
  if(list.higher(list[i].next,list[i].prev))
    return   (list.higher(i,list[i].next) ? list[i].next : -1);
  else
    return   (list.higher(i,list[i].prev) ? list[i].prev : -1);
}

int leafParent(int i, const Dlist& list)
{
  /* Simplified version of parent for the leaves of the original tree.
     These leaves are assumed to have parents (not true for a 1-node tree).
     Also, the leaves occur lower in the tree than any ancestor. */

  if(list[i].prev<0)
    return list[i].next;
  else if(list[i].next<0)
    return list[i].prev;
  else if(list.higher(list[i].next,list[i].prev))
    return list[i].next;
  else
    return list[i].prev;
}

void removeLeaf(int i, int d, Dlist& list)
/*  Removes i from the linked list. d is the parent of i. 
    Sets i to be the appropriate child of d. */
{
  if(list[i].prev>=0)
    list[list[i].prev].next = list[i].next;
  if(list[i].next>=0)
    list[list[i].next].prev = list[i].prev;
  list[i].node->branchLength = list[i].node->height-list[d].node->height;
  list[i].node->parent = list[d].node;
  if(i>d)
    list[d].node->right = list[i].node;
  else
    list[d].node->left = list[i].node;
}

Internal* makeTree(Dlist& list, int numNodes)
{
  /* Change left, right, parent, and branchLength of the nodes to
     reflect the tree with nodes of given heights. list is an
     doubly-linked list containing an array of nodes of the tree where
     every other element, starting at 0, is a leaf, and every other
     element starting a 1, is an internal node. The height field of
     each node is set to the height of the node in the excursion. */

  Internal* root;

  for(int i1=0;i1<numNodes;i1+=2) {
    removeLeaf(i1,leafParent(i1,list),list);
    list[i1].done = 1;
  }
  for(int i2=1;i2<numNodes;i2+=2) {
    if(!list[i2].done) {
      int j,d;
      for(j=i2,d=parentIndex(j,list);
	  d>=0;				// d >= 0 only if j is a leaf.
	  j=d,d=parentIndex(j,list)) {
	removeLeaf(j,d,list);
	list[j].done = 1;
      }
      if(d==-2) {
	root = (Internal*)(list[j].node);
	list[j].done = 1;
      }
    }
  }
  root->branchLength = 0;
  root->parent = NULL;
  return root;
}

int Leaf::readBambe(istream& f, Vector<Node*>&)
{
  /* Reads in a leaf of a bambe tree and does consistency checks.
     Returns the number of the parent read in. */

  int taxa,tnode,nLeft,nRight,nParent;

  if(!(f >> taxa >> tnode >> nLeft >> nRight >> nParent >> branchLength)) {
    error << "Error: Premature end of data in tree file." << endError;
    quit(1);
  } 
  if(taxa != number+1) {
    error << "Error: Taxa is not the node number + 1." << endError;
    quit(1);
  }
  if(nLeft != -1 || nRight != -1) {
    error << "Error: Leaf has a child." << endError;
    quit(1);
  }
  int c;
  SKIPLINE(f,c);
  return nParent;
}

void Leaf::restrictSites(const SiteData& sd, 
			 const Vector<int>& parentFirstCanonSite,
			 const Vector<int>& parentCanonSites)
{
  /* Establish a mapping from the list of canonical, non-variable
     sites of the node's parent to those of the the node, which must
     be a leaf. parentFirstCanonSite[t] is the number of the first
     canonical, non-variable site with tag t in the parent's list of
     such sites. parentCanonSites[n] is the site number (in the global
     array sites) of the nth canonical site of the parent. Sets
     lsite[i] to be the index of the node's portion of the
     loglikelihood calculation for the parent for site i which is
     stored in the node's frag array. lsite is set to the index of the
     oneFrag element (numTags*NUM_BASES) if the site restricted to the
     node is a gap, to an element of the ufrag segment (>
     numTags*NUM_BASES) if the site restricted to the node is an
     uncertain base, and otherwise to and element of the kfrag segment
     (< numTags*NUM_BASES). firstCanonSite and canonSites of the node
     are set. */

  int offset = 1 + sd.getNumTags()*NUM_BASES;

  for(int tag=0;tag<sd.getNumTags();tag++) 
    for(int i=parentFirstCanonSite[tag];i<parentFirstCanonSite[tag+1];i++) {
      int j = parentCanonSites[i];
      int sym = sd.getBase(number)[j];
      int cb;
      if(sym == GAP)
	lsites[i] = sd.getNumTags()*NUM_BASES;
      else if(singleBase(sym,cb))
	lsites[i] = tag*NUM_BASES + cb;
      else
	lsites[i] = offset + sd.getUncertainPos(number)[tag][sym];
    }
}

void Leaf::findPl(const SiteData& sd, const Parameters& params) 
{ 
  /* Does the partial likelihood calculation for a leaf.
     Sets ufrag, kfrag, and gap segments of the frag array. */

  Frag& myFrag = *frag;
  params.findPtable(myFrag,branchLength); 		// find kfrags.
  myFrag[sd.getNumTags()*NUM_BASES] = sd.getOneFrag();  // set gap frag.
  for(int i=0, j=sd.getNumTags()*NUM_BASES+1;		// calculate ufrags.
      i<sd.getNumUncertain(number);
      i++, j++) { 
    int k = sd.getUncertainBase(number)[i]; 
    int tag = sd.getUncertainTag(number)[i]; 
    for(int pb=0;pb<NUM_BASES;pb++) { 
      myFrag[j][pb] = 0;
      for(int p=0,m=0x1;p<NUM_BASES;p++,m=(m<<1)) 
 	if(k & m) 
 	  myFrag[j][pb] += myFrag[tag*NUM_BASES+p][pb]; 
    } 
  }
} 

int Internal::readBambe(istream& f, Vector<Node*>& nodes)
{
  /* Reads in an internal node of a bambe tree and does consistency checks.
     nodes is an array of pointers to the nodes of the tree.
     Returns the number of the parent read in. */

  int taxa,tnode,nLeft,nRight,nParent;

  if(!(f >> taxa >> tnode >> nLeft >> nRight >> nParent 
       >> branchLength)) {
    error << "Error: Premature end of data in tree file." << endError;
    quit(1);
  } 
  if(taxa != -1) {
    error << "Error: Taxa is not -1." << endError;
    quit(1);
  }
  if(nLeft == -1 || nRight == -1) {
    error << "Error: Internal leaf doesn't have two children." << endError;
    quit(1);
  }
  left = nodes[nLeft];
  right = nodes[nRight];
  nodes[nLeft]->parent =  nodes[nRight]->parent = this;
  int c;
  SKIPLINE(f,c);
  return nParent;
}

void Internal::restrictSites(const SiteData& sd, 
			     const Vector<int>& parentFirstCanonSite,
			     const Vector<int>& parentCanonSites)
{
  /* Establish a mapping from the list of canonical, non-variable
     sites of the node's parent to those of the the node, which is an
     internal node.  parentFirstCanonSite[t] is the number of the
     first canonical, non-variable site with tag t in the parent's
     list of such sites. parentCanonSites[n] is the site number (in
     the global array sites) of the nth canonical site of the
     parent. Sets lsite[i] to point to the node's portion of the
     loglikelihood calculation for the parent for site i which is
     stored in the node's frag array. lsite is set to the index of an
     element of the kfrag segment (< numTags*NUM_BASES) if the site
     restricted to the node is constant. Otherwise, lsite is set to an
     index of the vfrag segment (>= numTags*NUM_BASES). firstCanonSite
     and canonSites of the node are set. */

  Site constMask[NUM_BASES];
  for(int b=0;b<NUM_BASES;b++) {
    siteAnd(constMask[b],sd.getConstantMask(b),mask);
  }
  // sl is used to sort sites after restriction for finding canonical ones.
  Vector<NumberedSite> sl(parentFirstCanonSite[sd.getNumTags()]); 

  int offset = sd.getNumTags()*NUM_BASES;

  int numCanon = 0;
  firstCanonSite[0] = 0;

  for(int tag=0;tag<sd.getNumTags();tag++) {
    int numNonconst = 0;
    for(int i=parentFirstCanonSite[tag];i<parentFirstCanonSite[tag+1];i++) {
      int j = parentCanonSites[i];
      siteAnd(sl[numNonconst].site,sd.getUniqSite(j),mask);
      int b;
      if((b=siteMember(sl[numNonconst].site,constMask,NUM_BASES))<NUM_BASES)
	lsites[i] = tag*NUM_BASES + b;
      else {
	sl[numNonconst].n = i;
	sl[numNonconst++].index = j;
      }
    }

    if(numNonconst>0) {
      sl.sort(numNonconst,cmpNumberedSites);
      canonSites[numCanon] = sl[0].index;
      lsites[sl[0].n] = offset + numCanon++;
      for(int i=1;i<numNonconst;i++)
	if(sl[i].site == sl[i-1].site) {
	  lsites[sl[i].n] = lsites[sl[i-1].n];
	}
	else {
	  canonSites[numCanon] = sl[i].index;
	  lsites[sl[i].n] = offset + numCanon++;
	}
    }
    firstCanonSite[tag+1] = numCanon;
  }
}

void Internal::restrictAllSites(const SiteData& sd)
{
  /* The node cannot be the root. Recursively restricts the sites of all 
     nodes below, but not including the node. */
  
  left->restrictSitesNonroot(sd,this);
  left->restrictAllSites(sd);
  right->restrictSitesNonroot(sd,this);
  right->restrictAllSites(sd);
}

void Internal::findPl(const SiteData& sd, const Parameters& params)
{ 
  /* Does the partial likelihood calculation for an internal node.
     Sets kfrag and vfrag segments of the frag array. */

  int offset = sd.getNumTags() * NUM_BASES;

  Vector<BaseArray> ptab(sd.getNumTags()*NUM_BASES);
  params.findPtable(ptab,branchLength);

  Frag& myFrag = *frag;
  Frag& leftFrag = *(left->frag);
  Frag& rightFrag = *(right->frag);

  for(int i=0;i<numFrags();i++)
    for(int b=0;b<NUM_BASES;b++)
      myFrag[i][b] = 0;

  // Calculate frags for constant sites (kfrag).

  for(int k=0;k<sd.getNumTags()*NUM_BASES;k+=NUM_BASES) {
    for(int j=k;j<k+NUM_BASES;j++) {
      BaseArray& lb = leftFrag[j];
      BaseArray& rb = rightFrag[j];
      for(int b=0,r=k;b<NUM_BASES;b++,r++) {
	// r == k+b
	double p = lb[b] * rb[b];
	for(int pb=0;pb<NUM_BASES;pb++)
	  myFrag[j][pb] += ptab[r][pb] * p;
      }
    }
  }

  // Calculate frags for variable sites (vfrag).

  for(int tag2=0,j=0;tag2<sd.getNumTags();tag2++,j+=NUM_BASES) {
   for(int i=firstCanonSite[tag2],m=offset+i;
       i<firstCanonSite[tag2+1]; 
       i++,m++) {
     // m == i+offset
     BaseArray& lb = leftFrag[left->lsites[i]];
     BaseArray& rb = rightFrag[right->lsites[i]];
     for(int b=0,r=j;b<NUM_BASES;b++,r++) {
       // r == j+b
       double p = lb[b] * rb[b];
	for(int pb=0;pb<NUM_BASES;pb++)
	  myFrag[m][pb] += ptab[r][pb] * p;
     }
   }
  }
}

void Internal::findAllPl(const SiteData& sd, const Parameters& params)
{ 
  /* Does the partial likelihood calculation for all nodes of the tree
     rooted at the node. Should not be called on the root. */
  
  left->findAllPl(sd,params);
  right->findAllPl(sd,params);
  findPl(sd,params);
} 

Internal* Internal::saveSubtree(Vector<Node*>& nodes)
{
  /* Copies the node, its children, parent, and sibling. The copies are linked
     to one another appropriately. Links to nodes outside the copied ones
     are retained. */

  Internal* copy = (Internal*)(nodes[number]);
  copy->copyOneNode(*this);
  copy->left = nodes[left->number];
  copy->right = nodes[right->number];
  copy->parent = nodes[parent->number];
  copy->left->copyOneNode(*left);
  copy->right->copyOneNode(*right);
  copy->left->parent = copy->right->parent = copy;
  copy->parent->copyOneNode(*parent);
  if(parent->left == this) {
    copy->parent->left = copy;
    copy->parent->right = nodes[parent->right->number];
    copy->parent->right->copyOneNode(*(parent->right));
  }
  else {
    copy->parent->right = copy;
    copy->parent->left = nodes[parent->left->number];
    copy->parent->left->copyOneNode(*(parent->left));
  }
  copy->parent->left->parent = copy->parent->right->parent = copy->parent;
  return copy;
}

void Internal::replaceSubtreeClock(Vector<Node*>& nodes)
{
  /* Moves the node, its children, sibling, and all ancestors by
     the to the array nodes (essentially the list of free nodes). */
  
  nodes[left->number] = left;
  nodes[right->number] = right;
  Node* sib = getSib();
  nodes[sib->number] = sib;
  for(Node *m=this,*p;m;m=p) {
    p = m->parent;
    nodes[m->number] = m;
  }
}

void Internal::findAncestorsPl(const SiteData& sd, const Parameters& params,
			       Internal*& copy, const Internal*& root,
			       Internal*& rootCopy, double& loglikelihood,
			       Vector<Node*>& nodes)
{
  /* Calculates the loglikelihood of the parent of the node and all its
     ancestors. Also saves a copy of all ancestors (except the parent)
     and links the copies together appropriately. copy is a copy of the node.
     Sets rootCopy to be a copy of the root. */

  Internal *n, *cn, *p, *cp;
  for(n=(Internal*)parent,cn=(Internal*)(copy->parent); n!=root; n=p, cn=cp) {
    p = (Internal*)(n->parent);
    n->findPl(sd,params);
    cp = (Internal*)(nodes[p->number]);
    cp->copyOneNode(*p);
    if(p->left == n) {
      cp->left = cn;
      cp->right = p->right;
    }
    else {
      cp->left = p->left;
      cp->right = cn;
    }
    cn->parent = cp;
  }
  rootCopy = (Internal*)cn;
  loglikelihood = root->findPlRoot(sd,params);
}

void Internal::resetParentsClock(Node* root)
{
  /* Sets the parent pointers of the children of the node, the
     children of its sibling and the siblings of all ancestors correctly. */

  if(left->left)
    left->left->parent = left->right->parent = left;
  if(right->left)
    right->left->parent = right->right->parent = right;
  Node* sib = getSib();
  if(sib->left)
    sib->left->parent = sib->right->parent = sib;
  for(Node *m=parent;m!=root;m=m->parent)
    (m->getSib())->parent = m->parent;
}

int cmpHeights(const void *n1,const void *n2)
{  
  double c =(*((Node **)n1))->height - (*((Node **)n2))->height;
  return (c<0 ? -1 : (c>0 ? 1 : 0));
}


double hastingsBeta(double c, double x, double y, double eta)
  /* c is the scale of the scaled Beta distribution on [0,c].
     x is the current state and the mean of the scaled Beta distribution
     y is the proposed state
     eta = alpha + beta is a tuning parameter for the Beta distribution.
     
     return q(y,x) / q(x,y) */
{
  if(y > c)
    return 0;
  double alphaX = x*eta/c;
  double betaX = eta - alphaX;
  double alphaY = y*eta/c;
  double betaY = eta - alphaY;
  double lhr = Rand::lgamma(alphaX) - Rand::lgamma(alphaY)
      + Rand::lgamma(betaX) - Rand::lgamma(betaY)
      + (alphaY - 1) * log(x) - (alphaX - 1) * log(y)
      + (betaY - 1) * log(c-x) - (betaX - 1) * log(c-y);
  return SAFE_EXP(lhr);
}

double hastingsBetaPair(double cx, double cy, double x1, double y1, double x2, 
                          double y2, double eta)
  /* <x1,y1> is the current state with x1 > y1.
     <x2,y2> is the proposed state with x2 > y2.
     cx is the scale of the larger scaled Beta distribution on [0,cx].
     cy is the scale of the smaller scaled Beta distribution on [0,cy].
     x1 is the mean of the scaled Beta distribution on [0,cx]
     y1 is the mean of the scaled Beta distribution on [0,cy]
     eta = alpha + beta is a tuning parameter for the Beta distribution.
     
     return q(<x2,y2>,<x1,y1>) / q(<x1,y1>,<x1,y1>) */
{
  double alphaX1 = x1*eta/cx;
  double betaX1 = eta - alphaX1;
  double alphaX2 = x2*eta/cx;
  double betaX2 = eta - alphaX2;
  double alphaY1 = y1*eta/cy;
  double betaY1 = eta - alphaY1;
  double alphaY2 = y2*eta/cy;
  double betaY2 = eta - alphaY2;
  
  double ax1 = alphaX1 - 1;
  double bx1 = betaX1 - 1;
  double ax2 = alphaX2 - 1;
  double bx2 = betaX2 - 1;
  double ay1 = alphaY1 - 1;
  double by1 = betaY1 - 1;
  double ay2 = alphaY2 - 1;
  double by2 = betaY2 - 1;
  
  double logconst = Rand::lgamma(alphaX1) - Rand::lgamma(alphaX2)
    + Rand::lgamma(betaX1) - Rand::lgamma(betaX2)
    + Rand::lgamma(alphaY1) - Rand::lgamma(alphaY2)
    + Rand::lgamma(betaY1) - Rand::lgamma(betaY2);
  
  double num = pow(x1,ax2) * pow(cx-x1,bx2) * pow(y1,ay2) * pow(cy-y1,by2);
  double den = pow(x2,ax1) * pow(cx-x2,bx1) * pow(y2,ay1) * pow(cy-y2,by1);

  if(x1 < cy)
    num += pow(y1,ax2) * pow(cx-y1,bx2) * pow(x1,ay2) * pow(cy-x1,by2);
  if(x2 < cy)
    den += pow(y2,ax1) * pow(cx-y2,bx1) * pow(x2,ay1) * pow(cy-x2,by1);

  return SAFE_EXP(logconst) * num / den;
}

double Internal::localUpdateClock(const RunSettings& rs, const SiteData& sd,
				  const Parameters& params,
				  const Internal* root, Internal*& copy,
				  Internal*& rootCopy, double& hr,
				  Vector<Node*>& nodes)
{
  /* 
   * ---------------------------------------------------------------

   * The node is the child node of a randomly chosen internal edge.
   * The tree is locally changed, keeping the sum of all branch 
   * lengths constant.
   *
   *    l   r
   *     \ /
   *      *   s   
   *       \ /
   *        p
   *
   */

  copy = saveSubtree(nodes);
  Node* sibling = getSib();
  Node* newSibling;

  double h = (parent == (Node*)root ? 0 : parent->branchLength);
  Node *min=left, *mid=right, *max=sibling;
  min->height = min->branchLength + branchLength + h;
  mid->height = mid->branchLength + branchLength + h;
  max->height = max->branchLength + h;
  double oldX = branchLength + h;
  double oldY = h;

  threeSortNode(min,mid,max);

  double minHeight = min->height;
  double cx = mid->height;
  double cy = minHeight;
  double x,y;
  if(parent == (Node*)root) {
    min->height = minHeight * 
      SAFE_EXP(rs.getLocalTune() * (Rand::runif() - 0.5));
    mid->height += min->height - minHeight;
    cx = mid->height;
    max->height += min->height - minHeight;
    oldX += min->height - minHeight;
    y = 0;
    if(rs.getUseBeta())
      x = Rand::rbeta(oldX*rs.getBetaTune()/cx,(cx-oldX)*rs.getBetaTune()/cx)
	* cx;
    else 
      x = Rand::runif() * mid->height;
  }
  else if(rs.getUseBeta()) {
    y = Rand::rbeta(oldY*rs.getBetaTune()/cy,(cy-oldY)*rs.getBetaTune()/cy) 
      * cy;
    x = Rand::rbeta(oldX*rs.getBetaTune()/cx,(cx-oldX)*rs.getBetaTune()/cx)
      * cx;
  }
  else {
    y = Rand::runif() * minHeight;
    x = Rand::runif() * mid->height;
  }

  if(x < y)
    { double t = x; x = y; y = t; }

  Node* chosen;
  if(x > min->height) {
    chosen = min; // chosen is the child to be closest to the gp.
    hr = (branchLength+h > minHeight ? 1.0 : 1.0/3.0);
  }
  else {
    int choice = (int)(Rand::runif()*3);
    chosen = (choice==0 ? min : (choice==1 ? mid : max));
    hr = (branchLength+h > minHeight ? 3.0 : 1.0);
  }
  branchLength = x - y;
  //parent->branchLength = y;  
  //if(parent == NULL)
  parent->branchLength = y;  
  if(parent == (Node*)root) {
    hr *= (min->height * min->height) / (minHeight * minHeight);
    if(rs.getUseBeta()) 
      hr *= hastingsBeta(cx,oldX,x,rs.getBetaTune());
  }
  else if(rs.getUseBeta())
    hr *= hastingsBetaPair(cx,cy,oldX,oldY,x,y,rs.getBetaTune());

  if(chosen == sibling) { // Topology does not change.
    newSibling = sibling;
    left->branchLength = left->height - x;
    right->branchLength = right->height - x;
    sibling->branchLength = sibling->height - y;
  }
  else { // Topology changes
    newSibling = chosen;
    Node* other = chosen==left ? right : left;
    if(parent->left==sibling)
      if(chosen==left)
	swapNode(parent->left,left);
      else
	swapNode(parent->left,right);
    else if(chosen==left)
      swapNode(parent->right,left);
    else
      swapNode(parent->right,right);
    //swapNode(parent->left == sibling ? 
    //parent->left : parent->right,
    //chosen==left ? left : right);
    swapNode(chosen->parent,sibling->parent);
    chosen->branchLength = chosen->height - y;
    sibling->branchLength = sibling->height - x;
    other->branchLength = other->height - x;

    siteOr(*intMask,other->mask,sibling->mask);
    if(parent == (Node*)root) {
      this->restrictSitesRoot(sd);
      chosen->restrictSitesRoot(sd);
    }
    else {
      this->restrictSitesNonroot(sd,parent);
      chosen->restrictSitesNonroot(sd,parent);
    }
    sibling->restrictSitesNonroot(sd,this);
    other->restrictSitesNonroot(sd,this);
  }
  left->findPl(sd,params);
  right->findPl(sd,params);
  newSibling->findPl(sd,params);
  findPl(sd,params);
  double loglikelihood;
  findAncestorsPl(sd,params,copy,root,rootCopy,loglikelihood,nodes);
  return loglikelihood;
}

void findDist(const RunSettings& rs, double m, double r, double& y, double& hr)
{
  if(rs.getUseBeta()) {
    double alpha = r * rs.getBetaTune();
    y = Rand::rbeta(alpha,rs.getBetaTune()-alpha) * m;
    hr *= hastingsBeta(m,r*m,y,rs.getBetaTune());
  }
  else
    y = Rand::runif() * m;
}

void Internal::changeDistances(const RunSettings& rs, double& distAN, 
			       double& distNE, double& distEC,
			       double &hr, int& rotated)
{
  double x,y;
  double distAE = distAN + distNE;
  double distAC = distAE + distEC;
  double m = distAC * SAFE_EXP(rs.getLocalTune() * (Rand::runif()-0.5));
  double mratio = m / distAC;
  hr = mratio * mratio;
  if(Rand::runif() < 0.5) {
    x = distAN * mratio;
    findDist(rs,m,distAE/distAC,y,hr);
  }
  else {
    y = distAE * mratio;
    findDist(rs,m,distAN/distAC,x,hr);
  }
  rotated = (y < x);
  if(rotated)
    { double t = x; x = y; y = t; }
  distAN = x;
  distNE = y - x;
  distEC = m - y;
}

void Internal::replaceSubtreeNonClock(int topology, Vector<Node*>& nodes)
{
  switch(topology) {
  case 1: {
    Node* sib = getSib(); 
    nodes[sib->left->number] = sib->left;
    nodes[sib->right->number] = sib->right;
  }
  break;
  case 2: { 
    Node* sib = parent->getSib();
    nodes[sib->number] = sib;
    break;
  }
  case 3: break;
  }
  replaceSubtreeClock(nodes);
}


void Internal::resetParentsNonClock(Node* root,int topology)
{
  switch(topology) {
  case 1: {
    if(left->left)
      left->left->parent = left->right->parent = left;
    if(right->left)
      right->left->parent = right->right->parent = right;
    Node* sib = getSib(); 
    if(sib->left->left)
      sib->left->left->parent = sib->left->right->parent = sib->left;
    if(sib->right->left)
      sib->right->left->parent = sib->right->right->parent = sib->right;
    break;
  }
  case 2: {
    if(left->left)
      left->left->parent = left->right->parent = left;
    if(right->left)
      right->left->parent = right->right->parent = right;
    Node* sib = getSib();
    if(sib->left)
      sib->left->parent = sib->right->parent = sib;
    Node* u = parent->getSib();
    if(u->left) 
      u->left->parent = u->right->parent = u;
    break;
  }
  case 3:  resetParentsClock(root);  break;
  }
}

double Internal::localUpdateNonClock(const RunSettings& rs, 
				     const SiteData& sd, 
				     const Parameters& params,
				     const Internal* root, Internal*& copy,
				     Internal*& rootCopy, int& topology, 
				     double& hr, Vector<Node*>& nodes)
{
  /* 
   * ---------------------------------------------------------------
   * Update does not depend on rooting of the tree.
   * n is the child node of a randomly chosen internal edge.
   * The tree is locally changed, keeping the sum of all branch 
   * lengths constant.
   * if n is a child of the root, e is the other child of the root.
   * In the unrooted tree, randomly move n and e between a and c.
   *
   *        b  d
   *        |  |
   *     a--n--e--c
   *
   * The two children of n are randomly assigned to be a and b.
   * If e is the parent of n, n's sibling and grandparent are randomly
   * chosen to be c and d.
   * If e is n's sibling (because n's parent is the root), the sibling's
   * two children are randomly chosen to be c and d.
   * The two nodes are conceptually equivalent in the unrooted tree.
   * If n is randomly set below e in the stored rooted tree, relabel them
   * to make the code for updating masks easier.
   */

  /* Make copies of the affected nodes. */
  copy = saveSubtree(nodes);
  Node*& sibling = (Node*&)(getSib());
  Node*& a = (Node*&)(Rand::runif() >= 0.5 ? left : right);
  Node*& b = (Node*&)(a->getSib());
  int choice = (Rand::runif() >= 0.5); 	// Which neighbor is called c.
  int rotated;
  double loglikelihood;

  if(parent == (Node*)root) {
    /*     
     *    l   r u   v   (a,b) = (l,r) or (r,l)
     *     \ /   \ /    (c,d) = (u,v) or (v,u)
     *      *     s         e = s
     *       \   /          n = *this
     *        \ /
     *         p==root
     */
    topology = 1;
    Node* savedSib = copy->getSib();
    savedSib->left = nodes[sibling->left->number];
    savedSib->right = nodes[sibling->right->number];
    savedSib->left->copyOneNode(*(sibling->left));
    savedSib->right->copyOneNode(*(sibling->right));
    savedSib->left->parent = savedSib->right->parent = savedSib;
    Node*& c = (Node*&)(choice ? sibling->left : sibling->right);
    Node*& d = (Node*&)(choice ? sibling->right : sibling->left);
    double bl = branchLength + sibling->branchLength;
    changeDistances(rs,a->branchLength,bl,c->branchLength,hr,rotated);
    branchLength = sibling->branchLength = bl / 2;
    if(rotated) {
      swapNode(b,d);
      b->parent = this;
      d->parent = sibling;
      siteOr(*intMask,a->mask,b->mask);
      siteOr(*sibling->intMask,c->mask,d->mask);
      restrictSitesRoot(sd);
      sibling->restrictSitesRoot(sd);
      a->restrictSitesNonroot(sd,this);
      b->restrictSitesNonroot(sd,this); 
      c->restrictSitesNonroot(sd,sibling);
      d->restrictSitesNonroot(sd,sibling);
    }
    a->findPl(sd,params);
    c->findPl(sd,params);
    findPl(sd,params);
    sibling->findPl(sd,params);
    findAncestorsPl(sd,params,copy,root,rootCopy,loglikelihood,nodes);
  }
  else if (parent->parent == (Node*)root) {
    /*     
     *  l   r           (a,b) = (l,r) or (r,l)
     *   \ /            (c,d) = (s,u) or (u,s)
     *    *   s             e = p
     *     \ /              n = *this
     *      p   u         
     *       \ /
     *        v==root
     */
    topology = 2;
    Node* savedU = nodes[parent->getSib()->number];
    savedU->copyOneNode(*(parent->getSib()));
    // savedU->parent, and the root copy's child pointer are set after the
    // root is saved.
    Node*& c = (choice ? (Node*&)sibling : (Node*&)(parent->getSib()));
    Node*& d = (choice ? (Node*&)(parent->getSib()) : (Node*&)sibling);
    if(choice) { // c = sibling, d = u;
      changeDistances(rs,a->branchLength,branchLength,c->branchLength,hr,
		      rotated);
    }
    else { // c = u, d = sibling.
      double bl = parent->branchLength + c->branchLength;
      changeDistances(rs,a->branchLength,branchLength,bl,hr,rotated);
      parent->branchLength = c->branchLength = bl / 2;
    }
    if(rotated) {
      if(choice) {
	swapNode(a,c);
	a->parent = this;
	c->parent = parent;
      }
      else {
	swapNode(b,d);
	b->parent = this;
	d->parent = parent;
      }
      siteOr(*intMask,a->mask,b->mask);
      parent->left->restrictSitesNonroot(sd,parent);
      parent->right->restrictSitesNonroot(sd,parent);
      left->restrictSitesNonroot(sd,this);
      right->restrictSitesNonroot(sd,this); 
      if(!choice) {
	b->findPl(sd,params);
	d->findPl(sd,params);
      }
    }
    a->findPl(sd,params);
    c->findPl(sd,params);
    findPl(sd,params);
    findAncestorsPl(sd,params,copy,root,rootCopy,loglikelihood,nodes);
    savedU->parent = rootCopy;
    (root->left==parent ? rootCopy->right : rootCopy->left) = savedU;
  }
  else {
    /*     
     *  l   r           (a,b) = (l,r) or (r,l)
     *   \ /            (c,d) = (s,u) or (u,s)
     *    *   s             e = p
     *     \ /              n = *this
     *      p          
     *       \
     *        u!=root
     */
    topology = 3;
    Node*& c = (choice ? (Node*&)sibling : (Node*&)(parent->parent));
    Node*& d = (choice ? (Node*&)(parent->parent) : (Node*&)sibling);
    changeDistances(rs,a->branchLength,branchLength,
		    (choice ? sibling->branchLength : parent->branchLength),
		    hr,rotated);
    if(rotated) { 
      if(choice) {
	swapNode(a,c);
	a->parent = this;
	c->parent = parent;
      }
      else {
	swapNode(b,d);
	b->parent = this;
	d->parent = parent;
      }
      siteOr(*intMask,a->mask,b->mask);
      parent->left->restrictSitesNonroot(sd,parent);
      parent->right->restrictSitesNonroot(sd,parent);
      left->restrictSitesNonroot(sd,this);
      right->restrictSitesNonroot(sd,this); 
      if(choice) 
	c->findPl(sd,params);
      else {
	b->findPl(sd,params);
	d->findPl(sd,params);
      }
    }
    else if(choice) // !rotated
      c->findPl(sd,params);
    a->findPl(sd,params);
    findPl(sd,params);
    findAncestorsPl(sd,params,copy,root,rootCopy,loglikelihood,nodes);
  }
  return loglikelihood;
}

double Internal::findPlRoot(const SiteData& sd, const Parameters& params) const
{ 
  /* Calculates the partial likelihood for the node (which should be
     the root of the tree) assuming that the partial likelihood
     calculations have been done for the rest of the tree. */

  double osum = 0; 
  Frag& myFrag = *frag;
  Frag& leftFrag = *(left->frag);
  Frag& rightFrag = *(right->frag);

  // Calculate frags for constant sites (kfrag). 

  for(int tag1=0,k=0;tag1<sd.getNumTags();tag1++,k+=NUM_BASES)
    for(int bs=0;bs<NUM_BASES;bs++) { 
      BaseArray& lb = leftFrag[k+bs];
      BaseArray& rb = rightFrag[k+bs];
      double isum = 0; 
      for(int b=0;b<NUM_BASES;b++)
 	isum += params.getParam(tag1).pi[b] * lb[b] * rb[b];
      if(params.getUseInvariantSites()) {
	double prob = params.getParam(tag1).invariantProb;
	osum += sd.getRootConstSite(tag1)[bs] * log(isum * (1-prob) 
	        + prob * params.getParam(tag1).pi[bs]);
      }
      else {
	osum += sd.getRootConstSite(tag1)[bs] * log(isum); 
      }
    }

  // Calculate frags for variables sites (vfrag). 

  for(int tag2=0;tag2<sd.getNumTags();tag2++) {
    for(int i=sd.getFirstSite(tag2);i<sd.getFirstSite(tag2+1);i++) { 
      BaseArray& lb = leftFrag[left->lsites[i]];
      BaseArray& rb = rightFrag[right->lsites[i]];
      double isum = 0; 
      for(int b=0;b<NUM_BASES;b++)
 	isum += (params.getParam(tag2)).pi[b] * lb[b] * rb[b];
      osum += sd.getSiteCount(i) * log(isum); 
    }
    if(params.getUseInvariantSites())
      osum += log(1 - params.getParam(tag2).invariantProb) 
	* sd.getNumVariableSites(tag2);
  }
  return osum;
} 

void Internal::checkParents(int numNodes, Vector<Node*>& nodes, 
			Vector<int>& par)
{
  // Check that the parents of the nodes match the nodes found int
  // the parent array (after reading in a bambe file).

  for(int i=0;i<numNodes;i++)
    if(!(nodes[i] == this ? 
	 nodes[i]->parent == NULL : 
	 nodes[i]->parent == nodes[par[i]])) {
      error << "Error: Failed parent consistency check." << endError;
      quit(1);
    }
}

Internal* Internal::chooseInternalNode(int numLeaves)
{
  // Returns an internal node other than the root. 

  int n = (int)(Rand::runif()*(numLeaves-2)+numLeaves);
  if(n >= number)
    n++;
  Node* node;
  findNumber(n,node);
  return (Internal*)node;
}

Internal* Internal::chooseInternalEdge(int numLeaves)
{
  // Returns a node that is an endpoint of an internal edge.

  int bad1 = number;
  int bad2 = (right->left==NULL ? left->number : right->number);
  if(bad1 > bad2)
    swapInt(bad1,bad2);
  int n = (int)(Rand::runif()*(numLeaves-3)+numLeaves);
  if(n >= bad1)
    n++;
  if(n >= bad2)
    n++;
  Node* node;
  findNumber(n,node);
  return (Internal*)node;
}

void splitBranchInt(int x, Node* r, Node* s, Node*& lft, Node*& rght)
{
  /* The subtree rooted at r contains x. s is the sibling of r. dir
     of the nodes on the path to x contain the directions from the node 
     to x (left=1, right=0). Sets lft to the node containing x and rght
     to the root of the rest of the reordered tree. */

  if(r->number==x) {
    r->branchLength = s->branchLength = s->branchLength/2;
    lft = r;
    rght = s;
  }
  else {
    Node*& c = (r->dir ? r->left : r->right);
    Node* t = c;
    c = s;
    s->parent = r;
    r->branchLength = t->branchLength;
    splitBranchInt(x,t,r,lft,rght);
  }
}

Node* Internal::splitBranch(int x)
{
  /* The leaf containing taxon x is made the left child of this node.
     This node should not initially contain taxon x. 
     Returns the lowest changed node of the reordered tree. */

  Node* n;
  findNumber(x,n);
  Node *r = (dir?left:right);	// The child whose subtree contains x.
  Node *s = (dir?right:left);	// The other child.
  s->branchLength = r->branchLength + s->branchLength;
  splitBranchInt(x,r,s,left,right);
  left->parent = right->parent = this;
  return s;
}

void Internal::rerootTree(const SiteData& sd, const Parameters& params,
			  int numNodes) 
{
  /* Randomly reroots the tree rooted at the node. Called on the root.*/

  int newRootChild = (int)(Rand::runif()*(numNodes-3));
  int a = number;			// Randomly choose a node other than
  int b = left->number;			// the root or its children.
  int c = right->number;
  threeSortInt(a,b,c);
  if(newRootChild >= a)
    newRootChild++;
  if(newRootChild >= b)
    newRootChild++;
  if(newRootChild >= c)
    newRootChild++;
  Node* s = splitBranch(newRootChild);// Split the tree so that  
  // node becomes a child of the
  // root s is the lowest
  // changed node.
  int numAncest=0;		
  Vector<Node*> ancestors(numNodes);

  for(Node* n=s->parent;             // Redo the calculations for s
      n!=NULL;				        // and all ancestors of s.
      n=n->parent) {
    ancestors[numAncest++] = n;		       
    siteOr(*n->intMask,n->left->mask,n->right->mask);
  }
  left->restrictSitesRoot(sd);
  right->restrictSitesRoot(sd);
  for(int j1=numAncest-2;j1>=0;j1--) {
    ancestors[j1]->left->restrictSitesNonroot(sd,ancestors[j1]);
    ancestors[j1]->right->restrictSitesNonroot(sd,ancestors[j1]);
  }
  if(s->left) {
    s->left->restrictSitesNonroot(sd,s);
    s->right->restrictSitesNonroot(sd,s);
  }
  for(int j2=0;j2<numAncest;j2++) {
    ancestors[j2]->left->findPl(sd,params);
    ancestors[j2]->right->findPl(sd,params);
  }
}

double Internal::findLogLikelihood(const SiteData& sd, 
				   const Parameters& params)
{
  /* Sets all masks of all the nodes of the tree rooted at this node.
     Restricts the sites of all nodes and finds the log likelihood of the 
     tree rooted at the node. */

  propagateMasks();
  left->findRootChildLl(sd,params);
  right->findRootChildLl(sd,params);
  return findPlRoot(sd,params);
}


