// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.mrbayes;

import java.io.*;

import topali.data.MBTreeResult;

public class MBParser 
{

	public static MBTreeResult parse(File dir, MBTreeResult res) throws Exception{
		
		//Parse tree
		File file = new File(dir, "mb.nex.con");
		BufferedReader in = new BufferedReader(new FileReader(file));
		String str = in.readLine();
		while (str != null)
		{
			if (str.startsWith("   tree"))
			{
				String tree = str.substring(str.indexOf("=") + 2);
				res.setTreeStr(tree);
				break;
			}

			str = in.readLine();
		}
		in.close();
		
		//Parse process output
		StringBuffer summary = new StringBuffer();
		file = new File(dir, "mb.out");
		in = new BufferedReader(new FileReader(file));
		str = null;
		boolean warn = false;
		boolean p1 = false, p2 = false, p3 = false;
		while((str=in.readLine())!=null) {
			str = str.trim();
			if(str.startsWith("Estimated marginal likelihoods")) {
				p1 = true;
				continue;
			}
			
			if(p1 && str.startsWith("TOTAL")) {
				String[] tmp = str.split("\\s+");
				res.setLnl(Double.parseDouble(tmp[1]));
				p1 = false;
				continue;
			}
			
			if(str.startsWith("the phylogenetic context")) {
				summary.append(str);
				summary.append("\n\n");
				p2 = false;
				continue;
			}
			
			if(p2 || str.startsWith("95% Cred. Interval")) {
				p2 = true;
				summary.append(str);
				summary.append('\n');
				String[] tmp = str.split("\\s+");
				try
				{
					double psrf = Double.parseDouble(tmp[6]);
					if(psrf>1.2)
						warn = true;
				} catch (RuntimeException e)
				{
				}
			}
			
			if(str.startsWith("Clade credibility values")) {
				p3 = false;
				continue;
			}
			
			if(p3 || str.startsWith("Summary statistics for taxon bipartitions")) {
				summary.append(str);
				if(!p3)
					summary.append("\n-----------------------------------------");
				else
					summary.append('\n');
				p3 = true;
				String[] tmp = str.split("\\s+");
				try
				{
					double psrf = Double.parseDouble(tmp[8]);
					if(psrf>1.2)
						warn = true;
				} catch (RuntimeException e)
				{
				}
			}
		}
		
		res.summary = summary.toString();
		res.info = res.info.replaceFirst("\\[SUMMARY\\]", res.summary);
		
		if(warn) {
			res.warning = "One or more PSRF values are greater than 1.2!\n" +
			"This means the MCMC chains may have not converged.\n" +
			"Please rerun the analysis with a higher number of\n" +
			"generations and/or longer burnin period.\n" +
			"(For further details view tree and click on information icon)";
		}
		
		return res;
	}
	
}
