# Cessna 140 Flight Test — Android

An offline Android flight-test aid for measuring the actual climb performance of a Cessna 140 without relying on the airplane's VSI.

## Version 1.1 features

- Guides timed climb runs at user-selected indicated airspeeds.
- Creates reciprocal A/B heading pairs to reduce wind bias.
- Calculates climb rate from elapsed time and start/end altimeter readings.
- Identifies the best tested Vy from the peak reciprocal-pair climb rate.
- Identifies a Vx candidate with the FAA tangent-from-origin proxy: average climb rate divided by IAS.
- Records native Android GPS distance, groundspeed, heading, and accuracy for separate ground-gradient documentation.
- Lets the pilot reject runs affected by turbulence, lift/sink, IAS error, or power anomalies.
- Suggests a second, finer test pass around the preliminary Vx candidate.
- Stores all test data locally and exports CSV or JSON backups.
- Works offline and keeps the phone screen awake during a run.

## Build and download the APK

Every push to `main` runs the included GitHub Actions workflow. Open **Actions**, select the successful **Build Android APK** run, then download the `Cessna140-Flight-Test-APK` artifact. The artifact contains `Cessna140-Flight-Test.apk`.

## Safety

This is an experimental flight-test aid, not approved aircraft performance data. Vx and Vy vary with weight, configuration, density altitude, and aircraft condition. Use a safety pilot or passenger to operate the phone, test only at a safe altitude in smooth air, remain clear of traffic, and follow the aircraft's approved limitations.
