package bontempos.PointerDevice;

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
	public final float   servoSpeed           =   (float) (0.1/60f) ; //0.1/60 * 4; //for each angle, it takes 't' time to move.
	public final float[] servoRangeX          =   {0f, 180f}; 
	public final float[] servoRangeY          =   {0f, 180f};
	public final float   targetDistTolerance  =   (float) 0.1;        //detects when servos needs to start moving
	public final PVector initialPosition 	  =   new PVector(90,90);	//middle of servo (if 180)
	
	//LASER
	public int id;
	public byte laser, plaser;

	//MOVEMENT
	float speed                        =   1f;        				//increment mult for speed
	boolean enableAccel                =   false;
	boolean allowRetargeting           =   true;      		     	//if allowRetargeting is true and target changes, trajectory will be recalculated and restarted
	boolean enableHomography           =   true;
	float trajectorySpanX;
	float trajectorySpanY;
	float moveInterval;
	int moveStartTime;
	boolean moving                     =   false;          			//when false, calculates a trajectory path to the target and set moving to true
	boolean motorBusy                  =   false;            		//motor is currently executing a trajectory segment and cannot accept other instructions
	Checker motorBusyChecker;
	PVector target                     =   new PVector();
	PVector ipos                       =   new PVector();  			//initial position before start moving
	PVector ppos                       =   new PVector();  			//last frame/step position

	//STATE FLAGS
	boolean invertY                    =   false;
	boolean invertX                    =   false;
	boolean setYasX                    =   false;          			 //changes Y axis to X and vice versa
	boolean active                     =   true;
	boolean error;

	//VISUAL AID
	int idColor                        =   0xaa55dd;
	int idColorError                   =   0xff0000;
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
		System.out.println("Pointer " + id + " created");
	}
	
	
	public void draw() {
		if(active){
			update();
		}
	}


	void update(){
		//return feedback - or visual aid
		
		//not sure if should be here or inside PointerController class
//		if (onCalibration) {
//			PointerController.cornerShape(id);
//			PApplet p = PointerController.getParent();
//			if ( p.keyPressed && p.key >= '1' && p.key <= '6' )  //<-- 6, designed for projMatrix, but now this is inside CORNER settings.
//				updateCorner( p.key - '0',  new PVector(p.mouseX, p.mouseY) );
//		}
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
	
	//-------------------------------< GETTERS >-----------------------------------------------
	
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


	  void updateMove() {
	    if ( this.dist(target) > targetDistTolerance ) {

	      move();

	      //TODO --->> below is to be done inside MOVE()
	      
	      if (enableHomography && !PointerController.onCalibration) {
	        pfpos[0] = fpos[0];
	        pfpos[1] = fpos[1];
	        PVector tmp =  HomographyMatrix.solve((PVector)this, hMatrix) ;
	        fpos[0] = tmp.x;
	        fpos[1] = tmp.y;
	      } else {
	        pfpos[0] = ppos.x;
	        pfpos[1] = ppos.y;
	        fpos[0] = x;
	        fpos[1] = y;
	      }
	      
	      
	        //pfpos[0] = ppos.x;
	        //pfpos[1] = ppos.y;
	        //fpos[0] = x;
	        //fpos[1] = y;


	      //grid experimental
//	      if (enableGrid) {
//	        PVector gridTransformed = toGrid (  fpos[0],fpos[1] );
//	        fpos[0] =  gridTransformed.x;
//	        fpos[1] =  gridTransformed.y;
//	      }
	      
	      
	    } else {
//	      fill(#ff0000);
//	      noStroke();
//	      ellipse(200, 20, 10, 10);
//	      fill(-1);
//	      text("idle", 210, 20);
	    }
	  }


	  void moveTo( float tx, float ty ) {
	    /*this function sets a new target usually far from current position. If this is the case, move() function  will take place)*/
	    //println("MOVING TO: ", tx, ty );
	    if (allowRetargeting && moving) setMoving(false); //if allowRetargeting is true and target changes, trajectory will be recalculated and restarted
	    target.set(tx, ty);

	    //if an insctruction to move to is given but target and current position are the same, just say the moviment is finished:
	    if ( this.dist(target) < targetDistTolerance ) {
	     // println("NOT MOVING BECAUSE TOO CLOSE TO TARGET");
	      setMoving(false);
	    }
	  }

	  float calculateServoTrajectoryTime( PVector ini, PVector end) {
	    return Math.max( Math.abs(end.x - ini.x), Math.abs(end.y - ini.x)) * servoSpeed * 1000 ;
	  }

	  float calculateTrajectoryTime() {

	    //***************************** TODO
	    moveStartTime = (int) PointerController.getParent().millis();                                                                      //initial time before movement starts
	    //float[] fTarget;                                                      
	    //if (enableHomography && !onCalibration) {
	    //  PVector tmp =  HomographyMatrix.solve((PVector)this, hMatrix) ;
	    //  ipos.set(tmp.x, tmp.y);
	    //  tmp = HomographyMatrix.solve( (PVector)target, hMatrix );
	    //  //fTarget = new float[]{tmp.x, tmp.y};
	    //  target.set(tmp.x, tmp.y);
	    //} else {
	    //  ipos.set(x, y);
	    //   //fTarget = new float[]{target.x, target.y};
	    //}
	    ipos.set(x, y); //initial position before movement starts
	    trajectorySpanX = target.x-ipos.x ;                                                                     //time to travel in axis X
	    trajectorySpanY = target.y-ipos.y ;                                                                     //time to travel in axis Y
	   // println("ini:", ipos, "dx:", trajectorySpanX, "dy:", trajectorySpanY, "moveInterval:", moveInterval);
	    return Math.max( Math.abs(trajectorySpanX), Math.abs(trajectorySpanY)) * servoSpeed / speed * 1000 ;           //choose longer time and convert to seconds ?
	  }

	  void move() {
	    /* the condition for this function to occur is if target is set far from current position */

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
	        Action releaseMotor = new Action( this, "setMotorBusy", false );
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




	  void setMoving(boolean b) {
	    moving = b;
	    if (!b) Action.perform("moveCompleted"+id);
	  }


	  void setMotorBusy( boolean b ) {
	    motorBusy = b;
	    //if(!b) println(millis(), "motor released");
	  }

	  void onMoveComplete() {
	    //println(millis(), "move is completed");
//	    if (!linesToDraw.isEmpty()) Action.perform( "line_P"+id ); //this action dispatches next line in a list if list is not empty
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

