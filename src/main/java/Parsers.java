
import java.io.*;
import java.util.*;
import java.util.stream.*;
import javax.servlet.http.*;

import fj.data.Either;

import com.google.gson.reflect.TypeToken;
import com.google.gson.*;

import com.univocity.parsers.common.processor.*;
import com.univocity.parsers.conversions.*;
import com.univocity.parsers.csv.*;

import com.ebay.xcelite.sheet.*;
import com.ebay.xcelite.reader.*;
import com.ebay.xcelite.writer.*;
import com.ebay.xcelite.*;

import org.javafp.parsecj.*;
import org.javafp.data.*;
import static org.javafp.parsecj.Combinators.*;
import static org.javafp.parsecj.Text.*;

import spark.QueryParamsMap;

public class Parsers{

  static final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

  public static Either<String, Map<String, String>> jsonToMap(String body){
      Map<String, String> emptyMap = Collections.emptyMap();
      if(!body.isEmpty())
        try{
          return Either.right(gson.fromJson(body, new TypeToken<Map<String, String>>(){}.getType()));
        }catch(Exception e){
          System.out.println(e.toString());
          return Either.left("Could not parse JSON object");
        }
      else
        return Either.right(emptyMap);
  }

  public static Either<String, List<Map<String, String>>> csvToMap(String body){
      CsvParser parser = new CsvParser(new CsvParserSettings());
      List<String[]> rows = parser.parseAll(new StringReader(body));
      List<Map<String, String>> mappedValues =
          new ArrayList<Map<String, String>>();
      String[] headers = rows.get(0);
      for(int i=1; i < rows.size(); i++){
          String[] values = rows.get(i);
          Map<String, String> attr = new LinkedHashMap<String, String>();
          for(int j=0; j < headers.length; j++){
              attr.put(headers[j], values[j]);
          }
          mappedValues.add(attr);
      }
      if(!mappedValues.isEmpty())
        return Either.right(mappedValues);
      else
        return Either.left("Could not parse CSV payload");
  }

  public static Either<String, List<Map<String, String>>> xlsxToMap(HttpServletRequest hsr){
      try{
          List<Map<String, String>> mappedValues = new ArrayList();
          Xcelite xcelite = new Xcelite(hsr.getInputStream());
          XceliteSheet sheet = xcelite.getSheet("data_sheet");
          SheetReader<Collection<Object>> simpleReader = sheet.getSimpleReader();
          List<Collection<Object>> data =
              new ArrayList<Collection<Object>>(simpleReader.read());

          List<String> headers = data.get(0).stream()
             .map(object -> (object != null ? object.toString() : null))
             .collect(Collectors.toList());

          Integer headersSize = headers.size();
          Integer rowsSize = data.size();

          for (int i=1; i<rowsSize; i++){
              List<Object> values = new ArrayList(data.get(i));
              Map<String, String> value = new LinkedHashMap<String, String>();
              for(int j=0; j<headersSize; j++){
                  String header = headers.get(j);
                  if(header != null && !header.isEmpty())
                      value.put(header,
                              values.get(j)==null?null:values.get(j).toString());
              }
              mappedValues.add(value);
          }
          return Either.right(mappedValues);
      }catch(IOException ioe){
          System.out.println(ioe.toString());
          return Either.left("Could not parse XLSX payload");
      }
  }

  public static class QueryParams{

      public Either<String, List<String>> select;
      public Either<String, List<Structure.Order>> order;
      public Map<String, String> filters;

      private final Parser<Character, String> identifier = regex("[a-zA-Z0-9_]*");

      private final Parser<Character, IList<String>> selectP = identifier.sepBy(chr(','));

      private final Parser<Character, Optional<Structure.Order.Direction>> orderDirP =
        optionalOpt(choice(
          attempt(string(".asc").then(retn(Structure.Order.Direction.ASC))),
          attempt(string(".desc").then(retn(Structure.Order.Direction.DESC)))));

      private final Parser<Character, Structure.Order> orderItemP = identifier.bind(item -> orderDirP.bind(dir -> retn(new Structure.Order(item, dir))));
      private final Parser<Character, IList<Structure.Order>> orderP = orderItemP.sepBy1(chr(','));

      public QueryParams(QueryParamsMap map){
        Map<String, String> normalized = normalizeMap(map.toMap());
        this.select  = parseSelect(Optional.ofNullable(normalized.get("select")));
        this.order   = parseOrder(Optional.ofNullable(normalized.get("order")));
        this.filters = parseFilters(normalized);
      }

      private Either<String, List<String>> parseSelect(Optional<String> val){
        if(val.isPresent())
          try{
            return Either.right(IList.toList(selectP.parse(State.of(val.get())).getResult()));
          }catch(Exception e){
            return Either.left(Errors.messageToJson(e.toString()));
          }
        else
          return Either.right(Collections.emptyList());
      }

      private Either<String, List<Structure.Order>> parseOrder(Optional<String> val){
        if(val.isPresent())
          try{
            return Either.right(IList.toList(orderP.parse(State.of(val.get())).getResult()));
          }catch(Exception e){
            return Either.left(Errors.messageToJson(e.toString()));
          }
        else
          return Either.right(Collections.emptyList());
      }

      private Map<String, String> parseFilters(Map<String, String> map){
        Map<String, String> mapWithout = map.entrySet().stream()
            .filter( x -> !x.getKey().equalsIgnoreCase("order")&&
                          !x.getKey().equalsIgnoreCase("select"))
            .collect(Collectors.toMap( x -> x.getKey(), x -> x.getValue()));
        return mapWithout;
      }

      //Spark gets an array of query param values. This is because they consider this valid:
      //`/table?select=val1&select=val2` and they get an array of `select` values.
      //Just take the first value here.
      private Map<String, String> normalizeMap(Map<String, String[]> map){
          return map.entrySet().stream().collect(
                  Collectors.toMap(
                      Map.Entry::getKey, e -> e.getValue()[0]
          ));
      }
  }

}
