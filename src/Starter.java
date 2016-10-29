import java.io.IOException;
import java.util.Scanner;

public class Starter {
    public static void main(String... args){
        boolean choiceIsCorrect = false;
        BasicConnector connector = null;
        СокетХуёкет маккуин = new СокетХуёкет(1337, "ЕЕЕЕЕ БАТЬ МОЙ ХУЙ");
        System.out.println(маккуин.гдеСкорость());
        System.out.println(маккуин.аеКЧАУ());
        System.out.println("Choose your destiny: 1 for server, 2 for client ");
        while(!choiceIsCorrect) {
            int i = Integer.parseInt(new Scanner(System.in).nextLine());
            if (i == 1) {
                connector = new Server();
                choiceIsCorrect = true;
            }
            if (i == 2) {
                connector = new Client();
                choiceIsCorrect = true;
            }
            if (i != 1 && i != 2) {
                System.out.println("Please, specify correct number");
            }
        }
        connector.startup();
    }
}
