// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.print.Printable;
import java.lang.reflect.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableModel;

import topali.data.*;
import topali.data.models.*;
import topali.gui.*;
import topali.var.utils.*;

@SuppressWarnings("unchecked")
public class MTResultPanel extends ResultPanel implements ListSelectionListener
{
	GradientPanel gp;
	TablePanel tp;
	ModelInfoPanel infoPanel;

	Model selModel = null;

	public MTResultPanel(AlignmentData data, ModelTestResult result) {
		super(data, result);
		JPanel p1 = new JPanel(new BorderLayout());

		gp = new GradientPanel("Substitution Models for "+result.type);
		gp.setStyle(GradientPanel.OFFICE2003);
		p1.add(gp, BorderLayout.NORTH);

		if(SysPrefs.javaVersion<6) {
			result.sortModels(ModelComparator.BIC);
		}

		tp = getTablePanel(result);
		tp.accessTable().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tp.accessTable().getSelectionModel().addListSelectionListener(this);

		try {
			if(SysPrefs.javaVersion>5) {
				//even if this code is never called if java < 6, I can't use TableRowSorter directly
				//(strange: But I can use RadialGradientPaint in DNA- and ProteinModelDiagramm)
				Class clazz = Class.forName("javax.swing.table.TableRowSorter");
				Constructor construct = clazz.getConstructor(TableModel.class);
				Method setComparator = clazz.getMethod("setComparator", int.class, Comparator.class);
				Method toggleSortOrder = clazz.getMethod("toggleSortOrder", int.class);
				Method setRowSorter = Class.forName("javax.swing.JTable").getMethod("setRowSorter", Class.forName("javax.swing.RowSorter"));
				Object sorter = construct.newInstance(tp.accessTable().getModel());
				for(int i=0; i<tp.accessTable().getColumnCount(); i++)
					setComparator.invoke(sorter, i, new SimpleComparator());
				setRowSorter.invoke(tp.accessTable(), sorter);
				toggleSortOrder.invoke(sorter, 5);
			}

			p1.add(tp, BorderLayout.CENTER);

			infoPanel = new ModelInfoPanel(data,this, result);

			JScrollPane sp = new JScrollPane(infoPanel);
			p1.add(sp, BorderLayout.SOUTH);

			addContent(p1, false);

			//pre-select the first entry
			tp.accessTable().getSelectionModel().setSelectionInterval(0, 0);
			valueChanged(null);

			if (((ModelTestResult)result).splitType == ModelTestResult.SINGLE_MODEL_RUN)
				modelSetTo(data.getSequenceSet().getProps().getModel());
			else if (((ModelTestResult)result).splitType == ModelTestResult.CP_MODEL_RUN_CP1)
				modelSetTo(data.getSequenceSet().getProps().getCpModel1());
			else if (((ModelTestResult)result).splitType == ModelTestResult.CP_MODEL_RUN_CP2)
				modelSetTo(data.getSequenceSet().getProps().getCpModel2());
			else if (((ModelTestResult)result).splitType == ModelTestResult.CP_MODEL_RUN_CP3)
				modelSetTo(data.getSequenceSet().getProps().getCpModel3());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ModelTestResult getResult() {
		return (ModelTestResult)this.result;
	}

	public void modelSetTo(Model mod) {
		String name = mod.getIGName();

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
		Model bestAIC1 = ModelUtils.getBestModel(result.models, ModelUtils.CRIT_AIC1);
		Model bestAIC2 = ModelUtils.getBestModel(result.models, ModelUtils.CRIT_AIC2);
		Model bestBIC = ModelUtils.getBestModel(result.models, ModelUtils.CRIT_BIC);
		//Model bestLRT = ModelUtils.getBestModel(result.models, ModelUtils.CRIT_LNL);
		Model best5SHLRT = null;
		Model bestHLRT1 = null;
		Model bestHLRT2 = null;
		Model bestHLRT3 = null;
		Model bestHLRT4 = null;
		if(data.getSequenceSet().getProps().isNucleotides()) {
			if(result.type.equals(ModelTestResult.TYPE_PHYML)) {
				best5SHLRT = ModelUtils.perform5HTHLRT(result.models, 0.01);
			}
			if(result.type.equals(ModelTestResult.TYPE_MRBAYES)) {
				bestHLRT1 = ModelUtils.performHLRT1(result.models, 0.01);
				bestHLRT2 = ModelUtils.performHLRT2(result.models, 0.01);
				bestHLRT3 = ModelUtils.performHLRT3(result.models, 0.01);
				bestHLRT4 = ModelUtils.performHLRT4(result.models, 0.01);
			}
		}

		Vector<String> colNames = new Vector<String>();
		colNames.add("Model");
		colNames.add("K");
		colNames.add("-\u2113");
		colNames.add("AIC\u2081");
		colNames.add("AIC\u2082");
		colNames.add("BIC");

		Vector<String> tips = new Vector<String>();
		tips.add("<html>Model Name<br>(click to sort)</html>");
		tips.add("<html>Number of Parameters<br>(click to sort)</html>");
		tips.add("<html>- Log Likelihood<br>(click to sort)</html>");
		tips.add("<html>Akaike information criterion<br>(R.F. distance: Best vs All)<br>(click to sort)</html>");
		tips.add("<html>AIC with second order correction<br>(R.F. distance: Best vs All)<br>(click to sort)</html>");
		tips.add("<html>Bayesian information criterion<br>(R.F. distance: Best vs All)<br>(click to sort)</html>");

		Vector<Vector<Object>> rowData = new Vector<Vector<Object>>(result.models.size());
		int i = 0;
		for(Model m : result.models) {
			Vector<Object> row = new Vector<Object>();

			String name = m.getIGName();

			int df = m.getFreeParameters();
			if(m.isGamma())
				df++;
			if(m.isInv())
				df++;

			String col1 = Utils.d2.format((m.getLnl()*(-1)));
			//col1 += " ("+getDistance(bestLRT.getIGName(), m.getIGName(), result)+")";
			if(data.getSequenceSet().getProps().isNucleotides()) {
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

			String col2 = Utils.d2.format(m.getAic1());
			col2 += " ("+getDistance(bestAIC1.getIGName(), m.getIGName(), result)+")";
			if(bestAIC1.matches(m)) {
				Color c = new Color(240,240,100);
				col2 += "<color="+c.getRed()+","+c.getGreen()+","+c.getBlue()+">";
				col2 +="<tooltip=\"Best Model\">";
			}

			String col3 = Utils.d2.format(m.getAic2());
			col3 += " ("+getDistance(bestAIC2.getIGName(), m.getIGName(), result)+")";
			if(bestAIC2.matches(m)) {
				Color c = new Color(240,240,100);
				col3 += "<color="+c.getRed()+","+c.getGreen()+","+c.getBlue()+">";
				col3 +="<tooltip=\"Best Model\">";
			}

			String col4 = Utils.d2.format(m.getBic());
			col4 += " ("+getDistance(bestBIC.getIGName(), m.getIGName(), result)+")";
			if(bestBIC.matches(m)) {
				Color c = new Color(240,240,100);
				col4 += "<color="+c.getRed()+","+c.getGreen()+","+c.getBlue()+">";
				col4 +="<tooltip=\"Best Model\">";

				setModel(m);
			}

			row.add(name);
			row.add(""+df);
			row.add(col1);
			row.add(col2);
			row.add(col3);
			row.add(col4);
			rowData.add(row);

			i++;
		}

		TablePanel tp = new TablePanel(rowData, colNames, "table");
		((CustomTable)tp.accessTable()).setHeaderToolTips(tips);
		tp.accessTable().getColumnModel().getColumn(1).setMaxWidth(50);


		return tp;
	}

	void setModel(Model m)
	{
		ModelTestResult r = (ModelTestResult) result;

		// If it was just a single run of model selection, set a single model
		if (r.splitType == ModelTestResult.SINGLE_MODEL_RUN)
			data.getSequenceSet().getProps().setModel(m);
		// Set the model for the CP region that this result corresponds to
		else if (r.splitType == ModelTestResult.CP_MODEL_RUN_CP1)
			data.getSequenceSet().getProps().setCpModel1(m);
		else if (r.splitType == ModelTestResult.CP_MODEL_RUN_CP2)
			data.getSequenceSet().getProps().setCpModel2(m);
		else if (r.splitType == ModelTestResult.CP_MODEL_RUN_CP3)
			data.getSequenceSet().getProps().setCpModel3(m);

		ProjectState.setDataChanged();
	}

	private int getDistance(String mod1, String mod2, ModelTestResult result) {
		for(Distance<String> d : result.rfDistances) {
			if(d.getObj1().equals(mod1) && d.getObj2().equals(mod2)) {
				return (int)d.getDistance();
			}
		}
		return -1;
	}


	public String getAnalysisInfo()
	{
		ModelTestResult result = (ModelTestResult)this.result;

		StringBuffer sb = new StringBuffer();
		sb.append(result.guiName);

		sb.append("\n\nRuntime: " + ((result.endTime - result.startTime) / 1000)
				+ " seconds");

		sb.append("\n\nSelected sequences:");

		for (String seq : result.selectedSeqs)
			sb.append("\n  " + data.getSequenceSet().getNameForSafeName(seq));

		sb.append("\n\n");
		sb.append("Algorithm: Maximum Likelihood (PhyML)\n");
		sb.append("Application: PhyML-aLRT (Version 2.4.5)\n");
		 sb.append("M. Anisimova, O. Gascuel (2006), Approximate likelihood ratio\n" +
		 		"test for branchs: A fast, accurate and powerful alternative,\n" +
		 		"Systematic Biology, 55(4), pp 539-552.\n");
		 sb.append("Guindon S, Gascuel O. (2003), A simple, fast, and accurate\n" +
		 		"algorithm to estimate large phylogenies by maximum likelihood,\n" +
		 		"Systematic Biology. 52(5) pp 696-704. ");
		return sb.toString();
	}


	public Printable[] getPrintables()
	{
		return new Printable[] {tp.getPrintable()};
	}


	public void setThreshold(double t)
	{
		//no threshold to set
	}



	public void valueChanged(ListSelectionEvent e)
	{
		if(e==null || e.getValueIsAdjusting())
			return;

		int index = tp.accessTable().getSelectedRow();
		if(index<0)
			return;

		String modName = (String) tp.accessTable().getValueAt(index, 0);
		String[] tmp = modName.split("\\+");
		tmp[0] = tmp[0].replaceAll("\\<.*\\>", "");
		boolean gamma = (modName.contains("+G"));
		boolean inv = (modName.contains("+I"));
		this.selModel = ModelUtils.getModel(tmp[0], gamma, inv, ((ModelTestResult)result).models);

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
