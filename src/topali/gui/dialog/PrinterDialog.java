// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.*;

import topali.gui.*;
import doe.MsgBox;

public class PrinterDialog extends JDialog
{
	private static PrinterJob job = PrinterJob.getPrinterJob();

	private static PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();

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
			public void windowOpened(WindowEvent e)
			{
				new Printer().start();
			}
		});

		pack();
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setLocationRelativeTo(MsgBox.frm);
		setResizable(false);
		setVisible(true);
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
		}
	}

	class Printer extends Thread
	{
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
					MsgBox.msg(Text.format(Text.GuiDiag
							.getString("PrinterDialog.err01"), e), MsgBox.ERR);
				}
			}

			setVisible(false);
		}
	}
}
