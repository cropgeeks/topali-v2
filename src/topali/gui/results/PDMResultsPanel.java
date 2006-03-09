// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import javax.swing.*;

import pal.statistics.*;

import topali.data.*;
import topali.gui.*;
import topali.gui.dialog.*;

import doe.*;

public class PDMResultsPanel extends GraphResultsPanel
	implements MouseMotionListener, MouseListener
{
	private PDMResult result;	
	private AlignmentGraph glbGraph, locGraph;
	private HistogramPanel histoPanel;
	
	public PDMResultsPanel(AlignmentData data, PDMResult result)
	{
		super(data, result);
		this.result = result;
		
		float[] threshold = { 0.95f };
				
		glbGraph = new AlignmentGraph(data, result, result.glbData, threshold);
		glbGraph.getGraphPanel().addMouseListener(new MyPopupMenuAdapter());
		glbGraph.setBorder(BorderFactory.createTitledBorder("Global divergence measure:"));
		locGraph = new AlignmentGraph(data, result, result.locData, threshold);
		locGraph.getGraphPanel().addMouseListener(new MyPopupMenuAdapter());
		locGraph.setBorder(BorderFactory.createTitledBorder("Local divergence measure:"));		
		
		glbGraph.getGraphPanel().addMouseMotionListener(this);
		glbGraph.getGraphPanel().addMouseListener(this);
		locGraph.getGraphPanel().addMouseMotionListener(this);
		locGraph.getGraphPanel().addMouseListener(this);
		
		setThreshold(result.thresholdCutoff);
		
		histoPanel = new HistogramPanel();
						
		JPanel p1 = new JPanel(new GridLayout(2, 1, 5, 5));
		p1.add(glbGraph);
		p1.add(locGraph);
		
		JPanel p2 = new JPanel(new BorderLayout());
		p2.setBorder(BorderFactory.createTitledBorder(
			"Probability distribution (" + result.histograms[0].length + "):"));
		p2.add(histoPanel);
		
		JPanel p3 = new JPanel(new BorderLayout());
		p3.setBorder(BorderFactory.createLineBorder(Icons.grayBackground, 4));
		p3.add(p1);
		p3.add(p2, BorderLayout.SOUTH);
		
		setLayout(new BorderLayout());
		add(toolbar, BorderLayout.EAST);
		add(p3);
	}
	
	public void print()
	{
		Printable[] toPrint = { glbGraph, locGraph };		
		new PrinterDialog(toPrint);
	}
	
	public void setThreshold(float thresholdCutoff)
	{
		result.thresholdCutoff = thresholdCutoff;
		System.out.println("cutoff is " + thresholdCutoff);
			
//		glbGraph.setThresholdValue(thresholdCutoff);
		locGraph.setThresholdValue(result.calculateThreshold());
	}
	
	protected void showThresholdDialog()
		{ new ThresholdDialog(this, result.thresholdCutoff); }
	
	protected String getAnalysisText()
	{
		String str = new String(result.guiName);
		
		str += "\n\nRuntime: " + ((result.endTime-result.startTime)/1000) + " seconds";
		
		str += "\n\npdm_window:          " + result.pdm_window;
		str += "\npdm_step:            " + result.pdm_step;
		str += "\npdm_prune:           " + result.pdm_prune;
		str += "\npdm_cutoff:          " + result.pdm_cutoff;
		str += "\npdm_seed:            " + result.pdm_seed;
		str += "\npdm_burn:            " + result.pdm_burn;
		str += "\npdm_cycles:          " + result.pdm_cycles;
		str += "\npdm_burn_algorithm:  " + result.pdm_burn_algorithm;
		str += "\npdm_main_algorithm:  " + result.pdm_main_algorithm;
		str += "\npdm_use_beta:        " + result.pdm_use_beta;
		str += "\npdm_parameter_update_interval: " + result.pdm_parameter_update_interval;
		str += "\npdm_update_theta:    " + result.pdm_update_theta;
		str += "\npdm_tune_interval:   " + result.pdm_tune_interval;
		str += "\npdm_molecular_clock: " + result.pdm_molecular_clock;
		str += "\npdm_category_list:   " + result.pdm_category_list;
		str += "\npdm_initial_theta:   " + result.pdm_initial_theta;
		str += "\npdm_outgroup:        " + result.pdm_outgroup;
		str += "\npdm_global_tune:     " + result.pdm_global_tune;
		str += "\npdm_local_tune:      " + result.pdm_local_tune;
		str += "\npdm_theta_tune:      " + result.pdm_theta_tune;
		str += "\npdm_beta_tune:       " + result.pdm_beta_tune;
		
		str += "\n\nSelected sequences:";
		
		for (String seq: result.selectedSeqs)
			str += "\n  " + data.getSequenceSet().getNameForSafeName(seq);
		
		return str;
	}
	
	protected void saveCSV(File filename)
		throws Exception
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(filename));
			
		out.write("Nucleotide, Global, ,Nucleotide, Local");
		out.newLine();

		for (int i = 0; i < result.glbData.length; i++)
		{
			out.write(result.glbData[i][0] + ", " + result.glbData[i][1]);
			
			if (i < result.locData.length)
				out.write(", , " + result.locData[i][0] + ", " + result.locData[i][1]);
			
			out.newLine();
		}
			
		out.close();
	}
	
	protected void savePNG(File f)
		throws Exception
	{
		String[] s = splitName(f.getName());		
		File gName = new File(f.getParent(), s[0] + " (global)" + s[1]);
		File lName = new File(f.getParent(), s[0] + " (local)" + s[1]);
		
		Utils.saveComponent(glbGraph.getGraphPanel(), gName, 600, 250);
		Utils.saveComponent(locGraph.getGraphPanel(), lName, 600, 250);
		updateUI();
		
		MsgBox.msg("Graph data successfully saved to:\n"
			+ gName + "\n" + lName, MsgBox.INF);
	}
	
	public void mouseMoved(MouseEvent e)
	{		
		int nuc = locGraph.getGraphPanel().getNucleotideFromPoint(e.getX());

		int pos = -1;
//		int nuc = getNucleotideFromPoint(e.getPoint().x);
		
		if (nuc >= result.locData[0][0] - result.pdm_step)
			// Position: +stepSize moves right one window to avoid -0.x and
			// +0.x both giving the same window
			pos = (int) ( (nuc-result.locData[0][0]+result.pdm_step ) / (float)result.pdm_step);
		
	
		if (pos < 0 || pos >= result.histograms.length)
			histoPanel.setData(null);
		else
			histoPanel.setData(result.histograms[pos]);
	}
	
	public void mouseExited(MouseEvent e)
		{ histoPanel.setData(null);	}
	
	public void mouseDragged(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}	
}