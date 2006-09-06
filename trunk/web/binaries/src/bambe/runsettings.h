#ifndef RUNSETTINGSHDR
#define RUNSETTINGSHDR

using namespace std;

#include "util.h"
#include <iostream>
#include <string>
#include <fstream>
#include <ctype.h>

#ifdef WINDOWS
#include <strstrea.h>
#define ISTRSTREAM istrstream
#elif (__GNUC__>=3)
#include <sstream>
#define ISTRSTREAM istringstream
#else
#include <strstream.h>
#define ISTRSTREAM istrstream
#endif

class RunSettings;

class RunSetting {
 public:
  RunSetting(char *n, char *v, char *b="", char *r="") :
    name(n), line(0), bounds(b), restriction(r) {
    inputSettings[count++]=this;
    strcpy(val,v); 
  }
  static int getCount();
  static RunSetting *getRunSetting(int i);
  static RunSetting *findInputSetting(const char *);
  void setValue(char*,int);
  virtual void parse(const RunSettings&)=0;
  virtual void print(ostream&)=0;
  const char *getName() const { return name; }
  const char *getValue() const { return val; }

 protected:
  static int count;     // The number of run settings.
  static RunSetting *(inputSettings[250]); // Array of run settings.

  char *name;           // The external name.
  char val[MAX_LINE];   // The value.
  int line;             // The number of the line in the settings file which sets it (0=not set).
  char *bounds;         // The range.
  char *restriction;    // The restriction.

  void parseError();
  char *parseString();
  int fndString(char*,int,const char* const[],int&);
  void addCategory(int,int *,int&);
  void printBadParam(char *,int);
  int getCategories(char *&,int *,char *,int,int&);
  void boundsError();
  void boundsParseError();
  int parseSign(char*&);
  int parseInfinity(char*&);
  int parseInt(char*&);
  double parseDouble(char*&);
  int checkRestrictionInt(char*&,int);
  void checkIntBounds(int);
  void checkDoubleBounds(double);
  void checkRestriction();
};

class RSInt : public RunSetting {
 public:
  int v;
  RSInt(char *n, char *v, char *b="", char *r="") : RunSetting(n,v,b,r) {}
  void parse(const RunSettings &rs) {
    ISTRSTREAM iss(val);
    if((iss>>v)==NULL)
      parseError();
    checkIntBounds(v);
    checkRestriction();
  }
  void print(ostream& f) { f << name << "=" << v << endl; }
};

class RSDouble : public RunSetting {
 public:
  double v;
  RSDouble(char *n, char *v, char *b="", char *r="") : RunSetting(n,v,b,r) {}
  void parse(const RunSettings &rs) {
    ISTRSTREAM iss(val);
    if((iss>>v)==NULL)
      parseError();
    checkDoubleBounds(v);
    checkRestriction();
  }
  void print(ostream& f) { f << name << "=" << v << endl; }
};

class RSBoolean : public RunSetting {
 public:
  int v;
  RSBoolean(char *n, char *v, char *r="") : RunSetting(n,v,"",r) {}
  void parse(const RunSettings &);
  void print(ostream& f) { f << name << "=" << (v?"true":"false") << endl; }
};

class RSAlgorithm : public RunSetting {
 static const char* const values[];
 public:
 enum { GLOBAL, LOCAL };
  int v;
  RSAlgorithm(char *n, char *v, char *r="") : RunSetting(n,v,"",r) {}
  void parse(const RunSettings &);
  void print(ostream& f) { f << name << "=" << values[v] << endl; }
};

class RSModel : public RunSetting {
  static const char* const values[];
 public:
  enum { HKY85, F84, TN93, GREV};
  int v;
  RSModel(char *n, char *v, char *r="") : RunSetting(n,v,"",r) {}
  void parse(const RunSettings &);
  void print(ostream& f) { f << name << "=" << values[v] << endl; }
};


