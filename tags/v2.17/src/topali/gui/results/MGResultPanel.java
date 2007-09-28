// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.awt.print.Printable;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

import topali.data.*;
import topali.gui.*;

import doe.*;

@SuppressWarnings("unchecked")
public class MGResultPanel extends ResultPanel
{
	GradientPanel gp;
	TablePanel tp;
	
	JToggleButton filter;
	String tooltip1 = "Show all models";
	String tooltip2 = "Show only models, which are currently supported by TOPALi";
	
	public MGResultPanel(AlignmentData data, MGResult result) {
		super(data, result);
		tp = getTablePanel(result);
		updateTable(false);
		
		gp = new GradientPanel("Substitution Models (supported by TOPALi)");
		gp.setStyle(GradientPanel.OFFICE2003);
		
		TableRowSorter sorter = new TableRowSorter(tp.accessTable().getModel());
		for(int i=0; i<tp.accessTable().getColumnCount(); i++)
			sorter.setComparator(i, new SimpleComparator());
		tp.accessTable().setRowSorter(sorter);
		
		JPanel p1 = new JPanel(new BorderLayout());
		p1.add(gp, BorderLayout.NORTH);
		p1.add(tp);
		
		addContent(p1, false);
		
		filter = new JToggleButton();
		filter.setIcon(Icons.VISIBLE);
		filter.setToolTipText(tooltip1);
		filter.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				updateTable(filter.isSelected());
				String tt = filter.isSelected() ? tooltip2 : tooltip1;
				filter.setToolTipText(tt);
				
				if (filter.isSelected())
					gp.setTitle("Substitution Models (all results)");
				else
					gp.setTitle("Substitution Models (supported by TOPALi)");
			}
		});
		super.toolbar.add(filter);
	}
	
	public TablePanel getTablePanel(MGResult result) {		
		Vector<Object> colNames = new Vector<Object>();
		colNames.add("Model");
		colNames.add("\u2113");
		colNames.add("AIC\u2081");
		colNames.add("AIC\u2082");
		colNames.add("BIC");
		
		Vector<Object> rowData = new Vector<Object>(result.models.size());

		TablePanel tp = new TablePanel(rowData, colNames, TablePanel.RIGHT);
		return tp;
	}
	
	private void updateTable(boolean showAll) {
		HashSet<String> suppModels = new HashSet<String>();
		//dna
		suppModels.add("JC");
		suppModels.add("K80");
		suppModels.add("F81");
		suppModels.add("HKY");
		suppModels.add("GTR");
		//protein
		suppModels.add("MtMam");
		suppModels.add("MTRev");
		suppModels.add("RTRev");
		suppModels.add("VT");
		suppModels.add("CPRev");
		suppModels.add("Blosum");
		suppModels.add("JTT");
		suppModels.add("DAY");
		suppModels.add("WAG");
		
		DefaultTableModel mod = (DefaultTableModel)tp.accessTable().getModel();
		while(mod.getRowCount()>0)
			mod.removeRow(0);
		
		MGResult result = (MGResult)super.result;
		for(SubstitutionModel m : result.models) {
			Vector<Object> row = new Vector<Object>();
			String name = m.getName();
			
			if(!showAll) {
				String[] tmp = name.split("\\+");
				if(!suppModels.contains(tmp[0]))
					continue;
			}
			
			row.add(name);
			row.add(Prefs.d2.format(m.getLnl()));
			row.add(Prefs.d2.format(m.getAic1()));
			row.add(Prefs.d2.format(m.getAic2()));
			row.add(Prefs.d2.format(m.getBic()));
			mod.addRow(row);
		}
	}
	
	@Override
	public String getAnalysisInfo()
	{
		StringBuffer sb = new StringBuffer();
		
		sb.append("\nApplication: ModelGenerator (Version 0.84)\n");
		sb.append("Thomas M Keane, Christopher J Creevey , Melissa M Pentony, Thomas J Naughton\n" +
				"and James O McInerney (2006) Assessment of methods for amino acid matrix selection\n" +
				"and their use on empirical data shows that ad hoc assumptions for choice of matrix\n" +
				"are not justified, BMC Evolutionary Biology, 6:29");
		
		return sb.toString();
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
				res = (d1>d2) ? 1 : -1;
			} catch (NumberFormatException e)
			{
				res = o1.toString().compareTo(o2.toString());
			}
			
			return res;
		}
		
	}
}
