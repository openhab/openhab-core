This is a working bundle for demonstrating/ testing the Oauth2 client.
Passwords, secrets, etc have to be configured through config admin in order for it to work

Simply deploy it to the runtime; then smarthome oauth commands will be registered and ready to test.


# Example 1: (Using authorization code)

## Try these on the OSGI console:

```
smarthome oauth Code cleanupEverything
smarthome oauth Code create
smarthome oauth Code getClient <fill in handle from create step>
smarthome oauth Code getAuthorizationUrl
```

```
now open browser with the URL from above step, authenticate yourself
to a real oauth provider
if everything works properly, it should redirect you to your redirectURL
Read the code http parameter from the redirectURL
```

```
smarthome oauth Code getAccessTokenByCode <code from redirectURL parameter>
smarthome oauth Code getCachedAccessToken
smarthome oauth Code refresh
smarthome oauth Code close
```

# Example 2: (Using ResourceOwner credentials i.e. you have the user's username and password directly)

## Try these on the OSGI console:

```
smarthome oauth ResourceOwner create
smarthome oauth ResourceOwner getClient <fill in handle from create step>
smarthome oauth ResourceOwner getAccessTokenByResourceOwnerPassword
smarthome oauth ResourceOwner getCachedAccessToken
smarthome oauth ResourceOwner refresh
smarthome oauth ResourceOwner close
```

### load again, similar to reboot/restart

```
smarthome oauth ResourceOwner getClient <fill in handle from create step>
smarthome oauth ResourceOwner getCachedAccessToken
smarthome oauth ResourceOwner refresh
```

### Done playing, delete this service permanently

```
smarthome oauth ResourceOwner delete <fill in handle from create step>
```

### Verify this is deleted (will throw exception)

```
smarthome oauth ResourceOwner getCachedAccessToken 
```

### Cannot get the client after delete

```
smarthome oauth ResourceOwner getClient <fill in handle from create step>
```
