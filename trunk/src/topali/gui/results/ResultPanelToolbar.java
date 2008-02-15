// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import static topali.mod.Filters.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.apache.log4j.Logger;
import scri.commons.gui.MsgBox;
import topali.data.*;
import topali.gui.*;
import topali.gui.dialog.*;
import topali.i18n.Text;
import topali.mod.Filters;

public class ResultPanelToolbar extends JToolBar {
	Logger log = Logger.getLogger(this.getClass());

	// Always enabled:
	JButton bInfo, bReselect, bExport;
	Action aInfo, aReselect, aExport;
	// Just enabled if ResultPanel contains a graph
	JButton bThres, bAddPart, bAutoPart, bToolTips;
	Action aThres, aAddPart, aAutoPart, aToolTips;

	final ResultPanel resPanel;
	final AlignmentData data;
	final AlignmentResult result;

	public ResultPanelToolbar(ResultPanel resPanel, AlignmentData data, AlignmentResult result) {
		this.resPanel = resPanel;
		this.data = data;
		this.result = result;

		addStandardActions();
		add(new JToolBar.Separator());

	}

	public void enableButtons(boolean bInfo, boolean bReselect, boolean bThres, boolean bAddPart, boolean bAutoPart, boolean bToolTips) {
		this.bInfo.setEnabled(bInfo);
		this.bReselect.setEnabled(bReselect);
		this.bThres.setEnabled(bThres);
		this.bAddPart.setEnabled(bAddPart);
		this.bAutoPart.setEnabled(bAutoPart);
		this.bToolTips.setEnabled(bToolTips);
	}

