#include "sitedata.h"
#include "runsettings.h"
#include "basedata.h"
#include "site.h"
#include <ctype.h>
#include <stdlib.h>
#include <fstream>

const BaseArray SiteData::oneFrag(1.0);

const char SiteData::baseSymbol[NUM_SYMBOLS] 
  = {'\0', 'A', 'G', 'R', 'C', 'M', 'S', 'V', 'T', 'W', 'K', 'D', 'Y',
     'H', 'B', '-'};

int singleBase(int sym, int& b)
{
  /* If sym corresponds to a single base, returns 1 and sets b to the base.
     Returns 0 otherwise. */

  switch (sym) {
  case 0x1: b = 0; return 1;
  case 0x2: b = 1; return 1;
  case 0x4: b = 2; return 1;
  case 0x8: b = 3; return 1;
  default: return 0;
  }
}

void SiteData::parseCatstring(const char*& str, Vector<int>& tagList, 
			      int& lenTl, int& lastGroup, int internalCat[]) 
{
  /* Parses a string representing a list of categories up to, but
     not including the final asterisk (if it exists).
     str is the unscanned part of the category list string.
     tagList is the corresponding array of tags.
     lenTl is the size of tagList.
     lastGroup is the starting position of the last group in tagList.
     internalCat maps the category names to the tag names. */

  while(isspace(*str))
    str++;
  if(*str == '(') {
    str++;
    lastGroup = lenTl;
    int x;
    parseCatstring(str,tagList,lenTl,x,internalCat);
    while(isspace(*str))
      str++;
    str++;
  }
  else if(isdigit(*str)) {
    int n = *str - '0';
    for(str++;isdigit(*str);str++)
      n = 10 * n + *str - '0';
    lastGroup = lenTl;
    if(lenTl >= numSites) {
      error<< "Warning: There are more categories than sites. The remainder "
	   << "will be ignored." << endError;
      return;
    }
    tagList[lenTl++] = internalCat[n];
  }
  else 
    return;
  while(isspace(*str))
    str++;
  if(*str == '^') {
    str++;
    while(isspace(*str))
      str++;
    int n = *str - '0';
    for(str++;isdigit(*str);str++)
      n = 10 * n + *str - '0';
    int size = lenTl - lastGroup;
    int newLen = lenTl + size * (n-1);
    if(newLen > numSites) {
      error << "Warning: There are more categories than sites. The "
	    << "remainder will be ignored." << endError;
      for(int i=lenTl,j=lastGroup;i<numSites;i++,j++)
	tagList[i] = tagList[j];
      lenTl = numSites;
      return;
    }
    for(int i=lenTl,j=lastGroup;i<newLen;i++,j++)
      tagList[i] = tagList[j];
    lenTl = newLen;
    lastGroup = newLen - size;
    while(isspace(*str))
      str++;
  }
  if(*str == ',') {
    str++;
    parseCatstring(str,tagList,lenTl,lastGroup,internalCat);
  }
}

void SiteData::parseCategories(const RunSettings& rs, const BaseData&,
			       Vector<int>& tagList)
{
  /* tagList[i] is the tag of category i. */

  int internalCat[MAX_CATEGORIES];
  for(int tag=0;tag<numTags;tag++)
    internalCat[rs.getCategories(tag)] = tag;
  int lenTl = 0, lastGroup;
  const char *catList = rs.getCategoryList();
  parseCatstring(catList,tagList,lenTl,lastGroup,internalCat);
  if(lenTl < numSites)
    if(*catList != '*') {
      error << "Error: There are fewer categories than sites." << endError;
      quit(1);
    }
    else
      for(int i=lenTl,j=lastGroup;i<numSites;i++,j++)
	tagList[i] = tagList[j];
}

