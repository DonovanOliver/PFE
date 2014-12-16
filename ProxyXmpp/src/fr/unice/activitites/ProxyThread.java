package fr.unice.activitites;


import android.util.Log;

import java.net.*;
import java.util.Arrays;
import java.io.*;



public class ProxyThread extends Thread {

    protected class StreamCopyThread extends Thread {
	private Socket inSock;
	private Socket outSock;
	private boolean done=false;
	private StreamCopyThread peer;

	public StreamCopyThread(Socket inSock, Socket outSock) {
	    this.inSock=inSock;
	    this.outSock=outSock;
	}

	public void run() {
	    byte[] buf=new byte[bufSize];
	    int count=-1;
	    int logcount=0;
	    try {
		InputStream in=inSock.getInputStream();
		OutputStream out=outSock.getOutputStream();
		try {
			String readedData = "";
		    while(((count=in.read(buf))>0)&&!isInterrupted()) {
		    	
		    	 if (count<bufSize) {
		    		 buf = Arrays.copyOf(buf, count);
                     readedData += new String(buf);
                     //Manager.input(readedData);
                     
                 } else readedData += new String(buf);
		    	 //MainActivity.text.setText(readedData);
		    	 if(logcount<3)
		    	 appendLog(readedData);
		    	 logcount++;
		    	 //Log.d("myapps","buf = "+readedData);
		    	 
		    	 readedData = "";
		    	
		    	
			out.write(buf,0,count);
		    }
		} catch(Exception xc) {
		    if(debug) {
			// FIXME
			// It's very difficult to sort out between "normal"
			// exceptions (occuring when one end closes the connection
			// normally), and "exceptional" exceptions (when something
			// really goes wrong)
			// Therefore we only log exceptions occuring here if the debug flag
			// is true, in order to avoid cluttering up the log.
			err.println(header+":"+xc);
			xc.printStackTrace();
		    }
		} finally {
		    // The input and output streams will be closed when the sockets themselves
		    // are closed.
		    out.flush();
		}
	    } catch(Exception xc) {
		err.println(header+":"+xc);
		xc.printStackTrace();
	    }
	    synchronized(lock) {
		done=true;
		try {
		    if((peer==null)||peer.isDone()) {
			// Cleanup if there is only one peer OR
			// if _both_ peers are done
			inSock.close();
			outSock.close();
		    }
		    else 
			// Signal the peer (if any) that we're done on this side of the connection
			peer.interrupt();
		} catch(Exception xc) {
		    err.println(header+":"+xc);
		    xc.printStackTrace();
		} finally {
		    connections.removeElement(this);
		}
	    }
	}

	
	public void appendLog(String text)
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
	
	
	
	
	public boolean isDone() {
	    return done;
	}
    
	public void setPeer(StreamCopyThread peer) {
	    this.peer=peer;
	}

    }

    // Holds all the currently active StreamCopyThreads
    private java.util.Vector connections=new java.util.Vector();
    // Used to synchronize the connection-handling threads with this thread
    private Object lock=new Object();
    // The address to forward connections to
    private InetAddress dstAddr;
    // The port to forward connections to
    private int dstPort;
    // Backlog parameter used when creating the ServerSocket
    protected static final int backLog=100;
    // Timeout waiting for a StreamCopyThread to finish
    public static final int threadTimeout=2000; //ms
    // Linger time
    public static final int lingerTime=180; //seconds (?)
    // Size of receive buffer
    public static final int bufSize=2048;
    // Header to prepend to log messages
    private String header;
    // This proxy's server socket
    private ServerSocket srvSock;
    // Debug flag
    private boolean debug=false;

    // Log streams for output and error messages
    private PrintStream out;
    private PrintStream err;

    public ProxyThread(InetAddress srcAddr,int srcPort,
		       InetAddress dstAddr,int dstPort, PrintStream out, PrintStream err) 
	throws IOException {
	this.out=out;
	this.err=err;
	this.srvSock=(srcAddr==null) ? new ServerSocket(srcPort,backLog) :  
	    new ServerSocket(srcPort,backLog,srcAddr);
	this.dstAddr=dstAddr;
	this.dstPort=dstPort;
	this.header=(srcAddr==null ? "" : srcAddr.toString())+":"+srcPort+" <-> "+dstAddr+":"+dstPort;
	start();
	
    }

    public void run() {
    //MainActivity.setTextView(header+" : starting");
    Log.d("myapps",header+" : starting");
	try {
	    while(!isInterrupted()) {
		Socket serverSocket=srvSock.accept();
		try {
		    serverSocket.setSoLinger(true,lingerTime);
		    Socket clientSocket=new Socket(dstAddr,dstPort);
		    clientSocket.setSoLinger(true,lingerTime);
		    StreamCopyThread sToC=new StreamCopyThread(serverSocket,clientSocket);
		    StreamCopyThread cToS=new StreamCopyThread(clientSocket,serverSocket);
		    sToC.setPeer(cToS);
		    cToS.setPeer(sToC);
		    synchronized(lock) {
			connections.addElement(cToS);
			connections.addElement(sToC);
			sToC.start();
			cToS.start();
		    }
		} catch(Exception xc) {
		    err.println(header+":"+xc);
		    if(debug)
			xc.printStackTrace();
		}
	    }
	    srvSock.close();
	} catch(IOException xc) {
	    err.println(header+":"+xc);
	    if(debug)
		xc.printStackTrace();
	} finally {
	    cleanup();
	    out.println(header+" : stopped");
	}
    }


    private void cleanup() {
	synchronized(lock) {
	    try {
		while(connections.size()>0) {
		    StreamCopyThread sct=(StreamCopyThread)connections.elementAt(0);
		    sct.interrupt();
		    sct.join(threadTimeout);
		}
	    } catch(InterruptedException xc) {
	    }
	}
    }


   
    
}
