package overlaycountdown;

import java.util.Optional;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author Sourabh Bhat
 */
public class OverlayCountdown extends Application {

    final double FONT_SIZE = 60;
    final Image icon_32x32 = new Image(this.getClass().getResourceAsStream("/images/timer_32x32.png"));
    final Image icon_64x64 = new Image(this.getClass().getResourceAsStream("/images/timer_64x64.png"));
    static String displayMessage = "Time Up!!";
    static volatile int totalTime = 0;
    static volatile int timeRemaining = 0;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Text hrs = new Text("00");
        hrs.setFont(Font.font(FONT_SIZE));

        Text colon1 = new Text(":");
        colon1.setFont(Font.font(FONT_SIZE));

        Text mins = new Text("00");
        mins.setFont(Font.font(FONT_SIZE));

        Text colon2 = new Text(":");
        colon2.setFont(Font.font(FONT_SIZE));

        Text secs = new Text("00");
        secs.setFont(Font.font(FONT_SIZE));

        HBox root = new HBox(hrs, colon1, mins, colon2, secs);
        root.setBackground(Background.EMPTY);
        root.setEffect(new DropShadow(2, 5, 5, Color.GRAY));
        root.setCursor(Cursor.MOVE);
        grabAndDrag(root, primaryStage);
        createContextMenu(root, primaryStage);

        Scene scene = new Scene(root, Color.TRANSPARENT);
        primaryStage.setScene(scene);

        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.getIcons().add(icon_32x32);
        primaryStage.getIcons().add(icon_64x64);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.setTitle("Countdown");
        primaryStage.show();

        int timeInSecs = setupTimer(primaryStage);
        if (timeInSecs == 0) {
            Platform.exit();
        }
        startTimer(hrs, mins, secs, primaryStage);
    }

    private int setupTimer(Stage primaryStage) {
        int timeInSecs = getDutarionFromUser(primaryStage);
        System.out.println(timeInSecs);
        if (timeInSecs != 0) {
            timeRemaining = timeInSecs;
            totalTime = timeInSecs;
        }
        return timeInSecs;
    }

    private int getDutarionFromUser(Stage parent) {

        Predicate<TextField> textFieldValidation = tf -> {
            if (tf.getText().trim().equals("")) {
                return true;
            }
            int time;
            try {
                time = Integer.parseInt(tf.getText());
            } catch (NumberFormatException ex) {
                return false;
            }
            return time > 0 && time < 60;
        };

        Dialog dialog = new Dialog();
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        GridPane pane = new GridPane();

        TextField hrs = new TextField();
        TextField mins = new TextField();
        TextField secs = new TextField();

        hrs.setPrefColumnCount(3);
        hrs.setPromptText("hrs");
        hrs.setAlignment(Pos.CENTER);
        hrs.textProperty().addListener(l -> {
            okButton.setDisable(
                    !textFieldValidation.test(hrs)
                    || !textFieldValidation.test(mins)
                    || !textFieldValidation.test(secs)
            );
        });

        mins.setPrefColumnCount(3);
        mins.setPromptText("mins");
        mins.setAlignment(Pos.CENTER);
        mins.textProperty().addListener(l -> {
            okButton.setDisable(
                    !textFieldValidation.test(hrs)
                    || !textFieldValidation.test(mins)
                    || !textFieldValidation.test(secs)
            );
        });

        secs.setPrefColumnCount(3);
        secs.setPromptText("secs");
        secs.setAlignment(Pos.CENTER);
        secs.textProperty().addListener(l -> {
            okButton.setDisable(
                    !textFieldValidation.test(hrs)
                    || !textFieldValidation.test(mins)
                    || !textFieldValidation.test(secs)
            );
        });

        pane.addRow(0, hrs,
                new Text(":"), mins,
                new Text(":"), secs);

        TextField msgTextField = new TextField(displayMessage);
        pane.add(msgTextField, 0, 1, 5, 1);

        dialog.getDialogPane().setContent(pane);
        dialog.initOwner(parent);

        Optional<ButtonType> buttonType = dialog.showAndWait();
        if (buttonType.get() == ButtonType.OK) {
            displayMessage = msgTextField.getText();
        } else {
            hrs.setText("");
            mins.setText("");
            secs.setText("");
        }
        return getTimeInSecs(hrs, mins, secs);
    }

    private int getTimeInSecs(TextField hrsField, TextField minsField, TextField secsField) {
        int hrs = toInt(hrsField.getText());
        int mins = toInt(minsField.getText());
        int secs = toInt(secsField.getText());

        return hrs * 60 * 60 + mins * 60 + secs;
    }

    private String getTimeString(int timeInSecs) {
        int hrs = timeInSecs / 60 / 60;
        int mins = (timeInSecs - hrs * 60 * 60) / 60;
        int secs = (timeInSecs - hrs * 60 * 60 - mins * 60);

        return String.format("%02d:", hrs)
                + String.format("%02d:", mins)
                + String.format("%02d", secs);
    }

    private int toInt(String str) {
        int num = str.trim().equals("")
                ? 0
                : Integer.parseInt(str.trim());
        return (num < 0) ? 0 : num;
    }

    private void startTimer(Text hrsText, Text minsText, Text secsText, Stage parent) {
        Thread timerThread = new Thread(() -> {
            while (timeRemaining >= 0 && parent.isShowing()) {
                int hrs = timeRemaining / 60 / 60;
                int mins = (timeRemaining - hrs * 60 * 60) / 60;
                int secs = (timeRemaining - hrs * 60 * 60 - mins * 60);

                hrsText.setText(String.format("%02d", hrs));
                minsText.setText(String.format("%02d", mins));
                secsText.setText(String.format("%02d", secs));
                LockSupport.parkNanos(1_000_000_000);
                timeRemaining--;
            }
            playNotificationSound(parent);
        });
        timerThread.start();
    }

    private void playNotificationSound(Stage parent) {
        /*
         * The sound effect is permitted for commercial use under 
         * license Creative Commons Attribution 4.0 International License.
         * Downloaded from "http://www.orangefreesounds.com/ringing-clock/"
         */
        Platform.runLater(() -> {
            MediaPlayer mp = new MediaPlayer(new Media(this.getClass()
                    .getResource("/sounds/Ringing-clock.mp3").toString()));
            mp.play();
            Alert timeUpAlert = new Alert(Alert.AlertType.INFORMATION);
            timeUpAlert.setTitle("Time up (" + getTimeString(totalTime) + ")");
            timeUpAlert.setHeaderText(displayMessage);
            timeUpAlert.initOwner(parent);
            timeUpAlert.showAndWait();
            mp.stop();
            parent.hide();
        });
    }

    double startX;
    double startY;
    double posX;
    double posY;

    private void grabAndDrag(Node root, Stage stage) {
        root.setOnMousePressed(e -> {
            startX = e.getScreenX();
            startY = e.getScreenY();
            posX = stage.getX();
            posY = stage.getY();
        });
        root.setOnMouseDragged(e -> {

            double currX = e.getScreenX();
            double currY = e.getScreenY();
            double dx = currX - startX;
            double dy = currY - startY;

            stage.setX(posX + dx);
            stage.setY(posY + dy);
        });
    }

    private void createContextMenu(Node root, Stage stage) {
        ContextMenu menu = new ContextMenu();

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());

        MenuItem resetItem = new MenuItem("Reset...");
        resetItem.setOnAction(e -> setupTimer(stage));

        menu.getItems().addAll(resetItem, exitItem);

        root.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                menu.show(root, e.getScreenX(), e.getScreenY());
            } else {
                menu.hide();
            }
        });
    }
}
