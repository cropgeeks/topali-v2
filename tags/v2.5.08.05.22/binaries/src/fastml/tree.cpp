#include "definitions.h"
#include "tree.h"
#include "treeUtil.h"
#include "logFile.h"
#include "someUtil.h"
#include <cassert>
#include <algorithm>
#include <iostream>
#include <iomanip>
#include <fstream>
#include <ctime>

using namespace std;

const MDOUBLE tree::FLAT_LENGTH_VALUE = 0.3f;
const int tree::TREE_NULL = -1;

//removeSon: remove pSon from sons list.
//does not delete pSon
void tree::TreeNode::removeSon(TreeNode* pSon) {
	vector<nodeP>::iterator vec_iter = remove(_sons.begin(), _sons.end(), pSon);
	_sons.erase(vec_iter,_sons.end()); // pg 1170, primer.
}

void tree::TreeNode::claimSons(){
	for(int i=0;i<getNumberOfSons();i++) {
		getSon(i)->setFather(this);
	}
}
//*******************************************************************************
// Constructors Destructors
//*******************************************************************************
tree::tree() {
	_root=NULL;
}

tree::tree(const string& treeFileName, vector<char>& isFixed) {
	ifstream in(treeFileName.c_str());
	if (in == NULL) {
		errorMsg::reportError("Error - unable to locate tree file.",1);
	}
	if (readPhylipTreeTopology(in,isFixed)) {
		create_names_to_internal_nodes();
		return;
	}
	in.close();
	errorMsg::reportError("Unable to read phylip tree file",1);
}
	
tree::tree(const string& treeFileName) {
	ifstream in(treeFileName.c_str());
	if (in == NULL) {
		errorMsg::reportError("Error - unable to locate tree file.",1); 
	}
	if (readPhylipTreeTopology(in)) {
		create_names_to_internal_nodes();
		return;
	}
	in.close();
	errorMsg::reportError("Unable to read phylip tree file",1);
}

tree::tree(istream &in) {
	if (readPhylipTreeTopology(in)) {
		create_names_to_internal_nodes();
		return;
	}
	errorMsg::reportError("Unable to read phylip tree file",1);// also quit the program
}

tree::tree(istream &in,vector<char>& isFixed) {
	if (readPhylipTreeTopology(in,isFixed)) {
		create_names_to_internal_nodes();
		return;
	}
	errorMsg::reportError("Unable to read phylip tree file",1);// also quit the program
}

tree::tree(const vector<char>& tree_contents) {
	readPhylipTreeTopology(tree_contents);
	create_names_to_internal_nodes();
	return;
}

tree::tree(const vector<char>& tree_contents, vector<char>& isFixed) {
	readPhylipTreeTopology(tree_contents,isFixed);
	create_names_to_internal_nodes();
	return;
}

tree::tree(const tree &otherTree) {
	_root = NULL;
	if (otherTree._root == NULL) 
		return; // if tree to copy is empty.
	createRootNode();
	_root->setName(otherTree._root->name());
	for (int i=0; i <otherTree._root->getNumberOfSons(); ++i) {
		recursiveBuildTree( _root, otherTree.getRoot()->getSon(i));
	}
}


tree& tree::operator=(const tree &otherTree) {
	if (otherTree._root == NULL) 
		return *this; // if tree to copy is empty.
	createRootNode();
	_root->setName(otherTree._root->name());
	for (int i=0; i <otherTree._root->getNumberOfSons(); ++i) {
		recursiveBuildTree( _root, otherTree.getRoot()->getSon(i));
	}
	return *this;
}

void tree::clear() {
	vector<nodeP> vec;
	getAllNodes(vec, _root);

	for (int k=0; k < vec.size(); k++) {
		delete(vec[k]);
	}

	_nodes = 0;
	_leaves =0;
	_root = NULL;

}

//*******************************************************************************
// questions on the tree topology
//*******************************************************************************

//stores the father and sons of node inNodeP in vNeighbourVector
void tree::getNeigboursOfNode(vector<nodeP> &vNeighbourVector, const nodeP inNodeP) const {
	vNeighbourVector.clear();
	for (int i=0; i < inNodeP->getNumberOfSons();++i) {
		vNeighbourVector.push_back(inNodeP->getSon(i));
	}
	if (getRoot() != inNodeP)	
		vNeighbourVector.push_back(inNodeP->father());
}


