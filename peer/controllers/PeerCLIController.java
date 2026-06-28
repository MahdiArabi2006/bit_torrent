package peer.controllers;

import common.models.CLICommands;
import common.models.Message;
import common.utils.FileUtils;
import common.utils.JSONUtils;
import common.utils.MD5Hash;
import peer.app.PeerApp;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class PeerCLIController {
    public static String processCommand(String command) {
        if (PeerCommands.DOWNLOAD.matches(command)) {
            String fileName = PeerCommands.DOWNLOAD.getGroup(command, 1);
            return handleDownload(fileName);
        } else if (PeerCommands.LIST_FILES.matches(command)) {
            return handleListFiles();
        } else if (PeerCommands.END.matches(command)) {
            return endProgram();
        } else {
            return CLICommands.invalidCommand;
        }
    }

    private static String handleListFiles() {
        Map<String, String> files = FileUtils.listFilesInFolder(PeerApp.getSharedFolderPath());
        if (files.isEmpty()) {
            return "Repository is empty.";
        }
        Map<String, String> sortedMap = new TreeMap<>(files);
        return fileListToken(sortedMap);
    }

    private static String handleDownload(String fileName) {
        Map<String, String> files = FileUtils.listFilesInFolder(PeerApp.getSharedFolderPath());
        if (files.containsKey(fileName)) return "You already have the file!";
        Message serverResponse = P2TConnectionController.sendFileRequest(PeerApp.getP2TConnection(), fileName);
        String response = serverResponse.getFromBody("response");
        if (response.equals("error")) {
            String error = serverResponse.getFromBody("error");
            if (error.equals("not_found")) return "No peer has the file!";
            else if (error.equals("multiple_hash")) return "Multiple hashes found!";
            else return "";
        }
        String fileHash = serverResponse.getFromBody("md5");
        String peerIP = serverResponse.getFromBody("peer_have");
        int peerPort = serverResponse.getIntFromBody("peer_port");

        File file = new File(PeerApp.getSharedFolderPath(), fileName);

        try (
                Socket peerSocket = new Socket(peerIP, peerPort);
                DataOutputStream out = new DataOutputStream(peerSocket.getOutputStream());
                InputStream in = new BufferedInputStream(peerSocket.getInputStream());
                FileOutputStream newFile = new FileOutputStream(file)
        ) {
            peerSocket.setSoTimeout(PeerApp.TIMEOUT_MILLIS);
            out.writeUTF(JSONUtils.toJson(createDownloadRequest(fileName, fileHash)));
            out.flush();
            peerSocket.setSoTimeout(0);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                newFile.write(buffer, 0, bytesRead);
            }
            newFile.flush();

            String newFileHash = MD5Hash.HashFile(file.getPath());
            if (newFileHash.equals(fileHash)) {
                String sender = peerIP + ":" + peerPort;
                PeerApp.addReceivedFile(sender, file);
                return "File downloaded successfully: " + fileName;
            } else {
                newFile.close();
                file.delete();
                return "The file has been downloaded from peer but is corrupted!";
            }
        } catch (Exception ignored) {
            file.delete();
            return "";
        }
    }

    public static String endProgram() {
        PeerApp.endAll();
        return "";
    }

    private static String fileListToken(Map<String, String> files) {
        StringBuilder token = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, String> stringStringEntry : files.entrySet()) {
            token.append(stringStringEntry.getKey()).append(" ").append(stringStringEntry.getValue());
            if ((count++) != files.size() - 1) {
                token.append("\n");
            }
        }
        return token.toString();
    }

    private static Message createDownloadRequest(String fileName, String fileHash) {
        HashMap<String, Object> body = new HashMap<>();
        body.put("name", fileName);
        body.put("md5", fileHash);
        body.put("receiver_ip", PeerApp.getPeerIP());
        body.put("receiver_port", PeerApp.getPeerPort());
        return new Message(body, Message.Type.download_request);
    }
}
