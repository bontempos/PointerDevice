/*
 * 
 * 
 * 
  USAGE:
  PointerModel model = new PointerModel().setLabel("model01").setProjectionApperture(30);
  model.addServo().label("servoX").setFunction(MAIN_X).setSpeed(2.0).setMaxPulse(500).setMinPulse(90);
  model.addServo().label("servoY").setFunction(MAIN_Y).setSpeed(2.0).setMaxPulse(500).setMinPulse(90);
  model.addLaserModule().frequency(205).power(0.005);

 */
package pointer.environment;


import java.util.ArrayList;
import java.util.List;

import pointer.components.PointerHardware;
import pointer.components.PointerHardwareInterface;
import pointer.components.PointerServoMotor;
import pointer.components.PointerSteppingMotor;
import pointer.system.PointerConstants;
import pointer.system.PointerConstants.PointerFunction;



public class ProjectorModel {

	String label;
	float physicalDepth;
	float physicalWidth;
	float physicalHeight;
	float projectionAppertureAngle;
	List <PointerHardware<?>> hardwareComponents;


	public ProjectorModel() {
		hardwareComponents = new ArrayList<PointerHardware<?>>();
	}
	
	public ProjectorModel setLabel( String label){
		this.label = label;
		return this;
	}
	
	public String getLabel(){
		return label;
	}

	public ProjectorModel setProjectionApperture( float projectionAppertureAngle){
		this.projectionAppertureAngle = projectionAppertureAngle;
		return this;
	}
	
	public PointerServoMotor addServo(){
		PointerServoMotor m = new PointerServoMotor();
		hardwareComponents.add(m);
		return m;
	}

	public PointerSteppingMotor addSteppingMotor(){
		PointerSteppingMotor m = new PointerSteppingMotor();
		hardwareComponents.add(m);
		return m;
	}

	public ProjectorModel addLaserModule(){
		return this;
	}
	
	public PointerHardware<?> get (PointerFunction function){
		for( PointerHardware<?> h: hardwareComponents){
			if(h.function ==  function ){
				return h;
			}
		}
		return null;
	}
}








