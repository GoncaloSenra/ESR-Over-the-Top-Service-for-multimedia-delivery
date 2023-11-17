import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import javax.swing.*;
import javax.swing.Timer;

import java.io.*;
import java.awt.*;
import java.util.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;



class Server extends JFrame implements ActionListener {

    private DatagramSocket serverSocket;

    private static String RP_IP;

    private IpWithMask ip_rp;

    private String path;//futuramente vai ser o path do video a streamar

    private ConcurrentHashMap<String,Boolean> Rois;//DEBUG:

    private Boolean retry;

    private Semaphore semaphore ; // Inicializa o semáforo com uma permissão


    //static String VideoFileName; // video para enviar

    //GUI:
    //----------------
    JLabel label;

    // RTP variables:
    // ----------------
    DatagramPacket senddp; // UDP packet containing the video frames (to send)A
    DatagramSocket RTPsocket; // socket to be used to send and receive UDP packet

    // Video constants:
    // ------------------
    int imagenb = 0; // image nb of the image currently transmitted
    VideoStream video; // VideoStream object used to access video frames
    static int MJPEG_TYPE = 26; // RTP payload type for MJPEG video
    static int FRAME_PERIOD = 100; // Frame period of the video to stream, in ms
    static int VIDEO_LENGTH = 500; // length of the video in frames

    Timer sTimer; // timer used to send the images at the video frame rate
    byte[] sBuf; // buffer used to store the images to send to the client



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

        for(ConcurrentHashMap.Entry<String, Boolean> entry : s.Rois.entrySet()){
            s.video = new VideoStream(entry.getKey()); // init the VideoStream object:
            System.out.println("Servidor: vai enviar video da file " + entry.getKey());
            break; //BUG: só envia o primeiro video
        }

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

        //thread1.start();
        //thread2.start();
        //thread3.start();
        //thread4.start();
    
    }

    public Server() throws Exception {
        
        super("Servidor");
        
        // init para a parte do servidor
        sTimer = new Timer(FRAME_PERIOD, this); // init Timer para servidor
        sTimer.setInitialDelay(0);
        sTimer.setCoalesce(true);
        sBuf = new byte[15000]; // allocate memory for the sending buffer
        

        // Handler to close the main window
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                // stop the timer and exit
                sTimer.stop();
                System.exit(0);
            }
        });

        // GUI:
        label = new JLabel("Send frame #        ", JLabel.CENTER);
        getContentPane().add(label, BorderLayout.CENTER);

        sTimer.start();

        try {
            this.ip_rp = new IpWithMask(RP_IP);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //this.serverSocket = new DatagramSocket(10000); 

        this.Rois = new ConcurrentHashMap<String,Boolean>();
        this.retry = true;
        System.out.println("-----------------");
        semaphore = new Semaphore(1);
    }

    
    public void connect_RP() {
        
        try {
            DatagramSocket connSocket = new DatagramSocket(5004);

           // while (true) {
                //if (retry) {
                    retry = false;
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
                    //semaphore.release();
                //}
                //Thread.sleep(2000);
                
            //}


        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        } /*catch (InterruptedException e3) {
            e3.printStackTrace();
        }*/
        
    }

    
    public void pong(){ 
        try {
            //semaphore.acquire();
            DatagramSocket pingSocket = new DatagramSocket(5001);
            //pingSocket.setSoTimeout(5000);
            
            while(true){
                try {
                    //TODO: se demorar mais de x a receber o pong,tem que avisar o utilizador que o rp foi down e que tem que se ligar de novo
                    
                    byte[] receiveData = new byte[8192];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    pingSocket.receive(receivePacket);
                    byte[] data = new byte[8192];
                    
                    data = "PONG".getBytes();
                
                    DatagramPacket sendPacket = new DatagramPacket(data, data.length, ip_rp.getAddress() ,5000);
                    pingSocket.send(sendPacket);

                        
                        
                } catch (SocketTimeoutException e3) {
                    
                    System.out.println("CAIU");
                    retry = true;
                    for(ConcurrentHashMap.Entry<String, Boolean> entry : Rois.entrySet()){
                        entry.setValue(false);
                    }
                    System.out.println("Rois me a pika " + Rois.toString());
                }
            }
                
        } catch (SocketException e1) {
                e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        } /*catch (InterruptedException ie) {
            System.err.println("Erro ao adquirir o semáforo");
        }*/
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

    // ------------------------
    // Handler for timer
    // ------------------------
    public void actionPerformed(ActionEvent e) {

        // if the current image nb is less than the length of the video
        for(ConcurrentHashMap.Entry<String, Boolean> entry : Rois.entrySet()){
            if (entry.getValue()) {
                
            
                if (imagenb < VIDEO_LENGTH) {
                    // update current imagenb
                    imagenb++;

                    try {
                        // get next frame to send from the video, as well as its size
                        int image_length = video.getnextframe(sBuf);

                        // Builds an RTPpacket object containing the frame
                        RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb * FRAME_PERIOD, sBuf, image_length, entry.getKey());

                        // get to total length of the full rtp packet to send
                        int packet_length = rtp_packet.getlength() ;
                        
                        // retrieve the packet bitstream and store it in an array of bytes
                        byte[] packet_bits = new byte[packet_length];
                        rtp_packet.getpacket(packet_bits);

                        // send the packet as a DatagramPacket over the UDP socket
                        senddp = new DatagramPacket(packet_bits, packet_length, ip_rp.getAddress(), 9000);
                        RTPsocket.send(senddp);

                        System.out.println("Send frame #" + imagenb);
                        // print the header bitstream
                        rtp_packet.printheader();

                        // update GUI
                        // label.setText("Send frame #" + imagenb);
                    } catch (Exception ex) {
                        System.out.println("Exception caught: " + ex);
                        System.exit(0);
                    }
                } else {
                    // ATENÇAO AO COPYRIGHT
                    
                    imagenb = 0; //image nb of the image currently transmitted
                    MJPEG_TYPE = 26; //RTP payload type for MJPEG video
                    FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
                    VIDEO_LENGTH = 500; //length of the video in frames

                    //if we have reached the end of the video file, stop the timer
                    System.out.println("Final da stream");
                    sTimer.setInitialDelay(0);
                    sTimer.setCoalesce(true);
                    sBuf = new byte[15000];

                    try {
                        video = new VideoStream(entry.getKey()); //init the VideoStream object:

                    } catch (Exception exc) {
                        System.out.println("Erro video");
                    }
                }
            }
        }
    }
}