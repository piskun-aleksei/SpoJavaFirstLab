import java.io.IOException;


import java.io.*;
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
 * Created by Stas on 08.11.16.
 */
public class UDPClient implements BasicConnector {
    private DatagramSocket connection;
    private BufferedReader user = new BufferedReader(new InputStreamReader(System.in));
    private byte[] buffer;
    private DatagramPacket incomingPacket;
    private DatagramPacket sendPacket;


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
            connection = new DatagramSocket();
            buffer = new byte[socket_buf];
            incomingPacket = new DatagramPacket(buffer, buffer.length);
            InetAddress IPAddress = InetAddress.getByName(ip);
            sendPacket = new DatagramPacket(buffer, buffer.length, IPAddress, port);
            send("Got ya!");
            generateRequests();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateRequests() {
        try {

            while (true) {
                Arrays.fill(buffer, (byte) 0);
                connection.receive(incomingPacket);
                String lineFromServer = new String(buffer);

                Arrays.fill(buffer, (byte) 0);
                if (lineFromServer == null) {
                    throw new IOException();
                }
                System.out.println("Sever: " + lineFromServer.trim());
                while (true) {
                    String lineFromUser = user.readLine();
                    if (processCommand(lineFromUser)) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
        }
        disconnect();
    }

    private boolean processCommand(String line) throws IOException {
        String[] command = line.split(" ");
        String result = new String();
        for (int i = 1; i < command.length; i++)
            result += command[i] + " ";
        switch (command[0].toUpperCase()) {
            case "UPLOAD":
                return upload(result, line);
            case "DOWNLOAD":
                return prepareDownload(result, line);
            default:
                send(line);
        }
        return true;
    }

    private boolean upload(String filename, String command) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("No such file on client");
            return false;
        }
        send(command);
        RandomAccessFile fileReader;
        fileReader = new RandomAccessFile(file, "r");
        while (true) {
            connection.receive(incomingPacket);
            String lineFromServer = new String(buffer);
            if (lineFromServer == null) throw new IOException();
            long uploadedBytes = Long.parseLong(lineFromServer);
            if (uploadedBytes != -1) {
                fileReader.seek(uploadedBytes);
                int countBytes = fileReader.read(buffer);
                if (countBytes > 0) {
                    send(buffer, countBytes);
                } else {
                    send("FileEnding");
                }
            } else {
                break;
            }
        }
        return true;
    }

    private boolean checkForFile(String command) throws IOException {
        send(command);
        connection.receive(incomingPacket);
        String line = new String(buffer);
        if (line == null) throw new IOException();
        System.out.println("Sever: " + line);
        switch (line.trim()) {
            case "No file":
                return false;
            case "File was found":
                return true;
        }
        return false;
    }

    private boolean prepareDownload(String filename, String command) throws IOException {
        String fileName = new String("downloaded_" + filename).trim();
        File file = new File(fileName);
        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(
                Paths.get(fileName), StandardOpenOption.READ,
                StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        if (file.exists()) {
            if (!checkForFile(command)) {
                return false;
            }
            download(fileChannel, file, file.length());
        } else {
            if (!checkForFile(command)) {
                return false;
            }
            download(fileChannel, file, 0);
        }
        return true;
    }

    private void download(AsynchronousFileChannel afc, File file, long offset) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        long timeStart = System.currentTimeMillis();
        connection.setSoTimeout(1000);
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
                if (tempBuf[getLastIndex(buffer)] == '\00') {
                    byte[] tempBufZero = new byte[getLastIndex(buffer)];
                    ByteBuffer.wrap(buffer).get(tempBufZero, 0, getLastIndex(buffer));
                    afc.write(ByteBuffer.wrap(tempBufZero), offset);
                } else {
                    afc.write(ByteBuffer.wrap(tempBuf), offset);
                }
                offset += count;
            } catch (SocketTimeoutException e) {
                offset -= buffer.length;
                send(String.valueOf(offset));
                continue;
            }
        }
        connection.setSoTimeout(0);
    }

    private void send(String data) throws IOException {
        data += "\r\n";
        byte[] b = data.getBytes();
        sendPacket.setData(b);
        sendPacket.setLength(b.length);
        connection.send(sendPacket);
    }

    private void send(byte[] data, int count) throws IOException {
        sendPacket.setData(data);
        sendPacket.setLength(count);
        connection.send(sendPacket);
    }

    private void disconnect() {
        System.out.println(connection.getRemoteSocketAddress() + " disconnected");
    }

    private int getLastIndex(byte[] bytes) {
        int pos = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                for (int j = i; j < bytes.length; j++) {
                    if (bytes[j] != 0) {
                        break;
                    }
                    if (j == bytes.length - 1) {
                        pos = i;
                        break;
                    }
                }
                if (pos != 0) {
                    break;
                }
            }
            if (i == bytes.length - 1) {
                pos = bytes.length - 1;
            }
        }
        return pos;
    }
}