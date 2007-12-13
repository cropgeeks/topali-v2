#include "util.h"
#include "node.h"
#include "tree.h"

#include <math.h>
#include <iostream>
#include <ctype.h>
#include <stdlib.h>
#include <fstream>

void multiplyPtables(int numTags, Vector<BaseArray>& ptab,
		     const Vector<BaseArray>& atab,
		     const Vector<BaseArray>& btab);
		     
int countPairs(const SiteData& sd, Vector<BaseArray>& xtab,
		int taxon1, int taxon2);		     

double newtonUpdate(int numTags, double old, const Vector<BaseArray>& xtab,
		    const Vector<BaseArray>& ptab,
		    const Vector<BaseArray>& atab,
		    const Vector<BaseArray>& btab);

double findDeriv(const SiteData& sd, const Parameters& params,
		 const Vector<BaseArray>& xtab, const Vector<BaseArray>& qtab, 
		 double branchLength);

double findZero(const SiteData& sd, const Parameters& params,
		const Vector<BaseArray>& xtab, const Vector<BaseArray>& qtab,
		int taxon1, int taxon2, double tolerance);

double getDistTwoTaxa(const SiteData& sd, const Parameters& params, 
		      int taxon1, int taxon2);
		      				 		    		     
void printDistanceMatrix(const SiteData& sd, ostream& f, Matrix<double>& dist,
			 int n);

void readDistanceMatrix(Matrix<double>& dist, int& n);

inline void swapNode(Node*& a, Node*& b) { Node* t=a; a=b; b=t; }

void Tree::readBambeTree(const char* filename)
{
  /* Reads a bambe format tree. */

  ifstream f(filename);
  if(!f) {
    error << "Error: Cannot open bambe tree file " << filename << "."
	  << endError;
    quit(1);
  }

  int c;
  Vector<int> parent(numNodes);

  int rootNum;
  if(!(f >> rootNum)) {
    error << "Error: Tree file not in BAMBE format." << endError;
    quit(1);
  }
  root = (Internal*)nodes[rootNum];
  root->setParent(NULL);
  SKIPLINE(f,c);
  SKIPLINE(f,c);
  for(int i=0;i<numNodes;i++)
    parent[i] = nodes[i]->readBambe(f,nodes);
  f.close();
  root->checkParents(numNodes,nodes,parent);
}

void Tree::printBambeTree(ostream& f) const
{
  /* Print out a tree in bambe format. */

  static PrintNodes pnodes(numLeaves,numNodes,nodes);
  Internal* n = (Internal*)(root->copyTopology(pnodes));

  f.setf(f.flags() | ios::fixed);
  f << " " << root->getNumber() << " " << numLeaves << endl;
  f << "taxon node lchild rchild parent branchlen    height" << endl;
  double hgt;

  n->setHeights(0,hgt);
  n->setNodes(pnodes);
  for(int i=0;i<numNodes;i++) 
    pnodes[i]->printBambe(f);
  f << "log likelihood = " << loglikelihood << endl << endl;
}

int Tree::findTaxaName(NumberedName *key)
{
  NumberedName *p;

  if((p=orderedName.search(key,numLeaves,cmpName))!=0)
    return p->n;
  error << "Error: Taxon name /" << key->name << "/ not found." << endError;
  quit(1);
  return 0; // Needed for Visual C++
}

Node* Tree::readNewickInt(istream& f, int& n, Vector<Node*> nds)
{
  /* n is the number of the next internal node to be created. 
     Returns the root of the subtree read in. */

  int c = readNodeChar(f);
  if(c == '(') {
    Node* lc = readNewickInt(f,n,nds);
    readSpecialChar(f,',',"comma");
    Node* rc = readNewickInt(f,n,nds);
    readSpecialChar(f,')',"right parenthesis");
    readSpecialChar(f,':', "colon");
    if(n>=numNodes) {
      error << "Error: Too many nodes in input tree." << endError;
      quit(1);
    }
    nds[n]->setBranchLength(readBranchLength(f));
    nds[n]->setChildren(lc,rc);
    lc->setParent(nds[n]);
    rc->setParent(nds[n]);
    return nds[n++];
  }
  else {
    f.putback(c);
    NumberedName nbuf;
    if(!f.get(nbuf.name,MAX_LINE,':')) {
      error << "Error: Cannot read taxon name in newick file." << endError;
      quit(1);
    }
    while((c=f.get())!=0 && (c!=':'))
      ;
    int n1 = findTaxaName(&nbuf);
    nds[n1]->setChildren(NULL,NULL);
    nds[n1]->setBranchLength(readBranchLength(f));
    return nds[n1];
  }
}

