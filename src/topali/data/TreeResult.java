package topali.data;

import java.awt.*;
import java.io.*;
import java.util.*;

import pal.misc.*;
import pal.tree.*;

/* Storage class that represents a phylogenetic tree (stored as a NH format
 * string. Trees created from a SequenceSet alignment track their sequences in
 * safeName form (SEQ001 etc)
 */
public class TreeResult extends AnalysisResult
{
	// Codes for deciding how to display the tree
	public static final int NORMAL = 0;
	public static final int CIRCULAR = 1;
	public static final int TEXTUAL = 2;	// newhamshire txt
	public static final int CLUSTER = 3;	// clustering details
	
	// The tree bit
	private String treeStr;
	// What partition of the original alignment was used to created it?
	private int pS, pE;
	
	// This tree *may* be associated with one or more sequence clusters
	private LinkedList<SequenceCluster> clusters = null;
	
	// Additional variables required when this tree is displayed
	public int viewMode = NORMAL;
	public boolean isSizedToFit;
	public int x = -9, y = -9;
	public int width, height;
	
	public TreeResult()
	{
		clusters = new LinkedList<SequenceCluster>();
	}
	
	public TreeResult(String treeStr)
		{ this.treeStr = treeStr; }

	public String getTreeStr()
		{ return treeStr; }
	
	public void setTreeStr(String treeStr)
		{ this.treeStr = treeStr; }
	
	public LinkedList<SequenceCluster> getClusters()
		{ return clusters; }
	
	public void setClusters(LinkedList<SequenceCluster> clusters)
		{ this.clusters = clusters; }
	
	public int getPartitionStart()
		{ return pS; }
	public void setPartitionStart(int pS)
		{ this.pS = pS; }
	
	public int getPartitionEnd()
		{ return pE; }
	public void setPartitionEnd(int pE)
		{ this.pE = pE; }

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
		{ return (x == -9 && y == -9); }
	
	/* Returns a suitable title for any windows displaying this tree */
	public String getTitle()
	{
		if (guiName != null && (pS!=0 && pE!=0))
			return guiName + " (" + pS + " - " + pE + ")";
		else if (guiName != null)
			return guiName;
		else
			return "Unknown Tree";
	}
}
