import org.rapidoid.http.*;
import java.util.*;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;
import static com.mscharhag.oleaster.matcher.Matchers.expect;
import com.mscharhag.oleaster.runner.OleasterRunner;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;

@RunWith(OleasterRunner.class)
public class CorsTest {

  {
    describe("CORS", () -> {
      it("should return cors headers when Origin is specified", () -> {
        HttpResp res =  HTTP.get("http://localhost:9090/projects")
          .header("Origin", "http://example.org")
          .execute();
        expect(res.code()).toEqual(200);
        expect(res.headers().get("Access-Control-Allow-Origin")).toEqual("*");
        expect(res.headers().get("Access-Control-Allow-Methods")).toEqual("GET,POST,PUT,PATCH,DELETE,OPTIONS");
      });
    });
    //TODO: tests for OPTIONS
  }
}
