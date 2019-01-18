---
layout: documentation
---

# Profiles

The communication between the framework and the Thing handlers is managed by the "Communication Manager", which in turn uses "Profiles"  to determined what exactly needs to be done. 
This provides some flexibility to influence these communication paths.

By their nature, profiles are correlated to links between Items and Channels (i.e. `ItemChannelLinks`). So if one Channel is linked to several Items it also will have several profile instances, each handling the communication to exactly one of these Items. 
The same applies for the situation where one Item is linked to multiple Channels. 

Profiles are created by ProfileFactories and are retained for the lifetime of their link. 
This means that they are allowed to retain a transient state, like e.g. the timestamp of the the last event or the last state. 
With this, it is possible to take into account the temporal dimension when calculating the appropriate action in any situation.

There exist two different kinds of profiles: state and trigger profiles.

### State Profiles

State profiles are responsible for communication between Items and their corresponding state Channels (`ChannelKind.STATE`). 
Their purpose is to forward state updates and commands to and from the Thing handlers.

### Trigger Profiles

Trigger Channels (`ChannelKind.TRIGGER`) by themselves do not maintain a state (as by their nature they only fire events). 
With the help of trigger profiles they can be linked to Items anyway. 
Hence the main purpose of a trigger profile is to calculate a state based on the fired events. 
This state then is forwarded to the linked Item by sending `ItemStateEvents`. 

Trigger profiles are powerful means to implement some immediate, straight-forward logic without the need to write any rules. 

Apart from that, they do not pass any commands or state updates to and from the Thing handler as by their nature trigger Channels are not capable of handling these.
