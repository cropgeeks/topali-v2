// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.modeltest.analysis;

import java.io.*;

public class ModelTestParser
{
	File file;
	double lnl;
	double[] subRates;
	double[] baseFreq;
	double inv;
	int gammaCat;
	double gamma;
	
	public ModelTestParser(File f) throws Exception {
		this.file = f;
		subRates = new double[6];
		baseFreq = new double[4];
		parse();
	}
	
	public double getLnl()
	{
		return lnl;
	}


	public double[] getSubRates()
	{
		return subRates;
	}


	public double[] getBaseFreq()
	{
		return baseFreq;
	}

	public double getInv()
	{
		return inv;
	}

	public int getGammaCat()
	{
		return gammaCat;
	}

	public double getGamma()
	{
		return gamma;
	}

	private void parse() throws Exception{
		BufferedReader in = new BufferedReader(new FileReader(file));
		
		String line = null;
		boolean bf = false;
		boolean sr = false;
		int i = 0;
		
		while((line=in.readLine())!=null) {
			String[] tmp = line.trim().split("\\s+");
			if(tmp.length<2)
				continue;
			
			if(tmp[1].equals("Likelihood")) {
				this.lnl = Double.parseDouble(tmp[5]);
				continue;
			}
			
			if(tmp[1].equals("Proportion")) {
				this.inv = Double.parseDouble(tmp[5]);
				continue;
			}
			
			if(tmp[1].equals("Number") && tmp[3].equals("categories")) {
				this.gammaCat = Integer.parseInt(tmp[5]);
				continue;
			}
			
			if(tmp[1].equals("Gamma")) {
				this.gamma = Double.parseDouble(tmp[5]);
				continue;
			}
			
			if(tmp[1].equals("Nucleotides")) {
				bf = true;
				continue;
			}
			
			if(tmp[1].equals("GTR")) {
				sr = true;
				bf = false;
				i = 0;
				continue;
			}
			
			if(bf) {
				baseFreq[i] = Double.parseDouble(tmp[2]);
				i++;
			}
			
			if(sr) {
				subRates[i] = Double.parseDouble(tmp[3].replace("(fixed)", ""));
				i++;
				if(i>5)
					sr=false;
			}
		}
	}
}
