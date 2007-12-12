// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.util.Vector;

public class RaxmlResult extends TreeResult
{	
	public static final String RH_CAT = "CAT";
	public static final String RH_MIX = "MIX";
	public static final String RH_GAMMA = "GAMMA";
	
	public String raxmlPath;
	public String rateHet = "";
	public boolean empFreq = false;
	public int bootstrap = -1;
	public Vector<RaxPartition> partitions = new Vector<RaxPartition>();
	
	public RaxmlResult()
	{
		super();
		isResubmittable = true;
	}
	public RaxmlResult(int id)
	{
		super(id);
		isResubmittable = true;
	}
}
