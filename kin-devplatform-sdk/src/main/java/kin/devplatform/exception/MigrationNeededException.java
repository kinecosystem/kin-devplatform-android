package kin.devplatform.exception;

public class MigrationNeededException extends BlockchainException{

    public static final String EXCEPTION_MESSAGE = "Server is migrated to new kin blockchain so client(SDK) migration is also needed";

    public MigrationNeededException() {
        super(MIGRATION_IS_NEEDED, EXCEPTION_MESSAGE, null);
    }
}
