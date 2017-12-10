/**
 * Pointer Devices main controller and serial communication class
 *
 * @author       Anderson Sudario
 * @version      1.0
 * 2017
 */

//TODO every place using fromScreen - could be fromCanvas <-- make some automatic function to select among them.


package bontempos.PointerDevice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;


import bontempos.Game.Act.Act;
import bontempos.Game.Act.Action;
import bontempos.Game.Act.Checker;
import bontempos.Game.Act.Countdown;
import bontempos.ProjectionMatrix.HomographyMatrix;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PShape;
import processing.core.PVector;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.event.KeyEvent;
import processing.serial.Serial;


//what about extending this from a ArrayList ? so instead of pc.pointers.get(0), we use just pc.get(0);

public  class PointerController {

	public static int baudRate = 38400; //230400;
	public static String serialInput = null;
	public static String cmdOutput = "";
	public static PointerDevice selectedPointer;
	public static ArrayList<PointerDevice> pointers  = new ArrayList<PointerDevice>();

	public static int [] screenSize  = {100,100};		//size of the parent window;
	public static int [] canvasPosition  = {0,0};		//left upper corner position of canvas inside screen
	public static int [] canvasSize  = {0,0};			//if value > 0, canvas become active on some calculations

	public static int sentCommands;
	public static boolean onCalibration;
	private static boolean laserOnCalibration = false;	//after pressing buttons 1 to 6 for calibration, it turns laser off automatically when releasing key
	public static boolean useProjectionMatrix; 			//when calibrating for 3D objects
	public static boolean forceHomographyOnCalibration;	//calibration deactivates homography calculation by default. 

	public static Act act;								//using timers to trigger actions after timeup

	private static PointerController instance;
	static PApplet parent;

	private static Serial serial;
	private String arduinoReply = null;
	private static  int	 servoUpdateRate	  =	  20;  //TODO replace by pointerUpdateRate (because it also updates laser gradient, not just servo)
	Countdown servoUpdateTimer;

	private int maxPointer = 4;
	private boolean useMaxPointerLimit = true; 			//if true, will ignore file readings
	private PrintWriter config;							//txt file with calibration data
	private BufferedReader reader;


	//############################################# < RESERVED KEYS ON CALIBRATION > ##################################################

	public char CALIBRTION_MODE_KEY = 	'c';				//toggles calibration mode when pressed
	public char TOGGLE_HOMOGRAPHY = 	'h';
	public char SAVE_DATA = 			's';
	public char LOAD_DATA = 			'l';

	public char TOGGLE_LASER = 			'L';
	public char DRAW_FRAME	=			'F';
	//numbers from 1 to 6 for each of calibration vertices
	//LEFT and RIGHT to move among selected Pointers

	//############################################# < GETTERS > ##################################################

	public static PApplet getParent() {
		return parent;
	}

	public int activePointers(){
		int size = 0;
		for(int i = 0; i < pointers.size(); i++){
			if(pointers.get(i).active) size++;
		}
		return size;
	}

	public PointerDevice getPointer(int pointerId){
		return pointers.get(pointerId);
	}

	public int getSelectedPointerId(){
		if(selectedPointer != null && !pointers.isEmpty()){
			return selectedPointer.id;
		}else{
			return -1;
		}
	}

	public static int getServoUpdateRate() {
		return servoUpdateRate;
	}

	
	public boolean isOnCalibration(){
		return onCalibration;
	}

	//############################################# < SETTERS > ##################################################

	public static void setParent(PApplet parent) {
		PointerController.parent = parent;
	}

	public void setCalibration(boolean bool){ //int pointId
		onCalibration = bool;
	}

	public void setScreenSize( int width, int height){
		screenSize[0] = width;
		screenSize[1] = height;
		System.out.println("Screen size:" + screenSize[0] + " x " + screenSize[1]);
	}

	public  void setCanvasSize( int width, int height){
		canvasSize[0] = width;
		canvasSize[1] = height;
	}


	public static void setServoUpdateRate(int servoUpdateRate) {
		PointerController.servoUpdateRate = servoUpdateRate;
	}

