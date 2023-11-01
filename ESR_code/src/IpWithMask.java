import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpWithMask implements Serializable{
    private InetAddress address;
    private InetAddress network;
    private int mask;

    public IpWithMask(String ipWithMask) throws UnknownHostException {
        String[] parts = ipWithMask.split("/");
        if (parts.length == 2) {
            this.address = InetAddress.getByName(parts[0]);
            this.mask = Integer.parseInt(parts[1]);
            // Aplicar a máscara para obter a rede
            byte[] addressBytes = address.getAddress();
            for (int i = this.mask; i < addressBytes.length * 8; i++) {
                int byteIndex = i / 8;
                int bitIndex = 7 - (i % 8);
                addressBytes[byteIndex] &= ~(1 << bitIndex);
            }
            this.network = InetAddress.getByAddress(addressBytes);
        } else {
            throw new IllegalArgumentException("Formato inválido para endereço IP com máscara.");
        }
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getMask() {
        return mask;
    }

    public InetAddress getNetwork() {
        return network;
    }

    @Override
    public String toString() {
        return address.getHostAddress() + "/" + mask + " (" + network.getHostAddress() + ")";
    }
}

