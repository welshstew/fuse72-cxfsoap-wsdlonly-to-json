#
# Bash script to copy broker certificates to amq7 broker home, assuming AMQ version 7.2.4
#

AMQ_USER=admin
AMQ_PASSWORD=admin
AMQ_BASE=/Users/jtavares/amq-broker-7.2.3
AMQ_HOME=/Users/jtavares/amq-broker-7.2.3
BROKER_NAME=mybroker
BROKER_PATH=$AMQ_HOME/instances/$BROKER_NAME
AMQ_ACCEPTOR='<acceptor name="amqp-ssl">tcp://0.0.0.0:5671?tcpSendBufferSize=1048576;tcpReceiveBufferSize=1048576;protocols=AMQP;useEpoll=true;amqpCredits=1000;amqpMinCredits=300;connectionsAllowed=1000;sslEnabled=true;keyStorePath=broker_ks.p12;keyStorePassword=password;trustStorePath=broker_ts.p12;trustStorePassword=password</acceptor> \
</acceptors>'

# Copy the broker certificates
cd ../certificates
cp broker* $BROKER_PATH/etc

# Configure a new SSL acceptor
#CONTENT=$(echo $AMQ_ACCEPTOR | sed 's/\//\\\//g')
sed "s|<\/acceptors>|${AMQ_ACCEPTOR}|" $BROKER_PATH/etc/broker.xml > $BROKER_PATH/etc/broker_sed.xml && \
mv $BROKER_PATH/etc/broker_sed.xml $BROKER_PATH/etc/broker.xml




