package peer.controllers;

import common.models.Message;
import common.utils.FileUtils;
import peer.app.P2TConnectionThread;
import peer.app.PeerApp;

import java.util.HashMap;
import java.util.Map;

public class P2TConnectionController {
    public static Message handleCommand(Message message) {
        if (message.getFromBody("command").equals("status")) {
            return status();
        } else if (message.getFromBody("command").equals("get_files_list")) {
            return getFilesList();
        } else if (message.getFromBody("command").equals("get_sends")) {
            return getSends();
        } else if (message.getFromBody("command").equals("get_receives")) {
            return getReceives();
        } else {
            return new Message();
        }
    }

    private static Message getReceives() {
        return createReceiveFiles();
    }

    private static Message getSends() {
        return createSendFiles();
    }

    public static Message getFilesList() {
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "get_files_list");
        body.put("response", "ok");
        body.put("files", FileUtils.listFilesInFolder(PeerApp.getSharedFolderPath()));
        return new Message(body, Message.Type.response);
    }

    public static Message status() {
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "status");
        body.put("response", "ok");
        body.put("peer", PeerApp.getPeerIP());
        body.put("listen_port", PeerApp.getPeerPort());
        return new Message(body, Message.Type.response);
    }

    public static Message sendFileRequest(P2TConnectionThread tracker, String fileName) {
        Message fileRequest = createFileRequest(fileName);
        return tracker.sendAndWaitForResponse(fileRequest, PeerApp.TIMEOUT_MILLIS);
    }

    private static Message createFileRequest(String fileName) {
        HashMap<String, Object> body = new HashMap<>();
        body.put("name", fileName);
        return new Message(body, Message.Type.file_request);
    }

    private static Message createSendFiles() {
        HashMap<String, Object> body = new HashMap<>();
        body.put("response", "ok");
        body.put("command", "get_sends");
        body.put("sent_files", PeerApp.getSentFiles());
        return new Message(body, Message.Type.response);
    }

    private static Message createReceiveFiles() {
        HashMap<String, Object> body = new HashMap<>();
        body.put("response", "ok");
        body.put("command", "get_receives");
        body.put("received_files", PeerApp.getReceivedFiles());
        return new Message(body, Message.Type.response);
    }
}