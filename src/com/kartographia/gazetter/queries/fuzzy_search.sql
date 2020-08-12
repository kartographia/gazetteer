--with vars(q) as (select 'bordo')

select * from (

select distinct on(place_id) * from (

select row_number() OVER () as row_id, id as name_id, place_id, name, country_code, rank, type, rel from (

------------------------
-- Trigram Search
------------------------

select * from (
select name.id, name.place_id, name.name, country_code, rank, place.type, similarity(name, (select q from vars)) as rel
from gazetter.place join gazetter.name on place.id=place_id
where similarity(name, (select q from vars))>0.9 --and place.type='populated place'
order by rel desc, rank, name.type limit 10
) a

------------------------

union all

------------------------
-- Phonetic Search
------------------------

select * from (
select name.id, name.place_id, name.name, country_code, rank, place.type,  similarity(name, (select q from vars)) as rel
from gazetter.place join gazetter.name on place.id=place_id
where (DMETAPHONE_ALT(name) = DMETAPHONE_ALT((select q from vars))) --and place.type='populated place'
order by rank, rel desc, name.type limit 15
) b

------------------------

) search_results

) search_results_with_row_id

) search_results_with_distinct_place_id

order by row_id;
