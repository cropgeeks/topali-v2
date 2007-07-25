// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.phyml;

import java.io.*;

import topali.data.PhymlResult;

public class PhymlParser
{

	public static PhymlResult parse(File f, PhymlResult res) throws Exception {
		
		BufferedReader in = new BufferedReader(new FileReader(f));
		StringBuffer sb = new StringBuffer();
		String line = null;
		while((line = in.readLine())!=null)
			sb.append(line);
		
		res.setTreeStr(sb.toString());
		
		return res;
	}
}
