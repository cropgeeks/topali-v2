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


/** MPI TAGS */

#define COMPUTE_TREE 0
#define TREE         1
#define FINALIZE     2
#define JOB_REQUEST  3

/*  Program constants and parameters  */

#define smoothings     32         /* maximum smoothing passes through tree */
#define iterations     10         /* maximum iterations of iterations per insert */
#define newzpercycle   1          /* iterations of makenewz per tree traversal */
#define nmlngth        100        /* number of characters in species name */
#define deltaz         0.00001    /* test of net branch length change in update */
#define defaultz       0.9        /* value of z assigned as starting point */
#define unlikely       -1.0E300   /* low likelihood for initialization */
#define largeDouble    1.0E300    /* same as positive number */



#define zmin       1.0E-15  /* max branch prop. to -log(zmin) (= 34) */
#define zmax (1.0 - 1.0E-6) /* min branch prop. to 1.0-zmax (= 1.0E-6) */
#define twotothe256  \
  115792089237316195423570985008687907853269984665640564039457584007913129639936.0
                                                     /*  2**256 (exactly)  */

#define minlikelihood  (1.0/twotothe256)
#define minusminlikelihood -minlikelihood

#define badEval        1.0
#define badZ           0.0
#define badRear         -1


#define TRUE             1
#define FALSE            0

#define treeNone         0
#define treeNewick       1
#define treeProlog       2
#define treePHYLIP       3
#define treeMaxType      3
#define treeDefType  treePHYLIP

#define LIKELIHOOD_EPSILON 0.0000001

#define ALPHA_MIN    0.01
#define ALPHA_MAX    1000.0

#define RATE_MIN     0.0000001
#define RATE_MAX     1000000.0

#define TT_MIN       0.0000001
#define TT_MAX       1000000.0

#define MODEL_EPSILON 0.0001
#define ITMAX 100

#define SHFT(a,b,c,d)                (a)=(b);(b)=(c);(c)=(d);
#define SIGN(a,b)                    ((b) > 0.0 ? fabs(a) : -fabs(a))

#define ABS(x)    (((x)<0)   ?  (-(x)) : (x))
#define MIN(x,y)  (((x)<(y)) ?    (x)  : (y))
#define MAX(x,y)  (((x)>(y)) ?    (x)  : (y))
#define NINT(x)   ((int) ((x)>0 ? ((x)+0.5) : ((x)-0.5)))


#define PointGamma(prob,alpha,beta)  PointChi2(prob,2.0*(alpha))/(2.0*(beta))

#define programName        "RAxML-VI-HPC"
#define programVersion     "2.2.3"
#define programDate        "FEBRUARY 2006"

#define  TREE_EVALUATION         0
#define  BIG_RAPID_MODE          1
#define  PARALLEL_MODE           2
#define  CALC_BIPARTITIONS       3
#define  SPLIT_MULTI_GENE        4
#define  CHECK_ALIGNMENT         5

#define M_GTRCAT      1
#define M_GTRGAMMA    2
#define M_PROTCAT     5
#define M_PROTGAMMA   6

#define DAYHOFF  0
#define DCMUT    1
#define JTT      2
#define MTREV    3
#define WAG      4
#define RTREV    5
#define CPREV    6
#define VT       7
#define BLOSUM62 8
#define MTMAM    9
#define GTR      10

typedef  int boolean;

typedef  struct  likelihood_vector 
{
  double      a, c, g, t;
  int         exp;
} 
  likelivector;

typedef  struct  gamma_likelihood_vector 
{
 
  double     a0, c0, g0, t0, a1, c1, g1, t1, a2, c2, g2, t2, a3, c3, g3, t3;
  int        exp;
} 
  gammalikelivector;


typedef struct
{
 
  double     v[20];
  int        exp;
} 
  protlikelivector;

typedef struct
{
 
  double     v[80];
  int        exp;
} 
  protgammalikelivector;

typedef struct pvv 
{
  int  parsimonyScore;
  char parsimonyState; 
} 
  parsimonyVector;

typedef struct
{
  int  parsimonyScore;
  int  parsimonyState; 
} 
  parsimonyVectorProt;


