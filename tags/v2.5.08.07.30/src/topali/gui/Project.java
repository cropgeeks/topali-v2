// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import static topali.mod.Filters.TOP;

import java.beans.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;

import org.apache.log4j.Logger;
import org.exolab.castor.xml.*;
import org.xml.sax.InputSource;

import scri.commons.file.FileUtils;
import topali.data.*;
import topali.fileio.*;
import topali.gui.dialog.LoadMonitorDialog;
import topali.i18n.Text;
import topali.mod.Filters;
import scri.commons.gui.MsgBox;

public class Project extends DataObject
{
	 static Logger log = Logger.getLogger(Project.class);

	 public String appversion;

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
		appversion = Install4j.VERSION;
	}

	public LinkedList<AlignmentData> getDatasets()
	{
		return datasets;
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

	public AlignmentData containsDatasetBySeqs(AlignmentData data) {
		AlignmentData match = null;
		for(AlignmentData data2 : datasets) {
			if(data.getName().equals(data2.getName()) && data.getSequenceSet().getSize()==data2.getSequenceSet().getSize()) {
				SequenceSet ss = data.getSequenceSet();
				SequenceSet ss2 = data2.getSequenceSet();
				boolean seqMatch = true;
				for(int i=0; i<ss.getSize(); i++) {
					Sequence s = ss.getSequence(i);
					Sequence s2 = ss2.getSequence(i);
					//seqMatch = s.getName().equals(s2.getName()) && s.getSequence().equals(s2.getSequence());
					seqMatch = s.getSequence().equals(s2.getSequence());
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

	public int[] getTreePath()
	{
		return treePath;
	}

	public void setTreePath(int[] treePath)
	{
		this.treePath = treePath;
	}

	// Calls load() to load the given project from disk, or opens a FileDialog
	// to prompt for the project name if filename is null
	public static Project open(String filename)
	{
		if (filename != null)
			return open(new File(filename));

		// Create the dialog
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File(Prefs.gui_dir));
		fc.setDialogTitle(Text.get("Project.gui01"));

		Filters.setFilters(fc, TOP, TOP);

		if (fc.showOpenDialog(TOPALi.winMain) == JFileChooser.APPROVE_OPTION)
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
			byte[] xml = readZipFileEntry(filename, "project.xml");

			//Check with which version we're dealing with:
			//if there is no appVersion attribute it's 2.17
			String appVersion = "2.17";
			BufferedReader reader = new BufferedReader(new StringReader(new String(xml, "UTF-8")));
			String line = null;
			while((line=reader.readLine())!=null) {
				if(line.matches("\\.*<project.*\\>.*")) {
					int i = line.indexOf("appversion=");
					if(i!=-1) {
						int s = line.indexOf('"', i);
						int e = line.indexOf('"', s+1);
						appVersion = line.substring(s+1, e);
						log.info("Project file appVersion="+appVersion);
					}
					else
						log.info("Project file appVersion<2.17");
					break;
				}
			}
			reader.close();

			if(!appVersion.equals(Install4j.VERSION)) {
				String notes = "";
				if(appVersion.equals("2.17")) {
					URL tmpUrl = Project.class.getResource("/res/xslt/2.17-2.18.xsl");
					File xsltFile = new File(SysPrefs.tmpDir, "topali_2.17-2.18.xsl");
					FileUtils.writeFile(xsltFile, tmpUrl.openStream());
					notes += getTranformationNotes(xsltFile);
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					InputStream xmlin = new ByteArrayInputStream(xml);
					InputStream xslin = new FileInputStream(xsltFile);
					XSLTransformer.transform(xmlin, xslin, bos);
					xml = bos.toByteArray();
					bos.close();
					xmlin.close();
					xslin.close();
					appVersion = "2.18";
				}

				if(appVersion.equals("2.18")) {
				  //2.19 is just a bug fix release, data structure didn't change
					appVersion = "2.19";
				}

				// ... do further tranformations in future (2.18-2.19, 2.19-...)

				if(!notes.equals("")) {
					String msg = "This project was created with an older TOPALi version.\n" +
							"TOPALi will try to import it, but it is possible that not all\n" +
							"data can be transferred. Please read these notes carefully:\n\n";
					msg += notes;
					MsgBox.msg(msg, MsgBox.INF);
				}
			}

			String str = Text.get("LoadMonitorDialog.gui06");
			LoadMonitorDialog.setLabel(str);

			Unmarshaller unmarshaller = Castor.getUnmarshaller();
			ByteArrayInputStream bis = new ByteArrayInputStream(xml);
			Project p = (Project) unmarshaller.unmarshal(new InputSource(bis));
			bis.close();

			p.filename = filename;
			return p;

		} catch (Exception e)
		{
			MsgBox.msg(Text.get("Project.err01",filename, e), MsgBox.ERR);

			log.warn("Error opening file\n",e);
			return null;
		}
	}

	private static byte[] readZipFileEntry(File zipFile, String entry) throws Exception {
		ZipFile zip = new ZipFile(zipFile);
		ZipEntry e = zip.getEntry(entry);
		InputStream zin = zip.getInputStream(e);

		byte[] data = null;
		ArrayList<Byte> buffer = new ArrayList<Byte>();
		int c = -1;
		while((c=zin.read())!=-1) {
			buffer.add((byte)c);
		}
		zin.close();
		zip.close();

		data = new byte[buffer.size()];
		for(int i=0; i<buffer.size(); i++)
			data[i] = buffer.get(i);

		//Convert to String and back to bytes, to remove possible invalid characters
		String st = new String(data, "UTF-8");
		st = st.replaceAll("\uFFFD", "_"); //replace invalid char with _
		return st.getBytes("UTF-8");
	}

	private static String getTranformationNotes(File xsltFile) throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(xsltFile));
		String line = null;
		boolean start = false;
		StringBuffer sb = new StringBuffer();
		while((line=in.readLine())!=null) {
			if(line.matches(".*Notes:.*")) {
				start = true;
				continue;
			}
			if(line.matches(".*\\-\\-\\>.*") && start)
				break;

			if(start) {
				sb.append(line+"\n");
			}
		}
		in.close();

		return sb.toString();
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
			MsgBox.msg(Text.get("Project.err02", p.filename, e.getMessage()), MsgBox.ERR);

			log.warn("Error saving file\n",e);
			return false;
		}

		return true;
	}

	private boolean saveAs()
	{
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(Text.get("Project.gui02"));
		fc.setAcceptAllFileFilterUsed(false);
		// If the project has never been saved it won't have a filename object
		if (filename != null)
			fc.setSelectedFile(filename);
		else
			fc.setSelectedFile(new File(Prefs.gui_dir, "project "
					+ Prefs.gui_project_count + ".topali"));

		Filters.setFilters(fc, TOP, TOP);

		while (fc.showSaveDialog(TOPALi.winMain) == JFileChooser.APPROVE_OPTION)
		{
			File file = Filters.getSelectedFileForSaving(fc);

			// Confirm overwrite
			if (file.exists())
			{
				String msg = Text.get("Project.msg01", file);
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

	public void merge(Project proj) {
		for(AlignmentData data : proj.getDatasets()) {
			boolean found = false;
			for(AlignmentData thisData : datasets) {
				if(thisData.getID()==data.getID()) {
					thisData.merge(data);
					found = true;
				}
			}
			if(!found)
				addDataSet(data);
		}
	}

}
