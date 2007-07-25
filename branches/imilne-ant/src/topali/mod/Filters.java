package topali.mod;

import java.io.File;
import java.util.*;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public class Filters extends FileFilter
{
	public static final int PNG = 1;

	public static final int TRE = 2;

	public static final int FAS = 3;

	public static final int PHY_S = 4;

	public static final int PHY_I = 5;

	public static final int XML = 6;

	public static final int TOP = 7;

	public static final int MSF = 8;

	public static final int NEX = 9;

	public static final int ALN = 10;

	public static final int ZIP = 11;

	public static final int BAM = 12;

	public static final int CSV = 13;

	public static final int NEX_B = 14;

	public static final int CLU = 15;
	
	public static final int TXT = 16;

	private Hashtable<String, Object> filters = new Hashtable<String, Object>();

	private String description = null;

	private String fullDescription = null;

	private boolean useExtensionsInDescription = true;

	private String extStr;

	private int extInt;

	// Creates a new filter that filters on the given type
	public static Filters getFileFilter(int type)
	{
		Filters filter = new Filters();

		switch (type)
		{
		case PNG:
			filter.addExtension("png", PNG);
			filter.setDescription("Portable Network Graphics Image Files");
			break;

		case TRE:
			filter.addExtension("tre", TRE);
			filter.setDescription("New Hampshire Tree Files");
			break;

		//
		case FAS:
			filter.addExtension("fas", FAS);
			filter.addExtension("fasta", FAS);
			filter.setDescription("FastA Files");
			break;

		//
		case PHY_S:
			filter.addExtension("phy", PHY_S);
			filter.addExtension("phylip", PHY_S);
			filter.setDescription("Phylip Sequential Files");
			break;

		case PHY_I:
			filter.addExtension("phy", PHY_I);
			filter.addExtension("phylip", PHY_I);
			filter.setDescription("Phylip Interleaved Files");
			break;

		case XML:
			filter.addExtension("xml", XML);
			filter.setDescription("XML Project Files");
			break;

		case TOP:
			filter.addExtension("topali", TOP);
			filter.setDescription("TOPALi Project Files");
			break;

		//
		case NEX:
			filter.addExtension("nex", NEX);
			filter.setDescription("Nexus (Standard) Files");
			break;

		//
		case MSF:
			filter.addExtension("msf", MSF);
			filter.setDescription("GCG Multiple Sequence Format Files");
			break;

		//
		case ALN:
			filter.addExtension("aln", ALN);
			filter.setDescription("ClustalW Files");
			break;

		case ZIP:
			filter.addExtension("zip", ZIP);
			filter.setDescription("VAMSAS Zipped Archive Files");
			break;

		case CSV:
			filter.addExtension("csv", CSV);
			filter.setDescription("Comma Delimited Files");
			break;

		case NEX_B:
			filter.addExtension("nex", NEX_B);
			filter.setDescription("Nexus (MrBayes) Files");
			break;

		case CLU:
			filter.addExtension("txt", CLU);
			filter.setDescription("Grouped Cluster Files");
			break;

		case TXT:
			filter.addExtension("txt", TXT);
			filter.setDescription("Text Files");
			break;
			
		/*
		 * case 2 : filter.addExtension("csv"); filter.setDescription("CSV
		 * (Comma Delimited) Files"); break;
		 * 
		 * case 6 : filter.addExtension("gif"); filter.setDescription("Image
		 * Files"); break;
		 * 
		 * case 7 : filter.addExtension("htm"); filter.addExtension("html");
		 * filter.setDescription("HTML Files"); break;
		 * 
		 * case 13 : filter.addExtension("txt"); filter.setDescription("Plain
		 * Text Files"); break;
		 * 
		 * case 14 : filter.addExtension("txt"); filter.setDescription("Cluster
		 * Detail Files"); break;
		 */
		}

		return filter;
	}

	// Modifies the JFileChooser so that it contains file filters for the given
	// array of file types. Also sets the chooser so that the "selected" filter
	// is picked as the default
	public static void setFilters(JFileChooser fc, int selected,
			Object... index)
	{
		Filters[] filters = new Filters[index.length];

		FileFilter f = fc.getFileFilter();

		int toSelect = -1;
		for (int i = 0; i < index.length; i++)
		{
			int filterIndex = (Integer) index[i];

			filters[i] = Filters.getFileFilter(filterIndex);
			fc.addChoosableFileFilter(filters[i]);

			if (filterIndex == selected)
				toSelect = i;
		}

		if (toSelect >= 0)
			fc.setFileFilter(filters[toSelect]);
		else
			fc.setFileFilter(f);
	}

	// Returns the (last) extension set on this Filters object. Used to append
	// an extension onto filenames that were named without one.
	public String getExtStr()
	{
		return extStr;
	}

	public int getExtInt()
	{
		return extInt;
	}

	// Pulls back the selected file from the file chooser, then renames it if it
	// doesn't have a suitable extension. Then returns the file.
	public static File getSelectedFileForSaving(JFileChooser fc)
	{
		File file = fc.getSelectedFile();
		Filters filter = (Filters) fc.getFileFilter();

		// Make sure it has an appropriate extension
		if (file.exists() == false)
			if (file.getName().indexOf(".") == -1)
				file = new File(file.getPath() + "." + filter.getExtStr());

		return file;
	}

	// ////////////////////////////

	/**
	 * Return true if this file should be shown in the directory pane, false if
	 * it shouldn't.
	 * 
	 * Files that begin with "." are ignored.
	 * 
	 * @see #getExtension
	 * @see FileFilter#accepts
	 */
	public boolean accept(File f)
	{
		if (f != null)
		{
			if (f.isDirectory())
			{
				return true;
			}
			String extension = getExtension(f);
			if (extension != null && filters.get(getExtension(f)) != null)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Return the extension portion of the file's name .
	 * 
	 * @see #getExtension
	 * @see FileFilter#accept
	 */
	public String getExtension(File f)
	{
		if (f != null)
		{
			String filename = f.getName();
			int i = filename.lastIndexOf('.');
			if (i > 0 && i < filename.length() - 1)
			{
				return filename.substring(i + 1).toLowerCase();
			}
			;
		}
		return null;
	}

	/**
	 * Adds a filetype "dot" extension to filter against.
	 * 
	 * For example: the following code will create a filter that filters out all
	 * files except those that end in ".jpg" and ".tif":
	 * 
	 * ExampleFileFilter filter = new ExampleFileFilter();
	 * filter.addExtension("jpg"); filter.addExtension("tif");
	 * 
	 * Note that the "." before the extension is not needed and will be ignored.
	 */
	public void addExtension(String extension, int extInteger)
	{
		if (filters == null)
		{
			filters = new Hashtable<String, Object>(5);
		}
		filters.put(extension.toLowerCase(), this);
		fullDescription = null;

		extStr = extension.toLowerCase();
		extInt = extInteger;
	}

	/**
	 * Returns the human readable description of this filter. For example: "JPEG
	 * and GIF Image Files (*.jpg, *.gif)"
	 * 
	 * @see setDescription
	 * @see setExtensionListInDescription
	 * @see isExtensionListInDescription
	 * @see FileFilter#getDescription
	 */
	public String getDescription()
	{
		if (fullDescription == null)
		{
			if (description == null || isExtensionListInDescription())
			{
				fullDescription = description == null ? "(" : description
						+ " (";
				// build the description from the extension list
				Enumeration extensions = filters.keys();
				if (extensions != null)
				{
					fullDescription += "." + (String) extensions.nextElement();
					while (extensions.hasMoreElements())
					{
						fullDescription += ", "
								+ (String) extensions.nextElement();
					}
				}
				fullDescription += ")";
			} else
			{
				fullDescription = description;
			}
		}
		return fullDescription;
	}

	/**
	 * Sets the human readable description of this filter. For example:
	 * filter.setDescription("Gif and JPG Images");
	 * 
	 * @see setDescription
	 * @see setExtensionListInDescription
	 * @see isExtensionListInDescription
	 */
	public void setDescription(String description)
	{
		this.description = description;
		fullDescription = null;
	}

	/**
	 * Determines whether the extension list (.jpg, .gif, etc) should show up in
	 * the human readable description.
	 * 
	 * Only relevent if a description was provided in the constructor or using
	 * setDescription();
	 * 
	 * @see getDescription
	 * @see setDescription
	 * @see isExtensionListInDescription
	 */
	public void setExtensionListInDescription(boolean b)
	{
		useExtensionsInDescription = b;
		fullDescription = null;
	}

	/**
	 * Returns whether the extension list (.jpg, .gif, etc) should show up in
	 * the human readable description.
	 * 
	 * Only relevent if a description was provided in the constructor or using
	 * setDescription();
	 * 
	 * @see getDescription
	 * @see setDescription
	 * @see setExtensionListInDescription
	 */
	public boolean isExtensionListInDescription()
	{
		return useExtensionsInDescription;
	}
}
