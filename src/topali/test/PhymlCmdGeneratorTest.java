// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.test;

import topali.cluster.PhyMLCmdGenerator;
import topali.cluster.jobs.phyml.PhymlMonitor;
import topali.data.models.*;

public class PhymlCmdGeneratorTest
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		Model mod = ModelManager.getInstance().generateModel("wag", true, true);
		String cmd = PhyMLCmdGenerator.getModelCmd("seq2", mod, true, true, null);
		System.out.println(cmd);
	}

}
