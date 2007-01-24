#ifndef NODEHDR
#define NODEHDR

#include "util.h"
#include "runsettings.h"
#include "sitedata.h"
#include "parameters.h"
#include "site.h"

class DlistItem;
class Dlist;
class Leaf;
class Internal;
class Neighbor;

// frag[numUniqueSites]: frag[i][b] is the stored partial calculation
// for the node and site i. This stored result is used for calculating
// the likelihood for the parent, say n, for base b, and for a site j
// of n whose restriction to the node is i. The layout of the frag
// array is different for leaves and internal nodes. The frag array
// contains part of the stored partial likelikehood calculation for
// the parent of the node for each site and for constant bases, and in
// the case of leaves, for the gap and for any uncertain bases. For
// leaves, the layout of the frag array is: constant sites, gap,
// uncertain bases. For internal nodes, the layout is: constant sites,
// variable sites. Note that there are numTags*NUM_BASES constant
// sites, and sd.numUncertain[n] uncertain bases for the leaf holding
// taxon n, so the size of frag for a leaf is 1 + numTags*NUM_BASES +
// sd.numUncertain[n]. The size of frag for an internal node is
// numTags*NUM_BASES + number of unique variable sites.

class Frag : public Vector<BaseArray> {
 public:
  Frag(int n) : Vector<BaseArray>(n) {}
};

class Node {
  friend class Leaf;
  friend class Internal;
 public:
  Node(int i, const char * const  & taxaNam, int nFrags0, int nFrags, 
       int nUniqueSites, int nTags, Site* mask1, const Site& mask2) :
    number(i),
    taxaName(taxaNam),
    numFrags0(nFrags0),
    frag(new Frag(nFrags)),
    canonSites(nUniqueSites),
    firstCanonSite(nTags+1),
    numCanonSites(firstCanonSite[nTags]),
    lsites(nUniqueSites),
    intMask(mask1),
    mask(mask1 ? *mask1 : mask2)
    {}

  ~Node() { delete intMask; delete frag; }

  int getNumber() const 		{ return number; }

  double getHeight() const 		{ return height; }

  void setChildren(Node *l, Node *r)    { left = l; right = r; }

  void setParent(Node *p) 	        { parent = p; }

  void setBranchLength(double b) 	{ branchLength = b; }

  void setHeight(double h) 		{ height = h; }

  const char * const getTaxaName() const { return taxaName; }

  virtual int setMinTaxa()=0;		// Store and return the smallest
  					// taxa of the nodes of the tree.

  virtual void setHeights(double,	// Set heights of all nodes of tree.
			  double&)=0;   // Returns height of subtree. This
					// height is only valid in clock case.

  virtual void checkHeights(double)=0;	// Leaf heights may differ due to
  					// rounding. If heights are close,
  					// they are standardized. (for clock)

  virtual void diameter(double&,	// Calculate height & diameter of tree.
			double&)=0;     // Does not assume the clock.

  virtual void jiggleHeights(		// Perturb the heights of the nodes.
    const RunSettings&,double)=0;

  virtual void setNodes(Vector<Node*>&) // sets nodes so the nodes[i]
    =0;					// points to the node numbered i.

  virtual int readBambe(istream&,	// Read in a node in bambe format.
			Vector<Node*>&)=0;

  virtual void printTopology(ostream&)  // Print out the tree topology of the
    =0;					// tree rooted at the node.

  virtual void print(ostream&,int)=0;	// Print out the tree rooted at the
  					// node in newick format. Prints taxa
  					// if flag is true, numbers o.w.

  void print(ostream& c)	   	// Print out the tree rooted at the
    { print(c,1); }			// node in newick format.

  virtual void printBambe(ostream&)=0;  // Print out node in bambe format.
  				        // setHeights must be called first.

  virtual void swapChildren()=0;	// Randomly swap children of the tree.

  virtual void setRandomHeight(		// Randomly set heights of the nodes.
    const RunSettings&)=0;

  friend Internal* makeTree(Dlist&,int);// Create a tree from a list of nodes.

