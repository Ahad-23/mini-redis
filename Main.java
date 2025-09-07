import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        MiniRedis redis = new MiniRedis();
        Scanner scanner = new Scanner(System.in);

        System.out.println("MiniRedis started, Type commands (SET, GET, DEL). Type EXIT to quit.");
        while (true) {
            System.out.print("mini-redis> ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("EXIT")) break;
            System.out.println(redis.execute(input));
        }
    }
}
