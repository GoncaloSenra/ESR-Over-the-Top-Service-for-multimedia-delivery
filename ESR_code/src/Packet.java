import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Packet implements Serializable{
    private String data;
    private InetAddress IP;
    private ConcurrentLinkedQueue<InetAddress> path;
    private ConcurrentLinkedQueue<InetAddress> networks;
    private ConcurrentLinkedQueue<InetAddress> prevNetworks;

    public Packet(String data){
        this.data = data;
        this.IP = null;
        this.path = new ConcurrentLinkedQueue<InetAddress>();
        this.networks = new ConcurrentLinkedQueue<InetAddress>();
        this.prevNetworks = new ConcurrentLinkedQueue<InetAddress>();
    }

    public ConcurrentLinkedQueue<InetAddress> getPath() {
        return this.path;
    }

    public ConcurrentLinkedQueue<InetAddress> getNetworks() {
        return this.networks;
    }

    public ConcurrentLinkedQueue<InetAddress> getPrevNetworks() {
        return this.prevNetworks;
    }

    public String getData(){
        return data;
    }


    public void setPath(InetAddress ip){
        this.path.add(ip);
    }

    public void setNetworks(InetAddress ip){
        this.networks.add(ip);
    }

    public void setPrevNetworks(InetAddress ip){
        this.prevNetworks.add(ip);
    }

    public void setPrevNetworksZero(){
        this.prevNetworks.clear();
    }

    public void setPrevNetworksAll(ConcurrentLinkedQueue<InetAddress> n){
        this.prevNetworks.addAll(n);
    }


    public void setData(String data){
        this.data = data;
    }

    public String toString(){
        return "Data: " + data + " IP: " + IP + " Path: " + path.toString() + " Networks: " + networks.toString();
    }

}