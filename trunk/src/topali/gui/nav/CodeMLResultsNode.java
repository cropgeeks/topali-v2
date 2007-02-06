// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import javax.swing.*;

import topali.data.*;
import topali.gui.*;
import topali.gui.results.*;

public class CodeMLResultsNode extends ResultsNode implements IPrintable
{
	private CodeMLResultsPanel panel;
	
	CodeMLResultsNode(AlignmentData data, CodeMLResult result)
	{
		super(data, result);
		
		panel = new CodeMLResultsPanel(data, result);
	}
	
	public String toString()
		{ return result.guiName; }
		
	public String getHelpKey()
		{ return "cml_method"; }
	
	public JComponent getPanel()
		{ return panel; }
	
	public boolean isPrintable()
		{ return false; }
	
	public void print()
	{
//		panel.print();
	}
}