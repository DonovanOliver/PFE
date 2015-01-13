package fr.unice.proxy.serial;

/**
 * This class is responsible for keeping all the exchanged keys between 2 proxies during the Key Exchange phase.
 * @author andrei
 *
 */
public class SerialKeys {
	
	/**
	 * Public by default constructor.
	 */
	public SerialKeys() {}
	
	/**
	 * The AES Secret Key as byte array
	 */
	private byte[] secretKeyAES;
	
	/**
	 * The DES Secret Key as byte array
	 */
	private byte[] secretKeyDES;
	
	/**
	 * The 3DES Secret Key as byte array
	 */
	private byte[] secretKey3DES;
	
	/**
	 * The RSA public key as byte array
	 */
	private byte[] publicKeyRSA;
	
	/**
	 * The DSA public key as byte array
	 */
	private byte[] publicKeyDSA;
	
	
	public byte[] getSecretKeyAES() {
		return secretKeyAES;
	}
	public void setSecretKeyAES(byte[] secretKeyAES) {
		this.secretKeyAES = secretKeyAES;
	}
	public byte[] getSecretKeyDES() {
		return secretKeyDES;
	}
	public void setSecretKeyDES(byte[] secretKeyDES) {
		this.secretKeyDES = secretKeyDES;
	}
	public byte[] getSecretKey3DES() {
		return secretKey3DES;
	}
	public void setSecretKey3DES(byte[] secretKey3DES) {
		this.secretKey3DES = secretKey3DES;
	}
	public byte[] getPublicKeyRSA() {
		return publicKeyRSA;
	}
	public void setPublicKeyRSA(byte[] publicKeyRSA) {
		this.publicKeyRSA = publicKeyRSA;
	}
	public byte[] getPublicKeyDSA() {
		return publicKeyDSA;
	}
	public void setPublicKeyDSA(byte[] publicKeyDSA) {
		this.publicKeyDSA = publicKeyDSA;
	}
	
	

}
