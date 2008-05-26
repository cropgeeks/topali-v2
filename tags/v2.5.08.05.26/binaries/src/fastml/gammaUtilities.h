 #ifndef ___GAMMA_UTILITIES
#define ___GAMMA_UTILITIES

#include "definitions.h"

/******************************************************************************
gamma utilities include calculating ln gamma and integral of gamma.
used mainly in building the gamma function and creating categories within it
******************************************************************************/
MDOUBLE gammp(MDOUBLE a, MDOUBLE x);
MDOUBLE gammln(MDOUBLE xx);

void gser(MDOUBLE *gamser, MDOUBLE a, MDOUBLE x, MDOUBLE *gln);
void gcf(MDOUBLE *gammcf, MDOUBLE a, MDOUBLE x, MDOUBLE *gln);

MDOUBLE search_for_z_in_dis_with_beta_1(MDOUBLE alpha, MDOUBLE ahoson);
MDOUBLE search_for_z_in_dis_with_any_beta(MDOUBLE alpha,MDOUBLE beta, MDOUBLE ahoson);
MDOUBLE the_avarage_r_in_category_between_a_and_b(MDOUBLE a, MDOUBLE b, MDOUBLE alpha, MDOUBLE beta, int k);

const int ITMAX = 100;
const MDOUBLE EPS = static_cast<MDOUBLE>(0.0000003);
const MDOUBLE FPMIN = static_cast<MDOUBLE>(1.0e-30);
const MDOUBLE ERR_FOR_GAMMA_CALC = static_cast<MDOUBLE>(0.00001);
const MDOUBLE MINIMUM_ALPHA_PARAM = static_cast<MDOUBLE>(0.05);

#endif
