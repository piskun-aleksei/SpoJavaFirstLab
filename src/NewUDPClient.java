import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Brotorias on 20.11.2016.
 */
public class NewUDPClient implements BasicConnector {
    private DatagramSocket serverConnection;
    private InetAddress address;
    private int port;
    private BufferedReader user = new BufferedReader(new InputStreamReader(System.in));
    private ArrayList<byte[]> bytes = new ArrayList<>();

    @Override
    public void startup() {
        while (true) {
            setupConnection();
        }

    }
    private void setupConnection() {
        try {
            System.out.println("Enter UDPServer's IP address: ");
            String ip = user.readLine();
            System.out.println("Enter UDPServer's Port number: ");
            String portLine = user.readLine();
            if (portLine.equals("")) {
                return;
            }
            Integer port = Integer.parseInt(portLine);
            connect(ip, port);
        } catch (IOException | NumberFormatException e) {
        }
    }

    private void connect(String ip, Integer port) {
        try {
            serverConnection = new DatagramSocket();
            address = InetAddress.getByName(ip);
            this.port = port;
            generateRequests();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateRequests() {
        try {

            while (true) {
                String lineFromUser = user.readLine();
                sendQuiet(lineFromUser);
                String line = new String(lineFromUser);
                line = line.trim();
                int delimiterIndex = line.indexOf(" ");
                String command = delimiterIndex == -1 ? line.toLowerCase()
                        : line.toLowerCase().substring(0, delimiterIndex).trim();
                String argument = delimiterIndex == -1 ? "" : line.substring(delimiterIndex).trim();
                switch (command) {
                    case "echo":
                        System.out.println(receiveString().trim());
                        break;
                    case "download":
                        prepareDownload(argument);
                        break;
                    default:
                        System.out.println(receiveString().trim());
                }
            }
        } catch (IOException e) {
        }

    }

    private boolean prepareDownload(String filename) throws IOException {
        String fileName = new String("downloaded_" + filename).trim();
        String isFileExists = receiveString();
        System.out.println(isFileExists.trim());
        if(isFileExists.trim().equals("File was found")) {
            System.out.println("hui");
            Integer numPackets = Integer.parseInt(receiveString());
            System.out.println(numPackets);
            File file = new File(fileName);
            AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(
                    Paths.get(fileName), StandardOpenOption.READ,
                    StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            if (file.exists()) {
                return false;
            } else {
                download(fileChannel, file, numPackets);
            }
            return true;
        }
        return  false;
    }

    private void download(AsynchronousFileChannel afc, File file, int numPackets) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        long timeStart = System.currentTimeMillis();

        while (true) {
            DatagramPacket incoming = receiveData();
            String check = new String(incoming.getData());
            if(check.trim().equals("ENDFILE")) {
                afc.close();
                break;
            }
            bytes.add(incoming.getData());
        }
        for(int i = 0; i < bytes.size(); i ++){
            afc.write(ByteBuffer.wrap(Arrays.copyOfRange(bytes.get(i), 0, bytes.get(i).length - 4)),
                    fromByteArray(Arrays.copyOfRange(bytes.get(i),
                            bytes.get(i).length - 4, bytes.get(i).length)) * socket_buf);
        }
    }
    private void sendQuiet(String data) throws IOException {
        data += "\r\n";
        DatagramPacket dp = new DatagramPacket(data.getBytes(), data.getBytes().length, address, port);
        serverConnection.send(dp);
    }

    private void send(String data) throws IOException {
        data += "\r\n";
        DatagramPacket dp = new DatagramPacket(data.getBytes(), data.getBytes().length, address, port);
        serverConnection.send(dp);
        System.out.print("<<< " + data);
    }

    private void send(byte[] data, int count) throws IOException {
        DatagramPacket dp = new DatagramPacket(data, count, address, port);
        serverConnection.send(dp);
    }

    private String receiveString() throws IOException {
        byte[] buffer = new byte[socket_buf];
        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
        serverConnection.receive(incoming);
        return new String(incoming.getData(), 0, incoming.getLength());
    }

    private DatagramPacket receiveData() throws IOException {
        byte[] buffer = new byte[socket_buf];
        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
        serverConnection.receive(incoming);
        return incoming;
    }

    private static int fromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    private static byte[] toByteArray(int value) {
        return  ByteBuffer.allocate(4).putInt(value).array();
    }
}
