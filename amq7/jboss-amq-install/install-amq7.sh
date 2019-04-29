
#
# Bash script to install amq7, assuming it is downloaded to /tmp directory, currently expecting: /tmp/amq-broker-7.2.4-bin.zip
#

AMQ_USER=admin
AMQ_PASSWORD=admin
QUEUES="hello,hello1,hello2,hello3"
DOWNLOADED_AMQ7_BROKER=/tmp/amq-broker-7.2.4-bin.zip
AMQ_BASE=/opt/jboss/amq/
AMQ_HOME=/opt/jboss/amq/amq-broker-7.2.4
BROKER_NAME=mybroker
BROKER_PATH=$AMQ_HOME/instances/$BROKER_NAME


subscription-manager repos --enable rhel-7-server-rpms && subscription-manager repos --enable rhel-7-server-optional-rpms
yum update -y
yum install -y java-1.8.0-openjdk-devel

#create amq user
groupadd amq
#create amq group
useradd amq -g amq

mkdir -p $AMQ_BASE
unzip $DOWNLOADED_AMQ7_BROKER -d $AMQ_BASE

cd $AMQ_HOME
./bin/artemis create $BROKER_NAME --user $AMQ_USER --password $AMQ_PASSWORD --queues $QUEUES --require-login $BROKER_PATH
chown amq:amq -R $AMQ_BASE

#configure access
sed -i 's/localhost/0.0.0.0/g' $BROKER_PATH/etc/bootstrap.xml
sed -i 's/localhost/*/g' $BROKER_PATH/etc/jolokia-access.xml

# configure firewall
# acceptors
firewall-cmd --zone=public --permanent --add-port=61616/tcp
firewall-cmd --zone=public --permanent --add-port=5672/tcp
firewall-cmd --zone=public --permanent --add-port=5671/tcp
firewall-cmd --zone=public --permanent --add-port=61613/tcp
firewall-cmd --zone=public --permanent --add-port=5445/tcp
firewall-cmd --zone=public --permanent --add-port=1883/tcp

# console
firewall-cmd --zone=public --permanent --add-port=8161/tcp

firewall-cmd --reload

# configure service
echo "[Unit]
Description = JBoss Active MQ (AMQ)
After = network.target

[Service]
ExecStart = $BROKER_PATH/bin/artemis-service start
ExecStop = $BROKER_PATH/bin/artemis-service stop
Type=forking
User = amq
Group = amq

[Install]
WantedBy = multi-user.target" > /usr/lib/systemd/system/jboss-amq.service

systemctl enable jboss-amq
systemctl start jboss-amq