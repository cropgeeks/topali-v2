// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.lrt.analysis;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

import pal.alignment.SimpleAlignment;
import topali.cluster.*;
import topali.cluster.jobs.dss.analysis.DSS;
import topali.data.*;
import topali.fileio.Castor;
import topali.var.utils.Utils;

public class LRTAnalysis extends AnalysisThread {
    Logger log = Logger.getLogger(this.getClass());

    private SequenceSet ss;

    private LRTResult result;

    // Array of LRT scores for each window position
    ArrayList<Data> data = new ArrayList<Data>();

    // Maximum LRT value found
    double maximum;

    private File pctDir;

    // If running on the cluster, the subjob will be started within its own
    // JVM
    public static void main(String[] args) {
	new LRTAnalysis(new File(args[0])).run();
    }

    // If running locally, the job will be started via a normal constructor
    // call
    public LRTAnalysis(File runDir) {
	super(runDir);
    }

    @Override
	public void runAnalysis() throws Exception
	{
		// Read the LRTResult
		File resultFile = new File(runDir.getParentFile(), "submit.xml");
		result = (LRTResult) Castor.unmarshall(resultFile);
		// Read the SequenceSet
		ss = new SequenceSet(new File(runDir, "lrt.fasta"));

		// Temporary working directory
		wrkDir = ClusterUtils.getWorkingDirectory(result, runDir
				.getParentFile().getName(), runDir.getName());

		// Percent directory
		pctDir = new File(runDir, "percent");
		int window = result.window;
		int step = result.step;
		
		if(result.type==LRTResult.TYPE_VARIABLE) {
		    int[] varSites = getVariableSites(ss);
		    int windowCount = ((varSites.length - window) / step) + 1;
		    
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

			if(w1S<0 || w1E<w1S || w2S<w1E || w2E<w2S) {
			    System.out.println("!");
			}
			SimpleAlignment win[] = new SimpleAlignment[3];
			win[0] = ss.getAlignment(w1S, w1E, true);
			win[1] = ss.getAlignment(w2S, w2E, true);
			win[2] = ss.getAlignment(w1S, w2E, true);

			LRT lrt = new LRT(result, win, result.alpha, result.tRatio, result.gapThreshold);
			
			double lrtRes = lrt.calculate();
			
			//Add data point
			data.add(new Data(w1E, lrtRes));
			
			// Is it bigger than the current maximum?
			if (lrtRes > maximum)
				maximum = lrtRes;
			
			int percent = (int) (j / (float) windowCount * 100);
			ClusterUtils.setPercent(pctDir, percent);
		    }
		}
		else {
		    int windowCount = ((ss.getLength() - window) / step) + 1;
		    int firstWinPos = (int) (1 + (window / 2f - 0.5));
		    int pos = firstWinPos;
			int w = 1;

			for (int i = 0; i < windowCount; i++, pos += step, w += step)
			{
				if (LocalJobs.isRunning(result.jobId) == false)
					throw new Exception("cancel");

				new File(wrkDir, "window" + (i + 1)).mkdir();

				// 1st half window
				final int win1S = w;
				final int win1E = w + (window / 2) - 1;

				// 2nd half window
				final int win2S = w + (window / 2);
				final int win2E = w + window - 1;

				// Strip out the two partitions
				SimpleAlignment win[] = new SimpleAlignment[3];
				win[0] = ss.getAlignment(win1S, win1E, true);
				win[1] = ss.getAlignment(win2S, win2E, true);
				win[2] = ss.getAlignment(win1S, win2E, true);

				LRT lrt = new LRT(result, win, result.alpha, result.tRatio, result.gapThreshold);
				
				double lrtRes = lrt.calculate();
				data.add(new Data(pos, lrtRes));
				
				// Is it bigger than the current maximum?
				if (lrtRes > maximum)
					maximum = lrtRes;

				int percent = (int) (i / (float) windowCount * 100);
				ClusterUtils.setPercent(pctDir, percent);
			}
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