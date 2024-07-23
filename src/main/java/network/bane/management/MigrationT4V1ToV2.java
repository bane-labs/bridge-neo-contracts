package network.bane.management;

import io.neow3j.devpack.Account;
import io.neow3j.devpack.Hash160;

import static network.bane.management.BridgeManagementContract.key_governor;
import static network.bane.management.BridgeManagementContract.key_owner;
import static network.bane.management.BridgeManagementContract.key_relayer;
import static network.bane.management.BridgeManagementContract.key_securityguard;

public class MigrationT4V1ToV2 {

    static void migrate() {
        Hash160 newOwner = Account.createStandardAccount(BridgeManagementContract.baseMap.getECPoint(key_owner));
        BridgeManagementContract.baseMap.put(key_owner, newOwner);

        Hash160 newRelayer = Account.createStandardAccount(BridgeManagementContract.baseMap.getECPoint(key_relayer));
        BridgeManagementContract.baseMap.put(key_relayer, newRelayer);

        Hash160 newGovernor = Account.createStandardAccount(BridgeManagementContract.baseMap.getECPoint(key_governor));
        BridgeManagementContract.baseMap.put(key_governor, newGovernor);

        Hash160 newSecurityGuard =
                Account.createStandardAccount(BridgeManagementContract.baseMap.getECPoint(key_securityguard));
        BridgeManagementContract.baseMap.put(key_securityguard, newSecurityGuard);
    }

}
