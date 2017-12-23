/**
 * Pointer device controller
 *
 * @author       Anderson Sudario
 * @version      1.0
 * 2017
 */

package bontempos.PointerDevice;

import java.util.ArrayList;

import bontempos.Game.Act.Action;
import bontempos.Game.Act.ActionList;
import bontempos.Game.Act.Checker;
import bontempos.Game.Act.Countdown;
import bontempos.ProjectionMatrix.HomographyMatrix;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PShape;
import processing.core.PVector;


/*
 *  TODO - > moveTo will include laser parameter as optional - thnik about how to add laser of actions to list or not
 */

public class PointerDevice extends PVector  {


	public final int laserMaxBrightness 	  =   255;	// 		0xFF; 	//(byte) 255;  // int i = laser & 0xff;
	public final float[] servoRangeX          =   {0f, 180f}; 
	public final float[] servoRangeY          =   {0f, 180f};
	public final float   targetDistTolerance  =   1f;        //detects when servos needs to start moving
	public final PVector initialPosition 	  =   new PVector(90,90);	//middle of servo (if 180)
	public float  servoSpeed           =   120f/60f; //in 120ms it moves 60º

	public boolean echo	 			  = false;

	//LASER
	public int id;
	public int laser, plaser;
	public boolean allowLaserGradient  =   false;					//fragments laser intensity (if different values) while moving btw different positions
	public boolean canDraw			   =   true;					//
	public boolean useLines			   =   true;					//draws connecting current and previous position
	
	//MOVEMENT
	boolean onMovement				   =   false;
	float speed                        =   1f;        				//increment mult for speed
	boolean enableAccel                =   false;
	boolean allowRetargeting           =   false;      		     	//if allowRetargeting is true and target changes, trajectory will be recalculated and restarted
	boolean enableHomography           =   true;
	int moveInterval;
	int moveStartTime;
	boolean onTarget                   =   false;          			//when false, calculates a trajectory path to the target and set moving to true
	boolean waiting                    =   false;            		//paused by some countdown or timer progress

	ArrayList<PVector> targetList 	   =   new ArrayList<PVector>();//list of (extra/stored) targets to perform
	PVector target                     =   new PVector();			//current target (the last in the target list). Target can't never be null. This is how system knows if must start moving or its idle, comparing current position and target
	PVector ppos                       =   new PVector();  			//last frame/step position
	ArrayList<PVector>trajectoryPoints;		
	int trajectorySteps;

	//STATE FLAGS
	boolean invertY                    =   false;
	boolean invertX                    =   false;
	boolean setYasX                    =   false;          			 //changes Y axis to X and vice versa
	boolean active                     =   true;
	boolean error;

	//VISUAL AID
	int idColor                        =   0xffaa55dd;
	int idColorError                   =   0x00ff0000;
	PShape activeArea;


	//CALIBRATION
	//boolean onCalibration               =   false;					//homographic and projMatrix setting of vertices

	//HOMOGRAPHY
	float[] fpos                        =   new float[2];   		//final position (if homography is used)
	float[] pfpos                       =   new float[2];   		//previous final position
	double[][] hMatrix;
	PVector [] defaultSquaredPlane      =    
			//input plane for homography eg. screen size, mouse manipulation window, etc
		{    
				new PVector(),
				new PVector(PointerController.screenSize[0], 0),
				new PVector(PointerController.screenSize[0], PointerController.screenSize[1]),
				new PVector(0, PointerController.screenSize[1]) //<-- screen size frame
		};
	PVector [] corner                  =     
			//out plane for homography  eg. distorted plane where pointers are writting on. 
			//Negative values or values greater than servoLimit( 180 degrees ) are clamped on serialOutput
		{
				new PVector(servoRangeX[0], servoRangeY[0]),
				new PVector(servoRangeX[1], servoRangeY[0]),
				new PVector(servoRangeX[1], servoRangeY[1]),
				new PVector(servoRangeX[0], servoRangeY[1])
		};

	//PROJECTION MATRIX
	PVector[] defaultZero() {
		return new PVector[]{new PVector(), new PVector(), new PVector(), new PVector(), new PVector(), new PVector()};
	}

	PVector[] p2d = defaultZero();
	PVector[] p3d = defaultZero();



