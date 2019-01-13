package kin.devplatform;

public interface KinMigrationListener {

    /**
     * Method is invoked before the migration process itself will start.
     * <p><b>Note:</b> This method may not be called.</p>
     */
    void onStart();

    /**
     * Method is invoked when migration is finished, whether there was an actual migration or not.
     */
    void onFinish();

    /**
     * Method is invoked when an error is occurred
     * @param e the exception of the error.
     */
    void onError(Exception e); // TODO: 09/01/2019 maybe use the devplatfor base exception?

}
