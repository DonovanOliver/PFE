package fr.unice.proxy.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Luis Delgado (@ldelgadoj)
 * @version 1.0 (11/07/2012)
 */
public class HTTPConnection {

    public static String get(String sUrl) {
        String response = "";
        try {
            URL url = new URL(sUrl);
            URLConnection uc = url.openConnection();
            InputStreamReader isr = new InputStreamReader(uc.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line=br.readLine())!=null) {
                response += line;
            }
        } catch (Exception ex) {
            System.err.println("[HTTPConnection > GET] " +
                    "Exception " + ex.getClass().getSimpleName() + " launched");
        } finally {
            return response;
        }
    }

}