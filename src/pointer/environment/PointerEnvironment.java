/*
 *  All changes in environment 
 */

package pointer.environment;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;

import bontempos.ProjectionMatrix.HomographyMatrix;
import pointer.PointerController;
import pointer.gui.GUIObject;
import pointer.gui.SelectableList;
import pointer.projection.ProjectionMask;
import processing.core.PVector;
import processing.data.JSONArray;
import processing.data.JSONObject;

import static pointer.PointerController.papplet;
import static pointer.system.PointerConstants.*;

public class PointerEnvironment {

	public static boolean editMode;
	public static PointerEnvironment instance;

	static ArrayList<String>storedTags;
	static boolean includeModel = true;

	static JSONObject _environ;
	static String fileName = "new environment";
	static ArrayList<String> tags;
	
	public static SelectableList<PointerProjector> pointers;
	public static ArrayList<Cluster> clusters;
	public static ArrayList<ProjectionSurface> surfaces;
	public static ArrayList<ProjectionMask> masks;
	public static ArrayList<ProjectorModel> models;

//	static int activeSelectionList = POINTER_LIST;
	
	

//	static int selectedId = 0;
//	static GUIObject<?> selected;
//	static GUIObject<?> last;
//	static GUIObject<?> first;

	public PointerEnvironment() {
		System.out.println("Blank Environment");
		editMode = false;
		tags = new ArrayList<String>();
		pointers = new SelectableList<PointerProjector>();
		clusters = new ArrayList<Cluster>() ;
		surfaces = new ArrayList<ProjectionSurface>() ;
		masks = new ArrayList<ProjectionMask>() ;
		models = new ArrayList<ProjectorModel>() ;
		buildEnvironment();
	}


	public static void setName( String name  ){
		fileName = name;
	}


	public static void setFileTag( String tag ){
		tags.add(tag);
		storedTags.add(tag);
		HashSet<String> noDups = new HashSet<String>(storedTags);
		storedTags.clear();
		storedTags.addAll(noDups);
	}


	/*
	 *  POINTER
	 */
	public static PointerProjector addPointer(){
		return addPointer( new PointerProjector() );
	}
	public static PointerProjector addPointer(PointerProjector pointer){
		pointers.add(pointer);
		return pointer;
	}

	public static PointerProjector getPointer(){
		return getPointer( PointerProjector._selected_id );
	}
	
	public static PointerProjector getPointer( int pointerId ){
		return pointers.get(pointerId);
	}

	public static PointerProjector getPointer(PositionOnList position){
		switch(position){
		case SELECTED:
			for(PointerProjector p : pointers){
				if(p._id == PointerProjector._selected_id) return p;
			}
		case LAST:
			return (PointerProjector) pointers.get(pointers.size()-1 );
		case FIRST:
			return (PointerProjector) pointers.get(0);
		}
		return null;
	}


	/*
	 *  SURFACE
	 */
	public static ProjectionSurface addProjectionSurface(){
		return addProjectionSurface( new ProjectionSurface() );
	}

	public static ProjectionSurface addProjectionSurface( ProjectionSurface surface){
		surfaces.add(surface);
		return surface;
	}

	public static ProjectionSurface getProjectionSurface( int surfaceId ){
		return surfaces.get(surfaceId);
	}

	public static ProjectionSurface getProjectionSurface(PositionOnList constant){
		switch(constant){
		case SELECTED:
			for(ProjectionSurface s : surfaces){
				if(s._id == ProjectionSurface.selectedId) return s;
			}
		case LAST:
			return (ProjectionSurface) surfaces.get(surfaces.size()-1 );
		case FIRST:
			return (ProjectionSurface) surfaces.get(0);
		}
		return null;
	}


	/*
	 *  MASK
	 */
	public static ProjectionMask getProjectionMask( int mask_id ){
		System.out.println("masks size: " + masks.size());
		for(ProjectionMask m : masks){
			if(m._id == mask_id){
				System.out.println("found mask id " + m._id);
				return m;
			}
		}
		return null;
	}


