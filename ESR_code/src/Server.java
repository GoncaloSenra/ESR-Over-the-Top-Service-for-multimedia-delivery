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

    private ConcurrentHashMap<String, Boolean> Rois;// DEBUG:

    private Boolean retry;

    //private static Semaphore semaphore = new Semaphore(1); // Inicializa o semáforo com uma permissão

    
    public static void main(String args[]) throws Exception {

        Server s = new Server(args[0]);


        // Itera pelos argumentos e os adiciona à lista
        for (int i = 1; i < args.length; i++) {
            System.out.println();
            s.Rois.put(args[i], true);//DEBUG: SO PARA TESTAR, SENAO TEM QUE ESTAR A FALSE
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

        // thread1.start();
        // thread2.start();
        // thread3.start();
        // thread4.start();

        //TODO: ver como fazer para ligar e deligar a stream de video e como fazer para mais que um video.
        //talvez um array de threads ou trocar o rois de em vez de boolean o id da thread que ligou o
        // server de video para depois conseguir parar a thread mais tarde 
        for(ConcurrentHashMap.Entry<String, Boolean> entry : s.getRois().entrySet()){
            if(entry.getValue()){
                System.out.println("A enviar video para o RP " + entry.getKey());
                File f = new File(entry.getKey());
                if (f.exists()) {
                    // Create a Main object
                    ServerRTP sRtp = new ServerRTP(entry.getKey(),s.ip_rp.getAddress(),0,"movie");
                    // show GUI: (opcional!)
                    // s.pack();
                    // s.setVisible(true);
                } else
                    System.out.println("Ficheiro de video não existe: " + entry.getKey());
            }
        }

    }

    public Server(String ip) throws Exception {

        this.Rois = new ConcurrentHashMap<String, Boolean>();
        this.retry = true;
        this.ip_rp = new IpWithMask(ip);

        
        System.out.println("-----------------");
    }

    public ConcurrentHashMap<String, Boolean> getRois() {
        return Rois;
    }

    public void setRois(String key, Boolean value) {
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

            for (ConcurrentHashMap.Entry<String, Boolean> entry : Rois.entrySet()) {
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
                    for (ConcurrentHashMap.Entry<String, Boolean> entry : Rois.entrySet()) {
                        entry.setValue(false);
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

    public void sendVideo() {

        try {
            DatagramSocket videoSocket = new DatagramSocket();

            while (true) {

                Thread.sleep(4000);

                for (ConcurrentHashMap.Entry<String, Boolean> entry : Rois.entrySet()) {
                    if (entry.getValue()) {
                        Packet p = new Packet(entry.getKey());

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(p);
                        oos.close();

                        byte[] data = baos.toByteArray();

                        DatagramPacket sendPacket = new DatagramPacket(data, data.length, ip_rp.getAddress(), 9000);
                        videoSocket.send(sendPacket);

                        System.out.println("SENT: " + entry.getKey() + " to RP: " + ip_rp.getAddress() + ":" + 9000);
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