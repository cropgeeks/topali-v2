// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.BorderLayout;
import java.awt.print.Printable;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.*;

import topali.data.AlignmentData;
import topali.data.AlignmentResult;
import topali.gui.Icons;

/**
 * Base class for all alignment result panels
 */
public abstract class ResultPanel extends JPanel
{
	//Some predefined number formatters
	public static final NumberFormat branchLengthFormat = new DecimalFormat("#.###");
	public static final NumberFormat omegaFormat = new DecimalFormat("#.###");
	public static final NumberFormat lrtFormat = new DecimalFormat("#.###");
	public static final NumberFormat likelihoodFormat = new DecimalFormat("#####.##");
	
	AlignmentData data;
	AlignmentResult result;
	
	JPanel contentPanel;
	ResultPanelToolbar toolbar;
	
	public ResultPanel(AlignmentData data, AlignmentResult result) {
		this.data = data;
		this.result = result;
		
		contentPanel = new JPanel(new BorderLayout());
		contentPanel.setBorder(BorderFactory.createLineBorder(Icons.grayBackground,4));
		toolbar = new ResultPanelToolbar(this, data, result);
		toolbar.setFloatable(false);
		toolbar.setBorderPainted(false);
		toolbar.setOrientation(JToolBar.HORIZONTAL);
		toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		
		this.setLayout(new BorderLayout());
		add(contentPanel, BorderLayout.CENTER);
		add(toolbar, BorderLayout.NORTH);
		
	}

	/**
	 * Add the actual content to this ResultPanel 
	 * (this method is meant to be called by subclasses of ResultPanel)
	 * @param comp
	 * @param containGraphs
	 */
	public void addContent(JComponent comp, boolean containGraphs) {
		this.contentPanel.add(comp);
		toolbar.enableGraphButtons(containGraphs);
	}
	
	/**
	 * Get detailed information about the analysis
	 * @return 
	 */
	public abstract String getAnalysisInfo();
	
	/**
	 * Set the threshold
	 * @param t
	 */
	public abstract void setThreshold(double t);
	
	/**
	 * Get the printable content of this ResultPanel
	 * @return
	 */
	public abstract Printable[] getPrintables();
	
}
