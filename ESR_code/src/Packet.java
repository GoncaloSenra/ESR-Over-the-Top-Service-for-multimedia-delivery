import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Packet implements Serializable{
    private String data;
    private InetAddress IP;
    private ConcurrentLinkedQueue<InetAddress> path;
    public Packet(String data, InetAddress IP){
        this.data = data;
        this.IP = IP;
        this.path = new ConcurrentLinkedQueue<InetAddress>();
        this.path.add(IP);
    }

    public Packet(String data){
        this.data = data;
        this.IP = null;
        this.path = new ConcurrentLinkedQueue<InetAddress>();
    }

    public ConcurrentLinkedQueue<InetAddress> getPath() {
        return this.path;
    }

    public String getData(){
        return data;
    }

    public InetAddress getIP(){
        return IP;
    }


    public void setPath(InetAddress ip){
        this.path.add(ip);
    }

    public void setData(String data){
        this.data = data;
    }

    public void setIP(InetAddress IP){
        this.IP = IP;
    }


    public String toString(){
        return "Data: " + data + " IP: " + IP + " Path: " + path.toString();
    }

}