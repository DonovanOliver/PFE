package fr.unice.proxy.components;



import fr.unice.proxy.serial.SecurityPreferences;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * This class is responsible for the interaction with the database. Defines the
 * basic CRUD operations, and gives the ability to grab policies to apply, along
 * with their algorithms, based on the level and data type
 * 
 * @author andrei
 * 
 */
public class PolicyEngine {
	
	/**
	 * The unique Singleton instance
	 */
	private static PolicyEngine uniqueInstance;

	// Table fields
	public static final String KEY_ROWID = "_id";
	public static final String KEY_LEVEL = "level";
	public static final String KEY_DATATYPE = "datatype";
	public static final String KEY_PROPERTY = "property";
	public static final String KEY_ALGORITHM = "algorithm";
	
	private DatabaseHelper databaseHelper;
	private SQLiteDatabase sqldb;

	/**
	 * Database creation sql statement
	 */
	private static final String DATABASE_CREATE = "create table policies (" + 
			"_id integer primary key autoincrement, " +
			"level integer not null, " +
			"datatype text not null, " +
			"property text not null, " +
			"algorithm text not null);";
	
	/**
	 * Database name
	 */
    private static final String DATABASE_NAME = "pfe";
    
    /**
     * Table name
     */
    private static final String TABLE_NAME = "policies";
    
    /**
     * Database version
     */
    private static final int DATABASE_VERSION = 2;
	
	/**
	 * The context from within the database is called
	 */
	private final Context context;

	/**
     * Private constructor - takes the context to allow the database to be
     * opened/created. 
     * Opens the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @param context the Context within which to work
     * @throws SQLException if the database could be neither opened or created
     */
	private PolicyEngine(Context context) throws SQLException {
		this.context = context;
    	databaseHelper = new DatabaseHelper(this.context);
        sqldb = databaseHelper.getWritableDatabase();
        insertBasicProperties();
	}
	
	public static void initializeDatabase(Context context) {
		if (uniqueInstance == null) {
			uniqueInstance = new PolicyEngine(context);
		}
	}
	
	public static PolicyEngine getInstance() {
		return uniqueInstance;
	}

    public void close() {
    	databaseHelper.close();
    }
    
