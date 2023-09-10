import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CourseRegistrationApp extends JFrame {
    private final String DB_URL = "jdbc:mysql://localhost:3306/studnet";
    private final String DB_USER = "root";
    private final String DB_PASSWORD = "root";

    private JComboBox<String> courseComboBox;
    private JTextField studentNameField;
    private JTextArea courseListTextArea;
    private JLabel availableSeatsLabel;
    private JButton dropButton;

    public CourseRegistrationApp() {
        setTitle("Course Registration System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 2));

        JLabel courseLabel = new JLabel("Select Course:");
        courseComboBox = new JComboBox<>();
        populateCourseComboBox();

        JLabel nameLabel = new JLabel("Student Name:");
        studentNameField = new JTextField();

        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                registerStudent();
            }


        });
        dropButton = new JButton("Drop Course");
        dropButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dropCourse();
            }
        });

        panel.add(dropButton);

        availableSeatsLabel = new JLabel();

        courseListTextArea = new JTextArea();
        courseListTextArea.setEditable(false);

        panel.add(courseLabel);
        panel.add(courseComboBox);
        panel.add(nameLabel);
        panel.add(studentNameField);
        panel.add(registerButton);
        panel.add(new JLabel()); // Empty label for spacing
        panel.add(new JLabel("Available Seats:"));
        panel.add(availableSeatsLabel);
        panel.add(new JLabel("Available Courses:"));
        panel.add(new JScrollPane(courseListTextArea)); // Use JScrollPane for the list

        add(panel, BorderLayout.NORTH);

        // Add a list of registered students for each course
        JPanel studentListPanel = new JPanel();
        studentListPanel.setLayout(new BorderLayout());
        JTextArea studentListTextArea = new JTextArea();
        studentListTextArea.setEditable(false);
        studentListPanel.add(new JLabel("Registered Students:"), BorderLayout.NORTH);
        studentListPanel.add(new JScrollPane(studentListTextArea), BorderLayout.CENTER);
        add(studentListPanel, BorderLayout.CENTER);
    }

    private void dropCourse() {
        String selectedCourse = (String) courseComboBox.getSelectedItem();
        String studentName = studentNameField.getText();

        if (selectedCourse != null && !selectedCourse.isEmpty() && !studentName.isEmpty()) {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Check if the student is registered for the selected course
                String checkSql = "SELECT * FROM registrations WHERE course_title = ? AND student_name = ?";
                PreparedStatement checkStatement = connection.prepareStatement(checkSql);
                checkStatement.setString(1, selectedCourse);
                checkStatement.setString(2, studentName);
                ResultSet checkResult = checkStatement.executeQuery();

                if (!checkResult.next()) {
                    JOptionPane.showMessageDialog(this, "Course not found in your registrations.");
                    return;
                }

                // Drop the course
                String sql = "DELETE FROM registrations WHERE course_title = ? AND student_name = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, selectedCourse);
                statement.setString(2, studentName);
                int rowsDeleted = statement.executeUpdate();

                if (rowsDeleted > 0) {
                    JOptionPane.showMessageDialog(this, "Course dropped successfully!");
                    studentNameField.setText("");
                    refreshStudentList(selectedCourse); // Refresh the student list
                    updateAvailableSeats(selectedCourse); // Update available seats
                    updateAvailableCourses(selectedCourse); // Update available courses
                } else {
                    JOptionPane.showMessageDialog(this, "Course drop failed.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Course drop failed.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a course and enter student name.");
        }
    }

    private void populateCourseComboBox() {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT course_title FROM courses";
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String courseTitle = resultSet.getString("course_title");
                courseComboBox.addItem(courseTitle);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Display available seats and available courses when a course is selected
        courseComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedCourse = (String) courseComboBox.getSelectedItem();
                if (selectedCourse != null) {
                    updateAvailableSeats(selectedCourse);
                    updateAvailableCourses(selectedCourse);
                }
            }
        });
    }

    private void registerStudent() {
        String selectedCourse = (String) courseComboBox.getSelectedItem();
        String studentName = studentNameField.getText();

        if (selectedCourse != null && !selectedCourse.isEmpty() && !studentName.isEmpty()) {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Check if the course has available slots
                String slotsCheckSql = "SELECT COUNT(*) AS enrolled FROM registrations WHERE course_title = ?";
                PreparedStatement slotsCheckStatement = connection.prepareStatement(slotsCheckSql);
                slotsCheckStatement.setString(1, selectedCourse);
                ResultSet slotsCheckResult = slotsCheckStatement.executeQuery();

                if (slotsCheckResult.next()) {
                    int enrolledCount = slotsCheckResult.getInt("enrolled");
                    if (enrolledCount >= 30) {
                        JOptionPane.showMessageDialog(this, "Registration failed. Course is full.");
                        return;
                    }
                }

                // Register the student
                String sql = "INSERT INTO registrations (course_title, student_name) VALUES (?, ?)";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, selectedCourse);
                statement.setString(2, studentName);
                statement.executeUpdate();

                JOptionPane.showMessageDialog(this, "Registration successful!");
                studentNameField.setText("");
                refreshStudentList(selectedCourse); // Refresh the student list
                updateAvailableSeats(selectedCourse); // Update available seats
                updateAvailableCourses(selectedCourse); // Update available courses
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Registration failed.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a course and enter student name.");
        }
    }

    private void refreshStudentList(String courseTitle) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT student_name FROM registrations WHERE course_title = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, courseTitle);
            ResultSet resultSet = statement.executeQuery();

            StringBuilder studentList = new StringBuilder();
            while (resultSet.next()) {
                String studentName = resultSet.getString("student_name");
                studentList.append(studentName).append("\n");
            }

            courseListTextArea.setText(studentList.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateAvailableSeats(String courseTitle) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String enrolledSql = "SELECT COUNT(*) AS enrolled FROM registrations WHERE course_title = ?";
            PreparedStatement enrolledStatement = connection.prepareStatement(enrolledSql);
            enrolledStatement.setString(1, courseTitle);
            ResultSet enrolledResult = enrolledStatement.executeQuery();

            if (enrolledResult.next()) {
                int enrolledCount = enrolledResult.getInt("enrolled");
                int availableSeats = 60 - enrolledCount;
                availableSeatsLabel.setText(String.valueOf(availableSeats));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateAvailableCourses(String selectedCourse) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT course_title FROM courses WHERE course_title != ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, selectedCourse);
            ResultSet resultSet = statement.executeQuery();

            StringBuilder availableCourses = new StringBuilder();
            while (resultSet.next()) {
                String courseTitle = resultSet.getString("course_title");
                availableCourses.append(courseTitle).append("\n");
            }

            courseListTextArea.setText(availableCourses.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CourseRegistrationApp app = new CourseRegistrationApp();
            app.setVisible(true);
        });
    }
}
