#include "runsettings.h"
#include <ctype.h>
#include <math.h>

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


void upcaseString(char *s) { for(int i=0;i<strlen(s);i++) s[i]=toupper(s[i]); }

int RunSetting::count = 0;

RunSetting *(RunSetting::inputSettings[250]); // Array of run settings.

int RunSetting::getCount() { return count; }

RunSetting *RunSetting::getRunSetting(int i) { return inputSettings[i]; }

void RunSetting::setValue(char *v, int i) 
{
  if(line)
    error << "Warning: parameter (" << name << ") multiply defined (line "
	  << line << " and line " << i << ")." << endError;
  line = i;
  strcpy(val,v);
}

RunSetting* RunSetting::findInputSetting(const char *s) 
{
  /* Returns a pointer to a parameter given the name s of a valid parameter.
     Otherwise returns NULL. */

  if(strcmp(s,"")==0)
    return NULL;
  for(int i=0;i<count;i++)
    if(strcmp(s,inputSettings[i]->name)==0)
      return inputSettings[i];
  return NULL;
}

void RunSetting::parseError() 
{
  error << "Error: Could not understand parameter value '"
	<< stripRightSpaces(val,val+strlen(val)) << "' (line "
	<< line << ")." << endError;
  quit(1);
}

int RunSetting::fndString(char *v, int n, const char* const vl[], int& intval)
{
  char s[MAX_LINE], vl2[MAX_LINE];
  ISTRSTREAM iss(val);
  iss >> s;
  upcaseString(s);
  for(intval=0;intval<n;intval++) {
    strcpy(vl2,vl[intval]);
    upcaseString(vl2);
    if(strcmp(s,vl2)==0)
      return 1;
  }
  return 0;
}

char *RunSetting::parseString() 
{
  stripRightSpaces(val,val+strlen(val));
  return val;
}

void RunSetting::boundsError() 
{
  error << "Error: " << name << "(" << val << ") must be in the range " << bounds
	<< " (line " << line << ")" << endError;
  quit(1);
}

void RunSetting::boundsParseError() 
{
  error << "Internal Error: Could not understand bounds " << bounds << " in run setting "
	<< name << "." << endl;
  quit(1);
}

int RunSetting::parseSign(char *&p) 
{ 
  if(*p=='-') {
    p++;
    return -1;
  }
  else if(*p=='+')
    p++;
  return 1;
}

int RunSetting::parseInfinity(char *&p) 
{
  if(toupper(p[0])=='I' && toupper(p[1])=='N' && toupper(p[2])=='F') {
    p += 3;
    return 1;
  }
  return 0;
}
 
int RunSetting::parseInt(char *&p) 
{
  if(!isdigit(*p)) 
    boundsParseError();
  int n = *p - '0';
  p++;
  while(isdigit(*p)) {
    n = 10 * n + *p - '0';
    p++;
  }
  return n;
}

double RunSetting::parseDouble(char *&p) 
{
  int n;
  if(isdigit(*p)) {
    n = *p - '0';
    p++;
  }
  else if(*p='.')
    n = 0;
  else 
    boundsParseError();
  while(isdigit(*p)) {
    n = 10 * n + *p - '0';
    p++;
  }
  if(*p!='.')
    return (double)n;
  p++;
  int f=0,s=1;
  while(isdigit(*p)) {
    f = 10 * f + *p - '0';
    s *= 10;
    p++;
  }
  return (double)n + double(f)/double(s);
}

void RunSetting::checkIntBounds(int v) 
{
  if(bounds[0]=='\0')
    return;
  char ch;
  char *p=bounds;
  int strict, sign, n;
  while(isspace(*p))
    p++;
  if(*p=='[')
    strict = 0;
  else if(*p=='(')
    strict = 1;
  else 
    boundsParseError();
  p++;
  while(isspace(*p))
    p++;
  sign = parseSign(p);
  if(parseInfinity(p)) {
    if(sign!=-1)
      boundsParseError();
  }
  else {
    n = parseInt(p) * sign;
    if(strict ? v <= n : v < n)
      boundsError();
  }
  while(isspace(*p))
    p++;
  if(*p!=',')
    boundsParseError();
  p++;
  while(isspace(*p))
    p++;
  sign = parseSign(p);
  if(parseInfinity(p)) {
    if(sign!=1)
      boundsParseError();
  }
  else {
    n = parseInt(p) * sign;
    while(isspace(*p))
      p++;      
    if(*p==']')
      strict = 0;
    else if(*p==')')
      strict = 1;
    else 
      boundsParseError();
    if(strict ? v >= n : v > n)
      boundsError();
  }
}

