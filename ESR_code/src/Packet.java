import java.io.*;
import java.net.*;
import java.util.*;


public class Packet implements Serializable{
    private String data;
    InetAddress IP;
    
    public Packet(String data, InetAddress IP){
        this.data = data;
        this.IP = IP;
    }

    public String getData(){
        return data;
    }

    public InetAddress getIP(){
        return IP;
    }



    public void setData(String data){
        this.data = data;
    }

    public void setIP(InetAddress IP){
        this.IP = IP;
    }

    public String toString(){
        return "Data: " + data + " IP: " + IP;
    }

}