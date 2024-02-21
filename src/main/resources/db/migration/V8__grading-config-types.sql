UPDATE PROBLEM_SET
    SET GRADING_CONFIG = ('{"@class":"ch.trick17.gradingserver.model.ImplGradingConfig",' || SUBSTR(GRADING_CONFIG, 2)) FORMAT JSON;
