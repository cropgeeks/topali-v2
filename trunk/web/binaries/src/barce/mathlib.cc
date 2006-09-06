/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

#include <iostream>
#include <stdlib.h> /* For RAND_MAX */
#include "mathlib.h"

using namespace std;


/*  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    double beta(double a, double b);
 *
 *  DESCRIPTION
 *
 *    This function returns the value of the beta function
 *    evaluated with arguments a and b.
 *
 *  NOTES
 *
 *    This routine is a translation into C of a Fortran subroutine
 *    by W. Fullerton of Los Alamos Scientific Laboratory.
 *    Some modifications have been made so that the routines
 *    conform to the IEEE 754 standard.
 */

double beta(double a, double b)
{
    static double xmax = 0;
    static double alnsml = 0;
    double val, xmin;

    if (xmax == 0) {
	    gammalims(&xmin, &xmax);
	    alnsml = log(d1mach(1));
    }

#ifdef IEEE_754
    /* NaNs propagated correctly */
    if(ISNAN(a) || ISNAN(b)) return a + b;
#endif

    if (a < 0 || b < 0) {
	ML_ERROR(ME_DOMAIN);
	return ML_NAN;
    }
    else if (a == 0 || b == 0) {
	return ML_POSINF;
    }
#ifdef IEEE_754
    else if (!FINITE(a) || !FINITE(b)) {
	return 0;
    }
#endif

    if (a + b < xmax)
	return gammafn(a) * gammafn(b) / gammafn(a+b);

    val = lbeta(a, b);
    if (val < alnsml) {
	/* a and/or b so big that beta underflows */
	ML_ERROR(ME_UNDERFLOW);
	return ML_UNDERFLOW;
    }
    return exp(val);
}
/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    int chebyshev_init(double *dos, int nos, double eta)
 *    double chebyshev_eval(double x, double *a, int n)
 *
 *  DESCRIPTION
 *
 *    "chebyshev_init" determines the number of terms for the
 *    double precision orthogonal series "dos" needed to insure
 *    the error is no larger than "eta".  Ordinarily eta will be
 *    chosen to be one-tenth machine precision.
 *
 *    "chebyshev_eval" evaluates the n-term Chebyshev series
 *    "a" at "x".
 *
 *  NOTES
 *
 *    These routines are translations into C of Fortran routines
 *    by W. Fullerton of Los Alamos Scientific Laboratory.
 *
 *    Based on the Fortran routine dcsevl by W. Fullerton.
 *    Adapted from R. Broucke, Algorithm 446, CACM., 16, 254 (1973).
 */

/* NaNs propagated correctly */


int chebyshev_init(double *dos, int nos, double eta)
{
    int i, ii;
    double err;

    if (nos < 1)
	return 0;

    err = 0.0;
    i = 0;			/* just to avoid compiler warnings */
    for (ii=1; ii<=nos; ii++) {
	i = nos - ii;
	err += fabs(dos[i]);
	if (err > eta) {
	    return i;
	}
    }
    return i;
}


double chebyshev_eval(double x, double *a, int n)
{
    double b0, b1, b2, twox;
    int i;

    if (n < 1 || n > 1000) {
	ML_ERROR(ME_DOMAIN);
	return ML_NAN;
    }

    if (x < -1.1 || x > 1.1) {
	ML_ERROR(ME_DOMAIN);
	return ML_NAN;
    }

    twox = x * 2;
    b2 = b1 = 0;
    b0 = 0;
    for (i = 1; i <= n; i++) {
	b2 = b1;
	b1 = b0;
	b0 = twox * b1 - b2 + a[n - i];
    }
    return (b0 - b2) * 0.5;
}
/*
 *  Mathlib - A Mathematical Function Library
 *  Copyright (C) 1998  Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/* NaNs propagated correctly */

double d1mach(int i)
{
    switch(i) {
    case 1: return DBL_MIN;
    case 2: return DBL_MAX;

    case 3: return pow((double)i1mach(10), -(double)i1mach(14));
    case 4: return pow((double)i1mach(10), 1-(double)i1mach(14));

    case 5: return log10(2.0);

    default: return 0.0;
    }
}

double d1mach_(int *i)
{
	return d1mach(*i);
}
/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    double dbeta(double x, double a, double b);
 *
 *  DESCRIPTION
 *
 *    The density of the beta distribution.
 */


double dbeta(double x, double a, double b)
{
    double y;
#ifdef IEEE_754
    /* NaNs propagated correctly */
    if (ISNAN(x) || ISNAN(a) || ISNAN(b)) return x + a + b;
#endif
    if (a <= 0.0 || b <= 0.0) {
	ML_ERROR(ME_DOMAIN);
	return ML_NAN;
    }
    if (x < 0)
	return 0;
    if (x > 1)
	return 0;
    y = beta(a, b);
    a = pow(x, a - 1);
    b = pow(1.0 - x, b - 1.0);
    //#ifndef IEEE_754
    //   if(errno) return ML_NAN;
    //#endif
    return a * b / y;
}
/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    double dexp(double x, double scale)
 *
 *  DESCRIPTION
 *
 *    The density of the exponential distribution.
 *
 */


double dexp(double x, double scale)
{
#ifdef IEEE_754
    /* NaNs propagated correctly */
    if (ISNAN(x) || ISNAN(scale)) return x + scale;
#endif
    if (scale <= 0.0) {
	ML_ERROR(ME_DOMAIN);
	return ML_NAN;
    }
    if (x < 0.0)
	return 0.0;
    return exp(-x / scale) / scale;
}
/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA.
 *
 *  SYNOPSIS

 *
 *    #include "Mathlib.h"
 *    double rexp(double scale)
 *
 *  DESCRIPTION
 *
 *    Random variates from the exponential distribution.
 *
 */


double rexp(double scale)
{
    if (
#ifdef IEEE_754
        !R_FINITE(scale) ||
#endif
        scale <= 0.0) {
        ML_ERROR(ME_DOMAIN);
        return ML_NAN;
    }
    return scale * sexp();
}


/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    double dgamma(double x, double shape, double scale);
 *
 *  DESCRIPTION
 *
 *    Computes the density of the gamma distribution.
 */


double dgamma(double x, double shape, double scale)
{
#ifdef IEEE_754
    if (ISNAN(x) || ISNAN(shape) || ISNAN(scale))
	return x + shape + scale;
#endif
    if (shape <= 0 || scale <= 0) {
	ML_ERROR(ME_DOMAIN);
	return ML_NAN;
    }
    if (x < 0)
	return 0;
    if (x == 0) {
	if (shape < 1) {
	    ML_ERROR(ME_RANGE);
	    return ML_POSINF;
	}
	if (shape > 1) {
	    return 0;
	}
	return 1 / scale;
    }
    x = x / scale;
    return exp((shape - 1) * log(x) - lgammafn(shape) - x) / scale;
}
/*
 *  R : A Computer Langage for Statistical Data Analysis
 *  Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */


double fmax2(double x, double y)
{
#ifdef IEEE_754
	if (ISNAN(x) || ISNAN(y))
		return x + y;
#endif
	return (x < y) ? y : x;
}
/*
 *  R : A Computer Langage for Statistical Data Analysis
 *  Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */


double fmin2(double x, double y)
{
#ifdef IEEE_754
	if (ISNAN(x) || ISNAN(y))
		return x + y;
#endif
	return (x < y) ? x : y;
}
/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    double gammafn(double x);
 *
 *  DESCRIPTION
 *
 *    This function computes the value of the gamma function.
 *
 *  NOTES
 *
 *    This function is a translation into C of a Fortran subroutine
 *    by W. Fullerton of Los Alamos Scientific Laboratory.
 *
 *    The accuracy of this routine compares (very) favourably
 *    with those of the Sun Microsystems portable mathematical
 *    library.
 */


