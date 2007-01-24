#include "site.h"

int Site::size;

void Site::addBase(int n, int b) {
  /* Adds base b of taxon n to a site vector s. */
  /* Assumes that word size is divisible by NUM_BASES. */

  int p = NUM_BASES*(n/(WORDSIZE-1));	   // Number of the group (of 
					   // NUM_BASES words) base goes into.
  int r = n%(WORDSIZE-1);    		   // Number of the half-byte in that
  					   // group [0,WORDSIZE-2].
  int d = (r+1)/(WORDSIZE/NUM_BASES);      // Number of the word of the group
  					   // (0-3).
  int e = (r+1)%(WORDSIZE/NUM_BASES);      // 0, if base spans two words.

  if(e==0) { 
    word[p+d-1] |= (b >> d);
    word[p+d]   |= ((b & (GAP >> (NUM_BASES - d))) << (WORDSIZE - 1 - d));
  }
  else
    word[p+d]   |= (b << 
		    (WORDSIZE/NUM_BASES*(d+1)-2-r)*NUM_BASES+(NUM_BASES-1-d)); 
}

int Site::retrieveBase(int n) const {
  /* Adds base b of taxon n to a site vector s. */
  /* Assumes that word size is divisible by NUM_BASES. */

  int p = NUM_BASES*(n/(WORDSIZE-1));	   // Number of the group (of 
					   // NUM_BASES words) base goes into.
  int r = n%(WORDSIZE-1);    		   // Number of the half-byte in that
  					   // group [0,WORDSIZE-2].
  int d = (r+1)/(WORDSIZE/NUM_BASES);      // Number of the word of the group
  					   // (0-3).
  int e = (r+1)%(WORDSIZE/NUM_BASES);      // 0, if base spans two words.

  if(e==0) { 
    return (((word[p+d-1] & (GAP >> d)) << d) |
	    ((word[p+d] >> (WORDSIZE - 1 - d)) & (GAP >> (NUM_BASES-d))));
  }
  else
    return 
      ((word[p+d] 
	>> ((WORDSIZE/NUM_BASES*(d+1)-2-r)*NUM_BASES+(NUM_BASES-1-d))) 
       & GAP);
}

