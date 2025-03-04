package com.varun.tcp.clockout.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Component
public class TimeInUi extends Application implements CommandLineRunner {
    private static final String FILE_PATH = "timein.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private Stage primaryStage;
    private Label reminderLabel;

    @Override
    public void run(String... args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Time In Logger");

        Label label = new Label("Select Time In:");
        DatePicker datePicker = new DatePicker();
        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, 9);
        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, 0);
        Button submitButton = new Button("Submit");
        reminderLabel = new Label();

        VBox layout = new VBox(10, label, datePicker, hourSpinner, minuteSpinner, submitButton, reminderLabel);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 300, 250);
        primaryStage.setScene(scene);

        submitButton.setOnAction(event -> {
            //LocalDateTime timeIn = LocalDateTime.parse(LocalDate.now() + " " + time, FORMATTER);
            LocalDateTime timeIn = LocalDateTime.of(datePicker.getValue(), java.time.LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue()));
            saveTimeIn(timeIn);
            scheduleReminder(timeIn);
            //primaryStage.close();
        });

        if (!checkExistingTimeIn()) {
            primaryStage.show();
        }
    }

    private boolean checkExistingTimeIn() {
        if (!Files.exists(Paths.get(FILE_PATH))) {
            return false;
        }
        try {
            List<String> lines = Files.readAllLines(Paths.get(FILE_PATH));
            /*if (!lines.isEmpty() && lines.get(0).startsWith(LocalDate.now().toString())) {
                return true;
            }*/
            return !lines.isEmpty();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                    //System.out.println("Reminder: It's been 8 hours since you clocked in!");
                    Platform.runLater(() -> reminderLabel.setText("Reminder: It's been 8 hours since you clocked in!"));
                }
            }, delay);
        }
    }
}
