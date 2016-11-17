grant select, insert, update, delete on api.items to anonymous
grant select, insert, update, delete on api.projects to anonymous

-- grant select on functions that return tables
grant select on api.get_projects_lt to anonymous
grant select on api.get_names to anonymous

-- grant execute functions that return scalars(weird, I know)
grant execute on api.plus to anonymous

-- grant execute on procedures
grant execute on api.mult_xyz_by to anonymous

go
