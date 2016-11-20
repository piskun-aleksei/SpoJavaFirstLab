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
import java.util.Arrays;

/**
 * Created by Brotorias on 20.11.2016.
 */
public class NewUDPClient implements BasicConnector {
    private DatagramSocket serverConnection;
    private InetAddress address;
    private int port;
    private BufferedReader user = new BufferedReader(new InputStreamReader(System.in));

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
                        //download(incoming.getAddress(), incoming.getPort(),argument);
                        break;
                    default:
                        System.out.println(receiveString().trim());
                }
            }
        } catch (IOException e) {
        }

    }

    private boolean prepareDownload(String filename, String command) throws IOException {
        String fileName = new String("downloaded_" + filename).trim();
        File file = new File(fileName);
        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(
                Paths.get(fileName), StandardOpenOption.READ,
                StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        if (file.exists()) {
            download(fileChannel, file, file.length());
        } else {
            download(fileChannel, file, 0);
        }
        return true;
    }

    private void download(AsynchronousFileChannel afc, File file, long offset) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        long timeStart = System.currentTimeMillis();
        while (true) {
            try {
                send(String.valueOf(offset));
                connection.receive(incomingPacket);
                int count = buffer.length;
                String check = new String(buffer);
                if (check.startsWith("FileEnding")) {
                    System.out.println("Sever: File downloaded");
                    send("FileSaved");
                    long time = System.currentTimeMillis() - timeStart;
                    Long speed = offset / time;
                    System.out.println("Speed: " + speed + " kb/s");
                    afc.close();
                    break;
                }

                System.out.println("Sever: offset: " + offset);
                byte[] tempBuf = new byte[getLastIndex(buffer) + 1];
                ByteBuffer.wrap(buffer).get(tempBuf, 0, getLastIndex(buffer) + 1);
                if(tempBuf[getLastIndex(buffer)] == '\00'){
                    byte[] tempBufZero = new byte[getLastIndex(buffer)];
                    ByteBuffer.wrap(buffer).get(tempBufZero, 0, getLastIndex(buffer));
                    afc.write(ByteBuffer.wrap(tempBufZero), offset);
                }
                else {
                    afc.write(ByteBuffer.wrap(tempBuf), offset);
                }
                offset += count;
            }
            catch (SocketTimeoutException e){
                offset -= buffer.length;
                send(String.valueOf(offset));
                continue;
            }
        }
        connection.setSoTimeout(0);
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
        byte[] buffer = new byte[32 * 1024];
        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
        serverConnection.receive(incoming);
        return new String(incoming.getData(), 0, incoming.getLength());
    }

    private DatagramPacket receiveData() throws IOException {
        byte[] buffer = new byte[32 * 1024];
        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
        serverConnection.receive(incoming);
        return incoming;
    }
}
