import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Enumeration;

public class oNode {

    public ConcurrentLinkedQueue<String> ipVizinhos;

    public static String name;

    public static void main(String[] args) throws Exception {
        name = args[0];
        oNode o = new oNode();

    }

    public void parseConfigFile(String name) {
        
        try {
            
            ipVizinhos = new ConcurrentLinkedQueue<String>();
    
            //System.out.println(System.getProperty("user.dir"));
            //System.out.println("p"+name +".txt");
            FileReader f = new FileReader("./src/" + name + ".txt");
            BufferedReader buffer = new BufferedReader(f);
                       
            String ip;
            while ((ip=buffer.readLine()) != null) {
                ipVizinhos.add(ip);
            }
            
            f.close();
    
            //debug
            System.out.println("Ip's na lista: " + ipVizinhos.toString());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Erro ao ler arquivo de configuracao");
        }
        
    }

    public oNode() throws Exception {

        parseConfigFile(name);

        
        InetAddress IP = null;

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (address instanceof Inet4Address) { // Filtra apenas os endereços IPv4
                            // if(networkInterface.getDisplayName().contains("Ethernet adapter Ethernet")){
                                System.out.println("Interface: " + networkInterface.getDisplayName());
                                System.out.println("IP Address: " + address.getHostAddress());
                                IP = address;
                                System.out.println("Hostname: " + address.getHostName());
                                System.out.println();
                            // }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        DatagramSocket serverSocket = new DatagramSocket(9876);
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
                    InetAddress IPAddress = p.getIP();
                    int port = receivePacket.getPort();

                    System.out.println("RECEIVED: " + str + " from " + receivePacket.getAddress() + ":" + port);
                    Packet pk = new Packet(str ,IP);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(pk);
                    oos.close();
                    byte[] datak = baos.toByteArray();
                    String ipk = receivePacket.getAddress().toString().replace("/", "");
                    for (String ip : ipVizinhos) {
                        //System.out.println("|" + ip + "|" + receivePacket.getAddress().toString().replace("/", "") + "|");
                        if (!ip.equals(ipk)) {
                            InetAddress IPAddress2 = InetAddress.getByName(ip);
                            DatagramPacket sendPacket = new DatagramPacket(datak, datak.length, IPAddress2, 9876);
                            serverSocket.send(sendPacket);
                            System.out.println("SENT: " + str + " to " + IPAddress2 + ":" + 9876);
                        }
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
