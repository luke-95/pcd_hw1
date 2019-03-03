import config.AppConfig;
import utils.cli.ClientCommandLineParser;
import utils.cli.CommandLineParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    AppConfig appConfig;

    public static void main(String args[]) {
        CommandLineParser cliParser = new ClientCommandLineParser();
        AppConfig appConfig = new AppConfig();
        cliParser.parseCliArguments(args, appConfig);

        Client client = new Client(appConfig);
        client.run();
        System.out.println("Hello World");
    }

    Client(AppConfig config) {
        this.appConfig = config;
    }

    public void run()
    {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter no of frames to be sent:");

        if (appConfig.getUseUDP()) {

        }

        int framesCount = scanner.nextInt();
        try {
            Socket socket = new Socket(appConfig.getIp(), appConfig.getPort());
            PrintStream printStream = new PrintStream(socket.getOutputStream());

            for (int currentFrameIndex = 0; currentFrameIndex <= framesCount; ) {
                if (currentFrameIndex == framesCount) {
                    printStream.println("exit");
                    break;
                }
                System.out.println("Frame #" + currentFrameIndex +" is sent");
                printStream.println(currentFrameIndex);

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String ack = bufferedReader.readLine();
                if (ack != null) {
                    System.out.println("Acknowledgement was Received from receiver");
                    currentFrameIndex++;
                } else {
                    printStream.println(currentFrameIndex);
                }
            }
        } catch (SocketException socketException) {
            socketException.printStackTrace();
        } catch (UnknownHostException unknownHostException) {
            unknownHostException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public void doNothing() {

    }
}