double gammafn(double x)
{
    static double gamcs[42] = {
	+.8571195590989331421920062399942e-2,
	+.4415381324841006757191315771652e-2,
	+.5685043681599363378632664588789e-1,
	-.4219835396418560501012500186624e-2,
	+.1326808181212460220584006796352e-2,
	-.1893024529798880432523947023886e-3,
	+.3606925327441245256578082217225e-4,
	-.6056761904460864218485548290365e-5,
	+.1055829546302283344731823509093e-5,
	-.1811967365542384048291855891166e-6,
	+.3117724964715322277790254593169e-7,
	-.5354219639019687140874081024347e-8,
	+.9193275519859588946887786825940e-9,
	-.1577941280288339761767423273953e-9,
	+.2707980622934954543266540433089e-10,
	-.4646818653825730144081661058933e-11,
	+.7973350192007419656460767175359e-12,
	-.1368078209830916025799499172309e-12,
	+.2347319486563800657233471771688e-13,
	-.4027432614949066932766570534699e-14,
	+.6910051747372100912138336975257e-15,
	-.1185584500221992907052387126192e-15,
	+.2034148542496373955201026051932e-16,
	-.3490054341717405849274012949108e-17,
	+.5987993856485305567135051066026e-18,
	-.1027378057872228074490069778431e-18,
	+.1762702816060529824942759660748e-19,
	-.3024320653735306260958772112042e-20,
	+.5188914660218397839717833550506e-21,
	-.8902770842456576692449251601066e-22,
	+.1527474068493342602274596891306e-22,
	-.2620731256187362900257328332799e-23,
	+.4496464047830538670331046570666e-24,
	-.7714712731336877911703901525333e-25,
	+.1323635453126044036486572714666e-25,
	-.2270999412942928816702313813333e-26,
	+.3896418998003991449320816639999e-27,
	-.6685198115125953327792127999999e-28,
	+.1146998663140024384347613866666e-28,
	-.1967938586345134677295103999999e-29,
	+.3376448816585338090334890666666e-30,
	-.5793070335782135784625493333333e-31
    };

    static int ngam = 0;
    static double xmin = 0.;
    static double xmax = 0.;
    static double xsml = 0.;
    static double dxrel = 0.;

    int i, n;
    double y;
    double sinpiy, value;

    if (ngam == 0) {
	ngam = chebyshev_init(gamcs, 42, 0.1 * d1mach(3));
	gammalims(&xmin, &xmax);
	xsml = exp(fmax2(log(d1mach(1)), -log(d1mach(2)))+0.01);
	dxrel = sqrt(d1mach(4));
    }

#ifdef IEEE_754
    if(ISNAN(x)) return x;
#endif

    y = fabs(x);

    if (y <= 10) {

	/* Compute gamma(x) for -10 <= x <= 10. */
	/* Reduce the interval and find gamma(1 + y) for */
	/* 0 <= y < 1 first of all. */

	n = (int) x;
	if(x < 0) --n;
	y = x - n;/* n = floor(x)  ==>	y in [ 0, 1 ) */
	--n;
	value = chebyshev_eval(y * 2 - 1, gamcs, ngam) + .9375;
	if (n == 0)
	    return value;/* x = 1.dddd = 1+y */

	if (n < 0) {
	    /* compute gamma(x) for -10 <= x < 1 */

	    /* If the argument is exactly zero or a negative integer */
	    /* then return NaN. */
	    if (x == 0 || (x < 0 && x == n + 2)) {
		ML_ERROR(ME_RANGE);
		return ML_NAN;
	    }

	    /* The answer is less than half precision */
	    /* because x too near a negative integer. */
	    if (x < -0.5 && fabs(x - (int)(x - 0.5) / x) < dxrel) {
		ML_ERROR(ME_PRECISION);
	    }

	    /* The argument is so close to 0 that the result would overflow. */
	    if (y < xsml) {
		ML_ERROR(ME_RANGE);
		if(x > 0) return ML_POSINF;
		else return ML_NEGINF;
	    }

	    n = -n;

	    for (i = 0; i < n; i++) {
		value /= (x + i);
	    }
	    return value;
	}
	else {
	    /* gamma(x) for 2 <= x <= 10 */

	    for (i = 1; i <= n; i++) {
		value *= (y + i);
	    }
	    return value;
	}
    }
    else {
	/* gamma(x) for	 y = |x| > 10. */

	if (x > xmax) {			/* Overflow */
	    ML_ERROR(ME_RANGE);
	    return ML_POSINF;
	}

	if (x < xmin) {			/* Underflow */
	    ML_ERROR(ME_UNDERFLOW);
	    return ML_UNDERFLOW;
	}

	value = exp((y - 0.5) * log(y) - y + M_LN_SQRT_2PI + lgammacor(y));

	if (x > 0)
	    return value;

	if (fabs((x - (int)(x - 0.5))/x) < dxrel){

	    /* The answer is less than half precision because */
	    /* the argument is too near a negative integer. */

	    ML_ERROR(ME_PRECISION);
	}

	sinpiy = sin(M_PI * y);
	if (sinpiy == 0) {		/* Negative integer arg - overflow */
	    ML_ERROR(ME_RANGE);
	    return ML_POSINF;
	}

	return -M_PI / (y * sinpiy * value);
    }
}
/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    void gammalims(double *xmin, double *xmax);
 *
 *  DESCRIPTION
 *
 *    This function alculates the minimum and maximum legal bounds
 *    for x in gammafn(x).  These are not the only bounds, but they
 *    are the only non-trivial ones to calculate.
 *
 *  NOTES
 *
 *    This routine is a translation into C of a Fortran subroutine
 *    by W. Fullerton of Los Alamos Scientific Laboratory.
 */


/* FIXME: We need an ifdef'ed version of this which gives  */
/* the exact values when we are using IEEE 754 arithmetic. */

void gammalims(double *xmin, double *xmax)
{
    double alnbig, alnsml, xln, xold;
    int i;

    alnsml = log(d1mach(1));
    *xmin = -alnsml;
    for (i=1; i<=10; ++i) {
	xold = *xmin;
	xln = log(*xmin);
	*xmin -= *xmin * ((*xmin + .5) * xln - *xmin - .2258 + alnsml) /
		(*xmin * xln + .5);
	if (fabs(*xmin - xold) < .005) {
	    *xmin = -(*xmin) + .01;
	    goto find_xmax;
	}
    }

    /* unable to find xmin */

    ML_ERROR(ME_NOCONV);
    *xmin = *xmax = ML_NAN;

find_xmax:

    alnbig = log(d1mach(2));
    *xmax = alnbig;
    for (i=1; i<=10; ++i) {
	xold = *xmax;
	xln = log(*xmax);
	*xmax -= *xmax * ((*xmax - .5) * xln - *xmax + .9189 - alnbig) /
		(*xmax * xln - .5);
	if (fabs(*xmax - xold) < .005) {
	    *xmax += -.01;
	    goto done;
	}
    }

    /* unable to find xmax */

    ML_ERROR(ME_NOCONV);
    *xmin = *xmax = ML_NAN;

done:
    *xmin = fmax2(*xmin, -(*xmax) + 1);
}
/*
 *  Mathlib - A Mathematical Function Library
 *  Copyright (C) 1998  Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
 

int i1mach(int i)
{
    switch(i) {

    case  1: return 5;
    case  2: return 6;
    case  3: return 0;
    case  4: return 0;

    case  5: return CHAR_BIT * sizeof(int);
    case  6: return sizeof(int)/sizeof(char);

    case  7: return 2;
    case  8: return CHAR_BIT * sizeof(int) - 1;
    case  9: return INT_MAX;

    case 10: return FLT_RADIX;

    case 11: return FLT_MANT_DIG;
    case 12: return FLT_MIN_EXP;
    case 13: return FLT_MAX_EXP;

    case 14: return DBL_MANT_DIG;
    case 15: return DBL_MIN_EXP;
    case 16: return DBL_MAX_EXP;

    default: return 0;
    }
}

int i1mach_(int *i)
{
	return i1mach(*i);
}
/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    double lbeta(double a, double b);
 *
 *  DESCRIPTION
 *
 *    This function returns the value of the log beta function.
 *
 *  NOTES
 *
 *    This routine is a translation into C of a Fortran subroutine
 *    by W. Fullerton of Los Alamos Scientific Laboratory.
 */


