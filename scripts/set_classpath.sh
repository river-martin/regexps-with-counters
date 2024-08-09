# Get the paths to the class files of the project's dependencies.
mvn dependency:build-classpath -Dmdep.outputFile=.dependency-classpath.txt
# Add the paths to the CLASSPATH environment variable.
export CLASSPATH=target/classes/:$(cat .dependency-classpath.txt):${CLASSPATH}
rm -f .dependency-classpath.txt