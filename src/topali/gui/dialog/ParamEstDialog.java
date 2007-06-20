// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.BorderLayout;
import java.awt.event.*;

import javax.swing.*;

import pal.alignment.SimpleAlignment;
import topali.analyses.ParamEstimateThread;
import topali.data.SequenceSet;
import topali.gui.Text;
import doe.MsgBox;

public class ParamEstDialog extends JDialog
{
	private ParamEstimateThread estThread;
	
	public ParamEstDialog(SequenceSet ss, int[] indices)
	{
		super(MsgBox.frm, Text.Analyses.getString("ParamEstDialog.gui01"), true);

		SimpleAlignment alignment = ss.getAlignment(indices, 1, ss.getLength(),
				true);
		
		JPanel p1 = new JPanel(new BorderLayout());
		p1.add(new JLabel(Text.Analyses.getString("ParamEstDialog.gui02")),
				BorderLayout.NORTH);
		p1.add(new JLabel(Text.Analyses.getString("ParamEstDialog.gui03")),
				BorderLayout.SOUTH);
		p1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(p1, BorderLayout.CENTER);

		if(ss.isDNA()) {
			estThread = new ParamEstimateThread(alignment);
		}
		
		addWindowListener(new WindowAdapter()
		{
			public void windowOpened(WindowEvent event)
			{
				Runnable r = new Runnable()
				{
					public void run()
					{
						if(estThread!=null)
							estThread.start();

						while (estThread!=null&&estThread.isAlive())
							try
							{
								Thread.sleep(250);
							} catch (InterruptedException e)
							{
							}

						setVisible(false);
					}
				};

				new Thread(r).start();
				
			}
		});

		pack();
		setResizable(false);
		setLocationRelativeTo(MsgBox.frm);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setVisible(true);
	}

	public double getRatio()
	{
		return estThread.getRatio();
	}

	public double getAlpha()
	{
		return estThread.getAlpha();
	}

	public double getKappa()
	{
		return estThread.getKappa();
	}

	public double getAvgDistance()
	{
		return estThread.getAvgDistance();
	}

	public double[] getFreqs()
	{
		return estThread.getFreqs();
	}
}
