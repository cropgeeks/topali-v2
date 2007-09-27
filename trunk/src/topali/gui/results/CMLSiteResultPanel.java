// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.print.Printable;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import org.apache.log4j.Logger;

import topali.data.*;
import topali.gui.Prefs;
import topali.var.MathUtils;

import doe.*;

/**
 * Panel for displaying codeml site model result
 */
public class CMLSiteResultPanel extends ResultPanel implements
		ListSelectionListener
{
	Logger log = Logger.getLogger(this.getClass());
	
	TablePanel table;
	JSplitPane sp;

	GraphPanel graph;
	JPanel blankPanel;

	public CMLSiteResultPanel(AlignmentData data, CodeMLResult result)
	{
		super(data, result);
		System.out.println(result);
		this.table = createTablePanel();
		this.graph = createGraphPanel();
	//	this.graph.setEnabled(false);

		blankPanel = new JPanel(new BorderLayout());
		blankPanel.add(
			new JLabel("(models predicting positive selection sites will display graph data here when selected)", JLabel.CENTER));

		sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		sp.setTopComponent(table);
		sp.setBottomComponent(blankPanel);

		GradientPanel gp = new GradientPanel("PAML/CodeML Sites Models");
		gp.setStyle(GradientPanel.OFFICE2003);
		JPanel p1 = new JPanel(new BorderLayout());
		p1.add(gp, BorderLayout.NORTH);
		p1.add(sp);

		addContent(p1, true);

		setThreshold(result.threshold);

		sp.setResizeWeight(0.3);
		sp.setDividerLocation(200);

	}

	private GraphPanel createGraphPanel()
	{
		GraphPanel p = new GraphPanel(data, (AlignmentResult)result, new double[][]
		{}, 1.05, GraphPanel.RIGHT);
		return p;
	}

	private TablePanel createTablePanel()
	{
		Vector<String> names = new Vector<String>();
		names.add("Model"); 
		names.add("\u2113");
		names.add("p\u2080"); //2
		names.add("p\u2081");
		names.add("p\u2082"); //4
		names.add("\u03C9"+"\u2080");
		names.add("\u03C9"+"\u2081"); //6
		names.add("\u03C9"+"\u2082");
		names.add("p"); //8 
		names.add("q");
		names.add("df"); //10
		names.add("-2\u2206L");
		names.add("Sig"); //12
		names.add("PSS");

		Vector<Vector<String>> data = getTableVector(((AlignmentResult)result).threshold);

		TablePanel p = new TablePanel(data, names, TablePanel.RIGHT);
		p.accessTable().getSelectionModel().addListSelectionListener(this);
		p.accessTable().getColumnModel().getColumn(0).setMinWidth(120);
		p.accessTable().getColumnModel().getColumn(1).setMaxWidth(60); 
		p.accessTable().getColumnModel().getColumn(2).setMaxWidth(40);
		p.accessTable().getColumnModel().getColumn(3).setMaxWidth(40); 
		p.accessTable().getColumnModel().getColumn(4).setMaxWidth(40);
		p.accessTable().getColumnModel().getColumn(5).setMaxWidth(40); 
		p.accessTable().getColumnModel().getColumn(6).setMaxWidth(40); 
		p.accessTable().getColumnModel().getColumn(7).setMaxWidth(40); 
		p.accessTable().getColumnModel().getColumn(8).setMaxWidth(40);
		p.accessTable().getColumnModel().getColumn(9).setMaxWidth(40);
		p.accessTable().getColumnModel().getColumn(10).setMaxWidth(40);
		p.accessTable().getColumnModel().getColumn(11).setMaxWidth(60);
		p.accessTable().getColumnModel().getColumn(12).setMaxWidth(60);
		p.accessTable().setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
		p.setBackground(Color.WHITE);
		return p;
	}

	private Vector<Vector<String>> getTableVector(double thres)
	{
		CodeMLResult result = (CodeMLResult) this.result;
		Vector<Vector<String>> data = new Vector<Vector<String>>();
		
		double ll = -1;
		int np = -1;
		
		for (int i=0; i<result.models.size(); i++)
		{
			CMLModel m = result.models.get(i);
			
			Vector<String> v = new Vector<String>();
			v.add(m.name+"("+m.nParameter+")");
			v.add(Prefs.d2.format(m.likelihood));
			if (m.p0 != -1)
				v.add(Prefs.d3.format(m.p0));
			else
				v.add("");
			if (m.p1 != -1)
				v.add(Prefs.d3.format(m.p1));
			else
				v.add("");
			if (m.p2 != -1)
				v.add(Prefs.d3.format(m.p2));
			else
				v.add("");
			if (m.w0 != -1)
				v.add(Prefs.d3.format(m.w0));
			else
				v.add("");
			if (m.w1 != -1)
				v.add(Prefs.d3.format(m.w1));
			else
				v.add("");
			if (m.w2 != -1)
				v.add(Prefs.d3.format(m.w2));
			else
				v.add("");
			if (m.p != -1)
				v.add(Prefs.d3.format(m.p));
			else
				v.add("");
			if (m.q != -1)
				v.add(Prefs.d3.format(m.q));
			else
				v.add("");

			if((i%2)==1 && np != -1) {
				try
				{
					int df = m.nParameter - np;
					double lr = MathUtils.calcLR(m.likelihood, ll);
					double lrt = MathUtils.calcLRT(lr, df);
					String lrtString = MathUtils.getRoughSignificance(lrt);
					v.add(""+df);
					v.add(Prefs.d3.format(lr));
					v.add(lrtString);
				} catch (RuntimeException e)
				{
					log.warn(e);
					v.add("");
					v.add("");
					v.add("");
				}
			}
			else {
				v.add("");
				v.add("");
				v.add("");
			}
			
			np = m.nParameter;
			ll = m.likelihood;
			
			if (m.supportsPSS)
			{
				List<PSSite> pss = m.getPSS(thres);
				StringBuffer sb = new StringBuffer();
				for (PSSite s : pss)
					sb.append("" + s.getPos() + s.getAa() + " ");
				v.add(sb.toString());
			} else
				v.add("--");

			data.add(v);
		}
		return data;
	}

	@Override
	public String getAnalysisInfo()
	{
		CodeMLResult result = (CodeMLResult) this.result;

		StringBuffer sb = new StringBuffer();
		sb.append(result.guiName+"\n\n");
		sb.append("Runtime: " + ((result.endTime - result.startTime) / 1000)+ " seconds\n");

		sb.append("Analysis type: Site model\n\n");

		sb.append("Selected models:\n\n");
		for (CMLModel m : result.models)
		{
			sb.append(m.toString() + "\n");
		}

		sb.append("\nSelected sequences:\n");
		for (String seq : result.selectedSeqs)
			sb.append("\n  " + data.getSequenceSet().getNameForSafeName(seq));

		sb.append("\n\nApplication: CodeML (PAML, Version 4)\n");
		sb.append("Yang, Ziheng (2007),  PAML 4: Phylogenetic Analysis by Maximum Likelihood.\n" +
				"Molecular Biology and Evolution, 24(8), pp 1586-91.");

		return sb.toString();
	}

	@Override
	public void setThreshold(double t)
	{
		((AlignmentResult)this.result).threshold = t;
		graph.setThreshold(t);
		Vector<Vector<String>> data = getTableVector(t);
		table.setData(data);
	}

	@Override
	public Printable[] getPrintables()
	{
		Printable[] p = new Printable[2];
		p[0] = table;
		p[1] = graph;
		return p;
	}

	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting() || table.accessTable().getSelectedRow() < 0)
			return;

		int location = sp.getDividerLocation();

		CodeMLResult result = (CodeMLResult) this.result;
		CMLModel m = result.models.get(table.accessTable().getSelectedRow());

		double[][] data;
		if (m.supportsPSS)
		{
			List<PSSite> pss = m.getPSS(-1f);
			data = new double[super.data.getSequenceSet().getLength()][2];
			for (int i = 0; i < pss.size(); i++)
			{
				PSSite pssite = pss.get(i);
				int n = pssite.getPos();
				double p = pssite.getP();

				// n[1,length] -> data[x][y], x[0, length-1]
				data[n - 2][0] = n - 1;
				data[n - 1][0] = n;
				data[n][0] = n + 1;

				data[n - 2][1] = p;
				data[n - 1][1] = p;
				data[n][1] = p;
			}
//			graph.setEnabled(true);
			sp.setBottomComponent(graph);
		} else
		{
			data = new double[this.data.getSequenceSet().getLength()][2];
			for (int i = 0; i < data.length; i++)
			{
				data[i][0] = i;
				data[i][1] = 0;
			}

//			graph.setEnabled(false);
			sp.setBottomComponent(blankPanel);
		}
		graph.setChartData(data);

		sp.setDividerLocation(location);
	}

}
