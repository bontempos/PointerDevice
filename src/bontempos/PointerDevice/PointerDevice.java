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


public class PointerDevice extends PVector  {

	public final int     laserMaxBrightness   =   255;
	public final float[] servoRangeX          =   {0f, 180f}; 
	public final float[] servoRangeY          =   {0f, 180f};
	public final float   targetDistTolerance  =   1f;        //detects when servos needs to start moving
	public final PVector initialPosition 	  =   new PVector(90,90);	//middle of servo (if 180)
	public float  servoSpeed           =   120f/60f; //in 120ms it moves 60º

	//LASER
	public int id;
	public byte laser, plaser;

	//MOVEMENT
	boolean onMovement				   =   false;
	float speed                        =   1f;        				//increment mult for speed
	boolean enableAccel                =   false;
	boolean allowRetargeting           =   false;      		     	//if allowRetargeting is true and target changes, trajectory will be recalculated and restarted
	boolean enableHomography           =   true;
	int moveInterval;
	int moveStartTime;
	boolean onTarget                   =   false;          			//when false, calculates a trajectory path to the target and set moving to true
	//boolean servoBusy                  =   false;            		//motor is currently executing a trajectory segment and cannot accept other instructions

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
	public ActionList linesToDraw;
	public String evalNext ; // this replaces the "evalNext" for loading next action in a list of actions. This string must includ pointer id.


	public PointerDevice() {

		System.out.println("Pointer");
		linesToDraw = new ActionList();

		PointerController.getParent().registerMethod("draw", this);
		this.x = initialPosition.x;
		this.y = initialPosition.y;
		target.x = x; //stabilizing position (current == target)
		target.y = y; //stabilizing position (current == target)
		id = PointerController.pointers.size();
		detectLaserChange = new Checker("laserChange"+id, this, "onLaserChange"); 
		detectLaserChange.setPermanent(true);
		detectMoveComplete = new Checker("moveCompleted"+id, this, "onMoveComplete"); 
		detectMoveComplete.setPermanent(true);
		evalNext = "line_P"+id;
		linesToDraw.setEvalName( evalNext ); 
		//linesToDraw.setAutoClear( true );
		activeArea =  cornerShape(); //tmp
		buildHMatrix();
		trajectoryPoints = new ArrayList<PVector>(); //TODO this might be not the right place (needs a "clear" when all pointers have no trajectory scheduled)
		System.out.println("Pointer " + id + " created");
	}


	public void draw() {
		if(active){
			update();
		}
	}


