// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import javax.swing.*;

import topali.data.*;
import topali.gui.*;
import topali.gui.results.*;

public class LRTResultsNode extends ResultsNode implements IPrintable
{
	private LRTResultsPanel panel;
	
	LRTResultsNode(AlignmentData data, LRTResult result)
	{
		super(data, result);
		
		panel = new LRTResultsPanel(data, result);
	}
	
	public String toString()
		{ return result.guiName; }
		
	public String getHelpKey()
		{ return "lrt_method"; }
	
	public JComponent getPanel()
		{ return panel; }
	
	public boolean isPrintable()
		{ return true; }
	
	public void print()
	{
		panel.print();
	}
}