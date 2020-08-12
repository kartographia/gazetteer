-- Index Fields for Search/Ingest
CREATE INDEX IDX_PLACE_SOURCE_KEY ON GAZETTER.PLACE(SOURCE_KEY);
CREATE INDEX IDX_PLACE_COUNTRY_CODE ON GAZETTER.PLACE(COUNTRY_CODE);
CREATE INDEX IDX_PLACE_RANK ON GAZETTER.PLACE(RANK);
CREATE INDEX IDX_PLACE_TYPE ON GAZETTER.PLACE(TYPE);
CREATE INDEX IDX_PLACE_SUBTYPE ON GAZETTER.PLACE(SUBTYPE);



-- Create unique index on the place table
create unique index idx_place_unique on gazetter.place(source_id, source_key, country_code);
create unique index idx_name_unique on gazetter.name(place_id, uname);