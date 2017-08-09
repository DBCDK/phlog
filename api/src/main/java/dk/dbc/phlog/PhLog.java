/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU 3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.phlog;

import dk.dbc.phlog.dto.PhLogEntry;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
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
                .setParameter("after", Timestamp.from(after))
                .setParameter("before", Timestamp.from(before));
        return new ResultSet<>(query);
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * This class represents a one-time iteration of a phlog repository result set of non-managed entities.
     */
    public class ResultSet<T> implements Iterable<T>, AutoCloseable {
        private final int BUFFER_SIZE = 50;

        final CursoredStream cursor;

        ResultSet(Query query) {
            // Yes we are breaking general JPA compatibility using below QueryHints and CursoredStream,
            // but we need to be able to handle very large result sets.

            // Configures the query to return a CursoredStream, which is a stream of the JDBC ResultSet.
            query.setHint(QueryHints.CURSOR, HintValues.TRUE);
            // Configures the CursoredStream with the number of objects fetched from the stream on a next() call.
            query.setHint(QueryHints.CURSOR_PAGE_SIZE, BUFFER_SIZE);
            // Configures the JDBC fetch-size for the result set.
            query.setHint(QueryHints.JDBC_FETCH_SIZE, BUFFER_SIZE);
            // Configures the query to not use the shared cache and the transactional cache/persistence context.
            // Resulting objects will be read and built directly from the database, and not registered in the
            // persistence context. Changes made to the objects will not be updated unless merged and object identity
            // will not be maintained.
            // This is necessary to avoid OutOfMemoryError from very large persistence contexts.
            query.setHint(QueryHints.MAINTAIN_CACHE, HintValues.FALSE);

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
                    // To avoid OutOfMemoryError we occasionally need to clear the internal data structure of the
                    // CursoredStream.
                    if (cursor.getPosition() % BUFFER_SIZE == 0) {
                        cursor.clear();
                    }
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
