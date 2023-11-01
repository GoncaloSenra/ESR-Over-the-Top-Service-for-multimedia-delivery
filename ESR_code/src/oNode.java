import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class oNode {

    public ConcurrentHashMap<IpWithMask, Boolean> activeRouters;

    public ConcurrentLinkedQueue<InetAddress> bestPath;  

    public static String name;

    private boolean stream;

    private static boolean RP;

    public static void main(String[] args) {
        name = args[0];
        if (args.length == 2) {
            if (args[1].equals("RP")) {
                RP = true;
            }else {
                throw new IllegalArgumentException("Parametros invalidos ");
            }
            
        }

        oNode o = new oNode();

        //abrir sockets
        

        Thread thread1 = new Thread(() -> {
            o.hello();
        });

        Thread thread2 = new Thread(() -> {
            o.ping();
        });

        Thread thread3 = new Thread(() -> {
            o.pong();
        });

        Thread thread4 = new Thread(() -> {
            o.search();
        });

        Thread thread5 = new Thread(() -> {
            o.searchResult();
        });

        // Inicia as threads
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
        thread5.start();
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

        if (RP == true){
            this.stream = true; //TODO: debug
        }else{
            this.stream = false;
        }
    }


    private void hello() {

        try {

            DatagramSocket serverSocket = new DatagramSocket(9000);
            while (true) {
                byte[] receiveData = new byte[8192];
                byte[] sendData = new byte[8192];
    
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
                            p.setPrevNetworks(entry.getKey().getNetwork());
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
                                    //System.out.println("IP_NETWORK: " + ip_network);
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

    private void search() {

        try {

            DatagramSocket searchSocket = new DatagramSocket(6000);
            while (true) {
                byte[] receiveData = new byte[8192];
    
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                searchSocket.receive(receivePacket);
                
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
                            p.setPrevNetworks(entry.getKey().getNetwork());
                        }
                        System.out.println("RECEIVED: " + str + " from " + receivePacket.getAddress() + ":" + 6000);
                        
                        
                        
                        System.out.println("====================================");

                        if(stream){//router tem a stream -> vai enviar para tras para a origem de acordo com o path no pacote
                            p.setHops(1);
                            InetAddress dest = p.getPath().getLast();
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ObjectOutputStream oos = new ObjectOutputStream(baos);
                            oos.writeObject(p);
                            oos.close();
                            byte[] datak = baos.toByteArray();
                            DatagramPacket sendPacket = new DatagramPacket(datak, datak.length, dest, 6001);//envia para tras na porta 6001
                            searchSocket.send(sendPacket);
                            System.out.println("SENT to pc -> to: " + dest.getHostAddress() + ":" + 6001);

                        }else{//vai procurar o destino
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
                            for (ConcurrentHashMap.Entry<IpWithMask, Boolean> entry : activeRouters.entrySet()) {
                            if(entry.getValue()){
                                InetAddress packetNetwork = entry.getKey().getNetwork();
                                boolean sent = false; // Variável para verificar se o pacote foi enviado
                                //System.out.print("IP: " + packetNetwork.getHostAddress() + " -> " );
                                for (InetAddress ip_network : p.getNetworks()) {
                                    //System.out.println("IP_NETWORK: " + ip_network);
                                    if (ip_network.getHostAddress().equals(packetNetwork.getHostAddress())){
                                        sent = true;
                                        break;
                                    }
                                }
                            
                                if (!sent) {
                                    DatagramPacket sendPacket = new DatagramPacket(datak, datak.length, entry.getKey().getAddress(), 6000);
                                    searchSocket.send(sendPacket);
                                    System.out.println("SENT: " + str + " to " + entry.getKey().getAddress() + ":" + 6000);
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

    private void updateBestPath() {

        try {

            DatagramSocket BestPathSocket = new DatagramSocket(7000);
            while (true) {
                byte[] receiveData = new byte[8192];
    
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                BestPathSocket.receive(receivePacket);
                
                byte[] data = receivePacket.getData();
                
    
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bais);

                try {
                    Object readObject = ois.readObject();
                    if (readObject instanceof Packet) {
                        Packet p = (Packet) readObject;
                        String str = p.getData();
                        InetAddress IPAddress = InetAddress.getByName(receivePacket.getAddress().toString().replace("/", ""));
                        
                        bestPath.add(IPAddress);
                        this.stream = true;
                        
                        System.out.println("RECEIVED: " + str + " from " + receivePacket.getAddress() + ":" + 7000);
                        
                        System.out.println("====================================");
                        p.setHops(p.getHops() + 1);
                        InetAddress dest = p.getPathInv().get(p.getPathInv().size() - p.getHops());
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(p);
                        oos.close();
                        byte[] datak = baos.toByteArray();
                        DatagramPacket sendPacket = new DatagramPacket(datak, datak.length, dest, 7000);//envia para tras na porta 6001
                        BestPathSocket.send(sendPacket);
                        System.out.println("SENT to pc -> to: " + dest.getHostAddress() + ":" + 7000);

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

    // encontrou a stream : caminho de volta
    private void searchResult(){
       try {

            DatagramSocket searchResultSocket = new DatagramSocket(6001);
            while (true) {
                byte[] receiveData = new byte[8192];
    
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                searchResultSocket.receive(receivePacket);
                
                byte[] data = receivePacket.getData();
                
    
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bais);

                try {
                    Object readObject = ois.readObject();
                    if (readObject instanceof Packet) {
                        Packet p = (Packet) readObject;
                        String str = p.getData();
                        InetAddress IPAddress = InetAddress.getByName(receivePacket.getAddress().toString().replace("/", ""));
                        
                        p.setPathInv(IPAddress);
                        p.setHops(p.getHops() + 1);

                        //System.out.println("RECEIVED: " + str + " from " + receivePacket.getAddress() + ":" + 6001 + " -> " + p.getPathInv().toString() + " -> " + p.getHops() + p.getPath().toString());
                        
                        System.out.println("====================================");

                        InetAddress dest = p.getPath().get(p.getPath().size() - p.getHops());
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(p);
                        oos.close();
                        byte[] datak = baos.toByteArray();
                        DatagramPacket sendPacket = new DatagramPacket(datak, datak.length, dest, 6001);//envia para tras na porta 6001
                        searchResultSocket.send(sendPacket);
                        System.out.println("SENT to pc -> to: " + dest.getHostAddress() + ":" + 6001);

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
                byte[] receiveData = new byte[8192];
                byte[] data = new byte[8192];

                data = "PING".getBytes();

                for (ConcurrentHashMap.Entry<IpWithMask, Boolean> entry : activeRouters.entrySet()) {
                    
                    try {

                        Thread.sleep(1000);
                        DatagramPacket sendPacket = new DatagramPacket(data, data.length, entry.getKey().getAddress(), 8500);
                        pingSocket.send(sendPacket);
                        //System.out.println("PING enviado para: " + entry.getKey().getAddress().getHostName());
                        
                        
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        pingSocket.receive(receivePacket);
                
                        InetAddress addr = receivePacket.getAddress();                   

                        if (addr.getHostName().equals(entry.getKey().getAddress().getHostName())) {
                            //System.out.println("PONG recebido de: " + entry.getKey().getAddress().getHostName());
                            entry.setValue(true);
                            //System.out.println("Tabela recebeu PONG:" + activeRouters.toString());
                        }
                    } catch (SocketTimeoutException e3) {
                        //System.out.println("------TIMEOUT : " + entry.getKey().getAddress().getHostName() + " -----");
                        entry.setValue(false);
                        //System.out.println("Tabela ardes -> " + activeRouters.toString());
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
                byte[] receiveData = new byte[8192];
                byte[] sendData = new byte[8192];
                
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                pongSocket.receive(receivePacket);
                //System.out.println("PING recebido! ->" + receivePacket.getAddress().getHostName());
            
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject("pong");
                oos.close();
                sendData = baos.toByteArray();

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), 8000);
                pongSocket.send(sendPacket);
                //System.out.println("PONG enviado! ->" + receivePacket.getAddress().getHostName());
            }
            

        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        
    }
}