	public  void setCanvasPosition( int x, int y){
		canvasPosition[0] = x;
		canvasPosition[1] = y;
	}


	//##############################################  CONTROLLER INIT  ##################################################
	
	public PointerController(PApplet parent) {
		initialize(parent, Serial.list()[1], 9600) ;
	}
	
	public PointerController(PApplet parent, int baudRate) {
		initialize(parent, Serial.list()[1], baudRate) ;
	}
	
	public PointerController(PApplet parent, String serialAddress, int baudRate) {
		initialize(parent, serialAddress, baudRate);
	}

	private void initialize(PApplet parent, String serialAddress, int baudRate){
		PointerController.parent = parent;
		PointerController.baudRate = baudRate;
		setScreenSize(parent.width, parent.height); //default
		parent.registerMethod("draw", this);
		parent.registerMethod("keyEvent", this);
		forceHomographyOnCalibration = false;
		act = new Act(parent);
		serialInit(serialAddress, baudRate);
		Action servoUpdateAuto = new Action( this, "executeServosTrajectories");
		servoUpdateAuto.setEcho(false);
		servoUpdateTimer = new Countdown(servoUpdateRate,servoUpdateAuto); 
		servoUpdateTimer.setRepeat(true);
		servoUpdateTimer.start();
		//servoUpdateChecker.setPermanent(true);
	}

	public static PointerController get() {
		if (instance == null) {
			instance = new PointerController(parent,baudRate);
		}
		return instance;
	}


	//##############################################  KEYBOARD  ##################################################

	public void keyEvent(KeyEvent event){
		switch (event.getAction()) {

		case KeyEvent.PRESS:

			//set calibration mode
			if (event.getKey() == CALIBRTION_MODE_KEY ) setCalibration( (isOnCalibration() ) ? false : true);

			if(isOnCalibration()){
				if (event.getKeyCode() == PConstants.RIGHT) selectNextPointer();
				if (event.getKeyCode() == PConstants.LEFT) selectPrevPointer();
				if (event.getKey() == TOGGLE_HOMOGRAPHY) forceHomographyOnCalibration =! forceHomographyOnCalibration; //toggleHomographicPlane(); //TODO <- this function could be outside onCalibration?
				if (event.getKey() == SAVE_DATA) savePointers();
				if (event.getKey() == LOAD_DATA) loadPointers();
				if (event.getKey() == 'p') selectedPointer.printCorners();//pointers.get(0).printCorners();

				if (event.getKey() == TOGGLE_LASER) toggleLaser();
				if (event.getKey() == DRAW_FRAME) drawFrame();
			}
			//System.out.println("Pressed key: " + event.getKeyCode());
			break;
		}

	}


	//##############################################  UTILS  #################################################

	private boolean moveMove(){
		return (parent.mouseX != parent.pmouseX || parent.mouseY != parent.pmouseY);
	}

	private static boolean usingCanvas(){
		return (canvasSize[0] > 0 && canvasSize[1] > 0);
	}

	public void drawFrame(){

	}

	//##############################################  UPDATE  ##################################################

	public void draw(){

		//____________________________________ IN CALIBRATION MODE

		if (onCalibration){

			//if no pointer is selected, select pointer 0 
			if(selectedPointer == null && !pointers.isEmpty()) selectedPointer = pointers.get(0);

			int pid = selectedPointer.id;

			if(parent.keyPressed) {
				if(parent.key >= '1' && parent.key <= '6'){

					int k = parent.key - '0';

					laserOnCalibration = true;

					if(useProjectionMatrix){

					}else{
						//moves corners for homography
						//converting mouse from screen/canvas to servo
						PVector mouse = new PVector(parent.mouseX, parent.mouseY); //pure data with absolute mouse position

						if(selectedPointer.invertY) mouse.y = flipY(mouse.y); //TODO - must use Xflip too, of course

						selectedPointer.updateCorner( k,   toServos(fromScreen(mouse.copy())) ); //TODO using no flip here but should. But if it does, will display wrong.

						if(selectedPointer.invertY) mouse.y = flipY(mouse.y);

						update( mouse, (byte)1, pid);
					}
				}
			}


			else{

				//after releasing key 1 to 6, after calibration, turns laser off
				if(laserOnCalibration){
					laserOnCalibration = false;
					update(new PVector(parent.mouseX, parent.mouseY), (byte)0, pid);
				}


				byte laserState = selectedPointer.laser;
				if( moveMove() ){
					laserOnCalibration = false;
					update(new PVector(parent.mouseX, parent.mouseY), laserState, pid);
				}
			}

		}
		//____________________________________ IN NORMAL MODE

		//executeServosTrajectories();  //automatically update by servoUpdateChecker (setup in init)

	}

//	public void blibli(){
//		System.out.println("blibli:" + parent.millis());
//	}

