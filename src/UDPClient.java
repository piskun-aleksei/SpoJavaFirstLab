import java.io.*;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Random;


public class UDPClient extends BasicUDPConnector {

    private BufferedReader user;

    public UDPClient() {
        super( SENDER_PORT);
        user = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start() {
        try {
            System.out.println("Enter IP to upload file to");
            String ip = user.readLine();
            if (ip.equals("")) return;
            InetAddress inetAddress = InetAddress.getByName(ip);
            Integer port = RECEIVER_PORT;
            setRemote(inetAddress, port);
            while (true) {
                System.out.println("Enter file name:");
                String filename = readLineFromUser();
                try {
                    upload(filename);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readLineFromUser() {
        try {
            return user.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private void upload(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("No such file");
            return;
        }
        Date startTime = new Date();
        CustomPacket packet;
        int retryCount = 0;
        while (true) {
            String fileParams = filename + " " + file.length();
            send(fileParams);
            System.out.println("Client: " + fileParams);
            try {
                packet = receive( LOW_TIMEOUT);
                break;

            } catch (SocketTimeoutException e) {
                retryCount++;
                if(retryCount == RETRY_COUNT) {
                    System.out.println("No response from receiver");
                    return;
                }
            }
        }
        while (true) {
            if (packet.getNumber() ==  IMPORTANT_PACKET) {
                System.out.println("Server: " + packet.getDataAsString());
                double speed = (double)(((new Date()).getTime() - startTime.getTime()) * 1000) == 0
                        ? Double.MAX_VALUE
                        :(double) (file.length() * 8) / (double)(((new Date()).getTime() - startTime.getTime()) * 1000);
                System.out.println("Speed: " + speed + " Mbps");
                return;
            }
            long[] packetNumbers = packet.getDataAsLongArray();
            System.out.print("Server: Lost packets: ");
            for(int i = 0; i < packetNumbers.length; i++) {
                System.out.print("{" + packetNumbers[i] + "}");
                if(i != packetNumbers.length - 1) System.out.print(", ");
                else System.out.println(";");
            }
            RandomAccessFile fileReader = new RandomAccessFile(file, "r");
            for (long packetNumber : packetNumbers) {
                fileReader.seek(packetNumber *  BUFFER_SIZE);
                byte[] bytes = new byte[ BUFFER_SIZE];
                int countBytes = fileReader.read(bytes);
                Random rand = new Random();
                Double number = rand.nextDouble();
                if(number > 0.2) {

                    send(packetNumber, bytes, countBytes);
                }
            }
            fileReader.close();
            retryCount = 0;
            while (true) {
                System.out.println("Client: Last packet was sent");
                send( IMPORTANT_PACKET, "Last packet was sent");
                try {
                    packet = receive( LOW_TIMEOUT);
                    break;
                } catch (SocketTimeoutException e) {
                    retryCount++;
                    if(retryCount == RETRY_COUNT) {
                        System.out.println("No response from receiver");
                        return;
                    }
                }
            }
        }
    }
}
