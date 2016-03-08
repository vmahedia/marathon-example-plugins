package mesosphere.marathon.example.plugin.javaauth;

import mesosphere.marathon.plugin.PathId;
import mesosphere.marathon.plugin.auth.AuthorizedAction;
import mesosphere.marathon.plugin.auth.Authorizer;
import mesosphere.marathon.plugin.auth.Identity;
import mesosphere.marathon.plugin.http.HttpRequest;
import mesosphere.marathon.plugin.http.HttpResponse;
import org.apache.log4j.Logger;

public class JavaAuthorizer implements Authorizer {

    @Override
    public <Resource> boolean isAuthorized(Identity principal, AuthorizedAction<Resource> action, Resource resource) {
        return principal instanceof JavaIdentity &&
                resource instanceof PathId &&
                isAuthorized((JavaIdentity) principal, Action.byAction(action), (PathId) resource);

    }

    private boolean isAuthorized(JavaIdentity principal, Action action, PathId path) {
        log.debug(String.format("Authorizing user - %s for action - %s on Path - %s", principal.getName(),
                action.toString(), path.toString()));
        switch (action) {
            case CreateAppOrGroup:
                return principal.getUserPermissions().isAuthorized("create", path.toString());
            case UpdateAppOrGroup:
                return principal.getUserPermissions().isAuthorized("update", path.toString());
            case DeleteAppOrGroup:
                return principal.getUserPermissions().isAuthorized("delete", path.toString());
            case ViewAppOrGroup:
                return principal.getUserPermissions().isAuthorized("view", path.toString());
            case KillTask:
                return principal.getUserPermissions().isAuthorized("kill", path.toString());
            default:
                return false;
        }
    }

    @Override
    public void handleNotAuthorized(Identity principal, HttpRequest request, HttpResponse response) {
        response.status(403);
        response.body("application/json", "{\"problem\": \"Not Authorized to perform this action!\"}".getBytes());
    }

    static Logger log = Logger.getLogger(JavaAuthorizer.class.getName());
}
