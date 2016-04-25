
http POST localhost:8080/rpc/ultimo_dia_mes_anterior date='2016-01-01'  

http POST localhost:8080/rpc/edad_trabajador trabajador_id=1 fecha_referencia='2016-01-01'  

http POST localhost:8080/rpc/monto_quinta fecha='2016-01-01' trabajador_id=693 

http POST localhost:8080/rpc/anio_mes_iguales fecha1='2016-01-12' fecha2='2016-01-20'  

http POST localhost:8080/rpc/concepto_essalud monto_imponible_onp_essalud=320 aseguradora_salud_id=1 

# Run SqlTests.sql first
http POST localhost:8080/rpc/exec_param param=1 res1=3 res2=4 
