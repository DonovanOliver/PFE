import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServerThread extends Thread {
	    ServerSocket serverSocket;
		static final int SocketServerPORT = 8080;
		int count = 0;
		String message = "";

		@Override
		public void run() {
			try {
				serverSocket = new ServerSocket(SocketServerPORT);
				System.out.println("I'm waiting here: "+ serverSocket.getLocalPort());
				while (true) {
					Socket socket = serverSocket.accept();
					System.out.println(socket.getInetAddress()+ ":" + socket.getPort() + "\n");
					SocketServerReplyThread socketServerReplyThread = new SocketServerReplyThread(socket);
					socketServerReplyThread.run();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Exception dans Socket Server Thread "); 
			}
		}
	}