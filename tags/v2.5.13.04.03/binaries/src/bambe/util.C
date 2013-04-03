#include "util.h"
#include <ctype.h>
#include <math.h>
#include <float.h>

#ifdef WINDOWS
ErrorStream realError;
ErrorStream& error = realError;
void displayError(ErrorStream& c)
{
  c << ends;
  //AfxMessageBox((CString)(c.str()), MB_OK, 0);	
  cerr << "/*" << c.str() << "*/";	   // for Unix testing.
  c.seekp(ios::beg);
}
#else
ErrorStream &error = (ErrorStream&)cerr;
void displayError(ErrorStream& c) { endl(c); }
#endif

double fsign( double num, double sign );
char StringItr::null[] = "";

uint32 Rand::congrval;
uint32 Rand::tausval = 644672833;
const uint32 Rand::lambda = 69069;

double Rand::runif()
{
  /* Returns a pseudo-random number between 0 and 1.
   * runif is from the book Applied Statistics with S-PLUS by Ripley
   * (check the title/author) */

  congrval = congrval * lambda;
  tausval ^= tausval >> 15;
  tausval ^= tausval << 17;
  return( ((((tausval ^ congrval)>>1) & 017777777777U))/(double)020000000000U);
}

char* stripRightSpaces(char *s, char *end)
{
  /* Strips the right spaces from string s where end points to the 
     character after the string. */
  char *pos;

  for(pos=end-1;pos>=s&&isspace(*pos);pos--)
    ;
  *(pos+1) = '\0';
  return s;
}

int acceptable(double newLl, double oldLl, double hastingsRatio)
{
  return( (newLl - oldLl > log(Rand::runif() / hastingsRatio)) ? 1 : 0);
  //  return (SAFE_EXP(newLl - oldLl) * hastingsRatio > Rand::runif());
}

ostream& endError (ostream& o) { displayError((ErrorStream&) o); return o; }

void quit(int e)
{
#ifdef WINDOWS
  AfxEndThread(e);
#else
  exit(e);
#endif
}

int readNodeChar(istream& f)
{
  char d;

  while(isspace(d=f.get()))
    ;
  if(d == EOF) {
    error << "Error: Input file ends prematurely. Expected left parenthesis " 
	  << "or taxon name." << endError;
    quit(1);
  }
  return d;
}

int readSpecialChar(istream& f, char c, const char *name)
{
  char d;

  while(isspace(d=f.get()))
    ;
  if(d == EOF) {
    error << "Error: Input file ends prematurely. Expected " << name << "."
	  << endError;
    quit(1);
  }
  if(d != c) {
    error << "Error: Expected " << name << " in input file. Found " 
	  << d << "." << endError;
    quit(1);
  }
  return d;
}

double readBranchLength(istream& f)
{
  double b;

  if(!(f >> b)) {
    error << "Error: Missing branch Length in input tree." << endError;
    quit(1);
  }
  return b;
}


void *binsearch(const void *key, const void *base, size_t n, size_t
		size, int (*cmp)(const void *keyval, const void *datum))
{
  int low = 0, high = n-1;
  while(low <= high) {
    int midpos = (low+high)/2;
    void *mid = (void*)((char*)base + midpos*size);
    int c=cmp(key,mid);
    if(c==0)
      return mid;
    else if(c < 0)
      high = midpos-1;
    else
      low = midpos+1;
   }
  return NULL;
}

