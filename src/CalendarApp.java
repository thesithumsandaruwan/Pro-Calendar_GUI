import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.text.SimpleDateFormat;

class Event implements Serializable {
    String title;
    String data;
    LocalTime startTime;
    LocalTime endTime;
    boolean isRecurringDaily;
    boolean isRecurringWeekly;

    public Event(String title, String data, LocalTime startTime, LocalTime endTime, boolean isRecurringDaily, boolean isRecurringWeekly) {
        this.title = title;
        this.data = data;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isRecurringDaily = isRecurringDaily;
        this.isRecurringWeekly = isRecurringWeekly;
    }

    @Override
    public String toString() {
        return title + " (" + startTime + " - " + endTime + ")" +
                (isRecurringDaily ? " [Daily]" : "") +
                (isRecurringWeekly ? " [Weekly]" : "")+
                (data.isEmpty() ? "" : " [" + data + "]");
    }
}

public class CalendarApp extends JFrame {
    private JPanel calendarPanel;
    private JButton[][] dayButtons;
    private JTextArea eventArea;
    private JComboBox<String> viewComboBox;
    private JButton addEventButton, editEventButton, deleteEventButton;
    private JButton setPresentDateButton, setDayOffButton;
    private Map<LocalDate, List<Event>> events;
    private Set<LocalDate> daysOff;
    private LocalDate selectedDate;
    private LocalDate presentDate;

