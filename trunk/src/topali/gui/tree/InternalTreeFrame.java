package topali.gui.tree;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import topali.data.*;
import topali.gui.*;

class InternalTreeFrame extends JInternalFrame implements ComponentListener
{
	TreeResult tree;
	TreePanel panel;
	boolean isFloating;
	
	InternalTreeFrame(TreePane treePane, SequenceSet ss, TreeResult tree, int num)
		throws Exception
	{
		super(tree.getTitle(), true, true, true, true);
		this.tree = tree;
		
		panel = new TreePanel(treePane, ss, tree);
		add(panel);
				
		// If the tree hasn't got a width/height set, we can assume it hasn't
		// been onscreen yet, and therefore needs packed() and resized
		if (tree.isNotInitialized())
		{
			pack();
			
			Dimension d = getSize();
			Point p = new Point(num * 20, num * 20);
					
			// Fix large sizes (shrink frame height to fit the screen)
			int height = TOPALi.winMain.getSize().height;
			if (d.height > height)
			{
				d.setSize(d.width, height - 120);
				setSize(d);
			}

			tree.setRectangle(new Rectangle(p.x, p.y, d.width, d.height));
		}
		else
		{		
			setSize(tree.width, tree.height);		
		}
		
		setLocation(tree.x, tree.y);
		setFrameIcon(Icons.TREE_NORMAL);
						
		addComponentListener(this);
	}
			
	public void componentMoved(ComponentEvent e)
		{ handleEvent(); }
				
	public void componentResized(ComponentEvent e)
		{ handleEvent(); }
	
	private void handleEvent()
	{
		tree.setRectangle(new Rectangle(getLocation(), getSize()));
		WinMainMenuBar.aFileSave.setEnabled(true);
	}
	
	public void componentHidden(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
}