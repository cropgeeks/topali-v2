// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import topali.data.CMLModel;

public class Model3Parser extends CMLResultParser
{

	public Model3Parser(CMLModel model)
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
						model.likelihood = (Double.parseDouble(b[0]));
						continue;
					}

					if (line.startsWith("p:"))
					{
						b = line.split("\\s+");
						model.p0 = (Double.parseDouble(b[1]));
						model.p1 = (Double.parseDouble(b[2]));
						model.p2 = (Double.parseDouble(b[3]));
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
			StringBuffer pss = new StringBuffer();
			while ((line = in.readLine()) != null)
			{
				line = line.trim();
				Matcher m = p.matcher(line);
				if (m.matches() && start)
				{
					String[] tmp = line.split("\\s+");
					int pos = Integer.parseInt(tmp[0]);
					char aa = tmp[1].charAt(0);
					float prob = Float.parseFloat(tmp[4]);
					pss.append(pos);
					pss.append('|');
					pss.append(aa);
					pss.append('|');
					pss.append(prob);
					pss.append(' ');
				}
				if (line.startsWith("Naive Empirical Bayes (NEB)"))
					start = true;
				else if (line.startsWith("Positively selected sites"))
					start = false;
			}
			model.pss = (pss.toString());
			in.close();

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
