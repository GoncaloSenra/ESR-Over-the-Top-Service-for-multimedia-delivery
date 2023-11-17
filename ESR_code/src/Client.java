import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;



class Client {

    private static String routerIP;

    private static String videoName;

    private IpWithMask ip_router;

    private DatagramSocket clientSocket; //9000

    // RTP variables:
    // ----------------
    DatagramPacket rcvdp; // UDP packet received from the server (to receive)
    DatagramSocket RTPsocket; // socket to be used to send and receive UDP packet

    Timer cTimer; // timer used to receive data from the UDP socket
    byte[] cBuf; // buffer used to store data received from the server

    
    public static void main(String[] args) throws Exception {

        routerIP = args[0];
        videoName = args[1];
        //System.out.println("Router IP: " + routerIP);
        Client c = new Client();
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        
        System.out.println();

        Thread thread1 = new Thread(() -> {
            try {
                System.out.println("Thread 1");
                while (true) {
                    byte[] receiveData = new byte[8192];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    c.clientSocket.receive(receivePacket);

                    byte[] data = receivePacket.getData();
                    
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    ObjectInputStream ois = new ObjectInputStream(bais);

                    Object readObject = ois.readObject();
                    if (readObject instanceof Packet) {
                        Packet pdi = (Packet) readObject;
                        String str = pdi.getData();

                        System.out.println(str);
                        
                        System.out.println("=====================");
                    }
                    
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                c.bestPath();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        Thread thread3 = new Thread(() -> {
            try {
                c.pongRouter();;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        //thread1.start();
        //thread2.start();
        //thread3.start();

    }

    public Client() throws Exception {
        
        this.ip_router = new IpWithMask(routerIP);

        this.clientSocket = new DatagramSocket(9000); 

        // init para a parte do cliente
        // --------------------------
        cTimer = new Timer(20, new clientTimerListener());
        cTimer.setInitialDelay(0);
        cTimer.setCoalesce(true);
        cBuf = new byte[15000]; // allocate enough memory for the buffer used to receive data from the server

        // try {
        //     // socket e video
        //     clientSocket.setSoTimeout(5000); // setimeout to 5s
        // } catch (SocketException e) {
        //     System.out.println("Cliente: erro no socket: " + e.getMessage());
        // }
        
    }

    public void bestPath(){ //vai procurar o melhor caminho para o destino
        //1 envia pacote search(6000)
        //2 espera pelos pacotes a receber(6001)
        //3 escolhe o melhor caminho
        //4 envia o pacote para o melhor caminho sendo esse pacote noutra porta (7000)
        
        //variaveis
        ConcurrentLinkedQueue<Packet> pacotes = new ConcurrentLinkedQueue<Packet>();
        
        try {
            DatagramSocket searchSocket = new DatagramSocket(6001);
            //1
            searchSocket.setSoTimeout(2500);
            Packet p = new Packet(videoName);
            p.setAux(1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(p);
            oos.close();
            byte[] data = baos.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, ip_router.getAddress(), 6000);
            searchSocket.send(sendPacket);
            System.out.println("SENT: " + videoName + " to " + ip_router.getAddress() + ":" + 6000);

            try {
                //2
                while (true) {
                    byte[] receiveData = new byte[8192];
        
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    searchSocket.receive(receivePacket);
                    
                    byte[] dataR = receivePacket.getData();
                    
        
                    ByteArrayInputStream bais = new ByteArrayInputStream(dataR);
                    ObjectInputStream ois = new ObjectInputStream(bais);

                    Object readObject = ois.readObject();
                    if (readObject instanceof Packet) {
                        Packet pdi = (Packet) readObject;
                        //String str = pdi.getData();
                        InetAddress IPAddress = InetAddress.getByName(receivePacket.getAddress().toString().replace("/", ""));
                        pdi.setPathInv(IPAddress);
                        pdi.setHops(pdi.getHops() + 1);
                        pacotes.add(pdi);
                        //System.out.println("RECEIVED: " + str + " from " + receivePacket.getAddress() + ":" + 6001 + " -> " + pdi.getPathInv().toString() + " -> " + pdi.getHops() + pdi.getPath().toString());
                        
                        
                        System.out.println("====================================");
                    }
                }
            } catch (SocketTimeoutException e3) {
                System.out.println("TIMEOUT");               
            } catch (ClassNotFoundException e3) {
                e3.printStackTrace();
            }

            System.out.println("====================================");
            for (Packet packet : pacotes) {
                System.out.println(packet.toString());
                System.out.println(packet.getPath().toString());
            }
            System.out.println("====================================");
            
            //3
            //escolher o pacote com menos saltos e com menor latencia (ordem da lista)
            Packet bestPacket = pacotes.peek();
            for (Packet packet : pacotes) {
                if(packet.getHops() < bestPacket.getHops()){
                    bestPacket = packet;
                }
            }
            System.out.println("====================================");
            
            //4
            //enviar o pacote para o melhor caminho
            DatagramSocket sendSocket = new DatagramSocket();

            InetAddress dest2 = bestPacket.getPathInv().getLast();
            bestPacket.setHops(1);
            bestPacket.setData(videoName);

            ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
            ObjectOutputStream oos2 = new ObjectOutputStream(baos2);
            oos2.writeObject(bestPacket);
            oos2.close();
            byte[] datak = baos2.toByteArray();
            DatagramPacket sendPacket2 = new DatagramPacket(datak, datak.length, dest2, 7000);//envia para tras na porta 6001
            sendSocket.send(sendPacket2);
            System.out.println("SENT to pc -> to: " + dest2.getHostAddress() + ":" + 7000);
            
        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }


    public void pongRouter() {

        try {
            DatagramSocket pingSocket = new DatagramSocket(2001);
            while(true){
                //TODO: se demorar mais de x a receber o pong,tem que avisar o utilizador que o router? foi down e que tem que se ligar de novo
                
                byte[] receiveData = new byte[8192];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                pingSocket.receive(receivePacket);
                byte[] data = new byte[8192];
                
                data = "PONG".getBytes();
    
                DatagramPacket sendPacket = new DatagramPacket(data, data.length, receivePacket.getAddress() ,2000);
                pingSocket.send(sendPacket);
    
                
            }
            } catch (SocketException e1) {
                    e1.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            }

    }

    class clientTimerListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            // Construct a DatagramPacket to receive data from the UDP socket
            rcvdp = new DatagramPacket(cBuf, cBuf.length);

            try {
                // receive the DP from the socket:
                RTPsocket.receive(rcvdp);

                // create an RTPpacket object from the DP
                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());

                // print important header fields of the RTP packet received:
                System.out.println("Got RTP packet with SeqNum # " + rtp_packet.getsequencenumber() + " TimeStamp "
                        + rtp_packet.gettimestamp() + " ms, of type " + rtp_packet.getpayloadtype());

                // print header bitstream:
                rtp_packet.printheader();

                // get the payload bitstream from the RTPpacket object
                int payload_length = rtp_packet.getpayload_length();
                byte[] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                // get an Image object from the payload bitstream
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Image image = toolkit.createImage(payload, 0, payload_length);

                // display the image as an ImageIcon object
                //icon = new ImageIcon(image);
                //iconLabel.setIcon(icon);
            } catch (InterruptedIOException iioe) {
                System.out.println("Nothing to read");
            } catch (IOException ioe) {
                System.out.println("Exception caught: " + ioe);
            }
        }
    }

}
