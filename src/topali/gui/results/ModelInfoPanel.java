/*
 * ModelInfoPanel.java
 *
 * Created on 16 October 2007, 08:55
 */

package topali.gui.results;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;

import org.apache.log4j.Logger;

import topali.data.AlignmentData;
import topali.data.models.*;
import topali.gui.*;
import topali.var.*;

/**
 *
 * @author  dlindn
 */
public class ModelInfoPanel extends javax.swing.JPanel implements MouseListener {
    
	MTResultPanel mtpanel; 
	AlignmentData data;
	Model model;
	Logger log = Logger.getLogger(this.getClass());
	
    /** Creates new form ModelInfoPanel */
    public ModelInfoPanel(AlignmentData data, MTResultPanel mtpanel) {
    	this.data = data;
    	this.mtpanel = mtpanel;
        initComponents();
        setModel(null);
        modelDiagram.setToolTipText("Substitution model diagram (double click to expand)");
        modelDiagram.addMouseListener(this);
    }
    
    public void setModel(Model mod) {
    	if(mod!=null && mod==this.model)
    		return;
    	
    	this.model = mod;
    	
    	if(mod==null) {
    		setModName(null);
    		setAliases(null);
    		setlnl(Double.NaN);
    		setAIC1(Double.NaN);
    		setAIC2(Double.NaN);
    		setBIC(Double.NaN);
    		setGamma(-1, Double.NaN);
    		setInv(Double.NaN);
    		setSubRates(null);
    		setSubRateGroups(null);
    		setBaseFreqs(null);
    		setBaseFreqGroups(null);
    	}
    	else {
    		if(mod instanceof DNAModel) {
    			DNAModel m = (DNAModel)mod;
    			setModName(m.getName());
        		setAliases(m.getAliases());
        		
        		int df = m.getFreeParameters();
        		if(m.isGamma())
        			df++;
        		if(m.isInv())
        			df++;
        		double lnl = m.getLnl();
        		double aic1 = MathUtils.calcAIC1(m.getLnl(), df);
        		double aic2 = MathUtils.calcAIC2(m.getLnl(),df, data.getSequenceSet().getLength());
        		double bic = MathUtils.calcBIC(m.getLnl(), df, data.getSequenceSet().getLength());
        		setlnl(lnl);
        		setAIC1(aic1);
        		setAIC2(aic2);
        		setBIC(bic);
        		
        		if(mod.isGamma()) 
					setGamma(m.getGammaCat(), m.getAlpha());
				else
					setGamma(-1, Double.NaN);
				if(mod.isInv()) 
					setInv(m.getInvProp());
				else 
					setInv(Double.NaN);
				
        		modelDiagram.setModel(m);
        		rateHetDiagram.setModel(m);
        		
        		setSubRates(m.getSubRates());
        		setSubRateGroups(m.getSubRateGroups());
        		setBaseFreqs(m.getBaseFreqs());
        		setBaseFreqGroups(m.getBaseFreqGroups());
    		}
    		else if(mod instanceof ProteinModel) {
    			ProteinModel m = (ProteinModel)mod;
    			setModName(m.getName());
        		setAliases(m.getAliases());
        		setlnl(m.getLnl());
        		setAIC1(MathUtils.calcAIC1(m.getLnl(), m.getFreeParameters()));
        		setAIC2(MathUtils.calcAIC2(m.getLnl(), m.getFreeParameters(), data.getSequenceSet().getLength()));
        		setBIC(MathUtils.calcBIC(m.getLnl(), m.getFreeParameters(), data.getSequenceSet().getLength()));
        		
        		if(mod.isGamma())
					setGamma(m.getGammaCat(), m.getAlpha());
				else 
					setGamma(-1, Double.NaN);
				if(mod.isInv()) 
					setInv(m.getInvProp());
				else 
					setInv(Double.NaN);
				
        		modelDiagram.setModel(m);
        		
        		//Color c = Utils.calcColor(m.getName());
        		Color c = Color.WHITE;
        		modelDiagram.setBackground(c);
        		jPanel3.setBackground(c);
        		
        		rateHetDiagram.setModel(m);
        		
        		setSubRates(null);
        		setSubRateGroups(null);
        		setBaseFreqs(null);
        		setBaseFreqGroups(null);
    		}
    	}
    	repaint();
    	defaultButton.setEnabled(mod!=null && !data.getSequenceSet().getParams().getModel().matches(mod));
    }
    
