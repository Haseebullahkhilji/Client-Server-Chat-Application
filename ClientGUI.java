import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientGUI extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public ClientGUI(String serverIP, int port) {
        setTitle("Chat Client");
        setSize(500, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create components with styling
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setBackground(new Color(245, 245, 255));
        chatArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Chat Messages"));

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setPreferredSize(new Dimension(350, 30));
        inputField.setEnabled(false); // enabled only when connected

        sendButton = new JButton("Send");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sendButton.setEnabled(false);

        // Layout for input panel
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        inputPanel.add(inputField);
        inputPanel.add(sendButton);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // Send message when Send button clicked or Enter pressed
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        // Connect to server in background thread
        new Thread(() -> connectToServer(serverIP, port)).start();
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty() || out == null) {
            return;
        }
        // Send message to server
        out.println(message);

        // Display message in chat area with timestamp and "You:" prefix
        String time = TIME_FORMAT.format(new Date());
        chatArea.append(String.format("[%s] You: %s%n", time, message));
        inputField.setText("");
        scrollToBottom();
    }

// In ClientGUI.java, modify the connectToServer method:
private void connectToServer(String serverIP, int port) {
    try {
        socket = new Socket(serverIP, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        SwingUtilities.invokeLater(() -> {
            chatArea.append("✅ Connected to server at " + serverIP + ":" + port + "\n");
            inputField.setEnabled(true);
            sendButton.setEnabled(true);
            inputField.requestFocus();
        });

        // Message listener thread
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    final String msg = message;
                    String time = TIME_FORMAT.format(new Date());
                    SwingUtilities.invokeLater(() -> {
                        chatArea.append(String.format("[%s] %s%n", time, msg));
                        scrollToBottom();
                    });
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    chatArea.append("❌ Connection error: " + e.getMessage() + "\n");
                    inputField.setEnabled(false);
                    sendButton.setEnabled(false);
                });
            }
        }).start();

    } catch (IOException e) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append("❌ Connection failed: " + e.getMessage() + "\n");
        });
    }
}
    private void scrollToBottom() {
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void closeResources() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        SwingUtilities.invokeLater(() -> {
            inputField.setEnabled(false);
            sendButton.setEnabled(false);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String serverIP = JOptionPane.showInputDialog(null, "Enter Server IP:", "127.0.0.1");
            int port = 1234;
            ClientGUI client = new ClientGUI(serverIP, port);
            client.setVisible(true);
        });
    }
}

