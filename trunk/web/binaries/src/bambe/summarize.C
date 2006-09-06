#define VERSION 8

/* Summarize for BAMBE, Version 8.
 * (c) Copyright 1998, 1999, 2001, 2002, 2003 by Donald Simon & Bret Larget
 * Department of Mathematics/Computer Science, Duquesne University
 * 2/6/03
 */

using namespace std;

#include <iostream>
#include <fstream>

#ifdef WINDOWS
#include <strstrea.h>
#define ISTRSTREAM istrstream
#define FMTFLAGS int
#elif (__GNUC__>=3)
#include <sstream>
#define ISTRSTREAM istringstream
#define FMTFLAGS std::_Ios_Fmtflags
#else
#include <strstream.h>
#define ISTRSTREAM istrstream
#define FMTFLAGS int
#endif

#include <iomanip>
#include <stdlib.h>
#include <string>
#include <ctype.h>

#define DEFAULTTHRESHOLD .80
#define MAXCLADETOPS 100

#define SSIZE 200003 /* Must be larger than MAXTREES */
#define CSIZE 10709 /* Must be larger than MAXCLADES */
#define MAXLEAVES 200
#define MAXLINE 6*MAXLEAVES
#define SETSIZE ((MAXLEAVES+30)/31)
#define MAXCLADES 5000
#define MAXTREES 150000
const unsigned int bit = (1<<30);

#define SHASH(k1,k2)	((((k1)<<6)+(k2)) % SSIZE)

void usageError(char *name)
{
  cerr << "Usage: " << name << " [-n skipped_lines] [-p threshold] "
       << "[-c maximum_clade_topologies] <file 1> <file 2> ..." << endl;
  exit(1);
}

typedef unsigned int Set[SETSIZE];

unsigned int chash(const Set s)	
{
  unsigned int n;

  n = s[0];
  for(int i=1;i<SETSIZE;i++)
    n = ((n << 6) + s[i]) % CSIZE;
  return n;
}

void makeEmptySet(Set s)
{
  for(int i=0;i<SETSIZE;i++)
    s[i] = 0;
}

void copySet(Set s1, const Set s2)
{
  for(int i=0;i<SETSIZE;i++)
    s1[i] = s2[i];
}

void setUnion(const Set s1, const Set s2, Set s)
{
  for(int i=0;i<SETSIZE;i++)
    s[i] = s1[i] | s2[i];
}

void setDifference(const Set s1, const Set s2, Set s)
{
  for(int i=0;i<SETSIZE;i++)
    s[i] = s1[i] & ~s2[i];
}

int setEqual(const Set s1, const Set s2)
{
  for(int i=0;i<SETSIZE;i++)
    if(s1[i] != s2[i])
      return 0;
  return 1;
}

int subset (const Set s1, const Set s2)
{
  for(int i=0;i<SETSIZE;i++)
    if(s1[i] & ~s2[i])
      return 0;
  return 1;
}

void singleton(const int n, Set s)
{
  for(int i=0;i<SETSIZE;i++)
    s[i] = 0;
  s[(n-1)/31] = (1 << (30 - ((n-1) % 31)));
}

void printSet(const Set s)
{
  int first = 1;
  unsigned int n;

  cout << "{";
  for(int i=0;i<SETSIZE;i++) {
    n = s[i];
    for(int j=1;j<32;j++,n<<=1)
      if(n & bit) {
	if(first)
	  first = 0;
	else 
	  cout << ",";
	cout << j+31*i;
      }
  }
  cout << "}";
}

void printElements(const Set s)
{
  unsigned int n;

  for(int i=0;i<SETSIZE;i++) {
    n = s[i];
    for(int j=1;j<32;j++,n<<=1)
    if(n & bit)
      cout << setw(12) << " " << setw(3) << j+31*i << endl;
  }
}

int cmpSet(const Set s1, const Set s2)
{
  int c;

  for(int i=0;i<SETSIZE;i++)
    if(c=(s1[i]-s2[i]))
      return c;
  return 0;
}

int cindex[CSIZE], sindex[SSIZE];
int num_clades = 0, num_trees = 0;

class Ctab {
 public:
  Set set;
  int count;
  int trees;
  int first;
  int name;
  int time;
  int lasttopnum;
  int trans[MAXCLADETOPS+1][MAXCLADETOPS+1];
} ctab[MAXCLADES];

