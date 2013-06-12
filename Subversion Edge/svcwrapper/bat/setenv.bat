@echo off
rem quotes are required for correct handling of path with spaces

rem default java home
set wrapper_home=%~dp0/..

rem default java exe for running the wrapper
rem note this is not the java exe for running the application. the exe for running the application is defined in the wrapper configuration file
set java_exe="java"

if not defined JAVA_HOME goto findjava
if not exist "%JAVA_HOME%\bin\java.exe" goto findjava
set java_exe="%JAVA_HOME%\bin\java"
goto out

rem on Win64 this variable is set
:findjava
if not defined ProgramW6432 goto win32
if "%ProgramW6432%" == "%ProgramFiles%" goto is64flag

rem this means we are on Win64 but running 32-bit cmd.exe
if not exist %WINDIR%\sysnative\java.exe goto :is64flag
set java_exe="%WINDIR%\sysnative\java"
goto out

:is64flag
:win32
set java_exe=java

:out
rem location of the wrapper jar file. necessary lib files will be loaded by this jar. they must be at <wrapper_home>/lib/...
set wrapper_jar="%wrapper_home%/wrapper.jar"

rem setting java options for wrapper process. depending on the scripts used, the wrapper may require more memory.
set wrapper_java_options=-Xmx30m

rem wrapper bat file for running the wrapper
set wrapper_bat="%wrapper_home%/bat/wrapper.bat"

rem configuration file used by all bat files
set conf_file="%wrapper_home%/conf/wrapper.conf"

rem default configuration used in genConfig
set conf_default_file="%wrapper_home%\conf\wrapper.conf.default"

rem workaround possible problems on user environment by clearing variables:
set _JAVA_OPTIONS=
