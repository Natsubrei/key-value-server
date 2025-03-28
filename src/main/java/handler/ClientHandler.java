package handler;

import lombok.extern.slf4j.Slf4j;
import store.StoreFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

@Slf4j
public class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            String command;
            while ((command = in.readLine()) != null) {
                // log.debug("command: {}", command);
                String[] s = command.split(" ");
                switch (s[0]) {
                    case "PUT":
                        StoreFactory.getMap().put(s[1], s[2]);
                        out.println("OK\n");
                        break;
                    case "GET":
                        out.println(StoreFactory.getMap().get(s[1]) + "\n");
                        break;
                    case "DELETE":
                        StoreFactory.getMap().remove(s[1]);
                        out.println("OK\n");
                        break;
                    case "EXIT":
                        clientSocket.close();
                        return;
                    default:
                        out.println("ERROR: Unknown command\n");
                }
            }
        } catch (IOException e) {
            log.debug("Client handler error: {}", e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                log.debug("Error closing client socket: {}", e.getMessage());
            }
        }
    }
}
