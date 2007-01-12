package topali.vamsas;

import uk.ac.vamsas.objects.core.*;

public class JalviewFile
{
	private String creationDate, version;
	private VAMSAS vamsas;
	
	public JalviewFile()
	{
	}
	
	public void setCreationDate(String creationDate)
		{ this.creationDate = creationDate; }
	public String getCreationDate()
		{ return creationDate; }
	
	public void setVersion(String version)
		{ this.version = version; }
	
	public String getVersion() { return version; }
	
	public void setVAMSAS(VAMSAS vamsas)
		{ this.vamsas = vamsas; }
	public VAMSAS getVAMSAS()
		{ return vamsas; }
}