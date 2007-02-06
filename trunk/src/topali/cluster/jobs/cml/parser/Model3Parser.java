package topali.cluster.jobs.cml.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Model3Parser extends CMLResultParser {

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
					p2 = Double.parseDouble(b[3]);
					continue;
				}
				
				if(line.startsWith("w:")) {
					b = line.split("\\s+");
					w0 = Double.parseDouble(b[1]);
					w1 = Double.parseDouble(b[2]);
					w2 = Double.parseDouble(b[3]);
				}
				
			}
			
			//parse rst file
			in = new BufferedReader(new FileReader(rstFile));
			line = null;
			Pattern p = Pattern.compile("\\d+ \\w .+");
			while((line=in.readLine())!=null) {
				line = line.trim();
				Matcher m = p.matcher(line);
				if(m.matches()) {
					String[] tmp = line.split("\\s+");
					pss.add(tmp[0]+" "+tmp[1]+" "+tmp[2]);
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
