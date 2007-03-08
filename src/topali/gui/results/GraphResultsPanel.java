// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import static topali.gui.WinMainMenuBar.aAnlsCreateTree;
import static topali.gui.WinMainMenuBar.aAnlsPartition;
import static topali.mod.Filters.CSV;
import static topali.mod.Filters.PNG;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.*;

import topali.data.*;
import topali.gui.*;
import topali.gui.dialog.AnalysisInfoDialog;
import topali.mod.Filters;
import doe.MsgBox;

public abstract class GraphResultsPanel extends JPanel implements IThresholdListener
{
	protected AlignmentData data;

	protected AlignmentResult aResult;

	protected GraphResultsToolBar toolbar;

	public GraphResultsPanel(AlignmentData data, AlignmentResult aResult)
	{
		this.data = data;
		this.aResult = aResult;

		toolbar = new GraphResultsToolBar();
	}

	public abstract void setThreshold(float thresholdCutoff);

	protected abstract void showThresholdDialog();

	private void reselectSequencesQuery()
	{
		String msg = "This will reselect the sequences used at the time of this "
				+ "analysis in the main alignment view window. Continue?";

		if (MsgBox.yesno(msg, 0) == JOptionPane.YES_OPTION)
			reselectSequences();
	}

	protected void reselectSequences()
	{
		TOPALi.winMain.menuAnlsReselectSequences(aResult.selectedSeqs);
	}

	protected void showInfoDialog()
	{
		AnalysisInfoDialog dialog = new AnalysisInfoDialog(aResult);

		dialog.setText(getAnalysisText());
		dialog.setVisible(true);
	}

	protected abstract String getAnalysisText();

	protected void addSelectedPartition()
	{
		WinMain.rDialog.addCurrentRegion(PartitionAnnotations.class);
	}

	protected void addSelectedRegion(Class type)
	{
		WinMain.rDialog.addCurrentRegion(type);
	}

	protected void showToolTipDialog()
	{
		System.out.println(aResult.useTreeToolTips);

		TreeToolTipDialog dialog = new TreeToolTipDialog(
				aResult.useTreeToolTips, aResult.treeToolTipWindow, data
						.getSequenceSet().getLength());

		aResult.useTreeToolTips = dialog.isOptionChecked();
		aResult.treeToolTipWindow = dialog.getWindowSize();

		WinMainMenuBar.aFileSave.setEnabled(true);
	}

	protected void saveGraph()
	{
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Export Graphs");
		fc.setCurrentDirectory(new File(Prefs.gui_dir));
		fc.setSelectedFile(new File(aResult.guiName));

		Filters.setFilters(fc, Prefs.gui_filter_graph, CSV, PNG);
		fc.setAcceptAllFileFilterUsed(false);

		while (fc.showSaveDialog(MsgBox.frm) == JFileChooser.APPROVE_OPTION)
		{
			File file = Filters.getSelectedFileForSaving(fc);

			// Confirm overwrite
			if (file.exists())
			{
				String msg = file
						+ " already exists.\nDo you want to replace it?";
				int response = MsgBox.yesnocan(msg, 1);

				if (response == JOptionPane.NO_OPTION)
					continue;
				else if (response == JOptionPane.CANCEL_OPTION
						|| response == JOptionPane.CLOSED_OPTION)
					return;
			}

			// Otherwise it's ok to save...
			Prefs.gui_dir = "" + fc.getCurrentDirectory();
			Prefs.gui_filter_graph = ((Filters) fc.getFileFilter()).getExtInt();

			try
			{
				if (Prefs.gui_filter_graph == CSV)
				{
					saveCSV(file);
					MsgBox.msg("Graph data successfully saved to " + file,
							MsgBox.INF);
				} else if (Prefs.gui_filter_graph == PNG)
					savePNG(file);
			} catch (Exception e)
			{
				MsgBox.msg(
						"There was an unexpected error while saving graph data:\n "
								+ e, MsgBox.ERR);
			}

			return;
		}
	}

	// Splits a String that is assumed to contain a filename (either x.y or x)
	// and returns the name and the extension as the two parts of the array
	protected String[] splitName(String name)
	{
		if (name.indexOf(".") == -1)
			return new String[]
			{ name, "" };

		return new String[]
		{ name.substring(0, name.lastIndexOf(".")),
				name.substring(name.lastIndexOf(".")) };
	}

	protected abstract void saveCSV(File filename) throws Exception;

	protected abstract void savePNG(File filename) throws Exception;

