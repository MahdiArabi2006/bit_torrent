package peer.app;

import common.utils.MD5Hash;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TorrentP2PThread extends Thread {
    private final Socket socket;
    private final File file;
    private final String receiver;
    private final DataOutputStream dataOutputStream;
    private final AtomicBoolean end = new AtomicBoolean(false);

    public TorrentP2PThread(Socket socket, File file, String receiver) throws IOException {
        this.socket = socket;
        this.file = file;
        this.receiver = receiver;
        this.dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        PeerApp.addTorrentP2PThread(this);
    }

    @Override
    public void run() {
        try (
                FileInputStream fileInput = new FileInputStream(file)
        ) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInput.read(buffer)) != -1 && !end.get()) {
                dataOutputStream.write(buffer, 0, bytesRead);
            }
            dataOutputStream.flush();
            PeerApp.addSentFile(receiver, file);
        } catch (Exception ignored) {
        } finally {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception ignored) {
            }
            PeerApp.removeTorrentP2PThread(this);
        }
    }

    public void end() {
        end.set(true);
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ignored) {
        }
    }
}