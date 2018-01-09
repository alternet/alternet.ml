List=(
    tools-generator
    tools
    prop-bind-maven-plugin
    scanner
    parsing
    security
    security-auth
    security-jetty-9.1
    security-tomcat-8.0
)

echo "====== Cleaning alternet-libs ======"
cd alternet-libs
mvn clean

for Proj in "${List[@]}"
do
    echo "====== Building $Proj ======"
    cd ../$Proj
    mvn -Dmaven.test.failure.ignore=true clean install site
done

echo "====== Building alternet-libs ======"
cd ../alternet-libs
mvn -Dmaven.test.failure.ignore=true install site
