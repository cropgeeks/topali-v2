#ifndef BASEDATAHDR
#define BASEDATAHDR

#include "util.h"
#include "runsettings.h"

class BaseData {
public:
  BaseData(const RunSettings& rs);
  BaseData(char *filename);
  int getNumTaxa () const { return numTaxa; }
  int getNumSites () const { return numSites; }
  const char *getTaxaName (int i) const { return taxaName[i]; }
  int getBase (int i,int j) const { return base[i][j]; }
  
private:
  int numTaxa;		  	  // Number of taxa.
  int numSites;		  	  // Number of sites in original data.
  Matrix<char> taxaName;	  // taxaName[numTaxa][MAX_LINE]: names of the
  				  // taxa.
  Matrix<int> base;		  // base[numTaxa][numSites]:
  				  // base[n][i] is the symbol of taxon n at
  				  // site i of the original data file.

  void readClustalFile(istream&, const char[]);
  void readBambeFile(istream&, const char[]);
  void checkOutgroup(const RunSettings&);
  void getNewickSizeInt(istream&, int&, StringList&);
};

#endif