  friend int lessThan (const Node* a,   // height of a < height of b.
		       const Node* b)
    { return (a->height < b->height); }

  virtual Node* copyNodes(		// Copies node of tree to array.
    Vector<Node*>&)=0;

  virtual void copyFrags(Vector<Frag*>&) const=0;

  virtual void restoreFrags(Vector<Frag*>&)=0;

  virtual void listNodes(Dlist&,int&)=0;// Store the nodes in the list by dfs.

  virtual int findNumber(int,Node*&)=0; // Finds the node with the given taxon
			     		// and saves directions to it.

  virtual void propagateMasks()=0;	// Set the masks of the nodes.

  virtual Node* copyTopology(		// Copies topology of nodes to array.
    Vector<Node*>&)=0;

  virtual double findPartialLikelihood(const SiteData&,const Parameters&)=0;
  
  virtual int isLeaf()=0;	        // Whether or not a leaf.

 protected:
  static const char * const noName; // Blank string.

  static const Site noMask;	// Fake mask.

  int number;		        // The number of the node.

  Node *left, *right;     	// left and right child of the node.

  Node *parent;		        // parent of the node.

  double branchLength;	        // length of branch to parent.

  const char * const taxaName; // name of the stored taxon. Blank for 
  				// interior nodes. 

  int minTaxa;			// smallest taxon number of the
  				// subtree rooted at this node.

  double height;		// height of the node in the tree.

  int dir;			// Direction towards a certain node.
				// Used when reordering tree.

  int numFrags0;		// number of elements of the frag array,
  				// excluding canonical variable sites.

  Frag *frag;			// frag array.

  Vector<int> lsites;	        // lsites[numUniqueSites][NUM_BASES]: 
    			        // partial calculation (vfrag or kfrag) 
  				// stored for the parent and the parent's  
  				// canonical site i.

  Vector<int> canonSites;       // canonSites[numUniqueSites]: canonSites[n]
  				// is the list of indices of the canonical
  				// sites for the node.

  Vector<int> firstCanonSite;	// firstCanonSite[numTags+1]:firstCanonSite[tg]
  				// is the first site in the list of canonical
  				// variable sites for the node whose tag is tg.
  				// firstCanonSite[numTags] is the number of
  				// canonical variable sites for the node.

  int& numCanonSites;		// number of canonical sites. Reference to
  				// firstCanonSite[numTags].

  Site* intMask;		// The mask of an internal node. 
				// This is here so that it will be deleted
  				// when the node is deleted.

  const Site& mask;		// A mask is a special type of site. The bits
  				// corresponding to the taxon in the subtree
  				// rooted at the node are 1, with all others
  				// being 0. A leaf's mask does not change. It
  				// is indexed by the node number. For an
  				// internal node, the only bits in a mask that
  				// are set are the ones that correspond to
  				// taxon of the leaves of the tree rooted at
  				// the node. mask of an internal mask is a
  				// reference to intMask, and to sd.taxonMask
  				// for a leaf.

  Node* copyOneNode(const Node& n) {
    // number, taxaName, and mask should be set when node is created.
    left = n.left; right = n.right; parent = n.parent;
    branchLength = n.branchLength;
    if(n.parent) { // n is not the root.
      numFrags0 = n.numFrags0;
      vcopy(*frag,*(n.frag),n.numFrags());
      firstCanonSite = n.firstCanonSite;
      vcopy(canonSites,n.canonSites,n.numCanonSites);
      if(n.parent->parent) // n is not a child of the root.
	vcopy(lsites,n.lsites,n.parent->numCanonSites);
      else // n is a child of the  root, so copy them all.
	lsites = n.lsites;
    }
    if(intMask)
      *intMask = *(n.intMask);
    return this;
  }

  void listOneNode(Dlist&, int);

  Node* copyOneTopology(Node& node) {
    // number and taxaName should be set when node is created.
    left = node.left;
    right = node.right;
    parent = node.parent;
    branchLength = node.branchLength;
    return this;
  }

