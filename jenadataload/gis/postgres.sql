-- Check POSTGIS
SELECT PostGIS_Version();

-- Import geonames to db
create table geoname (
	geonameid	int,
	name varchar(200),
	asciiname varchar(200),
	alternatenames text,
	latitude float,
	longitude float,
	fclass char(1),
	fcode varchar(10),
	country varchar(2),
	cc2 varchar(200),
	admin1 varchar(20),
	admin2 varchar(80),
	admin3 varchar(20),
	admin4 varchar(20),
	population bigint,
	elevation int,
	gtopo30 int,
	timezone varchar(40),
	moddate date
);
create table alternatename (
	alternatenameId int,
	geonameid int,
	isoLanguage varchar(7),
	alternateName varchar(200),
	isPreferredName boolean,
	isShortName boolean,
	isColloquial boolean,
	isHistoric boolean,
	from_period varchar(20),
	to_period varchar(20)
);
copy geoname (geonameid,name,asciiname,alternatenames,latitude,longitude,fclass,fcode,country,cc2,admin1,admin2,admin3,admin4,population,elevation,gtopo30,timezone,moddate) from '/import/allCountries.txt' null as '';
copy alternatename  (alternatenameid, geonameid, isolanguage, alternatename, ispreferredname, isshortname, iscolloquial, ishistoric, from_period, to_period) from '/import/alternateNamesV2.txt' null as '';
ALTER TABLE ONLY alternatename ADD CONSTRAINT pk_alternatenameid PRIMARY KEY (alternatenameid);
ALTER TABLE ONLY geoname ADD CONSTRAINT pk_geonameid PRIMARY KEY (geonameid);
ALTER TABLE ONLY alternatename ADD CONSTRAINT fk_geonameid FOREIGN KEY (geonameid) REFERENCES geoname(geonameid);

-- Import polygons: should first import gpkg file using ogr2ogr tool
DROP MATERIALIZED VIEW IF EXISTS all_wikidata_regions_v2;
CREATE MATERIALIZED VIEW all_wikidata_regions_v2 AS SELECT string_agg(distinct featurecla, '\t'), wikidataid, st_union(geom) as geom from (
    select featurecla, wikidataid, geom from ne_10m_geography_regions_polys
    union distinct select featurecla, wikidataid, geom from ne_10m_geography_marine_polys
    union distinct select featurecla, wikidataid, geom from ne_10m_admin_0_countries
    union distinct select featurecla, wikidataid, geom from ne_10m_admin_1_states_provinces
    union distinct select featurecla, wikidataid, geom from ne_10m_populated_places
    union distinct select featurecla, wikidataid, geom from ne_10m_lakes
    union distinct select featurecla, wikidataid, geom from ne_10m_lakes_historic
    union distinct select featurecla, wikidataid, geom from ne_10m_playas
    ) as tt where wikidataid IS NOT NULL AND wikidataid ~ 'Q[0-9]+' AND st_area(geom) != 0 GROUP BY wikidataid;

-- Import points and regions: should run postprocess.ExportCanonicalCoordToCsv
create table new_dataset_points_v2
(
	id serial not null
		constraint new_dataset_points_v2_pk
			primary key,
	geom geometry,
	lon real,
	lat real
);

create index new_dataset_points_v2_geom_index
	on new_dataset_points_v2 USING GIST (geom);

TRUNCATE new_dataset_points_v2;
COPY new_dataset_points_v2 (id, lon, lat) from '/import/new_coord.csv' null as '';
UPDATE new_dataset_points_v2 SET geom=st_setsrid(st_point(lon, lat), 4326);

create table new_dataset_regions_v2
(
	id serial not null
		constraint new_dataset_regions_v2_pk
			primary key,
	geom geometry,
	geom_text text
);

create index new_dataset_regions_v2_geom_index
	on new_dataset_regions_v2 USING GIST (geom);

TRUNCATE new_dataset_regions_v2;
COPY new_dataset_regions_v2 (id, geom_text) from '/import/new_region.csv' null as '';
UPDATE new_dataset_regions_v2 SET geom=st_setsrid(st_geomfromtext(geom_text), 4326);