	//SERIAL
	String serialOut = "";
	boolean serialUpdating = false;


	//EVENTS AND LISTNERS
	Checker detectLaserChange;
	Checker detectMoveComplete;
	public ActionList actionList; 
	public String evalNext ; // this replaces the "evalNext" for loading next action in a list of actions. This string must includ pointer id.


	public PointerDevice() {

		actionList = new ActionList();

		PointerController.getParent().registerMethod("draw", this);
		this.x = initialPosition.x;
		this.y = initialPosition.y;
		target.x = x; //stabilizing position (current == target)
		target.y = y; //stabilizing position (current == target)
		id = PointerController.pointers.size();
		detectLaserChange = new Checker("laserChange"+id, this, "onLaserChange"); 
		detectLaserChange.setPermanent(true); //TODO why not set checker permanent as default later
		detectMoveComplete = new Checker("moveCompleted"+id, this, "onMoveComplete"); 
		detectMoveComplete.setPermanent(true);
		evalNext = "line_P"+id; //identifier to move to next action for this pointer ( when there are many pointers, "evalNext" actions must have a unique prefix for each pointer )
		actionList.setEvalName( evalNext ); 
		actionList.setAutoClear( true );
		activeArea =  cornerShape(); //tmp
		buildHMatrix();
		trajectoryPoints = new ArrayList<PVector>(); //TODO this might be not the right place (needs a "clear" when all pointers have no trajectory scheduled)
		System.out.println("Pointer " + id + " created");
	}


	public void draw() {
		if(active){
			update();
			//if(canDraw) drawOnCanvas(); //check inside updateMove()
		}
	}


	void update(){
		//what if we do it here?
		ppos = new PVector(x,y); 
		updateMove(); //if target is different of current position
		if(canDraw) drawOnCanvas();
		//return feedback - or visual aid


	}


	//---------------------------------- < SETTERS > ----------------------------------------
	public void setIdColor( int idColor ) {
		this.idColor = idColor;
	}

	public void setYasX ( boolean bool ){
		this.setYasX = bool;
	}

	public void setInvertX ( boolean bool ){
		this.invertX = bool;
	}

	public void setInvertY ( boolean bool ){
		this.invertY = bool;
	}

	public void setServoSpeed ( float servoSpeed ){
		this.servoSpeed = servoSpeed;
	}

	public void setLaserGradient( boolean bool ){
		this.allowLaserGradient = bool;
	}
	
	public void setLaserGradient( boolean bool , int millis){
		this.allowLaserGradient = bool;
	}
	//-------------------------------< GETTERS >-----------------------------------------------

	public float getServoSpeed() {
		return servoSpeed;
	}


	public void printCorners(){
		System.out.println("Pointer " + id + " corners:");
		for (int i = 0; i < 4; i++) {
			System.out.println( "["+i+"]: " + corner[i] );	
		}
	}

	public void printDefaultSquaredPlane(){
		System.out.println("Pointer " + id + " defaultSquaredPlane:");
		for (int i = 0; i < 4; i++) {
			System.out.println( "["+i+"]: " + defaultSquaredPlane[i] );	
		}
	}

	public int getLaser(){
		return laser;
	}
	
	//---------------------------------- <    ACTIONS     >-------------------------------------

	// moveTo( target ), moveTo( target, laser ), laserOn(), laserOff(), wait()

	//---------------------------------- <    MOVE     >-------------------------------------

	protected void alignToTarget(){
		onTarget = true;
		this.x = target.x;
		this.y = target.y;
	}

	protected void removeTarget(){
		targetList.remove(target);
	}

	protected void addTarget(PVector newTarget){
		if(echo) System.out.println("added to list, now: " + targetList.size());
		targetList.add(0,newTarget);
	}

	public void setTarget(PVector newTarget){
		target = newTarget.copy();
	}

	public void setTargetDraw(PVector newTarget, float laserIntensity){
		
		target = newTarget.copy();
		setLaser(laserIntensity);
		
		//this function would only call next action if moveComplete action happens.
		//that is not possible if target == current position (meaning only a laser change).
		//so, after target is set, we test if still overTarget returns true. If yes, we can force moveComplete().
		if(overTarget()){
			onMoveComplete();
		}
		
	}

