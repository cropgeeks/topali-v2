/*
 * AdvancedCDNAMrBayes.java
 *
 * Created on 18 October 2007, 15:58
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
public class AdvancedCDNAMrBayes extends javax.swing.JPanel {
    
	MBTreeResult result;
	SequenceSet ss;
	
    /** Creates new form AdvancedCDNAMrBayes */
    public AdvancedCDNAMrBayes(SequenceSet ss) {
    	this.result =new MBTreeResult();
    	this.ss = ss;
        initComponents();
        setDefaults();
    }
    
    public void initPrevResult(MBTreeResult result) {
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
    	
    	for(String s : result.linkedParameters) {
    		String[] split = s.split(",");
    		if(split[0].equals("pinvar")) {
    			for(int i=1; i<split.length; i++) {
    				if(split[i].equals("1")) 
    					pinvar1.setSelected(true);
    				if(split[i].equals("2")) 
    					pinvar2.setSelected(true);
    				if(split[i].equals("3")) 
    					pinvar3.setSelected(true);
    			}
    		}
    		if(split[0].equals("revmat")) {
    			for(int i=1; i<split.length; i++) {
    				if(split[i].equals("1")) 
    					revmat1.setSelected(true);
    				if(split[i].equals("2")) 
    					revmat2.setSelected(true);
    				if(split[i].equals("3")) 
    					revmat3.setSelected(true);
    			}
    		}
    		if(split[0].equals("shape")) {
    			for(int i=1; i<split.length; i++) {
    				if(split[i].equals("1")) 
    					shape1.setSelected(true);
    				if(split[i].equals("2")) 
    					shape2.setSelected(true);
    				if(split[i].equals("3")) 
    					shape3.setSelected(true);
    			}
    		}
    		if(split[0].equals("statefreq")) {
    			for(int i=1; i<split.length; i++) {
    				if(split[i].equals("1")) 
    					statefreq1.setSelected(true);
    				if(split[i].equals("2")) 
    					statefreq2.setSelected(true);
    				if(split[i].equals("3")) 
    					statefreq3.setSelected(true);
    			}
    		}
    		if(split[0].equals("tratio")) {
    			for(int i=1; i<split.length; i++) {
    				if(split[i].equals("1")) 
    					tratio1.setSelected(true);
    				if(split[i].equals("2")) 
    					tratio2.setSelected(true);
    				if(split[i].equals("3")) 
    					tratio3.setSelected(true);
    			}
    		}
    	}
    	
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
    	
    	Model defModel = ss.getProps().getModel();
    	if(Utils.indexof(models, defModel.getName())==-1) {
			if(ss.getProps().isNucleotides())
				defModel = ModelManager.getInstance().generateModel(Prefs.mb_default_dnamodel, defModel.isGamma(), defModel.isInv());
			else
				defModel = ModelManager.getInstance().generateModel(Prefs.mb_default_proteinmodel, defModel.isGamma(), defModel.isInv());
		}
    	p1mod.setSelectedItem(defModel.getName());
    	p2mod.setSelectedItem(defModel.getName());
    	p3mod.setSelectedItem(defModel.getName());
    	
    	p1g.setSelected(defModel.isGamma());
    	p2g.setSelected(defModel.isGamma());
    	p3g.setSelected(defModel.isGamma());
    	
    	p1i.setSelected(defModel.isInv());
    	p2i.setSelected(defModel.isInv());
    	p3i.setSelected(defModel.isInv());
    	
    	pinvar1.setSelected(false);
    	pinvar2.setSelected(false);
    	pinvar3.setSelected(false);
    	revmat1.setSelected(false);
    	revmat2.setSelected(false);
    	revmat3.setSelected(false);
    	shape1.setSelected(false);
    	shape2.setSelected(false);
    	shape3.setSelected(false);
    	statefreq1.setSelected(false);
    	statefreq2.setSelected(false);
    	statefreq3.setSelected(false);
    	tratio1.setSelected(false);
    	tratio2.setSelected(false);
    	tratio3.setSelected(false);
    	
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
    	
    	//create partitions
    	MBPartition p1 = new MBPartition("1-.\\3", "p1", ModelManager.getInstance().generateModel((String)p1mod.getSelectedItem(), p1g.isSelected(), p1i.isSelected()));
    	MBPartition p2 = new MBPartition("2-.\\3", "p2", ModelManager.getInstance().generateModel((String)p2mod.getSelectedItem(), p2g.isSelected(), p2i.isSelected()));
    	MBPartition p3 = new MBPartition("3-.\\3", "p3", ModelManager.getInstance().generateModel((String)p3mod.getSelectedItem(), p3g.isSelected(), p3i.isSelected()));
    	result.partitions.add(p1);
    	result.partitions.add(p2);
    	result.partitions.add(p3);
    	
    	
    	//create parameter linking
    	String link = "pinvar";
    	String one = null;
    	String two = null;
    	String three = null;
    	int count = 0;
    	if(pinvar1.isSelected()) {
    		one = "1";
    		count++;
    	}
    	if(pinvar2.isSelected()) {
    		two = "2";
    		count++;
    	}
    	if(pinvar3.isSelected()) {
    		three = "3";
    		count++;
    	}
    	if(count>1) {
    		String s = link+",";
    		if(one!=null)
    			s += one+",";
    		if(two!=null)
    			s += two+",";
    		if(three!=null)
    			s += three+",";
    		result.linkedParameters.add(s.substring(0, s.length()-1));
    	}
    	
    	link = "revmat";
    	one = null;
    	two = null;
    	three = null;
    	count = 0;
    	if(revmat1.isSelected()) {
    		one = "1";
    		count++;
    	}
    	if(revmat2.isSelected()) {
    		two = "2";
    		count++;
    	}
    	if(revmat3.isSelected()) {
    		three = "3";
    		count++;
    	}
    	if(count>1) {
    		String s = link+",";
    		if(one!=null)
    			s += one+",";
    		if(two!=null)
    			s += two+",";
    		if(three!=null)
    			s += three+",";
    		result.linkedParameters.add(s.substring(0, s.length()-1));
    	}
   	
    	link = "shape";
    	one = null;
    	two = null;
    	three = null;
    	count = 0;
    	if(shape1.isSelected()) {
    		one = "1";
    		count++;
    	}
    	if(shape2.isSelected()) {
    		two = "2";
    		count++;
    	}
    	if(shape3.isSelected()) {
    		three = "3";
    		count++;
    	}
    	if(count>1) {
    		String s = link+",";
    		if(one!=null)
    			s += one+",";
    		if(two!=null)
    			s += two+",";
    		if(three!=null)
    			s += three+",";
    		result.linkedParameters.add(s.substring(0, s.length()-1));
    	}
    	
    	link = "statefreq";
    	one = null;
    	two = null;
    	three = null;
    	count = 0;
    	if(statefreq1.isSelected()) {
    		one = "1";
    		count++;
    	}
    	if(statefreq2.isSelected()) {
    		two = "2";
    		count++;
    	}
    	if(statefreq3.isSelected()) {
    		three = "3";
    		count++;
    	}
    	if(count>1) {
    		String s = link+",";
    		if(one!=null)
    			s += one+",";
    		if(two!=null)
    			s += two+",";
    		if(three!=null)
    			s += three+",";
    		result.linkedParameters.add(s.substring(0, s.length()-1));
    	}
    	
    	link = "tratio";
    	one = null;
    	two = null;
    	three = null;
    	count = 0;
    	if(tratio1.isSelected()) {
    		one = "1";
    		count++;
    	}
    	if(tratio2.isSelected()) {
    		two = "2";
    		count++;
    	}
    	if(tratio3.isSelected()) {
    		three = "3";
    		count++;
    	}
    	if(count>1) {
    		String s = link+",";
    		if(one!=null)
    			s += one+",";
    		if(two!=null)
    			s += two+",";
    		if(three!=null)
    			s += three+",";
    		result.linkedParameters.add(s.substring(0, s.length()-1));
    	}
    	
    	//set general settings
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
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
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
        jPanel6 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        statefreq1 = new javax.swing.JCheckBox();
        statefreq2 = new javax.swing.JCheckBox();
        statefreq3 = new javax.swing.JCheckBox();
        revmat1 = new javax.swing.JCheckBox();
        revmat2 = new javax.swing.JCheckBox();
        revmat3 = new javax.swing.JCheckBox();
        pinvar1 = new javax.swing.JCheckBox();
        pinvar2 = new javax.swing.JCheckBox();
        pinvar3 = new javax.swing.JCheckBox();
        shape1 = new javax.swing.JCheckBox();
        shape2 = new javax.swing.JCheckBox();
        shape3 = new javax.swing.JCheckBox();
        jLabel21 = new javax.swing.JLabel();
        tratio1 = new javax.swing.JCheckBox();
        tratio2 = new javax.swing.JCheckBox();
        tratio3 = new javax.swing.JCheckBox();

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 100, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 100, Short.MAX_VALUE)
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(Text.get("Codon_Position_1")));

        jLabel2.setText(Text.get("Substitution_Model"));

        jLabel3.setText(Text.get("Gamma"));

        jLabel4.setText(Text.get("Invariant_Sites"));

        p1mod.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        p1g.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        p1g.setMargin(new java.awt.Insets(0, 0, 0, 0));

        p1i.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        p1i.setMargin(new java.awt.Insets(0, 0, 0, 0));

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel2)
                    .add(jLabel3)
                    .add(jLabel4))
                .add(51, 51, 51)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(p1i)
                    .add(p1g)
                    .add(p1mod, 0, 190, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(p1mod, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(p1g)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(p1i))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jLabel2)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel3)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel4)))
                .addContainerGap(11, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(Text.get("Codon_Position_2")));

        jLabel6.setText(Text.get("Substitution_Model"));

        jLabel7.setText(Text.get("Gamma"));

        jLabel8.setText(Text.get("Invariant_Sites"));

        p2mod.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        p2g.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        p2g.setMargin(new java.awt.Insets(0, 0, 0, 0));

        p2i.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        p2i.setMargin(new java.awt.Insets(0, 0, 0, 0));

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel6)
                    .add(jLabel7)
                    .add(jLabel8))
                .add(50, 50, 50)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(p2i)
                    .add(p2g)
                    .add(p2mod, 0, 191, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(0, 0, 0)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jLabel6)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel7)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel8))
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(p2mod, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(p2g)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(p2i)))
                .addContainerGap(10, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(Text.get("Codon_Position_3")));

        jLabel10.setText(Text.get("Substitution_Model"));

        jLabel11.setText(Text.get("Gamma"));

        jLabel12.setText(Text.get("Invariant_Sites"));

        p3mod.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        p3g.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        p3g.setMargin(new java.awt.Insets(0, 0, 0, 0));

        p3i.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        p3i.setMargin(new java.awt.Insets(0, 0, 0, 0));

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel10)
                    .add(jLabel11)
                    .add(jLabel12))
                .add(51, 51, 51)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(p3mod, 0, 190, Short.MAX_VALUE)
                    .add(p3i)
                    .add(p3g))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(jLabel10)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel11)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel12))
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(p3mod, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(p3g)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(p3i)))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(Text.get("General_Settings")));

        jLabel13.setText(Text.get("Sample_Frequency"));

        jLabel14.setText(Text.get("Burnin_(in_%)"));

        samFreq.setToolTipText("Sample frequency");

        nGen.setToolTipText("Number of generations");

        burnin.setToolTipText("Length of burnin period (relative to number of samples generated)");

        jLabel15.setText(Text.get("nGenerations"));

        jLabel16.setText(Text.get("nRuns"));

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel16)
                    .add(jLabel15)
                    .add(jLabel13)
                    .add(jLabel14))
                .add(53, 53, 53)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(burnin, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
                    .add(samFreq, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
                    .add(nGen, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, nRuns, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel16)
                    .add(nRuns, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel15)
                    .add(nGen, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel13)
                    .add(samFreq, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel14)
                    .add(burnin, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder(Text.get("Parameter_Linking_across_Codon_Positions")));

        jLabel1.setText(Text.get("Base_comp."));

        jLabel5.setText(Text.get("GTR/SYM_rates"));

        jLabel9.setText(Text.get("pINV"));

        jLabel17.setText(Text.get("alpha_(Gamma_dist.)"));

        jLabel18.setText("P3");

        jLabel19.setText("P2");

        jLabel20.setText("P1");

        statefreq1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        statefreq1.setMargin(new java.awt.Insets(0, 0, 0, 0));

        statefreq2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        statefreq2.setMargin(new java.awt.Insets(0, 0, 0, 0));

        statefreq3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        statefreq3.setMargin(new java.awt.Insets(0, 0, 0, 0));

        revmat1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        revmat1.setMargin(new java.awt.Insets(0, 0, 0, 0));

        revmat2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        revmat2.setMargin(new java.awt.Insets(0, 0, 0, 0));

        revmat3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        revmat3.setMargin(new java.awt.Insets(0, 0, 0, 0));

        pinvar1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        pinvar1.setMargin(new java.awt.Insets(0, 0, 0, 0));

        pinvar2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        pinvar2.setMargin(new java.awt.Insets(0, 0, 0, 0));

        pinvar3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        pinvar3.setMargin(new java.awt.Insets(0, 0, 0, 0));

        shape1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        shape1.setMargin(new java.awt.Insets(0, 0, 0, 0));

        shape2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        shape2.setMargin(new java.awt.Insets(0, 0, 0, 0));

        shape3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        shape3.setMargin(new java.awt.Insets(0, 0, 0, 0));

        jLabel21.setText(Text.get("HKY_K80_Ts/Tv"));

        tratio1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tratio1.setMargin(new java.awt.Insets(0, 0, 0, 0));

        tratio2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tratio2.setMargin(new java.awt.Insets(0, 0, 0, 0));

        tratio3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tratio3.setMargin(new java.awt.Insets(0, 0, 0, 0));

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel1)
                    .add(jLabel5)
                    .add(jLabel9)
                    .add(jLabel17)
                    .add(jLabel21))
                .add(46, 46, 46)
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel6Layout.createSequentialGroup()
                        .add(jLabel20)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel19)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel18))
                    .add(jPanel6Layout.createSequentialGroup()
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(statefreq1)
                            .add(revmat1)
                            .add(pinvar1)
                            .add(shape1)
                            .add(tratio1))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(statefreq2)
                            .add(revmat2)
                            .add(pinvar2)
                            .add(shape2)
                            .add(tratio2))
                        .add(6, 6, 6)
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(tratio3)
                            .add(shape3)
                            .add(pinvar3)
                            .add(revmat3)
                            .add(statefreq3))))
                .addContainerGap(154, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel20)
                    .add(jLabel19)
                    .add(jLabel18))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel6Layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel5)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel9)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel17)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel21))
                    .add(jPanel6Layout.createSequentialGroup()
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(statefreq2)
                            .add(statefreq1)
                            .add(statefreq3))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 9, Short.MAX_VALUE)
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(revmat3)
                            .add(revmat2)
                            .add(revmat1))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(pinvar2)
                            .add(pinvar3)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel6Layout.createSequentialGroup()
                                .add(pinvar1)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 8, Short.MAX_VALUE)
                                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(shape1)
                                    .add(shape2)
                                    .add(shape3))))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(tratio1)
                            .add(tratio2)
                            .add(tratio3))))
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 98, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner burnin;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
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
    private javax.swing.JCheckBox pinvar1;
    private javax.swing.JCheckBox pinvar2;
    private javax.swing.JCheckBox pinvar3;
    private javax.swing.JCheckBox revmat1;
    private javax.swing.JCheckBox revmat2;
    private javax.swing.JCheckBox revmat3;
    private javax.swing.JSpinner samFreq;
    private javax.swing.JCheckBox shape1;
    private javax.swing.JCheckBox shape2;
    private javax.swing.JCheckBox shape3;
    private javax.swing.JCheckBox statefreq1;
    private javax.swing.JCheckBox statefreq2;
    private javax.swing.JCheckBox statefreq3;
    private javax.swing.JCheckBox tratio1;
    private javax.swing.JCheckBox tratio2;
    private javax.swing.JCheckBox tratio3;
    // End of variables declaration//GEN-END:variables
    
}
