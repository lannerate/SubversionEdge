rem %~dp0 is location of current script under NT
set _REALPATH=%~dp0
set PATH=C:\pkg-tools\bin;C:\Program Files\GnuWin32\bin;C:\Program Files\Caphyon\Advanced Installer 7.7\bin\x86;C:\Program Files\CollabNet\CUBiT API Client;%PATH%
set PKG=cmd /c pkg
set VERSION=4.0.0
set CSVN=C:\collabnet-csvn

if %1.==stage. goto stage
if %1.==release. goto release

:dev
set REPOS=http://pkg.collab.net/dev/windows64/
set UPDATES=%REPOS%
set LOC=/Installers/windows/dev-builds
goto start

:stage
set REPOS=http://pkg.collab.net/qa/windows64/
set UPDATES=%REPOS%
set LOC=/Installers/windows/release-candidates
goto start

:release
set REPOS=http://pkg.collab.net/qa/windows64/
set UPDATES=http://pkg.collab.net/release/windows64/
set LOC=/Installers/windows/releases
goto start


:start
set CACHE=%CSVN%\.org.opensolaris,pkg\cfg_cache

rmdir %CSVN% /Q/S
mkdir %CSVN%

REM Initialize the local image
%PKG% image-create -U -a collab.net=%REPOS% %CSVN%
%PKG% -R %CSVN% set-property title "CollabNet Subversion Edge"
%PKG% -R %CSVN% set-property description "Package repository for CollabNet Subversion Edge."
%PKG% -R %CSVN% set-property send-uuid True
%PKG% -R %CSVN% set-authority -O %REPOS% collab.net

REM Install our application and required packages
%PKG% -R %CSVN% refresh
%PKG% -R %CSVN% install pkg
%PKG% -R %CSVN% install csvn
%PKG% -R %CSVN% image-update

REM Now prepare image for distribution
%PKG% -R %CSVN% set-authority -O %UPDATES% collab.net
%PKG% -R %CSVN% rebuild-index
%PKG% -R %CSVN% purge-history


REM Remove the UUID
xcopy /Y %CACHE% %TEMP%
grep -v "^uuid =" %TEMP%\cfg_cache > "%CACHE%"

REM Remove the variant ARCH
xcopy /Y %CACHE% %TEMP%
grep -v "variant.arch" %TEMP%\cfg_cache > "%CACHE%"

REM Cleanup content within the image
cd %CSVN%
rename temp-data data
xcopy /S/E/R/Y/C updates .
rmdir updates /Q/S
cd ".org.opensolaris,pkg"
rmdir download /Q/S

AdvancedInstaller.com /edit "%_REALPATH%\setup64.aip" /ResetSync APPDIR -clearcontent
AdvancedInstaller.com /edit "%_REALPATH%\setup64.aip" /NewSync APPDIR %CSVN%
AdvancedInstaller.com /edit "%_REALPATH%\setup64.aip" /SetVersion %VERSION%
AdvancedInstaller.com /rebuild "%_REALPATH%\setup64.aip"
cd "%_REALPATH%\Setup Files"
pbl upload -u markphip -k f04b2c60-2650-1378-80a8-4350af7da540 -l https://mgr.cloud.sp.collab.net/cubit_api/1 -p svnedge -t pub -r %LOC% --force CollabNetSubversionEdge-%VERSION%_setup-x86_64.exe