public class Lancement {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Thread socketServerThread = new Thread(new SocketServerThread());
		socketServerThread.start();

	}

}
