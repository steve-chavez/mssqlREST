
import org.rapidoid.http.HTTP;
import java.util.*;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;
import static com.mscharhag.oleaster.matcher.Matchers.expect;
import com.mscharhag.oleaster.runner.OleasterRunner;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;

@RunWith(OleasterRunner.class)
public class QueryTest {

    static final String BEARER = "Bearer "+ 
    "eyJhbGciOiJIUzI1NiJ9."+
    "eyJyb2xlIjoiYWRtaW4iLCJhcGVsbGlkbyI6IlJleWVzIiwibm"+
    "9tYnJlIjoiRWR1YXJkbyIsImVtYWlsIjoiZWR1cmV5QGdtYWlsLmNvbSJ9"+
    ".GVctZPoEaHNViROzGfdkkiOSpbgJAP16woePRqHOlhk";

    {
        describe("GET resource", () -> {
            describe("With no filtering", () -> {
                it("should respond with a json array", () -> {
                    String res =  HTTP.get("http://localhost:9090/item")
                        .headers(new HashMap<String,String>(){{ 
                            put("Authorization", BEARER); 
                        }}).fetch();
                    System.out.println(res);
                    expect(res).toEqual("[{\"id\":1},{\"id\":2},{\"id\":3},{\"id\":4},{\"id\":5}]");
                });
            });
        });
    }
}