	public void executeServosTrajectories(){
		//will execute update for all pointers if their trajectory arraylists have any trajectory fragment to perform.

		//update 1 step including all pointers

		PVector packagePositions[] = new PVector[activePointers()];
		float packageLasers[] = new float[activePointers()];
		
		//if all pointers are idle, return, so no need to send anything to serial;
		boolean idle = true;
		for (int k = 0; k < activePointers(); k++) {
			PointerDevice p = pointers.get(k);
			if(!p.trajectoryPoints.isEmpty()){
				idle = false;
				break;
			}
		}
		if(idle) return;
		

		//creating package of positions and laser for all pointers within ONE trajectory step
		for (int k = 0; k < activePointers(); k++) {
			PointerDevice p = pointers.get(k);
			//screen coord. is transformed to servo coord
			//homography plane has its corners restricted to servo coord.

			if(!p.trajectoryPoints.isEmpty()){
				//System.out.println("there is something to execute for pointer " + p.id);
				packagePositions[k] = p.trajectoryPoints.get( p.trajectoryPoints.size() - 1 ) ; //last trajectory position (closer to current position)
				packageLasers[k] = (byte)packagePositions[k].z;
				//remove transfered trajectory from list
				p.trajectoryPoints.remove( p.trajectoryPoints.size() - 1 ); //last
				//System.out.println("Pointer "+p.id+": trajectory points size: " + p.trajectoryPoints.size() );
			}else{
				//retransmitting last position and laser status
				//if pointer trajectory is set as uncompleted, change to completed;
				//System.out.println("retransmitting position for pointer " + p.id);
				
				packagePositions[k] = new PVector(p.target.x, p.target.y); 
				packageLasers[k] = p.laser;
			}
		}

		update( packagePositions, packageLasers, false); //default (true) using transformations
		
	}

	//##############################################  DISPLAY  ##################################################

	public void displayCorners(){
		//parent.shape(cornerShape(getSelectedPointerId())) ; //original shape/size
		PShape s = cornerShape(getSelectedPointerId()); 

		//reescaleShape
		PShape rs = parent.createShape();
		rs.beginShape();
		//		rs.fill( s.getFill(1)); //only works with certain renders (p2d..)
		//		rs.stroke(s.getStroke(0));
		rs.strokeWeight(1);
		rs.noFill();
		rs.stroke(0xff00ff00);
		for(int i = 0; i < 4 ; i++){
			PVector v = toScreen(fromServos(s.getVertex(i)));
			if(selectedPointer.invertY) v.y = flipY(v.y);
			rs.vertex(v.x, v.y);
		}
		rs.endShape(PConstants.CLOSE);
		parent.shape(rs);

		//number for corners
		for(int i = 0; i < 4 ; i++){
			PVector v = rs.getVertex(i);
			parent.fill(0xffffff00);
			parent.noStroke();
			parent.ellipse(v.x, v.y, 16, 16);
			parent.fill(0x00000000);
			parent.text(i+1, v.x-4, v.y+6);
			displayServoGrid();
		}

	}
	public void displayServoGrid(){
		displayServoGrid(0x12ffffff);
	}
	public void displayServoGrid(int color){

		//TODO create contents behind once in a PGraphic and just display it
		//display a grid according to the resolution of servo motors
		//originally lets just draw one grid representing the strokes on servo coord system
		float offX =  (usingCanvas())?(canvasSize[0]/180f):(screenSize[0]/180f);
		float offY = (usingCanvas())?(canvasSize[1]/180f):(screenSize[1]/180f);
		int w = (usingCanvas())?canvasSize[0]:screenSize[0];
		int h = (usingCanvas())?canvasSize[1]:screenSize[1];

		parent.stroke(color);
		parent.strokeWeight(1);

		//considering servo X with 180 degrees TODO should load pointerDevice servorange[0]
		for(int i = 1; i <= 180; i++){
			float x = (float) (i* offX);
			float y = i*offY;
			parent.line(x, 0, x, h);
			parent.line(0, y, w, y);
			if(i == 90){
				parent.line(x, 0, x, h);
				parent.line(0, y, w, y);
			}
		}
	}


