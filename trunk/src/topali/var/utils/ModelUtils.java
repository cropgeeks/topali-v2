// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var.utils;

import java.util.List;

import org.apache.log4j.Logger;

import topali.data.models.Model;

public class ModelUtils
{

	static Logger log = Logger.getLogger(ModelUtils.class);
	
	public static final int CRIT_LNL = 1;
	public static final int CRIT_AIC1 = 2;
	public static final int CRIT_AIC2 = 3;
	public static final int CRIT_BIC = 4;
	
	public static Model getBestModel(List<Model> models, int criteria) {
		if(models.size()>0) {
			
			Model result = models.get(0);
			for(int i=0; i<models.size(); i++) {
				Model mod = models.get(i);
				
				switch(criteria) {
				case CRIT_LNL:
					if(mod.getLnl()>result.getLnl())
						result = mod;
					break;
				case CRIT_AIC1:
					if(mod.getAic1()<result.getAic1())
						result = mod;
					break;
				case CRIT_AIC2:
					if(mod.getAic2()<result.getAic2())
						result = mod;
					break;
				case CRIT_BIC:
					if(mod.getBic()<result.getBic())
						result = mod;
					break;
				}
				
			}
			return result;
			
		}
		
		return null;
	}
	
	public static Model getModel(String name, boolean gamma, boolean inv,
			List<Model> models)
	{
		for (Model m : models)
		{
			if (m.is(name) && m.isGamma() == gamma && m.isInv() == inv)
				return m;
		}
		return null;
	}
	
	public static double lrt(Model mod1, Model mod2)
	{
		double lr = MathUtils.calcLR(mod1.getLnl(), mod2.getLnl());
		
		int fp1 = mod1.getFreeParameters();
		if(mod1.isGamma())
			fp1++;
		if(mod1.isInv())
			fp1++;
		
		int fp2 = mod2.getFreeParameters();
		if(mod2.isGamma())
			fp2++;
		if(mod2.isInv())
			fp2++;
		
		int df = (fp1>fp2) ? (fp1-fp2) : (fp2-fp1);

		double lrt = MathUtils.calcLRT(lr, df);
		return lrt;
	}

