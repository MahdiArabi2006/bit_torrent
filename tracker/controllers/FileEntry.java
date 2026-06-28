package tracker.controllers;

class FileEntry {
    String fileName;
    String md5;
    String ipPort;

    public FileEntry(String fileName, String md5, String ipPort) {
        this.fileName = fileName;
        this.md5 = md5;
        this.ipPort = ipPort;
    }

    public String getFileName() {
        return fileName;
    }

    public String getIpPort() {
        return ipPort;
    }
}
