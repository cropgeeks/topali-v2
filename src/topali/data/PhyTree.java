package topali.data;

import java.awt.*;
import java.io.*;

import pal.misc.*;
import pal.tree.*;

/* Storage class that represents a phylogenetic tree (stored as a NH format
 * string. Trees created from a SequenceSet alignment track their sequences in
 * safeName form (SEQ001 etc)
 */
public class PhyTree
{
	public static final int NORMAL = 0;
	public static final int CIRCULAR = 1;
	public static final int TEXTUAL = 2;
	
	// The tree bit
	private String treeStr;
	
	// Additional variables required when this tree is displayed
	public int viewMode = NORMAL;
	public boolean isSizedToFit;
	public int x = -9, y = -9;
	public int width = -9, height = -9;
	
	public PhyTree()
	{
	}
	
	public PhyTree(String treeStr)
		{ this.treeStr = treeStr; }

	public String getTreeStr()
		{ return treeStr; }
	
	public void setTreeStr(String treeStr)
		{ this.treeStr = treeStr; }
		

	public void setRectangle(Rectangle r)
	{
		x = r.getLocation().x;
		y = r.getLocation().y;
		width = r.getSize().width;
		height = r.getSize().height;
	}
	
	// Creates a PAL tree by first forming a tree from the NH string, then
	// parsing the tree to replace the safeNames with the actual names
	public Tree getDisplayablePALTree(SequenceSet ss)
		throws Exception
	{
		ReadTree palTree = new ReadTree(new PushbackReader(
			new StringReader(treeStr)));
		
		int count = palTree.getExternalNodeCount();		
		for (int i = 0; i < count; i++)
		{
			Node node = palTree.getExternalNode(i);
			
			String safeName = node.getIdentifier().getName();
			node.setIdentifier(new Identifier(ss.getNameForSafeName(safeName)));
		}
		
		return palTree;
	}
	
	public boolean isNotInitialized()
		{ return (x == -9 && y == -9 && width == -9 && height == -9); }
}
