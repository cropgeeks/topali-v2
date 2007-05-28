// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.print.Printable;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.table.TableRowSorter;

import topali.data.*;
import topali.gui.Prefs;

@SuppressWarnings("unchecked")
public class MGResultPanel extends ResultPanel
{

	TablePanel tp;
	
	public MGResultPanel(AlignmentData data, MGResult result) {
		super(data, result);
		tp = getTablePanel(result);
		
		TableRowSorter sorter = new TableRowSorter(tp.accessTable().getModel());
		for(int i=0; i<tp.accessTable().getColumnCount(); i++)
			sorter.setComparator(i, new SimpleComparator());
		tp.accessTable().setRowSorter(sorter);
		
		addContent(tp, false);
	}
	
	public TablePanel getTablePanel(MGResult result) {
		Vector<Object> colNames = new Vector<Object>();
		colNames.add("Name");
		colNames.add("ln(L)");
		colNames.add("AIC1");
		colNames.add("AIC2");
		colNames.add("BIC");
		
		Vector<Object> rowData = new Vector<Object>(result.models.size());
		for(SubstitutionModel m : result.models) {
			Vector<Object> row = new Vector<Object>();
			String name = m.getName();
			row.add(name);
			row.add(Prefs.d2.format(m.getLnl()));
			row.add(Prefs.d2.format(m.getAic1()));
			row.add(Prefs.d2.format(m.getAic2()));
			row.add(Prefs.d2.format(m.getBic()));
			rowData.add(row);
		}
		
		TablePanel tp = new TablePanel(rowData, colNames, TablePanel.RIGHT);
		return tp;
	}
	@Override
	public String getAnalysisInfo()
	{
		return "";
	}

	@Override
	public Printable[] getPrintables()
	{
		return new Printable[] {tp};
	}

	@Override
	public void setThreshold(double t)
	{
		//no threshold to set
	}

	class SimpleComparator implements Comparator {

		public int compare(Object o1, Object o2)
		{
			int res = 0;
			
			try
			{
				double d1 = Double.parseDouble(o1.toString());
				double d2 = Double.parseDouble(o2.toString());
				res = (d1>d2) ? -1 : 1;
			} catch (NumberFormatException e)
			{
				res = o1.toString().compareTo(o2.toString());
			}
			
			return res;
		}
		
	}
}
