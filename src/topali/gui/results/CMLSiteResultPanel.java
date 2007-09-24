// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.Color;
import java.awt.print.Printable;
import java.util.*;

import javax.swing.JSplitPane;
import javax.swing.event.*;

import topali.data.*;
import topali.gui.Prefs;

/**
 * Panel for displaying codeml site model result
 */
public class CMLSiteResultPanel extends ResultPanel implements
		ListSelectionListener
{

	TablePanel table;

	GraphPanel graph;

	public CMLSiteResultPanel(AlignmentData data, CodeMLResult result)
	{
		super(data, result);
		System.out.println(result);
		this.table = createTablePanel();
		this.graph = createGraphPanel();
		this.graph.setEnabled(false);
		
		JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		sp.setLeftComponent(table);
		sp.setRightComponent(graph);
		addContent(sp, true);

		setThreshold(result.threshold);
		
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
		names.add("Free parameters");
		names.add("Likelihood");
		names.add("P0");
		names.add("P1");
		names.add("P2");
		names.add("W0");
		names.add("W1");
		names.add("W2");
		names.add("P");
		names.add("Q");
		names.add("PSS");

		Vector data = getTableVector(((AlignmentResult)result).threshold);

		TablePanel p = new TablePanel(data, names, TablePanel.RIGHT);
		p.accessTable().getSelectionModel().addListSelectionListener(this);
		p.accessTable().getColumnModel().getColumn(0).setMinWidth(120);
		p.accessTable().getColumnModel().getColumn(11).setMinWidth(120);
		p.setBackground(Color.WHITE);
		return p;
	}

	private Vector<Vector> getTableVector(double thres)
	{
		CodeMLResult result = (CodeMLResult) this.result;
		Vector<Vector> data = new Vector<Vector>();
		for (CMLModel m : result.models)
		{
			Vector<String> v = new Vector<String>();
			v.add(m.name);
			v.add("" + m.nParameter);
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
		Vector data = getTableVector(t);
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
			graph.setEnabled(true);
		} else
		{
			data = new double[this.data.getSequenceSet().getLength()][2];
			for (int i = 0; i < data.length; i++)
			{
				data[i][0] = i;
				data[i][1] = 0;
			}
			
			graph.setEnabled(false);
		}
		graph.setChartData(data);
	}

}
