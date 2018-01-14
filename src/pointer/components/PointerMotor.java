package pointer.components;

public class PointerMotor<T> extends PointerHardware<T>{
	float torque;
	float speed; //degrees by time 
	float acceleration;
	int resolution;
	boolean servo = false;
	boolean invertedDirection = false;
	PointerMotor(){
	}
	
	
	public T setSpeed( float speed ){
		this.speed = speed;
		return (T) this;
	}
	
	
	
	public T setResolution( int resolution ){
		this.resolution = resolution;
		return (T) this;
	}
	
	
	
	public float getSpeed(){
		return speed;
	}
	
	
	public boolean getInvertDirection(){
		return invertedDirection;
	}
	
	
	public int getResolution(){
		return resolution;
	}
	
}
