/*
 *  handles user keyboard or mouse commands
 */
package pointer.system;

import java.util.Timer;
import java.util.TimerTask;

import static pointer.PointerController.environment;
import static pointer.system.PointerConstants.*;

import pointer.PointerController;
import pointer.environment.PointerEnvironment;
import pointer.environment.PointerProjector;
import pointer.projection.ProjectionMask;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PVector;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

public class IOcommandsHandler {

	PApplet papplet;
	private static boolean[] EDIT_PROJECTION_MASK_POINT = new boolean[6];

	private static boolean mouseReleased = true;
	private static int selectedEditPoint = -1;

	PGraphics editModeGraphic, propertiesGraphic; //TODO create view. temporary here for debug on screen

	private int idleAfterInterval = 5000;
	private static boolean idle = true;
	static Timer idleTimer = new Timer();
	static TimerTask setToIdle ;


	public IOcommandsHandler( PApplet papplet ){
		this.papplet = papplet;
		editModeGraphic = papplet.createGraphics(papplet.width, papplet.height); //TODO temporary here for debug on screen
		propertiesGraphic = papplet.createGraphics(papplet.width/2, 40);//TODO temporary here for debug on screen
		papplet.registerMethod("mouseEvent", this);
		papplet.registerMethod("keyEvent", this);
		papplet.registerMethod("draw", this);
	}

	private void updateSelectedPointer( float laserIntensity, PVector mousePosition){
		ProjectionMask selectedMask = PointerEnvironment.getPointer().getProjectionMask();
		PointerController.liveUpdate(environment.getPointer()._id, laserIntensity, selectedMask.convertFrom(PointerController.mainCanvas, mousePosition));
	}



	public void mouseEvent(MouseEvent e){
		if(PointerController.editMode){

			PVector mousePosition = new PVector(papplet.mouseX,papplet.mouseY);

			switch(e.getAction()){


			case MouseEvent.PRESS:
				mouseReleased = false;				
				//@streamOutput laser turns on, target is mousePosition
				updateSelectedPointer( environment.getPointer().getIntensity(), mousePosition);
				idle = false;
				break;


			case MouseEvent.MOVE:

				//@streamOutput laser turn off, target is mousePosition
				updateSelectedPointer( 0f, mousePosition);

				break;


			case MouseEvent.DRAG:
				setSelectedEditPoint();
				if(selectedEditPoint!=-1) {
					setPositionOnProjectionMaskCorner();
				}
				//@streamOutput laser turns on, target is mousePosition
				updateSelectedPointer( environment.getPointer().getIntensity(), mousePosition);

				break;


			case MouseEvent.RELEASE:
				mouseReleased = true;
				System.out.println("mouse released, stop motor");
				if(selectedEditPoint==-1){
					//@streamOutput laser turn off, target is mousePosition
					updateSelectedPointer( 0f, mousePosition);

					//startIdleTimeout();
					setIdle(true);
				}
				break;
			}
		}
	}

	void setSelectedEditPoint(){
		selectedEditPoint = -1;
		for(int i = 0; i < 6 ; i++){
			if(EDIT_PROJECTION_MASK_POINT[i]){
				selectedEditPoint = i;
				idle = mouseReleased;
				break;
			}
		}
	}

	public void keyEvent(KeyEvent e){
		if(PointerController.editMode){
			int index = e.getKey() - '0';


			switch(e.getAction()){

			case KeyEvent.TYPE:
				setIdle(false);

				if(e.getKey() == PointerConstants.SELECT_NEXT){;
				//TODO for now, only with pointers
				environment.pointers.selectNext();
				}

				else if(e.getKey() == PointerConstants.SELECT_BACK ){;
				environment.pointers.selectBack();
				}

				else if(e.getKey() == PointerConstants.EXIT_EDIT_MODE) {
					PointerController.setEditMode(false);
					idle = true;
					environment.buildEnvironment();
				}
				break;

			case KeyEvent.PRESS:
				setIdle(false);

				System.out.println("you pressed " + e.getKey() + " index:" + index + "keyCode" + e.getKeyCode());


				if(index >= 1 && index <= 6) {
					EDIT_PROJECTION_MASK_POINT[index-1] = true; 
					setSelectedEditPoint();
					if(!mouseReleased) setPositionOnProjectionMaskCorner();
				}

				else if(e.getKeyCode() == PointerConstants.TUNNING_UP){

				}

				else if(e.getKeyCode() == PointerConstants.TUNNING_DOWN){

				}

				else if(e.getKeyCode() == PointerConstants.TUNNING_LEFT){

				}

				else if(e.getKeyCode() == PointerConstants.TUNNING_RIGHT){

				}

				break;
			case KeyEvent.RELEASE:
				if(index >= 1 && index <= 6){
					EDIT_PROJECTION_MASK_POINT[index-1] = false;
					setSelectedEditPoint();
				}
				if(mouseReleased) setIdle(true); //startIdleTimeout();
				break;
			}
		}
	}



