package tracker.controllers;

import common.models.Message;
import tracker.app.PeerConnectionThread;
import tracker.app.TrackerApp;

import java.util.*;

public class TrackerConnectionController {
    public static Message handleCommand(Message message) {
        String fileName = message.getFromBody("name");
        Map<PeerConnectionThread, String> peerAndHash = findPeers(fileName);
        if (peerAndHash.isEmpty()) {
            return createNotFoundResponse();
        } else if (!isHashEqual(peerAndHash)) {
            return createHashConflictResponse();
        }
        return generateRandomPeer(peerAndHash);
    }

    public static Message getStatusMessage() {
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "status");
        return new Message(body, Message.Type.command);
    }

    public static Message getGetFilesMessage() {
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "get_files_list");
        return new Message(body, Message.Type.command);
    }

    public static Map<String, List<String>> getSends(PeerConnectionThread connection) {
        Message command = createGetSendsCommand();
        Message response = connection.sendAndWaitForResponse(command, 500);
        return response.getFromBody("sent_files");
    }

    public static Map<String, List<String>> getReceives(PeerConnectionThread connection) {
        Message command = createGetReceivesCommand();
        Message response = connection.sendAndWaitForResponse(command, 500);
        return response.getFromBody("received_files");
    }

    private static Message createGetSendsCommand() {
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "get_sends");
        return new Message(body, Message.Type.command);
    }

    private static Message createGetReceivesCommand() {
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "get_receives");
        return new Message(body, Message.Type.command);
    }

    private static Map<PeerConnectionThread, String> findPeers(String fileName) {
        Map<PeerConnectionThread, String> peerConnectionThreads = new HashMap<>();
        for (PeerConnectionThread connection : TrackerApp.getConnections()) {
            for (Map.Entry<String, String> stringStringEntry : connection.getFileAndHashes().entrySet()) {
                if (stringStringEntry.getKey().equals(fileName)) {
                    peerConnectionThreads.put(connection, stringStringEntry.getValue());
                    break;
                }
            }
        }
        return peerConnectionThreads;
    }

    private static Message createNotFoundResponse() {
        HashMap<String, Object> body = new HashMap<>();
        body.put("response", "error");
        body.put("error", "not_found");
        return new Message(body, Message.Type.response);
    }

    private static Message createHashConflictResponse() {
        HashMap<String, Object> body = new HashMap<>();
        body.put("response", "error");
        body.put("error", "multiple_hash");
        return new Message(body, Message.Type.response);
    }

    private static Message createFoundFileResponse(String hash, PeerConnectionThread peerConnectionThread) {
        HashMap<String, Object> body = new HashMap<>();
        body.put("response", "peer_found");
        body.put("md5", hash);
        body.put("peer_have", peerConnectionThread.getOtherSideIP());
        body.put("peer_port", peerConnectionThread.getOtherSidePort());
        return new Message(body, Message.Type.response);
    }

    private static boolean isHashEqual(Map<PeerConnectionThread, String> peerAndHash) {
        Set<String> uniqueHashes = new HashSet<>(peerAndHash.values());
        return uniqueHashes.size() == 1;
    }

    private static Message generateRandomPeer(Map<PeerConnectionThread, String> peerAndHash) {
        List<Map.Entry<PeerConnectionThread, String>> entries = new ArrayList<>(peerAndHash.entrySet());
        Random random = new Random();
        int randomIndex = random.nextInt(entries.size());
        return createFoundFileResponse(entries.get(randomIndex).getValue(), entries.get(randomIndex).getKey());
    }
}