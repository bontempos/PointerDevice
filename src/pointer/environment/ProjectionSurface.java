/*
  
  USAGE
  PointerSurface wall = new PointerSurface().setLabel("wall01").setPhysicalWidth(60).setPhysicalHeight(60);;
  env.addSurface(wall);
 */

package pointer.environment;

import pointer.gui.GUIObject;

public class ProjectionSurface extends GUIObject<ProjectionSurface> {
	static int _id;
	static int selectedId;
	public int fillColor;
	public float physicalWidth;
	public float physicalHeight;
	boolean is3DModelFace = false;
	
	public ProjectionSurface () {
		_id = PointerEnvironment.surfaces.size();
		_gui_label = "Surface " + _id;
		_selected_id = _id;
	}
	
	public ProjectionSurface setLabel(String label) {
		_gui_label = label;
		return this;
	}
	
	public ProjectionSurface setPhysicalWidth(float w){
		physicalWidth = w;
		return this;
	}
	
	public ProjectionSurface setPhysicalHeight(float h){
		physicalHeight = h;
		return this;
	}
	
	public ProjectionSurface setFillColor(int c){
		fillColor = c;
		return this;
	}
	
	
}