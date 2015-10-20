import java.io.*;
import java.net.*;

public class WebProxy {
    public static void main(String[] args) {
        System.out.println("Listening on port: " +  args[0]);
        int portNumber = Integer.valueOf(args[0]);
        run(portNumber);
    }

    public static void run(int port) {

        try {
            ServerSocket connection = new ServerSocket(port);
            byte[] req = new byte[1024];
            byte[] reply = new byte[4096];


            while(true) {
                Socket client = null;
                Socket server = null;

                // accept an incoming request from the browser
                client = connection.accept();

                // initialize an input and output stream for the client
                final InputStream clientIn = client.getInputStream();
                final OutputStream clientOut = client.getOutputStream();


                StringBuilder builder = new StringBuilder();
                InputStreamReader reader = new InputStreamReader(clientIn);
                BufferedReader br = new BufferedReader(reader);
                String msg = br.readLine();

                while(msg != null) {
                    builder.append(msg);
                    builder.append(" ");
                    msg = br.readLine();
                }

                String str = builder.toString();

                final int HTTP_LENGTH = 7;
                System.out.println("Request: " + str + "\n");
                String[] request = str.split(" ");
                System.out.println("URL: " + request[1]);
                System.out.println("Host Name: " + request[4]);
                String hostName = request[4];

                server = new Socket(hostName, 80);
                String dir = "";
                dir = request[1].substring(HTTP_LENGTH + hostName.length());
                System.out.println("Directory: " + dir);

                PrintWriter serv = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
                System.out.println("Sending get request to: " + url);
                serv.println("GET " + dir + " HTTP/1.1");
                for(int i = 0; i < httpreq.size(); i++) {

                    serv.println(httpreq.get(i));
                }
                serv.println();
                serv.flush();


            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
