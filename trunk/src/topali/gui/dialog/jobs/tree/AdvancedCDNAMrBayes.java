/*
 * AdvancedCDNAMrBayes.java
 *
 * Created on 18 October 2007, 15:58
 */

package topali.gui.dialog.jobs.tree;

import java.util.List;

import javax.swing.*;

import topali.cluster.jobs.modelgenerator.ModelGeneratorProcess;
import topali.cluster.jobs.mrbayes.MBCmdBuilder;
import topali.data.*;
import topali.data.models.*;
import topali.gui.Prefs;

/**
 *
 * @author  dlindn
 */
public class AdvancedCDNAMrBayes extends javax.swing.JPanel {
    
	MBTreeResult result;
	SequenceSet ss;
	
    /** Creates new form AdvancedCDNAMrBayes */
    public AdvancedCDNAMrBayes(SequenceSet ss, MBTreeResult result) {
    	this.result =new MBTreeResult();
    	this.ss = ss;
        initComponents();
        setDefaults();
        
        if(result!=null)
        	initPrevResult(result);
    }
    
    private void initPrevResult(MBTreeResult result) {
    	MBPartition p1 = result.partitions.get(0);
    	MBPartition p2 = result.partitions.get(1);
    	MBPartition p3 = result.partitions.get(2);
    	
    	p1mod.setSelectedItem(p1.model.getName());
    	p2mod.setSelectedItem(p2.model.getName());
    	p3mod.setSelectedItem(p3.model.getName());
    	
    	p1g.setSelected(p1.model.isGamma());
    	p2g.setSelected(p2.model.isGamma());
    	p3g.setSelected(p3.model.isGamma());
    	
    	p1i.setSelected(p1.model.isInv());
    	p2i.setSelected(p2.model.isInv());
    	p3i.setSelected(p3.model.isInv());
    	
    	nRuns.setValue(result.nRuns);
    	nGen.setValue(result.nGen);
    	samFreq.setValue(result.sampleFreq);
    	burnin.setValue((int)(result.burnin*100));
    }
    
    public void setDefaults() {
    	List<Model> mlist = ModelManager.getInstance().listMrBayesModels(true);
    	String[] models = new String[mlist.size()];
    	for(int i=0; i<mlist.size(); i++)
    		models[i] = mlist.get(i).getName();
    	
    	p1mod.setModel(new DefaultComboBoxModel(models));
    	p2mod.setModel(new DefaultComboBoxModel(models));
    	p3mod.setModel(new DefaultComboBoxModel(models));
    	
    	Model defModel = ss.getParams().getModel();
    	p1mod.setSelectedItem(defModel.getName());
    	p2mod.setSelectedItem(defModel.getName());
    	p3mod.setSelectedItem(defModel.getName());
    	
    	p1g.setSelected(defModel.isGamma());
    	p2g.setSelected(defModel.isGamma());
    	p3g.setSelected(defModel.isGamma());
    	
    	p1i.setSelected(defModel.isInv());
    	p2i.setSelected(defModel.isInv());
    	p3i.setSelected(defModel.isInv());
    	
    	SpinnerNumberModel mNRuns = new SpinnerNumberModel(Prefs.mb_runs, 1, 5, 1);
		this.nRuns.setModel(mNRuns);
		
		SpinnerNumberModel mNGen = new SpinnerNumberModel(Prefs.mb_gens, 10000, 500000, 10000);
		this.nGen.setModel(mNGen);
		
		SpinnerNumberModel mFreq = new SpinnerNumberModel(Prefs.mb_samplefreq, 1, 1000, 1);
		this.samFreq.setModel(mFreq);
		
		SpinnerNumberModel mBurn = new SpinnerNumberModel(Prefs.mb_burnin, 1, 99, 1);
		this.burnin.setModel(mBurn);
    }
    
