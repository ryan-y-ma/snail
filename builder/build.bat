@echo off

call config.bat

echo ��ʼ������Ŀ��%project%��

call clean.bat

cd ..\

rem �����Ŀ
echo -----------------------------------------------
echo �����Ŀ
echo -----------------------------------------------
call mvn -q clean package -DskipTests
call xcopy /S /Q .\target\%lib%\* %builder%%lib%\*
call copy .\target\%jar% %builder%
call copy %launcher% %builder%%exe%

rem ����JAVA���л���
rem ��ѯ�������jdeps --list-deps *.jar
echo -----------------------------------------------
echo ����JAVA���л���
echo -----------------------------------------------
call jlink --add-modules %modules% --output %builder%%runtime%

cd %builder%