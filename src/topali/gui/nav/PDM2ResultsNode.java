// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import javax.swing.JComponent;

import topali.data.AlignmentData;
import topali.data.PDM2Result;
import topali.gui.IPrintable;
import topali.gui.WinMainTipsPanel;
import topali.gui.results.PDM2ResultsPanel;

public class PDM2ResultsNode extends ResultsNode implements IPrintable
{
	private PDM2ResultsPanel panel;

	PDM2ResultsNode(AlignmentData data, PDM2Result result)
	{
		super(data, result);

		panel = new PDM2ResultsPanel(data, result);
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
		return "pdm2_method";
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