void RunSetting::checkDoubleBounds(double v) 
{
  if(bounds[0]=='\0')
    return;
  char *p=bounds;
  int strict, sign;
  double n;
  while(isspace(*p))
    p++;
  if(*p=='[')
    strict = 0;
  else if(*p=='(')
    strict = 1;
  else 
    boundsParseError();
  p++;
  while(isspace(*p))
    p++;
  sign = parseSign(p);
  if(parseInfinity(p)) {
    if(sign!=-1)
      boundsParseError();
  }
  else {
    n = parseDouble(p) * sign;
    if(strict ? v <= n : v < n)
      boundsError();
  }
  while(isspace(*p))
    p++;
  if(*p!=',')
    boundsParseError();
  p++;
  while(isspace(*p))
    p++;
  sign = parseSign(p);
  if(parseInfinity(p)) {
    if(sign!=1)
      boundsParseError();
  }
  else {
    n = parseDouble(p) * sign;
    while(isspace(*p))
      p++;      
    if(*p==']')
      strict = 0;
    else if(*p==')')
      strict = 1;
    else 
      boundsParseError();
    if(strict ? v >= n : v > n)
      boundsError();
  }
}

class ParseTree {
private:
  enum Comp { EQ, NEQ, LEQ, REQ };

  class PTNode {
  public:
    int numKeys;		// For a leaf, the number of keys in rs[].
    RunSetting *rs[200];	// For a leaf, the list of runsettings.
    Comp comp;			// For a leaf, the comparison operation.
    int numValues;		// For a leaf, the number of values in val.
    char val[200][MAX_LINE];	// For a leaf, the list of values.
    char op;			// For an internal node or a compressd leaf, the operation.
    PTNode *left, *right;	// The children of the node.

    PTNode() : left(NULL), right(NULL) {}
    PTNode(PTNode *l, char c, PTNode *r) : op(c), left(l), right(r) {}
    PTNode(RunSetting *r, Comp c, char *v) : comp(c), numKeys(1), numValues(1), left(NULL), right(NULL) {
      rs[0] = r;
      strcpy(val[0],v);
    }
  };

public:
  ParseTree(char *&p) { top = parseTreeInt(p,0,p); }
  int eval() { return evalInt(top); }
  void print(ostream& c) const { printInt(c,top,0,0,0); }
  void reduce() { top = reduceInt(top); } 

private:
  PTNode *top;
  PTNode *parseTreeInt(char *&,int,char *);
  PTNode *addNode(PTNode *, char, PTNode *);
  int attachNode(PTNode *,char,PTNode *);
  int printInt(ostream&,const PTNode *,int,int,int) const;
  int count(PTNode *,char) const;
  int evalInt(PTNode *);
  PTNode *reduceInt(PTNode *);
  int compare(const RunSetting *,const char *);
};

ParseTree::PTNode *ParseTree::addNode(PTNode *p, char c, PTNode *q) 
{ 
  if(p->left==NULL && p->comp==EQ && attachNode(p,c,q))
    return q;
  else
    return new PTNode(p,c,q);
}

int ParseTree::attachNode(PTNode *p, char c, PTNode *q) 
{
  if(q->left!=NULL)
    if(c==q->op)
      return attachNode(p,c,q->left);
    else
      return 0;
  if((q->comp==EQ || (q->comp==REQ && q->op==c)) && p->rs[0]==q->rs[0] && c=='|') {
    q->op = c;
    q->comp = REQ;
    strcpy(q->val[q->numValues++],p->val[0]);
    return 1;
  }
  else if((q->comp==EQ || (q->comp==LEQ && q->op==c)) && strcmp(p->val[0],q->val[0])==0) {
    q->op = c;
    q->comp = LEQ;
    q->rs[q->numKeys++] = p->rs[0];
    return 1;
  }
  else
    return 0;
}