	public void addStandardActions() {
		aInfo = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				AnalysisInfoDialog dialog = new AnalysisInfoDialog(result);
				dialog.setText(resPanel.getAnalysisInfo());
				dialog.setVisible(true);
			}
		};

		aReselect = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				String msg = "This will reselect the sequences used at the time of this " + "analysis in the main alignment view window. Continue?";

				if (MsgBox.yesno(msg, 0) == JOptionPane.YES_OPTION)
					TOPALi.winMain.menuAnlsReselectSequences(result.selectedSeqs);
			}
		};

		aExport = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser();
				fc.setDialogTitle(Text.get("Export_Data"));
				fc.setCurrentDirectory(new File(Prefs.gui_dir));
				fc.setSelectedFile(new File(result.guiName.replaceAll("\\s+", "_")));
				Filters.setFilters(fc, Prefs.gui_filter_table, CSV, TXT, PNG);
				fc.setAcceptAllFileFilterUsed(false);
				if (fc.showSaveDialog(TOPALi.winMain) == JFileChooser.APPROVE_OPTION) {
					Prefs.gui_dir = "" + fc.getCurrentDirectory();
					Prefs.gui_filter_table = ((Filters) fc.getFileFilter()).getExtInt();

					String basefilename = fc.getSelectedFile().getName();
					if (basefilename.indexOf('.') != -1) {
						basefilename = basefilename.substring(0, basefilename.lastIndexOf('.'));
					}

					try {

						StringBuffer filenames = new StringBuffer();
						int ext = Prefs.gui_filter_table;
						for (int i = 0; i < resPanel.getDataPanels().size(); i++) {
							DataVisPanel p = resPanel.getDataPanels().get(i);

							if (ext == TXT) {
								String filename = (p.getFriendlyName() != null) ? basefilename + "_" + p.getFriendlyName() : basefilename;
								File file = new File(Prefs.gui_dir, filename + ".txt");
								Object exportable = p.getExportable(DataVisPanel.FORMAT_TXT);
								if (exportable == null)
									continue;

								if (file.exists()) {
									if (MsgBox.yesno(Text.get("Project.msg01", file.getPath()), 1) != JOptionPane.YES_OPTION)
										continue;
								}

								BufferedWriter out = new BufferedWriter(new FileWriter(file));
								out.write((String) exportable);
								out.flush();
								out.close();
								filenames.append(file.getPath() + "\n");
							} else if (ext == CSV) {
								String filename = (p.getFriendlyName() != null) ? basefilename + "_" + p.getFriendlyName() : basefilename;
								File file = new File(Prefs.gui_dir, filename + ".csv");
								Object exportable = p.getExportable(DataVisPanel.FORMAT_CSV);
								if (exportable == null) {
									//fallback to txt
									i--;
									ext = TXT;
									continue;
								}

								if (file.exists()) {
									if (MsgBox.yesno(Text.get("Project.msg01", file.getPath()), 1) != JOptionPane.YES_OPTION)
										continue;
								}

								BufferedWriter out = new BufferedWriter(new FileWriter(file));
								out.write((String) exportable);
								out.flush();
								out.close();
								filenames.append(file.getPath() + "\n");
							} else if (ext == PNG) {
								String filename = (p.getFriendlyName() != null) ? basefilename + "_" + p.getFriendlyName() : basefilename;
								File file = new File(Prefs.gui_dir, filename + ".png");
								Object exportable = p.getExportable(DataVisPanel.FORMAT_IMAGE);
								if (exportable == null) {
									//fallback to txt
									i--;
									ext = TXT;
									continue;
								}

								if (file.exists()) {
									if (MsgBox.yesno(Text.get("Project.msg01", file.getPath()), 1) != JOptionPane.YES_OPTION)
										continue;
								}

								ImageIO.write((BufferedImage) exportable, "png", file);
								filenames.append(file.getPath() + "\n");
							}
							
//							else if (ext == SVG) {
//								String filename = (p.getFriendlyName() != null) ? basefilename + "_" + p.getFriendlyName() : basefilename;
//								File file = new File(Prefs.gui_dir, filename + ".svg");
//								Object exportable = p.getExportable(DataVisPanel.FORMAT_SVG);
//								if (exportable == null) {
//									//fallback to txt
//									i--;
//									ext = TXT;
//									continue;
//								}
//
//								if (file.exists()) {
//									if (MsgBox.yesno(Text.get("Project.msg01", file.getPath()), 1) != JOptionPane.YES_OPTION)
//										continue;
//								}
//
//								JComponent comp = (JComponent)exportable;
//								
//								// Get a DOMImplementation.
//						        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
//
//						        // Create an instance of org.w3c.dom.Document.
//						        String svgNS = "http://www.w3.org/2000/svg";
//						        Document document = domImpl.createDocument(svgNS, "svg", null);
//						        
//						        // Create an instance of the SVG Generator.
//						        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
//						        
//						        comp.paintComponents(svgGenerator);
//						        
//						        Writer out = new FileWriter(file);
//						        svgGenerator.stream(out, true);
//						        out.flush();
//						        out.close();
//						        
//								filenames.append(file.getPath() + "\n");
//							}

							ext = Prefs.gui_filter_table;
						}

						if (filenames.length() > 0)
							MsgBox.msg("Data successfully saved to: \n" + filenames.toString(), MsgBox.INF);

					}
					catch (Exception e1) {
						log.warn("Data export failed.", e1);
						MsgBox.msg("There was an unexpected error while saving data:\n " + e1, MsgBox.ERR);
					}
				}

			}
		};

		bInfo = (JButton) WinMainToolBar.getButton(false, null, "dss06", Icons.ANALYSIS_INFO, aInfo);
		bReselect = (JButton) WinMainToolBar.getButton(false, null, "dss05", Icons.RESELECT, aReselect);
		bExport = (JButton) WinMainToolBar.getButton(false, null, "export", Icons.EXPORT, aExport);

		add(bInfo);
		add(bExport);
		add(bReselect);
	}

	public void addGraphActions() {
		aThres = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				ThresholdDialog diag = new ThresholdDialog(resPanel, result.threshold);
				diag.setVisible(true);
			}
		};

		aAddPart = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				WinMain.rDialog.addCurrentRegion(PartitionAnnotations.class);
				// WinMainMenuBar.aFileSave.setEnabled(true);
				// WinMainMenuBar.aVamCommit.setEnabled(true);
				ProjectState.setDataChanged();
			}
		};

		aAutoPart = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				new AutoPartitionDialog(null, data, result);
			}
		};

		aToolTips = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				TreeToolTipDialog dialog = new TreeToolTipDialog(result.useTreeToolTips, result.treeToolTipWindow, data.getSequenceSet().getLength());

				result.useTreeToolTips = dialog.isOptionChecked();
				result.treeToolTipWindow = dialog.getWindowSize();

				// WinMainMenuBar.aFileSave.setEnabled(true);
				// WinMainMenuBar.aVamCommit.setEnabled(true);
				ProjectState.setDataChanged();
			}
		};

		bThres = (JButton) WinMainToolBar.getButton(false, null, "dss04", Icons.ADJUST_THRESHOLD, aThres);
		bAddPart = (JButton) WinMainToolBar.getButton(false, null, "dss03", Icons.ADD_PARTITION, aAddPart);
		bAutoPart = (JButton) WinMainToolBar.getButton(false, null, "dss02", Icons.AUTO_PARTITION, aAutoPart);
		bToolTips = (JButton) WinMainToolBar.getButton(false, null, "dss07", Icons.TREE_TOOLTIPS, aToolTips);

		add(bThres);
		add(bAddPart);
		add(bAutoPart);
		add(bToolTips);
	}

}
