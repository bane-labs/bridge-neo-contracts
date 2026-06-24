# Neo Contracts for the N3 - Neo X Bridge

This repository is used for the development of the Neo contracts for the bridge between Neo N3 and Neo X.

## Mainnet

| Contract             | Hash                                         |
|----------------------|----------------------------------------------|
| NeoXBridgeManagement | `0x148b3e0ca4f77476252862645e58f06b2562c414` |
| NeoXBridge           | `0xbb19cfc864b73159277e1fd39694b3fd5fc613d2` |
| MessageBridge        | `0xde9cc59df7484ae2f2d2c1ce7a66dccbd5c8bd11` | 
| ExecutionManager     | `0x1c8a07253c148555bb22cfd514ef7f8777b0dfea` | 

## Testnet (for Neo X T4)

| Contract             | Hash                                         |
|----------------------|----------------------------------------------|
| NeoXBridgeManagement | `0x7c83816fb00b95b87119f7e28f7b044e4ac7bac8` |
| NeoXBridge           | `0x2ba94444d43c9a084a5660982a9f95f43f07422e` |
| MessageBridge        | `0xde9cc59df7484ae2f2d2c1ce7a66dccbd5c8bd11` |
| ExecutionManager     | `0x1c8a07253c148555bb22cfd514ef7f8777b0dfea` |

## Quickstart

[Java 8](https://adoptium.net/) is required for compiling the smart contracts. The contracts are compiled as Java 8 class files and the neow3j compiler works from those class files. [Docker](https://www.docker.com/products/docker-desktop) is required for running smart contract tests.

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
./gradlew neow3jCompile -PclassName=network.bane.bridge.BridgeContract
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
./gradlew neow3jCompile -PclassName=network.bane.management.BridgeManagementContract
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
- Run the deployment script:

```bash
./gradlew runOps -PmainClass=network.bane.scripts.deploy.DeployAll
```

## Project Layout

- `src/contracts/java` contains smart contract code. Gradle wires this directory as the
  `main` source set for compatibility with the neow3j Gradle plugin.
- `src/client/java` contains off-chain client wrappers, DTOs, and helper code.
- `src/ops/java` contains deployment, initialization, on-chain reads, transaction sending, and admin/governor scripts.
- `src/test/java` contains tests. Tests can use the `client` source set.

## About

Neow3j is a Java SDK and smart contract devpack that provides easy and reliable tools to build Neo dApps and Smart Contracts using the Java platform (Java, Kotlin, Android).

Check out [neow3j.io](https://neow3j.io) for more information on neow3j and the technical documentation.

Neow3j is an open-source project developed by the community and maintained by [AxLabs](https://axlabs.com).
