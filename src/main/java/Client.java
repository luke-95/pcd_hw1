import config.AppConfig;
import utils.cli.ClientCommandLineParser;
import utils.cli.CommandLineParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.*;
import java.util.Scanner;

public class Client {
    AppConfig appConfig;

    public static void main(String args[]) {
        CommandLineParser cliParser = new ClientCommandLineParser();
        AppConfig appConfig = new AppConfig();
        cliParser.parseCliArguments(args, appConfig);

        Client client = new Client(appConfig);
        client.run();
    }

    Client(AppConfig config) {
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
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter no of frames to be sent:");



        int framesCount = scanner.nextInt();

        try {
            if (appConfig.getUseUrl()) {
                // Convert URL to IP
                InetAddress address = InetAddress.getByName(new URL(appConfig.getIp()).getHost());
                appConfig.setIp(address.getHostAddress());
            }

            // Start socket
            System.out.println(String.format("Will connect to %s:%d", appConfig.getIp(), appConfig.getPort()));
            Socket socket = new Socket(InetAddress.getByName(new URL(appConfig.getIp()).getHost()), appConfig.getPort());
            System.out.println("Connected");

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
                    Thread.sleep(2000);

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
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }
    }
}
