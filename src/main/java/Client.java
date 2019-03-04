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
        // Read file
        byte[] fileDataByteArray = new byte[(int) file.length()];
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        OutputStream serverOutputStream = serverSocket.getOutputStream();

        // Send file chunk by chunk
        for (int chunkIndex = 0; chunkIndex < chunk_count; ++chunkIndex) {
            // Read chunk from file
            bufferedInputStream.read(fileDataByteArray, 0, appConfig.getChunkSize());

            // Send chunk
            System.out.println("Sending chunk: " + chunkIndex);
            serverOutputStream.write(fileDataByteArray, 0, appConfig.getChunkSize());

            // Check ACK
            if (receiveInt(serverSocket) != ACK_OK) {
                // Received bad chunk index -> resend chunk
                System.out.println(String.format("Failed to receive ACK for chunk: %d", chunkIndex));
                chunkIndex -= 1;
            } else {
                System.out.println(String.format("Received ACK for chunk: %d", chunkIndex));
            }
        }
        serverOutputStream.flush();
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
