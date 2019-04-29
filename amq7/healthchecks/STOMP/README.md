# bash-mq #

a bash/awk producer/consumer for activemq or any STOMP compatible MQ

Inspired from squawk ( http://www.nobugs.org/blog/archives/2008/05/11/squawk-simple-queues-using-awk/ )

**producer usage :**

*echo "my message" | ./produce -h 127.0.0.1 -p 61613 -q fooqueue*


**consumer usage :**

*./consume -h 127.0.0.1 -p 61613 -q fooqueue -c ./handler*

**NOTE**

*it only works with GNU awk because other awk version can't handle \0 as Record Separator :(
