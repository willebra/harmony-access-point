package eu.domibus.api.model;

import eu.domibus.api.ebms3.model.Ebms3Property;
import eu.domibus.api.payload.PartInfoService;
import eu.domibus.api.spring.SpringContextProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.activation.DataHandler;
import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Cosmin Baciu
 * @since 5.0
 */
@NamedQueries({
        @NamedQuery(name = "PartInfo.findPartInfos", query = "select distinct pi from PartInfo pi left join fetch pi.partPropertyRefs where pi.userMessage.entityId=:ENTITY_ID order by pi.partOrder"),
        @NamedQuery(name = "PartInfo.findPartInfoByUserMessageEntityIdAndCid", query = "select distinct pi from PartInfo pi left join fetch pi.partPropertyRefs where pi.userMessage.entityId=:ENTITY_ID and pi.href=:CID"),
        @NamedQuery(name = "PartInfo.findPartInfoByUserMessageIdAndCid", query = "select distinct pi from PartInfo pi left join fetch pi.partPropertyRefs where pi.userMessage.messageId=:MESSAGE_ID and pi.href=:CID"),
        @NamedQuery(name = "PartInfo.findFilenames", query = "select pi.fileName from PartInfo pi where pi.userMessage.entityId IN :MESSAGEIDS and pi.fileName is not null"),
        @NamedQuery(name = "PartInfo.emptyPayloads", query = "update PartInfo p set p.binaryData = null where p in :PARTINFOS"),
        @NamedQuery(name = "PartInfo.findPartInfosLength", query = "select pi.length from PartInfo pi where pi.userMessage.entityId=:ENTITY_ID"),
})
@Entity
@Table(name = "TB_PART_INFO")
public class PartInfo extends AbstractBaseEntity implements Comparable<PartInfo> {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PartInfo.class);

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_MESSAGE_ID_FK")
    protected UserMessage userMessage;

    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY, mappedBy = "partInfo")
    private Set<PartPropertyRef> partPropertyRefs;

    @Transient
    private Set<PartProperty> partProperties; //NOSONAR

    @Embedded
    protected Description description; //NOSONAR

    @Column(name = "HREF")
    protected String href;

    @Lob
    @Column(name = "BINARY_DATA")
    @Basic(fetch = FetchType.EAGER)
    protected byte[] binaryData;

    @Column(name = "FILENAME")
    protected String fileName;

    @Column(name = "IN_BODY")
    protected boolean inBody;

    @Transient
    protected DataHandler payloadDatahandler; //NOSONAR

    @Column(name = "MIME")
    private String mime;

    @Column(name = "PART_LENGTH")
    protected long length = -1;

    @Column(name = "PART_ORDER")
    private int partOrder = 0;

    @Column(name = "ENCRYPTED")
    protected Boolean encrypted;

    @Column(name = "COMPRESSED")
    protected Boolean compressed;

    @Transient
    public String getMimeProperty() {
        return partProperties.stream()
                .filter(Objects::nonNull)
                .filter(partProperty -> StringUtils.equalsIgnoreCase(partProperty.getName(), Ebms3Property.MIME_TYPE))
                .findFirst()
                .map(Property::getValue)
                .orElse(null);
    }

    @Transient
    public DataHandler getPayloadDatahandler() {
        return payloadDatahandler;
    }

    @Transient
    public void setPayloadDatahandler(final DataHandler payloadDatahandler) {
        this.payloadDatahandler = payloadDatahandler;
    }

    public byte[] getBinaryData() {
        return binaryData;
    }

    public void setBinaryData(byte[] binaryData) {
        this.binaryData = binaryData;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(final String mime) {
        this.mime = mime;
    }

    public boolean isInBody() {
        return this.inBody;
    }

    public void setInBody(final boolean inBody) {
        this.inBody = inBody;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isEncrypted() {
        return BooleanUtils.toBoolean(encrypted);
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public Boolean getCompressed() {
        return BooleanUtils.toBoolean(compressed);
    }

    public void setCompressed(Boolean compressed) {
        this.compressed = compressed;
    }

    public UserMessage getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(UserMessage userMessage) {
        this.userMessage = userMessage;
    }

    @Transient
    public Set<PartPropertyRef> getPartPropertyRefs() {
        return partPropertyRefs;
    }

    @Transient
    public void setPartPropertyRefs(Set<PartPropertyRef> partPropertyRefs) {
        this.partPropertyRefs = partPropertyRefs;
    }

    @Transient
    public Set<PartProperty> getPartProperties() {
        return partProperties;
    }

    @Transient
    public void setPartProperties(Set<PartProperty> partProperties) {
        this.partProperties = partProperties;

        if (propertiesExist(partProperties)) {
            LOG.debug("partProperties already set");
            return;
        }

        this.partPropertyRefs = new HashSet<>();
        if (CollectionUtils.isEmpty(partProperties)) {
            LOG.debug("partProperties is empty");
            return;
        }

        partProperties.forEach(this::doAddPropertyRef);
    }

    @Transient
    public void addProperty(PartProperty partProperty) {
        if (partProperty == null) {
            LOG.debug("partProperty is null");
            return;
        }

        if (!propertyExists(partProperty)) {
            if (this.partProperties == null) {
                this.partProperties = new HashSet<>();
            }
            this.partProperties.add(partProperty);
        } else {
            LOG.debug("partProperty already present in partProperties");
        }

        if (!propertyRefExists(partProperty)) {
            doAddPropertyRef(partProperty);
        } else {
            LOG.debug("partProperty already present in property refs");
        }
    }

    @Transient
    public void removeProperty(PartProperty partProperty) {
        if (partProperty == null) {
            LOG.debug("partProperty is null");
            return;
        }

        PartProperty existing = getPropertyByName(partProperty);
        if (existing != null) {
            this.partProperties.remove(existing);
            if (propertyRefExists(existing)) {
                doRemovePropertyRef(existing);
            } else {
                LOG.debug("partProperty [{}] does not exist in ref collection.", partProperty);
            }
        } else {
            LOG.debug("partProperty not present in partProperties");
        }
    }

    @PostLoad
    public void loadBinary() {
        final PartInfoService partInfoService = SpringContextProvider.getApplicationContext().getBean("partInfoServiceImpl", PartInfoService.class);
        partInfoService.loadBinaryData(this);
    }

    public Description getDescription() {
        return this.description;
    }

    public void setDescription(final Description value) {
        this.description = value;
    }

    public String getHref() {
        return this.href;
    }

    public void setHref(final String value) {
        this.href = value;
    }

    @Transient
    public long getLength() {
        return length;
    }

    @Transient
    public void setLength(long length) {
        this.length = length;
    }

    public void setPartOrder(int partOrder) {
        this.partOrder = partOrder;
    }

    public int getPartOrder() {
        return partOrder;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("description", description)
                .append("partProperties", partProperties)
                .append("href", href)
                .append("binaryData", binaryData)
                .append("fileName", fileName)
                .append("inBody", inBody)
                .append("payloadDatahandler", payloadDatahandler)
                .append("mime", mime)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        PartInfo partInfo = (PartInfo) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(description, partInfo.description)
                //.append(partProperties, partInfo.partProperties)
                .append(href, partInfo.href)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .appendSuper(super.hashCode())
                .append(description)
                // .append(partProperties)
                .append(href)
                .toHashCode();
    }

    @Override
    public int compareTo(final PartInfo o) {
        return this.hashCode() - o.hashCode();
    }

    private boolean propertiesExist(Set<PartProperty> partProperties) {
        if (this.partPropertyRefs == null && partProperties == null) {
            LOG.debug("partProperties and partPropertyRefs are null->true");
            return true;
        }
        if (this.partPropertyRefs == null || partProperties == null) {
            LOG.debug("partProperties or partPropertyRefs are null->false");
            return false;
        }
        if (this.partPropertyRefs.size() != partProperties.size()) {
            LOG.debug("partProperties size different than partPropertyRefs size->false");
            return false;
        }
        List<Long> l1 = partProperties.stream().map(prop -> prop.getEntityId()).collect(Collectors.toList());
        List<Long> l2 = this.partPropertyRefs.stream().map(ref -> ref.propertyId).collect(Collectors.toList());
        return CollectionUtils.isEqualCollection(l1, l2);
    }

    private boolean propertyRefExists(PartProperty partProperty) {
        if (this.partPropertyRefs == null) {
            return false;
        }
        return this.partPropertyRefs.stream().anyMatch(ref -> ref.propertyId == partProperty.getEntityId());
    }

    private void doAddPropertyRef(PartProperty partProperty) {
        PartPropertyRef partPropertyRef = new PartPropertyRef();
        partPropertyRef.setPropertyId(partProperty.getEntityId());
        partPropertyRef.setPartInfo(this);
        if (partPropertyRefs == null) {
            this.partPropertyRefs = new HashSet<>();
        }
        this.partPropertyRefs.add(partPropertyRef);
    }

    private void doRemovePropertyRef(PartProperty partProperty) {
        Optional<PartPropertyRef> propertyRef = this.partPropertyRefs.stream()
                .filter(ref -> ref.propertyId == partProperty.getEntityId())
                .findFirst();
        if (propertyRef.isPresent()) {
            this.partPropertyRefs.remove(propertyRef.get());
        }
    }

    private boolean propertyExists(PartProperty partProperty) {
        return getPropertyByName(partProperty) != null;
    }

    private PartProperty getPropertyByName(PartProperty partProperty) {
        if (this.partProperties == null)
            return null;

        Optional<PartProperty> propRef = this.partProperties.stream().filter(prop -> Objects.equals(prop.getName(), partProperty.getName())).findFirst();
        if (!propRef.isPresent()) {
            return null;
        }
        return propRef.get();
    }
}
