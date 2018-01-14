import controlP5.*;
import java.util.*;

ControlP5 gui;

Accordion accordion;
Slider2D canvas;
Slider2D servos;
Group pointerGui;

//gui layout parameters

int outMargin = 40;
int yMargin = 15;
int xMargin = 15;
int xMidCol = 120;
int largeW = 100;
int toggleW = 20;
int bgColor = #000055;

int accWidth = 250;
int [] canvasPos = {outMargin, outMargin}; 
int [] canvasSize = new int[2];

int [] servoCanvasPos = new int[2];
int [] servoCanvasSize = new int[2];


void guiInit(PApplet parent) {
  gui = new ControlP5(parent);
  //gui.setAutoDraw(false);
  guiBuild();
}


void setCanvasSize ( int w, int h ) {
  canvasSize = new int[]{w, h};
}


int[] getServoCanvasSetting() {
  float []servoCanvas =  gui.get(Slider2D.class, "servosCanvas").getPosition( ) ;
  float []accor       =  gui.get(Accordion.class, "acc").getPosition();
  int[]pos = new int[]{ (int) (servoCanvas[0] + accor[0]), (int) ( servoCanvas[1] + accor[1] - 19 + 25*4 )}; //...
  int[]size =  new int[]{(int) gui.get(Slider2D.class, "servosCanvas").getWidth( ), (int) gui.get(Slider2D.class, "servosCanvas").getHeight( ) };
  return new int[]{ pos[0], pos[1], size[0], size[1] };
}

void guiBuild() {

  pc.setEnableKeyCommands(false);

  setCanvasSize( height - outMargin*2, height - outMargin*2 );
  pointerGui();


  //#############################################   ACCORDION   ###############################################
  accordion = gui.addAccordion("acc")
    .setPosition(width - accWidth - outMargin, outMargin)
    .setWidth(accWidth)
    .addItem(pointerGui)
    .setCollapseMode(Accordion.MULTI)
    .open();
  ;

  //#############################################   CANVAS   ###############################################

  //retarget this canvas to pointerController
  PointerController.setCanvas(40, 40, 640, 640 );





  canvas = gui.addSlider2D("canvas")  
    .setPosition(canvasPos[0], canvasPos[1])
    .setSize(canvasSize[0], canvasSize[1])
    .setColorBackground(color(0, 1))
    .setMinMax(0, 0, 180, 180) //< replaced by general MAX value? TODO
    .setValue(90, 90)
    .setLabel("Pointer Canvas")
    .onChange(new CallbackListener() { // a callback function that will be called onPress
    public void controlEvent(CallbackEvent theEvent) {
      mouseFollow();
    }
  }  
  );
}