class RSTreeType : public RunSetting {
  static const char* const values[];
 public:
  enum { RANDOM, UPGMA, NEIGH, BAMBE, NEWICK};
  int v;
  RSTreeType(char *n, char *v, char *r="") : RunSetting(n,v,"",r) {}
  void parse(const RunSettings &);
  void print(ostream& f) { f << name << "=" << values[v] << endl; }
};

class RSDoubleArray : public RunSetting {
 public:
  double input[MAX_CATEGORIES];
  Vector<double> initial;
  int size;

  RSDoubleArray(char *n, char *v, char *b="", char *r="") : RunSetting(n,v,b,r) {}
  void parse(const RunSettings &);
  int usesSingleKappa() { return 0; }
  void parseInput(int &,int [],double []);
  void parseParamList(const RunSettings &,int ,double *,Vector<double>& ,int *,double *);

  void print(ostream& f) {
    f << name << "=" << input[0];
    if(!usesSingleKappa())
      for(int i=1;i<size;i++)
        f << "," << input[i];
   f << endl;
  }
};

class RSDoubleArrayS : public RSDoubleArray {
 public:
  RSDoubleArrayS(char *n, char *v, char *b="", char *r="") : RSDoubleArray(n,v,b,r) {}
  int usesSingleKappa() { return 1; }
};

class RSSeed : public RunSetting {
 public:
  uint32 v;
  RSSeed(char *n, char *v, char *b="", char *r="") : RunSetting(n,v,b,r) {}
  void parse(const RunSettings &rs) {
    ISTRSTREAM iss(val);
    if((iss>>v)==NULL)
      parseError();
    v = v % (1UL << 31);
    if(v%2 == 0) {
      v++;
      error << "Warning: changing initial seed to be odd number" << endError;
    }
  }
  void print(ostream& f) { f << name << "=" << v << endl; }
};

class RSCategoryList : public RunSetting {
 public:
  int numTags;                    // number of tags (used internally)
  int numCategories;              // number of categories (used externally)
  char v[MAX_LINE];               // list of categories
  int categories[MAX_CATEGORIES]; // mapping from tag number to category number

  RSCategoryList(char *n, char *v, char *r="") : RunSetting(n,v,"",r) {}
  void parse(const RunSettings &);
  void print(ostream& f) { f << name << "=" << v << endl; }
};

class RSString : public RunSetting {
 public:
  char v[MAX_LINE];
  RSString(char *n, char *v, char *r="") : RunSetting(n,v,"",r) {}
  void parse(const RunSettings &rs) { 
    checkRestriction();
    strcpy(v,val); 
  }
  void print(ostream& f) { f << name << "=" << v << endl; }
};

class RunSettings {
 public:

  RunSettings(istream&);

  void print(ostream& c) const {
    for(int i=0;i<RunSetting::getCount();i++)
      RunSetting::getRunSetting(i)->print(c);
  }

  uint32 getSeed() const { return seed.v; }
  ifstream& getFlogp() { return flogp; }
  ifstream& getFkappa() { return fkappa; }
  ifstream& getFtop() { return ftop; }
  ifstream& getFout() { return fout;}
  ifstream& getFtree() { return ftree;}
  int getNumCycles() const { return cycles.v;}
  int getSampleInterval() const { return sampleInterval.v;}
  int getParamUpdateInterval() const { return paramUpdateInterval.v;}
  int getWindowInterval() const { return windowInterval.v;}
  int getBurn() const { return burn.v;}
  int getUpdateKappa() const { return updateKappa.v;}
  int getUpdateTheta() const { return updateTheta.v;}
  int getUpdatePi() const { return updatePi.v;}
  int getUpdateTtp() const { return updateTtp.v;}
  int getUpdateGamma() const { return updateGamma.v;}
  int getUpdateGrev() const { return updateGrev.v;}
  int getTuneInterval() const { return tuneInterval.v;}
  int getUpdateInvariantProb() const { return updateInvariantProb.v;}
  double getInitialKappa(int i) const { return initialKappa.initial[i];}
  double getInitialTheta(int i) const { return initialTheta.initial[i];}
  const Vector<double> getInitialPi(int i) const {
    switch(i) {
    case 0: return initialPia.initial;
    case 1: return initialPig.initial;
    case 2: return initialPic.initial;
    case 3: return initialPit.initial;
    }
  }
  int getEstimatePi() const { return estimatePi.v;}
  double getInitialTtp(int i) const { return initialTtp.initial[i];}
  double getInitialGamma(int i) const { return initialGamma.initial[i];}
  const Vector<double> getInitialR(int i) const {
    switch(i) {
    case RAC : return initialRac.initial;
    case RAG : return initialRag.initial;
    case RAT : return initialRat.initial;
    case RCG : return initialRcg.initial;
    case RCT : return initialRcg.initial;
    case RGT : return initialRgt.initial;
    }
  }
  int getUseInvariantSites() const { return useInvariantSites.v; }
  double getInvariantProb(int i) const { return invariantProbability.initial[i]; }
  double getInvariantPriorMean(int i) const { return invariantProbPriorMean.initial[i]; }
  double getInvariantPriorSD(int i) const { return invariantProbPriorSD.initial[i]; }

