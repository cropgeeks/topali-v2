// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.modelgenerator;

import java.io.*;

import topali.cluster.*;
import topali.data.MGResult;

public class ModelGeneratorProcess extends StoppableProcess implements ProcessOutputParser
{

	private File wrkDir;
	private File pctDir;
	
	ModelGeneratorProcess(File wrkDir, MGResult result)
	{
		this.wrkDir = wrkDir;
		this.result = result;
		runCancelMonitor();
	}

	@Override
	public void run() throws Exception
	{
		pctDir = new File(wrkDir, "percent");
		
		MGResult result = (MGResult) this.result;

		//sbrn.commons.file.FileUtils.writeFile(new File(wrkDir, "wibble"), result.mgPath);
		ProcessBuilder pb = new ProcessBuilder(result.javaPath, "-jar", result.mgPath, "mg.fasta", "4");
	
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);

		proc = pb.start();

		PrintWriter writer = new PrintWriter(new OutputStreamWriter(proc
				.getOutputStream()));

		StreamCatcher sc = new StreamCatcher(proc.getInputStream(), false);
		sc.addParser(this);
		sc.start();
		
		writer.close();

		try
		{
			proc.waitFor();
		} catch (Exception e)
		{
			System.out.println(e);
			if (LocalJobs.isRunning(result.jobId) == false)
				throw new Exception("cancel");
		}
	}

	public void parseLine(String line)
	{
		try
		{
			String[] tmp = line.split("\\s+");
			double x = Double.parseDouble(tmp[2]);
			double total = Double.parseDouble(tmp[4].substring(0, tmp[4].length()-1));
			int percent = (int)(x/total*100);
			ClusterUtils.setPercent(pctDir, percent);
		} catch (Exception e)
		{
			//e.printStackTrace();
		}
	}
	
	
}
