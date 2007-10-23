// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.print.Printable;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableRowSorter;

import topali.data.*;
import topali.data.models.Model;
import topali.gui.Prefs;
import topali.var.*;
import doe.GradientPanel;

@SuppressWarnings("unchecked")
public class MTResultPanel extends ResultPanel implements ListSelectionListener
{
	GradientPanel gp;
	TablePanel tp;
	ModelInfoPanel infoPanel;
	
	int select = -1;
	Model selModel = null;
	
	public MTResultPanel(AlignmentData data, ModelTestResult result) {
		super(data, result);
		JPanel p1 = new JPanel(new BorderLayout());
		
		gp = new GradientPanel("Substitution Models for "+result.selection);
		gp.setStyle(GradientPanel.OFFICE2003);
		p1.add(gp, BorderLayout.NORTH);
		
		tp = getTablePanel(result);
		tp.accessTable().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tp.accessTable().getSelectionModel().addListSelectionListener(this);
		TableRowSorter sorter = new TableRowSorter(tp.accessTable().getModel());
		for(int i=0; i<tp.accessTable().getColumnCount(); i++)
			sorter.setComparator(i, new SimpleComparator());
		tp.accessTable().setRowSorter(sorter);
		p1.add(tp, BorderLayout.CENTER);
		
		infoPanel = new ModelInfoPanel(data,this);
		p1.add(infoPanel, BorderLayout.SOUTH);
		
		addContent(p1, false);
	
		if(select>=0) {
			tp.accessTable().getSelectionModel().setSelectionInterval(select, select);
			valueChanged(null);
			modelSetTo(data.getSequenceSet().getParams().getModel());
			sorter.toggleSortOrder(5);
		}
	}
	
	public void modelSetTo(Model mod) {
		String name = mod.getName();
		if(mod.isInv())
			name += "+I";
		if(mod.isGamma())
			name += "+G";
		
		for(int i=0; i<tp.accessTable().getRowCount(); i++) {
			String name2 = (String)tp.accessTable().getValueAt(i, 0);
			if(name.equals(name2)) {
				tp.accessTable().setValueAt(name2+"<b>", i, 0);
			}
			else {
				tp.accessTable().setValueAt(name2.replaceAll("\\<b\\>", ""), i, 0);
			}
		}
	}
	
