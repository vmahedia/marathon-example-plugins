package mesosphere.marathon.example.plugin.javaauth;

import mesosphere.marathon.example.plugin.javaauth.data.UserPermissions;
import mesosphere.marathon.plugin.auth.Identity;

class JavaIdentity implements Identity {

    private final String name;

    private UserPermissions userPermissions;

    public JavaIdentity(String name, UserPermissions userPermissions) {
        this.name = name;
        this.userPermissions = userPermissions;
    }

    public String getName() {
        return name;
    }

    public UserPermissions getUserPermissions() {
        return userPermissions;
    }


}
