package tracker.controllers;

import common.models.CLICommands;
import tracker.app.PeerConnectionThread;
import tracker.app.TrackerApp;

import java.util.*;

public class TrackerCLIController {
    public static String processCommand(String command) {
        if (TrackerCommands.REFRESH_FILES.matches(command)) {
            return refreshFiles();
        } else if (TrackerCommands.RESET_CONNECTIONS.matches(command)) {
            return resetConnections();
        } else if (TrackerCommands.LIST_PEERS.matches(command)) {
            return listPeers();
        } else if (TrackerCommands.LIST_FILES.matches(command)) {
            String IPAndPort = TrackerCommands.LIST_FILES.getGroup(command, 1);
            return listFiles(IPAndPort);
        } else if (TrackerCommands.END.matches(command)) {
            return endProgram();
        } else if (TrackerCommands.GET_SENDS.matches(command)) {
            String IPAndPort = TrackerCommands.GET_SENDS.getGroup(command, 1);
            return getSends(IPAndPort);
        } else if (TrackerCommands.GET_RECEIVES.matches(command)) {
            String IPAndPort = TrackerCommands.GET_RECEIVES.getGroup(command, 1);
            return getReceives(IPAndPort);
        } else {
            return CLICommands.invalidCommand;
        }
    }

    private static String getReceives(String IPAndPort) {
        String[] input = IPAndPort.split(":");
        String IP = input[0];
        int port = Integer.parseInt(input[1]);
        PeerConnectionThread peerConnectionThread = TrackerApp.getConnectionByIpPort(IP, port);
        if (peerConnectionThread == null) {
            return "Peer not found.";
        }
        Map<String, List<String>> receives = new HashMap<>(TrackerConnectionController.getReceives(peerConnectionThread));
        if (receives.isEmpty()) return "No files received by " + IPAndPort;
        return transfersFilesListToken(receives);
    }

    private static String getSends(String IPAndPort) {
        String[] input = IPAndPort.split(":");
        String IP = input[0];
        int port = Integer.parseInt(input[1]);
        PeerConnectionThread peerConnectionThread = TrackerApp.getConnectionByIpPort(IP, port);
        if (peerConnectionThread == null) {
            return "Peer not found.";
        }
        Map<String, List<String>> sends = new HashMap<>(TrackerConnectionController.getSends(peerConnectionThread));
        if (sends.isEmpty()) return "No files sent by " + IPAndPort;
        return transfersFilesListToken(sends);
    }

    private static String listFiles(String IPAndPort) {
        String[] input = IPAndPort.split(":");
        String IP = input[0];
        int port = Integer.parseInt(input[1]);
        PeerConnectionThread peerConnectionThread = TrackerApp.getConnectionByIpPort(IP, port);
        if (peerConnectionThread == null) {
            return "Peer not found.";
        }
        if (peerConnectionThread.getFileAndHashes().isEmpty()) {
            return "Repository is empty.";
        }
        Map<String, String> sortedMap = new TreeMap<>(peerConnectionThread.getFileAndHashes());
        return filesListToken(sortedMap);
    }

    private static String listPeers() {
        if (TrackerApp.getConnections().isEmpty()) {
            return "No peers connected.";
        }
        return peerListToken();
    }

    private static String resetConnections() {
        Iterator<PeerConnectionThread> iterator = TrackerApp.getConnections().iterator();
        while (iterator.hasNext()) {
            PeerConnectionThread connection = iterator.next();
            boolean success = connection.refreshStatus();
            if (!success) {
                iterator.remove();
            } else {
                boolean success1 = connection.refreshFileList();
                if (!success1) {
                    iterator.remove();
                }
            }
        }
        return "";
    }

    private static String refreshFiles() {
        List<PeerConnectionThread> copy = new ArrayList<>(TrackerApp.getConnections());
        for (PeerConnectionThread peerConnectionThread : copy) {
            if (!peerConnectionThread.refreshFileList()) {
                TrackerApp.getConnections().remove(peerConnectionThread);
            }
        }
        return "";
    }

    private static String endProgram() {
        TrackerApp.endAll();
        return "";
    }

    private static String peerListToken() {
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < TrackerApp.getConnections().size(); i++) {
            PeerConnectionThread connection = TrackerApp.getConnections().get(i);
            message.append(connection.getOtherSideIP()).append(":").append(connection.getOtherSidePort());
            if (i != TrackerApp.getConnections().size() - 1) {
                message.append("\n");
            }
        }
        return message.toString();
    }

    private static String filesListToken(Map<String, String> fileAndHashes) {
        StringBuilder message = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, String> stringStringEntry : fileAndHashes.entrySet()) {
            message.append(stringStringEntry.getKey()).append(" ").append(stringStringEntry.getValue());
            if ((count++) != fileAndHashes.size() - 1) {
                message.append("\n");
            }
        }
        return message.toString();
    }

    private static String transfersFilesListToken(Map<String, List<String>> map) {
        List<FileEntry> flies = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String IPAndPort = entry.getKey();
            for (String fileNameAndHash : entry.getValue()) {
                String[] fileAndHash = fileNameAndHash.split(" ");
                flies.add(new FileEntry(fileAndHash[0], fileAndHash[1], IPAndPort));
            }
        }
        flies.sort(Comparator
                .comparing(FileEntry::getFileName)
                .thenComparing(FileEntry::getIpPort)
        );

        StringBuilder token = new StringBuilder();
        int count = 0;
        for (FileEntry fly : flies) {
            token.append(fly.fileName).append(" ").append(fly.md5).append(" - ").append(fly.ipPort);
            count++;
            if (count != flies.size()) {
                token.append("\n");
            }
        }
        return token.toString();
    }
}