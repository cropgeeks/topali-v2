// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import java.awt.print.Printable;

import javax.swing.JComponent;

import topali.data.*;
import topali.gui.results.LRTResultPanel;

public class LRTResultsNode extends ResultsNode 
{

	LRTResultsNode(AlignmentData data, LRTResult result)
	{
		super(data, result);

		panel = new LRTResultPanel(data, result);
	}

	public String toString()
	{
		return result.guiName;
	}

	public String getHelpKey()
	{
		return "lrt_method";
	}

	public JComponent getPanel()
	{
		return panel;
	}

	public boolean isPrintable()
	{
		return true;
	}
	
	public Printable[] getPrintables()
	{
		return panel.getPrintables();
	}
}