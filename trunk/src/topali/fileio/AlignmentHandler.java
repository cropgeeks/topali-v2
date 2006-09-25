// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.fileio;

import java.io.*;

import topali.data.*;
import static topali.fileio.AlignmentLoadException.*;
import static topali.mod.Filters.*;

/*
 * Handler class for the file types that the program supports
 */
public class AlignmentHandler
{
	private SequenceSet ss;
	
	public AlignmentHandler(SequenceSet ss)
		{ this.ss = ss; }
	
	
	public void openAlignment(File filename)
		throws AlignmentLoadException
	{
		// Can we load it?
		if (readFile(filename) == false)
			throw new AlignmentLoadException(UNKNOWN_FORMAT);
	}
	
	
	// Attempts to read the given file by sequentially loading it with each of
	// the supported file type classes until it is successfully read or fails
	private boolean readFile(File file)
	{
		ISeqFile seqFile = null;

		// Try possible file formats until successfull
/*		seqFile = new FileNexus(ss);
		if (seqFile.readFile(file))
			return true;
		
		seqFile = new FileMSF(ss);
		if (seqFile.readFile(file))
			return true;
			
		seqFile = new FileClustal(ss);
		if (seqFile.readFile(file))
			return true;

		seqFile = new FileFasta(ss);
		if (seqFile.readFile(file))
			return true;
*/
		seqFile = new FileReadSeq(ss);
		if (seqFile.readFile(file))
			return true;

//		seqFile = new FileGeneric(ss);
//		if (seqFile.readFile(file))
//			return true;

		return false;
	}

	public void save(File file, int[] sequences, int start, int end, int format, boolean useSafeNames)
		throws Exception
	{
		ISeqFile iFile = null;
		
		switch (format)
		{
			case FAS: iFile = new FileFasta(ss); break;
			case ALN: iFile = new FileClustal(ss); break;
			case MSF: iFile = new FileMSF(ss); break;
			case NEX: iFile = new FileNexus(ss); break;
			case NEX_B: iFile = new FileNexusMB(ss); break;
			case BAM: iFile = new FileBambe(ss); break;
			case PHY_S : iFile = new FilePhylipSeq(ss); break;
			case PHY_I : iFile = new FilePhylipInt(ss); break;
		}
		
		iFile.writeFile(file, sequences, start, end, useSafeNames);
	}

	// Prompts the user to save the current sequence set to a file
/*	void exportPartition(WinMain parent, int start, int end)
	{
		Object[] values = { TXT.gui.getString("ah_str01"),
			TXT.gui.getString("ah_str02") };
		int initial = Prefs.gui_part_select_all == true ? 0 : 1;
		
		Object selectedValue = JOptionPane.showInputDialog(MsgBox.ownerC,
			TXT.gui.getString("ah_str03"), TXT.gui.getString("ah_str04"),
			JOptionPane.QUESTION_MESSAGE, null, values, values[initial]);

		// If nothing was selected, just exit		
		if (selectedValue == null)
			return;
			
		Prefs.gui_part_select_all = (selectedValue == values[0] ? true : false);
		
		// Create a suitable base name for this partition
		String name = ss.getName() + " (" + (start+1) + "-" + end + ")";
	
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File(Prefs.gui_proj_dir));
		fc.setSelectedFile(new File(name));
		fc.setDialogTitle(TXT.gui.getString("ah_str04"));
		fc.setAcceptAllFileFilterUsed(false);

		// Filters
		int[] filters = { 5, 12, 8, 4, 10, 9, 15 };
		Filters.setFilters(fc, filters, Prefs.gui_file_export);
		

		while (fc.showDialog(parent, TXT.gui.getString("ah_str05"))
			== JFileChooser.APPROVE_OPTION)
		{
			Prefs.gui_file_export = fc.getFileFilter().getDescription();
			File file = fc.getSelectedFile();
			
			// Make sure it has an appropriate extension
			if (!file.exists())
			{
				System.out.println(file.getName());
				if (file.getName().indexOf(".") == -1)
				{
					if (Prefs.gui_file_export.startsWith("FastA"))
						file = new File(file.getPath() + ".fasta");
					else if (Prefs.gui_file_export.startsWith("Bambe"))
						file = new File(file.getPath() + ".bambe");
					else if (Prefs.gui_file_export.startsWith("Clustal"))
						file = new File(file.getPath() + ".aln");
					else if (Prefs.gui_file_export.startsWith("Phylip"))
						file = new File(file.getPath() + ".phylip");
					else if (Prefs.gui_file_export.startsWith("Nexus"))
						file = new File(file.getPath() + ".nex");
					else if (Prefs.gui_file_export.startsWith("GCG/MSF"))
						file = new File(file.getPath() + ".msf");
				}
			}
			
			if (file.exists())
			{
				int response = MsgBox.yesnocan(
					MsgBox.format(TXT.gui.getString("ah_str06"), "" + file), 1);
					
				if (response == JOptionPane.NO_OPTION)
					continue;
				else if (response == JOptionPane.CANCEL_OPTION ||
					response == JOptionPane.CLOSED_OPTION)
					return;
			}
			
			// Otherwise it's ok to save...
			Prefs.gui_proj_dir = "" + fc.getCurrentDirectory();
			saveFile(file, Prefs.gui_part_select_all, start, end);
			return;
		}
	}
	
	private void saveFile(File filename, boolean exportAll,
		int start, int end)
	{
		ISeqFile file = null;
		
		System.out.println("Save file from " + start + " to " + end);
		
		
		if (Prefs.gui_file_export.startsWith("FastA"))
			file = new FileFasta(ss);
		else if (Prefs.gui_file_export.startsWith("Bambe"))
			file = new FileBambe(ss);
		else if (Prefs.gui_file_export.startsWith("Clustal"))
			file = new FileClustal(ss);
		else if (Prefs.gui_file_export.startsWith("Phylip 3.4"))
			file = new FilePhylipInt(ss);
		else if (Prefs.gui_file_export.startsWith("Phylip Seq"))
			file = new FilePhylipSeq(ss);
		else if (Prefs.gui_file_export.startsWith("Nexus"))
			file = new FileNexus(ss);
		else if (Prefs.gui_file_export.startsWith("GCG/MSF"))
			file = new FileMSF(ss);
	
		int[] indices;
		if (exportAll)
			indices = ss.getAllSequences();
		else
			indices = ss.getSelectedSequences();
		
		if (file.writeFile(filename, indices, start, end))
			MsgBox.msg(MsgBox.format(TXT.gui.getString("ah_str07"),
				"" + filename),	MsgBox.INF);
	}
*/
}
