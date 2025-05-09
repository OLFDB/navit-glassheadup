==================
Navit Glass Headup
==================
Overview
########
This is a headup plugin for `Navit <https://navit.readthedocs.io/en/latest/>`_.
It shows turn by turn instructions and important navigation data on a Google Glass 1.

.. image:: https://private-user-images.githubusercontent.com/5459286/441401151-08a23e47-c11a-4521-9736-843d3cbeca6a.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3NDY4MDU5MTAsIm5iZiI6MTc0NjgwNTYxMCwicGF0aCI6Ii81NDU5Mjg2LzQ0MTQwMTE1MS0wOGEyM2U0Ny1jMTFhLTQ1MjEtOTczNi04NDNkM2NiZWNhNmEucG5nP1gtQW16LUFsZ29yaXRobT1BV1M0LUhNQUMtU0hBMjU2JlgtQW16LUNyZWRlbnRpYWw9QUtJQVZDT0RZTFNBNTNQUUs0WkElMkYyMDI1MDUwOSUyRnVzLWVhc3QtMSUyRnMzJTJGYXdzNF9yZXF1ZXN0JlgtQW16LURhdGU9MjAyNTA1MDlUMTU0NjUwWiZYLUFtei1FeHBpcmVzPTMwMCZYLUFtei1TaWduYXR1cmU9NGFmNGNkYTU1ODQzZWQwYzUyY2E3OThlZThlNTVmMDNjMTdkMjQ0ZTg5NTNkYjFkNWU5ODNiOTNmOGIxZWEzNiZYLUFtei1TaWduZWRIZWFkZXJzPWhvc3QifQ.7x8VPdvz2aG7SotkJCZebidneDYIzB4wDtryzByEVSk

Top left icon shows the navigation state. Next to it is the route length, the estimated time of arrival and the current time at the current position in local timezone. The signal strength icon shows the GNSS signal quality. 
In the second line you find the name of the street after the next turn. The turn instruction icon is in line three.Next to it the current GNSS speed is shown. Next to it the compass showing current magnetic heading and the direction to the destination as a green arrow.
Line four display the distance to the next turn, a warning icon for speedcams, or a TPMS warning if the TPMS plugin is installed as well, the WGS84 coordinate of the current position, the speed limit of the road, the direct distance to the destination and the current GNSS altitude. 
Line five shows the current streets name and in line six you'll find some OBDII data, if the OBD2 plugin is installed as well. OBD speed, coolant temperature and generator voltage.

You can build the source using Android Studio 4.2.2. In SDK Manager under Anroid 4.4 install the Glass Development Kit Preview, ASndrpod SDK platform 19 and Sources for Android SDK 19. 

You need to disable 'Hide obsolete packages' and enable 'Show package details' to be able to select these items under SDK Manager -> Android SDK.

Now you can build the APK and run it so it gets installed on the GG1 device.
