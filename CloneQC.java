import java.net.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import java.io.*;

public class CloneQC extends Application {
	
	final static int PORT = 8167;
	final static int SCENE_WIDTH = 700;
	final static int SCENE_HEIGHT = 500;
	static String nickName = "Marcus";
	
	Stage window;
	Scene scene;
	BorderPane layout;
	
	DatagramSocket socket;
	DatagramSocket reciever;
	DatagramPacket sendPacket;
	DatagramPacket recievePacket;
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		window = primaryStage;
		layout = new BorderPane();
		scene = new Scene(layout, SCENE_WIDTH, SCENE_HEIGHT);
		window.setScene(scene);
		window.setTitle("Quick Chat Messenger");
		
		socket = new DatagramSocket();
		socket.setBroadcast(true);
		
		layout.setTop(topicHandler());
		layout.setBottom(messageHandler());
		layout.setRight(setRightSide());
		
		window.show();
	} 
	
	
	public HBox topicHandler(){
		HBox topicHBox = new HBox(10);
		topicHBox.setPadding(new Insets(10,10,10,10));
		topicHBox.setAlignment(Pos.CENTER);
		TextField topicField = new TextField();
		Button btnSetTopic = new Button("Set topic");
		
		btnSetTopic.setOnAction(e -> {
			String topic = topicField.getText();
			window.setTitle(topic);
			String topicMsg = "B" + topic + " (" + nickName + ")\0";
			send(topicMsg);
		});
		
		topicHBox.getChildren().addAll(new Label("Topic:"), topicField, btnSetTopic);
		return topicHBox;
	}
	
	
	public GridPane messageHandler(){
		TextField messageField = new TextField();
		CheckBox privateBox = new CheckBox("private message");
		TextField privateDst = new TextField();
		messageField.setPrefSize(SCENE_WIDTH / 1.5, SCENE_HEIGHT / 10);
		messageField.setMaxWidth(SCENE_WIDTH / 1.5);
		
		messageField.setOnKeyPressed((KeyEvent ke) -> {
				if(ke.getCode() == KeyCode.ENTER){
					String message = null;
					if(privateBox.isSelected())
						message = "6"+ nickName +"\0" + privateDst.getText() + "\0" + messageField.getText() +"\0";
					else message = "2#Main\0"+ nickName +"\0"+ messageField.getText() +"\0";
					send(message);
					messageField.clear();
				}
			});
		
		HBox privateContainer = new HBox(5);
		privateContainer.setAlignment(Pos.CENTER);
		privateContainer.getChildren().addAll(new Label("Destination:"), privateDst);
		privateContainer.disableProperty().bind(privateBox.selectedProperty().not());
		
		GridPane grid = new GridPane();
		grid.setHgap(20);
		grid.setVgap(3);
		grid.add(new Label("Send a message:"), 0, 0);
		grid.add(new Separator(), 0, 1, 3, 1);
		grid.add(messageField, 0, 2, 2, 2);
		grid.add(privateBox, 2, 2);
		grid.add(privateContainer, 2, 3);
		
		grid.setPadding(new Insets(10,10,10,10));
		return grid;
	}
	
	
	public VBox setRightSide(){
	//	AnchorPane rightPane = new AnchorPane();
		VBox rightSide = new VBox(5);
	//	rightSide.setBorder(new Border());
		rightSide.getChildren().addAll(
				nickNameHandler(),
				new Separator(),
				statusHandler());
		
		
		return rightSide;
	}
	
	
	public TitledPane nickNameHandler(){
		TitledPane pane = new TitledPane();
		VBox nickNameVBox = new VBox(10);
		nickNameVBox.setPadding(new Insets(10,10,10,10));
		nickNameVBox.setAlignment(Pos.CENTER);
		TextField nameField = new TextField(nickName);
		Button btnSetName = new Button("Set name");
		
		btnSetName.setOnAction(e -> {
			String newName = nameField.getText();
			String newNameMsg = "3" + nickName + "\0" + newName + "\0" + "0";
			nickName = newName;
			send(newNameMsg);
		});
		
		send("4"+nickName+"\0#mars\0"+"30");
		
		nickNameVBox.getChildren().addAll(nameField, btnSetName);
		pane.setContent(nickNameVBox);
		pane.setText("Nickname");
		return pane;
	}
	
	
	public TitledPane statusHandler(){
		VBox statusVBox = new VBox();
		statusVBox.setStyle("-fx-background-color: #98FB98;");
		
		TitledPane pane = new TitledPane();
		final ToggleGroup group = new ToggleGroup();
		RadioButton normal = new RadioButton("Normal");
		RadioButton dnd = new RadioButton("Do Not Disturb");
		RadioButton away = new RadioButton("Away");
		RadioButton offline = new RadioButton("Offline");
		
		normal.setSelected(true);
		normal.setAccessibleText("00");
		dnd.setAccessibleText("10");
		away.setAccessibleText("20");
		offline.setAccessibleText("30");
		
		normal.setUserData("#98FB98");
		dnd.setUserData("#F08080");
		away.setUserData("#FFF68F");
		offline.setUserData("#CDCDC1");
		
		normal.setToggleGroup(group);
		dnd.setToggleGroup(group);
		away.setToggleGroup(group);
		offline.setToggleGroup(group);
		
		group.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
			if (group.getSelectedToggle() != null) {
				RadioButton temp = (RadioButton)group.getSelectedToggle();
				send("D" + nickName + "\0" + temp.getAccessibleText());
				statusVBox.setStyle("-fx-background-color: " + temp.getUserData() + ";");
			}
		});
		
		statusVBox.getChildren().addAll(normal, dnd, away, offline);
		pane.setContent(statusVBox);
		pane.setText("Status");
		
		return pane;
	}
	
	
	private void send(String message){
		byte[] data = null;
		try{
			data = message.getBytes();
			sendPacket = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), PORT);
			socket.send(sendPacket);
		}catch(IOException e){
			System.out.println(e.getMessage());
			System.exit(-1);
		}
	}

	
	public static void main(String[] args) throws IOException{
		launch(args);
	}
	
}




/*
 * 
 *  private message = 6\0myName\0hisName\0message\0
 *  change topic = BnewTopic\0
 *  nickname = 3myName\0newName\00
 *  status: Normal = DmyName\000
 *  		DND = DmyName\010
 *  		Away = DmyName\020
 *  		Offline = DmyName\030
 *  join channel: 4myName\0#chanName\030
 */