void Tree::readNewickTree(const char* filename)
{
  /* Read in a newick format tree. */

  ifstream f(filename);
  if(!f) {
    error << "Error: Cannot open newick tree file " << filename << "."
	  << endError;
    quit(1);
  }

  int n=0;
  Node *lc, *rc;

  int c = readNodeChar(f);
  if(c == '(') {
    n = numLeaves;	/* number of first internal node */
    lc = readNewickInt(f,n,nodes);
    readSpecialChar(f,',',"comma");
    rc = readNewickInt(f,n,nodes);
    readSpecialChar(f,')',"right parenthesis");
  }
  else {
    f.putback(c);
    NumberedName nbuf;
    f.get(nbuf.name,MAX_LINE,'\n');
    SKIPLINE(f,c);
    n = findTaxaName(&nbuf);
    if(n!=0) {
      error << "Internal error: The only taxon is not taxon 0." << endError;
      quit(1);
    }
    lc = rc = NULL;
  }
  readSpecialChar(f,';',"semicolon");
  f.close();
  if(n!=numNodes-1) {
    error << "Error: Wrong number of nodes in input tree." << endError;
    quit(1);
  }
  root = (Internal*)(nodes[n]);
  root->setChildren(lc,rc);
  root->setBranchLength(0);
  root->setParent(NULL);
  if(lc != NULL)
    lc->setParent(root);
  if(rc != NULL)
    rc->setParent(root);
}

/* Trees can be constructed from an inorder listing of the nodes, plus
   the depth of each node (distance from node to root). Dlist is a 
   doubly-linked list containing the inorder traversal of the nodes.
   done is a flag indicating the a node has been processed and has
   be added to the tree. */

void Tree::createRandomTree(const RunSettings& rs)
{
  for(int i1=0;i1<numNodes;i1++)	// Randomly set the heights of nodes.
    nodes[i1]->setRandomHeight(rs);
  for(int i2=0;i2<numLeaves-1;i2++)	// Randomly permute the leaves.
    swapNode(nodes[i2],nodes[(int)(Rand::runif()*(numLeaves-i2))+i2]);

  Dlist list(numNodes,nodes);	        // Construct a doubly-linked list of
					// nodes with the leaves at even
  					// locations.
  root = makeTree(list,numNodes);       // Create the tree given the excursion.
}

void multiplyPtables(int numTags, Vector<BaseArray>& ptab,
		     const Vector<BaseArray>& atab,
		     const Vector<BaseArray>& btab)
{
  for(int tag=0,m=0;tag<numTags;tag++,m+=NUM_BASES)
    for(int i=0;i<NUM_BASES;i++)
      for(int j=0;j<NUM_BASES;j++) {
        ptab[m+j][i] = 0;
        for(int k=0;k<NUM_BASES;k++)
          ptab[m+j][i] += atab[m+k][i] * btab[m+j][k];
      }
}

int countPairs(const SiteData& sd, Vector<BaseArray>& xtab,
		int taxon1, int taxon2)
{
  /* Sets x[m+b1][b2] to be the number of sites which have base b1 for
     taxon taxon1 and base b2 for taxon taxon2. Returns 1 if any sites
     have different bases for the two taxa, 0 otherwise. */

  for(int i=0;i<sd.getNumTags()*NUM_BASES;i++)
    for(int j=0;j<NUM_BASES;j++)
      xtab[i][j] = 0;

  int different = 0;
  for(int tag=0,m=0;tag<sd.getNumTags();tag++,m+=NUM_BASES) {
    // constant sites
    for(int b=0;b<NUM_BASES;b++)
      xtab[m+b][b] += sd.getRootConstSite(tag)[b];
    // non-constant sites 
    for(int i=sd.getFirstSite(tag);i<sd.getFirstSite(tag+1);i++) {
      /* ignoring the proper thing with gaps or indeterminate bases */
      int b1,b2;
      if(singleBase(sd.getBase(taxon1)[i],b1) && 
	 singleBase(sd.getBase(taxon2)[i],b2)) {
        xtab[m+b2][b1] += sd.getSiteCount(i);
	if(b1!=b2)
	  different = 1;
      }
    }
  }
  return different;
}

