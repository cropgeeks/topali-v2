// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import static topali.mod.Filters.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.print.Printable;
import java.io.*;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.JTable.PrintMode;
import org.apache.log4j.Logger;
import scri.commons.gui.MsgBox;
import topali.data.Prefs;
import topali.gui.*;
import topali.mod.Filters;

/**
 * Panel for displaying a table
 */
public class TablePanel extends DataVisPanel {
	Logger log = Logger.getLogger(this.getClass());

	int toolbarPos;

	JToolBar toolbar;

	CustomTable table;
	JScrollPane scroll;
	Vector<Vector<Object>> rowData;
	Vector<String> columnNames;

	/**
	 * Panel for displaying a table
	 * @param rowData
	 * @param columnNames
	 * @param toolbarPos Position where the toolbar should be placed
	 */
	public TablePanel(Vector<Vector<Object>> rowData, Vector<String> columnNames, String name) {
		super(name);
		this.rowData = rowData;
		this.columnNames = columnNames;

		CustomTable table = new CustomTable(rowData, columnNames);
		table.setPreferredScrollableViewportSize(table.getPreferredSize());
		this.table = table;

		this.setLayout(new BorderLayout());
		this.scroll = new JScrollPane(table);
		this.add(scroll, BorderLayout.CENTER);
	}

	/**
	 * Sets the data for the table
	 * @param rowData
	 */
	public void setData(Vector<Vector<Object>> rowData) {
		for (int i = 0; i < rowData.size(); i++) {
			Vector<Object> row = (Vector<Object>) rowData.get(i);
			for (int j = 0; j < row.size(); j++)
				table.setValueAt(row.get(j), i, j);
		}
		table.adjustRowHeight();
		table.repaint();
	}

	/**
	 * Just to have public access to the table (e. g. for adding selection listeneres)
	 * @return
	 */
	public JTable accessTable() {
		return table;
	}

	private Action getExportAction() {
		Action aExport = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser();
				fc.setDialogTitle("Export Graphs");
				fc.setCurrentDirectory(new File(Prefs.gui_dir));
				fc.setSelectedFile(new File("table"));

				Filters.setFilters(fc, Prefs.gui_filter_table, CSV, PNG);
				fc.setAcceptAllFileFilterUsed(false);

				if (fc.showSaveDialog(TOPALi.winMain) == JFileChooser.APPROVE_OPTION) {
					File file = Filters.getSelectedFileForSaving(fc);

					// Confirm overwrite
					if (file.exists()) {
						String msg = file + " already exists.\nDo you want to replace it?";
						int response = MsgBox.yesnocan(msg, 1);

						if (response == JOptionPane.CANCEL_OPTION || response == JOptionPane.CLOSED_OPTION)
							return;
					}

					Prefs.gui_dir = "" + fc.getCurrentDirectory();
					Prefs.gui_filter_table = ((Filters) fc.getFileFilter()).getExtInt();

					try {
						if (Prefs.gui_filter_table == CSV) {
							BufferedWriter out = new BufferedWriter(new FileWriter(file));
							out.write(table.getCSV());
							out.flush();
							out.close();
						} else if (Prefs.gui_filter_table == PNG) {
							ImageIO.write(table.getBufferedImage(), "png", file);
							updateUI();
						}

						MsgBox.msg("Data successfully saved to " + file, MsgBox.INF);
					}
					catch (IOException e1) {
						MsgBox.msg("There was an unexpected error while saving data:\n " + e, MsgBox.ERR);
						log.warn(e);
					}
				}
			}
		};
		return aExport;
	}

	
	public Printable getPrintable() {
		return table.getPrintable(PrintMode.FIT_WIDTH, null, null);
	}

	
	public Object getExportable(int format) {
		switch (format) {
			case FORMAT_TXT:
				return table.toString();
			case FORMAT_CSV:
				return table.getCSV();
			case FORMAT_IMAGE:
				return table.getBufferedImage();
			case FORMAT_SVG:
				return table;
			default:
				return null;
		}
	}

}
