/*
 * 
  one canvas is set to the same size of the screen. Because the wall (surface) is canvas should have similar shape to avoid distortions
  canvas1 = new PointerCanvas(); //default is .setSize( width, height ); 
  canvas.setFadeInterval(1000); //default. Set to -1 to disable the effect.
  once canvas is initialized it autoDraws on screen unless setAutoDraw(false);
  
  TODO canvas, contents and environment should implement updatable, because its risk when user wants to update both different things at same time, 
  so every time upload happens it should enter a queue ;
 */
package pointer.projection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import pointer.PointerController;
import pointer.environment.PointerEnvironment;
import pointer.environment.PointerProjector;
import pointer.gui.GUIObject;
import pointer.system.PointerClient;
import pointer.system.PointerConstants;
import pointer.system.Uploadable;
import processing.core.PApplet;
import processing.core.PVector;
import processing.data.JSONObject;

public class PointerCanvas extends GUIObject<PointerCanvas> implements Convertible<PointerCanvas>, Uploadable{
	
	int _id;
	int height;
	int width;
	int fadeInterval = 1000;
	boolean autoDraw = true; // when false, user must call canvas.draw() to be able to see this object.
	public boolean isSet = false; //inform if server is aware of canvas settings
	CanvasContents<?> contents;
	List<PointerProjector> pointers = new ArrayList<PointerProjector>();
	JSONObject _projectionInstructions;

	public PointerCanvas(){
		_id = PointerController.canvases.size();
		_gui_label = "Canvas " + _id;
		PointerController.papplet.registerMethod("draw", this);
		setSize( PointerController.papplet.width, PointerController.papplet.height );
	}


	public PointerCanvas(int _width, int _height) {
		setSize(_width, _height);
	}


	public PointerCanvas setSize( int w, int h){
		height = h;
		width = w;
		return this;
	}


	public PointerCanvas setFadeInterval( int fadeInterval ){
		this.fadeInterval = fadeInterval;
		return this;
	}

	public void setCanvas(){ //not sure about this name

		if(pointers.isEmpty()){
			System.out.println("no pointers assigned to canvas " + getGUILabel());
			return;
		}

		System.out.println("setting canvas " + getGUILabel());

		//get pointers assigned to this canvas
		byte[] assigned = new byte[ pointers.size() ];
		for(int i = 0; i<assigned.length;i++){
			PointerProjector p = pointers.get(i);
			assigned[i] = (byte)(p._id);
		}


		int protocol_size = assigned.length + 6;


		byte[] canvasSettings = new byte[]{  
				PointerConstants.SET_CANVAS, 				 //protocol for canvas
				(byte) ((protocol_size >> 8) & 0xff),(byte) (protocol_size & 0xff),
				(byte) ((_id >> 8) & 0xff),(byte) (_id & 0xff),    //(byte)(id/256), (byte)(id%256),
				(byte) ((width >> 8) & 0xff),(byte) (width & 0xff),
				(byte) ((height >> 8) & 0xff),(byte) (height & 0xff)
		};

		
		byte[] set_canvas = new byte[ canvasSettings.length + assigned.length ];
		System.arraycopy(canvasSettings, 0, set_canvas, 0, canvasSettings.length);
		System.arraycopy(assigned, 0, set_canvas, canvasSettings.length, assigned.length);
		System.out.println("sending canvas " + getGUILabel() + " to server now. Assigned size: " + pointers.size());		
		PointerClient.packs.add( set_canvas );

		isSet = true;
	}




	public void setContents(){
		if(contents != null){
			byte[] contents_in_bytes = contents.toByteArray();

			System.out.println( "contents_in_bytes size: " + contents_in_bytes.length);

			byte[] contentsProtocol = new byte[]{  
					PointerConstants.SET_CONTENTS, 						//protocol for contents
					//(byte) ((contents_in_bytes.length >> 16) & 0xff), 	//size of contents in MEDIUMINT byte 1
					(byte) ((contents_in_bytes.length >> 8) & 0xff),	//size of contents in MEDIUMINT byte 2
					(byte) (contents_in_bytes.length & 0xff)	 		//size of contents in MEDIUMINT byte 3
			};


			byte[] set_contents = new byte[ contentsProtocol.length + contents_in_bytes.length ];
			System.arraycopy(contentsProtocol, 0, set_contents, 0, contentsProtocol.length);
			System.arraycopy(contents_in_bytes, 0, set_contents, contentsProtocol.length, contents_in_bytes.length);


			PointerClient.packs.add( set_contents );

			contents.executedTimes++;
		}
	}




	public PointerCanvas assign( PointerProjector pointer ){
		pointers.add(pointer);
		return this;
	}