	//##############################################  SERIAL  ##################################################

//	public void serialInit(int baudRate) {
//		//System.out.println("serialInit");
//		serial = new Serial(parent, Serial.list()[1], baudRate );
//	}
	
	public void serialInit(String serialAddress, int baudRate) {
		//System.out.println("serialInit");
		serial = new Serial(parent, serialAddress, baudRate );
	}

	void serialRead() {
		if ( serial.available() > 0) {  // If data is available,
			arduinoReply = serial.readStringUntil('\n');
			if (arduinoReply != null) {
				System.out.println("IN-->:" + parent.millis() +", " + arduinoReply);
			}
		}
	}

	//setting ONLY LASER and keeping position
	public void update( float[] lasers ){
		PVector positions[] = new PVector[activePointers()];
		//for (int i = 0; i < activePointers(); i++) {
		for (int i = 0; i < pointers.size(); i++) {
			float _x = pointers.get(i).x;
			float _y = pointers.get(i).y;
			//System.out.println("pointer " + i + ": position:" + _x + "," + _y );
			positions[i] = new PVector(_x,_y);
		}
		update( positions, lasers, false); //retransmiting position with no transformation
	}




	//setting ONLY positions (mouse coord system) and keeping laser status
	public void update( PVector[] positions){
		float lasers[] = new float[activePointers()];
		for (int i = 0; i < activePointers(); i++) {
			lasers[i] = pointers.get(i).laser;
		}
		update( positions, lasers, true);
	}

	//setting BOTH positions and laser status [multi]
	public void update( PVector[] positions, float[] lasers){
		update( positions, lasers, true);
	}

	public void update( PVector[] positions, float[] lasers, boolean useTransformations){
		
		//main method. all update methods ends here.
		//this creates a package of contents to move all pointers in a very single moment and sends it to serial.

		byte serialPackage[] = {0};
		int  bytePos = 0;
		int byteSize =  4 * activePointers();


		for (int i = 0; i < activePointers(); i++) {
			PointerDevice p = pointers.get(i);


			if(p.active){

				//if(i == 0)  serial.write((byte)byteSize);   //first byte. number of following bytes (index, laser, servo1, servo2)
				if(i == 0){
					serialPackage = new byte[byteSize + 1];
					serialPackage[bytePos++] = (byte)byteSize;
				}
				
				p.setLaser(lasers[i]);
				serialPackage[bytePos++] = (byte)i;
				serialPackage[bytePos++] = p.laser;
				
				
				//pure position from mouse coord system
				float _x = positions[i].x;
				float _y = positions[i].y;

				if (useTransformations) { //TODO check calibration again
					//pointerTrasformations(  p, _x, _y );
					p.target = pointerTrasformations(  p, _x, _y );
					serialPackage[bytePos++] = (byte) servoLimit(p.target.x); //toTarget
					serialPackage[bytePos++] = (byte) servoLimit(p.target.y); //toTarget
				}else{
					//System.out.println("_x: "+ _x +" , _y:" + _y);
					serialPackage[bytePos++] = (byte)_x; //toTarget
					serialPackage[bytePos++] = (byte)_y; //toTarget
				}

				//update previous status
				//p.plaser = p.laser; //done in p.onLaserChange
				//p.ppos = p.copy(); //done in p.onMoveComplete
			}
		}
		sendCommands(serialPackage);
	}

