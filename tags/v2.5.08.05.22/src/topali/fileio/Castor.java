// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.fileio;

import java.io.*;

import org.apache.log4j.Logger;
import org.exolab.castor.mapping.*;
import org.exolab.castor.xml.*;

public class Castor
{
	static  Logger log = Logger.getLogger(Castor.class);
	
	private static Mapping mapping;

	private static Unmarshaller unmarshaller;

	// Private constructor so that this class can never be instantiated
	private Castor()
	{
	}

	static
	{
	    org.exolab.castor.util.LocalConfiguration.getInstance().getProperties()
	    .setProperty("org.exolab.castor.parser", "org.xml.sax.helpers.XMLReaderAdapter");

	org.exolab.castor.util.LocalConfiguration.getInstance().getProperties()
	    .setProperty("org.exolab.castor.xml.serializer.factory", "org.exolab.castor.xml.XercesJDK5XMLSerializerFactory");
	  
	
		try
		{
			mapping = new Mapping();
			mapping.loadMapping(""
					+ new Castor().getClass().getResource("/res/topali.xml"));

			if (unmarshaller == null)
			{
			unmarshaller = new Unmarshaller(mapping);
			unmarshaller.setWhitespacePreserve(true);
			unmarshaller.setIgnoreExtraElements(true);
			}
		} catch (Exception e)
		{
			// Critical error. Set the unmarshaller to null so it cannot be used
			log.fatal("Cannot create Unmarshaller!", e);
			unmarshaller = null;
		}
	}

	public static Unmarshaller getUnmarshaller()
	{
		return unmarshaller;
	}

	public static Mapping getMapping()
	{
		return mapping;
	}

	// Returns an XML representation of the given Object (based on the
	// information set in the mapping file)
	public static String getXML(Object obj) throws MappingException,
			IOException, MarshalException, ValidationException
	{
		
		StringWriter sOut = new StringWriter();

		Marshaller marshaller = new Marshaller(sOut);
		marshaller.setMapping(mapping);
		marshaller.setEncoding("UTF-8");
		marshaller.marshal(obj);

		try
		{
			sOut.close();
		} catch (IOException e)
		{
			log.warn(e);
		}

		return sOut.toString();
	}

	public static Object unmarshall(String xml) throws MarshalException,
			ValidationException
	{
		
		if(xml==null)
			log.warn("XML == null!");
		
		if(unmarshaller==null)
			log.warn("unmarshaller == null!");
		
		StringReader sr = new StringReader(xml);
		
		Object obj = unmarshaller.unmarshal(sr);
		return obj;
	}

	public static Object unmarshall(File file) throws IOException,
			MarshalException, ValidationException
	{
		
		BufferedReader in = new BufferedReader(new FileReader(file));
		Object obj = unmarshaller.unmarshal(in);
		in.close();

		return obj;
	}

	/*
	 * public static SequenceSet getSequenceSet(String xml) throws Exception {
	 * Castor.initialise(); Unmarshaller unmarshaller = new
	 * Unmarshaller(SequenceSet.class);
	 * unmarshaller.setWhitespacePreserve(true);
	 * unmarshaller.setMapping(getMapping());
	 * 
	 * return (SequenceSet) unmarshaller.unmarshal(new StringReader(xml)); }
	 * 
	 * public static PDMResult getPDMResult(String xml) throws Exception {
	 * Castor.initialise(); Unmarshaller unmarshaller = new
	 * Unmarshaller(PDMResult.class); unmarshaller.setMapping(getMapping());
	 * 
	 * return (PDMResult) unmarshaller.unmarshal(new StringReader(xml)); }
	 * 
	 * public static HMMResult getHMMResult(String xml) throws Exception {
	 * Castor.initialise(); Unmarshaller unmarshaller = new
	 * Unmarshaller(HMMResult.class); unmarshaller.setMapping(getMapping());
	 * 
	 * return (HMMResult) unmarshaller.unmarshal(new StringReader(xml)); }
	 * 
	 * public static DSSResult getDSSResult(String xml) throws Exception {
	 * Unmarshaller unmarshaller = new Unmarshaller(DSSResult.class); //
	 * unmarshaller.setMapping(getMapping());
	 * 
	 * return (DSSResult) unmarshaller.unmarshal(new StringReader(xml)); }
	 * 
	 * public static LRTResult getLRTResult(String xml) throws Exception {
	 * Castor.initialise(); Unmarshaller unmarshaller = new
	 * Unmarshaller(LRTResult.class); unmarshaller.setMapping(getMapping());
	 * 
	 * return (LRTResult) unmarshaller.unmarshal(new StringReader(xml)); }
	 * 
	 * public static MBTreeResult getMBTreeResult(String xml) throws Exception {
	 * Castor.initialise(); Unmarshaller unmarshaller = new
	 * Unmarshaller(MBTreeResult.class); unmarshaller.setMapping(getMapping());
	 * 
	 * return (MBTreeResult) unmarshaller.unmarshal(new StringReader(xml)); }
	 * 
	 * public static JobStatus getJobStatus(String xml) throws Exception {
	 * Castor.initialise(); Unmarshaller unmarshaller = new
	 * Unmarshaller(JobStatus.class); unmarshaller.setMapping(getMapping());
	 * 
	 * return (JobStatus) unmarshaller.unmarshal(new StringReader(xml)); }
	 */

	/*
	 * public static Mapping get3Mapping() throws IOException, MappingException {
	 * if (mapping == null) { mapping = new Mapping(); mapping.loadMapping( "" +
	 * new Castor().getClass().getResource("/res/topali.xml")); }
	 * 
	 * return mapping; }
	 */
	public static void saveXML(Object obj, File file) throws Exception
	{
		
		BufferedWriter out = new BufferedWriter(new FileWriter(file));

		out.write(getXML(obj));
		out.close();
	}
}