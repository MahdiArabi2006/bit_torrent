package peer.app;

import common.utils.MD5Hash;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.*;

public class PeerApp {
    public static final int TIMEOUT_MILLIS = 500;
    private static String IP;
    private static int port;
    private static String sharedFolderPath;
    private static P2PListenerThread listenerThread;
    private static P2TConnectionThread connectionThread;
    private static final List<TorrentP2PThread> torrentP2PThreads = new ArrayList<>();
    private static final Map<String, List<String>> sentFiles = new HashMap<>();
    private static final Map<String, List<String>> receivedFiles = new HashMap<>();

    private static boolean exitFlag = false;

    public static boolean isEnded() {
        return exitFlag;
    }

    public static void initFromArgs(String[] args) throws Exception {
        String[] arg0 = args[0].split(":");
        String[] arg1 = args[1].split(":");
        IP = arg0[0];
        port = Integer.parseInt(arg0[1]);
        String trackerIP = arg1[0];
        int trackerPort = Integer.parseInt(arg1[1]);
        sharedFolderPath = args[2];
        try {
            listenerThread = new P2PListenerThread(port);
            connectionThread = new P2TConnectionThread(new Socket(trackerIP, trackerPort));
        } catch (IOException e) {
            System.err.println("Request Timed out.");
        }
    }

    public static void endAll() {
        exitFlag = true;
        connectionThread.end();
        for (TorrentP2PThread torrentP2PThread : torrentP2PThreads) {
            torrentP2PThread.end();
        }
        sentFiles.clear();
        receivedFiles.clear();
    }

    public static void connectTracker() {
        if (connectionThread != null && !connectionThread.isAlive()) {
            connectionThread.start();
        } else {
            throw new IllegalStateException("connect thread is already running or not set.");
        }
    }

    public static void startListening() {
        if (listenerThread != null && !listenerThread.isAlive()) {
            listenerThread.start();
        } else {
            throw new IllegalStateException("listener thread is already running or not set.");
        }
    }

    public static void removeTorrentP2PThread(TorrentP2PThread torrentP2PThread) {
        torrentP2PThread.end();
        PeerApp.torrentP2PThreads.remove(torrentP2PThread);
    }

    public static void addTorrentP2PThread(TorrentP2PThread torrentP2PThread) {
        if (torrentP2PThread != null && torrentP2PThread.isAlive()) {
            PeerApp.torrentP2PThreads.add(torrentP2PThread);
        }
    }

    public static String getSharedFolderPath() {
        return sharedFolderPath;
    }

    public static void addSentFile(String receiver, File file) {
        if (PeerApp.sentFiles.containsKey(receiver)) {
            PeerApp.sentFiles.get(receiver).add(file.getName() + " " + MD5Hash.HashFile(file.getPath()));
        } else {
            List<String> list = new ArrayList<>();
            list.add(file.getName() + " " + MD5Hash.HashFile(file.getPath()));
            PeerApp.sentFiles.put(receiver, list);
        }
    }

    public static void addReceivedFile(String sender, File file) {
        if (PeerApp.receivedFiles.containsKey(sender)) {
            PeerApp.receivedFiles.get(sender).add(file.getName() + " " + MD5Hash.HashFile(file.getPath()));
        } else {
            List<String> list = new ArrayList<>();
            list.add(file.getName() + " " + MD5Hash.HashFile(file.getPath()));
            PeerApp.receivedFiles.put(sender, list);
        }
    }

    public static String getPeerIP() {
        return IP;
    }

    public static int getPeerPort() {
        return port;
    }

    public static Map<String, List<String>> getSentFiles() {
        return sentFiles;
    }

    public static Map<String, List<String>> getReceivedFiles() {
        return receivedFiles;
    }

    public static P2TConnectionThread getP2TConnection() {
        return connectionThread;
    }
}
