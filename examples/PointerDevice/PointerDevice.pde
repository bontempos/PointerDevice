import bontempos.PointerDevice.*;
import bontempos.Game.Act.*; 
import bontempos.ProjectionMatrix.*; 
import processing.serial.*;


int POINTERS = 2;
PointerController pc;
ArrayList<PVector> strokes;  

int timer;
int interval = 2000;
boolean finished = false;
boolean playing = false; 
boolean showHomo = false; //test
int delayTest = 20;

void setup() {
  size(400, 400, P2D);
  pc = new PointerController(this, 38400);
  pc.addPointer(POINTERS);
 
  
  println(" servoRate : ",  pc.getServoUpdateRate() );
  strokes = new ArrayList<PVector>();

  for(int i = 0; i < POINTERS;i++){
    PointerDevice p = pc.getPointer(i);
    p.setInvertY (true);
    //p.setServoSpeed(10); //motor speed
  }
}
/*
Pointer 0:
[0]: [ 41.85, 38.25, 0.0 ]
[1]: [ 117.45, 33.75, 0.0 ]
[2]: [ 106.649994, 135.0, 0.0 ]
[3]: [ 35.1, 124.2, 0.0 ]
*/
//void draw(){
//  PVector positions[] = new PVector[ POINTERS ];
//  for(int i = 0; i < POINTERS; i++){
//     positions[i] = new PVector (mouseX, mouseY);
//  }
//  if(mousePressed)pc.update(positions);
//}

void draw() {
  background(0);
  
  for(int i = 0; i < pc.pointers.get(0).actionList.size(); i++){
    String s = pc.pointers.get(0).actionList.getArrayList().get(i);
    text( s , 10, 200 + i * 20);
  }


  if (finished ) {
    if (millis()> timer + interval) drawLaser();
  }

  //if(mousePressed && mouseX != pmouseX && mouseY != pmouseY){
  //  strokes.add( new PVector(mouseX, mouseY, millis() ));
  //}



  if (mousePressed && mouseButton == RIGHT) {
    finished = false;
    strokes.clear();
  }






  if (!strokes.isEmpty()) renderStrokes();

  text( strokes.size(), 20, 20 );

  if ( pc.isOnCalibration() ) {
    text( "c", 100, 20);
    
    pc.displayCorners();
    
  } else {

    if ( mouseX != pmouseX || mouseY != pmouseY) {
      if (!keyPressed)      strokes.add( new PVector(mouseX, mouseY, (mousePressed)?1:0));
    }
  }
}


void keyTyped() {
  if (!pc.isOnCalibration() && key == 'h') showHomo =! showHomo;
  if (!strokes.isEmpty() && keyPressed && key == ' ') {
    playing =! playing;
    if (playing) { 
      finished = true;
      drawLaser();
    } else {
      finished = false;
    }
  }
  else if (keyPressed && key == 't'){
    println("--------------------------");
    PointerDevice p = pc.pointers.get(0);
    //p.moveTo( new PVector(int(random(180)),int(random(180))) );
    //p.moveTo( new PVector(150,100) , false);
    p.moveTo( new PVector(120,180));
  }
  else if (keyPressed && key == 'r'){
    PointerDevice p = pc.pointers.get(0);
    p.moveTo( new PVector(90,90));
  }
  else if (keyPressed && key == 'y'){
    println("--------------------------");
    PointerDevice p = pc.pointers.get(0);
    p.moveTo( new PVector(0,0));
    p.wait(1000); 
    p.moveTo( new PVector(180,0));
    p.moveTo( new PVector(180,180));
    p.moveTo( new PVector(0,180));
    p.moveTo( new PVector(0,0));
  }
}





void drawLaser() {

  /*
    send package (array) with all pointers
   pc.update( array_pvectors , (byte) array_lasers);
   
   or send individual data (for specific pointer)
   pc.update( new PVector(x,y) , (byte)laser, pointerIndex);
   be carefull about looping when using arrays or pointers to avoid redundancy
   */


  if (finished) {
    for (int i = 0; i < strokes.size(); i++) {

      //creating package of positions and laser for all pointers.
      //in the example below, all pointers have same instructions:
      PVector packagePositions[] = new PVector[POINTERS];
      byte packageLasers[] = new byte[POINTERS];
      for (int k = 0; k < POINTERS; k++) {
        //screen coord. is transformed to servo coord
        //homography plane has its corners restricted to servo coord.
        packagePositions[k] = strokes.get(i) ; 
        packageLasers[k] = (byte)strokes.get(i).z;
      }

      pc.update( packagePositions, packageLasers); //default (true) using transformations
      
      delay(delayTest);
      //pc.update( new PVector(mouseX, mouseY), (byte)0, 1);
    }
    timer = millis();
    finished = true;
  }
}


void renderStrokes() {
  for (int i = 0; i < strokes.size(); i++) {
    stroke( (strokes.get(i).z == 0)? 60 : 255);
    strokeWeight(10);
    PVector h = strokes.get(i);
    if (showHomo) h = pc.toHomography(strokes.get(i), 0); //just test to see if captures the homography at pointer 0

    point(h.x, h.y);
  }
}  