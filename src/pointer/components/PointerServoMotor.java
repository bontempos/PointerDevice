package pointer.components;


public class PointerServoMotor extends PointerMotor<PointerServoMotor>{
	int minPulse;
	int maxPulse;
	public  PointerServoMotor(){
		servo = true;
		resolution = 180;
	}
	public PointerServoMotor setMaxPulse(int maxPulse){
		this.maxPulse = maxPulse;
		return this;
	}
	public PointerServoMotor setMinPulse(int minPulse){
		this.minPulse = minPulse;
		return this;
	}
	
}