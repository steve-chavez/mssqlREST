
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
