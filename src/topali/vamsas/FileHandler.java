package topali.vamsas;

import java.io.*;
import javax.swing.*;

import topali.gui.*;
import topali.mod.*;
import static topali.mod.Filters.*;

import doe.*;

public class FileHandler
{
	public FileHandler()
	{
	}
	
	public void saveVamsas(topali.data.AlignmentData tAlignmentData)
	{
/*		File filename = saveAs();
		if (filename == null)
			return;
		
		long s = System.currentTimeMillis();
		TOPALi2Vamsas translator = new TOPALi2Vamsas();
		VAMSAS obj = translator.createVAMSAS(tAlignmentData);
		System.out.println("Conversion in " + (System.currentTimeMillis()-s));
		
		try
		{
			// Open an output stream to the zip...
			ZipOutputStream zOut = new ZipOutputStream(
				new BufferedOutputStream(new FileOutputStream(filename)));
			// And another for Castor to write to within the zip...
			BufferedWriter cOut = new BufferedWriter(
				new OutputStreamWriter(zOut));
			
			ZipEntry entry = new ZipEntry("vamsas.xml");
			zOut.putNextEntry(entry);
			
			Castor.initialise();
			
			Marshaller m = new Marshaller(cOut);
			m.setEncoding("UTF-8");
			m.marshal(obj);
			
			zOut.close();
		}
		catch (Exception e)
		{
			MsgBox.msg("Error while writing vamsas XML:\n  " + e, MsgBox.ERR);
		}
*/
	}
	
	public topali.data.AlignmentData[] loadVamsas()
	{
		return null;
		
/*		File filename = open();
		if (filename == null)
			return null;
		
		try
		{
			ZipFile zipFile = new ZipFile(filename);
			InputStream zin = zipFile.getInputStream(
				new ZipEntry("vamsas.xml"));
			BufferedReader in = new BufferedReader(new InputStreamReader(zin));
			
			Castor.initialise();
			
//			Unmarshaller unmarshaller = new Unmarshaller(JalviewFile.class);
			Unmarshaller unmarshaller = new Unmarshaller(VAMSAS.class);
			unmarshaller.setWhitespacePreserve(true);
			
//			JalviewFile jv = (JalviewFile) unmarshaller.unmarshal(in);
			VAMSAS vVamsas = (VAMSAS) unmarshaller.unmarshal(in);	
			zipFile.close();
			
			Vamsas2TOPALi translator = new Vamsas2TOPALi();

//			return translator.createTOPALi(jv.getVAMSAS());
			return translator.createTOPALi(vVamsas);
		}
		catch (AlignmentLoadException e)
		{
			int code = e.getReason();
			MsgBox.msg(Text.GuiFile.getString("ImportDataSetDialog.err0"
				+ code), MsgBox.ERR);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
			MsgBox.msg("Error while reading vamsas XML:\n  " + e, MsgBox.ERR);
		}
		
		return null;
*/
	}
	
	private File open()
	{
		// Create the dialog
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File(Prefs.gui_dir));
		fc.setDialogTitle("Import VAMSAS XML");
		
		Filters.setFilters(fc, ZIP, ZIP);
		
		if (fc.showOpenDialog(MsgBox.frm) == JFileChooser.APPROVE_OPTION)
		{
			File file = fc.getSelectedFile();
			Prefs.gui_dir = "" + fc.getCurrentDirectory();
			
			return file;
		}
		
		return null;
	}
	
	private File saveAs()
	{
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Export VAMSAS XML");
		fc.setSelectedFile(new File(Prefs.gui_dir, "vamsas.zip"));
		fc.setAcceptAllFileFilterUsed(false);
		
		Filters.setFilters(fc, ZIP, ZIP);

		while (fc.showSaveDialog(MsgBox.frm) == JFileChooser.APPROVE_OPTION)
		{
			File file = Filters.getSelectedFileForSaving(fc);
			
			// Confirm overwrite
			if (file.exists())
			{
				String msg = file + " exists. Overwrite?";
				int response = MsgBox.yesnocan(msg, 1);
					
				if (response == JOptionPane.NO_OPTION)
					continue;
				else if (response == JOptionPane.CANCEL_OPTION ||
					response == JOptionPane.CLOSED_OPTION)
					return null;
			}
			
			// Otherwise it's ok to save...
			Prefs.gui_dir = "" + fc.getCurrentDirectory();
			
			return file;
		}
		
		return null;
	}
}