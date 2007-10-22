// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs.mt;

import java.awt.BorderLayout;
import java.awt.event.*;

import javax.swing.*;

import topali.var.Utils;

public class MTDialog extends JDialog implements ActionListener
{

	private JButton bRun = new JButton(), bCancel = new JButton(), bDefault = new JButton(), bHelp = new JButton();
	
	public MTDialog() {
		init();
	}
	
	private void init() {
		this.setLayout(new BorderLayout());
		
		MTDialogPanel panel = new MTDialogPanel();
		add(panel, BorderLayout.CENTER);
		
		JPanel bp = Utils.getButtonPanel(bRun, bCancel, bDefault, bHelp, this, null);
		add(bp, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(bRun);
		Utils.addCloseHandler(this, bCancel);
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		
	}

	public static void main(String[] args) {
		MTDialog dlg = new MTDialog();
		dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dlg.pack();
		dlg.setVisible(true);
	}
}
