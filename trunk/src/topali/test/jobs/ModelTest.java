// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.test.jobs;

import java.io.File;
import java.util.*;

import topali.cluster.JobStatus;
import topali.cluster.jobs.ModelTestLocalJob;
import topali.cluster.jobs.modeltest.ModelTestMonitor;
import topali.data.*;
import topali.data.models.*;
import topali.var.utils.ModelUtils;

import junit.framework.TestCase;

public class ModelTest extends TestCase
{

	double confInt = 1d;
	
	AlignmentData data;
	ModelTestResult res;
	ArrayList<Model> expected;
	
	public void test() throws Exception {
		ModelTestLocalJob job = new ModelTestLocalJob(res, data);
		job.ws_submitJob();
		
		Thread.sleep(10000);
		
		while(true) {
			JobStatus st = (new ModelTestMonitor(job.jobDir)).getPercentageComplete();
			System.out.println(st.progress);
			Thread.sleep(10000);
			if(st.progress>=100)
				break;
		}
		
		List<Model> result = (new ModelTestMonitor(job.jobDir)).getResult().models;
		for(Model ex : expected) {
			//ignore these models...
			if(ex.is("trn") || ex.is("hky") || ex.is("gtr"))
				continue;
			Model mod = ModelUtils.getModel(ex.getName(), ex.isGamma(), ex.isInv(), result);
			boolean ok = (mod.getLnl()<(ex.getLnl()+confInt)) && (mod.getLnl()>(ex.getLnl()-confInt));
			assertTrue("exp:\n"+ex+"\n is:\n"+mod+"\n",ok);
		}
	}
	
	protected void setUp() throws Exception
	{
		setUpExpected();
		setUpResult();
	}

	private void setUpResult() throws Exception {
		
		File in = new File(this.getClass().getResource("/res/testing/test.fasta").toURI());
		SequenceSet ss = new SequenceSet(in);
		
		int[] indeces = new int[ss.getSize()];
		for(int i=0; i<indeces.length; i++)
			indeces[i] = i;
		ss.setSelectedSequences(indeces);
		
		data = new AlignmentData("test", ss);
		res = new ModelTestResult();
		res.isRemote = false;
		res.selectedSeqs = ss.getSelectedSequenceSafeNames();
		
		ArrayList<Model> models = new ArrayList<Model>();
		models.add(ModelManager.getInstance().generateModel("gtr", false, false));
		models.add(ModelManager.getInstance().generateModel("gtr", false, true));
		models.add(ModelManager.getInstance().generateModel("gtr", true, false));
		models.add(ModelManager.getInstance().generateModel("gtr", true, true));
		models.add(ModelManager.getInstance().generateModel("sym", false, false));
		models.add(ModelManager.getInstance().generateModel("sym", false, true));
		models.add(ModelManager.getInstance().generateModel("sym", true, false));
		models.add(ModelManager.getInstance().generateModel("sym", true, true));
		models.add(ModelManager.getInstance().generateModel("tvmef", false, false));
		models.add(ModelManager.getInstance().generateModel("tvmef", false, true));
		models.add(ModelManager.getInstance().generateModel("tvmef", true, false));
		models.add(ModelManager.getInstance().generateModel("tvmef", true, true));
		models.add(ModelManager.getInstance().generateModel("trn", false, false));
		models.add(ModelManager.getInstance().generateModel("trn", false, true));
		models.add(ModelManager.getInstance().generateModel("trn", true, false));
		models.add(ModelManager.getInstance().generateModel("trn", true, true));
		models.add(ModelManager.getInstance().generateModel("tvm", false, false));
		models.add(ModelManager.getInstance().generateModel("tvm", false, true));
		models.add(ModelManager.getInstance().generateModel("tvm", true, false));
		models.add(ModelManager.getInstance().generateModel("tvm", true, true));
		models.add(ModelManager.getInstance().generateModel("trnef", false, false));
		models.add(ModelManager.getInstance().generateModel("trnef", false, true));
		models.add(ModelManager.getInstance().generateModel("trnef", true, false));
		models.add(ModelManager.getInstance().generateModel("trnef", true, true));
		models.add(ModelManager.getInstance().generateModel("timef", false, false));
		models.add(ModelManager.getInstance().generateModel("timef", false, true));
		models.add(ModelManager.getInstance().generateModel("timef", true, false));
		models.add(ModelManager.getInstance().generateModel("timef", true, true));
		models.add(ModelManager.getInstance().generateModel("k80", false, false));
		models.add(ModelManager.getInstance().generateModel("k80", false, true));
		models.add(ModelManager.getInstance().generateModel("k80", true, false));
		models.add(ModelManager.getInstance().generateModel("k80", true, true));
		models.add(ModelManager.getInstance().generateModel("k81", false, false));
		models.add(ModelManager.getInstance().generateModel("k81", false, true));
		models.add(ModelManager.getInstance().generateModel("k81", true, false));
		models.add(ModelManager.getInstance().generateModel("k81", true, true));
		models.add(ModelManager.getInstance().generateModel("hky", false, false));
		models.add(ModelManager.getInstance().generateModel("hky", false, true));
		models.add(ModelManager.getInstance().generateModel("hky", true, false));
		models.add(ModelManager.getInstance().generateModel("hky", true, true));
		models.add(ModelManager.getInstance().generateModel("tim", false, false));
		models.add(ModelManager.getInstance().generateModel("tim", false, true));
		models.add(ModelManager.getInstance().generateModel("tim", true, false));
		models.add(ModelManager.getInstance().generateModel("tim", true, true));
		models.add(ModelManager.getInstance().generateModel("k81uf", false, false));
		models.add(ModelManager.getInstance().generateModel("k81uf", false, true));
		models.add(ModelManager.getInstance().generateModel("k81uf", true, false));
		models.add(ModelManager.getInstance().generateModel("k81uf", true, true));
		models.add(ModelManager.getInstance().generateModel("jc", false, false));
		models.add(ModelManager.getInstance().generateModel("jc", false, true));
		models.add(ModelManager.getInstance().generateModel("jc", true, false));
		models.add(ModelManager.getInstance().generateModel("jc", true, true));
		models.add(ModelManager.getInstance().generateModel("f81", false, false));
		models.add(ModelManager.getInstance().generateModel("f81", false, true));
		models.add(ModelManager.getInstance().generateModel("f81", true, false));
		models.add(ModelManager.getInstance().generateModel("f81", true, true));
		
		res.models = models;
	}
	