	//setting specific Pointer
	//avoid using this in a loop inside processing. Instead, send array of values to function above.
	//position comes in mouse coord system
	public void update( PVector position, float laser, int pointerId){
		float lasers[] = new float[activePointers()];
		PVector positions[] = new PVector[activePointers()];
		for (int i = 0; i < activePointers(); i++) {
			PointerDevice p = pointers.get(i);

			if(p.id == pointerId){
				//inserting new data on specific pointer ( position is in screen coord system converting to servo coord)
				//System.out.println("Pointer "+ i +": new position:" + p.x + ", " + p.y);
				p.target = pointerTrasformations(  pointers.get(pointerId), position.x, position.y );
				//pointerTrasformations( pointers.get(pointerId) , position.x, position.y ); //transforming pointer instance position directly 
				p.setLaser (laser);
			}

			//just retransmitting current data from all pointers (expects servo coord system)
			//System.out.println("Pointer "+ i +": retransmiting position:" + p.x + ", " + p.y);
			//positions[i] = toScreen(fromServos(new PVector(p.x, p.y))); //use toCanvas - if canvas[0] != 0 
			positions[i] = new PVector(p.target.x, p.target.y); //Retransmitting servo coord //toTarget
			lasers[i] = p.laser;

		}
		update( positions, lasers, false); //dont use transformation 
	}

	public void update(){
		//used for test - with same mouse input for all Pointers
		float lasers[] = new float[activePointers()];
		PVector positions[] = new PVector[activePointers()];
		for (int i = 0; i < activePointers(); i++) {
			positions[i].set(parent.mouseX,parent.mouseY);
			lasers[i] = pointers.get(i).laser;
		}

		update( positions, lasers);
	}

	protected void sendCommands(byte[] serialPackage){
		for(int i = 0; i < serialPackage.length; i++){
			//(first byte is the byteSize, than for each pointer: index, laser, servoX, servoY)
			//System.out.println("------>> serial package:" + serialPackage[i]);
			serial.write(serialPackage[i]);
		}
		sentCommands++ ;	
	}

	//##############################################  POINTER  ##################################################

	public void addPointer(int pointers){
		System.out.println("adding pointers");
		for(int i = 0; i < pointers; i++){
			PointerController.pointers.add( new PointerDevice() );
		}
	}

	public void activate(int id){
		pointers.get(id).active = true;
	}

	public void deactivate(int id){
		pointers.get(id).active = false;
	}

	//toggle selected laser
	public void toggleLaser(){ //TODO laser is turnig off
		float lasers[] = new float[pointers.size()];

		//if(selectedPointer != null) selectedPointer = pointers.get(0);

		int pid = selectedPointer.id;

		for(int i = 0; i < pointers.size(); i++){
			if(pointers.get(i).id == pid){
				//System.out.println("Pointer "+ pid +"  laser is:" + pointers.get(pid).laser);
				if( pointers.get(pid).laser == 0){
					lasers[ pid ] = 1;
					//System.out.println("Pointer "+ pid +": laser on");
				}else{
					lasers[ pid ] = 0;
					//System.out.println("Pointer "+ pid +": laser off");
				}
				//lasers[ pid ] = (pointers.get(pid).laser != (byte)0)?(byte)0:(byte)1;
			}else{
				lasers[ i ] = pointers.get(i).laser;
			}
		}
		for(int i = 0; i < pointers.size(); i++){
			//System.out.println( "lasers["+i+"]: "+lasers[i] );
		}
		update( lasers );

	}


	public void selectNextPointer(){
		if(selectedPointer != null){
			int pid = selectedPointer.id;
			if(pid < pointers.size()-1){
				selectedPointer = pointers.get(pid + 1);
			}
			System.out.println("SELECTED:" + selectedPointer.id + ", size:" + pointers.size());
		}
	}