	protected void updateMove() {
		


		if(!overTarget()){ //below can only happen when target and current position are not aligned

			//pointer is on movement or on pre-movement (calculating new trajectory)

			if(!onMovement){

				//1 - pre-movement, setting a new trajectory and start moving

				calculateServoTrajectory();
				setOnMovement(true);

			}else{

				//2 - pointer has a trajectory to move and its moving.

				if(!trajectoryPoints.isEmpty()){

					//3 - there are targets in a fragmented trajectory to move to
					PVector closerPosition = trajectoryPoints.get( trajectoryPoints.size() - 1 );
					set( closerPosition.copy() );
					//System.out.println("Pointer "+ id +": moving closer: " + x + "," + y + "[" + trajectoryPoints.size() +" steps left]");

					//temporary display for debug
					PointerController.parent.stroke(0xffff0000);
					PointerController.parent.strokeWeight(4);
					PointerController.parent.point(x, y);
					

				}else{

					//4 - all targets in a fragmented trajectory was performed but current position and final target are not aligned
					if(echo) System.out.println("Pointer "+ id +": trajectoryPoints is empty, should be overTarget. Current:"+this+ ", target;" + target);
					alignToTarget();
				}
			}
		} else {

			//temporary display for debug (stopped cursor)
			PointerController.parent.stroke(-1);
			PointerController.parent.strokeWeight(3);
			PointerController.parent.point(x, y);

			//goal
			alignToTarget(); //align if not
			if(onMovement) setOnMovement(false); //dispatches actions for movementCompleted

			//check if there are other targets to go stored on a list
			//			if(!targetList.isEmpty()){
			//				target = targetList.get( targetList.size() - 1 ); //by doing this, target != current position so movement starts
			//				targetList.remove(target);
			//			}

			//check if there are other actions to perform stored on actionList and if not waiting for any countdown
			//			if(!actionList.isEmpty()){
			//				if(!waiting) {
			//					//nextAction(); 
			//				}else{
			//					System.out.println("waiting: " + PointerController.parent.millis() );
			//				}
			//			}

		}
	}

	//shortcut with default
	public void moveToDraw(PVector newTarget , float laserIntensity){
		moveToDraw ( newTarget, laserIntensity, true);
	}

	
	//this function is called by user. It moves the pointer to a new target while sets laser intensity.
	//if this function is called without position offset (target == current position), only laser intensity will be evaluated
	//this function can be stacked 
	public void moveToDraw( PVector newTarget , float laserIntensity, boolean useTransformations){ //TODO set laser on and move.	
		if(useTransformations){
			newTarget = PointerController.pointerTrasformations(this,newTarget.x, newTarget.y); 
		}
		addAction( new Action("draw", this, "setTargetDraw", newTarget, laserIntensity) );
	}


	//shortcut with default
	public void moveTo( PVector newTarget ){
		moveTo(  newTarget, true );
	}

	
	public void moveTo( PVector newTarget , boolean useTransformations){
		if(echo) System.out.println("Pointer "+ id +": moveTo " + newTarget);

		//if using transformation, transforming from screen coord to servo:
		//TODO currently it only works with servo coord system in values
		if(useTransformations){
			newTarget = PointerController.pointerTrasformations(this,newTarget.x, newTarget.y); 
		}


		if( allowRetargeting && onMovement ){
			//terminate movement - so new trajectory can be calculated
			setOnMovement(false);
			alignToTarget();
			//set new target;
			target = newTarget.copy();
		}else{
			if( overTarget() ){ //testing statement
				if(echo) System.out.println("Pointer "+ id +": setting target");
				target = newTarget.copy();
			}else{
				if(echo) System.out.println("### adding new target while not over target");
				//store movement on a list to execute after current movement is finished
				//addTarget(newTarget); //TODO replace this by adding action in a list ?
				addAction( new Action("move", this, "setTarget" , newTarget) );
				//addActionSet( new Action("drawLine", this, "moveTo" , newTarget) );
			}
		}
	}



	private float roundTo(float value, int decimalPlaces)
	{
		//round to decimal places
		double shift = Math.pow(10, decimalPlaces);
		return (float) (Math.round(value*shift)/shift);
	}