double newtonUpdate(int numTags, double old, const Vector<BaseArray>& xtab,
		    const Vector<BaseArray>& ptab,
		    const Vector<BaseArray>& atab,
		    const Vector<BaseArray>& btab)
{
  // ptab is P(t), atab is P'(t), btab = P''(t) 


  double num=0,denom=0;
  
  for(int tag=0,m=0;tag<numTags;tag++,m+=NUM_BASES)
    for(int i=0;i<NUM_BASES;i++)
      for(int j=0;j<NUM_BASES;j++) {
	double temp = atab[m+j][i] / ptab[m+j][i];
        num += xtab[m+j][i] * temp;
        denom += xtab[m+j][i] * (btab[m+j][i] / ptab[m+j][i] - temp*temp);
      }
  return old - (num / denom);
}

double findDeriv(const SiteData& sd, const Parameters& params,
		 const Vector<BaseArray>& xtab, const Vector<BaseArray>& qtab, 
		 double branchLength)
{
  Vector<BaseArray> ptab(sd.getNumTags()*NUM_BASES);
  params.findPtable(ptab,branchLength);

  Vector<BaseArray> atab(sd.getNumTags()*NUM_BASES);
  multiplyPtables(sd.getNumTags(),atab,qtab,ptab);

  double sum = 0;

  for(int tag=0,m=0;tag<sd.getNumTags();tag++,m+=NUM_BASES)
    for(int i=0;i<NUM_BASES;i++) 
      for(int j=0;j<NUM_BASES;j++)
	sum += xtab[m+i][j] * atab[m+i][j] / ptab[m+i][j];
  return sum;
}

double findZero(const SiteData& sd, const Parameters& params,
		const Vector<BaseArray>& xtab, const Vector<BaseArray>& qtab,
		int taxon1, int taxon2, double tolerance)
{
  int i,count;
  double x,y,z,deriv;

  for(i=1,x=.1;(deriv=findDeriv(sd,params,xtab,qtab,x))<=0;i++,x/=10.)
    if(i>=10) {
      error << "Internal Error: Can't find positive derivative for " << "("
	    << taxon1 << "," << taxon2 << ")" << endError;
      quit(1);
    }
  for(i=1,y=1;(deriv=findDeriv(sd,params,xtab,qtab,y))>=0;i++,y*=10.)
    if(i>=10) {
      error << "Error: Distance between " << sd.getTaxaName(taxon1)
	    << " and " << sd.getTaxaName(taxon2) << " is infinite."
	    << endError;
      quit(1);
    }
  for(count=0,z=(x+y)/2;
      fabs(deriv=findDeriv(sd,params,xtab,qtab,z))>tolerance;
      count++,z=(x+y)/2)
    if(deriv>0)
      x = z;
    else
      y = z;
  return z;
}