typedef struct ratec 
{
  double accumulatedSiteLikelihood;
  double rate;
} 
  rateCategorize;


typedef  struct noderec 
{
  double           z;
  struct noderec  *next;
  struct noderec  *back;
  int              number;
  void            *x;   
  char            *tip;
} 
  node, *nodeptr;
 
typedef struct 
  {
    double lh;
    int number;
  }
  info;

typedef struct bInf {
  double likelihood;  
  nodeptr node;
} bestInfo;

typedef struct iL {
  bestInfo *list;
  int n;
  int valid;
} infoList;

typedef struct {
  nodeptr p; /* not required for other lists*/
  int pNum;
  int qNum;
  int support;
  int length;/*not required for other lists*/
  int *entries;
} bList;


typedef  struct 
{
  int              numsp;      
  int              sites;     
  char             **y;          
  char             *y0;         
  int              *wgt;        
  int              *wgt2;        
} rawdata;

typedef  struct {
  int             *alias;       /* site representing a pattern */
  int             *reAlias;
  int             *aliaswgt;    /* weight by pattern */
  int             *rateCategory; 
  int              endsite;     /* # of sequence patterns */
  int              wgtsum;      /* sum of weights of positions */
  double          *patrat;      /* rates per pattern */
  double          *patratStored;
  double          *wr;          /* weighted rate per pattern */
  double          *wr2;         /* weight*rate**2 per pattern */
} cruncheddata;



typedef  struct  {       

  /* model-dependent stuff */  
  likelivector     *gtrTip;
  protlikelivector *protTip;
  double           *ttRatios;
  double           *xvs;
  double           *invfreqrs;
  double           *invfreqys;
  double           *EI;
  double           *EV;
  double           *EIGN;
  double           *frequencies;
  double           *initialRates;
  double           *gammaRates;
  double           *alphas;
  double           *fracchanges;  
  double            fracchange;
  double            lhCutoff;
  double            lhAVG;
  unsigned long     lhDEC;              
  unsigned long     itCount;
  int               gammaCategories;

  /* model stuff end */

  double           startLH;
  double           endLH;
  double           likelihood;   
  double          *likelihoods;
  bList           *ML_Tree;
  int              countML_Tree;
  int              numberOfTrees;
  node           **nodep;
  node            *start; 
  int              mxtips;
  int              *model;
  int              *saveModel;
  int              **modelIndices;
  int              *constraintVector;
  int              ntips;
  int              nextnode;
  int              NumberOfCategories;
  int              NumberOfModels;
  int              parsimonyLength; 
  int              checkPointCounter;
  int              treeID;
  int              numberOfRates;
  int              numberOfOutgroups;
  int             *outgroupNums;
  char           **outgroups;
  boolean          prelabeled; 
  boolean          smoothed;
  boolean          rooted;
  boolean          grouped;
  boolean          constrained;
  boolean          doCutoff;
  rawdata         *rdta;        
  cruncheddata    *cdta; 
  
  char **nameList;
  char *tree_string;
  int treeStringLength;
  int bestParsimony;
  double bestOfNode;
  nodeptr removeNode;
  nodeptr insertNode;

  double zqr;
  double currentZQR;

  double currentLZR;
  double currentLZQ;
  double currentLZS;
  double currentLZI;
  double lzs;
  double lzq;
  double lzr;
  double lzi;

} tree;

typedef struct conntyp {
    double           z;           /* branch length */
    node            *p, *q;       /* parent and child sectors */
    void            *valptr;      /* pointer to value of subtree */
    int              descend;     /* pointer to first connect of child */
    int              sibling;     /* next connect from same parent */
    } connect, *connptr;

typedef  struct {
    double           likelihood;
  int              initialTreeNumber;
    connect         *links;       /* pointer to first connect (start) */
    node            *start;
    int              nextlink;    /* index of next available connect */
                                  /* tr->start = tpl->links->p */
    int              ntips;
    int              nextnode;
    int              scrNum;      /* position in sorted list of scores */
    int              tplNum;      /* position in sorted list of trees */
    
    boolean          prelabeled;  /* the possible tip names are known */
    boolean          smoothed;    /* branch optimization converged? */
    } topol;

