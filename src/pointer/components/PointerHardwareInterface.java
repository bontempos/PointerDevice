package pointer.components;

import pointer.system.PointerConstants.PointerFunction;

public interface PointerHardwareInterface<T> {
//	PointerFunction function = null;
	public T setLabel(String s);
	public T setFunction(PointerFunction function);
}
