package network.bane.poc;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Contract;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.ManifestExtra.ManifestExtras;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.annotations.Struct;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.StdLib;

import static io.neow3j.devpack.Helper.abort;
import static io.neow3j.devpack.Storage.getStorageContext;

@Permission(contract = "*")
@ManifestExtras({@ManifestExtra(key = "Author", value = "BaneLabs"),
        @ManifestExtra(key = "Description", value = "Arbitration Message Executor"),
        @ManifestExtra(key = "Source", value = "https://github.com/bane-labs/bridge-neo-contracts")})
public class ArbMsgExecutor {

    private static final StdLib stdLib = new StdLib();

    private static final int arbMsgsPrefix = 0x10;
    private static final StorageContext context = getStorageContext();
    private static final StorageMap arbMsgsMap = new StorageMap(context, arbMsgsPrefix);

    // Base Storage Keys
    private static final int ARB_MSGS_CURRENT_ID_KEY = 0x01;

    @OnDeployment
    public static void deploy(Object data, boolean update) {
        if (!update) {
            Storage.put(context, ARB_MSGS_CURRENT_ID_KEY, 0);
        }
    }

    @Struct
    public static class ArbMsgContainer {
        public ArbMsg arbMsg;
        public int state;
    }

    public static class ArbMsgExecutionState {
        public static final int PENDING = 1;
        public static final int REVERTED = 1 << 2; // 4
        public static final int EXECUTED = 1 << 4; // 8
    }

    @Struct
    public static class ArbMsg {
        public List<Invocation> invocations;

        public ArbMsg(List<Invocation> invocations) {
            this.invocations = invocations;
        }

        public static class Invocation {
            public Hash160 contract;
            public String method;
            public byte callFlags;
            public Object[] args;

            public Invocation(Hash160 contract, String method, byte callFlags, Object[] args) {
                this.contract = contract;
                this.method = method;
                this.callFlags = callFlags;
                this.args = args;
            }
        }
    }

    @Safe
    public Object getArbMsg(int id) {
        return arbMsgsMap.get(id);
    }

    public static void storeArbMsg(ByteString serializedArbMsg) {
        if (serializedArbMsg == null || serializedArbMsg.length() == 0) {
            abort("Serialized ArbMsg cannot be null or empty.");
        }
    }

    public static List<Object> execute(int id) {
        ArbMsg arbMsg = getArbMsgDeserialized(id);
        List<ArbMsg.Invocation> invocations = arbMsg.invocations;
        int nrInvocations = invocations.size();

        List<Object> returnValues = new List<>();

        for (int i = 0; i < nrInvocations; i++) {
            ArbMsg.Invocation invoc = invocations.get(i);
            if (invoc.args == null) {
                invoc.args = new Object[0];
            }
            Object returnValue = Contract.call(invoc.contract, invoc.method, invoc.callFlags, invoc.args);
            returnValues.add(returnValue);
        }
        return returnValues;
    }

    @Safe
    public static ArbMsg getArbMsgDeserialized(int id) {
        return (ArbMsg) stdLib.deserialize(arbMsgsMap.get(id));
    }

    @Safe
    public static List<ArbMsg.Invocation> getInvocations(int id) {
        ArbMsg arbMsg = getArbMsgDeserialized(id);
        if (arbMsg == null) {
            abort("ArbMsg with id " + id + " does not exist.");
        }
        return arbMsg.invocations;
    }

    public static void revert(int id) {
        // Todo: Consider adding logic that allows to withdraw an unexecuted ArbMsg.
    }

    public static void update(ByteString nefFile, String manifest, Object data) {
        new ContractManagement().update(nefFile, manifest, data);
    }

}
