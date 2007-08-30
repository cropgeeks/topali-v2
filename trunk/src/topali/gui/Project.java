// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import static topali.mod.Filters.TOP;

import java.beans.*;
import java.io.*;
import java.util.LinkedList;
import java.util.zip.*;

import javax.swing.*;

import org.apache.log4j.Logger;
import org.exolab.castor.xml.*;

import topali.data.*;
import topali.fileio.Castor;
import topali.gui.dialog.LoadMonitorDialog;
import topali.mod.Filters;
import doe.MsgBox;

public class Project extends DataObject
{
	static Logger log = Logger.getLogger(Project.class);
	
	// Temporary object used to track the (most recent) file this project was
	// opened from
	public File filename;

	// The datasets within the project
	private LinkedList<AlignmentData> datasets = new LinkedList<AlignmentData>();

	// Index of the selection path in the navigation tree, so TOPALi can restore
	// its state after a project-load
	private int[] treePath;

	public Project()
	{
	}

	public LinkedList<AlignmentData> getDatasets()
	{
		return datasets;
	}

	public void setDatasets(LinkedList<AlignmentData> datasets)
	{
		this.datasets = datasets;
	}

	public void addDataSet(AlignmentData data) {
		this.datasets.add(data);
		for(PropertyChangeListener l : changeListeners) {
			l.propertyChange(new PropertyChangeEvent(this, "alignmentData", null, data));
			//propagate listeners to alignment datasets
			data.addChangeListener(l);
		}
	}
	
	void removeDataSet(AlignmentData data)
	{
		datasets.remove(data);
		for(PropertyChangeListener l : changeListeners) {
			l.propertyChange(new PropertyChangeEvent(this, "alignmentData", data, null));
		}
	}

	public int[] getTreePath()
	{
		return treePath;
	}

	public void setTreePath(int[] treePath)
	{
		this.treePath = treePath;
	}

	public AlignmentData containsDatasetBySeqs(AlignmentData data) {
		AlignmentData match = null;
		for(AlignmentData data2 : datasets) {
			if(data.name.equals(data2.name) && data.getSequenceSet().getSize()==data2.getSequenceSet().getSize()) {
				SequenceSet ss = data.getSequenceSet();
				SequenceSet ss2 = data2.getSequenceSet();
				boolean seqMatch = true;
				for(int i=0; i<ss.getSize(); i++) {
					Sequence s = ss.getSequence(i);
					Sequence s2 = ss2.getSequence(i);
					seqMatch = s.getName().equals(s2.getName()) && s.getSequence().equals(s2.getSequence());
					if(!seqMatch)
						break;
				}
				if(seqMatch) {
					match = data2;
					break;
				}
			}
		}
		return match;
	}
	
//	public AlignmentData containsDatasetByID(AlignmentData data) {
//		for(AlignmentData data2 : datasets) {
//			if(data.getId()==data2.getId()) {
//				return data;
//			}
//		}
//		return null;
//	}
	
	// Calls load() to load the given project from disk, or opens a FileDialog
	// to prompt for the project name if filename is null
	public static Project open(String filename)
	{
		if (filename != null)
			return open(new File(filename));

		// Create the dialog
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File(Prefs.gui_dir));
		fc.setDialogTitle(Text.Gui.getString("Project.gui01"));

		Filters.setFilters(fc, TOP, TOP);

		if (fc.showOpenDialog(MsgBox.frm) == JFileChooser.APPROVE_OPTION)
		{
			File file = fc.getSelectedFile();
			Prefs.gui_dir = "" + fc.getCurrentDirectory();

			return open(file);
		}

		return null;
	}

	static Project open(File filename)
	{
		try
		{
			ZipFile zipFile = new ZipFile(filename);
			InputStream zin = zipFile
					.getInputStream(new ZipEntry("project.xml"));
			BufferedReader in = new BufferedReader(new InputStreamReader(zin));

			String str = Text.GuiDiag.getString("LoadMonitorDialog.gui06");
			LoadMonitorDialog.setLabel(str);

			// Create a new Unmarshaller
			// Castor.initialise();
			// Unmarshaller unmarshaller = new Unmarshaller();//Project.class);
			// unmarshaller.setMapping(Castor.getMapping());
			// unmarshaller.setWhitespacePreserve(true);
			// unmarshaller.setIgnoreExtraElements(true);

			// Unmarshal the person object
			Unmarshaller unmarshaller = Castor.getUnmarshaller();
			Project p = (Project) unmarshaller.unmarshal(in);
			in.close();

			p.filename = filename;

			zipFile.close();

			return p;
		} catch (Exception e)
		{
			MsgBox.msg(Text.format(Text.Gui.getString("Project.err01"),
					filename, e), MsgBox.ERR);

			log.warn("Error opening file\n",e);
			return null;
		}
	}

	static boolean save(Project p, boolean saveAs)
	{
		if (p.filename == null)
			saveAs = true;
		if (saveAs && p.saveAs() == false)
			return false;

		try
		{
			long s = System.currentTimeMillis();

			// Open an output stream to the zip...
			ZipOutputStream zOut = new ZipOutputStream(
					new BufferedOutputStream(new FileOutputStream(p.filename)));
			// And another for Castor to write to within the zip...
			BufferedWriter cOut = new BufferedWriter(new OutputStreamWriter(
					zOut));

			ZipEntry entry = new ZipEntry("project.xml");
			zOut.putNextEntry(entry);

			Marshaller m = new Marshaller(cOut);
			m.setMapping(Castor.getMapping());
			m.setEncoding("UTF-8");
			m.marshal(p);

			cOut.close();
			zOut.close();

			log.info("XML/Zip Write: "+ (System.currentTimeMillis() - s));
		} catch (Exception e)
		{
			MsgBox.msg(Text.format(Text.Gui.getString("Project.err02"),
					p.filename, e.getMessage()), MsgBox.ERR);

			log.warn("Error saving file\n",e);
			return false;
		}

		return true;
	}

	private boolean saveAs()
	{
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(Text.Gui.getString("Project.gui02"));
		fc.setAcceptAllFileFilterUsed(false);
		// If the project has never been saved it won't have a filename object
		if (filename != null)
			fc.setSelectedFile(filename);
		else
			fc.setSelectedFile(new File(Prefs.gui_dir, "project "
					+ Prefs.gui_project_count + ".topali"));

		Filters.setFilters(fc, TOP, TOP);

		while (fc.showSaveDialog(MsgBox.frm) == JFileChooser.APPROVE_OPTION)
		{
			File file = Filters.getSelectedFileForSaving(fc);

			// Confirm overwrite
			if (file.exists())
			{
				String msg = Text.format(Text.Gui.getString("Project.msg01"),
						file);
				int response = MsgBox.yesnocan(msg, 1);

				if (response == JOptionPane.NO_OPTION)
					continue;
				else if (response == JOptionPane.CANCEL_OPTION
						|| response == JOptionPane.CLOSED_OPTION)
					return false;
			}

			// Otherwise it's ok to save...
			Prefs.gui_dir = "" + fc.getCurrentDirectory();
			Prefs.gui_project_count++;
			filename = file;

			return true;
		}

		return false;
	}
}