  friend void splitBranchInt(int,// Splits the tree into two pieces
    Node*,Node*,Node*&,Node*&);  // based on where a taxon occurs.
    
  void printBL(ostream& f, int label) {	 
    // Print tree in newick format to stream f. Prints taxa name if label is
    // true, number o.w.
    print(f,label); 	 	// Prints branch length of all nodes. 
    f << ':' << setiosflags(ios::showpoint | ios::fixed) 
      << setprecision(6) << branchLength;
  }

  Node*& getSib() {		 // Returns the sibling of a node.
    return (parent->left == this ? parent->right : parent->left); 
  }
  
  friend int parentIndex(int,const Dlist&); // Index of node's parent in list.

  friend int leafParent(int,const Dlist&);  // Index of leaf's parent in list.

  friend void removeLeaf(int,int,Dlist&);   // Removes node from list.

  friend int cmpHeights(const void *,const void *);

  virtual void restrictSites(const SiteData&,const Vector<int>&, 
			     const Vector<int>&)=0;

  void restrictSitesRoot(const SiteData& sd) {
    /* Restricts the sites of a child of the root. */
    static IdentityVector id(sd.getNumUniqueSites());
    restrictSites(sd,sd.getFirstSite(),id);
  }

  void restrictSitesNonroot(const SiteData& sd, Node* par) {
    /* Restricts the sites of a child of a non-root node. */
    restrictSites(sd,par->firstCanonSite,par->canonSites);
  }

  virtual void restrictAllSites(const SiteData&)=0;

  virtual void findPl(const SiteData&,const Parameters&)=0;

  virtual void findAllPl(const SiteData&,const Parameters&)=0;

  void findRootChildLl(const SiteData& sd, const Parameters& params) {
    /* The node must be a child of the root. Restricts the sites of all
       nodes in the subtree rooted at node and calculates the partial
       likelihood for all nodes in the subtree. */
    restrictSitesRoot(sd);
    restrictAllSites(sd);
    findAllPl(sd,params);
  }

  virtual double findPlRoot(const SiteData&,const Parameters&) const=0;

  virtual int numFrags() const=0;// Returns the number of elements in frag.
};

class Leaf : public Node {
 public:
  Leaf(const SiteData& sd, int i) :
    Node(i,sd.getTaxaName(i),
	 1+sd.getNumTags()*NUM_BASES+sd.getNumUncertain(i),
	 1+sd.getNumTags()*NUM_BASES+sd.getNumUncertain(i),
	 sd.getNumUniqueSites(),sd.getNumTags(),NULL,sd.getTaxonMask(i)) {
    firstCanonSite[sd.getNumTags()] = 0; 
    left = right = NULL; 
  }

  Leaf(const Node * const n) : // used for printing
    Node(n->number,n->taxaName,0,0,0,0,NULL,noMask) {
    firstCanonSite[0] = 0; 
    left = right = NULL; 
  }

  int setMinTaxa() { return minTaxa = number; }

  void setHeights(double h, double& hgt) { height = hgt = h; }

  void checkHeights(double h) {
  /* Checks that the heights of all the leaves are almost the same (for the
     clock case). Due to round-off error in reading and writing tree
     files the heights may not be exactly equal. Resets branch lengths of
     leaves so the heights are exactly the same. h is the height of the 
     tree. */

    if(fabs(height-h)>1e-5) {
      error << "Error: Molecular clock does not hold for initial tree."
	    << endError;
      quit(1);
    }
    else
      branchLength += h-height;
  }

  void diameter(double& h, double& d) { h = d = 0; }

  void swapChildren() {}

  void printTopology(ostream& f) { f << number+1; }

  void print(ostream& f, int label) { 
    if(label)
      f << taxaName;
    else
      f << number+1;
  }

  int readBambe(istream&,Vector<Node*>&); // Read in a node in bambe format.

  void printBambe(ostream& f) {
    f << setiosflags(ios::showpoint | ios::fixed) << setw(4) << number+1
      << ' ' << setw(5) << number << ' ' << setw(6) << -1 << ' ' << setw(6) 
      << -1 << ' ' << setw(6) << (parent?parent->number:-1) << ' ' << setw(9)
      << setprecision(6) << branchLength << ' ' << setw(9) << height << endl;
  }

