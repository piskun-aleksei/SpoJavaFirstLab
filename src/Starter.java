import java.util.Scanner;

public class Starter {
    public static void main(String... args){
        boolean choiceIsCorrect = false;
        BasicConnector connector = null;
        System.out.println("Choose your destiny: 1 for TCP, 2 for UDP ");
        while(!choiceIsCorrect) {
            int i = Integer.parseInt(new Scanner(System.in).nextLine());
            if(i == 1) {
                System.out.println("Choose your destiny: 1 for server, 2 for client ");
                i = Integer.parseInt(new Scanner(System.in).nextLine());
                if (i == 1) {
                    connector = new TCPServer();
                    choiceIsCorrect = true;
                }
                if (i == 2) {
                    connector = new TCPClient();
                    choiceIsCorrect = true;
                }
                if (i != 1 && i != 2) {
                    System.out.println("Please, specify correct number");
                }
            }
            if(i == 2 && !choiceIsCorrect){
                System.out.println("Choose your destiny: 1 for server, 2 for client ");
                i = Integer.parseInt(new Scanner(System.in).nextLine());
                if (i == 1) {
                    connector = new NewUDPServer();
                    choiceIsCorrect = true;
                }
                if (i == 2) {
                    connector = new NewUDPClient();
                    choiceIsCorrect = true;
                }
                if (i != 1 && i != 2) {
                    System.out.println("Please, specify correct number");
                }
            }
        }
        connector.startup();
    }
}
