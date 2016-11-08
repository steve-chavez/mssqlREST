
import java.io.*;
import java.util.*;
import org.yaml.snakeyaml.*;

public class Util{

  public static class Config{
    String url, user, password; 
    Integer port;
    String defaultRole, secret; 
    Optional<String> origin;
    Optional<List<String>> jwtRoutines;
  }

  public static Optional<Config> fromYaml(String path){
    try{
      Map<String, Object> vals = (Map<String, Object>)new Yaml()
        .load(new FileInputStream(new File(path)));

      Config config = new Config();
      config.url = vals.get("url").toString(); 
      config.user = vals.get("user").toString(); 
      config.password = vals.get("password").toString(); 
      config.port = (Integer)vals.get("port"); 
      config.defaultRole = vals.get("defaultRole").toString();
      config.secret = vals.get("secret").toString(); 
      config.origin = Optional.ofNullable(vals.get("origin").toString()); 
      config.jwtRoutines = Optional.ofNullable((List<String>)vals.get("jwtRoutines"));
      return Optional.of(config);
    } catch (Exception e){
      return Optional.empty();
    }
  }
}
