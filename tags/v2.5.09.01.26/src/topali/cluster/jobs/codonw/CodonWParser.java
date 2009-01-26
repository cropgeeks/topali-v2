// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.codonw;

import java.io.*;

import topali.data.CodonWResult;

public class CodonWParser
{

	public static CodonWResult parse(File f1, File f2, CodonWResult result) {
		BufferedReader in = null;
		
		try
		{
			StringBuffer sb = new StringBuffer();
			sb.append("Summary:\n");
			
			in = new BufferedReader(new FileReader(f1));
			String line = null;
			while((line=in.readLine())!=null) {
				sb.append(line+"\n");
			}
			in.close();
				
			sb.append("\nCodon Usage table:\n");
			in = new BufferedReader(new FileReader(f2));
			while((line=in.readLine())!=null) {
				sb.append(line+"\n");
			}
			
			result.result = sb.toString();
		} catch (Exception e)
		{
		}
		finally {
			try
			{
				in.close();
			} catch (IOException e)
			{
			}
		}
		
		return result;
	}
}