	/**
	 * Can be performed on model[+I][+G]
	 * Model range: all models
	 * @param models
	 * @param alpha
	 * @return
	 */
	public static Model perform5HTHLRT(List<Model> models, double alpha) {
		try
		{
			Model JC = getModel("jc", false, false, models);
			Model F81 = getModel("f81", false, false, models);
			Model HKY = getModel("hky", false, false, models);
			Model TRN = getModel("trn", false, false, models);
			Model K81uf = getModel("k81uf", false, false, models);
			Model TVM = getModel("tvm", false, false, models);
			
			int[] jcCrit = new int[]    {0,0,0};
			int[] f81Crit = new int[]   {1,0,0};
			int[] k80Crit = new int[]   {0,1,0,0};
			int[] hkyCrit = new int[]   {1,1,0,0};
			int[] trnefCrit = new int[] {0,1,1,0};
			int[] trnCrit = new int[]   {1,1,1,0};
			int[] k81Crit = new int[]   {0,1,0,1,0};
			int[] k81ufCrit = new int[] {1,1,0,1,0};
			int[] timefCrit = new int[] {0,1,1,1,0};
			int[] timCrit = new int[]   {1,1,1,1,0};
			int[] tvmefCrit = new int[] {0,1,0,1,1};
			int[] tvmCrit = new int[]   {1,1,0,1,1};
			int[] symCrit = new int[]   {0,1,1,1,1};
			int[] gtrCrit = new int[]   {1,1,1,1,1};
			
			int[] testResult = new int[5];
			
			//#1, JC vs F81 (Base composition uniform?)
			testResult[0] = (lrt(JC, F81) < alpha) ? 1 : 0;
			
			//#2 F81 vs HKY (All rates uniform or Ti and Tv different?)
			testResult[1] = (lrt(F81, HKY) < alpha) ? 1 : 0;
			
			//#3 HKY vs TrN (Ti uniform or two different rates Ti(T-C) and Ti(A-G)?)
			testResult[2] = (lrt(HKY, TRN) < alpha) ? 1 : 0;
			
			//#4 HKY vs K81uf (Tv uniform or two different rates Tv(A-C,G-T) and Tv(A-T,C-G)?)
			testResult[3] = (lrt(HKY, K81uf) < alpha) ? 1 : 0;
			
			//#5 K81uf vs TVM (two rates for Tv or 4 rates for Tv?)
			testResult[4]= (lrt(K81uf, TVM) < alpha) ? 1 : 0;
			
			Model model = null;
			if(matches(testResult, jcCrit))
				model = getModel("jc", false, false, models);
			else if(matches(testResult, f81Crit))
				model = getModel("f81", false, false, models);
			else if(matches(testResult, k80Crit))
				model = getModel("k80", false, false, models);
			else if(matches(testResult, hkyCrit))
				model = getModel("hky", false, false, models);
			else if(matches(testResult, trnefCrit))
				model = getModel("trnef", false, false, models);
			else if(matches(testResult, trnCrit))
				model = getModel("trn", false, false, models);
			else if(matches(testResult, k81Crit))
				model = getModel("k81", false, false, models);
			else if(matches(testResult, k81ufCrit))
				model = getModel("k81uf", false, false, models);
			else if(matches(testResult, timefCrit))
				model = getModel("timef", false, false, models);
			else if(matches(testResult, timCrit))
				model = getModel("tim", false, false, models);
			else if(matches(testResult, tvmefCrit))
				model = getModel("tvmef", false, false, models);
			else if(matches(testResult, tvmCrit))
				model = getModel("tvm", false, false, models);
			else if(matches(testResult, symCrit))
				model = getModel("sym", false, false, models);
			else if(matches(testResult, gtrCrit))
				model = getModel("gtr", false, false, models);
			
			//finally test for rate heterogenicity and inv. sites
			Model modelI = getModel(model.getName(), false, true, models);
			Model modelG = getModel(model.getName(), true, false, models);
			Model modelIG = getModel(model.getName(), true, true, models);
			
			Model result = model;
			if(modelG!=null && lrt(model,modelG)<alpha) {
				if(modelI!=null && lrt(modelG,modelIG)<alpha)
					result = modelIG;
				else
					result = modelG;
			}
			else if(modelI!=null && lrt(model,modelI)<alpha) {
				if(modelG!=null && lrt(modelI,modelIG)<alpha)
					result = modelIG;
				else
					result = modelI;
			}
			
			return result;
			
		} catch (RuntimeException e)
		{
			log.warn("Could not perform 5ht lrt test!", e);
			return null;
		}
	}
	