  void setNodes(Vector<Node*>& nodes) { nodes[number] = this; }

  void setRandomHeight(const RunSettings& rs) { height = rs.getMaxDepth(); }

  void jiggleHeights(const RunSettings&,double) {}

  Node* copyNodes(Vector<Node*>& nodes) {
    return nodes[number]->copyOneNode(*this);
  }

  Node* copyTopology(Vector<Node*>& nodes) {
    return nodes[number]->copyOneTopology(*this);
  }

  int findNumber(int x, Node*& n) {
  /* Finds the node with number x in the subtree rooted at the node.
     The directions to x are put into dir of the nodes on the path,
     where 1 means go left and 0 means go right. Returns 1 if taxon x
     is found, 0 otherwise. Sets n to the node if found. */

    if(number == x) {
      n = this;
      return 1;
    }
    return 0;
  }

  void propagateMasks() {}

  double findPartialLikelihood(const SiteData&,const Parameters&) {
    error << "Internal Error: findPartialLikelihood called on a leaf." << 
      endError;
    quit(1);
    return 1;
  }

  int isLeaf() { return 1; }

  int findDistance(Matrix<double>&,int,int) { return 0; }

private:
  void listNodes(Dlist& list, int& count) { 
    listOneNode(list,count); 
    count++;
  }

  void copyFrags(Vector<Frag*>& saved) const {
    vcopy(*(saved[number]),*frag,numFrags()); }

  void restoreFrags(Vector<Frag*>& saved) {
    Frag* t = frag; frag = saved[number]; saved[number] = t;
  }

  void findAllPl(const SiteData& sd, const Parameters& params) {
    findPl(sd,params);
  }

  void restrictSites(const SiteData&,const Vector<int>&,const Vector<int>&);

  void restrictAllSites(const SiteData&) {}

  void findPl(const SiteData&,const Parameters&);

  double findPlRoot(const SiteData&,const Parameters&) const { 
    error << "Internal Error: findPlRoot called on leaf" << endError;
    return 0; }

  int numFrags() const { return numFrags0; }
};

class Internal : public Node {
 public:
  Internal(const SiteData& sd, int i) :
    Node(i,noName,sd.getNumTags()*NUM_BASES,
	 sd.getNumTags()*NUM_BASES+sd.getNumUniqueSites(),
	 sd.getNumUniqueSites(),sd.getNumTags(),new Site(),noMask)
    {}
  
  Internal(const Node * const n) : // used for printing
    Node(n->number,noName,0,0,0,0,NULL,noMask)
    {}

  int setMinTaxa() { 
    return minTaxa = MINIMUM(left->setMinTaxa(),right->setMinTaxa()); 
  }

  void setHeights(double h, double& hgt) {
    height = h;
    left->setHeights(h+left->branchLength,hgt);
    right->setHeights(h+right->branchLength,hgt);
  }

  void checkHeights(double h) {
    left->checkHeights(h);
    right->checkHeights(h);
  }

  void diameter(double& h, double& d) {
    /* For the tree rooted at the node, sets h to the maximum distance from 
       a leaf to the node, and sets d to the diameter (maximum distance between
       any two nodes). Does not assume the clock model. */

    double h1,h2,d1,d2,l1,l2;
    left->diameter(h1,d1);
    right->diameter(h2,d2);
    l1 = h1 + left->branchLength;
    l2 = h2 + right->branchLength;
    h = MAXIMUM(l1,l2);
    d = MAXIMUM(MAXIMUM(d1,d2),l1+l2);
  }

  void swapChildren() {
    // Randomly swaps the children of the tree rooted at the node.
    if(Rand::runif() < 0.5) {
      Node* t = left; left = right; right = t;
    }
    left->swapChildren();
    right->swapChildren();
  }