double Rand::snorm(void)
/*
**********************************************************************
                                                                      
                                                                      
     (STANDARD-)  N O R M A L  DISTRIBUTION                           
                                                                      
                                                                      
**********************************************************************
**********************************************************************
                                                                      
     FOR DETAILS SEE:                                                 
                                                                      
               AHRENS, J.H. AND DIETER, U.                            
               EXTENSIONS OF FORSYTHE'S METHOD FOR RANDOM             
               SAMPLING FROM THE NORMAL DISTRIBUTION.                 
               MATH. COMPUT., 27,124 (OCT. 1973), 927 - 937.          
                                                                      
     ALL STATEMENT NUMBERS CORRESPOND TO THE STEPS OF ALGORITHM 'FL'  
     (M=5) IN THE ABOVE PAPER     (SLIGHTLY MODIFIED IMPLEMENTATION)  
                                                                      
     Modified by Barry W. Brown, Feb 3, 1988 to use RANF instead of   
     SUNIF.  The argument IR thus goes away.                          
                                                                      
**********************************************************************
     THE DEFINITIONS OF THE CONSTANTS A(K), D(K), T(K) AND
     H(K) ARE ACCORDING TO THE ABOVEMENTIONED ARTICLE
*/
{
  static double a[32] = {
    0.0,3.917609E-2,7.841241E-2,0.11777,0.1573107,0.1970991,0.2372021,
    0.2776904,
    0.3186394,0.36013,0.4022501,0.4450965,0.4887764,0.5334097,0.5791322,
    0.626099,0.6744898,0.7245144,0.7764218,0.8305109,0.8871466,0.9467818,
    1.00999,1.077516,1.150349,1.229859,1.318011,1.417797,1.534121,1.67594,
    1.862732,2.153875
  };
  static double d[31] = {
    0.0,0.0,0.0,0.0,0.0,0.2636843,0.2425085,0.2255674,0.2116342,0.1999243,
    0.1899108,0.1812252,0.1736014,0.1668419,0.1607967,0.1553497,0.1504094,
    0.1459026,0.14177,0.1379632,0.1344418,0.1311722,0.128126,0.1252791,
    0.1226109,0.1201036,0.1177417,0.1155119,0.1134023,0.1114027,0.1095039
  };
  static double t[31] = {
    7.673828E-4,2.30687E-3,3.860618E-3,5.438454E-3,7.0507E-3,8.708396E-3,
    1.042357E-2,1.220953E-2,1.408125E-2,1.605579E-2,1.81529E-2,2.039573E-2,
    2.281177E-2,2.543407E-2,2.830296E-2,3.146822E-2,3.499233E-2,3.895483E-2,
    4.345878E-2,4.864035E-2,5.468334E-2,6.184222E-2,7.047983E-2,8.113195E-2,
    9.462444E-2,0.1123001,0.136498,0.1716886,0.2276241,0.330498,0.5847031
  };
  static double h[31] = {
    3.920617E-2,3.932705E-2,3.951E-2,3.975703E-2,4.007093E-2,4.045533E-2,
    4.091481E-2,4.145507E-2,4.208311E-2,4.280748E-2,4.363863E-2,4.458932E-2,
    4.567523E-2,4.691571E-2,4.833487E-2,4.996298E-2,5.183859E-2,5.401138E-2,
    5.654656E-2,5.95313E-2,6.308489E-2,6.737503E-2,7.264544E-2,7.926471E-2,
    8.781922E-2,9.930398E-2,0.11556,0.1404344,0.1836142,0.2790016,0.7010474
  };
  static int32 i;
  static double snorm,u,s,ustar,aa,w,y,tt;
  u = Rand::runif();
  s = 0.0;
  if(u > 0.5)
    s = 1.0;
  u += (u-s);
  u = 32.0*u;
  i = (int32) (u);
  if(i == 32) 
    i = 31;
  if(i != 0) {
    ustar = u-(double)i;
    aa = a[i-1];
    while(ustar <= t[i-1]) {
      u = Rand::runif();
      w = u*(a[i]-aa);
      tt = (0.5*w+aa)*w;
      if(ustar > tt)
	return (s == 1.0 ? -(aa+w) : aa+w);
      u = Rand::runif();
      while(ustar >= u) {
	tt = u;
	ustar = Rand::runif();
	if(ustar > tt)
	  return (s == 1.0 ? -(aa+w) : aa+w);
	u = Rand::runif();
      }
      ustar = Rand::runif();
    }
    w = (ustar-t[i-1])*h[i-1];
    return (s == 1.0 ? -(aa+w) : aa+w);
  }
  else {
    i = 6;
    aa = a[31];
    u += u;
    while(u < 1.0) {
      aa += d[i-1];
      i += 1;
      u += u;
    }
    u -= 1.0;
    for(;;) {
      w = u*d[i-1];
      tt = (0.5*w+aa)*w;
      ustar = Rand::runif();
      if(ustar > tt) 
	return (s == 1.0 ? -(aa+w) : aa+w);
      u = Rand::runif();
      while(ustar >= u) {
	tt = u;
	ustar = Rand::runif();
	if(ustar > tt) 
	  return (s == 1.0 ? -(aa+w) : aa+w);
	u = Rand::runif();
      }
      u = Rand::runif();
    }
  }
}

