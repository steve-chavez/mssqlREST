
# Introduction

This is a proof of concept for doing a http interface for any RDBMS(currently SQLServer) in a similar fashion to
[postgrest](https://github.com/begriffs/postgrest).

## Problems to solve

 - [x] Find a way to convert a ResultSet to JSON without a one-to-one mapping of table to object.
 - [ ] Find a safe way to construct queries invulnerable to sql injections(Maybe SQLBuilder or Squiggle).
 - [ ] Integrate library for connection pooling.
 - [ ] Use transactions.
 - [ ] Develop postgrest compliant http methods and querystrings(Which library?).

## Flow

### GET : 
 
    Table -> Obtain metadata from DB(datatypes) with Table -> Map ResultSet to Java Datatypes 
    -> Convert to JSON -> Send JSON through HTTP Response 

### POST :

    Table and posted data in JSON -> Obtain metadata from DB(datatypes) with Table 
    -> Map JSON posted data to Java datatypes matching DB datatypes 
    -> Insert to DB -> Send result through HTTP Response