	protected class GraphResultsToolBar extends JToolBar
	{
		JButton bExport, bAuto, bAdd, bAdjust, bReselect, bInfo, bToolTip;

		AbstractAction aExport, aAdd, aAdjust, aReselect, aInfo, aToolTip;

		GraphResultsToolBar()
		{
			setFloatable(false);
			setBorderPainted(false);
			setOrientation(VERTICAL);
			setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

			aExport = new AbstractAction()
			{
				public void actionPerformed(ActionEvent e)
				{
					saveGraph();
				}
			};

			aAdd = new AbstractAction("Add Selection as New Partition")
			{
				public void actionPerformed(ActionEvent e)
				{
					addSelectedPartition();
				}
			};

			aAdjust = new AbstractAction("Adjust Threshold Significance")
			{
				public void actionPerformed(ActionEvent e)
				{
					showThresholdDialog();
				}
			};

			aReselect = new AbstractAction()
			{
				public void actionPerformed(ActionEvent e)
				{
					reselectSequencesQuery();
				}
			};

			aInfo = new AbstractAction()
			{
				public void actionPerformed(ActionEvent e)
				{
					showInfoDialog();
				}
			};

			aToolTip = new AbstractAction()
			{
				public void actionPerformed(ActionEvent e)
				{
					showToolTipDialog();
				}
			};

			bExport = (JButton) WinMainToolBar.getButton(false, null, "dss01",
					Icons.EXPORT, aExport);
			bAuto = (JButton) WinMainToolBar.getButton(false, null, "dss02",
					Icons.AUTO_PARTITION, WinMainMenuBar.aAnlsPartition);
			bAdd = (JButton) WinMainToolBar.getButton(false, null, "dss03",
					Icons.ADD_PARTITION, aAdd);
			bAdjust = (JButton) WinMainToolBar.getButton(false, null, "dss04",
					Icons.ADJUST_THRESHOLD, aAdjust);
			bReselect = (JButton) WinMainToolBar.getButton(false, null,
					"dss05", Icons.RESELECT, aReselect);
			bInfo = (JButton) WinMainToolBar.getButton(false, null, "dss06",
					Icons.ANALYSIS_INFO, aInfo);
			bToolTip = (JButton) WinMainToolBar.getButton(false, null, "dss07",
					Icons.TREE_TOOLTIPS, aToolTip);

			add(bExport);
			addSeparator();
			add(bAdd);
			add(bAuto);
			add(bAdjust);
			add(bToolTip);
			addSeparator();
			add(bReselect);
			add(bInfo);
		}
	}

	class MyPopupMenuAdapter extends PopupMenuAdapter
	{

		JMenu annotate;

		MyPopupMenuAdapter()
		{
			// add(toolbar.aAdd, Icons.ADD_PARTITION, KeyEvent.VK_S, 0, 0, 0,
			// false);

			JMenuItem addPart = new JMenuItem();
			addPart.setAction(new AbstractAction()
			{
				public void actionPerformed(ActionEvent e)
				{
					WinMain.rDialog.addRegion(data.getActiveRegionS(), data
							.getActiveRegionE(), PartitionAnnotations.class);
				}
			});
			addPart.setText(Text.Gui.getString("aAlgnAddPartition"));
			addPart.setMnemonic(KeyEvent.VK_P);

			JMenuItem addCodReg = new JMenuItem();
			addCodReg.setAction(new AbstractAction()
			{
				public void actionPerformed(ActionEvent e)
				{
					WinMain.rDialog.addRegion(data.getActiveRegionS(), data
							.getActiveRegionE(), CDSAnnotations.class);
				}
			});
			addCodReg.setText(Text.Gui.getString("aAlgnAddCDS"));
			addCodReg.setMnemonic(KeyEvent.VK_C);

			annotate = new JMenu(Text.Gui.getString("menuAlgnAnnotate"));
			annotate.setIcon(Icons.ADD_PARTITION);
			annotate.add(addPart);
			annotate.add(addCodReg);
			p.add(annotate);

			add(aAnlsPartition, Icons.AUTO_PARTITION, KeyEvent.VK_P, 0, 0, 0,
					false);
			add(toolbar.aAdjust, Icons.ADJUST_THRESHOLD, KeyEvent.VK_A, 0, 0,
					0, false);
			add(aAnlsCreateTree, Icons.CREATE_TREE, KeyEvent.VK_T,
					KeyEvent.VK_T, KeyEvent.CTRL_MASK, 20, true);
		}

		@Override
		protected void handlePopup(int x, int y)
		{
			
		}
		
		
	}
}