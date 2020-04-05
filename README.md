[PostgREST](http://postgrest.org/en/v6.0/api.html)-like API for a SQL Server database.

## Quickstart

Run SQL Server in Docker:

```bash
docker run --name mssqlrest -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=my$ecretPassword' -p 1433:1433 -d mcr.microsoft.com/mssql/server:2017-latest
```

Make a quick test to make sure the instance is started:

```bash
docker exec -it mssqlrest /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P 'my$ecretPassword'

1> select 1
2> go

-----------
          1

(1 rows affected)

## Retry after a few seconds if you get the following error:
## Sqlcmd: Error: Microsoft ODBC Driver 17 for SQL Server : Login timeout expired.
## Sqlcmd: Error: Microsoft ODBC Driver 17 for SQL Server : TCP Provider: Error code 0x2749.
## Sqlcmd: Error: Microsoft ODBC Driver 17 for SQL Server : A network-related or instance-specific error has occurred while establishing a connection to SQL Server. Server is not found or not accessible. Check if instance name is correct and if SQL Server is configured to allow remote connections. For more information see SQL Server Books Online..
```

Run the [sample database](src/test/resources/all.sql):

```bash
docker cp src/test/resources/all.sql mssqlrest:/ && \
docker exec -it mssqlrest /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P 'my$ecretPassword' -i all.sql
```

Check the sample database:

```bash
docker exec -it mssqlrest /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P 'my$ecretPassword'
1> use mssqlrest_test
2> go
Changed database context to 'mssqlrest_test'.
1> select * from api.projects
2> go
id          name
----------- --------------------------------------------------
          1 updated project 1
          2 project 2
          3 project 3

(3 rows affected)
```

Download the [release](https://github.com/steve-chavez/mssqlrest/releases) and execute it using the sample config:

```bash
java -jar mssqlREST.jar src/test/resources/config.yml

[Thread-0] INFO org.eclipse.jetty.util.log - Logging initialized @709ms
[Thread-0] INFO spark.webserver.JettySparkServer - == Spark has ignited ...
[Thread-0] INFO spark.webserver.JettySparkServer - >> Listening on 0.0.0.0:9090
[Thread-0] INFO org.eclipse.jetty.server.Server - jetty-9.3.z-SNAPSHOT
[Thread-0] INFO org.eclipse.jetty.server.ServerConnector - Started ServerConnector@6f8cb5b1{HTTP/1.1,[http/1.1]}{0.0.0.0:9090}
[Thread-0] INFO org.eclipse.jetty.server.Server - Started @906ms
```

Do a request:

```bash
curl localhost:9090/projects

[{"name":"updated project 1","id":1},{"name":"project 2","id":2},{"name":"project 3","id":3}]
```

## Config file

```yaml
## The url of the database
url: jdbc:sqlserver://localhost:1433;database=mssqlrest_test;

## The connection user and password
user: authenticator
password: 2wsx!QAZ

## The port of the webserver
port: 9090

## The schema to be exposed
schema: api

## The default role used for every request
defaultRole: anonymous

## the jwt secret
secret: mysecretpass

## the routines(procedure or function) whose return value will be signed by the secret
jwtRoutines :
  - login
```

## Authentication

mssqlREST follows the [PostgREST auth guidelines](http://postgrest.org/en/v6.0/auth.html). We have the same tree roles:

```sql

CREATE LOGIN authenticator WITH PASSWORD = '2wsx!QAZ'
CREATE USER authenticator FOR LOGIN authenticator

CREATE USER anonymous WITHOUT LOGIN
CREATE USER webuser WITHOUT LOGIN

GRANT IMPERSONATE ON USER::anonymous TO authenticator
GRANT IMPERSONATE ON USER::webuser   TO authenticator
```

For impersonating another role, it uses [EXECUTE AS](https://docs.microsoft.com/en-us/sql/t-sql/statements/execute-as-transact-sql?redirectedfrom=MSDN&view=sql-server-ver15).
This is done in each transaction and at the end [REVERT](https://docs.microsoft.com/en-us/sql/t-sql/statements/revert-transact-sql?view=sql-server-ver15) is used.
This is similar to doing a `SET ROLE` and `DISCARD` in PostgreSQL. There's no equivalent of a `SET LOCAL ROLE` in SQL Server.

## JWT

The routines in the `jwtRoutines` config get their result signed(HS256) with the `secret` config. We'll use the `login` procedure
here as an example:

```bash
curl -H "Content-Type: application/json" -d '{"email": "johndoe@company.com", "password": "johnpass"}' l:9090/rpc/login

eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoid2VidXNlciIsImVtYWlsIjoiam9obmRvZUBjb21wYW55LmNvbSJ9.tJf3EOV_bdLgvKBo-NuYbR_JSUPEerrU6bJh9xOoTlQ
```

This token can be passed in the `Authorization` header. mssqlREST obtains the `role` claim from the JWT and then it will switch
the role accordingly.

```bash
export JWT="eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoid2VidXNlciIsImVtYWlsIjoiam9obmRvZUBjb21wYW55LmNvbSJ9.tJf3EOV_bdLgvKBo-NuYbR_JSUPEerrU6bJh9xOoTlQ"

curl -H "Authorization: Bearer $JWT" l:9090/privileged_projects

[{"name":"priv-project 1","id":1},{"name":"priv-project 2","id":2},{"name":"priv-project 3","id":3}]
```

Without the header this resource cannot be accessed.

```bash
curl l:9090/privileged_projects

{"message":"The resource doesn't exist or permission was denied"}
```

## Response formats

These formats are available:

- text/csv

```bash
curl -H "Accept: text/csv" localhost:9090/projects

id,name
1,updated project 1
2,project 2
3,project 3
```

- application/vnd.openxmlformats-officedocument.spreadsheetml.sheet (excel XLSX format)

```bash
curl -H "Accept: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" \
  localhost:9090/projects > projects.xlsx
```

- application/json(default, assumed for `*/*`)

## Filters

The `eq`, `neq`, `gt`, `gte`, `lt`, `lte` and `like` operators are supported:

```bash
curl localhost:9090/projects?id=eq.1

[{"name":"updated project 1","id":1}]

curl localhost:9090/projects?id=neq.1

[{"name":"project 2","id":2},{"name":"project 3","id":3}]

## the wildcard `%` must be used urlencoded as %25
curl localhost:9090/projects?name=like.%25project%25

[{"name":"updated project 1","id":1},{"name":"project 2","id":2},{"name":"project 3","id":3}]
```

The `select` and `order` query params are also supported:

```bash
curl "localhost:9090/projects?select=id&order=id.desc"

[{"id":3},{"id":2},{"id":1}]
```

## Insertions

You can POST with a JSON object to insert a single row.

```bash
curl -H "Content-Type: application/json" -d '{"id": "9", "name": "project 9"}' localhost:9090/projects
```

POSTing with JSON arrays is not supported.

Batch inserts with the CSV and XLSX formats are possible:

```bash
curl -H "Content-Type: text/csv" -d $'id,name\n10,project 10\n' localhost:9090/projects

## projects.xlsx with "10, project 10" content
curl -H "Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" \
  --data-binary @projects.xlsx  localhost:9090/projects
```

Check the insertions:

```bash
curl localhost:9090/projects?id=gte.9

[{"name":"project 9","id":9},{"name":"project 10","id":10}]
```

## Updates/Deletes

PATCH and DELETE can be used as usual:

```bash
curl -v -X PATCH -H "Content-Type: application/json" -d '{"name": "recently updated project 1"}' localhost:9090/projects?id=eq.1

curl localhost:9090/projects?id=eq.1

[{"name":"recently updated project 1xxx","id":1}]
```

```bash
curl -X DELETE localhost:9090/projects?id=eq.1

curl localhost:9090/projects?id=eq.1

[]
```

## Stored Procedures/Functions

You can call functions and procedures on the `/rpc/<routine>` endpoint with POST. The function parameters need to be a json object.
GET is not supported.

Calling a function that returns a table type:

```bash
curl -H "Content-Type: application/json" -d '{"id": 4}' localhost:9090/rpc/get_projects_lt

[{"name":"project 2","id":2},{"name":"project 3","id":3}]
```

The result of this kind of functions can be in any of the supported formats.

Calling a function that returns a scalar type:

```bash
curl -H "Content-Type: application/json" -d '{"a": 1, "b": 2}' localhost:9090/rpc/plus

3
```

Calling a procedure that returns values in its OUT parameters:

```bash
curl -H "Content-Type: application/json" -d '{"x": 2, "y": 4, "z": 6, "factor": 2}' localhost:9090/rpc/mult_xyz_by

{"x":4,"y":8,"z":12}
```

Procedures that use RETURN to return a table type are not supported. There isn't metadata that allows to infer the RETURN type.
Even SQL server `sp_help` doesn't know the return type.

## Metadata

You can get a JSON array of all the available relations at the root.

```bash
curl localhost:9090

[
  {
    "updateable": true,
    "selectable": true,
    "name": "entities",
    "deletable": true,
    "insertable": true
  },
  {
    "updateable": true,
    "selectable": true,
    "name": "items",
    "deletable": true,
    "insertable": true
  },
  {
    "updateable": true,
    "selectable": true,
    "name": "projects",
    "deletable": true,
    "insertable": true
  },
  {
    "updateable": false,
    "selectable": true,
    "name": "projects_view",
    "deletable": false,
    "insertable": false
  }
]
```

Also, to get the columns information for an invidual relation you can use `/?table=<tablename>`:

```bash
curl localhost:9090/?table=projects

[
  {
    "default": null,
    "nullable": false,
    "precision": 10,
    "name": "id",
    "scale": 0,
    "type": "int",
    "max_length": null
  },
  {
    "default": null,
    "nullable": false,
    "precision": null,
    "name": "name",
    "scale": null,
    "type": "varchar",
    "max_length": 50
  }
]
```

## Limitations

This project had to support an outdated SQL Server 2008 R2. That version didn't have native JSON support, so
the JSON response is built in java. SQL Server 2016 and newer versions have [JSON AUTO](https://docs.microsoft.com/en-us/sql/relational-databases/json/format-json-output-automatically-with-auto-mode-sql-server?view=sql-server-ver15),
this could be used for better performance.

There's no schema cache, this is wasteful since for every relation request the `information_schema.columns` VIEW is queried. However,
this has the advantage of not needing a schema reload. Adding new relations and routines should work normally with the started server.

For the above reasons, this project is not suitable for a public API. mssqlREST was used for developing an intranet application.

No support for `Prefer: return=representation`. Newer SQL Server versions have support for the [OUTPUT clause](https://docs.microsoft.com/en-us/sql/t-sql/queries/output-clause-transact-sql?view=sql-server-ver15)
which could be used to support this.

No support for resource embedding. JSON AUTO could be used for this since it works for JOINs as well: `select * from api.projects cross join api.items for json auto`.

## Development

For building and running the server:

```bash
gradle run
```

For running the tests:

```bash
bash src/test/resources/all.sh && \
docker cp src/test/resources/all.sql mssqlrest:/ && \
docker exec -it mssqlrest /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P 'my$ecretPassword' -i all.sql &&
gradle test
```

To generate a jar:

```
gradle jar
```

The generated jar will be at `./build/libs/mssqlREST.jar`.
