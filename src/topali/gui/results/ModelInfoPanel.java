/*
 * ModelInfoPanel.java
 *
 * Created on 16 October 2007, 08:55
 */

package topali.gui.results;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import org.apache.log4j.Logger;
import topali.analyses.TreeRootingThread;
import topali.data.AlignmentData;
import topali.data.models.*;
import topali.gui.*;
import topali.var.utils.Utils;

/**
 * 
 * @author dlindn
 */
public class ModelInfoPanel extends JPanel implements MouseListener {

    MTResultPanel mtpanel;
    AlignmentData data;
    Model model;
    Logger log = Logger.getLogger(this.getClass());

    Hashtable<String, JFrame> modelFrames = new Hashtable<String, JFrame>();
    Hashtable<String, JFrame> treeFrames = new Hashtable<String, JFrame>();

    /** Creates new form ModelInfoPanel */
    public ModelInfoPanel(AlignmentData data, MTResultPanel mtpanel) {
	this.data = data;
	this.mtpanel = mtpanel;
	initComponents();

	treeCanvas.setColouriser(data.getSequenceSet().getNameColouriser(
		Prefs.gui_color_seed));
	treeCanvas.setBackground(Color.GRAY);
	treeCanvas
		.setToolTipText("Estimated tree for this model (click to expand)");
	treeCanvas.addMouseListener(this);

	setModel(null);
	modelDiagram
		.setToolTipText("Substitution model diagram (click to expand)");
	modelDiagram.addMouseListener(this);
    }

    public void setModel(Model mod) {
	if (mod != null && mod == this.model)
	    return;

	this.model = mod;

	if (mod == null) {
	    setModName(null);
	    setAliases(null);
	    setGamma(-1, Double.NaN);
	    setInv(Double.NaN);
	    setSubRates(null);
	    setSubRateGroups(null);
	    setBaseFreqs(null);
	    setBaseFreqGroups(null);
	    setTree(null);
	} else {
	    if (mod instanceof DNAModel) {
		DNAModel m = (DNAModel) mod;
		setModName(m.getName());
		setAliases(m.getAliases());

		int df = m.getFreeParameters();
		if (m.isGamma())
		    df++;
		if (m.isInv())
		    df++;

		if (mod.isGamma())
		    setGamma(m.getGammaCat(), m.getAlpha());
		else
		    setGamma(-1, Double.NaN);
		if (mod.isInv())
		    setInv(m.getInvProp());
		else
		    setInv(Double.NaN);

		modelDiagram.setModel(m);
		rateHetDiagram.setModel(m);

		setSubRates(m.getSubRates());
		setSubRateGroups(m.getSubRateGroups());
		setBaseFreqs(m.getBaseFreqs());
		setBaseFreqGroups(m.getBaseFreqGroups());
	    } else if (mod instanceof ProteinModel) {
		ProteinModel m = (ProteinModel) mod;
		setModName(m.getName());
		setAliases(m.getAliases());

		if (mod.isGamma())
		    setGamma(m.getGammaCat(), m.getAlpha());
		else
		    setGamma(-1, Double.NaN);
		if (mod.isInv())
		    setInv(m.getInvProp());
		else
		    setInv(Double.NaN);

		modelDiagram.setModel(m);

		rateHetDiagram.setModel(m);

		setSubRates(null);
		setSubRateGroups(null);
		setBaseFreqs(null);
		setBaseFreqGroups(null);
	    }

	    int df = mod.getFreeParameters();
	    if (mod.isGamma())
		df++;
	    if (mod.isInv())
		df++;
	    df += (2 * mtpanel.getResult().selectedSeqs.length - 3);

	    scores.setText("\u2113 = " + Utils.d2.format(mod.getLnl())
		    + "; AIC\u2081 = " + Utils.d2.format(mod.getAic1())
		    + "; AIC\u2082 = " + Utils.d2.format(mod.getAic2())
		    + "; BIC = " + Utils.d2.format(mod.getBic()));
	    calcpara.setText("(df = " + df + "; n = "
		    + mtpanel.getResult().sampleSize + ")");

	    setTree(model.getTree());
	}
	repaint();
	defaultButton.setEnabled(mod != null
		&& !data.getSequenceSet().getParams().getModel().matches(mod));
    }

