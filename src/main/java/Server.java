import config.AppConfig;
import utils.cli.ClientCommandLineParser;
import utils.cli.CommandLineParser;
import utils.cli.ServerCommandLineParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Server {

    public static void main(String args[]) throws Exception {
        CommandLineParser cliParser = new ServerCommandLineParser();
        AppConfig appConfig = new AppConfig();
        cliParser.parseCliArguments(args, appConfig);

        Server server = new Server(appConfig);
        server.run();
    }

    AppConfig appConfig;

    Server(AppConfig config) {
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
        String receivedMessage ="not_exit", exitMessage ="exit";

        try {
            ServerSocket serverSocket = new ServerSocket(appConfig.getPort());
            System.out.println(String.format("Server waiting at port: %d", serverSocket.getLocalPort()));

            Socket ss_accept = serverSocket.accept();

            System.out.println(String.format("Connected to: %s", ss_accept.getLocalAddress()));
            BufferedReader ss_bufferedReader = new BufferedReader(new InputStreamReader(ss_accept.getInputStream()));
            PrintStream printStream = new PrintStream(ss_accept.getOutputStream());

            while (receivedMessage.compareTo(exitMessage) != 0) {
                Thread.sleep(1000);
                receivedMessage = ss_bufferedReader.readLine();
                if (receivedMessage.compareTo(exitMessage) == 0) {
                    break;
                }

                System.out.println("Frame #" + receivedMessage +" was received");
                Thread.sleep(500);
                printStream.println("Received");
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

        System.out.println("ALL FRAMES WERE RECEIVED SUCCESSFULLY");
    }
}
