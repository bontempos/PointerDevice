package pointer.environment;

import java.util.ArrayList;

import pointer.components.PointerMotor;
import pointer.gui.GUIObject;
import pointer.projection.ProjectionMask;
import pointer.system.PointerConstants.PointerFunction;
import processing.core.PVector;

public class PointerProjector extends GUIObject <PointerProjector>{ //change to PointProjector ><
	
	public static int _id;
	
	public boolean active = true;
	
	ProjectorModel model = new ProjectorModel();
	
	//Pointer:
	float intensity = 100;  					    // a value from 0 to xxx which constrains the brightness of laser
	float brightness;								// the variable current value of laser, from 0 to intensity
	float speed; 									// the variable aiming motor speed. initial value defined by model
	//float speedMin;									// limits the min speed of main motors x and y
	//float speedMax;									// limits the max speed of main motors x and y
	
	boolean mainXinverted = false;
	boolean mainYinverted = false;
	boolean switchXY = false;
	
	PVector initialPosition = new PVector();
	PVector projectionCenter = new PVector();
	ArrayList<ProjectionMask> projectionMasks = new ArrayList<ProjectionMask>(); 
//	PVector[] corners = {new PVector(), new PVector(180, 0), new PVector(180, 180), new PVector(0, 180)};
	
	boolean editMode = false;
	
	
	
	public PointerProjector() {
		_id = PointerEnvironment.pointers.size();
		_gui_label = "Pointer " + _id;
		_selected_id = _id;
	}
	
	
	
	public PointerProjector setId( int id ){
		_id = id;
		return this;
	}

	
	
	public PointerProjector setModel(ProjectorModel model) {
		this.model = model;

		//get default speed
		float mainX = ((PointerMotor<?>) model.get(PointerFunction.MAIN_X)).getSpeed(); //the speed of motor used in x axis 
		float mainY = ((PointerMotor<?>) model.get(PointerFunction.MAIN_Y)).getSpeed(); //the speed of motor used in y axis
		speed =  Math.min(mainX, mainY); //set initial values
		
		//get motor parameter
		mainXinverted = ((PointerMotor<?>) model.get(PointerFunction.MAIN_X)).getInvertDirection();
		mainYinverted = ((PointerMotor<?>) model.get(PointerFunction.MAIN_X)).getInvertDirection();
		return this;
	}

	
	
	public PointerProjector setLabel(String label) {
		_gui_label = label;
		PointerEnvironment.buildEnvironment();
		return this;
	}
	
	
	
	
	public PointerProjector setApperture(float f) {
		model.projectionAppertureAngle = f;
		return this;
	}
	
	
	
	
	public PointerProjector setIntensity( float intensity){
		this.intensity = intensity;
		return this;
	}
	
	
	
	public PointerProjector setBrightness( float brightness){
		this.brightness = brightness;
		return this;
	}
	
	
	
	public PointerProjector setSpeed( float speed ){
		this.speed = speed;
		return this;
	}
	
	
	
	public PointerProjector setInitialPosition( int position_x, int position_y ){
		this.initialPosition = new PVector( position_x, position_y );
		return this;
	}
	
	
	
	
	public ProjectorModel getModel(){
		return model;
	}
	
	
	public float getProjectionApperture(){
		return model.projectionAppertureAngle;
	}
	
	
	
	public float getIntensity(){
		return intensity;
	}
	
	
	
	public float getBrightness(){
		return brightness;
	}
	
	
	
	public float getSpeed(){
		return speed;
	}
	
	
	
	public boolean getMainXinverted(){
		return mainXinverted;
	}
	
	
	
	public boolean getMainYinverted(){
		return mainYinverted;
	}
	
	
	
	public PointerProjector projectOn( ProjectionSurface projection_surface ){
		ProjectionMask mask = new ProjectionMask().setPointer(this._id).setSurface(projection_surface._id);
		projectionMasks.add(mask);
		PointerEnvironment.masks.add(mask);
		return this;
	}

	
	
	public ProjectionMask getProjectionMask() {
		if(!projectionMasks.isEmpty()){
			return projectionMasks.get(0);
		}
		else{
			System.out.println("PointerDevice: projection mask list is empty");
			return null;
		}
	}
}
