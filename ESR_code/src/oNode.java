import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class oNode {

    public ConcurrentLinkedQueue<IpWithMask> ipVizinhos;

    public static String name;

    public static void main(String[] args) throws Exception {
        name = args[0];
        oNode o = new oNode();

    }

    public void parseConfigFile(String name) {

        try {

            ipVizinhos = new ConcurrentLinkedQueue<IpWithMask>();

            // System.out.println(System.getProperty("user.dir"));
            // System.out.println("p"+name +".txt");
            FileReader f = new FileReader(name + ".txt");
            BufferedReader buffer = new BufferedReader(f);

            String ip;
            while ((ip = buffer.readLine()) != null) {
                ipVizinhos.offer(new IpWithMask(ip));
            }

            f.close();

            // debug
            System.out.println("Ip's na lista: " + ipVizinhos.toString());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Erro ao ler arquivo de configuracao");
        }

    }

    public oNode() throws Exception {

        parseConfigFile(name);


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
                    InetAddress IPAddress = InetAddress.getByName(receivePacket.getAddress().toString().replace("/", ""));
                    p.setPath(IPAddress);

                    for (InetAddress address : p.getPrevNetworks()) {
                        if (!p.getNetworks().contains(address)) {
                            p.getNetworks().add(address);
                        }
                    }
                    p.setPrevNetworksZero();
                    
                    for (IpWithMask ip : ipVizinhos) {
                            p.setPrevNetworks(ip.getNetwork());
                    }

                    System.out.println("RECEIVED: " + str + " from " + receivePacket.getAddress() + ":" + 9876);
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

                    for (IpWithMask ip : ipVizinhos) {
                        InetAddress packetNetwork = ip.getNetwork();
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
                            DatagramPacket sendPacket = new DatagramPacket(datak, datak.length, ip.getAddress(), 9876);
                            serverSocket.send(sendPacket);
                            System.out.println("SENT: " + str + " to " + ip.getAddress() + ":" + 9876);
                        }
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
