import java.io.*;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Date;


public class UltimateUDPServer extends UDPBasicConnector {

    private BufferedReader user;

    public UltimateUDPServer() {
        super(SENDER_PORT);
        user = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start() {
        System.out.println("Enter IP address to send to: ");
        String ip;
        try {
            ip = user.readLine();
            InetAddress inetAddress = InetAddress.getByName(ip);
            Integer port = RECEIVER_PORT;
            setRemote(inetAddress, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            System.out.println("Enter file name to upload: ");
            String filename = listenToCommand();
            try {
                upload(filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void upload(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("No such file found");
            return;
        }
        Date startTime = new Date();
        CustomPacket packet;
        int retryCount = 0;
        while (true) {
            String fileParams = filename + " " + file.length();
            send(fileParams);
            //System.out.println("<<< " + fileParams);
            try {
                packet = receive(LOW_TIMEOUT);
                break;
            } catch (SocketTimeoutException e) {
                retryCount++;
                if(retryCount == MAX_RETRY_COUNT) {
                    System.out.println("No response from receiver");
                    return;
                }
            }
        }
        while (true) {
            if (packet.getNumber() == IMPORTANT_PACKET_IDENTIFICATOR) {
                //System.out.println(">>> " + packet.getDataAsString());
                double speed = (double)(((new Date()).getTime() - startTime.getTime()) * 1000) == 0
                        ? Double.MAX_VALUE
                        :(double) (file.length() * 8) / (double)(((new Date()).getTime() - startTime.getTime()) * 1000);
                System.out.println("Speed: " + speed + " Mbits per second");
                return;
            }
            long[] packetNumbers = packet.getDataAsLongArray();
            System.out.print("Server: No packets ");
            for(int i = 0; i < packetNumbers.length; i++) {
                System.out.print("{"+packetNumbers[i]);
                if(i != packetNumbers.length - 1) System.out.print("}, ");
                else System.out.println("}.");
            }
            RandomAccessFile fileReader = new RandomAccessFile(file, "r");
            for (long packetNumber : packetNumbers) {
                fileReader.seek(packetNumber * BUFFER_SIZE);
                byte[] bytes = new byte[BUFFER_SIZE];
                int countBytes = fileReader.read(bytes);
                send(packetNumber, bytes, countBytes);
                //System.out.println("<<< packet#" + packetNumber);
            }
            fileReader.close();
            retryCount = 0;
            while (true) {
                //System.out.println("Client: Last packet was sent");
                send(IMPORTANT_PACKET_IDENTIFICATOR, "Last packet was sent");
                try {
                    packet = receive(LOW_TIMEOUT);
                    break;
                } catch (SocketTimeoutException e) {
                    retryCount++;
                    if(retryCount == MAX_RETRY_COUNT) {
                        System.out.println("No response from receiver");
                        return;
                    }
                }
            }
        }
    }

    private String listenToCommand() {
        try {
            return user.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }


}
