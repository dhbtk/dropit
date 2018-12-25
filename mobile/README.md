Mobile architecture

SQLite database with:

* `computer` table
  * `id` UUID string
  * `secret`UUID secret
  * `name` name
  * `ip_address` current IP Address
  * `port` port
  * `token` token for this computer
  * `token_status` our token status
  
SharedPrefs with:

* `phoneId` UUID, randomly generated on first app boot
* `phoneName` string, this can probably be obtained somehow?
* `currentComputerId` UUID for the computer we are sharing stuff to

2 flows

# Sharing stuff to known computer

Steps 1-4 happen in the activity, step 5 is in BG service and gets a notification

1. Listen for UDP broadcast until we get one, update IP accordingly
2. Send a `getTokenStatus`, check if secret matches
3. If it does not, unregister computer and abort
4. If it does, read info about file, send `createTransfer`
5. Upload file
6. Success!

# Setting up

Activity reads computers from database, plus an additional status:
* `contacted`: default false

Activity that spawns an `AsyncTask` to start the `DiscoveryClient`, updating IP addresses if needed and setting
`contacted` to true, while notifying the activity in the process

## Clicking a computer

If we don't have a token yet, start a popup `Requesting authorization... Click "Approve" on your computer to finish
pairing.`. Request `getTokenStatus` every 500 ms to know if the user approved. If denied, show another popup telling
the user of that. Otherwise, mark as approved in database, set it as `currentComputerId`.

1. "Requesting pairing..."
2. "Please confirm the pairing on your computer to continue."