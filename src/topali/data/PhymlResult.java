// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import topali.data.models.Model;

public class PhymlResult extends TreeResult 
{

	public String phymlPath;
	public int bootstrap = 0;
	
	public Model model;
	
	public PhymlResult() {
		super();
		isResubmittable = true;
	}
	
	public PhymlResult(int id) {
		super(id);
		isResubmittable = true;
	}
}
