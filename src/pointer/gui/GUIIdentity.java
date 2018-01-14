package pointer.gui;

public class GUIIdentity<T> implements Selectable<T>{
	
	public int _gui_id = 0;
	public static int _selected_id = -1;
	public static boolean _isSelected = false;
	public SelectableList list;
	
	public GUIIdentity(){
		_gui_id++;
	}
	

	
	@Override
	public SelectableList getList() {
		return list;
	}



	@Override
	public T setGUIid(int gui_id) {
		_gui_id = gui_id;
		return (T) this;
	}
	

}
