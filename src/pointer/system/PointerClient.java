package pointer.system;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public  class PointerClient {

	//protocol: see PointerConstants
	


	static Socket socket;
	static OutputStream out = null;
	static int packetCount = 0;
	static public ArrayList<byte[]> packs = new ArrayList<byte[]>();


	public PointerClient() {
		System.out.println("Client initialized");
		try {
			socket = new Socket(InetAddress.getLoopbackAddress(), 9540);
			out = socket.getOutputStream();
		}
		catch(Exception e) {
			System.out.println("Error connecting to server!" + e);
		}
	}



	public static void update(){

		if (out != null) { //<-- no stream for the data to continue...
			if(!packs.isEmpty()){
				for(int i = packs.size() - 1; i >= 0; i-- ){
					byte[] pack = packs.get(i);
					//packs.remove(i);

					//TODO should wait for reply from server to upload next otherwise bits will be messy when sending two packs
					//out.write(pack, 0, pack.length); 

					// temporary workaround giving a small delay between uploading each pack;
					Timer timer = new Timer( );
					timer.schedule( new TimerTask (){
						@Override
						public void run(){
							try {
								out.write(pack, 0, pack.length);
							} catch (IOException e) {
								e.printStackTrace();
							} 
						}
					}, i * 100);

					//out.flush();
					packetCount++;
				}				
				//all packs sent:
				packs.clear();
			}
		}
	}

}



