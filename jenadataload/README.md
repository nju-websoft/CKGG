# Source code for data ingestion

## GIS data ingestion

Please refer to `gis/import_ne.md` for details.

## Running data ingestion

To run data ingestion, uncomment parts in `src/main/java/test/NewMain.java`, and run `mvn package && java -jar target/testjena-1.0-SNAPSHOT.jar`.

Notice data ingestion process runs in several passes, and extra works should be done between passes (such as importing to postgis, importing to Virtuoso), thus each pass only part of `src/main/java/test/NewMain.java` should be run.
