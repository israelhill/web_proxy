import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;

public class proxyd {

    public static void main(String[] args) {
        ServerSocket connection = null;
        final int PORT = Integer.valueOf(args[1]);
        System.out.println("Listening on port " + PORT);

        try {
            // create a socket listening on the specified port
            connection = new ServerSocket(PORT);
            System.out.println("Listening for requests from browser");
        }
        catch(IOException e) {
            System.out.println("An error occurred.");
            // Program failed. Exit
            System.exit(-1);
        }

        // listen on this port forever
        while(true) {
            try {
                new WebProxyThread(connection.accept()).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class WebProxyThread extends Thread {
        Socket connectionSocket;
        static Hashtable<String, InetAddress> dnsCache = new Hashtable<>();

        public WebProxyThread(Socket socket) {
            this.connectionSocket = socket;
        }

        //Overrides run() method from Java's Thread
        @Override
        public void run() {
            Socket server = null;
            final int HTTP_LENGTH = 7;

            try {
                // initialize an input and output stream for the client
                final InputStream clientIn = connectionSocket.getInputStream();
                final OutputStream clientOut = connectionSocket.getOutputStream();

                // read the request from the client
                StringBuilder builder = new StringBuilder();
                InputStreamReader reader = new InputStreamReader(clientIn);
                BufferedReader br = new BufferedReader(reader);

                String msg;
                String close = "Connection: close";
                String absoluteURL = "";
                String host = "";
                String directory = "";

                // modify the request. remove keep-alive to disallow persistent connections
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

                // get directory from the url
                directory = absoluteURL.substring(HTTP_LENGTH + host.length());
                // add GET line back to request with relative path
                builder.insert(0, ("GET " + directory + " HTTP/1.1" + "\r\n"));

                String strRequest = builder.toString();
                byte[] requestBytes = strRequest.getBytes();

                // get the host address from the DNS cache
                // create socket to talk to the host
                InetAddress hostAddress = checkCache(host);
                server = new Socket(hostAddress, 80);

                //write request to the host via server socket
                OutputStream serverOut = server.getOutputStream();
                serverOut.write(requestBytes);

                String requestToHost = new String(requestBytes);
                serverOut.flush();

                //get response from the host via server socket
                InputStream responseStream = server.getInputStream();

                // write response back to the client
                int res = responseStream.read();
                try {
                    while(res != -1) {
                        clientOut.write(res);
                        clientOut.flush();
                        res = responseStream.read();
                    }
                }
                catch(IOException e) {
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
                e.printStackTrace();
            }
            finally {
                try {
                    connectionSocket.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Returns the host address if it exists in the cache
         * else add it to the cache then return the address
         * @param hostName
         * @return
         */
        public InetAddress checkCache(String hostName) throws UnknownHostException {
            // the cache has the address of the host already. Return it.
            if(dnsCache.contains(hostName)) {
                return dnsCache.get(hostName);
            }
            else {
                // add the DNS record to the cache
                InetAddress hostAddress = InetAddress.getByName(hostName);
                dnsCache.put(hostName, hostAddress);

                // remove this record after 30 seconds
                new CacheTimeoutThread(hostName).start();
                return hostAddress;
            }
        }


        /**
         * Creates a thread that will delete DNS records from hash table.
         * Thread will sleep for 30 seconds, awaken, then delete the record.
         */
        private static class CacheTimeoutThread extends Thread {
            String hostName;
            final int THIRTY_SECONDS = 30000;

            public CacheTimeoutThread(String hostName) {
                this.hostName = hostName;
            }

            @Override
            public void run() {
                // delay deletion for 30 seconds
                try {
                    Thread.sleep(THIRTY_SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                finally {
                    // remove the record form the cache
                    dnsCache.remove(hostName);
                }
            }
        }
    }
}
