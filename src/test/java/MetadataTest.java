import org.rapidoid.http.*;
import java.util.*;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;
import static com.mscharhag.oleaster.matcher.Matchers.expect;
import com.mscharhag.oleaster.runner.OleasterRunner;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;

@RunWith(OleasterRunner.class)
public class MetadataTest {

  {
    describe("Metadata spec at root path", () -> {
      it("should return all tables and privileges", () -> {
        HttpResp res =  HTTP.get("http://localhost:9090/").execute();
        expect(res.code()).toEqual(200);
        expect(res.body()).toEqual("[{\"updateable\":true,\"selectable\":true,\"name\":\"entities\",\"deletable\":true,\"insertable\":true},{\"updateable\":true,\"selectable\":true,\"name\":\"items\",\"deletable\":true,\"insertable\":true},{\"updateable\":true,\"selectable\":true,\"name\":\"projects\",\"deletable\":true,\"insertable\":true}]");
      });

      it("should return a table columns data at /?table=<name>", () -> {
        HttpResp res =  HTTP.get("http://localhost:9090/?table=projects").execute();
        expect(res.code()).toEqual(200);
        expect(res.body()).toEqual("[{\"default\":null,\"nullable\":false,\"precision\":10,\"name\":\"id\",\"scale\":0,\"type\":\"int\",\"max_length\":null},{\"default\":null,\"nullable\":false,\"precision\":null,\"name\":\"name\",\"scale\":null,\"type\":\"varchar\",\"max_length\":50}]");
      });
    });
  }
}
