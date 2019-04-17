rm -rf /opt/jboss/amq/
rm /usr/lib/systemd/system/jboss-amq.service

firewall-cmd --zone=public --permanent --remove-port=61616/tcp
firewall-cmd --zone=public --permanent --remove-port=5672/tcp
firewall-cmd --zone=public --permanent --remove-port=5671/tcp
firewall-cmd --zone=public --permanent --remove-port=61613/tcp
firewall-cmd --zone=public --permanent --remove-port=5445/tcp
firewall-cmd --zone=public --permanent --remove-port=1883/tcp
firewall-cmd --zone=public --permanent --remove-port=8161/tcp

userdel amq
groupdel amq