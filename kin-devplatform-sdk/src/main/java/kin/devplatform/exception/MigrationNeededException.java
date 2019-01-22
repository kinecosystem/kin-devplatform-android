package kin.devplatform.exception;

public class MigrationNeededException extends BlockchainException{

    public static final String EXCEPTION_MESSAGE = "The user wallet has been migrated to the Kin blockchain. No Kin transactions will succeed until the client is migrated too.";

    public MigrationNeededException() {
        super(MIGRATION_IS_NEEDED, EXCEPTION_MESSAGE, null);
    }
}
