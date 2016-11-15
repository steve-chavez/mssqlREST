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

create function api.get_project(@id int)
returns table
as
return(
    select *
    from api.projects
    where id = @id
)

go
