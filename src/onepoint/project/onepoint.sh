
JAR=opproject-07-be.jar

if [ -e "$JAVA_HOME" ]; then
    $JAVA_HOME/bin/java -jar $JAR
else
    java -jar $JAR
fi