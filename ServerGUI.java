import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class ServerGUI extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private ServerSocket serverSocket;
    private static final int PORT = 1234;
    private static final Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public ServerGUI() {
        setTitle("Chat Server");
        setSize(600, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Chat display
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setBackground(new Color(245, 245, 245));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Server Chat"));

        // Input and send button
        inputField = new JTextField(40);
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sendButton = new JButton("Send");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout());
        inputPanel.add(inputField);
        inputPanel.add(sendButton);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // Listeners
        sendButton.addActionListener(e -> sendServerMessage());
        inputField.addActionListener(e -> sendServerMessage());

        // Start server thread
        new Thread(this::startServer).start();
    }

    private void sendServerMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;

        String time = TIME_FORMAT.format(new Date());
        chatArea.append(String.format("[%s] Server: %s\n", time, message));
        broadcastMessage("Server: " + message, null);
        inputField.setText("");
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            appendMessage("🚀 Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clientHandlers.add(handler);
                new Thread(handler).start();
                appendMessage("🔗 New client connected: " + clientSocket.getInetAddress());
            }
        } catch (IOException e) {
            appendMessage("❌ Server error: " + e.getMessage());
        }
    }

    public void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String time = TIME_FORMAT.format(new Date());
            chatArea.append(String.format("[%s] %s\n", time, message));
        });
    }

    public void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clientHandlers) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public void removeClient(ClientHandler client) {
        clientHandlers.remove(client);
    }

    // Inner class for handling each client
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            try {
                sendMessage("✅ Connected to the server.");
                String message;
                while ((message = in.readLine()) != null) {
                    appendMessage("💬 Client: " + message);
                    broadcastMessage("Client: " + message, this);
                }
            } catch (IOException e) {
                appendMessage("⚠️ Client disconnected: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {}
                removeClient(this);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerGUI server = new ServerGUI();
            server.setVisible(true);
        });
    }
}
