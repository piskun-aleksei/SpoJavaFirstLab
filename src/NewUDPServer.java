import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
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

    private void download(String filename) throws IOException, SocketCustomException {
        File file = new File(filename.trim());
        if (!file.exists()) {
            send("No such file");
            return;
        }
        send("File was found");
        RandomAccessFile fileReader;
        fileReader = new RandomAccessFile(file, "r");
        server.setSoTimeout(1000);
        String lineFromClient = null;
        int countBytes = 0;
        while (true) {
            try {
                Arrays.fill(buffer, (byte) 0);
                server.setSoTimeout(5000);
                try {
                    server.receive(incomingPacket);
                }catch (SocketTimeoutException e){
                    server.setSoTimeout(0);
                    throw  new SocketCustomException();
                }
                server.setSoTimeout(0);
                lineFromClient = new String(buffer);
                if (lineFromClient == null) {
                    throw new IOException();
                }
                if (lineFromClient.trim().equals("FileSaved")) {
                    System.out.println("UDPClient: " + lineFromClient);
                    send("File sent");
                    break;
                }

                long uploadedBytes = Long.parseLong(lineFromClient.trim());
                fileReader.seek(uploadedBytes);
                countBytes = fileReader.read(buffer);
                if (countBytes > 0) {
                    send(buffer, countBytes);
                } else {
                    send("FileEnding");
                }
            }
            catch(SocketTimeoutException e) {
                if (lineFromClient.trim().equals("FileSaved")) {
                    send("File sent");
                }
                else if(countBytes > 0){
                    send(buffer, countBytes);
                }
                else {
                    send("FileEnding");
                }
                continue;
            }
        }
        server.setSoTimeout(0);
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
}
