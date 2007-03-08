// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.util.List;

import javax.swing.JScrollPane;

import topali.data.*;

public class CodeMLSiteResultPanel extends GraphResultsPanel
{

	CodeMLSiteResultTable table;
	AlignmentGraph graph;
	
	public CodeMLSiteResultPanel(AlignmentData data, CodeMLResult result)
	{
		super(data, result);
		
		table = new CodeMLSiteResultTable(result, this);
		graph = new AlignmentGraph(data, result, new float[][]{}, 0.95f, AlignmentGraph.TYPE_HISTOGRAMM);
		graph.getGraphPanel().addMouseListener(new MyPopupMenuAdapter());
		
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.3;
		c.fill = GridBagConstraints.BOTH;
		this.add(new JScrollPane(table), c);
		c.weighty = 0.7;
		c.gridy = 1;
		this.add(new JScrollPane(graph), c);
		c.gridx = 1;
		c.gridheight = 2;
		c.weightx=0.01;
		c.fill = GridBagConstraints.NONE;
		this.add(toolbar, c);
	}
	
	public void setSelectedModel(int index) {
		CMLModel m = ((CodeMLResult)super.aResult).models.get(index);
		
		if(m.supportsPSS) {
			List<PSSite> pss = m.getPSS(-1f);
			float[][] data = new float[pss.size()][2];
			for (int i = 0; i < pss.size(); i++)
			{
				PSSite p = pss.get(i);
				data[i][0] = p.getPos() * 3 - 1;
				data[i][1] = (float)p.getP();
			}
			graph.setChartData(data);
		}
	}
	
	@Override
	protected String getAnalysisText()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void saveCSV(File filename) throws Exception
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void savePNG(File filename) throws Exception
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setThreshold(float thresholdCutoff)
	{
		table.setThreshold(thresholdCutoff);
		graph.setThresholdValue(thresholdCutoff);
	}

	@Override
	protected void showThresholdDialog()
	{
		// TODO Auto-generated method stub
		
	}

}
