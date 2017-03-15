/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU 3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.phlog;

import dk.dbc.phlog.dto.PhLogEntry;
import org.eclipse.persistence.queries.CursoredStream;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Iterator;

/**
 * This class contains the phlog repository API
 */
@Stateless
public class PhLog {
    @PersistenceContext(unitName = "phLogPU")
    EntityManager entityManager;

    public PhLog() {}

    public PhLog(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Finds all entries with timeOfLastModification in interval [after, before[
     * @param after lower bound for timeOfLastModification (inclusive)
     * @param before upper bound for timeOfLastModification (exclusive)
     * @return iterator as ResultSet abstraction
     */
    public ResultSet<PhLogEntry> getEntriesModifiedBetween(Instant after, Instant before) {
        final Query query = entityManager.createNamedQuery(PhLogEntry.GET_ENTRIES_WITH_TIME_OF_LAST_MODIFICATION_IN_INTERVAL_QUERY_NAME)
                .setHint("eclipselink.refresh", true)
                .setParameter("after", Timestamp.from(after))
                .setParameter("before", Timestamp.from(before));
        return new ResultSet<>(query);
    }

    /**
     * This class represents a one-time iteration of a phlog repository result set
     */
    public class ResultSet<T> implements Iterable<T>, AutoCloseable {
        final CursoredStream cursor;

        ResultSet(Query query) {
            // Yes we are breaking general JPA compatibility here,
            // but we need to be able to handle very large result sets.
            query.setHint("eclipselink.cursor", true);
            cursor = (CursoredStream) query.getSingleResult();
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                @Override
                public boolean hasNext() {
                    return cursor.hasNext();
                }

                @Override
                @SuppressWarnings("unchecked")
                public T next() {
                    return (T) cursor.next();
                }
            };
        }

        @Override
        public void close() {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
