/*
 * CMLSitePanel.java
 *
 * Created on 07 September 2007, 10:06
 */

package topali.gui.dialog.jobs.cml;

import java.util.Vector;
import topali.data.AlignmentData;
import topali.data.CMLModel;
import topali.data.CodeMLResult;
import topali.gui.*;

/**
 *
 * @author  dlindn
 */
public class CMLSitePanel extends javax.swing.JPanel {
    
    CMLSiteDialog parent;
    CodeMLResult res;
    
    CMLModel mod0, mod1a, mod2a, mod3, mod7, mod8;
    
    /** Creates new form CMLSitePanel */
    public CMLSitePanel(CodeMLResult res, CMLSiteDialog parent) {
        this.res = (res==null) ? new CodeMLResult(CodeMLResult.TYPE_SITEMODEL) : res;
        this.parent = parent;
        
        initComponents();
        
        setDefaults();
			
        if(res!=null)
        	initPreviousResult(res);
    }
    
    public void setDefaults() {
    	mod0 = new CMLModel(CMLModel.MODEL_M0);
		mod1a = new CMLModel(CMLModel.MODEL_M1a);
		mod2a = new CMLModel(CMLModel.MODEL_M2a);
		mod3 = new CMLModel(CMLModel.MODEL_M3);
		mod7 = new CMLModel(CMLModel.MODEL_M7);
		mod8 = new CMLModel(CMLModel.MODEL_M8);
		
		m0.setSelected(true);
		m1a.setSelected(true);
		m2a.setSelected(true);
		m3.setSelected(false);
		m7.setSelected(false);
		m8.setSelected(false);
    }
    
    public CodeMLResult getResult() {
 
    	if(m0.isSelected())
    		res.models.addAll(mod0.generateModels());
    	if(m1a.isSelected())
    		res.models.addAll(mod1a.generateModels());
    	if(m2a.isSelected())
    		res.models.addAll(mod2a.generateModels());
    	if(m3.isSelected())
    		res.models.addAll(mod3.generateModels());
    	if(m7.isSelected())
    		res.models.addAll(mod7.generateModels());
    	if(m8.isSelected())
    		res.models.addAll(mod8.generateModels());
    	
    	return res;
    }
    
