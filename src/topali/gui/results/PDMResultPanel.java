// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.Printable;

import javax.swing.*;

import org.apache.log4j.Logger;

import topali.analyses.AnalysisUtils;
import topali.data.*;
import topali.gui.GradientPanel;
import topali.var.utils.Utils;

public class PDMResultPanel extends ResultPanel implements MouseMotionListener,
	MouseListener {

    Logger log = Logger.getLogger(this.getClass());

    GraphPanel graph1, graph2;
    HistogramPanel histoPanel;

    public PDMResultPanel(AlignmentData data, PDMResult result) {
	super(data, result);

	try {
	    double[][] graph1Data = (double[][]) Utils.castArray(
		    result.glbData, double.class);
	    double[][] graph2Data = (double[][]) Utils.castArray(
		    result.locData, double.class);

	    graph1 = new GraphPanel(data, result, graph1Data, -1, "global");
	    graph2 = new GraphPanel(data, result, graph2Data, -1, "local");

	    graph2.setThreshold(result.threshold);

	    graph1.setBorder(BorderFactory
		    .createTitledBorder("Global divergence measure"));
	    graph2.setBorder(BorderFactory
		    .createTitledBorder("Local divergence measure"));

	    graph1.addMouseListener(this);
	    graph1.addMouseMotionListener(this);
	    graph2.addMouseListener(this);
	    graph2.addMouseMotionListener(this);

	    histoPanel = new HistogramPanel();
	    JPanel p1a = new JPanel(new BorderLayout());
	    p1a.add(histoPanel);
	    p1a.setBorder(BorderFactory.createLoweredBevelBorder());

	    JPanel p2a = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
	    p2a.add(new JLabel(
		    "Histogram of probability distribution (under mouse): "));
	    p2a.add(p1a);

	    JPanel p1 = new JPanel(new GridLayout(2, 1, 5, 5));
	    p1.add(graph1);
	    p1.add(graph2);

	    JPanel p2 = new JPanel(new BorderLayout());
	    p2.add(p1);
	    p2.add(p2a, BorderLayout.SOUTH);

	    GradientPanel gp = new GradientPanel(
		    "Probabilistic Divergence Measure (PDM)");
	    gp.setStyle(GradientPanel.OFFICE2003);
	    JPanel p3 = new JPanel(new BorderLayout());
	    p3.add(gp, BorderLayout.NORTH);
	    p3.add(p2, BorderLayout.CENTER);

	    addContent(p3, true);

	    setThreshold(result.threshold);

	} catch (Exception e) {
	    log.warn(e);
	}
    }

    @Override
    public String getAnalysisInfo() {
	PDMResult result = (PDMResult) this.result;
	String str = new String(result.guiName);

	str += "\n\nRuntime: " + ((result.endTime - result.startTime) / 1000)
		+ " seconds";

	str += "\n\npdm_window:          " + result.pdm_window;
	str += "\npdm_step:            " + result.pdm_step;
	str += "\npdm_runs:            " + (result.pdm_runs - 1);
	str += "\npdm_prune:           " + result.pdm_prune;
	str += "\npdm_cutoff:          " + result.pdm_cutoff;
	str += "\npdm_seed:            " + result.pdm_seed;
	str += "\npdm_burn:            " + result.pdm_burn;
	str += "\npdm_cycles:          " + result.pdm_cycles;
	str += "\npdm_burn_algorithm:  " + result.pdm_burn_algorithm;
	str += "\npdm_main_algorithm:  " + result.pdm_main_algorithm;
	str += "\npdm_use_beta:        " + result.pdm_use_beta;
	str += "\npdm_parameter_update_interval: "
		+ result.pdm_parameter_update_interval;
	str += "\npdm_update_theta:    " + result.pdm_update_theta;
	str += "\npdm_tune_interval:   " + result.pdm_tune_interval;
	str += "\npdm_molecular_clock: " + result.pdm_molecular_clock;
	str += "\npdm_category_list:   " + result.pdm_category_list;
	str += "\npdm_initial_theta:   " + result.pdm_initial_theta;
	str += "\npdm_outgroup:        " + result.pdm_outgroup;
	str += "\npdm_global_tune:     " + result.pdm_global_tune;
	str += "\npdm_local_tune:      " + result.pdm_local_tune;
	str += "\npdm_theta_tune:      " + result.pdm_theta_tune;
	str += "\npdm_beta_tune:       " + result.pdm_beta_tune;

	str += "\n\nSelected sequences:";

	for (String seq : result.selectedSeqs)
	    str += "\n  " + data.getSequenceSet().getNameForSafeName(seq);

	return str;
    }

    @Override
    public Printable[] getPrintables() {
	return new Printable[] { graph1.getPrintable(), graph2.getPrintable() };
    }

    @Override
    public void setThreshold(double t) {
	((AlignmentResult) result).threshold = t;
	PDMResult res = (PDMResult) result;

	float thres = AnalysisUtils.getArrayValue(res.thresholds, (float) t);

	graph2.setThreshold(thres);
    }

    public void mouseMoved(MouseEvent e) {
	PDMResult result = (PDMResult) this.result;
	if (result.histograms == null)
	    return;

	int nuc = graph2.getNucleotideFromPoint(e.getX());

	int pos = -1;

	if (nuc >= result.locData[0][0] - result.pdm_step)
	    // Position: +stepSize moves right one window to avoid -0.x and
	    // +0.x both giving the same window
	    pos = (int) ((nuc - result.locData[0][0] + result.pdm_step) / result.pdm_step);

	if (pos < 0 || pos >= result.histograms.length)
	    histoPanel.setData(null);
	else
	    histoPanel.setData(result.histograms[pos]);
    }

    public void mouseExited(MouseEvent e) {
	histoPanel.setData(null);
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

}
