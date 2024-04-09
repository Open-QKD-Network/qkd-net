# ETSI Integration
> temporary document to brainstorm and track work.
> [ETSI API](https://www.etsi.org/deliver/etsi_gs/QKD/001_099/014/01.01.01_60/gs_qkd014v010101p.pdf)

### Task
- on both nodes: read the key from a real QKD hardware device that provides an ETSI API and save it into `.qkd/qnl/qll/keys`.
- Overall flow:
![illustration of key flow](./overall%20flow.png)
    - Assume since A < B (alphabetically/lexicographically), 
    1. A calls **getKey**. Receives key.
    2. A notifies B and gives it the key information.
    3. B calls **getKey with key info** and gets the key.
    4. A and B save the key to file. We needs the files to be **synchronized** i.e. this step needs to be atomic i.e. only of the 2 should happen:
        - Both of them save the key to file
        - None of them save the key to file
        > Clarification: We are not responsible for ensuring the content of the files is the same; that is QKD's responsibility. We're responsible for ensuring the file getting saved is atomic. 

**Task #1**: Come up with a mechanism for site A to notify site B & send the key information.

**Task #2**: How the files are synchronized

### Revised Overall Flow
> Adapted from TCP's mechanism for [creating connections](https://en.wikipedia.org/wiki/Transmission_Control_Protocol#Connection_establishment).

In a diagram:
![illustration of key flow](./revised%20flow.png)
In text:
1. A calls **getKey**. Receives key.
2. `SYN`: A sends to B `{ keyInfo, random number alpha }`.
3. B retrieves key by calling **getKey with key info**.
4. `SYN_ACK`: B sends to A `{ (alpha + 1), new random number beta}`
5. A does the following
    - `ACK`: sends `{ beta+ 1 }` to B
    - waits for `x=30 seconds` (we can experiment & change this value)
    - saves key to file
6. Upon receivng the `ACK`, B saves the key to file.