double getDistTwoTaxa(const SiteData& sd, const Parameters& params, 
		      int taxon1, int taxon2)
{
  const double binsearchTol = 1.0e-5;
  const double tol = 1.0e-8;
  const int maxIterations = 200;
  int numTags = sd.getNumTags();

  Vector<BaseArray> xtab(numTags*NUM_BASES);
  Vector<BaseArray> qtab(numTags*NUM_BASES);
  Vector<BaseArray> ptab(numTags*NUM_BASES);
  Vector<BaseArray> atab(numTags*NUM_BASES);
  Vector<BaseArray> btab(numTags*NUM_BASES);

  int different = countPairs(sd,xtab,taxon1,taxon2);

  if(!different)
    return 0;

  // print_ptable(xtab);
  params.findQ(qtab);
  //try to find a good initial distance

  double firstBlen = findZero(sd,params,xtab,qtab,taxon1,taxon2,binsearchTol);  
  params.findPtable(ptab,firstBlen);
  multiplyPtables(numTags,atab,qtab,ptab);
  multiplyPtables(numTags,btab,qtab,atab);

  double curBlen = firstBlen;
  double nextBlen = newtonUpdate(numTags,curBlen,xtab,ptab,atab,btab);
  
  int count = 1;
  while(fabs(curBlen - nextBlen) > tol) {
    if(count==maxIterations) {
       cout << "Internal warning: Reverting to binary search." << endl;
      return findZero(sd,params,xtab,qtab,taxon1,taxon2,tol);  
    }
    curBlen = nextBlen;
    params.findPtable(ptab,curBlen);
    multiplyPtables(numTags,atab,qtab,ptab);
    multiplyPtables(numTags,btab,qtab,atab);
    nextBlen = newtonUpdate(numTags,curBlen,xtab,ptab,atab,btab);
    count++;
  }
  return(nextBlen);
}

void Tree::makeDistMatrix(const SiteData& sd, const Parameters& params,
			  Matrix<double>& dist)
			  
{  
  for(int i1=0;i1<numLeaves;i1++)
    dist[i1][i1] = 0;
  
  for(int i2=0;i2<numLeaves-1;i2++)
    for(int j=i2+1;j<numLeaves;j++)
      dist[i2][j] = dist[j][i2] = getDistTwoTaxa(sd,params,i2,j);
}

class HeapEl {
public:
  HeapEl() {}
  HeapEl(int i0, int j0, double p0) { i=i0; j=j0; p=p0; }
  int i,j;
  double p;
};

class Heap {
public:
  Heap(int s) : v(s) {}

  const HeapEl& top() const { return v[0]; }

  void setElement(int n, const HeapEl& h) { v[n]=h; }

  void setSize(int m) { size = m; }

  void makeHeap(Matrix<int>& el) {
    for(int k=size/2;k>=0;k--)
      downheap(el,k,v[k]);
  }

  void remove(Matrix<int>& el, int start) {downheap(el,start,v[--size]);}

   void change(Matrix<int>& el, int start, double newval) {
     if(newval < v[start].p)
       upheap(el,start,HeapEl(v[start].i,v[start].j,newval));
     else
       downheap(el,start,HeapEl(v[start].i,v[start].j,newval));
   }

   void print(ostream& c) {
     for(int i=0;i<size;i++)
       c << setiosflags(ios::showpoint | ios::fixed) << setprecision(2)
	 << setw(3) << i << " " << setw(10) << v[i].p << " " << setw(3)
	 << v[i].i << " " << setw(3) << v[i].j << endl;
   }

 private:
   int size;

   Vector<HeapEl> v;

   void upheap(Matrix<int>& el, int start, const HeapEl h) {
     int n,parent;
     for(n=start;n>0;n=parent) {
       parent = (n-1)/2;
       if(v[parent].p <= h.p)
	 break;
       v[n] = v[parent];
       el[v[n].i][v[n].j] = n;
     }
     v[n] = h;
     el[h.i][h.j] = n;    
   }

   void downheap(Matrix<int>& el, int start, const HeapEl h) {
     int n,child;
     for(n=start;2*n+1<size;n=child) {
       child = (2*n+2<size && v[2*n+2].p<v[2*n+1].p) ? 2*n+2: 2*n+1;
       if(v[child].p >= h.p)
	 break;
       v[n] = v[child];
       el[v[n].i][v[n].j] = n;
     }
     v[n] = h;
     el[h.i][h.j] = n;    
   }
 };

