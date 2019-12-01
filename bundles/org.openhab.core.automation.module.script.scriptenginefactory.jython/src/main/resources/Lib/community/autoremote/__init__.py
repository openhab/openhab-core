"""
Purpose
=======

With AutoRemote you have full control of your phone, from wherever you are by sending push notifications to your phone and reacting to them in Tasker or AutoRemote standalone apps.
This helper library aims to facilitate sending such AutoRemote notifications from custom openHAB scripts.


Requires
========

* - Setup an AutoRemote profile in Tasker to react to the message

Further reading is available at the author's web site (https://joaoapps.com/autoremote/).


Known Issues
============

None


Change Log
==========

* 09/19/19: Added this description.
"""
import os
from configuration import autoremote_configuration

def sendMessage(message, ttl=300, sender='openHAB'):
    '''
    Sends an autoremote message
    '''

    # Use GCM Server for delivery
    cmd = 'curl -s -G "https://autoremotejoaomgcd.appspot.com/sendmessage" ' \
        + '--data-urlencode "key='+autoremote_configuration['key']+'" ' \
        + '--data-urlencode "password='+autoremote_configuration['password']+'" ' \
        + '--data-urlencode "message='+message+'" ' \
        + '--data-urlencode "sender='+sender+'" ' \
        + '--data-urlencode "ttl='+str(ttl)+'" ' \
        + ' 1>/dev/null 2>&1 &'

    os.system(cmd)
