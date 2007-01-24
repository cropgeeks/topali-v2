#include "basedata.h"
#include "runsettings.h"

#include <iostream>
#include <ctype.h>
#include <stdlib.h>

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

#include <fstream>

int clustalBlankLine(char *line);
void parseClustalLine(char buf[], const char filename[], int lineno, 
		      char*& name, char*& rest, int& len);
void checkLineLength(const char filename[], int lineno, int& first, 
		     int& firstLength, int length, char *rest);
void readClustalFileInt(istream& f, const char filename[], StringList& taxa,
			LongStringList& sites);
int convChar(char c, int n, int x);
int getNextLine(istream& f, const char filename[], char buf[], int r, 
		int& lineno);
int bambeBlankLine(char buf[]);
/*
 *  int convChar(char c) 
 *
 *  A is coded as 0001, G is coded as 0010, C is coded as 0100, 
 *  and T(U) is coded as 1000
 */

int convChar(char c, int n, int x)
{
  /* Returns the bit vector associated with the symbol. */

  switch (c) {
  case 'A' : case 'a' : 			return 0x1;
  case 'G' : case 'g' : 			return 0x2;
  case 'C' : case 'c' : 			return 0x4;
  case 'U' : case 'u' : case 'T' : case 't' :   return 0x8;
  case 'M' : case 'm' : 			return 0x1 | 0x4;
  case 'R' : case 'r' : 			return 0x1 | 0x2;
  case 'W' : case 'w' : 			return 0x1 | 0x8;
  case 'S' : case 's' : 			return 0x2 | 0x4;
  case 'Y' : case 'y' : 			return 0x4 | 0x8;
  case 'K' : case 'k' : 			return 0x2 | 0x8;
  case 'V' : case 'v' : 			return 0x1 | 0x2 | 0x4;
  case 'H' : case 'h' : 			return 0x1 | 0x4 | 0x8;
  case 'D' : case 'd' : 			return 0x1 | 0x2 | 0x8;
  case 'B' : case 'b' : 			return 0x2 | 0x4 | 0x8;
  case 'X' : case 'x' : case 'N' : case 'n' :   return 0x1 | 0x2 | 0x8 | 0x4;
  case '~' : case '-' : case '?' :		return 0x1 | 0x2 | 0x8 | 0x4;
  case '.' : 
    if(n==0) {
      error << "Error: Base '.' appears in the first taxon." << endError;
      quit(1);
    }
    return x;
  default:
    error << "Warning: " << c << " unknown base. - used." << endError;
    return GAP;
  }
}

int getNextLine(istream& f, const char filename[], char buf[], int r, 
		int& lineno)
{ 
  /* r is the expected number of additional lines of input.
     r = -1 means that no data has been read yet. */

  char c;

  if(f.get(buf,MAX_LINE,'\n')) {
    SKIPLINE(f,c);
    lineno++;
    return 1;
  }
  if(r == 0) 
    return 0;
  if(r == -1)
    error << "Error: Data file " << filename << " is empty." << endError;
  else 
    error << "Error: Data file " << filename << " ends prematurely." 
	  << endError;
  quit(1);
  return 0; // meaningless, but Visual C++ wants it.
}


int clustalBlankLine(char *line)
{
  /* Returns 1 if line is a blank line in clustal format, 0 otherwise. */
  if (*line == '!')
    return 1;
  for(;*line;line++)
    if(!isdigit(*line) && !isspace(*line) && *line != '*' && *line != ':' 
       && *line != '.')
      return 0;
  return 1;
}

void parseClustalLine(char buf[], const char filename[], int lineno, 
		      char*& name, char*& rest, int& len)
  /* buf contains the line of the file to be parsed.
     filename is the name of the file (necessary for error messages).
     lineno is the number of the line read (ditto).
     name is set to the name in the line.
     rest is set to the site data on the line. 
     len is the length of rest. */
{
  for(name=buf;isspace(*name);name++)			// skip blanks.
    ;
  if(*name) {
    for(rest=name+1;*rest && !isspace(*rest);rest++)	// skip over nonblank.
      ;
    if(*rest) {
      *rest = '\0';
      for(rest++;isspace(*rest);rest++)			// skip blanks.
	;
      if(*rest) {
	char *end;
	for(end=rest+1;*end && !isspace(*end);end++)
	  ;
	*end = '\0';
	len = end-rest;
	if(len>0)
	  return;
      }
    }
  }
  error << "Error: Line (" << lineno << ") is not in clustal format in "
	<< "data file " << filename << "." << endError;
  quit(1);
}  