double Rand::sexpo(void)
/*
**********************************************************************
                                                                      
                                                                      
     (STANDARD-)  E X P O N E N T I A L   DISTRIBUTION                
                                                                      
                                                                      
**********************************************************************
**********************************************************************
                                                                      
     FOR DETAILS SEE:                                                 
                                                                      
               AHRENS, J.H. AND DIETER, U.                            
               COMPUTER METHODS FOR SAMPLING FROM THE                 
               EXPONENTIAL AND NORMAL DISTRIBUTIONS.                  
               COMM. ACM, 15,10 (OCT. 1972), 873 - 882.               
                                                                      
     ALL STATEMENT NUMBERS CORRESPOND TO THE STEPS OF ALGORITHM       
     'SA' IN THE ABOVE PAPER (SLIGHTLY MODIFIED IMPLEMENTATION)       
                                                                      
     Modified by Barry W. Brown, Feb 3, 1988 to use RANF instead of   
     SUNIF.  The argument IR thus goes away.                          
                                                                      
**********************************************************************
     Q(N) = SUM(ALOG(2.0)**K/K!)    K=1,..,N ,      THE HIGHEST N
     (HERE 8) IS DETERMINED BY Q(N)=1.0 WITHIN STANDARD PRECISION
*/
{
  static double q[8] = {
    0.6931472,0.9333737,0.9888778,0.9984959,0.9998293,0.9999833,0.9999986,
    1.0
  };
  int i;
  double a,u,ustar,umin;
  a = 0.0;
  for(u=2*Rand::runif();u<=1.0;u*=2)
    a += q[0];
  u -= 1.0;
  if(u <= q[0])
    return a+u;
  i = 0;
  umin = Rand::runif();
  do {
    ustar = Rand::runif();
    if(ustar < umin) umin = ustar;
  }
  while (u > q[++i]);
  return a+umin*q[0];
}

double fsign( double num, double sign )
/* Transfers sign of argument sign to argument num */
{
if ( ( sign>0.0f && num<0.0f ) || ( sign<0.0f && num>0.0f ) )
    return -num;
else return num;
}

