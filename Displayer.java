import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.application.Platform;

public class Displayer extends Thread{
	
	DatagramSocket socket;
	DatagramPacket receiver;
	
    public Displayer() {
        super("Displayer");
        try {
			socket = new DatagramSocket(CloneQC.PORT, InetAddress.getByName("0.0.0.0"));
			socket.setBroadcast(true);
		} catch (SocketException e) {
			System.out.println("Failed to create broadcast listening socket on port " + CloneQC.PORT);
		} catch (UnknownHostException e) {
			System.out.println(e.getMessage());		
		}
    }
	
	public void run(){
		
		while(true){
			byte[] data = new byte[1024];
			receiver = new DatagramPacket(data, data.length);
			try {
				socket.receive(receiver);
				System.out.println(new String(receiver.getData()));
				parse(receiver.getData());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	
	private void parse(byte[] raw){
		String msg = new String(raw);
		char[] msgChar = msg.toCharArray();
		List<String> parts = new ArrayList<String>(Arrays.asList(msg.split("\0")));
		
		for(String s : parts)
			System.out.println(s);
		
		switch(parts.get(0).toCharArray()[0]){
			case '2':				
				String channelName = parts.get(0).substring(1, parts.get(0).length());
				Platform.runLater(new Runnable() {
			         @Override
			         public void run() {
			        	 CloneQC.channels.get(channelName).getChildren().add(CloneQC.buildMessage("Vaniusha", "some text", false, true));
		             }
			    });
				
				break;
				
			case '6':
				break;
		}
	}
}
