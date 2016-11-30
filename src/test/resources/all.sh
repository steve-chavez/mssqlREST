## There's no concept of include or \i(like in postgres psql) in tsql:
## https://stackoverflow.com/questions/12715819/is-there-a-t-sql-equivalent-for-include-and-why

## So we use cat to produce a final sql file:
cat src/test/resources/db.sql src/test/resources/roles.sql \
    src/test/resources/auth.sql src/test/resources/api.sql \
    src/test/resources/data.sql src/test/resources/privileges.sql > src/test/resources/all.sql