	boolean overTarget(){
		//rounding to 3 decimals

		//System.out.println("Pointer "+ id + ": overTarget ("+(target.dist(this) < targetDistTolerance)+"): " + this + ", " + target );
		PVector targetXY = new PVector( roundTo(target.x,3), roundTo(target.y,3) );
		return targetXY.dist( new PVector(roundTo(x,3), roundTo(y,3)) ) < targetDistTolerance;
	}


	boolean overTimer() {
		return PointerController.parent.millis() > (moveStartTime + moveInterval);
	}


	void calculateLaserGradient(){
		//TODO
	}

	void calculateServoTrajectory() {

		//happens once before movement starts
		//creates a fragmented trajectory for servos
		//this fragmented trajectory is stored on a list and updated by the pc.servoUpdateRate

		float travelX = Math.abs(target.x - x); //suppose 90º
		float travelY = Math.abs(target.y - y); //suppose 180º;
		int timeX = (int)(travelX * servoSpeed); //180ms
		int timeY = (int)(travelY * servoSpeed); //360ms (if realSpeed = 2)

		if(echo) System.out.println("Pointer "+ id +" --> calculation starts for pointer "+ id +": " + PointerController.parent.millis());
		if(echo) System.out.println("going to "+ x + "," + y + " to " + target.x + "," + target.y);

		moveInterval = PApplet.max(timeX, timeY); //picking up the slower one (360ms)
		onTarget = false;

		//##   instead of sending data to servo, we need to create a list of trajectory points fragmented from target
		//##   and keep sending data to serial constantly

		//how many servoupdates are necessairy to perform this trajectory?
		trajectorySteps =  (int) Math.ceil ( moveInterval/PointerController.getServoUpdateRate() ); // we need to consider the rest of a division ( 18 )

		//TODO using acceleration
		// • using acceleration
		if (enableAccel) {
			//float[] t = Tween.easeInOut2d( transition, new float[]{ipos.x, ipos.y}, new float[]{target.x-ipos.x, target.y-ipos.y}, 1f);               
			//set(t[0], t[1]);                                                            // <--  THIS LINE SETS THE CURRENT POSITION TO THESE VALUES
		} 

		// • using uniform movement
		else {
			//	        Action releaseMotor = new Action( this, "setServoBusy", false );
			//	        new Countdown( (int) servoT, releaseMotor ).start();
			//	        set(  PVector.lerp(ipos, target, transition)  );                            // <--  THIS LINE SETS THE CURRENT POSITION TO THESE VALUES
		}

		//How about the function, vertical coordinate is nearest integer to (5/14 times horizontal coordinate) y = round(h/w* x)

		float xFragment =  travelX / trajectorySteps ;  // suppose 180/18 = 10
		float yFragment =  travelY / trajectorySteps ;  // suppose 180/18 = 10

		//directions from current to target;
		int signalX = (target.x >= x)? 1 : -1;
		int signalY = (target.y >= y)? 1 : -1;

		//initial position (current)
		float init_x = x;
		float init_y = y;

		for (int i = 0; i < trajectorySteps; i++) {

			init_x = init_x + xFragment * signalX;
			init_y =  init_y + yFragment * signalY;
			trajectoryPoints.add( 0 ,new PVector(Math.round(init_x),Math.round(init_y),laser )); //note the order

		}

		if(echo) System.out.println("Pointer "+ id +": trajectorySteps: " + trajectorySteps);

		moveStartTime = PointerController.parent.millis();

	}



	public void setOnMovement(boolean b) {
		//System.out.println(" id:"+id+" set to: "+b+" onMovement is " + onMovement);
		if (!b && onMovement) {
			Action.perform("moveCompleted"+id);
		}
		onMovement = b;
	}


	public void onMoveComplete() {
		ppos = new PVector(x,y); //update last position, well...does not include the fragmented trajectory.
		if(echo) System.out.println("\n >>> Pointer "+ id + ": " + PointerController.parent.millis() + " move is completed");
		nextAction(); //this action dispatches next line in a list if list is not empty
	}



	//---------------------------------- <    LASER     >-------------------------------------
	
