import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Date;


public class oNode {

    public ConcurrentHashMap<IpWithMask, Boolean> activeRouters;

    public ConcurrentLinkedQueue<InetAddress> ip_clients;

    public ConcurrentHashMap<String, ConcurrentLinkedQueue<InetAddress>> bestPath;  

    public static String name;

    private ConcurrentLinkedQueue<String>  streams;

    private static boolean RP;

    private ConcurrentLinkedQueue<ServerInfo> servers;

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
            o.StreamClient();
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
        
        Thread thread6 = new Thread(() -> {
            o.updateBestPath();
        });
        
        Thread thread10 = new Thread(() -> {
            o.pingClient();
        });

        if (RP) {
            Thread thread7 = new Thread(() -> {
                o.StreamServer();
            });
            
            Thread thread8 = new Thread(() -> {
                o.pingServer();
            });

            Thread thread9 = new Thread(() -> {
                o.connectServer();
            });

            thread7.start();
            thread8.start();
            thread9.start();
        } else {
            thread1.start();
        }
        
        // Inicia as threads
        thread2.start();
        thread3.start();
        thread4.start();
        thread5.start();
        thread6.start();
        thread10.start();
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

    public oNode() { // construtor
        this.parseConfigFile(name);
        bestPath = new ConcurrentHashMap<String,ConcurrentLinkedQueue<InetAddress>>();
        servers = new ConcurrentLinkedQueue<ServerInfo>();
        ip_clients = new ConcurrentLinkedQueue<InetAddress>();
        streams = new ConcurrentLinkedQueue<String>();
        if(RP){//BUG: ALDRABADO
            streams.add("X");
            streams.add("Y");
        }
    }

