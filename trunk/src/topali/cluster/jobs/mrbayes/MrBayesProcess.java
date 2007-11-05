// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.mrbayes;

import java.io.*;
import java.util.logging.Logger;

import topali.cluster.*;
import topali.data.MBTreeResult;

public class MrBayesProcess extends StoppableProcess implements ProcessOutputParser
{
	private File wrkDir;
	private File pctDir;
	int totalGen;

	MrBayesProcess(File wrkDir, MBTreeResult result)
	{
		this.wrkDir = wrkDir;
		this.result = result;
		this.totalGen = result.nGen;
		
		runCancelMonitor();
	}

	@Override
	public void run() throws Exception
	{
		pctDir = new File(wrkDir, "percent");
		
		MBTreeResult mbResult = (MBTreeResult) result;

		ProcessBuilder pb = new ProcessBuilder(mbResult.mbPath, "mb.nex");
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);

		proc = pb.start();

		PrintWriter writer = new PrintWriter(new OutputStreamWriter(proc
				.getOutputStream()));

		StreamCatcher sc = new StreamCatcher(proc.getInputStream(), false, true);
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
		
		try
		{
			// wait until sc has finished (and all data was written to its StringBuffer)
			sc.join();
		} catch (RuntimeException e)
		{
			//if sc has already finished, its ok...
		}
		
		File mbout = new File(wrkDir, "mb.out");
		BufferedWriter out = new BufferedWriter(new FileWriter(mbout));
		out.write(sc.getOutput());
		out.flush();
		out.close();
	}
	
	boolean parse = false;
	public void parseLine(String line)
	{
		line = line.trim();
		String tmp[] = line.split("\\s+");
		if(tmp[0].equals("Chain"))
			parse = true;
		else if(tmp[0].equals("Analysis"))
			parse = false;
		
		if(parse) {
			try
			{
				int gen = Integer.parseInt(tmp[0]);
				int percent = (int)((double)gen/(double)totalGen*100);
				ClusterUtils.setPercent(pctDir, percent);
			} catch (Exception e)
			{
				//Ignore NumberFormatExceptions
			}
		}
	}
	
}
