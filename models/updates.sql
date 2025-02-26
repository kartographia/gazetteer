-- Index Fields for Search/Ingest
CREATE INDEX IDX_PLACE_SOURCE_KEY ON GAZETTEER.PLACE(SOURCE_KEY);
CREATE INDEX IDX_PLACE_COUNTRY_CODE ON GAZETTEER.PLACE(COUNTRY_CODE);
CREATE INDEX IDX_PLACE_RANK ON GAZETTEER.PLACE(RANK);
CREATE INDEX IDX_PLACE_TYPE ON GAZETTEER.PLACE(TYPE);
CREATE INDEX IDX_PLACE_SUBTYPE ON GAZETTEER.PLACE(SUBTYPE);



-- Create unique index on the place table
create unique index idx_place_unique on gazetteer.place(source_id, source_key, country_code);
create unique index idx_name_unique on gazetteer.name(place_id, uname);