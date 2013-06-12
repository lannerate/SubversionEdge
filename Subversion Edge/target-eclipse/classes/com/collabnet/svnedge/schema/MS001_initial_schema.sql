CREATE MEMORY TABLE CATEGORY (
  ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY,
  VERSION BIGINT NOT NULL,
  NAME VARCHAR(255) NOT NULL
);

CREATE MEMORY TABLE INTERVAL (
  ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY,
  VERSION BIGINT NOT NULL,
  SECONDS BIGINT NOT NULL,
  NAME VARCHAR(255) NOT NULL);

CREATE MEMORY TABLE REPLICATED_REPOSITORY (
  ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY,
  VERSION BIGINT NOT NULL,
  ENABLED BOOLEAN NOT NULL,
  STATUS VARCHAR(255) NOT NULL,
  LAST_SYNC_REV BIGINT NOT NULL,
  REPO_ID BIGINT NOT NULL,
  LAST_SYNC_TIME BIGINT NOT NULL,
  STATUS_MSG VARCHAR(255),
  CONSTRAINT CTUQ_RR_REPO_ID UNIQUE(REPO_ID)
);

CREATE MEMORY TABLE REPOSITORY (
  ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY,
  VERSION BIGINT NOT NULL,
  NAME VARCHAR(255) NOT NULL,
  PERMISSIONS_OK BOOLEAN NOT NULL,
  CONSTRAINT CTUQ_R_NAME UNIQUE(NAME)
);

CREATE MEMORY TABLE ROLE (
  ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY,
  VERSION BIGINT NOT NULL,
  AUTHORITY VARCHAR(255) NOT NULL,
  DESCRIPTION VARCHAR(255) NOT NULL,
  CONSTRAINT CTUQ_ROLE_AUTHORITY UNIQUE(AUTHORITY)
);

CREATE MEMORY TABLE GROUPS (
  ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY,
  VERSION BIGINT NOT NULL,
  AUTHORITY VARCHAR(255) NOT NULL,
  DESCRIPTION VARCHAR(255) NOT NULL,
  NAME VARCHAR(255) NOT NULL,
  CONSTRAINT CTUQ_GROUP_AUTHORITY UNIQUE(AUTHORITY),
  CONSTRAINT CTUQ_GROUP_NAME UNIQUE(NAME)
);

CREATE MEMORY TABLE ROLE_PEOPLE (
  ROLE_ID BIGINT NOT NULL,
  USER_ID BIGINT NOT NULL,
  PRIMARY KEY(ROLE_ID, USER_ID),
  CONSTRAINT FK_RP_ROLE_ID FOREIGN KEY(ROLE_ID) REFERENCES ROLE(ID)
);

CREATE MEMORY TABLE GROUPS_PEOPLE (
  GROUP_ID BIGINT NOT NULL,
  USER_ID BIGINT NOT NULL,
  PRIMARY KEY(GROUP_ID, USER_ID),
  CONSTRAINT FK_GP_GROUP_ID FOREIGN KEY(GROUP_ID) REFERENCES GROUPS(ID)
);

CREATE MEMORY TABLE SERVER (
  ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY,
  VERSION BIGINT NOT NULL,
  NET_INTERFACE VARCHAR(255) NOT NULL,
  PORT INTEGER NOT NULL,
  LDAP_ENABLED BOOLEAN NOT NULL,
  LDAP_FILTER VARCHAR(255),
  REPO_PARENT_DIR VARCHAR(255) NOT NULL,
  LDAP_SERVER_CERT_VERIFICATION_NEEDED BOOLEAN NOT NULL,
  LDAP_AUTH_BINDDN VARCHAR(255),
  LDAP_LOGIN_ATTRIBUTE VARCHAR(255),
  LDAP_SEARCH_SCOPE VARCHAR(255),
  LDAP_SECURITY_LEVEL VARCHAR(255),
  REPLICA BOOLEAN NOT NULL,
  IP_ADDRESS VARCHAR(255) NOT NULL,
  LDAP_SERVER_PORT INTEGER NOT NULL,
  ADMIN_NAME VARCHAR(255),
  FILE_LOGIN_ENABLED BOOLEAN NOT NULL,
  HOSTNAME VARCHAR(255) NOT NULL,
  ADMIN_ALT_CONTACT VARCHAR(255),
  LDAP_SERVER_HOST VARCHAR(255),
  ADMIN_EMAIL VARCHAR(255) NOT NULL,
  PRUNE_LOGS_OLDER_THAN INTEGER,
  DEFAULT_START BOOLEAN NOT NULL,
  LDAP_AUTH_BIND_PASSWORD VARCHAR(255),
  LDAP_AUTH_BASEDN VARCHAR(255),
  USE_SSL BOOLEAN NOT NULL,
  ALLOW_ANONYMOUS_READ_ACCESS BOOLEAN NOT NULL,
  CONSTRAINT CTUQ_S_HOSTNAME UNIQUE(HOSTNAME)
);

