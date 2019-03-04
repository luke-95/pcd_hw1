import config.AppConfig;
import utils.cli.ClientCommandLineParser;
import utils.cli.CommandLineParser;

import java.io.*;
import java.net.*;
import java.util.Arrays;

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
        // Open Server Socket
        System.out.println("Using protocol: UDP");
        System.out.println(String.format("Connecting to: %s:%d", appConfig.getIp(), appConfig.getPort()));

        try {
            DatagramSocket serverSocket = new DatagramSocket();
            serverSocket.setSoTimeout(1000);

            InetAddress address = InetAddress.getByName(appConfig.getIp());


            // Open the file
            File file = new File(appConfig.getFilename());

            // Read file
            byte[] fileByteArray = new byte[(int) file.length()];
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            bufferedInputStream.read(fileByteArray, 0, (int)file.length());

            double chunkCount = Math.ceil(file.length() / appConfig.getChunkSize());
            int chunkOffset = 0;
            int ackSequence = 0;
            int chunkSize = appConfig.getChunkSize();
            for (int chunkIndex = 0; chunkIndex < chunkCount; ++chunkIndex) {
                chunkOffset = chunkIndex * chunkSize;

                // Get chunk of file as a message
                byte[] message = Arrays.copyOfRange(fileByteArray, chunkOffset, chunkOffset + chunkSize);
                // Send chunk
                DatagramPacket sendPacket = new DatagramPacket(message, message.length, address, appConfig.getPort());
                serverSocket.send(sendPacket);


                // Get ACK
                try {
                    byte[] ackBuffer = new byte[chunkSize];
                    DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                    serverSocket.receive(ackPacket);
                }
                catch (SocketTimeoutException e) {
                    // ACK timed out
                    chunkIndex -= 1;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendWithTcp()
    {
        // Open Server Socket
        System.out.println("Using protocol: TCP");
        System.out.println(String.format("Connecting to: %s:%d", appConfig.getIp(), appConfig.getPort()));

        try {
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
            handleTransferOverTcp(serverSocket, file);

            System.out.println("File transfer complete.");
            serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void handleTransferOverTcp(Socket serverSocket, File file) throws IOException
    {
        int chunkSize = appConfig.getChunkSize();
        int fileSize = (int)file.length();
        int byteOffset;
        double chunkCount = file.length() / chunkSize + ((file.length() % chunkSize == 0) ? 0 : 1);

        // Read file
        byte[] fileDataByteArray = new byte[fileSize];
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        bufferedInputStream.read(fileDataByteArray, 0, fileSize);

        // Send file chunk by chunk
        OutputStream serverOutputStream = serverSocket.getOutputStream();
        for (int chunkIndex = 0; chunkIndex < chunkCount; ++chunkIndex) {
            // Read chunk from file
            byteOffset = chunkIndex * chunkSize;

            if (chunkIndex == chunkCount - 1) {
                // Last piece might be smaller in size, adjust chunkSize accordingly
                chunkSize = fileSize - byteOffset;
            }

            // Send chunk
            System.out.println("Sending chunk: " + chunkIndex);
            serverOutputStream.write(fileDataByteArray, byteOffset, chunkSize);

            // -- Stop and Wait implementation --
            if (!appConfig.getUseStreaming()) {
                try {
                    // Check ACK
                    if (receiveInt(serverSocket) == ACK_OK) {
                        System.out.println(String.format("Received correct ACK for chunk: %d", chunkIndex));
                    } else {
                        // Received bad ACK message -> resend chunk
                        System.out.println(String.format("Received malformed ACK for chunk: %d", chunkIndex));
                        chunkIndex -= 1;
                    }
                } catch (SocketTimeoutException e) {
                    // ACK timed out -> resend chunk
                    System.out.println(String.format("Failed to receive ACK for chunk: %d", chunkIndex));
                    chunkIndex -= 1;
                }
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