	/**
	 * Can be performed on model[+I][+G]
	 * Model range: MrBayes
	 * @param models
	 * @param alpha
	 * @return
	 */
	public static Model performHLRT1(List<Model> models, double alpha)
	{
		try
		{
			Model JC = getModel("jc", false, false, models);
			Model JCI = getModel("jc", false, true, models);
			Model JCG = getModel("jc", true, false, models);
			Model JCIG = getModel("jc", true, true, models);

			Model F81 = getModel("f81", false, false, models);
			Model F81I = getModel("f81", false, true, models);
			Model F81G = getModel("f81", true, false, models);
			Model F81IG = getModel("f81", true, true, models);

			Model HKY = getModel("hky", false, false, models);
			Model HKYI = getModel("hky", false, true, models);
			Model HKYG = getModel("hky", true, false, models);
			Model HKYIG = getModel("hky", true, true, models);

			Model GTR = getModel("gtr", false, false, models);
			Model GTRI = getModel("gtr", false, true, models);
			Model GTRG = getModel("gtr", true, false, models);
			Model GTRIG = getModel("gtr", true, true, models);

			Model SYM = getModel("sym", false, false, models);
			Model SYMI = getModel("sym", false, true, models);
			Model SYMG = getModel("sym", true, false, models);
			Model SYMIG = getModel("sym", true, true, models);

			Model K80 = getModel("k80", false, false, models);
			Model K80I = getModel("k80", false, true, models);
			Model K80G = getModel("k80", true, false, models);
			Model K80IG = getModel("k80", true, true, models);

			boolean i = JCI != null;
			boolean g = JCG !=null;
			
			if (lrt(JC, F81) < alpha) /* 1,2 */
			{
				if (lrt(F81, HKY) < alpha) /* 3,4 */
				{
					if (lrt(HKY, GTR) < alpha) /* 5,6 */
					{
						if (g && lrt(GTR, GTRG) < alpha) /* 7, 8 */
						{
							if (i && lrt(GTRG, GTRIG) < alpha) /* 9,10 */
								return GTRIG; /* 12 */
							else
								return GTRG; /* 11 */
						} else
						{
							if (i && lrt(GTR, GTRI) < alpha) /* 13 14, */
								return GTRI; /* 16 */
							else
								return GTR; /* 15 */
						}
					} else
					{
						if (g && lrt(HKY, HKYG) < alpha) /* 17 , 18 */
						{
							if (i && lrt(HKYG, HKYIG) < alpha) /* 19 , 20 */
								return HKYIG; /* 22 */
							else
								return HKYG; /* 21 */
						} else
						{
							if (i && lrt(HKY, HKYI) < alpha) /* 23, 24 */
								return HKYI; /* 26 */
							else
								return HKY; /* 25 */
						}
					}
				} else
				{
					if (g && lrt(F81, F81G) < alpha) /* 27 , 28 */
					{
						if (i && lrt(F81G, F81IG) < alpha) /* 29 , 30 */
							return F81IG; /* 32 */
						else
							return F81G; /* 31 */
					} else
					{
						if (i && lrt(F81, F81I) < alpha) /* 33 , 34 */
							return F81I; /* 36 */
						else
							return F81; /* 35 */
					}
				}
			} else
			{
				if (lrt(JC, K80) < alpha) /* 37 , 38 */
				{
					if (lrt(K80, SYM) < alpha) /* 39 , 40 */
					{
						if (g && lrt(SYM, SYMG) < alpha) /* 41 , 42 */
						{
							if (i && lrt(SYMG, SYMIG) < alpha) /* 43, 44 */
								return SYMIG; /* 46 */
							else
								return SYMG; /* 45 */
						} else
						{
							if (i && lrt(SYM, SYMI) < alpha) /* 47 , 48 */
								return SYMI; /* 50 */
							else
								return SYM; /* 49 */
						}
					} else
					{
						if (g && lrt(K80, K80G) < alpha) /* 51, 52 */
						{
							if (i && lrt(K80G, K80IG) < alpha) /* 53 , 54 */
								return K80IG; /* 56 */
							else
								return K80G; /* 55 */
						} else
						{
							if (i && lrt(K80, K80I) < alpha) /* 57, 58 */
								return K80I; /* 60 */
							else
								return K80; /* 59 */
						}
					}
				} else
				{
					if (g && lrt(JC, JCG) < alpha) /* 61 , 62 */
					{
						if (i && lrt(JCG, JCIG) < alpha) /* 63 , 64 */
							return JCIG; /* 66 */
						else
							return JCG; /* 65 */
					} else
					{
						if (i && lrt(JC, JCI) < alpha) /* 67, 68 */
							return JCI; /* 70 */
						else
							return JC; /* 69 */
					}
				}
			}
		} catch (RuntimeException e)
		{
			log.warn("Could not perform hrlt1 test!", e);
			return null;
		}
	}