int ParseTree::printInt(ostream &c, const PTNode *p, int prec, int number, int outOf) const
{
  if(p==NULL)
    return number;
  if(p->left==NULL) {// leaf
    switch(p->comp) {
    case EQ:
      c << p->rs[0]->getName() << " is " << p->val[0];
      break;
    case NEQ: 
      c << p->rs[0]->getName() << " is not " << p->val[0];
      break;
    case LEQ: {
      if(p->numKeys==2)
	c << (p->op=='&' ? "both " : "either ") << p->rs[1]->getName()
	  << (p->op=='&' ? " and " : " or ") << p->rs[0]->getName();
      else { // p->numKeys>2
	c << (p->op=='&' ? "all of " : "at least one ") << p->rs[p->numKeys-1]->getName();
	for(int i=p->numKeys-2;i>0;i--)
	  c << ", " << p->rs[i]->getName();
	c << ", and " << p->rs[0]->getName();
      }
      c << (p->op=='&' ? " are " : " is ") << p->val[0];
      break;
    }
    case REQ: 
      c << p->rs[0]->getName() << " is ";
      if(p->numValues==2)
	c << "either " << p->val[1] << " or " << p->val[0];
      else {
	c << "one of " << p->val[p->numValues-1];
	for(int i=p->numValues-2;i>0;i--)
	  c << ", " << p->val[i];
	c << ", or " << p->val[0];
      }
      break;
    }
    return number+1;
  }
  else {
    int newPrec=(p->op=='&' ? 2 : 1);
    if(newPrec<prec) {
      c << "(";
      printInt(c,p,0,0,0);
      c << ")";
      return number+1;
    }
    else {
      int oldNumber=number;
      if(newPrec>prec) {
	number = 1;
	outOf = count(p->left,p->op) + count(p->right,p->op);
      }
      number = printInt(c,p->left,newPrec,number,outOf);
      if(p->op=='&')
	c << (outOf==2 ? " and " : (number==outOf ? ", and " : ", "));
      else
	c << (outOf==2 ? " or " : (number==outOf ? ", or " : ", "));
      number = printInt(c,p->right,newPrec,number,outOf);
      return (newPrec>prec ? oldNumber+1 : number);
    }
  }
}

int ParseTree::count(PTNode *p, char c) const
{
  return ((p->left==NULL || p->op!=c) ? 1 : count(p->left,c)+count(p->right,c));
}  

ParseTree::PTNode *ParseTree::reduceInt(PTNode *p)
{
  if(p->left==NULL) // leaf
    if(evalInt(p))
      return NULL;
    else if(p->comp!=LEQ || p->op!='&')
      return p;
    else {
      PTNode *q = new PTNode();
      q->op = '&';
      q->numValues = 1;
      strcpy(q->val[0],p->val[0]);
      q->numKeys = 0;
      for(int i=0;i<p->numKeys;i++)
	if(!compare(p->rs[i],p->val[0]))
	  q->rs[q->numKeys++] = p->rs[i];
      q->comp = (q->numKeys==1 ? EQ : LEQ);
      return q;
    }
  else { // Internal node
    PTNode* l=reduceInt(p->left);
    PTNode* r=reduceInt(p->right);
    if(p->op=='&')
      return (l==NULL ? r : (r==NULL ? l : new PTNode(l,'&',r)));
    else
      return ((l==NULL || r==NULL) ? NULL : new PTNode(l,'|',r));
  }
}

int ParseTree::evalInt(PTNode *p) 
{
  if(p->left==NULL) // leaf
    switch(p->comp) {
    case EQ:  return compare(p->rs[0],p->val[0]);
    case NEQ: return !compare(p->rs[0],p->val[0]);
    case LEQ: {
      int b = compare(p->rs[0],p->val[0]);
      for(int i=1;i<p->numKeys;i++) {
	int c=compare(p->rs[i],p->val[0]);
	b = (p->op=='&' ? (b && c) : (b || c));
      }
      return b;
    }
    case REQ: {
      int b = compare(p->rs[0],p->val[0]);
      for(int i=1;i<p->numValues;i++)
	b = b || compare(p->rs[0],p->val[i]);
      return b;
    }
    }
  else if(p->op=='&')
    return evalInt(p->left) && evalInt(p->right);
  else
    return evalInt(p->left) || evalInt(p->right);
}