    private void setModName(String name) {
    	if(name==null)
    		this.modelName.setText("--");
    	else {
    		this.modelName.setText(name);
    	}
    }
    
    private void setAliases(List<String> aliases) {
    	if(aliases==null)
    		this.aliases.setText("");
    	else {
	    	String tmp = "(";
	    	for(int i=0; i<aliases.size()-1; i++) {
	    		tmp += aliases.get(i)+"; ";
	    	}
	    	tmp+= aliases.get(aliases.size()-1);
	    	tmp+= ")";
	    	this.aliases.setText(tmp);
    	}
    }
    
    private void setlnl(double lnl) {
    	if(new Double(lnl).isNaN())
    		this.lnl.setText("--");
    	else
    		this.lnl.setText(Prefs.d2.format(lnl)+";   ");
    }
    
    private void setAIC1(double aic1) {
    	if(new Double(aic1).isNaN())
    		this.aic1.setText("--");
    	else
    		this.aic1.setText(Prefs.d2.format(aic1)+";   ");
    }
    
    private void setAIC2(double aic2) {
    	if(new Double(aic2).isNaN())
    		this.aic2.setText("--");
    	else
    		this.aic2.setText(Prefs.d2.format(aic2)+";   ");
    }
    
    private void setBIC(double bic) {
    	if(new Double(bic).isNaN())
    		this.bic.setText("--");
    	else
    		this.bic.setText(Prefs.d2.format(bic)+";");
    }
    
    private void setGamma(int cat, double a) {
    	if(cat<0 || new Double(a).isNaN())
    		this.gamma.setText("--");
    	else
    		this.gamma.setText("\u03b1="+Prefs.d2.format(a)+" (4 Cat.)");
    }
    
    private void setInv(double inv) {
    	if(new Double(inv).isNaN())
    		this.inv.setText("--");
    	else
    		this.inv.setText(""+Prefs.d2.format(inv));
    }
    
    private void setSubRates(double... rates) {
    	if(rates==null) {
    		this.subRates.setText("--");
    	}
    	else {
    		String tmp = Prefs.d2.format(rates[0])+" | "+Prefs.d2.format(rates[1])+" | "+Prefs.d2.format(rates[2])+" | "+Prefs.d2.format(rates[3])+" | "+Prefs.d2.format(rates[4])+" | "+Prefs.d2.format(rates[5]);
    		this.subRates.setText(tmp);
    	}
    }
    
    private void setSubRateGroups(char... groups) {
    	if(groups==null)
    		this.subRateGroups.setText("--");
    	else {
    		String tmp = "("+new String(groups)+")";
    		this.subRateGroups.setText(tmp);
    	}
    }
    
    private void setBaseFreqs(double... freqs) {
    	if(freqs==null) {
    		this.baseFreq.setText("--");
    	}
    	else {
    		String tmp = Prefs.d2.format(freqs[0])+" | "+Prefs.d2.format(freqs[1])+" | "+Prefs.d2.format(freqs[2])+" | "+Prefs.d2.format(freqs[3]);
    		this.baseFreq.setText(tmp);
    	}
    }
  
    private void setBaseFreqGroups(char... groups) {
    	if(groups==null)
    		this.baseFreqGroups.setText("--");
    	else {
    		String tmp = "("+new String(groups)+")";
    		this.baseFreqGroups.setText(tmp);
    	}
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        modelName = new javax.swing.JLabel();
        aliases = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        lnl = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        aic1 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        aic2 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        bic = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
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
        jLabel9 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        
        if(data.getSequenceSet().isDNA())
        	this.modelDiagram = new DNAModelDiagram();
        else
        	this.modelDiagram = new ProteinModelDiagram();
        
        rateHetDiagram = new topali.gui.results.RateHetDiagram();
        defaultButton = new javax.swing.JButton();

        modelName.setFont(new java.awt.Font("Tahoma", 1, 12));
        modelName.setText("GTR");

        aliases.setText("(General Time Reversible)");

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLabel1.setText("Model Test Scores:");

        jLabel2.setText("\u2113 = ");

        lnl.setText("123;  ");

        jLabel4.setText("AIC\u2081 = ");

        aic1.setText("123;   ");

        jLabel6.setText("AIC\u2082 = ");

        aic2.setText("123;   ");

        jLabel8.setText("BIC = ");

        bic.setText("123;");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lnl)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(aic1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(aic2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(bic)))
                .addContainerGap(398, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(lnl)
                    .addComponent(jLabel4)
                    .addComponent(aic1)
                    .addComponent(jLabel6)
                    .addComponent(aic2)
                    .addComponent(jLabel8)
                    .addComponent(bic))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLabel10.setText("Parameters:");

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

