import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import java.io.*;

public class CloneQC extends Application {
	
	final static int PORT = 8167;
	final static int SCENE_WIDTH = 800;
	final static int SCENE_HEIGHT = 500;
	static String nickName = "Marcus";
	
	Stage window;
	Scene scene;
	BorderPane layout;
	
	DatagramSocket socket;
	DatagramSocket reciever;
	DatagramPacket sendPacket;
	DatagramPacket recievePacket;
	TabPane tabPane = new TabPane();
	CheckBox spoofingBox = new CheckBox("spooing");
	TextField onBehalfTF = new TextField();
	CheckBox privateMessageBox  = new CheckBox("send a private message");
	TextField dstPrivateTF = new TextField();
	CheckBox noPrivacyBox  = new CheckBox("no privacy");
	
	public static volatile ScrollPane displayerSP = new ScrollPane();
	StackPane displayerRoot = new StackPane(displayerSP);
	static HashMap<String, VBox> channels = new HashMap<String, VBox>();
	ArrayList<String> joinedChannels = new ArrayList<String>();
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		window = primaryStage;
		layout = new BorderPane();
		scene = new Scene(layout, SCENE_WIDTH, SCENE_HEIGHT);
		window.setScene(scene);
		window.setTitle("Quick Chat Messenger");
		
		socket = new DatagramSocket();
		socket.setBroadcast(true);
		
		//reciever = new DatagramSocket(PORT);
		
		displayerSP.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		displayerSP.setHbarPolicy(ScrollBarPolicy.NEVER);
	//	displayerSP.setStyle("-fx-background-color: #DDDFFF;");
		displayerRoot.setPadding(new Insets(0, 10, 10, 10));
		displayerSP.setPadding(new Insets(10, 10, 10, 10));
		
		layout.setCenter(displayerRoot);
		layout.setTop(topicHandler());
		layout.setBottom(messageHandler());
		layout.setRight(setRightSide());
		
		displayerSP.setContent(channels.get("#Main"));
		
		new Displayer().start();
		
