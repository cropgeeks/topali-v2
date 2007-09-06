// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.io.Serializable;

/*
 * Class that stores both the results from running MrBayes and the
 * settings required to make the run (although not the data itself).
 */
public class MBTreeResult extends TreeResult implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 202990938174176824L;
	// The location of the MrBayes binary
	public String mbPath;
	public int nGen = 100000;
	public double burnin = 0.25;
	public int sampleFreq = 10;
	public boolean isCDNA = false;
	
	public String nexusCommands;
	
	public MBTreeResult()
	{
	}
}