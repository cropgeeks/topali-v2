// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data.models;

import java.util.Comparator;

public class ModelComparator implements Comparator<Model>
{
	public static final int NAME = 0;
	public static final int LNL = 1;
	public static final int AIC1 = 2;
	public static final int AIC2 = 3;
	public static final int BIC = 4;
	
	int mode = NAME;
	
	public ModelComparator() {	
	}
	
	public ModelComparator(int mode) {
		this.mode = mode;
	}

	@Override
	public int compare(Model o1, Model o2)
	{
		switch(mode) {
		case NAME:
			return o1.getName().compareTo(o2.getName());
		case LNL:
			if(o1.getLnl()>o2.getLnl())
				return 1;
			else
				return -1;
		case AIC1:
			if(o1.getAic1()>o2.getAic1())
				return 1;
			else
				return -1;
		case AIC2:
			if(o1.getAic2()>o2.getAic2())
				return 1;
			else
				return -1;
		case BIC:
			if(o1.getBic()>o2.getBic())
				return 1;
			else
				return -1;
		}
		return 0;
	}

}
