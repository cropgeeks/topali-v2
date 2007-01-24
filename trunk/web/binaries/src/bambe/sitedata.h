#ifndef SITEDATAHDR
#define SITEDATAHDR

#include "util.h"
#include "basedata.h"
#include "site.h"

class NumberedName {
public:
  int n;
  char name[MAX_LINE];
  friend int cmpName(const void* n1, const void* n2) {
    /* Returns a negative number if n1 precedes n2, 0 if the same, and
       a positive number if n1 comes after n2. */
    return strcmp(((NumberedName *)n1)->name,((NumberedName *)n2)->name);
  }
};

class SiteData {
 public:
  SiteData(const RunSettings&, const BaseData&);
  SiteData(const BaseData&);
  int getNumTaxa () const { return numTaxa; }
  int getNumSites () const { return numSites; }
  int getNumTags () const { return numTags; }
  const char * const getTaxaName (int i) const { return taxaName[i]; }
  const Matrix<char>& getNames() const { return taxaName; }
  const Vector<NumberedName>& getOrderedName() const { return orderedName; }
  int getTagSites(int i) const { return tagSites[i]; }
  const int * const getTagSymbols(int i) const { return tagSymbols[i]; }
  double getTagWeight(int i) const { return tagWeights[i]; }
  const Vector<double> getTagWeights() const { return tagWeights; }
  const Site& getTaxonMask(int i) const  { return taxonMask[i]; }
  const int * const getRootConstSite(int i) const { return rootConstSites[i]; }
  const Vector<int> getFirstSite() const { return firstSite; }
  int getFirstSite(int i) const { return firstSite[i]; }
  int getSiteCount(int i) const { return siteCount[i]; }
  int getNumUniqueSites() const { return numUniqueSites; }
  int getNumVariableSites(int i) const { return numVariableSites[i]; }
  const Site& getConstantMask(int i) const { return constantMask[i]; }
  const int * const getBase(int i) const { return base[i]; }
  const BaseArray& getOneFrag() const { return oneFrag; }
  friend int singleBase(int sym,int&);
  const Site& getSite(int i) const { return sites[i]; }
  const Vector<int> getNumVariableSites() const { return numVariableSites; }
  const Site& getUniqSite(int i) const { return uniqSites[i]; }
  int getNumUncertain(int i) const { return numUncertain[i]; }
  const int * const getUncertainBase(int i) const { return uncertainBase[i]; }
  const int * const getUncertainTag(int i) const { return uncertainTag[i]; }
  const PosArray * const getUncertainPos(int i) const { return uncertainPos[i]; }
  void printInfo(ostream&,const RunSettings&) const;
  void printMoreInfo(ostream&) const;
  const static char baseSymbol[NUM_SYMBOLS];

private:
  int numTaxa;		  	  // Number of taxa. (copied from basedata)

  int numSites;		  	  // Number of sites in original data. 
  				  // (copied from basedata)

  int numTags;			  // Number of tags. (copied from basedata)

  Matrix<char> taxaName;	  // taxaName[numTaxa][MAX_LINE]: names of the
  				  // taxa. (copied from basedata)

  Matrix<int> base;		  // base[numTaxa][numSites]:
  				  // base[n][i] is the symbol of taxon n at
  				  // site i where the sites are ordered by
  				  // their tags.

  Matrix<int> tagSymbols;	  // tagSymbols[numTags][NUM_SYMBOLS]:
  				  // tagSymbols[tg][s] is the number of times
	      			  // symbol s occurs with tag tg.

  Vector<int> tagSites;		  // tagSites[numTags]: tagSites[tg] is the
  				  // number of sites with tag tg.

  Vector<double> tagWeights;      // tagWeights[numTags]: tagWeights[tg] is
				  // the percentage of sites with tag tg.

  Matrix<int> taxaSymbols;	  // taxaSymbols[numTaxa][NUM_SYMBOLS]:
  				  // taxaSymbols[n][s] is the number of times
				  // symbol s occurs in taxon n.

  int totalSymbols[NUM_SYMBOLS];  // totalSymbols[s] is the number of times
  				  // symbol s occurs.

  Vector<int> firstSite;	  // firstSite[numTags+1]:  firstSite[tg] is
  				  // the index of the first site in array sites
  				  // with tag tg. firstSite[numTags] is the
  				  // number of sites in the array.

  Vector<NumberedName> 		  // orderedName[numTaxa]: the taxanames in
    orderedName;		  // alphabetic order.

  static const BaseArray oneFrag; // frag for gaps

/*  When first read in, all sites are stored from the input data. The
 *  array is then compacted so that each unique site is listed once
 *  and a count of the the number of times that site occurs in the
 *  data is kept. In both cases, the array is ordered so that all
 *  sites with tag 0 occur first, then all sites with tag 1, etc. */

  Vector<Site> sites;  		  // sites[numSites]: The original site
				  // listed in tag order.

  Vector<int> numVariableSites;	  // numVariableSites[numTags]: 
                                  // numVariableSites[tg] is the
  				  // number of variable sites with tag tg.

  int numUniqueSites;		  // Number of unique sites.

  Vector<Site> uniqSites;	  // uniqSites[numUniqueSites]: The unique
   				  // sites listed in tag order.  

  Vector<int> siteCount;	  // sitecount[numUniqueSites]: sitecount[i] is
   				  // the number of times site i occurs.

  Matrix<int> rootConstSites;     // rootConstSites[numTags][NUM_BASES]:
  				  // rootConstSites[tg][b] is the number of
  				  // constant sites of the root of base b
  				  // with tag tg.

  Site constantMask[NUM_BASES];   // constant_mask[b] has the bits
  				  // corresponding to base b set for every
  				  // taxa.

  Vector<Site> taxonMask;	  // taxonMask[numTaxa]: taxonMask[n] has the
  				  // bits set that correspond to taxon n.

  Vector<int> numUncertain;	  // numUncertain[numTaxa]: number of
				  // uncertain bases of a taxon.

  Matrix<int> uncertainBase;      // uncertainBase[numTaxa][NUM_SYMBOLS]:
  				  // Uncertain bases of a taxon are uniquely 
  				  // stored consecutively in a list.
  				  // uncertainBase[n][i] is the ith uncertain
  				  // base of the taxon n.

  Matrix<int> uncertainTag;	  // uncertainTag[numTaxa][NUM_SYMBOLS]:
  				  // uncertainTag[n][i] is the tag of the ith
  				  // uncertain base of the taxon n.

  Matrix<PosArray> uncertainPos;  // uncertainPos[numTaxa][numTags]
  				  // [NUM_SYMBOLS]:
  				  // uncertainPos[n][tg][sym] is the position
  				  // of the (tag,sym) pair in list of uncertain
  				  // bases of taxon n.

  void processBases(const RunSettings&, const BaseData&);
  int sortNames(const BaseData&);
  void createMasks();
  void findUniqueSites(const RunSettings&, const BaseData&);
  void parseCatstring(const char*&, Vector<int>&, int&, int&, int[]);
  void parseCategories(const RunSettings&, const BaseData&, Vector<int>&);
};

#endif

