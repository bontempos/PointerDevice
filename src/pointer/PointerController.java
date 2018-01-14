/**
 * Pointer Devices main controller
 *
 * @author       Anderson Sudario
 * @version      1.0
 * 2017
 */

package pointer;

import java.util.ArrayList;
import java.util.List;

import pointer.environment.PointerEnvironment;
import pointer.environment.PointerProjector;
import pointer.projection.PointerCanvas;
import pointer.system.IOcommandsHandler;
import pointer.system.PointerClient;
import pointer.system.PointerConstants;
import processing.core.PApplet;
import processing.core.PVector;


public  class PointerController {


	private static PointerClient client;
	private static IOcommandsHandler IOhandler;
	
	public static PApplet papplet;
	public static PointerEnvironment environment;
	public static PointerCanvas mainCanvas;
	public static List<PointerCanvas> canvases;
	
	protected static ArrayList<PointerProjector>activePointers;
	
	public static boolean editMode = false;

	public static boolean SET_CORNER_1 = false;
	public static boolean SET_CORNER_2 = false;
	public static boolean SET_CORNER_3 = false;
	public static boolean SET_CORNER_4 = false;

	public PointerController(PApplet papplet){
		PointerController.papplet = papplet;
		client = new PointerClient();
		mainCanvas = new PointerCanvas(papplet.width, papplet.height);
	    IOhandler = new IOcommandsHandler(papplet);
		environment = new PointerEnvironment(); //starts a blank environment;
		papplet.registerMethod("pre", this);
		canvases = new ArrayList<PointerCanvas>();
	}


	public void pre(){
		client.update();
	}
	
	public static boolean getEditMode(){
		return editMode;
	}

	public static void setEnvironment( PointerEnvironment _environment){
		environment = _environment;
	}

	public static void setEditMode( boolean editMode ){
		PointerController.editMode = editMode;
	}

	public static void uploadToServer( byte[] data){
		client.packs.add(data);
	}


	public void sendTest( byte [] pack ){
		client.packs.add(pack);
	}
	
	
	
	private static int updateActivePointers(){
		for(int i = 0; i < PointerEnvironment.pointers.size(); i++){
			if(PointerEnvironment.pointers.get(i).active) activePointers.add(PointerEnvironment.pointers.get(i));
		}
		return activePointers.size();
	}
	
	
	/*
	 *  single pointer update - avoid in a loop updating other pointers
	 */
	public static void liveUpdate(  int pointerId,  float laserBrightness, PVector finalPosition ){
		
		/*
		 *  laserBrightness is a float value between 0 and 100 meaning percentage.
		 *  laserIntentisy is a constrain value which clips the max percentage;
		 *  currently, a laser has resolution up to 4096 bits, but for now, using only 1 byte, from 0 to 255
		 *  so for now we need a converstion
		 */
		
		byte laser1byteRes =  (byte)( PApplet.norm(laserBrightness, 0f, 100f) * 256 );
		
		client.packs.add( new byte[]{ PointerConstants.SET_POINTER, 0, (byte)4, 
			    (byte)pointerId, laser1byteRes, (byte)finalPosition.x, (byte)finalPosition.y
		});
	}

	

	public static void liveUpdate( float []laserBrightness , PVector [] finalPosition){
		
	}
	
	public static void liveUpdate( PVector[] finalPosition ){
		
	}
	
	public static void liveUpdate( float[] finalPosition ){
	
	}
	
}






/*
 //setting specific (~selected) Pointer
	//avoid using this in a loop (for all pointers) inside processing. Instead, send array of values to function above.
	//position comes in mouse coord system
*	public void update( PVector position, int laser, int pointerId){
	
*		int lasers[] = new int[activePointers()];
*		PVector positions[] = new PVector[activePointers()];

		//loops thru all pointers copying their states, but updates only selected one:
*		for (int i = 0; i < activePointers(); i++) {
*			PointerDevice p = pointers.get(i);

			if(p.id == pointerId){
				//inserting new data on specific pointer ( position is in screen coord system converting to servo coord)
				//System.out.println("Pointer "+ i +": new position:" + p.x + ", " + p.y);
				p.target = pointerTrasformations(  pointers.get(pointerId), position.x, position.y );
				//pointerTrasformations( pointers.get(pointerId) , position.x, position.y ); //transforming pointer instance position directly 
				p.setLaser (laser);
			}

			//just retransmitting current data from all pointers (expects servo coord system)
			//System.out.println("Pointer "+ i +": retransmiting position:" + p.x + ", " + p.y);
			//positions[i] = toWindow(fromServos(new PVector(p.x, p.y))); //use toCanvas - if canvas[0] != 0 
			positions[i] = new PVector(p.target.x, p.target.y); //Retransmitting servo coord //toTarget
			lasers[i] = p.laser;

		}
		update( positions, lasers, false); //dont use transformation 
	}
 */