// get nodePTR from name
tree::nodeP tree::findNodeByName(const string inName, nodeP myNode) const{
	if (myNode==NULL) myNode=_root;
	if (myNode->name() == inName) return myNode;
	for (int i=0 ; i < myNode->getNumberOfSons(); i++ ) {
		nodeP answer = findNodeByName(inName, myNode->getSon(i));
		if (answer!=NULL) return answer;
	}
	return NULL;
}


//getPathBetweenAnyTwoNodes: store all nodes on the path from node1 to node2 in path
//the first node in path is node1. the last node is node2  
//1. store all nodes from node1 to the root and node2 to the root
//2. starting from the root - finds the first node (common_father) which is father to both node1 and node2
//3. store in <path> all nodes in the path from node1 to common_father, from node2 to common_father and common_father itself
void tree::getPathBetweenAnyTwoNodes(vector<nodeP> &path, const nodeP node1, const nodeP node2) const {

	path.clear();
	vector<nodeP> pathMatrix1;
	vector<nodeP> pathMatrix2;

	nodeP nodeup = node1;
	while (nodeup != _root)	{
		pathMatrix1.push_back(nodeup);
		nodeup = nodeup->father();
	}
	pathMatrix1.push_back(_root);

	nodeup = node2;
	while (nodeup != _root)	{
		pathMatrix2.push_back(nodeup);
		nodeup = nodeup->father();
	}
	pathMatrix2.push_back(_root);

	int tmp1 = pathMatrix1.size()-1;
	int tmp2 = pathMatrix2.size()-1;

	while ((tmp1 >= 0) && (tmp2 >= 0)) {
		if (pathMatrix1[tmp1] != pathMatrix2[tmp2]) 
			break;
		tmp1--; 
		tmp2--;
	}

	for (int y=0; y <= tmp1; ++y) 
		path.push_back(pathMatrix1[y]);
	path.push_back(pathMatrix1[tmp1+1]); // pushing once, the TreeNode that was common father to both.
	for (int j=tmp2; j >= 0; --j) {
		path.push_back(pathMatrix2[j]);
	}
	return;
}


void tree::getFromLeavesToRoot(vector<nodeP> &vNeighbourVector) const {
	getFromRootToLeaves(vNeighbourVector);
	reverse(vNeighbourVector.begin(),vNeighbourVector.end());
}


void tree::getFromRootToLeaves(vector<nodeP> &vec) const {
	getFromNodeToLeaves(vec,_root);
}


void tree::getFromNodeToLeaves(vector<nodeP> &vec, nodeP fromHereDown) const {
	vec.push_back(fromHereDown);
	for (int k=0; k < fromHereDown->getNumberOfSons(); k++) {
			getFromNodeToLeaves(vec, fromHereDown->getSon(k));
		}
		return;
}


void tree::getAllHTUs(vector<nodeP> &vec,nodeP fromHereDown ) const {
	vec.clear();
	getAllHTUsPrivate(vec,fromHereDown);
}


void tree::getAllHTUsPrivate(vector<nodeP> &vec,nodeP fromHereDown ) const {
	if (fromHereDown == NULL) return;
	if (fromHereDown->isInternal()) vec.push_back(fromHereDown);
	for (int k=0; k < fromHereDown->getNumberOfSons(); k++) {
		getAllHTUsPrivate(vec,fromHereDown->getSon(k));
	}
	return;
}


void tree::getAllNodes(vector<nodeP> &vec,nodeP fromHereDown ) const {
	vec.clear();
	getAllNodesPrivate(vec,fromHereDown);
}


void tree::getAllNodesPrivate(vector<nodeP> &vec,nodeP fromHereDown ) const {
	//DFS: depth first search
	if (fromHereDown == NULL) 
		return;
	vec.push_back(fromHereDown);
	for (int k=0; k < fromHereDown->getNumberOfSons(); k++) {
		getAllNodesPrivate(vec,fromHereDown->getSon(k));
	}
	return;
}


