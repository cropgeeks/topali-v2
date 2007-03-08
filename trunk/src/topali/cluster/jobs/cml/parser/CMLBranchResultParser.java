// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml.parser;

import java.io.*;

import topali.data.CMLHypothesis;

public class CMLBranchResultParser
{

	CMLHypothesis hypo;
	
	public CMLBranchResultParser(CMLHypothesis hypo) {
		this.hypo = hypo;
	}
	
	public void parse(File file) {
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line = null;
			
			boolean tree = false;
			
			while ((line = in.readLine()) != null)
			{
				line = line.trim();
				
				if (line.startsWith("lnL(ntime:"))
				{
					String a = line.substring(line.lastIndexOf(':') + 1);
					a = a.trim();
					String[] b = a.split("\\s+");
					hypo.likelihood = Double.parseDouble(b[0]);
					continue;
				}
				
				if(line.startsWith("w ratios as labels")) {
					tree = true;
					continue;
				}
				
				if(tree) {
					hypo.omegaTree = line;
					tree = false;
					continue;
				}
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
