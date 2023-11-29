
import java.net.*;
import java.awt.event.*;
import javax.swing.*;

public class ServerRTP implements ActionListener{

    // GUI:
    // ----------------
    // JLabel label;

    // RTP variables:
    // ----------------
    DatagramPacket senddp; // UDP packet containing the video frames (to send)A
    DatagramSocket RTPsocket; // socket to be used to send and receive UDP packet

    // Video constants:
    // ------------------
    int imagenb = 0; // image nb of the image currently transmitted
    VideoStream video; // VideoStream object used to access video frames
    static int MJPEG_TYPE = 26; // RTP payload type for MJPEG video
    static int FRAME_PERIOD = 100; // Frame period of the video to stream, in ms
    static int VIDEO_LENGTH = 500; // length of the video in frames

    Timer sTimer; // timer used to send the images at the video frame rate
    byte[] sBuf; // buffer used to store the images to send to the client

    //
    private InetAddress ip_rp ;
    private String name;
    private boolean isRunning;
    // --------------------------
    // Constructor
    // --------------------------
    public ServerRTP(String VideoFileName , InetAddress ip_rp,int image_nb, String name,Boolean Running){
        // init Frame
        // super("Servidor");

        this.ip_rp = ip_rp;
        this.imagenb = image_nb;
        this.name = name;
        this.isRunning = Running;
        
        // init para a parte do servidor
        sTimer = new Timer(FRAME_PERIOD, this); // init Timer para servidor
        sTimer.setInitialDelay(0);
        sTimer.setCoalesce(true);
        sBuf = new byte[15000]; // allocate memory for the sending buffer

        try {
            RTPsocket = new DatagramSocket(); // init RTP socket
            video = new VideoStream(VideoFileName); // init the VideoStream object:
            System.out.println("Servidor: vai enviar video da file " + VideoFileName);

        } catch (SocketException e) {
            System.out.println("Servidor: erro no socket: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Servidor: erro no video: " + e.getMessage());
        }

        //sTimer.start();
    }
    
    // ------------------------
    // Handler for timer
    // ------------------------
    public void actionPerformed(ActionEvent e) {

        // if the current image nb is less than the length of the video

        if(isRunning){

            if (imagenb < VIDEO_LENGTH) {
                // update current imagenb
                imagenb++;

                if(imagenb == 1)
                    System.out.println("A enviar frame " + imagenb + " - " + VIDEO_LENGTH);

                try {
                    // get next frame to send from the video, as well as its sizeum
                    int image_length = video.getnextframe(sBuf);;
                    
                    // System.out.println("A enviar frame " + imagenb + " com tamanho " + image_length);
                    // Builds an RTPpacket object containing the frame
                    RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb * FRAME_PERIOD, sBuf,
                            image_length,name);

                    // System.out.println(rtp_packet.toString());

                    // get to total length of the full rtp packet to send
                    int packet_length = rtp_packet.getlength();

                    // retrieve the packet bitstream and store it in an array of bytes
                    byte[] packet_bits = new byte[packet_length];
                    rtp_packet.getpacket(packet_bits);

                    // send the packet as a DatagramPacket over the UDP socket
                    senddp = new DatagramPacket(packet_bits, packet_length,ip_rp , 9000);
                    RTPsocket.send(senddp);

                    // System.out.println("Send frame #" + imagenb);
                    // print the header bitstream
                    rtp_packet.printheader();

                } catch (Exception ex) {
                    System.out.println("Exception caught: " + ex);
                    ex.printStackTrace();
                    System.exit(0);
                }
            } else {
                System.out.println("Final da stream");
                // ATENÇAO AO COPYRIGHT

                imagenb = 0; // image nb of the image currently transmitted
                MJPEG_TYPE = 26; // RTP payload type for MJPEG video
                FRAME_PERIOD = 100; // Frame period of the video to stream, in ms
                VIDEO_LENGTH = 500; // length of the video in frames

                // if we have reached the end of the video file, stop the timer
                sTimer.setInitialDelay(0);
                sTimer.setCoalesce(true);
                sBuf = new byte[15000];

                try {
                    video = new VideoStream(name); // init the VideoStream object:

                } catch (Exception exc) {
                    System.out.println("Erro video");
                }
            }
        }else{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            System.out.println("Servidor_"+ name + ": parou de enviar video");
        }
    }
    public void stopThread() {
        try {
            this.isRunning = false;
            sTimer.stop(); // Pare o timer se ainda estiver em execução
            
        } catch (Exception e) {
            System.err.println("Erro ao parar o thread: " + e.getMessage());
            System.exit(1);
        }
    }

    public void startThread(int image_nb) {
        try {
            this.isRunning = true;
            this.imagenb = image_nb;
            video.setFrameNb(image_nb);
            sTimer.setInitialDelay(0);
            sTimer.setCoalesce(true);
            sTimer.start(); // Iniciar o timer
            sBuf = new byte[15000]; // allocate memory for the sending buffer
            
        } catch (Exception e) {
            System.err.println("Erro ao iniciar a thread: " + e.getMessage());
            System.exit(1);
        }
    }
}