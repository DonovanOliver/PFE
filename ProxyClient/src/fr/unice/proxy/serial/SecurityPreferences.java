package fr.unice.proxy.serial;

import java.io.File;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import android.util.Log;

public class SecurityPreferences {

	public static int USER_LEVEL_BEGINNER = 1;

	/**
	 * Static value for Intermediate mode
	 */
	public static int USER_LEVEL_INTERMEDIATE = 2;

	/**
	 * Static value for Advanced mode
	 */
	public static int USER_LEVEL_ADVANCED = 3;

	/**
	 * The user level; beginner = 1, intermediate = 2, advanced = 3
	 */
	int userLevel;

	/**
	 * The security level, ranging from 1 to 4. This attribute is relevant only
	 * for beginner level.
	 */
	int securityLevel;

	/**
	 * The data type. It can be Personal, Administrative, Medical, Professional,
	 * Banking
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
	 * The algorithm to be used for encryption. Used only if
	 * <b>confidentiality</b> is set to true and user level is 3(advanced).
	 */
	String confidentialityAlgorithm;

	/**
	 * The algorithm to be used for signing. Used only if <b>authenticity</b> is
	 * set to true and user level is 3(advanced).
	 */
	String authenticityAlgorithm;

	/**
	 * The algorithm to be used for integrity checking. Used only if
	 * <b>integrity</b> is set to true and user level is 3(advanced).
	 */
	String integrityAlgorithm;

	public SecurityPreferences() {
	}

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

	public void readSecuPref() {
		try {
			// if (Config.contains("filters"))
			Log.d("myapps", " SecurityPreferences ReadSecuPref Start");
			{
				SAXBuilder builder = new SAXBuilder();
				File xmlFile = new File("sdcard/data.xml");
				Document document = (Document) builder.build(xmlFile);
				Element rootNode = document.getRootElement();
				List<Element> nodes = rootNode.getChildren("Config");
				for (Element node : nodes) {
					// String nodeName = node.getChildTextTrim("name");
					Log.d("myapps", "ReadSecuPref Node ");
					String beginner = node.getChildTextTrim("beginner");
					if (beginner != null)
						setUserLevel(1);
					String intermediate = node.getChildTextTrim("intermediate");
					if (intermediate != null)
						setUserLevel(2);
					String advanced = node.getChildTextTrim("advanced");
					if (advanced != null)
						setUserLevel(3);

					Log.i("myapps", "" + " beginner : " + beginner
							+ " intermediate : " + intermediate
							+ " advanced : " + advanced);

					/* Beginner */
					String type = node.getChildTextTrim("type");
					if (type != null)
						setDataType(type);
					String level = node.getChildTextTrim("level");
					if (level != null)
						setSecurityLevel(Integer.parseInt(level.substring(level
								.length() - 1)));

					/* intermediate & Advanced */

					String Conf = node.getChildTextTrim("confidentiality");
					if (Conf != null)
						setConfidentiality(Boolean.valueOf(Conf));
					String ConfAlgo = node.getChildTextTrim("ConfAlgo");
					if (ConfAlgo != null)
						setConfidentialityAlgorithm(ConfAlgo);
					String authenticity = node.getChildTextTrim("authenticity");
					if (authenticity != null)
						setAuthenticity(Boolean.valueOf(authenticity));
					String authAlgo = node.getChildTextTrim("authAlgo");
					if (authAlgo != null)
						setAuthenticityAlgorithm(authAlgo);
					String integrity = node.getChildTextTrim("integrity");
					if (integrity != null)
						setIntegrity(Boolean.valueOf(authenticity));
					String nonrepudiation = node
							.getChildTextTrim("non-repudiation");
					if (nonrepudiation != null)
						setNonrepudiation((Boolean.valueOf(nonrepudiation)));
					Log.i("myapps", " type : " + type + " level : \n" + level
							+ " Conf : " + Conf + " Auth : " + authenticity
							+ " inter " + integrity + "\nAuthalgo : "
							+ authAlgo + " ConfAlgo : " + ConfAlgo);
				}
				/*
				 * Advanced
				 * 
				 * xml.setValue("confidentiality",
				 * ""+chkConfidentiality.isChecked()); xml.setValue("ConfAlgo",
				 * ""
				 * +spinnerConfidentialityAlgorithms.getSelectedItem().toString
				 * ()); xml.setValue("authenticity",
				 * ""+chkAuthenticity.isChecked()); xml.setValue("authAlgo",
				 * ""+spinnerAuthenticityAlgorithms
				 * .getSelectedItem().toString()); xml.setValue("integrity",
				 * ""+chkIntegrity.isChecked()); xml.setValue("non-repudiation",
				 * ""+chkNonRepudiation.isChecked());
				 * 
				 * 
				 * 
				 * Intermidiate
				 * 
				 * xml.setValue("confidentiality",
				 * ""+chkConfidentiality.isChecked());
				 * xml.setValue("authenticity", ""+chkAuthenticity.isChecked());
				 * xml.setValue("integrity", ""+chkIntegrity.isChecked());
				 * xml.setValue("non-repudiation",
				 * ""+chkNonRepudiation.isChecked());
				 */

			}
		} catch (Exception ex) {
			Log.d("myapps", "[Filters > readFilters] Filters file read "
					+ "error (check " + ex + ")");
		}

	}
}
