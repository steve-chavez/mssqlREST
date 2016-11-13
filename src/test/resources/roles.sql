if suser_id('authenticator') is not null
  drop login authenticator;

CREATE LOGIN authenticator WITH PASSWORD = '2wsx!QAZ'
CREATE USER authenticator FOR LOGIN authenticator

CREATE USER anonymous WITHOUT LOGIN
CREATE USER webuser WITHOUT LOGIN

GRANT IMPERSONATE ON USER::anonymous TO authenticator
GRANT IMPERSONATE ON USER::webuser   TO authenticator

GO