class Stab {
 public:
  int left;
  int right;
  int count;
  int clade;
  int next;
  int name;
  int num;
} stab[MAXTREES];

void printTree(int k,int printId)
{
  if(k<0)
    cout << -k;
  else if(stab[k].name) {
    cout << char(ctab[stab[k].clade].name+'A'-1);
    if(printId)
      cout << stab[k].name;
  }
  else {
    cout << "(";
    printTree(stab[k].left,printId);
    cout << ",";
    printTree(stab[k].right,printId);
    cout << ")";
  }
}

void addTrans(int c, int t, int time, int cladetops)
{
  if(ctab[c].trees<=cladetops) {
    if(ctab[c].time == time-1)
      ctab[c].trans[ctab[c].lasttopnum][stab[t].num]++;
    else {
      ctab[c].trans[ctab[c].lasttopnum][0]++;
      ctab[c].trans[0][stab[t].num]++;
    }
    ctab[c].time = time;
    ctab[c].lasttopnum = stab[t].num;
  }
}

int increment(const Set s, unsigned int k, int i1, int i2, int time,
	      int cladetops)
{
  /* s is the set of taxa. k is the hash value of the subtree.
     i1 and i2 are the indices into the subtree table of the left
     and right subtrees, respectively. */

  int c,t;
  unsigned int j;

  while(t=sindex[k])
    if(stab[t].left == i1 && stab[t].right == i2) {
      stab[t].count++;
      c = stab[t].clade;
      ctab[c].count++;
      addTrans(c,t,time,cladetops);
      return t;
    }
    else
      k = (k+1) % SSIZE;

  if(num_trees == MAXTREES) {
    cerr << "Error: Too many distinct subtrees." << endl;
    exit(0);
  }

  t = sindex[k] = ++num_trees;
  stab[t].left = i1;
  stab[t].right = i2;
  stab[t].count = 1;

  j = chash(s);
  while(c=cindex[j])
    if(setEqual(ctab[c].set,s)) {
      stab[t].clade = c;
      ctab[c].count++;
      stab[t].num = ++ctab[c].trees;
      stab[t].next = ctab[c].first;
      ctab[c].first = t;
      stab[t].name = 0;
      addTrans(c,t,time,cladetops);
      return t;
    }
    else
      j = (j+1) % CSIZE;

  if(num_clades == MAXCLADES) {
    cerr << "Error: Too many distinct clades." << endl;
    exit(0);
  }

  c = cindex[j] = ++num_clades;
  stab[t].clade = c;
  copySet(ctab[c].set,s);
  ctab[c].count = 1;
  ctab[c].trees = 1;
  stab[t].next = -1;
  ctab[c].first = t;
  stab[t].name = 0;
  ctab[c].name = 0;
  ctab[c].time = time;
  ctab[c].lasttopnum = stab[t].num = 1;
  if(time>1) 
    ctab[c].trans[0][1]++;
  return t;
}
  
char *storeTree (char *top, int time, Set s, unsigned int& k, int& i,
		  int cladetops)
{
  char *top2, *top3;
  Set s1,s2;
  unsigned int k1,k2;
  int i1,i2;

  if(*top == '\0')
    return top;
  if(*top == '(') {
    top2 = storeTree(top+1,time,s1,k1,i1,cladetops);
    if(*top2 == ',')
      top2++;
    else {
      cerr << "Error: Missing comma - /" << top << "/" << endl;
      exit(1);
    }
    top3 = storeTree(top2,time,s2,k2,i2,cladetops);
    if(*top3 != ')') {
      cerr << "Error: Mismatched parentheses - /" << top << "/" << endl;
      exit(1);
    }
    setUnion(s1,s2,s);
    k = SHASH(k1,k2);
    i = increment(s, k, i1, i2, time, cladetops);
    return top3+1;
  }
  k = 0;
  for(;isdigit(*top);top++)
    k = 10 * k + (*top-'0');
  if(k>MAXLEAVES) {
    cerr << "Error: taxa number too large - " << k << endl;
    exit(1);
  }
  i = -k;
  singleton(k,s);
  return top;
}

