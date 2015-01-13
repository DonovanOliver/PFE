
package fr.unice.xmppproxy.filters;

import fr.unice.xmppproxy.ProxyManger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class 
 * @author 
 */

public class Filter implements IFFilter {

    private ArrayList<String[]> replaces = new ArrayList();
    private ArrayList<String[]> searches = new ArrayList();
    private boolean repeat = false, executed = false;
    private String contains, waitFor;
    private String stanza;
    public String name;

    public Filter(String name) {
        this.name = name;
    }

    public String process(String data) {
        if (contains != null) {
            if ((!executed || repeat) && data.contains(contains)) {
                for (String[] replace : replaces) {
                    data = data.replaceAll(replace[0], replace[1]);
                }
                for (String[] search : searches) {
                    try {
                        Pattern regex = Pattern.compile(search[0]);
                        Matcher matcher = regex.matcher(data);
                        ArrayList<String> matches = new ArrayList();
                        while (matcher.find()) matches.add(matcher.group());
                        if (search[1] != null) {
                            FileWriter fstream = new FileWriter(search[1], true);
                            BufferedWriter out = new BufferedWriter(fstream);
                            for (String match : matches) out.write(match);
                            out.close();
                        } else {
                            String output = "";
                            for (String match : matches) {
                                if (output.length()>0) output += "\n";
                                output += match;
                            }
                            
                        }

                    } catch(Exception ex) {
                        System.err.println("[Filter > " + name + "]" +
                                "Search output stream error");
                    }
                }
                executed = true;
            }
        } else if (waitFor != null) {
            Pattern regex = Pattern.compile(waitFor);
            Matcher matcher = regex.matcher(data);
            if ((!executed || repeat) && matcher.find()) {
                data = data.replaceAll(waitFor, matcher.group() + stanza);
            }
        } else executed = true;
        return data;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public void setContains(String regex) {
        contains = regex;
    }

    public void setWaitFor(String regex) {
        waitFor = regex;
    }

    public void setStanza(String stanza) {
        this.stanza = stanza;
    }

    public void addSearch(String regex, String output) {
        searches.add(new String[]{regex, output});
    }

    public void addRemove(String regex) {
        replaces.add(new String[]{regex, ""});
    }

    public void addReplace(String regex, String replacement) {
        replaces.add(new String[]{regex, replacement});
    }

}