int ParseTree::compare(const RunSetting *rs, const char *val)
{
  char val1[MAX_LINE],val2[MAX_LINE];
  strcpy(val1,rs->getValue());
  upcaseString(val1);
  strcpy(val2,val);
  upcaseString(val2);
  return ((strcmp(val1,val2))==0);
}

ParseTree::PTNode *ParseTree::parseTreeInt(char *&p, int prec, char *p0)
{
  PTNode *b;
  while(isspace(*p))
    p++;      
  if(*p=='(') {
    p++;
    b = parseTreeInt(p,0,p0);
    while(isspace(*p))
      p++;      
    if(*p!=')') {
      error << "Internal Error: Expected '(', found " << *p << " in " << p0 << endError;
      quit(1);
    }
    p++;
  }
  else {
    int i=0;
    char str[MAX_LINE];
    while(!isspace(*p) && *p!='!' && *p!='=')
      str[i++] = *(p++);
    str[i] = '\0';
    RunSetting* rs=RunSetting::findInputSetting(str);
    if(rs==NULL) {
      error << "Internal Error: Could not find run setting " << str << " in " << p0 << endError;
      quit(1);
    }
    while(isspace(*p))
      p++;      
    if((p[0]!='!' && p[0]!='=') || p[1]!='=') {
      error << "Internal Error: Expected '==' or '!=', found " << *p << " in " << p0 << endError;
      quit(1);
    }
    Comp comp=(p[0]=='!' ? NEQ : EQ);
    p += 2;
    while(isspace(*p))
      p++;      
    i = 0;
    while(!isspace(*p) && *p!=')' && *p!='\0')
      str[i++] = *(p++);
    str[i] = '\0';
    b = new PTNode(rs,comp,str);
  }
  for(;;) {
    while(isspace(*p))
      p++;      
    if(*p=='\0' || *p==')')
      return b;
    if(p[0]=='&' && p[1]=='&') {
      p += 2;
      b = addNode(b,'&',parseTreeInt(p,2,p0));
    }
    else if(p[0]=='|' && p[1]=='|') {
      if(prec>1)
	return b;
      p += 2;
      b = addNode(b,'|',parseTreeInt(p,1,p0));
    }
    else {
      error << "Internal Error: Expected ')', '&&', '||', or EOS; found " << *p << " in " << p0 << endError;
      quit(1);
    }
  }
}
  
void RunSetting::checkRestriction()
{
  if(line==0 || restriction[0]=='\0')
    return;
  char *p=restriction;
  ParseTree pt(p);
  if(!pt.eval()) {
    pt.reduce();
    error << "Warning: Parameter " << name << " is only used when ";
    pt.print(error);
    error << " (line " << line << ")." << endError;
  }
}
  
void RSDoubleArray::parseInput(int& v, int elementGiven[], double ardoubleval[])
{
  int okay=1;
  char s[MAX_LINE], *pos1, *pos2;

  strcpy(s,val);
  for(pos1=s;*pos1;pos1=pos2,v++) {
    if(v>=MAX_CATEGORIES) {
      error << "Warning: number of values exceeds maximum possible '"
	    << stripRightSpaces(val,val+strlen(val)) << "' (line "
	    << line << ")." << endError;
      break;
    }
    for(pos2=pos1;*pos2;pos2++)
      if(*pos2 == ',') {
	*pos2++ = '\0';
	break;
      }
    if(*pos1 == '\0') {
      elementGiven[v] = 0;
      ardoubleval[v] = 0;
    }
    else {
      ISTRSTREAM iss(pos1);
      if((iss>>ardoubleval[v])!=(void*)NULL)
	elementGiven[v] = 1;
      else {
	ardoubleval[v] = 0;
	okay = 0;
      }
    }
  }
  if(!okay)
    parseError();
}

