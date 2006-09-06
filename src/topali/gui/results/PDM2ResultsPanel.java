// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.print.*;
import java.io.*;
import javax.swing.*;

import topali.analyses.*;
import topali.data.*;
import topali.gui.*;
import topali.gui.dialog.*;

import doe.*;

public class PDM2ResultsPanel extends GraphResultsPanel
{
	private PDM2Result result;		
	private AlignmentGraph graph;
	
	public PDM2ResultsPanel(AlignmentData data, PDM2Result result)
	{
		super(data, result);
		this.result = result;
		
		graph = new AlignmentGraph(data, result, result.locData, result.thresholds);
		graph.setBorder(BorderFactory.createLineBorder(Icons.grayBackground, 4));
		graph.getGraphPanel().addMouseListener(new MyPopupMenuAdapter());
		
		setThreshold(result.thresholdCutoff);
		
		setLayout(new BorderLayout());
		add(toolbar, BorderLayout.EAST);
		add(graph);
	}
		
	public void print()
	{
		Printable[] toPrint = { graph };		
		new PrinterDialog(toPrint);
	}
	
	public void setThreshold(float thresholdCutoff)
	{
		result.thresholdCutoff = thresholdCutoff;
		
		float threshold =
			AnalysisUtils.getArrayValue(result.thresholds, thresholdCutoff);
		
		graph.setThresholdValue(threshold);
	}
	
	protected void showThresholdDialog()
	{ 
		new ThresholdDialog(this, result.thresholdCutoff);
	}
	
	protected String getAnalysisText()
	{
/*		String str = new String(result.guiName);
		
		str += "\n\nRuntime: " + ((result.endTime-result.startTime)/1000) + " seconds";
		
		str += "\n\nWindow size:    " + result.window;
		str += "\nStep size:      " + result.step;
		str += "\nMethod:         " + result.method;
		str += "\nPower:          " + result.power;
		str += "\nPass count:     " + result.passCount;
		str += "\nThreshold runs: " + (result.runs - 1);
		str += "\n\nSelected sequences:";
		
		for (String seq: result.selectedSeqs)
			str += "\n  " + data.getSequenceSet().getNameForSafeName(seq);
		
		return str;
*/
		return "TODO";
	}
	
	protected void saveCSV(File filename)
		throws IOException
	{
/*		BufferedWriter out = new BufferedWriter(new FileWriter(filename));
			
		out.write("Nucleotide, DSS Score");
		out.newLine();
			
		for (int i = 0; i < result.data.length; i++)
		{
			out.write(result.data[i][0] + ", " + result.data[i][1]);
			out.newLine();
		}
			
		out.close();
*/
	}
	
	protected void savePNG(File filename)
		throws Exception
	{
/*
		Utils.saveComponent(graph.getGraphPanel(), filename, 600, 250);
		updateUI();
		
		MsgBox.msg("Graph data successfully saved to " + filename, MsgBox.INF);
*/
	}
}