#include "mainbb.h"

#include "aaJC.h"
#include "amino.h"
#include "bbAlg.h"
#include "bestAlpha.h"
#include "brLenOptEM.h"
#include "chebyshevAccelerator.h"
#include "clustalFormat.h"
#include "computeMarginalReconstruction.h"
#include "distanceTable.h"
#include "fastaFormat.h"
#include "gammaDistribution.h"
#include "jointNoGamma.h"
#include "likeDist.h"
#include "logFile.h"
#include "maseFormat.h"
#include "molphyFormat.h"
#include "nexusFormat.h"
#include "nucleotide.h"
#include "nucJC.h"
#include "nj.h"
#include "phylipFormat.h"
#include "readDatMatrix.h"
#include "recognizeFormat.h"
#include "trivialAccelerator.h"
#include "uniDistribution.h"

mainbb::mainbb(int argc, char* argv[]) {
	fillOptionsParameters(argc,argv);
	printBBProjectInfo();
	printSearchParameters();
	getStartingSequenceData();
	getStartingStochasticProcess();
	getStartingEvolTreeTopology();
	_et.rootToUnrootedTree();
	getStartingBranchLengthsAndAlpha();
	printOutputTree();
	if (_options->doJoint) {
		if (_options->distributionName == bb_options::gam) {
            findAncestralSequencesGammaJoint();
        } else {
			findAncestralSequencesHomJoint();		
		}
	}
	getMarginalReconstruction();
}

void mainbb::printAncestralSequencesGammaJoint() {
	ofstream out(_options->outFile_seq_joint.c_str());
	switch (_options->seqOutputFormat){
		case (bb_options::mase)   : maseFormat::write(out,_resulutingJointReconstruction); break;
		case (bb_options::fasta)  : fastaFormat::write(out,_resulutingJointReconstruction); break;
		case (bb_options::clustal): clustalFormat::write(out,_resulutingJointReconstruction); break;
		case (bb_options::phylip) : phylipFormat::write(out,_resulutingJointReconstruction); break;
		case (bb_options::molphy) : molphyFormat::write(out,_resulutingJointReconstruction); break;
		case (bb_options::nexus)  : nexusFormat::write(out,_resulutingJointReconstruction); break;
	}
	out.close();
}

mainbb::~mainbb() {
	if (_alph) delete _alph;
	if (_options) delete _options;
	if (_sp) delete _sp;
}

void mainbb::getStartingEvolTreeTopology(){
	if (_options->treefile=="") {
		getStartingNJtreeNjMLdis();
	}
	else getStartingTreeFromTreeFile();
}



void mainbb::getStartingNJtreeNjMLdis() {
	// note that here ALWAYS, the ML distances are computed using
	// an homogenous rate distribution.
	uniDistribution lUni;
	pijAccelerator* lpijAcc = _sp->getPijAccelerator();// note this is just a copy of the pointer.
	stochasticProcess lsp(&lUni,lpijAcc);

	likeDist pd1(_options->alphabet_size,lsp,0.01);
	VVdouble disTab;
	vector<string> vNames;
	giveDistanceTable(&pd1,
					   _sc,
					   disTab,
					   vNames);
	getStartingTreeNJ_fromDistances(disTab,vNames);
} 

void mainbb::getStartingTreeNJ_fromDistances(const VVdouble& disTab,
	const vector<string>& vNames) {
	NJalg nj1;
	_et= nj1.computeNJtree(disTab,vNames);
}


void mainbb::getStartingTreeFromTreeFile(){
	_et= tree(_options->treefile);
	if (!_et.withBranchLength()) {
		_et.createFlatLengthMatrix(0.05);
		_options->optimizeBrLenOnStartingTree = true;
	}
}

void mainbb::getStartingBranchLengthsAndAlpha(){
	if (_options->distributionName == bb_options::hom) {
		if (_options->optimizeBrLenOnStartingTree == true) {
			cout<<"Optimizing branch lengths (Homogenuos model)..."<<endl;
			brLenOptEM::optimizeBranchLength1G_EM(_et,_sc,*_sp,NULL);
		}
	}
	else { // GAMMA MODEL!
		// Here we want to optimize branch lengths with a gamma model.
		// there are three options:
		//(1) User provides the alpha and no bbl.
		//(2) User provides the alpha and bbl
		//(3) Alpha is optimized from the data and bbl.


		// option 1 will not enter to any of these options.
		if ((_options->userProvideAlpha == true) && (_options->optimizeBrLenOnStartingTree == true)) {
			cout<<"Optimizing branch lengths (Gamma model, user alpha)..."<<endl;
			MDOUBLE intitalAlpha = 1.0;
			static_cast<gammaDistribution*>(_sp->distr())->setAlpha(intitalAlpha);
			brLenOptEM::optimizeBranchLength1G_EM(_et,_sc,*_sp,NULL);
		} 
		else if ((_options->userProvideAlpha == true) && (_options->optimizeBrLenOnStartingTree == false)) {
			return;
		}
		else if (_options->userProvideAlpha == false)  {
			cout<<"Optimizing branch lengths and alpha (Gamma model) ..."<<endl;
			bestAlphaAndBBL bbl2(_et,_sc,*_sp);
		}
	}
}