  double getValleyWin() const { return globalTune.v;}
  double getKappaWin() const { return kappaTune.v;}
  double getMaxDepth() const { return maxInitialTreeHeight.v;}
  const char* getDataFile() const { return dataFile.v;}
  const char* getTreeFile() const { return treeFile.v;}
  const char* getFileRoot() const { return fileRoot.v;}
  int getNumTags() const { return categoryList.numTags;}
  int getNumCategories() const { return categoryList.numCategories;}
  int getBurnAlgorithm() const { return burnAlgorithm.v;}
  int getMainAlgorithm() const { return mainAlgorithm.v;}
  int getSingleKappa() const { return singleKappa.v;}
  int getMclock() const { return molecularClock.v;}
  int getModel() const { return model.v;}
  int getOutGroup() const { return outgroup.v;}
  const char* getCategoryList() const { return categoryList.v; }
  int getCategories(int i) const { return categoryList.categories[i]; }
  int getInitialTreeType() const { return initialTreeType.v; }
  int getNewickFormat() const { return newickFormat.v; }
  double getThetaConst() const { return thetaTune.v; }
  double getPiConst() const { return piTune.v; }
  double getTtpWin() const { return ttpTune.v; }
  double getGammaWin() const { return gammaTune.v; }
  double getGrevTune() const { return grevTune.v; }
  double getInvariantProbTune() const { return invariantProbTune.v; }
  double getLocalTune() const { return localTune.v; }
  int getUseBeta() const { return useBeta.v; }
  double getBetaTune() const { return betaTune.v; }
  int getPrintAllTrees() const { return printAllTrees.v; }
  void scaleInitialTheta(double denom) {
    for(int i=0;i<categoryList.numTags;i++)
      initialTheta.initial[i] /= denom; }
  void scaleInitialR(int index, double denom) {
    for(int i=0;i<NUM_RVALUES;i++) {
      initialRac.initial[i] /= denom; 
      initialRag.initial[i] /= denom; 
      initialRat.initial[i] /= denom; 
      initialRcg.initial[i] /= denom; 
      initialRcg.initial[i] /= denom; 
      initialRgt.initial[i] /= denom; 
    }
  }
  void halveValleyWin() { globalTune.v /= 2.0; }

  // This kludge is necessary to be consistent with the old code.
  // The order should agree with the arrays in the corresponding classes.