void Tree::upgma(Matrix<double>& dist)
 {
   Heap heap(numLeaves*(numLeaves-1)/2);
   Vector<double> distToLeaves(numNodes);
   Vector<int> p(numLeaves);
   Vector<int> dnodes(numLeaves);
   Vector<int> size(numLeaves);
   Vector<int> row(numLeaves);
   Matrix<int> el(numLeaves,numLeaves);

   int m = 0;
   for(int i1=0;i1<numLeaves;i1++) {
     p[i1] = dnodes[i1] = row[i1] = i1;
     nodes[i1]->setChildren(NULL,NULL);
     size[i1] = 1;
     for(int j1=0;j1<i1;j1++) {
       heap.setElement(m,HeapEl(i1,j1,dist[i1][j1]));
       el[i1][j1] = m++;
     }
   }
   heap.setSize(m);

   for(int i2=0;i2<numNodes;i2++)
     distToLeaves[i2] = 0.;

   heap.makeHeap(el);

   int n = numLeaves;
   int nn = numLeaves;

   while(n>1) {
    HeapEl h = heap.top();
    double min = h.p;
    int i = h.i;
    int j = h.j;
    int mini= row[i];
    int minj = row[j];
    /* Note: 0 will be removed from the heap along with all of
       i's elements below. */
    nodes[p[i]]->setParent(nodes[nn]);
    nodes[p[j]]->setParent(nodes[nn]);
    /*    (*t)->branchlen[p[i]] = (*t)->branchlen[p[j]] = min/2; */
    distToLeaves[nn] = min/2;
    nodes[p[i]]->setBranchLength(min/2 - distToLeaves[p[i]]);
    nodes[p[j]]->setBranchLength(min/2 - distToLeaves[p[j]]);
    nodes[nn]->setChildren(nodes[p[i]],nodes[p[j]]);
    p[j] = nn++;
    for(int r1=0;r1<mini;r1++)
      heap.remove(el,el[i][dnodes[r1]]);
    for(int r2=mini+1;r2<n;r2++)
      heap.remove(el,el[dnodes[r2]][i]);
    for(int r3=0;r3<minj;r3++) {
      int k = dnodes[r3];
      dist[j][k] = (dist[j][k]*size[j]*size[k] + dist[i][k]*size[i]*size[k])
	/ ((size[j]+size[i])*size[k]);
      heap.change(el,el[j][k],dist[j][k]);
    }
    for(int r4=minj+1;r4<mini;r4++) {
      int k = dnodes[r4];
      dist[k][j] = (dist[k][j]*size[j]*size[k] + dist[i][k]*size[i]*size[k])
	/ ((size[j]+size[i])*size[k]);
      heap.change(el,el[k][j],dist[k][j]);
    }
    for(int r5=mini+1;r5<n;r5++) {
      int k = dnodes[r5];
      dist[k][j] = (dist[k][j]*size[j]*size[k] + dist[k][i]*size[i]*size[k])
	/ ((size[j]+size[i])*size[k]);
      heap.change(el,el[k][j],dist[k][j]);
    }
    size[j] += size[i];
    for(int r6=mini;r6<n-1;r6++) {
      dnodes[r6] = dnodes[r6+1];
      row[dnodes[r6]] = r6;
    }
    n--;
  }
  root = (Internal*)nodes[p[dnodes[0]]];
  root->setParent(NULL);
}