void SiteData::processBases(const RunSettings& rs,const BaseData& bd) 
{
  /* calculate tagSites, firstSite, tagWeights, sites, totalSymbols, 
     tagSymbols, taxaSymbols, numUncertain, uncertainBase, and uncertainTag */

  Vector<int> tagList(numSites);          // tagList[i] is the tag of
  parseCategories(rs,bd,tagList);	  // category i.

  Vector<int> count(numTags);	          // Running count of number of sites
  					  // with a given tag.

  // Calculate tagSites and initialize count.
  for(int tag1=0;tag1<numTags;tag1++)
    tagSites[tag1] = count[tag1] = 0;
  for(int site1=0;site1<numSites;site1++)
    tagSites[tagList[site1]]++;

  // Calculate firstSite and tagWeights.
  firstSite[0] = 0;
  for(int tag2=0;tag2<numTags;tag2++) {
    firstSite[tag2+1] = firstSite[tag2] + tagSites[tag2];
    tagWeights[tag2] = (double)tagSites[tag2] / numSites;
  }

  Vector<int> sitepos(numSites);         // sitepos[i] is the position of
  					 // site i after reordering by tags.

  // Calculate sitepos. Initialize sites, totalSymbols, tagSymbols, and
  // taxaSymbols.
  for(int site2=0;site2<numSites;site2++) {
    int tag = tagList[site2];
    sitepos[site2] = firstSite[tag] + count[tag]++;
    sites[site2].zero();
  }
  for(int sym=1;sym<NUM_SYMBOLS;sym++) {
    totalSymbols[sym] = 0;
    for(int tag=0;tag<numTags;tag++)
      tagSymbols[tag][sym] = 0;
    for(int n=0;n<numTaxa;n++)
      taxaSymbols[n][sym] = 0;
  }

  // Store base and site information. Calculate totalSymbols, tagSymbols,
  // and taxaSymbols.
  for(int n=0;n<numTaxa;n++)
  {
    for(int site=0;site<numSites;site++) {
      int tag = tagList[site];
      int j = sitepos[site];
      int b = bd.getBase(n,site);
      base[n][j] = b; 
      sites[j].addBase(n,b);
      totalSymbols[b]++;
      tagSymbols[tag][b]++;
      taxaSymbols[n][b]++;
    }

    // Calculate numUncertain, uncertainBase, and uncertainTag.
    int baseSeen[NUM_SYMBOLS];
    numUncertain[n] = 0;
    int x;
    for(int tag=0;tag<numTags;tag++) {
      for(int sym=1;sym<NUM_SYMBOLS;sym++)
	baseSeen[sym] = 0;
      for(int i=firstSite[tag];i<firstSite[tag+1];i++) {
	int b = base[n][i];
	if(b!=GAP && !singleBase(b,x) && !baseSeen[b]) {
	  baseSeen[b] = 1;
	  uncertainPos[n][tag][b] = numUncertain[n];
	  uncertainBase[n][numUncertain[n]] = b;
	  uncertainTag[n][numUncertain[n]++] = tag;
	}
      }
    }
  }
}  

int SiteData::sortNames(const BaseData& bd)
{
  // Sort the taxa names and store in OrderedName.
  for(int i1=0;i1<numTaxa;i1++) {
    strcpy(taxaName[i1],bd.getTaxaName(i1));
    strcpy(orderedName[i1].name,bd.getTaxaName(i1));
    orderedName[i1].n = i1;
  }
  orderedName.sort(numTaxa,cmpName);
  int bad = 0;
  for(int i2=0;i2<numTaxa-1;i2++)
    if(strcmp(orderedName[i2].name,orderedName[i2+1].name)==0) {
      error << "Warning: Taxa have identical names (" 
	    << orderedName[i2].name << ")" << endError;
      bad = 1;
    }
  return !bad;
}

void SiteData::createMasks()
{
  // Create constantMask and taxonMask.
  for(int b=0;b<NUM_BASES;b++) {
    constantMask[b].zero();
    for(int n=0;n<numTaxa;n++)
      constantMask[b].addBase(n,(0x1 << b));
  }
  for(int n=0;n<numTaxa;n++) {
    taxonMask[n].zero();
    taxonMask[n].addBase(n,GAP);
  }
}

