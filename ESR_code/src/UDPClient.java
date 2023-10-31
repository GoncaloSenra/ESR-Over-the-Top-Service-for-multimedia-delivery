import java.io.*;
import java.net.*;

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
        IpWithMask ip_router = new IpWithMask(routerIP);

        
        System.out.println();


        //FIXME: mandar mais do que uma mensagem
        while (true) {
            
            byte[] sendData = new byte[1024];
            byte[] receiveData = new byte[1024];
            
            String sentence = inFromUser.readLine();
            if(sentence.equals("exit")){
                clientSocket.close();
                break;
            }
            Packet p = new Packet(sentence);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(p);
            oos.close();
            byte[] data = baos.toByteArray();
            
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, ip_router.getAddress(), 9000);

            clientSocket.send(sendPacket);
            System.out.println("ENVIADO: " + sentence);

            // DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            // clientSocket.receive(receivePacket);
            // String modifiedSentence = new String(receivePacket.getData());
            // System.out.println("FROM SERVER:" + modifiedSentence);
        
        }


    }
}
