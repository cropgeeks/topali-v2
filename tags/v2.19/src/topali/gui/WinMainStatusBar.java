// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class WinMainStatusBar extends JPanel
{
	public static final byte OFF = 1;

	public static final byte GRE = 2;

	public static final byte BLU = 3;

	public static final byte RED = 4;

	// public static ImageIcon FLASH_RED, FLASH_BLU;

	private static JLabel label = new JLabel(" ");

	private static JLabel jobLabel = new JLabel(" ");

	private static JLabel jobIconLabel = new JLabel();

	public static boolean resetIcon = true;

	public static byte currentIcon = OFF;

	WinMainStatusBar(final WinMain winMain)
	{
		jobLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		jobLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				winMain.menuAnlsShowJobs();
			}
		});

		// try
		// {
		// FLASH_RED = new
		// ImageIcon(getClass().getResource("/res/icons/flash_red.gif"));
		// FLASH_BLU = new
		// ImageIcon(getClass().getResource("/res/icons/flash_blu.gif"));
		// }
		// catch (Exception e) { System.out.println(e); }

		jobIconLabel = new JLabel();
		setStatusIcon(OFF);

		JPanel jobPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		jobPanel.add(jobLabel);
		jobPanel.add(jobIconLabel);

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(1, 2, 2, 2));

		add(label, BorderLayout.WEST);
		add(jobPanel, BorderLayout.EAST);

		setVisible(Prefs.gui_statusbar_visible);
	}

	public static void setText(String text)
	{
		if (text != null)
			label.setText(" " + text);
		else
			label.setText(" ");
	}

	public static void setJobText(String text, boolean red)
	{
		if (red)
			jobLabel.setForeground(Color.red);
		else
			jobLabel.setForeground(Color.black);

		jobLabel.setText(text + " ");
	}

	public static void setStatusIcon(byte icon)
	{
		if (icon <= currentIcon && resetIcon == false)
			return;

		switch (icon)
		{
		case OFF:
			jobIconLabel.setIcon(Icons.STATUS_OFF);
			jobIconLabel.setToolTipText("No jobs running");
			break;
		case RED:
			jobIconLabel.setIcon(Icons.STATUS_RED);
			jobIconLabel
					.setToolTipText("One or more jobs are in an error state");
			break;
		case BLU:
			jobIconLabel.setIcon(Icons.STATUS_BLU);
			jobIconLabel
					.setToolTipText("One or more jobs have intermittent problems");
			break;
		case GRE:
			jobIconLabel.setIcon(Icons.STATUS_GRE);
			jobIconLabel.setToolTipText("All jobs are running");
			break;
		}

		currentIcon = icon;
		resetIcon = false;
	}
}