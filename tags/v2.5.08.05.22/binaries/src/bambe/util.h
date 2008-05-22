#ifndef UTILHDR
#define UTILHDR

using namespace std;

/*#include <math.h>*/
#include <iostream>
#include <iomanip>
#include <string>

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

#include <math.h>
/*#include <stdlib.h>*/

typedef int int32;
typedef unsigned int uint32;

#define WORDSIZE 	32
#define TWO             2
#define NUM_BASES	4
#define NUM_RVALUES     6
#define NUM_SYMBOLS     16
#define MAX_CATEGORIES	10
#define MAX_LINE 	513
#define GAP		0xf
#define MINIMUM(a,b)	((a)<(b)?(a):(b))
#define MAXIMUM(a,b)	((a)>(b)?(a):(b))
#define SKIPLINE(f,c)   {while((c=(f).get())!=EOF && ((c)!='\n')){}}

inline int lessThan(int a, int b) { return a < b; }

template <class C, int s>
class FixedVectorPtr;

template <class C, int s>
class FixedVector {
 friend class FixedVectorPtr<C,s>;
 public:
  FixedVector() {}
  FixedVector(const FixedVector& old) {
    for(int i=0;i<s;i++) 
      v[i] = old.v[i];
  }
  FixedVector(const C& val) {
    for(int i=0;i<s;i++) 
      v[i] = val;
  }
  FixedVector& operator= (const FixedVector& old) {
    for(int i=0;i<s;i++) 
      v[i] = old.v[i];
    return *this;
  }
  C& operator[] (int i) { return v[i]; }
  const C& operator[] (int i) const { return v[i]; }
 private:
  C v[s];
};

template <class C, int s>
class FixedVectorPtr {
 public:
  FixedVectorPtr() {}
  FixedVectorPtr(const FixedVector<C,s>& f) : p(f.v) {}
  FixedVectorPtr& operator= (const FixedVectorPtr& old) { 
    p = old.p;
    return *this;
  }
  const C& operator[] (int i) const { return p[i]; }
private:
  const C* p;
};
  
typedef FixedVector<double,NUM_BASES> BaseArray;
//typedef FixedVector<double,NUM_SYMBOLS> SymbolArray;
typedef FixedVector<BaseArray,NUM_BASES> BaseMatrix;
//typedef FixedVector<BaseArray,NUM_SYMBOLS> SymbolMatrix;
typedef FixedVector<int,NUM_SYMBOLS> PosArray;

#ifdef WINDOWS
typedef ostrstream ErrorStream;
#else
typedef ostream ErrorStream;
#endif

extern ErrorStream& error;

void displayError(ErrorStream& c);

ostream& endError (ostream&);

void quit(int);

void *binsearch(const void *key, const void *base, size_t n, size_t
		size, int (*cmp)(const void *keyval, const void *datum));

template<class C>
class Vector {
 public:
  Vector() { v = NULL; size = 0;}
  Vector(int s) { setSize(s); }
  Vector(const Vector& old) { 
    setSize(old.size); 
    for(int i=0;i<old.size;i++)
      v[i] = old.v[i];
  }
  ~Vector() { delete [] v; }
  Vector& operator=(const Vector& old) {
    // old.v and v should be the same size.
    for(int i=0;i<size;i++)
      v[i] = old.v[i];
    return *this;
  }    
  void setSize(int s) {
    // v should be NULL.
    size = s;
    if(!(v = new C[s])) {
      error << "Error: Out of space in Vector()." << endError;
      quit(1);
    }
  }
  friend void vcopy(Vector& v1, const Vector& v2, int n) {
    for(int i=0;i<n;i++)
      v1.v[i] = v2.v[i];
  }
  int getSize() const { return size; }
  C& operator[] (int i) { return v[i]; }
  const C& operator[] (int i) const { return v[i]; }
  void sort(size_t n, int (*cmp)(const void *, const void *)) {
    qsort((void *)v,n,sizeof(C),cmp);
  }
  void sort(size_t first, size_t last, 
	    int (*cmp)(const void *, const void *)) {
    qsort((void *)(v+first),last-first+1,sizeof(C),cmp);
  }
  C* search(const void *  key, size_t n, 
	    int (*cmp)(const void *, const void *)) const
    { return (C*)(binsearch(key,(void *)v,n,sizeof(C),cmp)); }
 protected:
  C* v;
  int size;
};

class IdentityVector : public Vector<int> {
public:
  IdentityVector(int s) : Vector<int>(s) { for(int i=0;i<s;i++) v[i] = i; }
};

