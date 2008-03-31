/*
 * EditAnnotationDialog.java
 *
 * Created on 19 February 2008, 10:53
 */

package topali.gui.dialog.annotation;

import java.util.List;
import javax.swing.*;
import topali.data.*;
import topali.data.annotations.*;
import topali.data.models.*;
import topali.i18n.Text;
import topali.var.utils.*;

/**
 *
 * @author  dlindn
 */
public class EditPartAnnoDialog extends javax.swing.JDialog {
	
	public int aStart, aEnd, aType;
	public String aModel, aComment;
	public boolean aGamma, aInv;
	
	public boolean cancelled = true;
	
	AlignmentData data;
	PartitionAnnotation anno;
	
    /** Creates new form EditAnnotationDialog */
    public EditPartAnnoDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
  
        init2();
    }
    
    public void init2() {
    	setTitle(Text.get("EditPartAnnoDialog.9"));
    	getRootPane().setDefaultButton(bOk);
    	Utils.addCloseHandler(this, bCancel);
    }
    
    public void newAnnotation(AlignmentData data) {
    	this.data = data;
    	
    	int maxLength = data.getSequenceSet().getLength();
    	SpinnerNumberModel mod1 = new SpinnerNumberModel(1, 1, maxLength, 1);
    	start.setModel(mod1);
    	SpinnerNumberModel mod2 = new SpinnerNumberModel(maxLength, 1, maxLength, 1);
    	end.setModel(mod2);
    	
    	DefaultComboBoxModel mod3 = new DefaultComboBoxModel(new String[]{"DNA", "RNA", "Protein", "Unknown"});
    	seqType.setModel(mod3);
    	int sel = 3;
    	int type = data.getSequenceSet().getProps().getType();
    	if(type==SequenceSetProperties.TYPE_DNA)
    		sel = 0;
    	else if(type==SequenceSetProperties.TYPE_RNA)
    		sel = 1;
    	else if(type==SequenceSetProperties.TYPE_PROTEIN)
    		sel = 2;
    	seqType.setSelectedIndex(sel);
    	
    	List<Model> models = null;
    	if(type==SequenceSetProperties.TYPE_DNA || type==SequenceSetProperties.TYPE_RNA || type==(SequenceSetProperties.TYPE_DNA+SequenceSetProperties.TYPE_RNA)) 
    		models = ModelManager.getInstance().listDNAModels();
    	else if(type==SequenceSetProperties.TYPE_PROTEIN)
    		models = ModelManager.getInstance().listProteinModels();
    	
    	if(models!=null) {
    		String selModel = data.getSequenceSet().getProps().getModel().getName();
    		String[] modNames = new String[models.size()];
    		int selIndex = 0;
    		for(int i=0; i<modNames.length; i++) {
    			modNames[i] = models.get(i).getName();
    			if(modNames[i].equals(selModel))
    				selIndex = i;
    		}
    		DefaultComboBoxModel mod4 = new DefaultComboBoxModel(modNames);
        	model.setModel(mod4);
        	model.setSelectedIndex(selIndex);
        	gamma.setSelected(data.getSequenceSet().getProps().getModel().isGamma());
        	inv.setSelected(data.getSequenceSet().getProps().getModel().isInv());
    	}
    	else {
    		model.setModel(new DefaultComboBoxModel());
    		model.setEnabled(false);
    		gamma.setEnabled(false);
    		inv.setEnabled(false);
    	}
    }
    
    public void setAnnotation(PartitionAnnotation anno, AlignmentData data) {
    	this.anno = anno;
    	this.data = data;
    	
    	int maxLength = data.getSequenceSet().getLength();
    	SpinnerNumberModel mod1 = new SpinnerNumberModel(anno.getStart(), 1, maxLength, 1);
    	start.setModel(mod1);
    	SpinnerNumberModel mod2 = new SpinnerNumberModel(anno.getEnd(), 1, maxLength, 1);
    	end.setModel(mod2);
    	if(anno.getComment()!=null)
    		comment.setText(anno.getComment());
    	
    	DefaultComboBoxModel mod3 = new DefaultComboBoxModel(new String[]{"DNA", "RNA", "Protein", "Unknown"});
    	seqType.setModel(mod3);
    	int sel = 3;
    	int type = anno.getSeqType();
    	if(type==SequenceSetProperties.TYPE_DNA)
    		sel = 0;
    	else if(type==SequenceSetProperties.TYPE_RNA)
    		sel = 1;
    	else if(type==SequenceSetProperties.TYPE_PROTEIN)
    		sel = 2;
    	seqType.setSelectedIndex(sel);
    	
    	List<Model> models = null;
    	if(type==SequenceSetProperties.TYPE_DNA || type==SequenceSetProperties.TYPE_RNA || type==(SequenceSetProperties.TYPE_DNA+SequenceSetProperties.TYPE_RNA)) 
    		models = ModelManager.getInstance().listDNAModels();
    	else if(type==SequenceSetProperties.TYPE_PROTEIN)
    		models = ModelManager.getInstance().listProteinModels();
    	
    	if(anno.getModel()!=null && models!=null) {
    		String selModel = anno.getModel().getName();
    		String[] modNames = new String[models.size()];
    		int selIndex = 0;
    		for(int i=0; i<modNames.length; i++) {
    			modNames[i] = models.get(i).getName();
    			if(modNames[i].equals(selModel))
    				selIndex = i;
    		}
    		DefaultComboBoxModel mod4 = new DefaultComboBoxModel(modNames);
        	model.setModel(mod4);
        	model.setSelectedIndex(selIndex);
        	gamma.setSelected(anno.getModel().isGamma());
        	inv.setSelected(anno.getModel().isInv());
    	}
    	else {
    		model.setModel(new DefaultComboBoxModel());
    		model.setEnabled(false);
    		gamma.setEnabled(false);
    		inv.setEnabled(false);
    	}
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        start = new javax.swing.JSpinner();
        end = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        model = new javax.swing.JComboBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        comment = new javax.swing.JTextArea();
        bCancel = new javax.swing.JButton();
        bOk = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        gamma = new javax.swing.JCheckBox();
        inv = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        seqType = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        jLabel1.setText(topali.i18n.Text.get("EditPartAnnoDialog.1"));
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel2.setText(topali.i18n.Text.get("EditPartAnnoDialog.2"));
        jLabel2.setName("jLabel2"); // NOI18N

        start.setName("start"); // NOI18N

        end.setName("end"); // NOI18N

        jLabel4.setText(topali.i18n.Text.get("EditPartAnnoDialog.4"));
        jLabel4.setName("jLabel4"); // NOI18N

        jLabel5.setText(topali.i18n.Text.get("EditPartAnnoDialog.8"));
        jLabel5.setName("jLabel5"); // NOI18N

        model.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        model.setName("model"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        comment.setColumns(20);
        comment.setRows(5);
        comment.setName("comment"); // NOI18N
        jScrollPane1.setViewportView(comment);

        bCancel.setText(topali.i18n.Text.get("cancel"));
        bCancel.setName("bCancel"); // NOI18N
        bCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bCancelActionPerformed(evt);
            }
        });

        bOk.setText(topali.i18n.Text.get("ok"));
        bOk.setName("bOk"); // NOI18N
        bOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bOkActionPerformed(evt);
            }
        });

        jLabel6.setText(topali.i18n.Text.get("EditPartAnnoDialog.5"));
        jLabel6.setName("jLabel6"); // NOI18N

        jLabel7.setText(topali.i18n.Text.get("EditPartAnnoDialog.6"));
        jLabel7.setName("jLabel7"); // NOI18N

        jLabel8.setText(topali.i18n.Text.get("EditPartAnnoDialog.7"));
        jLabel8.setName("jLabel8"); // NOI18N

        gamma.setName("gamma"); // NOI18N
        gamma.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gammaActionPerformed(evt);
            }
        });

        inv.setName("inv"); // NOI18N

        jLabel3.setText(topali.i18n.Text.get("EditPartAnnoDialog.10"));
        jLabel3.setName("jLabel3"); // NOI18N

        seqType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        seqType.setName("seqType"); // NOI18N
        seqType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                seqTypeActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(bOk)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(bCancel))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 298, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel1)
                            .add(jLabel2)
                            .add(jLabel3)
                            .add(jLabel4)
                            .add(jLabel6)
                            .add(jLabel7)
                            .add(jLabel8)
                            .add(jLabel5))
                        .add(36, 36, 36)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(inv)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, model, 0, 196, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, seqType, 0, 196, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, end, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE)
                            .add(start, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE)
                            .add(gamma))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(start, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(end, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(seqType, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jLabel4)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel6)
                    .add(model, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(jLabel7)
                        .add(9, 9, 9)
                        .add(jLabel8)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel5))
                    .add(layout.createSequentialGroup()
                        .add(gamma)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(inv)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(bCancel)
                    .add(bOk))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void bOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bOkActionPerformed
       aStart = (Integer)start.getValue();
       aEnd = (Integer)end.getValue();
       
       String type = (String)seqType.getSelectedItem();
       aType = SequenceSetProperties.TYPE_UNKNOWN;
       if(type.equals("DNA"))
    	   aType = SequenceSetProperties.TYPE_DNA;
       else if(type.equals("RNA"))
    	   aType = SequenceSetProperties.TYPE_RNA;
       else if(type.equals("Protein"))
    	   aType = SequenceSetProperties.TYPE_PROTEIN;
       
       if(model.isEnabled()) {
    	   aModel = (String)model.getSelectedItem();
    	   aGamma = gamma.isSelected();
    	   aInv = inv.isSelected();
       }
       else
    	   aModel = null;
       
       aComment = comment.getText();
       if(aComment.trim().equals(""))
    	   aComment = null;
       
       cancelled = false;
       
       setVisible(false);
       
}//GEN-LAST:event_bOkActionPerformed

    private void bCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bCancelActionPerformed
        this.setVisible(false);
    }//GEN-LAST:event_bCancelActionPerformed

    private void gammaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gammaActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_gammaActionPerformed

    private void seqTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_seqTypeActionPerformed
        String type = (String)seqType.getSelectedItem();
        
        List<Model> models = null;
    	if(type.equals("DNA") || type.equals("RNA")) 
    		models = ModelManager.getInstance().listDNAModels();
    	else if(type.equals("Protein"))
    		models = ModelManager.getInstance().listProteinModels();
    	
    	if(models!=null) {
    		Model selModel = null;
    		if(anno!=null)
    			selModel = anno.getModel();
    		else
    			selModel = data.getSequenceSet().getProps().getModel();
    		
    		String[] modNames = new String[models.size()];
    		int selIndex = 0;
    		for(int i=0; i<modNames.length; i++) {
    			modNames[i] = models.get(i).getName();
    			if(selModel!=null && modNames[i].equals(selModel.getName()))
    				selIndex = i;
    		}
    		DefaultComboBoxModel mod4 = new DefaultComboBoxModel(modNames);
        	model.setModel(mod4);
        	model.setSelectedIndex(selIndex);
        	if(selModel!=null)
        		gamma.setSelected(selModel.isGamma());
        	if(selModel!=null)
        		inv.setSelected(selModel.isInv());
        	
        	model.setEnabled(true);
    		gamma.setEnabled(true);
    		inv.setEnabled(true);
    	}
    	else {
    		model.setModel(new DefaultComboBoxModel());
    		model.setEnabled(false);
    		gamma.setEnabled(false);
    		inv.setEnabled(false);
    	}
}//GEN-LAST:event_seqTypeActionPerformed
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                EditPartAnnoDialog dialog = new EditPartAnnoDialog(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bCancel;
    private javax.swing.JButton bOk;
    private javax.swing.JTextArea comment;
    private javax.swing.JSpinner end;
    private javax.swing.JCheckBox gamma;
    private javax.swing.JCheckBox inv;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JComboBox model;
    private javax.swing.JComboBox seqType;
    private javax.swing.JSpinner start;
    // End of variables declaration//GEN-END:variables
    
}
