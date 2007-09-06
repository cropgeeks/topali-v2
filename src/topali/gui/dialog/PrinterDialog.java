// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;

import javax.print.attribute.*;
import javax.swing.*;

import org.apache.log4j.Logger;

import topali.gui.*;
import doe.MsgBox;

public class PrinterDialog extends JDialog
{
	 Logger log = Logger.getLogger(this.getClass());
	
	private static PrinterJob job = PrinterJob.getPrinterJob();

	public static PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();

	private JLabel label;

	// Components that are to be printed
	private Printable[] toPrint = null;

	public PrinterDialog(Printable[] toPrint)
	{
		super(MsgBox.frm, Text.GuiDiag.getString("PrinterDialog.gui01"), true);

		this.toPrint = toPrint;

		JLabel icon = new JLabel(Icons.PRINT);
		icon.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));

		label = new JLabel(Text.GuiDiag.getString("PrinterDialog.gui02"));
		label.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
		add(label);
		add(icon, BorderLayout.WEST);

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowOpened(WindowEvent e)
			{
				new Printer().start();
			}
		});

		pack();
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setLocationRelativeTo(MsgBox.frm);
		setResizable(false);
	}

	// Display the Java Printer PageSetup dialog
	public static void showPageSetupDialog(WinMain winMain)
	{
		winMain.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		job.pageDialog(aset);
		winMain.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	private void updateLabel()
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				label.setText(Text.GuiDiag.getString("PrinterDialog.gui03"));

				pack();
				setLocationRelativeTo(MsgBox.frm);
			}
		};

		try
		{
			SwingUtilities.invokeAndWait(r);
		} catch (Exception e)
		{
			log.warn(e);
		}
	}

	class Printer extends Thread
	{
		@Override
		public void run()
		{
			if (job.printDialog(aset))
			{
				updateLabel();

				try
				{
					for (Printable p : toPrint)
					{
						job.setPrintable(p);
						job.print(aset);
					}
				} catch (Exception e)
				{
					log.warn("Printing failed.\n",e);
					MsgBox.msg(Text.format(Text.GuiDiag
							.getString("PrinterDialog.err01"), e), MsgBox.ERR);
				}
			}

			setVisible(false);
		}
	}
}
