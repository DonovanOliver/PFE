package fr.unice.proxy.utils;

/**
 * class 
 * @author 
 */

public class FormatData {

    public static String format(String cadena, String searchBegin, String searchEnd) {
        int index1, index2, begin=0, end=0;
        index1 = cadena.indexOf(searchBegin);
        if (index1>=0) {
            begin = index1+searchBegin.length();
            String subString = cadena.substring(begin);
            index2 = subString.indexOf(searchEnd);
            if (index2>=0) end = index2;
        }
        return cadena.substring(begin, begin+end);
    }

}
