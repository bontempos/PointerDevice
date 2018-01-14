/*
 *  Writes and reads JSON files
 */

package pointer.projection;


import processing.core.PApplet;
import processing.data.JSONArray;
import processing.data.JSONObject;


public class JSONParser {

	static JSONParser instance;
	

	public JSONParser() {

	}

	public static void createJSONEnvironment(String fileName){
		JSONObject environ;
		
		JSONArray pointers;
		JSONArray clusters;
		JSONArray surfaces;
		JSONArray projection_masks;
		
		environ = new JSONObject();
		environ.setString("name", fileName);
		


		//PointerController.papplet.saveJSONObject(environ, "Pointers/"+fileName+".json");
	}


	







	//	public static int[] getHersheyCode(char c){
	//		return getHersheyCode(c - 0x0);
	//	}
	//
	//	
	//	public static int[] getHersheyCode(int ascii){
	//
	//		selectedPointer = pointers.get(0);
	//		int CONST = 33; //offset from ascii -> simplex[ c - CONST ]
	//
	//		JSONObject hershey = null;
	//
	//		try {
	//			//TODO relative path
	//			hershey = new JSONObject( new FileReader(System.getProperty("user.dir") + "/git/PointerDevice/src/bontempos/PointerDevice/hershey.json") );
	//		} catch (FileNotFoundException e) {
	//			e.printStackTrace();
	//		}
	//
	//		JSONObject simplex = hershey.getJSONObject("simplex");
	//		return  simplex.getJSONArray( String.valueOf(ascii) ).getIntArray();
	//
	//	}
}


