FORMAT: 1A

# Eclipse SmartHome UI Logging Bundle

This Bundle provides a resource at the REST API to enable a consumer to fetch the set log levels.
It also receives log messages which will be logged to the server's log.
The last logged messages by this API can be requested to be displayed in UIs.

Meta: This document is in normal markdown format,
but also compatible to [Apiary](apiary.com) for automatic doc generation and API testing.

## Log levels [/rest/log/levels]

### Get enabled log levels [GET]

 This depends on the current log settings at the backend.


+ Response 200 (application/json)

        {
            "warn": true,
            "error": true,
            "debug": true,
            "info": true
        }


## Log [/rest/log]

### Send log message [POST]

+ Request (application/json)

    + Attributes
        + severity (enum[string], required) - The severity of the log message
            + Members
                + `error`
                + `warn`
                + `info`
                + `debug`

        + url (string, optional) - The URL where the log event ocurred.
        + message (string, optional) - The message to log.

    + Body

            {
                "severity" : "error",
                "url" : "http://exmple.org/",
                "message" : "A test message"
            }

+ Response 200

+ Response 403 (application/json)

    + Attributes
        + error (string)
        + severity (string)

    + Body

            {
                 "error": "Your log severity is not supported.",
                 "severity": "info"
            }


## Log [/rest/log/{limit}]    

### Get last log messages [GET]

Return the last log entries received by `/rest/log/`.


+ Parameters
    + limit (number, optional) - Limit the amount of messages.

        On invalid input, limit is set to it's default.

        + Default: 500

+ Response 200 (application/json)

    + Attributes
        + timestamp (number) - UTC milliseconds from the epoch.

            In JavaScript, you can use this value for constructing a `Date`.

        + severity (enum[string])
            + Members
                + `error`
                + `warn`
                + `info`
                + `debug`

        + url (string)
        + message (string)

    + Body

            [
              {
                "timestamp": 1450531459479,
                "severity": "error",
                "url": "http://example.com/page1",
                "message": "test 5"
              },
              {
                "timestamp": 1450531459655,
                "severity": "error",
                "url": "http://example.com/page1",
                "message": "test 6"
              },
              {
                "timestamp": 1450531460038,
                "severity": "error",
                "url": "http://example.com/page2",
                "message": "test 7"
              }
            ]        