 private:
  enum { RAC=0, RAG, RAT, RCG, RCT, RGT };
  RSSeed         seed;                          // odd unsigned int between 0 and (4294967295 = 2^32 - 1)
  RSInt          burn;                          // the number of cycles during burn in                  
  RSAlgorithm    burnAlgorithm;                 // deformation algorithm used during burn in            
  RSAlgorithm    mainAlgorithm;                 // deformation algorithm used after burn in             
  RSInt          cycles;                        // number of cycles after burn in                       
  RSInt          sampleInterval;                // interval for storing the tree                        
  RSInt          paramUpdateInterval;           // interval for updating parameters                     
  RSBoolean      updateKappa;                   // whether or not to update kappa                       
  RSBoolean      updateTheta;                   // whether or not to update theta                       
  RSBoolean      updatePi;                      // whether or not to update pi                          
  RSBoolean      updateTtp;                     // whether or not to update ttp                         
  RSBoolean      updateGamma;                   // whether or not to update gamma                       
  RSBoolean      updateGrev;                    // whether or not to update grev model                  
  RSBoolean      updateInvariantProb;           // whether or not to update invariant prob              
  RSInt          tuneInterval;                  // interval for updating global tune in burn in         
  RSInt          windowInterval;                // interval for printing to the terminal                
  RSBoolean      molecularClock;                // whether or not molecular clock is used               
  RSModel        model;		                // likelihood model                                     
  RSCategoryList categoryList;                  // list of categories                                   
  RSBoolean      singleKappa;                   // whether or not all categories use same kappa         
  RSDoubleArrayS initialKappa;                  // the inital value for kappa                           
  RSDoubleArray  initialTheta;                  // the inital value for theta                           
  RSBoolean      estimatePi;                    // estimate pi from base frequencies                    
  RSDoubleArray  initialPia;                    // the initial value for pia                              
  RSDoubleArray  initialPic;                    // the initial value for pic                              
  RSDoubleArray  initialPig;                    // the initial value for pig                              
  RSDoubleArray  initialPit;                    // the initial value for pit                              
  RSDoubleArrayS initialTtp;                    // the inital value for ttp                             
  RSDoubleArrayS initialGamma;                  // the inital value for gamma                           
  RSDoubleArray  initialRac;                    // the inital value for rac                             
  RSDoubleArray  initialRag;                    // the inital value for rag
  RSDoubleArray  initialRat;                    // the inital value for rat                             
  RSDoubleArray  initialRcg;                    // the inital value for rcg                             
  RSDoubleArray  initialRct;                    // the inital value for rct                             
  RSDoubleArray  initialRgt;                    // the inital value for rgt                             
  RSBoolean      useInvariantSites;             // whether or not to use invariant sites                
  RSDoubleArray  invariantProbability;          // the invariant probability                            
  RSDoubleArray  invariantProbPriorMean;        // the invariant prob. prior mean                       
  RSDoubleArray  invariantProbPriorSD;          // the invariant prob. prior standard dev.              
  RSString       dataFile;                      // the name of the file where the data is stored
  RSInt          outgroup;                      // outgroup for non-clock case                          
  RSDouble       globalTune;                    // the size of half the window for updating valley depths
  RSDouble       kappaTune;                     // the size of half the window for updating kappa
  RSDouble       thetaTune;                     // tuning parameter for updating theta                  
  RSDouble       piTune;                        // tuning parameter for updating pi                     
  RSDouble       ttpTune;                       // tuning parameter for updating ttp                    
  RSDouble       gammaTune;                     // tuning parameter for updating gamma                  
  RSDouble       grevTune;                      // tuning parameter for updating grev model             
  RSDouble       invariantProbTune;             // tuning parameter for updating invariant pr.          
  RSDouble       localTune;                     // tuning parameter for stretching factor in local deformation algorithm
  RSBoolean      useBeta;                       // whether or not to use the beta distribution during local deformation
  RSDouble       betaTune;                      // tuning parameter for updating theta                  
  RSBoolean      printAllTrees;                 // Whether or not to print all the trees
  RSDouble       maxInitialTreeHeight;          // the maximum size of a valley depth for the initial tree
  RSString       fileRoot;                      // prefix for names of output files                     
  RSTreeType     initialTreeType;               // source of the input tree                             
  RSString       treeFile;                      // file containing initial tree                         
  RSBoolean      newickFormat;                  // whether or not to read and print in newick format

  ifstream flogp;                               // log likelihood output file
  ifstream fkappa;                              // parameter output file
  ifstream ftop;                                // tree topology output file
  ifstream fout;                                // summary statistics and run settings output
  ifstream ftree;                               // final tree output file
};

#endif
