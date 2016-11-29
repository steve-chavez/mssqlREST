package mssqlrest;

import java.util.*;
import java.sql.*;
import org.junit.runner.notification.Failure;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MainTest{

  private static boolean failed = false;

  public static void main(String[] args){
    ApplicationServer.main(args);
    List<Class> tests = new ArrayList();
    tests.add(CorsTest.class);
    tests.add(QueryTest.class);
    tests.add(MetadataTest.class);

    for (Class test : tests){
      runTests(test);
    }

    if(failed)
      System.exit(1);
    else
      System.exit(0);
  }

  private static void runTests(Class test){
    Result result = JUnitCore.runClasses(test);
    if(!result.getFailures().isEmpty())
      failed = true;
    for (Failure failure : result.getFailures()){
      System.out.println(failure.toString());
    }
  }

}

