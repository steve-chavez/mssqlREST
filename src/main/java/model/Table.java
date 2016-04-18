
package model;

import java.util.*;

public class Table{

    public String name;
    public Map<String, String> columns = new HashMap<String, String>();

    @Override
    public String toString(){
        return name + columns.toString();
    }
}
