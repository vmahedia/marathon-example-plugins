package mesosphere.marathon.example.plugin.javaauth;


import com.fasterxml.jackson.databind.ObjectMapper;
import mesosphere.marathon.example.plugin.javaauth.data.Users;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by vinitmahedia on 3/7/16.
 */
public class AuthConfigUpdaterTask implements Runnable {
    private String authConfigFilePath;
    private long lastUpdatedTimeInMillis;
    private AtomicReference<Users> authPermissions = new AtomicReference<Users>();

    public AuthConfigUpdaterTask(String authConfigFilePath) throws IOException {
        this.authConfigFilePath = authConfigFilePath;
        this.lastUpdatedTimeInMillis = System.currentTimeMillis();
        this.authPermissions.set(parse());
    }

    public AtomicReference<Users> getAuthPermissions() throws IOException{
        return authPermissions;
    }

    private Users parse() throws IOException{
        ObjectMapper objectMapper = new ObjectMapper();
        Users users = objectMapper.readValue(new File(authConfigFilePath), Users.class);
        log.debug(String.format("User Permissions Configuration - %s ", users.toString()));
        return users;
    }

    @Override
    public void run() {
        File authConfigFile = new File(authConfigFilePath);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        log.debug(String.format("File's Last Updated Time is - %s, Updaters Last Updated Time is - %s",
                sdf.format(authConfigFile.lastModified()), sdf.format(lastUpdatedTimeInMillis)));

        if (authConfigFile.lastModified() > lastUpdatedTimeInMillis) {
            lastUpdatedTimeInMillis = authConfigFile.lastModified();
            try {
                log.info(String.format("User Permission Config file - %s updated, parsing...", authConfigFilePath));
                Users users = parse();
                if(users != null) {
                    authPermissions.set(users);
                }
            } catch (IOException ioe) {
                // We can live with previously parsed permissions even if this fails so do not bailout here
                // Print a warning that there is a problem parsing new permissions even though it's updated
                log.error(String.format("Error parsing the Updated User Authorization Configuration, " +
                        "using existing permissions," +
                        "please check if the Json is formatted or if file exists at configured location - %s ",
                        authConfigFilePath), ioe);
            }
        }
    }
    static Logger log = Logger.getLogger(AuthConfigUpdaterTask.class.getName());

}
