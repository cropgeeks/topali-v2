// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import static topali.gui.WinMainMenuBar.*;

import java.awt.*;

import javax.swing.*;

import topali.data.*;
import topali.gui.Text;
import doe.DoeLayout;

public class ResultsNodeFolder extends INode
{
	ResultsNodeFolder(AlignmentData data)
	{
		super(data);
	}

	@Override
	public String toString()
	{
		return Text.GuiNav.getString("ResultsNode.gui01");
	}

	@Override
	public void setMenus()
	{
		aFileExportDataSet.setEnabled(true);

	}

	@Override
	public JComponent getPanel()
	{
		int count = 0;
		for (AnalysisResult result : data.getResults())
			if (result.endTime != 0)
				count++;

		String msg1 = null;
		if (count == 1)
			msg1 = count + " result available";
		else
			msg1 = count + " results available";
		if (count != 0)
			msg1 += " - expand the Results folder to view individual analyses";

		String msg2 = "";
		if (count != data.getResults().size())
		{
			int diff = data.getResults().size() - count;

			msg2 = "(" + ((diff == 1) ? "1 analysis " : diff + " analyses ");
			msg2 += "still in progress)";
		}

		DoeLayout layout = new DoeLayout();
		layout.getPanel().setBackground(Color.white);
		layout.add(new JLabel(msg1, SwingConstants.CENTER), 0, 0, 1, 1, new Insets(0,
				0, 0, 0));
		layout.add(new JLabel(msg2, SwingConstants.CENTER), 0, 1, 1, 1, new Insets(2,
				0, 0, 0));

		return layout.getPanel();
	}
}
