// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import java.awt.print.Printable;

import javax.swing.JComponent;

import topali.data.*;
import topali.gui.results.*;

public class CodeMLResultsNode extends ResultsNode
{

	CodeMLResultsNode(AlignmentData data, CodeMLResult result)
	{
		super(data, result);

		if(result.type==CodeMLResult.TYPE_SITEMODEL) {
			//panel = new CodeMLSiteResultPanel(data, result);
			panel = new CMLSiteResultPanel(data, result);
		}
		else if(result.type==CodeMLResult.TYPE_BRANCHMODEL)
			//panel = new CodeMLBranchResultPanel(data, result);
			panel = new CMLBranchResultPanel(data, result);
		}

	public String toString()
	{
		return result.guiName;
	}

	public String getHelpKey()
	{
		return "cml_method";
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