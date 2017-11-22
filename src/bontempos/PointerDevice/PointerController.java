/*
 * TODO every place using fromScreen - could be fromCanvas <-- make some automatic function to select among them.
 * 
 */

package bontempos.PointerDevice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;


import bontempos.Game.Act.Act;
import bontempos.ProjectionMatrix.HomographyMatrix;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PShape;
import processing.core.PVector;
import processing.event.KeyEvent;
import processing.serial.Serial;


public  class PointerController {

	public static int baudRate = 38400; //230400;
	public static String serialInput = null;
	public static String cmdOutput = "";
	public static PointerDevice selectedPointer;
	public static ArrayList<PointerDevice> pointers  = new ArrayList<PointerDevice>();

	public static int [] screenSize  = {100,100};
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
	
	public boolean isOnCalibration(){
		return onCalibration;
	}

	//############################################# < SETTERS > ##################################################

	public static void setParent(PApplet parent) {
		PointerController.parent = parent;
	}
	
	public void setCalibration(boolean bool){ //int pointId
		onCalibration = bool;
//		for(int i = 0; i < pointers.size(); i++){
//			PointerDevice p = pointers.get(pointId);
//			p.onCalibration = false;
//			if(p.id == pointId) {
//				p.onCalibration = bool;
//			}
//		}
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

	public  void setCanvasPosition( int x, int y){
		canvasPosition[0] = x;
		canvasPosition[1] = y;
	}


	//##############################################  CONTROLLER INIT  ##################################################


	public PointerController(PApplet parent, int baudRate) {
		PointerController.parent = parent;
		PointerController.baudRate = baudRate;
		setScreenSize(parent.width, parent.height); //default
		parent.registerMethod("draw", this);
		parent.registerMethod("keyEvent", this);
		forceHomographyOnCalibration = false;
		act = new Act(parent);
		serialInit(baudRate);
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
			}
			//System.out.println("Pressed key: " + event.getKeyCode());
			break;
		}
		
	}
	
	
	//##############################################  UTILS  #################################################
	
	private boolean moveMove(){
		return (parent.mouseX != parent.pmouseX || parent.mouseY != parent.pmouseY);
	}
	
	private boolean usingCanvas(){
		return (canvasSize[0] > 0 && canvasSize[1] > 0);
	}
	
	
	//##############################################  UPDATE  ##################################################

	public void draw(){

//		for (PointerDevice p : pointers) {
//			if (!p.serialOut.equals("") ) cmdOutput+= p.serialOut;
//		}

		//if (! cmdOutput.equals("") && serial != null ) execute(cmdOutput);
		
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
						System.out.println(k);
						//converting mouse from screen/canvas to servo
						PVector mouse = new PVector(parent.mouseX, parent.mouseY); //pure data with absolute mouse position
						
						if(selectedPointer.invertY) mouse.y = flipY(mouse.y); //TODO - must use Xflip too, of course
						
						selectedPointer.updateCorner( k,   toServos(fromScreen(mouse.copy())) ); //TODO using no flip here but should. But if it does, will display wrong.
						
						if(selectedPointer.invertY) mouse.y = flipY(mouse.y);
						
						update( mouse, (byte)1, pid);
					}
				}
			}else{
				
				
				if(laserOnCalibration){
					laserOnCalibration = false;
				    //after releasing key 1 to 6, after calibration, turns laser off
					update(new PVector(parent.mouseX, parent.mouseY), (byte)0, pid);
				}
				
				byte laserState = selectedPointer.laser;
				
				if( moveMove() ){
					laserOnCalibration = false;
					update(new PVector(parent.mouseX, parent.mouseY), laserState, pid);
				}
			}

		}
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
		//TODO
	}


	//##############################################  SERIAL  ##################################################

	public void serialInit(int baudRate) {
		System.out.println("serialInit");
		serial = new Serial(parent, Serial.list()[1], baudRate );
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
	public void update( byte[] lasers ){
		PVector positions[] = new PVector[activePointers()];
		//for (int i = 0; i < activePointers(); i++) {
		for (int i = 0; i < pointers.size(); i++) {
			float _x = pointers.get(i).x;
			float _y = pointers.get(i).y;
			System.out.println("pointer " + i + ": position:" + _x + "," + _y );
			positions[i] = new PVector(_x,_y);
		}
		update( positions, lasers, false); //retransmiting position with no transformation
	}
	
	
	
	
	//setting ONLY positions (mouse coord system) and keeping laser status
	public void update( PVector[] positions){
		byte lasers[] = new byte[activePointers()];
		for (int i = 0; i < activePointers(); i++) {
			lasers[i] = pointers.get(i).laser;
		}
		update( positions, lasers, true);
	}

	//setting BOTH positions and laser status [multi]
	public void update( PVector[] positions, byte[] lasers){
		update( positions, lasers, true);
	}
	
	public void update( PVector[] positions, byte[] lasers, boolean useTransformations){

		int byteSize =  4 * activePointers();

		for (int i = 0; i < activePointers(); i++) {
			PointerDevice p = pointers.get(i);

			
			if(p.active){

				if(i == 0)  serial.write((byte)byteSize);   //first byte. number of following bytes (index, laser, servo1, servo2)
				
				//pure position from mouse coord system
				float _x = positions[i].x;
				float _y = positions[i].y;
				
				if (useTransformations) {
					pointerTrasformations(  p, _x, _y );
				}
				
				p.laser  = lasers[i];

				//direct data with no trajectory calculation
				serial.write((byte)i);
				serial.write(p.laser);
				serial.write((byte) servoLimit(p.x));   //servo limit constrains values to 0 to 180 before sending to serial
				serial.write((byte) servoLimit(p.y));
				//if(i == 0)	System.out.println("OUT("+sentCommands+")-->:" +", "+ parent.millis() +", "+ i +", "+ p.laser +", "+ servoLimit(p.x) +", "+ servoLimit(p.y));
				
				//update previous status
				p.plaser = p.laser;
				p.ppos = p.copy();
			}
		}
		sentCommands++ ;
	}

	//setting specific Pointer
	//avoid using this in a loop inside processing. Instead, send array of values to function above.
	//position comes in mouse coord system
	public void update( PVector position, byte laser, int pointerId){
		byte lasers[] = new byte[activePointers()];
		PVector positions[] = new PVector[activePointers()];
		for (int i = 0; i < activePointers(); i++) {
			PointerDevice p = pointers.get(i);
		
			if(p.id == pointerId){
				//inserting new data on specific pointer ( position is in screen coord system converting to servo coord)
				//System.out.println("Pointer "+ i +": new position:" + p.x + ", " + p.y);
				pointerTrasformations( pointers.get(pointerId) , position.x, position.y ); //transforming pointer instance position directly 
				p.laser = laser;
			}
			
			//just retransmitting current data from all pointers (expects servo coord system)
		    //System.out.println("Pointer "+ i +": retransmiting position:" + p.x + ", " + p.y);
			//positions[i] = toScreen(fromServos(new PVector(p.x, p.y))); //use toCanvas - if canvas[0] != 0 
			positions[i] = new PVector(p.x, p.y); //Retransmitting servo coord
			lasers[i] = p.laser;
			
		}
		update( positions, lasers, false); //dont use transformation 
	}

	//this function modifies the pointer coord pvector directly
	//incoming should be mouse/screen/canvas coord system
	public void pointerTrasformations(PointerDevice p, float _x, float _y){

		double [][] matrix = p.hMatrix;

		//change x and y
		p.x = (p.setYasX)? _y : _x;
		p.y = (p.setYasX)? _x : _y;

		//System.out.println("in_p:" + p.x + ";" + p.y);

		//invert x or y (flip)
		if(p.invertX) p.x = flipX(p.x);
		if(p.invertY) p.y = flipY(p.y);
		
		//homographic plane
		if(p.enableHomography || forceHomographyOnCalibration){
			//PVector h = toHomography(new PVector(p.x, p.y), i);
			PVector h = toHomography(new PVector(p.x, p.y),matrix);
			p.set(h);
		}else{
			//if(i == 0) System.out.println("in _y:" + _y);
			p.x = toServos(fromScreen( p )).x;  //no need to use copy() because toServo breaks pvector instance.
			p.y = toServos(fromScreen( p )).y;
			//if(i == 0) System.out.println("out _y:" + _y);
		}
	}

	
	
	public void update(){
		//used for test - with same mouse input for all Pointers
		byte lasers[] = new byte[activePointers()];
		PVector positions[] = new PVector[activePointers()];
		for (int i = 0; i < activePointers(); i++) {
			positions[i].set(parent.mouseX,parent.mouseY);
			lasers[i] = pointers.get(i).laser;
		}

		update( positions, lasers);
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
		byte lasers[] = new byte[pointers.size()];
		
		//if(selectedPointer != null) selectedPointer = pointers.get(0);

		int pid = selectedPointer.id;

		for(int i = 0; i < pointers.size(); i++){
			if(pointers.get(i).id == pid){
				System.out.println("Pointer "+ pid +"  laser is:" + pointers.get(pid).laser);
				if( pointers.get(pid).laser == 0){
					lasers[ pid ] = (byte)1;
					System.out.println("Pointer "+ pid +": laser on");
				}else{
					lasers[ pid ] = (byte)0;
					System.out.println("Pointer "+ pid +": laser off");
				}
				//lasers[ pid ] = (pointers.get(pid).laser != (byte)0)?(byte)0:(byte)1;
			}else{
				lasers[ i ] = pointers.get(i).laser;
			}
		}
		for(int i = 0; i < pointers.size(); i++){
			System.out.println( "lasers["+i+"]: "+lasers[i] );
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

	public PVector toHomography(PVector in, double[][] hMatrix) {
		System.out.println("toHomography input:" +  in.x + "," + in.y);
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
	
	public float flipX(float _x){
		if(usingCanvas()){
			return PApplet.map(_x, 0f,canvasSize[0],canvasSize[0],0f);
		}else{
			return PApplet.map(_x, 0f,screenSize[0],screenSize[0],0f);
		}
	}
	
	public float flipY(float _y){
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
							System.out.println("load corner " + i + ": " + pointer.corner[i].x + ", " + pointer.corner[i].y);
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
