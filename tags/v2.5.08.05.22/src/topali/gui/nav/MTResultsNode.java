// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import java.awt.print.Printable;

import javax.swing.JComponent;

import topali.data.*;
import topali.gui.results.MTResultPanel;

public class MTResultsNode extends ResultsNode
{

	MTResultPanel panel;
	
	public MTResultsNode(AlignmentData data, ModelTestResult result) {
		super(data, result);
		panel = new MTResultPanel(data, result);
	}
	
	
	public JComponent getPanel()
	{
		return panel;
	}

	public Printable[] getPrintables()
	{
		return panel.getPrintables();
	}

	public boolean isPrintable()
	{
		return true; 
	}

	
	public String toString()
	{
		return result.guiName;
	}
}