    private void setModName(String name) {
	if (name == null)
	    this.modelName.setText("--");
	else {
	    this.modelName.setText(name);
	}
    }

    private void setAliases(List<String> aliases) {
	if (aliases == null)
	    this.aliases.setText("");
	else {
	    String tmp = "(";
	    for (int i = 0; i < aliases.size() - 1; i++) {
		tmp += aliases.get(i) + "; ";
	    }
	    tmp += aliases.get(aliases.size() - 1);
	    tmp += ")";
	    this.aliases.setText(tmp);
	}
    }

    private void setGamma(int cat, double a) {
	if (cat < 0 || new Double(a).isNaN())
	    this.gamma.setText("\u03b1 = n/a");
	else
	    this.gamma.setText("\u03b1 = " + Utils.d3.format(a));
    }

    private void setInv(double inv) {
	if (new Double(inv).isNaN())
	    this.inv.setText("pINV = n/a");
	else
	    this.inv.setText("pINV = " + Utils.d3.format(inv));
    }

    private void setSubRates(double... rates) {
	if (rates == null) {
	    this.subRates.setText("[see matrix]");
	} else {
	    String tmp = Utils.d3.format(rates[0]) + " | "
		    + Utils.d3.format(rates[1]) + " | "
		    + Utils.d3.format(rates[2]) + " | "
		    + Utils.d3.format(rates[3]) + " | "
		    + Utils.d3.format(rates[4]) + " | "
		    + Utils.d3.format(rates[5]);
	    this.subRates.setText(tmp);
	}
    }

    private void setSubRateGroups(char... groups) {
	if (groups == null)
	    this.subRateGroups.setText("n/a");
	else {
	    String tmp = "(" + new String(groups) + ")";
	    this.subRateGroups.setText(tmp);
	}
    }

    private void setBaseFreqs(double... freqs) {
	if (freqs == null) {
	    this.baseFreq.setText("[see matrix]");
	} else {
	    String tmp = Utils.d3.format(freqs[0]) + " | "
		    + Utils.d3.format(freqs[1]) + " | "
		    + Utils.d3.format(freqs[2]) + " | "
		    + Utils.d3.format(freqs[3]);
	    this.baseFreq.setText(tmp);
	}
    }

    private void setBaseFreqGroups(char... groups) {
	if (groups == null)
	    this.baseFreqGroups.setText("n/a");
	else {
	    String tmp = "(" + new String(groups) + ")";
	    this.baseFreqGroups.setText(tmp);
	}
    }