void checkLineLength(const char filename[], int lineno, int& first, 
		     int& firstLength, int length, char *rest)
{
  /* If first line, sets firstLength to length (and clears first).
     Otherwise compares the length of the site info to the length
     of the site info of the first line of the section (firstLength). */

  if(first) {
    firstLength = length;
    first = 0;
  }
  else if(length < firstLength) {
    error << "Error: Line " << lineno << " in data file " << filename << 
      " ends prematurely." << endError;
    quit(1);
  }
  if(length > firstLength) {
    rest[firstLength] = '\0';
    error << "Warning: Line " << lineno << " in data file " << filename << 
      " contains extra information. Ignored." << endError;
  }
}

void readClustalFileInt(istream& f, const char filename[], StringList& taxa,
			LongStringList& sites)
  /* Reads in the clustal format file f called filename.
     Returns a linked list of taxa and a linked list of sites. */
{
  char buf[MAX_LINE], *name, *rest,c;
  int lineno, length, firstLength, first;

  lineno = 1; // Header line read by BaseData(). 
  // skip blank lines 
  while(getNextLine(f,filename,buf,-1,lineno) && clustalBlankLine(buf))
    ;
  // read first section of data
  first = 1;
  do {
    parseClustalLine(buf,filename,lineno,name,rest,length);
    checkLineLength(filename,lineno,first,firstLength,length,rest);
    taxa.append(name);
    sites.add(rest);
    if(!f.get(buf,MAX_LINE,'\n'))
      return;
    SKIPLINE(f,c);
    lineno++;
  }
  while(*buf && !clustalBlankLine(buf));

  while(1) {				// read other sections
    do {				// skip blank lines 
      if(!f.get(buf,MAX_LINE,'\n'))
	return;
      SKIPLINE(f,c);
      lineno++;
    }
    while(clustalBlankLine(buf));
    first = 1;
    // read other sections.
    ListItr<char *> p(taxa);
    ListItr<StringList*> q(sites);
    for(int n=taxa.getSize()-1;p.more();n--) {
      parseClustalLine(buf,filename,lineno,name,rest,length);
      checkLineLength(filename,lineno,first,firstLength,length,rest);
      if(strcmp(p.next(),name)!=0) {
	error << "Error: Taxon names differ in file " << filename << 
	  " at line " << lineno << "." << endError;
	quit(1);
      }
      q.next()->append(rest);
      if(!getNextLine(f,filename,buf,n,lineno))
	return;
    }
    if(*buf && !clustalBlankLine(buf)) {
      error << "Error: Section of data file " << filename << " is not " <<
	"terminated by blank line or end of file. At line " << lineno << "."
	   << endError;
      quit(1);
    }
  }
}

void BaseData::readClustalFile(istream& f, const char filename[])
{
  StringList taxa;
  LongStringList sites;

  readClustalFileInt(f,filename,taxa,sites);
  numTaxa = taxa.getSize();
  ListItr<StringList*> b(sites);  
  numSites = (b.more() ? b.next()->getLength() : 0);
  taxaName.setSize(numTaxa,MAX_LINE);
  ListItr<char *> p(taxa);
  for(int i=0;p.more();i++)
    strcpy(taxaName[i],p.next());
  base.setSize(numTaxa,numSites);
  ListItr<StringList*> q(sites);
  for(int n=0;q.more();n++) {
    StringItr r(*(q.next()));
    for(int s=0;r.more();s++)
      base[n][s] = convChar(r.next(),n,base[0][s]);
  }
}

int bambeBlankLine(char buf[])
{
  for(char *p=buf;*p;p++)
    if(!isspace(*p))
      return 0;
  return 1;
}

void BaseData::readBambeFile(istream& f, const char *filename)
{
  /* Reads in the bambe file f and stores the site information
     in base[n][k] and the taxa names in taxaName[n]. */

  int c,len;
  char buf[MAX_LINE],*comment;

  taxaName.setSize(numTaxa,MAX_LINE);
  base.setSize(numTaxa,numSites);
  for(int n=0;n<numTaxa;n++)
  {
    // Read the taxon name
    do {
      if(!f.get(buf,MAX_LINE,'\n')) {
	error << "Error: Data file " << filename << " ends prematurely." 
	      << endError;
	quit(1);
      }
      SKIPLINE(f,c);
    }
    while(bambeBlankLine(buf)) ;
    len = strlen(buf);
    if((comment=strchr(buf,'#'))!=0)
      stripRightSpaces(buf,comment);
    else
      stripRightSpaces(buf,buf+len);
    strcpy(taxaName[n],buf);
    for(int s=0;s<numSites;s++) {
      while((c=f.get())!=0 && isspace(c))
	;
      if(f.eof()) {
	error <<"Error: Premature end of data file " << filename << "." 
	      << endError;
	quit(1);
      }
      base[n][s] = convChar(c,n,base[0][s]);
    }
    SKIPLINE(f,c);
  }
}

