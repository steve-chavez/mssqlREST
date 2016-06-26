
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
 - [x] Support for GET with XLS response.
 - [x] Support for GET with ORDER BY.
 - [x] Support for GET with SELECT projection.
 - [x] Support for POST with CSV Content-Type.
 - [x] Support for explicit nulls in request body. 
 - [x] Support for JWT encoding and decoding. 
 - [x] Support for Header authorization. 
 - [x] Support for Cookie authorization. 

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

## Sql Server Error 15517

- http://www.sqlservercentral.com/blogs/brian_kelley/2013/04/22/troubleshooting-sql-server-error-15517/
- https://danieladeniji.wordpress.com/2009/10/09/ms-sql-server-service-broker-errors-15517-33009/
- http://www.biztalkgurus.com/biztalk_server/biztalk_2009/f/32/p/13271/25836.aspx
- https://social.msdn.microsoft.com/Forums/sqlserver/en-US/0dca9324-e274-4e7a-bb6b-e8709fea809c/error-15517?forum=sqlservicebroker

