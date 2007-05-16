// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.modelgenerator;

import java.io.*;

import topali.data.MGResult;
import topali.data.SubstitutionModel;

public class MGParser
{

	public static MGResult parse(File file, MGResult models) {
		models.models.clear();
		
		BufferedReader reader = null;
		
		try
		{
			int start = 0;
			
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line=reader.readLine())!=null)
			{
				if(line.trim().equals(""))
					continue;
				
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
					
					String[] tmp2 = s1.split("\\+");
					String name = tmp2[0];
					boolean i = s1.contains("+I");
					boolean g = s1.contains("+G");
					boolean f = s1.contains("+F");
					SubstitutionModel mod = models.getModel(name, i, g, f);
					if(mod==null) {
						mod = new SubstitutionModel();
						mod.setName(name);
						mod.setI(i);
						mod.setG(g);
						mod.setF(f);
						models.models.add(mod);
					}
					mod.setAic1(aic1);
					mod.setLnl(lnl1);
					
					tmp2 = s2.split("\\+");
					name = tmp2[0];
					i = s2.contains("+I");
					g = s2.contains("+G");
					f = s2.contains("+F");
					
					mod = models.getModel(name, i, g, f);
					if(mod==null) {
						mod = new SubstitutionModel();
						mod.setName(name);
						mod.setI(i);
						mod.setG(g);
						mod.setF(f);
						models.models.add(mod);
					}
					mod.setAic2(aic2);
					mod.setLnl(lnl2);
					
					tmp2 = s3.split("\\+");
					name = tmp2[0];
					i = s3.contains("+I");
					g = s3.contains("+G");
					f = s3.contains("+F");
					
					mod = models.getModel(name, i, g, f);
					if(mod==null) {
						mod = new SubstitutionModel();
						mod.setName(name);
						mod.setI(i);
						mod.setG(g);
						mod.setF(f);
						models.models.add(mod);
					}
					mod.setBic(bic);
					mod.setLnl(lnl3);
				}
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
