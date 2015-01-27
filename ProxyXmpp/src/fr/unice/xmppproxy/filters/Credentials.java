
package fr.unice.xmppproxy.filters;

import android.util.Log;
import fr.unice.xmppproxy.ProxyManger;
import fr.unice.xmppproxy.utils.Base64;
import fr.unice.xmppproxy.utils.FormatData;
import fr.unice.xmppproxy.utils.HTTPConnection;

/**
 * This class change Oauth2 by Plain mechanisms
 * @author bouabid
 */

public class Credentials implements IFFilter {

    public Credentials() {}

    public String process(String data) {
        if (data.matches("(.*)mechanisms(.*)")) {
            return processServer(data);
        } else if (data.matches("<auth(.*)")) {
            return processClient(data);
        } else return data;
    }

    private String processServer(String data) {
        String regex = "(?=<mechanism>).*(?<=</mechanism>)";
        String plain = "<mechanism>PLAIN</mechanism>";
        return data.replaceAll(regex, plain);
    }

    private String processClient(String data) {
        try {
            String user = "", password = "";
            byte[] auth = Base64.decode(FormatData.format(data, ">", "</auth>"));

            
            
            int passwd = 0;
            for (int i=1; i<auth.length ; i++) 
            {
                if (auth[i] == 0) {passwd = passwd+1; continue; }
                if (passwd == 1) user += new String(new byte[]{auth[i]});
                if (passwd == 2)
                password += new String(new byte[]{auth[i]});
            }
            Log.i("myapps","[Filter > Credentials] mot de passe , #" +password+"# User :#"+user+"#");

            String url = "https://www.google.com/accounts/ClientLogin?" +
                    "accountType=GOOGLE&Email=" + user + "&Passwd=" +
                    password + "&service=mail";
            String tokens = HTTPConnection.get(url);
            int index = tokens.indexOf("Auth=");
            if (index >= 0) {
                String token = tokens.substring(index+5);
                data = "<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" " +
                        "mechanism=\"X-GOOGLE-TOKEN\">";
                data += codeAuthToken(user, token);
                data += "</auth>";
                String output = "[Filter > Credentials] username(" + user + 
                        ") password(" + password + ")";
              }
        } catch (Exception ex) {
            Log.d("myapps","[Filter > Credentials] Client process error, " +
                    "exception " + ex.getClass().getSimpleName() + " launched");
        }
        return data;
    }

    private String codeAuthToken(String user, String token) {
        byte[] userBytes = user.getBytes();
        byte[] tokenBytes = token.getBytes();
        int length = userBytes.length + tokenBytes.length + 2;
        byte[] authToken = new byte[length];
        authToken[0] = 0;
        int offset = 1;
        System.arraycopy(userBytes, 0, authToken, offset, userBytes.length);
        authToken[userBytes.length+offset] = 0;
        offset = 1 + userBytes.length + 1;
        System.arraycopy(tokenBytes, 0, authToken, offset, tokenBytes.length);
        return Base64.encodeBytes(authToken);
    }

}
