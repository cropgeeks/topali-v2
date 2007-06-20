// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.tree;

import java.awt.print.Printable;
import java.util.LinkedList;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.log4j.Logger;

import topali.data.*;
import topali.gui.*;

public class TreePane extends JDesktopPane implements InternalFrameListener
{
	Logger log = Logger.getLogger(this.getClass());
	
	// The AlignmentData object that stores the trees for this pane
	private AlignmentData data;

	// Keeps track of any floating tree dialogs
	private LinkedList<ExternalTreeFrame> frames = new LinkedList<ExternalTreeFrame>();

	// A reference to the node in the JTree that holds this data. Needed so that
	// changes to the node's text fire the correct events in the tree to redraw
	// it properly
	private DefaultMutableTreeNode node;

	private int n = 1;

	public TreePane(AlignmentData data)
	{
		this.data = data;

		setBackground(Icons.grayBackground);
	}

	public void displayTree(SequenceSet ss, TreeResult tree)
	{
		try
		{
			InternalTreeFrame frame = new InternalTreeFrame(this, ss, tree, n);

			frame.addInternalFrameListener(this);
			frame.setVisible(true);

			add(frame);

			try
			{
				frame.setSelected(true);
			} catch (Exception e)
			{
			}

			// (Potentially) reset the n counter used to position frames
			n = ((++n > 10) ? 1 : n);
		} catch (Exception e)
		{
			log.warn("Problem showing tree.\n",e);
		}
	}

	// Either floats (or stops floating) the given TreePanel. Each part of the
	// IF statement must find the internal or external frame that holds the
	// panel, then swap it into the opposite type.
	void toggleFloating(TreePanel panel, boolean toFloat)
	{
		if (toFloat)
		{
			for (JInternalFrame iFrame : getAllFrames())
			{
				InternalTreeFrame iTreeFrame = (InternalTreeFrame) iFrame;
				if (iTreeFrame.panel == panel)
				{
					ExternalTreeFrame eTreeFrame = new ExternalTreeFrame(this,
							panel, iTreeFrame);
					frames.add(eTreeFrame);

					return;
				}
			}
		} else
		{
			for (ExternalTreeFrame eFrame : frames)
			{
				if (eFrame.panel == panel)
				{
					eFrame.doClose();
					frames.remove(eFrame);

					return;
				}
			}
		}

		checkStatus();
	}

	public int getFrameCount()
	{
		return getAllFrames().length;
	}

	public void setTreeNode(DefaultMutableTreeNode node)
	{
		this.node = node;
	}

	// 1) Checks to see if this view should still allow printing
	// 2) Checks to see if the tree node for this view needs repainting
	private void checkStatus()
	{
		WinMainMenuBar.aFilePrint.setEnabled(isPrintable());
		WinMainMenuBar.aFilePrintPreview.setEnabled(isPrintable());
		WinMain.navPanel.getModel().nodeChanged(node);
	}

	public boolean isPrintable()
	{
		return (getSelectedFrame() == null) ? false : true;
	}

	public Printable[] getPrintables() {
		InternalTreeFrame iFrame = (InternalTreeFrame) getSelectedFrame();
		return new Printable[]{ iFrame.panel.canvas };
	}
	
	// Responds to a TreeFrame being closed - removes the tree from both the
	// GUI and the underlying project
	public void internalFrameClosed(InternalFrameEvent e)
	{
		InternalTreeFrame frame = (InternalTreeFrame) e.getSource();
		if (frame.isFloating)
			return;

		data.removeResult(frame.tree);
		WinMainMenuBar.aFileSave.setEnabled(true);

		checkStatus();
	}

	public void internalFrameOpened(InternalFrameEvent e)
	{
		checkStatus();
	}

	public void internalFrameActivated(InternalFrameEvent e)
	{
		checkStatus();
	}

	public void internalFrameIconified(InternalFrameEvent e)
	{
		checkStatus();
	}

	public void internalFrameDeiconified(InternalFrameEvent e)
	{
		checkStatus();
	}

	public void internalFrameDeactivated(InternalFrameEvent e)
	{
		checkStatus();
	}

	public void internalFrameClosing(InternalFrameEvent e)
	{
	}
}
