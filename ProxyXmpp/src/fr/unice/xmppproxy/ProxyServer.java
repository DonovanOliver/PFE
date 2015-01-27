
package fr.unice.xmppproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

import android.util.Log;

/**
 * This class is running a server socket to gtalk server to xmpp requests  
 * @author bouabid
 */
public class ProxyServer implements Runnable {

    private Socket socket;
    private boolean stop = false;
    private InputStream is;
    private PrintWriter pw;

    public ProxyServer(String ip, int port) {
        try {
        	Log.d("myapps","[OutputSocket > OutputSocket ] "+"1");
            socket = new Socket(ip, port);
            Log.d("myapps","[OutputSocket > OutputSocket ] "+"2");
            is = socket.getInputStream();
            Log.d("myapps","[OutputSocket > OutputSocket ] "+"3");
            OutputStream os = socket.getOutputStream();
            pw = new PrintWriter(new OutputStreamWriter(os), true);
        } catch (Exception ex) {
            Log.d("myapps","[OutputSocket > constructor] " +
                    "Exception " + ex.getClass().getSimpleName() + " launched");
        }
    }

    public void run() {
        String readedData = "";
        try {
            while(!stop) {
                int maxbytes = 1024;
                byte[] b = new byte[maxbytes];
                int bytes = 0;
                if ((bytes = is.read(b)) != -1) {
                    if (bytes<maxbytes) {
                        b = Arrays.copyOf(b, bytes);
                        readedData += new String(b);
                        ProxyManger.output(readedData);
                        readedData = "";
                    } else readedData += new String(b);
                } else if (!readedData.equals("")) {
                    ProxyManger.output(readedData);
                    readedData = "";
                }
            }
        } catch (Exception ex) {
             java.util.logging.Logger.getLogger(ProxyServer.class.getName()).log(
                    java.util.logging.Level.SEVERE, null, ex);
            Log.d("myapps","[OutputSocket > run] " +
                    "Exception " + ex );
        } finally {
            ProxyManger.exit();
        }
    }

    public void write(String output) {
        pw.println(output);
    }

    public void stop() throws IOException {
        stop = true;
        if (!socket.isClosed()) socket.close();
    }

}
