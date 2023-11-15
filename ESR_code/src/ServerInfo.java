import java.net.InetAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerInfo {
    
    private InetAddress address;

    private ConcurrentLinkedQueue<String> videos;

    private long latency;


    public ServerInfo(InetAddress address ,ConcurrentLinkedQueue<String> videos, long latency) {
        this.address = address;
        this.videos = new ConcurrentLinkedQueue<>();
        this.videos.addAll(videos);
        this.latency = latency;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public ConcurrentLinkedQueue<String> getVideos() {
        return this.videos;
    }

    public void setVideos(String video) {
        this.videos.add(video);
    }

    public long getLatency() {
        return this.latency;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }

    public String toString() {
        return "Address: " + this.address + " Videos: " + this.videos + " Latency: " + this.latency;
    }

}
