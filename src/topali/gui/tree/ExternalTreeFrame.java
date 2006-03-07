package topali.gui.tree;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import doe.*;

class ExternalTreeFrame extends JDialog
{
	TreePane treePane;
	TreePanel panel;
	InternalTreeFrame iFrame;
	
	ExternalTreeFrame(TreePane treePane, TreePanel panel, InternalTreeFrame iFrame)
	{
		super(MsgBox.frm, panel.getTreeResult().getTitle(), false);
		
		this.treePane = treePane;
		this.panel = panel;
		this.iFrame = iFrame;
		
		add(panel);
		addCloseHandler();
		pack();
		
		iFrame.isFloating = true;
		iFrame.setVisible(false);
		setLocation(20, 20);		
		setVisible(true);
	}
	
	private void addCloseHandler()
	{
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e)
			{
				treePane.toggleFloating(panel, false);
			}
		});
	}
	
	void doClose()
	{
		setVisible(false);
		
		iFrame.isFloating = false;
		iFrame.add(panel);
		iFrame.setVisible(true);
		
		// Overkill...but need a way to make the toolbar realise that the mouse
		// ISN'T over the button anymore when the window closes. Tried repaint()
		// revalidate() etc...
		panel.toolbar.bFloat.setSelected(false);
		panel.toolbar.bFloat.setEnabled(false);
		
		panel.toolbar.bFloat.setEnabled(true);
	}
}