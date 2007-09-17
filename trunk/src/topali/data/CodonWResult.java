// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;


public class CodonWResult extends AlignmentResult
{
	public String codonwPath;
	public String geneticCode = SequenceSetParams.GENETICCODE_UNIVERSAL;
	public String result;
	
	public CodonWResult() {
		super();
	}
	
	public CodonWResult(int id) {
		super(id);
	}
}
