
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
