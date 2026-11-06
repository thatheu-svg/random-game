@echo off
setlocal

set BUILD=C:\temp\bb-build
set DIST=C:\temp\bb-dist
set ZIP=C:\temp\BerryBlast.zip

rem start clean every time
rmdir /s /q "%BUILD%" 2>nul
rmdir /s /q "%DIST%" 2>nul
del "%ZIP%" 2>nul
mkdir "%BUILD%\classes"
mkdir "%BUILD%\jarinput"

rem compile for Java 17 and copy the four images next to the classes
javac --release 17 -d "%BUILD%\classes" Player.java Enemy.java FastEnemy.java TankEnemy.java Upgrade.java || goto :fail
copy player.png "%BUILD%\classes" >nul || goto :fail
copy galaxy_puddle.png "%BUILD%\classes" >nul || goto :fail
copy fastenemy.png "%BUILD%\classes" >nul || goto :fail
copy tankenemy.png "%BUILD%\classes" >nul || goto :fail

rem pack everything into one jar, then wrap it with a bundled Java runtime
jar --create --file "%BUILD%\jarinput\BerryBlast.jar" --main-class Player -C "%BUILD%\classes" . || goto :fail
jpackage --type app-image --name BerryBlast --input "%BUILD%\jarinput" --main-jar BerryBlast.jar --main-class Player --dest "%DIST%" || goto :fail

rem zip the whole BerryBlast folder
powershell -NoProfile -Command "Compress-Archive -Path '%DIST%\BerryBlast' -DestinationPath '%ZIP%'" || goto :fail

echo.
echo Done: %ZIP%
goto :end

:fail
echo.
echo BUILD FAILED. Read the message above.
exit /b 1

:end