import java.net.*;
import java.io.*;

public class Client {
    public static void main(String[] args) {
        final String serverIP = "127.0.0.1";
        final int port = 1234;

        try (Socket socket = new Socket(serverIP, port);
             BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("🔌 Connected to chat server. Type messages below:");

            Thread listener = new Thread(() -> {
                String response;
                try {
                    while ((response = input.readLine()) != null) {
                        System.out.println("📩 " + response);
                    }
                } catch (IOException e) {
                    System.err.println("❌ Connection lost.");
                }
            });

            listener.setDaemon(true);
            listener.start();

            String userInput;
            while ((userInput = keyboard.readLine()) != null) {
                out.println(userInput);
            }

        } catch (IOException e) {
            System.err.println("⚠️ Could not connect to server: " + e.getMessage());
        }
    }
}
