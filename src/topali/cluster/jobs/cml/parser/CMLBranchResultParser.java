// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml.parser;

import java.io.*;

import topali.data.CMLHypothesis;

public class CMLBranchResultParser
{

	CMLHypothesis hypo;
	
	public CMLBranchResultParser(CMLHypothesis hypo) {
		this.hypo = hypo;
	}
	
	public void parse(File file) {
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line = null;
			
			boolean tree = false;
			
			while ((line = in.readLine()) != null)
			{
				line = line.trim();
				
				if (line.startsWith("lnL(ntime:"))
				{
					String a = line.substring(line.lastIndexOf(':') + 1);
					a = a.trim();
					String[] b = a.split("\\s+");
					hypo.likelihood = Double.parseDouble(b[0]);
					continue;
				}
				
				if(line.startsWith("w ratios as labels")) {
					tree = true;
					continue;
				}
				
				if(line.startsWith("omega (dN/dS)")) {
					double[] omegas = new double[1];
					String[] b = line.split("\\s+");
					omegas[0] = Double.parseDouble(b[3]);
					hypo.omegas = omegas;
				}
				
				if(tree) {
					hypo.omegaTree = line;
					parseOmegaTree(line);
					tree = false;
					continue;
				}
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void parseOmegaTree(String line) {
		//(((((X04752Mus#1 #0.2503 : 0.023998, U07177Rat#1 #0.2503 : 0.037592) #0.0976 : 0.030078, AF070995C#1 #0.2503 : 0.033726) #0.0976 : 0.026557, (U95378Sus#1 #0.2503 : 0.027655, U13680Hom#1 #0.2503 : 0.030732)#1 #0.2503 : 0.028495)#1 #0.2503 : 0.103484, (((U07178Sus #0.0976 : 0.014142, X02152Hom #0.0976 : 0.016169) #0.0976 : 0.005587, M22585rab #0.0976 : 0.025076) #0.0976 : 0.001822, (U13687Mus #0.0976 : 0.009844, NM017025R #0.0976 : 0.012971) #0.0976 : 0.024224) #0.0976 : 0.015323) #0.0976 : 0.032405, (X53828OG1#2 #0.0828 : 0.025952, U28410OG2#2 #0.0828 : 0.032492)#2 #0.0828 : 0.000000);
		
		String[] split = line.split("\\s+");
		
		//Detect how much categories we have
		int max = 1;
		for(String s : split) {
			if(s.matches("\\w*#\\d+")) {
				int i = s.indexOf('#');
				int n = Integer.parseInt(s.substring(i+1));
				n += 1;
				if(n>max)
					max = n;
			}
		}
		
		double[] omegas = new double[max];
		
		for(int i=0; i<split.length; i++) {
			String s = split[i];
			if(s.matches("#\\d+\\.\\d+")) {
				double w = Double.parseDouble(s.substring(1));
				String s2 = split[i-1];
				if(s2.matches("\\w*#\\d")) {
					int index = s2.indexOf('#');
					int pos = Integer.parseInt(s2.substring(index+1));
					omegas[pos] = w;
				}
				else { 
					omegas[0] = w;
				}
			}
		}
		
		hypo.omegas = omegas;
	}
}