double Rand::sgamma(double a)
/*
**********************************************************************
                                                                      
                                                                      
     (STANDARD-)  G A M M A  DISTRIBUTION                             
                                                                      
                                                                      
**********************************************************************
**********************************************************************
                                                                      
               PARAMETER  A >= 1.0  !                                 
                                                                      
**********************************************************************
                                                                      
     FOR DETAILS SEE:                                                 
                                                                      
               AHRENS, J.H. AND DIETER, U.                            
               GENERATING GAMMA VARIATES BY A                         
               MODIFIED REJECTION TECHNIQUE.                          
               COMM. ACM, 25,1 (JAN. 1982), 47 - 54.                  
                                                                      
     STEP NUMBERS CORRESPOND TO ALGORITHM 'GD' IN THE ABOVE PAPER     
                                 (STRAIGHTFORWARD IMPLEMENTATION)     
                                                                      
     Modified by Barry W. Brown, Feb 3, 1988 to use RANF instead of   
     SUNIF.  The argument IR thus goes away.                          
                                                                      
**********************************************************************
                                                                      
               PARAMETER  0.0 < A < 1.0  !                            
                                                                      
**********************************************************************
                                                                      
     FOR DETAILS SEE:                                                 
                                                                      
               AHRENS, J.H. AND DIETER, U.                            

               COMPUTER METHODS FOR SAMPLING FROM GAMMA,              
               BETA, POISSON AND BINOMIAL DISTRIBUTIONS.              
               COMPUTING, 12 (1974), 223 - 246.                       
                                                                      
     (ADAPTED IMPLEMENTATION OF ALGORITHM 'GS' IN THE ABOVE PAPER)    
                                                                      
**********************************************************************
     INPUT: A =PARAMETER (MEAN) OF THE STANDARD GAMMA DISTRIBUTION
     OUTPUT: SGAMMA = SAMPLE FROM THE GAMMA-(A)-DISTRIBUTION
     COEFFICIENTS Q(K) - FOR Q0 = SUM(Q(K)*A**(-K))
     COEFFICIENTS A(K) - FOR Q = Q0+(T*T/2)*SUM(A(K)*V**K)
     COEFFICIENTS E(K) - FOR EXP(Q)-1 = SUM(E(K)*Q**K)
     PREVIOUS A PRE-SET TO ZERO - AA IS A', AAA IS A"
     SQRT32 IS THE SQUAREROOT OF 32 = 5.656854249492380
*/
{

  const double q1 = 4.166669E-2;
  const double q2 = 2.083148E-2;
  const double q3 = 8.01191E-3;
  const double q4 = 1.44121E-3;
  const double q5 = -7.388E-5;
  const double q6 = 2.4511E-4;
  const double q7 = 2.424E-4;
  const double a1 = 0.3333333;
  const double a2 = -0.250003;
  const double a3 = 0.2000062;
  const double a4 = -0.1662921;
  const double a5 = 0.1423657;
  const double a6 = -0.1367177;
  const double a7 = 0.1233795;
  const double e1 = 1.0;
  const double e2 = 0.4999897;
  const double e3 = 0.166829;
  const double e4 = 4.07753E-2;
  const double e5 = 1.0293E-2;
  static double aa = 0.0;
  static double aaa = 0.0;
  const double sqrt32 = 5.656854;
  static double sgamma,s2,s,d,t,x,u,r,q0,b,si,c,v,q,e,w,p;

  if(a >= 1.0) {
    if(a != aa) {
      /*
	STEP  1:  RECALCULATIONS OF S2,S,D IF A HAS CHANGED
      */
      aa = a;
      s2 = a-0.5;
      s = sqrt(s2);
      d = sqrt32-12.0*s;
    }
    /*
      STEP  2:  T=STANDARD NORMAL DEVIATE,
      X=(S,1/2)-NORMAL DEVIATE.
      IMMEDIATE ACCEPTANCE (I)
    */
    t = snorm();
    x = s+0.5*t;
    sgamma = x*x;
    if(t >= 0.0) return sgamma;
    /*
      STEP  3:  U= 0,1 -UNIFORM SAMPLE. SQUEEZE ACCEPTANCE (S)
    */
    u = Rand::runif();
    if(d*u <= t*t*t) return sgamma;
    /*
      STEP  4:  RECALCULATIONS OF Q0,B,SI,C IF NECESSARY
    */
    if(a != aaa) {
      aaa = a;
      r = 1.0/ a;
      q0 = ((((((q7*r+q6)*r+q5)*r+q4)*r+q3)*r+q2)*r+q1)*r;
      /*
	APPROXIMATION DEPENDING ON SIZE OF PARAMETER A
	THE CONSTANTS IN THE EXPRESSIONS FOR B, SI AND
	C WERE ESTABLISHED BY NUMERICAL EXPERIMENTS
      */
      if(a <= 3.686) {
	/*
	  CASE 1:  A .LE. 3.686
	*/
	b = 0.463+s+0.178*s2;
	si = 1.235;
	c = 0.195/s-7.9E-2+1.6E-1*s;
      }
      else if(a <= 13.022) {
	/*
	  CASE 2:  3.686 .LT. A .LE. 13.022
	*/
	b = 1.654+7.6E-3*s2;
	si = 1.68/s+0.275;
	c = 6.2E-2/s+2.4E-2;
      }
      else {
	/*
	  CASE 3:  A .GT. 13.022
	*/
	b = 1.77;
	si = 0.75;
	c = 0.1515/s;
      }
    }
    /*
      STEP  5:  NO QUOTIENT TEST IF X NOT POSITIVE
    */
    if(x > 0.0) {
      /*
	STEP  6:  CALCULATION OF V AND QUOTIENT Q
      */
      v = t/(s+s);
      if(fabs(v) <= 0.25) 
	q = q0+0.5*t*t*((((((a7*v+a6)*v+a5)*v+a4)*v+a3)*v+a2)*v+a1)*v;
      else
	q = q0-s*t+0.25*t*t+(s2+s2)*log(1.0+v);

      /*
	STEP  7:  QUOTIENT ACCEPTANCE (Q)
      */
      if(log(1.0-u) <= q) return sgamma;
    }
    for(;;) {
      /*
	STEP  8:  E=STANDARD EXPONENTIAL DEVIATE
	U= 0,1 -UNIFORM DEVIATE
	T=(B,SI)-DOUBLE EXPONENTIAL (LAPLACE) SAMPLE
      */
      e = sexpo();
      u = Rand::runif();
      u += (u-1.0);
      t = b+fsign(si*e,u);
      /*
	STEP  9:  REJECTION IF T .LT. TAU(1) = -.71874483771719
      */
      if(t >= -.71874483771719)
	/*
	  STEP 10:  CALCULATION OF V AND QUOTIENT Q
	*/
	{
	  v = t/(s+s);
	if(fabs(v) <= 0.25) 
	  q = q0+0.5*t*t*((((((a7*v+a6)*v+a5)*v+a4)*v+a3)*v+a2)*v+a1)*v;
	else 
	  q = q0-s*t+0.25*t*t+(s2+s2)*log(1.0+v);
	/*
	  STEP 11:  HAT ACCEPTANCE (H) (IF Q NOT POSITIVE GO TO STEP 8)
	*/
	if(q > 0.0) {
	  if(q <= 0.5) 
	    w = ((((e5*q+e4)*q+e3)*q+e2)*q+e1)*q;
	  else 
	    w = SAFE_EXP(q)-1.0;

	  /*
	    IF T IS REJECTED, SAMPLE AGAIN AT STEP 8
	  */
	  if(c*fabs(u) <= w*SAFE_EXP(e-0.5*t*t)) {
	    x = s+0.5*t;
	    sgamma = x*x;
	    return sgamma;
	  }
	}
      }
    }
  }
  else {
    /*
      ALTERNATE METHOD FOR PARAMETERS A BELOW 1  (.3678794=EXP(-1.))
    */
    aa = 0.0;
    b = 1.0+0.3678794*a;
    for(;;) {
      p = b*Rand::runif();
      if(p >= 1.0) {
	sgamma = -log((b-p)/ a);
	if(sexpo() >= (1.0-a)*log(sgamma)) 
	  return sgamma;
      }
      else {
	sgamma = SAFE_EXP(log(p)/ a);
	if(sexpo() >= sgamma) 
	  return sgamma;
      }
    }
  }
}