template<class C>
class Matrix {
 public:  
  Matrix() { m = NULL; s = NULL; }
  Matrix(int r, int c) { setSize(r,c); }
  Matrix(const Matrix& old) {
    setSize(old.rows,old.columns);
    for(int r=0;r<old.rows;r++)
      for(int c=0;c<old.columns;c++)
	m[r][c] = old.m[r][c];
  }
  void setSize(int r, int c) {
    // m should originally be NULL.
    rows = r;
    columns = c;
    if(!(m = new C*[r]) || !(s = m[0] = new C[r*c])) {
      error << "Error: Out of space in Matrix()." << endError;
      quit(1);
    }
    for(int i=0;i<r-1;i++)
      m[i+1] = m[i] + c;
  }
  ~Matrix() { 
    if(s) delete [] s;
    if(m) delete [] m;
  }
  C*& operator[] (int i) { return m[i]; }
  const C * const operator[] (int i) const { return m[i]; }
private:
  C **m;	// The array of rows.
  C *s;		// The data area. (The user might change m[0], i.e., sort.)
  int rows, columns;
};

template <class C> class ListItr;

template <class C>
  class ListEl {
  public:
    C val;
    ListEl* next;
    ListEl(const C& s, ListEl* n) : val(s) { next = n;}
  };

template <class C>
class List {
  // Linked list of C.
  friend class ListItr<C>;
public:
  List() { head = tail = NULL; size = 0;}
  ~List() { 
    if(head) {
      ListEl<C> *p=head;
      for(ListEl<C> *q=p->next;q;p=q,q=p->next)
	delete p;
      delete p;
    }
  }
  void add(const C& s) { 
    size++;
    if(head)
      tail = tail->next = new ListEl<C>(s,NULL);
    else
      head = tail = new ListEl<C>(s,NULL);
  }
  int getSize() const { return size; }
private:
  ListEl<C> *head, *tail;
  int size;
};

template <class C>
class ListItr {
  // List iterator.
public:
  ListItr(List<C>& list) { current = list.head; }
  int more() { return current!=NULL; }
  C& next() { C& v = current->val; current = current->next; return v; }
private:
  ListEl<C> * current;
};

class StringList : public List<char*> {
  //*** Linked list of strings (char *). append(s) attaches a copy of s.
public:
  StringList() { length = 0; }
  StringList(const char *s) { length = 0; append(s); }
  void append(const char *s) { 
    int len = strlen(s);
    length += len;
    char *p= new char[len+1];
    strcpy(p,s); 
    add(p); 
  }
  ~StringList() { for(ListItr<char *> p(*this);p.more();delete p.next())
  	{} }
  int getLength() { return length; }
  void print(ostream& c) { 
    for(ListItr<char *> p(*this);p.more(); c << p.next())
      {} 
  }
private:
  int length;
};

class StringItr {
  // StringList Iterator: moves through each character of each element.
public:
  StringItr(StringList& list) : it(list) { current = null; }
  int more() { 
    while(!*current && it.more())
      current = it.next();
    return *current;
  }
  char next() { return *(current++); }
private:
  static char null[];
  ListItr<char*> it;
  char *current;
};

class LongStringList : public List<StringList*> {
  // List of linked list of strings.
public:
  void add(const char* s) { List<StringList*>::add(new StringList(s)); }
  ~LongStringList() { 
    for(ListItr<StringList*> p(*this);p.more();delete p.next())
      {}
  }
};

char *stripRightSpaces(char *, char *);

#ifdef ALPHA
#define SAFE_EXP(x) ((x)<-200.0 ? 0.0 : exp(x))
#else
#define SAFE_EXP(x) (exp(x))
#endif

class AcceptCounts {
 public:
  AcceptCounts() { accept = accepts = kaccept = kaccepts = 0;
                   paccept = paccepts = 0; }
  int accept, accepts, kaccept, kaccepts, paccept, paccepts;
};

int acceptable(double,double,double);

class Rand {
 public:
  Rand(unsigned int seed) {congrval = seed;}    
  static double runif();
  static void rdirich(const Vector<double>,Vector<double>&,int);
  static double rgamma(double alpha,double lambda1) { 
    return sgamma(alpha)/lambda1; }
  static double rbeta(double alpha, double beta) {
    double x = rgamma(alpha,1);
    double y = rgamma(beta,1);
    return x/(x+y);
  }
#ifdef HASLGAMMA
  static double lgamma(double a) { return ::lgamma(a); }
#else
  static double lgamma(double);
#endif

 private:
  static uint32 congrval;
  static uint32 tausval;
  static const uint32 lambda;

  static double snorm(void);
  static double sexpo(void);
  static double sgamma(double);
};

int readNodeChar(istream&);
int readSpecialChar(istream&,char,const char*);
double readBranchLength(istream&);

#endif



