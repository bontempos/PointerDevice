package pointer.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;



public class SelectableList< E > extends ArrayList< E >  { 

	static int  _guiIdentityCounter = 0; //move to container

	@Override
	public boolean add( E  e) {

		((GUIIdentity<?>)e)._gui_id = _guiIdentityCounter;
		((GUIIdentity<?>)e).list = this;
		GUIObjectContainter._map.put( ((GUIIdentity<?>)e) , _guiIdentityCounter);
		_guiIdentityCounter++;

		return super.add((E) e);
	}


	public E selectNext(){
		System.out.println("SELNEX");
		//get aware of current selected item.
		//if selected item is in this list
		//convert selected item to this list's position

		int localPosition = GUIObjectContainter.getListRelativePoistion(this);
		if( localPosition < this.size() -1 || localPosition == -1) return null;
		GUIIdentity._selected_id = GUIObjectContainter.getListAbsolutePosition(localPosition,this);
		return this.get(localPosition++);
	}


	public E selectBack(){
		//get aware of current selected item.
		//if selected item is in this list
		//convert selected item to this list's position

		int localPosition = GUIObjectContainter.getListRelativePoistion(this);
		if( localPosition <= 0 ) return null;
		GUIIdentity._selected_id = GUIObjectContainter.getListAbsolutePosition(localPosition,this);
		return this.get(localPosition--);

	}

}

class GUIObjectContainter{

	public static HashMap< GUIIdentity<?>, Integer > _map = new HashMap< GUIIdentity<?>, Integer >();

	/*
	 * returns the position of selected object from inside its current list
	 */
	public static int getListRelativePoistion(SelectableList< ? > list){

		int selected = GUIIdentity._selected_id; 

		Set<?> set = _map.entrySet();
		Iterator<?> itr = set.iterator();
		while( itr.hasNext() )
		{
			Map.Entry entry = (Map.Entry) itr.next();
			GUIIdentity<?> identity = ( GUIIdentity<?> ) entry.getKey();
			if(selected == identity._gui_id)
			{
				if( identity.list == list )
				{
					return (int)entry.getValue();
				}
			}
		}
		return -1 ; //not found
	}

	
	/*
	 * returns the position of selected object from inside its current list
	 */
	public static int getListAbsolutePosition(int localPosition , SelectableList< ? > list){
		
		Set<?> set = _map.entrySet();
		Iterator<?> itr = set.iterator();
		while( itr.hasNext() )
		{
			Map.Entry entry = (Map.Entry) itr.next();
			
			
			GUIIdentity<?> identity = ( GUIIdentity<?> ) entry.getKey();
			
			if(localPosition == identity._gui_id)
			{
				if( identity.list == list )
				{
					return (int)identity._gui_id;
				}
			}
		}
		return -1 ; //not found
	}

}