package mesosphere.marathon.example.plugin.javaauth.data;

import mesosphere.marathon.plugin.PathId;
import org.apache.log4j.Logger;

import java.util.ArrayList;

/**
 * Created by vinitmahedia on 3/6/16.
 */
public class UserPermissions {
    public String user;
    public String password;
    public ArrayList<Permission> permissions;

    public boolean isAuthorized(String action, String pathId) {
        log.debug(String.format("user - %s, action - %s, path - %s", user, action, pathId));
        for(Permission permission: permissions) {
            if(pathId.startsWith(permission.on)) {
                permission.allowed.equalsIgnoreCase(action);
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder userPermissionsJson = new StringBuilder();
        userPermissionsJson.append(String.format("{ \"user\" : \"%s\", \"password\" : \"%s\" , ", user, password));
        userPermissionsJson.append("\"permissions\": [ ");
        for(Permission permission : permissions) {
            userPermissionsJson.append(String.format("{ \"allowed\": \"%s\", \"on\": \"%s\"} ,",
                    permission.allowed, permission.on));
        }
        userPermissionsJson.setLength(userPermissionsJson.length() - 1);
        userPermissionsJson.append("] }");
        return userPermissionsJson.toString();
    }
    static Logger log = Logger.getLogger(UserPermissions.class.getName());
}