	void update(){
		updateMove(); //if target is different of current position

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

	public void setServoSpeed ( int servoSpeed ){
		this.servoSpeed = servoSpeed;
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



	//---------------------------------- <    MOVE     >-------------------------------------

	protected void alignToTarget(){
		onTarget = true;
		this.set(target.x, target.y);
	}

	protected void removeTarget(){
		targetList.remove(target);
	}

	protected void addTarget(PVector newTarget){
		System.out.println("added to list, now: " + targetList.size());
		targetList.add(0,newTarget);
	}

	protected void updateMove() {

		if(!overTarget()){ //below can only happen when target and current position are not aligned

			//pointer is on movement or on pre-movement (calculating new trajectory)

			if(!onMovement){
				//pre-movement, setting a new trajectory and start moving
				calculateServoTrajectory();
				setOnMovement(true);
			}else{
				//pointer has a trajectory to move and its moving.
				if(!trajectoryPoints.isEmpty()){

					PVector closerPosition = trajectoryPoints.get( trajectoryPoints.size() - 1 );
					set( closerPosition.copy() );
					System.out.println("Pointer "+ id +": moving closer: " + x + "," + y + "[" + trajectoryPoints.size() +" steps left]");

					//temporary display for debug
					PointerController.parent.stroke(0xffff0000);
					PointerController.parent.strokeWeight(4);
					PointerController.parent.point(x, y);

				}else{
					System.out.println("Pointer "+ id +": trajectoryPoints is empty, should be overTarget");
					alignToTarget();
				}
			}
		} else {

			//temporary display for debug
			PointerController.parent.stroke(-1);
			PointerController.parent.strokeWeight(3);
			PointerController.parent.point(x, y);

			//goal
			if(!overTarget()){
				alignToTarget(); //align if not
			}
			setOnMovement(false); //dispatches actions for movementCompleted

			//check if there are other targets to go stored on a list
			if(!targetList.isEmpty()){
				System.out.println("\n -->> checking stored target size: " + targetList.size());
				target = targetList.get( targetList.size() - 1 );
				targetList.remove(target);
				System.out.println("removed targed from list. size: " + targetList.size());
				System.out.println("onMovement:" + onMovement + ", overTarget():" + overTarget());
			}
		}
	}

	public void moveTo( PVector newTarget ){
		moveTo(  newTarget, true );
	}

	public void moveTo( PVector newTarget , boolean useTransformations){ //TODO
		System.out.println("Pointer "+ id +": moveTo " + newTarget);
		if( allowRetargeting && onMovement ){
			//terminate movement - so new trajectory can be calculated
			setOnMovement(false);
			alignToTarget();
			//set new target;
			target = newTarget.copy();
		}else{
			if(overTarget() ){ //testing statement
				System.out.println("Pointer "+ id +": setting target");
				target = newTarget.copy();
			}else{
				System.out.println("### adding new target while not over target");
				//store movement on a list to execute after current movement is finished
				addTarget(newTarget);
			}
		}
	}

	//	  void moveTo( float tx, float ty ) {
	//	    /*this function sets a new target usually far from current position. If this is the case, move() function  will take place)*/
	//	    //println("MOVING TO: ", tx, ty );
	//	    if (allowRetargeting && moving) setMoving(false); //if allowRetargeting is true and target changes, trajectory will be recalculated and restarted
	//	    target.set(tx, ty);
	//
	//	    //if an insctruction to move to is given but target and current position are the same, just say the moviment is finished:
	//	    if ( this.dist(target) < targetDistTolerance ) {
	//	     // println("NOT MOVING BECAUSE TOO CLOSE TO TARGET");
	//	      setMoving(false);
	//	    }
	//	  }

	boolean overTarget(){
		//System.out.println("Pointer "+ id + ": overTarget ("+(target.dist(this) < targetDistTolerance)+"): " + this + ", " + target );
		return target.dist(this) < targetDistTolerance;
	}

	boolean overTimer() {
		return PointerController.parent.millis() > (moveStartTime + moveInterval);
	}

	void calculateServoTrajectory() {


		//happens once before movement starts
		//creates a fragmented trajectory for servos

		float travelX = Math.abs(target.x - x); //suppose 90º
		float travelY = Math.abs(target.y - y); //suppose 180º;
		int timeX = (int)(travelX * servoSpeed); //180ms
		int timeY = (int)(travelY * servoSpeed); //360ms (if realSpeed = 2)

		System.out.println("Pointer "+ id +" --> calculation starts for pointer "+ id +": " + PointerController.parent.millis());
		System.out.println("going to "+ x + "," + y + " to " + target.x + "," + target.y);

		moveInterval = PApplet.max(timeX, timeY); //picking up the slower one (360ms)
		onTarget = false;

		//##   instead of sending data to servo, we need to create a list of trajectory points fragmented from target
		//##   and keep sending data to serial constantly

		//how many servoupdates are necessairy to perform this trajectory?
		trajectorySteps =  (int) Math.ceil ( moveInterval/PointerController.getServoUpdateRate() ); // we need to consider the rest of a division ( 18 )


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

		System.out.println("Pointer "+ id +": trajectorySteps: " + trajectorySteps);

		moveStartTime = PointerController.parent.millis();

	}


	/*
	  void move() {
	    //the condition for this function to occur is if target is set far from current position 

	    //if not moving, calculate trajectory first
	    if (!moving) {
	      setMoving(true);                                                                               //toggle move
	      moveInterval = calculateTrajectoryTime();
	    } 

	    //if moving, execute movement.  (TODO -> below should NOT happen at every frame, while motor is still processing last movement)
	    else {


	      ppos.set( this.copy() );                                                     // update last position as current position, than update current position below

	      PointerController.get();
		float transition = PointerController.getParent().norm( PointerController.getParent().millis()-moveStartTime, 0f, Math.max(10, moveInterval) ); 

	      PVector segmentTarget = PVector.lerp(ipos, target, transition);

	      float servoT = calculateServoTrajectoryTime( new PVector(x, y), segmentTarget ) ;

	      //println("movement: ", System.currentTimeMillis(), "transition:", x, y, "to", segmentTarget.x, segmentTarget.y, transition, ", Servo T:", servoT);

	      //println("transition", transition, "moveInterval", moveInterval);

	      // • using acceleration
	      if (enableAccel) {
	        //float[] t = Tween.easeInOut2d( transition, new float[]{ipos.x, ipos.y}, new float[]{target.x-ipos.x, target.y-ipos.y}, 1f);               
	        //set(t[0], t[1]);                                                            // <--  THIS LINE SETS THE CURRENT POSITION TO THESE VALUES
	      } 

	      // • using uniform movement
	      else {
	        Action releaseMotor = new Action( this, "setServoBusy", false );
	        new Countdown( (int) servoT, releaseMotor ).start();
	        set(  PVector.lerp(ipos, target, transition)  );                            // <--  THIS LINE SETS THE CURRENT POSITION TO THESE VALUES
	      }

	      //end of movement, when distance between target and current position are inside tolerance
	      if (transition > 0.9) { 
	        setMoving(false);
	        set(target.x, target.y);                                                   //set current position same as target.
	        //ppos.set( this.copy() );                                                   //this grants serial will not send irrelevant data
	      }
	    }
	  }
	 */


	void setOnMovement(boolean b) {
		if (!b && onMovement) Action.perform("moveCompleted"+id);
		onMovement = b;
	}


	void onMoveComplete() {
		System.out.println("Pointer "+ id + ": " + PointerController.parent.millis() + " move is completed");
		if (!linesToDraw.isEmpty()) Action.perform( "line_P"+id ); //this action dispatches next line in a list if list is not empty
	}





	//---------------------------------- <    LASER     >-------------------------------------


	public void setLaser(int value) {
		System.out.println("Laser: " + value);
		laser = (byte)value;
		Action.perform("laserChange"+id);
		PointerController.cmdOutput += id + "," +  value  + ";";
	}


	void onLaserChange() {
		plaser = laser;
		System.out.println("LASER CHANGED "+ laser);
		//		if (!linesToDraw.isEmpty()) nextAction(); //this action dispatches next line in a list if list is not empty
	}

	//---------------------------------- <  NEXT ACTION IN LIST  >-------------------------------------

	public void nextAction(){
		System.out.println( PointerController.getParent().millis() +  " calling next action");
		Action.perform( "line_P"+id );
	}


	//---------------------------------- <  HOMOGRAPHY  >-------------------------------------


	void buildHMatrix() {
		hMatrix = null;
		System.out.println("buildHMatrix");
		while (hMatrix == null) {
			printDefaultSquaredPlane();
			printCorners();
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



	//---------------------------------- <  ACTION LIST   > ------------------------------------


	public void clearActionList(){
		//		linesToDraw.clear();
	}


	//---------------------------------- <  SERIAL OUTPUT  >-------------------------------------

	public void updateSerialOutput() {
		serialUpdating = x != ppos.x || y != ppos.y || laser != plaser;
		if (serialUpdating)    serialWrite();
	}

	void serialWrite() {
		//test if position is within servo limits
		if (fpos[0] < servoRangeX[0] || fpos[0] > servoRangeX[1] || fpos[1] < servoRangeY[0] || fpos[1] > servoRangeY[1]) {
			error = true;
		} else {
			error = false;
		}
		float finalX = Math.min(fpos[0], servoRangeX[1]);
		float finalY = Math.min(fpos[1], servoRangeY[1]);
		serialOut = id + "," +  laser  + "," + (int) ((setYasX)?finalY:finalX)  + "," + (int) ((setYasX)?finalX:finalY)   +  ";";
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

		System.out.println("cornerShape()");
		System.out.println("A: " + A.x +", "+ A.y);
		System.out.println("B: " + B.x +", "+ B.y);
		System.out.println("C: " + C.x +", "+ C.y);
		System.out.println("D: " + D.x +", "+ D.y);

		s.vertex(A.x, A.y);
		s.vertex(B.x, B.y);
		s.vertex(C.x, C.y);
		s.vertex(D.x, D.y);

		s.endShape();
		return s;
	}


}

