//TODO: GET XLSX and POST XLSX are working but not they're tested - maybe test with xcellite
package mssqlrest;

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
      describe("JSON", () -> {
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

        it("should fail for invalid json", () -> {
          HttpResp res =  HTTP.post("http://localhost:9090/projects")
            .body("'id'".getBytes())
            .execute();
          expect(res.body()).toEqual("{\"message\":\"Could not parse JSON object\"}");
          expect(res.code()).toEqual(400);
        });
      });

      // Curl should be tested with: `-d $''` or --data-binary. Otherwise it strips the newlines
      // curl -H "Content-Type: text/csv" -d $'id,name\n11,project 11\n' l:9090/projects
      describe("CSV", () -> {
        it("should succeed for json payload", () -> {
          HttpResp res =  HTTP.post("http://localhost:9090/entities")
            .header("Content-Type", "text/csv")
            .body("id,name\n1,entity 1\n2,entity 2\n3, entity 3".getBytes())
            .execute();
          expect(res.code()).toEqual(200);
        });

        it("should fail for invalid csv", () -> {
          HttpResp res =  HTTP.post("http://localhost:9090/entities")
            .header("Content-Type", "text/csv")
            .body("'id'".getBytes())
            .execute();
          expect(res.body()).toEqual("{\"message\":\"Could not parse CSV payload\"}");
          expect(res.code()).toEqual(400);
        });
      });
    });

    describe("GET resource", () -> {
      describe("With no filtering", () -> {
        it("should respond with a json array", () -> {
          HttpResp res =  HTTP.get("http://localhost:9090/items").execute();
          expect(res.body()).toEqual("[{\"id\":1},{\"id\":2},{\"id\":3},{\"id\":4},{\"id\":5}]");
          expect(res.code()).toEqual(200);
        });

        it("should ?select columns", () -> {
          HttpResp res =  HTTP.get("http://localhost:9090/projects?select=id").execute();
          expect(res.body()).toEqual("[{\"id\":1},{\"id\":2},{\"id\":3},{\"id\":4}]");
          expect(res.code()).toEqual(200);
        });

        it("should ?order columns", () -> {
          HttpResp res =  HTTP.get("http://localhost:9090/projects?select=id&order=id.desc").execute();
          expect(res.body()).toEqual("[{\"id\":4},{\"id\":3},{\"id\":2},{\"id\":1}]");
          expect(res.code()).toEqual(200);
        });

        it("should respond with a CSV", () -> {
          HttpResp res =  HTTP.get("http://localhost:9090/items")
            .header("Accept", "text/csv")
            .execute();
          expect(res.body()).toEqual("id\n1\n2\n3\n4\n5\n");
          expect(res.code()).toEqual(200);
        });
      });

      describe("With filtering", () -> {
        it("should filter with eq", () -> {
          HttpResp res =  HTTP.get("http://localhost:9090/items?id=eq.1").execute();
          expect(res.body()).toEqual("[{\"id\":1}]");
          expect(res.code()).toEqual(200);
        });

        it("should filter with neq", () -> {
          HttpResp res =  HTTP.get("http://localhost:9090/items?id=neq.1").execute();
          expect(res.body()).toEqual("[{\"id\":2},{\"id\":3},{\"id\":4},{\"id\":5}]");
          expect(res.code()).toEqual(200);
        });

        it("should filter with gt", () -> {
          HttpResp res =  HTTP.get("http://localhost:9090/items?id=gt.2").execute();
          expect(res.body()).toEqual("[{\"id\":3},{\"id\":4},{\"id\":5}]");
          expect(res.code()).toEqual(200);
        });

        it("should filter with gte", () -> {
          HttpResp res =  HTTP.get("http://localhost:9090/items?id=gte.2").execute();
          expect(res.body()).toEqual("[{\"id\":2},{\"id\":3},{\"id\":4},{\"id\":5}]");
          expect(res.code()).toEqual(200);
        });

        it("should filter with lt", () -> {
          HttpResp res =  HTTP.get("http://localhost:9090/items?id=lt.3").execute();
          expect(res.body()).toEqual("[{\"id\":1},{\"id\":2}]");
          expect(res.code()).toEqual(200);
        });

        it("should filter with lte", () -> {
          HttpResp res =  HTTP.get("http://localhost:9090/items?id=lte.3").execute();
          expect(res.body()).toEqual("[{\"id\":1},{\"id\":2},{\"id\":3}]");
          expect(res.code()).toEqual(200);
        });

        // Must use % urlencoded as %25
        it("should filter with like", () -> {
          HttpResp res =  HTTP.get("http://localhost:9090/projects?select=name&name=like.%25project%25").execute();
          expect(res.body()).toEqual("[{\"name\":\"project 1\"},{\"name\":\"project 2\"},{\"name\":\"project 3\"},{\"name\":\"project 4\"}]");
          expect(res.code()).toEqual(200);
        });
      });

      it("should get a single json object for Prefer: plurality=singular", () -> {
        HttpResp res =  HTTP.get("http://localhost:9090/items?id=eq.1")
          .header("Prefer", "plurality=singular")
          .execute();
        expect(res.body()).toEqual("{\"id\":1}");
        expect(res.code()).toEqual(200);
      });

      it("should work for a VIEW", () -> {
        HttpResp res =  HTTP.get("http://localhost:9090/projects_view?select=name&id=lt.3").execute();
        expect(res.body()).toEqual("[{\"name\":\"project 1\"},{\"name\":\"project 2\"}]");
        expect(res.code()).toEqual(200);
      });
    });

    describe("PATCH", () -> {
      it("should succeed when patching with json object", () -> {
        HttpResp res =  HTTP.patch("http://localhost:9090/projects?id=eq.1")
          .body("{'name': 'updated project 1'}".getBytes())
          .execute();
        expect(res.code()).toEqual(200);
      });
    });

    describe("DELETE", () -> {
      it("should succeed deleting", () -> {
        HttpResp res =  HTTP.delete("http://localhost:9090/projects?id=eq.4")
          .execute();
        expect(res.code()).toEqual(200);
      });
    });

    describe("CALL rpc", () -> {
      describe("with a FUNCTION", () -> {
        it("should respond with a json array on a FUNCTION that returns TABLE", () -> {
          HttpResp res =  HTTP.post("http://localhost:9090/rpc/get_projects_lt")
            .body("{\"id\": 5}".getBytes())
            .execute();
          expect(res.body()).toEqual("[{\"name\":\"updated project 1\",\"id\":1},{\"name\":\"project 2\",\"id\":2},{\"name\":\"project 3\",\"id\":3}]");
          expect(res.code()).toEqual(200);
        });

        it("should respond with a json array on a FUNCTION that returns a dynamic TABLE", () -> {
          HttpResp res =  HTTP.post("http://localhost:9090/rpc/get_names")
            .body("".getBytes())
            .execute();
          expect(res.body()).toEqual("[{\"last_name\":\"Doe\",\"first_name\":\"John\"}]");
          expect(res.code()).toEqual(200);
        });

        it("should respond with a scalar on a FUNCTION that returns a scalar", () -> {
          HttpResp res =  HTTP.post("http://localhost:9090/rpc/plus")
            .body("{\"a\": 3, \"b\": 4}".getBytes())
            .execute();
          expect(res.body()).toEqual("7");
          expect(res.code()).toEqual(200);
        });

        it("should respond with a CSV on a FUNCTION that returns TABLE", () -> {
          HttpResp res =  HTTP.post("http://localhost:9090/rpc/get_projects_lt")
            .header("Accept", "text/csv")
            .body("{\"id\": 3}".getBytes())
            .execute();
          expect(res.body()).toEqual("id,name\n1,updated project 1\n2,project 2\n");
          expect(res.code()).toEqual(200);
        });
      });

      describe("with a PROCEDURE", () -> {
        it("should respond with a JSON object of OUT parameters", () -> {
          HttpResp res =  HTTP.post("http://localhost:9090/rpc/mult_xyz_by")
            .body("{\"x\": 1, \"y\": 2, \"z\": 3, \"factor\": 4}".getBytes())
            .execute();
          expect(res.body()).toEqual("{\"x\":4,\"y\":8,\"z\":12}");
          expect(res.code()).toEqual(200);
        });
      });
    });
  }
}
