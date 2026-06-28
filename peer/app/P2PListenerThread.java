package peer.app;

import common.models.Message;
import common.utils.JSONUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import static peer.app.PeerApp.TIMEOUT_MILLIS;

public class P2PListenerThread extends Thread {
    private final ServerSocket serverSocket;

    public P2PListenerThread(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    private void handleConnection(Socket socket) throws Exception {
        if (socket == null) return;
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            socket.setSoTimeout(TIMEOUT_MILLIS);
            Message incomingMessage = JSONUtils.fromJson(in.readUTF());
            socket.setSoTimeout(0);
            if (!incomingMessage.getType().equals(Message.Type.download_request)) {
                socket.close();
                return;
            }
            String fileName = incomingMessage.getFromBody("name");
            String receiverIP = incomingMessage.getFromBody("receiver_ip");
            int receiverPort = incomingMessage.getIntFromBody("receiver_port");
            String receiver = receiverIP + ":" + receiverPort;
            File file = new File(PeerApp.getSharedFolderPath(), fileName);
            if (!file.exists()) {
                socket.close();
                return;
            }
            new TorrentP2PThread(socket, file, receiver).start();
        } catch (Exception e) {
            socket.close();
        }
    }

    @Override
    public void run() {
        while (!PeerApp.isEnded()) {
            try {
                Socket socket = serverSocket.accept();
                handleConnection(socket);
            } catch (Exception e) {
                break;
            }
        }

        try {
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }
    }
}