// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.*;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import topali.data.AlignmentData;
import topali.data.HMMResult;
import topali.gui.Icons;
import topali.gui.Utils;
import topali.gui.dialog.ThresholdDialog;
import doe.MsgBox;

public class HMMResultsPanel extends GraphResultsPanel
{
	private HMMResult result;

	private AlignmentGraph graph1, graph2, graph3;

	public HMMResultsPanel(AlignmentData data, HMMResult result)
	{
		super(data, result);
		this.result = result;

		float[] threshold =
		{ 0.95f };

		graph1 = new AlignmentGraph(data, result, result.data1, threshold);
		graph1.setHMMUpperLowerLimits();
		graph1.getGraphPanel().addMouseListener(new MyPopupMenuAdapter());
		graph1.setBorder(BorderFactory
				.createTitledBorder("Probability of topology 1 (1,2), (2,3):"));
		graph2 = new AlignmentGraph(data, result, result.data2, threshold);
		graph2.setHMMUpperLowerLimits();
		graph2.getGraphPanel().addMouseListener(new MyPopupMenuAdapter());
		graph2.setBorder(BorderFactory
				.createTitledBorder("Probability of topology 2 (1,3), (2,4):"));
		graph3 = new AlignmentGraph(data, result, result.data3, threshold);
		graph3.setHMMUpperLowerLimits();
		graph3.getGraphPanel().addMouseListener(new MyPopupMenuAdapter());
		graph3.setBorder(BorderFactory
				.createTitledBorder("Probability of topology 3 (1,4), (2,3):"));

		setThreshold(result.thresholdCutoff);

		JPanel p1 = new JPanel(new GridLayout(3, 1, 5, 5));
		p1.setBorder(BorderFactory.createLineBorder(Icons.grayBackground, 4));
		p1.add(graph1);
		p1.add(graph2);
		p1.add(graph3);

		setLayout(new BorderLayout());
		add(toolbar, BorderLayout.EAST);
		add(p1);
	}

	public void setThreshold(float thresholdCutoff)
	{
		result.thresholdCutoff = thresholdCutoff;

		graph1.setThresholdValue(thresholdCutoff);
		graph2.setThresholdValue(thresholdCutoff);
		graph3.setThresholdValue(thresholdCutoff);
	}

	protected void showThresholdDialog()
	{
		new ThresholdDialog(this, result.thresholdCutoff);
	}

	protected String getAnalysisText()
	{
		String str = new String(result.guiName);

		str += "\n\nRuntime: " + ((result.endTime - result.startTime) / 1000)
				+ " seconds";

		str += "\n\nhmm_model:            " + result.hmm_model;
		str += "\nhmm_initial:          " + result.hmm_initial;
		str += "\nhmm_freq_est_1:       " + result.hmm_freq_est_1;
		str += "\nhmm_freq_est_2:       " + result.hmm_freq_est_2;
		str += "\nhmm_freq_est_3:       " + result.hmm_freq_est_3;
		str += "\nhmm_freq_est_4:       " + result.hmm_freq_est_4;
		str += "\nhmm_transition:       " + result.hmm_transition;
		str += "\nhmm_transition_ratio: " + result.hmm_transition_ratio;
		str += "\nhmm_freq_1:           " + result.hmm_freq_1;
		str += "\nhmm_freq_2:           " + result.hmm_freq_2;
		str += "\nhmm_freq_3:           " + result.hmm_freq_3;
		str += "\nhmm_difficulty:       " + result.hmm_difficulty;

		str += "\n";

		str += "\nhmm_burn:             " + result.hmm_burn;
		str += "\nhmm_points:           " + result.hmm_points;
		str += "\nhmm_thinning:         " + result.hmm_thinning;
		str += "\nhmm_tuning:           " + result.hmm_tuning;
		str += "\nhmm_lambda:           " + result.hmm_lambda;
		str += "\nhmm_annealing:        " + result.hmm_annealing;
		str += "\nhmm_station:          " + result.hmm_station;
		str += "\nhmm_update:           " + result.hmm_update;
		str += "\nhmm_branch:           " + result.hmm_branch;

		str += "\n\nSelected sequences:";

		for (String seq : result.selectedSeqs)
			str += "\n  " + data.getSequenceSet().getNameForSafeName(seq);

		return str;
	}

	protected void saveCSV(File filename) throws Exception
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(filename));

		out.write("Nucleotide, Topology 1, Topology 2, Topology 3");
		out.newLine();

		for (int i = 0; i < result.data1.length; i++)
		{
			out.write(result.data1[i][0] + ", " + result.data1[i][1] + ", ");
			out.write(result.data2[i][0] + ", " + result.data3[i][1]);
			out.newLine();
		}

		out.close();
	}

	protected void savePNG(File f) throws Exception
	{
		String[] s = splitName(f.getName());
		File name1 = new File(f.getParent(), s[0] + " (prob1)" + s[1]);
		File name2 = new File(f.getParent(), s[0] + " (prob2)" + s[1]);
		File name3 = new File(f.getParent(), s[0] + " (prob3)" + s[1]);

		Utils.saveComponent(graph1.getGraphPanel(), name1, 600, 250);
		Utils.saveComponent(graph2.getGraphPanel(), name2, 600, 250);
		Utils.saveComponent(graph3.getGraphPanel(), name3, 600, 250);
		updateUI();

		MsgBox.msg("Graph data successfully saved to:\n" + name1 + "\n" + name1
				+ "\n" + name3, MsgBox.INF);
	}
}