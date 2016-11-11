
if db_id('xrest_test') is not null
  drop database xrest_test; 
  
create database xrest_test; 

use xrest_test;

create table items ( 
  id int not null identity, 
  constraint item_pk primary key (id)
) 
go

insert into dbo.items default values;
insert into dbo.items default values;
insert into dbo.items default values;

create user anonymous without login;

grant select on items to anonymous;
