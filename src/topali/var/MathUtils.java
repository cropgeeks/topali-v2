// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var;

import pal.statistics.ChiSquareDistribution;

public class MathUtils
{

	public static double calcLR(double l1, double l2) {
		double lr = (l1 > l2) ? 2 * (l1 - l2) : 2 * (l2 - l1);
		return lr;
	}
	
	public static double calcLRT(double lr, int df) {
		double lrt = 1 - ChiSquareDistribution.cdf(lr, df);
		return lrt;
	}
	
	public static String getRoughSignificance(double lrt) {
		if (lrt < 0.001)
			return "<0.001";
		else if (lrt < 0.01)
			return "<0.01";
		else if (lrt < 0.05)
			return "<0.05";
		else
			return "NS";
	}
	
	public static String getRoughSignificance(double l1, double l2, int df) {
		double lr = MathUtils.calcLR(l1, l2);
		double lrt = MathUtils.calcLRT(lr, df);
		return MathUtils.getRoughSignificance(lrt);
	}
	
	/**
	 * AIC1 = -2lnl + 2df
	 * (K = df)
	 * @param lnl Log-Likelihood
	 * @param df Degres of freedom (free parameters)
	 * @return
	 */
	public static double calcAIC1(double lnl, int df) {
		return (-2*lnl + 2*df);
	}
	
	/**
	 * AIC2 = -2lnl + 2df + 2df(df+1)/(n-df-1)
	 * @param lnl Log-Likelihood
	 * @param df Degres of freedom (free parameters)
	 * @param n Sample size (Sequence length)
	 * @return
	 */
	public static double calcAIC2(double lnl, int df, int n) {
		double dfd = (double)df;
		double nd = (double)n;
		return (calcAIC1(lnl, df) + 2d*dfd*(dfd + 1d)/(nd - dfd - 1d));
	}
	
	/**
	 * BIC = -2lnl + df * log(n)
	 * @param lnl Log-Likelihood
	 * @param df Degres of freedom (free parameters)
	 * @param n Sample size (Sequence length)
	 * @return
	 */
	public static double calcBIC(double lnl, int df, int n) {
		double dfd = (double)df;
		double nd = (double)n;
		return (-2d*lnl + dfd * Math.log(nd));
	}
}