void tree::getAllLeaves(vector<nodeP> &vec,nodeP fromHereDown ) const {
	vec.clear();
	getAllLeavesPrivate(vec,fromHereDown);
}


void tree::getAllLeavesPrivate(vector<nodeP> &vec,nodeP fromHereDown ) const {
	if (fromHereDown == NULL) return;
	if (fromHereDown->isLeaf()) vec.push_back(fromHereDown);
	for (int k=0; k < fromHereDown->getNumberOfSons(); k++) {
		getAllLeavesPrivate(vec,fromHereDown->getSon(k));
	}
	return;
}

MDOUBLE tree::findLengthBetweenAnyTwoNodes(const nodeP node1, const nodeP node2) const {
	vector<nodeP> pathMatrix;
	MDOUBLE sumOfDistances =0;
	getPathBetweenAnyTwoNodes(pathMatrix, node1, node2);
	for (int i=0; i < pathMatrix.size() ; i++) {
		if (pathMatrix[i]->father() != NULL) 
			sumOfDistances += pathMatrix[i]->dis2father();
	}
	return sumOfDistances;
}

// find length between two neighbouring nodes only
MDOUBLE tree::lengthBetweenNodes(const nodeP i, const nodeP j) const {
	if (i->father() == j) 
		return i->dis2father();
	assert (j->father() == i);
	return j->dis2father();
}  

//*******************************************************************************
// change tree topoplogy parameters - should be applied carefully
//*******************************************************************************

//set the new root at p_iNewRoot
void tree::rootAt(const nodeP p_iNewRoot) {
	if (_root == p_iNewRoot) 
		return;
	vector<nodeP> pathMatrix;
	getPathBetweenAnyTwoNodes(pathMatrix, _root, p_iNewRoot);
	//pathMatrix size is always bigger than 2.

	for (int i = 0; i < pathMatrix.size() - 1 ; i++) {
		pathMatrix[i]->_father = pathMatrix[i+1];
		pathMatrix[i]->setDisToFather( pathMatrix[i+1]->dis2father() );
		pathMatrix[i]->removeSon(pathMatrix[i+1]);
		pathMatrix[i+1]->_sons.push_back(pathMatrix[i+1]->father());
		pathMatrix[i+1]->_father = NULL;
	}
	_root = p_iNewRoot;
}

//create new names to all internal nodes.
//the new name will be NXX, where XX is htu number
void tree::create_names_to_internal_nodes() {
	vector<nodeP> htuVec;
	getAllHTUs(htuVec,_root);

	for (int i=0; i<htuVec.size(); ++i) {
		string name = int2string(i+1);
		htuVec[i]->setName((string)"N" + name);
	}
}


void  tree::multipleAllBranchesByFactor(MDOUBLE InFactor) {
	vector<nodeP> vec;
	getAllNodes(vec,_root );
	for (int i = 0; i < vec.size(); ++i) {
		if (vec[i]->father() != NULL) 
			vec[i]->setDisToFather(vec[i]->dis2father() * InFactor);
	}
	_root->setDisToFather(TREE_NULL);
}


void tree::createFlatLengthMatrix(const MDOUBLE newFlatDistance) {
	vector<nodeP> vec;
	getAllNodes(vec,_root );
	for (int i=0; i< vec.size(); ++i) {
		if (vec[i]->father() != NULL) vec[i]->setDisToFather(newFlatDistance);
	}
}

/*
void tree::set_length_to_father(nodeP iSon, MDOUBLE dLength) {
	iSon->setDisToFather(dLength);
}
*/

// helper function
class eqNameVLOCAL {
	public:
		explicit eqNameVLOCAL(const string& x) : _x(x) {}
		const string& _x;
		bool operator() (const tree::nodeP y){
			return _x == y->name();
		}
};

// removes sonNode from its father according to the name of sonNode
// this function should ONLY be used when the node, sonNode, is to be recycled soon!
// because this function does not change the number of leaves nor the number of nodes!
// nor does it change the father of sonNode.
void tree::removeNodeFromSonListOfItsFather(nodeP sonNode) {
	vector<tree::nodeP>::iterator vec_iter;
    vec_iter = remove_if(sonNode->_father->_sons.begin(), sonNode->_father->_sons.end(), eqNameVLOCAL(sonNode->name()));
    sonNode->father()->_sons.erase(vec_iter,sonNode->father()->_sons.end()); // pg 1170, primer.
}