	private void setUpExpected() {
		expected = new ArrayList<Model>();
		
		//Results taken from Johan Nylander's MrAIC
		Model gtr = ModelManager.getInstance().generateModel("gtr", false, false);
		gtr.setLnl(-6469.0465);
		expected.add(gtr);
		Model gtri = ModelManager.getInstance().generateModel("gtr", false, true);
		gtri.setLnl(-6260.9977);
		expected.add(gtri);
		Model gtrg = ModelManager.getInstance().generateModel("gtr", true, false);
		gtrg.setLnl(-6238.9710);
		expected.add(gtrg);
		Model gtrig = ModelManager.getInstance().generateModel("gtr", true, true);
		gtrig.setLnl(-6238.0539);
		expected.add(gtrig);
		
		Model sym = ModelManager.getInstance().generateModel("sym", false, false);
		sym.setLnl(-6479.6306);
		expected.add(sym);
		Model symi = ModelManager.getInstance().generateModel("sym", false, true);
		symi.setLnl(-6263.4095);
		expected.add(symi);
		Model symg = ModelManager.getInstance().generateModel("sym", true, false);
		symg.setLnl(-6244.8607);
		expected.add(symg);
		Model symig = ModelManager.getInstance().generateModel("sym", true, true);
		symig.setLnl(-6243.2760);
		expected.add(symig);
		
		Model tvmef = ModelManager.getInstance().generateModel("tvmef", false, false);
		tvmef.setLnl(-6488.5010);
		expected.add(tvmef);
		Model tvmefi = ModelManager.getInstance().generateModel("tvmef", false, true);
		tvmefi.setLnl(-6264.8900);
		expected.add(tvmefi);
		Model tvmefg = ModelManager.getInstance().generateModel("tvmef", true, false);
		tvmefg.setLnl(-6248.0454);
		expected.add(tvmefg);
		Model tvmefig = ModelManager.getInstance().generateModel("tvmef", true, true);
		tvmefig.setLnl(-6246.0017);
		expected.add(tvmefig);
		
		Model trn = ModelManager.getInstance().generateModel("trn", false, false);
		trn.setLnl(-6486.1026);
		expected.add(trn);
		Model trni = ModelManager.getInstance().generateModel("trn", false, true);
		trni.setLnl(-6268.7059);
		expected.add(trni);
		Model trng = ModelManager.getInstance().generateModel("trn", true, false);
		trng.setLnl(-6251.1155);
		expected.add(trng);
		Model trnig = ModelManager.getInstance().generateModel("trn", true, true);
		trnig.setLnl(-6249.6572);
		expected.add(trnig);
		
		Model tvm = ModelManager.getInstance().generateModel("tvm", false, false);
		tvm.setLnl(-6488.5409);
		expected.add(tvm);
		Model tvmi = ModelManager.getInstance().generateModel("tvm", false, true);
		tvmi.setLnl(-6269.5434);
		expected.add(tvmi);
		Model tvmg = ModelManager.getInstance().generateModel("tvm", true, false);
		tvmg.setLnl(-6249.7503);
		expected.add(tvmg);
		Model tvmig = ModelManager.getInstance().generateModel("tvm", true, true);
		tvmig.setLnl(-6248.1444);
		expected.add(tvmig);
		
		Model trnef = ModelManager.getInstance().generateModel("trnef", false, false);
		trnef.setLnl(-6492.8030);
		expected.add(trnef);
		Model trnefi = ModelManager.getInstance().generateModel("trnef", false, true);
		trnefi.setLnl(-6271.5932);
		expected.add(trnefi);
		Model trnefg = ModelManager.getInstance().generateModel("trnef", true, false);
		trnefg.setLnl(-6255.3107);
		expected.add(trnefg);
		Model trnefig = ModelManager.getInstance().generateModel("trnef", true, true);
		trnefig.setLnl(-6253.4737);
		expected.add(trnefig);
		
		Model timef = ModelManager.getInstance().generateModel("timef", false, false);
		timef.setLnl(-6492.6985);
		expected.add(timef);
		Model timefi = ModelManager.getInstance().generateModel("timef", false, true);
		timefi.setLnl(-6271.4568);
		expected.add(timefi);
		Model timefg = ModelManager.getInstance().generateModel("timef", true, false);
		timefg.setLnl(-6255.0602);
		expected.add(timefg);
		Model timefig = ModelManager.getInstance().generateModel("timef", true, true);
		timefig.setLnl(-6253.2356);
		expected.add(timefig);
		
		Model k80 = ModelManager.getInstance().generateModel("k80", false, false);
		k80.setLnl(-6501.5668);
		expected.add(k80);
		Model k80i = ModelManager.getInstance().generateModel("k80", false, true);
		k80i.setLnl(-6273.0083);
		expected.add(k80i);
		Model k80g = ModelManager.getInstance().generateModel("k80", true, false);
		k80g.setLnl(-6258.3029);
		expected.add(k80g);
		Model k80ig = ModelManager.getInstance().generateModel("k80", true, true);
		k80ig.setLnl(-6255.9940);
		expected.add(k80ig);
		
		Model k81 = ModelManager.getInstance().generateModel("k81", false, false);
		k81.setLnl(-6501.4620);
		expected.add(k81);
		Model k81i = ModelManager.getInstance().generateModel("k81", false, true);
		k81i.setLnl(-6272.8698);
		expected.add(k81i);
		Model k81g = ModelManager.getInstance().generateModel("k81", true, false);
		k81g.setLnl(-6258.0520);
		expected.add(k81g);
		Model k81ig = ModelManager.getInstance().generateModel("k81", true, true);
		k81ig.setLnl(-6255.7594);
		expected.add(k81ig);
		
		Model hky = ModelManager.getInstance().generateModel("hky", false, false);
		hky.setLnl(-6499.0207);
		expected.add(hky);
		Model hkyi = ModelManager.getInstance().generateModel("hky", false, true);
		hkyi.setLnl(-6271.1664);
		expected.add(hkyi);
		Model hkyg = ModelManager.getInstance().generateModel("hky", true, false);
		hkyg.setLnl(-6255.9407);
		expected.add(hkyg);
		Model hkyig = ModelManager.getInstance().generateModel("hky", true, true);
		hkyig.setLnl(-6253.8531);
		expected.add(hkyig);
		
		Model tim = ModelManager.getInstance().generateModel("tim", false, false);
		tim.setLnl(-6488.0268);
		expected.add(tim);
		Model timi = ModelManager.getInstance().generateModel("tim", false, true);
		timi.setLnl(-6275.6074);
		expected.add(timi);
		Model timg = ModelManager.getInstance().generateModel("tim", true, false);
		timg.setLnl(-6256.0032);
		expected.add(timg);
		Model timig = ModelManager.getInstance().generateModel("tim", true, true);
		timig.setLnl(-6254.8322);
		expected.add(timig);
		
		Model k81uf = ModelManager.getInstance().generateModel("k81uf", false, false);
		k81uf.setLnl(-6507.2580);
		expected.add(k81uf);
		Model k81ufi = ModelManager.getInstance().generateModel("k81uf", false, true);
		k81ufi.setLnl(-6281.3030);
		expected.add(k81ufi);
		Model k81ufg = ModelManager.getInstance().generateModel("k81uf", true, false);
		k81ufg.setLnl(-6265.3540);
		expected.add(k81ufg);
		Model k81ufig = ModelManager.getInstance().generateModel("k81uf", true, true);
		k81ufig.setLnl(-6263.3417);
		expected.add(k81ufig);
		
		Model jc = ModelManager.getInstance().generateModel("jc", false, false);
		jc.setLnl(-6689.3075);
		expected.add(jc);
		Model jci = ModelManager.getInstance().generateModel("jc", false, true);
		jci.setLnl(-6476.1406);
		expected.add(jci);
		Model jcg = ModelManager.getInstance().generateModel("jc", true, false);
		jcg.setLnl(-6466.7073);
		expected.add(jcg);
		Model jcig = ModelManager.getInstance().generateModel("jc", true, true);
		jcig.setLnl(-6463.6314);
		expected.add(jcig);
		
		Model f81 = ModelManager.getInstance().generateModel("f81", false, false);
		f81.setLnl(-6693.8314);
		expected.add(f81);
		Model f81i = ModelManager.getInstance().generateModel("f81", false, true);
		f81i.setLnl(-6485.9242);
		expected.add(f81i);
		Model f81g = ModelManager.getInstance().generateModel("f81", true, false);
		f81g.setLnl(-6475.4819);
		expected.add(f81g);
		Model f81ig = ModelManager.getInstance().generateModel("f81", true, true);
		f81ig.setLnl(-6472.7641);
		expected.add(f81ig);
	}
}
