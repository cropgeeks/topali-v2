// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import java.awt.print.Printable;

import javax.swing.JComponent;

import topali.data.*;
import topali.gui.results.MGResultPanel;

public class MGResultsNode extends ResultsNode
{

	MGResultPanel panel;
	
	public MGResultsNode(AlignmentData data, MGResult result) {
		super(data, result);
		panel = new MGResultPanel(data, result);
	}
	
	@Override
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
		//TODO: Fix table printing
		return false; 
	}

	@Override
	public String toString()
	{
		return result.guiName;
	}
}
