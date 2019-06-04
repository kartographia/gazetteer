
-- Create function to automatically uppercase names in the "name" table

CREATE OR REPLACE FUNCTION uppercase() RETURNS trigger AS $uppercase$
    BEGIN
        NEW.UNAME = UPPER(NEW.NAME);
        RETURN NEW;
    END;
$uppercase$ LANGUAGE plpgsql;

CREATE TRIGGER TGR_PLACE_NAME_UNAME BEFORE INSERT OR UPDATE ON GAZETTER.NAME
    FOR EACH ROW EXECUTE PROCEDURE uppercase();


CREATE INDEX IDX_PLACE_UNAME ON GAZETTER.NAME(UNAME);



-- Index Fields for Search/Ingest
CREATE INDEX IDX_PLACE_COUNTRY_CODE ON GAZETTER.PLACE(COUNTRY_CODE);
CREATE INDEX IDX_PLACE_RANK ON GAZETTER.PLACE(RANK);
CREATE INDEX IDX_PLACE_TYPE ON GAZETTER.PLACE(TYPE);
CREATE INDEX IDX_PLACE_SUBTYPE ON GAZETTER.PLACE(SUBTYPE);



-- Create unique index on the place table
create unique index idx_place_unique on gazetter.place(source_id, source_key, country_code);