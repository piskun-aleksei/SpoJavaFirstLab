import java.io.*;
import java.net.InetAddress;
import java.util.*;


public class UDPServer extends BasicUDPConnector {
    private BufferedReader user;
    private String currentFilename;
    private Long currentFileSize;
    private Map<Long,CustomPacket> packets;

    public UDPServer() {
        super(RECEIVER_PORT);
        user = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start() {
        listen();
    }

    private void listen() {
        while (true) {
            try {
                CustomPacket packet = receive();
                setRemote(packet.getPacket().getAddress(), packet.getPacket().getPort());
                long code = packet.getNumber();
                if (code == REGULAR_PACKET) {
                    System.out.println("Client: " + packet.getDataAsString());
                    onStartUploading(packet);
                } else if (code == IMPORTANT_PACKET) {
                    System.out.println("Client: " + packet.getDataAsString());
                    onEOFMessage(packet);
                } else {
                    onPacket(packet);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void onStartUploading(CustomPacket packet) throws IOException {
        String fileParams = packet.getDataAsString();
        int delimiterIndex = fileParams.indexOf(" ");
        String filename = "uploaded_" + fileParams.toLowerCase().substring(0, delimiterIndex).trim();
        long size = Long.valueOf(fileParams.substring(delimiterIndex).trim());
        File file = new File(filename);
        if (!file.exists()) {
            currentFilename = filename;
            currentFileSize = size;
            packets = new TreeMap<>();
            sendMissingPacketsNumbers();
        } else {
            if (currentFilename != null && currentFilename.equals(filename)) {
                sendMissingPacketsNumbers();
            } else {
                System.out.println("Server: File exists");
                send(IMPORTANT_PACKET, "File exists");
            }
        }
    }

    private void onEOFMessage(CustomPacket packet) throws IOException {
        sendMissingPacketsNumbers();
    }

    private void onPacket(CustomPacket packet) throws IOException {
        packets.put(packet.getNumber(), packet);
    }

    private void sendMissingPacketsNumbers() throws IOException {
        long totalPacketCount = currentFileSize / BUFFER_SIZE;
        if (currentFileSize % BUFFER_SIZE != 0) totalPacketCount++;
        long packetsNumber = 0;
        List<Long> numbers = new ArrayList<>();
        for (long i = 0; i < totalPacketCount; i++) {
            if (!packetIsUploaded(i)) {
                if(packetsNumber <= (BUFFER_SIZE/8)) {
                    numbers.add(i);
                    packetsNumber++;
                }
                 else {
                     break;
                }
            }
        }
        if (!numbers.isEmpty()) {
            System.out.print("Server: Missing packets: ");
            for (Long number : numbers) {
                System.out.print("{" + number + "}");
                if (numbers.indexOf(number) != numbers.size() - 1) System.out.print(", ");
                else System.out.println(";");
            }
            send(numbers);
        } else {
            System.out.println("Server: File was uploaded");
            send(IMPORTANT_PACKET, "File was uploaded");
            createFile();
        }
    }

    private void createFile() throws IOException {
        File file = new File(currentFilename);
        if (!file.exists()) file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);


        for (CustomPacket packet : packets.values()) {
            byte[] data = packet.getData();
            fos.write(data);
        }
        fos.close();
        currentFileSize = null;
        currentFilename = null;
        packets = null;
    }

    private boolean packetIsUploaded(long packetNumber) {
        if(packets.containsKey(packetNumber)){
            return true;
        }
        return false;
    }

}
