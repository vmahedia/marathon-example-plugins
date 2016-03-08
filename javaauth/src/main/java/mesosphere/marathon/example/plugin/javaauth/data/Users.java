package mesosphere.marathon.example.plugin.javaauth.data;

import org.apache.log4j.Logger;

import javax.naming.AuthenticationException;
import java.util.ArrayList;

/**
 * Created by vinitmahedia on 3/6/16.
 */
public class Users {
    public ArrayList<UserPermissions> users;

    @Override
    public String toString() {
        StringBuilder usersJson = new StringBuilder();
        usersJson.append("{ ");
        for (UserPermissions userPermissions : users) {
            usersJson.append(userPermissions.toString());
            usersJson.append(", ");
        }
        usersJson.setLength(usersJson.length() - 2);
        usersJson.append(" }");
        return usersJson.toString();
    }

    public UserPermissions authenticated(String username, String password) throws AuthenticationException {
        for (UserPermissions userPermissions: users) {
            if(username.equals(userPermissions.user) && password.equals(userPermissions.password)){
                return userPermissions;
            }
        }
        throw new AuthenticationException(String.format("Authentication failed for user - %s", username));
    }
}
