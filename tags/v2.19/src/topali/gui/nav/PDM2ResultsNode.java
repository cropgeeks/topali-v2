// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import java.awt.print.Printable;

import javax.swing.JComponent;

import topali.data.*;
import topali.gui.WinMainTipsPanel;

public class PDM2ResultsNode extends ResultsNode 
{

	PDM2ResultsNode(AlignmentData data, PDM2Result result)
	{
		super(data, result);

		//panel = new PDM2ResultsPanel(data, result);
	}

	@Override
	public String toString()
	{
		return result.guiName;
	}

	@Override
	public int getTipsKey()
	{
		return WinMainTipsPanel.TIPS_NONE;
	}

	@Override
	public String getHelpKey()
	{
		return "pdm2_method";
	}

	@Override
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