void RSDoubleArray::parseParamList(const RunSettings &rs,
				   int numvals, double *input, 
				   Vector<double>& valarray, int *given,
				   double *ardoubleval)
{ 

  const double tolerance = 1.0e-14;
  int j,k,warned = 0;

  valarray.setSize(rs.getNumTags());
  if(numvals >= rs.getNumCategories()) {
    if(numvals > rs.getNumCategories())
      error << "Warning: Too many values in " << name 
	    << " (line " << line << "). Extra values ignored." << endError;
    for(j=0;j<rs.getNumCategories();j++)
      input[j] = ardoubleval[j];
    for(k=0;k<rs.getNumTags();k++) {
      j = rs.getCategories(k);
      if(!given[j-1]) {
	error << "Error: Value must be specified for " << name 
	      << " of category " << j << " (line " << line << ")." 
	      << endError;
	quit(1);
      }
      else {
	checkDoubleBounds(ardoubleval[j-1]);
	if(usesSingleKappa() && rs.getSingleKappa() && 
	   k > 0 && fabs(ardoubleval[j-1]-valarray[0])>tolerance) {
	  if(!warned) {
	    error << "Warning: " << name << " values differ in "
		  << "single kappa mode. Only first value will be used (line " 
		  << line << ")." << endError;
	    warned = 1;
	  }
	  valarray[k] = valarray[0];
	}
	else
	  valarray[k] = ardoubleval[j-1];
      }
    }
  }
  else if(numvals == 1 && ((usesSingleKappa() && rs.getSingleKappa()) || line==0)) {
    checkDoubleBounds(ardoubleval[0]);
    for(k=0;k<rs.getNumTags();k++)
      valarray[k] = ardoubleval[0];
    for(j=0;j<rs.getNumCategories();j++)
      input[j] = ardoubleval[0];
  }
  else {
    error << "Error: Too few values in " << name << " (line "
	  << line << ")." << endError;
    quit(1);
  }
}

void RunSetting::addCategory(int n, int *cats, int &numTags)
{
  int *i,*j;

  for(i=cats;i<cats+numTags && *i<=n;i++)
    if(*i == n)
      return;
  for(j=cats+numTags;j>i;j--)
    *j = *(j-1);
  *j = n;
  numTags++;
}

void RunSetting::printBadParam(char *param, int lineno)
{
  error << "'" << stripRightSpaces(param,param+strlen(param)) 
	<<"' (line " << lineno << ")." << endError;

}

const char* const RSTreeType::values[] = {"random","upgma","neighbor-joining","bambe","newick"};

void RSTreeType::parse(const RunSettings &rs) 
{
  if(!fndString(val,sizeof(values)/sizeof(values[0]),values,v))
    parseError();
  if(strcmp(values[v],"neighbor-joining")==0  && rs.getMclock()) {
    error << "Error: Neighbor joining cannot be used with the molecular "
	  << "clock " << "(line " << line << ")." << endError;
    quit(1);
  }
}

void RSDoubleArray::parse(const RunSettings &rs) 
{
    int v=0;
    int elementGiven[MAX_CATEGORIES];
    double ardoubleval[MAX_CATEGORIES];

    size = rs.getNumCategories();
    parseInput(v,elementGiven,ardoubleval);
    checkRestriction();
    parseParamList(rs,v,input,initial,elementGiven,ardoubleval);
  }


void RSCategoryList::parse(const RunSettings &rs) 
{
  strcpy(v,val);
  stripRightSpaces(v,v+strlen(v));
  char *sv = v;
  numTags = 0;
  if(!getCategories(sv,categories,val,line,numTags)) {
    error << "Error: Empty category list ";
    printBadParam(val,line);
    quit(1);
  }
  if(*sv == '*') {
    (sv)++;
    while(isspace(*sv))
      (sv)++;
  }
  if(*sv != '\0') {
    error << "Error: Category list ends badly ";
    printBadParam(val,line);
    quit(1);
  }
  numCategories = categories[numTags-1];
}

void RSBoolean::parse(const RunSettings &rs) 
{
  char s[MAX_LINE];
  ISTRSTREAM iss(val);
  if((iss>>s)==NULL)
    parseError();
  upcaseString(s);
  if(strcmp(s,"TRUE")==0)
    v = 1;
  else if(strcmp(s,"FALSE")==0)
    v = 0;
  else
    parseError();
  checkRestriction();
}
const char* const RSAlgorithm::values[] = {"global","local"};

void RSAlgorithm::parse(const RunSettings &rs)
{
  if(!fndString(val,sizeof(values)/sizeof(values[0]),values,v))
    parseError();
  checkRestriction();
}

const char* const RSModel::values[] = {"HKY85","F84","TN93","GREV"};;

void RSModel::parse(const RunSettings &rs)
{
  if(!fndString(val,sizeof(values)/sizeof(values[0]),values,v))
    parseError();
  checkRestriction();
}

