
import org.rapidoid.http.HTTP;
import java.util.*;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;
import static com.mscharhag.oleaster.matcher.Matchers.expect;
import com.mscharhag.oleaster.runner.OleasterRunner;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;

@RunWith(OleasterRunner.class)
public class QueryTest {

  //Add quote table name tests
  {
    describe("GET resource", () -> {
      describe("With no filtering", () -> {
        it("should respond with a json array", () -> {
          String res =  HTTP.get("http://localhost:9090/items").fetch();
          expect(res).toEqual("[{\"id\":1},{\"id\":2},{\"id\":3}]");
        });
      });
    });
  }
}