typedef struct {
    double           best;        /* highest score saved */
    double           worst;       /* lowest score saved */
    topol           *start;       /* starting tree for optimization */
    topol          **byScore;
    topol          **byTopol;
    int              nkeep;       /* maximum topologies to save */
    int              nvalid;      /* number of topologies saved */
    int              ninit;       /* number of topologies initialized */
    int              numtrees;    /* number of alternatives tested */
    boolean          improved;
    } bestlist;

typedef  struct {  
  int              categories;
  int              model;
  int              bestTrav;
  int              max_rearrange;
  int              stepwidth;
  int              initial;
  boolean          initialSet;
  int              mode;
  long             boot;          
  boolean          bootstrapBranchLengths;
  boolean          restart;
  boolean          useWeightFile;  
  boolean          useMultipleModel;
  boolean          constraint;
  boolean          grouping;
  boolean          randomStartingTree;
  boolean          categorizeGamma;
  int            protEmpiricalFreqs;
  int            proteinMatrix;
  int            checkpoints;
  int            startingTreeOnly;
  int            rapidParsimony;
  int            useMixedModel;
  int            multipleRuns;
  int            parsimonySeed;
  int            *protModels; 
  int            *protFreqs;
  boolean        printRates;
  boolean        outgroup;
  double         likelihoodEpsilon;  
} analdef;





/****************************** FUNCTIONS ****************************************************/