void SiteData::findUniqueSites(const RunSettings&, const BaseData&)
{
  // calculate uniqSites and siteCount.
  Vector<NumberedSite> sl(numSites); // used to sort sites.
  Vector<int> orgSite(numSites);     // orgSite[i] is the original
  				     // index of site i after reordering.

  int numNonconst = 0;
  Vector<int> firstNonConst(numTags+1);
  firstNonConst[0] = 0;
  firstSite[0] = 0;
  for(int tag=0,first=firstSite[0],last=firstSite[1];
      tag<numTags;
      tag++,first=last,last=firstSite[tag+1]) {
    for(int b=0;b<NUM_BASES;b++)
      rootConstSites[tag][b] = 0;
    for(int i=first,j;i<last;i++)
      if((j=siteMember(sites[i],constantMask,NUM_BASES)) < NUM_BASES)
	rootConstSites[tag][j]++;
      else {
	sl[numNonconst].site = sites[i];
	sl[numNonconst++].index = i;
      }
    firstNonConst[tag+1] = numNonconst;
    numVariableSites[tag] = numNonconst - firstNonConst[tag];
    firstSite[tag+1] = firstSite[tag];
    if(firstNonConst[tag+1] > firstNonConst[tag]) {
      sl.sort(firstNonConst[tag],firstNonConst[tag+1]-1,cmpNumberedSites);
      firstSite[tag+1]++;
      for(int i1=firstNonConst[tag]+1;i1<firstNonConst[tag+1];i1++)
	if (sl[i1].site != sl[i1-1].site)
	  firstSite[tag+1]++;
    }
  }
  numUniqueSites = firstSite[numTags];
  uniqSites.setSize(numUniqueSites);
  siteCount.setSize(numUniqueSites);

  for(int tag2=0;tag2<numTags;tag2++) {
    if(firstNonConst[tag2+1] > firstNonConst[tag2]) {
      int count = firstSite[tag2];
      uniqSites[count] = sl[firstNonConst[tag2]].site;
      orgSite[count] = sl[firstNonConst[tag2]].index;
      siteCount[count++] = 1;
      for(int i=firstNonConst[tag2]+1;i<firstNonConst[tag2+1];i++)
	if (sl[i].site == sl[i-1].site)
	  siteCount[count-1]++;
	else {
	  uniqSites[count] = sl[i].site;
	  orgSite[count] = sl[i].index;
	  siteCount[count++] = 1;
	}
    }
  }

  // Reorder base into unique site ordering.
  Vector<int> base2(numSites);

  for(int i=0;i<numTaxa;i++) {
    for(int j1=0;j1<numSites;j1++)
      base2[j1] = base[i][j1];
    for(int j2=0;j2<numUniqueSites;j2++)
      base[i][j2] = base2[orgSite[j2]];
  }
}

SiteData::SiteData(const RunSettings& rs, const BaseData& bd):
  /* Processes the bases in the global array base[n][k] and stores the
     sites (ordered by tags) in the site array. Also counts the number 
     of symbols and sets up the list of uncertain bases for the taxa. 
     Assumes that bd.base[n][k] is in the order read in. */
  taxaName(bd.getNumTaxa(),MAX_LINE),
  base(bd.getNumTaxa(),bd.getNumSites()),
  tagSymbols(rs.getNumTags(),NUM_SYMBOLS),
  tagSites(rs.getNumTags()),
  tagWeights(rs.getNumTags()),
  taxaSymbols(bd.getNumTaxa(),NUM_SYMBOLS),
  firstSite(rs.getNumTags()+1),
  orderedName(bd.getNumTaxa()),
  sites(bd.getNumSites()),
  numVariableSites(rs.getNumTags()),
  // uniqSites and siteCount will be sized later.
  uniqSites(),
  siteCount(),
  rootConstSites(rs.getNumTags(),NUM_BASES),
  taxonMask(bd.getNumTaxa()),
  numUncertain(bd.getNumTaxa()),
  uncertainBase(bd.getNumTaxa(),NUM_SYMBOLS),
  uncertainTag(bd.getNumTaxa(),NUM_SYMBOLS),
  uncertainPos(bd.getNumTaxa(),rs.getNumTags())
{
  numTaxa = bd.getNumTaxa();
  numSites = bd.getNumSites();
  numTags = rs.getNumTags();
  processBases(rs,bd);
  if(!sortNames(bd) && rs.getInitialTreeType()==RSTreeType::NEWICK) {
    error << "Error: Cannot read in newick tree file." << endError;
    quit(1);
  }
  createMasks();
  findUniqueSites(rs,bd);
}