double lbeta(double a, double b)
{
    static double corr, p, q;

    p = q = a;
    if(b < p) p = b;/* := min(a,b) */
    if(b > q) q = b;/* := max(a,b) */

#ifdef IEEE_754
    if(ISNAN(a) || ISNAN(b))
	return a + b;
#endif

    /* both arguments must be >= 0 */

    if (p < 0) {
	ML_ERROR(ME_DOMAIN);
	return ML_NAN;
    }
    else if (p == 0) {
	return ML_POSINF;
    }
#ifdef IEEE_754
    else if (!FINITE(q)) {
	return ML_NEGINF;
    }
#endif

    if (p >= 10) {
	/* p and q are big. */
	corr = lgammacor(p) + lgammacor(q) - lgammacor(p + q);
	return log(q) * -0.5 + M_LN_SQRT_2PI + corr
		+ (p - 0.5) * log(p / (p + q)) + q * logrelerr(-p / (p + q));
    }
    else if (q >= 10) {
	/* p is small, but q is big. */
	corr = lgammacor(q) - lgammacor(p + q);
	return lgammafn(p) + corr + p - p * log(p + q)
		+ (q - 0.5) * logrelerr(-p / (p + q));
    }
    else
	/* p and q are small: p <= q > 10. */
	return log(gammafn(p) * (gammafn(q) / gammafn(p + q)));
}
/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    extern int signgam;
 *    double lgammafn(double x);
 *
 *  DESCRIPTION
 *
 *    This function computes log|gamma(x)|.  At the same time
 *    the variable "signgam" is set to the sign of the gamma
 *    function.
 *
 *  NOTES
 *
 *    This routine is a translation into C of a Fortran subroutine
 *    by W. Fullerton of Los Alamos Scientific Laboratory.
 *
 *    The accuracy of this routine compares (very) favourably
 *    with those of the Sun Microsystems portable mathematical
 *    library.
 */


int signgam;

double lgammafn(double x)
{
    static double xmax = 0.;
    static double dxrel = 0.;
    double ans, y, sinpiy;

    if (xmax == 0) {
	xmax = d1mach(2)/log(d1mach(2));
	dxrel = sqrt (d1mach(4));
    }

    signgam = 1;

#ifdef IEEE_754
    if(ISNAN(x)) return x;
#endif

    if (x <= 0 && x == (int)x) { /* Negative integer argument */
	ML_ERROR(ME_RANGE);
	return ML_POSINF;/* +Inf, since lgamma(x) = log|gamma(x)| */
    }

    y = fabs(x);

    if (y <= 10) {
	return log(fabs(gammafn(x)));
    }
    else { /* y = |x| > 10  */

	if (y > xmax) {
	    ML_ERROR(ME_RANGE);
	    return ML_POSINF;
	}

	if (x > 0)
	  return M_LN_SQRT_2PI + (x - 0.5) * log(x) - x + lgammacor(y);

	/* else: x < -10 */
	sinpiy = fabs(sin(M_PI * y));

	if (sinpiy == 0) { /* Negative integer argument ===
			      Now UNNECESSARY: caught above */
	    MATHLIB_WARNING(" ** should NEVER happen! *** [lgamma.c: Neg.int, y=%g]\n",y);
	    ML_ERROR(ME_DOMAIN);
	    return ML_NAN;
	}

	ans = M_LN_SQRT_PId2 + (x - 0.5) * log(y) - x
	      - log(sinpiy) - lgammacor(y);

	if(fabs((x - (int)(x - 0.5)) * ans / x) < dxrel) {

	    /* The answer is less than half precision because */
	    /* the argument is too near a negative integer. */

	    ML_ERROR(ME_PRECISION);
	}

	if (x > 0)
	  return ans;
	else if (((int)(-x))%2 == 0)
	  signgam = -1;
	return ans;
    }
}
/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    double lgammacor(double x);
 *
 *  DESCRIPTION
 *
 *    Compute the log gamma correction factor for x >= 10 so that
 *
 *    log(gamma(x)) = log(sqrt(2*pi))+(x-.5)*log(x)-x+lgammacor(x)
 *
 *  NOTES
 *
 *    This routine is a translation into C of a Fortran subroutine
 *    written by W. Fullerton of Los Alamos Scientific Laboratory.
 */


double lgammacor(double x)
{
    static double algmcs[15] = { 
	+.1666389480451863247205729650822e+0,
	-.1384948176067563840732986059135e-4,
	+.9810825646924729426157171547487e-8,
	-.1809129475572494194263306266719e-10,
	+.6221098041892605227126015543416e-13,
	-.3399615005417721944303330599666e-15,
	+.2683181998482698748957538846666e-17,
	-.2868042435334643284144622399999e-19,
	+.3962837061046434803679306666666e-21,
	-.6831888753985766870111999999999e-23,
	+.1429227355942498147573333333333e-24,
	-.3547598158101070547199999999999e-26,
	+.1025680058010470912000000000000e-27,
	-.3401102254316748799999999999999e-29,
	+.1276642195630062933333333333333e-30
    };
    static int nalgm = 0;
    static double xbig = 0;
    static double xmax = 0;
    double tmp;

    if (nalgm == 0) {
	nalgm = chebyshev_init(algmcs, 15, d1mach(3));
	xbig = 1 / sqrt(d1mach(3));
	xmax = exp(fmin2(log(d1mach(2) / 12), -log(12 * d1mach(1))));
    }

    if (x < 10) {
        ML_ERROR(ME_DOMAIN);
        return ML_NAN;
    }
    else if (x >= xmax) {
        ML_ERROR(ME_UNDERFLOW);
        return ML_UNDERFLOW;
    }
    else if (x < xbig) {
        tmp = 10 / x;
        return chebyshev_eval(tmp * tmp * 2 - 1, algmcs, nalgm) / x;
    }
    else return 1 / (x * 12);
}
/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    double dlnrel(double x);
 *
 *  DESCRIPTION
 *
 *    Compute the relative error logarithm.
 *
 *                      log(1 + x)
 *
 *  NOTES
 *
 *    This code is a translation of a Fortran subroutine of the
 *    same name written by W. Fullerton of Los Alamos Scientific
 *    Laboratory.
 */


double logrelerr(double x)
{
    /* series for alnr on the interval -3.75000e-01 to  3.75000e-01 */
    /*                               with weighted error   6.35e-32 */
    /*                                log weighted error  31.20     */
    /*                      significant figures required  30.93     */
    /*                           decimal places required  32.01     */
    static double alnrcs[43] = {
	+.10378693562743769800686267719098e+1,
	-.13364301504908918098766041553133e+0,
	+.19408249135520563357926199374750e-1,
	-.30107551127535777690376537776592e-2,
	+.48694614797154850090456366509137e-3,
	-.81054881893175356066809943008622e-4,
	+.13778847799559524782938251496059e-4,
	-.23802210894358970251369992914935e-5,
	+.41640416213865183476391859901989e-6,
	-.73595828378075994984266837031998e-7,
	+.13117611876241674949152294345011e-7,
	-.23546709317742425136696092330175e-8,
	+.42522773276034997775638052962567e-9,
	-.77190894134840796826108107493300e-10,
	+.14075746481359069909215356472191e-10,
	-.25769072058024680627537078627584e-11,
	+.47342406666294421849154395005938e-12,
	-.87249012674742641745301263292675e-13,
	+.16124614902740551465739833119115e-13,
	-.29875652015665773006710792416815e-14,
	+.55480701209082887983041321697279e-15,
	-.10324619158271569595141333961932e-15,
	+.19250239203049851177878503244868e-16,
	-.35955073465265150011189707844266e-17,
	+.67264542537876857892194574226773e-18,
	-.12602624168735219252082425637546e-18,
	+.23644884408606210044916158955519e-19,
	-.44419377050807936898878389179733e-20,
	+.83546594464034259016241293994666e-21,
	-.15731559416479562574899253521066e-21,
	+.29653128740247422686154369706666e-22,
	-.55949583481815947292156013226666e-23,
	+.10566354268835681048187284138666e-23,
	-.19972483680670204548314999466666e-24,
	+.37782977818839361421049855999999e-25,
	-.71531586889081740345038165333333e-26,
	+.13552488463674213646502024533333e-26,
	-.25694673048487567430079829333333e-27,
	+.48747756066216949076459519999999e-28,
	-.92542112530849715321132373333333e-29,
	+.17578597841760239233269760000000e-29,
	-.33410026677731010351377066666666e-30,
	+.63533936180236187354180266666666e-31,
    };
    static int nlnrel = 0;
    static double xmin = 0.;

    if (nlnrel == 0) {
        nlnrel = chebyshev_init(alnrcs, 43, 0.1 * d1mach(3));
        xmin = -1.0 + sqrt(d1mach(4));
    }

    if (x <= -1) {
	ML_ERROR(ME_DOMAIN);
	return ML_NAN;
    }

    if (x < xmin) {
	/* answer less than half precision because x too near -1 */
	ML_ERROR(ME_PRECISION);
    }

    if (fabs(x) <= .375)
	return x * (1 - x * chebyshev_eval(x / .375, alnrcs, nlnrel));
    else
	return log(x + 1);
}
/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */


