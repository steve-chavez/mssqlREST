
import org.rapidoid.http.*;
import java.util.*;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;
import static com.mscharhag.oleaster.matcher.Matchers.expect;
import com.mscharhag.oleaster.runner.OleasterRunner;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;

@RunWith(OleasterRunner.class)
public class QueryTest {

  {
    describe("POST resource", () -> {
      it("should succeed for json payload", () -> {
        HttpResp res =  HTTP.post("http://localhost:9090/projects")
          .body("{\"id\": 4, \"name\": \"project 4\"}".getBytes())
          .execute();
        expect(res.code()).toEqual(200);
      });

      it("should succeed for empty json payload", () -> {
        HttpResp res =  HTTP.post("http://localhost:9090/items")
          .body("{}".getBytes())
          .execute();
        expect(res.code()).toEqual(200);
      });

      it("should succeed for empty payload", () -> {
        HttpResp res =  HTTP.post("http://localhost:9090/items")
          .body("".getBytes())
          .execute();
        expect(res.code()).toEqual(200);
      });
    });

    describe("GET resource", () -> {
      describe("With no filtering", () -> {
        it("should respond with a json array", () -> {
          HttpResp res =  HTTP.get("http://localhost:9090/items").execute();
          expect(res.body()).toEqual("[{\"id\":1},{\"id\":2},{\"id\":3},{\"id\":4},{\"id\":5}]");
          expect(res.code()).toEqual(200);
        });
      });
    });
  }
}
