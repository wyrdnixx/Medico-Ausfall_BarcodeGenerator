# Medico-Ausfall_BarcodeGenerator

#
# copy java runtime files in local folder
# ./jre for runtime
# ./jdk for compiling
#
# set jva runtime path in start.bat (Windwos) or start.sh (Linux)
# JAVA_EXE="./jdk-25.0.2+10/bin/java"
#
#################


# Compile
./jdk-25.0.2+10/bin/javac -encoding UTF-8 -cp "lib/*" -d . Medico_Ausfall_BarcodeGenerator.java

# Run (optional: pass config path as first argument)
./jdk-25.0.2+10/bin/java -cp ".:lib/*" Medico_Ausfall_BarcodeGenerator