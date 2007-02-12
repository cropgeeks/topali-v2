// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.pdm2;

import java.io.File;

import topali.cluster.ClusterUtils;
import topali.cluster.LocalJobs;
import topali.data.*;
import topali.fileio.Castor;
import topali.mod.Filters;

public class PDMInitializer extends Thread
{
	private SequenceSet ss;

	private PDM2Result result;

	// Directory where the job will run
	private File jobDir;

	// Holds sequence indices (as not every sequences may be processed)
	private int[] indices;

	// Holds a list of windows that form the basic of the PDM job
	private RegionAnnotations.Region[] regions;

	public PDMInitializer(File jobDir, SequenceSet ss, PDM2Result result)
	{
		this.jobDir = jobDir;
		this.ss = ss;
		this.result = result;
	}

	public void run()
	{
		try
		{
			startThreads();
		} catch (Exception e)
		{
			ClusterUtils.writeError(new File(jobDir, "error.txt"), e);
		}
	}

	private void startThreads() throws Exception
	{
		// Ensure the directory for this job exists
		jobDir.mkdirs();

		// Store the PDM2Result object where it can be read by the sub-job
		Castor.saveXML(result, new File(jobDir, "submit.xml"));

		// Run the analysis
		runAnalysis();
	}

	/*
	 * private void runAnalysis() throws Exception { ThreadManager manager = new
	 * ThreadManager();
	 *  // Sequences that should be selected/saved for processing int[] indices =
	 * ss.getIndicesFromNames(result.selectedSeqs);
	 *  // Number of processors/jobs/slots/whatever available for use int
	 * nodeCount = manager.getMaxTokens(); if (result.isRemote) nodeCount =
	 * result.nProcessors;
	 *  // Before the job can be started (on the cluster) we need to break it //
	 * down into a set of groups, where each group contains [n] windows
	 * WindowChopperUpper wcu = new WindowChopperUpper(ss, result);
	 * RegionAnnotations.Region[] regions = wcu.getRegions(nodeCount);
	 * 
	 * System.out.println("Split alignment into " + regions.length + "
	 * regions");
	 * 
	 * int i = 1; for (RegionAnnotations.Region r: regions) {
	 * System.out.println("WINDOW: " + i);
	 *  // For each region, we now save its data to a sub folder run[n] File
	 * runDir = new File(jobDir, "run" + (i++)); runDir.mkdirs();
	 *  // TODO: remove at some point (not needed) new File(runDir, "region-" +
	 * r.getS() + "-" + r.getE()).createNewFile(); ////////
	 * 
	 * File seqFile = new File(runDir, "pdm.fasta"); ss.save(seqFile, indices,
	 * r.getS(), r.getE(), Filters.FAS, true);
	 *  // Start task locally... if (result.isRemote == false) { PDMAnalysis
	 * analysis = new PDMAnalysis(runDir); analysis.startThread(manager); } }
	 *  // Or on the cluster... if (result.isRemote)
	 * PDMWebService.runScript(jobDir, regions.length); }
	 */

	private void runAnalysis() throws Exception
	{
		// Sequences that should be selected/saved for processing
		indices = ss.getIndicesFromNames(result.selectedSeqs);

		// Number of processors/jobs/slots/whatever available for use
		int nodeCount = LocalJobs.manager.getMaxTokens();
		if (result.isRemote)
			nodeCount = result.nProcessors;

		// Before the job can be started (on the cluster) we need to break it
		// up into its windows, then split them between nodes
		// This code used to write the region data as a mini-alignment and the
		// analysis would then work out the windows, but this method is easier
		// to deal with when adjusting windows for variable site information
		WindowChopperUpper wcu = new WindowChopperUpper(ss, result);
		regions = wcu.getWindows();

		System.out.println("NoWindows=" + regions.length);

		// Number of windows per node (best case)
		int wN = (int) ((regions.length / nodeCount) + 1);
		// Number of nodes we actually end up using
		int nodes = (int) ((regions.length / wN) + 1);
		// Check for special case where integer rounddown goes tits up
		// (ie 39 nodes but all data written in 38)
		if (wN * (nodes - 1) == regions.length)
			nodes -= 1;

		for (int i = 0; i < nodes; i++)
		{
			int sIndex = (i * wN);
			int eIndex = (i * wN) + wN; // (no -1 because we want an extra
										// overlapping window)

			writeWindows(i + 1, sIndex, eIndex);
		}

		// Submit job to the cluster...
		if (result.isRemote)
			PDMWebService.runScript(jobDir, nodes + 1);

		// Or start post processing task locally (after other windows complete)
		else
		{
			File file = new File(new File(jobDir, "nodes"), "runX");
			new PDMAnalysis(file).start(LocalJobs.manager);
		}

	}

	private void writeWindows(int nodeIndex, int s, int e) throws Exception
	{
		File runDir = new File(new File(jobDir, "nodes"), "run" + nodeIndex);
		runDir.mkdirs();

		// Write the full alignment too, just in case...
		File seqFile = new File(runDir, "pdm.fasta");
		ss.save(seqFile, indices, Filters.FAS, true);

		for (int i = s, win = 1; i <= e; i++, win++)
		{
			// (There will be no "extra" window for the last node's data)
			if (i >= regions.length)
				continue;

			RegionAnnotations.Region r = regions[i];

			seqFile = new File(runDir, "win" + win + ".nex");
			ss.save(seqFile, indices, r.getS(), r.getE(), Filters.NEX_B, true);
		}

		// Start this window running locally
		if (result.isRemote == false)
		{
			PDMAnalysis analysis = new PDMAnalysis(runDir);
			analysis.start(LocalJobs.manager);
		}
	}
}