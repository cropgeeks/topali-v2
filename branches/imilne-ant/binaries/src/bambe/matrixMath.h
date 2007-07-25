
#ifndef MATRIXMATH
#define MATRIXMATH

#include "util.h"

class MatrixMath {
private:
  void SetToIdentity(Vector<BaseArray>& matrix);
  void DivideByTwos(Vector<BaseArray>& A, int power);
  void ComputeLandU(Vector<BaseArray> C, Vector<BaseArray>& L, 
		  Vector<BaseArray>& U);
  void ForwardSubstitutionRow(Vector<BaseArray> L, double* b);
  void BackSubstitutionRow(Vector<BaseArray> U, double* b);
  void GaussianElimination(Vector<BaseArray> A, Vector<BaseArray> B, 
			 Vector<BaseArray>& X);
  int LogBase2Plus1(double x);

  const int qValue;

public:
  MatrixMath();
  ~MatrixMath() {}
  void PrintMatrix(Vector<BaseArray>& matrix);
  void MultiplyMatrices(Vector<BaseArray> A, Vector<BaseArray> B, 
		      Vector<BaseArray>& Result);
  void MultiplyMatrixByScalar(Vector<BaseArray> A, double scalar, 
			    Vector<BaseArray>& Result);
  void AddTwoMatrices(Vector<BaseArray> A, Vector<BaseArray> B, 
		    Vector<BaseArray>& Sum);

  void ComputeMatrixExponential(Vector<BaseArray> A, Vector<BaseArray>& F);
};

#endif
