import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Displayer extends Thread{
	
	DatagramSocket socket;
	DatagramPacket reciever;
	
    public Displayer() {
        super("Displayer");
        try {
			socket = new DatagramSocket(CloneQC.PORT, InetAddress.getByName("127.0.0.1"));
		} catch (SocketException e) {
			System.out.println("Failed to create broadcast listening socket on port " + CloneQC.PORT);
		} catch (UnknownHostException e) {
			System.out.println(e.getMessage());		
		}
    }
	
	public void run(){
		
		while(true){
			byte[] data = null;
			reciever = new DatagramPacket(data, data.length);
			System.out.println(data);
		}
		
	}
}