		window.show();
	} 
	
	
	public HBox topicHandler(){
		HBox topicHBox = new HBox(10);
		topicHBox.setPadding(new Insets(10,10,10,10));
		topicHBox.setAlignment(Pos.CENTER);
		TextField topicField = new TextField();
		Button btnSetTopic = new Button("Set topic");
		topicField.setPromptText("Set a new topic");
		btnSetTopic.setOnAction(e -> {
			String topic = topicField.getText();
			window.setTitle(topic);
			String topicMsg = "B" + topic + " (" + nickName + ")\0";
			send(topicMsg);
		});
		
		topicHBox.getChildren().addAll(new Label("Topic:"), topicField, btnSetTopic);
		return topicHBox;
	}
	
	
	public TabPane messageHandler(){
		joinedChannels.add("#Main");
		setChannelTab("#Main");
		tabPane.setStyle("-fx-background-color: #DDDFFF;");
		
		tabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
		    @Override
		    public void changed(ObservableValue<? extends Tab> observable, Tab oldTab, Tab newTab) {
		    	displayerSP.setContent(channels.get(newTab.getText()));
		    }
		});
		
		return tabPane;
	}
	
	
	public void setChannelTab(String channelName){
		VBox channelVB = new VBox(3);			// where the messages will be displayed
		channels.put(channelName, channelVB);
		
		TextField messageField = new TextField();
		messageField.setPrefSize(SCENE_WIDTH / 1.5, SCENE_HEIGHT / 7);
		messageField.setMaxWidth(SCENE_WIDTH / 1.5);
		messageField.setPromptText("Send a message ...");
		
		messageField.setOnKeyPressed((KeyEvent ke) -> {
				if(ke.getCode() == KeyCode.ENTER){
					String text = messageField.getText();
					String sender = null;
					String message = null;
					if(spoofingBox.isSelected()) sender = onBehalfTF.getText();
					else sender = nickName;
					
					if(privateMessageBox.isSelected()) message = "6"+ sender +"\0" + dstPrivateTF.getText() + "\0" + text +"\0";
					else { message = "2" + channelName + "\0"+ sender +"\0"+ text +"\0";
						   channels.get(channelName).getChildren().add(buildMessage(sender, text, false, false));
						 }
					
					displayerSP.setVvalue(displayerSP.getVmax()); // auto-scroll
					send(message);
					messageField.clear();
				}
			});
		
		StackPane root = new StackPane(messageField);
		root.setPadding(new Insets(10,10,10,10));
		
		Tab newTab = new Tab(channelName);
		newTab.setContent(root);
		tabPane.getTabs().add(newTab);
	}
	
	
	public VBox setRightSide(){
		VBox rightSide = new VBox(5);
		Accordion options = new Accordion(nickNameHandler(), statusHandler(), channelsHandler(), privacyHandler());
		options.setPadding(new Insets(0,10,10,0));
		options.setExpandedPane(options.getPanes().get(1));
		rightSide.getChildren().add(options);
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
		
		nickNameVBox.getChildren().addAll(nameField, btnSetName);
		nickNameVBox.setStyle("-fx-background-color: #DDDFFF;");
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
	
	
	public TitledPane channelsHandler(){
		TitledPane pane = new TitledPane();
		VBox channelsVBox = new VBox(3);
		TextField addChannelField = new TextField();
		ScrollPane channelSP = new ScrollPane();
		ListView<Label> list = new ListView<Label>();
		list.getBoundsInParent();
		channelSP.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		channelSP.setHbarPolicy(ScrollBarPolicy.NEVER);
		channelSP.setContent(list);
		
		addChannelField.setPromptText("Add a new channel [Enter]");
		
		addChannelField.setOnKeyPressed((KeyEvent ke) -> {
			if(ke.getCode() == KeyCode.ENTER){
				
				ContextMenu contextMenu = new ContextMenu();
				MenuItem joinItem = new MenuItem("join");
				MenuItem leaveItem = new MenuItem("leave");
				MenuItem deleteItem = new MenuItem("delete");
				contextMenu.getItems().addAll(joinItem, leaveItem, deleteItem);
				
				String channelName = "#" + addChannelField.getText();
				Label ch = new Label(channelName);
				ch.setContextMenu(contextMenu);
				
				joinItem.setOnAction(e -> {
					for(String c : joinedChannels)
						if(c.equals(ch.getText())) return;
					setChannelTab(ch.getText());
					send("4" + nickName + "\0" + channelName + "\0" + "30");
					joinedChannels.add(channelName);
				});
				
				leaveItem.setOnAction(e -> {
					send("5" + nickName + "\0" + channelName + "\0" + "0");
					tabPane.getTabs().remove(getTabIndex(channelName));
					joinedChannels.remove(channelName);
				});
				
				deleteItem.setOnAction(e -> {
					ObservableList<Label> items = list.getItems();
					for(int i = 0; i < items.size(); i++){
						if(items.get(i).getText().equals(channelName))
							items.remove(i);
					}
					
					if(joinedChannels.remove(channelName)){
						send("5" + nickName + "\0" + channelName + "\0" + "0");
						int index = getTabIndex(channelName);
						if(index != -1) tabPane.getTabs().remove(index);
					}
				});
				
				list.getItems().add(ch);				
				addChannelField.clear();
			}
		});
		
		channelsVBox.getChildren().addAll(addChannelField, channelSP);
		channelsVBox.setStyle("-fx-background-color: #DDDFFF;");
		pane.setContent(channelsVBox);
		pane.setText("Channels");
		return pane;
	}
	
	
	private int getTabIndex(String name){
		ObservableList<Tab> tabs = tabPane.getTabs();
		for(int i = 0; i < tabs.size(); i++){
			if(tabs.get(i).getText().equals(name))
				return i;
		}
		return -1;
	}
	
	
	public TitledPane privacyHandler(){
		TitledPane pane = new TitledPane();
		pane.setText("Privacy");
		VBox privacyVB = new VBox(3);
		
		privateMessageBox = new CheckBox("send a private message");
		spoofingBox = new CheckBox("spoofing");
		noPrivacyBox = new CheckBox("no privacy");
		
		HBox privateContainer = new HBox(5);
		privateContainer.setAlignment(Pos.CENTER);
		privateContainer.setPadding(new Insets(0, 0, 10, 0));
		privateContainer.getChildren().addAll(new Label("destination:"), dstPrivateTF);
		privateContainer.disableProperty().bind(privateMessageBox.selectedProperty().not());
		
		HBox spoofingContainer = new HBox(5);
		spoofingContainer.setAlignment(Pos.CENTER);
		spoofingContainer.setPadding(new Insets(0, 0, 10, 0));
		spoofingContainer.getChildren().addAll(new Label("on behalf of:"), onBehalfTF);
		spoofingContainer.disableProperty().bind(spoofingBox.selectedProperty().not());
		
		privacyVB.getChildren().addAll(privateMessageBox, privateContainer, new Separator(), 
									   spoofingBox,	spoofingContainer, new Separator(),
									   noPrivacyBox);
		privacyVB.setStyle("-fx-background-color: #DDDFFF;");
		
		pane.setContent(privacyVB);
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
			//System.exit(-1);
		}
	}

	
	public static VBox buildMessage(String sender, String message, boolean prvt, boolean incoming){
		VBox finalMessage = null;		
		String timeStamp = new SimpleDateFormat("dd.MM.yyyy  HH:mm:ss").format(new Date());
		Label timeStampLabel = new Label("on " + timeStamp);
		timeStampLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #AAAAAA;");
		
		Label senderInfo = new Label(sender + ":  ");
		senderInfo.setStyle("-fx-font-weight: bold;");
		
		Label text = new Label(message);
		text.setPrefWidth(displayerSP.getWidth() / 2.2);
		text.setWrapText(true); 	// display it in multiline
	
		String backgroundColor = "-fx-background-color: #DDDFFF;";
		String borderColor = "-fx-border-color: #AAAFFF;";
		
		if(prvt) {
				finalMessage = new VBox(new Label("<private>"), timeStampLabel, new HBox(senderInfo, text));
				backgroundColor = "-fx-background-color: #FFAEB9;";
				borderColor = "-fx-border-color: #CD8C95;";
			}
		else finalMessage = new VBox(timeStampLabel, new HBox(senderInfo, text));
		
		if(incoming){
			backgroundColor = "-fx-background-color: #54FF9F;";
			borderColor = "-fx-border-color: #43CD80;";
		}
		
		finalMessage.setStyle("-fx-padding: 10;" + 
                "-fx-border-style: solid inside;" + 
                "-fx-border-width: 2;" +
                "-fx-border-radius: 5;" + 
                borderColor +
                backgroundColor +
                "-fx-padding-top: 10px;");
		
		return finalMessage;
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
 *  leave channel: 5myName\0#chanName\00
 */