void pointerGui() {
  int y = 0;
  int yLine = 25;


  pointerGui = gui.addGroup("Pointer")
    .setBackgroundColor(color(bgColor, 64))
    .setSize(220, height - outMargin * 2)
    ;

  gui.addButton("createPointer")
    .setPosition(xMargin, yMargin + yLine * y++ )
    .setSize(largeW*2, 20)
    .setLabel("new pointer")
    .moveTo(pointerGui);



  // SELECTED:

  Group selPointerGroup = gui.addGroup("selPointerGroup")
    .setPosition(0, 70)
    .hideBar()
    .close()
    .moveTo(pointerGui);
  ;


  y = 0;


  gui.addTextlabel("selected")
    .setPosition(xMargin, yMargin + yLine * y++ )
    .setText("Selected Pointer: none")
    .setFont(createFont("Standard 07_58", 12))
    //.setLabel("Selected Pointer: " + selectedPointerId)
    .moveTo(selPointerGroup);



  gui.addToggle("enableSelectedPointer")
    .setPosition(xMargin, yMargin + yLine * y)
    .setSize(toggleW, 20 )
    .setLabel("Active")
    .setBroadcast(false)
    .moveTo(selPointerGroup)

    .onClick(
    new CallbackListener() { // a callback function that will be called onPress
    public void controlEvent(CallbackEvent theEvent) {
      if ( pc.getSelectedPointer() != null) {
        pc.getSelectedPointer().setActive( gui.get(Toggle.class, "enableSelectedPointer").getBooleanValue() );
        if (!pc.getSelectedPointer().isActive()) {
          pc.getSelectedPointer().clearActionList();
          pc.getSelectedPointer().setLaser(0);
        }
      }
    }
  }  
  )
  .getCaptionLabel().align(ControlP5.RIGHT_OUTSIDE, CENTER).setPaddingX(5);




  gui.addToggle("isYInverted")
    .setPosition(xMidCol, yMargin + yLine * y++)
    .setSize(toggleW, 20 )
    .setLabel("Y Inverted")
    .setBroadcast(false)
    .moveTo(selPointerGroup)
    .onClick(
    new CallbackListener() { // a callback function that will be called onPress
    public void controlEvent(CallbackEvent theEvent) {
      if ( pc.getSelectedPointer() != null) {
        pc.getSelectedPointer().setInvertY( gui.get(Toggle.class, "isYInverted").getBooleanValue() );
      }
    }
  }  
  )
  .getCaptionLabel().align(ControlP5.RIGHT_OUTSIDE, CENTER).setPaddingX(5);





  gui.addToggle("isVertical")
    .setPosition(xMargin, yMargin + yLine * y)
    .setSize(toggleW, 20 )
    .setLabel("X is Vertical")
    .setBroadcast(false)
    .moveTo(selPointerGroup)
    .onClick(
    new CallbackListener() { // a callback function that will be called onPress
    public void controlEvent(CallbackEvent theEvent) {
      if ( pc.getSelectedPointer() != null) {
        pc.getSelectedPointer().setYasX( gui.get(Toggle.class, "isVertical").getBooleanValue() );
      }
    }
  }  
  )
  .getCaptionLabel().align(ControlP5.RIGHT_OUTSIDE, CENTER).setPaddingX(5);





  gui.addToggle("isXInverted")
    .setPosition(xMidCol, yMargin + yLine * y++)
    .setSize(toggleW, 20 )
    .setLabel("X Inverted")
    .setBroadcast(false)
    .moveTo(selPointerGroup)
    .onClick(
    new CallbackListener() { // a callback function that will be called onPress
    public void controlEvent(CallbackEvent theEvent) {
      if ( pc.getSelectedPointer() != null) {
        pc.getSelectedPointer().setInvertX( gui.get(Toggle.class, "isXInverted").getBooleanValue() );
      }
    }
  }  
  )
  .getCaptionLabel().align(ControlP5.RIGHT_OUTSIDE, CENTER).setPaddingX(5);


  servos = gui.addSlider2D("servosCanvas")  
    .setPosition(xMargin, yMargin + yLine * y)
    .setSize(largeW*2, largeW*2)
    .setMinMax(0, 0, 180, 180) //< replaced by general MAX value? TODO
    .setLabel("Servos")
    .setValue(90, 90)
    .setColorBackground(color(0, 1))
    .setColorForeground(color(0, 1))
    //.setColorActive(color(0,1)) //mouseOver
    .moveTo(selPointerGroup)
    //.setLabel("Pointer Canvas")
    .onChange(new CallbackListener() { // a callback function that will be called onPress
    public void controlEvent(CallbackEvent theEvent) {
      //mouseFollow();
    }
  }  
  );

  y+=y*3;

  gui.addToggle("enableCalibration")
    .setPosition(62, yMargin - 19 + yLine * y)
    .setSize(60, 15 )
    .setLabel("enable move")
    .setBroadcast(false)
    .setColorActive(#113366)
    .moveTo(selPointerGroup)
    .onClick(
    new CallbackListener() { // a callback function that will be called onPress
    public void controlEvent(CallbackEvent theEvent) {
      pc.setCalibration( gui.get(Toggle.class, "enableCalibration").getBooleanValue() );
      if ( pc.isOnCalibration() ) {
        gui.get(Toggle.class, "enableCalibration").setColorActive(#ffffff).setLabel("disable move");
      } else {
        gui.get(Toggle.class, "enableCalibration").setColorActive(#113366).setLabel("enable move");
      }
    }
  }  
  )
  .getCaptionLabel().align(ControlP5.RIGHT_OUTSIDE, CENTER).setPaddingX(5);

  gui.addToggle("laserButton")
    .setPosition(xMargin, yMargin + yLine * y++ )
    .setSize(largeW*2, 20)
    .setLabel("set Laser ON")
    .setBroadcast(false)
    .moveTo(selPointerGroup)
    .onClick(
    new CallbackListener() {     
    public void controlEvent(CallbackEvent theEvent) {
      setLaser();
    }
  }  
  )
  .getCaptionLabel().align(ControlP5.CENTER, CENTER).setPaddingX(5);

  gui.addButton("drawRect")
    .setPosition(xMargin, yMargin + yLine * y++ )
    .setSize(largeW*2, 20)
    .setLabel("Draw rectangle area")
    .moveTo(selPointerGroup);


  gui.addButton("removePointer")
    .setPosition(xMargin, yMargin + yLine * y++ )
    .setSize(largeW*2, 20)
    .setLabel("remove pointer")
    .moveTo(selPointerGroup);



  //draw last so it can render ABOVE other items.
  gui.addScrollableList("pointerSelDropdown")
    .setPosition(xMargin, yMargin + yLine * 1 )
    .setLabel("select pointer") 
    .setSize(largeW*2, 60)
    .setBarHeight(20)
    .setItemHeight(20)
    .addItems(pointerIdList())
    //.bringToFront() //not working as i expected. Have to look check
    .moveTo(pointerGui)
    .setOpen(false)
    ;
}


List<String> pointerIdList() {
  List l = new ArrayList<String>();
  for (int i = 0; i < pc.pointers.size(); i++) {
    l.add( String.valueOf( pc.pointers.get(i).id ) );
  }
  return l;
}


void pointerSelDropdown(int n) {
  //  println(n, gui.get(ScrollableList.class, "pointerSelDropdown").getItem(n));
  pc.selectedPointer = pc.pointers.get(  n  );
  updatePointerGui();
}


void setLaser() {
  int laserValue = (gui.get(Toggle.class, "laserButton").getBooleanValue())?255:0;
  pc.getSelectedPointer().laser = laserValue;
  if ( laserValue > 0 ) {
    gui.get(Toggle.class, "laserButton").setColorActive(#ffffff).setLabel("set Laser OFF");
  } else {
    gui.get(Toggle.class, "laserButton").setColorActive(#113366).setLabel("set Laser ON");
  }
}

void drawRect(){
  if(pc.getSelectedPointer() != null){
    pc.getSelectedPointer().drawRectangle(true);
  }
}

void updatePointerGui() {
  gui.get(Group.class, "selPointerGroup").open();
  gui.get(Textlabel.class, "selected").setText( ("Selected Pointer: " + ((pc.getSelectedPointer() != null) ? str(pc.getSelectedPointer().id) : "none") ).toUpperCase() ) ;
  gui.get(ScrollableList.class, "pointerSelDropdown").clear().addItems(pointerIdList());
  gui.get(Toggle.class, "enableSelectedPointer").setValue( pc.getSelectedPointer().isActive() );
  gui.get(Toggle.class, "isVertical").setValue( pc.getSelectedPointer().isYasX() );
  gui.get(Toggle.class, "isYInverted").setValue( pc.getSelectedPointer().isInvertY() );
  //gui.get(Toggle.class, "enableLaser").setValue( (pc.getSelectedPointer().laser>0)?true:false );
  //gui.get(Slider.class, "speedMult").setValue ( pc.getSelectedPointer().getServoSpeed() );

  //gui.get(RadioButton.class, "clockOrder").activate( clockOrder[selectedPointer.id] );
}