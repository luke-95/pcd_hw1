package config;

public class AppConfig {

    private int chunkSize;
    private String filename;
    private int port;
    private String ip;
    private boolean useUDP;
    private boolean useStreaming;
    private boolean useUrl;

    public AppConfig() {
        useUDP = true;
        useStreaming = false;
        port = 80;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Boolean getUseUDP() {
        return useUDP;
    }

    public void setUseUDP(Boolean useUdp) {
        this.useUDP = useUdp;
    }

    public Boolean getUseStreaming() {
        return useStreaming;
    }

    public void setUseStreaming(Boolean useStreaming) {
        this.useStreaming = useStreaming;
    }

    public boolean getUseUrl() {
        return useUrl;
    }

    public void setUseUrl(boolean useUrl) {
        this.useUrl = useUrl;
    }
}