//*******************************************************************************
// Input-Output
//*******************************************************************************


void tree::output(string treeOutFile, TREEformats fmt, bool withHTU ) const {
	ofstream os(treeOutFile.c_str());
	output(os, fmt, withHTU);
	os.close();
}




void tree::output(ostream& os, TREEformats fmt, bool withHTU) const {
	if (_root == NULL) {
		LOG(1,<<" empty tree "); 
		return; 
	}
	if (fmt == PHYLIP) 
		outputInPhylipTreeFormat(os, withHTU);
	else if (fmt == ANCESTOR) 
		outputInAncestorTreeFormat(os,withHTU);
	else if (fmt == ANCESTORID) 
		outputInAncestorIdTreeFormat(os,withHTU);
	os<<endl;		
}




void tree::outputInAncestorTreeFormat(
			ostream& treeOutStream, bool distances) const{
	time_t ltime;
	int i,k,spaces;
	vector<nodeP> vec;
	int maxNameLen = 0;

	getAllLeaves(vec,_root);
	for (int w=0; w<vec.size();++w) {
		if (maxNameLen<vec[w]->name().size()) maxNameLen = vec[w]->name().size();
	}
	maxNameLen++; // this is just the longest name of taxa plus one



	time( &ltime );
	treeOutStream<<"# created on "<< ctime( &ltime ) ;

	treeOutStream<<"name";
	spaces = maxNameLen-4;
	for (k=0;k<spaces;++k) treeOutStream<<" ";

	treeOutStream<<"father";
	spaces = 7-6;
	for (k=0;k<spaces;++k) treeOutStream<<" ";

	if (distances) {
		treeOutStream<<"disance to father";
		treeOutStream<<"    ";
	}
	
	treeOutStream<<" sons";
	spaces = maxNameLen-4;
	for (k=0;k<spaces;++k) treeOutStream<<" ";

	treeOutStream<<endl;
	

	for (i=0; i<vec.size();++i) {
		treeOutStream<<vec[i]->name();
		spaces = maxNameLen-vec[i]->name().size();
		for (k=0;k<spaces;++k) treeOutStream<<" ";

		if (vec[i] != _root) {
			treeOutStream<<vec[i]->father()->name();
			spaces = 7-vec[i]->father()->name().size();
			for (k=0;k<spaces;++k) treeOutStream<<" ";
		}
		else {
			treeOutStream<<"root!";
			spaces = 7-5;
			for (k=0;k<spaces;++k) treeOutStream<<" ";
		}

		if ((vec[i] != _root) && distances) {
			treeOutStream<<vec[i]->dis2father();
		}

		for (int j=0; j < vec[i]->getNumberOfSons(); j++) {
			treeOutStream<<" "<<vec[i]->_sons[j]->name();
		}
		treeOutStream<<endl;
	}

	vec.clear();
	getAllHTUs(vec,_root );

	for (i=0; i<vec.size();++i) {
		treeOutStream<<vec[i]->name();
		spaces = maxNameLen-vec[i]->name().size();
		for (k=0;k<spaces;++k) treeOutStream<<" ";

		if (vec[i] != _root) {
			treeOutStream<<vec[i]->father()->name();
			spaces = 7-vec[i]->father()->name().size();
			for (k=0;k<spaces;++k) treeOutStream<<" ";
		}
		else {
			treeOutStream<<"root!";
			spaces = maxNameLen-5;
			for (k=0;k<spaces;++k) treeOutStream<<" ";
		}
		
		if (vec[i] != _root && distances) treeOutStream<<vec[i]->dis2father();
		
		for (int j=0; j < vec[i]->getNumberOfSons(); j++) {
			treeOutStream<<" "<<vec[i]->_sons[j]->name();
		}
		treeOutStream<<endl;
	}
}