#ifdef IEEE_754
/* These are used in IEEE exception handling */
double m_zero = 0;
double m_one = 1;
double m_tiny = DBL_MIN;
#endif

#ifndef IEEE_754

void ml_error(int n)
{
    switch(n) {

    case ME_NONE:
	errno = 0;
	break;

    case ME_DOMAIN:
    case ME_NOCONV:
	errno = EDOM;
	break;

    case ME_RANGE:
	errno = ERANGE;
	break;

    default:
	break;
    }
}

#endif
/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    double pgamma(double x, double a, double scale);
 *
 *  DESCRIPTION
 *
 *    This function computes the distribution function for the
 *    gamma distribution with shape parameter a and scale parameter
 *    scale.  This is also known as the incomplete gamma function.
 *    See Abramowitz and Stegun (6.5.1) for example.
 *
 *  NOTES
 *
 *    This function is an adaptation of Algorithm 239 from the
 *    Applied Statistics Series.  The algorithm is faster than
 *    those by W. Fullerton in the FNLIB library and also the
 *    TOMS 542 alorithm of W. Gautschi.  It provides comparable
 *    accuracy to those algorithms and is considerably simpler.
 *
 *  REFERENCES
 *
 *    Algorithm 239, Incomplete Gamma Function
 *    Applied Statistics 37, 1988.
 */


static const double
    third = 1.0 / 3.0,
    zero = 0.0,
    one = 1.0,
    two = 2.0,
    oflo = 1.0e+37,
    three = 3.0,
    nine = 9.0,
    xbig = 1.0e+8,
    plimit = 1000.0e0,
    elimit = -88.0e0;

double pgamma(double x, double p, double scale)
{
    double pn1, pn2, pn3, pn4, pn5, pn6, arg, c, rn, a, b, an;
    double sum;

    /* check that we have valid values for x and p */

#ifdef IEEE_754
    if (ISNAN(x) || ISNAN(p) || ISNAN(scale))
	return x + p + scale;
#endif
    if(p <= zero || scale <= zero) {
	ML_ERROR(ME_DOMAIN);
	return ML_NAN;
    }
    x = x / scale;
    if (x <= zero)
	return 0.0;

    /* use a normal approximation if p > plimit */

    if (p > plimit) {
	pn1 = sqrt(p) * three * (pow(x/p, third) + one / (p * nine) - one);
	return pnorm(pn1, 0.0, 1.0);
    }

    /* if x is extremely large compared to p then return 1 */

    if (x > xbig)
	return one;

    if (x <= one || x < p) {

	/* use pearson's series expansion. */

	arg = p * log(x) - x - lgammafn(p + one);
	c = one;
	sum = one;
	a = p;
	do {
	    a = a + one;
	    c = c * x / a;
	    sum = sum + c;
	} while (c > DBL_EPSILON);
	arg = arg + log(sum);
	sum = zero;
	if (arg >= elimit)
	    sum = exp(arg);
    } else {

	/* use a continued fraction expansion */

	arg = p * log(x) - x - lgammafn(p);
	a = one - p;
	b = a + x + one;
	c = zero;
	pn1 = one;
	pn2 = x;
	pn3 = x + one;
	pn4 = x * b;
	sum = pn3 / pn4;
	for (;;) {
	    a = a + one;
	    b = b + two;
	    c = c + one;
	    an = a * c;
	    pn5 = b * pn3 - an * pn1;
	    pn6 = b * pn4 - an * pn2;
	    if (fabs(pn6) > zero) {
		rn = pn5 / pn6;
		if (fabs(sum - rn) <= fmin2(DBL_EPSILON, DBL_EPSILON * rn))
		    break;
		sum = rn;
	    }
	    pn1 = pn3;
	    pn2 = pn4;
	    pn3 = pn5;
	    pn4 = pn6;
	    if (fabs(pn5) >= oflo) {

                /* re-scale the terms in continued fraction */
		/* if they are large */

		pn1 = pn1 / oflo;
		pn2 = pn2 / oflo;
		pn3 = pn3 / oflo;
		pn4 = pn4 / oflo;
	    }
	}
	arg = arg + log(sum);
	sum = one;
	if (arg >= elimit)
	    sum = one - exp(arg);
    }
    return sum;
}
/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    double pnorm(double x, double mu, double sigma);
 *
 *  DESCRIPTION
 *
 *    The main computation evaluates near-minimax approximations derived
 *    from those in "Rational Chebyshev approximations for the error
 *    function" by W. J. Cody, Math. Comp., 1969, 631-637.  This
 *    transportable program uses rational functions that theoretically
 *    approximate the normal distribution function to at least 18
 *    significant decimal digits.  The accuracy achieved depends on the
 *    arithmetic system, the compiler, the intrinsic functions, and
 *    proper selection of the machine-dependent constants.
 *
 *  REFERENCE
 *
 *    Cody, W. D. (1993).
 *    ALGORITHM 715: SPECFUN - A Portable FORTRAN Package of
 *    Special Function Routines and Test Drivers".
 *    ACM Transactions on Mathematical Software. 19, 22-32.
 */


/*  Mathematical Constants */

const double SIXTEN=1.6;  			/* Magic Cutoff */

