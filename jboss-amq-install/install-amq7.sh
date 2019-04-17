
#
# Bash script to install amq7, assuming it is downloaded to /tmp directory, currently expecting: /tmp/amq-broker-7.2.4-bin.zip
#

subscription-manager repos --enable rhel-7-server-rpms && subscription-manager repos --enable rhel-7-server-optional-rpms
yum update -y
yum install -y java-1.8.0-openjdk-devel

#create amq user
groupadd amq
#create amq group
useradd amq -g amq

DOWNLOADED_AMQ7_BROKER=/tmp/amq-broker-7.2.4-bin.zip
AMQ_BASE=/opt/jboss/amq/
mkdir -p $AMQ_BASE
unzip $DOWNLOADED_AMQ7_BROKER -d $AMQ_BASE
AMQ_HOME=/opt/jboss/amq/amq-broker-7.2.4
cd $AMQ_HOME
./bin/artemis create mybroker --user admin --password admin --queues jms.queue.hello --require-login ./instances/mybroker
chown amq:amq -R /opt/jboss/amq/

#configure access
sed -i 's/localhost/0.0.0.0/g' /opt/jboss/amq/amq-broker-7.2.4/instances/mybroker/etc/bootstrap.xml
sed -i 's/localhost/*/g' /opt/jboss/amq/amq-broker-7.2.4/instances/mybroker/etc/jolokia-access.xml

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
ExecStart = /opt/jboss/amq/amq-broker-7.2.4/instances/mybroker/bin/artemis-service start
ExecStop = /opt/jboss/amq/amq-broker-7.2.4/instances/mybroker/bin/artemis-service stop
Type=forking
User = amq
Group = amq

[Install]
WantedBy = multi-user.target" > /usr/lib/systemd/system/jboss-amq.service

systemctl enable jboss-amq
systemctl start jboss-amq