char *makeSet (char *top, Set s)
{
  char *top2, *top3;
  Set s1,s2;
  int k;

  if(*top == '\0')
    return top;
  if(*top == '(') {
    top2 = makeSet(top+1,s1);
    if(*top2 == ',')
      top2++;
    else {
      cerr << "Error: Missing comma - /" << top << "/" << endl;
      exit(1);
    }
    top3 = makeSet(top2,s2);
    if(*top3 != ')') {
      cerr << "Error: Mismatched parentheses - /" << top << "/" << endl;
      exit(1);
    }
    setUnion(s1,s2,s);
    return top3+1;
  }
  k = 0;
  for(;isdigit(*top);top++)
    k = 10 * k + (*top-'0');
  if(k>MAXLEAVES) {
    cerr << "Error: taxa number too large - " << k << endl;
    exit(1);
  }
  singleton(k,s);
  return top;
}

int cmpTops2Int(int a, int b)
{ int c;

  if(a==b) 
    return 0;
  if(a<0)
    if(b<0)
      return b-a;
    else
      return -1;
  else if(b<0)
    return 1;
  else {
    if(stab[a].name)
      if(stab[b].name)
	return ctab[stab[a].clade].name - ctab[stab[b].clade].name;
      else 
	return -1;
    else if(stab[b].name)
      return 1;
    else 
      return ((c = cmpTops2Int(stab[a].left,stab[b].left)) ? c :
	      cmpTops2Int(stab[a].right,stab[b].right));
  }
}

int cmpTops2(const void *a, const void *b)
{
  return (cmpTops2Int(*((int *)a),*((int *)b)));
}

int cmpTops(const void *a, const void *b)
{
  return stab[*((int *)b)].count - stab[*((int *)a)].count;
}
  

int cmpClades(const void *a, const void *b)
{
  return cmpSet(ctab[*((int *)b)].set,ctab[*((int *)a)].set);
}

void addClade(int i, int clades[], int& num)
{
  int j=0;

  while(j<num)
    if(subset(ctab[i].set,ctab[clades[j]].set))
      return;
    else if(subset(ctab[clades[j]].set,ctab[i].set))
      clades[j] = clades[--(num)];
    else
      j++;
  clades[num++] = i;
}

int printClades(int min, int maxsub, int clades[], Set universalSet)
{
  unsigned int m;
  int i,j,k,l,c;
  int num=0;
  int tops[MAXCLADETOPS];
  Set allset;

  makeEmptySet(allset);
  for(m=0;m<CSIZE;m++)
    if((c=cindex[m]) && ctab[c].count >= min && ctab[c].trees <= maxsub) 
      addClade(c,clades,num);
  qsort(clades,num,sizeof(int),cmpClades);
  for(i=0;i<num;i++) {
    j = clades[i];
    ctab[j].name = i+1;
    setUnion(ctab[j].set,allset,allset);
    cout << setw(6) << ctab[j].count << "  " << char(i+'A') << "    ";
    printSet(ctab[j].set);
    cout << endl;
    for(k=ctab[j].first,l=0;k!=-1;k=stab[k].next,l++)
      tops[l] = k;
    qsort(tops,l,sizeof(tops[0]),cmpTops);
    for(k=0;k<l;k++) {
      cout << setw(6) << stab[tops[k]].count << "  " << char(i+'A');
      FMTFLAGS oldOptions = cout.setf(ios::left);
      cout << setw(3) << k+1 << " ";
      cout.flags(oldOptions);
      printTree(tops[k],1);
      cout << endl;
      stab[tops[k]].name = k+1;
    }
    cout << endl;
  }
  setDifference(universalSet,allset,allset);
  printElements(allset);
  return num;
}

void printTrees(int trees[], int n, int total, int printId)
{
  int sum = 0, k, m;

  qsort(trees,n,sizeof(int),cmpTops);
  for(k=0;k<n;k++) {
    m = stab[trees[k]].count;
    sum += m;
    cout << setw(5) << m << " " << setw(6) << setprecision(3)
	 << m/(double)total << " " << setw(6) << sum/(double)total << " ";
    printTree(trees[k],printId);
    cout << endl;
  }
}

