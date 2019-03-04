import config.AppConfig;
import utils.cli.ClientCommandLineParser;
import utils.cli.CommandLineParser;

import java.io.*;
import java.net.*;

public class Client {
    public static int ACK_OK = 100;
    private AppConfig appConfig;

    public static void main(String args[]) {
        CommandLineParser cliParser = new ClientCommandLineParser();
        AppConfig appConfig = new AppConfig();
        cliParser.parseCliArguments(args, appConfig);

        Client client = new Client(appConfig);
        client.run();
    }

    public Client(AppConfig config) {
        this.appConfig = config;
    }

    public void run()
    {
        if (appConfig.getUseUDP()) {
            sendWithUdp();
        } else {
            sendWithTcp();
        }
    }


    public void sendWithUdp()
    {

    }

    public void sendWithTcp()
    {
        try {
            // Open Server Socket
            System.out.println(String.format("Connecting to: %s:%d", appConfig.getIp(), appConfig.getPort()));
            Socket serverSocket = new Socket(appConfig.getIp(), appConfig.getPort());
            serverSocket.setSoTimeout(1000);
            System.out.println("Connected");

            // Send chunk size
            sendInt(serverSocket, appConfig.getChunkSize());

            // Open file
            File file = new File(appConfig.getFilename());

            System.out.println(String.format("Will send: %s", appConfig.getFilename()));
            System.out.println(String.format("File size: %d", file.length()));
            System.out.println(String.format("Chunk size: %d", appConfig.getChunkSize()));


            System.out.println("Starting file transfer...");
            if (appConfig.getUseStreaming()) {
                sendWithStreaming(serverSocket, file);
            } else {
                sendWithStopWait(serverSocket, file);
            }
            System.out.println("File transfer complete.");
            serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sendWithStopWait(Socket serverSocket, File file) throws IOException
    {
        double chunk_count = Math.ceil(file.length() / appConfig.getChunkSize());
        int byteOffset = 0;
        int chunkIndex;

        // Read file
        byte[] fileDataByteArray = new byte[(int) file.length()];
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        bufferedInputStream.read(fileDataByteArray, 0, (int)file.length());

        // Send file chunk by chunk
        OutputStream serverOutputStream = serverSocket.getOutputStream();
        for (chunkIndex = 0; chunkIndex < chunk_count; ++chunkIndex) {
            // Read chunk from file
            byteOffset = chunkIndex * appConfig.getChunkSize();

            // Send chunk
            System.out.println("Sending chunk: " + chunkIndex);
            serverOutputStream.write(fileDataByteArray, byteOffset, appConfig.getChunkSize());

            if (!appConfig.getUseStreaming()) {
                // Check ACK
                try {
                    if (receiveInt(serverSocket) != ACK_OK) {
                        // Received bad ACK message -> resend chunk
                        System.out.println(String.format("Received malformed ACK for chunk: %d", chunkIndex));
                        chunkIndex -= 1;
                    } else {
                        System.out.println(String.format("Received ACK for chunk: %d", chunkIndex));
                    }
                } catch (SocketTimeoutException e) {
                    // ACK timed out -> resend chunk
                    System.out.println(String.format("Failed to receive ACK for chunk: %d", chunkIndex));
                    chunkIndex -= 1;
                }
            }
        }

        // Send any leftover chunks
        boolean complete = false;
        while (!complete) {
            byteOffset += appConfig.getChunkSize();
            serverOutputStream.write(fileDataByteArray, byteOffset, (int)(file.length() - byteOffset));

            // Check ACK
            try {
                if (receiveInt(serverSocket) != ACK_OK) {
                    // Received bad ACK message -> resend chunk
                    System.out.println(String.format("Received malformed ACK for chunk: %d", chunkIndex));
                    complete = false;
                } else {
                    System.out.println(String.format("Received ACK for chunk: %d", chunkIndex));
                }
            } catch (SocketTimeoutException e) {
                // ACK timed out -> resend chunk
                System.out.println(String.format("Failed to receive ACK for chunk: %d", chunkIndex));
                complete = false;
            }
        }

    }


    private void sendWithStreaming(Socket serverSocket, File file) throws IOException {
        // Read file
        byte[] fileDataByteArray = new byte[(int) file.length()];
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        bufferedInputStream.read(fileDataByteArray, 0, fileDataByteArray.length);

        // Stream whole file
        OutputStream serverOutputStream = serverSocket.getOutputStream();
        serverOutputStream.write(fileDataByteArray, 0, fileDataByteArray.length);
        serverOutputStream.flush();
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

    private int receiveInt(Socket clientSocket) throws IOException, SocketTimeoutException {
        int value;
        DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
        value = dataInputStream.readInt();
        return value;
    }
}
