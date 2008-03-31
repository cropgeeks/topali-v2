/*
 * AdvancedMrBayes.java
 *
 * Created on 07 September 2007, 08:35
 */

package topali.gui.dialog.jobs.tree.mrbayes;

import java.util.List;

import javax.swing.*;

import topali.data.*;
import topali.data.models.*;
import topali.i18n.Text;
import topali.var.utils.Utils;

/**
 *
 * @author  dlindn
 */
public class AdvancedMrBayes extends javax.swing.JPanel {
    
	AlignmentData data;
	SequenceSet ss;
	MBTreeResult result;
	
    /** Creates new form AdvancedMrBayes */
    public AdvancedMrBayes(AlignmentData data) {
    	this.data = data;
    	this.ss = data.getSequenceSet();
	this.result = new MBTreeResult();
		
        initComponents();
        initValues();
        
    }
    
    public void initValues() {
		SequenceSetProperties params = ss.getProps();
		
		List<Model> mlist = ModelManager.getInstance().listMrBayesModels(ss.getProps().isNucleotides());
		String[] models = new String[mlist.size()];
		for(int i=0; i<mlist.size(); i++)
			models[i] = mlist.get(i).getName();
		
		ComboBoxModel cm = new DefaultComboBoxModel(models);
		this.subModel.setModel(cm);

		Model m = params.getModel();
		if(Utils.indexof(models, m.getName())==-1) {
			if(ss.getProps().isNucleotides())
				m = ModelManager.getInstance().generateModel(Prefs.mb_default_dnamodel, m.isGamma(), m.isInv());
			else
				m = ModelManager.getInstance().generateModel(Prefs.mb_default_proteinmodel, m.isGamma(), m.isInv());
		}
		
		this.subModel.setSelectedItem(m.getName());
		
		this.gamma.setSelected(params.getModel().isGamma());
		this.inv.setSelected(params.getModel().isInv());
		
		SpinnerNumberModel mNRuns = new SpinnerNumberModel(Prefs.mb_runs, 1, 5, 1);
		this.nRuns.setModel(mNRuns);
		
		SpinnerNumberModel mNGen = new SpinnerNumberModel(Prefs.mb_gens, 10000, 500000, 10000);
		this.nGen.setModel(mNGen);
		
		SpinnerNumberModel mFreq = new SpinnerNumberModel(Prefs.mb_samplefreq, 1, 1000, 1);
		this.samFreq.setModel(mFreq);
		
		SpinnerNumberModel mBurn = new SpinnerNumberModel(Prefs.mb_burnin, 1, 99, 1);
		this.burnin.setModel(mBurn);
	}
    
    public void setDefaults() {
    	String model;
    	if(ss.getProps().isNucleotides())
    		model = Prefs.mb_default_dnamodel;
    	else
    		model = Prefs.mb_default_proteinmodel;
    	
    	this.subModel.setSelectedItem(model);
    	
    	this.gamma.setSelected(Prefs.mb_default_model_gamma);
		this.inv.setSelected(Prefs.mb_default_model_inv);
		
    	SpinnerNumberModel mNRuns = new SpinnerNumberModel(Prefs.mb_runs_default, 1, 5, 1);
		this.nRuns.setModel(mNRuns);
		
		SpinnerNumberModel mNGen = new SpinnerNumberModel(Prefs.mb_gens_default, 10000, 500000, 10000);
		this.nGen.setModel(mNGen);
		
		SpinnerNumberModel mFreq = new SpinnerNumberModel(Prefs.mb_samplefreq_default, 1, 1000, 1);
		this.samFreq.setModel(mFreq);
		
		SpinnerNumberModel mBurn = new SpinnerNumberModel(Prefs.mb_burnin_default, 1, 99, 1);
		this.burnin.setModel(mBurn);
    }
    
    public void initPrevResult(MBTreeResult res) {
    	this.nRuns.setValue(res.nRuns);
    	this.nGen.setValue(res.nGen);
    	this.burnin.setValue((int)(res.burnin*100));
    	this.samFreq.setValue(res.sampleFreq);
    }
    
    public MBTreeResult onOK() {
		
		String name = (String)subModel.getSelectedItem();
		boolean g = gamma.isSelected();
		boolean i = inv.isSelected();
		
		ss.getProps().setModel(ModelManager.getInstance().generateModel(name, g, i));
		
		int length = data.getActiveRegionE()-data.getActiveRegionS()+1;
		MBPartition p = new MBPartition("1-"+length, "part", ModelManager.getInstance().generateModel(name, g, i));
		result.partitions.add(p);
		result.nRuns = (Integer) nRuns.getValue();
		result.burnin = ((Integer)burnin.getValue()).doubleValue()/100d;
		result.nGen = (Integer)nGen.getValue();
		result.sampleFreq = (Integer)samFreq.getValue();
		
		Prefs.mb_runs = result.nRuns;
		Prefs.mb_burnin = (Integer)burnin.getValue();
		Prefs.mb_gens = result.nGen;
		Prefs.mb_samplefreq = result.sampleFreq;
		
		return result;
	}
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel3 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        samFreq = new javax.swing.JSpinner();
        nGen = new javax.swing.JSpinner();
        burnin = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        nRuns = new javax.swing.JSpinner();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        subModel = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        inv = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        gamma = new javax.swing.JCheckBox();

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(Text.get("General_Settings")));

        jLabel5.setText(Text.get("Sample_Frequency"));

        jLabel6.setText(Text.get("Burnin_(in_%)"));

        samFreq.setToolTipText("Sample frequency");

        nGen.setToolTipText("Number of generations");

        burnin.setToolTipText("Length of burnin period (relative to number of samples generated)");

        jLabel4.setText(Text.get("nGenerations"));

        jLabel7.setText(Text.get("nRuns"));

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel6)
                    .add(jLabel5)
                    .add(jLabel4)
                    .add(jLabel7))
                .add(30, 30, 30)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, burnin, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, samFreq, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, nGen, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE)
                    .add(nRuns, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel7)
                    .add(nRuns, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(nGen, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel4))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(samFreq, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel5))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(burnin, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel6))
                .addContainerGap(11, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(Text.get("Model_Settings")));

        jLabel1.setText(Text.get("Substitution_Model"));

        subModel.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        subModel.setToolTipText("Substitution model to use");

        jLabel2.setText(Text.get("Invariant_Sites"));

        inv.setToolTipText("Allow invariant sites");
        inv.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        inv.setMargin(new java.awt.Insets(0, 0, 0, 0));

        jLabel3.setText(Text.get("Gamma"));

        gamma.setToolTipText("Use gamma distribution");
        gamma.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gamma.setMargin(new java.awt.Insets(0, 0, 0, 0));

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel1)
                    .add(jLabel3)
                    .add(jLabel2))
                .add(32, 32, 32)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(gamma)
                    .add(inv)
                    .add(subModel, 0, 209, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(0, 0, 0)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel3)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel2))
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(subModel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(gamma)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(inv)))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner burnin;
    private javax.swing.JCheckBox gamma;
    private javax.swing.JCheckBox inv;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JSpinner nGen;
    private javax.swing.JSpinner nRuns;
    private javax.swing.JSpinner samFreq;
    private javax.swing.JComboBox subModel;
    // End of variables declaration//GEN-END:variables
    
}