void Tree::neighborJoin(Matrix<double>& dist)
{
  Vector<double> rate(numNodes);
  Vector<int> p(numNodes), dnodes(numNodes);

  for(int i1=0;i1<numLeaves;i1++) {
    p[i1] = dnodes[i1] = i1;
    nodes[i1]->setChildren(NULL,NULL);
  }

  int n = numLeaves;
  int nn = numLeaves;

  while(n>2) {
  /* find sum of distances to all leaves, assuming
     that dist is complete (both upper and lower triangles, 0 on diagonal) */
    for(int i2=0;i2<n;i2++) {
      rate[dnodes[i2]] = 0;
      for(int j2=0;j2<n;j2++)
        rate[dnodes[i2]] += dist[dnodes[i2]][dnodes[j2]];
    }
    
    double min = dist[dnodes[1]][dnodes[0]]
      - (rate[dnodes[1]] + rate[dnodes[0]])/(double)(n-2);
    int mini = 1;
    int minj = 0;
    for(int i3=1;i3<n;i3++) {
      for(int j3=0;j3<i3;j3++) {
        double newval = dist[dnodes[i3]][dnodes[j3]]
          - (rate[dnodes[i3]] + rate[dnodes[j3]])/(double)(n-2);
	if(newval < min) {
	  min = newval;
	  mini = i3;
	  minj = j3;
	}
      }
    }

    int i4 = dnodes[mini];
    int j4 = dnodes[minj];
    nodes[p[i4]]->setParent(nodes[nn]);
    nodes[p[j4]]->setParent(nodes[nn]);

    /* don't assign negative branch lengths */
    double blen = (dist[i4][j4] + (rate[i4] - rate[j4])/(double)(n-2))/2;
    if(blen > 0) {
      nodes[p[i4]]->setBranchLength(blen);
      nodes[p[j4]]->setBranchLength(dist[i4][j4]-blen);
    }
    else {
      nodes[p[i4]]->setBranchLength(0);
      nodes[p[j4]]->setBranchLength(dist[i4][j4]);
    }
    nodes[nn]->setChildren(nodes[p[i4]],nodes[p[j4]]);
    p[j4] = nn++;
    for(int r1=0;r1<n;r1++)
      if(r1!=mini && r1!=minj) {
        int k4 = dnodes[r1];
        dist[j4][k4] = dist[k4][j4] 
	  = (dist[i4][k4] + dist[j4][k4] - dist[i4][j4]) / 2;
      }
    for(int r2=mini;r2<n-1;r2++)
      dnodes[r2] = dnodes[r2+1];
    n--;
  }
  int i5 = dnodes[1];
  int j5 = dnodes[0];
  nodes[p[i5]]->setParent(nodes[nn]);
  nodes[p[j5]]->setParent(nodes[nn]);
  nodes[p[i5]]->setBranchLength(dist[i5][j5]/2);
  nodes[p[j5]]->setBranchLength(dist[i5][j5]/2);
  nodes[nn]->setChildren(nodes[p[i5]],nodes[p[j5]]);
  root = (Internal*)nodes[nn];
  root->setParent(NULL);
}

void printDistanceMatrix(const SiteData& sd, ostream& f, Matrix<double>& dist,
			 int n)
{
  char buf[MAX_LINE];

  f << endl;
  for(int i=0;i<n;i++)
  {
    strncpy(buf,sd.getTaxaName(i),10);
    buf[10] = '\0';
    f << buf << ' ';
    f << setiosflags(ios::showpoint | ios::fixed) << dist[i][0];
    for(int j=1;j<n;j++)
      f << " " << setiosflags(ios::showpoint | ios::fixed) << dist[i][j];
    f << endl;
  }
  f << endl;
}

void readDistanceMatrix(Matrix<double>& dist, int& n)
{
  ifstream f("infile");
  f >> n;
  int c;
  SKIPLINE(f,c);
  for(int i=0;i<n;i++) {
    for(int k=0;k<10;k++)
      f.get();
    for(int j=0;j<n;j++)
      f >> dist[i][j];
    SKIPLINE(f,c);
  }
}

Tree::Tree(const RunSettings& rs, const SiteData& sd, 
	   const Parameters& params) : 
  orderedName(sd.getOrderedName()),
  numLeaves(sd.getNumTaxa()),
  numNodes(2*numLeaves-1),
  nodes(numNodes),
  mclock(rs.getMclock()),
  newickFormat(rs.getNewickFormat()),
  savedState(sd)
{

  // Create all the nodes at once and change their values later.
  for(int i1=0;i1<numLeaves;i1++)
    nodes[i1] = new Leaf(sd,i1);
  for(int i2=numLeaves;i2<numNodes;i2++)
    nodes[i2] = new Internal(sd,i2);
  switch(rs.getInitialTreeType()) {
  case RSTreeType::RANDOM: createRandomTree(rs); break;
  case RSTreeType::NEWICK: readNewickTree(rs.getTreeFile()); break;
  case RSTreeType::BAMBE:  readBambeTree(rs.getTreeFile()); break;
  case RSTreeType::UPGMA:  { 
    Matrix<double> dist(numNodes,numNodes);
    makeDistMatrix(sd,params,dist);
    upgma(dist);
    break;
  }
  case RSTreeType::NEIGH:  { 
    Matrix<double> dist(numNodes,numNodes);
    makeDistMatrix(sd,params,dist);
    neighborJoin(dist);
    break;
  }
  }
  if(mclock) {
    double hgt;
    root->setHeights(0,hgt);
    root->checkHeights(hgt);
  }
  else 
    outGroup = rs.getOutGroup(); 
  loglikelihood = root->findLogLikelihood(sd,params);
}