void tree::outputInPhylipTreeFormat(ostream& os, bool withHTU ) const {
	// special case of a tree with 1 or 2 taxa.
	if (getLeavesNum() == 1) {
		os<<"("<<_root->name()<<")"<<endl;
		return;
	}
	else if (getLeavesNum() == 2) {
		os<<"("<<_root->name()<<":0.0,";

		os<<_root->getSon(0)->name()<<":" <<setiosflags(ios::fixed) <<_root->getSon(0)->dis2father()<<")"<<endl;
		return;
	}
	// ========================================
	os<<"(";
	// going over all the son
	int i;
	for (i=0; i<_root->getNumberOfSons()-1; ++i)
	{
		print_from(_root->getSon(i),os, withHTU);
		os<<",";
	}
	
	print_from(_root->getSon(i),os, withHTU);
	os<<")";
	if (withHTU==true) 
			os<<_root->name();
	char c=';';
	os<<c;
}


int tree::print_from(nodeP from_node, ostream& os, bool withHTU ) const {
	int i;
	if (from_node->isLeaf()) 
		os<<from_node->name();
	else {
		os<<"(";
		for (i=0; i<from_node->getNumberOfSons()-1; ++i) {
			print_from(from_node->getSon(i),os,withHTU);
			os<<",";
		}
		print_from(from_node->getSon(i),os,withHTU);
		os<<")";
		if (withHTU==true) 
			os<<from_node->name();
	}
	os<<":"<<setiosflags(ios::fixed) <<from_node->dis2father();
	return 0;
}


bool tree::readPhylipTreeTopology(istream &in) {
	const vector<char> tree_contents = PutTreeFileIntoVector(in);
	return readPhylipTreeTopology(tree_contents);
}

bool tree::readPhylipTreeTopology(istream &in,vector<char>& isFixed) {
	const vector<char> tree_contents = PutTreeFileIntoVector(in);
	return readPhylipTreeTopology(tree_contents,isFixed);
}



bool tree::readPhylipTreeTopology(const vector<char>& tree_contents) {
	vector<char> isFixed;
	return readPhylipTreeTopology(tree_contents,isFixed);
}

bool tree::readPhylipTreeTopology(const vector<char>& tree_contents,vector<char>& isFixed) {


	int nextFreeID =0; // to give id's for nodes.
	_leaves = GetNumberOfLeaves(tree_contents);
	_root = new TreeNode(nextFreeID);
	++nextFreeID;
	_nodes = GetNumberOfInternalNodes(tree_contents) + _leaves;

	isFixed.resize(_nodes,0); // 0 = not fixed, 1 = fixed.
	nodeP conection2part=NULL;
	vector<char>::const_iterator itCurrent = tree_contents.begin();

	if (verifyChar(itCurrent,OPENING_BRACE)||verifyChar(itCurrent,OPENING_BRACE2)){
		do {
				itCurrent++;
				conection2part = readPart(itCurrent,nextFreeID,isFixed);
				// readPart returns a pointer for him self
				_root->_sons.push_back(conection2part);
				conection2part->_father = _root;

		}  while (verifyChar(itCurrent, COMMA));
	}	
	if (!(verifyChar(itCurrent, CLOSING_BRACE)||verifyChar(itCurrent, CLOSING_BRACE2))) {
		errorMsg::reportError("Error reading tree file.",1); // also quit
	}

	// this part is for the cases where all the edges are fixed. In such case - this part changes
	// all the branches to not fixed.
	int z=0;
	bool allFixed = true;
	for (z=1; z< isFixed.size(); ++z) {
		if (isFixed[z] == 0) {
			allFixed = false;
			break;
		}
	}
	if (allFixed) {
		for (z=1; z< isFixed.size(); ++z) {
			isFixed[z] = 0;
		}
	}


	return true;
}


