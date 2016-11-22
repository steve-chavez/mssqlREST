
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

public class ApplicationServer {

    static final String COOKIE_NAME = "x-rest-jwt";

    public static void main(String[] args){

        Optional<Configurator.Config> config = Configurator.fromYaml(args[0]);

        if(!config.isPresent()){
          System.out.println("Config file doesn't exist or is an invalid YAML format");
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

        Optional<String> optOrigin = config.get().origin;

        List<String> jwtRoutines = config.get().jwtRoutines
          .orElse(new ArrayList<String>());

        // Headers:
        // Plurality: singular, plural
        // Resource : definition, data
        Spark.before(new Filter() {
            @Override
            public void handle(Request request, Response response) {
                if(optOrigin.isPresent()){
                    response.header("Access-Control-Allow-Origin", optOrigin.get().toString());
                    response.header("Access-Control-Allow-Credentials", "true");
                }else
                    response.header("Access-Control-Allow-Origin", "*");
                response.header("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
                response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Plurality, Resource");
            }
        });

        //---------Pre flight---------
        Spark.options("/", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            response.status(200);
            return "";
        });

        Spark.options("/:table", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            response.status(200);
            return "";
        });

        Spark.options("/rpc/:routine", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            response.status(200);
            return "";
        });

        Spark.get("/", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
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
        });

        Spark.get("/:table", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());

            Parser.QueryParams queryParams = new Parser.QueryParams(request.queryMap().toMap());

            Optional<String> resource = Optional.ofNullable(request.headers("Resource"));
            Optional<String> plurality = Optional.ofNullable(request.headers("Plurality"));
            Optional<String> accept = Optional.ofNullable(request.headers("Accept"));
            Boolean singular = plurality.isPresent() && plurality.get().equals("singular");

            String table = request.params(":table");
            Structure.Format format = accept.map(x -> Structure.toFormat(x)).orElse(Structure.Format.JSON);

            if(format == Structure.Format.OTHER){
              response.status(406);
              return null;
            } else {
              response.type(Structure.toMediaType(format));
              if(!resource.isPresent()){
                  Either<Object, Object> result = queryExecuter.selectFrom(table,
                          queryParams.filters, queryParams.select, queryParams.order, singular, format,
                          getRole(secret, request, defaultRole));
                  if(result.isRight()){
                      if(format == Structure.Format.XLSX){
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
              }else{
                  Either<Object, Object> result = queryExecuter.selectTableMetaData(table,
                          singular, getRole(secret, request, defaultRole));
                  if(result.isRight()){
                      response.type("application/json");
                      response.status(200);
                      return result.right().value().toString();
                  }else{
                      response.type("application/json");
                      response.status(400);
                      return result.left().value().toString();
                  }
              }
            }
        });

        Spark.post("/:table", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());

            Optional<String> contentType = Optional.ofNullable(request.headers("Content-Type"));
            Optional<String> resource = Optional.ofNullable(request.headers("Resource"));

            Structure.Format format = contentType.map(x -> Structure.toFormat(x)).orElse(Structure.Format.JSON);

            response.type(Structure.toMediaType(format));

            switch(format){
              case JSON: {
                Either<String, Map<String, String>> parsedMap = Parser.jsonToMap(request.body());
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
                Either<String, List<Map<String, String>>> parsedMap = Parser.csvToMap(request.body());
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
                Either<String, List<Map<String, String>>> parsedMap = Parser.xlsxToMap(request.raw());
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
              default:
                response.status(406);
                return null;
            }
        });

        Spark.patch("/:table", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            Optional<String> resource = Optional.ofNullable(request.headers("Resource"));

            Either<String, Map<String, String>> parsedMap = Parser.jsonToMap(request.body());
            if(parsedMap.isRight()){
              Parser.QueryParams queryParams = new Parser.QueryParams(request.queryMap().toMap());
              Either<Object, Object> result = queryExecuter.updateSet(request.params(":table"),
                      parsedMap.right().value(), queryParams.filters, getRole(secret, request, defaultRole));
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
            System.out.println(request.requestMethod() + " : " + request.url());
            Optional<String> resource = Optional.ofNullable(request.headers("Resource"));

            Parser.QueryParams queryParams = new Parser.QueryParams(request.queryMap().toMap());
            Either<Object, Object> result = queryExecuter.deleteFrom(request.params(":table"),
                    queryParams.filters, getRole(secret, request, defaultRole));
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
            System.out.println(request.requestMethod() + " : " + request.url());
            Optional<String> resource = Optional.ofNullable(request.headers("Resource"));
            Optional<String> accept = Optional.ofNullable(request.headers("Accept"));

            Structure.Format format = accept.map(x -> Structure.toFormat(x)).orElse(Structure.Format.JSON);
            if(format == Structure.Format.OTHER){
              response.status(406);
              return null;
            } else {
              response.type(Structure.toMediaType(format));
              Either<String, Map<String, String>> parsedMap = Parser.jsonToMap(request.body());
              if(parsedMap.isRight()){
                Either<Object, Object> result = queryExecuter.callRoutine(request.params(":routine"),
                    parsedMap.right().value(), format, false, getRole(secret, request, defaultRole));
                if(result.isRight()){
                    if(jwtRoutines.contains(request.params(":routine")))
                        return Jwts.builder()
                            .setPayload(result.right().value().toString())
                            .signWith(SignatureAlgorithm.HS256, secret).compact();
                    else if(format == Structure.Format.XLSX){
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
            }
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

