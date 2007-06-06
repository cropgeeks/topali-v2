// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.mrbayes;

import java.io.*;

import topali.cluster.*;
import topali.data.MBTreeResult;

public class MrBayesProcess extends StoppableProcess implements ProcessOutputParser
{

	private File wrkDir;
	private File pctDir;
	int totalGen;
	
	boolean parse = false;
	
	MrBayesProcess(File wrkDir, MBTreeResult result, int totalGen)
	{
		this.wrkDir = wrkDir;
		this.result = result;
		this.totalGen = totalGen;
		
		runCancelMonitor();
	}

	void run() throws Exception
	{
		pctDir = new File(wrkDir, "percent");
		
		MBTreeResult mbResult = (MBTreeResult) result;

		ProcessBuilder pb = new ProcessBuilder(mbResult.mbPath, "mb.nex");
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);

		proc = pb.start();

		PrintWriter writer = new PrintWriter(new OutputStreamWriter(proc
				.getOutputStream()));

		StreamCatcher sc = new StreamCatcher(proc.getInputStream(), false);
		sc.addParser(this);
		sc.start();
		
		// writer.println("Y");
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
			}
		}
	}
	
	
}
