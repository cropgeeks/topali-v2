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
}
