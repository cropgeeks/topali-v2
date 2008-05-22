#include "matrixMath.h"

MatrixMath::MatrixMath() :
  qValue(4)
{
}

void MatrixMath::PrintMatrix(Vector<BaseArray>& matrix)
{
  for (int i = 0; i < NUM_BASES; i++) {
    for(int j = 0; j < NUM_BASES; j++)
      cout << matrix[i][j] << " ";
    cout << endl;
  }
}

void MatrixMath::SetToIdentity(Vector<BaseArray>& matrix)
{
  int row, col;

  for (row = 0; row < NUM_BASES; row++)
    for (col = 0; col < NUM_BASES; col++)
      matrix[row][col] = (row == col ? 1 : 0);
}

/*
  Matrix Multiplication: Dot Product
  Page 11
 */
void MatrixMath::MultiplyMatrices(Vector<BaseArray> A, Vector<BaseArray> B, 
		      Vector<BaseArray>& Result)
{
  int i, j, k;

  for (i = 0; i < NUM_BASES; i++)
    for (j = 0; j < NUM_BASES; j++) {
      Result[i][j] = 0;
      for (k = 0; k < NUM_BASES; k++) {
	Result[i][j] = Result[i][j] + A[i][k] * B[k][j];
      }
    }
}

void MatrixMath::MultiplyMatrixByScalar(Vector<BaseArray> A, double scalar, 
			    Vector<BaseArray>& Result)
{
  int row, col;
  
  for (row = 0; row < NUM_BASES; row++)
    for (col = 0; col < NUM_BASES; col++)
      Result[row][col] = A[row][col] * scalar;
}

void MatrixMath::AddTwoMatrices(Vector<BaseArray> A, Vector<BaseArray> B, 
		    Vector<BaseArray>& Sum)
{
  int row, col;
  
  for (row = 0; row < NUM_BASES; row++)
    for (col = 0; col < NUM_BASES; col++) {
      Sum[row][col] = A[row][col] + B[row][col];
    }
}


void MatrixMath::DivideByTwos(Vector<BaseArray>& A, int power)
{
  int divisor = 1;
  int i, row, col;

  for (i = 0; i < power; i++)
    divisor = divisor * 2;

  for (row = 0; row < NUM_BASES; row++)
    for (col = 0; col < NUM_BASES; col++)
      A[row][col] /= divisor;
}

/*
  Gaxpy Gaussian Elimination
  Page 99
 */
void MatrixMath::ComputeLandU(Vector<BaseArray> C, Vector<BaseArray>& L, 
		  Vector<BaseArray>& U)
{
  int i, j, k, m;
  int row, col;

  for (j = 0; j < NUM_BASES; j++) {
    for (k = 0; k < j; k++)
      for (i = k+1; i < j; i++)
	C[i][j] = C[i][j] - C[i][k] * C[k][j];
      
    for (k = 0; k < j; k++)
      for (i = j; i < NUM_BASES; i++)
	C[i][j] = C[i][j] - C[i][k]*C[k][j];

    for (m = j+1; m < NUM_BASES; m++)
      C[m][j] /= C[j][j]; 
  }

  for (row = 0; row < NUM_BASES; row++)
    for (col = 0; col < NUM_BASES; col++) {
      if (row <= col) {
	U[row][col] = C[row][col];
	L[row][col] = (row == col ? 1 : 0);
      }
      else {
	L[row][col] = C[row][col];
	U[row][col] = 0;
      }
    }
}

/*
  Forward Substitution - Row
  Page 87
 */
void MatrixMath::ForwardSubstitutionRow(Vector<BaseArray> L, double* b)
{
  int i, j;
  double dotProduct;

  b[0] = b[0] / L[0][0];
  for (i = 1; i < NUM_BASES; i++) {
    dotProduct = 0;
    for (j = 0; j < i; j++)
      dotProduct += L[i][j] * b[j];
    
    b[i] = (b[i] - dotProduct) / L[i][i];
  }
}

