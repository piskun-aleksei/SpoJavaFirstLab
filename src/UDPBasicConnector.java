import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;

public class UDPBasicConnector {

    protected DatagramSocket connection;
    protected InetAddress address;
    protected int port;

    public static final int LOW_TIMEOUT = 1000;
    public static final int INFINITE_TIMEOUT = 0;
    public static final int BUFFER_SIZE = 5000;
    public static final long REGULAR_PACKET_IDENTIFICATOR = -1;
    public static final long IMPORTANT_PACKET_IDENTIFICATOR = -2;
    public static final int RECEIVER_PORT = 6790;
    public static final int SENDER_PORT = 6791;
    public static final int MAX_RETRY_COUNT = 5;

    protected UDPBasicConnector(int port) {
        try {
            connection = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    protected UDPBasicConnector setRemote(InetAddress address, int port) {
        this.address = address;
        this.port = port;
        return this;
    }

    protected void send(String data) throws IOException {
        send(REGULAR_PACKET_IDENTIFICATOR, data);
    }

    protected void send(long number, String data) throws IOException {
        data += "\r\n";
        send(number, data.getBytes(), data.length());
    }

    protected void send(long number, byte[] data, int count) throws IOException {
        connection.send(CustomPacket.getPacketForSend(number, data, count, address, port));
    }

    protected void send(List<Long> data) throws IOException {
        send(REGULAR_PACKET_IDENTIFICATOR, data);
    }

    protected void send(long number, List<Long> data) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream outputStream = new DataOutputStream(byteStream);
        for (Long value : data) outputStream.writeLong(value);
        byte[] numbersBytes = byteStream.toByteArray();
        int bytesCount = numbersBytes.length > BUFFER_SIZE
                ? (BUFFER_SIZE / Long.BYTES) * Long.BYTES
                : numbersBytes.length;
        send(number, numbersBytes, bytesCount);
    }

    protected CustomPacket receive() throws IOException {
        return receive(INFINITE_TIMEOUT);
    }

    protected CustomPacket receive(int timeout) throws IOException {
        connection.setSoTimeout(timeout);
        CustomPacket enumeratedPacket = CustomPacket.getPacketForReceive(BUFFER_SIZE);
        connection.receive(enumeratedPacket.getPacket());
        return enumeratedPacket;
    }
}
