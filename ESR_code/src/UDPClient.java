import java.io.*;
import java.net.*;
import java.util.Enumeration;

class UDPClient {

    public static String routerIP;
    public static void main(String[] args) throws Exception {

        routerIP = args[0];
        //System.out.println("Router IP: " + routerIP);
        UDPClient c = new UDPClient();
        
    }

    public UDPClient() throws Exception {

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName(routerIP);
        InetAddress IP = null;
        // try {
        //     Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        //     while (interfaces.hasMoreElements()) {
        //         NetworkInterface networkInterface = interfaces.nextElement();
        //         if (networkInterface.isUp() && !networkInterface.isLoopback()) {
        //             Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
        //             while (addresses.hasMoreElements()) {
        //                 InetAddress address = addresses.nextElement();
        //                 if (address instanceof Inet4Address) { // Filtra apenas os endereços IPv4
        //                     // if(networkInterface.getDisplayName().contains("Ethernet adapter Ethernet")){
        //                         System.out.println("Interface: " + networkInterface.getDisplayName());
        //                         System.out.println("IP Address: " + address.getHostAddress());
        //                         IP = address;
        //                         System.out.println("Hostname: " + address.getHostName());
        //                         System.out.println();
        //                     // }
        //                 }
        //             }
        //         }
        //     }
        // } catch (SocketException e) {
        //     e.printStackTrace();
        // }
        
        System.out.println();

        while (true) {
            
            byte[] sendData = new byte[1024];
            byte[] receiveData = new byte[1024];
            
            String sentence = inFromUser.readLine();
            if(sentence.equals("exit")){
                clientSocket.close();
                break;
            }
            Packet p = new Packet(sentence,IP);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(p);
            oos.close();
            byte[] data = baos.toByteArray();
            
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, IP, 9876);

            clientSocket.send(sendPacket);
            System.out.println("ENVIADO: " + sentence);

            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            String modifiedSentence = new String(receivePacket.getData());
            System.out.println("FROM SERVER:" + modifiedSentence);
        
        }


    }
}
