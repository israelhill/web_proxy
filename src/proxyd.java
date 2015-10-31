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


                InetAddress hostAddress = checkCache(host);
                server = new Socket(hostAddress, 80);

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
                InetAddress hostAddress = InetAddress.getByName(hostName);
                dnsCache.put(hostName, hostAddress);

                // remove this record after 30 seconds
                new CacheTimeoutThread(hostName).start();
                return hostAddress;
            }
        }


        /**
         * Creates a thread that will delete dns records from has table.
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
