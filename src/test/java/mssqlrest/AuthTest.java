package mssqlrest;

import org.rapidoid.http.*;
import java.util.*;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;
import static com.mscharhag.oleaster.matcher.Matchers.expect;
import com.mscharhag.oleaster.runner.OleasterRunner;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;

@RunWith(OleasterRunner.class)
public class AuthTest {

  {
    describe("Auth", () -> {
      it("should return the signed JWT", () -> {
        HttpResp res =  HTTP.post("http://localhost:9090/rpc/login")
          .body("{'email': 'johndoe@company.com', 'password': 'johnpass'}".getBytes())
          .execute();
        expect(res.code()).toEqual(200);
        expect(res.body()).toEqual("eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoid2VidXNlciIsImVtYWlsIjoiam9obmRvZUBjb21wYW55LmNvbSJ9.tJf3EOV_bdLgvKBo-NuYbR_JSUPEerrU6bJh9xOoTlQ");
      });

      it("should reject the request on the privileged resource", () -> {
        HttpResp res =  HTTP.get("http://localhost:9090/privileged_projects?select=id")
          .execute();
        expect(res.code()).toEqual(400);
        expect(res.body()).toEqual("{\"message\":\"The resource doesn't exist or permission was denied\"}");
      });

      it("should allow the request when the JWT is specified in the Authorization header", () -> {
        HttpResp res =  HTTP.get("http://localhost:9090/privileged_projects?select=id")
          .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoid2VidXNlciIsImVtYWlsIjoiam9obmRvZUBjb21wYW55LmNvbSJ9.tJf3EOV_bdLgvKBo-NuYbR_JSUPEerrU6bJh9xOoTlQ")
          .execute();
        expect(res.code()).toEqual(200);
        expect(res.body()).toEqual("[{\"id\":1},{\"id\":2},{\"id\":3}]");
      });
    });
  }
}
