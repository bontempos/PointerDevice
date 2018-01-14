package pointer.projection;

import processing.core.PVector;

public interface Convertible<T> {
	int[] getSize();
	T setSize( int width, int height );
	PVector convertFrom( Convertible<?> source, PVector inputPosition);
}
