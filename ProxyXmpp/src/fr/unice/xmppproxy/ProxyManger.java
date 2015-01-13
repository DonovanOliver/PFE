
package fr.unice.xmppproxy;

import fr.unice.xmppproxy.filters.Filters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.util.Log;

/**
 * Manger Class  
 * @author bouabid
 */
public class ProxyManger {

    private static ProxyClient inputSocket;
    private static ProxyServer outputSocket;
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static int cmpt = 0;

    public static void launch() {

        Filters.readFilters();
        inputSocket = new ProxyClient(8000);
        String xmppServer;
        xmppServer = "64.233.167.125";
        outputSocket = new ProxyServer(xmppServer, 5222);
        new Thread(inputSocket).start();
        new Thread(outputSocket).start();
    }

    public static synchronized void input(String raw) {
    	 //Log.i("myapps","--->  "+ raw);
    	String filtered = Filters.processClient(raw);
		//outputSocket.write(filtered);
		debug("client", raw, filtered);
    }

    public static synchronized void output(String raw) {
        String filtered = Filters.processServer(raw);
        inputSocket.write(filtered);
        debug("server", raw, filtered);
    }

    public static synchronized void debug(String type, String raw, String filtered) {
		 String output = "";
		
		 if (!filtered.equals(raw)) 
		 {
		     output += "[" + type + "(r)]\n" + raw;
		 }
		 output += "\n[" + type + "]\n" + filtered + "\n";
		 //appendLog(output);
		
     }

    public static synchronized void exit() {
        try {
            if (inputSocket != null) inputSocket.stop();
            if (outputSocket != null) outputSocket.stop();
            System.exit(0);
        } catch (IOException ex) {
            Log.d("myapps","[Manager > exit]" +
                    "Sockets stop process failed, IOException launched");
        }
    }
    
    public static void appendLog(String text)
	{       
	   File logFile = new File("sdcard/log.txt");
	   if (!logFile.exists())
	   {
	      try
	      {
	         logFile.createNewFile();
	      } 
	      catch (IOException e)
	      {
	         // TODO Auto-generated catch block
	         e.printStackTrace();
	      }
	   }
	   try
	   {
	      //BufferedWriter for performance, true to set append to file flag
	      BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
	      buf.append(text);
	      buf.newLine();
	      buf.close();
	   }
	   catch (IOException e)
	   {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
	   }
	}
	

}