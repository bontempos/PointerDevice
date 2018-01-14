package pointer.components;


import pointer.system.PointerConstants.PointerFunction;

public class PointerHardware< T > implements PointerHardwareInterface<T>{
	String label;
	public PointerFunction function; 
	private T me;
	
	PointerHardware(){
		me = (T)this;
	}
	
	public T setLabel(String label){
		this.label = label;
		return me;
	}

	public T setFunction(PointerFunction function){
		this.function = function;
		return me;
	}

}

