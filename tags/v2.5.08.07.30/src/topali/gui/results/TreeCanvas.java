// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.Graphics;
import java.io.*;

import javax.swing.JPanel;

import org.apache.log4j.Logger;

import pal.gui.*;
import pal.tree.*;

public class TreeCanvas extends JPanel
{
	Logger log = Logger.getLogger(this.getClass());
	
	private NameColouriser nc;
	
	Tree tree;
	
	public TreeCanvas() {
		
	}

	public TreeCanvas(TreeCanvas canv) {
		this.tree = canv.tree;
		this.nc = canv.nc;
	}
	
	public void setColouriser(NameColouriser nc) {
		this.nc = nc;
	}
	
	public void setTree(String tree) {
		if(tree!=null && !tree.equals("")) {
			PushbackReader pbread = new PushbackReader(new StringReader(tree));
			try
			{
				this.tree = new ReadTree(pbread);
			} catch (TreeParseException e)
			{
				log.warn("Error parsing tree.", e);
			}
		}
	}
	
	
	public void paint(Graphics g)
	{
		super.paintComponent(g);
		
		if(this.tree!=null) {
			int w = getWidth();
			int h = getHeight();
			TreePainterNormal painter = new TreePainterNormal(this.tree, "", false);
			if(this.nc!=null)
				painter.setColouriser(nc);
			else
				painter.setUsingColor(false);
			painter.setPenWidth(1);
			painter.paint(g, w, h);
			//System.out.println(this.tree.toString());
		}
	}
	
}
