package topali.cluster.jobs.cml.parser;

import java.io.BufferedReader;
import java.io.FileReader;

public class Model8Parser extends CMLResultParser {

	@Override
	public void parse(String resultFile, String rstFile) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(resultFile));
			String line = null;
			while((line=in.readLine())!=null) {
				String a;
				String[] b;
				
				line = line.trim();
				
				if(line.startsWith("lnL(ntime:")) {
					a = line.substring(line.lastIndexOf(':')+1);
					a = a.trim();
					b = a.split("\\s+");
					lnl = Double.parseDouble(b[0]);
					continue;
				}
				
				if(line.startsWith("p0=")) {
					b = line.split("\\s+");
					p0 = Double.parseDouble(b[1]);
					p = Double.parseDouble(b[3]);
					q = Double.parseDouble(b[5]);
					continue;
				}
				
				if(line.startsWith("(p1=")) {
					b = line.split("\\s+");
					p1 = Double.parseDouble(b[1].replaceAll("\\)", ""));
					_w = Double.parseDouble(b[3]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
