// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.Printable;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import topali.analyses.AnalysisUtils;
import topali.data.*;
import topali.var.Utils;

public class PDMResultPanel extends ResultPanel implements MouseMotionListener, MouseListener
{

	GraphPanel graph1, graph2;
	HistogramPanel histoPanel;
	
	public PDMResultPanel(AlignmentData data, PDMResult result)
	{
		super(data, result);
		graph1 = new GraphPanel(data, result, Utils.float2doubleArray(result.glbData), -1, GraphPanel.RIGHT);
		graph2 = new GraphPanel(data, result, Utils.float2doubleArray(result.locData), -1, GraphPanel.RIGHT);
		graph2.setThreshold(result.threshold);
		
		graph1.setBorder(BorderFactory
				.createTitledBorder("Global divergence measure"));
		graph2.setBorder(BorderFactory
				.createTitledBorder("Local divergence measure"));
		
		graph1.addMouseListener(this);
		graph1.addMouseMotionListener(this);
		graph2.addMouseListener(this);
		graph2.addMouseMotionListener(this);
		
		histoPanel = new HistogramPanel();
		histoPanel.setBorder(BorderFactory.createTitledBorder(""));
		
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0.45;
		//c.insets = new Insets(0,0,2,0);
		c.fill = GridBagConstraints.BOTH;
		p.add(graph1, c);
		GridBagConstraints c2 = new GridBagConstraints();
		c2.gridx = 0;
		c2.gridy = 1;
		c2.weightx = 1;
		c2.weighty = 0.45;
		//c2.insets = new Insets(2,0,0,0);
		c2.fill = GridBagConstraints.BOTH;
		p.add(graph2, c2);
		GridBagConstraints c3 = new GridBagConstraints();
		c3.gridx = 0;
		c3.gridy = 2;
		c3.weightx = 1;
		c3.weighty = 0.1;
		c3.insets = new Insets(2,2,2,2);
		c3.fill = GridBagConstraints.BOTH;
		p.add(histoPanel, c3);
		
//		JSplitPane p = new JSplitPane(JSplitPane.VERTICAL_SPLIT, graph1, graph2);
//		p.setDividerLocation(0.5d);
//		
//		int h = (int)this.getPreferredSize().getHeight();
//		p.setDividerLocation((int)(h/2));
		
		addContent(p, true);
		
		setThreshold(result.threshold);
	}

	@Override
	public String getAnalysisInfo()
	{
		PDMResult result = (PDMResult)this.result;
		String str = new String(result.guiName);

		str += "\n\nRuntime: " + ((result.endTime - result.startTime) / 1000)
				+ " seconds";

		str += "\n\npdm_window:          " + result.pdm_window;
		str += "\npdm_step:            " + result.pdm_step;
		str += "\npdm_runs:            " + (result.pdm_runs - 1);
		str += "\npdm_prune:           " + result.pdm_prune;
		str += "\npdm_cutoff:          " + result.pdm_cutoff;
		str += "\npdm_seed:            " + result.pdm_seed;
		str += "\npdm_burn:            " + result.pdm_burn;
		str += "\npdm_cycles:          " + result.pdm_cycles;
		str += "\npdm_burn_algorithm:  " + result.pdm_burn_algorithm;
		str += "\npdm_main_algorithm:  " + result.pdm_main_algorithm;
		str += "\npdm_use_beta:        " + result.pdm_use_beta;
		str += "\npdm_parameter_update_interval: "
				+ result.pdm_parameter_update_interval;
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

		for (String seq : result.selectedSeqs)
			str += "\n  " + data.getSequenceSet().getNameForSafeName(seq);

		return str;
	}

	@Override
	public Printable[] getPrintables()
	{
		return new Printable[] {graph1, graph2};
	}

	@Override
	public void setThreshold(double t)
	{
		((AlignmentResult)result).threshold = t;
		PDMResult res = (PDMResult)result;
		
		float thres= AnalysisUtils.getArrayValue(res.thresholds,
				(float)t);
		
		graph2.setThreshold(thres);
	}

	public void mouseMoved(MouseEvent e)
	{		
		PDMResult result = (PDMResult)this.result;
		if(result.histograms==null)
			return;
		
		int nuc = graph2.getNucleotideFromPoint(e.getX());
		
		int pos = -1;
		
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