    private void StreamServer() { // stream desde os servers ate ao RP,(Apenas o RP tem esta funcao)

        try {

            DatagramSocket serverSocket = new DatagramSocket(9000);
            while (true) {
                try {
                    Thread.sleep(7500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                byte[] receiveData = new byte[8192];
                byte[] sendData = new byte[8192];
                
                String x = "X";
                String y = "Y";
                
                Packet px = new Packet(x);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(px);
                oos.close();
                byte[] datax = baos.toByteArray();
                
                Packet py =  new Packet(y);
                ByteArrayOutputStream baosy = new ByteArrayOutputStream();
                ObjectOutputStream oosy = new ObjectOutputStream(baosy);
                oosy.writeObject(py);
                oosy.close();
                byte[] datay = baos.toByteArray();

                for (ConcurrentHashMap.Entry<String,ConcurrentLinkedQueue<InetAddress>> entry : bestPath.entrySet()) {
                    
                    for (InetAddress ip : entry.getValue()) {
                        if (entry.getKey().equals(x)) {
                            DatagramPacket sendPacket = new DatagramPacket(datax, datax.length, ip, 9000);
                            serverSocket.send(sendPacket);
                            System.out.println("SENT: " + x + " to " + ip.getHostName() + ":" + 9000);

                        } else if (entry.getKey().equals(y)) {
                            DatagramPacket sendPacket = new DatagramPacket(datay, datay.length, ip, 9000);
                            serverSocket.send(sendPacket);
                            System.out.println("SENT: " + y + " to " + ip.getHostName() + ":" + 9000);

                        }
                        
                    }
                }         
            }
        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }
    
    private void pingServer() { // veridica o estado dos servers e remove os que nao respondem
            
            try {

                DatagramSocket pingSocket = new DatagramSocket(5000);
                pingSocket.setSoTimeout(1000);
                while (true) {
                    byte[] receiveData = new byte[8192];
                    byte[] data = new byte[8192];
                    
                    Thread.sleep(5000);

                    data = "PING".getBytes();
    
                    for (ServerInfo entry : servers) {
                        for(int i = 0; i < 3; i++) {
                            
                            try {

                                DatagramPacket sendPacket = new DatagramPacket(data, data.length, entry.getAddress() ,5001);
                                Date now = new Date();
                                double timeSend = now.getTime();
                                pingSocket.send(sendPacket);
                                //System.out.println("PING enviado para: " + entry.getKey().getAddress().getHostName());
                                
                                
                                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                                pingSocket.receive(receivePacket);

                                now  = new Date();
                                double timeRecieve = now.getTime();

                                double latencia = timeRecieve - timeSend;

                                entry.setLatency(latencia);                 

                                System.out.println(entry.getAddress().getHostName() + ": " + latencia);

                                break;
                            } catch (SocketTimeoutException e3) {
                                //System.out.println("------TIMEOUT : " + entry.getKey().getAddress().getHostName() + " -----");
                                System.out.println("ARDEU" + i);
                                if (i == 2) {
                                    System.out.println("REMOVIDO");
                                    servers.remove(entry);
                                }else {
                                    continue;
                                }
                                //System.out.println("Tabela ardes -> " + activeRouters.toString());          
                            } 

                        }

                    }
                }
            } catch (SocketException e1) {
                e1.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            }catch (InterruptedException e4){
                e4.printStackTrace();
            }

    }

    private void pingClient() { // veridica o estado dos clientes e remove os que nao respondem
            
            try {
    
                DatagramSocket pingCSocket = new DatagramSocket(2000);
                pingCSocket.setSoTimeout(1000);
                while (true) {
                    byte[] receiveData = new byte[8192];
                    byte[] data = new byte[8192];
                    
                    Thread.sleep(5000);

                    data = "PING".getBytes();
    
                    for (InetAddress entry : ip_clients) {
                        for(int i = 0; i < 3; i++) {
                            
                            try {

                                DatagramPacket sendPacket = new DatagramPacket(data, data.length, entry, 2001);

                                pingCSocket.send(sendPacket);
                                //System.out.println("PING enviado para: " + entry.getKey().getAddress().getHostName());
                                
                                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                                pingCSocket.receive(receivePacket);          

                                System.out.println(entry.getHostName() + ": " + "ONLINE");

                                break;
                            } catch (SocketTimeoutException e3) {
                                //System.out.println("------TIMEOUT : " + entry.getKey().getAddress().getHostName() + " -----");
                                System.out.println("ARDEU" + i);
                                if (i == 2) {
                                    ip_clients.remove(entry);
                                    System.out.println("REMOVIDO");
                                    bestPath.remove(entry);
                                }else {
                                    continue;
                                }
                                //System.out.println("Tabela ardes -> " + activeRouters.toString());          
                            } 

                        }

                    }
                }
            } catch (SocketException e1) {
                e1.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            }catch (InterruptedException e4){
                e4.printStackTrace();
            }

    }

    private void connectServer() { // recebe conexão do servidor e adiciona-o à lista de servidores
        
        try {
            
            DatagramSocket helloSocket = new DatagramSocket(5003);

            while (true) {
                byte[] receiveData = new byte[8192];
                
                
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                helloSocket.receive(receivePacket);
                byte[] data = receivePacket.getData();
                
    
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bais);

                try {
                    Object readObject = ois.readObject();
                    if (readObject instanceof Packet) {
                        Packet p = (Packet) readObject;
                        ConcurrentLinkedQueue<String> info = p.getInfo();
                        InetAddress IPAddress = InetAddress.getByName(receivePacket.getAddress().toString().replace("/", ""));
                        
                        servers.add(new ServerInfo(IPAddress, info,-1));

                        System.out.println("RECEIVED: " + servers.toString() + " from " + receivePacket.getAddress() + ":" + 5003);
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

    private void StreamClient() { //recebe stream do RP e envia para os clientes

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
                        //InetAddress IPAddress = InetAddress.getByName(receivePacket.getAddress().toString().replace("/", ""));
                        
    
                        System.out.println("RECEIVED: " + str + " from " + receivePacket.getAddress() + ":" + 9000);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(p);
                        oos.close();
                        
                        byte[] datak = baos.toByteArray();
                        
                        // envia a "stream" para todos os ips que quiserem a stream
                        for (ConcurrentHashMap.Entry<String, ConcurrentLinkedQueue<InetAddress>> entry : bestPath.entrySet()) {
                            if(entry.getKey().equals(str)){
                                for (InetAddress ip : entry.getValue()) {

                                    DatagramPacket sendPacket = new DatagramPacket(datak, datak.length, ip, 9000);
                                    serverSocket.send(sendPacket);
                                    System.out.println("SENT: " + str + " to " + ip.getHostAddress() + ":" + 9000);

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

    private void search() { // procura o RP e envia para tras (cliente) o caminho para o RP

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
                        
                        if(str.equals("search0")){
                            if(!ip_clients.contains(IPAddress))
                                ip_clients.add(IPAddress);
                            p.setData("search");    
                        }
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
                        
                        if(streams.contains(str)){ //router tem a stream -> vai enviar para tras para a origem de acordo com o path no pacote
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

                        }else{ // vai procurar o destino
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

    private void updateBestPath() { // recebe o caminho para o RP (ou para onde ja tem a stream) e guarda-o

        try {

            DatagramSocket BestPathSocket = new DatagramSocket(7000);
            while (true) {
                System.out.println(bestPath.toString());
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
                        
                        
                        System.out.println("RECEIVED: " + str + " from " + receivePacket.getAddress() + ":" + 7000);
                        
                        if(!streams.contains(str)){
                            ConcurrentLinkedQueue<InetAddress> IPAdd = new ConcurrentLinkedQueue<InetAddress>();
                            IPAdd.add(IPAddress);
                            bestPath.put(str, IPAdd);
                            System.out.println("====================================");
                            this.streams.add(str);
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
                        } else {
                            bestPath.get(str).add(IPAddress);//DEBUG:
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

    private void searchResult(){ // encontrou a stream : caminho de volta
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
                
                Thread.sleep(5000);
                
                for (ConcurrentHashMap.Entry<IpWithMask, Boolean> entry : activeRouters.entrySet()) {
                    for(int i = 0; i < 3; i++){
                    
                        try {
                            
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
                            
                            break;
                        } catch (SocketTimeoutException e3) {
                            //System.out.println("------TIMEOUT : " + entry.getKey().getAddress().getHostName() + " -----");
                            if (i == 2) {
                                //System.out.println("desativado: "+ entry.getKey().getAddress().getHostName());
                                entry.setValue(false);
                            }else {
                                continue;
                            }                
                        }
                    }
                }
                
            }

        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        } catch (InterruptedException e4){
            e4.printStackTrace();
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