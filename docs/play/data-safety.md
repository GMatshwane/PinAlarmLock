# Data safety form answers

Play Console → App content → Data safety.

Use these answers. They match the shipped app: no `INTERNET` permission, on-device DataStore only.

## Overview

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | Not asked when collection is No |
| Do you provide a way for users to request that their data is deleted? | Not asked when collection is No |

Google’s Data safety definition of “collected” is data that leaves the device. PIN salt/hash and enrolled package names stay in on-device preferences and are never transmitted by this app.

## Independent check

If Play later treats on-device PIN hash or enrolled package names as collected “App activity” / “Other in-app messages”:

- Collected: yes, stored on device
- Shared: no
- Processed ephemerally: no
- Required / app functionality
- Encrypted in transit: not applicable (never leaves the device)
- Users can delete: yes — clear app storage / uninstall

Do not declare advertising, analytics, or account data. The app has none.
