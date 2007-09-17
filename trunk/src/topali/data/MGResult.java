// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.util.*;

import sun.nio.cs.ext.MacGreek;

public class MGResult extends AlignmentResult 
{
	public String mgPath;
	public ArrayList<SubstitutionModel> models = new ArrayList<SubstitutionModel>();
	public String javaPath;
	
	public MGResult() {
		super();
	}
	
	public MGResult(int id) {
		super(id);
	}
	
	public SubstitutionModel getModel(String name) {
		SubstitutionModel res = null;
		
		for(SubstitutionModel m : models) {
			if(m.name.equals(name)) {
				res = m;
				break;
			}
		}
		
		return res;
	}

	public void sortByAIC1() {
		Collections.sort(models, new ModelsComparator(0));
	}
	
	public void sortByAIC2() {
		Collections.sort(models, new ModelsComparator(1));
	}
	
	public void sortByBIC() {
		Collections.sort(models, new ModelsComparator(2));
	}
	
	public void sortByLNL() {
		Collections.sort(models, new ModelsComparator(3));
	}
	
	/**
		 * toString methode: creates a String representation of the object
		 * @return the String representation
		 * @author info.vancauwenberge.tostring plugin
	
		 */
		@Override
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			for(int i=0; i<models.size(); i++)
				buffer.append(i+" "+(models.get(i)+"\n"));
			return buffer.toString();
		}
	
	class ModelsComparator implements Comparator<SubstitutionModel> {

		int by;
		
		public ModelsComparator(int sortBy) {
			this.by = sortBy;
		}
		
		public int compare(SubstitutionModel o1, SubstitutionModel o2)
		{
			int ret = 0;
			switch(by) {
			case 0 : ret = (o1.aic1>o2.aic1) ? 1 : -1; break;
			case 1 : ret = (o1.aic2>o2.aic2) ? 1 : -1; break;
			case 2 : ret = (o1.bic>o2.bic) ? 1 : -1; break;
			case 3 : ret = (o1.lnl<o2.lnl) ? 1 : -1; break;
			}
			
			return ret;
		}
		
	}
}