    public CalendarApp() {
        events = new HashMap<>();
        daysOff = new HashSet<>();
        selectedDate = LocalDate.of(2024, 7, 1);
        presentDate = LocalDate.of(2024, 7, 1);

        setTitle("July 2024 Calendar");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        createCalendarPanel();
        createControlPanel();
        createEventPanel();

        loadEvents();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveEvents();
            }
        });
    }

    private void createCalendarPanel() {
    calendarPanel = new JPanel();
    calendarPanel.setLayout(new BoxLayout(calendarPanel, BoxLayout.Y_AXIS));
    
    calendarPanel.add(Box.createVerticalStrut(10));
    
    JLabel titleLabel = new JLabel("2024 July", SwingConstants.CENTER);
    titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    titleLabel.setFont(new Font("", Font.BOLD, 24));
    calendarPanel.add(titleLabel);
    
    JPanel daysPanel = new JPanel(new GridLayout(6, 7));
    dayButtons = new JButton[5][7];
    
    String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    for (String dayName : dayNames) {
        JLabel dayLabel = new JLabel(dayName, SwingConstants.CENTER);
        daysPanel.add(dayLabel);
    }

    for (int i = 0; i < 5; i++) {
        for (int j = 0; j < 7; j++) {
            dayButtons[i][j] = new JButton();
            dayButtons[i][j].addActionListener(e -> {
                JButton source = (JButton) e.getSource();
                if (!source.getText().isEmpty()) {
                    selectedDate = LocalDate.of(2024, 7, Integer.parseInt(source.getText()));
                    updateEventArea();
                }
            });
            daysPanel.add(dayButtons[i][j]);
        }
    }
    
    updateCalendar();
    calendarPanel.add(daysPanel);
    add(calendarPanel, BorderLayout.CENTER);
}


    private void createControlPanel() {
        JPanel controlPanel = new JPanel();
        viewComboBox = new JComboBox<>(new String[]{"Daily", "Weekly", "Monthly"});
        viewComboBox.addActionListener(e -> updateEventArea());

        addEventButton = new JButton("Add Event");
        addEventButton.addActionListener(e -> addEvent());

        editEventButton = new JButton("Edit Event");
        editEventButton.addActionListener(e -> editEvent());

        deleteEventButton = new JButton("Delete Event");
        deleteEventButton.addActionListener(e -> deleteEvent());

        setPresentDateButton = new JButton("Set Present Date");
        setPresentDateButton.addActionListener(e -> setPresentDate());

        setDayOffButton = new JButton("Set/Unset Day Off");
        setDayOffButton.addActionListener(e -> toggleDayOff());

        controlPanel.add(viewComboBox);
        controlPanel.add(addEventButton);
        controlPanel.add(editEventButton);
        controlPanel.add(deleteEventButton);
        controlPanel.add(setPresentDateButton);
        controlPanel.add(setDayOffButton);

        add(controlPanel, BorderLayout.NORTH);
    }

    private void createEventPanel() {
        eventArea = new JTextArea();
        eventArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(eventArea);
        add(scrollPane, BorderLayout.SOUTH);
    }

    private void updateCalendar() {
        LocalDate date = LocalDate.of(2024, 7, 1);
        int dayOfWeek = date.getDayOfWeek().getValue() % 7;

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 7; j++) {
                if (i == 0 && j < dayOfWeek) {
                    dayButtons[i][j].setText("");
                    dayButtons[i][j].setEnabled(false);
                } else if (date.getMonthValue() == 7) {
                    dayButtons[i][j].setText(String.valueOf(date.getDayOfMonth()));
                    dayButtons[i][j].setEnabled(true);
                    if (daysOff.contains(date)) {
                        events.remove(date);
                        dayButtons[i][j].setBackground(Color.RED);
                    } else if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                        if (events.containsKey(date)) {
                        dayButtons[i][j].setBackground(Color.YELLOW);
                        }
                        else {
                        dayButtons[i][j].setBackground(Color.LIGHT_GRAY);
                        }
                    } else if (events.containsKey(date)) {
                        dayButtons[i][j].setBackground(Color.GREEN);
                    } else {
                        dayButtons[i][j].setBackground(null);
                    }
                    if (date.isBefore(presentDate)) {
                        dayButtons[i][j].setEnabled(false);
                    }
                    date = date.plusDays(1);
                } else {
                    dayButtons[i][j].setText("");
                    dayButtons[i][j].setEnabled(false);
                }
            }
        }
    }

    private void updateEventArea() {
        eventArea.setText("");
        String viewType = (String) viewComboBox.getSelectedItem();

        switch (viewType) {
            case "Daily":
                displayDailyView();
                break;
            case "Weekly":
                displayWeeklyView();
                break;
            case "Monthly":
                displayMonthlyView();
                break;
        }
    }

    private void displayDailyView() {
        List<Event> dailyEvents = events.getOrDefault(selectedDate, new ArrayList<>());
        eventArea.append("Events for " + selectedDate + ":\n");
        for (Event event : dailyEvents) {
            eventArea.append("- " + event + "\n");
        }
    }

    private void displayWeeklyView() {
        LocalDate startOfWeek = selectedDate.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        eventArea.append("Events for week of " + startOfWeek + " to " + endOfWeek + ":\n");
        for (LocalDate date = startOfWeek; !date.isAfter(endOfWeek); date = date.plusDays(1)) {
            List<Event> dailyEvents = events.getOrDefault(date, new ArrayList<>());
            if (!dailyEvents.isEmpty()) {
                eventArea.append(date + ":\n");
                for (Event event : dailyEvents) {
                    eventArea.append("- " + event + "\n");
                }
            }
        }
    }

    private void displayMonthlyView() {
        eventArea.append("Events for July 2024:\n");
        for (LocalDate date = LocalDate.of(2024, 7, 1); date.getMonthValue() == 7; date = date.plusDays(1)) {
            List<Event> dailyEvents = events.getOrDefault(date, new ArrayList<>());
            if (!dailyEvents.isEmpty()) {
                eventArea.append(date + ":\n");
                for (Event event : dailyEvents) {
                    eventArea.append("- " + event + "\n");
                }
            }
        }
    }

    private void addEvent() {
        if (daysOff.contains(selectedDate)) {
            JOptionPane.showMessageDialog(this, "Cannot add events on dayoffs.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedDate.isBefore(presentDate)) {
            JOptionPane.showMessageDialog(this, "Cannot add events for past dates.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedDate.getDayOfWeek() == DayOfWeek.SATURDAY || selectedDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "This is a weekend day. Do you want to add an event anyway?",
                    "Weekend Warning",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        JPanel panel = new JPanel(new GridLayout(0, 1));
        JTextField titleField = new JTextField(20);
        JTextField dataField = new JTextField(50);
        JComboBox<String> timeComboBox1 = new JComboBox<>();
        JComboBox<String> timeComboBox2 = new JComboBox<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        for (int i = 0; i < 48; i++) {
            timeComboBox1.addItem(timeFormat.format(calendar.getTime()));
            timeComboBox2.addItem(timeFormat.format(calendar.getTime()));
            calendar.add(Calendar.MINUTE, 30);
        }
        JCheckBox recurDailyBox = new JCheckBox("Recur Daily");
        JCheckBox recurWeeklyBox = new JCheckBox("Recur Weekly");

        panel.add(new JLabel("Event Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Data:"));
        panel.add(dataField);
        panel.add(new JLabel("Start Time:"));
        panel.add(timeComboBox1);
        panel.add(new JLabel("End Time:"));
        panel.add(timeComboBox2);
        panel.add(recurDailyBox);
        panel.add(recurWeeklyBox);

        int result = JOptionPane.showConfirmDialog(null, panel, "Add Event",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String title = titleField.getText();
            String data = dataField.getText();
            LocalTime startTime = LocalTime.parse((String) timeComboBox1.getSelectedItem());
            LocalTime endTime = LocalTime.parse((String) timeComboBox2.getSelectedItem());
            boolean isRecurringDaily = recurDailyBox.isSelected();
            boolean isRecurringWeekly = recurWeeklyBox.isSelected();

            if (title.isEmpty() || startTime.equals(endTime) || endTime.isBefore(startTime)) {
                JOptionPane.showMessageDialog(this, "Invalid event details.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Event newEvent = new Event(title, data, startTime, endTime, isRecurringDaily, isRecurringWeekly);

            if (isEventOverlapping(newEvent, selectedDate)) {
                JOptionPane.showMessageDialog(this, "This event overlaps with an existing event.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            events.computeIfAbsent(selectedDate, k -> new ArrayList<>()).add(newEvent);

            if (isRecurringDaily || isRecurringWeekly) {
                addRecurringEvent(newEvent, isRecurringDaily, isRecurringWeekly);
            }

            events.get(selectedDate).sort(Comparator.comparing(e -> e.startTime));
            updateEventArea();
            updateCalendar();
        }
    }

    private boolean isEventOverlapping(Event newEvent, LocalDate thisDate) {
        List<Event> dailyEvents = events.getOrDefault(thisDate, new ArrayList<>());
        for (Event existingEvent : dailyEvents) {
            if (!(newEvent.endTime.isBefore(existingEvent.startTime) || newEvent.startTime.isAfter(existingEvent.endTime))) {
                return true;
            }
        }
        return false;
    }

    private void addRecurringEvent(Event event, boolean isRecurringDaily, boolean isRecurringWeekly) {
        LocalDate currentDate = selectedDate.plusDays(1);
        int choice = JOptionPane.showConfirmDialog(this,
                    "Do you want to add this event indcluding weekends?",
                    "Event adding Warning",
                    JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.NO_OPTION) {
                while (currentDate.getMonthValue() == 7 && currentDate.getYear() == 2024) {
                if (isRecurringDaily || (isRecurringWeekly && currentDate.getDayOfWeek() == selectedDate.getDayOfWeek())) {
                    if ((currentDate.getDayOfWeek() != DayOfWeek.SATURDAY && currentDate.getDayOfWeek() != DayOfWeek.SUNDAY) && !daysOff.contains(currentDate)) {
                        if (!isEventOverlapping(event, currentDate)) {
                            events.computeIfAbsent(currentDate, k -> new ArrayList<>()).add(event);
                            events.get(currentDate).sort(Comparator.comparing(e -> e.startTime));
                        }
                    }
                }
                currentDate = currentDate.plusDays(1);
                }
            }
            if (choice ==JOptionPane.YES_OPTION) {
                while (currentDate.getMonthValue() == 7 && currentDate.getYear() == 2024) {
                if (isRecurringDaily || (isRecurringWeekly && currentDate.getDayOfWeek() == selectedDate.getDayOfWeek())) {
                    if (!daysOff.contains(currentDate)) {
                        if (!isEventOverlapping(event, currentDate)) {
                            events.computeIfAbsent(currentDate, k -> new ArrayList<>()).add(event);
                            events.get(currentDate).sort(Comparator.comparing(e -> e.startTime));
                        }
                    }
                }
                currentDate = currentDate.plusDays(1);
                }
            }
    }

    private void editEvent() {
        List<Event> dailyEvents = events.get(selectedDate);
        if (dailyEvents == null || dailyEvents.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No events to edit on this date.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Event selectedEvent = (Event) JOptionPane.showInputDialog(this,
                "Select event to edit:",
                "Edit Event",
                JOptionPane.QUESTION_MESSAGE,
                null,
                dailyEvents.toArray(),
                dailyEvents.get(0));

        if (selectedEvent != null) {
            JPanel panel = new JPanel(new GridLayout(0, 1));
            JTextField titleField = new JTextField(selectedEvent.title, 20);
            JTextField dataField = new JTextField(selectedEvent.data, 50);
            JSpinner startTimeSpinner = new JSpinner(new SpinnerDateModel());
            JSpinner endTimeSpinner = new JSpinner(new SpinnerDateModel());
            JSpinner.DateEditor startTimeEditor = new JSpinner.DateEditor(startTimeSpinner, "HH:mm");
            JSpinner.DateEditor endTimeEditor = new JSpinner.DateEditor(endTimeSpinner, "HH:mm");
            startTimeSpinner.setEditor(startTimeEditor);
            endTimeSpinner.setEditor(endTimeEditor);
            startTimeSpinner.setValue(Date.from(selectedEvent.startTime.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant()));
            endTimeSpinner.setValue(Date.from(selectedEvent.endTime.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant()));
            JCheckBox recurDailyBox = new JCheckBox("Recur Daily", selectedEvent.isRecurringDaily);
            JCheckBox recurWeeklyBox = new JCheckBox("Recur Weekly", selectedEvent.isRecurringWeekly);

            panel.add(new JLabel("Event Title:"));
            panel.add(titleField);
            panel.add(new JLabel("Data:"));
            panel.add(dataField);
            panel.add(new JLabel("Start Time:"));
            panel.add(startTimeSpinner);
            panel.add(new JLabel("End Time:"));
            panel.add(endTimeSpinner);
            panel.add(recurDailyBox);
            panel.add(recurWeeklyBox);

            int result = JOptionPane.showConfirmDialog(null, panel, "Edit Event",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String title = titleField.getText();
                String data = dataField.getText();
                LocalTime startTime = LocalTime.parse(startTimeEditor.getFormat().format(startTimeSpinner.getValue()));
                LocalTime endTime = LocalTime.parse(endTimeEditor.getFormat().format(endTimeSpinner.getValue()));
                boolean isRecurringDaily = recurDailyBox.isSelected();
                boolean isRecurringWeekly = recurWeeklyBox.isSelected();

                if (title.isEmpty() || startTime.equals(endTime) || endTime.isBefore(startTime)) {
                    JOptionPane.showMessageDialog(this, "Invalid event details.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Event updatedEvent = new Event(title, data, startTime, endTime, isRecurringDaily, isRecurringWeekly);

                if (isEventOverlapping(updatedEvent, selectedDate)) {
                    JOptionPane.showMessageDialog(this, "This event overlaps with an existing event.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                dailyEvents.remove(selectedEvent);
                //dailyEvents.add(updatedEvent);

                if (selectedEvent.isRecurringDaily || selectedEvent.isRecurringWeekly) {
                    removeRecurringEvent(selectedEvent, presentDate);
                }
                
                if (isRecurringDaily || isRecurringWeekly) {
                    events.computeIfAbsent(selectedDate, k -> new ArrayList<>()).add(updatedEvent);
                    addRecurringEvent(updatedEvent, isRecurringDaily, isRecurringWeekly);
                }
                else {
                    events.computeIfAbsent(selectedDate, k -> new ArrayList<>()).add(updatedEvent);
                }

                events.get(selectedDate).sort(Comparator.comparing(e -> e.startTime));
                updateEventArea();
                updateCalendar();
            }
        }
    }

    private void deleteEvent() {
        List<Event> dailyEvents = events.get(selectedDate);
        if (dailyEvents == null || dailyEvents.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No events to delete on this date.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Event selectedEvent = (Event) JOptionPane.showInputDialog(this,
                "Select event to delete:",
                "Delete Event",
                JOptionPane.QUESTION_MESSAGE,
                null,
                dailyEvents.toArray(),
                dailyEvents.get(0));

        if (selectedEvent != null) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Delete this event" + (selectedEvent.isRecurringDaily || selectedEvent.isRecurringWeekly ? " and all its recurrences? (Yes - Recurrsion delete , No - Single delete)" : "?"),
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                dailyEvents.remove(selectedEvent);
                if (dailyEvents.isEmpty()) {
                    events.remove(selectedDate);
                }

                if (selectedEvent.isRecurringDaily || selectedEvent.isRecurringWeekly) {
                    removeRecurringEvent(selectedEvent, presentDate);
                }

                updateEventArea();
                updateCalendar();
            }
            else if (choice == JOptionPane.NO_OPTION && (selectedEvent.isRecurringDaily || selectedEvent.isRecurringWeekly)) {
                dailyEvents.remove(selectedEvent);
                if (dailyEvents.isEmpty()) {
                    events.remove(selectedDate);
                }

                updateEventArea();
                updateCalendar();
            }
        }
    }

    private void removeRecurringEvent(Event event, LocalDate startDate) {
    for (Map.Entry<LocalDate, List<Event>> entry : events.entrySet()) {
        LocalDate entryDate = entry.getKey();
        if (entryDate.isBefore(startDate)) {
            continue;
        }
        entry.getValue().removeIf(e -> e.title.equals(event.title) &&
                e.startTime.equals(event.startTime) &&
                e.endTime.equals(event.endTime));
    }
    events.values().removeIf(List::isEmpty);
}

    private void setPresentDate() {
        String input = JOptionPane.showInputDialog(this, "Enter present date (yyyy-MM-dd):");
        try {
            LocalDate newPresentDate = LocalDate.parse(input);
            if (newPresentDate.getYear() == 2024 && newPresentDate.getMonthValue() == 7) {
                presentDate = newPresentDate;
                updateCalendar();
                JOptionPane.showMessageDialog(this, "Present date set to: " + presentDate);
            } else {
                JOptionPane.showMessageDialog(this, "Date must be in July 2024.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Please use yyyy-MM-dd.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleDayOff() {
        /*if (selectedDate.getDayOfWeek() == DayOfWeek.SATURDAY || selectedDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            JOptionPane.showMessageDialog(this, "Weekends are always off.", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }*/

        if (daysOff.contains(selectedDate)) {
            daysOff.remove(selectedDate);
            JOptionPane.showMessageDialog(this, selectedDate + " is no longer a day off.", "Day Off Removed", JOptionPane.INFORMATION_MESSAGE);
        } else {
            daysOff.add(selectedDate);
            JOptionPane.showMessageDialog(this, selectedDate + " is now set as a day off.", "Day Off Set", JOptionPane.INFORMATION_MESSAGE);
        }

        updateCalendar();
    }

    private void saveEvents() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("events.dat"))) {
            oos.writeObject(events);
            oos.writeObject(daysOff);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadEvents() {
        File file = new File("events.dat");
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                events = (Map<LocalDate, List<Event>>) ois.readObject();
                daysOff = (Set<LocalDate>) ois.readObject();
                updateCalendar();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalendarApp().setVisible(true));
    }
}
