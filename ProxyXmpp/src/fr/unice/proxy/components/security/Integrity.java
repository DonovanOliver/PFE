package fr.unice.proxy.components.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * This class is responsible for verifying the integrity of data
 * 
 * @author andrei
 * 
 */
public class Integrity {

	/**
	 * The algorithm to be used for hashing
	 */
	private String algo;

	/**
	 * Public constructor by default. Uses SHA-1 as algorithm if it is not specified.
	 */
	public Integrity() {
		setAlgorithm("SHA-1");
	}

	/**
	 * Public constructor with 1 argument. The argument is the algorithm to be used for hashing
	 * @param algorithm
	 */
	public Integrity(String algorithm) {
		setAlgorithm(algorithm);
	}

	/**
	 * Compute the hash of the specified bytes array using the protocol set.
	 * 
	 * @param text
	 *            : bytes array to sign
	 * @return the hash of the specified bytes array.
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException 
	 */
	public byte[] hash(byte[] text) throws NoSuchAlgorithmException, NoSuchProviderException {
		
		MessageDigest md = MessageDigest.getInstance(getAlgorithm(), "BC");
		byte[] Digest = md.digest(text);
		
		return Digest;
	}

	/**
	 * Verify the hash of the specified bytes array.
	 * 
	 * @param text
	 * @param hash
	 * @return true if the hash is in accordance with the text, false otherwise.
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException 
	 */
	public boolean verifyHash(byte[] text, byte[] hash)
			throws NoSuchAlgorithmException, NoSuchProviderException {

		MessageDigest md = MessageDigest.getInstance(getAlgorithm(), "BC");
		byte[] Digest = md.digest(text);

		return MessageDigest.isEqual(Digest, hash);
	}

	// ------GETTERS & SETTERS

	public String getAlgorithm() {
		return algo;
	}

	public void setAlgorithm(String algorithm) {
		this.algo = algorithm;
	}

}
