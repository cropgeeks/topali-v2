// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.Color;
import java.awt.print.Printable;
import java.util.List;
import java.util.Vector;

import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import topali.data.*;

public class CMLSiteResultPanel extends ResultPanel implements ListSelectionListener
{

	TablePanel table;
	GraphPanel graph;
	
	public CMLSiteResultPanel(AlignmentData data, CodeMLResult result)
	{
		super(data, result);
		this.table = createTablePanel();
		this.graph = createGraphPanel();
		
		JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		sp.setLeftComponent(table);
		sp.setRightComponent(graph);
		addContent(sp, true);
	}

	private GraphPanel createGraphPanel() {
		GraphPanel p = new GraphPanel(data, result, new double[][]{}, 1.05, GraphPanel.RIGHT);
		return p;
	}

	private TablePanel createTablePanel() {
		Vector<String> names = new Vector<String>();
		names.add("Model");
		names.add("Free parameters");
		names.add("Likelihood");
		names.add("dN/dS");
		names.add("W");
		names.add("P0");
		names.add("P1");
		names.add("P2");
		names.add("W0");
		names.add("W1");
		names.add("W2");
		names.add("P");
		names.add("Q");
		names.add("W");
		names.add("PSS");
		
		Vector data = getTableVector(result.threshold);
		
		TablePanel p = new TablePanel(data, names, null, TablePanel.RIGHT);
		p.accessTable().getSelectionModel().addListSelectionListener(this);
		p.accessTable().getColumnModel().getColumn(0).setMinWidth(120);
		p.accessTable().getColumnModel().getColumn(14).setMinWidth(120);
		p.setBackground(Color.WHITE);
		return p;
	}
	
	private Vector<Vector> getTableVector(double thres) {
		CodeMLResult result = (CodeMLResult) this.result;
		Vector<Vector> data = new Vector<Vector>();
		for(CMLModel m : result.models) {
			Vector<String> v = new Vector<String>();
			v.add(m.name);
			v.add(""+m.nParameter);
			v.add(ResultPanel.likelihoodFormat.format(m.likelihood));
			if(m.dnDS!=-1)
				v.add(ResultPanel.omegaFormat.format(m.dnDS));
			else
				v.add("");
			if(m.w!=-1)
				v.add(ResultPanel.omegaFormat.format(m.w));
			else
				v.add("");
			if(m.p0!=-1)
				v.add(ResultPanel.omegaFormat.format(m.p0));
			else
				v.add("");
			if(m.p1!=-1)
				v.add(ResultPanel.omegaFormat.format(m.p1));
			else
				v.add("");
			if(m.p2!=-1)
				v.add(ResultPanel.omegaFormat.format(m.p2));
			else
				v.add("");
			if(m.w0!=-1)
				v.add(ResultPanel.omegaFormat.format(m.w0));
			else
				v.add("");
			if(m.w1!=-1)
				v.add(ResultPanel.omegaFormat.format(m.w1));
			else
				v.add("");
			if(m.w2!=-1)
				v.add(ResultPanel.omegaFormat.format(m.w2));
			else
				v.add("");
			if(m.p!=-1)
				v.add(ResultPanel.omegaFormat.format(m.p));
			else
				v.add("");
			if(m.q!=-1)
				v.add(ResultPanel.omegaFormat.format(m.q));
			else
				v.add("");
			if(m._w!=-1)
				v.add(ResultPanel.omegaFormat.format(m._w));
			else
				v.add("");
			
			if(m.supportsPSS) {
				List<PSSite> pss = m.getPSS(thres);
				StringBuffer sb = new StringBuffer();
				for(PSSite s : pss)
					sb.append(""+s.getPos()+s.getAa()+" ");
				v.add(sb.toString());
			}
			else
				v.add("--");
			
			data.add(v);
		}
		return data;
	}
	
	@Override
	public String getAnalysisInfo()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("Analysis type: Site model\n\n");

		CodeMLResult result = (CodeMLResult) this.result;
		for(CMLModel m : result.models) {
			sb.append(m.toString()+"\n");
		}
		
		sb.append("\nSelected sequences:\n");
		for (String seq : result.selectedSeqs)
			sb.append("\n  " + data.getSequenceSet().getNameForSafeName(seq));
		
		return sb.toString();
	}

	@Override
	public void setThreshold(double t)
	{
		this.result.threshold = t;
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
		if(e.getValueIsAdjusting() || table.accessTable().getSelectedRow()<0)
			return;
		
		CodeMLResult result = (CodeMLResult)this.result;
		CMLModel m = result.models.get(table.accessTable().getSelectedRow());

		double[][] data;
		if (m.supportsPSS)
		{
			List<PSSite> pss = m.getPSS(-1f);
			data = new double[pss.size()*3][2];
			for (int i = 0; i < pss.size(); i++)
			{
				PSSite pssite = pss.get(i);
				int n = pssite.getPos()*3-1;
				double p = pssite.getP();
				data[n-2][0] = n-1;
				data[n-2][1] = p;
				data[n-1][0] = n;
				data[n-1][1] = p;
				data[n][0] = n+1;
				data[n-2][1] = p;
			}
		}
		else {
			data = new double[this.data.getSequenceSet().getLength()][2];
			for(int i=0; i<data.length; i++) {
				data[i][0] = i;
				data[i][1] = 0;
			}
		}
		graph.setChartData(data);
	}
	
	
}
