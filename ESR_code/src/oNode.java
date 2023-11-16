
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Date;
import java.io.*;

public class oNode {

    public ConcurrentHashMap<IpWithMask, Boolean> activeRouters; // lista de routers ativos

    public ConcurrentLinkedQueue<InetAddress> ip_clients; // lista de clientes ativos

    public ConcurrentHashMap<String, ConcurrentLinkedQueue<InetAddress>> bestPath;  //lista de caminhos para o cliente de cada conteudo  
    
    public ConcurrentHashMap<String, ConcurrentLinkedQueue<InetAddress>> bestPathInv; //lista de caminhos para o RP de cada conteudo

    public static String name; // nome do ficheiro de configuração

    private ConcurrentLinkedQueue<String>  streams; // lista de streams ativas no node 

    private static boolean RP; // se o node é o RP

    private ConcurrentLinkedQueue<ServerInfo> servers; // lista de servidores ativos para o RP

    private ConcurrentHashMap<String, InetAddress> streams_IP; // lista de streams ativas no node com o ip do servidor que esta a streamar

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
        
        Thread thread11 = new Thread(() -> {
            o.cancelStream();
        });

        Thread thread12 = new Thread(() -> {
            o.cancelStreamClient();
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
        thread11.start();
        thread12.start();
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
        bestPathInv = new ConcurrentHashMap<String,ConcurrentLinkedQueue<InetAddress>>();
        servers = new ConcurrentLinkedQueue<ServerInfo>();
        ip_clients = new ConcurrentLinkedQueue<InetAddress>();
        streams = new ConcurrentLinkedQueue<String>();
        streams_IP = new ConcurrentHashMap<String, InetAddress>();
        // if(RP){//BUG: ALDRABADO
        //     streams.add("X");
        //     streams.add("Y");
        // }
    }

    private void StreamServer() { // stream desde os servers ate ao RP,(Apenas o RP tem esta funcao)

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

    // private void checkVideo(){
    //     try {

    //         DatagramSocket checkSocket = new DatagramSocket(9999);
    //         while (true) {
                
    //             Thread.sleep(10000); //vai verificar se as metricas sao as melhores 

    //             byte[] sendData = new byte[8192];
                
    //             for (String iterable_element : streams) {
    //                 if(!streams_IP.containsKey(iterable_element.trim())){// pedir a um server que tenha a stream
    //                     InetAddress ip = null;
    //                     double latency = 1000000000;
    //                     for (ServerInfo entry : servers) {

                            

    //                         for (ConcurrentHashMap.Entry<String, ConcurrentLinkedQueue<InetAddress>> entry2 : bestPath.entrySet()) {
    //                             if(entry2.getKey().equals(iterable_element)){
    //                                 if(entry.getInfo().contains(iterable_element)){
    //                                     streams_IP.put(iterable_element, entry.getAddress());
    //                                     break;
    //                                 }
    //                             }
    //                         }
    //                     }
    //                 }else{//vereficar so as metricas 

    //                 }
    //             }
                
       
    //         }
    //     } catch (SocketException e1) {
    //         e1.printStackTrace();
    //     } catch (IOException e2) {
    //         e2.printStackTrace();
    //     }catch (InterruptedException ie){
    //         System.err.println("Erro na thread checkVideo");
    //     }
    // }
    
    private void pingServer() { // veridica o estado dos servers e remove os que nao respondem
            
            try {

                DatagramSocket pingSocket = new DatagramSocket(5000);
                pingSocket.setSoTimeout(1000);
                while (true) {
                    byte[] receiveData = new byte[8192];
                    byte[] data = new byte[8192];
                    
                    Thread.sleep(2000);

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

                                //System.out.println(entry.getAddress().getHostName() + ": " + latencia);

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
                    
                    Thread.sleep(2000);

                    data = "PING".getBytes();
    
                    for (InetAddress entry : ip_clients) {
                        for(int i = 0; i < 3; i++) {
                            
                            try {

                                DatagramPacket sendPacket = new DatagramPacket(data, data.length, entry, 2001);

                                pingCSocket.send(sendPacket);
                                //System.out.println("PING enviado para: " + entry.getKey().getAddress().getHostName());
                                
                                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                                pingCSocket.receive(receivePacket);          

                                // System.out.println(entry.getHostName() + ": " + "ONLINE");

                                break;
                            } catch (SocketTimeoutException e3) {
                                //System.out.println("------TIMEOUT : " + entry.getKey().getAddress().getHostName() + " -----");
                                System.out.println("ARDEU" + i);
                                if (i == 2) {
                                    ip_clients.remove(entry);
                                    System.out.println("REMOVIDO");
                                    
                                    for (ConcurrentHashMap.Entry<String, ConcurrentLinkedQueue<InetAddress>> entry2 : bestPath.entrySet()) {
                                        if(entry2.getValue().contains(entry)){
                                            ConcurrentLinkedQueue<InetAddress> lista = entry2.getValue();
                                            if(lista.size() == 1){
                                                bestPath.remove(entry2.getKey());
                                                streams.remove(entry2.getKey());

                                                data = entry2.getKey().getBytes();
                                                for (ConcurrentHashMap.Entry<String, ConcurrentLinkedQueue<InetAddress>> entry3 : bestPathInv.entrySet()) {
                                                    if(entry3.getKey().trim().equals(entry2.getKey().trim())){
                                                        for (InetAddress ipInv : entry3.getValue()) {
                                                            DatagramPacket sendPacket1 = new DatagramPacket(data, data.length, ipInv , 6666);
                                                            pingCSocket.send(sendPacket1);
                                                            System.out.println("SENT: " + entry2.getKey() + " to " + ipInv + ":" + 6666);
                                                        }
                                                        bestPathInv.remove(entry3.getKey());
                                                    }
                                                
                                                }
                                                break;
                                            }else{
                                                lista.remove(entry);
                                                bestPath.put(entry2.getKey(), lista);
                                                
                                                break;
                                            }
                                        }
                                    }
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

    private void cancelStream(){ //cancelar o envio de uma stream para um cliente
        
        try {
            DatagramSocket cancelSocket = new DatagramSocket(6666);

            while (true) {
                byte[] receiveData = new byte[8192];
                byte[] sendData = new byte[8192];
                
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                cancelSocket.receive(receivePacket);
                //System.out.println("PING recebido! ->" + receivePacket.getAddress().getHostName());
                byte[] data = receivePacket.getData();
                String entry = new String(data);
                System.out.println("entry: |" + entry + "|");

                System.out.println("------- CANCEL ----------");
                System.out.println("RECEIVED: " + entry + " from " + receivePacket.getAddress() + ":" + 6666);
                System.out.println("0best Path: " + bestPath.toString());
                for (ConcurrentHashMap.Entry<String, ConcurrentLinkedQueue<InetAddress>> entry2 : bestPath.entrySet()) {
                    System.out.println("entry2: " +"|"+ entry2.getKey()+ "|" + "entry" + "|" + entry + "|");
                    if(entry2.getKey().trim().equals(entry.trim())){
                        ConcurrentLinkedQueue<InetAddress> lista = entry2.getValue();
                        System.out.println("lista: " + lista.toString());
                        InetAddress IPAddress = InetAddress.getByName(receivePacket.getAddress().toString().replace("/", ""));
                        if(lista.size() == 1 && lista.contains(IPAddress)) {
                            bestPath.remove(entry.trim());
                            //if(!RP)//DEBUG: ALDRABADO
                            streams.remove(entry.trim());
                            if(RP){
                                for (ConcurrentHashMap.Entry<String, InetAddress> entry3 : streams_IP.entrySet()) {
                                    if(entry3.getKey().trim().equals(entry.trim())){
                                        Packet p2 = new Packet(entry.trim());
                                        p2.setAux(0);
                                        
                                        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                                        ObjectOutputStream oos2 = new ObjectOutputStream(baos2);
                                        oos2.writeObject(p2);
                                        oos2.close();
                                        byte[] datak2 = baos2.toByteArray();
                                        DatagramPacket sendPacket2 = new DatagramPacket(datak2, datak2.length, entry3.getValue(), 9999);//envia para tras na porta 9999
                                        cancelSocket.send(sendPacket2);
                                        System.out.println("SENT to s -> to: " + entry3.getValue().getHostAddress() + ":" + 9999);
                                        streams_IP.remove(entry3.getKey());
                                        break;
                                    }
                                }
                                
                            }

                            data = entry2.getKey().getBytes();

                            for (ConcurrentHashMap.Entry<String, ConcurrentLinkedQueue<InetAddress>> entry3 : bestPathInv.entrySet()) {
                                if(entry3.getKey().trim().equals(entry2.getKey().trim())){
                                    for (InetAddress ipInv : entry3.getValue()) {
                                        DatagramPacket sendPacket1 = new DatagramPacket(data, data.length, ipInv , 6666);
                                        cancelSocket.send(sendPacket1);
                                        System.out.println("SENT: " + entry2.getKey() + " to " + ipInv + ":" + 6666);
                                        entry3.getValue().remove(ipInv);
                                    }
                                    bestPathInv.remove(entry3.getKey());
                                }
                            
                            }

                            System.out.println("streams: " + streams.toString());
                            System.out.println("best Path: " + bestPath.toString());
                            System.out.println("best Path Inv: " + bestPathInv.toString());
                            break;
                        }else {
                            lista.remove(IPAddress);
                            bestPath.put(entry2.getKey(), lista);
                            System.out.println("streams: " + streams.toString());
                            System.out.println("best Path: " + bestPath.toString());
                            System.out.println("best Path Inv: " + bestPathInv.toString());
                            break;
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

    private void cancelStreamClient(){ //router ate cliente a avisar que vai deixar de receber a stream "X"

        
        try {
            DatagramSocket cancelSocketClient = new DatagramSocket(6500);

            while (true) {
                byte[] receiveData = new byte[8192];
                byte[] sendData = new byte[8192];
                
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                cancelSocketClient.receive(receivePacket);
                //System.out.println("PING recebido! ->" + receivePacket.getAddress().getHostName());
                byte[] data = receivePacket.getData();
                String entry = new String(data);
                System.out.println("entry: |" + entry + "|");

                System.out.println("------- CANCEL To Client----------");
                System.out.println("RECEIVED: " + entry + " from " + receivePacket.getAddress() + ":" + 6500);
                System.out.println("0best Path: " + bestPath.toString());
                for (ConcurrentHashMap.Entry<String, ConcurrentLinkedQueue<InetAddress>> entry2 : bestPath.entrySet()) {
                    System.out.println("entry2: " +"|"+ entry2.getKey()+ "|" + "entry" + "|" + entry + "|");
                    if(entry2.getKey().trim().equals(entry.trim())){
                        bestPathInv.remove(entry2.getKey());
                        streams.remove(entry2.getKey().trim()); 
                        if(RP){
                            for (ConcurrentHashMap.Entry<String, InetAddress> entry3 : streams_IP.entrySet()) {
                                if(entry3.getKey().trim().equals(entry.trim())){
                                    Packet p2 = new Packet(entry.trim());
                                    p2.setAux(0);
                                    
                                    ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                                    ObjectOutputStream oos2 = new ObjectOutputStream(baos2);
                                    oos2.writeObject(p2);
                                    oos2.close();
                                    byte[] datak2 = baos2.toByteArray();
                                    DatagramPacket sendPacket2 = new DatagramPacket(datak2, datak2.length, entry3.getValue(), 9999);//envia para tras na porta 9999
                                    cancelSocketClient.send(sendPacket2);
                                    System.out.println("SENT to s -> to: " + entry3.getValue().getHostAddress() + ":" + 9999);
                                    streams_IP.remove(entry3.getKey());
                                    break;
                                }
                            }
                        }

                        ConcurrentLinkedQueue<InetAddress> lista = entry2.getValue();
                        System.out.println("lista: " + lista.toString());

                        for (InetAddress ipInv : entry2.getValue()) {
                            DatagramPacket sendPacket1 = new DatagramPacket(data, data.length, ipInv , 6500);
                            cancelSocketClient.send(sendPacket1);
                            System.out.println("SENT: " + entry2.getKey() + " to " + ipInv + ":" + 6500);
                        }
                        bestPath.remove(entry2.getKey());
                        System.out.println("streams: " + streams.toString());
                        System.out.println("best Path: " + bestPath.toString());
                        System.out.println("best Path Inv: " + bestPathInv.toString());
                        
                    }
                }    
        
            }

        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
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
                        
                        if(p.getAux() == 1) {
                            if(!ip_clients.contains(IPAddress))
                                ip_clients.add(IPAddress);
                            p.setAux(0);    
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
                        System.out.println("RECEIVED: " + p.getData() + " from " + receivePacket.getAddress() + ":" + 6000);
                        
                        
                        if(streams.contains(str) || RP){ //router tem a stream -> vai enviar para tras para a origem de acordo com o path no pacote
                            System.out.println("Contem a stream");
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
                            System.out.println("Nao contem a stream");
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
                            System.out.println(activeRouters.toString());
                            System.out.println(p.getNetworks().toString());
                            System.out.println("====================================");
                            for (ConcurrentHashMap.Entry<IpWithMask, Boolean> entry : activeRouters.entrySet()) {
                                System.out.println("##->" + entry.getKey().getNetwork().getHostAddress());
                                if(entry.getValue()){ // se o router estiver ativo
                                    InetAddress packetNetwork = entry.getKey().getNetwork();
                                    boolean sent = false; // Variável para verificar se o pacote foi enviado
                                    //System.out.print("IP: " + packetNetwork.getHostAddress() + " -> " );
                                    for (InetAddress ip_network : p.getNetworks()) {
                                        System.out.println("#######->" + ip_network.getHostAddress());
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
                                    else {
                                        System.out.println("Pacote ja enviado para este router");
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
                        
                        System.out.println(streams.toString());
                        System.out.println("|" + str + "|");
                        // if(RP){
                        //     if(!streams.contains(str)){//fazer um pedido a um server com aquela stream  
                        //         InetAddress ip = null;
                        //         double latency = 1000000000;
                                
                        //         for (ServerInfo server : servers) {
                        //             ConcurrentLinkedQueue<String> videos = server.getVideos();
                        //             if (videos.contains(str.trim())) {
                        //                 if(latency < server.getLatency()){
                        //                     ip = server.getAddress();
                        //                     latency = server.getLatency();
                        //                 }
                                        
                        //             }
                        //         }

                        //         Packet p2 = new Packet(str);
                        //         p2.setAux(1);
                                
                        //         ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        //         ObjectOutputStream oos = new ObjectOutputStream(baos);
                        //         oos.writeObject(p2);
                        //         oos.close();
                        //         byte[] datak = baos.toByteArray();
                        //         DatagramPacket sendPacket = new DatagramPacket(datak, datak.length, ip, 9999);//envia para tras na porta 9999
                        //         BestPathSocket.send(sendPacket);
                        //         System.out.println("SENT to ROUTER -> to: " + ip.getHostAddress() + ":" + 9999);
                        //         streams.add(str);
                        //         streams_IP.put(str, ip);
                        //     }
                        //     else{//faz nada 

                        //     }
                        // } else 
                        if(!streams.contains(str)){
                            if(!bestPath.containsKey(str)){
                                ConcurrentLinkedQueue<InetAddress> IPAdd = new ConcurrentLinkedQueue<InetAddress>();
                                IPAdd.add(IPAddress);
                                bestPath.put(str, IPAdd);
                            }else{//so adicionar o ip
                                ConcurrentLinkedQueue<InetAddress> lista = bestPath.get(str);
                                lista.add(IPAddress);
                                bestPath.put(str, lista);
                            }
                            
                            System.out.println("====================================");
                            this.streams.add(str);
    
                            
                            if(RP){
                                System.out.println("RP PORRA!!");
                                InetAddress ip = null;
                                double latency = 1000000000;
                                
                                for (ServerInfo server : servers) {
                                    ConcurrentLinkedQueue<String> videos = server.getVideos();
                                    if (videos.contains(str.trim())) {
                                        if(latency > server.getLatency()){
                                            ip = server.getAddress();
                                            latency = server.getLatency();
                                        }
                                        
                                    }
                                }
                                System.out.println("IP escolhido: " + ip.getHostAddress() + " -> " + latency);
                                Packet p2 = new Packet(str);
                                p2.setAux(1);
                                
                                ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                                ObjectOutputStream oos2 = new ObjectOutputStream(baos2);
                                oos2.writeObject(p2);
                                oos2.close();
                                byte[] datak2 = baos2.toByteArray();
                                DatagramPacket sendPacket2 = new DatagramPacket(datak2, datak2.length, ip, 9999);//envia para tras na porta 9999
                                BestPathSocket.send(sendPacket2);
                                System.out.println("SENT to s -> to: " + ip.getHostAddress() + ":" + 9999);
                                streams_IP.put(str, ip);
                            }else{

                                p.setHops(p.getHops() + 1);
                                InetAddress dest = p.getPathInv().get(p.getPathInv().size() - p.getHops());
                                if(!bestPathInv.containsKey(str)){
                                    ConcurrentLinkedQueue<InetAddress> IPAdd = new ConcurrentLinkedQueue<InetAddress>();
                                    IPAdd.add(dest);
                                    bestPathInv.put(str, IPAdd);
                                }else{//so adicionar o ip
                                    ConcurrentLinkedQueue<InetAddress> lista = bestPathInv.get(str);
                                    lista.add(dest);
                                    bestPathInv.put(str, lista);
                                }


                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ObjectOutputStream oos = new ObjectOutputStream(baos);
                                oos.writeObject(p);
                                oos.close();
                                byte[] datak = baos.toByteArray();
                                DatagramPacket sendPacket = new DatagramPacket(datak, datak.length, dest, 7000);//envia para tras na porta 6001
                                BestPathSocket.send(sendPacket);
                                System.out.println("SENT to pc -> to: " + dest.getHostAddress() + ":" + 7000);
                            }
                        } else {
                            if(!bestPath.containsKey(str)){
                                ConcurrentLinkedQueue<InetAddress> IPAdd = new ConcurrentLinkedQueue<InetAddress>();
                                IPAdd.add(IPAddress);
                                bestPath.put(str, IPAdd);
                            } else {//so adicionar o ip
                                ConcurrentLinkedQueue<InetAddress> lista = bestPath.get(str);
                                lista.add(IPAddress);
                                bestPath.put(str, lista);
                            }
                        }
                        
                        System.out.println("============== PATHS ==============");
                        System.out.println("BESTPATH: " + bestPath.toString());
                        System.out.println("BESTPATHINV: " + bestPathInv.toString());
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
                
                Thread.sleep(2000);
                
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
                                if(entry.getValue()){
                                    //BESTPATHINV
                                    for (ConcurrentHashMap.Entry<String, ConcurrentLinkedQueue<InetAddress>> entry2 : bestPath.entrySet()) {
                                        if(entry2.getValue().contains(entry.getKey().getAddress())){
                                            if(entry2.getValue().size() == 1){//mandar tudo down ate ao rp 

                                                for (ConcurrentHashMap.Entry<String, ConcurrentLinkedQueue<InetAddress>> entry3 : bestPathInv.entrySet()) {
                                                    if(entry3.getKey().trim().equals(entry2.getKey().trim())){
                                                        data = entry2.getKey().getBytes();
                                                        DatagramPacket sendPacket1 = new DatagramPacket(data, data.length, entry3.getValue().peek() , 6666);
                                                        pingSocket.send(sendPacket1);
                                                        System.out.println("SENT: " + entry2.getKey() + " to " + entry3.getValue().peek() + ":" + 6666);
                                                        entry3.getValue().poll();
                                                    }
                                                }
                                                bestPath.remove(entry2.getKey());
                                                //if(!RP)//DEBUG: ALDRABADO
                                                streams.remove(entry2.getKey().trim());
                                                if(RP){
                                                    for (ConcurrentHashMap.Entry<String, InetAddress> entry3 : streams_IP.entrySet()) {
                                                        if(entry3.getKey().trim().equals(entry2.getKey().trim())){
                                                            Packet p2 = new Packet(entry2.getKey().trim());
                                                            p2.setAux(0);
                                                            
                                                            ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                                                            ObjectOutputStream oos2 = new ObjectOutputStream(baos2);
                                                            oos2.writeObject(p2);
                                                            oos2.close();
                                                            byte[] datak2 = baos2.toByteArray();
                                                            DatagramPacket sendPacket2 = new DatagramPacket(datak2, datak2.length, entry3.getValue(), 9999);//envia para tras na porta 9999
                                                            pingSocket.send(sendPacket2);
                                                            System.out.println("SENT to s -> to: " + entry3.getValue().getHostAddress() + ":" + 9999);
                                                            streams_IP.remove(entry3.getKey());
                                                            break;
                                                        }
                                                    }
                                                    
                                                }     
                                            }else{//servia mais que um cliente
                                                ConcurrentLinkedQueue<InetAddress> lista = entry2.getValue();
                                                lista.remove(entry.getKey().getAddress());
                                                bestPath.put(entry2.getKey(), lista);
                                            }
                                        }
                                        System.out.println("Path" + bestPath.toString());
                                        System.out.println("Inv" + bestPathInv.toString());
                                    }  
                                    
                                    for (ConcurrentHashMap.Entry<String, ConcurrentLinkedQueue<InetAddress>> entry4 : bestPathInv.entrySet()) {

                                        if(entry4.getValue().contains(entry.getKey().getAddress())){
                                            bestPathInv.remove(entry4.getKey());
                                            Thread.sleep(3000);
                                            //TODO: procura um novo e se nao encontrar remover o conteudo da lista de bestPath e propagar ate ele 
                                            
                                            // System.out.println("Procurar novo caminho");
                                            // ConcurrentLinkedQueue<InetAddress> listapikena = new ConcurrentLinkedQueue<InetAddress>();
                                            // listapikena = new ConcurrentLinkedQueue<>();
                                            // listapikena.add(bestPathPikeno(entry4.getKey()));
                                            // bestPathInv.put(entry4.getKey(),  listapikena);
                                            
                                            //nao encontrou, remover ate aos clientes a stream nas listas
                                            
                                            for (ConcurrentHashMap.Entry<String, ConcurrentLinkedQueue<InetAddress>> entry5 : bestPath.entrySet()) {
                                                if(entry5.getKey().trim().equals(entry4.getKey().trim())){
                                                    data = entry5.getKey().getBytes();
                                                    for(InetAddress ips : entry5.getValue()){
                                                        DatagramPacket sendPacket1 = new DatagramPacket(data, data.length, ips , 6500);
                                                        pingSocket.send(sendPacket1);
                                                        System.out.println("SENT: " + entry5.getKey() + " to " + ips + ":" + 6500);
                                                    }
                                                    bestPath.remove(entry5.getKey());
                                                    //if(!RP)//DEBUG: ALDRABADO
                                                    streams.remove(entry5.getKey().trim());
                                                    if(RP){
                                                        for (ConcurrentHashMap.Entry<String, InetAddress> entry3 : streams_IP.entrySet()) {
                                                            if(entry3.getKey().trim().equals(entry4.getKey().trim())){
                                                                Packet p2 = new Packet(entry4.getKey().trim());
                                                                p2.setAux(0);
                                                                
                                                                ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                                                                ObjectOutputStream oos2 = new ObjectOutputStream(baos2);
                                                                oos2.writeObject(p2);
                                                                oos2.close();
                                                                byte[] datak2 = baos2.toByteArray();
                                                                DatagramPacket sendPacket2 = new DatagramPacket(datak2, datak2.length, entry3.getValue(), 9999);//envia para tras na porta 9999
                                                                pingSocket.send(sendPacket2);
                                                                System.out.println("SENT to s -> to: " + entry3.getValue().getHostAddress() + ":" + 9999);
                                                                streams_IP.remove(entry5.getKey());
                                                                break;
                                                            }
                                                        }
                                                        
                                                    }
                                                        
                                                }
                                            }

                                            if(entry4.getValue().size() == 1){//mandar tudo down ate ao rp 
                                                bestPathInv.remove(entry4.getKey());
                                            }else{//servia mais que um cliente
                                                ConcurrentLinkedQueue<InetAddress> lista = entry4.getValue();
                                                lista.remove(entry.getKey().getAddress());
                                                bestPathInv.put(entry4.getKey(), lista);
                                            }
                                        }
                                        System.out.println("Path" + bestPath.toString());
                                        System.out.println("Inv" + bestPathInv.toString());
                                    }
                                    /*
                                     * Se um router for abaixo verificar pelo bestPathInv se estava a enviar algum
                                     * conteudo para este router, se sim entao remover o router da lista de bestPathInv
                                     * e procurar um novo caminho para receber a stream
                                     * 
                                     * Por outro lado no caso do router estar a enviar um conteudo para o router que foi
                                     * desligado entao remover o conteudo da lista de bestPath
                                     * 
                                     * DONE: Alterar o cancelStrem / pingClient para remover o caminho pelo bestPathInv ao inves de remover
                                     * pelos vizinhos
                                     * 
                                     */
                                }
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

    // public InetAddress bestPathPikeno(String videoName){ //vai procurar o melhor caminho para o destino
    //     //1 envia pacote search(6000)
    //     //2 espera pelos pacotes a receber(6001)
    //     //3 escolhe o melhor caminho
    //     //4 envia o pacote para o melhor caminho sendo esse pacote noutra porta (7000)
        
    //     //variaveis
    //     ConcurrentLinkedQueue<Packet> pacotes = new ConcurrentLinkedQueue<Packet>();
    //     InetAddress addr = null;
    //     try {
    //         DatagramSocket searchSocket2 = new DatagramSocket(6001);
    //         //1
    //         searchSocket2.setSoTimeout(2500);
    //         Packet p = new Packet(videoName);
    //         p.setAux(1);
    //         ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //         ObjectOutputStream oos = new ObjectOutputStream(baos);
    //         oos.writeObject(p);
    //         oos.close();
    //         byte[] data = baos.toByteArray();
            
    //         for (ConcurrentHashMap.Entry<IpWithMask, Boolean> entry : activeRouters.entrySet()) {
    //             DatagramPacket sendPacket = new DatagramPacket(data, data.length, entry.getKey().getAddress(), 6000);
    //             searchSocket2.send(sendPacket);
    //             System.out.println("SENT: " + videoName + " to " + entry.getKey().getAddress() + ":" + 6000);
    //         }

    //         try {
    //             //2
    //             while (true) {
    //                 byte[] receiveData = new byte[8192];
        
    //                 DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    //                 searchSocket2.receive(receivePacket);
                    
    //                 byte[] dataR = receivePacket.getData();
                    
        
    //                 ByteArrayInputStream bais = new ByteArrayInputStream(dataR);
    //                 ObjectInputStream ois = new ObjectInputStream(bais);

    //                 Object readObject = ois.readObject();
    //                 if (readObject instanceof Packet) {
    //                     Packet pdi = (Packet) readObject;
    //                     //String str = pdi.getData();
    //                     InetAddress IPAddress = InetAddress.getByName(receivePacket.getAddress().toString().replace("/", ""));
    //                     pdi.setPathInv(IPAddress);
    //                     pdi.setHops(pdi.getHops() + 1);
    //                     pacotes.add(pdi);
    //                     //System.out.println("RECEIVED: " + str + " from " + receivePacket.getAddress() + ":" + 6001 + " -> " + pdi.getPathInv().toString() + " -> " + pdi.getHops() + pdi.getPath().toString());
                        
                        
    //                     System.out.println("====================================");
    //                 }
    //             }
    //         } catch (SocketTimeoutException e3) {
    //             System.out.println("TIMEOUT");               
    //         } catch (ClassNotFoundException e3) {
    //             e3.printStackTrace();
    //         }

    //         System.out.println("====================================");
    //         for (Packet packet : pacotes) {
    //             System.out.println(packet.toString());
    //             System.out.println(packet.getPath().toString());
    //         }
    //         System.out.println("====================================");
            
    //         //3
    //         //escolher o pacote com menos saltos e com menor latencia (ordem da lista)
    //         Packet bestPacket = pacotes.peek();
    //         for (Packet packet : pacotes) {
    //             if(packet.getHops() < bestPacket.getHops()){
    //                 bestPacket = packet;
    //             }
    //         }
    //         System.out.println("====================================");
            
    //         //4
    //         //enviar o pacote para o melhor caminho

    //         InetAddress dest2 = bestPacket.getPathInv().getLast();
    //         bestPacket.setHops(1);
    //         bestPacket.setData(videoName);

    //         ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
    //         ObjectOutputStream oos2 = new ObjectOutputStream(baos2);
    //         oos2.writeObject(bestPacket);
    //         oos2.close();
    //         byte[] datak = baos2.toByteArray();
    //         DatagramPacket sendPacket2 = new DatagramPacket(datak, datak.length, dest2, 7000);//envia para tras na porta 6001
    //         searchSocket2.send(sendPacket2);
    //         System.out.println("SENT to pc -> to: " + dest2.getHostAddress() + ":" + 7000);
    //         searchSocket2.close();
    //         //
    //         addr = bestPacket.getPathInv().getLast();
            
    //     } catch (SocketException e1) {
    //         e1.printStackTrace();
    //     } catch (IOException e2) {
    //         e2.printStackTrace();
    //     }
    //     return addr;
    // }


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