    /**
     * Add a new Policy using the level, property and algorithm provided. If the policy is
     * successfully created return the new rowId for that policy, otherwise return
     * a -1 to indicate failure.
     * 
     * @param level the security level
     * @param property the security property to apply
     * @param algorithm the algorithm to be applied for the property
     * @param datatype the datatype associated for the property
     * @return rowId or -1 if failed
     */
    public long addProperty(int level, String datatype, String property, String algorithm) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_LEVEL, level);
        initialValues.put(KEY_DATATYPE, datatype);
        initialValues.put(KEY_PROPERTY, property);
        initialValues.put(KEY_ALGORITHM, algorithm);

        return sqldb.insert(TABLE_NAME, null, initialValues);
    }
    
    /**
     * This function returns the properties and their algorithms found based on the security level and the datatype
     * and sets them to the SecurityPreferences
     * @param securityLevel The security level
     * @param dataType The data type
     * @param SecurityPreferences The security properties what will be edited
     */
	public void setProperties(final int securityLevel, final String dataType, SecurityPreferences sp) {
		String property = null, algorithm = null;
		String[] selArgs = new String[] {Integer.toString(securityLevel), dataType};
		// Get all the policies with the specified security level and data type
		// as a Cursor
		Cursor cursor = sqldb.rawQuery("SELECT DISTINCT "
				+ KEY_PROPERTY + ", " + KEY_ALGORITHM
				+ " FROM " + TABLE_NAME
				+ " WHERE " + KEY_LEVEL + " = ?"
				+ " AND " + KEY_DATATYPE + " = ?", selArgs);

		if (cursor.getCount() == 0) {
			// If no result found, return
			return;
		}
		// Iterate over the cursor to get the property and algorithm for each row
		while (cursor.moveToNext()) {
			// For each row, get the property and algorithm and set to the SecurityPreferences
			property = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PROPERTY));
			algorithm = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ALGORITHM));
			
			if (property.equalsIgnoreCase("C")) {
				sp.setConfidentiality(true);
				sp.setConfidentialityAlgorithm(algorithm);
				Log.d("myapps"," RequestManger C true algo + "+algorithm);
			}
			if (property.equalsIgnoreCase("A")) {
				sp.setAuthenticity(true);
				sp.setAuthenticityAlgorithm(algorithm);
				Log.d("myapps"," RequestManger A true algo + "+algorithm);
			}
			if (property.equalsIgnoreCase("I")) {
				sp.setIntegrity(true);
				sp.setIntegrityAlgorithm(algorithm);
				Log.d("myapps"," RequestManger I true algo + "+algorithm);
			}
			if (property.equalsIgnoreCase("N")) {
				sp.setNonrepudiation(true);
				Log.d("myapps"," RequestManger N true algo + "+algorithm);
			}
		}
	}
    
    private void insertBasicProperties() {
    	// We check first that if there are any policies in the table
    	// If there aren't, then insert the basic ones
    	Cursor cursor = sqldb.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    	if (cursor.getCount() == 0) {
			addProperty(1, "Personal", "I", "SHA-1");
			addProperty(1, "Personal", "C", "AES");
			addProperty(2, "Personal", "I", "SHA-256");
			addProperty(2, "Personal", "C", "AES");
			addProperty(3, "Personal", "I", "SHA-256");
			addProperty(3, "Personal", "C", "AES");
			addProperty(3, "Personal", "A", "SHA1withDSA");
			addProperty(4, "Personal", "I", "SHA-384");
			addProperty(4, "Personal", "C", "AES");
			addProperty(4, "Personal", "A", "SHA384withRSA");
			addProperty(4, "Personal", "N", "none");
			
			addProperty(1, "Administrative", "I", "MD5");
			addProperty(2, "Administrative", "I", "MD5");
			addProperty(2, "Administrative", "C", "DES");
			addProperty(3, "Administrative", "I", "SHA-1");
			addProperty(3, "Administrative", "C", "DES");
			addProperty(3, "Administrative", "A", "SHA1withDSA");
			addProperty(4, "Administrative", "I", "SHA-256");
			addProperty(4, "Administrative", "C", "AES");
			addProperty(4, "Administrative", "A", "SHA1withDSA");
			addProperty(4, "Administrative", "N", "none");
			
			addProperty(1, "Medical", "I", "SHA-256");
			addProperty(1, "Medical", "C", "AES");
			addProperty(2, "Medical", "I", "SHA-256");
			addProperty(2, "Medical", "C", "AES");
			addProperty(2, "Medical", "A", "SHA1withDSA");
			addProperty(3, "Medical", "I", "SHA-384");
			addProperty(3, "Medical", "C", "RSA");
			addProperty(3, "Medical", "A", "SHA256withRSA");
			addProperty(3, "Medical", "N", "none");
			addProperty(4, "Medical", "I", "SHA-384");
			addProperty(4, "Medical", "C", "RSA");
			addProperty(4, "Medical", "A", "SHA512withRSA");
			addProperty(4, "Medical", "N", "none");
			
			addProperty(1, "Professional", "I", "SHA-1");
			addProperty(1, "Professional", "C", "AES");
			addProperty(2, "Professional", "I", "SHA-256");
			addProperty(2, "Professional", "C", "RSA");
			addProperty(3, "Professional", "I", "SHA-256");
			addProperty(3, "Professional", "C", "RSA");
			addProperty(3, "Professional", "A", "SHA1withDSA");
			addProperty(4, "Professional", "I", "SHA-512");
			addProperty(4, "Professional", "C", "RSA");
			addProperty(4, "Professional", "A", "SHA384withRSA");
			addProperty(4, "Professional", "N", "none");
			
			addProperty(1, "Banking", "I", "SHA-384");
			addProperty(1, "Banking", "C", "AES");
			addProperty(1, "Banking", "A", "SHA1withDSA");
			addProperty(2, "Banking", "I", "SHA-512");
			addProperty(2, "Banking", "C", "RSA");
			addProperty(2, "Banking", "A", "SHA384withRSA");
			addProperty(3, "Banking", "I", "SHA-512");
			addProperty(3, "Banking", "C", "RSA");
			addProperty(3, "Banking", "A", "SHA384withRSA");
			addProperty(3, "Banking", "N", "none");
			addProperty(4, "Banking", "I", "SHA-512");
			addProperty(4, "Banking", "C", "RSA");
			addProperty(4, "Banking", "A", "SHA512withRSA");
			addProperty(4, "Banking", "N", "none");
    	}
    }
	

	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

		/**
		 * This overrided method is called when we create the class. It will create the
		 * database if it was not already created.
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);			
		}

		/**
		 * Method used when upgrading database to a newer version.
		 * Drops the old table and creates a new version.
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS policies;");
            onCreate(db);
		}
		
	}


}
