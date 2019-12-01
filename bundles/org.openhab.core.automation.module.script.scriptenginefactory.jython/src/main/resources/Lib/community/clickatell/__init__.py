# -*- coding: utf-8 -*-
"""
This module can be used to send SMS messages via the Clickatell HTTP/S API at https://api.clickatell.com/.

This file was originally published at https://github.com/jacques/pyclickatell.

2018-07-07: B. Synnerlig added smsEncode() function

License
-------

Copyright (c) 2006-2012 Jacques Marneweck. All rights reserved.
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
"""

import urllib, urllib2

try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO

__author__      = "Jacques Marneweck <jacques@php.net>, Arne Brodowski <mail@arnebrodowski.de>"
__version__     = "0.1.1-alpha"
__copyright__   = "Copyright (c) 2006 Jacques Marneweck, 2008 Arne Brodowski. All rights reserved."
__license__     = "The MIT License"

def smsEncode(my_str):
    # Convert to GSM 03.38 character set and URL-encode
    utf8Chars=['%','\n',' ','"','&',',','.',u'/',':',';','<','=','>','?',u'¡',u'£','#',u'¥',u'§',u'Ä',u'Å',u'à',u'ä',u'å',u'Æ',u'Ç',u'É',u'è',u'é',u'ì',u'Ñ',u'ñ',u'ò',u'ö',u'Ø',u'Ö',u'Ü',u'ù',u'ü',u'ß',u'\\',u'*',u'\'','(',u')',u'@',u'+',u'$',u'[',u']',u'^',u'{',u'|',u'}',u'~']
    gsmChars=['%25','%0D','%20','%22','%26','%2C','%2E','%2F','%3A','%3B','%3C','%3D','%3E','%3F','%A1','%A3','%A4','%A5','%A7','%C4','%C5','%E0','%E4','%E5','%C6','%C7','%C9','%E8','%E9','%EC','%D1','%F1','%F2','%F6','%D8','%D6','%DC','%F9','%FC','%DF','%5C','%2A','%27','%28','%29','%40','%2B','%24','%5B','%5D','%5E','%7B','%7C','%7D','%7E']

    for i in range(0,len(gsmChars)):
        my_str = my_str.replace(utf8Chars[i],gsmChars[i])
    return my_str

def require_auth(func):
    """
    decorator to ensure that the Clickatell object is authed before proceeding
    """
    def inner(self, *args, **kwargs):
        if not self.has_authed:
            self.auth()
        return func(self, *args, **kwargs)
    return inner

class ClickatellError(Exception):
    """
    Base class for Clickatell errors
    """

class ClickatellAuthenticationError(ClickatellError):
    pass

class Clickatell(object):
    """
    Provides a wrapper around the Clickatell HTTP/S API interface
    """

    def __init__ (self, username, password, api_id, sender):
        """
        Initialise the Clickatell class
        Expects:
         - username - your Clickatell Central username
         - password - your Clickatell Central password
         - api_id - your Clickatell Central HTTP API identifier
        """
        self.has_authed = False

        self.username = username
        self.password = password
        self.api_id = api_id
        self.sender = sender

        self.session_id = None


    def auth(self, url='https://api.clickatell.com/http/auth'):
        """
        Authenticate against the Clickatell API server
        """
        post = [
            ('user', self.username),
            ('password', self.password),
            ('api_id', self.api_id),
        ]

        result = self.curl(url, post)

        if result[0] == 'OK':
            assert (32 == len(result[1]))
            self.session_id = result[1]
            self.has_authed = True
            return True
        else:
            raise ClickatellAuthenticationError(': '.join(result))

    @require_auth
    def getbalance(self, url='https://api.clickatell.com/http/getbalance'):
        """
        Get the number of credits remaining at Clickatell
        """
        post = [
            ('session_id', self.session_id),
        ]

        result = self.curl(url, post)
        if result[0] == 'Credit':
            assert (0 <= result[1])
            return result[1]
        else:
            return False

    @require_auth
    def getmsgcharge(self, apimsgid, url='https://api.clickatell.com/http/getmsgcharge'):
        """
        Get the message charge for a previous sent message
        """
        assert (32 == len(apimsgid))
        post = [
            ('session_id', self.session_id),
            ('apimsgid', apimsgid),
        ]

        result = self.curl(url, post)
        result = ' '.join(result).split(' ')

        if result[0] == 'apiMsgId':
            assert (apimsgid == result[1])
            assert (0 <= result[3])
            return result[3]
        else:
            return False

    @require_auth
    def ping(self, url='https://api.clickatell.com/http/ping'):
        """
        Ping the Clickatell API interface to keep the session open
        """
        post = [
            ('session_id', self.session_id),
        ]

        result = self.curl(url, post)

        if result[0] == 'OK':
            return True
        else:
            self.has_authed = False
            return False

    @require_auth
    def sendmsg(self, message, url = 'https://api.clickatell.com/http/sendmsg'):
        """
        Send a mesage via the Clickatell API server
        Takes a message in the following format:

        message = {
            'to': 'to_msisdn',
            'text': 'This is a test message',
        }
        Return a tuple. The first entry is a boolean indicating if the message
        was send successfully, the second entry is an optional message-id.
        Example usage::
            result, uid = clickatell.sendmsg(message)
            if result == True:
                print "Message was sent successfully"
                print "Clickatell returned %s" % uid
            else:
                print "Message was not sent"
        """
        if not (message.has_key('to') or message.has_key('text')):
            raise ClickatellError("A message must have a 'to' and a 'text' value")

        message['text'] = smsEncode(message['text'])

        post = [
            ('session_id', self.session_id),
            ('from', self.sender),
            ('to', message['to']),
        ]
        postStr = urllib.urlencode(post)
        postStr += '&text='+ message['text']

        result = self.curl(url, postStr, False)

        if result[0] == 'ID':
            assert (result[1])
            return (True, result[1])
        else:
            return (False, None)

    @require_auth
    def tokenpay(self, voucher, url='https://api.clickatell.com/http/token_pay'):
        """
        Redeem a voucher via the Clickatell API interface
        """
        assert (16 == len(voucher))
        post = [
            ('session_id', self.session_id),
            ('token', voucher),
        ]

        result = self.curl(url, post)

        if result[0] == 'OK':
            return True
        else:
            return False

    def curl(self, url, post, urlEncode=True):
        """
        Inteface for sending web requests to the Clickatell API Server
        """
        try:
            if urlEncode:
                data = urllib2.urlopen(url, urllib.urlencode(post))
            else:
                data = urllib2.urlopen(url, post)
        except urllib2.URLError(v):
            raise ClickatellError(v)

        return data.read().split(": ")
