
import java.io.*;
import java.util.*;
import java.util.stream.*;
import javax.servlet.http.*;

import com.google.gson.reflect.TypeToken;
import com.google.gson.*;

import com.univocity.parsers.common.processor.*;
import com.univocity.parsers.conversions.*;
import com.univocity.parsers.csv.*;

import com.ebay.xcelite.sheet.*;
import com.ebay.xcelite.reader.*;
import com.ebay.xcelite.writer.*;
import com.ebay.xcelite.*;

public class Parser{

  static final Gson gson = new GsonBuilder().serializeNulls().create();

  public static Map<String, String> jsonToMap(String body){
      Map<String, String> emptyMap = Collections.emptyMap();
      if(!body.isEmpty())
        return gson.fromJson(body, new TypeToken<Map<String, String>>(){}.getType());
      else
        return emptyMap;
  }

  public static List<Map<String, String>> csvToMap(String body){
      CsvParser parser = new CsvParser(new CsvParserSettings());
      List<String[]> rows = parser.parseAll(new StringReader(body));
      List<Map<String, String>> mappedValues =
          new ArrayList<Map<String, String>>();
      String[] headers = rows.get(0);
      int length = headers.length;
      for(int i=1; i < rows.size(); i++){
          String[] values = rows.get(i);
          Map<String, String> attr = new LinkedHashMap<String, String>();
          for(int j=0; j<length; j++){
              attr.put(headers[j], values[j]);
          }
          mappedValues.add(attr);
      }
      return mappedValues;
  }

  public static List<Map<String, String>> xlsxToMap(HttpServletRequest hsr){
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
          return mappedValues;
      }catch(IOException ioe){
          System.out.println(ioe.getMessage());
          return null;
      }
  }

  public static class QueryParams{

      public Optional<String> select;
      public Optional<String> order;
      public Map<String, String> filters;

      public QueryParams(Map<String, String[]> map){
          Map<String, String> normalized = normalizeMap(map);
          this.select  = parseSelect(normalized);
          this.order   = parseOrder(normalized);
          this.filters = parseFilters(normalized);
      }

      //Spark gets an array of query param values. I guess because they consider this valid:
      //`/table?select=val1&select=val2` and they get an array of `select` values.
      //Just take the first value here.
      private Map<String, String> normalizeMap(Map<String, String[]> map){
          return map.entrySet().stream().collect(
                  Collectors.toMap(
                      Map.Entry::getKey, e -> e.getValue()[0]
          ));
      }

      private Optional<String> parseSelect(Map<String, String> map){
        String optSelect = map.entrySet().stream()
            .filter( x -> x.getKey().equalsIgnoreCase("select"))
            .map( x -> x.getValue())
            .collect(Collectors.joining());
        return optSelect.isEmpty()?Optional.empty():Optional.of(optSelect);
      }

      private Optional<String> parseOrder(Map<String, String> map){
        String optOrder = map.entrySet().stream()
            .filter( x -> x.getKey().equalsIgnoreCase("order"))
            .map( x -> x.getValue())
            .collect(Collectors.joining());
        return optOrder.isEmpty()?Optional.empty():Optional.of(optOrder);
      }

      private Map<String, String> parseFilters(Map<String, String> map){
        Map<String, String> mapWithout = map.entrySet().stream()
            .filter( x -> !x.getKey().equalsIgnoreCase("order")&&
                          !x.getKey().equalsIgnoreCase("select"))
            .collect(Collectors.toMap( x -> x.getKey(), x -> x.getValue()));
        return mapWithout;
      }
  }

}
