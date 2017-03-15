INSERT INTO entry (agencyId, bibliographicRecordId, timeOfLastModification) VALUES (123456, 'testId1', 'NOW'::TIMESTAMP - '1 YEAR'::INTERVAL);
INSERT INTO entry (agencyId, bibliographicRecordId, timeOfLastModification) VALUES (123456, 'testId2', 'NOW'::TIMESTAMP - '1 SECOND'::INTERVAL);
INSERT INTO entry (agencyId, bibliographicRecordId, timeOfLastModification) VALUES (123456, 'testId3', 'NOW'::TIMESTAMP + '1 SECOND'::INTERVAL);
INSERT INTO entry (agencyId, bibliographicRecordId, timeOfLastModification) VALUES (123456, 'testId4', 'NOW'::TIMESTAMP + '1 YEAR'::INTERVAL);