	/**
	 * Can be performed on model+I+G
	 * Model range: MrBayes
	 * @param models
	 * @param alpha
	 * @return
	 */
	public static Model performHLRT2(List<Model> models, double alpha)
	{
		try
		{
			Model JC = getModel("jc", false, false, models);
			Model JCI = getModel("jc", false, true, models);
			Model JCG = getModel("jc", true, false, models);
			Model JCIG = getModel("jc", true, true, models);

			Model F81 = getModel("f81", false, false, models);
			Model F81I = getModel("f81", false, true, models);
			Model F81G = getModel("f81", true, false, models);
			Model F81IG = getModel("f81", true, true, models);

			Model HKY = getModel("hky", false, false, models);
			Model HKYI = getModel("hky", false, true, models);
			Model HKYG = getModel("hky", true, false, models);
			Model HKYIG = getModel("hky", true, true, models);

			Model GTR = getModel("gtr", false, false, models);
			Model GTRI = getModel("gtr", false, true, models);
			Model GTRG = getModel("gtr", true, false, models);
			Model GTRIG = getModel("gtr", true, true, models);

			Model SYM = getModel("sym", false, false, models);
			Model SYMI = getModel("sym", false, true, models);
			Model SYMG = getModel("sym", true, false, models);
			Model SYMIG = getModel("sym", true, true, models);

			Model K80 = getModel("k80", false, false, models);
			Model K80I = getModel("k80", false, true, models);
			Model K80G = getModel("k80", true, false, models);
			Model K80IG = getModel("k80", true, true, models);

			boolean i = JCI != null;
			boolean g = JCG !=null;
			if(!i || !g)
				return null;
			
			if (lrt(SYMIG, GTRIG) < alpha)/* A */
			{
				if (lrt(HKYIG, GTRIG) < alpha) /* B */
				{
					if (lrt(GTRI, GTRIG) < alpha) /* C */
					{
						if (lrt(GTRG, GTRIG) < alpha) /* D */
							return GTRIG;
						else
							return GTRG;
					} else
					{
						if (lrt(GTR, GTRI) < alpha) /* E */
							return GTRI;
						else
							return GTR;
					}
				} else
				{
					if (lrt(F81IG, HKYIG) < alpha) /* F */
					{
						if (lrt(HKYI, HKYIG) < alpha) /* G */
						{
							if (lrt(HKYG, HKYIG) < alpha) /* H */
								return HKYIG;
							else
								return HKYG;
						} else
						{
							if (lrt(HKY, HKYI) < alpha) /* I */
								return HKYI;
							else
								return HKY;
						}
					} else
					{
						if (lrt(F81I, F81IG) < alpha) /* J */
						{
							if (lrt(F81G, F81IG) < alpha) /* K */
								return F81IG;
							else
								return F81G;
						} else
						{
							if (lrt(F81, F81I) < alpha) /* L */
								return F81I;
							else
								return F81;
						}
					}
				}
			} else
			{
				if (lrt(K80IG, SYMIG) < alpha) /* M */
				{
					if (lrt(SYMI, SYMIG) < alpha) /* N */
					{
						if (lrt(SYMG, SYMIG) < alpha) /* O */
							return SYMIG;
						else
							return SYMG;
					} else
					{
						if (lrt(SYM, SYMI) < alpha) /* P */
							return SYMI;
						else
							return SYM;
					}
				} else
				{
					if (lrt(JCIG, K80IG) < alpha) /* Q */
					{
						if (lrt(K80I, K80IG) < alpha) /* R */
						{
							if (lrt(K80G, K80IG) < alpha) /* S */
								return K80IG;
							else
								return K80G;
						} else
						{
							if (lrt(K80, K80I) < alpha) /* T */
								return K80I;
							else
								return K80;
						}
					} else
					{
						if (lrt(JCI, JCIG) < alpha) /* U */
						{
							if (lrt(JCG, JCIG) < alpha) /* V */
								return JCIG;
							else
								return JCG;
						} else
						{
							if (lrt(JC, JCI) < alpha) /* X */
								return JCI;
							else
								return JC;
						}
					}
				}
			}
		} catch (RuntimeException e)
		{
			log.warn("Could not perform hrlt2 test!", e);
			return null;
		}
	}

