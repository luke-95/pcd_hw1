//SENDER//

import java.io.*;
import java.net.*;
import java.util.Scanner;

class StopWaitSender {

    public static void main(String args[]) throws Exception {
        StopWaitSender sws = new StopWaitSender();
        sws.run();
    }

    public void run() throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter no of frames to be sent:");
        int totalFrameCount = scanner.nextInt();
        Socket socket = new Socket("localhost", 9999);
        PrintStream printStream = new PrintStream(socket.getOutputStream());
        for (int currentFrameIndex = 0; currentFrameIndex <= totalFrameCount; ) {
            if (currentFrameIndex >= totalFrameCount) {
                printStream.println("exit");
                break;
            }
            System.out.println("Frame #" + currentFrameIndex +" is sent");
            printStream.println(currentFrameIndex);
            BufferedReader bf = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String ack = bf.readLine();
            if (ack != null) {
                System.out.println("Acknowledgement was Received from receiver");
                currentFrameIndex++;
                Thread.sleep(4000);
            } else {
                printStream.println(currentFrameIndex);
            }
        }
    }
}

