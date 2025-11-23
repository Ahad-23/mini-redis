import java.io.*;
import java.net.Socket;
public class MiniRedisCli {
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", 6379);
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        System.out.println(in.readLine()); // welcome

        while (true) {
            System.out.print("> ");
            String cmd = console.readLine();
            out.write(cmd + "\r\n");
            out.flush();
            System.out.println(in.readLine());
        }
    }
}
