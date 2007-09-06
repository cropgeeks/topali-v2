// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import static topali.mod.Filters.TXT;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.print.*;
import java.io.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import topali.gui.*;
import topali.mod.Filters;
import topali.var.Utils;
import doe.MsgBox;

public class TextPanel extends JPanel implements Printable
{
	
	 Logger log = Logger.getLogger(this.getClass());

//	Toolbar positions
	public static final int NO = 0;

	public static final int TOP = 1;

	public static final int LEFT = 2;

	public static final int BOTTOM = 3;

	public static final int RIGHT = 4;
	
	int toolbarPos;
	
	JTextArea text;
	
	public TextPanel(int toolbarPos) {
		this.setLayout(new BorderLayout());
		this.toolbarPos = toolbarPos;
		
		JToolBar tb = createToolbar();
		switch(toolbarPos) {
		case TOP: this.add(tb, BorderLayout.NORTH); break;
		case LEFT: this.add(tb, BorderLayout.WEST); break;
		case BOTTOM: this.add(tb, BorderLayout.SOUTH); break;
		case RIGHT: this.add(tb, BorderLayout.EAST); break;
		}
		
		text = new JTextArea();
		text.setEditable(false);
		Utils.setTextAreaDefaults(text);
		this.add(new JScrollPane(text), BorderLayout.CENTER);
	}
	
	public void setText(String text) {
		this.text.setText(text);
	}

	private JToolBar createToolbar()
	{
		int pos = (this.toolbarPos == LEFT || this.toolbarPos == RIGHT) ? SwingConstants.VERTICAL
				: SwingConstants.HORIZONTAL;
		JToolBar tb = new JToolBar(pos);

		tb.setFloatable(false);
		tb.setBorderPainted(false);
		tb.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		JButton bExport = new JButton(getExportAction());
		bExport.setIcon(Icons.EXPORT);
		bExport.setToolTipText("Export as text file");
		tb.add(bExport);

		return tb;
	}
	
	private Action getExportAction()
	{
		Action aExport = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser fc = new JFileChooser();
				fc.setDialogTitle("Export");
				fc.setCurrentDirectory(new File(Prefs.gui_dir));
				fc.setSelectedFile(new File("codonw"));

				Filters.setFilters(fc, Prefs.gui_filter_table, TXT);
				fc.setAcceptAllFileFilterUsed(false);

				if (fc.showSaveDialog(MsgBox.frm) == JFileChooser.APPROVE_OPTION)
				{
					File file = Filters.getSelectedFileForSaving(fc);

					// Confirm overwrite
					if (file.exists())
					{
						String msg = file
								+ " already exists.\nDo you want to replace it?";
						int response = MsgBox.yesnocan(msg, 1);

						if (response == JOptionPane.CANCEL_OPTION
								|| response == JOptionPane.CLOSED_OPTION)
							return;
					}

					Prefs.gui_dir = "" + fc.getCurrentDirectory();
					Prefs.gui_filter_table = ((Filters) fc.getFileFilter()).getExtInt();
					
					try
					{
							BufferedWriter out = new BufferedWriter(
									new FileWriter(file));
							out.write(text.getText());
							out.flush();
							out.close();

						MsgBox.msg("Data successfully saved to " + file,
								MsgBox.INF);
					} catch (IOException e1)
					{
						MsgBox.msg(
								"There was an unexpected error while saving data:\n "
										+ e, MsgBox.ERR);
						log.warn(e);
					}
				}
			}
		};
		return aExport;
	}
	
	/**
	 * @see java.awt.print.Printable
	 */
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
			throws PrinterException
	{
		int response = NO_SUCH_PAGE;

	    Graphics2D g2 = (Graphics2D) graphics;

	    //  for faster printing, turn off double buffering
	    //disableDoubleBuffering(text);

	    Dimension d = text.getSize(); //get size of document
	    double panelWidth = d.width; //width in pixels
	    double panelHeight = d.height; //height in pixels

	    double pageHeight = pageFormat.getImageableHeight(); //height of printer page
	    double pageWidth = pageFormat.getImageableWidth(); //width of printer page

	    double scale = pageWidth / panelWidth;
	    int totalNumPages = (int) Math.ceil(scale * panelHeight / pageHeight);
	    System.out.println("np="+totalNumPages);
	    
	    //  make sure not print empty pages
	    if (pageIndex >= totalNumPages) {
	      response = NO_SUCH_PAGE;
	    }
	    else {

	      //  shift Graphic to line up with beginning of print-imageable region
	      g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

	      //  shift Graphic to line up with beginning of next page to print
	      g2.translate(0f, -pageIndex * pageHeight);

	      //  scale the page so the width fits...
	      g2.scale(scale, scale);

	      text.paint(g2); //repaint the page for printing

	      //enableDoubleBuffering(text);
	      response = Printable.PAGE_EXISTS;
	    }
	    return response;
	}
	
}
