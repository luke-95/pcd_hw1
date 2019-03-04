import config.AppConfig;
import utils.cli.CommandLineParser;
import utils.cli.ServerCommandLineParser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ThreadLocalRandom;

public class Server {

    public static void main(String args[]) throws Exception {
        CommandLineParser cliParser = new ServerCommandLineParser();
        AppConfig appConfig = new AppConfig();
        cliParser.parseCliArguments(args, appConfig);

        Server server = new Server(appConfig);
        server.run();
    }

    AppConfig appConfig;

    public Server(AppConfig config) {
        this.appConfig = config;
    }

    public void run() {
        if (appConfig.getUseUDP())
        {
            receiveWithUdp();
        } else {
            receiveWithTcp();
        }
    }

    public void receiveWithUdp() {

    }

    public void receiveWithTcp() {


        try {
            ServerSocket serverSocket = null;
            serverSocket = new ServerSocket(appConfig.getPort());
            System.out.println(String.format("Waiting at port: %d", serverSocket.getLocalPort()));

            while(true) {
                // Wait for new connection
                Socket clientSocket = null;
                clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(2000);
                System.out.println(String.format("Client connected: %s:%d", clientSocket.getLocalAddress(), clientSocket.getLocalPort()));

                // Receive Chunk size
                appConfig.setChunkSize(receiveInt(clientSocket));
                System.out.println(String.format("Chunk size: %d", appConfig.getChunkSize()));

                System.out.println("Starting file transfer...");
                if (appConfig.getUseStreaming()) {
                    receiveWithStreaming(clientSocket);
                } else {
                    receiveWithStopAndWait(clientSocket);
                }
                System.out.println("Transfer complete.");
            }
        } catch (FileNotFoundException fileNotFoundException){
            fileNotFoundException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void receiveWithStopAndWait(Socket clientSocket) throws IOException {
        int bytesReceived;
        int frameCount = 0;
        byte[] buffer = new byte[appConfig.getChunkSize()];

        // Open the remote socket's input stream
        InputStream clientInputStream = clientSocket.getInputStream();

        // Open a local output stream, to write the received file
        OutputStream localFileOutputStream = new FileOutputStream("test_output.txt");

        // Receive chunks
        while ((bytesReceived = clientInputStream.read(buffer)) != -1) {

            int randomNum = ThreadLocalRandom.current().nextInt(1, 20 + 1);
            if (randomNum == 1) {
                // 1 in 20 chance to miss a reply.
                // Ignore the buffer's contents
            } else {
                // Write received chunk to file
                localFileOutputStream.write(buffer, 0, bytesReceived);
                System.out.println(String.format("Received chunk: %d", frameCount));

                // Reply with ACK message
                sendInt(clientSocket, Client.ACK_OK);
                System.out.println(String.format("Sent ACK for chunk: %d", frameCount));

                frameCount += 1;
            }

        }
        // Close the FileOutputStream handle
        localFileOutputStream.close();
    }

    private void receiveWithStreaming(Socket clientSocket) throws IOException {
        int bytesRead;
        // File transfer
        InputStream clientInputStream = clientSocket.getInputStream();
        OutputStream localFileOutputStream = new FileOutputStream("test_output.txt");
        byte[] buffer = new byte[appConfig.getChunkSize()];
        while ((bytesRead = clientInputStream.read(buffer)) != -1) {
            localFileOutputStream.write(buffer, 0, bytesRead);
        }
        // Closing the FileOutputStream handle
        localFileOutputStream.close();
    }

    private void sendInt(Socket serverSocket, int value)
    {
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(serverSocket.getOutputStream());
            dataOutputStream.writeInt(value);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private int receiveInt(Socket clientSocket) {
        int value = -1;
        try {
            DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
            value = dataInputStream.readInt();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return value;
    }
}
