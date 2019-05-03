#!/bin/sh
#
# Bash script to configure the jmx exporter for Prometheus
#
AMQ_USER=admin
AMQ_PASSWORD=admin
AMQ_BASE=/opt/jboss/amq/
AMQ_HOME=/opt/jboss/amq/amq-broker-7.2.4
BROKER_NAME=mybroker
BROKER_PATH=$AMQ_HOME/instances/$BROKER_NAME

# Copy the jmx exporter jar
cd ../../prometheus
cp jmx_prometheus* $BROKER_PATH/bin
cp artemis.yml $BROKER_PATH/bin

# Add jar as JVM parameter
sed "s|JAVA_ARGS|JAVA_OPTS|" $BROKER_PATH/etc/artemis.profile  > $BROKER_PATH/etc/artemis_sed.profile && \
mv $BROKER_PATH/etc/artemis_sed.profile $BROKER_PATH/etc/artemis.profile

sed 's|# Java Opts|# Java Opts \
#Variable containing arguments for JMX_EXPORTER and JAVA_OPTS \
JAVA_ARGS="$JAVA_OPTS -javaagent:'${BROKER_PATH}'/bin/jmx_prometheus_javaagent-0.11.0.jar=8080:'${BROKER_PATH}'/bin/artemis.yml" \
\
|' $BROKER_PATH/etc/artemis.profile  > $BROKER_PATH/etc/artemis_sed.profile && \
mv $BROKER_PATH/etc/artemis_sed.profile $BROKER_PATH/etc/artemis.profile

# Check if JMX remote is enabled

if grep -q "com.sun.management.jmxremote" $BROKER_PATH/bin/artemis; then
    if grep  -q "com.sun.management.jmxremote=false" $BROKER_PATH/bin/artemis; then
        sed "s|-Dcom.sun.management.jmxremote=false|-Dcom.sun.management.jmxremote=true|" $BROKER_PATH/bin/artemis  > $BROKER_PATH/bin/artemis_sed && \
        mv $BROKER_PATH/bin/artemis_sed $BROKER_PATH/bin/artemis
    else
       exit 0
    fi
else
    sed '/-classpath/a\ 
    \    -Dcom.sun.management.jmxremote=true \ \\\
    ' $BROKER_PATH/bin/artemis  > $BROKER_PATH/bin/artemis_sed && \
    mv $BROKER_PATH/bin/artemis_sed $BROKER_PATH/bin/artemis
fi