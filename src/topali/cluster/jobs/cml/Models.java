package topali.cluster.jobs.cml;

public class Models
{
	private static String nl = System.getProperty("line.separator");
	
	static String getModel(int model)
	{
		StringBuffer settings = new StringBuffer();
		
		// Apply common model settings
		settings.append("seqfile   = seq.phy" + nl);
		settings.append("treefile  = tree.txt" + nl);
		settings.append("outfile   = results.txt" + nl);
		settings.append("noisy     = 9" + nl);
		settings.append("verbose   = 0" + nl);
		settings.append("runmode   = 0" + nl);
		
		switch (model)
		{
			case 1: return getModel_M0(settings);
			case 2: return getModel_M1(settings);
			case 3: return getModel_M2(settings);
			case 4: return getModel_M1a(settings);
			case 5: return getModel_M2a(settings);
			case 6: return getModel_M3(settings);
			case 7: return getModel_M7(settings);
			case 8: return getModel_M8(settings);
		}
		
		return new String();
	}
	
	private static String getModel_M0(StringBuffer settings)
	{
		settings.append(nl + "*M0" + nl);
		
		settings.append("seqtype   = 1" + nl);
		settings.append("CodonFreq = 2" + nl);
		settings.append("model     = 0" + nl);
		settings.append("icode     = 0" + nl);
		settings.append("fix_kappa = 0" + nl);
		settings.append("kappa     = 2" + nl);
		settings.append("fix_omega = 0" + nl);
		settings.append("omega     = 5" + nl);
		
		settings.append("NSsites   = 0" + nl);
		
		return settings.toString();
	}
	
	private static String getModel_M1(StringBuffer settings)
	{
		settings.append(nl + "*M1" + nl);
		
		settings.append("seqtype   = 1" + nl);
		settings.append("CodonFreq = 2" + nl);
		settings.append("model     = 0" + nl);
		settings.append("icode     = 0" + nl);
		settings.append("fix_kappa = 0" + nl);
		settings.append("kappa     = 2" + nl);
		settings.append("fix_omega = 1" + nl);
		settings.append("omega     = 1" + nl);
		
		settings.append("NSsites   = 1" + nl);
		
		return settings.toString();
	}
	
	private static String getModel_M2(StringBuffer settings)
	{
		settings.append(nl + "*M2" + nl);
		
		settings.append("seqtype   = 1" + nl);
		settings.append("CodonFreq = 2" + nl);
		settings.append("model     = 0" + nl);
		settings.append("icode     = 0" + nl);
		settings.append("fix_kappa = 0" + nl);
		settings.append("kappa     = 2" + nl);
		settings.append("fix_omega = 1" + nl);
		settings.append("omega     = 1" + nl);
		
		settings.append("NSsites   = 2" + nl);
		
		return settings.toString();
	}
	
	private static String getModel_M1a(StringBuffer settings)
	{
		settings.append(nl + "*M1a" + nl);
		
		settings.append("seqtype   = 1" + nl);
		settings.append("CodonFreq = 2" + nl);
		settings.append("model     = 0" + nl);
		settings.append("icode     = 0" + nl);
		settings.append("fix_kappa = 0" + nl);
		settings.append("kappa     = 2" + nl);
		settings.append("fix_omega = 0" + nl);
		settings.append("omega     = 5" + nl);
		
		settings.append("NSsites   = 1" + nl);
		
		return settings.toString();
	}
	
	private static String getModel_M2a(StringBuffer settings)
	{
		settings.append(nl + "*M2a" + nl);
		
		settings.append("seqtype   = 1" + nl);
		settings.append("CodonFreq = 2" + nl);
		settings.append("model     = 0" + nl);
		settings.append("icode     = 0" + nl);
		settings.append("fix_kappa = 0" + nl);
		settings.append("kappa     = 2" + nl);
		settings.append("fix_omega = 0" + nl);
		settings.append("omega     = 5" + nl);
		
		settings.append("NSsites   = 2" + nl);
		
		return settings.toString();
	}
	
	private static String getModel_M3(StringBuffer settings)
	{
		settings.append(nl + "*M3" + nl);
		
		settings.append("seqtype   = 1" + nl);
		settings.append("CodonFreq = 2" + nl);
		settings.append("model     = 0" + nl);
		settings.append("icode     = 0" + nl);
		settings.append("fix_kappa = 0" + nl);
		settings.append("kappa     = 2" + nl);
		settings.append("fix_omega = 0" + nl);
		settings.append("omega     = 5" + nl);
		
		settings.append("NSsites   = 3" + nl);
		settings.append("ncatG     = 3" + nl);
		
		return settings.toString();
	}
	
	private static String getModel_M7(StringBuffer settings)
	{
		settings.append(nl + "*M7" + nl);
		
		settings.append("seqtype   = 1" + nl);
		settings.append("CodonFreq = 2" + nl);
		settings.append("model     = 0" + nl);
		settings.append("icode     = 0" + nl);
		settings.append("fix_kappa = 0" + nl);
		settings.append("kappa     = 2" + nl);
		settings.append("fix_omega = 0" + nl);
		settings.append("omega     = 5" + nl);
		
		settings.append("NSsites   = 7" + nl);
		settings.append("ncatG     = 10" + nl);
		
		return settings.toString();
	}
	
	private static String getModel_M8(StringBuffer settings)
	{
		settings.append(nl + "*M8" + nl);
		
		settings.append("seqtype   = 1" + nl);
		settings.append("CodonFreq = 2" + nl);
		settings.append("model     = 0" + nl);
		settings.append("icode     = 0" + nl);
		settings.append("fix_kappa = 0" + nl);
		settings.append("kappa     = 2" + nl);
		settings.append("fix_omega = 1" + nl);
		settings.append("omega     = 1" + nl);
		
		settings.append("NSsites   = 8" + nl);
		settings.append("ncatG     = 10" + nl);
		
		return settings.toString();
	}
	
	public static void main(String[] args)
	{
		System.out.println("M0");
		System.out.println(getModel(1) + "\n");
		
		System.out.println("M1");
		System.out.println(getModel(2) + "\n");
		
		System.out.println("M2");
		System.out.println(getModel(3) + "\n");
		
		System.out.println("M1a");
		System.out.println(getModel(4) + "\n");
		
		System.out.println("M2a");
		System.out.println(getModel(5) + "\n");
		
		System.out.println("M3");
		System.out.println(getModel(6) + "\n");
		
		System.out.println("M7");
		System.out.println(getModel(7) + "\n");
		
		System.out.println("M8");
		System.out.println(getModel(8) + "\n");
	}
}