void BaseData::checkOutgroup(const RunSettings& rs)
{
  /* check that the outgroup isn't too big. */
  if(rs.getOutGroup() > numTaxa)
    if(rs.getMclock())
      error << "Warning: outgroup (" << rs.getOutGroup() << ") (unused in this"
	    << " model) exceeds number of taxa (" << numTaxa << ")." 
	    << endError;
    else {
      error << "Error: outgroup (" << rs.getOutGroup() << ") exceeds number"
	    << " of taxa (" << numTaxa << ")." << endError;
      quit(1);
    }
}

BaseData::BaseData(const RunSettings& rs)
{
  /* Reads in either a BAMBE or clustal datafile and
     stores the taxanames in taxaName, and the base information in
     base. */

  char c;
  ifstream f(rs.getDataFile());
  if(f.fail()) {
    error <<"Error: Cannot open data file " << rs.getDataFile() << "." 
	  << endError;
    quit(1);
  }
  char buf[MAX_LINE];
  if(!f.get(buf,MAX_LINE,'\n')) {
    error << "Error: Data file " << rs.getDataFile() << " is empty."
	  << endError;
    quit(1);
  }
  SKIPLINE(f,c);
  ISTRSTREAM iss(buf);
  if((iss >> numTaxa >> numSites) != (void*)NULL)
    readBambeFile(f,rs.getDataFile());
  else if(strstr(buf,"CLUSTAL"))
    readClustalFile(f,rs.getDataFile());
  else {
    error << "Error: Data file " << rs.getDataFile()
	  << " is not in a readable format." << endError;
    quit(1);
  }
  f.close();

  if(numTaxa < 4) {
    error << "Error: Data file must contain at least four taxa." << endError;
    quit(1);
  }
  checkOutgroup(rs);			  // outgroup should be <= numTaxa.
}


void BaseData::getNewickSizeInt(istream& f, int& n, StringList& taxa)
{
  /* Reads in the taxa names from a newick tree file (or more than 1
     taxon) and stores them in taxa. n is set to the number
     of taxa.  The opening and closing parentheses are read by the
     calling function. */

  int c = readNodeChar(f);
  if(c == '(') {
    getNewickSizeInt(f,n,taxa);
    readSpecialChar(f,',',"comma");
    getNewickSizeInt(f,n,taxa);
    readSpecialChar(f,')',"right parenthesis");
    readSpecialChar(f,':', "colon");
    readBranchLength(f);
  }
  else {
    f.putback(c);
    char buf[MAX_LINE];
    if(!f.get(buf,MAX_LINE,':')) {
      error << "Error: Cannot read taxon name in newick file." << endError;
      quit(1);
    }
    taxa.append(buf);
    while((c=f.get())!=0 && (c!=':'))
      ;
    readBranchLength(f);
    n++;
  }
}

BaseData::BaseData(char *filename) 
{ 
  /* Reads in the taxa names from a newick tree file and stores them in
     taxaName. n is set to the number of taxa. */

  StringList taxa;
  ifstream f(filename);
  if(!f) {
    error << "Error: Cannot open newick tree file " << filename << "."
	  << endError;
    quit(1);
  }
  numTaxa = 0;
  int c = readNodeChar(f);
  if(c == '(') {
    getNewickSizeInt(f,numTaxa,taxa);
    readSpecialChar(f,',',"comma");
    getNewickSizeInt(f,numTaxa,taxa);
    readSpecialChar(f,')',"right parenthesis");
  }
  else {
    f.putback(c);
    char buf[MAX_LINE];
    f.get(buf,MAX_LINE,'\n');
    taxa.append(buf);
    SKIPLINE(f,c);
    numTaxa++;
  }
  readSpecialChar(f,';',"semicolon");
  f.close();
  taxaName.setSize(numTaxa,MAX_LINE);
  ListItr<char *> p(taxa);
  for(int i=0;p.more();i++)
    strcpy(taxaName[i],p.next());
  numSites = 0;
}