    public MBTreeResult onOk() {
    	MBPartition p1 = new MBPartition("1-.\\3", "p1", ModelManager.getInstance().generateModel((String)p1mod.getSelectedItem(), p1g.isSelected(), p1i.isSelected()));
    	MBPartition p2 = new MBPartition("2-.\\3", "p2", ModelManager.getInstance().generateModel((String)p2mod.getSelectedItem(), p2g.isSelected(), p2i.isSelected()));
    	MBPartition p3 = new MBPartition("3-.\\3", "p3", ModelManager.getInstance().generateModel((String)p3mod.getSelectedItem(), p3g.isSelected(), p3i.isSelected()));
    	result.partitions.add(p1);
    	result.partitions.add(p2);
    	result.partitions.add(p3);
    	
    	result.nRuns = (Integer) nRuns.getValue();
		result.burnin = ((Integer)burnin.getValue()).doubleValue()/100d;
		result.nGen = (Integer)nGen.getValue();
		result.sampleFreq = (Integer)samFreq.getValue();
    	
		Prefs.mb_burnin = (Integer)burnin.getValue();
		Prefs.mb_gens = result.nGen;
		Prefs.mb_runs = result.nRuns;
		Prefs.mb_samplefreq = result.sampleFreq;
		
    	return result;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jPanel4 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        p1mod = new javax.swing.JComboBox();
        p1g = new javax.swing.JCheckBox();
        p1i = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        p2mod = new javax.swing.JComboBox();
        p2g = new javax.swing.JCheckBox();
        p2i = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        p3mod = new javax.swing.JComboBox();
        p3g = new javax.swing.JCheckBox();
        p3i = new javax.swing.JCheckBox();
        jPanel5 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        samFreq = new javax.swing.JSpinner();
        nGen = new javax.swing.JSpinner();
        burnin = new javax.swing.JSpinner();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        nRuns = new javax.swing.JSpinner();

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Codon Position 1"));
        jLabel2.setText("Substitution Model:");

        jLabel3.setText("Gamma:");

        jLabel4.setText("Invariable Sites:");

        p1mod.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        p1g.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        p1g.setMargin(new java.awt.Insets(0, 0, 0, 0));

        p1i.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        p1i.setMargin(new java.awt.Insets(0, 0, 0, 0));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4))
                .addGap(4, 4, 4)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(p1i)
                    .addComponent(p1g)
                    .addComponent(p1mod, 0, 106, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(p1mod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(p1g))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(p1i))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Codon Position 2"));
        jLabel6.setText("Substitution Model:");

        jLabel7.setText("Gamma:");

        jLabel8.setText("Invariable Sites:");

        p2mod.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        p2g.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        p2g.setMargin(new java.awt.Insets(0, 0, 0, 0));

        p2i.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        p2i.setMargin(new java.awt.Insets(0, 0, 0, 0));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7)
                    .addComponent(jLabel8))
                .addGap(4, 4, 4)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(p2i)
                    .addComponent(p2g)
                    .addComponent(p2mod, 0, 106, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(p2mod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(p2g))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(p2i)))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Codon Position 3"));
        jLabel10.setText("Substitution Model:");

        jLabel11.setText("Gamma:");

        jLabel12.setText("Invariable Sites:");

        p3mod.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        p3g.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        p3g.setMargin(new java.awt.Insets(0, 0, 0, 0));

        p3i.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        p3i.setMargin(new java.awt.Insets(0, 0, 0, 0));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel10)
                    .addComponent(jLabel11)
                    .addComponent(jLabel12))
                .addGap(4, 4, 4)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(p3i)
                    .addComponent(p3g)
                    .addComponent(p3mod, 0, 106, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(p3mod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(p3g))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(p3i)))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("General MrBayes Settings"));
        jLabel13.setText("Sample Frequency:");

        jLabel14.setText("Burnin (in %):");

        samFreq.setToolTipText("Sample frequency");

        nGen.setToolTipText("Number of generations");

        burnin.setToolTipText("Length of burnin period (relative to number of samples generated)");

        jLabel15.setText("nGenerations:");

        jLabel16.setText("nRuns:");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel16)
                        .addGap(62, 62, 62)
                        .addComponent(nRuns, javax.swing.GroupLayout.DEFAULT_SIZE, 106, Short.MAX_VALUE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel15)
                            .addComponent(jLabel13)
                            .addComponent(jLabel14))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nGen, javax.swing.GroupLayout.DEFAULT_SIZE, 106, Short.MAX_VALUE)
                            .addComponent(samFreq, javax.swing.GroupLayout.DEFAULT_SIZE, 106, Short.MAX_VALUE)
                            .addComponent(burnin, javax.swing.GroupLayout.DEFAULT_SIZE, 106, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(nRuns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(nGen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(samFreq, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(burnin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner burnin;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JSpinner nGen;
    private javax.swing.JSpinner nRuns;
    private javax.swing.JCheckBox p1g;
    private javax.swing.JCheckBox p1i;
    private javax.swing.JComboBox p1mod;
    private javax.swing.JCheckBox p2g;
    private javax.swing.JCheckBox p2i;
    private javax.swing.JComboBox p2mod;
    private javax.swing.JCheckBox p3g;
    private javax.swing.JCheckBox p3i;
    private javax.swing.JComboBox p3mod;
    private javax.swing.JSpinner samFreq;
    // End of variables declaration//GEN-END:variables
    
}
