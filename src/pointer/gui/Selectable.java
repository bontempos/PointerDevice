package pointer.gui;

public interface Selectable<T> {
	T setGUIid( int gui_id);
	SelectableList<T> getList();
}
