// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.test;

import topali.cluster.jobs.PhyMLCmdGenerator;
import topali.cluster.jobs.phyml.*;
import topali.data.models.*;

public class PhymlCmdGeneratorTest
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		Model mod = ModelManager.getInstance().generateModel("wag", true, true);
		String cmd = PhyMLCmdGenerator.getModelCmd("seq2", mod, true, true, 10, null);
		System.out.println(cmd);
	}

}
