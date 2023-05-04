package com.example.WGUSoftware2.controller;

import com.example.WGUSoftware2.model.Appointments;
import com.example.WGUSoftware2.utility.Library;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.converter.LocalTimeStringConverter;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ModifyAppointmentController implements Initializable {
    @FXML
    private TextField appointmentIdTxt;

    @FXML
    private ComboBox<Integer> contactIdCB;

    @FXML
    private ComboBox<Integer> customerIdCB;

    @FXML
    private TextField descriptionTxt;

    @FXML
    private DatePicker endDateDP;

    @FXML
    private Spinner<LocalTime> endTimeSpinner;

    @FXML
    private TextField locationTxt;

    @FXML
    private DatePicker startDateDP;

    @FXML
    private Spinner<LocalTime> startTimeSpinner;

    @FXML
    private TextField titleTxt;

    @FXML
    private TextField typeTxt;

    @FXML
    private ComboBox<Integer> userIdCB;

    /**
     * Confirms if the user wants to go back and switches to the appointments page.
     * @param event action on a button
     * @throws IOException catches RUNTIME ERROR
     */
    @FXML
    void cancelHandler(ActionEvent event) throws IOException {
        // Pop up a confirmation to confirm if the user wants to go back
        Library.switchScreen(event, Library.appointmentsUrl);
    }

    /**
     * Takes the items in the text fields and updates the element in the database and switches to appointments page.
     * @param event action on a button
     * @throws IOException catches RUNTIME ERROR
     */
    @FXML
    void modifyHandler(ActionEvent event) throws IOException {
        // Save the contents to the database and run exception handling
        try {
            int appointmentID = Integer.parseInt(appointmentIdTxt.getText());
            String title = titleTxt.getText();
            String description = descriptionTxt.getText();
            String location = locationTxt.getText();
            String type = typeTxt.getText();

            // Check if fields are empty
            if (title.isEmpty() || description.isEmpty() || location.isEmpty() || type.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Input fields cannot be empty.");
                alert.showAndWait();
                return;
            }

            int contactID = contactIdCB.getValue();
            int customerID = customerIdCB.getValue();
            int userID = userIdCB.getValue();

            // Initial available dates
            LocalDate startDate = startDateDP.getValue();
            LocalTime startTime = startTimeSpinner.getValue();
            LocalDate endDate = endDateDP.getValue();
            LocalTime endTime = endTimeSpinner.getValue();
            // Check if startTime and endTime are within business hours
            if (startTime.isBefore(LocalTime.of(8, 0)) || endTime.isAfter(LocalTime.of(22, 0))) {
                // Display an error message to the user
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Invalid appointment time");
                alert.setContentText("Appointments can only be scheduled between 8:00 a.m. and 10:00 p.m. EST, including weekends.");
                alert.showAndWait();
                return; // Exit the method without saving the appointment
            }
            // Check if startDate is equal to or before endDate
            if (startDate.isAfter(endDate)) {
                // Display an error message to the user
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Invalid appointment time");
                alert.setContentText("Start date must be before or equal to end date.");
                alert.showAndWait();
                return; // Exit the method without saving the appointment
            }

            // Check if startTime is equal to or before endTime
            if (!startTime.isBefore(endTime)) {
                // Display an error message to the user
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Invalid appointment time");
                alert.setContentText("Start time must be before end time.");
                alert.showAndWait();
                return; // Exit the method without saving the appointment
            }

            ZonedDateTime startDateTime = ZonedDateTime.of(startDate, startTime, ZoneId.systemDefault());
            ZonedDateTime endDateTime = ZonedDateTime.of(endDate, endTime, ZoneId.systemDefault());

            // Check for overlapping appointments
            if (Appointments.modifyCheckAppointmentOverlap(customerID, startDateTime, endDateTime, appointmentID)) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Overlapping appointment");
                alert.setContentText("The selected customer already has an appointment scheduled during the specified time.");
                alert.showAndWait();
                return;
            }
            Appointments.updateAppointment(appointmentID, title, description, location, type, contactID, customerID, userID, startDateTime, endDateTime);
            Library.switchScreen(event, Library.appointmentsUrl);
        }catch(NumberFormatException e){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Dialogue");
            alert.setContentText("Please enter a valid value for each input field");
            alert.showAndWait();
        }catch (NullPointerException e){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Dialogue");
            alert.setContentText("Please enter a valid value for each input field");
            alert.showAndWait();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void sendAppointment(Appointments appointment) throws SQLException {
        Appointments.setAppointmentFields(appointment, appointmentIdTxt, titleTxt, descriptionTxt, locationTxt, typeTxt, contactIdCB, customerIdCB, userIdCB, startDateDP, endDateDP, startTimeSpinner, endTimeSpinner);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            // Load available ids into combo box
            ObservableList<Integer> contactIDs = Appointments.getAllContactIDs();
            contactIdCB.setItems(contactIDs);
            ObservableList<Integer> customerIDs = Appointments.getAllCustomerIDs();
            customerIdCB.setItems(customerIDs);
            ObservableList<Integer> userIDs = Appointments.getAllUserIDs();
            userIdCB.setItems(userIDs);

            // Start time spinner
            SpinnerValueFactory<LocalTime> startTimeValueFactory = new SpinnerValueFactory<LocalTime>() {
                {
                    // Set initial value and wrap-around behavior
                    setConverter(new LocalTimeStringConverter(DateTimeFormatter.ofPattern("h:mm a"), null));
                    setWrapAround(true);
                }

                @Override
                public void decrement(int steps) {
                    // Decrement the value by the specified number of steps
                    setValue(getValue().minusMinutes(steps * 15));
                }

                @Override
                public void increment(int steps) {
                    // Increment the value by the specified number of steps
                    setValue(getValue().plusMinutes(steps * 15));
                }
            };

            // End time spinner
            SpinnerValueFactory<LocalTime> endTimeValueFactory = new SpinnerValueFactory<LocalTime>() {
                {
                    // Set initial value and wrap-around behavior
                    setConverter(new LocalTimeStringConverter(DateTimeFormatter.ofPattern("h:mm a"), null));
                    setWrapAround(false);
                }

                @Override
                public void decrement(int steps) {
                    // Decrement the value by the specified number of steps
                    setValue(getValue().minusMinutes(steps * 15));
                }

                @Override
                public void increment(int steps) {
                    // Increment the value by the specified number of steps
                    setValue(getValue().plusMinutes(steps * 15));
                }
            };
            startTimeSpinner.setValueFactory(startTimeValueFactory);
            endTimeSpinner.setValueFactory(endTimeValueFactory);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
