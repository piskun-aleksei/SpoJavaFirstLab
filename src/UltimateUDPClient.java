import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class UltimateUDPClient extends UDPBasicConnector {

    private BufferedReader user;
    private String currentFilename;
    private Long currentFileSize;
    private List<CustomPacket> packets;

    public UltimateUDPClient() {
        super(RECEIVER_PORT);
        user = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start() {
        loop:
        while (true) {
            //System.out.println("1 - to set address of a client");
            //System.out.println("2 - wait for a file");
            String line;
            /*try {
                line = user.readLine();
                if (line.equals("")) continue;
            } catch (IOException e) {
                continue;
            }*/
            line = "2";
            switch (line.charAt(0)) {
                case '1':
                    try {
                        System.out.println("Clint's IP address: ");
                        String ip = user.readLine();
                        if (ip.equals("")) return;
                        InetAddress inetAddress = InetAddress.getByName(ip);
                        Integer port = SENDER_PORT;
                        setRemote(inetAddress, port);
                    } catch (IOException | NumberFormatException ignored) {
                    }
                    break;
                case '2':
                    waitForUpload();
                    break;
                case '3':
                    break loop;
            }
        }
    }



    private void uploadingStartHandler(CustomPacket packet) throws IOException {
        String fileParams = packet.getDataAsString();
        int delimiterIndex = fileParams.indexOf(" ");
        String filename = "uploaded_" + fileParams.toLowerCase().substring(0, delimiterIndex).trim();
        long size = Long.valueOf(fileParams.substring(delimiterIndex).trim());
        File file = new File(filename);
        if (!file.exists()) {
            currentFilename = filename;
            currentFileSize = size;
            packets = new ArrayList<>();
            sendMissingPacketsNumbers();
        } else {
            if (currentFilename != null && currentFilename.equals(filename)) {
                sendMissingPacketsNumbers();
            } else {
                System.out.println("Server: File exists");
                send(IMPORTANT_PACKET_IDENTIFICATOR, "File exists");
            }
        }
    }
    private void waitForUpload() {
        while (true) {
            try {
                CustomPacket packet = receive();
                setRemote(packet.getPacket().getAddress(), SENDER_PORT);
                long code = packet.getNumber();
                if (code == REGULAR_PACKET_IDENTIFICATOR) {
                    //System.out.println(">>> " + packet.getDataAsString());
                    uploadingStartHandler(packet);
                } else if (code == IMPORTANT_PACKET_IDENTIFICATOR) {
                    //System.out.println(">>> " + packet.getDataAsString());
                    uploadingEndingHandler(packet);
                } else {
                    //System.out.println(">>> packet#" + packet.getNumber());
                    packetHandler(packet);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadingEndingHandler(CustomPacket packet) throws IOException {
        sendMissingPacketsNumbers();
    }

    private void packetHandler(CustomPacket packet) throws IOException {
        packets.add(packet);
    }

    private void sendMissingPacketsNumbers() throws IOException {
        long totalPacketCount = currentFileSize / BUFFER_SIZE;
        if (currentFileSize % BUFFER_SIZE != 0) totalPacketCount++;
        List<Long> numbers = new ArrayList<>();
        long count = 0;
        for (long i = 0; i < totalPacketCount; i++) {
            if (!isUploaded(i)){
                numbers.add(i);
                count++;
            }
            if(count >= (BUFFER_SIZE / 8)){
                break;
            }
        }
        if (!numbers.isEmpty()) {
            System.out.print("Server: Missing packets: ");
            for(Long number : numbers) {
                System.out.print("{" + number);
                if(numbers.indexOf(number) != numbers.size() - 1) System.out.print("}; ");
                else System.out.println("}.");
            }
            send(numbers);
        } else {
            //System.out.println("Server: File was uploaded");
            send(IMPORTANT_PACKET_IDENTIFICATOR, "File was uploaded");
            createFile();
        }
    }

    private void deleteUnwantedPackets() {
        List<CustomPacket> deletedPackets = new ArrayList<>();
        for (int i = 0; i < packets.size(); i++)
            for (int j = i + 1; j < packets.size(); j++)
                if (packets.get(j).getNumber() == packets.get(i).getNumber()) deletedPackets.add(packets.get(j));
        packets.removeAll(deletedPackets);
    }

    private void createFile() throws IOException {
        File file = new File(currentFilename);
        if(!file.exists()) file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        deleteUnwantedPackets();
        Collections.sort(packets, (o1, o2) -> (int)(o1.getNumber() - o2.getNumber()));
        for (CustomPacket packet : packets) {
            byte[] data = packet.getData();
            fos.write(data);
        }
        fos.close();
        currentFileSize = null;
        currentFilename = null;
        packets = null;
    }

    private boolean isUploaded(long packetNumber) {
        for (CustomPacket packet : packets) if (packet.getNumber() == packetNumber) return true;
        return false;
    }


}
