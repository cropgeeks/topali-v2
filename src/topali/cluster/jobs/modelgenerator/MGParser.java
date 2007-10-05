// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.modelgenerator;

import java.io.*;

import topali.data.*;

public class MGParser
{

	public static MGResult parse(File file, MGResult models) {
		models.models.clear();
		
		BufferedReader reader = null;
		
		String bestLRT = null;
		String bestAIC1 = null;
		String bestAIC2 = null;
		String bestBIC = null;
		
		String curSection = "";
		
		try
		{
			int start = 0;
			
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line=reader.readLine())!=null)
			{

				if(line.trim().equals(""))
					continue;
				
				if(line.startsWith("****Hierarchical")) {
					curSection = "lrt";
					continue;
				}
				if(line.startsWith("****Akaike Information Criterion 1")) {
					curSection = "aic1";
					continue;
				}
				if(line.startsWith("****Akaike Information Criterion 2")) {
					curSection = "aic2";
					continue;
				}
				if(line.startsWith("****Bayesian")) {
					curSection = "bic";
					continue;
				}
				
				if(curSection.equals("lrt") && line.startsWith("Model Selected:")) {
					String[] tmp = line.split("\\s+");
					bestLRT = tmp[2];
					continue;
				}
				
				if(curSection.equals("aic1") && line.startsWith("Model Selected:")) {
					String[] tmp = line.split("\\s+");
					bestAIC1 = tmp[2];
					continue;
				}
				
				if(curSection.equals("aic2") && line.startsWith("Model Selected:")) {
					String[] tmp = line.split("\\s+");
					bestAIC2 = tmp[2];
					continue;
				}
				
				if(curSection.equals("bic") && line.startsWith("Model Selected:")) {
					String[] tmp = line.split("\\s+");
					bestBIC = tmp[2];
					continue;
				}
				
				if(line.startsWith("---")) {
					start++;
					continue;
				}
				
				if(start==2) {
					String[] tmp = line.split("\\s+");
					if(tmp.length<10)
						continue;
					double aic1 = Double.parseDouble(tmp[1]);
					double aic2 = Double.parseDouble(tmp[4]);
					double bic = Double.parseDouble(tmp[7]);
					double lnl1 = Double.parseDouble(tmp[3]);
					double lnl2 = Double.parseDouble(tmp[6]);
					double lnl3 = Double.parseDouble(tmp[9]);
					String s1 = tmp[2];
					String s2 = tmp[5];
					String s3 = tmp[8];
					
					SubstitutionModel mod = models.getModel(s1);
					if(mod==null) {
						mod = new SubstitutionModel();
						mod.setName(s1);
						models.models.add(mod);
					}
					mod.setAic1(aic1);
					mod.setLnl(lnl1);
					
					mod = models.getModel(s2);
					if(mod==null) {
						mod = new SubstitutionModel();
						mod.setName(s2);
						models.models.add(mod);
					}
					mod.setAic2(aic2);
					mod.setLnl(lnl2);

					mod = models.getModel(s3);
					if(mod==null) {
						mod = new SubstitutionModel();
						mod.setName(s3);
						models.models.add(mod);
					}
					mod.setBic(bic);
					mod.setLnl(lnl3);
				}
			}
			
			for(SubstitutionModel m : models.models) {
				String name = m.getName();
				if(name.equals(bestLRT))
					name += " (hLRT)";
				else if(name.equals(bestAIC1))
					name += " (AIC1)";
				else if(name.equals(bestAIC2))
					name += " (AIC2)";
				else if(name.equals(bestBIC))
					name += " (BIC)";
				m.setName(name);
			}
			
			
		} catch (Exception e)
		{
			e.printStackTrace();
		} 
		finally {
			try
			{
				reader.close();
			} catch (IOException e)
			{
			}
		}
		
		return models;
	}
	
}