  void printTopology(ostream& f) {
    // Print out the tree topology of the tree rooted at the node to stream f. 
    // Note that setMinTaxa must called before this function.

    f << '(';
    if (left->minTaxa < right->minTaxa) {
      left->printTopology(f);
      f << ','; 
      right->printTopology(f);
    }
    else {
      right->printTopology(f);
      f << ','; 
      left->printTopology(f);
    }
    f << ')';
  }

  void print(ostream& f, int label) {
    // Prints out the tree rooted at the node in newick format to stream f. 
    // Does not print out the branch length for this node, but does for all
    // other nodes in the tree. setMinTaxa should be called before this. 
    // Prints out the taxa names if label is true, numbers o.w.
    f << '(';
    if(left->minTaxa < right->minTaxa) {
      left->printBL(f,label);
      f << ',';
      right->printBL(f,label);
    }
    else {
      right->printBL(f,label);
      f << ',';
      left->printBL(f,label);
    }
    f << ')';
  }

  int readBambe(istream&,Vector<Node*>&); // Read in a node in bambe format.

  void printBambe(ostream& f) {
    f << setiosflags(ios::showpoint | ios::fixed) << setw(4) << -1 << ' ' 
      << setw(5) << number << ' ' << setw(6) << left->number << ' ' << setw(6)
      << right->number << ' ' << setw(6) << (parent?parent->number:-1) << ' ' 
      << setw(9) << setprecision(6) << branchLength << ' ' << setw(9)
      << height << endl;
  }

  void setNodes(Vector<Node*>& nodes) {
    nodes[number] = this;
    left->setNodes(nodes);
    right->setNodes(nodes);
  }

  void setRandomHeight(const RunSettings& rs) { 
    // Not recursive because called on node array before tree exists.
    height = rs.getMaxDepth() * Rand::runif();
  }

  Node* copyNodes(Vector<Node*>& nodes) {
    // Copies the tree rooted at the node. 
    nodes[number]->copyOneNode(*this);
    nodes[number]->left = left->copyNodes(nodes);
    nodes[number]->right = right->copyNodes(nodes);
    nodes[number]->left->parent = nodes[number]->right->parent = nodes[number];
    return nodes[number];
  }

  void jiggleHeights(const RunSettings& rs, double treeHeight) {
    /* Adds a uniform perturbation to each valley depth. The height fields are
       set to the new heights. Differences will be accurate and all heights
       are off by a common offset. treeHeight is the height of the tree. */

    height += (2 * Rand::runif() - 1) * rs.getValleyWin();
    if(height > treeHeight)
      height = 2 * treeHeight - height;
    left->jiggleHeights(rs,treeHeight);
    right->jiggleHeights(rs,treeHeight);
  }

  int findNumber(int x, Node*& n) {
    /* Finds the node with number x in the subtree rooted at the node.
       The directions to x are put into dir of the nodes on the path,
       where 1 means go left and 0 means go right. Returns 1 if taxon x
       is found, 0 otherwise. Sets n to the node if found. */

    if(number == x) {
      n = this;
      return 1;
    }
    else if(left->findNumber(x,n)) {
      dir = 1;
      return 1;
    }
    else if(right->findNumber(x,n)) {
      dir = 0;
      return 1;
    }
    else
      return 0;
  }

  void propagateMasks() {
    // Propagate the node masks up the tree.
    left->propagateMasks();
    right->propagateMasks();
    siteOr(*intMask,left->mask,right->mask);
  }

  Node* copyTopology(Vector<Node*>& nodes) {
    nodes[number]->copyOneTopology(*this);
    nodes[number]->left = left->copyTopology(nodes);
    nodes[number]->right = right->copyTopology(nodes);
    nodes[number]->left->parent = nodes[number]->right->parent = nodes[number];
    return nodes[number];
  }

  void copyFrags(Vector<Frag*>& saved) const {
    // Copies the frag values of the nodes of the tree rooted at here to f.
    vcopy(*(saved[number]),*frag,numFrags());
    left->copyFrags(saved);
    right->copyFrags(saved);
  }