void Rand::rdirich(const Vector<double> alpha, Vector<double>& y,int k)
/*
 * alpha is an array of k positive numbers.
 * return y distributed according to the Dirichlet(alpha) distribution
 */
{
  double sum=0.0;
  int i;
  
  for(i=0;i<k;i++)
  {
    y[i] = rgamma(alpha[i],1.0);
    sum += y[i];
  }
  for(i=0;i<k;i++)
    y[i] /= sum;
}

#ifndef HASLGAMMA
double Rand::lgamma(double alpha)
{
/*
    function to return natural log of gamma function

    From StatLib, Applied Statistics Algorithm 66

    Translated from Fortran to C by Bret Larget, June 2, 1998
*/
  double a[9] = {0.9999999999995183,
                 676.5203681218835,
                 -1259.139216722289,
                 771.3234287757674,
                 -176.6150291498386,
                 12.50734324009056,
                 -0.1385710331296526,
                 0.9934937113930748e-05,
                 0.1659470187408462e-06};

  const double LNSQRT2PI=0.9189385332046727;

  if(alpha <= 0.0)
    alpha = DBL_EPSILON;
  double ans = 0.0;
  double tmp = alpha + 7.0;
  for(int j=8;j>0;j--)
  {
    ans += a[j]/tmp;
    tmp -= 1.0;
  }
  ans += a[0];
  ans = log(ans) + LNSQRT2PI - (alpha+6.5) + (alpha-0.5)*log(alpha+6.5);
  return ans;
}
  
#endif 

