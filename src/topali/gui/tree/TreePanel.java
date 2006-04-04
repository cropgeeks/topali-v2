// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.tree;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.print.*;
import java.util.*;
import javax.swing.*;

import pal.tree.*;
import pal.gui.*;

import topali.data.*;
import topali.gui.*;

class TreePanel extends JPanel
{
	// Reference back to the SequenceSet containing the sequences in this tree
	private SequenceSet ss;
	
	// The TOPALi internal tree class
	private TreeResult tree;
	// The PAL palTree class
	private Tree palTree;
	
	private JScrollPane sp;
	private JTextArea txtNewHamp;
	private NameColouriser nc;
	
	TreeCanvas canvas;
	TreePanelToolBar toolbar;	
	
	TreePanel(TreePane treePane, SequenceSet ss, TreeResult tree)
		throws Exception
	{
		this.ss = ss;
		this.tree = tree;
		
		palTree = tree.getDisplayablePALTree(ss);
		createNewHampshireText();
		createNameColouriser();
		
		// Initialise the GUI controls
		canvas = new TreeCanvas(palTree);
		toolbar = new TreePanelToolBar(treePane, this, tree);
		sp = new JScrollPane(canvas);
		sp.getHorizontalScrollBar().setUnitIncrement(25);
		sp.getVerticalScrollBar().setUnitIncrement(25);
		
		setLayout(new BorderLayout());
		add(toolbar, BorderLayout.NORTH);
		add(sp);
		
		// Which view should be shown at startup?
		setViewMode(tree.viewMode);
	}
	
	private void createNewHampshireText()
	{
		txtNewHamp = new JTextArea();
		Utils.setTextAreaDefaults(txtNewHamp);
		
		try { txtNewHamp.setText(palTree.toString()); }
		catch (Exception e) {}
	}
	
	// Uses SequenceCluster information to create a PAL NameColouriser object
	// that can be used to colour each sequence in the tree according to the
	// cluster that it belongs to.
	private void createNameColouriser()
	{
		if (tree.getClusters() == null)
			return;
		
		nc = new NameColouriser();
		int i = 0;
		
		for (SequenceCluster cluster: tree.getClusters())
		{
			Color c = Utils.getColor(i);
			for (String seq: cluster.getSequences())
				nc.addMapping(seq, c);
			
			i++;
		}
	}
	
	TreeResult getTreeResult()
		{ return tree; }
	
	Tree getPalTree()
		{ return palTree; }
	
	SequenceSet getSequenceSet()
		{ return ss; }
	
	BufferedImage getSavableImage()
		{ return canvas.getSavableImage(); }
	
	void setSizedToFit(boolean sizedToFit)
	{
		tree.isSizedToFit = sizedToFit;
		canvas.setTree(palTree);
		sp.getViewport().setView(sp.getViewport().getView());
	}
	
	void setViewMode(int viewMode)
	{
		tree.viewMode = viewMode;
		
		// Viewing new hampshire details...
		if (viewMode == tree.TEXTUAL)
			sp.getViewport().setView(txtNewHamp);
		// Or viewing the tree itself
		else
		{
			canvas.setTree(palTree);
			sp.getViewport().setView(canvas);
		}
	}
	
	void setClusters(LinkedList<SequenceCluster> clusters)
	{
		tree.setClusters(clusters);
		createNameColouriser();
	}
	
	// Internal canvas class that actually handles the painting of the palTree
	class TreeCanvas extends JPanel implements Printable
	{
		private TreePainter painter;
		private Dimension dimension;
		
		TreeCanvas(Tree njTree)
			{ setTree(njTree); }
		
		
		void setTree(Tree newTree)
		{
			if (tree.viewMode == tree.CIRCULAR)
				painter = new TreePainterCircular(newTree, "", false);
			else
				painter = new TreePainterNormal(newTree, "", false);
			
			painter.setUsingColor(false);
			if (nc == null)
				painter.setColouriser(ss.getNameColouriser(Prefs.gui_color_seed));
			else
				painter.setColouriser(nc);
			
			dimension = new Dimension(painter.getPreferredSize().width + 50,
				painter.getPreferredSize().height + 50);
		}

		public Dimension getPreferredSize()
		{
			if (tree.isSizedToFit)
				return new Dimension(0, 0);
			else
				return dimension;
		}
		
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			
			if (tree.isSizedToFit)
				doPainting(g, sp.getSize().width, sp.getSize().height);
			else
				doPainting(g, getSize().width, getSize().height);
		}
		
		private void doPainting(Graphics g, int width, int height)
		{
			setTree(palTree);

			painter.setPenWidth(1);
			painter.paint(g, width, height);
		}
		
		public int print(Graphics graphics, PageFormat pf, int pageIndex)
		{
			Graphics2D g2 = (Graphics2D) graphics;
					
			double panelWidth = pf.getImageableWidth();		//width in pixels
			double panelHeight = dimension.height;			//height in pixels
			
			double pageHeight = pf.getImageableHeight();	//height of printer page
			double pageWidth = pf.getImageableWidth();		//width of printer page
			
			// Scale factor (1:1 for printing trees)
			double scale = pageWidth/panelWidth;
			int totalNumPages = (int)Math.ceil(scale * panelHeight / pageHeight);
			
			// Make sure empty pages aren't printed
			if (pageIndex >= totalNumPages)
				return NO_SUCH_PAGE;
			
			// Shift Graphic to line up with beginning of print-imageable region
//			if (pageIndex == 0)
				g2.translate(pf.getImageableX(), pf.getImageableY());
			// Shift Graphic to line up with beginning of next page to print
//			else
				g2.translate(0f, -pageIndex * pageHeight);
	
			doPainting(g2, (int)panelWidth, (int)panelHeight);
			
			return PAGE_EXISTS;
		}

		// Creates a BufferedImage and draws the tree directly onto it
		BufferedImage getSavableImage()
		{
			int w = dimension.width;
			int h = dimension.height;
			
			BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			
			g.setColor(Color.white);
			g.fillRect(0, 0, w, h);
			doPainting(g, w, h);
			g.dispose();
			
			return image;
		}
	}
}