CREATE EXTENSION postgis;

CREATE FUNCTION last_modified() RETURNS trigger AS $last_modified$
    BEGIN
        NEW.LAST_MODIFIED := current_timestamp;
        RETURN NEW;
    END;
$last_modified$ LANGUAGE plpgsql;

CREATE TABLE SOURCE (
    ID BIGSERIAL NOT NULL,
    NAME VARCHAR(25) NOT NULL UNIQUE,
    DESCRIPTION text,
    INFO jsonb,
    CONSTRAINT PK_SOURCE PRIMARY KEY (ID)
);

CREATE TABLE PLACE (
    ID BIGSERIAL NOT NULL,
    COUNTRY_CODE CHAR(2) NOT NULL,
    ADMIN1 CHAR(2),
    ADMIN2 text,
    GEOM geometry(Geometry,4326) NOT NULL,
    TYPE text,
    SUBTYPE text,
    RANK integer,
    SOURCE_ID bigint,
    SOURCE_KEY bigint,
    SOURCE_DATE integer,
    INFO jsonb,
    LAST_MODIFIED TIMESTAMP with time zone,
    CONSTRAINT PK_PLACE PRIMARY KEY (ID)
);


CREATE TRIGGER TGR_PLACE_UPDATE BEFORE INSERT OR UPDATE ON PLACE
    FOR EACH ROW EXECUTE PROCEDURE last_modified();

CREATE TABLE PLACE_NAME (
    ID BIGSERIAL NOT NULL,
    NAME text NOT NULL,
    UNAME text,
    LANGUAGE_CODE CHAR(3),
    TYPE integer,
    PLACE_ID bigint,
    INFO jsonb,
    LAST_MODIFIED TIMESTAMP with time zone,
    CONSTRAINT PK_PLACE_NAME PRIMARY KEY (ID)
);


CREATE TRIGGER TGR_PLACE_NAME_UPDATE BEFORE INSERT OR UPDATE ON PLACE_NAME
    FOR EACH ROW EXECUTE PROCEDURE last_modified();

CREATE TABLE EDIT (
    ID BIGSERIAL NOT NULL,
    SUMMARY text NOT NULL,
    DETAILS text,
    INFO jsonb,
    START_DATE TIMESTAMP with time zone NOT NULL,
    END_DATE TIMESTAMP with time zone,
    CONSTRAINT PK_EDIT PRIMARY KEY (ID)
);

CREATE TABLE CHANGE_REQUEST (
    ID BIGSERIAL NOT NULL,
    PLACE_ID bigint NOT NULL,
    INFO jsonb NOT NULL,
    LAST_MODIFIED TIMESTAMP with time zone,
    CONSTRAINT PK_CHANGE_REQUEST PRIMARY KEY (ID)
);


CREATE TRIGGER TGR_CHANGE_REQUEST_UPDATE BEFORE INSERT OR UPDATE ON CHANGE_REQUEST
    FOR EACH ROW EXECUTE PROCEDURE last_modified();

ALTER TABLE PLACE ADD FOREIGN KEY (SOURCE_ID) REFERENCES SOURCE(ID)
    ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE PLACE_NAME ADD FOREIGN KEY (PLACE_ID) REFERENCES PLACE(ID)
    ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX IDX_PLACE_GEOM ON PLACE USING GIST(GEOM);
CREATE INDEX IDX_PLACE_SOURCE ON PLACE(SOURCE_ID);
CREATE INDEX IDX_PLACE_NAME_PLACE ON PLACE_NAME(PLACE_ID);
