import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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
		List<String> parts = new ArrayList<String>(Arrays.asList(msg.split("\0")));
		
		switch(parts.get(0).toCharArray()[0]){
			case '2':
				if(!parts.get(1).equals(CloneQC.nickName)){
					String channelName = parts.get(0).substring(1, parts.get(0).length());
					
					Platform.runLater(new Runnable() {
				         @Override
				         public void run() {
				        	 CloneQC.channels.get(channelName).getChildren().add(CloneQC.buildMessage(parts.get(1), parts.get(2), true));
			             }
				    });
				}
				break;
				
			case '6':
				if(CloneQC.noPrivacyBox.isSelected()){
					Platform.runLater(new Runnable() {
				         @Override
				         public void run() {
				        	 CloneQC.channels.get("#Main").getChildren().add(getPrivateMsg(parts));
			             }
				    });
				}
				break;
		}
	}
	
	
	public VBox getPrivateMsg(List<String> list){
		VBox finalMessage = null;		
		String timeStamp = new SimpleDateFormat("dd.MM.yyyy  HH:mm:ss").format(new Date());
		Label timeStampLabel = new Label("on " + timeStamp);
		timeStampLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #AAAAAA;");
		
		String srcName = list.get(0).substring(1, list.get(0).length());
		Label src = new Label(srcName);
		src.setStyle("-fx-font-weight: bold;");
		
		Label dst = new Label(list.get(1));
		dst.setStyle("-fx-font-weight: bold;");
		
		Label text = new Label(list.get(2));
		text.setPrefWidth(CloneQC.displayerSP.getWidth() / 2.2);
		text.setWrapText(true); 	// display it in multiline
		
		finalMessage = new VBox(new Label("<private>"), timeStampLabel, 
								new VBox(new HBox(new Label("From:  "), src), 
								new HBox(new Label("To:  "), dst)), 
								new Label("\n"),text);
		
		finalMessage.setStyle("-fx-padding: 10;" + 
                "-fx-border-style: solid inside;" + 
                "-fx-border-width: 2;" +
                "-fx-border-radius: 5;" + 
                "-fx-border-color: #CD8C95;" +
                "-fx-background-color: #FFAEB9;" +
                "-fx-padding-top: 10px;");
		
		finalMessage.setTranslateX(text.getPrefWidth() / 2);
		
		return finalMessage;
	}
}
