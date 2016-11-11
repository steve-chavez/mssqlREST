
import java.util.*;
import java.sql.*;
import org.junit.runner.notification.Failure;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MainTest{

  public static void main(String[] args){
    ApplicationServer.main(args);
    List<Class> tests = new ArrayList();
    tests.add(QueryTest.class);

    for (Class test : tests){
      runTests(test);
    }

    System.exit(0);
  }

  private static void runTests(Class test){
    Result result = JUnitCore.runClasses(test);
    for (Failure failure : result.getFailures()){
      System.out.println(failure.toString());
    }
  }

  //private static void setupDatabase(String path){
    //Optional<Configurator.Config> config = Configurator.fromYaml(path);
    //try{
      //Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
      //Connection conn = DriverManager.getConnection(config.get().url, 
          //config.get().user, config.get().password);
      //Statement statement=conn.createStatement();
      //String ddl = new String(
          //Files.readAllBytes(Paths.get("src/test/resources/ddl.sql")));
      //String dml = new String(
          //Files.readAllBytes(Paths.get("src/test/resources/dml.sql")));
      //String a = new String(
          //Files.readAllBytes(Paths.get("a.sql")));
      //while (rs.next())
        //System.out.println(rs.getString(1));
      //Runtime.getRuntime().exec("mssql -u sa -p admin01 -q \"create database dd\"");
      //ProcessBuilder pb = new ProcessBuilder(
          //"mssql", "-u", "sa", "-p", "admin01", "-q", ddl);
      //Process p = pb.start();
    //}catch(Exception e){
      //System.out.println("Couldn't setup database: " + e.toString());
    //}
  //}

}

