import java.io.*;
import java.net.*;

public class WebProxyThread extends Thread {
    Socket connectionSocket;

    public WebProxyThread(Socket socket) {
        this.connectionSocket = socket;
    }

    //Overrides run() method from Java's Thread
    @Override
    public void run() {
        Socket server = null;
        final int BUFFER_SIZE = 2048;
        final int HTTP_LENGTH = 7;

        try {
            byte[] req = new byte[2048];

            // initialize an input and output stream for the client
            final InputStream clientIn = connectionSocket.getInputStream();
            final OutputStream clientOut = connectionSocket.getOutputStream();

            StringBuilder builder = new StringBuilder();
            InputStreamReader reader = new InputStreamReader(clientIn);
            BufferedReader br = new BufferedReader(reader);
            String msg;
            String close = "Connection: close";
            String absoluteURL = "";
            String host = "";
            String directory = "";

            while((msg = br.readLine()) != null && !msg.isEmpty()) {
                if(msg.contains("keep-alive")) {
                    builder.append(close);
                    builder.append("\r\n");
                }
                else if(msg.contains("GET")) {
                    String[] split = msg.split(" ");
                    absoluteURL = split[1];
                }
                else if(msg.contains("Host:")) {
                    String[] split = msg.split(" ");
                    host = split[1];
                    builder.append(msg);
                    builder.append("\r\n");
                }
                else {
                    builder.append(msg);
                    builder.append("\r\n");
                }
            }
            builder.append("\r\n");

            System.out.println("HOST: " + host);
            directory = absoluteURL.substring(HTTP_LENGTH + host.length());
            builder.insert(0, ("GET " + directory + " HTTP/1.1" + "\r\n"));

            String strRequest = builder.toString();
            byte[] arr = strRequest.getBytes();

            server = new Socket(host, 80);

            //write request to the host via server socket
            OutputStream serverOut = server.getOutputStream();
            serverOut.write(arr);

            String requestToHost = new String(arr);
            serverOut.flush();
            System.out.println("REQUEST TO HOST: " + "\n" + requestToHost);

            //copy response from the host via server socket
            InputStream responseStream = server.getInputStream();

            int res = responseStream.read();
            try {
                while(res != -1) {
                    clientOut.write(res);
                    clientOut.flush();
                    res = responseStream.read();
                }
            }
            catch(IOException e) {
                System.out.println("ERROR:" + "\n");
                e.printStackTrace();
            }

            // close connections
            serverOut.close();
            responseStream.close();
            clientOut.close();
            clientIn.close();

            server.close();
        }
        catch (IOException e) {
            System.out.println("ERROR:" + "\n");
            e.printStackTrace();
        }
        finally {
            try {
                System.out.println("Closing connection socket.");
                connectionSocket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