double pnorm(double x, double mu, double sigma)
{
    static double c[9] = {
	0.39894151208813466764,
	8.8831497943883759412,
	93.506656132177855979,
	597.27027639480026226,
	2494.5375852903726711,
	6848.1904505362823326,
	11602.651437647350124,
	9842.7148383839780218,
	1.0765576773720192317e-8
    };

    static double d[8] = {
	22.266688044328115691,
	235.38790178262499861,
	1519.377599407554805,
	6485.558298266760755,
	18615.571640885098091,
	34900.952721145977266,
	38912.003286093271411,
	19685.429676859990727
    };

    static double p[6] = {
	0.21589853405795699,
	0.1274011611602473639,
	0.022235277870649807,
	0.001421619193227893466,
	2.9112874951168792e-5,
	0.02307344176494017303
    };

    static double q[5] = {
	1.28426009614491121,
	0.468238212480865118,
	0.0659881378689285515,
	0.00378239633202758244,
	7.29751555083966205e-5
    };

    static double a[5] = {
	2.2352520354606839287,
	161.02823106855587881,
	1067.6894854603709582,
	18154.981253343561249,
	0.065682337918207449113
    };

    static double b[4] = {
	47.20258190468824187,
	976.09855173777669322,
	10260.932208618978205,
	45507.789335026729956
    };

    double xden, temp, xnum, result, ccum;
    double del, min, eps, xsq;
    double y;
    int i;

    /* Note: The structure of these checks has been */
    /* carefully thought through.  For example, if x == mu */
    /* and sigma == 0, we still get the correct answer. */

#ifdef IEEE_754
    if(ISNAN(x) || ISNAN(mu) || ISNAN(sigma))
	return x + mu + sigma;
#endif
    if (sigma < 0) {
	ML_ERROR(ME_DOMAIN);
	return ML_NAN;
    }
    x = (x - mu) / sigma;
#ifdef IEEE_754
    if(!finite(x)) {
	if(x < 0) return 0;
	else return 1;
    }
#endif

    eps = DBL_EPSILON * 0.5;
    min = DBL_MIN;
    y = fabs(x);
    if (y <= 0.66291) {
	xsq = 0.0;
	if (y > eps) {
	    xsq = x * x;
	}
	xnum = a[4] * xsq;
	xden = xsq;
	for (i = 1; i <= 3; ++i) {
	    xnum = (xnum + a[i - 1]) * xsq;
	    xden = (xden + b[i - 1]) * xsq;
	}
	result = x * (xnum + a[3]) / (xden + b[3]);
	temp = result;
	result = 0.5 + temp;
	ccum = 0.5 - temp;
    }
    else if (y <= M_SQRT_32) {

	/* Evaluate pnorm for 0.66291 <= |z| <= sqrt(32) */

	xnum = c[8] * y;
	xden = y;
	for (i = 1; i <= 7; ++i) {
	    xnum = (xnum + c[i - 1]) * y;
	    xden = (xden + d[i - 1]) * y;
	}
	result = (xnum + c[7]) / (xden + d[7]);
	xsq = floor(y * SIXTEN) / SIXTEN;
	del = (y - xsq) * (y + xsq);
	result = exp(-xsq * xsq * 0.5) * exp(-del * 0.5) * result;
	ccum = 1.0 - result;
	if (x > 0.0) {
	    temp = result;
	    result = ccum;
	    ccum = temp;
	}
    }
    else if(y < 50) {

	/* Evaluate pnorm for sqrt(32) < |z| < 50 */

	result = 0.0;
	xsq = 1.0 / (x * x);
	xnum = p[5] * xsq;
	xden = xsq;
	for (i = 1; i <= 4; ++i) {
	    xnum = (xnum + p[i - 1]) * xsq;
	    xden = (xden + q[i - 1]) * xsq;
	}
	result = xsq * (xnum + p[4]) / (xden + q[4]);
	result = (M_1_SQRT_2PI - result) / y;
	xsq = floor(x * SIXTEN) / SIXTEN;
	del = (x - xsq) * (x + xsq);
	result = exp(-xsq * xsq * 0.5) * exp(-del * 0.5) * result;
	ccum = 1.0 - result;
	if (x > 0.0) {
	    temp = result;
	    result = ccum;
	    ccum = temp;
	}
    }
    else {
	if(x > 0) {
	    result = 1.0;
	    ccum = 0.0;
	}
	else {
	    result = 0.0;
	    ccum = 1.0;
	}
    }
    if (result < min) {
	result = 0.0;
    }
    if (ccum < min) {
	ccum = 0.0;
    }
    return result;
}
/*
 *  R : A Computer Langage for Statistical Data Analysis
 *  Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/* Reference:
 * R. C. H. Cheng (1978).
 * Generating beta variates with nonintegral shape parameters.
 * Communications of the ACM 21, 317-322.
 * (Algorithms BB and BC)
 */


static double expmax = 0.0;

#define repeat for(;;)

double rbeta(double aa, double bb)
{
	static double a, b, delta, r, s, t, u1, u2, v, w, y, z;
	static double alpha, beta, gamma, k1, k2;
	static double olda = -1.0;
	static double oldb = -1.0;
	int qsame;

	if (expmax == 0.0)
		expmax = log(DBL_MAX);

	qsame = (olda == aa) && (oldb == bb);

	if (!qsame) {
		if (aa > 0.0 && bb > 0.0) {
			olda = aa;
			oldb = bb;
		} else {
			ML_ERROR(ME_DOMAIN);
			return ML_NAN;
		}
	}
	if (fmin2(aa, bb) <= 1.0) {	/* Algorithm BC */
		if (!qsame) {
			a = fmax2(aa, bb);
			b = fmin2(aa, bb);
			alpha = a + b;
			beta = 1.0 / b;
			delta = 1.0 + a - b;
			k1 = delta * (0.0138889 + 0.0416667 * b) /
			    (a * beta - 0.777778);
			k2 = 0.25 + (0.5 + 0.25 / delta) * b;
		}
		repeat {
			u1 = sunif();
			u2 = sunif();
			if (u1 < 0.5) {
				y = u1 * u2;
				z = u1 * y;
				if (0.25 * u2 + z - y >= k1)
					continue;
			} else {
				z = u1 * u1 * u2;
				if (z <= 0.25)
					break;
				if (z >= k2)
					continue;
			}
			v = beta * log(u1 / (1.0 - u1));
			if (v <= expmax)
				w = a * exp(v);
			else
				w = DBL_MAX;
			if (alpha * (log(alpha / (b + w)) + v) - 1.3862944
			    >= log(z))
				goto deliver;
		}
		v = beta * log(u1 / (1.0 - u1));
		if (v <= expmax)
			w = a * exp(v);
		else
			w = DBL_MAX;
	} else {		/* Algorithm BB */
		if (!qsame) {
			a = fmin2(aa, bb);
			b = fmax2(aa, bb);
			alpha = a + b;
			beta = sqrt((alpha - 2.0) / (2.0 * a * b - alpha));
			gamma = a + 1.0 / beta;
		}
		do {
			u1 = sunif();
			u2 = sunif();
			v = beta * log(u1 / (1.0 - u1));
			if (v <= expmax)
				w = a * exp(v);
			else
				w = DBL_MAX;
			z = u1 * u1 * u2;
			r = gamma * v - 1.3862944;
			s = a + r - w;
			if (s + 2.609438 >= 5.0 * z)
				break;
			t = log(z);
			if (s > t)
				break;
		}
		while (r + alpha * log(alpha / (b + w)) < t);
	}

      deliver:
	return (aa != a) ? b / (b + w) : w / (b + w);
}

/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    double sexp(void);
 *
 *  DESCRIPTION
 *
 *    Random variates from the standard normal distribution.
 *
 *  REFERENCE
 *
 *    Ahrens, J.H. and Dieter, U. (1972).
 *    Computer methods for sampling from the exponential and
 *    normal distributions.
 *    Comm. ACM, 15, 873-882.
 */

double sexp(void)
{
    /* q[k-1] = sum(alog(2.0)**k/k!) k=1,..,n, */
    /* The highest n (here 8) is determined by q[n-1] = 1.0 */
    /* within standard precision */
    static double q[] =
    {
	0.6931471805599453,
	0.9333736875190459,
	0.9888777961838675,
	0.9984959252914960,
	0.9998292811061389,
	0.9999833164100727,
	0.9999985691438767,
	0.9999998906925558,
	0.9999999924734159,
	0.9999999995283275,
	0.9999999999728814,
	0.9999999999985598,
	0.9999999999999289,
	0.9999999999999968,
	0.9999999999999999,
	1.0000000000000000
    };
    double a, u, ustar, umin;
    int i;

    a = 0.0;
    do {
      u = sunif();
    } while(u == 0.0);
    for (;;) {
	u = u + u;
	if (u > 1.0)
	    break;
	a = a + q[0];
    }
    u = u - 1.0;

    if (u <= q[0])
	return a + u;

    i = 0;
    ustar = sunif();
    umin = ustar;
    do {
	ustar = sunif();
	if (ustar < umin)
	    umin = ustar;
	i = i + 1;
    }
    while (u > q[i]);
    return a + umin * q[0];
}
/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    double snorm(void);
 *
 *  DESCRIPTION
 *
 *    Random variates from the STANDARD normal distribution  N(0,1).
 *
 * Is called from  rnorm(..), but also rt(), rf(), rgamma(), ...
 */

