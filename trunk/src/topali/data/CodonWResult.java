// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.io.Serializable;


public class CodonWResult extends AlignmentResult implements Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -331644372085211514L;
	public String codonwPath;
	public String geneticCode = SequenceSetParams.GENETICCODE_UNIVERSAL;
	public String result;
	
	public CodonWResult() {
		
	}
	
}
