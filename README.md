# FullSuiteAstro
An android application developed using Java that utilizes all the various sensors of your Android device to determine its current tilt. Works ~relatively~ well given
the sensitivity of the various magnetometers on an Android device. 

Uses deques and linked lists to crunch a LOT of sensor data to even out the various fluctuations.

Requires location ( to calculate ALT AZ), network ( to update RA DEC of stars), and sensor access (to do the meat and potatoes of it all.) 

Future plans include planetary calculations, a GUI based "guide", better sensor fusion, a filter for the incoming sensor data, and general stablility updates.