        jLabel13.setText("Rate Heterogeneity (\u0393):  ");

        jLabel14.setText("Proportion of Invariant Sites (pInv):  ");

        gamma.setText("--");

        inv.setText("--");

        jLabel9.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel9.setForeground(new java.awt.Color(140, 0, 0));
        jLabel9.setText("pINV");

        jLabel11.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel11.setForeground(new java.awt.Color(0, 0, 160));
        jLabel11.setText("\u0393");

        jLabel7.setText(",");

        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        javax.swing.GroupLayout modelDiagramLayout = new javax.swing.GroupLayout(modelDiagram);
        modelDiagram.setLayout(modelDiagramLayout);
        modelDiagramLayout.setHorizontalGroup(
            modelDiagramLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        modelDiagramLayout.setVerticalGroup(
            modelDiagramLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 120, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(modelDiagram, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(modelDiagram, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout rateHetDiagramLayout = new javax.swing.GroupLayout(rateHetDiagram);
        rateHetDiagram.setLayout(rateHetDiagramLayout);
        rateHetDiagramLayout.setHorizontalGroup(
            rateHetDiagramLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 200, Short.MAX_VALUE)
        );
        rateHetDiagramLayout.setVerticalGroup(
            rateHetDiagramLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 124, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel13)
                    .addComponent(jLabel10)
                    .addComponent(jLabel3)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel14)
                            .addComponent(jLabel5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(baseFreq)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(baseFreqGroups))
                            .addComponent(inv)
                            .addComponent(gamma)
                            .addComponent(subRates))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(subRateGroups)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 32, Short.MAX_VALUE)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel9))
                    .addComponent(rateHetDiagram, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel13)
                            .addComponent(gamma))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel14)
                            .addComponent(inv))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(subRates)
                            .addComponent(subRateGroups))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(baseFreq)
                            .addComponent(baseFreqGroups)))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(jLabel7)
                            .addComponent(jLabel11))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(rateHetDiagram, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        defaultButton.setText("Select");
        defaultButton.setToolTipText("Use this model for further analysis");
        defaultButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                defaultButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(modelName)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 692, Short.MAX_VALUE)
                        .addComponent(defaultButton))
                    .addComponent(aliases)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(defaultButton)
                    .addComponent(modelName))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(aliases)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void defaultButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_defaultButtonActionPerformed
    	log.info("Set default model to:\n"+model);
        data.getSequenceSet().getParams().setModel(model);
        defaultButton.setEnabled(false);
        mtpanel.modelSetTo(model);
    }//GEN-LAST:event_defaultButtonActionPerformed
    
    
    @Override
	public void mouseClicked(MouseEvent e)
	{
		if(e.getClickCount()>1) {
			JFrame frame = new JFrame(model.getName());
			JPanel p = new JPanel(new BorderLayout());
			p.setBackground(Color.WHITE);
			p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			ModelDiagram dia = data.getSequenceSet().isDNA() ?
					new DNAModelDiagram((DNAModel)model) :
						new ProteinModelDiagram((ProteinModel)model);
			p.add(dia, BorderLayout.CENTER);
			dia.setBackground(Color.WHITE);
			frame.getContentPane().add(p);
			frame.pack();
			frame.setSize(400,480);
			frame.setLocationRelativeTo(TOPALi.winMain);
			frame.setVisible(true);
		}
		
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
		setCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		
	}


	// Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel aic1;
    private javax.swing.JLabel aic2;
    private javax.swing.JLabel aliases;
    private javax.swing.JLabel baseFreq;
    private javax.swing.JLabel baseFreqGroups;
    private javax.swing.JLabel bic;
    private javax.swing.JButton defaultButton;
    private javax.swing.JLabel gamma;
    private javax.swing.JLabel inv;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
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
    private javax.swing.JLabel lnl;
    private topali.gui.results.ModelDiagram modelDiagram;
    private javax.swing.JLabel modelName;
    private topali.gui.results.RateHetDiagram rateHetDiagram;
    private javax.swing.JLabel subRateGroups;
    private javax.swing.JLabel subRates;
    // End of variables declaration//GEN-END:variables
    
}
