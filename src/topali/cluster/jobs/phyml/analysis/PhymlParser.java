// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.phyml.analysis;

import java.io.*;

import topali.data.PhymlResult;
import topali.var.tree.NHTreeUtils;

public class PhymlParser
{

	public static PhymlResult parse(File treeFile, File file, PhymlResult res) throws Exception {
		
		BufferedReader in = new BufferedReader(new FileReader(treeFile));
		StringBuffer sb = new StringBuffer();
		String line = null;
		while((line = in.readLine())!=null)
			sb.append(line);
		
		String tree = sb.toString();
		//TODO: WORKAROUND for phyml's zero bootstrap values
		tree = NHTreeUtils.removeBootstrapValues(tree, 1);
		//END WORKAROUND
		res.setTreeStr(tree);
		
		in = new BufferedReader(new FileReader(file));
		line = null;
		while((line=in.readLine())!=null) {
			String[] tmp = line.split("\\s+");
			if(tmp.length>1 && tmp[1].equals("Likelihood")) {
				res.setLnl(Double.parseDouble(tmp[5]));
			}
		}
		return res;
	}
}
