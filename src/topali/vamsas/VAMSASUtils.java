// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.vamsas;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.exolab.castor.xml.*;

import topali.data.AlignmentData;
import topali.fileio.Castor;
import topali.gui.Project;
import uk.ac.vamsas.client.*;

public class VAMSASUtils
{
	static Logger log = Logger.getLogger(VAMSASUtils.class);
	
	public static boolean storeProject(Project project, IClientDocument cDoc) {	
		
		IClientAppdata data = cDoc.getClientAppdata();
		if(data==null) {
			log.warn("Could not get a IClientAppdata.");
			return false;
		}
		
		StringWriter out = new StringWriter();
		
		try
		{
			Marshaller m = new Marshaller(out);
			m.setMapping(Castor.getMapping());
			m.setEncoding("UTF-8");
			m.marshal(project);
			
			data.getClientOutputStream().write(out.toString().getBytes());
		} catch (Exception e)
		{
			log.warn("Marshalling failed.", e);
			return false;
		} 
		
		//data.setUserAppdata(out.toString().getBytes());
		
		return true;
	}
	
	public static Project loadProject(Project project, IClientDocument cDoc) {
		IClientAppdata data = cDoc.getClientAppdata();
		if(data==null) {
			log.warn("Could not get a IClientAppdata.");
			return project;
		}

		if(!data.hasClientAppdata()) {
			log.info("No stored project found in VAMSAS document.");
			return project;
		}
		
		DataInput din = data.getClientInputStream();
		Vector<Byte> tmp = new Vector<Byte>();
		while(true) {
			try
			{
				tmp.add(din.readByte());
			} catch (IOException e)
			{
				break;
			}
		}
		
		byte[] bytes = new byte[tmp.size()];
		for(int i=0; i<tmp.size(); i++)
			bytes[i] = tmp.get(i).byteValue();
		
		InputStreamReader in = new InputStreamReader(new ByteArrayInputStream(bytes));
		Unmarshaller u = new Unmarshaller();
		try
		{
			u.setMapping(Castor.getMapping());
			Project vProject = (Project)u.unmarshal(in);
			if(project!=null) {
				for(AlignmentData align : vProject.getDatasets())
					project.addDataSet(align);
				return project;
			}
			return vProject;
		} catch (Exception e)
		{
			log.warn("Unmarshalling failed.", e);
			return null;
		}
	}
}
