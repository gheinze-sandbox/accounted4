REM In order to avoid password prompting create the file:
REM
REM     C:\Users\glenn\AppData\Roaming\postgresql
REM
REM with contents:
REM
REM     hostname:port:database:username:password
REM
REM See Chapter 30.14 "The Password File" in the postgres docs
REM
REM Since there is not a way to set the search path when using a jdbc
REM datasource, you can change the default search_path for the user:
REM
REM     ALTER USER postgres SET search_path TO tia,public;



set output=create_schema_objects.sql

echo -- Dynamic script generation start > %output%
echo select now();                     >> %output%

REM Generate a script to create schema objects

for /F "eol=;tokens=1" %%i in (create_list.txt) do (
    echo
    echo --------------------------- >> %output%
    echo -- Processing File: %%i     >> %output%
    echo --------------------------- >> %output%
    type %%i >> %output%
)



REM =============================================
REM == Run generated script
REM =============================================
psql -v ON_ERROR_STOP=1 -p 5432 -e postgres postgres < %output%

REM pause