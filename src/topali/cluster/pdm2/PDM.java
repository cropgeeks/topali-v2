// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.pdm2;

import java.io.*;

import pal.alignment.*;

import topali.data.*;

/* 
 * Class that performs a PDM calculation on a single window of alignment data.
 */
class PDM
{
	private File wrkDir;
	private PDMResult result;
	
	private SimpleAlignment window;
	
	PDM(File wrkDir, PDMResult result)
	{
		this.wrkDir = wrkDir;
		this.result = result;
	}
	
/*	PDM(File wrkDir, PDMResult result, SimpleAlignment window)
	{
		this.wrkDir = wrkDir;
		this.result = result;
		this.window = window;
	}
*/
	
	void calculatePDM()
	{
		// take the window (NEXUS format), and run MrBayes
		
		try
		{
			RunMrBayes mb = new RunMrBayes(wrkDir, result);
			mb.run();
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
	}
}