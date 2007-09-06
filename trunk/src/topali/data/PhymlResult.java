// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.io.Serializable;

public class PhymlResult extends TreeResult implements Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4370753010007394293L;
	public String phymlPath;
	public int bootstrap = 0;
	public boolean optTopology = false;
	public boolean optBranchPara = false;
	
	public String[] phymlParameters;
	
}
