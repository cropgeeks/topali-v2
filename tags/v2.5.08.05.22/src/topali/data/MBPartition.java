// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import topali.data.models.Model;

public class MBPartition
{
	public String indeces;
	public String name;
	public Model model;
	
	public MBPartition() {
		
	}
	
	public MBPartition(String indeces, String name, Model model)
	{
		this.indeces = indeces;
		this.name = name;
		this.model = model;
	}
}
