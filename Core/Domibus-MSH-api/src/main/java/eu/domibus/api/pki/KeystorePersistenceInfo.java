package eu.domibus.api.pki;

/**
 * Interface describing the persistence of a keyStore
 *
 * @author Ion Perpegel
 * @since 5.1
 */
public interface KeystorePersistenceInfo {

    /**
     * The name of the keyStore
     *
     * @return
     */
    String getName();

    /**
     * if the keyStore must exist or not
     *
     * @return
     */
    boolean isOptional();

    /**
     * The file location of the keyStore
     *
     * @return
     */
    String getFileLocation();

    void setFileLocation(String filLocation);

    /**
     * The type of the keyStore
     *
     * @return
     */
    String getType();

    void setType(String type);

    /**
     * The password of the keyStore
     *
     * @return
     */
    String getPassword();

}
