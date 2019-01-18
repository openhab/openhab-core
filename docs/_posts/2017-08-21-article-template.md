---
layout: post
title: "Null Annotations"
date:   2017-08-21
image: "2017-08-21-article-template.png"
published: false
---

Everything is better with Null Annotations. Far far away, behind the word mountains, far from the countries Vokalia and Consonantia, there live the blind texts. <!--more--> 

##### The first paragraph
Separated they live in Bookmarksgrove right at the coast of the Semantics, a large language ocean. A small river named Duden flows by their place and supplies it with the necessary regelialia. It is a paradisematic country, in which roasted parts of sentences fly into your mouth. Even the all-powerful Pointing has no control about the blind texts it is an almost unorthographic life One day however a small line of blind text by the name of Lorem Ipsum decided to leave for the far World of Grammar. The Big Oxmox advised her not to do so, because there were thousands of bad Commas, wild Question Marks 

##### Null or not Null is the question
One morning, when Gregor Samsa woke from troubled dreams, he found himself transformed in his bed into a horrible vermin. He lay on his armour-like back, and if he lifted his head a little he could see his brown belly, slightly domed and divided by arches into stiff sections. The bedding was hardly able to cover it and seemed ready to slide off any moment. His many legs, pitifully thin compared with the size of the rest of him, waved about helplessly as he looked. "What's happened to me?" he thought

```
/**
 * Creates a thing based on given thing type uid.
 *
 * @param thingTypeUID
 *            thing type uid (can not be null)
 * @param configuration
 *            (can not be null)
 * @param thingUID
 *            thingUID (can not be null)
 * @return thing (can be null, if thing type is unknown)
 */
protected @Nullable Thing createThing(
        @NonNull ThingTypeUID thingTypeUID, 
        @NonNull Configuration configuration,
        @NonNull ThingUID thingUID) {
    return createThing(thingTypeUID, configuration, thingUID, null);
}
```
It showed a lady fitted out with a fur hat and fur boa who sat upright, raising a heavy fur muff that covered the whole of her lower arm towards the viewer.

##### Nullable is okay here
This is a very important image: 
![openHAB logo](http://3.bp.blogspot.com/-ejvDecjGQiA/UZASTVsv0oI/AAAAAAAAF0k/87RVq4PMAzk/s1600/openhab.jpg)
