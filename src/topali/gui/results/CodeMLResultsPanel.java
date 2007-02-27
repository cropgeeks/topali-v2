// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import topali.data.*;
import topali.gui.Icons;

public class CodeMLResultsPanel extends JPanel implements ChangeListener
{
	CodeMLResultTable table;
	//CodeMLGraphPanel graph = null;
	AlignmentGraph graph;
	JLabel labelP;
	JSlider sliderP;
	
	AlignmentData data;
	AlignmentResult result;
	
	public CodeMLResultsPanel(AlignmentData data, CodeMLResult result)
	{	
		this.data = data;
		this.result = result;
		this.setBorder(BorderFactory.createLineBorder(Icons.grayBackground, 4));
		
		//table = new CodeMLResultTable(result, this);
		
		JPanel sliderPanel = new JPanel();
		sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.X_AXIS));
		sliderPanel.add(Box.createHorizontalGlue());
		labelP = new JLabel("p(w>1) = ");
		sliderPanel.add(labelP);
		sliderP = new JSlider(JSlider.HORIZONTAL, 0, 100, 95);
		sliderP.setMajorTickSpacing(5);
		sliderP.setMinorTickSpacing(1);
		sliderP.setPaintTicks(true);
		sliderP.setPaintLabels(true);
		Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		labelTable.put(0, new JLabel("0.00"));
		labelTable.put(50, new JLabel("0.50"));
		labelTable.put(75, new JLabel("0.75"));
		labelTable.put(90, new JLabel("0.90"));
		labelTable.put(100, new JLabel("1.00"));
		sliderP.setLabelTable(labelTable);
		sliderP.addChangeListener(this);
		sliderPanel.add(sliderP);
		sliderPanel.add(Box.createHorizontalGlue());
		
		//graph = new CodeMLGraphPanel(data, result);
		graph = new AlignmentGraph(data, result, new float[][]{}, 0.95f, AlignmentGraph.TYPE_HISTOGRAMM);
		
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.25;
		c.fill = GridBagConstraints.BOTH;
		this.add(new JScrollPane(table), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weighty = 0.05;
		c.gridy = 1;
		this.add(sliderPanel, c);
		c.weighty = 0.7;
		c.gridy = 2;
		c.fill = GridBagConstraints.BOTH;
		this.add(new JScrollPane(graph), c);
	}
	
	public void setSelectedModel(CMLModel m) {
		float minP = (float)sliderP.getValue()/100f;
		table.setThreshold(minP);
		
		List<PSSite> pss = m.getPSS(-1f);
		float[][] data = new float[pss.size()][2];
		for (int i = 0; i < pss.size(); i++)
		{
			PSSite p = pss.get(i);
			//ser.add(p.getPos() * 3 - 1, p.getP());
			data[i][0] = p.getPos() * 3 - 1;
			data[i][1] = (float)p.getP();
		}
		graph.setChartData(data);
		graph.setThresholdValue(minP);
		//graph.setModel(m);
		//graph.setThreshold(minP);
		this.revalidate();
	}

	public void stateChanged(ChangeEvent e)
	{
		if(e.getSource().equals(sliderP)) {
			float minP = (float)sliderP.getValue()/100f;
			graph.setThresholdValue(minP);
			table.setThreshold(minP);
		}
		
	}
	
	
}