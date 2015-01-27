package fr.unice.proxy.components.security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

/**
 * This class is responsible for generating signatures and verifying signature validity
 * @author andrei
 *
 */
public class Authenticity {
	
	/**
	 * The algorithm to be used for signing and verifying
	 */
	private String algo;
	
	/**
	 * The Public Key used to check whether a signature is valid or not
	 */
	private PublicKey publicKey = null;
	
	/**
	 * The Private Key used to sign a document
	 */
	private PrivateKey privateKey = null;

	/**
	 * Public Default Constructor. Uses SHA1withDSA as algorithm if none specified
	 */
	public Authenticity() {
		setAlgorithm("SHA1withDSA");
	}
	
	/**
	 * Public Constructor with the algorithm to be used.
	 * @param algorithm The algorithm to be used
	 */
	public Authenticity(String algorithm) {
		setAlgorithm(algorithm);
	}

	/**
	 * Function responsible for signing a document
	 * @param clear
	 * @return Signature of the input
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws SignatureException
	 * @throws NoSuchProviderException 
	 */
	public byte[] sign(byte[] clear) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchProviderException {
		Signature sign = Signature.getInstance(getAlgorithm(), "BC");
		sign.initSign(getPrivateKey());
		sign.update(clear);
		return sign.sign();
	}
	
	/**
	 * Function responsible for verifying if a signature is valid
	 * @param signature The signature to be verified
	 * @return Validity
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws SignatureException
	 * @throws NoSuchProviderException 
	 */
	public boolean verifySignature(byte[] text, byte[] signature) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchProviderException {
		Signature sign = Signature.getInstance(getAlgorithm(), "BC");
		sign.initVerify(getPublicKey());
		sign.update(text);
		return sign.verify(signature);
	}
	
	// ------GETTERS & SETTERS
	
	public String getAlgorithm() {
		return algo;
	}

	public void setAlgorithm(String algo) {
		this.algo = algo;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(PrivateKey privateKey) {
		this.privateKey = privateKey;
	}

}
