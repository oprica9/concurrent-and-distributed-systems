package app;

import app.model.ServentInfo;
import app.mutex.SuzukiKasamiMutex;
import servent.message.NewNodeMessage;
import servent.message.util.MessageUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ServentInitializer implements Runnable {

    @Override
    public void run() {
        String someServent = getSomeServent();

        if (someServent.isEmpty()) {
            AppConfig.timestampedErrorPrint("Error in contacting bootstrap. Exiting...");
            System.exit(0);
        }

        if (someServent.equals("first")) { // We are first
            AppConfig.timestampedStandardPrint("First node in Chord system.");
            SuzukiKasamiMutex.initToken();
        } else {
            String[] split = someServent.split(":");
            String serventIp = split[0];
            int serventPort = -1;
            try {
                serventPort = Integer.parseInt(split[1]);
            } catch (NumberFormatException e) {
                AppConfig.timestampedErrorPrint("Error in parsing servent information: " + someServent + "(must be in format: ip_address:port)");
                System.exit(0);
            }

            // Add this one, so we have at least one node to send a token request to
//            AppConfig.chordState.getAllNodes().add(new ServentInfo(serventIp, serventPort));
            AppConfig.temp = new ServentInfo(serventIp, serventPort);

            // Bootstrap gave us something else - let that node tell our successor that we are here
            NewNodeMessage nnm = new NewNodeMessage(
                    AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                    serventIp, serventPort
            );
            MessageUtil.sendMessage(nnm);
        }
    }

    private String getSomeServent() {
        int bsPort = AppConfig.BOOTSTRAP_PORT;
        String bsIpAddress = AppConfig.BOOTSTRAP_IP;

        StringBuilder retServent = new StringBuilder();

        try {
            Socket bsSocket = new Socket(bsIpAddress, bsPort);

            PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
            bsWriter.write("Hail\n" + AppConfig.myServentInfo.getIpAddress() + "\n" + AppConfig.myServentInfo.getListenerPort() + "\n");
            bsWriter.flush();

            Scanner bsScanner = new Scanner(bsSocket.getInputStream());
            retServent.append(bsScanner.nextLine());

            bsSocket.close();
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint(e);
        }

        return retServent.toString();
    }

}
