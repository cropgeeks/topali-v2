#ifndef SITEHDR
#define SITEHDR

#include "util.h"
#include <iostream>
#include <iomanip>

/*  A base is represented by a bit vector of size 4. If the base is
 *  known only one bit is set. If the base may be one of several 
 *  (uncertain) then each corresponding bit is set. A gap is
 *  indicated by having all four bits cleared.
 *
 *  A is coded as 0001, G is coded as 0010, C is coded as 0100, 
 *  and T is coded as 1000.
 *
 *  A site is a sequuence of bases. A bit vector comprised of an array
 *  of integers represents sites. The bit vectors representing the bases
 *  are packed into the integers. The sign bit of each integer is unused
 *  so that the comparison function used by quicksort can subtract
 *  two integers. On our systems, an integer is 32 bits, so 31 bits are used.
 *  The non-sign bits of the integers in the array are used consecutively,
 *  so that a base may be split between two integers.
 * 
 *  Sites are stored from left to right in the order the taxa are read in.
 *
 *  Important note: size must be set before creating any sites!
 *  size = (numTaxa*NUM_BASES+(WORDSIZE-2))/(WORDSIZE-1)];
 */

class Site {
 public:
  Site(int numTaxa) { size = (numTaxa*NUM_BASES+(WORDSIZE-2))/(WORDSIZE-1); }
  Site() : word(size) {} // size must be set before creating a Site!
  Site(const Site& s2) : word(s2.size) {
    for(int i=0;i<size;i++)
      word[i] = s2.word[i];
  }
  int& operator[] (int i) { return word[i]; }
  void zero() {
    /* Zero out site. */
    for(int i=0;i<size;i++)
      word[i] = 0;
  }
  Site& operator= (const Site& s2) {
    /* Copies s2. */
    for(int i=0;i<size;i++)
      word[i]=s2.word[i];
    return *this;
  }
  friend int operator== (const Site& s1, const Site& s2) {
    /* Returns 1 if sites s1 and s2 are equal. Returns 0 otherwise. */
    for(int i=0;i<size;i++)
      if(s1.word[i]!=s2.word[i])
	return 0;
    return 1;
  }
  friend int operator!= (const Site& s1, const Site& s2) {
    /* Returns 1 if sites s1 and s2 are not equal. Returns 0 otherwise. */
    for(int i=0;i<size;i++)
      if(s1.word[i]!=s2.word[i])
	return 1;
    return 0;
  }
  friend void siteAnd(Site& s, const Site& s1, const Site& s2) {
    /* s is the bitwise conjunction of s1 and s2. */
    for(int i=0;i<size;i++)
      s.word[i] = (s1.word[i] & s2.word[i]);
  }
  friend void siteOr(Site &s, const Site& s1, const Site& s2) {
    /* s is the bitwise disjunction of s1 and s2. */
    for(int i=0;i<size;i++)
      s.word[i] = (s1.word[i] | s2.word[i]);
  }
  friend int siteSubset(const Site& s1, const Site& s2) {
    /* s is the bitwise disjunction of s1 and s2. */
    for(int i=0;i<size;i++)
      if(s1.word[i] & ~s2.word[i])
	return 0;
    return 1;
  }
  void printHex(ostream &c) const {
    c << hex << setw(8) << setfill('0') << word[0];
    for(int i=1;i<size;i++)
      c << ' ' << hex << setw(8) << setfill('0') << word[i];
    c << dec;
  }
  void print(ostream &c, int numTaxa) const {
    static const char baseSymbol[NUM_SYMBOLS] 
      = {'\0', 'A', 'G', 'R', 'C', 'M', 'S', 'V', 'T', 'W', 'K', 'D', 'Y',
	 'H', 'B', '-'};    
    for(int i=0;i<numTaxa;i++)
      c << baseSymbol[retrieveBase(i)];
  }
  friend int siteMember(const Site& s,  const Site* l, int n) {
    /* Returns an index into array of sites l (of size n) whose element is
       equal to s if one exists. Returns n otherwise. */
    int i = 0;
    while(i<n && s!=l[i])
      i++;
    return i;
  }
  friend int cmpSites(const Site& n1, const Site& n2) {
    /* Returns a negative number if n1 precedes n2, 0 if the same, and
       a positive number if n1 comes after n2. */
    int c;
    for(int i=0;i<size;i++)
      if((c = n1.word[i] - n2.word[i]) != 0)
	return c;
    return 0;
  }
  void addBase(int n, int b);
  int retrieveBase(int) const;
 private:
  static int size;
  Vector<int> word;
};

/* Sites are sorted by the lexicographical ordering of (bit vector of bases,
   index into the global site array). NumberedSite is used for the sort. */

class NumberedSite  {
 public:
  Site site;		// the site.
  int index;		// index of the site in the site array.
  int n;		// index of the site in site list of the node.

  void print(ostream& c) const { 
    c << index << ' ' << n << ' ';
    this->site.printHex(c);
    c << endl;
  }

  friend int cmpNumberedSites(const void* n1, const void* n2) {
    /* Returns a negative number if n1 precedes n2, 0 if the same, and
       a positive number if n1 comes after n2. */
    int c = cmpSites(((const NumberedSite*)n1)->site,
		     ((const NumberedSite*)n2)->site);
    if(c != 0)
      return c;
    else
      return ((const NumberedSite*)n1)->index-((const NumberedSite*)n2)->index;
  }
};

#endif