extern double evaluatePartialGTRCAT ( tree *tr, nodeptr p, int i, double ki );
extern boolean newviewGTRCAT ( tree *tr, nodeptr p );
extern double evaluateGTRCAT ( tree *tr, nodeptr p );
extern double makenewzGTRCAT ( tree *tr, nodeptr p, nodeptr q, double z0, int maxiter );
extern double evaluatePartialGTRCATMULT ( tree *tr, nodeptr p, int i, double ki );
extern boolean newviewGTRCATMULT ( tree *tr, nodeptr p );
extern double evaluateGTRCATMULT ( tree *tr, nodeptr p );
extern double makenewzGTRCATMULT ( tree *tr, nodeptr p, nodeptr q, double z0, int maxiter );
extern boolean newviewPARTITIONGTRCATMULT ( tree *tr, nodeptr p, int model );
extern double evaluateGTRCATMULTPARTITION ( tree *tr, nodeptr p, int model );
extern double evaluatePartialGTRCATMULT ( tree *tr, nodeptr p, int i, double ki );
extern boolean newviewGTRCATMULT ( tree *tr, nodeptr p );
extern double evaluateGTRCATMULT ( tree *tr, nodeptr p );
extern double makenewzGTRCATMULT ( tree *tr, nodeptr p, nodeptr q, double z0, int maxiter );
extern boolean newviewPARTITIONGTRCATMULT ( tree *tr, nodeptr p, int model );
extern double evaluateGTRCATMULTPARTITION ( tree *tr, nodeptr p, int model );
extern double evaluatePartialGTRCAT ( tree *tr, nodeptr p, int i, double ki );
extern boolean newviewGTRCAT ( tree *tr, nodeptr p );
extern double evaluateGTRCAT ( tree *tr, nodeptr p );
extern double makenewzGTRCAT ( tree *tr, nodeptr p, nodeptr q, double z0, int maxiter );
extern double evaluatePartialGTRCATPROT ( tree *tr, nodeptr p, int i, double ki );
extern double evaluateGTRCATPROT ( tree *tr, nodeptr p );
extern double makenewzGTRCATPROT ( tree *tr, nodeptr p, nodeptr q, double z0, int maxiter );
extern boolean newviewGTRCATPROT ( tree *tr, nodeptr p );
extern double evaluatePartialGTRCATPROTMULT ( tree *tr, nodeptr p, int i, double ki );
extern double evaluateGTRCATPROTMULT ( tree *tr, nodeptr p );
extern double makenewzGTRCATPROTMULT ( tree *tr, nodeptr p, nodeptr q, double z0, int maxiter );
extern boolean newviewGTRCATPROTMULT ( tree *tr, nodeptr p );
extern boolean newviewPARTITIONGTRCATPROTMULT ( tree *tr, nodeptr p, int model );
extern double evaluateGTRCATPROTMULTPARTITION ( tree *tr, nodeptr p, int model );
extern double evaluatePartialGTRCATPROTMULT ( tree *tr, nodeptr p, int i, double ki );
extern double evaluateGTRCATPROTMULT ( tree *tr, nodeptr p );
extern double makenewzGTRCATPROTMULT ( tree *tr, nodeptr p, nodeptr q, double z0, int maxiter );
extern boolean newviewGTRCATPROTMULT ( tree *tr, nodeptr p );
extern double evaluatePartialGTRCATPROT ( tree *tr, nodeptr p, int i, double ki );
extern double evaluateGTRCATPROT ( tree *tr, nodeptr p );
extern double makenewzGTRCATPROT ( tree *tr, nodeptr p, nodeptr q, double z0, int maxiter );
extern boolean newviewGTRCATPROT ( tree *tr, nodeptr p );
extern boolean newviewGTRGAMMA ( tree *tr, nodeptr p );
extern double evaluateGTRGAMMA ( tree *tr, nodeptr p );
extern double evaluateGTRGAMMA_ARRAY ( tree *tr, nodeptr p, double *array );
extern double makenewzGTRGAMMA ( tree *tr, nodeptr p, nodeptr q, double z0, int maxiter );
extern boolean newviewGTRGAMMAMULT ( tree *tr, nodeptr p );
extern double evaluateGTRGAMMAMULT ( tree *tr, nodeptr p );
extern double makenewzGTRGAMMAMULT ( tree *tr, nodeptr p, nodeptr q, double z0, int maxiter );
extern boolean newviewGTRGAMMAMULTPARTITION ( tree *tr, nodeptr p, int model );
extern double evaluateGTRGAMMAMULTPARTITION ( tree *tr, nodeptr p, int model );
extern boolean newviewGTRGAMMAMULT ( tree *tr, nodeptr p );
extern double evaluateGTRGAMMAMULT ( tree *tr, nodeptr p );
extern double makenewzGTRGAMMAMULT ( tree *tr, nodeptr p, nodeptr q, double z0, int maxiter );
extern boolean newviewGTRGAMMAMULTPARTITION ( tree *tr, nodeptr p, int model );
extern double evaluateGTRGAMMAMULTPARTITION ( tree *tr, nodeptr p, int model );
extern boolean newviewGTRGAMMA ( tree *tr, nodeptr p );
extern double evaluateGTRGAMMA ( tree *tr, nodeptr p );
extern double makenewzGTRGAMMA ( tree *tr, nodeptr p, nodeptr q, double z0, int maxiter );
extern boolean newviewGTRGAMMAPROT ( tree *tr, nodeptr p );
extern double evaluateGTRGAMMAPROT ( tree *tr, nodeptr p );
extern double makenewzGTRGAMMAPROT ( tree *tr, nodeptr p, nodeptr q, double z0, int maxiter );
extern boolean newviewGTRGAMMAPROTMULT ( tree *tr, nodeptr p );
extern double evaluateGTRGAMMAPROTMULT ( tree *tr, nodeptr p );
extern double makenewzGTRGAMMAPROTMULT ( tree *tr, nodeptr p, nodeptr q, double z0, int maxiter );
extern boolean newviewGTRGAMMAPROTMULTPARTITION ( tree *tr, nodeptr p, int model );
extern double evaluateGTRGAMMAPROTMULTPARTITION ( tree *tr, nodeptr p, int model );
extern boolean newviewGTRGAMMAPROTMULT ( tree *tr, nodeptr p );
extern double evaluateGTRGAMMAPROTMULT ( tree *tr, nodeptr p );
extern double makenewzGTRGAMMAPROTMULT ( tree *tr, nodeptr p, nodeptr q, double z0, int maxiter );
extern boolean newviewGTRGAMMAPROTMULTPARTITION ( tree *tr, nodeptr p, int model );
extern double evaluateGTRGAMMAPROTMULTPARTITION ( tree *tr, nodeptr p, int model );
extern boolean newviewGTRGAMMAPROT ( tree *tr, nodeptr p );
extern double evaluateGTRGAMMAPROT ( tree *tr, nodeptr p );
extern double makenewzGTRGAMMAPROT ( tree *tr, nodeptr p, nodeptr q, double z0, int maxiter );
extern double gettime ( void );
extern int gettimeSrand ( void );
extern double randum ( long *seed );
extern int filexists ( char *filename );
extern void *getxnode ( nodeptr p );
extern void hookup ( nodeptr p, nodeptr q, double z );
extern void getnums ( rawdata *rdta );
extern boolean digitchar ( int ch );
extern boolean whitechar ( int ch );
extern void uppercase ( int *chptr );
extern int findch ( int c );
extern void getyspace ( rawdata *rdta );
extern void freeyspace ( rawdata *rdta );
extern boolean setupTree ( tree *tr, int nsites, analdef *adef );
extern void freeTreeNode ( nodeptr p );
extern void freeTree ( tree *tr );
extern boolean getdata ( boolean reRead, analdef *adef, rawdata *rdta, tree *tr );
extern void inputweights ( analdef *adef, rawdata *rdta, cruncheddata *cdta );
extern void getinput ( boolean reRead, analdef *adef, rawdata *rdta, cruncheddata *cdta, tree *tr );
extern void makeboot ( analdef *adef, rawdata *rdta, cruncheddata *cdta );
extern void sitesort ( rawdata *rdta, cruncheddata *cdta, tree *tr, analdef *adef );
extern void sitecombcrunch ( rawdata *rdta, cruncheddata *cdta, tree *tr, analdef *adef );
extern boolean makeweights ( analdef *adef, rawdata *rdta, cruncheddata *cdta, tree *tr );
extern boolean makevalues ( rawdata *rdta, cruncheddata *cdta, tree *tr, analdef *adef, boolean reRead );
extern int sequenceSimilarity ( char *tipJ, char *tipK, int n );
extern void checkSequences ( tree *tr, rawdata *rdta, analdef *adef );
extern void splitMultiGene ( tree *tr, rawdata *rdta, analdef *adef );
extern void allocNodex ( tree *tr, analdef *adef );
extern void freeNodex ( tree *tr );
extern void initAdef ( analdef *adef );
extern int modelExists ( char *model, analdef *adef );
extern int mygetopt ( int argc, char **argv, char *opts, int *optind, char **optarg );
extern void get_args ( int argc, char *argv[], boolean print_usage, analdef *adef, rawdata *rdta, tree *tr );
extern void errorExit ( int e );
extern void makeFileNames ( tree *tr, analdef *adef, int argc, char *argv[] );
extern void readData ( boolean reRead, analdef *adef, rawdata *rdta, cruncheddata *cdta, tree *tr );
extern void printVersionInfo ( void );
extern void printREADME ( void );
extern void printModelAndProgramInfo ( tree *tr, analdef *adef, int argc, char *argv[] );
extern void printResult ( tree *tr, analdef *adef, boolean finalPrint );
extern void printBootstrapResult ( tree *tr, analdef *adef, boolean finalPrint );
extern void printBipartitionResult ( tree *tr, analdef *adef, boolean finalPrint );
extern void printLog ( tree *tr, analdef *adef, boolean finalPrint );
extern void printStartingTree ( tree *tr, analdef *adef, boolean finalPrint );
extern void writeInfoFile ( analdef *adef, tree *tr, double t );
extern void finalizeInfoFile ( tree *tr, analdef *adef );
extern int main ( int argc, char *argv[] );
extern int countTips ( nodeptr p );
extern void getTips ( nodeptr p, int *c, int *entries );
extern void makeBipartitionsRec ( nodeptr p, bList *blThis, int *bCountThis );
extern bList *bipartitionList ( tree *tr, boolean initialList, int *bCountThis );
extern void printBlist ( bList *blThis, int n );
extern void freeBList ( bList *blThis, int n );
extern void updateReferenceList ( bList *referenceList, int referenceListLength, bList *currentList, int currentListLength );
extern void calcBipartitions ( tree *tr, analdef *adef );
extern void makeVal ( char code, double *val );
extern void baseFrequenciesGTR ( rawdata *rdta, cruncheddata *cdta, tree *tr, analdef *adef );
extern void initProtMat ( tree *tr, int model, double f[20], analdef *adef, int proteinMatrix );
extern void initReversibleGTR ( rawdata *rdta, cruncheddata *cdta, tree *tr, analdef *adef, int model );
extern double LnGamma ( double alpha );
extern double IncompleteGamma ( double x, double alpha, double ln_gamma_alpha );
extern double PointNormal ( double prob );
extern double PointChi2 ( double prob, double v );
extern void makeGammaCats ( tree *tr, int model );
extern void initModel ( tree *tr, rawdata *rdta, cruncheddata *cdta, analdef *adef );
extern void doBootstrap ( tree *tr, analdef *adef, rawdata *rdta, cruncheddata *cdta );
extern void doInference ( tree *tr, analdef *adef, rawdata *rdta, cruncheddata *cdta );
extern double evaluateRate ( tree *tr, int i, double rate, analdef *adef, int model );
extern double evaluateAlpha ( tree *tr, double alpha, int model, analdef *adef );
extern int brakAlpha ( double *param, double *ax, double *bx, double *cx, double *fa, double *fb, double *fc, double lim_inf, double lim_sup, tree *tr, int model, analdef *adef );
extern double brentAlpha ( double ax, double bx, double cx, double fa, double fb, double fc, double tol, double *xmin, int model, tree *tr, analdef *adef );
extern double brentRates ( double ax, double bx, double cx, double fa, double fb, double fc, double tol, double *xmin, int model, tree *tr, analdef *adef, int i );
extern int brakRates ( double *param, double *ax, double *bx, double *cx, double *fa, double *fb, double *fc, double lim_inf, double lim_sup, tree *tr, int i, analdef *adef, int model );
extern void optAlpha ( tree *tr, analdef *adef, double modelEpsilon, int model );
extern double optRates ( tree *tr, analdef *adef, double modelEpsilon, int model );
extern void resetBranches ( tree *tr );
extern void modOpt ( tree *tr, analdef *adef );
extern void optimizeAlphaMULT ( tree *tr, int model, analdef *adef );
extern void optimizeAlpha ( tree *tr, analdef *adef );
extern void alterRates ( tree *tr, int k, analdef *adef );
extern double alterRatesMULT ( tree *tr, int k, analdef *adef, int model );
extern void optimizeRates ( tree *tr, analdef *adef );
extern void categorize ( tree *tr, rateCategorize *rc );
extern void optimizeRateCategories ( tree *tr, int categorized, int _maxCategories, analdef *adef );
extern void optimizeAlphas ( tree *tr, analdef *adef );
extern int optimizeModel ( tree *tr, analdef *adef, int finalOptimization );
extern void optimizeAllRateCategories ( tree *tr );
extern void optimizeRatesOnly ( tree *tr, analdef *adef );
extern boolean lineContainsOnlyWhiteChars ( char *line );
extern int isNum ( char c );
extern void skipWhites ( char **ch );
extern void analyzeIdentifier ( char **ch, analdef *adef, int modelNumber );
extern void setModel ( int model, int position, int *a );
extern int myGetline ( char **lineptr, int *n, FILE *stream );
extern void parsePartitions ( analdef *adef, rawdata *rdta, cruncheddata *cdta, tree *tr, boolean reRead );
extern void newviewParsimonyDNA ( tree *tr, nodeptr p );
extern int evaluateParsimonyDNA ( tree *tr, nodeptr p );
extern void newviewParsimonyPROT ( tree *tr, nodeptr p );
extern int evaluateParsimonyPROT ( tree *tr, nodeptr p );
extern void initravParsimonyNormal ( tree *tr, nodeptr p );
extern void initravParsimony ( tree *tr, nodeptr p, int *constraintVector );
extern void insertParsimony ( tree *tr, nodeptr p, nodeptr q );
extern void insertRandom ( nodeptr p, nodeptr q );
extern nodeptr buildNewTip ( tree *tr, nodeptr p );
extern void buildSimpleTree ( tree *tr, int ip, int iq, int ir );
extern void buildSimpleTreeRandom ( tree *tr, int ip, int iq, int ir );
extern int checker ( tree *tr, nodeptr p );
extern void testInsertParsimony ( tree *tr, nodeptr p, nodeptr q );
extern void restoreTreeParsimony ( tree *tr, nodeptr p, nodeptr q );
extern int markBranches ( nodeptr *branches, nodeptr p, int *counter );
extern void addTraverseParsimony ( tree *tr, nodeptr p, nodeptr q, int mintrav, int maxtrav );
extern nodeptr findAnyTip ( nodeptr p );
extern int randomInt ( int n );
extern void makePermutation ( int *perm, int n, analdef *adef );
extern void initravDISTParsimony ( tree *tr, nodeptr p, int distance );
extern nodeptr removeNodeParsimony ( tree *tr, nodeptr p );
extern boolean tipHomogeneityChecker ( tree *tr, nodeptr p, int grouping );
extern int rearrangeParsimony ( tree *tr, nodeptr p, int mintrav, int maxtrav );
extern void restoreTreeRearrangeParsimony ( tree *tr );
extern void allocNodexParsimony ( tree *tr, analdef *adef );
extern void freeNodexParsimony ( tree *tr );
extern void restore ( tree *tr, int *alias );
extern void sortInformativeSites ( tree *tr, int *informative, int *alias );
extern void determineUninformativeSites ( tree *tr, int *informative, int *alias );
extern void makeRandomTree ( tree *tr, analdef *adef );
extern void reorderNodes ( tree *tr, nodeptr *np, nodeptr p, int *count );
extern void nodeRectifier ( tree *tr );
extern void makeParsimonyTree ( tree *tr, analdef *adef );
extern void makeParsimonyTreeIncomplete ( tree *tr, analdef *adef );
extern void tred2 ( double *a, const int n, const int np, double *d, double *e );
extern double pythag ( double a, double b );
extern void tqli ( double *d, double *e, int n, int np, double *z );
extern boolean initrav ( tree *tr, nodeptr p );
extern boolean initravDIST ( tree *tr, nodeptr p, int distance );
extern void initravPartition ( tree *tr, nodeptr p, int model );
extern double partitionLikelihood ( tree *tr, int model );
extern boolean update ( tree *tr, nodeptr p );
extern void updateNNI ( tree *tr, nodeptr p );
extern boolean smooth ( tree *tr, nodeptr p );
extern boolean smoothTree ( tree *tr, int maxtimes );
extern boolean localSmooth ( tree *tr, nodeptr p, int maxtimes );
extern void resetInfoList ( void );
extern void initInfoList ( int n );
extern void freeInfoList ( void );
extern void insertInfoList ( nodeptr node, double likelihood );
extern boolean smoothRegion ( tree *tr, nodeptr p, int region );
extern boolean regionalSmooth ( tree *tr, nodeptr p, int maxtimes, int region );
extern nodeptr removeNodeBIG ( tree *tr, nodeptr p );
extern nodeptr removeNodeRestoreBIG ( tree *tr, nodeptr p );
extern boolean insertBIG ( tree *tr, nodeptr p, nodeptr q, boolean glob );
extern boolean insertRestoreBIG ( tree *tr, nodeptr p, nodeptr q, boolean glob );
extern void restoreTopologyOnly ( tree *tr, bestlist *bt );
extern boolean testInsertBIG ( tree *tr, nodeptr p, nodeptr q );
extern void addTraverseBIG ( tree *tr, nodeptr p, nodeptr q, int mintrav, int maxtrav );
extern int rearrangeBIG ( tree *tr, nodeptr p, int mintrav, int maxtrav );
extern void traversalOrder ( nodeptr p, int *count, nodeptr *nodeArray );
extern double treeOptimizeRapid ( tree *tr, int mintrav, int maxtrav, analdef *adef, bestlist *bt );
extern boolean testInsertRestoreBIG ( tree *tr, nodeptr p, nodeptr q );
extern void restoreTreeFast ( tree *tr );
extern int determineRearrangementSetting ( tree *tr, analdef *adef, bestlist *bestT, bestlist *bt );
extern void computeBIGRAPID ( tree *tr, analdef *adef );
extern boolean treeEvaluate ( tree *tr, double smoothFactor );
extern void *tipValPtr ( nodeptr p );
extern int cmpTipVal ( void *v1, void *v2 );
extern topol *setupTopol ( int maxtips, int nsites );
extern void freeTopol ( topol *tpl );
extern int saveSubtree ( nodeptr p, topol *tpl );
extern nodeptr minSubtreeTip ( nodeptr p0 );
extern nodeptr minTreeTip ( nodeptr p );
extern void saveTree ( tree *tr, topol *tpl );
extern void copyTopol ( topol *tpl1, topol *tpl2 );
extern boolean restoreTreeRecursive ( topol *tpl, tree *tr );
extern boolean restoreTree ( topol *tpl, tree *tr );
extern boolean restoreTopology ( topol *tpl, tree *tr );
extern int initBestTree ( bestlist *bt, int newkeep, int numsp, int sites );
extern void resetBestTree ( bestlist *bt );
extern boolean freeBestTree ( bestlist *bt );
extern int cmpSubtopol ( connptr p10, connptr p1, connptr p20, connptr p2 );
extern int cmpTopol ( void *tpl1, void *tpl2 );
extern int cmpTplScore ( void *tpl1, void *tpl2 );
extern int findInList ( void *item, void *list[], int n, int (* cmpFunc)() );
extern int findTreeInList ( bestlist *bt, tree *tr );
extern int saveBestTree ( bestlist *bt, tree *tr );
extern int recallBestTreeRecursive ( bestlist *bt, int rank, tree *tr );
extern int recallBestTree ( bestlist *bt, int rank, tree *tr );
extern int recallBestTopology ( bestlist *bt, int rank, tree *tr );
extern boolean readKeyValue ( char *string, char *key, char *format, void *value );
extern char *Tree2String ( char *treestr, tree *tr, nodeptr p, boolean printBranchLengths, boolean printNames, 
			   boolean printLikelihood, boolean rellTree, boolean finalPrint, analdef *adef );
