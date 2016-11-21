create schema api

go

create table api.items (
  id int IDENTITY(1,1) not null,
  constraint item_pk primary key (id)
)

go

insert into api.items default values
insert into api.items default values
insert into api.items default values

go

create table api.projects (
  id int not null,
  name varchar(50) not null,
  constraint project_pk primary key (id)
)

go

insert into api.projects values(1, 'project 1')
insert into api.projects values(2, 'project 2')
insert into api.projects values(3, 'project 3')

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
