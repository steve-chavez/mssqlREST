
CREATE PROCEDURE dbo.exec_param(@param INT, @res1 INT OUT, @res2 INT OUT) 
AS
BEGIN
    SET NOCOUNT ON;
    SET @res1 = @res1 + @param;
    SET @res2 = @res2 + @param;
END;