    private void initPreviousResult(CodeMLResult res) {
		m0.setSelected(false);
		m1a.setSelected(false);
		m2a.setSelected(false);
		m3.setSelected(false);
		m7.setSelected(false);
		m8.setSelected(false);
		
		for(CMLModel m : res.models) {
			if(m.model.equals(mod0.model)) {
				m0.setSelected(true);
				mod0 = m;
			}
			if(m.model.equals(mod1a.model)) {
				m1a.setSelected(true);
				mod1a = m;
			}
			if(m.model.equals(mod2a.model)) {
				m2a.setSelected(true);
				mod2a = m;
			}
			if(m.model.equals(mod3.model)) {
				m3.setSelected(true);
				mod3 = m;
			}
			if(m.model.equals(mod7.model)) {
				m7.setSelected(true);
				mod7 = m;
			}
			if(m.model.equals(mod8.model)) {
				m8.setSelected(true);
				mod8 = m;
			}
		}
	}
        
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jPanel1 = new javax.swing.JPanel();
        m0 = new javax.swing.JCheckBox();
        m1a = new javax.swing.JCheckBox();
        m2a = new javax.swing.JCheckBox();
        m3 = new javax.swing.JCheckBox();
        m7 = new javax.swing.JCheckBox();
        m8 = new javax.swing.JCheckBox();
        m7set = new javax.swing.JButton();
        m3set = new javax.swing.JButton();
        m2aset = new javax.swing.JButton();
        m1aset = new javax.swing.JButton();
        m0set = new javax.swing.JButton();
        m8set = new javax.swing.JButton();

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Site Models"));
        jPanel1.setToolTipText("");
        m0.setText("M0 (One Ratio)");
        m0.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        m0.setMargin(new java.awt.Insets(0, 0, 0, 0));
        m0.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                m0StateChanged(evt);
            }
        });

        m1a.setText("M1a (Nearly Neutral)");
        m1a.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        m1a.setMargin(new java.awt.Insets(0, 0, 0, 0));
        m1a.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                m1aStateChanged(evt);
            }
        });

        m2a.setText("M2a (Positive Selection)");
        m2a.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        m2a.setMargin(new java.awt.Insets(0, 0, 0, 0));
        m2a.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                m2aStateChanged(evt);
            }
        });

        m3.setText("M3 (Discrete, 3 Categories)");
        m3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        m3.setMargin(new java.awt.Insets(0, 0, 0, 0));
        m3.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                m3StateChanged(evt);
            }
        });

        m7.setText("M7 (Beta, 10 Categories)");
        m7.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        m7.setMargin(new java.awt.Insets(0, 0, 0, 0));
        m7.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                m7StateChanged(evt);
            }
        });

        m8.setText("M8 (Beta & w>1, 11 Categories)");
        m8.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        m8.setMargin(new java.awt.Insets(0, 0, 0, 0));
        m8.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                m8StateChanged(evt);
            }
        });

        m7set.setIcon(Icons.SETTINGS);
        m7set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m7setActionPerformed(evt);
            }
        });

        m3set.setIcon(Icons.SETTINGS);
        m3set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m3setActionPerformed(evt);
            }
        });

        m2aset.setIcon(Icons.SETTINGS);
        m2aset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m2asetActionPerformed(evt);
            }
        });

        m1aset.setIcon(Icons.SETTINGS);
        m1aset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m1asetActionPerformed(evt);
            }
        });

        m0set.setIcon(Icons.SETTINGS);
        m0set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m0setActionPerformed(evt);
            }
        });

        m8set.setIcon(Icons.SETTINGS);
        m8set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m8setActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(m8)
                    .addComponent(m7)
                    .addComponent(m3)
                    .addComponent(m2a)
                    .addComponent(m0)
                    .addComponent(m1a))
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(m2aset, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(m8set)
                            .addComponent(m3set))
                        .addComponent(m7set))
                    .addComponent(m0set)
                    .addComponent(m1aset))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(m0)
                    .addComponent(m0set))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(m1a)
                    .addComponent(m1aset))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(m2aset)
                    .addComponent(m2a))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(m3set)
                    .addComponent(m3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(m7set)
                    .addComponent(m7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(m8set)
                    .addComponent(m8))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(12, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void m8StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_m8StateChanged
        checkSelected();
    }//GEN-LAST:event_m8StateChanged

    private void m7StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_m7StateChanged
        checkSelected();
    }//GEN-LAST:event_m7StateChanged

    private void m3StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_m3StateChanged
        checkSelected();
    }//GEN-LAST:event_m3StateChanged

    private void m2aStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_m2aStateChanged
        checkSelected();
    }//GEN-LAST:event_m2aStateChanged

    private void m1aStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_m1aStateChanged
        checkSelected();
    }//GEN-LAST:event_m1aStateChanged

    private void m0StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_m0StateChanged
        checkSelected();
    }//GEN-LAST:event_m0StateChanged

    private void checkSelected() {
    	if(parent.bRun!=null)
    		parent.bRun.setEnabled(m0.isSelected() || m1a.isSelected() || m2a.isSelected() || m3.isSelected() || m7.isSelected() || m8.isSelected());
    }
    
    private void m8setActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m8setActionPerformed
        OmegaValuesDialog dlg = new OmegaValuesDialog(parent, true, mod8);
        dlg.setVisible(true);
        if(dlg.getModel()!=null)
            mod8 = dlg.getModel();
    }//GEN-LAST:event_m8setActionPerformed

    private void m7setActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m7setActionPerformed
        OmegaValuesDialog dlg = new OmegaValuesDialog(parent, true, mod7);
        dlg.setVisible(true);
        if(dlg.getModel()!=null)
            mod7 = dlg.getModel();
    }//GEN-LAST:event_m7setActionPerformed

    private void m3setActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m3setActionPerformed
        OmegaValuesDialog dlg = new OmegaValuesDialog(parent, true, mod3);
        dlg.setVisible(true);
        if(dlg.getModel()!=null)
            mod3 = dlg.getModel();
    }//GEN-LAST:event_m3setActionPerformed

    private void m2asetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m2asetActionPerformed
        OmegaValuesDialog dlg = new OmegaValuesDialog(parent, true, mod2a);
        dlg.setVisible(true);
        if(dlg.getModel()!=null)
            mod2a = dlg.getModel();
    }//GEN-LAST:event_m2asetActionPerformed

    private void m1asetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m1asetActionPerformed
        OmegaValuesDialog dlg = new OmegaValuesDialog(parent, true, mod1a);
        dlg.setVisible(true);
        if(dlg.getModel()!=null)
            mod1a = dlg.getModel();
    }//GEN-LAST:event_m1asetActionPerformed

    private void m0setActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m0setActionPerformed
        OmegaValuesDialog dlg = new OmegaValuesDialog(parent, true, mod0);
        dlg.setVisible(true);
        if(dlg.getModel()!=null)
            mod0 = dlg.getModel();
    }//GEN-LAST:event_m0setActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JCheckBox m0;
    private javax.swing.JButton m0set;
    private javax.swing.JCheckBox m1a;
    private javax.swing.JButton m1aset;
    private javax.swing.JCheckBox m2a;
    private javax.swing.JButton m2aset;
    private javax.swing.JCheckBox m3;
    private javax.swing.JButton m3set;
    private javax.swing.JCheckBox m7;
    private javax.swing.JButton m7set;
    private javax.swing.JCheckBox m8;
    private javax.swing.JButton m8set;
    // End of variables declaration//GEN-END:variables
    
}