package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class BootstrapServer {

    private final List<String> activeServents;
    private volatile boolean working = true;

    public BootstrapServer() {
        activeServents = new ArrayList<>();
    }

    /**
     * Expects one command line argument - the port to listen on.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            AppConfig.timestampedErrorPrint("Bootstrap started without port argument.");
        }

        int bsPort = 0;
        try {
            bsPort = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Bootstrap port not valid: " + args[0]);
            System.exit(0);
        }

        AppConfig.timestampedStandardPrint("Bootstrap server started on port: " + bsPort);

        BootstrapServer bs = new BootstrapServer();
        bs.doBootstrap(bsPort);
    }

    public void doBootstrap(int bsPort) {
        Thread cliThread = new Thread(new CLIWorker());
        cliThread.start();

        ServerSocket listenerSocket = null;
        try {
            listenerSocket = new ServerSocket(bsPort);
            listenerSocket.setSoTimeout(1000);
        } catch (IOException e1) {
            AppConfig.timestampedErrorPrint("Problem while opening listener socket.");
            System.exit(0);
        }

        Random rand = new Random(System.currentTimeMillis());

        while (working) {
            try {
                Socket newServentSocket = listenerSocket.accept();

                /*
                 * Handling these messages is intentionally sequential, to avoid problems with
                 * concurrent initial starts.
                 *
                 * In practice, we would have an always-active backbone of servents to avoid this problem.
                 */

                Scanner socketScanner = new Scanner(newServentSocket.getInputStream());
                String message = socketScanner.nextLine();

                /*
                 * New servent has hailed us. He is sending us his own listener port.
                 * He wants to get a listener port from a random active servent,
                 * or -1 if he is the first one.
                 */
                switch (message) {
                    case "Hail" -> {
                        String newServentIp = socketScanner.nextLine();
                        int newServentPort = socketScanner.nextInt();

                        String newServent = newServentIp + ":" + newServentPort;

                        System.out.println("Got: " + newServent);
                        PrintWriter socketWriter = new PrintWriter(newServentSocket.getOutputStream());

                        if (activeServents.isEmpty()) {
                            socketWriter.write("first" + "\n");
                            activeServents.add(newServent); //first one doesn't need to confirm
                            System.out.println("Adding servent: " + newServent);
                        } else {
                            String randServent = activeServents.get(rand.nextInt(activeServents.size()));
                            socketWriter.write(randServent + "\n");
                        }

                        socketWriter.flush();
                        newServentSocket.close();
                    }
                    case "New" -> {
                        // When a servent is confirmed not to be a collider, we add him to the list.
                        String newServentIp = socketScanner.nextLine();
                        int newServentPort = socketScanner.nextInt();

                        String newServent = newServentIp + ":" + newServentPort;

                        System.out.println("Adding servent: " + newServent);

                        activeServents.add(newServent);
                        newServentSocket.close();
                    }
                    case "Dead" -> {
                        String deadServentIp = socketScanner.nextLine();
                        int deadServentPort = socketScanner.nextInt();
                        String deadServent = deadServentIp + ":" + deadServentPort;
                        System.out.println("Removing dead servent: " + deadServent);
                        activeServents.remove(deadServent);
                        newServentSocket.close();
                    }
                }

            } catch (SocketTimeoutException e) {
                //
            } catch (IOException e) {
                AppConfig.timestampedErrorPrint(e);
            }
        }
    }

    private class CLIWorker implements Runnable {
        @Override
        public void run() {
            Scanner sc = new Scanner(System.in);

            String line;
            while (true) {
                line = sc.nextLine();

                if (line.equals("stop")) {
                    working = false;
                    break;
                }
            }

            sc.close();
        }
    }
}
