import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;


public class TCPClient implements BasicConnector {

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
            System.out.println("Enter TCPServer's IP address: ");
            String ip = user.readLine();
            System.out.println("Enter TCPServer's Port number: ");
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
            connection.setOOBInline(true);
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

        while (true) {
            send(String.valueOf(offset));
            byte[] buffer = new byte[socket_buf];
            int count = input.read(buffer);
            String check = new String(buffer);
            if (check.contains("FileEnding")) {
                System.out.println("Sever: File downloaded");
                send("FileSaved");
                long time = System.currentTimeMillis() - timeStart;
                Long speed = offset / time;
                System.out.println("Speed: " + speed + " kb/s");
                afc.close();
                break;
            }
            ArrayList<Byte> bytes = new ArrayList<>();
            int j = 0;
            for (int i=0; i<count; i++){
                if(buffer[i] == -128 && i+3 <= count){
                    if(buffer[i+1]== -127 && buffer[i+2] == 127 && buffer[i+3] == 126)
                        System.out.println("Sever: offset: " + offset);
                }
                else {
                    bytes.add(buffer[i]);
                }
            }
            byte[] tempBuf = new byte[bytes.size()];
            for (int i = 0; i<bytes.size();i++){
                tempBuf[i] = bytes.get(i);
            }
            afc.write(ByteBuffer.wrap(tempBuf), offset);
            offset += count;
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
