
# This program is free software. It comes without any warranty, to
# the extent permitted by applicable law. You can redistribute it
# and/or modify it under the terms of the Do What The Fuck You Want
# To Public License, Version 2, as published by Sam Hocevar. See
# http://sam.zoy.org/wtfpl/COPYING for more details.


    BEGIN { 
        RS="\0"; 
        ORS="\0"
        subscribe_str = ("SUBSCRIBE\ndestination:" queue "\nactivemq.prefetchSize:1\nack:client\n\n")

        #CONNECT and SUBSCRIBE to "queue"
        print "CONNECT\nlogin:"mylogin"\npasscode:"mypass"\n\n"
        print subscribe_str
        fflush()
    }

    { 
        # ActiveMQ appear to use nul/lf as separator, so strip newline
        sub("^\n", "") 
        id = extract_msgid($0) 
        body = extract_body($0)

        if (id != "") {
            cmd = ( usercmd " "usercmdoption " >&2")
            print body | cmd;
            r = close(cmd);

            if (r == 0) {
                print "ACK\n" id "\n"
            } else {
                print ("UNSUBSCRIBE\ndestination:" queue "\n\n")
                system("sleep 1")
                print subscribe_str       
            }
            fflush()
        }
    }

    function extract_msgid(msg) {
        match(msg, "message-id:[[:graph:]]*\n", a)
        return a[0]
        #if(match(msg,"message-id:[[:graph:]]*\n")) {
        #    return substr(msg,RSTART,RLENGTH)
        #}
    }

    function extract_body(msg) {
        return substr( msg, match($0, "\n\n") + 2 )
    } 

