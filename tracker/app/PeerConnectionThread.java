package tracker.app;

import common.models.ConnectionThread;
import common.models.Message;
import common.utils.JSONUtils;
import tracker.controllers.TrackerConnectionController;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

import static tracker.app.TrackerApp.TIMEOUT_MILLIS;

public class PeerConnectionThread extends ConnectionThread {
    private HashMap<String, String> fileAndHashes;

    public PeerConnectionThread(Socket socket) throws IOException {
        super(socket);
    }

    @Override
    public boolean initialHandshake() {
        try {
            socket.setSoTimeout(TIMEOUT_MILLIS);
            sendMessage(TrackerConnectionController.getStatusMessage());
            Message status = JSONUtils.fromJson(dataInputStream.readUTF());
            sendMessage(TrackerConnectionController.getGetFilesMessage());
            Message filesList = JSONUtils.fromJson(dataInputStream.readUTF());

            socket.setSoTimeout(0);

            this.otherSideIP = status.getFromBody("peer");
            this.otherSidePort = status.getIntFromBody("listen_port");
            TrackerApp.addPeerConnection(this);
            this.fileAndHashes = new HashMap<>(filesList.getFromBody("files"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean refreshStatus() {
        try {
            Message status = sendAndWaitForResponse(TrackerConnectionController.getStatusMessage(),
                    TIMEOUT_MILLIS);
            this.otherSideIP = status.getFromBody("peer");
            this.otherSidePort = status.getIntFromBody("listen_port");
            return true;
        } catch (Exception e) {
            TrackerApp.removePeerConnection(this);
            return false;
        }
    }

    public boolean refreshFileList() {
        try {
            Message filesList = sendAndWaitForResponse(TrackerConnectionController.getGetFilesMessage(),
                    TIMEOUT_MILLIS);
            this.fileAndHashes = new HashMap<>(filesList.getFromBody("files"));
            return true;
        } catch (Exception e) {
            TrackerApp.removePeerConnection(this);
            return false;
        }
    }

    @Override
    protected boolean handleMessage(Message message) {
        if (message.getType() == Message.Type.file_request) {
            sendMessage(TrackerConnectionController.handleCommand(message));
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        super.run();
        TrackerApp.removePeerConnection(this);
    }

    public HashMap<String, String> getFileAndHashes() {
        return fileAndHashes;
    }
}
