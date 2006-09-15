package topali.vamsas;

import java.io.*;
import java.util.*;

import topali.data.*;

import org.vamsas.client.*;
import org.vamsas.client.simpleclient.*;
import org.vamsas.objects.core.*;
import org.vamsas.test.simpleclient.*;

public class VamsasClient extends ArchiveClient implements Runnable
{
	private Hashtable hashtable = new Hashtable();
	
	public VamsasClient(File sessionFile)
	{
		super(System.getProperty("user.name"), "BioSS", sessionFile);
		
		new Thread(this).start();
	}
		
	public void writeToFile(AlignmentData data)
	{
		System.out.println("Hash for dataset is: " + data.hashCode());
		
		ClientDoc cDoc = getUpdateable();
		
		VAMSAS[] roots = cDoc.getVamsasRoots();
		
//		ArchiveReports.rootReport(cDoc.getVamsasRoots(), true, System.out);
		
		// The object we're going to create (or update)
		VAMSAS vVAMSAS = null;
		
		// See if we can find this dataset
		String key = (String) hashtable.get(data);
		if (key != null)
		{
			for (int i = 0; i < roots.length; i++)
			{
				String id = roots[i].getVorbaId().getId();
				if (key.equals(id))
					vVAMSAS = roots[i];
			}
		}
		
		TOPALi2Vamsas writer = null;
		if (vVAMSAS == null)
			writer = new TOPALi2Vamsas();
		else
			writer = new TOPALi2Vamsas(vVAMSAS);
		
		vVAMSAS = writer.createVAMSAS(data);
		
		// The dataset can now be used as a key to store the ID assigned to its
		// VAMSAS partner for later use
		String vID = cDoc.registerObject(vVAMSAS).getId();
		hashtable.put(data, vID);
		
		// Only add if it didn't already exist
//		if (key == null)
		// TODO: Come up with some magic way of working out what's changed and needs updated
		// For now, just always add ANOTHER dataset to the object
			cDoc.addVamsasRoot(vVAMSAS);
		doUpdate(cDoc);
		
				
		cDoc.closeDoc();
		
		System.out.println(hashtable);
	}
	
	public AlignmentData[] readFromFile()
	{
		// Store all results read in a temp vector
		Vector<AlignmentData> vector = new Vector<AlignmentData>();
		
		ClientDoc cDoc = getUpdateable();
		
		VAMSAS[] roots = cDoc.getVamsasRoots();
		
		for (int i = 0; i < roots.length; i++)
		{
			Vamsas2TOPALi reader = new Vamsas2TOPALi();
			
			try
			{
				AlignmentData[] data = reader.createTOPALi(roots[i]);
				for (AlignmentData d: data)
					vector.add(d);
			}
			catch (Exception e) {}
		}
		
		cDoc.closeDoc();
		
		AlignmentData[] array = new AlignmentData[vector.size()];
		for (int i = 0; i < vector.size(); i++)
			array[i] = vector.get(i);
		
		return array;
	}
	
	public void run()
	{
		while (true)
		{
			ClientDoc cDoc = watch(0);
			
			if (cDoc != null)
			{
				System.out.println("Doc changed");
				cDoc.closeDoc();
			}
			else
				System.out.println("Doc NOT changed");
			
			// Wait for X milliseconds until it's time to check again
			try { Thread.sleep(50); }
			catch (InterruptedException e) {}
		}		
	}
}