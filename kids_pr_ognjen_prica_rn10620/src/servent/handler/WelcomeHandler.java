package servent.handler;

import app.AppConfig;
import app.FileManager;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.UpdateMessage;
import servent.message.WelcomeMessage;
import servent.message.util.MessageUtil;

public class WelcomeHandler implements MessageHandler {

    private final Message clientMessage;
    private final FileManager fileManager;

    public WelcomeHandler(Message clientMessage, FileManager fileManager) {
        this.clientMessage = clientMessage;
        this.fileManager = fileManager;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.WELCOME) {
            AppConfig.timestampedErrorPrint("Welcome handler got a message that is not WELCOME");
            return;
        }

        WelcomeMessage welcomeMsg = (WelcomeMessage) clientMessage;

        AppConfig.chordState.init(welcomeMsg);
        fileManager.init(welcomeMsg);

        UpdateMessage um = new UpdateMessage(
                AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                AppConfig.chordState.getNextNodeIpAddress(), AppConfig.chordState.getNextNodePort(),
                ""
        );

//        System.out.println(AppConfig.myServentInfo.getIpAddress() + ":" + AppConfig.myServentInfo.getListenerPort());
//        System.out.println(AppConfig.chordState.getNextNodeIpAddress() + ":" + AppConfig.chordState.getNextNodePort());

        MessageUtil.sendMessage(um);
    }

}

