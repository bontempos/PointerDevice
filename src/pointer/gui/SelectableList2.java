//package pointer.gui;
//
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.TreeSet;
//
//
////GUIIdentity<?>
//
//public class SelectableList2< E > extends ArrayList< E >  { //extends ArrayList< E  > {
//	
//	// GUIIdentity._selected_id; 
//	static int  _guiIdentityCounter = 0;
//	
//	@Override
//	public boolean add( E  e) {
//		_guiIdentityCounter++;
//		((GUIIdentity<E>)e).list = this;
//		return super.add((E) e);
//	}
//	
//	
//	/*
//	 *  iterates all selectable list but perform only at the list selected_id is found
//	 */
//	public GUIIdentity<E> selectNext() {
//		
//		
//		
//		Iterator<E> _it = this.iterator();
//		if(_it.hasNext()){
//			GUIIdentity._selected_id = ((GUIIdentity<E>) _it.next())._gui_id;
//			return (GUIIdentity<E>) _it.next();
//		}
//		return null;
//	}
//
////	public GUIIdentity<E> selectNext() {
////		int _selected_id = GUIIdentity._selected_id++;
////		
////		E e = 
////		
////		SelectableList<?> Objectlist = ((GUIIdentity<?>)e).getList();
////		
////		if( Objectlist.size() + 1 > _selected_id){
////			return (GUIIdentity<E>) Objectlist.get(_selected_id);
////		}
////		return (GUIIdentity<E>) Objectlist.get(_selected_id);
////	}
////	
////	public GUIIdentity<E> selectBack(E e) {
////		int _selected_id = GUIIdentity._selected_id++;
////		
////		SelectableList<?> Objectlist = ((GUIIdentity<?>)e).getList();
////		
////		if( Objectlist.size() + 1 > _selected_id){
////			return (GUIIdentity<E>) Objectlist.get(_selected_id);
////		}
////		return (GUIIdentity<E>) Objectlist.get(_selected_id);
////	}
//	//
//	//	@Override
//	//	public Object selectLast() {
//	//		// TODO Auto-generated method stub
//	//		return null;
//	//	}
//	//
//	//	@Override
//	//	public Object selectFirst() {
//	//		// TODO Auto-generated method stub
//	//		return null;
//	//	}
//	//
//	//	@Override
//	//	public Object getList() {
//	//		// TODO Auto-generated method stub
//	//		return null;
//	//	}
//	//
//	//	@Override
//	//	public Object setList() {
//	//		// TODO Auto-generated method stub
//	//		return null;
//	//	}
//	//
//	//	@Override
//	//	public Object addItem() {
//	//		// TODO Auto-generated method stub
//	//		return null;
//	//	}
//	//
//	//	@Override
//	//	public Object removeItem() {
//	//		// TODO Auto-generated method stub
//	//		return null;
//	//	}
//
//}