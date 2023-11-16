import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


class Server {

    private DatagramSocket serverSocket;

    private static String RP_IP;

    private IpWithMask ip_rp;

    private String path;//futuramente vai ser o path do video a streamar

    private ConcurrentHashMap<String,Boolean> Rois;//DEBUG:

    public static void main(String args[]) throws Exception {
        RP_IP = args[0]; 

        Server s = new Server();

        // Itera pelos argumentos e os adiciona à lista
        for (int i = 1; i < args.length; i++) {
            System.out.println();
            s.Rois.put(args[i],false);
        }

        // Exibe os argumentos armazenados na lista
        System.out.println("Argumentos fornecidos:");
        System.out.println(s.Rois.toString());
    

        Thread thread1 = new Thread(() -> {
            s.connect_RP();
        });

        Thread thread2 = new Thread(() -> {
            s.pong();
        });

        Thread thread3 = new Thread(() -> {
            s.sendVideo();
        });
        
        Thread thread4 = new Thread(() -> {
            s.checkVideo();
        });

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
    
    }

    public Server() {
        try {
            this.ip_rp = new IpWithMask(RP_IP);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //this.serverSocket = new DatagramSocket(10000); 

        this.Rois = new ConcurrentHashMap<String,Boolean>();
        System.out.println("-----------------");
    }

    
    public void connect_RP() {
        
        try {
            DatagramSocket connSocket = new DatagramSocket(5004);
            ConcurrentLinkedQueue<String> lista = new ConcurrentLinkedQueue<String>();
            
            for(ConcurrentHashMap.Entry<String, Boolean> entry : Rois.entrySet()){
                lista.add(entry.getKey());
            }
            Packet p = new Packet(lista);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(p);
            oos.close();
            byte[] data = baos.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, ip_rp.getAddress(), 5003);
            connSocket.send(sendPacket);
            System.out.println("SENT: " + "search" + " to " + ip_rp.getAddress() + ":" + 5003);

        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        
    }

    
    public void pong(){ 
        try {
        DatagramSocket pingSocket = new DatagramSocket(5001);
        while(true){
            //TODO: se demorar mais de x a receber o pong,tem que avisar o utilizador que o rp foi down e que tem que se ligar de novo
            
            byte[] receiveData = new byte[8192];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            pingSocket.receive(receivePacket);
            byte[] data = new byte[8192];
            
            data = "PONG".getBytes();
        
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, ip_rp.getAddress() ,5000);
            pingSocket.send(sendPacket);

            
        }
        } catch (SocketException e1) {
                e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    public void checkVideo() {

        try {
            DatagramSocket checkSocket = new DatagramSocket(9999);

            while (true) {
                
                DatagramPacket receivePacket = new DatagramPacket(new byte[8192], 8192);
                checkSocket.receive(receivePacket);
                
                byte[] data = receivePacket.getData();
                
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bais);

                try {
                    Object readObject = ois.readObject();
                    if (readObject instanceof Packet) {
                        Packet p = (Packet) readObject;
                        
                        if (p.getAux() == 1)
                            Rois.put(p.getData(), true);
                        else
                            Rois.put(p.getData(), false);
                    
                    }
                } catch (ClassNotFoundException e3) {
                    e3.printStackTrace();
                }
            }
            
        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();  
        }
    }


    public void sendVideo(){
 
        try {
            DatagramSocket videoSocket = new DatagramSocket();

            while (true) {

                Thread.sleep(4000);

                for (ConcurrentHashMap.Entry<String, Boolean> entry : Rois.entrySet()){
                    if (entry.getValue()) {
                        byte[] data = entry.getKey().getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(data, data.length, ip_rp.getAddress(), 9000);
                        videoSocket.send(sendPacket);
                    }
                }
            }
            
        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();  
        } catch (InterruptedException e3) {
            e3.printStackTrace();
        }


    }
}