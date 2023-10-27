import java.io.*;
import java.net.*;
import java.util.*;


public class Packet implements Serializable{
    private String data;
    
    public Packet(String data){
        this.data = data;
    }

    public String getData(){
        return data;
    }

    public void setData(String data){
        this.data = data;
    }

    public String toString(){
        return "Data: " + data;
    }

}