	public TablePanel getTablePanel(ModelTestResult result) {
		Model bestAIC1 = ModelUtils.getBestAIC1Model(result.models);
		Model bestAIC2 = ModelUtils.getBestAIC2Model(result.models, data.getSequenceSet().getLength());
		Model bestBIC = ModelUtils.getBestBICModel(result.models, data.getSequenceSet().getLength());
		Model best5SHLRT = null;
		Model bestHLRT1 = null;
		Model bestHLRT2 = null;
		Model bestHLRT3 = null;
		Model bestHLRT4 = null;
		if(data.getSequenceSet().isDNA()) {
			if(result.selection.equals(ModelTestResult.PHYML)) {
				best5SHLRT = ModelUtils.perform5HTHLRT(result.models, 0.01);
			}
			if(result.selection.equals(ModelTestResult.MRBAYES)) {
				bestHLRT1 = ModelUtils.performHLRT1(result.models, 0.01);
				bestHLRT2 = ModelUtils.performHLRT2(result.models, 0.01);
				bestHLRT3 = ModelUtils.performHLRT3(result.models, 0.01);
				bestHLRT4 = ModelUtils.performHLRT4(result.models, 0.01);
			}
		}
		
		Vector<Object> colNames = new Vector<Object>();
		colNames.add("Model");
		colNames.add("\u2113");
		colNames.add("K");
		colNames.add("AIC\u2081");
		colNames.add("AIC\u2082");
		colNames.add("BIC");
		
		Vector<Object> rowData = new Vector<Object>(result.models.size());
		int i = 0;
		for(Model m : result.models) {
			Vector<Object> row = new Vector<Object>();
			
			String name = m.getName();
			if(m.isInv())
				name += "+I";
			if(m.isGamma())
				name +="+G";
			
			int df = m.getFreeParameters();
			if(m.isGamma())
				df++;
			if(m.isInv())
				df++;
			
			int n = data.getSequenceSet().getLength();
			
			double aic1 = MathUtils.calcAIC1(m.getLnl(), df);
			double aic2 = MathUtils.calcAIC2(m.getLnl(), df, n);
			double bic = MathUtils.calcBIC(m.getLnl(), df, n);
			
			row.add(name);
			
			String col1 = Prefs.d2.format(m.getLnl());
			if(data.getSequenceSet().isDNA()) {
				String tt = "Best Model according to ";
				int bestModel = 0;
				if(best5SHLRT!=null && best5SHLRT.matches(m)) {
					bestModel++;
					tt += "5HT hLRT; ";
				}
				if(bestHLRT1!=null && bestHLRT1.matches(m)) {
					bestModel++;
					tt +="PCN hLRT1; ";
				}
				if(bestHLRT2!=null && bestHLRT2.matches(m)) {
					bestModel++;
					tt +="PCN hLRT2; ";
				}
				if(bestHLRT3!=null && bestHLRT3.matches(m)) {
					bestModel++;
					tt +="PCN hLRT3; ";
				}
				if(bestHLRT4!=null && bestHLRT4.matches(m)) {
					bestModel++;
					tt +="PCN hLRT4; ";
				}
				Color c = null;
				if(bestModel>=1)
					c = new Color(240,240,100);
				if(c!=null) {
					col1+="<color="+c.getRed()+","+c.getGreen()+","+c.getBlue()+">";
					col1+="<tooltip=\""+tt+"\">";
				}
			}
			
			String col2 = Prefs.d2.format(aic1);
			if(bestAIC1.matches(m)) {
				Color c = new Color(240,240,100);
				col2 += "<color="+c.getRed()+","+c.getGreen()+","+c.getBlue()+">";
				col2 +="<tooltip=\"Best Model\">";
			}
			
			String col3 = Prefs.d2.format(aic2);
			if(bestAIC2.matches(m)) {
				Color c = new Color(240,240,100);
				col3 += "<color="+c.getRed()+","+c.getGreen()+","+c.getBlue()+">";
				col3 +="<tooltip=\"Best Model\">";
			}
			
			String col4 = Prefs.d2.format(bic);
			if(bestBIC.matches(m)) {
				Color c = new Color(240,240,100);
				col4 += "<color="+c.getRed()+","+c.getGreen()+","+c.getBlue()+">";
				col4 +="<tooltip=\"Best Model\">";
				select = i;
				data.getSequenceSet().getParams().setModel(m);
			}
			
			row.add(col1);
			
			row.add(""+df);
			
			row.add(col2);
			row.add(col3);
			row.add(col4);
			rowData.add(row);
			
			i++;
		}
		
		TablePanel tp = new TablePanel(rowData, colNames, TablePanel.RIGHT);
		
		tp.accessTable().getColumnModel().getColumn(2).setMaxWidth(50); 
		
		
		
		return tp;
	}
	
	@Override
	public String getAnalysisInfo()
	{
		StringBuffer sb = new StringBuffer();
		
		return sb.toString();
	}

	@Override
	public Printable[] getPrintables()
	{
		return new Printable[] {tp.getPrintable()};
	}

	@Override
	public void setThreshold(double t)
	{
		//no threshold to set
	}
	
	
	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		int index = tp.accessTable().getSelectedRow();
		if(index<0)
			return;
		
		String modName = (String) tp.accessTable().getValueAt(index, 0);
		String[] tmp = modName.split("\\+");
		boolean gamma = (modName.contains("+G"));
		boolean inv = (modName.contains("+I"));
		for(Model m : ((ModelTestResult)result).models) {
			if(m.getName().equals(tmp[0]) && m.isGamma()==gamma && m.isInv()==inv) {
				this.selModel = m;
				break;
			}
		}
		
		infoPanel.setModel(this.selModel);
	}


	class SimpleComparator implements Comparator {

		public int compare(Object o1, Object o2)
		{
			int res = 0;
			
			try
			{
				String s1 = o1.toString();
				String s2 = o2.toString();
				s1 = s1.replaceAll("\\<.*\\>", "");
				s2 = s2.replaceAll("\\<.*\\>", "");
				double d1 = Double.parseDouble(s1);
				double d2 = Double.parseDouble(s2);
				res = (d1>d2) ? 1 : -1;
			} catch (NumberFormatException e)
			{
				res = o1.toString().compareTo(o2.toString());
			}
			
			return res;
		}
		
	}

}