    private void setTree(String tree) {
	if (tree != null) {
	    String mptree = (new TreeRootingThread(tree, false))
		    .getMPRootedTree();
	    if (mptree != null) {
		mptree = mptree.replaceAll("SEQ0+", "");
		treeCanvas.setTree(mptree);
	    }
	}
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code
    // ">//GEN-BEGIN:initComponents
    private void initComponents() {
	jPanel5 = new javax.swing.JPanel();
	modelName = new javax.swing.JLabel();
	aliases = new javax.swing.JLabel();
	jPanel2 = new javax.swing.JPanel();
	jLabel3 = new javax.swing.JLabel();
	jLabel5 = new javax.swing.JLabel();
	subRates = new javax.swing.JLabel();
	baseFreq = new javax.swing.JLabel();
	subRateGroups = new javax.swing.JLabel();
	baseFreqGroups = new javax.swing.JLabel();
	jLabel13 = new javax.swing.JLabel();
	jLabel14 = new javax.swing.JLabel();
	gamma = new javax.swing.JLabel();
	inv = new javax.swing.JLabel();
	jPanel4 = new javax.swing.JPanel();
	treeCanvas = new topali.gui.results.TreeCanvas();
	jPanel6 = new javax.swing.JPanel();
	modelDiagram = new topali.gui.results.ModelDiagram();
	if (data.getSequenceSet().getParams().isDNA()) {
	    this.modelDiagram = new DNAModelDiagram();
	} else {
	    this.modelDiagram = new ProteinModelDiagram();
	}

	jLabel1 = new javax.swing.JLabel();
	scores = new javax.swing.JLabel();
	calcpara = new javax.swing.JLabel();
	jPanel1 = new javax.swing.JPanel();
	jLabel11 = new javax.swing.JLabel();
	jLabel7 = new javax.swing.JLabel();
	jLabel9 = new javax.swing.JLabel();
	rateHetDiagram = new topali.gui.results.RateHetDiagram();
	defaultButton = new javax.swing.JButton();

	javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(
		jPanel5);
	jPanel5.setLayout(jPanel5Layout);
	jPanel5Layout.setHorizontalGroup(jPanel5Layout.createParallelGroup(
		javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 100,
		Short.MAX_VALUE));
	jPanel5Layout.setVerticalGroup(jPanel5Layout.createParallelGroup(
		javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 100,
		Short.MAX_VALUE));

	modelName.setFont(new java.awt.Font("Tahoma", 1, 12));
	modelName.setText("GTR");

	aliases.setText("(General Time Reversible)");

	jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
	jLabel3.setText("Substitution Rates:");

	jLabel5.setText("Base Frequencies:");

	subRates.setText("0.1 | 0.1 |  0.1  | 0.1 |  0.1 |  0.1");
	subRates.setToolTipText("A-C | A-G | A-T | C-G | C-T | G-T");

	baseFreq.setText("0.1 | 0.1 | 0.1 | 0.1");
	baseFreq.setToolTipText("A | C | G | T");

	subRateGroups.setText("(000000)");
	subRateGroups.setToolTipText("A-C | A-G | A-T | C-G | C-T | G-T");

	baseFreqGroups.setText("(0000)");
	baseFreqGroups.setToolTipText("A | C | G | T");

	jLabel13.setText("Rate Heterogeneity (\u0393):");

	jLabel14.setText("Proportion of Invariant Sites:");

	gamma.setText("--");

	inv.setText("--");

	jPanel4.setBorder(javax.swing.BorderFactory
		.createLineBorder(new java.awt.Color(0, 0, 0)));
	javax.swing.GroupLayout treeCanvasLayout = new javax.swing.GroupLayout(
		treeCanvas);
	treeCanvas.setLayout(treeCanvasLayout);
	treeCanvasLayout.setHorizontalGroup(treeCanvasLayout
		.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
		.addGap(0, 233, Short.MAX_VALUE));
	treeCanvasLayout.setVerticalGroup(treeCanvasLayout.createParallelGroup(
		javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 177,
		Short.MAX_VALUE));

	javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(
		jPanel4);
	jPanel4.setLayout(jPanel4Layout);
	jPanel4Layout.setHorizontalGroup(jPanel4Layout.createParallelGroup(
		javax.swing.GroupLayout.Alignment.LEADING).addComponent(
		treeCanvas, javax.swing.GroupLayout.DEFAULT_SIZE,
		javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
	jPanel4Layout.setVerticalGroup(jPanel4Layout.createParallelGroup(
		javax.swing.GroupLayout.Alignment.LEADING).addComponent(
		treeCanvas, javax.swing.GroupLayout.DEFAULT_SIZE,
		javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

	jPanel6.setBorder(javax.swing.BorderFactory
		.createLineBorder(new java.awt.Color(0, 0, 0)));
	javax.swing.GroupLayout modelDiagramLayout = new javax.swing.GroupLayout(
		modelDiagram);
	modelDiagram.setLayout(modelDiagramLayout);
	modelDiagramLayout.setHorizontalGroup(modelDiagramLayout
		.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
		.addGap(0, 128, Short.MAX_VALUE));
	modelDiagramLayout.setVerticalGroup(modelDiagramLayout
		.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
		.addGap(0, 155, Short.MAX_VALUE));

	javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(
		jPanel6);
	jPanel6.setLayout(jPanel6Layout);
	jPanel6Layout.setHorizontalGroup(jPanel6Layout.createParallelGroup(
		javax.swing.GroupLayout.Alignment.LEADING).addGroup(
		javax.swing.GroupLayout.Alignment.TRAILING,
		jPanel6Layout.createSequentialGroup().addContainerGap()
			.addComponent(modelDiagram,
				javax.swing.GroupLayout.DEFAULT_SIZE,
				javax.swing.GroupLayout.DEFAULT_SIZE,
				Short.MAX_VALUE).addContainerGap()));
	jPanel6Layout.setVerticalGroup(jPanel6Layout.createParallelGroup(
		javax.swing.GroupLayout.Alignment.LEADING).addGroup(
		jPanel6Layout.createSequentialGroup().addContainerGap()
			.addComponent(modelDiagram,
				javax.swing.GroupLayout.DEFAULT_SIZE,
				javax.swing.GroupLayout.DEFAULT_SIZE,
				Short.MAX_VALUE).addContainerGap()));

	jLabel1.setText("Scores:");

	scores.setText("\u2113=; AIC\u2081=; AIC\u2082=; BIC=");
	scores.setToolTipText("Model Test Scores");

	calcpara.setText("(df=; n=)");
	calcpara
		.setToolTipText("df: Degrees of Freedom (Model + Tree); n: Sample Size ");

	jPanel1.setBorder(javax.swing.BorderFactory
		.createLineBorder(new java.awt.Color(0, 0, 0)));
	jLabel11.setFont(new java.awt.Font("Tahoma", 1, 11));
	jLabel11.setForeground(new java.awt.Color(0, 0, 160));
	jLabel11.setText("\u0393");

	jLabel7.setText(",");

	jLabel9.setFont(new java.awt.Font("Tahoma", 1, 11));
	jLabel9.setForeground(new java.awt.Color(140, 0, 0));
	jLabel9.setText("pINV");

	javax.swing.GroupLayout rateHetDiagramLayout = new javax.swing.GroupLayout(
		rateHetDiagram);
	rateHetDiagram.setLayout(rateHetDiagramLayout);
	rateHetDiagramLayout.setHorizontalGroup(rateHetDiagramLayout
		.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
		.addGap(0, 165, Short.MAX_VALUE));
	rateHetDiagramLayout.setVerticalGroup(rateHetDiagramLayout
		.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
		.addGap(0, 135, Short.MAX_VALUE));

	javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(
		jPanel1);
	jPanel1.setLayout(jPanel1Layout);
	jPanel1Layout
		.setHorizontalGroup(jPanel1Layout
			.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(
				javax.swing.GroupLayout.Alignment.TRAILING,
				jPanel1Layout
					.createSequentialGroup()
					.addContainerGap(
						javax.swing.GroupLayout.DEFAULT_SIZE,
						Short.MAX_VALUE)
					.addGroup(
						jPanel1Layout
							.createParallelGroup(
								javax.swing.GroupLayout.Alignment.TRAILING)
							.addComponent(
								rateHetDiagram,
								javax.swing.GroupLayout.PREFERRED_SIZE,
								165,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addGroup(
								jPanel1Layout
									.createSequentialGroup()
									.addComponent(
										jLabel11)
									.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
									.addComponent(
										jLabel7)
									.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
									.addComponent(
										jLabel9)))
					.addContainerGap()));
	jPanel1Layout
		.setVerticalGroup(jPanel1Layout
			.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(
				jPanel1Layout
					.createSequentialGroup()
					.addContainerGap()
					.addGroup(
						jPanel1Layout
							.createParallelGroup(
								javax.swing.GroupLayout.Alignment.BASELINE)
							.addComponent(jLabel7)
							.addComponent(jLabel11)
							.addComponent(jLabel9))
					.addPreferredGap(
						javax.swing.LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(
						rateHetDiagram,
						javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE,
						Short.MAX_VALUE)
					.addContainerGap()));

	javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(
		jPanel2);
	jPanel2.setLayout(jPanel2Layout);
	jPanel2Layout
		.setHorizontalGroup(jPanel2Layout
			.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(
				jPanel2Layout
					.createSequentialGroup()
					.addContainerGap()
					.addGroup(
						jPanel2Layout
							.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
							.addComponent(jLabel3)
							.addGroup(
								jPanel2Layout
									.createSequentialGroup()
									.addGroup(
										jPanel2Layout
											.createParallelGroup(
												javax.swing.GroupLayout.Alignment.LEADING)
											.addComponent(
												jLabel13)
											.addComponent(
												jLabel14))
									.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
									.addGroup(
										jPanel2Layout
											.createParallelGroup(
												javax.swing.GroupLayout.Alignment.TRAILING,
												false)
											.addGroup(
												javax.swing.GroupLayout.Alignment.LEADING,
												jPanel2Layout
													.createParallelGroup(
														javax.swing.GroupLayout.Alignment.LEADING)
													.addComponent(
														inv)
													.addGroup(
														javax.swing.GroupLayout.Alignment.TRAILING,
														jPanel2Layout
															.createSequentialGroup()
															.addPreferredGap(
																javax.swing.LayoutStyle.ComponentPlacement.RELATED,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																Short.MAX_VALUE)
															.addGroup(
																jPanel2Layout
																	.createParallelGroup(
																		javax.swing.GroupLayout.Alignment.LEADING)
																	.addComponent(
																		baseFreq)
																	.addComponent(
																		subRates)
																	.addComponent(
																		subRateGroups)
																	.addComponent(
																		baseFreqGroups))))
											.addComponent(
												gamma,
												javax.swing.GroupLayout.Alignment.LEADING)))
							.addComponent(jLabel5)
							.addComponent(jLabel1)
							.addComponent(scores)
							.addComponent(calcpara))
					.addPreferredGap(
						javax.swing.LayoutStyle.ComponentPlacement.RELATED,
						javax.swing.GroupLayout.DEFAULT_SIZE,
						Short.MAX_VALUE)
					.addComponent(
						jPanel4,
						javax.swing.GroupLayout.PREFERRED_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(
						javax.swing.LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(
						jPanel6,
						javax.swing.GroupLayout.PREFERRED_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(
						javax.swing.LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(
						jPanel1,
						javax.swing.GroupLayout.PREFERRED_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addContainerGap()));
	jPanel2Layout
		.setVerticalGroup(jPanel2Layout
			.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(
				jPanel2Layout
					.createSequentialGroup()
					.addContainerGap()
					.addGroup(
						jPanel2Layout
							.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
							.addComponent(
								jPanel4,
								0,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								Short.MAX_VALUE)
							.addComponent(
								jPanel6,
								0,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								Short.MAX_VALUE)
							.addComponent(
								jPanel1,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								Short.MAX_VALUE)
							.addGroup(
								jPanel2Layout
									.createSequentialGroup()
									.addGap(
										11,
										11,
										11)
									.addGroup(
										jPanel2Layout
											.createParallelGroup(
												javax.swing.GroupLayout.Alignment.BASELINE)
											.addComponent(
												gamma)
											.addComponent(
												jLabel13,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												14,
												javax.swing.GroupLayout.PREFERRED_SIZE))
									.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
									.addGroup(
										jPanel2Layout
											.createParallelGroup(
												javax.swing.GroupLayout.Alignment.BASELINE)
											.addComponent(
												jLabel14)
											.addComponent(
												inv))
									.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
									.addGroup(
										jPanel2Layout
											.createParallelGroup(
												javax.swing.GroupLayout.Alignment.BASELINE)
											.addComponent(
												jLabel3)
											.addComponent(
												subRates))
									.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
									.addComponent(
										subRateGroups)
									.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
									.addGroup(
										jPanel2Layout
											.createParallelGroup(
												javax.swing.GroupLayout.Alignment.BASELINE)
											.addComponent(
												jLabel5)
											.addComponent(
												baseFreq))
									.addGroup(
										jPanel2Layout
											.createParallelGroup(
												javax.swing.GroupLayout.Alignment.LEADING)
											.addGroup(
												jPanel2Layout
													.createSequentialGroup()
													.addPreferredGap(
														javax.swing.LayoutStyle.ComponentPlacement.RELATED)
													.addComponent(
														baseFreqGroups))
											.addGroup(
												jPanel2Layout
													.createSequentialGroup()
													.addGap(
														20,
														20,
														20)
													.addComponent(
														jLabel1)))
									.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
									.addComponent(
										scores,
										javax.swing.GroupLayout.PREFERRED_SIZE,
										14,
										javax.swing.GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
									.addComponent(
										calcpara)))
					.addContainerGap()));

	defaultButton.setText("Select");
	defaultButton.setToolTipText("Use this model for further analysis");
	defaultButton.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		defaultButtonActionPerformed(evt);
	    }
	});

	javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
	this.setLayout(layout);
	layout
		.setHorizontalGroup(layout
			.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(
				layout
					.createSequentialGroup()
					.addContainerGap()
					.addGroup(
						layout
							.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
							.addComponent(
								jPanel2,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								Short.MAX_VALUE)
							.addGroup(
								javax.swing.GroupLayout.Alignment.TRAILING,
								layout
									.createSequentialGroup()
									.addComponent(
										modelName)
									.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED,
										835,
										Short.MAX_VALUE)
									.addComponent(
										defaultButton))
							.addComponent(aliases))
					.addContainerGap()));
	layout.setVerticalGroup(layout.createParallelGroup(
		javax.swing.GroupLayout.Alignment.LEADING).addGroup(
		layout.createSequentialGroup().addGroup(
			layout.createParallelGroup(
				javax.swing.GroupLayout.Alignment.BASELINE)
				.addComponent(defaultButton).addComponent(
					modelName)).addPreferredGap(
			javax.swing.LayoutStyle.ComponentPlacement.RELATED)
			.addComponent(aliases).addGap(13, 13, 13).addComponent(
				jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE,
				javax.swing.GroupLayout.DEFAULT_SIZE,
				Short.MAX_VALUE).addContainerGap()));
    }// </editor-fold>//GEN-END:initComponents

    private void defaultButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_defaultButtonActionPerformed
	log.info("Set default model to:\n" + model);
	data.getSequenceSet().getParams().setModel(model);
	defaultButton.setEnabled(false);
	mtpanel.modelSetTo(model);
    }// GEN-LAST:event_defaultButtonActionPerformed

    @Override
    public void mouseClicked(MouseEvent e) {
	if (e.getSource() == modelDiagram) {
	    String name = "Modeldiagram: " + model.getName();
	    if (model.isInv())
		name += "+I";
	    if (model.isGamma())
		name += "+G";

	    JFrame frame = modelFrames.get(name);
	    if (frame == null || !frame.isDisplayable()) {
		frame = new JFrame(name);
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(Color.WHITE);
		p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		ModelDiagram dia = data.getSequenceSet().getParams().isDNA() ? new DNAModelDiagram(
			(DNAModel) model)
			: new ProteinModelDiagram((ProteinModel) model);
		p.add(dia, BorderLayout.CENTER);
		dia.setBackground(Color.WHITE);
		frame.getContentPane().add(p);
		frame.pack();
		frame.setSize(400, 480);
		frame.setLocationRelativeTo(TOPALi.winMain);
		modelFrames.put(name, frame);
		frame.setVisible(true);
	    } else {
		frame.setVisible(true);
		frame.requestFocus();
	    }
	}

	else if (e.getSource() == treeCanvas) {
	    String name = "Tree estimation: " + model.getName();
	    if (model.isInv())
		name += "+I";
	    if (model.isGamma())
		name += "+G";

	    JFrame frame = treeFrames.get(name);
	    if (frame == null || !frame.isDisplayable()) {
		frame = new JFrame(name);
		frame.setSize(400, 480);
		frame.getContentPane().add(new TreeCanvas(treeCanvas));
		frame.setLocationRelativeTo(TOPALi.winMain);
		treeFrames.put(name, frame);
		frame.setVisible(true);
	    } else {
		frame.setVisible(true);
		frame.requestFocus();
	    }
	}
    }

    @Override
    public void mouseEntered(MouseEvent e) {
	setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void mouseExited(MouseEvent e) {
	setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel aliases;
    private javax.swing.JLabel baseFreq;
    private javax.swing.JLabel baseFreqGroups;
    private javax.swing.JLabel calcpara;
    private javax.swing.JButton defaultButton;
    private javax.swing.JLabel gamma;
    private javax.swing.JLabel inv;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private topali.gui.results.ModelDiagram modelDiagram;
    private javax.swing.JLabel modelName;
    private topali.gui.results.RateHetDiagram rateHetDiagram;
    private javax.swing.JLabel scores;
    private javax.swing.JLabel subRateGroups;
    private javax.swing.JLabel subRates;
    private topali.gui.results.TreeCanvas treeCanvas;
    // End of variables declaration//GEN-END:variables

}
