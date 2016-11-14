create table items (
  id int IDENTITY(1,1) not null,
  constraint item_pk primary key (id)
)

go

insert into items default values
insert into items default values
insert into items default values

go

create table projects (
  id int not null,
  name varchar(50) not null,
  constraint project_pk primary key (id)
)

go

insert into projects values(1, 'project 1')
insert into projects values(2, 'project 2')
insert into projects values(3, 'project 3')

go