CREATE MEMORY TABLE STAT_ACTION (
  ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY,
  VERSION BIGINT NOT NULL,
  DELETE_ID BIGINT,
  GROUP_ID BIGINT,
  CONSOLIDATE_SOURCE_ID BIGINT,
  COLLECT_ID BIGINT NOT NULL,
  CONSTRAINT FK_SA_DELETE_ID FOREIGN KEY(DELETE_ID) REFERENCES INTERVAL(ID),
  CONSTRAINT FK_SA_SOURCE_ID FOREIGN KEY(CONSOLIDATE_SOURCE_ID) REFERENCES STAT_ACTION(ID),
  CONSTRAINT FK_SA_COLLECT_ID FOREIGN KEY(COLLECT_ID) REFERENCES INTERVAL(ID)
);

CREATE MEMORY TABLE STAT_GROUP (
  ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY,
  VERSION BIGINT NOT NULL,
  IS_REPLICA BOOLEAN NOT NULL,
  UNIT_ID BIGINT NOT NULL,
  CATEGORY_ID BIGINT NOT NULL,
  TITLE VARCHAR(255) NOT NULL,
  NAME VARCHAR(255) NOT NULL,
  CONSTRAINT CTUQ_SG_NAME UNIQUE(NAME),
  CONSTRAINT FK_SG_CATEGORY_ID FOREIGN KEY(CATEGORY_ID) REFERENCES CATEGORY(ID)
);

CREATE MEMORY TABLE STAT_VALUE (
  ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY,
  VERSION BIGINT NOT NULL,
  INTERVAL BIGINT NOT NULL,
  AVERAGE_VALUE DOUBLE NOT NULL,
  MIN_VALUE DOUBLE NOT NULL,
  UPLOADED BOOLEAN NOT NULL,
  LAST_VALUE DOUBLE NOT NULL,
  STATISTIC_ID BIGINT NOT NULL,
  TIMESTAMP BIGINT NOT NULL,
  REPO_ID BIGINT,
  DERIVED BOOLEAN NOT NULL,
  MAX_VALUE DOUBLE NOT NULL,
  CONSTRAINT FK_SV_REPO_ID FOREIGN KEY(REPO_ID) REFERENCES REPOSITORY(ID)
);

CREATE MEMORY TABLE STATISTIC (
  ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY,
  VERSION BIGINT NOT NULL,
  TITLE VARCHAR(255) NOT NULL,
  NAME VARCHAR(255) NOT NULL,
  TYPE VARCHAR(255) NOT NULL,
  GROUP_ID BIGINT NOT NULL,
  CONSTRAINT CTUQ_STAT_NAME UNIQUE(NAME),
  CONSTRAINT FK_STAT_GROUP_ID FOREIGN KEY(GROUP_ID) REFERENCES STAT_GROUP(ID)
);

CREATE MEMORY TABLE SVN_LOG (
  ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY,
  VERSION BIGINT NOT NULL,
  TIMESTAMP BIGINT NOT NULL,
  REVISION VARCHAR(255) NOT NULL,
  USERNAME VARCHAR(255) NOT NULL,
  REPO_ID BIGINT,
  PATH VARCHAR(255) NOT NULL,
  ACTION VARCHAR(255) NOT NULL,
  LINE_NUMBER INTEGER NOT NULL,
  CONSTRAINT FK_SLOG_REPO_ID FOREIGN KEY(REPO_ID) REFERENCES REPOSITORY(ID)
);
  
CREATE MEMORY TABLE UNIT (
  ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY,
  VERSION BIGINT NOT NULL,
  FORMATTER VARCHAR(255),
  NAME VARCHAR(255) NOT NULL,
  MIN_VALUE INTEGER,
  MAX_VALUE INTEGER,
  CONSTRAINT CTUQ_UNIT_NAME UNIQUE(NAME)
);
  
CREATE MEMORY TABLE USER (
  ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY,
  VERSION BIGINT NOT NULL,
  PASSWD VARCHAR(255) NOT NULL,
  ENABLED BOOLEAN NOT NULL,
  USERNAME VARCHAR(255) NOT NULL,
  EMAIL VARCHAR(255) NOT NULL,
  DESCRIPTION VARCHAR(255) NOT NULL,
  REAL_USER_NAME VARCHAR(255) NOT NULL,
  CONSTRAINT CTUQ_USER_USERNAME UNIQUE(USERNAME)
);
  
ALTER TABLE REPLICATED_REPOSITORY ADD CONSTRAINT FK_RR_REPO_ID FOREIGN KEY(REPO_ID) REFERENCES REPOSITORY(ID);
ALTER TABLE ROLE_PEOPLE ADD CONSTRAINT FK_RP_USER_ID FOREIGN KEY(USER_ID) REFERENCES USER(ID);
ALTER TABLE GROUPS_PEOPLE ADD CONSTRAINT FK_GP_USER_ID FOREIGN KEY(USER_ID) REFERENCES USER(ID);
ALTER TABLE STAT_ACTION ADD CONSTRAINT FK_SA_GROUP_ID FOREIGN KEY(GROUP_ID) REFERENCES STAT_GROUP(ID);
ALTER TABLE STAT_GROUP ADD CONSTRAINT FK_SG_UNIT_ID FOREIGN KEY(UNIT_ID) REFERENCES UNIT(ID);
ALTER TABLE STAT_VALUE ADD CONSTRAINT FK_SV_STATISTIC_ID FOREIGN KEY(STATISTIC_ID) REFERENCES STATISTIC(ID);
