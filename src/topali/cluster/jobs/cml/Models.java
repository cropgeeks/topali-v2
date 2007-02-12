// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml;

public class Models
{
	public final static int MAX = 8;

	// public final static int MODEL_M0 = 1;
	// public final static int MODEL_M1 = 2;
	// public final static int MODEL_M2 = 3;
	// public final static int MODEL_M1a = 4;
	// public final static int MODEL_M2a = 5;
	// public final static int MODEL_M3 = 6;
	// public final static int MODEL_M7 = 7;
	// public final static int MODEL_M8 = 8;

	public final static int MODEL_M0 = 1;

	public final static int MODEL_M1 = 4;

	public final static int MODEL_M2 = 5;

	public final static int MODEL_M1a = 2;

	public final static int MODEL_M2a = 3;

	public final static int MODEL_M3 = 6;

	public final static int MODEL_M7 = 7;

	public final static int MODEL_M8 = 8;

	private static String nl = System.getProperty("line.separator");

	public static String getModelName(int model)
	{
		switch (model)
		{
		case MODEL_M0:
			return "M0";
		case MODEL_M1:
			return "M1";
		case MODEL_M2:
			return "M2";
		case MODEL_M1a:
			return "M1a";
		case MODEL_M2a:
			return "M2a";
		case MODEL_M3:
			return "M3";
		case MODEL_M7:
			return "M7";
		case MODEL_M8:
			return "M8";
		}

		return new String();
	}

	public static String getFullModelName(int model)
	{
		switch (model)
		{
		case MODEL_M0:
			return "M0 (one-ratio)";
		case MODEL_M1:
			return "M1 (NearlyNeutral)";
		case MODEL_M2:
			return "M2 (PositiveSelection)";
		case MODEL_M1a:
			return "M1a (NearlyNeutral)";
		case MODEL_M2a:
			return "M2a (PositiveSelection)";
		case MODEL_M3:
			return "M3 (discrete (3 categories))";
		case MODEL_M7:
			return "M7 (beta (10 categories))";
		case MODEL_M8:
			return "M8 (beta&w>1 (11 categories))";
		}

		return new String();
	}

	public static int getNParameters(int model)
	{
		switch (model)
		{
		case MODEL_M0:
			return 1;
		case MODEL_M1:
			return 1;
		case MODEL_M2:
			return 3;
		case MODEL_M1a:
			return 2;
		case MODEL_M2a:
			return 4;
		case MODEL_M3:
			return 5;
		case MODEL_M7:
			return 2;
		case MODEL_M8:
			return 4;
		}
		return -1;
	}

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
		case MODEL_M0:
			return getModel_M0(settings);
		case MODEL_M1:
			return getModel_M1(settings);
		case MODEL_M2:
			return getModel_M2(settings);
		case MODEL_M1a:
			return getModel_M1a(settings);
		case MODEL_M2a:
			return getModel_M2a(settings);
		case MODEL_M3:
			return getModel_M3(settings);
		case MODEL_M7:
			return getModel_M7(settings);
		case MODEL_M8:
			return getModel_M8(settings);
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