void mainbb::getStartingStochasticProcess() {
	int numberOfCategories = _options->gammaCategies;
	MDOUBLE alpha = _options->gammaPar;
	if (_options->distributionName == bb_options::hom) {
		numberOfCategories = 1; // forcing homogenous model.
		alpha = 1.0;
		cout<<"Using homogenous model (no among site rate variation)"<<endl;
	} else {
		cout<<"Using a Gamma model with: "<<numberOfCategories<<" discrete categories "<<endl;
	}
	distribution *dist = new gammaDistribution(alpha,numberOfCategories);
	replacementModel *probMod=NULL;
	pijAccelerator *pijAcc=NULL;
	switch (_options->modelName){
		case (bb_options::day): 
			probMod=new pupAll(datMatrixHolder::dayhoff);
			pijAcc = new chebyshevAccelerator(probMod);
			cout<<"Amino acid replacement matrix is Dayhoff"<<endl;
			break;
		case (bb_options::jtt):
			probMod=new pupAll(datMatrixHolder::jones);
			pijAcc = new chebyshevAccelerator(probMod);
			cout<<"Amino acid replacement matrix is JTT"<<endl;
			break;
		case (bb_options::rev):
			probMod=new pupAll(datMatrixHolder::mtREV24);
			pijAcc = new chebyshevAccelerator(probMod);
			cout<<"Amino acid replacement matrix is mtREV24"<<endl;
			 break;
		case (bb_options::wag):
			probMod=new pupAll(datMatrixHolder::wag);
			pijAcc = new chebyshevAccelerator(probMod);
			cout<<"Amino acid replacement matrix is WAG"<<endl;
			break;
		case (bb_options::cprev):
			probMod=new pupAll(datMatrixHolder::cpREV45);
			pijAcc = new chebyshevAccelerator(probMod);
			cout<<"Amino acid replacement matrix is cpREV45"<<endl;
			break;
		case (bb_options::nucjc):
			probMod=new nucJC;
			pijAcc = new trivialAccelerator(probMod);
			cout<<"Nucleotide substitution model is Jukes and Cantor"<<endl;
			break;
		case (bb_options::aajc):
			probMod=new aaJC; pijAcc = new trivialAccelerator(probMod);
			cout<<"Amino acid replacement matrix is Jukes and Cantor"<<endl;
			break;
		default:
			errorMsg::reportError("this probablistic model is not yet available");
	}
	_sp = new stochasticProcess(dist, pijAcc);
	if (probMod) delete probMod;
	if (pijAcc) delete pijAcc;
	if (dist) delete dist;
}

void mainbb::printOutputTree() {
	ofstream f;
	string fileName1=_options->outTreeFileNewick;
	f.open(fileName1.c_str());
	_et.output(f,tree::PHYLIP,true);
	f.close();
	cout<<"The tree in 'Newick tree format' (with the internal nodes labeled)\nwas written to a file name called "<<fileName1<<endl;
	fileName1 = _options->outTreeFileAncestor;
	f.open(fileName1.c_str());
	_et.output(f,tree::ANCESTOR);
	f.close();
	cout<<"The tree in 'ANCESTOR tree format' was written to a file name called "<<fileName1<<endl;
}

void mainbb::fillOptionsParameters(int argc, char* argv[]) {
	_options = new bb_options(argc, argv);
}

void mainbb::getStartingSequenceData(){	
 	if (_options->alphabet_size==4) _alph = new nucleotide;
	else if (_options->alphabet_size == 20) _alph = new amino;
	else errorMsg::reportError("no such alphabet in function rate4site::getStartingSequenceData");
   
	fstream fstream1(_options->seqfile.c_str());
	_sc = recognizeFormat::read(fstream1,_alph);
	_sc.changeGaps2MissingData();
}
	
void mainbb::printSearchParameters() {
	if (_options->verbose) {
		LOG(1,<<"\nBB parameters: "<<endl);
		LOG(1,<<endl);
		LOG(1,<<"-------------------------------------------------------------------------------"<<endl);
		LOG(1,<<endl);
		if (_options->treefile.size()>0) {LOG(1,<<"Tree file is: "<<_options->treefile<<endl)}
		else LOG(1,<<"Starting tree is the NJ tree "<<endl);
		if (_options->seqfile.size()>0) LOG(1,<<"Sequence file is: "<<_options->seqfile<<endl);
	}
}
             
