package mesosphere.marathon.example.plugin.javaauth;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import akka.dispatch.ExecutionContexts;
import akka.dispatch.Futures;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.nashorn.internal.parser.JSONParser;
import mesosphere.marathon.example.plugin.javaauth.data.Permission;
import mesosphere.marathon.example.plugin.javaauth.data.UserPermissions;
import mesosphere.marathon.example.plugin.javaauth.data.Users;
import mesosphere.marathon.plugin.auth.Authenticator;
import mesosphere.marathon.plugin.auth.Identity;
import mesosphere.marathon.plugin.http.HttpRequest;
import mesosphere.marathon.plugin.http.HttpResponse;
import mesosphere.marathon.plugin.plugin.PluginConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.net.SyslogAppender;
import play.api.libs.json.JsObject;
import play.api.libs.json.JsString;
import play.api.libs.json.JsValue;
import play.libs.Json;
import scala.Option;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

import javax.naming.AuthenticationException;


public class JavaAuthenticator implements Authenticator, PluginConfiguration {

    private final ExecutionContext EC = ExecutionContexts.fromExecutorService(Executors.newSingleThreadExecutor());
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final long DEFAULT_INTERVAL_IN_SECONDS = 30;
    private AuthConfigUpdaterTask authConfigUpdaterTask;

    @Override
    public Future<Option<Identity>> authenticate(HttpRequest request) {
        return Futures.future(() -> Option.apply(doAuth(request)), EC);
    }

    private Identity doAuth(HttpRequest request) {
        try {
            Option<String> header = request.header("Authorization").headOption();
            if (header.isDefined() && header.get().startsWith("Basic ")) {
                String encoded = header.get().replaceFirst("Basic ", "");
                String decoded = new String(Base64.getDecoder().decode(encoded), "UTF-8");
                String[] userPass = decoded.split(":", 2);
                if (userPass.length == 2) {
                    return doAuth(userPass[0], userPass[1]);
                }
            }
        } catch (Exception ex) { /* do not authenticate in case of exception */ }
        return null;
    }

    /**
     * Authenticate, if the username matches the password.
     */
    private Identity doAuth(String username, String password) {
        UserPermissions userPermissions;
        try {
            userPermissions = authConfigUpdaterTask.getAuthPermissions().
                    get().authenticated(username, password);
            return new JavaIdentity(username, userPermissions);
        } catch (IOException | AuthenticationException e) {
            log.error(e);
            return null;
        }
    }

    @Override
    public void handleNotAuthenticated(HttpRequest request, HttpResponse response) {
        response.status(401);
        response.header("WWW-Authenticate", "Basic realm=\"Marathon: Username==Password\"");
        response.body("application/json", "{\"problem\": \"Not Authenticated!\"}".getBytes());
    }

    @Override
    public void initialize(JsObject configuration) {
        long interval = DEFAULT_INTERVAL_IN_SECONDS;
        Map<String, JsValue> conf = scala.collection.JavaConversions.mapAsJavaMap(configuration.value());
        String filePathKey = "permissions-conf-file";
        String intervalKey = "permissions-conf-file-check-interval-seconds";
        log.info(conf.toString());
        String filePath = "";
        if(conf.containsKey(filePathKey)) {
            filePath = conf.get(filePathKey).toString().replace("\"","");
        }
        if(conf.containsKey(intervalKey)){
            interval = Long.parseLong(conf.get(intervalKey).toString());
            if(interval <= 0) {
                interval = DEFAULT_INTERVAL_IN_SECONDS;
            }
        }
        try {
            authConfigUpdaterTask = new AuthConfigUpdaterTask(filePath);
            scheduler.scheduleAtFixedRate(authConfigUpdaterTask, interval, interval, TimeUnit.SECONDS);
        } catch (IOException ioe) {
            log.fatal("Unable to Initialize Auth plugin, Exiting", ioe);
            System.exit(-1);
        }
    }
    static Logger log = Logger.getLogger(JavaAuthenticator.class.getName());
}