Tree::Tree(const SiteData& sd, char* filename) :
  orderedName(sd.getOrderedName()),
  numLeaves(sd.getNumTaxa()),
  numNodes(2*numLeaves-1),
  nodes(numNodes),
  mclock(1),
  newickFormat(1),
  savedState(sd)
{
  for(int i1=0;i1<numLeaves;i1++) 
    nodes[i1] = new Leaf(sd,i1);
  for(int i2=numLeaves;i2<numNodes;i2++)
    nodes[i2] = new Internal(sd,i2);
  readNewickTree(filename);
  loglikelihood = 0.0;
}


void Tree::checkClock()
{
  if(mclock) {
    double hgt;
    root->setHeights(0,hgt);
    root->checkHeights(hgt);
  }
}

void Tree::print(ostream& f) const
{
  if(newickFormat)
    printNewickTree(f);
  else // bambe format
    printBambeTree(f);
}

void Tree::printNewickTree(ostream& f) const
{
  static PrintNodes pnodes(numLeaves,numNodes,nodes);
  Internal* n = (Internal*)(root->copyTopology(pnodes));
  if(!mclock) 
    n->splitBranch(outGroup-1);
  n->setMinTaxa(); 
  ((Node*)n)->print(f); 
  f << ';' << endl;
}

void Tree::printNumberTree(ostream& f) const
{
  static PrintNodes pnodes(numLeaves,numNodes,nodes);
  Internal* n = (Internal*)(root->copyTopology(pnodes));
  if(!mclock) 
    n->splitBranch(outGroup-1);
  n->setMinTaxa(); 
  n->print(f,0); 
  f << ';' << endl;
}

void Tree::printTopology(ostream& f) const 
{ 
  if(mclock) {
    root->setMinTaxa(); 
    root->printTopology(f); 
  }
  else {
    static PrintNodes pnodes(numLeaves,numNodes,nodes);
    Internal* n = (Internal*)(root->copyTopology(pnodes));
    n->splitBranch(outGroup-1);
    n->setMinTaxa();
    n->printTopology(f);
  }
}

void Tree::reroot(const SiteData& sd, const Parameters& params)
{
  root->rerootTree(sd,params,numNodes);
}  

void Tree::globalUpdate(const RunSettings& rs, const SiteData& sd, 
			const Parameters& params) 
{ 
  /* Create an excursion based on the heights of the nodes. Perturb
     the excursion and re-create the tree based on the new heights. */

  root->swapChildren();	     // Randomly swap children of nodes.
  Dlist list(numNodes,root);	     // Create an inorder traversal of tree.
  jiggleHeights(rs,list);	     // Jiggle the heights of the nodes.
  root = makeTree(list,numNodes);  // Reorder the tree to match new heights.
  loglikelihood = root->findLogLikelihood(sd,params);

}

void Tree::localUpdate(const RunSettings& rs, const SiteData& sd, 
		       const Parameters& params) 
{
  savedState.loglikelihood = loglikelihood;
  if(mclock) {
    savedState.node = root->chooseInternalNode(numLeaves);
    loglikelihood 
      = savedState.node->localUpdateClock(rs,sd,params,root,savedState.copy,
					  savedState.root,hastingsRatio,
					  savedState.nodes);
  }
  else {
    savedState.node = root->chooseInternalEdge(numLeaves);
    loglikelihood 
      = savedState.node->localUpdateNonClock(rs,sd,params,root,savedState.copy,
					     savedState.root,
					     savedState.topology,
					     hastingsRatio,
					     savedState.nodes);
  }
}

void Tree::restoreLocal()
{
  if(mclock) {
    savedState.node->replaceSubtreeClock(savedState.nodes);
    root = savedState.root;
    savedState.copy->resetParentsClock(root);
  }
  else {
    savedState.node->replaceSubtreeNonClock(savedState.topology,
					    savedState.nodes);
    root = savedState.root;
    savedState.copy->resetParentsNonClock(root,savedState.topology);
  }
  loglikelihood = savedState.loglikelihood;
}