void mainbb::printBBProjectInfo() {
	LOG(1,<<"*******************************************************************************"<<endl);
	LOG(1,<<"B&B: A Branch and Bound algorithm for Ancestral Sequence Reconstruction.       "<<endl);
	LOG(1,<<"For information, please send email to Tal Pupko: talp@post.tau.ac.il           "<<endl);
	LOG(1,<<"Ref: Pupko, T., Pe'er, I., Graur, D. Hasegawa, M., and Friedman N. 2002.       "<<endl);
	LOG(1,<<"A branch-and-bound algorithm for the inference of ancestral amino-acid         "<<endl);
	LOG(1,<<"sequences when the replacement rate varies among sites: Application to the     "<<endl);
	LOG(1,<<"evolution of five gene families. Bioinformatics 18: 1116-1123.                 "<<endl);
	LOG(1,<<"*******************************************************************************"<<endl);
	LOG(1,<<endl);
}

void mainbb::findAncestralSequencesGammaJoint() {
	bbAlg::boundMethod bm;
	if (_options->boundMethod == bb_options::max) bm=bbAlg::max;
	else if (_options->boundMethod == bb_options::sum) bm=bbAlg::sum;
	else if (_options->boundMethod == bb_options::both) bm=bbAlg::both;

	bbAlg bbAlg1(_et,*_sp,_sc,bm,_options->reportFile,_options->computeAgainExactTreshold);
	MDOUBLE res = bbAlg1.bbReconstructAllPositions(_resulutingJointReconstruction);
	cout<<" the likelihood of this reconstruction is: "<<res<<endl;
	bbAlg1.outputTheJointProbAtEachSite(_options->outFile_prob_joint);
	printAncestralSequencesGammaJoint();
}

void mainbb::findAncestralSequencesHomJoint() {
	jointNoGamma jng(_et,*_sp,_sc);
	jng.compute();
	jng.outputTheJointProbAtEachSite(_options->outFile_prob_joint);
	sequenceContainer withAncestral = jng.getTheJointReconstruction();
	ofstream jointNoGammaReconstructionOutputFile(_options->outFile_seq_joint.c_str());
	switch (_options->seqOutputFormat) {
	case bb_options::mase: 
			 maseFormat::write(jointNoGammaReconstructionOutputFile,withAncestral);
			break;
	case bb_options::molphy: 
			molphyFormat::write(jointNoGammaReconstructionOutputFile,withAncestral);
			break;
	case bb_options::clustal: 
			clustalFormat::write(jointNoGammaReconstructionOutputFile,withAncestral);
			break;
	case bb_options::fasta: 
			fastaFormat::write(jointNoGammaReconstructionOutputFile,withAncestral);
			break;
	case bb_options::phylip: 
			phylipFormat::write(jointNoGammaReconstructionOutputFile,withAncestral);
			break;
	case bb_options::nexus: 
			nexusFormat::write(jointNoGammaReconstructionOutputFile,withAncestral);
			break;
	default: errorMsg::reportError(" format not implemented yet in this version... ",1);
	}
}


void mainbb::getMarginalReconstruction(){
	computeMarginalReconstruction cmr(_et,*_sp,_sc);
	cmr.compute();
	cmr.outputTheMarginalProbForEachCharForEachNode(_options->outFile_prob_marginal);
	sequenceContainer withAncestral = cmr.getResultingMarginalReconstruction();
	ofstream marginalReconstructionOutputFile(_options->outFile_seq_marginal.c_str());
	switch (_options->seqOutputFormat) {
	case bb_options::mase: 
			 maseFormat::write(marginalReconstructionOutputFile,withAncestral);
			break;
	case bb_options::molphy: 
			molphyFormat::write(marginalReconstructionOutputFile,withAncestral);
			break;
	case bb_options::clustal: 
			clustalFormat::write(marginalReconstructionOutputFile,withAncestral);
			break;
	case bb_options::fasta: 
			fastaFormat::write(marginalReconstructionOutputFile,withAncestral);
			break;
	case bb_options::phylip: 
			phylipFormat::write(marginalReconstructionOutputFile,withAncestral);
			break;
	case bb_options::nexus: 
			nexusFormat::write(marginalReconstructionOutputFile,withAncestral);
			break;
	default: errorMsg::reportError(" format not implemented yet in this version... ",1);
	}
	marginalReconstructionOutputFile.close();
}

