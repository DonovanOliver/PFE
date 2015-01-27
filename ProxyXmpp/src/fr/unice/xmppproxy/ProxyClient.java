
package fr.unice.xmppproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import android.os.StrictMode;
import android.util.Log;

/**
 * This class is listening to gtalk client on port 8000 to xmpp requests  
 * @author bouabid
 */
public class ProxyClient implements Runnable {

    private ServerSocket serverSocket;
    private Socket client;
    private boolean stop = false;
    private InputStream is;
    private PrintWriter pw;
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    public ProxyClient(int port) {
        try {
        	
        	StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        	StrictMode.setThreadPolicy(policy); 
            serverSocket = new ServerSocket(port);
            Log.d("myapps","[InputSocket > InputSocket ] "+"1");
            client = serverSocket.accept();
            
            is = client.getInputStream();
            
            OutputStream os = client.getOutputStream();
            
            pw = new PrintWriter(new OutputStreamWriter(os), true);
            Log.d("myapps","[InputSocket > constructor] 2");
        } catch (Exception ex) {
            Log.d("myapps","[InputSocket > constructor] " +
                    "Exception " + ex + " launched"+ex);
        }
    }

    public void run() {
        String readedData = "";
        int maxbytes = 4096;
        byte[] b = new byte[maxbytes];
        try {
            while(!stop) {
                
            	int bytes = 0;
                //Thread.sleep0);
                if ((bytes = is.read(b)) > 0) {
                	
                	if (bytes<maxbytes) {
                        b = Arrays.copyOf(b, bytes);
                        readedData += new String(b);
                        //Log.d("myapps","[InputSocket > run ] debug "+readedData);    
                        if  (XMLcheck(readedData) == 0)
                        	{
                        	  // Log.d("myapps",ANSI_RED + readedData + ANSI_RESET);
                        	 	continue;
                        	}
                       
                        ProxyManger.input(readedData);
                        
                    } else {
                    	
                    	readedData += new String(b);
                    }
                } else if (!readedData.equals("")) {
                    ProxyManger.input(readedData);
                    Log.d("myapps","[InputSocket > run 3] "+readedData);
                }
                readedData = "";
               
            }
        } catch (Exception ex2) {
/*            System.err.println("[InputSocket > run] " +
                    "Exception 3" + ex2 + " launched");*/
        	ex2.printStackTrace();
        } finally {
            ProxyManger.exit();
        }
    }

    public void write(String output) {
        pw.println(output);
    }

    public void stop() throws IOException {
        stop = true;
        if (client!=null && !client.isClosed()) client.close();
        if (serverSocket!=null && !serverSocket.isClosed()) serverSocket.close();
    }
    
    public int XMLcheck(String readedData)  {
    	
    
    	if  ((readedData.startsWith("<?xml") || (readedData.startsWith("<stream:stream"))) && (readedData.endsWith(">")))
    	{
    		//Log.d("myapps",ANSI_RED + "TAG : 1 VALIDE " + ANSI_RESET);
    		return 1;
    	}
    if  (!readedData.endsWith(">") || (!readedData.contains(" ")))
	{
    	//Log.d("myapps",ANSI_RED + "TAG : 2 INVALIDE "+ readedData + ANSI_RESET);
	 	return 0;
	}
    
    if  (readedData.endsWith("/presence>") && (readedData.startsWith("<presence")))
	{
    	//Log.d("myapps",ANSI_RED + "TAG : VALIDE "+ readedData + ANSI_RESET);
	 	return 1;
	}
    
    if(!readedData.endsWith(readedData.substring(1,readedData.indexOf(" "))+">"))
    	{
    	  //Log.d("myapps",ANSI_RED + "TAG : 3 INVALIDE "+ readedData + ANSI_RESET);
    	  return 0;
    	}
    
    
    return 1 ;
    }

}
