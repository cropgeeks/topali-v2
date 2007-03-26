// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.print.Printable;

import topali.analyses.AnalysisUtils;
import topali.data.AlignmentData;
import topali.data.LRTResult;
import topali.var.Utils;

public class LRTResultPanel extends ResultPanel
{

	GraphPanel graph;
	
	public LRTResultPanel(AlignmentData data, LRTResult result)
	{
		super(data, result);
		graph = new GraphPanel(data, result, Utils.float2doubleArray(result.data), -1.0, GraphPanel.RIGHT);
		addContent(graph, true);
		setThreshold(result.threshold);
	}

	@Override
	public String getAnalysisInfo()
	{
		LRTResult result = (LRTResult) this.result;
		String str = new String(result.guiName);

		str += "\n\nRuntime: " + ((result.endTime - result.startTime) / 1000)
				+ " seconds";

		str += "\n\nWindow size:    " + result.window;
		str += "\nStep size:      " + result.step;
		str += "\nMethod:         " + result.method;
		str += "\nThreshold runs: " + (result.runs - 1);
		str += "\n\nSelected sequences:";

		for (String seq : result.selectedSeqs)
			str += "\n  " + data.getSequenceSet().getNameForSafeName(seq);

		return str;
	}

	@Override
	public Printable[] getPrintables()
	{
		return new Printable[] {graph};
	}

	@Override
	public void setThreshold(double t)
	{
		LRTResult res = (LRTResult) this.result;
		result.threshold = t;
		float thres = AnalysisUtils.getArrayValue(res.thresholds,(float)t);

		graph.setThreshold((double)thres);

	}

}
