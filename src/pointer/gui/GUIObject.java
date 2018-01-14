/*
 *  A common class between gui objects whith have a Transform object assigned
 */

package pointer.gui;

import java.awt.Color;
import java.util.Random;



public class GUIObject<T> extends GUIIdentity<  GUIObject<T>  >{
	 
	public String _gui_label = null;
	public int _color_id;
	public Transform _transform = new Transform();
	public boolean _updateOnRelease;
	public boolean _autoUpdate = true;
	
	
	public GUIObject(){
		_color_id = getRandColor();
	}
	
	protected int getRandColor(){
		Random random = new Random();
		final float hue = random.nextFloat();
		final float saturation = 0.9f;//1.0 for brilliant, 0.0 for dull
		final float luminance = 1.0f; //1.0 for brighter, 0.0 for black
		return (Color.getHSBColor(hue, saturation, luminance)).getRGB();
	}
	
	public String getGUILabel(){
		return _gui_label;
	}
	
	
	
	
	
}


