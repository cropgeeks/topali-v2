package topali.cluster.jobs.cml.parser;

import java.io.BufferedReader;
import java.io.FileReader;

public class Model1aParser extends CMLResultParser {

	@Override
	public void parse(String resultFile, String rstFile) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(resultFile));
			String line = null;
			while((line=in.readLine())!=null) {
				String a;
				String[] b;
				
				line = line.trim();
				
				//TODO: dN/dS
				
				if(line.startsWith("lnL(ntime:")) {
					a = line.substring(line.lastIndexOf(':')+1);
					a = a.trim();
					b = a.split("\\s+");
					lnl = Double.parseDouble(b[0]);
					continue;
				}
				
				if(line.startsWith("p:")) {
					b = line.split("\\s+");
					p0 = Double.parseDouble(b[1]);
					p1 = Double.parseDouble(b[2]);
					continue;
				}
				
				if(line.startsWith("w:")) {
					b = line.split("\\s+");
					w0 = Double.parseDouble(b[1]);
					w1 = Double.parseDouble(b[2]);
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
}
