import bontempos.PointerDevice.*;
import bontempos.Game.Act.*; 
import bontempos.ProjectionMatrix.*; 
import processing.serial.*;

int POINTERS = 1;
PointerController pc;
int servoSpeed = 2;


void setup() {
  size(1024, 720, P2D);

  pc = new PointerController(this, 38400);
  //pc.addPointer(POINTERS);
  //setupPointers();

  guiInit(this);
}

void setupPointer(int id) {

  PointerDevice p = pc.getPointer(id);
  p.setInvertY (true);
  p.setServoSpeed(servoSpeed); //motor speed . default = 2
}



void createPointer() {
  int last = pc.addPointer(1) - 1;
  setupPointer(last);
  pc.selectedPointer = pc.pointers.get(last);
  int[] settings = getServoCanvasSetting();
  pc.selectedPointer.servos.setDisplay(settings[0], settings[1], settings[2], settings[3]);

  pc.getSelectedPointer().setSquaredPlane( new PVector[]{ new PVector(40,40), new PVector(40+640,40), new PVector(40+640,40+640), new PVector(40,40+640)} );
  //pc.getSelectedPointer().setSquaredPlane( new PVector[]{ new PVector(), new PVector(640,0), new PVector(640,640), new PVector(0,640)} );

  updatePointerGui();
}



void mouseFollow() {
  PointerDevice p = pc.getSelectedPointer();
  if (p  != null) {
    int laserValue = 255; //set this to a slider
    boolean laserOn = mousePressed;
    if(!pc.isOnCalibration()){
      pc.update( new PVector(mouseX-40,mouseY-40), (laserOn)?laserValue:0, p.id); //first parameter is position x,y
    }else{
      //if on calibration, mouse is automatically captured by library.
    }
  }
}


void draw() {
  background(10);

  if ( pc.getSelectedPointer() != null) {

    pc.getSelectedPointer().displayCorners(); 

    pc.getSelectedPointer().servos.display();
    
    pc.getSelectedPointer().displayDefaultSquaredPlane();
    
    //pc.getSelectedPointer().printCorners();
    gui.get(Slider2D.class, "servosCanvas").setColorForeground((pc.getSelectedPointer().isOnMovement())? ControlP5Constants.AQUA : ControlP5Constants.BLUE ).setArrayValue( new float[]{pc.getSelectedPointer().x, pc.getSelectedPointer().y} );
  }
  
}