void Tree::jiggleHeights(const RunSettings& rs, Dlist& list)
{
  if(mclock) {
    double treeHeight;
    root->setHeights(0,treeHeight);
    root->jiggleHeights(rs,treeHeight);
  }
  else {
    /* Jiggles the heights of the nodes of tree whose inorder traversal
       is stored in list for the non-clock case. Changes the height
       field of the nodes.  Assumes the list is actually stored in
       inorder numbering. */

    double hgt;
    root->setHeights(0,hgt);
    double ht = list[0].node->getHeight();  // New height of the leaf before i.
    for(int i=1;i<numNodes;i+=2) {	    // Interior node in the inorder 
      					    // traversal to be processed
      double d1 = fabs(list[i-1].node->getHeight()-list[i].node->getHeight()
		       +(2*Rand::runif()-1)*rs.getValleyWin());
      double d2 = fabs(list[i+1].node->getHeight()-list[i].node->getHeight()
		       +(2*Rand::runif()-1)*rs.getValleyWin());
      list[i-1].node->setHeight(ht);
      list[i].node->setHeight(ht - d1);
      ht += d2 - d1;
    } 
    list[numNodes-1].node->setHeight(ht);
  }
}

class SavedFrags {
 public:
  SavedFrags(const SiteData& sd, int numLeaves, int numNodes) :
    frag(numNodes) {
    for(int i1=0;i1<numLeaves;i1++)
      frag[i1] = new Frag(1+sd.getNumTags()*NUM_BASES+sd.getNumUncertain(i1));
    for(int i2=numLeaves;i2<numNodes;i2++)
      frag[i2] = new Frag(sd.getNumTags()*NUM_BASES+sd.getNumUniqueSites());
  }

  ~SavedFrags() {
    for(int i=0;i<frag.getSize();i++)
      delete frag[i];
  }

  void copyFrags(const Internal* root) { root->copyFrags(frag); }

  void restoreFrags(Internal* root) { root->restoreFrags(frag); }

private:
  Vector<Frag*> frag;
};


void Tree::checkParamUpdate(const RunSettings& rs, const SiteData& sd,
			    Parameters& params, int cycle, AcceptCounts& ac)
{
  static SavedFrags saved(sd,numLeaves,numNodes);

  if (rs.getParamUpdateInterval() 
      && ((cycle-1) % rs.getParamUpdateInterval() == 0)) {
    Parameters proposed(rs,sd,params);
    saved.copyFrags(root);
    double oldLoglikelihood = loglikelihood;
    loglikelihood = root->findPartialLikelihood(sd,proposed);
    double hr=1;
    if(rs.getUpdateTheta())
      hr *= params.hastingsTheta(rs.getThetaConst(),sd.getTagWeights(),
				 proposed,sd.getNumTags());
    if(rs.getUpdatePi())
      hr *= params.hastingsPi(rs.getPiConst(),proposed,
			      sd.getNumTags(),NUM_BASES);
    if(rs.getModel() == RSModel::GREV && rs.getUpdateGrev())
      hr *= params.hastingsR(rs.getGrevTune(),proposed,
			      sd.getNumTags(),NUM_RVALUES);
    if(acceptable(loglikelihood,oldLoglikelihood,hr)) {
      params = proposed;
      ac.kaccept++;
      ac.kaccepts++;
    }
    else {
      saved.restoreFrags(root);
      loglikelihood = oldLoglikelihood;
    }

    // Test the invariant probability updates by themselves
    if (rs.getUpdateInvariantProb()) {
      hr = 1;
      oldLoglikelihood = loglikelihood;
      proposed = params;
      proposed.updateInvariantProb(rs, params);
      loglikelihood = root->findPlRoot(sd,proposed);
      hr *=params.hastingsP(rs.getInvariantProbTune(),proposed,
			      sd.getNumTags());
    
      if (acceptable(loglikelihood,oldLoglikelihood,hr)) {
	params = proposed;
	ac.paccept++;
	ac.paccepts++;
      }     
      else {
	loglikelihood = oldLoglikelihood;
      }
    }
  }
}

