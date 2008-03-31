// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data.annotations;

import topali.data.models.*;


public class PartitionAnnotation extends Annotation
{

	Model model = null;
	
	public PartitionAnnotation() {
		super();
	}
	
	public PartitionAnnotation(int id) {
		super(id);
	}

	public PartitionAnnotation(int start, int end) {
		super(start, end);
	}

	public PartitionAnnotation(PartitionAnnotation anno) {
		super(anno);
		this.model = ModelManager.getInstance().generateModel(anno.getModel().getName(), anno.getModel().isGamma(), anno.getModel().isInv());
	}
	
	public Model getModel() {
		return model;
	}

	public void setModel(Model model) {
		this.model = model;
	}
	
	
}
