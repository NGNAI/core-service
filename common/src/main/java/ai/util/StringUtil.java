package ai.util;

import java.text.Normalizer;

public class StringUtil {
    public static String toConstantCase(String input){
        if(input == null)
            return null;
        return removeExtraSpace(removeAccent(input)).replaceAll(" ","_").toUpperCase();
    }

    public static String removeAccent(String input){
        if(input == null)
            return null;
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        return normalized.replaceAll("\\p{M}","");
    }

    public static String removeExtraSpace(String input) {
        if (input == null) return null;

        return input.trim().replaceAll("\\s+", " ");
    }
}