	public void selectPrevPointer(){
		if(selectedPointer != null){
			int pid = selectedPointer.id;
			if(pid > 0){
				selectedPointer = pointers.get(pid - 1);
			}
			System.out.println("SELECTED:" + selectedPointer.id + ", size:" + pointers.size());
		}
	}

	public void toggleHomographicPlane(){
		if(selectedPointer != null){
			selectedPointer.enableHomography =! selectedPointer.enableHomography;
			System.out.println("Pointer (" + selectedPointer.id + "): homography:" + selectedPointer.enableHomography);
		}
	}


	public static PShape cornerShape(int pointerId) {
		PointerDevice p = pointers.get(pointerId);
		PShape s = PointerController.getParent().createShape();
		s.beginShape();
		s.noFill();
		//s.stroke((onCalibration)?0x00ff00:p.idColor);
		s.stroke(-1);
		s.strokeWeight(2);
		s.vertex(Math.min( p.servoRangeX[1], Math.max(p.servoRangeX[0], p.corner[0].x)), Math.min( p.servoRangeY[1], Math.max(p.servoRangeY[0], p.corner[0].y)));
		s.vertex(Math.min( p.servoRangeX[1], Math.max(p.servoRangeX[0], p.corner[1].x)), Math.min( p.servoRangeY[1], Math.max(p.servoRangeY[0], p.corner[1].y)));
		s.vertex(Math.min( p.servoRangeX[1], Math.max(p.servoRangeX[0], p.corner[2].x)), Math.min( p.servoRangeY[1], Math.max(p.servoRangeY[0], p.corner[2].y)));
		s.vertex(Math.min( p.servoRangeX[1], Math.max(p.servoRangeX[0], p.corner[3].x)), Math.min( p.servoRangeY[1], Math.max(p.servoRangeY[0], p.corner[3].y)));
		s.endShape(PConstants.CLOSE);
		return s;
	}

	//############################################# COORDINATE CONVERSIONS #######################################

	//this function modifies the pointer coord pvector directly
	//incoming should be mouse/screen/canvas coord system
	public static PVector pointerTrasformations(PointerDevice p, float _x, float _y){

		double [][] matrix = p.hMatrix;
		PVector transformed = new PVector();
		//change x and y
		transformed.x = (p.setYasX)? _y : _x;  //toTarget	
		transformed.y = (p.setYasX)? _x : _y;	 //toTarget

		//System.out.println("in_p:" + p.x + ";" + p.y);

		//invert x or y (flip)
		if(p.invertX) transformed.x = flipX(transformed.x); //toTarget
		if(p.invertY) transformed.y = flipY(transformed.y); //toTarget

		//homographic plane
		if(p.enableHomography || forceHomographyOnCalibration){
			//PVector h = toHomography(new PVector(p.x, p.y), i);
			PVector h = toHomography(transformed,matrix); //toTarget
			transformed.set(h);  //toTarget
		}else{
			//if(i == 0) System.out.println("in _y:" + _y);
			p.target.x = toServos(fromScreen( transformed)).x;  //no need to use copy() because toServo breaks pvector instance. //toTarget
			p.target.y = toServos(fromScreen( transformed)).y; //toTarget
			//if(i == 0) System.out.println("out _y:" + _y);
		}
		
		return transformed;
	}


	public int servoLimit(float value) {
		return (int)Math.max(0, Math.min(value, 180));
	}

	//	static PVector toScreen(PVector input){
	//		PVector in = input.copy();
	//		in.add(new PVector(controllerScreenPosition[0],controllerScreenPosition[1] ));
	//		return new PVector();
	//	}



	// TO VALUES FROM NORMAL

	public static PVector toScreen( PVector in ){ //input must be range from 0 to 1
		float _x =  in.x * screenSize[0] ;
		float _y =  in.y * screenSize[1] ;
		return  new PVector( _x, _y );
	}

	public static PVector toServos( PVector in ){ //input must be range from 0 to 1
		float x =  in.x * 180;
		float y =  in.y * 180;
		return  new PVector( x, y );
	}

