import config.AppConfig;
import utils.cli.ClientCommandLineParser;
import utils.cli.CommandLineParser;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Calendar;

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
        long startTime = Calendar.getInstance().getTimeInMillis();
        if (appConfig.getUseUDP()) {
            sendWithUdp();
        } else {
            sendWithTcp();
        }
        System.out.println("Finished with time: " + (Calendar.getInstance().getTimeInMillis() - startTime));
    }


    public void sendWithUdp()
    {
        // Open Server Socket
        System.out.println("Using protocol: UDP");
        System.out.println(String.format("Connecting to: %s:%d", appConfig.getIp(), appConfig.getPort()));

        try {
            // Open UDP socket
            DatagramSocket serverSocket = new DatagramSocket();
            serverSocket.setSoTimeout(500);

            // Convert IP to InetAddress
            InetAddress address = InetAddress.getByName(appConfig.getIp());

            // Send Chunk Size
            final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            final DataOutputStream dataOut = new DataOutputStream(byteOut);
            dataOut.writeInt(appConfig.getChunkSize());
            dataOut.close();
            byte[] chunkSizeMessage = byteOut.toByteArray();
            DatagramPacket chunkSizePacket = new DatagramPacket(chunkSizeMessage, chunkSizeMessage.length, address, appConfig.getPort());
            serverSocket.send(chunkSizePacket);

            // Load the file in a buffer
            File file = new File(appConfig.getFilename());
            byte[] fileDataBuffer = new byte[(int) file.length()];
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            bufferedInputStream.read(fileDataBuffer, 0, (int)file.length());

            // Init file transfer
            int bufferOffset;
            int fileSize = (int)file.length();
            int chunkSize = appConfig.getChunkSize();
            double chunkCount = fileSize / chunkSize + ((fileSize % chunkSize == 0) ? 0 : 1);

            // Transfer chunks
            System.out.println("Starting file transfer...");
            System.out.println(String.format("Will send: %s", appConfig.getFilename()));
            System.out.println(String.format("File size: %d", file.length()));
            System.out.println(String.format("Chunk size: %d", appConfig.getChunkSize()));
            for (int chunkIndex = 0; chunkIndex < chunkCount; ++chunkIndex) {
                bufferOffset = chunkIndex * chunkSize;
//                if (chunkIndex == chunkCount - 1) {
//                    // Last piece might be smaller in size, adjust chunkSize accordingly
//                    chunkSize = fileSize - bufferOffset;
//                }

                // Get chunk of file as a message
                byte[] message = Arrays.copyOfRange(fileDataBuffer, bufferOffset, bufferOffset + chunkSize);
                // Send chunk
                System.out.println("Sending chunk: " + chunkIndex);
                DatagramPacket chunkPacket = new DatagramPacket(message, message.length, address, appConfig.getPort());
                serverSocket.send(chunkPacket);

//                // Get ACK
//                try {
//                    byte[] ackBuffer = new byte[chunkSize];
//                    DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
//                    serverSocket.receive(ackPacket);
//                }
//                catch (SocketTimeoutException e) {
//                    // ACK timed out
//                    chunkIndex -= 1;
//                }
            }
            System.out.println("File transfer complete.");

            // Close socket
            serverSocket.close();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendWithTcp()
    {
        System.out.println("Using protocol: TCP");
        System.out.println(String.format("Connecting to: %s:%d", appConfig.getIp(), appConfig.getPort()));

        try {
            // Open TCP Socket
            Socket serverSocket = new Socket(appConfig.getIp(), appConfig.getPort());
            serverSocket.setSoTimeout(500);
            System.out.println("Connected");

            // Send chunk size
            sendInt(serverSocket, appConfig.getChunkSize());

            // Open file
            File file = new File(appConfig.getFilename());

            // Transfer file
            System.out.println("Starting file transfer...");
            System.out.println(String.format("Will send: %s", appConfig.getFilename()));
            System.out.println(String.format("File size: %d", file.length()));
            System.out.println(String.format("Chunk size: %d", appConfig.getChunkSize()));

            handleTransferOverTcp(serverSocket, file);
            System.out.println("File transfer complete.");

            // Close socket connection
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void handleTransferOverTcp(Socket serverSocket, File file) throws IOException
    {
        int chunkSize = appConfig.getChunkSize();
        int fileSize = (int)file.length();
        int bufferOffset;
        long sessionBytesSent = 0;
        int sessionChunksSent = 0;
        double chunkCount = file.length() / chunkSize + ((file.length() % chunkSize == 0) ? 0 : 1);

        // Load file in a buffer
        byte[] fileDataBuffer = new byte[fileSize];
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        bufferedInputStream.read(fileDataBuffer, 0, fileSize);

        // Send file chunk by chunk
        OutputStream serverOutputStream = serverSocket.getOutputStream();

        for (int chunkIndex = 0; chunkIndex < chunkCount; ++chunkIndex) {
            bufferOffset = chunkIndex * chunkSize;
            if (chunkIndex == chunkCount - 1) {
                // Last piece might be smaller in size, adjust chunkSize accordingly
                chunkSize = Math.abs(fileSize - bufferOffset);
            }

            // Send chunk
//                System.out.println("Sending chunk: " + chunkIndex);
            serverOutputStream.write(fileDataBuffer, bufferOffset, chunkSize);

            // -- Stop and Wait implementation --
            if (!appConfig.getUseStreaming()) {
                try {
                    // Check ACK
                    if (receiveInt(serverSocket) == ACK_OK) {
//                            System.out.println(String.format("Received correct ACK for chunk: %d", chunkIndex));
                    } else {
                        // Received bad ACK message -> resend chunk
//                            System.out.println(String.format("Received malformed ACK for chunk: %d", chunkIndex));
                        chunkIndex -= 1;
                    }
                } catch (SocketTimeoutException e) {
                    // ACK timed out -> resend chunk
//                        System.out.println(String.format("Failed to receive ACK for chunk: %d", chunkIndex));
                    chunkIndex -= 1;
                }
            }
            sessionBytesSent += chunkSize;
            sessionChunksSent += 1;
        }
        System.out.println(String.format("Session bytes sent: %d", sessionBytesSent));
        System.out.println(String.format("Session chunks sent: %d", sessionChunksSent));
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
