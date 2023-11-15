import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;


public class Packet implements Serializable{
    private String data; // conteudo do pacote
    private ArrayList<InetAddress> path; // caminho ida
    private ArrayList<InetAddress> pathInv; // caminho volta
    private ConcurrentLinkedQueue<InetAddress> networks; // redes que o pacoete ja passou
    private ConcurrentLinkedQueue<InetAddress> prevNetworks; // redes vizinhas do router de onde veio o pacote
    private ConcurrentLinkedQueue<String> info; // conteudo que o server tem
    private int hops; // numero de saltos

    private int aux; // auxiliar para saber se o pacote veio de um cliente ou de um router

    public Packet(String data){
        this.data = data;
        this.path = new ArrayList<InetAddress>();
        this.pathInv = new ArrayList<InetAddress>();
        this.networks = new ConcurrentLinkedQueue<InetAddress>();
        this.prevNetworks = new ConcurrentLinkedQueue<InetAddress>();
        this.info = new ConcurrentLinkedQueue<String>();
        this.hops = 0;
        this.aux = 0;
    }
    public Packet(ConcurrentLinkedQueue<String> info){
        this.data = null;
        this.path = new ArrayList<InetAddress>();
        this.pathInv = new ArrayList<InetAddress>();
        this.networks = new ConcurrentLinkedQueue<InetAddress>();
        this.prevNetworks = new ConcurrentLinkedQueue<InetAddress>();
        this.info = new ConcurrentLinkedQueue<String>();
        this.info.addAll(info);
        this.hops = 0;
        this.aux = 0;
    }

    public int getAux() {
        return this.aux;
    }

    public ArrayList<InetAddress> getPathInv() {
        return this.pathInv;
    }

    public int getHops() {
        return this.hops;
    }

    public ArrayList<InetAddress> getPath() { // caminho ida
        return this.path;
    }

    public ConcurrentLinkedQueue<InetAddress> getNetworks() { // redes que o pacoete ja passou
        return this.networks;
    }

    public ConcurrentLinkedQueue<InetAddress> getPrevNetworks() { // redes vizinhas do router de onde veio o pacote
        return this.prevNetworks;
    }

    public ConcurrentLinkedQueue<String> getInfo() { // conteudo que o server tem 
        return this.info;
    }

    public String getData(){
        return data;
    }

    public void setAux(int aux) {
        this.aux = aux;
    }

    public void setPathInv(InetAddress ip) { // caminho volta
        this.pathInv.add(ip);
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

    public void setInfo(String info){
        this.info.add(info);
    }


    public void setData(String data){
        this.data = data;
    }

    public String toString(){
        return "Data: " + data + " Path: " + path.toString() + " Networks: " + networks.toString() + " Hops: " + hops + " PathInv: " + pathInv.toString() + " PrevNetworks: " + prevNetworks.toString() + " Info: " + info.toString();
    }

}