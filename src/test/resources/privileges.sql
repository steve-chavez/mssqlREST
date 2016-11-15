grant select, insert, update, delete on api.items to anonymous
grant select, insert, update, delete on api.projects to anonymous

-- grant select on a function
-- grant execute on a procedure
grant select on api.get_project to anonymous

go