	void setPositionOnProjectionMaskCorner(){
		if(environment.getPointer() != null){
			ProjectionMask m = environment.getPointer().getProjectionMask();
			//uses the whole papplet screen as canvas for moving the mouse
			PVector inputPosition = new PVector(papplet.mouseX, papplet.mouseY);
			m.setHomographyCorner(selectedEditPoint, m.convertFrom(PointerController.mainCanvas, inputPosition));
			//m.setHomographyCorner( selectedEditPoint, new PVector(papplet.mouseX, papplet.mouseY) , papplet.width, papplet.height);
		}
	}


	void setIdle(boolean _idle){
		if(_idle) {
			startIdleTimeout();
		}else{
			idle = false;
			idleTimer.cancel();
			idleTimer.purge();
		}
	}

	void startIdleTimeout(){
		setToIdle = new TimerTask(){
			@Override
			public void run(){
				idle = true;
			}
		};
		idleTimer = new Timer();
		idleTimer.schedule( setToIdle, idleAfterInterval);
	}



	void displaySelectedEditPoint(){
		updatePropertiesGraphic();
		editModeGraphic.beginDraw();
		editModeGraphic.clear();
		editModeGraphic.image(propertiesGraphic,papplet.width/2 - (propertiesGraphic.width/2),0);
		if(!mouseReleased){
			editModeGraphic.stroke(0xffffffff);
			editModeGraphic.strokeWeight(4);
			editModeGraphic.point(papplet.mouseX, papplet.mouseY);
		}
		editModeGraphic.endDraw();
		papplet.image(editModeGraphic, 0, 0) ;
	}


	PVector[] getSelectedProjectionMaskCorners(){
		if(environment.getPointer() != null){
			ProjectionMask m = environment.getPointer().getProjectionMask();
			return  m.getHomographyCorners();
		}
		return null;
	}


	void updatePropertiesGraphic(){
		propertiesGraphic.beginDraw();
		propertiesGraphic.background(0xffffff00);
		propertiesGraphic.fill(0);
		propertiesGraphic.text("ID " + environment.getPointer()._id + ":", 3,15);
		if(selectedEditPoint!=-1){
			propertiesGraphic.text("edit point:" + selectedEditPoint, 40,15);

			//mouse position
			//propertiesGraphic.text("x:" + papplet.mouseX +" y:"+ papplet.mouseY, 11,35);

			//mask position
			PVector[] corners = getSelectedProjectionMaskCorners();

			propertiesGraphic.text("x:" + (int)corners[selectedEditPoint].x +" y:"+ (int)corners[selectedEditPoint].y, 10,35);


		}else{
			propertiesGraphic.text("<no selection>", 40,15);
			propertiesGraphic.text("click 1 to 4", 10,35);
		}
		propertiesGraphic.endDraw();
	}



	void displayHomographyCorners(){
		if(environment.getPointer() != null){
			ProjectionMask m = environment.getPointer().getProjectionMask();
			papplet.beginShape();
			papplet.noFill();
			papplet.stroke(-1);
			for(int i = 0; i < 4; i++){
				PVector corner = PointerController.mainCanvas.convertFrom(m, m.getHomographyCorners()[i]);
				papplet.vertex( corner.x, corner.y );
			}
			papplet.endShape(PConstants.CLOSE);
		}
	}



	public void draw(){
		if(!idle){
			displaySelectedEditPoint();
			displayHomographyCorners();
		}
	}

}


