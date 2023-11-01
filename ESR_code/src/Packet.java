import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;


public class Packet implements Serializable{
    private String data;
    private InetAddress IP;
    private ArrayList<InetAddress> path;
    private ArrayList<InetAddress> pathInv;
    private ConcurrentLinkedQueue<InetAddress> networks;
    private ConcurrentLinkedQueue<InetAddress> prevNetworks;
    private int hops;

    public Packet(String data){
        this.data = data;
        this.IP = null;
        this.path = new ArrayList<InetAddress>();
        this.pathInv = new ArrayList<InetAddress>();
        this.networks = new ConcurrentLinkedQueue<InetAddress>();
        this.prevNetworks = new ConcurrentLinkedQueue<InetAddress>();
        this.hops = 0;
    }

    public ArrayList<InetAddress> getPathInv() {
        return this.pathInv;
    }

    public InetAddress getIP() {
        return this.IP;
    }

    public int getHops() {
        return this.hops;
    }

    public ArrayList<InetAddress> getPath() {
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

    public void setPathInv(InetAddress ip) {
        this.pathInv.add(ip);
    }

    public void setIP(InetAddress IP) {
        this.IP = IP;
    }

    public void setHops(int hops) {
        this.hops = hops;
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
        return "Data: " + data + " IP: " + IP + " Path: " + path.toString() + " Networks: " + networks.toString() + " Hops: " + hops + " PathInv: " + pathInv.toString() + " PrevNetworks: " + prevNetworks.toString();
    }

}