tree::nodeP tree::readPart(	vector<char>::const_iterator& p_itCurrent,
						   int& nextFreeID,
						   vector<char> & isFixed) {
	if ( IsAtomicPart(p_itCurrent) )	{
		// read the name, i.e. - the content from the file
		nodeP newLeaf = new TreeNode(nextFreeID);
		isFixed[nextFreeID] = 1; // all edges to the leaves are fixed...
		++nextFreeID;

		// puting the name
		string tmpname;
		tmpname.erase();

		while (((*p_itCurrent)!=')') && 
			((*p_itCurrent)!='(') && 
			((*p_itCurrent)!=':') &&
			((*p_itCurrent)!=',') &&
			((*p_itCurrent)!='}') &&
			((*p_itCurrent)!='{')) 
		{
			tmpname +=(*p_itCurrent);
			++p_itCurrent;
		} // NEW
	
		
		
		
		newLeaf->setName(tmpname);	

		// if a number(==distance) exists on the right-hand, update the distance table
		if ( DistanceExists(p_itCurrent) ) 
			newLeaf->setDisToFather(getDistance(p_itCurrent));
		return newLeaf;
		
	}
	else // this is a complex part
	{
		nodeP newHTU = new TreeNode(nextFreeID);
		++nextFreeID;
		nodeP conection2part=NULL;

		do {
			++p_itCurrent;
			conection2part = readPart(p_itCurrent,nextFreeID,isFixed);
			conection2part->_father = newHTU;
			newHTU->_sons.push_back(conection2part);
		} while (verifyChar(p_itCurrent, COMMA));
		if (verifyChar(p_itCurrent, CLOSING_BRACE)) {
			isFixed[newHTU->id()] = 1;
		} else if (verifyChar(p_itCurrent, CLOSING_BRACE2)) {
			isFixed[newHTU->id()] = 0;
		} else {
			errorMsg::reportError("Error reading tree file (2)");
		}
		++p_itCurrent;
		
		// if a number(==distance) exists on the right-hand, update the distance table
		if ( DistanceExists(p_itCurrent) )
			newHTU->setDisToFather(getDistance(p_itCurrent));
		return newHTU;
 
	}
}

//copy the information from other_nodePTR to a new node, and set the father to father_nodePTR
//does not update the number of nodes and leaves  
tree::nodeP tree::recursiveBuildTree(tree::nodeP father_nodePTR, const tree::nodeP other_nodePTR) {

	tree::nodeP childPTR = createNode(father_nodePTR, other_nodePTR->id());
	childPTR->setName(other_nodePTR->name());
	childPTR->setDisToFather(other_nodePTR->dis2father());
	for (int k = 0 ; k < other_nodePTR->getNumberOfSons() ; ++k) {
			recursiveBuildTree(childPTR, other_nodePTR->getSon(k));
	}
	return childPTR;
}



void tree::updateNumberofNodesANDleaves() {
	vector<nodeP> vec;
	getAllLeaves(vec,getRoot());
	_leaves = vec.size();
	vec.clear();
	getAllNodes(vec,getRoot());
	_nodes = vec.size();
}

//removeLeaf: removes nodePTR from tree. also deletes nodePTR
void tree::removeLeaf(nodeP nodePTR) {
	if (!(nodePTR->isLeaf())) {
		errorMsg::reportError("Error in function deleteLeaf - Unable to remove a node, which is not a leaf ");
	}
	
	if (getNodesNum() == 1) {
		delete getRoot();
		_root = NULL;
	}

	if (nodePTR->isRoot()) {
		assert (nodePTR->getNumberOfSons() == 1);
		nodeP sonOfRoot = nodePTR->getSon(0);
		rootAt(sonOfRoot);
	}

	// leaf is not the root:
	nodeP fatheOfLeafToRemove = nodePTR->father();
	fatheOfLeafToRemove->removeSon(nodePTR);
	delete nodePTR;

	int tmpSons = fatheOfLeafToRemove->getNumberOfSons();
	if (tmpSons == 1)
		shrinkNode(fatheOfLeafToRemove);
	else if ((_root == fatheOfLeafToRemove) && (tmpSons == 2)) {
		nodeP tmp = _root;
		rootAt(_root->getSon(0));
		shrinkNode(tmp);
	}
	if (_root->isLeaf() && _root->getNumberOfSons() >0 ) 
		rootAt(_root->getSon(0));
	updateNumberofNodesANDleaves();
	return;
}


