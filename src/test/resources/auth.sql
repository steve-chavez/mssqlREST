
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
