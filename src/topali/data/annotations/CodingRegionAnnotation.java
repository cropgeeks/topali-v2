// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data.annotations;

import topali.data.models.*;

public class CodingRegionAnnotation extends Annotation
{

	Model modC1, modC2, modC3;
	
	public CodingRegionAnnotation() {
		super();
	}
	
	public CodingRegionAnnotation(int id) {
		super(id);
	}

	public CodingRegionAnnotation(int start, int end) {
		super(start, end);
	}

	public CodingRegionAnnotation(CodingRegionAnnotation anno) {
		super(anno);
		this.modC1 = ModelManager.getInstance().generateModel(anno.getModC1().getName(), anno.getModC1().isGamma(), anno.getModC1().isInv());
		this.modC2 = ModelManager.getInstance().generateModel(anno.getModC2().getName(), anno.getModC2().isGamma(), anno.getModC2().isInv()); 
		this.modC3 = ModelManager.getInstance().generateModel(anno.getModC3().getName(), anno.getModC3().isGamma(), anno.getModC3().isInv());
	}
	
	public Model getModC1() {
		return modC1;
	}

	public void setModC1(Model modC1) {
		this.modC1 = modC1;
	}

	public Model getModC2() {
		return modC2;
	}

	public void setModC2(Model modC2) {
		this.modC2 = modC2;
	}

	public Model getModC3() {
		return modC3;
	}

	public void setModC3(Model modC3) {
		this.modC3 = modC3;
	}
	
}
