// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.raxml.analysis;

import java.io.*;

public class RaxmlParser
{

	public static String getTree(File file) throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(file));
		String tree = in.readLine();
		in.close();
		return tree;
	}
	
	public static double getLikelihood(File file) throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line;
		while((line=in.readLine())!=null) {
			line = line.trim();
			if(line.startsWith("Likelihood")) {
				String[] tmp = line.split("\\s+");
				if(tmp[1].equals(":")) {
					double lnl = Double.parseDouble(tmp[2]);
					in.close();
					return lnl;
				}
			}
		}
		in.close();
		return 0;
	}
	
	public static String getOutput(File file) throws Exception {
		StringBuffer result = new StringBuffer();
		
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line;
		boolean parse = false;
		while((line=in.readLine())!=null) {
			if(line.startsWith("Executing")) {
				parse = true;
				continue;
			}
			if(line.startsWith("RAxML")) {
				parse = false;
				continue;
			}
			if(line.contains(" alpha ")) {
				int i = line.indexOf("alpha");
				result.append("alpha1 (, alpha2, alpha3): "+line.substring(i+5));
				parse = false;
				continue;
			}
			if(parse) {
				result.append(line+"\n");
			}
		}
		in.close();
		
		return result.toString();
	}
}
