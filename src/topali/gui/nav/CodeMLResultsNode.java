// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import javax.swing.JComponent;

import topali.data.AlignmentData;
import topali.data.CodeMLResult;
import topali.gui.IPrintable;
import topali.gui.results.CodeMLResultsPanel;

public class CodeMLResultsNode extends ResultsNode implements IPrintable
{
	private CodeMLResultsPanel panel;

	CodeMLResultsNode(AlignmentData data, CodeMLResult result)
	{
		super(data, result);

		panel = new CodeMLResultsPanel(result);
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
		return false;
	}

	public void print()
	{
		// panel.print();
	}
}