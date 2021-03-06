/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU 3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.phlog.dto;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.Map;

@Entity
@Table(name = "entry")
@NamedQueries({
    @NamedQuery(name = PhLogEntry.GET_ENTRIES_WITH_TIME_OF_LAST_MODIFICATION_IN_INTERVAL_QUERY_NAME,
                query = PhLogEntry.GET_ENTRIES_WITH_TIME_OF_LAST_MODIFICATION_IN_INTERVAL_QUERY)
})
public class PhLogEntry {
    public static final String GET_ENTRIES_WITH_TIME_OF_LAST_MODIFICATION_IN_INTERVAL_QUERY_NAME = "PhLogEntry.getEntriesModifiedBetween";
    public static final String GET_ENTRIES_WITH_TIME_OF_LAST_MODIFICATION_IN_INTERVAL_QUERY =
            "SELECT logEntry FROM PhLogEntry logEntry WHERE logEntry.timeOfLastModification >= :after AND logEntry.timeOfLastModification < :before";

    @EmbeddedId
    private Key key;
    private Boolean deleted;
    private Timestamp timeOfLastModification;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = StatusMapConverter.class)
    private Map<String, Integer> holdingsStatusMap;

    public Key getKey() {
        return key;
    }

    public PhLogEntry withKey(Key key) {
        this.key = key;
        return this;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public PhLogEntry withDeleted(Boolean deleted) {
        this.deleted = deleted;
        return this;
    }

    public Map<String, Integer> getHoldingsStatusMap() {
        return holdingsStatusMap;
    }

    public PhLogEntry withHoldingsStatusMap(Map<String, Integer> holdingsStatusMap) {
        this.holdingsStatusMap = holdingsStatusMap;
        return this;
    }

    public Timestamp getTimeOfLastModification() {
        return timeOfLastModification;
    }

    @PrePersist
    @PreUpdate
    void onDatabaseCommit() {
        this.timeOfLastModification = new Timestamp(System.currentTimeMillis());
    }

    @Override
    public String toString() {
        return "PhLogEntry{" +
                "key=" + key +
                ", deleted=" + deleted +
                ", timeOfLastModification=" + timeOfLastModification +
                '}';
    }

    @Embeddable
    public static class Key {
        private Integer agencyId;
        private String bibliographicRecordId;

        public Integer getAgencyId() {
            return agencyId;
        }

        public Key withAgencyId(Integer agencyId) {
            this.agencyId = agencyId;
            return this;
        }

        public String getBibliographicRecordId() {
            return bibliographicRecordId;
        }

        public Key withBibliographicRecordId(String bibliographicRecordId) {
            this.bibliographicRecordId = bibliographicRecordId;
            return this;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "agencyId=" + agencyId +
                    ", bibliographicRecordId='" + bibliographicRecordId + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Key key = (Key) o;

            if (agencyId != null ? !agencyId.equals(key.agencyId) : key.agencyId != null) {
                return false;
            }
            return bibliographicRecordId != null ? bibliographicRecordId.equals(key.bibliographicRecordId) : key.bibliographicRecordId == null;
        }

        @Override
        public int hashCode() {
            int result = agencyId != null ? agencyId.hashCode() : 0;
            result = 31 * result + (bibliographicRecordId != null ? bibliographicRecordId.hashCode() : 0);
            return result;
        }
    }
}