	/**
	 * Can be performed on model[+I][+G]
	 * Model range: MrBayes
	 * @param models
	 * @param alpha
	 * @return
	 */
	public static Model performHLRT3(List<Model> models, double alpha)
	{
		try
		{
			Model JC = getModel("jc", false, false, models);
			Model JCI = getModel("jc", false, true, models);
			Model JCG = getModel("jc", true, false, models);
			Model JCIG = getModel("jc", true, true, models);

			Model F81 = getModel("f81", false, false, models);
			Model F81I = getModel("f81", false, true, models);
			Model F81G = getModel("f81", true, false, models);
			Model F81IG = getModel("f81", true, true, models);

			Model HKY = getModel("hky", false, false, models);
			Model HKYI = getModel("hky", false, true, models);
			Model HKYG = getModel("hky", true, false, models);
			Model HKYIG = getModel("hky", true, true, models);

			Model GTR = getModel("gtr", false, false, models);
			Model GTRI = getModel("gtr", false, true, models);
			Model GTRG = getModel("gtr", true, false, models);
			Model GTRIG = getModel("gtr", true, true, models);

			Model SYM = getModel("sym", false, false, models);
			Model SYMI = getModel("sym", false, true, models);
			Model SYMG = getModel("sym", true, false, models);
			Model SYMIG = getModel("sym", true, true, models);

			Model K80 = getModel("k80", false, false, models);
			Model K80I = getModel("k80", false, true, models);
			Model K80G = getModel("k80", true, false, models);
			Model K80IG = getModel("k80", true, true, models);

			boolean i = JCI != null;
			boolean g = JCG !=null;
			
			if (g && lrt(JC, JCG) < alpha) /* A */
			{
				if (i && lrt(JCG, JCIG) < alpha) /* B */
				{
					if (lrt(JCIG, K80IG) < alpha) /* C */
					{
						if (lrt(K80IG, SYMIG) < alpha) /* D */
						{
							if (lrt(SYMIG, GTRIG) < alpha) /* E */
								return GTRIG;
							else
								return SYMIG;
						} else
						{
							if (lrt(K80IG, HKYIG) < alpha) /* F */
								return HKYIG;
							else
								return K80IG;
						}
					} else
					{
						if (lrt(JCIG, F81IG) < alpha) /* G */
							return F81IG;
						else
							return JCIG;
					}
				} else
				{
					if (lrt(JCG, K80G) < alpha) /* H */
					{
						if (lrt(K80G, SYMG) < alpha) /* I */
						{
							if (lrt(SYMG, GTRG) < alpha) /* J */
								return GTRG;
							else
								return SYMG;
						} else
						{
							if (lrt(K80G, HKYG) < alpha) /* K */
								return HKYG;
							else
								return K80G;
						}
					} else
					{
						if (lrt(JCG, F81G) < alpha) /* L */
							return F81G;
						else
							return JCG;
					}
				}
			} else
			{
				if (i && lrt(JC, JCI) < alpha) /* M */
				{
					if (lrt(JCI, K80I) < alpha) /* N */
					{
						if (lrt(K80I, SYMI) < alpha) /* O */
						{
							if (lrt(SYMI, GTRI) < alpha) /* P */
								return GTRI;
							else
								return SYMI;
						} else
						{
							if (lrt(K80I, HKYI) < alpha) /* Q */
								return HKYI;
							else
								return K80I;
						}
					} else
					{
						if (lrt(JCI, F81I) < alpha) /* R */
							return F81I;
						else
							return JCI;
					}
				} else
				{
					if (lrt(JC, K80) < alpha) /* S */
					{
						if (lrt(K80, SYM) < alpha) /* T */
						{
							if (lrt(SYM, GTR) < alpha) /* U */
								return GTR;
							else
								return SYM;
						} else
						{
							if (lrt(K80, HKY) < alpha) /* V */
								return HKY;
							else
								return K80;
						}
					} else
					{
						if (lrt(JC, F81) < alpha) /* X */
							return F81;
						else
							return JC;
					}
				}
			}
		} catch (RuntimeException e)
		{
			log.warn("Could not perform hrlt3 test!", e);
			return null;
		}
	}

