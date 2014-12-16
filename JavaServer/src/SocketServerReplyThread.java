import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketServerReplyThread extends Thread {

	private Socket hostThreadSocket;
	private InputStream input;
	String response;
	byte[] bytes;

	private short toUnsigned(byte c) {
		if (c >= 0) {
			return c;
		}
		return (short) (c + 256);
	}

	SocketServerReplyThread(Socket socket) {
		hostThreadSocket = socket;
		response = "";
		try {
			this.input = this.hostThreadSocket.getInputStream();
			System.out.println("INPUT");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		/*
		 * for (int i = 0; i < 256; i++) {
		 * System.out.print(toUnsigned((byte)i)+" "); }
		 */
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(
					1024);
			byte[] buffer = new byte[1024];
			// System.out.print("Receive bytes:");
			int bytesRead;
			/*
			 * notice: inputStream.read() will block if no data return
			 */
			while ((bytesRead = this.input.read(buffer)) != -1) {
				byteArrayOutputStream.write(buffer, 0, bytesRead);
				response += byteArrayOutputStream.toString("UTF-8");
				/*
				 * bytes = byteArrayOutputStream.toByteArray(); for (int i = 0;
				 * i < bytes.length; i++) {
				 * System.out.print(toUnsigned(bytes[i]) + " "); }
				 */
				if (response.contains("GET")) {
					System.out.println(response);
					GenerateUrlStr urlStr = new GenerateUrlStr(response);
					String host = urlStr.getHost();
					ClientThread client = new ClientThread(host, 80, response);
					client.run();

					OutputAndroid outAndroid = new OutputAndroid(
							this.hostThreadSocket, client.getResponseRouteur());
					outAndroid.run();

				} else {
					System.out.println("La requête ne provient pas du browser");
					break;
				}

			}
			System.out.println("Sortie boucle");

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			response = "UnknownHostException: " + e.toString();
			System.out.println(response);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			response = "IOException: " + e.toString();
			System.out.println(response);
		} finally {
			if (this.hostThreadSocket != null) {
				try {
					System.out.println("Fermeture du socket d'acceptation");
					this.hostThreadSocket.close();

				} catch (IOException e) { // TODO Auto-generated catch block
					System.out.println("Exception SocketServerReplyThread");
					e.printStackTrace();

				}
			}
		}

	}
}