	public static PVector toCanvas( PVector in, int canvasId) {
		float x =   in.x * canvasSize[0] + canvasPosition[0] ;
		float y =   in.y * canvasSize[1] + canvasPosition[1] ;
		return  new PVector( x, y );
	}

	public static PVector toHomography(PVector in, double[][] hMatrix) {
		//System.out.println("toHomography input:" +  in.x + "," + in.y);
		return HomographyMatrix.solve(in, hMatrix);
	}

	public PVector toHomography(PVector in, int pointerId) {
		return toHomography(in, pointers.get(pointerId).hMatrix);
	}

	// FROM VALUES TO NORMAL

	public static PVector fromScreen( PVector in ) {
		float x =  PApplet.norm( in.x, 0, screenSize[0] );
		float y =  PApplet.norm( in.y, 0, screenSize[1] );
		return  new PVector( x, y );
	}

	public static PVector fromServos( PVector in ) {
		float x =  PApplet.norm( in.x, 0, 180 );
		float y =  PApplet.norm( in.y, 0, 180 );
		return  new PVector( x, y );
	}

	public static PVector fromCanvas( PVector in, int canvasId) {

		float x =  PApplet.norm( canvasPosition[0], 0f, canvasPosition[0]  + canvasSize[0] );
		float y =  PApplet.norm( canvasPosition[1], 0f, canvasPosition[1]  + canvasSize[1] );
		return  new PVector( x, y );
	}

	public static PVector fromCanvas( PVector in) {
		//TODO - canvasId = 0; see GUI
		return  fromCanvas( in, 0 );
	}

	public static float flipX(float _x){
		if(usingCanvas()){
			return PApplet.map(_x, 0f,canvasSize[0],canvasSize[0],0f);
		}else{
			return PApplet.map(_x, 0f,screenSize[0],screenSize[0],0f);
		}
	}

	public static float flipY(float _y){
		if(usingCanvas()){
			return PApplet.map(_y, 0f,canvasSize[1],canvasSize[1],0f);
		}else{
			return PApplet.map(_y, 0f,screenSize[1],screenSize[1],0f);
		}
	}


	////converts a value from servo (0~180) to (0~width) (to be used to extract coords from servos, including homography points )
	//PVector fromServos( PVector out ) {
	//  return fromServos( out, new int[]{ 0, 180 }, new int[]{ 0, 180 } ); //default servo
	//}

	//PVector fromServos( PVector out, int[] rangeX, int[] rangeY ) {
	//  float x = map( out.x, rangeX[0], rangeX[1], 0, width );
	//  float y = map( out.y, rangeY[0], rangeY[1], 0, height );
	//  return new PVector(x, y);
	//}
	
	
	//##############################################  JSON PARSER  ##################################################
	
	public Action[] getHersheyActions(char c){
		return getHersheyActions(c - 0x0);
	}
	
