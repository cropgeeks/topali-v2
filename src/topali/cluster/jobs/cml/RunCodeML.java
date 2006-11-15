// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.cml;

import java.io.*;

import pal.alignment.*;
import pal.distance.*;
import pal.tree.*;

import topali.cluster.*;
import topali.data.*;

import sbrn.commons.file.*;

class RunCodeML
{
	private File wrkDir;
	private CodeMLResult result;
	
	RunCodeML(File wrkDir, CodeMLResult result)
	{
		this.wrkDir = wrkDir;
		this.result = result;
	}
	
	void run()
		throws Exception
	{
		ProcessBuilder pb = new ProcessBuilder(result.codemlPath);
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);
		
		Process proc = pb.start();
						
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(
			proc.getOutputStream()));
		
		new StreamCatcher(proc.getInputStream(), true);
		
		writer.close();
		
		try { proc.waitFor(); }
		catch (Exception e) {
			System.out.println(e);
		}
	}
	
	// Saves the codeml.ctl settings file used by CODEML
	void saveCTLSettings(int iteration)
		throws IOException
	{
		String nl = System.getProperty("line.separator");
		
		StringBuffer settings = new StringBuffer();
		
		settings.append("seqfile   = seq.phy" + nl);
		settings.append("treefile  = tree.txt" + nl);
		settings.append("outfile   = results.txt" + nl);
		settings.append("noisy     = 9" + nl);				// 0,1,2,3,9: how much rubbish on the screen
		settings.append("verbose   = 1" + nl);				// 1:detailed output
		settings.append("runmode   = 0" + nl);				// 0:user defined tree
		settings.append("seqtype   = 1" + nl);				// 1:codons
		settings.append("CodonFreq = 2" + nl);				// 0:equal, 1:F1X4, 2:F3X4, 3:F61
		settings.append("model     = 0" + nl);				// 0:one omega ratio for all branches
		settings.append("icode     = 0" + nl);				// 0:universal code
		settings.append("fix_kappa = 0" + nl);				// 1:kappa fixed, 0:kappa to be estimated
		settings.append("kappa     = 2" + nl);				// initial or fixed kappa
		settings.append("fix_omega = 0" + nl);				// 1:omega fixed, 0:omega to be estimated 
		settings.append("omega     = 5" + nl);				// initial omega
		
		settings.append("*ncatG    = 3" + nl);
		settings.append("*ncatG    = 10" + nl);

		/*
		 * *set ncatG for models M3, M7, and M8!!!
		 * # of site categories for M3 in Table 4
		 * # of site categories for M7 and M8 in Table 4
		 */
		
		settings.append("NSsites   = 0" + nl);
		
		/*
		 * 0:one omega ratio (M0)
		 * 1:neutral (M1)
		 * 2:selection (M2)
		 * 3:discrete (M3)
		 * 7:beta (M7)
		 * 8:beta&w (M8)
		 */

		File ctlFile = new File(wrkDir, "codeml.ctl");
		FileUtils.writeFile(ctlFile, settings.toString());
	}
	
	// Fast method to generate a JC/NJ tree from the alignment to be analysed
	void createTree()
		throws Exception
	{
		String file = new File(wrkDir, "seq.phy").getPath();
		ReadAlignment alignment = new ReadAlignment(file);
		
		JukesCantorDistanceMatrix dm = new JukesCantorDistanceMatrix(alignment);
		Tree tree = new NeighborJoiningTree(dm);
		
		FileUtils.writeFile(new File(wrkDir, "tree.txt"), tree.toString());
	}
}
