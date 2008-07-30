package topali.gui;

import java.io.*;
import java.util.LinkedList;

public class BundleScanner
{
	private static LinkedList<String> enKeys;

	public static void main(String[] args)
	{
		File folder = new File(args[0]);

		scanFolder(folder);
	}

	private static void scanFolder(File folder)
	{
		for (File f: folder.listFiles())
		{
			if (f.isDirectory() && f.getName().contains(".svn") == false)
				scanFolder(f);

			if (f.getName().contains(".properties") == false)
				continue;

			if (f.getName().contains("_nl") == false &&
				f.getName().contains("_de") == false)
			{
				parseFile(f);
			}
		}
	}

	private static void parseFile(File en)
	{
		System.out.println();
		System.out.println("CHECKING " + en);

		enKeys = readKeys(en);

		File de = new File(en.getParent(), en.getName().replace(".prop", "_de.prop"));
		checkFile(de);
		File nl = new File(en.getParent(), en.getName().replace(".prop", "_nl.prop"));
		checkFile(nl);
	}

	private static void checkFile(File file)
	{
		System.out.println("  verifiying " + file);

		LinkedList<String> fileKeys = readKeys(file);
		if (fileKeys == null)
			return;

		for (String key: enKeys)
		{
			if (fileKeys.contains(key) == false)
			{
				System.out.println("    - missing: " + key);
			}
		}
	}

	private static LinkedList<String> readKeys(File file)
	{
		LinkedList<String> keys = new LinkedList<String>();

		try
		{
			BufferedReader in = new BufferedReader(new FileReader(file));

			String str = null;
			while ((str = in.readLine()) != null)
			{
				int index = str.indexOf("=");
				if (index != -1)
				{
					String key = str.substring(0, index).trim();
					keys.add(key);
				}
			}

			in.close();
		}
		catch (FileNotFoundException e)
		{
			System.out.println("    FILE NOT FOUND");
			return null;
		}
		catch (Exception e)
		{
			System.out.println("  " + e);
		}

		return keys;
	}
}