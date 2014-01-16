package gxtproject.server.util;

import com.mongodb.DBObject;

import java.util.Map;

public class HierarchyUtil {

    public static Object fetchValueByComplexKey(Map map, String complexKey) {
        String keys[] = complexKey.split("\\.");
        int depth = keys.length;

        if (depth < 2) {
            return map.get(complexKey);
        }

        Map innerMap = (Map) map.get(keys[0]);
        for (int i = 1; i < keys.length - 1; i++) {
            innerMap = (Map) innerMap.get(keys[i]);
        }

        return innerMap.get(keys[keys.length - 1]);
    }

    public static Object fetchValueByComplexKey(DBObject object, String complexKey) {
        String keys[] = complexKey.split("\\.");
        int depth = keys.length;

        if (depth < 2) {
            return object.get(complexKey);
        }

        DBObject innerObject = (DBObject) object.get(keys[0]);
        for (int i = 1; i < keys.length - 1; i++) {
            innerObject = (DBObject) innerObject.get(keys[i]);
        }

        return innerObject.get(keys[keys.length - 1]);
    }
}
