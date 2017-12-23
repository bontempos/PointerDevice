/**
 * Pointer Devices visual information drew by each device
 *
 * @author       Anderson Sudario
 * @version      1.0
 * 2017
 */


package bontempos.PointerDevice;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

public class PointerCanvas {

	PApplet parent;

	public int canvasBGcolor =   0;
	public int laserColor    =   0x0000ff00;
	public PGraphics canvas;             //object where pixels are written
	public PGraphics aid;                //used for debuging
	public boolean showAid    = true;    //shows visual aid for debuging
	public int fadeInterval   = 10;      //takes x frames to fade any written pixel
	public float fadeSpeed    = 0.01f;    //slower for darker ambient light;
	PImage canvasImg;             //faster way to get canvas pixels is transforming pGraphics in Pimage after frame is complete.
	public int[] canvasSize   = new int[2];
	

	public PointerCanvas(PApplet parent) {
		this.parent = parent;
		parent.registerMethod("draw", this);
		setCanvasSize(parent.width, parent.height);
		aid = parent.createGraphics(canvasSize[0], canvasSize[1], PConstants.P2D);
		canvas = parent.createGraphics(canvasSize[0], canvasSize[1], PConstants.P2D);
	}

	public void draw(){
		if (parent.frameCount%fadeInterval == 0)  fadeLaserColor();
		parent.image( canvas, 0, 0);
		canvasImg = getCopy(canvas);
	}

	public void setCanvasSize( int width, int height){
		canvasSize = new int[]{width, height};
	}

	/*
	fades laser color to bg color drw in canvas object - to simulate a glow-in-the-dark effect
	 */
	void fadeLaserColor() {
		canvas.beginDraw();
		canvas.loadPixels();
		for (int i = 0; i < canvasSize[0]*canvasSize[1]; i++) {
			int pixelColor = canvas.pixels[i];
			canvas.pixels[i] = parent.lerpColor(pixelColor,0x00000000,fadeSpeed);
		}
		canvas.updatePixels();
		canvas.endDraw();
	}

	/*
	 * clear PGraphics
	 */
	void clearCanvas(){
		canvas.beginDraw();
		canvas.clear();
		canvas.endDraw();
	}

	PImage getCopy(PImage image) {
		PImage newImage = parent.createImage(image.width, image.height, image.format);
		image.loadPixels();
		parent.arrayCopy(image.pixels, 0, newImage.pixels, 0, image.pixels.length);
		newImage.updatePixels();
		return newImage;
	}

	
	public void drawPoint( PVector pos , int color){
		//open canvas
	    canvas.beginDraw();
	    canvas.strokeWeight(8);
	    canvas.stroke(color);
	    canvas.point(pos.x, pos.y);
	    System.out.println("\n\n>>>>>>>>>>>> "+pos.x +","+pos.y);
	    //canvas.point(50,50);
	    canvas.endDraw();
	}
	
	public void drawPointer(PointerDevice p){
		
		
		//open canvas
	    canvas.beginDraw();
	    canvas.strokeWeight(2);

	    float newBrightness = p.laser ;
	   
	   
	    if (newBrightness > 0) {
	      //System.out.println("drawPointer: "+ newBrightness );
	      parent.colorMode(PConstants.HSB);
	      int c = parent.color( parent.hue(laserColor), parent.saturation(laserColor), newBrightness); //not best solution;  bitwise technique is faster
	      canvas.stroke( c );
	      if (p.useLines) {
	        canvas.line(p.x, p.y, p.ppos.x, p.ppos.y);
	      } else {
	        canvas.point(p.x, p.y);
	      }
	    }
	    //close canvas
	    canvas.endDraw();
	}
}
