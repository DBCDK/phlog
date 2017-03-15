/*
Copyright Dansk Bibliotekscenter a/s. Licensed under GNU 3
See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
*/

CREATE TABLE entry (
  agencyId                INTEGER NOT NULL,
  bibliographicRecordId   TEXT NOT NULL,
  deleted                 BOOLEAN NOT NULL DEFAULT FALSE,
  timeOfLastModification  TIMESTAMP NOT NULL DEFAULT clock_timestamp(),
  PRIMARY KEY (agencyId, bibliographicRecordId)
);
CREATE INDEX entry_timeOfLastModification_index ON entry(timeOfLastModification);
