// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import static topali.mod.Filters.CSV;
import static topali.mod.Filters.PNG;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.print.*;
import java.io.*;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.*;

import topali.gui.*;
import topali.mod.Filters;
import doe.MsgBox;

/**
 * Panel for displaying a table
 */
public class TablePanel extends JPanel implements Printable
{
	public static final int NO = 0;

	public static final int TOP = 1;

	public static final int LEFT = 2;

	public static final int BOTTOM = 3;

	public static final int RIGHT = 4;

	int toolbarPos;

	JToolBar toolbar;

	CustomTable table;
	JScrollPane scroll;
	Vector rowData, columnNames;

	public TablePanel(Vector rowData, Vector columnNames, String title,
			int toolbarPos)
	{
		this.rowData = rowData;
		this.columnNames = columnNames;
		this.toolbarPos = toolbarPos;

		CustomTable table = new CustomTable(rowData, columnNames);
		table.setPreferredScrollableViewportSize(table.getPreferredSize());
		this.table = table;
		this.toolbar = createToolbar();

		this.setLayout(new BorderLayout());
		this.scroll = new JScrollPane(table);
		this.add(scroll, BorderLayout.CENTER);
		switch (toolbarPos)
		{
		case TOP:
			this.add(this.toolbar, BorderLayout.NORTH);
			break;
		case RIGHT:
			this.add(this.toolbar, BorderLayout.EAST);
			break;
		case BOTTOM:
			this.add(this.toolbar, BorderLayout.SOUTH);
			break;
		case LEFT:
			this.add(this.toolbar, BorderLayout.WEST);
			break;
		}

		if (title != null)
			setBorder(BorderFactory.createTitledBorder(title));
	}

	public void setData(Vector rowData)
	{
		for (int i = 0; i < rowData.size(); i++)
		{
			Vector row = (Vector) rowData.get(i);
			for (int j = 0; j < row.size(); j++)
				table.setValueAt(row.get(j), i, j);
		}
		table.adjustRowHeight();
		table.repaint();
	}

	public JTable accessTable()
	{
		return table;
	}

	private JToolBar createToolbar()
	{
		int pos = (this.toolbarPos == LEFT || this.toolbarPos == RIGHT) ? JToolBar.VERTICAL
				: JToolBar.HORIZONTAL;
		JToolBar tb = new JToolBar(pos);

		tb.setFloatable(false);
		tb.setBorderPainted(false);
		tb.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		JButton bExport = new JButton(getExportAction());
		bExport.setIcon(Icons.EXPORT);
		bExport.setToolTipText("Export as CSV");
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
				fc.setDialogTitle("Export Graphs");
				fc.setCurrentDirectory(new File(Prefs.gui_dir));
				fc.setSelectedFile(new File("table"));

				Filters.setFilters(fc, Prefs.gui_filter_table, CSV, PNG);
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
						if (Prefs.gui_filter_table == CSV)
						{
							BufferedWriter out = new BufferedWriter(
									new FileWriter(file));
							out.write(table.getCSV());
							out.flush();
							out.close();
						}
						else if(Prefs.gui_filter_table == PNG) {
							ImageIO.write(table.getPNGImage(), "png", file);
							updateUI();
						}

						MsgBox.msg("Data successfully saved to " + file,
								MsgBox.INF);
					} catch (IOException e1)
					{
						MsgBox.msg(
								"There was an unexpected error while saving data:\n "
										+ e, MsgBox.ERR);
						TOPALi.log.warning(e.toString());
					}
				}
			}
		};
		return aExport;
	}

	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
			throws PrinterException
	{
		return table.print(graphics, pageFormat, pageIndex);
	}

}
