package pointer.environment;

import java.util.ArrayList;

public class Cluster {
	  String label;
	  ArrayList<Integer> pointerIds = new ArrayList<Integer>();

	  Cluster Cluster() {
	    return this;
	  }

	  Cluster setLabel(String l) {
	    label = l;
	    return this;
	  }

	  Cluster addPointer( String[] pointerLabels ) {
	    for (int i = 0; i < pointerLabels.length; i++) {
	      boolean notFound = true;
	      for (PointerProjector p : PointerEnvironment.pointers) {
	        if (p._gui_label.equals( pointerLabels[i])) {
	          notFound = false;
	          pointerIds.add( p._id );
	          System.out.println("pointer added to cluster");
	          break;
	        }
	      }
	      if (notFound) System.out.println("no pointer was found under the label:" +  pointerLabels[i]);
	    }
	    return this;
	  }
	  Cluster setPointerIds( int[] ids ) {
	    for (int i = 0; i < ids.length; i++) {
	      pointerIds.add(  ids[i]  );
	    }
	    return this;
	  }
	}