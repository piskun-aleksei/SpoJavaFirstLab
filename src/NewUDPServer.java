import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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
                        switch (command) {
                            case "echo":
                                send(incoming.getAddress(), incoming.getPort(), argument);
                                break;
                            case "download":
                                //download(incoming.getAddress(), incoming.getPort(),argument);
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

    private void send(InetAddress address, int port, String data) throws IOException {
        data += "\r\n";
        DatagramPacket dp = new DatagramPacket(data.getBytes(), data.getBytes().length, address, port);
        server.send(dp);
        System.out.print(address + ":" + port + " <<< " + data);
    }

    private void send(InetAddress address, int port, byte[] data, int count) throws IOException {
        DatagramPacket dp = new DatagramPacket(data, count, address, port);
        server.send(dp);
    }

    private void sendQuiet(InetAddress address, int port, String data) throws IOException {
        data += "\r\n";
        DatagramPacket dp = new DatagramPacket(data.getBytes(), data.getBytes().length, address, port);
        server.send(dp);
    }

    private String receiveString() throws IOException {
        byte[] buffer = new byte[32 * 1024];
        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
        server.receive(incoming);
        return new String(incoming.getData(), 0, incoming.getLength());
    }

    private DatagramPacket receiveData() throws IOException {
        byte[] buffer = new byte[32 * 1024];
        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
        server.receive(incoming);
        return incoming;
    }
}
