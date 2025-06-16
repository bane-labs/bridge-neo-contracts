package network.bane.poc;

import io.neow3j.devpack.Storage;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.EventParameterNames;
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.events.Event2Args;

import static io.neow3j.devpack.Storage.getStorageContext;

@Permission(contract = "*")
public class SimpleStore {

    @DisplayName("Store")
    @EventParameterNames({"Key", "Value"})
    public static Event2Args<Integer, String> onStore;

    public static boolean storeEmitAndReturnSuccess(int key, String value) {
        if (Storage.get(getStorageContext(), key) != null) {
            return false;
        }
        Storage.put(getStorageContext(), key, value);
        onStore.fire(key, value);
        return true;
    }

    @Safe
    public static String get(int key) {
        return Storage.getString(getStorageContext(), key);
    }
}
