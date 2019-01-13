package kin.devplatform.exception;

public class MigrationNeededExceptions extends BlockchainException{

    public static final String EXCEPTION_MESSAGE = "sdk and server are not in the same blockchain version so migration is needed";

    public MigrationNeededExceptions() {
        super(BLOCKCHAIN_VERSIONS_ARE_NOT_THE_SAME, EXCEPTION_MESSAGE, null);
    }
}
