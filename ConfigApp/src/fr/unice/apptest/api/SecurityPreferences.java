package fr.unice.apptest.api;

public class SecurityPreferences {
	
	/**
	 * The user level; beginner = 1, intermediate = 2, advanced = 3
	 */
	int userLevel;
	
	/**
	 * The security level, ranging from 1 to 4. This attribute is relevant only for beginner level.
	 */
	int securityLevel;
	
	/**
	 * The data type. It can be Personal, Administrative, Medical, Professional, Banking
	 */
	String dataType;
	
	/**
	 * Check if confidentiality is selected.
	 */
	boolean confidentiality;
	
	/**
	 * Check if authenticity is selected.
	 */
	boolean authenticity;
	
	/**
	 * Check if integrity is selected.
	 */
	boolean integrity;
	
	/**
	 * Check if nonrepudiation is selected.
	 */
	boolean nonrepudiation;
	
	/**
	 * The algorithm to be used for encryption. Used only if <b>confidentiality</b> is set to true and user level is 3(advanced).
	 */
	String confidentialityAlgorithm;
	
	/**
	 * The algorithm to be used for signing. Used only if <b>authenticity</b> is set to true and user level is 3(advanced).
	 */
	String authenticityAlgorithm;
	
	/**
	 * The algorithm to be used for integrity checking. Used only if <b>integrity</b> is set to true and user level is 3(advanced).
	 */
	String integrityAlgorithm;
	
	public SecurityPreferences() {}
	
	public SecurityPreferences(int userLevel) {
		setUserLevel(userLevel);
	}
	
	// GETTERS & SETTERS
	public int getUserLevel() {
		return userLevel;
	}
	
	public void setUserLevel(int userLevel) {
		this.userLevel = userLevel;
	}
	
	public int getSecurityLevel() {
		return securityLevel;
	}
	
	public void setSecurityLevel(int securityLevel) {
		this.securityLevel = securityLevel;
	}
	
	public String getDataType() {
		return dataType;
	}
	
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
	
	public boolean isConfidentiality() {
		return confidentiality;
	}
	
	public void setConfidentiality(boolean confidentiality) {
		this.confidentiality = confidentiality;
	}
	
	public boolean isAuthenticity() {
		return authenticity;
	}
	
	public void setAuthenticity(boolean authenticity) {
		this.authenticity = authenticity;
	}
	
	public boolean isIntegrity() {
		return integrity;
	}
	
	public void setIntegrity(boolean integrity) {
		this.integrity = integrity;
	}
	
	public boolean isNonrepudiation() {
		return nonrepudiation;
	}
	
	public void setNonrepudiation(boolean nonrepudiation) {
		this.nonrepudiation = nonrepudiation;
	}
	
	public String getConfidentialityAlgorithm() {
		return confidentialityAlgorithm;
	}
	
	public void setConfidentialityAlgorithm(String confidentialityAlgorithm) {
		this.confidentialityAlgorithm = confidentialityAlgorithm;
	}
	
	public String getAuthenticityAlgorithm() {
		return authenticityAlgorithm;
	}
	
	public void setAuthenticityAlgorithm(String authenticityAlgorithm) {
		this.authenticityAlgorithm = authenticityAlgorithm;
	}
	
	public String getIntegrityAlgorithm() {
		return integrityAlgorithm;
	}
	
	public void setIntegrityAlgorithm(String integrityAlgorithm) {
		this.integrityAlgorithm = integrityAlgorithm;
	}
	
}
