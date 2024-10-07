package network.bane.utils.env;

public class GetEnv {

    public static String getEnvVariable(String variableName) {
        String value = System.getenv(variableName);
        if (value == null) {
            throw new IllegalArgumentException("Environment variable " + variableName + " is not set.");
        }
        return value;
    }

    public static String getEnvVariableOrDefault(String variableName, String defaultValue) {
        String value = System.getenv(variableName);
        return value != null ? value : defaultValue;
    }

}
