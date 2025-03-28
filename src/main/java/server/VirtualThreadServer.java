package server;

import lombok.extern.slf4j.Slf4j;
import handler.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用 Virtual Thread 实现的键值服务器
 */
@Slf4j
public class VirtualThreadServer {
    public static void main(String[] args) {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
             ServerSocket serverSocket = new ServerSocket(8080)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // log.debug("Connection established: {}", clientSocket);
                executorService.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            log.error("Server error");
        }
    }
}
