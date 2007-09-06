// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.mrbayes;

import java.io.*;

import org.apache.log4j.Logger;

public class MrBayesParser
{
	static  Logger log = Logger.getLogger(MrBayesParser.class);
	
	public static String getTree(File f) {
		try
		{
			String treeStr = null;

			BufferedReader in = new BufferedReader(new FileReader(f));
			String str = in.readLine();
			while (str != null)
			{
				if (str.startsWith("   tree"))
				{
					treeStr = str.substring(str.indexOf("=") + 2);
					break;
				}

				str = in.readLine();
			}

			in.close();
			
			return treeStr;

		} catch (Exception e)
		{
			log.error("Problem reading MrBayes file.", e);
		}
		
		log.warn("File didn't contain MrBayes tree!");
		return null;
	}
	
}
