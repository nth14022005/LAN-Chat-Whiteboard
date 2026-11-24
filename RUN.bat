@echo off
setlocal
cd /d "%~dp0"
if not exist bin mkdir bin
(for /r src %%f in (*.java) do @echo "%%f") > sources.txt
javac --release 17 -encoding UTF-8 -cp "lib/*" -d bin @sources.txt
if errorlevel 1 goto fail
java -cp "bin;lib/*" com.lanchat.main.MainApp
goto end
:fail
echo Can JDK 17 hoac moi hon. Co the Import va Run trong Eclipse.
:end
if exist sources.txt del sources.txt
pause
