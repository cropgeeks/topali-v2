// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.test;

import java.util.Arrays;

import junit.framework.TestCase;
import topali.var.utils.Utils;

public class UtilsTest extends TestCase
{

	public void testOpenBrowser() {
//		boolean success = false;
//		try
//		{
//			success = Utils.openBrowser("http://www.google.co.uk");
//		} catch (Exception e)
//		{
//			e.printStackTrace();
//		} 
//		assertTrue(success);
	}
	
	public void testcastArray() throws Exception {
	    String[][] array = new String[][] {{"0", "1", "2"}, {"3", "4", "5"}};
	    Integer[][] exp1 = new Integer[][] {{new Integer(0), new Integer(1), new Integer(2)}, {new Integer(3), new Integer(4), new Integer(5)}};
	    int[][] exp2 = new int[][] {{0,1,2},{3,4,5}};
	    float[][] exp3 = new float[][] {{0f,1f,2f},{3f,4f,5f}};
	    
	    Integer[][] a1 = (Integer[][])Utils.castArray(array, Integer.class);
	    int[][] a2 = (int[][])Utils.castArray(a1, int.class);
	    float[][] a3 = (float[][])Utils.castArray(a2, float.class);
	    int[][] a4 = (int[][])Utils.castArray(a2, int.class);
	    
	    assertTrue(Arrays.deepEquals(a1, exp1));
	    assertTrue(Arrays.deepEquals(a2, exp2));
	    assertTrue(Arrays.deepEquals(a3, exp3));
	    assertTrue(Arrays.deepEquals(a4, exp2));
	}
}
