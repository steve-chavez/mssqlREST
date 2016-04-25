
import java.util.*;

public class Structure{

    public static class Table{

        public String name;
        public Map<String, String> columns = new HashMap<String, String>();

        @Override
        public String toString(){
            return name + columns.toString();
        }

    }

    public static class Routine{
        public String name; 
        public String type; 
        public String returnType;
        public Map<String, Parameter> parameters = new HashMap<String, Parameter>();
        public Map<String, String> returnColumns = new HashMap<String, String>();

        @Override
        public String toString(){
            return name + " : " + 
                String.join(",", Arrays.asList(type, returnType)) +
                "; parameters : " + parameters.toString();
        }
    }

    public static class Parameter{
        public String name; 
        public Integer ordinalPosition; 
        public String dataType;
        public String parameterMode;

        @Override
        public String toString(){
            return name + " : " + 
                String.join(",", Arrays.asList(dataType,
                    ordinalPosition.toString(), parameterMode));
        }
    }
}
