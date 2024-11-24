package resource.artifact.controllers.user;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import resource.artifact.MainApplication;
import resource.artifact.domains.Message;
import resource.artifact.domains.ReplyMessage;
import resource.artifact.domains.User;
import resource.artifact.services.SocialNetworking;
import resource.artifact.utils.events.AccountEvent;
import resource.artifact.utils.events.ChangeEvent;
import resource.artifact.utils.fx.AlertCreator;
import resource.artifact.utils.observers.Observer;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserAccController implements Observer<AccountEvent> {

    @FXML
    private Button notificationsButton;
    @FXML
    private TableView<User> tableView;
    @FXML
    private TableColumn<User,String> tableColumnUsername;
    @FXML
    private TableColumn<User,String> tableColumnFirstName;
    @FXML
    private TableColumn<User,String> tableColumnLastName;
    @FXML
    private TableColumn<User,Boolean> tableColumnSelect;
    @FXML
    private Label usernameLabel;

    @FXML
    private VBox chatBox;
    @FXML
    private VBox chatMessages;
    @FXML
    private TextField chatInput;
    @FXML
    private Button chatButton;

    private final List<User> messageUsers = new ArrayList<>();
    private Stage sendFriendRequestStage;
    private Stage friendRequestsStage;
    private Stage notificationsStage;

    private boolean ChatOpen = false;

    ObservableList<User> model = FXCollections.observableArrayList();

    private SocialNetworking service;
    private User user;

    public void setService(SocialNetworking service){
        this.service = service;
        service.addObserver(this);
    }

    public void setUser(User user) {
        this.user = user;
        usernameLabel.setText(user.getUsername());
        initView();
    }

    private void initView(){
        initModel();
        ImageView imageView = new ImageView(new Image(Objects.requireNonNull(
                MainApplication.class.getResourceAsStream("images/notifications-1.png"))));
        imageView.setFitWidth(20);
        imageView.setFitHeight(20);
        notificationsButton.setGraphic(imageView);
        service.get_all_friend_requests().stream()
                .filter(friendRequest -> friendRequest.getReceiver().equals(user.getId()) && friendRequest.getStatus().equals(false))
                .forEach(friendRequest -> notificationsButton.setStyle("-fx-background-color: red"));

        service.get_all_user_messages(user).forEach(this::createMessageBox);

    }

    private void initModel() {
        service.get_all_friends_of_User(user.getId().toString()).forEach(model::add);
        tableView.setItems(model);
    }

    @FXML
    public void initialize() {
        tableColumnUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        tableColumnFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        tableColumnLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));

        tableView.getSelectionModel().setSelectionMode(
                SelectionMode.MULTIPLE
        );

        tableView.setItems(model);
    }

    @FXML
    private void deleteFriendship(ActionEvent actionEvent) {
        User selectItem = tableView.getSelectionModel().getSelectedItem();
        if(selectItem!=null){
            service.del_friendship(user.getId().toString(),selectItem.getId().toString());
        }
    }

    @FXML
    private void openChat(ActionEvent actionEvent) {
        Stage stage = (Stage) chatBox.getScene().getWindow();
        if (ChatOpen) {
            chatBox.setVisible(false);
            chatButton.setText("Open Chat");
            stage.setWidth(stage.getWidth() - 500);
            chatBox.setMinWidth(0);
            ChatOpen = false;
        } else {
            chatBox.setVisible(true);
            chatButton.setText("Close Chat");
            stage.setWidth(stage.getWidth() + 500);
            chatBox.setMinWidth(500);
            ChatOpen = true;
        }
    }

    @FXML
    private void sendMessage(ActionEvent actionEvent) {

        if (!ifTextExits()) return;

        Message msg = new Message(user, messageUsers, chatInput.getText(), LocalDateTime.now());
        service.add_message(msg);

    }

    private void createMessageBox(Message msg) {
        String message = (msg instanceof ReplyMessage)
                ?  ((ReplyMessage) msg).replyMessage()
                : msg.getMessage();

        if (msg.getFrom().equals(user))
            message = "You: " + message;
        else
            message = msg.getFrom().getUsername() + ": " + message;

        long nrDim = 0L;
        for (int i = 1; i < message.length(); i++) {
            if (i % 70 == 0)
                message = message.substring(0, i) + "\n" + message.substring(i);

            if (message.charAt(i) == '\n')
                    nrDim++;
        }

         HBox messageBox = new HBox();
         messageBox.setStyle("-fx-border-color: black; -fx-padding: 10; " +
                 "-fx-background-color: " + generateColorCode(msg.getFrom().getUsername()) + ";");
         Label messageLabel = new Label(message);
         messageLabel.setMinWidth(400);
         messageLabel.setMaxWidth(400);
         messageLabel.setMinHeight(20 * (nrDim + 1));

         String finalMessage = message;
         Button replyButton = new Button("Reply");
            replyButton.setOnAction(actionEvent -> {
                if (!ifTextExits()) return;
                ReplyMessage replyMessage = new ReplyMessage(user,messageUsers,chatInput.getText(), LocalDateTime.now(),msg);
                service.add_message(replyMessage);
            });

         messageBox.getChildren().addAll(messageLabel, replyButton);
         chatMessages.getChildren().add(messageBox);
         chatInput.clear();
    }

    private boolean ifTextExits() {
        if(chatInput.getText().isEmpty()) {
            AlertCreator.create(Alert.AlertType.INFORMATION, "Please write a message");
            return false;
        }
        messageUsers.clear();
        messageUsers.addAll(tableView.getSelectionModel().getSelectedItems());
        if(messageUsers.isEmpty()){
            AlertCreator.create(Alert.AlertType.INFORMATION, "Please select a user to send the message to");
            return false;
        }
        return true;
    }

    private String generateColorCode(String username) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(username.getBytes());
            BigInteger no = new BigInteger(1, digest);
            StringBuilder hashText = new StringBuilder(no.toString(16));
            while (hashText.length() < 32) {
                hashText.insert(0, "0");
            }
            return "#" + hashText.substring(0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void update(AccountEvent actionEvent) {

        if(actionEvent.getEvent()== ChangeEvent.REMOVED_FRIENDSHIP){
            if(actionEvent.getFriendship().getFirst().equals(user.getId())
                    || actionEvent.getFriendship().getLast().equals(user.getId())) {
                User foundUser = service.find_user(actionEvent.getFriendship().getFirst()).orElse(new User());
                if (foundUser == user)
                    foundUser = service.find_user(actionEvent.getFriendship().getLast()).orElse(new User());

                model.remove(foundUser);
            }
        }
        if(actionEvent.getEvent()==ChangeEvent.ADD_FRIENDSHIP){
            if(actionEvent.getFriendship().getFirst().equals(user.getId())
            || actionEvent.getFriendship().getLast().equals(user.getId())){

                User foundUser = service.find_user(actionEvent.getFriendship().getFirst()).orElse(new User());
                if (foundUser == user)
                    foundUser = service.find_user(actionEvent.getFriendship().getLast()).orElse(new User());

                model.add(foundUser);
            }
        }
        if(actionEvent.getEvent()==ChangeEvent.SEND_FRIENDREQUEST){
            service.get_all_friend_requests().stream()
                    .filter(friendRequest -> friendRequest.getReceiver().equals(user.getId()) &&
                            friendRequest.getSender().equals(actionEvent.getData().getId()))
                    .forEach(friendRequest ->
                        notificationsButton.setStyle("-fx-background-color: red"));
        }  if(actionEvent.getEvent()==ChangeEvent.REMOVED_FRIENDREQUEST){
            service.get_all_friend_requests().stream()
                    .filter(friendRequest -> friendRequest.getReceiver().equals(user.getId()) &&
                            friendRequest.getSender().equals(actionEvent.getData().getId()))
                    .forEach(friendRequest ->
                        notificationsButton.setStyle("-fx-background-color: white"));
        }

        if(actionEvent.getEvent()==ChangeEvent.ADD_MESSAGE){
            if(actionEvent.getMessage().getTo().contains(user) || actionEvent.getMessage().getFrom().equals(user)){
                createMessageBox(actionEvent.getMessage());
            }
        }
    }

    @FXML
    public void sendFriendRequest(ActionEvent actionEvent) throws IOException {
        if (sendFriendRequestStage == null || !sendFriendRequestStage.isShowing()) {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("sendFR-view.fxml"));
            sendFriendRequestStage = new Stage();
            Pane userLayout = fxmlLoader.load();
            sendFriendRequestStage.setScene(new Scene(userLayout));

            SendFriendRequestController sendFriendRequestController = fxmlLoader.getController();
            sendFriendRequestController.setUser(user);
            sendFriendRequestController.setService(service);

            sendFriendRequestStage.setTitle("Send Friend Request");

            sendFriendRequestStage.show();
        } else {
            sendFriendRequestStage.toFront();
        }
    }

    @FXML
    public void showFriendRequests(ActionEvent actionEvent) throws IOException {
        if (friendRequestsStage == null || !friendRequestsStage.isShowing()) {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("friendRequests-view.fxml"));
            friendRequestsStage = new Stage();
            Pane userLayout = fxmlLoader.load();
            friendRequestsStage.setScene(new Scene(userLayout));

            UserFriendRequests friendRequestsController = fxmlLoader.getController();
            friendRequestsController.setUser(user);
            friendRequestsController.setService(service);

            friendRequestsStage.setTitle("Friend Requests");

            friendRequestsStage.show();
        } else {
            friendRequestsStage.toFront();
        }
    }

    @FXML
    public void openNotificationsTab(ActionEvent actionEvent) throws IOException {
        notificationsButton.setStyle("fx-background-color: white");
        if (notificationsStage == null || !notificationsStage.isShowing()) {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("notifications-view.fxml"));
            notificationsStage = new Stage();
            Pane userLayout = fxmlLoader.load();
            notificationsStage.setScene(new Scene(userLayout));

            NotificationsTab notificationsTabController = fxmlLoader.getController();
            notificationsTabController.setService(service);
            notificationsTabController.setUser(user);


            notificationsStage.setTitle("Notifications");

            notificationsStage.show();
        } else {
            notificationsStage.toFront();
        }
    }
}