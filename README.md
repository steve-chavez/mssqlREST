
# Introduction

This is a proof of concept for doing a http interface for any RDBMS(currently SQLServer) in a similar fashion to
[postgrest](https://github.com/begriffs/postgrest).

## Implemented

 - [x] Support for GET with json response.
 - [x] Support for POST with json response.
 - [x] Support for UPDATE with no response.
 - [x] Support for DELETE with no response.
 - [x] Support for FUNCTION(Table valued, scalar valued)
 - [ ] Support for PROCEDURE

## Problems to solve

 - [x] Find a way to convert a ResultSet to JSON without a one-to-one mapping of table to object.
 - [x] Find a safe way to construct queries invulnerable to sql injections(Maybe SQLBuilder or Squiggle).
 - [ ] Integrate library for connection pooling.
 - [ ] Use transactions.
 - [ ] Develop postgrest compliant http methods and querystrings(Which library?).
 - [ ] Consistent error messages for formating exceptions(Check what postgrest does for this).

## Notes

- [StringJoiner](http://stackoverflow.com/a/22577565)
