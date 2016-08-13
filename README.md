
# Introduction

This is a proof of concept for doing a http interface for any RDBMS(currently SQLServer) in a similar fashion to
[postgrest](https://github.com/begriffs/postgrest).

## Implemented

 - [x] Support for GET with json response.
 - [x] Support for POST with json response.
 - [x] Support for UPDATE with no response.
 - [x] Support for DELETE with no response.
 - [x] Support for FUNCTION(Table valued, scalar valued)
 - [x] Support for PROCEDURE(Only OUT parameters as result,
       even SQL server sp_help doesn't know return type when using RETURN, also in t-sql only INOUT parameters exist)
 - [x] Support for user impersonation.
 - [x] Support for GET with CSV response.
 - [x] Support for GET with XLSX response.
 - [x] Support for GET with ORDER BY.
 - [x] Support for GET with SELECT projection.
 - [x] Support for POST with CSV Content-Type.
 - [x] Support for POST with XLSX Content-Type.
 - [x] Support for explicit nulls in request body. 
 - [x] Support for JWT encoding and decoding. 
 - [x] Support for Header authorization. 
 - [x] Support for Cookie authorization. 
 - [x] Support for file extension for resource. 

## Improvements

 - [ ] Improve query parameter mapping to table value in
       PATCH and DELETE methods(Don't program by accident).

## Problems to solve

 - [x] Find a way to convert a ResultSet to JSON without a one-to-one mapping of table to object.
 - [x] Find a safe way to construct queries invulnerable to sql injections(Maybe SQLBuilder or Squiggle).
 - [x] Integrate library for connection pooling.
 - [x] Use transactions.
 - [ ] Develop postgrest compliant http methods and querystrings(Which library?).
 - [ ] Consistent error messages for formating exceptions(Check what postgrest does for this).
 - [ ] Consistent http success codes(Check what postgrest does for this).
 - [ ] Consistent http error codes(Check what postgrest does for this).
 - [ ] Consistent errors for not having privilege to access routines(Check what postgrest does for this).

## Notes

- [StringJoiner](http://stackoverflow.com/a/22577565)
- https://github.com/Azure/azure-content/blob/master/articles/sql-database/sql-database-manage-azure-ssms.md
- https://msdn.microsoft.com/en-us/library/ms181362.aspx
- https://msdn.microsoft.com/en-us/library/ms173463.aspx
- [Metadata of return table in stored procedure](http://stackoverflow.com/questions/14574773/retrieve-column-names-and-types-of-a-stored-procedure/14575114#14575114)
- [All Grants](http://stackoverflow.com/questions/497317/how-can-i-view-all-grants-for-an-sql-database)
- [Constraints](http://stackoverflow.com/questions/14229277/sql-server-2008-get-table-constraints)
- [Find references to table](http://stackoverflow.com/questions/17501840/how-can-i-find-out-what-foreign-key-constraint-references-a-table-in-sql-server)
- [Download file in client](http://stackoverflow.com/questions/3665115/create-a-file-in-memory-for-user-to-download-not-through-server/18197341?noredirect=1#answer-3665147)
- [JDBC](http://stackoverflow.com/questions/17657057/workaround-for-null-primitives-in-jdbc-preparedstatement)
- [Function default](http://stackoverflow.com/questions/8358315/t-sql-function-with-default-parameters)
- [Stored procedure default obtain](http://stackoverflow.com/questions/14652361/determine-whether-sp-parameter-has-a-default-value-in-t-sql)
- [Jasper compile once](http://stackoverflow.com/questions/14738332/how-to-compile-jrxml-only-once)
- [CDC](https://www.simple-talk.com/sql/learn-sql-server/introduction-to-change-data-capture-(cdc)-in-sql-server-2008/)
- [UTF-8 Gradle](http://stackoverflow.com/questions/21267234/show-utf-8-text-properly-in-gradle/34717160#34717160)
- [All permissions on tables with sys](https://msdn.microsoft.com/en-us/library/ms188367.aspx)
- [Sql Server Cursor Procedure](http://stackoverflow.com/questions/1045880/using-a-cursor-with-dynamic-sql-in-a-stored-procedure)
- [Get procedure prvilege](http://stackoverflow.com/questions/13152329/finding-stored-procedures-having-execute-permission)
- [dbo vs db_onwer](http://stackoverflow.com/questions/2731787/what-is-the-difference-between-db-owner-and-the-user-that-owns-the-database)

## SQL SERVER tricks

- http://stackoverflow.com/questions/741414/insert-update-trigger-how-to-determine-if-insert-or-update
- http://stackoverflow.com/questions/16758684/combine-inserted-and-deleted-in-a-trigger-without-using-a-join

## Sql Server Error 15517

- http://www.sqlservercentral.com/blogs/brian_kelley/2013/04/22/troubleshooting-sql-server-error-15517/
- https://danieladeniji.wordpress.com/2009/10/09/ms-sql-server-service-broker-errors-15517-33009/
- http://www.biztalkgurus.com/biztalk_server/biztalk_2009/f/32/p/13271/25836.aspx
- https://social.msdn.microsoft.com/Forums/sqlserver/en-US/0dca9324-e274-4e7a-bb6b-e8709fea809c/error-15517?forum=sqlservicebroker

## Magic syntax that dont actually grant sql privileges

GRANT SELECT,INSERT,UPDATE,DELETE ON SCHEMA::dbo TO UserName

exec sp_addrolemember 'db_owner', 'UserName'

## Pending bugs

When Bearer is appended without space to the token.
java.util.NoSuchElementException: No value present
        at java.util.Optional.get(Optional.java:135)
        at ApplicationServer.getRoleFromCookieOrHeader(ApplicationServer.java:128)
        at ApplicationServer.lambda$main$13(ApplicationServer.java:277)
        at spark.RouteImpl$1.handle(RouteImpl.java:58)
        at spark.webserver.MatcherFilter.doFilter(MatcherFilter.java:162)
        at spark.webserver.JettyHandler.doHandle(JettyHandler.java:61)
        at org.eclipse.jetty.server.session.SessionHandler.doScope(SessionHandler.java:189)
        at org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:141)
        at org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:119)
        at org.eclipse.jetty.server.Server.handle(Server.java:517)
        at org.eclipse.jetty.server.HttpChannel.handle(HttpChannel.java:302)
        at org.eclipse.jetty.server.HttpConnection.onFillable(HttpConnection.java:242)
        at org.eclipse.jetty.io.AbstractConnection$ReadCallback.succeeded(AbstractConnection.java:245)
        at org.eclipse.jetty.io.FillInterest.fillable(FillInterest.java:95)
        at org.eclipse.jetty.io.SelectChannelEndPoint$2.run(SelectChannelEndPoint.java:75)
        at org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume.produceAndRun(ExecuteProduceConsume.java:213)
        at org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume.run(ExecuteProduceConsume.java:147)
        at org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:654)
        at org.eclipse.jetty.util.thread.QueuedThreadPool$3.run(QueuedThreadPool.java:572)
        at java.lang.Thread.run(Thread.java:745)


Hikari error, better include the driver name
ds.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
[qtp834300090-19] INFO com.zaxxer.hikari.HikariDataSource - HikariPool-1 - Started.
Failed to get driver instance for jdbcUrl=jdbc:sqlserver://rrhh.database.windows.net;database=RRHH;

