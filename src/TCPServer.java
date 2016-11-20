import javafx.util.Pair;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TCPServer implements BasicConnector {
    private ServerSocket server;
    private Socket connection;
    private InputStream input;
    private OutputStream output;
    private long startTime;
    private File serverFile;
    private String clientAddress;

    @Override
    public void startup() {
        try {
            server = new ServerSocket(6790);
            startTime = System.currentTimeMillis();
            if (server != null) {
                System.out.println("TCPServer is up");
                while (true) {
                    connection = server.accept();
                    input = connection.getInputStream();
                    output = connection.getOutputStream();
                    System.out.println(connection.getRemoteSocketAddress() + " connected");
                    processRequest();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void processRequest() {
        try {
            send("Connected to " + connection.getLocalSocketAddress());
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
            while (true) {
                String line = inputReader.readLine();
                if (line == null) {
                    break;
                }
                System.out.println("TCPClient: " + line);
                process(line);
            }
        } catch (IOException ignored) {
        } finally {
            quit();
        }
    }

    private void process(String line) throws IOException {
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
                connection.close();
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
            result = "TCPServer is up for " + currentTimeSec.toString() + " seconds";
        } else {
            result = "TCPServer is up for " + (int) (currentTimeSec / 60) + " minutes and " +
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
                String currentClientIP = connection.getRemoteSocketAddress().toString()
                        .substring(0, connection.getRemoteSocketAddress().toString().indexOf(":"));
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
            clientAddress = connection.getRemoteSocketAddress().toString();
        }
        while (true) {
            send(String.valueOf(offset));
            byte[] buffer = new byte[socket_buf];
            int count = input.read(buffer);
            if (new String(buffer).startsWith("FileEnding")) {
                System.out.println(connection.getRemoteSocketAddress() + "TCPClient: eof");
                if (serverFile != null) {
                    String desiredFileIP = clientAddress.substring(0, clientAddress.indexOf(":"));
                    String currentClientIP = connection.getRemoteSocketAddress().toString()
                            .substring(0, connection.getRemoteSocketAddress().toString().indexOf(":"));
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
            System.out.println("TCPClient: offset: " + offset);
            Files.write(Paths.get(file.getPath().trim()), Arrays.copyOfRange(buffer, 0, count), StandardOpenOption.APPEND);
        }
    }

    private void download(String filename) throws IOException {
        File file = new File(filename.trim());
        if (!file.exists()) {
            send("No such file");
            return;
        }
        send("File was found");
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
        RandomAccessFile fileReader;
        fileReader = new RandomAccessFile(file, "r");
        int urgent = 0;
        while (true) {
            String lineFromClient = inputReader.readLine();
            if (lineFromClient == null) {
                throw new IOException();
            }
            if (lineFromClient.trim().equals("FileSaved")) {
                System.out.println("TCPClient: " + lineFromClient);
                send("File sent");
                break;
            }
            long uploadedBytes = Long.parseLong(lineFromClient);
            fileReader.seek(uploadedBytes);
            byte[] bytes = new byte[socket_buf];
            int countBytes = fileReader.read(bytes);

            urgent++;
            if(urgent == 100){
                urgent = 0;
                connection.sendUrgentData(-128);
                connection.sendUrgentData(-127);
                connection.sendUrgentData(127);
                connection.sendUrgentData(126);
            }
            if (countBytes > 0) {
                send(bytes, countBytes);
            } else {
                send("FileEnding");
            }
        }
    }

    private void send(String data) throws IOException {
        data += "\r\n";
        output.write(data.getBytes());
        output.flush();
    }

    private void send(byte[] data, int count) throws IOException {
        output.write(data, 0, count);
        output.flush();
    }


    private void quit() {
        try {
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(connection.getRemoteSocketAddress() + " disconnected");
    }
}
