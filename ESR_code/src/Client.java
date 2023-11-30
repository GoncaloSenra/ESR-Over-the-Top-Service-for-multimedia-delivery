import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

class Client {

    private static String routerIP;

    private static String videoName;

    private IpWithMask ip_router;

    JFrame f = new JFrame("Cliente de Testes");
    JButton tearButton = new JButton("Teardown");
    JPanel mainPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JLabel iconLabel = new JLabel();
    ImageIcon icon;

    // RTP variables:
    // ----------------
    DatagramPacket rcvdp; // UDP packet received from the server (to receive)
    DatagramSocket RTPsocket; // socket to be used to send and receive UDP packet

    Timer cTimer; // timer used to receive data from the UDP socket
    byte[] cBuf; // buffer used to store data received from the server

    DatagramSocket searchSocket;
    DatagramSocket cancelVideo;
    DatagramSocket pingSocket;

    public static void main(String[] args) throws Exception {

        routerIP = args[0];
        videoName = args[1];
        // System.out.println("Router IP: " + routerIP);
        Client c = new Client();

        Thread thread1 = new Thread(() -> {
            Boolean connected = true;
            while (connected) {
                connected = c.bestPath();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
            
            try {
                c.pingSocket.setSoTimeout(15000);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                c.pongRouter();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread thread3 = new Thread(() -> {
            try {
                c.cancelStream();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        thread1.start();
        thread2.start();
        thread3.start();

        System.out
                .println("Client: " + c.ip_router.getAddress().toString() + " " + c.ip_router.getNetwork().toString());

    }

    public Client() throws Exception {

        this.ip_router = new IpWithMask(routerIP);

        try {
            // socket e video
            this.RTPsocket = new DatagramSocket(9000); // init RTP socket (o mesmo para o cliente e servidor)

            this.pingSocket = new DatagramSocket(2001);

            this.searchSocket = new DatagramSocket(6001);
            this.searchSocket.setSoTimeout(2500);

            this.cancelVideo = new DatagramSocket(6500);

            // System.out.println("Cliente: vai receber video");
        } catch (SocketException e) {
            System.out.println("Cliente: erro no socket: " + e.getMessage());
        }

        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        // Buttons
        buttonPanel.setLayout(new GridLayout(1, 0));
        buttonPanel.add(tearButton);

        // handlers... (so um)
        tearButton.addActionListener(new tearButtonListener());

        // Image display label
        iconLabel.setIcon(null);

        // frame layout
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        iconLabel.setBounds(0, 0, 380, 280);
        buttonPanel.setBounds(0, 280, 380, 50);

        f.getContentPane().add(mainPanel, BorderLayout.CENTER);
        f.setSize(new Dimension(390, 370));
        f.setVisible(true);

        // init para a parte do cliente
        // --------------------------
        cTimer = new Timer(20, new clientTimerListener());
        cTimer.setInitialDelay(0);
        cTimer.setCoalesce(true);
        cBuf = new byte[15000]; // allocate enough memory for the buffer used to receive data from the server

        // start the timers ...
        cTimer.start();

    }

    public Boolean bestPath() { // vai procurar o melhor caminho para o destino
        // 1 envia pacote search(6000)
        // 2 espera pelos pacotes a receber(6001)
        // 3 escolhe o melhor caminho
        // 4 envia o pacote para o melhor caminho sendo esse pacote noutra porta (7000)

        // variaveis
        ConcurrentLinkedQueue<Packet> pacotes = new ConcurrentLinkedQueue<Packet>();

        try {

            // 1
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
                // 2
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
                        // String str = pdi.getData();
                        InetAddress IPAddress = InetAddress
                                .getByName(receivePacket.getAddress().toString().replace("/", ""));
                        pdi.setPathInv(IPAddress);
                        pdi.setHops(pdi.getHops() + 1);
                        pacotes.add(pdi);
                        // System.out.println("RECEIVED: " + str + " from " + receivePacket.getAddress()
                        // + ":" + 6001 + " -> " + pdi.getPathInv().toString() + " -> " + pdi.getHops()
                        // + pdi.getPath().toString());

                        // System.out.println("====================================");
                    }
                }
            } catch (SocketTimeoutException e3) {
                System.out.println("TIMEOUT\n");
            } catch (ClassNotFoundException e3) {
                e3.printStackTrace();
            }

            System.out.println("===Caminhos possiveis===");
            for (Packet packet : pacotes) {
                // System.out.println(packet.toString());
                System.out.println(packet.getPathInv().toString());
            }
            // System.out.println("====================================");

            // 3
            // escolher o pacote com menos saltos e com menor latencia (ordem da lista)
            Packet bestPacket = pacotes.peek();
            for (Packet packet : pacotes) {
                if (packet.getHops() < bestPacket.getHops()) {
                    bestPacket = packet;
                }
            }
            // System.out.println("====================================");
            

            // 4
            // enviar o pacote para o melhor caminho
            if (pacotes.size() == 0) {
                return true;
            } else {
                System.out.println("===Caminhos escolhido===");
                System.out.println(bestPacket.getPathInv().toString());

                InetAddress dest2 = bestPacket.getPathInv().get(bestPacket.getPathInv().size() - 1);
                bestPacket.setHops(1);
                bestPacket.setData(videoName);

                ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                ObjectOutputStream oos2 = new ObjectOutputStream(baos2);
                oos2.writeObject(bestPacket);
                oos2.close();
                byte[] datak = baos2.toByteArray();
                DatagramPacket sendPacket2 = new DatagramPacket(datak, datak.length, dest2, 7000);
                                                                                                  
                searchSocket.send(sendPacket2);
                System.out.println("SENT to pc -> to: " + dest2.getHostAddress() + ":" + 7000);
                return false;
            }

        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        return true;
    }

    public void pongRouter() {
   
        while (true) {

            try {

                byte[] receiveData = new byte[8192];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                pingSocket.receive(receivePacket);
                byte[] data = new byte[8192];

                data = "PONG".getBytes();

                DatagramPacket sendPacket = new DatagramPacket(data, data.length, receivePacket.getAddress(), 2000);
                pingSocket.send(sendPacket);
            }catch (SocketTimeoutException e3) {
                System.out.println("[Feedback] Router folha down");
                Boolean connected = true;
                while (connected) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    connected = bestPath();
                }
            } catch (Exception e) {
                continue;
            }

        }


    }

    public void cancelStream() {
        try {
            while (true) {
                byte[] receiveData = new byte[8192];
                
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                cancelVideo.receive(receivePacket);
                //System.out.println("PING recebido! ->" + receivePacket.getAddress().getHostName());
                Boolean connected = true;
                while (connected) {
                    connected = bestPath();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }       
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Handler for tear button
    // -----------------------
    class tearButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            System.out.println("[Feedback] Teardown Button pressed !");
            // stop the timer
            cTimer.stop();
            // exit
            System.exit(0);
        }
    }

    // ------------------------------------
    // Handler for timer (para cliente)
    // ------------------------------------

    class clientTimerListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            // Construct a DatagramPacket to receive data from the UDP socket
            rcvdp = new DatagramPacket(cBuf, cBuf.length);

            try {
                // receive the DP from the socket:
                // System.out.println("Client: vai receber video");
                RTPsocket.receive(rcvdp);

                // create an RTPpacket object from the DP
                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
                if (rtp_packet.getsequencenumber() == 1) {
                    System.out.println("[Feedback] Vai receber o video" + rtp_packet.getVideoName() + " de" + routerIP);
                }

                // print important header fields of the RTP packet received:
                // System.out.println("Got RTP packet with SeqNum # " + rtp_packet.getsequencenumber() + " TimeStamp "
                //         + rtp_packet.gettimestamp() + " ms, of type " + rtp_packet.getpayloadtype());

                // print header bitstream:
                // rtp_packet.printheader();
                // System.out.println("videoName-> " + rtp_packet.getVideoName());

                // get the payload bitstream from the RTPpacket object
                int payload_length = rtp_packet.getpayload_length();
                byte[] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                // get an Image object from the payload bitstream
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Image image = toolkit.createImage(payload, 0, payload_length);

                // display the image as an ImageIcon object
                icon = new ImageIcon(image);
                iconLabel.setIcon(icon);

            } catch (InterruptedIOException iioe) {
                System.out.println("Nothing to read");
            } catch (IOException ioe) {
                System.out.println("Exception caught: " + ioe);
            }
        }
    }
}
