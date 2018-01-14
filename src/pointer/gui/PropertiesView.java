package pointer.gui;

import processing.core.PApplet;
import processing.core.PGraphics;

public class PropertiesView {
	static PGraphics view;

	public static void updatePropertiesGraphic(){
		view.beginDraw();
		view.background(0xffffff00);
		view.fill(0);
//		if(selectedEditPoint!=-1){
//			view.text("edit point:" + selectedEditPoint, 10,15);
//			view.text("x:" + papplet.mouseX +" y:"+ papplet.mouseY, 10,35);
//		}else{
//			view.text("no selection", 10,15);
//			view.text("click 1 to 4", 10,35);
//		}
		view.endDraw();
	}
}
