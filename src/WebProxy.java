import java.io.IOException;
import java.net.ServerSocket;

public class WebProxy {

    public static void main(String[] args) {
        ServerSocket connection = null;
        final int PORT = Integer.valueOf(args[0]);
        System.out.println("Listening on port " + PORT);

        try {
            connection = new ServerSocket(PORT);
            System.out.println("Listening for requests from browser");
        }
        catch(IOException e) {
            System.out.println("An error occurred.");
            // Program failed. Exit
            System.exit(-1);
        }

        while(true) {
            try {
                new WebProxyThread(connection.accept()).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
