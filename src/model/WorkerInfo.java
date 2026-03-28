package model;
public class WorkerInfo {
    private String workerId;
    private String host;
    private int port;
    private long lastHeartbeat;

    public WorkerInfo(String workerId, String host, int port) {
        this.workerId = workerId;
        this.host = host;
        this.port = port;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public String getWorkerId() { return workerId; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public long getLastHeartbeat() { return lastHeartbeat; }
    
    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }
}