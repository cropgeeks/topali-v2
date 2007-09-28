// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml.parser;

import java.io.*;
import java.util.regex.*;

import topali.data.*;

public class Model2aParser extends CMLResultParser
{

	public Model2aParser(CMLModel model)
	{
		super(model);
	}
	
	@Override
	public void parse(String resultFile, String rstFile)
	{
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(resultFile));
			String line = null;
			while ((line = in.readLine()) != null)
			{
				String a;
				String[] b;

				line = line.trim();

				try
				{
					if (line.startsWith("lnL(ntime:"))
					{
						a = line.substring(line.lastIndexOf(':') + 1);
						a = a.trim();
						b = a.split("\\s+");
						model.likelihood = Double.parseDouble(b[0]);
						continue;
					}

					if (line.startsWith("p:"))
					{
						b = line.split("\\s+");
						model.p0 = (Double.parseDouble(b[1]));
						model.p1 = (Double.parseDouble(b[2]));
						model.p1 = (Double.parseDouble(b[3]));
						continue;
					}

					if (line.startsWith("w:"))
					{
						b = line.split("\\s+");
						model.w0 = (Double.parseDouble(b[1]));
						model.w1 = (Double.parseDouble(b[2]));
						model.w2 = (Double.parseDouble(b[3]));
						continue;
					}

				} catch (RuntimeException e)
				{
					e.printStackTrace();
					System.err.println("Error parsing line:/n" + line);
				}

			}
			in.close();

			// parse rst file
			in = new BufferedReader(new FileReader(rstFile));
			line = null;
			Pattern p = Pattern.compile("\\d+ \\D .+");
			boolean start = false;
			while ((line = in.readLine()) != null)
			{
				line = line.trim();
				Matcher m = p.matcher(line);
				if (m.matches() && start)
				{
					String[] tmp = line.split("\\s+");
					int pos = Integer.parseInt(tmp[0]);
					pos = pos*3-1; //transform the aa position into a nuc. position
					char aa = tmp[1].charAt(0);
					float prob = Float.parseFloat(tmp[4]);
					PSSite pss = new PSSite(pos, aa, prob);
					model.pssList.add(pss);
					
				}
				if (line.startsWith("Naive Empirical Bayes (NEB)"))
					start = true;
				else if (line.startsWith("Positively selected sites"))
					start = false;
			}
			in.close();

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