-- Import unbelong (A.PCLI and A.ADM1 entities): should run loadcsv.ExportUnBelongToCsv
create table unbelong
(
	id serial not null
		constraint unbelong_pk
			primary key
);
TRUNCATE unbelong;
COPY unbelong (id) from '/import/unbelong_id.csv' null as '';

-- Import part-of relation
DROP MATERIALIZED VIEW IF EXISTS belongs_v4_temp;
DROP MATERIALIZED VIEW IF EXISTS belongs_v4;
ALTER TABLE new_dataset_points_v2 SET (parallel_workers = 20);
ALTER TABLE new_dataset_regions_v2 SET (parallel_workers = 20);
ALTER TABLE unbelong SET (parallel_workers = 20);
CREATE MATERIALIZED VIEW belongs_v4_temp AS
(
-- point to polygon
SELECT new_dataset_points_v2.id AS from_id, new_dataset_regions_v2.id AS to_id, TRUE AS from_point
FROM new_dataset_points_v2, new_dataset_regions_v2
WHERE ST_WITHIN(new_dataset_points_v2.geom, new_dataset_regions_v2.geom) AND new_dataset_points_v2.id NOT IN (SELECT id FROM new_dataset_regions_v2)
UNION ALL
-- polygon to polygon
SELECT from_t.id AS from_id, to_t.id AS to_id, FALSE AS from_point
FROM new_dataset_regions_v2 AS from_t, new_dataset_regions_v2 AS to_t
WHERE ST_AREA(st_intersection(from_t.geom, to_t.geom)) / ST_AREA(from_t.geom) >= 0.95 AND from_t.id != to_t.id AND ST_AREA(from_t.geom) < ST_AREA(to_t.geom)
);
-- exclude unbelong
CREATE MATERIALIZED VIEW belongs_v4 AS SELECT from_id, to_id FROM belongs_v4_temp WHERE EXISTS (SELECT id FROM unbelong WHERE id=from_id) OR NOT from_point;

-- Import to ocean distance
DROP VIEW IF EXISTS in_ocean_points_v3;
DROP VIEW IF EXISTS in_ocean_shapes_v3;
DROP VIEW IF EXISTS not_in_ocean_points_v3;
DROP VIEW IF EXISTS not_in_ocean_shapes_v3;
DROP MATERIALIZED VIEW IF EXISTS place_to_ocean_distance_v3;
CREATE VIEW in_ocean_points_v3 AS
    SELECT id
    FROM new_dataset_points_v2
    WHERE EXISTS(SELECT TRUE FROM ne_110m_ocean WHERE st_within(new_dataset_points_v2.geom, geom))
      AND id NOT IN (SELECT id FROM new_dataset_regions_v2);
CREATE VIEW in_ocean_shapes_v3 AS
    SELECT id
    FROM new_dataset_regions_v2
    WHERE EXISTS(
        SELECT TRUE
        FROM ne_110m_ocean
        WHERE st_area(st_intersection(new_dataset_regions_v2.geom, geom)) > 0
    );
CREATE VIEW not_in_ocean_points_v3 AS
    SELECT id, new_dataset_points_v2.geom AS geom
    FROM new_dataset_points_v2
    WHERE NOT EXISTS(SELECT TRUE FROM ne_110m_ocean WHERE st_within(new_dataset_points_v2.geom, geom))
      AND id NOT IN (SELECT id FROM new_dataset_regions_v2);
CREATE VIEW not_in_ocean_shapes_v3 AS
    SELECT id, new_dataset_regions_v2.geom AS geom
    FROM new_dataset_regions_v2
    WHERE NOT EXISTS (
        SELECT TRUE
        FROM ne_110m_ocean
        WHERE st_area(st_intersection(new_dataset_regions_v2.geom, geom)) > 0
    );
