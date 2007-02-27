// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml.parser;

import java.io.BufferedReader;
import java.io.FileReader;

import topali.data.CMLModel;

public class Model1aParser extends CMLResultParser
{

	public Model1aParser(CMLModel model)
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
						model.setLikelihood(Double.parseDouble(b[0]));
						continue;
					}

					if (line.startsWith("p:"))
					{
						b = line.split("\\s+");
						model.setP0(Double.parseDouble(b[1]));
						model.setP1(Double.parseDouble(b[2]));
						continue;
					}

					if (line.startsWith("w:"))
					{
						b = line.split("\\s+");
						model.setW0(Double.parseDouble(b[1]));
						model.setW1(Double.parseDouble(b[2]));
						continue;
					}

					if (model.getDnDS() == -1
							&& line.matches("\\d\\.\\.\\d(\\s+\\d+\\.\\d+){8}"))
					{
						b = line.split("\\s+");
						model.setDnDS(Double.parseDouble(b[4]));
					}
				} catch (RuntimeException e)
				{
					e.printStackTrace();
					System.err.println("Error parsing line:/n" + line);
				}

			}
			in.close();

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
