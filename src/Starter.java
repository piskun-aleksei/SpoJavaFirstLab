import java.util.Scanner;

public class Starter {
    public static void main(String... args){
        boolean choiceIsCorrect = false;

        System.out.println("Choose your destiny: 1 for TCP, 2 for UDP ");
        while(!choiceIsCorrect) {
            int i = Integer.parseInt(new Scanner(System.in).nextLine());
            if(i == 1) {
                BasicConnector connector = null;
                System.out.println("Choose your destiny: 1 for server, 2 for client ");
                i = Integer.parseInt(new Scanner(System.in).nextLine());
                if (i == 1) {
                    connector = new TCPServer();
                    choiceIsCorrect = true;
                    connector.startup();
                }
                if (i == 2) {
                    connector = new TCPClient();
                    choiceIsCorrect = true;
                    connector.startup();
                }
                if (i != 1 && i != 2) {
                    System.out.println("Please, specify correct number");
                }
            }
            if(i == 2 && !choiceIsCorrect){

                System.out.println("Choose your destiny: 1 for server, 2 for client ");
                i = Integer.parseInt(new Scanner(System.in).nextLine());
                if (i == 1) {
                    UDPServer connector = new UDPServer();
                    choiceIsCorrect = true;
                    connector.start();
                }
                if (i == 2) {
                    UDPClient connector = new UDPClient();
                    choiceIsCorrect = true;
                    connector.start();
                }
                if (i != 1 && i != 2) {
                    System.out.println("Please, specify correct number");
                }
            }
        }

    }
}
