// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import static topali.gui.WinMainMenuBar.*;

import javax.swing.JComponent;

import topali.data.AlignmentData;
import topali.gui.*;

public class SequenceSetNode extends INode
{
	private String nodeName;

	private AlignmentPanel alignmentPanel;

	SequenceSetNode(AlignmentData data)
	{
		super(data);

		// Format the nodeName: Alignment (seqs x length)
		nodeName = Text.format(Text.GuiNav.getString("SequenceSetNode.gui01"),
				"" + ss.getSize(), "" + ss.getLength());

		alignmentPanel = new AlignmentPanel(data);
	}

	public int getTipsKey()
	{
		return WinMainTipsPanel.TIPS_ALN;
	}

	public String getHelpKey()
	{
		return "view_alignment";
	}

	public String toString()
	{
		return nodeName;
	}

	public JComponent getPanel()
	{
		return alignmentPanel;
	}

	// Convenience method to do the above without casting
	public AlignmentPanel getAlignmentPanel()
	{
		return alignmentPanel;
	}

	public void setMenus()
	{
		aFileExportDataSet.setEnabled(true);

		aAlgnDisplaySummary.setEnabled(true);
		aAlgnPhyloView.setEnabled(true);
		aAlgnSelectAll.setEnabled(true);
		aAlgnSelectNone.setEnabled(true);
		aAlgnSelectUnique.setEnabled(true);
		aAlgnSelectInvert.setEnabled(true);
		aAlgnFindSeq.setEnabled(true);
		aAlgnGoTo.setEnabled(true);

		boolean dna = data.getSequenceSet().isDNA();
		aAnlsRunPDM.setEnabled(dna);
		aAnlsRunPDM2.setEnabled(dna);
		aAnlsRunHMM.setEnabled(dna);
		aAnlsRunDSS.setEnabled(dna);
		aAnlsRunLRT.setEnabled(dna);
		aAnlsRunCodeMLBranch.setEnabled(dna);
		aAnlsRunCodeMLSite.setEnabled(dna);
		aAnlsRunMG.setEnabled(true);
		aAnlsRunCW.setEnabled(dna);
		
		aAnlsCreateTree.setEnabled(true);

		aVamExport.setEnabled(true);

		alignmentPanel.getListPanel().setMenus();
	}
}