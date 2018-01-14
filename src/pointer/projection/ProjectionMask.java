/*
 *  A projection mask resolution is directly related to a the motor components properties.
 *  If its a servo, a mask will be always 180 x 180, in case the motor range can be fully exploited.
 */
package pointer.projection;

import pointer.components.PointerMotor;
import pointer.environment.PointerEnvironment;
import pointer.environment.PointerProjector;
import pointer.environment.ProjectionSurface;
import pointer.gui.GUIObject;
import pointer.system.PointerConstants.PointerFunction;
import processing.core.PApplet;
import processing.core.PVector;

public class ProjectionMask  implements Convertible<ProjectionMask> {
	public String _label;
	public int _surfaceId;
	public int _pointerId;
	public int _selectedId;
	public int _id;
	public float _optimalProjectionDistance;
	public int _widthResolution;
	public int _heightResolution;

	public PVector[] _homographyCorners = {new PVector(), new PVector(180, 0), new PVector(180, 180), new PVector(0, 180)}; //default using servo ranges

	

	public ProjectionMask() {
		_id = PointerEnvironment.masks.size();
		System.out.println("mask created " + _id);
		_label = "Projection mask " + _id;
		_selectedId = _id;
	}
	
	
	
	public ProjectionMask setPointer(int p) {
		_pointerId = p;
		PointerProjector pointer = PointerEnvironment.getPointer(p);
		_widthResolution = ((PointerMotor<?>) pointer.getModel().get(PointerFunction.MAIN_X)).getResolution();
		_heightResolution = ((PointerMotor<?>) pointer.getModel().get(PointerFunction.MAIN_Y)).getResolution();
		System.out.println("after getting resolution from model " + _heightResolution +","+ _heightResolution);
		return this;
	}
	
	
	
	public ProjectionMask setSize( int width, int height ){
		_widthResolution = width;
		_heightResolution = height;
		return this;
	}
	
	
	public ProjectionMask setSurface(int surface_id) {
		_surfaceId = surface_id; 
		return this;
	}

	
	
	/*
	 *  absolute position ONLY. For a servo motor - position must be from 0 to 180 in each axis
	 */
	public void setHomographyCorner(int index, PVector position) {
		_homographyCorners[index] = position;
	}
	
	
	/*
	 *  @Deprecated
	 *  converts a postion from a rectangular frame (display, canvas, etc) size.
	*/
//	public void setHomographyCorner(int index, PVector position, int from_width, int from_height) {
//		
//		float XtoMaskWidth =  PApplet.map( PApplet.constrain(position.x, 0, from_width),  0, from_width, 0, widthResolution);
//		float YtoMaskHeight =  PApplet.map(PApplet.constrain(position.y, 0, from_height), 0, from_height, 0, heightResolution);
//		
//		homographyCorners[index] = new PVector(XtoMaskWidth, YtoMaskHeight);
//	}
	
	
	public int[] getSize(){
		return new int[]{_widthResolution, _heightResolution};
	}
	
	
	public PVector getHomographyCorner( int index ){
		return _homographyCorners[index];
	}
	
	
	
	
	public PVector[] getHomographyCorners(){
		return _homographyCorners;
	}

	
	
	
	public float getOptimalProjectionDistance(){
		return _optimalProjectionDistance;
	}
	
	
	
	public PVector convertFrom( Convertible<?> source, PVector inputPosition ){
		
		float XtoMaskWidth =  PApplet.map( PApplet.constrain(inputPosition.x, 0, source.getSize()[0]),  0, source.getSize()[0], 0, _widthResolution);
		float YtoMaskHeight =  PApplet.map(PApplet.constrain(inputPosition.y, 0,source.getSize()[1]), 0, source.getSize()[1], 0, _heightResolution);
		
		return new PVector(XtoMaskWidth, YtoMaskHeight);
	}
	
	
	//returns distance in the same unit projection surface physical size was set
	private void calculateOptimalProjectionDistance(){

		//fit a squared projection area in relate surface (now limited to non-transformed-rectangular surface)
		ProjectionSurface surface = PointerEnvironment.getProjectionSurface(_surfaceId);
		float surfaceMinSize = Math.min(surface.physicalWidth, surface.physicalHeight);
		float oppositeCathetus = surfaceMinSize/2;
		float projectionApperture = PointerEnvironment.getPointer(_pointerId).getProjectionApperture();
		float projectionAngle = projectionApperture/2;
		double angle = Math.toRadians( projectionAngle );
		double hypothenuse = oppositeCathetus / (Math.sin(angle)) ;
		double adjacentCathetus = hypothenuse * Math.cos(angle);

		_optimalProjectionDistance = (float) adjacentCathetus;
	}
	

}