//getAllBranches: returns two vectors such that nodesUp[i] is the father of nodesDown[i]
void tree::getAllBranches(vector<nodeP> &nodesUp, vector<nodeP> & nodesDown){
	vector<nodeP> localVec;
	getAllNodes(localVec, _root);
	for (int i=0 ; i < localVec.size() ; i++) {
		if (localVec[i]->father() != NULL) {
			nodesUp.push_back(localVec[i]->father());
			nodesDown.push_back(localVec[i]);
		}
	}
	return;
}





// the idea is that if we have a node with only one son (a tree like: node1---node2---node3)
// we can eliminate node2 (which is nodePTR)
void tree::shrinkNode(nodeP nodePTR) {

	if (nodePTR->getNumberOfSons() != 1) {
		vector<string> err;
		err.push_back("you requested to eliminate a node with more than 1 sons.");
		err.push_back(" error in function shrink node");
		errorMsg::reportError(err); // also quit the program.
	}

	nodeP fatherNode = nodePTR->father();
	nodeP sonNode = nodePTR->getSon(0);

	// taking care of the son node:
	sonNode->_father = fatherNode;
	sonNode->setDisToFather(sonNode->dis2father() + nodePTR->dis2father());

	// takind car of father node
	fatherNode->removeSon(nodePTR);
	fatherNode->_sons.push_back(sonNode);

	// delete the nodePTR
	delete nodePTR;
	updateNumberofNodesANDleaves();
}


//createRootNode: erase the current tree and create a tree with one node. 
void tree::createRootNode() {
	clear();
	_root = new TreeNode(0);
	_leaves=1;
	_nodes=1;
}


tree::nodeP tree::createNode(nodeP fatherNode, const int id) {
	nodeP tmp = new TreeNode(id);
	_nodes++;
	if (!fatherNode->isLeaf()) {
		// if fatherNode is a leaf then we remove one leaf and add one leaf, so no change.
		++_leaves; 
	}
	// there is one case when your father IS a leaf and yet you have to increase the number of leaves
	// this is when you father is the root, and you add the first child
	if (fatherNode->isRoot() && fatherNode->getNumberOfSons()==0) {
		++_leaves;
	}
	tmp->_father = fatherNode;
	fatherNode->setSon(tmp);
	return tmp;
}

bool tree::withBranchLength() const{
	if (_root->_sons.empty()) return false;
	else if (_root->getSon(0)->dis2father() != TREE_NULL) return true;
	return false;
}
 
ostream &operator<<(ostream &out, const tree &tr){
	tr.output(out,tree::ANCESTOR);
	return out;
}

/*
void tree::fillNodesID() {
	vector<nodeP> vec;
	getAllNodes(vec,_root );
	for (int i=0; i< vec.size(); ++i) {
		vec[i]->setID( i);
	}
}
*/



/*
void tree::cut_tree_in_two_leaving_interMediate_node(nodeP node2split,tree &small1,tree &small2) const {
	tree tmpCopyOfThisTree = (*this);
	nodeP node2splitOnNewTree = tmpCopyOfThisTree.getNodeByName(node2split->name());
	string interNode = "interNode";
	assert(node2split->father() != NULL);
	nodeP tmp = tmpCopyOfThisTree.makeNodeBetweenTwoNodes(node2splitOnNewTree->father(),node2splitOnNewTree, interNode);
	tmpCopyOfThisTree.rootAt(tmp);
	tmpCopyOfThisTree.cut_tree_in_two_special(tmp, small1,small2);
	nodeP toDel1 = small1.getNodeByName(interNode);
};
*/


