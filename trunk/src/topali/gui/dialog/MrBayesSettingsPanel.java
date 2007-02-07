// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import doe.*;

class MrBayesSettingsPanel extends JPanel
{
	private JComboBox cModel, cVariation;
    
    MrBayesSettingsPanel()
    {
		createControls();
    }

	private void createControls()
	{
		cModel = new JComboBox();		
		cModel.addItem("F81 (1 parameter)");
		cModel.addItem("HKY (2 parameter)");
		cModel.addItem("GTR (6 parameter)");
		JLabel lModel = new JLabel("Evolutionary model:");
		lModel.setLabelFor(cModel);
		lModel.setDisplayedMnemonic(KeyEvent.VK_E);
		
		cVariation = new JComboBox();
		cVariation.addItem("Gamma");
		cVariation.addItem("Invgamma");
		JLabel lVariation = new JLabel("Rate variation:");
		lVariation.setLabelFor(cVariation);
		lVariation.setDisplayedMnemonic(KeyEvent.VK_R);
		
		
		DoeLayout layout = new DoeLayout();
		
		JLabel label = new JLabel("These settings only apply if creating trees using the MrBayes method");
		
		layout.add(label, 0, 0, 1, 2, new Insets(5, 5, 10, 5));
		
		layout.add(lModel, 0, 1, 0, 1, new Insets(0, 5, 5, 0));
		layout.add(cModel, 1, 1, 1, 1, new Insets(0, 5, 5, 5));
		
		layout.add(lVariation, 0, 2, 0, 1, new Insets(0, 5, 5, 0));
		layout.add(cVariation, 1, 2, 1, 1, new Insets(0, 5, 5, 5));
		
		setLayout(new BorderLayout());
		layout.getPanel().setBorder(BorderFactory.createTitledBorder("Additional settings for MrBayes (NOT FUNCTIONAL YET):"));
		add(layout.getPanel(), BorderLayout.NORTH);
	}
}