CREATE TABLE ne_110m_coastline_geog AS SELECT fid, featurecla, scalerank, min_zoom, Geography(geom) AS geog FROM ne_110m_coastline;
CREATE INDEX ne_110m_coastline_geog_index ON ne_10m_coastline_geog USING GIST (geog);
CREATE MATERIALIZED VIEW place_to_ocean_distance_v3 AS
    SELECT DISTINCT ON(id) id, distance FROM (
                      SELECT id, 0 AS distance
                      FROM in_ocean_points_v3
                      UNION ALL
                      SELECT id, 0 AS distance
                      FROM in_ocean_shapes_v3
                      UNION ALL
                      SELECT id, MIN(st_distance(ne_110m_coastline_geog.geog, geography(not_in_ocean_points_v3.geom))) AS distance
                      FROM ne_110m_coastline_geog, not_in_ocean_points_v3
                      GROUP BY id
                      UNION ALL
                      SELECT id, MIN(st_distance(ne_110m_coastline_geog.geog, geography(not_in_ocean_shapes_v3.geom))) AS distance
                      FROM ne_110m_coastline_geog, not_in_ocean_shapes_v3
                      GROUP BY id
    ) AS t1;

-- Import ocean current
DROP MATERIALIZED VIEW IF EXISTS associated_ocean_flows_v3;
DROP MATERIALIZED VIEW IF EXISTS all_place_ocean_flows_v3;
CREATE MATERIALIZED VIEW all_place_ocean_flows_v3 AS SELECT * FROM (
                  SELECT st_distance(geography(ST_Transform(st_linemerge(wkb_geometry), 4326)),
                      geography(new_dataset_points_v2.geom)) AS distance,
                         new_dataset_points_v2.id AS id,
                         export_output.name AS ocean_flow_name
                  from export_output,
                       new_dataset_points_v2
                  WHERE new_dataset_points_v2.id NOT IN (SELECT id FROM new_dataset_regions_v2)
                  UNION ALL
                  SELECT st_distance(geography(ST_Transform(st_linemerge(wkb_geometry), 4326)),
                      geography(new_dataset_regions_v2.geom)) AS distance,
                         new_dataset_regions_v2.id AS id,
                         export_output.name AS ocean_flow_name
                  from export_output,
                       new_dataset_regions_v2
              ) as temp where distance <= 1000000;

-- Import climate
DROP MATERIALIZED VIEW IF EXISTS all_place_climate_v3;
CREATE MATERIALIZED VIEW all_place_climate_v3 AS
SELECT DISTINCT tt3.id, new_climate_mapping.name AS climate_name FROM ((
SELECT id, MIN(climate_id) AS climate_id FROM (
    SELECT new_dataset_points_v2.id AS id, new_climate.id AS climate_id FROM new_climate, new_dataset_points_v2
        WHERE st_within(new_dataset_points_v2.geom, new_climate.wkb_geometry)
          AND new_dataset_points_v2.id NOT IN (SELECT id FROM new_dataset_regions_v2)
    UNION ALL
    SELECT id, 9 AS climate_id FROM new_dataset_points_v2
        WHERE new_dataset_points_v2.lat > 70 OR new_dataset_points_v2.lat < -60
         AND new_dataset_points_v2.id NOT IN (SELECT id FROM new_dataset_regions_v2)
) AS tt2 GROUP BY id)
    UNION ALL(
        SELECT new_dataset_regions_v2.id AS id, new_climate.id AS climate_id FROM new_climate, new_dataset_regions_v2
            WHERE st_intersects(new_dataset_regions_v2.geom, new_climate.wkb_geometry)
        UNION ALL
        SELECT new_dataset_regions_v2.id AS id, 9 AS climate_id FROM new_dataset_regions_v2
            WHERE st_ymax(new_dataset_regions_v2.geom) > 70 OR st_ymin(new_dataset_regions_v2.geom) < -60
)) AS tt3, new_climate_mapping WHERE tt3.climate_id = new_climate_mapping.id;

-- Sampling for checking part of relation
SELECT * FROM belongs_v4_temp TABLESAMPLE BERNOULLI (1) REPEATABLE (1) WHERE NOT from_point LIMIT 100;
SELECT * FROM belongs_v4_temp TABLESAMPLE BERNOULLI (0.0003) REPEATABLE (1) WHERE EXISTS (SELECT id FROM unbelong WHERE id=from_id) LIMIT 100;
