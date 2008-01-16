// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.print.Printable;

import javax.swing.*;

import org.apache.log4j.Logger;

import topali.data.*;
import topali.var.utils.Utils;
import doe.GradientPanel;

public class HMMResultPanel extends ResultPanel {

    Logger log = Logger.getLogger(this.getClass());

    GraphPanel graph1, graph2, graph3;

    public HMMResultPanel(AlignmentData data, HMMResult result) {
	super(data, result);
	try {
	    double[][] graph1data = (double[][]) Utils.castArray(result.data1,
		    double.class);
	    double[][] graph2data = (double[][]) Utils.castArray(result.data2,
		    double.class);
	    double[][] graph3data = (double[][]) Utils.castArray(result.data3,
		    double.class);

	    graph1 = new GraphPanel(data, result, graph1data, 1.01,
		    GraphPanel.RIGHT);
	    graph2 = new GraphPanel(data, result, graph1data, 1.01,
		    GraphPanel.RIGHT);
	    graph3 = new GraphPanel(data, result, graph1data, 1.01,
		    GraphPanel.RIGHT);

	    graph1
		    .setBorder(BorderFactory
			    .createTitledBorder("Probability of topology 1 (1,2), (3,4):"));
	    graph2
		    .setBorder(BorderFactory
			    .createTitledBorder("Probability of topology 2 (1,3), (2,4):"));
	    graph3
		    .setBorder(BorderFactory
			    .createTitledBorder("Probability of topology 3 (1,4), (2,3):"));

	    JPanel p = new JPanel(new GridBagLayout());
	    GridBagConstraints c1 = new GridBagConstraints();
	    c1.gridx = 0;
	    c1.gridy = 0;
	    c1.weightx = 1;
	    c1.weighty = 0.3;
	    c1.fill = GridBagConstraints.BOTH;
	    p.add(graph1, c1);
	    GridBagConstraints c2 = new GridBagConstraints();
	    c2.gridx = 0;
	    c2.gridy = 1;
	    c2.weightx = 1;
	    c2.weighty = 0.3;
	    c2.fill = GridBagConstraints.BOTH;
	    p.add(graph2, c2);
	    GridBagConstraints c3 = new GridBagConstraints();
	    c3.gridx = 0;
	    c3.gridy = 2;
	    c3.weightx = 1;
	    c3.weighty = 0.3;
	    c3.fill = GridBagConstraints.BOTH;
	    p.add(graph3, c3);

	    GradientPanel gp = new GradientPanel("Hidden Markov Model (HMM)");
	    gp.setStyle(GradientPanel.OFFICE2003);
	    JPanel p1 = new JPanel(new BorderLayout());
	    p1.add(gp, BorderLayout.NORTH);
	    p1.add(p);

	    addContent(p1, true);

	    setThreshold(result.threshold);

	} catch (Exception e) {
	    log.warn(e);
	}
    }

    @Override
    public String getAnalysisInfo() {
	HMMResult result = (HMMResult) this.result;

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

    @Override
    public Printable[] getPrintables() {
	return new Printable[] { graph1, graph2, graph3 };
    }

    @Override
    public void setThreshold(double t) {
	((AlignmentResult) result).threshold = t;
	graph1.setThreshold(t);
	graph2.setThreshold(t);
	graph3.setThreshold(t);
    }

}