int RunSetting::getCategories(char*& str, int *cats, char *param, 
				int lineno, int &numTags) 
{
  /* Parses a string representing a list of categories up to, but
     not including the final asterisk (if it exists).
     Checks for syntax errors and sets up the category array.
     str is the unscanned part of the category list string.
     cats is an array of categories (in order) seen so far.
     numTags is the number of distinct categories seen so far.
     Returns whether or not a category was read in from the string. */

  int n;

  while(isspace(*str))
    str++;
  if(*str == '(') {
    str++;
    if(!getCategories(str,cats,param,lineno,numTags)) {
      error << "Error: Empty group in category list ";
      printBadParam(param,lineno);
      quit(1);
    }
    while(isspace(*str))
      str++;
    if(*str == '*') {
      error << "Error: Asterisk must be the last symbol in the category list ";
      printBadParam(param,lineno);
      quit(1);
    }
    if(*str != ')') {
      error << "Error: Mismatched parentheses in category list ";
      printBadParam(param,lineno);
      quit(1);
    }
    str++;
  }
  else if(isdigit(*str)) {
    n = *str - '0';
    for(str++;isdigit(*str);str++)
      n = 10 * n + *str - '0';
    if(n == 0) {
      error << "Error: Category must be a positive number in category list ";
      printBadParam(param,lineno);
      quit(1);
    }
    else if(n > MAX_CATEGORIES) {
      error << "Error: Category must be less than "  << MAX_CATEGORIES
	    << " in category list ";
      printBadParam(param,lineno);
      quit(1);
    }
    addCategory(n,cats,numTags);
  }
  else 
    return 0;
  while(isspace(*str))
    str++;
  if(*str == '^') {
    str++;
    while(isspace(*str))
      str++;
    if(!isdigit(*str)) {
      error << "Error: Repeat count must be an integer in the category list ";
      printBadParam(param,lineno);
      quit(1);
    }
    n = *str - '0';
    for(str++;isdigit(*str);str++)
      n = 10 * n + *str - '0';
    if(n == 0) {
      error << "Error: Repeat count may not be zero in the category list ";
      printBadParam(param,lineno);
      quit(1);
    }
    while(isspace(*str))
      str++;
  }
  if(*str == ',') {
    str++;
    getCategories(str,cats,param,lineno,numTags);
  }
  return 1;
}

