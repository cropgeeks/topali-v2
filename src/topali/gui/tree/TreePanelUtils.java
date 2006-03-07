// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.tree;

import java.io.*;
import javax.imageio.*;
import javax.swing.*;

import pal.tree.*;

import doe.*;

import topali.gui.*;
import topali.mod.*;
import static topali.mod.Filters.*;

class TreePanelUtils
{
	static void exportTree(TreePanel panel)
	{
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(Text.GuiTree.getString("TreePanelUtils.gui01"));
		fc.setCurrentDirectory(new File(Prefs.gui_dir));
				
		Filters.setFilters(fc, Prefs.gui_filter_tree, PNG, TRE);
		fc.setAcceptAllFileFilterUsed(false);

		while (fc.showSaveDialog(MsgBox.frm) == JFileChooser.APPROVE_OPTION)
		{
			File file = Filters.getSelectedFileForSaving(fc);

			// Confirm overwrite
			if (file.exists())
			{
				String msg = Text.format(
					Text.GuiTree.getString("TreePanelUtils.msg01"), file);
				int response = MsgBox.yesnocan(msg, 1);
					
				if (response == JOptionPane.NO_OPTION)
					continue;
				else if (response == JOptionPane.CANCEL_OPTION ||
					response == JOptionPane.CLOSED_OPTION)
					return;
			}
			
			// Otherwise it's ok to save...
			Prefs.gui_dir = "" + fc.getCurrentDirectory();
			Prefs.gui_filter_tree = ((Filters)fc.getFileFilter()).getExtInt();
			
			if (Prefs.gui_filter_tree == PNG)
				savePNG(file, panel);
			else if (Prefs.gui_filter_tree == TRE)
				saveTRE(file, panel);

			return;
		}
	}
	
	static void savePNG(File file, TreePanel panel)
	{
		try
		{
			ImageIO.write(panel.getSavableImage(), "png", file);
			MsgBox.msg(Text.format(Text.GuiTree.getString(
				"TreePanelUtils.msg02"), file), MsgBox.INF);
		}
		catch (Exception e)
		{
			MsgBox.msg(Text.format(
				Text.GuiTree.getString("TreePanelUtils.err01"), e), MsgBox.ERR);
		}
	}
	
	static void saveTRE(File file, TreePanel panel)
	{
		PrintWriter out = null;
		
		try
		{
			out = new PrintWriter(new FileOutputStream(file));
			TreeUtils.printNH(panel.getPalTree(), out, true, true);
			
			MsgBox.msg(Text.format(Text.GuiTree.getString(
				"TreePanelUtils.msg03"), file), MsgBox.INF);
		}
		catch (Exception e)
		{
			MsgBox.msg(Text.format(
				Text.GuiTree.getString("TreePanelUtils.err01"), e), MsgBox.ERR);
		}
		
		try { out.close(); }
		catch (Exception e) {}
	}
}
