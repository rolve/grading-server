ALTER TABLE SUBMISSION
    ADD COLUMN RESULT JSON DEFAULT '';

UPDATE SUBMISSION
SET RESULT = JSON_OBJECT(
        '@class': 'ch.trick17.gradingserver.model.ImplGradingResult',
        'properties':  ('[' || REGEXP_REPLACE(REGEXP_REPLACE(PROPERTIES,   '[^;]+', '"$0"'), ';', ',') || ']') FORMAT JSON,
        'passedTests': ('[' || REGEXP_REPLACE(REGEXP_REPLACE(PASSED_TESTS, '[^;]+', '"$0"'), ';', ',') || ']') FORMAT JSON,
        'failedTests': ('[' || REGEXP_REPLACE(REGEXP_REPLACE(FAILED_TESTS, '[^;]+', '"$0"'), ';', ',') || ']') FORMAT JSON)
WHERE ERROR IS NULL;

UPDATE SUBMISSION
SET RESULT = JSON_OBJECT(
        '@class': 'ch.trick17.gradingserver.model.ErrorResult',
        'error': ERROR)
WHERE ERROR IS NOT NULL;

ALTER TABLE SUBMISSION
    DROP COLUMN DETAILS, ERROR, FAILED_TESTS, PASSED_TESTS, PROPERTIES;