	//TODO - prefix names with "ACT" for programmable actions
	public void actLaserOff(){
				addAction( new Action("LaserOff", this, "setLaserNextAction", 0) );
	}
	
	public void actLaserOn(){
		addAction( new Action("LaserOff", this, "setLaserNextAction", PointerController.getMaxLaserBrightness()) );
	}
	
	public void setLaserNextAction(float value){
		setLaser(value);
		nextAction();
	}

	public void setLaser(float value) {
		if(echo) System.out.println("Laser: " + value);
		laser = (int)value;
		
		Action.perform("laserChange"+id);
	}


	public void onLaserChange() {
		if(laser != plaser){
			plaser = laser;
			System.out.println("LASER CHANGED "+ laser);
			
			int c = (laser==0f)?0x55ff9999:0x555555ff;
			PointerController.canvas.drawPoint( new PVector(x,y),c);
			
			//nextAction(); //when used with moveToDraw, it was calling next call twice, and performing 2 actions at same time
		}
	}

	//---------------------------------- <    WAIT     >-------------------------------------

	public void delayAction(int timeToDelay){
		//this is performed by action in action list : will basically perform an action to call next action after a countdown.
		waiting = true;
		System.out.println("----------------start waiting for " + timeToDelay);
		new Countdown( timeToDelay, new Action(this, "stopWait")).start(); //TODO for some reason i cant call the function nextAction() directly
	}

	public void wait(int timeToWait){
		//used in actionlist to insert some delay between actions.
		System.out.println("#### adding in list: waiting for " + timeToWait);
		addAction( new Action("delay", this, "delayAction" , timeToWait) );

	}

	public void stopWait() {
		waiting = false;
		nextAction();
	}
	
	//---------------------------------- <    DRAW VECTOR SHAPES AND CHARACTERS     >-------------------------------------
	public void drawChar( char c ){
		int charWidth; 
		PVector lastPos = new PVector();
		int[] code = PointerController.getHersheyCode(c);
		
		charWidth = code[1];
		lastPos.x = code[2];
		lastPos.y = code[3];
		
		System.out.println("lastPos.x:" + lastPos.x +",lastPos.y:"+ lastPos.y );
		
		//fix initial position
		lastPos = getCharCodePosition(lastPos, charWidth); //?? last position is contaminated by absolute prompt position??
		
		//LASER OFF:
		actLaserOff();
		
		//move to initial position
		
		System.out.println("moveToIniPos:  lastPos: "+ lastPos );
		moveToDraw( lastPos, 0);
		wait(500); 
		
		for (int i = 2; i < Math.min(code[0]*2 + 1 , 112) ; i+=2) {  //loop starts on index 2 and iterates skipping 1 line
			//inside this loop commands to draw lines or just move to places will be performed:
			
			PVector offsetPos = getCharCodePosition( new PVector(code[i], code[i+1]), charWidth);
			System.out.println("position:" + i +", code[i]:"+ offsetPos.x + " code[i+1]:" + offsetPos.y);
			
			
			//DRAW LINE
			if (code[i] != -1 && lastPos.x != -1 ) { 
				//System.out.println("  > drawLine:  code[i]:"+ offsetPos.x + " code[i+1]:" + offsetPos.y);
				moveToDraw( offsetPos, laserMaxBrightness );
			}
			
			//LASER OFF
			else if (code[i] == -1){
				//System.out.println("  > Stop:  code[i]:"+ offsetPos.x + " code[i+1]:" + offsetPos.y);
				wait(100);
				actLaserOff();
				wait(100);
			}
			
			//MOVE TO
			else if  ( lastPos.x == -1 ) {
				//System.out.println("  > moveTo:  code[i]:"+ offsetPos.x + " code[i+1]:" + offsetPos.y);
				moveToDraw( offsetPos, 0 );
				wait(250);
			}
						
			else{
				//System.out.println("this case also exists: code[i]:"+code[i] + " lastPos.x:" + lastPos.x);
			}
			lastPos.x = code[i];
			lastPos.y = code[i+1];
			
		}
		//FINISH 
		actLaserOff();
		wait(250);
		//set character new position;
		
		nextAction();
	}
	
	
	PVector getCharCodePosition( PVector lastPos , int charWidth ){
		PVector charPos = PointerController.promptPosition;
		float s = PointerController.charSize;
		if(PointerController.charCenterAlign){
			return new PVector (  charPos.x + (lastPos.x-(charWidth/2))*s , charPos.y+lastPos.y*s ); //y inverted
		}else{
			return new PVector (  charPos.x + lastPos.x*s , charPos.y + lastPos.y*s ); //y inverted
		}
	}
	
	
	public void drawRectangle(){
		moveToDraw( new PVector(servoRangeX[0], servoRangeY[0]), 0);
		wait(500); 
		moveToDraw( new PVector(servoRangeX[1], servoRangeY[0]), laserMaxBrightness);
		moveToDraw( new PVector(servoRangeX[1], servoRangeY[1]), 1);
		moveToDraw( new PVector(servoRangeX[0], servoRangeY[1]), laserMaxBrightness);
		moveToDraw( new PVector(servoRangeX[0], servoRangeY[0]), laserMaxBrightness);
		actLaserOff();
		wait(500); 
		//moveToDraw( new PVector(servoRangeX[0], servoRangeY[0]), 0);
		moveToDraw( new PVector(90,90), 0);
		nextAction();
	}
	
