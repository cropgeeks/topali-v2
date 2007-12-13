// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.BorderLayout;
import java.awt.print.Printable;

import javax.swing.*;

import topali.data.*;
import topali.gui.Icons;

/**
 * Base class for all alignment result panels
 */
public abstract class ResultPanel extends JPanel
{

	AlignmentData data;
	AnalysisResult result;

	JPanel contentPanel;
	public ResultPanelToolbar toolbar;

	public ResultPanel(AlignmentData data, AnalysisResult result) {
		this.data = data;
		this.result = result;

		this.setLayout(new BorderLayout());

		contentPanel = new JPanel(new BorderLayout());
		contentPanel.setBorder(BorderFactory.createLineBorder(Icons.grayBackground,4));
		add(contentPanel, BorderLayout.CENTER);

		if(result instanceof AlignmentResult) {
		toolbar = new ResultPanelToolbar(this, data, (AlignmentResult)result);
		toolbar.setFloatable(false);
		toolbar.setBorderPainted(false);
		toolbar.setOrientation(SwingConstants.HORIZONTAL);
		toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		add(toolbar, BorderLayout.NORTH);
		}
	}

	/**
	 * Add the actual content to this ResultPanel
	 * (this method is meant to be called by subclasses of ResultPanel)
	 * @param comp
	 * @param containGraphs
	 */
	public void addContent(JComponent comp, boolean containGraphs) {
		this.contentPanel.add(comp, BorderLayout.CENTER);
		if(toolbar!=null) {
			if(containGraphs)
				toolbar.addGraphActions();
			//toolbar.enableGraphButtons(containGraphs);
		}
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
