// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.util.Vector;

/*
 * Class that stores both the results from running MrBayes and the
 * settings required to make the run (although not the data itself).
 */
public class MBTreeResult extends TreeResult 
{
	// The location of the MrBayes binary
	public String mbPath;
	public int nRuns = 2;
	public int nGen = 100000;
	public double burnin = 0.25;
	public int sampleFreq = 10;
	public String summary = "";
	public Vector<MBPartition> partitions = new Vector<MBPartition>();
	public Vector<String> linkedParameters = new Vector<String>();
	
	public MBTreeResult()
	{
		super();
		isResubmittable = true;
	}
	
	public MBTreeResult(int id) {
		super(id);
		isResubmittable = true;
	}
}