	//---------------------------------- <  NEXT ACTION IN LIST  >-------------------------------------

	public void nextAction(){
		if(!actionList.isEmpty()){
			if(echo) System.out.println( PointerController.getParent().millis() +  " calling next action");
			Action.perform( evalNext ); //performs next action 
		}else{
			if(echo) System.out.println( "no action to perform." );
		}
	}


	//---------------------------------- <  ACTION LIST   > ------------------------------------

	public void addActionSet(Action [] a){
		actionList.addSet(a);
	}

	public void addAction(Action  a){
		actionList.add(a);
	}

	public void clearActionList(){
		actionList.clear();
	}

	//---------------------------------- <  HOMOGRAPHY  >-------------------------------------

	void buildHMatrix() {
		hMatrix = null;
		if(echo) System.out.println("buildHMatrix");
		while (hMatrix == null) {
			//printDefaultSquaredPlane();
			//printCorners();
			hMatrix = HomographyMatrix.get(defaultSquaredPlane, corner); 
			if (hMatrix == null) {
				System.out.println("PointerDevice Class: Failed to create Homography plane");
			}
		}
	}


	public void updateCorner( int k , PVector newPosition) {
		corner[k-1].set( newPosition.x, newPosition.y );
		buildHMatrix();
	}



	PShape cornerShape() {

		PShape s = PointerController.getParent().createShape();
		s.beginShape(PConstants.QUAD);
		s.noFill();
		//s.stroke((onCalibration)?0x00ff00:idColor);
		s.strokeWeight(1);
		PVector A = new PVector(Math.min( servoRangeX[1], Math.max(servoRangeX[0], corner[0].x)),Math.min( servoRangeY[1], Math.max(servoRangeY[0], corner[0].y)));
		PVector B = new PVector(Math.min( servoRangeX[1], Math.max(servoRangeX[0], corner[1].x)), Math.min( servoRangeY[1], Math.max(servoRangeY[0], corner[1].y)));
		PVector C = new PVector(Math.min( servoRangeX[1], Math.max(servoRangeX[0], corner[2].x)), Math.min( servoRangeY[1], Math.max(servoRangeY[0], corner[2].y)));
		PVector D = new PVector(Math.min( servoRangeX[1], Math.max(servoRangeX[0], corner[3].x)), Math.min( servoRangeY[1], Math.max(servoRangeY[0], corner[3].y)));
		
		if(echo){
		System.out.println("cornerShape()");
		System.out.println("A: " + A.x +", "+ A.y);
		System.out.println("B: " + B.x +", "+ B.y);
		System.out.println("C: " + C.x +", "+ C.y);
		System.out.println("D: " + D.x +", "+ D.y);
		}

		s.vertex(A.x, A.y);
		s.vertex(B.x, B.y);
		s.vertex(C.x, C.y);
		s.vertex(D.x, D.y);

		s.endShape();
		return s;
	}
	
	
	//---------------------------------- <  DRAWING, DISPLAY  >-------------------------------------
	private void drawOnCanvas(){
		PointerController.canvas.drawPointer(this);
	}
	


}