void SiteData::printInfo(ostream& f, const RunSettings& rs) const
{
  f << setw(5) << numTaxa << "\t\tnumber of taxa" << endl;
  f << setw(5) << numSites << "\t\ttotal number of sites" << endl;
  f << setw(5) << numUniqueSites << "\t\ttotal number of unique sites" << endl
    << endl;
  int d = numTaxa * numSites;
  f << "Number of constant sites" << endl << endl;
  f << "Category";
  int columnTotal[NUM_BASES], total = 0;
  for(int b1=0;b1<NUM_BASES;b1++) {
    f << "   " << baseSymbol[0x1<<b1] << "   ";
    columnTotal[b1] = 0;
  }
  f << "Total" << endl;
  for(int i=0;i<numTags;i++) {
    int rowTotal = 0;
    f << setw(3) << rs.getCategories(i) << "     ";
    for(int b=0;b<NUM_BASES;b++) {
      f << setw(4) << rootConstSites[i][b] << "   ";
      rowTotal += rootConstSites[i][b];
      columnTotal[b] += rootConstSites[i][b];
    }
    total += rowTotal;
    f << setw(4) << rowTotal << endl;
  }
  f << "Total   ";
  for(int b2=0;b2<NUM_BASES;b2++) 
    f << setw(4) << columnTotal[b2] << "   ";
  f << setw(4) << total << endl << endl;

  f << "Number of variable sites" << endl << endl;
  f << "Category  Total" << endl;
  for(int i=0;i<numTags;i++) {
    f << setw(3) << rs.getCategories(i) << "       ";
    f << setw(4) << numVariableSites[i] << endl;
  }
  f << endl;

  f << "Number of unique sites" << endl << endl;
  f << "Category  Total    Site" << endl;
  int tag = 0;
  for (int i=0; i < numUniqueSites; i++) {
    if (i == firstSite[tag]) 
      tag++;
    f << setw(4) << tag << "      " << setw(4) << siteCount[i] << "     ";
    uniqSites[i].print(f, numTaxa) ;
    f << endl;
  }
  f << endl;

  f << "Overall   Count   Proportion" << endl << endl;
  for(int b3=0;b3<NUM_BASES;b3++)
    f << setiosflags(ios::showpoint | ios::fixed) << "   " 
      << baseSymbol[0x1<<b3] << "      " << setw(5) << totalSymbols[0x1<<b3] 
      << "      " << setw(6) << setprecision(4) 
      << (double)totalSymbols[0x1<<b3]/(double)(d) << endl;
  f << setiosflags(ios::showpoint | ios::fixed) << "   " << baseSymbol[GAP]
    << "      " << setw(5) << totalSymbols[GAP] 
    << "      " << setw(6) << setprecision(4) 
    << (double)totalSymbols[GAP]/(double)(d) << endl;
  int x;
  for(int sym=1;sym<NUM_SYMBOLS;sym++)
    if(sym!=GAP && !singleBase(sym,x))
      f << setiosflags(ios::showpoint | ios::fixed) << "   " 
	<< baseSymbol[sym] << "      " << setw(5) 
	<< totalSymbols[sym] << "      " << setw(6) << setprecision(4) 
	<< (double)totalSymbols[sym]/(double)(d) << endl;
  f << endl;
}

void SiteData::printMoreInfo(ostream& f) const
{
  f << "Taxon        ";
  for(int b=0;b<NUM_BASES;b++)
    f << baseSymbol[0x1<<b] << "       ";
  f << baseSymbol[GAP] << "      other" << endl;
  for(int i1=0;i1<numTaxa;i1++)
  {
    f << setw(3) << i1+1 << "      ";
    double sum = (double)taxaSymbols[i1][GAP]/(double)numSites;
    double gapprop = sum;
    for(int b=0;b<NUM_BASES;b++) {
      double prop = (double)taxaSymbols[i1][0x1<<b]/(double)numSites;
      sum += prop;
      f << setiosflags(ios::showpoint | ios::fixed) << "  " << setw(6) 
	<< setprecision(4) << prop;
    }
    f << setiosflags(ios::showpoint | ios::fixed) << "  " << setw(6) 
      << setprecision(4) << gapprop << "  " << setw(6) << 1-sum << endl;
  }
  f << endl << endl << "Taxa names:" << endl;
  for(int i2=0;i2<numTaxa;i2++)
    f << setw(3) << i2+1 << "  " << taxaName[i2] << endl;
  f << endl << endl;
}

SiteData::SiteData(const BaseData& bd) :
  numTaxa(bd.getNumTaxa()),
  numSites(0),
  taxaName(bd.getNumTaxa(),MAX_LINE),
  orderedName(bd.getNumTaxa()),
  taxonMask(bd.getNumTaxa()),
  numUncertain(bd.getNumTaxa()),
  numTags(0),
  numUniqueSites(0)
{
  for(int i=0;i<numTaxa;i++)
    numUncertain[i] = 0;
  sortNames(bd);
}
  
