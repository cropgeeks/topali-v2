// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs;

import java.io.File;

import org.apache.log4j.*;

import topali.cluster.*;
import topali.cluster.jobs.fastml.RunFastML;
import topali.data.*;
import topali.fileio.Castor;
import topali.gui.Prefs;

public class FastMLLocalJob extends AnalysisJob
{
	Logger log = Logger.getLogger(this.getClass());
	
	private SequenceSet ss;

	private File jobDir;

	public FastMLLocalJob(FastMLResult result, AlignmentData data)
	{
		this.result = result;    
		this.data = data;
		this.ss = data.getSequenceSet();
		result.startTime = System.currentTimeMillis();
		result.jobId = "" + System.currentTimeMillis();
		result.tmpDir = Prefs.tmpDir.getPath();

		jobDir = new File(Prefs.tmpDir, result.jobId);

		LocalJobs.addJob(result.jobId);
	}

	public String ws_submitJob() throws Exception
	{
		try
		{
			new RunFastML(jobDir, ss, (FastMLResult) result).start();

			result.status = JobStatus.RUNNING;
			return result.jobId;
		} catch (Exception e)
		{
			System.out.println(e);
			result.status = JobStatus.FATAL_ERROR;
			throw e;
		}
	}

	public JobStatus ws_getProgress() throws Exception
	{
		if (new File(jobDir, "error.txt").exists())
		{
			log.log(Level.ERROR, "error.txt found");
			throw new Exception("FastML error.txt");
		}
		
		if(new File(jobDir, "result.xml").exists())
			return new JobStatus(100, 0, "_status");
		else
			return new JobStatus(0, 0, "_status");
	}

	public AnalysisResult ws_downloadResult() throws Exception
	{
		result = (FastMLResult) Castor.unmarshall(new File(jobDir, "result.xml"));
		return result;
	}

	public void ws_cleanup() throws Exception
	{
		ClusterUtils.emptyDirectory(jobDir, true);
		result.status = JobStatus.COMPLETED;

		LocalJobs.delJob(result.jobId);
	}

	public void ws_cancelJob()
	{
		LocalJobs.cancelJob(result.jobId);
	}

}