	public Action[] getHersheyActions(int ascii){
		int CONST = 33; //offset from ascii
		
		//JSONParser parser = new JSONParser();		
		//JSONObject obj = (JSONObject) parser.parse(new FileReader("hershey.json"));
		
		//String test = (String) obj.get("ascii");
		
		//int[] a = simplex[ c - CONST ] ;  //get character from the list
		//  int charWidth = a[1];
		//  int [] last = {a[2], a[3]}; //initializes with first position
		
		JSONObject obj = null;
		try {
			obj = new JSONObject( new FileReader("hershey.json") );
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String pageName = obj.getJSONObject("pageInfo").getString("simplex");

		JSONArray arr = obj.getJSONArray("posts");
		
		
		
		//TODO
		Action set[] = new Action[1];
		
		return set; 
	}
	
	
	
	
	

	//############################################# POINTERS CONFIG SAVE LOAD DATA  #######################################

	public void savePointers() {

		try {
			//ckDirectory:
			File directory = new File(parent.sketchPath("Pointers"));
			if(!directory.exists()) directory.mkdir();
			File file =  new File( parent.sketchPath("Pointers/config.txt") ) ;
			config = new PrintWriter(file);
			config.print( "config " + pointers.size());
			config.println("; ");


			for ( PointerDevice p : pointers ) {

				config.print( "#POINTER " + p.id);
				config.print("; ");

				config.print( "active " + p.active);
				config.print("; ");

				config.print( "invertX " + p.invertX);
				config.print("; ");

				config.print( "invertY " + p.invertY);
				config.print("; ");

				config.print( "setYasX " + p.setYasX);
				config.print("; ");

				///----------------------------------------- projection matrix p2d points
				//TODO - not sure if using?
				for ( int i = 0; i < 6; i++) {
					config.print( "P"+ i + " " + p.p2d[i].x + " " + p.p2d[i].y);
					config.print("; ");
				}


				///----------------------------------------- homography defined corners 
				for ( int i = 0; i < 4; i++) {
					config.print( "C"+ i + " " + p.corner[i].x + " " + p.corner[i].y);
					config.print("; ");
				}

				config.println("");
			}


		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally  {
			if ( config != null ) 
			{	
				config.flush();
				config.close();
				System.out.println("pointers confg saved");
			}
		}
	}

	public void loadPointers(){

		File file =  new File( parent.sketchPath("Pointers/config.txt") ) ;
		try {
			int numberOfPointer; //on file
			pointers = new ArrayList<PointerDevice>(); //refresh array (deleting all previous pointers)
			int lineNum = 0;
			reader = new BufferedReader( new FileReader(file) );
			PointerDevice pointer;

			for (String next = "", line = reader.readLine(); line != null; line = next) {

				String[] pieces = line.split(";"); // pieces are ("pointId pointX pointY")

				if(lineNum == 0){
					numberOfPointer = (int) Float.parseFloat( pieces[0].split(" ")[1] );
				}
				else if (lineNum > 0) { // 
					if (useMaxPointerLimit) {
						if (lineNum  > maxPointer) {
							break;
						}
					}

					if(false){
						//CAMERA
					}else{

						pointer = new PointerDevice();

						int k = 1; // k = 0 contains the pointer id. starts with 1
						//pointer.id            = int ( split(pieces[0], " ")[1] );

						pointer.active        = ( pieces[k++].split(" ")[2].equals("true") )? true : false;
						pointer.invertX       = ( pieces[k++].split(" ")[2].equals("true") )? true : false;
						pointer.invertY       = ( pieces[k++].split(" ")[2].equals("true") )? true : false;
						pointer.setYasX       = ( pieces[k++].split(" ")[2].equals("true") )? true : false;

						////----------------------------------------- projection matrix read
						for (int i = 0; i < 6; i++) {
							pointer.p2d[i] = new PVector();
							pointer.p2d[i].x = Float.parseFloat( pieces[k].split(" ")[2] );
							pointer.p2d[i].y = Float.parseFloat( pieces[k].split(" ")[3] );
							k++;
						}

						////----------------------------------------- homography corners read
						for (int i = 0; i < 4; i++) {
							pointer.corner[i] = new PVector();
							pointer.corner[i].x = Float.parseFloat( pieces[k].split(" ")[2] );
							pointer.corner[i].y = Float.parseFloat( pieces[k].split(" ")[3] );
							//System.out.println("load corner " + i + ": " + pointer.corner[i].x + ", " + pointer.corner[i].y);
							k++;
						}

						//						pointer.defaultSquaredPlane = new PVector[]{
						//								new PVector(pointer.servoRangeX[0], pointer.servoRangeY[0]), 
						//								new PVector(pointer.servoRangeX[1], pointer.servoRangeY[0]), 
						//								new PVector(pointer.servoRangeX[1], pointer.servoRangeY[1]), 
						//								new PVector(pointer.servoRangeX[0], pointer.servoRangeY[1]) 
						//								};

						pointer.buildHMatrix();

						pointers.add(pointer);   
					}
				}
				lineNum++;
				next = reader.readLine();
			}
			System.out.println("Matrix loaded");


		}catch (IOException e) {
			e.printStackTrace();
		}

		pointers.get(0).printCorners();
	}
}
