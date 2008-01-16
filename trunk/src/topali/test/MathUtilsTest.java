// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.test;

import topali.var.utils.MathUtils;
import junit.framework.TestCase;

public class MathUtilsTest extends TestCase
{
	public void testRound() {
		double exp = 0.05;
		double d1 = MathUtils.round(0.049, 2);
		double d2 = MathUtils.round(0.051, 2);
		assertEquals(exp, d1);
		assertEquals(exp, d2);
	}
	
	public void testCalcLR() {
		double exp1 = 20;
		double exp2 = 40;
		double lr1 = MathUtils.calcLR(-1000, -1010);
		double lr2 = MathUtils.calcLR(-1000, -980);
		assertEquals(exp1, lr1);
		assertEquals(exp2, lr2);
	}
	
	public void testCalcLRT() {
		double exp1 = 0.0067;
		double exp2 = 0.2873;
		double lrt1 = MathUtils.calcLRT(10, 2);
		lrt1 = MathUtils.round(lrt1, 4);
		double lrt2 = MathUtils.calcLRT(5, 4);
		lrt2 = MathUtils.round(lrt2, 4);
		assertEquals(exp1, lrt1);
		assertEquals(exp2, lrt2);
	}
	
	public void testGetRoughSignificance() {
		String exp1 = "<0.001";
		String exp2 = "<0.01";
		String exp3 = "<0.05";
		String exp4 = "NS";
		String s1 = MathUtils.getRoughSignificance(0.0001);
		String s2 = MathUtils.getRoughSignificance(0.008);
		String s3 = MathUtils.getRoughSignificance(0.02);
		String s4 = MathUtils.getRoughSignificance(0.06);
		assertEquals(exp1, s1);
		assertEquals(exp2, s2);
		assertEquals(exp3, s3);
		assertEquals(exp4, s4);
	}
	
	public void testCalcAIC1() {
		double exp = 12537.9420;
		double d = MathUtils.calcAIC1(-6238.9710, 30);
		d = MathUtils.round(d, 4);
		assertEquals(exp, d);
	}
	
	public void testCalcAIC2() {
		double exp = 12539.8635;
		double d = MathUtils.calcAIC2(-6238.9710, 30, 999);
		d = MathUtils.round(d, 4);
		assertEquals(exp, d);
	}
	
	public void testCalcBIC() {
		double exp = 12685.1446;
		double d = MathUtils.calcBIC(-6238.9710, 30, 999);
		d = MathUtils.round(d, 4);
		assertEquals(exp, d);
	}
}
