/*
 *  MrBayes 3.1.1
 *
 *  copyright 2002-2005
 *
 *  John P. Huelsenbeck
 *  Section of Ecology, Behavior and Evolution
 *  Division of Biological Sciences
 *  University of California, San Diego
 *  La Jolla, CA 92093-0116
 *
 *  johnh@biomail.ucsd.edu
 *
 *	Fredrik Ronquist
 *  School of Computational Science
 *  Florida State University
 *  Tallahassee, FL 32306-4120
 *
 *  ronquist@csit.fsu.edu
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details (www.gnu.org).
 *
 * 07/06/2005: Paul, added patch from Konrad Scheffler <konrad@cbio.uct.ac.za>, tagged khs07062005. 
 * 
 */
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <math.h>
#include <string.h>
#include <ctype.h>
#include <limits.h>
#include "mb.h"
#include "globals.h"
#include "bayes.h"
#include "mcmc.h"
#include "model.h"
#include "command.h"
#include "mbmath.h"
#include "sump.h"
#include "sumt.h"
#include "plot.h"

#if defined(WIN_VERSION) && !defined(__GNUC__)
#define VISUAL
#endif

#ifdef VISUAL
/* NO_ERROR is defined in mb.h (as 0) and also in WinError.h (as 0L) */
#undef NO_ERROR 
/* ERROR is defined in mb.h (as 1) and also in WinGDI.h (as 0). we use the mb.h value */
#undef ERROR
#include <windows.h>
#undef ERROR
#define ERROR 1
#define SIGINT CTRL_C_EVENT
#else
#include <signal.h>
typedef void (*sighandler_t)(int);
#endif
/*static int confirmAbortRun;*/

#if defined(__MWERKS__)
#include "SIOUX.h"
#endif

#define	A							0
#define	C							1
#define	G							2
#define	T							3
#define	AA							0
#define	AC							1
#define	AG							2
#define	AT							3
#define	CA							4
#define	CC							5
#define	CG							6
#define	CT							7
#define	GA							8
#define	GC							9
#define	GG							10
#define	GT							11
#define	TA							12
#define	TC							13
#define	TG							14
#define	TT							15
#define LIKE_EPSILON				1.0e-300
#define BRLEN_EPSILON				1.0e-8
#define RESCALE_FREQ				1			/* node cond like rescaling frequency */
#define	SCALER_REFRESH_FREQ			20			/* generations between refreshing scaler nodes */		
#define	NUCMODEL_4BY4				0
#define	NUCMODEL_DOUBLET			1
#define	NUCMODEL_CODON				2
#define MAX_SMALL_JUMP				10			/* threshold for precalculating trans probs of adgamma model */
#define BIG_JUMP					100			/* threshold for using stationary approximation */
#define MAX_RUNS                    120         /* maximum number of independent runs */
#define	PFILE                       0
#define TFILE						1
#define	CALFILE						2
#define MCMCFILE                    3

/* debugging compiler statements */
#undef	DEBUG_COMPRESSDATA
#undef	DEBUG_ADDDUMMYCHARS
#undef	DEBUG_CREATEPARSMATRIX
#undef	DEBUG_SETUPTERMSTATE
#undef	DEBUG_INITCHAINCONDLIKES
#undef	DEBUG_SETCHAINPARAMS
#undef	DEBUG_BUILDSTARTTREE
#undef	DEBUG_RUNCHAIN
#undef	DEBUG_NOSHORTCUTS
#undef	DEBUG_NOSCALING
#undef	DEBUG_LOCAL
#undef	DEBUG_SPRCLOCK
#undef	DEBUG_TIPROBS_STD
#undef	DEBUG_RUN_WITHOUT_DATA
#undef	DEBUG_UNROOTED_SLIDER
#undef	DEBUG_BIASED_SPR
#undef  DEBUG_CONSTRAINTS

/* local (to this file) data types */
typedef struct pfnode
	{
	struct pfnode	*left;
	struct pfnode	*right;
	int				*count;
	long			*partition;
	} PFNODE;

/* local prototypes */
int     AddDummyChars (void);
int     AddTreeSamples (int from, int to);
PFNODE *AddPartition (PFNODE *r, long *p, int runId);
int     AddToPrintString (char *tempStr);
int     AddTreeToPartitionCounters (Tree *tree, int treeId, int runId);
Tree   *AllocateTree (int numTaxa, int isTreeRooted);
int     AttemptSwap (int swapA, int swapB, long int *seed);
int	    Bit (int n, long *p);
int     BuildConstraintTree (Tree *t, PolyTree *pt);
void    BuildExhaustiveSearchTree (Tree *t, int chain, int nTaxInTree, TreeInfo *tInfo);
int     BuildStartTree (Tree *t, long int *seed);
int     CalcLike_Adgamma (int d, Param *param, int chain, MrBFlt *lnL);
void    CalcPartFreqStats (PFNODE *p, STATS *stat);
void    CalculateTopConvDiagn (int numSamples);
#ifdef VISUAL
BOOL WINAPI CatchInterrupt(DWORD signum);
#else
void    CatchInterrupt(int signum);
#endif
void	CheckCharCodingType (Matrix *m, CharInfo *ci);
int		CheckConstraints (Tree *t);
int     CheckExpandedModels (void);
int     CheckSetConstraints (Tree *t);
int     CheckTemperature (void);
void	CloseMBPrintFiles (void);
PFNODE *CompactTree (PFNODE *p);
int     CompressData (void);
int     CondLikeDown_Bin (TreeNode *p, int division, int chain);
int     CondLikeDown_Gen (TreeNode *p, int division, int chain);
#		if !defined (SSE)
int	    CondLikeDown_NUC4 (TreeNode *p, int division, int chain);
#		else
int	    CondLikeDown_NUC4_SSE (TreeNode *p, int division, int chain);
#		endif
int	    CondLikeDown_NY98 (TreeNode *p, int division, int chain);
int     CondLikeDown_Std (TreeNode *p, int division, int chain);
int     CondLikeRoot_Bin (TreeNode *p, int division, int chain);
int     CondLikeRoot_Gen (TreeNode *p, int division, int chain);
#		if !defined (SSE)
int	    CondLikeRoot_NUC4 (TreeNode *p, int division, int chain);
#		else
int	    CondLikeRoot_NUC4_SSE (TreeNode *p, int division, int chain);
#		endif
int	    CondLikeRoot_NY98 (TreeNode *p, int division, int chain);
int     CondLikeRoot_Std (TreeNode *p, int division, int chain);
int	    CondLikeScaler_Gen (TreeNode *p, int division, int chain);
#		if !defined (FAST_LOG)
int	    CondLikeScaler_NUC4 (TreeNode *p, int division, int chain);
#		else
int	    CondLikeScaler_NUC4_fast (TreeNode *p, int division, int chain);
#		endif
int	    CondLikeScaler_NY98 (TreeNode *p, int division, int chain);
int     CondLikeScaler_Std (TreeNode *p, int division, int chain);
int     CondLikeUp_Bin (TreeNode *p, int division, int chain);
int     CondLikeUp_Gen (TreeNode *p, int division, int chain);
int     CondLikeUp_NUC4 (TreeNode *p, int division, int chain);
int     CondLikeUp_Std (TreeNode *p, int division, int chain);
void    CopyParams (int chain);
void	CopyPFNodeDown (PFNODE *p);
void    CopySubtreeToTree (Tree *subtree, Tree *t);
int		CopyToTreeFromPolyTree (Tree *to, PolyTree *from);
int		CopyToTreeFromTree (Tree *to, Tree *from);
void    CopyTrees (int chain);
void    CopyTreeToSubtree (Tree *t, Tree *subtree);
int     CreateParsMatrix (void);
#		if defined (MPI_ENABLED)
int     DoesProcHaveColdChain (void);
#       endif
int     ExhaustiveParsimonySearch (Tree *t, int chain, TreeInfo *tInfo);
int     ExtendChainQuery ();
int		FillNormalParams (long int *seed);
int     FillNumSitesOfPat (void);
int     FillRelPartsString (Param *p, char relPartString[100]);
int		FillTreeParams (long int *seed);
int     Flip01 (int x);
void    FlipOneBit (int n, long *p);
void    FreeChainMemory (void);
void	FreeTree (Tree *t);
void    GetChainIds (void);
int     GetEmpiricalFreqs (int *relParts, int nRelParts);
void    GetDownPass (Tree *t);
void    GetNodeDownPass (Tree *t, TreeNode *p, int *i, int *j);
MrBFlt  *GetParamVals (Param *parm, int chain, int state);
MrBFlt  *GetParamSubVals (Param *parm, int chain, int state);
int     GetParsimonyBrlens (Tree *t, int chain, MrBFlt *brlens);
int     GetParsimonyDownStates (Tree *t, int chain);
MrBFlt  GetParsimonyLength (Tree *t, int chain);
void    GetParsimonySubtreeRootstate (Tree *t, TreeNode *root, int chain);
void    GetPolyDownPass (PolyTree *t);
void    GetPolyNodeDownPass (PolyTree *t, PolyNode *p, int *i, int *j);
void    GetPossibleAAs (int aaCode, int aa[]);
void    GetPossibleNucs (int nucCode, int nuc[]);
void    GetPossibleRestrictionSites (int resSiteCode, int *sites);
int     GetRandomEmbeddedSubtree (Tree *t, int nTerminals, long *seed, int *nEmbeddedTrees);
MrBFlt  GetRate (int division, int chain);
void    GetSprParsimonyLengths (int chain, int nNodes1, int nNodes2, TreeNode **subTree1DP, TreeNode **subTree2DP, TreeNode *root2, MrBFlt *pLengths);
void    GetStamp (void);
void    GetSwappers (int *swapA, int *swapB, int curGen);
void    GetTempDownPassSeq (TreeNode *p, int *i, TreeNode **dp);
Tree    *GetTree (Param *parm, int chain, int state);
Tree    *GetTreeFromIndex (int index, int chain, int state);
int     InitCalibratedBrlens (Tree *t);
int     InitChainCondLikes (void);
int     InitClockBrlens (Tree *t);
int     InitInvCondLikes (void);
int     InitParsSets (void);
int     InitSprParsSets (void);
int     InitTermCondLikes (void);
int     IsBitSet (int i, long *bits);
int     IsClockSatisfied (Tree *t, MrBFlt tol);
int		IsPFNodeEmpty (PFNODE *p);
void    JukesCantor (MrBFlt *tiP, MrBFlt length);
PFNODE *LargestNonemptyPFNode (PFNODE *p, int *i, int j);
long    LastBlock (FILE *fp, char *lineBuf, int longestLine);
int     Likelihood_Adgamma (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats);
int	    Likelihood_Gen (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats);
int	    Likelihood_NUC4 (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats);
int     Likelihood_NY98 (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats);
int     Likelihood_Pars (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats);
int     Likelihood_ParsCodon (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats);
int     Likelihood_ParsStd (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats);
int     Likelihood_Res (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats);
int     Likelihood_Std (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats);
MrBFlt  LogLike (int chain);
MrBFlt  LogOmegaPrior (MrBFlt w1, MrBFlt w2, MrBFlt w3);
MrBFlt  LogPrior (int chain);
int     LnBirthDeathPriorPr (Tree *t, MrBFlt *prob, MrBFlt sR, MrBFlt eR, MrBFlt sF);
int     LnCoalescencePriorPr (Tree *t, MrBFlt *prob, MrBFlt theta, MrBFlt growth);
MrBFlt  LnP1 (MrBFlt t, MrBFlt l, MrBFlt m, MrBFlt r);
MrBFlt  LnVt (MrBFlt t, MrBFlt l, MrBFlt m, MrBFlt r);
void    MarkClsBelow (TreeNode *p);
MrBFlt  MaximumValue (MrBFlt x, MrBFlt y);
MrBFlt  MinimumValue (MrBFlt x, MrBFlt y);
int     Move_Aamodel (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_Adgamma (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_Beta (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_Beta_M (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_BiasedSpr (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_BrLen (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_ClockRate (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_Extinction (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_Extinction_M (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_ExtTBR (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_GammaShape_M (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_Growth (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_Local (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_LocalClock (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_NNI (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int		Move_NNI_Hetero (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int		Move_NodeSlider (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_Omega (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_Omega_M (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_OmegaBeta_M (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_OmegaGamma_M (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_OmegaCat (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_OmegaNeu (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_OmegaPos (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_OmegaPur (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_ParsEraser1 (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_Pinvar (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_RateMult_Dir (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_Revmat_Dir (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_Speciation (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_Speciation_M (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_SPRClock (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_Statefreqs (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_SwitchRate (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_SwitchRate_M (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_Theta (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_Tratio_Dir (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
int     Move_UnrootedSlider (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp);
void    NodeToNodeDistances (Tree *t, TreeNode *fromNode);
int     NumNonExcludedChar (void);
int     NumNonExcludedTaxa (void);
int     PickProposal (long int *seed);
int     PosSelProbs (TreeNode *p, int division, int chain);
int		PreparePrintFiles (void);
int     PrintAncStates_Bin (TreeNode *p, int division, int chain);
int     PrintAncStates_Gen (TreeNode *p, int division, int chain);
int     PrintAncStates_NUC4 (TreeNode *p, int division, int chain);
int     PrintAncStates_Std (TreeNode *p, int division, int chain);
int     PrintCalTree (int curGen, Tree *tree);
int	    PrintChainCondLikes (int chain, int precision);
int	    PrintCompMatrix (void);
int	    PrintMatrix (void);
int     PrintMCMCDiagnosticsToFile (int curGen);
void    PrintParamValues (Param *p, int chain, char *s);
int	    PrintParsMatrix (void);
int		PrintSiteRates_Gen (TreeNode *p, int division, int chain);
int		PrintSiteRates_Std (TreeNode *p, int division, int chain);
int     PrintStates (int curGen, int coldId);
int     PrintStatesToFiles (int n);
int     PrintSwapInfo (void);
int     PrintTermState (void);
void	PrintTiProbs (MrBFlt *tP, MrBFlt *bs, int nStates);
int     PrintTopConvInfo (void);
void    PrintToScreen (int curGen, time_t endingT, time_t startingT);
/* void    PrintToScreen (int curGen, clock_t endingT, clock_t startingT);*/
int     PrintTree (int curGen, Tree *tree);
int     ProcessStdChars (void);
int		RandResolve (Tree *destination, PolyTree *t, long *seed);
#       if defined (MPI_ENABLED)
int     ReassembleMoveInfo (void);
int     ReassembleSwapInfo (void);
#       endif
int		RecreateTree (Tree *t, char *s);
int     RemovePartition (PFNODE *r, long *p, int runId);
int     RemoveTreeFromPartitionCounters (Tree *tree, int treeId, int runId);
int     RemoveTreeSamples (int from, int to);
int     ReopenMBPrintFiles (void);
int     requestAbortRun(void);
int     ResetScalers (void);
int     RunChain (long int *seed);
int     SetAARates (void);
void    SetBit (int i, long *bits);
int     SetChainParams (void);
int		SetLikeFunctions (void);
int		SetModelInfo (void);
int		SetMoves (void);
int     SetNucQMatrix (MrBFlt **a, int n, int whichChain, int division, MrBFlt rateMult, MrBFlt *rA, MrBFlt *rS);
int     SetProteinQMatrix (MrBFlt **a, int n, int whichChain, int division, MrBFlt rateMult);
int	    SetStdQMatrix (MrBFlt **a, int nStates, MrBFlt *bs, int cType);
void    SetUpMoveTypes (void);
int     SetUpPartitionCounters (void);
int	    SetUpTermState (void);
int     ShowMCMCTree (Tree *t);
void    ShowValuesForChain (int chn);
PFNODE *SmallestNonemptyPFNode (PFNODE *p, int *i, int j);
int		StateCode_AA (int n);
int		StateCode_NUC4 (int n);
int		StateCode_Std (int n);
PFNODE *Talloc (void);
void	Tfree (PFNODE *r);
MrBFlt  Temperature (int x);
int     TiProbs_Fels (TreeNode *p, int division, int chain);
int     TiProbs_Gen (TreeNode *p, int division, int chain);
int     TiProbs_GenCov (TreeNode *p, int division, int chain);
int     TiProbs_Hky (TreeNode *p, int division, int chain);
int     TiProbs_JukesCantor (TreeNode *p, int division, int chain);
int     TiProbs_Std (TreeNode *p, int division, int chain);
int     TiProbs_Res (TreeNode *p, int division, int chain);
void    TouchAllPartitions (void);
void    TouchAllTreeNodes (Tree *t);
void    TouchAllTrees (int chain);
MrBFlt  TreeLength (Param *param, int chain);
int     UpDateCijk (int whichPart, int whichChain);
void    WriteTreeToFile (TreeNode *p, int showBrlens, int isRooted);
void    WriteCalTreeToFile (TreeNode *p, MrBFlt clockRate);

/* globals */
char			inputFileName[100];          /* input (NEXUS) file name                      */
Chain			chainParams;                 /* parameters of Markov chain                   */
int				numTaxa;                     /* number of taxa in character matrix           */
int				numChar;                     /* number of characters in character matrix     */
char			stamp[11];                   /* holds a unique identifier for each analysis  */

/* local (to this file) variables */
int				numLocalChains;              /* number of Markov chains                      */
int				numLocalTaxa;                /* number of non-excluded taxa                  */
int				localOutGroup;               /* outgroup for non-excluded taxa               */
char			*localTaxonNames = NULL;            /* stores names of non-excluded taxa            */
MrBFlt			*localTaxonAges = NULL;			 /* stores local taxon ages                      */
int				*chainId = NULL;                    /* information on the id (0 ...) of the chain   */
MrBFlt			*curLnL = NULL;                     /* stores log likelihood                        */
MrBFlt			*curLnPr = NULL;                    /* stores log prior probability                 */
ModelInfo		modelSettings[MAX_NUM_DIVS]; /* stores important info on model params        */
int				numParams;                   /* number of parameter types                    */
MrBFlt			*paramValues = NULL;                /* stores actual values of chain parameters     */
int				*relevantParts = NULL;              /* partitions that are affected by this move    */
MrBFlt			empiricalFreqs[200];         /* emprical base frequencies for partition      */
Tree			*mcmcTree;                   /* trees for mcmc                               */
int				numCalibratedTrees;          /* number of dated trees for one chain & state  */
int				numTrees;                    /* number of trees for one chain and state	     */
TreeNode		*mcmcNodes;                  /* space for tree nodes                         */
TreeNode		**mcmcNodePtrs;              /* space for node pointer vectors               */
Param			*params = NULL;			         /* params for mcmc								 */
Param			**subParamPtrs;		         /* pointer to subparams for topology params     */
int				*sympiIndex;                 /* sympi state freq index for multistate chars  */
int				numLocalChar;                /* number of non-excluded characters            */
int				compMatrixRowSize;	         /* row size of compressed matrix				 */
int				parsMatrixRowSize;	         /* row size of parsimony matrix                 */
int				parsNodeLenRowSize;	         /* row size of parsimony node length matrix     */
int				tiProbRowSize;		         /* row size of transition prob matrix			 */
int				paramValsRowSize;	         /* row size of paramValues matrix				 */
int				numCompressedChars;          /* number of compressed characters				 */
int				*compCharPos;		         /* char position in compressed matrix           */
int				*compColPos;		         /* column position in compressed matrix		 */
int				*termState = NULL;			         /* index to terminal state ti:s                 */
int				*isPartAmbig = NULL;		         /* does terminal taxon have partial ambiguity	 */
long int		*compMatrix;		         /* compressed character matrix					 */
long int		*parsMatrix = NULL;		         /* parsimony (bitset) matrix for terminals  	 */
long int		*parsSets = NULL;					 /* parsimony (bitset) matrix for int nodes 	 */
CLFlt			*numSitesOfPat;		         /* no. sites of each pattern					 */
CLFlt			*termCondLikes = NULL;		         /* cond likes for terminals                     */
CLFlt			*chainCondLikes;	         /* cond likes for chains						 */
int				condLikeRowSize;	         /* row size of cond like matrices				 */
int				*origChar;			         /* index from compressed char to original char  */
int				*stateSize;			         /* # states for each compressed char			 */
int				*stdType;				     /* compressed std char type: ord, unord, irrev  */
int				*tiIndex;				     /* compressed std char ti index                 */
int				*bsIndex;				     /* compressed std stat freq index               */
int				*weight;			         /* weight of each compressed char				 */
int				*chainTempId;                /* info on temp, change to float holding temp?  */
int				state[MAX_CHAINS];           /* state of chain								 */
int				augmentData;		         /* are data being augmented for any division?	 */
int				*nAccepted;			         /* counter of accepted moves					 */
CLFlt			**chainCLPtrSpace;           /* space holding pointers to cond likes         */
CLFlt			***condLikePtr;		         /* pointers to cond likes for chain and node	 */
long			**parsPtrSpace = NULL;				 /* space holding pointers to parsimony sets     */
long			***parsPtr = NULL;					 /* pointers to pars state sets for chain & node */
CLFlt			*parsNodeLengthSpace = NULL;		 /* space for parsimony node lengths			 */
CLFlt			**parsNodeLen = NULL;				 /* pointers to pars node lengths for chains     */
char			*printString;                /* string for printing to a file                */
size_t			printStringSize;             /* length of printString                        */
long int		*sprParsMatrix;		         /* SPR parsimony (bitset) matrix for terminals  */
long int		*sprParsSets;                /* SPR parsimony (bitset) matrix for all nodes  */
long			**sprParsPtrSpace;           /* space holding pointers to SPR parsimony sets */
long			***sprParsPtr;				 /* ptrs to SPR pars state sets for chain & node */
int				sprParsMatrixRowSize;	     /* row size of SPR parsimony matrix             */
CLFlt			*treeScalerSpace;            /* space holding tree scalers					 */
CLFlt			**treeScaler;		         /* pointers to tree scalers for each chain		 */
CLFlt			*nodeScalerSpace;	         /* space holding cond like node scalers         */
CLFlt			**nodeScaler;		         /* pointers to cond like scalers for each chain */
CLFlt			*tiProbSpace;		         /* space holding tiProbs						 */
CLFlt			**tiProbs;			         /* pointers to tiProbs for each chain			 */
int				cijkRowSize;		         /* row size of cijk information                 */
MrBFlt			*cijkSpace;			         /* space holding cijk information               */
MrBFlt			**cijks;			         /* pointers to cijk for each chain              */
CLFlt			*preLikeL;			         /* precalculated cond likes for left descendant */
CLFlt			*preLikeR;			         /* precalculated cond likes for right descendant*/
CLFlt			*preLikeA;			         /* precalculated cond likes for ancestor        */
MrBFlt			*invCondLikes = NULL;		         /* cond likes for invariable sites  		     */
MCMCMove		*moves;				         /* vector of moves							 	 */
int				numCalibratedLocalTaxa;	     /* the number of dated local terminal taxa  	 */
int				numMoves;			         /* the number of moves used by chain			 */
int				numMoveTypes;		         /* the number of move types                     */
MoveType		moveTypes[NUM_MOVE_TYPES];   /* holds information on the move types          */
int				codon[6][64];                /* holds info on amino acids coded in code      */
MrBFlt			aaJones[20][20];	         /* rates for Jones model                        */
MrBFlt			aaDayhoff[20][20];           /* rates for Dayhoff model                      */
MrBFlt			aaMtrev24[20][20];	         /* rates for mtrev24 model                      */
MrBFlt			aaMtmam[20][20];	         /* rates for mtmam model                        */
MrBFlt			aartREV[20][20];             /* rates for rtREV model                        */
MrBFlt			aaWAG[20][20];               /* rates for WAG model                          */
MrBFlt			aacpREV[20][20];             /* rates for aacpREV model                      */
MrBFlt			aaVt[20][20];                /* rates for VT model                           */
MrBFlt			aaBlosum[20][20];            /* rates for Blosum62 model                     */
MrBFlt			jonesPi[20];                 /* stationary frequencies for Jones model       */
MrBFlt			dayhoffPi[20];               /* stationary frequencies for Dayhoff model     */
MrBFlt			mtrev24Pi[20];               /* stationary frequencies for mtrev24 model     */
MrBFlt			mtmamPi[20];                 /* stationary frequencies for mtmam model       */
MrBFlt			rtrevPi[20];                 /* stationary frequencies for rtREV model       */
MrBFlt			wagPi[20];                   /* stationary frequencies for WAG model         */
MrBFlt			cprevPi[20];                 /* stationary frequencies for aacpREV model     */
MrBFlt			vtPi[20];                    /* stationary frequencies for VT model          */
MrBFlt			blosPi[20];                  /* stationary frequencies for Blosum62 model    */
int				chainHasAdgamma;			 /* indicates if chain has adgamma HMMs			 */
int				inferPosSel;			 	 /* indicates if positive selection is inferred  */
MrBFlt			*posSelProbs;                /* probs. for positive selection                */
int				hasMarkovTi[MAX_SMALL_JUMP]; /* vector marking size of observed HMM jumps    */
int				*siteJump;					 /* vector of sitejumps for adgamma model        */
int				rateProbRowSize;			 /* size of rate probs for one chain one state   */
MrBFlt			*rateProbSpace;				 /* space for rate probs used by adgamma model   */
MrBFlt			**rateProbs;				 /* pointers to rate probs used by adgamma model */
MrBFlt			**markovTi[MAX_SMALL_JUMP];  /* trans prob matrices used in calc of adgamma  */
MrBFlt			**markovTiN;				 /* trans prob matrices used in calc of adgamma  */
int				whichReweightNum;            /* used for setting reweighting of char pats    */
int				***swapInfo;                 /* keeps track of attempts & successes of swaps */
int				tempIndex;                   /* keeps track of which user temp is specified  */
CLFlt			*ancStateCondLikes;          /* used for final cond likes of int nodes       */
/*MrBFlt*/int			abortMove;					 /* flag determining whether to abort move       */
PFNODE			**partFreqTreeRoot;			 /* root of tree(s) holding partition freqs      */
int				nLongsNeeded;				 /* number of longs needed for partitions        */
long			**partition;                 /* matrix holding partitions                    */
MrBFlt          *maxLnL0 = NULL;                    /* maximum likelihood                           */
FILE			*fpMcmc = NULL;              /* pointer to .mcmc file                        */
FILE			**fpParm = NULL;             /* pointer to .p file(s)                        */
FILE			***fpTree = NULL;            /* pointer to .t file(s)                        */
FILE			***fpCal = NULL;             /* pointer to .cal file(s)                      */
static int 	confirmAbortRun;                 /* flag for aborting mcmc analysis              */

#if defined (MPI_ENABLED)
int				lowestLocalRunId;			 /* lowest local run Id                          */
int				highestLocalRunId;			 /* highest local run Id                         */
#endif


/*-----------------------------------------------------------------------
|
|	AddDummyChars: Add dummy characters to relevant partitions
|
------------------------------------------------------------------------*/
int AddDummyChars (void)

{

	int			i, j, k, d, numIncompatible, numDeleted, numStdChars, oldRowSize,
				newRowSize, numDummyChars, newColumn, newChar, oldColumn, oldChar, 
				isCompat, *tempChar, numIncompatibleChars;
	long		*tempMatrix;
	CLFlt		*tempSitesOfPat;
	ModelInfo	*m;
	ModelParams	*mp;
	CharInfo	cinfo;
	Matrix		matrix;

	extern int	NBits(int x);

	/* set pointers to NULL */
	tempMatrix = NULL;
	tempSitesOfPat = NULL;
	tempChar = NULL;

	/* check how many dummy characters needed in total */
	numDummyChars = 0;
	numStdChars = 0;
	for (d=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];
		mp = &modelParams[d];

		m->numDummyChars = 0;

		if (mp->dataType == RESTRICTION && !strcmp(mp->parsModel,"No"))
			{
			if (!strcmp(mp->coding, "Variable"))
				m->numDummyChars = 2;
			else if (!strcmp(mp->coding, "Noabsencesites") || !strcmp(mp->coding, "Nopresencesites"))
				m->numDummyChars = 1;
			else if (!strcmp(mp->coding, "Informative"))
				m->numDummyChars = 2 + 2 * numLocalTaxa;
			}

		if (mp->dataType == STANDARD && !strcmp(mp->parsModel,"No"))
			{
			if (!strcmp(mp->coding, "Variable"))
				m->numDummyChars = 2;
			else if (!strcmp(mp->coding, "Informative"))
				m->numDummyChars = 2 + 2 * numLocalTaxa;
			numStdChars += (m->numChars + m->numDummyChars);
			}

		numDummyChars += m->numDummyChars;
		m->numChars += m->numDummyChars;

		}

	/* exit if dummy characters not needed */
	if (numDummyChars == 0)
		return NO_ERROR;

	/* print original compressed matrix */
#	if	0
	MrBayesPrint ("Compressed matrix before adding dummy characters...\n");
	PrintCompMatrix();
#	endif		

	/* set row sizes for old and new matrices */
	oldRowSize = compMatrixRowSize;
	compMatrixRowSize += numDummyChars;
	newRowSize = compMatrixRowSize;
	numCompressedChars += numDummyChars;

	/* allocate space for new data */
	tempMatrix = (long *) calloc (numLocalTaxa * newRowSize, sizeof(long));
	tempSitesOfPat = (CLFlt *) calloc (numCompressedChars, sizeof(CLFlt));
	tempChar = (int *) calloc (compMatrixRowSize, sizeof(int));
	if (!tempMatrix || !tempSitesOfPat || !tempChar)
		{
		MrBayesPrint ("%s   Problem allocating temporary variables in AddDummyChars\n", spacer);
		goto errorExit;
		}

	/* initialize indices */
	oldChar = newChar = newColumn = numDeleted = 0;

	/* set up matrix struct */
	matrix.origin = compMatrix;
	matrix.nRows = numLocalTaxa;
	matrix.rowSize = oldRowSize;

	/* loop over divisions */
	for (d=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];
		mp = &modelParams[d];

		/* insert the dummy characters first for each division */
		if (m->numDummyChars > 0)
			{
			MrBayesPrint("%s   Adding dummy characters (unobserved site patterns) for division %d\n", spacer, d+1);

			if (!strcmp(mp->coding, "Variable") || !strcmp(mp->coding, "Informative"))
				{
				for (k=0; k<2; k++)
					{
					for (i=0; i<numLocalTaxa; i++)
						tempMatrix[pos(i,newColumn,newRowSize)] = (1<<k);
					tempSitesOfPat[newChar] = 0;
					tempChar[newColumn] = -1;
					newChar++;
					newColumn++;
					}
				}

			if (!strcmp(mp->coding, "Informative"))
				{
				for (k=0; k<2; k++)
					{
					for (i=0; i< numLocalTaxa; i++)
						{
						for (j=0; j<numLocalTaxa; j++)
							{
							if(j == i)
								tempMatrix[pos(j,newColumn,newRowSize)] = (1 << k) ^ 3;
							else
								tempMatrix[pos(j,newColumn,newRowSize)] = 1 << k;
							}
						tempSitesOfPat[newChar] = 0;
						tempChar[newColumn] = -1;
						newChar++;
						newColumn++;
						}
					}
				}

			if (!strcmp(mp->coding, "Noabsencesites"))
				{
				for (i=0; i<numLocalTaxa; i++)
					tempMatrix[pos(i,newColumn,newRowSize)] = 1;
				tempSitesOfPat[newChar] = 0;
				tempChar[newColumn] = -1;
				newChar++;
				newColumn++;
				}

			if (!strcmp(mp->coding, "Nopresencesites"))
				{
				for (i=0; i<numLocalTaxa; i++)
					tempMatrix[pos(i,newColumn,newRowSize)] = 2;
				tempSitesOfPat[newChar] = 0;
				tempChar[newColumn] = -1;
				newChar++;
				newColumn++;
				}
			}

		/* add the normal characters */
		numIncompatible = numIncompatibleChars = 0;
		for (oldColumn=m->compMatrixStart; oldColumn<m->compMatrixStop; oldColumn++)
			{
			isCompat = YES;
			/* first check if the character is supposed to be present */
			if (m->numDummyChars > 0)
				{
				/* set up matrix struct */
				matrix.column = oldColumn;
				/* set up charinfo struct */
				cinfo.dType = mp->dataType;
				cinfo.cType = charInfo[origChar[oldChar]].ctype;
				cinfo.nStates = charInfo[origChar[oldChar]].numStates;
				CheckCharCodingType(&matrix, &cinfo);

				if (!strcmp(mp->coding, "Variable") && cinfo.variable == NO)
					isCompat = NO;
				else if (!strcmp(mp->coding, "Informative") && cinfo.informative == NO)
					isCompat = NO;
				else if (!strcmp(mp->coding, "Noabsencesites") && cinfo.constant[0] == YES)
					isCompat = NO;
				else if (!strcmp(mp->coding, "Nopresencesites") && cinfo.constant[1] == YES)
					isCompat = NO;
				}

			if (isCompat == NO)
				{
				numIncompatible++;
				numIncompatibleChars += (int) numSitesOfPat[oldChar];
				oldChar++;
				}
			else
				{
				/* add character */
				for (i=0; i<numLocalTaxa; i++)
					tempMatrix[pos(i,newColumn,newRowSize)] = compMatrix[pos(i,oldColumn,oldRowSize)];
				/* set indices */
				compCharPos[origChar[oldColumn]] = newChar;
				compColPos[origChar[oldColumn]] = newColumn;
				tempSitesOfPat[newChar] = numSitesOfPat[oldChar];
				tempChar[newColumn] = origChar[oldColumn];
				newColumn++;
				if ((oldColumn-m->compMatrixStart+1) % m->nCharsPerSite == 0)
					{
					newChar++;
					oldChar++;
					}
				}
			}

		/* print a warning if there are incompatible characters */
		if (numIncompatible > 0)
			{
			m->numChars -= numIncompatible;
			m->numUncompressedChars -= numIncompatibleChars;
			numDeleted += numIncompatible;
			if (numIncompatibleChars > 1)
				{
				MrBayesPrint ("%s   WARNING: There are %d characters incompatible with the specified\n", spacer, numIncompatibleChars);
				MrBayesPrint ("%s            coding bias. These characters will be excluded.\n", spacer);
				}
			else
				{
				MrBayesPrint ("%s   WARNING: There is one character incompatible with the specified\n", spacer);
				MrBayesPrint ("%s            coding bias. This character will be excluded.\n", spacer);
				}
			}

		/* update division comp matrix and comp char pointers */
		m->compCharStop = newChar;
		m->compMatrixStop = newColumn;
		m->compCharStart = newChar - m->numChars;
		m->compMatrixStart = newColumn - m->nCharsPerSite * m->numChars;

		}	/* next division */

	/* compress matrix if necessary */
	if (numDeleted > 0)
		{
		for (i=k=0; i<numLocalTaxa; i++)
			{
			for (j=0; j<newRowSize-numDeleted; j++)
				{
				tempMatrix[k++] = tempMatrix[j+i*newRowSize];
				}
			}
		numCompressedChars -= numDeleted;
		compMatrixRowSize -= numDeleted;
		}

	/* free old data, set pointers to new data */
	free (compMatrix);
	free (numSitesOfPat);
	free (origChar);
	
	compMatrix = tempMatrix;
	numSitesOfPat = tempSitesOfPat;
	origChar = tempChar;
	
	tempMatrix = NULL;
	tempSitesOfPat = NULL;
	tempChar = NULL;
	
	/* print new compressed matrix */
#	if	defined (DEBUG_ADDDUMMYCHARS)
	MrBayesPrint ("After adding dummy characters...\n");
	PrintCompMatrix();
#	endif		

	return NO_ERROR;

	errorExit:
		if (tempMatrix)
			free (tempMatrix);
		if (tempSitesOfPat)
			free (tempSitesOfPat);
		if (tempChar)
			free (tempChar);

		return ERROR;
	
}





/* AddPartition: Add a partition to the tree keeping track of partition frequencies */
PFNODE *AddPartition (PFNODE *r, long *p, int runId)
{
	int		i, comp;
	
	if (r == NULL)
		{
		/* new partition */
		r = Talloc ();					/* create a new node */
		if (r == NULL)
			return NULL;
		for (i=0; i<nLongsNeeded; i++)
			r->partition[i] = p[i];
		for (i=0; i<chainParams.numRuns; i++)
			r->count[i] = 0;
		r->count[runId] = 1;
		r->left = r->right = NULL;
		}
	else
		{
		for (i=0; i<nLongsNeeded; i++)
			{
			if (r->partition[i] != p[i])
				break;
			}
		
		if (i == nLongsNeeded)
			comp = 0;
		else if (r->partition[i] < p[i])
			comp = -1;
		else
			comp = 1;
		
		if (comp == 0)			/* repeated partition */
			r->count[runId]++;
		else if (comp < 0)		/* greater than -> into left subtree */
			{
			if ((r->left = AddPartition (r->left, p, runId)) == NULL)
				{
				Tfree (r);
				return NULL;
				}
			}
		else
			{
			/* smaller than -> into right subtree */
			if ((r->right = AddPartition (r->right, p, runId)) == NULL)
				{
				Tfree (r);
				return NULL;
				}
			}
		}

	return r;
}





int AddToPrintString (char *tempStr)

{

	size_t			len1, len2;
	
	len1 = (int) strlen(printString);
	len2 = (int) strlen(tempStr);
	if (len1 + len2 + 5 > printStringSize)
		{
		printStringSize += len1 + len2 - printStringSize + 200;
		printString = realloc((void *)printString, printStringSize * sizeof(char));
		if (!printString)
			{
			MrBayesPrint ("%s   Problem reallocating printString (%d)\n", spacer, printStringSize * sizeof(char));
			goto errorExit;
			}
		}
	strcat(printString, tempStr);	
#	if 0
	printf ("printString(%d) -> \"%s\"\n", printStringSize, printString);
#	endif	
	return (NO_ERROR);
	
	errorExit:
		return (ERROR);

}





/* AddTreeSamples: Add tree samples to partition counters */
int AddTreeSamples (int from, int to)
{
	int	i, j, k, longestLine;
	long	lastBlock;
	char	*word, *s, *lineBuf;
	FILE	*fp;
	Tree	*t;
	char	temp[100];

#	if defined (MPI_ENABLED)
	if (proc_id != 0)
		return (NO_ERROR);
#	endif

	for (i=0; i<numTrees; i++)
		{
		if (GetTreeFromIndex(i, 0, 0)->isRooted == YES)
			t = chainParams.rtree;
		else
			t = chainParams.utree;

		for (j=0; j<chainParams.numRuns; j++)
			{
			if (numTrees == 1)
				sprintf (temp, "%s.run%d.t", chainParams.chainFileName, j+1);
			else
				sprintf (temp, "%s.tree%d.run%d.t", chainParams.chainFileName, i+1, j+1);

			if ((fp = OpenBinaryFileR (temp)) == NULL)
				return (ERROR);
			longestLine = LongestLine (fp);
			fclose (fp);

			if ((fp = OpenTextFileR (temp)) == NULL)
				return (ERROR);
			
			lineBuf = (char *) calloc (longestLine + 10, sizeof (char));
			if (!lineBuf)
				{
				fclose (fp);
				return (ERROR);
				}

			lastBlock = LastBlock (fp, lineBuf, longestLine);
			fseek (fp, lastBlock, SEEK_SET);

			for (k=1; k<=to; k++)
				{
				do {
					if (fgets (lineBuf, longestLine, fp) == NULL)
						return ERROR;
					word = strtok (lineBuf, " ");
					} while (strcmp (word, "tree") != 0);
				if (k>=from)
					{
					s = strtok (NULL, ";");
					while (*s != '(')
						s++;
					if (RecreateTree (t, s) == ERROR)
						{
						fclose (fp);
						free (lineBuf);
						return ERROR;
						}
					if (AddTreeToPartitionCounters (t, i, j) == ERROR)
						{
						fclose (fp);
						free (lineBuf);
						return ERROR;
						}
					}
				}
			fclose (fp);
			free (lineBuf);
			} /* next run */
		} /* next tree */
	return (NO_ERROR);
}





/* AddTreeToPartitionCounters: Break a tree into partitions and add those to counters */
int AddTreeToPartitionCounters (Tree *tree, int treeId, int runId)
{
	int			i, j;
	TreeNode	*p;

	for (i=0; i<tree->nIntNodes-1; i++)
		{
		p = tree->intDownPass[i];
		for (j=0; j<nLongsNeeded; j++)
			{
			partition[p->index][j] = partition[p->left->index][j] | partition[p->right->index][j];
			}

		if ((partFreqTreeRoot[treeId] = AddPartition (partFreqTreeRoot[treeId], partition[p->index], runId)) == NULL)
			{
			MrBayesPrint ("%s   Could not allocate space for new partition in AddTreeToPartitionCounters\n", spacer);
			return ERROR;
			}
		}

	return NO_ERROR;
}






/* AllocateTree: Allocate memory space for a tree (unrooted or rooted) */
Tree *AllocateTree (int numTaxa, int isTreeRooted)
{
	Tree	*t;
	
	t = (Tree *) calloc (1, sizeof (Tree));
	if (t == NULL)
		return NULL;

	t->isRooted = isTreeRooted;
	if (isTreeRooted == NO)
		{
		t->nIntNodes = numTaxa - 2;
		t->nNodes = numTaxa + t->nIntNodes;
		}
	else
		{
		t->nIntNodes = numTaxa - 1;
		t->nNodes = numTaxa + t->nIntNodes + 1;	/* add one for the root node */
		}

	if ((t->nodes = (TreeNode *) calloc (t->nNodes, sizeof (TreeNode))) == NULL)
		{
		free (t);
		return NULL;
		}

	if ((t->allDownPass = (TreeNode **) calloc (t->nNodes + t->nIntNodes, sizeof (TreeNode *))) == NULL)
		{
		free (t->nodes);
		free (t);
		return NULL;
		}
	t->intDownPass = t->allDownPass + t->nNodes;

	return t;
}





int AttemptSwap (int swapA, int swapB, long int *seed)

{

	int				d, tempX, reweightingChars, isSwapSuccessful, chI, chJ, runId;
	MrBFlt			tempA, tempB, lnLikeA, lnLikeB, lnPriorA, lnPriorB, lnR, r,
					lnLikeStateAonDataB=0.0, lnLikeStateBonDataA=0.0, lnL;
	ModelInfo		*m;
	Tree			*tree;
#					if defined (MPI_ENABLED)
	int				numChainsForProc, tempIdA=0, tempIdB=0, proc, procIdForA=0, procIdForB=0, 
					whichElementA=0, whichElementB=0, lower, upper, areWeA, doISwap, ierror,
					myId, partnerId;
	MrBFlt			swapRan;
	MPI_Status 		status[2];
	MPI_Request		request[2];
#					endif
	
#	if defined (MPI_ENABLED)
	/* get the number of chains handled by this proc */
	/* the number will be corrected further down for unbalanced scenarios */
	numChainsForProc = (int) (chainParams.numChains * chainParams.numRuns / num_procs);

#	endif

	/* are we using character reweighting? */
	reweightingChars = NO;
	if ((chainParams.weightScheme[0] + chainParams.weightScheme[1]) > 0.00001)
		reweightingChars = YES;
			
#	if defined (MPI_ENABLED)

	/* figure out processors involved in swap */
	lower = upper = 0;
	for (proc=0; proc<num_procs; proc++)
		{
		/* assign or increment chain id */
		if (proc < (chainParams.numChains * chainParams.numRuns) % num_procs)
			upper += numChainsForProc+1;
		else
			upper += numChainsForProc;

		/* if swapA lies between lower and upper
			* chain id's we know that this is the proc
			* swapA is in */
		if (swapA >= lower && swapA < upper)
			{
			procIdForA = proc;
			whichElementA = swapA - lower;
			}
		if (swapB >= lower && swapB < upper)
			{
			procIdForB = proc;
			whichElementB = swapB - lower;
			}
		lower = upper;
		}

	/* NOTE: at this point, procIdForA and procIdForB *
		* store the proc id's of swapping procs. Also,   *
		* whichElementA and whichElementB store the      *
		* chainId[] index of swapping procs              */

	/* figure out if I am involved in the swap */
	doISwap = areWeA = NO;
	if (proc_id == procIdForA)
		{
		doISwap = YES;
		areWeA = YES;
		}
	else if (proc_id == procIdForB)
		{
		doISwap = YES;
		}

	/* chain's that do not swap, continue to the next iteration */	
	if (doISwap == YES)
		{
		
		/* no need to communicate accross processors if swapping chains are in the same proc */
		if (procIdForA == procIdForB)
			{
			if (reweightingChars == YES)
				{
				/* use character reweighting */
				lnLikeStateAonDataB = 0.0;
				for (d=0; d<numCurrentDivisions; d++)
					{
					m = &modelSettings[d];
					tree = GetTree(m->brlens, whichElementA, state[whichElementA]);
					lnL = 0.0;
					m->Likelihood (tree->root->left, d, whichElementA, &lnL, chainId[whichElementB] % chainParams.numChains);
					lnLikeStateAonDataB += lnL;
					}
				lnLikeStateBonDataA = 0.0;
				for (d=0; d<numCurrentDivisions; d++)
					{
					m = &modelSettings[d];
					tree = GetTree(m->brlens, whichElementB, state[whichElementB]);
					lnL = 0.0;
					m->Likelihood (tree->root->left, d, whichElementB, &lnL, chainId[whichElementA] % chainParams.numChains);
					lnLikeStateBonDataA += lnL;
					}
				}

			/*curLnPr[whichElementA] = LogPrior(whichElementA);
			curLnPr[whichElementB] = LogPrior(whichElementB);*/

			/* then do the serial thing - simply swap chain id's */
			tempA = Temperature (chainId[whichElementA]);
			tempB = Temperature (chainId[whichElementB]);
			lnLikeA = curLnL[whichElementA];
			lnLikeB = curLnL[whichElementB];
			lnPriorA = curLnPr[whichElementA];
			lnPriorB = curLnPr[whichElementB];
			if (reweightingChars == YES)
				lnR = (tempB * (lnLikeStateAonDataB + lnPriorA) + tempA * (lnLikeStateBonDataA + lnPriorB)) - (tempA * (lnLikeA + lnPriorA) + tempB * (lnLikeB + lnPriorB));
			else
				lnR = (tempB * (lnLikeA + lnPriorA) + tempA * (lnLikeB + lnPriorB)) - (tempA * (lnLikeA + lnPriorA) + tempB * (lnLikeB + lnPriorB));
			if (lnR <  -100.0)
				r =  0.0;
			else if (lnR > 0.0)
				r =  1.0;
			else
				r =  exp(lnR);

			isSwapSuccessful = NO;
			if (RandomNumber(seed) < r)
				{
				/* swap chain id's (heats) */
				tempX = chainId[whichElementA];
				chainId[whichElementA] = chainId[whichElementB];
				chainId[whichElementB] = tempX;
				if (reweightingChars == YES)
					{
					curLnL[whichElementA] = lnLikeStateAonDataB;
					curLnL[whichElementB] = lnLikeStateBonDataA;
					}
				isSwapSuccessful = YES;
				}
				
			chI = chainId[whichElementA];
			chJ = chainId[whichElementB];
			if (chainId[whichElementB] < chainId[whichElementA])
				{
				chI = chainId[whichElementB];
				chJ = chainId[whichElementA];
				}
			runId = chI / chainParams.numChains;
			chI = chI % chainParams.numChains;
			chJ = chJ % chainParams.numChains;
			swapInfo[runId][chJ][chI]++;
			if (isSwapSuccessful == YES)
				swapInfo[runId][chI][chJ]++;
			}
		/* we need to communicate across processors */
		else
			{
			if (reweightingChars == YES)
				{
				/* If we are reweighting characters, then we need to do an additional communication to
					figure out the chainId's of the partner. We need to have this information so we can
					properly calculate likelihoods with switched observations. */
				if (areWeA == YES)
					{
					lnLikeStateAonDataB = 0.0;
					myId = chainId[whichElementA];
					ierror = MPI_Isend (&myId, 1, MPI_INT, procIdForB, 0, MPI_COMM_WORLD, &request[0]);
					if (ierror != MPI_SUCCESS)
						{
						return (ERROR);
						}
					ierror = MPI_Irecv (&partnerId, 1, MPI_INT, procIdForB, 0, MPI_COMM_WORLD, &request[1]);
					if (ierror != MPI_SUCCESS)
						{
						return (ERROR);
						}
					ierror = MPI_Waitall (2, request, status);
					if (ierror != MPI_SUCCESS)
						{
						return (ERROR);
						}
					for (d=0; d<numCurrentDivisions; d++)
						{
						m = &modelSettings[d];
						tree = GetTree(m->brlens, whichElementA, state[whichElementA]);
						lnL = 0.0;
						m->Likelihood (tree->root->left, d, whichElementA, &lnL, partnerId);
						lnLikeStateAonDataB = lnL;
						}
					}
				else
					{
					lnLikeStateBonDataA = 0.0;
					myId = chainId[whichElementB];
					ierror = MPI_Isend (&myId, 1, MPI_INT, procIdForA, 0, MPI_COMM_WORLD, &request[0]);
					if (ierror != MPI_SUCCESS)
						{
						return (ERROR);
						}
					ierror = MPI_Irecv (&partnerId, 1, MPI_INT, procIdForA, 0, MPI_COMM_WORLD, &request[1]);
					if (ierror != MPI_SUCCESS)
						{
						return (ERROR);
						}
					ierror = MPI_Waitall (2, request, status);
					if (ierror != MPI_SUCCESS)
						{
						return (ERROR);
						}
					for (d=0; d<numCurrentDivisions; d++)
						{
						m = &modelSettings[d];
						tree = GetTree(m->brlens, whichElementB, state[whichElementB]);
						lnL = 0.0;
						m->Likelihood (tree->root->left, d, whichElementB, &lnL, partnerId);
						lnLikeStateBonDataA = lnL;
						}
					}
				}
			if (areWeA == YES)
				{
				/*curLnPr[whichElementA] = LogPrior(whichElementA);*/

				/* we are processor A */
				tempIdA = chainId[whichElementA];
				lnLikeA = curLnL[whichElementA];
				lnPriorA = curLnPr[whichElementA];
				swapRan = RandomNumber(seed);

				myStateInfo[0] = lnLikeA;
				myStateInfo[1] = lnPriorA;
				myStateInfo[2] = tempIdA;
				myStateInfo[3] = swapRan;
				if (reweightingChars == YES)
					{
					myStateInfo[2] = lnLikeStateAonDataB;
					tempIdB = partnerId;
					}
					
				ierror = MPI_Isend (&myStateInfo, 4, MPI_DOUBLE, procIdForB, 0, MPI_COMM_WORLD, &request[0]);
				if (ierror != MPI_SUCCESS)
					{
					return (ERROR);
					}
				ierror = MPI_Irecv (&partnerStateInfo, 4, MPI_DOUBLE, procIdForB, 0, MPI_COMM_WORLD, &request[1]);
				if (ierror != MPI_SUCCESS)
					{
					return (ERROR);
					}
				ierror = MPI_Waitall (2, request, status);
				if (ierror != MPI_SUCCESS)
					{
					return (ERROR);
					}

				lnLikeA = curLnL[whichElementA];
				lnLikeB = partnerStateInfo[0];
				lnPriorA = curLnPr[whichElementA];
				lnPriorB = partnerStateInfo[1];
				if (reweightingChars == YES)
					lnLikeStateBonDataA = partnerStateInfo[2];
				else
					tempIdB = partnerStateInfo[2];
				
				tempA = Temperature (tempIdA);
				tempB = Temperature (tempIdB);

				if (reweightingChars == YES)
					lnR = (tempB * (lnLikeStateAonDataB + lnPriorA) + tempA * (lnLikeStateBonDataA + lnPriorB)) - (tempA * (lnLikeA + lnPriorA) + tempB * (lnLikeB + lnPriorB));
				else
					lnR = (tempB * (lnLikeA + lnPriorA) + tempA * (lnLikeB + lnPriorB)) - (tempA * (lnLikeA + lnPriorA) + tempB * (lnLikeB + lnPriorB));
				if (lnR < -100.0)
					r = 0.0;
				else if (lnR > 0.0)
					r = 1.0;
				else
					r = exp(lnR);

				/* process A's random number is used to make the swap decision */
				isSwapSuccessful = NO;
				if (swapRan < r)
					{
					/* swap chain id's (heats) */
					if (reweightingChars == YES)
						chainId[whichElementA] = tempIdB;
					else
						chainId[whichElementA] = (int)(partnerStateInfo[2]);
					if (reweightingChars == YES)
						{
						curLnL[whichElementA] = lnLikeStateAonDataB;
						}
					isSwapSuccessful = YES;
					}
					
				/* only processor A keeps track of the swap success/failure */
				chI = tempIdA;
				chJ = tempIdB;
				if (tempIdB < tempIdA)
					{
					chI = tempIdB;
					chJ = tempIdA;
					}
				runId = chI / chainParams.numChains;
				chI = chI % chainParams.numChains;
				chJ = chJ % chainParams.numChains;
				swapInfo[runId][chJ][chI]++;
				if (isSwapSuccessful == YES)
					swapInfo[runId][chI][chJ]++;
					
				}
			else
				{
				/*curLnPr[whichElementB] = LogPrior(whichElementB);*/

				/* we are processor B */
				tempIdB  = chainId[whichElementB];
				lnLikeB  = curLnL[whichElementB];
				lnPriorB = curLnPr[whichElementB];
				swapRan  = -1.0;

				myStateInfo[0] = lnLikeB;
				myStateInfo[1] = lnPriorB;
				myStateInfo[2] = tempIdB;
				myStateInfo[3] = swapRan;
				if (reweightingChars == YES)
					{
					myStateInfo[2] = lnLikeStateBonDataA;
					tempIdA = partnerId;
					}

				ierror = MPI_Isend (&myStateInfo, 4, MPI_DOUBLE, procIdForA, 0, MPI_COMM_WORLD, &request[0]);
				if (ierror != MPI_SUCCESS)
					{
					return (ERROR);
					}
				ierror = MPI_Irecv (&partnerStateInfo, 4, MPI_DOUBLE, procIdForA, 0, MPI_COMM_WORLD, &request[1]);
				if (ierror != MPI_SUCCESS)
					{
					return (ERROR);
					}
				ierror = MPI_Waitall (2, request, status);
				if (ierror != MPI_SUCCESS)
					{
					return (ERROR);
					}

				lnLikeB = curLnL[whichElementB];
				lnLikeA = partnerStateInfo[0];
				lnPriorB = curLnPr[whichElementB];
				lnPriorA = partnerStateInfo[1];
				if (reweightingChars == YES)
					lnLikeStateAonDataB = partnerStateInfo[2];
				else
					tempIdA = partnerStateInfo[2];

				tempB = Temperature (tempIdB);
				tempA = Temperature (tempIdA);

				if (reweightingChars == YES)
					lnR = (tempB * (lnLikeStateAonDataB + lnPriorA) + tempA * (lnLikeStateBonDataA + lnPriorB)) - (tempA * (lnLikeA + lnPriorA) + tempB * (lnLikeB + lnPriorB));
				else
					lnR = (tempB * (lnLikeA + lnPriorA) + tempA * (lnLikeB + lnPriorB)) - (tempA * (lnLikeA + lnPriorA) + tempB * (lnLikeB + lnPriorB));
				if (lnR < -100.0)
					r = 0.0;
				else if (lnR > 0.0)
					r = 1.0;
				else
					r = exp(lnR);

				/* we use process A's random number to make the swap decision */
				if (partnerStateInfo[3] < r)
					{
					if (reweightingChars == YES)
						chainId[whichElementB] = tempIdA;
					else
						chainId[whichElementB] = (int)(partnerStateInfo[2]);
					if (reweightingChars == YES)
						{
						curLnL[whichElementB] = lnLikeStateBonDataA;
						}
					}

				}
			}
		}
#	else
	if (reweightingChars == YES)
		{
		/* use character reweighting */
		lnLikeStateAonDataB = 0.0;
		for (d=0; d<numCurrentDivisions; d++)
			{
			m = &modelSettings[d];
			tree = GetTree(m->brlens, swapA, state[swapA]);
			lnL = 0.0;
			m->Likelihood (tree->root->left, d, swapA, &lnL, chainId[swapB] % chainParams.numChains);
			lnLikeStateAonDataB += lnL;
			}
		lnLikeStateBonDataA = 0.0;
		for (d=0; d<numCurrentDivisions; d++)
			{
			m = &modelSettings[d];
			tree = GetTree(m->brlens, swapB, state[swapB]);
			lnL = 0.0;
			m->Likelihood (tree->root->left, d, swapB, &lnL, chainId[swapA] % chainParams.numChains);
			lnLikeStateBonDataA += lnL;
			}
		}
	/*curLnPr[swapA] = LogPrior(swapA);
	curLnPr[swapB] = LogPrior(swapB);*/
	tempA = Temperature (chainId[swapA]);
	tempB = Temperature (chainId[swapB]);
	lnLikeA = curLnL[swapA];
	lnLikeB = curLnL[swapB];
	lnPriorA = curLnPr[swapA];
	lnPriorB = curLnPr[swapB];

	if (reweightingChars == YES)
		lnR = (tempB * (lnLikeStateAonDataB + lnPriorA) + tempA * (lnLikeStateBonDataA + lnPriorB)) - (tempA * (lnLikeA + lnPriorA) + tempB * (lnLikeB + lnPriorB));
	else
		lnR = (tempB * (lnLikeA + lnPriorA) + tempA * (lnLikeB + lnPriorB)) - (tempA * (lnLikeA + lnPriorA) + tempB * (lnLikeB + lnPriorB));
	if (lnR < -100.0)
		r = 0.0;
	else if (lnR > 0.0)
		r =  1.0;
	else
		r =  exp (lnR);

	isSwapSuccessful = NO;
	if (RandomNumber(seed) < r)
		{
		tempX = chainId[swapA];
		chainId[swapA] = chainId[swapB];
		chainId[swapB] = tempX;

		if (reweightingChars == YES)
			{
			curLnL[swapA] = lnLikeStateAonDataB;
			curLnL[swapB] = lnLikeStateBonDataA;
			}
		isSwapSuccessful = YES;
		}
		
	chI = chainId[swapA];
	chJ = chainId[swapB];
	if (chainId[swapB] < chainId[swapA])
		{
		chI = chainId[swapB];
		chJ = chainId[swapA];
		}
	runId = chI / chainParams.numChains;
	chI = chI % chainParams.numChains;
	chJ = chJ % chainParams.numChains;
	swapInfo[runId][chJ][chI]++;
	if (isSwapSuccessful == YES)
		swapInfo[runId][chI][chJ]++;
#	endif
	
	return (NO_ERROR);
	
}




/*----------------------------------------------------------------
|
|	Bit: return 1 if bit n is set in long *p
|		else return 0
|
-----------------------------------------------------------------*/
int Bit (int n, long *p)

{

	long		x;

	p += n / nBitsInALong;
	x = 1 << (n % nBitsInALong);

	if ((x & (*p)) == 0)
		return 0;
	else
		return 1;

}





/*----------------------------------------------------------------
|
|	BuildConstraintTree: Build constraint tree
|
----------------------------------------------------------------*/
int BuildConstraintTree (Tree *t, PolyTree *pt)

{

	int				i, j, k, k1, nLongsNeeded, nextNode;
	long int		*constraintPartition, *mask;
	PolyNode		*pp, *qq, *rr, *ss, *tt;
	   	
	nLongsNeeded = (numLocalTaxa / nBitsInALong) + 1;
	constraintPartition = (long int *) calloc (2*nLongsNeeded, sizeof(long int));
	if (!constraintPartition)
		{
		MrBayesPrint ("%s   Problems allocating constraintPartition in BuildConstraintTree", spacer);
		return ERROR;
		}
	mask = constraintPartition + nLongsNeeded;

	/* calculate mask (needed to take care of unused bits when flipping partitions) */
	for (i=0; i<(nBitsInALong - (numLocalTaxa%nBitsInALong)); i++)
		SetBit (i+numLocalTaxa, mask); 

	/* reset all nodes */
	for (i=0; i<2*numLocalTaxa; i++)
		{
		pp = &pt->nodes[i];
		pp->isDated = NO;
		pp->age = -1.0;
		pp->isLocked = NO;
		pp->lockID = -1;
		}

	/* build a bush */
	pt->root = &pt->nodes[numLocalTaxa];
	for (i=0; i<numLocalTaxa; i++)
		{
		pp = &pt->nodes[i];
		pp->index = i;
		pp->left = NULL;
		if (i == numLocalTaxa - 1)
			pp->sib = NULL;
		else
			pp->sib = &pt->nodes[i+1];
		pp->anc = pt->root;
		}
	pp = pt->root;
	pp->left = &pt->nodes[0];
	pp->anc = pp->sib = NULL;
	pt->nNodes = numLocalTaxa + 1;
	pt->nIntNodes = 1;

	/* make sure the outgroup is the right-most node */
	pt->nodes[localOutGroup].index = numLocalTaxa - 1;
	pt->nodes[numLocalTaxa - 1].index = localOutGroup;

	/* set partition specifiers in bush */
	for (i=0; i<numLocalTaxa; i++)
		{
		pp = &pt->nodes[i];
		SetBit(pp->index, pp->partition);
		}
	pp = pt->root;
	for (i=0; i<numLocalTaxa; i++)
		SetBit (i, pp->partition);

	/* set terminal taxon labels */
	for (i=0; i<numLocalTaxa; i++)
		{
		pp = &pt->nodes[i];
		GetNameFromString (localTaxonNames, pp->label, pp->index + 1);
		}

	/* resolve the bush according to constraints */
	/* for now, satisfy all constraints */
	/* for now, bail out if constraints are not compatible */
	/* Eventually, we might want to be build a parsimony (WAB) or compatibility (WIB) matrix and
	   draw a starting tree from the universe according to the score of the tree. A simple way of accomplishing
	   approximately this is to use sequential addition, with probabilities in each step determined
	   by the parsimony or compatibility score of the different possibilities. */ 
	nextNode = numLocalTaxa + 1;
	for (i=0; i<numDefinedConstraints; i++)
		{
		if (t->constraints[i] == NO)
			continue;
		
		/* initialize bits in partition to add */
		for (j=0; j<nLongsNeeded; j++)
			constraintPartition[j] = 0;
		
		/* set bits in partition to add */
		for (j=k=k1=0; j<numTaxa; j++)
			{
			if (taxaInfo[j].isDeleted == YES)
				continue;
			if (taxaInfo[j].constraints[i] == 1)
				{
				SetBit(k,constraintPartition);
				k1++;
				}
			k++;
			}
		
		/* check that partition is informative if tree is unrooted */
		if ((k1 == 1 || k1 == numLocalTaxa-1) && t->isRooted == NO)
			{
			MrBayesPrint ("%s   WARNING: Constraint %d is uninformative and will be disregarded\n", spacer, i);
			t->constraints[i] = NO;
			continue;
			}

		/* make sure outgroup is outside constrained partition if the tree is unrooted */
		if (t->isRooted == NO && IsBitSet(localOutGroup, constraintPartition))
			FlipBits(constraintPartition, nLongsNeeded, mask);

		/* find first included terminal */
		for (k=0; !IsBitSet(k,constraintPartition); k++)
			;
		for (j=0; pt->nodes[j].index != k; j++)
			;
		pp = &pt->nodes[j];

		/* go down until node is not included in constraint */
		do {
			qq = pp;
			pp = pp->anc;		
		} while (IsPartNested(pp->partition, constraintPartition, nLongsNeeded));	

		/* check that the node has not yet been included */
		for (j=0; j<nLongsNeeded; j++)
			{
			if (qq->partition[j] != constraintPartition[j])
				break;
			}
		if (j==nLongsNeeded)
			{
			MrBayesPrint ("%s   WARNING: Constraint %d is a duplicate of another constraint and will be ignored\n", spacer, i+1);
			t->constraints[i] = NO;
			continue;
			}

		/* create a new node */
		tt = &pt->nodes[nextNode++];
		tt->anc = pp;
		tt->isLocked = YES;
		tt->lockID = i;
		if (constraintAges[i] > 0.0)
			{
			tt->age = constraintAges[i];
			tt->isDated = YES;
			}
		for (j=0; j<nLongsNeeded; j++)
			tt->partition[j] = constraintPartition[j];
		pt->nIntNodes++;
		pt->nNodes++;

		/* sort descendant nodes in two connected groups: included and excluded */
		/* if there is a descendant that overlaps (incompatible) then return error */
		rr = ss = NULL;
		qq = pp->left;
		do {
			if (IsPartNested(qq->partition, constraintPartition, nLongsNeeded))
				{
				if (ss != NULL)
					ss->sib = qq;
				else
					tt->left = qq;
				ss = qq;
				qq->anc = tt;
				}
			else if (IsPartCompatible(qq->partition, constraintPartition, nLongsNeeded))
				{
				if (rr != NULL)
					rr->sib = qq;
				else
					tt->sib = qq;
				rr = qq;
				}
			else
				goto errorExit;
			qq = qq->sib;
			} while (qq != NULL);
		pp->left = tt;
		rr->sib = ss->sib = NULL;
		}

	/* exit */
	free (constraintPartition);
	return NO_ERROR;

errorExit:
	free (constraintPartition);
	return ERROR;
}





void BuildExhaustiveSearchTree (Tree *t, int chain, int nTaxInTree, TreeInfo *tInfo)
{
	int			i;
	TreeNode	*p, *q, *r;
		
	if (nTaxInTree == t->nIntNodes + 1) {
		
		/* Get downpass */
		GetDownPass (t);

		/* Calculate cost of this tree and add to counter */
		tInfo->curScore = GetParsimonyLength (t, chain);
		if (tInfo->curScore < tInfo->minScore)
			{
			tInfo->totalScore *= pow ((tInfo->warp/3.0) / (1.0 - tInfo->warp), tInfo->minScore - tInfo->curScore);
			tInfo->totalScore += 1.0;
			tInfo->minScore = tInfo->curScore;
			}
		else
			tInfo->totalScore += pow (tInfo->warp/3.0, tInfo->curScore - tInfo->minScore) * pow (1.0-tInfo->warp, tInfo->minScore - tInfo->curScore);
	}

	else {

		/* find node to connect */
		q=tInfo->leaf[nTaxInTree];

		/* add using this ancestral node */
		p=tInfo->vertex[nTaxInTree-1];
		q->anc=p;
		p->right=q;

		for (i=0;i<2*nTaxInTree-1;i++) {
			/* find node to connect to */
			if (i>=nTaxInTree)
				r=tInfo->vertex[i-nTaxInTree];
			else
				r=tInfo->leaf[i];

			/* add to this node */
			p->left=r;
			if (r->anc==NULL)
				p->anc=NULL;
			else {
				p->anc=r->anc;
				if (r->anc->left==r)
					r->anc->left=p;
				else
					r->anc->right=p;
			}
			r->anc=p;

			/* next level */
			BuildExhaustiveSearchTree (t, chain, nTaxInTree+1, tInfo);

			if (tInfo->stopScore > 0.0 && tInfo->totalScore >= tInfo->stopScore)
				return;

			/* restore tree before trying next possibility */
			r->anc=p->anc;
			if (r->anc!=NULL) {
				if (r->anc->left==p)
					r->anc->left=r;
				else
					r->anc->right=r;
			}
		}
	}
}





/*----------------------------------------------------------------
|
|	BuildStartTree: Build one starting tree
|
----------------------------------------------------------------*/
int    BuildStartTree (Tree *t, long int *seed)

{

	int				i, j, whichNde, n, *tempNums, numAvailTips, intNodeNum, whichElement, 
					pId, pLftId=0, pRhtId=0, pAncId=0, nUser, stopLoop, tempOut=0, nLongsNeeded;
	MrBFlt			sum, oldBls[5], newBls[5], tempLength, depth;
	TreeNode		*p, *q, *up, *dn, *tempTree, **tempDP,
					*tempRoot=NULL, *p1, *p2, *lft, *rht, *anc, *a, *b, *c;
	PolyTree		constraintTree;
	char			tempName[100];
	long int		*bitsets;
	   
	/* set pointers allocated locally to NULL for correct exit on error */
	tempNums = NULL;
	tempTree = NULL;
	tempDP = NULL;
	constraintTree.nodes = NULL;
	constraintTree.allDownPass = NULL;
	constraintTree.intDownPass = NULL;
	bitsets = NULL;

	/* wipe structure information clean, only setting memory index */
	for (i=0; i<2*numLocalTaxa; i++)
		{
		p = &t->nodes[i];
		p->left = NULL;
		p->right = NULL;
		p->anc = NULL;
		p->memoryIndex = i;
		p->index = 0;
		p->upDateCl = YES;
		p->upDateTi = YES;
		p->marked = NO;
		p->x = p->y = 0;
		for (j=0; j<MAX_NUM_DIV_LONGS; j++)
			{
			p->clSpace[j] = 0;
			p->tiSpace[j] = 0;
			p->scalersSet[j] = 0;
			}
		p->scalerNode = NO;
		strcpy(p->label, "no name");
		p->length = BRLENS_MIN;
		p->nodeDepth = 0.0;
		p->isDated = NO;
		p->age = -1.0;
		p->isLocked = NO;
		p->lockID = -1;
		}
	
	/* now, we start making some trees */
	if (t->checkConstraints == YES && (!strcmp(chainParams.chainStartTree, "Random")))
		{
		/* make random tree consistent with constraints */
		/* first allocate space for partition specifiers and polytomous tree */
		nLongsNeeded = (numLocalTaxa / nBitsInALong) + 1;
		bitsets = (long int *) calloc (2*numLocalTaxa*nLongsNeeded, sizeof(long int));
		if (bitsets == NULL)
			{
			MrBayesPrint ("%s   Problem allocating space for bitsets in BuildStartTree", spacer);
			goto errorExit;
			}
		constraintTree.nodes = (PolyNode *) calloc (2*numLocalTaxa, sizeof (PolyNode));
		constraintTree.intDownPass = (PolyNode **) calloc (numLocalTaxa, sizeof (PolyNode *));
		constraintTree.allDownPass = (PolyNode **) calloc (2*numLocalTaxa, sizeof (PolyNode *));
		if (constraintTree.nodes == NULL || constraintTree.intDownPass == NULL || constraintTree.allDownPass == NULL)
			{
			MrBayesPrint ("%s   Problem allocating space for constraint tree in BuildStartTree", spacer);
			goto errorExit;
			}

		/* and set partition specifiers and memoryIndex*/
		for (i=0; i<2*numLocalTaxa; i++)
			{
			constraintTree.nodes[i].partition = (bitsets + i*nLongsNeeded);
			constraintTree.nodes[i].memoryIndex = i;
			constraintTree.nodes[i].index = i;
			}
		
		/* then make polytomous tree consistent with constraints 
			using the tree struct to find the active constraints */
		if (BuildConstraintTree (t, &constraintTree) == ERROR)
			goto errorExit;

		/* resolve this tree randomly to get a dichotomous tree */
		if (RandResolve (t, &constraintTree, seed) == ERROR)
			goto errorExit;

		/* copy this tree structure to the start tree */
		/* the start tree is rooted correctly in function CopyTo... 
			according to info in the tree struct */
		if (CopyToTreeFromPolyTree (t, &constraintTree) == ERROR)
			goto errorExit;

		/* set branch lengths */
		if (t->isRooted == NO)
			{
			/* unrooted tree, make all brlens the same */
			for (i=0; i<t->nNodes; i++)
				{
				p = t->allDownPass[i];
				p->length =  0.1;
				}
			}
		else if (t->isCalibrated == YES)
			{
			/* calibrated tree */
			if (InitCalibratedBrlens (t) == ERROR)
				{
				MrBayesPrint ("%s   Failed to build calibrated starting tree\n", spacer);
				goto errorExit;
				}
			}
		else
			{
			/* rooted but uncalibrated tree */
			for (i=0; i<t->nNodes-1; i++)
				{
				p = t->allDownPass[i];
				if (p->left == NULL)
					p->nodeDepth = 0.0;
				else
					{
					if (p->left->nodeDepth > p->right->nodeDepth)
						p->nodeDepth = p->left->nodeDepth +  0.1;
					else
						p->nodeDepth = p->right->nodeDepth +  0.1;
					}
				}
			for (i=0; i<t->nNodes-2; i++)
				{
				p = t->allDownPass[i];
				p->length = p->anc->nodeDepth - p->nodeDepth;
				}
			}

		/* finally free constraint tree and bitsets */
		free (bitsets);
		free (constraintTree.allDownPass);
		free (constraintTree.intDownPass);
		free (constraintTree.nodes);
		}
	else if (!strcmp(chainParams.chainStartTree, "Random"))
		{
		/* We only deal with random, unconstrained trees here, rooted or unrooted */
		
		/* first make tree with three terminal nodes */
		p = &t->nodes[0];
		q = &t->nodes[1];
		p->left = q;
		q->anc = p;
		p->length = 0.0f;
		p->nodeDepth = 1.0f;
		q->length =  RandomNumber(seed);
		q->nodeDepth = 1.0f - q->length;
		p = q;
		q = &t->nodes[2];
		p->left = q;
		q->anc = p;
		q->length = 1.0f - p->length;
		q->nodeDepth = 0.0f;
		q = &t->nodes[3];
		p->right = q;
		q->anc = p;
		q->length = 1.0f - p->length;
		q->nodeDepth = 0.0f;

		/* then add rest of nodes two at a time to random branch */
		for (i=4; i<t->nNodes; i += 2)
			{
			do
				{
				whichNde = (int)(RandomNumber(seed) * i);
				p = &t->nodes[whichNde];
				} while (p->anc == NULL);
			q = p->anc;
			up = &t->nodes[i];
			dn = &t->nodes[i+1];
			if (q->left == p)
				{
				q->left = dn;
				dn->anc = q;
				dn->left = p;
				p->anc = dn;
				dn->right = up;
				up->anc = dn;
				dn->length = p->length *  RandomNumber(seed);
				dn->nodeDepth = q->nodeDepth - dn->length;
				p->length -= dn->length;
				up->length = dn->nodeDepth;
				up->nodeDepth = 0.0;
				}
			else
				{
				q->right = dn;
				dn->anc = q;
				dn->right = p;
				p->anc = dn;
				dn->left = up;
				up->anc = dn;
				dn->length = p->length *  RandomNumber(seed);
				dn->nodeDepth = q->nodeDepth - dn->length;
				p->length -= dn->length;
				up->length = dn->nodeDepth;
				up->nodeDepth =  0.0;
				}
			}
		t->root= &t->nodes[0];
		
		/* get the traversal sequence for the nodes */
		GetDownPass (t);
		
		/* relabel tips and interior nodes */
		tempNums = (int *)malloc((size_t) ((numLocalTaxa) * sizeof(int)));
		if (!tempNums)
			{
			MrBayesPrint ("%s   Problem allocating tempNums (%d)\n", spacer, (numLocalTaxa-1) * sizeof(int));
			goto errorExit;
			}
		if (t->isRooted == NO)
			{
			n = 0;
			for (i=0; i<numLocalTaxa; i++)
				if (i != localOutGroup)
					tempNums[n++] = i;
			numAvailTips = numLocalTaxa - 1;
			}
		else
			{
			for (i=0; i<numLocalTaxa; i++)
				tempNums[i] = i;
			numAvailTips = numLocalTaxa;
			}
		intNodeNum = numLocalTaxa;
		for (i=0; i<t->nNodes; i++)
			{
			p = t->allDownPass[i];
			if (p->left == NULL && p->right == NULL && p->anc != NULL)
				{
				whichElement = (int)(RandomNumber(seed) * numAvailTips);
				p->index = tempNums[whichElement];
				tempNums[whichElement] = tempNums[numAvailTips-1];
				numAvailTips--;
				}
			else if (p->left != NULL && p->right == NULL && p->anc == NULL)
				{
				if (t->isRooted == NO)
					{
					p->index = localOutGroup;
					}
				else
					{
					p->index = intNodeNum;
					intNodeNum++;
					}
				}
			else
				{
				p->index = intNodeNum;
				intNodeNum++;
				}
			}
		free (tempNums);
		
		/* add taxon labels to tips of tree */
		for (i=0; i<t->nNodes; i++)
			{
			p = t->allDownPass[i];
			if (t->isRooted == NO)
				{
				if (p->left == NULL || p->right == NULL || p->anc == NULL)
					{
					if (GetNameFromString (localTaxonNames, tempName, p->index + 1) == ERROR)
						{
						MrBayesPrint ("%s   Error getting taxon names \n", spacer);
						goto errorExit;
						}
					strcpy (p->label, tempName);
					}
				}
			else
				{
				if (p->left == NULL && p->right == NULL && p->anc != NULL)
					{
					if (GetNameFromString (localTaxonNames, tempName, p->index + 1) == ERROR)
						{
						MrBayesPrint ("%s   Error getting taxon names \n", spacer);
						goto errorExit;
						}
					strcpy (p->label, tempName);
					}
				}
			}
			
		/* branch lengths */
		if (t->isRooted == NO)
			{
			/* We have an unrooted tree and we will make all of the branch lengths equal in
			   length (v = 0.1). */
			for (i=0; i<t->nNodes; i++)
				{
				p = t->allDownPass[i];
				if (p->anc != NULL)
					p->length = 0.1;
				}
			}
		else
			{
			/* Otherwise we have a rooted tree. The branch lengths were initialized when the
			   tree was being built (above), but the branch to the left of the root node has
			   a length. We rescale the branch lengths such that the root of the tree is at 0. */
			tempLength = t->root->left->length;
			for (i=0; i<t->nNodes; i++)
				{
				p = t->allDownPass[i];
				if (p->anc != NULL)
					{
					if (p->anc->anc != NULL)
						{
						p->length /= (1.0f - tempLength);
						p->nodeDepth /= (1.0f - tempLength);
						}
					else
						{
						p->length = 0.0f;
						p->nodeDepth = 1.0f;
						}
					}
				else
					{
					p->length = p->nodeDepth = 0.0f;
					}
				}
				
			/* Let the total depth be determined by tree size */
			/* First count max number of branches to root */
			for (i=0; i<t->nNodes; i++)
				{
				p = t->allDownPass[i];
				if (p->left == NULL || p->right == NULL)
					p->x = 0;
				else
					{
					if (p->left->x > p->right->x)
						p->x = p->left->x + 1;
					else
						p->x = p->right->x + 1;
					}
				}
			/* Then set depth as a function of max number of branches */
			depth = 0.01f * t->root->left->x;
			/* Now rescale all branch lengths and node times so that the root is at depth = depth. */
			tempLength = t->root->left->nodeDepth;
			for (i=0; i<t->nNodes; i++)
				{
				p = t->allDownPass[i];
				if (p->anc != NULL)
					{
					if (p->anc->anc != NULL)
						{
						p->nodeDepth *= depth/tempLength;
						}
					else
						{
						p->nodeDepth = depth;
						}
					}
				}
			for (i=0; i<t->nNodes; i++)
				{
				p = t->allDownPass[i];
				if (p->anc != NULL)
					{
					if (p->anc->anc != NULL)
						{
						p->length = p->anc->nodeDepth - p->nodeDepth;
						}
					else
						{
						p->length = 0.0;
						}
					}
				}

			/* if some nodes are calibrated then we need to recalculate */
			/* the branch lengths using a method consistent with calibrations */
			if (t->isCalibrated == YES)
				if (InitCalibratedBrlens (t) == ERROR)
					{
					MrBayesPrint ("%s   Failed to build calibrated starting tree\n", spacer);
					goto errorExit;
					}
			}
		}
	else
		{
		/* Our chain is going to start from the user-input tree. */

		/* Is a user tree even specified? */
		if (isUserTreeDefined == NO)
			{
			MrBayesPrint ("%s   A user tree has not been defined to start chain\n", spacer);
			goto errorExit;
			}

		/* allocate some temporary space */
		tempTree = (TreeNode *)malloc((size_t) (2 * numTaxa * sizeof(TreeNode)));
		if (!tempTree)
			{
			MrBayesPrint ("%s   Problem allocating tempTree (%d)\n", spacer, 2 * numTaxa * sizeof(Tree));
			goto errorExit;
			}
		tempDP = (TreeNode **)malloc((size_t) (2 * numTaxa * sizeof(TreeNode *)));
		if (!tempDP)
			{
			MrBayesPrint ("%s   Problem allocating tempDP (%d)\n", spacer, 2 * numTaxa * sizeof(Tree));
			goto errorExit;
			}
			
		/* set temporary space to NULL */
		for (i=0; i<2*numTaxa; i++)
			{
			tempTree[i].left = NULL;
			tempTree[i].right = NULL;
			tempTree[i].anc = NULL;
			tempTree[i].length = 0.0;
			}
			
		/* copy user tree into temporary space */
		for (i=0; i<2*numTaxa; i++)
			{
			p = userNodes + i;
			if (p == NULL)
				pId = -1;
			else
				{
				pId = p->index;
				if (p->left == NULL)
					pLftId = -1;
				else
					pLftId = p->left->index;
				if (p->right == NULL)
					pRhtId = -1;
				else
					pRhtId = p->right->index;
				if (p->anc == NULL)
					pAncId = -1;
				else
					pAncId = p->anc->index;
				}
				
			if (pId != -1)
				{
				q = tempTree + pId;
				q->index = pId;
				q->length = 0.0;
				if (userBrlensDef == YES)
					q->length = p->length;
				if (p == userRoot)
					tempRoot = q;
				lft = rht = anc = NULL;
				if (pLftId != -1)
					{
					lft = tempTree + pLftId;
					q->left = lft;
					}
				if (pRhtId != -1)
					{
					rht = tempTree + pRhtId;
					q->right = rht;
					}
				if (pAncId != -1)
					{
					anc = tempTree + pAncId;
					q->anc = anc;
					}
				}
			}
			
		/* get down pass of original user tree nodes */
		nUser = 0;
		GetUserDownPass (tempRoot, tempDP, &nUser);
		
		/* Change root of tree, if necessary. Note that when the user tree is read in, it
		   is automatically rooted in a consistent manner. If the user tree is rooted,
		   then the outgroup species is the first right node. If the user tree is unrooted,
		   then the outgroup species points down. */
		if (isUserTreeRooted == NO && t->isRooted == YES)
			{
			/* root the tree as the user tree was unrooted and we want it rooted */
			/* first, lets get two empty nodes */
			p1 = p2 = NULL;
			for (i=0, n=0; i<2*numTaxa; i++)
				{
				p = tempTree + i;
				if (p->left == NULL && p->right == NULL && p->anc == NULL)
					{
					if (n == 0)
						p1 = p;
					else if (n == 1)
						p2 = p;
					n++;
					}
				}
			if (p1 == NULL || p2 == NULL)
				{
				MrBayesPrint ("%s   Could not find two empty nodes\n", spacer);
				goto errorExit;
				}
			
			/* now, bend the current root node around and root with an empty node */
			p = tempRoot;
			q = p->left;
			q->anc = p->anc = p1;
			p->left = p->right = NULL;
			p1->anc = p2;
			p1->left = q;
			p1->right = p;
			p2->left = p1;
			tempRoot = p2;
			q->length *= 0.5;
			p->length = q->length;
			tempRoot->length = p1->length = 0.0;
			}
		else if (isUserTreeRooted == YES && t->isRooted == NO)
			{
			p1 = tempRoot->left;
			p = p1->left;
			q = p1->right;
			if (q->index != outGroupNum)
				{
				MrBayesPrint ("%s   User defined tree is improperly rooted\n", spacer);
				goto errorExit;
				}
				
			p->anc = q;
			q->left = p;
			p1->left = p1->right = p1->anc = q->anc = NULL;
			p->length += q->length;
			q->length = 0.0;
			tempRoot = q;
			}
		
		/* get down pass of modified user tree nodes */
		nUser = 0;
		GetUserDownPass (tempRoot, tempDP, &nUser);

		/* If we have an unrooted tree, make certain it is rooted at the correct node. Specifically,
		   we need to make certain that the node pointing down is either the undeleted outgroup
		   taxon (outGroupNum) or the first undeleted taxon. localOutGroup is the number of either
		   the outgroup or the first undeleted taxon, if the outgroup was deleted. */
		if (t->isRooted == NO)
			{
			/* Get the number of the local outgroup in terms of the old taxon numbers. */
			for (i=0, n=0; i<numTaxa; i++)
				{
				if (n == localOutGroup && taxaInfo[i].isDeleted == NO)
					{
					tempOut = i;
					break;
					}
				if (taxaInfo[i].isDeleted == NO)
					n++;
				}

			/* The tree is not rooted as we want it. We had better 
			   reroot the tree at tempOut. */
			if (tempRoot->index != tempOut)
				{
				/* Mark nodes from taxon tempOut to the root */
				for (i=0; i<nUser; i++)
					{
					p = tempDP[i];
					p->marked = NO;
					if (p->index == tempOut)
						p->marked = YES;
					}
				for (i=0; i<nUser; i++)
					{
					p = tempDP[i];
					if (p->left != NULL && p->right != NULL && p->anc != NULL)
						{
						if (p->left->marked == YES || p->right->marked == YES)
							p->marked = YES;
						}
					}
				
				/* find a few spare nodes */
				p1 = p2 = NULL;
				for (i=0, n=0; i<2*numTaxa; i++)
					{
					p = tempTree + i;
					if (p->left == NULL && p->right == NULL && p->anc == NULL)
						{
						if (n == 0)
							p1 = p;
						else if (n == 1)
							p2 = p;
						n++;
						}
					}
				if (p1 == NULL || p2 == NULL)
					{
					MrBayesPrint ("%s   Could not find two empty nodes\n", spacer);
					goto errorExit;
					}
				
				/* temporarily root tree */
				a = tempRoot->left;
				b = tempRoot;
				b->left = b->right = NULL;
				a->anc = b->anc = p1;
				p1->left = a;
				p1->right = b;
				p1->anc = p2;
				p2->left = p1;
				p2->right = p2->anc = NULL;
				tempRoot = p2;
				a->length *= 0.5;
				b->length = a->length;
				
				/* set p to the node left of the new root */
				p = p1;
							
				/* now, rotate tree until tempOut is to the left of root */
				stopLoop = NO;
				do
					{
					if (p->left->marked == YES && p->right->marked == NO)
						{
						q = p->left;
						c = p->right;
						}
					else if (p->left->marked == NO && p->right->marked == YES)
						{
						q = p->right;
						c = p->left;
						}
					else 
						{
						MrBayesPrint ("%s   Could not find an unmarked node\n", spacer);
						goto errorExit;
						}
					if (q->left == NULL && q->right == NULL)
						stopLoop = YES;
					else
						{
						if (q->left->marked == YES && q->right->marked == NO)
							{
							a = q->right;
							b = q->left;
							}
						else if (q->left->marked == NO && q->right->marked == YES)
							{
							a = q->left;
							b = q->right;
							}
						else 
							{
							MrBayesPrint ("%s   Could not find an unmarked node\n", spacer);
							goto errorExit;
							}
						}
					if (stopLoop == NO)
						{
						p->left = b;
						b->anc = p;
						p->right = q;
						q->anc = p;
						q->left = a;
						q->right = c;
						a->anc = c->anc = q;
						q->marked = NO;	
						c->length += q->length;
						b->length *= 0.5;
						q->length = b->length;
						}
					} while (stopLoop == NO);
					
				/* now, root the tree at tempOut */
				a = p->left;
				b = p->right;
				a->left = b;
				b->anc = a;
				a->right = a->anc = NULL;
				p1->left = p1->right = p1->anc = NULL;
				p2->left = p2->right = p2->anc = NULL;
				b->length += a->length;
				a->length = 0.0;
				tempRoot = a;
						
				/* get traversal sequence as the tree has changed */
				nUser = 0;
				GetUserDownPass (tempRoot, tempDP, &nUser);
				}
			}

		/* Trim away some tips, if they are excluded. We don't have to worry about
		   the rooting of the tree because if the tree is unrooted, a non-excluded
		   taxon definitely points down. (That is the point of the exercise immediately
		   above.) */
		for (i=0; i<nUser; i++)
			{
			p = tempDP[i];
			if (p->left == NULL && p->right == NULL && p->anc != NULL && taxaInfo[p->index].isDeleted == YES)
				{
				q = p->anc;
				if (q->left == p)
					up = q->right;
				else
					up = q->left;
				dn = q->anc;
				if (dn->left == q)
					dn->left = up;
				else
					dn->right = up;
				up->anc = dn;
				up->length += q->length;
				p->left = p->right = p->anc = NULL;
				q->left = q->right = q->anc = NULL;
				}
			}
			
		/* get traversal sequence (again!) as the tree might have changed */
		nUser = 0;
		GetUserDownPass (tempRoot, tempDP, &nUser);
		
		/* relabel tips */
		for (i=0; i<nUser; i++)
			{
			p = tempDP[i];
			if (p->left == NULL && p->right == NULL && p->anc != NULL)
				{
				for (j=0, n=0; j<numTaxa; j++)
					{
					if (p->index == j)
						{
						p->index = n;
						break;
						}
					if (taxaInfo[j].isDeleted == NO)
						n++;
					}
				}
			else if (p->left != NULL && p->right == NULL && p->anc == NULL && t->isRooted == NO)
				{
				for (j=0, n=0; j<numTaxa; j++)
					{
					if (p->index == j)
						{
						p->index = n;
						break;
						}
					if (taxaInfo[j].isDeleted == NO)
						n++;
					}
				}
			}

		/* relabel interior nodes */
		for (i=0, n=numLocalTaxa; i<nUser; i++)
			{
			p = tempDP[i];
			if (p->left != NULL && p->right != NULL && p->anc != NULL)
				p->index = n++;
			else if (p->left != NULL && p->right == NULL && p->anc == NULL && t->isRooted == YES)
				p->index = n++;
			}
			
		/* at last! copy tree into nodes */
		for (i=0; i<nUser; i++)
			{
			p = tempDP[i];
			p1 = &t->nodes[p->index];
			p1->index = p->index;
			p1->length = p->length;
			if (p == tempRoot)
				{
				t->root = p1;
				}
			}
		for (i=0; i<nUser; i++)
			{
			p = tempDP[i];
			p1 = &t->nodes[p->index];
			if (p->left != NULL)
				{
				p1->left = &t->nodes[p->left->index];
				}
			if (p->right != NULL)
				{
				p1->right = &t->nodes[p->right->index];
				}
			if (p->anc != NULL)
				{
				p1->anc = &t->nodes[p->anc->index];
				}
			}
			
		/* free temporary nodes */
		free (tempTree);
		free (tempDP);

		/* get the traversal sequence for the nodes */
		GetDownPass (t);
			
		/* add taxon labels to tips of tree */
		for (i=0; i<t->nNodes; i++)
			{
			p = t->allDownPass[i];
			if (t->isRooted == NO)
				{
				if (p->left == NULL || p->right == NULL || p->anc == NULL)
					{
					if (GetNameFromString (localTaxonNames, tempName, p->index + 1) == ERROR)
						{
						MrBayesPrint ("%s   Error getting taxon names \n", spacer);
						goto errorExit;
						}
					strcpy (p->label, tempName);
					}
				}
			else
				{
				if (p->left == NULL && p->right == NULL && p->anc != NULL)
					{
					if (GetNameFromString (localTaxonNames, tempName, p->index + 1) == ERROR)
						{
						MrBayesPrint ("%s   Error getting taxon names \n", spacer);
						goto errorExit;
						}
					strcpy (p->label, tempName);
					}
				}
			}
			
		/* perturb each user tree, if needed */
		if (chainParams.numStartPerts > 0)
			{
			for (i=0; i<chainParams.numStartPerts; i++)
				{
				do
					{
					whichElement = (int)(RandomNumber(seed) * t->nIntNodes);
					p = t->intDownPass[whichElement];
					} while (p->anc->anc == NULL);
				q = p->anc;
				a  = p->left;
				b  = p->right;
				if (q->left == p)
					{
					c  = q->right;
					}
				else	
					{
					c  = q->left;
					}
				sum = a->length + b->length + c->length + p->length + q->length;
				if (RandomNumber(seed) < 0.5)
					{
					p->left = a;
					a->anc  = p;

					p->right = c;
					c->anc  = p;

					q->left = p;
					p->anc = q;

					q->right = b;
					b->anc  = q;
					}
				else
					{
					p->left = b;
					b->anc  = p;

					p->right = c;
					c->anc  = p;

					q->left = p;
					p->anc = q;

					q->right = a;
					a->anc  = q;
					}
				if (sum > 0.0)
					{
					/* We also need to change the branch lengths. However, we don't
					   change the overall tree length this way. */
					oldBls[0] = a->length / sum;
					oldBls[1] = b->length / sum;
					oldBls[2] = c->length / sum;
					oldBls[3] = p->length / sum;
					oldBls[4] = q->length / sum;
					DirichletRandomVariable (oldBls, newBls, 5, seed);
					a->length = newBls[0] * sum;
					b->length = newBls[1] * sum;
					c->length = newBls[2] * sum;
					p->length = newBls[3] * sum;
					q->length = newBls[4] * sum;
					}
				else
					{
					a->length = BRLENS_MIN;
					b->length = BRLENS_MIN;
					c->length = BRLENS_MIN;
					p->length = BRLENS_MIN;
					q->length = BRLENS_MIN;
					}
				}
				
			/* get the traversal sequence for the nodes as we probably changed the tree */
			GetDownPass (t);
			}
			
		/* branch lengths */
		if (t->isRooted == NO)
			{
			if (userBrlensDef == YES)
				{
				/* We have an unrooted tree and user branch lengths have been defined.
				   We don't need to do anything, but continue on our way. */
				}
			else
				{
				/* We have an unrooted tree and user branch lengths have not been defined.
				   We will make all of the branch lengths equal in length (v = 0.1). */
				for (i=0; i<t->nNodes; i++)
					{
					p = t->allDownPass[i];
					if (p->anc != NULL)
						p->length = 0.1f;
					}
				}
			}
		else
			{
			if (userBrlensDef == YES)
				{
				/* We have a rooted tree and user branch lengths have been defined.
				   We check that the user-defined branch lengths satisfy the clock.
				   If they do, then everything is fine and we continue on our way.
				   If they don't, then we initialize the branch lengths */
				if (IsClockSatisfied(t,  0.00001) == NO)
					{
					if (InitClockBrlens (t) == ERROR)
						{
						MrBayesPrint ("%s   There has been an error when initializing branch lengths\n", spacer);
						goto errorExit;
						}
					}
				}
			else
				{
				/* We have a rooted tree and user branch lengths have not been defined.
				   We need to initialize the branch lengths (such that the clock is
				   satisfied). */
				if (InitClockBrlens (t) == ERROR)
					{
					MrBayesPrint ("%s   There has been an error when initializing branch lengths\n", spacer);
					goto errorExit;
					}
				}
			}

		}

	/* if constraints are defined and the starting tree is a user tree, check that it fulfils constraints */
	if (t->checkConstraints == YES && (!strcmp(chainParams.chainStartTree, "User")))
		{
		if (CheckSetConstraints (t) == ERROR)
			{
			MrBayesPrint ("%s   User tree cannot be used as starting tree because it does not fulfil constraints\n", spacer);
			goto errorExit;
			}
		}

	/* check constraints just in case */
	if (t->checkConstraints == YES)
		{
		if (CheckConstraints (t) == ERROR)
			{
			MrBayesPrint ("%s   Something wrong with constraints (a bug)\n", spacer);
			goto errorExit;
			}
		}

	/* show tree */
#	if defined DEBUG_BUILDSTARTTREE
	if (ShowMCMCTree (t) == ERROR)
		{
		MrBayesPrint ("%s   Problem showing mcmc tree\n", spacer);
		goto errorExit;
		}

	ShowNodes (t->root, 3, t->isRooted);
	getchar ();
#	endif
	
	return (NO_ERROR);

	errorExit:
		if (tempNums)
			free (tempNums);
		if (tempTree)
			free (tempTree);
		if (tempDP)
			free (tempDP);
		if (bitsets)
			free (bitsets);
		if (constraintTree.allDownPass)
			free (constraintTree.allDownPass);
		if (constraintTree.intDownPass)
			free (constraintTree.intDownPass);
		if (constraintTree.nodes)
			free (constraintTree.nodes);

		return ERROR;

}





/*------------------------------------------------------------------
|
|	CalcLike_Adgamma: calc likelihood for one adgamma correlation HMM
|
-------------------------------------------------------------------*/
int CalcLike_Adgamma (int d, Param *param, int chain, MrBFlt *lnL)

{
	int				c, i, j, nRates, pos;
	long int		inHMM;
	MrBFlt			logScaler, max, prob, *F,
					*oldF, *tempF, fSpace[2][MAX_GAMMA_CATS];
	MrBFlt			*rP;
	CLFlt			freq, *lnScaler;
	ModelInfo		*m;
	
	/* find nRates for first division in HMM */
	m = &modelSettings[d];
	nRates = m->numGammaCats;

	/* calculate rate category frequencies */
	freq = (CLFlt) ((CLFlt) 1.0 / nRates);

	/* find Markov trans probs */
	F = GetParamSubVals (param,chain, state[chain]);
	for (i=pos=0; i<nRates; i++)
		for (j=0; j<nRates; j++)
			markovTi[0][i][j] = F[pos++];
	
	/* precalculate Markov trans probs up to largest small jump */
	/* but only if needed                                       */
	for (i=1; i<MAX_SMALL_JUMP; i++)
		{
		if (hasMarkovTi[i] == YES)
			{
			if (hasMarkovTi[i-1] == YES || i == 1)
				MultiplyMatrices(nRates, markovTi[i-1], markovTi[0], markovTi[i]);
			else
				MultiplyMatrixNTimes(nRates, markovTi[0], i+1, markovTi[i]);
			}
		}
		
	/* find site scaler for this chain and state */
	lnScaler = treeScaler[chain] + state[chain] * numCompressedChars;
	
	/* find rate probs for this chain and state */
	rP = rateProbs[chain] + state[chain] * rateProbRowSize;
	
	/* set bit vector indicating divisions in this HMM */
	inHMM = 0;
	for (i=0; i<param->nRelParts; i++)
		{
		if (modelSettings[param->relParts[i]].shape ==
			modelSettings[d].shape)
			{
			SetBit(param->relParts[i], &inHMM);
			}
		}
	
	/* reset logScaler */
	logScaler = 0.0;

	/* Perform the so-called forward algorithm of HMMs  */	
	/* set up the space for f(c,i) */
	F = fSpace[0];
	oldF = fSpace[1];

	for (c=0; c<numChar; c++)
		{
		if (IsBitSet(charInfo[c].partitionId[partitionNum-1] - 1, &inHMM) == YES)
			break;
		}

			
	/* fill in fi(0) */
	max = 0.0;
	m = &modelSettings[charInfo[c].partitionId[partitionNum-1] - 1];
	pos = m->rateProbStart + (compCharPos[c] - m->compCharStart) * m->numGammaCats;

	for (i=0; i<nRates; i++)
		{
		F[i] = rP[pos++];
		if (F[i] > max)
			max = F[i];
		}

	for (i=0; i<nRates; i++)
		F[i] /= max;

	logScaler += lnScaler[compCharPos[c]] +  log(max);

	/* now step along the sequence to the end */
	for (c++; c<numChar; c++)
		{
		/* skip if excluded */
		if (charInfo[c].isExcluded == YES)
			continue;
		
		/* skip if not in HMM */
		if (IsBitSet(charInfo[c].partitionId[partitionNum-1] - 1,&inHMM) == NO)
			continue;
		
		/* switch F and oldF, since the previous F is now old */
		tempF = F;
		F = oldF;
		oldF = tempF;

		/* find the position of the rate probs */
		m = &modelSettings[charInfo[c].partitionId[partitionNum-1] - 1];
		pos = m->rateProbStart + (compCharPos[c] - m->compCharStart) * m->numGammaCats;
				
		/* calculate the HMM forward probs fi(x) at site x in HMM */
		if (siteJump[c] <= MAX_SMALL_JUMP)
			{
			max = 0.0;
			for (i=0; i<nRates; i++)
				{
				prob = 0.0;
				for (j=0; j<nRates; j++)
					prob += markovTi[siteJump[c]-1][i][j] * oldF[j];
				F[i] = rP[pos++] * prob;
				if (F[i] > max)
					max = F[i];
				}
			}
		else if (siteJump[c] < BIG_JUMP)	/* intermediate jump, calculate trans probs */
			{
			MultiplyMatrixNTimes(nRates, markovTi[0], siteJump[c], markovTiN);
			max = 0.0;
			for (i=0; i<nRates; i++)
				{
				prob = 0.0;
				for (j=0; j<nRates; j++)
					prob += markovTiN[i][j] * oldF[j];
				F[i] = rP[pos++] * prob;
				if (F[i] > max)
					max = F[i];
				}
			}
		else	/* big jump, use stationary freqs */
			{
			max = 0.0;
			for (i=0; i<nRates; i++)
				{
				prob = 0.0;
				for (j=0; j<nRates; j++)
					prob += (oldF[j] / freq);
				F[i] = rP[pos++] * prob;
				if (F[i] > max)
					max = F[i];
				}
			}

		/* rescale and adjust total scaler with HMM scaler and site scaler */
		for (i=0; i<nRates; i++)
			F[i] /= max;

		logScaler += lnScaler[compCharPos[c]] +  log(max);
		
		}
	
	/* now pull the rate probs together at the end, F contains the vals needed */
	prob =  0.0;
	for (i=0; i<nRates; i++)
		prob += (freq * F[i]);

	(*lnL) = logScaler +  log(prob);

	return (NO_ERROR);

}




/* CalcPartFreqStats: Calculate standard deviation of partition frequencies */
void CalcPartFreqStats (PFNODE *p, STATS *stat)
{
	int 	i, j, n, min;
	MrBFlt 	f, sum, sumsq, stdev;

	n = chainParams.numRuns;
	min = (int) (chainParams.minPartFreq * stat->numSamples);

	if (p->left != NULL) 
		CalcPartFreqStats (p->left, stat);
	if (p->right != NULL)
		CalcPartFreqStats (p->right, stat);

	for (i=0; i<n; i++)
		{
		if (p->count[i] >= min)
			break;
		}
	if (i == n)
		return;

	sum = 0.0;
	sumsq = 0.0;
	for (i=0; i<n; i++)
		{
		f = p->count[i] / stat->numSamples;
		sum += f;
		sumsq += f * f;
		}
	
	f = (sumsq - sum * sum / n) / (n - 1);
	if (f < 0.0)
		stdev = 0.0;
	else
		stdev = sqrt (f);
	
	stat->sum += stdev;
	stat->numPartitions++;

	if (chainParams.allComps == YES)
		{
		for (i=0; i<n; i++)
			{
			for (j=i+1; j<n; j++)
				{
				if (p->count[i] < min && p->count[j] < min)
					continue;

				sum = 0.0;
				sumsq = 0.0;

				f = p->count[i] / stat->numSamples;
				sum += f;
				sumsq += f * f;
				
				f = p->count[j] / stat->numSamples;
				sum += f;
				sumsq += f * f;

				f = (2.0 * sumsq - sum * sum);
				if (f < 0.0)
					stdev = 0.0;
				else
					stdev = (sqrt (f)) / (MrBFlt) 2.0;
				
				stat->pair[i][j] += stdev;
				stat->pair[j][i]++;
				}
			}
		}
}





/*----------------------------------------------------------------
|
|	CalculateTopConvDiagn: Calculate average standard deviation in
|      clade credibility (partition frequency) values
|
----------------------------------------------------------------*/
void CalculateTopConvDiagn (int numSamples)
{
	int		i, j, n;
	STATS	*stat;
	
	for (n=0; n<numTrees; n++)
		{
		stat = &chainParams.stat[n];
		stat->numSamples = numSamples;
		stat->numPartitions = 0.0;
		stat->sum = 0.0;

		if (chainParams.allComps == YES)
			{
			for (i=0; i<chainParams.numRuns; i++)
				for (j=0; j<chainParams.numRuns; j++)
					stat->pair[i][j] = 0.0;
			}
		
		CalcPartFreqStats (partFreqTreeRoot[n], stat);
		
		stat->avgStdDev = stat->sum / stat->numPartitions;
		}
}




/*-----------------------------------------------------------
|
|	CheckCharCodingType: check if character is parsimony-
|		informative, variable, or constant
|
-----------------------------------------------------------*/

void CheckCharCodingType (Matrix *m, CharInfo *ci)

{
	int		i, j, k, x, n1[10], n2[10], largest, smallest, numPartAmbig,
			numConsidered, numInformative, lastInformative=0, uniqueBits,
			newPoss, oldPoss, combinations[2048], *newComb, *oldComb, *tempComb;

	extern int NBits (int x);

	/* set up comb pointers */
	oldComb = combinations;
	newComb = oldComb + 1024;

	/* set counters to 0 */
	numPartAmbig = numConsidered = 0;

	/* set variable and informative to yes */
	ci->variable = ci->informative = YES;

	/* set constant to no and state counters to 0 for all states */
	for (i=0; i<10; i++)
		{
		ci->constant[i] = NO;
		n1[i] = n2[i] = 0;
		}

	for (i=0; i<m->nRows; i++)
		{
		/* retrieve character */
		x = m->origin[m->column + i*m->rowSize];

		/* add it to counters if not all ambiguous */
		if (NBits(x) < ci->nStates)
			{
			numConsidered++;
			if (NBits(x) > 1)
				numPartAmbig++;
			for (j=0; j<10; j++)
				{
				if (((1<<j) & x) != 0)
					{	
					n1[j]++;
					if (NBits(x) == 1)
						n2[j]++;
					}
				}
			}
		}

	/* if the ambig counter for any state is equal to the number of considered
	   states, then set constant for that state and set variable and informative to no */
	for (i=0; i<10; i++)
		{
		if (n1[i] == numConsidered)
			{
			ci->constant[i] = YES;
			ci->variable = ci->informative = NO;
			}
		}

	/* return if variable is no or if a restriction site char */
	if (ci->variable == NO || ci->dType == RESTRICTION)
		return;

	/* the character is either (variable and uninformative) or informative */
	
	/* first consider unambiguous characters */
	/* find smallest and largest unambiguous state for this character */
	smallest = 9;
	largest = 0;
	for (i=0; i<10; i++)
		{
		if (n2[i] > 0)
			{
			if (i < smallest)
				smallest = i;
			if (i > largest)
				largest = i;
			}
		}
		
	/* count the number of informative states in the unambiguous codings */
	for (i=numInformative=0; i<10; i++)
		{
		if (ci->cType == ORD && n2[i] > 0 && i != smallest && i != largest)
			{	
			numInformative++;
			lastInformative = i;
			}
		else if (n2[i] > 1)
			{
			numInformative++;
			lastInformative = i;
			}
		}

	/* set informative */
	if (numInformative > 1)
		ci->informative = YES;
	else
		ci->informative = NO;

	
	/* we can return now unless informative is no and numPartAmbig is not 0 */
	if (!(numPartAmbig > 0 && ci->informative == NO))
		return;

	/* check if partially ambiguous observations make this character informative
	   after all */
	
	/* first set the bits for the taken states */
	x = 0;
	for (i=0; i<10; i++)
		{
		if (n2[i] > 0 && i != lastInformative)
			x |= (1<<i);
		}
	oldPoss = 1;
	oldComb[0] = x;

	/* now go through all partambig chars and see if we can add them without
	   making the character informative */
	for (i=0; i<m->nRows; i++)
		{
		x = m->origin[m->column + i*m->rowSize];
		/* if partambig */ 
		if (NBits(x) > 1 && NBits(x) < ci->nStates)
			{
			/* remove lastInformative */
			x &= !(1<<lastInformative);
			/* reset newPoss */
			newPoss = 0;
			/* see if we can add it, store all possible combinations */
			for (j=0; j<oldPoss; j++)
				{
				uniqueBits = x & (!oldComb[j]);
				for (k=0; k<10; k++)
					{
					if (((1<<k) & uniqueBits) != 0)
						newComb[newPoss++] = oldComb[j] | (1<<k);
					}
				}
			/* break out if we could not add it */
			if (newPoss == 0)
				break;
			
			/* prepare for next partAmbig */
			oldPoss = newPoss;
			tempComb = oldComb;
			oldComb = newComb;
			newComb = tempComb;
			}
		}

	if (i < m->nRows)
		ci->informative = YES;

	return;
	
}





/*----------------------------------------------------------------
|
|	CheckConstraints: Check that tree complies with constraints
|
----------------------------------------------------------------*/
int CheckConstraints (Tree *t)

{

	int				a, i, j, k, nLongsNeeded;
	long int		*constraintPartition, *mask, *bitsets;
	TreeNode		*p=NULL;
	   	
	/* allocate space */
	nLongsNeeded = (numLocalTaxa / nBitsInALong) + 1;

	bitsets = (long int *) calloc ((t->nNodes + 1)*nLongsNeeded + /*1*/
                                       1+numLocalTaxa/nBitsInALong+1, 
                                       sizeof(long int));

	if (!bitsets)
		{
		MrBayesPrint ("%s   Problems allocating bitsets in CheckConstraints", spacer);
		return ERROR;
		}

	/* set partition pointers */
	for (i=0; i<t->nNodes; i++) 
		{
		p = t->allDownPass[i];
		p->partition = bitsets + i*nLongsNeeded;
		}
	constraintPartition = bitsets + (t->nNodes * nLongsNeeded);
	mask = bitsets + ((t->nNodes + 1) * nLongsNeeded);

	/* calculate mask (needed to take care of unused bits when flipping partitions) */
	for (i=0; i<(nBitsInALong - (numLocalTaxa%nBitsInALong)); i++) 
	  SetBit (i+numLocalTaxa, mask); 
	
	/* set partition specifiers for terminals */
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->left == NULL || (t->isRooted == NO && p->anc == NULL))
			SetBit(p->index, p->partition);
		}

	/* set partition specifiers for interior nodes */
	for (i=0; i<t->nIntNodes; i++)
		{
		p = t->intDownPass[i];
		if (p->anc != NULL)
			{
			for (j=0; j<nLongsNeeded; j++)
				p->partition[j] = p->left->partition[j] | p->right->partition[j];
			}
		}

	for (a=0; a<t->nConstraints; a++)
		{
		if (t->constraints[a] == NO)
			continue;

		/* set bits in partition to add */
		for (j=0; j<nLongsNeeded; j++)
			constraintPartition[j] = 0;

		for (j=k=0; j<numTaxa; j++)
			{
			if (taxaInfo[j].isDeleted == YES)
				continue;
			if (taxaInfo[j].constraints[a] == 1)
				SetBit(k,constraintPartition);
			k++;
			}

		/* make sure outgroup is outside constrained partition if unrooted tree */
		if (t->isRooted == NO && IsBitSet(localOutGroup, constraintPartition))
			FlipBits(constraintPartition, nLongsNeeded, mask);

		/* find the locked node */
		for (i=j=0; i<t->nNodes; i++)
			{
			if (t->allDownPass[i]->isLocked == YES && t->allDownPass[i]->lockID == a)
				{
				p = t->allDownPass[i];
				j++;
				}
			}
	
		if (j != 1)
			{
			MrBayesPrint ("%s   Lock number %d is not set or set several places\n", spacer, a);
			goto errorExit;
			}

		/* check that locked node is correct */
		for (i=0; i<nLongsNeeded; i++)
			{
			if (p->partition[i] != constraintPartition[i]) 
				{
				MrBayesPrint ("%s   Lock number %d is set for the wrong node\n", spacer, a);
				goto errorExit;
				}
			}

		}
	
	/* exit */
	free (bitsets);
	return NO_ERROR;

errorExit:
	free (bitsets);
	return ERROR;
}




/*-----------------------------------------------------------
|
|   CheckExpandedModels: check data partitions that have
|   the codon or doublet model specified
|
-------------------------------------------------------------*/
int CheckExpandedModels (void)

{

	int				c, d, i, t, s, s1, s2, s3, whichNuc, uniqueId, numCharsInPart, 
					firstChar, lastChar, contiguousPart, badBreak, badExclusion,
					nGone, nuc1, nuc2, nuc3, foundStopCodon, posNucs1[4], posNucs2[4], posNucs3[4],
					oneGoodCodon, foundUnpaired, nPair, allCheckedOut;
	char			tempStr[100];
	ModelParams		*mp;
	
	/* first, set charId to 0 for all characters */
	for (i=0; i<numChar; i++)
		charInfo[i].charId = 0;
	
	/* loop over partitions */
	allCheckedOut = 0;
	uniqueId = 1;
	for (d=0; d<numCurrentDivisions; d++)
		{
		mp = &modelParams[d];
		
		if (mp->dataType == DNA || mp->dataType == RNA)
			{
			if (!strcmp(mp->nucModel,"Codon"))
				{
				/* start check that the codon model is appropriate for this partition */
				
				/* find first character in this partition */
				for (c=0; c<numChar; c++)
					{
					if (charInfo[c].partitionId[partitionNum-1] == d+1)
						break;
					}
				firstChar = c;
				/*printf ("   first character = %d\n", firstChar);*/
				
				/* find last character in this partition */
				for (c=numChar-1; c>=0; c--)
					{
					if (charInfo[c].partitionId[partitionNum-1] == d+1)
						break;
					}
				lastChar = c;
				/*printf ("   last character = %d\n", lastChar);*/
				
				/* check that the number of characters in partition is divisible by 3 */
				numCharsInPart = 0;
				for (c=0; c<numChar; c++)
					{
					if (charInfo[c].partitionId[partitionNum-1] != d+1)
						continue;
					numCharsInPart++;
					}
				/*printf ("   numCharsInPart = %d\n", numCharsInPart);*/
				if (numCharsInPart % 3 != 0)
					{
					if (numCurrentDivisions == 1)
						{
						MrBayesPrint ("%s   The number of characters is not divisible by three.\n", spacer);
						MrBayesPrint ("%s   You specified a codon model which requires triplets\n", spacer);
						MrBayesPrint ("%s   (codons) as the input. However, you only have %d \n", spacer, numCharsInPart);
						MrBayesPrint ("%s   characters.  \n", spacer);
						}
					else
						{
						MrBayesPrint ("%s   The number of characters in partition %d is not\n", spacer, d+1);
						MrBayesPrint ("%s   divisible by three. You specified a codon model\n", spacer);
						MrBayesPrint ("%s   which requires triplets (codons) as the input. \n", spacer);
						MrBayesPrint ("%s   However, you only have %d characters in this  \n", spacer, numCharsInPart);
						MrBayesPrint ("%s   partition \n", spacer);
						}
					return (ERROR);
					}
				
				/* check that all of the characters in the partition are contiguous */
				contiguousPart = YES;
				for (c=firstChar; c<=lastChar; c++)
					{
					if (charInfo[c].partitionId[partitionNum-1] != d+1)
						contiguousPart = NO;
					}
				if (contiguousPart == NO)
					{
					MrBayesPrint ("%s   Partition %d is not contiguous. You specified that\n", spacer, d+1);
					MrBayesPrint ("%s   a codon model be used for this partition. However, there\n", spacer);
					MrBayesPrint ("%s   is another partition that is between some of the characters\n", spacer);
					MrBayesPrint ("%s   in this partition. \n", spacer);
					return (ERROR);
					}
					
				/* check that there is not a break inside a triplet of characters */
				badBreak = NO;
				whichNuc = 0;
				for (c=firstChar; c<=lastChar; c++)
					{
					whichNuc++;
					if (charInfo[c].bigBreakAfter == YES && whichNuc != 3)
						badBreak = YES;
					if (whichNuc == 3)
						whichNuc = 0;
					}
				if (badBreak == YES)
					{
					MrBayesPrint ("%s   You specified a databreak inside of a coding triplet.\n", spacer);
					MrBayesPrint ("%s   This is a problem, as you imply that part of the codon\n", spacer);
					MrBayesPrint ("%s   lies in one gene and the remainder in another gene. \n", spacer);
					return (ERROR);
					}

				/* make certain excluded characters are in triplets */
				badExclusion = NO;
				whichNuc = nGone = 0;
				for (c=firstChar; c<=lastChar; c++)
					{
					whichNuc++;
					if (charInfo[c].isExcluded == YES)
						nGone++;
					if (whichNuc == 3)
						{
						if (nGone == 1 || nGone == 2)
							badExclusion = YES;
						whichNuc = nGone = 0;
						}
					}
				if (badExclusion == YES)
					{
					MrBayesPrint ("%s   In excluding characters, you failed to remove all of the\n", spacer);
					MrBayesPrint ("%s   sites of at least one codon. If you exclude sites, make \n", spacer);
					MrBayesPrint ("%s   certain to exclude all of the sites in the codon(s). \n", spacer);
					return (ERROR);
					}
				
				/* check that there are no stop codons */
				foundStopCodon = NO;
				for (c=firstChar; c<=lastChar; c+=3)
					{
					if (charInfo[c].isExcluded == NO)
						{
						for (t=0; t<numTaxa; t++)
							{
							if (taxaInfo[t].isDeleted == YES)
								continue;
							nuc1 = matrix[pos(t,c+0,numChar)];
							nuc2 = matrix[pos(t,c+1,numChar)];
							nuc3 = matrix[pos(t,c+2,numChar)];
							GetPossibleNucs (nuc1, posNucs1);
							GetPossibleNucs (nuc2, posNucs2);
							GetPossibleNucs (nuc3, posNucs3);
							
							oneGoodCodon = NO;
							s = 0;
							for (s1=0; s1<4; s1++)
								{
								for (s2=0; s2<4; s2++)
									{
									for (s3=0; s3<4; s3++)
										{
										if (posNucs1[s1] == 1 && posNucs2[s2] == 1 && posNucs3[s3] == 1)
											{
											if (mp->codon[s1*16 + s2*4 + s3] != 21)
												oneGoodCodon = YES;
											}
										s++;
										}
									}
								}
							if (oneGoodCodon == NO)
								{
								foundStopCodon = YES;
								if (GetNameFromString (taxaNames, tempStr, t+1) == ERROR)
									{
									MrBayesPrint ("%s   Could not find taxon %d\n", spacer, i+1);
									return (ERROR);
									}
								MrBayesPrint ("%s   Stop codon: taxon %s, sites %d to %d (%c%c%c, %s code)\n", spacer, 
									tempStr, c+1, c+3, WhichNuc (nuc1), WhichNuc (nuc2), WhichNuc (nuc3), mp->geneticCode);
								}
							}
						}
					}				
				if (foundStopCodon == YES)
					{
					MrBayesPrint ("%s   At least one stop codon was found. Stop codons are not\n", spacer);
					MrBayesPrint ("%s   allowed under the codon models.  \n", spacer);
					return (ERROR);
					}
				
				/* everything checks out. Now we can initialize charId */
				whichNuc = 0;
				for (c=firstChar; c<=lastChar; c++)
					{
					whichNuc++;
					charInfo[c].charId = uniqueId;
					if (whichNuc == 3)
						{
						whichNuc = 0;
						uniqueId++;
						}
					}
				
				allCheckedOut++;
				/* end check that the codon model is appropriate for this partition */
				}
			else if (!strcmp(mp->nucModel,"Doublet"))
				{
				/* start check that the doublet model is appropriate for this partition */
				
				/* Check that pairsId does not equal 0 for any of the characters in
				   the partition. If it does, then this means that at least one 
				   site was not appropriately paired. Remember, that pairsId is
				   initialized 1, 2, 3, ... for the first pair, second pair, etc. 
				   Also, check that every pair is only represented two times. */
				foundUnpaired = NO;
				for (c=0; c<numChar; c++)
					{
					if (charInfo[c].partitionId[partitionNum-1] == d+1 && charInfo[c].pairsId == 0 && charInfo[c].isExcluded == NO)
						foundUnpaired = YES;
					}
					
				for (c=0; c<numChar; c++)
					{
					if (charInfo[c].partitionId[partitionNum-1] == d+1 && charInfo[c].isExcluded == NO)
						{
						nPair = 1;
						for (i=0; i<numChar; i++)
							{
							if (i != c && charInfo[i].partitionId[partitionNum-1] == d+1 && charInfo[i].isExcluded == NO && charInfo[c].pairsId == charInfo[i].pairsId)
								nPair++;
							}
						if (nPair != 2)
							foundUnpaired = YES;
						}
					}
				if (foundUnpaired == YES)
					{
					if (numCurrentDivisions == 1)
						{
						MrBayesPrint ("%s   Found unpaired nucleotide sites. The doublet model\n", spacer);
						MrBayesPrint ("%s   requires that all sites are paired. \n", spacer);
						}
					else
						{
						MrBayesPrint ("%s   Found unpaired nucleotide sites in partition %d.\n", spacer, d+1);
						MrBayesPrint ("%s   The doublet model requires that all sites are paired. \n", spacer);
						}
					return (ERROR);
					}

				/* everything checks out. Now we can initialize charId */
				for (c=0; c<numChar; c++)
					{
					nuc1 = nuc2 = -1;
					if (charInfo[c].partitionId[partitionNum-1] == d+1 && charInfo[c].charId == 0)
						{
						nuc1 = c;
						for (i=0; i<numChar; i++)
							{
							if (i != c && charInfo[i].charId == 0 && charInfo[c].pairsId == charInfo[i].pairsId)
								nuc2 = i;
							}
						if (nuc1 >= 0 && nuc2 >= 0)
							{
							charInfo[nuc1].charId = charInfo[nuc2].charId = uniqueId;
							uniqueId++;
							}
						else
							{
							MrBayesPrint ("%s   Weird doublet problem in partition %d.\n", spacer, d+1);
							return (ERROR);
							}
						}
					}
					
				allCheckedOut++;
				/* end check that the doublet model is appropriate for this partition */
				}
			}
		}
		
	if (allCheckedOut > 0)
		MrBayesPrint ("%s   Codon/Doublet models successfully checked\n", spacer);

		
#	if 0
	for (c=0; c<numChar; c++)
			printf (" %d", charId[c]);
	printf ("\n");
#	endif
		
	return (NO_ERROR);
	
}





/*----------------------------------------------------------------
|
|	CheckSetConstraints: Check and set tree constraints
|
----------------------------------------------------------------*/
int CheckSetConstraints (Tree *t)

{

	int				a, i, j, k, nLongsNeeded, foundIt;
	long int		*constraintPartition, *mask, *bitsets;
	TreeNode		*p;
	   	
	/* allocate space */
	nLongsNeeded = (numLocalTaxa / nBitsInALong) + 1;
	bitsets = (long int *) calloc (2*nLongsNeeded*numLocalTaxa + 2, sizeof(long int));
	if (!bitsets)
		{
		MrBayesPrint ("%s   Problems allocating bitsets in CheckSetConstraints", spacer);
		return ERROR;
		}

	/* set partition pointers */
	for (i=0; i<t->nNodes; i++) 
		{
		p = t->allDownPass[i];
		p->partition = bitsets + i*nLongsNeeded;
		}
	constraintPartition = bitsets + (t->nNodes * nLongsNeeded);
	mask = bitsets + ((t->nNodes + 1) * nLongsNeeded);

	/* calculate mask (needed to take care of unused bits when flipping partitions) */
	for (i=0; i<(nBitsInALong - (numLocalTaxa%nBitsInALong)); i++)
		SetBit (i+numLocalTaxa, mask); 
	
	/* set partition specifiers for terminals */
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->left == NULL || p->right == NULL)
			SetBit(p->index, p->partition);
		}

	/* set partition specifiers for interior nodes */
	for (i=0; i<t->nIntNodes; i++)
		{
		p = t->intDownPass[i];
		for (j=0; j<nLongsNeeded; j++)
			p->partition[j] = p->left->partition[j] | p->right->partition[j];
		}

	/* this was "a,t->nConstraints" I cannot believe that is correct.
	   not sure if this is?*/
	for (a=0; a<t->nConstraints; a++)
		{

		/* set bits in partition to add */
		for (j=0; j<nLongsNeeded; j++)
			constraintPartition[j] = 0;
		for (j=k=0; j<numTaxa; j++)
			{
			if (taxaInfo[j].isDeleted == YES)
				continue;
			if (taxaInfo[j].constraints[0] == 1)
				SetBit(k,constraintPartition);
			k++;
			}

		/* make sure outgroup is outside constrained partition (marked 0) */
		if (IsBitSet(localOutGroup, constraintPartition))
			FlipBits(constraintPartition, nLongsNeeded, mask);

		/* find the node that should be locked */
		foundIt = NO;
		for (i=0; i<t->nIntNodes; i++)
			{
			p = t->intDownPass[i];
			for (j=0; j<nLongsNeeded; j++)
				{
				if (p->partition[j] != constraintPartition[j])
					break;
				}

			if (j == nLongsNeeded)
				{
				foundIt = YES;
				p->lockID = a;
				break;
				}				
			}
	
		if (foundIt == NO)
			{
			MrBayesPrint ("%s   User tree breaks constraint %d\n", spacer, a);
			goto errorExit;
			}
		}
	
	/* exit */
	free (bitsets);
	return NO_ERROR;
errorExit:
	free (bitsets);
	return ERROR;
}




int CheckTemperature (void)

{

	if (chainParams.userDefinedTemps == YES)
		{ /*chainParams.userTemps[0] != 1.0*/
		  if (fabs(chainParams.userTemps[0]-1.0)>ETA)
			{
			MrBayesPrint ("%s   The first user-defined temperature must be 1.0.\n", spacer);
			return (ERROR);
			}
		}

	return (NO_ERROR);
	
}




void CloseMBPrintFiles (void)
{
	int		i, k, n;

	for (n=0; n<chainParams.numRuns; n++)
		{
#		if defined (MPI_ENABLED)
		if (proc_id == 0)
			{
#		endif
		k = n;

		if (fpParm[k])
			fclose (fpParm[k]);
		fpParm[k] = NULL;

		for (i=0; i<numTrees; i++)
			{
			if (fpTree[k][i])
				{
				fprintf (fpTree[k][i], "end;\n");
				fclose (fpTree[k][i]);
				}
			fpTree[k][i] = NULL;
			}

		for (i=0; i<numCalibratedTrees; i++)
			{
			if (fpCal[k][i])
				{
				fprintf (fpCal[k][i], "end;\n");
				fclose (fpCal[k][i]);
				}
			fpCal[k][i] = NULL;
			}
#		if defined (MPI_ENABLED)
			}
#		endif
		}

#	if defined (MPI_ENABLED)
	if (proc_id != 0)
		return;
#	endif

	if (chainParams.numRuns > 1 && chainParams.mcmcDiagn == YES)
		{
		if (fpMcmc)
			fclose (fpMcmc);
		fpMcmc = NULL;
		}
		
}



/* CompactTree: prune partition tree */
PFNODE *CompactTree (PFNODE *p)
{
	int			i, j;
	PFNODE		*q, *r;

	if (p == NULL)
		return NULL;
	
	i = j = 0;
	if (IsPFNodeEmpty(p) == YES)
		{
		/* steal info from terminal on the way up */
		q = SmallestNonemptyPFNode (p->left, &i, 0);
		r = LargestNonemptyPFNode (p->right, &j, 0);

		if (q != NULL || r != NULL)
			{
			if (i < j)
				q = r;
		
			for (i=0; i<chainParams.numRuns; i++)
				{
				p->count[i] = q->count[i];
				q->count[i] = 0;
				}
			for (i=0; i<nLongsNeeded; i++)
				p->partition[i] = q->partition[i];
			}
		}

	p->left = CompactTree (p->left);
	p->right = CompactTree (p->right);

	/* delete on the way down if empty */
	if (IsPFNodeEmpty(p) == YES)
		{
		Tfree (p);
		return NULL;
		}
	else
		return p;
}





/*-----------------------------------------------------------
|
|   CompressData: compress original data matrix
|
-------------------------------------------------------------*/
int CompressData (void)

{

	int				a, c, d, i, j, k, t, col[3], isSame, newRow, newColumn,
					*isTaken, *tempSitesOfPat, *tempChar;
	long			*tempMatrix;
	ModelInfo		*m;
	ModelParams		*mp;

#	if defined DEBUG_COMPRESSDATA
	if (PrintMatrix() == ERROR)
		goto errorExit;
	getchar();
#	endif

	/* set all pointers that will be allocated locally to NULL */
	isTaken = NULL;
	tempMatrix = NULL;
	tempSitesOfPat = NULL;
	tempChar = NULL;

	/* allocate indices pointing from original to compressed matrix */
	if (memAllocs[ALLOC_COMPCOLPOS] == YES)
		{
		MrBayesPrint ("%s   compColPos not free in CompressData\n", spacer);
		goto errorExit;
		}
	compColPos = (int *)malloc((size_t) (numChar * sizeof(int)));
	if (!compColPos)
		{
		MrBayesPrint ("%s   Problem allocating compColPos (%d)\n", spacer, numChar * sizeof(int));
		goto errorExit;
		}
	for (i=0; i<numChar; i++)
		compColPos[i] = 0;
	memAllocs[ALLOC_COMPCOLPOS] = YES;

	if (memAllocs[ALLOC_COMPCHARPOS] == YES)
		{
		MrBayesPrint ("%s   compCharPos not free in CompressData\n", spacer);
		goto errorExit;
		}
	compCharPos = (int *)malloc((size_t) (numChar * sizeof(int)));
	if (!compCharPos)
		{
		MrBayesPrint ("%s   Problem allocating compCharPos (%d)\n", spacer, numChar * sizeof(int));
		goto errorExit;
		}
	for (i=0; i<numChar; i++)
		compCharPos[i] = 0;
	memAllocs[ALLOC_COMPCHARPOS] = YES;

	/* allocate space for temporary matrix, tempSitesOfPat,             */
	/* vector keeping track of whether a character has been compressed, */
	/* and vector indexing first original char for each compressed char */
	tempMatrix = (long *) calloc (numLocalTaxa * numLocalChar, sizeof(long));
	tempSitesOfPat = (int *) calloc (numLocalChar, sizeof(int));
	isTaken = (int *) calloc (numChar, sizeof(int));
	tempChar = (int *) calloc (numLocalChar, sizeof(int));
	if (!tempMatrix || !tempSitesOfPat || !isTaken || !tempChar)
		{
		MrBayesPrint ("%s   Problem allocating temporary variables in CompressData\n", spacer);
		goto errorExit;
		}

	/* set index to first empty column in temporary matrix */
	newColumn = 0;

	/* initialize number of compressed characters */
	numCompressedChars = 0;

	/* sort and compress data */
	for (d=0; d<numCurrentDivisions; d++)
		{
		MrBayesPrint ("%s   Compressing data matrix for division %d\n", spacer, d+1);

		/* set pointers to the model params and settings for this division */
		m = &modelSettings[d];
		mp = &modelParams[d];

		/* set column offset for this division in compressed matrix */
		m->compMatrixStart = newColumn;

		/* set compressed character offset for this division */
		m->compCharStart = numCompressedChars;

		/* set number of compressed characters to 0 for this division */
		m->numChars = 0;

		/* set number of uncompressed characters to 0 for this division */
		m->numUncompressedChars = 0;

		/* find the number of original characters per model site */
		m->nCharsPerSite = 1;
		if (mp->dataType == DNA || mp->dataType == RNA)
			{	
			if (!strcmp(mp->nucModel, "Doublet"))
				m->nCharsPerSite = 2;
			if (!strcmp(mp->nucModel, "Codon"))
				m->nCharsPerSite = 3;
			}
		
		/* sort and compress the characters for this division */
		for (c=0; c<numChar; c++)
			{
			if (charInfo[c].isExcluded == YES || charInfo[c].partitionId[partitionNum-1] != d+1 || isTaken[c] == YES)
				continue;

			col[0] = c;
			isTaken[c] = YES;
			
			/* find additional columns if more than one character per model site      */
			/* return error if the number of matching characters is smaller or larger */
			/* than the actual number of characters per model site                    */
			if (m->nCharsPerSite > 1)
				{
				j = 1;
				if (charInfo[c].charId == 0)
					{
					MrBayesPrint("%s   Character %d is not properly defined\n", spacer, c+1);
					goto errorExit;
					}
				for (i=c+1; i<numChar; i++)
					{
					if (charInfo[i].charId == charInfo[c].charId)
						{
						if (j >= m->nCharsPerSite)
							{
							MrBayesPrint("%s   Too many matches in charId (division %d char %d)\n", spacer, d, numCompressedChars);
							goto errorExit;
							}
						else
							{
							col[j++] = i;
							isTaken[i] = YES;
							}
						}
					}
				if (j != m->nCharsPerSite)
					{
					MrBayesPrint ("%s   Too few matches in charId (division %d char %d)\n", spacer, d, numCompressedChars);
					goto errorExit;
					}
				}
			
			/* add character to temporary matrix in column(s) at newColumn */
			for (t=newRow=0; t<numTaxa; t++)
				{
				if (taxaInfo[t].isDeleted == YES)
					continue;

				for (k=0; k<m->nCharsPerSite; k++)
					{
					tempMatrix[pos(newRow,newColumn+k,numLocalChar)] = matrix[pos(t,col[k],numChar)];
					}
				newRow++;
				}
			
			/* is it unique? */
			isSame = NO;
			if (mp->dataType != CONTINUOUS)
				{
				for (i=m->compMatrixStart; i<newColumn; i+=m->nCharsPerSite)
					{
					isSame = YES;
					for (j=0; j<numLocalTaxa; j++)
						for (k=0; k<m->nCharsPerSite; k++)
							if (tempMatrix[pos(j,newColumn+k,numLocalChar)] != tempMatrix[pos(j,i+k,numLocalChar)])
								{
								isSame = NO;
								break;
								}
					if (isSame == YES)
						break;
					}
				}

			/* if subject to data augmentation, it is always unique */
			if (!strcmp(mp->augmentData, "Yes"))
				{
				for (k=0; k<m->nCharsPerSite; k++)
					{
					if (charInfo[col[k]].isMissAmbig == YES)
						isSame = NO;
					}
				}

			m->numUncompressedChars += m->nCharsPerSite;			
			if (isSame == NO)
				{
				/* if it is unique then it should be added */
				tempSitesOfPat[numCompressedChars] = 1;
				for (k=0; k<m->nCharsPerSite; k++)
					{
					compColPos[col[k]] = newColumn + k;
					compCharPos[col[k]] = numCompressedChars;
					tempChar[newColumn + k] = col[k];
					}
				newColumn+=m->nCharsPerSite;
				m->numChars++;
				numCompressedChars++;
				}
			else
				{
				/* it it is not unique then simply update tempSitesOfPat     */
				/* calculate compressed character position and put it into a */
				/* (i points to compressed column position)                  */
				a = m->compCharStart + ((i - m->compMatrixStart) / m->nCharsPerSite);
				tempSitesOfPat[a]++;
				for (k=0; k<m->nCharsPerSite; k++)
					{
					compColPos[col[k]] = i;
					compCharPos[col[k]] = a;
					/* tempChar (pointing from compressed to uncompresed) */
					/* can only be set for first pattern */
					}
				}
			}	/* next character */
			
		/* check that the partition has at least a single character */
		if (m->numChars <= 0)
			{
			MrBayesPrint ("%s   You must have at least one site in a partition. Partition %d\n", spacer, d+1);
			MrBayesPrint ("%s   has %d site patterns.\n", spacer, m->numChars);
			goto errorExit;
			}

		MrBayesPrint("%s   Division %d has %d unique site patterns\n", spacer, d+1, m->numChars);

		m->compCharStop = m->compCharStart + m->numChars;
		m->compMatrixStop = newColumn;

		} /* next division */

	compMatrixRowSize = newColumn;

	/* now we know the size, so we can allocate space for the compressed matrix ... */
	if (memAllocs[ALLOC_COMPMATRIX] == YES)
		{
		MrBayesPrint ("%s   compMatrix not free in CompressData\n", spacer);
		goto errorExit;
		}
	compMatrix = (long *) calloc (compMatrixRowSize * numLocalTaxa, sizeof(long));
	if (!compMatrix)
		{
		MrBayesPrint ("%s   Problem allocating compMatrix (%d)\n", spacer, compMatrixRowSize * numLocalTaxa * sizeof(long));
		goto errorExit;
		}
	memAllocs[ALLOC_COMPMATRIX] = YES;
	
	if (memAllocs[ALLOC_NUMSITESOFPAT] == YES)
		{
		MrBayesPrint ("%s   numSitesOfPat not free in CompressData\n", spacer);
		goto errorExit;
		}
	numSitesOfPat = (CLFlt *) calloc (numCompressedChars, sizeof(CLFlt));
	if (!numSitesOfPat)
		{
		MrBayesPrint ("%s   Problem allocating numSitesOfPat (%d)\n", spacer, numCompressedChars * sizeof(MrBFlt));
		goto errorExit;
		}
	memAllocs[ALLOC_NUMSITESOFPAT] = YES;

	if (memAllocs[ALLOC_ORIGCHAR] == YES)
		{
		MrBayesPrint ("%s   origChar not free in CompressData\n", spacer);
		goto errorExit;
		}
	origChar = (int *)malloc((size_t) (compMatrixRowSize * sizeof(int)));
	if (!origChar)
		{
		MrBayesPrint ("%s   Problem allocating originalChar (%d)\n", spacer, numCompressedChars * sizeof(int));
		goto errorExit;
		}
	memAllocs[ALLOC_ORIGCHAR] = YES;

	/* ... and copy the data there */
	for (i=0; i<numLocalTaxa; i++)
		for (j=0; j<compMatrixRowSize; j++)
			compMatrix[pos(i,j,compMatrixRowSize)] = tempMatrix[pos(i,j,numLocalChar)];

	for (i=0; i<numCompressedChars; i++)
		numSitesOfPat[i] = (CLFlt) tempSitesOfPat[i];

	for (i=0; i<compMatrixRowSize; i++)
		origChar[i] = tempChar[i];

#	if defined (DEBUG_COMPRESSDATA)
	if (PrintCompMatrix() == ERROR)
		goto errorExit;
	getchar();
#	endif

	/* free the temporary variables */
	free (tempSitesOfPat);
	free (tempMatrix);
	free (isTaken);
	free (tempChar);

	return NO_ERROR;

	errorExit:
		if (tempMatrix)
		    free (tempMatrix);
		if (tempSitesOfPat)
			free (tempSitesOfPat);
		if (isTaken)
			free (isTaken);
		if (tempChar)
			free (tempChar);

		return ERROR;	
		
}





/*----------------------------------------------------------------
|
|	CondLikeDown_Bin: binary model with or without rate
|		variation
|
-----------------------------------------------------------------*/
int CondLikeDown_Bin (TreeNode *p, int division, int chain)

{

	int				c, k;
	CLFlt			*clL, *clR, *clP, *pL, *pR, *tiPL, *tiPR;
	ModelInfo		*m;
	
	/* find model settings for this division */
	m = &modelSettings[division];

	/* flip state of node so that we are not overwriting old cond likes */
	FlipOneBit (division, &p->clSpace[0]);
	
	/* find conditional likelihood pointers */
	clL = condLikePtr[chain][p->left->index ] + m->condLikeStart + Bit(division, &p->left->clSpace[0] ) * condLikeRowSize;
	clR = condLikePtr[chain][p->right->index] + m->condLikeStart + Bit(division, &p->right->clSpace[0]) * condLikeRowSize;
	clP = condLikePtr[chain][p->index       ] + m->condLikeStart + Bit(division, &p->clSpace[0]       ) * condLikeRowSize;
	
	/* find transition probabilities */
	pL = tiProbs[chain] + m->tiProbStart + (2*p->left->index  + Bit(division, &p->left->tiSpace[0] )) * tiProbRowSize;
	pR = tiProbs[chain] + m->tiProbStart + (2*p->right->index + Bit(division, &p->right->tiSpace[0])) * tiProbRowSize;

	for (c=0; c<m->numChars; c++)
		{
		tiPL = pL;
		tiPR = pR;
		for (k=0; k<m->numGammaCats; k++)
			{
			*(clP++) = (tiPL[0]*clL[0] + tiPL[1]*clL[1])
					  *(tiPR[0]*clR[0] + tiPR[1]*clR[1]);
			*(clP++) = (tiPL[2]*clL[0] + tiPL[3]*clL[1])
					  *(tiPR[2]*clR[0] + tiPR[3]*clR[1]);
			tiPR += 4;
			tiPL += 4;
			clL += 2;
			clR += 2;
			}
		}

	return NO_ERROR;
	
}




/*----------------------------------------------------------------
|
|	CondLikeDown_Gen: general n-state model with or without rate
|		variation
|
-----------------------------------------------------------------*/
int CondLikeDown_Gen (TreeNode *p, int division, int chain)

{

	int				a, b, c, h, i, j, k, shortCut, *lState=NULL, *rState=NULL, catStart,
					nObsStates, nStates, nStatesSquared, preLikeJump;
	CLFlt			likeL, likeR, *pL, *pR, *tiPL, *tiPR, *clL, *clR, *clP;
	ModelInfo		*m;
	
	/* find model settings for this division and nStates, nStatesSquared */
	m = &modelSettings[division];
	nObsStates = m->numStates;
	nStates = m->numModelStates;
	nStatesSquared = nStates * nStates;
	preLikeJump = nObsStates * nStates;

	/* flip state of node so that we are not overwriting old cond likes */
	FlipOneBit (division, &p->clSpace[0]);
	
	/* find conditional likelihood pointers */
	clL = condLikePtr[chain][p->left->index ] + m->condLikeStart + Bit(division, &p->left->clSpace[0] ) * condLikeRowSize;
	clR = condLikePtr[chain][p->right->index] + m->condLikeStart + Bit(division, &p->right->clSpace[0]) * condLikeRowSize;
	clP = condLikePtr[chain][p->index       ] + m->condLikeStart + Bit(division, &p->clSpace[0]       ) * condLikeRowSize;
	
	/* find transition probabilities (or calculate instead) */
	pL = tiProbs[chain] + m->tiProbStart + (2*p->left->index  + Bit(division, &p->left->tiSpace[0] )) * tiProbRowSize;
	pR = tiProbs[chain] + m->tiProbStart + (2*p->right->index + Bit(division, &p->right->tiSpace[0])) * tiProbRowSize;

	/* find likelihoods of site patterns for left branch if terminal */
	shortCut = 0;
#	if !defined (DEBUG_NOSHORTCUTS)
	if (p->left->left == NULL && m->isPartAmbig[p->left->index] == NO)
		{
		shortCut |= 1;
		lState = termState + m->compCharStart + p->left->index * numCompressedChars;
		tiPL = pL;
		for (k=a=0; k<m->numGammaCats; k++)
			{
			catStart = a;
			for (i=0; i<nObsStates; i++)
				for (j=i; j<nStatesSquared; j+=nStates)
					preLikeL[a++] = tiPL[j];
			for (b=1; b<nStates/nObsStates; b++)
				{
				a = catStart;
				for (i=0; i<nObsStates; i++)
					{
					for (j=i+b*nObsStates; j<nStatesSquared; j+=nStates)
						preLikeL[a++] += tiPL[j];
					}
				}
			/* for ambiguous */
			for (i=0; i<nStates; i++)
				preLikeL[a++] = 1.0;
			tiPL += nStatesSquared;
			}
		}

	/* find likelihoods of site patterns for right branch if terminal */
	if (p->right->left == NULL && m->isPartAmbig[p->right->index] == NO)
		{
		shortCut |= 2;
		rState = termState + m->compCharStart + p->right->index * numCompressedChars;
		tiPR = pR;
		for (k=a=0; k<m->numGammaCats; k++)
			{
			catStart = a;
			for (i=0; i<nObsStates; i++)
				for (j=i; j<nStatesSquared; j+=nStates)
					preLikeR[a++] = tiPR[j];
			for (b=1; b<nStates/nObsStates; b++)
				{
				a = catStart;
				for (i=0; i<nObsStates; i++)
					{
					for (j=i+b*nObsStates; j<nStatesSquared; j+=nStates)
						preLikeR[a++] += tiPR[j];
					}
				}
			/* for ambiguous */
			for (i=0; i<nStates; i++)
				preLikeR[a++] = 1.0;
			tiPR += nStatesSquared;
			}
		}
#	endif

	switch (shortCut)
		{
		case 0:
			for (c=0; c<m->numChars; c++)
				{
				tiPL = pL;
				tiPR = pR;
				for (k=h=0; k<m->numGammaCats; k++)
					{
					for (i=0; i<nStates; i++)
						{
						likeL = likeR = 0.0;
						for (j=0; j<nStates; j++)
							{
							likeL += tiPL[h]   * clL[j];
							likeR += tiPR[h++] * clR[j];
							}
						*(clP++) = likeL * likeR;
						}
					clL += nStates;
					clR += nStates;
					}
				}
			break;
		case 1:
			for (c=0; c<m->numChars; c++)
				{
				tiPR = pR;
				a = lState[c];
				for (k=h=0; k<m->numGammaCats; k++)
					{
					for (i=0; i<nStates; i++)
						{
						likeR = 0.0;
						for (j=0; j<nStates; j++)
							{
							likeR += tiPR[h++]*clR[j];
							}
						*(clP++) = preLikeL[a++] * likeR;
						}
					clR += nStates;
					a += preLikeJump;
					}
				}
			break;
		case 2:
			for (c=0; c<m->numChars; c++)
				{
				tiPL = pL;
				a = rState[c];
				for (k=h=0; k<m->numGammaCats; k++)
					{
					for (i=0; i<nStates; i++)
						{
						likeL = 0.0;
						for (j=0; j<nStates; j++)
							{
							likeL += tiPL[h++]*clL[j];
							}
						*(clP++) = preLikeR[a++] * likeL;
						}
					clL += nStates;
					a += preLikeJump;
					}
				}
			break;
		case 3:
			for (c=0; c<m->numChars; c++)
				{
				a = lState[c];
				b = rState[c];
				for (k=h=0; k<m->numGammaCats; k++)
					{
					for (i=0; i<nStates; i++)
						*(clP++) = preLikeL[a++]*preLikeR[b++];
					a += preLikeJump;
					b += preLikeJump;
					}
				}
			break;
		}

	return NO_ERROR;
	
}






/*----------------------------------------------------------------
|
|	CondLikeDown_NUC4: 4by4 nucleotide model with or without rate
|		variation
|
-----------------------------------------------------------------*/
int CondLikeDown_NUC4 (TreeNode *p, int division, int chain)

{
	int				c, h, i, j, k, shortCut, *lState=NULL, *rState=NULL;
	CLFlt			*clL, *clR, *clP, *pL, *pR, *tiPL, *tiPR;
	ModelInfo		*m;
	
	m = &modelSettings[division];

	/* flip state of node so that we are not overwriting old cond likes */
	FlipOneBit (division, &p->clSpace[0]);
	
	/* find conditional likelihood pointers */
	clL = condLikePtr[chain][p->left->index ] + m->condLikeStart + Bit(division, &p->left->clSpace[0] ) * condLikeRowSize;
	clR = condLikePtr[chain][p->right->index] + m->condLikeStart + Bit(division, &p->right->clSpace[0]) * condLikeRowSize;
	clP = condLikePtr[chain][p->index       ] + m->condLikeStart + Bit(division, &p->clSpace[0]       ) * condLikeRowSize;
	
	/* find transition probabilities (or calculate instead) */
	pL = tiProbs[chain] + m->tiProbStart + (2*p->left->index  + Bit(division, &p->left->tiSpace[0] )) * tiProbRowSize;
	pR = tiProbs[chain] + m->tiProbStart + (2*p->right->index + Bit(division, &p->right->tiSpace[0])) * tiProbRowSize;

	/* find likelihoods of site patterns for left branch if terminal */
	shortCut = 0;
#	if !defined (DEBUG_NOSHORTCUTS)
	if (p->left->left == NULL && m->isPartAmbig[p->left->index] == NO)
		{
		shortCut |= 1;
		lState = termState + m->compCharStart + p->left->index * numCompressedChars;
		tiPL = pL;
		for (k=j=0; k<m->numGammaCats; k++)
			{
			for (i=0; i<4; i++)
				{
				preLikeL[j++] = tiPL[0];
				preLikeL[j++] = tiPL[4];
				preLikeL[j++] = tiPL[8];
				preLikeL[j++] = tiPL[12];
				tiPL++;
				}
			/* for ambiguous */
			for (i=0; i<4; i++)
				preLikeL[j++] = 1.0;
			tiPL += 12;
			}
		}

	/* find likelihoods of site patterns for right branch if terminal */
	if (p->right->left == NULL && m->isPartAmbig[p->right->index] == NO)
		{
		shortCut |= 2;
		rState = termState + m->compCharStart + p->right->index * numCompressedChars;
		tiPR = pR;
		for (k=j=0; k<m->numGammaCats; k++)
			{
			for (i=0; i<4; i++)
				{
				preLikeR[j++] = tiPR[0];
				preLikeR[j++] = tiPR[4];
				preLikeR[j++] = tiPR[8];
				preLikeR[j++] = tiPR[12];
				tiPR++;
				}
			/* for ambiguous */
			for (i=0; i<4; i++)
				preLikeR[j++] = 1.0;
			tiPR += 12;
			}
		}
#	endif

	switch (shortCut)
		{
		case 0:
			for (c=h=0; c<m->numChars; c++)
				{
				tiPL = pL;
				tiPR = pR;
				for (k=0; k<m->numGammaCats; k++)
					{
					clP[h++] =   (tiPL[AA]*clL[A] + tiPL[AC]*clL[C] + tiPL[AG]*clL[G] + tiPL[AT]*clL[T])
								*(tiPR[AA]*clR[A] + tiPR[AC]*clR[C] + tiPR[AG]*clR[G] + tiPR[AT]*clR[T]);
					clP[h++] =   (tiPL[CA]*clL[A] + tiPL[CC]*clL[C] + tiPL[CG]*clL[G] + tiPL[CT]*clL[T])
								*(tiPR[CA]*clR[A] + tiPR[CC]*clR[C] + tiPR[CG]*clR[G] + tiPR[CT]*clR[T]);
					clP[h++] =   (tiPL[GA]*clL[A] + tiPL[GC]*clL[C] + tiPL[GG]*clL[G] + tiPL[GT]*clL[T])
								*(tiPR[GA]*clR[A] + tiPR[GC]*clR[C] + tiPR[GG]*clR[G] + tiPR[GT]*clR[T]);
					clP[h++] =   (tiPL[TA]*clL[A] + tiPL[TC]*clL[C] + tiPL[TG]*clL[G] + tiPL[TT]*clL[T])
								*(tiPR[TA]*clR[A] + tiPR[TC]*clR[C] + tiPR[TG]*clR[G] + tiPR[TT]*clR[T]);
					clL += 4;
					clR += 4;
					tiPL += 16;
					tiPR += 16;
					}
				}
			break;
		case 1:
			for (c=h=0; c<m->numChars; c++)
				{
				tiPR = pR;
				i = lState[c];
				for (k=0; k<m->numGammaCats; k++)
					{
					clP[h++] =   preLikeL[i++]
								*(tiPR[AA]*clR[A] + tiPR[AC]*clR[C] + tiPR[AG]*clR[G] + tiPR[AT]*clR[T]);
					clP[h++] =   preLikeL[i++]
								*(tiPR[CA]*clR[A] + tiPR[CC]*clR[C] + tiPR[CG]*clR[G] + tiPR[CT]*clR[T]);
					clP[h++] =   preLikeL[i++]
								*(tiPR[GA]*clR[A] + tiPR[GC]*clR[C] + tiPR[GG]*clR[G] + tiPR[GT]*clR[T]);
					clP[h++] =   preLikeL[i++]
								*(tiPR[TA]*clR[A] + tiPR[TC]*clR[C] + tiPR[TG]*clR[G] + tiPR[TT]*clR[T]);
					clR += 4;
					tiPR += 16;
					i += 16;
					}
				}
			break;
		case 2:
			for (c=h=0; c<m->numChars; c++)
				{
				tiPL = pL;
				i = rState[c];
				for (k=0; k<m->numGammaCats; k++)
					{
					clP[h++] =   (tiPL[AA]*clL[A] + tiPL[AC]*clL[C] + tiPL[AG]*clL[G] + tiPL[AT]*clL[T])
								*preLikeR[i++];
					clP[h++] =   (tiPL[CA]*clL[A] + tiPL[CC]*clL[C] + tiPL[CG]*clL[G] + tiPL[CT]*clL[T])
								*preLikeR[i++];
					clP[h++] =   (tiPL[GA]*clL[A] + tiPL[GC]*clL[C] + tiPL[GG]*clL[G] + tiPL[GT]*clL[T])
								*preLikeR[i++];
					clP[h++] =   (tiPL[TA]*clL[A] + tiPL[TC]*clL[C] + tiPL[TG]*clL[G] + tiPL[TT]*clL[T])
								*preLikeR[i++];
					clL += 4;
					tiPL += 16;
					i += 16;
					}
				}
			break;
		case 3:
			for (c=h=0; c<m->numChars; c++)
				{
				i = lState[c];
				j = rState[c];
				for (k=0; k<m->numGammaCats; k++)
					{
					clP[h++] =   preLikeL[i++]*preLikeR[j++];
					clP[h++] =   preLikeL[i++]*preLikeR[j++];
					clP[h++] =   preLikeL[i++]*preLikeR[j++];
					clP[h++] =   preLikeL[i++]*preLikeR[j++];
					i += 16;
					j += 16;
					}
				}
		}

	return NO_ERROR;
	
}





#if defined SSE
/*----------------------------------------------------------------
|
|	CondLikeDown_NUC4_SSE: 4by4 nucleotide model with or without rate
|		variation, using SSE instructions
|
-----------------------------------------------------------------*/
int CondLikeDown_NUC4_SSE (TreeNode *p, int division, int chain)

{
	int				c, k;
	__m128			*clL, *clR, *clP, *pL, *pR, *tiPL, *tiPR;
	__m128			m1, m2, m3, m4, m5, m6, m7, m8;
	ModelInfo		*m;
	
	m = &modelSettings[division];

	/* flip state of node so that we are not overwriting old cond likes */
	FlipOneBit (division, &p->clSpace[0]);
	
	/* find conditional likelihood pointers */
	clL = (__m128 *) (condLikePtr[chain][p->left->index ] + m->condLikeStart + Bit(division, &p->left->clSpace[0] ) * condLikeRowSize);
	clR = (__m128 *) (condLikePtr[chain][p->right->index] + m->condLikeStart + Bit(division, &p->right->clSpace[0]) * condLikeRowSize);
	clP = (__m128 *) (condLikePtr[chain][p->index       ] + m->condLikeStart + Bit(division, &p->clSpace[0]       ) * condLikeRowSize);
	
	/* find transition probabilities (or calculate instead) */
	pL = (__m128 *) (tiProbs[chain] + m->tiProbStart + (2*p->left->index  + Bit(division, &p->left->tiSpace[0] )) * tiProbRowSize);
	pR = (__m128 *) (tiProbs[chain] + m->tiProbStart + (2*p->right->index + Bit(division, &p->right->tiSpace[0])) * tiProbRowSize);

	for (c=0; c<m->numChars; c++)
		{
		tiPL = pL;
		tiPR = pR;
		for (k=0; k<m->numGammaCats; k++)
			{
			m1 = *clL;
			m2 = *clR;
			m3 = _mm_shuffle_ps (m1, m1, _MM_SHUFFLE (0, 3, 2, 1));
			m4 = _mm_shuffle_ps (m2, m2, _MM_SHUFFLE (0, 3, 2, 1));
			m5 = _mm_mul_ps (m1, tiPL[0]);
			m6 = _mm_mul_ps (m2, tiPR[0]);
			m7 = _mm_mul_ps (m3, tiPL[1]);
			m8 = _mm_mul_ps (m4, tiPR[1]);
			m7 = _mm_add_ps (m5, m7);
			m8 = _mm_add_ps (m6, m8);
			
			m1 = _mm_shuffle_ps (m3, m3, _MM_SHUFFLE (0, 3, 2, 1));
			m2 = _mm_shuffle_ps (m4, m4, _MM_SHUFFLE (0, 3, 2, 1));
			m3 = _mm_shuffle_ps (m1, m1, _MM_SHUFFLE (0, 3, 2, 1));
			m4 = _mm_shuffle_ps (m2, m2, _MM_SHUFFLE (0, 3, 2, 1));
			m5 = _mm_mul_ps (m1, tiPL[2]);
			m6 = _mm_mul_ps (m2, tiPR[2]);
			m7 = _mm_add_ps (m5, m7);
			m8 = _mm_add_ps (m6, m8);
			m5 = _mm_mul_ps (m3, tiPL[3]);
			m6 = _mm_mul_ps (m4, tiPR[3]);
			m7 = _mm_add_ps (m5, m7);
			m8 = _mm_add_ps (m6, m8);

			*clP = _mm_mul_ps (m7, m8);
			clP++;
			clL++;
			clR++;
			}
		}

	return NO_ERROR;
	
}
#endif




/*----------------------------------------------------------------
|
|	CondLikeDown_NY98: codon model with omega variation
|
-----------------------------------------------------------------*/
int CondLikeDown_NY98 (TreeNode *p, int division, int chain)

{

	int				a, b, c, h, i, j, k, shortCut, *lState=NULL, *rState=NULL, nStates, nStatesSquared;
	int                     nObsStates, preLikeJump; /* khs07062005 */

	CLFlt			likeL, likeR, *pL, *pR, *tiPL, *tiPR, *clL, *clR, *clP;
	ModelInfo		*m;
	
	/* find model settings for this division and nStates, nStatesSquared */
	m = &modelSettings[division];
	nObsStates = m->numStates; /* khs07062005: copied from CondLikeDown_Gen */
	nStates = m->numModelStates;
	nStatesSquared = nStates * nStates;
	preLikeJump = nObsStates * nStates; /* khs07062005: copied from CondLikeDown_Gen*/

	/* flip state of node so that we are not overwriting old cond likes */
	FlipOneBit (division, &p->clSpace[0]);
	
	/* find conditional likelihood pointers */
	clL = condLikePtr[chain][p->left->index ] + m->condLikeStart + Bit(division, &p->left->clSpace[0] ) * condLikeRowSize;
	clR = condLikePtr[chain][p->right->index] + m->condLikeStart + Bit(division, &p->right->clSpace[0]) * condLikeRowSize;
	clP = condLikePtr[chain][p->index       ] + m->condLikeStart + Bit(division, &p->clSpace[0]       ) * condLikeRowSize;
	
	/* find transition probabilities (or calculate instead) */
	pL = tiProbs[chain] + m->tiProbStart + (2*p->left->index  + Bit(division, &p->left->tiSpace[0] )) * tiProbRowSize;
	pR = tiProbs[chain] + m->tiProbStart + (2*p->right->index + Bit(division, &p->right->tiSpace[0])) * tiProbRowSize;

	/* find likelihoods of site patterns for left branch if terminal */
	shortCut = 0;
#	if !defined (DEBUG_NOSHORTCUTS)
	if (p->left->left == NULL && m->isPartAmbig[p->left->index] == NO)
		{
		shortCut |= 1;
		lState = termState + m->compCharStart + p->left->index * numCompressedChars;
		tiPL = pL;
		for (k=a=0; k<m->numOmegaCats; k++)
			{
			for (i=0; i<nStates; i++)
				for (j=i; j<nStatesSquared; j+=nStates)
					preLikeL[a++] = tiPL[j];
			/* for ambiguous */
			for (i=0; i<nStates; i++)
				preLikeL[a++] = 1.0;
			tiPL += nStatesSquared;
			}
		}

	/* find likelihoods of site patterns for right branch if terminal */
	if (p->right->left == NULL && m->isPartAmbig[p->right->index] == NO)
		{
		shortCut |= 2;
		rState = termState + m->compCharStart + p->right->index * numCompressedChars;
		tiPR = pR;
		for (k=a=0; k<m->numOmegaCats; k++)
			{
			for (i=0; i<nStates; i++)
				for (j=i; j<nStatesSquared; j+=nStates)
					preLikeR[a++] = tiPR[j];
			/* for ambiguous */
			for (i=0; i<nStates; i++)
				preLikeR[a++] = 1.0;
			tiPR += nStatesSquared;
			}
		}
#	endif

	switch (shortCut)
		{
		case 0:
			for (c=0; c<m->numChars; c++)
				{
				tiPL = pL;
				tiPR = pR;
				for (k=h=0; k<m->numOmegaCats; k++)
					{
					for (i=0; i<nStates; i++)
						{
						likeL = likeR = 0.0;
						for (j=0; j<nStates; j++)
							{
							likeL += tiPL[h]*clL[j];
							likeR += tiPR[h++]*clR[j];
							}
						*(clP++) = likeL * likeR;
						}
					clL += nStates;
					clR += nStates;
					}
				}
			break;
		case 1:
			for (c=0; c<m->numChars; c++)
				{
				tiPR = pR;
				a = lState[c];
				for (k=h=0; k<m->numOmegaCats; k++)
					{
					for (i=0; i<nStates; i++)
						{
						likeR = 0.0;
						for (j=0; j<nStates; j++)
							{
							likeR += tiPR[h++]*clR[j];
							}
						*(clP++) = preLikeL[a++] * likeR;
						}
					clR += nStates;
					a += preLikeJump; /* khs07062005, was a+=nStatesSquared;*/
					}
				}
			break;
		case 2:
			for (c=0; c<m->numChars; c++)
				{
				tiPL = pL;
				a = rState[c];
				for (k=h=0; k<m->numOmegaCats; k++)
					{
					for (i=0; i<nStates; i++)
						{
						likeL = 0.0;
						for (j=0; j<nStates; j++)
							{
							likeL += tiPL[h++]*clL[j];
							}
						*(clP++) = preLikeR[a++] * likeL;
						}
					clL += nStates;
					a += preLikeJump; /* khs07062005, was a+=nStatesSquared;*/
					}
				}
			break;
		case 3:
			for (c=0; c<m->numChars; c++)
				{
				a = lState[c];
				b = rState[c];
				for (k=h=0; k<m->numOmegaCats; k++)
					{
					for (i=0; i<nStates; i++)
						*(clP++) = preLikeL[a++]*preLikeR[b++];
					a += preLikeJump; /* khs07062005, was a+=nStatesSquared;*/
					b += preLikeJump; /* khs07062005, was a+=nStatesSquared;*/
					}
				}
			break;
		}

	return NO_ERROR;
	
}






/*----------------------------------------------------------------
|
|	CondLikeDown_Std: variable number of states model
|		with or without rate variation
|
-----------------------------------------------------------------*/
int CondLikeDown_Std (TreeNode *p, int division, int chain)

{

	int				a, c, h, i, j, k, nStates, nCats;
	CLFlt			*clL, *clR, *clP, *pL, *pR, *tiPL, *tiPR, likeL, likeR;
	ModelInfo		*m;
	
	m = &modelSettings[division];

	/* flip state of node so that we are not overwriting old cond likes */
	FlipOneBit (division, &p->clSpace[0]);
	
	/* find conditional likelihood pointers */
	clL = condLikePtr[chain][p->left->index]  + m->condLikeStart + Bit(division, &p->left->clSpace[0] ) * condLikeRowSize;
	clR = condLikePtr[chain][p->right->index] + m->condLikeStart + Bit(division, &p->right->clSpace[0]) * condLikeRowSize;;
	clP = condLikePtr[chain][p->index]        + m->condLikeStart + Bit(division, &p->clSpace[0]       ) * condLikeRowSize;
	
	/* find transition probabilities */
	pL = tiProbs[chain] + m->tiProbStart + (2*p->left->index  + Bit(division, &p->left->tiSpace[0] )) * tiProbRowSize;
	pR = tiProbs[chain] + m->tiProbStart + (2*p->right->index + Bit(division, &p->right->tiSpace[0])) * tiProbRowSize;

	/* calculate ancestral probabilities */
	for (c=h=0; c<m->numChars; c++)
		{
		tiPL = pL + m->tiIndex[c];
		tiPR = pR + m->tiIndex[c];
		nStates = m->nStates[c];
		
		/* the following lines ensure that nCats is 1 unless */
		/* the character is binary and beta categories are used  */
		if (nStates == 2)
			nCats = m->numBetaCats;
		else
			nCats = 1;

		/* now multiply with the gamma cats */
		nCats *= m->numGammaCats;
		for (k=j=0; k<nCats; k++)
			{
			for (a=0; a<nStates; a++)
				{
				likeL = likeR = 0.0;
				for (i=0; i<nStates; i++)
					{
					likeL += tiPL[j] * clL[i];
					likeR += tiPR[j++] * clR[i];
					}
				clP[h++] = likeL * likeR;
				}
			clL += nStates;
			clR += nStates;
			}
		}

	return NO_ERROR;
}





/*----------------------------------------------------------------
|
|	CondLikeRoot_Bin: binary model with or without rate
|		variation
|
-----------------------------------------------------------------*/
int CondLikeRoot_Bin (TreeNode *p, int division, int chain)

{

	int				c, k;
	CLFlt			*clL, *clR, *clP, *clA, *pL, *pR, *pA, *tiPL, *tiPR, *tiPA;
	ModelInfo		*m;

	/* find model settings for this division */
	m = &modelSettings[division];
	
	/* flip state of node so that we are not overwriting old cond likes */
	FlipOneBit (division, &p->clSpace[0]);
	
	/* find conditional likelihood pointers */
	clL = condLikePtr[chain][p->left->index ] + m->condLikeStart + Bit(division, &p->left->clSpace[0] ) * condLikeRowSize;
	clR = condLikePtr[chain][p->right->index] + m->condLikeStart + Bit(division, &p->right->clSpace[0]) * condLikeRowSize;
	clP = condLikePtr[chain][p->index       ] + m->condLikeStart + Bit(division, &p->clSpace[0]       ) * condLikeRowSize;
	clA = condLikePtr[chain][p->anc->index  ] + m->condLikeStart + Bit(division, &p->anc->clSpace[0]  ) * condLikeRowSize;
	
	/* find transition probabilities (or calculate instead) */
	pL = tiProbs[chain] + m->tiProbStart + (2*p->left->index  + Bit(division, &p->left->tiSpace[0] )) * tiProbRowSize;
	pR = tiProbs[chain] + m->tiProbStart + (2*p->right->index + Bit(division, &p->right->tiSpace[0])) * tiProbRowSize;
	pA = tiProbs[chain] + m->tiProbStart + (2*p->index        + Bit(division, &p->tiSpace[0]       )) * tiProbRowSize;

	for (c=0; c<m->numChars; c++)
		{
		tiPL = pL;
		tiPR = pR;
		tiPA = pA;
		for (k=0; k<m->numGammaCats; k++)
			{
			*(clP++) = (tiPL[0]*clL[0] + tiPL[1]*clL[1])
					  *(tiPR[0]*clR[0] + tiPR[1]*clR[1])
					  *(tiPA[0]*clA[0] + tiPA[1]*clA[1]);
			*(clP++) = (tiPL[2]*clL[0] + tiPL[3]*clL[1])
					  *(tiPR[2]*clR[0] + tiPR[3]*clR[1])
					  *(tiPA[2]*clA[0] + tiPA[3]*clA[1]);
			tiPR += 4;
			tiPL += 4;
			tiPA += 4;
			clL += 2;
			clR += 2;
			clA += 2;
			}
		}

	return NO_ERROR;
	
}





/*----------------------------------------------------------------
|
|	CondLikeRoot_Gen: general n-state model with or without rate
|		variation
|
-----------------------------------------------------------------*/
int CondLikeRoot_Gen (TreeNode *p, int division, int chain)

{

	int				a, b, c, h, i, j, k, shortCut, *lState=NULL, *rState=NULL, *aState=NULL,
					catStart, nObsStates, nStates, nStatesSquared, preLikeJump;
	CLFlt			likeL, likeR, likeA, *clL, *clR, *clP, *clA, *pL, *pR, *pA,
					*tiPL, *tiPR, *tiPA;
	ModelInfo		*m;
	
	/* find model settings for this division and nStates, nStatesSquared */
	m = &modelSettings[division];
	nObsStates = m->numStates;
	nStates = m->numModelStates;
	nStatesSquared = nStates * nStates;
	preLikeJump = nObsStates * nStates;

	/* flip state of node so that we are not overwriting old cond likes */
	FlipOneBit (division, &p->clSpace[0]);
	
	/* find conditional likelihood pointers */
	clL = condLikePtr[chain][p->left->index ] + m->condLikeStart + Bit(division, &p->left->clSpace[0] ) * condLikeRowSize;
	clR = condLikePtr[chain][p->right->index] + m->condLikeStart + Bit(division, &p->right->clSpace[0]) * condLikeRowSize;
	clP = condLikePtr[chain][p->index       ] + m->condLikeStart + Bit(division, &p->clSpace[0]       ) * condLikeRowSize;
	clA = condLikePtr[chain][p->anc->index  ] + m->condLikeStart + Bit(division, &p->anc->clSpace[0]  ) * condLikeRowSize;
	
	/* find transition probabilities (or calculate instead) */
	pL = tiProbs[chain] + m->tiProbStart + (2*p->left->index  + Bit(division, &p->left->tiSpace[0] )) * tiProbRowSize;
	pR = tiProbs[chain] + m->tiProbStart + (2*p->right->index + Bit(division, &p->right->tiSpace[0])) * tiProbRowSize;
	pA = tiProbs[chain] + m->tiProbStart + (2*p->index        + Bit(division, &p->tiSpace[0]       )) * tiProbRowSize;

	/* find likelihoods of site patterns for left branch if terminal */
	shortCut = 0;
#	if !defined (DEBUG_NOSHORTCUTS)
	if (p->left->left == NULL && m->isPartAmbig[p->left->index] == NO)
		{
		shortCut |= 1;
		lState = termState + m->compCharStart + p->left->index * numCompressedChars;
		tiPL = pL;
		for (k=a=0; k<m->numGammaCats; k++)
			{
			catStart = a;
			for (i=0; i<nObsStates; i++)
				for (j=i; j<nStatesSquared; j+=nStates)
					preLikeL[a++] = tiPL[j];
			for (b=1; b<nStates/nObsStates; b++)
				{
				a = catStart;
				for (i=0; i<nObsStates; i++)
					{
					for (j=i+b*nObsStates; j<nStatesSquared; j+=nStates)
						preLikeL[a++] += tiPL[j];
					}
				}
			/* for ambiguous */
			for (i=0; i<nStates; i++)
				preLikeL[a++] = 1.0;
			tiPL += nStatesSquared;
			}
		}

	/* find likelihoods of site patterns for right branch if terminal */
	if (p->right->left == NULL && m->isPartAmbig[p->right->index] == NO)
		{
		shortCut |= 2;
		rState = termState + m->compCharStart + p->right->index * numCompressedChars;
		tiPR = pR;
		for (k=a=0; k<m->numGammaCats; k++)
			{
			catStart = a;
			for (i=0; i<nObsStates; i++)
				for (j=i; j<nStatesSquared; j+=nStates)
					preLikeR[a++] = tiPR[j];
			for (b=1; b<nStates/nObsStates; b++)
				{
				a = catStart;
				for (i=0; i<nObsStates; i++)
					{
					for (j=i+b*nObsStates; j<nStatesSquared; j+=nStates)
						preLikeR[a++] += tiPR[j];
					}
				}
			/* for ambiguous */
			for (i=0; i<nStates; i++)
				preLikeR[a++] = 1.0;
			tiPR += nStatesSquared;
			}
		}

	/* find likelihoods of site patterns for anc branch, always terminal */
	if (m->isPartAmbig[p->anc->index] == YES)
		{
		shortCut = 4;
		}
	else 
		{
		aState = termState + m->compCharStart + p->anc->index * numCompressedChars;
		tiPA = pA;
		for (k=a=0; k<m->numGammaCats; k++)
			{
			catStart = a;
			for (i=0; i<nObsStates; i++)
				for (j=i; j<nStatesSquared; j+=nStates)
					preLikeA[a++] = tiPA[j];
			for (b=1; b<nStates/nObsStates; b++)
				{
				a = catStart;
				for (i=0; i<nObsStates; i++)
					{
					for (j=i+b*nObsStates; j<nStatesSquared; j+=nStates)
						preLikeA[a++] += tiPA[j];
					}
				}
			/* for ambiguous */
			for (i=0; i<nStates; i++)
				preLikeA[a++] = 1.0;
			tiPA += nStatesSquared;
			}
		}
#	else
	shortCut = 4;
#	endif

	switch (shortCut)
		{
	case 4:
		for (c=0; c<m->numChars; c++)
			{
			tiPL = pL;
			tiPR = pR;
			tiPA = pA;
			for (k=h=0; k<m->numGammaCats; k++)
				{
				for (i=0; i<nStates; i++)
					{
					likeL = likeR = likeA = 0.0;
					for (j=0; j<nStates; j++)
						{
						likeL += tiPL[h]   * clL[j];
						likeR += tiPR[h]   * clR[j];
						likeA += tiPA[h++] * clA[j];
						}
					*(clP++) = likeL * likeR * likeA;
					}
				clL += nStates;
				clR += nStates;
				clA += nStates;
				}
			}
		break;
	case 0:
	case 3:
		for (c=0; c<m->numChars; c++)
			{
			tiPL = pL;
			tiPR = pR;
			a = aState[c];
			for (k=h=0; k<m->numGammaCats; k++)
				{
				for (i=0; i<nStates; i++)
					{
					likeL = likeR = 0.0;
					for (j=0; j<nStates; j++)
						{
						likeL += tiPL[h]*clL[j];
						likeR += tiPR[h++]*clR[j];
						}
					*(clP++) = likeL * likeR * preLikeA[a++];
					}
				clL += nStates;
				clR += nStates;
				a += preLikeJump;
				}
			}
		break;
	case 1:
		for (c=0; c<m->numChars; c++)
			{
			tiPR = pR;
			a = lState[c];
			b = aState[c];
			for (k=h=0; k<m->numGammaCats; k++)
				{
				for (i=0; i<nStates; i++)
					{
					likeR = 0.0;
					for (j=0; j<nStates; j++)
						{
						likeR += tiPR[h++]*clR[j];
						}
					*(clP++) = preLikeL[a++] * likeR * preLikeA[b++];
					}
				clR += nStates;
				a += preLikeJump;
				b += preLikeJump;
				}
			}
		break;
	case 2:
		for (c=0; c<m->numChars; c++)
			{
			tiPL = pL;
			a = rState[c];
			b = aState[c];
			for (k=h=0; k<m->numGammaCats; k++)
				{
				for (i=0; i<nStates; i++)
					{
					likeL = 0.0;
					for (j=0; j<nStates; j++)
						{
						likeL += tiPL[h++]*clL[j];
						}
					*(clP++) = likeL * preLikeR[a++] * preLikeA[b++];
					}
				clL += nStates;
				a += preLikeJump;
				b += preLikeJump;
				}
			}
		break;
		}

	return NO_ERROR;
}





/*----------------------------------------------------------------
|
|	CondLikeRoot_NUC4: 4by4 nucleotide model with or without rate
|		variation
|
-----------------------------------------------------------------*/
int CondLikeRoot_NUC4 (TreeNode *p, int division, int chain)

{
	int				c, h, i, j, k, shortCut, *lState=NULL, *rState=NULL, *aState=NULL;
	CLFlt			*clL, *clR, *clP, *clA, *pL, *pR, *pA, *tiPL, *tiPR, *tiPA;
	ModelInfo		*m;
	
	m = &modelSettings[division];

	/* flip state of node so that we are not overwriting old cond likes */
	FlipOneBit (division, &p->clSpace[0]);
	
	/* find conditional likelihood pointers */
	clL = condLikePtr[chain][p->left->index ] + m->condLikeStart + Bit(division, &p->left->clSpace[0] ) * condLikeRowSize;
	clR = condLikePtr[chain][p->right->index] + m->condLikeStart + Bit(division, &p->right->clSpace[0]) * condLikeRowSize;
	clP = condLikePtr[chain][p->index       ] + m->condLikeStart + Bit(division, &p->clSpace[0]       ) * condLikeRowSize;
	clA = condLikePtr[chain][p->anc->index  ] + m->condLikeStart + Bit(division, &p->anc->clSpace[0]  ) * condLikeRowSize;
	
	/* find transition probabilities (or calculate instead) */
	pL = tiProbs[chain] + m->tiProbStart + (2*p->left->index  + Bit(division, &p->left->tiSpace[0] )) * tiProbRowSize;
	pR = tiProbs[chain] + m->tiProbStart + (2*p->right->index + Bit(division, &p->right->tiSpace[0])) * tiProbRowSize;
	pA = tiProbs[chain] + m->tiProbStart + (2*p->index        + Bit(division, &p->tiSpace[0]       )) * tiProbRowSize;

	/* find likelihoods of site patterns for left branch if terminal */
	shortCut = 0;
#	if !defined (DEBUG_NOSHORTCUTS)
	if (p->left->left == NULL && m->isPartAmbig[p->left->index] == NO)
		{
		shortCut |= 1;
		lState = termState + m->compCharStart + p->left->index * numCompressedChars;
		tiPL = pL;
		for (k=j=0; k<m->numGammaCats; k++)
			{
			for (i=0; i<4; i++)
				{
				preLikeL[j++] = tiPL[0];
				preLikeL[j++] = tiPL[4];
				preLikeL[j++] = tiPL[8];
				preLikeL[j++] = tiPL[12];
				tiPL++;
				}
			/* for ambiguous */
			for (i=0; i<4; i++)
				preLikeL[j++] = 1.0;
			tiPL += 12;
			}
		}

	/* find likelihoods of site patterns for right branch if terminal */
	if (p->right->left == NULL && m->isPartAmbig[p->right->index] == NO)
		{
		shortCut |= 2;
		rState = termState + m->compCharStart + p->right->index * numCompressedChars;
		tiPR = pR;
		for (k=j=0; k<m->numGammaCats; k++)
			{
			for (i=0; i<4; i++)
				{
				preLikeR[j++] = tiPR[0];
				preLikeR[j++] = tiPR[4];
				preLikeR[j++] = tiPR[8];
				preLikeR[j++] = tiPR[12];
				tiPR++;
				}
			/* for ambiguous */
			for (i=0; i<4; i++)
				preLikeR[j++] = 1.0;
			tiPR += 12;
			}
		}

	/* find likelihoods of site patterns for anc branch, always terminal */
	if (m->isPartAmbig[p->anc->index] == YES)
		{
		shortCut = 4;
		}
	else 
		{
		aState = termState + m->compCharStart + p->anc->index * numCompressedChars;
		tiPA = pA;
		for (k=j=0; k<m->numGammaCats; k++)
			{
			for (i=0; i<4; i++)
				{
				preLikeA[j++] = tiPA[0];
				preLikeA[j++] = tiPA[4];
				preLikeA[j++] = tiPA[8];
				preLikeA[j++] = tiPA[12];
				tiPA++;
				}
			/* for ambiguous */
			for (i=0; i<4; i++)
				preLikeA[j++] = 1.0;
			tiPA += 12;
			}
		}
#	else
	shortCut = 4;
#	endif

	switch (shortCut)
		{
	case 4:
		for (c=h=0; c<m->numChars; c++)
			{
			tiPL = pL;
			tiPR = pR;
			tiPA = pA;
			for (k=0; k<m->numGammaCats; k++)
				{
				clP[h++] =   (tiPL[AA]*clL[A] + tiPL[AC]*clL[C] + tiPL[AG]*clL[G] + tiPL[AT]*clL[T])
							*(tiPR[AA]*clR[A] + tiPR[AC]*clR[C] + tiPR[AG]*clR[G] + tiPR[AT]*clR[T])
							*(tiPA[AA]*clA[A] + tiPA[AC]*clA[C] + tiPA[AG]*clA[G] + tiPA[AT]*clA[T]);
				clP[h++] =   (tiPL[CA]*clL[A] + tiPL[CC]*clL[C] + tiPL[CG]*clL[G] + tiPL[CT]*clL[T])
							*(tiPR[CA]*clR[A] + tiPR[CC]*clR[C] + tiPR[CG]*clR[G] + tiPR[CT]*clR[T])
							*(tiPA[CA]*clA[A] + tiPA[CC]*clA[C] + tiPA[CG]*clA[G] + tiPA[CT]*clA[T]);
				clP[h++] =   (tiPL[GA]*clL[A] + tiPL[GC]*clL[C] + tiPL[GG]*clL[G] + tiPL[GT]*clL[T])
							*(tiPR[GA]*clR[A] + tiPR[GC]*clR[C] + tiPR[GG]*clR[G] + tiPR[GT]*clR[T])
							*(tiPA[GA]*clA[A] + tiPA[GC]*clA[C] + tiPA[GG]*clA[G] + tiPA[GT]*clA[T]);
				clP[h++] =   (tiPL[TA]*clL[A] + tiPL[TC]*clL[C] + tiPL[TG]*clL[G] + tiPL[TT]*clL[T])
							*(tiPR[TA]*clR[A] + tiPR[TC]*clR[C] + tiPR[TG]*clR[G] + tiPR[TT]*clR[T])
							*(tiPA[TA]*clA[A] + tiPA[TC]*clA[C] + tiPA[TG]*clA[G] + tiPA[TT]*clA[T]);
				clL += 4;
				clR += 4;
				clA += 4;
				tiPL += 16;
				tiPR += 16;
				tiPA += 16;
				}
			}
		break;

	case 0:
	case 3:
		for (c=h=0; c<m->numChars; c++)
			{
			tiPL = pL;
			tiPR = pR;
			i = aState[c];
			for (k=0; k<m->numGammaCats; k++)
				{
				clP[h++] =   (tiPL[AA]*clL[A] + tiPL[AC]*clL[C] + tiPL[AG]*clL[G] + tiPL[AT]*clL[T])
							*(tiPR[AA]*clR[A] + tiPR[AC]*clR[C] + tiPR[AG]*clR[G] + tiPR[AT]*clR[T])
							*preLikeA[i++];
				clP[h++] =   (tiPL[CA]*clL[A] + tiPL[CC]*clL[C] + tiPL[CG]*clL[G] + tiPL[CT]*clL[T])
							*(tiPR[CA]*clR[A] + tiPR[CC]*clR[C] + tiPR[CG]*clR[G] + tiPR[CT]*clR[T])
							*preLikeA[i++];
				clP[h++] =   (tiPL[GA]*clL[A] + tiPL[GC]*clL[C] + tiPL[GG]*clL[G] + tiPL[GT]*clL[T])
							*(tiPR[GA]*clR[A] + tiPR[GC]*clR[C] + tiPR[GG]*clR[G] + tiPR[GT]*clR[T])
							*preLikeA[i++];
				clP[h++] =   (tiPL[TA]*clL[A] + tiPL[TC]*clL[C] + tiPL[TG]*clL[G] + tiPL[TT]*clL[T])
							*(tiPR[TA]*clR[A] + tiPR[TC]*clR[C] + tiPR[TG]*clR[G] + tiPR[TT]*clR[T])
							*preLikeA[i++];
				clL += 4;
				clR += 4;
				tiPL += 16;
				tiPR += 16;
				i += 16;
				}
			}
		break;

	case 1:
		for (c=h=0; c<m->numChars; c++)
			{
			tiPR = pR;
			i = lState[c];
			j = aState[c];
			for (k=0; k<m->numGammaCats; k++)
				{
				clP[h++] =   (tiPR[AA]*clR[A] + tiPR[AC]*clR[C] + tiPR[AG]*clR[G] + tiPR[AT]*clR[T])
							*preLikeL[i++]*preLikeA[j++];
				clP[h++] =   (tiPR[CA]*clR[A] + tiPR[CC]*clR[C] + tiPR[CG]*clR[G] + tiPR[CT]*clR[T])
							*preLikeL[i++]*preLikeA[j++];
				clP[h++] =   (tiPR[GA]*clR[A] + tiPR[GC]*clR[C] + tiPR[GG]*clR[G] + tiPR[GT]*clR[T])
							*preLikeL[i++]*preLikeA[j++];
				clP[h++] =   (tiPR[TA]*clR[A] + tiPR[TC]*clR[C] + tiPR[TG]*clR[G] + tiPR[TT]*clR[T])
							*preLikeL[i++]*preLikeA[j++];
				clR += 4;
				tiPR += 16;
				i += 16;
				j += 16;
				}
			}
		break;

	case 2:
		for (c=h=0; c<m->numChars; c++)
			{
			tiPL = pL;
			i = rState[c];
			j = aState[c];
			for (k=0; k<m->numGammaCats; k++)
				{
				clP[h++] =   (tiPL[AA]*clL[A] + tiPL[AC]*clL[C] + tiPL[AG]*clL[G] + tiPL[AT]*clL[T])
							*preLikeR[i++]*preLikeA[j++];
				clP[h++] =   (tiPL[CA]*clL[A] + tiPL[CC]*clL[C] + tiPL[CG]*clL[G] + tiPL[CT]*clL[T])
							*preLikeR[i++]*preLikeA[j++];
				clP[h++] =   (tiPL[GA]*clL[A] + tiPL[GC]*clL[C] + tiPL[GG]*clL[G] + tiPL[GT]*clL[T])
							*preLikeR[i++]*preLikeA[j++];
				clP[h++] =   (tiPL[TA]*clL[A] + tiPL[TC]*clL[C] + tiPL[TG]*clL[G] + tiPL[TT]*clL[T])
							*preLikeR[i++]*preLikeA[j++];
				clL += 4;
				tiPL += 16;
				i += 16;
				j += 16;
				}
			}
		break;
		}

	return NO_ERROR;
	
}





#if defined SSE
/*----------------------------------------------------------------
|
|	CondLikeRoot_NUC4_SSE: 4by4 nucleotide model with or without rate
|		variation using SSE instructions
|
-----------------------------------------------------------------*/
int CondLikeRoot_NUC4_SSE (TreeNode *p, int division, int chain)

{
	int				c, k;
	__m128			*clL, *clR, *clP, *clA, *pL, *pR, *pA, *tiPL, *tiPR, *tiPA;
	__m128			m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12;
	ModelInfo		*m;
	
	m = &modelSettings[division];

	/* flip state of node so that we are not overwriting old cond likes */
	FlipOneBit (division, &p->clSpace[0]);
	
	/* find conditional likelihood pointers */
	clL = (__m128 *) (condLikePtr[chain][p->left->index ] + m->condLikeStart + Bit(division, &p->left->clSpace[0] ) * condLikeRowSize);
	clR = (__m128 *) (condLikePtr[chain][p->right->index] + m->condLikeStart + Bit(division, &p->right->clSpace[0]) * condLikeRowSize);
	clP = (__m128 *) (condLikePtr[chain][p->index       ] + m->condLikeStart + Bit(division, &p->clSpace[0]       ) * condLikeRowSize);
	clA = (__m128 *) (condLikePtr[chain][p->anc->index  ] + m->condLikeStart + Bit(division, &p->anc->clSpace[0]  ) * condLikeRowSize);
	
	/* find transition probabilities (or calculate instead) */
	pL = (__m128 *) (tiProbs[chain] + m->tiProbStart + (2*p->left->index  + Bit(division, &p->left->tiSpace[0] )) * tiProbRowSize);
	pR = (__m128 *) (tiProbs[chain] + m->tiProbStart + (2*p->right->index + Bit(division, &p->right->tiSpace[0])) * tiProbRowSize);
	pA = (__m128 *) (tiProbs[chain] + m->tiProbStart + (2*p->index        + Bit(division, &p->tiSpace[0]       )) * tiProbRowSize);

	for (c=0; c<m->numChars; c++)
		{
		tiPL = pL;
		tiPR = pR;
		tiPA = pA;
		for (k=0; k<m->numGammaCats; k++)
			{
			m1 = *clL;
			m2 = *clR;
			m3 = *clA;
			m4 = _mm_shuffle_ps (m1, m1, _MM_SHUFFLE (0, 3, 2, 1));
			m5 = _mm_shuffle_ps (m2, m2, _MM_SHUFFLE (0, 3, 2, 1));
			m6 = _mm_shuffle_ps (m3, m3, _MM_SHUFFLE (0, 3, 2, 1));
			m7 = _mm_mul_ps (m1, tiPL[0]);
			m8 = _mm_mul_ps (m2, tiPR[0]);
			m9 = _mm_mul_ps (m3, tiPA[0]);
			m10 = _mm_mul_ps (m4, tiPL[1]);
			m11 = _mm_mul_ps (m5, tiPR[1]);
			m12 = _mm_mul_ps (m6, tiPA[1]);
			m7 = _mm_add_ps (m7, m10);
			m8 = _mm_add_ps (m8, m11);
			m9 = _mm_add_ps (m9, m12);
			
			m1 = _mm_shuffle_ps (m4, m4, _MM_SHUFFLE (0, 3, 2, 1));
			m2 = _mm_shuffle_ps (m5, m5, _MM_SHUFFLE (0, 3, 2, 1));
			m3 = _mm_shuffle_ps (m6, m6, _MM_SHUFFLE (0, 3, 2, 1));
			m4 = _mm_shuffle_ps (m1, m1, _MM_SHUFFLE (0, 3, 2, 1));
			m5 = _mm_shuffle_ps (m2, m2, _MM_SHUFFLE (0, 3, 2, 1));
			m6 = _mm_shuffle_ps (m3, m3, _MM_SHUFFLE (0, 3, 2, 1));
			m10 = _mm_mul_ps (m1, tiPL[2]);
			m11 = _mm_mul_ps (m2, tiPR[2]);
			m12 = _mm_mul_ps (m3, tiPA[2]);
			m7 = _mm_add_ps (m7, m10);
			m8 = _mm_add_ps (m8, m11);
			m9 = _mm_add_ps (m9, m12);
			m10 = _mm_mul_ps (m4, tiPL[3]);
			m11 = _mm_mul_ps (m5, tiPR[3]);
			m12 = _mm_mul_ps (m6, tiPA[3]);
			m7 = _mm_add_ps (m7, m10);
			m8 = _mm_add_ps (m8, m11);
			m9 = _mm_add_ps (m9, m12);

			m1 = _mm_mul_ps (m7, m8);
			*clP = _mm_mul_ps (m1, m9);
			clP++;
			clL++;
			clR++;
			clA++;
			}
		}

	return NO_ERROR;
	
}
#endif




/*----------------------------------------------------------------
|
|	CondLikeRoot_NY98: codon model with omega variation
|
-----------------------------------------------------------------*/
int CondLikeRoot_NY98 (TreeNode *p, int division, int chain)

{

	int				a, b, c, h, i, j, k, shortCut, *lState=NULL, *rState=NULL, *aState=NULL,
					nStates, nStatesSquared;
	int                     nObsStates, preLikeJump; /* khs07062005 */
	CLFlt			likeL, likeR, likeA, *clL, *clR, *clP, *clA, *pL, *pR, *pA,
					*tiPL, *tiPR, *tiPA;
	ModelInfo		*m;
	
	/* find model settings for this division and nStates, nStatesSquared */
	m = &modelSettings[division];
	nObsStates = m->numStates; /* khs07062005: copied from CondLikeRoot_Gen*/
	nStates = m->numModelStates;
	nStatesSquared = nStates * nStates;
	preLikeJump = nObsStates * nStates; /* khs07062005: copied from CondLikeRoot_Gen*/

	/* flip state of node so that we are not overwriting old cond likes */
	FlipOneBit (division, &p->clSpace[0]);
	
	/* find conditional likelihood pointers */
	clL = condLikePtr[chain][p->left->index ] + m->condLikeStart + Bit(division, &p->left->clSpace[0] ) * condLikeRowSize;
	clR = condLikePtr[chain][p->right->index] + m->condLikeStart + Bit(division, &p->right->clSpace[0]) * condLikeRowSize;
	clP = condLikePtr[chain][p->index       ] + m->condLikeStart + Bit(division, &p->clSpace[0]       ) * condLikeRowSize;
	clA = condLikePtr[chain][p->anc->index  ] + m->condLikeStart + Bit(division, &p->anc->clSpace[0]  ) * condLikeRowSize;
	
	/* find transition probabilities (or calculate instead) */
	pL = tiProbs[chain] + m->tiProbStart + (2*p->left->index  + Bit(division, &p->left->tiSpace[0] )) * tiProbRowSize;
	pR = tiProbs[chain] + m->tiProbStart + (2*p->right->index + Bit(division, &p->right->tiSpace[0])) * tiProbRowSize;
	pA = tiProbs[chain] + m->tiProbStart + (2*p->index        + Bit(division, &p->tiSpace[0]       )) * tiProbRowSize;

	/* find likelihoods of site patterns for left branch if terminal */
	shortCut = 0;
#	if !defined (DEBUG_NOSHORTCUTS)
	if (p->left->left == NULL && m->isPartAmbig[p->left->index] == NO)
		{
		shortCut |= 1;
		lState = termState + m->compCharStart + p->left->index * numCompressedChars;
		tiPL = pL;
		for (k=a=0; k<m->numOmegaCats; k++)
			{
			for (i=0; i<nStates; i++)
				for (j=i; j<nStatesSquared; j+=nStates)
					preLikeL[a++] = tiPL[j];
			/* for ambiguous */
			for (i=0; i<nStates; i++)
				preLikeL[a++] = 1.0;
			tiPL += nStatesSquared;
			}
		}

	/* find likelihoods of site patterns for right branch if terminal */
	if (p->right->left == NULL && m->isPartAmbig[p->right->index] == NO)
		{
		shortCut |= 2;
		rState = termState + m->compCharStart + p->right->index * numCompressedChars;
		tiPR = pR;
		for (k=a=0; k<m->numOmegaCats; k++)
			{
			for (i=0; i<nStates; i++)
				for (j=i; j<nStatesSquared; j+=nStates)
					preLikeR[a++] = tiPR[j];
			/* for ambiguous */
			for (i=0; i<nStates; i++)
				preLikeR[a++] = 1.0;
			tiPR += nStatesSquared;
			}
		}

	/* find likelihoods of site patterns for anc branch, always terminal */
	if (m->isPartAmbig[p->anc->index] == YES)
		{
		shortCut = 4;
		}
	else 
		{
		aState = termState + m->compCharStart + p->anc->index * numCompressedChars;
		tiPA = pA;
		for (k=a=0; k<m->numOmegaCats; k++)
			{
			for (i=0; i<nStates; i++)
				for (j=i; j<nStatesSquared; j+=nStates)
					preLikeA[a++] = tiPA[j];
			/* for ambiguous */
			for (i=0; i<nStates; i++)
				preLikeA[a++] = 1.0;
			tiPA += nStatesSquared;
			}
		}
#	else
	shortCut = 4;
#	endif

	switch (shortCut)
		{
	case 4:
		for (c=0; c<m->numChars; c++)
			{
			tiPL = pL;
			tiPR = pR;
			tiPA = pA;
			for (k=h=0; k<m->numOmegaCats; k++)
				{
				for (i=0; i<nStates; i++)
					{
					likeL = likeR = likeA = 0.0;
					for (j=0; j<nStates; j++)
						{
						likeL += tiPL[h]*clL[j];
						likeR += tiPR[h]*clR[j];
						likeA += tiPA[h++]*clA[j];
						}
					*(clP++) = likeL * likeR * likeA;
					}
				clL += nStates;
				clR += nStates;
				clA += nStates;
				}
			}
		break;
	case 0:
	case 3:
		for (c=0; c<m->numChars; c++)
			{
			tiPL = pL;
			tiPR = pR;
			a = aState[c];
			for (k=h=0; k<m->numOmegaCats; k++)
				{
				for (i=0; i<nStates; i++)
					{
					likeL = likeR = 0.0;
					for (j=0; j<nStates; j++)
						{
						likeL += tiPL[h]*clL[j];
						likeR += tiPR[h++]*clR[j];
						}
					*(clP++) = likeL * likeR * preLikeA[a++];
					}
				clL += nStates;
				clR += nStates;
				a += preLikeJump; /* khs07062005, was a+=nStates;*/
				}
			}
		break;
	case 1:
		for (c=0; c<m->numChars; c++)
			{
			tiPR = pR;
			a = lState[c];
			b = aState[c];
			for (k=h=0; k<m->numOmegaCats; k++)
				{
				for (i=0; i<nStates; i++)
					{
					likeR = 0.0;
					for (j=0; j<nStates; j++)
						{
						likeR += tiPR[h++]*clR[j];
						}
					*(clP++) = preLikeL[a++] * likeR * preLikeA[b++];
					}
				clR += nStates;
				a += preLikeJump; /* khs07062005, was a+=nStates;*/
				b += preLikeJump; /* khs07062005, was a+=nStates;*/
				}
			}
		break;
	case 2:
		for (c=0; c<m->numChars; c++)
			{
			tiPL = pL;
			a = rState[c];
			b = aState[c];
			for (k=h=0; k<m->numOmegaCats; k++)
				{
				for (i=0; i<nStates; i++)
					{
					likeL = 0.0;
					for (j=0; j<nStates; j++)
						{
						likeL += tiPL[h++]*clL[j];
						}
					*(clP++) = likeL * preLikeR[a++] * preLikeA[b++];
					}
				clL += nStates;
				a += preLikeJump; /* khs07062005, was a+=nStates;*/
				b += preLikeJump; /* khs07062005, was a+=nStates;*/
				}
			}
		break;
		}

	return NO_ERROR;
}





/*----------------------------------------------------------------
|
|	CondLikeRoot_Std: variable number of states model
|		with or without rate variation
|
-----------------------------------------------------------------*/
int CondLikeRoot_Std (TreeNode *p, int division, int chain)

{

	int				a, c, h, i, j, k, nStates, nCats;
	CLFlt			*clL, *clR, *clP, *clA, *pL, *pR, *pA, *tiPL, *tiPR, *tiPA,
					likeL, likeR, likeA;
	ModelInfo		*m;
	
	m = &modelSettings[division];

	/* flip state of node so that we are not overwriting old cond likes */
	FlipOneBit (division, &p->clSpace[0]);
	
	/* find conditional likelihood pointers */
	clL = condLikePtr[chain][p->left->index ] + m->condLikeStart + Bit(division, &p->left->clSpace[0] ) * condLikeRowSize;
	clR = condLikePtr[chain][p->right->index] + m->condLikeStart + Bit(division, &p->right->clSpace[0]) * condLikeRowSize;;
	clP = condLikePtr[chain][p->index       ] + m->condLikeStart + Bit(division, &p->clSpace[0]       ) * condLikeRowSize;
	clA = condLikePtr[chain][p->anc->index  ] + m->condLikeStart + Bit(division, &p->anc->clSpace[0]  ) * condLikeRowSize;
	
	/* find transition probabilities */
	pL = tiProbs[chain] + m->tiProbStart + (2*p->left->index  + Bit(division, &p->left->tiSpace[0] )) * tiProbRowSize;
	pR = tiProbs[chain] + m->tiProbStart + (2*p->right->index + Bit(division, &p->right->tiSpace[0])) * tiProbRowSize;
	pA = tiProbs[chain] + m->tiProbStart + (2*p->index        + Bit(division, &p->tiSpace[0]       )) * tiProbRowSize;

	/* calculate ancestral probabilities */
	for (c=h=0; c<m->numChars; c++)
		{
		tiPL = pL + m->tiIndex[c];
		tiPR = pR + m->tiIndex[c];
		tiPA = pA + m->tiIndex[c];
		nStates = m->nStates[c];

		/* the following lines ensure that nCats is 1 unless */
		/* the character is binary and beta categories are used  */
		if (nStates == 2)
			nCats = m->numBetaCats;
		else
			nCats = 1;

		/* now multiply with the gamma cats */
		nCats *= m->numGammaCats;
		for (k=j=0; k<nCats; k++)
			{
			for (a=0; a<nStates; a++)
				{
				likeL = likeR = likeA = 0.0;
				for (i=0; i<nStates; i++)
					{
					likeL += tiPL[j] * clL[i];
					likeR += tiPR[j] * clR[i];
					likeA += tiPA[j++] * clA[i];
					}
				clP[h++] = likeL * likeR * likeA;
				}
			clL += nStates;
			clR += nStates;
			clA += nStates;
			}
		}

	return NO_ERROR;
}




/*----------------------------------------------------------------
|
|	CondLikeUp_Bin: pull likelihoods up and calculate scaled
|		finals, binary model with or without rate variation
|
-----------------------------------------------------------------*/
int CondLikeUp_Bin (TreeNode *p, int division, int chain)

{

	int				c, k;
	CLFlt			*clFA, *clFP, *clDP, *pA, *tiPA, likeUp[2];
	ModelInfo		*m;
	
	/* find model settings for this division */
	m = &modelSettings[division];

	if (p->anc->anc == NULL)
		{
		/* this is the root node */
		/* find conditional likelihood pointers = down cond likes */
		/* use conditional likelihood scratch space for final cond likes */
		clDP = condLikePtr[chain][p->index       ] + m->condLikeStart +      Bit(division, &p->clSpace[0]      ) * condLikeRowSize;
		clFP = condLikePtr[chain][p->index       ] + m->condLikeStart + (1 ^ Bit(division, &p->clSpace[0]     )) * condLikeRowSize;

		for (c=0; c<m->numChars; c++)
			{
			for (k=0; k<m->numGammaCats; k++)
				{
				*(clFP++) = *(clDP++);
				*(clFP++) = *(clDP++);
				}
			}
		}
	else
		{
		/* find conditional likelihood pointers */
		/* use conditional likelihood scratch space for final cond likes */
		clFA = condLikePtr[chain][p->anc->index  ] + m->condLikeStart + (1 ^ Bit(division, &p->anc->clSpace[0])) * condLikeRowSize;
		clFP = condLikePtr[chain][p->index       ] + m->condLikeStart + (1 ^ Bit(division, &p->clSpace[0]     )) * condLikeRowSize;
		clDP = condLikePtr[chain][p->index       ] + m->condLikeStart +      Bit(division, &p->clSpace[0]      ) * condLikeRowSize;

		/* find transition probabilities */
		pA = tiProbs[chain] + m->tiProbStart + (2*p->anc->index  + Bit(division, &p->left->tiSpace[0] )) * tiProbRowSize;

		for (c=0; c<m->numChars; c++)
			{
			tiPA = pA;
			for (k=0; k<m->numGammaCats; k++)
				{
				likeUp[0] = clFA[0] / (tiPA[0]*clDP[0] + tiPA[1]*clDP[1]);
				likeUp[1] = clFA[1] / (tiPA[2]*clDP[0] + tiPA[3]*clDP[2]);
				*(clFP++) = (likeUp[0]*tiPA[0] + likeUp[1]*tiPA[2])*clDP[0];
				*(clFP++) = (likeUp[0]*tiPA[1] + likeUp[1]*tiPA[3])*clDP[1];
				tiPA += 4;
				clFA += 2;
				clDP += 2;
				}
			}
		}	

	return NO_ERROR;
	
}




/*----------------------------------------------------------------
|
|	CondLikeUp_Gen: pull likelihoods up and calculate scaled
|		finals for an interior node
|
-----------------------------------------------------------------*/
int CondLikeUp_Gen (TreeNode *p, int division, int chain)

{

	int				a, c, i, j, k, nStates, nStatesSquared, nGammaCats;
	CLFlt			*clFA, *clFP, *clDP, *pA, *tiPA, *likeUp, sum;
	ModelInfo		*m;
	
	/* find model settings for this division */
	m = &modelSettings[division];

	/* find number of states in the model */
	nStates = m->numModelStates;
	nStatesSquared = nStates * nStates;

	/* find number of gamma cats */
	nGammaCats = m->numGammaCats;

	/* use preallocated scratch space */
	likeUp = ancStateCondLikes;

	/* calculate final states */
	if (p->anc->anc == NULL)
		{
		/* this is the root node */
		/* find conditional likelihood pointers = down cond likes */
		/* use conditional likelihood scratch space for final cond likes */
		clDP = condLikePtr[chain][p->index       ] + m->condLikeStart +      Bit(division, &p->clSpace[0]      ) * condLikeRowSize;
		clFP = condLikePtr[chain][p->index       ] + m->condLikeStart + (1 ^ Bit(division, &p->clSpace[0]     )) * condLikeRowSize;

		/* final cond likes = downpass cond likes */
		for (c=0; c<m->numChars; c++)
			{
			/* copy cond likes */ 
			for (k=0; k<nGammaCats*nStates; k++)
				*(clFP++) = *(clDP++);
			}
		}
	else
		{
		/* find conditional likelihood pointers */
		/* use conditional likelihood scratch space for final cond likes */
		clFA = condLikePtr[chain][p->anc->index  ] + m->condLikeStart + (1 ^ Bit(division, &p->anc->clSpace[0])) * condLikeRowSize;
		clFP = condLikePtr[chain][p->index       ] + m->condLikeStart + (1 ^ Bit(division, &p->clSpace[0]     )) * condLikeRowSize;
		clDP = condLikePtr[chain][p->index       ] + m->condLikeStart +      Bit(division, &p->clSpace[0]      ) * condLikeRowSize;

		/* find transition probabilities */
		pA = tiProbs[chain] + m->tiProbStart + (2*p->anc->index  + Bit(division, &p->left->tiSpace[0] )) * tiProbRowSize;

		for (c=0; c<m->numChars; c++)
			{
			tiPA = pA;
			for (k=0; k<nGammaCats; k++)
				{
				for (a=j=0; a<nStates; a++)
					{
					sum = 0.0;
					for (i=0; i<nStates; i++)
						sum += tiPA[j++]*clDP[i];
					likeUp[a] = clFA[a] / sum;
					}
					
				for (a=0; a<nStates; a++)
					{
					sum = 0.0;
					j = a;
					for (i=0; i<nStates; i++)
						{
						sum += likeUp[i] * tiPA[j];
						j += nStates;
						}
					*(clFP++) = sum * clDP[a];
					}

				clFA += nStates;
				clDP += nStates;
				tiPA += nStatesSquared;
				}
			}
		}	

	return NO_ERROR;
}




/*----------------------------------------------------------------
|
|	CondLikeUp_NUC4: pull likelihoods up and calculate scaled
|		finals for an interior node
|
-----------------------------------------------------------------*/
int     CondLikeUp_NUC4 (TreeNode *p, int division, int chain)

{

	int				c, k, nGammaCats;
	CLFlt			*clFA, *clFP, *clDP, *pA, *tiPA, likeUp[4];
	ModelInfo		*m;
	
	/* find model settings for this division */
	m = &modelSettings[division];

	/* find number of gamma cats */
	nGammaCats = m->numGammaCats;

	/* calculate final states */
	if (p->anc->anc == NULL)
		{
		/* this is the root node */
		/* find conditional likelihood pointers = down cond likes */
		/* use conditional likelihood scratch space for final cond likes */
		clDP = condLikePtr[chain][p->index       ] + m->condLikeStart +      Bit(division, &p->clSpace[0]      ) * condLikeRowSize;
		clFP = condLikePtr[chain][p->index       ] + m->condLikeStart + (1 ^ Bit(division, &p->clSpace[0]     )) * condLikeRowSize;

		/* final cond likes = downpass cond likes */
		for (c=0; c<m->numChars; c++)
			{
			/* copy cond likes */ 
			for (k=0; k<nGammaCats; k++)
				{
				*(clFP++) = *(clDP++);
				*(clFP++) = *(clDP++);
				*(clFP++) = *(clDP++);
				*(clFP++) = *(clDP++);
				}
			}
		}
	else
		{
		/* find conditional likelihood pointers */
		/* use conditional likelihood scratch space for final cond likes */
		clFA = condLikePtr[chain][p->anc->index  ] + m->condLikeStart + (1 ^ Bit(division, &p->anc->clSpace[0])) * condLikeRowSize;
		clFP = condLikePtr[chain][p->index       ] + m->condLikeStart + (1 ^ Bit(division, &p->clSpace[0]     )) * condLikeRowSize;
		clDP = condLikePtr[chain][p->index       ] + m->condLikeStart +      Bit(division, &p->clSpace[0]      ) * condLikeRowSize;

		/* find transition probabilities */
		pA = tiProbs[chain] + m->tiProbStart + (2*p->anc->index  + Bit(division, &p->left->tiSpace[0] )) * tiProbRowSize;

		for (c=0; c<m->numChars; c++)
			{
			tiPA = pA;
			for (k=0; k<nGammaCats; k++)
				{
				likeUp[A] = clFA[A] / (tiPA[AA]*clDP[A] + tiPA[AC]*clDP[C] + tiPA[AG]*clDP[G] + tiPA[AT]*clDP[T]);
				likeUp[C] = clFA[C] / (tiPA[CA]*clDP[A] + tiPA[CC]*clDP[C] + tiPA[CG]*clDP[G] + tiPA[CT]*clDP[T]);
				likeUp[G] = clFA[G] / (tiPA[GA]*clDP[A] + tiPA[GC]*clDP[C] + tiPA[GG]*clDP[G] + tiPA[GT]*clDP[T]);
				likeUp[T] = clFA[T] / (tiPA[TA]*clDP[A] + tiPA[TC]*clDP[C] + tiPA[TG]*clDP[G] + tiPA[TT]*clDP[T]);				
					
				clFP[A] = (likeUp[A]*tiPA[AA] + likeUp[C]*tiPA[CA] + likeUp[G]*tiPA[GA] + likeUp[T]*tiPA[TA])*clDP[A];
				clFP[C] = (likeUp[A]*tiPA[AC] + likeUp[C]*tiPA[CC] + likeUp[G]*tiPA[GC] + likeUp[T]*tiPA[TC])*clDP[C];
				clFP[G] = (likeUp[A]*tiPA[AG] + likeUp[C]*tiPA[CG] + likeUp[G]*tiPA[GG] + likeUp[T]*tiPA[TG])*clDP[G];
				clFP[T] = (likeUp[A]*tiPA[AT] + likeUp[C]*tiPA[CT] + likeUp[G]*tiPA[GT] + likeUp[T]*tiPA[TT])*clDP[T];

				clFA += 4;
				clFP += 4;
				clDP += 4;
				tiPA += 16;
				}
			}
		}	

	return NO_ERROR;
}




/*----------------------------------------------------------------
|
|	CondLikeUp_Std: pull likelihoods up and calculate scaled
|		finals for an interior node
|
-----------------------------------------------------------------*/
int     CondLikeUp_Std (TreeNode *p, int division, int chain)
{

	int				a, c, i, j, k, nStates, nCats;
	CLFlt			*clFA, *clFP, *clDP, *pA, *tiPA, likeUp[10], sum;
	ModelInfo		*m;
	
	/* find model settings for this division */
	m = &modelSettings[division];

	/* calculate final states */
	if (p->anc->anc == NULL)
		{
		/* this is the root node */
		/* find conditional likelihood pointers = down cond likes */
		/* use conditional likelihood scratch space for final cond likes */
		clDP = condLikePtr[chain][p->index       ] + m->condLikeStart +      Bit(division, &p->clSpace[0]      ) * condLikeRowSize;
		clFP = condLikePtr[chain][p->index       ] + m->condLikeStart + (1 ^ Bit(division, &p->clSpace[0]     )) * condLikeRowSize;

		/* final cond likes = downpass cond likes */
		for (c=0; c<m->numChars; c++)
			{
			/* calculate nStates and nCats */
			nStates = m->nStates[c];
			
			/* the following lines ensure that nCats is 1 unless */
			/* the character is binary and beta categories are used  */
			if (nStates == 2)
				nCats = m->numBetaCats;
			else
				nCats = 1;

			/* finally multiply with the gamma cats */
			nCats *= m->numGammaCats;

			/* copy cond likes */ 
			for (k=0; k<nCats*nStates; k++)
				*(clFP++) = *(clDP++);
			}
		}
	else
		{
		/* find conditional likelihood pointers */
		/* use conditional likelihood scratch space for final cond likes */
		clFA = condLikePtr[chain][p->anc->index  ] + m->condLikeStart + (1 ^ Bit(division, &p->anc->clSpace[0])) * condLikeRowSize;
		clFP = condLikePtr[chain][p->index       ] + m->condLikeStart + (1 ^ Bit(division, &p->clSpace[0]     )) * condLikeRowSize;
		clDP = condLikePtr[chain][p->index       ] + m->condLikeStart +      Bit(division, &p->clSpace[0]      ) * condLikeRowSize;

		/* find transition probabilities */
		pA = tiProbs[chain] + m->tiProbStart + (2*p->anc->index  + Bit(division, &p->left->tiSpace[0] )) * tiProbRowSize;

		for (c=0; c<m->numChars; c++)
			{
			/* find transition probs for this character */
			tiPA = pA + m->tiIndex[c];

			/* calculate nStates and nCats */
			nStates = m->nStates[c];
			
			/* the following lines ensure that nCats is 1 unless */
			/* the character is binary and beta categories are used  */
			if (nStates == 2)
				nCats = m->numBetaCats;
			else
				nCats = 1;

			/* finally multiply with the gamma cats */
			nCats *= m->numGammaCats;

			/* now calculate the final cond likes */
			for (k=0; k<nCats; k++)
				{
				for (a=j=0; a<nStates; a++)
					{
					sum = 0.0;
					for (i=0; i<nStates; i++)
						sum += tiPA[j++]*clDP[i];
					likeUp[a] = clFA[a] / sum;
					}
					
				for (a=0; a<nStates; a++)
					{
					sum = 0.0;
					j = a;
					for (i=0; i<nStates; i++)
						{
						sum += likeUp[i] * tiPA[j];
						j += nStates;
						}
					clFP[a] = sum * clDP[a];
					}

				clFP += nStates;
				clFA += nStates;
				clDP += nStates;
				tiPA += nStates*nStates;
				}
			}
		}	

	return NO_ERROR;
}




/*----------------------------------------------------------------
|
|	CondLikeScaler_Gen: general n-state model with or without rate
|		variation
|
-----------------------------------------------------------------*/
int CondLikeScaler_Gen (TreeNode *p, int division, int chain)

{

	int				c, i, j, k, n, nStates;
	CLFlt			scaler, *clP, *scPNew, *scPOld, *lnScaler;
	ModelInfo		*m;

	m = &modelSettings[division];
	nStates = m->numModelStates;

	/* find conditional likelihood pointer */
	clP = condLikePtr[chain][p->index] + m->condLikeStart + Bit(division, &p->clSpace[0]) * condLikeRowSize;

	scPNew = nodeScaler[chain] + m->compCharStart + (2 * (p->index - numLocalTaxa) +  Bit(division, &p->clSpace[0]   )) * numCompressedChars;
	scPOld = nodeScaler[chain] + m->compCharStart + (2 * (p->index - numLocalTaxa) + (Bit(division, &p->clSpace[0])^1)) * numCompressedChars;

	lnScaler = treeScaler[chain] + m->compCharStart + state[chain] * numCompressedChars;

	/* subtract old values; these can also be associated with old scaler nodes */
	if (Bit(division, &p->scalersSet[0]) == 1)
		{
		for (c=0; c<m->numChars; c++)
			lnScaler[c] -= scPOld[c];	/*subtract old value */
		FlipOneBit(division, &p->scalersSet[0]);
		}

	/* add new values only if currently a scaler node */
	if (p->scalerNode == YES)
		{
		i = j = 0;
		for (c=0; c<m->numChars; c++)
			{
			scaler = 0.0;
			for (k=0; k<m->numGammaCats; k++)
				{
				for (n=0; n<nStates; n++)
					{
					if (clP[i] > scaler)
						scaler = clP[i];
					i++;
					}
				}
			for (k=0; k<m->numGammaCats; k++)
				{
				for (n=0; n<nStates; n++)
					clP[j++] /= scaler;
				}
			scPNew[c] = (CLFlt) log (scaler);	/* store scaler       */
			lnScaler[c] += scPNew[c];	/* add to site scaler */
			}
		FlipOneBit(division, &p->scalersSet[0]);
		}

	return (NO_ERROR);

}





#if !defined (FAST_LOG)
/*----------------------------------------------------------------
|
|	CondLikeScaler_NUC4: 4by4 nucleotide model with or without rate
|		variation
|
-----------------------------------------------------------------*/
int CondLikeScaler_NUC4 (TreeNode *p, int division, int chain)

{
	int				c, i, j, k;
	CLFlt			scaler, *clP, *scPNew, *scPOld, *lnScaler;
	ModelInfo		*m;
	
	m = &modelSettings[division];

	/* find conditional likelihood pointer */
	clP = condLikePtr[chain][p->index] + m->condLikeStart + Bit(division, &p->clSpace[0]) * condLikeRowSize;
	
	scPNew = nodeScaler[chain] + m->compCharStart + (2 * (p->index - numLocalTaxa) +  Bit(division, &p->clSpace[0]   )) * numCompressedChars;
	scPOld = nodeScaler[chain] + m->compCharStart + (2 * (p->index - numLocalTaxa) + (Bit(division, &p->clSpace[0])^1)) * numCompressedChars;

	lnScaler = treeScaler[chain] + m->compCharStart + state[chain] * numCompressedChars;

	/* divide by old values; these can also be associated with old scaler nodes */
	if (Bit(division, &p->scalersSet[0]) == 1)
		{
		for (c=0; c<m->numChars; c++)
			lnScaler[c] -= scPOld[c];	/* subtract old value from tree scaler */
		FlipOneBit(division,&p->scalersSet[0]);	/* clear flag marking scalers set */
		}

	/* multiply by new values only if currently a scaler node */
	if (p->scalerNode == YES)
		{
		i = j = 0;
		for (c=0; c<m->numChars; c++)
			{
			scaler = 0.0;
			for (k=0; k<m->numGammaCats; k++)
				{
				if (clP[i] > scaler)
					scaler = clP[i];
				i++;
				if (clP[i] > scaler)
					scaler = clP[i];
				i++;
				if (clP[i] > scaler)
					scaler = clP[i];
				i++;
				if (clP[i] > scaler)
					scaler = clP[i];
				i++;
				}

			for (k=0; k<m->numGammaCats; k++)
				{
				clP[j++] /= scaler;
				clP[j++] /= scaler;
				clP[j++] /= scaler;
				clP[j++] /= scaler;
				}

			scPNew[c] = (CLFlt) log (scaler);			/* store node scaler */
			lnScaler[c] += scPNew[c];		/* add into tree scaler  */
			}

		FlipOneBit(division,&p->scalersSet[0]);	/* set flag marking scalers set */
		}
	return NO_ERROR;
	
}





#else
/*----------------------------------------------------------------
|
|	CondLikeScaler_NUC4_fast: 4by4 nucleotide model with or without rate
|		variation using fast log approximation
|
-----------------------------------------------------------------*/
int CondLikeScaler_NUC4_fast (TreeNode *p, int division, int chain)

{
	int				c, i, j, k, x, *expPtr, log_2;
	CLFlt			scaler, *scPNew, *scPOld, *lnScaler, *clP;
	ModelInfo		*m;
	
	m = &modelSettings[division];

	/* find conditional likelihood pointer */
	clP = condLikePtr[chain][p->index] + m->condLikeStart + Bit(division, &p->clSpace[0]) * condLikeRowSize;
	
	scPNew = nodeScaler[chain] + m->compCharStart + (2 * (p->index - numLocalTaxa) +  Bit(division, &p->clSpace[0]   )) * numCompressedChars;
	scPOld = nodeScaler[chain] + m->compCharStart + (2 * (p->index - numLocalTaxa) + (Bit(division, &p->clSpace[0])^1)) * numCompressedChars;

	lnScaler = treeScaler[chain] + m->compCharStart + state[chain] * numCompressedChars;

	/* divide by old values; these can also be associated with old scaler nodes */
	if (Bit(division, &p->scalersSet[0]) == 1)
		{
		for (c=0; c<m->numChars; c++)
			lnScaler[c] -= scPOld[c];	/* subtract old value from tree scaler */
		FlipOneBit(division,&p->scalersSet[0]);	/* clear flag marking scalers set */
		}

	/* multiply by new values only if currently a scaler node */
	if (p->scalerNode == YES)
		{
		i = j = 0;
		for (c=0; c<m->numChars; c++)
			{
			scaler = 0.0;
			for (k=0; k<m->numGammaCats; k++)
				{
				if (clP[i] > scaler)
					scaler = clP[i];
				i++;
				if (clP[i] > scaler)
					scaler = clP[i];
				i++;
				if (clP[i] > scaler)
					scaler = clP[i];
				i++;
				if (clP[i] > scaler)
					scaler = clP[i];
				i++;
				}

			if (scaler < 1E-10)
				{
				for (k=0; k<m->numGammaCats; k++)
					{
					clP[j++] /= scaler;
					clP[j++] /= scaler;
					clP[j++] /= scaler;
					clP[j++] /= scaler;
					}
			
				/* calculate fast log of scaler (max error below 0.007) */
				/* this code is due to Laurent de Soras (2001) through Peter Beerli */			
				expPtr = (int *) (&scaler);
				x = *expPtr;
				log_2 = ((x >> 23) & 255) - 128;
				x &= ~(255 << 23);
				x += 127 << 23;
				*expPtr = x;

				scaler = ((-1.0f/3) * scaler + 2) * scaler - 2.0f/3;	/* this line can be omitted for greater speed but lesser accuracy */
				scaler += log_2;
				scaler *= 0.69314718f;

				scPNew[c] = scaler;			/* store log of node scaler */
				lnScaler[c] += scPNew[c];		/* add into tree scaler  */
				}
			else
				{
				j += 4*m->numGammaCats;
				scPNew[c] = 0.0;	/* do not scale */
				}
			}

		FlipOneBit(division,&p->scalersSet[0]);	/* set flag marking scalers set */
		}
	return NO_ERROR;
	
}
#endif





/*----------------------------------------------------------------
|
|	CondLikeScaler_NY98: codon model with omega variation
|
-----------------------------------------------------------------*/
int CondLikeScaler_NY98 (TreeNode *p, int division, int chain)

{

	int				c, i, j, k, n, nStates;
	CLFlt			scaler, *clP, *scPNew, *scPOld, *lnScaler;
	ModelInfo		*m;

	m = &modelSettings[division];
	nStates = m->numModelStates;

	/* find conditional likelihood pointer */
	clP = condLikePtr[chain][p->index] + m->condLikeStart + Bit(division, &p->clSpace[0]) * condLikeRowSize;

	scPNew = nodeScaler[chain] + m->compCharStart + (2 * (p->index - numLocalTaxa) +  Bit(division, &p->clSpace[0]   )) * numCompressedChars;
	scPOld = nodeScaler[chain] + m->compCharStart + (2 * (p->index - numLocalTaxa) + (Bit(division, &p->clSpace[0])^1)) * numCompressedChars;

	lnScaler = treeScaler[chain] + m->compCharStart + state[chain] * numCompressedChars;

	/* subtract old values; these can also be associated with old scaler nodes */
	if (Bit(division,&p->scalersSet[0]) == 1)
		{
		for (c=0; c<m->numChars; c++)
			lnScaler[c] -= scPOld[c];	/*subtract old value */
		FlipOneBit(division,&p->scalersSet[0]);
		}

	/* add new values only if currently a scaler node */
	if (p->scalerNode == YES)
		{
		i = j = 0;
		for (c=0; c<m->numChars; c++)
			{
			scaler = 0.0;
			for (k=0; k<m->numOmegaCats; k++)
				{
				for (n=0; n<nStates; n++)
					{
					if (clP[i] > scaler)
						scaler = clP[i];
					i++;
					}
				}
			for (k=0; k<m->numOmegaCats; k++)
				{
				for (n=0; n<nStates; n++)
					clP[j++] /= scaler;
				}

			scPNew[c] = (CLFlt) log(scaler);	/* store scaler       */
			lnScaler[c] += scPNew[c];	/* add to site scaler */
			}
		FlipOneBit(division,&p->scalersSet[0]);
		}

	return (NO_ERROR);

}





/*----------------------------------------------------------------
|
|	CondLikeScaler_Std: variable states model with or without
|		rate variation
|
-----------------------------------------------------------------*/
int CondLikeScaler_Std (TreeNode *p, int division, int chain)

{

	int				a, c, i, j, k, nStates, numReps;
	CLFlt			scaler, *clP, *scPNew, *scPOld, *lnScaler;
	ModelInfo		*m;
	
	m = &modelSettings[division];

	/* find conditional likelihood pointer */
	clP = condLikePtr[chain][p->index] + m->condLikeStart + Bit(division, &p->clSpace[0]) * condLikeRowSize;
	
	scPNew = nodeScaler[chain] + m->compCharStart + (2 * (p->index - numLocalTaxa) +  Bit(division, &p->clSpace[0]   )) * numCompressedChars;
	scPOld = nodeScaler[chain] + m->compCharStart + (2 * (p->index - numLocalTaxa) + (Bit(division, &p->clSpace[0])^1)) * numCompressedChars;

	lnScaler = treeScaler[chain] + m->compCharStart + state[chain] * numCompressedChars;

	/* subtract old values; these can also be associated with old scaler nodes */
	if (Bit (division, &p->scalersSet[0]) == 1)
		{
		for (c=0; c<m->numChars; c++)
			lnScaler[c] -= scPOld[c];	/*subtract old value */
		FlipOneBit (division, &p->scalersSet[0]);
		}

	/* add new values only if currently a scaler node */
	if (p->scalerNode == YES)
		{
		i = j = 0;
		for (c=0; c<m->numChars; c++)
			{
			scaler = 0.0;
			nStates = m->nStates[c];

			if (nStates == 2 && m->numBetaCats > 1)
				numReps = m->numGammaCats * m->numBetaCats;
			else
				numReps = m->numGammaCats;

			for (k=0; k<numReps; k++)
				{
				for (a=0; a<nStates; a++)
					{
					if (clP[i] > scaler)
						scaler = clP[i];
					i++;
					}
				}

			for (k=0; k<numReps; k++)
				{
				for (a=0; a<nStates; a++)
					clP[j++] /= scaler;
				}

			scPNew[c] = (CLFlt) log(scaler);	/* store scaler       */
			lnScaler[c] += scPNew[c];	/* add to site scaler */
			}

		FlipOneBit(division,&p->scalersSet[0]);
		}
		
	return NO_ERROR;
	
}





/*-----------------------------------------------------------------
|
|	CopyParams: copy parameters of touched divisions
|
-----------------------------------------------------------------*/
void CopyParams (int chain)

{

	int			i, j, fromState, toState;
	MrBFlt		*from, *to;
	ModelInfo	*m;
	Param		*p;

	/* copy all params                                               */
	/* now done for all vars, can also be done for only touched vars */
	/* but then m->upDateCl must be kept separate for each chain!    */
	for (i=0; i<numParams; i++)
		{
		p = &params[i];

		from = GetParamVals (p, chain, state[chain]);
		to = GetParamVals (p, chain, (state[chain] ^ 1));

		for (j=0; j<p->nValues; j++)
			to[j] = from[j];

		from = GetParamSubVals (p, chain, state[chain]);
		to = GetParamSubVals (p, chain, (state[chain] ^ 1));

		for (j=0; j<p->nSubValues; j++)
			to[j] = from[j];

		}

	/* copy division params (model settings) for chain */
	/* reset division update flags                     */
	fromState = 2 * chain + state[chain];
	toState   = 2 * chain + (state[chain] ^ 1);
	for (i=0; i<numCurrentDivisions; i++)
		{
		m = &modelSettings[i];
		m->lnLike[toState] = m->lnLike[fromState];
		if (m->parsModelId == YES)
			m->parsTreeLength[toState] = m->parsTreeLength[fromState];
		m->upDateCl = NO;
		}
		
	/* copy cijk information */
	fromState = state[chain];
	toState   = (state[chain] ^ 1);
	for (i=0; i<numCurrentDivisions; i++)
		{
		m = &modelSettings[i];
		m->cijkBits[chain][toState] = m->cijkBits[chain][fromState];
		m->upDateCijk[chain][toState]   = NO;
		m->upDateCijk[chain][fromState] = NO;
		}	

	return;

}





void CopySubtreeToTree (Tree *subtree, Tree *t)

{
	
        int			i, /*j,*/ k;
	TreeNode	*p, *q=NULL, *r;

	for (i=/*j=*/0; i<subtree->nNodes - 1; i++)
		{
		p = subtree->allDownPass[i];

		for (k=0; k<t->nNodes; k++)
			{
			q = t->allDownPass[k];
			if (q->index == p->index)
				break;
			}
		q->length = p->length;
		q->marked = YES;
		if (p->left != NULL && p->right != NULL)
			{
			for (k=0; k<t->nNodes; k++)
				{
				r = t->allDownPass[k];
				if (r->index == p->left->index)
					{
					q->left = r;
					r->anc = q;
					}
				else if (r->index == p->right->index)
					{
					q->right = r;
					r->anc = q;
					}
				}
			}
		}

	p = subtree->root;

	for (k=0; k<t->nNodes; k++)
		{
		q = t->allDownPass[k];
		if (q->index == p->index)
			break;
		}

	if (q->left->marked == YES)
		{
		for (k=0; k<t->nIntNodes; k++)
			{
			r = t->intDownPass[k];
			if (r->index == p->left->index)
				{
				q->left = r;
				r->anc = q;
				}
			}
		}
	else if (q->right->marked == YES)
		{
		for (k=0; k<t->nIntNodes; k++)
			{
			r = t->intDownPass[k];
			if (r->index == p->left->index)
				{
				q->right = r;
				r->anc = q;
				}
			}
		}
}





/*-----------------------------------------------------------------
|
|	CopyToTreeFromPolyTree: copies second tree (polytomous) to first
|		tree (used to initialize constrained starting trees)
|		An unrooted tree will rooted on outgroup
|		A rooted tree will be randomly rooted on a node below all
|			defined constraints
|
-----------------------------------------------------------------*/
int CopyToTreeFromPolyTree (Tree *to, PolyTree *from)

{

	int			i;
	PolyNode	*p;
	TreeNode	*q, *q1;


	/* copy nodes */
	for (i=0; i<from->nNodes; i++)
		{
		/* copy pointers */
		p  = from->nodes + i;
		q  = to->nodes + i;

		if (p->anc != NULL)
			q->anc = to->nodes + p->anc->memoryIndex;
		else
			q->anc = NULL;

		if (p->left != NULL)	
			q->left = to->nodes + p->left->memoryIndex;
		else
			q->left = NULL;

		if (p->left != NULL)
			q->right = to->nodes + p->left->sib->memoryIndex;
		else
			q->right = NULL;

		q->memoryIndex			  = p->memoryIndex; 
		q->index                  = p->index; 
		q->isLocked				  = p->isLocked;
		q->lockID				  = p->lockID;
		q->isDated				  = p->isDated;
		q->age					  = p->age;
		strcpy(q->label, p->label);
		}
	
	/* fix root */
	if (to->isRooted == NO)
		{
		p = from->root;
		q = to->nodes + p->memoryIndex;
		q->anc = to->root = to->nodes + p->left->sib->sib->memoryIndex;
		to->root->left = q;
		to->root->right = to->root->anc = NULL;
		to->nNodes = from->nNodes;
		to->nIntNodes = from->nIntNodes;
		}
	else
		{
		p = from->root;
		q = to->nodes + p->memoryIndex;
		q1 = to->nodes + from->nNodes;
		q->anc = q1;
		q1->left = q;
		q1->right = q1->anc = NULL;
		q1->memoryIndex = from->nNodes;
		q1->index = from->nNodes + 1;
		q1->isLocked = NO;
		q1->lockID = -1;
		q1->isDated = NO;
		q1->age = -1.0;
		to->root = q1;
		to->nNodes = from->nNodes + 1;
		to->nIntNodes = from->nIntNodes;
		}

	/* get downpass */
	GetDownPass (to);

	/* reset interior indices because they will be weird in the polytomous tree */
	for (i=0; i<to->nIntNodes; i++)
		to->intDownPass[i]->index = i + numLocalTaxa;

	return (NO_ERROR);
		
}




/*-----------------------------------------------------------------
|
|	CopyToTreeFromTree: copies second tree to first tree
|		(used to initialize brlen sets for same topology)
|
-----------------------------------------------------------------*/
int CopyToTreeFromTree (Tree *to, Tree *from)

{

	int			i, j;
	TreeNode	*p, *q;

	/* copy nodes */
	for (i=0; i<from->nNodes; i++)
		{
		/* copy pointers */
		p  = from->nodes + i;
		q  = to->nodes + i;

		if (p->anc != NULL)
			q->anc = to->nodes + p->anc->memoryIndex;
		else
			q->anc = NULL;

		if (p->left != NULL)	
			q->left = to->nodes + p->left->memoryIndex;
		else
			q->left = NULL;

		if (p->right != NULL)	
			q->right = to->nodes + p->right->memoryIndex;
		else
			q->right = NULL;

		q->memoryIndex			  = p->memoryIndex; 
		q->index                  = p->index; 
		q->scalerNode			  = p->scalerNode;			
		q->upDateCl               = p->upDateCl	= NO;	/* reset update cond like flag  */
		q->upDateTi				  = p->upDateTi = NO;	/* reset update trans prob flag */
		for (j=0; j<MAX_NUM_DIV_LONGS; j++)
			{
			q->clSpace[j]         = p->clSpace[j];
			q->tiSpace[j]         = p->tiSpace[j];
			q->scalersSet[j]      = p->scalersSet[j]; 
			}
		q->marked                 = p->marked;
		q->length                 = p->length;
		q->nodeDepth              = p->nodeDepth;
		q->x                      = p->x;
		q->y                      = p->y;
		q->isDated				  = p->isDated;
		q->age					  = p->age;
		q->isLocked				  = p->isLocked;
		q->lockID				  = p->lockID;
		strcpy (q->label, p->label);
		}
	
	for (i=0; i<from->nIntNodes; i++)
		{
		to->intDownPass[i] = to->nodes + from->intDownPass[i]->memoryIndex;
		}
	for (i=0; i<from->nNodes; i++)
		{
		to->allDownPass[i] = to->nodes + from->allDownPass[i]->memoryIndex;
		}

	to->root = to->nodes + from->root->memoryIndex;
	to->clockRate = from->clockRate;
	/* rest of tree info is constant and need not be copied */
	
	return (NO_ERROR);
		
}




/*-----------------------------------------------------------------
|
|	CopyTrees: copies touched trees for chain
|		resets node update flags in the process
|
-----------------------------------------------------------------*/
void CopyTrees (int chain)

{

	int			i, j, n, fromState, toState;
	TreeNode	*p, *q;
	Tree		*from, *to;

	for (n=0; n<numTrees; n++)
		{
		from = GetTreeFromIndex (n, chain, state[chain]);		
		to = GetTreeFromIndex (n, chain, (state[chain]^1));

		/* copy nodes */
		for (j=0; j<from->nNodes; j++)
			{
			/* copy pointers */
			p  = from->nodes + j;
			q  = to->nodes + j;

			if (p->anc != NULL)
				q->anc = to->nodes + p->anc->memoryIndex;
			else
				q->anc = NULL;

			if (p->left != NULL)	
				q->left = to->nodes + p->left->memoryIndex;
			else
				q->left = NULL;

			if (p->right != NULL)	
				q->right = to->nodes + p->right->memoryIndex;
			else
				q->right = NULL;

			q->memoryIndex			  = p->memoryIndex; 
			q->index                  = p->index; 
			q->scalerNode			  = p->scalerNode;			
			q->upDateCl               = p->upDateCl	= NO;	/* reset update cond like flag  */
			q->upDateTi				  = p->upDateTi = NO;	/* reset update trans prob flag */
			for (i=0; i<MAX_NUM_DIV_LONGS; i++)
				{
				q->clSpace[i]         = p->clSpace[i];
				q->tiSpace[i]         = p->tiSpace[i];
				q->scalersSet[i]      = p->scalersSet[i]; 
				}
			q->marked                 = p->marked;
			q->length                 = p->length;
			q->nodeDepth              = p->nodeDepth;
			q->x                      = p->x;
			q->y                      = p->y;
			q->isDated				  = p->isDated;
			q->age					  = p->age;
			q->isLocked				  = p->isLocked;
			q->lockID				  = p->lockID;
			strcpy (q->label, p->label);
			}
		
		for (i=0; i<from->nIntNodes; i++)
			{
			to->intDownPass[i] = to->nodes + from->intDownPass[i]->memoryIndex;
			}
		for (i=0; i<from->nNodes; i++)
			{
			to->allDownPass[i] = to->nodes + from->allDownPass[i]->memoryIndex;
			}

		to->root = to->nodes + from->root->memoryIndex;
		to->clockRate = from->clockRate;
		/* rest of tree info is constant and need not be copied */
		
		}

	/* now copy tree scalers for divisions */
	if (treeScaler)
		{
		fromState = state[chain] * numCompressedChars;
		toState = (state[chain] ^ 1) * numCompressedChars;
		for (i=0; i<numCompressedChars; i++)
			treeScaler[chain][toState+i] = treeScaler[chain][fromState+i];
		}

	return;
		
}




void CopyTreeToSubtree (Tree *t, Tree *subtree)

{
	
	int			i, j, k;
	TreeNode	*p, *q, *r;

	for (i=j=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->marked == NO)
			continue;

		q = &subtree->nodes[j++];
		q->index = p->index;
		q->length = p->length;
		for (k=0; k<MAX_NUM_DIV_LONGS; k++)
			q->clSpace[k] = p->clSpace[k];
		if (p->left == NULL || p->left->marked == NO)
			q->left = q->right = NULL;
		else
			{
			for (k=0; k<j-1; k++)
				{
				r = &subtree->nodes[k];
				if (r->index == p->left->index)
					{
					q->left = r;
					r->anc = q;
					}
				else if (r->index == p->right->index)
					{
					q->right = r;
					r->anc = q;
					}
				}
			}
		
		if (p->anc->marked == NO)
			{
			r = &subtree->nodes[j++];
			subtree->root = r;
			r->anc = r->right = NULL;
			r->left = q;
			q->anc = r;
			r->length = 0.0;
			r->index = p->anc->index;
			for (k=0; k<MAX_NUM_DIV_LONGS; k++)
				r->clSpace[k] = p->anc->clSpace[k];
			}

		}

	GetDownPass (subtree);

	subtree->isRooted = t->isRooted;
	subtree->nRelParts = t->nRelParts;
	subtree->relParts = t->relParts;
}




/*---------------------------------------------------------------------------
|
|	CreateParsMatrix: create parsimony (bitset) matrix
|		(this version without compression)
|
----------------------------------------------------------------------------*/
int CreateParsMatrix (void)

{

	int				i, j, k, d, nParsStatesForCont, nuc1, nuc2, nuc3, newColumn,
					codingNucCode, allNucCode, allAmbig;
	long			x, x1, x2, x3, *longPtr;
	ModelInfo		*m;
	ModelParams		*mp;

	/* this variable determines how many parsimony states are used           */
	/* to represent continuous characters (determines weight of these chars) */
	nParsStatesForCont = 3;

	/* create the parsimony (bitset) matrix */
	/* first find out how large it is and set division pointers */
	parsMatrixRowSize = 0;
	for (d=0; d<numCurrentDivisions; d++)
		{
		MrBayesPrint ("%s   Creating parsimony (bitset) matrix for division %d\n", spacer, d+1);

		m = &modelSettings[d];
		mp = &modelParams[d];
		m->parsMatrixStart = parsMatrixRowSize;

		/* find how many parsimony ints (long) are needed for each model site */
		if (mp->dataType == CONTINUOUS)
			{
			/* scale continuous characters down to an ordered parsimony character */
			/* with nParsStatesForCont states, represent this character as a set */
			/* of binary characters by additive binary coding */
			m->nParsIntsPerSite = nParsStatesForCont - 1;
			}
		else
			m->nParsIntsPerSite = 1 + mp->nStates / nBitsInALong;
		parsMatrixRowSize += m->nParsIntsPerSite * m->numChars;

		m->parsMatrixStop = parsMatrixRowSize;
		}
		
	/* then allocate space for it */
	if (memAllocs[ALLOC_PARSMATRIX] == YES)
		{
		MrBayesPrint ("%s   parsMatrix not free in CreateParsMatrix\n", spacer);
		return (ERROR);
		}
	parsMatrix = (long *) calloc (parsMatrixRowSize * numLocalTaxa, sizeof(long));
	if (!parsMatrix)
		{
		MrBayesPrint ("%s   Problem allocating parsMatrix\n", spacer);
		return (ERROR);
		}
	memAllocs[ALLOC_PARSMATRIX] = YES;
	
	/* and fill it in */
	newColumn = 0;
	for (d=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];
		mp = &modelParams[d];

		if (mp->dataType == CONTINUOUS)
			{
			for (i=0; i<numLocalTaxa; i++)
				{
				newColumn = m->parsMatrixStart;
				for (j=m->compMatrixStart; j<m->compMatrixStop; j++)
					{
					x = compMatrix[pos(i,j,compMatrixRowSize)];

					for (k=0; k<nParsStatesForCont - 1; k++)
						{
						if (x > (k + 1) * 1000 / nParsStatesForCont)
							parsMatrix[pos(i,newColumn,parsMatrixRowSize)] = 1;
						else
							parsMatrix[pos(i,newColumn,parsMatrixRowSize)] = 2;

						newColumn++;
						}
					}
				}
			}
		else if (m->nCharsPerSite == 1 && m->nParsIntsPerSite == 1)
			{
			allAmbig = (1<<mp->nStates) - 1;
			for (i=0; i<numLocalTaxa; i++)
				{
				newColumn = m->parsMatrixStart;
				for (j=m->compMatrixStart; j<m->compMatrixStop; j++)
					{
					x = compMatrix[pos(i,j,compMatrixRowSize)];

					if (x == MISSING || x == GAP)
						parsMatrix[pos(i,newColumn,parsMatrixRowSize)] = allAmbig;
					else
						parsMatrix[pos(i,newColumn,parsMatrixRowSize)] = x;

					newColumn++;
					}
				}
			}
		else if (!strcmp(mp->nucModel, "Doublet") && (mp->dataType == DNA || mp->dataType == RNA))
			{
			allAmbig = 15;
			for (i=0; i<numLocalTaxa; i++)
				{
				newColumn = m->parsMatrixStart;
				for (j=m->compMatrixStart; j<m->compMatrixStop; j+=m->nCharsPerSite)
					{
					/* fetch the original values x1 and x2 */
					x1 = compMatrix[pos(i,j,compMatrixRowSize)];
					if (x1 == MISSING || x1 == GAP)
						x1 = allAmbig;
					x2 = compMatrix[pos(i,j+1,compMatrixRowSize)];
					if (x2 == MISSING || x2 == GAP)
						x2 = allAmbig;
					/* squeeze them together in the new value x */
					x = 0;
					for (nuc1=0; nuc1<4; nuc1++)
						{
						for (nuc2=0; nuc2<4; nuc2++)
							{
							if (IsBitSet(nuc1,&x1) == YES && IsBitSet(nuc2, &x2) == YES)
								x |= (1<<(nuc1*4 + nuc2));
							}
						}
					
					parsMatrix[pos(i,newColumn,parsMatrixRowSize)] = x;
					newColumn++;
					}
				}
			}
		else if (!strcmp(mp->nucModel, "Codon") && (mp->dataType == DNA || mp->dataType == RNA))
			{
			allAmbig = 15;
			for (i=0; i<numLocalTaxa; i++)
				{
				newColumn = m->parsMatrixStart;
				for (j=m->compMatrixStart; j<m->compMatrixStop; j+=m->nCharsPerSite)
					{
					/* fetch the original values x1, x2, and x3*/
					x1 = compMatrix[pos(i,j,compMatrixRowSize)];
					if (x1 == MISSING || x1 == GAP)
						x1 = allAmbig;
					x2 = compMatrix[pos(i,j+1,compMatrixRowSize)];
					if (x2 == MISSING || x2 == GAP)
						x2 = allAmbig;
					x3 = compMatrix[pos(i,j+2,compMatrixRowSize)];
					if (x3 == MISSING || x3 == GAP)
						x3 = allAmbig;

					/* squeeze them together in the new long string pointed to by longPtr */
					longPtr = parsMatrix + pos(i,newColumn,parsMatrixRowSize);
					allNucCode = codingNucCode = 0;
					for (nuc1=0; nuc1<4; nuc1++)
						for (nuc2=0; nuc2<4; nuc2++)
							for (nuc3=0; nuc3<4; nuc3++)
								{
								if (mp->codon[allNucCode] != 21)
									{
									if (IsBitSet(nuc1, &x1) == YES && IsBitSet(nuc2, &x2) == YES && IsBitSet(nuc3, &x3) == YES)
										SetBit(codingNucCode, longPtr);
									codingNucCode++;
									}
								allNucCode++;
								}

					newColumn += m->nParsIntsPerSite;
					}
				}
			}
		else
			{
			MrBayesPrint ("%s   Unrecognized data format during bitset compression\n");
			return ERROR;
			}
		}

	/* print bitset matrix */
#	if	defined (DEBUG_CREATEPARSMATRIX)
	PrintParsMatrix();
	getchar();
#	endif		

	return NO_ERROR;

}



#ifdef VISUAL
BOOL WINAPI CatchInterrupt2(DWORD signum) 
#else
void CatchInterrupt(int signum)
#endif
{
	if(signum==SIGINT)
		confirmAbortRun = YES;
#ifdef VISUAL
	return TRUE;
#endif
}





int requestAbortRun(void) 

{
	int	c;
	int ret=0;

	MrBayesPrint ("\n   Control C detected\n");
	MrBayesPrint ("   Do you really want to stop the run (y/n)?");
	do {
		c = getchar();
	} while (c == ' ');
	if (c == 'y' || c == 'Y')
		ret=1;
	else 
	{
		MrBayesPrint ("   Mcmc run continued ...\n\n");
		ret=0;
		confirmAbortRun = FALSE;
#	ifndef VISUAL
	/* to be safe: some implementations reset the signal handler after a signal */
	signal(SIGINT, CatchInterrupt);
#	endif
	}
	do {
		c = getchar();
	} while (c != '\n' && c != '\r');
	return ret;
}





int DoMcmc (void)

{

	long int		seed, numGlobalChains;
	int			rc;

#					if defined (MPI_ENABLED)
	int				i, testNumChains;
#					endif
#	ifndef VISUAL
	sighandler_t      sigint_oldhandler;
#	endif

	/* set file names */
	sumtParams.numRuns = chainParams.numRuns;
	sumpParams.numRuns = chainParams.numRuns;
	
	if (fileNameChanged == YES)
		{
		strcpy (sumtParams.sumtFileName, chainParams.chainFileName);
		strcpy (sumpParams.sumpFileName, chainParams.chainFileName);
		strcpy (sumpParams.sumpOutfile, sumpParams.sumpFileName);
		strcat (sumpParams.sumpOutfile, ".stat");

		if (chainParams.numRuns == 1)
			sprintf (comptreeParams.comptFileName1, "%s.run1.t", chainParams.chainFileName);
		else /* if (chainParams.numRuns > 1) */
			sprintf (comptreeParams.comptFileName1, "%s.t", chainParams.chainFileName);
		strcpy (comptreeParams.comptFileName2, comptreeParams.comptFileName1);

		if (chainParams.numRuns == 1)
			sprintf (plotParams.plotFileName, "%s.run1.p", chainParams.chainFileName);
		else /* if (chainParams.numRuns > 1) */
			sprintf (plotParams.plotFileName, "%s.p", chainParams.chainFileName);

		if (chainParams.numRuns > 1)
			MrBayesPrint ("%s   Setting chain output file name to \"%s.<run<i>.p/run<i>.t>\"\n", spacer, chainParams.chainFileName);
		else
			MrBayesPrint ("%s   Setting chain output file name to \"%s.<p/t>\"\n", spacer, chainParams.chainFileName);

		fileNameChanged = NO;
		}

	/* Check to see that we have a data matrix. Otherwise, the MCMC is rather
	   pointless. */
	if (defMatrix == NO)
		{
		MrBayesPrint ("%s   A character matrix must be defined first\n", spacer);
		goto errorExit;
		}
	MrBayesPrint ("%s   Running Markov chain\n", spacer);
	
	/* Check the chain temperature parameters */
	if (CheckTemperature () == ERROR)
		goto errorExit;
	
	/* Set the chain random number seeds here. We have two seeds. One
	   (called swapSeed) is only used to determine which two chains 
	   will swap states in the next trial. The other (called seed) is
	   the seed for our work-horse pseudorandom number generator. Note
	   that if we are doing MPI, we want the swap seed to be the same
	   for every processor. This is taken care of when we initialize
	   things in the program. If we are doing MPI, we also want to make
	   certain that seed is different for every processor. */
#	if defined (MPI_ENABLED)
	seed = chainParams.chainSeed + (proc_id + 1);
#	else
	seed = chainParams.chainSeed;
#	endif

	/* Get a unique identifier (stamp) for this run. This is used as
	   an identifier for each mcmc analysis. It uses runIDSeed to initialize 
	   the stamp. All of the processors should have the same seed, so 
	   this should be safe. */
	GetStamp ();
	
	MrBayesPrint ("%s   Seed = %d\n", spacer, chainParams.chainSeed);
	MrBayesPrint ("%s   Swapseed = %d\n", spacer, swapSeed);

	/* Check the model settings. This sets up a parameter table, which
	   we can interpet later. */
	if (CheckModel() == ERROR)
		goto errorExit;
	MrBayesPrint ("\n");
				
	/* Set up rates for standard amino acid models, in case we need them. */
	if (SetAARates () == ERROR)
		goto errorExit;
		
	/* Determine the number of chains */
	numGlobalChains = chainParams.numRuns * chainParams.numChains;
#	if defined (MPI_ENABLED)
	/* tell user how many chains each processor has been assigned */
	if (num_procs > numGlobalChains)
		{
		MrBayesPrint ("%s   The number of chains must be at least as great\n", spacer);
		MrBayesPrint ("%s   as the number of processors (%d)\n", spacer, num_procs);
		goto errorExit;
		}
	if (proc_id == 0)
		{
		for (i=0; i<num_procs; i++)
			{
			testNumChains = (int)(numGlobalChains / num_procs);
			if (i < (numGlobalChains % num_procs))
				testNumChains++;
			MrBayesPrint ("%s   Number of chains on processor %d = %d\n", spacer, i+1, testNumChains);
			}
		}
		
	/* Try to evenly distribute the chains on all processors. */
	numLocalChains = (int)(numGlobalChains / num_procs);

	/* If there are any chains remaining, distribute them
	   in order starting with proc 0. (This may cause a load imbalance.) */
	if (proc_id < (numGlobalChains % num_procs))
		numLocalChains++;
#	else
	numLocalChains = numGlobalChains;
#	endif
	if (numLocalChains < 1)
		return (NO_ERROR);
		
	/* Check that the settings for doublet or codon models are correct. */
	if (CheckExpandedModels() == ERROR)
		goto errorExit;

	/* How many taxa are included in the analysis? */
	numLocalTaxa = NumNonExcludedTaxa ();
	if (numLocalTaxa <= 0)
		goto errorExit;
	MrBayesPrint ("%s   Number of taxa = %d\n", spacer, numLocalTaxa);
		
	/* How many characters are included in the analysis? */
	numLocalChar = NumNonExcludedChar ();
	MrBayesPrint ("%s   Number of characters = %d\n", spacer, numLocalChar);
	
	/* Set up move types. */
	SetUpMoveTypes();
		
	/* Compress data and calculates some things needed for setting up params. */
	if (CompressData() == ERROR)
		goto errorExit;

	/* Add dummy characters, if needed. */
	if (AddDummyChars() == ERROR)
		goto errorExit;

	/* Process standard characters (calculates bsIndex, tiIndex, and more). */
	if (ProcessStdChars() == ERROR)
		goto errorExit;

	/* Set up modelinfo and parameters for the chain. */
	if (SetChainParams () == ERROR)
		goto errorExit;

	/* Fill in normal parameters. */
	if (FillNormalParams (&seed) == ERROR)
		goto errorExit;

	/* Fill in trees. */
	if (FillTreeParams (&seed) == ERROR)
		goto errorExit;

	/* Set the moves to be used by the chain. */
	if (SetMoves () == ERROR)
		goto errorExit;

	/* Set the likelihood function pointers. */
	if (SetLikeFunctions () == ERROR)
		goto errorExit;

	/* Set up number of characters of each character pattern. */
	if (FillNumSitesOfPat () == ERROR)
		goto errorExit;

	/* Create a bitset matrix, used for setting conditional likelihoods. */
	if (CreateParsMatrix() == ERROR)
		goto errorExit;

	/* Set up a terminal state index matrix for local compression. */
	if (SetUpTermState() == ERROR)
		goto errorExit;

	/* Initialize conditional likelihoods for terminals. */
	if (InitTermCondLikes() == ERROR)
		goto errorExit;

	/* Initialize invariable conditional likelihoods. */
	if (InitInvCondLikes() == ERROR)
		goto errorExit;

	/* Initialize conditional likelihoods and transition probabilities for chain (the working space). */
	if (InitChainCondLikes () == ERROR)
		goto errorExit;
	
	/* Initialize space for SPR parsimony state sets for chain. */
	if (InitSprParsSets () == ERROR)
		goto errorExit;

	/* Initialize space for parsimony state sets for chain (if needed). */
	if (InitParsSets () == ERROR)
		goto errorExit;

	/*! setup a signal handler to catch interrupts, ignore failure */
#ifdef VISUAL
	SetConsoleCtrlHandler(CatchInterrupt2, TRUE);
#else
	sigint_oldhandler = signal(SIGINT, CatchInterrupt);
#endif
	confirmAbortRun = NO;

	/* Run the Markov chain. */
	rc = RunChain (&seed);
	if (rc == ERROR)
		goto errorExit;
	else if (rc == ABORT)
		{
		FreeChainMemory();
		return ABORT;
		}
		
	/*! restore the default signal handler */
#ifdef VISUAL
	SetConsoleCtrlHandler(CatchInterrupt2, FALSE);
#else
	if(sigint_oldhandler!=SIG_ERR) 
	  signal(SIGINT, sigint_oldhandler);
#endif

	/* Reset the global seed at end of chain. We don't want successive
	   chains to all start with the same random number seed. */
	chainParams.chainSeed = seed;
	globalSeed = seed;

	/* Free up all memory allocated for the chain. */
	FreeChainMemory ();
	
	return (NO_ERROR);
	
	errorExit:
		FreeChainMemory ();
		return (ERROR);
	
}





int DoMcmcp (void)

{

	if (defMatrix == NO)
		{
		MrBayesPrint ("%s   A character matrix must be defined first\n", spacer);
		return (ERROR);
		}

	sumtParams.numRuns = chainParams.numRuns;
	sumpParams.numRuns = chainParams.numRuns;
	
	if (fileNameChanged == YES)
		{
		strcpy (sumtParams.sumtFileName, chainParams.chainFileName);
		strcpy (sumpParams.sumpFileName, chainParams.chainFileName);
		strcpy (sumpParams.sumpOutfile, sumpParams.sumpFileName);
		strcat (sumpParams.sumpOutfile, ".stat");

		if (chainParams.numRuns == 1)
			sprintf (comptreeParams.comptFileName1, "%s.run1.t", chainParams.chainFileName);
		else /* if (chainParams.numRuns > 1) */
			sprintf (comptreeParams.comptFileName1, "%s.t", chainParams.chainFileName);
		strcpy (comptreeParams.comptFileName2, comptreeParams.comptFileName1);

		if (chainParams.numRuns == 1)
			sprintf (plotParams.plotFileName, "%s.run1.p", chainParams.chainFileName);
		else /* if (chainParams.numRuns > 1) */
			sprintf (plotParams.plotFileName, "%s.p", chainParams.chainFileName);

		if (chainParams.numRuns > 1)
			MrBayesPrint ("%s   Setting chain output file name to \"%s.<run<i>.p/run<i>.t>\"\n", spacer, chainParams.chainFileName);
		else
			MrBayesPrint ("%s   Setting chain output file name to \"%s.<p/t>\"\n", spacer, chainParams.chainFileName);

		fileNameChanged = NO;
		}

	MrBayesPrint ("%s   Successfully set chain parameters\n", spacer);

	return (NO_ERROR);
	
}





int DoMcmcParm (char *parmName, char *tkn)

{

	int			tempI;
	MrBFlt		tempD;
	char		tempStr[100];
	
	if (defMatrix == NO)
		{
		MrBayesPrint ("%s   A character matrix must be defined first\n", spacer);
		return (ERROR);
		}

	if (expecting == Expecting(PARAMETER))
		{
		expecting = Expecting(EQUALSIGN);
		}
	else
		{
		/* set Seed (chainSeed) ***************************************************************/
		if (!strcmp(parmName, "Seed"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(NUMBER);
			else if (expecting == Expecting(NUMBER))
				{
				sscanf (tkn, "%d", &tempI);
				chainParams.chainSeed = tempI;
				MrBayesPrint ("%s   Setting chain seed to %ld\n", spacer, chainParams.chainSeed);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Swapseed (global variable swapSeed) ***************************************************************/
		else if (!strcmp(parmName, "Swapseed"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(NUMBER);
			else if (expecting == Expecting(NUMBER))
				{
				sscanf (tkn, "%d", &tempI);
				swapSeed = tempI;
				MrBayesPrint ("%s   Setting swapseed to %ld\n", spacer, swapSeed);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set run ID */
		/* this setting is provided for GRID use only, so that identical runs can be generated */
		else if (!strcmp(parmName, "Runidseed"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(NUMBER);
			else if (expecting == Expecting(NUMBER))
				{
				sscanf (tkn, "%d", &tempI);
				runIDSeed = tempI;
				MrBayesPrint ("%s   Setting run ID [stamp] seed to %ld [for GRID use]\n", spacer, swapSeed);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Ngen (numGen) ******************************************************************/
		else if (!strcmp(parmName, "Ngen"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(NUMBER);
			else if (expecting == Expecting(NUMBER))
				{
				sscanf (tkn, "%d", &tempI);
				if (tempI < 1)
					{
					MrBayesPrint ("%s   Too few generations\n", spacer);
					return (ERROR);
					}
				chainParams.numGen = tempI;
				MrBayesPrint ("%s   Setting number of generations to %d\n", spacer, chainParams.numGen);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Samplefreq (sampleFreq) ********************************************************/
		else if (!strcmp(parmName, "Samplefreq"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(NUMBER);
			else if (expecting == Expecting(NUMBER))
				{
				sscanf (tkn, "%d", &tempI);
				if (tempI < 1)
					{
					MrBayesPrint ("%s   Sampling chain too infrequently\n", spacer);
					return (ERROR);
					}
				chainParams.sampleFreq = tempI;
				MrBayesPrint ("%s   Setting sample frequency to %d\n", spacer, chainParams.sampleFreq);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Printfreq (printFreq) **********************************************************/
		else if (!strcmp(parmName, "Printfreq"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(NUMBER);
			else if (expecting == Expecting(NUMBER))
				{
				sscanf (tkn, "%d", &tempI);
				if (tempI < 1)
					{
					MrBayesPrint ("%s   Printing to screen too infrequently\n", spacer);
					return (ERROR);
					}
				chainParams.printFreq = tempI;
				MrBayesPrint ("%s   Setting print frequency to %d\n", spacer, chainParams.printFreq);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Printmax (printMax) **********************************************************/
		else if (!strcmp(parmName, "Printmax"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(NUMBER);
			else if (expecting == Expecting(NUMBER))
				{
				sscanf (tkn, "%d", &tempI);
				if (tempI < 1)
					{
					MrBayesPrint ("%s   You need to print at least one chain\n", spacer);
					return (ERROR);
					}
				chainParams.printMax = tempI;
				MrBayesPrint ("%s   Setting maximum number of chains to print to screen to %d\n", spacer, chainParams.printMax);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Printall (printAll) ********************************************************/
		else if (!strcmp(parmName, "Printall"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(ALPHA);
			else if (expecting == Expecting(ALPHA))
				{
				if (IsArgValid(tkn, tempStr) == NO_ERROR)
					{
					if (!strcmp(tempStr, "Yes"))
						chainParams.printAll = YES;
					else
						chainParams.printAll = NO;
					}
				else
					{
					MrBayesPrint ("%s   Invalid argument for Printall\n", spacer);
					return (ERROR);
					}
				if (chainParams.allChains == YES)
					MrBayesPrint ("%s   Printing all chains to screen\n", spacer);
				else
					MrBayesPrint ("%s   Printing only cold chains to screen\n", spacer);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Swapfreq (swapFreq) ************************************************************/
		else if (!strcmp(parmName, "Swapfreq"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(NUMBER);
			else if (expecting == Expecting(NUMBER))
				{
				sscanf (tkn, "%d", &tempI);
				if (tempI < 1)
					{
					MrBayesPrint ("%s   Swapping states too infrequently\n", spacer);
					return (ERROR);
					}
				chainParams.swapFreq = tempI;
				MrBayesPrint ("%s   Setting swap frequency to %d\n", spacer, chainParams.swapFreq);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Nswaps (numSwaps) ************************************************************/
		else if (!strcmp(parmName, "Nswaps"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(NUMBER);
			else if (expecting == Expecting(NUMBER))
				{
				sscanf (tkn, "%d", &tempI);
				if (tempI < 1)
					{
					MrBayesPrint ("%s   There must be at least one swap per swapping cycle\n", spacer);
					return (ERROR);
					}
				chainParams.swapFreq = tempI;
				MrBayesPrint ("%s   Setting number of swaps per swapping cycle to %d\n", spacer, chainParams.numSwaps);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Allchains (allChains) ********************************************************/
		else if (!strcmp(parmName, "Allchains"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(ALPHA);
			else if (expecting == Expecting(ALPHA))
				{
				if (IsArgValid(tkn, tempStr) == NO_ERROR)
					{
					if (!strcmp(tempStr, "Yes"))
						chainParams.allChains = YES;
					else
						chainParams.allChains = NO;
					}
				else
					{
					MrBayesPrint ("%s   Invalid argument for Allchains\n", spacer);
					return (ERROR);
					}
				if (chainParams.allChains == YES)
					MrBayesPrint ("%s   Calculating MCMC diagnostics for all chains\n", spacer);
				else
					MrBayesPrint ("%s   Calculating MCMC diagnostics only for cold chain(s)\n", spacer);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Allcomps (allComps) ************************************************************/
		else if (!strcmp(parmName, "Allcomps"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(ALPHA);
			else if (expecting == Expecting(ALPHA))
				{
				if (IsArgValid(tkn, tempStr) == NO_ERROR)
					{
					if (!strcmp(tempStr, "Yes"))
						chainParams.allComps = YES;
					else
						chainParams.allComps = NO;
					}
				else
					{
					MrBayesPrint ("%s   Invalid argument for Allcomps\n", spacer);
					return (ERROR);
					}
				if (chainParams.allComps == YES)
					MrBayesPrint ("%s   Calculating MCMC diagnostics for all pairwise run comparisons\n", spacer);
				else
					MrBayesPrint ("%s   Only calculating overall MCMC diagnostics\n", spacer);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Mcmcdiagn (mcmcDiagn) ********************************************************/
		else if (!strcmp(parmName, "Mcmcdiagn"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(ALPHA);
			else if (expecting == Expecting(ALPHA))
				{
				if (IsArgValid(tkn, tempStr) == NO_ERROR)
					{
					if (!strcmp(tempStr, "Yes"))
						chainParams.mcmcDiagn = YES;
					else
						chainParams.mcmcDiagn = NO;
					}
				else
					{
					MrBayesPrint ("%s   Invalid argument for mcmc diagnostics\n", spacer);
					return (ERROR);
					}
				if (chainParams.saveBrlens == YES)
					MrBayesPrint ("%s   Calculating MCMC diagnostics\n", spacer);
				else
					MrBayesPrint ("%s   Not calculating MCMC diagnostics\n", spacer);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Diagnfreq (diagnFreq) ************************************************************/
		else if (!strcmp(parmName, "Diagnfreq"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(NUMBER);
			else if (expecting == Expecting(NUMBER))
				{
				sscanf (tkn, "%d", &tempI);
				if (tempI < 1)
					{
					MrBayesPrint ("%s   Diagnosing MCMC behavior too infrequently\n", spacer);
					return (ERROR);
					}
				chainParams.diagnFreq = tempI;
				MrBayesPrint ("%s   Setting diagnosing frequency to %d\n", spacer, chainParams.diagnFreq);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Minpartfreq (minPartFreq) ************************************************************/
		else if (!strcmp(parmName, "Minpartfreq"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(NUMBER);
			else if (expecting == Expecting(NUMBER))
				{
				sscanf (tkn, "%lf", &tempD);
				if (tempD < 0.01)
					{
					MrBayesPrint ("%s   Minimum partition frequency too low (< 0.01)\n", spacer);
					return (ERROR);
					}
				if (tempD > 0.8)
					{
					MrBayesPrint ("%s   Minimum partition frequency too high (> 0.8)\n", spacer);
					return (ERROR);
					}
				chainParams.minPartFreq = tempD;
				MrBayesPrint ("%s   Setting minimum partition frequency to %.2f\n", spacer, chainParams.minPartFreq);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Nruns (numRuns) ****************************************************************/
		else if (!strcmp(parmName, "Nruns"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(NUMBER);
			else if (expecting == Expecting(NUMBER))
				{
				sscanf (tkn, "%d", &tempI);
				if (tempI < 1)
					{
					MrBayesPrint ("%s   Too few runs (minimum of 1 run)\n", spacer);
					return (ERROR);
					}
				if (tempI > MAX_RUNS)
					{
					MrBayesPrint ("%s   Too many runs (maximum of %d runs)\n", spacer, MAX_RUNS);
					return (ERROR);
					}
				chainParams.numRuns = tempI;
				fileNameChanged = YES;
				MrBayesPrint ("%s   Setting number of runs to %d\n", spacer, chainParams.numRuns);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Nchains (numChains) ************************************************************/
		else if (!strcmp(parmName, "Nchains"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(NUMBER);
			else if (expecting == Expecting(NUMBER))
				{
				sscanf (tkn, "%d", &tempI);
				if (tempI < 1)
					{
					MrBayesPrint ("%s   Too few chains (minimum of 1 chain)\n", spacer);
					return (ERROR);
					}
				if (tempI > MAX_CHAINS)
					{
					MrBayesPrint ("%s   Too many chains (maximum of %d chains)\n", spacer, MAX_CHAINS);
					return (ERROR);
					}
				chainParams.numChains = tempI;
				MrBayesPrint ("%s   Setting number of chains to %d\n", spacer, chainParams.numChains);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Temp (chainTemp) ***************************************************************/
		else if (!strcmp(parmName, "Temp"))
			{
			if (expecting == Expecting(EQUALSIGN))
				{
				tempIndex = 0;
				expecting = Expecting(NUMBER) | Expecting(LEFTPAR);
				}
			else if (expecting == Expecting(LEFTPAR))
				{
				chainParams.userDefinedTemps = YES;
				expecting = Expecting(NUMBER);
				}
			else if (expecting == Expecting(RIGHTPAR))
				{
				MrBayesPrint ("%s   Setting user-defined temperatures\n", spacer);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else if (expecting == Expecting(COMMA))
				{
				expecting = Expecting(NUMBER);
				}
			else if (expecting == Expecting(NUMBER))
				{
				if (chainParams.userDefinedTemps == NO)
					{
					sscanf (tkn, "%lf", &tempD);
					chainParams.chainTemp = tempD;
					MrBayesPrint ("%s   Setting heating parameter to %lf\n", spacer, chainParams.chainTemp);
					expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
					}
				else
					{
					if (tempIndex >= MAX_CHAINS)
						{
						MrBayesPrint ("%s   Too many user-defined temperatures (%d maximum)\n", spacer, MAX_CHAINS);
						return (ERROR);
						}
					sscanf (tkn, "%lf", &tempD);
					chainParams.userTemps[tempIndex++] = tempD;
					expecting = Expecting(COMMA) | Expecting(RIGHTPAR);
					}
				}
			else
				return (ERROR);
			}
		/* set Reweight (weightScheme) ********************************************************/
		else if (!strcmp(parmName, "Reweight"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(LEFTPAR);
			else if (expecting == Expecting(LEFTPAR))
				{
				expecting = Expecting(NUMBER);
				whichReweightNum = 0;
				}
			else if (expecting == Expecting(NUMBER))
				{
				if (whichReweightNum < 0 || whichReweightNum > 2)
					return (ERROR);
				sscanf (tkn, "%lf", &tempD);
				chainParams.weightScheme[whichReweightNum] = tempD;
				if (whichReweightNum < 2)
					{
					if (tempD < 0.0 || tempD > 100.0)
						{
						MrBayesPrint ("%s   The reweighting parameter must be between 0 and 100\n", spacer);
						chainParams.weightScheme[0] = chainParams.weightScheme[1] = 0.0;
						chainParams.weightScheme[2] = 1.0;
						return (ERROR);
						}
					}
				else
					{
					if (tempD <= 0.0 || tempD > 1.0)
						{
						MrBayesPrint ("%s   The reweighting increment must be between 0 and 1\n", spacer);
						chainParams.weightScheme[0] = chainParams.weightScheme[1] = 0.0;
						chainParams.weightScheme[2] = 1.0;
						return (ERROR);
						}
					}
				if (whichReweightNum == 0)
					{
					expecting = Expecting(COMMA);
					}
				else if (whichReweightNum == 1)
					{
					if (chainParams.weightScheme[0] + chainParams.weightScheme[1] > 100.0)
						{
						MrBayesPrint ("%s   The sum of the reweighting parameters cannot exceed 100 %%\n", spacer);
						chainParams.weightScheme[0] = chainParams.weightScheme[1] = 0.0;
						chainParams.weightScheme[2] = 1.0;
						return (ERROR);
						}
					expecting = Expecting(COMMA) | Expecting(RIGHTPAR);
					}
				else
					{
					expecting = Expecting(RIGHTPAR);
					}
				whichReweightNum++;
				}
			else if ((expecting & Expecting(COMMA)) == Expecting(COMMA))
				expecting = Expecting(NUMBER);
			else if ((expecting & Expecting(RIGHTPAR)) == Expecting(RIGHTPAR))
				{
				if (chainParams.weightScheme[0] >= 100.0)
					{
					MrBayesPrint ("%s   Cannot decrease weight of all characters\n", spacer);
					chainParams.weightScheme[0] = chainParams.weightScheme[1] = 0.0;
					chainParams.weightScheme[2] = 1.0;
					return (ERROR);
					}
				MrBayesPrint ("%s   Setting reweighting parameter to (%1.2lf v, %1.2lf ^) increment = %1.2lf\n", 
					spacer, chainParams.weightScheme[0], chainParams.weightScheme[1], chainParams.weightScheme[2]);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Filename (chainFileName) *******************************************************/
		else if (!strcmp(parmName, "Filename"))
			{
			if (expecting == Expecting(EQUALSIGN))
				{
				expecting = Expecting(ALPHA);
				readWord = YES;
				}
			else if (expecting == Expecting(ALPHA))
				{
				sscanf (tkn, "%s", tempStr);
				strcpy (chainParams.chainFileName, tempStr);
				fileNameChanged = YES;
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Relburnin (relativeBurnin) ********************************************************/
		else if (!strcmp(parmName, "Relburnin"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(ALPHA);
			else if (expecting == Expecting(ALPHA))
				{
				if (IsArgValid(tkn, tempStr) == NO_ERROR)
					{
					if (!strcmp(tempStr, "Yes"))
						chainParams.relativeBurnin = YES;
					else
						chainParams.relativeBurnin = NO;
					}
				else
					{
					MrBayesPrint ("%s   Invalid argument for Relburnin\n", spacer);
					return (ERROR);
					}
				if (chainParams.relativeBurnin == YES)
					MrBayesPrint ("%s   Using relative burnin (a fraction of samples discarded).\n", spacer);
				else
					MrBayesPrint ("%s   Using absolute burnin (a fixed number of samples discarded).\n", spacer);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Burnin (chainBurnIn) ***********************************************************/
		else if (!strcmp(parmName, "Burnin"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(NUMBER);
			else if (expecting == Expecting(NUMBER))
				{
				sscanf (tkn, "%d", &tempI);
				chainParams.chainBurnIn = tempI;
				MrBayesPrint ("%s   Setting chain burn-in to %d\n", spacer, chainParams.chainBurnIn);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Burninfrac (burninFraction) ************************************************************/
		else if (!strcmp(parmName, "Burninfrac"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(NUMBER);
			else if (expecting == Expecting(NUMBER))
				{
				sscanf (tkn, "%lf", &tempD);
				if (tempD < 0.01)
					{
					MrBayesPrint ("%s   Burnin fraction too low (< 0.01)\n", spacer);
					return (ERROR);
					}
				if (tempD > 0.50)
					{
					MrBayesPrint ("%s   Burnin fraction too high (> 0.50)\n", spacer);
					return (ERROR);
					}
				chainParams.burninFraction = tempD;
				MrBayesPrint ("%s   Setting burnin fraction to %.2f\n", spacer, chainParams.burninFraction);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Stoprule (stopRule) ********************************************************/
		else if (!strcmp(parmName, "Stoprule"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(ALPHA);
			else if (expecting == Expecting(ALPHA))
				{
				if (IsArgValid(tkn, tempStr) == NO_ERROR)
					{
					if (!strcmp(tempStr, "Yes"))
						chainParams.stopRule = YES;
					else
						chainParams.stopRule = NO;
					}
				else
					{
					MrBayesPrint ("%s   Invalid argument for Stoprule\n", spacer);
					return (ERROR);
					}
				if (chainParams.stopRule == YES)
					MrBayesPrint ("%s   Using stopping rule.\n", spacer);
				else
					MrBayesPrint ("%s   Not using stopping rule.\n", spacer);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Stopval (stopVal) ************************************************************/
		else if (!strcmp(parmName, "Stopval"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(NUMBER);
			else if (expecting == Expecting(NUMBER))
				{
				sscanf (tkn, "%lf", &tempD);
				if (tempD < 0.000001)
					{
					MrBayesPrint ("%s   Stop value too low (< 0.000001)\n", spacer);
					return (ERROR);
					}
				if (tempD > 0.20)
					{
					MrBayesPrint ("%s   Stop value too high (> 0.20)\n", spacer);
					return (ERROR);
					}
				chainParams.stopVal = tempD;
				MrBayesPrint ("%s   Setting burnin fraction to %.2f\n", spacer, chainParams.burninFraction);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Startingtree (chainStartTree) **************************************************/
		else if (!strcmp(parmName, "Startingtree"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(ALPHA);
			else if (expecting == Expecting(ALPHA))
				{
				if (IsArgValid(tkn, tempStr) == NO_ERROR)
					strcpy(chainParams.chainStartTree, tempStr);
				else
					{
					MrBayesPrint ("%s   Invalid starting tree argument\n", spacer);
					return (ERROR);
					}
				MrBayesPrint ("%s   Setting starting tree to \"%s\"\n", spacer, chainParams.chainStartTree);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Nperts (numStartPerts) *********************************************************/
		else if (!strcmp(parmName, "Nperts"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(NUMBER);
			else if (expecting == Expecting(NUMBER))
				{
				sscanf (tkn, "%d", &tempI);
				chainParams.numStartPerts = tempI;
				MrBayesPrint ("%s   Setting number of perturbations to start tree to %d\n", spacer, chainParams.numStartPerts);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Savebrlens (saveBrlens) ********************************************************/
		else if (!strcmp(parmName, "Savebrlens"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(ALPHA);
			else if (expecting == Expecting(ALPHA))
				{
				if (IsArgValid(tkn, tempStr) == NO_ERROR)
					{
					if (!strcmp(tempStr, "Yes"))
						chainParams.saveBrlens = YES;
					else
						chainParams.saveBrlens = NO;
					}
				else
					{
					MrBayesPrint ("%s   Invalid argument for saving branch lengths\n", spacer);
					return (ERROR);
					}
				if (chainParams.saveBrlens == YES)
					MrBayesPrint ("%s   Setting program to save branch length information\n", spacer);
				else
					MrBayesPrint ("%s   Setting program to not save branch lengths\n", spacer);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Redirect (redirect) ********************************************************/
		else if (!strcmp(parmName, "Redirect"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(ALPHA);
			else if (expecting == Expecting(ALPHA))
				{
				if (IsArgValid(tkn, tempStr) == NO_ERROR)
					{
					if (!strcmp(tempStr, "Yes"))
						chainParams.redirect = YES;
					else
						chainParams.redirect = NO;
					}
				else
					{
					MrBayesPrint ("%s   Invalid argument for redirecting output\n", spacer);
					return (ERROR);
					}
				if (chainParams.saveBrlens == YES)
					MrBayesPrint ("%s   Setting program to redirect information\n", spacer);
				else
					MrBayesPrint ("%s   Setting program not to redirect information\n", spacer);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Data (runWithData) ************************************************************/
		else if (!strcmp(parmName, "Data"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(ALPHA);
			else if (expecting == Expecting(ALPHA))
				{
				if (IsArgValid(tkn, tempStr) == NO_ERROR)
					{
					if (!strcmp(tempStr, "Yes"))
						chainParams.runWithData = YES;
					else
						chainParams.runWithData = NO;
					}
				else
					{
					MrBayesPrint ("%s   Invalid argument for Data\n", spacer);
					return (ERROR);
					}
				if (chainParams.runWithData == NO)
					MrBayesPrint ("%s   Running without data (only for checking priors!).\n", spacer);
				else
					MrBayesPrint ("%s   Running with data (standard analysis).\n", spacer);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Ordertaxa (chainParams.orderTaxa) *********************************************/
		else if (!strcmp(parmName, "Ordertaxa"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(ALPHA);
			else if (expecting == Expecting(ALPHA))
				{
				if (IsArgValid(tkn, tempStr) == NO_ERROR)
					{
					if (!strcmp(tempStr, "Yes"))
						chainParams.orderTaxa = YES;
					else
						chainParams.orderTaxa = NO;
					}
				else
					{
					MrBayesPrint ("%s   Invalid argument for ordertaxa\n", spacer);
					return (ERROR);
					}
				if (sumtParams.calcTrprobs == YES)
					MrBayesPrint ("%s   Setting ordertaxa to yes\n", spacer);
				else
					MrBayesPrint ("%s   Setting ordertaxa to no\n", spacer);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		/* set Swapadjacent (swapAdjacentOnly) **************************************************/
		else if (!strcmp(parmName, "Swapadjacent"))
			{
			if (expecting == Expecting(EQUALSIGN))
				expecting = Expecting(ALPHA);
			else if (expecting == Expecting(ALPHA))
				{
				if (IsArgValid(tkn, tempStr) == NO_ERROR)
					{
					if (!strcmp(tempStr, "Yes"))
						chainParams.swapAdjacentOnly = YES;
					else
						chainParams.swapAdjacentOnly = NO;
					}
				else
					{
					MrBayesPrint ("%s   Invalid argument for Swapadjacent\n", spacer);
					return (ERROR);
					}
				if (chainParams.swapAdjacentOnly == YES)
					MrBayesPrint ("%s   Setting program to attempt swaps only between chains of adjacent temperatures\n", spacer);
				else
					MrBayesPrint ("%s   Setting program to attempt all possible swaps between chains\n", spacer);
				expecting = Expecting(PARAMETER) | Expecting(SEMICOLON);
				}
			else
				return (ERROR);
			}
		else
			return (ERROR);
		}

	return (NO_ERROR);
		
}





int ExhaustiveParsimonySearch (Tree *t, int chain, TreeInfo *tInfo)

{

	int			i, j, k;
	TreeNode    *p;
	
	for (i=j=k=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->left == NULL || p->right == NULL)
			tInfo->leaf[j++] = p;
		else
			tInfo->vertex[k++] = p;
		}

	tInfo->leaf[0]->anc = tInfo->leaf[1]->anc = tInfo->vertex[0];
	tInfo->vertex[0]->left = tInfo->leaf[0];
	tInfo->vertex[0]->right = tInfo->leaf[1];
	tInfo->leaf[t->nIntNodes+1]->left = tInfo->vertex[0];
	tInfo->vertex[0]->anc = tInfo->leaf[t->nIntNodes+1];

	BuildExhaustiveSearchTree (t, chain, 2, tInfo);

	return (NO_ERROR);
}




int ExtendChainQuery ()

{

	int				i, extendChain, additionalCycles, len;
	char			s[100];
	
#	if defined (MPI_ENABLED)
	if (proc_id == 0)
		{
		MrBayesPrint ("\n");
		MrBayesPrint ("%s   Continue with analysis? (yes/no): ", spacer);
		extendChain = NO;
		for (i=0; i<10; i++)
			{
			fgets (s, 99, stdin);
			s[99]='\0';
			len = strlen (s);
			for (i=0; i<len; i++)
				s[i] = tolower(s[i]);
			if ((s[0] == 'y' && len == 2) || (s[0] == 'y' && s[1] == 'e' && len == 3) || (s[0] == 'y' && s[1] == 'e' && s[2] == 's' && len == 4))
				{
				extendChain = YES;
				break;
				}
			else if ((s[0] == 'n' && len == 2) || (s[0] == 'n' && s[1] == 'o' && len == 3))
				{
				extendChain = NO;
				break;
				}
			MrBayesPrint ("%s   Enter Yes or No: ", spacer);
			}
		}
	MPI_Bcast (&extendChain, 1, MPI_INT, 0, MPI_COMM_WORLD);
	if (extendChain == YES)
		{
		if (proc_id == 0)
			{
			additionalCycles = 0;
			do
				{
				if (additionalCycles < 0)
					MrBayesPrint ("%s      Number must be greater than or equal to 0: ", spacer);
				else
					MrBayesPrint ("%s      Additional number of generations: ", spacer);

				fgets (s, 100, stdin);
				sscanf (s, "%d", &additionalCycles);

				} while (additionalCycles < 0);
			MrBayesPrint ("\n");
			}
		MPI_Bcast (&additionalCycles, 1, MPI_INT, 0, MPI_COMM_WORLD);
			
		return (additionalCycles);
		}
	else
		return (0);
#	else
	MrBayesPrint ("\n");
	MrBayesPrint ("%s   Continue with analysis? (yes/no): ", spacer);
	extendChain = NO;
	for (i=0; i<10; i++)
		{
		/* if terminal is dead there is no sense in waiting for a reply */
		if (fgets (s, 20, stdin) == NULL)
			{
			extendChain = NO;
			break;
			}
		len = (int) strlen (s);
		for (i=0; i<len; i++)
			s[i] = tolower(s[i]);
				
		if ((s[0] == 'y' && len == 2) || (s[0] == 'y' && s[1] == 'e' && len == 3) || (s[0] == 'y' && s[1] == 'e' && s[2] == 's' && len == 4))
			{
			extendChain = YES;
			break;
			}
		else if ((s[0] == 'n' && len == 2) || (s[0] == 'n' && s[1] == 'o' && len == 3))
			{
			extendChain = NO;
			break;
			}
		MrBayesPrint ("%s   Enter Yes or No: ", spacer);
		}
	
	if (extendChain == YES)
		{
		additionalCycles = 0;
		do
			{
			if (additionalCycles < 0)
				MrBayesPrint ("%s      Number must be greater than or equal to 0: ", spacer);
			else
				MrBayesPrint ("%s      Additional number of generations: ", spacer);

			fgets (s, 20, stdin);
			sscanf (s, "%d", &additionalCycles);

			} while (additionalCycles < 0);
		MrBayesPrint ("\n");
		return (additionalCycles);
		}
	else
		return (0);
#	endif

}





/*------------------------------------------------------------------------
|
|	FillNormalParams: Allocate and fill in non-tree parameters
|
-------------------------------------------------------------------------*/
int FillNormalParams (long int *seed)

{

	int			b, c, i, j, k, n, chn, index, nOfParams, tempInt;
	MrBFlt		sum, *bs, *value, *subValue, symDir[10], scaler;

	Param		*p;
	ModelInfo	*m;
	ModelParams	*mp;
	

	/* allocate space */
	nOfParams = 0;
	for (k=0; k<numParams; k++)
		{
		nOfParams += params[k].nValues;
		nOfParams += params[k].nSubValues;
		}

	paramValsRowSize = nOfParams;
	nOfParams *= (2 * numLocalChains);

	if (memAllocs[ALLOC_PARAMVALUES] == YES)
		{
		MrBayesPrint ("%s   paramValues not free in FillNormalParams\n", spacer);
		return ERROR;
		}
	paramValues = (MrBFlt *) calloc (nOfParams, sizeof(MrBFlt));
	if (!paramValues)
		{
		MrBayesPrint ("%s   Problem allocating paramValues\n", spacer);
		return ERROR;
		}
	else
		memAllocs[ALLOC_PARAMVALUES] = YES;

	/* set pointers to values for chain 1 state 0            */
	/* this scheme keeps the chain and state values together */
	nOfParams = 0;
	for (k=0; k<numParams; k++)
		{
		p = &params[k];
		p->values = paramValues + nOfParams;
		nOfParams += p->nValues;
		p->subValues = paramValues + nOfParams;
		nOfParams += p->nSubValues;
		}
	
	/* allocate space for index values needed to handle sympi if relevant */
	/* first count number of sympis needed */
	for (k=n=i=0; k<numParams; k++)
		{
		p = &params[k];
		n += p->nSympi;
		}

	/* then allocate and fill in */
	if (n > 0)
		{
		if (memAllocs[ALLOC_SYMPIINDEX] == YES)
			{
			MrBayesPrint ("%s   sympiIndex is not free in FillNormalParams\n", spacer);
			return ERROR;
			}
		sympiIndex = (int *) calloc (3*n, sizeof (int));
		if (!sympiIndex)
			{
			MrBayesPrint ("%s   Problem allocating sympiIndex\n", spacer);
			return ERROR;
			}
		else
			memAllocs[ALLOC_SYMPIINDEX] = YES;

		/* set up sympi pointers and fill sympiIndex */
		for (k=i=0; k<numParams; k++)
			{
			p = &params[k];
			if (p->nSympi > 0)
				{
				p->sympiBsIndex = sympiIndex + i;
				p->sympinStates = sympiIndex + i + n;
				p->sympiCType = sympiIndex + i + (2 * n);
				for (j=0; j<p->nRelParts; j++)
					{
					m = &modelSettings[p->relParts[j]];
					for (c=0; c<m->numChars; c++)
						{
						if (m->nStates[c] > 2 && (m->cType[c] == UNORD || m->cType[c] == ORD))
							{
							p->sympinStates[c] = m->nStates[c];
							p->sympiBsIndex[c] = m->bsIndex[c];
							p->sympiCType[c] = m->cType[c];
							}
						}
					}
				i += p->nSympi;
				}
			}
		}	
	
	/* fill in values for nontree params for current state of chains */
	for (chn=0; chn<numLocalChains; chn++)
		{
		for (k=0; k<numParams; k++)
			{
			p  = &params[k];
			mp = &modelParams[p->relParts[0]];
			m  = &modelSettings[p->relParts[0]];
			
			/* find model settings and nStates, pInvar, invar cond likes */

			value = GetParamVals (p, chn, state[chn]);
			subValue = GetParamSubVals (p, chn, state[chn]);

			if (p->paramType == P_TRATIO)
				{
				/* Fill in tratios **************************************************************************************/
				if (p->paramId == TRATIO_DIR)
					value[0] = 1.0;
				else if (p->paramId == TRATIO_FIX)
					value[0] = mp->tRatioFix;
				}
			else if (p->paramType == P_REVMAT)
				{
				/* Fill in revMat ***************************************************************************************/
				/* rates are stored in order, AC or AR first, using the Dirichlet parameterization */
				if (p->paramId == REVMAT_DIR)
					{
					for (j=0; j<p->nValues; j++)
						value[j] = 1.0 / (MrBFlt) (p->nValues);
					}
				else if (p->paramId == REVMAT_FIX)
					{
					scaler = 0.0;
					if (mp->dataType == PROTEIN)
						{
						for (j=0; j<190; j++)
							scaler += (value[j] = mp->aaRevMatFix[j]);
						for (j=0; j<190; j++)
							value[j] /= scaler;
						}
					else
						{
						for (j=0; j<6; j++)
							scaler += (value[j] = mp->revMatFix[j]);
						for (j=0; j<6; j++)
							value[j] /= scaler;
						}
					}
				}
			else if (p->paramType == P_OMEGA)
				{
				/* Fill in omega ****************************************************************************************/
				if (p->nValues == 1)
					{
					if (p->paramId == OMEGA_DIR)
						value[0] = 1.0;
					else if (p->paramId == OMEGA_FIX)
						value[0] = mp->omegaFix;
					}
				else
					{
					if (!strcmp(mp->omegaVar, "Ny98"))
						{
						if (p->paramId == OMEGA_BUD || p->paramId == OMEGA_BUF || p->paramId == OMEGA_BED ||
						    p->paramId == OMEGA_BEF || p->paramId == OMEGA_BFD || p->paramId == OMEGA_BFF)
							value[0] = RandomNumber(seed);
						else if (p->paramId == OMEGA_FUD || p->paramId == OMEGA_FUF || p->paramId == OMEGA_FED ||
						         p->paramId == OMEGA_FEF || p->paramId == OMEGA_FFD || p->paramId == OMEGA_FFF)
							value[0] = mp->ny98omega1Fixed;
						value[1] = 1.0;
						if (p->paramId == OMEGA_BUD || p->paramId == OMEGA_BUF || p->paramId == OMEGA_FUD ||
						    p->paramId == OMEGA_FUF)
							value[2] = mp->ny98omega3Uni[0] + RandomNumber(seed) * (mp->ny98omega3Uni[1] - mp->ny98omega3Uni[0]);
						else if (p->paramId == OMEGA_BED || p->paramId == OMEGA_BEF || p->paramId == OMEGA_FED ||
						         p->paramId == OMEGA_FEF)
							value[2] =  (1.0 + -(1.0/mp->ny98omega3Exp) * log(1.0 - RandomNumber(seed)));
						else
							value[2] = mp->ny98omega3Fixed;
						if (p->paramId == OMEGA_BUD || p->paramId == OMEGA_BED || p->paramId == OMEGA_BFD || 
							p->paramId == OMEGA_FUD || p->paramId == OMEGA_FED || p->paramId == OMEGA_FFD) 
						    {
							subValue[3] = mp->codonCatDir[0];
							subValue[4] = mp->codonCatDir[1];
							subValue[5] = mp->codonCatDir[2];
							DirichletRandomVariable (&subValue[3], &subValue[0], 3, seed);
							}
						else
							{
							subValue[0] = mp->codonCatFreqFix[0];
							subValue[1] = mp->codonCatFreqFix[1];
							subValue[2] = mp->codonCatFreqFix[2];
							subValue[3] = 0.0;
							subValue[4] = 0.0;
							subValue[5] = 0.0;
							}
						}
					else if (!strcmp(mp->omegaVar, "M3"))
						{
						if (p->paramId == OMEGA_FD || p->paramId == OMEGA_FF)
							{
							value[0] = mp->m3omegaFixed[0];
							value[1] = mp->m3omegaFixed[1];
							value[2] = mp->m3omegaFixed[2];
							}
						else
							{
							value[0] =  0.1;
							value[1] =  1.0;
							value[2] =  3.0;
							}
						if (p->paramId == OMEGA_ED || p->paramId == OMEGA_FD) 
						    {
							subValue[3] = mp->codonCatDir[0];
							subValue[4] = mp->codonCatDir[1];
							subValue[5] = mp->codonCatDir[2];
							DirichletRandomVariable (&subValue[3], &subValue[0], 3, seed);
							}
						else
							{
							subValue[0] = mp->codonCatFreqFix[0];
							subValue[1] = mp->codonCatFreqFix[1];
							subValue[2] = mp->codonCatFreqFix[2];
							subValue[3] = 0.0;
							subValue[4] = 0.0;
							subValue[5] = 0.0;
							}
						}
					else if (!strcmp(mp->omegaVar, "M10"))
						{
						if (p->paramId == OMEGA_10UUB || p->paramId == OMEGA_10UEB || p->paramId == OMEGA_10UFB ||
						    p->paramId == OMEGA_10EUB || p->paramId == OMEGA_10EEB || p->paramId == OMEGA_10EFB ||
						    p->paramId == OMEGA_10FUB || p->paramId == OMEGA_10FEB || p->paramId == OMEGA_10FFB) 
						    {
							subValue[mp->numM10BetaCats + mp->numM10GammaCats + 2] = mp->codonCatDir[0];
							subValue[mp->numM10BetaCats + mp->numM10GammaCats + 3] = mp->codonCatDir[1];
							DirichletRandomVariable (&subValue[mp->numM10BetaCats + mp->numM10GammaCats + 2], &subValue[mp->numM10BetaCats + mp->numM10GammaCats + 0], 2, seed);
							}
						else
							{
							subValue[mp->numM10BetaCats + mp->numM10GammaCats + 0] = mp->codonCatFreqFix[0];
							subValue[mp->numM10BetaCats + mp->numM10GammaCats + 1] = mp->codonCatFreqFix[1];
							subValue[mp->numM10BetaCats + mp->numM10GammaCats + 2] = 0.0;
							subValue[mp->numM10BetaCats + mp->numM10GammaCats + 3] = 0.0;
							}
							
						for (i=0; i<mp->numM10BetaCats; i++)
							subValue[i] = subValue[mp->numM10BetaCats + mp->numM10GammaCats + 0] / mp->numM10BetaCats;
						for (i=mp->numM10BetaCats; i<mp->numM10BetaCats+mp->numM10GammaCats; i++)
							subValue[i] = subValue[mp->numM10BetaCats + mp->numM10GammaCats + 1] / mp->numM10GammaCats;

						if (p->paramId == OMEGA_10FUB || p->paramId == OMEGA_10FUF || p->paramId == OMEGA_10FEB ||
						    p->paramId == OMEGA_10FEF || p->paramId == OMEGA_10FFB || p->paramId == OMEGA_10FFF)
							{
							subValue[mp->numM10BetaCats + mp->numM10GammaCats + 4] = mp->m10betaFix[0];
							subValue[mp->numM10BetaCats + mp->numM10GammaCats + 5] = mp->m10betaFix[1];
							}
						else
							{
							subValue[mp->numM10BetaCats + mp->numM10GammaCats + 4] = 1.0;
							subValue[mp->numM10BetaCats + mp->numM10GammaCats + 5] = 1.0;
							}

						if (p->paramId == OMEGA_10UFB || p->paramId == OMEGA_10UFF || p->paramId == OMEGA_10EFB ||
						    p->paramId == OMEGA_10EFF || p->paramId == OMEGA_10FFB || p->paramId == OMEGA_10FFF)
							{
							subValue[mp->numM10BetaCats + mp->numM10GammaCats + 6] = mp->m10gammaFix[0];
							subValue[mp->numM10BetaCats + mp->numM10GammaCats + 7] = mp->m10gammaFix[1];
							}
						else
							{
							subValue[mp->numM10BetaCats + mp->numM10GammaCats + 6] = 1.0;
							subValue[mp->numM10BetaCats + mp->numM10GammaCats + 7] = 1.0;
							}
							
						BetaBreaks (subValue[mp->numM10BetaCats + mp->numM10GammaCats + 4], subValue[mp->numM10BetaCats + mp->numM10GammaCats + 5], &value[0], mp->numM10BetaCats);
						if (DiscreteGamma (&value[mp->numM10BetaCats], subValue[mp->numM10BetaCats + mp->numM10GammaCats + 6], subValue[mp->numM10BetaCats + mp->numM10GammaCats + 7], mp->numM10GammaCats, 0) == ERROR)
							return (ERROR);
						for (i=0; i<mp->numM10GammaCats; i++)
							value[mp->numM10BetaCats + i] += 1.0;

							
						}
					else
						{
						
						}
					}
				}
			else if (p->paramType == P_PI)
				{
				/* Fill in state frequencies ****************************************************************************/
				if (p->paramId == SYMPI_UNI || p->paramId == SYMPI_UNI_MS)
					value[0] = 1.0;
				else if (p->paramId == SYMPI_EXP || p->paramId == SYMPI_EXP_MS)
					value[0] = 1.0;

				else if (p->paramId == SYMPI_FIX || p->paramId == SYMPI_FIX_MS)
					value[0] = mp->symBetaFix;

				else if (p->paramId == SYMPI_EQUAL)
					{
					for (n=index=0; n<9; n++)
						{
						for (i=0; i<p->nRelParts; i++)
							if (modelSettings[p->relParts[i]].isTiNeeded[n] == YES)
								break;
						if (i < p->nRelParts)
							{
							for (j=0; j<(n+2); j++)
								{
								subValue[index++] =  (1.0 / (n + 2));
								}
							}
						}
					for (n=9; n<13; n++)
						{
						for (i=0; i<p->nRelParts; i++)
							if (modelSettings[p->relParts[i]].isTiNeeded[n] == YES)
								break;
						if (i < p->nRelParts)
							{
							for (j=0; j<(n-6); j++)
								{
								subValue[index++] =  (1.0 / (n - 6));
								}
							}
						}
					}

				else if (p->paramId == PI_DIR)
					{
					if (mp->numDirParams != mp->nStates && mp->numDirParams != 0)
						{
						MrBayesPrint ("%s   Mismatch between number of dirichlet parameters (%d) and the number of states (%d)\n", spacer, mp->numDirParams, mp->nStates);
						return ERROR;
						}

					/* if user has not set dirichlet parameters, go with default */
					/* overall variance equals number of states */
					if (mp->numDirParams == 0)
						for (i=0; i<mp->nStates; i++)
							value[i] = mp->stateFreqsDir[i] = 1.0;
					else
						for (i=0; i<mp->nStates; i++)
							value[i] = mp->stateFreqsDir[i];

					/* now fill in subvalues */
					for (i=0; i<mp->nStates; i++)
						subValue[i] =  (1.0 / mp->nStates);
#					if defined ASYMMETRY
					if (mp->dataType == RESTRICTION)
						{
						for (i=2; i<4; i++)
							subValue[i] =  0.5;
						}
#					endif
					}

				else if (p->paramId == PI_USER)
					{
					for (i=0; i<mp->nStates; i++)
						subValue[i] =  mp->stateFreqsFix[i];
					}
					
				else if (p->paramId == PI_FIXED)
					{
					if (!strcmp(mp->aaModelPr, "Fixed"))
						{
						if (!strcmp(mp->aaModel, "Jones"))
							{
							for (i=0; i<mp->nStates; i++)
								subValue[i] = jonesPi[i];
							}
						else if (!strcmp(mp->aaModel, "Dayhoff"))
							{
							for (i=0; i<mp->nStates; i++)
								subValue[i] = dayhoffPi[i];
							}
						else if (!strcmp(mp->aaModel, "Mtrev"))
							{
							for (i=0; i<mp->nStates; i++)
								subValue[i] = mtrev24Pi[i];
							}
						else if (!strcmp(mp->aaModel, "Mtmam"))
							{
							for (i=0; i<mp->nStates; i++)
								subValue[i] = mtmamPi[i];
							}
						else if (!strcmp(mp->aaModel, "Wag"))
							{
							for (i=0; i<mp->nStates; i++)
								subValue[i] = wagPi[i];
							}
						else if (!strcmp(mp->aaModel, "Rtrev"))
							{
							for (i=0; i<mp->nStates; i++)
								subValue[i] = rtrevPi[i];
							}
						else if (!strcmp(mp->aaModel, "Cprev"))
							{
							for (i=0; i<mp->nStates; i++)
								subValue[i] = cprevPi[i];
							}
						else if (!strcmp(mp->aaModel, "Vt"))
							{
							for (i=0; i<mp->nStates; i++)
								subValue[i] = vtPi[i];
							}
						else if (!strcmp(mp->aaModel, "Blosum"))
							{
							for (i=0; i<mp->nStates; i++)
								subValue[i] = blosPi[i];
							}

						}
					}

				else if (p->paramId == PI_EMPIRICAL)
					{
					if (GetEmpiricalFreqs (p->relParts, p->nRelParts) == ERROR)
						return (ERROR);
					for (i=0; i<mp->nStates; i++)
						value[i] = empiricalFreqs[i];
					}

				else if (p->paramId == PI_EQUAL)
					{
					for (i=0; i<mp->nStates; i++)
						subValue[i] =  (1.0 / mp->nStates);
					}

				/* Deal with transition asymmetry for standard characters */
				/* First, fill in stationary frequencies for beta categories if needed; */
				/* discard category frequencies (assume equal) */
				if (p->paramId == SYMPI_FIX || p->paramId == SYMPI_UNI || p->paramId == SYMPI_EXP
					|| p->paramId == SYMPI_FIX_MS || p->paramId == SYMPI_UNI_MS || p->paramId == SYMPI_EXP_MS)
					{
					for (i=index=0; i<p->nRelParts; i++)
						if (modelSettings[p->relParts[i]].isTiNeeded[0] == YES)
							break;
					if (i < p->nRelParts)
						{
						index += (2 * mp->numBetaCats);
						BetaBreaks (value[0], value[0], subValue, mp->numBetaCats);
						b = 2*mp->numBetaCats;
						for (i=b-2; i>0; i-=2)
							{
							subValue[i] = subValue[i/2];
							}
						for (i=1; i<b; i+=2)
							{
							subValue[i] =  (1.0 - subValue[i-1]);
							}
						subValue += (2 * mp->numBetaCats);
						}

					/* Then fill in state frequencies for multistate chars, one set for each */
					for (i=0; i<10; i++)
						symDir[i] = value[0];
						
					for (c=0; c<p->nSympi; c++)
						{
						/* now fill in subvalues */
						DirichletRandomVariable (symDir, subValue, p->sympinStates[c], seed);
						sum = 0.0;
						for (i=0; i<p->sympinStates[c]; i++)
							{
							if (subValue[i] < 0.0001)
								subValue[i] =  0.0001;
							sum += subValue[i];
							}
						for (i=0; i<mp->nStates; i++)
							subValue[i] /= sum;

						subValue += p->sympinStates[c];
						}
					}
				}
			else if (p->paramType == P_SHAPE)
				{
				/* Fill in gamma values ********************************************************************************/
				/* first get hyperprior */
				if (p->paramId == SHAPE_UNI)
					{
					value[0] = 0.5;
					if (value[0] < mp->shapeUni[0] || value[0] > mp->shapeUni[1])
						value[0] = mp->shapeUni[0] + (mp->shapeUni[1] - mp->shapeUni[0]) *  0.5;
					}
				else if (p->paramId == SHAPE_EXP)
					value[0] = 0.5;
				else if (p->paramId == SHAPE_FIX)
					value[0] = mp->shapeFix;
				/* now fill in rates */
				if (DiscreteGamma (subValue, value[0], value[0], mp->numGammaCats, 0) == ERROR)
					return (ERROR);
				}
			else if (p->paramType == P_PINVAR)
				{
				/* Fill in pInvar ***************************************************************************************/
				if (p->paramId == PINVAR_UNI)
					value[0] = 0.0;

				else if (p->paramId == PINVAR_FIX)
					value[0] =  mp->pInvarFix;
				}
			else if (p->paramType == P_CORREL)
				{
				/* Fill in correlation parameter of adgamma model *******************************************************/
				if (p->paramId == CORREL_UNI)
					value[0] = 0.0;

				else if (p->paramId == CORREL_FIX)
					value[0] =  mp->corrFix;
				
				/* Fill in correlation matrices */
				AutodGamma (subValue, value[0], mp->numGammaCats);
				}
			else if (p->paramType == P_SWITCH)
				{
				/* Fill in switchRates for covarion model ***************************************************************/
				for (j=0; j<2; j++)
					{
					if (p->paramId == SWITCH_UNI)
						value[j] = RandomNumber(seed) * (mp->covswitchUni[1] - mp->covswitchUni[0]) + mp->covswitchUni[0];

					else if (p->paramId == SWITCH_EXP)
						value[j] =   (-(1.0/mp->covswitchExp) * log(1.0 - RandomNumber(seed)));

					else if (p->paramId == SWITCH_FIX)
						value[j] = mp->covswitchFix[j];
					}
				}
			else if (p->paramType == P_RATEMULT)
				{
				/* Fill in rateMult for division rates ******************************************************************/
				for (j=0; j<p->nValues; j++)
					{
					value[j] = 1.0;
					/* fill in more info about the divisions if this is a true rate multiplier
					   and not a base rate */
					if (p->nSubValues > 0)
						{
						/* num uncompressed chars */
						subValue[j] =  (modelSettings[p->relParts[j]].numUncompressedChars);
						/* rates scaled to first partition */
						subValue[p->nValues + j] = 1.0;
						/* Dirichlet parameters */
						subValue[2 * p->nValues + j] = modelParams[p->relParts[j]].ratePrDir;
						}
					}
				}
			else if (p->paramType == P_SPECRATE)
				{
				/* Fill in speciation rates *****************************************************************************/
				if (p->paramId == SPECRATE_FIX)
					value[0] = mp->speciationFix;
				else 
					value[0] = 1.0;
				}
			else if (p->paramType == P_EXTRATE)
				{
				/* Fill in extinction rates *****************************************************************************/
				if (p->paramId == EXTRATE_FIX)
					value[0] = mp->extinctionFix;
				else
					value[0] =  0.2;
				}
			else if (p->paramType == P_THETA)
				{
				/* Fill in theta ****************************************************************************************/
				if (p->paramId == THETA_UNI)
					value[0] = RandomNumber(seed) * (mp->thetaUni[1] - mp->thetaUni[0]) + mp->thetaUni[0];

				else if (p->paramId == THETA_EXP)
					value[0] =   (-(1.0/mp->thetaExp) * log(1.0 - RandomNumber(seed)));

				else if (p->paramId == THETA_FIX)
					value[0] = mp->thetaFix;
				}
			else if (p->paramType == P_AAMODEL)
				{
				/* Fill in theta ****************************************************************************************/
				if (p->paramId == AAMODEL_MIX)
					{
					/* amino acid model ID's
						AAMODEL_POISSON			0
						AAMODEL_JONES			1
						AAMODEL_DAY				2
						AAMODEL_MTREV			3
						AAMODEL_MTMAM			4
						AAMODEL_WAG				5
						AAMODEL_RTREV			6 
						AAMODEL_CPREV           7 
						AAMODEL_VT				8
						AAMODEL_BLOSUM			9 */

					/* set the amino acid model  (the meaning of the numbers is defined) */
					tempInt = (int)(RandomNumber(seed) * 10);
					value[0] = tempInt;
					
					/* we need to make certain that the aa frequencies are filled in correctly */
					bs = GetParamSubVals (m->stateFreq, chn, 0);
					if (tempInt == AAMODEL_POISSON)
						{
						for (i=0; i<mp->nStates; i++)
							bs[i] =  (1.0 / 20.0);
						}
					else if (tempInt == AAMODEL_JONES)
						{
						for (i=0; i<mp->nStates; i++)
							bs[i] = jonesPi[i];
						}
					else if (tempInt == AAMODEL_DAY)
						{
						for (i=0; i<mp->nStates; i++)
							bs[i] = dayhoffPi[i];
						}
					else if (tempInt == AAMODEL_MTREV)
						{
						for (i=0; i<mp->nStates; i++)
							bs[i] = mtrev24Pi[i];
						}
					else if (tempInt == AAMODEL_MTMAM)
						{
						for (i=0; i<mp->nStates; i++)
							bs[i] = mtmamPi[i];
						}
					else if (tempInt == AAMODEL_WAG)
						{
						for (i=0; i<mp->nStates; i++)
							bs[i] = wagPi[i];
						}
					else if (tempInt == AAMODEL_RTREV)
						{
						for (i=0; i<mp->nStates; i++)
							bs[i] = rtrevPi[i];
						}
					else if (tempInt == AAMODEL_CPREV)
						{
						for (i=0; i<mp->nStates; i++)
							bs[i] = cprevPi[i];
						}
					else if (tempInt == AAMODEL_VT)
						{
						for (i=0; i<mp->nStates; i++)
							bs[i] = vtPi[i];
						}
					else if (tempInt == AAMODEL_BLOSUM)
						{
						for (i=0; i<mp->nStates; i++)
							bs[i] = blosPi[i];
						}
						
					for (i=0; i<p->nSubValues; i++)
						{
						subValue[i] = mp->aaModelPrProbs[i];
						}
					
					}
				}

			}	/* next param */
		}	/* next chain */

	return NO_ERROR;
}		




		
int FillNumSitesOfPat (void)

{

	int			i, j, n, *increased, *decreased, nToDecrease, nToIncrease, whichToChange;
	MrBFlt		ran, sum;
	CLFlt		wtIncrement;
	
	wtIncrement = (CLFlt) chainParams.weightScheme[2];
	increased = decreased = NULL;
	
	/* reallocate numSitesOfPat */
	if (memAllocs[ALLOC_NUMSITESOFPAT] == NO)
		{
		MrBayesPrint ("%s   numSitesOfPat is not allocated\n", spacer);
		goto errorExit;
		}
	memAllocs[ALLOC_NUMSITESOFPAT] = NO;
	numSitesOfPat = (CLFlt *) realloc((void *) numSitesOfPat, numCompressedChars * chainParams.numChains * sizeof(MrBFlt));
	if (!numSitesOfPat)
		{
		MrBayesPrint ("%s   Problem reallocating numSitesOfPat (%d)\n", spacer, numCompressedChars * chainParams.numChains * sizeof(MrBFlt));
		goto errorExit;
		}
	memAllocs[ALLOC_NUMSITESOFPAT] = YES;

	/* copy first numCompressedChars into the remaining bits */
	if (chainParams.numChains > 1)
		{
		for (i=0; i<numCompressedChars; i++)
			{
			for (j=1; j<chainParams.numChains; j++)
				{
				numSitesOfPat[j * numCompressedChars + i] = numSitesOfPat[i];
				}
			}
		}	
		
	/* reweight characters for each chain */
	if (chainParams.numChains > 1)
		{
		if (chainParams.weightScheme[0] + chainParams.weightScheme[1] > 0.0001)
			MrBayesPrint ("%s   Reweighting of characters characters for chains 1 to %d\n", spacer, chainParams.numChains);

		/* check that we don't have an HMM */
		if (chainHasAdgamma == YES && chainParams.weightScheme[0] + chainParams.weightScheme[1] > 0.0001)
			{
			MrBayesPrint ("%s   Reweighting of characters is not allowed with an autocorrelated gamma model\n", spacer);
			goto errorExit;
			}
		
		/* how many characters */
		n = 0;
		for (i=0; i<numCompressedChars; i++)
			n += (int)numSitesOfPat[0 * numCompressedChars + i];
		nToDecrease = (int)(n * chainParams.weightScheme[0] / 100.0);
		nToIncrease = (int)(n * chainParams.weightScheme[1] / 100.0);
		if (chainParams.weightScheme[0] + chainParams.weightScheme[1] > 0.0001)
			{
			MrBayesPrint ("%s      Decreasing weight of %d characters\n", spacer, nToDecrease);
			MrBayesPrint ("%s      Increasing weight of %d characters\n", spacer, nToIncrease);
			}
		
		/* allocate memory */
		increased = (int *)malloc((size_t) (2 * numCompressedChars * sizeof(int)));
		if (!increased)
			{
			MrBayesPrint ("%s   Problem reallocating increased (%d)\n", spacer, numCompressedChars * chainParams.numChains * sizeof(int));
			goto errorExit;
			}
		decreased = increased + numCompressedChars;

		/* reweight characters for each chain */
		for (j=1; j<chainParams.numChains; j++)
			{
			for (i=0; i<numCompressedChars; i++)
				increased[i] = decreased[i] = 0;

			/* decrease weight of characters */
			for (i=0; i<nToDecrease; i++)
				{
				do
					{
					ran = RandomNumber(&swapSeed);
					sum = 0.0;
					for (whichToChange=0; whichToChange<numCompressedChars; whichToChange++)
						{
						sum += numSitesOfPat[0 * numCompressedChars + whichToChange] / n;
						if (ran < sum)
							break;
						}
					if (whichToChange < 0 || whichToChange >= numCompressedChars)
						continue;
					} while (decreased[whichToChange] >= numSitesOfPat[0 * numCompressedChars + whichToChange]);
				decreased[whichToChange]++;
				numSitesOfPat[j * numCompressedChars + whichToChange] -= wtIncrement;
				if (numSitesOfPat[j * numCompressedChars + whichToChange] < 0)
					{
					MrBayesPrint ("%s   Problem reweighting characters\n", spacer);
					goto errorExit;
					}
				}

			/* increase weight of characters */
			for (i=0; i<nToDecrease; i++)
				{
				do
					{
					ran = RandomNumber(&swapSeed);
					sum = 0.0;
					for (whichToChange=0; whichToChange<numCompressedChars; whichToChange++)
						{
						sum += numSitesOfPat[0 * numCompressedChars + whichToChange] / n;
						if (ran < sum)
							break;
						}
					if (whichToChange < 0 || whichToChange >= numCompressedChars)
						continue;
					} while ((increased[whichToChange] + decreased[whichToChange]) >= numSitesOfPat[0 * numCompressedChars + whichToChange]);
				increased[whichToChange]++;
				numSitesOfPat[j * numCompressedChars + whichToChange] += wtIncrement;
				if (numSitesOfPat[j * numCompressedChars + whichToChange] < 0)
					{
					MrBayesPrint ("%s   Problem reweighting characters\n", spacer);
					goto errorExit;
					}
				}

			}
			
		/* free allocated memory */
		free (increased);
		}
		
#	if 0
	/* print site patterns for each chain */
	for (i=0; i<numCompressedChars; i++)
		{
		MrBayesPrint ("%4d -- ", i);
		for (j=0; j<chainParams.numChains; j++)
			{
			MrBayesPrint ("%4.1lf ", numSitesOfPat[j * numCompressedChars + i]);
			}
		MrBayesPrint ("\n");
		}
#	endif

	return (NO_ERROR);
	
	errorExit:
		if (increased)
			free (increased);
		return (ERROR);
	
}





int FillRelPartsString (Param *p, char relPartString[100])

{

	int			i, n, filledString;
	char		temp[10];
	
	if (numCurrentDivisions == 1)
		{
		filledString = NO;
		strcpy (relPartString, "");
		}
	else
		{
		filledString = YES;
		if (p->nRelParts == numCurrentDivisions)
			{
			strcpy (relPartString, "{all}");
			}
		else
			{
			strcpy (relPartString, "{");
			for (i=n=0; i<p->nRelParts; i++)
				{
				n++;
				sprintf (temp, "%d", p->relParts[i] + 1);
				strcat (relPartString, temp);
				if (n < p->nRelParts)
					strcat (relPartString, ",");
				}
			strcat (relPartString, "}");
			}
		}
		
	return (filledString);

}


/*------------------------------------------------------------------
|
|	FillTreeParams: Fill in trees and branch lengths
|
------------------------------------------------------------------*/
int FillTreeParams (long int *seed)

{

	int			i, j, k, n, chn, nNodes, nNodePtrs, nOfParams, nOfTrees;

	Param		*p, *q;
	Tree		*tree, *tree0, *tree1;

	/* count the number of trees and dated trees */
	/* based on branch length parameters */
	numTrees = 0;
	numCalibratedTrees = 0;
	for (k=0; k<numParams; k++)
		{
		if (params[k].paramType == P_BRLENS)
			{
			numTrees++;
			if (params[k].paramId == BRLENS_CCLOCK_UNI ||
				params[k].paramId == BRLENS_CCLOCK_COAL ||
				params[k].paramId == BRLENS_CCLOCK_BD)
				numCalibratedTrees++;
			}
		}
			
	/* we need to add the trees that do not have any branch lengths */
	/* that is, the pure parsimony model trees */
	for (k=0; k<numParams; k++)
		{
		if (params[k].paramType == P_TOPOLOGY)
			{
			if (params[k].paramId == TOPOLOGY_PARSIMONY_UNIFORM ||
				params[k].paramId == TOPOLOGY_PARSIMONY_CONSTRAINED)
				numTrees++;
			}
		}

	/* allocate space for trees */
	/*	one tree is needed for each brlen parameter
		A topology may apply to several trees; a topology parameter
		contains pointers to all trees it applies to */
	if (memAllocs[ALLOC_MCMCTREES] == YES)
		{
		MrBayesPrint ("%s   Space for MCMC trees not free in FillTreeParams\n", spacer);
		return ERROR;
		}
	mcmcTree = (Tree *) malloc (numTrees * 2 * numLocalChains * sizeof(Tree));
	subParamPtrs = (Param **) malloc (numTrees * sizeof (Param *));		/* pointers from topology to brlens (trees) */
	mcmcNodes = (TreeNode *) malloc (numTrees * 2 * numLocalChains * 2 * numLocalTaxa * sizeof (TreeNode));
	mcmcNodePtrs = (TreeNode **) malloc (numTrees * 2 * numLocalChains * 3 * numLocalTaxa * sizeof (TreeNode *));
	if (!mcmcTree || !subParamPtrs || !mcmcNodes || !mcmcNodePtrs)
		{
		MrBayesPrint ("%s   Problem allocating MCMC trees\n", spacer);
		if (mcmcTree) 
			free (mcmcTree);
		if (subParamPtrs) 
			free (subParamPtrs);
		if (mcmcNodes) 
			free (mcmcNodes);
		if (mcmcNodePtrs) 
			free (mcmcNodePtrs);
		return ERROR;
		}
	else
		memAllocs[ALLOC_MCMCTREES] = YES;

	/* set tree pointers */
	nNodes = 0;
	nNodePtrs = 0;
	for (i=0; i<2*numTrees*numLocalChains; i++)
		{
		tree = &mcmcTree[i];
		tree->nodes = mcmcNodes + nNodes;
		nNodes += 2 * numLocalTaxa;
		tree->allDownPass = mcmcNodePtrs + nNodePtrs;
		nNodePtrs += 2 * numLocalTaxa;
		tree->intDownPass = mcmcNodePtrs + nNodePtrs;
		nNodePtrs += numLocalTaxa;
		}

	/* set brlens param pointers and tree values */
	/* the scheme below keeps trees for the same state and chain together */
	nOfTrees = 0;
	for (k=0; k<numParams; k++)
		{
		p = &params[k];
		if (p->paramType == P_BRLENS || p->paramId == TOPOLOGY_PARSIMONY_UNIFORM ||
			p->paramId == TOPOLOGY_PARSIMONY_CONSTRAINED)
			{
			/* set a pointer to the tree */
			p->tree = mcmcTree + nOfTrees;
			p->treeIndex = nOfTrees;
			nOfTrees++;

			/* set appropriate values for all trees */
			if (p->paramId == BRLENS_CLOCK_UNI || p->paramId == BRLENS_CLOCK_BD || p->paramId == BRLENS_CLOCK_COAL)
				{
				for (i=0; i<2*numLocalChains; i++)
					{
					tree = p->tree + i*numTrees;
					tree->isRooted = YES;
					tree->nNodes = 2 * numLocalTaxa;
					tree->nIntNodes = numLocalTaxa - 1;
					tree->nRelParts = p->nRelParts;
					tree->relParts = p->relParts;
					tree->isCalibrated = NO;
					}
				}
			else if (p->paramId == BRLENS_CCLOCK_UNI || p->paramId == BRLENS_CCLOCK_BD || p->paramId == BRLENS_CCLOCK_COAL)
				{
				for (i=0; i<2*numLocalChains; i++)
					{
					tree = p->tree + i*numTrees;
					tree->isRooted = YES;
					tree->nNodes = 2 * numLocalTaxa;
					tree->nIntNodes = numLocalTaxa - 1;
					tree->nRelParts = p->nRelParts;
					tree->relParts = p->relParts;
					tree->isCalibrated = YES;	
					}
				}
			else
				{
				for (i=0; i<2*numLocalChains; i++)
					{
					tree = p->tree + i*numTrees;
					tree->isRooted = NO;
					tree->nNodes = 2 * numLocalTaxa - 2;
					tree->nIntNodes = numLocalTaxa - 2;
					tree->nRelParts = p->nRelParts;
					tree->relParts = p->relParts;
					tree->isCalibrated = NO;
					}
				}
			}
		}

	/* initialize number of subparams for each topology */
	for (k=0; k<numParams; k++)
		if (params[k].paramType == P_TOPOLOGY)
			params[k].nSubParams = 0;
	
	/* count number of trees (brlens) for each topology */
	for (k=0; k<numParams; k++)
		if (params[k].paramType == P_BRLENS)
			{
			p = &params[k];
			q = modelSettings[p->relParts[0]].topology;
			q->nSubParams++;
			}
	/* make sure there is also one subparam for a parsimony tree */
	for (k=0; k<numParams; k++)
		if (params[k].paramType == P_TOPOLOGY)
			{
			p = &params[k];
			if (p->nSubParams == 0)
				p->nSubParams = 1;
			}

	/* set topology pointers and associated tree values */
	/* update paramId */
	nOfParams = 0;
	for (k=0; k<numParams; k++)
		{
		p = &params[k];
		if (p->paramType == P_TOPOLOGY)
			{
			/* set pointers to subparams */
			p->subParams = subParamPtrs + nOfParams;
			nOfParams += p->nSubParams;

			if (p->paramId == TOPOLOGY_PARSIMONY_UNIFORM ||
				p->paramId == TOPOLOGY_PARSIMONY_CONSTRAINED)
				/* pure parsimony topology case */
				{
				/* there is no brlen subparam */
				/* so let subparam point to the param itself */
				p->subParams[0] = p;
				/* p->tree and p->treeIndex have been set above */
				for (chn=0; chn<numLocalChains; chn++)
					{
					tree0 = GetTree (p, chn, 0);
					tree1 = GetTree (p, chn, 1);
					if (p->paramId == TOPOLOGY_PARSIMONY_CONSTRAINED)
						{
						tree0->checkConstraints = tree1->checkConstraints = YES;
						tree0->nConstraints = tree1->nConstraints = modelParams[p->relParts[i]].numActiveConstraints;
						tree0->constraints = tree1->constraints = modelParams[p->relParts[i]].activeConstraints;
						tree0->nLocks = tree1->nLocks = modelParams[p->relParts[i]].numActiveConstraints;
						}
					else
						tree0->checkConstraints = tree1->checkConstraints = NO;
					}
				}
			else
				{
				/* first set brlens pointers for any parsimony partitions */
				for (i=j=0; i<p->nRelParts; i++)
					{
					if (modelSettings[p->relParts[i]].parsModelId == YES)
						{
						modelSettings[p->relParts[i]].brlens = p;
						}
					}

				/* now proceed with pointer assignment and tree setup */
				q = modelSettings[p->relParts[0]].brlens;
				n = 0;	/* number of stored subParams */
				i = 0;	/* relevant partition number  */
				while (i < p->nRelParts)
					{
					for (j=0; j<n; j++)
						if (q == p->subParams[j])
							break;
					
					if (j == n && q != p)	/* a new tree (brlens) for this topology */
						{
						p->subParams[n++] = q;
						for (chn=0; chn<numLocalChains; chn++)
							{
							tree0 = GetTree (q, chn, 0);
							tree1 = GetTree (q, chn, 1);
							if (p->paramId == TOPOLOGY_NCL_CONSTRAINED || p->paramId == TOPOLOGY_CL_CONSTRAINED || p->paramId == TOPOLOGY_CCL_CONSTRAINED)
								{
								tree0->checkConstraints = tree1->checkConstraints = YES;
								tree0->nConstraints = tree1->nConstraints = modelParams[p->relParts[i]].numActiveConstraints;
								tree0->constraints = tree1->constraints = modelParams[p->relParts[i]].activeConstraints;
								tree0->nLocks = tree1->nLocks = modelParams[p->relParts[i]].numActiveConstraints;
								}
							else
								tree0->checkConstraints = tree1->checkConstraints = NO;
							}
						}
					q = modelSettings[p->relParts[++i]].brlens;
					}
				
				p->tree = p->subParams[0]->tree;
				p->treeIndex = p->subParams[0]->treeIndex;
				}

			/* update paramId (important for matching parameter with move types) */
			if (p->paramId == TOPOLOGY_NCL_CONSTRAINED)
				{
				if (p->nSubParams > 1)
					p->paramId = TOPOLOGY_NCL_CONSTRAINED_HETERO;
				else
					p->paramId = TOPOLOGY_NCL_CONSTRAINED_HOMO;
				}
			else if (p->paramId == TOPOLOGY_NCL_UNIFORM)
				{
				if (p->nSubParams > 1)
					p->paramId = TOPOLOGY_NCL_UNIFORM_HETERO;
				else
					p->paramId = TOPOLOGY_NCL_UNIFORM_HOMO;
				}

			else if (p->paramId == TOPOLOGY_CL_CONSTRAINED)
				{
				if (p->nSubParams > 1)
					p->paramId = TOPOLOGY_CL_CONSTRAINED_HETERO;
				else
					p->paramId = TOPOLOGY_CL_CONSTRAINED_HOMO;
				}
			else if (p->paramId == TOPOLOGY_CL_UNIFORM)
				{
				if (p->nSubParams > 1)
					p->paramId = TOPOLOGY_CL_UNIFORM_HETERO;
				else
					p->paramId = TOPOLOGY_CL_UNIFORM_HOMO;
				}

			else if (p->paramId == TOPOLOGY_CCL_CONSTRAINED)
				{
				if (p->nSubParams > 1)
					p->paramId = TOPOLOGY_CCL_CONSTRAINED_HETERO;
				else
					p->paramId = TOPOLOGY_CCL_CONSTRAINED_HOMO;
				}
			else if (p->paramId == TOPOLOGY_CCL_UNIFORM)
				{
				if (p->nSubParams > 1)
					p->paramId = TOPOLOGY_CCL_UNIFORM_HETERO;
				else
					p->paramId = TOPOLOGY_CCL_UNIFORM_HOMO;
				}
				
			}
		}
		
	/* initialize and build starting trees for state 0 */
	for (chn=0; chn<numLocalChains; chn++)
		{
		for (k=0; k<numParams; k++)
			{
			p = &params[k];
			if (p->paramType == P_TOPOLOGY)
				{
				tree = GetTree (p->subParams[0], chn, 0);
				if (BuildStartTree(tree, seed) == ERROR)
					return ERROR;
				for (i=1; i<p->nSubParams; i++)
					{
					tree1 = GetTree (p->subParams[i], chn, 0);
					if (CopyToTreeFromTree(tree1, tree) == ERROR)
						return ERROR;
					}
				}
			}
		}

	return NO_ERROR;
}





int Flip01 (int x)

{

	if (x == 0)
		return (1);
	else
		return (0);
		
}





/*-----------------------------------------------------------------
|
|	FlipOneBit: flip bit n in long *p
|
------------------------------------------------------------------*/
void FlipOneBit (int n, long *p)

{

	long		x;

	p += n/nBitsInALong;
	x = 1 << (n % nBitsInALong);
	(*p) ^= x;

}





void FreeChainMemory (void)

{

	int			i;

	if (memAllocs[ALLOC_CURLNL] == YES)
		{
		free (maxLnL0); 
		free (curLnL);  
		memAllocs[ALLOC_CURLNL] = NO;
		}
	if (memAllocs[ALLOC_CURLNPR] == YES)
		{
		free (curLnPr); 
		memAllocs[ALLOC_CURLNPR] = NO;
		}
	if (memAllocs[ALLOC_CHAINID] == YES)
		{
		free (chainId); 
		memAllocs[ALLOC_CHAINID] = NO;
		}
	if (memAllocs[ALLOC_PARAMVALUES] == YES)
		{
		free (paramValues);
		memAllocs[ALLOC_PARAMVALUES] = NO;
		}
	if (memAllocs[ALLOC_PARAMS] == YES)
		{
		free (params);   
		free (relevantParts);
		memAllocs[ALLOC_PARAMS] = NO;
		}
	if (memAllocs[ALLOC_LOCTAXANAMES] == YES)
		{
		free (localTaxonNames);
		memAllocs[ALLOC_LOCTAXANAMES] = NO;
		}
	if (memAllocs[ALLOC_LOCTAXAAGES] == YES)
		{
		free (localTaxonAges); 
		memAllocs[ALLOC_LOCTAXAAGES] = NO;
		}
	if (memAllocs[ALLOC_PARSMATRIX] == YES)
		{
		free (parsMatrix);     
		memAllocs[ALLOC_PARSMATRIX] = NO;
		}
	if (memAllocs[ALLOC_TERMSTATE] == YES)
		{
		free (termState);      
		memAllocs[ALLOC_TERMSTATE] = NO;
		}
	if (memAllocs[ALLOC_ISPARTAMBIG] == YES)
		{
		free (isPartAmbig);    
		memAllocs[ALLOC_ISPARTAMBIG] = NO;
		}
	if (memAllocs[ALLOC_TERMCONDLIKES] == YES)
		{
#if defined SSE
		_aligned_free (termCondLikes);
#else
		free (termCondLikes);  
#endif
		memAllocs[ALLOC_TERMCONDLIKES] = NO;
		}
	if (memAllocs[ALLOC_INVCONDLIKES] == YES)
		{
                free (invCondLikes);   
		memAllocs[ALLOC_INVCONDLIKES] = NO;
		}
	if (memAllocs[ALLOC_PARSSETS] == YES)
		{
		free (parsSets);       
		free (parsPtrSpace);   
		free (parsPtr);        
		free (parsNodeLengthSpace);
		free (parsNodeLen);      
		memAllocs[ALLOC_PARSSETS] = NO;
		}	
	if (memAllocs[ALLOC_SPR_PARSSETS] == YES)
		{
		free (sprParsMatrix);
		free (sprParsSets);
		free (sprParsPtrSpace);
		free (sprParsPtr);
		memAllocs[ALLOC_SPR_PARSSETS] = NO;
		}	
	if (memAllocs[ALLOC_COMPMATRIX] == YES)
		{
		free (compMatrix);
		memAllocs[ALLOC_COMPMATRIX] = NO;
		}
	if (memAllocs[ALLOC_NUMSITESOFPAT] == YES)
		{
		free (numSitesOfPat);
		memAllocs[ALLOC_NUMSITESOFPAT] = NO;
		}
	if (memAllocs[ALLOC_COMPCOLPOS] == YES)
		{
		free (compColPos);
		memAllocs[ALLOC_COMPCOLPOS] = NO;
		}
	if (memAllocs[ALLOC_COMPCHARPOS] == YES)
		{
		free (compCharPos);
		memAllocs[ALLOC_COMPCHARPOS] = NO;
		}
	if (memAllocs[ALLOC_ORIGCHAR] == YES)
		{
		free (origChar);
		memAllocs[ALLOC_ORIGCHAR] = NO;
		}
	if (memAllocs[ALLOC_CHAINCONDLIKES] == YES)
		{
#if defined SSE
		_aligned_free (chainCondLikes);
#else
		free (chainCondLikes);
#endif
		free (chainCLPtrSpace);
		free (condLikePtr);
		memAllocs[ALLOC_CHAINCONDLIKES] = NO;
		}
	if (memAllocs[ALLOC_CLSCALERS] == YES)
		{
		free (treeScalerSpace);
		free (treeScaler);
		free (nodeScalerSpace);
		free (nodeScaler);
		memAllocs[ALLOC_CLSCALERS] = NO;
		}
	if (memAllocs[ALLOC_MCMCTREES] == YES)
		{
		free (mcmcTree);
		free (subParamPtrs);
		free (mcmcNodes);
		free (mcmcNodePtrs);
		memAllocs[ALLOC_MCMCTREES] = NO;
		}
	if (memAllocs[ALLOC_MOVES] == YES)
		{
		free (moves[0].nAccepted);
		free (moves);
		memAllocs[ALLOC_MOVES] = NO;
		}
	if (memAllocs[ALLOC_TIPROBS] == YES)
		{
#if defined SSE
		_aligned_free (tiProbSpace);
#else
		free (tiProbSpace);
#endif
		free (tiProbs);
		memAllocs[ALLOC_TIPROBS] = NO;
		}
	if (memAllocs[ALLOC_PRELIKES] == YES)
		{
		free (preLikeL);
		memAllocs[ALLOC_PRELIKES] = NO;
		}
	if (memAllocs[ALLOC_CIJK] == YES)
		{
		free (cijkSpace);
		free (cijks);
		memAllocs[ALLOC_CIJK] = NO;
		}
	if (memAllocs[ALLOC_RATEPROBS] == YES)
		{
		free (rateProbSpace);
		free (rateProbs);
		memAllocs[ALLOC_RATEPROBS] = NO;
		}
	if (memAllocs[ALLOC_SITEJUMP] == YES)
		{
		free (siteJump);
		memAllocs[ALLOC_SITEJUMP] = NO;
		}
	if (memAllocs[ALLOC_MARKOVTIS] == YES)
		{
		for (i=0; i<MAX_SMALL_JUMP; i++)
			if (markovTi[i] != NULL)
				FreeSquareDoubleMatrix(markovTi[i]);
		FreeSquareDoubleMatrix(markovTiN);
		memAllocs[ALLOC_MARKOVTIS] = NO;
		}
	if (memAllocs[ALLOC_STDTYPE] == YES)
		{
		free (stdType);
		memAllocs[ALLOC_STDTYPE] = NO;
		}
	if (memAllocs[ALLOC_SWAPINFO] == YES)
		{
		for (i=0; i<chainParams.numRuns; i++)
			FreeSquareIntegerMatrix(swapInfo[i]);
		free (swapInfo);
		memAllocs[ALLOC_SWAPINFO] = NO;
		}
	if (memAllocs[ALLOC_SYMPIINDEX] == YES)
		{
		free (sympiIndex);
		memAllocs[ALLOC_SYMPIINDEX] = NO;
		}
	if (memAllocs[ALLOC_POSSELPROBS] == YES)
		{
		free (posSelProbs);
		memAllocs[ALLOC_POSSELPROBS] = NO;
		}
	if (memAllocs[ALLOC_ANCSTATECONDLIKES] == YES)
		{
		free (ancStateCondLikes);
		memAllocs[ALLOC_ANCSTATECONDLIKES] = NO;
		}
	if (memAllocs[ALLOC_PFCOUNTERS] == YES)
		{
		free (partition[0]);
		free (partition);
		for (i=0; i<numTrees; i++)
			Tfree (partFreqTreeRoot[i]);
		free (partFreqTreeRoot);
		memAllocs[ALLOC_PFCOUNTERS] = NO;
		}
	if (memAllocs[ALLOC_FILEPOINTERS] == YES)
		{
		CloseMBPrintFiles ();
		if (fpCal != NULL)
			{
			free (fpCal[0]);
			free (fpCal);
			}
		if (fpTree != NULL)
			{
			free (fpTree[0]);
			free (fpTree);
			}
		if (fpParm != NULL)
			free (fpParm);
		fpParm = NULL;
		fpTree = NULL;
		fpCal = NULL;
		fpMcmc = NULL;
		memAllocs[ALLOC_FILEPOINTERS] = NO;
		}
	if (memAllocs[ALLOC_STATS] == YES)
		{
		if (chainParams.allComps == YES)
			{
			for (i=0; i<chainParams.numRuns; i++)
				FreeSquareDoubleMatrix (chainParams.stat[i].pair);
			}
		free (chainParams.stat);
		memAllocs[ALLOC_STATS] = NO;
		}
	if (memAllocs[ALLOC_DIAGNUTREE] == YES)
		{
		FreeTree (chainParams.utree);
		memAllocs[ALLOC_DIAGNUTREE] = NO;
		}
	if (memAllocs[ALLOC_DIAGNRTREE] == YES)
		{
		FreeTree (chainParams.rtree);
		memAllocs[ALLOC_DIAGNRTREE] = NO;
		}
}





/* FreeTree: Free memory space for a tree (unrooted or rooted) */
void FreeTree (Tree *t)
{
        free (t->nodes);
	free (t->allDownPass);
	free (t);
}




void GetChainIds (void)

{

	/* Fill <chainId[]> with the global chain number.
	   Ex. For proc_0, chain[0] = 0;
			 chain[1] = 1;
			 chain[2] = 2; (numchains = 3)
	   For proc_1, chain[0] = 3;
			 chain[1] = 4; (numchains = 2)
	   etc... 
	*/

#	if defined (MPI_ENABLED)

	int		i, proc, numChainsForProc, numGlobalChains;
	int 	id;
	int		remainder;

	/* calculate global number of chains */
	numGlobalChains = chainParams.numChains * chainParams.numRuns;
	
	/* there are <remainder> chains left over after
	   load balancing the chains */
	remainder = numGlobalChains % num_procs;

	/* get the number of chains handled by this proc */
	numChainsForProc = (int) (numGlobalChains / num_procs);

	/* we must distribute the remaining chains (causing
	   the chain load between procs to become unbalanced) */
	if (proc_id < remainder) 
		numChainsForProc++;

	/* NOTE: at this point, procs can have different number of numChainsForProc
	   (one more for chains < remainder, one less for procs larger than or equal
	   to the remainder) */

	id = 0;
	for (proc=0; proc<num_procs; proc++)
		{
		/* assign or increment chain id */
		if (proc == proc_id)
			{
			for (i=0; i<numChainsForProc; i++)
				chainId[i] = id++;
			}
		else
			{
			/* procs below the remainder have 1 more chain
			   than procs above */
			if (proc < remainder) 
				{
				for (i=0; i<(numGlobalChains / num_procs) + 1; i++)
					id++;
				}
			/* procs above the remainder have one less chain
			   than procs below */
			else
				{
				for (i=0; i<(numGlobalChains / num_procs); i++)
					id++;
				}
			}
		}
#	else

	int		chn;
	
	for (chn=0; chn<numLocalChains; chn++)
		chainId[chn] = chn;

#	endif
		
}





int GetEmpiricalFreqs (int *relParts, int nRelParts)

{

	int				i, j, k, m, n, thePartition, nuc[20], ns, temp, isDNA, isProtein, firstRel;
	MrBFlt			freqN[20], sum, sumN[20]/*, rawCounts[20]*/;

	isDNA = isProtein = NO;
	ns = 0;
	firstRel = 0;
	for (i=0; i<nRelParts; i++)
		{
		thePartition = relParts[i];
		if (i == 0)
			{
			if (modelParams[thePartition].dataType == DNA || modelParams[i].dataType == RNA)
				{
				isDNA = YES;
				ns = 4;
				}
			else if (modelParams[thePartition].dataType == PROTEIN)
				{
				isProtein = YES;
				ns = 20;
				}
			else if (modelParams[thePartition].dataType == RESTRICTION)
				{
				ns = 2;
				}
			else
				{
				MrBayesPrint ("%s   Cannot get empirical state frequencies for this datatype (%d)\n", spacer, modelSettings[i].dataType);
				return (ERROR);
				}
			firstRel = thePartition;
			}
		else
			{
			if (modelParams[thePartition].dataType == DNA || modelParams[i].dataType == RNA)
				temp = 4;
			else if (modelParams[thePartition].dataType == PROTEIN)
				temp = 20;
			else if (modelParams[thePartition].dataType == RESTRICTION)
				temp = 2;
			else
				{
				MrBayesPrint ("%s   Unknown data type in GetEmpiricalFreqs\n", spacer);
				return (ERROR);
				}
			if (ns != temp)
				{
				MrBayesPrint ("%s   Averaging state frequencies over partitions with different data types\n", spacer);
				return (ERROR);
				}
			}
		}
	if (ns == 0)
		{
		MrBayesPrint ("%s   Could not find a relevant partition\n", spacer);
		return (ERROR);
		}

	for (i=0; i<200; i++)
		empiricalFreqs[i] = 0.0;
	
	for (m=0; m<ns; m++)
		freqN[m] =  1.0 / ns;
		
	/* for (m=0; m<ns; m++)
	   rawCounts[m] = 0.0; NEVER USED */
		
	for (m=0; m<ns; m++)
		sumN[m] = 0.0;
	for (k=0; k<nRelParts; k++)
		{
		thePartition = relParts[k];
		for (i=0; i<numTaxa; i++)
			{
			if (taxaInfo[i].isDeleted == NO)
				{
				for (j=0; j<numChar; j++)
					{
					if (charInfo[j].isExcluded == NO && charInfo[j].partitionId[partitionNum-1] - 1 == thePartition)
						{
						if (isDNA == YES)
							GetPossibleNucs (matrix[pos(i,j,numChar)], nuc);
						else if (isProtein == YES)
							GetPossibleAAs (matrix[pos(i,j,numChar)], nuc);
						else
							GetPossibleRestrictionSites (matrix[pos(i,j,numChar)], nuc);
						sum = 0.0;
						for (m=0; m<ns; m++)
							sum += freqN[m] * nuc[m];
						for (m=0; m<ns; m++)
							sumN[m] += freqN[m] * nuc[m] / sum;
						}
					}
				}
			}
		}
	sum = 0.0;
	for (m=0; m<ns; m++)
		sum += sumN[m];
	for (m=0; m<ns; m++)
		freqN[m] = sumN[m] / sum;

	if (modelParams[firstRel].dataType == DNA || modelParams[firstRel].dataType == RNA)
		{
		if (!strcmp(modelParams[firstRel].nucModel, "4by4"))
			{
			for (m=0; m<ns; m++)
				empiricalFreqs[m] = freqN[m];
			}
		else if (!strcmp(modelParams[firstRel].nucModel, "Doublet"))
			{
			i = 0;
			for (m=0; m<ns; m++)
				for (n=0; n<ns; n++)
					empiricalFreqs[i++] = freqN[m] * freqN[n];
			}
		else
			{
			if (!strcmp(modelParams[firstRel].geneticCode, "Universal"))
				{
				for (i=0; i<61; i++)
					empiricalFreqs[i] = freqN[modelParams[firstRel].codonNucs[i][0]] * freqN[modelParams[firstRel].codonNucs[i][1]] * freqN[modelParams[firstRel].codonNucs[i][2]];
				}
			else if (!strcmp(modelParams[firstRel].geneticCode, "Vertmt"))
				{
				for (i=0; i<60; i++)
					empiricalFreqs[i] = freqN[modelParams[firstRel].codonNucs[i][0]] * freqN[modelParams[firstRel].codonNucs[i][1]] * freqN[modelParams[firstRel].codonNucs[i][2]];
				}
			else if (!strcmp(modelParams[firstRel].geneticCode, "Mycoplasma"))
				{
				for (i=0; i<62; i++)
					empiricalFreqs[i] = freqN[modelParams[firstRel].codonNucs[i][0]] * freqN[modelParams[firstRel].codonNucs[i][1]] * freqN[modelParams[firstRel].codonNucs[i][2]];
				}
			else if (!strcmp(modelParams[firstRel].geneticCode, "Yeast"))
				{
				for (i=0; i<62; i++)
					empiricalFreqs[i] = freqN[modelParams[firstRel].codonNucs[i][0]] * freqN[modelParams[firstRel].codonNucs[i][1]] * freqN[modelParams[firstRel].codonNucs[i][2]];
				}
			else if (!strcmp(modelParams[firstRel].geneticCode, "Ciliates"))
				{
				for (i=0; i<63; i++)
					empiricalFreqs[i] = freqN[modelParams[firstRel].codonNucs[i][0]] * freqN[modelParams[firstRel].codonNucs[i][1]] * freqN[modelParams[firstRel].codonNucs[i][2]];
				}
			else if (!strcmp(modelParams[firstRel].geneticCode, "Metmt"))
				{
				for (i=0; i<62; i++)
					empiricalFreqs[i] = freqN[modelParams[firstRel].codonNucs[i][0]] * freqN[modelParams[firstRel].codonNucs[i][1]] * freqN[modelParams[firstRel].codonNucs[i][2]];
				}
			sum = 0.0;
			for (i=0; i<64; i++)
				sum += empiricalFreqs[i];
			for (i=0; i<64; i++)
				empiricalFreqs[i] /= sum;
			
			}
		}
	else
		{
		for (m=0; m<ns; m++)
			empiricalFreqs[m] = freqN[m];
		}
		
	return (NO_ERROR);

}





/* get down pass for tree t (wrapper function) */
void GetDownPass (Tree *t)

{

	int i, j;

	i = j = 0;
	GetNodeDownPass (t, t->root, &i, &j);
		
}





/* get the actual down pass sequences */
void GetNodeDownPass (Tree *t, TreeNode *p, int *i, int *j)

{
	
	if (p != NULL )
		{
		GetNodeDownPass (t, p->left,  i, j);
		GetNodeDownPass (t, p->right, i, j);
		if (p->left != NULL && p->right != NULL && p->anc != NULL)
			{
			t->intDownPass[(*i)++] = p;
			t->allDownPass[(*j)++] = p;
			}
		else if (p->left == NULL && p->right == NULL && p->anc != NULL)
			{
			t->allDownPass[(*j)++] = p;
			}
		else if (p->left != NULL && p->right == NULL && p->anc == NULL)
			{
			t->allDownPass[(*j)++] = p;
			}
		}
		
}





/* get down pass for polytomous tree t (wrapper function) */
void GetPolyDownPass (PolyTree *t)

{

	int i, j;

	i = j = 0;
	GetPolyNodeDownPass (t, t->root, &i, &j);
		
}





/* get the actual down pass sequences for a polytomous tree */
void GetPolyNodeDownPass (PolyTree *t, PolyNode *p, int *i, int *j)

{
	
	PolyNode	*q;
	
	if (p->left != NULL)
		{
		for (q=p->left; q!=NULL; q=q->sib)
			GetPolyNodeDownPass(t, q, i, j);
		}

	t->allDownPass[(*i)++] = p;
	if (p->left != NULL )
		t->intDownPass[(*j)++] = p;

}





MrBFlt	*GetParamVals (Param *parm, int chain, int state)

{

	return parm->values + (2 * chain + state) * paramValsRowSize;
	
}





MrBFlt	*GetParamSubVals (Param *parm, int chain, int state)

{

	return parm->subValues + (2 * chain + state) * paramValsRowSize;
	
}





int GetParsimonyBrlens (Tree *t, int chain, MrBFlt *brlens)

{
	
	int				c, i, n, division;
	long			*pL, *pR, *pP, *pA, x;
	CLFlt			*nSitesOfPat;
	TreeNode        *p;
	ModelInfo		*m;

	/* Reset all brlens */
	for (i=0; i<t->nNodes-1; i++)
		brlens[i] = 0.0;
	
	/* Loop over divisions */
	for (n=0; n<t->nRelParts; n++)		
		{
		division = t->relParts[n];

		/* Find model settings */
		m = &modelSettings[division];

		/* Find number of site patterns */
		nSitesOfPat = numSitesOfPat + ((chainId[chain] % chainParams.numChains) * numCompressedChars) + m->compCharStart;

		/* Make uppass node by node */
		for (i=t->nIntNodes-1; i>=0; i--)
			{
			p = t->intDownPass[i];

			/* Find downpass (pL, pR, pP) and final-pass (pA) parsimony sets for the node and its environment */
			pL    = parsPtr[chain][p->left->index]  + m->parsMatrixStart + Bit(division, p->left->clSpace ) * parsMatrixRowSize;
			pR    = parsPtr[chain][p->right->index] + m->parsMatrixStart + Bit(division, p->right->clSpace) * parsMatrixRowSize;
			pP    = parsPtr[chain][p->index]        + m->parsMatrixStart + Bit(division, p->clSpace       ) * parsMatrixRowSize;
			pA    = parsPtr[chain][p->anc->index]   + m->parsMatrixStart + Bit(division, p->anc->clSpace  ) * parsMatrixRowSize;
			
			for (c=0; c<m->numChars; c++)
				{
				x = pP[c] & pA[c];
				if (x == 0)
					{
					if ((pL[c] & pR[c]) != 0)
						x = pP[c] | ((pL[c] | pR[c]) & pA[c]);
					else
						x = pP[c] | pA[c];
					}
				pP[c] = x;
				}
			}

		/* Record branch lengths (all nodes except root) in downpass */
		for (i=0; i<t->nNodes-1; i++)
			{
			p = t->allDownPass[i];

			/* Find final-pass parsimony sets for the node and its ancestor */
			pP    = parsPtr[chain][p->index]        + m->parsMatrixStart + Bit(division, p->clSpace       ) * parsMatrixRowSize;
			pA    = parsPtr[chain][p->anc->index]   + m->parsMatrixStart + Bit(division, p->anc->clSpace  ) * parsMatrixRowSize;
			
			for (c=0; c<m->numChars; c++)
				{
				if ((pP[c] & pA[c]) == 0)
					brlens[i] += nSitesOfPat[c];
				}
			}
		}

	return (NO_ERROR);

}



int GetParsimonyDownStates (Tree *t, int chain)

{
	
	int				c, i, n, division;
	long			*pL, *pR, *pP, x;
	CLFlt			*nSitesOfPat; 
	TreeNode		*p;
	ModelInfo		*m;

	for (n=0; n<t->nRelParts; n++)
		{
		division = t->relParts[n];
			
		/* Find model settings */
		m = &modelSettings[division];

		/* Find number of site patterns */
		nSitesOfPat = numSitesOfPat + ((chainId[chain] % chainParams.numChains) * numCompressedChars) + m->compCharStart;
	
		/* Make downpass node by node */
		for (i=0; i<t->nIntNodes; i++)
			{
			p = t->intDownPass[i];

			/* find downpass parsimony sets for the node and its environment */
			pL   = parsPtr[chain][p->left->index]  + m->parsMatrixStart + Bit(division, p->left->clSpace )     * parsMatrixRowSize;
			pR   = parsPtr[chain][p->right->index] + m->parsMatrixStart + Bit(division, p->right->clSpace)     * parsMatrixRowSize;
			pP   = parsPtr[chain][p->index]        + m->parsMatrixStart + Bit(division, p->clSpace       )     * parsMatrixRowSize;
		
			for (c=0; c<m->numChars; c++)
				{
				x = pL[c] & pR[c];
				if (x == 0)
					{
					x = pL[c] | pR[c];
					}
				pP[c] = x;
				}
			}
		}

	return (NO_ERROR);

}




MrBFlt GetParsimonyLength (Tree *t, int chain)

{
	
	int				c, i, n, division;
	long			*pL, *pR, *pP, *pA, x;
	CLFlt			*nSitesOfPat;
	MrBFlt			length;
	TreeNode		*p;
	ModelInfo		*m;

	/* Reset length */
	length = 0.0;

	for (n=0; n<t->nRelParts; n++)		
		{
		division = t->relParts[n];

		/* Find model settings */
		m = &modelSettings[division];

		/* Find number of site patterns */
		nSitesOfPat = numSitesOfPat + ((chainId[chain] % chainParams.numChains) * numCompressedChars) + m->compCharStart;

		/* Make downpass node by node */
		for (i=0; i<t->nIntNodes; i++)
			{
			p = t->intDownPass[i];

			/* find downpass parsimony sets for the node and its environment */
			pL    = parsPtr[chain][p->left->index]  + m->parsMatrixStart + Bit(division, p->left->clSpace )     * parsMatrixRowSize;
			pR    = parsPtr[chain][p->right->index] + m->parsMatrixStart + Bit(division, p->right->clSpace)     * parsMatrixRowSize;
			pP    = parsPtr[chain][p->index]        + m->parsMatrixStart + Bit(division, p->clSpace       )     * parsMatrixRowSize;
			
			if (p->anc->anc != NULL)
				{
				for (c=0; c<m->numChars; c++)
					{
					x = pL[c] & pR[c];
					if (x == 0)
						{
						x = pL[c] | pR[c];
						length += nSitesOfPat[c];
						}
					pP[c] = x;
					}
				}
			else
				{
				pA    = parsPtr[chain][p->anc->index]   + m->parsMatrixStart + Bit(division, p->anc->clSpace       )     * parsMatrixRowSize;
				for (c=0; c<m->numChars; c++)
					{
					x = pL[c] & pR[c];
					if (x == 0)
						{
						x = pL[c] | pR[c];
						length += nSitesOfPat[c];
						}
					pP[c] = x;
					if ((x & pA[c]) == 0)
						{
						length += nSitesOfPat[c];
						}
					}
				}
			}
		}

	return length;

}




void GetParsimonySubtreeRootstate (Tree *t, TreeNode *root, int chain)

{
	
	int				c, i, n, division;
	long			*pD, *pP, *pA, x;
	TreeNode		*p;
	ModelInfo		*m;

	/* Lopp over divisions */
	for (n=0; n<t->nRelParts; n++)
		{
		division = t->relParts[n];
			
		/* Find model settings */
		m = &modelSettings[division];

		for (i=0; i<t->nNodes; i++)
			{
			p = t->allDownPass[i];
			p->marked = NO;
			}

		p = root;
		while (p->anc != NULL)
			{
			p->marked = YES;
			p = p->anc;
			}

		/* Make uppass node by node */
		for (i=t->nIntNodes-1; i>=0; i--)
			{
			p = t->intDownPass[i];

			/* continue if no work needs to be done */
			if (p->marked == NO)
				continue;

			/* find downpass and uppass parsimony sets for the node and its environment */
			pP    = parsPtr[chain][p->index     ]  + m->parsMatrixStart + Bit(division, p->clSpace      )     * parsMatrixRowSize;
			if (p->left->marked == YES)
				pD    = parsPtr[chain][p->right->index]  + m->parsMatrixStart + Bit(division, p->right->clSpace )     * parsMatrixRowSize;
			else
				pD    = parsPtr[chain][p->left->index ]  + m->parsMatrixStart + Bit(division, p->left->clSpace )     * parsMatrixRowSize;
		
			pA    = parsPtr[chain][p->anc->index] + m->parsMatrixStart + Bit(division, p->anc->clSpace)     * parsMatrixRowSize;
		
			for (c=0; c<m->numChars; c++)
				{
				x = pD[c] & pA[c];
				if (x == 0)
					{
					x = (pD[c] | pA[c]);
					}
				pP[c] = x;
				}

			if (p == root)
				break;
			}
		}

}




void GetPossibleAAs (int aaCode, int aa[])

{

	int		m;
	
	for (m=0; m<20; m++)
		aa[m] = 0;
		
	if (aaCode > 0 && aaCode <= 20)
		aa[aaCode-1] = 1;
	else
		{
		for (m=0; m<20; m++)
			aa[m] = 1;
		}
#	if 0
	printf ("%2d -- ", aaCode);
	for (m=0; m<20; m++)
		printf("%d", aa[m]);
	printf ("\n");
#	endif

}





void GetPossibleNucs (int nucCode, int nuc[])

{

	if (nucCode == 1)
		{
		nuc[0] = 1;
		nuc[1] = 0;
		nuc[2] = 0;
		nuc[3] = 0;
		}
	else if (nucCode == 2)
		{
		nuc[0] = 0;
		nuc[1] = 1;
		nuc[2] = 0;
		nuc[3] = 0;
		}
	else if (nucCode == 3)
		{
		nuc[0] = 1;
		nuc[1] = 1;
		nuc[2] = 0;
		nuc[3] = 0;
		}
	else if (nucCode == 4)
		{
		nuc[0] = 0;
		nuc[1] = 0;
		nuc[2] = 1;
		nuc[3] = 0;
		}
	else if (nucCode == 5)
		{
		nuc[0] = 1;
		nuc[1] = 0;
		nuc[2] = 1;
		nuc[3] = 0;
		}
	else if (nucCode == 6)
		{
		nuc[0] = 0;
		nuc[1] = 1;
		nuc[2] = 1;
		nuc[3] = 0;
		}
	else if (nucCode == 7)
		{
		nuc[0] = 1;
		nuc[1] = 1;
		nuc[2] = 1;
		nuc[3] = 0;
		}
	else if (nucCode == 8)
		{
		nuc[0] = 0;
		nuc[1] = 0;
		nuc[2] = 0;
		nuc[3] = 1;
		}
	else if (nucCode == 9)
		{
		nuc[0] = 1;
		nuc[1] = 0;
		nuc[2] = 0;
		nuc[3] = 1;
		}
	else if (nucCode == 10)
		{
		nuc[0] = 0;
		nuc[1] = 1;
		nuc[2] = 0;
		nuc[3] = 1;
		}
	else if (nucCode == 11)
		{
		nuc[0] = 1;
		nuc[1] = 1;
		nuc[2] = 0;
		nuc[3] = 1;
		}
	else if (nucCode == 12)
		{
		nuc[0] = 0;
		nuc[1] = 0;
		nuc[2] = 1;
		nuc[3] = 1;
		}
	else if (nucCode == 13)
		{
		nuc[0] = 1;
		nuc[1] = 0;
		nuc[2] = 1;
		nuc[3] = 1;
		}
	else if (nucCode == 14)
		{
		nuc[0] = 0;
		nuc[1] = 1;
		nuc[2] = 1;
		nuc[3] = 1;
		}
	else
		{
		nuc[0] = 1;
		nuc[1] = 1;
		nuc[2] = 1;
		nuc[3] = 1;
		}

}




void GetPossibleRestrictionSites (int resSiteCode, int *sites)

{

	int		m;
	
	for (m=0; m<2; m++)
		sites[m] = 0;
		
	if (resSiteCode == 1)
		sites[0] = 1;
	else if (resSiteCode == 2)
		sites[1] = 1;
	else
		sites[0] = sites[1] = 1;

#	if 0
	printf ("%2d -- ", aaCode);
	for (m=0; m<20; m++)
		printf("%d", aa[m]);
	printf ("\n");
#	endif

}




int GetRandomEmbeddedSubtree (Tree *t, int nTerminals, long *seed, int *nEmbeddedTrees)

{
	
	int			i, j, k, n, ran, *pP, *pL, *pR, nLeaves, *nSubTrees;
	TreeNode	*p=NULL, **leaf;

	/* Calculate number of leaves in subtree (number of terminals minus the root) */
	nLeaves = nTerminals - 1;
	
	/* Initialize all flags */
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		p->marked = NO;
		p->x = 0;
		p->y = 0;
		}
	
	/* Allocate memory */
	nSubTrees = (int *) calloc (nTerminals * t->nNodes, sizeof(int));
	if (!nSubTrees)
		return (ERROR);
	leaf = (TreeNode **) malloc (nLeaves * sizeof (TreeNode *));
	if (!leaf)
		{
		free (nSubTrees);
		return (ERROR);
		}

	/* Calculate how many embedded trees rooted at each node */
	(*nEmbeddedTrees) = 0;
	for (i=0; i<t->nNodes-1; i++)
		{
		p = t->allDownPass[i];
		if (p->left == NULL)
			{
			p->x = 0;
			nSubTrees[p->index*nTerminals + 1] = 1;
			}
		else
			{
			pL = nSubTrees + p->left->index*nTerminals;
			pR = nSubTrees + p->right->index*nTerminals;
			pP = nSubTrees + p->index*nTerminals;
			pP[1] = 1;
			for (j=2; j<=nLeaves; j++)
				{
				for (k=1; k<j; k++)
					{
					pP[j] += pL[k] * pR[j-k];
					}
				}
			p->x = pP[nLeaves];
			(*nEmbeddedTrees) += p->x;
			}
		}

	/* Randomly select one embedded tree of the right size */
	ran = (int) (RandomNumber(seed) * (*nEmbeddedTrees));

	/* Find the interior root corresponding to this tree */
	for (i=j=0; i<t->nIntNodes; i++)
		{
		p = t->intDownPass[i];
		j += p->x;
		if (j>ran)
			break;
		}

	/* Find one random embedded tree with this root */
	p->y = nLeaves;
	p->marked = YES;
	leaf[0] = p;
	n = 1;
	while (n < nLeaves)
		{
		/* select a node with more than one descendant */
		for (i=0; i<n; i++)
			{
			p = leaf[i];
			if (p->y > 1)
				break;
			}

		/* break it into descendants */
		pL = nSubTrees + p->left->index*nTerminals;
		pR = nSubTrees + p->right->index*nTerminals;
		pP = nSubTrees + p->index*nTerminals;
		ran = (int) (RandomNumber (seed) * pP[p->y]);
		k = 0;
		for (j=1; j<p->y; j++)
			{
			k += pL[j] * pR[p->y-j];
			if (k > ran)
				break;
			}
		p->left->y = j;
		p->right->y = p->y - j;
		p->left->marked = YES;
		p->right->marked = YES;
		leaf[i] = p->left;
		leaf[n++] = p->right;
		}

	free (nSubTrees);
	free (leaf);

	return (NO_ERROR);
}

		
		
		
/* GetRate: retrieve the base rate for the division and chain in current state */
MrBFlt GetRate (int division, int chain)

{

	Param	*p;
	MrBFlt	*values;
	int		i;

	p = modelSettings[division].rateMult;
	values = GetParamVals (p, chain, state[chain]);

	if (p->nValues == 1)
		return values[0];

	for (i=0; i<p->nRelParts; i++)
		{
		if (p->relParts[i] == division)
			return values[i];
		}

	return (0.0);

}





void GetSprParsimonyLengths (int chain, int nNodes1, int nNodes2, TreeNode **subTree1DP, TreeNode **subTree2DP, TreeNode *root2, MrBFlt *pLengths)

{

	int				i, j, c, d;
	long			*cl, *clL, *clR, *clA, *clU, *clM, x, y, z;
	CLFlt			temp1, temp2=0.0;
	CLFlt			*nSitesOfPat;
	TreeNode		*p, *q;
	ModelInfo		*m;

	/* get stateset for moveable tree (rooted at root2) */
	for (d=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];
		nSitesOfPat = numSitesOfPat + ((chainId[chain] % chainParams.numChains) * numCompressedChars) + m->compCharStart;
		if (m->upDateCl == YES)
			{
			
			temp2 = 0.0;
			for (i=0; i<nNodes2; i++)
				{
				q = subTree2DP[i];
				if (q->left != NULL && q->right != NULL && q->anc != NULL)
					{
					
					cl  = sprParsPtr[chain][q->index]        + UPPER * sprParsMatrixRowSize;
					clL = sprParsPtr[chain][q->left->index]  + UPPER * sprParsMatrixRowSize;
					clR = sprParsPtr[chain][q->right->index] + UPPER * sprParsMatrixRowSize;
					for (c=m->sprParsMatrixStart, j=0; c<m->sprParsMatrixStop; c++)
						{
						x = clL[c];
						y = clR[c];
						z = x & y;
						if (z == 0)
							{
							cl[c] = x | y;
							temp2 += numSitesOfPat[j++];
							}
						else
							cl[c] = z;
						}
					}
				}
			
			}
		}

	/* get stateset on up branches for unmoveable tree (rooted at root1) */
	for (d=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];
		nSitesOfPat = numSitesOfPat + ((chainId[chain] % chainParams.numChains) * numCompressedChars) + m->compCharStart;
		if (m->upDateCl == YES)
			{
			
			for (i=0; i<nNodes1; i++)
				{
				q = subTree1DP[i];
				if (q->left == NULL && q->right == NULL && q->anc != NULL)
					{
					q->uL = 0;
					}
				else if (q->anc == NULL)
					{
					cl  = sprParsPtr[chain][q->left->index] + LOWER * sprParsMatrixRowSize;
					clA = sprParsPtr[chain][q->index]       + UPPER * sprParsMatrixRowSize;
					for (c=m->sprParsMatrixStart; c<m->sprParsMatrixStop; c++)
						{
						cl[c] = clA[c];
						}
					q->uL = q->left->dL = 0;
					}
				else if (q->left != NULL && q->right != NULL && q->anc != NULL)
					{
					cl  = sprParsPtr[chain][q->index]        + UPPER * sprParsMatrixRowSize;
					clL = sprParsPtr[chain][q->left->index]  + UPPER * sprParsMatrixRowSize;
					clR = sprParsPtr[chain][q->right->index] + UPPER * sprParsMatrixRowSize;
					temp1 = 0.0;;
					for (c=m->sprParsMatrixStart, j=0; c<m->sprParsMatrixStop; c++)
						{
						x = clL[c];
						y = clR[c];
						z = x & y;
						if (z == 0)
							{
							cl[c] = x | y;
							temp1 += numSitesOfPat[j++];
							}
						else
							cl[c] = z;
						}
					q->uL = q->left->uL + q->right->uL + (int)temp1;
					}
				}
			
			}
		}

	/* get stateset on down branches for unmoveable tree (rooted at root1) */
	for (d=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];
		nSitesOfPat = numSitesOfPat + ((chainId[chain] % chainParams.numChains) * numCompressedChars) + m->compCharStart;
		if (m->upDateCl == YES)
			{
			
			for (i=nNodes1-1; i>=0; i--)
				{
				q = subTree1DP[i];
				if (q->anc != NULL)
					{
					if (q->anc->anc != NULL)
						{
						cl  = sprParsPtr[chain][q->index] + LOWER * sprParsMatrixRowSize;
						if (q->anc->left == q)
							clU = sprParsPtr[chain][q->anc->right->index] + UPPER * sprParsMatrixRowSize;
						else
							clU = sprParsPtr[chain][q->anc->left->index] + UPPER * sprParsMatrixRowSize;
						clA = sprParsPtr[chain][q->anc->index] + LOWER * sprParsMatrixRowSize;
						temp1 = 0.0;
						for (c=m->sprParsMatrixStart, j=0; c<m->sprParsMatrixStop; c++)
							{
							x = clU[c];
							y = clA[c];
							z = x & y;
							if (z == 0)
								{
								cl[c] = x | y;
								temp1 += numSitesOfPat[j++];
								}
							else
								cl[c] = z;
							}
						if (q->anc->left == q)
							q->dL = q->anc->right->uL + q->anc->dL + (int)temp1;
						else
							q->dL = q->anc->left->uL + q->anc->dL + (int)temp1;
						
						cl  = sprParsPtr[chain][q->index] + LOWER  * sprParsMatrixRowSize;
						clM = sprParsPtr[chain][q->index] + MIDDLE * sprParsMatrixRowSize;
						clL = sprParsPtr[chain][q->index] + UPPER  * sprParsMatrixRowSize;
						temp1 = 0.0;
						for (c=m->sprParsMatrixStart, j=0; c<m->sprParsMatrixStop; c++)
							{
							x = cl[c];
							y = clL[c];
							z = x & y;
							if (z == 0)
								{
								clM[c] = x | y;
								temp1 += numSitesOfPat[j++];
								}
							else
								clM[c] = z;
							}
						q->mL = q->uL + q->dL + (int)temp1;
						/*printf ("length(%d) = %d %d %d\n", q->index, q->uL, q->dL, q->mL);*/
						}
					else
						{
						cl  = sprParsPtr[chain][q->index] + LOWER  * sprParsMatrixRowSize;
						clM = sprParsPtr[chain][q->index] + MIDDLE * sprParsMatrixRowSize;
						clL = sprParsPtr[chain][q->index] + UPPER  * sprParsMatrixRowSize;
						temp1 = 0.0;
						for (c=m->sprParsMatrixStart, j=0; c<m->sprParsMatrixStop; c++)
							{
							x = cl[c];
							y = clL[c];
							z = x & y;
							if (z == 0)
								{
								clM[c] = x | y;
								temp1 += numSitesOfPat[j++];
								}
							else
								clM[c] = z;
							}
						q->mL = q->uL + q->dL + (int)temp1;
						/*printf ("lengtH(%d) = %d %d %d\n", q->index, q->uL, q->dL, q->mL);*/
						}
					}
					
				}

			}
		}
	
	/* Calculate parsimony lengths of all possible attachment points. */
	for (c=0; c<nNodes1; c++)
		pLengths[c] = 0.0;
	for (d=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];
		nSitesOfPat = numSitesOfPat + ((chainId[chain] % chainParams.numChains) * numCompressedChars) + m->compCharStart;
		if (m->upDateCl == YES)
			{
			
			p = root2->left;
			for (i=0; i<nNodes1; i++)
				{
				pLengths[i] = 0.0;
				q = subTree1DP[i];
				if (q->anc != NULL)
					{
					cl  = sprParsPtr[chain][p->index] + UPPER  * sprParsMatrixRowSize;
					clM = sprParsPtr[chain][q->index] + MIDDLE * sprParsMatrixRowSize;
					temp1 = 0.0;
					for (c=m->sprParsMatrixStart, j=0; c<m->sprParsMatrixStop; c++)
						{
						x = cl[c];
						y = clM[c];
						z = x & y;
						if (z == 0)
							temp1 += numSitesOfPat[j++];
						}
					pLengths[i] += q->mL + (int)(temp1 + temp2);
					}
				}	
			
			}
		}

#	if 0
	for (i=0; i<nNodes1; i++)
		{
		p = subTree1DP[i];
		if (p->anc != NULL)
			printf ("%4d -- %lf \n", p->index, pLengths[i]);
		else	
			printf ("%4d -- NULL Ancestor \n ", p->index);
		}
#	endif

}





void GetStamp (void)

{

	int		i;
	char	temp[10];
	
	strcpy (stamp, "");
	for (i=0; i<10; i++)
		{
		sprintf (temp, "%d", (int)(RandomNumber(&runIDSeed) * 10));
		strcat (stamp, temp);
		}
	MrBayesPrint ("%s   MCMC stamp = %s\n", spacer, stamp);

}





void GetSwappers (int *swapA, int *swapB, int run)

{

	int			i;
	
	/* this works for both the serial and parallel versions because the swapSeed is identical for all
		processors, ensuring they all get the same sequence of chainIds to swap */
#	if defined (MPI_ENABLED)

	/* For now, we wonly allow random swaps in the MPI version. Other schemes require
	   tagging of messages, or a dedicated server node doing message processing.      */
	(*swapA) = (int) (RandomNumber(&swapSeed) * chainParams.numChains);
	(*swapB) = (int) (RandomNumber(&swapSeed) * (chainParams.numChains - 1));
	if ((*swapB) == (*swapA))
		(*swapB) = chainParams.numChains - 1;

#	else

	if (chainParams.swapAdjacentOnly == NO)
		{
		(*swapA) = (int) (RandomNumber(&swapSeed) * chainParams.numChains);
		(*swapB) = (int) (RandomNumber(&swapSeed) * (chainParams.numChains - 1));
		if ((*swapB) == (*swapA))
			(*swapB) = chainParams.numChains - 1;
		}
	else
		{
		(*swapA) = (int) (RandomNumber(&swapSeed) * (chainParams.numChains - 1));
		(*swapB) = (*swapA) + 1;
		}
#   endif

	i = run * chainParams.numChains;
	(*swapA) += i;
	(*swapB) += i;

	return;
}





void GetTempDownPassSeq (TreeNode *p, int *i, TreeNode **dp)

{
	
	if (p != NULL)
		{
		GetTempDownPassSeq (p->left,  i, dp);
		GetTempDownPassSeq (p->right, i, dp);
		dp[(*i)++] = p;
		}
		
}





Tree *GetTree (Param *parm, int chain, int state)

{

	return &mcmcTree[parm->treeIndex + ((2 * chain + state) * numTrees)];
	
}





Tree *GetTreeFromIndex (int index, int chain, int state)

{

	return &mcmcTree[index + ((2 * chain + state) * numTrees)];
	
}





/*------------------------------------------------------------------------
|
|	InitChainCondLikes: allocate space for chain conditional likelihoods
|		and scalers, initialize conditional likelihood pointer indices
|		for all trees, allocate space for invariable cond likes if needed,
|		allocate space for parsimony states if needed
|		allocate space for chain rateProbs if needed (adgamma model)
|		allocate space for final state probs if needed (inferAncStates)
|
-------------------------------------------------------------------------*/
int InitChainCondLikes (void)

{

	int				i, j, k, n, c, d, s, chain, nObs, chosen, oneMatSize, nNodes,
					nScalerNodes, numReps;
	long			*charBits;
	ModelInfo		*m;
	ModelParams		*mp;
	CLFlt			*clPtr;

	/* calculate space for conditional likelihoods */
	/* including scratch space for interior nodes  */
	/* based on rooted trees					   */
	oneMatSize = (numLocalTaxa - 1) * 2 * condLikeRowSize;

	/* add space for terminal conditional likelihoods for each chain
	   this is not needed for standard approach to missing data */
	if (augmentData == YES)
		oneMatSize += numLocalTaxa * condLikeRowSize;

	/* check if conditional likelihoods are needed */
	if (oneMatSize > 0)
		MrBayesPrint ("%s   Initializing conditional likelihoods for internal nodes\n", spacer);
	else
		return NO_ERROR;

	nNodes = 2 * numLocalTaxa - 1;

	if (memAllocs[ALLOC_CHAINCONDLIKES] == YES)
		{
		MrBayesPrint ("%s   Space for chain cond likes not free in InitChainCondLikes\n", spacer);
		return ERROR;
		}
#if defined SSE
	chainCondLikes = (CLFlt *) ALIGNEDMALLOC (numLocalChains * oneMatSize * sizeof(CLFlt), 16);
#else
	chainCondLikes = (CLFlt *) calloc (numLocalChains * oneMatSize, sizeof(CLFlt));
#endif
	chainCLPtrSpace = (CLFlt **) malloc (numLocalChains * nNodes * sizeof(CLFlt *));
	condLikePtr = (CLFlt ***) malloc (numLocalChains * sizeof(CLFlt **));
	if (!chainCondLikes || !chainCLPtrSpace || !condLikePtr)
		{
		if (chainCondLikes) 
			free (chainCondLikes);
		if (chainCLPtrSpace) 
			free (chainCLPtrSpace);
		if (condLikePtr) 
			free (condLikePtr);
		MrBayesPrint ("%s   The program ran out of memory while trying to allocate\n", spacer);
		MrBayesPrint ("%s   the conditional likelihoods for the chain. Please try\n", spacer);
		MrBayesPrint ("%s   allocating more memory to the program, or running the\n", spacer);
		MrBayesPrint ("%s   data on a computer with more memory.\n", spacer);
		return ERROR;
		}	
	else
		memAllocs[ALLOC_CHAINCONDLIKES] = YES;
	
	/* calculate space needed for scaler values */
	/* set num scalers and scaler start for each division */
	nScalerNodes = nNodes - numLocalTaxa;

	/* allocate space for node scalers, tree scalers and scaler pointers */
	if (memAllocs[ALLOC_CLSCALERS] == YES)
		{
		MrBayesPrint ("%s   Space for cond like scalers not free in InitChainCondLikes\n", spacer);
		return ERROR;
		}
	treeScalerSpace = (CLFlt *) calloc (2 * numLocalChains * numCompressedChars, sizeof(CLFlt));
	treeScaler = (CLFlt **) malloc (numLocalChains * sizeof(CLFlt *));
	nodeScalerSpace = (CLFlt *) calloc (2 * numLocalChains * nScalerNodes * numCompressedChars, sizeof(CLFlt));
	nodeScaler = (CLFlt **) malloc (numLocalChains * sizeof(CLFlt *));
	if (!treeScalerSpace || !treeScaler || !nodeScalerSpace || !nodeScaler)
		{
		MrBayesPrint ("%s   The program ran out of memory while trying to allocate\n", spacer);
		MrBayesPrint ("%s   the conditional likelihoods for the chain. Please try\n", spacer);
		MrBayesPrint ("%s   allocating more memory to the program, or running the\n", spacer);
		MrBayesPrint ("%s   data on a computer with more memory.\n", spacer);
		if (treeScalerSpace) 
			free (treeScalerSpace);
		if (treeScaler) 
			free (treeScaler);
		if (nodeScalerSpace) 
			free (nodeScalerSpace);
		if (nodeScaler) 
			free (nodeScaler);
		return ERROR;
		}
	else
		memAllocs[ALLOC_CLSCALERS] = YES;
	
	for (i=0; i<2 * numLocalChains * numCompressedChars; i++)
		treeScalerSpace[i] = 0.0;
	for (i=0; i<2 * numLocalChains * nScalerNodes * numCompressedChars; i++)
		nodeScalerSpace[i] = 0.0;

	/* set chain pointers to cond likes and tree and node scalers */
	for (chain=0; chain<numLocalChains; chain++)
		{
		condLikePtr[chain] = chainCLPtrSpace + chain * nNodes;
		treeScaler[chain] = treeScalerSpace + 2 * chain * numCompressedChars;
		nodeScaler[chain] = nodeScalerSpace + 2 * chain * nScalerNodes * numCompressedChars;
		}

	/* fill in conditional likelihoods for terminals if data				*/
	/* augmentation is requested											*/
	/* set cond like pointers for terminal nodes							*/
	if (augmentData == YES)
		{
		for (chain=0; chain<numLocalChains; chain++)
			{
			/* copy termCondLikes to chain */
			clPtr = chainCondLikes + chain * oneMatSize;
			for (i=0; i<condLikeRowSize * numLocalTaxa; i++)
				clPtr[i] = termCondLikes[i];

			/* set condLikePtr for terminals for chain */
			for (i=0; i<numLocalTaxa; i++)
				condLikePtr[chain][i] = clPtr + i * condLikeRowSize;

			/* pick one random state */
			for (d=0; d<numCurrentDivisions; d++)
				{
				m = &modelSettings[d];
				mp = &modelParams[d];
				
				if (mp->dataType == STANDARD && !strcmp(mp->augmentData, "Yes"))
					{
					for (i=0; i<numLocalTaxa; i++)
						{
						charBits = parsMatrix + m->parsMatrixStart + i * parsMatrixRowSize ;
						clPtr = condLikePtr[chain][i] + m->condLikeStart;
						for (c=m->compCharStart; c<m->compCharStop; c++)
							{
							numReps = m->numGammaCats * (int) pow(m->numBetaCats, m->nStates[c]);

							/* check if it is equivocal */
							for (s=nObs=0; s<m->nStates[c]; s++)
								if (IsBitSet(s, charBits))
									nObs++;

							if (nObs > 1)
								chosen = rand() % nObs;	/* pick random if equivocal */
							else
								chosen = 0;	/* pick first one if unequivocal */

							for (k=0; k<numReps; k++)
								{
								for (s=j=0; s<m->nStates[c]; s++)
									{
									if (IsBitSet(s, charBits))
										{
										if (j != chosen)
											(*clPtr) = 0.0;	/* reset if not chosen */
										j++;
										}
									clPtr++;
									}
								}
							charBits += m->nParsIntsPerSite;
							}
						}
					}
				else if (!strcmp(mp->augmentData, "Yes") && mp->dataType != CONTINUOUS)
					{
					numReps = m->numGammaCats * m->numBetaCats * m->numModelStates / mp->nStates;
					for (i=0; i<numLocalTaxa; i++)
						{
						charBits = parsMatrix + m->parsMatrixStart + i * parsMatrixRowSize;
						clPtr = condLikePtr[chain][i] + m->condLikeStart;
						for (c=0; c<m->numChars; c++)
							{
							/* check if it is equivocal */
							for (s=nObs=0; s<mp->nStates; s++)
								if (IsBitSet(s, charBits))
									nObs++;

							if (nObs > 1)
								chosen = rand() % nObs;	/* pick random if equivocal */
							else
								chosen = 0;	/* pick first one if unequivocal */

							for (k=0; k<numReps; k++)
								{
								for (s=j=0; s<mp->nStates; s++)
									{
									if (IsBitSet(s, charBits))
										{
										if (j != chosen)
											(*clPtr) = 0.0;	/* reset if not chosen */
										j++;
										}
									clPtr++;
									}
								}
							charBits += m->nParsIntsPerSite;
							}
						}
					}
				}	/* next division */
			}	/* next chain */
		}	/* end of augment data section */
	else
		{
		/* we are not augmenting data; just set cond like pointers */
		for (chain=0; chain<numLocalChains; chain++)
			{
			/* set condLikePtr for terminals for chain */
			for (i=0; i<numLocalTaxa; i++)
				condLikePtr[chain][i] = termCondLikes + i * condLikeRowSize;
			}
		}
	
	/* set up cond like pointers for interior nodes */
	for (chain=0; chain<numLocalChains; chain++)
		{
		clPtr = chainCondLikes + chain * oneMatSize;
		if (augmentData == YES)
			clPtr += numLocalTaxa * condLikeRowSize;

		/* set condLikePtr for interior nodes for chain                 */
		/* the factor 2 makes sure there is scratch space for each node */
		for (i=numLocalTaxa; i<2*numLocalTaxa-1; i++)
			{
			condLikePtr[chain][i] = clPtr;
			clPtr += 2 * condLikeRowSize;
			}
		}

	/* print chain cond likes */
#	if defined (DEBUG_INITCHAINCONDLIKES)
	for (chain=0; chain<1; chain++)
		PrintChainCondLikes(chain, 0);
#	endif

	/* Here we allocate space for transition probs. We also initialize the transition probabilities. */
	
	/* first, figure out the space for half of one chain */
	nNodes = 2 * numLocalTaxa - 1;  /* This size works for rooted or unrooted trees. We simply allocate two */
	i = 0;                          /* more matrices than we need.                                          */
	n = 0;
	for (j=0; j<numCurrentDivisions; j++)
		{
		m = &modelSettings[j];
		mp = &modelParams[j];
		m->tiProbStart = i;
		if (m->parsModelId == YES)
			continue;
		if (mp->dataType == STANDARD)
			{
			if (m->stateFreq->paramId == SYMPI_EQUAL)
				{
				for (k=0; k<9; k++)
					{
					if (m->isTiNeeded[k] == YES)
						i += (k + 2) * (k + 2) * m->numGammaCats;
					}
				for (k=9; k<13; k++)
					{
					if (m->isTiNeeded[k] == YES)
						i += (k - 6) * (k - 6) * m->numGammaCats;
					}
				for (k=13; k<18; k++)
					{
					if (m->isTiNeeded[k] == YES)
						i += (k - 11) * (k - 11) * m->numGammaCats;
					}
				}
			else
				{
				/* deal with unequal state frequencies */
				if (m->isTiNeeded[0] == YES)
					i += 4 * m->numGammaCats * m->numBetaCats;
				for (c=0; c<m->numChars; c++)
					{
					if (m->nStates[c] > 2 && (m->cType[c] == UNORD || m->cType[c] == ORD))
						{
						i += (m->nStates[c] * m->nStates[c]) * m->numGammaCats;
						}
					}
				}
			}
		else
			{
			i += m->numModelStates * m->numModelStates * m->numTiCats;
			k = (m->numModelStates + 1) * m->numModelStates * m->numTiCats;
			if (k > n)
				n = k;
			}
		}
	tiProbRowSize = i;

	/* we may not need ti probs (parsimony model) */
	if (tiProbRowSize == 0)
		return NO_ERROR;
		
	/* allocate tiprob */
	if (memAllocs[ALLOC_TIPROBS] == YES)
		{
		MrBayesPrint ("%s   Space for transition probs not free in InitChainCondLikes\n", spacer);
		return ERROR;
		}
#if defined SSE
	tiProbSpace = (CLFlt *) ALIGNEDMALLOC (2 * numLocalChains * nNodes * tiProbRowSize * sizeof(CLFlt), 16);
#else
	tiProbSpace = (CLFlt *) malloc (2 * numLocalChains * nNodes * tiProbRowSize * sizeof(CLFlt));
#endif
	tiProbs = (CLFlt **) malloc (numLocalChains * sizeof(CLFlt *));
	if (!tiProbSpace || !tiProbs)
		{
		MrBayesPrint ("%s   The program ran out of memory while trying to allocate\n", spacer);
		MrBayesPrint ("%s   the transition probabilities for the chain. Please try\n", spacer);
		MrBayesPrint ("%s   allocating more memory to the program, or running the\n", spacer);
		MrBayesPrint ("%s   data on a computer with more memory.\n", spacer);
		if (tiProbSpace) 
			free (tiProbSpace);
		if (tiProbs) 
			free (tiProbs);
		return ERROR;
		}
	else
		memAllocs[ALLOC_TIPROBS] = YES;

	/* set chain tiProbs pointers */
	for (i=j=0; i<numLocalChains; i++)
		{
		tiProbs[i] = tiProbSpace + j;
		j += 2 * nNodes * tiProbRowSize;
		}

	/* also, alloc space for precalculated likelihoods (size, n, calculated above) */
	if (n > 0) /* don't bother allocating precalculated likelihoods if we only have morphological characters */
		{
		if (memAllocs[ALLOC_PRELIKES] == YES)
			{
			MrBayesPrint ("%s   Space for preLikes not free in InitChainCondLikes\n", spacer);
			return ERROR;
			}
		preLikeL = (CLFlt *) malloc (3 * n * sizeof(CLFlt));
		if (!preLikeL)
			{
			MrBayesPrint ("%s   Problem allocating preLikes\n", spacer);
			return ERROR;
			}
		memAllocs[ALLOC_PRELIKES] = YES;
		preLikeR = preLikeL + n;
		preLikeA = preLikeR + n;
		}
	
	/* allocate space for cijk */
	
	/* figure out size of cijk */
	i = 0;
	for (j=0; j<numCurrentDivisions; j++)
		{
		m = &modelSettings[j];
		m->cijkStart = i;
		i += m->nCijk;
		}
	cijkRowSize = i;

	/* allocate cijk */
	if (memAllocs[ALLOC_CIJK] == YES)
		{
		MrBayesPrint ("%s   Space for cijk not free in InitChainCondLikes\n", spacer);
		return ERROR;
		}
	if (cijkRowSize > 0)
		{
		cijkSpace = (MrBFlt *) malloc (2 * numLocalChains * cijkRowSize * sizeof(MrBFlt));
		cijks = (MrBFlt **) malloc (numLocalChains * sizeof(MrBFlt *));
		if (!cijkSpace || !cijks)
			{
			MrBayesPrint ("%s   The program ran out of memory while trying to allocate\n", spacer);
			MrBayesPrint ("%s   the eigen values and vectors for the chain. Please try\n", spacer);
			MrBayesPrint ("%s   allocating more memory to the program, or running the\n", spacer);
			MrBayesPrint ("%s   data on a computer with more memory.\n", spacer);
			if (cijkSpace) 
				free (cijkSpace);
			if (cijks) 
				free (cijks);
			return ERROR;
			}
		else
			memAllocs[ALLOC_CIJK] = YES;
		}
	
	/* set chain cijk pointers */
	if (cijkRowSize > 0)
		{
		for (i=j=0; i<numLocalChains; i++)
			{
			cijks[i] = cijkSpace + j;
			j += 2 * cijkRowSize;
			}
		}
	
	/* allocate space for rateProbs needed by adgamma model */
	if (chainHasAdgamma == YES)
		{
		/* calculate size needed */
		i = 0;
		for (j=0; j<numCurrentDivisions; j++)
			{
			m = &modelSettings[j];
			if (m->correlation != NULL)
				{
				m->rateProbStart = i;
				i += m->numGammaCats * m->numChars;
				}
			}
		rateProbRowSize = i;

		/* allocate space */
		if (memAllocs[ALLOC_RATEPROBS] == YES)
			{
			MrBayesPrint ("%s   Space for rate probs not free in InitChainCondLikes\n", spacer);
			return ERROR;
			}
		rateProbSpace = (MrBFlt *) malloc (2 * numLocalChains * rateProbRowSize * sizeof(MrBFlt));
		rateProbs = (MrBFlt **) malloc (numLocalChains * sizeof(MrBFlt *));
		if (!rateProbSpace || !rateProbs)
			{
			MrBayesPrint ("%s   Problem allocating rate probs\n", spacer);
			if (rateProbSpace) 
				free (rateProbSpace);
			if (rateProbs) 
				free (rateProbs);
			return ERROR;
			}
		else
			memAllocs[ALLOC_RATEPROBS] = YES;

		/* set chain rateProbs pointers */
		for (i=j=0; i<numLocalChains; i++)
			{
			rateProbs[i] = rateProbSpace + j;
			j += 2 * rateProbRowSize;
			}
		}
		
	/* Allocate space for final interior node state probabilities, if needed */
	/* This often assigns more space than needed, but the space is insignificant */
	if (inferAncStates == YES)
		{
		if (memAllocs[ALLOC_ANCSTATECONDLIKES] == YES)
			{
			MrBayesPrint ("%s   Space for ancStateCondLikes not free in InitChainCondLikes\n", spacer);
			return ERROR;
			}
		ancStateCondLikes = (CLFlt *) calloc (condLikeRowSize, sizeof (CLFlt));
		if (!ancStateCondLikes)
			{
			MrBayesPrint ("%s   Problem allocating ancStateCondLikes in InitChainCondLikes\n", spacer);
			return ERROR;
			}
		else
			memAllocs[ALLOC_ANCSTATECONDLIKES] = YES;
		}

	return NO_ERROR;
	
}




/*-------------------------------------------------------------------
|
|	InitCalibratedBrlens: This routine will build a clock tree
|		consistent with calibration constraints on terminal
|		taxa and/or constrained interior nodes.
|		It is based on the assumption that BRLENSEPSILON is small
|		enough that it can be disregarded.
|
--------------------------------------------------------------------*/
int InitCalibratedBrlens (Tree *t)

{

	int				i, recalibrate;
	TreeNode		*p;
	MrBFlt			totDepth, minLength;

	minLength =  0.0001;	/* minimum branch length */

	if (t->isRooted == NO)
		{
		MrBayesPrint ("%s   Tree is unrooted\n", spacer);
		return (ERROR);
		}
	
	/* date all nodes from top to bottom with min. age as nodeDepth*/
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->anc != NULL)
			{
			if (p->left == NULL && p->right == NULL)
				{
				if (localTaxonAges[p->index] < 0.0)
					{
					p->nodeDepth = 0.0;
					p->age = -1.0;
					}
				else
					{
					p->nodeDepth = p->age = localTaxonAges[p->index];
					}
				}
			else
				{
				if (p->left->nodeDepth > p->right->nodeDepth)
					p->nodeDepth = p->left->nodeDepth;
				else
					p->nodeDepth = p->right->nodeDepth;
				if (p->age > 0.0)
					{
					if (p->age < p->nodeDepth)
						{
						MrBayesPrint ("%s   Calibration inconsistency\n", spacer);
						return (ERROR);
						}
					else
						p->nodeDepth = p->age;
					}
				}
			}
		}
	
	/* scale tree so that it has depth 1.0 */
	totDepth = t->root->left->nodeDepth;
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->anc != NULL)
			p->nodeDepth = (p->nodeDepth) / totDepth;
		else
			p->nodeDepth = 0.0;
		}
	/* calculate clock rate based on this total depth */
	t->clockRate =  (1.0 / totDepth);
			
	/* adjust node depths so that a branch is at least of length minLength */
	recalibrate = NO;
	for (i=0; i<t->nIntNodes; i++)
		{
		p = t->intDownPass[i];
		if (p->anc != NULL)
			{
			if (p->nodeDepth - p->left->nodeDepth < minLength)
				{
				p->nodeDepth = p->left->nodeDepth + minLength;
				if (p->age > 0.0)
					{
					recalibrate = YES;
					}
				}
			if (p->nodeDepth - p->right->nodeDepth < minLength)
				{
				p->nodeDepth = p->right->nodeDepth + minLength;
				if (p->age > 0.0)
					recalibrate = YES;
				}
			}
		else
			p->nodeDepth = 0.0;
		}

	if (recalibrate == YES)
		return (ERROR);

	/* calculate branch lengths */
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->anc != NULL)
			{
			if (p->anc->anc != NULL)
				{
				p->length = p->anc->nodeDepth - p->nodeDepth;
				if (p->length < BRLENS_MIN)
					p->length = BRLENS_MIN;
				}
			else
				p->length = 0.0;
			}
		}
	
	return (NO_ERROR);
	
}





int InitClockBrlens (Tree *t)

{

	int				i, maxBrSegments=0;
	TreeNode		*p;

	if (t->isRooted == NO)
		{
		MrBayesPrint ("%s   Tree is unrooted\n", spacer);
		return (ERROR);
		}
	
	MrBayesPrint ("%s   Initializing branch lengths of clock-constrained tree\n", spacer);
		
	/* calculate maximum number of branch segments above root */
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->anc != NULL)
			{
			if (p->left == NULL && p->right == NULL)
				{
				p->x = 0;
				}
			else
				{
				if (p->left->x > p->right->x)
					p->x = p->left->x + 1;
				else
					p->x = p->right->x + 1;
				}
			if (p->anc->anc == NULL)
				maxBrSegments = p->x;
			}
		}

	/* assign node depths */
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->anc != NULL)
			p->nodeDepth = (p->x) / maxBrSegments;
		else
			p->nodeDepth = 0.0;
		}
		
	/* calculate branch lengths */
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->anc != NULL)
			{
			if (p->anc->anc != NULL)
				p->length = p->anc->nodeDepth - p->nodeDepth;
			else
				p->length = 0.0;
			}
		}
	
	return (NO_ERROR);
	
}





/*------------------------------------------------------------------------
|
|	InitInvCondLikes: allocate and initialize invariable conditional
|		likelihoods if needed
|
|		NB! Fills in invariable cond likes for all hidden states; this
|		is convenient although some space is wasted
|
-------------------------------------------------------------------------*/
int InitInvCondLikes (void)

{

	int			c, d, i, s, invCondLikeSize, isConstant;
	long int	*charBits;
	MrBFlt		*cI;
	ModelInfo	*m;
	ModelParams	*mp;

	/* figure out how low large array needed to store invariable cond likes */
	invCondLikeSize = 0;
	for (d=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];

		if (m->pInvar == NULL)
			continue;
		
		invCondLikeSize += m->numChars * m->numModelStates;

		}
	
	if (invCondLikeSize == 0)
		return NO_ERROR;
	
	MrBayesPrint ("%s   Initializing invariable-site conditional likelihoods\n", spacer);
		
	/* allocate and set space for invariable-site conditional likelihoods	*/
	if (memAllocs[ALLOC_INVCONDLIKES] == YES)
		{
		MrBayesPrint ("%s   invCondLikes not free in InitInvCondLikes\n", spacer);
		return ERROR;
		}
	invCondLikes = (MrBFlt *) calloc (invCondLikeSize, sizeof(MrBFlt));
	if (invCondLikes)
		memAllocs[ALLOC_INVCONDLIKES] = YES;
	else
		{
		MrBayesPrint ("%s   Problem allocating invCondLikes (%d MrBFlt)\n", spacer, invCondLikeSize);
		return ERROR;
		}	
	
	/* initialize invariable-site conditional likelihoods to 0.0 */
	for (i=0; i<invCondLikeSize; i++)
		invCondLikes[i] = 0.0;

	/* fill in invariable-site conditional likelihoods */
	invCondLikeSize = 0;
	for (d=0; d<numCurrentDivisions; d++)
		{

		m = &modelSettings[d];
		mp = &modelParams[d];
		
		if (m->pInvar == NULL)
			continue;
		
		cI = m->invCondLikes = invCondLikes + invCondLikeSize;
		invCondLikeSize += m->numModelStates * m->numChars;
		
		if (mp->dataType == STANDARD)
			{
			for (c=0; c<m->numChars; c++)
				{
				for (s=0; s<m->nStates[c]; s++)
					{
					isConstant = YES;
					charBits = parsMatrix + m->parsMatrixStart + c * m->nParsIntsPerSite;
					for (i=0; i<numLocalTaxa; i++)
						{
						if (IsBitSet(s, charBits) == NO)
							{
							isConstant = NO;
							break;
							}
						charBits += parsMatrixRowSize;
						}
					if (isConstant == YES)
						(*(cI++)) = 1.0;
					}
				}
			}
		else	/* all other models for which pInvar is applicable */
			{
			for (c=0; c<m->numChars; c++)
				{
				for (s=0; s<m->numModelStates; s++)
					{
					isConstant = YES;
					charBits = parsMatrix + m->parsMatrixStart + c * m->nParsIntsPerSite;
					for (i=0; i<numLocalTaxa; i++)
						{
						if (IsBitSet(s, charBits) == NO)
							{
							isConstant = NO;
							break;
							}
						charBits += parsMatrixRowSize;
						}
					if (isConstant == YES)
						*cI = 1.0;
					cI++;
					}
				}
			}

		}	/* next division */

#	if 0
	invCondLikeSize = 0;
	for (d=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];
		mp = &modelParams[d];
		if (m->pInvar == NULL)
			continue;
		cI = m->invCondLikes = invCondLikes + invCondLikeSize;
		invCondLikeSize += m->numModelStates * m->numChars;
		if (mp->dataType == STANDARD)
			{
			}
		else
			{
			for (c=0; c<m->numChars; c++)
				{
				printf ("%4d -- ", c);
				for (s=0; s<m->numModelStates; s++)
					{
					printf ("%1.0lf", *cI);
					cI++;
					}
				printf ("\n");
				}
			}
		}
#	endif
		
	return NO_ERROR;
	
}





/*------------------------------------------------------------------------
|
|	InitParsSets: allocate space for chain parsimony states and set
|		parsimony state pointers indices
|
-------------------------------------------------------------------------*/
int InitParsSets (void)

{

	int				i, j, k, d, chain, nIntNodes, nNodes, nParsSets;
	long			*ptr;
	ModelInfo		*m;

	/* Calculate number of nodes and number of internal nodes for rooted tree (worst case) */
	nIntNodes = numLocalTaxa - 1;
	nNodes = 2 * numLocalTaxa - 1;

	/* Check if parsimony state sets are needed */
	nParsSets = 0;
	parsNodeLenRowSize = 0;
	for (d=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];
		if (m->parsModelId == YES || m->parsimonyBasedMove == YES)
			{
			nParsSets += m->numChars * m->nParsIntsPerSite;
			parsNodeLenRowSize += nNodes;
			}
		}
	
	/* Return if nothing to do */
	if (nParsSets == 0)
		return NO_ERROR;

	/* Print a message */
	MrBayesPrint ("%s   Allocating space for parsimony sets\n", spacer);
		
	/* Allocate space for state sets and pointers */
	if (memAllocs[ALLOC_PARSSETS] == YES)
		{
		MrBayesPrint ("%s   Space for parsimony state sets not free in InitParsSets\n", spacer);
		return ERROR;
		}
	parsSets = (long *) calloc (numLocalChains * nIntNodes * nParsSets * 2, sizeof(long));
	parsPtrSpace = (long **) malloc (numLocalChains * nNodes * sizeof(long *));
	parsPtr = (long ***) malloc (numLocalChains * sizeof(long **));
	parsNodeLengthSpace = (CLFlt *) calloc (numLocalChains * parsNodeLenRowSize * 2, sizeof (CLFlt));
	parsNodeLen = (CLFlt **) malloc (numLocalChains * sizeof(CLFlt *));
	if (!parsSets || !parsPtrSpace || !parsPtr || !parsNodeLengthSpace || !parsNodeLen)
		{
		if (parsSets) 
			free (parsSets);
		if (parsPtrSpace) 
			free (parsPtrSpace);
		if (parsPtr) 
			free (parsPtr);
		if (parsNodeLengthSpace)
			free (parsNodeLengthSpace);
		if (parsNodeLen)
			free (parsNodeLen);
		MrBayesPrint ("%s   Problem allocating parsimony state sets\n", spacer);
		return ERROR;
		}	
	else
		memAllocs[ALLOC_PARSSETS] = YES;
	
	/* Set parsimony chain pointer arrays */
	for (chain=0; chain<numLocalChains; chain++)
		{
		parsPtr[chain] = parsPtrSpace + chain * nNodes;
		}	
	
	/* Compress terminal state sets */
	for (i=j=0; i<numLocalTaxa; i++)
		{
		ptr = parsMatrix + i*parsMatrixRowSize;
		for (d=0; d<numCurrentDivisions; d++)
			{
			m = &modelSettings[d];
			if (m->parsModelId == NO && m->parsimonyBasedMove == NO)
				continue;
			for (k = m->parsMatrixStart; k<m->parsMatrixStop; k++)
				parsMatrix[j++] = ptr[k];
			}
		}
	parsMatrixRowSize = nParsSets;
	
	/* Reset parsimony matrix start and stop index */
	/* Set pointers to parsNodeLengths */
	for (d=i=j=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];
		if (m->parsModelId == NO && m->parsimonyBasedMove == NO)
			continue;
		m->parsMatrixStart = i;
		m->parsNodeLenStart = j;
		i += m->numChars * m->nParsIntsPerSite;
		j += 2 * nNodes;
		m->parsMatrixStop = i;
		}
	
	/* Set up pointers to terminal state sets */
	for (chain=0; chain<numLocalChains; chain++)
		{
		for (i=0; i<numLocalTaxa; i++)
			parsPtr[chain][i] = parsMatrix + i * parsMatrixRowSize;
		}
	
	/* Set up pointers to internal node state sets */
	/* the factor 2 is needed to make room for scratch space */
	for (chain=0; chain<numLocalChains; chain++)
		{
		ptr = parsSets + chain * parsMatrixRowSize * nIntNodes * 2;

		/* Set up pointers to interior node state sets                  */
		/* the factor 2 makes sure there is scratch space for each node */
		for (i=numLocalTaxa; i<2*numLocalTaxa-1; i++)
			parsPtr[chain][i] = ptr + 2 * (i - numLocalTaxa) * parsMatrixRowSize;
		}

	/* Set parsimony node lengths pointer for chains */
	/* The factor 2 is needed to allow room for scratch space */
	for (chain=0; chain<numLocalChains; chain++)
		{
		parsNodeLen[chain] = parsNodeLengthSpace + chain * 2 * parsNodeLenRowSize;
		}	

	return NO_ERROR;

}





/*------------------------------------------------------------------------
|
|	InitSprParsSets: allocate space for chain parsimony states and set
|		parsimony state pointers indices
|
-------------------------------------------------------------------------*/
int InitSprParsSets (void)

{

	int				c, i, j, d, chain, nIntNodes, nNodes;
	long			*ptr, x;
	ModelInfo		*m;

	/* Calculate number of nodes and number of internal nodes for rooted tree (worst case) */
	nIntNodes = numLocalTaxa - 1;
	nNodes = 2 * numLocalTaxa - 1;
	sprParsMatrixRowSize = parsMatrixRowSize;

	/* Print a message */
	MrBayesPrint ("%s   Allocating space for SPR parsimony sets\n", spacer);

	/* Allocate space for state sets and pointers */
	if (memAllocs[ALLOC_SPR_PARSSETS] == YES)
		{
		MrBayesPrint ("%s   Space for SPR parsimony state sets not free in InitSprParsSets\n", spacer);
		return ERROR;
		}
	sprParsMatrix = (long *) calloc (sprParsMatrixRowSize * numLocalTaxa, sizeof(long));
	sprParsSets = (long *) calloc (numLocalChains * nNodes * sprParsMatrixRowSize * 3, sizeof(long));
	sprParsPtrSpace = (long **) malloc (numLocalChains * nNodes * sizeof(long *));
	sprParsPtr = (long ***) malloc (numLocalChains * sizeof(long **));
	if (!sprParsMatrix || !sprParsSets || !sprParsPtrSpace || !sprParsPtr)
		{
		if (sprParsMatrix)
			free (sprParsMatrix);
		if (sprParsSets) 
			free (sprParsSets);
		if (sprParsPtrSpace) 
			free (sprParsPtrSpace);
		if (sprParsPtr) 
			free (sprParsPtr);
		MrBayesPrint ("%s   Problem allocating SPR parsimony state sets\n", spacer);
		return ERROR;
		}	
	else
		memAllocs[ALLOC_SPR_PARSSETS] = YES;

	/* copy parsMatrix into sprParsMatrix */
	for (d=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];
		for (i=0; i<numLocalTaxa; i++)
			{
			for (c=m->parsMatrixStart; c<m->parsMatrixStop; c++)
				{
				x = parsMatrix[pos(i, c, parsMatrixRowSize)];
				sprParsMatrix[pos(i, c, parsMatrixRowSize)] = x;
				}
			}
		}

	/* Set parsimony matrix start and stop index */
	for (d=i=j=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];
		m->sprParsMatrixStart = m->parsMatrixStart;
		m->sprParsMatrixStop = m->parsMatrixStop;
		}

	/* Set parsimony chain pointer arrays */
	for (chain=0; chain<numLocalChains; chain++)
		sprParsPtr[chain] = sprParsPtrSpace + chain * nNodes;
		
	/* Set up pointers to state sets */
	for (chain=0; chain<numLocalChains; chain++)
		{
		ptr = sprParsSets + chain * nNodes * 3 * sprParsMatrixRowSize;
		for (i=0; i<nNodes; i++)
			sprParsPtr[chain][i] = ptr + 3 * i * sprParsMatrixRowSize;
		}
		
	/* set the state sets for the terminals */
	for (chain=0; chain<numLocalChains; chain++)
		{
		for (d=0; d<numCurrentDivisions; d++)
			{
			m = &modelSettings[d];
			for (i=0; i<numLocalTaxa; i++)
				{
				ptr = sprParsPtr[chain][i] + UPPER * sprParsMatrixRowSize;
				for (c=m->parsMatrixStart; c<m->parsMatrixStop; c++)
					ptr[c] = sprParsMatrix[pos(i, c, parsMatrixRowSize)];
				}
			}
		}
	
#	if 0
	/* print terminal state sets */
	{
	char			tempName[100];
	for (chain=0; chain<numLocalChains; chain++)
		{
		for (d=0; d<numCurrentDivisions; d++)
			{
			MrBayesPrint ("Parsimony (bitset) matrix for chain %d, division %d\n\n", chain+1, d+1);
			m = &modelSettings[d];
			for (i=0; i<numLocalTaxa; i++)
				{
				ptr = sprParsPtr[chain][i] + UPPER * sprParsMatrixRowSize;
				GetNameFromString(taxaNames, tempName, i+1);
				MrBayesPrint ("%-10.10s   ", tempName);
				for (c=m->sprParsMatrixStart; c<m->sprParsMatrixStop; c++)
					{
					MrBayesPrint("%2ld ", ptr[c]);
					}
				MrBayesPrint("\n");
				}
			}
		}
	}
#	endif

	return NO_ERROR;

}





/*------------------------------------------------------------------------
|
|	InitTermCondLikes: calculate rowsize of condLikes and
|		allocate and initialize conditional likelihoods
|		for terminals based on parsimony (bitset) matrix
|
-------------------------------------------------------------------------*/
int InitTermCondLikes (void)

{

	int			c, d, i, j, k, s, maxRates, numReps, oneMatSize,
				corrModel[MAX_NUM_DIVS];
	long int	*charBits;
	CLFlt		*cL;
	ModelInfo	*m;
	ModelParams *mp=NULL;

	/* figure out how large cL matrix is needed */
	condLikeRowSize = 0;
	for (d=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];
		mp = &modelParams[d];

		m->condLikeStart = condLikeRowSize;

		if (m->parsModelId == YES)
			continue;

		if (mp->dataType == STANDARD)
			{
			for (c=0; c<m->numChars; c++)
				{
				numReps = m->numGammaCats;
				if (m->nStates[c] == 2)
					numReps *= m->numBetaCats;
				condLikeRowSize += m->nStates[c] * numReps;
				}
			}
		else
			condLikeRowSize += m->numChars * m->numGammaCats * m->numOmegaCats * m->numModelStates;
		
		m->condLikeStop = condLikeRowSize;
		}

	oneMatSize = numLocalTaxa * condLikeRowSize;

	/* check if conditional likelihoods are needed */
	if (oneMatSize > 0)
		MrBayesPrint ("%s   Initializing conditional likelihoods for terminals\n", spacer);
	else
		return NO_ERROR;

	/* take care of adgamma model */
	if (chainHasAdgamma == YES)
		{
		/* allocate siteJump */
		if (memAllocs[ALLOC_SITEJUMP] == YES)
			{
			MrBayesPrint ("%s   siteJump not free in InitTermCondLikes\n", spacer);
			return ERROR;
			}
		siteJump = (int *) calloc (numChar, sizeof(int));
		if (siteJump)
			memAllocs[ALLOC_SITEJUMP] = YES;
		else
			{
			MrBayesPrint ("%s   Problem allocating siteJump in InitTermCondLikes (%d ints)\n", spacer, numChar);
			return ERROR;
			}
		
		/* reset vector indicating the matrices needed */
		for (i=0; i<MAX_SMALL_JUMP; i++)
			hasMarkovTi[i] = NO;
		
		/* fill in siteJump */
		for (i=0; i<MAX_NUM_DIVS; i++)
			corrModel[i] = 0;

		k = 1;	/* index to corr model, 0 means no corr model */
		maxRates = 0;	/* max no. rates */
		for (d=0; d<numCurrentDivisions; d++)
			modelSettings[d].mark = NO;

		for (d=0; d<numCurrentDivisions; d++)
			{
			m = &modelSettings[d];
			
			if (m->correlation == NULL || m->mark == YES)
				continue;
			
			m->mark = YES;
			for (i=0; i<m->correlation->nRelParts; i++)
				{
				if (modelSettings[m->correlation->relParts[i]].shape == 
					modelSettings[d].shape)
					{
					modelSettings[m->correlation->relParts[i]].mark = YES;
					corrModel[m->correlation->relParts[i]] = k;
					}
				}
			k++;

			if (m->numGammaCats > maxRates)
				maxRates = m->numGammaCats;

			}

		for (c=0; c<numChar; c++)
			{
			if (charInfo[c].isExcluded == YES)
				continue;
			
			if ((k=corrModel[charInfo[c].partitionId[partitionNum-1] - 1]) == 0)
				continue;

			/* How far back is last char in this HMM? */
			for (j=c-1; j>=0; j--)
				{
				if (corrModel[charInfo[j].partitionId[partitionNum-1] - 1] == k)
					break;
				}

			if (j<0)
				siteJump[c] = 0;
			else if (charInfo[j].bigBreakAfter == YES)
				siteJump[c] = BIG_JUMP;
			else
				{
				siteJump[c] = c - j;
				hasMarkovTi[c-j-1] = YES;
				}
			}

		/* check if any HMM is empty */
		k=0;
		for (i=0; i<MAX_NUM_DIVS; i++)
			{
			if (corrModel[i] > k)
				k = corrModel[i];
			}
		for (i=1; i<=k; i++)
			{
			for (c=j=0; c<numChar; c++)
				{
				if (charInfo[c].isExcluded == NO && corrModel[charInfo[c].partitionId[partitionNum-1] - 1] == i)
					j = c;
				}
			if (j == 0)
				{
				MrBayesPrint ("%s   ERROR: HMM model %d is empty.\n",spacer,i);
				return (ERROR);
				}
			}

		/* allocate MarkovTis (space needed for calculations) */
		if (memAllocs[ALLOC_MARKOVTIS] == YES)
			{
			MrBayesPrint ("%s   markovTis not free in InitTermCondLikes\n", spacer);
			return ERROR;
			}

		for (i=0; i<MAX_SMALL_JUMP; i++)
			{
			if (hasMarkovTi[i] == YES || i == 0)	/* base matrix always needed */
				{
				markovTi[i] = AllocateSquareDoubleMatrix(maxRates);
				if (markovTi[i] == NULL)
					break;
				}
			else
				markovTi[i] = NULL;
			}

		markovTiN = AllocateSquareDoubleMatrix(maxRates);
		if (i >= MAX_SMALL_JUMP && markovTiN)
			memAllocs[ALLOC_MARKOVTIS] = YES;
		else
			{
			MrBayesPrint ("%s   Problem allocating MarkovTis in InitTermCondLikes (%d MrBFlt)\n", spacer, 2 * MAX_GAMMA_CATS * MAX_GAMMA_CATS);
			for (i=0; i<MAX_SMALL_JUMP; i++)
				if (markovTi[i] != NULL) 
					FreeSquareDoubleMatrix (markovTi[i]);
			if (markovTiN != NULL) 
				FreeSquareDoubleMatrix(markovTiN);
			return ERROR;
			}
	
		}	/* adgamma model taken care of */

	/* allocate and set space for terminal conditional likelihoods unless	*/
	/* we are going to augment data											*/
	for (d=0; d<numCurrentDivisions; d++)
		if (!strcmp(mp->augmentData, "Yes"))
			return NO_ERROR;

	if (memAllocs[ALLOC_TERMCONDLIKES] == YES)
		{
		MrBayesPrint ("%s   termCondLikes not free in InitTermCondLikes\n", spacer);
		return ERROR;
		}
#if defined SSE
	termCondLikes = (CLFlt *) ALIGNEDMALLOC (oneMatSize * sizeof(CLFlt), 16);
#else
	termCondLikes = (CLFlt *) calloc (oneMatSize, sizeof(CLFlt));
#endif
	if (termCondLikes)
		memAllocs[ALLOC_TERMCONDLIKES] = YES;
	else
		{
		MrBayesPrint ("%s   The program ran out of memory while trying to allocate\n", spacer);
		MrBayesPrint ("%s   the tip conditional likelihoods for the chain. Please try\n", spacer);
		MrBayesPrint ("%s   allocating more memory to the program, or running the\n", spacer);
		MrBayesPrint ("%s   data on a computer with more memory.\n", spacer);
		return ERROR;
		}	
	
	/* initialize conditional likelihoods to 0.0 */
	for (i=0; i<oneMatSize; i++)
		termCondLikes[i] = 0.0;

	/* fill in conditional likelihoods */
	for (d=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];
		mp = &modelParams[d];
		
		if (mp->dataType == STANDARD)
			{
			for (i=0; i<numLocalTaxa; i++)
				{
				charBits = parsMatrix + m->parsMatrixStart + i * parsMatrixRowSize;
				cL = termCondLikes + m->condLikeStart + i * condLikeRowSize;
				for (c=m->compCharStart; c<m->compCharStop; c++)
					{
					numReps = m->numGammaCats;
					if (m->nStates[c - m->compCharStart] == 2)
						numReps *= m->numBetaCats;
					for (k=0; k<numReps; k++)
						{
						for (s=0; s<m->nStates[c - m->compCharStart]; s++)
							{
							if (IsBitSet(s, charBits))
								(*cL) = 1.0;
							cL++;
							}
						}
					charBits++;
					}
				}
			}
		else if (mp->dataType == CONTINUOUS)
			{
			/* TODO: fill in from compressed matrix */
			
			}
		else
			{
			numReps = m->numModelStates / mp->nStates;
			numReps *= m->numTiCats;

			for (i=0; i<numLocalTaxa; i++)
				{
				charBits = parsMatrix + m->parsMatrixStart + i * parsMatrixRowSize;
				cL = termCondLikes + m->condLikeStart + i * condLikeRowSize;
				for (c=m->compCharStart; c<m->compCharStop; c++)
					{
					for (k=0; k<numReps; k++)
						{
						for (s=0; s<mp->nStates; s++)
							{
							if (IsBitSet(s, charBits))
								(*cL) = 1.0;
							cL++;
							}
						}
					charBits += m->nParsIntsPerSite;
					}
				}
			}

		}	/* next division */

	return NO_ERROR;

}






int IsBitSet (int i, long *bits)

{

	long		x;

	bits += i / nBitsInALong;

	x = 1 << (i % nBitsInALong);

	if ((*bits) & x)
		return (YES);
	else
		return (NO);
		
}





int IsClockSatisfied (Tree *t, MrBFlt tol)

{

	int				i, foundFirstLength, isClockLike;
	MrBFlt			firstLength=0.0, length;
	TreeNode		*p, *q;

	if (t->isRooted == NO)
		return (NO);
		
	foundFirstLength = NO;
	isClockLike = YES;
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->left == NULL && p->right == NULL)
			{
			length = 0.0;
			q = p;
			while (q->anc != NULL)
				{
				if (q->anc->anc != NULL)
					length += q->length;
				q = q->anc;
				}
			if (foundFirstLength == NO)
				{
				firstLength = length;
				foundFirstLength = YES;
				}
			else
				{
				if (AreDoublesEqual (firstLength, length, tol) == NO)
					isClockLike = NO;
				}
			}
		}
	if (firstLength < BRLENS_MIN)
		isClockLike = NO;

	return (isClockLike);
	
}





int IsPFNodeEmpty (PFNODE *p)
{
	int	i;

	for (i=0; i<chainParams.numRuns; i++)
		{
		if (p->count[i] > 0)
			break;
		}
	if (i == chainParams.numRuns)
		return YES;
	else
		return NO;
}





void JukesCantor (MrBFlt *tiP, MrBFlt length)

{

	int		i, j, index;
	MrBFlt	pChange, pNoChange;
	
	/* calculate probabilities */
	pChange   =  0.25 -  0.25 * exp(-( 4.0/ 3.0)*length);
	pNoChange =  0.25 +  0.75 * exp(-( 4.0/ 3.0)*length);

	/* fill in values */
	for (i=index=0; i<4; i++)
		{
		for (j=0; j<4; j++)
			{
			if (i == j)
				tiP[index++] = pNoChange;
			else
				tiP[index++] = pChange;
			}
		}
		
}





/* LargestNonemptyPFNode: recursive function to largest nonempty node in a subtree */
PFNODE *LargestNonemptyPFNode (PFNODE *p, int *i, int j)
{
	PFNODE *q;

	++j;
	if (p == NULL)
		return NULL;
	
	q = LargestNonemptyPFNode (p->left, i, j);
	
	if (q != NULL)
		{
		return q;
		}
	else if (IsPFNodeEmpty (p) == NO)
		{
		*i = j;
		return p;
		}
	else
		{
		return LargestNonemptyPFNode (p->right, i, j);
		}
}





/* LastBlock: Return file position of last block in file */
long LastBlock (FILE *fp, char *lineBuf, int longestLine)
{
	long	lastBlock;
	char	*word;
	
	lastBlock = 0L;
	rewind (fp);

	while ((fgets (lineBuf, longestLine, fp)) != NULL)
		{
		word = strtok (lineBuf, " ");
		if (strcmp (word, "begin") == 0)
			lastBlock = ftell (fp);
		}

	return lastBlock;
}




/*------------------------------------------------------------------
|
|	Likelihood_Adgamma: all n-state models with autocorrelated
|		 discrete gamma rate variation, NOT morph, restriction,
|		 codon or doublet models; just fill in rateProbs
|
-------------------------------------------------------------------*/
int Likelihood_Adgamma (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats)

{

	int				c, j, k, nStates, nStatesDiv2;
	MrBFlt			*bs, *swr, s01, s10, probOn, probOff, covBF[40];
	MrBFlt			like, *rP;
	CLFlt			*clP;
	ModelInfo		*m;
	
	/* NOTE: whichSitePats offsets numSitesOfPat by whichSitePats X numCompressedChars.
	   This is done so we can use the character reweighting scheme for "heating" chains. This was easy to
	   accomplish for all of the models except this one, which doesn't use numSitesOfPat when calculating
	   likelihoods. Either we disallow autocorrelated rates when using MCMC with character reweighting, or
	   we properly calculate likelihoods when some site patterns have increased or decreased weight. For
	   now, we do not allow MCMCMC with character reweighting with this HMM; we bail out in the function
	   FillNumSitesOfPat if we have Adgamma rate variation and reweighting. */
	k = whichSitePats;
	
	/* find model settings */
	m = &modelSettings[division];
	
	/* get the number of states */
	nStates = m->numModelStates;
	nStatesDiv2 = nStates / 2;
	
	/* find base frequencies */
	bs = GetParamSubVals (m->stateFreq, chain, state[chain]);

	/* find conditional likelihood pointer */
	clP = condLikePtr[chain][p->index] + m->condLikeStart + Bit(division, &p->clSpace[0]) * condLikeRowSize;
	
	/* find pointer to rate probabilities */
	rP = rateProbs[chain] + state[chain] * rateProbRowSize + m->rateProbStart;

	/* loop over characters and calculate rate probs */
	if (m->switchRates != NULL)
		{
		swr = GetParamVals (m->switchRates, chain, state[chain]);
		s01 = swr[0];
		s10 = swr[1];
		probOn = s01 / (s01 + s10);
		probOff =  1.0 - probOn;
		for (j=0; j<nStatesDiv2; j++)
			{
			covBF[j] = bs[j] * probOn;
			covBF[j+nStatesDiv2] = bs[j] * probOff;
			}
		bs = covBF;
		}

	for (c=0; c<m->numChars; c++)
		{
		for (k=0; k<m->numGammaCats; k++)
			{
			like =  0.0;
			for (j=0; j<nStates; j++)
				like += (*(clP++)) *  bs[j];
			*(rP++) = like;
			}
		}

	/* reset lnL, likelihood calculated later for this model */
	*lnL =  0.0;

	return (NO_ERROR);

}





/*------------------------------------------------------------------
|
|	Likelihood_Gen: general n-state models with or without rate
|		variation
|
-------------------------------------------------------------------*/

int Likelihood_Gen (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats)

{
	int				c, j, k, nStates, hasPInvar;
	MrBFlt			s01, s10, probOn, probOff, *swr;
	MrBFlt			covBF[40], freq, *bs, *clInvar=NULL, like, likeI, pInvar=0.0, lnLike;
	CLFlt			*clP, *lnScaler, *nSitesOfPat;
	ModelInfo		*m;
	
	/* find model settings and nStates, pInvar, invar cond likes */
	m = &modelSettings[division];
	nStates = m->numModelStates;
	if (m->pInvar == NULL)
		{
		hasPInvar = NO;
		}
	else
		{
		hasPInvar = YES;
		pInvar =  *(GetParamVals (m->pInvar, chain, state[chain]));
		clInvar = m->invCondLikes;
		}

	/* find conditional likelihood pointer */
	clP = condLikePtr[chain][p->index] + m->condLikeStart + Bit(division, &p->clSpace[0]) * condLikeRowSize;
	
	/* find base frequencies */
	bs = GetParamSubVals (m->stateFreq, chain, state[chain]);

	/* if covarion model, adjust base frequencies */
	if (m->switchRates != NULL)
		{
		/* find the stationary frequencies */
		swr = GetParamVals(m->switchRates, chain, state[chain]);
		s01 = swr[0];
		s10 = swr[1];
		probOn = s01 / (s01 + s10);
		probOff =  1.0 - probOn;

		/* now adjust the base frequencies; on-state stored first in cond likes */
		for (j=0; j<nStates/2; j++)
			{
			covBF[j] = bs[j] * probOn;
			covBF[j+nStates/2] = bs[j] * probOff;
			}

		/* finally set bs pointer to adjusted values */
		bs = covBF;
		}

	/* find category frequencies */
	if (hasPInvar == NO)
		freq =  1.0 /  m->numGammaCats;
	else
		freq = (1.0 - pInvar) /  m->numGammaCats;

	/* find site scaler */
	lnScaler = treeScaler[chain] + m->compCharStart + state[chain] * numCompressedChars;
	
	/* find nSitesOfPat */
	nSitesOfPat = numSitesOfPat + (whichSitePats*numCompressedChars) + m->compCharStart;
	
	/* reset lnL */
	*lnL = 0.0;

	/* loop over characters */
	if (hasPInvar == NO)
		{
		for (c=0; c<m->numChars; c++)
			{
			like = 0.0;
			for (k=0; k<m->numGammaCats; k++)
				for (j=0; j<nStates; j++)
					like += (*(clP++)) * bs[j];
			like *= freq;

			/* check against LIKE_EPSILON (values close to zero are problematic) */
			if (like < LIKE_EPSILON)
				{
				MrBayesPrint ("%s   WARNING: In LIKE_EPSILON - for division %d char %d has like = %1.30lf\n", spacer, division, c, like);
				return ERROR;
				}
			else	
				{
				(*lnL) += (lnScaler[c] +  log(like)) * nSitesOfPat[c];
				}
			}
		}
	else
		{
		/* has invariable category */
		for (c=0; c<m->numChars; c++)
			{
			likeI = like = 0.0;
			for (k=0; k<m->numGammaCats; k++)
				for (j=0; j<nStates; j++)
					{
					like += (*(clP++)) * bs[j];
					}
			like *= freq;
			for (j=0; j<nStates; j++)
				likeI += (*(clInvar++)) * bs[j] * pInvar;
			if (lnScaler[c] < -200)
				{
				/* we are not going to be able to exponentiate the scaling factor */
				if (likeI > 1E-70)
					{
					/* forget about like; it is going to be insignificant compared to likeI */
					lnLike = log(likeI);
					}
				else
					{
					/* treat likeI as if 0.0, that is, ignore it completely */
					lnLike = log(like) + lnScaler[c];
					}
				}
			else
				lnLike = log (like + (likeI / exp (lnScaler[c]))) + lnScaler[c];

			/* check against LIKE_EPSILON (values close to zero are problematic) */
			if (like < LIKE_EPSILON)
				{
				printf ("lnScaler[%d] = %lf likeI = %lf\n", c, lnScaler[c], likeI);
				MrBayesPrint ("%s   WARNING: In LIKE_EPSILON - for division %d char %d has like = %1.30lf\n", spacer, division, c, like);
				getchar();
				return ERROR;
				}
			else	
				{
				(*lnL) += lnLike * nSitesOfPat[c];
				}
			}		
		}
		
	return NO_ERROR;
	
}





/*------------------------------------------------------------------
|
|	Likelihood_NUC4: 4by4 nucleotide models with or without rate
|		variation
|
-------------------------------------------------------------------*/
int Likelihood_NUC4 (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats)

{

	int				c, i, k, hasPInvar;
	MrBFlt			freq, likeI, *bs, like, *clInvar=NULL, pInvar=0.0, lnLike;
	CLFlt			*clP, *lnScaler, *nSitesOfPat;
	ModelInfo		*m;
	
	/* find model settings and pInvar, invar cond likes */
	m = &modelSettings[division];
	if (m->pInvar == NULL)
		{
		hasPInvar = NO;
		}
	else
		{
		hasPInvar = YES;
		pInvar =  *(GetParamVals (m->pInvar, chain, state[chain]));
		clInvar = m->invCondLikes;
		}

	/* find conditional likelihood pointer */
	clP = condLikePtr[chain][p->index] + m->condLikeStart + Bit(division, &p->clSpace[0]) * condLikeRowSize;
	
	/* find base frequencies */
	bs = GetParamSubVals (m->stateFreq, chain, state[chain]);

	/* find category frequencies */
	if (hasPInvar == NO)
		freq =  1.0 /  m->numGammaCats;
	else
		freq =  (1.0 - pInvar) /  m->numGammaCats;

	/* find tree scaler */
	lnScaler = treeScaler[chain] + m->compCharStart + state[chain] * numCompressedChars;
	
	/* find nSitesOfPat */
	nSitesOfPat = numSitesOfPat + (whichSitePats*numCompressedChars) + m->compCharStart;
	
	/* reset lnL */
	*lnL = 0.0;

	/* loop over characters */
	if (hasPInvar == NO)
		{
		for (c=i=0; c<m->numChars; c++)
			{
			like = 0.0;
			for (k=0; k<m->numGammaCats; k++)
				{
				like += (clP[A] * bs[A] + clP[C] * bs[C] + clP[G] * bs[G] + clP[T] * bs[T]);
				clP += 4;
				}
			like *= freq;
			
			/* check against LIKE_EPSILON (values close to zero are problematic) */
			if (like < LIKE_EPSILON)
				{
				MrBayesPrint ("%s   WARNING: In LIKE_EPSILON - for division %d char %d has like = %1.30lf\n", spacer, division, c, like);
				return ERROR;
				}
			else	
				{
				(*lnL) += (lnScaler[c] +  log(like)) * nSitesOfPat[c];
				}
			}
		}
	else
		{
		/* has invariable category */
		for (c=i=0; c<m->numChars; c++)
			{
			like = 0.0;
			for (k=0; k<m->numGammaCats; k++)
				{
				like += (clP[A] * bs[A] + clP[C] * bs[C] + clP[G] * bs[G] + clP[T] * bs[T]);
				clP += 4;
				}
			like *= freq;
			likeI = (clInvar[A] * bs[A] + clInvar[C] * bs[C] + clInvar[G] * bs[G] + clInvar[T] * bs[T]) * pInvar;
			if (lnScaler[c] < -200)
				{
				/* we are not going to be able to exponentiate the scaling factor */
				if (likeI > 1E-70)
					{
					/* forget about like; it is going to be insignificant compared to likeI */
					lnLike = log(likeI);
					}
				else
					{
					/* treat likeI as if 0.0, that is, ignore it completely */
					lnLike = log(like) + lnScaler[c];
					}
				}
			else
				lnLike = log (like + (likeI / exp (lnScaler[c]))) + lnScaler[c];

			clInvar += 4;

			/* check against LIKE_EPSILON (values close to zero are problematic) */
			if (like < LIKE_EPSILON)
				{
				MrBayesPrint ("%s   WARNING: In LIKE_EPSILON - for division %d char %d has like = %1.30lf\n", spacer, division, c, like);
				return ERROR;
				}
			else	
				{
				(*lnL) += lnLike * nSitesOfPat[c];
				}
			}		
		}
		
	return NO_ERROR;
}





/*------------------------------------------------------------------
|
|	Likelihood_NY98: Codon model with three selection categories,
|		after Nielsen and Yang (1998).
|
-------------------------------------------------------------------*/
int Likelihood_NY98 (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats)

{

	int				c, j, k, nStates;
	MrBFlt			catLike, like, *bs, *omegaCatFreq;
	CLFlt			*clP, *lnScaler, *nSitesOfPat;
	ModelInfo		*m;
	
	m = &modelSettings[division];

	/* number of states */
	nStates = m->numModelStates;

	/* find conditional likelihood pointer */
	clP = condLikePtr[chain][p->index] + m->condLikeStart + Bit(division, &p->clSpace[0]) * condLikeRowSize;
	
	/* find codon frequencies */
	bs = GetParamSubVals (m->stateFreq, chain, state[chain]);
	
	/* find category frequencies */
	omegaCatFreq = GetParamSubVals (m->omega, chain, state[chain]);

	/* find site scaler */
	lnScaler = treeScaler[chain] + m->compCharStart + state[chain] * numCompressedChars;
	
	/* find nSitesOfPat */
	nSitesOfPat = numSitesOfPat + (whichSitePats*numCompressedChars) + m->compCharStart;
	
	*lnL = 0.0;	/* reset lnL */

	for (c=m->numDummyChars; c<m->numChars; c++)
		{
		like = 0.0;
		for (k=0; k<m->numOmegaCats; k++)
			{
			catLike = 0.0;
			for (j=0; j<nStates; j++)
				catLike += clP[j] * bs[j];
			like += catLike * omegaCatFreq[k];
			clP += nStates;
			}
		/* check against LIKE_EPSILON (values close to zero are problematic) */
		if (like < LIKE_EPSILON)
			{
			MrBayesPrint ("%s   WARNING: In LIKE_EPSILON - for division %d char %d has like = %1.30lf\n", spacer, division, c, like);
			return ERROR;
			}
		else	
			{
			(*lnL) += (lnScaler[c] +  log(like)) * nSitesOfPat[c];
			}
		}

	return NO_ERROR;
	
}





/*------------------------------------------------------------------
|
|	Likelihood_Res: restriction site model with or without rate
|		variation
|
-------------------------------------------------------------------*/
int Likelihood_Res (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats)

{

	int				c, k;
	MrBFlt			*bs, freq, like, pUnobserved, pObserved;
	CLFlt			*clP, *lnScaler, *nSitesOfPat;
	ModelInfo		*m;
	
	m = &modelSettings[division];

	/* find conditional likelihood pointer */
	clP = condLikePtr[chain][p->index] + m->condLikeStart + Bit(division, &p->clSpace[0]) * condLikeRowSize;
	
	/* find base frequencies */
#	if defined ASYMMETRY
	bs = GetParamSubVals (m->stateFreq, chain, state[chain]) + 2;
#	else
	bs = GetParamSubVals (m->stateFreq, chain, state[chain]);
#	endif

	/* find category frequencies */
	freq =  1.0 /  m->numGammaCats;

	/* find site scaler */
	lnScaler = treeScaler[chain] + m->compCharStart + state[chain] * numCompressedChars;
	
	/* find nSitesOfPat */
	nSitesOfPat = numSitesOfPat + (whichSitePats*numCompressedChars) + m->compCharStart;
	
	*lnL = 0.0;	/* reset lnL */

	pUnobserved = 0.0;
	for (c=0; c<m->numDummyChars; c++)
		{
		like = 0.0;
		for (k=0; k<m->numGammaCats; k++)
			{
			like += (clP[0]*bs[0] + clP[1]*bs[1]) * freq;
			clP += 2;
			}
		pUnobserved += like *  exp(lnScaler[c]);
		}

	pObserved =  1.0 - pUnobserved;
	if (pObserved < LIKE_EPSILON)
		{
		MrBayesPrint ("%s   WARNING: p(Observed) < LIKE_EPSILON - for division %d p(Observed) = %1.30lf\n", spacer, division, pObserved);
		return ERROR;
		}

	for (c=m->numDummyChars; c<m->numChars; c++)
		{
		like = 0.0;
		for (k=0; k<m->numGammaCats; k++)
			{
			like += (clP[0]*bs[0] + clP[1]*bs[1]) * freq;
			clP += 2;
			}
		/* check against LIKE_EPSILON (values close to zero are problematic) */
		if (like < LIKE_EPSILON)
			{
			MrBayesPrint ("%s   WARNING: In LIKE_EPSILON - for division %d char %d has like = %1.30lf\n", spacer, division, c, like);
			return ERROR;
			}
		else	
			{
			(*lnL) += (lnScaler[c] +  log(like)) * nSitesOfPat[c];
			}
		}

	/* correct for absent characters */
	(*lnL) -=  log(pObserved) * (m->numUncompressedChars);

	return NO_ERROR;
	
}






/*------------------------------------------------------------------
|
|	Likelihood_Std: variable states model with or without rate
|		variation
|
-------------------------------------------------------------------*/
int Likelihood_Std (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats)

{
	int				b, c, j, k, nBetaCats, nGammaCats, nStates;
	MrBFlt			catLike, catFreq, gammaFreq, like, *bs, *bsBase,
					pUnobserved, pObserved;
	CLFlt			*clP, *lnScaler, *nSitesOfPat;
	ModelInfo		*m;
	
	m = &modelSettings[division];

	/* find conditional likelihood pointer */
	clP = condLikePtr[chain][p->index] + m->condLikeStart + Bit(division, &p->clSpace[0]) * condLikeRowSize;
	
	/* find base frequencies */
	bsBase = GetParamSubVals (m->stateFreq, chain, state[chain]);

	/* find gamma category number and frequencies */
	nGammaCats = m->numGammaCats;
	gammaFreq = 1.0 /  nGammaCats;

	/* find site scaler */
	lnScaler = treeScaler[chain] + m->compCharStart + state[chain] * numCompressedChars;
	
	/* find nSitesOfPat */
	nSitesOfPat = numSitesOfPat + (whichSitePats*numCompressedChars) + m->compCharStart;
	
	*lnL = 0.0;	/* reset lnL */

	if (m->numBetaCats == 1)
		{
		pUnobserved = 0.0;
		catFreq = gammaFreq;
		for (c=j=0; c<m->numDummyChars; c++)
			{
			like = 0.0;
			nStates = m->nStates[c];
			bs = bsBase + m->bsIndex[c];
			for (k=0; k<nGammaCats; k++)
				{
				catLike = 0.0;
				for (j=0; j<nStates; j++)
					catLike += clP[j] * bs[j];
				like += catLike * catFreq;
				clP += nStates;
				}
			pUnobserved += like *  exp(lnScaler[c]);
			}

		pObserved =  1.0 - pUnobserved;
		if (pObserved < LIKE_EPSILON)
			pObserved =  LIKE_EPSILON;

		for (c=m->numDummyChars; c<m->numChars; c++)
			{
			like = 0.0;
			nStates = m->nStates[c];
			bs = bsBase + m->bsIndex[c];

			for (k=0; k<nGammaCats; k++)
				{
				catLike = 0.0;
				for (j=0; j<nStates; j++)
					catLike += clP[j] * bs[j];
				like += catLike * catFreq;
				clP += nStates;
				}
			/* check against LIKE_EPSILON (values close to zero are problematic) */
			if (like < LIKE_EPSILON)
				{
				MrBayesPrint ("%s   WARNING: In LIKE_EPSILON - for division %d char %d has like = %1.30lf\n", spacer, division, c, like);
				return ERROR;
				}
			else	
				{
				(*lnL) += (lnScaler[c] +  log(like)) * nSitesOfPat[c];
				}
			}
		}
	else
		{
		pUnobserved = 0.0;
		for (c=j=0; c<m->numDummyChars; c++)
			{
			like = 0.0;
			nStates = m->nStates[c];
			bs = bsBase + m->bsIndex[c];
			if (nStates == 2)
				{
				nBetaCats = m->numBetaCats;
				catFreq = gammaFreq /  nBetaCats;
				}
			else
				{
				nBetaCats = 1;
				catFreq = gammaFreq;
				}
			for (b=0; b<nBetaCats; b++)
				{
				for (k=0; k<nGammaCats; k++)
					{
					catLike = 0.0;
					for (j=0; j<nStates; j++)
						catLike += clP[j] * bs[j];
					like += catLike * catFreq;
					clP += nStates;
					}
				bs += nStates;
				}
			pUnobserved += like *  exp(lnScaler[c]);
			}

		pObserved =  1.0 - pUnobserved;
		if (pObserved < LIKE_EPSILON)
			pObserved =  LIKE_EPSILON;

		for (c=m->numDummyChars; c<m->numChars; c++)
			{
			like = 0.0;
			nStates = m->nStates[c];
			bs = bsBase + m->bsIndex[c];
			if (nStates == 2)
				{
				nBetaCats = m->numBetaCats;
				catFreq = gammaFreq /  nBetaCats;
				}
			else
				{
				nBetaCats = 1;
				catFreq = gammaFreq;
				}
			for (b=0; b<nBetaCats; b++)
				{
				for (k=0; k<nGammaCats; k++)
					{
					catLike = 0.0;
					for (j=0; j<nStates; j++)
						catLike += clP[j] * bs[j];
					like += catLike * catFreq;
					clP += nStates;
					}
				bs += nStates;
				}
			/* check against LIKE_EPSILON (values close to zero are problematic) */
			if (like < LIKE_EPSILON)
				{
				MrBayesPrint ("%s   WARNING: In LIKE_EPSILON - for division %d char %d has like = %1.30lf\n", spacer, division, c, like);
				return ERROR;
				}
			else	
				{
				(*lnL) += (lnScaler[c] +  log(like)) * nSitesOfPat[c];
				}
			}
		}

	/* correct for absent characters */
	(*lnL) -=  log(pObserved) * (m->numUncompressedChars);

	return NO_ERROR;
	
}





/*------------------------------------------------------------------
|
|	Likelihood_Pars: likelihood under the Tuffley and Steel (1997)
|		model for characters with constant number of states. The idea
|		is described in:
|
|       Tuffley, C., and M. Steel. 1997. Links between maximum likelihood
|          and maximum parsimony under a simple model of site substitution.
|          Bull. Math. Bio. 59:581-607.
|
|		The likelihood under the Tuffley and Steel (1997) model is:
|       
|	    L = k^[-(T + n)]
|	   
|	    where L is the likelihood
|	          k is the number of character states
|	          T is the parsimony tree length
|	          n is the number of characters 
|
|	The parsimony calculator does not use character packing; this is
|		to enable reweighting of characters 
|
|	Note that this is an empirical Bayes approach in that it uses the
|		maximum likelihood branch length.
|
-------------------------------------------------------------------*/
int Likelihood_Pars (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats)

{
	
	int				c, i, nStates;
	long			done, *pL, *pR, *pP, *pA, *oldpP, x;
	CLFlt			nParsChars, treeLength;
	CLFlt			length, *nSitesOfPat, *newNodeLength, oldNodeLength;
	Tree			*t;
	ModelInfo		*m;

	/* Find model settings */
	m = &modelSettings[division];

	/* Get tree */
	t = GetTree(m->brlens,chain,state[chain]);
	
	/* Get parsimony tree length */
	treeLength = (CLFlt) m->parsTreeLength[2 * chain + state[chain]];
	
	/* Get number of states */
	nStates = m->numStates;

	/* Get number of sites of pat */
	nSitesOfPat = numSitesOfPat + (whichSitePats*numCompressedChars) + m->compCharStart;

	/* Mark the nodes that can be stop nodes				 */
	/* (there must not be any touched side nodes below them) */
	p = t->root;
	p->marked = YES;
	for (i=t->nIntNodes-1; i>=0; i--)
		{
		p = t->intDownPass[i];
		p->marked = NO;
		if (p->upDateCl == YES && p->anc->marked == YES)
			{
			if (p->left->upDateCl == NO || p->right->upDateCl == NO)
				p->marked = YES;
			}
		}

	/* Now make downpass node by node */
	for (i=0; i<t->nIntNodes; i++)
		{
		p = t->intDownPass[i];

		/* continue if no work needs to be done */
		if (p->upDateCl == NO)
			continue;

		/* find parsimony sets for the node and its environment */
		pL    = parsPtr[chain][p->left->index]  + m->parsMatrixStart + Bit(division, p->left->clSpace ) * parsMatrixRowSize;
		pR    = parsPtr[chain][p->right->index] + m->parsMatrixStart + Bit(division, p->right->clSpace) * parsMatrixRowSize;
		oldpP = parsPtr[chain][p->index]        + m->parsMatrixStart + Bit(division, p->clSpace       ) * parsMatrixRowSize;

		/* find old node length */
		oldNodeLength = parsNodeLen[chain][p->index + m->parsNodeLenStart + Bit(division, p->clSpace) * parsNodeLenRowSize];
		
		/* flip one bit so we do not overwrite old state set for P */
		FlipOneBit(division, p->clSpace);

		/* find new parsimony state set and pointer to new node length for p */
		pP = parsPtr[chain][p->index] + m->parsMatrixStart + Bit(division, p->clSpace) * parsMatrixRowSize;
		newNodeLength = &parsNodeLen[chain][p->index + m->parsNodeLenStart + Bit(division, p->clSpace) * parsNodeLenRowSize];

		if (t->isRooted == NO && p->anc->anc == NULL)
			{
			pA = parsPtr[chain][p->anc->index] + m->parsMatrixStart + Bit(division, p->anc->clSpace) * parsMatrixRowSize;
			length = 0.0;
			for (c=0; c<m->numChars; c++)
				{
				x = pL[c] & pR[c];
				if (x == 0)
					{
					x = pL[c] | pR[c];
					length += nSitesOfPat[c];
					}
				if ((x & pA[c]) == 0)
					length += nSitesOfPat[c];
				pP[c] = x;
				}
			treeLength += (length - oldNodeLength);
			newNodeLength[0] = length;
			}
		else
			{
			length = 0.0;
			done = 0;
			for (c=0; c<m->numChars; c++)
				{
				x = pL[c] & pR[c];
				if (x == 0)
					{
					x = pL[c] | pR[c];
					length += nSitesOfPat[c];
					}
				pP[c] = x;
				done |= (x^oldpP[c]);
				}
			treeLength += (length - oldNodeLength);
			newNodeLength[0] = length;
			if (p->marked == YES && done == 0)
				break;
			}
		}

	/* Count number of characters in the partition. It is calculated
	   on the fly because this number is going to differ for
	   different chains if character reweighting is used. */
	nSitesOfPat = numSitesOfPat + (whichSitePats*numCompressedChars) + m->compCharStart;
	nParsChars = 0.0;
	for (c=0; c<m->numChars; c++)
		nParsChars += nSitesOfPat[c];

	/* Calculate likelihood from parsimony tree length */
	*lnL = - ((treeLength + nParsChars) *  log (nStates));

	/* Store current parsimony tree length */
	m->parsTreeLength[2 * chain + state[chain]] = treeLength;

	return (NO_ERROR);

}





int Likelihood_ParsCodon (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats)

{

	int				x, y;
	TreeNode		*q;
	
	/* no warnings */
	q = p;
	x = division;
	y = chain;
	*lnL = 0.0;
	x = whichSitePats;

	MrBayesPrint ("%s   Parsimony calculator for codons not yet implemented\n", spacer);
	
	return ERROR;

}





/*------------------------------------------------------------------
|
|	Likelihood_Pars: likelihood under the Tuffley and Steel (1997)
|		model for characters with constant number of states. The idea
|		is described in:
|
|       Tuffley, C., and M. Steel. 1997. Links between maximum likelihood
|          and maximum parsimony under a simple model of site substitution.
|          Bull. Math. Bio. 59:581-607.
|
|		The likelihood under the Tuffley and Steel (1997) model is:
|       
|	    L = k^[-(T + n)]
|	   
|	    where L is the likelihood
|	          k is the number of character states
|	          T is the parsimony tree length
|	          n is the number of characters 
|
|	The parsimony calculator does not use character packing; this is
|		to enable reweighting of characters 
|
|	Note that this is an empirical Bayes approach in that it uses the
|		maximum likelihood branch length.
|
|	This variant of the calculator assumes that the number of states
|       is variable. It does not take state order into account.
|
-------------------------------------------------------------------*/
int Likelihood_ParsStd (TreeNode *p, int division, int chain, MrBFlt *lnL, int whichSitePats)

{
	
	int				c, i, *nStates;
	long			*pL, *pR, *pP, *pA, *oldpP, x;
	CLFlt			*treeLength;
	CLFlt			*nSitesOfPat;
	Tree			*t;
	ModelInfo		*m;

	/* Find model settings */
	m = &modelSettings[division];

	/* Get tree */
	t = GetTree(m->brlens,chain,state[chain]);
	
	/* Allocate space for parsimony tree length */
	treeLength = (CLFlt *) calloc (m->numChars, sizeof (CLFlt));
	
	/* Get number of states */
	nStates = m->nStates;

	/* Get number of sites of pat */
	nSitesOfPat = numSitesOfPat + (whichSitePats*numCompressedChars) + m->compCharStart;

	/* Make downpass node by node; do not skip any nodes */
	for (i=0; i<t->nIntNodes; i++)
		{
		p = t->intDownPass[i];

		/* find parsimony sets for the node and its environment */
		pL    = parsPtr[chain][p->left->index]  + m->parsMatrixStart + Bit(division, p->left->clSpace ) * parsMatrixRowSize;
		pR    = parsPtr[chain][p->right->index] + m->parsMatrixStart + Bit(division, p->right->clSpace) * parsMatrixRowSize;
		oldpP = parsPtr[chain][p->index]        + m->parsMatrixStart + Bit(division, p->clSpace       ) * parsMatrixRowSize;

		/* flip one bit so we do not overwrite old state set for p */
		FlipOneBit(division, p->clSpace);

		/* find new parsimony state set for p */
		pP = parsPtr[chain][p->index] + m->parsMatrixStart + Bit(division, p->clSpace) * parsMatrixRowSize;

		if (t->isRooted == NO && p->anc->anc == NULL)
			{
			pA = parsPtr[chain][p->anc->index] + m->parsMatrixStart + Bit(division, p->anc->clSpace) * parsMatrixRowSize;
			for (c=0; c<m->numChars; c++)
				{
				x = pL[c] & pR[c];
				if (x == 0)
					{
					x = pL[c] | pR[c];
					treeLength[c] += nSitesOfPat[c];
					}
				if ((x & pA[c]) == 0)
					treeLength[c] += nSitesOfPat[c];
				pP[c] = x;
				}
			}
		else
			{
			for (c=0; c<m->numChars; c++)
				{
				x = pL[c] & pR[c];
				if (x == 0)
					{
					x = pL[c] | pR[c];
					treeLength[c] += nSitesOfPat[c];
					}
				pP[c] = x;
				}
			}
		}

	/* Calculate the likelihood one character at a time */
	*lnL = 0.0;
	for (c=0; c<m->numChars; c++)
		{
		*lnL -= ((treeLength[c] + nSitesOfPat[c]) * log (nStates[c]));
		}

	/* Free space for parsimony character states */
	free (treeLength);

	return (NO_ERROR);

}





/*-----------------------------------------------------------------
|
|	LogLike: calculate the log likelihood of the new state of the
|		chain
|
-----------------------------------------------------------------*/
MrBFlt LogLike (int chain)

{

	int				i, d;
	TreeNode		*p;
	ModelInfo		*m;
	Tree			*tree;
	MrBFlt			chainLnLike, lnL;

	/* initialize chain cond like */
	chainLnLike = 0.0;
	
	if (chainParams.runWithData == NO)
		return (chainLnLike);

#	if defined (DEBUG_RUN_WITHOUT_DATA)
	return (chainLnLike);
#	endif
	
	/* Cycle through divisions and recalculate tis and cond likes as necessary. */
	/* Code below does not try to avoid recalculating ti probs for divisions    */
	/* that could share ti probs with other divisions.                          */
	for (d=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];
		
		if (m->upDateCl == YES)
			{
			tree = GetTree(m->brlens, chain, state[chain]);

			if (m->parsModelId == NO)
				{
				for (i=0; i<tree->nIntNodes; i++)
					{
					p = tree->intDownPass[i];
					
					if (p->left->upDateTi == YES)
						{
						/* shift state of ti probs for node */
						FlipOneBit (d, &p->left->tiSpace[0]);
						m->TiProbs (p->left, d, chain);
						}

					if (p->right->upDateTi == YES)
						{
						/* shift state of ti probs for node */
						FlipOneBit (d, &p->right->tiSpace[0]);
						m->TiProbs (p->right, d, chain);
						}

					if (tree->isRooted == NO)
						{
						if (p->anc->anc == NULL && p->upDateTi == YES)
							{
							/* shift state of ti probs for node */
							FlipOneBit (d, &p->tiSpace[0]);
							m->TiProbs (p, d, chain);
							}
						}

					if (p->upDateCl == YES)
						{
						if (tree->isRooted == NO)
							{
							if (p->anc->anc == NULL)
								m->CondLikeRoot (p, d, chain);
							else
								m->CondLikeDown (p, d, chain);
							}
						else
							m->CondLikeDown (p, d, chain);

#						if !defined (DEBUG_NOSCALING)
						if (p->scalerNode == YES || Bit(d,&p->scalersSet[0]) == 1)
							m->CondLikeScaler (p, d, chain);
#						endif
						}
					}
				}
				
			lnL = 0.0;
			m->Likelihood (tree->root->left, d, chain, &lnL, (chainId[chain] % chainParams.numChains));
			m->lnLike[2*chain + state[chain]] =  lnL;
			}
		chainLnLike += m->lnLike[2*chain + state[chain]];
		}

	/* unmark all divisions */
	if (chainHasAdgamma == YES)
		{
		for (d=0; d<numCurrentDivisions; d++)
			{
			m = &modelSettings[d];
			m->mark = NO;
			}
		
		/* update HMM likelihoods if appropriate */
		for (d=0; d<numCurrentDivisions; d++)
			{
			m = &modelSettings[d];
			
			if (m->upDateCl == YES && m->correlation != NULL && m->mark != YES)
				{
				lnL = 0.0;
				CalcLike_Adgamma(d, m->correlation, chain, &lnL);

				/* store the value for the cases where the HMM is not touched */
				m->lnLike[2*chain + state[chain]] =  lnL;
				
				/* add it to chainLnLike - it was not added above since the division */
				/* lnL was set to zero after the update call to Likelihood_Adgamma */
				chainLnLike += lnL;
				
				/* set mark for other divisions in the HMM
				   (i.e., those with the same correlation parameter
				   AND the same gamma shape parameter) */
				for (i=0; i<m->correlation->nRelParts; i++)
					{
					if (modelSettings[m->correlation->relParts[i]].shape ==
						modelSettings[d].shape)
						{
						modelSettings[m->correlation->relParts[i]].mark = YES;
						}
					}
				}
			}
		}

	return (chainLnLike);
	
}





MrBFlt LogOmegaPrior (MrBFlt w1, MrBFlt w2, MrBFlt w3)

{

	/* This function returns the log prior probability of 
	   the ratio on three omegas. Here, we have three
	   nonsynonymous/synonymous rate ratios, denoted w1, w2,
	   and w3. They have the property that w1 < w2 < w3. 
	   Remember that w1 = dN1/dS, w2 = dN2/dS, and
	   w3 = dN3/dS. We assume that dN1, dN2, dN3, and dS
	   are all independent draws from the same exponential
	   distribution, and that dN1, dN2, and dN3 are the
	   order statistics. The w1, w2, and w3, then, are
	   all scaled to the same dS r.v. */
	   
	MrBFlt		lnProb;
	
	lnProb =  (log(36.0) - 4.0 * log(1.0 + w1 + w2 + w3));
	 
	 return (lnProb);
	 
}




 
MrBFlt LogPrior (int chain)

{

	int				i, n, nStates;
	MrBFlt			*st, *sst, lnPrior, sum, x, theta, growth, *alphaDir, newProp[6], sR, eR, sF;
	Param			*p;
	ModelParams 	*mp;
	ModelInfo		*m;
	Tree			*t;
	TreeNode		*branch;
	
	lnPrior	= 0.0;
	for (n=0; n<numParams; n++)
		{
		p = &params[n];
		st  = GetParamVals (p, chain, state[chain]);
		sst = GetParamSubVals (p, chain, state[chain]);

		if (p->paramType == P_TRATIO)
			{
			/* tratio parameter */
			mp = &modelParams[p->relParts[0]];
			if (p->paramId == TRATIO_DIR)
				{
				alphaDir = mp->tRatioDir;
				newProp[0] =  (st[0] / (st[0] + 1.0));
				newProp[1] =  (1.0 - newProp[0]);
				x = 0.0;
				for (i=0; i<2; i++)
					x += (alphaDir[i]-1.0)*log(newProp[i]);
				lnPrior = x;
				}
			else
				{
				
				}
			}
		else if (p->paramType == P_REVMAT)
			{
			/* revmat parameter */
			mp = &modelParams[p->relParts[0]];
			if (p->paramId == REVMAT_DIR)
				{
				alphaDir = mp->revMatDir;
				sum = 0.0;
				for (i=0; i<6; i++)
					sum += st[i];
				for (i=0; i<6; i++)
					newProp[i] = st[i] / sum;
				x = 0.0;
				for (i=0; i<6; i++)
					x += (alphaDir[5-i] - 1.0) * log(newProp[i]);
				lnPrior = x;
				}
			else
				{

				}
			}
		else if (p->paramType == P_OMEGA)
			{
			/* omega parameter */
			mp = &modelParams[p->relParts[0]];
			if (p->paramId == OMEGA_DIR)
				{
				alphaDir = mp->omegaDir;
				newProp[0] = st[0] / (st[0] + 1.0);
				newProp[1] = 1.0 - newProp[0];
				x = 0.0;
				for (i=0; i<2; i++)
					x += (alphaDir[i]-1.0)*log(newProp[i]);
				lnPrior = x;
				}
			else
				{
				
				}
			}
		else if (p->paramType == P_PI)
			{
			/* state frequencies parameter */
			if (p->paramId == PI_DIR)
				{
				mp = &modelParams[p->relParts[0]];
				nStates = p->nSubValues;
				sum = 0.0;
				for (i=0; i<nStates; i++)
					sum += st[i];
				x = LnGamma(sum);
				for (i=0; i<nStates; i++)
					x -= LnGamma(st[i]);
				for (i=0; i<nStates; i++)
					x += (st[i] - 1.0)*log(sst[i]);
				lnPrior += x;
				}
			}
		else if (p->paramType == P_SHAPE)
			{
			/* gamma shape parameter */
			mp = &modelParams[p->relParts[0]];
			if (p->paramId == SHAPE_UNI)
				{
				lnPrior += log(1.0) - log(mp->shapeUni[1] - mp->shapeUni[0]);
				}
			else if (p->paramId == SHAPE_EXP)
				{
				lnPrior += log(mp->shapeExp) - mp->shapeExp * st[0];
				}
			}
		else if (p->paramType == P_PINVAR)
			{
			/* proportion of invariable sites parameter */
			mp = &modelParams[p->relParts[0]];
			lnPrior += log(1.0) - log(mp->pInvarUni[1] - mp->pInvarUni[0]);
			}
		else if (p->paramType == P_CORREL)
			{
			/* adGamma model parameter */
			mp = &modelParams[p->relParts[0]];
			lnPrior += log(1.0) - log(mp->corrUni[1] - mp->corrUni[0]);
			}
		else if (p->paramType == P_SWITCH)
			{
			/* switching rate parameter of covarion model */
			mp = &modelParams[p->relParts[0]];
			if (p->paramId == SWITCH_UNI)
				{
				lnPrior += log(1.0) - log(mp->covswitchUni[1] - mp->covswitchUni[0]);
				}
			else if (p->paramId == SWITCH_EXP)
				{
				lnPrior += log(mp->covswitchExp) - mp->covswitchExp * st[0];
				}
			}
		else if (p->paramType == P_RATEMULT)
			{
			
			
			

			}
		else if (p->paramType == P_TOPOLOGY)
			{




			}
		else if (p->paramType == P_BRLENS)
			{
			/* branch lengths */
			mp = &modelParams[p->relParts[0]];
			t = GetTree (p, chain, state[chain]);
			if (t->isRooted == YES)
				{
				if (p->paramId == BRLENS_CLOCK_UNI || p->paramId == BRLENS_CCLOCK_UNI)
					{
					/* uniformly distributed branch lengths */
					if (!strcmp(mp->treeHeightPr, "Exponential"))
						lnPrior += log(mp->treeHeightExp) - mp->treeHeightExp * t->root->nodeDepth;
					else
						lnPrior += mp->treeHeightGamma[1] * log(mp->treeHeightGamma[0]) - LnGamma(mp->treeHeightGamma[1]) + (mp->treeHeightGamma[1] - 1.0) * log(t->root->nodeDepth) - mp->treeHeightGamma[0] * t->root->nodeDepth;
					}
				else if (p->paramId == BRLENS_CLOCK_COAL || p->paramId == BRLENS_CCLOCK_COAL)
					{
					/* coalescence prior */
					m = &modelSettings[p->relParts[0]];
					theta = *(GetParamVals (m->theta, chain, state[chain]));
					if (!strcmp(mp->growthPr, "Fixed"))
						growth = mp->growthFix;
					else
						growth = *(GetParamVals (m->growthRate, chain, state[chain]));
					if (LnCoalescencePriorPr (t, &x, theta, growth) == ERROR)
						{
						MrBayesPrint ("%s   Problem calculating prior for coalescence process\n", spacer);
						}
					lnPrior += x;
					}
				else
					{
					/* birth-death prior */
					m = &modelSettings[p->relParts[0]];
					sR = *(GetParamVals (m->speciationRates, chain, state[chain]));
					eR = *(GetParamVals (m->extinctionRates, chain, state[chain]));
					sF = mp->sampleProb;
					if (LnBirthDeathPriorPr (t, &x, sR, eR, sF) == ERROR)
						{
						MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
						}
					lnPrior += x;
					if (!strcmp(mp->treeHeightPr, "Exponential"))
						lnPrior += log(mp->treeHeightExp) - mp->treeHeightExp * t->root->nodeDepth;
					else
						lnPrior += mp->treeHeightGamma[1] * log(mp->treeHeightGamma[0]) - LnGamma(mp->treeHeightGamma[1]) + (mp->treeHeightGamma[1] - 1.0) * log(t->root->nodeDepth) - mp->treeHeightGamma[0] * t->root->nodeDepth;
					}
				}
			else
				{
				if (p->paramId == BRLENS_UNI)
					{
					for (i=0; i<t->nNodes; i++)
						{
						branch = t->allDownPass[i];
						if (branch->anc != NULL)
							lnPrior += log(1.0) - log(mp->brlensUni[1] - BRLENS_MIN);
						}
					}
				else if (p->paramId == BRLENS_EXP)
					{
					for (i=0; i<t->nNodes; i++)
						{
						branch = t->allDownPass[i];
						if (branch->anc != NULL)
							lnPrior += log(mp->brlensExp) - mp->brlensExp * branch->length;
						}
					}
				}
			}
		else if (p->paramType == P_SPECRATE)
			{
			/* speciation rate parameter */
			mp = &modelParams[p->relParts[0]];
			if (p->paramId == SPECRATE_UNI)
				{
				lnPrior += log(1.0) - log(mp->speciationUni[1] - mp->speciationUni[0]);
				}
			else if (p->paramId == SPECRATE_EXP)
				{
				lnPrior += log(mp->speciationExp) - mp->speciationExp * st[0];
				}
			}
		else if (p->paramType == P_EXTRATE)
			{
			/* extinction rate parameter */
			mp = &modelParams[p->relParts[0]];
			if (p->paramId == EXTRATE_UNI)
				{
				lnPrior += log(1.0) - log(mp->extinctionUni[1] - mp->extinctionUni[0]);
				}
			else if (p->paramId == EXTRATE_EXP)
				{
				lnPrior += log(mp->extinctionExp) - mp->extinctionExp * st[0];
				}
			}
		else if (p->paramType == P_THETA)
			{
			/* neutral coalescence parameter */
			mp = &modelParams[p->relParts[0]];
			if (p->paramId == THETA_UNI)
				{
				lnPrior += log(1.0) - log(mp->thetaUni[1] - mp->thetaUni[0]);
				}
			else if (p->paramId == THETA_EXP)
				{
				lnPrior += log(mp->thetaExp) - mp->thetaExp * st[0];
				}
			}
		else if (p->paramType == P_AAMODEL)
			{
			lnPrior += sst[(int)st[0]];
			}
		else if (p->paramType == P_BRCORR)
			{
			
			
			

			}
		else if (p->paramType == P_BRSIGMA)
			{
			
			
			

			}
		else if (p->paramType == P_GROWTH)
			{
			/* population growth parameter */
			mp = &modelParams[p->relParts[0]];
			if (p->paramId == GROWTH_UNI)
				{
				lnPrior += log(1.0) - log(mp->growthUni[1] - mp->growthUni[0]);
				}
			else if (p->paramId == GROWTH_EXP)
				{
				lnPrior += log(mp->growthExp) - mp->growthExp * st[0];
				}
			}

		}

	return (lnPrior);

}





int LnBirthDeathPriorPr (Tree *t, MrBFlt *prob, MrBFlt sR, MrBFlt eR, MrBFlt sF)

{

	int				i, j, nNodes;
	MrBFlt			rootTime=0.0, *nt;
	TreeNode		*p;

	/* allocate space for the scaled speciation times */
	nt = (MrBFlt *)malloc((size_t) (t->nIntNodes) * sizeof(MrBFlt));
	if (!nt)
		{
		printf ("\n   ERROR: Problem allocating nt\n");
		return (ERROR);
		}

	/* get the node times and put them into a vector */
	for (i=j=0; i<t->nIntNodes; i++)
		{
		p = t->intDownPass[i];
		if (p->anc->anc != NULL)
			nt[j++] = p->nodeDepth;
		else
			rootTime = p->nodeDepth;
		}
	nNodes = j;

	/* rescale all of the node times on the tree */
	for (i=0; i<nNodes; i++)
		nt[i] /= rootTime;
		
	/* I think this is correct. It looks as if Yang and Rannala (1997)
	   have the root time constrained to be 1.0. */
	rootTime = 1.0;
							
	/* calculate probabilities of tree */
	if (/*sR != eR*/ fabs(sR-eR)>ETA) /* != is _not_ a very good operator for floating points */
		{
		(*prob) = (numLocalTaxa - 2.0) * log(sR);
		for (i=0; i<nNodes; i++)
			(*prob) += LnP1 (nt[i], sR, eR, sF) - LnVt (rootTime, sR, eR, sF);
		}
	else
		{
		(*prob) = 0.0;
		for (i=0; i<nNodes; i++)
			(*prob) += log (1.0 + sF * eR) - (2.0 * log(1.0 + sF * eR * nt[i]));
		}
		
	/* free memory */
	free (nt);
	
	return (NO_ERROR);
		
}





/*---------------------------------------------------------------------------------
|
|   LnCoalescencePriorPr
|
|   This function calculates the probability of a tree under the neutral
|   coalescence prior with a (potentially) exponentially growing population.
|   We assume a rooted tree that satisfies the molecular clock constraint. The
|   Tree is labelled as follows:
|
|                                      t_4 ___  
|     \         \         \        /            \
|      \         \         \      /             | 
|   I_4 \         \         \    /              | g_4
|        \         \         \  /               |
|         \         \         \/       t_3 ___  /
|          \         \        /                 \
|           \         \      /                  |
|   I_3      \         \    /                   | g_3
|             \         \  /                    |
|              \         \/            t_2 ___  / 
|               \        /                      \
|                \      /                       |
|   I_2           \    /                        | g_2
|                  \  /                         |
|                   \/                 t_1 ___  /
|    
|   Each interval on the tree is specified by successive coalescence events.
|   These intervals are denoted I_2, I_3, I_4, ..., with the subscript denoting
|   how many lineages exist in that interval. The time of each coalescence event
|   is designated t_1, t_2, t_3, ..., where the subscript denotes the number
|   of lineages that exist after the coalescence (t_3, for instance, would be
|   the time of the coalescence that went from four lineages to three lineages).
|   The duration of the i-th interval is designated g_i.
|
|   The probability of the coalescence tree is:
|   
|   prob = (k_C_2 / (N(t_k + t_k))) * exp(-integral_(from x=t_k, to g_k + t_k)  (k_C_2 / N(x)) dx)
|
|   where N(x) = N(0) * exp(-r*x). For the constant population size case,
|   N(0) = N_e. r is the population growth parameter for the exponentially
|   growing population. Here, theta = N(0) * mu when organisms are haploid and
|   theta = 2 * N(0) * mu when the organism is diploid.
|
|   Below, ct holds the n - 1 coalescence times (t_i, above) sorted from the
|   smallest to the largest. Remember that t_4 < t_3 < t_2, etc. 
|
---------------------------------------------------------------------------------*/
int LnCoalescencePriorPr (Tree *t, MrBFlt *prob, MrBFlt theta, MrBFlt growth)

{

	int				i, j, k, nNodes;
	MrBFlt			*ct, tempD, lastCoalescenceTime, coalescenceTime, intervalLength;
	TreeNode		*p;

	/* allocate space for the coalescence times */
	ct = (MrBFlt *)malloc((size_t) (t->nIntNodes) * sizeof(MrBFlt));
	if (!ct)
		{
		printf ("\n   ERROR: Problem allocating ct\n");
		return (ERROR);
		}

	/* get the coalescence times and put them into a vector */
	for (i=j=0; i<t->nIntNodes; i++)
		{
		p = t->intDownPass[i];
		if (p->anc != NULL)
			ct[j++] = p->nodeDepth;
		}
	nNodes = j;

	/* sort the coalescence times */
	Sort (ct, nNodes);
	
	/*for (i=0, k=numLocalTaxa; i<nNodes; i++)
		{
		printf ("%4d -- %2d %lf\n", i, k, ct[i]);
		k--;
		}*/
		
	/* calculate probability of the tree */
	if (AreDoublesEqual (growth, 0.0, 0.000001) == YES)
		{
		/* use this if there is no population growth */
		tempD = lastCoalescenceTime = 0.0;
		for (i=0, k=numLocalTaxa; i<nNodes; i++)
			{
			coalescenceTime = ct[i];
			intervalLength = coalescenceTime - lastCoalescenceTime;
			lastCoalescenceTime = ct[i];
			tempD += - (k * (k-1) * intervalLength) / (theta);
			k--;
			}
		(*prob) = (numLocalTaxa - 1) * log(1.0 / theta) + tempD;
		}
	else
		{
		/* use this if the population is growing exponentially */
		tempD = lastCoalescenceTime = 0.0;
		for (i=0, k=numLocalTaxa; i<nNodes; i++)
			{
			coalescenceTime = ct[i];
			intervalLength = coalescenceTime - lastCoalescenceTime;
			tempD += growth * coalescenceTime + (((k * (k-1)) / (theta * growth)) * (exp(growth * lastCoalescenceTime) - exp(growth * coalescenceTime)));
			lastCoalescenceTime = ct[i];
			k--;
			}
		(*prob) = (numLocalTaxa - 1) * log(1.0 / theta) + tempD;
		}

	/*printf ("coal pr = %lf theta = %lf, nNodes = %d, nt = %d tempD = %lf\n", *prob, theta, nNodes, numLocalTaxa, tempD);*/

	/* free memory */
	free (ct);
	
	return (NO_ERROR);
		
}





MrBFlt LnP1 (MrBFlt t, MrBFlt l, MrBFlt m, MrBFlt r)

{

	MrBFlt		p0t;
	
	p0t = r*(l-m) / (r*l + (l*(1.0-r)-m)*exp((m-l)*t) );
	
	return (log(1.0/r) + 2.0*log(p0t) + (m-l)*t);

}





MrBFlt LnVt (MrBFlt t, MrBFlt l, MrBFlt m, MrBFlt r)

{

	MrBFlt		p0t;
	
	p0t = r*(l-m) / (r*l + (l*(1.0-r)-m)*exp((m-l)*t) );
	
	return (log(1.0 - (1.0/r) * p0t * exp((m-l)*t)));

}





/*----------------------------------------------------------------
|
|	MarkClsBelow: We mark all of the nodes below p as in need of
|      updating for the conditional likelihoods. Note that we do
|      do not mark p itself or the very root node of the tree.
|
----------------------------------------------------------------*/
void MarkClsBelow (TreeNode *p)

{

	TreeNode *q;

	q = p;
	while (q->anc != NULL)
		{
		if (q != p) 
 		  q->upDateCl = YES; 
		q = q->anc;
		}

}





MrBFlt MaximumValue (MrBFlt x, MrBFlt y)

{

	if (x > y)
		return (x);
	else
		return (y);
		
}





MrBFlt MinimumValue (MrBFlt x, MrBFlt y)

{

	if (x < y)
		return (x);
	else
		return (y);
		
}





int Move_Aamodel (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* Change amino acid model for model mixing 
	   amino acid model ID's
		AAMODEL_POISSON			0
		AAMODEL_JONES			1
		AAMODEL_DAY				2
		AAMODEL_MTREV			3
		AAMODEL_MTMAM			4
		AAMODEL_WAG				5
		AAMODEL_RTREV			6 
		AAMODEL_CPREV           7 
		AAMODEL_VT				8
		AAMODEL_BLOSUM			9 */

	int			i, oldM, newM;
	MrBFlt		*bs, x, *subValue;
	ModelParams *mp;
	
	/* no warnings */
	x = mvp[0];
	
	/* get model params */
	mp = &modelParams[param->relParts[0]];

	subValue = GetParamSubVals(param, chain, state[chain]);

	/* get old value of model */
	newM = oldM = (int)*GetParamVals(param, chain, state[chain]);
	
	/* get a new model ID */
	do
		{
		newM = (int)(RandomNumber(seed) * 10);
		} while (newM == oldM);

	/* set proposal ratio */
	*lnProposalRatio = 0.0;
	
	/* set prior ratio */
	*lnPriorRatio = subValue[newM] - subValue[oldM];
	
	/* copy new amino acid model ID back */
	*GetParamVals(param, chain, state[chain]) = (MrBFlt)newM;
	
	/* set amino acid frequencies */
	bs = GetParamSubVals (modelSettings[param->relParts[0]].stateFreq, chain, state[chain]);
	if (newM == AAMODEL_POISSON)
		{
		for (i=0; i<mp->nStates; i++)
			bs[i] = 1.0 / 20.0;
		}
	else if (newM == AAMODEL_JONES)
		{
		for (i=0; i<mp->nStates; i++)
			bs[i] = jonesPi[i];
		}
	else if (newM == AAMODEL_DAY)
		{
		for (i=0; i<mp->nStates; i++)
			bs[i] = dayhoffPi[i];
		}
	else if (newM == AAMODEL_MTREV)
		{
		for (i=0; i<mp->nStates; i++)
			bs[i] = mtrev24Pi[i];
		}
	else if (newM == AAMODEL_MTMAM)
		{
		for (i=0; i<mp->nStates; i++)
			bs[i] = mtmamPi[i];
		}
	else if (newM == AAMODEL_WAG)
		{
		for (i=0; i<mp->nStates; i++)
			bs[i] = wagPi[i];
		}
	else if (newM == AAMODEL_RTREV)
		{
		for (i=0; i<mp->nStates; i++)
			bs[i] = rtrevPi[i];
		}
	else if (newM == AAMODEL_CPREV)
		{
		for (i=0; i<mp->nStates; i++)
			bs[i] = cprevPi[i];
		}
	else if (newM == AAMODEL_VT)
		{
		for (i=0; i<mp->nStates; i++)
			bs[i] = vtPi[i];
		}
	else if (newM == AAMODEL_BLOSUM)
		{
		for (i=0; i<mp->nStates; i++)
			bs[i] = blosPi[i];
		}

	/* Set update flags for all partitions that share this amino acid model. Note that the conditional
	   likelihood update flags have been set before we even call this function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));

	/* Set update flags for cijks for all affected partitions. */
	for (i=0; i<param->nRelParts; i++)
		modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);
	
}





int Move_Adgamma (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* Change correlation parameter (-1, 1) of adgamma model */

	int			i, isValidP;
	MrBFlt		oldP, newP, window, minP, maxP, ran, *markovTiValues;
	ModelParams *mp;

	/* get size of window, centered on current rho */
	window = mvp[0];

	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* get minimum and maximum values for rho */
	minP = mp->corrUni[0];
	maxP = mp->corrUni[1];

	/* get address of markovTi */
	markovTiValues = GetParamSubVals (param, chain, state[chain]);

	/* get old value of rho */
	newP = oldP = *GetParamVals(param, chain, state[chain]);

	/* change value for rho */
	ran = RandomNumber(seed);
	newP = oldP + window * (ran - 0.5);
	
	/* check that new value is valid */
	isValidP = NO;
	do
		{
		if (newP < minP)
			newP = 2* minP - newP;
		else if (newP > maxP)
			newP = 2 * maxP - newP;
		else
			isValidP = YES;
		} while (isValidP == NO);

	/* get proposal ratio */
	*lnProposalRatio = 0.0;
	
	/* get prior ratio */
	*lnPriorRatio = 0.0;
	
	/* copy new rho value back */
	*GetParamVals(param, chain, state[chain]) = newP;

	/* fill in new Markov trans probs */
	AutodGamma (markovTiValues, newP, mp->numGammaCats);
		
	/* Set update flags for all partitions that share this rho. Note that the conditional
	   likelihood update flags have been set before we even call this function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));

	/* Update flags for divisions already set */

	return (NO_ERROR);
}





int Move_Beta (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* change symmetric Dirichlet variance using sliding window */

	int			i, k, isValidB, isPriorExp;
	MrBFlt		oldB, newB, window, minB, maxB, ran, priorExp=0.0, *bs;
	ModelParams *mp;

	/* get size of window, centered on current rate value */
	window = mvp[0];

	/* get model paramaters */
	mp = &modelParams[param->relParts[0]];

	/* get prior, minimum and maximum values for rate     */
	if (!strcmp(mp->symPiPr,"Uniform"))
		{
		isPriorExp = NO;
		minB = mp->symBetaUni[0];
		maxB = mp->symBetaUni[1];
		}
	else
		{
		isPriorExp = YES;
		priorExp = mp->symBetaExp;
		minB = SYMPI_MIN;
		maxB = SYMPI_MAX;
		}

	/* get old value of symDir */
	oldB = *GetParamVals(param, chain, state[chain]);

	/* change value */
	ran = RandomNumber(seed);
	newB = oldB + window * (ran - 0.5);

	/* check validity */
	isValidB = NO;
	do
		{
		if (newB < minB)
			newB = 2* minB - newB;
		else if (newB > maxB)
			newB = 2 * maxB - newB;
		else
			isValidB = YES;
		} while (isValidB == NO);

	/* set new value of symDir */
	*GetParamVals(param, chain, state[chain]) = newB;

	/* get proposal ratio */
	*lnProposalRatio = 0.0;

	/* get prior ratio */
	if (isPriorExp == YES)
		{
		*lnPriorRatio = priorExp * (oldB - newB);
		}
	else
		*lnPriorRatio = 0.0;

	/* fill in the new betacat frequencies */
	bs = GetParamSubVals(param, chain, state[chain]);
	k = mp->numBetaCats;
	BetaBreaks (newB, newB, bs, k);
	k *= 2;
	for (i=k-2; i>0; i-=2)
		{
		bs[i] = bs[i/2];
		}
	for (i=1; i<k; i+=2)
		{
		bs[i] = 1.0 - bs[i-1];
		}
		
	/* Set update flags for all tree nodes. Note that the conditional
	   likelihood update flags have been set for the relevant partitions
	   before we even call the move function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));
		
	/* may need to hit update flag for cijks if we have multistate characters */
	for (i=0; i<param->nRelParts; i++)
		if (modelSettings[param->relParts[i]].nCijkParts > 1)
			modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);

}





int Move_Beta_M (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* change symmetric Dirichlet variance using multiplier */

	int			i, k, isValidB, isPriorExp;
	MrBFlt		oldB, newB, minB, maxB, priorExp=0.0, *bs, ran, factor, tuning;
	ModelParams *mp;

	/* get tuning parameter */
	tuning = mvp[0];

	/* get model paramaters */
	mp = &modelParams[param->relParts[0]];

	/* get prior, minimum and maximum values for rate     */
	if (!strcmp(mp->symPiPr,"Uniform"))
		{
		isPriorExp = NO;
		minB = mp->symBetaUni[0];
		maxB = mp->symBetaUni[1];
		}
	else
		{
		isPriorExp = YES;
		priorExp = mp->symBetaExp;
		minB = SYMPI_MIN;
		maxB = SYMPI_MAX;
		}

	/* get old value of symDir */
	oldB = *GetParamVals(param, chain, state[chain]);

	/* change value */
	ran = RandomNumber(seed);
	factor = exp(tuning * (ran - 0.5));
	newB = oldB * factor;

	/* check validity */
	isValidB = NO;
	do
		{
		if (newB < minB)
			newB = minB * minB / newB;
		else if (newB > maxB)
			newB = maxB * maxB / newB;
		else
			isValidB = YES;
		} while (isValidB == NO);

	/* set new value of symDir */
	*GetParamVals(param, chain, state[chain]) = newB;

	/* get proposal ratio */
	*lnProposalRatio = log (newB / oldB);

	/* get prior ratio */
	if (isPriorExp == YES)
		{
		*lnPriorRatio = priorExp * (oldB - newB);
		}
	else
		*lnPriorRatio = 0.0;

	/* fill in the new betacat frequencies */
	bs = GetParamSubVals(param, chain, state[chain]);
	k = mp->numBetaCats;
	BetaBreaks (newB, newB, bs, k);
	k *= 2;
	for (i=k-2; i>0; i-=2)
		{
		bs[i] = bs[i/2];
		}
	for (i=1; i<k; i+=2)
		{
		bs[i] = 1.0 - bs[i-1];
		}
		
	/* Set update flags for all tree nodes. Note that the conditional
	   likelihood update flags have been set for the relevant partitions
	   before we even call the move function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));
		
	/* may need to hit update flag for cijks if we have multistate characters */
	for (i=0; i<param->nRelParts; i++)
		if (modelSettings[param->relParts[i]].nCijkParts > 1)
			modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);

}




/*----------------------------------------------------------------
|
|	Move_BiasedSpr: This proposal mechanism changes the topology and
|      branch lengths of an unrooted tree. 
|
|      Programmed by JH 2003-08-13
|
|	   TODO: Smart update of tiprobs, rnd insert both ends of
|		the moved branch on attachment branch
----------------------------------------------------------------*/
int Move_BiasedSpr (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	int			i, isVPriorExp, isBranchAnc, nNodes1, nNodes2;
	MrBFlt		tuning, warp, minV, maxV, brlensExp=0.0, newM, oldM, oldAttachmentV, newAttachmentV, v1, v2, v3, *pLengths, *probs,
				minLength, maxLength, sum, tempL, ran, origAttachmentPointProb=0.0, newAttachmentPointProb=0.0;
	TreeNode	*v, *w, *p, *q, *a, *b, *c, *d, *e, *origAttach1, *origAttach2, *root1, *root2,
				**subTree1DP, **subTree2DP, *newAttachmentNode, *origRoot;
	Tree		*t;
	ModelParams *mp;

	/* allocate some memory for this move */
	subTree1DP = (TreeNode **)malloc(sizeof(TreeNode *) * 2 * 2 * numTaxa);
	if (!subTree1DP)
		{
		MrBayesPrint ("%s   ERROR: Could not allocate subTree1DP\n", spacer);
		return (ERROR);
		}
	subTree2DP = subTree1DP + (2 * numTaxa);
	pLengths = (MrBFlt *)malloc(sizeof(MrBFlt) * 2 * 2 * numTaxa);
	if (!pLengths)
		{
		MrBayesPrint ("%s   ERROR: Could not allocate pLengths\n", spacer);
		free (subTree1DP);
		return (ERROR);
		}
	probs = pLengths + (2 * numTaxa);
	
	/* set the tuning parameter */
	tuning = mvp[0];
	
	/* set the parsimony warp factor */
	warp = mvp[1];
	
	/* initialize log prior and log proposal probabilities */
	*lnPriorRatio = *lnProposalRatio = 0.0;
	
	/* get tree */
	t = GetTree (param, chain, state[chain]);
	origRoot = t->root;

	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* max brlen */
	if (param->subParams[0]->paramId == BRLENS_UNI)
		{
		maxV = mp->brlensUni[1];
		isVPriorExp = NO;
		}
	else
		{
		maxV = BRLENS_MAX;
		brlensExp = mp->brlensExp;
		isVPriorExp = YES;
		}
		
	/* min brlen */
	minV = BRLENS_MIN;
	
	/* initialize all flags */
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		p->upDateCl = p->marked = NO;
		}
	
	/* Calculate log prior probability before the branch lengths of the tree have 
	   been changed. */
	if (isVPriorExp == YES)
		{
		for (i=0; i<t->nNodes; i++)
			{
			p = t->allDownPass[i];
			if (p->anc != NULL)
				*lnPriorRatio -= log(brlensExp) - brlensExp * p->length;
			}
		}

	/* Pick a branch. This branch is marked at the top as "v" and at the bottom as "w". 
	   We also note whether the branch is the ancestral one, or not. Finally, this branch
	   will eventually have its length changed via a Larget & Simon type of contraction/
	   expansion. */
	do
		{
		v = t->allDownPass[(int)(RandomNumber(seed) * t->nNodes)];
		} while (v->anc == NULL);
	w = v->anc;
	if (w->anc == NULL)
		isBranchAnc = YES;
	else
		isBranchAnc = NO;
	oldM = v->length;
	newM = oldM * exp(tuning * (RandomNumber(seed)-0.5));
	
	/* reflect the new value if necessary */
	while (newM < minV || newM > maxV)
		{
		if (newM > maxV)
			newM = maxV * maxV / newM;
		else if (newM < minV)
			newM = minV * minV / newM;
		}
	v->length = newM;

	/* update proposal probability */
	(*lnProposalRatio) += log(newM / oldM);
		
	/* snip tree into two pieces */
	if (v->left == NULL && v->right == NULL && v->anc != NULL)
		{ 
#		if defined (DEBUG_BIASED_SPR)
		printf ("Clip tip\n");
#		endif
		/* v is one of the tip sequences that is not at the root of the tree. 
		   We will clip the tree into two parts at v->anc. */
		b = v->anc;
		if (b->left == v)
			a = b->right;
		else
			a = b->left;
		c = b->anc;
		origAttach1 = a;
		origAttach2 = c;
		oldAttachmentV = a->length + b->length;
		if (c->left == b)
			c->left = a;
		else
			c->right = a;
		a->anc = c;
		b->left = v;
		b->anc = b->right = NULL;
		a->length += b->length;
		b->length = 0.0;
		root2 = b;
		root1 = t->root;
		}
	else if (v->anc->anc == NULL)
		{
		/* v is the tip sequence that points down (i.e., is at the root of the tree). */
#		if defined (DEBUG_BIASED_SPR)
		printf ("Clip tip (bottom)\n");
#		endif
		origAttach1 = v->left;
		origAttach2 = v->right;
		oldAttachmentV = v->left->length + v->right->length;
		while (v->left->left != NULL)
			{
			a = v->left;
			b = v->right;
			c = a->left;
			d = a->right;
			v->left = c;
			v->right = a;
			a->left = b;
			a->right = d;
			b->anc = d->anc = a;
			a->anc = c->anc = v;
			b->length += a->length;
			c->length *= 0.5;
			a->length = c->length;
			}
		a = v->left;
		b = v->right;
		b->anc = a;
		a->left = b;
		a->right = a->anc = NULL;
		b->length += a->length;
		a->length = 0.0;
		root1 = a;
		a = v->anc;
		b = v;
		v = a;
		v->length = b->length;
		b->length = 0.0;
		v->left = v->right = NULL;
		v->anc = b;
		b->left = v;
		b->right = b->anc = NULL;
		root2 = b;
		}
	else
		{
		if (RandomNumber(seed) < 0.5)
			{
#			if defined (DEBUG_BIASED_SPR)
			printf ("Clip v->anc\n");
#			endif
			/* v is an internal node and we cut it at v->anc. This is easy. */
			b = v->anc;
			if (b->left == v)
				a = b->right;
			else
				a = b->left;
			c = b->anc;
			origAttach1 = a;
			origAttach2 = c;
			oldAttachmentV = a->length + b->length;
			if (c->left == b)
				c->left = a;
			else
				c->right = a;
			a->anc = c;
			b->left = v;
			b->anc = b->right = NULL;
			a->length += b->length;
			b->length = 0.0;
			root2 = b;
			root1 = t->root;
			}
		else
			{
#			if defined (DEBUG_BIASED_SPR)
			printf ("Clip v\n");
#			endif
			/* v is an internal node and we cut it at v. This is not so easy. */
			origAttach1 = v->left;
			origAttach2 = v->right;
			oldAttachmentV = v->left->length + v->right->length;
			while (v->left->left != NULL)
				{
				a = v->left;
				b = v->right;
				c = a->left;
				d = a->right;
				v->left = c;
				v->right = a;
				a->left = b;
				a->right = d;
				b->anc = d->anc = a;
				a->anc = c->anc = v;
				b->length += a->length;
				c->length *= 0.5;
				a->length = c->length;
				}
			a = v->left;
			b = v->right;
			b->anc = a;
			a->left = b;
			a->right = a->anc = NULL;
			b->length += a->length;
			a->length = 0.0;
			root1 = a;

			v->left = v->right = NULL;
			b = v->anc;
			if (b->left == v)
				a = b->right;
			else
				a = b->left;
			c = b->anc;
			if (c->left == b)
				c->left = a;
			else
				c->right = a;
			a->anc = c;
			v1 = a->length;
			v2 = b->length;
			v3 = v->length;
			d = t->root;
			e = d->left;
			b->left = e;
			b->right = d;
			d->left = d->right = NULL;
			e->anc = d->anc = b;
			b->anc = v;
			v->left = b;
			v->right = v->anc = NULL;
			a->length = v1 + v2;
			b->length = v3;
			e->length *= 0.5;
			d->length = e->length;
			a->marked = c->marked = YES;
			q = a;
			while (q->anc != NULL)
				{
				q->marked = YES;
				q = q->anc;
				q->marked = YES;
				}
			v = b;
			while (v->left != a && v->right != a)
				{
				if (v->left->marked == YES && v->right->marked == NO)
					{
					b = v->left;
					c = v->right;
					}
				else if (v->left->marked == NO && v->right->marked == YES)
					{
					b = v->right;
					c = v->left;
					}
				else
					{
					MrBayesPrint ("%s   ERROR: In BiasedSPR\n", spacer);
					return (ERROR);
					}
				if (b->left != NULL && b->right != NULL)
					{
					if (b->left->marked == YES && b->right->marked == NO)
						{
						e = b->left;
						d = b->right;
						}
					else if (b->left->marked == NO && b->right->marked == YES)
						{
						e = b->right;
						d = b->left;
						}
					else
						{
						MrBayesPrint ("%s   ERROR: In BiasedSPR\n", spacer);
						return (ERROR);
						}
					}
				else
					{
					MrBayesPrint ("%s   ERROR: In BiasedSPR\n", spacer);
					return (ERROR);
					}
				v->left = e;
				v->right = b;
				b->left = d;
				b->right = c;
				e->anc = b->anc = v;
				d->anc = c->anc = b;
				b->marked = NO;
				c->length += b->length;
				e->length *= 0.5;
				b->length = e->length;
				}
			v->length = v3;
			if (v->left == a)
				{
				v->left->length = v1;
				v->right->length = v2;
				}
			else
				{
				v->left->length = v2;
				v->right->length = v1;
				}
			root2 = v->anc;
			}
		}

	/* There are now two subtrees. One tree, rooted at root1, is not moveable. The other tree will be attached to one
	   of the nodes of the tree rooted at root1. The other tree has a dangling root (i.e., the root node is not a
	   taxon). This tree will be connected by root2 to a branch in the tree rooted at root1. */
	   
	/* Get a downpass sequences for the trees rooted at root1 and root2. */
	i = 0;
	GetTempDownPassSeq (root1, &i, subTree1DP);
	nNodes1 = i;
	i = 0;
	GetTempDownPassSeq (root2, &i, subTree2DP);
	nNodes2 = i;

	/* Get parsimony lengths of all possible attachment points. */
	GetSprParsimonyLengths (chain, nNodes1, nNodes2, subTree1DP, subTree2DP, root2, pLengths);

	/* find minimum and maximum lengths for reattachment points */
	minLength =  1000000.0;
	maxLength = -1000000.0;
	for (i=0; i<nNodes1; i++)
		{
		p = subTree1DP[i];
		if (p->anc != NULL)
			{
			if (pLengths[i] < minLength)
				minLength = pLengths[i];
			if (pLengths[i] > maxLength)
				maxLength = pLengths[i];
			}
		}

	/* calculate probabilities of reattaching to each node based on parsimony score */
	sum = 0.0;
	for (i=0; i<nNodes1; i++)
		{
		p = subTree1DP[i];
		if (p->anc != NULL)
			{
			tempL = maxLength - pLengths[i];
			probs[i] = pow((tempL + 1.0), warp);
			sum += probs[i];
			}
		}
	for (i=0; i<nNodes1; i++)
		{
		p = subTree1DP[i];
		if (p->anc != NULL)
			probs[i] /= sum;
		}
		
	/* get probability of reattaching to old attachment point */
	for (i=0; i<nNodes1; i++)
		{
		p = subTree1DP[i];
		if (p->anc != NULL)
			{
			if ((p == origAttach1 && p->anc == origAttach2) || (p == origAttach2 && p->anc == origAttach1))
				origAttachmentPointProb = probs[i];
			}
		}
#	if defined (DEBUG_BIASED_SPR)
	for (i=0; i<nNodes1; i++)
		{
		p = subTree1DP[i];
		if (p->anc != NULL)
			{
			printf ("%4d -- %lf %lf ", p->index, pLengths[i], probs[i]);
			if ((p == origAttach1 && p->anc == origAttach2) || (p == origAttach2 && p->anc == origAttach1))
				printf (" <- original attachment\n");
			else
				printf ("\n");
			}
		else	
			printf ("%4d -- \n ", p->index);
		}
#	endif

	/* decide which branch will be the attachment point */
	ran = RandomNumber(seed);
	sum = 0.0;
	newAttachmentNode = NULL;
	for (i=0; i<nNodes1; i++)
		{
		p = subTree1DP[i];
		if (p->anc != NULL)
			{
			sum += probs[i];
			if (ran < sum)
				{
				newAttachmentPointProb = probs[i];
				newAttachmentNode = p;
				break;
				}
			}
		}
	newAttachmentV = newAttachmentNode->length;
#	if defined (DEBUG_BIASED_SPR)
	printf ("newAttachmentNode=%d origAttachmentPointProb=%lf newAttachmentPointProb=%lf\n", newAttachmentNode->index, origAttachmentPointProb, newAttachmentPointProb);
#	endif
	p = root2;
	b = p->left;
	a = newAttachmentNode->anc;
	if (a->left == newAttachmentNode)
		a->left = p;
	else
		a->right = p;
	p->anc = a;
	p->left = newAttachmentNode;
	p->right = b;
	newAttachmentNode->anc = b->anc = p;
	v1 = newAttachmentNode->length * RandomNumber(seed);
	p->length = v1;
	newAttachmentNode->length -= v1;
	t->root = root1;

	/* adjust proposal ratio based on lengths of old and new attachment points */
	(*lnProposalRatio) += log(newAttachmentV) - log(oldAttachmentV);
	(*lnProposalRatio) += log (origAttachmentPointProb) - log (newAttachmentPointProb);
	
	/* get the new downpass sequence */
	GetDownPass (t);


	/* set the root node to its original position */
	/*if (t->root != origRoot)
		{
		q = NULL;
		for (i=0; i<t->nNodes; i++)
			{
			p = t->allDownPass[i];
			p->marked = NO;
			if (p == origRoot)
				q = p;
			}
		if (q == NULL)
			{
			MrBayesPrint ("%s   ERROR: Could not find original root position\n", spacer);
			return (ERROR);
			}
		
		p = origRoot;
		do
			{
			p->marked = YES;
			if (p->anc != NULL)
				p = p->anc;
			} while (p->anc != NULL);
		t->root->marked = NO;
		
		p = t->root;
		q = p->left;
		v = &tempNode;
		v->left = q;
		v->right = p;
		v->anc = NULL;
		p->anc = q->anc = v;
		q->length *= 0.5;
		p->length = q->length;
		
		while (v->left != origRoot)
			{
			a = v->left;
			b = v->right;
			if (a->marked == YES && b->marked == NO)
				{
				}
			else
				{
				MrBayesPrint ("%s   ERROR: Problem marking nodes in biased SPR (1)\n", spacer);
				return (ERROR);
				}
			if (a->left == NULL && a->right == NULL)
				{
				if (a != origRoot)
					{
					MrBayesPrint ("%s   ERROR: Problem marking nodes in biased SPR (2)\n", spacer);
					return (ERROR);
					}
				}
			else
				{
				if (a->left->marked == YES && a->right->marked == NO)
					{
					c = a->left;
					d = a->right;
					}
				else if (a->left->marked == NO && a->right->marked == YES)
					{
					c = a->right;
					d = a->left;
					}
				else
					{
					MrBayesPrint ("%s   ERROR: Problem marking nodes in biased SPR (3)\n", spacer);
					return (ERROR);
					}
				v->left = c;
				v->right = a;
				a->anc = c->anc = v;
				a->left = d;
				a->right = b;
				b->anc = d->anc = a;
				a->marked = NO;
				b->length += a->length;
				c->length *= 0.5;
				a->length = c->length;
				}
			}
		
		a = v->left;
		b = v->right;
		b->anc = a;
		a->left = b;
		a->right = a->anc = NULL;
		b->length += a->length;
		a->length = 0.0;
		v->left = v->right = v->anc = NULL;
		t->root = a;
		
		if (a != origRoot)	
			{
			MrBayesPrint ("%s   ERROR: Incorrectly rerooted tree in biased SPR\n", spacer);
			return (ERROR);
			}
		
		GetDownPass (t);
		}*/

	/* update all of the conditional likelihood and transition probability flags */
	TouchAllTreeNodes (t);

	/* Calculate log prior probability after the branch lengths of the tree have 
	   been changed. */
	if (isVPriorExp == YES)
		{
		for (i=0; i<t->nNodes; i++)
			{
			p = t->allDownPass[i];
			if (p->anc != NULL)
				*lnPriorRatio += log(brlensExp) - brlensExp * p->length;
			}
		}
	
	/* free memory */
	free (subTree1DP);
	free (pLengths);
				
	return (NO_ERROR);

}





int Move_BrLen (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* change one branch length */

	MrBFlt		tuning, maxV, minV, m, newM, brlensPrExp=0.0;
	TreeNode	*p;
	ModelParams *mp;
	Tree		*t;

	tuning = mvp[0]; /* Larget & Simon's tuning parameter lambda */

	mp = &modelParams[param->relParts[0]];

	/* max and min brlen */
	if (param->paramId == BRLENS_UNI)
		{
		minV = mp->brlensUni[0];
		maxV = mp->brlensUni[1];
		}
	else
		{
		minV = BRLENS_MIN;
		maxV = BRLENS_MAX;
		brlensPrExp = mp->brlensExp;
		}

	/* get tree */
	t = GetTree (param, chain, state[chain]);

	/* pick a branch */
	do
		{
		p = t->allDownPass[(int)(RandomNumber(seed)*t->nNodes)];
		} while (p->anc == NULL || (t->isRooted == YES && p->anc->anc == NULL));

	/* determine new length */
	m = p->length;
	newM = m * exp(tuning * (RandomNumber(seed) - 0.5));

	/* reflect new length if necessary */
	while (newM < minV || newM > maxV)
		{
		if (newM < minV)
			newM = minV * minV / newM;
		else if (newM > maxV)
			newM = maxV * maxV / newM;
		}
	p->length = newM;

	/* calculate proposal ratio */
	/* must be based on new length after reflection */
	(*lnProposalRatio) = log(newM / m);

	/* set flags for update of transition probabilities at p */
	p->upDateTi = YES;

	/* set the update flag for cond likes if p is connected to root in unrooted */
	/* tree, if this is not done, cond likes are not updated in this case       */
  	if (t->isRooted == NO && p->anc->anc == NULL)  
  		p->upDateCl = YES;  

	/* set flags for update of cond likes from p->anc and down to root */
	while (p->anc != NULL)
		{
		p = p->anc;
		p->upDateCl = YES; 
		}

	/* update prior if exponential prior on branch lengths */
	if (param->paramId == BRLENS_EXP)
		(*lnPriorRatio) = brlensPrExp * (m - newM);

	return (NO_ERROR);
	
}





int Move_ClockRate (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* change clock rate (tree height) for calibrated tree using multiplier */

	int			i, isPriorExp, isValidWait;
	MrBFlt		factor, tuning, minV, newLnPrior, oldLnPrior=0.0, theta=0.0, sR=0.0, eR=0.0, sF=0.0,
				priorExp=0.0, minWait, maxWait, newWait, oldWait, treeFactor, growth=0.0,
				x=0.0, y=0.0;
	TreeNode	*p;
	ModelInfo	*m;
	ModelParams *mp;
	Tree		*t;

	tuning = mvp[0]; /* Larget & Simon's tuning parameter lambda */

	mp = &modelParams[param->relParts[0]];
	m = &modelSettings[param->relParts[0]];

	/* min brlen */
	minV = BRLENS_MIN;

	t = GetTree (param, chain, state[chain]);

	/* prior on clock waiting time */
	if (!strcmp(mp->calWaitPr,"Exponential"))
		{
		isPriorExp = YES;
		priorExp = mp->calWaitExp;
		minWait = 0.00000000001;
		maxWait = 10000000000000000.0;
		}
	else
		{
		isPriorExp = NO;
		minWait = mp->calWaitUni[0];
		maxWait = mp->calWaitUni[1];
		}
	
	/* calculate prior ratio (part 1/2) */
	if (param->paramId == BRLENS_CLOCK_UNI ||
		param->paramId == BRLENS_CCLOCK_UNI)
		{
		/* uniformly distributed branch lengths */
		(*lnPriorRatio) = 0.0;
		}
	else if (param->paramId == BRLENS_CLOCK_COAL ||
			 param->paramId == BRLENS_CCLOCK_COAL)
		{
		/* coalescence prior */
		m = &modelSettings[param->relParts[0]];
		theta = *(GetParamVals (m->theta, chain, state[chain]));
		if (!strcmp(mp->growthPr, "Fixed"))
			growth = mp->growthFix;
		else
			growth = *(GetParamVals (m->growthRate, chain, state[chain]));
		if (LnCoalescencePriorPr (t, &oldLnPrior, theta, growth) == ERROR)
			{
			MrBayesPrint ("%s   Problem calculating prior for coalescence process\n", spacer);
			return (ERROR);
			}
		}
	else
		{
		/* birth-death prior */
		m = &modelSettings[param->relParts[0]];
		sR = *(GetParamVals (m->speciationRates, chain, state[chain]));
		eR = *(GetParamVals (m->extinctionRates, chain, state[chain]));
		sF = mp->sampleProb;
		if (LnBirthDeathPriorPr (t, &oldLnPrior, sR, eR, sF) == ERROR)
			{
			MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
			return (ERROR);
			}
		}

	/* determine multiplication factor */
	factor = exp(tuning * (RandomNumber(seed) - 0.5));

	/* multiply clock waiting time by this factor */
	oldWait = 1.0 / t->clockRate;
	newWait = oldWait * factor;

	/* check that clock waiting time is within bounds */
	isValidWait = NO;
	do
		{
		if (newWait < minWait)
			newWait = 2* minWait - newWait;
		else if (newWait > maxWait)
			newWait = 2 * maxWait - newWait;
		else
			isValidWait = YES;
		} while (isValidWait == NO);	
	
	/* store new waiting time as clock rate */
	treeFactor = oldWait / newWait;
	t->clockRate = 1.0 / newWait;

	/* multiply all branch lengths by this factor */
	/* set update flags in the process */
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->anc != NULL)
			{
			if (p->anc->anc != NULL)
				{
				p->length *= treeFactor;
				if (p->length < BRLENS_MIN)
					p->length = BRLENS_MIN;
				p->upDateTi = YES;
				}
			if (p->left != NULL)
 				p->upDateCl = YES; 
			}
		}

	/* now set node depths in a safe manner */
	for (i=0; i<t->nIntNodes; i++)
		{
		p = t->intDownPass[i];
		x = p->left->nodeDepth + p->left->length;
		y = p->right->nodeDepth + p->right->length;
		if (x > y)
			{
			p->right->length += (x - y);
			p->nodeDepth = x;
			}
		else
			{
			p->left->length += (y - x);
			p->nodeDepth = y;
			}
		}

	/* calculate proposal ratio */
	(*lnProposalRatio) = log(newWait/oldWait);

	/* calculate prior ratio (part 2/2) */
	if (param->paramId == BRLENS_CLOCK_UNI || param->paramId == BRLENS_CCLOCK_UNI)
		{
		/* uniformly distributed branch lengths */
		(*lnPriorRatio) = 0.0;
		}
	else if (param->paramId == BRLENS_CLOCK_COAL || param->paramId == BRLENS_CCLOCK_COAL)
		{
		/* coalescence prior */
		if (LnCoalescencePriorPr (t, &newLnPrior, theta, growth) == ERROR)
			{
			MrBayesPrint ("%s   Problem calculating prior for coalescence process\n", spacer);
			return (ERROR);
			}
		(*lnPriorRatio) = newLnPrior - oldLnPrior;
		}
	else
		{
		/* birth-death prior */
		if (LnBirthDeathPriorPr (t, &newLnPrior, sR, eR, sF) == ERROR)
			{
			MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
			return (ERROR);
			}
		(*lnPriorRatio) = newLnPrior - oldLnPrior;
		}

	/* finally adjust for prior on clock ratio */
	if (isPriorExp == YES)
		(*lnPriorRatio) += priorExp * (oldWait - newWait);

	return (NO_ERROR);
	
}





int Move_Extinction (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* change extinction rate using sliding window */
	
	int			isMPriorExp, isValidM;
	MrBFlt		oldM, newM, window, minM, maxM, muExp=0.0, ran, sR, eR, sF, oldLnPrior, newLnPrior;
	ModelParams *mp;
	ModelInfo	*m;
	Tree		*t;

	/* get size of window, centered on current mu value */
	window = mvp[0];

	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* get speciation rate */
	m = &modelSettings[param->relParts[0]];
	sR = *(GetParamVals (m->speciationRates, chain, state[chain]));
	
	/* get minimum and maximum values for mu */
	if (param->paramId == EXTRATE_UNI)
		{
		minM = mp->extinctionUni[0];
		if (sR > mp->extinctionUni[1])
			maxM = mp->extinctionUni[1];
		else
			maxM = sR;
		if (maxM < minM)
			minM = 0.0;
		isMPriorExp = NO;
		}
	else
		{
		minM = 0.0;
		maxM = sR;
		muExp = mp->extinctionExp;
		isMPriorExp = YES;
		}

	/* get old value of mu */
	newM = oldM = *GetParamVals(param, chain, state[chain]);

	/* change value for mu */
	ran = RandomNumber(seed);
	newM = oldM + window * (ran - 0.5);
	
	/* check that new value is valid */
	isValidM = NO;
	do
		{
		if (newM < minM)
			newM = 2* minM - newM;
		else if (newM > maxM)
			newM = 2 * maxM - newM;
		else
			isValidM = YES;
		} while (isValidM == NO);

	/* get proposal ratio */
	*lnProposalRatio = 0.0;
	
	/* calculate prior ratio */
	t = GetTree(modelSettings[param->relParts[0]].brlens,chain,state[chain]);
	sF = mp->sampleProb;
	eR = oldM;
	if (LnBirthDeathPriorPr (t, &oldLnPrior, sR, eR, sF) == ERROR)
		{
		MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
		return (ERROR);
		}
	eR = newM;
	if (LnBirthDeathPriorPr (t, &newLnPrior, sR, eR, sF) == ERROR)
		{
		MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
		return (ERROR);
		}
	if (isMPriorExp == NO)
		*lnPriorRatio = newLnPrior - oldLnPrior;
	else
		*lnPriorRatio = -muExp * (newM - oldM) + (newLnPrior - oldLnPrior);
	
	/* copy new mu value back */
	*GetParamVals(param, chain, state[chain]) = newM;

	return (NO_ERROR);

}





int Move_Extinction_M (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* change extinction rate using multiplier */

	int			isMPriorExp, isValidM;
	MrBFlt		oldM, newM, minM, maxM, muExp=0.0, ran, tuning, factor, sR, eR, sF, oldLnPrior, newLnPrior;
	ModelParams *mp;
	ModelInfo	*m;
	Tree		*t;

	/* get tuning parameter */
	tuning = mvp[0];

	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* get speciation rate */
	m = &modelSettings[param->relParts[0]];
	sR = *(GetParamVals (m->speciationRates, chain, state[chain]));
	
	/* get minimum and maximum values for mu */
	if (param->paramId == EXTRATE_UNI)
		{
		minM = mp->extinctionUni[0];
		if (sR > mp->extinctionUni[1])
			maxM = mp->extinctionUni[1];
		else
			maxM = sR;
		if (maxM < minM)
			minM = 0.0;
		isMPriorExp = NO;
		}
	else
		{
		minM = 0.0;
		maxM = sR;
		muExp = mp->extinctionExp;
		isMPriorExp = YES;
		}

	/* get old value of mu */
	newM = oldM = *GetParamVals(param, chain, state[chain]);

	/* change value for mu */
	ran = RandomNumber(seed);
	factor = exp(tuning * (ran - 0.5));
	newM = oldM * factor;
	
	/* check that new value is valid */
	isValidM = NO;
	do
		{
		if (newM < minM)
			newM = minM * minM / newM;
		else if (newM > maxM)
			newM = maxM * maxM / newM;
		else
			isValidM = YES;
		} while (isValidM == NO);

	/* get proposal ratio */
	*lnProposalRatio = log (factor);
	
	/* calculate prior ratio */
	t = GetTree(modelSettings[param->relParts[0]].brlens,chain,state[chain]);
	sF = mp->sampleProb;
	eR = oldM;
	if (LnBirthDeathPriorPr (t, &oldLnPrior, sR, eR, sF) == ERROR)
		{
		MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
		return (ERROR);
		}
	eR = newM;
	if (LnBirthDeathPriorPr (t, &newLnPrior, sR, eR, sF) == ERROR)
		{
		MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
		return (ERROR);
		}
	if (isMPriorExp == NO)
		*lnPriorRatio = newLnPrior - oldLnPrior;
	else
		*lnPriorRatio = -muExp * (newM - oldM) + (newLnPrior - oldLnPrior);
	
	/* copy new mu value back */
	*GetParamVals(param, chain, state[chain]) = newM;

	return (NO_ERROR);

}




int Move_ExtSPRClock (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* Change branch lengths and topology (potentially) using SPR-type move 
	   with extension probability (rather than window, attachment rate or similar). */

	/* This move picks a branch and then moves its lower attachment point 
	   from its original position, one node at a time, with
	   a probability determined by the extensionProb parameter. This is
	   done in a way consistent with the clock constraints and any locked
	   nodes there might be in the tree.
	   
	   On the ending branch, the attachment point is reinserted randomly
	   along the branch (below the maximum age of the node). */
	
	int			topologyHasChanged, isStartLocked=0, isStopLocked=0, nRootNodes, directionUp;
	MrBFlt		delta, x, y, oldBrlen=0.0, newBrlen=0.0, tuning, extensionProb, oldLnPrior, newLnPrior;
	MrBFlt		theta=0.0, growth=0.0, sR=0.0, eR=0.0, sF=0.0, minV;
	TreeNode	*p, *a, *b, *u, *v;
	Tree		*t;
	ModelParams *mp;
	ModelInfo	*m;

#if defined (DEBUG_ExtSPRClock)
	int			i;
#endif

	/* this parameter should be possible to set by user */
	extensionProb = mvp[0];	/* extension probability */
	tuning = mvp[1];        /* Larget & Simon's tuning parameter lambda */
	(*lnProposalRatio) = (*lnPriorRatio) = 0.0;
	
	/* get tree */
	t = GetTree (param, chain, state[chain]);

	/* min branch length */
	minV = BRLENS_MIN;
	
#if defined (DEBUG_CONSTRAINTS)
	CheckConstraints (t);
#endif

#if defined (DEBUG_ExtSPRClock)
	for (i=0; i<t->nNodes-2; i++) {
		p = t->allDownPass[i];
		if (p->length < minV) {
			printf ("%s   ERROR when entering extSPRClock: node %d has length %lf", spacer, p->index, p->length);
			return ERROR;
		}
		if (p->nodeDepth >= p->anc->nodeDepth) {
			printf ("%s   ERROR when entering extSPRClock: node %d has depth %lf larger than its ancestor %d depth %lf", spacer, p->index, p->nodeDepth, p->anc->index, p->anc->nodeDepth);
			return ERROR;
		}
	}
#endif

	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* assume no topology change */
	topologyHasChanged = NO;

#	if defined (DEBUG_ExtSPRClock)
	printf ("Before:\n");
	ShowNodes (t->r, 2, YES);
	getchar();
#	endif
	
	/* calculate prior ratio (part 1/2) */
	if (param->subParams[0]->paramId == BRLENS_CLOCK_UNI ||
		param->subParams[0]->paramId == BRLENS_CCLOCK_UNI)
		{
		/* uniformly distributed branch lengths */
		if (!strcmp(mp->treeHeightPr, "Exponential"))
			oldLnPrior = log(mp->treeHeightExp) - mp->treeHeightExp * t->root->left->nodeDepth;
		else
			oldLnPrior = mp->treeHeightGamma[1] * log(mp->treeHeightGamma[0]) - LnGamma(mp->treeHeightGamma[1]) + (mp->treeHeightGamma[1] - 1.0) * log(t->root->left->nodeDepth) - mp->treeHeightGamma[0] * t->root->left->nodeDepth;
		}
	else if (param->subParams[0]->paramId == BRLENS_CLOCK_COAL ||
			 param->subParams[0]->paramId == BRLENS_CCLOCK_COAL)
		{
		/* coalescence prior */
		m = &modelSettings[param->relParts[0]];
		theta = *(GetParamVals (m->theta, chain, state[chain]));
		if (!strcmp(mp->growthPr, "Fixed"))
			growth = mp->growthFix;
		else
			growth = *(GetParamVals (m->growthRate, chain, state[chain]));
		if (LnCoalescencePriorPr (t, &oldLnPrior, theta, growth) == ERROR)
			{
			MrBayesPrint ("%s   Problem calculating prior for coalescence process\n", spacer);
			return ERROR;
			}
		}
	else
		{
		/* birth-death prior */
		m = &modelSettings[param->relParts[0]];
		sR = *(GetParamVals (m->speciationRates, chain, state[chain]));
		eR = *(GetParamVals (m->extinctionRates, chain, state[chain]));
		sF = mp->sampleProb;
		if (LnBirthDeathPriorPr (t, &oldLnPrior, sR, eR, sF) == ERROR)
			{
			MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
			return ERROR;
			}
		if (!strcmp(mp->treeHeightPr, "Exponential"))
			oldLnPrior += log(mp->treeHeightExp) - mp->treeHeightExp * t->root->left->nodeDepth;
		else
			oldLnPrior += mp->treeHeightGamma[1] * log(mp->treeHeightGamma[0]) - LnGamma(mp->treeHeightGamma[1]) + (mp->treeHeightGamma[1] - 1.0) * log(t->root->left->nodeDepth) - mp->treeHeightGamma[0] * t->root->left->nodeDepth;
		}

	/* pick a branch */
	do
		{
		p = t->allDownPass[(int)(RandomNumber(seed)*(t->nNodes - 1))];
		} while (p->anc->anc == NULL || p->anc->isDated == YES);
		
	/* set up pointers for nodes around the picked branch */
	v = p;
	u = p->anc;
	if (u->left == v)
		a = u->right;
	else
		a = u->left;
	b = u->anc;

	if (b->anc == NULL)
		{
		/* close to root, do things differently */
		if (u->left->length < u->right->length)
			x = u->left->length;
		else
			x = u->right->length;

		do {
			y = exp ((RandomNumber (seed) - 0.5) * tuning) * x;
		} while (y < minV);

		/* adjust proposal and prior ratios */
		(*lnProposalRatio) = (log(y) - log(x));
		
		/* adjust branch lengths */
		delta = y - x;
		v->length += delta;
		a->length += delta;
		u->nodeDepth += delta;

		/* set tiprobs update flags */
		v->upDateTi = YES;
		a->upDateTi = YES;
		}
	else
		{
		/* record branch length */
		if (u->anc->anc == NULL)
			oldBrlen = a->length;
		else
			oldBrlen = a->length + u->length;

		/* cut tree */
		a->anc = b;
		if (b->left == u)
			b->left = a;
		else
			b->right = a;
		if (b->anc != NULL)
			a->length += u->length;

		/* determine initial direction of move and whether the reverse move would be stopped by constraints */
		isStartLocked = NO;
		if (RandomNumber(seed) < 0.5)
			{
			directionUp = YES;
			if (b->anc == NULL || u->isLocked == YES)
				isStartLocked = YES;
			}
		else
			{
			directionUp = NO;
			if (a->left == NULL || a->isLocked == YES || a->nodeDepth < v->nodeDepth)
				isStartLocked = YES;
			}
			
		/* move around in root subtree */
		for (nRootNodes=0; RandomNumber(seed)<extensionProb; nRootNodes++) 
			{
			if (directionUp == YES) 
				{	/* going up tree */
				if (a->left == NULL || a->isLocked == YES || a->nodeDepth < (v->nodeDepth + 3*minV) ||
					a->length < 3*minV)
					break;		/* can't go further */
				topologyHasChanged = YES;
				b = a;
				if (RandomNumber(seed) < 0.5)
					a = a->left;
				else
					a = a->right;
				if (u->isLocked == YES)
					{
					b->isLocked = YES;
					u->isLocked = NO;
					b->lockID = u->lockID;
					u->lockID = 0;
					}
				}
			else 
				{	/* going down tree */
				if (b->anc == NULL || u->isLocked == YES)
					break;		/* can't go further */
				topologyHasChanged = YES;
				if (RandomNumber(seed)<0.5 || b->anc->anc == NULL || b->length < 3*minV) 
					{
					directionUp = YES; /* switch direction */
					/* find sister of a */
					if (b->left == a) 
						{
						a = b->right;
						}
					else 
						{  
						a = b->left;
						}
					/* as long as we are moving upwards
	     			the cond likes to update will be
					flagged by the last pass from u to the root */
					}	
				else 
					{	/* continue down */
					a = b;
					b = b->anc;
					a->upDateCl = YES; 
					if (a->isLocked == YES)
						{
						u->isLocked = YES;
						a->isLocked = NO;
						u->lockID = a->lockID;
						a->lockID = 0;
						}
					}
				}
			}
			
		/* determine whether the forward move was or would have been stopped by constraints */
		isStopLocked = NO;
		if (directionUp == NO)
			{
			if (b->anc == NULL || u->isLocked == YES)
				isStopLocked = YES;
			}
		else
			{
			if (a->left == NULL || a->isLocked == YES || a->nodeDepth < v->nodeDepth)
				isStopLocked = YES;
			}

		/* reattach u */
		if (u->left == v)
			u->right = a;
		else
			u->left = a;
		a->anc = u;
		u->anc = b;
		if (b->left == a)
			b->left = u;
		else
			b->right = u;

		/* insert u randomly on branch below a */
		if (a->nodeDepth > v->nodeDepth)
			x = a->length;
		else
			x = (b->nodeDepth - v->nodeDepth);
		newBrlen = x;

		do {
			y = RandomNumber (seed) * x;
		} while (y < minV || (x - y) < minV);
		
		/* adjust lengths */
		a->length -= y;
		u->length = y;
		u->nodeDepth = a->nodeDepth + a->length;
		v->length = u->nodeDepth - v->nodeDepth;
		
		/* set tiprobs update flags */
		a->upDateTi = YES;
		u->upDateTi = YES;
		v->upDateTi = YES;
		}

	/* set flags for update of cond likes from u and down to root */
	p = u;
	while (p->anc != NULL)
		{
		p->upDateCl = YES; 
		p = p->anc;
		}

	/* get down pass sequence if tree topology has changed */
	if (topologyHasChanged == YES)
		{
		GetDownPass (t);
		}

	/* adjust proposal ratio */
	if (topologyHasChanged == YES)
		{
		(*lnProposalRatio) += log (newBrlen) - log (oldBrlen);
		if (isStartLocked == NO && isStopLocked == YES)
			(*lnProposalRatio) += log (1.0 - extensionProb);
		else if (isStartLocked == YES && isStopLocked == NO)
			(*lnProposalRatio) -= log (1.0 - extensionProb);
		}

	/* calculate prior ratio (part 2/2) */
	if (param->subParams[0]->paramId == BRLENS_CLOCK_UNI ||
		param->subParams[0]->paramId == BRLENS_CCLOCK_UNI)
		{
		/* uniformly distributed branch lengths */
		if (!strcmp(mp->treeHeightPr, "Exponential"))
			newLnPrior = log(mp->treeHeightExp) - mp->treeHeightExp * t->root->left->nodeDepth;
		else
			newLnPrior = mp->treeHeightGamma[1] * log(mp->treeHeightGamma[0]) - LnGamma(mp->treeHeightGamma[1]) + (mp->treeHeightGamma[1] - 1.0) * log(t->root->left->nodeDepth) - mp->treeHeightGamma[0] * t->root->left->nodeDepth;
		(*lnPriorRatio) = newLnPrior - oldLnPrior;
		}
	else if (param->subParams[0]->paramId == BRLENS_CLOCK_COAL ||
			 param->subParams[0]->paramId == BRLENS_CCLOCK_COAL)
		{
		/* coalescence prior */
		if (LnCoalescencePriorPr (t, &newLnPrior, theta, growth) == ERROR)
			{
			MrBayesPrint ("%s   Problem calculating prior for coalescence process\n", spacer);
			return ERROR;
			}
		(*lnPriorRatio) = newLnPrior - oldLnPrior;
		}
	else
		{
		/* birth-death prior */
		if (LnBirthDeathPriorPr (t, &newLnPrior, sR, eR, sF) == ERROR)
			{
			MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
			return ERROR;
			}
		if (!strcmp(mp->treeHeightPr, "Exponential"))
			newLnPrior += log(mp->treeHeightExp) - mp->treeHeightExp * t->root->left->nodeDepth;
		else
			newLnPrior += mp->treeHeightGamma[1] * log(mp->treeHeightGamma[0]) - LnGamma(mp->treeHeightGamma[1]) + (mp->treeHeightGamma[1] - 1.0) * log(t->root->left->nodeDepth) - mp->treeHeightGamma[0] * t->root->left->nodeDepth;
		(*lnPriorRatio) = newLnPrior - oldLnPrior;
		}

#	if defined (DEBUG_ExtSPRClock)
	printf ("After:\n");
	ShowNodes (t->root, 2, YES);
	getchar();
	printf ("Proposal ratio: %f\n",(*lnProposalRatio));
	printf ("v: %d  u: %d  a: %d  b: %d\n",v->index, u->index, a->index, b->index);
	printf ("No. nodes moved in root subtree: %d\n",nRootNodes);
	printf ("Has topology changed? %d\n",topologyHasChanged);
	getchar();
#	endif

#	if defined (TOPOLOGY_MOVE_STATS)
	if (topologyHasChanged == YES)
		gTopologyHasChanged = YES;
	else
		gTopologyHasChanged = NO;

	gNodeMoves = nRootNodes;
#	endif

#if defined DEBUG_CONSTRAINTS
	CheckConstraints (t);
#endif

#if defined (DEBUG_ExtSPRClock)
	for (i=0; i<t->nNodes-2; i++) {
		p = t->allDownPass[i];
		if (p->length < minV) {
			printf ("%s   ERROR when leaving extSPRClock: node %d has length %lf", spacer, p->index, p->length);
			getchar();
		}
		if (p->nodeDepth >= p->anc->nodeDepth) {
			printf ("%s   ERROR when leaving extSPRClock: node %d has depth %lf larger than its ancestor %d depth %lf", spacer, p->index, p->nodeDepth, p->anc->index, p->anc->nodeDepth);
			getchar();
		}
	}
#endif

	return (NO_ERROR);
	
}





int Move_ExtTBR (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* Change branch lengths and topology (potentially) using TBR (unrooted) 
	   with extension probability (rather than window). */

	/* this move type picks a branch and two "danglies", modifies their length
	   independently according to the method of Larget & Simon (1999: MBE); it then
	   moves the danglies away from their original position one node at a time with
	   a probability determined by the extensionProb parameter

	   when the danglies are moved, their direction is changed
	   this "reflection" is necessary to enable the back move

	   This move type has been tested on all combinations of rooted and unrooted,
	   constrained and unconstrained trees */
	
	int			topologyHasChanged, nCrownNodes, nRootNodes, directionLeft, directionUp, 
				isVPriorExp;
	MrBFlt		m, x, y, tuning, maxV, minV, extensionProb, brlensExp=0.0;
	TreeNode	*p, *a, *b, *c, *d, *u, *v;
	Tree		*t;
	ModelParams *mp;

	/* these parameters should be possible to set by user */
	extensionProb = mvp[0];	/* extension probability */
	tuning = mvp[1];        /* Larget & Simon's tuning parameter lambda */
	
	/* get tree */
	t = GetTree (param, chain, state[chain]);
#if defined DEBUG_CONSTRAINTS
	CheckConstraints (t);
#endif

	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* max and min brlen */
	if (param->subParams[0]->paramId == BRLENS_UNI)
		{
		minV = mp->brlensUni[0] > BRLENS_MIN ? mp->brlensUni[0] : BRLENS_MIN;
		maxV = mp->brlensUni[1];
		isVPriorExp = NO;
		}
	else
		{
		minV = BRLENS_MIN;
		maxV = BRLENS_MAX;
		brlensExp = mp->brlensExp;
		isVPriorExp = YES;
		}

	topologyHasChanged = NO;

#	if defined (DEBUG_ExtTBR)
	printf ("Before:\n");
	ShowNodes (t->r, 2, NO);
	getchar();
#	endif
	
	/* pick an internal branch */
	do
		{
		p = t->intDownPass[(int)(RandomNumber(seed)*t->nIntNodes)];
		} while (p->anc->anc == NULL);
		
	/* set up pointers for nodes around the picked branch */
	/* cut the tree into crown, root and attachment part */
	/* change the relevant lengths in the attachment part */
	/* the lengths of a and v are automatically contained in the */
	/* "attachment" part but the length of c has to be stored in x */
	v = p;
	u = p->anc;

	/* set up pointers for crown part */
	/* this also determines direction of move in crown part */
	if (RandomNumber(seed) < 0.5)
		{
		c = v->left;
		d = v->right;
		directionLeft = YES;
		}
	else
		{
		c = v->right;
		d = v->left;
		directionLeft = NO;
		}

	/* cut and reconnect crown part */
	c->anc = d;
	d->anc = c;
	
	/* record c length and adjust with multiplier using reflection */
	m = c->length;
	x = c->length * exp(tuning * (RandomNumber(seed) - 0.5));		/* save the modified dangling branch for later use */
	while (x < minV || x > maxV)
		{
		if (x < minV)
			x = minV * minV / x;
		else if (x > maxV)
			x = maxV * maxV / x;
		}
	
	/* calculate proposal and prior ratio based on length modification */
	(*lnProposalRatio) = log (x / m);
	if (isVPriorExp == YES)
		(*lnPriorRatio) = brlensExp * (m - x);

	/* record v length and adjust with multiplier using reflection*/
	m = v->length;
	v->length *= exp(tuning * (RandomNumber(seed) - 0.5));
	while (v->length < minV || v->length > maxV)
		{
		if (v->length < minV)
			v->length = minV * minV / v->length;
		else if (v->length > maxV)
			v->length = maxV * maxV / v->length;
		}
	v->upDateTi = YES;

	/* adjust proposal and prior ratio based on length modification */
	(*lnProposalRatio) += log (v->length / m);
	if (isVPriorExp == YES)
		(*lnPriorRatio) += brlensExp * (m - v->length);

	/* mark nodes in root part */
	/* also determines direction of move in root part */
	if (RandomNumber(seed) < 0.5)
		{
		if (u->left == v)
			a = u->right;
		else
			a = u->left;
		b = u->anc;
		directionUp = YES;
		}
	else
		{
		if (u->left == v)
			b = u->right;
		else
			b = u->left;
		a = u->anc;
		directionUp = NO;
		}

	/* cut root part*/
	/* store branch to be modified in u->length */
	if (directionUp == NO) 
		{
		b->anc = a;
		if (a->left == u)
			a->left = b;
		else
			a->right = b;
		}
	else 
		{
		a->anc = b;
		if (b->left == u)
			b->left = a;
		else
			b->right = a;
		y = a->length;
		a->length = u->length;
		u->length = y;
		a->upDateTi = YES;
		}

	/* adjust length of branch to be modified */
	/* if it is not the root branch of a rooted tree */
	if (t->isRooted == NO || u->anc->anc != NULL) 
		{
		m = u->length;
		u->length *= exp(tuning * (RandomNumber(seed) - 0.5));
		while (u->length < minV || u->length > maxV)
			{
			if (u->length < minV)
				u->length = minV * minV / u->length;
			else if (u->length > maxV)
				u->length = maxV * maxV / u->length;
			}

		/* adjust proposal and prior ratio based on length modification */
		(*lnProposalRatio) += log (u->length / m);
		if (isVPriorExp == YES)
			(*lnPriorRatio) += brlensExp * (m - u->length);
		}
	u->upDateTi = YES;
		
	/* adjust proposal ratio for backward move in root subtree
	   if starting from interior, unconstrained branch
	   double test needed to capture the case of no move */
	if (directionUp == NO)
		{
		if (b->left != NULL && b->isLocked == NO &&
			a->anc  != NULL && u->isLocked == NO)
			(*lnProposalRatio) += log(1.0 - extensionProb);
		}
	else
		{
		if (a->left != NULL && a->isLocked == NO &&
			b->anc  != NULL && b->isLocked == NO)
			(*lnProposalRatio) += log(1.0 - extensionProb);
		}

	/* adjust proposal ratio for backward move in crown subtree
	   if starting from interior, unconstrained branch
	   double test is needed to capture the case of no move */
	if (c->left != NULL && c->isLocked == NO && 
		d->left != NULL && d->isLocked == NO)
		(*lnProposalRatio) += log(1.0 - extensionProb);

	/* move around in root subtree */
	for (nRootNodes=0; RandomNumber(seed)<extensionProb; nRootNodes++) 
		{
		if (directionUp == YES) 
			{	/* going up tree */
			if (a->left == NULL || a->isLocked == YES)
				break;		/* can't go further */
			topologyHasChanged = YES;
			b = a;
			if (RandomNumber(seed) < 0.5)
				a = a->left;
			else
				a = a->right;
			if (u->isLocked == YES)
				{
				b->isLocked = YES;
				u->isLocked = NO;
				b->lockID = u->lockID;
				u->lockID = 0;
				}
			}
		else 
			{	/* going down tree */
			if (a->anc == NULL || u->isLocked == YES)
				break;		/* can't go further */
			topologyHasChanged = YES;
			if (RandomNumber(seed)<0.5) 
				{
				directionUp = YES; /* switch direction */
				/* find sister of a */
				if (a->left == b) 
					{
					b = a;
					a = a->right;
					}
				else 
					{  
					b = a;
					a = a->left;
					}
				/* as long as we are moving upwards
				the cond likes to update will be
				flagged by the last pass from u to the root */
				}	
			else 
				{	/* continue down */
				b = a;
				a = a->anc;
				b->upDateCl = YES; 
				if (b->isLocked == YES)
					{
					u->isLocked = YES;
					b->isLocked = NO;
					u->lockID = b->lockID;
					b->lockID = 0;
					}
				}
			}
		}

	/* adjust proposal ratio for forward move if stop branch is interior & unconstrained
	   test of both ends makes sure that no adjustment is made if no move was made */
	if (directionUp == YES) 
		{
		if (a->left != NULL && a->isLocked == NO &&
			b->anc  != NULL && b->isLocked == NO) 
			(*lnProposalRatio) -= log(1.0 - extensionProb);
		}
	else 
		{
		if (a->anc  != NULL && u->isLocked == NO &&
			b->left != NULL && b->isLocked == NO)
			(*lnProposalRatio) -= log(1.0 - extensionProb);
		}

	/* move around in crown subtree */
	for (nCrownNodes=0; RandomNumber(seed)<extensionProb; nCrownNodes++) 
		{
		if (c->left == NULL || c->isLocked == YES)
			break;	/* can't go further */
		topologyHasChanged = YES;
		if (RandomNumber(seed) < 0.5) 
			{
			/* rotate c anticlockwise - prepare pointers for move left */
			c->anc = c->left;  /* the root will be in the direction we are heading */
			c->left = c->right;
			c->right = d;
			}
		else 
			{
			/* rotate c clockwise - prepare pointers for move right */
			c->anc = c->right;	/* the root will be in the direction we are heading */
			c->right = c->left;
			c->left = d;  
			}
		/* OK - let's move!; c->anc points in the right direction
		don't forget to move the branch lengths as well */
		d = c;
		c = c->anc;
		d->length = c->length;
		d->upDateCl = YES; 
		d->upDateTi = YES;
		}

	/* adjust proposal ratio for forward move if stop branch is interior & unconstrained
	   double test makes sure that no adjustment is made if no move was made */
	if (c->left != NULL && c->isLocked == NO &&
		d->left != NULL && d->isLocked == NO)
		(*lnProposalRatio) -= log(1.0 - extensionProb);

	/* combine the subtrees */
	c->anc = v;
	d->anc = v;
	if (directionLeft == YES) 
		{
		v->left = c;
		v->right = d;
		}
	else 
		{
		v->left = d;
		v->right = c;
		}

	/* the dangling branch is inserted in reverted position
	   such that the back move will be possible
	   if we have moved around in crown subtree
	   otherwise it is left in its original position */
	if (nCrownNodes > 0)
		{
		d->length = x;
		d->upDateTi = YES;
		}
	else
		{
		c->length = x;
		c->upDateTi = YES;
		}	

	if (directionUp == YES) 
		{
		u->anc = b;
		if (u->left == v)
			u->right = a;
		else 
			u->left = a;
		a->anc = u;
		if (b->left == a)
			b->left = u;
		else
			b->right = u;
		/* the dangling branch is contained in u->length
		   and will automatically be inserted in the right position
		   to enable the back move regardless of whether it was
		   initially directed upwards or downwards
		   BUT if we haven't moved in root subtree, it is advantageous (necessary
		   for rooted trees) to avoid switching branches, which occurs otherwise
		   if directionUp == YES */
		if (nRootNodes == 0) 
			{
			x = u->length;
			u->length = a->length;
			a->length = x;
			a->upDateTi = YES;
			u->upDateTi = NO;	/* u retains its old length */
			}
		}
	else 
		{
		u->anc = a;
		if (u->left == v)
			u->right = b;
		else 
			u->left = b;
		b->anc = u;
		if (a->left == b)
			a->left = u;
		else
			a->right = u;
		/* the modified branch contained in u->length will have
		   to be moved to b->length to enable back move
		   BUT if we haven't moved, it is better to keep it in place
		   (necessary for rooted trees) */
		if (nRootNodes > 0) 
			{
			x = u->length;
			u->length = b->length;
			b->length = x;
			b->upDateTi = YES;
			}
		}
		
	/* set flags for update of cond likes from v and down to root */
	p = v;
	while (p->anc != NULL)
		{
		p->upDateCl = YES;
		p = p->anc;
		}

	/* get down pass sequence if tree topology has changed */
	if (topologyHasChanged == YES)
		{
		GetDownPass (t);
		}

#	if defined (DEBUG_FTBR)
	printf ("After:\n");
	ShowNodes (t->root, 2, NO);
	getchar();
	printf ("Proposal ratio: %f\n",(*lnProposalRatio));
	printf ("v: %d  u: %d  c: %d  d: %d  a: %d  b: %d\n",v->index, u->index, 
		c->index, d->index, a->index, b->index);
	printf ("No. nodes moved in root subtree: %d\n",nRootNodes);
	printf ("No. nodes moved in crown subtree: %d\n",nCrownNodes);
	printf ("Has topology changed? %d\n",topologyHasChanged);
	getchar();
#	endif

#	if defined (TOPOLOGY_MOVE_STATS)
	if (topologyHasChanged == YES)
		gTopologyHasChanged = YES;
	else
		gTopologyHasChanged = NO;

	gNodeMoves = nCrownNodes + nRootNodes;
#	endif

#if defined DEBUG_CONSTRAINTS
	CheckConstraints (t);
#endif

	return (NO_ERROR);
	
}





int Move_GammaShape_M (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* change gamma shape parameter using multiplier */
	
	int			i, isAPriorExp, isValidA;
	MrBFlt		oldA, newA, minA, maxA, alphaExp=0.0, ran, factor, tuning, *gammaRates;
	ModelParams *mp;

	/* get tuning parameter */
	tuning = mvp[0];

	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* get minimum and maximum values for alpha */
	if (param->paramId == SHAPE_UNI)
		{
		minA = mp->shapeUni[0];
		maxA = mp->shapeUni[1];
		if (minA < MIN_SHAPE_PARAM)
			minA = MIN_SHAPE_PARAM;
		if (maxA > MAX_SHAPE_PARAM)
			maxA = MAX_SHAPE_PARAM;
		isAPriorExp = NO;
		}
	else
		{
		minA = MIN_SHAPE_PARAM;
		maxA = MAX_SHAPE_PARAM;
		alphaExp = mp->shapeExp;
		isAPriorExp = YES;
		}

	/* get old value of alpha */
	oldA = *GetParamVals(param, chain, state[chain]);

	/* change value for alpha */
	ran = RandomNumber(seed);
	factor = exp(tuning * (ran - 0.5));
	newA = oldA * factor;

	/* check validity */
	isValidA = NO;
	do
		{
		if (newA < minA)
			newA = minA * minA / newA;
		else if (newA > maxA)
			newA = maxA * maxA / newA;
		else
			isValidA = YES;
		} while (isValidA == NO);

	/* get proposal ratio */
	*lnProposalRatio = log(newA / oldA);
	
	/* get prior ratio */
	if (isAPriorExp == NO)
		*lnPriorRatio = 0.0;
	else
		*lnPriorRatio = -alphaExp * (newA - oldA);
	
	/* copy new alpha value back */
	*GetParamVals(param, chain, state[chain]) = newA;
	
	/* now, update gamma category information */
	gammaRates = GetParamSubVals (param, chain, state[chain]);
	if (DiscreteGamma (gammaRates, newA, newA, mp->numGammaCats, 0) == ERROR)
		return (ERROR);

	/* Set update flags for all partitions that share this alpha. Note that the conditional 
	   likelihood update flags have been set before we even call this function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));
		
	/* We need to update flags when we have a covarion model */
	for (i=0; i<param->nRelParts; i++)
		if (modelSettings[param->relParts[i]].nCijkParts > 1)
			modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);

}





int Move_Growth (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	int			isGPriorExp, isGPriorNorm, isValidG;
	MrBFlt			oldG, newG, window, minG=0.0, maxG=0.0, growthExp=0.0, ran, oldLnPrior, 
                                newLnPrior, curTheta, growthMu=0.0, growthVar=0.0, x, y;
	ModelParams 	        *mp;
	ModelInfo		*m;
	Tree			*t;

	/* get size of window, centered on current growth value */
	window = mvp[0];

	/* get model params */
	m = &modelSettings[param->relParts[0]];
	mp = &modelParams[param->relParts[0]];
	curTheta = *(GetParamVals (m->theta, chain, state[chain]));
	
	/* get minimum and maximum values for theta */
	isGPriorExp = isGPriorNorm = NO;
	if (param->paramId == GROWTH_UNI)
		{
		minG = mp->growthUni[0];
		maxG = mp->growthUni[1];
		}
	else if (param->paramId == GROWTH_EXP)
		{
		minG = 0.0;
		maxG = GROWTH_MAX;
		growthExp = mp->growthExp;
		isGPriorExp = YES;
		}
	else if (param->paramId == GROWTH_NORMAL)
		{
		minG = GROWTH_MIN;
		maxG = GROWTH_MAX;
		growthMu  = mp->growthNorm[0];
		growthVar = mp->growthNorm[1];
		isGPriorNorm = YES;
		}

	/* get old value of theta */
	newG = oldG = *GetParamVals(param, chain, state[chain]);
	if (newG < minG)
		newG = oldG = minG;

	/* change value for theta */
	ran = RandomNumber(seed);
	newG = oldG + window * (ran - 0.5);
	
	/* check that new value is valid */
	isValidG = NO;
	do
		{
		if (newG < minG)
			newG = 2* minG - newG;
		else if (newG > maxG)
			newG = 2 * maxG - newG;
		else
			isValidG = YES;
		} while (isValidG == NO);

	/* get proposal ratio */
	*lnProposalRatio = 0.0;
	
	/* get prior ratio */
	t = GetTree(modelSettings[param->relParts[0]].brlens,chain,state[chain]);
	if (LnCoalescencePriorPr (t, &oldLnPrior, curTheta, oldG) == ERROR)
		{
		MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
		return (ERROR);
		}
	if (LnCoalescencePriorPr (t, &newLnPrior, curTheta, newG) == ERROR)
		{
		MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
		return (ERROR);
		}
	if (isGPriorExp == NO && isGPriorNorm == NO)
		*lnPriorRatio = newLnPrior - oldLnPrior;
	else if (isGPriorExp == YES)
		*lnPriorRatio = -growthExp * (newG - oldG) + (newLnPrior - oldLnPrior);
	else if (isGPriorNorm == YES)
		{
		x = log(1.0) - log(growthVar) - 0.5 * log(2.0 * 3.14) - 0.5 * ((newG - growthMu) / growthVar) * ((newG - growthMu) / growthVar);
		y = log(1.0) - log(growthVar) - 0.5 * log(2.0 * 3.14) - 0.5 * ((oldG - growthMu) / growthVar) * ((oldG - growthMu) / growthVar);
		*lnPriorRatio = x - y;
		}
				
	/* copy new theta value back */
	*GetParamVals(param, chain, state[chain]) = newG;

	return (NO_ERROR);

}





/*----------------------------------------------------------------
|
|	Move_Local: This proposal mechanism changes the topology and
|      branch lengths of an unrooted tree using the LOCAL mech-
|      anism described by Larget & Simon (1999):
|
|      Larget, B. L., and D. L. Simon. 1999. Markov chain 
|         Monte Carlo algorithms for the Bayesian analysis 
|         of phylogenetic trees. Molecular Biology and 
|         Evolution 16:750-759.
|
|      Programmed by FR 2001-10-14 and partly rewritten 2002-02-21
|      for more elegance and the ability to deal with rooted trees.
|	   Support for locked nodes added 2004-01-12 based on mb v2.01.
|	   Calculation of the Hastings ratio corrected 2004-07-01.
|	   Boundary conditions correctly taken care of 2004-09-29.
|	   NB! An alternative to reflection is to skip moves, which might
|          be better for the LOCAL given the complexity of taking
|		   the boundary conditions into account
|
----------------------------------------------------------------*/
int Move_Local (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{
	
	int			topologyHasChanged, isVPriorExp, directionUp, moveX;
	MrBFlt		oldM, newM, x, y, newX, newY,
				tuning, minV, maxV, brlensExp=0.0;
	TreeNode	*v, *u, *a, *b, *c, *d;
	Tree		*t;
	ModelParams *mp;

	tuning = mvp[0]; /* Larget & Simon's tuning parameter lambda */
	
	/* get tree */
	t = GetTree (param, chain, state[chain]);
#if defined DEBUG_CONSTRAINTS
	CheckConstraints (t);
#endif

	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* max and min brlen */
	if (param->subParams[0]->paramId == BRLENS_UNI)
		{
		minV = mp->brlensUni[0] > BRLENS_MIN ? mp->brlensUni[0] : BRLENS_MIN;
		maxV = mp->brlensUni[1];
		isVPriorExp = NO;
		}
	else
		{
		minV = BRLENS_MIN;
		maxV = BRLENS_MAX;
		brlensExp = mp->brlensExp;
		isVPriorExp = YES;
		}

	topologyHasChanged = NO;

#	if defined (DEBUG_LOCAL)
	printf ("Before:\n");
	ShowNodes (t->root, 2, NO);
	getchar();
#	endif
	
	/* pick an internal branch */
	do
		{
		v = t->intDownPass[(int)(RandomNumber(seed)*t->nIntNodes)];
		} while (v->anc->anc == NULL);
		
	/* set up pointers for crown part */
	if (RandomNumber(seed) < 0.5)
		{
		c = v->left;
		d = v->right;
		}
	else
		{
		c = v->right;
		d = v->left;
		}

	/* set up pointers for root part */
	u = v->anc;
	if (RandomNumber(seed) < 0.5 || (t->isRooted == YES && u->anc->anc == NULL))
		{
		directionUp = YES;
		if (u->left == v)
			a = u->right;
		else
			a = u->left;
		b = u->anc;
		}
	else
		{
		directionUp = NO;
		if (u->left == v)
			b = u->right;
		else
			b = u->left;
		a = u->anc;
		}

	/* store old and new path length as well as old x and y */
	oldM = c->length + v->length;
	if (directionUp == YES)
		{
		oldM += a->length;
		x = a->length;
		}
	else
		{
		oldM += u->length;
		x = u->length;
		}

	y = x + v->length;

	/* pick dangly to move */
	if (RandomNumber(seed) < 0.5)
		moveX = YES;
	else
		moveX = NO;

	/* find new m value */
	newM = exp(tuning * (RandomNumber(seed) - 0.5)) * oldM;

	/* pick dangly to move and
	   pick new attachment point */
	if (moveX == YES)
		{
		/* choose new x */

		/* first update y */
		newY = y * (newM / oldM);

		/* find reinsertion point */
		if (v->isLocked == YES) 
			{
			newX = RandomNumber(seed) *  newY;
			}
		else
			{
			newX = RandomNumber(seed) * newM;
			}
		}
	else
		{
		/* choose new y */

		/* first update x */
		newX = x * (newM / oldM);

		/* find reinsertion point */
		if (v->isLocked == YES)
			{
			newY = RandomNumber(seed) * newM - newX;
			}
		else
			{
			newY = RandomNumber(seed) * newM;
			}
		}

	/* adjust proposal and prior ratio based on length modification */
	/* and insertion mechanism */	
	(*lnProposalRatio) += 3.0 * log (newM / oldM);
	if (isVPriorExp == YES)
		(*lnPriorRatio) = brlensExp * (oldM - newM);

	/* make topology move if necessary and then set branch lengths */
	if (newX > newY)
		{
		/* check if we need to abort */
		if (newY < minV || newY > maxV || (newX-newY) < minV || (newX-newY) > maxV || (newM-newX) < minV || (newM-newX) > maxV)
			{
			abortMove = YES;
			return NO_ERROR;
			}

		/* topology has changed */
		topologyHasChanged = YES;
		/* detach v and d */
		/* this scheme differs from that used by Larget and Simon but is more
		   convenient because it avoids tree rotations */
		if (u->left == v)
			u->left = c;
		else
			u->right = c;
		c->anc = u;
		if (directionUp == YES)
			{
			/* place v and d below a */
			if (v->left == d)
				v->right = a;
			else
				v->left = a;
			a->anc = v;
			if (u->left == a)
				u->left = v;
			else
				u->right = v;
			/* v->anc is already u */
			/* adjust lengths */
			c->length = newM - newX;
			v->length = newX - newY;
			a->length = newY;
			}
		else
			{
			/* place v and d below u */
			if (u->isLocked == YES)
				{
				v->isLocked = YES;
				u->isLocked = NO;
				v->lockID = u->lockID;
				u->lockID = 0;
				}
			if (v->left == d)
				v->right = u;
			else
				v->left = u;
			u->anc = v;
			v->anc = a;
			if (a->left == u)
				a->left = v;
			else
				a->right = v;
			/* adjust lengths */
			c->length = newM - newX;
			u->length = newX - newY;
			v->length = newY;
			}
		}
	else
		{
		/* check if we need to abort */
		if (newX < minV || newX > maxV || (newY-newX) < minV || (newY-newX) > maxV || (newM-newY) < minV || (newM-newY) > maxV)
			{
			abortMove = YES;
			return NO_ERROR;
			}

		/* topology has not changed */
		c->length = newM - newY;
		v->length = newY - newX;
		if (directionUp == YES)
			a->length = newX;
		else
			u->length = newX;
		}
				
	/* set update of transition probs */
	c->upDateTi = YES;
	v->upDateTi = YES;
	if (directionUp == YES)
		a->upDateTi = YES;
	else
		u->upDateTi = YES;
		
	/* set flags for update of cond likes from v and u down to root */
	v->upDateCl = YES; 
 	u->upDateCl = YES; 
	if (directionUp == YES)
		v = b;
	else
		v = a;
	while (v->anc != NULL)
		{
		v->upDateCl = YES; 
		v = v->anc;
		}

	/* get downpass sequence if tree topology has changed */
	if (topologyHasChanged == YES)
		{
		GetDownPass (t);
		}
		
#	if defined (DEBUG_LOCAL)
	printf ("After:\n");
	ShowNodes (t->root, 2, NO);
	getchar();
	printf ("Proposal ratio: %f\n",(*lnProposalRatio));
	printf ("v: %d  u: %d  c: %d  d: %d  a: %d  b: %d\n",v->index, u->index, 
		c->index, d->index, a->index, b->index);
	printf ("Has topology changed? %d\n",topologyHasChanged);
	getchar();
#	endif

#	if defined (TOPOLOGY_MOVE_STATS)
	if (topologyHasChanged == YES)
		gTopologyHasChanged = YES;
	else
		gTopologyHasChanged = NO;
#	endif

#if defined DEBUG_CONSTRAINTS
	CheckConstraints(t);
#endif

	return (NO_ERROR);
	
}





/*----------------------------------------------------------------
|
|	Move_LocalClock: This proposal mechanism changes the topology and
|      branch lengths of a rooted tree using the LOCAL (clock) mech-
|      anism described by Larget & Simon (1999):
|
|      Larget, B. L., and D. L. Simon. 1999. Markov chain 
|         Monte Carlo algorithms for the Bayesian analysis 
|         of phylogenetic trees. Molecular Biology and 
|         Evolution 16:750-759.
|
|      Programmed by JH 2002-07-07
|	   Modified by FR 2004-05-22 to handle locked and dated
|			trees
|
----------------------------------------------------------------*/
int Move_LocalClock (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{
	
	int		topologyHasChanged, vIsRoot, aSister, bSister, cSister;
	MrBFlt		x, y, tuning, minV, h1, h2, h3, h[3], hPrime[3], tempD, ran, distUv, distCv,
			sR=0.0, eR=0.0, sF=0.0, oldLnPrior, newLnPrior, theta=0.0, growth=0.0;
	TreeNode	*u, *v, *w=NULL, *a, *b, *c, *deepestChild, *p;
	Tree		*t;
	ModelParams	 *mp;
	ModelInfo	*m;

	/* tuning parameter ("lambda" in Larget and Simon, 1999) */
	tuning = mvp[0];
	
	/* get tree */
	t = GetTree (param, chain, state[chain]);

	/* get model params */
	mp = &modelParams[param->relParts[0]];
			
	/* min brlen */
	minV = BRLENS_MIN;

#if defined (DEBUG_LOCAL)
	/* check branch lengths and node depths */
	for (i=0; i<t->nNodes-2; i++) {
		p = t->allDownPass[i];
		if (p->length < minV) {
			printf ("%s   ERROR when entering LocalClock: node %d has length %lf", spacer, p->index, p->length);
			return ERROR;
		}
		if (p->nodeDepth >= p->anc->nodeDepth) {
			printf ("%s   ERROR when entering LocalClock: node %d has depth %lf larger than its ancestor %d depth %lf", spacer, p->index, p->nodeDepth, p->anc->index, p->anc->nodeDepth);
			return ERROR;
		}
	}
#endif

	/* calculate prior ratio (part 1/2) */
	if (param->subParams[0]->paramId == BRLENS_CLOCK_UNI ||
		param->subParams[0]->paramId == BRLENS_CCLOCK_UNI)
		{
		/* uniformly distributed branch lengths */
		if (!strcmp(mp->treeHeightPr, "Exponential"))
			oldLnPrior = log(mp->treeHeightExp) - mp->treeHeightExp * t->root->left->nodeDepth;
		else
			oldLnPrior = mp->treeHeightGamma[1] * log(mp->treeHeightGamma[0]) - LnGamma(mp->treeHeightGamma[1]) + (mp->treeHeightGamma[1] - 1.0) * log(t->root->left->nodeDepth) - mp->treeHeightGamma[0] * t->root->left->nodeDepth;
		}
	else if (param->subParams[0]->paramId == BRLENS_CLOCK_COAL ||
			 param->subParams[0]->paramId == BRLENS_CCLOCK_COAL)
		{
		/* coalescence prior */
		m = &modelSettings[param->relParts[0]];
		theta = *(GetParamVals (m->theta, chain, state[chain]));
		if (!strcmp(mp->growthPr, "Fixed"))
			growth = mp->growthFix;
		else
			growth = *(GetParamVals (m->growthRate, chain, state[chain]));
		if (LnCoalescencePriorPr (t, &oldLnPrior, theta, growth) == ERROR)
			{
			MrBayesPrint ("%s   Problem calculating prior for coalescence process\n", spacer);
			return (ERROR);
			}
		}
	else
		{
		/* birth-death prior */
		m = &modelSettings[param->relParts[0]];
		sR = *(GetParamVals (m->speciationRates, chain, state[chain]));
		eR = *(GetParamVals (m->extinctionRates, chain, state[chain]));
		sF = mp->sampleProb;
		if (LnBirthDeathPriorPr (t, &oldLnPrior, sR, eR, sF) == ERROR)
			{
			MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
			return (ERROR);
			}
		if (!strcmp(mp->treeHeightPr, "Exponential"))
			oldLnPrior += log(mp->treeHeightExp) - mp->treeHeightExp * t->root->left->nodeDepth;
		else
			oldLnPrior += mp->treeHeightGamma[1] * log(mp->treeHeightGamma[0]) - LnGamma(mp->treeHeightGamma[1]) + (mp->treeHeightGamma[1] - 1.0) * log(t->root->left->nodeDepth) - mp->treeHeightGamma[0] * t->root->left->nodeDepth;
		}

	topologyHasChanged = NO;

#	if defined (DEBUG_LOCAL)
	printf ("Before:\n");
	ShowNodes (t->root, 2, YES);
#	endif

	/* set up pointers */
	do
		{
		u = t->intDownPass[(int)(RandomNumber(seed)*t->nIntNodes)];
		} while (u->anc->anc == NULL);
	v = u->anc;
	a = u->left;
	b = u->right;
	if (v->left == u)
		c = v->right;
	else
		c = v->left;
	vIsRoot = NO;
	if (v->anc->anc != NULL)
		w = v->anc;
	else
		vIsRoot = YES;
		
	/* get distances from root of move (w or v) to nodes a, b, and c */
	if (vIsRoot == NO)
		h1 = h2 = h3 = v->length;
	else
		h1 = h2 = h3 = 0.0;
	h1 += u->length + a->length;
	h2 += u->length + b->length;
	h3 += c->length;
	h[0] = h1;
	h[1] = h2;
	h[2] = h3;
	
	/* we also need the distances between u <-> v and c <-> v to calculate the hastings' term */
	distUv = u->length;
	distCv = c->length;
		
	/* sort distances (simply make three comparisons and swap values, if necessary) */
	if (h[0] > h[1])
		{
		tempD = h[1];
		h[1] = h[0];
		h[0] = tempD;
		}
	if (h[0] > h[2])
		{
		tempD = h[2];
		h[2] = h[0];
		h[0] = tempD;
		}
	if (h[1] > h[2])
		{
		tempD = h[2];
		h[2] = h[1];
		h[1] = tempD;
		}
	hPrime[0] = h[0];
	hPrime[1] = h[1];
	hPrime[2] = h[2];
		
	/* Find the child node (a, b, or c) that is closest to the root (i.e., has smallest h_i; i=1,2,3). This
	   part deals with the possibility that some of the nodes are at the same nodeDepth and randomly assigns
	   a node to be deepest in case of ties. */
	if (AreDoublesEqual (h1, h2, 0.00000001) == YES && AreDoublesEqual (h1, h3, 0.00000001) == YES && AreDoublesEqual (h2, h3, 0.00000001) == YES)
		{
		ran = RandomNumber(seed);
		if (ran < 0.33333333)
			deepestChild = a;
		else if (ran > 0.66666666)
			deepestChild = b;
		else
			deepestChild = c;
		}
	else if (AreDoublesEqual (h1, h2, 0.00000001) == YES && AreDoublesEqual (h1, h3, 0.00000001) == NO && AreDoublesEqual (h2, h3, 0.00000001) == NO)
		{
		if (h1 < h3)
			{
			ran = RandomNumber(seed);
			if (ran < 0.5)
				deepestChild = a;
			else
				deepestChild = b;
			}
		else
			deepestChild = c;
		}
	else if (AreDoublesEqual (h1, h2, 0.00000001) == NO && AreDoublesEqual (h1, h3, 0.00000001) == YES && AreDoublesEqual (h2, h3, 0.00000001) == NO)
		{
		if (h1 < h2)
			{
			ran = RandomNumber(seed);
			if (ran < 0.5)
				deepestChild = a;
			else
				deepestChild = c;
			}
		else
			deepestChild = b;
		}
	else if (AreDoublesEqual (h1, h2, 0.00000001) == NO && AreDoublesEqual (h1, h3, 0.00000001) == NO && AreDoublesEqual (h2, h3, 0.00000001) == YES)
		{
		if (h2 < h1)
			{
			ran = RandomNumber(seed);
			if (ran < 0.5)
				deepestChild = b;
			else
				deepestChild = c;
			}
		else
			deepestChild = a;
		}
	else
		{
		if (h1 < h2 && h1 < h3)
			deepestChild = a;
		else if (h2 < h1 && h2 < h3)
			deepestChild = b;
		else
			deepestChild = c;
		}
	
	/* get x and y */
	/* for most of the branches, the proposal ratio is 0.0 so it makes sense to set this first */
	(*lnProposalRatio) = 0.0;
	if (u->isDated == YES && v->isDated == YES)
		{
		/* this proposal is wasted, change nothing */
		if (vIsRoot == NO)
			{
			y = v->length;
			x = y + u->length;
			}
		else
			{
			y = 0.0;
			x = u->length;
			}
		}
	else if (u->isDated == YES && v->isDated == NO)
		{
		/* we can only change the position of v */
		if (vIsRoot == NO)
			{
			/* the upper limit of v's height is determined either by u-length + v->length or by c->length + v->length (h[0]) */
			x = v->length + u->length;
			if (x > h[0])
				x = h[0];
			do {
				y = RandomNumber(seed) * x;
			} while (y < minV || (x - y) < minV);
			}
		else
			{
			/* the upper limit of v's height is determined either by u-length + v->length or by c (h[0]) */
			if (u->length > c->length)
				{
				do {
					x = h[0] * exp(tuning * (RandomNumber(seed) - 0.5));
				} while (x < minV);
				v->nodeDepth = c->nodeDepth + x;
				(*lnProposalRatio) = log(x / h[0]);
				}
			else
				{
				do {
					x = u->length * exp(tuning * (RandomNumber(seed) - 0.5));
				} while (x < minV);
				v->nodeDepth = u->nodeDepth + x;
				(*lnProposalRatio) = log(x / u->length);
				}
			y = 0.0;
			}
		}
	else if (u->isDated == NO && v->isDated == YES)
		{
		/* we can only change the position of u */
		if (vIsRoot == NO)
			y = v->length;
		else
			y = 0.0;
		if (u->isLocked == YES)
			{
			if (h1 > h2)
				{
				do {
					x = y + RandomNumber(seed) * (h2 - y);
				} while ((h2 - x) < minV || (x - y) < minV);
				}
			else
				{
				do {
					x = y + RandomNumber(seed) * (h1 - y);
				} while ((h1 - x) < minV || (x - y) < minV);
				}
			}
		else
			{
			do {
				x = y + RandomNumber(seed) * (h[1] - y);
			} while ((h[1] - x) < minV || (x - y) < minV);
			}
		}
	/* if we reach the statements down here, neither u nor v is dated */
	else if (u->isLocked == YES)
		{
		if (h1 > h2)
			{
			do {
				y = RandomNumber(seed) * h[0];
				x = y + RandomNumber(seed) * (h2 - y);
			} while (y < minV || (x - y) < minV || (h2 - x) < minV);
			}
		else
			{
			do {
				y = RandomNumber(seed) * h[0];
				x = y + RandomNumber(seed) * (h1 - y);
			} while (y < minV || (x - y) < minV || (h1 - x) < minV);
			}
		}
	else if (vIsRoot == NO)
		{
		/* this is the standard variant for nonroot v */
		do {
			x = RandomNumber(seed) * h[1];
			y = RandomNumber(seed) * h[0];
		} while (y < minV || x < minV || fabs(x - y) < minV || (h[0] - y) < minV || (h[1] - x) < minV);
		}
	else
		{
		/* this is the standard variant when v is the root */
		do {
			hPrime[0] = h[0] * exp(tuning * (RandomNumber(seed) - 0.5));
		} while (hPrime[0] < minV);
		hPrime[1] = h[1] + hPrime[0] - h[0];
		hPrime[2] = h[2] + hPrime[0] - h[0];
		v->nodeDepth = deepestChild->nodeDepth + hPrime[0]; /* adjust depth of the root node, if v is the root */
		y = 0.0;
		do {
			x = RandomNumber(seed) * hPrime[1];
		} while (x < minV || (hPrime[1] - x) < minV);
		(*lnProposalRatio) += log(hPrime[0] / h[0]);
		}	
		
	/* decide which topology we will construct (cSister is what we started with) */
	aSister = bSister = cSister = NO;
	/* if u is locked then we cannot change topology */
	if (u->isLocked == YES)
		cSister = YES;
	else if (MaximumValue (x, y) < (hPrime[0] - minV))
		{
		ran = RandomNumber(seed);
		if (ran < 0.33333333)
			aSister = YES;
		else if (ran > 0.66666666)
			bSister = YES;
		else 
			cSister = YES;
		}
	else
		{
		if (deepestChild == a)
			aSister = YES;
		else if (deepestChild == b)
			bSister = YES;
		else 
			cSister = YES;
		}
	
	/* adjust lengths of nodes u and v */
	u->length = MaximumValue (x, y) - MinimumValue (x, y);
	v->length = MinimumValue (x, y);
	if (vIsRoot == NO)
		v->nodeDepth = w->nodeDepth - v->length;
	u->nodeDepth = v->nodeDepth - u->length;
	
	/* adjust pointers and lengths of nodes a, b, and c */
	topologyHasChanged = NO;
	if (cSister == YES)
		{
		if (v->left == u)
			v->right = c;
		else
			v->left = c;
		u->left = a;
		u->right = b;
		a->anc = b->anc = u;
		c->anc = v;
		a->length = u->nodeDepth - a->nodeDepth;
		b->length = u->nodeDepth - b->nodeDepth;
		c->length = v->nodeDepth - c->nodeDepth;
		}
	else if (bSister == YES)
		{
		if (v->left == u)
			v->right = b;
		else
			v->left = b;
		u->left = a;
		u->right = c;
		a->anc = c->anc = u;
		b->anc = v;
		a->length = u->nodeDepth - a->nodeDepth;
		b->length = v->nodeDepth - b->nodeDepth;
		c->length = u->nodeDepth - c->nodeDepth;
		topologyHasChanged = YES;
		}
	else
		{
		if (v->left == u)
			v->right = a;
		else
			v->left = a;
		u->left = b;
		u->right = c;
		b->anc = c->anc = u;
		a->anc = v;
		a->length = v->nodeDepth - a->nodeDepth;
		b->length = u->nodeDepth - b->nodeDepth;
		c->length = u->nodeDepth - c->nodeDepth;
		topologyHasChanged = YES;
		}

#if		0		/* check that the local produces good branchlengths */
	if (a->length < 0.0 || b->length < 0.0 || c->length < 0.0 || u->length < 0.0 || v->length < 0.0) {
		printf ("Error\n");
		getchar();
	}
#endif

	/* calculate the proposal ratio due to asymmetric topology changes */
	if (u->isLocked == NO)
		{
		if (distUv > distCv && MaximumValue (x, y) < hPrime[0])
			(*lnProposalRatio) += log(3.0);
		else if (distUv < distCv && MaximumValue (x, y) > hPrime[0])
			(*lnProposalRatio) += log(1.0 / 3.0);
		}

	/* set update of transition probs */
	a->upDateTi = b->upDateTi = c->upDateTi = u->upDateTi = YES;
	if (vIsRoot == NO)
		v->upDateTi = YES;

	/* set flags for update of cond likes from u down to root */
	p = u;
	while (p->anc != NULL)
		{
		p->upDateCl = YES;
		p = p->anc;
		}
		
	/* get downpass sequence if tree topology has changed */
	if (topologyHasChanged == YES)
		GetDownPass (t);
		
	/* calculate prior ratio (part 2/2) */
	if (param->subParams[0]->paramId == BRLENS_CLOCK_UNI ||
		param->subParams[0]->paramId == BRLENS_CCLOCK_UNI)
		{
		/* uniformly distributed branch lengths */
		if (!strcmp(mp->treeHeightPr, "Exponential"))
			newLnPrior = log(mp->treeHeightExp) - mp->treeHeightExp * t->root->left->nodeDepth;
		else
			newLnPrior = mp->treeHeightGamma[1] * log(mp->treeHeightGamma[0]) - LnGamma(mp->treeHeightGamma[1]) + (mp->treeHeightGamma[1] - 1.0) * log(t->root->left->nodeDepth) - mp->treeHeightGamma[0] * t->root->left->nodeDepth;
		(*lnPriorRatio) = newLnPrior - oldLnPrior;
		}
	else if (param->subParams[0]->paramId == BRLENS_CLOCK_COAL ||
			 param->subParams[0]->paramId == BRLENS_CCLOCK_COAL)
		{
		/* coalescence prior */
		if (LnCoalescencePriorPr (t, &newLnPrior, theta, growth) == ERROR)
			{
			MrBayesPrint ("%s   Problem calculating prior for coalescence process\n", spacer);
			return (ERROR);
			}
		(*lnPriorRatio) = newLnPrior - oldLnPrior;
		}
	else
		{
		/* birth-death prior */
		if (LnBirthDeathPriorPr (t, &newLnPrior, sR, eR, sF) == ERROR)
			{
			MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
			return (ERROR);
			}
		if (!strcmp(mp->treeHeightPr, "Exponential"))
			newLnPrior += log(mp->treeHeightExp) - mp->treeHeightExp * t->root->left->nodeDepth;
		else
			newLnPrior += mp->treeHeightGamma[1] * log(mp->treeHeightGamma[0]) - LnGamma(mp->treeHeightGamma[1]) + (mp->treeHeightGamma[1] - 1.0) * log(t->root->left->nodeDepth) - mp->treeHeightGamma[0] * t->root->left->nodeDepth;
		(*lnPriorRatio) = newLnPrior - oldLnPrior;
		}
		
#	if defined (DEBUG_LOCAL)
	printf ("After:\n");
	ShowNodes (t->root, 2, YES);
	printf ("Has topology changed? %d\n",topologyHasChanged);
#	endif
	
#	if defined (TOPOLOGY_MOVE_STATS)
	if (topologyHasChanged == YES)
		gTopologyHasChanged = YES;
	else
		gTopologyHasChanged = NO;
#	endif

#if defined (DEBUG_LOCAL)
	/* check branch lengths and node depths */
	for (i=0; i<t->nNodes-2; i++) {
		p = t->allDownPass[i];
		if (p->length < minV) {
			printf ("%s   ERROR when leaving LocalClock: node %d has length %lf", spacer, p->index, p->length);
			return ERROR;
		}
		if (p->nodeDepth >= p->anc->nodeDepth) {
			printf ("%s   ERROR when leaving LocalClock: node %d has depth %lf larger than its ancestor %d depth %lf", spacer, p->index, p->nodeDepth, p->anc->index, p->anc->nodeDepth);
			return ERROR;
		}
	}
#endif

	return (NO_ERROR);
	
}





/* change topology using NNI */
int Move_NNI (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	int			i;
	TreeNode	*p, *u, *v, *a, *b, *c;
	Tree		*t;
	
	/* no warnings */
	(*lnPriorRatio) = (*lnProposalRatio) = 0.0;
	i = (int)mvp[0];
		
	/* get tree */
	t = GetTree (param, chain, state[chain]);

	/* pick an internal branch */
	do
		{
		p = t->intDownPass[(int)(RandomNumber(seed)*t->nIntNodes)];
		} while (p->anc->anc == NULL);
		
	/* set up area of rearrangement */
	u = p;
	v = u->anc;
	a = u->left;
	b = u->right;
	if (v->left == u)
		c = v->right;
	else
		c = v->left;
		
	/* change topology */
	if (RandomNumber(seed) < 0.5)
		{
		if (v->left == u)
			v->right = b;
		else
			v->left = b;
		u->left = a;
		u->right = c;
		a->anc = c->anc = u;
		b->anc = v;
		}
	else
		{
		if (v->left == u)
			v->right = a;
		else
			v->left = a;
		u->left = b;
		u->right = c;
		b->anc = c->anc = u;
		a->anc = v;
		}

	/* set update of parsimony sets */
	while (p->anc != NULL)
		{
		p->upDateCl = YES; 
		p = p->anc;
		}
	
	GetDownPass(t);
	
	return (NO_ERROR);

}





/* change topology with unlinked brlens using NNI */
int Move_NNI_Hetero (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	int			i, isVPriorExp, isParsimony, brIndex, moveType;
	MrBFlt		tuning, minV=0.0, maxV=0.0, brlensExp=0.0, oldM, newM, f;
	TreeNode	*p, *u, *v, *a, *b, *c;
	Tree		*t;
	ModelParams *mp;
	
	tuning = mvp[0];
	(*lnPriorRatio) = (*lnProposalRatio) = 0.0;
		
	/* get first tree */
	t = GetTree (param, chain, state[chain]);

	/* pick an internal branch */
	do
		{
		brIndex = (int) (RandomNumber(seed) * t->nIntNodes);
		p = t->intDownPass[brIndex];
		} while (p->anc->anc == NULL);
		
	/* decide on how to change the tree */
	if (RandomNumber(seed) < (1.0/3.0))
		moveType = 0;
	else if (RandomNumber(seed) < (2.0/3.0))
		moveType = 1;
	else
		moveType = 2;
	
	/* cycle through trees */
	for (i=0; i<param->nSubParams; i++)
		{
		/* get model params */
		mp = &modelParams[param->relParts[0]];
				
		/* brlen model, max min brlen*/
		isVPriorExp = isParsimony = NO;
		if (param->subParams[0]->paramId == BRLENS_UNI)
			{
			maxV = mp->brlensUni[1];
			minV = mp->brlensUni[0] > BRLENS_MIN ? mp->brlensUni[0] : BRLENS_MIN;
			}
		else if (param->subParams[0]->paramId == BRLENS_EXP)
			{
			maxV = BRLENS_MAX;
			minV = BRLENS_MIN;
			brlensExp = mp->brlensExp;
			isVPriorExp = YES;
			}
		else if (param->subParams[0]->paramId == BRLENS_PARSIMONY)
			{
			maxV = minV = 0.0;
			isParsimony = YES;
			}

		/* get tree */
		t = GetTree (param->subParams[i], chain, state[chain]);

		/* find p */
		p = t->intDownPass[brIndex];

		/* set up area of rearrangement */
		u = p;
		v = u->anc;
		a = u->left;
		b = u->right;
		if (v->left == u)
			c = v->right;
		else
			c = v->left;
		
		/* adjust branch lengths */
		if (isParsimony == NO)
			{
			oldM = a->length;
			f = a->length;
			a->length *= exp (tuning * (RandomNumber(seed) - 0.5));
			while (a->length < minV || a->length > maxV)
				{
				if (a->length < minV)
					a->length = minV * minV / a->length;
				else if (a->length > maxV)
					a->length = maxV * maxV / a->length;
				}
			newM = a->length;
			(*lnProposalRatio) += log(a->length / f);
			
			oldM += b->length;
			f = b->length;
			b->length *= exp (tuning * (RandomNumber(seed) - 0.5));
			while (b->length < minV || b->length > maxV)
				{
				if (b->length < minV)
					b->length = minV * minV / b->length;
				else if (b->length > maxV)
					b->length = maxV * maxV / b->length;
				}
			newM += b->length;
			(*lnProposalRatio) += log(b->length / f);

			oldM += v->length;
			f = v->length;
			v->length *= exp (tuning * (RandomNumber(seed) - 0.5));
			while (v->length < minV || v->length > maxV)
				{
				if (v->length < minV)
					v->length = minV * minV / v->length;
				else if (v->length > maxV)
					v->length = maxV * maxV / v->length;
				}
			newM += v->length;
			(*lnProposalRatio) += log(v->length / f);

			oldM += u->length;
			f = u->length;
			u->length *= exp (tuning * (RandomNumber(seed) - 0.5));
			while (u->length < minV || u->length > maxV)
				{
				if (u->length < minV)
					u->length = minV * minV / u->length;
				else if (u->length > maxV)
					u->length = maxV * maxV / u->length;
				}
			newM += u->length;
			(*lnProposalRatio) += log(u->length / f);

			oldM += c->length;
			f = c->length;
			c->length *= exp (tuning * (RandomNumber(seed) - 0.5));
			while (c->length < minV || c->length > maxV)
				{
				if (c->length < minV)
					c->length = minV * minV / c->length;
				else if (c->length > maxV)
					c->length = maxV * maxV / c->length;
				}
			newM += c->length;
			(*lnProposalRatio) += log(c->length / f);

			if (isVPriorExp == YES)
				(*lnPriorRatio) += brlensExp * (oldM - newM);
			}

		/* change topology */
		if (moveType == 0)
			{
			if (v->left == u)
				v->right = b;
			else
				v->left = b;
			u->left = a;
			u->right = c;
			a->anc = c->anc = u;
			b->anc = v;
			}
		else if (moveType == 1)
			{
			if (v->left == u)
				v->right = a;
			else
				v->left = a;
			u->left = b;
			u->right = c;
			b->anc = c->anc = u;
			a->anc = v;
			}
		else
			{
			/* do not change topology */
			}

		/* set update of ti probs */
		a->upDateTi = YES;
		b->upDateTi = YES;
		c->upDateTi = YES;
		u->upDateTi = YES;
		v->upDateTi = YES;
		
		/* set update of conditional likelihoods */
		while (p->anc != NULL)
			{
			p->upDateCl = YES; 
			p = p->anc;
			}

		/* reset tree downpass sequences */
		GetDownPass(t);

		}
	
	return (NO_ERROR);

}





/*-----------------------------------------------------------------------------------
|
|	Move_NodeSlider: move the position of one node without changing topology
|
-------------------------------------------------------------------------------------*/

int Move_NodeSlider (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{
	MrBFlt		tuning, maxV, minV, oldM, newM, brlensPrExp=0.0, newMin, newMax, oldMin, oldMax;
	TreeNode	*p, *q;
	ModelParams *mp;
	Tree		*t;

	tuning = mvp[0]; /* Larget & Simon's tuning parameter lambda */

	mp = &modelParams[param->relParts[0]];

	/* max and min brlen */
	if (param->paramId == BRLENS_UNI)
		{
		minV = mp->brlensUni[0] > BRLENS_MIN ? mp->brlensUni[0] : BRLENS_MIN;
		maxV = mp->brlensUni[1];
		}
	else
		{
		minV = BRLENS_MIN;
		maxV = BRLENS_MAX;
		brlensPrExp = mp->brlensExp;
		}

	/* get tree */
	t = GetTree (param, chain, state[chain]);

	/* pick an interior branch */
	do
		{
		p = t->intDownPass[(int)(RandomNumber(seed)*t->nIntNodes)];
		} while (p->anc == NULL || (t->isRooted == YES && p->anc->anc == NULL));

	/* pick one descendant branch */
	if (RandomNumber(seed) < 0.5)
		q = p->left;
	else
		q = p->right;
	
	/* determine new length */
	oldM = (q->length + p->length);
	newM = oldM * exp(tuning * (RandomNumber(seed) - 0.5));
	while (newM < 2.0 * minV || newM > 2.0 * maxV)
		{
		if (newM < 2.0 * minV)
			newM = 4.0 * minV * minV / newM;
		else if (newM > 2.0 * maxV)
			newM = 4.0 * maxV * maxV / newM;
		}

	/* determine new lengths of p and q */
	newMin = minV > newM - maxV ? minV : newM - maxV;
	newMax = maxV < newM - minV ? maxV : newM - minV;
	oldMin = minV > oldM - maxV ? minV : oldM - maxV;
	oldMax = maxV > oldM - minV ? maxV : oldM - minV;

	q->length = newMin + RandomNumber(seed) * (newMax - newMin);
	p->length = newM - q->length;

	/* set flags for update of transition probabilities at p and q */
	p->upDateTi = YES;
	q->upDateTi = YES;
	p->upDateCl = YES;

	/* set flags for update of cond likes from p->anc and down to root */
	while (p->anc != NULL)
		{
		p = p->anc;
		p->upDateCl = YES;
		}

	/* calculate proposal ratio */
	(*lnProposalRatio) = log(newM / oldM) + log ((newMax - newMin) / (oldMax - oldMin));

	/* update prior if exponential prior on branch lengths */
	if (param->paramId == BRLENS_EXP)
		(*lnPriorRatio) = brlensPrExp * (oldM - newM);

	return (NO_ERROR);
	
}





/*----------------------------------------------------------------
|
|	Move_Omega: Change the nonysnonymous/synonymous rate ratio
|      Note that this is appropriate when omegavar=equal
|
----------------------------------------------------------------*/
int Move_Omega (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* change omega using sliding window */
	
	int			i, isValidO;
	MrBFlt		oldO, newO, window, minO, maxO, ran, *alphaDir, oldPropRatio, newPropRatio, x, y;
	ModelParams	*mp;

	/* get size of window, centered on current omega value */
	window = mvp[0];
	
	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* get minimum and maximum values for omega */
	minO = KAPPA_MIN;
	maxO = KAPPA_MAX;

	/* get old value of omega */
	oldO = *GetParamVals(param, chain, state[chain]);

	/* get Dirichlet parameters */
	alphaDir = mp->tRatioDir;

	/* change value for omega */
	ran = RandomNumber(seed);
	newO = oldO + window * (ran - 0.5);
	
	/* check that new value is valid */
	isValidO = NO;
	do
		{
		if (newO < minO)
			newO = 2.0 * minO - newO;
		else if (newO > maxO)
			newO = 2.0 * maxO - newO;
		else
			isValidO = YES;
		} while (isValidO == NO);

	/* get proposal ratio */
	*lnProposalRatio = 0.0;
	
	/* get prior ratio from Dirichlet */
	oldPropRatio = oldO / (oldO + 1.0);
	newPropRatio = newO / (newO + 1.0);
	x = ((alphaDir[0] - 1.0) * log(newPropRatio)) + ((alphaDir[1] - 1.0) * log (1.0 - newPropRatio));
	y = ((alphaDir[0] - 1.0) * log(oldPropRatio)) + ((alphaDir[1] - 1.0) * log (1.0 - oldPropRatio));
	(*lnPriorRatio) = x - y;
	
	/* copy new omega value back */
	*GetParamVals(param, chain, state[chain]) = newO;

	/* Set update flags for all partitions that share this kappa. Note that the conditional
	   likelihood update flags have been set before we even call this function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));

	/* Set update flags for cijks for all affected partitions. If this is a simple 4 X 4 model,
	   we don't take any hit, because we will never go into a general transition probability
	   calculator. However, for covarion, doublet, and codon models, we do want to update
	   the cijk flag. */
	for (i=0; i<param->nRelParts; i++)
		modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);

}





/*----------------------------------------------------------------
|
|	Move_Omega_M: Change the nonysnonymous/synonymous rate ratio
|      using multiplier. Note that this is appropriate when
|      omegavar=equal
|
----------------------------------------------------------------*/
int Move_Omega_M (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* change omega using multiplier */
	
	int			i, isValidO;
	MrBFlt		oldO, newO, minO, maxO, tuning, ran, factor, *alphaDir, oldPropRatio, newPropRatio, x, y;
	ModelParams	*mp;

	/* get model params */
	mp = &modelParams[param->relParts[0]];

	/* get tuning parameter */
	tuning = mvp[0];

	/* get minimum and maximum values for omega */
	minO = KAPPA_MIN;
	maxO = KAPPA_MAX;

	/* get old value of omega */
	oldO = *GetParamVals(param, chain, state[chain]);

	/* get Dirichlet parameters */
	alphaDir = mp->omegaDir;

	/* change value for omega */
	ran = RandomNumber(seed);
	factor = exp(tuning * (ran - 0.5));
	newO = oldO * factor;
	
	/* check that new value is valid */
	isValidO = NO;
	do
		{
		if (newO < minO)
			newO = minO * minO / newO;
		else if (newO > maxO)
			newO = maxO * maxO / newO;
		else
			isValidO = YES;
		} while (isValidO == NO);

	/* get proposal ratio */
	*lnProposalRatio = log(newO / oldO);
	
	/* get prior ratio from Dirichlet */
	oldPropRatio = oldO / (oldO + 1.0);
	newPropRatio = newO / (newO + 1.0);
	x = ((alphaDir[0] - 1.0) * log(newPropRatio)) + ((alphaDir[1] - 1.0) * log (1.0 - newPropRatio));
	y = ((alphaDir[0] - 1.0) * log(oldPropRatio)) + ((alphaDir[1] - 1.0) * log (1.0 - oldPropRatio));
	(*lnPriorRatio) = x - y;
	
	/* copy new omega value back */
	*GetParamVals(param, chain, state[chain]) = newO;

	/* Set update flags for all partitions that share this omega. Note that the conditional
	   likelihood update flags have been set before we even call this function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));

	/* Set update flags for cijks for all affected partitions. If this is a simple 4 X 4 model,
	   we don't take any hit, because we will never go into a general transition probability
	   calculator. However, for covarion, doublet, and codon models, we do want to update
	   the cijk flag. */
	for (i=0; i<param->nRelParts; i++)
		modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);

}





/*----------------------------------------------------------------
|
|	Move_OmegaBeta_M: Change parameters of the beta distribution
|      using multiplier for the M10 model. Note that this is 
|      appropriate whenomegavar=M10
|
----------------------------------------------------------------*/
int Move_OmegaBeta_M (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	int			i, isValidVal, whichParam;
	MrBFlt		oldVal, newVal, minVal, maxVal, *vals, *subVals, tuning, ran, factor;
	ModelParams	*mp;
	
	/* do we pick alpha or beta of the Beta distribution to change */
	if (RandomNumber(seed) < 0.5)
		whichParam = 0;
	else
		whichParam = 1;

	/* get model params */
	mp = &modelParams[param->relParts[0]];

	/* get tuning parameter */
	tuning = mvp[0];

	/* get minimum and maximum values for omega */
	minVal = 0.05;
	maxVal = 100.0;

	/* get old value of omega */
	vals = GetParamVals(param, chain, state[chain]);
	subVals = GetParamSubVals(param, chain, state[chain]);
	oldVal = subVals[mp->numM10BetaCats + mp->numM10GammaCats + 4 + whichParam];

	/* change value for alpha/beta */
	ran = RandomNumber(seed);
	factor = exp(tuning * (ran - 0.5));
	newVal = oldVal * factor;
	
	/* check that new value is valid */
	isValidVal = NO;
	do
		{
		if (newVal < minVal)
			newVal = minVal * minVal / newVal;
		else if (newVal > maxVal)
			newVal = maxVal * maxVal / newVal;
		else
			isValidVal = YES;
		} while (isValidVal == NO);

	/* get proposal ratio */
	*lnProposalRatio = log(newVal / oldVal);
	
	/* get prior ratio */
	if (!strcmp(mp->m10betapr, "Exponential"))
		(*lnPriorRatio) = (log(mp->m10betaExp) - newVal * mp->m10betaExp) - (log(mp->m10betaExp) - oldVal * mp->m10betaExp);
	else
		(*lnPriorRatio) = 0.0;
	
	/* copy new omega value back */
	subVals[mp->numM10BetaCats + mp->numM10GammaCats + 4 + whichParam] = newVal;
	
	/* update the omega values */
	BetaBreaks (subVals[mp->numM10BetaCats + mp->numM10GammaCats + 4], subVals[mp->numM10BetaCats + mp->numM10GammaCats + 5], &vals[0], mp->numM10BetaCats);

	/* Set update flags for all partitions that share this kappa. Note that the conditional
	   likelihood update flags have been set before we even call this function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));

	/* Set update flags for cijks for all affected partitions. If this is a simple 4 X 4 model,
	   we don't take any hit, because we will never go into a general transition probability
	   calculator. However, for covarion, doublet, and codon models, we do want to update
	   the cijk flag. */
	for (i=0; i<param->nRelParts; i++)
		modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);

}





/*----------------------------------------------------------------
|
|	Move_OmegaGamma_M: Change parameters of the gamma distribution
|      using multiplier for the M10 model. Note that this is 
|      appropriate whenomegavar=M10
|
----------------------------------------------------------------*/
int Move_OmegaGamma_M (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	int			i, isValidVal, whichParam;
	MrBFlt		oldVal, newVal, minVal, maxVal, *vals, *subVals, tuning, ran, factor, quantile95;
	ModelParams	*mp;
	
	/* do we pick alpha or beta of the Gamma distribution to change */
	if (RandomNumber(seed) < 0.5)
		whichParam = 0;
	else
		whichParam = 1;

	/* get model params */
	mp = &modelParams[param->relParts[0]];

	/* get tuning parameter */
	tuning = mvp[0];

	/* get minimum and maximum values for omega */
	minVal = 0.05;
	maxVal = 100.0;

	/* get values */
	vals = GetParamVals(param, chain, state[chain]);
	subVals = GetParamSubVals(param, chain, state[chain]);
	oldVal = subVals[mp->numM10BetaCats + mp->numM10GammaCats + 6 + whichParam];

	/* change value for alpha/beta */
	do
		{
		ran = RandomNumber(seed);
		factor = exp(tuning * (ran - 0.5));
		newVal = oldVal * factor;
		
		/* check that new value is valid */
		isValidVal = NO;
		do
			{
			if (newVal < minVal)
				newVal = minVal * minVal / newVal;
			else if (newVal > maxVal)
				newVal = maxVal * maxVal / newVal;
			else
				isValidVal = YES;
			} while (isValidVal == NO);

		/* check that the distribution does not go too far to the right */
		if (whichParam == 0)
			quantile95 = QuantileGamma (0.95, newVal, subVals[mp->numM10BetaCats + mp->numM10GammaCats + 7]);
		else
			quantile95 = QuantileGamma (0.95, subVals[mp->numM10BetaCats + mp->numM10GammaCats + 6], newVal);

		} while (quantile95 > 100.0);
		
	/* get proposal ratio */
	*lnProposalRatio = log(newVal / oldVal);
	
	/* get prior ratio */
	if (!strcmp(mp->m10gammapr, "Exponential"))
		(*lnPriorRatio) = (log(mp->m10gammaExp) - newVal * mp->m10gammaExp) - (log(mp->m10gammaExp) - oldVal * mp->m10gammaExp);
	else
		(*lnPriorRatio) = 0.0;
	
	/* copy new value back */
	subVals[mp->numM10BetaCats + mp->numM10GammaCats + 6 + whichParam] = newVal;
	
	/* update the omega values */
	if (DiscreteGamma (&vals[mp->numM10BetaCats], subVals[mp->numM10BetaCats + mp->numM10GammaCats + 6], subVals[mp->numM10BetaCats + mp->numM10GammaCats + 7], mp->numM10GammaCats, 0) == ERROR)
		return (ERROR);
	for (i=0; i<mp->numM10GammaCats; i++)
		vals[mp->numM10BetaCats + i] += 1.0;

	/* Set update flags for all partitions that share this kappa. Note that the conditional
	   likelihood update flags have been set before we even call this function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));

	/* Set update flags for cijks for all affected partitions. If this is a simple 4 X 4 model,
	   we don't take any hit, because we will never go into a general transition probability
	   calculator. However, for covarion, doublet, and codon models, we do want to update
	   the cijk flag. */
	for (i=0; i<param->nRelParts; i++)
		modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);

}





#undef	DO_DIR_CAT_PROP
int Move_OmegaCat (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

#	if defined (DO_DIR_CAT_PROP)
	int			i, localNumCats, numBetaAndGammaCats;
	MrBFlt		dirichletParameters[3], *newSubVals, *oldSubVals, *newFreqs, *oldFreqs, *priorParams, sum, alpha, x, y;
	ModelParams	*mp;
#	else
	int			i, localNumCats, numBetaAndGammaCats, whichCat, isValidP;
	MrBFlt		scaledFreqs[3], *newSubVals, *oldSubVals, *newFreqs, *oldFreqs, *priorParams, sum, window, x, y, minP, maxP, oldP, newP, ran;
	ModelParams	*mp;
#	endif

	/* get model params */
	mp = &modelParams[param->relParts[0]];

	/* how many categories are there */
	localNumCats = 3;
	numBetaAndGammaCats = 0;
	if (!strcmp(mp->omegaVar, "M10"))
		{
		localNumCats = 2;
		numBetaAndGammaCats = mp->numM10BetaCats + mp->numM10GammaCats;
		}
		
	/* get the values we need */
	newSubVals = GetParamSubVals (param, chain, state[chain]);
	oldSubVals = GetParamSubVals (param, chain, state[chain] ^ 1);
	if (!strcmp(mp->omegaVar, "M10"))
		{
		newFreqs = newSubVals + numBetaAndGammaCats;
		oldFreqs = oldSubVals + numBetaAndGammaCats;
		priorParams = newSubVals + (numBetaAndGammaCats + 2);
		}
	else
		{
		newFreqs = newSubVals + 0;
		oldFreqs = oldSubVals + 0;
		priorParams = newFreqs + 3;
		}

#	if defined (DO_DIR_CAT_PROP)
	/* get parameter of proposal mechanism */
	alpha = mvp[0];

	/* multiply old values with some large number to get new values close to the old ones */
	for (i=0; i<localNumCats; i++)
		dirichletParameters[i] = oldFreqs[i] * alpha;

	/* get the new category frequencies */
	DirichletRandomVariable (dirichletParameters, newFreqs, localNumCats, seed);
	sum = 0.0;
	for (i=0; i<localNumCats; i++)
		{
		if (newFreqs[i] < 0.0001)
			newFreqs[i] = 0.0001;
		sum += newFreqs[i];
		}
	for (i=0; i<localNumCats; i++)
		newFreqs[i] /= sum;
		
	/* and get the new frequencies of the omega values, if we have another
	   distribution for omega too */
	if (!strcmp(mp->omegaVar, "M10"))
		{
		for (i=0; i<mp->numM10BetaCats; i++)
			newSubVals[i] = newFreqs[0] / mp->numM10BetaCats;
		for (i=mp->numM10BetaCats; i<mp->numM10BetaCats+mp->numM10GammaCats; i++)
			newSubVals[i] = newFreqs[1] / mp->numM10GammaCats;
		}	

	/* get proposal ratio */
	sum = 0.0;
	for (i=0; i<localNumCats; i++)
		sum += newFreqs[i]*alpha;
	x = LnGamma(sum);
	for (i=0; i<localNumCats; i++)
		x -= LnGamma(newFreqs[i]*alpha);
	for (i=0; i<localNumCats; i++)
		x += (newFreqs[i]*alpha-1.0)*log(oldFreqs[i]);
	sum = 0.0;
	for (i=0; i<localNumCats; i++)
		sum += oldFreqs[i]*alpha;
	y = LnGamma(sum);
	for (i=0; i<localNumCats; i++)
		y -= LnGamma(oldFreqs[i]*alpha);
	for (i=0; i<localNumCats; i++)
		y += (oldFreqs[i]*alpha-1.0)*log(newFreqs[i]);
	(*lnProposalRatio) = x - y;
#	else
	/* get parameter of proposal mechanism */
	window = mvp[0];
	
	minP = 0.0;
	maxP = 100.0;

	scaledFreqs[0] = 1.0;
	for (i=1; i<localNumCats; i++)
		scaledFreqs[i] = oldFreqs[i] / oldFreqs[0];	
	whichCat = 1 + (int)(RandomNumber(seed) * (localNumCats - 1));
	oldP = newP = scaledFreqs[whichCat];

	/* change value for category */
	ran = RandomNumber(seed);
	newP = oldP + window * (ran - 0.5);
	
	/* check that new value is valid */
	isValidP = NO;
	do
		{
		if (newP < minP)
			newP = 2* minP - newP;
		else if (newP > maxP)
			newP = 2 * maxP - newP;
		else
			isValidP = YES;
		} while (isValidP == NO);
		
	scaledFreqs[whichCat] = newP;
	sum = 0.0;
	for (i=0; i<localNumCats; i++)
		sum += scaledFreqs[i];

	for (i=0; i<localNumCats; i++)
		newFreqs[i] = scaledFreqs[i] / sum;	
		
	/* get proposal ratio */
	(*lnProposalRatio) = 0.0;
#	endif

	/* get prior ratio */
	sum = 0.0;
	for (i=0; i<localNumCats; i++)
		sum += priorParams[i];
	x = LnGamma(sum);
	for (i=0; i<localNumCats; i++)
		x -= LnGamma(priorParams[i]);
	y = x;
	for (i=0; i<localNumCats; i++)
		x += (priorParams[i]-1.0)*log(newFreqs[i]);
	for (i=0; i<localNumCats; i++)
		y += (priorParams[i]-1.0)*log(oldFreqs[i]);
	(*lnPriorRatio) = x - y;
		
	/* Set update flags for all partitions that share this omega. Note that the conditional
	   likelihood update flags have been set before we even call this function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));

	/* Set update flags for cijks for all affected partitions. */
	for (i=0; i<param->nRelParts; i++)
		modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);
	
}





/*----------------------------------------------------------------
|
|	Move_OmegaNeu: Change the nonysnonymous/synonymous rate ratio
|      for neutral sites
|
----------------------------------------------------------------*/
int Move_OmegaNeu (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	int			i, isOPriorExp, isValidO;
	MrBFlt		oldO, newO, window, minO, maxO, ran, *value, x, y;
	ModelParams *mp;

	/* get size of window, centered on current omega value */
	window = mvp[0];

	/* get model params */
	mp = &modelParams[param->relParts[0]];

	/* get old value of omega */
	value = GetParamVals(param, chain, state[chain]);
	newO = oldO = value[1];

	/* get minimum and maximum values for omega */
	minO = value[0];
	maxO = value[2];
	
	/* the only way we would be updating the middle category (omega2) is
	   if we have an exponential prior on all three omegas */
	isOPriorExp = YES;

	/* change value for omega */
	ran = RandomNumber(seed);
	newO = oldO + window * (ran - 0.5);
	
	/* check that new value is valid */
	isValidO = NO;
	do
		{
		if (newO < minO)
			newO = 2 * minO - newO;
		else if (newO > maxO)
			newO = 2 * maxO - newO;
		else
			isValidO = YES;
		} while (isValidO == NO);

	/* get proposal ratio */
	*lnProposalRatio = 0.0;
	
	/* copy new omega value back */
	value[1] = newO;

	/* get prior ratio */
	if (isOPriorExp == NO)
		{
		*lnPriorRatio = 0.0;
		}
	else
		{
		x = LogOmegaPrior (value[0], newO, value[2]);
		y = LogOmegaPrior (value[0], oldO, value[2]);
		*lnPriorRatio = x - y;
		}

	/* Set update flags for all partitions that share this omega. Note that the conditional
	   likelihood update flags have been set before we even call this function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));

	/* Set update flags for cijks for all affected partitions. */
	for (i=0; i<param->nRelParts; i++)
		modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);

}





/*----------------------------------------------------------------
|
|	Move_OmegaPos: Change the nonysnonymous/synonymous rate ratio
|      for positively selected sites
|
----------------------------------------------------------------*/
int Move_OmegaPos (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	int			i, isValidO, omegaUni, omegaExp1, omegaExp2;
	MrBFlt		oldO, newO, window, minO=0.0, maxO=0.0, ran, *value, x, y;
	ModelParams *mp;

	/* get size of window, centered on current omega value */
	window = mvp[0];

	/* get model params */
	mp = &modelParams[param->relParts[0]];

	/* get old value of omega */
	value = GetParamVals(param, chain, state[chain]);
	newO = oldO = value[2];
	
	/* determine prior for omega */
	omegaUni = omegaExp1 = omegaExp2 = NO;
	if (param->paramId == OMEGA_BUD || param->paramId == OMEGA_BUF || param->paramId == OMEGA_FUD || param->paramId == OMEGA_FUF)
		omegaUni = YES;
	else if (param->paramId == OMEGA_BED || param->paramId == OMEGA_BEF || param->paramId == OMEGA_FED || param->paramId == OMEGA_FEF)
		omegaExp1 = YES;
	else if (param->paramId == OMEGA_ED || param->paramId == OMEGA_EF)
		omegaExp2 = YES;
		
	/* get minimum and maximum values for omega */
	if (omegaUni == YES)
		{
		minO = mp->ny98omega3Uni[0];
		if (minO < value[1])
			minO = value[1];
		maxO = mp->ny98omega3Uni[1];
		if (maxO > KAPPA_MAX)
			maxO = KAPPA_MAX;
		}
	else if (omegaExp1 == YES || omegaExp2 == YES)
		{
		minO = value[1];
		maxO = KAPPA_MAX;
		}

	/* change value for omega */
	ran = RandomNumber(seed);
	newO = oldO + window * (ran - 0.5);
	
	/* check that new value is valid */
	isValidO = NO;
	do
		{
		if (newO < minO)
			newO = 2* minO - newO;
		else if (newO > maxO)
			newO = 2 * maxO - newO;
		else
			isValidO = YES;
		} while (isValidO == NO);

	/* get proposal ratio */
	*lnProposalRatio = 0.0;
	
	/* copy new omega value back */
	value[2] = newO;

	/* get prior ratio (part 2) */
	if (omegaUni == YES)
		{
		*lnPriorRatio = 0.0;
		}
	else if (omegaExp1 == YES)
		{
		x = log(mp->ny98omega3Exp) - mp->ny98omega3Exp * newO;
		y = log(mp->ny98omega3Exp) - mp->ny98omega3Exp * oldO;
		*lnPriorRatio = x - y;
		}
	else if (omegaExp2 == YES)
		{
		x = LogOmegaPrior (value[0], value[1], newO);
		y = LogOmegaPrior (value[0], value[1], oldO);
		*lnPriorRatio = x - y;
		}

	/* Set update flags for all partitions that share this omega. Note that the conditional
	   likelihood update flags have been set before we even call this function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));

	/* Set update flags for cijks for all affected partitions. */
	for (i=0; i<param->nRelParts; i++)
		modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);

}





/*----------------------------------------------------------------
|
|	Move_OmegaPur: Change the nonysnonymous/synonymous rate ratio
|      for purifying selection sites
|
----------------------------------------------------------------*/
int Move_OmegaPur (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	int			i, isOPriorExp, isValidO;
	MrBFlt		oldO, newO, window, minO, maxO, ran, *value, x, y;
	ModelParams *mp;

	/* get size of window, centered on current omega value */
	window = mvp[0];

	/* get model params */
	mp = &modelParams[param->relParts[0]];

	/* get old value of omega */
	value = GetParamVals(param, chain, state[chain]);
	newO = oldO = value[0];
	
	/* get minimum and maximum values for omega */
	minO = 0.0;
	maxO = value[1];
	
	/* get prior for omega */
	if (param->paramId == OMEGA_BUD || param->paramId == OMEGA_BUF || param->paramId == OMEGA_BED || 
		param->paramId == OMEGA_BEF || param->paramId == OMEGA_BFD || param->paramId == OMEGA_BFF) 
		isOPriorExp = NO;
	else
		isOPriorExp = YES;

	/* change value for omega */
	ran = RandomNumber(seed);
	newO = oldO + window * (ran - 0.5);
	
	/* check that new value is valid */
	isValidO = NO;
	do
		{
		if (newO < minO)
			newO = 2* minO - newO;
		else if (newO > maxO)
			newO = 2 * maxO - newO;
		else
			isValidO = YES;
		} while (isValidO == NO);

	/* get proposal ratio */
	*lnProposalRatio = 0.0;
	
	/* copy new omega value back */
	value[0] = newO;

	/* get prior ratio (part 2) */
	if (isOPriorExp == NO)
		{
		*lnPriorRatio = 0.0;
		}
	else
		{
		x = LogOmegaPrior (newO, value[1], value[2]);
		y = LogOmegaPrior (oldO, value[1], value[2]);
		*lnPriorRatio = x - y;
		}

	/* Set update flags for all partitions that share this omega. Note that the conditional
	   likelihood update flags have been set before we even call this function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));

	/* Set update flags for cijks for all affected partitions. */
	for (i=0; i<param->nRelParts; i++)
		modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);

}





/*----------------------------------------------------------------
|
|	Move_ParsEraser1: This proposal mechanism changes the topology and
|      branch lengths of an unrooted tree. A randomly chosen region of
|      the tree is erased. Parsimony is used to guide the selection of
|	   a new topology for the erased part of the tree. The parsimony
|      branch lengths are used to guide the proposal of new branch
|      lengths. This variant (1) uses exhaustive enumeration.
|
|      Programmed by FR 2004-10-23--
|
----------------------------------------------------------------*/
int Move_ParsEraser1 (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	int			i, j, isVPriorExp, nSubTerminals, nEmbeddedTrees;
	MrBFlt		alphaPi, warp, minV, maxV, minP, maxP, brlensExp=0.0, newM, oldM, maxLen,
				*brlensCur, *brlensNew, *parslensCur, *parslensNew,
				curLength, newLength, lnJacobian, lnRandomRatio, alpha[2], prob[2],
				minLenCur, minLenNew /*, f */;
	TreeNode	*p=NULL;
	Tree		*t, *subtree, *subtree1, memTree[2];
	ModelParams *mp;
	ModelInfo	*m;
	TreeInfo	tInfo;

	/* set pointers to NULL */
	subtree = subtree1 = NULL;
	brlensCur = NULL;
	for (i=0; i<2; i++)
		{
		memTree[i].allDownPass = NULL;
		memTree[i].intDownPass = NULL;
		memTree[i].nodes = NULL;
		}
	tInfo.leaf = NULL;

	/* Set alpha Pi for Dirichlet p generator */
	alphaPi = mvp[0];
	alphaPi = 0.1;
	
	/* Set the parsimony warp factor */
	warp = mvp[1];
	warp = 0.1;
	
	/* Set the number of terminals (nSubTerminals, column 3) in erased tree */
	/* Erased Nodes => Leaves => Terminals => Embedded trees => Embedded histories => New trees
                  2 => 3      => 4         => 2              => 2 = 2!             => 3 = 1*3
                  3 => 4      => 5         => 5              => 6 = 3!             => 15 = 1*3*5
				  4 => 5      => 6         => 14             => 24 = 4!            => 105 = 1*3*5*7
				  5 => 6      => 7         => 42             => 120 = 5!           => 945 = 1*3*5*7*9
				  etc				*/	
	nSubTerminals = 4;

	/* initialize log prior and log proposal probabilities */
	*lnPriorRatio = *lnProposalRatio = 0.0;
	
	/* get tree */
	t = GetTree (param, chain, state[chain]);

	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* max and min brlen */
	if (param->subParams[0]->paramId == BRLENS_UNI)
		{
		minV = mp->brlensUni[0] > BRLENS_MIN ? mp->brlensUni[0] : BRLENS_MIN;
		maxV = mp->brlensUni[1];
		isVPriorExp = NO;
		}
	else
		{
		minV = BRLENS_MIN;
		maxV = BRLENS_MAX;
		brlensExp = mp->brlensExp;
		isVPriorExp = YES;
		}
	minP = 3.0 * ((1.0 / 4.0) - ((1.0 / 4.0) * exp (-4.0 * minV / 3.0)));
	maxP = 3.0 * ((1.0 / 4.0) - ((1.0 / 4.0) * exp (-4.0 * maxV / 3.0)));
			
	/* allocate some memory for this move */
	brlensCur = (MrBFlt *) malloc (8 * nSubTerminals * sizeof (MrBFlt));
	if (!brlensCur)
		{
		MrBayesPrint ("%s   ERROR: Could not allocate brlensCur\n", spacer);
		goto errorExit;
		}
	brlensNew = brlensCur + 2*nSubTerminals;
	parslensCur = brlensCur + 4 * nSubTerminals;
	parslensNew = brlensCur + 6 * nSubTerminals;

	subtree = &memTree[0];
	subtree->nNodes = 2 * nSubTerminals - 2;
	subtree->nIntNodes = nSubTerminals - 2;
	subtree->nodes = (TreeNode *) calloc (subtree->nNodes, sizeof (TreeNode));
	subtree->allDownPass = (TreeNode **) calloc (subtree->nNodes, sizeof (TreeNode **));
	subtree->intDownPass = (TreeNode **) calloc (subtree->nIntNodes, sizeof (TreeNode **));
	if (!subtree->nodes || !subtree->intDownPass || !subtree->allDownPass)
		{
		MrBayesPrint ("%s   ERROR: Could not allocate subtree\n", spacer);
		goto errorExit;
		}

	subtree1 = &memTree[1];
	subtree1->nNodes = 2 * nSubTerminals - 2;
	subtree1->nIntNodes = nSubTerminals - 2;
	subtree1->nodes = (TreeNode *) calloc (subtree1->nNodes, sizeof (TreeNode));
	subtree1->allDownPass = (TreeNode **) calloc (subtree1->nNodes, sizeof (TreeNode **));
	subtree1->intDownPass = (TreeNode **) calloc (subtree1->nIntNodes, sizeof (TreeNode **));
	if (!subtree1->nodes || !subtree1->intDownPass || !subtree1->allDownPass)
		{
		MrBayesPrint ("%s   ERROR: Could not allocate subtree1\n", spacer);
		goto errorExit;
		}

	tInfo.leaf = (TreeNode **) calloc (t->nNodes, sizeof(TreeNode *));
	if (!tInfo.leaf)
		{
		MrBayesPrint ("%s   ERROR: Could not allocate tInfo.leaf\n", spacer);
		goto errorExit;
		}
	tInfo.vertex = tInfo.leaf + t->nNodes - t->nIntNodes;

	/* Select a random embedded subtree with nSubTerminals terminals */
	if (GetRandomEmbeddedSubtree (t, nSubTerminals, seed, &nEmbeddedTrees) == ERROR)
		{
		MrBayesPrint ("%s   ERROR: Could not get subtree\n", spacer);
		goto errorExit;
		}

	/* Set update flags (We'd better do it before the marked nodes disappear) */
	for (i=0; i<t->nIntNodes; i++)
		{
		p = t->intDownPass[i];
		if (p->marked == YES)
			{
			p->upDateCl = YES; 
			p->upDateTi = YES;
			}
		else if (p->left->upDateCl == YES || p->right->upDateCl == YES)
		        p->upDateCl = YES; 
		}

	/* Fill in subtrees */
	CopyTreeToSubtree (t, subtree);	
	CopyTreeToSubtree (t, subtree1);

	/* Calculate downstates and upstate of root node of subtree */
	GetParsimonyDownStates (t, chain);
	for (i=0; i<t->nIntNodes; i++)
		{
		p = t->intDownPass[i];
		if (p->marked == YES && p->anc->marked == NO)
			break;
		}
	GetParsimonySubtreeRootstate (t, p->anc, chain);

	/* Get parsimony length of current tree */
	curLength = GetParsimonyLength (subtree, chain);
	
	/* Get the Markov and parsimony branch lengths of the current subtree */
	GetParsimonyBrlens (subtree, chain, parslensCur);
	for (i=0; i<subtree->nNodes-1; i++)
		brlensCur[i] = subtree->allDownPass[i]->length;

	/* Calculate parsimony score of all trees relative to shortest tree (1.0) */
	tInfo.totalScore = 0.0;
	tInfo.stopScore = -1.0;
	tInfo.minScore = curLength;
	tInfo.warp = warp;
	ExhaustiveParsimonySearch (subtree, chain, &tInfo);
		
	/* Choose one of these trees randomly based on its score */
	tInfo.stopScore = RandomNumber(seed) * tInfo.totalScore;
	tInfo.totalScore = 0.0;
	ExhaustiveParsimonySearch (subtree1, chain, &tInfo);
	/* ShowNodes (subtree1->root, 0 , NO); */
	/* getchar(); */

	/* Get length of that tree */

	newLength = GetParsimonyLength (subtree1, chain);

	/* Get the parsimony branch lengths of the new subtree */
	GetParsimonyBrlens (subtree1, chain, parslensNew);

	/* Find the maximum length of a branch */
	maxLen = 0.0;
	for (i=0; i<t->nRelParts; i++)
		{
		j = t->relParts[i];
		m = &modelSettings[j];
		maxLen += m->numUncompressedChars;
		}
	
	/* Find the Markov branch lengths of the new subtree */
	/* Calculate Jacobian and prob ratio for the Dirichlet random number generator */
	/* Note: this code does not correct for the difference between the area under the tail regions of the beta
	   caused by different alpha and beta values being associated with different probabilities outside of
	   the minP and maxP region. However, this effect should be negligible unless maxLen and/or alphaPi are
	   extremely small. */
	lnJacobian = lnRandomRatio = 0.0;
	minLenCur = minLenNew = 0.0;
	/*
	for (i=0; i<subtree1->nNodes-1; i++)
		{
		minLenCur += parslensCur[i];
		minLenNew += parslensNew[i];
		}
	for (i=0; i<subtree1->nNodes-1; i++)
		{
		p = subtree1->allDownPass[i];
		f = newLength / minLenNew;
		alpha[0] = parslensNew[i] * f * alphaPi + 1.0;
		alpha[1] = (maxLen - parslensNew[i] * f) * alphaPi + 1.0;
		do {
			DirichletRandomVariable (alpha, prob, 2, seed);
		} while (prob[0] >= maxP || prob[0] <= minP);
		p->length = (-3.0 / 4.0) * log (1.0 - 4.0 * prob[0] / 3.0);
		lnJacobian += (-4.0 * brlensCur[i] / 3.0) - log (1.0 - 4.0 * prob[0] / 3.0);
		lnRandomRatio -= log (pow (prob[0], alpha[0] - 1.0) * pow (prob[1], alpha[1] - 1.0));
		f = curLength / minLenNew;
		alpha[0] = parslensCur[i] * f * alphaPi + 1.0;
		alpha[1] = (maxLen - parslensCur[i] * f) * alphaPi + 1.0;
		prob[0] = 3.0 * ((1.0 / 4.0) - ((1.0 / 4.0) * exp (-4.0 * brlensCur[i] / 3.0)));
		prob[1] = 1.0 - prob[0];
		lnRandomRatio += log (pow (prob[0], alpha[0] - 1.0) * pow (prob[1], alpha[1] - 1.0));
		}
	*/
	alpha[0] = alpha[1] = prob[0] = prob[1] = 1.0;

	/* Store the new Markov branch lengths */
	for (i=0; i<subtree1->nNodes-1; i++)
		brlensNew[i] = subtree1->allDownPass[i]->length;

	/* Calculate the proposal ratio */
	(*lnProposalRatio) = lnJacobian + lnRandomRatio + log (warp/3.0) * (curLength - newLength) + log (1.0-warp) * (newLength - curLength);

	/* Calculate the prior ratio */
	if (isVPriorExp == YES)
		{
		newM = oldM = 0.0;
		for (i=0; i<subtree->nNodes-1; i++)
			{
			oldM += brlensCur[i];
			newM += brlensNew[i];
			}
		(*lnPriorRatio) += brlensExp * (oldM - newM);
		}

	/* Copy subtree into tree */
	CopySubtreeToTree (subtree1, t);
	/* ShowNodes (subtree1->root, 0, NO); */
	/* ShowNodes (t->root, 0, NO); */

	/* Update node sequences */
	GetDownPass (t);
	
	/* correct for difference in number of embedded subtrees */
	if (GetRandomEmbeddedSubtree (t, nSubTerminals, seed, &i) == ERROR)
		{
		MrBayesPrint ("%s   Could not count number of subtrees in Move_ParsEraser1\n", spacer);
		goto errorExit;
		}
	if (i != nEmbeddedTrees)
		(*lnProposalRatio) += log ((MrBFlt) nEmbeddedTrees / (MrBFlt) i);

	/* Free memory allocated for this move */
	free (subtree->allDownPass);
	free (subtree->intDownPass);
	free (subtree->nodes);
	free (subtree1->allDownPass);
	free (subtree1->intDownPass);
	free (subtree1->nodes);
	free (brlensCur);
	free (tInfo.leaf);

	return (NO_ERROR);

errorExit:

	free (subtree->allDownPass);
	free (subtree->intDownPass);
	free (subtree->nodes);
	free (subtree1->allDownPass);
	free (subtree1->intDownPass);
	free (subtree1->nodes);
	free (brlensCur);
	free (tInfo.leaf);

	return (ERROR);

}





int Move_Pinvar (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* change proportion of invariable sites (pInvar) */

	int			i, isValidP;
	MrBFlt		oldP, newP, window, minP, maxP, ran;
	ModelParams *mp;

	/* get size of window, centered on current pInvar value */
	window = mvp[0];

	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* get minimum and maximum values for pInvar */
	minP = mp->pInvarUni[0];
	maxP = mp->pInvarUni[1];

	/* get old value of pInvar */
	newP = oldP = *GetParamVals(param, chain, state[chain]);

	/* change value for pInvar */
	ran = RandomNumber(seed);
	newP = oldP + window * (ran - 0.5);

	/* check validity */
	isValidP = NO;
	do
		{
		if (newP < minP)
			newP = 2* minP - newP;
		else if (newP > maxP)
			newP = 2 * maxP - newP;
		else
			isValidP = YES;
		} while (isValidP == NO);

	/* get proposal ratio */
	*lnProposalRatio = 0.0;
	
	/* get prior ratio */
	*lnPriorRatio = 0.0;
	
	/* copy new pInvar value back */
	*GetParamVals(param, chain, state[chain]) = newP;

	/* Set update flags for all partitions that share this pInvar. Note that the conditional
	   likelihood update flags for divisions have been set before we even call this function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));
	
	/* However, you do need to update cijk flags if this is a covarion model */
	/* TO DO */
	
	return (NO_ERROR);

}





/*----------------------------------------------------------------
|
|	Move_RateMult_Dir: Change rate multiplier using Dirichlet
|      proposal.
|
----------------------------------------------------------------*/
int Move_RateMult_Dir (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	int			i, nRates;
	MrBFlt		alphaPi, *value, *subValue, numSites, *alphaDir, x, y, sum,
				dirParm[MAX_NUM_DIVS], oldRate[MAX_NUM_DIVS], newRate[MAX_NUM_DIVS];

	/* get so called alphaPi parameter */
	alphaPi = mvp[0];

	/* get number of rates */
	nRates = param->nValues;

	/* get pointer to rates and number of uncompressed chars */
	value = GetParamVals(param, chain, state[chain]);
	subValue = GetParamSubVals(param, chain, state[chain]);

	/* get Dirichlet parameters */
	alphaDir = subValue + 2*nRates;

	/* calculate old ratesum proportions */
	sum = 0.0;
	for (i=0; i<nRates; i++)
		sum += subValue[nRates + i];
	for (i=0; i<nRates; i++)
		oldRate[i] = subValue[nRates + i] / sum;
	
	/* multiply old ratesum proportions with some large number to get new values close to the old ones */
	for (i=0; i<nRates; i++)
		dirParm[i] = oldRate[i] * alphaPi;
	
	/* get new values */
	DirichletRandomVariable (dirParm, newRate, nRates, seed);

	/* check new values */
	sum = 0.0;
	for (i=0; i<nRates; i++)
		{
		if (newRate[i] < DIR_MIN)
			newRate[i] = DIR_MIN;
		sum += newRate[i];
		}
	for (i=0; i<nRates; i++)
		newRate[i] /= sum;

	/* calculate and copy new rate ratio values back */
	for (i=1; i<nRates; i++)
		subValue[nRates + i] = newRate[i] / newRate[0];
	
	/* get proposal ratio */
	sum = 0.0;
	for (i=0; i<nRates; i++)
		sum += newRate[i]*alphaPi;
	x = LnGamma(sum);
	for (i=0; i<nRates; i++)
		x -= LnGamma(newRate[i]*alphaPi);
	for (i=0; i<nRates; i++)
		x += (newRate[i]*alphaPi-1.0)*log(oldRate[i]);
	sum = 0.0;
	for (i=0; i<nRates; i++)
		sum += oldRate[i]*alphaPi;
	y = LnGamma(sum);
	for (i=0; i<nRates; i++)
		y -= LnGamma(oldRate[i]*alphaPi);
	for (i=0; i<nRates; i++)
		y += (oldRate[i]*alphaPi-1.0)*log(newRate[i]);
	(*lnProposalRatio) = x - y;

	/* get prior ratio */
	x = y = 0.0;
	for (i=0; i<nRates; i++)
		x += (alphaDir[i]-1.0)*log(newRate[i]);
	for (i=0; i<nRates; i++)
		y += (alphaDir[i]-1.0)*log(oldRate[i]);
	(*lnPriorRatio) = x - y;

	/* calculate new scaled rates */
	sum = 0.0;
	numSites = 0;
	for (i=0; i<nRates; i++)
		{
		sum += subValue[nRates + i] * subValue[i];
		numSites += subValue[i];
		}
	sum = (MrBFlt)numSites / sum;
	for (i=0; i<nRates; i++)
		{
		value[i] = subValue[nRates + i] * sum;
		}

	/* Set update flags for all partitions that share the rate multiplier. Note that the conditional
	   likelihood update flags have been set before we even call this function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));
		
	/* may need to hit update flag for cijks when you have a covarion model */
	for (i=0; i<param->nRelParts; i++)
		if (modelSettings[param->relParts[i]].nCijkParts > 1)
			modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);

}





/*----------------------------------------------------------------
|
|	Move_Revmat_Dir: Change rate matrix using Dirichlet proposal
|      mechanism.
|
----------------------------------------------------------------*/
int Move_Revmat_Dir (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* change revMat using Dirichlet proposal */
	
	int			i, nRates;
	MrBFlt		oldRate[200], newRate[200], dirParm[200], *value, sum, x, y, *alphaDir, alphaPi;
	ModelParams *mp;

	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* get so called alpha Pi parameter */
	if (mp->dataType == PROTEIN)
		alphaPi = 1000.0 * mvp[0];
	else
		alphaPi = mvp[0];

	/* get rates and nRates */
	value = GetParamVals(param, chain, state[chain]);
	nRates = param->nValues;

	/* get Dirichlet parameters */
	if (mp->dataType == PROTEIN)
		alphaDir = mp->aaRevMatDir;
	else
		alphaDir = mp->revMatDir;

	/* copy old rates */
	for (i=0; i<nRates; i++)
		oldRate[i] = value[i];
	
	/* multiply old ratesum props with some large number to get new values close to the old ones */
	for (i=0; i<nRates; i++)
		dirParm[i] = oldRate[i] * alphaPi;
	
	/* get new values */
	DirichletRandomVariable (dirParm, newRate, nRates, seed);

	/* check new values */
	sum = 0.0;
	for (i=0; i<nRates; i++)
		{
		if (newRate[i] < DIR_MIN)
			newRate[i] = DIR_MIN;
		sum += newRate[i];
		}
	for (i=0; i<nRates; i++)
		newRate[i] /= sum;

	/* copy new rate ratio values back */
	for (i=0; i<nRates; i++)
		{
		value[i] = newRate[i];
		}
	
	/* get proposal ratio */
	sum = 0.0;
	for (i=0; i<nRates; i++)
		sum += newRate[i]*alphaPi;
	x = LnGamma(sum);
	for (i=0; i<nRates; i++)
		x -= LnGamma(newRate[i]*alphaPi);
	for (i=0; i<nRates; i++)
		x += (newRate[i]*alphaPi-1.0)*log(oldRate[i]);
	sum = 0.0;
	for (i=0; i<nRates; i++)
		sum += oldRate[i]*alphaPi;
	y = LnGamma(sum);
	for (i=0; i<nRates; i++)
		y -= LnGamma(oldRate[i]*alphaPi);
	for (i=0; i<nRates; i++)
		y += (oldRate[i]*alphaPi-1.0)*log(newRate[i]);
	(*lnProposalRatio) = x - y;

	/* get prior ratio */
	x = y = 0.0;
	for (i=0; i<nRates; i++)
		x += (alphaDir[i]-1.0)*log(newRate[i]);
	for (i=0; i<6; i++)
		y += (alphaDir[i]-1.0)*log(oldRate[i]);
	(*lnPriorRatio) = x - y;

	/* Set update flags for all partitions that share this revmat. Note that the conditional
	   likelihood update flags have been set before we even call this function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes (GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));
		
	/* Set update flags for cijks for all affected partitions */
	for (i=0; i<param->nRelParts; i++)
		modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);

}





int Move_Speciation (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* change speciation rate using sliding window */
	
	int			isLPriorExp, isValidL;
	MrBFlt		oldL, newL, window, minL, maxL, lambdaExp=0.0, ran, sR, eR, sF, oldLnPrior, newLnPrior;
	ModelParams *mp;
	ModelInfo	*m;
	Tree		*t;

	/* get size of window, centered on current lambda value */
	window = mvp[0];

	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* get extinction rate */
	m = &modelSettings[param->relParts[0]];
	eR = *(GetParamVals (m->extinctionRates, chain, state[chain]));
	
	/* get minimum and maximum values for lambda */
	if (param->paramId == SPECRATE_UNI)
		{
		if (mp->speciationUni[0] < eR)
			minL = eR;
		else
			minL = mp->speciationUni[0];
		minL = mp->speciationUni[0];
		maxL = mp->speciationUni[1];
		isLPriorExp = NO;
		}
	else
		{
		minL = eR;
		maxL = KAPPA_MAX;
		lambdaExp = mp->speciationExp;
		isLPriorExp = YES;
		}

	/* get old value of lambda */
	newL = oldL = *GetParamVals(param, chain, state[chain]);

	/* change value for lambda */
	ran = RandomNumber(seed);
	newL = oldL + window * (ran - 0.5);
	
	/* check that new value is valid */
	isValidL = NO;
	do
		{
		if (newL < minL)
			newL = 2* minL - newL;
		else if (newL > maxL)
			newL = 2 * maxL - newL;
		else
			isValidL = YES;
		} while (isValidL == NO);

	/* get proposal ratio */
	*lnProposalRatio = 0.0;
	
	/* calculate prior ratio */
	t = GetTree(modelSettings[param->relParts[0]].brlens,chain,state[chain]);
	sF = mp->sampleProb;
	sR = oldL;
	if (LnBirthDeathPriorPr (t, &oldLnPrior, sR, eR, sF) == ERROR)
		{
		MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
		return (ERROR);
		}
	sR = newL;
	if (LnBirthDeathPriorPr (t, &newLnPrior, sR, eR, sF) == ERROR)
		{
		MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
		return (ERROR);
		}
	if (isLPriorExp == NO)
		*lnPriorRatio = newLnPrior - oldLnPrior;
	else
		*lnPriorRatio = -lambdaExp * (newL - oldL) + (newLnPrior - oldLnPrior);
	
	/* copy new lambda value back */
	*GetParamVals(param, chain, state[chain]) = newL;

	return (NO_ERROR);
	
}





int Move_Speciation_M (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* change speciation rate using multiplier */
	
	int			isLPriorExp, isValidL;
	MrBFlt		oldL, newL, minL, maxL, lambdaExp=0.0, ran, sR, eR, sF, oldLnPrior, newLnPrior,
				tuning, factor;
	ModelParams *mp;
	ModelInfo	*m;
	Tree		*t;

	/* get tuning parameter */
	tuning = mvp[0];

	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* get extinction rate */
	m = &modelSettings[param->relParts[0]];
	eR = *(GetParamVals (m->extinctionRates, chain, state[chain]));
	
	/* get minimum and maximum values for lambda */
	if (param->paramId == SPECRATE_UNI)
		{
		if (mp->speciationUni[0] < eR)
			minL = eR;
		else
			minL = mp->speciationUni[0];
		minL = mp->speciationUni[0];
		maxL = mp->speciationUni[1];
		isLPriorExp = NO;
		}
	else
		{
		minL = eR;
		maxL = KAPPA_MAX;
		lambdaExp = mp->speciationExp;
		isLPriorExp = YES;
		}

	/* get old value of lambda */
	newL = oldL = *GetParamVals(param, chain, state[chain]);

	/* change value for lambda */
	ran = RandomNumber(seed);
	factor = exp(tuning * (ran - 0.5));
	newL = oldL * factor;
	
	/* check that new value is valid */
	isValidL = NO;
	do
		{
		if (newL < minL)
			newL = minL * minL / newL;
		else if (newL > maxL)
			newL = maxL * maxL / newL;
		else
			isValidL = YES;
		} while (isValidL == NO);

	/* get proposal ratio */
	*lnProposalRatio = log (newL / oldL);
	
	/* calculate prior ratio */
	t = GetTree(modelSettings[param->relParts[0]].brlens,chain,state[chain]);
	sF = mp->sampleProb;
	sR = oldL;
	if (LnBirthDeathPriorPr (t, &oldLnPrior, sR, eR, sF) == ERROR)
		{
		MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
		return (ERROR);
		}
	sR = newL;
	if (LnBirthDeathPriorPr (t, &newLnPrior, sR, eR, sF) == ERROR)
		{
		MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
		return (ERROR);
		}
	if (isLPriorExp == NO)
		*lnPriorRatio = newLnPrior - oldLnPrior;
	else
		*lnPriorRatio = -lambdaExp * (newL - oldL) + (newLnPrior - oldLnPrior);
	
	/* copy new lambda value back */
	*GetParamVals(param, chain, state[chain]) = newL;

	return (NO_ERROR);
	
}




/*----------------------------------------------------------------
|
|	Move_SPRClock: This proposal mechanism changes the topology and
|      branch lengths of a rooted tree. 
|
|      Programmed by JH 2002-11-18
|
----------------------------------------------------------------*/
int Move_SPRClock (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{


	int				i, stopLoop, topologyHasChanged, direction=0, nTipsOnSideA=0, nTipsOnSideB=0, whichTip, tempI, isPathTerminusRoot=0;
	MrBFlt			attachmentRate, lnProbForward, lnProbReverse, dist=0.0, pathLength=0.0, pathLengthA=0.0, pathLengthB=0.0,
					oldAttachmentDepth=0.0, newAttachmentDepth=0.0, newD, oldD, oldCeiling=0.0, newCeiling=0.0, alphaPi, x, y, sum, len, baseOfPath=0.0,
					newLnPrior, oldLnPrior, sR=0.0, eR=0.0, sF=0.0, theta=0.0, growth=0.0, dirichletParameters[2], oldProportions[2], newProportions[2],
					newPathLengthA, newPathLengthB;
	TreeNode		*p, *q, *nodeToMove=NULL, *nodeToDetach=NULL, *oldAttachmentNode=NULL, *newAttachmentNode=NULL, *pathEndA=NULL, *pathEndB=NULL;
	Tree			*t;
	ModelParams 	*mp;
	ModelInfo		*m;
	
	/* Parameters used in this move. */
	alphaPi = mvp[0];              /* Dirichlet parameter.                           */
	attachmentRate = mvp[1];       /* The parameter of the exponential distribution. */
	
	/* Set the proposal ratio. */
	lnProbForward = lnProbReverse = (*lnProposalRatio) = 0.0;
	
	/* Set the prior ratio. */
	(*lnPriorRatio) = 0.0;
	
	/* Get the tree. */
	t = GetTree (param, chain, state[chain]);

#	if defined (DEBUG_SPRCLOCK)
	for (i=0; i<0; i++)
		RandomNumber(seed);
	printf ("Before:\n");
	ShowNodes (t->root, 2, YES);
#	endif

	/* get model params */
	mp = &modelParams[param->relParts[0]];
			
	/* calculate prior ratio (part 1/2) */
	if (param->subParams[0]->paramId == BRLENS_CLOCK_UNI ||
		param->subParams[0]->paramId == BRLENS_CCLOCK_UNI)
		{
		/* uniformly distributed branch lengths */
		if (!strcmp(mp->treeHeightPr, "Exponential"))
			oldLnPrior = log(mp->treeHeightExp) - mp->treeHeightExp * t->root->left->nodeDepth;
		else
			oldLnPrior = mp->treeHeightGamma[1] * log(mp->treeHeightGamma[0]) - LnGamma(mp->treeHeightGamma[1]) + (mp->treeHeightGamma[1] - 1.0) * log(t->root->left->nodeDepth) - mp->treeHeightGamma[0] * t->root->left->nodeDepth;
		}
	else if (param->subParams[0]->paramId == BRLENS_CLOCK_COAL ||
			 param->subParams[0]->paramId == BRLENS_CCLOCK_COAL)
		{
		/* coalescence prior */
		m = &modelSettings[param->relParts[0]];
		theta = *(GetParamVals (m->theta, chain, state[chain]));
		if (!strcmp(mp->growthPr, "Fixed"))
			growth = mp->growthFix;
		else
			growth = *(GetParamVals (m->growthRate, chain, state[chain]));
		if (LnCoalescencePriorPr (t, &oldLnPrior, theta, growth) == ERROR)
			{
			MrBayesPrint ("%s   Problem calculating prior for coalescence process\n", spacer);
			goto errorExit;
			}
		}
	else
		{
		/* birth-death prior */
		m = &modelSettings[param->relParts[0]];
		sR = *(GetParamVals (m->speciationRates, chain, state[chain]));
		eR = *(GetParamVals (m->extinctionRates, chain, state[chain]));
		sF = mp->sampleProb;
		if (LnBirthDeathPriorPr (t, &oldLnPrior, sR, eR, sF) == ERROR)
			{
			MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
			goto errorExit;
			}
		if (!strcmp(mp->treeHeightPr, "Exponential"))
			oldLnPrior += log(mp->treeHeightExp) - mp->treeHeightExp * t->root->left->nodeDepth;
		else
			oldLnPrior += mp->treeHeightGamma[1] * log(mp->treeHeightGamma[0]) - LnGamma(mp->treeHeightGamma[1]) + (mp->treeHeightGamma[1] - 1.0) * log(t->root->left->nodeDepth) - mp->treeHeightGamma[0] * t->root->left->nodeDepth;
		}

	/* Pick a branch to move and get pointers to nodes in area of rearrangement. */
	stopLoop = NO;
	do
		{
		nodeToMove = t->allDownPass[(int)(RandomNumber(seed)*t->nNodes)];
		if (nodeToMove->anc != NULL)
			if (nodeToMove->anc->anc != NULL)
				stopLoop = YES;
		} while (stopLoop == NO);
	nodeToDetach = nodeToMove->anc;
	if (nodeToMove->anc->left == nodeToMove)
		oldAttachmentNode = nodeToMove->anc->right;
	else
		oldAttachmentNode = nodeToMove->anc->left;

	/* Pick two tips to be the ends of the path. */
	nTipsOnSideA = nTipsOnSideB = 0;
	pathEndA = pathEndB = NULL;
	for (i=t->nNodes-1; i>=0; i--)
		{
		p = t->allDownPass[i];
		p->marked = NO;
		if (p == nodeToDetach)
			p->marked = YES;
		else if (p == nodeToMove)
			p->marked = 2;
			
		if (p->anc != NULL)
			{
			if (p != nodeToMove && p->anc->marked == YES)
				p->marked = YES;
			else if (p->anc->marked == 2)
				p->marked = 2;
			}
		if (p->left == NULL && p->right == NULL && p->marked == YES)
			nTipsOnSideA++;
		else if (p->left == NULL && p->right == NULL && p->marked == NO)
			{
			if (p != nodeToMove)
				nTipsOnSideB++;
			}
		}
	nTipsOnSideB++;
	whichTip = (int)(RandomNumber(seed)*nTipsOnSideA);
	tempI = 0;
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->left == NULL && p->right == NULL && p->marked == YES)
			{
			if (tempI == whichTip)
				{
				pathEndA = p;
				break;
				}
			tempI++;
			}
		}
	whichTip = (int)(RandomNumber(seed)*nTipsOnSideB);
	if (whichTip == 0)
		pathEndB = t->root;
	else
		{
		tempI = 1;
		for (i=0; i<t->nNodes; i++)
			{
			p = t->allDownPass[i];
			if (p->left == NULL && p->right == NULL && p->marked == NO && p != nodeToMove)
				{
				if (tempI == whichTip)
					{
					pathEndB = p;
					break;
					}
				tempI++;
				}
			}
		}
	isPathTerminusRoot = NO;
	if (pathEndB == t->root)
		isPathTerminusRoot = YES;
	if (pathEndA == NULL || pathEndB == NULL)
		{
		MrBayesPrint ("%s   Problem getting path ends in SPRClock\n", spacer);
		goto errorExit;
		}
	else if (pathEndA == t->root)
		{
		MrBayesPrint ("%s   Path end A should not be the root\n", spacer);
		goto errorExit;
		}
				
	/* Mark the nodes on the path. */
	pathLength = baseOfPath = 0.0;
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		p->marked = NO;
		if (p == pathEndA || p == pathEndB)
			p->marked = YES;
		if (p->left != NULL && p->right != NULL)
			{
			if (p->left->marked == YES && p->right->marked == NO)
				p->marked = YES;
			else if (p->left->marked == NO && p->right->marked == YES)
				p->marked = YES;
			else if (p->left->marked == YES && p->right->marked == YES)
				baseOfPath = p->nodeDepth;
			}
		if (p->marked == YES)
			pathLength += p->length;
		}	

	/* Get length of path above nodeToDetach */
	pathLengthA = oldAttachmentNode->anc->nodeDepth;
	if (isPathTerminusRoot == NO)
		{
		pathLength = 2.0 * baseOfPath;
		pathLengthB = 2.0 * baseOfPath - pathLengthA;
		}
	else
		{
		pathLength = 0.0;
		pathLengthB = 0.0;
		}
	
	/* Readjust time of node to be moved and lengths of branches above the node-to-move. Also,
	   get the old and new ceilings. */
	oldAttachmentDepth = oldAttachmentNode->anc->nodeDepth;
	if (nodeToMove->left == NULL && nodeToMove->right == NULL)
		{
		oldD = newD = 0.0;
		(*lnProposalRatio) += 0.0;
		}
	else
		{
		if (nodeToMove->left->length < nodeToMove->right->length)
			oldD = nodeToMove->left->length;
		else
			oldD = nodeToMove->right->length;
		len = oldD + nodeToMove->length;
		oldProportions[0] = oldD / len;
		oldProportions[1] = 1.0 - oldProportions[0];
		dirichletParameters[0] = oldProportions[0] * alphaPi;
		dirichletParameters[1] = oldProportions[1] * alphaPi;
		DirichletRandomVariable (dirichletParameters, newProportions, 2, seed);
		for (i=0; i<2; i++)
			if (newProportions[i] < 0.01)
				newProportions[i] = 0.01;
		sum = newProportions[0] + newProportions[1];
		newProportions[0] /= sum;
		newProportions[1] /= sum;
		sum = newProportions[0]*alphaPi + newProportions[1]*alphaPi;
		x = LnGamma(sum) - LnGamma(newProportions[0]*alphaPi) -  LnGamma(newProportions[1]*alphaPi);
		x += (newProportions[0]*alphaPi-1.0)*log(oldProportions[0]) + (newProportions[1]*alphaPi-1.0)*log(oldProportions[1]);
		sum = oldProportions[0]*alphaPi + oldProportions[1]*alphaPi;
		y = LnGamma(sum) - LnGamma(oldProportions[0]*alphaPi) - LnGamma(oldProportions[1]*alphaPi);
		y += (oldProportions[0]*alphaPi-1.0)*log(newProportions[0]) + (oldProportions[1]*alphaPi-1.0)*log(newProportions[1]);
		(*lnProposalRatio) += x - y;
		newD = newProportions[0] * len;
		/*printf ("%1.10lf %1.10lf %1.10lf %1.10lf \n", oldProportions[0], oldProportions[1], newProportions[0], newProportions[1]);*/
		}
	oldCeiling = nodeToMove->nodeDepth;
	newCeiling = oldCeiling + (newD - oldD);
	nodeToMove->nodeDepth = newCeiling;
	if (nodeToMove->left != NULL)
		nodeToMove->left->length = nodeToMove->nodeDepth - nodeToMove->left->nodeDepth;
	if (nodeToMove->right != NULL)
		nodeToMove->right->length = nodeToMove->nodeDepth - nodeToMove->right->nodeDepth;
	
	/* pick a direction to slide the node in */
	if (RandomNumber(seed) < 0.5)
		direction = UP;
	else
		direction = DOWN;
		
	/* pick the distance to move the node */
	if (direction == UP)
		{
		len = pathLengthA - newCeiling;
		
		do
			{
			dist = -(1.0 / attachmentRate) * log(1.0 - RandomNumber(seed) * (1.0 - exp(-attachmentRate*len)));
			if (dist > len)
				{
				MrBayesPrint ("%s   Problem with new attachment point (%lf %lf)\n", spacer, len, dist);
				goto errorExit;
				}
			newAttachmentDepth = oldAttachmentDepth - dist;
			} while (oldAttachmentDepth - dist < newCeiling);
			
		lnProbForward += log(attachmentRate) - attachmentRate*dist - log(1.0 - exp(-attachmentRate*len));
		newPathLengthA = pathLengthA - dist;
		newPathLengthB = pathLengthB + dist;
		if (isPathTerminusRoot == NO)
			{
			len = pathLengthB - oldCeiling;
			lnProbReverse += log(attachmentRate) - attachmentRate*dist - log(1.0 - exp(-attachmentRate*len));
			}
		else
			{
			lnProbReverse += log(attachmentRate) - attachmentRate*dist;
			}
		}
	else
		{
		if (isPathTerminusRoot == NO)
			{
			len = pathLengthB - newCeiling;
			dist = -(1.0 / attachmentRate) * log(1.0 - RandomNumber(seed) * (1.0 - exp(-attachmentRate*len)));
			if (dist > len)
				{
				MrBayesPrint ("%s   Problem with new attachment point\n", spacer);
				goto errorExit;
				}
			lnProbForward += log(attachmentRate) - attachmentRate*dist - log(1.0 - exp(-attachmentRate*len));
			}
		else
			{
			dist = -(1.0 / attachmentRate) * log(1.0 - RandomNumber(seed));
			lnProbForward += log(attachmentRate) - attachmentRate*dist;
			}
		newPathLengthA = pathLengthA + dist;
		newPathLengthB = pathLengthB - dist;
		len = newPathLengthA - oldCeiling;
		lnProbReverse += log(attachmentRate) - attachmentRate*dist - log(1.0 - exp(-attachmentRate*len));
		}
	
	/* figure out the new attachment depth */
	if (direction == UP)
		{
		/* figured out above */
		}
	else	
		{
		if (isPathTerminusRoot == NO)
			{
			if (oldAttachmentDepth + dist < baseOfPath)
				newAttachmentDepth = oldAttachmentDepth + dist;
			else
				{
				dist -= baseOfPath - oldAttachmentDepth;
				newAttachmentDepth = baseOfPath - dist;
				}
			}
		else
			{
			newAttachmentDepth = oldAttachmentDepth + dist;
			}		
		}
	if (newAttachmentDepth < newCeiling)
		{
		MrBayesPrint ("%s   Problem with new attachment point 1 (dist = %1.10lf len = %lf\n", spacer, dist, len);
		goto errorExit;
		}
		
	/* find the new attachment node */
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->anc != NULL)
			{
			if (p->anc->anc != NULL)
				{
				if (p->marked == YES && p->nodeDepth < newAttachmentDepth && p->anc->nodeDepth > newAttachmentDepth)
					{
					newAttachmentNode = p;
					break;
					}
				}
			else
				{
				if (p->marked == YES && p->nodeDepth < newAttachmentDepth)
					{
					newAttachmentNode = p;
					break;
					}
				}
			}
		}	

#	if defined (DEBUG_SPRCLOCK)
	printf ("nodeToMove        = %d\n", nodeToMove->index);
	printf ("nodeToDetach      = %d\n", nodeToDetach->index);
	printf ("oldAttachmentNode = %d\n", oldAttachmentNode->index);
	printf ("nTipsOnSideA      = %d\n", nTipsOnSideA);
	printf ("nTipsOnSideB      = %d\n", nTipsOnSideB);
	printf ("pathLength        = %lf\n", pathLength);
	printf ("pathLengthA       = %lf\n", pathLengthA);
	printf ("baseOfPath        = %lf\n", baseOfPath);
	if (isPathTerminusRoot == NO)
		printf ("pathLengthB       = %lf\n", pathLengthB);
	else
		printf ("pathLengthB       = infinity\n");
	if (pathEndA != NULL)
		printf ("pathEndA          = %d\n", pathEndA->index);
	if (pathEndB != NULL)
		printf ("pathEndB          = %d\n", pathEndB->index);
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		printf ("%4d -- %d\n", p->index, p->marked);
		}
	printf ("oldCeiling        = %lf\n", oldCeiling);
	printf ("newCeiling        = %lf\n", newCeiling);
	if (direction == UP)
		printf ("dist up           = %lf\n", dist);
	else
		printf ("dist down         = %lf\n", dist);
	printf ("oldAttachmentDepth= %lf\n", oldAttachmentDepth);
	printf ("newAttachmentDepth= %lf\n", newAttachmentDepth);
	printf ("newAttachmentNode = %d\n", newAttachmentNode->index);
#	endif
			
	/* Check to see if the topology of the tree has changed. */
	if (newAttachmentNode == oldAttachmentNode || newAttachmentNode == nodeToDetach)
		topologyHasChanged = NO;
	else
		topologyHasChanged = YES;

	/* Make the rearrangement by detaching and reattaching the subtree. */
	if (topologyHasChanged == NO)
		{
		p = nodeToDetach->anc;
		nodeToDetach->nodeDepth = newAttachmentDepth;
		nodeToDetach->length = p->nodeDepth - nodeToDetach->nodeDepth;
		nodeToMove->length = nodeToDetach->nodeDepth - nodeToMove->nodeDepth;
		newAttachmentNode->length = newAttachmentNode->anc->nodeDepth - newAttachmentNode->nodeDepth;
		oldAttachmentNode->length = oldAttachmentNode->anc->nodeDepth - oldAttachmentNode->nodeDepth;
		if (newAttachmentNode->anc->anc == NULL)
			nodeToDetach->length = 0.0;
		}
	else
		{
		p = nodeToDetach->anc;
		if (p->left == nodeToDetach)
			p->left = oldAttachmentNode;
		else
			p->right = oldAttachmentNode;
		oldAttachmentNode->anc = p;
		oldAttachmentNode->length += nodeToDetach->length;
		nodeToDetach->left = nodeToDetach->right = nodeToDetach->anc = NULL;
		nodeToDetach->length = 0.0;
		nodeToDetach->nodeDepth = newAttachmentDepth;
		p = newAttachmentNode->anc;
		if (p->left == newAttachmentNode)
			{
			p->left = nodeToDetach;
			nodeToDetach->anc = p;
			nodeToDetach->left = newAttachmentNode;
			newAttachmentNode->anc = nodeToDetach;
			nodeToDetach->right = nodeToMove;
			nodeToMove->anc = nodeToDetach;
			}
		else
			{
			p->right = nodeToDetach;
			nodeToDetach->anc = p;
			nodeToDetach->right = newAttachmentNode;
			newAttachmentNode->anc = nodeToDetach;
			nodeToDetach->left = nodeToMove;
			nodeToMove->anc = nodeToDetach;
			}
		nodeToDetach->length = p->nodeDepth - nodeToDetach->nodeDepth;
		nodeToMove->length = nodeToDetach->nodeDepth - nodeToMove->nodeDepth;
		newAttachmentNode->length = nodeToDetach->nodeDepth - newAttachmentNode->nodeDepth;
		if (newAttachmentNode->anc->anc == NULL)
			nodeToDetach->length = 0.0;
		}
	
	/* Get the downpass sequence for the new tree if the topology has changed. */
	if (topologyHasChanged == YES)
		GetDownPass (t);
		
	/* Calculate another term in the proposal ratio */
	if (topologyHasChanged == YES)
		{
		lnProbForward += log(1.0 / nTipsOnSideA) + log(1.0 / nTipsOnSideB);

		nTipsOnSideA = nTipsOnSideB = 0;
		for (i=t->nNodes-1; i>=0; i--)
			{
			p = t->allDownPass[i];
			p->marked = NO;
			if (p == nodeToDetach)
				p->marked = YES;
			else if (p == nodeToMove)
				p->marked = 2;
				
			if (p->anc != NULL)
				{
				if (p != nodeToMove && p->anc->marked == YES)
					p->marked = YES;
				else if (p->anc->marked == 2)
					p->marked = 2;
				}
			if (p->left == NULL && p->right == NULL && p->marked == YES)
				nTipsOnSideA++;
			else if (p->left == NULL && p->right == NULL && p->marked == NO)
				{
				if (p != nodeToMove)
					nTipsOnSideB++;
				}
			}
		nTipsOnSideB++;
		lnProbReverse += log(1.0 / nTipsOnSideA) + log(1.0 / nTipsOnSideB);
		}

	/* Update proposal ratio. */
	(*lnProposalRatio) += lnProbReverse + lnProbForward;

	/* Set flags for update of transition probabilities. */
	oldAttachmentNode->upDateTi = YES;
	nodeToDetach->upDateTi = YES;
	nodeToMove->upDateTi = YES;
	newAttachmentNode->upDateTi = YES;
	if (nodeToMove->left != NULL && nodeToMove->right != NULL)
		nodeToMove->left->upDateTi = nodeToMove->right->upDateTi = YES;
		
	/* readjust branch lengths if they are too small */
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->left != NULL && p->right != NULL && p->anc != NULL)
			{
			if (p->nodeDepth - p->left->nodeDepth < BRLENS_MIN || p->nodeDepth - p->right->nodeDepth < BRLENS_MIN)
				{
				if (p->left->nodeDepth > p->right->nodeDepth)
					p->nodeDepth = p->left->nodeDepth + BRLENS_MIN;
				else
					p->nodeDepth = p->right->nodeDepth + BRLENS_MIN;
				p->left->length  = p->nodeDepth - p->left->nodeDepth;
				p->right->length = p->nodeDepth - p->right->nodeDepth;
				p->length = p->anc->nodeDepth - p->nodeDepth;
				p->upDateTi = p->left->upDateTi = p->right->upDateTi = YES;
				q = p;
				while (q->anc != NULL)
					{
					q->upDateCl = YES;
					q = q->anc;
					}
				}
			}
		}

	/* check all of the branch lengths */
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->anc != NULL)
			{
			if (p->anc->anc != NULL)
				{
				if (p->length < 0.0)
					{
					MrBayesPrint ("%s   Negative branch length in SPR clock\n", spacer);
					goto errorExit;
					}
				}
			}
		}
		
	/* Set flags for update of conditional likelihoods. */
	p = oldAttachmentNode->anc;
	while (p->anc != NULL)
		{
		p->upDateCl = YES;
		p = p->anc;
		}
	p = nodeToMove;
	while (p->anc != NULL)
		{
		p->upDateCl = YES;
		p = p->anc;
		}

	/* calculate prior ratio (part 2/2) */
	if (param->subParams[0]->paramId == BRLENS_CLOCK_UNI ||
		param->subParams[0]->paramId == BRLENS_CCLOCK_UNI)
		{
		/* uniformly distributed branch lengths */
		if (!strcmp(mp->treeHeightPr, "Exponential"))
			newLnPrior = log(mp->treeHeightExp) - mp->treeHeightExp * t->root->left->nodeDepth;
		else
			newLnPrior = mp->treeHeightGamma[1] * log(mp->treeHeightGamma[0]) - LnGamma(mp->treeHeightGamma[1]) + (mp->treeHeightGamma[1] - 1.0) * log(t->root->left->nodeDepth) - mp->treeHeightGamma[0] * t->root->left->nodeDepth;
		(*lnPriorRatio) = newLnPrior - oldLnPrior;
		}
	else if (param->subParams[0]->paramId == BRLENS_CLOCK_COAL ||
			 param->subParams[0]->paramId == BRLENS_CCLOCK_COAL)
		{
		/* coalescence prior */
		if (LnCoalescencePriorPr (t, &newLnPrior, theta, growth) == ERROR)
			{
			MrBayesPrint ("%s   Problem calculating prior for coalescence process\n", spacer);
			goto errorExit;
			}
		(*lnPriorRatio) = newLnPrior - oldLnPrior;
		}
	else
		{
		/* birth-death prior */
		if (LnBirthDeathPriorPr (t, &newLnPrior, sR, eR, sF) == ERROR)
			{
			MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
			goto errorExit;
			}
		if (!strcmp(mp->treeHeightPr, "Exponential"))
			newLnPrior += log(mp->treeHeightExp) - mp->treeHeightExp * t->root->left->nodeDepth;
		else
			newLnPrior += mp->treeHeightGamma[1] * log(mp->treeHeightGamma[0]) - LnGamma(mp->treeHeightGamma[1]) + (mp->treeHeightGamma[1] - 1.0) * log(t->root->left->nodeDepth) - mp->treeHeightGamma[0] * t->root->left->nodeDepth;
		(*lnPriorRatio) = newLnPrior - oldLnPrior;
		}

#	if defined (DEBUG_SPRCLOCK)
	printf ("After:\n");
	ShowNodes (t->root, 2, YES);
	getchar();
#	endif

	return (NO_ERROR);
	
	errorExit:
		printf ("Before:\n");
		ShowNodes (t->root, 2, YES);
		printf ("nodeToMove        = %d\n", nodeToMove->index);
		printf ("nodeToDetach      = %d\n", nodeToDetach->index);
		printf ("oldAttachmentNode = %d\n", oldAttachmentNode->index);
		printf ("nTipsOnSideA      = %d\n", nTipsOnSideA);
		printf ("nTipsOnSideB      = %d\n", nTipsOnSideB);
		printf ("pathLength        = %lf\n", pathLength);
		printf ("pathLengthA       = %lf\n", pathLengthA);
		printf ("baseOfPath        = %lf\n", baseOfPath);
		if (isPathTerminusRoot == NO)
			printf ("pathLengthB       = %lf\n", pathLengthB);
		else
			printf ("pathLengthB       = infinity\n");
		if (pathEndA != NULL)
			printf ("pathEndA          = %d\n", pathEndA->index);
		if (pathEndB != NULL)
			printf ("pathEndB          = %d\n", pathEndB->index);
		for (i=0; i<t->nNodes; i++)
			{
			p = t->allDownPass[i];
			printf ("%4d -- %d\n", p->index, p->marked);
			}
		printf ("oldCeiling        = %lf\n", oldCeiling);
		printf ("newCeiling        = %lf\n", newCeiling);
		if (direction == UP)
			printf ("dist up           = %lf\n", dist);
		else
			printf ("dist down         = %lf\n", dist);
		printf ("oldAttachmentDepth= %lf\n", oldAttachmentDepth);
		printf ("newAttachmentDepth= %lf\n", newAttachmentDepth);
		printf ("newAttachmentNode = %d\n", newAttachmentNode->index);
		return (ERROR);

}





int Move_Statefreqs (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* change pi */
	int			i, nStates;
	MrBFlt		dirichletParameters[64], *newPi, *oldPi, *priorPi, sum, alphaPi, x, y;


	/* get the values we need */
	nStates = param->nSubValues;
	priorPi = GetParamVals(param, chain, state[chain]);
	newPi = GetParamSubVals (param, chain, state[chain]);
	oldPi = GetParamSubVals (param, chain, state[chain] ^ 1);
	
#	if defined ASYMMETRY
	nStates /= 2;
	if (RandomNumber (seed) < 0.5)
		{
		newPi += 2;
		oldPi += 2;
		}
#	endif
	
	alphaPi = mvp[0];

	/* multiply old values with some large number to get new values close to the old ones */
	for (i=0; i<nStates; i++)
		dirichletParameters[i] = oldPi[i] * alphaPi;

	DirichletRandomVariable (dirichletParameters, newPi, nStates, seed);

	sum = 0.0;
	for (i=0; i<nStates; i++)
		{
		if (newPi[i] < 0.0001)
			newPi[i] = 0.0001;
		sum += newPi[i];
		}
	for (i=0; i<nStates; i++)
		newPi[i] /= sum;

	/* get proposal ratio */
	sum = 0.0;
	for (i=0; i<nStates; i++)
		sum += newPi[i]*alphaPi;
	x = LnGamma(sum);
	for (i=0; i<nStates; i++)
		x -= LnGamma(newPi[i]*alphaPi);
	for (i=0; i<nStates; i++)
		x += (newPi[i]*alphaPi-1.0)*log(oldPi[i]);
	sum = 0.0;
	for (i=0; i<nStates; i++)
		sum += oldPi[i]*alphaPi;
	y = LnGamma(sum);
	for (i=0; i<nStates; i++)
		y -= LnGamma(oldPi[i]*alphaPi);
	for (i=0; i<nStates; i++)
		y += (oldPi[i]*alphaPi-1.0)*log(newPi[i]);
	(*lnProposalRatio) = x - y;

	/* get prior ratio */
	sum = 0.0;
	for (i=0; i<nStates; i++)
		sum += priorPi[i];
	x = LnGamma(sum);
	for (i=0; i<nStates; i++)
		x -= LnGamma(priorPi[i]);
	y = x;
	for (i=0; i<nStates; i++)
		x += (priorPi[i]-1.0)*log(newPi[i]);
	for (i=0; i<nStates; i++)
		y += (priorPi[i]-1.0)*log(oldPi[i]);
	(*lnPriorRatio) = x - y;
		
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));
		
	/* Set update flags for cijks for all affected partitions. If this is a simple 4 X 4 model,
	   we don't take any hit, because we will never go into a general transition probability
	   calculator. However, for many models we do want to update the cijk flag, as the transition
	   probability matrices require diagonalizing the rate matrix. */
	for (i=0; i<param->nRelParts; i++)
		modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);
	
}





int Move_SwitchRate (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* change switch rate of covarion model using sliding window */
	
	int			i, isSPriorExp, isValidS, whichRate;
	MrBFlt		oldS, newS, window, minS, maxS, sExp=0.0, ran, *value;
	ModelParams *mp;

	/* decide which switching rate to change */
	if (RandomNumber(seed) < 0.5)
		whichRate = 0;
	else
		whichRate = 1;
		
	/* get size of window, centered on current switching rates value */
	window = mvp[0];

	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* get minimum and maximum values for switching rate */
	if (param->paramId == SWITCH_UNI)
		{
		minS = mp->covswitchUni[0];
		maxS = mp->covswitchUni[1];
		isSPriorExp = NO;
		}
	else
		{
		minS = 0.01;
		maxS = KAPPA_MAX;
		sExp = mp->covswitchExp;
		isSPriorExp = YES;
		}

	/* get old value of switching rate */
	value = GetParamVals(param, chain, state[chain]);
	newS = oldS = value[whichRate];

	/* change value for switching rate */
	ran = RandomNumber(seed);
	newS = oldS + window * (ran - 0.5);
	
	/* check that new value is valid */
	isValidS = NO;
	do
		{
		if (newS < minS)
			newS = 2* minS - newS;
		else if (newS > maxS)
			newS = 2 * maxS - newS;
		else
			isValidS = YES;
		} while (isValidS == NO);

	/* get proposal ratio */
	*lnProposalRatio = 0.0;
	
	/* get prior ratio */
	if (isSPriorExp == NO)
		*lnPriorRatio = 0.0;
	else
		*lnPriorRatio = -sExp * (newS - oldS);
	
	/* copy new switching rate value back */
	value[whichRate] = newS;

	/* Set update flags for all partitions that share this switching rate. Note that the conditional
	   likelihood update flags have been set before we even call this function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));

	/* Set update flags for cijks for all affected partitions. If this is a simple 4 X 4 model,
	   we don't take any hit, because we will never go into a general transition probability
	   calculator. However, for covarion, doublet, and codon models, we do want to update
	   the cijk flag. */
	for (i=0; i<param->nRelParts; i++)
		modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);

}





int Move_SwitchRate_M (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* change switch rate of covarion model using multiplier */

	int			i, isSPriorExp, isValidS, whichRate;
	MrBFlt		oldS, newS, minS, maxS, sExp=0.0, tuning, ran, factor, *value;
	ModelParams *mp;

	/* decide which switching rate to change */
	if (RandomNumber(seed) < 0.5)
		whichRate = 0;
	else
		whichRate = 1;
		
	/* get tuning parameter */
	tuning = mvp[0];

	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* get minimum and maximum values for switching rate */
	if (param->paramId == SWITCH_UNI)
		{
		minS = mp->covswitchUni[0];
		maxS = mp->covswitchUni[1];
		isSPriorExp = NO;
		}
	else
		{
		minS = 0.01;
		maxS = KAPPA_MAX;
		sExp = mp->covswitchExp;
		isSPriorExp = YES;
		}

	/* get old value of switching rate */
	value = GetParamVals(param, chain, state[chain]);
	newS = oldS = value[whichRate];

	/* change value for switching rate */
	ran = RandomNumber(seed);
	factor = exp(tuning * (ran - 0.5));
	newS = oldS * factor;
	
	/* check that new value is valid */
	isValidS = NO;
	do
		{
		if (newS < minS)
			newS = minS * minS / newS;
		else if (newS > maxS)
			newS = maxS * maxS / newS;
		else
			isValidS = YES;
		} while (isValidS == NO);

	/* get proposal ratio */
	*lnProposalRatio = log (newS / oldS);
	
	/* get prior ratio */
	if (isSPriorExp == NO)
		*lnPriorRatio = 0.0;
	else
		*lnPriorRatio = -sExp * (newS - oldS);
	
	/* copy new switching rate value back */
	value[whichRate] = newS;

	/* Set update flags for all partitions that share this switching rate. Note that the conditional
	   likelihood update flags have been set before we even call this function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));

	/* Set update flags for cijks for all affected partitions. If this is a simple 4 X 4 model,
	   we don't take any hit, because we will never go into a general transition probability
	   calculator. However, for covarion, doublet, and codon models, we do want to update
	   the cijk flag. */
	for (i=0; i<param->nRelParts; i++)
		modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);

}




int Move_Theta (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	int				isTPriorExp, isValidT;
	MrBFlt			oldT, newT, window, minT, maxT, thetaExp=0.0, ran, oldLnPrior, newLnPrior, growth;
	ModelParams 	*mp;
	ModelInfo		*m;
	Tree			*t;

	/* get size of window, centered on current theta value */
	window = mvp[0];

	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* get minimum and maximum values for theta */
	if (param->paramId == THETA_UNI)
		{
		minT = mp->thetaUni[0];
		maxT = mp->thetaUni[1];
		isTPriorExp = NO;
		}
	else
		{
		minT = 0.0001;
		maxT = 10000000;
		thetaExp = mp->thetaExp;
		isTPriorExp = YES;
		}

	/* get old value of theta */
	newT = oldT = *GetParamVals(param, chain, state[chain]);

	/* change value for theta */
	ran = RandomNumber(seed);
	newT = oldT + window * (ran - 0.5);
	
	/* check that new value is valid */
	isValidT = NO;
	do
		{
		if (newT < minT)
			newT = 2* minT - newT;
		else if (newT > maxT)
			newT = 2 * maxT - newT;
		else
			isValidT = YES;
		} while (isValidT == NO);

	/* get proposal ratio */
	*lnProposalRatio = 0.0;
	
	/* get prior ratio */
	t = GetTree(modelSettings[param->relParts[0]].brlens,chain,state[chain]);
	m = &modelSettings[param->relParts[0]];
	if (!strcmp(mp->growthPr, "Fixed"))
		growth = mp->growthFix;
	else
		growth = *(GetParamVals (m->growthRate, chain, state[chain]));
	if (LnCoalescencePriorPr (t, &oldLnPrior, oldT, growth) == ERROR)
		{
		MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
		return (ERROR);
		}
	if (LnCoalescencePriorPr (t, &newLnPrior, newT, growth) == ERROR)
		{
		MrBayesPrint ("%s   Problem calculating prior for birth-death process\n", spacer);
		return (ERROR);
		}
	if (isTPriorExp == NO)
		*lnPriorRatio = newLnPrior - oldLnPrior;
	else
		*lnPriorRatio = -thetaExp * (newT - oldT) + (newLnPrior - oldLnPrior);
	
	/* copy new theta value back */
	*GetParamVals(param, chain, state[chain]) = newT;

	return (NO_ERROR);

}





int Move_Tratio_Dir (Param *param, int chain, long int *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	/* change tratio using Dirichlet proposal */
	
	int			i;
	MrBFlt		oldK, alphaPi, *alphaDir, oldProp[2], newProp[2], dirParm[2], sum, x, y;
	ModelParams	*mp;

	/* get model params */
	mp = &modelParams[param->relParts[0]];

	/* get so called alphaPi parameter */
	alphaPi = mvp[0];

	/* get old value of kappa */
	oldK = *GetParamVals(param, chain, state[chain]);

	/* get Dirichlet parameters */
	alphaDir = mp->tRatioDir;

	/* calculate old ratesum proportions */
	oldProp[0] = oldK / (oldK + 1.0);
	oldProp[1] = 1.0 - oldProp[0];
	
	/* multiply old ratesum props with some large number to get new values close to the old ones */
	dirParm[0] = oldProp[0] * alphaPi;
	dirParm[1] = oldProp[1] * alphaPi;
	
	/* get new values */
	DirichletRandomVariable (dirParm, newProp, 2, seed);

	sum = 0.0;
	for (i=0; i<2; i++)
		{
		if (newProp[i] < DIR_MIN)
			newProp[i] = DIR_MIN;
		sum += newProp[i];
		}
	for (i=0; i<2; i++)
		newProp[i] /= sum;

	/* calculate and copy new kappa value back */
	*GetParamVals(param, chain, state[chain]) = newProp[0] / newProp[1];

	/* get proposal ratio */
	sum = 0.0;
	for (i=0; i<2; i++)
		sum += newProp[i]*alphaPi;
	x = LnGamma(sum);
	for (i=0; i<2; i++)
		x -= LnGamma(newProp[i]*alphaPi);
	for (i=0; i<2; i++)
		x += (newProp[i]*alphaPi-1.0)*log(oldProp[i]);
	sum = 0.0;
	for (i=0; i<2; i++)
		sum += oldProp[i]*alphaPi;
	y = LnGamma(sum);
	for (i=0; i<2; i++)
		y -= LnGamma(oldProp[i]*alphaPi);
	for (i=0; i<2; i++)
		y += (oldProp[i]*alphaPi-1.0)*log(newProp[i]);
	(*lnProposalRatio) = x - y;

	/* get prior ratio */
	x = y = 0.0;
	for (i=0; i<2; i++)
		x += (alphaDir[i]-1.0)*log(newProp[i]);
	for (i=0; i<2; i++)
		y += (alphaDir[i]-1.0)*log(oldProp[i]);
	(*lnPriorRatio) = x - y;
		
	/* Set update flags for all partitions that share this kappa. Note that the conditional
	   likelihood update flags have been set before we even call this function. */
	for (i=0; i<param->nRelParts; i++)
		TouchAllTreeNodes(GetTree(modelSettings[param->relParts[i]].brlens,chain,state[chain]));

	/* Set update flags for cijks for all affected partitions. If this is a simple 4 X 4 model,
	   we don't take any hit, because we will never go into a general transition probability
	   calculator. However, for covarion, doublet, and codon models, we do want to update
	   the cijk flag. */
	for (i=0; i<param->nRelParts; i++)
		modelSettings[param->relParts[i]].upDateCijk[chain][state[chain]] = YES;

	return (NO_ERROR);

}




/*----------------------------------------------------------------
|
|	Move_UnrootedSlider: This proposal mechanism changes the topology and
|      branch lengths of an unrooted tree. 
|
|      Programmed by JH 2003-08-13
|
----------------------------------------------------------------*/
int Move_UnrootedSlider (Param *param, int chain, long *seed, MrBFlt *lnPriorRatio, MrBFlt *lnProposalRatio, MrBFlt *mvp)

{

	int			i, topologyHasChanged, isVPriorExp, isBranchAnc, direction=0, stopPathGen, moveToWhichPath, stopLoop;
	MrBFlt		tuning, expParam, minV, maxV, brlensExp=0.0, pathLength, pathLength1, pathLength2, oldM, newM, dist, sum, excess;
	TreeNode	*v, *w, *p, *q, *a, *b, *c, *d, *newAttachmentNode;
	Tree		*t;
	ModelParams *mp;

	/* set the tuning parameter */
	tuning = mvp[0];
	
	/* set the exponential parameter */
	expParam = mvp[1];
	
	/* get tree */
	t = GetTree (param, chain, state[chain]);
#	if defined (DEBUG_UNROOTED_SLIDER)
	MrBayesPrint ("Before:\n");
	ShowNodes (t->root, 3, NO);
#	endif

	/* initialize log prior and log proposal probabilities */
	*lnPriorRatio = *lnProposalRatio = 0.0;
	
	/* get model params */
	mp = &modelParams[param->relParts[0]];
	
	/* max brlen */
	if (param->subParams[0]->paramId == BRLENS_UNI)
		{
		maxV = mp->brlensUni[1];
		isVPriorExp = NO;
		}
	else
		{
		maxV = BRLENS_MAX;
		brlensExp = mp->brlensExp;
		isVPriorExp = YES;
		}
		
	/* min brlen */
	minV = BRLENS_MIN;
	
	/* Calculate log prior probability before the branch lengths of the tree have 
	   been changed. */
	if (isVPriorExp == YES)
		{
		for (i=0; i<t->nNodes; i++)
			{
			p = t->allDownPass[i];
			if (p->anc != NULL)
				*lnPriorRatio -= log(brlensExp) - brlensExp * p->length;
			}
		}

	/* Pick a branch. This branch is marked at the top as "v" and at the bottom as "w". 
	   We also note whether the branch is the ancestral one, or not. Finally, this branch
	   will eventually have its length changed via a Larget & Simon type of contraction/
	   expansion. */
	do
		{
		v = t->allDownPass[(int)(RandomNumber(seed) * t->nNodes)];
		} while (v->anc == NULL);
	w = v->anc;
	if (w->anc == NULL)
		isBranchAnc = YES;
	else
		isBranchAnc = NO;
#	if defined (DEBUG_UNROOTED_SLIDER)
	MrBayesPrint ("v=%d w=%d isBranchAnc=%d\n", v->index, w->index, isBranchAnc);
#	endif

	/* mark path */
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		p->marked = NO;
		p->x = p->y = 0;
		}
	v->marked = w->marked = YES;
	for (i=0; i<2; i++)
		{
		if (isBranchAnc == NO)
			{
			if (i == 0)
				direction = DOWN;
			else
				direction = UP;
			p = w;
			}
		else
			{
			direction = UP;
			p = v;
			}
		stopPathGen = NO;
		do
			{
			p->marked = YES;
			p->x = i + 1;
			if (direction == DOWN && p->anc != NULL)
				{
				p = p->anc;
				}
			else if (direction == DOWN && p->anc == NULL)
				{
				stopPathGen = YES;
				}
			else if (direction == UP && (p->left != NULL && p->right != NULL))
				{
				if (p->left->marked == NO && p->right->marked == NO)
					{
					if (RandomNumber(seed) < 0.5)
						p = p->left;
					else
						p = p->right;
					}
				else if (p->left->marked == NO && p->right->marked == YES)
					p = p->left;
				else if (p->left->marked == YES && p->right->marked == NO)
					p = p->right;
				else
					{
					MrBayesPrint ("%s   ERROR: All nodes above are marked\n", spacer);
					return (ERROR);
					}
				}
			else if (direction == UP && (p->left == NULL || p->right == NULL))
				{
				stopPathGen = YES;
				}
			else
				{
				MrBayesPrint ("%s   ERROR: Should not be here in UnrootedNodeSlider\n", spacer);
				return (ERROR);
				}
			if (direction == DOWN && stopPathGen == NO)
				if (RandomNumber(seed) < 0.5)
					direction = UP;
			} while (stopPathGen == NO);
		}
	if (isBranchAnc == NO)
		{
		v->marked = NO;
		w->marked = YES;
		v->x = 0;
		w->x = w->anc->x;
		}
	else
		{
		v->marked = w->marked = NO;
		v->x = w->x = 0;
		}
	for (i=0; i<t->nNodes; i++) /* mark any kinks in the path by setting p->y = 1 */
		{
		p = t->allDownPass[i];
		if (p != v && p != w && p->left != NULL && p->right != NULL)
			{
			if (p->x > 0)
				{
				if (p->left->x == p->right->x && p->x == p->left->x)
					{
					p->marked = NO;
					p->y = 1;
					}
				}
			}
		}
#	if defined (DEBUG_UNROOTED_SLIDER)
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->marked == YES || p->x > 0)
			printf ("%4d %4d %4d \n", p->index, p->marked, p->x);
		}
#	endif
		
	/* Calculate the path lengths on the two sides of
	   the branch designated with v-w. */
	pathLength1 = pathLength2 = 0.0;
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->marked == YES && p->y == 0)
			{
			if (p->x == 1)
				pathLength1 += p->length;
			else if (p->x == 2)
				pathLength2 += p->length;
			else
				{
				MrBayesPrint ("%s   ERROR: Marked node should have x=1 or x=2\n", spacer);
				return (ERROR);
				}
			}
		}
#	if defined (DEBUG_UNROOTED_SLIDER)
	printf ("pathLength1=%lf pathLength2=%lf\n", pathLength1, pathLength2);
#	endif
		
	/* Change the length of branch marked by v and w. While we are at it, we
	   mark this branch for update of the transition probability (the branch
	   changed its length, so the transition probabilities will be different).
	   We will also mark all of the branches on a path from v to the root as
	   in need of updating for the conditional likelihoods. */
	oldM = v->length;
	newM = oldM * exp(tuning * (RandomNumber(seed) - 0.5));
	if (newM < minV)
		newM = minV;
	else if (newM > maxV)
		newM = maxV;
	v->length = newM;
	v->upDateTi = YES;
	MarkClsBelow (v);

	/* Here we calculate the first part of the proposal ratio. We are changing the length of
	   the branch marked at the top by node v and at the bottom by node w. The length is
	   changed by multiplying the old branch length by e^(tuning * (r - 1/2)); that is, we
	   use the Larget & Simon type of move. The proposal ratio is given as: */
	*lnProposalRatio += log(newM) - log(oldM);
	
	/* Pick a direction to slide the branch in. */
	moveToWhichPath = 1;
	if (RandomNumber(seed) < 0.5)
		moveToWhichPath = 2;
		
	/* Pick an amount to slide. We slide the branch an exponentially-distributed amount
	   from its starting position. We condition on the length of the path in the direction
	   we are sliding. Hence, the probability density for sliding the branch is
	   
	   f(x) = expParam * exp(expParam * x) / (1.0 - exp(expParam * pathLength) 
	   
	   where x is the amount to slide, expParam is the parameter of the exponential
	   distribution, and pathLength is the length of the path in direction we are sliding. */
	if (moveToWhichPath == 1)
		pathLength = pathLength1;
	else
		pathLength = pathLength2;
	dist = -(1.0 / expParam) * log(1.0 - RandomNumber(seed) * (1.0 - exp(-expParam * pathLength)));
#	if defined (DEBUG_UNROOTED_SLIDER)
	printf ("dist=%lf pathLength=%lf\n", dist, pathLength);
#	endif
	
	/* Find the new attachment branch. This is a pain in the neck. */
	if (isBranchAnc == NO)
		p = w;
	else
		p = v;
	stopLoop = NO;
	sum = 0.0;
	do
		{
		q = NULL;
		if (p->left != NULL)
			if (p->left->x == moveToWhichPath)
				{
				q = p->left;
				direction = UP;
				}
		if (p->right != NULL)
			if (p->right->x == moveToWhichPath)
				{
				if (q == NULL)
					{
					q = p->right;
					direction = UP;
					}
				else
					{
					MrBayesPrint ("%s   ERROR: At least two inappropriately marked nodes\n", spacer);
					return (ERROR);
					}
				}
		if (p->anc != NULL)
			if (p->anc->x == moveToWhichPath)
				{
				if (q == NULL)
					{
					q = p->anc;
					direction = DOWN;
					}
				else
					{
					MrBayesPrint ("%s   ERROR: At least two inappropriately marked nodes\n", spacer);
					return (ERROR);
					}
				}
		if (q == NULL)
			{
			MrBayesPrint ("%s   ERROR: Could not find an appropriately marked node\n", spacer);
			return (ERROR);
			}
			
		excess = dist - sum;
		if (direction == UP)
			sum += q->length;
		else if (direction == DOWN && p->y == 0)
			sum += p->length;
		p->x = 0;
		if (sum > dist)
			{
			stopLoop = YES;
			if (direction == UP)
				p = q;
			}
		else
			p = q;

		} while (stopLoop == NO);
	newAttachmentNode = p;
	
	/* Decide whether the topology of the tree has changed. If we are sliding
	   the branch below w, then the topology has not changed if newAttachmentNode =
	   w. If we are sliding the branch above w, then the topology does not change
	   if newAttachmentNode->anc = w. Note that if the branch we are sliding
	   is the ancestral one, then the topology does not change if 
	   newAttachmentNode->anc = v. */
	topologyHasChanged = YES;
	if (isBranchAnc == NO)
		{
		if (newAttachmentNode->anc != NULL)
			if (newAttachmentNode->anc == w)
				topologyHasChanged = NO;
		if (newAttachmentNode == w)
			topologyHasChanged = NO;
		}
	else
		{
		if (newAttachmentNode->anc != NULL)
			if (newAttachmentNode->anc == v)
				topologyHasChanged = NO;
		}
		
	/* Move the branch to the new attachment point. We need to keep track of a lot
	   of pointers, and also make certain to mark updates for conditional likelihoods
	   and transition probabilities on the fly. */
	if (isBranchAnc == NO)
		{
		if (topologyHasChanged == YES)
			{
			b = w->anc;
			if (w->left == v)
				a = w->right;
			else
				a = w->left;
			if (b->left == w)
				{
				b->left = a;
				a->anc = b;
				a->length += w->length;
				}
			else
				{
				b->right = a;
				a->anc = b;
				a->length += w->length;
				}
			a->upDateTi = YES;
			MarkClsBelow (a);
				
			if (newAttachmentNode->anc == NULL)
				{
				MrBayesPrint ("%s   ERROR: This isn't right!\n", spacer);
				return (ERROR);
				}
			else
				q = newAttachmentNode->anc;
			if (q->left == newAttachmentNode)
				{
				q->left = w;
				w->anc = q;
				w->left = newAttachmentNode;
				w->right = v;
				p->anc = v->anc = w;
				}
			else
				{
				q->right = w;
				w->anc = q;
				w->right = newAttachmentNode;
				w->left = v;
				newAttachmentNode->anc = v->anc = w;
				}
			w->length = excess;
			newAttachmentNode->length -= excess;
			if (newAttachmentNode->length < 0.0)
				{
				MrBayesPrint ("%s   ERROR: Negative branch length on p\n", spacer);
				return (ERROR);
				}
			if (w->length < 0.0)
				{
				MrBayesPrint ("%s   ERROR: Negative branch length on w\n", spacer);
				return (ERROR);
				}
			newAttachmentNode->upDateTi = YES;
			w->upDateTi = YES;
			MarkClsBelow (v);
			}
		else
			{
			if (w->left == v)
				a = w->right;
			else
				a = w->left;
			if (newAttachmentNode == w)
				{
				w->length -= excess;
				a->length += excess;
				}
			else
				{
				w->length += excess;
				a->length -= excess;
				}
			if (w->length < 0.0)
				{
				MrBayesPrint ("%s   ERROR: Negative branch length on w (2)\n", spacer);
				return (ERROR);
				}
			if (a->length < 0.0)
				{
				MrBayesPrint ("%s   ERROR: Negative branch length on a\n", spacer);
				return (ERROR);
				}
			a->upDateTi = YES;
			w->upDateTi = YES;
			MarkClsBelow (a);
			}
		}
	else
		{
		if (topologyHasChanged == YES)
			{
			for (i=0; i<t->nNodes; i++)
				{
				p = t->allDownPass[i];
				p->marked = NO;
				}
			p = newAttachmentNode;
			while (p != v && p->anc != NULL)
				{
				p->marked = YES;
				p = p->anc;
				}
			do
				{
				if (v->left == NULL || v->right == NULL)
					{
					MrBayesPrint ("%s   ERROR: v->left or v->right is null\n", spacer);
					return (ERROR);
					}
				if (v->left->marked == YES && v->right->marked == NO)
					{
					a = v->left;
					b = v->right;
					}
				else if (v->left->marked == NO && v->right->marked == YES)
					{
					a = v->right;
					b = v->left;
					}
				else if (v->left->marked == YES && v->right->marked == YES)
					{
					MrBayesPrint ("%s   ERROR: v->left and v->right are marked\n", spacer);
					return (ERROR);
					}
				else
					{
					MrBayesPrint ("%s   ERROR: v->left and v->right are not marked\n", spacer);
					return (ERROR);
					}
				if (a->left == NULL || a->right == NULL)
					{
					MrBayesPrint ("%s   ERROR: a's descendents should not be null\n", spacer);
					return (ERROR);
					}
				if (a->left->marked == YES && a->right->marked == NO)
					{
					c = a->right;
					d = a->left;
					}
				else if (a->left->marked == NO && a->right->marked == YES)
					{
					c = a->left;
					d = a->right;
					}
				else
					{
					MrBayesPrint ("%s   ERROR: one of a's descendents should be marked\n", spacer);
					return (ERROR);
					}
				
				v->left = d;
				v->right = a;
				a->anc = d->anc = v;
				a->left = c;
				a->right = b;
				b->anc = c->anc = a;
				a->marked = NO;
				b->length += a->length;
				if (d == newAttachmentNode)
					{
					d->length -= excess;
					a->length = excess;
					}
				else
					{
					d->length *= 0.5;
					a->length = d->length;
					}
				b->upDateTi = YES;
				d->upDateTi = YES;
				a->upDateTi = YES;
				a->upDateCl = YES; 
				v->upDateCl = YES; 
				} while (v->left != newAttachmentNode && v->right != newAttachmentNode);
			}
		else
			{
			if (v->left == newAttachmentNode)
				a = v->right;
			else
				a = v->left;
			newAttachmentNode->length -= excess;
			a->length += excess;
			newAttachmentNode->upDateTi = YES;
			a->upDateTi = YES;
			v->upDateCl = YES;
			}
		}
		
	/* Here we calculate the second part of the proposal ratio. Note that after changing the length
	   of the selected node (the proposal ratio for this part is taken care of in part 1), that
	   we then (1) choose a direction to slide the node and (2) decide on an amount to slide, then sliding
	   the node the appropriate amount. In part 1, we choose each direction with probability 1/2. Because
	   the forward and backward move will contain this 1/2, they cancel out in the Hasting's ratio, and
	   we ignore this part of the move from here on. However, the second part is more complicated. We
	   slide the node an exponentially distributed amount, conditional on not sliding the node past
	   the tip (that is, the amount we slide cannot be larger than the path in that direction). I think
	   that the following correctly accounts for this move. */
	if (moveToWhichPath == 1)
		{
		*lnProposalRatio += ( log(expParam) - expParam * dist - log(1.0 - exp(-expParam * (pathLength2 + dist))) ) - 
							( log(expParam) - expParam * dist - log(1.0 - exp(-expParam * pathLength1)) );
		}
	else
		{
		*lnProposalRatio += ( log(expParam) - expParam * dist - log(1.0 - exp(-expParam * (pathLength1 + dist))) ) - 
							( log(expParam) - expParam * dist - log(1.0 - exp(-expParam * pathLength2)) );
		}

	/* get downpass sequence if tree topology has changed */
	if (topologyHasChanged == YES)
		{
		GetDownPass (t);
		}
#	if defined (DEBUG_UNROOTED_SLIDER)
	MrBayesPrint ("After:\n");
	ShowNodes (t->root, 3, NO);
#	endif

	/* Calculate log prior probability after the branch lengths of the tree have 
	   been changed. */
	if (isVPriorExp == YES)
		{
		for (i=0; i<t->nNodes; i++)
			{
			p = t->allDownPass[i];
			if (p->anc != NULL)
				*lnPriorRatio += log(brlensExp) - brlensExp * p->length;
			}
		}
				
	return (NO_ERROR);

}





void NodeToNodeDistances (Tree *t, TreeNode *fromNode)

{

	int				i;
	TreeNode		*p;
	
	/* set all distances to 0.0 and also set marks on all nodes to NO */
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		p->x = NO;
		p->d = 0.0;
		}
		
	/* find distances, and mark path, below "fromNode" */
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p == fromNode)
			{
			p->x = YES;
			}
		if (p->left != NULL && p->right != NULL && p->anc != NULL)
			{
			if (p->left->x == YES)
				{
				p->x = YES;
				p->d = p->left->d + p->left->length;
				}
			else if (p->right->x == YES)
				{
				p->x = YES;
				p->d = p->right->d + p->right->length;
				}
			}
		}
		
	/* find all other distances */
	for (i=t->nNodes-1; i>=0; i--)
		{
		p = t->allDownPass[i];
		if (p->anc == NULL)
			{
			if (p == fromNode)
				p->d = 0.0;
			else
				p->d = p->left->d + p->left->length;
			}
		else
			{
			if (p->x == NO)
				{
				p->d = p->anc->d + p->length;
				}
			}
		}

}





int NumNonExcludedChar (void)

{

	int		i, n;
	
	/* count number of non-excluded characters */
	n = 0;
	for (i=0; i<numChar; i++)
		{
		if (charInfo[i].isExcluded == NO)
			{
			n++;
			}
		}
	
	return n;
	
}





int NumNonExcludedTaxa (void)

{

	int			i, j, nt, howMany;
	char		tempName[100];
	
	/* count number of non-excluded taxa */
	nt = 0;
	for (i=0; i<numTaxa; i++)
		{
		if (taxaInfo[i].isDeleted == NO)
			{
			nt++;
			}
		}
		
	/* allocate memory */
	if (memAllocs[ALLOC_LOCTAXANAMES] == YES)
		{
		MrBayesPrint ("%s   localTaxonNames not free in NumNonExcludedTaxa\n", spacer);
		return (0);
		}
	localTaxonNames = (char *)malloc((size_t) (nt * 100 * sizeof(char)));
	if (!localTaxonNames)
		{
		MrBayesPrint ("%s   Problem allocating localTaxonNames (%d)\n", spacer, nt * 100 * sizeof(char));
		return (ERROR);
		}
	for (i=0; i<nt*100; i++)
		{
		localTaxonNames[i] = ' ';
		if (i == nt*100 - 1)
			localTaxonNames[i] = '\0';
		}
	memAllocs[ALLOC_LOCTAXANAMES] = YES;

	if (memAllocs[ALLOC_LOCTAXAAGES] == YES)
		{
		MrBayesPrint ("%s   localTaxonAges not free in NumNonExcludedTaxa\n", spacer);
		return (0);
		}
	localTaxonAges = (MrBFlt *)malloc((size_t) (nt * sizeof(MrBFlt)));
	if (!localTaxonAges)
		{
		MrBayesPrint ("%s   Problem allocating localTaxonAges (%d)\n", spacer, nt * sizeof(char));
		return (ERROR);
		}
	for (i=0; i<nt; i++)
		{
		localTaxonAges[i] = -1.0;
		}
	memAllocs[ALLOC_LOCTAXAAGES] = YES;
		
	/* store names and ages of non-excluded taxa */
	numCalibratedLocalTaxa = 0;
	for (i=j=0; i<numTaxa; i++)
		{
		if (taxaInfo[i].isDeleted == NO)
			{
			if (GetNameFromString (taxaNames, tempName, i+1) == ERROR)
				{
				MrBayesPrint ("%s   Error getting taxon names \n", spacer);
				return (0);
				}
			if (AddToString (tempName, localTaxonNames, &howMany) == ERROR)
				{
				MrBayesPrint ("%s   Problem adding charset %s to list\n", spacer, tempName);
				return (0);
				}
			localTaxonAges[j++] = taxaInfo[i].taxaAge;
			if (taxaInfo[i].taxaAge > -0.00001)
				numCalibratedLocalTaxa ++;
			}
		}
		
	/* reset outgroup, if necessary */
	if (taxaInfo[outGroupNum].isDeleted == YES)
		{
		localOutGroup = 0;
		}
	else
		{
		howMany = 0;
		for (i=0; i<numTaxa; i++)
			{
			if (i == outGroupNum)
				{
				localOutGroup = howMany;
				break;
				}
			if (taxaInfo[i].isDeleted == NO)
				howMany++;
			}
		}
		
	
#	if 0
	/* show non-excluded taxa */
	for (i=0; i<nt; i++)
		{
		if (GetNameFromString (localTaxonNames, tempName, i+1) == ERROR)
			{
			MrBayesPrint ("%s   Error getting taxon names \n", spacer);
			return (0);
			}
		MrBayesPrint ("%s   %4d %s\n", spacer, i+1, tempName);
		}
#	endif
		
	return (nt);
	
}





/*----------------------------------------------------------------------
|
|	OpenMBPrintFile: Open a file the first time for printing
|
------------------------------------------------------------------------*/
FILE *OpenNewMBPrintFile (char *fileName)

{

	int		i, j, overWrite, len;
	char	s[101];
	FILE    *fp;

	/* Open file, use noWarn to determine if the user should be prompted
	   to have the file over-written or appended. */
	if (noWarn == YES)
		{
		/* overwrite file, if already present */		
		if ((fp = fopen(fileName, "r")) != NULL)
			{
			MrBayesPrint ("%s   Overwriting file \"%s\"\n", spacer, fileName);
			fclose (fp);
			}
		return (OpenTextFileW(fileName));
		}
	else
		{
		/* prompt user if file is already present */
		if ((fp = fopen(fileName,"r")) != NULL)
			{
			fclose (fp);
			MrBayesPrint ("\n");
			MrBayesPrint ("%s   File \"%s\" already exists\n", spacer, fileName);
			MrBayesPrint ("%s   Overwrite information in this file (yes/no): ", spacer);

			overWrite = NO;
			for (i=0; i<10; i++)
				{
				if (fgets (s, 100, stdin) == NULL)
					{
					MrBayesPrint ("%s   Failed to retrieve answer, file will be appended to\n", spacer);
					overWrite = NO;
					break;
					}
				len = (int) strlen (s);
				for (j=0; j<len; j++)
					s[j] = tolower(s[j]);
							
				if ((s[0] == 'y' && len == 2) || (s[0] == 'y' && s[1] == 'e' && len == 3) || (s[0] == 'y' && s[1] == 'e' && s[2] == 's' && len == 4))
					{
					overWrite = YES;
					break;
					}
				else if ((s[0] == 'n' && len == 2) || (s[0] == 'n' && s[1] == 'o' && len == 3))
					{
					overWrite = NO;
					break;
					}
				if (i<9)
					MrBayesPrint ("%s   Enter Yes or No: ", spacer);
				else
					MrBayesPrint ("%s   MrBayes does not understand, file will be appended to\n", spacer);
				}
			
			if (overWrite == YES)
				{
				MrBayesPrint ("%s   Overwriting file \"%s\"\n", spacer, fileName);
				return (OpenTextFileW(fileName));
				}
			else
				{
				MrBayesPrint ("%s   Appending to file \"%s\"\n", spacer, fileName);
				return (OpenTextFileA(fileName));
				}
			}

		else
			{
			/* file is not present */
			return (OpenTextFileW(fileName));
			}
		}
}





int PickProposal (long int *seed)

{
	
	MrBFlt		ran;
	int			i;

	ran = RandomNumber (seed);
	
	for (i=0; moves[i].cumProposalProb <= ran; i++)
		;
		
	return i;

}





int PosSelProbs (TreeNode *p, int division, int chain)

{

	int				c, j, k, nStates;
	MrBFlt			catLike, like[100], *bs, *omegaCatFreq, *omega,
					posProb, *ps, sum;
	CLFlt			*clP;
	ModelInfo		*m;
	
	m = &modelSettings[division];

	/* number of states */
	nStates = m->numModelStates;

	/* find conditional likelihood pointer */
	clP = condLikePtr[chain][p->index] + m->condLikeStart + Bit(division, &p->clSpace[0]) * condLikeRowSize;
	
	/* find base frequencies */
	bs = GetParamSubVals (m->stateFreq, chain, state[chain]);
	
	/* find category frequencies */
	omegaCatFreq = GetParamSubVals (m->omega, chain, state[chain]);
	
	/* get category omegas */
	omega = GetParamVals (m->omega, chain, state[chain]);

	/* find posSelProbs */
	ps = posSelProbs + m->compCharStart;
	for (c=m->numDummyChars; c<m->numChars; c++)
		{
		sum = 0.0;
		for (k=0; k<m->numOmegaCats; k++)
			{
			like[k] = 0.0;
			catLike = 0.0;
			for (j=0; j<nStates; j++)
				catLike += clP[j] * bs[j];
			like[k] = catLike * omegaCatFreq[k];
			sum += like[k];
			clP += nStates;
			}
		posProb = 0.0;
		for (k=0; k<m->numOmegaCats; k++)
			{
			if (omega[k] > 1.0)
				posProb += like[k] / sum;
			}
		ps[c] = posProb;
		}

	return NO_ERROR;
	
}





/*----------------------------------------------------------------------
|
|	PreparePrintFiles: Prepare .t, .p, .cal, and .mcmc files for printing
|
------------------------------------------------------------------------*/
int PreparePrintFiles (void)

{

	int			i, k, n;
	char		localFileName[100], fileName[100];

	/* Allocate space for file pointers */
	n = chainParams.numRuns;

	if (memAllocs[ALLOC_FILEPOINTERS] == YES)
		{
		MrBayesPrint ("%s   File pointers already allocated in PreparePrintFiles\n", spacer);
		return ERROR;
		}
	fpMcmc = NULL;
	fpParm = NULL;
	fpTree = fpCal = NULL;	
	fpParm = (FILE **) calloc (n, sizeof (FILE *));	
	if (fpParm == NULL)
		{
		MrBayesPrint ("%s   Could not allocate fpParm in PreparePrintFiles\n", spacer);
		return ERROR;
		}
	memAllocs[ALLOC_FILEPOINTERS] = YES;
	fpTree = (FILE ***) calloc (n, sizeof (FILE **));	
	if (fpTree == NULL)
		{
		MrBayesPrint ("%s   Could not allocate fpTree in PreparePrintFiles\n", spacer);
		return ERROR;
		}
	fpTree[0] = (FILE **) calloc (numTrees*n, sizeof (FILE *));
	if (fpTree[0] == NULL)
		{
		MrBayesPrint ("%s   Could not allocate fpTree[0] in PreparePrintFiles\n", spacer);
		return ERROR;
		}
	for (i=1; i<n; i++)
		fpTree[i] = fpTree[0] + i*numTrees;
	if (numCalibratedTrees > 0)
		{
		fpCal = (FILE ***) calloc (n, sizeof (FILE **));	
		if (fpCal == NULL)
			{
			MrBayesPrint ("%s   Could not allocate fpCal in PreparePrintFiles\n", spacer);
			return ERROR;
			}
		fpCal[0] = (FILE **) calloc (numCalibratedTrees*n, sizeof (FILE *));
		if (fpCal[0] == NULL)
			{
			MrBayesPrint ("%s   Could not allocate fpCal[0] in PreparePrintFiles\n", spacer);
			return ERROR;
			}
		for (i=1; i<n; i++)
			fpCal[i] = fpCal[0] + i*numCalibratedTrees;
		}
	memAllocs[ALLOC_FILEPOINTERS] = YES;

	/* Get root of local file name */
	strcpy (localFileName, chainParams.chainFileName);

	/* Prepare the .p, .t, and .cal files */
	for (n=0; n<chainParams.numRuns; n++)
		{
		k = n;

		if (chainParams.numRuns == 1)
			sprintf (fileName, "%s.p", localFileName);
		else
			sprintf (fileName, "%s.run%d.p", localFileName, n+1);
#		if defined (MPI_ENABLED)
		if (proc_id == 0)
#		endif
		if ((fpParm[k] = OpenNewMBPrintFile (fileName)) == NULL)
			return (ERROR);

		for (i=0; i<numTrees; i++)
			{
			if (numTrees == 1 && chainParams.numRuns == 1)
				sprintf (fileName, "%s.t", localFileName);
			else if (numTrees > 1 && chainParams.numRuns == 1)
				sprintf (fileName, "%s.tree%d.t", localFileName, i+1);
			else if (numTrees == 1 && chainParams.numRuns > 1)
				sprintf (fileName, "%s.run%d.t", localFileName, n+1);
			else
				sprintf (fileName, "%s.tree%d.run%d.t", localFileName, i+1, n+1);
#			if defined (MPI_ENABLED)
			if (proc_id == 0)
#			endif
			if ((fpTree[k][i] = OpenNewMBPrintFile (fileName)) == NULL)
				return (ERROR);
			}

		for (i=0; i<numCalibratedTrees; i++)
			{
			if (numCalibratedTrees == 1 && chainParams.numRuns == 1)
				sprintf (fileName, "%s.t", localFileName);
			else if (numCalibratedTrees > 1 && chainParams.numRuns == 1)
				sprintf (fileName, "%s.tree%d.t", localFileName, i+1);
			else if (numCalibratedTrees == 1 && chainParams.numRuns > 1)
				sprintf (fileName, "%s.run%d.t", localFileName, n+1);
			else
				sprintf (fileName, "%s.tree%d.run%d.t", localFileName, i+1, n+1);
#			if defined (MPI_ENABLED)
			if (proc_id == 0)
#			endif
			if ((fpCal[k][i] = OpenNewMBPrintFile (fileName)) == NULL)
				return (ERROR);
			}
		}

	/* Prepare the .mcmc file */
	if (chainParams.mcmcDiagn == YES)
		{
		sprintf (fileName, "%s.mcmc", chainParams.chainFileName);
#		if defined (MPI_ENABLED)
		if (proc_id == 0)
#		endif
		if ((fpMcmc = OpenNewMBPrintFile (fileName)) == NULL)
			return (ERROR);
		}

	return (NO_ERROR);
}




/*----------------------------------------------------------------
|
|	PrintAncStates_Bin: print ancestral states after final pass
|		Binary model with or without rate variation
|
-----------------------------------------------------------------*/
int PrintAncStates_Bin (TreeNode *p, int division, int chain)

{

	int				c, i, k;
	MrBFlt			*bs, freq;
	CLFlt			*clFP, *cL, sum;
	char			tempStr[200];
	ModelInfo		*m;
	
	/* find model settings for this division */
	m = &modelSettings[division];

	/* find state frequencies */
	bs = GetParamSubVals (m->stateFreq, chain, state[chain]);
	
	/* find frequencies of rate categories */
	freq =  1.0 /  m->numGammaCats;
	
	/* find the conditional likelihoods from the final pass */
	clFP = condLikePtr[chain][p->index] + m->condLikeStart + (1 ^ Bit(division, &p->clSpace[0])) * condLikeRowSize;

	/* find the preallocated working space */
	cL = ancStateCondLikes;
	
	/* cycle over the compressed characters */
	for (c=i=0; c<m->numChars; c++)
		{
		cL[0] = cL[1] = 0.0;
		for (k=0; k<m->numGammaCats; k++)
			{
			cL[0] += clFP[i++];
			cL[1] += clFP[i++];
			}
		cL[0] *= (CLFlt) (bs[0] * freq);
		cL[1] *= (CLFlt) (bs[1] * freq);
		sum = cL[0] + cL[1];
		cL[0] /= sum;
		cL[1] /= sum;
		cL += 2;
		}

	/* print the resulting conditional likelihoods cycling over uncompressed chars */
	for (c=0; c<numChar; c++)
		{
		if (charInfo[c].isExcluded == YES || charInfo[c].partitionId[partitionNum-1] != division+1)
			continue;
		i = compCharPos[c] - m->compCharStart;
		cL = ancStateCondLikes + (i*2);
		sprintf (tempStr, "%f\t%f\t", cL[0], cL[1]);
		if (AddToPrintString (tempStr) == ERROR) return (ERROR);
		}

	return NO_ERROR;	

}




/*----------------------------------------------------------------
|
|	PrintAncStates_Gen: print ancestral states after final pass
|		General model with or without rate variation
|
-----------------------------------------------------------------*/
int PrintAncStates_Gen (TreeNode *p, int division, int chain)

{

	int				c, i, k, nStates;
	MrBFlt			*bs, freq;
	CLFlt			*clFP, *cL, sum;
	char			tempStr[200];
	ModelInfo		*m;
	
	/* find model settings for this division */
	m = &modelSettings[division];

	/* find state frequencies */
	bs = GetParamSubVals (m->stateFreq, chain, state[chain]);
	
	/* find number of states */
	nStates = m->numModelStates;
	
	/* find frequencies of rate categories */
	freq =  1.0 /  m->numGammaCats;
	
	/* find the conditional likelihoods from the final pass */
	clFP = condLikePtr[chain][p->index] + m->condLikeStart + (1 ^ Bit(division, &p->clSpace[0])) * condLikeRowSize;

	/* find the preallocated working space */
	cL = ancStateCondLikes;
	
	/* cycle over the compressed characters */
	for (c=0; c<m->numChars; c++)
		{
		for (i=0; i<nStates; i++)
			cL[i] = 0.0;

		for (k=0; k<m->numGammaCats; k++)
			{
			for (i=0; i<nStates; i++)
				cL[i] += *(clFP++);
			}

		sum = 0.0;
		for (i=0; i<nStates; i++)
			{
			cL[i] *= (CLFlt) (bs[i] * freq);
			sum += cL[i];
			}

		for (i=0; i<nStates; i++)
			cL[i] /= sum;

		cL += nStates;
		}

	/* print the resulting conditional likelihoods cycling over uncompressed chars */
	for (c=0; c<numChar; c++)
		{
		if (charInfo[c].isExcluded == YES || charInfo[c].partitionId[partitionNum-1] != division+1)
			continue;
		i = compCharPos[c] - m->compCharStart;
		cL = ancStateCondLikes + (i*nStates);
		for (i=0; i<nStates; i++)
			{
			sprintf (tempStr, "%f\t", cL[i]);
			if (AddToPrintString (tempStr) == ERROR) return (ERROR);
			}
		}

	return NO_ERROR;	

}




/*----------------------------------------------------------------
|
|	PrintAncStates_NUC4: print ancestral states after final pass
|		4-state nucleotide model with or without rate variation
|
-----------------------------------------------------------------*/
int PrintAncStates_NUC4 (TreeNode *p, int division, int chain)

{

	int				c, i, k;
	MrBFlt			*bs, freq;
	CLFlt			*clFP, *cL, sum;
	char			tempStr[200];
	ModelInfo		*m;
	
	/* find model settings for this division */
	m = &modelSettings[division];

	/* find state frequencies */
	bs = GetParamSubVals (m->stateFreq, chain, state[chain]);
	
	/* find frequencies of rate categories */
	freq =  1.0 /  m->numGammaCats;
	
	/* find the conditional likelihoods from the final pass */
	clFP = condLikePtr[chain][p->index] + m->condLikeStart + (1 ^ Bit(division, &p->clSpace[0])) * condLikeRowSize;

	/* find the preallocated working space */
	cL = ancStateCondLikes;
	
	/* cycle over the compressed characters */
	for (c=i=0; c<m->numChars; c++)
		{
		cL[A] = cL[C] = cL[G] = cL[T] = 0.0;
		for (k=0; k<m->numGammaCats; k++)
			{
			cL[A] += clFP[A];
			cL[C] += clFP[C];
			cL[G] += clFP[G];
			cL[T] += clFP[T];
			clFP += 4;
			}
		cL[A] *= (CLFlt) (bs[A] * freq);
		cL[C] *= (CLFlt) (bs[C] * freq);
		cL[G] *= (CLFlt) (bs[G] * freq);
		cL[T] *= (CLFlt) (bs[T] * freq);
		sum = cL[A] + cL[C] + cL[G] + cL[T];
		cL[A] /= sum;
		cL[C] /= sum;
		cL[G] /= sum;
		cL[T] /= sum;
		cL += 4;
		}

	/* print the resulting conditional likelihoods cycling over uncompressed chars */
	for (c=0; c<numChar; c++)
		{
		if (charInfo[c].isExcluded == YES || charInfo[c].partitionId[partitionNum-1] != division+1)
			continue;
		i = compCharPos[c] - m->compCharStart;
		cL = ancStateCondLikes + (i*4);
		sprintf (tempStr, "%f\t%f\t%f\t%f\t", cL[A], cL[C], cL[G], cL[T]);
		if (AddToPrintString (tempStr) == ERROR) return ERROR;
		}

	return NO_ERROR;	

}




/*----------------------------------------------------------------
|
|	PrintAncStates_Std: print ancestral states after final pass
|		Standard model with or without rate variation
|
-----------------------------------------------------------------*/
int PrintAncStates_Std (TreeNode *p, int division, int chain)

{

	int				c, i, j, k, nStates, nCats;
	MrBFlt			*bs, freq;
	CLFlt			*clFP, *cL, sum;
	char			tempStr[200];
	ModelInfo		*m;
	
	/* find model settings for this division */
	m = &modelSettings[division];

	/* find state frequencies */
	bs = GetParamSubVals (m->stateFreq, chain, state[chain]);
	
	/* find frequencies of rate categories */
	freq =  1.0 /  m->numGammaCats;
	
	/* find the conditional likelihoods from the final pass */
	clFP = condLikePtr[chain][p->index] + m->condLikeStart + (1 ^ Bit(division, &p->clSpace[0])) * condLikeRowSize;

	/* find the preallocated working space */
	cL = ancStateCondLikes;
	
	/* cycle over the compressed characters */
	for (c=0; c<m->numChars; c++)
		{
		nStates = m->nStates[c];

		/* the following lines ensure that nCats is 1 unless */
		/* the character is binary and beta categories are used  */
		if (nStates == 2)
			nCats = m->numBetaCats;
		else
			nCats = 1;

		/* now multiply with the gamma cats */
		nCats *= m->numGammaCats;

		for (i=0; i<nStates; i++)
			cL[i] = 0.0;

		for (k=0; k<nCats; k++)
			{
			for (i=0; i<nStates; i++)
				cL[i] += clFP[i];
			clFP += nStates;
			}

		sum = 0.0;
		for (i=0; i<nStates; i++)
			{
			cL[i] *= (CLFlt) (bs[i] * freq);
			sum += cL[i];
			}

		for (i=0; i<nStates; i++) 
			cL[i] /= sum;

		cL += nStates;
		}

	/* print the resulting conditional likelihoods cycling over uncompressed chars */
	for (c=0; c<numChar; c++)
		{
		if (charInfo[c].isExcluded == YES || charInfo[c].partitionId[partitionNum-1] != division+1)
			continue;
		
		k = compCharPos[c] - m->compCharStart;
		for (i=j=0; i<k; i++)
			j += m->nStates[i];
		cL = ancStateCondLikes + j;

		for (i=0; i<m->nStates[k]; i++)
			{
			sprintf (tempStr, "%f\t", cL[i]);
			if (AddToPrintString (tempStr) == ERROR) return (ERROR);
			}
		}

	return NO_ERROR;	

}




int PrintCalTree (int curGen, Tree *tree)

{

	int				i, counter;
	char			tempNameStr[100], tempStr[200];
	
	/* allocate the print string */
	printStringSize = 200;
	printString = (char *)malloc((size_t) (printStringSize * sizeof(char)));
	if (!printString)
		{
		MrBayesPrint ("%s   Problem allocating printString (%d)\n", spacer, printStringSize * sizeof(char));
		return (ERROR);
		}
	strcpy (printString, "");

	if (curGen == 1)
		{
		/* print translation block information */
		sprintf (tempStr, "#NEXUS\n");
		if (AddToPrintString (tempStr) == ERROR) return(ERROR);
		sprintf (tempStr, "[ID: %s]\n", stamp);
		if (AddToPrintString (tempStr) == ERROR) return(ERROR);
		sprintf (tempStr, "begin trees;\n");
		if (AddToPrintString (tempStr) == ERROR) return(ERROR);
		sprintf (tempStr, "   translate\n");
		if (AddToPrintString (tempStr) == ERROR) return(ERROR);
		counter = 0;
		for (i=0; i<numTaxa; i++)
			{
			if (taxaInfo[i].isDeleted == YES)
				continue;
			counter++;
			if (GetNameFromString (taxaNames, tempNameStr, i+1) == ERROR)
				{
				MrBayesPrint ("%s   Could not find taxon %d\n", spacer, i+1);
				return (ERROR);
				}
			if (counter == numLocalTaxa)
				sprintf (tempStr, "      %2d %s;\n", counter, tempNameStr);
			else
				sprintf (tempStr, "      %2d %s,\n", counter, tempNameStr);
			if (AddToPrintString (tempStr) == ERROR) return(ERROR);
			}
		}
	
	/* write calibrated (dated) tree */
	sprintf (tempStr, "   tree rep.%d = ", curGen);
	if (AddToPrintString (tempStr) == ERROR) return(ERROR);
   	WriteCalTreeToFile (tree->root->left, tree->clockRate);
   	sprintf (tempStr, ";\n");
	if (AddToPrintString (tempStr) == ERROR) return(ERROR);
   	   		
   	return (NO_ERROR);
		
}





/*-----------------------------------------------------------------------
|
|	PrintChainCondLikes: Print conditional likelihoods for chain
|
|
------------------------------------------------------------------------*/
int PrintChainCondLikes (int chain, int precision)

{

	int				i, j=0, c, d, printWidth, nextColumn, nChars;
	CLFlt			*clP;
	ModelInfo		*m;
	ModelParams		*mp;
	Tree			*t;
	TreeNode		*p;

	if (!chainCondLikes || !termCondLikes)
		return ERROR;

	printWidth = 79;

	/* print with space between if precision > 0, else no space */
	nChars = precision;
	if (precision > 0)
		nChars +=2;

	for (d=0; d<numCurrentDivisions; d++)
		{
		MrBayesPrint ("\nConditional likelihoods for chain %d division %d\n\n", chain, d+1);

		m = &modelSettings[d];
		mp = &modelParams[d];

		/* print output in multiples of numModelStates */
		for (i=0; i<79; i+=m->numModelStates)
			;
		printWidth = i - m->numModelStates;
		if (printWidth==0)
			printWidth = m->numModelStates;
		printWidth +=9;

		t = GetTree (m->brlens, chain, state[chain]);

		for (c=m->condLikeStart; c<m->condLikeStop; c=j)
			{
			for (i=0; i<t->nNodes; i++)
				{
				p = t->allDownPass[i];
				if (p->left == NULL || (t->isRooted == NO && p->anc == NULL))
					{
					clP = condLikePtr[chain][p->index] + Bit (d, &p->clSpace[0]) * condLikeRowSize;
					MrBayesPrint ("%4d -- ", p->index);
					j = c;
					for (nextColumn=9; nextColumn < printWidth; nextColumn+=nChars + 1)
						{
						if (j >= m->condLikeStop)
							break;
						MrBayesPrint("%-*.*f", nChars + 1, precision, clP[j++]);
						}
					MrBayesPrint ("\n");
					}
				}
			for (i=0; i<t->nIntNodes; i++)
				{
				p = t->intDownPass[i];
				clP = condLikePtr[chain][p->index] + Bit (d, &p->clSpace[0]) * condLikeRowSize;
				MrBayesPrint ("%4d -- ", p->index);
				j = c;
				for (nextColumn=9; nextColumn < printWidth; nextColumn+=nChars + 1)
					{
					if (j >= m->condLikeStop)
						break;
					MrBayesPrint("%-*.*f", nChars + 1, precision, clP[j++]);
					}
				MrBayesPrint ("\n");
				}
			MrBayesPrint("\n");
			if ((c-m->condLikeStart)% 8 == 0)
				getchar();
			}
				
		}	/* next division */

	return NO_ERROR;

}





/*-----------------------------------------------------------------------
|
|	PrintCompMatrix: Print compressed matrix
|
|
|
------------------------------------------------------------------------*/
int PrintCompMatrix (void)

{
	int				i, j, k, c, d;
	ModelInfo		*m;
	ModelParams		*mp;
	char			tempName[100];
	char			(*whichChar)(int);

	extern char		WhichAA (int x);
	extern char		WhichNuc (int x);
	extern char		WhichRes (int x);
	extern char		WhichStand (int x);


	if (!compMatrix)
		return ERROR;

	whichChar = &WhichNuc;
	
	for (d=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];
		mp = &modelParams[d];

		if (mp->dataType == DNA || mp->dataType == RNA)
			whichChar = &WhichNuc;
		if (mp->dataType == PROTEIN)
			whichChar = &WhichAA;
		if (mp->dataType == RESTRICTION)
			whichChar = &WhichRes;
		if (mp->dataType == STANDARD)
			whichChar = &WhichStand;

		MrBayesPrint ("\nCompressed matrix for division %d\n\n", d+1);
		
		k = 66;
		if (mp->dataType == CONTINUOUS)
			k /= 4;

		for (c=m->compMatrixStart; c<m->compMatrixStop; c+=k)
			{
			for (i=0; i<numLocalTaxa; i++)
				{
				GetNameFromString(localTaxonNames, tempName, i+1);
				MrBayesPrint ("%-10.10s   ", tempName);
				for (j=c; j<c+k; j++)
					{
					if (j >= m->compMatrixStop)
						break;
					if (mp->dataType == CONTINUOUS)
						MrBayesPrint ("%3d ", compMatrix[pos(i,j,compMatrixRowSize)]);
					else
						MrBayesPrint ("%c", whichChar(compMatrix[pos(i,j,compMatrixRowSize)]));
					}
				MrBayesPrint("\n");
				}
			MrBayesPrint("\nNo. sites    ");
			for (j=c; j<c+k; j++)
				{
				if (j >= m->compMatrixStop)
					break;
				i = (int) numSitesOfPat[m->compCharStart + (0*numCompressedChars) + (j - m->compMatrixStart)/m->nCharsPerSite]; /* NOTE: We are printing the unadulterated site pat nums */
				if (i>9)
					i = 'A' + i - 10;
				else
					i = '0' + i;
				if (mp->dataType == CONTINUOUS)
					MrBayesPrint("   %c ", i);
				else
					{
					if ((j-m->compMatrixStart) % m->nCharsPerSite == 0)
						MrBayesPrint ("%c", i);
					else
						MrBayesPrint(" ");
					}
				}
			MrBayesPrint ("\nOrig. char   ");
			for (j=c; j<c+k; j++)
				{
				if (j >= m->compMatrixStop)
					break;
				i = origChar[j];
				if (i>9)
					i = '0' + (i % 10);
				else
					i = '0' +i;
				if (mp->dataType == CONTINUOUS)
					MrBayesPrint("   %c ", i);
				else
					MrBayesPrint ("%c", i);
				}

			if (mp->dataType == STANDARD && m->nStates != NULL)
				{
				MrBayesPrint ("\nNo. states   ");
				for (j=c; j<c+k; j++)
					{
					if (j >= m->compMatrixStop)
						break;
					i = m->nStates[j-m->compCharStart];
					MrBayesPrint ("%d", i);
					}
				MrBayesPrint ("\nCharType     ");
				for (j=c; j<c+k; j++)
					{
					if (j >= m->compMatrixStop)
						break;
					i = m->cType[j-m->compMatrixStart];
					if (i == ORD)
						MrBayesPrint ("%c", 'O');
					else if (i == UNORD)
						MrBayesPrint ("%c", 'U');
					else
						MrBayesPrint ("%c", 'I');
					}
				MrBayesPrint ("\ntiIndex      ");
				for (j=c; j<c+k; j++)
					{
					if (j >= m->compMatrixStop)
						break;
					i = m->tiIndex[j-m->compCharStart];
					MrBayesPrint ("%d", i % 10);
					}
				MrBayesPrint ("\nbsIndex      ");
				for (j=c; j<c+k; j++)
					{
					if (j >= m->compMatrixStop)
						break;
					i = m->bsIndex[j-m->compCharStart];
					MrBayesPrint ("%d", i % 10);
					}
				}
			MrBayesPrint ("\n\n");
			}
		MrBayesPrint ("Press return to continue\n");
		getchar();
		}	/* next division */

	return NO_ERROR;

}





/*----------------------------------------------------------------------
|
|	PrintMatrix: Print data matrix
|
|
------------------------------------------------------------------------*/
int PrintMatrix (void)

{

	int				i, j=0, c, printWidth, nextColumn;
	char			tempName[100];

	extern char		WhichAA (int x);
	extern char		WhichNuc (int x);
	extern char		WhichRes (int x);
	extern char		WhichStand (int x);


	if (!matrix)
		return ERROR;
	
	MrBayesPrint ("\nData matrix\n\n");
	
	printWidth = 79;

	for (c=0; c<numChar; c=j)
		{
		for (i=0; i<numTaxa; i++)
			{
			GetNameFromString(taxaNames, tempName, i+1);
			MrBayesPrint ("%-10.10s   ", tempName);
			j = c;
			for (nextColumn=13; nextColumn < printWidth; nextColumn++)
				{
				if (j >= numChar)
					break;
				if (charInfo[j].charType == CONTINUOUS && nextColumn < printWidth - 3)
					break;
				if (charInfo[j].charType == CONTINUOUS)
					{	
					MrBayesPrint ("%3d ", matrix[pos(i,j,numChar)]);
					nextColumn += 3;
					}
				else if (charInfo[j].charType == DNA || charInfo[j].charType == RNA)
					MrBayesPrint ("%c", WhichNuc(matrix[pos(i,j,numChar)]));
				else if (charInfo[j].charType == AA)
					MrBayesPrint ("%c", WhichAA(matrix[pos(i,j,numChar)]));
				else if (charInfo[j].charType == RESTRICTION)
					MrBayesPrint ("%c", WhichRes(matrix[pos(i,j,numChar)]));
				else if (charInfo[j].charType == STANDARD)
					MrBayesPrint ("%c", WhichStand(matrix[pos(i,j,numChar)]));
				j++;
				}
			MrBayesPrint("\n");
			}
		MrBayesPrint ("\n");
		}

	return NO_ERROR;

}





/*----------------------------------------------------------------------
|
|	PrintMCMCDiagnosticsToFile: Print acceptance ratios, swapping
|      frequencies, and convergence diagnostics to file.
|
------------------------------------------------------------------------*/
int PrintMCMCDiagnosticsToFile (int curGen)

{

	int			i, j, n;
	MCMCMove	*theMove;

#if defined (MPI_ENABLED)
	if (ReassembleMoveInfo() == ERROR)
		return (ERROR);
	if (chainParams.numChains > 1 && ReassembleSwapInfo() == ERROR)
		return (ERROR);
	if (proc_id != 0)
		return (NO_ERROR);
#endif

	/* Print header if curGen == 1 */
	if (curGen == 1)
		{
		MrBayesPrintf (fpMcmc, "[LEGEND:\n");
		MrBayesPrintf (fpMcmc, "   Gen               --  Generation\n");
		if (chainParams.allChains == YES)
			MrBayesPrintf (fpMcmc, "   <name>(1){2.3}    --  Acceptance rate of move <name> changing parameter 1 in run 2, chain 3\n");
		else /* if (chainParams.allChains == NO) */
			MrBayesPrintf (fpMcmc, "   <name>(1){2}      --  Acceptance rate of move <name> changing parameter 1 in run 2\n");
		if (chainParams.numChains > 1 && chainParams.numRuns > 1)
			MrBayesPrintf (fpMcmc, "   Swap(1-2){3}      --  Acceptance rate of swaps between chains 1 and 2 in run 3\n");
		else if (chainParams.numChains > 1 && chainParams.numRuns == 1)
			MrBayesPrintf (fpMcmc, "   Swap(1-2)         --  Acceptance rate of swaps between chains 1 and 2\n");
		if (chainParams.numRuns > 1 && numTrees == 1)
			MrBayesPrintf (fpMcmc, "   StdDev(s)         --  Average standard deviation of split frequencies\n");
		else if (chainParams.numRuns > 1 && numTrees > 1)
			MrBayesPrintf (fpMcmc, "   StdDev(s_1)       --  Average standard deviation of split frequencies for tree 1\n");
		MrBayesPrintf (fpMcmc, "]\n\n");

		MrBayesPrintf (fpMcmc, "Gen");
		for (n=0; n<chainParams.numRuns; n++)
			{
			if (chainParams.allChains == YES)
				{
				for (i=0; i<chainParams.numChains; i++)
					{
					for (j=0; j<numMoves; j++)
						{
						theMove = &moves[j];
						MrBayesPrintf (fpMcmc, "\t%s(%d){%d.%d}", theMove->shortName, theMove->parm->index+1, n+1, i+1);
						}
					}
				}
			else
				{
				for (i=0; i<numMoves; i++)
					{
					theMove = &moves[i];
					if (chainParams.numRuns == 1)
						MrBayesPrintf (fpMcmc, "\t%s(%d)", theMove->shortName, theMove->parm->index+1);
					else
						MrBayesPrintf (fpMcmc, "\t%s(%d){%d}", theMove->shortName, theMove->parm->index+1, n+1);
					}
				}
			if (chainParams.numChains > 1)
				{
				for (i=0; i<chainParams.numChains; i++)
					{
					for (j=i+1; j<chainParams.numChains; j++)
						{
						if (chainParams.numRuns == 1)
							MrBayesPrintf (fpMcmc, "\tSwap(%d-%d)", i+1, j+1);
						else
							MrBayesPrintf (fpMcmc, "\tSwap(%d-%d){%d}", i+1, j+1, n+1);
						}
					}
				}
			}

		if (chainParams.numRuns > 1)
			{
			for (n=0; n<numTrees; n++)
				{
				if (numTrees == 1)
					MrBayesPrintf (fpMcmc, "\tStdDev(s)");
				else
					MrBayesPrintf (fpMcmc, "\tStdDev(s_%d)", n+1);
		
				if (chainParams.allComps == YES)
					{
					for (i=0; i<chainParams.numRuns; i++)
						{
						for (j=i+1; j<chainParams.numRuns; j++)
							{
							if (numTrees == 1)
								MrBayesPrintf (fpMcmc, "\tStdDev(s)[%d-%d]", i+1, j+1);
							else
								MrBayesPrintf (fpMcmc, "\tStdDev(s_%d)[%d-%d]", n+1, i+1, j+1);
							}
						}
					}
				}
			}
		MrBayesPrintf (fpMcmc, "\n");
		fflush (fpMcmc);
		return (NO_ERROR);
		}

	MrBayesPrintf (fpMcmc, "%d", curGen);

	for (n=0; n<chainParams.numRuns; n++)
		{
		if (chainParams.allChains == YES)
			{
			for (j=n*chainParams.numChains; j<(n+1)*chainParams.numRuns; j++)
				{
				for (i=0; i<numMoves; i++)
					{
					theMove = &moves[i];
					if (theMove->nTried[j] < 1)
						MrBayesPrintf (fpMcmc, "\tN/A");
					else
						MrBayesPrintf (fpMcmc, "\t%.6f", (MrBFlt) theMove->nAccepted[j] / (MrBFlt) theMove->nTried[j]);
					}
				}
			}
		else
			{
			j = n*chainParams.numChains;
			for (i=0; i<numMoves; i++)
				{
				theMove = &moves[i];
				if (theMove->nTried[j] < 1)
					MrBayesPrintf (fpMcmc, "\tN/A");
				else
					MrBayesPrintf (fpMcmc, "\t%.6f", (MrBFlt) theMove->nAccepted[j] / (MrBFlt) theMove->nTried[j]);
				}
			}
		if (chainParams.numChains > 1)
			{
			for (i=0; i<chainParams.numChains; i++)
				{
				for (j=i+1; j<chainParams.numChains; j++)
					{
					MrBayesPrintf (fpMcmc, "\t%.6f", (MrBFlt) swapInfo[n][i][j] / (MrBFlt) swapInfo[n][j][i]);
					}
				}
			}
		}

	if (chainParams.numRuns > 1)
		{
		for (n=0; n<numTrees; n++)
			{
			if (chainParams.relativeBurnin == NO && curGen < chainParams.chainBurnIn * chainParams.sampleFreq)
				MrBayesPrintf (fpMcmc, "\tN/A");
			else
				{
				MrBayesPrintf (fpMcmc, "\t%.6f", chainParams.stat[n].avgStdDev);
				}
			if (chainParams.allComps == YES)
				{
				for (i=0; i<chainParams.numRuns; i++)
					{
					for (j=i+1; j<chainParams.numRuns; j++)
						{
						if (chainParams.relativeBurnin == NO && curGen < chainParams.chainBurnIn * chainParams.sampleFreq)
							MrBayesPrintf (fpMcmc, "\tN/A");
						else
							MrBayesPrintf (fpMcmc, "\t%.6f", chainParams.stat[n].pair[i][j] / chainParams.stat[n].pair[j][i]);
						}
					}
				}
			}
		}

	MrBayesPrintf (fpMcmc, "\n");
	fflush (fpMcmc);

	return (NO_ERROR);
}





/*----------------------------------------------------------------------
|
|	PrintParamValues: print parameter values and subvalues for param
|
----------------------------------------------------------------------*/
void PrintParamValues (Param *p, int chain, char *s)

{
	
	int			j;
	MrBFlt		*value0, *value1;
	
	if (p == NULL)
		MrBayesPrint ("%s   %s = NULL\n", spacer, s);
	else
		{
		if (p->nValues > 0)
			{
			value0 = GetParamVals (p, chain, 0);
			value1 = GetParamVals (p, chain, 1);
			for (j=0; j<p->nValues; j++)
				MrBayesPrint ("%s   hyper [%s] = (%lf %lf)\n", spacer, s, value0[j], value1[j]);
			}
		if (p->nSubValues > 0)
			{
			value0 = GetParamSubVals (p, chain, 0);
			value1 = GetParamSubVals (p, chain, 1);
			for (j=0; j<p->nSubValues; j++)
				MrBayesPrint ("%s   %s = (%lf %lf)\n", spacer, s, value0[j], value1[j]);
			}
		}
	MrBayesPrint ("\n\n");

	return;

}





/*----------------------------------------------------------------------
|
|	PrintParsMatrix: Print parsimony (bitset) matrix
|		using hexadecimal representation
|
|
------------------------------------------------------------------------*/
int PrintParsMatrix (void)

{

	int				i, j=0, k, c, d, printWidth, nextColumn, nChars, inputChar;
	long			x, y;
	char			ch, tempName[200];
	ModelInfo		*m;
	ModelParams		*mp;

	if (!parsMatrix)
		return ERROR;

	printWidth = 79;

	for (d=0; d<numCurrentDivisions; d++)
		{
		MrBayesPrint ("\nParsimony (bitset) matrix for division %d\n\n", d+1);

		m = &modelSettings[d];
		mp = &modelParams[d];

		nChars = 1 + (int) (log((1 << mp->nStates) - 1) / log(16));
	
		for (c=m->parsMatrixStart; c<m->parsMatrixStop; c=j)
			{
			MrBayesPrint ("Parsimony sets for character %d -- \n", ((c - m->parsMatrixStart) / m->nParsIntsPerSite));
			for (i=0; i<numTaxa; i++)
				{
				GetNameFromString(taxaNames, tempName, i+1);
				MrBayesPrint ("%-10.10s   ", tempName);
				j = c;
				for (nextColumn=13; nextColumn < printWidth; nextColumn+=nChars + 1)
					{
					if (j >= m->parsMatrixStop)
						break;
					x = parsMatrix[pos(i,j++,parsMatrixRowSize)];
					for (k=8 - nChars; k<8; k++)
						{
						y = (x >> (4* (7 - k))) & 15;
#ifdef PAUL
						if (y > 16) DEBUG("y is too big %ld\n",y);
#endif
						if (y < 10)
							ch = (char) y + '0';
						else
							ch = (char) y - 10 + 'A';
						MrBayesPrint("%c", ch);
						}
					MrBayesPrint(" ");
					}
				MrBayesPrint ("\n");
				}
			MrBayesPrint("\n");
			printf ("Do you want to stop (y/n)?\n");
			inputChar = getchar();
			if (inputChar == 'y' || inputChar == 'Y')
				return NO_ERROR;
			else
				MrBayesPrint ("\n");
			}
		}	/* next division */

	return NO_ERROR;

}





/*------------------------------------------------------------------
|
|	PrintSiteRates_Gen: general n-state models with rate variation
|
-------------------------------------------------------------------*/

int PrintSiteRates_Gen (TreeNode *p, int division, int chain)

{
	int				c, j, k, nStates, hasPInvar;
	MrBFlt			freq, siteLike, invLike, catLike, pInvar=0.0, *bs,
					*clInvar=NULL, *catRate, baseRate;
	MrBFlt			s01, s10, probOn, probOff, *swr, covBF[40];
	CLFlt			*lnScaler, *clP, *siteRates;
	char			tempStr[200];
	ModelInfo		*m;
	
	/* find model settings and nStates, pInvar, invar cond likes */
	m = &modelSettings[division];
	nStates = m->numModelStates;
	if (m->pInvar == NULL)
		{
		hasPInvar = NO;
		}
	else
		{
		hasPInvar = YES;
		pInvar =  *(GetParamVals (m->pInvar, chain, state[chain]));
		clInvar = m->invCondLikes;
		}

	/* find conditional likelihood pointer */
	clP = condLikePtr[chain][p->index] + m->condLikeStart + Bit(division, &p->clSpace[0]) * condLikeRowSize;
	
	/* use scratch space for root node for temporary calculations */
	siteRates = condLikePtr[chain][p->index] + m->condLikeStart + (Bit(division, &p->clSpace[0]) ^ 1) * condLikeRowSize;
	
	/* find site scaler */
	lnScaler = treeScaler[chain] + m->compCharStart + state[chain] * numCompressedChars;
	
	/* find base frequencies */
	bs = GetParamSubVals (m->stateFreq, chain, state[chain]);

	/* if covarion model, adjust base frequencies */
	if (m->switchRates != NULL)
		{
		/* find the stationary frequencies */
		swr = GetParamVals(m->switchRates, chain, state[chain]);
		s01 = swr[0];
		s10 = swr[1];
		probOn = s01 / (s01 + s10);
		probOff = 1.0 - probOn;

		/* now adjust the base frequencies; on-state stored first in cond likes */
		for (j=0; j<nStates/2; j++)
			{
			covBF[j] = bs[j] * probOn;
			covBF[j+nStates/2] = bs[j] * probOff;
			}

		/* finally set bs pointer to adjusted values */
		bs = covBF;
		}

	/* find category frequencies */
	if (hasPInvar == NO)
		freq =  1.0 /  m->numGammaCats;
	else
		freq =  (1.0 - pInvar) /  m->numGammaCats;

	/* get rate multipliers (for gamma & partition specific rates) */
	baseRate =  GetRate (division, chain);
	
	/* compensate for invariable sites */
	if (hasPInvar == YES)
		baseRate /= ( 1.0 - pInvar);
		
	/* get category rates */
	catRate = GetParamSubVals (m->shape, chain, state[chain]);

	/* loop over characters */
	if (hasPInvar == NO)
		{
		/* no invariable category */
		for (c=0; c<m->numChars; c++)
			{
			siteLike = 0.0;
			siteRates[c] = 0.0;
			for (k=0; k<m->numGammaCats; k++)
				{
				catLike = 0.0;
				for (j=0; j<nStates; j++)
					catLike += (*(clP++)) * bs[j];
				siteRates[c] += (CLFlt) (catLike * catRate[k]);
				siteLike += catLike;
				}
			siteRates[c] *= (CLFlt) (baseRate / siteLike);	/* category frequencies and site scaler cancel out */
			}
		}
	else
		{
		/* has invariable category */
		for (c=0; c<m->numChars; c++)
			{
			siteLike = invLike = 0.0;
			siteRates[c] = 0.0;
			for (k=0; k<m->numGammaCats; k++)
				{
				catLike = 0.0;
				for (j=0; j<nStates; j++)
					catLike += (*(clP++)) * bs[j];
				siteRates[c] += (CLFlt) (catLike * catRate[k]);
				siteLike += catLike;
				}
			siteLike *= freq;
			siteRates[c] *= (CLFlt) freq;
			for (j=0; j<nStates; j++)
				invLike += (*(clInvar++)) * bs[j];
			siteLike += (invLike /  exp (lnScaler[c]) * pInvar);
			/* we do not need to add the invariable category into siteRates before rescaling because the rate is 0.0 */
			siteRates[c] *= (CLFlt) (baseRate / siteLike);	/* site scaler cancels out; category frequencies dealt with above */
			}
		}
		
	/* print the resulting site rates cycling over uncompressed chars */
	for (c=0; c<numChar; c++)
		{
		if (charInfo[c].isExcluded == YES || charInfo[c].partitionId[partitionNum-1] != division+1)
			continue;
		j = compCharPos[c] - m->compCharStart;
		sprintf (tempStr, "%f\t", siteRates[j]);
		if (AddToPrintString (tempStr) == ERROR) return (ERROR);
		}

	return NO_ERROR;
	
}





/*------------------------------------------------------------------
|
|	PrintSiteRates_Std: standard model with rate variation
|
-------------------------------------------------------------------*/

int PrintSiteRates_Std (TreeNode *p, int division, int chain)

{
	int				c, j, k, nStates;
	MrBFlt			siteLike, catLike, *bs, *catRate, baseRate;
	CLFlt			*clP, *siteRates;
	char			tempStr[200];
	ModelInfo		*m;
	
	/* find model settings */
	m = &modelSettings[division];

	/* find conditional likelihood pointer */
	clP = condLikePtr[chain][p->index] + m->condLikeStart + Bit(division, &p->clSpace[0]) * condLikeRowSize;
	
	/* use scratch space for root node for temporary calculations */
	siteRates = condLikePtr[chain][p->index] + m->condLikeStart + (Bit(division, &p->clSpace[0]) ^ 1) * condLikeRowSize;
	
	/* find base frequencies */
	bs = GetParamSubVals (m->stateFreq, chain, state[chain]);

	/* get rate multiplier */
	baseRate =  GetRate (division, chain);
	
	/* get category rates */
	catRate = GetParamSubVals (m->shape, chain, state[chain]);

	/* loop over characters */
	for (c=0; c<m->numChars; c++)
		{
		siteLike = 0.0;
		siteRates[c] = 0.0;
		nStates = m->nStates[c];
		for (k=0; k<m->numGammaCats; k++)
			{
			catLike = 0.0;
			for (j=0; j<nStates; j++)
				catLike += (*(clP++)) * bs[j];
			siteRates[c] += (CLFlt) (catLike * catRate[k]);
			siteLike += catLike;
			}
		siteRates[c] *= (CLFlt)(baseRate / siteLike);	/* category frequencies and site scaler cancel out */
		}
		
	/* print the resulting site rates cycling over uncompressed chars */
	for (c=0; c<numChar; c++)
		{
		if (charInfo[c].isExcluded == YES || charInfo[c].partitionId[partitionNum-1] != division+1)
			continue;
		j = compCharPos[c] - m->compCharStart;
		sprintf (tempStr, "%f\t", siteRates[j]);
		if (AddToPrintString (tempStr) == ERROR) return (ERROR);
		}

	return NO_ERROR;

}





int PrintStates (int curGen, int coldId)

{

	int				d, i, j, k, compressedCharPosition, *printedChar=NULL, origAlignmentChars[3];
	char			partString[100], tempStr[800];
	MrBFlt			*st, *sst, sum;
	Param			*p;
	ModelInfo		*m;
	Tree			*tree;
	TreeNode		*node;
	ModelParams		*mp;

	/* allocate the print string */
	printStringSize = 200;
	printString = (char *)malloc((size_t) (printStringSize * sizeof(char)));
	if (!printString)
		{
		MrBayesPrint ("%s   Problem allocating printString (%d)\n", spacer, printStringSize * sizeof(char));
		goto errorExit;
		}
	strcpy (printString, "");

	/* Allocate memory, temporarily, in case we have positive selection or infer site rates */
	if (inferPosSel == YES)
		{
		if (memAllocs[ALLOC_POSSELPROBS] == YES)
			{
			MrBayesPrint ("%s   posSelProbs not free in PrintStates\n", spacer);
			goto errorExit;
			}
		posSelProbs = (MrBFlt *)malloc((size_t) (numCompressedChars * sizeof(MrBFlt)));
		if (!posSelProbs)
			{
			MrBayesPrint ("%s   Problem allocating posSelProbs (%d)\n", spacer, numCompressedChars * sizeof(MrBFlt));
			goto errorExit;
			}
		for (i=0; i<numCompressedChars; i++)
			posSelProbs[i] =  -10.0;
		memAllocs[ALLOC_POSSELPROBS] = YES;
		}
	if (inferPosSel == YES || inferSiteRates == YES)
		{
		printedChar = (int *)malloc((size_t) (numChar * sizeof(int)));
		if (!printedChar)
			{
			MrBayesPrint ("%s   Problem allocating printedChar (%d)\n", spacer, numChar * sizeof(int));
			goto errorExit;
			}
		for (i=0; i<numChar; i++)
			printedChar[i] = NO;
		}

	/* Set up the header to the file. */
	if (curGen == 1)
		{
		sprintf (tempStr, "[ID: %s]\n", stamp);
		if (AddToPrintString (tempStr) == ERROR) goto errorExit;
		sprintf (tempStr, "Gen\t");
		if (AddToPrintString (tempStr) == ERROR) goto errorExit;
		sprintf (tempStr, "LnL\t");
		if (AddToPrintString (tempStr) == ERROR) goto errorExit;

		/* print tree lengths for all trees */
		for (i=0; i<numParams; i++)
			{
			p = &params[i];

			if (p->paramType == P_BRLENS)
				{
				if (FillRelPartsString(p, partString) == YES)
					sprintf (tempStr, "TL%s\t", partString);
				else
					sprintf (tempStr, "TL\t");
				if (AddToPrintString (tempStr) == ERROR) goto errorExit;
				}
			}

		/* print clock rates for calibrated trees */
		for (i=0; i<numParams; i++)
			{
			p = &params[i];

			if (p->paramId == BRLENS_CCLOCK_UNI ||
				p->paramId == BRLENS_CCLOCK_COAL ||
				p->paramId == BRLENS_CCLOCK_BD)
				{
				if (FillRelPartsString(p, partString) == YES)
					sprintf (tempStr, "calRate%s\t", partString);
				else
					sprintf (tempStr, "calRate\t");
				if (AddToPrintString (tempStr) == ERROR) goto errorExit;
				}
			}

		/* print substitution model parameters */
		for (i=0; i<numParams; i++)
			{
			p = &params[i];
			if (p->printParam == YES)
				{
				sprintf (tempStr, "%s\t", p->paramName);
				if (AddToPrintString (tempStr) == ERROR) goto errorExit;
				}
			}
			
		if (inferSiteRates == YES)
			{
			for (i=0; i<numChar; i++)
				printedChar[i] = NO;
	
			for (i=0; i<numChar; i++)
				{ 
				if (charInfo[i].isExcluded == YES)
					continue;
				if (printedChar[i] == YES)
					continue;
				d = charInfo[i].partitionId[partitionNum-1] - 1;
				m = &modelSettings[d];
				mp = &modelParams[d];
				if (m->printSiteRates == YES)
					{
					if (m->nCharsPerSite == 1)
						{
						sprintf (tempStr, "r(%d)\t", i+1);
						if (AddToPrintString (tempStr) == ERROR) goto errorExit;
						}
					else
						{
						origAlignmentChars[0] = i;
						k = 1;
						for (j=i+1; j<numChar; j++)
							{
							if (compCharPos[i] == compCharPos[j])
								{
								if (k > m->nCharsPerSite)
									return (ERROR);
								origAlignmentChars[k++] = j;
								printedChar[j] = YES;
								}
							}
						if (k != m->nCharsPerSite)
							return (ERROR);
						sprintf (tempStr, "r(%d,", origAlignmentChars[0]);
						if (AddToPrintString (tempStr) == ERROR) goto errorExit;
						for (j=1; j<k-1; j++)
							{
							sprintf (tempStr, "%d,", origAlignmentChars[j]);
							if (AddToPrintString (tempStr) == ERROR) goto errorExit;
							}
						sprintf (tempStr, "%d)\t", origAlignmentChars[k-1]);
						if (AddToPrintString (tempStr) == ERROR) goto errorExit;
						}
					}
				}
			}

		if (inferPosSel == YES)
			{
			for (d=0; d<numCurrentDivisions; d++)
				{
				m = &modelSettings[d];
				tree = GetTree(m->brlens, coldId, state[coldId]);
				if (m->printPosSel == YES)
					{
					if (PosSelProbs (tree->root->left, d, coldId) == ERROR)
						{
						goto errorExit;
						}
					}
				}
			/*for (i=0; i<numChar; i++)
				printf ("%4d -- %3d %3d\n", i, compCharPos[i], compColPos[i]);*/
			for (i=0; i<numChar; i++)
				{
				compressedCharPosition = compCharPos[i];
				if (posSelProbs[compressedCharPosition] >= 0.0 && printedChar[i] == NO)
					{
					for (j=k=0; j<numChar; j++)
						{
						if (charInfo[j].charId == charInfo[i].charId)
							{
							origAlignmentChars[k++] = j;
							printedChar[j] = YES;
							}
						}
					sprintf (tempStr, "pr+(%d,%d,%d)\t", origAlignmentChars[0]+1, origAlignmentChars[1]+1, origAlignmentChars[2]+1);
					if (AddToPrintString (tempStr) == ERROR) goto errorExit;
					}
				}	
			for (i=0; i<numChar; i++)
				printedChar[i] = NO;
			}
			
		if (inferAncStates == YES)
			{
			for (j=0; j<numChar; j++)
				{ 
				if (charInfo[j].isExcluded == YES)
					continue;
				d = charInfo[j].partitionId[partitionNum-1] - 1;
				m = &modelSettings[d];
				mp = &modelParams[d];
				if (m->printAncStates == YES)
					{
					for (i=0; i<mp->numActiveConstraints; i++)
						{
						if (mp->dataType == STANDARD)
							{
							for (k=0; k<m->nStates[compCharPos[j] - m->compCharStart]; k++)
								{
								if (mp->numActiveConstraints > 1)
									sprintf (tempStr, "p(%c){%d@%d}\t", m->StateCode(k), j+1, i+1);
								else
									sprintf (tempStr, "p(%c){%d}\t", m->StateCode(k), j+1);
								if (AddToPrintString (tempStr) == ERROR) goto errorExit;
								}
							}
						else
							{
							for (k=0; k<m->numStates; k++)
								{
								if (mp->numActiveConstraints > 1)
									sprintf (tempStr, "p(%c){%d@%d}\t", m->StateCode(k), j+1, i+1);
								else
									sprintf (tempStr, "p(%c){%d}\t", m->StateCode(k), j+1);
								if (AddToPrintString (tempStr) == ERROR) goto errorExit;
								}
							}
						}
					}
				}
			}
			
		sprintf (tempStr, "\n");
		if (AddToPrintString (tempStr) == ERROR) goto errorExit;
		}
		
	/* now print parameter values */
	sprintf (tempStr, "%d\t", curGen);
	if (AddToPrintString (tempStr) == ERROR) goto errorExit;
	sprintf (tempStr, "%1.3lf\t", curLnL[coldId]);
	if (AddToPrintString (tempStr) == ERROR) goto errorExit;

	/* print tree lengths for all trees */
	for (i=0; i<numParams; i++)
		{
		p = &params[i];

		if (p->paramType == P_BRLENS)
			{
			sprintf (tempStr, "%1.3lf\t", TreeLength(p, coldId));
			if (AddToPrintString (tempStr) == ERROR) goto errorExit;
			}
		}

	/* print clock rates for calibrated trees */
	for (i=0; i<numParams; i++)
		{
		p = &params[i];

		if (p->paramId == BRLENS_CCLOCK_UNI ||
			p->paramId == BRLENS_CCLOCK_COAL ||
			p->paramId == BRLENS_CCLOCK_BD)
			{
			tree = GetTree (p, coldId, state[coldId]);
			sprintf (tempStr, "%lf\t", tree->clockRate);
			if (AddToPrintString (tempStr) == ERROR) goto errorExit;
			}
		}

	/* print ordinary parameters */
	for (i=0; i<numParams; i++)
		{
		p = &params[i];

		/* get model params */
		mp = &modelParams[p->relParts[0]];
		
		st  = GetParamVals (p, coldId, state[coldId]);
		sst = GetParamSubVals (p, coldId, state[coldId]);

		if (p->printParam == YES)
			{
			if (p->paramType == P_PI && p->paramId != SYMPI_EXP && p->paramId != SYMPI_EXP_MS && p->paramId != SYMPI_UNI && p->paramId != SYMPI_UNI_MS)
				{
				/* We print the subvalues if we are dealing with state frequencies (state frequencies are held in subvalues). If we have
				   morphological characters, then we don't want to print out the state frequencies because they will be integrated out over a
				   dirichlet prior that is specified in values, not subvalues. */
				for (j=0; j<p->nSubValues; j++)
					{
					sprintf (tempStr, "%lf\t", sst[j]);
					if (AddToPrintString (tempStr) == ERROR) goto errorExit;
					}
				}
			else if (p->paramType == P_TRATIO && !strcmp(mp->tratioFormat,"Dirichlet"))
				{
				sprintf (tempStr, "%lf\t%lf\t", st[0] / (1.0 + st[0]), 1.0 / (1.0 + st[0]));
				if (AddToPrintString (tempStr) == ERROR) goto errorExit;
				}
			else if (p->paramType == P_REVMAT && !strcmp(mp->revmatFormat,"Ratio"))
				{
				sum = st[p->nValues-1];
				for (j=0; j<p->nValues; j++)
					{
					sprintf (tempStr, "%lf\t", st[j] / sum);
					if (AddToPrintString (tempStr) == ERROR) goto errorExit;
					}
				}
			else if (p->paramType == P_RATEMULT)
				{
				if (!strcmp(mp->ratemultFormat,"Ratio"))
					{
					for (j=0; j<p->nValues; j++)
						{
						sprintf (tempStr, "%lf\t", sst[j + p->nValues]);
						if (AddToPrintString (tempStr) == ERROR) goto errorExit;
						}
					}
				else if (!strcmp(mp->ratemultFormat, "Dirichlet"))
					{
					sum = 0.0;
					for (j=0; j<p->nValues; j++)
						sum += sst[j + p->nValues];
					for (j=0; j<p->nValues; j++)
						{
						sprintf (tempStr, "%lf\t", sst[j + p->nValues] / sum);
						if (AddToPrintString (tempStr) == ERROR) goto errorExit;
						}
					}
				else
					{
					for (j=0; j<p->nValues; j++)
						{
						sprintf (tempStr, "%lf\t", st[j]);
						if (AddToPrintString (tempStr) == ERROR) goto errorExit;
						}
					}
				}
			else
				{
				for (j=0; j<p->nValues; j++)
					{
					sprintf (tempStr, "%lf\t", st[j]);
					if (AddToPrintString (tempStr) == ERROR) goto errorExit;
					}
				}
			if (p->paramType == P_OMEGA && p->paramId != OMEGA_DIR && p->paramId != OMEGA_FIX && p->paramId != OMEGA_FFF && p->paramId != OMEGA_FF && p->paramId != OMEGA_10FFF)
				{
				/* OK, we also need to print subvalues for the category frequencies in a NY98-like model. */
				if (!strcmp(mp->omegaVar, "M10"))
					{
					for (j=0; j<4; j++)
						{
						sprintf (tempStr, "%lf\t", sst[mp->numM10BetaCats + mp->numM10GammaCats + 4 + j]);
						if (AddToPrintString (tempStr) == ERROR) goto errorExit;
						}
					for (j=0; j<2; j++)
						{
						sprintf (tempStr, "%lf\t", sst[mp->numM10BetaCats + mp->numM10GammaCats + j]);
						if (AddToPrintString (tempStr) == ERROR) goto errorExit;
						}
					}
				else
					{
					for (j=0; j<3; j++)
						{
						sprintf (tempStr, "%lf\t", sst[j]);
						if (AddToPrintString (tempStr) == ERROR) goto errorExit;
						}
					}
				}
			}
		}
		
	/* If the user wants to infer sites that are under positive selection, then we need to print out the posterior
	   probability that each site is a positively selected one here. */
	if (inferPosSel == YES)
		{
		/* loop over the divisions, calculating the probability of being in the positively
		   selected class for each relevant partition */
		for (d=0; d<numCurrentDivisions; d++)
			{
			m = &modelSettings[d];
			tree = GetTree(m->brlens, coldId, state[coldId]);
			if (m->Likelihood == &Likelihood_NY98)
				{
				if (PosSelProbs (tree->root->left, d, coldId) == ERROR)
					{
					goto errorExit;
					}
				}
			}

		/* print the probabilities for the appropriate sites in the original alignment */
		for (i=0; i<numChar; i++)
			{
			compressedCharPosition = compCharPos[i];
			if (posSelProbs[compressedCharPosition] >= 0.0 && printedChar[i] == NO)
				{
				for (j=k=0; j<numChar; j++)
					{
					if (charInfo[j].charId == charInfo[i].charId)
						{
						origAlignmentChars[k++] = j;
						printedChar[j] = YES;
						}
					}
				sprintf (tempStr, "%lf\t", posSelProbs[compressedCharPosition]);
				if (AddToPrintString (tempStr) == ERROR) goto errorExit;
				/*printf ("%4d -> (%3d,%3d,%3d) %1.25lf\n", i, origAlignmentChars[0]+1, origAlignmentChars[1]+1, origAlignmentChars[2]+1, posSelProbs[compressedCharPosition]);*/
				}
			}

		/* free memory */
		if (memAllocs[ALLOC_POSSELPROBS] == YES)
			free (posSelProbs);
		memAllocs[ALLOC_POSSELPROBS] = NO;
		free (printedChar);
		}
	 
	/* if user wants site rates, we print those here */
	if (inferSiteRates == YES)
		{
		for (d=0; d<numCurrentDivisions; d++)
			{
			m = &modelSettings[d];
			if (m->printSiteRates == YES)
				{
				mp = &modelParams[d];
				tree = GetTree (m->brlens, coldId, state[coldId]);
				node = tree->root->left;
				m->PrintSiteRates (node, d, coldId);
				}
			}
		}			

	/* if user wants ancestral states for constrained nodes, we obtain and print those here */
	if (inferAncStates == YES)
		{
		for (d=0; d<numCurrentDivisions; d++)
			{
			m = &modelSettings[d];
			if (m->printAncStates == YES)
				{
				mp = &modelParams[d];
				tree = GetTree (m->brlens, coldId, state[coldId]);
				if (tree->isRooted == NO)
					j = tree->nIntNodes - 1;
				else
					j = tree->nIntNodes - 2;
				for (i=j; i>=0; i--)
					{
					node = tree->intDownPass[i];
					m->CondLikeUp (node, d, coldId);
					}
				for (k=0; k<mp->numActiveConstraints; k++)
					{
					for (i=j; i>=0; i--)
						{
						node = tree->intDownPass[i];
						if (node->isLocked == YES && k == node->lockID)
							m->PrintAncStates (node, d, coldId);
						}
					}
				}
			}
		}			

	sprintf (tempStr, "\n");
	if (AddToPrintString (tempStr) == ERROR) goto errorExit;
		
	return (NO_ERROR);
	
	errorExit:
		if (printedChar)
			free (printedChar);
		if (memAllocs[ALLOC_POSSELPROBS] == YES)
			free (posSelProbs);
		memAllocs[ALLOC_POSSELPROBS] = NO;
		return (ERROR);
	
}





/*----------------------------------------------------------------------
|
|	PrintStatesToFiles: Print trees and model parameters to files. We
|   only come into this function if it is the first cycle of the chain
|   or if we hit a cycle number evenly divisible by the sample frequency,
|   or this is the last cycle of the chain.
|
------------------------------------------------------------------------*/
int PrintStatesToFiles (int curGen)

{

	int				i, j, chn, coldId, runId;
	Tree			*tree=NULL;
#	if defined (MPI_ENABLED)
	int				id, x, doesThisProcHaveId, procWithChain, ierror, tag, nErrors, sumErrors, isTreeCalibrated;
	MPI_Status 		status;
#	endif

#	if !defined (MPI_ENABLED)
	/* print parameter values and trees (single-processor version) */
	for (chn=0; chn<numLocalChains; chn++)
		{
		if ((chainId[chn] % chainParams.numChains) == 0)
			{
			coldId = chn;
			runId = chainId[chn] / chainParams.numChains;

			/* print parameter values */
			if (PrintStates (curGen, coldId) == ERROR)
				return (ERROR);
			fprintf (fpParm[runId], "%s", printString);
			fflush (fpParm[runId]);
			free(printString);

			/* print trees */
			for (i=j=0; i<numTrees; i++)
				{
				tree = GetTreeFromIndex (i, coldId, state[coldId]);
		
				if (PrintTree (curGen, tree) == ERROR)
					return (ERROR);
				fprintf (fpTree[runId][i], "%s", printString);
				fflush (fpTree[runId][i]);
				free(printString);

				if (tree->isCalibrated == YES)
					{
					if (PrintCalTree (curGen, tree) == ERROR)
						return ERROR;
					fprintf (fpCal[runId][j], "%s", printString);
					fflush (fpCal[runId][j++]);
					free(printString);
					}

				if (chainParams.mcmcDiagn == YES && chainParams.numRuns > 1)
					{
					if (chainParams.relativeBurnin == YES || curGen >= chainParams.chainBurnIn * chainParams.sampleFreq)
						{
						if (AddTreeToPartitionCounters (tree, i, runId) == ERROR)
							return ERROR;
						}
					}
					
				}
			}
		}
#	else
	/* print parameter values and trees (parallel version) */
	
	/* Wait for all of the processors to get to this point before starting the printing. */
	ierror = MPI_Barrier (MPI_COMM_WORLD);
	if (ierror != MPI_SUCCESS)
		{
		MrBayesPrint ("%s   Problem at chain barrier.\n", spacer);
		return ERROR;
		}
	tag = nErrors = 0;
		
	/* Loop over runs. */
	for (runId=0; runId<chainParams.numRuns; runId++)
		{
		/* Get the ID of the chain we want to print. Remember, the ID's should be numbered
		   0, 1, 2, ..., numChains X numRuns. Chains numbered 0, numChains, 2 X numChains, ...
		   are cold. */
		id = runId * chainParams.numChains;
		
		/* Does this processor have the chain? */
		doesThisProcHaveId = NO;
		coldId = 0;
		for (chn=0; chn<numLocalChains; chn++)
			{
			if (chainId[chn] == id)
				{
				doesThisProcHaveId = YES;
				coldId = chn;
				break;
				}
			}
		
		/* Tell all the processors which has the chain we want to print. We do this using the MPI_AllReduce
		   function. If the processor does not have the chain, then it initializes x = 0. If it does
		   have the chain, then x = proc_id. When the value of x is summed over all the processors, the sum
		   should be the proc_id of the processor with the chain. Possible values are 0, 1, 2, num_procs-1. 
		   Note that every processor knows procWithChain because we are using MPI_Allreduce, instead of MPI_Reduce. */
		x = 0;
		if (doesThisProcHaveId == YES)
			x = proc_id;
		ierror = MPI_Allreduce (&x, &procWithChain, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
		if (ierror != MPI_SUCCESS)
			{
			MrBayesPrint ("%s   Problem finding processor with chain to print.\n", spacer);
			return (ERROR);
			}

		/* ****************************************************************************************************/
		/* print parameter values *****************************************************************************/
		
		/* Fill printString with the contents to be printed on proc_id = 0. Note
		   that printString is allocated in the function. */
		if (doesThisProcHaveId == YES)
			{
			if (PrintStates (curGen, coldId) == ERROR)
				nErrors++;
			}
		MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
		if (sumErrors > 0)
			{
			MrBayesPrint ("%s   Problem with PrintStates.\n", spacer);
			return ERROR;
			}
		
		/* First communication: Send/receive the length of the printString. */
		if (proc_id == 0 || proc_id == procWithChain)
			{
			if (procWithChain != 0)
				{
				if (proc_id == procWithChain)
					{
					/* Find out how large the string is, and send the information to proc_id = 0. */
					ierror = MPI_Send (&printStringSize, 1, MPI_INT, 0, tag, MPI_COMM_WORLD);
					if (ierror != MPI_SUCCESS)
						nErrors++;
					}
				else
					{
					/* Receive the length of the string from proc_id = procWithChain, and then allocate
					   printString to be that length. */
					ierror = MPI_Recv (&printStringSize, 1, MPI_INT, procWithChain, tag, MPI_COMM_WORLD, &status);
					if (ierror != MPI_SUCCESS)
						{
						MrBayesPrint ("%s   Problem receiving printStringSize from proc_id = %d\n", spacer, procWithChain);
						nErrors++;
						}
					printString = (char *)malloc((size_t) (printStringSize * sizeof(char)));
					if (!printString)
						{
						MrBayesPrint ("%s   Problem allocating printString (%d)\n", spacer, printStringSize * sizeof(char));
						nErrors++;
						}
					strcpy (printString, "");
					}
				}
			}
		MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
		if (sumErrors > 0)
			{
			MrBayesPrint ("%s   Problem with first communication (states).\n", spacer);
			return ERROR;
			}

		/* Second communication: Send/receive the printString. */
		if (proc_id == 0 || proc_id == procWithChain)
			{
			if (procWithChain != 0)
				{					
				if (proc_id == procWithChain)
					{
					/* Send the printString to proc_id = 0. After we send the string to proc_id = 0, we can
					   free it. */
					ierror = MPI_Send (&printString[0], printStringSize, MPI_CHAR, 0, tag, MPI_COMM_WORLD);
					if (ierror != MPI_SUCCESS)
						nErrors++;
					free(printString);
					}
				else
					{
					/* Receive the printString from proc_id = procWithChain. */
					ierror = MPI_Recv (&printString[0], printStringSize, MPI_CHAR, procWithChain, tag, MPI_COMM_WORLD, &status);
					if (ierror != MPI_SUCCESS)
						{
						MrBayesPrint ("%s   Problem receiving printString from proc_id = %d\n", spacer, procWithChain);
						nErrors++;
						}
					}
				}
			}
		MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
		if (sumErrors > 0)
			{
			MrBayesPrint ("%s   Problem with second communication (states).\n", spacer);
			return ERROR;
			}

		/* Print the string with the parameter information if we are proc_id = 0. */
		if (proc_id == 0)
			{
			fprintf (fpParm[runId], "%s", printString);
			fflush (fpParm[runId]);
			free(printString);
			}

		/* ****************************************************************************************************/
		/* print trees ****************************************************************************************/
		for (i=j=0; i<numTrees; i++)
			{
			/* First, print the regular tree to the file. */
			
			/* Fill printString with the contents to be printed on proc_id = 0. Note
			   that printString is allocated in the function. */
			if (doesThisProcHaveId == YES)
				{
				tree = GetTreeFromIndex (i, coldId, state[coldId]);
				if (PrintTree (curGen, tree) == ERROR)
					nErrors++;
				}
			MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
			if (sumErrors > 0)
				{
				MrBayesPrint ("%s   Problem with PrintStates.\n", spacer);
				return ERROR;
				}
				
			/* Communicate whether the tree is calibrated or not. We use the usual trick, with all processors first
			   initializing x = NO (0). The processor with the chain then initializes x to NO/YES (0/1), and the result
			   for x is summed over all the processors. If the sum is 0, then the tree is not calibrated. Otherwise,
			   the sum will be 1, and the tree is calibrated. */
			x = NO;
			if (doesThisProcHaveId == YES)
				x = tree->isCalibrated;			
			ierror = MPI_Allreduce (&x, &isTreeCalibrated, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
			if (ierror != MPI_SUCCESS)
				{
				MrBayesPrint ("%s   Problem deciding whether the tree is calibrated.\n", spacer);
				return (ERROR);
				}
					
			/* First communication: Send/receive the length of the printString. */
			if (proc_id == 0 || proc_id == procWithChain)
				{
				if (procWithChain != 0)
					{
					if (proc_id == procWithChain)
						{
						/* Find out how large the string is, and send the information to proc_id = 0. */
						ierror = MPI_Send (&printStringSize, 1, MPI_INT, 0, tag, MPI_COMM_WORLD);
						if (ierror != MPI_SUCCESS)
							nErrors++;
						}
					else
						{
						/* Receive the length of the string from proc_id = procWithChain, and then allocate
						   printString to be that length. */
						ierror = MPI_Recv (&printStringSize, 1, MPI_INT, procWithChain, tag, MPI_COMM_WORLD, &status);
						if (ierror != MPI_SUCCESS)
							{
							MrBayesPrint ("%s   Problem receiving printStringSize from proc_id = %d\n", spacer, procWithChain);
							nErrors++;
							}
						printString = (char *)malloc((size_t) (printStringSize * sizeof(char)));
						if (!printString)
							{
							MrBayesPrint ("%s   Problem allocating printString (%d)\n", spacer, printStringSize * sizeof(char));
							nErrors++;
							}
						strcpy (printString, "");
						}
					}
				}
			MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
			if (sumErrors > 0)
				{
				MrBayesPrint ("%s   Problem with first communication (states).\n", spacer);
				return ERROR;
				}

			/* Second communication: Send/receive the printString. */
			if (proc_id == 0 || proc_id == procWithChain)
				{
				if (procWithChain != 0)
					{					
					if (proc_id == procWithChain)
						{
						/* Send the printString to proc_id = 0. After we send the string to proc_id = 0, we can
						   free it. */
						ierror = MPI_Send (&printString[0], printStringSize, MPI_CHAR, 0, tag, MPI_COMM_WORLD);
						if (ierror != MPI_SUCCESS)
							nErrors++;
						free(printString);
						}
					else
						{
						/* Receive the printString from proc_id = procWithChain. */
						ierror = MPI_Recv (&printString[0], printStringSize, MPI_CHAR, procWithChain, tag, MPI_COMM_WORLD, &status);
						if (ierror != MPI_SUCCESS)
							{
							MrBayesPrint ("%s   Problem receiving printString from proc_id = %d\n", spacer, procWithChain);
							nErrors++;
							}
						}
					}
				}
			MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
			if (sumErrors > 0)
				{
				MrBayesPrint ("%s   Problem with second communication (states).\n", spacer);
				return ERROR;
				}

			/* Print the string with the parameter information if we are proc_id = 0. */
			if (proc_id == 0)
				{
				fprintf (fpTree[runId][i], "%s", printString);
				fflush (fpTree[runId][i]);
				free(printString);
				}

			/* Second, print the calibration tree to the file. */
			if (isTreeCalibrated == YES)
				{
				/* Fill printString with the contents to be printed on proc_id = 0. Note
				   that printString is allocated in the function. */
				if (doesThisProcHaveId == YES)
					{
					tree = GetTreeFromIndex (i, coldId, state[coldId]);
					if (PrintCalTree (curGen, tree) == ERROR)
						nErrors++;
					}
				MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
				if (sumErrors > 0)
					{
					MrBayesPrint ("%s   Problem with PrintStates.\n", spacer);
					return ERROR;
					}
			
				/* First communication: Send/receive the length of the printString. */
				if (proc_id == 0 || proc_id == procWithChain)
					{
					if (procWithChain != 0)
						{
						if (proc_id == procWithChain)
							{
							/* Find out how large the string is, and send the information to proc_id = 0. */
							ierror = MPI_Send (&printStringSize, 1, MPI_INT, 0, tag, MPI_COMM_WORLD);
							if (ierror != MPI_SUCCESS)
								nErrors++;
							}
						else
							{
							/* Receive the length of the string from proc_id = procWithChain, and then allocate
							   printString to be that length. */
							ierror = MPI_Recv (&printStringSize, 1, MPI_INT, procWithChain, tag, MPI_COMM_WORLD, &status);
							if (ierror != MPI_SUCCESS)
								{
								MrBayesPrint ("%s   Problem receiving printStringSize from proc_id = %d\n", spacer, procWithChain);
								nErrors++;
								}
							printString = (char *)malloc((size_t) (printStringSize * sizeof(char)));
							if (!printString)
								{
								MrBayesPrint ("%s   Problem allocating printString (%d)\n", spacer, printStringSize * sizeof(char));
								nErrors++;
								}
							strcpy (printString, "");
							}
						}
					}
				MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
				if (sumErrors > 0)
					{
					MrBayesPrint ("%s   Problem with first communication (states).\n", spacer);
					return ERROR;
					}

				/* Second communication: Send/receive the printString. */
				if (proc_id == 0 || proc_id == procWithChain)
					{
					if (procWithChain != 0)
						{					
						if (proc_id == procWithChain)
							{
							/* Send the printString to proc_id = 0. After we send the string to proc_id = 0, we can
							   free it. */
							ierror = MPI_Send (&printString[0], printStringSize, MPI_CHAR, 0, tag, MPI_COMM_WORLD);
							if (ierror != MPI_SUCCESS)
								nErrors++;
							free(printString);
							}
						else
							{
							/* Receive the printString from proc_id = procWithChain. */
							ierror = MPI_Recv (&printString[0], printStringSize, MPI_CHAR, procWithChain, tag, MPI_COMM_WORLD, &status);
							if (ierror != MPI_SUCCESS)
								{
								MrBayesPrint ("%s   Problem receiving printString from proc_id = %d\n", spacer, procWithChain);
								nErrors++;
								}
							}
						}
					}
				MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
				if (sumErrors > 0)
					{
					MrBayesPrint ("%s   Problem with second communication (states).\n", spacer);
					return ERROR;
					}

				/* Print the string with the parameter information if we are proc_id = 0. */
				if (proc_id == 0)
					{
					fprintf (fpCal[runId][j], "%s", printString);
					fflush (fpCal[runId][j++]);
					free(printString);
					}
				}
			}

		/* Have all of the chains wait here, until the string has been successfully printed on proc_id = 0. */
		ierror = MPI_Barrier (MPI_COMM_WORLD);
		if (ierror != MPI_SUCCESS)
			{
			MrBayesPrint ("%s   Problem at chain barrier.\n", spacer);
			return ERROR;
			}
		}
#	endif
		
	return (NO_ERROR);
	
}




int PrintSwapInfo (void)

{

	int			i, j, n, maxNumExchanges, len, maxLen, reweightingChars=0;
	char		temp[50];

	if (chainParams.numChains == 1)
		return NO_ERROR;

#	if defined (MPI_ENABLED)
	if (ReassembleSwapInfo() == ERROR)
		return ERROR;
	if (proc_id != 0)
		return NO_ERROR;
#	endif

	for (n=0; n<chainParams.numRuns; n++)
		{
		maxNumExchanges = 0;
		for (i=0; i<chainParams.numChains; i++)
			for (j=0; j<chainParams.numChains; j++)
				if (i > j && swapInfo[n][i][j] > maxNumExchanges)
					maxNumExchanges = swapInfo[n][i][j];
		sprintf (temp, "%d", maxNumExchanges);
		maxLen = (int) strlen(temp);
		if (maxLen < 4)
			maxLen = 4;
			
		reweightingChars = NO;
		if ((chainParams.weightScheme[0] + chainParams.weightScheme[1]) > 0.00001)
			reweightingChars = YES;

		if (chainParams.numRuns == 1)
			MrBayesPrint ("\n%s   Chain swap information:\n\n", spacer);
		else
			MrBayesPrint ("\n%s   Chain swap information for run %d:\n\n", spacer, n+1);

		MrBayesPrint ("%s          ", spacer);
		for (j=0; j<chainParams.numChains; j++)
			{
			sprintf (temp, "%d", j+1);
			len = (int) strlen(temp);
			MrBayesPrint ("%*c %d ", maxLen-len, ' ', j+1);
			}
		MrBayesPrint ("\n");
		
		MrBayesPrint ("%s        --", spacer);
		for (j=0; j<chainParams.numChains; j++)
			{
			MrBayesPrint ("--");
			for (i=0; i<maxLen; i++)
				MrBayesPrint ("-");
			}
		MrBayesPrint ("\n");
		
		for (i=0; i<chainParams.numChains; i++)
			{
			MrBayesPrint ("%s   %4d | ", spacer, i+1);
			for (j=0; j<chainParams.numChains; j++)
				{
				if (i < j)
					{
					if (swapInfo[n][j][i] <= 0)
						{
						MrBayesPrint ("%*c%s ", maxLen-3, ' ', " N/A");
						}
					else
						{
						sprintf (temp, "%1.2lf", (MrBFlt)swapInfo[n][i][j]/swapInfo[n][j][i]);
						len = (int) strlen(temp);
						MrBayesPrint ("%*c%1.2lf ", maxLen-len+1, ' ', (MrBFlt)swapInfo[n][i][j]/swapInfo[n][j][i]);
						}
					}
				else if (i == j)
					{
					MrBayesPrint ("%*c ", maxLen+1, ' ');
					}
				else
					{
					sprintf (temp, "%d", swapInfo[n][i][j]);
					len = (int) strlen(temp);
					MrBayesPrint ("%*c%d ", maxLen-len+1, ' ', swapInfo[n][i][j]);
					}
				}
			MrBayesPrint ("\n");
			}
		}

	MrBayesPrint ("\n%s   Upper diagonal: Proportion of successful state exchanges between chains\n", spacer);
	MrBayesPrint ("%s   Lower diagonal: Number of attempted state exchanges between chains\n", spacer);
		
	MrBayesPrint ("\n%s   Chain information:\n\n", spacer);
	MrBayesPrint ("%s     ID -- Heat ", spacer);
	if (reweightingChars == YES)
		MrBayesPrint ("%% Dn %% Up\n");
	else
		MrBayesPrint ("\n");
	
	MrBayesPrint ("%s    -----------", spacer);
	if (reweightingChars == YES)
		MrBayesPrint ("----------\n");
	else
		MrBayesPrint ("\n");
	for (i=0; i<chainParams.numChains; i++)
		{
		MrBayesPrint ("%s   %4d -- %1.2lf ", spacer, i+1, Temperature (i)/*1.0 / (1.0 + chainParams.chainTemp * i)*/);
		if (reweightingChars == YES)
			{
			if (i == 0)
				{
				MrBayesPrint ("  0%%   0%% (cold chain)\n");
				}
			else
				{
				sprintf (temp, "%d", (int)chainParams.weightScheme[0]);
				len = (int) strlen(temp);
				MrBayesPrint ("%*c%d%% ", 3-len, ' ', (int)chainParams.weightScheme[0]);
				sprintf (temp, "%d", (int)chainParams.weightScheme[1]);
				len = (int) strlen(temp);
				MrBayesPrint ("%*c%d%% \n", 3-len, ' ', (int)chainParams.weightScheme[1]);
				}
			}
		else
			{
			if (i == 0)
				MrBayesPrint (" (cold chain)\n");
			else
				MrBayesPrint ("\n");
			}
		}
	if (chainParams.userDefinedTemps == NO)
		{
		MrBayesPrint ("\n%s   Heat = 1 / (1 + T * (ID - 1))\n", spacer);
		MrBayesPrint ("%s      (where T = %1.2lf is the temperature and ID is the chain number)\n", spacer, chainParams.chainTemp);
		}
	if (reweightingChars == YES)
		MrBayesPrint ("%s   Reweighting increment = %1.2lf\n", spacer, chainParams.weightScheme[2]);
		
	return (NO_ERROR);
		
}




/*----------------------------------------------------------------------
|
|	PrintTermState: Print terminal state index matrix
|
------------------------------------------------------------------------*/
int PrintTermState (void)

{

	int				i, j=0, c, d, printWidth, nextColumn, nDigits, nReps;
	char			tempName[100];
	ModelInfo		*m;
	ModelParams		*mp;

	if (!parsMatrix)
		return ERROR;

	printWidth = 79;

	for (d=0; d<numCurrentDivisions; d++)
		{
		MrBayesPrint ("\nTerminal state index matrix for division %d\n\n", d+1);

		m = &modelSettings[d];
		mp = &modelParams[d];

		if (!strcmp(mp->covarionModel, "Yes"))
			nReps = 2;
		else
			nReps = 1;

		nDigits = 1 + (int)(log10(mp->nStates * mp->nStates * nReps));
	
		for (c=m->compCharStart; c<m->compCharStop; c=j)
			{
			for (i=0; i<numTaxa; i++)
				{
				GetNameFromString(taxaNames, tempName, i+1);
				MrBayesPrint ("%-10.10s   ", tempName);
				j = c;
				for (nextColumn=13; nextColumn < printWidth; nextColumn+=nDigits + 1)
					{
					if (j >= m->compCharStop)
						break;
					MrBayesPrint ("%*d ",nDigits, termState[pos(i,j++,numCompressedChars)]);
					}
				MrBayesPrint ("\n");
				}
			MrBayesPrint("\n");
			}
		}	/* next division */

	return NO_ERROR;

}





/*--------------------------------------------------
|
|	PrintTiProbs: This function is for debugging of
|		tiProbs; it will print a square matrix of
|		tiProbs, check row sums, and check for time
|		reversibility
|
---------------------------------------------------*/
void PrintTiProbs (MrBFlt *tP, MrBFlt *bs, int nStates)

{
	int		i, j;
	MrBFlt	*tiP, sum;

	tiP = tP;

	printf ("\nTransition matrix\n");
	for (i=0; i<nStates; i++)
		{
		printf ("\t%d", i);
		}
	printf ("\tsum\n");

	for (i=0; i<nStates; i++)
		{
		printf ("%d\t", i);
		sum = 0.0;
		for (j=0; j<nStates; j++)
			{
			printf ("\t%.6f",tP[j]);
			sum += tP[j];
			}
		printf ("\t%.6f\n",sum);
		tP += nStates;
		}

	printf ("\nTime reversibility\n");

	printf ("State 1\tState 2\tforward\tbackward\tabs diff\n");
	for (i=0; i<nStates; i++)
		{
		for (j=i+1; j<nStates; j++)
			{
			printf ("%d\t%d\t%.6f\t%.6f\t%.6f\n", i, j, tiP[i*nStates+j]*bs[i],
				tiP[j*nStates+i]*bs[j], fabs(tiP[i*nStates+j]*bs[i] - tiP[j*nStates+i]*bs[j]));
			}
		}

	getchar();
	return;
}





int PrintTopConvInfo (void)

{

	int			i, j, n, len, maxLen;
	char		temp[50];
	MrBFlt		maxNumPartitions;
	STATS		*stat;

	if (chainParams.numRuns == 1)
		return NO_ERROR;

#	if defined (MPI_ENABLED)
	if (proc_id != 0)
		return (NO_ERROR);
#	endif

	for (n=0; n<numTrees; n++)
		{
		stat = &chainParams.stat[n];
		maxNumPartitions = 0.0;
		for (i=0; i<chainParams.numRuns; i++)
			for (j=0; j<chainParams.numRuns; j++)
				if (i > j && stat->pair[i][j] > maxNumPartitions)
					maxNumPartitions = stat->pair[i][j];
		sprintf (temp, "%d", (int) maxNumPartitions);
		maxLen = (int) strlen(temp);
		if (maxLen < 5)
			maxLen = 5;
		
		if (numTrees == 1)
			{
			MrBayesPrint ("%s   Pairwise average standard deviation of split frequencies (upper diagonal)\n", spacer);
			MrBayesPrint ("%s      and number of qualifying splits for each comparison (lower diagonal):\n\n", spacer);
			}
		else
			{
			MrBayesPrint ("%s   Pairwise average standard deviation of split frequencies in tree %d (upper diagonal)\n", spacer, n);
			MrBayesPrint ("%s      and number of qualifying splits for each comparison (lower diagonal):\n\n", spacer);
			}

		MrBayesPrint ("%s          ", spacer);
		for (j=0; j<chainParams.numRuns; j++)
			{
			sprintf (temp, "%d", j+1);
			len = (int) strlen(temp);
			MrBayesPrint ("%*c %d ", maxLen-len, ' ', j+1);
			}
		MrBayesPrint ("\n");
	
		MrBayesPrint ("%s        --", spacer);
		for (j=0; j<chainParams.numRuns; j++)
			{
			MrBayesPrint ("--");
			for (i=0; i<maxLen; i++)
				MrBayesPrint ("-");
			}
		MrBayesPrint ("\n");
	
		for (i=0; i<chainParams.numRuns; i++)
			{
			MrBayesPrint ("%s   %4d | ", spacer, i+1);
			for (j=0; j<chainParams.numRuns; j++)
				{
				if (i < j)
					{
					sprintf (temp, "%1.3lf", stat->pair[i][j]/stat->pair[j][i]);
					len = (int) strlen(temp);
					MrBayesPrint ("%*c%1.3lf ", maxLen-len+1, ' ', stat->pair[i][j]/stat->pair[j][i]);
					}
				else if (i == j)
					{
					MrBayesPrint ("%*c ", maxLen+1, ' ');
					}
				else
					{
					sprintf (temp, "%d", (int) stat->pair[i][j]);
					len = (int) strlen(temp);
					MrBayesPrint ("%*c%d ", maxLen-len+1, ' ', (int) stat->pair[i][j]);
					}
				}
			MrBayesPrint ("\n");
			}
	
		MrBayesPrint ("\n");
		}

	return (NO_ERROR);
}


void PrintToScreen (int curGen, time_t endingT, time_t startingT)
{

	int			i, chn, nHours, nMins, nSecs;
	MrBFlt		timePerGen;

#	if defined (MPI_ENABLED)
	int			numLocalColdChains, numFirstAndLastCold;
	
	if (curGen == 1)
		{
		MrBayesPrint ("\n");
		MrBayesPrint ("%s   Chain results:\n\n", spacer);
		}
	MrBayesPrint ("%s   %4d -- ", spacer, curGen);
	numLocalColdChains = numFirstAndLastCold = 0;
	for (chn=0; chn<numLocalChains; chn++)
		{
		if ((chainId[chn] % chainParams.numChains) == 0)
			{
			numLocalColdChains++;
			if (chn == 0 || chn == numLocalChains - 1)
				numFirstAndLastCold++;
			}
		}

	i = 0;
	for (chn=0; chn<numLocalChains; chn++)
		{
		if (i > chainParams.printMax)	
			{
			if (i == chainParams.printMax + 1)
				{
				i++;
				if (numLocalColdChains > 0 && numLocalColdChains > numFirstAndLastCold)
					MrBayesPrint ("[...%d more local chains...] ", numLocalChains - 2);
				else
					MrBayesPrint ("(...%d more local chains...) ", numLocalChains - 2);
				continue;
				}
			else
				continue;
			}
		if ((chainId[chn] % chainParams.numChains) == 0)
			{
			i++;
			if (chainParams.printAll == YES)
				MrBayesPrint ("[%1.3lf] ", curLnL[chn]);
			else
				MrBayesPrint ("[%1.3lf] .. ", curLnL[chn]);
			}
		else if (chainParams.printAll == YES)
			{
			i++;
			MrBayesPrint ("(%1.3lf) ", curLnL[chn]);
			}
		if (chn < numLocalChains - 1 && (chainId[chn] / chainParams.numChains != chainId[chn+1] / chainParams.numChains))
			MrBayesPrint ("* ");
		}
		
	if (numLocalColdChains == chainParams.numRuns)
		MrBayesPrint ("(...%d remote chains...) ", (chainParams.numChains*chainParams.numRuns) - numLocalChains);
	else
		MrBayesPrint ("[...%d remote chains...] ", (chainParams.numChains*chainParams.numRuns) - numLocalChains);

	if (curGen > 1)
		{
		timePerGen = (MrBFlt) (((float) endingT-startingT)/curGen);
		nSecs = (int)((chainParams.numGen - curGen) * timePerGen);
		nHours = (int)nSecs / 3600;
		nSecs -= nHours * 3600;
		nMins = nSecs / 60; 
		nSecs -= nMins * 60;
		MrBayesPrint ("-- %d:", nHours);
		if (nMins > 9)
			MrBayesPrint ("%d:", nMins);
		else
			MrBayesPrint ("0%d:", nMins);
		if (nSecs > 9)
			MrBayesPrint ("%d", nSecs);
		else
			MrBayesPrint ("0%d", nSecs);
		}
	MrBayesPrint ("\n");
	fflush (stdout);
	
#	else

	if (curGen == 1)
		{
		MrBayesPrint ("\n");
		MrBayesPrint ("%s   Chain results:\n\n", spacer);
		}
	MrBayesPrint ("%s   %5d -- ", spacer, curGen);
	if (numLocalChains == 1)
		MrBayesPrint ("%1.3lf ", curLnL[0]);
	else
		{
		i = 0;
		for (chn=0; chn<numLocalChains; chn++)
			{
			if (i >= chainParams.printMax)
				{
				if (i == chainParams.printMax)
					MrBayesPrint (".. ");
				i++;
				continue;
				}
			if (chainParams.numChains == 1)
				{
				MrBayesPrint ("%1.3lf ", curLnL[chn]);
				i++;
				}
			else if (chainId[chn] % chainParams.numChains == 0)
				{
				if (chainParams.printAll == YES)
					MrBayesPrint ("[%1.3lf] ", curLnL[chn]);
				else
					MrBayesPrint ("[%1.3lf][%d] .. ", curLnL[chn], chn % chainParams.numChains + 1);
				i++;
				}
			else if (chainParams.printAll == YES)
				{
				MrBayesPrint ("(%1.3lf) ", curLnL[chn]);
				i++;
				}
			if (chn < numLocalChains - 1 && (chainId[chn] / chainParams.numChains != chainId[chn+1] / chainParams.numChains)
				&& i < chainParams.printMax - 1)
				MrBayesPrint ("* ");
			}
		}
		
	if (curGen > 1)
		{
		timePerGen = (MrBFlt) (((float) endingT-startingT)/curGen);
		nSecs = (int)((chainParams.numGen - curGen) * timePerGen);
		nHours = (int)nSecs / 3600;
		nSecs -= nHours * 3600;
		nMins = nSecs / 60; 
		nSecs -= nMins * 60;
		MrBayesPrint ("-- %d:", nHours);
		if (nMins > 9)
			MrBayesPrint ("%d:", nMins);
		else
			MrBayesPrint ("0%d:", nMins);
		if (nSecs > 9)
			MrBayesPrint ("%d", nSecs);
		else
			MrBayesPrint ("0%d", nSecs);
		}
	MrBayesPrint ("\n");
	
	fflush (stdout);
	
#	endif
		
}





int PrintTree (int curGen, Tree *tree)

{

	int				i, counter;
	char			tempNameStr[100], tempStr[200];
	TreeNode		*p=NULL, *q;

	/* allocate the print string */
	printStringSize = 200;
	printString = (char *)malloc((size_t) (printStringSize * sizeof(char)));
	if (!printString)
		{
		MrBayesPrint ("%s   Problem allocating printString (%d)\n", spacer, printStringSize * sizeof(char));
		return (ERROR);
		}
	strcpy (printString, "");

	/* order the taxa */
	if (chainParams.orderTaxa == YES)
		{
		for (i=0; i<tree->nNodes-1; i++)
			{
			p = tree->allDownPass[i];
			if (p->left == NULL)
				{
				if (p->index == localOutGroup)
					p->x = -1;
				else
					p->x = p->index;
				}
			else if (p->left->x < p->right->x)
				p->x = p->left->x;
			else
				p->x = p->right->x;
			}
		for (i=0; i<tree->nIntNodes; i++)
			{
			if (p->left->x > p->right->x)
				{
				q = p->left;
				p->left = p->right;
				p->right = q;
				}
			}
		}
	
	/* print the translate block information and the top of the file */
	if (curGen == 1)
		{
		/* print #NEXUS and translation block information */
		sprintf (tempStr, "#NEXUS\n");
		if (AddToPrintString (tempStr) == ERROR) return(ERROR);
		sprintf (tempStr, "[ID: %s]\n", stamp);
		if (AddToPrintString (tempStr) == ERROR) return(ERROR);
		sprintf (tempStr, "begin trees;\n");
		if (AddToPrintString (tempStr) == ERROR) return(ERROR);
		sprintf (tempStr, "   translate\n");
		if (AddToPrintString (tempStr) == ERROR) return(ERROR);
		counter = 0;
		for (i=0; i<numTaxa; i++)
			{
			if (taxaInfo[i].isDeleted == YES)
				continue;
			counter++;
			if (GetNameFromString (taxaNames, tempNameStr, i+1) == ERROR)
				{
				MrBayesPrint ("%s   Could not find taxon %d\n", spacer, i+1);
				return (ERROR);
				}
			if (counter == numLocalTaxa)
				sprintf (tempStr, "      %2d %s;\n", counter, tempNameStr);
			else
				sprintf (tempStr, "      %2d %s,\n", counter, tempNameStr);
			if (AddToPrintString (tempStr) == ERROR) return(ERROR);
			}
		}
	
	/* write the tree in Newick format */
	sprintf (tempStr, "   tree rep.%d = ", curGen);
	if (AddToPrintString (tempStr) == ERROR) return(ERROR);
   	WriteTreeToFile (tree->root->left, chainParams.saveBrlens, tree->isRooted);
   	sprintf (tempStr, ";\n");
	if (AddToPrintString (tempStr) == ERROR) return(ERROR);
   		   		
   	return (NO_ERROR);
		
}





/*--------------------------------------------------------------
|
|	ProcessStdChars: help function used by CompressData
|		to handle standard characters
|
---------------------------------------------------------------*/
int ProcessStdChars (void)

{

	int				c, d, j, k, numStandardChars;
	ModelInfo		*m;
	ModelParams		*mp;

	/* set character type, no. states, ti index and bs index for standard characters */
	/* first calculate how many standard characters we have */
	numStandardChars = 0;
	for (d=0; d<numCurrentDivisions; d++)
		{
		mp = &modelParams[d];
		m = &modelSettings[d];

		if (mp->dataType != STANDARD)
			continue;

		numStandardChars += m->numChars;
		}
	
	/* return if there are no standard characters */
	if (numStandardChars == 0)
		return NO_ERROR;

	/* we are still here so we have standard characters and need to deal with them */
	
	/* first allocate space for stdType, stateSize, tiIndex, bsIndex */
	if (memAllocs[ALLOC_STDTYPE] == YES)
		{
		MrBayesPrint ("%s   stdType not free in CompressData\n", spacer);
		return ERROR;
		}
	stdType = (int *)calloc((size_t) (4 * numStandardChars), sizeof(int));
	if (!stdType)
		{
		MrBayesPrint ("%s   Problem allocating stdType (%d ints)\n", 4 * numStandardChars);
		return ERROR;
		}
	memAllocs[ALLOC_STDTYPE] = YES;
	stateSize = stdType + numStandardChars;
	tiIndex = stateSize + numStandardChars;
	bsIndex = tiIndex + numStandardChars;

	/* then fill in stdType and stateSize, set pointers */
	/* also fill in isTiNeeded for each division and tiIndex for each character */
	for (d=j=0; d<numCurrentDivisions; d++)
		{
		mp = &modelParams[d];
		m = &modelSettings[d];
		
		if (mp->dataType != STANDARD)
			continue;

		/* set m->numBetaCats */
		if (!(AreDoublesEqual(mp->symBetaFix, -1.0, 0.00001) == YES && !strcmp(mp->symPiPr,"Fixed")))
			m->numBetaCats = mp->numBetaCats;
		else
			m->numBetaCats = 1;

		if (activeParams[P_SHAPE][d] > 0)
			m->numGammaCats = mp->numGammaCats;
		else
			m->numGammaCats = 1;

		m->cType = stdType + j;
		m->nStates = stateSize + j;
		m->tiIndex = tiIndex + j;
		m->bsIndex = bsIndex + j;

		for (c=0; c<m->numChars; c++)
			{
			if (origChar[c+m->compMatrixStart] < 0)
				{
				/* this is a dummy character */
				m->cType[c] = UNORD;
				m->nStates[c] = 2;
				}
			else
				{
				/* this is an ordinary character */
				m->cType[c] = charInfo[origChar[c + m->compMatrixStart]].ctype;
				m->nStates[c] = charInfo[origChar[c + m->compMatrixStart]].numStates;
				}
			
			/* check ctype settings */
			if (m->nStates[c] < 2)
				{
				MrBayesPrint ("%s   WARNING: Compressed character %d of division %d has less than two observed\n", spacer, c+m->compCharStart, d+1);
				MrBayesPrint ("%s            states; it will be assumed to have two states.\n", spacer);
				m->nStates[c] = 2;
				}
			if (m->nStates[c] > 6 && m->cType[c] != UNORD)
				{
				MrBayesPrint ("%s   Only unordered model supported for characters with more than 6 states\n", spacer);
				return ERROR;
				}
			if (m->nStates[c] == 2 && m->cType[c] == ORD)
				m->cType[c] = UNORD;
			if (m->cType[c] == IRREV)
				{
				MrBayesPrint ("%s   Irreversible model not yet supported\n", spacer);
				return ERROR;
				}
			
			/* set the ti probs needed */
			if (m->cType[c] == UNORD)
				m->isTiNeeded[m->nStates[c]-2] = YES;
			if (m->cType[c] == ORD)
				m->isTiNeeded[m->nStates[c]+6] = YES;
			if (m->cType[c] == IRREV)
				m->isTiNeeded[m->nStates[c]+11] = YES;
			}

		/* set ti index for each compressed character here          */
		/* set bs index later (in SetChainParams) when we know more */
		/* first for unordered characters */
		for (k=0; k<9; k++)
			{
			if (m->isTiNeeded [k] == NO)
				continue;

			for (c=0; c<m->numChars; c++)
				{
				if (m->cType[c] != UNORD || m->nStates[c] > k + 2)
					{
					m->tiIndex[c] += (k + 2) * (k + 2) * m->numGammaCats;
					}
				}
			}

		/* second for ordered characters */
		for (k=9; k<13; k++)
			{
			if (m->isTiNeeded [k] == NO)
				continue;

			for (c=0; c<m->numChars; c++)
				{
				if (m->cType[c] == IRREV || (m->cType[c] == ORD && m->nStates[c] > k - 6))
					{
					m->tiIndex[c] += (k - 6) * (k - 6) * m->numGammaCats;
					}
				}
			}

		/* third for irrev characters */
		for (k=13; k<18; k++)
			{
			if (m->isTiNeeded [k] == NO)
				continue;

			for (c=0; c<m->numChars; c++)
				{
				if (m->cType[c] == IRREV && m->nStates[c] > k - 11)
					{
					m->tiIndex[c] += (k - 11) * (k - 11) * m->numGammaCats;
					}
				}
			}

		/* finally take beta cats into account in tiIndex        */
		/* the beta cats will only be used for binary characters */
		if (m->numBetaCats > 1 && m->isTiNeeded[0] == YES)
			{
			for (c=0; c<m->numChars; c++)
				{
				if (m->nStates[c] > 2)
					{
					m->tiIndex[c] += 4 * (m->numBetaCats - 1) * m->numGammaCats;
					}
				}
			}
		j += m->numChars;
		}

	return NO_ERROR;
	
}





/*--------------------------------------------------------------------
|
|		RandResolve: Randomly resolve a polytomous tree
|
---------------------------------------------------------------------*/
int RandResolve (Tree *destination, PolyTree *t, long *seed)

{

	int			i, j, nextNode, stopNode, rand1, rand2;
	PolyNode	*p=NULL, *q, *r, *s, *u;

	/* count immediate descendants */
	GetPolyDownPass(t);
	for (i=0; i<t->nIntNodes; i++)
		{
		p = t->intDownPass[i];
		j = 0;
		for (q=p->left; q!=NULL; q=q->sib)
			j++;
		p->x = j;
		}

	/* add one node at a time */
	if (destination->isRooted == NO)
		stopNode = 2*numLocalTaxa - 2;
	else
		stopNode = 2*numLocalTaxa - 1;
	for (nextNode=t->nNodes; nextNode < stopNode; nextNode++)
		{
		/* find a polytomy to break */
		for (i=0; i<t->nIntNodes; i++)
			{
			p = t->intDownPass[i];
			if (destination->isRooted == YES && p->x > 2)
				break;
			if (destination->isRooted == NO && ((p->anc != NULL && p->x > 2) || (p->anc == NULL && p->x > 3)))
				break;
			}

		/* if we can't find one, there's an error */
		if (i == t->nIntNodes)
			return  ERROR;

		/* identify two descendants randomly */
		/* make sure we do not select outgroup if it is an unrooted tree */
		if (p->anc == NULL && destination->isRooted == NO)
			j = p->x - 1;
		else
			j = p->x;
		rand1 = (int) (RandomNumber(seed) * j);
		rand2 = (int) (RandomNumber(seed) *(j-1));
		if (rand2 == rand1)
			rand2 = j-1;

		/* create a new node */
		u = &t->nodes[nextNode];
		u->anc = p;
		u->x = 2;
		p->x --;
		t->nNodes++;
		t->nIntNodes++;
		
		/* connect tree together */
		r = s = NULL;
		for (q = p->left, j=0; q!= NULL; q = q->sib, j++)
			{
			if (rand1 == j || rand2 == j)
				{
				q->anc = u;
				if (s == NULL)
					u->left = q;
				else
					s->sib = q;
				s = q;
				}
			else
				{
				if (r == NULL)
					u->sib = q;
				else
					r->sib = q;
				r = q;
				}
			}
		r->sib = NULL;
		s->sib = NULL;
		p->left = u;

		/* update tree */
		GetPolyDownPass (t);
		}
	return NO_ERROR;
}




#if defined (MPI_ENABLED)
int ReassembleMoveInfo (void)
{
	int		i, j, x, sum, ierror;
	MCMCMove	*mv;

	for (i=0; i<numMoves; i++)
		{
		mv = &moves[i];
		for (j=0; j<chainParams.numRuns*chainParams.numChains; j++)
			{
			if (proc_id == 0)
				x = 0;
			else
				x = mv->nAccepted[j];
			ierror = MPI_Allreduce (&x, &sum, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
			if (ierror != MPI_SUCCESS)
				return (ERROR);
			if (proc_id == 0)
				mv->nAccepted[j] += sum;
			else
				mv->nAccepted[j] = 0;

			if (proc_id == 0)
				x = 0;
			else
				x = mv->nTried[j];
			ierror = MPI_Allreduce (&x, &sum, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
			if (ierror != MPI_SUCCESS)
				return (ERROR);
			if (proc_id == 0)
				mv->nTried[j] += sum;
			else
				mv->nTried[j] = 0;
			}
		}
	return (NO_ERROR);
}




int ReassembleSwapInfo (void)
{
	int	i, j, n, x, sum, ierror;
	
	for (n=0; n<chainParams.numRuns; n++)
		{
		for (i=0; i<chainParams.numChains; i++)
			{
			for (j=0; j<chainParams.numChains; j++)
				{
				if (i != j)
					{
					if (proc_id == 0)
						x = 0;
					else
						x = swapInfo[n][i][j];
					ierror = MPI_Allreduce (&x, &sum, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
					if (ierror != MPI_SUCCESS)
						return (ERROR);
					if (proc_id == 0)
						swapInfo[n][i][j] += sum;
					else
						swapInfo[n][i][j] = 0;
					}
				}
			}
		}

	return (NO_ERROR);
}
#endif




int RecreateTree (Tree *t, char *s)
{
	TreeNode	*p, *q;
	int			i, j, k, inLength;
	char		temp[30];
	
	/* set all pointers to NULL */
	for (i=0; i<t->nNodes; i++)
		{
		p = &t->nodes[i];
		p->anc = p->right = p->left = NULL;
		p->index = -1;
		}
	p = &t->nodes[0];

	inLength = NO;
	for (i=0, j=1; *s!='\0'; s++)
		{
		if (*s == ',' || *s == ')' || *s == ':')
			{
			if (p->right == NULL && inLength == NO)
				{
				temp[i] = '\0';
				k = atoi (temp);
				p->index = k-1;
				i = 0;
				}
			else
				inLength = NO;
			}
		if (*s == '(')
			{
			q = p;
			p = &t->nodes[j++];
			q->left = p;
			p->anc = q;
			}
		else if (*s == ',')
			{
			if (p->anc->right == NULL)
				{
				q = p->anc;
				p = &t->nodes[j++];
				p->anc = q;
				q->right = p;
				}
			else /* if p->anc->right == p (near 'root' of unrooted trees) */
				{
				q = p->anc;
				p = &t->nodes[j++];
				q->anc = p;
				p->left = q;
				}
			}
		else if (*s == ')')
			{
			p = p->anc;
			}
		else if (*s == ':')
			{
			inLength = YES;
			}
		else if (inLength == NO)
			{
			temp[i++] = *s;
			}
		}

	/* attach root to rooted tree */
	if (t->isRooted == YES)
		{
		p = &t->nodes[0];
		q = &t->nodes[j];
		q->left = p;
		p->anc = q;
		}

	j = numLocalTaxa;
	for (i=0; i<t->nNodes; i++)
		{
		p = &t->nodes[i];
		if (p->index == -1)
			p->index = j++;
		if (p->anc == NULL)
			t->root = p;
		}

	GetDownPass (t);

	return NO_ERROR;
}





/* RemovePartition: Remove a partition from the tree keeping track of partition frequencies */
int RemovePartition (PFNODE *r, long *p, int runId)
{
	int		i, comp;
	
	if (r == NULL)
		{
		return (ERROR);
		}
	else
		{
		for (i=0; i<nLongsNeeded; i++)
			{
			if (r->partition[i] != p[i])
				break;
			}
		
		if (i == nLongsNeeded)
			comp = 0;
		else if (r->partition[i] < p[i])
			comp = -1;
		else
			comp = 1;
		
		if (comp == 0)			/* match */
			{
			if (r->count[runId] == 0)
				return ERROR;
			else
				r->count[runId]--;
			}
		else if (comp < 0)		/* greater than -> into left subtree */
			{
			if ((RemovePartition (r->left, p, runId)) == ERROR)
				return ERROR;
			}
		else
			{
			/* less than -> into right subtree */
			if ((RemovePartition (r->right, p, runId)) == ERROR)
				return ERROR;
			}
		}

	return (NO_ERROR);
}





/* RemoveTreeFromPartitionCounters: Break a tree into partitions and remove those from counters */
int RemoveTreeFromPartitionCounters (Tree *tree, int treeId, int runId)
{
	int			i, j;
	TreeNode	*p;
	
	for (i=0; i<tree->nIntNodes-1; i++)
		{
		p = tree->intDownPass[i];
		for (j=0; j<nLongsNeeded; j++)
			{
			partition[p->index][j] = partition[p->left->index][j] | partition[p->right->index][j];
			}
		
		if ((RemovePartition (partFreqTreeRoot[treeId], partition[p->index], runId)) == ERROR)
			{
			MrBayesPrint ("%s   Could not remove partition %d in RemoveTreeFromPartitionCounters\n", spacer, p->index);
			return ERROR;
			}
		}

	return NO_ERROR;
}





/* RemoveTreeSamples: Remove tree samples from partition counters */
int RemoveTreeSamples (int from, int to)
{
	int		i, j, k, longestLine;
	long	lastBlock;
	char	*word, *s, *lineBuf;
	FILE	*fp;
	Tree	*t;
	char	temp[100];

#	if defined (MPI_ENABLED)
	if (proc_id != 0)
		return (NO_ERROR);
#	endif

	for (i=0; i<numTrees; i++)
		{
		if (GetTreeFromIndex(i,0,0)->isRooted == YES)
			t = chainParams.rtree;
		else
			t = chainParams.utree;

		for (j=0; j<chainParams.numRuns; j++)
			{
			if (numTrees == 1)
				sprintf (temp, "%s.run%d.t", chainParams.chainFileName, j+1);
			else
				sprintf (temp, "%s.tree%d.run%d.t", chainParams.chainFileName, i+1, j+1);

			if ((fp = OpenBinaryFileR (temp)) == NULL)
				return (ERROR);

			longestLine = LongestLine (fp);

			fclose (fp);

			if ((fp = OpenTextFileR (temp)) == NULL)
				return (ERROR);
			
			lineBuf = (char *) calloc (longestLine + 1, sizeof (char));
			if (!lineBuf)
				{
				fclose (fp);
				return (ERROR);
				}

			lastBlock = LastBlock (fp, lineBuf, longestLine);

			fseek (fp, lastBlock, SEEK_SET);

			for (k=1; k<=to; k++)
				{
				do {
					if (fgets (lineBuf, longestLine, fp) == NULL)
						return ERROR;
					word = strtok (lineBuf, " ");
					} while (strcmp (word, "tree") != 0);
				
				if (k>=from)
					{
					for (s = strtok (NULL, ";"); *s != '('; s++)
						;
					if (RecreateTree (t, s) == ERROR)
						{
						fclose (fp);
						return (ERROR);
						};
					if (RemoveTreeFromPartitionCounters (t, i, j) == ERROR)
						{
						fclose (fp);
						return (ERROR);
						};
					}
				}

			fclose (fp);
			}
		}

	/* remove unnecessary nodes from the tree holding partition counters */
	for (i=0; i<numTrees; i++)
		{
		partFreqTreeRoot[i] = CompactTree (partFreqTreeRoot[i]);
		}
	return (NO_ERROR);
}





int ReopenMBPrintFiles (void)
{
	int		i, k, n;
	char	fileName[100], localFileName[100];
	
	/* Get root of local file name */
	strcpy (localFileName, chainParams.chainFileName);

	/* Reopen the .p, .t, and .cal files */
	for (n=0; n<chainParams.numRuns; n++)
		{
		k = n;

		if (chainParams.numRuns == 1)
			sprintf (fileName, "%s.p", localFileName);
		else
			sprintf (fileName, "%s.run%d.p", localFileName, n+1);

#		if defined (MPI_ENABLED)
		if (proc_id == 0)
#		endif
		if ((fpParm[k] = OpenTextFileA (fileName)) == NULL)
			return (ERROR);

		for (i=0; i<numTrees; i++)
			{
			if (numTrees == 1 && chainParams.numRuns == 1)
				sprintf (fileName, "%s.t", localFileName);
			else if (numTrees > 1 && chainParams.numRuns == 1)
				sprintf (fileName, "%s.tree%d.t", localFileName, i+1);
			else if (numTrees == 1 && chainParams.numRuns > 1)
				sprintf (fileName, "%s.run%d.t", localFileName, n+1);
			else
				sprintf (fileName, "%s.tree%d.run%d.t", localFileName, i+1, n+1);

#			if defined (MPI_ENABLED)
			if (proc_id == 0)
#			endif
			if ((fpTree[k][i] = OpenTextFileA (fileName)) == NULL)
				return (ERROR);
			}

		for (i=0; i<numCalibratedTrees; i++)
			{
			if (numCalibratedTrees == 1 && chainParams.numRuns == 1)
				sprintf (fileName, "%s.t", localFileName);
			else if (numCalibratedTrees > 1 && chainParams.numRuns == 1)
				sprintf (fileName, "%s.tree%d.t", localFileName, i+1);
			else if (numCalibratedTrees == 1 && chainParams.numRuns > 1)
				sprintf (fileName, "%s.run%d.t", localFileName, n+1);
			else
				sprintf (fileName, "%s.tree%d.run%d.t", localFileName, i+1, n+1);

#			if defined (MPI_ENABLED)
			if (proc_id == 0)
#			endif
			if ((fpCal[k][i] = OpenNewMBPrintFile (fileName)) == NULL)
				return (ERROR);
			}
		}

	/* Take care of the mpi procs that do not have a mcmc file */
#	if defined (MPI_ENABLED)
	if (proc_id != 0)
		return (NO_ERROR);
#	endif

	/* Reopen the .mcmc file */
	if (chainParams.mcmcDiagn == YES)
		{
		sprintf (fileName, "%s.mcmc", chainParams.chainFileName);

		if ((fpMcmc = OpenTextFileA (fileName)) == NULL)
			return (ERROR);
		}

	return (NO_ERROR);
}





/*-------------------------------------------------------------------
|
|	ResetScalers: reset scaler nodes of all trees of all chains
|		This scheme ensures that minimally RESCALE_FREQ and
|		maximally 2 * RESCALE_FREQ - 1 unscaled interior nodes occur
|		before rescaling is done
|
--------------------------------------------------------------------*/
int ResetScalers (void)

{
	int			i, n, chn;
	Tree		*t;
	TreeNode	*p;

	for (chn=0; chn<numLocalChains; chn++)
		{
		for (i=0; i<numTrees; i++)
			{
			t = GetTreeFromIndex (i, chn, state[chn]);
		
			/* set the node depth value of terminal nodes to zero */
			for (n=0; n<t->nNodes; n++)
				{
				p = t->allDownPass[n];
				if (p->left == NULL)
					p->x = 0;
				}

			/* loop over interior nodes */
			for (n=0; n<t->nIntNodes; n++)
				{
				p = t->intDownPass[n];

				p->x = p->left->x + p->right->x + 1;

				if (p->x > RESCALE_FREQ)
					{
					p->scalerNode = YES;
					p->x = 0;
					}
				else
					p->scalerNode = NO;
				}
			}
		}

	return NO_ERROR;
}





int RunChain (long int *seed)

{
	
	int		i, j, k, n, chn, swapA=0, swapB=0, whichMove, acceptMove, lastDiagnostics, stopChain,
			nErrors;
	MrBFlt		r=0.0, lnLikelihoodRatio, lnPriorRatio, lnProposalRatio, lnLike=0.0, lnPrior=0.0, f, CPUTime;
	MCMCMove	*theMove, *mv;
	time_t		startingT, endingT, stoppingT1, stoppingT2;
	clock_t		previousCPUTime, currentCPUTime;

#				if defined (MPI_ENABLED)
	int			ierror, sumErrors, barrierFreq;
	MrBFlt		best;
	MPI_Status 	status;
#				endif
#				if defined (DEBUG_RUNCHAIN)
	ModelInfo	*m;
#				endif

	/* set nErrors to 0 */
	nErrors = 0;
	
#	if defined (MPI_ENABLED)
	barrierFreq = chainParams.printFreq;
#	endif

	if (numLocalTaxa < 4)
		{
		for (i=0; i<numTrees; i++)
			if (GetTreeFromIndex(i, 0, 0)->isRooted == NO)
				break;
		if (i < numTrees && numLocalTaxa < 4)
			{
			MrBayesPrint ("%s   There must be at least four taxa in the analysis\n", spacer);
			return (ERROR);
			}
		else if (i == numTrees && numLocalTaxa < 3)
			{
			MrBayesPrint ("%s   There must be at least three taxa in the analysis\n", spacer);
			return (ERROR);
			}
		}

	/* Adjust default comparetree file name; we know now how many trees we have */
	if (numTrees > 1 && chainParams.numRuns > 1)
		sprintf (comptreeParams.comptFileName1, "%s.tree1.run1.t", chainParams.chainFileName);
	else if (numTrees > 1 && chainParams.numRuns == 1)
		sprintf (comptreeParams.comptFileName1, "%s.tree1.t", chainParams.chainFileName);
	else if (numTrees == 1 && chainParams.numRuns > 1)
		sprintf (comptreeParams.comptFileName1, "%s.run1.t", chainParams.chainFileName);
	else if (numTrees == 1 && chainParams.numRuns == 1)
		sprintf (comptreeParams.comptFileName1, "%s.t", chainParams.chainFileName);
	strcpy (comptreeParams.comptFileName2, comptreeParams.comptFileName1);

	/* allocate some memory for the chains */
	if (memAllocs[ALLOC_CURLNL] == YES)
		{
		MrBayesPrint ("%s   curLnL is already allocated\n", spacer);
		nErrors++;
		}
	else if ((curLnL = (MrBFlt *)malloc((size_t) (numLocalChains * sizeof(MrBFlt)))) == NULL)
		{
		MrBayesPrint ("%s   Problem allocating curLnL (%d)\n", spacer, numLocalChains * sizeof(MrBFlt));
		nErrors++;
		}
	else if ((maxLnL0 = (MrBFlt *) calloc (chainParams.numRuns * chainParams.numChains, sizeof(MrBFlt))) == NULL)
		{
		MrBayesPrint ("%s   Problem allocating maxLnL0\n", spacer, numLocalChains * sizeof(MrBFlt));
		free (curLnL);
		nErrors++;
		}
	else
		memAllocs[ALLOC_CURLNL] = YES;
#	if defined (MPI_ENABLED)
	MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
	if (sumErrors > 0)
		{
		MrBayesPrint ("%s   Memory allocation error on at least one processor\n", spacer);
		return ERROR;
		}
#	else
	if (nErrors > 0)
		return ERROR;
#	endif

	if (memAllocs[ALLOC_CURLNPR] == YES)
		{
		MrBayesPrint ("%s   curLnPr is already allocated\n", spacer);
		nErrors++;
		}
	else if ((curLnPr = (MrBFlt *)malloc((size_t) (numLocalChains * sizeof(MrBFlt)))) == NULL)
		{
		MrBayesPrint ("%s   Problem allocating curLnPr (%d)\n", spacer, numLocalChains * sizeof(MrBFlt));
		nErrors++;
		}
	else
		memAllocs[ALLOC_CURLNPR] = YES;
#	if defined (MPI_ENABLED)
	MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
	if (sumErrors > 0)
		{
		MrBayesPrint ("%s   Memory allocation error on at least one processor\n", spacer);
		return ERROR;
		}
#	else
	if (nErrors > 0)
		return ERROR;
#	endif

	if (memAllocs[ALLOC_CHAINID] == YES)
		{
		MrBayesPrint ("%s   chainId is already allocated\n", spacer);
		nErrors++;
		}
	else if ((chainId = (int *)malloc((size_t) (numLocalChains * sizeof(int)))) == NULL)
		{
		MrBayesPrint ("%s   Problem allocating chainId (%d)\n", spacer, numLocalChains * sizeof(int));
		nErrors++;
		}
	else
		memAllocs[ALLOC_CHAINID] = YES;
#	if defined (MPI_ENABLED)
	MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
	if (sumErrors > 0)
		{
		MrBayesPrint ("%s   Memory allocation error on at least one processor\n", spacer);
		return ERROR;
		}
#	else
	if (nErrors > 0)
		return ERROR;
#	endif

	if (memAllocs[ALLOC_SWAPINFO] == YES)
		{
		MrBayesPrint ("%s   swapInfo is already allocated\n", spacer);
		nErrors++;
		}
	else if ((swapInfo = (int ***) calloc (chainParams.numRuns, sizeof (int **))) == NULL)
		{
		MrBayesPrint ("%s   Problem allocating swapInfo\n", spacer);
		nErrors++;
		}
	else
		{
		for (n=0; n<chainParams.numRuns; n++)
			{
			swapInfo[n] = AllocateSquareIntegerMatrix (chainParams.numChains);
			if (!swapInfo[n])
				{
				MrBayesPrint ("%s   Problem allocating swapInfo[%d]\n", spacer, n);
				for (i=0; i<n; i++)
					free (swapInfo[i]);
				free (swapInfo);
				nErrors++;
				break;
				}
			}
		memAllocs[ALLOC_SWAPINFO] = YES;
		}
#	if defined (MPI_ENABLED)
	MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
	if (sumErrors > 0)
		{
		MrBayesPrint ("%s   Memory allocation error on at least one processor\n", spacer);
		return ERROR;
		}
#	else
	if (nErrors > 0)
		return ERROR;
#	endif

	for (n=0; n<chainParams.numRuns; n++)
		for (i=0; i<chainParams.numChains; i++)
			for (j=0; j<chainParams.numChains; j++)
				swapInfo[n][i][j] = 0;

	/* set up counters for topological convergence diagnostics */
	/* allocate tree used for some topological convergence diagnostics */
	if (chainParams.mcmcDiagn == YES && chainParams.numRuns > 1)
		{
		if (SetUpPartitionCounters () == ERROR)
			nErrors++;
#		if defined (MPI_ENABLED)
		if (proc_id == 0)
			{
			if ((chainParams.stat = (STATS *) calloc (numTrees, sizeof (STATS))) == NULL)
				nErrors++;
			else
				{
				memAllocs[ALLOC_STATS] = YES;
				for (i=0; i<numTrees; i++)
					chainParams.stat[i].pair = NULL;
				}

			for (i=j=k=0; i<numTrees; i++)
				{
				if (GetTreeFromIndex(i,0,0)->isRooted == NO)
					j = 1;
				else
					k = 1;
				}
			if (j == 1)
				{
				if ((chainParams.utree = AllocateTree (numLocalTaxa, NO)) == NULL)
					{
					nErrors++;
					}
				else
					memAllocs[ALLOC_DIAGNUTREE] = YES;
				}
			if (k == 1)
				{
				if ((chainParams.rtree = AllocateTree (numLocalTaxa, YES)) == NULL)
					{
					nErrors++;
					}
				else
					memAllocs[ALLOC_DIAGNRTREE] = YES;
				}
			if (chainParams.allComps == YES)
				{
				for (i=0; i<numTrees; i++)
					{
					if ((chainParams.stat[i].pair = AllocateSquareDoubleMatrix (chainParams.numRuns)) == NULL)
						{
						nErrors++;
						break;
						}
					}
				}
			}
		MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
		if (sumErrors > 0)
			{
			MrBayesPrint ("%s   Memory allocation error on at least one processor\n", spacer);
			return ERROR;
			}
#		else
		if ((chainParams.stat = (STATS *) calloc (numTrees, sizeof (STATS))) == NULL)
			return ERROR;
		else
			{
			memAllocs[ALLOC_STATS] = YES;
			for (i=0; i<numTrees; i++)
				chainParams.stat[i].pair = NULL;
			}
		if (chainParams.relativeBurnin == YES)
			{
			for (i=j=k=0; i<numTrees; i++)
				{
				if (GetTreeFromIndex(i,0,0)->isRooted == NO)
					j = 1;
				else
					k = 1;
				}
			if (j == 1)
				{
				if ((chainParams.utree = AllocateTree (numLocalTaxa, NO)) == NULL)
					{
					MrBayesPrint ("%s   Could not allocate chainParams.utree in RunChain\n", spacer);
					return ERROR;
					}
				else
					memAllocs[ALLOC_DIAGNUTREE] = YES;
		  
				}
			if (k == 1)
				{
				if ((chainParams.rtree = AllocateTree (numLocalTaxa, YES)) == NULL)
					{
					MrBayesPrint ("%s   Could not allocate chainParams.rtree in RunChain\n", spacer);
					return ERROR;
					}
				else
					memAllocs[ALLOC_DIAGNRTREE] = YES;
				}
			}
		if (chainParams.allComps == YES)
			{
			for (i=0; i<numTrees; i++)
				{
				if ((chainParams.stat[i].pair = AllocateSquareDoubleMatrix (chainParams.numRuns)) == NULL)
					{
					MrBayesPrint ("%s   Could not allocate chainParams.stat.pair in RunChain\n", spacer);
					return ERROR;
					}
				}
			}
#		endif
		}

	/* get chain IDs */
	GetChainIds ();
	
	/* initialize likelihoods and prior                  */
	/* touch everything and calculate initial cond likes */
	ResetScalers ();
	TouchAllPartitions ();
	for (chn=0; chn<numLocalChains; chn++)
		{
		if (chn % chainParams.numChains == 0)
			{
			if (chainParams.numRuns == 1)
				MrBayesPrint ("%s   Initial log likelihoods:\n", spacer);
			else
				MrBayesPrint ("%s   Initial log likelihoods for run %d:\n", spacer, chn / chainParams.numChains + 1);
			}
		TouchAllTrees (chn);
		curLnL[chn] = LogLike(chn);
		MrBayesPrint ("%s      Chain %d -- %.6lf\n", spacer, (chn % chainParams.numChains) + 1, curLnL[chn]);
		curLnPr[chn] = LogPrior(chn);
		}

	if (PreparePrintFiles() == ERROR)
		nErrors++;
#	if defined (MPI_ENABLED)
	MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
	if (sumErrors > 0)
		{
		MrBayesPrint ("%s   Memory allocation error on at least one processor\n", spacer);
		return ERROR;
		}
#	else
	if (nErrors > 1)
		return ERROR;
#	endif

	if (chainParams.relativeBurnin == NO)
		lastDiagnostics = chainParams.chainBurnIn;
	else
		lastDiagnostics = 0;
	stopChain = NO;

	for (i=0; i<chainParams.numRuns; i++)
		maxLnL0[i] = -100000000.0;

	startingT=time(0);
	CPUTime = 0.0;
	previousCPUTime = clock();

	for (n=1; n<=chainParams.numGen; n++) /* begin run chain */
		{

		currentCPUTime = clock();
		if (currentCPUTime - previousCPUTime > 10 * CLOCKS_PER_SEC)
			{
			CPUTime += (currentCPUTime - previousCPUTime) / (MrBFlt) (CLOCKS_PER_SEC);
			previousCPUTime = currentCPUTime;
			}

		/*! AbortRun is set by the signal handler when it receives a CTRL-C */
#if 		defined  (MPI_ENABLED)
		if (confirmAbortRun == YES && requestAbortRun()==1)
			nErrors++;
		if (n % barrierFreq == 1)
			{
			MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
			if (sumErrors > 0)
				{
				if (confirmAbortRun == NO)
					{
					MrBayesPrint ("%s   RunChain error on at least one processor\n", spacer);
						return ERROR;
					}
				else
					{
					return ABORT;
					}
				}
			}
#else
		if (confirmAbortRun == YES && requestAbortRun()==1)
			return ABORT;
#endif
		/* Refresh scalers every SCALER_REFRESH_FREQ generations.                */
		/* It is done before copying so we know it will take effect immediately. */
		/* However, the actual scalers are recalculated only when really needed. */
		if (n % SCALER_REFRESH_FREQ == 0)
			ResetScalers();

		for (chn=0; chn<numLocalChains; chn++)
			{
			/* The global variable state[chain] gives state.                     */
			/* First copy everything from current state of chain to new state.   */

			/* copy all touched trees and reset update flags                     */
			/* this will also take care of node cond likes, node & site scalers  */
			/* cond likes and transition probability matrices                    */
			CopyTrees (chn);

			/* copy all model parameters */
			CopyParams (chn);

			/* shift the state of the chain to the new state */
			/* all calculations will be done on this state   */
			state[chn] ^= 1;  /* XORing with 1 switches between 0 and 1 */

			/* decide which move to make */
			whichMove = PickProposal(seed);
			theMove = &moves[whichMove];
			
			/* set prior and proposal ratios */
			lnProposalRatio = 0.0;
			lnPriorRatio = 0.0;

			/* reset abort move flag */
			abortMove = NO;
			
			/* Touch the relevant partitions       */
			/* as a service to the move functions. */
			for (i=0; i<theMove->parm->nRelParts; i++)
				modelSettings[theMove->parm->relParts[i]].upDateCl = YES;

			/*TouchAllPartitions();*/    /* for debugging copying shortcuts */

			/* make move */
			if ((theMove->moveFxn)(theMove->parm, chn, seed, &lnPriorRatio, &lnProposalRatio, theMove->proposalParam) == ERROR)
				{
				MrBayesPrint ("%s   Error in move %s\n", spacer, theMove->name);
#				if defined (MPI_ENABLED)
				nErrors++;
#				else
				return ERROR;
#				endif
				}

			/* calculate likelihood ratio */
			if (abortMove == NO)
				{
				  /*TouchAllTrees(chn);*/  /* for debugging copying shortcuts */
				lnLike = LogLike(chn);
				lnLikelihoodRatio = lnLike - curLnL[chn];
				lnPrior = curLnPr[chn] + lnPriorRatio;

				/* heat */
				lnLikelihoodRatio *= Temperature (chainId[chn]);
				lnPriorRatio      *= Temperature (chainId[chn]);
			
				/* calculate the acceptance probability */
				if (lnLikelihoodRatio + lnPriorRatio + lnProposalRatio < -100.0)
					r = 0.0;
				else if (lnLikelihoodRatio + lnPriorRatio + lnProposalRatio > 0.0)
					r = 1.0;
				else
					r = exp(lnLikelihoodRatio + lnPriorRatio + lnProposalRatio);
				}

			/* decide to accept or reject the move */
			acceptMove = NO;
			i = chainId[chn];
			theMove->nTried[i]++;
			if (abortMove == NO && RandomNumber(seed) < r)
				{
				acceptMove = YES;
				theMove->nAccepted[i]++;
				}

			/* update the chain */
			if (acceptMove == NO)
				{
				/* the new state did not work out so shift chain back */
				state[chn] ^= 1;
				}
			else 
				{
				/* if the move is accepted then let the chain stay in the new state */
				/* store the likelihood/prior of the chain */
				curLnL[chn] = lnLike;
				curLnPr[chn] = lnPrior;
				}

			/*ShowValuesForChain (chn); */

			if (curLnL[chn] > maxLnL0[i])
				maxLnL0[i] = curLnL[chn];

			}

		/* attempt swap(s) */
		if (chainParams.numChains > 1 && n % chainParams.swapFreq == 0)
			{
			for (i = 0; i<chainParams.numRuns; i++)
				{
				for (j = 0; j<chainParams.numSwaps; j++)
				GetSwappers (&swapA, &swapB, i);
				if (AttemptSwap (swapA, swapB, seed) == ERROR)
					{
#					if defined (MPI_ENABLED)
					nErrors++;
#					else
					MrBayesPrint ("%s   Unsuccessful swap of states\n", spacer);
					return ERROR;
#					endif
					}
				}
			}

		/* print information to screen */
		if (n == 1 || n % chainParams.printFreq == 0)
			PrintToScreen(n, time(0), startingT);

		/* print information to files */
		if (n == 1 || n == chainParams.numGen || n % chainParams.sampleFreq == 0)
			{
			if (PrintStatesToFiles (n) == ERROR)
				{
#				if defined (MPI_ENABLED)
				nErrors++;
#				else
				return ERROR;
#				endif
				}
			}

		/* print mcmc diagnostics */
		if (chainParams.mcmcDiagn == YES && (n % chainParams.diagnFreq == 0 || n == 1 || n == chainParams.numGen))
			{
			if (chainParams.numRuns > 1 && ((n > 1 && chainParams.relativeBurnin == YES)
				|| (n >= chainParams.chainBurnIn * chainParams.sampleFreq && chainParams.relativeBurnin == NO)))
				{
				/* we need some space for coming output */
				MrBayesPrint ("\n");
				/* first reassemble files if mpi version */				
#				if defined (MPI_ENABLED)
				/* the following function returns immediately in MPI if proc_id != 0 */
				if (AddTreeSamples (lastDiagnostics + 1, n/chainParams.sampleFreq+1) == ERROR)
					nErrors++;
				MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
				if (sumErrors > 0)
					{
					MrBayesPrint ("%s   AddTreeSamples error on at least one processor\n", spacer);
						return ERROR;
					}
#				endif
				/* remove tree samples if using burninpercentage */
				/* the following function returns immediately in MPI if proc_id != 0 */
				if (chainParams.relativeBurnin == YES)
					{
					if (RemoveTreeSamples ((int)(chainParams.burninFraction * lastDiagnostics) + 1, (int) (chainParams.burninFraction * ((n/chainParams.sampleFreq)+1))) == ERROR)
						{
						nErrors++;
						MrBayesPrint ("%s   Problem removing tree samples\n", spacer);
						}
#					if defined (MPI_ENABLED)
					MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
					if (sumErrors > 0)
						return ERROR;
#					else
					if (nErrors > 0)
							return ERROR;
#					endif
					}

				lastDiagnostics = (n/chainParams.sampleFreq)+1;
				if (chainParams.relativeBurnin == YES)
					i = lastDiagnostics - (int) (lastDiagnostics * chainParams.burninFraction);
				else
					i = lastDiagnostics - chainParams.chainBurnIn;
#				if defined (MPI_ENABLED)
				if (proc_id == 0)
					{
#				endif
				/* calculate statistics */
				CalculateTopConvDiagn (i);
				/* output statistics */
				if (numTrees == 1)
					{
					f = chainParams.stat[0].avgStdDev;
					MrBayesPrint ("%s   Average standard deviation of split frequencies: %.6f\n", spacer, f);
					if (f <= chainParams.stopVal)
						stopChain = YES;
					if (n < chainParams.numGen - chainParams.printFreq && (chainParams.stopRule == NO || stopChain == NO))
						MrBayesPrint ("\n");
					}
				else
					{
					stopChain = YES;
					for (i=0; i<numTrees; i++)
						{
						f = chainParams.stat[i].avgStdDev;
						MrBayesPrint ("%s   Average standard deviation of split frequencies for tree %d: %.6f\n", spacer, i+1, f);
						if (f > chainParams.stopVal)
							stopChain = NO;
						}
					if (n < chainParams.numGen - chainParams.printFreq && (chainParams.stopRule == NO || stopChain == NO))
						MrBayesPrint ("\n");
					}
				if (chainParams.allComps == YES)
					PrintTopConvInfo ();
#				if defined (MPI_ENABLED)
					}
				ierror = MPI_Bcast (&stopChain, 1, MPI_INT, 0, MPI_COMM_WORLD);
				if (ierror != MPI_SUCCESS)
					{
					MrBayesPrint ("%s   Problem broadcasting stop value\n", spacer);
					nErrors++;
					}
#				endif
				}
			/* part of the following function needs to be performed by all MPI processors */
			if (PrintMCMCDiagnosticsToFile (n) == ERROR)
				nErrors++;
#			if defined (MPI_ENABLED)
			MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
			if (sumErrors > 0)
				{
				MrBayesPrint ("%s   RunChain error on at least one processor\n", spacer);
				return ERROR;
				}
#			else
			if (nErrors > 0)
				return (ERROR);
#			endif
			}

		/* check if time to break because stopVal reached */
		if (chainParams.stopRule == YES && stopChain == YES)
			{
			MrBayesPrint ("\n%s   Analysis stopped because convergence diagnostic hit stop value.\n", spacer);
			break;
			}
			
		/* user may want to extend chain */
		if (n == chainParams.numGen && autoClose == NO)
			{
			  stoppingT1 = time(0); 
			  currentCPUTime = clock();
			  CPUTime += (currentCPUTime - previousCPUTime) / (MrBFlt) CLOCKS_PER_SEC;
			  previousCPUTime = currentCPUTime;
			  chainParams.numGen += ExtendChainQuery ();
			  stoppingT2 = time(0);
			  startingT += (stoppingT2-stoppingT1);
			  previousCPUTime = clock();
			  /* timers should not be increased during the wait for a reply */
			}

#		if defined (DEBUG_RUNCHAIN)
		/* debugging code */
		if (n % chainParams.printFreq == 0)
			{
			for (chn=0; chn<numLocalChains; chn++)
				{
				m = &modelSettings[0];
				printf ("%f  --   ", *GetParamVals(m->shape, chn, state[chn]));
				}
				printf("\n");
			}
#		endif

		} /* end run chain */
	endingT = time(0);
	currentCPUTime = clock();
	CPUTime += (currentCPUTime - previousCPUTime) / (MrBFlt) CLOCKS_PER_SEC;

	CloseMBPrintFiles (); /* redundant because files closed in FreeChainMemory but kept here as a safeguard in case of future changes */

	MrBayesPrint ("\n");
	if (difftime (endingT, startingT) > 2.0)
		MrBayesPrint ("%s   Analysis completed in %.0f seconds\n", spacer, 
			  difftime(endingT, startingT));
	else if (difftime (endingT, startingT) >= 1.0)
		MrBayesPrint ("%s   Analysis completed in 1 second\n", spacer);
	else
		MrBayesPrint ("%s   Analysis completed in less than 1 second\n", spacer);

#if defined (MPI_ENABLED)
	MrBayesPrint ("%s   Analysis used %1.2f seconds of CPU time on processor 0\n", spacer, (MrBFlt) CPUTime);
#else
	MrBayesPrint ("%s   Analysis used %1.2f seconds of CPU time\n", spacer, (MrBFlt) CPUTime);
#endif

#	if defined (MPI_ENABLED)
	/* find the best likelihoods across all of the processors */
	ierror = MPI_Barrier (MPI_COMM_WORLD);
	if (ierror != MPI_SUCCESS)
		{
		MrBayesPrint ("%s   Problem at chain barrier\n", spacer);
		return (ERROR);
		}
	for (j=0; j<chainParams.numRuns * chainParams.numChains; j++)
		{
		best = maxLnL0[j];
		for (i=1; i<num_procs; i++)
			{
			if (proc_id == 0)
				{
				ierror = MPI_Recv (&maxLnL0[j], 1, MPI_DOUBLE, i, i, MPI_COMM_WORLD, &status);
				if (ierror != MPI_SUCCESS)
					{ 
					MrBayesPrint ("%s   Problem with MPI_Recv", spacer);
					return ERROR;
					}
				if (maxLnL0[j] > best)
					best = maxLnL0[j];
				}
			else if (proc_id == i)
				{
				ierror = MPI_Send (&maxLnL0[j], 1, MPI_DOUBLE, 0, i, MPI_COMM_WORLD);
				if (ierror != MPI_SUCCESS)
					{
					MrBayesPrint ("%s   Problem with MPI_Send\n", spacer);
					return ERROR;
					}
				}
			}
		maxLnL0[j] = best;
		}
#	endif

	if (chainParams.numRuns == 1)
		{
		if (chainParams.numChains == 1)
			MrBayesPrint ("%s   Likelihood of best state was %1.2lf\n", spacer, maxLnL0[0]);
		else
			MrBayesPrint ("%s   Likelihood of best state for \"cold\" chain was %1.2lf\n", spacer, maxLnL0[0]);
		}
	else
		{
		for (j=0; j<chainParams.numRuns*chainParams.numChains; j++)
			{
			if (j % chainParams.numChains == 0)
				{
				if (chainParams.numChains == 1)
					MrBayesPrint ("%s   Likelihood of best state for run %d was %1.2lf\n", spacer, j/chainParams.numChains+1, maxLnL0[j/chainParams.numChains]);
				else
					MrBayesPrint ("%s   Likelihood of best state for \"cold\" chain of run %d was %1.2lf\n", spacer, j/chainParams.numChains+1, maxLnL0[j/chainParams.numChains]);
				}
			}
		}

#	if defined (MPI_ENABLED)
	/* we need to collect the information on the number of accepted moves if
	   this is a parallel version */
	if (ReassembleMoveInfo() == ERROR)
		nErrors++;
	MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
	if (sumErrors > 0)
		{
		MrBayesPrint ("%s   ReassembleMoveInfo failed\n", spacer);
		return ERROR;
		}
#	endif

	/* print acceptance rates for the moves */
	for (j=0; j<chainParams.numChains*chainParams.numRuns; j++)
		{
		if (chainParams.numChains == 1)
			{
			if (chainParams.numRuns == 1)
				MrBayesPrint ("%s   Acceptance rates for the moves:\n", spacer);
			else
				MrBayesPrint ("%s   Acceptance rates for the moves in run %d:\n", spacer, j/chainParams.numChains+1);
			}
		else if (j % chainParams.numChains == 0)
			{
			if (chainParams.numRuns == 1)
				MrBayesPrint ("%s   Acceptance rates for the moves in the \"cold\" chain:\n", spacer);
			else
				MrBayesPrint ("%s   Acceptance rates for the moves in the \"cold\" chain of run %d:\n", spacer, j/chainParams.numChains+1);
			}
		else if (chainParams.allChains == YES)
			{
			if (chainParams.numRuns == 1)
				MrBayesPrint ("%s   Acceptance rates for the moves in chain %d (heated):\n", spacer, j+1);
			else
				MrBayesPrint ("%s   Acceptance rates for the moves in chain %d of run %d (heated):\n", spacer, j%chainParams.numChains+1, j/chainParams.numChains+1);
			}

		if (j % chainParams.numChains == 0 || chainParams.allChains == YES)
			{
			MrBayesPrint ("%s      With prob.  Chain accepted changes to\n", spacer);

			for (i=0; i<numMoves; i++)
				{
				mv = &moves[i];
				if (mv->nTried[0] == 0)
					MrBayesPrint ("%s      not tried   param. %d %s\n", spacer, mv->parm->index+1, mv->name);
				else
					MrBayesPrint ("%s       %6.2f %%   param. %d %s\n", spacer, 100.0*(MrBFlt)(mv->nAccepted[j])/(MrBFlt)(mv->nTried[j]), mv->parm->index+1, mv->name);
				}
			}
		}

	/* output information on the success of the chain state swap proposals */
	if (PrintSwapInfo () == ERROR)
		nErrors++;
#	if defined (MPI_ENABLED)
	MPI_Allreduce (&nErrors, &sumErrors, 1, MPI_INT, MPI_SUM, MPI_COMM_WORLD);
	if (sumErrors > 0)
		{
		MrBayesPrint ("%s   PrintSwapInfo failed\n", spacer);
		return ERROR;
		}
#	else
	if (nErrors > 1)
		{
		MrBayesPrint ("%s   PrintSwapInfo failed\n", spacer);
		return ERROR;
		}
#	endif

#	if defined (MPI_ENABLED)
	if (proc_id != 0)
		return (NO_ERROR);
#	endif

	if (chainParams.numRuns > 1 && chainParams.mcmcDiagn == YES)
		{
		f = 0.0;
		for (i=0; i<numTrees; i++)
			{
			if (chainParams.stat[i].avgStdDev > f)
				f = chainParams.stat[i].avgStdDev;
			}
		if (f > 0.10)
			{
			MrBayesPrint ("\n");
			MrBayesPrint ("%s   ************************* WARNING!! ************************************  \n", spacer);
			MrBayesPrint ("%s   MrBayes suspects that your runs have not converged because the tree       \n", spacer);
			MrBayesPrint ("%s   samples are very different (average standard deviation of split frequen-  \n", spacer);
			MrBayesPrint ("%s   cies larger than 0.10 (%1.2lf)). MrBayes suggests that you run the ana-   \n", spacer, f);
			MrBayesPrint ("%s   lysis longer or try to improve the MCMC sampling efficiency by fine-      \n", spacer);
			MrBayesPrint ("%s   tuning MCMC proposal or heating parameters.                               \n", spacer);
			}
		}

	return (NO_ERROR);
}





int SetAARates (void)

{

	int			i, j;
	MrBFlt		diff, sum, scaler;
	
	/* A R N D C Q E G H I L K M F P S T W Y V */

	/* jones */
	aaJones[ 0][ 0] =   0; aaJones[ 0][ 1] =  58; aaJones[ 0][ 2] =  54; aaJones[ 0][ 3] =  81; aaJones[ 0][ 4] =  56; 
	aaJones[ 0][ 5] =  57; aaJones[ 0][ 6] = 105; aaJones[ 0][ 7] = 179; aaJones[ 0][ 8] =  27; aaJones[ 0][ 9] =  36; 
	aaJones[ 0][10] =  30; aaJones[ 0][11] =  35; aaJones[ 0][12] =  54; aaJones[ 0][13] =  15; aaJones[ 0][14] = 194; 
	aaJones[ 0][15] = 378; aaJones[ 0][16] = 475; aaJones[ 0][17] =   9; aaJones[ 0][18] =  11; aaJones[ 0][19] = 298; 
	aaJones[ 1][ 0] =  58; aaJones[ 1][ 1] =   0; aaJones[ 1][ 2] =  45; aaJones[ 1][ 3] =  16; aaJones[ 1][ 4] = 113; 
	aaJones[ 1][ 5] = 310; aaJones[ 1][ 6] =  29; aaJones[ 1][ 7] = 137; aaJones[ 1][ 8] = 328; aaJones[ 1][ 9] =  22; 
	aaJones[ 1][10] =  38; aaJones[ 1][11] = 646; aaJones[ 1][12] =  44; aaJones[ 1][13] =   5; aaJones[ 1][14] =  74; 
	aaJones[ 1][15] = 101; aaJones[ 1][16] =  64; aaJones[ 1][17] = 126; aaJones[ 1][18] =  20; aaJones[ 1][19] =  17; 
	aaJones[ 2][ 0] =  54; aaJones[ 2][ 1] =  45; aaJones[ 2][ 2] =   0; aaJones[ 2][ 3] = 528; aaJones[ 2][ 4] =  34; 
	aaJones[ 2][ 5] =  86; aaJones[ 2][ 6] =  58; aaJones[ 2][ 7] =  81; aaJones[ 2][ 8] = 391; aaJones[ 2][ 9] =  47; 
	aaJones[ 2][10] =  12; aaJones[ 2][11] = 263; aaJones[ 2][12] =  30; aaJones[ 2][13] =  10; aaJones[ 2][14] =  15; 
	aaJones[ 2][15] = 503; aaJones[ 2][16] = 232; aaJones[ 2][17] =   8; aaJones[ 2][18] =  70; aaJones[ 2][19] =  16; 
	aaJones[ 3][ 0] =  81; aaJones[ 3][ 1] =  16; aaJones[ 3][ 2] = 528; aaJones[ 3][ 3] =   0; aaJones[ 3][ 4] =  10; 
	aaJones[ 3][ 5] =  49; aaJones[ 3][ 6] = 767; aaJones[ 3][ 7] = 130; aaJones[ 3][ 8] = 112; aaJones[ 3][ 9] =  11; 
	aaJones[ 3][10] =   7; aaJones[ 3][11] =  26; aaJones[ 3][12] =  15; aaJones[ 3][13] =   4; aaJones[ 3][14] =  15; 
	aaJones[ 3][15] =  59; aaJones[ 3][16] =  38; aaJones[ 3][17] =   4; aaJones[ 3][18] =  46; aaJones[ 3][19] =  31; 
	aaJones[ 4][ 0] =  56; aaJones[ 4][ 1] = 113; aaJones[ 4][ 2] =  34; aaJones[ 4][ 3] =  10; aaJones[ 4][ 4] =   0; 
	aaJones[ 4][ 5] =   9; aaJones[ 4][ 6] =   5; aaJones[ 4][ 7] =  59; aaJones[ 4][ 8] =  69; aaJones[ 4][ 9] =  17; 
	aaJones[ 4][10] =  23; aaJones[ 4][11] =   7; aaJones[ 4][12] =  31; aaJones[ 4][13] =  78; aaJones[ 4][14] =  14; 
	aaJones[ 4][15] = 223; aaJones[ 4][16] =  42; aaJones[ 4][17] = 115; aaJones[ 4][18] = 209; aaJones[ 4][19] =  62; 
	aaJones[ 5][ 0] =  57; aaJones[ 5][ 1] = 310; aaJones[ 5][ 2] =  86; aaJones[ 5][ 3] =  49; aaJones[ 5][ 4] =   9; 
	aaJones[ 5][ 5] =   0; aaJones[ 5][ 6] = 323; aaJones[ 5][ 7] =  26; aaJones[ 5][ 8] = 597; aaJones[ 5][ 9] =   9; 
	aaJones[ 5][10] =  72; aaJones[ 5][11] = 292; aaJones[ 5][12] =  43; aaJones[ 5][13] =   4; aaJones[ 5][14] = 164; 
	aaJones[ 5][15] =  53; aaJones[ 5][16] =  51; aaJones[ 5][17] =  18; aaJones[ 5][18] =  24; aaJones[ 5][19] =  20; 
	aaJones[ 6][ 0] = 105; aaJones[ 6][ 1] =  29; aaJones[ 6][ 2] =  58; aaJones[ 6][ 3] = 767; aaJones[ 6][ 4] =   5; 
	aaJones[ 6][ 5] = 323; aaJones[ 6][ 6] =   0; aaJones[ 6][ 7] = 119; aaJones[ 6][ 8] =  26; aaJones[ 6][ 9] =  12; 
	aaJones[ 6][10] =   9; aaJones[ 6][11] = 181; aaJones[ 6][12] =  18; aaJones[ 6][13] =   5; aaJones[ 6][14] =  18; 
	aaJones[ 6][15] =  30; aaJones[ 6][16] =  32; aaJones[ 6][17] =  10; aaJones[ 6][18] =   7; aaJones[ 6][19] =  45; 
	aaJones[ 7][ 0] = 179; aaJones[ 7][ 1] = 137; aaJones[ 7][ 2] =  81; aaJones[ 7][ 3] = 130; aaJones[ 7][ 4] =  59; 
	aaJones[ 7][ 5] =  26; aaJones[ 7][ 6] = 119; aaJones[ 7][ 7] =   0; aaJones[ 7][ 8] =  23; aaJones[ 7][ 9] =   6; 
	aaJones[ 7][10] =   6; aaJones[ 7][11] =  27; aaJones[ 7][12] =  14; aaJones[ 7][13] =   5; aaJones[ 7][14] =  24; 
	aaJones[ 7][15] = 201; aaJones[ 7][16] =  33; aaJones[ 7][17] =  55; aaJones[ 7][18] =   8; aaJones[ 7][19] =  47; 
	aaJones[ 8][ 0] =  27; aaJones[ 8][ 1] = 328; aaJones[ 8][ 2] = 391; aaJones[ 8][ 3] = 112; aaJones[ 8][ 4] =  69; 
	aaJones[ 8][ 5] = 597; aaJones[ 8][ 6] =  26; aaJones[ 8][ 7] =  23; aaJones[ 8][ 8] =   0; aaJones[ 8][ 9] =  16; 
	aaJones[ 8][10] =  56; aaJones[ 8][11] =  45; aaJones[ 8][12] =  33; aaJones[ 8][13] =  40; aaJones[ 8][14] = 115; 
	aaJones[ 8][15] =  73; aaJones[ 8][16] =  46; aaJones[ 8][17] =   8; aaJones[ 8][18] = 573; aaJones[ 8][19] =  11; 
	aaJones[ 9][ 0] =  36; aaJones[ 9][ 1] =  22; aaJones[ 9][ 2] =  47; aaJones[ 9][ 3] =  11; aaJones[ 9][ 4] =  17; 
	aaJones[ 9][ 5] =   9; aaJones[ 9][ 6] =  12; aaJones[ 9][ 7] =   6; aaJones[ 9][ 8] =  16; aaJones[ 9][ 9] =   0; 
	aaJones[ 9][10] = 229; aaJones[ 9][11] =  21; aaJones[ 9][12] = 479; aaJones[ 9][13] =  89; aaJones[ 9][14] =  10; 
	aaJones[ 9][15] =  40; aaJones[ 9][16] = 245; aaJones[ 9][17] =   9; aaJones[ 9][18] =  32; aaJones[ 9][19] = 961; 
	aaJones[10][ 0] =  30; aaJones[10][ 1] =  38; aaJones[10][ 2] =  12; aaJones[10][ 3] =   7; aaJones[10][ 4] =  23; 
	aaJones[10][ 5] =  72; aaJones[10][ 6] =   9; aaJones[10][ 7] =   6; aaJones[10][ 8] =  56; aaJones[10][ 9] = 229; 
	aaJones[10][10] =   0; aaJones[10][11] =  14; aaJones[10][12] = 388; aaJones[10][13] = 248; aaJones[10][14] = 102; 
	aaJones[10][15] =  59; aaJones[10][16] =  25; aaJones[10][17] =  52; aaJones[10][18] =  24; aaJones[10][19] = 180; 
	aaJones[11][ 0] =  35; aaJones[11][ 1] = 646; aaJones[11][ 2] = 263; aaJones[11][ 3] =  26; aaJones[11][ 4] =   7; 
	aaJones[11][ 5] = 292; aaJones[11][ 6] = 181; aaJones[11][ 7] =  27; aaJones[11][ 8] =  45; aaJones[11][ 9] =  21; 
	aaJones[11][10] =  14; aaJones[11][11] =   0; aaJones[11][12] =  65; aaJones[11][13] =   4; aaJones[11][14] =  21; 
	aaJones[11][15] =  47; aaJones[11][16] = 103; aaJones[11][17] =  10; aaJones[11][18] =   8; aaJones[11][19] =  14; 
	aaJones[12][ 0] =  54; aaJones[12][ 1] =  44; aaJones[12][ 2] =  30; aaJones[12][ 3] =  15; aaJones[12][ 4] =  31; 
	aaJones[12][ 5] =  43; aaJones[12][ 6] =  18; aaJones[12][ 7] =  14; aaJones[12][ 8] =  33; aaJones[12][ 9] = 479; 
	aaJones[12][10] = 388; aaJones[12][11] =  65; aaJones[12][12] =   0; aaJones[12][13] =  43; aaJones[12][14] =  16; 
	aaJones[12][15] =  29; aaJones[12][16] = 226; aaJones[12][17] =  24; aaJones[12][18] =  18; aaJones[12][19] = 323; 
	aaJones[13][ 0] =  15; aaJones[13][ 1] =   5; aaJones[13][ 2] =  10; aaJones[13][ 3] =   4; aaJones[13][ 4] =  78; 
	aaJones[13][ 5] =   4; aaJones[13][ 6] =   5; aaJones[13][ 7] =   5; aaJones[13][ 8] =  40; aaJones[13][ 9] =  89; 
	aaJones[13][10] = 248; aaJones[13][11] =   4; aaJones[13][12] =  43; aaJones[13][13] =   0; aaJones[13][14] =  17; 
	aaJones[13][15] =  92; aaJones[13][16] =  12; aaJones[13][17] =  53; aaJones[13][18] = 536; aaJones[13][19] =  62; 
	aaJones[14][ 0] = 194; aaJones[14][ 1] =  74; aaJones[14][ 2] =  15; aaJones[14][ 3] =  15; aaJones[14][ 4] =  14; 
	aaJones[14][ 5] = 164; aaJones[14][ 6] =  18; aaJones[14][ 7] =  24; aaJones[14][ 8] = 115; aaJones[14][ 9] =  10; 
	aaJones[14][10] = 102; aaJones[14][11] =  21; aaJones[14][12] =  16; aaJones[14][13] =  17; aaJones[14][14] =   0; 
	aaJones[14][15] = 285; aaJones[14][16] = 118; aaJones[14][17] =   6; aaJones[14][18] =  10; aaJones[14][19] =  23; 
	aaJones[15][ 0] = 378; aaJones[15][ 1] = 101; aaJones[15][ 2] = 503; aaJones[15][ 3] =  59; aaJones[15][ 4] = 223; 
	aaJones[15][ 5] =  53; aaJones[15][ 6] =  30; aaJones[15][ 7] = 201; aaJones[15][ 8] =  73; aaJones[15][ 9] =  40; 
	aaJones[15][10] =  59; aaJones[15][11] =  47; aaJones[15][12] =  29; aaJones[15][13] =  92; aaJones[15][14] = 285; 
	aaJones[15][15] =   0; aaJones[15][16] = 477; aaJones[15][17] =  35; aaJones[15][18] =  63; aaJones[15][19] =  38; 
	aaJones[16][ 0] = 475; aaJones[16][ 1] =  64; aaJones[16][ 2] = 232; aaJones[16][ 3] =  38; aaJones[16][ 4] =  42; 
	aaJones[16][ 5] =  51; aaJones[16][ 6] =  32; aaJones[16][ 7] =  33; aaJones[16][ 8] =  46; aaJones[16][ 9] = 245; 
	aaJones[16][10] =  25; aaJones[16][11] = 103; aaJones[16][12] = 226; aaJones[16][13] =  12; aaJones[16][14] = 118; 
	aaJones[16][15] = 477; aaJones[16][16] =   0; aaJones[16][17] =  12; aaJones[16][18] =  21; aaJones[16][19] = 112; 
	aaJones[17][ 0] =   9; aaJones[17][ 1] = 126; aaJones[17][ 2] =   8; aaJones[17][ 3] =   4; aaJones[17][ 4] = 115; 
	aaJones[17][ 5] =  18; aaJones[17][ 6] =  10; aaJones[17][ 7] =  55; aaJones[17][ 8] =   8; aaJones[17][ 9] =   9; 
	aaJones[17][10] =  52; aaJones[17][11] =  10; aaJones[17][12] =  24; aaJones[17][13] =  53; aaJones[17][14] =   6; 
	aaJones[17][15] =  35; aaJones[17][16] =  12; aaJones[17][17] =   0; aaJones[17][18] =  71; aaJones[17][19] =  25; 
	aaJones[18][ 0] =  11; aaJones[18][ 1] =  20; aaJones[18][ 2] =  70; aaJones[18][ 3] =  46; aaJones[18][ 4] = 209; 
	aaJones[18][ 5] =  24; aaJones[18][ 6] =   7; aaJones[18][ 7] =   8; aaJones[18][ 8] = 573; aaJones[18][ 9] =  32; 
	aaJones[18][10] =  24; aaJones[18][11] =   8; aaJones[18][12] =  18; aaJones[18][13] = 536; aaJones[18][14] =  10; 
	aaJones[18][15] =  63; aaJones[18][16] =  21; aaJones[18][17] =  71; aaJones[18][18] =   0; aaJones[18][19] =  16; 
	aaJones[19][ 0] = 298; aaJones[19][ 1] =  17; aaJones[19][ 2] =  16; aaJones[19][ 3] =  31; aaJones[19][ 4] =  62; 
	aaJones[19][ 5] =  20; aaJones[19][ 6] =  45; aaJones[19][ 7] =  47; aaJones[19][ 8] =  11; aaJones[19][ 9] = 961; 
	aaJones[19][10] = 180; aaJones[19][11] =  14; aaJones[19][12] = 323; aaJones[19][13] =  62; aaJones[19][14] =  23; 
	aaJones[19][15] =  38; aaJones[19][16] = 112; aaJones[19][17] =  25; aaJones[19][18] =  16; aaJones[19][19] =   0; 

	jonesPi[ 0] = 0.076748;
	jonesPi[ 1] = 0.051691;
	jonesPi[ 2] = 0.042645;
	jonesPi[ 3] = 0.051544;
	jonesPi[ 4] = 0.019803;
	jonesPi[ 5] = 0.040752;
	jonesPi[ 6] = 0.061830;
	jonesPi[ 7] = 0.073152;
	jonesPi[ 8] = 0.022944;
	jonesPi[ 9] = 0.053761;
	jonesPi[10] = 0.091904;
	jonesPi[11] = 0.058676;
	jonesPi[12] = 0.023826;
	jonesPi[13] = 0.040126;
	jonesPi[14] = 0.050901;
	jonesPi[15] = 0.068765;
	jonesPi[16] = 0.058565;
	jonesPi[17] = 0.014261;
	jonesPi[18] = 0.032102;
	jonesPi[19] = 0.066005;

	/* dayhoff */
	aaDayhoff[ 0][ 0] =   0; aaDayhoff[ 0][ 1] =  27; aaDayhoff[ 0][ 2] =  98; aaDayhoff[ 0][ 3] = 120; aaDayhoff[ 0][ 4] =  36; 
	aaDayhoff[ 0][ 5] =  89; aaDayhoff[ 0][ 6] = 198; aaDayhoff[ 0][ 7] = 240; aaDayhoff[ 0][ 8] =  23; aaDayhoff[ 0][ 9] =  65; 
	aaDayhoff[ 0][10] =  41; aaDayhoff[ 0][11] =  26; aaDayhoff[ 0][12] =  72; aaDayhoff[ 0][13] =  18; aaDayhoff[ 0][14] = 250; 
	aaDayhoff[ 0][15] = 409; aaDayhoff[ 0][16] = 371; aaDayhoff[ 0][17] =   0; aaDayhoff[ 0][18] =  24; aaDayhoff[ 0][19] = 208; 
	aaDayhoff[ 1][ 0] =  27; aaDayhoff[ 1][ 1] =   0; aaDayhoff[ 1][ 2] =  32; aaDayhoff[ 1][ 3] =   0; aaDayhoff[ 1][ 4] =  23; 
	aaDayhoff[ 1][ 5] = 246; aaDayhoff[ 1][ 6] =   1; aaDayhoff[ 1][ 7] =   9; aaDayhoff[ 1][ 8] = 240; aaDayhoff[ 1][ 9] =  64; 
	aaDayhoff[ 1][10] =  15; aaDayhoff[ 1][11] = 464; aaDayhoff[ 1][12] =  90; aaDayhoff[ 1][13] =  14; aaDayhoff[ 1][14] = 103; 
	aaDayhoff[ 1][15] = 154; aaDayhoff[ 1][16] =  26; aaDayhoff[ 1][17] = 201; aaDayhoff[ 1][18] =   8; aaDayhoff[ 1][19] =  24; 
	aaDayhoff[ 2][ 0] =  98; aaDayhoff[ 2][ 1] =  32; aaDayhoff[ 2][ 2] =   0; aaDayhoff[ 2][ 3] = 905; aaDayhoff[ 2][ 4] =   0; 
	aaDayhoff[ 2][ 5] = 103; aaDayhoff[ 2][ 6] = 148; aaDayhoff[ 2][ 7] = 139; aaDayhoff[ 2][ 8] = 535; aaDayhoff[ 2][ 9] =  77; 
	aaDayhoff[ 2][10] =  34; aaDayhoff[ 2][11] = 318; aaDayhoff[ 2][12] =   1; aaDayhoff[ 2][13] =  14; aaDayhoff[ 2][14] =  42; 
	aaDayhoff[ 2][15] = 495; aaDayhoff[ 2][16] = 229; aaDayhoff[ 2][17] =  23; aaDayhoff[ 2][18] =  95; aaDayhoff[ 2][19] =  15; 
	aaDayhoff[ 3][ 0] = 120; aaDayhoff[ 3][ 1] =   0; aaDayhoff[ 3][ 2] = 905; aaDayhoff[ 3][ 3] =   0; aaDayhoff[ 3][ 4] =   0; 
	aaDayhoff[ 3][ 5] = 134; aaDayhoff[ 3][ 6] = 1153; aaDayhoff[ 3][ 7] = 125; aaDayhoff[ 3][ 8] =  86; aaDayhoff[ 3][ 9] =  24; 
	aaDayhoff[ 3][10] =   0; aaDayhoff[ 3][11] =  71; aaDayhoff[ 3][12] =   0; aaDayhoff[ 3][13] =   0; aaDayhoff[ 3][14] =  13; 
	aaDayhoff[ 3][15] =  95; aaDayhoff[ 3][16] =  66; aaDayhoff[ 3][17] =   0; aaDayhoff[ 3][18] =   0; aaDayhoff[ 3][19] =  18; 
	aaDayhoff[ 4][ 0] =  36; aaDayhoff[ 4][ 1] =  23; aaDayhoff[ 4][ 2] =   0; aaDayhoff[ 4][ 3] =   0; aaDayhoff[ 4][ 4] =   0; 
	aaDayhoff[ 4][ 5] =   0; aaDayhoff[ 4][ 6] =   0; aaDayhoff[ 4][ 7] =  11; aaDayhoff[ 4][ 8] =  28; aaDayhoff[ 4][ 9] =  44; 
	aaDayhoff[ 4][10] =   0; aaDayhoff[ 4][11] =   0; aaDayhoff[ 4][12] =   0; aaDayhoff[ 4][13] =   0; aaDayhoff[ 4][14] =  19; 
	aaDayhoff[ 4][15] = 161; aaDayhoff[ 4][16] =  16; aaDayhoff[ 4][17] =   0; aaDayhoff[ 4][18] =  96; aaDayhoff[ 4][19] =  49; 
	aaDayhoff[ 5][ 0] =  89; aaDayhoff[ 5][ 1] = 246; aaDayhoff[ 5][ 2] = 103; aaDayhoff[ 5][ 3] = 134; aaDayhoff[ 5][ 4] =   0; 
	aaDayhoff[ 5][ 5] =   0; aaDayhoff[ 5][ 6] = 716; aaDayhoff[ 5][ 7] =  28; aaDayhoff[ 5][ 8] = 606; aaDayhoff[ 5][ 9] =  18; 
	aaDayhoff[ 5][10] =  73; aaDayhoff[ 5][11] = 153; aaDayhoff[ 5][12] = 114; aaDayhoff[ 5][13] =   0; aaDayhoff[ 5][14] = 153; 
	aaDayhoff[ 5][15] =  56; aaDayhoff[ 5][16] =  53; aaDayhoff[ 5][17] =   0; aaDayhoff[ 5][18] =   0; aaDayhoff[ 5][19] =  35; 
	aaDayhoff[ 6][ 0] = 198; aaDayhoff[ 6][ 1] =   1; aaDayhoff[ 6][ 2] = 148; aaDayhoff[ 6][ 3] = 1153; aaDayhoff[ 6][ 4] =   0; 
	aaDayhoff[ 6][ 5] = 716; aaDayhoff[ 6][ 6] =   0; aaDayhoff[ 6][ 7] =  81; aaDayhoff[ 6][ 8] =  43; aaDayhoff[ 6][ 9] =  61; 
	aaDayhoff[ 6][10] =  11; aaDayhoff[ 6][11] =  83; aaDayhoff[ 6][12] =  30; aaDayhoff[ 6][13] =   0; aaDayhoff[ 6][14] =  51; 
	aaDayhoff[ 6][15] =  79; aaDayhoff[ 6][16] =  34; aaDayhoff[ 6][17] =   0; aaDayhoff[ 6][18] =  22; aaDayhoff[ 6][19] =  37; 
	aaDayhoff[ 7][ 0] = 240; aaDayhoff[ 7][ 1] =   9; aaDayhoff[ 7][ 2] = 139; aaDayhoff[ 7][ 3] = 125; aaDayhoff[ 7][ 4] =  11; 
	aaDayhoff[ 7][ 5] =  28; aaDayhoff[ 7][ 6] =  81; aaDayhoff[ 7][ 7] =   0; aaDayhoff[ 7][ 8] =  10; aaDayhoff[ 7][ 9] =   0; 
	aaDayhoff[ 7][10] =   7; aaDayhoff[ 7][11] =  27; aaDayhoff[ 7][12] =  17; aaDayhoff[ 7][13] =  15; aaDayhoff[ 7][14] =  34; 
	aaDayhoff[ 7][15] = 234; aaDayhoff[ 7][16] =  30; aaDayhoff[ 7][17] =   0; aaDayhoff[ 7][18] =   0; aaDayhoff[ 7][19] =  54; 
	aaDayhoff[ 8][ 0] =  23; aaDayhoff[ 8][ 1] = 240; aaDayhoff[ 8][ 2] = 535; aaDayhoff[ 8][ 3] =  86; aaDayhoff[ 8][ 4] =  28; 
	aaDayhoff[ 8][ 5] = 606; aaDayhoff[ 8][ 6] =  43; aaDayhoff[ 8][ 7] =  10; aaDayhoff[ 8][ 8] =   0; aaDayhoff[ 8][ 9] =   7; 
	aaDayhoff[ 8][10] =  44; aaDayhoff[ 8][11] =  26; aaDayhoff[ 8][12] =   0; aaDayhoff[ 8][13] =  48; aaDayhoff[ 8][14] =  94; 
	aaDayhoff[ 8][15] =  35; aaDayhoff[ 8][16] =  22; aaDayhoff[ 8][17] =  27; aaDayhoff[ 8][18] = 127; aaDayhoff[ 8][19] =  44; 
	aaDayhoff[ 9][ 0] =  65; aaDayhoff[ 9][ 1] =  64; aaDayhoff[ 9][ 2] =  77; aaDayhoff[ 9][ 3] =  24; aaDayhoff[ 9][ 4] =  44; 
	aaDayhoff[ 9][ 5] =  18; aaDayhoff[ 9][ 6] =  61; aaDayhoff[ 9][ 7] =   0; aaDayhoff[ 9][ 8] =   7; aaDayhoff[ 9][ 9] =   0; 
	aaDayhoff[ 9][10] = 257; aaDayhoff[ 9][11] =  46; aaDayhoff[ 9][12] = 336; aaDayhoff[ 9][13] = 196; aaDayhoff[ 9][14] =  12; 
	aaDayhoff[ 9][15] =  24; aaDayhoff[ 9][16] = 192; aaDayhoff[ 9][17] =   0; aaDayhoff[ 9][18] =  37; aaDayhoff[ 9][19] = 889; 
	aaDayhoff[10][ 0] =  41; aaDayhoff[10][ 1] =  15; aaDayhoff[10][ 2] =  34; aaDayhoff[10][ 3] =   0; aaDayhoff[10][ 4] =   0; 
	aaDayhoff[10][ 5] =  73; aaDayhoff[10][ 6] =  11; aaDayhoff[10][ 7] =   7; aaDayhoff[10][ 8] =  44; aaDayhoff[10][ 9] = 257; 
	aaDayhoff[10][10] =   0; aaDayhoff[10][11] =  18; aaDayhoff[10][12] = 527; aaDayhoff[10][13] = 157; aaDayhoff[10][14] =  32; 
	aaDayhoff[10][15] =  17; aaDayhoff[10][16] =  33; aaDayhoff[10][17] =  46; aaDayhoff[10][18] =  28; aaDayhoff[10][19] = 175; 
	aaDayhoff[11][ 0] =  26; aaDayhoff[11][ 1] = 464; aaDayhoff[11][ 2] = 318; aaDayhoff[11][ 3] =  71; aaDayhoff[11][ 4] =   0; 
	aaDayhoff[11][ 5] = 153; aaDayhoff[11][ 6] =  83; aaDayhoff[11][ 7] =  27; aaDayhoff[11][ 8] =  26; aaDayhoff[11][ 9] =  46; 
	aaDayhoff[11][10] =  18; aaDayhoff[11][11] =   0; aaDayhoff[11][12] = 243; aaDayhoff[11][13] =   0; aaDayhoff[11][14] =  33; 
	aaDayhoff[11][15] =  96; aaDayhoff[11][16] = 136; aaDayhoff[11][17] =   0; aaDayhoff[11][18] =  13; aaDayhoff[11][19] =  10; 
	aaDayhoff[12][ 0] =  72; aaDayhoff[12][ 1] =  90; aaDayhoff[12][ 2] =   1; aaDayhoff[12][ 3] =   0; aaDayhoff[12][ 4] =   0; 
	aaDayhoff[12][ 5] = 114; aaDayhoff[12][ 6] =  30; aaDayhoff[12][ 7] =  17; aaDayhoff[12][ 8] =   0; aaDayhoff[12][ 9] = 336; 
	aaDayhoff[12][10] = 527; aaDayhoff[12][11] = 243; aaDayhoff[12][12] =   0; aaDayhoff[12][13] =  92; aaDayhoff[12][14] =  17; 
	aaDayhoff[12][15] =  62; aaDayhoff[12][16] = 104; aaDayhoff[12][17] =   0; aaDayhoff[12][18] =   0; aaDayhoff[12][19] = 258; 
	aaDayhoff[13][ 0] =  18; aaDayhoff[13][ 1] =  14; aaDayhoff[13][ 2] =  14; aaDayhoff[13][ 3] =   0; aaDayhoff[13][ 4] =   0; 
	aaDayhoff[13][ 5] =   0; aaDayhoff[13][ 6] =   0; aaDayhoff[13][ 7] =  15; aaDayhoff[13][ 8] =  48; aaDayhoff[13][ 9] = 196; 
	aaDayhoff[13][10] = 157; aaDayhoff[13][11] =   0; aaDayhoff[13][12] =  92; aaDayhoff[13][13] =   0; aaDayhoff[13][14] =  11; 
	aaDayhoff[13][15] =  46; aaDayhoff[13][16] =  13; aaDayhoff[13][17] =  76; aaDayhoff[13][18] = 698; aaDayhoff[13][19] =  12; 
	aaDayhoff[14][ 0] = 250; aaDayhoff[14][ 1] = 103; aaDayhoff[14][ 2] =  42; aaDayhoff[14][ 3] =  13; aaDayhoff[14][ 4] =  19; 
	aaDayhoff[14][ 5] = 153; aaDayhoff[14][ 6] =  51; aaDayhoff[14][ 7] =  34; aaDayhoff[14][ 8] =  94; aaDayhoff[14][ 9] =  12; 
	aaDayhoff[14][10] =  32; aaDayhoff[14][11] =  33; aaDayhoff[14][12] =  17; aaDayhoff[14][13] =  11; aaDayhoff[14][14] =   0; 
	aaDayhoff[14][15] = 245; aaDayhoff[14][16] =  78; aaDayhoff[14][17] =   0; aaDayhoff[14][18] =   0; aaDayhoff[14][19] =  48; 
	aaDayhoff[15][ 0] = 409; aaDayhoff[15][ 1] = 154; aaDayhoff[15][ 2] = 495; aaDayhoff[15][ 3] =  95; aaDayhoff[15][ 4] = 161; 
	aaDayhoff[15][ 5] =  56; aaDayhoff[15][ 6] =  79; aaDayhoff[15][ 7] = 234; aaDayhoff[15][ 8] =  35; aaDayhoff[15][ 9] =  24; 
	aaDayhoff[15][10] =  17; aaDayhoff[15][11] =  96; aaDayhoff[15][12] =  62; aaDayhoff[15][13] =  46; aaDayhoff[15][14] = 245; 
	aaDayhoff[15][15] =   0; aaDayhoff[15][16] = 550; aaDayhoff[15][17] =  75; aaDayhoff[15][18] =  34; aaDayhoff[15][19] =  30; 
	aaDayhoff[16][ 0] = 371; aaDayhoff[16][ 1] =  26; aaDayhoff[16][ 2] = 229; aaDayhoff[16][ 3] =  66; aaDayhoff[16][ 4] =  16; 
	aaDayhoff[16][ 5] =  53; aaDayhoff[16][ 6] =  34; aaDayhoff[16][ 7] =  30; aaDayhoff[16][ 8] =  22; aaDayhoff[16][ 9] = 192; 
	aaDayhoff[16][10] =  33; aaDayhoff[16][11] = 136; aaDayhoff[16][12] = 104; aaDayhoff[16][13] =  13; aaDayhoff[16][14] =  78; 
	aaDayhoff[16][15] = 550; aaDayhoff[16][16] =   0; aaDayhoff[16][17] =   0; aaDayhoff[16][18] =  42; aaDayhoff[16][19] = 157; 
	aaDayhoff[17][ 0] =   0; aaDayhoff[17][ 1] = 201; aaDayhoff[17][ 2] =  23; aaDayhoff[17][ 3] =   0; aaDayhoff[17][ 4] =   0; 
	aaDayhoff[17][ 5] =   0; aaDayhoff[17][ 6] =   0; aaDayhoff[17][ 7] =   0; aaDayhoff[17][ 8] =  27; aaDayhoff[17][ 9] =   0; 
	aaDayhoff[17][10] =  46; aaDayhoff[17][11] =   0; aaDayhoff[17][12] =   0; aaDayhoff[17][13] =  76; aaDayhoff[17][14] =   0; 
	aaDayhoff[17][15] =  75; aaDayhoff[17][16] =   0; aaDayhoff[17][17] =   0; aaDayhoff[17][18] =  61; aaDayhoff[17][19] =   0; 
	aaDayhoff[18][ 0] =  24; aaDayhoff[18][ 1] =   8; aaDayhoff[18][ 2] =  95; aaDayhoff[18][ 3] =   0; aaDayhoff[18][ 4] =  96; 
	aaDayhoff[18][ 5] =   0; aaDayhoff[18][ 6] =  22; aaDayhoff[18][ 7] =   0; aaDayhoff[18][ 8] = 127; aaDayhoff[18][ 9] =  37; 
	aaDayhoff[18][10] =  28; aaDayhoff[18][11] =  13; aaDayhoff[18][12] =   0; aaDayhoff[18][13] = 698; aaDayhoff[18][14] =   0; 
	aaDayhoff[18][15] =  34; aaDayhoff[18][16] =  42; aaDayhoff[18][17] =  61; aaDayhoff[18][18] =   0; aaDayhoff[18][19] =  28; 
	aaDayhoff[19][ 0] = 208; aaDayhoff[19][ 1] =  24; aaDayhoff[19][ 2] =  15; aaDayhoff[19][ 3] =  18; aaDayhoff[19][ 4] =  49; 
	aaDayhoff[19][ 5] =  35; aaDayhoff[19][ 6] =  37; aaDayhoff[19][ 7] =  54; aaDayhoff[19][ 8] =  44; aaDayhoff[19][ 9] = 889; 
	aaDayhoff[19][10] = 175; aaDayhoff[19][11] =  10; aaDayhoff[19][12] = 258; aaDayhoff[19][13] =  12; aaDayhoff[19][14] =  48; 
	aaDayhoff[19][15] =  30; aaDayhoff[19][16] = 157; aaDayhoff[19][17] =   0; aaDayhoff[19][18] =  28; aaDayhoff[19][19] =   0;

	dayhoffPi[ 0] = 0.087127;
	dayhoffPi[ 1] = 0.040904;
	dayhoffPi[ 2] = 0.040432;
	dayhoffPi[ 3] = 0.046872;
	dayhoffPi[ 4] = 0.033474;
	dayhoffPi[ 5] = 0.038255;
	dayhoffPi[ 6] = 0.049530;
	dayhoffPi[ 7] = 0.088612;
	dayhoffPi[ 8] = 0.033618;
	dayhoffPi[ 9] = 0.036886;
	dayhoffPi[10] = 0.085357;
	dayhoffPi[11] = 0.080482;
	dayhoffPi[12] = 0.014753;
	dayhoffPi[13] = 0.039772;
	dayhoffPi[14] = 0.050680;
	dayhoffPi[15] = 0.069577;
	dayhoffPi[16] = 0.058542;
	dayhoffPi[17] = 0.010494;
	dayhoffPi[18] = 0.029916;
	dayhoffPi[19] = 0.064718;

	/* mtrev24 */
	aaMtrev24[ 0][ 0] =   0.00; aaMtrev24[ 0][ 1] =  23.18; aaMtrev24[ 0][ 2] =  26.95; aaMtrev24[ 0][ 3] =  17.67; aaMtrev24[ 0][ 4] =  59.93;
	aaMtrev24[ 0][ 5] =   1.90; aaMtrev24[ 0][ 6] =   9.77; aaMtrev24[ 0][ 7] = 120.71; aaMtrev24[ 0][ 8] =  13.90; aaMtrev24[ 0][ 9] =  96.49;
	aaMtrev24[ 0][10] =  25.46; aaMtrev24[ 0][11] =   8.36; aaMtrev24[ 0][12] = 141.88; aaMtrev24[ 0][13] =   6.37; aaMtrev24[ 0][14] =  54.31;
	aaMtrev24[ 0][15] = 387.86; aaMtrev24[ 0][16] = 480.72; aaMtrev24[ 0][17] =   1.90; aaMtrev24[ 0][18] =   6.48; aaMtrev24[ 0][19] = 195.06;
	aaMtrev24[ 1][ 0] =  23.18; aaMtrev24[ 1][ 1] =   0.00; aaMtrev24[ 1][ 2] =  13.24; aaMtrev24[ 1][ 3] =   1.90; aaMtrev24[ 1][ 4] = 103.33;
	aaMtrev24[ 1][ 5] = 220.99; aaMtrev24[ 1][ 6] =   1.90; aaMtrev24[ 1][ 7] =  23.03; aaMtrev24[ 1][ 8] = 165.23; aaMtrev24[ 1][ 9] =   1.90;
	aaMtrev24[ 1][10] =  15.58; aaMtrev24[ 1][11] = 141.40; aaMtrev24[ 1][12] =   1.90; aaMtrev24[ 1][13] =   4.69; aaMtrev24[ 1][14] =  23.64;
	aaMtrev24[ 1][15] =   6.04; aaMtrev24[ 1][16] =   2.08; aaMtrev24[ 1][17] =  21.95; aaMtrev24[ 1][18] =   1.90; aaMtrev24[ 1][19] =   7.64;
	aaMtrev24[ 2][ 0] =  26.95; aaMtrev24[ 2][ 1] =  13.24; aaMtrev24[ 2][ 2] =   0.00; aaMtrev24[ 2][ 3] = 794.38; aaMtrev24[ 2][ 4] =  58.94;
	aaMtrev24[ 2][ 5] = 173.56; aaMtrev24[ 2][ 6] =  63.05; aaMtrev24[ 2][ 7] =  53.30; aaMtrev24[ 2][ 8] = 496.13; aaMtrev24[ 2][ 9] =  27.10;
	aaMtrev24[ 2][10] =  15.16; aaMtrev24[ 2][11] = 608.70; aaMtrev24[ 2][12] =  65.41; aaMtrev24[ 2][13] =  15.20; aaMtrev24[ 2][14] =  73.31;
	aaMtrev24[ 2][15] = 494.39; aaMtrev24[ 2][16] = 238.46; aaMtrev24[ 2][17] =  10.68; aaMtrev24[ 2][18] = 191.36; aaMtrev24[ 2][19] =   1.90;
	aaMtrev24[ 3][ 0] =  17.67; aaMtrev24[ 3][ 1] =   1.90; aaMtrev24[ 3][ 2] = 794.38; aaMtrev24[ 3][ 3] =   0.00; aaMtrev24[ 3][ 4] =   1.90;
	aaMtrev24[ 3][ 5] =  55.28; aaMtrev24[ 3][ 6] = 583.55; aaMtrev24[ 3][ 7] =  56.77; aaMtrev24[ 3][ 8] = 113.99; aaMtrev24[ 3][ 9] =   4.34;
	aaMtrev24[ 3][10] =   1.90; aaMtrev24[ 3][11] =   2.31; aaMtrev24[ 3][12] =   1.90; aaMtrev24[ 3][13] =   4.98; aaMtrev24[ 3][14] =  13.43;
	aaMtrev24[ 3][15] =  69.02; aaMtrev24[ 3][16] =  28.01; aaMtrev24[ 3][17] =  19.86; aaMtrev24[ 3][18] =  21.21; aaMtrev24[ 3][19] =   1.90;
	aaMtrev24[ 4][ 0] =  59.93; aaMtrev24[ 4][ 1] = 103.33; aaMtrev24[ 4][ 2] =  58.94; aaMtrev24[ 4][ 3] =   1.90; aaMtrev24[ 4][ 4] =   0.00;
	aaMtrev24[ 4][ 5] =  75.24; aaMtrev24[ 4][ 6] =   1.90; aaMtrev24[ 4][ 7] =  30.71; aaMtrev24[ 4][ 8] = 141.49; aaMtrev24[ 4][ 9] =  62.73;
	aaMtrev24[ 4][10] =  25.65; aaMtrev24[ 4][11] =   1.90; aaMtrev24[ 4][12] =   6.18; aaMtrev24[ 4][13] =  70.80; aaMtrev24[ 4][14] =  31.26;
	aaMtrev24[ 4][15] = 277.05; aaMtrev24[ 4][16] = 179.97; aaMtrev24[ 4][17] =  33.60; aaMtrev24[ 4][18] = 254.77; aaMtrev24[ 4][19] =   1.90;
	aaMtrev24[ 5][ 0] =   1.90; aaMtrev24[ 5][ 1] = 220.99; aaMtrev24[ 5][ 2] = 173.56; aaMtrev24[ 5][ 3] =  55.28; aaMtrev24[ 5][ 4] =  75.24;
	aaMtrev24[ 5][ 5] =   0.00; aaMtrev24[ 5][ 6] = 313.56; aaMtrev24[ 5][ 7] =   6.75; aaMtrev24[ 5][ 8] = 582.40; aaMtrev24[ 5][ 9] =   8.34;
	aaMtrev24[ 5][10] =  39.70; aaMtrev24[ 5][11] = 465.58; aaMtrev24[ 5][12] =  47.37; aaMtrev24[ 5][13] =  19.11; aaMtrev24[ 5][14] = 137.29;
	aaMtrev24[ 5][15] =  54.11; aaMtrev24[ 5][16] =  94.93; aaMtrev24[ 5][17] =   1.90; aaMtrev24[ 5][18] =  38.82; aaMtrev24[ 5][19] =  19.00;
	aaMtrev24[ 6][ 0] =   9.77; aaMtrev24[ 6][ 1] =   1.90; aaMtrev24[ 6][ 2] =  63.05; aaMtrev24[ 6][ 3] = 583.55; aaMtrev24[ 6][ 4] =   1.90;
	aaMtrev24[ 6][ 5] = 313.56; aaMtrev24[ 6][ 6] =   0.00; aaMtrev24[ 6][ 7] =  28.28; aaMtrev24[ 6][ 8] =  49.12; aaMtrev24[ 6][ 9] =   3.31;
	aaMtrev24[ 6][10] =   1.90; aaMtrev24[ 6][11] = 313.86; aaMtrev24[ 6][12] =   1.90; aaMtrev24[ 6][13] =   2.67; aaMtrev24[ 6][14] =  12.83;
	aaMtrev24[ 6][15] =  54.71; aaMtrev24[ 6][16] =  14.82; aaMtrev24[ 6][17] =   1.90; aaMtrev24[ 6][18] =  13.12; aaMtrev24[ 6][19] =  21.14;
	aaMtrev24[ 7][ 0] = 120.71; aaMtrev24[ 7][ 1] =  23.03; aaMtrev24[ 7][ 2] =  53.30; aaMtrev24[ 7][ 3] =  56.77; aaMtrev24[ 7][ 4] =  30.71;
	aaMtrev24[ 7][ 5] =   6.75; aaMtrev24[ 7][ 6] =  28.28; aaMtrev24[ 7][ 7] =   0.00; aaMtrev24[ 7][ 8] =   1.90; aaMtrev24[ 7][ 9] =   5.98;
	aaMtrev24[ 7][10] =   2.41; aaMtrev24[ 7][11] =  22.73; aaMtrev24[ 7][12] =   1.90; aaMtrev24[ 7][13] =   1.90; aaMtrev24[ 7][14] =   1.90;
	aaMtrev24[ 7][15] = 125.93; aaMtrev24[ 7][16] =  11.17; aaMtrev24[ 7][17] =  10.92; aaMtrev24[ 7][18] =   3.21; aaMtrev24[ 7][19] =   2.53;
	aaMtrev24[ 8][ 0] =  13.90; aaMtrev24[ 8][ 1] = 165.23; aaMtrev24[ 8][ 2] = 496.13; aaMtrev24[ 8][ 3] = 113.99; aaMtrev24[ 8][ 4] = 141.49;
	aaMtrev24[ 8][ 5] = 582.40; aaMtrev24[ 8][ 6] =  49.12; aaMtrev24[ 8][ 7] =   1.90; aaMtrev24[ 8][ 8] =   0.00; aaMtrev24[ 8][ 9] =  12.26;
	aaMtrev24[ 8][10] =  11.49; aaMtrev24[ 8][11] = 127.67; aaMtrev24[ 8][12] =  11.97; aaMtrev24[ 8][13] =  48.16; aaMtrev24[ 8][14] =  60.97;
	aaMtrev24[ 8][15] =  77.46; aaMtrev24[ 8][16] =  44.78; aaMtrev24[ 8][17] =   7.08; aaMtrev24[ 8][18] = 670.14; aaMtrev24[ 8][19] =   1.90;
	aaMtrev24[ 9][ 0] =  96.49; aaMtrev24[ 9][ 1] =   1.90; aaMtrev24[ 9][ 2] =  27.10; aaMtrev24[ 9][ 3] =   4.34; aaMtrev24[ 9][ 4] =  62.73;
	aaMtrev24[ 9][ 5] =   8.34; aaMtrev24[ 9][ 6] =   3.31; aaMtrev24[ 9][ 7] =   5.98; aaMtrev24[ 9][ 8] =  12.26; aaMtrev24[ 9][ 9] =   0.00;
	aaMtrev24[ 9][10] = 329.09; aaMtrev24[ 9][11] =  19.57; aaMtrev24[ 9][12] = 517.98; aaMtrev24[ 9][13] =  84.67; aaMtrev24[ 9][14] =  20.63;
	aaMtrev24[ 9][15] =  47.70; aaMtrev24[ 9][16] = 368.43; aaMtrev24[ 9][17] =   1.90; aaMtrev24[ 9][18] =  25.01; aaMtrev24[ 9][19] =1222.94;
	aaMtrev24[10][ 0] =  25.46; aaMtrev24[10][ 1] =  15.58; aaMtrev24[10][ 2] =  15.16; aaMtrev24[10][ 3] =   1.90; aaMtrev24[10][ 4] =  25.65;
	aaMtrev24[10][ 5] =  39.70; aaMtrev24[10][ 6] =   1.90; aaMtrev24[10][ 7] =   2.41; aaMtrev24[10][ 8] =  11.49; aaMtrev24[10][ 9] = 329.09;
	aaMtrev24[10][10] =   0.00; aaMtrev24[10][11] =  14.88; aaMtrev24[10][12] = 537.53; aaMtrev24[10][13] = 216.06; aaMtrev24[10][14] =  40.10;
	aaMtrev24[10][15] =  73.61; aaMtrev24[10][16] = 126.40; aaMtrev24[10][17] =  32.44; aaMtrev24[10][18] =  44.15; aaMtrev24[10][19] =  91.67;
	aaMtrev24[11][ 0] =   8.36; aaMtrev24[11][ 1] = 141.40; aaMtrev24[11][ 2] = 608.70; aaMtrev24[11][ 3] =   2.31; aaMtrev24[11][ 4] =   1.90;
	aaMtrev24[11][ 5] = 465.58; aaMtrev24[11][ 6] = 313.86; aaMtrev24[11][ 7] =  22.73; aaMtrev24[11][ 8] = 127.67; aaMtrev24[11][ 9] =  19.57;
	aaMtrev24[11][10] =  14.88; aaMtrev24[11][11] =   0.00; aaMtrev24[11][12] =  91.37; aaMtrev24[11][13] =   6.44; aaMtrev24[11][14] =  50.10;
	aaMtrev24[11][15] = 105.79; aaMtrev24[11][16] = 136.33; aaMtrev24[11][17] =  24.00; aaMtrev24[11][18] =  51.17; aaMtrev24[11][19] =   1.90;
	aaMtrev24[12][ 0] = 141.88; aaMtrev24[12][ 1] =   1.90; aaMtrev24[12][ 2] =  65.41; aaMtrev24[12][ 3] =   1.90; aaMtrev24[12][ 4] =   6.18;
	aaMtrev24[12][ 5] =  47.37; aaMtrev24[12][ 6] =   1.90; aaMtrev24[12][ 7] =   1.90; aaMtrev24[12][ 8] =  11.97; aaMtrev24[12][ 9] = 517.98;
	aaMtrev24[12][10] = 537.53; aaMtrev24[12][11] =  91.37; aaMtrev24[12][12] =   0.00; aaMtrev24[12][13] =  90.82; aaMtrev24[12][14] =  18.84;
	aaMtrev24[12][15] = 111.16; aaMtrev24[12][16] = 528.17; aaMtrev24[12][17] =  21.71; aaMtrev24[12][18] =  39.96; aaMtrev24[12][19] = 387.54;
	aaMtrev24[13][ 0] =   6.37; aaMtrev24[13][ 1] =   4.69; aaMtrev24[13][ 2] =  15.20; aaMtrev24[13][ 3] =   4.98; aaMtrev24[13][ 4] =  70.80;
	aaMtrev24[13][ 5] =  19.11; aaMtrev24[13][ 6] =   2.67; aaMtrev24[13][ 7] =   1.90; aaMtrev24[13][ 8] =  48.16; aaMtrev24[13][ 9] =  84.67;
	aaMtrev24[13][10] = 216.06; aaMtrev24[13][11] =   6.44; aaMtrev24[13][12] =  90.82; aaMtrev24[13][13] =   0.00; aaMtrev24[13][14] =  17.31;
	aaMtrev24[13][15] =  64.29; aaMtrev24[13][16] =  33.85; aaMtrev24[13][17] =   7.84; aaMtrev24[13][18] = 465.58; aaMtrev24[13][19] =   6.35;
	aaMtrev24[14][ 0] =  54.31; aaMtrev24[14][ 1] =  23.64; aaMtrev24[14][ 2] =  73.31; aaMtrev24[14][ 3] =  13.43; aaMtrev24[14][ 4] =  31.26;
	aaMtrev24[14][ 5] = 137.29; aaMtrev24[14][ 6] =  12.83; aaMtrev24[14][ 7] =   1.90; aaMtrev24[14][ 8] =  60.97; aaMtrev24[14][ 9] =  20.63;
	aaMtrev24[14][10] =  40.10; aaMtrev24[14][11] =  50.10; aaMtrev24[14][12] =  18.84; aaMtrev24[14][13] =  17.31; aaMtrev24[14][14] =   0.00;
	aaMtrev24[14][15] = 169.90; aaMtrev24[14][16] = 128.22; aaMtrev24[14][17] =   4.21; aaMtrev24[14][18] =  16.21; aaMtrev24[14][19] =   8.23;
	aaMtrev24[15][ 0] = 387.86; aaMtrev24[15][ 1] =   6.04; aaMtrev24[15][ 2] = 494.39; aaMtrev24[15][ 3] =  69.02; aaMtrev24[15][ 4] = 277.05;
	aaMtrev24[15][ 5] =  54.11; aaMtrev24[15][ 6] =  54.71; aaMtrev24[15][ 7] = 125.93; aaMtrev24[15][ 8] =  77.46; aaMtrev24[15][ 9] =  47.70;
	aaMtrev24[15][10] =  73.61; aaMtrev24[15][11] = 105.79; aaMtrev24[15][12] = 111.16; aaMtrev24[15][13] =  64.29; aaMtrev24[15][14] = 169.90;
	aaMtrev24[15][15] =   0.00; aaMtrev24[15][16] = 597.21; aaMtrev24[15][17] =  38.58; aaMtrev24[15][18] =  64.92; aaMtrev24[15][19] =   1.90;
	aaMtrev24[16][ 0] = 480.72; aaMtrev24[16][ 1] =   2.08; aaMtrev24[16][ 2] = 238.46; aaMtrev24[16][ 3] =  28.01; aaMtrev24[16][ 4] = 179.97;
	aaMtrev24[16][ 5] =  94.93; aaMtrev24[16][ 6] =  14.82; aaMtrev24[16][ 7] =  11.17; aaMtrev24[16][ 8] =  44.78; aaMtrev24[16][ 9] = 368.43;
	aaMtrev24[16][10] = 126.40; aaMtrev24[16][11] = 136.33; aaMtrev24[16][12] = 528.17; aaMtrev24[16][13] =  33.85; aaMtrev24[16][14] = 128.22;
	aaMtrev24[16][15] = 597.21; aaMtrev24[16][16] =   0.00; aaMtrev24[16][17] =   9.99; aaMtrev24[16][18] =  38.73; aaMtrev24[16][19] = 204.54;
	aaMtrev24[17][ 0] =   1.90; aaMtrev24[17][ 1] =  21.95; aaMtrev24[17][ 2] =  10.68; aaMtrev24[17][ 3] =  19.86; aaMtrev24[17][ 4] =  33.60;
	aaMtrev24[17][ 5] =   1.90; aaMtrev24[17][ 6] =   1.90; aaMtrev24[17][ 7] =  10.92; aaMtrev24[17][ 8] =   7.08; aaMtrev24[17][ 9] =   1.90;
	aaMtrev24[17][10] =  32.44; aaMtrev24[17][11] =  24.00; aaMtrev24[17][12] =  21.71; aaMtrev24[17][13] =   7.84; aaMtrev24[17][14] =   4.21;
	aaMtrev24[17][15] =  38.58; aaMtrev24[17][16] =   9.99; aaMtrev24[17][17] =   0.00; aaMtrev24[17][18] =  26.25; aaMtrev24[17][19] =   5.37;
	aaMtrev24[18][ 0] =   6.48; aaMtrev24[18][ 1] =   1.90; aaMtrev24[18][ 2] = 191.36; aaMtrev24[18][ 3] =  21.21; aaMtrev24[18][ 4] = 254.77;
	aaMtrev24[18][ 5] =  38.82; aaMtrev24[18][ 6] =  13.12; aaMtrev24[18][ 7] =   3.21; aaMtrev24[18][ 8] = 670.14; aaMtrev24[18][ 9] =  25.01;
	aaMtrev24[18][10] =  44.15; aaMtrev24[18][11] =  51.17; aaMtrev24[18][12] =  39.96; aaMtrev24[18][13] = 465.58; aaMtrev24[18][14] =  16.21;
	aaMtrev24[18][15] =  64.92; aaMtrev24[18][16] =  38.73; aaMtrev24[18][17] =  26.25; aaMtrev24[18][18] =   0.00; aaMtrev24[18][19] =   1.90;
	aaMtrev24[19][ 0] = 195.06; aaMtrev24[19][ 1] =   7.64; aaMtrev24[19][ 2] =   1.90; aaMtrev24[19][ 3] =   1.90; aaMtrev24[19][ 4] =   1.90;
	aaMtrev24[19][ 5] =  19.00; aaMtrev24[19][ 6] =  21.14; aaMtrev24[19][ 7] =   2.53; aaMtrev24[19][ 8] =   1.90; aaMtrev24[19][ 9] =1222.94;
	aaMtrev24[19][10] =  91.67; aaMtrev24[19][11] =   1.90; aaMtrev24[19][12] = 387.54; aaMtrev24[19][13] =   6.35; aaMtrev24[19][14] =   8.23;
	aaMtrev24[19][15] =   1.90; aaMtrev24[19][16] = 204.54; aaMtrev24[19][17] =   5.37; aaMtrev24[19][18] =   1.90; aaMtrev24[19][19] =   0.00;

	mtrev24Pi[ 0] = 0.072;
	mtrev24Pi[ 1] = 0.019;
	mtrev24Pi[ 2] = 0.039;
	mtrev24Pi[ 3] = 0.019;
	mtrev24Pi[ 4] = 0.006;
	mtrev24Pi[ 5] = 0.025;
	mtrev24Pi[ 6] = 0.024;
	mtrev24Pi[ 7] = 0.056;
	mtrev24Pi[ 8] = 0.028;
	mtrev24Pi[ 9] = 0.088;
	mtrev24Pi[10] = 0.168;
	mtrev24Pi[11] = 0.023;
	mtrev24Pi[12] = 0.054;
	mtrev24Pi[13] = 0.061;
	mtrev24Pi[14] = 0.054;
	mtrev24Pi[15] = 0.072;
	mtrev24Pi[16] = 0.086;
	mtrev24Pi[17] = 0.029;
	mtrev24Pi[18] = 0.033;
	mtrev24Pi[19] = 0.043;
	
	/* mtmam */
	aaMtmam[ 0][ 0] =   0; aaMtmam[ 0][ 1] =  32; aaMtmam[ 0][ 2] =   2; aaMtmam[ 0][ 3] =  11; aaMtmam[ 0][ 4] =   0;
	aaMtmam[ 0][ 5] =   0; aaMtmam[ 0][ 6] =   0; aaMtmam[ 0][ 7] =  78; aaMtmam[ 0][ 8] =   8; aaMtmam[ 0][ 9] =  75;
	aaMtmam[ 0][10] =  21; aaMtmam[ 0][11] =   0; aaMtmam[ 0][12] =  76; aaMtmam[ 0][13] =   0; aaMtmam[ 0][14] =  53;
	aaMtmam[ 0][15] = 342; aaMtmam[ 0][16] = 681; aaMtmam[ 0][17] =   5; aaMtmam[ 0][18] =   0; aaMtmam[ 0][19] = 398;
	aaMtmam[ 1][ 0] =  32; aaMtmam[ 1][ 1] =   0; aaMtmam[ 1][ 2] =   4; aaMtmam[ 1][ 3] =   0; aaMtmam[ 1][ 4] = 186;
	aaMtmam[ 1][ 5] = 246; aaMtmam[ 1][ 6] =   0; aaMtmam[ 1][ 7] =  18; aaMtmam[ 1][ 8] = 232; aaMtmam[ 1][ 9] =   0;
	aaMtmam[ 1][10] =   6; aaMtmam[ 1][11] =  50; aaMtmam[ 1][12] =   0; aaMtmam[ 1][13] =   0; aaMtmam[ 1][14] =   9;
	aaMtmam[ 1][15] =   3; aaMtmam[ 1][16] =   0; aaMtmam[ 1][17] =  16; aaMtmam[ 1][18] =   0; aaMtmam[ 1][19] =   0;
	aaMtmam[ 2][ 0] =   2; aaMtmam[ 2][ 1] =   4; aaMtmam[ 2][ 2] =   0; aaMtmam[ 2][ 3] = 864; aaMtmam[ 2][ 4] =   0;
	aaMtmam[ 2][ 5] =   8; aaMtmam[ 2][ 6] =   0; aaMtmam[ 2][ 7] =  47; aaMtmam[ 2][ 8] = 458; aaMtmam[ 2][ 9] =  19;
	aaMtmam[ 2][10] =   0; aaMtmam[ 2][11] = 408; aaMtmam[ 2][12] =  21; aaMtmam[ 2][13] =   6; aaMtmam[ 2][14] =  33;
	aaMtmam[ 2][15] = 446; aaMtmam[ 2][16] = 110; aaMtmam[ 2][17] =   6; aaMtmam[ 2][18] = 156; aaMtmam[ 2][19] =   0;
	aaMtmam[ 3][ 0] =  11; aaMtmam[ 3][ 1] =   0; aaMtmam[ 3][ 2] = 864; aaMtmam[ 3][ 3] =   0; aaMtmam[ 3][ 4] =   0;
	aaMtmam[ 3][ 5] =  49; aaMtmam[ 3][ 6] = 569; aaMtmam[ 3][ 7] =  79; aaMtmam[ 3][ 8] =  11; aaMtmam[ 3][ 9] =   0;
	aaMtmam[ 3][10] =   0; aaMtmam[ 3][11] =   0; aaMtmam[ 3][12] =   0; aaMtmam[ 3][13] =   5; aaMtmam[ 3][14] =   2;
	aaMtmam[ 3][15] =  16; aaMtmam[ 3][16] =   0; aaMtmam[ 3][17] =   0; aaMtmam[ 3][18] =   0; aaMtmam[ 3][19] =  10;
	aaMtmam[ 4][ 0] =   0; aaMtmam[ 4][ 1] = 186; aaMtmam[ 4][ 2] =   0; aaMtmam[ 4][ 3] =   0; aaMtmam[ 4][ 4] =   0;
	aaMtmam[ 4][ 5] =   0; aaMtmam[ 4][ 6] =   0; aaMtmam[ 4][ 7] =   0; aaMtmam[ 4][ 8] = 305; aaMtmam[ 4][ 9] =  41;
	aaMtmam[ 4][10] =  27; aaMtmam[ 4][11] =   0; aaMtmam[ 4][12] =   0; aaMtmam[ 4][13] =   7; aaMtmam[ 4][14] =   0;
	aaMtmam[ 4][15] = 347; aaMtmam[ 4][16] = 114; aaMtmam[ 4][17] =  65; aaMtmam[ 4][18] = 530; aaMtmam[ 4][19] =   0;
	aaMtmam[ 5][ 0] =   0; aaMtmam[ 5][ 1] = 246; aaMtmam[ 5][ 2] =   8; aaMtmam[ 5][ 3] =  49; aaMtmam[ 5][ 4] =   0;
	aaMtmam[ 5][ 5] =   0; aaMtmam[ 5][ 6] = 274; aaMtmam[ 5][ 7] =   0; aaMtmam[ 5][ 8] = 550; aaMtmam[ 5][ 9] =   0;
	aaMtmam[ 5][10] =  20; aaMtmam[ 5][11] = 242; aaMtmam[ 5][12] =  22; aaMtmam[ 5][13] =   0; aaMtmam[ 5][14] =  51;
	aaMtmam[ 5][15] =  30; aaMtmam[ 5][16] =   0; aaMtmam[ 5][17] =   0; aaMtmam[ 5][18] =  54; aaMtmam[ 5][19] =  33;
	aaMtmam[ 6][ 0] =   0; aaMtmam[ 6][ 1] =   0; aaMtmam[ 6][ 2] =   0; aaMtmam[ 6][ 3] = 569; aaMtmam[ 6][ 4] =   0;
	aaMtmam[ 6][ 5] = 274; aaMtmam[ 6][ 6] =   0; aaMtmam[ 6][ 7] =  22; aaMtmam[ 6][ 8] =  22; aaMtmam[ 6][ 9] =   0;
	aaMtmam[ 6][10] =   0; aaMtmam[ 6][11] = 215; aaMtmam[ 6][12] =   0; aaMtmam[ 6][13] =   0; aaMtmam[ 6][14] =   0;
	aaMtmam[ 6][15] =  21; aaMtmam[ 6][16] =   4; aaMtmam[ 6][17] =   0; aaMtmam[ 6][18] =   0; aaMtmam[ 6][19] =  20;
	aaMtmam[ 7][ 0] =  78; aaMtmam[ 7][ 1] =  18; aaMtmam[ 7][ 2] =  47; aaMtmam[ 7][ 3] =  79; aaMtmam[ 7][ 4] =   0;
	aaMtmam[ 7][ 5] =   0; aaMtmam[ 7][ 6] =  22; aaMtmam[ 7][ 7] =   0; aaMtmam[ 7][ 8] =   0; aaMtmam[ 7][ 9] =   0;
	aaMtmam[ 7][10] =   0; aaMtmam[ 7][11] =   0; aaMtmam[ 7][12] =   0; aaMtmam[ 7][13] =   0; aaMtmam[ 7][14] =   0;
	aaMtmam[ 7][15] = 112; aaMtmam[ 7][16] =   0; aaMtmam[ 7][17] =   0; aaMtmam[ 7][18] =   1; aaMtmam[ 7][19] =   5;
	aaMtmam[ 8][ 0] =   8; aaMtmam[ 8][ 1] = 232; aaMtmam[ 8][ 2] = 458; aaMtmam[ 8][ 3] =  11; aaMtmam[ 8][ 4] = 305;
	aaMtmam[ 8][ 5] = 550; aaMtmam[ 8][ 6] =  22; aaMtmam[ 8][ 7] =   0; aaMtmam[ 8][ 8] =   0; aaMtmam[ 8][ 9] =   0;
	aaMtmam[ 8][10] =  26; aaMtmam[ 8][11] =   0; aaMtmam[ 8][12] =   0; aaMtmam[ 8][13] =   0; aaMtmam[ 8][14] =  53;
	aaMtmam[ 8][15] =  20; aaMtmam[ 8][16] =   1; aaMtmam[ 8][17] =   0; aaMtmam[ 8][18] =1525; aaMtmam[ 8][19] =   0;
	aaMtmam[ 9][ 0] =  75; aaMtmam[ 9][ 1] =   0; aaMtmam[ 9][ 2] =  19; aaMtmam[ 9][ 3] =   0; aaMtmam[ 9][ 4] =  41;
	aaMtmam[ 9][ 5] =   0; aaMtmam[ 9][ 6] =   0; aaMtmam[ 9][ 7] =   0; aaMtmam[ 9][ 8] =   0; aaMtmam[ 9][ 9] =   0;
	aaMtmam[ 9][10] = 232; aaMtmam[ 9][11] =   6; aaMtmam[ 9][12] = 378; aaMtmam[ 9][13] =  57; aaMtmam[ 9][14] =   5;
	aaMtmam[ 9][15] =   0; aaMtmam[ 9][16] = 360; aaMtmam[ 9][17] =   0; aaMtmam[ 9][18] =  16; aaMtmam[ 9][19] =2220;
	aaMtmam[10][ 0] =  21; aaMtmam[10][ 1] =   6; aaMtmam[10][ 2] =   0; aaMtmam[10][ 3] =   0; aaMtmam[10][ 4] =  27;
	aaMtmam[10][ 5] =  20; aaMtmam[10][ 6] =   0; aaMtmam[10][ 7] =   0; aaMtmam[10][ 8] =  26; aaMtmam[10][ 9] = 232;
	aaMtmam[10][10] =   0; aaMtmam[10][11] =   4; aaMtmam[10][12] = 609; aaMtmam[10][13] = 246; aaMtmam[10][14] =  43;
	aaMtmam[10][15] =  74; aaMtmam[10][16] =  34; aaMtmam[10][17] =  12; aaMtmam[10][18] =  25; aaMtmam[10][19] = 100;
	aaMtmam[11][ 0] =   0; aaMtmam[11][ 1] =  50; aaMtmam[11][ 2] = 408; aaMtmam[11][ 3] =   0; aaMtmam[11][ 4] =   0;
	aaMtmam[11][ 5] = 242; aaMtmam[11][ 6] = 215; aaMtmam[11][ 7] =   0; aaMtmam[11][ 8] =   0; aaMtmam[11][ 9] =   6;
	aaMtmam[11][10] =   4; aaMtmam[11][11] =   0; aaMtmam[11][12] =  59; aaMtmam[11][13] =   0; aaMtmam[11][14] =  18;
	aaMtmam[11][15] =  65; aaMtmam[11][16] =  50; aaMtmam[11][17] =   0; aaMtmam[11][18] =  67; aaMtmam[11][19] =   0;
	aaMtmam[12][ 0] =  76; aaMtmam[12][ 1] =   0; aaMtmam[12][ 2] =  21; aaMtmam[12][ 3] =   0; aaMtmam[12][ 4] =   0;
	aaMtmam[12][ 5] =  22; aaMtmam[12][ 6] =   0; aaMtmam[12][ 7] =   0; aaMtmam[12][ 8] =   0; aaMtmam[12][ 9] = 378;
	aaMtmam[12][10] = 609; aaMtmam[12][11] =  59; aaMtmam[12][12] =   0; aaMtmam[12][13] =  11; aaMtmam[12][14] =   0;
	aaMtmam[12][15] =  47; aaMtmam[12][16] = 691; aaMtmam[12][17] =  13; aaMtmam[12][18] =   0; aaMtmam[12][19] = 832;
	aaMtmam[13][ 0] =   0; aaMtmam[13][ 1] =   0; aaMtmam[13][ 2] =   6; aaMtmam[13][ 3] =   5; aaMtmam[13][ 4] =   7;
	aaMtmam[13][ 5] =   0; aaMtmam[13][ 6] =   0; aaMtmam[13][ 7] =   0; aaMtmam[13][ 8] =   0; aaMtmam[13][ 9] =  57;
	aaMtmam[13][10] = 246; aaMtmam[13][11] =   0; aaMtmam[13][12] =  11; aaMtmam[13][13] =   0; aaMtmam[13][14] =  17;
	aaMtmam[13][15] =  90; aaMtmam[13][16] =   8; aaMtmam[13][17] =   0; aaMtmam[13][18] = 682; aaMtmam[13][19] =   6;
	aaMtmam[14][ 0] =  53; aaMtmam[14][ 1] =   9; aaMtmam[14][ 2] =  33; aaMtmam[14][ 3] =   2; aaMtmam[14][ 4] =   0;
	aaMtmam[14][ 5] =  51; aaMtmam[14][ 6] =   0; aaMtmam[14][ 7] =   0; aaMtmam[14][ 8] =  53; aaMtmam[14][ 9] =   5;
	aaMtmam[14][10] =  43; aaMtmam[14][11] =  18; aaMtmam[14][12] =   0; aaMtmam[14][13] =  17; aaMtmam[14][14] =   0;
	aaMtmam[14][15] = 202; aaMtmam[14][16] =  78; aaMtmam[14][17] =   7; aaMtmam[14][18] =   8; aaMtmam[14][19] =   0;
	aaMtmam[15][ 0] = 342; aaMtmam[15][ 1] =   3; aaMtmam[15][ 2] = 446; aaMtmam[15][ 3] =  16; aaMtmam[15][ 4] = 347;
	aaMtmam[15][ 5] =  30; aaMtmam[15][ 6] =  21; aaMtmam[15][ 7] = 112; aaMtmam[15][ 8] =  20; aaMtmam[15][ 9] =   0;
	aaMtmam[15][10] =  74; aaMtmam[15][11] =  65; aaMtmam[15][12] =  47; aaMtmam[15][13] =  90; aaMtmam[15][14] = 202;
	aaMtmam[15][15] =   0; aaMtmam[15][16] = 614; aaMtmam[15][17] =  17; aaMtmam[15][18] = 107; aaMtmam[15][19] =   0;
	aaMtmam[16][ 0] = 681; aaMtmam[16][ 1] =   0; aaMtmam[16][ 2] = 110; aaMtmam[16][ 3] =   0; aaMtmam[16][ 4] = 114;
	aaMtmam[16][ 5] =   0; aaMtmam[16][ 6] =   4; aaMtmam[16][ 7] =   0; aaMtmam[16][ 8] =   1; aaMtmam[16][ 9] = 360;
	aaMtmam[16][10] =  34; aaMtmam[16][11] =  50; aaMtmam[16][12] = 691; aaMtmam[16][13] =   8; aaMtmam[16][14] =  78;
	aaMtmam[16][15] = 614; aaMtmam[16][16] =   0; aaMtmam[16][17] =   0; aaMtmam[16][18] =   0; aaMtmam[16][19] = 237;
	aaMtmam[17][ 0] =   5; aaMtmam[17][ 1] =  16; aaMtmam[17][ 2] =   6; aaMtmam[17][ 3] =   0; aaMtmam[17][ 4] =  65;
	aaMtmam[17][ 5] =   0; aaMtmam[17][ 6] =   0; aaMtmam[17][ 7] =   0; aaMtmam[17][ 8] =   0; aaMtmam[17][ 9] =   0;
	aaMtmam[17][10] =  12; aaMtmam[17][11] =   0; aaMtmam[17][12] =  13; aaMtmam[17][13] =   0; aaMtmam[17][14] =   7;
	aaMtmam[17][15] =  17; aaMtmam[17][16] =   0; aaMtmam[17][17] =   0; aaMtmam[17][18] =  14; aaMtmam[17][19] =   0;
	aaMtmam[18][ 0] =   0; aaMtmam[18][ 1] =   0; aaMtmam[18][ 2] = 156; aaMtmam[18][ 3] =   0; aaMtmam[18][ 4] = 530;
	aaMtmam[18][ 5] =  54; aaMtmam[18][ 6] =   0; aaMtmam[18][ 7] =   1; aaMtmam[18][ 8] =1525; aaMtmam[18][ 9] =  16;
	aaMtmam[18][10] =  25; aaMtmam[18][11] =  67; aaMtmam[18][12] =   0; aaMtmam[18][13] = 682; aaMtmam[18][14] =   8;
	aaMtmam[18][15] = 107; aaMtmam[18][16] =   0; aaMtmam[18][17] =  14; aaMtmam[18][18] =   0; aaMtmam[18][19] =   0;
	aaMtmam[19][ 0] = 398; aaMtmam[19][ 1] =   0; aaMtmam[19][ 2] =   0; aaMtmam[19][ 3] =  10; aaMtmam[19][ 4] =   0;
	aaMtmam[19][ 5] =  33; aaMtmam[19][ 6] =  20; aaMtmam[19][ 7] =   5; aaMtmam[19][ 8] =   0; aaMtmam[19][ 9] =2220;
	aaMtmam[19][10] = 100; aaMtmam[19][11] =   0; aaMtmam[19][12] = 832; aaMtmam[19][13] =   6; aaMtmam[19][14] =   0;
	aaMtmam[19][15] =   0; aaMtmam[19][16] = 237; aaMtmam[19][17] =   0; aaMtmam[19][18] =   0; aaMtmam[19][19] =   0;

	mtmamPi[ 0] = 0.0692;
	mtmamPi[ 1] = 0.0184;
	mtmamPi[ 2] = 0.0400;
	mtmamPi[ 3] = 0.0186;
	mtmamPi[ 4] = 0.0065;
	mtmamPi[ 5] = 0.0238;
	mtmamPi[ 6] = 0.0236;
	mtmamPi[ 7] = 0.0557;
	mtmamPi[ 8] = 0.0277;
	mtmamPi[ 9] = 0.0905;
	mtmamPi[10] = 0.1675;
	mtmamPi[11] = 0.0221;
	mtmamPi[12] = 0.0561;
	mtmamPi[13] = 0.0611;
	mtmamPi[14] = 0.0536;
	mtmamPi[15] = 0.0725;
	mtmamPi[16] = 0.0870;
	mtmamPi[17] = 0.0293;
	mtmamPi[18] = 0.0340;
	mtmamPi[19] = 0.0428;
	
	/* rtRev */
	aartREV[ 0][ 0] =   0; aartREV[ 1][ 0] =  34; aartREV[ 2][ 0] =  51; aartREV[ 3][ 0] =  10; aartREV[ 4][ 0] = 439;
	aartREV[ 5][ 0] =  32; aartREV[ 6][ 0] =  81; aartREV[ 7][ 0] = 135; aartREV[ 8][ 0] =  30; aartREV[ 9][ 0] =   1;
	aartREV[10][ 0] =  45; aartREV[11][ 0] =  38; aartREV[12][ 0] = 235; aartREV[13][ 0] =   1; aartREV[14][ 0] =  97;
	aartREV[15][ 0] = 460; aartREV[16][ 0] = 258; aartREV[17][ 0] =   5; aartREV[18][ 0] =  55; aartREV[19][ 0] = 197;
	aartREV[ 0][ 1] =  34; aartREV[ 1][ 1] =   0; aartREV[ 2][ 1] =  35; aartREV[ 3][ 1] =  30; aartREV[ 4][ 1] =  92;
	aartREV[ 5][ 1] = 221; aartREV[ 6][ 1] =  10; aartREV[ 7][ 1] =  41; aartREV[ 8][ 1] =  90; aartREV[ 9][ 1] =  24;
	aartREV[10][ 1] =  18; aartREV[11][ 1] = 593; aartREV[12][ 1] =  57; aartREV[13][ 1] =   7; aartREV[14][ 1] =  24;
	aartREV[15][ 1] = 102; aartREV[16][ 1] =  64; aartREV[17][ 1] =  13; aartREV[18][ 1] =  47; aartREV[19][ 1] =  29;
	aartREV[ 0][ 2] =  51; aartREV[ 1][ 2] =  35; aartREV[ 2][ 2] =   0; aartREV[ 3][ 2] = 384; aartREV[ 4][ 2] = 128;
	aartREV[ 5][ 2] = 236; aartREV[ 6][ 2] =  79; aartREV[ 7][ 2] =  94; aartREV[ 8][ 2] = 320; aartREV[ 9][ 2] =  35;
	aartREV[10][ 2] =  15; aartREV[11][ 2] = 123; aartREV[12][ 2] =   1; aartREV[13][ 2] =  49; aartREV[14][ 2] =  33;
	aartREV[15][ 2] = 294; aartREV[16][ 2] = 148; aartREV[17][ 2] =  16; aartREV[18][ 2] =  28; aartREV[19][ 2] =  21;
	aartREV[ 0][ 3] =  10; aartREV[ 1][ 3] =  30; aartREV[ 2][ 3] = 384; aartREV[ 3][ 3] =   0; aartREV[ 4][ 3] =   1;
	aartREV[ 5][ 3] =  78; aartREV[ 6][ 3] = 542; aartREV[ 7][ 3] =  61; aartREV[ 8][ 3] =  91; aartREV[ 9][ 3] =   1;
	aartREV[10][ 3] =   5; aartREV[11][ 3] =  20; aartREV[12][ 3] =   1; aartREV[13][ 3] =   1; aartREV[14][ 3] =  55;
	aartREV[15][ 3] = 136; aartREV[16][ 3] =  55; aartREV[17][ 3] =   1; aartREV[18][ 3] =   1; aartREV[19][ 3] =   6;
	aartREV[ 0][ 4] = 439; aartREV[ 1][ 4] =  92; aartREV[ 2][ 4] = 128; aartREV[ 3][ 4] =   1; aartREV[ 4][ 4] =   0;
	aartREV[ 5][ 4] =  70; aartREV[ 6][ 4] =   1; aartREV[ 7][ 4] =  48; aartREV[ 8][ 4] = 124; aartREV[ 9][ 4] = 104;
	aartREV[10][ 4] = 110; aartREV[11][ 4] =  16; aartREV[12][ 4] = 156; aartREV[13][ 4] =  70; aartREV[14][ 4] =   1;
	aartREV[15][ 4] =  75; aartREV[16][ 4] = 117; aartREV[17][ 4] =  55; aartREV[18][ 4] = 131; aartREV[19][ 4] = 295;
	aartREV[ 0][ 5] =  32; aartREV[ 1][ 5] = 221; aartREV[ 2][ 5] = 236; aartREV[ 3][ 5] =  78; aartREV[ 4][ 5] =  70;
	aartREV[ 5][ 5] =   0; aartREV[ 6][ 5] = 372; aartREV[ 7][ 5] =  18; aartREV[ 8][ 5] = 387; aartREV[ 9][ 5] =  33;
	aartREV[10][ 5] =  54; aartREV[11][ 5] = 309; aartREV[12][ 5] = 158; aartREV[13][ 5] =   1; aartREV[14][ 5] =  68;
	aartREV[15][ 5] = 225; aartREV[16][ 5] = 146; aartREV[17][ 5] =  10; aartREV[18][ 5] =  45; aartREV[19][ 5] =  36;
	aartREV[ 0][ 6] =  81; aartREV[ 1][ 6] =  10; aartREV[ 2][ 6] =  79; aartREV[ 3][ 6] = 542; aartREV[ 4][ 6] =   1;
	aartREV[ 5][ 6] = 372; aartREV[ 6][ 6] =   0; aartREV[ 7][ 6] =  70; aartREV[ 8][ 6] =  34; aartREV[ 9][ 6] =   1;
	aartREV[10][ 6] =  21; aartREV[11][ 6] = 141; aartREV[12][ 6] =   1; aartREV[13][ 6] =   1; aartREV[14][ 6] =  52;
	aartREV[15][ 6] =  95; aartREV[16][ 6] =  82; aartREV[17][ 6] =  17; aartREV[18][ 6] =   1; aartREV[19][ 6] =  35;
	aartREV[ 0][ 7] = 135; aartREV[ 1][ 7] =  41; aartREV[ 2][ 7] =  94; aartREV[ 3][ 7] =  61; aartREV[ 4][ 7] =  48;
	aartREV[ 5][ 7] =  18; aartREV[ 6][ 7] =  70; aartREV[ 7][ 7] =   0; aartREV[ 8][ 7] =  68; aartREV[ 9][ 7] =   1;
	aartREV[10][ 7] =   3; aartREV[11][ 7] =  30; aartREV[12][ 7] =  37; aartREV[13][ 7] =   7; aartREV[14][ 7] =  17;
	aartREV[15][ 7] = 152; aartREV[16][ 7] =   7; aartREV[17][ 7] =  23; aartREV[18][ 7] =  21; aartREV[19][ 7] =   3;
	aartREV[ 0][ 8] =  30; aartREV[ 1][ 8] =  90; aartREV[ 2][ 8] = 320; aartREV[ 3][ 8] =  91; aartREV[ 4][ 8] = 124;
	aartREV[ 5][ 8] = 387; aartREV[ 6][ 8] =  34; aartREV[ 7][ 8] =  68; aartREV[ 8][ 8] =   0; aartREV[ 9][ 8] =  34;
	aartREV[10][ 8] =  51; aartREV[11][ 8] =  76; aartREV[12][ 8] = 116; aartREV[13][ 8] = 141; aartREV[14][ 8] =  44;
	aartREV[15][ 8] = 183; aartREV[16][ 8] =  49; aartREV[17][ 8] =  48; aartREV[18][ 8] = 307; aartREV[19][ 8] =   1;
	aartREV[ 0][ 9] =   1; aartREV[ 1][ 9] =  24; aartREV[ 2][ 9] =  35; aartREV[ 3][ 9] =   1; aartREV[ 4][ 9] = 104;
	aartREV[ 5][ 9] =  33; aartREV[ 6][ 9] =   1; aartREV[ 7][ 9] =   1; aartREV[ 8][ 9] =  34; aartREV[ 9][ 9] =   0;
	aartREV[10][ 9] = 385; aartREV[11][ 9] =  34; aartREV[12][ 9] = 375; aartREV[13][ 9] =  64; aartREV[14][ 9] =  10;
	aartREV[15][ 9] =   4; aartREV[16][ 9] =  72; aartREV[17][ 9] =  39; aartREV[18][ 9] =  26; aartREV[19][ 9] =1048;
	aartREV[ 0][10] =  45; aartREV[ 1][10] =  18; aartREV[ 2][10] =  15; aartREV[ 3][10] =   5; aartREV[ 4][10] = 110;
	aartREV[ 5][10] =  54; aartREV[ 6][10] =  21; aartREV[ 7][10] =   3; aartREV[ 8][10] =  51; aartREV[ 9][10] = 385;
	aartREV[10][10] =   0; aartREV[11][10] =  23; aartREV[12][10] = 581; aartREV[13][10] = 179; aartREV[14][10] =  22;
	aartREV[15][10] =  24; aartREV[16][10] =  25; aartREV[17][10] =  47; aartREV[18][10] =  64; aartREV[19][10] = 112;
	aartREV[ 0][11] =  38; aartREV[ 1][11] = 593; aartREV[ 2][11] = 123; aartREV[ 3][11] =  20; aartREV[ 4][11] =  16;
	aartREV[ 5][11] = 309; aartREV[ 6][11] = 141; aartREV[ 7][11] =  30; aartREV[ 8][11] =  76; aartREV[ 9][11] =  34;
	aartREV[10][11] =  23; aartREV[11][11] =   0; aartREV[12][11] = 134; aartREV[13][11] =  14; aartREV[14][11] =  43;
	aartREV[15][11] =  77; aartREV[16][11] = 110; aartREV[17][11] =   6; aartREV[18][11] =   1; aartREV[19][11] =  19;
	aartREV[ 0][12] = 235; aartREV[ 1][12] =  57; aartREV[ 2][12] =   1; aartREV[ 3][12] =   1; aartREV[ 4][12] = 156;
	aartREV[ 5][12] = 158; aartREV[ 6][12] =   1; aartREV[ 7][12] =  37; aartREV[ 8][12] = 116; aartREV[ 9][12] = 375;
	aartREV[10][12] = 581; aartREV[11][12] = 134; aartREV[12][12] =   0; aartREV[13][12] = 247; aartREV[14][12] =   1;
	aartREV[15][12] =   1; aartREV[16][12] = 131; aartREV[17][12] = 111; aartREV[18][12] =  74; aartREV[19][12] = 236;
	aartREV[ 0][13] =   1; aartREV[ 1][13] =   7; aartREV[ 2][13] =  49; aartREV[ 3][13] =   1; aartREV[ 4][13] =  70;
	aartREV[ 5][13] =   1; aartREV[ 6][13] =   1; aartREV[ 7][13] =   7; aartREV[ 8][13] = 141; aartREV[ 9][13] =  64;
	aartREV[10][13] = 179; aartREV[11][13] =  14; aartREV[12][13] = 247; aartREV[13][13] =   0; aartREV[14][13] =  11;
	aartREV[15][13] =  20; aartREV[16][13] =  69; aartREV[17][13] = 182; aartREV[18][13] =1017; aartREV[19][13] =  92;
	aartREV[ 0][14] =  97; aartREV[ 1][14] =  24; aartREV[ 2][14] =  33; aartREV[ 3][14] =  55; aartREV[ 4][14] =   1;
	aartREV[ 5][14] =  68; aartREV[ 6][14] =  52; aartREV[ 7][14] =  17; aartREV[ 8][14] =  44; aartREV[ 9][14] =  10;
	aartREV[10][14] =  22; aartREV[11][14] =  43; aartREV[12][14] =   1; aartREV[13][14] =  11; aartREV[14][14] =   0;
	aartREV[15][14] = 134; aartREV[16][14] =  62; aartREV[17][14] =   9; aartREV[18][14] =  14; aartREV[19][14] =  25;
	aartREV[ 0][15] = 460; aartREV[ 1][15] = 102; aartREV[ 2][15] = 294; aartREV[ 3][15] = 136; aartREV[ 4][15] =  75;
	aartREV[ 5][15] = 225; aartREV[ 6][15] =  95; aartREV[ 7][15] = 152; aartREV[ 8][15] = 183; aartREV[ 9][15] =   4;
	aartREV[10][15] =  24; aartREV[11][15] =  77; aartREV[12][15] =   1; aartREV[13][15] =  20; aartREV[14][15] = 134;
	aartREV[15][15] =   0; aartREV[16][15] = 671; aartREV[17][15] =  14; aartREV[18][15] =  31; aartREV[19][15] =  39;
	aartREV[ 0][16] = 258; aartREV[ 1][16] =  64; aartREV[ 2][16] = 148; aartREV[ 3][16] =  55; aartREV[ 4][16] = 117;
	aartREV[ 5][16] = 146; aartREV[ 6][16] =  82; aartREV[ 7][16] =   7; aartREV[ 8][16] =  49; aartREV[ 9][16] =  72;
	aartREV[10][16] =  25; aartREV[11][16] = 110; aartREV[12][16] = 131; aartREV[13][16] =  69; aartREV[14][16] =  62;
	aartREV[15][16] = 671; aartREV[16][16] =   0; aartREV[17][16] =   1; aartREV[18][16] =  34; aartREV[19][16] = 196;
	aartREV[ 0][17] =   5; aartREV[ 1][17] =  13; aartREV[ 2][17] =  16; aartREV[ 3][17] =   1; aartREV[ 4][17] =  55;
	aartREV[ 5][17] =  10; aartREV[ 6][17] =  17; aartREV[ 7][17] =  23; aartREV[ 8][17] =  48; aartREV[ 9][17] =  39;
	aartREV[10][17] =  47; aartREV[11][17] =   6; aartREV[12][17] = 111; aartREV[13][17] = 182; aartREV[14][17] =   9;
	aartREV[15][17] =  14; aartREV[16][17] =   1; aartREV[17][17] =   0; aartREV[18][17] = 176; aartREV[19][17] =  26;
	aartREV[ 0][18] =  55; aartREV[ 1][18] =  47; aartREV[ 2][18] =  28; aartREV[ 3][18] =   1; aartREV[ 4][18] = 131;
	aartREV[ 5][18] =  45; aartREV[ 6][18] =   1; aartREV[ 7][18] =  21; aartREV[ 8][18] = 307; aartREV[ 9][18] =  26;
	aartREV[10][18] =  64; aartREV[11][18] =   1; aartREV[12][18] =  74; aartREV[13][18] =1017; aartREV[14][18] =  14;
	aartREV[15][18] =  31; aartREV[16][18] =  34; aartREV[17][18] = 176; aartREV[18][18] =   0; aartREV[19][18] =  59;
	aartREV[ 0][19] = 197; aartREV[ 1][19] =  29; aartREV[ 2][19] =  21; aartREV[ 3][19] =   6; aartREV[ 4][19] = 295;
	aartREV[ 5][19] =  36; aartREV[ 6][19] =  35; aartREV[ 7][19] =   3; aartREV[ 8][19] =   1; aartREV[ 9][19] =1048;
	aartREV[10][19] = 112; aartREV[11][19] =  19; aartREV[12][19] = 236; aartREV[13][19] =  92; aartREV[14][19] =  25;
	aartREV[15][19] =  39; aartREV[16][19] = 196; aartREV[17][19] =  26; aartREV[18][19] =  59; aartREV[19][19] =   0;
	rtrevPi[ 0] = 0.0646;
	rtrevPi[ 1] = 0.0453;
	rtrevPi[ 2] = 0.0376;
	rtrevPi[ 3] = 0.0422;
	rtrevPi[ 4] = 0.0114;
	rtrevPi[ 5] = 0.0606;
	rtrevPi[ 6] = 0.0607;
	rtrevPi[ 7] = 0.0639;
	rtrevPi[ 8] = 0.0273;
	rtrevPi[ 9] = 0.0679;
	rtrevPi[10] = 0.1018;
	rtrevPi[11] = 0.0751;
	rtrevPi[12] = 0.0150;
	rtrevPi[13] = 0.0287;
	rtrevPi[14] = 0.0681;
	rtrevPi[15] = 0.0488;
	rtrevPi[16] = 0.0622;
	rtrevPi[17] = 0.0251;
	rtrevPi[18] = 0.0318;
	rtrevPi[19] = 0.0619;
	
	/* wag */
	aaWAG[ 0][ 0] = 0.0000000; aaWAG[ 1][ 0] = 0.5515710; aaWAG[ 2][ 0] = 0.5098480; aaWAG[ 3][ 0] = 0.7389980; aaWAG[ 4][ 0] = 1.0270400; 
	aaWAG[ 5][ 0] = 0.9085980; aaWAG[ 6][ 0] = 1.5828500; aaWAG[ 7][ 0] = 1.4167200; aaWAG[ 8][ 0] = 0.3169540; aaWAG[ 9][ 0] = 0.1933350; 
	aaWAG[10][ 0] = 0.3979150; aaWAG[11][ 0] = 0.9062650; aaWAG[12][ 0] = 0.8934960; aaWAG[13][ 0] = 0.2104940; aaWAG[14][ 0] = 1.4385500; 
	aaWAG[15][ 0] = 3.3707900; aaWAG[16][ 0] = 2.1211100; aaWAG[17][ 0] = 0.1131330; aaWAG[18][ 0] = 0.2407350; aaWAG[19][ 0] = 2.0060100;
	aaWAG[ 0][ 1] = 0.5515710; aaWAG[ 1][ 1] = 0.0000000; aaWAG[ 2][ 1] = 0.6353460; aaWAG[ 3][ 1] = 0.1473040; aaWAG[ 4][ 1] = 0.5281910;  
	aaWAG[ 5][ 1] = 3.0355000; aaWAG[ 6][ 1] = 0.4391570; aaWAG[ 7][ 1] = 0.5846650; aaWAG[ 8][ 1] = 2.1371500; aaWAG[ 9][ 1] = 0.1869790;  
	aaWAG[10][ 1] = 0.4976710; aaWAG[11][ 1] = 5.3514200; aaWAG[12][ 1] = 0.6831620; aaWAG[13][ 1] = 0.1027110; aaWAG[14][ 1] = 0.6794890;  
	aaWAG[15][ 1] = 1.2241900; aaWAG[16][ 1] = 0.5544130; aaWAG[17][ 1] = 1.1639200; aaWAG[18][ 1] = 0.3815330; aaWAG[19][ 1] = 0.2518490;
	aaWAG[ 0][ 2] = 0.5098480; aaWAG[ 1][ 2] = 0.6353460; aaWAG[ 2][ 2] = 0.0000000; aaWAG[ 3][ 2] = 5.4294200; aaWAG[ 4][ 2] = 0.2652560;  
	aaWAG[ 5][ 2] = 1.5436400; aaWAG[ 6][ 2] = 0.9471980; aaWAG[ 7][ 2] = 1.1255600; aaWAG[ 8][ 2] = 3.9562900; aaWAG[ 9][ 2] = 0.5542360;  
	aaWAG[10][ 2] = 0.1315280; aaWAG[11][ 2] = 3.0120100; aaWAG[12][ 2] = 0.1982210; aaWAG[13][ 2] = 0.0961621; aaWAG[14][ 2] = 0.1950810;  
	aaWAG[15][ 2] = 3.9742300; aaWAG[16][ 2] = 2.0300600; aaWAG[17][ 2] = 0.0719167; aaWAG[18][ 2] = 1.0860000; aaWAG[19][ 2] = 0.1962460;
	aaWAG[ 0][ 3] = 0.7389980; aaWAG[ 1][ 3] = 0.1473040; aaWAG[ 2][ 3] = 5.4294200; aaWAG[ 3][ 3] = 0.0000000; aaWAG[ 4][ 3] = 0.0302949;  
	aaWAG[ 5][ 3] = 0.6167830; aaWAG[ 6][ 3] = 6.1741600; aaWAG[ 7][ 3] = 0.8655840; aaWAG[ 8][ 3] = 0.9306760; aaWAG[ 9][ 3] = 0.0394370;  
	aaWAG[10][ 3] = 0.0848047; aaWAG[11][ 3] = 0.4798550; aaWAG[12][ 3] = 0.1037540; aaWAG[13][ 3] = 0.0467304; aaWAG[14][ 3] = 0.4239840;  
	aaWAG[15][ 3] = 1.0717600; aaWAG[16][ 3] = 0.3748660; aaWAG[17][ 3] = 0.1297670; aaWAG[18][ 3] = 0.3257110; aaWAG[19][ 3] = 0.1523350;
	aaWAG[ 0][ 4] = 1.0270400; aaWAG[ 1][ 4] = 0.5281910; aaWAG[ 2][ 4] = 0.2652560; aaWAG[ 3][ 4] = 0.0302949; aaWAG[ 4][ 4] = 0.0000000;  
	aaWAG[ 5][ 4] = 0.0988179; aaWAG[ 6][ 4] = 0.0213520; aaWAG[ 7][ 4] = 0.3066740; aaWAG[ 8][ 4] = 0.2489720; aaWAG[ 9][ 4] = 0.1701350;  
	aaWAG[10][ 4] = 0.3842870; aaWAG[11][ 4] = 0.0740339; aaWAG[12][ 4] = 0.3904820; aaWAG[13][ 4] = 0.3980200; aaWAG[14][ 4] = 0.1094040;  
	aaWAG[15][ 4] = 1.4076600; aaWAG[16][ 4] = 0.5129840; aaWAG[17][ 4] = 0.7170700; aaWAG[18][ 4] = 0.5438330; aaWAG[19][ 4] = 1.0021400;
	aaWAG[ 0][ 5] = 0.9085980; aaWAG[ 1][ 5] = 3.0355000; aaWAG[ 2][ 5] = 1.5436400; aaWAG[ 3][ 5] = 0.6167830; aaWAG[ 4][ 5] = 0.0988179;  
	aaWAG[ 5][ 5] = 0.0000000; aaWAG[ 6][ 5] = 5.4694700; aaWAG[ 7][ 5] = 0.3300520; aaWAG[ 8][ 5] = 4.2941100; aaWAG[ 9][ 5] = 0.1139170;  
	aaWAG[10][ 5] = 0.8694890; aaWAG[11][ 5] = 3.8949000; aaWAG[12][ 5] = 1.5452600; aaWAG[13][ 5] = 0.0999208; aaWAG[14][ 5] = 0.9333720;  
	aaWAG[15][ 5] = 1.0288700; aaWAG[16][ 5] = 0.8579280; aaWAG[17][ 5] = 0.2157370; aaWAG[18][ 5] = 0.2277100; aaWAG[19][ 5] = 0.3012810;
	aaWAG[ 0][ 6] = 1.5828500; aaWAG[ 1][ 6] = 0.4391570; aaWAG[ 2][ 6] = 0.9471980; aaWAG[ 3][ 6] = 6.1741600; aaWAG[ 4][ 6] = 0.0213520;  
	aaWAG[ 5][ 6] = 5.4694700; aaWAG[ 6][ 6] = 0.0000000; aaWAG[ 7][ 6] = 0.5677170; aaWAG[ 8][ 6] = 0.5700250; aaWAG[ 9][ 6] = 0.1273950;  
	aaWAG[10][ 6] = 0.1542630; aaWAG[11][ 6] = 2.5844300; aaWAG[12][ 6] = 0.3151240; aaWAG[13][ 6] = 0.0811339; aaWAG[14][ 6] = 0.6823550;  
	aaWAG[15][ 6] = 0.7049390; aaWAG[16][ 6] = 0.8227650; aaWAG[17][ 6] = 0.1565570; aaWAG[18][ 6] = 0.1963030; aaWAG[19][ 6] = 0.5887310;
	aaWAG[ 0][ 7] = 1.4167200; aaWAG[ 1][ 7] = 0.5846650; aaWAG[ 2][ 7] = 1.1255600; aaWAG[ 3][ 7] = 0.8655840; aaWAG[ 4][ 7] = 0.3066740;  
	aaWAG[ 5][ 7] = 0.3300520; aaWAG[ 6][ 7] = 0.5677170; aaWAG[ 7][ 7] = 0.0000000; aaWAG[ 8][ 7] = 0.2494100; aaWAG[ 9][ 7] = 0.0304501;  
	aaWAG[10][ 7] = 0.0613037; aaWAG[11][ 7] = 0.3735580; aaWAG[12][ 7] = 0.1741000; aaWAG[13][ 7] = 0.0499310; aaWAG[14][ 7] = 0.2435700;  
	aaWAG[15][ 7] = 1.3418200; aaWAG[16][ 7] = 0.2258330; aaWAG[17][ 7] = 0.3369830; aaWAG[18][ 7] = 0.1036040; aaWAG[19][ 7] = 0.1872470;
	aaWAG[ 0][ 8] = 0.3169540; aaWAG[ 1][ 8] = 2.1371500; aaWAG[ 2][ 8] = 3.9562900; aaWAG[ 3][ 8] = 0.9306760; aaWAG[ 4][ 8] = 0.2489720;  
	aaWAG[ 5][ 8] = 4.2941100; aaWAG[ 6][ 8] = 0.5700250; aaWAG[ 7][ 8] = 0.2494100; aaWAG[ 8][ 8] = 0.0000000; aaWAG[ 9][ 8] = 0.1381900;  
	aaWAG[10][ 8] = 0.4994620; aaWAG[11][ 8] = 0.8904320; aaWAG[12][ 8] = 0.4041410; aaWAG[13][ 8] = 0.6793710; aaWAG[14][ 8] = 0.6961980;  
	aaWAG[15][ 8] = 0.7401690; aaWAG[16][ 8] = 0.4733070; aaWAG[17][ 8] = 0.2625690; aaWAG[18][ 8] = 3.8734400; aaWAG[19][ 8] = 0.1183580;
	aaWAG[ 0][ 9] = 0.1933350; aaWAG[ 1][ 9] = 0.1869790; aaWAG[ 2][ 9] = 0.5542360; aaWAG[ 3][ 9] = 0.0394370; aaWAG[ 4][ 9] = 0.1701350;  
	aaWAG[ 5][ 9] = 0.1139170; aaWAG[ 6][ 9] = 0.1273950; aaWAG[ 7][ 9] = 0.0304501; aaWAG[ 8][ 9] = 0.1381900; aaWAG[ 9][ 9] = 0.0000000;  
	aaWAG[10][ 9] = 3.1709700; aaWAG[11][ 9] = 0.3238320; aaWAG[12][ 9] = 4.2574600; aaWAG[13][ 9] = 1.0594700; aaWAG[14][ 9] = 0.0999288;  
	aaWAG[15][ 9] = 0.3194400; aaWAG[16][ 9] = 1.4581600; aaWAG[17][ 9] = 0.2124830; aaWAG[18][ 9] = 0.4201700; aaWAG[19][ 9] = 7.8213000;
	aaWAG[ 0][10] = 0.3979150; aaWAG[ 1][10] = 0.4976710; aaWAG[ 2][10] = 0.1315280; aaWAG[ 3][10] = 0.0848047; aaWAG[ 4][10] = 0.3842870;  
	aaWAG[ 5][10] = 0.8694890; aaWAG[ 6][10] = 0.1542630; aaWAG[ 7][10] = 0.0613037; aaWAG[ 8][10] = 0.4994620; aaWAG[ 9][10] = 3.1709700;  
	aaWAG[10][10] = 0.0000000; aaWAG[11][10] = 0.2575550; aaWAG[12][10] = 4.8540200; aaWAG[13][10] = 2.1151700; aaWAG[14][10] = 0.4158440;  
	aaWAG[15][10] = 0.3447390; aaWAG[16][10] = 0.3266220; aaWAG[17][10] = 0.6653090; aaWAG[18][10] = 0.3986180; aaWAG[19][10] = 1.8003400;
	aaWAG[ 0][11] = 0.9062650; aaWAG[ 1][11] = 5.3514200; aaWAG[ 2][11] = 3.0120100; aaWAG[ 3][11] = 0.4798550; aaWAG[ 4][11] = 0.0740339;  
	aaWAG[ 5][11] = 3.8949000; aaWAG[ 6][11] = 2.5844300; aaWAG[ 7][11] = 0.3735580; aaWAG[ 8][11] = 0.8904320; aaWAG[ 9][11] = 0.3238320;  
	aaWAG[10][11] = 0.2575550; aaWAG[11][11] = 0.0000000; aaWAG[12][11] = 0.9342760; aaWAG[13][11] = 0.0888360; aaWAG[14][11] = 0.5568960;  
	aaWAG[15][11] = 0.9671300; aaWAG[16][11] = 1.3869800; aaWAG[17][11] = 0.1375050; aaWAG[18][11] = 0.1332640; aaWAG[19][11] = 0.3054340;
	aaWAG[ 0][12] = 0.8934960; aaWAG[ 1][12] = 0.6831620; aaWAG[ 2][12] = 0.1982210; aaWAG[ 3][12] = 0.1037540; aaWAG[ 4][12] = 0.3904820;  
	aaWAG[ 5][12] = 1.5452600; aaWAG[ 6][12] = 0.3151240; aaWAG[ 7][12] = 0.1741000; aaWAG[ 8][12] = 0.4041410; aaWAG[ 9][12] = 4.2574600;  
	aaWAG[10][12] = 4.8540200; aaWAG[11][12] = 0.9342760; aaWAG[12][12] = 0.0000000; aaWAG[13][12] = 1.1906300; aaWAG[14][12] = 0.1713290;  
	aaWAG[15][12] = 0.4939050; aaWAG[16][12] = 1.5161200; aaWAG[17][12] = 0.5157060; aaWAG[18][12] = 0.4284370; aaWAG[19][12] = 2.0584500;
	aaWAG[ 0][13] = 0.2104940; aaWAG[ 1][13] = 0.1027110; aaWAG[ 2][13] = 0.0961621; aaWAG[ 3][13] = 0.0467304; aaWAG[ 4][13] = 0.3980200;  
	aaWAG[ 5][13] = 0.0999208; aaWAG[ 6][13] = 0.0811339; aaWAG[ 7][13] = 0.0499310; aaWAG[ 8][13] = 0.6793710; aaWAG[ 9][13] = 1.0594700;  
	aaWAG[10][13] = 2.1151700; aaWAG[11][13] = 0.0888360; aaWAG[12][13] = 1.1906300; aaWAG[13][13] = 0.0000000; aaWAG[14][13] = 0.1614440;  
	aaWAG[15][13] = 0.5459310; aaWAG[16][13] = 0.1719030; aaWAG[17][13] = 1.5296400; aaWAG[18][13] = 6.4542800; aaWAG[19][13] = 0.6498920;
	aaWAG[ 0][14] = 1.4385500; aaWAG[ 1][14] = 0.6794890; aaWAG[ 2][14] = 0.1950810; aaWAG[ 3][14] = 0.4239840; aaWAG[ 4][14] = 0.1094040;  
	aaWAG[ 5][14] = 0.9333720; aaWAG[ 6][14] = 0.6823550; aaWAG[ 7][14] = 0.2435700; aaWAG[ 8][14] = 0.6961980; aaWAG[ 9][14] = 0.0999288;  
	aaWAG[10][14] = 0.4158440; aaWAG[11][14] = 0.5568960; aaWAG[12][14] = 0.1713290; aaWAG[13][14] = 0.1614440; aaWAG[14][14] = 0.0000000;  
	aaWAG[15][14] = 1.6132800; aaWAG[16][14] = 0.7953840; aaWAG[17][14] = 0.1394050; aaWAG[18][14] = 0.2160460; aaWAG[19][14] = 0.3148870;
	aaWAG[ 0][15] = 3.3707900; aaWAG[ 1][15] = 1.2241900; aaWAG[ 2][15] = 3.9742300; aaWAG[ 3][15] = 1.0717600; aaWAG[ 4][15] = 1.4076600;  
	aaWAG[ 5][15] = 1.0288700; aaWAG[ 6][15] = 0.7049390; aaWAG[ 7][15] = 1.3418200; aaWAG[ 8][15] = 0.7401690; aaWAG[ 9][15] = 0.3194400;  
	aaWAG[10][15] = 0.3447390; aaWAG[11][15] = 0.9671300; aaWAG[12][15] = 0.4939050; aaWAG[13][15] = 0.5459310; aaWAG[14][15] = 1.6132800;  
	aaWAG[15][15] = 0.0000000; aaWAG[16][15] = 4.3780200; aaWAG[17][15] = 0.5237420; aaWAG[18][15] = 0.7869930; aaWAG[19][15] = 0.2327390;
	aaWAG[ 0][16] = 2.1211100; aaWAG[ 1][16] = 0.5544130; aaWAG[ 2][16] = 2.0300600; aaWAG[ 3][16] = 0.3748660; aaWAG[ 4][16] = 0.5129840;  
	aaWAG[ 5][16] = 0.8579280; aaWAG[ 6][16] = 0.8227650; aaWAG[ 7][16] = 0.2258330; aaWAG[ 8][16] = 0.4733070; aaWAG[ 9][16] = 1.4581600;  
	aaWAG[10][16] = 0.3266220; aaWAG[11][16] = 1.3869800; aaWAG[12][16] = 1.5161200; aaWAG[13][16] = 0.1719030; aaWAG[14][16] = 0.7953840;  
	aaWAG[15][16] = 4.3780200; aaWAG[16][16] = 0.0000000; aaWAG[17][16] = 0.1108640; aaWAG[18][16] = 0.2911480; aaWAG[19][16] = 1.3882300;
	aaWAG[ 0][17] = 0.1131330; aaWAG[ 1][17] = 1.1639200; aaWAG[ 2][17] = 0.0719167; aaWAG[ 3][17] = 0.1297670; aaWAG[ 4][17] = 0.7170700;  
	aaWAG[ 5][17] = 0.2157370; aaWAG[ 6][17] = 0.1565570; aaWAG[ 7][17] = 0.3369830; aaWAG[ 8][17] = 0.2625690; aaWAG[ 9][17] = 0.2124830;  
	aaWAG[10][17] = 0.6653090; aaWAG[11][17] = 0.1375050; aaWAG[12][17] = 0.5157060; aaWAG[13][17] = 1.5296400; aaWAG[14][17] = 0.1394050;  
	aaWAG[15][17] = 0.5237420; aaWAG[16][17] = 0.1108640; aaWAG[17][17] = 0.0000000; aaWAG[18][17] = 2.4853900; aaWAG[19][17] = 0.3653690;
	aaWAG[ 0][18] = 0.2407350; aaWAG[ 1][18] = 0.3815330; aaWAG[ 2][18] = 1.0860000; aaWAG[ 3][18] = 0.3257110; aaWAG[ 4][18] = 0.5438330;  
	aaWAG[ 5][18] = 0.2277100; aaWAG[ 6][18] = 0.1963030; aaWAG[ 7][18] = 0.1036040; aaWAG[ 8][18] = 3.8734400; aaWAG[ 9][18] = 0.4201700;  
	aaWAG[10][18] = 0.3986180; aaWAG[11][18] = 0.1332640; aaWAG[12][18] = 0.4284370; aaWAG[13][18] = 6.4542800; aaWAG[14][18] = 0.2160460;  
	aaWAG[15][18] = 0.7869930; aaWAG[16][18] = 0.2911480; aaWAG[17][18] = 2.4853900; aaWAG[18][18] = 0.0000000; aaWAG[19][18] = 0.3147300;
	aaWAG[ 0][19] = 2.0060100; aaWAG[ 1][19] = 0.2518490; aaWAG[ 2][19] = 0.1962460; aaWAG[ 3][19] = 0.1523350; aaWAG[ 4][19] = 1.0021400;  
	aaWAG[ 5][19] = 0.3012810; aaWAG[ 6][19] = 0.5887310; aaWAG[ 7][19] = 0.1872470; aaWAG[ 8][19] = 0.1183580; aaWAG[ 9][19] = 7.8213000;  
	aaWAG[10][19] = 1.8003400; aaWAG[11][19] = 0.3054340; aaWAG[12][19] = 2.0584500; aaWAG[13][19] = 0.6498920; aaWAG[14][19] = 0.3148870;  
	aaWAG[15][19] = 0.2327390; aaWAG[16][19] = 1.3882300; aaWAG[17][19] = 0.3653690; aaWAG[18][19] = 0.3147300; aaWAG[19][19] = 0.0000000;
	wagPi[ 0] = 0.08662790;
	wagPi[ 1] = 0.04397200;
	wagPi[ 2] = 0.03908940;
	wagPi[ 3] = 0.05704510;
	wagPi[ 4] = 0.01930780;
	wagPi[ 5] = 0.03672810;
	wagPi[ 6] = 0.05805890;
	wagPi[ 7] = 0.08325180;
	wagPi[ 8] = 0.02443130;
	wagPi[ 9] = 0.04846600;
	wagPi[10] = 0.08620970;
	wagPi[11] = 0.06202860;
	wagPi[12] = 0.01950273;
	wagPi[13] = 0.03843190;
	wagPi[14] = 0.04576310;
	wagPi[15] = 0.06951790;
	wagPi[16] = 0.06101270;
	wagPi[17] = 0.01438590;
	wagPi[18] = 0.03527420;
	wagPi[19] = 0.07089560;

	/* cpRev */
	aacpREV[ 0][ 0] =    0; aacpREV[ 0][ 1] =  105; aacpREV[ 0][ 2] =  227; aacpREV[ 0][ 3] =  175; aacpREV[ 0][ 4] =  669; 
	aacpREV[ 0][ 5] =  157; aacpREV[ 0][ 6] =  499; aacpREV[ 0][ 7] =  665; aacpREV[ 0][ 8] =   66; aacpREV[ 0][ 9] =  145; 
	aacpREV[ 0][10] =  197; aacpREV[ 0][11] =  236; aacpREV[ 0][12] =  185; aacpREV[ 0][13] =   68; aacpREV[ 0][14] =  490; 
	aacpREV[ 0][15] = 2440; aacpREV[ 0][16] = 1340; aacpREV[ 0][17] =   14; aacpREV[ 0][18] =   56; aacpREV[ 0][19] =  968; 
	aacpREV[ 1][ 0] =  105; aacpREV[ 1][ 1] =    0; aacpREV[ 1][ 2] =  357; aacpREV[ 1][ 3] =   43; aacpREV[ 1][ 4] =  823; 
	aacpREV[ 1][ 5] = 1745; aacpREV[ 1][ 6] =  152; aacpREV[ 1][ 7] =  243; aacpREV[ 1][ 8] =  715; aacpREV[ 1][ 9] =  136; 
	aacpREV[ 1][10] =  203; aacpREV[ 1][11] = 4482; aacpREV[ 1][12] =  125; aacpREV[ 1][13] =   53; aacpREV[ 1][14] =   87; 
	aacpREV[ 1][15] =  385; aacpREV[ 1][16] =  314; aacpREV[ 1][17] =  230; aacpREV[ 1][18] =  323; aacpREV[ 1][19] =   92; 
	aacpREV[ 2][ 0] =  227; aacpREV[ 2][ 1] =  357; aacpREV[ 2][ 2] =    0; aacpREV[ 2][ 3] = 4435; aacpREV[ 2][ 4] =  538; 
	aacpREV[ 2][ 5] =  768; aacpREV[ 2][ 6] = 1055; aacpREV[ 2][ 7] =  653; aacpREV[ 2][ 8] = 1405; aacpREV[ 2][ 9] =  168; 
	aacpREV[ 2][10] =  113; aacpREV[ 2][11] = 2430; aacpREV[ 2][12] =   61; aacpREV[ 2][13] =   97; aacpREV[ 2][14] =  173; 
	aacpREV[ 2][15] = 2085; aacpREV[ 2][16] = 1393; aacpREV[ 2][17] =   40; aacpREV[ 2][18] =  754; aacpREV[ 2][19] =   83; 
	aacpREV[ 3][ 0] =  175; aacpREV[ 3][ 1] =   43; aacpREV[ 3][ 2] = 4435; aacpREV[ 3][ 3] =    0; aacpREV[ 3][ 4] =   10; 
	aacpREV[ 3][ 5] =  400; aacpREV[ 3][ 6] = 3691; aacpREV[ 3][ 7] =  431; aacpREV[ 3][ 8] =  331; aacpREV[ 3][ 9] =   10; 
	aacpREV[ 3][10] =   10; aacpREV[ 3][11] =  412; aacpREV[ 3][12] =   47; aacpREV[ 3][13] =   22; aacpREV[ 3][14] =  170; 
	aacpREV[ 3][15] =  590; aacpREV[ 3][16] =  266; aacpREV[ 3][17] =   18; aacpREV[ 3][18] =  281; aacpREV[ 3][19] =   75; 
	aacpREV[ 4][ 0] =  669; aacpREV[ 4][ 1] =  823; aacpREV[ 4][ 2] =  538; aacpREV[ 4][ 3] =   10; aacpREV[ 4][ 4] =    0; 
	aacpREV[ 4][ 5] =   10; aacpREV[ 4][ 6] =   10; aacpREV[ 4][ 7] =  303; aacpREV[ 4][ 8] =  441; aacpREV[ 4][ 9] =  280; 
	aacpREV[ 4][10] =  396; aacpREV[ 4][11] =   48; aacpREV[ 4][12] =  159; aacpREV[ 4][13] =  726; aacpREV[ 4][14] =  285; 
	aacpREV[ 4][15] = 2331; aacpREV[ 4][16] =  576; aacpREV[ 4][17] =  435; aacpREV[ 4][18] = 1466; aacpREV[ 4][19] =  592; 
	aacpREV[ 5][ 0] =  157; aacpREV[ 5][ 1] = 1745; aacpREV[ 5][ 2] =  768; aacpREV[ 5][ 3] =  400; aacpREV[ 5][ 4] =   10; 
	aacpREV[ 5][ 5] =    0; aacpREV[ 5][ 6] = 3122; aacpREV[ 5][ 7] =  133; aacpREV[ 5][ 8] = 1269; aacpREV[ 5][ 9] =   92; 
	aacpREV[ 5][10] =  286; aacpREV[ 5][11] = 3313; aacpREV[ 5][12] =  202; aacpREV[ 5][13] =   10; aacpREV[ 5][14] =  323; 
	aacpREV[ 5][15] =  396; aacpREV[ 5][16] =  241; aacpREV[ 5][17] =   53; aacpREV[ 5][18] =  391; aacpREV[ 5][19] =   54; 
	aacpREV[ 6][ 0] =  499; aacpREV[ 6][ 1] =  152; aacpREV[ 6][ 2] = 1055; aacpREV[ 6][ 3] = 3691; aacpREV[ 6][ 4] =   10; 
	aacpREV[ 6][ 5] = 3122; aacpREV[ 6][ 6] =    0; aacpREV[ 6][ 7] =  379; aacpREV[ 6][ 8] =  162; aacpREV[ 6][ 9] =  148; 
	aacpREV[ 6][10] =   82; aacpREV[ 6][11] = 2629; aacpREV[ 6][12] =  113; aacpREV[ 6][13] =  145; aacpREV[ 6][14] =  185; 
	aacpREV[ 6][15] =  568; aacpREV[ 6][16] =  369; aacpREV[ 6][17] =   63; aacpREV[ 6][18] =  142; aacpREV[ 6][19] =  200; 
	aacpREV[ 7][ 0] =  665; aacpREV[ 7][ 1] =  243; aacpREV[ 7][ 2] =  653; aacpREV[ 7][ 3] =  431; aacpREV[ 7][ 4] =  303; 
	aacpREV[ 7][ 5] =  133; aacpREV[ 7][ 6] =  379; aacpREV[ 7][ 7] =    0; aacpREV[ 7][ 8] =   19; aacpREV[ 7][ 9] =   40; 
	aacpREV[ 7][10] =   20; aacpREV[ 7][11] =  263; aacpREV[ 7][12] =   21; aacpREV[ 7][13] =   25; aacpREV[ 7][14] =   28; 
	aacpREV[ 7][15] =  691; aacpREV[ 7][16] =   92; aacpREV[ 7][17] =   82; aacpREV[ 7][18] =   10; aacpREV[ 7][19] =   91; 
	aacpREV[ 8][ 0] =   66; aacpREV[ 8][ 1] =  715; aacpREV[ 8][ 2] = 1405; aacpREV[ 8][ 3] =  331; aacpREV[ 8][ 4] =  441; 
	aacpREV[ 8][ 5] = 1269; aacpREV[ 8][ 6] =  162; aacpREV[ 8][ 7] =   19; aacpREV[ 8][ 8] =    0; aacpREV[ 8][ 9] =   29; 
	aacpREV[ 8][10] =   66; aacpREV[ 8][11] =  305; aacpREV[ 8][12] =   10; aacpREV[ 8][13] =  127; aacpREV[ 8][14] =  152; 
	aacpREV[ 8][15] =  303; aacpREV[ 8][16] =   32; aacpREV[ 8][17] =   69; aacpREV[ 8][18] = 1971; aacpREV[ 8][19] =   25; 
	aacpREV[ 9][ 0] =  145; aacpREV[ 9][ 1] =  136; aacpREV[ 9][ 2] =  168; aacpREV[ 9][ 3] =   10; aacpREV[ 9][ 4] =  280; 
	aacpREV[ 9][ 5] =   92; aacpREV[ 9][ 6] =  148; aacpREV[ 9][ 7] =   40; aacpREV[ 9][ 8] =   29; aacpREV[ 9][ 9] =    0; 
	aacpREV[ 9][10] = 1745; aacpREV[ 9][11] =  345; aacpREV[ 9][12] = 1772; aacpREV[ 9][13] =  454; aacpREV[ 9][14] =  117; 
	aacpREV[ 9][15] =  216; aacpREV[ 9][16] = 1040; aacpREV[ 9][17] =   42; aacpREV[ 9][18] =   89; aacpREV[ 9][19] = 4797; 
	aacpREV[10][ 0] =  197; aacpREV[10][ 1] =  203; aacpREV[10][ 2] =  113; aacpREV[10][ 3] =   10; aacpREV[10][ 4] =  396; 
	aacpREV[10][ 5] =  286; aacpREV[10][ 6] =   82; aacpREV[10][ 7] =   20; aacpREV[10][ 8] =   66; aacpREV[10][ 9] = 1745; 
	aacpREV[10][10] =    0; aacpREV[10][11] =  218; aacpREV[10][12] = 1351; aacpREV[10][13] = 1268; aacpREV[10][14] =  219; 
	aacpREV[10][15] =  516; aacpREV[10][16] =  156; aacpREV[10][17] =  159; aacpREV[10][18] =  189; aacpREV[10][19] =  865; 
	aacpREV[11][ 0] =  236; aacpREV[11][ 1] = 4482; aacpREV[11][ 2] = 2430; aacpREV[11][ 3] =  412; aacpREV[11][ 4] =   48; 
	aacpREV[11][ 5] = 3313; aacpREV[11][ 6] = 2629; aacpREV[11][ 7] =  263; aacpREV[11][ 8] =  305; aacpREV[11][ 9] =  345; 
	aacpREV[11][10] =  218; aacpREV[11][11] =    0; aacpREV[11][12] =  193; aacpREV[11][13] =   72; aacpREV[11][14] =  302; 
	aacpREV[11][15] =  868; aacpREV[11][16] =  918; aacpREV[11][17] =   10; aacpREV[11][18] =  247; aacpREV[11][19] =  249; 
	aacpREV[12][ 0] =  185; aacpREV[12][ 1] =  125; aacpREV[12][ 2] =   61; aacpREV[12][ 3] =   47; aacpREV[12][ 4] =  159; 
	aacpREV[12][ 5] =  202; aacpREV[12][ 6] =  113; aacpREV[12][ 7] =   21; aacpREV[12][ 8] =   10; aacpREV[12][ 9] = 1772; 
	aacpREV[12][10] = 1351; aacpREV[12][11] =  193; aacpREV[12][12] =    0; aacpREV[12][13] =  327; aacpREV[12][14] =  100; 
	aacpREV[12][15] =   93; aacpREV[12][16] =  645; aacpREV[12][17] =   86; aacpREV[12][18] =  215; aacpREV[12][19] =  475; 
	aacpREV[13][ 0] =   68; aacpREV[13][ 1] =   53; aacpREV[13][ 2] =   97; aacpREV[13][ 3] =   22; aacpREV[13][ 4] =  726; 
	aacpREV[13][ 5] =   10; aacpREV[13][ 6] =  145; aacpREV[13][ 7] =   25; aacpREV[13][ 8] =  127; aacpREV[13][ 9] =  454; 
	aacpREV[13][10] = 1268; aacpREV[13][11] =   72; aacpREV[13][12] =  327; aacpREV[13][13] =    0; aacpREV[13][14] =   43; 
	aacpREV[13][15] =  487; aacpREV[13][16] =  148; aacpREV[13][17] =  468; aacpREV[13][18] = 2370; aacpREV[13][19] =  317; 
	aacpREV[14][ 0] =  490; aacpREV[14][ 1] =   87; aacpREV[14][ 2] =  173; aacpREV[14][ 3] =  170; aacpREV[14][ 4] =  285; 
	aacpREV[14][ 5] =  323; aacpREV[14][ 6] =  185; aacpREV[14][ 7] =   28; aacpREV[14][ 8] =  152; aacpREV[14][ 9] =  117; 
	aacpREV[14][10] =  219; aacpREV[14][11] =  302; aacpREV[14][12] =  100; aacpREV[14][13] =   43; aacpREV[14][14] =    0; 
	aacpREV[14][15] = 1202; aacpREV[14][16] =  260; aacpREV[14][17] =   49; aacpREV[14][18] =   97; aacpREV[14][19] =  122; 
	aacpREV[15][ 0] = 2440; aacpREV[15][ 1] =  385; aacpREV[15][ 2] = 2085; aacpREV[15][ 3] =  590; aacpREV[15][ 4] = 2331; 
	aacpREV[15][ 5] =  396; aacpREV[15][ 6] =  568; aacpREV[15][ 7] =  691; aacpREV[15][ 8] =  303; aacpREV[15][ 9] =  216; 
	aacpREV[15][10] =  516; aacpREV[15][11] =  868; aacpREV[15][12] =   93; aacpREV[15][13] =  487; aacpREV[15][14] = 1202; 
	aacpREV[15][15] =    0; aacpREV[15][16] = 2151; aacpREV[15][17] =   73; aacpREV[15][18] =  522; aacpREV[15][19] =  167; 
	aacpREV[16][ 0] = 1340; aacpREV[16][ 1] =  314; aacpREV[16][ 2] = 1393; aacpREV[16][ 3] =  266; aacpREV[16][ 4] =  576; 
	aacpREV[16][ 5] =  241; aacpREV[16][ 6] =  369; aacpREV[16][ 7] =   92; aacpREV[16][ 8] =   32; aacpREV[16][ 9] = 1040; 
	aacpREV[16][10] =  156; aacpREV[16][11] =  918; aacpREV[16][12] =  645; aacpREV[16][13] =  148; aacpREV[16][14] =  260; 
	aacpREV[16][15] = 2151; aacpREV[16][16] =    0; aacpREV[16][17] =   29; aacpREV[16][18] =   71; aacpREV[16][19] =  760; 
	aacpREV[17][ 0] =   14; aacpREV[17][ 1] =  230; aacpREV[17][ 2] =   40; aacpREV[17][ 3] =   18; aacpREV[17][ 4] =  435; 
	aacpREV[17][ 5] =   53; aacpREV[17][ 6] =   63; aacpREV[17][ 7] =   82; aacpREV[17][ 8] =   69; aacpREV[17][ 9] =   42; 
	aacpREV[17][10] =  159; aacpREV[17][11] =   10; aacpREV[17][12] =   86; aacpREV[17][13] =  468; aacpREV[17][14] =   49; 
	aacpREV[17][15] =   73; aacpREV[17][16] =   29; aacpREV[17][17] =    0; aacpREV[17][18] =  346; aacpREV[17][19] =   10; 
	aacpREV[18][ 0] =   56; aacpREV[18][ 1] =  323; aacpREV[18][ 2] =  754; aacpREV[18][ 3] =  281; aacpREV[18][ 4] = 1466; 
	aacpREV[18][ 5] =  391; aacpREV[18][ 6] =  142; aacpREV[18][ 7] =   10; aacpREV[18][ 8] = 1971; aacpREV[18][ 9] =   89; 
	aacpREV[18][10] =  189; aacpREV[18][11] =  247; aacpREV[18][12] =  215; aacpREV[18][13] = 2370; aacpREV[18][14] =   97; 
	aacpREV[18][15] =  522; aacpREV[18][16] =   71; aacpREV[18][17] =  346; aacpREV[18][18] =    0; aacpREV[18][19] =  119; 
	aacpREV[19][ 0] =  968; aacpREV[19][ 1] =   92; aacpREV[19][ 2] =   83; aacpREV[19][ 3] =   75; aacpREV[19][ 4] =  592; 
	aacpREV[19][ 5] =   54; aacpREV[19][ 6] =  200; aacpREV[19][ 7] =   91; aacpREV[19][ 8] =   25; aacpREV[19][ 9] = 4797; 
	aacpREV[19][10] =  865; aacpREV[19][11] =  249; aacpREV[19][12] =  475; aacpREV[19][13] =  317; aacpREV[19][14] =  122; 
	aacpREV[19][15] =  167; aacpREV[19][16] =  760; aacpREV[19][17] =   10; aacpREV[19][18] =  119; aacpREV[19][19] =    0; 

	cprevPi[0] = 0.076;
	cprevPi[1] = 0.062;
	cprevPi[2] = 0.041;
	cprevPi[3] = 0.037;
	cprevPi[4] = 0.009;
	cprevPi[5] = 0.038;
	cprevPi[6] = 0.049;
	cprevPi[7] = 0.084;
	cprevPi[8] = 0.025;
	cprevPi[9] = 0.081;
	cprevPi[10] = 0.101;
	cprevPi[11] = 0.050;
	cprevPi[12] = 0.022;
	cprevPi[13] = 0.051;
	cprevPi[14] = 0.043;
	cprevPi[15] = 0.062;
	cprevPi[16] = 0.054;
	cprevPi[17] = 0.018;
	cprevPi[18] = 0.031;
	cprevPi[19] = 0.066;
	
	/* VT model */
	aaVt[ 0][ 0] = 0.000000; aaVt[ 0][ 1] = 0.233108; aaVt[ 0][ 2] = 0.199097; aaVt[ 0][ 3] = 0.265145; aaVt[ 0][ 4] = 0.227333; 
	aaVt[ 0][ 5] = 0.310084; aaVt[ 0][ 6] = 0.567957; aaVt[ 0][ 7] = 0.876213; aaVt[ 0][ 8] = 0.078692; aaVt[ 0][ 9] = 0.222972; 
	aaVt[ 0][10] = 0.424630; aaVt[ 0][11] = 0.393245; aaVt[ 0][12] = 0.211550; aaVt[ 0][13] = 0.116646; aaVt[ 0][14] = 0.399143; 
	aaVt[ 0][15] = 1.817198; aaVt[ 0][16] = 0.877877; aaVt[ 0][17] = 0.030309; aaVt[ 0][18] = 0.087061; aaVt[ 0][19] = 1.230985; 
	aaVt[ 1][ 0] = 0.233108; aaVt[ 1][ 1] = 0.000000; aaVt[ 1][ 2] = 0.210797; aaVt[ 1][ 3] = 0.105191; aaVt[ 1][ 4] = 0.031726; 
	aaVt[ 1][ 5] = 0.493763; aaVt[ 1][ 6] = 0.255240; aaVt[ 1][ 7] = 0.156945; aaVt[ 1][ 8] = 0.213164; aaVt[ 1][ 9] = 0.081510; 
	aaVt[ 1][10] = 0.192364; aaVt[ 1][11] = 1.755838; aaVt[ 1][12] = 0.087930; aaVt[ 1][13] = 0.042569; aaVt[ 1][14] = 0.128480; 
	aaVt[ 1][15] = 0.292327; aaVt[ 1][16] = 0.204109; aaVt[ 1][17] = 0.046417; aaVt[ 1][18] = 0.097010; aaVt[ 1][19] = 0.113146; 
	aaVt[ 2][ 0] = 0.199097; aaVt[ 2][ 1] = 0.210797; aaVt[ 2][ 2] = 0.000000; aaVt[ 2][ 3] = 0.883422; aaVt[ 2][ 4] = 0.027495; 
	aaVt[ 2][ 5] = 0.275700; aaVt[ 2][ 6] = 0.270417; aaVt[ 2][ 7] = 0.362028; aaVt[ 2][ 8] = 0.290006; aaVt[ 2][ 9] = 0.087225; 
	aaVt[ 2][10] = 0.069245; aaVt[ 2][11] = 0.503060; aaVt[ 2][12] = 0.057420; aaVt[ 2][13] = 0.039769; aaVt[ 2][14] = 0.083956; 
	aaVt[ 2][15] = 0.847049; aaVt[ 2][16] = 0.471268; aaVt[ 2][17] = 0.010459; aaVt[ 2][18] = 0.093268; aaVt[ 2][19] = 0.049824; 
	aaVt[ 3][ 0] = 0.265145; aaVt[ 3][ 1] = 0.105191; aaVt[ 3][ 2] = 0.883422; aaVt[ 3][ 3] = 0.000000; aaVt[ 3][ 4] = 0.010313; 
	aaVt[ 3][ 5] = 0.205842; aaVt[ 3][ 6] = 1.599461; aaVt[ 3][ 7] = 0.311718; aaVt[ 3][ 8] = 0.134252; aaVt[ 3][ 9] = 0.011720; 
	aaVt[ 3][10] = 0.060863; aaVt[ 3][11] = 0.261101; aaVt[ 3][12] = 0.012182; aaVt[ 3][13] = 0.016577; aaVt[ 3][14] = 0.160063; 
	aaVt[ 3][15] = 0.461519; aaVt[ 3][16] = 0.178197; aaVt[ 3][17] = 0.011393; aaVt[ 3][18] = 0.051664; aaVt[ 3][19] = 0.048769; 
	aaVt[ 4][ 0] = 0.227333; aaVt[ 4][ 1] = 0.031726; aaVt[ 4][ 2] = 0.027495; aaVt[ 4][ 3] = 0.010313; aaVt[ 4][ 4] = 0.000000; 
	aaVt[ 4][ 5] = 0.004315; aaVt[ 4][ 6] = 0.005321; aaVt[ 4][ 7] = 0.050876; aaVt[ 4][ 8] = 0.016695; aaVt[ 4][ 9] = 0.046398; 
	aaVt[ 4][10] = 0.091709; aaVt[ 4][11] = 0.004067; aaVt[ 4][12] = 0.023690; aaVt[ 4][13] = 0.051127; aaVt[ 4][14] = 0.011137; 
	aaVt[ 4][15] = 0.175270; aaVt[ 4][16] = 0.079511; aaVt[ 4][17] = 0.007732; aaVt[ 4][18] = 0.042823; aaVt[ 4][19] = 0.163831; 
	aaVt[ 5][ 0] = 0.310084; aaVt[ 5][ 1] = 0.493763; aaVt[ 5][ 2] = 0.275700; aaVt[ 5][ 3] = 0.205842; aaVt[ 5][ 4] = 0.004315; 
	aaVt[ 5][ 5] = 0.000000; aaVt[ 5][ 6] = 0.960976; aaVt[ 5][ 7] = 0.128660; aaVt[ 5][ 8] = 0.315521; aaVt[ 5][ 9] = 0.054602; 
	aaVt[ 5][10] = 0.243530; aaVt[ 5][11] = 0.738208; aaVt[ 5][12] = 0.120801; aaVt[ 5][13] = 0.026235; aaVt[ 5][14] = 0.156570; 
	aaVt[ 5][15] = 0.358017; aaVt[ 5][16] = 0.248992; aaVt[ 5][17] = 0.021248; aaVt[ 5][18] = 0.062544; aaVt[ 5][19] = 0.112027; 
	aaVt[ 6][ 0] = 0.567957; aaVt[ 6][ 1] = 0.255240; aaVt[ 6][ 2] = 0.270417; aaVt[ 6][ 3] = 1.599461; aaVt[ 6][ 4] = 0.005321; 
	aaVt[ 6][ 5] = 0.960976; aaVt[ 6][ 6] = 0.000000; aaVt[ 6][ 7] = 0.250447; aaVt[ 6][ 8] = 0.104458; aaVt[ 6][ 9] = 0.046589; 
	aaVt[ 6][10] = 0.151924; aaVt[ 6][11] = 0.888630; aaVt[ 6][12] = 0.058643; aaVt[ 6][13] = 0.028168; aaVt[ 6][14] = 0.205134; 
	aaVt[ 6][15] = 0.406035; aaVt[ 6][16] = 0.321028; aaVt[ 6][17] = 0.018844; aaVt[ 6][18] = 0.055200; aaVt[ 6][19] = 0.205868; 
	aaVt[ 7][ 0] = 0.876213; aaVt[ 7][ 1] = 0.156945; aaVt[ 7][ 2] = 0.362028; aaVt[ 7][ 3] = 0.311718; aaVt[ 7][ 4] = 0.050876; 
	aaVt[ 7][ 5] = 0.128660; aaVt[ 7][ 6] = 0.250447; aaVt[ 7][ 7] = 0.000000; aaVt[ 7][ 8] = 0.058131; aaVt[ 7][ 9] = 0.051089; 
	aaVt[ 7][10] = 0.087056; aaVt[ 7][11] = 0.193243; aaVt[ 7][12] = 0.046560; aaVt[ 7][13] = 0.050143; aaVt[ 7][14] = 0.124492; 
	aaVt[ 7][15] = 0.612843; aaVt[ 7][16] = 0.136266; aaVt[ 7][17] = 0.023990; aaVt[ 7][18] = 0.037568; aaVt[ 7][19] = 0.082579; 
	aaVt[ 8][ 0] = 0.078692; aaVt[ 8][ 1] = 0.213164; aaVt[ 8][ 2] = 0.290006; aaVt[ 8][ 3] = 0.134252; aaVt[ 8][ 4] = 0.016695; 
	aaVt[ 8][ 5] = 0.315521; aaVt[ 8][ 6] = 0.104458; aaVt[ 8][ 7] = 0.058131; aaVt[ 8][ 8] = 0.000000; aaVt[ 8][ 9] = 0.020039; 
	aaVt[ 8][10] = 0.103552; aaVt[ 8][11] = 0.153323; aaVt[ 8][12] = 0.021157; aaVt[ 8][13] = 0.079807; aaVt[ 8][14] = 0.078892; 
	aaVt[ 8][15] = 0.167406; aaVt[ 8][16] = 0.101117; aaVt[ 8][17] = 0.020009; aaVt[ 8][18] = 0.286027; aaVt[ 8][19] = 0.068575; 
	aaVt[ 9][ 0] = 0.222972; aaVt[ 9][ 1] = 0.081510; aaVt[ 9][ 2] = 0.087225; aaVt[ 9][ 3] = 0.011720; aaVt[ 9][ 4] = 0.046398; 
	aaVt[ 9][ 5] = 0.054602; aaVt[ 9][ 6] = 0.046589; aaVt[ 9][ 7] = 0.051089; aaVt[ 9][ 8] = 0.020039; aaVt[ 9][ 9] = 0.000000; 
	aaVt[ 9][10] = 2.089890; aaVt[ 9][11] = 0.093181; aaVt[ 9][12] = 0.493845; aaVt[ 9][13] = 0.321020; aaVt[ 9][14] = 0.054797; 
	aaVt[ 9][15] = 0.081567; aaVt[ 9][16] = 0.376588; aaVt[ 9][17] = 0.034954; aaVt[ 9][18] = 0.086237; aaVt[ 9][19] = 3.654430; 
	aaVt[10][ 0] = 0.424630; aaVt[10][ 1] = 0.192364; aaVt[10][ 2] = 0.069245; aaVt[10][ 3] = 0.060863; aaVt[10][ 4] = 0.091709; 
	aaVt[10][ 5] = 0.243530; aaVt[10][ 6] = 0.151924; aaVt[10][ 7] = 0.087056; aaVt[10][ 8] = 0.103552; aaVt[10][ 9] = 2.089890; 
	aaVt[10][10] = 0.000000; aaVt[10][11] = 0.201204; aaVt[10][12] = 1.105667; aaVt[10][13] = 0.946499; aaVt[10][14] = 0.169784; 
	aaVt[10][15] = 0.214977; aaVt[10][16] = 0.243227; aaVt[10][17] = 0.083439; aaVt[10][18] = 0.189842; aaVt[10][19] = 1.337571; 
	aaVt[11][ 0] = 0.393245; aaVt[11][ 1] = 1.755838; aaVt[11][ 2] = 0.503060; aaVt[11][ 3] = 0.261101; aaVt[11][ 4] = 0.004067; 
	aaVt[11][ 5] = 0.738208; aaVt[11][ 6] = 0.888630; aaVt[11][ 7] = 0.193243; aaVt[11][ 8] = 0.153323; aaVt[11][ 9] = 0.093181; 
	aaVt[11][10] = 0.201204; aaVt[11][11] = 0.000000; aaVt[11][12] = 0.096474; aaVt[11][13] = 0.038261; aaVt[11][14] = 0.212302; 
	aaVt[11][15] = 0.400072; aaVt[11][16] = 0.446646; aaVt[11][17] = 0.023321; aaVt[11][18] = 0.068689; aaVt[11][19] = 0.144587; 
	aaVt[12][ 0] = 0.211550; aaVt[12][ 1] = 0.087930; aaVt[12][ 2] = 0.057420; aaVt[12][ 3] = 0.012182; aaVt[12][ 4] = 0.023690; 
	aaVt[12][ 5] = 0.120801; aaVt[12][ 6] = 0.058643; aaVt[12][ 7] = 0.046560; aaVt[12][ 8] = 0.021157; aaVt[12][ 9] = 0.493845; 
	aaVt[12][10] = 1.105667; aaVt[12][11] = 0.096474; aaVt[12][12] = 0.000000; aaVt[12][13] = 0.173052; aaVt[12][14] = 0.010363; 
	aaVt[12][15] = 0.090515; aaVt[12][16] = 0.184609; aaVt[12][17] = 0.022019; aaVt[12][18] = 0.073223; aaVt[12][19] = 0.307309; 
	aaVt[13][ 0] = 0.116646; aaVt[13][ 1] = 0.042569; aaVt[13][ 2] = 0.039769; aaVt[13][ 3] = 0.016577; aaVt[13][ 4] = 0.051127; 
	aaVt[13][ 5] = 0.026235; aaVt[13][ 6] = 0.028168; aaVt[13][ 7] = 0.050143; aaVt[13][ 8] = 0.079807; aaVt[13][ 9] = 0.321020; 
	aaVt[13][10] = 0.946499; aaVt[13][11] = 0.038261; aaVt[13][12] = 0.173052; aaVt[13][13] = 0.000000; aaVt[13][14] = 0.042564; 
	aaVt[13][15] = 0.138119; aaVt[13][16] = 0.085870; aaVt[13][17] = 0.128050; aaVt[13][18] = 0.898663; aaVt[13][19] = 0.247329; 
	aaVt[14][ 0] = 0.399143; aaVt[14][ 1] = 0.128480; aaVt[14][ 2] = 0.083956; aaVt[14][ 3] = 0.160063; aaVt[14][ 4] = 0.011137; 
	aaVt[14][ 5] = 0.156570; aaVt[14][ 6] = 0.205134; aaVt[14][ 7] = 0.124492; aaVt[14][ 8] = 0.078892; aaVt[14][ 9] = 0.054797; 
	aaVt[14][10] = 0.169784; aaVt[14][11] = 0.212302; aaVt[14][12] = 0.010363; aaVt[14][13] = 0.042564; aaVt[14][14] = 0.000000; 
	aaVt[14][15] = 0.430431; aaVt[14][16] = 0.207143; aaVt[14][17] = 0.014584; aaVt[14][18] = 0.032043; aaVt[14][19] = 0.129315; 
	aaVt[15][ 0] = 1.817198; aaVt[15][ 1] = 0.292327; aaVt[15][ 2] = 0.847049; aaVt[15][ 3] = 0.461519; aaVt[15][ 4] = 0.175270; 
	aaVt[15][ 5] = 0.358017; aaVt[15][ 6] = 0.406035; aaVt[15][ 7] = 0.612843; aaVt[15][ 8] = 0.167406; aaVt[15][ 9] = 0.081567; 
	aaVt[15][10] = 0.214977; aaVt[15][11] = 0.400072; aaVt[15][12] = 0.090515; aaVt[15][13] = 0.138119; aaVt[15][14] = 0.430431; 
	aaVt[15][15] = 0.000000; aaVt[15][16] = 1.767766; aaVt[15][17] = 0.035933; aaVt[15][18] = 0.121979; aaVt[15][19] = 0.127700; 
	aaVt[16][ 0] = 0.877877; aaVt[16][ 1] = 0.204109; aaVt[16][ 2] = 0.471268; aaVt[16][ 3] = 0.178197; aaVt[16][ 4] = 0.079511; 
	aaVt[16][ 5] = 0.248992; aaVt[16][ 6] = 0.321028; aaVt[16][ 7] = 0.136266; aaVt[16][ 8] = 0.101117; aaVt[16][ 9] = 0.376588; 
	aaVt[16][10] = 0.243227; aaVt[16][11] = 0.446646; aaVt[16][12] = 0.184609; aaVt[16][13] = 0.085870; aaVt[16][14] = 0.207143; 
	aaVt[16][15] = 1.767766; aaVt[16][16] = 0.000000; aaVt[16][17] = 0.020437; aaVt[16][18] = 0.094617; aaVt[16][19] = 0.740372; 
	aaVt[17][ 0] = 0.030309; aaVt[17][ 1] = 0.046417; aaVt[17][ 2] = 0.010459; aaVt[17][ 3] = 0.011393; aaVt[17][ 4] = 0.007732; 
	aaVt[17][ 5] = 0.021248; aaVt[17][ 6] = 0.018844; aaVt[17][ 7] = 0.023990; aaVt[17][ 8] = 0.020009; aaVt[17][ 9] = 0.034954; 
	aaVt[17][10] = 0.083439; aaVt[17][11] = 0.023321; aaVt[17][12] = 0.022019; aaVt[17][13] = 0.128050; aaVt[17][14] = 0.014584; 
	aaVt[17][15] = 0.035933; aaVt[17][16] = 0.020437; aaVt[17][17] = 0.000000; aaVt[17][18] = 0.124746; aaVt[17][19] = 0.022134; 
	aaVt[18][ 0] = 0.087061; aaVt[18][ 1] = 0.097010; aaVt[18][ 2] = 0.093268; aaVt[18][ 3] = 0.051664; aaVt[18][ 4] = 0.042823; 
	aaVt[18][ 5] = 0.062544; aaVt[18][ 6] = 0.055200; aaVt[18][ 7] = 0.037568; aaVt[18][ 8] = 0.286027; aaVt[18][ 9] = 0.086237; 
	aaVt[18][10] = 0.189842; aaVt[18][11] = 0.068689; aaVt[18][12] = 0.073223; aaVt[18][13] = 0.898663; aaVt[18][14] = 0.032043; 
	aaVt[18][15] = 0.121979; aaVt[18][16] = 0.094617; aaVt[18][17] = 0.124746; aaVt[18][18] = 0.000000; aaVt[18][19] = 0.125733; 
	aaVt[19][ 0] = 1.230985; aaVt[19][ 1] = 0.113146; aaVt[19][ 2] = 0.049824; aaVt[19][ 3] = 0.048769; aaVt[19][ 4] = 0.163831; 
	aaVt[19][ 5] = 0.112027; aaVt[19][ 6] = 0.205868; aaVt[19][ 7] = 0.082579; aaVt[19][ 8] = 0.068575; aaVt[19][ 9] = 3.654430; 
	aaVt[19][10] = 1.337571; aaVt[19][11] = 0.144587; aaVt[19][12] = 0.307309; aaVt[19][13] = 0.247329; aaVt[19][14] = 0.129315; 
	aaVt[19][15] = 0.127700; aaVt[19][16] = 0.740372; aaVt[19][17] = 0.022134; aaVt[19][18] = 0.125733; aaVt[19][19] = 0.000000; 

	vtPi[ 0] = 0.078837;
	vtPi[ 1] = 0.051238;
	vtPi[ 2] = 0.042313;
	vtPi[ 3] = 0.053066;
	vtPi[ 4] = 0.015175;
	vtPi[ 5] = 0.036713;
	vtPi[ 6] = 0.061924;
	vtPi[ 7] = 0.070852;
	vtPi[ 8] = 0.023082;
	vtPi[ 9] = 0.062056;
	vtPi[10] = 0.096371;
	vtPi[11] = 0.057324;
	vtPi[12] = 0.023771;
	vtPi[13] = 0.043296;
	vtPi[14] = 0.043911;
	vtPi[15] = 0.063403;
	vtPi[16] = 0.055897;
	vtPi[17] = 0.013272;
	vtPi[18] = 0.034399;
	vtPi[19] = 0.073101;
	
	/* Blosum62 */
	aaBlosum[ 0][ 0] = 0.000000000000; aaBlosum[ 0][ 1] = 0.735790389698; aaBlosum[ 0][ 2] = 0.485391055466; aaBlosum[ 0][ 3] = 0.543161820899; aaBlosum[ 0][ 4] = 1.459995310470; 
	aaBlosum[ 0][ 5] = 1.199705704602; aaBlosum[ 0][ 6] = 1.170949042800; aaBlosum[ 0][ 7] = 1.955883574960; aaBlosum[ 0][ 8] = 0.716241444998; aaBlosum[ 0][ 9] = 0.605899003687; 
	aaBlosum[ 0][10] = 0.800016530518; aaBlosum[ 0][11] = 1.295201266783; aaBlosum[ 0][12] = 1.253758266664; aaBlosum[ 0][13] = 0.492964679748; aaBlosum[ 0][14] = 1.173275900924; 
	aaBlosum[ 0][15] = 4.325092687057; aaBlosum[ 0][16] = 1.729178019485; aaBlosum[ 0][17] = 0.465839367725; aaBlosum[ 0][18] = 0.718206697586; aaBlosum[ 0][19] = 2.187774522005; 
	aaBlosum[ 1][ 0] = 0.735790389698; aaBlosum[ 1][ 1] = 0.000000000000; aaBlosum[ 1][ 2] = 1.297446705134; aaBlosum[ 1][ 3] = 0.500964408555; aaBlosum[ 1][ 4] = 0.227826574209; 
	aaBlosum[ 1][ 5] = 3.020833610064; aaBlosum[ 1][ 6] = 1.360574190420; aaBlosum[ 1][ 7] = 0.418763308518; aaBlosum[ 1][ 8] = 1.456141166336; aaBlosum[ 1][ 9] = 0.232036445142; 
	aaBlosum[ 1][10] = 0.622711669692; aaBlosum[ 1][11] = 5.411115141489; aaBlosum[ 1][12] = 0.983692987457; aaBlosum[ 1][13] = 0.371644693209; aaBlosum[ 1][14] = 0.448133661718; 
	aaBlosum[ 1][15] = 1.122783104210; aaBlosum[ 1][16] = 0.914665954563; aaBlosum[ 1][17] = 0.426382310122; aaBlosum[ 1][18] = 0.720517441216; aaBlosum[ 1][19] = 0.438388343772; 
	aaBlosum[ 2][ 0] = 0.485391055466; aaBlosum[ 2][ 1] = 1.297446705134; aaBlosum[ 2][ 2] = 0.000000000000; aaBlosum[ 2][ 3] = 3.180100048216; aaBlosum[ 2][ 4] = 0.397358949897; 
	aaBlosum[ 2][ 5] = 1.839216146992; aaBlosum[ 2][ 6] = 1.240488508640; aaBlosum[ 2][ 7] = 1.355872344485; aaBlosum[ 2][ 8] = 2.414501434208; aaBlosum[ 2][ 9] = 0.283017326278; 
	aaBlosum[ 2][10] = 0.211888159615; aaBlosum[ 2][11] = 1.593137043457; aaBlosum[ 2][12] = 0.648441278787; aaBlosum[ 2][13] = 0.354861249223; aaBlosum[ 2][14] = 0.494887043702; 
	aaBlosum[ 2][15] = 2.904101656456; aaBlosum[ 2][16] = 1.898173634533; aaBlosum[ 2][17] = 0.191482046247; aaBlosum[ 2][18] = 0.538222519037; aaBlosum[ 2][19] = 0.312858797993; 
	aaBlosum[ 3][ 0] = 0.543161820899; aaBlosum[ 3][ 1] = 0.500964408555; aaBlosum[ 3][ 2] = 3.180100048216; aaBlosum[ 3][ 3] = 0.000000000000; aaBlosum[ 3][ 4] = 0.240836614802; 
	aaBlosum[ 3][ 5] = 1.190945703396; aaBlosum[ 3][ 6] = 3.761625208368; aaBlosum[ 3][ 7] = 0.798473248968; aaBlosum[ 3][ 8] = 0.778142664022; aaBlosum[ 3][ 9] = 0.418555732462; 
	aaBlosum[ 3][10] = 0.218131577594; aaBlosum[ 3][11] = 1.032447924952; aaBlosum[ 3][12] = 0.222621897958; aaBlosum[ 3][13] = 0.281730694207; aaBlosum[ 3][14] = 0.730628272998; 
	aaBlosum[ 3][15] = 1.582754142065; aaBlosum[ 3][16] = 0.934187509431; aaBlosum[ 3][17] = 0.145345046279; aaBlosum[ 3][18] = 0.261422208965; aaBlosum[ 3][19] = 0.258129289418; 
	aaBlosum[ 4][ 0] = 1.459995310470; aaBlosum[ 4][ 1] = 0.227826574209; aaBlosum[ 4][ 2] = 0.397358949897; aaBlosum[ 4][ 3] = 0.240836614802; aaBlosum[ 4][ 4] = 0.000000000000; 
	aaBlosum[ 4][ 5] = 0.329801504630; aaBlosum[ 4][ 6] = 0.140748891814; aaBlosum[ 4][ 7] = 0.418203192284; aaBlosum[ 4][ 8] = 0.354058109831; aaBlosum[ 4][ 9] = 0.774894022794; 
	aaBlosum[ 4][10] = 0.831842640142; aaBlosum[ 4][11] = 0.285078800906; aaBlosum[ 4][12] = 0.767688823480; aaBlosum[ 4][13] = 0.441337471187; aaBlosum[ 4][14] = 0.356008498769; 
	aaBlosum[ 4][15] = 1.197188415094; aaBlosum[ 4][16] = 1.119831358516; aaBlosum[ 4][17] = 0.527664418872; aaBlosum[ 4][18] = 0.470237733696; aaBlosum[ 4][19] = 1.116352478606; 
	aaBlosum[ 5][ 0] = 1.199705704602; aaBlosum[ 5][ 1] = 3.020833610064; aaBlosum[ 5][ 2] = 1.839216146992; aaBlosum[ 5][ 3] = 1.190945703396; aaBlosum[ 5][ 4] = 0.329801504630; 
	aaBlosum[ 5][ 5] = 0.000000000000; aaBlosum[ 5][ 6] = 5.528919177928; aaBlosum[ 5][ 7] = 0.609846305383; aaBlosum[ 5][ 8] = 2.435341131140; aaBlosum[ 5][ 9] = 0.236202451204; 
	aaBlosum[ 5][10] = 0.580737093181; aaBlosum[ 5][11] = 3.945277674515; aaBlosum[ 5][12] = 2.494896077113; aaBlosum[ 5][13] = 0.144356959750; aaBlosum[ 5][14] = 0.858570575674; 
	aaBlosum[ 5][15] = 1.934870924596; aaBlosum[ 5][16] = 1.277480294596; aaBlosum[ 5][17] = 0.758653808642; aaBlosum[ 5][18] = 0.958989742850; aaBlosum[ 5][19] = 0.530785790125; 
	aaBlosum[ 6][ 0] = 1.170949042800; aaBlosum[ 6][ 1] = 1.360574190420; aaBlosum[ 6][ 2] = 1.240488508640; aaBlosum[ 6][ 3] = 3.761625208368; aaBlosum[ 6][ 4] = 0.140748891814; 
	aaBlosum[ 6][ 5] = 5.528919177928; aaBlosum[ 6][ 6] = 0.000000000000; aaBlosum[ 6][ 7] = 0.423579992176; aaBlosum[ 6][ 8] = 1.626891056982; aaBlosum[ 6][ 9] = 0.186848046932; 
	aaBlosum[ 6][10] = 0.372625175087; aaBlosum[ 6][11] = 2.802427151679; aaBlosum[ 6][12] = 0.555415397470; aaBlosum[ 6][13] = 0.291409084165; aaBlosum[ 6][14] = 0.926563934846; 
	aaBlosum[ 6][15] = 1.769893238937; aaBlosum[ 6][16] = 1.071097236007; aaBlosum[ 6][17] = 0.407635648938; aaBlosum[ 6][18] = 0.596719300346; aaBlosum[ 6][19] = 0.524253846338; 
	aaBlosum[ 7][ 0] = 1.955883574960; aaBlosum[ 7][ 1] = 0.418763308518; aaBlosum[ 7][ 2] = 1.355872344485; aaBlosum[ 7][ 3] = 0.798473248968; aaBlosum[ 7][ 4] = 0.418203192284; 
	aaBlosum[ 7][ 5] = 0.609846305383; aaBlosum[ 7][ 6] = 0.423579992176; aaBlosum[ 7][ 7] = 0.000000000000; aaBlosum[ 7][ 8] = 0.539859124954; aaBlosum[ 7][ 9] = 0.189296292376; 
	aaBlosum[ 7][10] = 0.217721159236; aaBlosum[ 7][11] = 0.752042440303; aaBlosum[ 7][12] = 0.459436173579; aaBlosum[ 7][13] = 0.368166464453; aaBlosum[ 7][14] = 0.504086599527; 
	aaBlosum[ 7][15] = 1.509326253224; aaBlosum[ 7][16] = 0.641436011405; aaBlosum[ 7][17] = 0.508358924638; aaBlosum[ 7][18] = 0.308055737035; aaBlosum[ 7][19] = 0.253340790190; 
	aaBlosum[ 8][ 0] = 0.716241444998; aaBlosum[ 8][ 1] = 1.456141166336; aaBlosum[ 8][ 2] = 2.414501434208; aaBlosum[ 8][ 3] = 0.778142664022; aaBlosum[ 8][ 4] = 0.354058109831; 
	aaBlosum[ 8][ 5] = 2.435341131140; aaBlosum[ 8][ 6] = 1.626891056982; aaBlosum[ 8][ 7] = 0.539859124954; aaBlosum[ 8][ 8] = 0.000000000000; aaBlosum[ 8][ 9] = 0.252718447885; 
	aaBlosum[ 8][10] = 0.348072209797; aaBlosum[ 8][11] = 1.022507035889; aaBlosum[ 8][12] = 0.984311525359; aaBlosum[ 8][13] = 0.714533703928; aaBlosum[ 8][14] = 0.527007339151; 
	aaBlosum[ 8][15] = 1.117029762910; aaBlosum[ 8][16] = 0.585407090225; aaBlosum[ 8][17] = 0.301248600780; aaBlosum[ 8][18] = 4.218953969389; aaBlosum[ 8][19] = 0.201555971750; 
	aaBlosum[ 9][ 0] = 0.605899003687; aaBlosum[ 9][ 1] = 0.232036445142; aaBlosum[ 9][ 2] = 0.283017326278; aaBlosum[ 9][ 3] = 0.418555732462; aaBlosum[ 9][ 4] = 0.774894022794; 
	aaBlosum[ 9][ 5] = 0.236202451204; aaBlosum[ 9][ 6] = 0.186848046932; aaBlosum[ 9][ 7] = 0.189296292376; aaBlosum[ 9][ 8] = 0.252718447885; aaBlosum[ 9][ 9] = 0.000000000000; 
	aaBlosum[ 9][10] = 3.890963773304; aaBlosum[ 9][11] = 0.406193586642; aaBlosum[ 9][12] = 3.364797763104; aaBlosum[ 9][13] = 1.517359325954; aaBlosum[ 9][14] = 0.388355409206; 
	aaBlosum[ 9][15] = 0.357544412460; aaBlosum[ 9][16] = 1.179091197260; aaBlosum[ 9][17] = 0.341985787540; aaBlosum[ 9][18] = 0.674617093228; aaBlosum[ 9][19] = 8.311839405458; 
	aaBlosum[10][ 0] = 0.800016530518; aaBlosum[10][ 1] = 0.622711669692; aaBlosum[10][ 2] = 0.211888159615; aaBlosum[10][ 3] = 0.218131577594; aaBlosum[10][ 4] = 0.831842640142; 
	aaBlosum[10][ 5] = 0.580737093181; aaBlosum[10][ 6] = 0.372625175087; aaBlosum[10][ 7] = 0.217721159236; aaBlosum[10][ 8] = 0.348072209797; aaBlosum[10][ 9] = 3.890963773304; 
	aaBlosum[10][10] = 0.000000000000; aaBlosum[10][11] = 0.445570274261; aaBlosum[10][12] = 6.030559379572; aaBlosum[10][13] = 2.064839703237; aaBlosum[10][14] = 0.374555687471; 
	aaBlosum[10][15] = 0.352969184527; aaBlosum[10][16] = 0.915259857694; aaBlosum[10][17] = 0.691474634600; aaBlosum[10][18] = 0.811245856323; aaBlosum[10][19] = 2.231405688913; 
	aaBlosum[11][ 0] = 1.295201266783; aaBlosum[11][ 1] = 5.411115141489; aaBlosum[11][ 2] = 1.593137043457; aaBlosum[11][ 3] = 1.032447924952; aaBlosum[11][ 4] = 0.285078800906; 
	aaBlosum[11][ 5] = 3.945277674515; aaBlosum[11][ 6] = 2.802427151679; aaBlosum[11][ 7] = 0.752042440303; aaBlosum[11][ 8] = 1.022507035889; aaBlosum[11][ 9] = 0.406193586642; 
	aaBlosum[11][10] = 0.445570274261; aaBlosum[11][11] = 0.000000000000; aaBlosum[11][12] = 1.073061184332; aaBlosum[11][13] = 0.266924750511; aaBlosum[11][14] = 1.047383450722; 
	aaBlosum[11][15] = 1.752165917819; aaBlosum[11][16] = 1.303875200799; aaBlosum[11][17] = 0.332243040634; aaBlosum[11][18] = 0.717993486900; aaBlosum[11][19] = 0.498138475304; 
	aaBlosum[12][ 0] = 1.253758266664; aaBlosum[12][ 1] = 0.983692987457; aaBlosum[12][ 2] = 0.648441278787; aaBlosum[12][ 3] = 0.222621897958; aaBlosum[12][ 4] = 0.767688823480; 
	aaBlosum[12][ 5] = 2.494896077113; aaBlosum[12][ 6] = 0.555415397470; aaBlosum[12][ 7] = 0.459436173579; aaBlosum[12][ 8] = 0.984311525359; aaBlosum[12][ 9] = 3.364797763104; 
	aaBlosum[12][10] = 6.030559379572; aaBlosum[12][11] = 1.073061184332; aaBlosum[12][12] = 0.000000000000; aaBlosum[12][13] = 1.773855168830; aaBlosum[12][14] = 0.454123625103; 
	aaBlosum[12][15] = 0.918723415746; aaBlosum[12][16] = 1.488548053722; aaBlosum[12][17] = 0.888101098152; aaBlosum[12][18] = 0.951682162246; aaBlosum[12][19] = 2.575850755315; 
	aaBlosum[13][ 0] = 0.492964679748; aaBlosum[13][ 1] = 0.371644693209; aaBlosum[13][ 2] = 0.354861249223; aaBlosum[13][ 3] = 0.281730694207; aaBlosum[13][ 4] = 0.441337471187; 
	aaBlosum[13][ 5] = 0.144356959750; aaBlosum[13][ 6] = 0.291409084165; aaBlosum[13][ 7] = 0.368166464453; aaBlosum[13][ 8] = 0.714533703928; aaBlosum[13][ 9] = 1.517359325954; 
	aaBlosum[13][10] = 2.064839703237; aaBlosum[13][11] = 0.266924750511; aaBlosum[13][12] = 1.773855168830; aaBlosum[13][13] = 0.000000000000; aaBlosum[13][14] = 0.233597909629; 
	aaBlosum[13][15] = 0.540027644824; aaBlosum[13][16] = 0.488206118793; aaBlosum[13][17] = 2.074324893497; aaBlosum[13][18] = 6.747260430801; aaBlosum[13][19] = 0.838119610178; 
	aaBlosum[14][ 0] = 1.173275900924; aaBlosum[14][ 1] = 0.448133661718; aaBlosum[14][ 2] = 0.494887043702; aaBlosum[14][ 3] = 0.730628272998; aaBlosum[14][ 4] = 0.356008498769; 
	aaBlosum[14][ 5] = 0.858570575674; aaBlosum[14][ 6] = 0.926563934846; aaBlosum[14][ 7] = 0.504086599527; aaBlosum[14][ 8] = 0.527007339151; aaBlosum[14][ 9] = 0.388355409206; 
	aaBlosum[14][10] = 0.374555687471; aaBlosum[14][11] = 1.047383450722; aaBlosum[14][12] = 0.454123625103; aaBlosum[14][13] = 0.233597909629; aaBlosum[14][14] = 0.000000000000; 
	aaBlosum[14][15] = 1.169129577716; aaBlosum[14][16] = 1.005451683149; aaBlosum[14][17] = 0.252214830027; aaBlosum[14][18] = 0.369405319355; aaBlosum[14][19] = 0.496908410676; 
	aaBlosum[15][ 0] = 4.325092687057; aaBlosum[15][ 1] = 1.122783104210; aaBlosum[15][ 2] = 2.904101656456; aaBlosum[15][ 3] = 1.582754142065; aaBlosum[15][ 4] = 1.197188415094; 
	aaBlosum[15][ 5] = 1.934870924596; aaBlosum[15][ 6] = 1.769893238937; aaBlosum[15][ 7] = 1.509326253224; aaBlosum[15][ 8] = 1.117029762910; aaBlosum[15][ 9] = 0.357544412460; 
	aaBlosum[15][10] = 0.352969184527; aaBlosum[15][11] = 1.752165917819; aaBlosum[15][12] = 0.918723415746; aaBlosum[15][13] = 0.540027644824; aaBlosum[15][14] = 1.169129577716; 
	aaBlosum[15][15] = 0.000000000000; aaBlosum[15][16] = 5.151556292270; aaBlosum[15][17] = 0.387925622098; aaBlosum[15][18] = 0.796751520761; aaBlosum[15][19] = 0.561925457442; 
	aaBlosum[16][ 0] = 1.729178019485; aaBlosum[16][ 1] = 0.914665954563; aaBlosum[16][ 2] = 1.898173634533; aaBlosum[16][ 3] = 0.934187509431; aaBlosum[16][ 4] = 1.119831358516; 
	aaBlosum[16][ 5] = 1.277480294596; aaBlosum[16][ 6] = 1.071097236007; aaBlosum[16][ 7] = 0.641436011405; aaBlosum[16][ 8] = 0.585407090225; aaBlosum[16][ 9] = 1.179091197260; 
	aaBlosum[16][10] = 0.915259857694; aaBlosum[16][11] = 1.303875200799; aaBlosum[16][12] = 1.488548053722; aaBlosum[16][13] = 0.488206118793; aaBlosum[16][14] = 1.005451683149; 
	aaBlosum[16][15] = 5.151556292270; aaBlosum[16][16] = 0.000000000000; aaBlosum[16][17] = 0.513128126891; aaBlosum[16][18] = 0.801010243199; aaBlosum[16][19] = 2.253074051176; 
	aaBlosum[17][ 0] = 0.465839367725; aaBlosum[17][ 1] = 0.426382310122; aaBlosum[17][ 2] = 0.191482046247; aaBlosum[17][ 3] = 0.145345046279; aaBlosum[17][ 4] = 0.527664418872; 
	aaBlosum[17][ 5] = 0.758653808642; aaBlosum[17][ 6] = 0.407635648938; aaBlosum[17][ 7] = 0.508358924638; aaBlosum[17][ 8] = 0.301248600780; aaBlosum[17][ 9] = 0.341985787540; 
	aaBlosum[17][10] = 0.691474634600; aaBlosum[17][11] = 0.332243040634; aaBlosum[17][12] = 0.888101098152; aaBlosum[17][13] = 2.074324893497; aaBlosum[17][14] = 0.252214830027; 
	aaBlosum[17][15] = 0.387925622098; aaBlosum[17][16] = 0.513128126891; aaBlosum[17][17] = 0.000000000000; aaBlosum[17][18] = 4.054419006558; aaBlosum[17][19] = 0.266508731426; 
	aaBlosum[18][ 0] = 0.718206697586; aaBlosum[18][ 1] = 0.720517441216; aaBlosum[18][ 2] = 0.538222519037; aaBlosum[18][ 3] = 0.261422208965; aaBlosum[18][ 4] = 0.470237733696; 
	aaBlosum[18][ 5] = 0.958989742850; aaBlosum[18][ 6] = 0.596719300346; aaBlosum[18][ 7] = 0.308055737035; aaBlosum[18][ 8] = 4.218953969389; aaBlosum[18][ 9] = 0.674617093228; 
	aaBlosum[18][10] = 0.811245856323; aaBlosum[18][11] = 0.717993486900; aaBlosum[18][12] = 0.951682162246; aaBlosum[18][13] = 6.747260430801; aaBlosum[18][14] = 0.369405319355; 
	aaBlosum[18][15] = 0.796751520761; aaBlosum[18][16] = 0.801010243199; aaBlosum[18][17] = 4.054419006558; aaBlosum[18][18] = 0.000000000000; aaBlosum[18][19] = 1.000000000000; 
	aaBlosum[19][ 0] = 2.187774522005; aaBlosum[19][ 1] = 0.438388343772; aaBlosum[19][ 2] = 0.312858797993; aaBlosum[19][ 3] = 0.258129289418; aaBlosum[19][ 4] = 1.116352478606; 
	aaBlosum[19][ 5] = 0.530785790125; aaBlosum[19][ 6] = 0.524253846338; aaBlosum[19][ 7] = 0.253340790190; aaBlosum[19][ 8] = 0.201555971750; aaBlosum[19][ 9] = 8.311839405458; 
	aaBlosum[19][10] = 2.231405688913; aaBlosum[19][11] = 0.498138475304; aaBlosum[19][12] = 2.575850755315; aaBlosum[19][13] = 0.838119610178; aaBlosum[19][14] = 0.496908410676; 
	aaBlosum[19][15] = 0.561925457442; aaBlosum[19][16] = 2.253074051176; aaBlosum[19][17] = 0.266508731426; aaBlosum[19][18] = 1.000000000000; aaBlosum[19][19] = 0.000000000000; 	

	blosPi[ 0] = 0.074; 
	blosPi[ 1] = 0.052; 
	blosPi[ 2] = 0.045; 
	blosPi[ 3] = 0.054;
	blosPi[ 4] = 0.025; 
	blosPi[ 5] = 0.034; 
	blosPi[ 6] = 0.054; 
	blosPi[ 7] = 0.074;
	blosPi[ 8] = 0.026; 
	blosPi[ 9] = 0.068; 
	blosPi[10] = 0.099; 
	blosPi[11] = 0.058;
	blosPi[12] = 0.025; 
	blosPi[13] = 0.047; 
	blosPi[14] = 0.039; 
	blosPi[15] = 0.057;
	blosPi[16] = 0.051; 
	blosPi[17] = 0.013; 
	blosPi[18] = 0.032; 
	blosPi[19] = 0.073;

	/* now, check that the matrices are symmetrical */
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			diff = aaJones[i][j] - aaJones[j][i];
			if (diff < 0.0)
				diff = -diff;
			if (diff > 0.001)
				{
				MrBayesPrint ("%s   ERROR: Jones model is not symmetrical.\n");
				return (ERROR);
				}
			}
		}
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			diff = aaDayhoff[i][j] - aaDayhoff[j][i];
			if (diff < 0.0)
				diff = -diff;
			if (diff > 0.001)
				{
				MrBayesPrint ("%s   ERROR: Dayhoff model is not symmetrical.\n");
				return (ERROR);
				}
			}
		}
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			diff = aaMtrev24[i][j] - aaMtrev24[j][i];
			if (diff < 0.0)
				diff = -diff;
			if (diff > 0.001)
				{
				MrBayesPrint ("%s   ERROR: mtrev24 model is not symmetrical.\n");
				return (ERROR);
				}
			}
		}
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			diff = aaMtmam[i][j] - aaMtmam[j][i];
			if (diff < 0.0)
				diff = -diff;
			if (diff > 0.001)
				{
				MrBayesPrint ("%s   ERROR: mtmam model is not symmetrical.\n");
				return (ERROR);
				}
			}
		}
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			diff = aartREV[i][j] - aartREV[j][i];
			if (diff < 0.0)
				diff = -diff;
			if (diff > 0.001)
				{
				MrBayesPrint ("%s   ERROR: aartREV model is not symmetrical.\n");
				return (ERROR);
				}
			}
		}
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			diff = aaWAG[i][j] - aaWAG[j][i];
			if (diff < 0.0)
				diff = -diff;
			if (diff > 0.001)
				{
				MrBayesPrint ("%s   ERROR: aaWAG model is not symmetrical.\n");
				return (ERROR);
				}
			}
		}
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			diff = aacpREV[i][j] - aacpREV[j][i];
			if (diff < 0.0)
				diff = -diff;
			if (diff > 0.001)
				{
				MrBayesPrint ("%s   ERROR: cpREV model is not symmetrical.\n");
				return (ERROR);
				}
			}
		}
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			diff = aaVt[i][j] - aaVt[j][i];
			if (diff < 0.0)
				diff = -diff;
			if (diff > 0.001)
				{
				MrBayesPrint ("%s   ERROR: Vt model is not symmetrical.\n");
				return (ERROR);
				}
			}
		}
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			diff = aaBlosum[i][j] - aaBlosum[j][i];
			if (diff < 0.0)
				diff = -diff;
			if (diff > 0.001)
				{
				MrBayesPrint ("%s   ERROR: Blosum model is not symmetrical.\n");
				return (ERROR);
				}
			}
		}
	
	/* rescale stationary frequencies, to make certain they sum to 1.0 */
	sum = 0.0;
	for (i=0; i<20; i++)
		sum += jonesPi[i];
	for (i=0; i<20; i++)
		jonesPi[i] /= sum;
	sum = 0.0;
	for (i=0; i<20; i++)
		sum += dayhoffPi[i];
	for (i=0; i<20; i++)
		dayhoffPi[i] /= sum;
	sum = 0.0;
	for (i=0; i<20; i++)
		sum += mtrev24Pi[i];
	for (i=0; i<20; i++)
		mtrev24Pi[i] /= sum;
	sum = 0.0;
	for (i=0; i<20; i++)
		sum += mtmamPi[i];
	for (i=0; i<20; i++)
		mtmamPi[i] /= sum;
	sum = 0.0;
	for (i=0; i<20; i++)
		sum += rtrevPi[i];
	for (i=0; i<20; i++)
		rtrevPi[i] /= sum;
	sum = 0.0;
	for (i=0; i<20; i++)
		sum += wagPi[i];
	for (i=0; i<20; i++)
		wagPi[i] /= sum;
	sum = 0.0;
	for (i=0; i<20; i++)
		sum += cprevPi[i];
	for (i=0; i<20; i++)
		cprevPi[i] /= sum;
	sum = 0.0;
	for (i=0; i<20; i++)
		sum += vtPi[i];
	for (i=0; i<20; i++)
		vtPi[i] /= sum;
	sum = 0.0;
	for (i=0; i<20; i++)
		sum += blosPi[i];
	for (i=0; i<20; i++)
		blosPi[i] /= sum;
		
	/* multiply entries by amino acid frequencies */
	for (i=0; i<20; i++)
		{
		for (j=0; j<20; j++)
			{
			aaJones[i][j]   *= jonesPi[j];
			aaDayhoff[i][j] *= dayhoffPi[j];
			aaMtrev24[i][j] *= mtrev24Pi[j];
			aaMtmam[i][j]   *= mtmamPi[j];
			aartREV[i][j]   *= rtrevPi[j];
			aaWAG[i][j]     *= wagPi[j];
			aacpREV[i][j]   *= cprevPi[j];
			aaVt[i][j]      *= vtPi[j];
			aaBlosum[i][j] *= blosPi[j];
			}
		}
		
	/* rescale, so branch lengths are in terms of expected number of
	   amino acid substitutions per site */
	scaler = 0.0;
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			scaler += jonesPi[i] * aaJones[i][j];
			scaler += jonesPi[j] * aaJones[j][i];
			}
		}
	scaler = 1.0 / scaler;
	for (i=0; i<20; i++)
		for (j=0; j<20; j++)
			aaJones[i][j] *= scaler;
	scaler = 0.0;
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			scaler += dayhoffPi[i] * aaDayhoff[i][j];
			scaler += dayhoffPi[j] * aaDayhoff[j][i];
			}
		}
	scaler = 1.0 / scaler;
	for (i=0; i<20; i++)
		for (j=0; j<20; j++)
			aaDayhoff[i][j] *= scaler;
	scaler = 0.0;
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			scaler += mtrev24Pi[i] * aaMtrev24[i][j];
			scaler += mtrev24Pi[j] * aaMtrev24[j][i];
			}
		}
	scaler = 1.0 / scaler;
	for (i=0; i<20; i++)
		for (j=0; j<20; j++)
			aaMtrev24[i][j] *= scaler;
	scaler = 0.0;
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			scaler += mtmamPi[i] * aaMtmam[i][j];
			scaler += mtmamPi[j] * aaMtmam[j][i];
			}
		}
	scaler = 1.0 / scaler;
	for (i=0; i<20; i++)
		for (j=0; j<20; j++)
			aaMtmam[i][j] *= scaler;
	scaler = 0.0;
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			scaler += rtrevPi[i] * aartREV[i][j];
			scaler += rtrevPi[j] * aartREV[j][i];
			}
		}
	scaler = 1.0 / scaler;
	for (i=0; i<20; i++)
		for (j=0; j<20; j++)
			aartREV[i][j] *= scaler;
	scaler = 0.0;
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			scaler += wagPi[i] * aaWAG[i][j];
			scaler += wagPi[j] * aaWAG[j][i];
			}
		}
	scaler = 1.0 / scaler;
	for (i=0; i<20; i++)
		for (j=0; j<20; j++)
			aaWAG[i][j] *= scaler;
	scaler = 0.0;
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			scaler += cprevPi[i] * aacpREV[i][j];
			scaler += cprevPi[j] * aacpREV[j][i];
			}
		}
	scaler = 1.0 / scaler;
	for (i=0; i<20; i++)
		for (j=0; j<20; j++)
			aacpREV[i][j] *= scaler;
	scaler = 0.0;
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			scaler += vtPi[i] * aaVt[i][j];
			scaler += vtPi[j] * aaVt[j][i];
			}
		}
	scaler = 1.0 / scaler;
	for (i=0; i<20; i++)
		for (j=0; j<20; j++)
			aaVt[i][j] *= scaler;
	scaler = 0.0;
	for (i=0; i<20; i++)
		{
		for (j=i+1; j<20; j++)
			{
			scaler += blosPi[i] * aaBlosum[i][j];
			scaler += blosPi[j] * aaBlosum[j][i];
			}
		}
	scaler = 1.0 / scaler;
	for (i=0; i<20; i++)
		for (j=0; j<20; j++)
			aaBlosum[i][j] *= scaler;
	
	/* set diagonal of matrix */
	for (i=0; i<20; i++)
		{
		sum = 0.0;
		for (j=0; j<20; j++)
			{
			if (i != j)
				sum += aaJones[i][j];
			}
		aaJones[i][i] = -sum;
		}
	for (i=0; i<20; i++)
		{
		sum = 0.0;
		for (j=0; j<20; j++)
			{
			if (i != j)
				sum += aaDayhoff[i][j];
			}
		aaDayhoff[i][i] = -sum;
		}
	for (i=0; i<20; i++)
		{
		sum = 0.0;
		for (j=0; j<20; j++)
			{
			if (i != j)
				sum += aaMtrev24[i][j];
			}
		aaMtrev24[i][i] = -sum;
		}
	for (i=0; i<20; i++)
		{
		sum = 0.0;
		for (j=0; j<20; j++)
			{
			if (i != j)
				sum += aaMtmam[i][j];
			}
		aaMtmam[i][i] = -sum;
		}
	for (i=0; i<20; i++)
		{
		sum = 0.0;
		for (j=0; j<20; j++)
			{
			if (i != j)
				sum += aartREV[i][j];
			}
		aartREV[i][i] = -sum;
		}
	for (i=0; i<20; i++)
		{
		sum = 0.0;
		for (j=0; j<20; j++)
			{
			if (i != j)
				sum += aaWAG[i][j];
			}
		aaWAG[i][i] = -sum;
		}
	for (i=0; i<20; i++)
		{
		sum = 0.0;
		for (j=0; j<20; j++)
			{
			if (i != j)
				sum += aacpREV[i][j];
			}
		aacpREV[i][i] = -sum;
		}
	for (i=0; i<20; i++)
		{
		sum = 0.0;
		for (j=0; j<20; j++)
			{
			if (i != j)
				sum += aaVt[i][j];
			}
		aaVt[i][i] = -sum;
		}
	for (i=0; i<20; i++)
		{
		sum = 0.0;
		for (j=0; j<20; j++)
			{
			if (i != j)
				sum += aaBlosum[i][j];
			}
		aaBlosum[i][i] = -sum;
		}

#	if 0
	for (i=0; i<20; i++)
		{
		MrBayesPrint ("%s   ", spacer);
		for (j=0; j<20; j++)
			{
			if (aaJones[i][j] < 0.0)
				MrBayesPrint ("%1.3lf ", aaJones[i][j]);
			else
				MrBayesPrint (" %1.3lf ", aaJones[i][j]);
			}
		MrBayesPrint ("\n");
		}
#	endif

	return (NO_ERROR);
	
}





void SetBit (int i, long int *bits)

{
	long		x;

	bits += i / nBitsInALong;

	x = 1 << (i % nBitsInALong);

	(*bits) |= x;
	
	return;
}





/*-----------------------------------------------------------------
|
|	SetChainParams: Set up parameter structs for values needed by
|		the chains, including trees
|
|----------------------------------------------------------------*/
int SetChainParams (void)

{

	int			isBaseRateNeeded, c, i, j, k, n, n1, n2, index, isPartTouched[MAX_NUM_DIVS],
				numRelParts, nRelParts, areAllPartsParsimony, clockSwitch;
	char		tempCodon[15], tempMult[15], temp[300], partString[100];
	Param		*p;
	ModelInfo	*m;
	ModelParams *mp;

#	if defined DEBUG_SETCHAINPARAMS
	/* only for debugging */
	MrBFlt		lnPriorRatio = 0.0, lnProposalRatio = 0.0;
#	endif

	/* wipe all chain information */
	numParams = 0;
	numTrees = 0;
	chainHasAdgamma = NO;
	inferSiteRates = NO;
	inferAncStates = NO;

	/* set model info */
	if (SetModelInfo() == ERROR)
		return (ERROR);

	/* figure out number of parameters */
	/* this relies on activeParams[j][i] being set to 1, 2, ..., numParams */
	nRelParts = 0;
	isBaseRateNeeded = NO;
	for (j=0; j<NUM_LINKED; j++)
		{
		for (i=0; i<numCurrentDivisions; i++)
			{
			if (activeParams[j][i] > numParams)
				numParams = activeParams[j][i];
			if (activeParams[j][i] > 0)
				nRelParts++;
			else if (j==P_RATEMULT)
				{
				/* no rate multiplier for this div, so base rate needed */
				nRelParts++;	/* some rate is always needed */
				isBaseRateNeeded = YES;	
				}
			}
		}

	/* add in a base rate parameter if needed */
	if (isBaseRateNeeded == YES)
		numParams++;

	/* allocate space for parameters */
	if (memAllocs[ALLOC_PARAMS] == YES)
		{
		MrBayesPrint ("%s   params or relevantParts not free in SetChainParams\n", spacer);
		return ERROR;
		}
	params = (Param *) malloc (numParams * sizeof(Param));
	relevantParts = (int *) malloc (nRelParts * sizeof(int));
	if (!params || !relevantParts)
		{
		MrBayesPrint ("%s   Problem allocating params and relevantParts\n", spacer);
		if (params)
			free (params);
		if (relevantParts)
			free (relevantParts);
		return ERROR;
		}
	else
		memAllocs[ALLOC_PARAMS] = YES;

	/* fill in info on each parameter */
	nRelParts = 0;	/* now cumulative number of relevant partitions */
	for (k=0; k<numParams; k++)
		{
		p = &params[k];

		/* find affected partitions */
		numRelParts = 0;
		for (j=0; j<NUM_LINKED; j++)
			{
			for (i=0; i<numCurrentDivisions; i++)
				{
				if (activeParams[j][i] == k + 1)
					{
					numRelParts++;
					isPartTouched[i] = YES;
					}
				else
					isPartTouched[i] = NO;
				}
			if (numRelParts > 0)
				break;
			}

		/* find pointer to modelParams of first relevant partition */
		/* this will be handy later on */
		for (i=0; i<numCurrentDivisions; i++)
			if (isPartTouched[i] == YES)
				break;
		mp = &modelParams[i];
		
		/* Parameter nValues and nSubValues, which are needed for memory allocation
		   are calculated for each case in the code below. nSympi, however, is
		   only used for one special type of parameter and it therefore makes
		   sense to initialize it to 0 here. */
		p->nSympi = 0;
		
		/* should this parameter be printed to a file? */
		p->printParam = NO;

		/* set index number of parameter */
		p->index = k;
		
		/* set up information for parameter */
		if (j == P_TRATIO)
			{
			/* Set up tratio ****************************************************************************************/
			p->paramType = P_TRATIO;
			p->nValues = 1;
			p->nSubValues = 0;
			p->nRelParts = numRelParts;
			p->relParts = relevantParts + nRelParts;
			nRelParts += numRelParts;
			for (i=n=0; i<numCurrentDivisions; i++)
				if (isPartTouched[i] == YES)
					{
					p->relParts[n++] = i;
					modelSettings[i].tRatio = p;
					}

			/* find the parameter x prior type */
			if (!strcmp(mp->tRatioPr,"Beta"))
				p->paramId = TRATIO_DIR;
			else
				p->paramId = TRATIO_FIX;
				
			if (p->paramId != TRATIO_FIX)
				p->printParam = YES;
			if (!strcmp(mp->tratioFormat,"Ratio"))
				{
				/* report ti/tv ratio */
				strcpy (p->paramName, "kappa");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				}
			else
				{
				/* report prop. of ratesum (Dirichlet) */
				strcpy (p->paramName, "ti");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				strcat (p->paramName, "\ttv");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				}
			}
		else if (j == P_REVMAT)
			{
			/* Set up revMat ****************************************************************************************/
			p->paramType = P_REVMAT;
			if (mp->dataType == PROTEIN)
				p->nValues = 190;
			else
				p->nValues = 6;
			p->nSubValues = 0;
			p->nRelParts = numRelParts;
			p->relParts = relevantParts + nRelParts;
			nRelParts += numRelParts;
			for (i=n=0; i<numCurrentDivisions; i++)
				if (isPartTouched[i] == YES)
					{
					p->relParts[n++] = i;
					modelSettings[i].revMat = p;
					}

			/* find the parameter x prior type */
			if (!strcmp(mp->revMatPr,"Dirichlet"))
				p->paramId = REVMAT_DIR;
			else
				p->paramId = REVMAT_FIX;

			if (p->paramId != REVMAT_FIX)
				p->printParam = YES;
			if (FillRelPartsString (p, partString) == YES)
				{
				if (mp->dataType == PROTEIN)
					{
					for (n1=0; n1<20; n1++)
						{
						for (n2=n1+1; n2<20; n2++)
							{
							if (n1==0 && n2==1)
								sprintf (p->paramName, "r(%c<->%c)%s", StateCode_AA(n1), StateCode_AA(n2), partString);
							else
								{
								sprintf (temp, "\tr(%c<->%c)", StateCode_AA(n1), StateCode_AA(n2));
								strcat (p->paramName, temp);
								strcat (p->paramName, partString);
								}
							}
						}
					}
				else
					sprintf (p->paramName, "r(A<->C)%s\tr(A<->G)%s\tr(A<->T)%s\tr(C<->G)%s\tr(C<->T)%s\tr(G<->T)%s",
						partString, partString, partString, partString, partString, partString);
				}
			else
				{
				if (mp->dataType == PROTEIN)
					{
					for (n1=0; n1<20; n1++)
						{
						for (n2=n1+1; n2<20; n2++)
							{
							if (n1==0 && n2==1)
								sprintf (p->paramName, "r(%c<->%c)", StateCode_AA(n1), StateCode_AA(n2));
							else
								{
								sprintf (temp, "\tr(%c<->%c)", StateCode_AA(n1), StateCode_AA(n2));
								strcat (p->paramName, temp);
								strcat (p->paramName, partString);
								}
							}
						}
					}
				else
					sprintf (p->paramName, "r(A<->C)\tr(A<->G)\tr(A<->T)\tr(C<->G)\tr(C<->T)\tr(G<->T)");
				}	
			}
		else if (j == P_OMEGA)
			{
			/* Set up omega *****************************************************************************************/
			p->paramType = P_OMEGA;
			if (!strcmp(mp->omegaVar, "M3"))
				{
				p->nValues = 3;
				
				p->nSubValues = 6;
				p->nRelParts = numRelParts;
				p->relParts = relevantParts + nRelParts;
				nRelParts += numRelParts;
				for (i=n=0; i<numCurrentDivisions; i++)
					if (isPartTouched[i] == YES)
						{
						p->relParts[n++] = i;
						modelSettings[i].omega = p;
						}
			
				/* find the parameter x prior type */
				if (     !strcmp(mp->m3omegapr, "Exponential") && !strcmp(mp->codonCatFreqPr, "Fixed"))
				    p->paramId = OMEGA_EF;
				else if (!strcmp(mp->m3omegapr, "Exponential") && !strcmp(mp->codonCatFreqPr, "Dirichlet"))
				    p->paramId = OMEGA_ED;
				else if (!strcmp(mp->m3omegapr, "Fixed")       && !strcmp(mp->codonCatFreqPr, "Fixed"))
				    p->paramId = OMEGA_FF;
				else if (!strcmp(mp->m3omegapr, "Fixed")       && !strcmp(mp->codonCatFreqPr, "Dirichlet"))
				    p->paramId = OMEGA_FD;

				if (p->paramId != OMEGA_FF)
					p->printParam = YES;
				strcpy (p->paramName, "omega(1)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				strcat (p->paramName, "\tomega(2)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				strcat (p->paramName, "\tomega(3)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
					
				strcat (p->paramName, "\tpi(1)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				strcat (p->paramName, "\tpi(2)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				strcat (p->paramName, "\tpi(3)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				}
			else if (!strcmp(mp->omegaVar, "M10"))
				{
				p->nValues = mp->numM10BetaCats + mp->numM10GammaCats;
				p->nSubValues = mp->numM10BetaCats + mp->numM10GammaCats + 8;
				p->nRelParts = numRelParts;
				p->relParts = relevantParts + nRelParts;
				nRelParts += numRelParts;
				for (i=n=0; i<numCurrentDivisions; i++)
					if (isPartTouched[i] == YES)
						{
						p->relParts[n++] = i;
						modelSettings[i].omega = p;
						}

				/* find the parameter x prior type */
				if (    !strcmp(mp->m10betapr, "Uniform")     && !strcmp(mp->m10gammapr, "Uniform")     && !strcmp(mp->codonCatFreqPr, "Dirichlet"))
				    p->paramId = OMEGA_10UUB;
				else if (!strcmp(mp->m10betapr, "Uniform")     && !strcmp(mp->m10gammapr, "Uniform")     && !strcmp(mp->codonCatFreqPr, "Fixed")    )
				    p->paramId = OMEGA_10UUF;
				else if (!strcmp(mp->m10betapr, "Uniform")     && !strcmp(mp->m10gammapr, "Exponential") && !strcmp(mp->codonCatFreqPr, "Dirichlet"))
				    p->paramId = OMEGA_10UEB;
				else if (!strcmp(mp->m10betapr, "Uniform")     && !strcmp(mp->m10gammapr, "Exponential") && !strcmp(mp->codonCatFreqPr, "Fixed")    )
				    p->paramId = OMEGA_10UEF;
				else if (!strcmp(mp->m10betapr, "Uniform")     && !strcmp(mp->m10gammapr, "Fixed")       && !strcmp(mp->codonCatFreqPr, "Dirichlet"))
				    p->paramId = OMEGA_10UFB;
				else if (!strcmp(mp->m10betapr, "Uniform")     && !strcmp(mp->m10gammapr, "Fixed")       && !strcmp(mp->codonCatFreqPr, "Fixed")    )
				    p->paramId = OMEGA_10UFF;
				else if (!strcmp(mp->m10betapr, "Exponential") && !strcmp(mp->m10gammapr, "Uniform")     && !strcmp(mp->codonCatFreqPr, "Dirichlet"))
				    p->paramId = OMEGA_10EUB;
				else if (!strcmp(mp->m10betapr, "Exponential") && !strcmp(mp->m10gammapr, "Uniform")     && !strcmp(mp->codonCatFreqPr, "Fixed")    )
				    p->paramId = OMEGA_10EUF;
				else if (!strcmp(mp->m10betapr, "Exponential") && !strcmp(mp->m10gammapr, "Exponential") && !strcmp(mp->codonCatFreqPr, "Dirichlet"))
				    p->paramId = OMEGA_10EEB;
				else if (!strcmp(mp->m10betapr, "Exponential") && !strcmp(mp->m10gammapr, "Exponential") && !strcmp(mp->codonCatFreqPr, "Fixed")    )
				    p->paramId = OMEGA_10EEF;
				else if (!strcmp(mp->m10betapr, "Exponential") && !strcmp(mp->m10gammapr, "Fixed")       && !strcmp(mp->codonCatFreqPr, "Dirichlet"))
				    p->paramId = OMEGA_10EFB;
				else if (!strcmp(mp->m10betapr, "Exponential") && !strcmp(mp->m10gammapr, "Fixed")       && !strcmp(mp->codonCatFreqPr, "Fixed")    )
				    p->paramId = OMEGA_10EFF;
				else if (!strcmp(mp->m10betapr, "Fixed")       && !strcmp(mp->m10gammapr, "Uniform")     && !strcmp(mp->codonCatFreqPr, "Dirichlet"))
				    p->paramId = OMEGA_10FUB;
				else if (!strcmp(mp->m10betapr, "Fixed")       && !strcmp(mp->m10gammapr, "Uniform")     && !strcmp(mp->codonCatFreqPr, "Fixed")    )
				    p->paramId = OMEGA_10FUF;
				else if (!strcmp(mp->m10betapr, "Fixed")       && !strcmp(mp->m10gammapr, "Exponential") && !strcmp(mp->codonCatFreqPr, "Dirichlet"))
				    p->paramId = OMEGA_10FEB;
				else if (!strcmp(mp->m10betapr, "Fixed")       && !strcmp(mp->m10gammapr, "Exponential") && !strcmp(mp->codonCatFreqPr, "Fixed")    )
				    p->paramId = OMEGA_10FEF;
				else if (!strcmp(mp->m10betapr, "Fixed")       && !strcmp(mp->m10gammapr, "Fixed")       && !strcmp(mp->codonCatFreqPr, "Dirichlet"))
				    p->paramId = OMEGA_10FFB;
				else if (!strcmp(mp->m10betapr, "Fixed")       && !strcmp(mp->m10gammapr, "Fixed")       && !strcmp(mp->codonCatFreqPr, "Fixed")    )
				    p->paramId = OMEGA_10FFF;

				if (p->paramId != OMEGA_10FFF)
					p->printParam = YES;
				for (i=0; i<p->nValues; i++)
					{
					sprintf (temp, "\tomega(%d)", i+1);
					strcat (p->paramName, temp);
					if (FillRelPartsString (p, partString) == YES)
						strcat (p->paramName, partString);
					}
				strcat (p->paramName, "\tbeta(alpha)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				strcat (p->paramName, "\tbeta(beta)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				strcat (p->paramName, "\tgamma(alpha)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				strcat (p->paramName, "\tgamma(beta)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				strcat (p->paramName, "\tpi(1)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				strcat (p->paramName, "\tpi(2)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				}
			else if (!strcmp(mp->omegaVar, "Ny98"))
				{
				p->nValues = 3;
				
				p->nSubValues = 6;
				p->nRelParts = numRelParts;
				p->relParts = relevantParts + nRelParts;
				nRelParts += numRelParts;
				for (i=n=0; i<numCurrentDivisions; i++)
					if (isPartTouched[i] == YES)
						{
						p->relParts[n++] = i;
						modelSettings[i].omega = p;
						}
			
				/* find the parameter x prior type */
				if (     !strcmp(mp->ny98omega1pr, "Beta")  && !strcmp(mp->ny98omega3pr, "Uniform")     && !strcmp(mp->codonCatFreqPr, "Dirichlet"))
				    p->paramId = OMEGA_BUD;
				else if (!strcmp(mp->ny98omega1pr, "Beta")  && !strcmp(mp->ny98omega3pr, "Uniform")     && !strcmp(mp->codonCatFreqPr, "Fixed"))
				    p->paramId = OMEGA_BUF;
				else if (!strcmp(mp->ny98omega1pr, "Beta")  && !strcmp(mp->ny98omega3pr, "Exponential") && !strcmp(mp->codonCatFreqPr, "Dirichlet"))
				    p->paramId = OMEGA_BED;
				else if (!strcmp(mp->ny98omega1pr, "Beta")  && !strcmp(mp->ny98omega3pr, "Exponential") && !strcmp(mp->codonCatFreqPr, "Fixed"))
				    p->paramId = OMEGA_BEF;
				else if (!strcmp(mp->ny98omega1pr, "Beta")  && !strcmp(mp->ny98omega3pr, "Fixed")       && !strcmp(mp->codonCatFreqPr, "Dirichlet"))
				    p->paramId = OMEGA_BFD;
				else if (!strcmp(mp->ny98omega1pr, "Beta")  && !strcmp(mp->ny98omega3pr, "Fixed")       && !strcmp(mp->codonCatFreqPr, "Fixed"))
				    p->paramId = OMEGA_BFF;
				else if (!strcmp(mp->ny98omega1pr, "Fixed") && !strcmp(mp->ny98omega3pr, "Uniform")     && !strcmp(mp->codonCatFreqPr, "Dirichlet"))
				    p->paramId = OMEGA_FUD;
				else if (!strcmp(mp->ny98omega1pr, "Fixed") && !strcmp(mp->ny98omega3pr, "Uniform")     && !strcmp(mp->codonCatFreqPr, "Fixed"))
				    p->paramId = OMEGA_FUF;
				else if (!strcmp(mp->ny98omega1pr, "Fixed") && !strcmp(mp->ny98omega3pr, "Exponential") && !strcmp(mp->codonCatFreqPr, "Dirichlet"))
				    p->paramId = OMEGA_FED;
				else if (!strcmp(mp->ny98omega1pr, "Fixed") && !strcmp(mp->ny98omega3pr, "Exponential") && !strcmp(mp->codonCatFreqPr, "Fixed"))
				    p->paramId = OMEGA_FEF;
				else if (!strcmp(mp->ny98omega1pr, "Fixed") && !strcmp(mp->ny98omega3pr, "Fixed")       && !strcmp(mp->codonCatFreqPr, "Dirichlet"))
				    p->paramId = OMEGA_FFD;
				else if (!strcmp(mp->ny98omega1pr, "Fixed") && !strcmp(mp->ny98omega3pr, "Fixed")       && !strcmp(mp->codonCatFreqPr, "Fixed"))
				    p->paramId = OMEGA_FFF;
				
				if (p->paramId != OMEGA_FFF)
					p->printParam = YES;
				strcpy (p->paramName, "omega(-)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				strcat (p->paramName, "\tomega(N)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				strcat (p->paramName, "\tomega(+)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
					
				strcat (p->paramName, "\tpi(-)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				strcat (p->paramName, "\tpi(N)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				strcat (p->paramName, "\tpi(+)");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				}
			else
				{
				p->nValues = 1;
				p->nSubValues = 0;
				p->nRelParts = numRelParts;
				p->relParts = relevantParts + nRelParts;
				nRelParts += numRelParts;
				for (i=n=0; i<numCurrentDivisions; i++)
					if (isPartTouched[i] == YES)
						{
						p->relParts[n++] = i;
						modelSettings[i].omega = p;
						}

				/* find the parameter x prior type */
				if (!strcmp(mp->omegaPr,"Dirichlet"))
					p->paramId = OMEGA_DIR;
				else
					p->paramId = OMEGA_FIX;

				if (p->paramId != OMEGA_FIX)
					p->printParam = YES;
				strcpy (p->paramName, "omega");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				}
			}
		else if (j == P_PI)
			{
			/* Set up state frequencies *****************************************************************************/
			p->paramType = P_PI;
			p->nRelParts = numRelParts;
			p->relParts = relevantParts + nRelParts;
			nRelParts += numRelParts;
			for (i=n=0; i<numCurrentDivisions; i++)
				{
				if (isPartTouched[i] == YES)
					{
					p->relParts[n++] = i;
					modelSettings[i].stateFreq = p;
					}
				}

			/* find the parameter x prior type */
			/* and the number of values and subvalues needed */
			if (mp->dataType == STANDARD)
				{
				/* symmetric hyperprior with only one variable (0 if equal) */
				p->nValues = 1;
				if (!strcmp(mp->symPiPr,"Uniform"))
					{
					for (i=0; i<p->nRelParts; i++)
						if (modelSettings[p->relParts[i]].nCijk > 0)
							break;
					if (i < p->nRelParts) 
						p->paramId = SYMPI_UNI_MS;
					else
						p->paramId = SYMPI_UNI;
					}
				else if (!strcmp(mp->symPiPr,"Exponential"))
					{
					for (i=0; i<p->nRelParts; i++)
						if (modelSettings[p->relParts[i]].nCijk > 0)
							break;
					if (i < p->nRelParts) 
						p->paramId = SYMPI_EXP_MS;
					else
						p->paramId = SYMPI_EXP;
					}
				else if (!strcmp(mp->symPiPr,"Fixed"))
					{
					if (AreDoublesEqual(mp->symBetaFix, -1.0, 0.00001) == YES)
						{
						p->paramId = SYMPI_EQUAL;
						p->nValues = 0;
						}
					else
						{
						for (i=0; i<p->nRelParts; i++)
							if (modelSettings[p->relParts[i]].nCijk > 0)
								break;
						if (i < p->nRelParts) 
							p->paramId = SYMPI_FIX_MS;
						else
							p->paramId = SYMPI_FIX;
						}
					}
				if (p->paramId == SYMPI_EQUAL)
					{
					/* calculate the number of state frequencies needed */
					/* also set bsIndex appropriately                   */
					for (n=index=0; n<9; n++)
						{
						for (i=0; i<p->nRelParts; i++)
							if (modelSettings[p->relParts[i]].isTiNeeded[n] == YES)
								break;
						if (i < p->nRelParts)
							{
							for (i=0; i<p->nRelParts; i++)
								{
								m = &modelSettings[p->relParts[i]];
								for (c=0; c<m->numChars; c++)
									{
									if (m->cType[c] != UNORD || m->nStates[c] > n + 2)
										{
										m->bsIndex[c] += (n + 2);
										}
									}
								}
							index += (n + 2);
							}
						}
					for (n=9; n<13; n++)
						{
						for (i=0; i<p->nRelParts; i++)
							if (modelSettings[p->relParts[i]].isTiNeeded[n] == YES)
								break;
						if (i < p->nRelParts)
							{
							for (i=0; i<p->nRelParts; i++)
								{
								m = &modelSettings[p->relParts[i]];
								for (c=0; c<m->numChars; c++)
									{
									if (m->cType[c] == ORD && m->nStates[c] > n - 6)
										{
										m->bsIndex[c] += (n - 6);
										}
									}
								}
							index += (n - 6);
							}
						}
					p->nSubValues = index;
					}
				else
					{
					/* now we need space for beta category frequencies */
					for (i=index=0; i<p->nRelParts; i++)
						if (modelSettings[p->relParts[i]].isTiNeeded[0] == YES)
							break;
					if (i < p->nRelParts) 
						index += (2 * modelSettings[p->relParts[0]].numBetaCats);
					/* as well as one set of frequencies for each multistate character */
					for (i=0; i<p->nRelParts; i++)
						{
						m = &modelSettings[p->relParts[i]];
						for (c=0; c<m->numChars; c++)
							{
							if (m->nStates[c] > 2 && (m->cType[c] == UNORD || m->cType[c] == ORD))
								{
								m->bsIndex[c] = index;
								index += m->nStates[c];
								p->nSympi++;
								}
							}
						}
					}
				p->nSubValues = index;
				if (p->paramId == SYMPI_EXP || p->paramId == SYMPI_EXP_MS || p->paramId == SYMPI_UNI || p->paramId == SYMPI_UNI_MS)
					p->printParam = YES;
				strcpy (p->paramName, "symdir");
				if (FillRelPartsString (p, partString) == YES)
					strcat (p->paramName, partString);
				}
			else
				{
				/* deal with all models except standard */
				/* no hyperprior or fixed to one value, set default to 0  */
				p->nValues = 0;
				/* one subvalue for each state */
				p->nSubValues = mp->nStates;
				if (!strcmp(mp->stateFreqPr, "Dirichlet"))
					{
					p->paramId = PI_DIR;
					p->nValues = mp->nStates;
					}
				else if (!strcmp(mp->stateFreqPr, "Fixed") && !strcmp(mp->stateFreqsFixType,"User"))
					p->paramId = PI_USER;
				else if (!strcmp(mp->stateFreqPr, "Fixed") && !strcmp(mp->stateFreqsFixType,"Empirical"))
					p->paramId = PI_EMPIRICAL;
				else if (!strcmp(mp->stateFreqPr, "Fixed") && !strcmp(mp->stateFreqsFixType,"Equal"))
					{
					p->paramId = PI_EQUAL;
					}
					
				if (mp->dataType == PROTEIN)
					{
					if (!strcmp(mp->aaModelPr, "Fixed"))
						{
						if (!strcmp(mp->aaModel, "Poisson"))
							p->paramId = PI_EQUAL;
						else if (!strcmp(mp->aaModel, "Equalin") || !strcmp(mp->aaModel, "Gtr"))
							{
							/* p->paramId stays to what it was set to above */
							}
						else
							p->paramId = PI_FIXED;
						}
					else
						p->paramId = PI_FIXED;
					}
					
				if (p->paramId == PI_DIR)
					p->printParam = YES;
				if (mp->dataType == DNA || mp->dataType == RNA)
					{
					if (!strcmp(mp->nucModel, "4by4"))
						{
						if (FillRelPartsString (p, partString) == YES)
							{
							sprintf (temp, "pi(A)%s\tpi(C)%s\tpi(G)%s\tpi(T)%s", partString, partString, partString, partString);
							strcpy (p->paramName, temp);
							}
						else
							strcpy (p->paramName, "pi(A)\tpi(C)\tpi(G)\tpi(T)");
						}
					else if (!strcmp(mp->nucModel, "Doublet"))
						{
						if (FillRelPartsString (p, partString) == YES)
							{
							sprintf (temp, "pi(AA)%s\tpi(AC)%s\tpi(AG)%s\tpi(AT)%s\tpi(CA)%s\tpi(CC)%s\tpi(CG)%s\tpi(CT)%s\tpi(GA)%s\tpi(GC)%s\tpi(GG)%s\tpi(GT)%s\tpi(TA)%s\tpi(TC)%s\tpi(TG)%s\tpi(TT)%s",
							partString, partString, partString, partString, partString, partString, partString, partString, partString, partString, partString, partString, partString, partString, partString, partString);
							strcpy (p->paramName, temp);
							}
						else
							strcpy (p->paramName, "pi(AA)\tpi(AC)\tpi(AG)\tpi(AT)\tpi(CA)\tpi(CC)\tpi(CG)\tpi(CT)\tpi(GA)\tpi(GC)\tpi(GG)\tpi(GT)\tpi(TA)\tpi(TC)\tpi(TG)\tpi(TT)");
						}
					else if (!strcmp(mp->nucModel, "Codon"))
						{
						for (c=0; c<p->nSubValues; c++)
							{
							if (mp->codonNucs[c][0] == 0)
								strcpy (tempCodon, "pi(A");
							else if (mp->codonNucs[c][0] == 1)
								strcpy (tempCodon, "pi(C");
							else if (mp->codonNucs[c][0] == 2)
								strcpy (tempCodon, "pi(G");
							else
								strcpy (tempCodon, "pi(T");
							if (mp->codonNucs[c][1] == 0)
								strcat (tempCodon, "A");
							else if (mp->codonNucs[c][1] == 1)
								strcat (tempCodon, "C");
							else if (mp->codonNucs[c][1] == 2)
								strcat (tempCodon, "G");
							else
								strcat (tempCodon, "T");
							if (mp->codonNucs[c][2] == 0)
								strcat (tempCodon, "A)");
							else if (mp->codonNucs[c][2] == 1)
								strcat (tempCodon, "C)");
							else if (mp->codonNucs[c][2] == 2)
								strcat (tempCodon, "G)");
							else
								strcat (tempCodon, "T)");
							if (FillRelPartsString (p, partString) == YES)
								strcat (tempCodon, partString);
							if (c == 0)
								strcpy (p->paramName, tempCodon);
							else
								{
								strcat (p->paramName, "\t");
								strcat (p->paramName, tempCodon);
								}
							}
						}
					}
				else if (mp->dataType == PROTEIN)
					{
					if (FillRelPartsString (p, partString) == YES)
						{
						sprintf (temp, "pi(Ala)%s\tpi(Arg)%s\tpi(Asn)%s\tpi(Asp)%s\tpi(Cys)%s\tpi(Gln)%s\tpi(Glu)%s\tpi(Gly)%s\tpi(His)%s\tpi(Ile)%s\tpi(Leu)%s\tpi(Lys)%s\tpi(Met)%s\tpi(Phe)%s\tpi(Pro)%s\tpi(Ser)%s\tpi(Thr)%s\tpi(Trp)%s\tpi(Tyr)%s\tpi(Val)%s",
						partString, partString, partString, partString, partString, partString, partString, partString, partString, partString,
						partString, partString, partString, partString, partString, partString, partString, partString, partString, partString);
						strcpy (p->paramName, temp);
						}
					else
						strcpy (p->paramName, "pi(Ala)\tpi(Arg)\tpi(Asn)\tpi(Asp)\tpi(Cys)\tpi(Gln)\tpi(Glu)\tpi(Gly)\tpi(His)\tpi(Ile)\tpi(Leu)\tpi(Lys)\tpi(Met)\tpi(Phe)\tpi(Pro)\tpi(Ser)\tpi(Thr)\tpi(Trp)\tpi(Tyr)\tpi(Val)");
					}
				else if (mp->dataType == RESTRICTION)
					{
					if (FillRelPartsString (p, partString) == YES)
						{
						sprintf (temp, "pi(0)%s\tpi(1)%s", partString, partString);
						strcpy (p->paramName, temp);
						}
					else
						strcpy (p->paramName, "pi(0)\tpi(1)");
#					if defined ASYMMETRY
					p->nSubValues = 4;
#					endif
					}
				else
					{
					MrBayesPrint ("%s   Unknown data type in SetChainParams\n", spacer);
					}
				}
			}
		else if (j == P_SHAPE)
			{
			/* Set up shape parameter of gamma **********************************************************************/
			p->paramType = P_SHAPE;
			p->nValues = 1;
			p->nSubValues = mp->numGammaCats;
			p->nRelParts = numRelParts;
			p->relParts = relevantParts + nRelParts;
			nRelParts += numRelParts;
			for (i=n=0; i<numCurrentDivisions; i++)
				if (isPartTouched[i] == YES)
					{
					p->relParts[n++] = i;
					modelSettings[i].shape = p;
					}

			/* find the parameter x prior type */
			mp = &modelParams[p->relParts[0]];
			if (!strcmp(mp->shapePr,"Uniform"))
				p->paramId = SHAPE_UNI;
			else if (!strcmp(mp->shapePr,"Exponential"))
				p->paramId = SHAPE_EXP;
			else
				p->paramId = SHAPE_FIX;
				
			if (p->paramId != SHAPE_FIX)
				p->printParam = YES;
			strcpy (p->paramName, "alpha");
			if (FillRelPartsString (p, partString) == YES)
				strcat (p->paramName, partString);
			}
		else if (j == P_PINVAR)
			{
			/* Set up proportion of invariable sites ****************************************************************/
			p->paramType = P_PINVAR;
			p->nValues = 1;
			p->nSubValues = 0;
			p->nRelParts = numRelParts;
			p->relParts = relevantParts + nRelParts;
			nRelParts += numRelParts;
			for (i=n=0; i<numCurrentDivisions; i++)
				if (isPartTouched[i] == YES)
					{
					p->relParts[n++] = i;
					modelSettings[i].pInvar = p;
					}

			/* find the parameter x prior type */
			if (!strcmp(mp->pInvarPr,"Uniform"))
				p->paramId = PINVAR_UNI;
			else
				p->paramId = PINVAR_FIX;
				
			if (p->paramId != PINVAR_FIX)
				p->printParam = YES;
			strcpy (p->paramName, "pinvar");
			if (FillRelPartsString (p, partString) == YES)
				strcat (p->paramName, partString);
			}
		else if (j == P_CORREL)
			{
			/* Set up correlation parameter of adgamma model ********************************************************/
			chainHasAdgamma = YES;
			p->paramType = P_CORREL;
			p->nValues = 1;
			p->nSubValues = mp->numGammaCats * mp->numGammaCats;
			p->nRelParts = numRelParts;
			p->relParts = relevantParts + nRelParts;
			nRelParts += numRelParts;
			for (i=n=0; i<numCurrentDivisions; i++)
				if (isPartTouched[i] == YES)
					{
					p->relParts[n++] = i;
					modelSettings[i].correlation = p;
					}

			/* find the parameter x prior type */
			if (!strcmp(mp->adGammaCorPr,"Uniform"))
				p->paramId = CORREL_UNI;
			else
				p->paramId = CORREL_FIX;

			if (p->paramId != CORREL_FIX)
				p->printParam = YES;
			strcpy (p->paramName, "rho");
			if (FillRelPartsString (p, partString) == YES)
				strcat (p->paramName, partString);
			}
		else if (j == P_SWITCH)
			{
			/* Set up switchRates for covarion model ****************************************************************/
			p->paramType = P_SWITCH;
			p->nValues = 2;
			p->nSubValues = mp->numGammaCats * mp->numGammaCats;
			p->nRelParts = numRelParts;
			p->relParts = relevantParts + nRelParts;
			nRelParts += numRelParts;
			for (i=n=0; i<numCurrentDivisions; i++)
				if (isPartTouched[i] == YES)
					{
					p->relParts[n++] = i;
					modelSettings[i].switchRates = p;
					}

			/* find the parameter x prior type */
			if (!strcmp(mp->covSwitchPr,"Uniform"))
				p->paramId = SWITCH_UNI;
			else if (!strcmp(mp->covSwitchPr,"Exponential"))
				p->paramId = SWITCH_EXP;
			else
				p->paramId = SWITCH_FIX;
				
			if (p->paramId != SWITCH_FIX)
				p->printParam = YES;
			if (FillRelPartsString (p, partString) == YES)
				{
				strcpy (p->paramName, "s(off->on)");
				strcat (p->paramName, partString);
				strcat (p->paramName, "\ts(on->off)");
				strcat (p->paramName, partString);
				}
			else
				strcpy (p->paramName, "s(off->on)\ts(on->off)");
			}
		else if (j == P_RATEMULT)
			{
			/* Set up rateMult for partition specific rates ***********************************************************/
			p->paramType = P_RATEMULT;
			p->nValues = p->nRelParts = numRelParts; /* keep scaled division rates in value                        */
			p->nSubValues = p->nValues * 3;          /* keep number of uncompressed chars for scaling in subValue  */
													 /* also keep rate ratios and priors here					   */
			p->relParts = relevantParts + nRelParts;
			nRelParts += numRelParts;
			for (i=n=0; i<numCurrentDivisions; i++)
				if (isPartTouched[i] == YES)
					{
					p->relParts[n++] = i;
					modelSettings[i].rateMult = p;
					}

			/* find the parameter x prior type */
			if (p->nValues == 1)
				p->paramId = RATEMULT_FIX;
			else
				p->paramId = RATEMULT_DIR;

			if (p->paramId != RATEMULT_FIX)
				p->printParam = YES;
			for (i=0; i<numCurrentDivisions; i++)
				{
				if (isPartTouched[i] == YES)
					{
					sprintf (tempMult, "m{%d}", i+1);
					if (i == 0)
						strcpy (p->paramName, tempMult);
					else
						{
						strcat (p->paramName, "\t");
						strcat (p->paramName, tempMult);
						}
					}
				}
			}
		else if (j == NUM_LINKED)
			{
			/* Set up base rate parameter ***************************************************************************/
			p->paramType = P_RATEMULT;
			p->nSubValues = 0;
			p->nValues = 1;
			p->nRelParts = 0;
			p->relParts = relevantParts + nRelParts;
			nRelParts += numRelParts;
			for (i=n=0; i<numCurrentDivisions; i++)
				if (modelSettings[i].rateMult==NULL)
					{
					p->relParts[n++] = i;
					p->nRelParts++;
					modelSettings[i].rateMult = p;
					}

			/* find the parameter x prior type */
			p->paramId = RATEMULT_FIX;
			}
		else if (j == P_TOPOLOGY)
			{
			/* Set up topology **************************************************************************************/
			p->paramType = P_TOPOLOGY;
			p->nValues = 0;
			p->nSubValues = 0;
			p->nRelParts = numRelParts;
			p->relParts = relevantParts + nRelParts;
			nRelParts += numRelParts;
			for (i=n=0; i<numCurrentDivisions; i++)
				if (isPartTouched[i] == YES)
					{
					p->relParts[n++] = i;
					modelSettings[i].topology = p;
					}
					
			/* check that the model is not parsimony for all of the relevant partitions */
			areAllPartsParsimony = YES;
			for (i=0; i<p->nRelParts; i++)
				{
				if (modelSettings[p->relParts[i]].parsModelId == NO)
					areAllPartsParsimony = NO;
				}

			/* check if topology is calibrated, clock or unconstrained */
			clockSwitch = 0;
			for (i=0; i<p->nRelParts; i++)
				{
				if (!strcmp(modelParams[p->relParts[i]].brlensPr, "Clock"))
					clockSwitch++;
				}

			/* find the parameter x prior type */
			if (areAllPartsParsimony == YES)
				{
				if (!strcmp(mp->topologyPr, "Uniform"))
					p->paramId = TOPOLOGY_PARSIMONY_UNIFORM;
				else
					p->paramId = TOPOLOGY_PARSIMONY_CONSTRAINED;
				/* For this case, we also need to set the brlens ptr of the relevant partitions
				   so that it points to the topology parameter, since the rest of the
				   program will try to access the tree through this pointer. In FillTreeParams,
				   we will make sure that a pure parsimony topology parameter contains a pointer
				   to the relevant tree (like a brlens parameter would normally) */
				for (i=0; i<p->nRelParts; i++)
					modelSettings[p->relParts[i]].brlens = p;
				}
			else
				{
				if (!strcmp(mp->topologyPr, "Uniform") && clockSwitch == 0)
					p->paramId = TOPOLOGY_NCL_UNIFORM;
				else if (!strcmp(mp->topologyPr,"Constraints") && clockSwitch == 0)
					p->paramId = TOPOLOGY_NCL_CONSTRAINED;
				else if (!strcmp(mp->topologyPr, "Uniform") && clockSwitch >= 1)
					{
					if (numCalibratedLocalTaxa > 0)
						p->paramId = TOPOLOGY_CCL_UNIFORM;
					else
						p->paramId = TOPOLOGY_CL_UNIFORM;
					}
				else if (!strcmp(mp->topologyPr,"Constraints") && clockSwitch >= 1)
					{
					if (numCalibratedLocalTaxa > 0)
						p->paramId = TOPOLOGY_CCL_CONSTRAINED;
					else
						p->paramId = TOPOLOGY_CL_CONSTRAINED;
					}
				}
			}
		else if (j == P_BRLENS)
			{
			/* Set up branch lengths ********************************************************************************/
			p->paramType = P_BRLENS;
			p->nValues = 0;
			p->nSubValues = 0;
			p->nRelParts = numRelParts;
			p->relParts = relevantParts + nRelParts;
			nRelParts += numRelParts;
			for (i=n=0; i<numCurrentDivisions; i++)
				if (isPartTouched[i] == YES)
					{
					p->relParts[n++] = i;
					modelSettings[i].brlens = p;
					}

			/* find the parameter x prior type */
			if (modelSettings[p->relParts[0]].parsModelId == YES)
				p->paramId = BRLENS_PARSIMONY;
			else
				{
				if (!strcmp(mp->brlensPr, "Clock"))
					{
					if (numCalibratedLocalTaxa > 0)
						{
						if (!strcmp(mp->clockPr,"Uniform"))
							p->paramId = BRLENS_CCLOCK_UNI;
						else if (!strcmp(mp->clockPr,"Coalescence"))
							p->paramId = BRLENS_CCLOCK_COAL;
						else if (!strcmp(mp->clockPr, "Birthdeath"))
							p->paramId = BRLENS_CCLOCK_BD;
						}
					else
						{
						if (!strcmp(mp->clockPr,"Uniform"))
							p->paramId = BRLENS_CLOCK_UNI;
						else if (!strcmp(mp->clockPr,"Coalescence"))
							p->paramId = BRLENS_CLOCK_COAL;
						else if (!strcmp(mp->clockPr, "Birthdeath"))
							p->paramId = BRLENS_CLOCK_BD;
						}
					}
				else if (!strcmp(mp->brlensPr, "Unconstrained"))
					{
					if (!strcmp(mp->unconstrainedPr,"Uniform"))
						p->paramId = BRLENS_UNI;
					else if (!strcmp(mp->unconstrainedPr,"Exponential"))
						p->paramId = BRLENS_EXP;
					}
				}
			}
		else if (j == P_SPECRATE)
			{
			/* Set up speciation rates ******************************************************************************/
			p->paramType = P_SPECRATE;
			p->nValues = 1;
			p->nSubValues = 0;
			p->nRelParts = numRelParts;
			p->relParts = relevantParts + nRelParts;
			nRelParts += numRelParts;
			for (i=n=0; i<numCurrentDivisions; i++)
				if (isPartTouched[i] == YES)
					{
					p->relParts[n++] = i;
					modelSettings[i].speciationRates = p;
					}

			/* find the parameter x prior type */
			if (!strcmp(mp->speciationPr,"Uniform"))
				p->paramId = SPECRATE_UNI;
			else if (!strcmp(mp->speciationPr,"Exponential"))
				p->paramId = SPECRATE_EXP;
			else
				p->paramId = SPECRATE_FIX;

			if (p->paramId != SPECRATE_FIX)
				p->printParam = YES;
			strcpy (p->paramName, "lambda");
			if (FillRelPartsString (p, partString) == YES)
				strcat (p->paramName, partString);
			}
		else if (j == P_EXTRATE)
			{
			/* Set up extinction rates ******************************************************************************/
			p->paramType = P_EXTRATE;
			p->nValues = 1;
			p->nSubValues = 0;
			p->nRelParts = numRelParts;
			p->relParts = relevantParts + nRelParts;
			nRelParts += numRelParts;
			for (i=n=0; i<numCurrentDivisions; i++)
				if (isPartTouched[i] == YES)
					{
					p->relParts[n++] = i;
					modelSettings[i].extinctionRates = p;
					}

			/* find the parameter x prior type */
			if (!strcmp(mp->extinctionPr,"Uniform"))
				p->paramId = EXTRATE_UNI;
			else if (!strcmp(mp->speciationPr,"Exponential"))
				p->paramId = EXTRATE_EXP;
			else
				p->paramId = EXTRATE_FIX;

			if (p->paramId != EXTRATE_FIX)
				p->printParam = YES;
			strcpy (p->paramName, "mu");
			if (FillRelPartsString (p, partString) == YES)
				strcat (p->paramName, partString);
			}
		else if (j == P_THETA)
			{
			/* Set up theta *****************************************************************************************/
			p->paramType = P_THETA;
			p->nValues = 1;
			p->nSubValues = 0;
			p->nRelParts = numRelParts;
			p->relParts = relevantParts + nRelParts;
			nRelParts += numRelParts;
			for (i=n=0; i<numCurrentDivisions; i++)
				if (isPartTouched[i] == YES)
					{
					p->relParts[n++] = i;
					modelSettings[i].theta = p;
					}

			/* find the parameter x prior type */
			if (!strcmp(mp->thetaPr,"Uniform"))
				p->paramId = THETA_UNI;
			else if (!strcmp(mp->thetaPr,"Exponential"))
				p->paramId = THETA_EXP;
			else
				p->paramId = THETA_FIX;

			if (p->paramId != THETA_FIX)
				p->printParam = YES;
			strcpy (p->paramName, "theta");
			if (FillRelPartsString (p, partString) == YES)
				strcat (p->paramName, partString);
			}
		else if (j == P_GROWTH)
			{
			/* Set up growth rate ************************************************************************************/
			p->paramType = P_GROWTH;
			p->nValues = 1;
			p->nSubValues = 0;
			p->nRelParts = numRelParts;
			p->relParts = relevantParts + nRelParts;
			nRelParts += numRelParts;
			for (i=n=0; i<numCurrentDivisions; i++)
				if (isPartTouched[i] == YES)
					{
					p->relParts[n++] = i;
					modelSettings[i].growthRate = p;
					}

			/* find the parameter x prior type */
			if (!strcmp(mp->growthPr,"Uniform"))
				p->paramId = GROWTH_UNI;
			else if (!strcmp(mp->growthPr,"Exponential"))
				p->paramId = GROWTH_EXP;
			else if (!strcmp(mp->growthPr,"Normal"))
				p->paramId = GROWTH_NORMAL;
			else
				p->paramId = GROWTH_FIX;

			if (p->paramId != GROWTH_FIX)
				p->printParam = YES;
			strcpy (p->paramName, "growthRate");
			if (FillRelPartsString (p, partString) == YES)
				strcat (p->paramName, partString);
			}
		else if (j == P_AAMODEL)
			{
			/* Set up aamodel *****************************************************************************************/
			p->paramType = P_AAMODEL;
			p->nValues = 1;
			p->nSubValues = 10;
			p->nRelParts = numRelParts;
			p->relParts = relevantParts + nRelParts;
			nRelParts += numRelParts;
			for (i=n=0; i<numCurrentDivisions; i++)
				if (isPartTouched[i] == YES)
					{
					p->relParts[n++] = i;
					modelSettings[i].aaModel = p;
					}

			/* find the parameter x prior type */
			if (!strcmp(mp->aaModelPr,"Mixed"))
				p->paramId = AAMODEL_MIX;
			else
				p->paramId = AAMODEL_FIX;

			if (p->paramId != AAMODEL_FIX)
				p->printParam = YES;
			strcpy (p->paramName, "aamodel");
			if (FillRelPartsString (p, partString) == YES)
				strcat (p->paramName, partString);
			}

		}
		
	return (NO_ERROR);	
}





/*----------------------------------------------------------------------------
|
|	SetLikeFunctions: This function will set up the pointers from each
|		data division to the relevant likelihood functions
|
-----------------------------------------------------------------------------*/
int SetLikeFunctions (void)

{
	
	int			i;

	ModelInfo	*m;


	/* couple divisions with likelihood functions */
	for (i=0; i<numCurrentDivisions; i++)
		{
		m = &modelSettings[i];
		
		if (m->dataType == DNA || m->dataType == RNA)
			{
			if (m->parsModelId == YES)
				{
				m->Likelihood = &Likelihood_Pars;
				}
			else
				{
				if (m->nucModelId == NUCMODEL_4BY4)
					{
					if (m->numModelStates > 4)
						{
						/* covariotide model */
						/* TODO: allow autocorrelated rates */
						m->CondLikeDown = &CondLikeDown_Gen;
						m->CondLikeRoot = &CondLikeRoot_Gen;
						m->CondLikeScaler = &CondLikeScaler_Gen;
						if (m->correlation != NULL)
							m->Likelihood = &Likelihood_Adgamma;
						else
							m->Likelihood = &Likelihood_Gen;
						if (m->nCijkParts == 1)
							m->TiProbs = &TiProbs_Gen;
						else if (m->nCijkParts > 1)
							m->TiProbs = &TiProbs_GenCov;
						}
					else
						{
#if defined SSE
						m->CondLikeDown = &CondLikeDown_NUC4_SSE;
						m->CondLikeRoot = &CondLikeRoot_NUC4_SSE;
#if defined FAST_LOG
						m->CondLikeScaler = &CondLikeScaler_NUC4_fast;
#else
						m->CondLikeScaler = &CondLikeScaler_NUC4;
#endif
#else
						m->CondLikeDown = &CondLikeDown_NUC4;
						m->CondLikeRoot = &CondLikeRoot_NUC4;
#if defined FAST_LOG
						m->CondLikeScaler = &CondLikeScaler_NUC4_fast;
#else
						m->CondLikeScaler = &CondLikeScaler_NUC4;
#endif
#endif
						if (m->correlation != NULL)
							m->Likelihood = &Likelihood_Adgamma;
						else
							m->Likelihood = &Likelihood_NUC4;
						if (m->nst == 1)
							m->TiProbs = &TiProbs_Fels;
						else if (m->nst == 2)
							m->TiProbs = &TiProbs_Hky;
						else
							m->TiProbs = &TiProbs_Gen;
						m->CondLikeUp = &CondLikeUp_NUC4;
						m->StateCode = &StateCode_NUC4;
						m->PrintAncStates = &PrintAncStates_NUC4;
						m->PrintSiteRates = &PrintSiteRates_Gen;
						}
					}
				else if (m->nucModelId == NUCMODEL_DOUBLET)
					{
					m->CondLikeDown = &CondLikeDown_Gen;
					m->CondLikeRoot = &CondLikeRoot_Gen;
					m->CondLikeScaler = &CondLikeScaler_Gen;
					m->Likelihood = &Likelihood_Gen;
					if (m->nst == 1)
						m->TiProbs = &TiProbs_Gen;
					else if (m->nst == 2)
						m->TiProbs = &TiProbs_Gen;
					else
						m->TiProbs = &TiProbs_Gen;
					}
				else
					{
					/* codon models */
					if (m->numOmegaCats == 1)
						{
						m->CondLikeDown   = &CondLikeDown_Gen;
						m->CondLikeRoot   = &CondLikeRoot_Gen;
						m->CondLikeScaler = &CondLikeScaler_Gen;
						m->Likelihood     = &Likelihood_Gen;
						}
					else
						{
						m->CondLikeDown   = &CondLikeDown_NY98;
						m->CondLikeRoot   = &CondLikeRoot_NY98;
						m->CondLikeScaler = &CondLikeScaler_NY98;
						m->Likelihood     = &Likelihood_NY98;
						}
					m->TiProbs        = &TiProbs_Gen;
					if (m->nCijkParts > 1)
						m->TiProbs = &TiProbs_GenCov;
					}
				}
			}
		else if (m->dataType == PROTEIN)
			{
			if (m->parsModelId == YES)
				{
				m->Likelihood = &Likelihood_Pars;
				}
			else
				{
				/* TODO:allow autocorrelated rates for covarion model */
				m->CondLikeDown   = &CondLikeDown_Gen;
				m->CondLikeRoot   = &CondLikeRoot_Gen;
				m->CondLikeScaler = &CondLikeScaler_Gen;
				if (m->correlation != NULL)
					m->Likelihood = &Likelihood_Adgamma;
				else
					m->Likelihood = &Likelihood_Gen;
				m->TiProbs        = &TiProbs_Gen;
				if (m->numModelStates > 20 && m->nCijkParts == 1)
					m->TiProbs = &TiProbs_Gen;
				else if (m->numModelStates > 20 && m->nCijkParts > 1)
					m->TiProbs = &TiProbs_GenCov;
				m->CondLikeUp = &CondLikeUp_Gen;
				m->StateCode = &StateCode_AA;
				m->PrintAncStates = &PrintAncStates_Gen;
				m->PrintSiteRates = &PrintSiteRates_Gen;
				}
			}
		else if (m->dataType == RESTRICTION)
			{
			if (m->parsModelId == YES)
				{
				m->Likelihood = &Likelihood_Pars;
				}
			else
				{
				m->CondLikeDown   = &CondLikeDown_Bin;
				m->CondLikeRoot   = &CondLikeRoot_Bin;
				m->CondLikeScaler = &CondLikeScaler_Gen;
				m->Likelihood     = &Likelihood_Res;
				m->TiProbs        = &TiProbs_Res;
				m->CondLikeUp = &CondLikeUp_Bin;
				m->StateCode = &StateCode_Std;
				m->PrintAncStates = &PrintAncStates_Bin;
				m->PrintSiteRates = &PrintSiteRates_Gen;
				}
			}
		else if (m->dataType == STANDARD)
			{
			if (m->parsModelId == YES)
				{
				if (m->numModelStates == 2)
					{
					m->Likelihood = &Likelihood_Pars; /* this is much faster if number of states do not vary */
					m->numStates = 2;	/* this is needed for the parsimony calculator */
					}
				else
					m->Likelihood = &Likelihood_ParsStd;
				}
			else
				{
				m->CondLikeDown   = &CondLikeDown_Std;
				m->CondLikeRoot   = &CondLikeRoot_Std;
				m->CondLikeScaler = &CondLikeScaler_Std;
				m->Likelihood     = &Likelihood_Std;
				m->TiProbs        = &TiProbs_Std;
				m->CondLikeUp	  = &CondLikeUp_Std;
				m->StateCode	  = &StateCode_Std;
				m->PrintAncStates = &PrintAncStates_Std;
				m->PrintSiteRates = &PrintSiteRates_Std;
				}
			}		
		else if (m->dataType == CONTINUOUS)
			{
			
			}		
		else
			{
			MrBayesPrint ("%s   ERROR: Data should be one of these types!\n", spacer);
			return ERROR;
			}
		
		}

	return NO_ERROR;

}





/*----------------------------------------------------------------------------
|
|	SetModelInfo: This function will set up model info using model
|		params
|
-----------------------------------------------------------------------------*/
int SetModelInfo (void)

{

	int				c, i, j, chn, ts;
	ModelParams		*mp;
	ModelInfo		*m;
	
	/* wipe all model settings */
	for (i=0; i<MAX_NUM_DIVS; i++)
		{
		m = &modelSettings[i];

		/* make certain that we set this to "NO" so we 
		   calculate cijk information when needed */
		for (j=0; j<MAX_CHAINS; j++)
			m->upDateCijk[j][0] = m->upDateCijk[j][1] = YES;

		/* make certain that we start with a parsimony branch length of zero */
		for (j=0; j<MAX_CHAINS; j++)
			m->parsTreeLength[j*2] = m->parsTreeLength[j*2+1] = 0.0;

		m->tRatio = NULL;
		m->revMat = NULL;
		m->omega = NULL;
		m->stateFreq = NULL;
		m->shape = NULL;
		m->pInvar = NULL;
		m->correlation = NULL;
		m->switchRates = NULL;
		m->rateMult = NULL;
		m->topology = NULL;
		m->brlens = NULL;
		m->speciationRates = NULL;
		m->extinctionRates = NULL;
		m->theta = NULL;
		m->aaModel = NULL;

		m->CondLikeDown = NULL;
		m->CondLikeRoot = NULL;
		m->CondLikeScaler = NULL;
		m->Likelihood = NULL;
		m->TiProbs = NULL;

		m->CondLikeUp = NULL;
		m->StateCode = NULL;
		m->PrintAncStates = NULL;
		m->PrintSiteRates = NULL;
		
		m->printPosSel = NO;
		m->printAncStates = NO;
		m->printSiteRates = NO;

		m->parsimonyBasedMove = NO;
		}

	/* set state of all chains to zero */
	for (chn=0; chn<numLocalChains; chn++)
		state[chn] = 0;

	/* fill in modelSettings info with some basic model characteristics */
	for (i=0; i<numCurrentDivisions; i++)
		{
		mp = &modelParams[i];
		m = &modelSettings[i];
		
		m->dataType = mp->dataType;

		/* nuc model structure */
		if (!strcmp(mp->nucModel, "4by4"))
			m->nucModelId = NUCMODEL_4BY4;
		else if (!strcmp(mp->nucModel, "Doublet"))
			m->nucModelId = NUCMODEL_DOUBLET;
		else
			m->nucModelId = NUCMODEL_CODON;
			
		/* model nst */
		if (!strcmp(mp->nst, "1"))
			m->nst = 1;
		else if (!strcmp(mp->nst, "2"))
			m->nst = 2;
		else if (!strcmp(mp->nst, "6"))
			m->nst = 6;
		else
			m->nst = 203;
			
		/* We set the aa model here. We have two options. First, the model
		   could be fixed, in which case mp->aaModel has been set. We then
		   go ahead and also set the model settings. Second, the model
		   could be mixed. In this case, the amino acid matrix is considered
		   a parameter, and we will deal with it below. It doesn't hurt
		   to set it here anyway (it will be overwritten later). */
		if (!strcmp(mp->aaModelPr, "Fixed"))
			{
			if (!strcmp(mp->aaModel, "Poisson"))
				m->aaModelId = AAMODEL_POISSON;
			else if (!strcmp(mp->aaModel, "Equalin"))
				m->aaModelId = AAMODEL_EQ;
			else if (!strcmp(mp->aaModel, "Jones"))
				m->aaModelId = AAMODEL_JONES;
			else if (!strcmp(mp->aaModel, "Dayhoff"))
				m->aaModelId = AAMODEL_DAY;
			else if (!strcmp(mp->aaModel, "Mtrev"))
				m->aaModelId = AAMODEL_MTREV;
			else if (!strcmp(mp->aaModel, "Mtmam"))
				m->aaModelId = AAMODEL_MTMAM;
			else if (!strcmp(mp->aaModel, "Wag"))
				m->aaModelId = AAMODEL_WAG;
			else if (!strcmp(mp->aaModel, "Rtrev"))
				m->aaModelId = AAMODEL_RTREV;
			else if (!strcmp(mp->aaModel, "Cprev"))
				m->aaModelId = AAMODEL_CPREV;
			else if (!strcmp(mp->aaModel, "Vt"))
				m->aaModelId = AAMODEL_VT;
			else if (!strcmp(mp->aaModel, "Blosum"))
				m->aaModelId = AAMODEL_BLOSUM;
			else if (!strcmp(mp->aaModel, "Gtr"))
				m->aaModelId = AAMODEL_GTR;
			else
				{
				MrBayesPrint ("%s   Uncertain amino acid model\n", spacer);
				return (ERROR);
				}
			}
		else
			m->aaModelId = -1;
			
		/* parsimony model? */
		if (!strcmp(mp->parsModel, "Yes"))
			m->parsModelId = YES;
		else
			m->parsModelId = NO;

		/* number of gamma categories */
		if (activeParams[P_SHAPE][i] > 0)
			m->numGammaCats = mp->numGammaCats;
		else
			m->numGammaCats = 1;

		/* number of beta categories */
		if (mp->dataType == STANDARD && !(AreDoublesEqual(mp->symBetaFix, -1.0, 0.00001) == YES && !strcmp(mp->symPiPr,"Fixed")))
			m->numBetaCats = mp->numBetaCats;
		else
			m->numBetaCats = 1;

		/* number of omega categories */
		if ((mp->dataType == DNA || mp->dataType == RNA) && (!strcmp(mp->omegaVar, "Ny98") || !strcmp(mp->omegaVar, "M3")) && !strcmp(mp->nucModel, "Codon"))
			{
			m->numOmegaCats = 3;
			m->numGammaCats = 1; /* if we are here, then we cannot have gamma or beta variation */
			m->numBetaCats = 1;
			}
		else if ((mp->dataType == DNA || mp->dataType == RNA) && !strcmp(mp->omegaVar, "M10") && !strcmp(mp->nucModel, "Codon"))
			{
			m->numOmegaCats = mp->numM10BetaCats + mp->numM10GammaCats;
			m->numGammaCats = 1; /* if we are here, then we cannot have gamma or beta variation */
			m->numBetaCats = 1;
			}
		else
			m->numOmegaCats = 1;

		/* number of transition matrices depends on numGammaCats, numBetaCats, and numOmegaCats */
		m->numTiCats = m->numGammaCats * m->numBetaCats * m->numOmegaCats;

		/* TODO: check that numStates and numModelStates are set
			appropriately for codon and doublet models */

		/* number of observable states */
		if (mp->dataType == STANDARD)
			m->numStates = 0;	/* zero, meaining variable */
		else
			m->numStates = mp->nStates;
		
		/* number of model states including hidden ones */
		if ((mp->dataType == DNA || mp->dataType == RNA) && !strcmp (mp->covarionModel, "Yes") && !strcmp(mp->nucModel, "4by4"))
			m->numModelStates = mp->nStates * 2;
		else if (mp->dataType == PROTEIN && !strcmp (mp->covarionModel, "Yes"))
			m->numModelStates = mp->nStates * 2;
		else if (mp->dataType == CONTINUOUS)
			m->numModelStates = 0;
		else if (mp->dataType == STANDARD)
			{
			/* find maximum number of states */
			m->numModelStates = 0;
			for (c=0; c<m->numChars; c++)
				if (m->nStates[c] > m->numModelStates)
					m->numModelStates = m->nStates[c];
			}
		else
			m->numModelStates = mp->nStates;
			
		/* Fill in some information for calculating cijk. We will use m->nCijk to 
		   figure out if we need to diagonalize Q to calculate transition probabilities.
		   If nCijk = 0, then we won't bother. We use nCijk later in this function. */
		m->nCijk = 0;
		m->nCijkParts = 1;
		if (mp->dataType == PROTEIN)
			{
			ts = m->numModelStates;
			m->nCijk = (ts * ts * ts) + (2 * ts);
			if (!strcmp (mp->covarionModel, "Yes"))
				{
				m->nCijk *= m->numGammaCats;
				m->nCijkParts = m->numGammaCats;
				}
			}
		else if (mp->dataType == STANDARD)
			{
			if (!(strcmp(mp->symPiPr,"Fixed") == 0 && AreDoublesEqual(mp->symBetaFix, -1.0, 0.00001) == YES))
				{
				m->nCijkParts = 0;
				for (c=0; c<m->numChars; c++)
					{
					if (m->nStates[c] > 3 && (m->cType[c] == UNORD || m->cType[c] == ORD))
						{
						ts = m->nStates[c];
						m->nCijk += (ts * ts * ts) + (2 * ts);
						m->nCijkParts++;
						}
					}
				}
			}
		else if (mp->dataType == DNA || mp->dataType == RNA)
			{
			if (m->nucModelId == NUCMODEL_4BY4)
				{
				if (!strcmp (mp->covarionModel, "No") && m->nst != 6 && m->nst != 203)
					m->nCijk = 0;
				else
					{
					ts = m->numModelStates;
					m->nCijk = (ts * ts * ts) + (2 * ts);
					}
				if (!strcmp (mp->covarionModel, "Yes"))
					{
					m->nCijk *= m->numGammaCats;
					m->nCijkParts = m->numGammaCats;
					}
				}
			else if (m->nucModelId == NUCMODEL_DOUBLET)
				{
				ts = m->numModelStates;
				m->nCijk = (ts * ts * ts) + (2 * ts);
				}
			else if (m->nucModelId == NUCMODEL_CODON)
				{
				ts = m->numModelStates;
				m->nCijk = (ts * ts * ts) + (2 * ts);
				m->nCijk *= m->numOmegaCats;
				m->nCijkParts = m->numOmegaCats;
				}
			else
				{
				MrBayesPrint ("%s   ERROR: Something is wrong if you are here.\n");
				return ERROR;
				}
			}

		/* assume all divisions need different transition probability matrices */
		/* TO DO: check first if divisions can share ti matrices */
		m->tiProbsId = i;
		if (mp->dataType == CONTINUOUS)
			m->tiProbsId = i;

		/* check if we should calculate ancestral states */
		if (!strcmp(mp->inferAncStates,"Yes"))
			{
			if (mp->dataType == PROTEIN && !strcmp(mp->covarionModel, "No"))
				m->printAncStates = YES;
			else if (mp->dataType == DNA || mp->dataType == RNA)
				{
				if (!strcmp(mp->nucModel,"4by4") && !strcmp(mp->covarionModel, "No"))
					m->printAncStates = YES;
				}
			else if (mp->dataType == STANDARD || mp->dataType == RESTRICTION)
				m->printAncStates = YES;
			if (m->printAncStates == YES)
				inferAncStates = YES;
			}

		/* check if we should calculate site rates */
		if (!strcmp(mp->inferSiteRates,"Yes"))
			{
			if (m->numGammaCats > 1)
				{
				m->printSiteRates = YES;
				inferSiteRates = YES;
				}
			}

		/* check if we should calculate positive selection */
		if (!strcmp(mp->inferPosSel, "Yes"))
			{
			if (m->numOmegaCats > 1)
				{
				m->printPosSel = YES;
				inferPosSel = YES;
				}
			}
		}

	return (NO_ERROR);	
}





/*----------------------------------------------------------------------------
|
|	SetMoves: This function will set up the moves to be used in updating
|		the parameters in the possibly mixed model
|
-----------------------------------------------------------------------------*/
int SetMoves (void)

{
	
	int			i, j, j1, k, n;
	MrBFlt		sum;

	Param		*p;
	MCMCMove	*mv;


	/* set parameter proposal probs */
	for (k=0; k<numParams; k++)
		params[k].relProposalProb = 1.0;

	/* set up moves                                              */
	/* each combination of moveType and param is a separate move */
	
	/* first count moves */
	numMoves = 0;
	for (k=0; k<numParams; k++)
		{
		p = &params[k];
		for (i=0; i<NUM_MOVE_TYPES; i++)
			for (j=0; j<moveTypes[i].nApplicable; j++)
				if (moveTypes[i].applicableTo[j] == p->paramId)
					if (p->relProposalProb * moveTypes[i].relProposalProb > 0.00001)
						numMoves++;
		}

	/* then allocate space for moves */
	if (memAllocs[ALLOC_MOVES] == YES)
		{
		MrBayesPrint ("%s   Space for moves is not free in SetMoves\n", spacer);
		return ERROR;
		}
	moves = (MCMCMove *) malloc (numMoves * sizeof (MCMCMove));
	if (!moves)
		{
		MrBayesPrint ("%s   Problem allocating moves\n", spacer);
		return ERROR;
		}
	k = chainParams.numRuns * chainParams.numChains;
	moves[0].nAccepted = (int *) calloc (numMoves * 2 * k, sizeof (int));
	if (!moves[0].nAccepted)
		{
		free (moves);
		MrBayesPrint ("%s   Problem allocating moves\n", spacer);
		return ERROR;
		}
	moves[0].nTried = moves[0].nAccepted + k;
	for (i=1; i<numMoves; i++)
		{
		moves[i].nAccepted = moves[0].nAccepted + i*2*k;
		moves[i].nTried = moves[0].nAccepted + i*2*k + k;
		}
	memAllocs[ALLOC_MOVES] = YES;

	/* then set moves */
	n = 0;
	sum = 0.0;
	for (k=0; k<numParams; k++)
		{
		p = &params[k];
		for (i=0; i<numMoveTypes; i++)
			for (j=0; j<moveTypes[i].nApplicable; j++)
				if (moveTypes[i].applicableTo[j] == p->paramId)
					{
					if (p->relProposalProb * moveTypes[i].relProposalProb < 0.00001)
						continue;
					moves[n].name = moveTypes[i].name;
					moves[n].shortName = moveTypes[i].shortName;
					moves[n].relProposalProb = p->relProposalProb * moveTypes[i].relProposalProb;
					sum += moves[n].relProposalProb;
					moves[n].cumProposalProb = sum;
					moves[n].parm = p;
					moves[n].moveFxn = moveTypes[i].moveFxn;
					for (j1=0; j1<chainParams.numRuns; j1++)
						moves[n].nAccepted[j1] = moves[n].nTried[j1] = 0;
					moves[n].proposalParam[0] = moveTypes[i].proposalParam[0];
					moves[n].proposalParam[1] = moveTypes[i].proposalParam[1];
					if (moveTypes[i].parsimonyBased == YES)
						{
						for (j1=0; j1<p->nRelParts; j1++)
							modelSettings[p->relParts[j1]].parsimonyBasedMove = YES;
						}
					n++;
					}
		}

	/* rescale proposal probs */
	for (i=0; i<numMoves; i++)
		{
		moves[i].cumProposalProb /= sum;
		moves[i].relProposalProb /= sum;
		}
	
	/* and then print moves, what params they apply to, and proposal probabilities */
	MrBayesPrint ("%s   The MCMC sampler will use the following moves:\n", spacer);
	MrBayesPrint ("%s      With prob.  Chain will change\n", spacer);
	for (i=0; i<numMoves; i++)
		{
		mv = &moves[i];
		MrBayesPrint ("%s       %6.2f %%   param. %d %s\n", spacer, 100*mv->relProposalProb, mv->parm->index+1, mv->name);
		}

	return NO_ERROR;

}





int SetNucQMatrix (MrBFlt **a, int n, int whichChain, int division, MrBFlt rateMult, MrBFlt *rA, MrBFlt *rS)

{

	register int	i, j, k;
	int				isTransition=0, nDiff, rtNum=0;
	MrBFlt			scaler, mult=0.0, probOn, sum, *swr, s01, s10, s[4][4], nonsyn, *rateValues=NULL, *bs, dN, dS;
	ModelInfo		*m;
	ModelParams 	*mp;
		
	/* set up pointers to the appropriate model information */
	mp = &modelParams[division];
	m = &modelSettings[division];

	/* All of the models that are set up in this function require the frequencies
	   of the nucleotides (or doublets or codons). They will also require either
	   a transition/transversion rate ratio or the GTR rate parameters. The 
	   "rateValues" will either be
	   
	      rateValues[0] = transtion/transversion rate (kappa)
	   
	   for nst=2 models or
	   
	      rateValues[0] = G <-> T rate
	      rateValues[1] = C <-> T rate
	      rateValues[2] = C <-> G rate
	      rateValues[3] = A <-> T rate
	      rateValues[4] = A <-> G rate
	      rateValues[5] = A <-> C rate
	      
	   for nst=6 models. For GTR models, rateValues[0] = 1 (always). */
	bs = GetParamSubVals (m->stateFreq, whichChain, state[whichChain]);
	if (m->nst == 2)
		rateValues = GetParamVals(m->tRatio, whichChain, state[whichChain]);
	else if (m->nst == 6 || m->nst == 203)
		rateValues = GetParamVals(m->revMat, whichChain, state[whichChain]);

	if (n == 4) 
		{
		/* 4 X 4 model:
		
		   Here, we set the rate matrix for the GTR model (Tavare, 1986). We
		   need not only the 6 rates for this model (rateValues), but also the 
		   base frequencies (bs). */
		    
		/* set diagonal of Q matrix to 0 */
		for (i=0; i<4; i++)
			a[i][i] = 0.0;

		/* initialize Q matrix */
		scaler = 0.0;
		for (i=0; i<4; i++)
			{
			for (j=i+1; j<4; j++)
				{
				if (i == 0 && j == 1)
					mult = rateValues[0];
				else if (i == 0 && j == 2)
					mult = rateValues[1];
				else if (i == 0 && j == 3)
					mult = rateValues[2];
				else if (i == 1 && j == 2)
					mult = rateValues[3];
				else if (i == 1 && j == 3)
					mult = rateValues[4];
				else if (i == 2 && j == 3)
					mult = rateValues[5];
				a[i][i] -= (a[i][j] = bs[j] * mult);
				a[j][j] -= (a[j][i] = bs[i] * mult);
				scaler += bs[i] * a[i][j];
				scaler += bs[j] * a[j][i];
				}
			}
			
		/* rescale Q matrix */
		scaler = 1.0 / scaler;
		for (i=0; i<4; i++)
			for (j=0; j<4; j++)
				a[i][j] *= scaler;
		}
	else if (n == 8) /* we have a 4 X 4 covarion model */
		{
		/* 8 X 8 covarion model:
		
		   Here, we set the rate matrix for the covarion model (Tuffley and
		   Steel, 1997). We need the rate parameters of the model 
		   (contained in rateValues), the frequencies of the four nucleotides,
		   and the switching rates to completely specify the rate matrix. We
		   first set up the 4 X 4 submatrix that represents changes (the upper
		   left portion of the 8 X 8 matrix). Note that if we have rate
		   variation across sites, that we need to deal with the multiplication
		   in the rate matrix (i.e., we cannot simply deal with rate variation
		   by multiplying the branch length by a rate multiplier as we can
		   with other models). Instead, we multiply the scaled rate matrix
		   by the rate multiplier. */

		/* Get the switching rates. The rate of off->on is s01 and the rate
		   of on->off is s10. The stationary probability of the switch process
		   is prob1 = s01/(s01+s10) and prob0 = s10/(s01+s10). */
		swr = GetParamVals (m->switchRates, whichChain, state[whichChain]);
		s01 = swr[0];
		s10 = swr[1];
		probOn = s01 / (s01 + s10);
		
		/* set matrix a to 0 */
		for (i=0; i<8; i++)
			for (j=0; j<8; j++)
				a[i][j] = 0.0;

		/* set up the 4 X 4 matrix representing substitutions (s[][]; upper left) */
		if (m->nst == 1)
			{
			scaler = 0.0;
			for (i=0; i<4; i++)
				{
				for (j=i+1; j<4; j++)
					{
					s[i][j] = bs[j];
					s[j][i] = bs[i];
					scaler += bs[i] * s[i][j] * probOn;
					scaler += bs[j] * s[j][i] * probOn;
					}
				}
			}
		else if (m->nst == 2)
			{
			scaler = 0.0;
			for (i=0; i<4; i++)
				{
				for (j=i+1; j<4; j++)
					{
					if ((i == 0 && j == 2) || (i == 2 && j == 0) || (i == 1 && j == 3) || (i == 3 && j == 1))
						mult = rateValues[0];
					else
						mult = 1.0;
					s[i][j] = bs[j] * mult;
					s[j][i] = bs[i] * mult;
					scaler += bs[i] * s[i][j] * probOn;
					scaler += bs[j] * s[j][i] * probOn;
					}
				}
			}
		else
			{
			scaler = 0.0;
			for (i=0; i<4; i++)
				{
				for (j=i+1; j<4; j++)
					{
					if (i == 0 && j == 1)
						mult = rateValues[0];
					else if (i == 0 && j == 2)
						mult = rateValues[1];
					else if (i == 0 && j == 3)
						mult = rateValues[2];
					else if (i == 1 && j == 2)
						mult = rateValues[3];
					else if (i == 1 && j == 3)
						mult = rateValues[4];
					else if (i == 2 && j == 3)
						mult = rateValues[5];

					s[i][j] = bs[j] * mult;
					s[j][i] = bs[i] * mult;
					scaler += bs[i] * s[i][j] * probOn;
					scaler += bs[j] * s[j][i] * probOn;
					}
				}
			}

		/* rescale off diagonal elements of s[][] matrix */
		scaler = 1.0 / scaler;
		for (i=0; i<4; i++)
			{
			for (j=0; j<4; j++)
				{
				if (i != j)
					s[i][j] *= scaler;
				}
			}
			
		/* now, scale s[][] by rate factor */
		for (i=0; i<4; i++)
			{
			for (j=0; j<4; j++)
				{
				if (i != j)
					s[i][j] *= rateMult;
				}
			}
			
		/* put in diagonal elements of s[][] */
		for (i=0; i<4; i++)
			{
			sum = 0.0;
			for (j=0; j<4; j++)
				{
				if (i != j)
					sum += s[i][j];
				}
			s[i][i] = -(sum + s10);
			}
				
		/* Now, put s[][] into top left portion of a matrix and fill in the
		   other parts of the matrix with the appropriate switching rates. */
		for (i=0; i<4; i++)
			for (j=0; j<4; j++)
				a[i][j] = s[i][j];
		for (i=4; i<8; i++)
			a[i][i] = -s01;
		a[0][4] = s10;
		a[1][5] = s10;
		a[2][6] = s10;
		a[3][7] = s10;
		a[4][0] = s01;
		a[5][1] = s01;
		a[6][2] = s01;
		a[7][3] = s01;
		
#		if 0
		for (i=0; i<8; i++)
			{
			for (j=0; j<8; j++)
				printf ("%1.10lf ", a[i][j]);
			printf ("\n");
			}
		for (i=0; i<4; i++)
			printf ("%lf ", bs[i]);
		printf ("\n");
		printf ("s01 = %lf s10 = %lf pi1 = %lf pi0 = %lf\n", s01, s10, probOn, 1-probOn);
#		endif
		}
	else if (n == 16) 
		{
		/* 16 X 16 doublet model:
		
		   We have a doublet model. The states are in the order AA, AC, AG, AT, CA, CC
		   CG, CT, GA, GC, GG, GT, TA, TC, TG, TT. The rate matrix is straight-forward
		   to set up. We simply multiply the rate parameter (e.g., the ti/tv rate
		   ratio) by the doublet frequencies. */
		   
		/* set diagonal of Q matrix to 0 */
		for (i=0; i<16; i++)
			a[i][i] = 0.0;

		if (m->nst == 1) /* F81-like doublet model */
			{
			scaler = 0.0;
			for (i=0; i<16; i++)
				{
				for (j=i+1; j<16; j++)
					{
					if (((doublet[i].first & doublet[j].first) == 0) && ((doublet[i].second & doublet[j].second) == 0))
						mult = 0.0;
					else
						mult = 1.0;					
					a[i][i] -= (a[i][j] = bs[j] * mult);
					a[j][j] -= (a[j][i] = bs[i] * mult);
					scaler += bs[i] * a[i][j];
					scaler += bs[j] * a[j][i];
					}
				}
			}
		else if (m->nst == 2) /* HKY-like doublet model */
			{
			scaler = 0.0;
			for (i=0; i<16; i++)
				{
				for (j=i+1; j<16; j++)
					{
					if (((doublet[i].first & doublet[j].first) == 0) && ((doublet[i].second & doublet[j].second) == 0))
						mult = 0.0;
					else
						{
						if ((doublet[i].first & doublet[j].first) == 0)
							{
							if ((doublet[i].first + doublet[j].first) == 5 || (doublet[i].first + doublet[j].first) == 10)
								mult = rateValues[0];
							else
								mult = 1.0;
							}
						else
							{
							if ((doublet[i].second + doublet[j].second) == 5 || (doublet[i].second + doublet[j].second) == 10)
								mult = rateValues[0];
							else
								mult = 1.0;
							}
						}				
					a[i][i] -= (a[i][j] = bs[j] * mult);
					a[j][j] -= (a[j][i] = bs[i] * mult);
					scaler += bs[i] * a[i][j];
					scaler += bs[j] * a[j][i];
					}
				}
			}
		else /* GTR-like doublet model */
			{
			scaler = 0.0;
			for (i=0; i<16; i++)
				{
				for (j=i+1; j<16; j++)
					{
					if (((doublet[i].first & doublet[j].first) == 0) && ((doublet[i].second & doublet[j].second) == 0))
						mult = 0.0;
					else
						{
						if ((doublet[i].first & doublet[j].first) == 0)
							{
							if ((doublet[i].first + doublet[j].first) == 3)
								mult = rateValues[0];
							else if ((doublet[i].first + doublet[j].first) == 5)
								mult = rateValues[1];
							else if ((doublet[i].first + doublet[j].first) == 9)
								mult = rateValues[2];
							else if ((doublet[i].first + doublet[j].first) == 6)
								mult = rateValues[3];
							else if ((doublet[i].first + doublet[j].first) == 10)
								mult = rateValues[4];
							else
								mult = rateValues[5];
							}
						else
							{
							if ((doublet[i].second + doublet[j].second) == 3)
								mult = rateValues[0];
							else if ((doublet[i].second + doublet[j].second) == 5)
								mult = rateValues[1];
							else if ((doublet[i].second + doublet[j].second) == 9)
								mult = rateValues[2];
							else if ((doublet[i].second + doublet[j].second) == 6)
								mult = rateValues[3];
							else if ((doublet[i].second + doublet[j].second) == 10)
								mult = rateValues[4];
							else
								mult = rateValues[5];
							}
						}				
					a[i][i] -= (a[i][j] = bs[j] * mult);
					a[j][j] -= (a[j][i] = bs[i] * mult);
					scaler += bs[i] * a[i][j];
					scaler += bs[j] * a[j][i];
					}
				}
			}
					
			
		/* rescale Q matrix */
		scaler = 1.0 / scaler;
		for (i=0; i<16; i++)
			for (j=0; j<16; j++)
				a[i][j] *= scaler;
		}
	else
		{
		/* 64(ish) X 64(ish) codon model:
		
		   Here, we set the rate matrix for the codon model (see Goldman and
		   Yang, 1994). Note that we can specifiy any general type of codon
		   model, with these constraints:
		   
		  	a[i][j] = 0                      -> if i and j differ at 2 or 3 nucleotides
		  	a[i][j] = rateValues[0] * bs[j]  -> if synonymous G <-> T change
		  	a[i][j] = rateValues[1] * bs[j]  -> if synonymous C <-> T change
		  	a[i][j] = rateValues[2] * bs[j]  -> if synonymous C <-> G change
		  	a[i][j] = rateValues[3] * bs[j]  -> if synonymous A <-> T change
		  	a[i][j] = rateValues[4] * bs[j]  -> if synonymous A <-> G change
		  	a[i][j] = rateValues[5] * bs[j]  -> if synonymous A <-> C change
		  	
		  	a[i][j] = rateValues[0] * nonsyn * bs[j]  -> if nonsynonymous G <-> T change
		  	a[i][j] = rateValues[1] * nonsyn * bs[j]  -> if nonsynonymous C <-> T change
		  	a[i][j] = rateValues[2] * nonsyn * bs[j]  -> if nonsynonymous C <-> G change
		  	a[i][j] = rateValues[3] * nonsyn * bs[j]  -> if nonsynonymous A <-> T change
		  	a[i][j] = rateValues[4] * nonsyn * bs[j]  -> if nonsynonymous A <-> G change
		  	a[i][j] = rateValues[5] * nonsyn * bs[j]  -> if nonsynonymous A <-> C change
		  	
		  Other models, such as the one used by Nielsen & Yang (1998) can be obtained
		  from this model by restricing transitions and transversions to have the same rate.
		  nonsyn is the nonsynonymous/synonymous rate ratio (often called the
		  dN/dS ratio). If we are in this part of the function, then we rely on it
		  being called with the "rateMult" parameter specifying the dN/dS ratio. Note
		  that the size of the matrix will never be 64 X 64 as we only consider changes
		  among coding triplets (i.e., we exclude the stop codons). */
		  
		/* get the nonsynonymous/synonymous rate ratio */
		nonsyn = rateMult; 
		
		/* set diagonal of Q matrix to 0 */
		for (i=0; i<n; i++)
			a[i][i] = 0.0;
			
		/* set dN and dS rates to zero */
		dN = dS = 0.0;

		if (m->nst == 1) /* F81-like codon model */
			{
			scaler = 0.0;
			for (i=0; i<n; i++)
				{
				for (j=i+1; j<n; j++)
					{
					nDiff = 0;
					for (k=0; k<3; k++)
						{
						if (mp->codonNucs[i][k] != mp->codonNucs[j][k])
							nDiff++;
						}
					if (nDiff > 1)
						{
						mult = 0.0;
						}
					else
						{
						if (mp->codonAAs[i] == mp->codonAAs[j])
							mult = 1.0;
						else
							mult = nonsyn;
						}
					
					a[i][i] -= (a[i][j] = bs[j] * mult);
					a[j][j] -= (a[j][i] = bs[i] * mult);
					if (mp->codonAAs[i] == mp->codonAAs[j])
						dS += (bs[i] * a[i][j] + bs[j] * a[j][i]);
					else
						dN += (bs[i] * a[i][j] + bs[j] * a[j][i]);
					scaler += bs[i] * a[i][j];
					scaler += bs[j] * a[j][i];
					}
				}
			}
		else if (m->nst == 2) /* HKY-like codon model */
			{
			scaler = 0.0;
			for (i=0; i<n; i++)
				{
				for (j=i+1; j<n; j++)
					{
					nDiff = 0;
					for (k=0; k<3; k++)
						{
						if (mp->codonNucs[i][k] != mp->codonNucs[j][k])
							{
							nDiff++;
							if ((mp->codonNucs[i][k] == 0 && mp->codonNucs[j][k] == 2) || (mp->codonNucs[i][k] == 2 && mp->codonNucs[j][k] == 0) ||
							    (mp->codonNucs[i][k] == 1 && mp->codonNucs[j][k] == 3) || (mp->codonNucs[i][k] == 3 && mp->codonNucs[j][k] == 1))
								isTransition = YES;
							else
								isTransition = NO;
							}
						}
					if (nDiff > 1)
						{
						mult = 0.0;
						}
					else
						{
						if (mp->codonAAs[i] == mp->codonAAs[j])
							mult = 1.0;
						else
							mult = nonsyn;
						if (isTransition == YES)
							mult *= rateValues[0];
						}
					
					a[i][i] -= (a[i][j] = bs[j] * mult);
					a[j][j] -= (a[j][i] = bs[i] * mult);
					if (mp->codonAAs[i] == mp->codonAAs[j])
						dS += (bs[i] * a[i][j] + bs[j] * a[j][i]);
					else
						dN += (bs[i] * a[i][j] + bs[j] * a[j][i]);
					scaler += bs[i] * a[i][j];
					scaler += bs[j] * a[j][i];
					}
				}
			}
		else /* GTR-like codon model */
			{
			scaler = 0.0;
			for (i=0; i<n; i++)
				{
				for (j=i+1; j<n; j++)
					{
					nDiff = 0;
					for (k=0; k<3; k++)
						{
						if (mp->codonNucs[i][k] != mp->codonNucs[j][k])
							{
							nDiff++;
							if ((mp->codonNucs[i][k] == 0 && mp->codonNucs[j][k] == 1) || (mp->codonNucs[i][k] == 1 && mp->codonNucs[j][k] == 0))
								rtNum = 0;
							else if ((mp->codonNucs[i][k] == 0 && mp->codonNucs[j][k] == 2) || (mp->codonNucs[i][k] == 2 && mp->codonNucs[j][k] == 0))
								rtNum = 1;
							else if ((mp->codonNucs[i][k] == 0 && mp->codonNucs[j][k] == 3) || (mp->codonNucs[i][k] == 3 && mp->codonNucs[j][k] == 0))
								rtNum = 2;
							else if ((mp->codonNucs[i][k] == 1 && mp->codonNucs[j][k] == 2) || (mp->codonNucs[i][k] == 2 && mp->codonNucs[j][k] == 1))
								rtNum = 3;
							else if ((mp->codonNucs[i][k] == 1 && mp->codonNucs[j][k] == 3) || (mp->codonNucs[i][k] == 3 && mp->codonNucs[j][k] == 1))
								rtNum = 4;
							else
								rtNum = 5;
							}
						}
					if (nDiff > 1)
						{
						mult = 0.0;
						}
					else
						{
						if (mp->codonAAs[i] == mp->codonAAs[j])
							mult = 1.0;
						else
							mult = nonsyn;
						if (rtNum == 0)
							mult *= rateValues[0];
						else if (rtNum == 1)
							mult *= rateValues[1];
						else if (rtNum == 2)
							mult *= rateValues[2];
						else if (rtNum == 3)
							mult *= rateValues[3];
						else if (rtNum == 4)
							mult *= rateValues[4];
						else
							mult *= rateValues[5];
						}
					
					a[i][i] -= (a[i][j] = bs[j] * mult);
					a[j][j] -= (a[j][i] = bs[i] * mult);
					if (mp->codonAAs[i] == mp->codonAAs[j])
						dS += (bs[i] * a[i][j] + bs[j] * a[j][i]);
					else
						dN += (bs[i] * a[i][j] + bs[j] * a[j][i]);
					scaler += bs[i] * a[i][j];
					scaler += bs[j] * a[j][i];
					}
				}
			}

		/* rescale Q matrix */
		if (inferPosSel == YES)
			{
			/* If we have a positive selection model with multiple categories, then
			   we do not rescale the rate matrix until we have finished generating
			   all of the necessary rate matrices. The rescaling occurrs in 
			   UpDateCijk. */
			(*rA) = dN;
			(*rS) = dS;
			}
		else
			{
			scaler = 1.0 / scaler;
			for (i=0; i<n; i++)
				for (j=0; j<n; j++)
					a[i][j] *= scaler;
			(*rA) = (*rS) = 1.0;
			}			
		}

#	if 0
	for (i=0; i<n; i++)
		{
		for (j=0; j<n; j++)
			printf ("%0.5lf ", a[i][j]);
		printf ("\n");
		}
#	endif

	return (NO_ERROR);
	
}





int SetProteinQMatrix (MrBFlt **a, int n, int whichChain, int division, MrBFlt rateMult)

{

	register int	i, j, k;
	int				aaModelID;
	MrBFlt			scaler, probOn, sum, *swr, s01, s10, *bs, *rt;
	ModelInfo		*m;
	ModelParams 	*mp;
		
	/* set up pointers to the appropriate model information */
	mp = &modelParams[division];
	m = &modelSettings[division];

	/* get amino acid model ID 
		AAMODEL_POISSON			0
		AAMODEL_JONES			1
		AAMODEL_DAY				2
		AAMODEL_MTREV			3
		AAMODEL_MTMAM			4
		AAMODEL_WAG				5
		AAMODEL_RTREV			6
		AAMODEL_CPREV           7
		AAMODEL_VT				8
		AAMODEL_BLOSUM			9
		AAMODEL_EQ			   10
		AAMODEL_GTR            11 */
		
	if (m->aaModelId >= 0)
		aaModelID = m->aaModelId;
	else
		aaModelID = (int)*GetParamVals(m->aaModel, whichChain, state[whichChain]);
	
	/* Make certain that we have either 20 or 40 states. Anything
	   else means we have a real problem. */
	if (n != 20 && n != 40)
		{
		MrBayesPrint ("%s   ERROR: There should be 20 or 40 states for the aa model\n");
		return (ERROR);
		}

	if (n == 20)
		{
		/* We have a run-of-the-mill amino acid model (i.e., 20 X 20). */
		if (aaModelID == AAMODEL_POISSON)
			{
			scaler = 1.0 / 19.0;
			for (i=0; i<20; i++)
				{
				for (j=i+1; j<20; j++)
					{
					a[i][j] = scaler;
					a[j][i] = scaler;
					}
				}
			for (i=0; i<20; i++)
				a[i][i] = -1.0;
			}
		else if (aaModelID == AAMODEL_EQ)
			{
			bs = GetParamSubVals (m->stateFreq, whichChain, state[whichChain]);
			for (i=0; i<20; i++)
				for (j=0; j<20; j++)
					a[i][j] = 0.0;
			scaler = 0.0;	
			for (i=0; i<20; i++)
				{
				for (j=i+1; j<20; j++)
					{
					a[i][i] -= (a[i][j] = bs[j]);
					a[j][j] -= (a[j][i] = bs[i]);
					scaler += bs[i] * a[i][j];
					scaler += bs[j] * a[j][i];
					}
				}
			scaler = 1.0 / scaler;
			for (i=0; i<20; i++)
				for (j=0; j<20; j++)
					a[i][j] *= scaler;
			}
		else if (aaModelID == AAMODEL_GTR)
			{
			bs = GetParamSubVals (m->stateFreq, whichChain, state[whichChain]);
			rt = GetParamVals (m->revMat, whichChain, state[whichChain]);
			for (i=0; i<20; i++)
				for (j=0; j<20; j++)
					a[i][j] = 0.0;
			scaler = 0.0;
			for (i=k=0; i<20; i++)
				{
				for (j=i+1; j<20; j++)
					{
					a[i][i] -= (a[i][j] = bs[j] * rt[k]);
					a[j][j] -= (a[j][i] = bs[i] * rt[k]);
					k++;
					}
				}
			for (i=0; i<20; i++)
				scaler += -(bs[i] * a[i][i]);
			for (i=0; i<20; i++)
				for (j=0; j<20; j++)
					a[i][j] /= scaler;
			}
		else if (aaModelID == AAMODEL_JONES)
			{
			for (i=0; i<20; i++)
				for (j=0; j<20; j++)
					a[i][j] = aaJones[i][j];
			}
		else if (aaModelID == AAMODEL_DAY)
			{
			for (i=0; i<20; i++)
				for (j=0; j<20; j++)
					a[i][j] = aaDayhoff[i][j];
			}
		else if (aaModelID == AAMODEL_MTREV)
			{
			for (i=0; i<20; i++)
				for (j=0; j<20; j++)
					a[i][j] = aaMtrev24[i][j];
			}
		else if (aaModelID == AAMODEL_MTMAM)
			{
			for (i=0; i<20; i++)
				for (j=0; j<20; j++)
					a[i][j] = aaMtmam[i][j];
			}
		else if (aaModelID == AAMODEL_RTREV)
			{
			for (i=0; i<20; i++)
				for (j=0; j<20; j++)
					a[i][j] = aartREV[i][j];
			}
		else if (aaModelID == AAMODEL_WAG)
			{
			for (i=0; i<20; i++)
				for (j=0; j<20; j++)
					a[i][j] = aaWAG[i][j];
			}
		else if (aaModelID == AAMODEL_CPREV)
			{
			for (i=0; i<20; i++)
				for (j=0; j<20; j++)
					a[i][j] = aacpREV[i][j];
			}
		else if (aaModelID == AAMODEL_VT)
			{
			for (i=0; i<20; i++)
				for (j=0; j<20; j++)
					a[i][j] = aaVt[i][j];
			}
		else if (aaModelID == AAMODEL_BLOSUM)
			{
			for (i=0; i<20; i++)
				for (j=0; j<20; j++)
					a[i][j] = aaBlosum[i][j];
			}
		else
			{
			MrBayesPrint ("%s   ERROR: Don't understand which amino acid model is needed\n");
			return (ERROR);
			}
#		if 0
		for (i=0; i<20; i++)
			{
			for (j=0; j<20; j++)
				printf ("%1.3lf ", a[i][j]);
			printf ("\n");
			}
#		endif
		}
	else
		{
		/* 40 X 40 covarion model:
		
		   We have a covarion model, and must set up the other quadrants. Note that if
		   we are at this point in the code, that we have already set up the upper left
		   portion of the 40 X 40 rate matrix. Note that if we have rate
		   variation across sites, that we need to deal with the multiplication
		   in the rate matrix (i.e., we cannot simply deal with rate variation
		   by multiplying the branch length by a rate multiplier as we can
		   with other models). Instead, we multiply the scaled rate matrix
		   by the rate multiplier. */

		/* Get the switching rates. The rate of off->on is s01 and the rate
		   of on->off is s10. The stationary probability of the switch process
		   is prob1 = s01/(s01+s10) and prob0 = s10/(s01+s10). */
		swr = GetParamVals (m->switchRates, whichChain, state[whichChain]);
		s01 = swr[0];
		s10 = swr[1];
		probOn = s01 / (s01 + s10);
		
		/* set matrix a[][] to 0 */
		for (i=0; i<40; i++)
			for (j=0; j<40; j++)
				a[i][j] = 0.0;	
				
		/* fill in upper-left sub matrix (where substitutions occur */
		if (aaModelID == AAMODEL_POISSON)
			{
			scaler = 0.0;
			for (i=0; i<20; i++)
				{
				for (j=i+1; j<20; j++)
					{
					a[i][j] = 0.05;
					a[j][i] = 0.05;
					scaler += 0.05 * a[i][j] * probOn;
					scaler += 0.05 * a[j][i] * probOn;
					}
				}
			}
		else if (aaModelID == AAMODEL_EQ)
			{
			bs = GetParamSubVals (m->stateFreq, whichChain, state[whichChain]);
			scaler = 0.0;
			for (i=0; i<20; i++)
				{
				for (j=i+1; j<20; j++)
					{
					a[i][j] = bs[j];
					a[j][i] = bs[i];
					scaler += bs[i] * a[i][j] * probOn;
					scaler += bs[j] * a[j][i] * probOn;
					}
				}
			}
		else if (aaModelID == AAMODEL_GTR)
			{
			bs = GetParamSubVals (m->stateFreq, whichChain, state[whichChain]);
			rt = GetParamVals (m->revMat, whichChain, state[whichChain]);
			for (i=0; i<20; i++)
				for (j=0; j<20; j++)
					a[i][j] = 0.0;
			scaler = 0.0;
			for (i=k=0; i<20; i++)
				{
				for (j=i+1; j<20; j++)
					{
					a[i][i] -= (a[i][j] = bs[j] * rt[k]);
					a[j][j] -= (a[j][i] = bs[i] * rt[k]);
					k++;
					}
				}
			for (i=0; i<20; i++)
				scaler += -(bs[i] * a[i][i]);
			for (i=0; i<20; i++)
				for (j=0; j<20; j++)
					a[i][j] /= scaler;
			for (i=0; i<20; i++)
				{
				for (j=i+1; j<20; j++)
					{
					a[i][j] = bs[j];
					a[j][i] = bs[i];
					scaler += bs[i] * a[i][j] * probOn;
					scaler += bs[j] * a[j][i] * probOn;
					}
				}
			}
		else if (aaModelID == AAMODEL_JONES)
			{
			scaler = 0.0;
			for (i=0; i<20; i++)
				{
				for (j=i+1; j<20; j++)
					{
					a[i][j] = aaJones[i][j];
					a[j][i] = aaJones[j][i];
					scaler += jonesPi[i] * a[i][j] * probOn;
					scaler += jonesPi[j] * a[j][i] * probOn;
					}
				}
			}
		else if (aaModelID == AAMODEL_DAY)
			{
			scaler = 0.0;
			for (i=0; i<20; i++)
				{
				for (j=i+1; j<20; j++)
					{
					a[i][j] = aaDayhoff[i][j];
					a[j][i] = aaDayhoff[j][i];
					scaler += dayhoffPi[i] * a[i][j] * probOn;
					scaler += dayhoffPi[j] * a[j][i] * probOn;
					}
				}
			}
		else if (aaModelID == AAMODEL_MTREV)
			{
			scaler = 0.0;
			for (i=0; i<20; i++)
				{
				for (j=i+1; j<20; j++)
					{
					a[i][j] = aaMtrev24[i][j];
					a[j][i] = aaMtrev24[j][i];
					scaler += mtrev24Pi[i] * a[i][j] * probOn;
					scaler += mtrev24Pi[j] * a[j][i] * probOn;
					}
				}
			}
		else if (aaModelID == AAMODEL_MTMAM)
			{
			scaler = 0.0;
			for (i=0; i<20; i++)
				{
				for (j=i+1; j<20; j++)
					{
					a[i][j] = aaMtmam[i][j];
					a[j][i] = aaMtmam[j][i];
					scaler += mtmamPi[i] * a[i][j] * probOn;
					scaler += mtmamPi[j] * a[j][i] * probOn;
					}
				}
			}
		else if (aaModelID == AAMODEL_RTREV)
			{
			scaler = 0.0;
			for (i=0; i<20; i++)
				{
				for (j=i+1; j<20; j++)
					{
					a[i][j] = aartREV[i][j];
					a[j][i] = aartREV[j][i];
					scaler += rtrevPi[i] * a[i][j] * probOn;
					scaler += rtrevPi[j] * a[j][i] * probOn;
					}
				}
			}
		else if (aaModelID == AAMODEL_WAG)
			{
			scaler = 0.0;
			for (i=0; i<20; i++)
				{
				for (j=i+1; j<20; j++)
					{
					a[i][j] = aaWAG[i][j];
					a[j][i] = aaWAG[j][i];
					scaler += wagPi[i] * a[i][j] * probOn;
					scaler += wagPi[j] * a[j][i] * probOn;
					}
				}
			}
		else if (aaModelID == AAMODEL_CPREV)
			{
			scaler = 0.0;
			for (i=0; i<20; i++)
				{
				for (j=i+1; j<20; j++)
					{
					a[i][j] = aacpREV[i][j];
					a[j][i] = aacpREV[j][i];
					scaler += cprevPi[i] * a[i][j] * probOn;
					scaler += cprevPi[j] * a[j][i] * probOn;
					}
				}
			}
		else if (aaModelID == AAMODEL_VT)
			{
			scaler = 0.0;
			for (i=0; i<20; i++)
				{
				for (j=i+1; j<20; j++)
					{
					a[i][j] = aaVt[i][j];
					a[j][i] = aaVt[j][i];
					scaler += vtPi[i] * a[i][j] * probOn;
					scaler += vtPi[j] * a[j][i] * probOn;
					}
				}
			}
		else if (aaModelID == AAMODEL_BLOSUM)
			{
			scaler = 0.0;
			for (i=0; i<20; i++)
				{
				for (j=i+1; j<20; j++)
					{
					a[i][j] = aaBlosum[i][j];
					a[j][i] = aaBlosum[j][i];
					scaler += blosPi[i] * a[i][j] * probOn;
					scaler += blosPi[j] * a[j][i] * probOn;
					}
				}
			}
		else
			{
			MrBayesPrint ("%s   ERROR: Don't understand which amino acid model is needed\n");
			return (ERROR);
			}

		/* rescale off diagonal elements of Q matrix */
		scaler = 1.0 / scaler;
		for (i=0; i<20; i++)
			{
			for (j=0; j<20; j++)
				{
				if (i != j)
					a[i][j] *= scaler;
				}
			}
			
		/* now, scale by rate factor */
		for (i=0; i<20; i++)
			{
			for (j=0; j<20; j++)
				{
				if (i != j)
					a[i][j] *= rateMult;
				}
			}
			
		/* put in diagonal elements */
		for (i=0; i<20; i++)
			{
			sum = 0.0;
			for (j=0; j<20; j++)
				{
				if (i != j)
					sum += a[i][j];
				a[i][i] = -(sum + s10);
				}
			}
				
		/* fill in the other three submatrices */
		for (i=20; i<40; i++)
			a[i][i] = -s01;
		for (i=0; i<20; i++)
			{
			a[i][20+i] = s10;
			a[20+i][i] = s01;
			}
					   
		}

	return (NO_ERROR);
	
}





int SetStdQMatrix (MrBFlt **a, int nStates, MrBFlt *bs, int cType)

{

	register int	i, j;
	MrBFlt			scaler;

	/* This function sets up ordered or unordered models for standard characters
	   with unequal stationary state frequencies. It requires the stationary
	   frequencies of the states (passed when calling the function). It also
	   needs to know the number of states and the type (ordered or unordered) 
	   of the character. */

	/* set diagonal of Q matrix to 0 */
	for (i=0; i<nStates; i++)
		a[i][i] = 0.0;

	/* initialize Q matrix */
	scaler = 0.0;
	if (cType == UNORD)
		{
		/* unordered characters */
		for (i=0; i<nStates; i++)
			{
			for (j=0; j<nStates; j++)
				{
				if (i != j)
					{
					a[i][i] -= (a[i][j] = bs[j]);
					scaler += bs[i] * a[i][j];
					}
				}
			}
		}
	else
		{
		/* ordered characters */
		for (i=0; i<nStates; i++)
			{
			for (j=0; j<nStates; j++)
				{
				if (abs(i - j) == 1)
					{
					a[i][i] -= (a[i][j] = bs[j]);
					scaler += bs[i] * a[i][j];
					}
				}
			}
		}
		
	/* rescale Q matrix */
	for (i=0; i<nStates; i++)
		for (j=0; j<nStates; j++)
			a[i][j] /= scaler;

#	if defined DEBUG_SETSTDQMATRIX
	for (i=0; i<nStates; i++)
		{
		for (j=0; j<nStates; j++)
			printf ("%0.5lf ", a[i][j]);
		printf ("\n");
		}
#	endif

	return (NO_ERROR);
	
}





/*------------------------------------------------------------------------
|
|	SetUpMoveTypes: Set up structs holding info on each move type
|
------------------------------------------------------------------------*/
void SetUpMoveTypes (void)

{
	
	/* TO DO: register the move type here when new move functions are added           */
	/* Remember to check that the number of move types does not exceed NUM_MOVE_TYPES */
	/* defined in mb.h. Also, make certain that any changes here are mirrored in      */
	/* the "props" move in command.h.                                                 */
	int			i;
	MoveType	*mt;

	i = 0;

	mt = &moveTypes[i++];
	mt->name = "(omega) with sliding window";
	strcpy (mt->shortName, "slider");
	mt->applicableTo[0] = OMEGA_DIR;
	mt->nApplicable = 1;
	mt->moveFxn = &Move_Omega;
	mt->relProposalProb = relProbs[0];
	mt->proposalParam[0] = proParam[0][0]; /* sliding window size */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(state frequencies) with Dirichlet proposal";
	strcpy (mt->shortName, "Dirichlet");
	mt->applicableTo[0] = PI_DIR;
	mt->nApplicable = 1;
	mt->moveFxn = &Move_Statefreqs;
	mt->relProposalProb = relProbs[1];
	mt->proposalParam[0] = proParam[1][0]; /* so-called "alphaPi" */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(symmetric Dirichlet/beta) with sliding window";
	strcpy (mt->shortName, "slider");
	mt->applicableTo[0] = SYMPI_UNI;
	mt->applicableTo[1] = SYMPI_EXP;
	mt->nApplicable = 2;
	mt->moveFxn = &Move_Beta;
	mt->relProposalProb = relProbs[2];
	mt->proposalParam[0] = proParam[2][0]; /* dirichlet parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(prop. invar. sites) with beta proposal";
	strcpy (mt->shortName, "beta");
	mt->applicableTo[0] = PINVAR_UNI;
	mt->nApplicable = 1;
	mt->moveFxn = &Move_Pinvar;
	mt->relProposalProb = relProbs[3];
	mt->proposalParam[0] = proParam[3][0]; /* beta parameter size */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(correlation of adgamma) with sliding window";
	strcpy (mt->shortName, "slider");
	mt->applicableTo[0] = CORREL_UNI;
	mt->nApplicable = 1;
	mt->moveFxn = &Move_Adgamma;
	mt->relProposalProb = relProbs[4];
	mt->proposalParam[0] = proParam[4][0]; /* sliding window size */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(covarion switch rates) with sliding window";
	strcpy (mt->shortName, "slider");
	mt->applicableTo[0] = SWITCH_UNI;
	mt->applicableTo[1] = SWITCH_EXP;
	mt->nApplicable = 2;
	mt->moveFxn = &Move_SwitchRate;
	mt->relProposalProb = relProbs[5];
	mt->proposalParam[0] = proParam[5][0]; /* sliding window size */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(speciation rate) with sliding window";
	strcpy (mt->shortName, "slider");
	mt->applicableTo[0] = SPECRATE_UNI;
	mt->applicableTo[1] = SPECRATE_EXP;
	mt->nApplicable = 2;
	mt->moveFxn = &Move_Speciation;
	mt->relProposalProb = relProbs[6];
	mt->proposalParam[0] = proParam[6][0]; /* sliding window size */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(extinction rate) with sliding window";
	strcpy (mt->shortName, "slider");
	mt->applicableTo[0] = EXTRATE_UNI;
	mt->applicableTo[1] = EXTRATE_EXP;
	mt->nApplicable = 2;
	mt->moveFxn = &Move_Extinction;
	mt->relProposalProb = relProbs[7];
	mt->proposalParam[0] = proParam[7][0]; /* sliding window size */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(coalescence parameter) with sliding window";
	strcpy (mt->shortName, "slider");
	mt->applicableTo[0] = THETA_UNI;
	mt->applicableTo[1] = THETA_EXP;
	mt->nApplicable = 2;
	mt->moveFxn = &Move_Theta;
	mt->relProposalProb = relProbs[8];
	mt->proposalParam[0] = proParam[8][0]; /* sliding window size */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(topology and branch lengths) with LOCAL";
	strcpy (mt->shortName, "LOCAL");
	mt->applicableTo[0] = TOPOLOGY_NCL_UNIFORM_HOMO;
	mt->applicableTo[1] = TOPOLOGY_NCL_CONSTRAINED_HOMO;
	mt->nApplicable = 2;
	mt->moveFxn = &Move_Local;
	mt->relProposalProb = relProbs[9];
	mt->proposalParam[0] = proParam[9][0]; /* "tuning" parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(branch lengths) with multiplier";
	strcpy (mt->shortName, "mult");
	mt->applicableTo[0] = BRLENS_UNI;
	mt->applicableTo[1] = BRLENS_EXP;
	mt->nApplicable = 2;
	mt->moveFxn = &Move_BrLen;
	mt->relProposalProb = relProbs[10];
	mt->proposalParam[0] = proParam[10][0]; /* "tuning" parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(topology and branch lengths) with extending TBR";
	strcpy (mt->shortName, "eTBR");
	mt->applicableTo[0] = TOPOLOGY_NCL_UNIFORM_HOMO;
	mt->applicableTo[1] = TOPOLOGY_NCL_CONSTRAINED_HOMO;
	mt->nApplicable = 2;
	mt->moveFxn = &Move_ExtTBR;
	mt->relProposalProb = relProbs[11];
	mt->proposalParam[0] = proParam[11][0]; /* extension probability */
	mt->proposalParam[1] = proParam[11][1]; /* "tuning" parameter    */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(omega 1) with sliding window";
	strcpy (mt->shortName, "slider");
	mt->applicableTo[0] = OMEGA_BUD;
	mt->applicableTo[1] = OMEGA_BUF;
	mt->applicableTo[2] = OMEGA_BED;
	mt->applicableTo[3] = OMEGA_BEF;
	mt->applicableTo[4] = OMEGA_BFD;
	mt->applicableTo[5] = OMEGA_BFF;
	mt->applicableTo[6] = OMEGA_ED;
	mt->applicableTo[7] = OMEGA_EF;
	mt->nApplicable = 8;
	mt->moveFxn = &Move_OmegaPur;
	mt->relProposalProb = relProbs[12];
	mt->proposalParam[0] = proParam[12][0]; /* sliding window size */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(omega 3) with sliding window";
	strcpy (mt->shortName, "slider");
	mt->applicableTo[0] = OMEGA_BUD;
	mt->applicableTo[1] = OMEGA_BUF;
	mt->applicableTo[2] = OMEGA_BED;
	mt->applicableTo[3] = OMEGA_BEF;
	mt->applicableTo[4] = OMEGA_FUD;
	mt->applicableTo[5] = OMEGA_FUF;
	mt->applicableTo[6] = OMEGA_FED;
	mt->applicableTo[7] = OMEGA_FEF;
	mt->applicableTo[8] = OMEGA_ED;
	mt->applicableTo[9] = OMEGA_EF;
	mt->nApplicable = 10;
	mt->moveFxn = &Move_OmegaPos;
	mt->relProposalProb = relProbs[13];
	mt->proposalParam[0] = proParam[13][0]; /* sliding window size */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(omega cat. freqs.) with Dirichlet proposal";
	strcpy (mt->shortName, "Dirichlet");
	mt->applicableTo[0] = OMEGA_BUD;
	mt->applicableTo[1] = OMEGA_BED;
	mt->applicableTo[2] = OMEGA_BFD;
	mt->applicableTo[3] = OMEGA_FUD;
	mt->applicableTo[4] = OMEGA_FED;
	mt->applicableTo[5] = OMEGA_FFD;
	mt->applicableTo[6] = OMEGA_ED;
	mt->applicableTo[7] = OMEGA_FD;
	mt->applicableTo[8] = OMEGA_10UUB;
	mt->applicableTo[9] = OMEGA_10UEB;
	mt->applicableTo[10] = OMEGA_10UFB;
	mt->applicableTo[11] = OMEGA_10EUB;
	mt->applicableTo[12] = OMEGA_10EEB;
	mt->applicableTo[13] = OMEGA_10EFB;
	mt->applicableTo[14] = OMEGA_10FUB;
	mt->applicableTo[15] = OMEGA_10FEB;
	mt->applicableTo[16] = OMEGA_10FFB;
	mt->nApplicable = 17;
	mt->moveFxn = &Move_OmegaCat;
	mt->relProposalProb = relProbs[14];
	mt->proposalParam[0] = proParam[14][0]; /* Dirichlet parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(topology and branch lengths) with clock-constrained LOCAL";
	strcpy (mt->shortName, "cLOCAL");
	mt->applicableTo[0] = TOPOLOGY_CL_UNIFORM_HOMO;
	mt->applicableTo[1] = TOPOLOGY_CCL_UNIFORM_HOMO;
	mt->applicableTo[2] = TOPOLOGY_CL_CONSTRAINED_HOMO;
	mt->applicableTo[3] = TOPOLOGY_CCL_CONSTRAINED_HOMO;
	mt->nApplicable = 4;
	mt->moveFxn = &Move_LocalClock;
	mt->relProposalProb = relProbs[15];
	mt->proposalParam[0] = proParam[15][0]; /* "tuning" parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(topology and branch lengths) with clock-constrained SPR";
	strcpy (mt->shortName, "cSPR");
	mt->applicableTo[0] = TOPOLOGY_CL_UNIFORM_HOMO;
	mt->applicableTo[1] = TOPOLOGY_CCL_UNIFORM_HOMO;
	mt->nApplicable = 2;
	mt->moveFxn = &Move_SPRClock;
	mt->relProposalProb = relProbs[16];
	mt->proposalParam[0] = proParam[16][0]; /* "tuning" parameter    */
	mt->proposalParam[1] = proParam[16][1]; /* exponential parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(amino acid model) randomly";
	strcpy (mt->shortName, "uniform");
	mt->applicableTo[0] = AAMODEL_MIX;
	mt->nApplicable = 1;
	mt->moveFxn = &Move_Aamodel;
	mt->relProposalProb = relProbs[17];
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(topology and branch lengths) with NNI";
	strcpy (mt->shortName, "pNNI");
	mt->applicableTo[0] = TOPOLOGY_PARSIMONY_UNIFORM;
	mt->applicableTo[1] = TOPOLOGY_PARSIMONY_CONSTRAINED;
	mt->nApplicable = 2;
	mt->moveFxn = &Move_NNI;
	mt->relProposalProb = relProbs[18];
	mt->proposalParam[0] = 0.0;
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(omega) with multiplier";
	strcpy (mt->shortName, "mult");
	mt->applicableTo[0] = OMEGA_DIR;
	mt->nApplicable = 1;
	mt->moveFxn = &Move_Omega_M;
	mt->relProposalProb = relProbs[19];
	mt->proposalParam[0] = proParam[19][0]; /* tuning parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(symmetric Dirichlet/beta) with multiplier";
	strcpy (mt->shortName, "mult");
	mt->applicableTo[0] = SYMPI_UNI;
	mt->applicableTo[1] = SYMPI_EXP;
	mt->nApplicable = 2;
	mt->moveFxn = &Move_Beta_M;
	mt->relProposalProb = relProbs[20];
	mt->proposalParam[0] = proParam[20][0]; /* tuning parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(gamma shape) with multiplier";
	strcpy (mt->shortName, "mult");
	mt->applicableTo[0] = SHAPE_UNI;
	mt->applicableTo[1] = SHAPE_EXP;
	mt->nApplicable = 2;
	mt->moveFxn = &Move_GammaShape_M;
	mt->relProposalProb = relProbs[21];
	mt->proposalParam[0] = proParam[21][0]; /* tuning parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(covarion switch rates) with multiplier";
	strcpy (mt->shortName, "mult");
	mt->applicableTo[0] = SWITCH_UNI;
	mt->applicableTo[1] = SWITCH_EXP;
	mt->nApplicable = 2;
	mt->moveFxn = &Move_SwitchRate_M;
	mt->relProposalProb = relProbs[22];
	mt->proposalParam[0] = proParam[22][0]; /* tuning parameter */
	mt->parsimonyBased = NO;

	i++;
	
	mt = &moveTypes[i++];
	mt->name = "(speciation rate) with multiplier";
	strcpy (mt->shortName, "mult");
	mt->applicableTo[0] = SPECRATE_UNI;
	mt->applicableTo[1] = SPECRATE_EXP;
	mt->nApplicable = 2;
	mt->moveFxn = &Move_Speciation_M;
	mt->relProposalProb = relProbs[23];
	mt->proposalParam[0] = proParam[23][0]; /* tuning parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(extinction rate) with multiplier";
	strcpy (mt->shortName, "mult");
	mt->applicableTo[0] = EXTRATE_UNI;
	mt->applicableTo[1] = EXTRATE_EXP;
	mt->nApplicable = 2;
	mt->moveFxn = &Move_Extinction_M;
	mt->relProposalProb = relProbs[24];
	mt->proposalParam[0] = proParam[24][0]; /* tuning parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(clock rate) with multiplier";
	strcpy (mt->shortName, "mult");
	mt->applicableTo[0] = BRLENS_CCLOCK_UNI;
	mt->applicableTo[1] = BRLENS_CCLOCK_COAL;
	mt->applicableTo[2] = BRLENS_CCLOCK_BD;
	mt->nApplicable = 3;
	mt->moveFxn = &Move_ClockRate;
	mt->relProposalProb = relProbs[25];
	mt->proposalParam[0] = proParam[25][0]; /* tuning parameter */
	mt->parsimonyBased = NO;

	/* the following move is in principle also applicable to TOPOLOGY_NCL_UNIFORM_HOMO */
	mt = &moveTypes[i++];
	mt->name = "(topology and branch lengths) with NNI";
	strcpy (mt->shortName, "hNNI");
	mt->applicableTo[0] = TOPOLOGY_NCL_UNIFORM_HETERO;
	/* mt->applicableTo[1] = TOPOLOGY_NCL_UNIFORM_HOMO; */
	mt->nApplicable = 1; /* 2; */
	mt->moveFxn = &Move_NNI_Hetero;
	mt->relProposalProb = relProbs[26];
	mt->proposalParam[0] = proParam[26][0]; /* tuning parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(branch lengths) with nodeslider";
	strcpy (mt->shortName, "nslider");
	mt->applicableTo[0] = BRLENS_UNI;
	mt->applicableTo[1] = BRLENS_EXP;
	mt->nApplicable = 2;
	mt->moveFxn = &Move_NodeSlider;
	mt->relProposalProb = relProbs[27];
	mt->proposalParam[0] = proParam[27][0]; /* tuning parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(coalescence growth parameter) with sliding window";
	strcpy (mt->shortName, "slider");
	mt->applicableTo[0] = GROWTH_UNI;
	mt->applicableTo[1] = GROWTH_EXP;
	mt->applicableTo[2] = GROWTH_NORMAL;
	mt->nApplicable = 3;
	mt->moveFxn = &Move_Growth;
	mt->relProposalProb = relProbs[28];
	mt->proposalParam[0] = proParam[28][0]; /* sliding window size */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(omega 2) with sliding window";
	strcpy (mt->shortName, "slider");
	mt->applicableTo[0] = OMEGA_ED;
	mt->applicableTo[1] = OMEGA_EF;
	mt->nApplicable = 2;
	mt->moveFxn = &Move_OmegaNeu;
	mt->relProposalProb = relProbs[29];
	mt->proposalParam[0] = proParam[29][0]; /* sliding window size */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(tratio) with Dirichlet proposal";
	strcpy (mt->shortName, "Dirichlet");
	mt->applicableTo[0] = TRATIO_DIR;
	mt->nApplicable = 1;
	mt->moveFxn = &Move_Tratio_Dir;
	mt->relProposalProb = relProbs[30];		
	mt->proposalParam[0] = proParam[30][0]; /* alphaPi parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(revmat) with Dirichlet proposal";
	strcpy (mt->shortName, "Dirichlet");
	mt->applicableTo[0] = REVMAT_DIR;
	mt->nApplicable = 1;
	mt->moveFxn = &Move_Revmat_Dir;
	mt->relProposalProb = relProbs[31];
	mt->proposalParam[0] = proParam[31][0]; /* alphaPi parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(omega beta params.) with multiplier";
	strcpy (mt->shortName, "mult");
	mt->applicableTo[0] = OMEGA_10UUB;
	mt->applicableTo[1] = OMEGA_10UUF;
	mt->applicableTo[2] = OMEGA_10UEB;
	mt->applicableTo[3] = OMEGA_10UEF;
	mt->applicableTo[4] = OMEGA_10UFB;
	mt->applicableTo[5] = OMEGA_10UFF;
	mt->applicableTo[6] = OMEGA_10EUB;
	mt->applicableTo[7] = OMEGA_10EUF;
	mt->applicableTo[8] = OMEGA_10EEB;
	mt->applicableTo[9] = OMEGA_10EEF;
	mt->applicableTo[10] = OMEGA_10EFB;
	mt->applicableTo[11] = OMEGA_10EFF;
	mt->nApplicable = 12;
	mt->moveFxn = &Move_OmegaBeta_M;
	mt->relProposalProb = relProbs[32];
	mt->proposalParam[0] = proParam[32][0]; 
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(omega gamma params.) with multiplier";
	strcpy (mt->shortName, "Dirichlet");
	mt->applicableTo[0] = OMEGA_10UUB;
	mt->applicableTo[1] = OMEGA_10UUF;
	mt->applicableTo[2] = OMEGA_10UEB;
	mt->applicableTo[3] = OMEGA_10UEF;
	mt->applicableTo[4] = OMEGA_10EUB;
	mt->applicableTo[5] = OMEGA_10EUF;
	mt->applicableTo[6] = OMEGA_10EEB;
	mt->applicableTo[7] = OMEGA_10EEF;
	mt->applicableTo[8] = OMEGA_10FUB;
	mt->applicableTo[9] = OMEGA_10FUF;
	mt->applicableTo[10] = OMEGA_10FEB;
	mt->applicableTo[11] = OMEGA_10FEF;
	mt->nApplicable = 12;
	mt->moveFxn = &Move_OmegaGamma_M;
	mt->relProposalProb = relProbs[33];
	mt->proposalParam[0] = proParam[33][0]; 
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(topology and branch lengths) with branch slider";
	strcpy (mt->shortName, "bslider");
	mt->applicableTo[0] = TOPOLOGY_NCL_UNIFORM_HOMO;
	mt->nApplicable = 1;
	mt->moveFxn = &Move_UnrootedSlider;
	mt->relProposalProb = relProbs[34];
	mt->proposalParam[0] = proParam[34][0]; /* "tuning" parameter */
	mt->proposalParam[1] = proParam[34][1]; /* exponential parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(topology and branch lengths) with biased SPR";
	strcpy (mt->shortName, "bSPR");
	mt->applicableTo[0] = TOPOLOGY_NCL_UNIFORM_HOMO;
	mt->nApplicable = 1;
	mt->moveFxn = &Move_BiasedSpr;
	mt->relProposalProb = relProbs[35];
	mt->proposalParam[0] = proParam[35][0]; /* "tuning" parameter */
	mt->proposalParam[1] = proParam[35][1]; /* warp parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(rate multiplier) with Dirichlet proposal";
	strcpy (mt->shortName, "Dirichlet");
	mt->applicableTo[0] = RATEMULT_DIR;
	mt->nApplicable = 1;
	mt->moveFxn = &Move_RateMult_Dir;
	mt->relProposalProb = relProbs[36];
	mt->proposalParam[0] = proParam[36][0]; /* alphaPi parameter */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(topology and branch lengths) with extending SPR (clock)";
	strcpy (mt->shortName, "ceSPR");
	mt->applicableTo[0] = TOPOLOGY_CL_UNIFORM_HOMO;
	mt->applicableTo[1] = TOPOLOGY_CCL_UNIFORM_HOMO;
	mt->applicableTo[2] = TOPOLOGY_CL_CONSTRAINED_HOMO;
	mt->applicableTo[3] = TOPOLOGY_CCL_CONSTRAINED_HOMO;
	mt->nApplicable = 4;
	mt->moveFxn = &Move_ExtSPRClock;
	mt->relProposalProb = relProbs[37];
	mt->proposalParam[0] = proParam[37][0]; /* tuning parameter (lambda)*/
	mt->proposalParam[1] = proParam[37][1]; /* extension probability */
	mt->parsimonyBased = NO;

	mt = &moveTypes[i++];
	mt->name = "(topology and branch lengths) with parsimony eraser 1";
	strcpy (mt->shortName, "pEraser1");
	mt->applicableTo[0] = TOPOLOGY_NCL_UNIFORM_HOMO;
	mt->nApplicable = 1;
	mt->moveFxn = &Move_ParsEraser1;
	mt->relProposalProb = relProbs[38];
	mt->proposalParam[0] = proParam[38][0]; /* alphaPi */
	mt->proposalParam[1] = proParam[38][1]; /* parsimony warp factor */
	mt->parsimonyBased = YES;

	numMoveTypes = i;
	
}





/*----------------------------------------------------------------------
|
|   SetUpPartitionCounters: Set up partitions and the root of the
|      partition frequency tree							
|
|----------------------------------------------------------------------*/
int SetUpPartitionCounters (void)
{
	int		i;
	
#	if defined (MPI_ENABLED)
	/* we only keep partition counters on proc 0 in the MPI version */
	if (proc_id != 0)
		return (NO_ERROR);
#	endif
	nLongsNeeded = 1 + numLocalTaxa / nBitsInALong;
	
	if (memAllocs[ALLOC_PFCOUNTERS] == YES)
		{
		MrBayesPrint ("%s   ERROR: pfcounters not free in SetUpPartitionCounters\n", spacer);
		return ERROR;
		}
	partition = (long **) calloc (2*numLocalTaxa, sizeof (long *));
	if (partition == NULL)
		{
		MrBayesPrint ("%s   Failed to allocate partition in SetUpPartitionCounters\n", spacer);
		return ERROR;
		}
	partition[0] = (long *) calloc (2*numLocalTaxa * nLongsNeeded, sizeof(long));
	if (partition[0] == NULL)
		{
		free (partition);
		MrBayesPrint ("%s   Failed to allocate partition[0] in SetUpPartitionCounters\n", spacer);
		return ERROR;
		}
	partFreqTreeRoot = (PFNODE **) calloc (numTrees, sizeof (PFNODE *));
	if (partFreqTreeRoot == NULL)
		{
		free (partition);
		free (partition[0]);
		MrBayesPrint ("%s   Failed to allocate partFreqTreeRoot in SetUpPartitionCounters\n", spacer);
		return ERROR;
		}
	memAllocs[ALLOC_PFCOUNTERS] = YES;

	for (i=1; i<2*numLocalTaxa; i++)
		{
		partition[i] = partition[0] + i*nLongsNeeded;
		}
	
	for (i=0; i<numLocalTaxa; i++)
		SetBit (i, partition[i]);

	for (i=0; i<numTrees; i++)
		partFreqTreeRoot[i] = NULL;

	return NO_ERROR;
}





/*----------------------------------------------------------------------
|
|	SetupTermState: create matrix holding unambiguous states for
|		terminals (used for local compression on terminal branches)
|
-----------------------------------------------------------------------*/
int SetUpTermState (void)

{

	int			i, j, k, n, c, d, x=0, nReps;
	long int	*p;
	ModelInfo	*m;
	ModelParams *mp;

	/* allocate space for termState and isPartAmbig */
	if (memAllocs[ALLOC_TERMSTATE] == YES || memAllocs[ALLOC_ISPARTAMBIG] == YES)
		{
		MrBayesPrint ("%s   termState or isPartAmbig is not free in SetupTermState\n", spacer);
		return ERROR;
		}
	termState = (int *) calloc (numLocalTaxa * numCompressedChars, sizeof(int));
	if (termState)
		memAllocs[ALLOC_TERMSTATE] = YES;
	else
		{
		MrBayesPrint ("%s   Problem allocating termState\n", spacer);
		return (ERROR);
		}
	isPartAmbig = (int *) calloc (numLocalTaxa*numCurrentDivisions, sizeof(int));
	if (isPartAmbig)
		memAllocs[ALLOC_ISPARTAMBIG] = YES;
	else
		{
		MrBayesPrint ("%s   Problem allocating isPartAmbig\n", spacer);
		return (ERROR);
		}

	/*initialize isPartAmbig */
	for (i=0; i<numLocalTaxa*numCurrentDivisions; i++)
		isPartAmbig[i] = NO;

	/* loop over divisions */
	for (d=0; d<numCurrentDivisions; d++)
		{
		m = &modelSettings[d];
		mp = &modelParams[d];

		/* don't do anything for continuous data */
		if (mp->dataType == CONTINUOUS)
			continue;
		
		if (m->numModelStates > mp->nStates)
			nReps = m->numModelStates / mp->nStates;

		m->isPartAmbig = isPartAmbig + d * numLocalTaxa;

		for (i=0; i<numLocalTaxa; i++)
			{
			j = m->parsMatrixStart;
			for (c=m->compCharStart; c<m->compCharStop; c++)
				{
				p = &parsMatrix[pos(i,j,parsMatrixRowSize)];
				for (k=n=0; k<mp->nStates; k++)
					{
					if (IsBitSet(k, p))
						{
						x = k;
						n++;
						}
					}
				/* find appropriate index */
				if (n == 1)
					termState[pos(i,c,numCompressedChars)] = x * m->numModelStates;
				else if (n == mp->nStates)
					termState[pos(i,c,numCompressedChars)] = mp->nStates * m->numModelStates;
				else
					m->isPartAmbig[i] = YES;

				j += m->nParsIntsPerSite;

				}
			}
		}


	/* print the termState matrix */
#	if	defined (DEBUG_SETUPTERMSTATE)
	PrintTermState();
	getchar();
#	endif

	return NO_ERROR;
	
}





int ShowMCMCTree (Tree *t)

{

	int 			i, j, k, x, nLines, nLevels, levelDepth, from, to;
	char			treeLine[SCREENWIDTH2], labelLine[100];
	TreeNode		*p;
	
	/* get coordinates */
	x = 0;
	nLines = 0;
	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		if (p->left == NULL && p->right == NULL)
			{
			p->x = x;
			x += 2;
			p->y = 0;
			nLines += 2;
			}
		else if (p->left != NULL && p->right != NULL && p->anc != NULL)
			{
			p->x = p->left->x + (p->right->x - p->left->x) / 2;
			if (p->left->y > p->right->y)
				p->y = p->left->y + 1;
			else
				p->y = p->right->y + 1;
			}
		else
			{
			p->x = x;
			x += 2;
			p->y = 0;
			}
		} 

	/* print tree out, line-by-line */
	levelDepth = SCREENWIDTH / t->root->left->y;
	nLevels = t->root->left->y;
	for (j=0; j<=nLines-2; j++)
		{
		if (j % 2 == 0)
			{
			for (i=0; i<t->nNodes; i++)
				{
				p = t->allDownPass[i];
				if (p->left == NULL && p->x == j)
					{
					strcpy (labelLine, p->label);
					}
				}
			}
			
		for (i=0; i<SCREENWIDTH-1; i++)
			treeLine[i] = ' ';
		treeLine[SCREENWIDTH-1] = '\0';
		for (i=0; i<t->nNodes; i++)
			{
			p = t->allDownPass[i];
			if (p->anc != NULL)
				{
				if (p->anc->anc != NULL)
					{
					if (p->x == j)
						{
						from = (nLevels - p->anc->y) * levelDepth;
						to   = (nLevels - p->y) * levelDepth;
						if (p->y == 0)
							to = SCREENWIDTH-1;
						if (to >= SCREENWIDTH)
							to = SCREENWIDTH-1;
							
						for (k=from; k<to; k++)
							treeLine[k] = '-';
						if (p->anc->left == p)
							treeLine[from] = '/';
						else
							treeLine[from] = '\\';
						if (p->left != NULL)
							{
							treeLine[to] = '+';
							}
						if (p->anc->anc == t->root && p->anc->right == p)
							{
							if (t->isRooted == NO)
								treeLine[to] = '+';
							else
								treeLine[from] = '\\';
							}
						}
					else
						{
						if (p->left != NULL && p->right != NULL)
							{
							if (j < p->x && j > p->left->x)
								{
								from = (nLevels - p->y) * levelDepth;
								treeLine[from] = '|';
								}
							else if (j > p->x && j < p->right->x && p->left != NULL)
								{
								from = (nLevels - p->y) * levelDepth;
								treeLine[from] = '|';
								}
							}
						}
					}
				else
					{
					if (p->x == j)
						{
						treeLine[0] = '|'; /* temp */
						}
					else if (j < p->x && j > p->left->x)
						{
						treeLine[0] = '|';
						}
					else if (j > p->x && j < p->right->x)
						{
						treeLine[0] = '|';
						}
					if (t->isRooted == NO)
						{
						if (j > p->x && j <= nLines-2)
							treeLine[0] = '|';
						if (j == p->right->x)
							treeLine[0] = '+';
						}
					else
						{
						if (j == p->x)
							treeLine[0] = '+';
						}
					}
				}
			}
		treeLine[SCREENWIDTH-1] = '\0';
		if (j % 2 == 0)
			MrBayesPrint ("   %s %s\n", treeLine, labelLine);
		else
			MrBayesPrint ("   %s \n", treeLine);
		}

	if (t->isRooted == NO)
		{
		for (i=0; i<SCREENWIDTH; i++)
			treeLine[i] = ' ';
		treeLine[SCREENWIDTH-1] = '\0';
		MrBayesPrint ("   |\n");
		for (k=0; k<SCREENWIDTH; k++)
			treeLine[k] = '-';
		treeLine[SCREENWIDTH-1] = '\0';
		treeLine[0] = '\\';
		strcpy (labelLine, t->root->label);
		labelLine[19] = '\0';
		MrBayesPrint ("   %s %s\n", treeLine, labelLine);
		}
	
	return (NO_ERROR);
	   
}





void ShowValuesForChain (int chn)

{

	int				i;
	char			s[100];
		
	MrBayesPrint ("%s   Chain = %d\n", spacer, chn);
	MrBayesPrint ("%s      numParams = %d\n", spacer, numParams);
	MrBayesPrint ("%s      numTrees  = %d\n", spacer, numTrees);
	MrBayesPrint ("%s      current state: %d\n", spacer, state[chn]);
	
	strcat (spacer, "   ");

	/* tRatio */
	for (i=0; i<numCurrentDivisions; i++)
		{
		sprintf (s, "tRatio[%d]", i);
		PrintParamValues (modelSettings[i].tRatio, chn, s);
		}

	/* revMat */
	for (i=0; i<numCurrentDivisions; i++)
		{
		sprintf (s, "revMat[%d]", i);
		PrintParamValues (modelSettings[i].revMat, chn, s);
		}

	/* stateFreq */
	for (i=0; i<numCurrentDivisions; i++)
		{
		sprintf (s, "stateFreq[%d]", i);
		PrintParamValues (modelSettings[i].stateFreq, chn, s);
		}

	/* omega */
	for (i=0; i<numCurrentDivisions; i++)
		{
		sprintf (s, "omega[%d]", i);
		PrintParamValues (modelSettings[i].omega, chn, s);
		}

	/* shape */
	for (i=0; i<numCurrentDivisions; i++)
		{
		sprintf (s, "shape[%d]", i);
		PrintParamValues (modelSettings[i].shape, chn, s);
		}

	/* pInvar */
	for (i=0; i<numCurrentDivisions; i++)
		{
		sprintf (s, "pInvar[%d]", i);
		PrintParamValues (modelSettings[i].pInvar, chn, s);
		}

	/* correlation */
	for (i=0; i<numCurrentDivisions; i++)
		{
		sprintf (s, "correlation[%d]", i);
		PrintParamValues (modelSettings[i].correlation, chn, s);
		}

	/* switchRates */
	for (i=0; i<numCurrentDivisions; i++)
		{
		sprintf (s, "switchRates[%d]", i);
		PrintParamValues (modelSettings[i].switchRates, chn, s);
		}

	/* rateMult */
	for (i=0; i<numCurrentDivisions; i++)
		{
		sprintf (s, "rateMult[%d]", i);
		PrintParamValues (modelSettings[i].rateMult, chn, s);
		}

	/* speciationRates */
	for (i=0; i<numCurrentDivisions; i++)
		{
		sprintf (s, "speciationRates[%d]", i);
		PrintParamValues (modelSettings[i].speciationRates, chn, s);
		}

	/* extinctionRates */
	for (i=0; i<numCurrentDivisions; i++)
		{
		sprintf (s, "extinctionRates[%d]", i);
		PrintParamValues (modelSettings[i].extinctionRates, chn, s);
		}

	/* theta */
	for (i=0; i<numCurrentDivisions; i++)
		{
		sprintf (s, "theta[%d]", i);
		PrintParamValues (modelSettings[i].theta, chn, s);
		}

	/* topology */
	for (i=0; i<numCurrentDivisions; i++)
		{
		MrBayesPrint ("%s   topology[%d] state 0\n", spacer, i);
		ShowMCMCTree(GetTree (modelSettings[i].topology, chn, 0));
		MrBayesPrint ("%s   topology[%d] state 1\n", spacer, i);
		ShowMCMCTree(GetTree (modelSettings[i].topology, chn, 1));
		}
		
	/* brlens */
	for (i=0; i<numCurrentDivisions; i++)
		{
		MrBayesPrint ("%s   tree[%d] state 0\n", spacer, i);
		ShowMCMCTree(GetTree (modelSettings[i].topology, chn, 0));
		MrBayesPrint ("%s   tree[%d] state 1\n", spacer, i);
		ShowMCMCTree(GetTree (modelSettings[i].topology, chn, 1));
		}

	spacer[strlen(spacer) - 3] = '\0';

#	if	0
	for (i=0; i<sizeOfParamValues; i++)
		MrBayesPrint ("%4d -- %lf\n", i, paramValues[i]);
#	endif

}





/* SmallestNonemptyPFNode: recursive function to smallest nonempty node in a subtree */
PFNODE *SmallestNonemptyPFNode (PFNODE *p, int *i, int j)
{
	PFNODE *q;

	++j;
	if (p == NULL)
		return NULL;
	
	q = SmallestNonemptyPFNode (p->right, i, j);
	
	if (q != NULL)
		{
		return q;
		}
	else if (IsPFNodeEmpty (p) == NO)
		{
		*i = j;
		return p;
		}
	else
		{
		return SmallestNonemptyPFNode (p->left, i, j);
		}
}





/* Talloc: Allocate space for a new node in the tree keeping track of partition frequencies */
PFNODE *Talloc (void)
{
	PFNODE	*temp;

	temp = (PFNODE *) malloc (sizeof(PFNODE));
	if (temp == NULL)
		return NULL;

	temp->partition = (long *) calloc (nLongsNeeded, sizeof (long));
	if (temp->partition == NULL)
		{
		free (temp);
		return NULL;
		}

	temp->count = (int *) calloc (chainParams.numRuns, sizeof (int));
	if (temp->count == NULL)
		{
		free (temp->partition);
		free (temp);
		return NULL;
		}

	return temp; 
}





MrBFlt Temperature (int id)

{

	/* let id be number of chain in run */
	id %= chainParams.numChains;
	
	if (chainParams.userDefinedTemps == YES)
		{
		return (chainParams.userTemps[id]);
		}
	else
		{
		return (1.0 / (1.0 + chainParams.chainTemp * id));
		}

}





/* Tfree: Free space for partition frequency counter tree */
void Tfree (PFNODE *r)
{
	if (r != NULL)
		{
		if (r->left != NULL)
			Tfree (r->left);
		if (r->right != NULL)
			Tfree (r->right);

		free (r->partition);
		free (r->count);
		free (r);
		}
}





int TiProbs_Fels (TreeNode *p, int division, int chain)

{

	int			i, j, k, index;
	MrBFlt		a, t, u, x, z, beta, bigPi_j[4], pij, bigPij,
				*catRate, baseRate, theRate, *pis;
	CLFlt		*tiP;
	ModelInfo	*m;
#	if defined SSE
	float		temp[16];
#	endif
	a = 0.0;

	m = &modelSettings[division];

	/* find transition probabilities */
	tiP = tiProbs[chain] + m->tiProbStart + (2*p->index + Bit(division, &p->tiSpace[0])) * tiProbRowSize;

	/* get base frequencies */
	pis = GetParamSubVals (m->stateFreq, chain, state[chain]);
	
	/* get rate multipliers (for gamma & partition specific rates) */
	theRate =  1.0;
	baseRate =  GetRate (division, chain);
	/* compensate for invariable sites if appropriate */
	if (m->pInvar != NULL)
		baseRate /= ( 1.0 - ( *GetParamVals(m->pInvar, chain, state[chain])));
	/* get category rates */
	if (m->shape == NULL)
		catRate = &theRate;
	else
		catRate = GetParamSubVals (m->shape, chain, state[chain]);
	
	/* rescale beta */
	beta =  (0.5 / ((pis[0] + pis[2])*(pis[1] + pis[3]) + ((pis[0]*pis[2]) + (pis[1]*pis[3]))));

	bigPi_j[0] =  (pis[0] + pis[2]);
	bigPi_j[1] =  (pis[1] + pis[3]);
	bigPi_j[2] =  (pis[0] + pis[2]);
	bigPi_j[3] =  (pis[1] + pis[3]);

	/* fill in values */
	for (k=index=0; k<m->numGammaCats; k++)
		{
		t =  pow(0.05, a) * pow (p->length, 1 - a) * baseRate * catRate[k];

		/* calculate probabilities */
		for (i=0; i<4; i++)
			{
			for (j=0; j<4; j++)
				{
				bigPij = bigPi_j[j];
				pij =  pis[j];
				u =  1.0/bigPij -  1.0;
				x =  exp(-beta * t);
				z = (bigPij - pij) / bigPij;
				
				if (i == j)
					tiP[index++] = (CLFlt) (pij + pij * u * x + z * x);
				else
					tiP[index++] = (CLFlt) (pij + pij * u * x - (pij/bigPij) * x);
				}
			}
		}

#if defined SSE
	/* rearrange tiprobs to fit shuffling SSE */
	for (k=0; k<m->numGammaCats; k++)
		{
		temp[0] = tiP[0];
		temp[1] = tiP[1];
		temp[2] = tiP[2];
		temp[3] = tiP[3];
		temp[4] = tiP[4];
		temp[5] = tiP[5];
		temp[6] = tiP[6];
		temp[7] = tiP[7];
		temp[8] = tiP[8];
		temp[9] = tiP[9];
		temp[10] = tiP[10];
		temp[11] = tiP[11];
		temp[12] = tiP[12];
		temp[13] = tiP[13];
		temp[14] = tiP[14];
		temp[15] = tiP[15];
		tiP[0] = temp[0];
		tiP[1] = temp[5];
		tiP[2] = temp[10];
		tiP[3] = temp[15];
		tiP[4] = temp[1];
		tiP[5] = temp[6];
		tiP[6] = temp[11];
		tiP[7] = temp[12];
		tiP[8] = temp[2];
		tiP[9] = temp[7];
		tiP[10] = temp[8];
		tiP[11] = temp[13];
		tiP[12] = temp[3];
		tiP[13] = temp[4];
		tiP[14] = temp[9];
		tiP[15] = temp[14];
		tiP += 16;
		}
#endif

	return NO_ERROR;

}





/*----------------------------------------------------------------
|
|	TiProbs_Gen: Calculates transition probabilities for general
|		models with or without rate variation. This function does
|       not work with:
|      
|       1. codon models with omega variation or
|       2. covarion models with rate variation
|
|   In either of these cases, TiProbs_GenCov is used
|
-----------------------------------------------------------------*/
int TiProbs_Gen (TreeNode *p, int division, int chain)

{
	
	register int	i, j, k, n, s, index;
	MrBFlt			t, *catRate, baseRate, *eigenValues, *cijk, 
					EigValexp[64], sum, *ptr, theRate, correctionFactor;
	CLFlt			*tiP;
	ModelInfo		*m;
#if defined SSE
	float			temp[16];
#endif
	
	m = &modelSettings[division];
	n = m->numModelStates;
	
	/* find the correction factor to make branch lengths
	   in terms of expected number of substitutions per character */
	correctionFactor = 1.0;
	if (m->dataType == DNA || m->dataType == RNA)
		{
		if (m->nucModelId == NUCMODEL_DOUBLET)
			correctionFactor = 2.0;
		else if (m->nucModelId == NUCMODEL_CODON)
			correctionFactor = 3.0;
		}

	/* update cijk if necessary */
	if (m->upDateCijk[chain][state[chain]] == YES)
		{
		if (UpDateCijk (division, chain) == ERROR)
			return (ERROR);
		}

	/* find transition probabilities */
	tiP = tiProbs[chain] + m->tiProbStart + (2*p->index + Bit(division, &p->tiSpace[0])) * tiProbRowSize;
	
	/* get rate multipliers (for gamma & partition specific rates) */
	theRate = 1.0;
	baseRate =  GetRate (division, chain);
	
	/* compensate for invariable sites if appropriate */
	if (m->pInvar != NULL)
		baseRate /= ( 1.0 - ( *GetParamVals(m->pInvar, chain, state[chain])));
		
	/* get category rates */
	if (m->shape == NULL)
		catRate = &theRate;
	else
		catRate = GetParamSubVals (m->shape, chain, state[chain]);
		
	/* get eigenvalues and cijk pointers */
	eigenValues = cijks[chain] + m->cijkStart + m->cijkBits[chain][state[chain]] * cijkRowSize;
	cijk        = eigenValues + (2 * n);

	/* fill in values */
	for (k=index=0; k<m->numGammaCats; k++)
		{
		t =  p->length * baseRate * catRate[k] * correctionFactor;
		
		for (s=0; s<n; s++)
			EigValexp[s] =  exp(eigenValues[s] * t);

		ptr = cijk;
		for (i=0; i<n; i++)
			{
			for (j=0; j<n; j++)
				{
				sum = 0.0;
				for(s=0; s<n; s++)
					sum += (*ptr++) * EigValexp[s];
				tiP[index++] = (CLFlt) ((sum < 0.0) ? 0.0 : sum);
				}
			}
		}

#	if 0
	printf ("v = %lf (%d)\n", t, p->index);
	for (i=index=0; i<n; i++)
		{
		for (j=0; j<n; j++)
			printf ("%1.4lf ", tiP[index++]);
		printf ("\n");
		}
	printf ("\n");
#	endif

#if defined SSE
	if ((m->dataType == DNA || m->dataType == RNA) && m->nucModelId == NUCMODEL_4BY4)
		{
		/* rearrange tiprobs to fit shuffling SSE */
		for (k=0; k<m->numGammaCats; k++)
			{
			temp[0] = tiP[0];
			temp[1] = tiP[1];
			temp[2] = tiP[2];
			temp[3] = tiP[3];
			temp[4] = tiP[4];
			temp[5] = tiP[5];
			temp[6] = tiP[6];
			temp[7] = tiP[7];
			temp[8] = tiP[8];
			temp[9] = tiP[9];
			temp[10] = tiP[10];
			temp[11] = tiP[11];
			temp[12] = tiP[12];
			temp[13] = tiP[13];
			temp[14] = tiP[14];
			temp[15] = tiP[15];
			tiP[0] = temp[0];
			tiP[1] = temp[5];
			tiP[2] = temp[10];
			tiP[3] = temp[15];
			tiP[4] = temp[1];
			tiP[5] = temp[6];
			tiP[6] = temp[11];
			tiP[7] = temp[12];
			tiP[8] = temp[2];
			tiP[9] = temp[7];
			tiP[10] = temp[8];
			tiP[11] = temp[13];
			tiP[12] = temp[3];
			tiP[13] = temp[4];
			tiP[14] = temp[9];
			tiP[15] = temp[14];
			tiP += 16;
			}
		}
#endif

	return NO_ERROR;

}





/*----------------------------------------------------------------
|
|	TiProbs_GenCov: Calculates transition probabilities for codon
|		models with omega variation or covarion models with
|       rate variation.
|
-----------------------------------------------------------------*/
int TiProbs_GenCov (TreeNode *p, int division, int chain)

{
	
	register int	i, j, k, n, s, index;
	int				sizeOfSingleCijk;
	MrBFlt			t, *eigenValues, *cijk, EigValexp[64], sum, *ptr, correctionFactor;
	CLFlt			*tiP;
	ModelInfo		*m;
	
	m = &modelSettings[division];
	n = m->numModelStates;
	
	/* find the correction factor to make branch lengths
	   in terms of expected number of substitutions per character */
	correctionFactor = 1.0;
	if (m->dataType == DNA || m->dataType == RNA)
		{
		if (m->nucModelId == NUCMODEL_DOUBLET)
			correctionFactor = 2.0;
		else if (m->nucModelId == NUCMODEL_CODON)
			correctionFactor = 3.0;
		}

	/* update cijk if necessary */
	if (m->upDateCijk[chain][state[chain]] == YES)
		{
		if (UpDateCijk (division, chain) == ERROR)
			return (ERROR);
		}

	/* find transition probabilities */
	tiP = tiProbs[chain] + m->tiProbStart + (2*p->index + Bit(division, &p->tiSpace[0])) * tiProbRowSize;
			
	/* get eigenvalues and cijk pointers */
	eigenValues = cijks[chain] + m->cijkStart + m->cijkBits[chain][state[chain]] * cijkRowSize;
	cijk        = eigenValues + (2 * n);
	
	/* get offset size (we need to move the pointers to the appropriate
	   cijk information for these models) */
	sizeOfSingleCijk = m->nCijk / m->nCijkParts;

	/* fill in values */
	for (k=index=0; k<m->nCijkParts; k++)
		{
		t =  p->length * correctionFactor;
		for (s=0; s<n; s++)
			EigValexp[s] =  exp(eigenValues[s] * t);

		ptr = cijk;
		for (i=0; i<n; i++)
			{
			for (j=0; j<n; j++)
				{
				sum = 0.0;
				for(s=0; s<n; s++)
					sum += (*ptr++) * EigValexp[s];
				tiP[index++] = (CLFlt) ((sum < 0.0) ? 0.0 : sum);
				}
			}
			
		/* increment pointers by m->nCijk */
		if (k+1 < m->nCijkParts)
			{
			/* shift pointers */
			eigenValues += sizeOfSingleCijk;
			cijk        += sizeOfSingleCijk;
			}
		}
		
#	if 0
	for (i=index=0; i<n; i++)
		{
		for (j=0; j<n; j++)
			printf ("%1.4lf ", tiP[index++]);
		printf ("\n");
		}
#	endif

	return NO_ERROR;

}





/*-----------------------------------------------------------------
|
|	TiProbs_Hky: update transition probabilities for 4by4
|		nucleotide model with nst == 2 (K80/HKY85)
|		with or without rate variation
|
------------------------------------------------------------------*/
int TiProbs_Hky (TreeNode *p, int division, int chain)

{

	int			i, j, k, index;
	MrBFlt		t, kap, u, w, x, y, z, beta, bigPi_j[4], pij, bigPij, *pis,
				*catRate, baseRate, theRate;
	CLFlt		*tiP;
	ModelInfo	*m;
#if defined SSE
	float		temp[16];
#endif
	
	m = &modelSettings[division];

	/* find transition probabilities */
	tiP = tiProbs[chain] + m->tiProbStart + (2*p->index + Bit(division, &p->tiSpace[0])) * tiProbRowSize;

	/* get kappa */
	kap =  *GetParamVals (m->tRatio, chain, state[chain]);
	
	/* get base frequencies */
	pis = GetParamSubVals (m->stateFreq, chain, state[chain]);
	
	/* get rate multipliers (for gamma & partition specific rates) */
	theRate = 1.0;
	baseRate =  GetRate (division, chain);
	/* compensate for invariable sites if appropriate */
	if (m->pInvar != NULL)
		baseRate /= ( 1.0 - ( *GetParamVals(m->pInvar, chain, state[chain])));
	/* get category rates */
	if (m->shape == NULL)
		catRate = &theRate;
	else
		catRate = GetParamSubVals (m->shape, chain, state[chain]);
	
	/* rescale beta */
	beta =  0.5 / ((pis[0] + pis[2])*(pis[1] + pis[3]) + kap*((pis[0]*pis[2]) + (pis[1]*pis[3])));

	bigPi_j[0] = pis[0] + pis[2];
	bigPi_j[1] = pis[1] + pis[3];
	bigPi_j[2] = pis[0] + pis[2];
	bigPi_j[3] = pis[1] + pis[3];

	/* fill in values */
	for (k=index=0; k<m->numGammaCats; k++)
		{
		t =  p->length * baseRate * catRate[k];

		/* calculate probabilities */
		for (i=0; i<4; i++)
			{
			for (j=0; j<4; j++)
				{
				bigPij = bigPi_j[j];
				pij = pis[j];
				u =  1.0/bigPij -  1.0;
				w = -beta * ( 1.0 + bigPij * (kap -  1.0));
				x =  exp(-beta * t);
				y =  exp(w * t);
				z = (bigPij - pij) / bigPij;
				
				if (i == j)
					tiP[index++] = (CLFlt) (pij + pij * u * x + z * y);
				else if ((i == 0 && j == 2) || (i == 2 && j == 0) || (i == 1 && j == 3) || (i == 3 && j == 1))
					tiP[index++] = (CLFlt) (pij + pij * u * x - (pij/bigPij) * y);
				else
					tiP[index++] = (CLFlt) (pij * ( 1.0 - x));
				}
			}
		}
		
#if defined SSE
	/* rearrange tiprobs to fit shuffling SSE */
	for (k=0; k<m->numGammaCats; k++)
		{
		temp[0] = tiP[0];
		temp[1] = tiP[1];
		temp[2] = tiP[2];
		temp[3] = tiP[3];
		temp[4] = tiP[4];
		temp[5] = tiP[5];
		temp[6] = tiP[6];
		temp[7] = tiP[7];
		temp[8] = tiP[8];
		temp[9] = tiP[9];
		temp[10] = tiP[10];
		temp[11] = tiP[11];
		temp[12] = tiP[12];
		temp[13] = tiP[13];
		temp[14] = tiP[14];
		temp[15] = tiP[15];
		tiP[0] = temp[0];
		tiP[1] = temp[5];
		tiP[2] = temp[10];
		tiP[3] = temp[15];
		tiP[4] = temp[1];
		tiP[5] = temp[6];
		tiP[6] = temp[11];
		tiP[7] = temp[12];
		tiP[8] = temp[2];
		tiP[9] = temp[7];
		tiP[10] = temp[8];
		tiP[11] = temp[13];
		tiP[12] = temp[3];
		tiP[13] = temp[4];
		tiP[14] = temp[9];
		tiP[15] = temp[14];
		tiP += 16;
		}
#endif

	return NO_ERROR;

}





/*-----------------------------------------------------------------
|
|	TiProbs_JukesCantor: update transition probabilities for 4by4
|		nucleotide model with nst == 1 (Jukes-Cantor)
|		with or without rate variation
|
------------------------------------------------------------------*/
int TiProbs_JukesCantor (TreeNode *p, int division, int chain)

{
	
	/* calculate Jukes Cantor transition probabilities */
	
	int			i, j, k, index;
	MrBFlt		*catRate, baseRate;
	CLFlt		pNoChange, pChange, *tiP;
	ModelInfo	*m;
#if defined SSE
	float		temp[16];
#endif
	
	m = &modelSettings[division];

	/* find transition probabilities */
	tiP = tiProbs[chain] + m->tiProbStart + (2*p->index + Bit(division, &p->tiSpace[0])) * tiProbRowSize;

	baseRate =  1.0;
	if (m->shape == NULL)
		catRate = &baseRate;
	else
		catRate = GetParamSubVals (m->shape, chain, state[chain]);
	
	/* fill in values */
	for (k=index=0; k<m->numGammaCats; k++)
		{
		/* calculate probabilities */
		pChange   = (CLFlt) (0.25 - 0.25 * exp(-(4.0/3.0)*p->length*catRate[k]));
		pNoChange = (CLFlt) (0.25 + 0.75 * exp(-(4.0/3.0)*p->length*catRate[k]));
		for (i=0; i<4; i++)
			{
			for (j=0; j<4; j++)
				{
				if (i == j)
					tiP[index++] = pNoChange;
				else
					tiP[index++] = pChange;
				}
			}
		}

#if defined SSE
	/* rearrange tiprobs to fit shuffling SSE */
	for (k=0; k<m->numGammaCats; k++)
		{
		temp[0] = tiP[0];
		temp[1] = tiP[1];
		temp[2] = tiP[2];
		temp[3] = tiP[3];
		temp[4] = tiP[4];
		temp[5] = tiP[5];
		temp[6] = tiP[6];
		temp[7] = tiP[7];
		temp[8] = tiP[8];
		temp[9] = tiP[9];
		temp[10] = tiP[10];
		temp[11] = tiP[11];
		temp[12] = tiP[12];
		temp[13] = tiP[13];
		temp[14] = tiP[14];
		temp[15] = tiP[15];
		tiP[0] = temp[0];
		tiP[1] = temp[5];
		tiP[2] = temp[10];
		tiP[3] = temp[15];
		tiP[4] = temp[1];
		tiP[5] = temp[6];
		tiP[6] = temp[11];
		tiP[7] = temp[12];
		tiP[8] = temp[2];
		tiP[9] = temp[7];
		tiP[10] = temp[8];
		tiP[11] = temp[13];
		tiP[12] = temp[3];
		tiP[13] = temp[4];
		tiP[14] = temp[9];
		tiP[15] = temp[14];
		tiP += 16;
		}
#endif

	return NO_ERROR;
}





/*-----------------------------------------------------------------
|
|	TiProbs_Res: update transition probabilities for binary
|		restriction site model with or without rate variation
|
------------------------------------------------------------------*/
int TiProbs_Res (TreeNode *p, int division, int chain)

{
	
	int			k, index;
	MrBFlt		baseRate, eV, mu, theRate, v,
				*bs, *catRate;
	CLFlt		*tiP;
	ModelInfo	*m;
	
	/* find model settings for the division */
	m = &modelSettings[division];

	/* find transition probabilities */
	tiP = tiProbs[chain] + m->tiProbStart + (2*p->index + Bit(division, &p->tiSpace[0])) * tiProbRowSize;

	/* find rates */
	baseRate =  GetRate (division, chain);
	theRate =  1.0;
	if (m->shape == NULL)
		catRate = &theRate;
	else
		catRate = GetParamSubVals (m->shape, chain, state[chain]);
	
	/* find base frequencies */
	bs = GetParamSubVals(m->stateFreq, chain, state[chain]);

	/* calculate scaling factor */
	mu =  1.0 / (2.0 * bs[0] * bs[1]);
	
	/* fill in values */
	for (k=index=0; k<m->numGammaCats; k++)
		{
		/* calculate probabilities */
		v =  p->length * baseRate * catRate[k];
		eV =  exp(-mu * v);
		tiP[index++] = (CLFlt) (bs[0] + bs[1] * eV);
		tiP[index++] = (CLFlt) (bs[1] - bs[1] * eV);
		tiP[index++] = (CLFlt) (bs[0] - bs[0] * eV);
		tiP[index++] = (CLFlt) (bs[1] + bs[0] * eV);
		}

	return NO_ERROR;

}





/*-----------------------------------------------------------------
|
|	TiProbs_Std: update transition probabilities for
|		variable states model with or without rate variation
|
------------------------------------------------------------------*/
int TiProbs_Std (TreeNode *p, int division, int chain)

{
	
	int			b, c, i, j, k, n, s, nStates, index, index2;
	MrBFlt		v, eV1, eV2, eV3, eV4, eV5, *catRate,
				baseRate, theRate, pi, f1, f2, f3, f4, f5, f6, f7, root, EigValexp[10],
				*eigenValues, *cijk, sum, *bs, mu;
	CLFlt		pNoChange, pChange, *tiP;
	ModelInfo	*m;
#	if defined (DEBUG_TIPROBS_STD)
	int			index3;
#	endif

	m = &modelSettings[division];

	/* find transition probabilities */
	tiP = tiProbs[chain] + m->tiProbStart + (2*p->index + Bit(division, &p->tiSpace[0])) * tiProbRowSize;

	/* get rate multiplier */
	theRate =  1.0;
	baseRate =  GetRate (division, chain);

	/* get category rates */
	if (m->shape == NULL)
		catRate = &theRate;
	else
		catRate = GetParamSubVals (m->shape, chain, state[chain]);
	
#	if defined (DEBUG_TIPROBS_STD)
	/* find base frequencies */
	bs = GetParamSubVals(m->stateFreq, chain, state[chain]);
#	endif

	/* fill in values; this has to be done differently if state freqs are not equal */
	if (m->stateFreq->paramId == SYMPI_EQUAL)
		{
		/* equal state frequencies */
		/* fill in values for unordered characters */
		index = 0;
#		if defined (DEBUG_TIPROBS_STD)
		index3 = 0;
#		endif
		for (nStates=2; nStates<=10; nStates++)
			{
			if (m->isTiNeeded[nStates-2] == NO)
				continue;
			for (k=0; k<m->numGammaCats; k++)
				{
				/* calculate probabilities */
				v =  p->length*catRate[k]*baseRate;
				eV1 =  exp(-(nStates / (nStates -  1.0)) * v);
				pChange   = (CLFlt) ((1.0 / nStates) - ((1.0 / nStates) * eV1));
				pNoChange = (CLFlt) ((1.0 / nStates) + ((nStates - 1.0) / nStates) * eV1);
				for (i=0; i<nStates; i++)
					{
					for (j=0; j<nStates; j++)
						{
						if (i == j)
							tiP[index++] = pNoChange;
						else
							tiP[index++] = pChange;
						}
					}
				}
#			if defined (DEBUG_TIPROBS_STD)
			index3 += nStates;
#			endif
			}

		/* fill in values for 3-state ordered character */
		if (m->isTiNeeded[9] == YES)
			{
			nStates = 3;
			for (k=0; k<m->numGammaCats; k++)
				{
				/* calculate probabilities */
				v =  p->length * catRate[k] * baseRate;
				eV1 =  exp (-(3.0 / 4.0) * v);
				eV2 =  exp (-(9.0 / 4.0) * v);
				
				/* pij(0,0) */
				tiP[index] = (CLFlt) ((1.0 / 3.0) + (eV1 / 2.0) + (eV2 / 6.0));
				/* pij(0,1) = pij(1,0) */
				tiP[index+1] = tiP[index+3] = (CLFlt) ((1.0 / 3.0) - (eV2 / 3.0));
				/* pij(0,2) */
				tiP[index+2] = (CLFlt) ((1.0 / 3.0) - (eV1 / 2.0) + (eV2 / 6.0));
				/* pij(1,1) */
				tiP[index+4] = (CLFlt) ((1.0 / 3.0) + (2.0 * eV2 / 3.0));
				
				/* fill in mirror part of matrix */
				index += 5;
				index2 = index - 2;
				for (i=0; i<4; i++)
					tiP[index++] = tiP[index2--];
				}

#			if defined (DEBUG_TIPROBS_STD)
			PrintTiProbs (tiP+index-(nStates*nStates), bs+index3, nStates);
			index3 += nStates;
#			endif
			}

		/* 4-state ordered character */
		if (m->isTiNeeded[10] == YES)
			{
			nStates = 4;
			pi = 1.0 / 4.0;
			root =  sqrt (2.0);
			f1 = root +  1.0;
			f2 = root -  1.0;

			for (k=0; k<m->numGammaCats; k++)
				{
				/* calculate probabilities */
				v =  p->length * catRate[k] * baseRate;
				eV1 =  1.0 / ( exp ((4.0 * v) / 3.0));
				eV2 =  exp ((2.0 * (root - 2.0) * v) / 3.0) / root;
				eV3 =  1.0 / (root *  exp ((2.0 * (root + 2.0) * v) / 3.0));
				
				/* pij(0,0) */
				tiP[index] = (CLFlt) (pi * (1.0 + eV1 + (f1*eV2) + (f2*eV3)));
				/* pij(0,1) = pij(1,0) */
				tiP[index+1] = tiP[index+4] = (CLFlt) (pi * (1.0 - eV1 + eV2 - eV3));
				/* pij(0,2) = tiP(1,3) */
				tiP[index+2] = tiP[index+7] = (CLFlt) (pi * (1.0 - eV1 - eV2 + eV3));
				/* pij(0,3) */
				tiP[index+3] = (CLFlt) (pi * (1.0 + eV1 - (f1*eV2) - (f2*eV3)));
				/* pij(1,1) */
				tiP[index+5] = (CLFlt) (pi * (1.0 + eV1 + (f2*eV2) + (f1*eV3)));
				/* pij(1,2) */
				tiP[index+6] = (CLFlt) (pi * (1.0 + eV1 - (f2*eV2) - (f1*eV3)));

				/* fill in mirror part of matrix */
				index += 8;
				index2 = index - 1;
				for (i=0; i<8; i++)
					tiP[index++] = tiP[index2--];
				}
#			if defined (DEBUG_TIPROBS_STD)
			PrintTiProbs (tiP+index-(nStates*nStates), bs+index3, nStates);
			index3 += nStates;
#			endif
			}

		/* 5-state ordered character */
		if (m->isTiNeeded[11] == YES)
			{
			nStates = 5;
			pi = 1.0 / 5.0;
			root =  sqrt (5.0);

			f5 = root /  4.0;
			f1 =  0.75 + f5;;
			f2 =  1.25 + f5;
			f3 =  1.25 - f5;
			f4 =  0.75 - f5;
			f5 = f5 *  2.0;
			f6 = f5 +  0.5;
			f7 = f5 -  0.5;

			for (k=0; k<m->numGammaCats; k++)
				{
				/* calculate probabilities */
				v =  p->length * catRate[k] * baseRate;
				v *=  5.0 /  16.0;

				eV1 =  exp ((root -  3.0) * v);
				eV2 =  exp (-(root +  3.0) * v);
				eV3 =  exp ((root -  5.0) * v);
				eV4 =  exp (-(root +  5.0) * v);

				/* pij(0,0) */
				tiP[index] = (CLFlt) (pi*( 1.0 + (f1*eV3) + (f2*eV1) + (f3*eV2) + (f4*eV4)));
				/* pij(0,1) = pij(1,0) */
				tiP[index+1] = tiP[index+5] =
					(CLFlt) (pi*(1.0 - (eV3/2.0) + (f5*eV1) - (f5*eV2) - (eV4/2.0)));
				/* pij(0,2) = pij(2,0) */
				tiP[index+2] = tiP[index+10] = (CLFlt) (pi*(1.0 - (f6*eV3) + (f7*eV4)));
				/* pij(0,3) = pij(1,4) */
				tiP[index+3] = tiP[index+9] =
					(CLFlt) (pi*(1.0 - (eV3/2.0) - (f5*eV1) + (f5*eV2) - (eV4/2.0)));
				/* pij(0,4) */
				tiP[index+4] = (CLFlt) (pi*(1.0 + (f1*eV3) - (f2*eV1) - (f3*eV2) + (f4*eV4)));
				/* pij(1,1) */
				tiP[index+6] = (CLFlt) (pi*(1.0 + (f4*eV3) + (f3*eV1) + (f2*eV2) + (f1*eV4)));
				/* pij(1,2) = pij(2,1) */
				tiP[index+7] = tiP[index+11] = (CLFlt) (pi*(1.0 + (f7*eV3) - (f6*eV4)));
				/* pij(1,3) */
				tiP[index+8] = (CLFlt) (pi*(1.0 + (f4*eV3) - (f3*eV1) - (f2*eV2) + (f1*eV4)));
				/* pij(2,2) */
				tiP[index+12] = (CLFlt) (pi*(1.0 + (2.0*eV3) + (2.0*eV4)));

				/* fill in mirror part of matrix */
				index += 13;
				index2 = index - 2;
				for (i=0; i<12; i++)
					tiP[index++] = tiP[index2--];
				}
#			if defined (DEBUG_TIPROBS_STD)
			PrintTiProbs (tiP+index-(nStates*nStates), bs+index3, nStates);
			index3 += nStates;
#			endif
			}

		/* 6-state ordered character */
		if (m->isTiNeeded[12] == YES)
			{
			nStates = 6;
			pi =  1.0 /  6.0;
			root =  sqrt (3.0);

			f4 = (3.0 / (2.0 * root));
			f1 =  1.0 + f4;
			f2 =  1.0 - f4;
			f3 =  0.5 + f4;
			f4 =  0.5 - f4;

			for (k=0; k<m->numGammaCats; k++)
				{
				/* calculate probabilities */
				v =  p->length * catRate[k] * baseRate;
				v /=  5.0;

				eV1 =  exp ( -9 * v);
				eV2 =  exp ( -6 * v);
				eV3 =  exp ( -3 * v);
				eV4 =  exp ( 3.0 * (root -  2.0) * v);
				eV5 =  exp (-( 3.0 * (root +  2.0) * v));

				/* pij(0,0) */
				tiP[index] = (CLFlt) (pi*( 1.0 + (0.5*eV1) + eV2 + (1.5*eV3) + (f1*eV4) + (f2*eV5)));
				/* pij(0,1) = pij(1,0) */
				tiP[index+1] = tiP[index+6] = (CLFlt) (pi*(1.0 - eV1 - eV2 + (f3*eV4) + (f4*eV5)));
				/* pij(0,2) = pij(2,0) */
				tiP[index+2] = tiP[index+12] = 
					(CLFlt) (pi*(1.0 + (0.5*eV1) - eV2 - (1.5*eV3) + (0.5*eV4) + (0.5*eV5)));
				/* pij(0,3) = pij(2,5) */
				tiP[index+3] = tiP[index+17] = 
					(CLFlt) (pi*(1.0 + (0.5*eV1) + eV2 - (1.5*eV3) - (0.5*eV4) - (0.5*eV5)));
				/* pij(0,4) = pij(1,5) */
				tiP[index+4] = tiP[index+11] = (CLFlt) (pi*(1.0 - eV1 + eV2 - (f3*eV4) - (f4*eV5)));
				/* pij(0,5) */
				tiP[index+5] = (CLFlt) (pi*(1.0 + (0.5*eV1) - eV2 + (1.5*eV3) - (f1*eV4) - (f2*eV5)));
				/* pij(1,1) */
				tiP[index+7] = (CLFlt) (pi*(1.0 + (2.0*eV1) + eV2 + eV4 + eV5));
				/* pij(1,2) = pij(2,1) */
				tiP[index+8] = tiP[index+13] = (CLFlt) (pi*(1.0 - eV1 + eV2 - (f4*eV4) - (f3*eV5)));
				/* pij(1,3) = pij(2,4) */
				tiP[index+9] = tiP[index+16] = (CLFlt) (pi*(1.0 - eV1 - eV2 + (f4*eV4) + (f3*eV5)));
				/* pij(1,4) */
				tiP[index+10] = (CLFlt) (pi*(1.0 + (2.0*eV1) - eV2 - eV4 - eV5));
				/* pij(2,2) */
				tiP[index+14] = (CLFlt) (pi*(1.0 + (0.5*eV1) + eV2 + (1.5*eV3) + (f2*eV4) + (f1*eV5)));
				/* pij(2,3) */
				tiP[index+15] = (CLFlt) (pi*(1.0 + (0.5*eV1) - eV2 + (1.5*eV3) - (f2*eV4) - (f1*eV5)));

				/* fill in mirror part of matrix */
				index += 18;
				index2 = index - 1;
				for (i=0; i<18; i++)
					tiP[index++] = tiP[index2--];
				}
#			if defined (DEBUG_TIPROBS_STD)
			PrintTiProbs (tiP+index-(nStates*nStates), bs+index3, nStates);
			index3 += nStates;
#			endif
			}
		}
	else
		{
		/* unequal state frequencies */

		/* first fill in for binary characters using beta categories if needed */
		if (m->isTiNeeded[0] == YES)
			{
			/* find base frequencies */
			bs = GetParamSubVals(m->stateFreq, chain, state[chain]);

			/* calculate alpha and beta */
			index = 0;

			/* cycle through beta and gamma cats */
			for (b=0; b<m->numBetaCats; b++)
				{
				mu =  1.0 / (2.0 * bs[0] * bs[1]);
				for (k=0; k<m->numGammaCats; k++)
					{
					/* calculate probabilities */
					v =  p->length*catRate[k]*baseRate;
					eV1 =  exp(- mu * v);
					tiP[index++] = (CLFlt) (bs[0] + (bs[1] * eV1));
					tiP[index++] = (CLFlt) (bs[1] - (bs[1] * eV1));
					tiP[index++] = (CLFlt) (bs[0] - (bs[0] * eV1));
					tiP[index++] = (CLFlt) (bs[1] + (bs[0] * eV1));
					}
				/* update stationary state frequency pointer */
				bs += 2;
				}
			}

		/* now use general algorithm for the other cases */
		if (m->nCijk > 0)
			{

			/* first update cijk if necessary */
			if (m->nCijk > 0 && m->upDateCijk[chain][state[chain]] == YES)
				{
				if (UpDateCijk (division, chain) == ERROR)
					return (ERROR);
				}

			/* then get first set of eigenvalues */
			eigenValues = cijks[chain] + m->cijkStart + m->cijkBits[chain][state[chain]] * cijkRowSize;

			/* and cycle through the relevant characters */
			for (c=0; c<m->stateFreq->nSympi; c++)
				{
				n = m->stateFreq->sympinStates[c];

				/* get cijk pointers */
				cijk = eigenValues + (2 * n);

				/* fill in values */
				for (k=index=0; k<m->numGammaCats; k++)
					{
					v =  p->length * baseRate * catRate[k];
					
					for (s=0; s<n; s++)
						EigValexp[s] =  exp(eigenValues[s] * v);

					for (i=0; i<n; i++)
						{
						for (j=0; j<n; j++)
							{
							sum = 0.0;
							for(s=0; s<n; s++)
								sum += (*cijk++) * EigValexp[s];
							tiP[index++] = (CLFlt) ((sum <  0.0) ?  0.0 : sum);
							}
						}
					}

				/* update eigenValues pointer */
				eigenValues += (n * n * n) + (2 * n);

				}
			}
		}

	return NO_ERROR;

}





void TouchAllPartitions (void)

{

	int i;

	for (i=0; i<numCurrentDivisions; i++)
		{
		modelSettings[i].upDateCl = YES;
		}

	return;
	
}





void TouchAllTreeNodes (Tree *t)

{

	int			i;
	TreeNode	*p;

	for (i=0; i<t->nNodes; i++)
		{
		p = t->allDownPass[i];
		p->upDateCl = YES;
		p->upDateTi = YES;
		}
		
}





void TouchAllTrees (int chain)

{

	int			i, j;
	Tree		*t;
	TreeNode	*p;

	for (i=0; i<numTrees; i++)
		{
		t = GetTreeFromIndex (i, chain, state[chain]);
		for (j=0; j<t->nNodes; j++)
			{
			p = t->allDownPass[j];
			p->upDateCl = YES;
			p->upDateTi = YES;
			}
		}

	return;
	
}





/*------------------------------------------------------------------
|
|	TreeLength: Calculates the tree length as the sum of the lengths
|		of all the branches. The tree length is the expected number 
|       of character state transformations per character over the 
|       entire phylogenetic tree.
|
-------------------------------------------------------------------*/
MrBFlt TreeLength (Param *param, int chain)

{

	int				i, j;
	MrBFlt			tl;
	Tree			*t;
	TreeNode		*p;

	if (param->paramId == BRLENS_PARSIMONY)
		{
		tl = 0.0;
		for (j=0; j<param->nRelParts; j++)
			tl += modelSettings[param->relParts[j]].parsTreeLength[2*chain+state[chain]];
		}
	else
		{
		/* get tree */
		t = GetTree (param, chain, state[chain]);
		
		/* loop over all branches of the tree */
		tl = 0.0;
		for (i=0; i<t->nNodes; i++)
			{
			p = t->allDownPass[i];
			if (p->anc != NULL)
				{
				if (p->anc->anc == NULL)
					{
					if (t->isRooted == NO)
						tl += p->length;
					}
				else
					{
					tl += p->length;
					}
				}
			}
		}
				
	return (tl);
	
}





int UpDateCijk (int whichPart, int whichChain)

{

	int			c, i, j, k, n, n3, isComplex, sizeOfSingleCijk, cType, numQAllocated;
	MrBFlt		**q[100], **eigvecs, **inverseEigvecs;
	MrBFlt		*eigenValues, *eigvalsImag, *cijk;
	MrBFlt		*bs, *bsBase, *rateOmegaValues=NULL, rA=0.0, rS=0.0, posScaler, *omegaCatFreq=NULL;
	complex		**Ceigvecs, **CinverseEigvecs;
	ModelInfo	*m;
	Param		*p;
		
	/* get a pointer to the model settings for this partition */
	m = &modelSettings[whichPart];
	
	/* we should only go through here if we have cijk information available for the partition */
	if (m->nCijk > 0) 
		{
		/* flip the appropriate bit in "m->cijkBits[whichChain][state[whichChain]]" */
		m->cijkBits[whichChain][state[whichChain]] = Flip01(m->cijkBits[whichChain][state[whichChain]]);
		
		/* figure out information on either omega values or rate values, if necessary */
		if (m->dataType == DNA || m->dataType == RNA)
			{
			if (m->nucModelId == NUCMODEL_CODON)                                                    /* we have a NY98 model     */
				{
				rateOmegaValues = GetParamVals(m->omega, whichChain, state[whichChain]);
				if (inferPosSel == YES)
					omegaCatFreq = GetParamSubVals (m->omega, whichChain, state[whichChain]);
				}
			else if (m->nCijkParts > 1 && m->nucModelId == NUCMODEL_4BY4 && m->numModelStates == 8) /* we have a covarion model */
				rateOmegaValues = GetParamSubVals (m->shape, whichChain, state[whichChain]);        /* with rate variation      */
			}
		else if (m->dataType == PROTEIN)
			{
			if (m->nCijkParts > 1)                                                                  /* we have a covarion model */
				rateOmegaValues = GetParamSubVals (m->shape, whichChain, state[whichChain]);        /* with rate variation      */
			}
		else if (m->dataType != STANDARD)
			{
			MrBayesPrint ("%s   ERROR: Should not be updating cijks!\n", spacer);
			return (ERROR);
			}
		
		if (m->dataType == STANDARD)
			{
			/* set pointers and other stuff needed */
			numQAllocated = 1;
			p = m->stateFreq;
			eigenValues = cijks[whichChain] + m->cijkStart + m->cijkBits[whichChain][state[whichChain]] * cijkRowSize;
			q[0] = AllocateSquareDoubleMatrix (10);
			eigvecs = AllocateSquareDoubleMatrix (10);
			inverseEigvecs = AllocateSquareDoubleMatrix (10);
			Ceigvecs = AllocateSquareComplexMatrix (10);
			CinverseEigvecs = AllocateSquareComplexMatrix (10);
			bsBase = GetParamSubVals(m->stateFreq, whichChain, state[whichChain]);
			
			/* cycle over characters needing cijks */
			for (c=0; c<p->nSympi; c++)
				{
				n = p->sympinStates[c];
				bs = bsBase + p->sympiBsIndex[c];
				cType = p->sympiCType[c];
				n3 = n * n * n;
				eigvalsImag = eigenValues + n;
				cijk = eigenValues + (2 * n);
				if (SetStdQMatrix (q[0], n, bs, cType) == ERROR)
					return (ERROR);
				isComplex = GetEigens (n, q[0], eigenValues, eigvalsImag, eigvecs, inverseEigvecs, Ceigvecs, CinverseEigvecs);
				if (isComplex == NO)
					{
					CalcCijk (n, cijk, eigvecs, inverseEigvecs);
					}
				else
					{
					MrBayesPrint ("%s   ERROR: Complex eigenvalues found!\n", spacer);
					goto errorExit;
					}
				eigenValues += (n3 + (2 * n));
				}
			}
		else
			{
			/* all other data types */
			numQAllocated = m->nCijkParts;
			sizeOfSingleCijk = m->nCijk / m->nCijkParts;
			n = m->numModelStates;
			n3 = n * n * n;
			eigenValues = cijks[whichChain] + m->cijkStart + m->cijkBits[whichChain][state[whichChain]] * cijkRowSize;
			eigvalsImag = eigenValues + n;
			cijk        = eigenValues + (2 * n);
			for (k=0; k<numQAllocated; k++)
				q[k] = AllocateSquareDoubleMatrix (n);
			eigvecs = AllocateSquareDoubleMatrix (n);
			inverseEigvecs = AllocateSquareDoubleMatrix (n);
			Ceigvecs = AllocateSquareComplexMatrix (n);
			CinverseEigvecs = AllocateSquareComplexMatrix (n);
			bs = GetParamSubVals (m->stateFreq, whichChain, state[whichChain]);
			
			if (m->nCijkParts == 1)
				{
				if (m->dataType == DNA || m->dataType == RNA)
					{
					if (m->nucModelId == NUCMODEL_CODON)
						{
						if (SetNucQMatrix (q[0], n, whichChain, whichPart, rateOmegaValues[0], &rA, &rS) == ERROR)
							goto errorExit;
						}
					else
						{
						if (SetNucQMatrix (q[0], n, whichChain, whichPart, 1.0, &rA, &rS) == ERROR)
							goto errorExit;
						}
					}
				else
					{
					if (SetProteinQMatrix (q[0], n, whichChain, whichPart, 1.0) == ERROR)
						goto errorExit;
					}
				isComplex = GetEigens (n, q[0], eigenValues, eigvalsImag, eigvecs, inverseEigvecs, Ceigvecs, CinverseEigvecs);
				if (isComplex == NO)
					{
					CalcCijk (n, cijk, eigvecs, inverseEigvecs);
					}
				else
					{
					MrBayesPrint ("%s   ERROR: Complex eigenvalues found!\n", spacer);
					goto errorExit;
					}
				}
			else
				{
				/* Here, we calculate the rate matrices (Q) for various nucleotide and amino acid
				   data models. Usually, when the rate matrix is set in SetNucQMatrix, it is scaled
				   such that the average substitution rate is one. However, there is a complication
				   for positive selection models using codon rate matrices. First, we have more than
				   one matrix; in fact, we have as many rate matrices as there are omega values. Second,
				   the mean substitution rate still has to be one. And third, we want the synonymous
				   rate to be the same across the rate matrices. For positive selection models, the Q
				   matrix comes out of SetNucQMatrix unscaled. Once we have all m->nCijkParts rate 
				   matrices, we then scale again, this time to ensure that the mean substitution rate is one. */

				/* First, calculate rate matrices for each category: */
				posScaler = 0.0;
				for (k=0; k<m->nCijkParts; k++)
					{
					if (m->dataType == DNA || m->dataType == RNA)
						{
						if (SetNucQMatrix (q[k], n, whichChain, whichPart, rateOmegaValues[k], &rA, &rS) == ERROR)
							goto errorExit;
						}
					else
						{
						if (SetProteinQMatrix (q[k], n, whichChain, whichPart, rateOmegaValues[k]) == ERROR)
							goto errorExit;
						}
					if (inferPosSel == YES)
						posScaler += omegaCatFreq[k] * (rS + rA);
					}
					
				/* Then rescale the rate matrices, if this is a positive selection model: */
				if (inferPosSel == YES)
					{
					posScaler = 1.0 / posScaler;
					for (k=0; k<m->nCijkParts; k++)
						{
						for (i=0; i<n; i++)
							for (j=0; j<n; j++)
								q[k][i][j] *= posScaler;
						}
					}

				/* Finally, calculate eigenvalues, etc.: */
				for (k=0; k<m->nCijkParts; k++)
					{
					isComplex = GetEigens (n, q[k], eigenValues, eigvalsImag, eigvecs, inverseEigvecs, Ceigvecs, CinverseEigvecs);
					if (isComplex == NO)
						{
						CalcCijk (n, cijk, eigvecs, inverseEigvecs);
						}
					else
						{
						MrBayesPrint ("%s   ERROR: Complex eigenvalues found!\n", spacer);
						goto errorExit;
						}
					if (k+1 < m->nCijkParts)
						{
						/* shift pointers */
						eigenValues += sizeOfSingleCijk;
						eigvalsImag += sizeOfSingleCijk;
						cijk        += sizeOfSingleCijk;
						}
					}
				}
			}
			
		for (k=0; k<numQAllocated; k++)
			FreeSquareDoubleMatrix (q[k]);
		FreeSquareDoubleMatrix (eigvecs);
		FreeSquareDoubleMatrix (inverseEigvecs);
		FreeSquareComplexMatrix (Ceigvecs);
		FreeSquareComplexMatrix (CinverseEigvecs);
		
		m->upDateCijk[whichChain][state[whichChain]] = NO;
		}
		
	return (NO_ERROR);

	errorExit:		
		for (k=0; k<numQAllocated; k++)
			FreeSquareDoubleMatrix (q[k]);
		FreeSquareDoubleMatrix (eigvecs);
		FreeSquareDoubleMatrix (inverseEigvecs);
		FreeSquareComplexMatrix (Ceigvecs);
		FreeSquareComplexMatrix (CinverseEigvecs);

		return ERROR;

}





void WriteCalTreeToFile (TreeNode *p, MrBFlt clockRate)

{

	char			tempStr[200];

	if (p != NULL)
		{
		
		if (p->left == NULL && p->right == NULL)
			{
			sprintf (tempStr, "%d:%lf", p->index + 1, p->nodeDepth/clockRate);
			AddToPrintString (tempStr);
			}
		else
			{
			if (p->anc != NULL)
				{
				sprintf (tempStr, "(");
				AddToPrintString (tempStr);
				}
			WriteCalTreeToFile (p->left,  clockRate);
			sprintf (tempStr, ",");
			AddToPrintString (tempStr);
			WriteCalTreeToFile (p->right, clockRate);	
			if (p->anc != NULL)
				{
				if (p->anc->anc != 0)
					sprintf (tempStr, "):%lf", p->nodeDepth/clockRate);
				else
					sprintf (tempStr, ")");
				AddToPrintString (tempStr);
				}
			}
		}
}





void WriteTreeToFile (TreeNode *p, int showBrlens, int isRooted)

{

	char			tempStr[200];
	
	if (p != NULL)
		{
		
		if (p->left == NULL && p->right == NULL)
			{
			if (showBrlens == YES)
				sprintf (tempStr, "%d:%lf", p->index + 1, p->length);
			else
				sprintf (tempStr, "%d", p->index + 1);
			AddToPrintString (tempStr);
			}
		else
			{
			if (p->anc != NULL)
				{
				sprintf (tempStr, "(");
				AddToPrintString (tempStr);
				}
			WriteTreeToFile (p->left,  showBrlens, isRooted);
			sprintf (tempStr, ",");
			AddToPrintString (tempStr);
			WriteTreeToFile (p->right, showBrlens, isRooted);	
			if (p->anc != NULL)
				{
				if (p->anc->anc == NULL && isRooted == NO)
					{
					if (showBrlens == YES)
						sprintf (tempStr, ",%d:%lf", p->anc->index + 1, p->length);
					else
						sprintf (tempStr, ",%d", p->anc->index + 1);
					AddToPrintString (tempStr);
					}
				
				if (showBrlens == YES && p->anc->anc != NULL)
					sprintf (tempStr, "):%lf", p->length);
				else
					sprintf (tempStr, ")");
				AddToPrintString (tempStr);
					
				}
			}
		}
}


