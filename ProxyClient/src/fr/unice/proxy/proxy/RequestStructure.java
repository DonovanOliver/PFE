package fr.unice.proxy.proxy;

import fr.unice.proxy.serial.SecurityPreferences;





public class RequestStructure {

	SecurityPreferences secPref; 
	String cryptedmsj ;
    String propApplied; 
     
	public RequestStructure(SecurityPreferences secPref,String cryptedmsj,String propApplied) {
		this.secPref = secPref;
		this.cryptedmsj = cryptedmsj;
		this.propApplied = propApplied;
	}
	

	public SecurityPreferences getSecurityPreferences() {
		return secPref ;
	}
	
	public String getcryptedmsj() {
		return cryptedmsj;
	}
	
	public String getpropApplied() {
		return propApplied;
	}
	
	
	
}
