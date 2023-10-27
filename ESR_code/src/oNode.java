import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;

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
                    String IPAddress = receivePacket.getAddress().toString().replace("/", "");
                    int port = receivePacket.getPort();

                    System.out.println("RECEIVED: " + str + " from " + IPAddress + ":" + port);
                    
                    for (String ip : ipVizinhos) {
                        if (!ip.equals(IPAddress)) {
                            InetAddress IPAddress2 = InetAddress.getByName(ip);
                            DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress2, port);
                            serverSocket.send(sendPacket);
                            System.out.println("SENT: " + str + " to " + IPAddress2 + ":" + port);
                        }
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
