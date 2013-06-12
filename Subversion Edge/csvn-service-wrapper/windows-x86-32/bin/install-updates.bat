@echo off
setlocal

rem
rem Installs updates on Windows.
rem   Checks if service has ended by waiting for PID file
rem   to be gone.  Then copies all files from "updates"
rem   folder.  If no errors, it removes the folder,
rem   otherwise it leaves them.

rem
rem Find the application home.
rem
rem %~dp0 is location of current script under NT
set _REALPATH=%~dp0
set _LOG=%_REALPATH%..\data\logs\updates.log

echo "Beginning install of updates." > "%_LOG%"
echo "AppHome is %_REALPATH%" >> "%_LOG%"

set _CSVN_UPDATE=false
set _APACHE_UPDATE=false

if exist "%_REALPATH%..\data\run\csvn-svn.updated" set _APACHE_UPDATE=true
if exist "%_REALPATH%..\data\run\csvn.updated" set _CSVN_UPDATE=true

REM Console has exited, so we don't need to wait for java pid
REM See if we have to stop/wait for Apache
if %_APACHE_UPDATE%.==false. goto copyfiles
echo "Stopping Apache/SVN service ..." >> "%_LOG%"
net stop "CollabNet Subversion Server" >> "%_LOG%"

:loop2
if not exist "%_REALPATH%..\data\run\httpd.pid" goto copyfiles
call "%_REALPATH%\wait" 5
echo "Waiting for %_REALPATH%..\data\run\httpd.pid" >> "%_LOG%"
goto loop2

:copyfiles
REM exit now if there are no updates
if not exist "%_REALPATH%..\updates" goto noaction
echo "Copying contents of updates folder" >> "%_LOG%"
xcopy /S/E/R/Y "%_REALPATH%..\updates" "%_REALPATH%..\"  >> "%_LOG%" 2>&1
set _XCOPY_ERRORLEVEL=%ERRORLEVEL%
if %_XCOPY_ERRORLEVEL% GEQ 2 goto logerror
if %_XCOPY_ERRORLEVEL% EQU 1 goto noaction
if %_XCOPY_ERRORLEVEL% EQU 0 goto cleanup

:noaction
echo "No updates on file system, exiting without taking any action" >> "%_LOG%"
goto exit

:cleanup
REM Delete the updates folder
echo "No errors.  Deleting updates folder." >> "%_LOG%"
rmdir "%_REALPATH%..\updates" /Q/S >> "%_LOG%"
goto exit

:logerror
echo "There was an XCOPY error applying the updates. Exit code: %_XCOPY_ERRORLEVEL%" >> "%_LOG%"
goto exit

:exit
if %_APACHE_UPDATE%.==true. goto startserver
exit 0

:startserver
echo "Restarting Apache/SVN service ..." >> "%_LOG%"
net start "CollabNet Subversion Server" >> "%_LOG%"