void printTrans(const int clades[], int n, int total)
{
  int i, j, k, s, sum;
  int zerorow, zerocol;
  char cname;
  int (*cladeMatrix)[MAXCLADETOPS+1];
  int num[MAXCLADETOPS+1];

  for(i=0;i<n;i++) {
    s = ctab[clades[i]].trees;
    cname = char(ctab[clades[i]].name+'A'-1);
    cladeMatrix = ctab[clades[i]].trans;
    for(k=ctab[clades[i]].first;k!=-1;k=stab[k].next)
      num[stab[k].name] = stab[k].num;
    cout << "     | ";
    for(j=1;j<s+1;j++) {
      cout << "  " << cname;
      FMTFLAGS oldOptions = cout.setf(ios::left);
      cout << setw(3) << j;
      cout.flags(oldOptions);
    }
    zerocol = zerorow = sum = 0;
    for(k=0;k<s+1;k++)
      for(j=0;j<s+1;j++)
	sum += cladeMatrix[k][j];
    cladeMatrix[0][0] = total - 1 - sum;
    for(k=0;k<s+1;k++) {
      zerocol = zerocol || cladeMatrix[k][0];
      zerorow = zerorow || cladeMatrix[0][k];
    }
    if(zerocol)
      cout << "   -  ";
    cout << "\n-----+";
    for(j=1;j<s;j++)
      cout << "------";
    if(zerocol)
      cout << "------";
    cout << "-----";
    cout << endl;
    for(k=1;k<s+1;k++) {
      cout << " " << cname;
      FMTFLAGS oldOptions = cout.setf(ios::left);
      cout << setw(3) << k << "|";
      cout.flags(oldOptions);
      for(j=1;j<s+1;j++)
	cout << setw(5) << cladeMatrix[num[k]][num[j]] << " ";
      if(zerocol)
	cout << setw(5) << cladeMatrix[num[k]][0] << " ";
      cout << endl;
    }
    if(zerorow) {
      cout << "  -  |";
      for(j=1;j<s+1;j++)
	cout << setw(5) << cladeMatrix[0][num[j]] << " ";
      if(zerocol)
	cout << setw(5) << cladeMatrix[0][0] << " ";
      cout << endl;
    }
    cout << endl << endl;
  }
}

void addZeroTrans(int i, int time)
{
  int c;

  if(i>0) {
    c = stab[i].clade;
    if(ctab[c].time != time)
      ctab[c].trans[ctab[c].lasttopnum][0]++;
    addZeroTrans(stab[i].left,time);
    addZeroTrans(stab[i].right,time);
  }
}

void printCladeCount(int k, int total)
{
  int c;

  if(k>=0) {
    c = stab[k].clade;
    cout << setw(10) << ctab[c].count << " " << setw(6) << setprecision(3)
	 << (double)ctab[c].count/(double)total << " ";
    printSet(ctab[c].set);
    cout << endl;
    printCladeCount(stab[k].left,total);
    printCladeCount(stab[k].right,total);
  }
}

void printCommonClades(int total, int clades[])
{
  int num=0,j;
  for(int c=0;c<num_clades;c++)
    if(ctab[c].count>total/100)
    clades[num++] = c;
  qsort(clades,num,sizeof(int),cmpClades);
  for(int i=0;i<num;i++) {
    j = clades[i];
    cout << setw(10) << ctab[j].count << " " << setw(6) << setprecision(3)
	 << (double)ctab[j].count/(double)total << " ";
    printSet(ctab[j].set);
    cout << endl;
  }

}

void readFile(istream& f, char* firstLine, int& first, int n, int& total, 
	      Set set, unsigned int& k, int& j, int cladetops)
{
  char s[MAXLINE],c;
  for(int i=0;i<n;i++)
    if(f.getline(s,MAXLINE,'\n')==NULL) {
      cerr << "Error: Input contains " << (i==n-1 ? "exactly" : "fewer than ")
	   << n << " lines." << endl;
      exit(1);
    }
  while(f.get(s,MAXLINE,'\n')) {
    while((c=f.get())!=EOF && c !='\n')
      ;
    storeTree(s, ++total, set, k, j, cladetops);
    if(first) {
      strcpy(firstLine,s);
      first = 0;
    }
  }
}

