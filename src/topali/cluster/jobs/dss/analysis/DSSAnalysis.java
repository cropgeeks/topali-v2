// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.dss.analysis;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

import pal.alignment.SimpleAlignment;
import topali.cluster.*;
import topali.data.*;
import topali.fileio.Castor;
import topali.var.utils.Utils;

public class DSSAnalysis extends AnalysisThread {
    Logger log = Logger.getLogger(this.getClass());

    private SequenceSet ss;

    private DSSResult result;

    // Maximum DSS value found
    double maximum;

    // List for storing the x,y graph data
    ArrayList<Data> data = new ArrayList<Data>();
    
    // List holding all single dss windows
    ArrayList<DSS> dssWindows = new ArrayList<DSS>();

    private File pctDir;

    // If running on the cluster, the subjob will be started within its own JVM
    public static void main(String[] args) {
	new DSSAnalysis(new File(args[0])).run();
    }

    // If running locally, the job will be started via a normal constructor call
    public DSSAnalysis(File runDir) {
	super(runDir);
    }

    
    public void runAnalysis() throws Exception {
	// Read the DSSResult
	File jobDir = runDir.getParentFile();
	File resultFile = new File(jobDir, "submit.xml");
	result = (DSSResult) Castor.unmarshall(resultFile);
	// Read the SequenceSet
	ss = new SequenceSet(new File(runDir, "dss.fasta"));

	// Percent directory
	pctDir = new File(runDir, "percent");

	// Temporary working directory
	wrkDir = ClusterUtils.getWorkingDirectory(result, jobDir.getName(),
		runDir.getName());

	int window = result.window;
	int step = result.step;
	
	if (result.type == DSSResult.TYPE_VARIABLE) {
	    int[] varSites = getVariableSites(ss);
	    
	    for (int i = 0, j=0; (i + window) < varSites.length; i += step, j++) {
		int wS;
		if (i == 0) //first window always starts at 0
		    wS = 0;
		else
		    wS = (varSites[i] + varSites[i - 1]) / 2; //otherwise start lies in the middle of this variable site and the previous one

		int wE;
		if (i + window + 1 < varSites.length) //if it's not the last window, the end lies in the middle of this variable site and the next one
		    wE = (varSites[i + window] + varSites[i + window + 1]) / 2;
		else
		    wE = varSites[i + window];

		int w1S = wS+1; // add +1 to everything because ss.getAlignment() expects 1,2,... coordinates
		//int w1E = (wS + wE) / 2 +1;
		int w1E = varSites[i+(window/2)];
		int w2S = w1E + 2;
		int w2E = wE + 1;

		//Add a 0 data point 
		data.add(new Data(w1E, 0));

		SimpleAlignment win1 = ss.getAlignment(w1S, w1E, true);
		SimpleAlignment win2 = ss.getAlignment(w2S, w2E, true);

		File dssWrkDir = new File(wrkDir, "win" + (j + 1));
		dssWindows.add(new DSS(dssWrkDir, result, win1, win2,
			result.gapThreshold));
	    }
	} else {
	    int windowCount = ((ss.getLength() - window) / step) + 1;
	    int firstWinPos = (int) (1 + (window / 2f - 0.5));
	    int pos = firstWinPos;
	    int w = 1;
	    for (int i = 0; i < windowCount; i++, pos += step, w += step) {
		if (LocalJobs.isRunning(result.jobId) == false)
		    throw new Exception("cancel");

		// Set position for each data point
		data.add(new Data(pos, 0));

		// 1st half window
		final int win1S = w;
		final int win1E = w + (window / 2) - 1;

		// 2nd half window
		final int win2S = w + (window / 2);
		final int win2E = w + window - 1;

		// Strip out the two partitions
		SimpleAlignment win1 = ss.getAlignment(win1S, win1E, true);
		SimpleAlignment win2 = ss.getAlignment(win2S, win2E, true);

		File dssWrkDir = new File(wrkDir, "win" + (i + 1));
		dssWindows.add(new DSS(dssWrkDir, result, win1, win2,
			result.gapThreshold));
	    }
	}

	// 2) Run all the Fitch calculations
	for (int i = 0; i < data.size(); i++) {
	    if (LocalJobs.isRunning(result.jobId) == false)
		throw new Exception("cancel");

	    dssWindows.get(i).calculateFitchScores();

	    // Should reach 50% by the end of the loop
	    int percent = (int) (((i / (float) data.size()) * 100) / 2.0f);
	    ClusterUtils.setPercent(pctDir, percent);
	    // System.out.println("percent="+percent);
	}
	RunFitch fitch = new RunFitch(result);
	fitch.runFitchScripts(wrkDir, data.size());

	// 3) Perform actual DSS calculations (using Fitch results)
	for (int i = 0; i < data.size(); i++) {
	    if (LocalJobs.isRunning(result.jobId) == false)
		throw new Exception("cancel");

	    // Work out the DSS statistic
	    data.get(i).y = dssWindows.get(i).calculateDSS();

	    // Is it bigger than the current maximum?
	    if (data.get(i).y > maximum)
		maximum = data.get(i).y;

	    // Should reach 100% by the end of the loop
	    int percent = (int) (((i / (float) data.size()) * 100) / 2.0f) + 50;
	    ClusterUtils.setPercent(pctDir, percent);
	    // System.out.println("percent="+percent);
	}

	writeResults();

	ClusterUtils.setPercent(pctDir, 105);

	ClusterUtils.emptyDirectory(wrkDir, true);
    }

    private void writeResults() {
	BufferedWriter out = null;
	try {
	    out = new BufferedWriter(
		    new FileWriter(new File(runDir, "out.xls")));
	    out.write("" + data.size() + "\t" + maximum);
	    out.newLine();
	    out.newLine();

	    for (int i = 0; i < data.size(); i++) {
		out.write(data.get(i).x + "\t" + data.get(i).y);
		out.newLine();
	    }
	} catch (IOException e) {
	    System.out.println(e);
	}

	try {
	    if (out != null)
		out.close();
	} catch (Exception e) {
	}
    }

    /**
     * Gets an array holding the positions of all variable sites in an alignment
     * @param ss
     * @return
     */
    private int[] getVariableSites(SequenceSet ss) {
	ArrayList<Integer> list = new ArrayList<Integer>();
	for (int i = 0; i < ss.getLength(); i++) {
	    char c = ss.getSequence(0).getBuffer().charAt(i);

	    ListIterator<Sequence> itor = ss.getSequences().listIterator(1);
	    while (itor.hasNext()) {
		if (itor.next().getBuffer().charAt(i) != c) {
		    list.add(i);
		    break;
		}
	    }
	}

	try {
	    int[] result = (int[]) Utils.castArray(list.toArray(), int.class);
	    return result;
	} catch (Exception e) {
	    log.warn(e);
	    return null;
	}
    }

    /**
     * Just holds a pair of x,y data
     */
    class Data {
	public double x;
	public double y;

	public Data(double x, double y) {
	    this.x = x;
	    this.y = y;
	}
    }
}