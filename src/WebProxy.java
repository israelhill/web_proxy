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
                Socket client;
                Socket servercd;

                // accept an incoming request from the browser
                client = connection.accept();

                // initialize an input and output stream for the client
                final InputStream clientIn = client.getInputStream();
                final OutputStream clientOut = client.getOutputStream();


                StringBuilder builder = new StringBuilder();
                int len = clientIn.read(req);
                if(len > 0) {

                    String str;
                    str = new String(req, 0, len);

                    final int HTTP_LENGTH = 7;
                    System.out.println("REQUEST: " + "\n" + str + "\n");
                    String replace = str.replace("\n", " ");
                    String[] request = replace.split(" ");

                    System.out.println("URL: " + request[1]);
                    String hostName = request[4].trim();
                    System.out.println("Host name: " + hostName);


                    server = new Socket(hostName, 80);
                    String dir = request[1].substring(HTTP_LENGTH + hostName.length());
                    System.out.println("Directory: " + dir);

                    //write request
                    OutputStream serverOut = server.getOutputStream();
                    serverOut.write(req, 0, len);

                    //copy response
                    InputStream outGoingStream = server.getInputStream();
                    for(int length; (length = outGoingStream.read(req)) != -1;) {
                        clientOut.write(req, 0, length);
                    }

                    clientIn.close();
                    clientOut.close();
                    serverOut.close();
                    outGoingStream.close();

                    server.close();

                }
                else {
                    clientOut.close();
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
