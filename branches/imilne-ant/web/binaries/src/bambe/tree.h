#ifndef TREEHDR
#define TREEHDR

#include "util.h"
#include "runsettings.h"
#include "site.h"
#include "sitedata.h"
#include "parameters.h"
#include "node.h"

class SavedState {
public:
  SavedState(const SiteData& sd) : nodes(2*sd.getNumTaxa()-1)
    {
      for(int i1=0;i1<sd.getNumTaxa();i1++)
	nodes[i1] = new Leaf(sd,i1);
      for(int i2=sd.getNumTaxa();i2<2*sd.getNumTaxa()-1;i2++)
	nodes[i2] = new Internal(sd,i2);
    }
  ~SavedState() { for(int i=0;i<nodes.getSize();i++) delete nodes[i]; }
  Vector<Node*> nodes; 		// nodes[numNodes]: array for copies of nodes.
  Internal *node, *copy;
  Internal *root;
  int topology;
  double loglikelihood;
};

class Tree {
 public:
  Tree(const RunSettings&, const SiteData&, const Parameters&);
  Tree(const SiteData&, char*);

  ~Tree() { 
    root->setNodes(nodes);
    for(int i=0;i<nodes.getSize();i++) delete nodes[i]; 
  }
  void print(ostream&) const;
  void printNewickTree(ostream&) const;
  void printNumberTree(ostream&) const;
  void printTopology(ostream&) const;
  double getRadius() const { double h,d; root->diameter(h,d); return d/2; }
  double getLoglikelihood() const { return loglikelihood; }
  void checkParamUpdate(const RunSettings&,const SiteData&,Parameters&,
			int,AcceptCounts&);
  void globalUpdate(const RunSettings&,const SiteData&,const Parameters&);
  void localUpdate(const RunSettings&,const SiteData&,const Parameters&);
  void saveGlobal() {
    savedState.root = (Internal*)(root->copyNodes(savedState.nodes));
    savedState.loglikelihood = loglikelihood;
  }
  void restoreGlobal() {
    root->setNodes(savedState.nodes);
    root = savedState.root;
    loglikelihood = savedState.loglikelihood;
  }
  void restoreLocal();
  double getSavedLoglikelihood() const { return savedState.loglikelihood; }
  double getHastingsRatio() const { return hastingsRatio; }
  void checkClock();
  void reroot(const SiteData&,const Parameters&);

 protected:
  int numLeaves;	// the number of taxa.
  int numNodes;		// the number of nodes of the tree.
  Vector<Node*> nodes;  // nodes[numNodes]: array of nodes of the tree.
  Internal *root;	// the root of the tree.
  double loglikelihood; // the loglikehood of the tree.
  int newickFormat;	// print out in newick format.
  int mclock;		// whether or not the molecular clock is used.
  int outGroup;		// the out group for the non-clock case.
  const Vector<NumberedName>& orderedName;
  SavedState savedState;
  double hastingsRatio;

  void jiggleHeights(const RunSettings&, Dlist&);
  int findTaxaName(NumberedName*);
  void readBambeTree(const char*);
  void printBambeTree(ostream&) const;
  void readNewickTree(const char*);
  void createRandomTree(const RunSettings&);
  Node* readNewickInt(istream&,int&,Vector<Node*>);
  void makeDistMatrix(const SiteData&,const Parameters&,Matrix<double>&);
  void upgma(Matrix<double>& dist);
  void neighborJoin(Matrix<double>&);
};

class PrintNodes : public Vector<Node*> {
public:
  PrintNodes(int numLeaves, int numNodes, const Vector<Node*>old)
    : Vector<Node*>(numNodes) {
    for(int i1=0;i1<numLeaves;i1++)
      v[old[i1]->getNumber()] = new Leaf(old[i1]);
    for(int i2=numLeaves;i2<numNodes;i2++)
      v[old[i2]->getNumber()] = new Internal(old[i2]);
  }
  ~PrintNodes() { for(int i=0;i<size;i++) delete v[i]; }
};
  
#endif

