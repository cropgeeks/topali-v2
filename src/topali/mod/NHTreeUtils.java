// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.mod;

public class NHTreeUtils
{

	public static String removeBranchLengths(String tree) {
		String result = tree.replaceAll(":\\d+.\\d+", "");
		return result;
	}
	
//	public static String treeview2ATV(String tree) {
//		//(((U95378Sus:0.043288,U13680Hom:0.0537653):0.0502906,(AF070995C:0.0824677,(X04752Mus:0.044759,U07177Rat:0.0578752):0.0477362):0.0368567):0.0404227,((X53828OG1:0.083491,U28410OG2:0.0877002):0.0436926,((NM017025R:0.0383638,U13687Mus:0.032364):0.0470628[&&NHX:B=0.1397],(M22585rab:0.0634783,(X02152Hom:0.0463035,U07178Sus:0.0440972):0.0120578):0.0112219):0.02754):0.051498)
//		//(((((X04752Mus#1 #0.2514 : 0.024074, U07177Rat#1 #0.2514 : 0.037689) #0.0940 : 0.029344, AF070995C#1 #0.2514 : 0.033689) #0.0940 : 0.025941, (U95378Sus#1 #0.2514 : 0.027733, U13680Hom#1 #0.2514 : 0.030799)#1 #0.2514 : 0.028543)#1 #0.2514 : 0.103545, (((U07178Sus #0.0940 : 0.013747, X02152Hom #0.0940 : 0.015731) #0.0940 : 0.005406, M22585rab #0.0940 : 0.024427) #0.0940 : 0.001732, (U13687Mus #0.0940 : 0.009570, NM017025R #0.0940 : 0.012602) #0.0940 : 0.023654) #0.0940 : 0.015055) #0.0940 : 0.011475, (X53828OG1 #0.0940 : 0.028085, U28410OG2 #0.0940 : 0.035188) #0.0940 : 0.020382);
//		StringBuffer sb = new StringBuffer();
//		for(char c: tree.toCharArray()) {
//			
//		}
//	}
}
