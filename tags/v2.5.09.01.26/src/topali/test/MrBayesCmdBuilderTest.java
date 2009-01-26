// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.test;

import topali.cluster.jobs.mrbayes.MrBayesCmdBuilder;

public class MrBayesCmdBuilderTest
{

	public static void main(String[] args) {
		MrBayesCmdBuilder mrb = new MrBayesCmdBuilder(false);
		mrb.setAaModel(MrBayesCmdBuilder.AAMODEL_MIXED);
		String cmds = mrb.getCommands();
		System.out.println(cmds);
	}
}
