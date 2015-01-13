package fr.unice.proxy.serial;

/**
 * This class is responsible for keeping the secured data sent from one proxy to another.
 * @author andrei
 *
 */
public class SerialData {
	
	/**
	 * The content that the user originally sends, encrypted or not
	 */
	private byte[] content;
	
	/**
	 * The signature of the content
	 */
	private byte[] signature;
	
	/**
	 * The payload added for the Non-Repudiation
	 */
	private byte[] payLoad;
	
	public byte[] getContent() {
		return content;
	}
	public void setContent(byte[] content) {
		this.content = content;
	}
	public byte[] getSignature() {
		return signature;
	}
	public void setSignature(byte[] signature) {
		this.signature = signature;
	}
	public byte[] getPayLoad() {
		return payLoad;
	}
	public void setPayLoad(byte[] payLoad) {
		this.payLoad = payLoad;
	}
	
	
	

}
