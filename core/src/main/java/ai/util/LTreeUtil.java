package ai.util;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class LTreeUtil {
    public String buildPath(String parentPath, UUID id){
        return parentPath!=null && !parentPath.isEmpty() ? String.format("%s.%s",parentPath,id) : id.toString();
    }
}
