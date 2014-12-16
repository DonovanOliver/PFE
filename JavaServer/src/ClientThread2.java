import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class ClientThread2 extends Thread {
	String addr;
	int port;
	String response = "";
	String request = "";

	public ClientThread2(String addr, int port, String request) {
		this.addr = addr;
		this.port = port;
		this.request = request;
		System.out.println("Création du client thread");

	}

	public String getResponseRouteur() {
		return response;
	}

	public void run() {

		SocketAddress remote = null;
		SocketChannel channel = null;
		try {
			remote = new InetSocketAddress(addr, port);
			channel = SocketChannel.open(remote);
			Charset charset = Charset.forName("us-ascii");
			CharsetDecoder decoder = charset.newDecoder();
			String request = this.request;
			
			System.out.println("-------------------------");

			ByteBuffer header = ByteBuffer.wrap(request.getBytes("US-ASCII"));
			channel.write(header);

			ByteBuffer buffer = ByteBuffer.allocate(8192);
			while (channel.read(buffer) != -1) {
				buffer.flip();
				response += decoder.decode(buffer).toString();
				System.out.println("La réponse du routeur 2="+response);
				buffer.clear();
			}
			channel.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
