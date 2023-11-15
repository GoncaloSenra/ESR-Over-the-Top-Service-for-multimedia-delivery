import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;

class Server {

    private DatagramSocket serverSocket;

    private IpWithMask ip_router;

    private static String RP_IP;

    private IpWithMask ip_rp;

    private String path;//futuramente vai ser o path do video a streamar

    private ConcurrentLinkedQueue<String> Rois;//DEBUG:

    public static void main(String args[]) throws Exception {
        RP_IP = args[0]; 

        Server s = new Server();

        // Itera pelos argumentos e os adiciona à lista
        for (int i = 1; i < args.length; i++) {
                s.Rois.add(args[i]);
        }

        // Exibe os argumentos armazenados na lista
        System.out.println("Argumentos fornecidos:");
        for (String argumento : s.Rois) {
            System.out.println(argumento);
        }
    

        Thread thread1 = new Thread(() -> {
            s.connect_RP();
        });

        Thread thread2 = new Thread(() -> {
            s.pong();
        });

        thread1.start();
        thread2.start();
    
    }

    void Server() throws Exception  {
        this.ip_rp = new IpWithMask(RP_IP);

        this.serverSocket = new DatagramSocket(10000); 
        Rois = new ConcurrentLinkedQueue<String>();

    }

    
    public void connect_RP() {
        
        try {
            DatagramSocket connSocket = new DatagramSocket();
            Packet p = new Packet(Rois);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(p);
            oos.close();
            byte[] data = baos.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, ip_router.getAddress(), 5500);
            connSocket.send(sendPacket);
            System.out.println("SENT: " + "search" + " to " + ip_router.getAddress() + ":" + 5500);

        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        
    }

    
    public void pong(){ 
        try {
            DatagramSocket pingSocket = new DatagramSocket(5001);
            byte[] receiveData = new byte[8192];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            pingSocket.receive(receivePacket);
            byte[] data = new byte[8192];
            
            data = "PONG".getBytes();

            DatagramPacket sendPacket = new DatagramPacket(data, data.length, ip_rp.getAddress() ,5000);
            pingSocket.send(sendPacket);
            
        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }
}