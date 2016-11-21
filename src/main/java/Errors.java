
import java.sql.*;
import java.util.*;
import com.google.gson.*;

public class Errors{

  static final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

  public static String exceptionToJson(SQLException e){
      Map<String, Object> map = new HashMap();
      map.put("message", e.getMessage());
      map.put("code", e.getErrorCode());
      return gson.toJson(map);
  }

  public static String messageToJson(String message){
      Map<String, String> map = new HashMap();
      map.put("message", message);
      return gson.toJson(map);
  }

  public static String missingError(){
      return messageToJson("The resource doesn't exist or permission was denied");
  }
}
