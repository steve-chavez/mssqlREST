
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
 - [x] Support for POST with CSV Content-Type.
 - [x] Support for explicit nulls in request body. 
 - [x] Support for jwt encoding and decoding. 

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
