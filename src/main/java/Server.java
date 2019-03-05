import config.AppConfig;
import utils.cli.CommandLineParser;
import utils.cli.ServerCommandLineParser;

import java.io.*;
import java.net.*;
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
        System.out.println("Using protocol: UDP");
        try {
            // Open UDP socket
            DatagramSocket serverSocket = new DatagramSocket(appConfig.getPort());
            System.out.println(String.format("Waiting at port: %d", serverSocket.getLocalPort()));

            boolean receiving;
            while (true)
            {
                // Receive Chunk Size
                int receivedFramesCount = 0;
                byte[] chunkSizeBuffer = new byte[4];
                DatagramPacket chunkSizePacket = new DatagramPacket(chunkSizeBuffer, chunkSizeBuffer.length);
                serverSocket.receive(chunkSizePacket);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(chunkSizeBuffer);
                DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
                int chunkSize = dataInputStream.readInt();
                appConfig.setChunkSize(32);
                System.out.println("Received chunk size: " + chunkSize);
                serverSocket.setSoTimeout(2000);

                // Receive File
                System.out.println("Starting file transfer...");
                receiving = true;
                while(receiving) {
                    OutputStream localFileOutputStream = new FileOutputStream("test_udp_output.txt");
                    byte[] message = new byte[chunkSize];

                    try {
                        // Receive packet and retrieve message
                        DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
                        serverSocket.receive(receivedPacket);
                        message = receivedPacket.getData();
                        localFileOutputStream.write(message, 0, message.length);
//                        System.out.println(String.format("Received chunk: %d", receivedFramesCount));
                        receivedFramesCount += 1;
                    } catch (SocketTimeoutException e){
                        receiving = false;
                    }
                }
                System.out.println("Transfer complete.");

                //infinite timeout
                serverSocket.setSoTimeout(0);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void receiveWithTcp() {
        System.out.println("Using protocol: TCP");

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

                // Receive ChunkSize message
                appConfig.setChunkSize(receiveInt(clientSocket));
                System.out.println(String.format("Chunk size: %d", appConfig.getChunkSize()));

                // Receive the file
                System.out.println("Starting file transfer...");
                handleTransferOverTcp(clientSocket);
                System.out.println("Transfer complete.");
            }
        } catch (FileNotFoundException fileNotFoundException){
            fileNotFoundException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void handleTransferOverTcp(Socket clientSocket) throws IOException {
        int bytesReceived;
        int sessionBytesReceived = 0;
        int sessionChunksReceived = 0;
        byte[] buffer = new byte[appConfig.getChunkSize()];

        // Open the remote socket's input stream
        InputStream clientInputStream = clientSocket.getInputStream();

        // Open a local output stream, to write the received file
        OutputStream localFileOutputStream = new FileOutputStream("test_tcp_output.txt");

        // Receive chunks
        while ((bytesReceived = clientInputStream.read(buffer)) != -1) {

            if (appConfig.getUseStreaming())
            {
                // -- Streaming implementation --
                localFileOutputStream.write(buffer, 0, bytesReceived);
//                System.out.println(String.format("Received chunk: %d", sessionChunksReceived));

            }
            else
            {
                // -- Stop and Wait implementation --
                // Random for a 1 in 100 chance to "miss" (ignore) a reply.
                int randomNum = ThreadLocalRandom.current().nextInt(1, 100 + 1);
                if (randomNum == 50) {
                    // Ignore the received chunk
                } else {
                    // Write received chunk to file
                    localFileOutputStream.write(buffer, 0, bytesReceived);
//                    System.out.println(String.format("Received chunk: %d", sessionChunksReceived));

                    // Reply with ACK message
                    sendInt(clientSocket, Client.ACK_OK);
//                    System.out.println(String.format("Sent ACK for chunk: %d", sessionChunksReceived));
                }
            }
            sessionBytesReceived += bytesReceived;
            sessionChunksReceived += 1;
        }
        System.out.println(String.format("Session bytes received: %d", sessionBytesReceived));
        System.out.println(String.format("Session chunks received: %d", sessionChunksReceived));
        // Close the FileOutputStream handle
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
