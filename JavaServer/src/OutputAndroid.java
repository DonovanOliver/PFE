import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;

public class OutputAndroid extends Thread {
	private Socket hostThreadSocket;
	String message;

	OutputAndroid(Socket socket, String message) {
		hostThreadSocket = socket;
		this.message = message;
	}

	@Override
	public void run() {
		OutputStream outputStream;
		try {
			outputStream = hostThreadSocket.getOutputStream();
			PrintStream printStream = new PrintStream(outputStream);
			printStream.print(this.message);
			printStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			message += "Something wrong! " + e.toString() + "\n";
			System.out.println(message);
		}

	}

}
