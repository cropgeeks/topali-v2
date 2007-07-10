package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import javax.swing.*;
import javax.print.attribute.*;

import topali.gui.*;

import doe.*;

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
		
		addWindowListener(new WindowAdapter() {
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
		Runnable r = new Runnable() {
			public void run()
			{
				label.setText(Text.GuiDiag.getString("PrinterDialog.gui03"));
				
				pack();
				setLocationRelativeTo(MsgBox.frm);
			}
		};
		
		try { SwingUtilities.invokeAndWait(r); }
		catch (Exception e) {}
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
					for (Printable p: toPrint)
					{
						job.setPrintable(p);
						job.print(aset);
					}
				}
				catch (Exception e)
				{
					MsgBox.msg(Text.format(Text.GuiDiag.getString(
						"PrinterDialog.err01"), e), MsgBox.ERR);
				}
			}
			
			setVisible(false);
		}
	}
}

