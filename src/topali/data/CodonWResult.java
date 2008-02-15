// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import topali.gui.dialog.jobs.CodonWDialog;


public class CodonWResult extends AlignmentResult
{
	public String codonwPath;
	public String geneticCode = CodonWDialog.GENETICCODE_UNIVERSAL;
	public String result;
	
	public CodonWResult() {
		super();
		isResubmittable = true;
	}
	
	public CodonWResult(int id) {
		super(id);
		isResubmittable = true;
	}
}
