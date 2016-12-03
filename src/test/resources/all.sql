
if db_id('mssqlrest_test') is not null
  drop database mssqlrest_test

go

create database mssqlrest_test

go

use mssqlrest_test

go


if suser_id('authenticator') is not null
  drop login authenticator;

CREATE LOGIN authenticator WITH PASSWORD = '2wsx!QAZ'
CREATE USER authenticator FOR LOGIN authenticator

CREATE USER anonymous WITHOUT LOGIN
CREATE USER webuser WITHOUT LOGIN

GRANT IMPERSONATE ON USER::anonymous TO authenticator
GRANT IMPERSONATE ON USER::webuser   TO authenticator

GO


create schema auth
go

create function auth.role_exists(@role varchar(256))
returns bit as
begin
    declare @exists bit = 0;
    set @exists = coalesce(
            (select 1 from sys.database_principals where name = @role), 0);
    return @exists;
end;
go

create table auth.users (
  email varchar(254) not null,
  password char(40) not null,
  role varchar(256) not null,
  constraint users_pk primary key (email),
  constraint users_email_valid_ck check (email like '%_@__%.__%'),
  constraint users_role_exists_ck check (auth.role_exists(role)=1)
);
go

create function auth.encrypt(@password varchar(40))
returns char(40) as
begin;
    return convert(char(40), hashbytes('sha1', @password), 2);
end;
go

create trigger users_instead_of_insert on auth.users
instead of insert as
begin
    insert into auth.users(email, password, role)
    select email, auth.encrypt(password), role
    from inserted;
end;
go

create function auth.valid_credentials(@email varchar(254), @password char(40))
returns bit as
begin
    declare @valid bit = 0;
    set @valid = coalesce(
      (select 1 from auth.users
          where email = @email and
          password = auth.encrypt(@password)), 0);
    return @valid;
end;
go

create schema api
go

create table api.items (
  id int IDENTITY(1,1) not null,
  constraint item_pk primary key (id)
)
go

create table api.projects (
  id int not null,
  name varchar(50) not null,
  constraint project_pk primary key (id)
)
go

create view api.projects_view as
select id, name from api.projects
go

create function api.get_projects_lt(@id int)
returns table
as
return(
    select *
    from api.projects
    where id < @id
)
go

create function api.get_names()
returns @result table (
    first_name varchar(256), last_name varchar(256)
) as
begin
    insert into @result select 'John', 'Doe';
    return;
end;
go

create function api.plus(@a int, @b int)
returns int
as
begin
    declare @result int;
    set @result = @a + @b;
    return @result;
end;
go

create procedure api.mult_xyz_by(
  @x int out,
  @y int out,
  @z int out,
  @factor int)
as
begin
  set @x = @x * @factor;
  set @y = @y * @factor;
  set @z = @z * @factor;
end;
go

create table api.entities (
  id int not null,
  name varchar(50) not null,
  constraint entity_pk primary key (id)
)
go

create procedure api.login(
    @email varchar(254) out, @password char(40),
    @role varchar(256) = null out) as
begin
    declare @valid bit = 0;

    set @valid = (select auth.valid_credentials(@email, @password));
    if @valid = 0 begin
        raiserror('invalid user or password', 16, 1); return;
    end

    select @role = role, @email = email
    from auth.users where email = @email
end;
go

create table api.privileged_projects (
  id int not null,
  name varchar(50) not null,
  constraint privileged_projects_pk primary key (id)
)
go

insert into api.items default values
insert into api.items default values
insert into api.items default values
go

insert into api.projects values(1, 'project 1')
insert into api.projects values(2, 'project 2')
insert into api.projects values(3, 'project 3')
go

insert into auth.users values('johndoe@company.com', 'johnpass', 'webuser')
insert into auth.users values('janedoe@company.com', 'janepass', 'webuser')
go

insert into api.privileged_projects values(1, 'priv-project 1')
insert into api.privileged_projects values(2, 'priv-project 2')
insert into api.privileged_projects values(3, 'priv-project 3')
go

-- grant all doesn't work on sql server
grant select, insert, update, delete on api.items to anonymous
grant select, insert, update, delete on api.projects to anonymous
grant select                         on api.projects_view to anonymous
grant select, insert, update, delete on api.entities to anonymous

-- grant select on functions that return tables
grant select on api.get_projects_lt to anonymous
grant select on api.get_names to anonymous

-- grant execute functions that return scalars(weird, I know)
grant execute on api.plus to anonymous

-- grant execute on procedures
grant execute on api.mult_xyz_by to anonymous
-- note that anonymous doesn't have permission for the auth tables used inside the procedure
grant execute on api.login to anonymous

grant select, insert, update, delete on api.privileged_projects to webuser

go
