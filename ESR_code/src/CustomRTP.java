
public class CustomRTP {
    
    private String name;
    
    private RTPpacket rtp;

    public CustomRTP(String name, RTPpacket rtp) {
        this.name = name;
        this.rtp = rtp;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RTPpacket getRtp() {
        return this.rtp;
    }

    public void setRtp(RTPpacket rtp) {
        this.rtp = rtp;
    }


    public String toString() {
        return "{" +
            " name='" + getName() + "'" +
            ", rtp='" + getRtp() + "'" +
            "}";
    }
    

   

}