void summarize(int n, double threshold, int cladetops,
	       char **names, int num_names)
{
  char s[MAXLINE];
  int total=0;
  Set set,universalSet;
  int i,l,r;
  unsigned int k;
  unsigned int m;
  int trees[SSIZE];
  int clades[MAXLEAVES],num;
  char **name;
  int first = 1;

  for(name=names;name<names+num_names;name++) {
    if(strcmp(*name,"-")==0)
      readFile(cin,s,first,n,total,set,k,i,cladetops);
    else {
      ifstream f(*name);
      if(!f) {
	cerr << "Error: Could not open " << *name << endl;
	exit(1);
      }
      readFile(f,s,first,n,total,set,k,i,cladetops);
      f.close();
    }
  }

  if(total==0) {
    cerr << "Error: Input contains exactly " << n << " lines." << endl;
    exit(1);
  }

  addZeroTrans(i,total);
  makeSet(s,universalSet);
  cout << "******************** Named clades "
       << "********************" << endl << endl;
  num = printClades(int(total*threshold),cladetops,clades,universalSet);
  cout << endl;

  m = chash(universalSet);
  while(!(setEqual(ctab[cindex[m]].set,universalSet)))
      m = (m+1) % CSIZE;
  for(r=ctab[cindex[m]].first,l=0;r!=-1;r=stab[r].next,l++)
    trees[l] = r;
  cout <<  "******************** Tree topologies "
       << "********************"  << endl << endl;
  cout << "Count  Prob.  Cum.  Tree topology" << endl;
  printTrees(trees,l,total,1);
  cout << endl;

  cout << "***** Posterior probabilities of clades in most probable tree "
       << "topology *****" << endl << endl;
  cout << "     Count  Prob. Tree topology" << endl;
  printCladeCount(trees[0],total);
  cout << endl;

  cout << "******************** Clade transition matrices "
       << "********************" << endl << endl;
  printTrans(clades,num,total);

  qsort(trees,l,sizeof(int),cmpTops2);
  for(r=1,i=0;r<l;r++)
    if(cmpTops2Int(trees[i],trees[r])==0)
      stab[trees[i]].count += stab[trees[r]].count;
    else
      trees[++i] = trees[r];

  cout << "******************** Clade tree topologies "
       << "********************" << endl << endl;
  cout << "Count  Prob.  Cum.  Tree topology" << endl;
  printTrees(trees,i+1,total,0);

  cout << endl;
  cout << "************************ Common Clades ";
  cout << "************************" << endl << endl;
  printCommonClades(total,clades);
}

int main(int argc, char *argv[])
{
  int n=0,cladetops=MAXCLADETOPS,num_names=0;
  double threshold=DEFAULTTHRESHOLD;
  char *names[100],**p;
  int i;

  cout.setf(ios::fixed, ios::floatfield);
  cout.setf(ios::showpoint);
  cerr.setf(ios::fixed, ios::floatfield);
  cerr.setf(ios::showpoint);

  cout << "******************** BAMBE Summarize Version " << VERSION 
       << " ********************" << endl << endl;
  cout << "Invocation: ";
  for(i=0;i<argc;i++)
    cout << argv[i] << " ";
  cout << endl << endl;
  if(argc > 1) {
    for(p=argv+1;p<argv+argc;p++) {
      if(strcmp(*p,"-n")==0) {
	if(++p>=argv+argc)
	  usageError(argv[0]);
	ISTRSTREAM f(*p);
	if(!(f >> n))
	  usageError(argv[0]);
      }
      else if(strcmp(*p,"-p")==0) {
	if(++p>=argv+argc)
	  usageError(argv[0]);
	ISTRSTREAM f(*p);
	if(!(f >> threshold))
	  usageError(argv[0]);
	if(threshold < 0.5 || threshold > 1.0) {
	  cerr << "Error: threshold value (" << setprecision(3) 
	    << threshold << ") must be between 0.5 and 1.0." << endl;
	  exit(1);
	}
      }
      else if(strcmp(*p,"-c")==0) {
	if(++p>=argv+argc)
	  usageError(argv[0]);
	ISTRSTREAM f(*p);
	if(!(f >> cladetops))
	  usageError(argv[0]);
	if(cladetops < 1 || cladetops > MAXCLADETOPS) {
	  cerr << "Error: Number of topologies (" << cladetops 
	       << ") of a named clade must be between 1 and " << MAXCLADETOPS
	       << "." << endl;
	  exit(1);
	}
      }
      else if(num_names<100)
	names[num_names++] = *p;
    }
    summarize(n,threshold,cladetops,names,num_names);
  }
  else 
    usageError(argv[0]);
  return 0;
}
