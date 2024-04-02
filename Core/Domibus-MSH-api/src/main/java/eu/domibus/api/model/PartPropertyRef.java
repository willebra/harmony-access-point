package eu.domibus.api.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author Ion Perpegel
 * @since 5.0.8
 */
@Entity
@Table(name = "TB_PART_PROPERTIES")
public class PartPropertyRef implements Serializable {

    @Id
    @Column(name = "PART_INFO_PROPERTY_FK")
    protected Long propertyId;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "PART_INFO_ID_FK")
    private PartInfo partInfo;

    public PartInfo getPartInfo() {
        return partInfo;
    }

    public void setPartInfo(PartInfo partInfo) {
        this.partInfo = partInfo;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("propertyId", propertyId)
                .append("partInfo", partInfo)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        PartPropertyRef partPropertyRef = (PartPropertyRef) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(partInfo, partPropertyRef.partInfo)
                .append(propertyId, partPropertyRef.propertyId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(partInfo)
                .append(propertyId)
                .toHashCode();
    }

}
