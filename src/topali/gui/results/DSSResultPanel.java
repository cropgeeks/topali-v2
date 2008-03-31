// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.BorderLayout;
import java.awt.print.Printable;

import javax.swing.JPanel;

import org.apache.log4j.Logger;

import topali.analyses.AnalysisUtils;
import topali.data.*;
import topali.gui.GradientPanel;
import topali.var.utils.Utils;

public class DSSResultPanel extends ResultPanel {

    Logger log = Logger.getLogger(this.getClass());

    GraphPanel graph;

    public DSSResultPanel(AlignmentData data, DSSResult result) {
	super(data, result);
	try {
	    double[][] graphData = (double[][]) Utils.castArray(result.data,
		    double.class);
	    graph = new GraphPanel(data, result, graphData, -1, null);

	    GradientPanel gp = new GradientPanel(
		    "Difference of Sums of Squares (DSS)");
	    gp.setStyle(GradientPanel.OFFICE2003);
	    JPanel p1 = new JPanel(new BorderLayout());
	    p1.add(gp, BorderLayout.NORTH);
	    p1.add(graph);

	    addContent(p1, true);
	    setThreshold(result.threshold);

	} catch (Exception e) {
	    log.warn(e);
	}
    }

    
    public void setThreshold(double threshold) {
	DSSResult res = (DSSResult) result;
	if (res.thresholds == null)
	    return;

	res.threshold = threshold;

	float thres = AnalysisUtils.getArrayValue(res.thresholds,
		(float) threshold);

	graph.setThreshold(thres);
    }

    
    public String getAnalysisInfo() {
	DSSResult result = (DSSResult) this.result;

	String str = new String(result.guiName);

	str += "\n\nRuntime: " + ((result.endTime - result.startTime) / 1000)
		+ " seconds";

	str += "\n\nWindow size:      " + result.window;
	str += "\nStep size:        " + result.step;
	str += "\nVar. window size: " + (result.type==DSSResult.TYPE_VARIABLE ? "true" : "false");
	str += "\nMethod:           " + result.method;
	str += "\nPower:            " + result.power;
	str += "\nPass count:       " + result.passCount;
	str += "\nThreshold runs:   " + (result.runs - 1);
	str += "\n\nSelected sequences:";

	for (String seq : result.selectedSeqs)
	    str += "\n  " + data.getSequenceSet().getNameForSafeName(seq);

	return str;
    }

    
    public Printable[] getPrintables() {
	return new Printable[] { graph.getPrintable() };
    }

}