#define KINDERMAN_RAMAGE
#ifdef AHRENS_DIETER

/*
 *  REFERENCE
 *
 *    Ahrens, J.H. and Dieter, U.
 *    Extensions of Forsythe's method for random sampling from
 *    the normal distribution.
 *    Math. Comput. 27, 927-937.
 *
 *    The definitions of the constants a[k], d[k], t[k] and
 *    h[k] are according to the abovementioned article
 */

static double a[32] =
{
	0.0000000, 0.03917609, 0.07841241, 0.1177699,
	0.1573107, 0.19709910, 0.23720210, 0.2776904,
	0.3186394, 0.36012990, 0.40225010, 0.4450965,
	0.4887764, 0.53340970, 0.57913220, 0.6260990,
	0.6744898, 0.72451440, 0.77642180, 0.8305109,
	0.8871466, 0.94678180, 1.00999000, 1.0775160,
	1.1503490, 1.22985900, 1.31801100, 1.4177970,
	1.5341210, 1.67594000, 1.86273200, 2.1538750
};

static double d[31] =
{
	0.0000000, 0.0000000, 0.0000000, 0.0000000,
	0.0000000, 0.2636843, 0.2425085, 0.2255674,
	0.2116342, 0.1999243, 0.1899108, 0.1812252,
	0.1736014, 0.1668419, 0.1607967, 0.1553497,
	0.1504094, 0.1459026, 0.1417700, 0.1379632,
	0.1344418, 0.1311722, 0.1281260, 0.1252791,
	0.1226109, 0.1201036, 0.1177417, 0.1155119,
	0.1134023, 0.1114027, 0.1095039
};

static double t[31] =
{
	7.673828e-4, 0.002306870, 0.003860618, 0.005438454,
	0.007050699, 0.008708396, 0.010423570, 0.012209530,
	0.014081250, 0.016055790, 0.018152900, 0.020395730,
	0.022811770, 0.025434070, 0.028302960, 0.031468220,
	0.034992330, 0.038954830, 0.043458780, 0.048640350,
	0.054683340, 0.061842220, 0.070479830, 0.081131950,
	0.094624440, 0.112300100, 0.136498000, 0.171688600,
	0.227624100, 0.330498000, 0.584703100
};

static double h[31] =
{
	0.03920617, 0.03932705, 0.03950999, 0.03975703,
	0.04007093, 0.04045533, 0.04091481, 0.04145507,
	0.04208311, 0.04280748, 0.04363863, 0.04458932,
	0.04567523, 0.04691571, 0.04833487, 0.04996298,
	0.05183859, 0.05401138, 0.05654656, 0.05953130,
	0.06308489, 0.06737503, 0.07264544, 0.07926471,
	0.08781922, 0.09930398, 0.11555990, 0.14043440,
	0.18361420, 0.27900160, 0.70104740
};

#define repeat for(;;)

double snorm(void)
{
    double s, u, w, y, ustar, aa, tt;
    int i;

    u = sunif();
    s = 0.0;
    if (u > 0.5)
	s = 1.0;
    u = u + u - s;
    u *= 32.0;
    i = (int) u;
    if (i == 32)
	i = 31;
    if (i != 0) {
	ustar = u - i;
	aa = a[i - 1];
	while (ustar <= t[i - 1]) {
	    u = sunif();
	    w = u * (a[i] - aa);
	    tt = (w * 0.5 + aa) * w;
	    repeat {
		if (ustar > tt)
		    goto deliver;
		u = sunif();
		if (ustar < u)
		    break;
		tt = u;
		ustar = sunif();
	    }
	    ustar = sunif();
	}
	w = (ustar - t[i - 1]) * h[i - 1];
    }
    else {
	i = 6;
	aa = a[31];
	repeat {
	    u = u + u;
	    if (u >= 1.0)
		break;
	    aa = aa + d[i - 1];
	    i = i + 1;
	}
	u = u - 1.0;
	repeat {
	    w = u * d[i - 1];
	    tt = (w * 0.5 + aa) * w;
	    repeat {
		ustar = sunif();
		if (ustar > tt)
		    goto jump;
		u = sunif();
		if (ustar < u)
		    break;
		tt = u;
	    }
	    u = sunif();
	}
    jump:;
    }

deliver:
    y = aa + w;
    return (s == 1.0) ? -y : y;

}

#endif

#ifdef KINDERMAN_RAMAGE

/*
 *  REFERENCE
 *
 *    Kinderman A. J. and Ramage J. G. (1976).
 *    Computer generation of normal random variables.
 *    JASA 71, 893-896.
 */

#define C1		0.398942280401433
#define C2		0.180025191068563
#define g(x)		(C1*exp(-x*x/2.0)-C2*(a-fabs(x)))

static double a =  2.216035867166471;

double snorm()
{
    double t, u1, u2, u3;

    u1 = sunif();
    if(u1 < 0.884070402298758) {
	u2 = sunif();
	return a*(1.13113163544180*u1+u2-1);
    }

    if(u1 >= 0.973310954173898) {
    tail:
	u2 = sunif();
	u3 = sunif();
	t = (a*a-2*log(u3));
	if( u2*u2<(a*a)/t )
	    return (u1 < 0.986655477086949) ? sqrt(t) : -sqrt(t) ;
	goto tail;
    }

    if(u1 >= 0.958720824790463) {
    region3:
	u2 = sunif();
	u3 = sunif();
	t = a - 0.630834801921960* fmin2(u2,u3);
	if(fmax2(u2,u3) <= 0.755591531667601)
	    return (u2<u3) ? t : -t ;
	if(0.034240503750111*fabs(u2-u3) <= g(t))
	    return (u2<u3) ? t : -t ;
	goto region3;
    }

    if(u1 >= 0.911312780288703) {
    region2:
	u2 = sunif();
	u3 = sunif();
	t = 0.479727404222441+1.105473661022070*fmin2(u2,u3);
	if( fmax2(u2,u3)<=0.872834976671790 )
	    return (u2<u3) ? t : -t ;
	if( 0.049264496373128*fabs(u2-u3)<=g(t) )
	    return (u2<u3) ? t : -t ;
	goto region2;
    }

region1:
    u2 = sunif();
    u3 = sunif();
    t = 0.479727404222441-0.595507138015940*fmin2(u2,u3);
    if(fmax2(u2,u3) <= 0.805577924423817)
	return (u2<u3) ? t : -t ;
    goto region1;
}

#endif



// uniform now
extern int seed;

/*  
  Function to generate a uniform random number between 0 and 1

  Uses: Global seed variable
  
  Notes: (1) Taken from Numerical Recipes in C, page 207
*/
double sunif(void)
{
  // original R version of sunif() commented out below.
  // it's a dreadful random number generator under my system (RAND_MAX
  // is 32657 approx and it can return zeros..... frequently)
  // has been replaced by the randon number generator below - thank you Karen)

 // Wichmann + Hill random number generator (Alg. AS183 in
 // Applied Statistics 31 (1982))
  // ix, iy and iz are global seed variables and must be initialised in main

   ix=(171*ix)%30269L;
   iy=(172*iy)%30307L;
   iz=(170*iz)%30323L;
   double tem=fmod((ix/30269.0)+(iy/30307.0)+(iz/30323.0),1.0);
   return tem;
}

/**********
  double tem;
  static double y, v[98];
  static int iff = 0;
  int cc;

  if (seed < 0 || iff == 0) {
    iff = 1;
    srand(seed);
    seed = 1;
    for (cc = 1; cc < 98; cc++) v[cc] = rand();
    for (cc = 1; cc < 98; cc++) v[cc] = rand();
    y = rand();
  }
  cout << RAND_MAX << endl;
  cc = 1 + 97.0 * y / (RAND_MAX + 1.0);

  y = v[cc];
  v[cc] = rand();
//  return (double) (y / (RAND_MAX + 1.0));
  tem = (double)(y / (RAND_MAX + 1.0));
  return tem;
}
********/