	/**
	 * Can be performed on model[+I][+G]
	 * Model range: MrBayes
	 * @param models
	 * @param alpha
	 * @return
	 */
	public static Model performHLRT4(List<Model> models, double alpha)
	{
		try
		{
			Model JC = getModel("jc", false, false, models);
			Model JCI = getModel("jc", false, true, models);
			Model JCG = getModel("jc", true, false, models);
			Model JCIG = getModel("jc", true, true, models);

			Model F81 = getModel("f81", false, false, models);
			Model F81I = getModel("f81", false, true, models);
			Model F81G = getModel("f81", true, false, models);
			Model F81IG = getModel("f81", true, true, models);

			Model HKY = getModel("hky", false, false, models);
			Model HKYI = getModel("hky", false, true, models);
			Model HKYG = getModel("hky", true, false, models);
			Model HKYIG = getModel("hky", true, true, models);

			Model GTR = getModel("gtr", false, false, models);
			Model GTRI = getModel("gtr", false, true, models);
			Model GTRG = getModel("gtr", true, false, models);
			Model GTRIG = getModel("gtr", true, true, models);

			Model SYM = getModel("sym", false, false, models);
			Model SYMI = getModel("sym", false, true, models);
			Model SYMG = getModel("sym", true, false, models);
			Model SYMIG = getModel("sym", true, true, models);

			Model K80 = getModel("k80", false, false, models);
			Model K80I = getModel("k80", false, true, models);
			Model K80G = getModel("k80", true, false, models);
			Model K80IG = getModel("k80", true, true, models);

			boolean i = JCI != null;
			boolean g = JCG !=null;
			
			if (i && g && lrt(GTRI, GTRIG) < alpha) /* A */
			{
				if (lrt(GTRG, GTRIG) < alpha) /* B */
				{
					if (lrt(HKYIG, GTRIG) < alpha) /* C */
					{
						if (lrt(SYMIG, GTRIG) < alpha) /* D */
							return GTRIG;
						else
							return SYMIG;
					} else
					{
						if (lrt(F81IG, HKYIG) < alpha) /* E */
						{
							if (lrt(K80IG, HKYIG) < alpha) /* F */
								return HKYIG;
							else
								return K80IG;
						} else
						{
							if (lrt(JCIG, F81IG) < alpha) /* G */
								return F81IG;
							else
								return JCIG;
						}
					}
				} else
				{
					if (lrt(HKYG, GTRG) < alpha) /* H */
					{
						if (lrt(SYMG, GTRG) < alpha) /* I */
							return GTRG;
						else
							return SYMG;
					} else
					{
						if (lrt(F81G, HKYG) < alpha) /* J */
						{
							if (lrt(K80G, HKYG) < alpha) /* K */
								return HKYG;
							else
								return K80G;
						} else
						{
							if (lrt(JCG, F81G) < alpha) /* L */
								return F81G;
							else
								return JCG;
						}
					}
				}
			} else
			{
				if (i && lrt(GTR, GTRI) < alpha) /* M */
				{
					if (lrt(HKYI, GTRI) < alpha) /* N */
					{
						if (lrt(SYMI, GTRI) < alpha) /* O */
							return GTRI;
						else
							return SYMI;
					} else
					{
						if (lrt(F81I, HKYI) < alpha) /* P */
						{
							if (lrt(K80I, HKYI) < alpha) /* Q */
								return HKYI;
							else
								return K80I;
						} else
						{
							if (lrt(JCI, F81I) < alpha) /* R */
								return F81I;
							else
								return JCI;
						}
					}
				} else
				{
					if (lrt(HKY, GTR) < alpha) /* S */
					{
						if (lrt(SYM, GTR) < alpha) /* T */
							return GTR;
						else
							return SYM;
					} else
					{
						if (lrt(F81, HKY) < alpha) /* U */
						{
							if (lrt(K80, HKY) < alpha) /* V */
								return HKY;
							else
								return K80;
						} else
						{
							if (lrt(JC, F81) < alpha) /* X */
								return F81;
							else
								return JC;
						}
					}
				}
			}
		} catch (RuntimeException e)
		{
			log.warn("Could not perform hrlt1 test!", e);
			return null;
		}
	}
	
	private static boolean matches(int[] pattern, int[] obj) {
		for(int i=0; i<obj.length; i++) {
			if(pattern[i]!=obj[i])
				return false;
		}
		return true;
	}
}
