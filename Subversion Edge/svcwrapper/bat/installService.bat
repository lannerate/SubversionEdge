if %1.==. goto skip
set JAVA_HOME=%~1

:skip
call setenv.bat
%wrapper_bat% -i %conf_file%