/*
*  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    double runif(double a, double b);
 *
 *  DESCRIPTION
 *
 *    Random variates from the uniform distribution.
 */

double runif(double a, double b)
{
    if (b < a) {
        ML_ERROR(ME_DOMAIN);
        return ML_NAN;
    }
    if (a == b)
        return a;
    else
        return a + (b - a) * sunif();
}


double ddirich(double* y, const double* alpha, int k)
{
  // returns the log density of a dirichlet function with parameters alpha
  // at observed values y.
  // Dirichlet function form obtained from Durbin et al, 1998
 
 int i;
 double temp = 0.0;
 for(i=0; i<k; i++) temp+=alpha[i];
 
 double tem = log(gammafn(temp));
 for(i=0; i<k; i++) tem += -log(gammafn(alpha[i])) + alpha[i]*log(y[i]);
 
 // 12.12.00 was the code below, don't know why
 // double tem=1.0;
 // for(i=0; i<k; i++) tem += -log(gammafn(alpha[i]) ) + alpha[i]*log(y[i]);

 return tem;
}

/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA.
 *
 *  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    double qgamma(double p, double shape, double scale);
 *
 *  DESCRIPTION
 *
 *    Compute the quantile function of the gamma distribution.
 *
 *  NOTES
 *
 *    This function is based on the Applied Statistics
 *    Algorithm AS 91 ("ppchi2") and via pgamma(.) AS 239.
 *
 *  REFERENCES
 *
 *    Best, D. J. and D. E. Roberts (1975).
 *    Percentage Points of the Chi-Squared Disribution.
 *    Applied Statistics 24, page 385.
 */

const double C7=4.67;
const double C8=6.66;
const double C9=6.73;
const double C10=13.32;

const double EPS1=1e-2;
const double EPS2=5e-7; /* final precision */
const int MAXIT=1000;  /* was 20 */

const double pMIN=1e-100;    /* was 0.000002 = 2e-6 */
const double pMAX=(1-1e-12);  /* was 0.999998 = 1 - 2e-6 */

double qgamma(double p, double alpha, double scale)
{
    static const double
	i420  = 1./ 420.,
	i2520 = 1./ 2520.,
	i5040 = 1./ 5040;

    double a, b, c, ch, g, p1, v;
    double p2, q, s1, s2, s3, s4, s5, s6, t, x;
    int i;

    errno = 0;
    /* test arguments and initialise */

#ifdef IEEE_754
    if (ISNAN(p) || ISNAN(alpha) || ISNAN(scale))
	return p + alpha + scale;
#endif

    if (p < 0 || p > 1 || alpha <= 0) {
	ML_ERROR(ME_DOMAIN);
	return ML_NAN;
    }
    if (/* 0 <= */ p < pMIN) return 0;
    if (/* 1 >= */ p > pMAX) return ML_POSINF;

    v = 2*alpha;

    c = alpha-1;
    g = lgammafn(alpha);/* log Gamma(v/2) */


/*----- Phase I : Starting Approximation */


    if(v < (-1.24)*log(p)) {	/* for small chi-squared */

	ch = pow(p*alpha*exp(g+alpha*M_LN_2), 1/alpha);
	if(ch < EPS2) {/* Corrected according to AS 91; MM, May 25, 1999 */
	    goto END;
	}

    } else if(v > 0.32) {	/*  using Wilson and Hilferty estimate */

	x = qnorm(p, 0, 1);
	p1 = 0.222222/v;
	ch = v*pow(x*sqrt(p1)+1-p1, 3);

	/* starting approximation for p tending to 1 */

	if( ch > 2.2*v + 6 )
	    ch = -2*(log(1-p) - c*log(0.5*ch) + g);

    } else { /* for v <= 0.32 */

	ch = 0.4;
	a = log(1-p) + g + c*M_LN_2;
	do {
	    q = ch;
	    p1 = 1. / (1+ch*(C7+ch));
	    p2 = ch*(C9+ch*(C8+ch));
	    t = -0.5 +(C7+2*ch)*p1 - (C9+ch*(C10+3*ch))/p2;
	    ch -= (1- exp(a+0.5*ch)*p2*p1)/t;
	} while(fabs(q - ch) > EPS1*fabs(ch));
    }

/*----- Phase II: Iteration
 *	Call pgamma() [AS 239]  and calculate seven term taylor series
 */
    for( i=1 ; i <= MAXIT ; i++ ) {
	q = ch;
	p1 = 0.5*ch;
	p2 = p - pgamma(p1, alpha, 1);
#ifdef IEEE_754
	if(!R_FINITE(p2))
#else
	if(errno != 0)
#endif
		return ML_NAN;

	t = p2*exp(alpha*M_LN_2+g+p1-c*log(ch));
	b = t/ch;
	a = 0.5*t - b*c;
	s1 = (210+a*(140+a*(105+a*(84+a*(70+60*a))))) * i420;
	s2 = (420+a*(735+a*(966+a*(1141+1278*a)))) * i2520;
	s3 = (210+a*(462+a*(707+932*a))) * i2520;
	s4 = (252+a*(672+1182*a)+c*(294+a*(889+1740*a))) * i5040;
	s5 = (84+2264*a+c*(1175+606*a)) * i2520;
	s6 = (120+c*(346+127*c)) * i5040;
	ch += t*(1+0.5*t*s1-b*c*(s1-b*(s2-b*(s3-b*(s4-b*(s5-b*s6))))));
	if(fabs(q - ch) < EPS2*ch)
	    goto END;
    }
    ML_ERROR(ME_PRECISION);
 END:
    return 0.5*scale*ch;
}
/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA.
 *
 *  SYNOPSIS
 *
 *    double qnorm(double p, double mu, double sigma);
 *
 *  DESCRIPTION
 *
 *    Compute the quantile function for the normal distribution.
 *
 *    For small to moderate probabilities, algorithm referenced
 *    below is used to obtain an initial approximation which is
 *    polished with a final Newton step.
 *
 *    For very large arguments, an algorithm of Wichura is used.
 *
 *  REFERENCE
 *
 *    Beasley, J. D. and S. G. Springer (1977).
 *    Algorithm AS 111: The percentage points of the normal distribution,
 *    Applied Statistics, 26, 118-121.
 */


double qnorm(double p, double mu, double sigma)
{
    double q, r, val;

#ifdef IEEE_754
    if (ISNAN(p) || ISNAN(mu) || ISNAN(sigma))
	return p + mu + sigma;
#endif
    if (p < 0.0 || p > 1.0) {
	ML_ERROR(ME_DOMAIN);
	return ML_NAN;
    }

    q = p - 0.5;

    if (fabs(q) <= 0.42) {

	/* 0.08 < p < 0.92 */

	r = q * q;
	val = q * (((-25.44106049637 * r + 41.39119773534) * r
		    - 18.61500062529) * r + 2.50662823884)
	    / ((((3.13082909833 * r - 21.06224101826) * r
		 + 23.08336743743) * r + -8.47351093090) * r + 1.0);
    }
    else {

	/* p < 0.08 or p > 0.92, set r = min(p, 1 - p) */

	r = p;
	if (q > 0.0)
	    r = 1.0 - p;

	if(r > DBL_EPSILON) {
	    r = sqrt(-log(r));
	    val = (((2.32121276858 * r + 4.85014127135) * r
		    - 2.29796479134) * r - 2.78718931138)
		/ ((1.63706781897 * r + 3.54388924762) * r + 1.0);
	    if (q < 0.0)
		val = -val;
	}
	else if(r > 1e-300) {		/* Assuming IEEE here? */
	    val = -2 * log(p);
	    r = log(6.283185307179586476925286766552 * val);
	    r = r/val + (2 - r)/(val * val)
		+ (-14 + 6 * r - r * r)/(2 * val * val * val);
	    val = sqrt(val * (1 - r));
	    if(q < 0.0)
		val = -val;
	    return val;
	}
	else {
	    ML_ERROR(ME_RANGE);
	    if(q < 0.0) {
		return ML_NEGINF;
	    }
	    else {
		return ML_POSINF;
	    }
	}
    }
    val = val - (pnorm(val, 0.0, 1.0) - p) / dnorm(val, 0.0, 1.0);
    return mu + sigma * val;
}

