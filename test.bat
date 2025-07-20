call mvn clean install -T 18 -DskipTests -DskipChecks
echo on
call mvn clean install -pl :org.openhab.core.model.item.tests
echo on
call mvn clean install -pl :org.openhab.core.model.thing.tests
