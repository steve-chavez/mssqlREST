/*
* Class that:
*
* - Reads the config file and starts the server
* - Translates the client request to a SQL query
* - Performs JWT signing on the config specified SQL routines
* - Parses and validates the incomding JWT from the client
*/
package mssqlrest;

import spark.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;
import javax.servlet.http.*;

import com.zaxxer.hikari.*;

import io.jsonwebtoken.*;

import fj.data.Either;

import java.io.*;
import java.nio.file.*;

import org.slf4j.LoggerFactory;

import static mssqlrest.Structure.*;

public class ApplicationServer {

  static final String COOKIE_NAME = "mssqlrest-jwt";

  private static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ApplicationServer.class);

  public static void main(String[] args){

      Optional<Configurator.Config> config = Configurator.fromYaml(args[0]);

      if(!config.isPresent()){
        System.out.println("Config file doesn't exist or is invalid");
        System.exit(0);
      }

      HikariDataSource ds = new HikariDataSource();
      ds.setJdbcUrl(config.get().url);
      ds.setUsername(config.get().user);
      ds.setPassword(config.get().password);

      Spark.port(config.get().port);

      QueryExecuter queryExecuter = new QueryExecuter(config.get().schema, ds);

      String defaultRole = config.get().defaultRole;

      String secret = config.get().secret;

      List<String> jwtRoutines = config.get().jwtRoutines
        .orElse(new ArrayList<String>());

      Spark.get("/", (request, response) -> {
          LOGGER.info(request.requestMethod() + ": " + request.url());
          Optional<String> tableQueryParam = Parsers.tableQueryParam(request.queryMap());
          if(tableQueryParam.isPresent()){
            Either<Object, Object> result = queryExecuter.selectTableMetaData(tableQueryParam.get(),
                    getRole(secret, request, defaultRole));
            if(result.isRight()){
                response.type("application/json");
                response.status(200);
                return result.right().value().toString();
            }else{
                response.type("application/json");
                response.status(400);
                return result.left().value().toString();
            }
          }else{
            Either<Object, Object> result = queryExecuter.selectAllPrivilegedTables(
                    getRole(secret, request, defaultRole));
            if(result.isRight()){
                response.type("application/json");
                response.status(200);
                return result.right().value().toString();
            }else{
                response.status(400);
                return result.left().value().toString();
            }
          }
      });

      Spark.get("/:table", (request, response) -> {
          LOGGER.info(request.requestMethod() + ": " + request.url());

          Parsers.QueryParams queryParams = new Parsers.QueryParams(request.queryMap());

          Optional<String> prefer = Optional.ofNullable(request.headers("Prefer"));
          Optional<String> accept = Optional.ofNullable(request.headers("Accept"));
          Boolean singular = prefer.isPresent() && prefer.get().equals("plurality=singular");

          String table = request.params(":table");
          Format format = accept.map(x -> toFormat(x)).orElse(Format.JSON);

          response.type(toMediaType(format));

          if(queryParams.select.isLeft() || queryParams.order.isLeft() || queryParams.filters.isLeft()){
            response.type("application/json");
            response.status(400);
            return queryParams.select.isLeft() ? queryParams.select.left().value() :
                   queryParams.order.isLeft() ? queryParams.order.left().value() : queryParams.filters.left().value();
          }else {
              Either<Object, Object> result = queryExecuter.selectFrom(table,
                      queryParams.filters.right().value(), queryParams.select.right().value(),
                      queryParams.order.right().value(), singular, format, getRole(secret, request, defaultRole));
              if(result.isRight()){
                  if(format == Format.XLSX){
                      response.header("Content-Disposition", "attachment");
                      response.status(200);
                      HttpServletResponse raw = response.raw();
                      raw.getOutputStream().write((byte[])result.right().value());
                      raw.getOutputStream().flush();
                      raw.getOutputStream().close();
                      return raw;
                  }
                  else{
                    response.status(200);
                    return result.right().value().toString();
                  }
              }else{
                  response.type("application/json");
                  response.status(400);
                  return result.left().value().toString();
              }
          }
      });

      Spark.post("/:table", (request, response) -> {
          LOGGER.info(request.requestMethod() + ": " + request.url());

          Optional<String> contentType = Optional.ofNullable(request.headers("Content-Type"));

          Format format = contentType.map(x -> toFormat(x)).orElse(Format.JSON);

          response.type(toMediaType(format));

          switch(format){
            case JSON: {
              Either<String, Map<String, String>> parsedMap = Parsers.Body.jsonToMap(request.body());
              if(parsedMap.isRight()){
                Either<Object, Object> result = queryExecuter.insertInto(request.params(":table"),
                        parsedMap.right().value(), getRole(secret, request, defaultRole));
                if(result.isRight()){
                    response.status(200);
                    return result.right().value().toString();
                }else{
                    response.type("application/json");
                    response.status(500);
                    return result.left().value().toString();
                }
              }else{
                  response.type("application/json");
                  response.status(400);
                  return Errors.messageToJson(parsedMap.left().value());
              }
            }
            case CSV: {
              Either<String, List<Map<String, String>>> parsedMap = Parsers.Body.csvToMap(request.body());
              if(parsedMap.isRight()){
                  Either<Object, Object> result = queryExecuter.batchInsertInto(request.params(":table"),
                          parsedMap.right().value(), getRole(secret, request, defaultRole));
                  if(result.isRight()){
                      response.status(200);
                      return result.right().value().toString();
                  }else{
                      response.type("application/json");
                      response.status(500);
                      return result.left().value().toString();
                  }
              }else{
                  response.type("application/json");
                  response.status(400);
                  return Errors.messageToJson(parsedMap.left().value());
              }
            }
            case XLSX: {
              Either<String, List<Map<String, String>>> parsedMap = Parsers.Body.xlsxToMap(request.raw());
              Either<Object, Object> result = queryExecuter.batchInsertInto(request.params(":table"),
                      parsedMap.right().value(), getRole(secret, request, defaultRole));
              if(parsedMap.isRight()){
                if(result.isRight()){
                    response.status(200);
                    return result.right().value().toString();
                }else{
                    response.type("application/json");
                    response.status(500);
                    return result.left().value().toString();
                }
              }else{
                response.type("application/json");
                response.status(400);
                return Errors.messageToJson(parsedMap.left().value());
              }
            }
            default: {
              return "";
            }
          }
      });

      Spark.patch("/:table", (request, response) -> {
          LOGGER.info(request.requestMethod() + ": " + request.url());

          Either<String, Map<String, String>> parsedMap = Parsers.Body.jsonToMap(request.body());
          if(parsedMap.isRight()){
            Parsers.QueryParams queryParams = new Parsers.QueryParams(request.queryMap());
            Either<Object, Object> result = queryExecuter.updateSet(request.params(":table"),
                    parsedMap.right().value(), queryParams.filters.right().value(), getRole(secret, request, defaultRole));
            if(result.isRight()){
                response.type("application/json");
                response.status(200);
                return result.right().value().toString();
            }else{
                response.type("application/json");
                response.status(500);
                return result.left().value().toString();
            }
          }
          else{
            response.type("application/json");
            response.status(400);
            return Errors.messageToJson(parsedMap.left().value());
          }
      });

      Spark.delete("/:table", (request, response) -> {
          LOGGER.info(request.requestMethod() + ": " + request.url());

          Parsers.QueryParams queryParams = new Parsers.QueryParams(request.queryMap());
          Either<Object, Object> result = queryExecuter.deleteFrom(request.params(":table"),
                  queryParams.filters.right().value(), getRole(secret, request, defaultRole));
          if(result.isRight()){
              response.type("application/json");
              response.status(200);
              return result.right().value().toString();
          }else{
              response.type("application/json");
              response.status(400);
              return result.left().value().toString();
          }
      });

      Spark.post("/rpc/:routine", (request, response) -> {
          LOGGER.info(request.requestMethod() + ": " + request.url());

          Optional<String> accept = Optional.ofNullable(request.headers("Accept"));

          Format format = accept.map(x -> toFormat(x)).orElse(Format.JSON);

          response.type(toMediaType(format));

          Either<String, Map<String, String>> parsedMap = Parsers.Body.jsonToMap(request.body());
          if(parsedMap.isRight()){
            Either<Object, Object> result = queryExecuter.callRoutine(request.params(":routine"),
                parsedMap.right().value(), format, false, getRole(secret, request, defaultRole));
            if(result.isRight()){
                if(jwtRoutines.contains(request.params(":routine")))
                    return Jwts.builder()
                        .setPayload(result.right().value().toString())
                        .signWith(SignatureAlgorithm.HS256, secret).compact();
                else if(format == Format.XLSX){
                    response.header("Content-Disposition", "attachment");
                    response.status(200);
                    HttpServletResponse raw = response.raw();
                    raw.getOutputStream().write((byte[])result.right().value());
                    raw.getOutputStream().flush();
                    raw.getOutputStream().close();
                    return raw;
                } else {
                  response.status(200);
                  return result.right().value().toString();
                }
            }else{
                response.type("application/json");
                response.status(400);
                return result.left().value().toString();
            }
          }else{
              response.type("application/json");
              response.status(400);
              return Errors.messageToJson(parsedMap.left().value());
          }
      });

      // CORS headers
      Spark.before(new Filter() {
          @Override
          public void handle(Request request, Response response) {
            Optional<String> origin = Optional.ofNullable(request.headers("Origin"));
            if(origin.isPresent()){
              response.header("Access-Control-Allow-Origin", "*");
              response.header("Access-Control-Allow-Credentials", "true");
              response.header("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
              response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, Prefer");
            }
          }
      });

      // The following are needed for pre-flight requests
      Spark.options("/", (request, response) -> {
          LOGGER.info(request.requestMethod() + ": " + request.url());
          response.status(200);
          return "";
      });

      Spark.options("/:table", (request, response) -> {
          LOGGER.info(request.requestMethod() + ": " + request.url());
          response.status(200);
          return "";
      });

      Spark.options("/rpc/:routine", (request, response) -> {
          LOGGER.info(request.requestMethod() + ": " + request.url());
          response.status(200);
          return "";
      });
  }

  private static String getRole(String secret, Request request, String defaultRole){
    return getRoleFromCookieOrHeader(secret, request).orElse(defaultRole);
  }

  private static Optional<String> getRoleFromCookieOrHeader(String secret, Request request){
      Optional<String> authorization = Optional.ofNullable(request.headers("Authorization"));
      Optional<String> cookie = Optional.ofNullable(request.cookie(COOKIE_NAME));
      if(cookie.isPresent()){
          return decodeGetRole(secret, cookie.get());
      }
      else if(authorization.isPresent()){
          Optional<String> bearer = obtainBearer(authorization.get());
          return decodeGetRole(secret, bearer.get());
      }
      else return Optional.empty();
  }

  private static Optional<String> decodeGetRole(String secret, String jwt){
      try{
          Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(jwt).getBody();
          return Optional.of(claims.get("role").toString());
      }catch(Exception e){
          return Optional.empty();
      }
  }

  private static Optional<String> obtainBearer(String s){
      if(s.isEmpty())
          return Optional.empty();
      else {
          String[] arr = s.split(" ");
          if(!arr[0].equalsIgnoreCase("Bearer"))
              return Optional.empty();
          else{
              if(arr.length == 1)
                  return Optional.empty();
              else
                  return Optional.of(arr[1]);
          }
      }
  }

}