	/*
	 *  this function is called everytime a new object is added or set on the environment
	 *  during edit mode, user makes changes on the environment variables and than click on a button to build the environment and send it to server;
	 */
	public static void buildEnvironment() {

		System.out.println("build environment");

		_environ = new JSONObject();


		_environ.setString("name", fileName );




		JSONArray _tags = new JSONArray();
		for (String s : tags) {
			int i = tags.indexOf(s);
			_tags.setString( i , s);
		}
		_environ.setJSONArray("tags", _tags);

		papplet.saveJSONObject(_environ, "Pointers/"+fileName+".json");


		JSONArray _pointers = new JSONArray();



		for (PointerProjector p : pointers) {


			JSONObject _model = new JSONObject();
			_model.setFloat( "physical_depth", p.model.physicalDepth );
			_model.setFloat( "physical_width", p.model.physicalWidth );
			_model.setFloat( "physical_height", p.model.physicalHeight );
			_model.setFloat( "projection_apperture_angle", p.model.projectionAppertureAngle );
			_model.setFloat( "speed", p.speed );



			JSONObject _pointer = new JSONObject();
			int i = pointers.indexOf(p);


			_pointer.setString( "label", p._gui_label );
			System.out.println("set p label:" + p._gui_label);
			_pointer.setInt( "color_id", p._color_id );
			_model.setFloat( "intensity", p.intensity );
			_pointer.setFloat( "speed", p.speed );
			_pointer.setJSONArray( "servos_initial_position", getVectorArrayJSON(p.initialPosition, false));
			_pointer.setJSONArray( "transform", getTransformArrayJSON( p ) );

			JSONObject _proPlane = new JSONObject();

			_proPlane.setFloat("apperture", p.model.projectionAppertureAngle);
			_pointer.setJSONArray( "projection_center", getVectorArrayJSON(p.projectionCenter, false));


			//			JSONArray _corners = new JSONArray();
			//			PVector[] corners = p.getProjectionMask().getHomographyCorners();
			//			for (int c = 0; c < 4; c++) {
			//				_corners.setJSONArray( c, getVectorArrayJSON(corners[c], false)); 
			//			}
			//			_proPlane.setJSONArray( "corners", _corners);


			_pointer.setJSONObject("projPlane", _proPlane) ;
			if (includeModel) _pointer.setJSONObject("model", _model) ;

			_pointers.setJSONObject(i, _pointer);
		}
		_environ.setJSONArray("pointers", _pointers);


		JSONArray _surfaces = new JSONArray();
		for (ProjectionSurface s : surfaces) {

			JSONObject _surface = new JSONObject();
			int i = surfaces.indexOf(s);
			_surface.setString( "label", s._gui_label );
			_surface.setInt( "color", s.fillColor );
			_surface.setFloat( "physical_width", s.physicalWidth );
			_surface.setFloat( "physical_height", s.physicalHeight );
			_surface.setBoolean( "is_3dmodel_face", s.is3DModelFace );
			_surface.setJSONArray( "translation", getTransformArrayJSON( s ) );
			_surfaces.setJSONObject(i, _surface);
		}
		_environ.setJSONArray("surfaces", _surfaces);


		JSONArray _masks = new JSONArray();
		if (!masks.isEmpty()) {
			for (ProjectionMask m : masks) {
				JSONObject _mask = new JSONObject();
				int i = masks.indexOf(m);
				_mask.setString( "label", m._label );
				_mask.setInt("Pointer", m._pointerId);
				_mask.setInt("Surface", m._surfaceId);


				JSONArray _corners = new JSONArray();
				PVector[] corners = m.getHomographyCorners();
				for (int c = 0; c < 4; c++) {
					_corners.setJSONArray( c, getVectorArrayJSON(corners[c], false)); 
				}
				_mask.setJSONArray( "corners", _corners);


				_masks.setJSONObject(i, _mask);

			}
		}
		_environ.setJSONArray("masks", _masks);



		JSONArray _clusters = new JSONArray();
		if (!masks.isEmpty()) {
			for (Cluster c : clusters) {
				JSONObject _cluster = new JSONObject();
				int i = clusters.indexOf(c);
				_cluster.setString( "label", c.label );

				JSONArray _pointerIds = new JSONArray();
				for (Integer id : c.pointerIds) {
					int index = c.pointerIds.indexOf(id);
					_pointerIds.setInt( index, id);
				}
				_cluster.setJSONArray("pointer_ids", _pointerIds);

				_clusters.setJSONObject(i, _cluster);
			}
		}
		_environ.setJSONArray("clusters", _clusters);


		PointerController.papplet.saveJSONObject(_environ, "Pointers/"+fileName+".json");

	}


	static JSONArray getVectorArrayJSON( PVector vec, boolean is3d) {
		JSONArray _vec = new JSONArray();
		_vec.setFloat( 0, vec.x );
		_vec.setFloat( 1, vec.y );
		if (is3d) _vec.setFloat( 2, vec.y );
		return _vec;
	}


	static JSONArray getTransformArrayJSON( GUIObject<?> obj ) {
		JSONArray _transform = new JSONArray();
		_transform.setFloat( 0, obj._transform.tx );
		_transform.setFloat( 1, obj._transform.ty );
		_transform.setFloat( 2, obj._transform.tz );
		_transform.setFloat( 3, obj._transform.rx );
		_transform.setFloat( 4, obj._transform.ry );
		_transform.setFloat( 5, obj._transform.rz );
		_transform.setFloat( 6, obj._transform.s );
		return _transform;
	}


	public static void upload(){

		buildEnvironment();


		byte[] JSONAsBytes;
		try {
			//convert JSON to bytes
			JSONAsBytes = _environ.toString().getBytes("UTF-8");
			System.out.println("uploading environment, bytes: " + JSONAsBytes.length);

			//adding protocol;
			//convering length to 2 bytes:
			byte lenByte1 = (byte) (JSONAsBytes.length / 256);
			byte lenByte2 = (byte) (JSONAsBytes.length % 256);
			byte[] protocol = { SET_ENVIRONMENT, lenByte1, lenByte2 };

			System.out.println("protocol:" + SET_ENVIRONMENT +","+ lenByte1 +","+ lenByte2  );

			//send to server
			byte[] destination = new byte[protocol.length + JSONAsBytes.length];
			System.arraycopy(protocol, 0, destination, 0, protocol.length);
			System.arraycopy(JSONAsBytes, 0, destination, protocol.length, JSONAsBytes.length);

			//PointerController.client.packs.add(destination);
			PointerController.uploadToServer(destination);

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	}


}


