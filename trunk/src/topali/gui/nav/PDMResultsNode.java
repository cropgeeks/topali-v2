// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import javax.swing.JComponent;

import topali.data.AlignmentData;
import topali.data.PDMResult;
import topali.gui.IPrintable;
import topali.gui.WinMainTipsPanel;
import topali.gui.results.PDMResultsPanel;

public class PDMResultsNode extends ResultsNode implements IPrintable
{
	private PDMResultsPanel panel;

	PDMResultsNode(AlignmentData data, PDMResult result)
	{
		super(data, result);

		panel = new PDMResultsPanel(data, result);
	}

	public String toString()
	{
		return result.guiName;
	}

	public int getTipsKey()
	{
		return WinMainTipsPanel.TIPS_NONE;
	}

	public String getHelpKey()
	{
		return "pdm_method";
	}

	public JComponent getPanel()
	{
		return panel;
	}

	public boolean isPrintable()
	{
		return true;
	}

	public void print()
	{
		panel.print();
	}
}