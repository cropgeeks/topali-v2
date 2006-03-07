// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import javax.swing.*;
import javax.swing.tree.*;

import topali.data.*;
import topali.gui.*;
import topali.gui.results.*;
import static topali.gui.WinMainMenuBar.*;

public class PDMResultsNode extends ResultsNode implements IPrintable
{
	private PDMResultsPanel panel;
	
	PDMResultsNode(AlignmentData data, PDMResult result)
	{
		super(data, result);
		
		panel = new PDMResultsPanel(data, result);
	}
	
	public String toString()
		{ return result.guiName; }
		
	public int getTipsKey()
		{ return WinMainTipsPanel.TIPS_NONE; }
	
	public String getHelpKey()
		{ return "pdm_method"; }
	
	public JComponent getPanel()
		{ return panel; }
	
	public boolean isPrintable()
		{ return true; }
	
	public void print()
	{
		panel.print();
	}
}