  void restoreFrags(Vector<Frag*>& saved) {
    // Restores the frag values of the nodes of the tree rooted here from f.
    Frag* t = frag; frag = saved[number]; saved[number] = t;
    left->restoreFrags(saved);
    right->restoreFrags(saved);
  }

  double localUpdateClock(const RunSettings&,const SiteData&,
			  const Parameters&,const Internal*,Internal*&,
			  Internal*&,double&,Vector<Node*>&);

  double localUpdateNonClock(const RunSettings&,const SiteData&,
			     const Parameters&,const Internal*,Internal*&,
			     Internal*&,int&, double&,Vector<Node*>&);

  void replaceSubtreeClock(Vector<Node*>&);

  void replaceSubtreeNonClock(int,Vector<Node*>&);

  void resetParentsClock(Node*);

  void resetParentsNonClock(Node*,int);

  double findPlRoot(const SiteData&,const Parameters&) const;

  Internal* chooseInternalNode(int);	// Choose an internal node of tree.

  Internal* chooseInternalEdge(int);	// Choose an endpt of internal edge.

  Node* splitBranch(int);		// Reorders the tree rooted at node so
					// the node containing taxon x is the
  					// left child of the node.

  void checkParents(int,Vector<Node*>&, // Check that parents match the input
		    Vector<int>&);      // after reading a bambe tree.

  void rerootTree(const SiteData&,      // Chooses a new root.
		  const Parameters&,int);

  double findLogLikelihood(		// Sets masks, restricts sites, and
    const SiteData&,const Parameters&); // does the likelihood calc for tree.

  double findPartialLikelihood(const SiteData& sd, const Parameters& params) {
    left->findAllPl(sd,params);
    right->findAllPl(sd,params);
    return findPlRoot(sd,params);
  }

  int isLeaf() { return 0; }

 private:
  void listNodes(Dlist& list, int& count) {
    left->listNodes(list,count);
    listOneNode(list,count);
    count++;
    right->listNodes(list,count);
  }

  void restrictSites(const SiteData&,const Vector<int>&,const Vector<int>&);

  void restrictAllSites(const SiteData&);

  void findPl(const SiteData&,const Parameters&);

  void findAllPl(const SiteData&,const Parameters&);

  Internal* saveSubtree(Vector<Node*>& nodes);

  void findAncestorsPl(const SiteData&,const Parameters&,Internal*&,
		       const Internal*&,Internal*&,double&,Vector<Node*>&);

  void changeDistances(const RunSettings&,double&,double&,double&,double&,
		       int&);

  int numFrags() const {return parent ? numFrags0 + numCanonSites : 0;}
};

class DlistItem {
 public:
  Node* node;
  int prev,next,done;
};

class Dlist : public Vector<DlistItem> {
  /* A doubly-linked list of nodes. The leaves are initially at even
     locations of the list. */
 public:
  Dlist(int s, Node* n) : Vector<DlistItem>(s) { 
    /* Does an inorder traversal of the tree rooted at n and places
       the nodes into the dlist. n should have s nodes. */
    int count = 0;
    n->listNodes(*this,count);
    v[s-1].next = -1;
  }
  Dlist(int s, Vector<Node*>& nodes) : Vector<DlistItem>(s) { 
    /* nodes is a vector of s nodes, with the leaves stored at the
       beginning. Places the nodes into the dlist with the leaves
       stored at every other element starting at 0. */
  for(int i1=0,j1=0;i1<(s+1)/2;i1++,j1+=2) {
    v[j1].node = nodes[i1];
    v[j1].prev = j1-1;
    v[j1].next = j1+1;
    v[j1].done = 0;
  }
  for(int i2=(s+1)/2,j2=1;i2<s;i2++,j2+=2) {
    v[j2].node = nodes[i2];
    v[j2].prev = j2-1;
    v[j2].next = j2+1;
    v[j2].done = 0;
  }
  v[s-1].next = -1;
  }
  int higher(int i,int j) const {
    // True if first node is lower than the second in an excursion. 
    return !lessThan(v[i].node,v[j].node);
  }
};

#endif

