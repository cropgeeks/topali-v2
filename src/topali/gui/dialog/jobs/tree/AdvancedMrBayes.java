/*
 * AdvancedMrBayes.java
 *
 * Created on 07 September 2007, 08:35
 */

package topali.gui.dialog.jobs.tree;

import java.util.*;

import javax.swing.*;

import doe.MsgBox;

import topali.cluster.jobs.mrbayes.MBCmdBuilder;
import topali.data.*;
import topali.data.models.*;
import topali.gui.Prefs;
import topali.var.Utils;

/**
 *
 * @author  dlindn
 */
public class AdvancedMrBayes extends javax.swing.JPanel {
    
	SequenceSet ss;
	MBTreeResult result;
	
	public boolean modelIsSupported = true;
	public boolean altModelFound = true;
	public String altModel = "";
	
    /** Creates new form AdvancedMrBayes */
    public AdvancedMrBayes(SequenceSet ss, MBTreeResult result) {
    	this.ss = ss;
		this.result = new MBTreeResult();
		
        initComponents();
        setDefaults();
        
        if(result!=null)
        	initPrevResult(result);
    }
    
    private void setDefaults() {
		SequenceSetParams params = ss.getParams();
		
		List<Model> mlist = ModelManager.getInstance().listMrBayesModels(ss.isDNA());
		String[] models = new String[mlist.size()];
		for(int i=0; i<mlist.size(); i++)
			models[i] = mlist.get(i).getName();
		
		ComboBoxModel cm = new DefaultComboBoxModel(models);
		this.subModel.setModel(cm);

		Model m = params.getModel();
		while(!Utils.contains(models, m.getName())) {
			modelIsSupported = false;
			Model next = ModelManager.getInstance().getNearestModel(m);
			if(next.getName().equals(m.getName())) {
				altModelFound = false;
				if(ss.isDNA()) 
					m = ModelManager.getInstance().generateModel("hky", m.isGamma(), m.isInv());
				else
					m = ModelManager.getInstance().generateModel("wag", m.isGamma(), m.isInv());
				break;
			}
			else 
				m = next;
		}
		altModel = m.getName();
		
		for(int i=0; i<models.length; i++) {
			if(models[i].equals(m.getName())) {
				this.subModel.setSelectedIndex(i);
				break;
			}
		}
		
		
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
    
    private void initPrevResult(MBTreeResult res) {
    	this.nRuns.setValue(res.nRuns);
    	this.nGen.setValue(res.nGen);
    	this.burnin.setValue((int)(res.burnin*100));
    	this.samFreq.setValue(res.sampleFreq);
    }
    
    public MBTreeResult onOK() {
		
		String name = (String)subModel.getSelectedItem();
		boolean g = gamma.isSelected();
		boolean i = inv.isSelected();
		
		ss.getParams().setModel(ModelManager.getInstance().generateModel(name, g, i));
		
		MBPartition p = new MBPartition("1-"+ss.getLength(), "part", ModelManager.getInstance().generateModel(name, g, i));
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
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
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

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("General MrBayes Settings"));
        jLabel5.setText("Sample Frequency:");

        jLabel6.setText("Burnin (in %):");

        samFreq.setToolTipText("Sample frequency");

        nGen.setToolTipText("Number of generations");

        burnin.setToolTipText("Length of burnin period (relative to number of samples generated)");

        jLabel4.setText("nGenerations:");

        jLabel7.setText("nRuns:");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addGap(62, 62, 62)
                        .addComponent(nRuns, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nGen, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
                            .addComponent(samFreq, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
                            .addComponent(burnin, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(nRuns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(nGen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(samFreq, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(burnin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Model parameters"));
        jLabel1.setText("Substitution Model:");

        subModel.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        subModel.setToolTipText("Substitution model to use");

        jLabel2.setText("Invariant Sites:");

        inv.setToolTipText("Allow invariant sites");
        inv.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        inv.setMargin(new java.awt.Insets(0, 0, 0, 0));

        jLabel3.setText("Gamma:");

        gamma.setToolTipText("Use gamma distribution");
        gamma.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gamma.setMargin(new java.awt.Insets(0, 0, 0, 0));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel3)
                    .addComponent(jLabel2))
                .addGap(4, 4, 4)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(gamma)
                    .addComponent(inv)
                    .addComponent(subModel, 0, 107, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(subModel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(gamma))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(inv)
                    .addComponent(jLabel2))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
