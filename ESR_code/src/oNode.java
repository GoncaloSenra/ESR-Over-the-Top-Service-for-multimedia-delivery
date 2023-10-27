import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Enumeration;

public class oNode {

    public ConcurrentLinkedQueue<InetAddress> ipVizinhos;

    public static String name;

    public static void main(String[] args) throws Exception {
        name = args[0];
        oNode o = new oNode();

    }

    public void parseConfigFile(String name) {

        try {

            ipVizinhos = new ConcurrentLinkedQueue<InetAddress>();

            // System.out.println(System.getProperty("user.dir"));
            // System.out.println("p"+name +".txt");
            FileReader f = new FileReader(name + ".txt");
            BufferedReader buffer = new BufferedReader(f);

            String ip;
            while ((ip = buffer.readLine()) != null) {
                ipVizinhos.add(InetAddress.getByName(ip));
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
                    int port = receivePacket.getPort();
                    p.setPath(IPAddress);


                    System.out.println("RECEIVED: " + str + " from " + receivePacket.getAddress() + ":" + port);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(p);
                    oos.close();
                    byte[] datak = baos.toByteArray();
                    
                    //System.out.println("|" + ip + "|" + receivePacket.getAddress().toString().replace("/", "") + "|");
                    for (InetAddress ip : ipVizinhos) {
                        if (!p.getPath().contains(ip)) {
                            InetAddress IPAddress2 = ip;
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
