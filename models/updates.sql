
-- Create function to automatically uppercase names in the "place_name" table

CREATE FUNCTION uppercase() RETURNS trigger AS $uppercase$
    BEGIN
        NEW.UNAME = UPPER(NEW.NAME);
        RETURN NEW;
    END;
$uppercase$ LANGUAGE plpgsql;

CREATE TRIGGER TGR_PLACE_NAME_UNAME BEFORE INSERT OR UPDATE ON PLACE_NAME
    FOR EACH ROW EXECUTE PROCEDURE uppercase();



-- Update the "place_id" foreign key on the "place_name" table

alter table place_name drop constraint place_name_place_id_fkey;
ALTER TABLE PLACE_NAME ADD FOREIGN KEY (PLACE_ID) REFERENCES PLACE(ID)
    ON DELETE CASCADE ON UPDATE NO ACTION;




-- Add foreign key on the "change_request" table

ALTER TABLE CHANGE_REQUEST ADD FOREIGN KEY (PLACE_ID) REFERENCES PLACE(ID)
    ON DELETE CASCADE ON UPDATE NO ACTION;



-- Index Fields for Search/Ingest

create index idx_place_uname on place_name(uname);
create index idx_place_cc on place(country_code);
create index idx_place_rank on place(rank);
create index idx_place_type on place(type);
create unique index idx_place_unique on place(source_id, source_key, country_code);
create unique index idx_place_name_unique on place_name(place_id, uname);



