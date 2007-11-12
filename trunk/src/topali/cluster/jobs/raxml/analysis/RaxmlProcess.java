// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.raxml.analysis;

import java.io.File;

import topali.cluster.*;
import topali.data.*;
import topali.data.models.*;
import topali.gui.TOPALi;

public class RaxmlProcess extends StoppableProcess
{

	private File wrkDir;

	RaxmlProcess(File wrkDir, RaxmlResult result)
	{
		this.result = result;
		this.wrkDir = wrkDir;
		runCancelMonitor();
	}

	@Override
	public void run() throws Exception
	{
		RaxmlResult res = (RaxmlResult)result;
		RaxPartition p = res.partitions.get(res.partitions.size()-1);
		
		String[] cmds = new String[] {res.raxmlPath, "-f", "d", "-m", getModel(res, p), "-q", "partitions", "-s", "seq", "-n", "out.txt"};
		
		if(TOPALi.debugJobs) {
			StringBuffer tmp = new StringBuffer();
			for(String s : cmds)
				tmp.append(s+" ");
			ClusterUtils.writeFile(new File(wrkDir, "cmds.txt"), tmp.toString());
		}
		
		ProcessBuilder pb = new ProcessBuilder(cmds);

		pb.directory(wrkDir);
		pb.redirectErrorStream(true);

		proc = pb.start();
		StreamCatcher sc = new StreamCatcher(proc.getInputStream(), false);
		sc.start();

		try
		{
			proc.waitFor();
			proc.destroy();
		} catch (Exception e)
		{
			System.out.println(e);
		}

		(new File(wrkDir, "finished")).createNewFile();
	}
	
	private String getModel(RaxmlResult res, RaxPartition p) {
		if(p.dna) {
			String model = "GTR"+res.rateHet;
			return model;
		}
		else {
			String model = "PROT"+res.rateHet;
			model += p.model.toUpperCase();
			if(p.model.toUpperCase().equals("BLOSUM"))
				model += "62";
			if(res.empFreq) {
				model+="F";
			}
			return model;
		}
	}
}
