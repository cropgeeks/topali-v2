// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.test.parsers;

import java.io.File;

import junit.framework.TestCase;
import topali.cluster.jobs.modeltest.analysis.TreeDistParser;


public class TreeDistParserTest extends TestCase
{

	public void test() throws Exception {
		int[][] expected = new int[][] {
				{1,2,3,4},
				{5,6,7,8}
		};
		
		TreeDistParser pars = new TreeDistParser();
		File file = new File(this.getClass().getResource("/res/testing/treeDist.txt").toURI());
		int[][] actual = pars.parse(file);
		
		assertTrue("Array dimension not equal!", expected.length==actual.length);
		assertTrue("Array dimension not equal!", expected[0].length==actual[0].length);
		
		for(int i=0; i<expected.length; i++) 
			for(int j=0; j<expected[i].length; j++)
				assertTrue("Array content is different!", expected[i][j]==actual[i][j]);
	}
}
