// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.codonw;

import java.io.*;

import topali.cluster.*;
import topali.data.*;

public class CodonWProcess extends StoppableProcess
{

	private File wrkDir;
	
	CodonWProcess(File wrkDir, CodonWResult result)
	{
		this.wrkDir = wrkDir;
		this.result = result;

		runCancelMonitor();
	}

	void run() throws Exception
	{
		CodonWResult result = (CodonWResult) this.result;
		
		ProcessBuilder pb = new ProcessBuilder(result.codonwPath, "codonw.fasta", "-code", ""+getGeneticCode(result.geneticCode), "-enc", "-gc3s", "-gc", "-sil_base", "-nomenu", "-silent");
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
	
	private int getGeneticCode(String code) {
		if(code.equals(SequenceSetParams.GENETICCODE_UNIVERSAL)) {
			return 0;
		}
		else if(code.equals(SequenceSetParams.GENETICCODE_CILIATES)) {
			return 5;
		}
		else if(code.equals(SequenceSetParams.GENETICCODE_VERTMT)) {
			return 1;
		}
		else if(code.equals(SequenceSetParams.GENETICCODE_YEAST)) {
			return 2;
		}
		
		return 0;
	}
}
