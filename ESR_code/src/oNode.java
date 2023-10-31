import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class oNode {

    public ConcurrentHashMap<IpWithMask, Boolean> activeRouters;

    public static String name;

    public static void main(String[] args) {
        name = args[0];
        oNode o = new oNode();

        Thread thread1 = new Thread(() -> {
            o.hello();
        });

        Thread thread2 = new Thread(() -> {
            o.ping();
        });

        Thread thread3 = new Thread(() -> {
            o.pong();
        });

        // Inicia as threads
        thread1.start();
        thread2.start();
        thread3.start();
    }   

    private void parseConfigFile(String name) {

        try {

            //ipVizinhos = new ConcurrentLinkedQueue<IpWithMask>();
            activeRouters = new ConcurrentHashMap<IpWithMask, Boolean>();
            
            // System.out.println(System.getProperty("user.dir"));
            // System.out.println("p"+name +".txt");
            FileReader f = new FileReader(name + ".txt");
            BufferedReader buffer = new BufferedReader(f);

            String ip;
            while ((ip = buffer.readLine()) != null) {
                activeRouters.put(new IpWithMask(ip), false);
            }

            f.close();

            // debug
            System.out.println("Ip's na lista: " + activeRouters.toString());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Erro ao ler arquivo de configuracao");
        }

    }

    public oNode() {
        this.parseConfigFile(name);
    }


    private void hello() {

        try {

            DatagramSocket serverSocket = new DatagramSocket(9000);
            while (true) {
                byte[] receiveData = new byte[1024];
                byte[] sendData = new byte[1024];
    
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                
                byte[] data = receivePacket.getData();
                
    
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bais);

                try {
                    Object readObject = ois.readObject();
                    if (readObject instanceof Packet) {
                        Packet p = (Packet) readObject;
                        String str = p.getData();
                        InetAddress IPAddress = InetAddress.getByName(receivePacket.getAddress().toString().replace("/", ""));
                        p.setPath(IPAddress);
    
                        for (InetAddress address : p.getPrevNetworks()) {     
                            if (!p.getNetworks().contains(address)) {
                                p.getNetworks().add(address);
                            }
                        }
                        p.setPrevNetworksZero();
                        
                        for (ConcurrentHashMap.Entry<IpWithMask, Boolean> entry : activeRouters.entrySet()) {
                            IpWithMask key = entry.getKey();
                            Boolean value = entry.getValue();
                            System.out.println("Clave: " + key + ", Valor: " + value);
                        }
    
                        System.out.println("RECEIVED: " + str + " from " + receivePacket.getAddress() + ":" + 9000);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(p);
                        oos.close();
                        byte[] datak = baos.toByteArray();
    
                        System.out.println("====================================");
                        for (InetAddress ip : p.getNetworks()) {
                            System.out.println("->" + ip.getHostAddress());
                        }
                        System.out.println("====================================");
    
                        //for (IpWithMask ip : activeRouters.) {
                        for (ConcurrentHashMap.Entry<IpWithMask, Boolean> entry : activeRouters.entrySet()) {
                            //IpWithMask key = entry.getKey();
                            //Boolean value = entry.getValue();
                            if(entry.getValue()){
                                InetAddress packetNetwork = entry.getKey().getNetwork();
                                boolean sent = false; // Variável para verificar se o pacote foi enviado
                                //System.out.print("IP: " + packetNetwork.getHostAddress() + " -> " );
                                for (InetAddress ip_network : p.getNetworks()) {
                                    //System.out.println("IP_NETWORK: " + ip_network);s
                                    if (ip_network.getHostAddress().equals(packetNetwork.getHostAddress())){
                                        sent = true;
                                        break;
                                    }
                                }
                            
                                if (!sent) {
                                    DatagramPacket sendPacket = new DatagramPacket(datak, datak.length, entry.getKey().getAddress(), 9000);
                                    serverSocket.send(sendPacket);
                                    System.out.println("SENT: " + str + " to " + entry.getKey().getAddress() + ":" + 9000);
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

    private void ping(){
        
        try {
            DatagramSocket pingSocket = new DatagramSocket(8000);
            pingSocket.setSoTimeout(1000);
            while (true) {
                byte[] receiveData = new byte[1024];
                byte[] sendData = new byte[1024];
                byte[] data = new byte[1024];

                data = "PING".getBytes();

                for (ConcurrentHashMap.Entry<IpWithMask, Boolean> entry : activeRouters.entrySet()) {
                    
                    try {

                        Thread.sleep(1000);
                        DatagramPacket sendPacket = new DatagramPacket(data, data.length, entry.getKey().getAddress(), 8500);
                        pingSocket.send(sendPacket);
                        System.out.println("PING enviado para: " + entry.getKey().getAddress().getHostName());
                        
                        
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        pingSocket.receive(receivePacket);
                        
                        
                        // Caiu no timeout
                        // if (receivePacket.getData() == null) {
                        //     System.out.println("------TIMEOUT : " + entry.getKey().getAddress().getHostName() + " -----");
                        //     entry.setValue(false);
                        //     System.out.println("Tabela ardes -> " + activeRouters.toString());
                        //     continue;
                        // }
                
                        InetAddress addr = receivePacket.getAddress();                   

                        if (addr.getHostName().equals(entry.getKey().getAddress().getHostName())) {
                            System.out.println("PONG recebido de: " + entry.getKey().getAddress().getHostName());
                            entry.setValue(true);
                            System.out.println("Tabela recebeu PONG:" + activeRouters.toString());
                        }
                    } catch (SocketTimeoutException e3) {
                        System.out.println("------TIMEOUT : " + entry.getKey().getAddress().getHostName() + " -----");
                        entry.setValue(false);
                        System.out.println("Tabela ardes -> " + activeRouters.toString());
                        continue;                
                    } catch (InterruptedException e4) {
                        e4.printStackTrace();
                    }
                }
                
            }

        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        
    }

    private void pong(){
        
        try {
            DatagramSocket pongSocket = new DatagramSocket(8500);

            while (true) {
                byte[] receiveData = new byte[1024];
                byte[] sendData = new byte[1024];
                
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                pongSocket.receive(receivePacket);
                System.out.println("PING recebido! ->" + receivePacket.getAddress().getHostName());
            
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject("pong");
                oos.close();
                sendData = baos.toByteArray();

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), 8000);
                pongSocket.send(sendPacket);
                System.out.println("PONG enviado! ->" + receivePacket.getAddress().getHostName());
            }
            

        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        
    }
}