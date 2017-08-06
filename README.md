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
 
Download
--------

<p align="center">
<a href='https://play.google.com/store/apps/details?id=com.kiwiandroiddev.sc2buildassistant&utm_source=github&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/badge_new.png'/></a>
</p>
 
Screenshots
-----------

![Builds list screenshot](https://lh3.googleusercontent.com/wH0L1n872xuXFvBJ5HC3vq5rXq8gbpqv562KknTP-fsVdsuIeOTbLZXgjbQ1k3zqrh9g=h900-rw?raw=true)
![Player screenshot](https://lh3.googleusercontent.com/UD1cyyNC2Az1Rc6Ya7sABwGX2n1X_FjECzy468sF4JbIybz8HXYTmInvhSc4QP9d9ME=h900-rw?raw=true)
![Build brief screenshot](https://lh3.googleusercontent.com/akqksJigH1KkoeZvY125wM7iRrSThYEnQcg7jI9I5dDJcL-tDBnb_jSkzp4flYOfdg=h900-rw?raw=true)

Libraries Used
--------------

* [Appcompat, Support Library (Design, v4, RecyclerView)](https://developer.android.com/topic/libraries/support-library/features.html)
* [Butterknife](https://github.com/JakeWharton/butterknife)
* [Timber](https://github.com/JakeWharton/timber)
* [Material Dialogs](https://github.com/afollestad/material-dialogs)
* [Dexter](https://github.com/Karumi/Dexter)
* [RxJava](https://github.com/ReactiveX/RxJava), [RxAndroid](https://github.com/ReactiveX/RxAndroid)
* [Mockito](https://github.com/mockito/mockito)
* [AssertJ](https://github.com/joel-costigliola/assertj-core)
* [Espresso](https://google.github.io/android-testing-support-library/docs/espresso/)
* [Fastlane Screengrab](https://github.com/fastlane/fastlane/tree/master/screengrab)

Warning to Developers
---------------------

This was originally released back in 2012 and is the first non-trivial Android app I wrote.

It was developed using mostly a trial-and-error process and Google's developer guide and sample apps
as a reference. While I would recommend this as a good way to grasp the fundamentals of Android,
it's not the best formula for building a robust, testable or extensible app :)

TLDR: if you're looking for the source code of a real-world app to use as a reference for building your own
app, you've come to the wrong place.

Instead, I'd recommend doing some reading on the MVP pattern and Clean Architecture for Android apps. Here's a classic article on the topic,
including sample code: https://fernandocejas.com/2014/09/03/architecting-android-the-clean-way/

Google's Android Architecture Blueprints repository could also be worth checking out:
https://github.com/googlesamples/android-architecture

Contributing
------------

### Development

Contributions are more than welcome. If you're a developer looking for ways to contribute, as a first stop you could check out the issues page for open bugs.
 
If you have an idea for a new feature you'd like to work on, please open an issue for it just in case it's something I've already started on.

### Translations

You can contribute translations to the crowd-sourced translations project here: http://www.getlocalization.com/sc2buildplayer/

Input from native speakers of all non-English languages for improving the grammar of spoken alerts (e.g. "Warp in 5 Zealots") is much appreciated.

Consider opening an issue if you have fully translated a language and would like to get it into the next release.

Disclaimer
----------

    StarCraft® II: Wings of Liberty™
    ©2010 Blizzard Entertainment, Inc. All rights reserved. Wings of Liberty is a trademark, and StarCraft and Blizzard Entertainment are trademarks or registered trademarks of Blizzard Entertainment, Inc. in the U.S. and/or other countries.
    
    StarCraft® II: Heart of the Swarm®
    ©2013 Blizzard Entertainment, Inc. All rights reserved. Heart of the Swarm and StarCraft are trademarks or registered trademarks of Blizzard Entertainment, Inc. in the U.S. and/or other countries.
    
    Content from Team Liquid (http://wiki.teamliquid.net/) licensed under CC-BY-SA.
    
    Content from GosuBuilds (http://www.gosubuilds.com/) used with permission.
    
    Content from Zergology (http://zergology.tumblr.com/) used with permission.
    
    Google Play and the Google Play logo are trademarks of Google Inc.

License
-------

    MIT License
    
    Copyright (c) 2017 Matt Clarke
    
    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:
    
    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
