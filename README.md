# Neo Contracts for N3-Bane Bridge

This repository is used for the development of the Neo contracts for the bridge between Neo N3 and Bane.

## Quickstart

[Java 8](https://adoptium.net/) (or higher) is required.  
[Docker](https://www.docker.com/products/docker-desktop) is required for running smart contract tests.

#### 1. **Clone this git repo:**

```bash
git clone https://github.com/bane-labs/bridge-neo-contracts.git
```

#### 2. **Go to the project directory:**

```bash
cd bridge-neo-contracts
```

#### 3. **Compile the smart contract for the bridge:**

```bash
./gradlew neow3jCompile -PclassName=network.bane.BridgeContract
```

#### 4. **You will see the following output in the directory `./build/neow3j`:**

```bash
$ ls -la build/neow3j
total 24
drwxr-xr-x  5 user  wheel  160 23 Feb 17:40 .
drwxr-xr-x  7 user  wheel  224 23 Feb 17:40 ..
-rw-r--r--  1 user  wheel  425 23 Feb 17:40 NeoXBridge.manifest.json
-rw-r--r--  1 user  wheel   94 23 Feb 17:40 NeoXBridge.nef
-rw-r--r--  1 user  wheel  430 23 Feb 17:40 NeoXBridge.nefdbgnfo
```

#### 5. **Compile the bridge management contract**

```bash
./gradlew neow3jCompile -PclassName=network.bane.BridgeManagementContract
```

Then, similar to the bridge contract before, you will see the following output in the directory `./build/neow3j`:

```bash
$ ls -la build/neow3j
total 24
drwxr-xr-x  5 user  wheel  160 23 Feb 17:40 .
drwxr-xr-x  7 user  wheel  224 23 Feb 17:40 ..
-rw-r--r--  1 user  wheel  425 23 Feb 17:40 NeoXBridgeManagement.manifest.json
-rw-r--r--  1 user  wheel   94 23 Feb 17:40 NeoXBridgeManagement.nef
-rw-r--r--  1 user  wheel  430 23 Feb 17:40 NeoXBridgeManagement.nefdbgnfo
```

#### 6. **Run the contract test**

```bash
./gradlew test
```

#### 7. **Deploy the bridge contracts locally**

- Run a local [Neo Express](https://github.com/neo-project/neo-express) instance. The project includes a Neo Express configuration file.
- Fund Alice's account: `neoxp transfer 100 GAS genesis alice`
- Go to the `network.bane.Deployment` class and run it.

## About

Neow3j is a Java SDK and smart contract devpack that provides easy and reliable tools to build Neo dApps and Smart Contracts using the 
Java platform (Java, Kotlin, Android).

Check out [neow3j.io](https://neow3j.io) for more information on neow3j and the technical documentation.

Neow3j is an open-source project developed by the community and maintained by [AxLabs](https://axlabs.com).
