# Three Node OpenQKDNetwork + CQPToolkit Integration

Firstly, set up qkd-net + cqptoolkit integration with two nodes, and [qkd-net with three nodes](https://github.com/Open-QKD-Network/qkd-net/tree/disable-spring-auth) if you haven't done so already.

## Configuration
In a three node network, we have three machines running one site agent and dummy driver each, and the intermediate machine in the connection running an additional dummy driver. If the site IDs are respectively `A`, `B`, and `C`, then node `B` has one dummy driver generating keys with site `A`'s dummy driver, and one dummy driver generating keys with site `C`'s dummy driver. \
\
Steps to reproduce demo:
- Checkout the `issue-27` branch in qkd-net, cqptoolkit, and `cqptoolkit/src/QKDInterfaces/proto` on all three computers.
- Untar (`tar xvf`) `qkd-jack-ID.tar` where `ID` is the site ID (i.e. `A`, `B`, or `C`). Replace `~/.qkd` with the extracted directory.
- Edit `~/.qkd/qnl/routes.json` as you normally would to set up a three node network (see link above).
- Make sure `~/.qkd/qnl/config.yaml` has the line `qkdLinkConfigLoc: qkdlink.json` on all sites. \
ex.
```json
base: .qkd/qnl
routeConfigLoc: routes.json
qnlSiteKeyLoc: qll/keys
qkdLinkConfigLoc: qkdlink.json
siteId: A
port: 9292
keyBytesSz: 32
keyBlockSz: 4 
qllBlockSz: 4096 
headerSz: 132
kmsIP: localhost
kmsPort: 9393
OTPConfig:
keyBlockSz: 4
keyLoc: otp/keys
```
- Make sure for every connected site with id `ID`, the file `~/.qkd/qnl/qll/keys/ID/qkdlink.json` exists and is formatted correctly. \
	ex. on site A in `~/.qkd/qnl/qll/keys/B/qkdlink.json`
	```json
	{
	    "localQKDDeviceId": "A_B_A",
	    "remoteQKDDeviceId": "A_B_B",
	    "localSiteAgentUrl": "192.168.2.29:8000",
	    "remoteSiteAgentUrl": "192.168.2.22:8000"
	}
	```

	The device IDs should match those defined in `dummy-ID.json` (for site ID `ID`) and follow the naming convention: `smaller_greater_local` where `smaller` is the lexiographically smaller site ID in the connection, `greater` is the lexiographically greater site ID, and `local` is the machine's site ID. For example, node `A` would have the above `qkdlink.json`, while node `B` would have two `qkdlink.json` files: \
	`~/.qkd/qnl/qll/keys/A/qkdlink.json`

	```json
	{
	    "localQKDDeviceId": "A_B_B",
	    "remoteQKDDeviceId": "A_B_A",
	    "localSiteAgentUrl": "192.168.2.22:8000",
	    "remoteSiteAgentUrl": "192.168.2.29:8000"
	}
	```
	`~/.qkd/qnl/qll/keys/C/qkdlink.json`
	```json
	{
	    "localQKDDeviceId": "B_C_B",
	    "remoteQKDDeviceId": "B_C_C",
	    "localSiteAgentUrl": "192.168.2.22:8000",
	    "remoteSiteAgentUrl": "192.168.2.86:8000"
	}
	```
	with the correct IP addresses.
- Similarly, each site should have `~/.qkd/qnl/siteagent.json` in the following format
	```json
	{
        "url": "192.168.2.29",
        "port": 8000
	}
	```
    with the IP address corresponding to the local machine.

## Setup
1. On all sites, first copy the relevant dummy driver config files to build-cqptoolkit (the ones ending with the site ID, so dummy\_A\_B\_A for site A, both dummy\_A\_B\_B and dummy\_B\_C\_B for site B, and dummy\_B\_C\_C for site C).Then, copy the script setupTest3`ID`.sh to build-cqptoolkit and run, where `ID` is the site ID. Also make sure teardownTest.sh is present. Wait for dummy drivers to start. They will not immediately register as qkd-net hasn't started yet. On site B, make sure there are two dummy drivers with device IDs `A_B_B` and `B_C_B`.
2. On all sites, navigate to qkd-net/kms and run `./scripts/run`. Wait for it to finish, and you should soon see the dummy drivers register. You should see something that looks like this:
	```
	DEBUG:  20220228-112841.289 /home/dell1/cqptoolkit/src/KeyManagement/Sites/SiteAgent.cpp.827:RegisterDevice: Device registering: A_B_A
	INFO:  20220228-112841.289 /home/dell1/cqptoolkit/src/KeyManagement/Sites/SiteAgent.cpp.851:RegisterDevice: New Alice device: A_B_A at '192.168.2.29:9000' on switch '' port ''
	INFO:  20220228-112841.289 /home/dell1/cqptoolkit/src/Drivers/DummyQKDDriver/DummyQKDDriver.cpp.109:Main: My device id is A_B_A
	```
    Around a minute later you should see the line:
	> Device pushed first key! \
	Then, inspect `lsrp.log`, `mapping.log`, and check `~/.qkd/qnl/qll/keys` for incoming keys. Make sure the first few keys match between nodes `A` and `B`, and nodes `B` and `C`.
3. You can now navigate to `qkd-net/applications/tls-kms-demo` on sites `A` and `C`, and run the file transfer as you normally would with bob as node `C` and alice as node `A`.
