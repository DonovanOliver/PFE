
package fr.unice.proxy.utils;

import fr.unice.proxy.proxy.ProxyService;
import java.io.FileReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import android.util.Log;

/**
 * @author Luis Delgado (@ldelgadoj)
 * @version 1.0 (11/07/2012)
 */

public class Config {

    private static HashMap<String,Object> config = new HashMap();

    public static Object get(String key) {
        if (config.containsKey(key)) return config.get(key);
        else return Boolean.FALSE;
    }

    public static boolean contains(String key) {
        return config.containsKey(key);
    }

    public static void readConfig() {
        try {
            Properties file = new Properties();
            file.load(new FileReader("xmpploit.properties"));
            Enumeration keys = file.keys();
            while(keys.hasMoreElements()) {
                String key = (String)keys.nextElement();
                String property = file.getProperty(key);
                if (property.equals("1")) {
                    config.put(key, Boolean.TRUE);
                } else if (!property.equals("0")) {
                    config.put(key, property);
                }
            }
            Log.d("myapps","xmpploit 1.0 (" +
                    "http://www.ldelgado.es/?xmpploit)\n" +
                    "developer: luis delgado (@ldelgadoj)\n");
        } catch(Exception ex) {
            Log.d("myapps","[Config > readConfig]" +
                    "Config file read error (check xmpploit.properties)");
            //Manager.exit();
        }
    }

}