	public PointerCanvas assignAll(){
		pointers.addAll(PointerEnvironment.pointers);
		return this;
	}


	//	public void project(){
	//		project(contents);
	//	}


	//	public void project(CanvasContents c){
	//		//TODO
	//	}


	//TEMPORARY TEST
	//public void project ( ArrayList<PVector> points ){
	public void project ( PVector[] points ){

		//which pointers are going to draw this?
		//and what is each task of each pointer? are they all drawing the same thing at same time?
		//are they splitting the task? how and which pointer is going to do what?
		//are they drawing at different time?

		//for now all drawing samething at sametime.
		//send protocol <- server gets read for receiving contents after setting actors
		//send contents

		contents = new CanvasContentsVector( this, points );
		
		if(!isSet) {
			//if canvas is not set, set once first, than send contents after a few
			setCanvas(); 
			Timer timer = new Timer();
			timer.schedule(new TimerTask(){
				@Override
				public void run(){
					setContents();
				}
			}
			, 200);			
		}else{
			setContents();
		}
	}

	





	public void draw(){

	}



	public int[] getSize() {
		return new int[]{width, height};
	}


	public PVector convertFrom(Convertible<?> source, PVector inputPosition) {
		float XtoMaskWidth =  PApplet.map( PApplet.constrain(inputPosition.x, 0, source.getSize()[0]),  0, source.getSize()[0], 0, width);
		float YtoMaskHeight =  PApplet.map(PApplet.constrain(inputPosition.y, 0,source.getSize()[1]), 0, source.getSize()[1], 0, height);	
		return new PVector(XtoMaskWidth, YtoMaskHeight);
	}


	

}

/*
 *  contents can be a list of points which describes a trajectory for the motors
 *  these points can be overlaped by a bitmap or gradient instructions to control the laser brightness
 *  
 *  contents can also be a bitmap image, and instructions to draw it will go to each pixel (in resolution) and project on it
 *  the order of projection by default starts with darker pixels to brighter pixels
 */
abstract class  CanvasContents<T> implements DrawableOnCanvas<T>{

	public int executedTimes = 0;
	public List<Codeline>codelines = new ArrayList<Codeline>();

	public void setCodelines( PVector[] points, float[] brightness_map  ){
		for(int i = 0; i < points.length ;i++){
			codelines.add( new Codeline( (short) points[i].x, (short) points[i].y, brightness_map[i]));
			System.out.println("codeline: " + points[i].x + ","  + points[i].y + "," + brightness_map + ",");
		}
	}
	
	public byte[] toByteArray(){
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ByteBuffer buffer = ByteBuffer.allocate(8); // + contents.assigned.length + 1 to indicate line size 
		for(int i = 0; i <codelines.size(); i++){
			Codeline line = codelines.get(i);
			buffer.clear();
			// plus add line size. For now its fixed to 8 because not including assigned pointers
			buffer.putShort(0,line.x_on_canvas);
			buffer.putShort(2,line.y_on_canvas);
			buffer.putFloat(4,line.brightness_at_position);
			// plus list of assigned pointers ids
			try {
				bout.write(buffer.array());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return  bout.toByteArray() ;
	}
}

/*
 *  sets codelines in a sequential list of points, like a path
 */
class CanvasContentsVector extends CanvasContents<CanvasContentsVector>{

	CanvasContentsVector ( PointerCanvas canvas, PVector[] points ){
		float[] allBright = new float[ points.length];
		for(int i = 0; i < points.length; i++) allBright[i] = 100; 
		setCodelines( points, allBright );
	}
	CanvasContentsVector (PointerCanvas canvas, ArrayList<PVector> points ){
		PVector[] _points = (PVector[]) points.toArray();
		float[] allBright = new float[ _points.length];
		for(int i = 0; i < _points.length; i++) allBright[i] = 100;
		setCodelines( _points, allBright );
	}
	CanvasContentsVector ( PVector[] points, float[] brightness_map){
		setCodelines( points, brightness_map );
	}
	

}

/*
 *  sets codelines in a list of points spatially distributed based on bitmap pixel intensity
 */
//class CanvasContentsBitmap extends CanvasContents{
//
//}

interface ContentsMaps{
	// bitmap for controlling intensity
	// bitmap for controlling assigments
}


interface DrawableOnCanvas<T>{
	void setCodelines( PVector[] points, float[] brightness_map  );
}

class Codeline{
	short x_on_canvas;
	short y_on_canvas;
	float brightness_at_position;
	int[] assigned; //each code of line could have a pointer assigned;
	Codeline (short x, short y, float b){
		x_on_canvas = x;
		y_on_canvas = y;
		brightness_at_position = b;
	}
}


