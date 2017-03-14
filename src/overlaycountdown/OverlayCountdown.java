package overlaycountdown;

import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
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

    final double FONT_SIZE = 80;

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

        Scene scene = new Scene(root, Color.TRANSPARENT);
        primaryStage.setScene(scene);

        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.setTitle("Countdown");
        primaryStage.show();

        int timeInSecs = getDutarionFromUser(primaryStage);

        System.out.println(timeInSecs);

        startTimer(hrs, mins, secs, timeInSecs, primaryStage);
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
            return time > 0 && time <= 60;
        };

        Dialog dialog = new Dialog();
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        GridPane pane = new GridPane();

        TextField hrs = new TextField();
        hrs.setPrefColumnCount(3);
        hrs.setPromptText("hrs");
        hrs.setAlignment(Pos.CENTER);
        hrs.textProperty().addListener(l -> okButton.setDisable(!textFieldValidation.test(hrs)));

        TextField mins = new TextField();
        mins.setPrefColumnCount(3);
        mins.setPromptText("mins");
        mins.setAlignment(Pos.CENTER);
        mins.textProperty().addListener(l -> okButton.setDisable(!textFieldValidation.test(mins)));

        TextField secs = new TextField();
        secs.setPrefColumnCount(3);
        secs.setPromptText("secs");
        secs.setAlignment(Pos.CENTER);
        secs.textProperty().addListener(l -> okButton.setDisable(!textFieldValidation.test(secs)));

        pane.addRow(0, hrs,
                new Text(":"), mins,
                new Text(":"), secs);

        dialog.getDialogPane().setContent(pane);
        dialog.initOwner(parent);

        dialog.showAndWait();
        return getTimeInSecs(hrs, mins, secs);
    }

    private int getTimeInSecs(TextField hrsField, TextField minsField, TextField secsField) {
        int hrs = toInt(hrsField.getText());
        int mins = toInt(minsField.getText());
        int secs = toInt(secsField.getText());

        return hrs * 60 * 60 + mins * 60 + secs;
    }

    private int toInt(String str) {
        int num = str.trim().equals("")
                ? 0
                : Integer.parseInt(str.trim());
        return (num < 0) ? 0 : num;
    }

    private void startTimer(Text hrsText, Text minsText, Text secsText, int timeInSec, Stage parent) {
        Thread timerThread = new Thread(() -> {
            int timeInSecs = timeInSec;
            while (timeInSecs >= 0 && parent.isShowing()) {
                int hrs = timeInSecs / 60 / 60;
                int mins = (timeInSecs - hrs * 60 * 60) / 60;
                int secs = (timeInSecs - hrs * 60 * 60 - mins * 60);

                hrsText.setText(String.format("%02d", hrs));
                minsText.setText(String.format("%02d", mins));
                secsText.setText(String.format("%02d", secs));
                LockSupport.parkNanos(1_000_000_000);
                timeInSecs--;
            }
            playNotificationSound();
        });
        timerThread.start();
    }

    private void playNotificationSound() {
        /*
         * The sound effect is permitted for commercial use under 
         * license Creative Commons Attribution 4.0 International License.
         * Downloaded from "http://www.orangefreesounds.com/ringing-clock/"
         */
        MediaPlayer mp = new MediaPlayer(new Media(this.getClass()
                .getResource("/sounds/Ringing-clock.mp3").toString()));
        mp.play();
    }
}
