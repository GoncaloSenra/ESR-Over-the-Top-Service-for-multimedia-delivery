import java.io.*;
import java.net.*;

class TCPServer {
    public static void main(String argv[]) throws Exception {
        String clientSentence;
        String capitalizedSentence;
        ServerSocket welcomeSocket = new ServerSocket(6000);
        
        while (true) {
            Socket connectionSocket = welcomeSocket.accept();
            //System.out.println("ACEITOU\n");
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            clientSentence = inFromClient.readLine();
            //System.out.println("LEU\n");
            capitalizedSentence = clientSentence.toUpperCase() + "\n";
            System.out.println(clientSentence);
            outToClient.writeBytes(capitalizedSentence);
        }
    }
}