void tree::outputInAncestorIdTreeFormat(
			ostream& treeOutStream, bool distances) const{
	time_t ltime;
	int i,k,spaces;
	vector<nodeP> vec;
	int maxNameLen = 0;

	getAllLeaves(vec,_root);
	for (int w=0; w<vec.size();++w) {
		if (maxNameLen<vec[w]->name().size()) maxNameLen = vec[w]->name().size();
	}
	maxNameLen++; // this is just the longest name of taxa plus one
	maxNameLen+=5;		// MN


	time( &ltime );
	treeOutStream<<"# created on "<< ctime( &ltime ) ;

	treeOutStream<<"name";
	spaces = maxNameLen-4;
	for (k=0;k<spaces;++k) treeOutStream<<" ";

	treeOutStream<<"father";
	spaces = 7-6;
	for (k=0;k<spaces;++k) treeOutStream<<" ";

	if (distances) {
		treeOutStream<<"disance to father";
		treeOutStream<<"    ";
	}
	
	treeOutStream<<" sons";
	spaces = maxNameLen-4;
	for (k=0;k<spaces;++k) treeOutStream<<" ";

	treeOutStream<<endl;
	

	for (i=0; i<vec.size();++i) {
	  treeOutStream<<vec[i]->name()<<"("<<vec[i]->id()<<")";
	  int len=3; if (vec[i]->id()>=10) len++;if (vec[i]->id()>=100) len++;
	  spaces = maxNameLen-vec[i]->name().size()-len;
		for (k=0;k<spaces;++k) treeOutStream<<" ";

		if (vec[i] != _root) {
			treeOutStream<<vec[i]->father()->name();
			spaces = 7-vec[i]->father()->name().size();
			for (k=0;k<spaces;++k) treeOutStream<<" ";
		}
		else {
			treeOutStream<<"root!";
			spaces = 7-5;
			for (k=0;k<spaces;++k) treeOutStream<<" ";
		}

		if ((vec[i] != _root) && distances) {
			treeOutStream<<vec[i]->dis2father();
		}
		//else treeOutStream<<"    ";

		for (int j=0; j < vec[i]->getNumberOfSons(); j++) {
			treeOutStream<<" "<<vec[i]->_sons[j]->name();
		}
		treeOutStream<<endl;
	}

	vec.clear();
	getAllHTUs(vec,_root );

	for (i=0; i<vec.size();++i) {
		treeOutStream<<vec[i]->name()<<"("<<vec[i]->id()<<")";
		int len=3; if (vec[i]->id()>=10) len++;if (vec[i]->id()>=100) len++;
		spaces = maxNameLen-vec[i]->name().size()-len;
		for (k=0;k<spaces;++k) treeOutStream<<" ";

		if (vec[i] != _root) {
			treeOutStream<<vec[i]->father()->name();
			spaces = 7-vec[i]->father()->name().size();
			for (k=0;k<spaces;++k) treeOutStream<<" ";
		}
		else {
			treeOutStream<<"root!";
			spaces = maxNameLen-5;
			for (k=0;k<spaces;++k) treeOutStream<<" ";
		}
		
		if (vec[i] != _root && distances) treeOutStream<<vec[i]->dis2father();
		
		for (int j=0; j < vec[i]->getNumberOfSons(); j++) {
			treeOutStream<<" "<<vec[i]->_sons[j]->name();
		}
		treeOutStream<<endl;
	}
}

//1. remove one of the root's sons. this node is called "toRemove"
//2. attach the sons of toRemove to the root.
//toRemove must have 2 sons so that the the root will have 3 sons.
//3. change the distToFather of the root's other son to be the sum of the distances of the root and its two sons 
//in practice: this func erase the root and makes toRemove the new root
void tree::rootToUnrootedTree() {
	if (getRoot()->getNumberOfSons() > 2) return; // tree is already unrooted!

	if (getRoot()->getSon(0)->getNumberOfSons() == 0) {
		tree::nodeP toRemove = getRoot()->getSon(1);
		getRoot()->getSon(0)->setDisToFather(getRoot()->getSon(1)->dis2father() + getRoot()->getSon(0)->dis2father());
		getRoot()->setSon(toRemove->getSon(0));
		for (int k = 1; k < toRemove->getNumberOfSons(); ++k) {
			getRoot()->setSon(toRemove->getSon(k));
		}
		delete toRemove;
		getRoot()->claimSons();
	}
	else {
		tree::nodeP toRemove = getRoot()->getSon(0);
		getRoot()->getSon(1)->setDisToFather(getRoot()->getSon(0)->dis2father() + getRoot()->getSon(1)->dis2father());
		getRoot()->setSon(toRemove->getSon(0));
		for (int k = 1; k < toRemove->getNumberOfSons(); ++k) {
			getRoot()->setSon(toRemove->getSon(k));
		}
		delete toRemove;
		getRoot()->claimSons();
	}
	updateNumberofNodesANDleaves();
}
