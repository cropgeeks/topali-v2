/* function to find the mean and standard deviation
   for each column of numbers, separated by white space
   in a single pass through a file
   using the method of provisional means

   assumes equal number of numbers per line */

using namespace std;

#include <iostream>
#include <iomanip>
#include <math.h>
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

#include <string>

#define MAXLINE 256
#define MAXCOL 100

int main(void)
{

  char buf[MAXLINE+1],c;
  double x, newmean[MAXCOL], oldmean[MAXCOL], sumsq[MAXCOL];
  int numColumn=0;

  /* read in first line */
  if(cin.get(buf,MAXLINE+1,'\n')) {
    while((c=cin.get())!=EOF && c!='\n')
      ;
    numColumn=0;
    ISTRSTREAM f(buf);
    while(f >> x) {
      newmean[numColumn] = oldmean[numColumn] = x;
      sumsq[numColumn++] = 0.;
    }
  }
  else {
    cerr << "Error: Cannot read first line of input" << endl;
    exit(1);
  }
  int n=1;
  
  /* read in rest of file */
  while(cin.get(buf,MAXLINE+1,'\n')) {
    while((c=cin.get())!=EOF && c!='\n')
      ;
    n++;
    int i=0;
    ISTRSTREAM f(buf);
    while(f >> x) {
      newmean[i] = oldmean[i] + (x - oldmean[i]) / (double) n;
      sumsq[i] += (x - oldmean[i]) * (x - newmean[i]);
      oldmean[i] = newmean[i];
      i++;
    }
    if(i != numColumn) {
      cerr << "Error: Line " << n << " has " << i
	   << " columns. It should have " << numColumn << "." << endl;
      exit(1);
    }
  }

  cout.setf(ios::fixed, ios::floatfield);
  cout.setf(ios::showpoint);
  for(int i=0;i<numColumn;i++)
    cout << setprecision(6) << newmean[i] << " +/- " 
	 << sqrt(sumsq[i]/(double)(n-1)) << endl;
  return 0;
}