RunSettings::RunSettings(istream& f) :

  /* Reads in and processes the run settings from file f. Sets
     the corresponding object variables. A run setting not listed in the
     file is set to its default value. The run settings are processed
     in the order given above in this constructor. */

  seed("seed","194024933"),
  burn("burn","1000","[0,Inf)"),
  burnAlgorithm("burn-algorithm","GLOBAL"),
  mainAlgorithm("main-algorithm","LOCAL"),
  cycles("cycles","6000","[0,Inf)"),
  sampleInterval("sample-interval","10","(0,Inf)"),
  paramUpdateInterval("parameter-update-interval","1","[0,Inf)"),
  updateKappa("update-kappa","true","likelihood-model==HKY85 || likelihood-model==F84"),
  updateTheta("update-theta","true"),
  updatePi("update-pi","true"),
  updateTtp("update-ttp","true","likelihood-model==TN93"),
  updateGamma("update-gamma","true","likelihood-model==TN93"),
  updateGrev("update-grev","true","likelihood-model==GREV"),
  updateInvariantProb("update-invariant-prob","false"),
  tuneInterval("tune-interval","200","(0,Inf)"),
  windowInterval("window-interval","200","(0,Inf)"),
  molecularClock("molecular-clock","true"),
  model("likelihood-model","GREV"),
  categoryList("category-list","1*"),
  singleKappa("single-kappa","false"),
  initialKappa("initial-kappa","2.0","(0,Inf)","likelihood-model==HKY85 || likelihood-model==F84"),
  initialTheta("initial-theta","1.0","(0,Inf)"),
  estimatePi("estimate-pi","true"),
  initialPia("initial-pia","0.25","(0,Inf)","estimate-pi==false"),
  initialPic("initial-pic","0.25","(0,Inf)","estimate-pi==false"),
  initialPig("initial-pig","0.25","(0,Inf)","estimate-pi==false"),
  initialPit("initial-pit","0.25","(0,Inf)","estimate-pi==false"),
  initialTtp("initial-ttp","1.0","(0,Inf)","likelihood-model==TN93"),
  initialGamma("initial-gamma","1.0","(0,Inf)","likelihood-model==TN93"),
  initialRac("initial-rac","1.0","(0,Inf)","likelihood-model==GREV"),
  initialRag("initial-rag","1.0","(0,Inf)","likelihood-model==GREV"),
  initialRat("initial-rat","1.0","(0,Inf)","likelihood-model==GREV"),
  initialRcg("initial-rcg","1.0","(0,Inf)","likelihood-model==GREV"),
  initialRct("initial-rct","1.0","(0,Inf)","likelihood-model==GREV"),
  initialRgt("initial-rgt","1.0","(0,Inf)","likelihood-model==GREV"),
  useInvariantSites("use-invariant-sites","false"),
  invariantProbability("invariant-probability","0.1","[0,1]"),
  invariantProbPriorMean("invariant-prob-prior-mean","0.5","(0,1.0)"),
  invariantProbPriorSD("invariant-prob-prior-sd","0.25","(0,0.5)"),
  dataFile("data-file","infile"),
  outgroup("outgroup","1","(0,Inf)","molecular-clock==true"),
  globalTune("global-tune","0.1","(0,Inf)",
	     "burn-algorithm==global || main-algorithm==global"),
  kappaTune("kappa-tune","0.1","[0,Inf)",
  	    "(likelihood-model==HKY85 || likelihood-model==F84) && update-kappa==true "
	    "&& parameter-update-interval!=0"),
  thetaTune("theta-tune","2000.0","[0,Inf)",
	    "update-theta==true && parameter-update-interval!=0"),
  piTune("pi-tune","2000.0","[0,Inf)",
	 "update-pi==true && parameter-update-interval!=0"),
  ttpTune("ttp-tune","0.1","[0,Inf)",
	  "likelihood-model==TN93 && update-ttp==true && parameter-update-interval!=0"),
  gammaTune("gamma-tune","0.1","[0,Inf)", 
	    "likelihood-model==TN93 && update-gamma==true && parameter-update-interval!=0"),
  grevTune("grev-tune","2000.0","[0,Inf)",
	   "likelihood-model==GREV  && update-grev==true && parameter-update-interval!=0"),
  invariantProbTune("invariant-prob-tune","2000.0","[0,Inf)",
		    "update-invariant-prob==true && parameter-update-interval!=0"),
  localTune("local-tune","0.19","(0,Inf)",
	    "main-algorithm==local || burn-algorithm==local"),
  useBeta("use-beta","false",
	  "main-algorithm==local || burn-algorithm==local"),
  betaTune("beta-tune","10.0","(0,Inf)",
	   "use-beta==true"),
  printAllTrees("print-all-trees","true"),
  maxInitialTreeHeight("max-initial-tree-height","0.1","(0,Inf)"),
  fileRoot("file-root","run1"),
  initialTreeType("initial-tree-type","random"),
  treeFile("tree-file","treefile",
	   "initial-tree-type==bambe || initial-tree-type==newick"),
  newickFormat("newick-format","true") {

  char buf[MAX_LINE],*pos,*eqpos;
  int lineNum = 0;

  while(f.getline(buf,MAX_LINE)) {
    lineNum++;
    if((pos=strchr(buf,'#'))!=0)
      *pos = '\0';
    pos = buf+strspn(buf," \t\n");
    if(*pos!='\0')
      if((eqpos=strchr(pos,'='))!=0) {
	stripRightSpaces(pos,eqpos);
	RunSetting *rs=RunSetting::findInputSetting(pos);
	if(rs!=NULL) {
	  eqpos = eqpos+1+strspn(eqpos+1," \t\n");
	  stripRightSpaces(eqpos,eqpos+strlen(eqpos));
	  rs->setValue(eqpos,lineNum);
	}
	else {
	  error << "Error: Unknown run setting name (" << pos << ") (line " 
		<< lineNum << ")." << endError;
	  quit(1);
	}
      }
      else {
	error << "Error: Illegal syntax. Missing '=' (line " << lineNum << ")." 
	      << endError;
	quit(1);
      }
  }
  for(int i=0;i<RunSetting::getCount();i++)
    RunSetting::getRunSetting(i)->parse(*this);
}

