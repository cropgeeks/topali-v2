// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.test;

import junit.framework.TestCase;
import topali.var.Utils;

public class UtilsTest extends TestCase
{

	public void testOpenBrowser() {
		boolean success = false;
		try
		{
			success = Utils.openBrowser("http://www.google.co.uk");
		} catch (Exception e)
		{
			e.printStackTrace();
		} 
		assertTrue(success);
	}
	
}
