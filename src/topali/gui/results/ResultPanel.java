// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.print.Printable;
import java.util.*;

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
		toolbar.setOrientation(SwingConstants.VERTICAL);
		toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		add(toolbar, BorderLayout.EAST);
		}
	}

	/**
	 * Add the actual content to this ResultPanel
	 * (this method is meant to be called by subclasses of ResultPanel)
	 * @param comp
	 * @param containGraphs
	 */
	public void addContent(Component comp, boolean containsGraphs) {
		this.contentPanel.add(comp, BorderLayout.CENTER);
		if(toolbar!=null) {
			if(containsGraphs)
				toolbar.addGraphActions();
			//toolbar.enableGraphButtons(containGraphs);
		}
	}
	
	public java.util.List<DataVisPanel> getDataPanels() {
	    LinkedList<DataVisPanel> result = new LinkedList<DataVisPanel>();
	    searchDataPanels(this.contentPanel, result);
	    return result;
	}
	
	private void searchDataPanels(JComponent comp, java.util.List<DataVisPanel> list) {
	    if(comp instanceof DataVisPanel)
		list.add((DataVisPanel)comp);
	    
	    for(int i=0; i<comp.getComponentCount(); i++) {
		Component tmp = comp.getComponent(i);
		if(tmp instanceof JComponent)
		    searchDataPanels((JComponent)comp.getComponent(i), list);
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