extern int treeFinishCom ( FILE *fp, char **strp );
extern int treeGetCh ( FILE *fp );
extern boolean treeLabelEnd ( int ch );
extern boolean treeGetLabel ( FILE *fp, char *lblPtr, int maxlen );
extern boolean treeFlushLabel ( FILE *fp );
extern int treeFindTipByLabel ( char *str, tree *tr );
extern int treeFindTipName ( FILE *fp, tree *tr );
extern void treeEchoContext ( FILE *fp1, FILE *fp2, int n );
extern boolean treeProcessLength ( FILE *fp, double *dptr );
extern int treeFlushLen ( FILE *fp );
extern boolean treeNeedCh ( FILE *fp, int c1, char *where );
extern boolean addElementLen ( FILE *fp, tree *tr, nodeptr p, boolean readBranchLengths );
extern int saveTreeCom ( char **comstrp );
extern boolean processTreeCom ( FILE *fp, tree *tr );
extern nodeptr uprootTree ( tree *tr, nodeptr p );
extern boolean treeReadLen ( FILE *fp, tree *tr, analdef *adef );
extern void treeReadTopologyOnly ( FILE *fp, tree *tr, analdef *adef, boolean readBranches );
extern boolean addElementLenMULT ( FILE *fp, tree *tr, nodeptr p, int partitionCounter );
extern boolean treeReadLenMULT ( FILE *fp, tree *tr, analdef *adef );
extern int str_treeFinishCom ( char **treestrp, char **strp );
extern int str_treeGetCh ( char **treestrp );
extern boolean str_treeGetLabel ( char **treestrp, char *lblPtr, int maxlen );
extern boolean str_treeFlushLabel ( char **treestrp );
extern int str_treeFindTipName ( char **treestrp, tree *tr );
extern boolean str_treeProcessLength ( char **treestrp, double *dptr );
extern boolean str_treeFlushLen ( char **treestrp );
extern boolean str_treeNeedCh ( char **treestrp, int c1, char *where );
extern boolean str_processTreeCom ( tree *tr, char **treestrp );
extern boolean str_processTreeComMerge ( int *ntaxa, char **treestrp );
extern boolean str_addElementLen ( char **treestrp, tree *tr, nodeptr p );
extern boolean str_treeReadLen ( char *treestr, tree *tr );
extern double str_readTreeLikelihood ( char *treestr );
extern void getStartingTree ( tree *tr, analdef *adef );
