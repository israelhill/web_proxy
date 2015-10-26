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

        try {
            byte[] req = new byte[2048];

            // initialize an input and output stream for the client
            final InputStream clientIn = connectionSocket.getInputStream();
            final OutputStream clientOut = connectionSocket.getOutputStream();

            // read the HTTP request from the client. Store in req array
            int requestLength = clientIn.read(req);
            if(requestLength > 0) {
                //Store the HTTP Request as a string
                String str = new String(req, 0, requestLength);

                final int HTTP_LENGTH = 7;
                String replace = str.replace("\n", " ");
                //Split the request at spaces
                String[] request = replace.split(" ");
                String hostName = request[4].trim();

                //Create a new socket to talk to the host
                server = new Socket(hostName, 80);
                //get the directory of the content
                String dir = request[1].substring(HTTP_LENGTH + hostName.length());
                System.out.println("Directory: " + dir);

                //write request to the host via server socket
                OutputStream serverOut = server.getOutputStream();
                serverOut.write(req, 0, requestLength);
                String requestToHost = new String(req, 0, requestLength);
                serverOut.flush();
                System.out.println("REQUEST TO HOST: " + "\n" + requestToHost);

                //copy response from the host via server socket
                InputStream responseStream = server.getInputStream();
                int length;

                // read HTTP response from the server socket and write it to the
                // client socket so it can be passed back to the browser
                try {
                    while((length = responseStream.read(req)) != -1) {
                        clientOut.write(req, 0, length);
                        clientOut.flush();
                    }
                }
                catch(IOException e) {

                }

                // close sockets and streams
                clientIn.close();
                clientOut.close();
                serverOut.close();
                responseStream.close();
            }
            else {
                clientOut.close();
            }
        }
        catch (IOException e) {
            //e.printStackTrace();
        }
    }
}
