/*
 * MTDialogPanel.java
 *
 * Created on 15 October 2007, 14:51
 */

package topali.gui.dialog.jobs.mt;

import java.util.*;

import javax.swing.DefaultComboBoxModel;

import topali.data.*;
import topali.data.models.*;
import topali.i18n.Text;

/**
 *
 * @author  dlindn
 */
public class MTDialogPanel extends javax.swing.JPanel {
	boolean dna;

    /** Creates new form MTDialogPanel */
    public MTDialogPanel(AlignmentData data, ModelTestResult res, boolean dna) {
//    	this.res = (res==null) ? new ModelTestResult() : res;

    	this.dna = dna;
        initComponents();

//        DefaultComboBoxModel mod = new DefaultComboBoxModel(new String[] {ModelTestResult.TYPE_PHYML, ModelTestResult.TYPE_MRBAYES});

		DefaultComboBoxModel mod;
		if (dna)
			mod = new DefaultComboBoxModel(new String[] {ModelTestResult.TYPE_PHYML, ModelTestResult.TYPE_MRBAYES});
		else
			mod = new DefaultComboBoxModel(new String[] { "PhyML or RaxML", "MrBayes" });
        models.setModel(mod);

        DefaultComboBoxModel mod2 = new DefaultComboBoxModel(new String[] {ModelTestResult.SAMPLE_SEQLENGTH, ModelTestResult.SAMPLE_ALGNSIZE});
        sampleSize.setModel(mod2);

		// DOMINIK - ARGHHHHHHH! NEVER set combo boxes by strings and rely on everything else being set
		// based on the value of those strings!!! Now when I want to CHANGE those strings, it's going to
		// screw up loads of other code. ARRRGH!
		if (Prefs.ms_models == ModelTestResult.TYPE_MRBAYES)
			models.setSelectedIndex(1);
//        models.setSelectedItem(Prefs.ms_models);
    	gamma.setSelected(Prefs.ms_gamma);
    	inv.setSelected(Prefs.ms_inv);
    	sampleSize.setSelectedItem(Prefs.ms_samplesize);

    	 if (data.getSequenceSet().getLength() % 3 != 0 || data.getSequenceSet().getProps().isNucleotides() == false)
    		checkProteinCoding.setEnabled(false);

 //   	if(res!=null)
 //   		initPrevResult(res);
    }

    private void initPrevResult(ModelTestResult res)
    {
    	if (res.type == ModelTestResult.TYPE_MRBAYES)
			this.models.setSelectedIndex(1);
   // 	this.models.setSelectedItem(res.type);

    	boolean gamma = false;
    	boolean inv = false;
    	for(Model mod : res.models) {
    	    if(mod.isGamma())
    		gamma = true;
    	    if(mod.isInv())
    		inv = true;
    	}
    	this.gamma.setSelected(gamma);
    	this.inv.setSelected(inv);
    	this.sampleSize.setSelectedItem(res.sampleCrit);
    }

    public ModelTestResult getResult()
    {
		ModelTestResult res = new ModelTestResult();

    	List<Model> availModels = null;
		if(this.models.getSelectedIndex() == 0) {
			availModels = ModelManager.getInstance().listPhymlModels(dna);
		}
		else if(this.models.getSelectedIndex() == 1) {
			availModels = ModelManager.getInstance().listMrBayesModels(dna);
		}
		ArrayList<Model> models = new ArrayList<Model>();
		for(Model m : availModels) {
				Model m1 = ModelManager.getInstance().generateModel(m.getName(), false, false);
				models.add(m1);
				if(gamma.isSelected()) {
					Model m2 = ModelManager.getInstance().generateModel(m.getName(), true, false);
					models.add(m2);
				}
				if(inv.isSelected()) {
					Model m3 = ModelManager.getInstance().generateModel(m.getName(), false, true);
					models.add(m3);
				}
				if(gamma.isSelected()&&inv.isSelected()) {
					Model m4 = ModelManager.getInstance().generateModel(m.getName(), true, true);
					models.add(m4);
				}
		}
		res.models = models;
		res.type = (String)this.models.getSelectedItem();
		res.sampleCrit = (String)this.sampleSize.getSelectedItem();

		Prefs.ms_gamma = gamma.isSelected();
		Prefs.ms_inv = inv.isSelected();
		Prefs.ms_models = (String)this.models.getSelectedItem();
		Prefs.ms_samplesize = (String)sampleSize.getSelectedItem();
    	return res;
    }

    public void setDefaults() {
    	models.setSelectedIndex(0);
    	gamma.setSelected(true);
    	inv.setSelected(true);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        models = new javax.swing.JComboBox();
        inv = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        gamma = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        checkProteinCoding = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        sampleSize = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Generate models:"));

        jLabel2.setText(Text.get("Gamma"));

        models.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "PhyML", "MrBayes" }));

        inv.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        inv.setMargin(new java.awt.Insets(0, 0, 0, 0));

        jLabel3.setText(Text.get("Invariant_Sites"));

        gamma.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gamma.setMargin(new java.awt.Insets(0, 0, 0, 0));

        jLabel1.setText("Running Model Selection allows you to ensure that the best model parameters");

        jLabel4.setText("for generating phylogenetic trees will be fed into the tree generation methods.");

        jLabel6.setText("Tree generation via:  ");

        checkProteinCoding.setText("Run model selection assuming protein-coding DNA (3 analyses)");

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel1)
                    .add(jLabel4)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel6)
                            .add(jPanel1Layout.createSequentialGroup()
                                .add(10, 10, 10)
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jLabel2)
                                    .add(jLabel3))))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel1Layout.createSequentialGroup()
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(inv))
                            .add(gamma)
                            .add(models, 0, 270, Short.MAX_VALUE)))
                    .add(checkProteinCoding))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel4)
                .add(18, 18, 18)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel6)
                    .add(models, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jLabel2)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel3))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(gamma)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(inv)))
                .add(18, 18, 18)
                .add(checkProteinCoding))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("AIC2/BIC calculation:"));

        sampleSize.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Sequence Length", "Alignment Size" }));

        jLabel5.setText(Text.get("Sample_Size"));

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel5)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(sampleSize, 0, 302, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel5)
                    .add(sampleSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JCheckBox checkProteinCoding;
    private javax.swing.JCheckBox gamma;
    private javax.swing.JCheckBox inv;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JComboBox models;
    private javax.swing.JComboBox sampleSize;
    // End of variables declaration//GEN-END:variables

}
