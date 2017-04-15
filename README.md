# Build Player for StarCraft 2

This app turns your Android phone into your own personal StarCraft II coach, giving you visual and audio reminders for buildings, units and upgrades as you play, for your chosen race and strategy.

**Features**

* Play like a pro by receiving reminders of complex build orders as you play
* 50 build orders included in version 2.6.5, including 4 builds for the new Legacy of the Void expansion
* All build orders include detailed explanation and references - learn the "why" as well as the "what" of effective strategies
* Includes strategies for every possible match-up of Terran, Zerg and Protoss
* Uses Text-To-Speech to give you in-game alerts for your build order - no need to look away from your monitor while playing
* Build order timings are based on either best possible times or times achieved by professional StarCraft 2 players
* Player features: Play/Pause/Stop/Seek, all SC2 Game Speeds
* Built-in editor so you can create your own build orders with timings, or customize the standard ones
* Supports arbitrary reminders - e.g. when to scout, if you should put only 2 workers on gas, etc.
* Configurable early warning time for build alerts
* Supports Wings of Liberty, Heart of the Swarm and Legacy of the Void expansions
* Simple Material Design interface
 
Screenshots
-----------

![App screenshot](https://lh3.googleusercontent.com/wH0L1n872xuXFvBJ5HC3vq5rXq8gbpqv562KknTP-fsVdsuIeOTbLZXgjbQ1k3zqrh9g=h900-rw?raw=true)

Warning to Developers
---------------------

This was originally released back in 2012 and is the first non-trivial Android app I wrote.

It was developed using mostly a trial-and-error process and Google's developer guide and sample apps
as a reference. While I would recommend this as a good way to grasp the fundamentals of Android,
it's not the best formula for building a robust, testable app that can be easily extended with new features over time.

This project has no real architecture to speak of and its implementation is tightly coupled to the Android framework.
As such is mostly untestable and untested.

What I'm trying to say is if you're looking for the source code of a real-world app to use as a reference for building your own
app, you've come to the wrong place.

Instead I'd recommend reading up on the MVP pattern and Clean Architecture for Android. Here's a classic article on the topic,
including sample code:

https://fernandocejas.com/2014/09/03/architecting-android-the-clean-way/