from core.log import logging, LOG_PREFIX
from community.clickatell import Clickatell
from configuration import clickatell_configuration

def sms(message, subscriber='Default'):
    '''
    Sends an SMS message through ClickaTell gateway.
    Example: sms("Hello")
    Example: sms("Hello", 'Amanda')
    @param param1: SMS Text
    @param param2: Subscriber. A numeric phone number or a phonebook name entry (String)
    '''
    log = logging.getLogger(LOG_PREFIX + ".community.clickatell.sendsms")
    phoneNumber = clickatell_configuration['phonebook'].get(subscriber, None)
    if phoneNumber is None:
        if subscriber.isdigit():
            phoneNumber = subscriber
        else:
            log.warn("Subscriber [{}] wasn't found in the phone book".format(subscriber))
            return
    gateway = Clickatell(clickatell_configuration['user'], clickatell_configuration['password'], clickatell_configuration['apiid'], clickatell_configuration['sender'])
    message = {'to': phoneNumber, 'text': message}
    log.info("Sending SMS to: [{}]".format(phoneNumber))
    retval, msg = gateway.sendmsg(message)
    if retval == True:
        log.info("SMS sent: [{}]".format(msg))
    else:
        log.warn("Error while sending SMS: [{}]".format(retval))
    return
