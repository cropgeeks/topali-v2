// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import javax.swing.*;

import topali.data.*;
import topali.gui.*;
import topali.gui.results.*;

public class PDM2ResultsNode extends ResultsNode implements IPrintable
{
	private PDM2ResultsPanel panel;
	
	PDM2ResultsNode(AlignmentData data, PDM2Result result)
	{
		super(data, result);
		
		panel = new PDM2ResultsPanel(data, result);
	}
	
	public String toString()
		{ return result.guiName; }
	
	public int getTipsKey()
		{ return WinMainTipsPanel.TIPS_NONE; }
	
	public String getHelpKey()
		{ return "pdm2_method"; }
	
	public JComponent getPanel()
		{ return panel; }
	
	public boolean isPrintable()
		{ return true; }
	
	public void print()
	{
		panel.print();
	}
}