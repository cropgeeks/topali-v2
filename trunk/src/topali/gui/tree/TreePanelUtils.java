// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.tree;

import java.io.*;
import java.text.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;

import pal.tree.*;

import doe.*;

import topali.data.*;
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
				
		Filters.setFilters(fc, Prefs.gui_filter_tree, PNG, TRE, CLU);
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
			else if (Prefs.gui_filter_tree == CLU)
				saveCLU(file, panel);

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
	
	static void saveCLU(File file, TreePanel panel)
	{
		BufferedWriter out = null;
		try
		{
			out = new BufferedWriter(new FileWriter(file));
			out.write(panel.getClusterText());
		}
		catch (Exception e)
		{
			MsgBox.msg(Text.format(
				Text.GuiTree.getString("TreePanelUtils.err01"), e), MsgBox.ERR);
		}
		
		try { out.close(); }
		catch (Exception e) {}
	}
	
	// What does this do?
	// Scans through the list of sequences, grouping clusters based on how
	// similar two sequences are. When a sequence is first found, a new group is
	// created for it (if it's not already in a group). All similar sequences
	// (when compared) with this sequence will then be added to that group.
	// Colours are then assigned to each unique group
	
	static void cluster(TreePanel panel)
	{
		// Get the threshold value to use
		try
		{	
			// TODO: Make a nice dialog with a spinner control to enter the num
			DecimalFormat d = new DecimalFormat("0.000000");
			String input = (String) JOptionPane.showInputDialog(MsgBox.frm,
				"Please enter the threshold limit for the new grouping:",
				"Enter Threshold", JOptionPane.PLAIN_MESSAGE, null, null,
				d.format(Prefs.gui_group_threshold));
				
			if (input == null)
				return;
			
			float t = d.parse(input).floatValue();
			if (t <= 0)	throw new Exception();			
			
			Prefs.gui_group_threshold = t;
		}
		catch (Exception e)
		{
			MsgBox.msg("Please ensure a valid number (greater than zero) is "
				+ "entered.", MsgBox.ERR);
			return;
		}
	
		TreeDistanceMatrix dist = new TreeDistanceMatrix(panel.getPalTree());
		
//		Vector groups = new Vector();
		
		// Create a list of (clustered) sequences, each element being a cluster
		LinkedList<SequenceCluster> clusters = new LinkedList<SequenceCluster>();
		
		for (int i = 0; i < dist.getSize(); i++)
		{
			// What is this sequence's name?
			String seqName = dist.getIdentifier(i).getName();
			
			// What group is it in?
//			Vector myGroup = getGroup(groups, seqName, true);
			SequenceCluster cluster = getCluster(clusters, seqName, true);
			
			
			// Compare this sequence against all the others
			for (int j = i+1; j < dist.getSize(); j++)
			{
				// Are they close?
				if (dist.getDistance(i, j) < Prefs.gui_group_threshold)
				{
					String seqToAdd = dist.getIdentifier(j).getName();
					
					// Check it's not already in a group
//					if (getGroup(groups, seqToAdd, false) == null)
					if (getCluster(clusters, seqToAdd, false) == null)
//						myGroup.addElement(seqToAdd);
						cluster.addSequence(seqToAdd);
				}
			}
		}
		
		
/*		NameColouriser nc = new NameColouriser();
		for (int i = 0; i < groups.size(); i++)
		{
			Color c = Utils.getColor(i);
			Vector group = (Vector) groups.elementAt(i);
			for (int j = 0; j < group.size(); j++)
				nc.addMapping((String)group.elementAt(j), c);
		}
		
		panel.setNameColouriser(nc);	
		WinMainMenuBar.aFileSave.setEnabled(true);
		

*/

		panel.setClusters(clusters);
		
		panel.repaint();
//		frame.setClusterDetails(clusterDetails);
				
		if (MsgBox.yesno(clusters.size() + " group(s) were created.\nWould you "
			+ "like the first sequence of each group to be selected in the "
			+ "sequence list?", 0) == JOptionPane.YES_OPTION)
		{		
			// Sequence selection
			int[] indices = new int[clusters.size()];
			int i = 0;
			
			for (SequenceCluster cluster: clusters)
//			for (int i = 0; i < clusters.size(); i++)
			{
//				Vector group = (Vector) groups.elementAt(i);
				String seqName = cluster.getFirstSequence();
				indices[i] = panel.getSequenceSet().getIndexOf(seqName, true);
				if (indices[i] == -1)
				{
					MsgBox.msg("Unable to find one or more sequences (in this tree"
						+ ") in the current project.\nYou may have renamed "
						+ "these sequences since this tree was created.",
						MsgBox.ERR);
					return;
				}
				
				i++;
			}
			
			panel.getSequenceSet().setSelectedSequences(indices);
			((WinMain) MsgBox.frm).menuViewDisplaySettings(true);	
			WinMainMenuBar.aFileSave.setEnabled(true);		
		}
	}
	
/*	private static Vector getGroup(Vector groups, String name, boolean create)
	{
		for (int i = 0; i < groups.size(); i++)
		{
			// Get the group at this position
			Vector group = (Vector) groups.elementAt(i);
			
			// Search for this sequence in this group
			for (int j = 0; j < group.size(); j++)
			{
				if (((String)group.elementAt(j)).equals(name))
					return group;
			}
		}
		
		// Not found? Then create a new group for it
		if (create)
		{
			Vector newGroup = new Vector();
			newGroup.addElement(name);
		
			groups.addElement(newGroup);
			return newGroup;
		}
		else
			return null;
	}
*/
	
	private static SequenceCluster getCluster(LinkedList<SequenceCluster> clusters, String name, boolean create)
	{
		for (SequenceCluster cluster: clusters)
			if (cluster.contains(name))
				return cluster;
		
		// Not found? Then create (and return) a new cluster for it
		if (create)
		{
			// Create the cluster
			SequenceCluster cluster = new SequenceCluster();			
			cluster.addSequence(name);
			
			// Add it to the list
			clusters.add(cluster);
			return cluster;
		}
		else
			return null;
	}
}
