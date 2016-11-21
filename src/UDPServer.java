import java.io.IOException;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/**
 * Created by Aliaksei_Piskun1 on 10-Nov-16.
 */
public class UDPServer implements BasicConnector {
    private DatagramSocket server;
    private long startTime;
    private File serverFile;
    private String clientAddress;
    private DatagramPacket incomingPacket;
    private DatagramPacket sendPacket;
    private byte[] buffer;

    @Override
    public void startup() {
        try {
            server = new DatagramSocket(6790);
            startTime = System.currentTimeMillis();
            buffer = new byte[socket_buf];
            incomingPacket = new DatagramPacket(buffer, buffer.length);
            server.receive(incomingPacket);
            InetAddress clientAddr = incomingPacket.getAddress();
            int clientPort = incomingPacket.getPort();
            sendPacket =
                    new DatagramPacket(buffer, buffer.length, clientAddr, clientPort);
            if (server != null) {
                System.out.println("UDPServer is up");
                while (true) {
                    System.out.println("Waiting");
                    try{
                        processRequest();
                    }catch (SocketCustomException e){
                        System.out.println("Off");
                        server.close();
                        server = new DatagramSocket(6790);
                        startTime = System.currentTimeMillis();
                        buffer = new byte[socket_buf];
                        incomingPacket = new DatagramPacket(buffer, buffer.length);
                        server.receive(incomingPacket);
                        clientAddr = incomingPacket.getAddress();
                        clientPort = incomingPacket.getPort();
                        sendPacket =
                                new DatagramPacket(buffer, buffer.length, clientAddr, clientPort);
                        System.out.printf("Got");
                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void processRequest() throws SocketCustomException {
        try {
            send("Connected to " + server.getLocalSocketAddress());
            while (true) {
                Arrays.fill(buffer, (byte)0);
                server.receive(incomingPacket);
                String line = new String(buffer);
                if (line == null) {
                    break;
                }
                System.out.println("UDPClient: " + line.trim());
                process(line);
            }
        } catch (IOException ignored) {
        }
    }

    private void process(String line) throws IOException, SocketCustomException {
        String[] command = line.split(" ");
        String result = new String();
        for (int i = 1; i < command.length; i++)
            result += command[i] + " ";
        switch (command[0].toUpperCase()) {
            case "ECHO":
                send(result);
                break;
            case "TIME":
                time();
                break;
            case "QUIT":
                server.close();
                break;
            case "UPLOAD":
                prepareUpload(result);
                break;
            case "DOWNLOAD":
                download(result);
                break;
            default:
                send("Unknown command: " + command[0]);
        }
    }

    private void time() throws IOException {
        String result = null;
        Long currentTime = System.currentTimeMillis() - startTime;
        Long currentTimeSec = currentTime / 1000;
        if (currentTimeSec < 60) {
            result = "UDPServer is up for " + currentTimeSec.toString() + " seconds";
        } else {
            result = "UDPServer is up for " + (int) (currentTimeSec / 60) + " minutes and " +
                    (int) (currentTimeSec % 60) + " seconds";
        }
        send(result);
    }

    private void prepareUpload(String argument) throws IOException {
        File file = new File("uploaded_" + argument);
        if (!file.exists()) {
            upload(file, 0);
        } else {
            if (serverFile != null) {
                String previousClientIP = clientAddress.substring(0, clientAddress.indexOf(":"));
                String currentClientIP = server.getRemoteSocketAddress().toString()
                        .substring(0, server.getRemoteSocketAddress().toString().indexOf(":"));
                if (!previousClientIP.equals(currentClientIP)) {
                    file.delete();
                    serverFile = null;
                    clientAddress = null;
                    upload(file, 0);
                } else {
                    upload(file, file.length());
                }
            } else {
                send("-1");
                send("File exists on server");
            }
        }
    }

    private void upload(File file, long offset) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
            serverFile = file;
            clientAddress = server.getRemoteSocketAddress().toString();
        }
        while (true) {
            send(String.valueOf(offset));
            server.receive(incomingPacket);
            String line = new String(buffer);
            int count = buffer.length;
            if (new String(buffer).startsWith("FileEnding")) {
                System.out.println(server.getRemoteSocketAddress() + "UDPClient: eof");
                if (serverFile != null) {
                    String desiredFileIP = clientAddress.substring(0, clientAddress.indexOf(":"));
                    String currentClientIP = server.getRemoteSocketAddress().toString()
                            .substring(0, server.getRemoteSocketAddress().toString().indexOf(":"));
                    if (desiredFileIP.equals(currentClientIP)) {
                        serverFile = null;
                        clientAddress = null;
                        send("-1");
                        send("File " + file.getName() + " was uploaded");
                        break;
                    }
                }
            }
            offset += count;
            System.out.println("UDPClient: offset: " + offset);
            Files.write(Paths.get(file.getPath().trim()), Arrays.copyOfRange(buffer, 0, count), StandardOpenOption.APPEND);
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

    private void send(String data) throws IOException {
        data += "\r\n";
        byte[] b = data.getBytes();
        sendPacket.setData(b);
        sendPacket.setLength(b.length);
        server.send(sendPacket);
        Arrays.fill( buffer, (byte) 0 );
    }

    private void send(byte[] data, int count) throws IOException {
        sendPacket.setData(data);
        sendPacket.setLength(data.length);
        server.send(sendPacket);
        Arrays.fill( buffer, (byte) 0 );
    }


    private void quit() {
        server.close();
        System.out.println(server.getRemoteSocketAddress() + " disconnected");
    }
}
