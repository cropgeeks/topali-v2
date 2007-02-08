// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml.parser;

import java.io.BufferedReader;
import java.io.FileReader;

public class Model7Parser extends CMLResultParser {

	@Override
	public void parse(String resultFile, String rstFile) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(resultFile));
			String line = null;
			while((line=in.readLine())!=null) {
				String a;
				String[] b;
				
				line = line.trim();
				
				try
				{
					if(line.startsWith("lnL(ntime:")) {
						a = line.substring(line.lastIndexOf(':')+1);
						a = a.trim();
						b = a.split("\\s+");
						model.likelihood = Float.parseFloat(b[0]);
						continue;
					}
					
					if(line.startsWith("p=")) {
						b = line.split("\\s+");
						model.p = Float.parseFloat(b[1]);
						model.q = Float.parseFloat(b[3]);
						continue;
					}
					
					if(model.dnDS==-1 && line.matches("\\d\\.\\.\\d(\\s+\\d+\\.\\d+){8}")) {
						b = line.split("\\s+");
						model.dnDS = Float.parseFloat(b[4]);
					}
				} catch (RuntimeException e)
				{
					e.printStackTrace();
					System.err.println("Error parsing line:/n"+line);
				}
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
