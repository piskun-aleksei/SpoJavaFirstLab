import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;


public class Client implements BasicConnector {

    private Socket connection;
    private InputStream input;
    private OutputStream output;
    private BufferedReader user = new BufferedReader(new InputStreamReader(System.in));


    @Override
    public void startup() {
        while (true) {
            setupConnection();
        }
    }

    private void setupConnection() {
        try {
            System.out.println("Enter Server's IP address: ");
            String ip = user.readLine();
            System.out.println("Enter Server's Port number: ");
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
            connection = new Socket(ip, port);
            input = connection.getInputStream();
            output = connection.getOutputStream();
            generateRequests();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateRequests() {
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
            while (true) {
                String lineFromServer = inputReader.readLine();
                if (lineFromServer == null) {
                    throw new IOException();
                }
                System.out.println("Sever: " + lineFromServer);
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
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
        RandomAccessFile fileReader;
        fileReader = new RandomAccessFile(file, "r");
        while (true) {
            String lineFromServer = inputReader.readLine();
            if (lineFromServer == null) throw new IOException();
            long uploadedBytes = Long.parseLong(lineFromServer);
            if (uploadedBytes != -1) {
                fileReader.seek(uploadedBytes);
                byte[] bytes = new byte[socket_buf];
                int countBytes = fileReader.read(bytes);
                if (countBytes > 0) {
                    send(bytes, countBytes);
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
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
        String line = inputReader.readLine();
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
        File file = new File("downloaded_" + filename);
        if (file.exists()) {
            if (!checkForFile(command)) {
                return false;
            }
            download(file, file.length());
        } else {
            if (!checkForFile(command)) {
                return false;
            }
            download(file, 0);
        }
        return true;
    }

    private void download(File file, long offset) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        while (true) {
            send(String.valueOf(offset));
            byte[] buffer = new byte[socket_buf];
            int count = input.read(buffer);
            String check = new String(buffer);
            if (check.startsWith("FileEnding")) {
                System.out.println("Sever: File downloaded");
                send("FileSaved");
                break;
            }
            offset += count;
            System.out.println("Sever: offset: " + offset);
            Files.write(Paths.get(file.getPath().trim()), Arrays.copyOfRange(buffer, 0, count), StandardOpenOption.APPEND);
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

    private void disconnect() {
        System.out.println(connection.getRemoteSocketAddress() + " disconnected");
    }
}
