This is a working bundle for demonstrating/ testing the OAuth2 client.
Passwords, secrets, etc have to be configured through config admin in order for it to work

Simply deploy it to the runtime; then openhab:oauth commands will be registered and ready to test.


# Example 1: (Using authorization code)

## Try these on the OSGI console:

```
openhab:oauth Code cleanupEverything
openhab:oauth Code create
openhab:oauth Code getClient <fill in handle from create step>
openhab:oauth Code getAuthorizationUrl
```

```
now open browser with the URL from above step, authenticate yourself
to a real oauth provider
if everything works properly, it should redirect you to your redirectURL
Read the code http parameter from the redirectURL
```

```
openhab:oauth Code getAccessTokenByCode <code from redirectURL parameter>
openhab:oauth Code getCachedAccessToken
openhab:oauth Code refresh
openhab:oauth Code close
```

# Example 2: (Using ResourceOwner credentials i.e. you have the user's username and password directly)

## Try these on the OSGI console:

```
openhab:oauth ResourceOwner create
openhab:oauth ResourceOwner getClient <fill in handle from create step>
openhab:oauth ResourceOwner getAccessTokenByResourceOwnerPassword
openhab:oauth ResourceOwner getCachedAccessToken
openhab:oauth ResourceOwner refresh
openhab:oauth ResourceOwner close
```

### load again, similar to reboot/restart

```
openhab:oauth ResourceOwner getClient <fill in handle from create step>
openhab:oauth ResourceOwner getCachedAccessToken
openhab:oauth ResourceOwner refresh
```

### Done playing, delete this service permanently

```
openhab:oauth ResourceOwner delete <fill in handle from create step>
```

### Verify this is deleted (will throw exception)

```
openhab:oauth ResourceOwner getCachedAccessToken 
```

### Cannot get the client after delete

```
openhab:oauth ResourceOwner getClient <fill in handle from create step>
```
