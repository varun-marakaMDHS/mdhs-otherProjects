package com.varun.tcp.clockout.ui;

import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class TimeInUi extends Application implements CommandLineRunner {
    private static final String FILE_PATH = "timein.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FORMATTER_ss = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private Stage primaryStage;
    private Label reminderLabel;
    private DatePicker datePicker;
    private Spinner<Integer> hourSpinner;
    private Spinner<Integer> minuteSpinner;
    private Button submitButton;
    private Button editButton;
    private Button clockOutButton;
    private HBox inputBox;
    private TrayIcon trayIcon;
    private ScheduledExecutorService executorService;
    private boolean blinkState = false;

    @Override
    public void run(String... args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Time In Logger");
        Label label = new Label("Select Time In:");
        label.setStyle("-fx-font-size: 14px;");
        datePicker = new DatePicker();
        hourSpinner = new Spinner<>(0, 23, 9);
        minuteSpinner = new Spinner<>(0, 59, 0);
        datePicker.setPrefWidth(130);
        hourSpinner.setPrefWidth(60);
        minuteSpinner.setPrefWidth(60);
        submitButton = new Button("Submit");
        submitButton.setStyle("-fx-font-size: 14px; -fx-pref-width: 120px; -fx-pref-height: 40px;");
        editButton = new Button("Edit");
        editButton.setStyle("-fx-font-size: 14px; -fx-pref-width: 120px; -fx-pref-height: 40px;");
        clockOutButton = new Button("Clock Out Now");
        clockOutButton.setStyle("-fx-font-size: 14px; -fx-pref-width: 160px; -fx-pref-height: 40px;");
        clockOutButton.setVisible(false);
        editButton.setDisable(true);
        reminderLabel = new Label();
        reminderLabel.setWrapText(true);
        ScrollPane scrollPane = new ScrollPane(reminderLabel);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefWidth(100);
        reminderLabel.setStyle("-fx-font-size: 18px;");

        inputBox = new HBox(10, label, datePicker, hourSpinner, minuteSpinner, submitButton);
        inputBox.setAlignment(Pos.CENTER);
        VBox layout = new VBox(10, inputBox, editButton, clockOutButton, scrollPane);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 650, 550);
        primaryStage.setScene(scene);

        submitButton.setOnAction(event -> {
            LocalDateTime timeIn = LocalDateTime.of(datePicker.getValue(), java.time.LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue()));
            saveTimeIn(timeIn);
            scheduleReminder(timeIn);
            disableInputs();
        });
        setupSystemTray(primaryStage);
        editButton.setOnAction(event -> enableInputs());
        clockOutButton.setOnAction(event -> clockOut());
        checkExistingTimeIn();
    }

    private boolean checkExistingTimeIn() {
        if (!Files.exists(Paths.get(FILE_PATH))) {
            primaryStage.show();
            return false;
        }
        try {
            List<String> lines = Files.readAllLines(Paths.get(FILE_PATH));
            if (!lines.isEmpty()) {
                LocalDateTime timeIn = LocalDateTime.parse(lines.get(0), FORMATTER);
                datePicker.setValue(timeIn.toLocalDate());
                hourSpinner.getValueFactory().setValue(timeIn.getHour());
                minuteSpinner.getValueFactory().setValue(timeIn.getMinute());
                if (LocalDateTime.now().isAfter(timeIn.plusHours(8))) {
                    enableInputs();
                } else {
                    disableInputs();
                    scheduleReminder(timeIn);
                }
                primaryStage.show();
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        primaryStage.show();
        return false;
    }

    private void saveTimeIn(LocalDateTime timeIn) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            writer.write(timeIn.format(FORMATTER));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void scheduleReminder(LocalDateTime timeIn) {
        //LocalDateTime reminderTime = timeIn.plusHours(8);
        LocalDateTime reminderTime = timeIn.plusSeconds(8);
        long delay = java.time.Duration.between(LocalDateTime.now(), reminderTime).toMillis();

        if (delay > 0) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    LocalDateTime.now();
                    Platform.runLater(() -> {
                        primaryStage.show();
                        primaryStage.toFront();
                        primaryStage.requestFocus();
                        reminderLabel.setText(reminderLabel.getText() + "\n"+ LocalDateTime.now().format(FORMATTER_ss) + " Reminder: It's been 8 hours since you clocked in!");
                        showClockOutButton();
                        showNotification("App Minimized", "You have a new notification!");
                        startBlinking();
                    });
                }
            }, delay);
        }
    }

    private void showClockOutButton() {
        clockOutButton.setVisible(true);
        animateButton(clockOutButton);
    }

    private void animateButton(Button button) {
        ScaleTransition st = new ScaleTransition(Duration.seconds(0.5), button);
        st.setByX(0.2);
        st.setByY(0.2);
        st.setCycleCount(ScaleTransition.INDEFINITE);
        button.setOnKeyPressed(event -> event.consume());
        st.setAutoReverse(true);
        st.play();
    }

    private void clockOut() {
        clockOutButton.setVisible(false);
        reminderLabel.setText(reminderLabel.getText() + "\n"+ LocalDateTime.now().format(FORMATTER_ss) + " You have clocked out.");
        stopBlinking();
    }

    private void showNotification(String title, String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    private void setupSystemTray(Stage primaryStage) {
        System.setProperty("java.awt.headless", "false");
        if (SystemTray.isSupported()) {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                Image defaultIcon = ImageIO.read(getClass().getResource("/icon.png"));
                trayIcon = new TrayIcon(defaultIcon, "JavaFX App");
                trayIcon.setImageAutoSize(true);
                trayIcon.addActionListener(e -> Platform.runLater(() -> primaryStage.setIconified(false)));

                tray.add(trayIcon);
            } catch (IOException | AWTException e) {
                e.printStackTrace();
            }
        }
    }

    private void startBlinking() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newScheduledThreadPool(1);
            executorService.scheduleAtFixedRate(() -> {
                if (trayIcon != null) {
                    Image iconToShow;
                    try {
                        iconToShow = ImageIO.read(getClass().getResource(blinkState ? "/icon.png" : "/icon_alert.png"));
                        trayIcon.setImage(iconToShow);
                        blinkState = !blinkState;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 500, TimeUnit.MILLISECONDS); // Change icon every 500ms
        }
    }

    private void stopBlinking() {
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
        try {
            Image defaultIcon = ImageIO.read(getClass().getResource("/icon.png"));
            trayIcon.setImage(defaultIcon); // Reset to default icon
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disableInputs() {
        datePicker.setDisable(true);
        hourSpinner.setDisable(true);
        minuteSpinner.setDisable(true);
        submitButton.setDisable(true);
        editButton.setDisable(false);
    }

    private void enableInputs() {
        datePicker.setDisable(false);
        hourSpinner.setDisable(false);
        minuteSpinner.setDisable(false);
        submitButton.setDisable(false);
        editButton.setDisable(true);
    }
}
