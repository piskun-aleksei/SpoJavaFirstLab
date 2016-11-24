import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Brotorias on 20.11.2016.
 */
public class NewUDPServer implements BasicConnector {
    private DatagramSocket server;
    private BufferedReader user = new BufferedReader(new InputStreamReader(System.in));

    @Override
    public void startup() {
        try {
            server = new DatagramSocket(6790);
            if (server != null) {
                System.out.println("Server is up");
                try {
                    while (true) {
                        DatagramPacket incoming = receiveData();
                        String line = new String(incoming.getData(), 0, incoming.getLength());
                        line = line.trim();
                        int delimiterIndex = line.indexOf(" ");
                        String command = delimiterIndex == -1 ? line.toLowerCase()
                                : line.toLowerCase().substring(0, delimiterIndex).trim();
                        String argument = delimiterIndex == -1 ? "" : line.substring(delimiterIndex).trim();
                        System.out.printf("");
                        switch (command) {
                            case "echo":
                                send(incoming.getAddress(), incoming.getPort(), argument);
                                break;
                            case "download":
                                download(incoming.getAddress(), incoming.getPort(), argument);
                                break;
                            default:
                                send(incoming.getAddress(), incoming.getPort(), "Unknown command: " + command);
                        }
                        //process(incoming.getAddress(), incoming.getPort(), line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private int fileOnServer(InetAddress address, int port, File file) throws IOException {
        try {
            if (!file.exists()) {
                send(address, port, "No such file");
                return 0;
            } else {
                send(address, port, "File was found");
            }
        }catch (SocketTimeoutException e){
            return -1;
        }
        return 1;
    }

    private int sendPacketsCount(InetAddress address, int port, int packetsCount) throws IOException {
        try {
                send(address, port, Integer.toString(packetsCount));
        }catch (SocketTimeoutException e){
            return -1;
        }
        return 1;
    }

    private int sendEndingPacket(InetAddress address, int port) throws IOException {
        try {
            send(address, port, "ENDFILE");
            return 0;
        } catch (SocketTimeoutException e) {
            return -1;
        }
    }

    private String checkWait() throws IOException {
        try {
            receiveString();
            return "Succes";
        } catch (SocketTimeoutException e) {
            return "Not";
        }
    }

    private void download(InetAddress address, int port, String filename) throws IOException {
        server.setSoTimeout(0);
        File file = new File(filename.trim());
        int result = -2;
        result = fileOnServer(address, port, file);
        receiveString();
        if(result == 0) return;
        RandomAccessFile fileReader;
        fileReader = new RandomAccessFile(file, "r");
        Integer countPackets = Math.round(fileReader.length() / socket_buf);
        if(fileReader.length() % socket_buf != 0)
            countPackets++;
        result = -2;

        result = sendPacketsCount(address, port, countPackets);

        receiveString();

        int currentPacket = 0;
        server.setSoTimeout(50);
        for (int i = 0; i <countPackets; i++) {
            fileReader.seek(currentPacket * socket_buf);
            byte[] bytes = new byte[socket_buf];
            int countBytes = fileReader.read(bytes);
            if (countBytes <= 0) break;
            send(address, port, bytes, countBytes, currentPacket);
            currentPacket++;
            System.out.println("packSent: " + currentPacket);
            System.out.println(checkWait());
        }

        server.setSoTimeout(0);
        result = sendEndingPacket(address, port);

        receiveString();

    }

    private void send(InetAddress address, int port, String data) throws IOException {
        data += "\r\n";
        DatagramPacket dp = new DatagramPacket(data.getBytes(), data.getBytes().length, address, port);
        server.send(dp);
        System.out.print(address + ":" + port + " <<< " + data);
    }

    private void send(InetAddress address, int port, byte[] data, int count, Integer packetNum) throws IOException {
        byte[] tempBuf = concatArrays(data, toByteArray(packetNum));
        System.out.println("packetSent: " + fromByteArray(Arrays.copyOfRange(tempBuf, tempBuf.length - 4, tempBuf.length)));
        DatagramPacket dp = new DatagramPacket(tempBuf, count + 4, address, port);
        server.send(dp);
    }

    private void sendQuiet(InetAddress address, int port, String data) throws IOException {
        data += "\r\n";
        DatagramPacket dp = new DatagramPacket(data.getBytes(), data.getBytes().length, address, port);
        server.send(dp);
    }

    private String receiveString() throws IOException {
        byte[] buffer = new byte[socket_buf];
        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
        server.receive(incoming);
        return new String(incoming.getData(), 0, incoming.getLength());
    }

    private DatagramPacket receiveData() throws IOException {
        byte[] buffer = new byte[socket_buf];
        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
        server.receive(incoming);
        return incoming;
    }

    private static int fromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    private static byte[] toByteArray(int value) {
        return  ByteBuffer.allocate(4).putInt(value).array();
    }

    private static byte[] concatArrays(byte[] a, byte[] b){
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
