package pointer.system;

import java.awt.event.KeyEvent;


public final class PointerConstants {
	
	
	/*
	 *  ENUMS
	 */
	public static enum PointerFunction{MAIN_X,MAIN_Y,X,Y,MAIN_laser};
	public static enum PositionOnList { SELECTED, LAST, FIRST };
	
	
	
	//protocol for streaming commands to server	
	public static final byte SET_ENVIRONMENT = 0;	//used to updload environments
	public static final byte SET_POINTER = 1; //used to driver pointer directly
	public static final byte SET_CANVAS = 2;	//used to upload canvas settings
	public static final byte SET_CONTENTS = 3; //used to upload contents codelines
	
	
	
	public static final  PointerFunction MAIN_X = PointerFunction.MAIN_X;
	public static PointerFunction MAIN_Y = PointerFunction.MAIN_Y;
	public static PointerFunction MAIN_laser = PointerFunction.MAIN_laser;
	
	
	public static final char SELECT_NEXT = 'n';
	public static final char SELECT_BACK = 'b';
	public static final char EXIT_EDIT_MODE = 'x';
	public static final int TUNNING_UP = KeyEvent.VK_UP;
	public static final int TUNNING_DOWN = KeyEvent.VK_DOWN;
	public static final int TUNNING_LEFT = KeyEvent.VK_LEFT;
	public static final int TUNNING_RIGHT = KeyEvent.VK_RIGHT;
	
	
	
	
	private PointerConstants()
	{	
		throw new AssertionError();
	}
	
}
