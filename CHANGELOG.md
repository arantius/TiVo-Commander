v14 Apr 13, 2012:

* Update target SDK for Holo theme, where available.
* Fix start time parsing crash for "HD Recordings" group which is missing it.
* Add action bar buttons.
* Help item in main menu.

v13 Nov 16, 2011:

* Fix the crash introduced in v12.
* Fix for "Disk Used" meter with 14.9 TiVo software.

v12 Nov 12, 2011:

* Display "New" and "HD" badges, channel, and recording length in Explore.
* Address some crash cases.

v11 Nov 1, 2011:

* Optionally vibrate on button presses in Remote.
* Make the MAK entry dialog use "phone" keyboard layout, since it's numbers.
* Show recordings' channel in My Shows list.
* Try to reduce some crashes.

v10 Oct 22, 2011:

* Fix rare network transmission issue.  This should address primarily when
  the "My Shows" section doesn't load, or only partially loads.
* Remove "Problem Report".  There don't seem to be many real problems left.
* Upgrade the JmDNS library in use.  This should fix some crashes when
  discovering TiVo devices.
* Fix keyboard input in the Remote section.  This fixes sending clear when
  rotating the phone, and a second when clear is really pressed.
* Fix display of details and episode selection in the "Upcoming" screen.

v9 Sep 10, 2011:

* Address a handful of crashes.
* Reminder about "Watch Now" feature behavior.

v8 Aug 31, 2011:

* Revamp device discovery and matching help.  Hopefully much more reliable.
  * This includes fixing a situation that could cause infinite loops.
* More help text.
* Fix a crash when deleting shows.
* Disk usage meter.
* Sort shows by date or title.

v7 Aug 28, 2011:

Enhancements:

* Load the "My Shows" list incrementally, so even long lists of shows appear
  quickly.  More are loaded only when they scroll into view.
* Show recorded date in My Shows.
* Discover, save, and re-use the "bodyId" value that will soon be required.
* Stop a recording in progress.

Bug fixes:

* Do not try to schedule a Season Pass with no channel.
* Correctly hide Record button when it has no choices.
* Fix Collection view to show description and credits.
* Fix rare crash ("Exceeded maximum number of wifi locks") in device discovery.

v6 Aug 23, 2011:

* Fix a stupid mistake I made in v5.
* Update the JmDNS library to a snapshot of 3.4.1.
* A bit more logging.

v5 Aug 22, 2011:

* Fixes for all the crash bugs I know about.
* Improvements for settings help text.

v4 Aug 20, 2011:

* Bug fixes, including a couple crash bugs.
* Crash reporting and problem reporting feature.
* TiVo discovery overhaul, including help text.
* Small visual enhancement; icons on the Explore tabs.

v3 Aug 19, 2011:

* Fix Person view.
* Reconnect when changing settings.  (This will make controlling more than one TiVo a little bit better.)
* Handle connection timeouts.
* Make season passes work when picking "all shows" recording limit.

v2 Aug 15, 2011:

* Remove maxSdkVersion.

v1 Aug 15, 2011:

* Initial release.
