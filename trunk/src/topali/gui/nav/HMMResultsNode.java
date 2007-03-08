// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import javax.swing.JComponent;

import topali.data.AlignmentData;
import topali.data.HMMResult;
import topali.gui.WinMainTipsPanel;
import topali.gui.results.HMMResultsPanel;

public class HMMResultsNode extends ResultsNode
{
	private HMMResultsPanel panel;

	HMMResultsNode(AlignmentData data, HMMResult result)
	{
		super(data, result);

		panel = new HMMResultsPanel(data, result);
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
		return "hmm_method";
	}

	public JComponent getPanel()
	{
		return panel;
	}
}