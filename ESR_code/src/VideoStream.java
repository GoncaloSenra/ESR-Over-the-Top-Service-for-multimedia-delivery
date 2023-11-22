//VideoStream

import java.io.*;

public class VideoStream {

    FileInputStream fis; // video file
    int frame_nb; // current frame nb
    String filename;

    // -----------------------------------
    // constructor
    // -----------------------------------
    public VideoStream(String filename) throws Exception {

        // init variables
        this.filename = filename;
        fis = new FileInputStream(filename);
        frame_nb = 0;//TODO: POSSIVELMENTE VAI TER QUE SER ALTERADO AQUI TAMBEM 
    }

    public void setFrameNb(int frameNb) throws Exception{
        this.frame_nb = frameNb;
        fis = new FileInputStream(filename);
    }

    // -----------------------------------
    // getnextframe
    // returns the next frame as an array of byte and the size of the frame
    // -----------------------------------
    public int getnextframe(byte[] frame) throws Exception {
        int length = 0;
        String length_string;
        byte[] frame_length = new byte[5];

        // read current frame length
        fis.read(frame_length, 0, 5);

        // transform frame_length to integer
        length_string = new String(frame_length, "UTF-8");
        length = Integer.parseInt(length_string);

        return (fis.read(frame, 0, length));
    }
}
