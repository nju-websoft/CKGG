# Source Code of CKGG Demo

## Running

- To run this demo, first import CKGG data into a Virtuoso server, add namespace prefix, and change endpoint in [../api/api.py](../api/api.py) to Virtuoso HTTP port in `utils/config.ts`.

- To be able to use search by coordinate, the coordinate information should be imported to named graph `<temp:coord>` using reified statement with predicate `ckgp:P23` and object in the form "POINT(*lon* *lat*)"^^geo:wktLiteral. The following Virtuoso isql script is used:

```plain
log_enable(2, 1);
SPARQL INSERT {GRAPH <temp:coord> {?s ckgp:P23 ?o}} WHERE {?s geo:lat ?lat; geo:lon ?lon. BIND(STRDT(CONCAT('POINT(', ?lon, ' ', ?lat, ')'), <http://www.opengis.net/ont/geosparql#wktLiteral>) AS ?o)};
log_enable(1, 1);
COMMIT WORK;
```

- Use `npm run build` to build front-end and copy `dist/` to api as `static/`, then run `gunicorn api:app` to run the demo.
