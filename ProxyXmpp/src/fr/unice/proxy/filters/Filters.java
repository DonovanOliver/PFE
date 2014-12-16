
package fr.unice.proxy.filters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import android.util.Log;

/**
 * class 
 * @author 
 */
public class Filters {

    private static ArrayList<IFFilter> clientFilters = new ArrayList();
    private static ArrayList<IFFilter> serverFilters = new ArrayList();

    public static String processClient(String data) {
        for (IFFilter filter : clientFilters) data = filter.process(data);
        return data;
    }

    public static String processServer(String data) {
        for (IFFilter filter : serverFilters) data = filter.process(data);
        return data;
    }

    public static void readFilters() {
        try {
            //if (Config.contains("filters")) 
        	{
                SAXBuilder builder = new SAXBuilder();
                File xmlFile = new File("sdcard/filters.xml");
                Document document = (Document) builder.build(xmlFile);
                Element rootNode = document.getRootElement();
                List<Element> nodes = rootNode.getChildren("filter");
                for (Element node : nodes) {
                    String nodeName = node.getChildTextTrim("name");
                    String type = node.getChildTextTrim("type");
                    
                    if (type.equalsIgnoreCase("internal")) {
                        addInternalFilter(node);
                    } else {
                        Filter filter = new Filter(nodeName);
                        String contains = node.getChildTextTrim("contains");
                        if (contains != null) filter.setContains(contains);
                        String waitFor = node.getChildTextTrim("waitfor");
                        if (waitFor != null) filter.setWaitFor(waitFor);
                        String stanza = node.getChildTextTrim("stanza");
                        if (stanza != null) filter.setStanza(stanza);
                        List<Element> replaces = node.getChildren("replace");
                        for (Element replace : replaces) {
                            String regex = replace.getChildTextTrim("regex");
                            String replacement = replace.getChildTextTrim("replacement");
                            filter.addReplace(regex, replacement);
                        }
                        List<Element> removes = node.getChildren("remove");
                        for (Element remove : removes) {
                            String regex = remove.getTextTrim();
                            filter.addRemove(regex);
                        }
                        Element search = node.getChild("search");
                        if (search != null) {
                            String regex = search.getChildTextTrim("regex");
                            String output = search.getChildTextTrim("output");
                            filter.addSearch(regex, output);
                        }
                        Element repeat = node.getChild("repeat");
                        if (repeat != null) filter.setRepeat(true);
                        boolean both = type.equals("both");
                        if (both || type.equalsIgnoreCase("client")) clientFilters.add(filter);
                        if (both || type.equalsIgnoreCase("server")) serverFilters.add(filter);
                    }
                }
            }
        } catch(Exception ex) {
            Log.d("myapps","[Filters > readFilters] Filters file read " +  "error (check " + ex + ")");
        }
    }

    private static void addInternalFilter(Element node) {
        String nodeName = node.getChildTextTrim("name");
        if (nodeName.equalsIgnoreCase("google_plain_mechanism")) {
            if (node.getChild("enabled")!= null) {
            	Log.d("myapps","[Filters > readFilters] OK ");
                clientFilters.add((new Credentials()));
                serverFilters.add((new Credentials()));
            }
        }
    }

}
