//RECEIVER//

import java.io.*;
import java.net.*;

class StopWaitReceiver {
    public static void main(String args[]) throws Exception {
        StopWaitReceiver swr = new StopWaitReceiver();
        swr.run();
    }



    public void run() throws Exception {
        String receivedMessage ="not_exit", exitMessage ="exit";
        ServerSocket serverSocket = new ServerSocket(9999);
        Socket ss_accept = serverSocket.accept();
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
        System.out.println("ALL FRAMES WERE RECEIVED SUCCESSFULLY");

    }

}