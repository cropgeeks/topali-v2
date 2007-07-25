// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.fastml;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

import topali.cluster.*;
import topali.data.FastMLResult;

public class FastMLProcess extends StoppableProcess
{

	Logger log = Logger.getLogger(this.getClass());
	
	private File wrkDir;
	
	FastMLProcess(File wrkDir, FastMLResult result)
	{
		this.wrkDir = wrkDir;
		this.result = result;
		runCancelMonitor();
	}

	void run() throws Exception
	{
		FastMLResult result = (FastMLResult) this.result;
		
		LinkedList<String> cmdList = new LinkedList<String>();
		cmdList.add(result.fastmlPath);
		if(result.gamma)
			cmdList.add("-g");
		cmdList.add("-b");
		if(result.model!=null && !result.model.equals("mn"))
			cmdList.add("-"+result.model);
		cmdList.add("-s");
		cmdList.add("seq.fasta");
		cmdList.add("-t");
		cmdList.add("tree.txt");
		
		String[] cmds = new String[cmdList.size()];
		cmds = cmdList.toArray(cmds);
		
		String tmp = "";
		for(String s : cmds)
			tmp += s+" ";
		log.info("Running FastML with: "+tmp);
		
		//sbrn.commons.file.FileUtils.writeFile(new File(wrkDir, "wibble"), result.mgPath);
		ProcessBuilder pb = new ProcessBuilder(cmds);
	
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);

		proc = pb.start();

		PrintWriter writer = new PrintWriter(new OutputStreamWriter(proc
				.getOutputStream()));

		StreamCatcher sc = new StreamCatcher(proc.getInputStream(), false);
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
	
}
