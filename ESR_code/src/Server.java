import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

class Server  {

    //private DatagramSocket serverSocket;

    private IpWithMask ip_rp;

    //private String path;// futuramente vai ser o path do video a streamar

    private ConcurrentHashMap<String, ServerRTP> Rois;// DEBUG:

    private Boolean retry;

    //private static Semaphore semaphore = new Semaphore(1); // Inicializa o semáforo com uma permissão

    
    public static void main(String args[]) throws Exception {

        Server s = new Server(args[0]);


        // Itera pelos argumentos e os adiciona à lista
        for (int i = 1; i < args.length; i++) {
            //System.out.println();
            s.Rois.put(args[i], null);
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
            s.checkVideo();
        });


        thread1.start();
        thread2.start();
        thread3.start();
    
    }

    public Server(String ip) throws Exception {

        this.Rois = new ConcurrentHashMap<>();
        this.retry = true;
        this.ip_rp = new IpWithMask(ip);

        
        System.out.println("-----------------");
    }

    public ConcurrentHashMap<String, ServerRTP> getRois() {
        return Rois;
    }

    public void setRois(String key, ServerRTP value) {
        Rois.put(key, value);
    }

    public Boolean getRetry() {
        return retry;
    }

    public void setRetry(Boolean retry) {
        this.retry = retry;
    }



    public void connect_RP() {

        try {
            DatagramSocket connSocket = new DatagramSocket(5004);

            // while (true) {
            // if (retry) {
            retry = false;
            ConcurrentLinkedQueue<String> lista = new ConcurrentLinkedQueue<String>();

            for (ConcurrentHashMap.Entry<String, ServerRTP> entry : Rois.entrySet()) {
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
            // semaphore.release();
            // }
            // Thread.sleep(2000);

            // }

        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        } /*
           * catch (InterruptedException e3) {
           * e3.printStackTrace();
           * }
           */

    }

    public void pong() {
        try {
            // semaphore.acquire();
            DatagramSocket pingSocket = new DatagramSocket(5001);
            // pingSocket.setSoTimeout(5000);

            while (true) {
                try {
                    // TODO: se demorar mais de x a receber o pong,tem que avisar o utilizador que o
                    // rp foi down e que tem que se ligar de novo

                    byte[] receiveData = new byte[8192];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    pingSocket.receive(receivePacket);
                    byte[] data = new byte[8192];

                    data = "PONG".getBytes();

                    DatagramPacket sendPacket = new DatagramPacket(data, data.length, ip_rp.getAddress(), 5000);
                    pingSocket.send(sendPacket);

                } catch (SocketTimeoutException e3) {

                    System.out.println("CAIU");
                    retry = true;
                    for (ConcurrentHashMap.Entry<String, ServerRTP> entry : Rois.entrySet()) {
                        if (!(entry.getValue() == null)) {
                            System.err.println("A parar o video " + entry.getKey());
                            entry.getValue().stopThread();
                            entry.setValue(null);

                        }
                    }
                    System.out.println("Rois me a pika " + Rois.toString());
                }
            }

        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        } /*
           * catch (InterruptedException ie) {
           * System.err.println("Erro ao adquirir o semáforo");
           * }
           */
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

                        if (p.getAux() == 1){//inicio do video

                            for(ConcurrentHashMap.Entry<String, ServerRTP> entry : Rois.entrySet()){
                                if(entry.getKey().trim().equals(p.getData().trim())){
                                        System.out.println("A enviar video para o RP " + entry.getKey());
                                        File f = new File(entry.getKey());
                                        if (f.exists()) {
                                            
                                            Rois.put(entry.getKey(), new ServerRTP(entry.getKey(), ip_rp.getAddress(), 0, entry.getKey()));//TODO: image_nb tem que vir no pacote do RP

                                        } else
                                            System.out.println("Ficheiro de video não existe: " + entry.getKey());
                                    }
                            }
                        }
                        else{//cancelar de enviar o video
                            for(ConcurrentHashMap.Entry<String, ServerRTP> entry : Rois.entrySet()){
                                if(entry.getKey().trim().equals(p.getData().trim())){
                                    if(!(entry.getValue() == null)){
                                        System.out.println("A parar o video " + entry.getKey());

                                        entry.getValue().stopThread();
                                        Rois.put(entry.getKey(), null);
                                    }
                                }
                            }
                        }
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

}