/*
  Back Substitution - Row
  Page 88
 */
void MatrixMath::BackSubstitutionRow(Vector<BaseArray> U, double* b)
{
  int i, j;
  double dotProduct;

  b[3] = b[3] / U[3][3];
  for (i = 2; i >= 0; i--) {
    dotProduct = 0;
    for (j = i+1; j < NUM_BASES; j++)
      dotProduct += U[i][j] * b[j];
    
    b[i] = (b[i] - dotProduct) / U[i][i];
  }
}

/*
  Computing X in AX = B
  Page 120
 */
void MatrixMath::GaussianElimination(Vector<BaseArray> A, Vector<BaseArray> B, 
			 Vector<BaseArray>& X)
{
  Vector<BaseArray> L(NUM_BASES);
  Vector<BaseArray> U(NUM_BASES);
  int i, k;
  double b[NUM_BASES];

  ComputeLandU(A, L, U);

  for (k = 0; k < NUM_BASES; k++) {
    for (i = 0; i < NUM_BASES; i++)
      b[i] = B[i][k];

    // Answer of Ly = b (which is solving for y) is copied into b.
    ForwardSubstitutionRow(L, b);

    // Answer of Ux = y (solving for x & the y was copied into b above) 
    // is also copied into b.
    BackSubstitutionRow(U, b);

    for (i = 0; i < NUM_BASES; i++)
      X[i][k] = b[i];
  }
}

int MatrixMath::LogBase2Plus1(double x)
{
  int j = 0;
  while(x > 1 - 1.0e-07) {
    x /= 2;
    j++;
  }
  return j;
}

void MatrixMath::ComputeMatrixExponential(Vector<BaseArray> A, Vector<BaseArray>& F)
{
  int i, j, k;
  int negativeFactor;
  double maxAValue, c;

  Vector<BaseArray> D(NUM_BASES); 
  Vector<BaseArray> N(NUM_BASES);
  Vector<BaseArray> X(NUM_BASES);
  Vector<BaseArray> cX(NUM_BASES);

  SetToIdentity(D);
  SetToIdentity(N);
  SetToIdentity(X);

  maxAValue = 0;
  for (i = 0; i < NUM_BASES; i++)
    maxAValue = MAXIMUM(maxAValue, fabs(A[i][i]));
  
  j = MAXIMUM(0, LogBase2Plus1(maxAValue));

  DivideByTwos(A, j);

  c = 1;
  for (k = 1; k <= qValue; k++) {
    c = c * (qValue - k + 1) / ((2 * qValue - k + 1) * k);
    
    // X = AX
    MultiplyMatrices(A, X, X);

    // N = N + cX
    MultiplyMatrixByScalar(X, c, cX);
    AddTwoMatrices(N, cX, N);

    // D = D + (-1)^k*cX
    negativeFactor = (k % 2 == 0 ? 1 : -1);
    if (negativeFactor == -1)
      MultiplyMatrixByScalar(cX, negativeFactor, cX);
    AddTwoMatrices(D, cX, D);      
  }

  GaussianElimination(D, N, F);

  for (k = 0; k < j; k++)
    MultiplyMatrices(F, F, F);
}

/*int max(int A, int B)
{
  return (A > B ? A : B);
}

double max(double A, double B)
{
  return (A > B ? A : B);
}*/

/*
void main()
{
  Vector<BaseArray> A(NUM_BASES), T(NUM_BASES);

  for (int i = 0; i < 3; i++)
    for (int j = 0; j < NUM_BASES; j++)
      A[i][j] = 1;

  A[0][0] = A[1][1] = A[2][2] = -3;
  A[3][0] = A[3][1] = A[3][2] = 2;
  A[3][3] = -6;

  ComputeMatrixExponential(A, T);

  cout << "Answer: " << endl;
  PrintMatrix(T);
}*/
