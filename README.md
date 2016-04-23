
# Introduction

This is a proof of concept for doing a http interface for any RDBMS(currently SQLServer) in a similar fashion to
[postgrest](https://github.com/begriffs/postgrest).

## Implemented

 - [x] Support for GET with json response.
 - [x] Support for POST with json response.
 - [x] Support for UPDATE with no response.
 - [x] Support for DELETE with no response.
 - [x] Support for FUNCTION
 - [ ] Support for PROCEDURE

## Not implemented

 - [ ] Function with TABLE return type not yet supported.

## Problems to solve

 - [x] Find a way to convert a ResultSet to JSON without a one-to-one mapping of table to object.
 - [x] Find a safe way to construct queries invulnerable to sql injections(Maybe SQLBuilder or Squiggle).
 - [ ] Integrate library for connection pooling.
 - [ ] Use transactions.
 - [ ] Develop postgrest compliant http methods and querystrings(Which library?).

## Flow

### GET : 
 
    Table -> Obtain metadata from DB(datatypes) with Table -> Map ResultSet to Java Datatypes 
    -> Convert to JSON -> Send JSON through HTTP Response 

    Table -> Obtain metadata from DB with Table(columns and types) -> Map this to java classes ->
    Build query with Table and java classes -> Prepare statement -> Construct JSON from ResultSet with Java classes -> HTTP Response JSON

### POST :

    Table and posted data in JSON -> Obtain metadata from DB(datatypes) with Table 
    -> Map JSON posted data to Java datatypes matching DB datatypes 
    -> Insert to DB -> Send result through HTTP Response

    Table and posted JSON -> Obtain metadata from DB with Table(columns and types) -> Map this to java classes ->
    Build query with java classes and JSON -> Prepare statement -> HTTP Response Result JSON

## Notes

- [StringJoiner](http://stackoverflow.com/a/22577565)