/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA.
 *
 *  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    double dnorm(double x, double mu, double sigma);
 *
 *  DESCRIPTION
 *
 *    Compute the density of the normal distribution.
 *
 * 	M_1_SQRT_2PI = 1 / sqrt(2 * pi)
 */

	/* The Normal Density Function */

double dnorm(double x, double mu, double sigma)
{

#ifdef IEEE_754
    if (ISNAN(x) || ISNAN(mu) || ISNAN(sigma))
	return x + mu + sigma;
#endif
    if (sigma <= 0) {
	ML_ERROR(ME_DOMAIN);
	return ML_NAN;
    }

    x = (x - mu) / sigma;
    return M_1_SQRT_2PI * exp(-0.5 * x * x) / sigma;
}


// 31.10.00 haven't checked that the Mathlib and RANDLIB functions match up
// eg the gamma generator

void rdirich(double* y, const double* alpha, int k)
{
  // returns a Dirichlet distribution, sampled from alpha (k parameters).
  // this is stored in y
  // calls the Mathlib function rgamma(shape, scale)

 double sum=0.0;
 int i;

 for(i=0; i<k; i++)
 {
  y[i] = (double)rgamma(alpha[i], 1.0);
  sum += y[i];
 }
 for(i=0; i<k; i++) y[i] /= sum;
}

/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA.
 *
 *  SYNOPSIS
 *
 *    #include "Mathlib.h"
 *    double rgamma(double a, double scale);
 *
 *  DESCRIPTION
 *
 *    Random variates from the gamma distribution.
 *
 *  REFERENCES
 *
 *    [1] Shape parameter a >= 1.  Algorithm GD in:
 *
 *	  Ahrens, J.H. and Dieter, U. (1982).
 *	  Generating gamma variates by a modified
 *	  rejection technique.
 *	  Comm. ACM, 25, 47-54.
 *
 *
 *    [2] Shape parameter 0 < a < 1. Algorithm GS in:
 *
 *        Ahrens, J.H. and Dieter, U. (1974).
 *	  Computer methods for sampling from gamma, beta,
 *	  poisson and binomial distributions.
 *	  Computing, 12, 223-246.
 *
 *    Input: a = parameter (mean) of the standard gamma distribution.
 *    Output: a variate from the gamma(a)-distribution
 *
 *    Coefficients q(k) - for q0 = sum(q(k)*a**(-k))
 *    Coefficients a(k) - for q = q0+(t*t/2)*sum(a(k)*v**k)
 *    Coefficients e(k) - for exp(q)-1 = sum(e(k)*q**k)
 */

static double a1 = 0.3333333;
static double a2 = -0.250003;
static double a3 = 0.2000062;
static double a4 = -0.1662921;
static double a5 = 0.1423657;
static double a6 = -0.1367177;
static double a7 = 0.1233795;
static double e1 = 1.0;
static double e2 = 0.4999897;
static double e3 = 0.166829;
static double e4 = 0.0407753;
static double e5 = 0.010293;
static double q1 = 0.04166669;
static double q2 = 0.02083148;
static double q3 = 0.00801191;
static double q4 = 0.00144121;
static double q5 = -7.388e-5;
static double q6 = 2.4511e-4;
static double q7 = 2.424e-4;
static double sqrt32 = 5.656854;

static double aa = 0.;
static double aaa = 0.;

#define repeat for(;;)

double rgamma(double a, double scale)
{
	static double b, c, d, e, p, q, r, s, t, u, v, w, x;
	static double q0, s2, si;
	double ret_val;

	if (a < 1.0) {
		/* alternate method for parameters a below 1 */
		/* 0.36787944117144232159 = exp(-1) */
		aa = 0.0;
		b = 1.0 + 0.36787944117144232159 * a;
		repeat {
			p = b * sunif();
			if (p >= 1.0) {
				ret_val = -log((b - p) / a);
				if (sexp() >= (1.0 - a) * log(ret_val))
					break;
			} else {
				ret_val = exp(log(p) / a);
				if (sexp() >= ret_val)
					break;
			}
		}
		return scale * ret_val;
	}
	/* Step 1: Recalculations of s2, s, d if a has changed */
	if (a != aa) {
		aa = a;
		s2 = a - 0.5;
		s = sqrt(s2);
		d = sqrt32 - s * 12.0;
	}
	/* Step 2: t = standard normal deviate, */
	/* x = (s,1/2)-normal deviate. */
	/* immediate acceptance (i) */

	t = snorm();
	x = s + 0.5 * t;
	ret_val = x * x;
	if (t >= 0.0)
		return scale * ret_val;

	/* Step 3: u = 0,1 - uniform sample. squeeze acceptance (s) */
	u = sunif();
	if (d * u <= t * t * t) {
		return scale * ret_val;
	}
	/* Step 4: recalculations of q0, b, si, c if necessary */

	if (a != aaa) {
		aaa = a;
		r = 1.0 / a;
		q0 = ((((((q7 * r + q6) * r + q5) * r + q4)
			* r + q3) * r + q2) * r + q1) * r;

		/* Approximation depending on size of parameter a */
		/* The constants in the expressions for b, si and */
		/* c were established by numerical experiments */

		if (a <= 3.686) {
			b = 0.463 + s + 0.178 * s2;
			si = 1.235;
			c = 0.195 / s - 0.079 + 0.16 * s;
		} else if (a <= 13.022) {
			b = 1.654 + 0.0076 * s2;
			si = 1.68 / s + 0.275;
			c = 0.062 / s + 0.024;
		} else {
			b = 1.77;
			si = 0.75;
			c = 0.1515 / s;
		}
	}
	/* Step 5: no quotient test if x not positive */

	if (x > 0.0) {
		/* Step 6: calculation of v and quotient q */
		v = t / (s + s);
		if (fabs(v) <= 0.25)
			q = q0 + 0.5 * t * t * ((((((a7 * v + a6)
					    * v + a5) * v + a4) * v + a3)
						 * v + a2) * v + a1) * v;
		else
			q = q0 - s * t + 0.25 * t * t + (s2 + s2)
			    * log(1.0 + v);


		/* Step 7: quotient acceptance (q) */

		if (log(1.0 - u) <= q)
			return scale * ret_val;
	}
	/* Step 8: e = standard exponential deviate */
	/* u= 0,1 -uniform deviate */
	/* t=(b,si)-double exponential (laplace) sample */

	repeat {
		e = sexp();
		u = sunif();
		u = u + u - 1.0;
		if (u < 0.0)
			t = b - si * e;
		else
			t = b + si * e;
		/* Step  9:  rejection if t < tau(1) = -0.71874483771719 */
		if (t >= -0.71874483771719) {
			/* Step 10:  calculation of v and quotient q */
			v = t / (s + s);
			if (fabs(v) <= 0.25)
				q = q0 + 0.5 * t * t * ((((((a7 * v + a6)
					    * v + a5) * v + a4) * v + a3)
						 * v + a2) * v + a1) * v;
			else
				q = q0 - s * t + 0.25 * t * t + (s2 + s2)
				    * log(1.0 + v);
			/* Step 11:  hat acceptance (h) */
			/* (if q not positive go to step 8) */
			if (q > 0.0) {
				if (q <= 0.5)
					w = ((((e5 * q + e4) * q + e3)
					      * q + e2) * q + e1) * q;
				else
					w = exp(q) - 1.0;
				/* if t is rejected */
				/* sample again at step 8 */
				if (c * fabs(u) <= w * exp(e - 0.5 * t * t))
					break;
			}
		}
	}
	x = s + 0.5 * t;
	return scale * x * x;
}



/***
main()
{
 double param;
 cout << "Enter a gamma parameter: ";
 cin >> param;
 double temp=qgamma(0.125, param, param);
 cout << "qgamma(0.125, param, param) = " << temp << endl;
}
***/
