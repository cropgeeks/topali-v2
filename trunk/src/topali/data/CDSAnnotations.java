package topali.data;

public class CDSAnnotations extends RegionAnnotations{

	public CDSAnnotations() {
		label = "Coding Regions";
	}
	
	@Override
	protected AnnotationElement create(int position) {
		return new AnnotationElement(AnnotationElement.CODINGREG, position);
	}

}